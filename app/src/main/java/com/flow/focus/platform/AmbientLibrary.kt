package com.flow.focus.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AmbientRecording(
    val id: String,
    val title: String,
    val hint: String,
    val url: String,
    val sourcePage: String,
    val creator: String,
    val duration: String,
    val license: String,
    val licenseUrl: String,
    val attribution: String? = null,
)

/** Source and license are kept beside each recording for auditability. */
object AmbientLibrary {
    val recordings = listOf(
        AmbientRecording(
            "rain", "باران واقعی", "ضبط میدانی بارش باران · حدود ۴۵ ثانیه",
            "https://commons.wikimedia.org/wiki/Special:Redirect/file/Rain_%281%29.ogg",
            "https://commons.wikimedia.org/wiki/File:Rain_(1).ogg", "ezwa", "00:45", "Public Domain",
            "https://creativecommons.org/publicdomain/zero/1.0/",
        ),
        AmbientRecording(
            "forest", "جنگل", "ضبط فضای جنگلی با پرندگان و باد · ۲:۰۳",
            "https://commons.wikimedia.org/wiki/Special:Redirect/file/20090610_0_ambience.ogg",
            "https://commons.wikimedia.org/wiki/File:20090610_0_ambience.ogg", "nille", "02:03", "Public Domain",
            "https://creativecommons.org/publicdomain/zero/1.0/",
        ),
        AmbientRecording(
            "wind", "باد", "ضبط صدای وزش باد در فضای داخلی · ۲:۰۸",
            "https://commons.wikimedia.org/wiki/Special:Redirect/file/Howling_wind.ogg",
            "https://commons.wikimedia.org/wiki/File:Howling_wind.ogg", "Tvabutzku1234", "02:08", "CC0 1.0",
            "https://creativecommons.org/publicdomain/zero/1.0/",
        ),
        AmbientRecording(
            "waves", "موج‌های دریاچه", "ضبط واقعی امواج کنار دریاچه‌ی انتاریو · ۴:۴۷",
            "https://commons.wikimedia.org/wiki/Special:Redirect/file/Waves.ogg",
            "https://commons.wikimedia.org/wiki/File:Waves.ogg", "Dsw4", "04:47", "Public Domain",
            "https://creativecommons.org/publicdomain/zero/1.0/",
        ),
        AmbientRecording(
            "shore", "موج کنار ساحل", "ضبط نزدیک موج و سنگ‌ریزه‌های ساحل · ۲:۰۰",
            "https://commons.wikimedia.org/wiki/Special:Redirect/file/Oceanwavescrushing.ogg",
            "https://commons.wikimedia.org/wiki/File:Oceanwavescrushing.ogg", "Luftrum", "02:00", "CC BY 3.0",
            "https://creativecommons.org/licenses/by/3.0/",
            "Oceanwavescrushing by Luftrum, CC BY 3.0. Source: https://commons.wikimedia.org/wiki/File:Oceanwavescrushing.ogg",
        ),
    )

    fun byId(id: String): AmbientRecording? = recordings.firstOrNull { it.id == id }
}

class AmbientAssetStore(private val context: Context) {
    private val folder = File(context.filesDir, "audio/ambient")
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    fun localFile(id: String): File? {
        val recording = AmbientLibrary.byId(id) ?: return null
        return File(folder, "${recording.id}.ogg").takeIf { it.isFile && it.length() > 1024L }
    }

    fun isDownloaded(id: String): Boolean = localFile(id) != null
    fun totalBytes(): Long = folder.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }?.sumOf { it.length() } ?: 0L

    suspend fun download(id: String): File = withContext(Dispatchers.IO) {
        val recording = AmbientLibrary.byId(id) ?: throw IllegalArgumentException("صدای محیط پیدا نشد.")
        folder.mkdirs()
        localFile(id)?.let { return@withContext it }
        val target = File(folder, "${recording.id}.ogg")
        val part = File(folder, "${recording.id}.ogg.part")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(recording.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MatFocus-Audio/1.2")
            }
            require(connection.responseCode in 200..299 && connection.url.protocol == "https") {
                "سرور فایل صوتی پاسخ HTTPS معتبر نداد."
            }
            val expected = connection.contentLengthLong
            require(expected <= 0L || expected <= MAX_AUDIO_BYTES) { "حجم فایل صوتی بیش از حد مجاز است." }
            var written = 0L
            connection.inputStream.use { input ->
                part.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        written += count
                        require(written <= MAX_AUDIO_BYTES) { "حجم فایل صوتی از سقف مجاز بیشتر شد." }
                        _progress.value = _progress.value + (id to if (expected > 0L) (written.toFloat() / expected).coerceIn(0f, 1f) else 0f)
                    }
                }
            }
            require(written > 1024L && (expected <= 0L || written == expected)) { "فایل صوتی ناقص دریافت شد." }
            target.delete()
            check(part.renameTo(target)) { "ذخیره‌ی صدای محیط ناموفق بود." }
            _progress.value = _progress.value - id
            target
        } catch (error: Exception) {
            part.delete()
            _progress.value = _progress.value - id
            throw error
        } finally {
            connection?.disconnect()
        }
    }

    fun delete(id: String): Boolean {
        val file = File(folder, "${id}.ogg")
        return !file.exists() || file.delete()
    }

    companion object {
        private const val MAX_AUDIO_BYTES = 60L * 1024L * 1024L
    }
}
