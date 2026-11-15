package com.flow.desktop.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

/**
 * FLOW icon family: one 24×24 grid, round caps/joins, a single stroke weight, optional filled layers.
 * Every icon in the app comes from here (no emoji, no unicode glyphs, no one-off drawings).
 * Paths are vector data, so they stay crisp at any Windows display scale.
 */
internal enum class FlowIcon(private val strokes: List<String>, private val fills: List<String> = emptyList()) {
    Home(listOf("M3.8 11 12 3.8 20.2 11", "M5.8 9.6V19a1 1 0 0 0 1 1H9.8v-5.2a1 1 0 0 1 1-1h2.4a1 1 0 0 1 1 1V20h3a1 1 0 0 0 1-1V9.6")),
    Focus(listOf(circle(12f, 13.5f, 7.5f), "M12 9.5V13.5l2.6 1.6", "M9.5 2.8h5", "M12 2.8V6")),
    Tasks(listOf("M9.5 6.5H20", "M9.5 12H20", "M9.5 17.5H20", "M3.6 6.4l1.4 1.4 2.4-2.8", "M3.6 11.9l1.4 1.4 2.4-2.8", "M3.6 17.4l1.4 1.4 2.4-2.8")),
    Routine(listOf("M4 11V9.5A3.5 3.5 0 0 1 7.5 6H19", "M16 3l3 3-3 3", "M20 13v1.5a3.5 3.5 0 0 1-3.5 3.5H5", "M8 21l-3-3 3-3")),
    Target(listOf(circle(12f, 12f, 8.6f), circle(12f, 12f, 4.6f), "M12 12h.01")),
    Calendar(listOf(roundRect(4f, 5f, 16f, 15f, 2.4f), "M4 10h16", "M8.5 3v4", "M15.5 3v4")),
    Chart(listOf("M6 20v-8", "M12 20V5", "M18 20v-5")),
    Music(listOf("M9 18V6l11-2v12", circle(6f, 18f, 3f), circle(17f, 16f, 3f))),
    Settings(listOf("M4 7h8", "M16 7h4", circle(14f, 7f, 2f), "M4 17h3", "M11 17h9", circle(9f, 17f, 2f))),
    Search(listOf(circle(10.5f, 10.5f, 6.5f), "M20 20l-4.9-4.9")),
    Play(emptyList(), listOf("M8 5.6v12.8a.6.6 0 0 0 .92.5l10-6.4a.6.6 0 0 0 0-1L8.92 5.1A.6.6 0 0 0 8 5.6z")),
    Pause(emptyList(), listOf(roundRect(6f, 5f, 4f, 14f, 1.4f), roundRect(14f, 5f, 4f, 14f, 1.4f))),
    Next(listOf("M18 5.5v13"), listOf("M5.5 6.2v11.6a.5.5 0 0 0 .78.42l8.4-5.8a.5.5 0 0 0 0-.84l-8.4-5.8a.5.5 0 0 0-.78.42z")),
    Previous(listOf("M6 5.5v13"), listOf("M18.5 6.2v11.6a.5.5 0 0 1-.78.42l-8.4-5.8a.5.5 0 0 1 0-.84l8.4-5.8a.5.5 0 0 1 .78.42z")),
    Shuffle(listOf("M3 7h3.2c2 0 3.2 1 4.3 2.7l2.8 4.6c1.1 1.7 2.3 2.7 4.3 2.7H20", "M17.5 14l2.8 3-2.8 3", "M3 17h3.2c1.2 0 2.1-.4 2.9-1.1", "M13.8 8.1c.8-.7 1.7-1.1 2.8-1.1H20", "M17.5 4l2.8 3-2.8 3")),
    Repeat(listOf("M4 11V9.5A3.5 3.5 0 0 1 7.5 6H19", "M16 3l3 3-3 3", "M20 13v1.5a3.5 3.5 0 0 1-3.5 3.5H5", "M8 21l-3-3 3-3")),
    RepeatOne(listOf("M4 11V9.5A3.5 3.5 0 0 1 7.5 6H19", "M16 3l3 3-3 3", "M20 13v1.5a3.5 3.5 0 0 1-3.5 3.5H5", "M8 21l-3-3 3-3", "M11 11l1.5-1V15")),
    Heart(listOf("M12 19.8S4.2 15.2 4.2 9.6A4.1 4.1 0 0 1 12 7.6a4.1 4.1 0 0 1 7.8 2C19.8 15.2 12 19.8 12 19.8z")),
    Plus(listOf("M12 5v14", "M5 12h14")),
    Minus(listOf("M5 12h14")),
    Check(listOf("M5 12.6l4.4 4.4L19 7.4")),
    Close(listOf("M6 6l12 12", "M18 6L6 18")),
    ChevronRight(listOf("M9.5 6l6 6-6 6")),
    ChevronLeft(listOf("M14.5 6l-6 6 6 6")),
    ChevronDown(listOf("M6 9.5l6 6 6-6")),
    Trash(listOf("M4 7h16", "M9.5 7V4.8c0-.4.3-.8.8-.8h3.4c.5 0 .8.4.8.8V7", "M6.5 7l.8 12c0 .6.5 1 1 1h7.4c.5 0 1-.4 1-1l.8-12", "M10 11v5", "M14 11v5")),
    Edit(listOf("M4 20l1-4.2L16.2 4.6a1.8 1.8 0 0 1 2.6 0l.6.6a1.8 1.8 0 0 1 0 2.6L8.2 19 4 20z", "M14.5 6.4l3.1 3.1")),
    Sun(listOf(circle(12f, 12f, 3.8f), "M12 2.8v2", "M12 19.2v2", "M2.8 12h2", "M19.2 12h2", "M5.5 5.5l1.4 1.4", "M17.1 17.1l1.4 1.4", "M5.5 18.5l1.4-1.4", "M17.1 6.9l1.4-1.4")),
    Moon(listOf("M20 14.5A8.5 8.5 0 1 1 9.5 4a6.8 6.8 0 0 0 10.5 10.5z")),
    Coffee(listOf("M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5V9z", "M16 10.5h1.5a2.5 2.5 0 0 1 0 5H15.5", "M8 3.5v2", "M12 3.5v2")),
    Reset(listOf("M4.6 12a7.4 7.4 0 1 0 2.3-5.4", "M4.6 4.6v4h4")),
    Skip(listOf("M5 6l7 6-7 6z", "M13 6l7 6-7 6z")),
    Volume(listOf("M4 9.5v5h3.5l5 4v-13l-5 4H4z", "M16 9a4 4 0 0 1 0 6", "M18.6 6.4a8 8 0 0 1 0 11.2")),
    Download(listOf("M12 4v11", "M7.5 10.5L12 15l4.5-4.5", "M5 19.5h14")),
    Upload(listOf("M12 15V4", "M7.5 8.5L12 4l4.5 4.5", "M5 19.5h14")),
    Leaf(listOf("M12 20v-8", "M12 12c0-3.5-2.5-6-6.5-6 0 3.5 2.5 6 6.5 6z", "M12 14.5c0-3 2-5 5.5-5 0 3-2 5-5.5 5z")),
    Star(listOf("M12 4.2l2.4 5 5.5.7-4 3.8 1 5.4L12 16.4l-4.9 2.7 1-5.4-4-3.8 5.5-.7z")),
    SidebarToggle(listOf(roundRect(4f, 5f, 16f, 14f, 2.4f), "M14.5 5v14")),
    Image(listOf(roundRect(4f, 5f, 16f, 14f, 2.4f), "M4 16l4.6-4.6 4 4 3-3L20 17", "M15.5 9.5h.01")),
    Clock(listOf(circle(12f, 12f, 8.6f), "M12 7.5V12l3 1.8")),
    CheckCircle(listOf(circle(12f, 12f, 8.6f), "M8.2 12.4l2.6 2.6 5-5.4")),
    Circle(listOf(circle(12f, 12f, 8.6f))),
    Flag(listOf("M5.5 20.5V4", "M5.5 4.8h11l-2 3.6 2 3.6h-11")),
    Layers(listOf("M12 4l8.4 4.4L12 12.8 3.6 8.4z", "M3.6 12.6L12 17l8.4-4.4", "M3.6 16.4L12 20.8l8.4-4.4")),
    Palette(listOf("M12 3.5a8.5 8.5 0 1 0 0 17c1.2 0 1.8-.8 1.8-1.7 0-.6-.3-1.1-.6-1.5-.3-.4-.5-.8-.5-1.3 0-1 .8-1.8 1.8-1.8H17a3.5 3.5 0 0 0 3.5-3.5C20.5 6.7 16.7 3.5 12 3.5z", "M8 11h.01", "M10.5 7.6h.01", "M15 8.4h.01")),
    Database(listOf("M4.5 6c0-1.4 3.4-2.5 7.5-2.5s7.5 1.1 7.5 2.5-3.4 2.5-7.5 2.5S4.5 7.4 4.5 6z", "M4.5 6v12c0 1.4 3.4 2.5 7.5 2.5s7.5-1.1 7.5-2.5V6", "M4.5 12c0 1.4 3.4 2.5 7.5 2.5s7.5-1.1 7.5-2.5")),
    Power(listOf("M12 3.5v8", "M7 6.6a7.5 7.5 0 1 0 10 0")),
    Info(listOf(circle(12f, 12f, 8.6f), "M12 11v5.2", "M12 7.8h.01")),
    Timer(listOf(circle(12f, 13.5f, 7.5f), "M12 9.5V13.5l2.6 1.6", "M9.5 2.8h5", "M12 2.8V6")),
    Speaker(listOf("M4 9.5v5h3.5l5 4v-13l-5 4H4z")),
    Sparkle(listOf("M12 3.5l1.7 5.3 5.3 1.7-5.3 1.7L12 17.5l-1.7-5.3L5 10.5l5.3-1.7z")),
    Bell(listOf("M6 16.5V11a6 6 0 0 1 12 0v5.5l1.6 2H4.4z", "M10 21h4"));

    private val strokePaths: List<Path> by lazy { strokes.map { PathParser().parsePathString(it).toPath() } }
    private val fillPaths: List<Path> by lazy { fills.map { PathParser().parsePathString(it).toPath() } }

    fun paths(): Pair<List<Path>, List<Path>> = strokePaths to fillPaths
}

@Composable
internal fun FlowIconView(
    icon: FlowIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    weight: Float = 1.7f,
    /** Fills closed shapes (selected / active state), e.g. a liked heart. */
    filled: Boolean = false,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val (strokePaths, fillPaths) = icon.paths()
        withTransform({ scale(s, s, Offset.Zero) }) {
            val stroke = Stroke(width = weight, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fillPaths.forEach { drawPath(it, tint) }
            strokePaths.forEach { path ->
                if (filled) drawPath(path, tint.copy(alpha = tint.alpha * .9f))
                drawPath(path, tint, style = stroke)
            }
        }
    }
}
