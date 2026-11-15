package com.flow.desktop.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flow.focus.data.GlassQuality

/**
 * FLOW desktop design tokens — the single source of truth for colour, spacing, shape and motion.
 * Nothing outside `design/` should hard-code a colour or a duration.
 */
@Immutable
internal class FlowPalette(
    val dark: Boolean,
    // Text
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // Accent
    val accent: Color,        // icons / text accents
    val accentFillA: Color,   // gradient start for filled controls (white text sits on it)
    val accentFillB: Color,   // gradient end
    val onAccent: Color,
    // Semantic
    val success: Color,
    val warning: Color,
    val danger: Color,
    // Surfaces
    val flatSurface: Color,   // used when no backdrop is sampled (Performance / Mica)
    val glassTint: Color,     // base veil over the sampled backdrop
    val glassLight: Color,    // specular colour
    val hairline: Color,
    val fillSubtle: Color,    // idle control fill
    val fillHover: Color,
    val fillStrong: Color,
    val scrim: Color,
    // Wallpaper art
    val wallBase: Color,
    val wallBlobs: List<Color>,
)

internal object FlowColors {
    val light = FlowPalette(
        dark = false,
        text = Color(0xFF121C33),
        textSecondary = Color(0xFF4A5875),
        textTertiary = Color(0xFF66738F),
        accent = Color(0xFF2F6BE6),
        accentFillA = Color(0xFF2F6FEA),
        accentFillB = Color(0xFF6E5CE8),
        onAccent = Color(0xFFFFFFFF),
        success = Color(0xFF0B8A62),
        warning = Color(0xFFB86A00),
        danger = Color(0xFFD1434A),
        flatSurface = Color(0xFFF6F9FF),
        glassTint = Color(0xFFFFFFFF),
        glassLight = Color(0xFFFFFFFF),
        hairline = Color(0x1F1B2A4A),
        fillSubtle = Color(0x0F1B2A4A),
        fillHover = Color(0x1A1B2A4A),
        fillStrong = Color(0x261B2A4A),
        scrim = Color(0x6615203A),
        wallBase = Color(0xFFE8EEFB),
        wallBlobs = listOf(Color(0xFF9DB9F5), Color(0xFFC7B5F2), Color(0xFFB8E0F5), Color(0xFFF1D3EE)),
    )
    val dark = FlowPalette(
        dark = true,
        text = Color(0xFFF2F5FF),
        textSecondary = Color(0xFFA9B4D0),
        textTertiary = Color(0xFF8490AE),
        accent = Color(0xFF6BA5FF),
        accentFillA = Color(0xFF3D80F6),
        accentFillB = Color(0xFF7D64F2),
        onAccent = Color(0xFFFFFFFF),
        success = Color(0xFF3CCB9B),
        warning = Color(0xFFF0B04A),
        danger = Color(0xFFFF7A80),
        flatSurface = Color(0xFF111A30),
        glassTint = Color(0xFF0C1630),
        glassLight = Color(0xFFFFFFFF),
        hairline = Color(0x24FFFFFF),
        fillSubtle = Color(0x0FFFFFFF),
        fillHover = Color(0x1AFFFFFF),
        fillStrong = Color(0x29FFFFFF),
        scrim = Color(0x99030614),
        wallBase = Color(0xFF060A18),
        wallBlobs = listOf(Color(0xFF2B3FD0), Color(0xFF6A38D8), Color(0xFF1C6BC7), Color(0xFF3B1F8C)),
    )
}

internal object FlowDims {
    // 4-pt grid
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp

    val windowInset = 16.dp
    val gap = 18.dp

    val panelRadius = 20.dp
    val cardRadius = 14.dp
    val controlRadius = 11.dp
    val sidebarRadius = 22.dp

    val sidebarExpanded = 228.dp
    val sidebarCollapsed = 68.dp
    val toolbarHeight = 54.dp
    val contentMaxWidth = 1560.dp

    val panelPadding = 22.dp
    val rowHeight = 56.dp
}

/** Motion tokens. `reduce` collapses every duration to zero (instant state changes). */
internal object FlowMotion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    const val press = 110
    const val hover = 160
    const val page = 240
    const val dialog = 200
    const val progress = 420
    const val expand = 260

    fun ms(base: Int, reduce: Boolean): Int = if (reduce) 0 else base
}

/**
 * Glass material profile. The numbers are real inputs to the renderer, not labels:
 * [bakeScale] and [blurSigma] define the cached backdrop blur, the rest are draw layers.
 * `effectiveBlurPx` is the resulting blur radius on screen.
 */
@Immutable
internal data class GlassSpec(
    val quality: GlassQuality,
    val samplesBackdrop: Boolean,
    val bakeScale: Float,
    val blurSigma: Float,
    val fillAlphaLight: Float,
    val fillAlphaDark: Float,
    val saturation: Float,
    val rimAlpha: Float,
    val sheenAlpha: Float,
    val shadowRings: Int,
    val shadowAlpha: Float,
    val grain: Boolean,
    /** Number of draw passes per glass surface — the dominant per-frame cost. */
    val passesPerSurface: Int,
) {
    val effectiveBlurPx: Float get() = if (samplesBackdrop) blurSigma / bakeScale else 0f
}

internal object GlassProfiles {
    fun of(quality: GlassQuality): GlassSpec = when (quality) {
        GlassQuality.LOW -> GlassSpec(
            quality, samplesBackdrop = false, bakeScale = 1f, blurSigma = 0f,
            fillAlphaLight = .86f, fillAlphaDark = .84f, saturation = 1f,
            rimAlpha = .30f, sheenAlpha = 0f, shadowRings = 0, shadowAlpha = 0f,
            grain = false, passesPerSurface = 2,
        )
        GlassQuality.MEDIUM -> GlassSpec(
            quality, samplesBackdrop = true, bakeScale = 1f / 6f, blurSigma = 3.6f,
            fillAlphaLight = .46f, fillAlphaDark = .42f, saturation = 1.08f,
            rimAlpha = .42f, sheenAlpha = 0f, shadowRings = 4, shadowAlpha = .10f,
            grain = false, passesPerSurface = 4,
        )
        GlassQuality.HIGH -> GlassSpec(
            quality, samplesBackdrop = true, bakeScale = 1f / 4f, blurSigma = 7f,
            fillAlphaLight = .38f, fillAlphaDark = .34f, saturation = 1.22f,
            rimAlpha = .62f, sheenAlpha = .16f, shadowRings = 6, shadowAlpha = .13f,
            grain = false, passesPerSurface = 5,
        )
        GlassQuality.ULTRA -> GlassSpec(
            quality, samplesBackdrop = true, bakeScale = 1f / 3f, blurSigma = 11f,
            fillAlphaLight = .30f, fillAlphaDark = .27f, saturation = 1.38f,
            rimAlpha = .82f, sheenAlpha = .24f, shadowRings = 8, shadowAlpha = .16f,
            grain = true, passesPerSurface = 7,
        )
    }

    val all: List<GlassSpec> = GlassQuality.entries.map(::of)
}

internal val LocalFlowPalette = staticCompositionLocalOf { FlowColors.dark }
internal val LocalGlass = staticCompositionLocalOf { GlassProfiles.of(GlassQuality.MEDIUM) }
internal val LocalReduceMotion = staticCompositionLocalOf { false }
internal val LocalPersianDigits = staticCompositionLocalOf { true }

internal object Flow {
    val colors: FlowPalette
        @androidx.compose.runtime.Composable @ReadOnlyComposable get() = LocalFlowPalette.current
}

/** WCAG relative-luminance contrast ratio; used by tests to guard readability. */
internal fun contrastRatio(a: Color, b: Color): Float {
    fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    fun lum(c: Color) = 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
    val l1 = lum(a)
    val l2 = lum(b)
    val hi = maxOf(l1, l2)
    val lo = minOf(l1, l2)
    return (hi + 0.05f) / (lo + 0.05f)
}
