package com.flow.desktop

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DesktopMusicState(
    val trackId: String? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.72f,
    val error: String? = null
)

/** Desktop MP3 player backed by Java Sound + MP3SPI. Audio is decoded locally, never streamed through a browser. */
class DesktopMusicPlayer {
    private val root = appDataDirectory()
    private val musicDir = File(root, "music").apply { mkdirs() }
    private val favoritesFile = File(root, "music-favorites.txt")
    private val audioExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "MedFlow-Audio").apply { isDaemon = true } }
    private val downloadExecutor = Executors.newFixedThreadPool(2) { r -> Thread(r, "MedFlow-MusicDownload").apply { isDaemon = true } }
    private val clock: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "MedFlow-AudioClock").apply { isDaemon = true } }
    private val _state = MutableStateFlow(DesktopMusicState())
    val state: StateFlow<DesktopMusicState> = _state.asStateFlow()
    private val _downloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloads: StateFlow<Map<String, Float>> = _downloads.asStateFlow()
    private val _favorites = MutableStateFlow(readFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    @Volatile private var clip: Clip? = null
    @Volatile private var pausedFrame = 0
    @Volatile private var currentId: String? = null

    init {
        clock.scheduleAtFixedRate({
            val current = clip ?: return@scheduleAtFixedRate
            val length = current.microsecondLength.coerceAtLeast(0L)
            val position = current.microsecondPosition.coerceAtLeast(0L)
            _state.value = _state.value.copy(
                playing = current.isRunning,
                positionMs = position / 1_000L,
                durationMs = length / 1_000L
            )
        }, 0, 250, TimeUnit.MILLISECONDS)
    }

    fun isDownloaded(track: DesktopTrack): Boolean {
        val file = File(musicDir, track.fileName)
        return file.isFile && file.length() == track.fileSize && plausibleMp3(file)
    }

    fun toggleFavorite(trackId: String) {
        if (DesktopMusicCatalog.tracks.none { it.id == trackId }) return
        val next = if (trackId in _favorites.value) _favorites.value - trackId else _favorites.value + trackId
        _favorites.value = next
        runCatching { favoritesFile.writeText(next.sorted().joinToString("\n"), Charsets.UTF_8) }
    }

    fun download(track: DesktopTrack) {
        if (isDownloaded(track)) return
        _downloads.value = _downloads.value + (track.id to 0f)
        downloadExecutor.execute {
            val part = File(musicDir, track.fileName + ".part")
            val target = File(musicDir, track.fileName)
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(track.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "MedFlow-Desktop/1.0")
                }
                require(connection.responseCode in 200..299 && connection.url.protocol == "https") {
                    "دریافت فایل موسیقی ناموفق بود."
                }
                val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
                require(contentType.isEmpty() || contentType.startsWith("audio/") || contentType == "application/octet-stream") {
                    "پاسخ سرور فایل صوتی معتبر نیست."
                }
                val contentLength = connection.contentLengthLong
                require(contentLength <= 0L || contentLength == track.fileSize) { "اندازه‌ی فایل با کاتالوگ مطابقت ندارد." }
                var written = 0L
                connection.inputStream.use { input ->
                    part.outputStream().buffered().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            written += count
                            require(written <= track.fileSize) { "فایل دریافتی بزرگ‌تر از اندازه‌ی مورد انتظار است." }
                            output.write(buffer, 0, count)
                            _downloads.value = _downloads.value + (track.id to (written.toFloat() / track.fileSize).coerceIn(0f, 1f))
                        }
                    }
                }
                require(written == track.fileSize && plausibleMp3(part)) { "فایل دریافتی ناقص یا MP3 نامعتبر است." }
                if (target.exists()) target.delete()
                check(part.renameTo(target)) { "ذخیره‌ی فایل موسیقی ناموفق بود." }
                _state.value = _state.value.copy(error = null)
            } catch (e: Exception) {
                part.delete()
                _state.value = _state.value.copy(error = e.message ?: "خطا در دریافت موسیقی")
            } finally {
                connection?.disconnect()
                _downloads.value = _downloads.value - track.id
            }
        }
    }

    fun play(track: DesktopTrack) {
        audioExecutor.execute {
            try {
                val file = File(musicDir, track.fileName)
                require(isDownloaded(track)) { "ابتدا فایل این آهنگ را دانلود کن." }
                if (currentId == track.id && clip != null) {
                    val existing = clip!!
                    if (!existing.isRunning) {
                        existing.framePosition = pausedFrame.coerceIn(0, (existing.frameLength - 1).coerceAtLeast(0))
                        existing.start()
                    }
                    _state.value = _state.value.copy(playing = true, error = null)
                    return@execute
                }
                closeClip()
                val source = AudioSystem.getAudioInputStream(file)
                val format = source.format
                val pcm = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, 16,
                    format.channels, format.channels * 2, format.sampleRate, false
                )
                val decoded = AudioSystem.getAudioInputStream(pcm, source)
                val next = AudioSystem.getClip()
                next.open(decoded)
                decoded.close()
                source.close()
                clip = next
                currentId = track.id
                pausedFrame = 0
                applyVolume(next, _state.value.volume)
                next.start()
                _state.value = DesktopMusicState(
                    trackId = track.id, playing = true, positionMs = 0,
                    durationMs = next.microsecondLength / 1_000L, volume = _state.value.volume
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(playing = false, error = e.message ?: "پخش این فایل ممکن نیست.")
            }
        }
    }

    fun togglePlayback() {
        audioExecutor.execute {
            val current = clip ?: return@execute
            if (current.isRunning) {
                pausedFrame = current.framePosition
                current.stop()
                _state.value = _state.value.copy(playing = false, positionMs = current.microsecondPosition / 1_000L)
            } else {
                current.framePosition = pausedFrame.coerceIn(0, (current.frameLength - 1).coerceAtLeast(0))
                current.start()
                _state.value = _state.value.copy(playing = true)
            }
        }
    }

    fun stop() {
        audioExecutor.execute {
            clip?.stop()
            pausedFrame = 0
            clip?.framePosition = 0
            _state.value = _state.value.copy(playing = false, positionMs = 0)
        }
    }

    fun seekTo(positionMs: Long) {
        audioExecutor.execute {
            val current = clip ?: return@execute
            val frame = (positionMs.coerceAtLeast(0) * current.format.frameRate / 1_000.0).toInt()
                .coerceIn(0, (current.frameLength - 1).coerceAtLeast(0))
            current.framePosition = frame
            pausedFrame = frame
            _state.value = _state.value.copy(positionMs = current.microsecondPosition / 1_000L)
        }
    }

    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = volume)
        audioExecutor.execute { clip?.let { applyVolume(it, volume) } }
    }

    fun close() {
        clock.shutdownNow()
        downloadExecutor.shutdownNow()
        audioExecutor.execute { closeClip() }
        audioExecutor.shutdown()
    }

    private fun closeClip() {
        runCatching { clip?.stop(); clip?.close() }
        clip = null
        currentId = null
        pausedFrame = 0
    }

    private fun applyVolume(target: Clip, value: Float) {
        runCatching {
            val control = target.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val min = control.minimum
            val max = control.maximum
            val gain = if (value <= 0.001f) min else (20f * kotlin.math.log10(value)).coerceIn(min, max)
            control.value = gain
        }
    }

    private fun plausibleMp3(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val head = ByteArray(3)
            if (input.read(head) != 3) false
            else (head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()) ||
                ((head[0].toInt() and 0xFF) == 0xFF && (head[1].toInt() and 0xE0) == 0xE0)
        }
    }.getOrDefault(false)

    private fun readFavorites(): Set<String> = runCatching {
        favoritesFile.readLines(Charsets.UTF_8).filter { id -> DesktopMusicCatalog.tracks.any { it.id == id } }.toSet()
    }.getOrDefault(emptySet())
}

internal fun appDataDirectory(): File {
    val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }?.let(::File)
        ?: File(System.getProperty("user.home"), "AppData/Roaming")
    return File(appData, "MedFlow").apply { mkdirs() }
}
