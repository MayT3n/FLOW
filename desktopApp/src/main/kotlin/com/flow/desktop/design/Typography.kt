package com.flow.desktop.design

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

/**
 * Persian typography. Vazirmatn is bundled as a local resource (see desktopApp/build.gradle.kts),
 * so rendering never depends on fonts installed on the user's machine.
 *
 * Rules: no letter-spacing (it breaks Arabic-script joining), generous line height (~1.5),
 * four weights only: Regular, Medium, SemiBold, Bold.
 */
@OptIn(ExperimentalTextApi::class)
internal object FlowFonts {
    val vazirmatn: FontFamily by lazy {
        FontFamily(
            Font("fonts/Vazirmatn-Regular.ttf", FontWeight.Normal),
            Font("fonts/Vazirmatn-Medium.ttf", FontWeight.Medium),
            Font("fonts/Vazirmatn-SemiBold.ttf", FontWeight.SemiBold),
            Font("fonts/Vazirmatn-Bold.ttf", FontWeight.Bold),
        )
    }

    val resourceNames = listOf(
        "fonts/Vazirmatn-Regular.ttf",
        "fonts/Vazirmatn-Medium.ttf",
        "fonts/Vazirmatn-SemiBold.ttf",
        "fonts/Vazirmatn-Bold.ttf",
    )
}

internal object FlowType {
    private val family get() = FlowFonts.vazirmatn

    private fun style(size: Int, weight: FontWeight, lineHeight: Int) =
        TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight, lineHeight = lineHeight.sp)

    /** Page / hero title (greeting). */
    val largeTitle get() = style(30, FontWeight.Bold, 44)
    /** Toolbar page title. */
    val title1 get() = style(20, FontWeight.Bold, 30)
    /** Panel title. */
    val title2 get() = style(15, FontWeight.SemiBold, 24)
    val headline get() = style(13, FontWeight.SemiBold, 21)
    val body get() = style(13, FontWeight.Normal, 21)
    val bodyMedium get() = style(13, FontWeight.Medium, 21)
    val callout get() = style(12, FontWeight.Medium, 19)
    val caption get() = style(11, FontWeight.Normal, 17)
    val captionMedium get() = style(11, FontWeight.Medium, 17)
    /** Large numerals (stat values). */
    val numeral get() = style(24, FontWeight.Bold, 34)
    /** Timer digits are drawn in fixed cells; this is the per-digit style. */
    fun timerDigit(sizeSp: Int) = style(sizeSp, FontWeight.Medium, (sizeSp * 1.2f).toInt())
}
