package com.flow.desktop.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.flow.desktop.DesktopIranianCalendar
import com.flow.desktop.DesktopMusicCatalog
import com.flow.desktop.DesktopRuntime
import com.flow.desktop.DesktopTrack
import com.flow.desktop.design.*
import com.flow.focus.core.DateFormats
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.data.Priority
import com.flow.focus.data.Task
import com.flow.focus.domain.Planner
import com.flow.focus.domain.Stats
import com.flow.focus.domain.StatsRange
import com.flow.focus.domain.TaskFilter
import com.flow.focus.domain.TaskQuery
import com.flow.focus.domain.TaskSort
import java.time.LocalDate

internal fun greetingFor(hour: Int): Pair<String, FlowIcon> = when (hour) {
    in 5..11 -> "صبح بخیر" to FlowIcon.Sun
    in 12..16 -> "ظهر بخیر" to FlowIcon.Sun
    in 17..20 -> "عصر بخیر" to FlowIcon.Sun
    else -> "شب بخیر" to FlowIcon.Moon
}

internal fun priorityColor(p: Priority, pal: FlowPalette) = when (p) {
    Priority.HIGH -> pal.danger
    Priority.MEDIUM -> pal.warning
    Priority.LOW -> pal.success
}

/** Plays a track, downloading it first when it is not on disk yet (same behaviour as before). */
internal fun playOrDownload(runtime: DesktopRuntime, track: DesktopTrack) {
    if (runtime.music.isDownloaded(track)) runtime.music.play(track) else runtime.music.download(track)
}

@Composable
private fun StatTile(icon: FlowIcon, label: String, value: String, hint: String, modifier: Modifier = Modifier) {
    val pal = Flow.colors
    Row(modifier.glass(FlowDims.cardRadius + 2.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(pal.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            FlowIconView(icon, pal.accent, size = 21.dp)
        }
        Column(Modifier.weight(1f)) {
            FlowText(label, style = FlowType.caption, color = pal.textSecondary, maxLines = 1)
            FlowText(value, style = FlowType.numeral, maxLines = 1)
            FlowText(hint, style = FlowType.caption, color = pal.textTertiary, maxLines = 1)
        }
    }
}

@Composable
internal fun DashboardPage(data: AppData, now: Long, runtime: DesktopRuntime, onNavigate: (Page) -> Unit) {
    val pal = Flow.colors
    val today = remember(now / 60_000) { LocalDate.now() }
    val dayStats = remember(data.sessions, data.tasks, today) { Stats.summarize(data, StatsRange.DAY, today) }
    val weekStats = remember(data.sessions, data.tasks, today) { Stats.summarize(data, StatsRange.WEEK, today) }
    val openTasks = data.tasks.count { !it.done }
    val (greet, greetIcon) = greetingFor(java.time.LocalTime.now().hour)
    val goalMin = data.settings.dailyGoalMin.coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1180.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            // Today status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                StatTile(FlowIcon.Timer, "زمان تمرکز امروز", num(dayStats.focusMinutes) + " دقیقه", "هدف روزانه ${num(goalMin)} دقیقه", Modifier.weight(1f))
                StatTile(FlowIcon.Tasks, "وظایف باقی‌مانده", num(openTasks), if (openTasks == 0) "همه انجام شده" else "در انتظار انجام", Modifier.weight(1f))
                StatTile(FlowIcon.CheckCircle, "جلسه‌های کامل", num(dayStats.completedSessions), "جلسه‌ی امروز", Modifier.weight(1f))
            }
            if (wide) {
                val rowH = (this@BoxWithConstraints.maxHeight - 112.dp).coerceIn(560.dp, 720.dp)
                Row(Modifier.fillMaxWidth().height(rowH), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                        TodayTasksPanel(data, runtime, onNavigate, Modifier.weight(1.15f).fillMaxWidth())
                        AgendaPanel(data, today, onNavigate, Modifier.weight(1f).fillMaxWidth())
                    }
                    TimerCard(data, now, runtime, Modifier.weight(1f).fillMaxHeight(), ringSize = if (rowH > 680.dp) 270.dp else 230.dp, digitSp = if (rowH > 680.dp) 54 else 46, showPhaseSwitcher = true, showTaskPicker = true)
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                        MusicCard(runtime, data, onNavigate)
                        WeeklyCard(weekStats.buckets.map { it.minutes }, weekStats.buckets.map { it.label }, today, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().height(500.dp), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                    TimerCard(data, now, runtime, Modifier.weight(1f).fillMaxHeight(), ringSize = 200.dp, digitSp = 40)
                    TodayTasksPanel(data, runtime, onNavigate, Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                        MusicCard(runtime, data, onNavigate)
                        AgendaPanel(data, today, onNavigate, Modifier.fillMaxWidth())
                    }
                    WeeklyCard(weekStats.buckets.map { it.minutes }, weekStats.buckets.map { it.label }, today, Modifier.weight(1f).heightIn(min = 280.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TodayTasksPanel(data: AppData, runtime: DesktopRuntime, onNavigate: (Page) -> Unit, modifier: Modifier) {
    val pal = Flow.colors
    val todayEpoch = LocalDate.now().toEpochDay()
    val list = remember(data.tasks, todayEpoch) {
        val todays = data.tasks.filter { it.dueDay == todayEpoch }.sortedWith(compareBy({ it.dueMinute ?: Int.MAX_VALUE }, { it.createdAt }))
        val overdue = TaskQuery.apply(data.tasks, TaskFilter.TODAY, TaskSort.DUE, null, "", todayEpoch).filter { it.dueDay != todayEpoch }
        val upcoming = TaskQuery.apply(data.tasks, TaskFilter.UPCOMING, TaskSort.DUE, null, "", todayEpoch)
        (overdue + todays + upcoming).take(6)
    }
    val (done, total) = remember(data.tasks, todayEpoch) { TaskQuery.todayProgress(data.tasks, todayEpoch) }
    GlassPanel(modifier) {
        PanelHeader("کارهای امروز", FlowIcon.CheckCircle) {
            if (total > 0) FlowText("${num(done)} از ${num(total)}", style = FlowType.captionMedium, color = pal.textSecondary)
            FlowButton("همه", { onNavigate(Page.Tasks) }, kind = ButtonKind.Ghost, compact = true)
        }
        if (list.isEmpty()) {
            EmptyState(FlowIcon.Check, "کار بازی نداری", "وظیفه‌ی جدید را از صفحه‌ی وظایف اضافه کن.", Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                list.forEach { task -> TaskMiniRow(task, runtime) }
            }
        }
    }
}

@Composable
private fun TaskMiniRow(task: Task, runtime: DesktopRuntime) {
    val pal = Flow.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(FlowDims.controlRadius)).padding(horizontal = 4.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowCheck(task.done, { runtime.setTaskDone(task.id, it) })
        Column(Modifier.weight(1f)) {
            FlowText(task.title, style = FlowType.bodyMedium, color = if (task.done) pal.textTertiary else pal.text, maxLines = 1)
            val due = task.dueDay?.let { numText(DateFormats.short(LocalDate.ofEpochDay(it))) } ?: "بدون سررسید"
            FlowText(due + (task.dueMinute?.let { " · " + clockText(it, LocalPersianDigits.current) } ?: ""), style = FlowType.caption, color = pal.textTertiary, maxLines = 1)
        }
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(priorityColor(task.priority, pal).copy(alpha = .14f)).padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(priorityColor(task.priority, pal)))
            FlowText(task.priority.label, style = FlowType.caption, color = pal.textSecondary)
        }
    }
}

@Composable
private fun MusicCard(runtime: DesktopRuntime, data: AppData, onNavigate: (Page) -> Unit) {
    val pal = Flow.colors
    val state by runtime.music.state.collectAsState()
    val track = DesktopMusicCatalog.tracks.firstOrNull { it.id == (state.trackId ?: data.settings.musicTrackId) } ?: DesktopMusicCatalog.tracks.first()
    val favorites by runtime.music.favorites.collectAsState()
    val isCurrent = state.trackId == track.id
    val downloads by runtime.music.downloads.collectAsState()
    val onDisk = remember(track.id, downloads.containsKey(track.id)) { runtime.music.isDownloaded(track) }
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoverArt(track.id, size = 64.dp, radius = 14.dp, playing = isCurrent && state.playing)
            Column(Modifier.weight(1f)) {
                FlowText(track.title, style = FlowType.title2, maxLines = 1)
                FlowText(DesktopMusicCatalog.artist, style = FlowType.caption, color = pal.textSecondary, maxLines = 1)
                FlowText(track.mood, style = FlowType.caption, color = pal.textTertiary, maxLines = 1)
            }
            FlowIconButton(
                FlowIcon.Heart, if (track.id in favorites) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                { runtime.music.toggleFavorite(track.id) }, selected = track.id in favorites, filledIcon = track.id in favorites,
            )
        }
        MusicProgress(runtime, isCurrent)
        LtrRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FlowIconButton(FlowIcon.Shuffle, "پخش تصادفی", { runtime.music.toggleShuffle() }, selected = state.shuffle)
            FlowIconButton(FlowIcon.Previous, "قبلی", { runtime.music.previous() }, enabled = state.trackId != null, filledIcon = true)
            val playing = isCurrent && state.playing
            FlowButton(
                label = if (playing) "مکث" else if (isCurrent) "ادامه" else if (onDisk) "پخش" else "دانلود و پخش",
                onClick = { if (isCurrent) runtime.music.togglePlayback() else playOrDownload(runtime, track) },
                kind = ButtonKind.Primary, icon = if (playing) FlowIcon.Pause else FlowIcon.Play, compact = true,
            )
            FlowIconButton(FlowIcon.Next, "بعدی", { runtime.music.next() }, enabled = state.trackId != null, filledIcon = true)
            FlowIconButton(
                if (state.repeatMode == 2) FlowIcon.RepeatOne else FlowIcon.Repeat,
                when (state.repeatMode) { 1 -> "تکرار فهرست"; 2 -> "تکرار آهنگ"; else -> "تکرار خاموش" },
                { runtime.music.cycleRepeatMode() }, selected = state.repeatMode != 0,
            )
        }
    }
}

@Composable
internal fun MusicProgress(runtime: DesktopRuntime, active: Boolean, modifier: Modifier = Modifier) {
    val pal = Flow.colors
    val state by runtime.music.state.collectAsState()
    val pos = if (active) state.positionMs else 0L
    val dur = if (active) state.durationMs else 0L
    Column(modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        FlowSlider(
            value = if (dur > 0) pos.toFloat() / dur else 0f,
            onChange = { if (active && dur > 0) runtime.music.seekTo((it * dur).toLong()) },
            modifier = Modifier.fillMaxWidth(), enabled = active && dur > 0,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FlowText(timeLabel(pos), style = FlowType.caption, color = pal.textTertiary)
            FlowText(timeLabel(dur), style = FlowType.caption, color = pal.textTertiary)
        }
    }
}

@Composable
private fun WeeklyCard(values: List<Long>, labels: List<String>, today: LocalDate, modifier: Modifier) {
    val idx = values.lastIndex
    GlassPanel(modifier) {
        PanelHeader("فعالیت هفتگی", FlowIcon.Chart)
        if (values.all { it == 0L }) {
            EmptyState(FlowIcon.Chart, "هنوز جلسه‌ای ثبت نشده", "با اولین جلسه‌ی تمرکز، روند هفته اینجا نمایش داده می‌شود.", Modifier.weight(1f))
        } else {
            BarChart(values, labels, Modifier.fillMaxWidth().weight(1f), highlight = idx, height = 130.dp)
        }
    }
}

@Composable
private fun AgendaPanel(data: AppData, today: LocalDate, onNavigate: (Page) -> Unit, modifier: Modifier) {
    val pal = Flow.colors
    val entries = remember(data.tasks, data.routines, data.routineChecks, today) { Planner.entriesFor(data, today) }
    val occasions = remember(today) { DesktopIranianCalendar.on(today) }
    GlassPanel(modifier) {
        PanelHeader("برنامه‌ی امروز", FlowIcon.Calendar) {
            FlowButton("تقویم", { onNavigate(Page.Planning) }, kind = ButtonKind.Ghost, compact = true)
        }
        occasions.forEach { ev ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (ev.isHoliday) pal.danger else pal.accent))
                FlowText(ev.titleFa, Modifier.weight(1f), FlowType.callout, if (ev.isHoliday) pal.danger else pal.textSecondary, maxLines = 1)
            }
        }
        if (entries.isEmpty() && occasions.isEmpty()) {
            EmptyState(FlowIcon.Calendar, "برنامه‌ای برای امروز نیست", "وظیفه یا روتین با سررسید امروز اینجا دیده می‌شود.")
        }
        entries.take(5).forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowText(e.minute?.let { clockText(it, LocalPersianDigits.current) } ?: "—", Modifier.width(46.dp), FlowType.caption, pal.textSecondary)
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (e.done) pal.success else Color(Categories.byId(e.categoryId).color)))
                Column(Modifier.weight(1f)) {
                    FlowText(e.title, style = FlowType.bodyMedium, color = if (e.done) pal.textTertiary else pal.text, maxLines = 1)
                    FlowText(Categories.byId(e.categoryId).name, style = FlowType.caption, color = pal.textTertiary)
                }
            }
        }
    }
}

// ───────────────────────── Focus page ─────────────────────────

@Composable
internal fun FocusPage(data: AppData, now: Long, runtime: DesktopRuntime) {
    val pal = Flow.colors
    val today = LocalDate.now()
    val dayStats = remember(data.sessions, today) { Stats.summarize(data, StatsRange.DAY, today) }
    val task = data.tasks.firstOrNull { it.id == data.timer.taskId }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= 980.dp
        val ring = if (maxHeight < 640.dp) 230.dp else 290.dp
        val side: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    PanelHeader("امروز", FlowIcon.Sparkle)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            FlowText(num(dayStats.focusMinutes), style = FlowType.numeral)
                            FlowText("دقیقه تمرکز", style = FlowType.caption, color = pal.textSecondary)
                        }
                        Column(Modifier.weight(1f)) {
                            FlowText(num(dayStats.completedSessions), style = FlowType.numeral)
                            FlowText("جلسه‌ی کامل", style = FlowType.caption, color = pal.textSecondary)
                        }
                    }
                    val goal = data.settings.dailyGoalMin.coerceAtLeast(1)
                    HBar(dayStats.focusMinutes.toFloat() / goal, pal.accent)
                    FlowText("هدف روزانه: ${num(goal)} دقیقه", style = FlowType.caption, color = pal.textTertiary)
                }
                GlassPanel(Modifier.fillMaxWidth()) {
                    PanelHeader("وظیفه‌ی در حال تمرکز", FlowIcon.Tasks)
                    if (task == null) FlowText("وظیفه‌ای انتخاب نشده است؛ از فهرست زیر تایمر انتخاب کن.", style = FlowType.caption, color = pal.textSecondary)
                    else {
                        FlowText(task.title, style = FlowType.headline)
                        val done = com.flow.focus.domain.Stats.pomodorosDone(data.sessions, task.id)
                        FlowText("${num(done)} از ${num(task.estimatedPomodoros)} پومودورو انجام شده", style = FlowType.caption, color = pal.textSecondary)
                        HBar(done.toFloat() / task.estimatedPomodoros.coerceAtLeast(1), pal.success)
                    }
                }
                GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 6.dp) {
                    PanelHeader("میانبرها", FlowIcon.Info)
                    listOf("Ctrl+1…9" to "رفتن به صفحه‌ها", "Ctrl+K" to "جست‌وجوی سریع", "Space" to "شروع / مکث تایمر", "Ctrl+R" to "بازنشانی").forEach { (k, d) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            FlowText(d, style = FlowType.caption, color = pal.textSecondary)
                            FlowText(ltr(k), style = FlowType.caption, color = pal.textTertiary)
                        }
                    }
                }
            }
        }
        if (split) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                TimerCard(data, now, runtime, Modifier.weight(1.35f).fillMaxHeight(), ringSize = ring, digitSp = if (ring > 250.dp) 58 else 48)
                side(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                TimerCard(data, now, runtime, Modifier.fillMaxWidth().height(560.dp), ringSize = 240.dp, digitSp = 50)
                side(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun LtrRow(modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, content: @Composable RowScope.() -> Unit) {
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Row(modifier, horizontalArrangement = horizontalArrangement, verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
