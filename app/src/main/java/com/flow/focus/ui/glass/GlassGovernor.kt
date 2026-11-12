package com.flow.focus.ui.glass

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.flow.focus.data.GlassQuality

/**
 * کاهش هوشمند کیفیت Liquid Glass.
 *  1) سقف سخت‌افزاری: بدون RenderEffect (قبل از Android 12) یا RAM کم، کیفیت محدود می‌شود.
 *  2) سقف پویا: اگر فریم‌های سنگین پشت‌سرهم دیده شود، یک پله کیفیت کم می‌شود (تا پایان اجرای برنامه).
 */
object GlassGovernor {
    fun deviceCap(context: Context): GlassQuality {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return GlassQuality.MEDIUM
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> GlassQuality.LOW
            am.isLowRamDevice -> GlassQuality.LOW
            am.memoryClass < 192 -> GlassQuality.MEDIUM
            else -> GlassQuality.HIGH
        }
    }

    fun effective(requested: GlassQuality, cap: GlassQuality, degrade: Int): GlassQuality {
        val level = (minOf(requested.ordinal, cap.ordinal) - degrade).coerceAtLeast(0)
        return GlassQuality.entries[level]
    }
}

private const val WINDOW_FRAMES = 60
private const val JANK_FRAME_NS = 33_000_000L
private const val BAD_WINDOW_RATIO = 0.4f
private const val BAD_WINDOWS_TO_DEGRADE = 2

/** تعداد پله‌هایی که کیفیت باید خودکار کم شود. فقط وقتی فریم رسم می‌شود نمونه می‌گیرد، پس در حالت بیکار هزینه‌ای ندارد. */
@Composable
fun rememberGlassDegradeLevel(enabled: Boolean): State<Int> {
    val level = remember { mutableIntStateOf(0) }
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (window == null || !enabled) return@DisposableEffect onDispose { }
        var frames = 0
        var janky = 0
        var badWindows = 0
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            frames++
            if (total > JANK_FRAME_NS) janky++
            if (frames >= WINDOW_FRAMES) {
                val bad = janky.toFloat() / frames >= BAD_WINDOW_RATIO
                badWindows = if (bad) badWindows + 1 else 0
                if (badWindows >= BAD_WINDOWS_TO_DEGRADE && level.intValue < 2) {
                    level.intValue += 1
                    badWindows = 0
                }
                frames = 0
                janky = 0
            }
        }
        window.addOnFrameMetricsAvailableListener(listener, Handler(Looper.getMainLooper()))
        onDispose { runCatching { window.removeOnFrameMetricsAvailableListener(listener) } }
    }
    return level
}
