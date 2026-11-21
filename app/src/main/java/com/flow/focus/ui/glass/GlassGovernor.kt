package com.flow.focus.ui.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.flow.focus.data.GlassQuality

/**
 * سقف سخت‌افزاری کیفیت Liquid Glass: بدون RenderEffect (قبل از Android 12) یا با RAM کم، کیفیت محدود می‌شود.
 * کاهش خودکار پویا عمداً حذف شد: چند فریم کند گذرا (باز شدن صفحه، دیکود والپیپر) کیفیت را تا بستن
 * برنامه پایین نگه می‌داشت و حتی تغییر تنظیمات هم آن را برنمی‌گرداند.
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

    fun effective(requested: GlassQuality, cap: GlassQuality): GlassQuality =
        GlassQuality.entries[minOf(requested.ordinal, cap.ordinal)]
}
