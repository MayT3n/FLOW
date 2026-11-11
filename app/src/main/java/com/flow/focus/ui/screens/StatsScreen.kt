package com.flow.focus.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flow.focus.core.formatMinutes
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.domain.Bucket
import com.flow.focus.domain.Stats
import com.flow.focus.domain.StatsRange
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.ScreenHeader
import com.flow.focus.ui.components.SectionTitle
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.rememberToday
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalGlass

@Composable
fun StatsScreen(data: AppData, modifier: Modifier = Modifier) {
    val c = Flow.colors
    val today = rememberToday()
    var rangeIdx by rememberSaveable { mutableStateOf(1) }
    val range = StatsRange.entries[rangeIdx]
    val s = remember(data.sessions, data.tasks, range, today) { Stats.summarize(data, range, today) }
    val hasData = data.sessions.isNotEmpty() || data.tasks.any { it.done }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("آمار و تحلیل")
        SegmentedControl(listOf("روزانه", "هفتگی", "ماهانه"), rangeIdx, { rangeIdx = it })
        Text(
            when (range) { StatsRange.DAY -> "امروز"; StatsRange.WEEK -> "۷ روز اخیر"; StatsRange.MONTH -> "۳۰ روز اخیر" }.let { num(it) },
            style = Flow.type.caption, color = c.textSecondary, modifier = Modifier.padding(horizontal = 4.dp),
        )

        if (!hasData) {
            GlassSurface(Modifier.fillMaxWidth()) {
                EmptyState(FlowIcons.Stats, "هنوز آماری ثبت نشده", "بعد از اولین جلسه‌ی تمرکز، نمودارها و آمار واقعی اینجا ظاهر می‌شوند.")
            }
        }

        // ---- کاشی‌های خلاصه ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(FlowIcons.Clock, if (s.focusMinutes == 0L) num("۰ دقیقه") else num(formatMinutes(s.focusMinutes)), "زمان تمرکز", c.accent, Modifier.weight(1f))
            Tile(FlowIcons.Target, num(s.completedSessions), "جلسه‌ی کامل‌شده", c.success, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(FlowIcons.Hourglass, num("${s.avgSessionMinutes} دقیقه"), "میانگین هر جلسه", c.accentAlt, Modifier.weight(1f))
            Tile(FlowIcons.Calendar, num(s.activeDays), "روز فعال", c.warning, Modifier.weight(1f))
        }
        Tile(FlowIcons.Tasks, num(s.tasksDone), "وظیفه‌ی تکمیل‌شده", c.success, Modifier.fillMaxWidth())

        // ---- نمودار میله‌ای ----
        SectionTitle(
            when (range) { StatsRange.DAY -> "تمرکز در ساعت‌های روز"; StatsRange.WEEK -> "تمرکز روزانه"; StatsRange.MONTH -> "تمرکز روزانه در ماه" },
        )
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BarChart(s.buckets, labelEvery = if (range == StatsRange.MONTH) 5 else 1)
                Text("دقیقه", style = Flow.type.caption, color = c.textSecondary)
            }
        }

        // ---- روند ----
        SectionTitle("روند ۸ هفته‌ی اخیر")
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendChart(s.weeklyTrend)
                val last = s.weeklyTrend.lastOrNull() ?: 0L
                val prev = s.weeklyTrend.getOrNull(s.weeklyTrend.size - 2) ?: 0L
                Text(
                    when {
                        last == 0L && prev == 0L -> "هنوز داده‌ای برای مقایسه نیست"
                        prev == 0L -> num("این هفته ${formatMinutes(last)} تمرکز داشتی")
                        last >= prev -> num("نسبت به هفته‌ی قبل ${formatMinutes(last - prev)} بیشتر")
                        else -> num("نسبت به هفته‌ی قبل ${formatMinutes(prev - last)} کمتر")
                    },
                    style = Flow.type.caption, color = c.textSecondary,
                )
            }
        }

        // ---- دسته‌بندی‌ها ----
        SectionTitle("عملکرد دسته‌بندی‌ها")
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (s.categories.isEmpty()) {
                    Text("جلسه‌هایی که به یک وظیفه وصل باشند اینجا به تفکیک دسته نمایش داده می‌شوند.", style = Flow.type.caption, color = c.textSecondary)
                } else {
                    val total = s.categories.sumOf { it.minutes }.coerceAtLeast(1)
                    s.categories.forEach { cs ->
                        val cat = Categories.byId(cs.categoryId)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(if (cs.categoryId == null) c.textSecondary else Color(cat.color)))
                                Text(cs.categoryId?.let { cat.name } ?: "بدون وظیفه", Modifier.weight(1f), style = Flow.type.body, color = c.textPrimary)
                                Text(num("${formatMinutes(cs.minutes)} · ${(cs.minutes * 100 / total)}%"), style = Flow.type.caption, color = c.textSecondary)
                            }
                            FlowProgressBar(cs.minutes.toFloat() / total, color = if (cs.categoryId == null) c.textSecondary else Color(cat.color), height = 6.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, caption: String, tint: Color, modifier: Modifier) {
    GlassSurface(modifier, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowIcon(icon, tint = tint, size = 24.dp)
            Text(value, style = Flow.type.heading, color = Flow.colors.textPrimary, maxLines = 1)
            Text(caption, style = Flow.type.caption, color = Flow.colors.textSecondary)
        }
    }
}

/** نمودار میله‌ای متحرک؛ زمان از راست به چپ (راست‌چین). مقدار انیمیشن فقط در لایه‌ی گرافیکی خوانده می‌شود. */
@Composable
private fun BarChart(buckets: List<Bucket>, labelEvery: Int) {
    val c = Flow.colors
    val animate = LocalGlass.current.animations
    val progress = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        if (animate) { progress.snapTo(0f); progress.animateTo(1f, tween(650)) } else progress.snapTo(1f)
    }
    val max = (buckets.maxOfOrNull { it.minutes } ?: 0L).coerceAtLeast(1L)
    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(if (buckets.size > 12) 2.dp else 8.dp), verticalAlignment = Alignment.Bottom) {
        buckets.forEachIndexed { i, b ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val frac = b.minutes.toFloat() / max
                    if (b.minutes > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(if (buckets.size > 12) 0.8f else 0.55f)
                                .fillMaxHeight(frac.coerceAtLeast(0.04f))
                                .graphicsLayer { scaleY = progress.value; transformOrigin = TransformOrigin(0.5f, 1f) }
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(Brush.verticalGradient(listOf(c.accentAlt, c.accent))),
                        )
                    } else {
                        Box(Modifier.fillMaxWidth(if (buckets.size > 12) 0.8f else 0.55f).height(3.dp).clip(CircleShape).background(c.track))
                    }
                }
                Text(
                    if (i % labelEvery == 0 || i == buckets.lastIndex) num(b.label) else "",
                    style = Flow.type.caption, color = c.textSecondary, maxLines = 1, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** نمودار خطی روند؛ قدیمی‌ترین نقطه سمت راست و جدیدترین سمت چپ. */
@Composable
private fun TrendChart(values: List<Long>) {
    val c = Flow.colors
    val animate = LocalGlass.current.animations
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        if (animate) { progress.snapTo(0f); progress.animateTo(1f, tween(800)) } else progress.snapTo(1f)
    }
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        if (values.size < 2) return@Canvas
        val pad = 8.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        fun pt(i: Int): Offset {
            val x = size.width - pad - w * i / (values.size - 1)
            val y = pad + h * (1f - values[i] / max)
            return Offset(x, y)
        }
        // خطوط راهنما
        for (g in 0..2) {
            val y = pad + h * g / 2f
            drawLine(c.track, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
        }
        val line = Path()
        val fill = Path()
        values.indices.forEach { i ->
            val p = pt(i)
            if (i == 0) { line.moveTo(p.x, p.y); fill.moveTo(p.x, size.height - pad); fill.lineTo(p.x, p.y) }
            else {
                val prev = pt(i - 1)
                val mx = (prev.x + p.x) / 2f
                line.cubicTo(mx, prev.y, mx, p.y, p.x, p.y)
                fill.cubicTo(mx, prev.y, mx, p.y, p.x, p.y)
            }
        }
        fill.lineTo(pt(values.lastIndex).x, size.height - pad)
        fill.close()
        clipRect(left = size.width * (1f - progress.value)) {
            drawPath(fill, Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.32f), Color.Transparent)))
            drawPath(line, Brush.horizontalGradient(listOf(c.accentAlt, c.accent)), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            values.indices.forEach { i -> drawCircle(c.accent, 3.5.dp.toPx(), pt(i)) }
        }
    }
}
