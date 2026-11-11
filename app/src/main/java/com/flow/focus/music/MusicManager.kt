package com.flow.focus.music

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val Context.matFocusMusicStore by preferencesDataStore(name = "matfocus_music_v1")

data class MusicSnapshot(
    val connected: Boolean = false,
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.8f,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
)

class MusicManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _snapshot = MutableStateFlow(MusicSnapshot())
    val snapshot: StateFlow<MusicSnapshot> = _snapshot.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val store = context.matFocusMusicStore
    private val favoritesKey = stringPreferencesKey("favorite_track_ids")
    private val json = Json { ignoreUnknownKeys = true }
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    @Volatile private var pendingPlayTrackId: String? = null
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    init {
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync().also { future ->
            future.addListener({
                runCatching {
                    controller = future.get().also { player ->
                        player.addListener(playerListener)
                        if (player.mediaItemCount == 0) {
                            player.setMediaItems(MusicCatalog.tracks.map { track -> MusicCatalog.mediaItem(track, downloadedFile(track)) })
                        }
                    }
                    pendingPlayTrackId?.let { id -> pendingPlayTrackId = null; play(id) }
                    publish()
                }
            }, ContextCompat.getMainExecutor(context))
        }
        scope.launch {
            store.data.catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
                .collect { prefs ->
                    _favorites.value = prefs[favoritesKey]?.let {
                        runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull()
                    } ?: emptySet()
                }
        }
        scope.launch {
            while (true) {
                publish()
                delay(500L)
            }
        }
    }

    fun play(trackId: String) {
        val player = controller
        if (player == null) { pendingPlayTrackId = trackId; return }
        val index = MusicCatalog.tracks.indexOfFirst { it.id == trackId }
        if (index < 0) return
        val items = MusicCatalog.tracks.map { track ->
            MusicCatalog.mediaItem(track, downloadedFile(track))
        }
        player.setMediaItems(items, index, C.TIME_UNSET)
        player.prepare()
        player.play()
        publish()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else {
            if (player.mediaItemCount == 0) play(MusicCatalog.tracks.first().id) else player.play()
        }
        publish()
    }

    fun pause() { controller?.pause(); publish() }
    fun next() { controller?.seekToNextMediaItem(); publish() }
    fun previous() { controller?.seekToPreviousMediaItem(); publish() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs); publish() }
    fun setVolume(value: Float) { controller?.volume = value.coerceIn(0f, 1f); publish() }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        publish()
    }

    fun cycleRepeat() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publish()
    }

    fun toggleFavorite(trackId: String) {
        if (MusicCatalog.tracks.none { it.id == trackId }) return
        scope.launch {
            store.edit { prefs ->
                val current = prefs[favoritesKey]?.let {
                    runCatching { json.decodeFromString<List<String>>(it).toSet() }.getOrNull()
                } ?: emptySet()
                prefs[favoritesKey] = json.encodeToString(if (trackId in current) current - trackId else current + trackId)
            }
        }
    }

    fun isDownloaded(trackId: String): Boolean =
        MusicCatalog.tracks.firstOrNull { it.id == trackId }?.let { downloadedFile(it)?.let(File::exists) == true } ?: false

    fun downloadedBytes(): Long = File(context.filesDir, "audio/music").listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    suspend fun downloadTrack(trackId: String, onProgress: (Float) -> Unit = {}): File =
        withContext(Dispatchers.IO) {
            val track = MusicCatalog.tracks.firstOrNull { it.id == trackId }
                ?: throw IllegalArgumentException("آهنگ پیدا نشد.")
            val folder = File(context.filesDir, "audio/music").apply { mkdirs() }
            val target = File(folder, track.fileName)
            if (target.isFile && target.length() == track.fileSize) return@withContext target
            val part = File(folder, track.fileName + ".part")
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(track.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "MatFocus-Music/1.2")
                }
                require(connection.responseCode in 200..299 && connection.url.protocol == "https") { "دریافت فایل صوتی ناموفق بود." }
                val length = connection.contentLengthLong
                require(length <= 0L || length == track.fileSize) { "حجم فایل صوتی با فهرست موسیقی مطابقت ندارد." }
                var written = 0L
                connection.inputStream.use { input ->
                    part.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            written += count
                            val p = (written.toFloat() / track.fileSize).coerceIn(0f, 1f)
                            _downloadProgress.value = _downloadProgress.value + (trackId to p)
                            onProgress(p)
                            require(written <= track.fileSize) { "فایل صوتی بزرگ‌تر از اندازه‌ی فهرست است." }
                        }
                    }
                }
                require(written == track.fileSize) { "فایل صوتی ناقص دانلود شد." }
                if (target.exists()) target.delete()
                check(part.renameTo(target)) { "ذخیره‌ی فایل صوتی ناموفق بود." }
                _downloadProgress.value = _downloadProgress.value - trackId
                target
            } catch (error: Exception) {
                part.delete()
                _downloadProgress.value = _downloadProgress.value - trackId
                throw error
            } finally {
                connection?.disconnect()
            }
        }

    fun removeDownload(trackId: String): Boolean {
        val track = MusicCatalog.tracks.firstOrNull { it.id == trackId } ?: return false
        val file = downloadedFile(track) ?: return false
        return !file.exists() || file.delete()
    }

    fun downloadedFile(track: MusicTrack): File? =
        File(context.filesDir, "audio/music/${track.fileName}").takeIf { it.isFile && it.length() == track.fileSize }

    private fun publish() {
        val p = controller
        if (p == null) {
            _snapshot.value = _snapshot.value.copy(connected = false)
            return
        }
        val id = p.currentMediaItem?.mediaId
        _snapshot.value = MusicSnapshot(
            connected = true,
            currentTrackId = id,
            isPlaying = p.isPlaying,
            positionMs = p.currentPosition.coerceAtLeast(0L),
            durationMs = p.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            volume = p.volume,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
        )
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller = null
        pendingPlayTrackId = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}
