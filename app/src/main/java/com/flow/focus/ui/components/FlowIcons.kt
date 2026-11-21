package com.flow.focus.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * مجموعه‌ی آیکون اختصاصی FLOW: شبکه‌ی ۲۴، خط ۱.۸، سر و اتصال گرد. همه با یک زبان بصری کشیده شده‌اند؛ هر آیکون یک خط ظریف و یک لایه‌ی نرم دوتونه دارد
 * و رنگ‌پذیرند (tint). هیچ ایموجی‌ای در رابط کاربری استفاده نمی‌شود.
 */
object FlowIcons {
    private const val SOFT_ALPHA = 0.24f

    private fun build(
        name: String,
        stroke: List<String> = emptyList(),
        filled: List<String> = emptyList(),
        soft: List<String> = emptyList(),
        width: Float = 1.7f,
        softAlpha: Float = SOFT_ALPHA,
        mirror: Boolean = false,
    ): ImageVector {
        val b = ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f, autoMirror = mirror,
        )
        // لایه‌ی نرم (دوتونه): همان رنگ آیکون با شفافیت کم؛ امضای بصری مجموعه‌ی FLOW
        for (d in soft) {
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = SolidColor(Color.Black), fillAlpha = softAlpha, stroke = null,
            )
        }
        for (d in stroke) {
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        for (d in filled) {
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black), strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val MatFocus by lazy {
        build(
            "matFocus",
            stroke = listOf(
                "M12 3.8a8.2 8.2 0 1 0 0 16.4 8.2 8.2 0 0 0 0-16.4z",
                "M12 2v1.8",
            ),
            filled = listOf("M12 11.1a.9.9 0 1 0 0 1.8.9.9 0 0 0 0-1.8z"),
            soft = listOf("M12 7.6a4.4 4.4 0 1 0 0 8.8 4.4 4.4 0 0 0 0-8.8z"),
            width = 1.8f,
        )
    }

    // ---- آیکون‌های اختصاصی Dock: هر تب دو حالت دارد؛ خطی (غیرفعال) و پرشده‌ی دوتونه (فعال) ----
    private fun dock(name: String, active: Boolean, stroke: List<String>, soft: List<String>, filled: List<String> = emptyList()) =
        build(
            name, stroke = stroke, filled = filled, soft = soft,
            width = if (active) 2.0f else 1.7f, softAlpha = if (active) 0.62f else 0.18f,
        )

    private val homeStroke = listOf("M4 11.4 12 4.4l8 7", "M6 10v7.6a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V10", "M10 19.6v-3.2a2 2 0 0 1 4 0v3.2", "M16 7.4V5.2h1.8v3.6")
    private val homeSoft = listOf("M6 10 12 5l6 5v7.6a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2z")
    val DockHome by lazy { dock("dockHome", false, homeStroke, homeSoft) }
    val DockHomeActive by lazy { dock("dockHomeActive", true, homeStroke, homeSoft) }

    // برنامه‌ریزی: تقویم با نشان ساعت روی گوشه (زمان‌بندی)
    private val planStroke = listOf(
        "M6 5.5h11a2.5 2.5 0 0 1 2.5 2.5v3.6",
        "M6 5.5A2.5 2.5 0 0 0 3.5 8v9.5A2.5 2.5 0 0 0 6 20h5",
        "M3.5 10h16", "M8 3.5v3.4", "M15.5 3.5v3.4",
        "M7.5 13.4h.01", "M11 13.4h.01", "M7.5 16.8h.01",
        "M17 12.8a4.2 4.2 0 1 0 0 8.4 4.2 4.2 0 0 0 0-8.4z", "M17 14.9v2.3l1.6.9",
    )
    private val planSoft = listOf(
        "M3.5 8A2.5 2.5 0 0 1 6 5.5h11A2.5 2.5 0 0 1 19.5 8v2h-16z",
        "M17 12.8a4.2 4.2 0 1 0 0 8.4 4.2 4.2 0 0 0 0-8.4z",
    )
    private val planFilled = emptyList<String>()
    val DockPlan by lazy { dock("dockPlan", false, planStroke, planSoft, planFilled) }
    val DockPlanActive by lazy { dock("dockPlanActive", true, planStroke, planSoft, planFilled) }

    private val routineStroke = listOf(
        "M19.5 12a7.5 7.5 0 0 1-13.2 4.9", "M4.5 12a7.5 7.5 0 0 1 13.2-4.9",
        "M18.2 3.8v3.6h-3.6", "M5.8 20.2v-3.6h3.6", "M9.4 12.2l1.9 1.9 3.4-3.8",
    )
    private val routineSoft = listOf("M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10z")
    val DockRoutine by lazy { dock("dockRoutine", false, routineStroke, routineSoft) }
    val DockRoutineActive by lazy { dock("dockRoutineActive", true, routineStroke, routineSoft) }

    private val statsStroke = listOf("M6.5 19.5v-4.5", "M12 19.5V9", "M17.5 19.5v-7")
    private val statsSoft = listOf("M4.9 15h3.2v4.5H4.9z", "M10.4 9h3.2v10.5h-3.2z", "M15.9 12.5h3.2v7h-3.2z")
    private val statsFilled = listOf("M17.5 4.7a1.3 1.3 0 1 0 0 2.6 1.3 1.3 0 0 0 0-2.6z")
    val DockStats by lazy { dock("dockStats", false, statsStroke, statsSoft, statsFilled) }
    val DockStatsActive by lazy { dock("dockStatsActive", true, statsStroke, statsSoft, statsFilled) }

    private val settingsStroke = listOf(
        "M4 6.5h9", "M18 6.5h2", "M15.5 4.3a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z",
        "M4 12h3", "M12 12h8", "M9.5 9.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z",
        "M4 17.5h8", "M17 17.5h3", "M14.5 15.3a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z",
    )
    private val settingsSoft = listOf(
        "M15.5 4.3a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z", "M9.5 9.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z",
        "M14.5 15.3a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z",
    )
    val DockSettings by lazy { dock("dockSettings", false, settingsStroke, settingsSoft) }
    val DockSettingsActive by lazy { dock("dockSettingsActive", true, settingsStroke, settingsSoft) }

    val Home by lazy {
        build(
            "home",
            stroke = listOf(
                "M4.2 11.2 12 4.6l7.8 6.6",
                "M6.2 9.9V18a1.6 1.6 0 0 0 1.6 1.6h8.4a1.6 1.6 0 0 0 1.6-1.6V9.9",
                "M10 19.6v-3.4a2 2 0 0 1 4 0v3.4",
            ),
            soft = listOf("M6.2 9.9 12 5l5.8 4.9V18a1.6 1.6 0 0 1-1.6 1.6H7.8A1.6 1.6 0 0 1 6.2 18z"),
        )
    }
    val Calendar by lazy {
        build(
            "calendar",
            stroke = listOf(
                "M5.6 5.5h12.8a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2z",
                "M3.6 10h16.8", "M8 3.5v3.4", "M16 3.5v3.4",
                "M8 14h.01", "M12 14h.01", "M16 14h.01", "M8 17.3h.01", "M12 17.3h.01",
            ),
            soft = listOf("M3.6 7.5a2 2 0 0 1 2-2h12.8a2 2 0 0 1 2 2V10H3.6z"),
        )
    }
    val Tasks by lazy {
        build(
            "tasks",
            stroke = listOf(
                "M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
                "M8.2 12.2l2.6 2.6 5-5.2",
            ),
            soft = listOf("M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"),
        )
    }
    val Stats by lazy {
        build(
            "stats",
            stroke = listOf("M6 20v-6.5", "M12 20V5", "M18 20v-9.5"),
            soft = listOf("M4.6 13.5h2.8V20H4.6z", "M10.6 5h2.8v15h-2.8z", "M16.6 10.5h2.8V20h-2.8z"),
        )
    }
    val Settings by lazy {
        build(
            "settings",
            stroke = listOf(
                "M4 7h8", "M18 7h2", "M15 4.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z",
                "M4 17h2", "M12 17h8", "M9 14.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z",
            ),
            soft = listOf("M15 4.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z", "M9 14.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4z"),
        )
    }
    val Play by lazy { build("play", filled = listOf("M8.5 5.8v12.4a.6.6 0 0 0 .9.5l10-6.2a.6.6 0 0 0 0-1L9.4 5.3a.6.6 0 0 0-.9.5z")) }
    val Pause by lazy { build("pause", filled = listOf("M7.5 5.5h2.7v13H7.5z", "M13.8 5.5h2.7v13h-2.7z")) }
    val Reset by lazy { build("reset", listOf("M5 12a7 7 0 1 0 2.1-5", "M5 4.6v4.2h4.2")) }
    val Skip by lazy { build("skip", filled = listOf("M6 6.2v11.6L14.6 12z"), stroke = listOf("M18 5.8v12.4")) }
    val More by lazy {
        build("more", filled = listOf("M12 5.2a1.4 1.4 0 1 0 0 2.8 1.4 1.4 0 0 0 0-2.8z", "M12 10.6a1.4 1.4 0 1 0 0 2.8 1.4 1.4 0 0 0 0-2.8z", "M12 16a1.4 1.4 0 1 0 0 2.8 1.4 1.4 0 0 0 0-2.8z"))
    }
    val Cup by lazy { build("cup", stroke = listOf("M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z", "M16 10.5h1.5a2 2 0 0 1 0 4H16", "M8 4.5v2", "M12 4.5v2"), soft = listOf("M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z")) }
    val Book by lazy { build("book", stroke = listOf("M12 6.5C10.5 5.3 8 5 4.5 5.2V18c3.5-.2 6 .1 7.5 1.3 1.5-1.2 4-1.5 7.5-1.3V5.2C16 5 13.5 5.3 12 6.5z", "M12 6.5v12.8"), soft = listOf("M12 6.5C10.5 5.3 8 5 4.5 5.2V18c3.5-.2 6 .1 7.5 1.3z")) }
    val Heart by lazy { build("heart", stroke = listOf("M20.8 8.6c0 2-1.1 3.5-2.5 4.9L12 20l-6.3-6.5C4.3 12.1 3.2 10.6 3.2 8.6a5.2 5.2 0 0 1 8.8-3.7 5.2 5.2 0 0 1 8.8 3.7z"), soft = listOf("M20.8 8.6c0 2-1.1 3.5-2.5 4.9L12 20l-6.3-6.5C4.3 12.1 3.2 10.6 3.2 8.6a5.2 5.2 0 0 1 8.8-3.7 5.2 5.2 0 0 1 8.8 3.7z")) }
    val Download by lazy { build("download", listOf("M12 3.5v10.8", "M7.5 10l4.5 4.5 4.5-4.5", "M4.5 19.5h15")) }
    val Music by lazy {
        build(
            "music",
            listOf(
                "M4 13v-1.2a8 8 0 0 1 16 0V13",
                "M4 12.5h3v6H6a2 2 0 0 1-2-2z",
                "M20 12.5h-3v6h1a2 2 0 0 0 2-2z",
                "M10 19.5h4",
            ),
        )
    }
    val Minus by lazy { build("minus", listOf("M5 12h14"), width = 1.8f) }
    val Plus by lazy { build("plus", listOf("M12 5v14", "M5 12h14"), width = 1.8f) }
    val Search by lazy { build("search", listOf("M11 4.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13z", "M16 16l4 4")) }
    val ChevronNext by lazy { build("chevronNext", listOf("M9.5 6l6 6-6 6"), mirror = true) }
    val ChevronPrev by lazy { build("chevronPrev", listOf("M14.5 6l-6 6 6 6"), mirror = true) }
    val ChevronDown by lazy { build("chevronDown", listOf("M6 9.5l6 6 6-6")) }
    val ChevronUp by lazy { build("chevronUp", listOf("M6 14.5l6-6 6 6")) }
    val Trash by lazy { build("trash", listOf("M5 7h14", "M9.5 7V4.8h5V7", "M7 7l.8 12h8.4L17 7", "M10 11v5", "M14 11v5")) }
    val Edit by lazy { build("edit", listOf("M4.5 19.5l1-4L16 5a1.8 1.8 0 0 1 2.5 0l.5.5a1.8 1.8 0 0 1 0 2.5L8.5 18.5z", "M14 7l3 3")) }
    val Clock by lazy { build("clock", stroke = listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z", "M12 8v4.2l2.8 1.8"), soft = listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z")) }
    val Target by lazy { build("target", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z", "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z", "M12 12h.01"), width = 1.8f) }
    val Leaf by lazy { build("leaf", stroke = listOf("M5 19C5 10.5 10 5.5 19 5c0 9-5 14-13.5 14", "M5 19l7.5-7.5"), soft = listOf("M5 19C5 10.5 10 5.5 19 5c0 9-5 14-13.5 14z")) }
    val Sun by lazy {
        build(
            "sun",
            listOf(
                "M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z", "M12 3v2", "M12 19v2", "M3 12h2", "M19 12h2",
                "M5.6 5.6l1.4 1.4", "M17 17l1.4 1.4", "M5.6 18.4L7 17", "M17 7l1.4-1.4",
            ),
        )
    }
    val Moon by lazy { build("moon", stroke = listOf("M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z"), soft = listOf("M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z")) }
    val Close by lazy { build("close", listOf("M6 6l12 12", "M18 6L6 18")) }
    val Check by lazy { build("check", listOf("M5.5 12.5l4.2 4.2L18.5 7.8"), width = 1.8f) }
    val Bell by lazy { build("bell", stroke = listOf("M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.5h-15z", "M10 20.5a2.2 2.2 0 0 0 4 0"), soft = listOf("M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.5h-15z")) }
    val Volume by lazy { build("volume", listOf("M4.5 9.5h3L12 6v12l-4.5-3.5h-3z", "M15.5 9a4 4 0 0 1 0 6", "M18 6.5a7.5 7.5 0 0 1 0 11")) }
    val Rain by lazy {
        build(
            "rain",
            listOf("M7 15a4 4 0 0 1-.5-7.9A5.5 5.5 0 0 1 17 8.5 3.5 3.5 0 0 1 17.5 15z", "M8 18l-1 2", "M12 18l-1 2", "M16 18l-1 2"),
        )
    }
    val Wind by lazy { build("wind", listOf("M3.5 9h10a2.5 2.5 0 1 0-2.5-2.5", "M3.5 13h14a2.5 2.5 0 1 1-2.5 2.5", "M3.5 17h6")) }
    val Wave by lazy { build("wave", listOf("M3 12h2l2-5 3 10 3-8 2 5 2-3h4")) }
    val Export by lazy { build("export", listOf("M12 15V4", "M7.5 8 12 3.5 16.5 8", "M5 19.5h14")) }
    val Import by lazy { build("import", listOf("M12 4v11", "M7.5 11l4.5 4.5 4.5-4.5", "M5 19.5h14")) }
    val Flag by lazy { build("flag", stroke = listOf("M6 20V4", "M6 5h11l-2 3.5 2 3.5H6"), soft = listOf("M6 5h11l-2 3.5 2 3.5H6z")) }
    val Shuffle by lazy { build("shuffle", listOf("M4 7h2.5a4 4 0 0 1 3.2 1.6l4.6 6.8A4 4 0 0 0 17.5 17H20", "M17 14l3 3-3 3", "M4 17h2.5a4 4 0 0 0 3.2-1.6l.8-1.2", "M15.5 7H20", "M17 4l3 3-3 3")) }
    val Repeat by lazy {
        build(
            "repeat",
            listOf("M5 11V9.5A2.5 2.5 0 0 1 7.5 7H18", "M15.5 4.5 18 7l-2.5 2.5", "M19 13v1.5a2.5 2.5 0 0 1-2.5 2.5H6", "M8.5 19.5 6 17l2.5-2.5"),
        )
    }
    val Move by lazy { build("move", listOf("M4.5 12h15", "M14.5 7l5 5-5 5"), mirror = true) }
    val Hourglass by lazy {
        build("hourglass", listOf("M7 4h10", "M7 20h10", "M8 4c0 4 4 5 4 8s-4 4-4 8", "M16 4c0 4-4 5-4 8s4 4 4 8"))
    }
    val Layers by lazy { build("layers", stroke = listOf("M12 4 3.5 8.5 12 13l8.5-4.5z", "M3.5 12.5 12 17l8.5-4.5", "M3.5 16 12 20.5l8.5-4.5"), soft = listOf("M12 4 3.5 8.5 12 13l8.5-4.5z")) }
    val ChartLine by lazy { build("chartLine", listOf("M4 18.5 9 13.5l3 2.5L20 7", "M14.5 7H20v5.5")) }
    val PieChart by lazy { build("pieChart", stroke = listOf("M11.5 3.5a8.5 8.5 0 1 0 9 9h-9z", "M14 3.8a8 8 0 0 1 6.2 6.2H14z"), soft = listOf("M11.5 3.5a8.5 8.5 0 1 0 9 9h-9z")) }
    val CalendarCheck by lazy { build("calendarCheck", listOf("M5.5 5h13A1.5 1.5 0 0 1 20 6.5v12a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5v-12A1.5 1.5 0 0 1 5.5 5z", "M4 9.5h16", "M8 3.5v3", "M16 3.5v3", "m8 14 2.2 2.2L16 10.5")) }
    val Flame by lazy { build("flame", stroke = listOf("M12 3.5c1.2 3.1 4.8 4.5 4.8 9a4.8 4.8 0 1 1-9.6 0c0-2.2 1.1-4.1 2.8-5.8.1 2 1.1 3 2 3.5.9-2.1.9-4.3 0-6.7z"), soft = listOf("M12 3.5c1.2 3.1 4.8 4.5 4.8 9a4.8 4.8 0 1 1-9.6 0c0-2.2 1.1-4.1 2.8-5.8.1 2 1.1 3 2 3.5.9-2.1.9-4.3 0-6.7z")) }
    val Award by lazy { build("award", listOf("M8 4h8v5a4 4 0 0 1-8 0z", "M8 8H5.5a2 2 0 0 0 0 4H9", "M16 8h2.5a2 2 0 0 1 0 4H15", "M10 13l-1.5 7 3.5-2 3.5 2-1.5-7")) }
    val Timer by lazy { build("timer", listOf("M9 3.5h6", "M12 7v5l3 2", "M6.2 7.2 4.8 5.8", "M17.8 7.2l1.4-1.4", "M12 5a8 8 0 1 0 0 16 8 8 0 0 0 0-16z")) }
    val Filter by lazy { build("filter", stroke = listOf("M4 5h16l-6.2 7v5.5l-3.6 1.8V12z"), soft = listOf("M4 5h16l-6.2 7v5.5l-3.6 1.8V12z")) }
    val Sparkles by lazy { build("sparkles", listOf("m12 3 1.2 5.8L19 10l-5.8 1.2L12 17l-1.2-5.8L5 10l5.8-1.2z", "m19 16 .6 2.4L22 19l-2.4.6L19 22l-.6-2.4L16 19l2.4-.6z")) }
    val Info by lazy { build("info", stroke = listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z", "M12 11v5", "M12 8h.01"), soft = listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z")) }
}
