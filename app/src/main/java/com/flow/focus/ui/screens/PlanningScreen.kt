package com.flow.focus.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.core.CalendarOccasion
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
import com.flow.focus.domain.TaskEntry
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.CheckCircle
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.Fab
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.JalaliDatePickerDialog
import com.flow.focus.ui.components.JalaliMonthView
import com.flow.focus.ui.components.ScreenHeader
import com.flow.focus.ui.components.SectionTitle
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.relativeDay
import com.flow.focus.ui.rememberToday
import com.flow.focus.ui.theme.Flow
import java.time.LocalDate

@Composable
fun PlanningScreen(vm: AppViewModel, data: AppData, modifier: Modifier = Modifier) {
    val today = rememberToday()
    val offlineOccasions by vm.offlineCalendarOccasions.collectAsStateWithLifecycle()
    val animations = Flow.glass.animations
    val calendarColors = Flow.colors
    var mode by rememberSaveable { mutableStateOf(0) } // ۰ روزانه، ۱ هفتگی، ۲ ماهانه
    var selected by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var monthAnchor by rememberSaveable { mutableStateOf(firstOfMonth(today).toEpochDay()) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    var routineEditing by remember { mutableStateOf<Routine?>(null) }
    var routineCreating by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    var moveTask by remember { mutableStateOf<Task?>(null) }

    val selDate = LocalDate.ofEpochDay(selected)
    val selectedYear = JalaliDate.of(selDate).year
    val selectedOccasions = remember(offlineOccasions, selected) {
        offlineOccasions.filter { it.epochDay == selected }
    }
    val hasOfflineYearData = remember(offlineOccasions, selectedYear) {
        offlineOccasions.any { JalaliDate.of(LocalDate.ofEpochDay(it.epochDay)).year == selectedYear }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("برنامه‌ریزی")
            Text(
                if (hasOfflineYearData) "مناسبت‌ها و تعطیلات آفلاین سال $selectedYear"
                else "داده‌ی مناسبت آفلاین برای سال $selectedYear در این نسخه موجود نیست",
                style = Flow.type.caption,
                color = Flow.colors.textSecondary,
            )
            SegmentedControl(listOf("روزانه", "هفتگی", "ماهانه"), mode, { mode = it })

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    if (animations) {
                        val direction = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 18 * direction }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 18 * direction })
                    } else {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    }
                },
                label = "planning-mode",
            ) { activeMode ->
                when (activeMode) {
                0 -> DailyView(
                    vm, data, selDate, today,
                    onDay = { selected = it.toEpochDay() },
                    onPickDate = { datePicker = true },
                    onEdit = { editing = it },
                    onMove = { moveTask = it },
                    onEditRoutine = { routineEditing = it },
                    onAddRoutine = { routineCreating = true },
                )
                1 -> WeeklyView(
                    vm, data, selDate, today,
                    onSelectDay = { selected = it.toEpochDay() },
                    onShiftWeek = { selected = selDate.plusDays(7L * it).toEpochDay() },
                    onEdit = { editing = it },
                    onMove = { moveTask = it },
                )
                else -> {
                    val a = JalaliDate.of(LocalDate.ofEpochDay(monthAnchor))
                    val first = LocalDate.ofEpochDay(monthAnchor)
                    val last = first.plusDays((a.lengthOfMonth() - 1).toLong())
                    val monthOccasions = remember(offlineOccasions, a.year, a.month) {
                        offlineOccasions.filter { occasion ->
                            val date = LocalDate.ofEpochDay(occasion.epochDay)
                            val jalali = JalaliDate.of(date)
                            jalali.year == a.year && jalali.month == a.month
                        }
                    }
                    val marked = remember(data.tasks, data.routines, monthAnchor, monthOccasions) {
                        Planner.markedDays(data, first, last) + monthOccasions.map { it.epochDay }
                    }
                    val eventColors = remember(data.tasks, data.routines, data.routineChecks, monthAnchor, monthOccasions) {
                        buildMap<Long, List<Color>> {
                            for (dayOfMonth in 1..a.lengthOfMonth()) {
                                val date = JalaliDate(a.year, a.month, dayOfMonth).toLocalDate()
                                val day = date.toEpochDay()
                                val taskColors = Planner.entriesFor(data, date)
                                    .map { Color(Categories.byId(it.categoryId).color) }
                                val occasionColors = monthOccasions.filter { it.epochDay == day }.map {
                                    if (it.isHoliday) calendarColors.danger else calendarColors.accent
                                }
                                val colors = (taskColors + occasionColors).distinct().take(3)
                                if (colors.isNotEmpty()) put(day, colors)
                            }
                        }
                    }
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            JalaliMonthView(
                                a.year, a.month, selected, today.toEpochDay(), marked,
                                onSelect = { selected = it.toEpochDay() },
                                onPrev = { monthAnchor = JalaliDate(a.year, a.month, 1).plusMonths(-1).toLocalDate().toEpochDay() },
                                onNext = { monthAnchor = JalaliDate(a.year, a.month, 1).plusMonths(1).toLocalDate().toEpochDay() },
                                eventColors = eventColors,
                            )
                        }
                    }
                    if (selDate != today) GlassButton("رفتن به امروز", {
                        selected = today.toEpochDay()
                        monthAnchor = firstOfMonth(today).toEpochDay()
                    }, Modifier.fillMaxWidth())
                    SectionTitle(num(DateFormats.full(selDate)))
                    EntryList(vm, data, selDate, onEdit = { editing = it }, onMove = { moveTask = it })
                }
                }
            }

            if (selectedOccasions.isNotEmpty()) {
                OfflineCalendarOccasions(selectedOccasions)
            }
        }
        Fab({ creating = true }, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 108.dp).navigationBarsPadding())
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
    if (routineCreating) {
        RoutineEditorDialog(null, { vm.saveRoutine(it); routineCreating = false }, null, { routineCreating = false })
    }
    routineEditing?.let { r ->
        RoutineEditorDialog(r, { vm.saveRoutine(it); routineEditing = null }, { vm.deleteRoutine(it.id); routineEditing = null }, { routineEditing = null })
    }
    if (datePicker) {
        JalaliDatePickerDialog(selected, onPick = { selected = it; datePicker = false }, onDismiss = { datePicker = false })
    }
    moveTask?.let { t ->
        JalaliDatePickerDialog(t.dueDay, onPick = { vm.moveTask(t.id, it); moveTask = null }, onDismiss = { moveTask = null })
    }
}

private fun firstOfMonth(d: LocalDate): LocalDate {
    val j = JalaliDate.of(d)
    return JalaliDate(j.year, j.month, 1).toLocalDate()
}

@Composable
private fun OfflineCalendarOccasions(items: List<CalendarOccasion>) {
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        level = GlassLevel.Subtle,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SectionTitle("مناسبت‌های این روز")
            items.forEach { occasion ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    FlowIcon(
                        if (occasion.isHoliday) FlowIcons.CalendarCheck else FlowIcons.Info,
                        tint = if (occasion.isHoliday) Flow.colors.danger else Flow.colors.accent,
                        size = 20.dp,
                    )
                    Text(
                        occasion.titleFa,
                        Modifier.weight(1f),
                        style = Flow.type.body,
                        color = Flow.colors.textPrimary,
                    )
                    if (occasion.isHoliday) {
                        Text("تعطیل رسمی", style = Flow.type.caption, color = Flow.colors.danger)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyView(
    vm: AppViewModel,
    data: AppData,
    day: LocalDate,
    today: LocalDate,
    onDay: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
    onEdit: (Task) -> Unit,
    onMove: (Task) -> Unit,
    onEditRoutine: (Routine) -> Unit,
    onAddRoutine: () -> Unit,
) {
    val c = Flow.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassIconButton(FlowIcons.ChevronPrev, { onDay(day.minusDays(1)) }, size = 40.dp, iconSize = 18.dp, contentDescription = "روز قبل")
        GlassSurface(
            Modifier.weight(1f).springClick(scaleDown = 0.98f, onClick = onPickDate),
            shape = RoundedCornerShape(20.dp), level = GlassLevel.Subtle,
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(num(DateFormats.full(day)), style = Flow.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (day != today) Text(num(relativeDay(day.toEpochDay(), today)), style = Flow.type.caption, color = c.textSecondary)
                else Text("امروز", style = Flow.type.caption, color = c.accent)
            }
        }
        GlassIconButton(FlowIcons.ChevronNext, { onDay(day.plusDays(1)) }, size = 40.dp, iconSize = 18.dp, contentDescription = "روز بعد")
    }
    if (day != today) GlassButton("برو به امروز", { onDay(today) }, Modifier.fillMaxWidth())

    GoalsCard(
        "اهداف روز", data.goals.filter { it.period == GoalPeriod.DAILY && it.periodStartDay == day.toEpochDay() },
        onAdd = { vm.addGoal(it, GoalPeriod.DAILY, day.toEpochDay()) }, onToggle = vm::toggleGoal, onDelete = vm::deleteGoal,
    )

    SectionTitle("برنامه‌ی روز")
    EntryList(vm, data, day, onEdit, onMove)

    SectionTitle("روتین‌های تکرارشونده", trailing = {
        GlassIconButton(FlowIcons.Plus, onAddRoutine, size = 36.dp, iconSize = 18.dp, contentDescription = "روتین جدید")
    })
    if (data.routines.isEmpty()) {
        EmptyState(FlowIcons.Repeat, "هنوز روتینی نداری", "کارهایی مثل ورزش یا مطالعه را یک بار تعریف کن تا هر هفته تکرار شوند.")
    } else {
        data.routines.forEach { r ->
            GlassSurface(Modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = { onEditRoutine(r) }), shape = RoundedCornerShape(20.dp), level = GlassLevel.Subtle) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowIcon(FlowIcons.Repeat, tint = c.accent, size = 20.dp)
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = Flow.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val days = if (r.weekdays.size == 7) "هر روز" else r.weekdays.joinToString("، ") { JalaliDate.weekdayNames[it] }
                        Text(num(days + (r.minute?.let { " · " + DateFormats.clock(it) } ?: "")), style = Flow.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryList(vm: AppViewModel, data: AppData, day: LocalDate, onEdit: (Task) -> Unit, onMove: (Task) -> Unit) {
    val entries = remember(data.tasks, data.routines, data.routineChecks, day) { Planner.entriesFor(data, day) }
    if (entries.isEmpty()) {
        EmptyState(FlowIcons.Calendar, "برنامه‌ای برای این روز نیست", "با دکمه‌ی + یک وظیفه اضافه کن.")
        return
    }
    val c = Flow.colors
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        level = GlassLevel.Subtle,
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 4.dp)) {
            entries.forEachIndexed { index, entry ->
                EntryRow(vm, entry, day, onEdit, onMove)
                if (index < entries.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.track))
                }
            }
        }
    }
}

@Composable
private fun EntryRow(vm: AppViewModel, e: PlanEntry, day: LocalDate, onEdit: (Task) -> Unit, onMove: (Task) -> Unit) {
    val c = Flow.colors
    val cat = Categories.byId(e.categoryId)
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .springClick(scaleDown = 0.99f, onClick = { if (e is TaskEntry) onEdit(e.task) })
            .padding(horizontal = 7.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            CheckCircle(
                e.done,
                { if (e is TaskEntry) vm.setTaskDone(e.task.id, !e.task.done) else if (e is RoutineEntry) vm.toggleRoutine(e.routine.id, day.toEpochDay()) },
                size = 28.dp, color = Color(cat.color),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    e.title, style = Flow.type.bodyStrong,
                    color = if (e.done) c.textSecondary else c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (e is RoutineEntry) FlowIcon(FlowIcons.Repeat, tint = c.textSecondary, size = 14.dp)
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(cat.color)))
                    Text(
                        num(cat.name + (e.minute?.let { " · " + DateFormats.clock(it) } ?: "")),
                        style = Flow.type.caption, color = c.textSecondary,
                    )
                }
            }
            if (e is TaskEntry && !e.task.done) {
                GlassIconButton(FlowIcons.Move, { onMove(e.task) }, size = 34.dp, iconSize = 16.dp, contentDescription = "جابه‌جایی به تاریخ دیگر")
            }
    }
}

@Composable
private fun WeeklyView(
    vm: AppViewModel,
    data: AppData,
    selectedDay: LocalDate,
    today: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    onShiftWeek: (Int) -> Unit,
    onEdit: (Task) -> Unit,
    onMove: (Task) -> Unit,
) {
    val c = Flow.colors
    val start = JalaliDate.startOfWeek(selectedDay)
    val end = start.plusDays(6)
    val selectedEpoch = selectedDay.toEpochDay()

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlassIconButton(FlowIcons.ChevronPrev, { onShiftWeek(-1) }, size = 40.dp, iconSize = 18.dp, contentDescription = "هفته‌ی قبل")
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(num(DateFormats.short(start) + " تا " + DateFormats.short(end)), style = Flow.type.heading, color = c.textPrimary)
            Text("نمای هفتگی", style = Flow.type.caption, color = c.textSecondary)
        }
        GlassIconButton(FlowIcons.ChevronNext, { onShiftWeek(1) }, size = 40.dp, iconSize = 18.dp, contentDescription = "هفته‌ی بعد")
    }

    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), level = GlassLevel.Subtle) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (i in 0..6) {
                val day = start.plusDays(i.toLong())
                val epoch = day.toEpochDay()
                val entries = remember(data.tasks, data.routines, data.routineChecks, epoch) {
                    Planner.entriesFor(data, day)
                }
                val selected = epoch == selectedEpoch
                val isToday = day == today
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (selected) c.accent else Color.Transparent)
                        .springClick(scaleDown = 0.94f, onClick = { onSelectDay(day) })
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        JalaliDate.weekdayInitials[i],
                        style = Flow.type.caption,
                        color = if (selected) c.textOnAccent.copy(alpha = 0.82f) else c.textSecondary,
                    )
                    Text(
                        num(JalaliDate.of(day).day),
                        style = Flow.type.bodyStrong,
                        color = if (selected) c.textOnAccent else c.textPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (entries.isEmpty()) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(if (isToday && !selected) c.accent else Color.Transparent))
                        } else {
                            repeat(minOf(entries.size, 3)) {
                                Box(
                                    Modifier.size(4.dp).clip(CircleShape)
                                        .background(if (selected) c.textOnAccent else c.accent),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedDay != today) {
        GlassButton("رفتن به امروز", { onSelectDay(today) }, Modifier.fillMaxWidth())
    }

    GoalsCard(
        "اهداف هفته",
        data.goals.filter { it.period == GoalPeriod.WEEKLY && it.periodStartDay == start.toEpochDay() },
        onAdd = { vm.addGoal(it, GoalPeriod.WEEKLY, start.toEpochDay()) },
        onToggle = vm::toggleGoal,
        onDelete = vm::deleteGoal,
    )

    SectionTitle(num(DateFormats.full(selectedDay)))
    EntryList(vm, data, selectedDay, onEdit = onEdit, onMove = onMove)
}
