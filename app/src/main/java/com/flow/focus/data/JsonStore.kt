package com.flow.focus.data

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** ذخیره‌سازی دائمی: یک فایل JSON با نوشتن اتمیک (فایل موقت + جابه‌جایی) تا در صورت قطع شدن برنامه خراب نشود. */
class JsonStore(private val file: File) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            json.decodeFromString<AppData>(file.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            // Never overwrite an earlier recovery copy; quarantine the invalid source under a unique name.
            quarantineCorruptFile()
            AppData()
        }
    }

    private fun quarantineCorruptFile() {
        val parent = file.parentFile ?: return
        val backup = File(parent, file.name + ".corrupt-" + System.nanoTime())
        runCatching {
            try {
                Files.move(file.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                file.copyTo(backup, overwrite = false)
                file.delete()
            }
        }
    }

    @Synchronized
    fun save(data: AppData) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(data), Charsets.UTF_8)
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun encode(data: AppData): String = json.encodeToString(data)

    fun decodeOrNull(text: String): AppData? = try {
        json.decodeFromString<AppData>(text)
    } catch (e: Exception) {
        null
    }
}
