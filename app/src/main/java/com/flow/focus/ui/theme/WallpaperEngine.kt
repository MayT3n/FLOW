package com.flow.focus.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.flow.focus.ui.glass.BuiltinWallpapers
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * موتور سازگاری با والپیپر.
 *
 * ۱) [WallpaperProfile]: روشنایی میانگین، روشنایی نقاط روشن و رنگ غالب والپیپر (از نمونه‌ی کوچک، نه تصویر کامل).
 * ۲) [WallpaperPlan]: کمترین پرده‌ی روشن/تیره و کمترین تقویت شیشه که خوانایی متن را تضمین می‌کند
 *    (کنتراست ثانویه ≥ ۳٫۶ در تاریک و ≥ ۳٫۸ در روشن). برای والپیپرهای آرام هیچ‌چیز اضافه نمی‌شود.
 * ۳) [adaptColors]: متن، شیشه، لهجه‌ی رنگی و سایه را با رنگ غالب والپیپر هماهنگ می‌کند.
 */
@Immutable
data class WallpaperProfile(
    /** میانگین روشنایی ادراکی (۰..۱) در فضای gamma، همان فضایی که اندروید در آن ترکیب می‌کند. */
    val meanP: Float,
    /** صدک ۸۵ روشنایی؛ نقاط روشن که متن روی آن‌ها سخت‌ترین حالت را دارد. */
    val hiP: Float,
    /** رنگ غالب با اشباع؛ Unspecified یعنی والپیپر خنثی است. */
    val dominant: Color,
    /** انحراف معیار روشنایی (۰..۰٫۵): تصویر شلوغ‌تر یعنی متن به شیشه‌ی پرتر و تارتر نیاز دارد. */
    val busy: Float = 0f,
)

@Immutable
data class WallpaperPlan(
    val scrimColor: Color,
    val scrimAlpha: Float,
    val boostColor: Color,
    val boostAlpha: Float,
    /** روشنایی نسبی (WCAG) سطح شیشه‌ای پس از اعمال پرده و تقویت؛ مبنای کنتراست رنگ‌های لهجه. */
    val effectiveY: Float,
    /** ضریب تاری پس‌زمینه‌ی شیشه (۱ = عادی)؛ تصویر شلوغ تارتر می‌شود. */
    val blurScale: Float = 1f,
) {
    companion object {
        val None = WallpaperPlan(Color.Black, 0f, Color.Black, 0f, 0.1f)
    }
}

/** پلان فعلی به‌صورت State تا تغییر پرده فقط Draw را باطل کند، نه Composition را. */
val LocalWallpaperPlan = compositionLocalOf<State<WallpaperPlan>> { mutableStateOf(WallpaperPlan.None) }

internal fun Color.perceptual(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

private fun channelLinear(v: Float): Float = if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)

internal fun Color.relativeLuminance(): Float =
    0.2126f * channelLinear(red) + 0.7152f * channelLinear(green) + 0.0722f * channelLinear(blue)

private fun yFromP(p: Float): Float = p.coerceIn(0f, 1f).pow(2.2f)

private fun contrast(a: Float, b: Float): Float = (max(a, b) + 0.05f) / (min(a, b) + 0.05f)

/** پروفایل پس‌زمینه‌های داخلی از روی تعریفشان؛ بدون رسم و بدون نمونه‌برداری. */
fun builtinProfile(id: String, base: FlowColors): WallpaperProfile {
    val wallpaper = BuiltinWallpapers.find(id)
    if (wallpaper == null) {
        val mean = (base.bgTop.perceptual() * 0.3f + base.bgMid.perceptual() * 0.4f + base.bgBottom.perceptual() * 0.3f)
        return WallpaperProfile(mean, max(mean, base.bgBottom.perceptual()), Color.Unspecified)
    }
    val look = wallpaper.look(base.isDark)
    val p = look.stops.map { it.perceptual() }
    val mean = p[0] * 0.3f + p[1] * 0.4f + p[2] * 0.3f
    val glowHi = 0.5f * look.glow.perceptual() + 0.5f * mean
    return WallpaperProfile(mean, max(p.max(), glowHi), look.glow)
}

/** پروفایل تصویر شخصی از یک شبکه‌ی ۲۰×۲۰ نمونه. */
fun sampleProfile(bitmap: Bitmap): WallpaperProfile {
    val n = 20
    val values = ArrayList<Float>(n * n)
    var bestScore = 0f
    var best = Color.Unspecified
    val hsv = FloatArray(3)
    for (iy in 0 until n) {
        for (ix in 0 until n) {
            val x = ((ix + 0.5f) / n * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val y = ((iy + 0.5f) / n * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val color = Color(bitmap.getPixel(x, y))
            values += color.perceptual()
            android.graphics.Color.colorToHSV(color.toArgb(), hsv)
            if (hsv[1] > 0.22f && hsv[2] in 0.2f..0.95f) {
                val score = hsv[1] * (1f - kotlin.math.abs(hsv[2] - 0.6f))
                if (score > bestScore) { bestScore = score; best = color }
            }
        }
    }
    // نواحی: بالا/وسط/پایین جدا سنجیده می‌شوند و بدترین ناحیه (روشن‌ترین نقاط) مبنای خوانایی است، نه میانگین کل.
    val third = n / 3
    var worstBand = 0f
    for (band in 0 until 3) {
        val slice = values.subList(band * third * n, ((band + 1) * third * n).coerceAtMost(values.size)).sorted()
        if (slice.isNotEmpty()) worstBand = max(worstBand, slice[(slice.size * 0.85f).toInt().coerceIn(0, slice.lastIndex)])
    }
    val mean = values.average().toFloat()
    val variance = values.fold(0f) { acc, v -> acc + (v - mean) * (v - mean) } / values.size
    values.sort()
    val hi = max(values[(values.size * 0.85f).toInt().coerceIn(0, values.lastIndex)], worstBand)
    return WallpaperProfile(mean, hi, best, busy = kotlin.math.sqrt(variance))
}

/**
 * کمترین ترکیب «پرده‌ی والپیپر + تقویت شیشه» که کنتراست متن ثانویه را به حد هدف می‌رساند.
 * در تم تاریک پرده سیاه و در تم روشن سفید است؛ [baseDim] کفِ تنظیم کاربر برای تیرگی (فقط تاریک) است.
 */
fun planFor(profile: WallpaperProfile, base: FlowColors, baseDim: Float): WallpaperPlan {
    val dark = base.isDark
    val lb = 0.65f * profile.meanP + 0.35f * profile.hiP
    val fa = base.glassFill.alpha
    val pf = base.glassFill.perceptual()
    val pc = if (dark) 0f else 1f
    val inkY = base.textSecondary.relativeLuminance()
    val minContrast = if (dark) 3.6f else 3.8f
    val s0 = if (dark) baseDim.coerceIn(0f, 0.65f) else 0f
    val sMax = if (dark) max(s0, 0.5f) else 0.42f
    val gMax = if (dark) 0.30f else 0.38f

    fun bgP(s: Float, g: Float): Float {
        val p1 = lb * (1f - s) + s * pc
        val p2 = p1 * (1f - fa) + fa * pf
        return p2 * (1f - g) + g * pc
    }

    var bestS = sMax
    var bestG = gMax
    var bestCost = Float.MAX_VALUE
    var s = s0
    while (s <= sMax + 0.0001f) {
        var g = 0f
        while (g <= gMax + 0.0001f) {
            if (contrast(inkY, yFromP(bgP(s, g))) >= minContrast) {
                val cost = (s - s0) + 1.3f * g
                if (cost < bestCost) { bestCost = cost; bestS = s; bestG = g }
                break
            }
            g += 0.04f
        }
        s += 0.025f
    }
    val boostColor = if (dark) Color.Black else Color.White
    // تصویر شلوغ: حتی اگر کنتراست میانگین کافی باشد، کمی شیشه‌ی پرتر و تارتر لازم است.
    val busyBoost = ((profile.busy - 0.12f) * 1.2f).coerceIn(0f, 0.2f)
    val g = max(bestG, busyBoost)
    return WallpaperPlan(boostColor, bestS, boostColor, g, yFromP(bgP(bestS, g)), blurScale = (1f + profile.busy * 1.4f).coerceAtMost(1.7f))
}

private fun ensureContrast(color: Color, bgY: Float, minContrast: Float, lighten: Boolean): Color {
    var c = color
    repeat(14) {
        if (contrast(c.relativeLuminance(), bgY) >= minContrast) return c
        c = lerp(c, if (lighten) Color.White else Color.Black, 0.08f)
    }
    return c
}

private fun matchHue(accent: Color, dominant: Color): Color {
    val a = FloatArray(3)
    val d = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), a)
    android.graphics.Color.colorToHSV(dominant.toArgb(), d)
    a[0] = d[0]
    a[1] = (a[1] * 0.6f + d[1] * 0.4f).coerceIn(0.28f, 0.85f)
    return Color(android.graphics.Color.HSVToColor(a))
}

/** پالت پایه را با والپیپر هماهنگ می‌کند؛ خروجی همان [FlowColors] است و کل UI خودکار از آن استفاده می‌کند. */
fun adaptColors(base: FlowColors, profile: WallpaperProfile, plan: WallpaperPlan): FlowColors {
    val dark = base.isDark
    val d = profile.dominant
    val hasHue = d != Color.Unspecified

    fun tintInk(ink: Color, amount: Float): Color {
        if (!hasHue) return ink
        val target = if (dark) lerp(Color.White, d, 0.28f) else lerp(Color(0xFF0B1220), d, 0.20f)
        return lerp(ink, target, amount).copy(alpha = ink.alpha)
    }

    val boost = plan.boostColor.copy(alpha = plan.boostAlpha)
    fun glass(color: Color, target: Color, amount: Float): Color {
        val tinted = if (hasHue) lerp(color, target, amount).copy(alpha = color.alpha) else color
        return if (plan.boostAlpha > 0.001f) boost.compositeOver(tinted) else tinted
    }

    val fillTarget = if (hasHue) (if (dark) lerp(Color.White, d, 0.55f) else lerp(Color.White, d, 0.30f)) else Color.White
    val strongTarget = if (hasHue) (if (dark) lerp(Color.Black, d, 0.45f) else lerp(Color.White, d, 0.30f)) else Color.White

    val y = plan.effectiveY
    val accent = if (hasHue) lerp(base.accent, matchHue(base.accent, d), 0.45f) else base.accent
    val accentFixed = ensureContrast(accent, y, 3.0f, lighten = dark)
    return base.copy(
        glassFill = glass(base.glassFill, fillTarget, 0.35f),
        glassFillStrong = glass(base.glassFillStrong, strongTarget, 0.30f),
        glassTintTop = if (hasHue) lerp(base.glassTintTop, lerp(Color.White, d, 0.35f), 0.25f).copy(alpha = base.glassTintTop.alpha) else base.glassTintTop,
        textPrimary = tintInk(base.textPrimary, 0.16f),
        textSecondary = tintInk(base.textSecondary, 0.22f),
        accent = accentFixed,
        accentAlt = ensureContrast(lerp(base.accentAlt, accentFixed, 0.5f), y, 2.6f, lighten = dark),
        accentSoft = accentFixed.copy(alpha = base.accentSoft.alpha),
        success = ensureContrast(base.success, y, 3.0f, lighten = dark),
        warning = ensureContrast(base.warning, y, 3.0f, lighten = dark),
        danger = ensureContrast(base.danger, y, 3.0f, lighten = dark),
        shadow = if (dark) base.shadow else base.shadow.copy(alpha = (base.shadow.alpha * (0.75f + 0.7f * y.coerceIn(0f, 1f))).coerceAtMost(0.34f)),
    )
}
