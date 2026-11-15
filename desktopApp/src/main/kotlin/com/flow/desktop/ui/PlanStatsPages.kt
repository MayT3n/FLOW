package com.flow.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flow.desktop.DesktopIranianCalendar
import com.flow.desktop.DesktopRuntime
import com.flow.desktop.design.*
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.domain.Planner
import com.flow.focus.domain.Stats
import com.flow.focus.domain.StatsRange
import java.time.LocalDate

// ───────────────────────── Planning / Calendar ─────────────────────────

@Composable
internal fun PlanningPage(data: AppData, runtime: DesktopRuntime) {
    val pal = Flow.colors
    val today = remember { LocalDate.now() }
    var selected by remember { mutableStateOf(today) }
    var month by remember { mutableStateOf(JalaliDate.of(today).withDay(1)) }
    var quick by remember { mutableStateOf("") }
    val gridStart = remember(month) { JalaliDate.startOfWeek(month.toLocalDate()) }
    val weeks = remember(gridStart, month) {
        val rows = (0 until 42).map { gridStart.plusDays(it.toLong()) }.chunked(7)
        // Trim a fully-outside trailing week so short months don't leave an empty row.
        rows.filterIndexed { i, w -> i < 5 || w.any { JalaliDate.of(it).month == month.month } }
    }
    val entries = remember(data.tasks, data.routines, data.routineChecks, selected) { Planner.entriesFor(data, selected) }
    val occasions = remember(selected) { DesktopIranianCalendar.on(selected) }
    val persian = LocalPersianDigits.current

    fun go(delta: Int) {
        month = month.plusMonths(delta)
        selected = month.toLocalDate()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= 980.dp
        val calendar: @Composable (Modifier) -> Unit = { m ->
            GlassPanel(m, verticalSpacing = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowText(numText(DateFormats.monthYear(month)), Modifier.weight(1f), FlowType.title1)
                    FlowButton("امروز", { month = JalaliDate.of(today).withDay(1); selected = today }, kind = ButtonKind.Ghost, compact = true)
                    // RTL: "previous" points to the right.
                    FlowIconButton(FlowIcon.ChevronRight, "ماه قبل", { go(-1) })
                    FlowIconButton(FlowIcon.ChevronLeft, "ماه بعد", { go(1) })
                }
                Row(Modifier.fillMaxWidth()) {
                    JalaliDate.weekdayNames.forEachIndexed { i, n ->
                        FlowText(
                            n, Modifier.weight(1f),
                            FlowType.caption, if (i == 6) pal.danger.copy(alpha = .85f) else pal.textTertiary, maxLines = 1, textAlign = TextAlign.Center,
                        )
                    }
                }
                Column(Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    weeks.forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { date ->
                                DayCell(date, month, selected, today, data, Modifier.weight(1f)) { selected = date }
                            }
                        }
                    }
                }
                val years = DesktopIranianCalendar.supportedJalaliYears()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(pal.accent, "دارای برنامه"); Legend(pal.danger, "تعطیل رسمی"); Legend(pal.warning, "مناسبت")
                    Spacer(Modifier.weight(1f))
                    FlowText("مناسبت‌های آفلاین: ${numText(years.joinToString("، "))}", style = FlowType.caption, color = pal.textTertiary)
                }
            }
        }
        val details: @Composable (Modifier) -> Unit = { m ->
            GlassPanel(m, verticalSpacing = 10.dp) {
                PanelHeader(numText(DateFormats.full(selected)), FlowIcon.Calendar)
                if (selected == today) FlowText("امروز", style = FlowType.captionMedium, color = pal.accent)
                occasions.forEach { ev ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background((if (ev.isHoliday) pal.danger else pal.warning).copy(alpha = .12f)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FlowText(if (ev.isHoliday) "تعطیل" else "مناسبت", style = FlowType.captionMedium, color = if (ev.isHoliday) pal.danger else pal.warning)
                        FlowText(ev.titleFa, Modifier.weight(1f), FlowType.callout, maxLines = 2)
                    }
                }
                if (entries.isEmpty()) EmptyState(FlowIcon.Calendar, "برای این روز برنامه‌ای نیست", "یک وظیفه‌ی سریع برای همین روز اضافه کن.")
                entries.forEach { e ->
                    val cat = Categories.byId(e.categoryId)
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (e.done) pal.success else Color(cat.color)))
                        Column(Modifier.weight(1f)) {
                            FlowText(e.title, style = FlowType.bodyMedium, color = if (e.done) pal.textTertiary else pal.text, maxLines = 1)
                            FlowText(if (e.done) "انجام شده" else "در انتظار · ${cat.name}", style = FlowType.caption, color = pal.textTertiary)
                        }
                        e.minute?.let { FlowText(clockText(it, persian), style = FlowType.caption, color = pal.textSecondary) }
                    }
                }
                Hairline()
                FlowTextField(
                    quick, { quick = it }, "افزودن وظیفه برای این روز…", leading = FlowIcon.Plus,
                    onEnter = { if (quick.isNotBlank()) { runtime.addTask(quick, dueDate = selected); quick = "" } },
                )
            }
        }
        if (split) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            calendar(Modifier.weight(1.6f).fillMaxHeight().verticalScroll(rememberScrollState()))
            details(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            calendar(Modifier.fillMaxWidth()); details(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        FlowText(label, style = FlowType.caption, color = Flow.colors.textSecondary)
    }
}

@Composable
private fun DayCell(date: LocalDate, month: JalaliDate, selected: LocalDate, today: LocalDate, data: AppData, modifier: Modifier, onClick: () -> Unit) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val j = JalaliDate.of(date)
    val inMonth = j.month == month.month && j.year == month.year
    val isSel = date == selected
    val isToday = date == today
    val occ = remember(date) { DesktopIranianCalendar.on(date) }
    val holiday = occ.any { it.isHoliday } || JalaliDate.weekdayIndex(date) == 6
    val planned = Planner.countOn(data, date) > 0
    val ph = rememberPressHover()
    val bg by animateColorAsState(
        when { isSel -> pal.accent; ph.hovered -> pal.fillHover; else -> Color.Transparent },
        tween(FlowMotion.ms(FlowMotion.hover, reduce)), label = "day-bg",
    )
    Column(
        modifier.height(54.dp).clip(RoundedCornerShape(FlowDims.cardRadius)).background(bg)
            .then(if (isToday && !isSel) Modifier.border(1.5.dp, pal.accent, RoundedCornerShape(FlowDims.cardRadius)) else Modifier)
            .flowClickable(ph, true, onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        FlowText(
            num(j.day),
            style = if (isSel || isToday) FlowType.headline else FlowType.body,
            color = when {
                isSel -> pal.onAccent
                !inMonth -> pal.textTertiary.copy(alpha = .5f)
                holiday -> pal.danger
                else -> pal.text
            },
        )
        Row(Modifier.height(7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (planned) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) pal.onAccent else pal.accent))
            if (occ.any { !it.isHoliday }) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) pal.onAccent else pal.warning))
            if (occ.any { it.isHoliday }) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) pal.onAccent else pal.danger))
        }
    }
}

// ───────────────────────── Statistics ─────────────────────────

@Composable
internal fun StatsPage(data: AppData) {
    val pal = Flow.colors
    var range by remember { mutableStateOf(StatsRange.WEEK) }
    val today = remember { LocalDate.now() }
    val s = remember(data.sessions, data.tasks, range, today) { Stats.summarize(data, range, today) }
    val labels = remember(s, range) {
        when (range) {
            StatsRange.DAY -> s.buckets.map { b -> "${b.label}h" }
            else -> s.buckets.map { it.label }
        }
    }
    val rangeName = when (range) { StatsRange.DAY -> "امروز"; StatsRange.WEEK -> "این هفته"; StatsRange.MONTH -> "این ماه" }
    val totalCat = s.categories.sumOf { it.minutes }.coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= 1000.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Segmented(listOf(StatsRange.DAY to "روز", StatsRange.WEEK to "هفته", StatsRange.MONTH to "ماه"), range, { range = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                BigStat(FlowIcon.Timer, "زمان تمرکز", num(s.focusMinutes), "دقیقه · $rangeName", Modifier.weight(1f))
                BigStat(FlowIcon.CheckCircle, "جلسه‌ی کامل", num(s.completedSessions), "میانگین ${num(s.avgSessionMinutes)} دقیقه", Modifier.weight(1f))
                BigStat(FlowIcon.Flag, "روزهای فعال", num(s.activeDays), "روز", Modifier.weight(1f))
                BigStat(FlowIcon.Tasks, "وظایف انجام‌شده", num(s.tasksDone), rangeName, Modifier.weight(1f))
            }
            val trend: @Composable (Modifier) -> Unit = { m ->
                GlassPanel(m) {
                    PanelHeader("روند تمرکز — $rangeName", FlowIcon.Chart)
                    if (s.buckets.all { it.minutes == 0L }) EmptyState(FlowIcon.Chart, "هنوز داده‌ی کافی نیست", "پس از ثبت جلسه‌های تمرکز، نمودار اینجا کامل می‌شود.")
                    else BarChart(
                        s.buckets.map { it.minutes }, labels, Modifier.fillMaxWidth(), height = 190.dp,
                        highlight = if (range == StatsRange.DAY) null else s.buckets.lastIndex,
                    )
                }
            }
            val weekly: @Composable (Modifier) -> Unit = { m ->
                GlassPanel(m) {
                    PanelHeader("هشت هفته‌ی اخیر", FlowIcon.Layers)
                    if (s.weeklyTrend.all { it == 0L }) EmptyState(FlowIcon.Layers, "هنوز روندی نیست", "با ادامه‌ی تمرکز، مقایسه‌ی هفتگی دیده می‌شود.")
                    else {
                        TrendChart(s.weeklyTrend, listOf("۷ هفته پیش", "۴ هفته پیش", "اکنون"), Modifier.fillMaxWidth(), height = 160.dp)
                        FlowText("مجموع دقیقه‌های تمرکز در هر هفته (جدیدترین در سمت راست)", style = FlowType.caption, color = pal.textTertiary)
                    }
                }
            }
            if (split) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                trend(Modifier.weight(1.3f)); weekly(Modifier.weight(1f))
            } else { trend(Modifier.fillMaxWidth()); weekly(Modifier.fillMaxWidth()) }
            GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 10.dp) {
                PanelHeader("تمرکز به تفکیک دسته", FlowIcon.Palette)
                if (s.categories.isEmpty()) FlowText("جلسه‌ای که به یک وظیفه وصل باشد در این بازه ثبت نشده است.", style = FlowType.caption, color = pal.textSecondary)
                s.categories.forEach { c ->
                    val cat = Categories.byId(c.categoryId)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FlowText(if (c.categoryId == null) "بدون دسته" else cat.name, Modifier.width(80.dp), FlowType.callout, maxLines = 1)
                        HBar(c.minutes.toFloat() / totalCat, if (c.categoryId == null) pal.textTertiary else Color(cat.color), Modifier.weight(1f), height = 10.dp)
                        FlowText("${num(c.minutes)} دقیقه · ${num(c.minutes * 100 / totalCat)}٪", Modifier.width(120.dp), FlowType.caption, pal.textSecondary, textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStat(icon: FlowIcon, label: String, value: String, hint: String, modifier: Modifier) {
    val pal = Flow.colors
    Column(modifier.glass(FlowDims.panelRadius).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowIconView(icon, pal.accent, size = 17.dp)
            FlowText(label, style = FlowType.callout, color = pal.textSecondary, maxLines = 1)
        }
        FlowText(value, style = FlowType.largeTitle, maxLines = 1)
        FlowText(hint, style = FlowType.caption, color = pal.textTertiary, maxLines = 1)
    }
}
