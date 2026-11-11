package com.flow.focus.remote

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    private val downloadMutex = Mutex()
    @Volatile private var readyManifest: UpdateManifest? = null

    suspend fun download(manifest: UpdateManifest): File = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            validateManifestForInstalledApp(manifest)
            val directory = File(context.filesDir, "updates")
            check((directory.isDirectory || directory.mkdirs()) && directory.isDirectory) {
                "ساخت پوشه‌ی دانلود آپدیت ناموفق بود."
            }
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
                    setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream")
                }
                require(connection.responseCode in 200..299 && connection.url.protocol == "https") {
                    "سرور دانلود پاسخ معتبر HTTPS نداد."
                }
                val responseLength = connection.contentLengthLong
                require(responseLength <= 0L || responseLength == manifest.fileSize) {
                    "حجم فایل با Manifest یکسان نیست."
                }

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
                            require(total <= manifest.fileSize && total <= RemoteContentValidation.MAX_APK_BYTES) {
                                "حجم فایل دانلودشده از مقدار مجاز بیشتر است."
                            }
                            _state.value = UpdateDownloadState.Downloading(
                                (total.toFloat() / manifest.fileSize.toFloat()).coerceIn(0f, 1f),
                            )
                        }
                    }
                }
                require(total == manifest.fileSize) { "دانلود ناقص است." }
                val expectedHash = manifest.sha256.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                require(MessageDigest.isEqual(digest.digest(), expectedHash)) {
                    "بررسی SHA-256 ناموفق بود."
                }

                verifyPackageAndSignature(temporary, manifest.versionCode, manifest.versionName)
                try {
                    Files.move(
                        temporary.toPath(),
                        target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: Exception) {
                    Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                readyManifest = manifest
                _state.value = UpdateDownloadState.Ready(target)
                target
            } catch (cancelled: CancellationException) {
                temporary.delete()
                readyManifest = null
                _state.value = UpdateDownloadState.Idle
                throw cancelled
            } catch (error: Exception) {
                temporary.delete()
                readyManifest = null
                _state.value = UpdateDownloadState.Failed(error.message ?: "دانلود یا اعتبارسنجی ناموفق بود.")
                throw error
            } finally {
                connection?.disconnect()
            }
        }
    }

    /** Revalidates the APK on IO, then opens Android's installer on the main thread. */
    suspend fun installDownloaded(): Boolean {
        try {
            val file = (state.value as? UpdateDownloadState.Ready)?.file
                ?: throw IllegalStateException("ابتدا باید فایل آپدیت با موفقیت دانلود شود.")
            val manifest = readyManifest
                ?: throw IllegalStateException("اطلاعات اعتبارسنجی آپدیت در دسترس نیست.")

            withContext(Dispatchers.IO) {
                check(file.isFile && file.length() == manifest.fileSize) {
                    "فایل آپدیت پیدا نشد یا ناقص است."
                }
                validateManifestForInstalledApp(manifest)
                verifyPackageAndSignature(file, manifest.versionCode, manifest.versionName)
                val actualHash = file.inputStream().use { input ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                    digest.digest()
                }
                val expectedHash = manifest.sha256.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                check(MessageDigest.isEqual(actualHash, expectedHash)) {
                    "فایل آپدیت پس از دانلود تغییر کرده است."
                }
            }

            return withContext(Dispatchers.Main.immediate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settingsIntent)
                    return@withContext false
                }

                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = contentUri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                }
                context.startActivity(installIntent)
                true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.value = UpdateDownloadState.Failed(error.message ?: "اعتبارسنجی یا شروع نصب ناموفق بود.")
            throw error
        }
    }

    private fun validateManifestForInstalledApp(manifest: UpdateManifest) {
        val installedCode = installedVersionCode()
        require(manifest.schemaVersion == 1 && manifest.updateAvailable) {
            "نسخه‌ی قابل دریافت اعلام نشده است."
        }
        require(manifest.versionCode > installedCode) {
            "این نسخه از برنامه جدیدتر نیست."
        }
        require(manifest.minimumSupportedVersion <= installedCode) {
            "نسخه‌ی فعلی برای این آپدیت بیش از حد قدیمی است؛ باید نسخه‌ی میانی نصب شود."
        }
        require(manifest.applicationId == context.packageName) {
            "شناسه‌ی برنامه با نسخه‌ی نصب‌شده یکسان نیست."
        }
        require(manifest.minimumSupportedVersion >= 0 && manifest.minimumSupportedVersion <= manifest.versionCode) {
            "حداقل نسخه‌ی پشتیبانی‌شده در Manifest معتبر نیست."
        }
        require(RemoteContentValidation.isValidManifest(manifest)) {
            "Manifest آپدیت معتبر نیست."
        }
    }

    private fun installedVersionCode(): Long {
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        return versionCode(installed)
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
        require(versionCode(archive) == expectedVersionCode.toLong()) {
            "شماره‌ی نسخه‌ی APK با Manifest تطابق ندارد."
        }
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
