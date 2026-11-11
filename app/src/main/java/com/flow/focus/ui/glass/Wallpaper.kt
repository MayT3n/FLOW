package com.flow.focus.ui.glass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.flow.focus.data.Settings
import com.flow.focus.data.WallpaperFit
import com.flow.focus.data.WallpaperKind
import com.flow.focus.data.WallpaperStore
import com.flow.focus.ui.theme.FlowColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** والپیپرهای داخلی: به‌صورت برنامه‌نویسی‌شده تولید می‌شوند (بدون فایل تصویری در APK). */
val BuiltinWallpaperNames = listOf("شفق", "غروب", "اقیانوس", "اخگر", "جنگل", "گرافیت")

private class Glow(val x: Float, val y: Float, val r: Float, val color: Long)
private class BuiltinSpec(val base: LongArray, val glows: List<Glow>)

private val builtinSpecs = listOf(
    BuiltinSpec(longArrayOf(0xFF0B1230, 0xFF1B2A72, 0xFF2A2468), listOf(Glow(0.2f, 0.15f, 0.9f, 0xFF3B6CFF), Glow(0.95f, 0.5f, 0.8f, 0xFF7A4DFF), Glow(0.3f, 0.95f, 0.9f, 0xFF22B8F0))),
    BuiltinSpec(longArrayOf(0xFF1A1033, 0xFF4A2250, 0xFF7A3A4A), listOf(Glow(0.8f, 0.2f, 0.9f, 0xFFFF8A5C), Glow(0.1f, 0.6f, 0.8f, 0xFFB04DFF), Glow(0.6f, 0.95f, 0.9f, 0xFFFFB84D))),
    BuiltinSpec(longArrayOf(0xFF031A2B, 0xFF0A3A55, 0xFF0F5A6E), listOf(Glow(0.25f, 0.2f, 0.9f, 0xFF22B8F0), Glow(0.9f, 0.6f, 0.8f, 0xFF1FA9A0), Glow(0.2f, 0.95f, 0.9f, 0xFF3B6CFF))),
    BuiltinSpec(longArrayOf(0xFF1B0D0D, 0xFF3A1A14, 0xFF5A2A1A), listOf(Glow(0.2f, 0.2f, 0.9f, 0xFFFF6B3D), Glow(0.9f, 0.55f, 0.8f, 0xFFFFB84D), Glow(0.4f, 0.95f, 0.9f, 0xFFC2410C))),
    BuiltinSpec(longArrayOf(0xFF07150F, 0xFF0F2E22, 0xFF174030), listOf(Glow(0.25f, 0.2f, 0.9f, 0xFF3DDC97), Glow(0.9f, 0.5f, 0.8f, 0xFF1FA971), Glow(0.3f, 0.95f, 0.9f, 0xFF7FD6B0))),
    BuiltinSpec(longArrayOf(0xFF0E0F12, 0xFF1A1C22, 0xFF272A33), listOf(Glow(0.2f, 0.15f, 0.9f, 0xFF5A6070), Glow(0.95f, 0.55f, 0.8f, 0xFF3A3F4C), Glow(0.3f, 0.95f, 0.9f, 0xFF6B7285))),
)

private fun argb(c: Long, alpha: Int = 255): Int = (c.toInt() and 0x00FFFFFF) or (alpha shl 24)

fun renderBuiltinWallpaper(index: Int, w: Int, h: Int): Bitmap {
    val spec = builtinSpecs[index.coerceIn(0, builtinSpecs.lastIndex)]
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(bmp)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    p.shader = android.graphics.LinearGradient(0f, 0f, 0f, h.toFloat(), IntArray(spec.base.size) { argb(spec.base[it]) }, null, Shader.TileMode.CLAMP)
    cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    for (g in spec.glows) {
        val r = g.r * w
        p.shader = RadialGradient(g.x * w, g.y * h, r, intArrayOf(argb(g.color, 150), argb(g.color, 0)), null, Shader.TileMode.CLAMP)
        cv.drawCircle(g.x * w, g.y * h, r, p)
    }
    return bmp
}

class PreparedWallpaper(val image: ImageBitmap, val luma: Float, val smooth: Boolean)

/** بارگذاری و کش والپیپر: هر تصویر یک بار دیکد می‌شود؛ تغییر شدت تاری فقط نسخه‌ی کوچک‌شده را دوباره می‌سازد. */
object WallpaperLoader {
    private var srcKey: String? = null
    private var srcBmp: Bitmap? = null
    private var lastKey: String? = null
    private var last: PreparedWallpaper? = null

    @Synchronized
    fun prepare(ctx: Context, kind: WallpaperKind, builtin: Int, rev: Long, blurLevel: Int): PreparedWallpaper? {
        val key = "$kind:$builtin:$rev:$blurLevel"
        if (key == lastKey) return last
        return try {
            val sKey = "$kind:$builtin:$rev"
            var src = if (sKey == srcKey) srcBmp else null
            if (src == null) {
                src = when (kind) {
                    WallpaperKind.BUILTIN -> renderBuiltinWallpaper(builtin, 720, 1560)
                    WallpaperKind.CUSTOM -> BitmapFactory.decodeFile(WallpaperStore(ctx).fileFor(rev).path)
                    WallpaperKind.GRADIENT -> null
                }
                srcBmp = src
                srcKey = if (src != null) sKey else null
            }
            if (src == null) {
                null
            } else {
                val luma = averageLuma(src)
                val out = if (blurLevel <= 0) src else blurred(src, blurLevel)
                val p = PreparedWallpaper(out.asImageBitmap(), luma, blurLevel > 0)
                lastKey = key
                last = p
                p
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun averageLuma(b: Bitmap): Float {
        val s = Bitmap.createScaledBitmap(b, 8, 8, true)
        var sum = 0f
        for (y in 0 until 8) for (x in 0 until 8) {
            val c = s.getPixel(x, y)
            sum += (0.2126f * android.graphics.Color.red(c) + 0.7152f * android.graphics.Color.green(c) + 0.0722f * android.graphics.Color.blue(c)) / 255f
        }
        if (s !== b) s.recycle()
        return sum / 64f
    }

    /** تاری ارزان و بدون پردازش هر فریم: کوچک‌سازی تدریجی (هرم گاوسی تقریبی)؛ بزرگ‌نمایی بعداً روی GPU با کیفیت High. */
    private fun blurred(src: Bitmap, level: Int): Bitmap {
        val tw = (src.width / (1f + level * 1.6f)).toInt().coerceAtLeast(24)
        val th = max(1, (src.height.toLong() * tw / src.width).toInt())
        var cur = src
        var w = src.width
        var h = src.height
        while (w / 2 > tw) {
            w /= 2
            h /= 2
            val n = Bitmap.createScaledBitmap(cur, max(w, 1), max(h, 1), true)
            if (cur !== src) cur.recycle()
            cur = n
        }
        val n = Bitmap.createScaledBitmap(cur, tw, th, true)
        if (cur !== src && cur !== n) cur.recycle()
        return n
    }
}

@Immutable
class WallpaperImage(val bitmap: ImageBitmap?, val luma: Float, val smooth: Boolean)

/** وضعیت مشترک والپیپر؛ ریشه و همه‌ی سطوح شیشه از همین یک منبع می‌خوانند. */
@Stable
class WallpaperState {
    var current by mutableStateOf(WallpaperImage(null, -1f, false))
    var previous by mutableStateOf<WallpaperImage?>(null)
    var progress by mutableFloatStateOf(1f)
    var fit by mutableStateOf(WallpaperFit.CROP)
    var filter by mutableStateOf<ColorFilter?>(null)
    var brightness by mutableFloatStateOf(0f)

    val usesGradient: Boolean
        get() = current.bitmap == null || (previous != null && previous?.bitmap == null)

    /** روشنایی میانگین مؤثر (۰..۱) پس از تنظیمات کاربر. */
    fun luma(dark: Boolean): Float {
        val c = current
        return if (c.bitmap == null) (if (dark) 0.14f else 0.88f)
        else (c.luma * (1f + brightness) + max(brightness, 0f) * 0.2f).coerceIn(0f, 1f)
    }

    suspend fun transitionTo(next: WallpaperImage, animated: Boolean) {
        if (!animated) {
            previous = null
            current = next
            progress = 1f
            return
        }
        previous = current
        current = next
        progress = 0f
        animate(0f, 1f, animationSpec = tween(480)) { v, _ -> progress = v }
        previous = null
        progress = 1f
    }
}

fun wallpaperFilter(brightness: Float, saturation: Float): ColorFilter? {
    if (abs(brightness) < 0.005f && abs(saturation - 1f) < 0.005f) return null
    val k = 1f + brightness
    val o = max(brightness, 0f) * 60f
    val s = saturation
    val ir = 0.2126f * (1 - s)
    val ig = 0.7152f * (1 - s)
    val ib = 0.0722f * (1 - s)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                k * (ir + s), k * ig, k * ib, 0f, o,
                k * ir, k * (ig + s), k * ib, 0f, o,
                k * ir, k * ig, k * (ib + s), 0f, o,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

@Composable
fun rememberWallpaperState(st: Settings): WallpaperState {
    val app = LocalContext.current.applicationContext
    val state = remember { WallpaperState() }
    val level = (st.wallpaperBlur / 10).coerceIn(0, 10)
    val key = "${st.wallpaperKind}:${st.wallpaperBuiltin}:${st.wallpaperRev}:$level"
    val animations by rememberUpdatedState(st.animations)
    val first = remember { booleanArrayOf(true) }
    val filter = remember(st.wallpaperBrightness, st.wallpaperSaturation) { wallpaperFilter(st.wallpaperBrightness, st.wallpaperSaturation) }

    SideEffect {
        state.fit = st.wallpaperFit
        state.filter = filter
        state.brightness = st.wallpaperBrightness
    }
    LaunchedEffect(key) {
        val p = if (st.wallpaperKind == WallpaperKind.GRADIENT) null
        else withContext(Dispatchers.Default) { WallpaperLoader.prepare(app, st.wallpaperKind, st.wallpaperBuiltin, st.wallpaperRev, level) }
        val img = if (p == null) WallpaperImage(null, -1f, false) else WallpaperImage(p.image, p.luma, p.smooth)
        val animated = animations && !first[0]
        first[0] = false
        state.transitionTo(img, animated)
    }
    return state
}

/** رسم والپیپر (یا گرادیان پیش‌فرض) در مختصات [w]×[h]؛ هم ریشه و هم بک‌دراپ شیشه‌ها از همین تابع استفاده می‌کنند. */
fun DrawScope.drawWallpaper(ws: WallpaperState, c: FlowColors, phase: Float, w: Float, h: Float) {
    val prev = ws.previous
    val p = ws.progress
    if (prev != null && p < 1f) {
        drawWallpaperImage(prev, ws, c, phase, w, h, 1f)
        drawWallpaperImage(ws.current, ws, c, phase, w, h, p)
    } else {
        drawWallpaperImage(ws.current, ws, c, phase, w, h, 1f)
    }
}

private fun DrawScope.drawWallpaperImage(img: WallpaperImage, ws: WallpaperState, c: FlowColors, phase: Float, w: Float, h: Float, alpha: Float) {
    val bmp = img.bitmap
    if (bmp == null) {
        if (alpha >= 0.999f) {
            drawFlowBackground(c, phase, w, h)
        } else {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply { this.alpha = alpha }
                canvas.saveLayer(Rect(0f, 0f, w, h), paint)
                drawFlowBackground(c, phase, w, h)
                canvas.restore()
            }
        }
        return
    }
    val iw = bmp.width.toFloat()
    val ih = bmp.height.toFloat()
    val crop = ws.fit == WallpaperFit.CROP
    val scale = if (crop) max(w / iw, h / ih) else min(w / iw, h / ih)
    val dw = iw * scale
    val dh = ih * scale
    if (!crop) drawFlowBackground(c, 0f, w, h)
    drawImage(
        bmp,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bmp.width, bmp.height),
        dstOffset = IntOffset(floor((w - dw) / 2f).toInt(), floor((h - dh) / 2f).toInt()),
        dstSize = IntSize(ceil(dw).toInt() + 1, ceil(dh).toInt() + 1),
        alpha = alpha,
        colorFilter = ws.filter,
        filterQuality = if (img.smooth) FilterQuality.High else FilterQuality.Medium,
    )
    // لایه‌ی ملایم خوانایی؛ مشترک بین ریشه و بک‌دراپ شیشه‌ها
    drawRect(Color.Black.copy(alpha = (if (c.isDark) 0.22f else 0.04f) * alpha))
}
