package com.flow.focus.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flow.focus.FlowApp
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.matFocusRemoteStore by preferencesDataStore(name = "matfocus_remote_v1")

@Serializable
data class NewsFeed(
    val schemaVersion: Int = 1,
    val updatedAt: String? = null,
    val items: List<NewsItem> = emptyList(),
)

@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val body: String,
    val category: String = "News",
    val publishedAt: Long,
    val expiresAt: Long? = null,
    val enabled: Boolean = true,
    val url: String? = null,
)

@Serializable
data class UpdateManifest(
    val schemaVersion: Int = 1,
    val versionName: String = "1.2",
    val versionCode: Int = 1027,
    val releaseDate: String = "",
    val changelog: List<String> = emptyList(),
    val downloadUrl: String = "",
    val fileSize: Long = 0L,
    val sha256: String = "",
    val minimumSupportedVersion: Int = 0,
    val mandatory: Boolean = false,
    val updateAvailable: Boolean = false,
    val applicationId: String = "com.flow.focus",
)

/** Pure validation helpers shared by the repository and unit tests. */
internal object RemoteContentValidation {
    const val MAX_JSON_CHARS = 1_000_000
    const val MAX_NEWS_ITEMS = 500
    const val MAX_APK_BYTES = 150L * 1024L * 1024L

    private val json = Json { ignoreUnknownKeys = true }

    fun parseNewsFeed(raw: String, nowMs: Long): NewsFeed? {
        if (raw.length > MAX_JSON_CHARS) return null
        val feed = runCatching { json.decodeFromString<NewsFeed>(raw) }.getOrNull() ?: return null
        if (feed.schemaVersion != 1 || feed.items.size > MAX_NEWS_ITEMS) return null

        val normalized = feed.items.asSequence()
            .filter { isValidNewsItem(it, nowMs) }
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
            .toList()
        return feed.copy(items = normalized)
    }

    fun isValidNewsItem(item: NewsItem, nowMs: Long): Boolean =
        item.enabled &&
            item.id.isNotBlank() &&
            item.id.length <= 96 &&
            item.id.none(Char::isISOControl) &&
            item.title.isNotBlank() &&
            item.title.length <= 180 &&
            item.body.isNotBlank() &&
            item.body.length <= 8_000 &&
            item.category.isNotBlank() &&
            item.category.length <= 32 &&
            item.publishedAt > 0L &&
            (item.expiresAt == null || (item.expiresAt > nowMs && item.expiresAt > item.publishedAt)) &&
            (item.url == null || isHttpsUrl(item.url))

    fun isValidManifest(value: UpdateManifest): Boolean =
        value.schemaVersion == 1 &&
            value.versionCode > 0 &&
            value.versionName.isNotBlank() &&
            value.versionName.length <= 40 &&
            value.applicationId == "com.flow.focus" &&
            value.minimumSupportedVersion >= 0 &&
            value.minimumSupportedVersion <= value.versionCode &&
            value.changelog.size <= 20 &&
            value.changelog.all { it.isNotBlank() && it.length <= 500 } &&
            (!value.updateAvailable || (
                isHttpsUrl(value.downloadUrl) &&
                    value.fileSize in 1L..MAX_APK_BYTES &&
                    value.sha256.matches(Regex("(?i)^[0-9a-f]{64}$"))
                ))

    fun isHttpsUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }.getOrDefault(false)

    fun readBounded(reader: Reader, maxChars: Int = MAX_JSON_CHARS): String? {
        val output = StringBuilder(minOf(maxChars, 8_192))
        val buffer = CharArray(8_192)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) return output.toString()
            if (output.length + count > maxChars) return null
            output.append(buffer, 0, count)
        }
    }
}

class RemoteContentRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val store = context.matFocusRemoteStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val feedKey = stringPreferencesKey("news_feed_json")
    private val manifestKey = stringPreferencesKey("latest_update_json")
    private val readIdsKey = stringPreferencesKey("read_notice_ids")
    private val lastSyncKey = longPreferencesKey("last_sync_ms")
    private val refreshMutex = Mutex()
    private var lastAttemptAt = 0L

    private val _items = MutableStateFlow<List<NewsItem>>(emptyList())
    val items: StateFlow<List<NewsItem>> = _items.asStateFlow()

    private val _manifest = MutableStateFlow<UpdateManifest?>(null)
    val manifest: StateFlow<UpdateManifest?> = _manifest.asStateFlow()

    private val _readIds = MutableStateFlow<Set<String>>(emptySet())
    val readIds: StateFlow<Set<String>> = _readIds.asStateFlow()

    private val _lastSync = MutableStateFlow<Long?>(null)
    val lastSync: StateFlow<Long?> = _lastSync.asStateFlow()

    init {
        scope.launch {
            store.data.catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
                .collect { prefs ->
                    val feed = prefs[feedKey]?.let {
                        runCatching { json.decodeFromString<NewsFeed>(it) }.getOrNull()
                    }
                    val update = prefs[manifestKey]?.let {
                        runCatching { json.decodeFromString<UpdateManifest>(it) }.getOrNull()
                    }
                    val reads = prefs[readIdsKey]?.let {
                        runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull()
                    } ?: emptySet()
                    _items.value = feed?.items.orEmpty()
                        .filter { RemoteContentValidation.isValidNewsItem(it, System.currentTimeMillis()) }
                        .distinctBy { it.id }
                        .sortedByDescending { it.publishedAt }
                    _manifest.value = update?.takeIf(RemoteContentValidation::isValidManifest)
                    _readIds.value = reads
                    _lastSync.value = prefs[lastSyncKey]
                }
        }
    }

    /** Refresh requests are serialized; failed or invalid responses never replace the last good cache. */
    suspend fun refresh(force: Boolean = false): Boolean = refreshMutex.withLock {
        val prefs = store.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .first()
        val now = System.currentTimeMillis()
        val last = prefs[lastSyncKey] ?: 0L
        if (!force && now - last in 0 until MIN_REFRESH_INTERVAL_MS) return@withLock true
        // App launch, onResume and WorkManager may all request a refresh together.
        if (now - lastAttemptAt in 0 until MIN_DUPLICATE_ATTEMPT_INTERVAL_MS) {
            return@withLock _lastSync.value != null
        }
        lastAttemptAt = now

        val feedText = fetchText(NEWS_URL)
        val manifestText = fetchText(UPDATE_URL)
        var feedValid = false
        var manifestValid = false

        if (feedText != null) {
            val parsed = RemoteContentValidation.parseNewsFeed(feedText, now)
            if (parsed != null) {
                feedValid = true
                val normalizedJson = json.encodeToString(parsed)
                if (prefs[feedKey] != normalizedJson) {
                    store.edit { it[feedKey] = normalizedJson }
                }
                _items.value = parsed.items
            }
        }

        if (manifestText != null) {
            val parsed = runCatching { json.decodeFromString<UpdateManifest>(manifestText) }.getOrNull()
            if (parsed != null && RemoteContentValidation.isValidManifest(parsed)) {
                manifestValid = true
                val manifestJson = json.encodeToString(parsed)
                if (prefs[manifestKey] != manifestJson) {
                    store.edit { it[manifestKey] = manifestJson }
                }
                _manifest.value = parsed
            }
        }

        val completeSync = feedValid && manifestValid
        if (completeSync) {
            store.edit { it[lastSyncKey] = now }
            _lastSync.value = now
        }
        completeSync
    }

    suspend fun markRead(id: String) {
        if (id.isBlank()) return
        store.edit { prefs ->
            val current = prefs[readIdsKey]?.let {
                runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull()
            } ?: emptySet()
            prefs[readIdsKey] = json.encodeToString((current + id).toList())
        }
    }

    suspend fun markUnread(id: String) {
        if (id.isBlank()) return
        store.edit { prefs ->
            val current = prefs[readIdsKey]?.let {
                runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull()
            } ?: emptySet()
            prefs[readIdsKey] = json.encodeToString((current - id).toList())
        }
    }

    suspend fun markAllRead() {
        store.edit { prefs ->
            val ids = _items.value.map { it.id }.toMutableSet()
            _manifest.value
                ?.takeIf { it.updateAvailable && it.versionCode > com.flow.focus.BuildConfig.VERSION_CODE }
                ?.let { ids.add("update:${it.versionCode}") }
            prefs[readIdsKey] = json.encodeToString(ids.toList())
        }
    }

    fun unreadCount(now: Long = System.currentTimeMillis()): Int =
        _items.value.count { it.id !in _readIds.value && it.enabled && (it.expiresAt == null || it.expiresAt > now) } +
            (_manifest.value?.let {
                if (it.updateAvailable && it.versionCode > com.flow.focus.BuildConfig.VERSION_CODE &&
                    "update:${it.versionCode}" !in _readIds.value
                ) 1 else 0
            } ?: 0)

    private suspend fun fetchText(url: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "MatFocus-Android/1.2")
            }
            if (connection.responseCode !in 200..299 || connection.url.protocol != "https") return@withContext null
            if (connection.contentLengthLong > RemoteContentValidation.MAX_JSON_CHARS) return@withContext null
            connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                RemoteContentValidation.readBounded(it)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val NEWS_URL = "https://raw.githubusercontent.com/MayT3n/FLOW/main/notifications/news.json"
        const val UPDATE_URL = "https://raw.githubusercontent.com/MayT3n/FLOW/main/updates/latest.json"
        private const val MIN_REFRESH_INTERVAL_MS = 15 * 60 * 1000L
        private const val MIN_DUPLICATE_ATTEMPT_INTERVAL_MS = 5_000L
    }
}

class RemoteContentSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        val remote = (applicationContext as FlowApp).container.remote
        if (remote.refresh(force = true)) Result.success() else Result.retry()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }
}

object RemoteContentScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RemoteContentSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "matfocus_remote_content_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
