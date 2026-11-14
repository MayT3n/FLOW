package com.flow.desktop

import com.flow.focus.data.GlassQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopDesignSystemTest {
    @Test
    fun glassProfilesAreDistinctAndProgressivelyMoreExpressive() {
        val profiles = listOf(
            MedFlowDesign.glassProfile(GlassQuality.LOW),
            MedFlowDesign.glassProfile(GlassQuality.MEDIUM),
            MedFlowDesign.glassProfile(GlassQuality.HIGH),
            MedFlowDesign.glassProfile(GlassQuality.ULTRA)
        )

        assertEquals(4, profiles.distinct().size)
        assertTrue(profiles.zipWithNext().all { (a, b) -> a.surfaceAlpha > b.surfaceAlpha })
        assertTrue(profiles.zipWithNext().all { (a, b) -> a.tintAlpha < b.tintAlpha })
        assertTrue(profiles.zipWithNext().all { (a, b) -> a.edgeAlpha < b.edgeAlpha })
        assertTrue(profiles.zipWithNext().all { (a, b) -> a.elevation < b.elevation })
        assertTrue(profiles[2].surfaceAlpha < .8f)
        assertTrue(profiles[3].surfaceAlpha < profiles[2].surfaceAlpha)
    }

    @Test
    fun performanceProfileKeepsTheMostOpaqueAndLightweightSurface() {
        val profile = MedFlowDesign.glassProfile(GlassQuality.LOW)
        assertEquals(.975f, profile.surfaceAlpha, 0.0001f)
        assertEquals(.006f, profile.tintAlpha, 0.0001f)
        assertEquals(1f, profile.elevation.value, 0.0001f)
    }
}
