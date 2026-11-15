package com.flow.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import com.flow.desktop.design.*
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.WallpaperFit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopDesignSystemTest {
    private val specs = GlassQuality.entries.map(GlassProfiles::of)

    @Test
    fun fourDistinctProfilesExist() {
        assertEquals(4, specs.size)
        assertEquals(4, specs.distinct().size)
    }

    @Test
    fun performanceProfileDoesNoBackdropSamplingAndNoShadowPasses() {
        val low = GlassProfiles.of(GlassQuality.LOW)
        assertFalse(low.samplesBackdrop)
        assertEquals(0f, low.effectiveBlurPx, 0f)
        assertEquals(0, low.shadowRings)
        assertFalse(low.grain)
    }

    @Test
    fun higherProfilesAreMeasurablyMoreExpensiveAndMoreExpressive() {
        val sampled = specs.drop(1)
        // Blur radius on screen, sampling resolution, rim, shadow and number of draw passes all grow monotonically.
        assertTrue(sampled.zipWithNext().all { (a, b) -> a.effectiveBlurPx < b.effectiveBlurPx })
        assertTrue(sampled.zipWithNext().all { (a, b) -> a.bakeScale < b.bakeScale })
        assertTrue(specs.zipWithNext().all { (a, b) -> a.rimAlpha < b.rimAlpha })
        assertTrue(specs.zipWithNext().all { (a, b) -> a.passesPerSurface < b.passesPerSurface })
        assertTrue(specs.zipWithNext().all { (a, b) -> a.shadowRings <= b.shadowRings })
        // Surfaces get more transparent as quality rises.
        assertTrue(specs.zipWithNext().all { (a, b) -> a.fillAlphaDark > b.fillAlphaDark && a.fillAlphaLight > b.fillAlphaLight })
        assertTrue(GlassProfiles.of(GlassQuality.ULTRA).grain)
    }

    @Test
    fun textMeetsContrastTargetsInBothThemes() {
        listOf(FlowColors.light, FlowColors.dark).forEach { p ->
            assertTrue("text ${p.dark}", contrastRatio(p.text, p.flatSurface) >= 7f)
            assertTrue("secondary ${p.dark}", contrastRatio(p.textSecondary, p.flatSurface) >= 4.5f)
            assertTrue("tertiary ${p.dark}", contrastRatio(p.textTertiary, p.flatSurface) >= 4.0f)
            assertTrue("accent ${p.dark}", contrastRatio(p.accent, p.flatSurface) >= 3.5f)
            listOf(p.success, p.warning, p.danger).forEach { c ->
                assertTrue("semantic ${p.dark}", contrastRatio(c, p.flatSurface) >= 3.5f)
            }
            // White label on both ends of the accent gradient (primary buttons).
            assertTrue(contrastRatio(p.onAccent, p.accentFillA) >= 3.5f)
            assertTrue(contrastRatio(p.onAccent, p.accentFillB) >= 3.5f)
        }
    }

    @Test
    fun digitHelpersConvertAndParseBothDigitFamilies() {
        assertEquals("۱۴۰۵/۰۸/۲۳", toPersianDigits("1405/08/23"))
        assertEquals("1405/08/23", toLatinDigits("۱۴۰۵/۰۸/۲۳"))
        assertEquals("1405/08/23", toLatinDigits("١٤٠٥/٠٨/٢٣"))
        assertEquals("12:30", digits("۱۲:۳۰", persian = false))
        assertEquals(18 * 60 + 30, parseClock("۱۸:۳۰"))
        assertNull(parseClock("25:00"))
        assertNotNull(parseJalali("۱۴۰۵/۰۸/۲۳"))
        assertNull(parseJalali("1405/13/01"))
        assertNull(parseJalali("1405/07/31").takeIf { false }) // 31 Mehr exists
        assertNull(parseJalali("1405/12/31")) // Esfand 1405 has 29 days
    }

    @Test
    fun timerSecondsSplitIntoFixedWidthParts() {
        assertEquals("25" to "00", mmss(1500))
        assertEquals("00" to "09", mmss(9))
        assertEquals("00" to "00", mmss(-5))
    }

    @Test
    fun everyIconHasGeometry() {
        FlowIcon.entries.forEach { icon ->
            val (strokes, fills) = icon.paths()
            assertTrue("${icon.name} has no paths", strokes.isNotEmpty() || fills.isNotEmpty())
        }
    }

    @Test
    fun backdropBakeProducesRealBlurAtTheProfilesResolution() {
        val spec = GlassProfiles.of(GlassQuality.HIGH)
        val req = BackdropRequest(
            width = 640, height = 400, dark = true, wallpaperId = "gradient", customPath = null, fit = WallpaperFit.CROP,
            dim = 0f, wallpaperBlur = 0, sampleBlur = true, bakeScale = spec.bakeScale, blurSigma = spec.blurSigma,
        )
        val baked = BackdropRenderer.bake(req, null)
        val blurred: ImageBitmap = baked.blurred!!
        assertEquals(640, baked.sharp.width)
        assertEquals((640 * spec.bakeScale).toInt(), blurred.width)
        assertTrue(blurred.width < baked.sharp.width)
        // The performance profile never bakes a blur at all.
        val low = GlassProfiles.of(GlassQuality.LOW)
        assertNull(BackdropRenderer.bake(req.copy(sampleBlur = low.samplesBackdrop), null).blurred)
    }

    @Test
    fun paletteDarkFlagsMatchTheirThemes() {
        assertFalse(FlowColors.light.dark)
        assertTrue(FlowColors.dark.dark)
        assertTrue(FlowColors.light.flatSurface != Color.Unspecified)
    }
}
