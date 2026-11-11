package com.flow.focus.remote

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.flow.focus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progress: Float) : UpdateDownloadState
    data class Ready(val file: File) : UpdateDownloadState
    data class Failed(val message: String) : UpdateDownloadState
}

/**
 * Download and validate an APK before asking Android's Package Installer to perform an in-place update.
 * No silent install is attempted. The platform confirmation screen remains mandatory for ordinary apps.
 */
class UpdateManager(private val context: Context) {
    private val _state = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val state: StateFlow<UpdateDownloadState> = _state.asStateFlow()

    suspend fun download(manifest: UpdateManifest): File = withContext(Dispatchers.IO) {
        require(manifest.schemaVersion == 1 && manifest.updateAvailable) { "نسخه‌ی قابل دریافت اعلام نشده است." }
        require(manifest.versionCode > BuildConfig.VERSION_CODE) { "این نسخه از برنامه جدیدتر نیست." }
        require(manifest.minimumSupportedVersion <= BuildConfig.VERSION_CODE) { "نسخه‌ی فعلی برای این آپدیت بیش از حد قدیمی است؛ باید نسخه‌ی میانی نصب شود." }
        require(manifest.applicationId == context.packageName) { "شناسه‌ی برنامه با نسخه‌ی نصب‌شده یکسان نیست." }
        require(manifest.minimumSupportedVersion >= 0 && manifest.minimumSupportedVersion <= manifest.versionCode) {
            "حداقل نسخه‌ی پشتیبانی‌شده در Manifest معتبر نیست."
        }
        require(manifest.downloadUrl.startsWith("https://")) { "لینک دانلود باید HTTPS باشد." }
        require(manifest.fileSize > 0L) { "حجم فایل در Manifest معتبر نیست." }
        require(manifest.sha256.matches(Regex("(?i)^[0-9a-f]{64}$"))) { "SHA-256 در Manifest معتبر نیست." }

        val directory = File(context.filesDir, "updates").apply { mkdirs() }
        val temporary = File(directory, "MatFocus-${manifest.versionCode}.apk.part")
        val target = File(directory, "MatFocus-${manifest.versionCode}.apk")
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(manifest.downloadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MatFocus-Updater/1.2")
            }
            require(connection.responseCode in 200..299 && connection.url.protocol == "https") {
                "سرور دانلود پاسخ معتبر HTTPS نداد."
            }
            val responseLength = connection.contentLengthLong
            require(responseLength <= 0L || responseLength == manifest.fileSize) { "حجم فایل با Manifest یکسان نیست." }

            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        total += count
                        _state.value = UpdateDownloadState.Downloading((total.toFloat() / manifest.fileSize).coerceIn(0f, 1f))
                        require(total <= manifest.fileSize) { "حجم فایل دانلودشده از مقدار اعلام‌شده بیشتر است." }
                    }
                }
            }
            require(total == manifest.fileSize) { "دانلود ناقص است." }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            require(actualHash.equals(manifest.sha256, ignoreCase = true)) { "بررسی SHA-256 ناموفق بود." }

            verifyPackageAndSignature(temporary, manifest.versionCode, manifest.versionName)
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) { "ذخیره‌ی فایل دانلودشده ناموفق بود." }
            _state.value = UpdateDownloadState.Ready(target)
            target
        } catch (error: Exception) {
            temporary.delete()
            _state.value = UpdateDownloadState.Failed(error.message ?: "دانلود یا اعتبارسنجی ناموفق بود.")
            throw error
        } finally {
            connection?.disconnect()
        }
    }

    /** Returns true if the installer was opened, false when the user must first allow this source. */
    fun installDownloaded(): Boolean {
        val file = (state.value as? UpdateDownloadState.Ready)?.file
            ?: throw IllegalStateException("ابتدا باید فایل آپدیت با موفقیت دانلود شود.")
        check(file.isFile && file.length() > 0L) { "فایل آپدیت پیدا نشد." }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return false
        }

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = contentUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        context.startActivity(installIntent)
        return true
    }

    private fun verifyPackageAndSignature(apk: File, expectedVersionCode: Int, expectedVersionName: String) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: throw IllegalArgumentException("فایل دانلودشده APK معتبر نیست.")
        require(archive.packageName == context.packageName) { "این APK متعلق به برنامه‌ی دیگری است." }
        require(versionCode(archive) == expectedVersionCode.toLong()) { "شماره‌ی نسخه‌ی APK با Manifest تطابق ندارد." }
        require(archive.versionName == expectedVersionName) { "نام نسخه‌ی APK با Manifest تطابق ندارد." }
        val minimumSdk = archive.applicationInfo?.minSdkVersion ?: 1
        require(Build.VERSION.SDK_INT >= minimumSdk) { "این APK با نسخه‌ی اندروید دستگاه سازگار نیست." }

        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val downloadedCertificates = certificateDigests(archive)
        val installedCertificates = certificateDigests(installed)
        require(downloadedCertificates.isNotEmpty() && downloadedCertificates == installedCertificates) {
            "امضای APK با نسخه‌ی نصب‌شده یکسان نیست؛ برای جلوگیری از حذف اطلاعات، نصب متوقف شد."
        }
    }

    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    private fun certificateDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }
}
