package com.flow.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flow.focus.data.GlassQuality

/**
 * MedFlow's desktop visual language.
 *
 * The window backdrop is provided by Windows DWM (Mica/Acrylic) when available;
 * composable surfaces add a layered glass treatment without expensive per-frame
 * bitmap capture, blur or custom GPU shaders.
 */
internal object MedFlowDesign {
    val darkBackground = Color(0xFF0B111C)
    val darkSurface = Color(0xFF142034)
    val darkElevated = Color(0xFF1A2A40)
    val darkVariant = Color(0xFF24354B)
    val darkText = Color(0xFFF4F7FC)
    val darkMuted = Color(0xFFA4B2C5)
    val darkAccent = Color(0xFF8AB8FF)
    val darkBorder = Color(0x38FFFFFF)

    val lightBackground = Color(0xFFEAF0F7)
    val lightSurface = Color(0xFFF9FBFE)
    val lightElevated = Color(0xFFFFFFFF)
    val lightVariant = Color(0xFFE2EAF3)
    val lightText = Color(0xFF1C293A)
    val lightMuted = Color(0xFF68778B)
    val lightAccent = Color(0xFF557FC8)
    val lightBorder = Color(0x40FFFFFF)

    val sidebarExpanded = 236.dp
    val sidebarCollapsed = 72.dp
    val contentMaxWidth = 1480.dp
    val windowInset = 18.dp

    val panelRadius = 23.dp
    val sidebarRadius = 27.dp
    val controlRadius = 14.dp
    val panelPaddingHorizontal = 20.dp
    val panelPaddingVertical = 17.dp

    data class GlassProfile(
        val surfaceAlpha: Float,
        val tintAlpha: Float,
        val edgeAlpha: Float,
        val highlightAlpha: Float,
        val elevation: Dp
    )

    /**
     * Performance keeps opaque, inexpensive surfaces. High and Ultra reveal more
     * of the DWM backdrop and add restrained edge/reflection detail.
     */
    fun glassProfile(quality: GlassQuality): GlassProfile = when (quality) {
        GlassQuality.LOW -> GlassProfile(
            surfaceAlpha = .975f,
            tintAlpha = .006f,
            edgeAlpha = .055f,
            highlightAlpha = .045f,
            elevation = 1.dp
        )
        GlassQuality.MEDIUM -> GlassProfile(
            surfaceAlpha = .86f,
            tintAlpha = .024f,
            edgeAlpha = .12f,
            highlightAlpha = .10f,
            elevation = 5.dp
        )
        GlassQuality.HIGH -> GlassProfile(
            surfaceAlpha = .73f,
            tintAlpha = .042f,
            edgeAlpha = .19f,
            highlightAlpha = .17f,
            elevation = 8.dp
        )
        GlassQuality.ULTRA -> GlassProfile(
            surfaceAlpha = .64f,
            tintAlpha = .062f,
            edgeAlpha = .25f,
            highlightAlpha = .23f,
            elevation = 11.dp
        )
    }
}
