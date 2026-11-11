package com.flow.focus.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.max

/**
 * کپی بهینه‌ی والپیپر انتخابی در حافظه‌ی خصوصی برنامه (نه JSON). فقط فایل‌های خود والپیپر مدیریت می‌شوند.
 */
class WallpaperStore(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "wallpaper")

    fun fileFor(rev: Long): File = File(dir, "custom_$rev.jpg")

    /** تصویر را کوچک و اصلاح‌جهت می‌کند و ذخیره می‌کند؛ شماره‌ی نسخه‌ی جدید یا null. */
    fun import(uri: Uri): Long? = try {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (bmp == null) {
                null
            } else {
                val rotation = cr.openInputStream(uri)?.use { exifRotation(it) } ?: 0
                val longest = max(bmp.width, bmp.height)
                if (longest > MAX_EDGE) {
                    val k = MAX_EDGE.toFloat() / longest
                    bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true)
                }
                if (rotation != 0) {
                    val m = Matrix().apply { postRotate(rotation.toFloat()) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                }
                dir.mkdirs()
                val rev = System.currentTimeMillis()
                val tmp = File(dir, "tmp_$rev")
                tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                val dst = fileFor(rev)
                if (tmp.renameTo(dst)) {
                    prune(keep = rev)
                    rev
                } else {
                    tmp.delete()
                    null
                }
            }
        }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }

    /** همه‌ی کپی‌های والپیپر به‌جز [keep] (در صورت null، همه) حذف می‌شوند. */
    fun prune(keep: Long?) {
        dir.listFiles()?.forEach { f -> if (keep == null || f.name != "custom_$keep.jpg") f.delete() }
    }

    private fun exifRotation(stream: java.io.InputStream): Int = try {
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    private companion object {
        const val MAX_EDGE = 2400
    }
}
