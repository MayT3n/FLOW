package com.flow.focus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.data.GoalPeriod
import com.flow.focus.data.Routine
import com.flow.focus.data.Task
import com.flow.focus.domain.PlanEntry
import com.flow.focus.domain.Planner
import com.flow.focus.domain.RoutineEntry
import com.flow.focus.domain.Stats
import com.flow.focus.domain.TaskEntry
import com.flow.focus.domain.TaskQuery
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.CheckCircle
import com.flow.focus.ui.components.Chip
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.Fab
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.FlowTextField
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.GlassMenu
import com.flow.focus.ui.components.JalaliDatePickerDialog
import com.flow.focus.ui.components.MenuAction
import com.flow.focus.ui.components.PlannerCalendar
import com.flow.focus.ui.components.ScreenHeader
import com.flow.focus.ui.components.SectionTitle
import com.flow.focus.ui.components.num
import com.flow.focus.ui.durationLabel
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.motion.Appear
import com.flow.focus.ui.motion.Motion
import com.flow.focus.ui.motion.rememberEnterState
import com.flow.focus.ui.relativeDay
import com.flow.focus.ui.rememberToday
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalDockContentPadding
import java.time.LocalDate

/**
 * برنامه‌ریزی یکپارچه: تقویم (ماهانه/هفتگی قابل جمع‌شدن)، جدول زمانی روز، کارهای بدون زمان، عقب‌افتاده‌ها،
 * کارهای بدون تاریخ، اهداف و روتین‌های همان روز در یک صفحه. وظایف یکی از انواع فعالیت‌اند.
 */
@Composable
fun PlanningScreen(vm: AppViewModel, data: AppData, onStartFocus: () -> Unit, modifier: Modifier = Modifier) {
    val today = rememberToday()
    val offlineOccasions by vm.offlineCalendarOccasions.collectAsStateWithLifecycle()
    val c = Flow.colors
    val animations = Flow.glass.animations

    var calendarExpanded by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var monthAnchor by rememberSaveable { mutableStateOf(firstOfMonth(today).toEpochDay()) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    var moveTask by remember { mutableStateOf<Task?>(null) }
    var runningRoutine by remember { mutableStateOf<Routine?>(null) }

    val selDate = LocalDate.ofEpochDay(selected)

    // تقویم همیشه ماهِ روز انتخابی را نشان می‌دهد، مگر کاربر ماه را جابه‌جا کند.
    LaunchedEffect(selected) { monthAnchor = firstOfMonth(selDate).toEpochDay() }

    val occasionsByDay = remember(offlineOccasions) { offlineOccasions.groupBy { it.epochDay } }
    val dots = remember(data.tasks, data.routines, occasionsByDay, monthAnchor, selected, c.isDark) {
        buildMap<Long, List<Color>> {
            fun add(date: LocalDate) {
                val key = date.toEpochDay()
                if (key in this) return
                val entryColors = Planner.entriesFor(data, date).map { Color(Categories.byId(it.categoryId).color) }
                val occasionColors = occasionsByDay[key].orEmpty().map { if (it.isHoliday) c.danger else c.accent }
                val colors = (entryColors + occasionColors).distinct().take(3)
                if (colors.isNotEmpty()) put(key, colors)
            }
            val anchor = JalaliDate.of(LocalDate.ofEpochDay(monthAnchor))
            for (day in 1..anchor.lengthOfMonth()) add(JalaliDate(anchor.year, anchor.month, day).toLocalDate())
            val weekStart = JalaliDate.startOfWeek(selDate)
            for (i in 0..6) add(weekStart.plusDays(i.toLong()))
        }
    }

    val q = remember(query) { TaskQuery.normalize(query) }
    fun matches(title: String, categoryId: String): Boolean =
        (category == null || categoryId == category) && (q.isEmpty() || TaskQuery.normalize(title).contains(q))

    val allEntries = remember(data.tasks, data.routines, data.routineChecks, selected) { Planner.entriesFor(data, selDate) }
    val entries = allEntries.filter { matches(it.title, it.categoryId) }
    val timed = entries.filter { it.minute != null }
    val untimed = entries.filter { it.minute == null }
    val overdueAll = remember(data.tasks, today) { Planner.overdue(data, today) }
    val overdue = if (selDate == today) overdueAll.filter { matches(it.title, it.categoryId) } else emptyList()
    val undated = remember(data.tasks) { Planner.undated(data) }.filter { matches(it.title, it.categoryId) }
    val (doneCount, totalCount) = Planner.summary(allEntries)
    val plannedMinutes = Planner.plannedMinutes(allEntries)

    val selectedOccasions = remember(occasionsByDay, selected) { occasionsByDay[selected].orEmpty() }
    val weekStart = JalaliDate.startOfWeek(selDate)

    fun goToday() {
        selected = today.toEpochDay()
        monthAnchor = firstOfMonth(today).toEpochDay()
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = LocalDockContentPadding.current + 72.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("برنامه‌ریزی", trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TodayPill(enabled = selDate != today, onClick = ::goToday)
                    GlassIconButton(
                        if (calendarExpanded) FlowIcons.ChevronUp else FlowIcons.ChevronDown,
                        { calendarExpanded = !calendarExpanded },
                        size = 38.dp, iconSize = 18.dp,
                        contentDescription = if (calendarExpanded) "جمع کردن تقویم" else "باز کردن تقویم",
                    )
                }
            })

            PlannerCalendar(
                expanded = calendarExpanded,
                selected = selDate,
                today = today,
                monthAnchor = monthAnchor,
                dotsByDay = dots,
                onSelect = { selected = it.toEpochDay() },
                onMonthAnchor = { monthAnchor = it },
            )

            DayHeader(selDate, today, doneCount, totalCount, plannedMinutes, onPickDate = { datePicker = true })

            FilterBar(
                category = category,
                onCategory = { category = it },
                searchOpen = searchOpen,
                onToggleSearch = { searchOpen = !searchOpen; if (!searchOpen) query = "" },
                query = query,
                onQuery = { query = it },
            )

            if (overdue.isNotEmpty()) {
                SectionTitle("عقب‌افتاده")
                PlanGroup {
                    overdue.forEachIndexed { index, task ->
                        PlanRow(
                            TaskEntry(task), selDate, index, showTime = false, dateHint = relativeDay(task.dueDay!!, today),
                            pomodoros = Stats.pomodorosDone(data.sessions, task.id),
                            vm = vm, onEdit = { editing = it }, onMove = { moveTask = it },
                            onFocus = { vm.selectTask(it.id); onStartFocus() }, onRunRoutine = { runningRoutine = it },
                            isLast = index == overdue.lastIndex,
                        )
                    }
                }
            }

            if (timed.isNotEmpty()) {
                SectionTitle("جدول روز")
                PlanGroup {
                    timed.forEachIndexed { index, entry ->
                        PlanRow(
                            entry, selDate, index, showTime = true, dateHint = null,
                            pomodoros = (entry as? TaskEntry)?.let { Stats.pomodorosDone(data.sessions, it.task.id) },
                            vm = vm, onEdit = { editing = it }, onMove = { moveTask = it },
                            onFocus = { vm.selectTask(it.id); onStartFocus() }, onRunRoutine = { runningRoutine = it },
                            isLast = index == timed.lastIndex,
                        )
                    }
                }
            }

            if (untimed.isNotEmpty()) {
                SectionTitle("بدون ساعت مشخص")
                PlanGroup {
                    untimed.forEachIndexed { index, entry ->
                        PlanRow(
                            entry, selDate, index, showTime = false, dateHint = null,
                            pomodoros = (entry as? TaskEntry)?.let { Stats.pomodorosDone(data.sessions, it.task.id) },
                            vm = vm, onEdit = { editing = it }, onMove = { moveTask = it },
                            onFocus = { vm.selectTask(it.id); onStartFocus() }, onRunRoutine = { runningRoutine = it },
                            isLast = index == untimed.lastIndex,
                        )
                    }
                }
            }

            if (entries.isEmpty() && overdue.isEmpty()) {
                EmptyState(
                    FlowIcons.Calendar,
                    if (q.isNotEmpty() || category != null) "موردی پیدا نشد" else "برنامه‌ای برای این روز نیست",
                    if (q.isNotEmpty() || category != null) "فیلتر یا عبارت جست‌وجو را تغییر بده." else "با دکمه‌ی + برنامه، رویداد یا وظیفه اضافه کن.",
                )
            }

            if (undated.isNotEmpty()) {
                SectionTitle("بدون تاریخ")
                PlanGroup {
                    undated.forEachIndexed { index, task ->
                        PlanRow(
                            TaskEntry(task), selDate, index, showTime = false, dateHint = null,
                            pomodoros = Stats.pomodorosDone(data.sessions, task.id),
                            vm = vm, onEdit = { editing = it }, onMove = { moveTask = it },
                            onFocus = { vm.selectTask(it.id); onStartFocus() }, onRunRoutine = { runningRoutine = it },
                            isLast = index == undated.lastIndex,
                        )
                    }
                }
            }

            if (selectedOccasions.isNotEmpty()) {
                OccasionsCard(selectedOccasions.map { it.titleFa to it.isHoliday })
            }

            GoalsCard(
                "اهداف روز",
                data.goals.filter { it.period == GoalPeriod.DAILY && it.periodStartDay == selected },
                onAdd = { vm.addGoal(it, GoalPeriod.DAILY, selected) }, onToggle = vm::toggleGoal, onDelete = vm::deleteGoal,
            )
            GoalsCard(
                "اهداف هفته",
                data.goals.filter { it.period == GoalPeriod.WEEKLY && it.periodStartDay == weekStart.toEpochDay() },
                onAdd = { vm.addGoal(it, GoalPeriod.WEEKLY, weekStart.toEpochDay()) }, onToggle = vm::toggleGoal, onDelete = vm::deleteGoal,
            )
        }

        // دکمه‌ی افزودن در لایه‌ی جدا از ستون اسکرول‌شونده است؛ جایگاهش هیچ اثری روی اندازه‌ی تقویم ندارد.
        Fab({ creating = true }, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = LocalDockContentPadding.current - 8.dp))
    }

    if (creating) {
        TaskEditorDialog(
            initial = null, template = Task("", "", dueDay = selected),
            onSave = { vm.saveTask(it); creating = false }, onDelete = null, onDismiss = { creating = false },
        )
    }
    editing?.let { t ->
        TaskEditorDialog(
            initial = t,
            onSave = { vm.saveTask(it); editing = null },
            onDelete = { vm.deleteTask(it.id); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (datePicker) {
        JalaliDatePickerDialog(selected, onPick = { selected = it; datePicker = false }, onDismiss = { datePicker = false })
    }
    moveTask?.let { t ->
        JalaliDatePickerDialog(t.dueDay, onPick = { vm.moveTask(t.id, it); moveTask = null }, onDismiss = { moveTask = null })
    }
    runningRoutine?.let { r ->
        RoutineRunnerDialog(
            routine = r,
            focusMin = data.settings.focusMin,
            onFinished = { vm.markRoutineDone(r.id, today.toEpochDay()) },
            onDismiss = { runningRoutine = null },
        )
    }
}

private fun firstOfMonth(d: LocalDate): LocalDate {
    val j = JalaliDate.of(d)
    return JalaliDate(j.year, j.month, 1).toLocalDate()
}

@Composable
private fun TodayPill(enabled: Boolean, onClick: () -> Unit) {
    val c = Flow.colors
    GlassSurface(
        Modifier.height(38.dp).springClick(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        level = GlassLevel.Subtle,
        tint = if (enabled) c.accent else null,
    ) {
        Box(Modifier.padding(horizontal = 14.dp).height(38.dp), contentAlignment = Alignment.Center) {
            Text("امروز", style = Flow.type.label, color = if (enabled) c.accent else c.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, today: LocalDate, done: Int, total: Int, plannedMinutes: Int, onPickDate: () -> Unit) {
    val c = Flow.colors
    GlassSurface(
        Modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = onPickDate),
        shape = RoundedCornerShape(22.dp),
        level = GlassLevel.Subtle,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    num(DateFormats.full(day)),
                    Modifier.weight(1f),
                    style = Flow.type.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (day == today) "امروز" else num(relativeDay(day.toEpochDay(), today)),
                    style = Flow.type.caption,
                    color = if (day == today) c.accent else c.textSecondary,
                    maxLines = 1,
                )
            }
            FlowProgressBar(if (total == 0) 0f else done.toFloat() / total, color = c.success, height = 5.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (total == 0) "برنامه‌ای ثبت نشده" else num("$done از $total انجام شد"),
                    style = Flow.type.caption, color = c.textSecondary, maxLines = 1,
                )
                if (plannedMinutes > 0) {
                    Text(num("زمان‌بندی‌شده: " + durationLabel(plannedMinutes)), style = Flow.type.caption, color = c.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    category: String?,
    onCategory: (String?) -> Unit,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
    query: String,
    onQuery: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(
                if (searchOpen) FlowIcons.Close else FlowIcons.Search, onToggleSearch,
                size = 36.dp, iconSize = 17.dp, active = searchOpen,
                contentDescription = if (searchOpen) "بستن جست‌وجو" else "جست‌وجو در برنامه",
            )
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("همه", category == null, { onCategory(null) })
                Categories.all.forEach { cat -> Chip(cat.name, category == cat.id, { onCategory(cat.id) }, dot = Color(cat.color)) }
            }
        }
        AnimatedVisibility(
            visibleState = rememberEnterState(searchOpen),
            enter = fadeIn(Motion.enter(Motion.Medium)) + expandVertically(Motion.soft()),
            exit = fadeOut(Motion.exit(Motion.Fast)) + shrinkVertically(Motion.exit(Motion.Short)),
        ) {
            FlowTextField(query, onQuery, "جست‌وجو در برنامه و وظایف", leading = FlowIcons.Search)
        }
    }
}

@Composable
private fun PlanGroup(content: @Composable () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), level = GlassLevel.Subtle) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 4.dp)) { content() }
    }
}

@Composable
private fun PlanRow(
    entry: PlanEntry,
    day: LocalDate,
    index: Int,
    showTime: Boolean,
    dateHint: String?,
    pomodoros: Int?,
    vm: AppViewModel,
    onEdit: (Task) -> Unit,
    onMove: (Task) -> Unit,
    onFocus: (Task) -> Unit,
    onRunRoutine: (Routine) -> Unit,
    isLast: Boolean,
) {
    val c = Flow.colors
    val cat = Categories.byId(entry.categoryId)
    var menu by remember { mutableStateOf(false) }
    val task = (entry as? TaskEntry)?.task
    val routine = (entry as? RoutineEntry)?.routine
    val start = entry.minute
    val duration = entry.durationMin

    Appear(delayMs = index.coerceAtMost(8) * 22) {
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .springClick(enabled = task != null, scaleDown = 0.99f, onClick = { if (task != null) onEdit(task) })
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showTime && start != null) {
                    Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(num(DateFormats.clock(start)), style = Flow.type.label, color = c.textPrimary, maxLines = 1)
                        if (duration != null) {
                            Text(num(DateFormats.clock((start + duration) % (24 * 60))), style = Flow.type.caption, color = c.textSecondary, maxLines = 1)
                        }
                    }
                    Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(Color(cat.color).copy(alpha = 0.85f)))
                }
                CheckCircle(
                    entry.done,
                    { if (task != null) vm.setTaskDone(task.id, !task.done) else if (routine != null) vm.toggleRoutine(routine.id, day.toEpochDay()) },
                    size = 26.dp, color = Color(cat.color), contentDescription = "تغییر وضعیت ${entry.title}",
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entry.title,
                        style = Flow.type.bodyStrong.copy(textDecoration = if (entry.done) TextDecoration.LineThrough else null),
                        color = if (entry.done) c.textSecondary else c.textPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    val kindLabel = task?.kind?.label ?: "روتین"
                    val pomodoroLabel = if (task != null && pomodoros != null && task.estimatedPomodoros > 0) " · $pomodoros/${task.estimatedPomodoros} جلسه" else ""
                    val timeLabel = if (!showTime && start != null) " · " + DateFormats.clock(start) else ""
                    val dateLabel = if (dateHint != null) " · $dateHint" else ""
                    val durationLabelText = if (duration != null && !showTime) " · " + durationLabel(duration) else ""
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (routine != null) FlowIcon(FlowIcons.Repeat, tint = c.textSecondary, size = 13.dp)
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(cat.color)))
                        Text(
                            num("$kindLabel · ${cat.name}$timeLabel$durationLabelText$pomodoroLabel$dateLabel"),
                            style = Flow.type.caption,
                            color = if (dateHint != null) c.danger else c.textSecondary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (task != null && !task.done) {
                    GlassIconButton(FlowIcons.Play, { onFocus(task) }, size = 34.dp, iconSize = 16.dp, contentDescription = "شروع تمرکز روی این مورد")
                }
                if (routine != null) {
                    GlassIconButton(FlowIcons.Play, { onRunRoutine(routine) }, size = 34.dp, iconSize = 16.dp, contentDescription = "اجرای روتین")
                }
                if (task != null) {
                    Box {
                        GlassIconButton(FlowIcons.More, { menu = true }, size = 34.dp, iconSize = 16.dp, contentDescription = "گزینه‌ها")
                        GlassMenu(
                            menu, { menu = false },
                            listOf(
                                MenuAction("ویرایش", FlowIcons.Edit) { onEdit(task) },
                                MenuAction("انتقال به تاریخ دیگر", FlowIcons.Move) { onMove(task) },
                                MenuAction("حذف", FlowIcons.Trash, destructive = true) { vm.deleteTask(task.id) },
                            ),
                        )
                    }
                }
            }
            if (!isLast) Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(1.dp).background(c.track))
        }
    }
}

@Composable
private fun OccasionsCard(items: List<Pair<String, Boolean>>) {
    val c = Flow.colors
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), level = GlassLevel.Subtle) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionTitle("مناسبت‌های این روز")
            items.forEach { (title, holiday) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    FlowIcon(if (holiday) FlowIcons.CalendarCheck else FlowIcons.Info, tint = if (holiday) c.danger else c.accent, size = 20.dp)
                    Text(title, Modifier.weight(1f), style = Flow.type.body, color = c.textPrimary)
                    if (holiday) Text("تعطیل رسمی", style = Flow.type.caption, color = c.danger)
                }
            }
        }
    }
}
