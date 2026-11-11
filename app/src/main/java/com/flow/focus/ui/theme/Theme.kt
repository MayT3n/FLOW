package com.flow.focus.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.Settings
import com.flow.focus.data.ThemeMode

/** پالت اختصاصی FLOW؛ تم روشن و تاریک جداگانه طراحی شده‌اند (نه معکوس هم). */
@Immutable
data class FlowColors(
    val isDark: Boolean,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassTintTop: Color,
    val glassEdge: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnAccent: Color,
    val accent: Color,
    val accentAlt: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val track: Color,
    val shadow: Color,
)

val DarkColors = FlowColors(
    isDark = true,
    bgTop = Color(0xFF0A1030), bgMid = Color(0xFF101A4A), bgBottom = Color(0xFF1B2A72),
    blobA = Color(0xFF3B6CFF), blobB = Color(0xFF7A4DFF), blobC = Color(0xFF22B8F0),
    glassFill = Color(0xFF9FB4FF).copy(alpha = 0.10f),
    glassFillStrong = Color(0xFF1A2560).copy(alpha = 0.78f),
    glassTintTop = Color.White.copy(alpha = 0.14f),
    glassEdge = Color.White,
    textPrimary = Color(0xFFF1F4FF), textSecondary = Color(0xFFAAB6E6), textOnAccent = Color.White,
    accent = Color(0xFF5C9DFF), accentAlt = Color(0xFF8C6BFF), accentSoft = Color(0xFF5C9DFF).copy(alpha = 0.22f),
    success = Color(0xFF3DDC97), warning = Color(0xFFFFB84D), danger = Color(0xFFFF6B81),
    track = Color.White.copy(alpha = 0.12f), shadow = Color(0xFF000000).copy(alpha = 0.45f),
)

val LightColors = FlowColors(
    isDark = false,
    bgTop = Color(0xFFEAF1FF), bgMid = Color(0xFFEFEBFF), bgBottom = Color(0xFFD9E4FF),
    blobA = Color(0xFF9DBBFF), blobB = Color(0xFFC7B2FF), blobC = Color(0xFFA6E3FF),
    glassFill = Color.White.copy(alpha = 0.52f),
    glassFillStrong = Color(0xFFF6F8FF).copy(alpha = 0.92f),
    glassTintTop = Color.White.copy(alpha = 0.65f),
    glassEdge = Color.White,
    textPrimary = Color(0xFF16204A), textSecondary = Color(0xFF5A6590), textOnAccent = Color.White,
    accent = Color(0xFF3A63F0), accentAlt = Color(0xFF7B52F0), accentSoft = Color(0xFF3A63F0).copy(alpha = 0.14f),
    success = Color(0xFF1FA971), warning = Color(0xFFE0891A), danger = Color(0xFFE0425F),
    track = Color(0xFF16204A).copy(alpha = 0.08f), shadow = Color(0xFF3A4C99).copy(alpha = 0.22f),
)

@Immutable
data class GlassConfig(
    val quality: GlassQuality = GlassQuality.MEDIUM,
    val opacity: Float = 1f,
    val blurDp: Int = 24,
    val shine: Float = 0.7f,
    val animations: Boolean = true,
)

val LocalFlowColors = compositionLocalOf { DarkColors }
val LocalGlass = compositionLocalOf { GlassConfig() }
val LocalPersianDigits = compositionLocalOf { true }

/** فونت: اگر Vazirmatn در assets/fonts باشد (CI دانلودش می‌کند) استفاده می‌شود، وگرنه فونت سیستم. */
fun loadFontFamily(ctx: Context): FontFamily {
    return try {
        val am = ctx.assets
        fun has(name: String) = try { am.open("fonts/$name").close(); true } catch (e: Exception) { false }
        if (has("Vazirmatn-Regular.ttf")) {
            val list = mutableListOf(Font(assetManager = am, path = "fonts/Vazirmatn-Regular.ttf", weight = FontWeight.Normal))
            if (has("Vazirmatn-Medium.ttf")) list += Font(assetManager = am, path = "fonts/Vazirmatn-Medium.ttf", weight = FontWeight.Medium)
            if (has("Vazirmatn-Bold.ttf")) {
                list += Font(assetManager = am, path = "fonts/Vazirmatn-Bold.ttf", weight = FontWeight.Bold)
                list += Font(assetManager = am, path = "fonts/Vazirmatn-Bold.ttf", weight = FontWeight.SemiBold)
            }
            FontFamily(list)
        } else FontFamily.Default
    } catch (e: Exception) {
        FontFamily.Default
    }
}

@Immutable
class FlowTypography(family: FontFamily) {
    val display = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 64.sp, letterSpacing = 0.sp)
    val title = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp)
    val heading = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 26.sp)
    val body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp)
    val bodyStrong = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 24.sp)
    val caption = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 19.sp)
    val label = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp)
}

val LocalFlowType = compositionLocalOf { FlowTypography(FontFamily.Default) }

object Flow {
    val colors: FlowColors @Composable get() = LocalFlowColors.current
    val type: FlowTypography @Composable get() = LocalFlowType.current
    val glass: GlassConfig @Composable get() = LocalGlass.current
}

private fun scheme(c: FlowColors): ColorScheme =
    (if (c.isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent, onPrimary = c.textOnAccent,
        background = c.bgTop, onBackground = c.textPrimary,
        surface = c.bgMid, onSurface = c.textPrimary,
    )

@Composable
fun FlowTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colors = if (dark) DarkColors else LightColors
    val ctx = LocalContext.current
    val family = remember { loadFontFamily(ctx) }
    val type = remember(family) { FlowTypography(family) }
    val glass = GlassConfig(settings.glassQuality, settings.glassOpacity, settings.glassBlurDp, settings.glassShine, settings.animations)
    MaterialTheme(colorScheme = scheme(colors)) {
        // رابط کاربری همیشه راست‌چین است، مستقل از زبان دستگاه.
        CompositionLocalProvider(
            LocalFlowColors provides colors,
            LocalFlowType provides type,
            LocalGlass provides glass,
            LocalPersianDigits provides settings.persianDigits,
            LocalLayoutDirection provides LayoutDirection.Rtl,
            content = content,
        )
    }
}
