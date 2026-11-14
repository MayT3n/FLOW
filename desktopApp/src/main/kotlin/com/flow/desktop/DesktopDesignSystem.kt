package com.flow.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared visual tokens for the Windows desktop experience.
 * Keep visual decisions here so screens do not drift into unrelated styles.
 */
internal object MedFlowDesign {
    // Dark surfaces are blue-charcoal rather than pure black.
    val darkBackground = Color(0xFF0D121A)
    val darkSurface = Color(0xFF141B25)
    val darkElevated = Color(0xFF1C2633)
    val darkVariant = Color(0xFF222E3D)
    val darkText = Color(0xFFF1F4F8)
    val darkMuted = Color(0xFF98A5B5)
    val darkAccent = Color(0xFF82A5F5)

    // Light mode has its own tonal hierarchy, not a simple inversion.
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
    val contentMaxWidth = 1580.dp
    val windowInset = 18.dp

    val panelRadius = 22.dp
    val sidebarRadius = 24.dp
    val controlRadius = 13.dp
    val panelPaddingHorizontal = 22.dp
    val panelPaddingVertical = 19.dp
}
