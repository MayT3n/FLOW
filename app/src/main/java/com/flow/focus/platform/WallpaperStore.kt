package com.flow.focus.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Keeps one optimized wallpaper copy in app-private storage.
 * No broad storage permission or Base64 settings payload is required.
 */
class WallpaperStore(context: Context) {
    private val app = context.applicationContext
    private val directory = File(app.filesDir, "wallpapers")

    fun import(uri: Uri): String? = runCatching {
        if (!directory.exists() && !directory.mkdirs()) error("Cannot create wallpaper directory")
        val bitmap = decode(uri) ?: error("Unsupported image")
        try {
            val target = File(directory, "wallpaper-" + UUID.randomUUID().toString() + ".jpg")
            val temp = File(directory, "wallpaper-" + UUID.randomUUID().toString() + ".tmp")
            FileOutputStream(temp).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) error("Image compression failed")
                output.fd.sync()
            }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            directory.listFiles()
                ?.filter { it.isFile && it.name.startsWith("wallpaper-") && it != target }
                ?.forEach { it.delete() }
            target.absolutePath
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()

    private fun decode(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(app.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width.coerceAtLeast(1)
                val height = info.size.height.coerceAtLeast(1)
                val scale = minOf(1f, MAX_DIMENSION.toFloat() / maxOf(width, height).toFloat())
                decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_DIMENSION) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    companion object {
        private const val MAX_DIMENSION = 2048
    }
}
