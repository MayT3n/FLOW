package com.flow.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flow.focus.data.GlassQuality

/**
 * Shared visual tokens for the Windows desktop experience.
 * Keep surfaces, spacing and material choices centralized.
 */
internal object MedFlowDesign {
    val darkBackground = Color(0xFF0D121A)
    val darkSurface = Color(0xFF141B25)
    val darkElevated = Color(0xFF1C2633)
    val darkVariant = Color(0xFF222E3D)
    val darkText = Color(0xFFF1F4F8)
    val darkMuted = Color(0xFF98A5B5)
    val darkAccent = Color(0xFF82A5F5)
    val darkBorder = Color(0x2BFFFFFF)

    val lightBackground = Color(0xFFF3F5F8)
    val lightSurface = Color(0xFFFFFFFF)
    val lightElevated = Color(0xFFF9FAFC)
    val lightVariant = Color(0xFFE9EEF4)
    val lightText = Color(0xFF202938)
    val lightMuted = Color(0xFF6D7989)
    val lightAccent = Color(0xFF4D6FD4)
    val lightBorder = Color(0xFFDDE3EA)

    val sidebarExpanded = 236.dp
    val sidebarCollapsed = 72.dp
    val contentMaxWidth = 1480.dp
    val windowInset = 18.dp

    val panelRadius = 20.dp
    val sidebarRadius = 22.dp
    val controlRadius = 12.dp
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
     * Deliberately distinct quality profiles.
     * Native DWM backdrop selection is handled by WindowsBackdrop.
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
            surfaceAlpha = .945f,
            tintAlpha = .018f,
            edgeAlpha = .085f,
            highlightAlpha = .11f,
            elevation = 4.dp
        )
        GlassQuality.HIGH -> GlassProfile(
            surfaceAlpha = .895f,
            tintAlpha = .034f,
            edgeAlpha = .125f,
            highlightAlpha = .19f,
            elevation = 7.dp
        )
        GlassQuality.ULTRA -> GlassProfile(
            surfaceAlpha = .855f,
            tintAlpha = .052f,
            edgeAlpha = .17f,
            highlightAlpha = .27f,
            elevation = 10.dp
        )
    }
}
