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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

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
                    val feed = prefs[feedKey]?.let { runCatching { json.decodeFromString<NewsFeed>(it) }.getOrNull() }
                    val update = prefs[manifestKey]?.let { runCatching { json.decodeFromString<UpdateManifest>(it) }.getOrNull() }
                    val reads = prefs[readIdsKey]?.let { runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull() } ?: emptySet()
                    _items.value = feed?.items.orEmpty().filter(::isActive).sortedByDescending { it.publishedAt }
                    _manifest.value = update?.takeIf(::isValidManifest)
                    _readIds.value = reads
                    _lastSync.value = prefs[lastSyncKey]
                }
        }
    }

    /** Fresh network data is pulled at launch/open and by WorkManager; failures preserve cached content. */
    suspend fun refresh(force: Boolean = false): Boolean {
        val prefs = store.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .first()
        val last = prefs[lastSyncKey] ?: 0L
        val now = System.currentTimeMillis()
        if (!force && now - last < MIN_REFRESH_INTERVAL_MS) return true

        val feedText = fetchText(NEWS_URL)
        val manifestText = fetchText(UPDATE_URL)
        var changed = false
        var validResponse = false

        if (feedText != null) {
            val parsed = runCatching { json.decodeFromString<NewsFeed>(feedText) }.getOrNull()
            if (parsed != null && parsed.schemaVersion == 1) {
                validResponse = true
                val normalized = parsed.copy(items = parsed.items.filter { it.id.isNotBlank() && it.title.isNotBlank() && isActive(it) })
                val normalizedJson = json.encodeToString(normalized)
                if (prefs[feedKey] != normalizedJson) {
                    store.edit { it[feedKey] = normalizedJson }
                    changed = true
                }
                _items.value = normalized.items.sortedByDescending { it.publishedAt }
            }
        }

        if (manifestText != null) {
            val parsed = runCatching { json.decodeFromString<UpdateManifest>(manifestText) }.getOrNull()
            if (parsed != null && isValidManifest(parsed)) {
                validResponse = true
                val manifestJson = json.encodeToString(parsed)
                if (prefs[manifestKey] != manifestJson) {
                    store.edit { it[manifestKey] = manifestJson }
                    changed = true
                }
                _manifest.value = parsed
            }
        }

        // Keep the last successful check timestamp. A failed request is retried on the next app open/worker run.
        if (validResponse) {
            store.edit { it[lastSyncKey] = now }
            _lastSync.value = now
        }
        return changed || (_items.value.isNotEmpty() || _manifest.value != null)
    }

    suspend fun markRead(id: String) {
        if (id.isBlank()) return
        store.edit { prefs ->
            val current = prefs[readIdsKey]?.let { runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull() } ?: emptySet()
            prefs[readIdsKey] = json.encodeToString((current + id).toList())
        }
    }

    suspend fun markUnread(id: String) {
        if (id.isBlank()) return
        store.edit { prefs ->
            val current = prefs[readIdsKey]?.let { runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull() } ?: emptySet()
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
            (_manifest.value?.let { if (it.updateAvailable && it.versionCode > com.flow.focus.BuildConfig.VERSION_CODE && "update:${it.versionCode}" !in _readIds.value) 1 else 0 } ?: 0)

    private fun isActive(item: NewsItem): Boolean =
        item.enabled && (item.expiresAt == null || item.expiresAt > System.currentTimeMillis())

    private fun isValidManifest(value: UpdateManifest): Boolean =
        value.schemaVersion == 1 &&
            value.versionCode > 0 &&
            value.versionName.isNotBlank() &&
            value.applicationId == "com.flow.focus" &&
            value.minimumSupportedVersion >= 0 &&
            value.minimumSupportedVersion <= value.versionCode &&
            (!value.updateAvailable || (
                value.downloadUrl.startsWith("https://") &&
                    value.fileSize > 0 &&
                    value.sha256.matches(Regex("(?i)^[0-9a-f]{64}$"))
                ))

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
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().take(MAX_JSON_CHARS) }
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
        private const val MAX_JSON_CHARS = 1_000_000
    }
}

class RemoteContentSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        val remote = (applicationContext as FlowApp).container.remote
        if (remote.refresh(force = true)) Result.success() else Result.retry()
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
