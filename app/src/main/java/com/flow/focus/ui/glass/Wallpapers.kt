package com.flow.focus.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** ظاهر یک پس‌زمینه‌ی داخلی در یک تم: سه ایستگاه گرادیان عمودی و دو نور رنگی. */
@Immutable
class WallpaperLook(
    val stops: List<Color>,
    val glow: Color,
    val glow2: Color,
    val glowAlpha: Float = 0.36f,
)

/** پس‌زمینه‌ی داخلی؛ برای هر تم (روشن/تاریک) یک طراحی جداگانه دارد، نه یک نسخه‌ی وارونه. */
@Immutable
class BuiltinWallpaper(
    val id: String,
    val label: String,
    val description: String,
    val dark: WallpaperLook,
    val light: WallpaperLook,
    /** اگر true باشد تصویر drawable با نام `<id>_dark` / `<id>_light` رسم می‌شود و look فقط برای پیش‌نمایش و پروفایل است. */
    val art: Boolean = false,
) {
    fun look(isDark: Boolean) = if (isDark) dark else light
}

object BuiltinWallpapers {
    val all: List<BuiltinWallpaper> = listOf(
        BuiltinWallpaper(
            "aurora", "شفق", "طیف آبی و بنفش با نور نرم",
            dark = WallpaperLook(listOf(Color(0xFF07172B), Color(0xFF173B59), Color(0xFF382B65)), Color(0xFF68C8D6), Color(0xFF6B7DD1)),
            light = WallpaperLook(listOf(Color(0xFFE8F4F8), Color(0xFFDCEAF5), Color(0xFFE8E4F4)), Color(0xFF70BFCB), Color(0xFFB9A8E0)),
            art = true,
        ),
        BuiltinWallpaper(
            "midnight", "نیمه‌شب", "رنگ‌های تیره و آرام",
            dark = WallpaperLook(listOf(Color(0xFF070D1B), Color(0xFF172A42), Color(0xFF26364B)), Color(0xFF547DB4), Color(0xFF7D8FA8)),
            light = WallpaperLook(listOf(Color(0xFFE7EDF5), Color(0xFFDCE5F0), Color(0xFFD2DEEC)), Color(0xFF799AC5), Color(0xFFA9B8D0)),
            art = true,
        ),
        BuiltinWallpaper(
            "dusk", "غروب", "ترکیب گرم غروب و آبی تیره",
            dark = WallpaperLook(listOf(Color(0xFF241B37), Color(0xFF69435E), Color(0xFF273D5C)), Color(0xFFD18B9A), Color(0xFF9C739A)),
            light = WallpaperLook(listOf(Color(0xFFF4E8EC), Color(0xFFE9DDE9), Color(0xFFDCE6F2)), Color(0xFFC58BA8), Color(0xFFB6A0D0)),
            art = true,
        ),
        BuiltinWallpaper(
            "ocean", "اقیانوس", "آبی‌سبز عمیق با درخشش آب",
            dark = WallpaperLook(listOf(Color(0xFF04202B), Color(0xFF0B4A5E), Color(0xFF0D2F4F)), Color(0xFF2EC4B6), Color(0xFF3A86FF)),
            light = WallpaperLook(listOf(Color(0xFFE6F7F6), Color(0xFFD5EEF2), Color(0xFFD7E6F7)), Color(0xFF4CC9C0), Color(0xFF7FB2F0)),
            art = true,
        ),
        BuiltinWallpaper(
            "ember", "آتش", "سرخ و نارنجی گرم با نور زغال",
            dark = WallpaperLook(listOf(Color(0xFF2A1410), Color(0xFF6B2E22), Color(0xFF3A1F36)), Color(0xFFFF8A5B), Color(0xFFE5527A)),
            light = WallpaperLook(listOf(Color(0xFFFCEFE6), Color(0xFFF8E1D6), Color(0xFFF3DCE4)), Color(0xFFFFA177), Color(0xFFEE8FA8)),
            art = true,
        ),
        BuiltinWallpaper(
            "forest", "جنگل", "سبز نعنایی با سایه‌ی آبی",
            dark = WallpaperLook(listOf(Color(0xFF0A1F18), Color(0xFF1B4A3A), Color(0xFF1F3347)), Color(0xFF5ED6A0), Color(0xFF7FB77E)),
            light = WallpaperLook(listOf(Color(0xFFEAF6EE), Color(0xFFDDEFE4), Color(0xFFDDE9F0)), Color(0xFF7BD3A8), Color(0xFFA8CF8A)),
            art = true,
        ),
        BuiltinWallpaper(
            "study_desk", "میز مطالعه", "میز مطالعه‌ی شبانه با چراغ و پنجره‌ی ستاره‌ای",
            dark = WallpaperLook(listOf(Color(0xFF0B1226), Color(0xFF1B2A4A), Color(0xFF2B2F55)), Color(0xFFFFB766), Color(0xFF6B7DD1)),
            light = WallpaperLook(listOf(Color(0xFFF4EAD9), Color(0xFFF1DCC3), Color(0xFFE6D0C4)), Color(0xFFFFD9A0), Color(0xFFEE8FA8)),
            art = true,
        ),
        BuiltinWallpaper(
            "study_library", "کتابخانه", "قفسه‌های کتاب با نور گرم",
            dark = WallpaperLook(listOf(Color(0xFF1A1220), Color(0xFF2A1C2E), Color(0xFF2A1C2E)), Color(0xFFFFB36B), Color(0xFF8C4B5A)),
            light = WallpaperLook(listOf(Color(0xFFF5ECDF), Color(0xFFEAD9C6), Color(0xFFEAD9C6)), Color(0xFFFFD08A), Color(0xFFE29A9A)),
            art = true,
        ),
        BuiltinWallpaper(
            "study_rain", "شب بارانی", "باران روی شیشه و نورهای تار شهر",
            dark = WallpaperLook(listOf(Color(0xFF0A0F1F), Color(0xFF141D35), Color(0xFF1C2440)), Color(0xFF7AB8FF), Color(0xFFFF7A9A)),
            light = WallpaperLook(listOf(Color(0xFFDFE8F1), Color(0xFFCDD9E6), Color(0xFFC2CFDF)), Color(0xFFB8DCFF), Color(0xFFFFC4D4)),
            art = true,
        ),
        BuiltinWallpaper(
            "study_hills", "تپه‌های آرام", "تپه و جنگل مه‌آلود برای تمرکز",
            dark = WallpaperLook(listOf(Color(0xFF0D1A2B), Color(0xFF27455A), Color(0xFF0E2227)), Color(0xFFFFD9A0), Color(0xFF2C4A56)),
            light = WallpaperLook(listOf(Color(0xFFCFE6F2), Color(0xFFF6E9D3), Color(0xFF7FAE9F)), Color(0xFFFFF2C7), Color(0xFF9CC2B5)),
            art = true,
        ),
    )

    val ids: Set<String> = all.map { it.id }.toSet()

    fun find(id: String): BuiltinWallpaper? = all.firstOrNull { it.id == id }

    /** شناسه‌های مجاز برای ذخیره‌سازی تنظیمات. */
    val allowedIds: Set<String> = ids + setOf("gradient", "custom")
}

/** تصویر هنری یک پس‌زمینه‌ی داخلی برای تم فعلی؛ null اگر پس‌زمینه تصویری نباشد. */
internal fun loadWallpaperArt(context: android.content.Context, id: String, isDark: Boolean, sample: Int = 1): android.graphics.Bitmap? {
    if (BuiltinWallpapers.find(id)?.art != true) return null
    val res = context.resources
    val resId = res.getIdentifier("${id}_${if (isDark) "dark" else "light"}", "drawable", context.packageName)
    if (resId == 0) return null
    return runCatching { android.graphics.BitmapFactory.decodeResource(res, resId, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }) }.getOrNull()
}

/** رسم پس‌زمینه‌ی داخلی [wallpaper] با طراحی مخصوص تم فعلی. */
internal fun DrawScope.drawLook(look: WallpaperLook, phase: Float, w: Float, h: Float, accentBloom: Color) {
    drawRect(Brush.verticalGradient(look.stops, startY = 0f, endY = h), size = Size(w, h))
    val motion = phase * 2f * PI.toFloat()
    val c1 = Offset(w * (0.78f + 0.04f * cos(motion)), h * (0.2f + 0.015f * sin(motion)))
    drawCircle(
        Brush.radialGradient(listOf(look.glow.copy(alpha = look.glowAlpha), look.glow.copy(alpha = 0f)), center = c1, radius = w * 0.78f),
        radius = w * 0.78f, center = c1,
    )
    val c2 = Offset(w * (0.1f + 0.03f * sin(motion)), h * 0.84f)
    drawCircle(
        Brush.radialGradient(listOf(look.glow2.copy(alpha = look.glowAlpha * 0.72f), Color.Transparent), center = c2, radius = w * 0.72f),
        radius = w * 0.72f, center = c2,
    )
    // نور سوم، بسیار ملایم، در پایین‌ِ وسط؛ عمق افق را می‌سازد
    val c3 = Offset(w * 0.55f, h * 1.02f)
    drawCircle(
        Brush.radialGradient(listOf(accentBloom.copy(alpha = 0.14f), Color.Transparent), center = c3, radius = w * 0.9f),
        radius = w * 0.9f, center = c3,
    )
}
