package com.flow.focus.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flow.focus.core.formatMinutes
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.domain.Bucket
import com.flow.focus.domain.CategoryStat
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
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.rememberToday
import com.flow.focus.ui.theme.LocalDockContentPadding
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.motion.Motion
import com.flow.focus.ui.theme.LocalGlass

@Composable
fun StatsScreen(data: AppData, modifier: Modifier = Modifier) {
    val c = Flow.colors
    val today = rememberToday()
    var rangeIdx by rememberSaveable { mutableStateOf(1) }
    val range = StatsRange.entries[rangeIdx]
    val summary = remember(data.sessions, data.tasks, range, today) {
        Stats.summarize(data, range, today)
    }
    val hasFocus = summary.focusMinutes > 0L
    val dailySummary = remember(data.sessions, data.tasks, today) {
        Stats.summarize(data, StatsRange.DAY, today)
    }

    Column(
        modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = LocalDockContentPadding.current),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        ScreenHeader("آمار و تحلیل")
        SegmentedControl(listOf("روزانه", "هفتگی", "ماهانه"), rangeIdx, { rangeIdx = it })
        val swapAnim1 = LocalGlass.current.animations
        AnimatedContent(
            targetState = range,
            transitionSpec = { Motion.valueSwap(swapAnim1, 150, 110) },
            label = "stats-range-label",
        ) { activeRange ->
            Text(
                when (activeRange) {
                    StatsRange.DAY -> "نمای امروز"
                    StatsRange.WEEK -> "۷ روز اخیر"
                    StatsRange.MONTH -> "۳۰ روز اخیر"
                },
                style = Flow.type.caption,
                color = c.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        OverviewCard(
            focusMinutes = summary.focusMinutes,
            completedSessions = summary.completedSessions,
            activeDays = summary.activeDays,
            range = range,
        )

        DailyGoalCard(
            focusMinutes = dailySummary.focusMinutes,
            goalMinutes = data.settings.dailyGoalMin,
        )

        SectionTitle("خلاصه عملکرد")
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard(
                    FlowIcons.Target,
                    num(summary.completedSessions),
                    "جلسه‌ی کامل‌شده",
                    c.success,
                    Modifier.weight(1f),
                )
                MetricCard(
                    FlowIcons.Hourglass,
                    num(summary.avgSessionMinutes.toString() + " دقیقه"),
                    "میانگین هر جلسه",
                    c.accentAlt,
                    Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard(
                    FlowIcons.CalendarCheck,
                    num(summary.activeDays),
                    "روز فعال",
                    c.warning,
                    Modifier.weight(1f),
                )
                MetricCard(
                    FlowIcons.Tasks,
                    num(summary.tasksDone),
                    "وظیفه‌ی تکمیل‌شده",
                    c.accent,
                    Modifier.weight(1f),
                )
            }
        }

        SectionTitle(
            when (range) {
                StatsRange.DAY -> "توزیع تمرکز در روز"
                StatsRange.WEEK -> "مقایسه‌ی روزهای هفته"
                StatsRange.MONTH -> "فعالیت روزانه در ماه"
            },
        )
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp)) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("زمان تمرکز", Modifier.weight(1f), style = Flow.type.bodyStrong, color = c.textPrimary)
                    Text(
                        num(formatMinutes(summary.focusMinutes)),
                        style = Flow.type.caption,
                        color = c.accent,
                    )
                }
                FocusBarChart(summary.buckets, labelEvery = if (range == StatsRange.MONTH) 5 else 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(c.accent))
                    Text(
                        if (range == StatsRange.DAY) "هر ستون یک بازه‌ی ۳ ساعته است" else "مجموع دقیقه‌های تمرکز",
                        Modifier.padding(start = 6.dp),
                        style = Flow.type.caption,
                        color = c.textSecondary,
                    )
                }
            }
        }

        SectionTitle("روند هشت هفته‌ی اخیر")
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp)) {
            Column(
                Modifier.padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                WeeklyTrendChart(summary.weeklyTrend)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("۸ هفته قبل", style = Flow.type.caption, color = c.textSecondary)
                    Text("این هفته", style = Flow.type.caption, color = c.textSecondary)
                }
                val current = summary.weeklyTrend.lastOrNull() ?: 0L
                val previous = summary.weeklyTrend.getOrNull(summary.weeklyTrend.lastIndex - 1) ?: 0L
                Text(
                    when {
                        current == 0L && previous == 0L -> "هنوز داده‌ای برای مقایسه ثبت نشده است"
                        previous == 0L -> "این هفته " + formatMinutes(current) + " تمرکز ثبت شده"
                        current >= previous -> "افزایش " + formatMinutes(current - previous) + " نسبت به هفته‌ی قبل"
                        else -> "کاهش " + formatMinutes(previous - current) + " نسبت به هفته‌ی قبل"
                    },
                    style = Flow.type.caption,
                    color = c.textSecondary,
                )
            }
        }

        SectionTitle("ترکیب دسته‌بندی‌ها")
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp)) {
            if (summary.categories.isEmpty()) {
                EmptyState(
                    FlowIcons.PieChart,
                    "هنوز ترکیبی برای نمایش نیست",
                    "جلسه‌های متصل به وظیفه، بر اساس دسته‌بندی در این بخش دیده می‌شوند.",
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            } else {
                CategoryBreakdown(summary.categories)
            }
        }

        if (!hasFocus) {
            GlassSurface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                level = GlassLevel.Subtle,
            ) {
                EmptyState(
                    FlowIcons.Flame,
                    "شروع آمار از اولین جلسه",
                    "با ثبت اولین جلسه‌ی تمرکز، نمودارها و روند پیشرفتت شکل می‌گیرند.",
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(focusMinutes: Long, goalMinutes: Int) {
    val colors = Flow.colors
    val goal = goalMinutes.coerceAtLeast(1).toLong()
    val progress = (focusMinutes.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val percent = (progress * 100f).toInt()
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        level = GlassLevel.Subtle,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FlowIcon(FlowIcons.Target, tint = Flow.colors.accent, size = 18.dp)
                    Text("هدف تمرکز امروز", style = Flow.type.bodyStrong, color = Flow.colors.textPrimary)
                }
                Text(
                    num("${formatMinutes(focusMinutes)} از ${goalMinutes} دقیقه"),
                    style = Flow.type.caption,
                    color = Flow.colors.textSecondary,
                )
                FlowProgressBar(progress, Modifier.fillMaxWidth(), height = 5.dp)
                Text(
                    if (progress >= 1f) "هدف روزانه کامل شده" else num("${formatMinutes((goal - focusMinutes).coerceAtLeast(0L))} تا رسیدن به هدف"),
                    style = Flow.type.caption,
                    color = if (progress >= 1f) Flow.colors.success else Flow.colors.textSecondary,
                )
            }
            Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val origin = Offset(inset, inset)
                    drawArc(
                        color = colors.track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = origin,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = colors.accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = origin,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Text(num("${percent}%"), style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun OverviewCard(
    focusMinutes: Long,
    completedSessions: Int,
    activeDays: Int,
    range: StatsRange,
) {
    val c = Flow.colors
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(27.dp),
        level = GlassLevel.Strong,
        elevated = true,
    ) {
        Column(
            Modifier.padding(horizontal = 19.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    FlowIcon(FlowIcons.ChartLine, tint = c.accent, size = 23.dp)
                }
                Column(
                    Modifier.weight(1f).padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text("زمان تمرکز", style = Flow.type.bodyStrong, color = c.textPrimary)
                    Text(
                        when (range) {
                            StatsRange.DAY -> "جمع کل امروز"
                            StatsRange.WEEK -> "جمع کل این هفته"
                            StatsRange.MONTH -> "جمع کل این ماه"
                        },
                        style = Flow.type.caption,
                        color = c.textSecondary,
                    )
                }
                FlowIcon(FlowIcons.Clock, tint = c.textSecondary, size = 19.dp)
            }
            val swapAnim2 = LocalGlass.current.animations
            AnimatedContent(
                targetState = num(formatMinutes(focusMinutes)),
                transitionSpec = { Motion.valueSwap(swapAnim2, 150, 110) },
                label = "stats-focus-total",
            ) { value ->
                Text(
                    value,
                    style = Flow.type.display.copy(fontSize = 38.sp, lineHeight = 46.sp),
                    color = c.textPrimary,
                    maxLines = 1,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.track))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverviewMiniStat(
                    FlowIcons.Target,
                    num(completedSessions),
                    "جلسه کامل",
                    Modifier.weight(1f),
                )
                OverviewMiniStat(
                    FlowIcons.CalendarCheck,
                    num(activeDays),
                    "روز فعال",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OverviewMiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        FlowIcon(icon, tint = Flow.colors.accent, size = 17.dp)
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(value, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary)
            Text(label, style = Flow.type.caption, color = Flow.colors.textSecondary)
        }
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier,
) {
    GlassSurface(
        modifier,
        shape = RoundedCornerShape(22.dp),
        level = GlassLevel.Subtle,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                FlowIcon(icon, tint = tint, size = 18.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                val swapAnim3 = LocalGlass.current.animations
                AnimatedContent(
                    targetState = value,
                    transitionSpec = { Motion.valueSwap(swapAnim3, 145, 100) },
                    label = "stats-metric-value",
                ) { displayedValue ->
                    Text(displayedValue, style = Flow.type.heading, color = Flow.colors.textPrimary, maxLines = 1)
                }
                Text(
                    label,
                    style = Flow.type.caption,
                    color = Flow.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FocusBarChart(buckets: List<Bucket>, labelEvery: Int) {
    val c = Flow.colors
    val animated = LocalGlass.current.animations
    val progress = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        if (animated) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(650))
        } else {
            progress.snapTo(1f)
        }
    }
    val maxValue = (buckets.maxOfOrNull { it.minutes } ?: 0L).coerceAtLeast(1L)
    Canvas(Modifier.fillMaxWidth().height(148.dp)) {
        val top = 6.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        val chartHeight = bottom - top
        for (step in 0..3) {
            val y = top + chartHeight * step / 3f
            drawLine(c.track, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val count = buckets.size.coerceAtLeast(1)
        val slot = size.width / count
        val barWidth = (slot * 0.54f).coerceAtMost(24.dp.toPx()).coerceAtLeast(4.dp.toPx())
        buckets.forEachIndexed { index, bucket ->
            val fraction = bucket.minutes.toFloat() / maxValue
            val barHeight = if (bucket.minutes > 0L) {
                (chartHeight * fraction * progress.value).coerceAtLeast(3.dp.toPx())
            } else 2.dp.toPx()
            val x = size.width - slot * (index + 0.5f) - barWidth / 2f
            drawRoundRect(
                brush = if (bucket.minutes > 0L) {
                    Brush.verticalGradient(listOf(c.accentAlt, c.accent), top, bottom)
                } else Brush.verticalGradient(listOf(c.track, c.track)),
                topLeft = Offset(x, bottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        buckets.forEachIndexed { index, bucket ->
            Text(
                if (index % labelEvery == 0 || index == buckets.lastIndex) num(bucket.label) else "",
                Modifier.weight(1f),
                style = Flow.type.caption,
                color = c.textSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeeklyTrendChart(values: List<Long>) {
    val c = Flow.colors
    val animated = LocalGlass.current.animations
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        if (animated) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(780))
        } else {
            progress.snapTo(1f)
        }
    }
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
    Canvas(Modifier.fillMaxWidth().height(132.dp)) {
        if (values.size < 2) return@Canvas
        val pad = 9.dp.toPx()
        val width = size.width - 2f * pad
        val height = size.height - 2f * pad
        val bottom = size.height - pad
        fun point(index: Int): Offset {
            val x = size.width - pad - width * index / (values.size - 1)
            val y = pad + height * (1f - values[index] / maxValue)
            return Offset(x, y)
        }
        for (line in 0..3) {
            val y = pad + height * line / 3f
            drawLine(c.track, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
        }
        val stroke = Path()
        val area = Path()
        values.indices.forEach { index ->
            val current = point(index)
            if (index == 0) {
                stroke.moveTo(current.x, current.y)
                area.moveTo(current.x, bottom)
                area.lineTo(current.x, current.y)
            } else {
                val previous = point(index - 1)
                val middle = (previous.x + current.x) / 2f
                stroke.cubicTo(middle, previous.y, middle, current.y, current.x, current.y)
                area.cubicTo(middle, previous.y, middle, current.y, current.x, current.y)
            }
        }
        area.lineTo(point(values.lastIndex).x, bottom)
        area.close()
        clipRect(left = size.width * (1f - progress.value)) {
            drawPath(area, Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.28f), Color.Transparent)))
            drawPath(
                stroke,
                Brush.horizontalGradient(listOf(c.accentAlt, c.accent)),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            values.indices.forEach { index ->
                drawCircle(c.accent, 3.6.dp.toPx(), point(index))
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(categories: List<CategoryStat>) {
    val c = Flow.colors
    val total = categories.sumOf { it.minutes }.coerceAtLeast(1L)
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CategoryDonut(categories, Modifier.size(142.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            categories.take(5).forEach { item ->
                val category = item.categoryId?.let { Categories.byId(it) }
                val color = category?.let { Color(it.color) } ?: c.textSecondary
                val label = category?.name ?: "بدون وظیفه"
                val percent = (item.minutes * 100L / total).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Column(
                        Modifier.weight(1f).padding(start = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Text(label, style = Flow.type.caption, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(num(formatMinutes(item.minutes)), style = Flow.type.caption, color = c.textSecondary)
                    }
                    Text(num(percent.toString() + "%"), style = Flow.type.caption, color = c.textSecondary)
                }
            }
            if (categories.size > 5) {
                Text(
                    num("و " + (categories.size - 5) + " دسته‌ی دیگر"),
                    style = Flow.type.caption,
                    color = c.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun CategoryDonut(categories: List<CategoryStat>, modifier: Modifier = Modifier) {
    val c = Flow.colors
    Canvas(modifier) {
        val strokeWidth = 19.dp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val origin = Offset(inset, inset)
        drawArc(
            color = c.track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = origin,
            size = arcSize,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        val total = categories.sumOf { it.minutes }.coerceAtLeast(1L).toFloat()
        var angle = -90f
        categories.forEach { item ->
            val categoryColor = item.categoryId?.let { Color(Categories.byId(it).color) } ?: c.textSecondary
            val rawSweep = 360f * item.minutes / total
            val sweep = (rawSweep - if (categories.size > 1) 2.6f else 0f).coerceAtLeast(0f)
            if (sweep > 0f) {
                drawArc(
                    color = categoryColor,
                    startAngle = angle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = origin,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
            angle += rawSweep
        }
    }
}
