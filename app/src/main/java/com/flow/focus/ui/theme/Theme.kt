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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
    bgTop = Color(0xFF101318), bgMid = Color(0xFF171C24), bgBottom = Color(0xFF202833),
    blobA = Color(0xFF526B86), blobB = Color(0xFF625A78), blobC = Color(0xFF477A7B),
    glassFill = Color(0xFFB8C8DC).copy(alpha = 0.065f),
    glassFillStrong = Color(0xFF252E39).copy(alpha = 0.46f),
    glassTintTop = Color.White.copy(alpha = 0.14f),
    glassEdge = Color.White,
    textPrimary = Color(0xFFF0F2F5), textSecondary = Color(0xFFAAB4C1), textOnAccent = Color.White,
    accent = Color(0xFF79A9E4), accentAlt = Color(0xFF91A6C7), accentSoft = Color(0xFF79A9E4).copy(alpha = 0.20f),
    success = Color(0xFF55B98A), warning = Color(0xFFD9A35A), danger = Color(0xFFE27A86),
    track = Color.White.copy(alpha = 0.11f), shadow = Color(0xFF000000).copy(alpha = 0.42f),
)

val LightColors = FlowColors(
    isDark = false,
    bgTop = Color(0xFFF4F5F7), bgMid = Color(0xFFF0F2F5), bgBottom = Color(0xFFE7ECF2),
    blobA = Color(0xFFB8C9DC), blobB = Color(0xFFC9C6D4), blobC = Color(0xFFB8D0CE),
    // Light glass stays translucent so custom and built-in wallpapers remain visible.
    // Light mode prioritises readable controls over excessive wallpaper bleed-through.
    glassFill = Color(0xFFF9FBFF).copy(alpha = 0.43f),
    glassFillStrong = Color(0xFFFBFCFF).copy(alpha = 0.68f),
    glassTintTop = Color.White.copy(alpha = 0.25f),
    glassEdge = Color.White,
    textPrimary = Color(0xFF202833), textSecondary = Color(0xFF687586), textOnAccent = Color.White,
    accent = Color(0xFF477DC5), accentAlt = Color(0xFF6B8FC8), accentSoft = Color(0xFF477DC5).copy(alpha = 0.13f),
    success = Color(0xFF238C66), warning = Color(0xFFB87928), danger = Color(0xFFD64D5D),
    track = Color(0xFF202833).copy(alpha = 0.09f), shadow = Color(0xFF42566F).copy(alpha = 0.16f),
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

/** Measured safe space reserved for the floating dock and optional mini-player. */
val LocalDockContentPadding = compositionLocalOf<Dp> { 140.dp }

/** فونت: اگر Vazirmatn در assets/fonts باشد (CI دانلودش می‌کند) استفاده می‌شود، وگرنه فونت سیستم. */
fun loadFontFamily(ctx: Context): FontFamily {
    return try {
        val am = ctx.assets
        fun has(name: String) = try { am.open("fonts/$name").close(); true } catch (e: Exception) { false }
        if (has("Vazirmatn-Regular.ttf")) {
            val list = mutableListOf(Font(assetManager = am, path = "fonts/Vazirmatn-Regular.ttf", weight = FontWeight.Normal))
            if (has("Vazirmatn-Medium.ttf")) list += Font(assetManager = am, path = "fonts/Vazirmatn-Medium.ttf", weight = FontWeight.Medium)
            if (has("Vazirmatn-SemiBold.ttf")) {
                list += Font(assetManager = am, path = "fonts/Vazirmatn-SemiBold.ttf", weight = FontWeight.SemiBold)
            } else if (has("Vazirmatn-Bold.ttf")) {
                list += Font(assetManager = am, path = "fonts/Vazirmatn-Bold.ttf", weight = FontWeight.SemiBold)
            }
            if (has("Vazirmatn-Bold.ttf")) {
                list += Font(assetManager = am, path = "fonts/Vazirmatn-Bold.ttf", weight = FontWeight.Bold)
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
