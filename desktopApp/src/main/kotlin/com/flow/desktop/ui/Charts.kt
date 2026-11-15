package com.flow.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flow.desktop.design.*

/** Rounds a maximum up to a "nice" axis ceiling so grid lines land on readable values. */
internal fun niceMax(v: Long): Long {
    if (v <= 0) return 60
    val steps = listOf(10L, 20L, 30L, 40L, 60L, 80L, 100L, 120L, 160L, 200L, 240L, 300L, 400L, 480L, 600L, 800L, 1000L)
    steps.firstOrNull { it >= v }?.let { return it }
    val pow = 1000L
    return ((v + pow - 1) / pow) * pow
}

@Composable
internal fun BarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    height: Dp = 150.dp,
    unit: String = "دقیقه",
) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val max = niceMax(values.maxOrNull() ?: 0L)
    val ticks = listOf(max, max * 3 / 4, max / 2, max / 4, 0L)
    val grow by animateFloatAsState(1f, tween(FlowMotion.ms(520, reduce), easing = FlowMotion.emphasized), label = "bars-in")
    val dense = values.size > 12
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize()) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))
                    for (i in 0..4) {
                        val y = size.height * i / 4f
                        drawLine(pal.hairline, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = if (i == 4) null else dash)
                    }
                }
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (dense) 3.dp else 10.dp)) {
                    values.forEachIndexed { i, v ->
                        val frac = (v.toFloat() / max).coerceIn(0f, 1f) * grow
                        val active = highlight == i
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier.fillMaxWidth(if (dense) .8f else .52f).fillMaxHeight(frac.coerceAtLeast(if (v > 0) .02f else 0f))
                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        if (active) Brush.verticalGradient(listOf(pal.accentFillB, pal.accentFillA))
                                        else Brush.verticalGradient(listOf(pal.accent.copy(alpha = .62f), pal.accent.copy(alpha = .30f))),
                                    ),
                            )
                        }
                    }
                }
            }
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                ticks.forEach { FlowText(num(it), style = FlowType.caption, color = pal.textTertiary, textAlign = TextAlign.End) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(if (dense) 3.dp else 10.dp)) {
                labels.forEachIndexed { i, l ->
                    val show = !dense || i % 3 == 0 || i == labels.lastIndex
                    FlowText(
                        if (show) numText(l) else "", Modifier.weight(1f), FlowType.caption,
                        if (highlight == i) pal.text else pal.textTertiary, maxLines = 1, textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.width(26.dp))
        }
    }
}

/** Smooth area/line chart. Oldest value is drawn at the right edge (reading direction in RTL). */
@Composable
internal fun TrendChart(values: List<Long>, labels: List<String>, modifier: Modifier = Modifier, height: Dp = 130.dp) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val reveal by animateFloatAsState(1f, tween(FlowMotion.ms(700, reduce), easing = FlowMotion.emphasized), label = "trend-in")
    val max = niceMax(values.maxOrNull() ?: 0L).toFloat()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = values.size
            if (n < 2) return@Canvas
            val padV = 8.dp.toPx()
            val h = size.height - 2 * padV
            fun px(i: Int) = size.width - size.width * i / (n - 1).toFloat()
            fun py(v: Long) = padV + h * (1f - (v / max) * reveal)
            for (i in 0..3) {
                val y = padV + h * i / 3f
                drawLine(pal.hairline, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            val line = Path()
            val pts = values.mapIndexed { i, v -> Offset(px(i), py(v)) }
            pts.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(p.x, p.y) else {
                    val prev = pts[i - 1]
                    val mx = (prev.x + p.x) / 2f
                    line.cubicTo(mx, prev.y, mx, p.y, p.x, p.y)
                }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(pts.last().x, size.height)
                lineTo(pts.first().x, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(pal.accent.copy(alpha = .28f), Color.Transparent)))
            drawPath(line, Brush.horizontalGradient(listOf(pal.accentFillB, pal.accentFillA)), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            pts.forEachIndexed { i, p ->
                if (i == 0) {
                    drawCircle(pal.accent.copy(alpha = .25f), 8.dp.toPx(), p)
                    drawCircle(Color.White, 4.5.dp.toPx(), p)
                    drawCircle(pal.accent, 3.dp.toPx(), p)
                } else drawCircle(pal.accent, 2.5.dp.toPx(), p)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { FlowText(numText(it), style = FlowType.caption, color = pal.textTertiary, maxLines = 1) }
        }
    }
}

@Composable
internal fun HBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(FlowMotion.ms(FlowMotion.progress, reduce), easing = FlowMotion.emphasized), label = "hbar")
    Box(modifier.fillMaxWidth().height(height).background(pal.fillSubtle, RoundedCornerShape(height))) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(color, RoundedCornerShape(height)))
    }
}
