package com.flow.desktop

import java.io.File
import java.util.Locale

/** Optional per-user Windows startup entry. Never writes machine-wide registry keys. */
object WindowsStartup {
    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val VALUE_NAME = "MedFlow"

    fun isSupported(): Boolean = System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")

    fun executablePath(): String? {
        if (!isSupported()) return null
        val packaged = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }?.let(::File)
        if (packaged?.isFile == true) return packaged.absolutePath
        val local = File(System.getProperty("user.dir"), "MedFlow.exe")
        return local.takeIf { it.isFile }?.absolutePath
    }

    fun isEnabled(): Boolean {
        if (!isSupported()) return false
        return runCatching {
            val process = ProcessBuilder("reg.exe", "query", RUN_KEY, "/v", VALUE_NAME)
                .redirectErrorStream(true).start()
            process.inputStream.use { it.readBytes() }
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    fun setEnabled(enabled: Boolean): Boolean {
        if (!isSupported()) return false
        return runCatching {
            val command = if (enabled) {
                val executable = executablePath() ?: return false
                ProcessBuilder("reg.exe", "add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", "\"$executable\"", "/f")
            } else {
                ProcessBuilder("reg.exe", "delete", RUN_KEY, "/v", VALUE_NAME, "/f")
            }
            val process = command.redirectErrorStream(true).start()
            process.inputStream.use { it.readBytes() }
            process.waitFor() == 0
        }.getOrDefault(false)
    }
}
