package com.flow.focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("برنامه‌ریزی")
            SegmentedControl(listOf("روزانه", "هفتگی", "ماهانه"), mode, { mode = it })

            when (mode) {
                0 -> DailyView(
                    vm, data, selDate, today,
                    onDay = { selected = it.toEpochDay() },
                    onPickDate = { datePicker = true },
                    onEdit = { editing = it },
                    onMove = { moveTask = it },
                    onEditRoutine = { routineEditing = it },
                    onAddRoutine = { routineCreating = true },
                )
                1 -> WeeklyView(vm, data, selDate, today, onOpenDay = { selected = it.toEpochDay(); mode = 0 }, onShiftWeek = { selected = selDate.plusDays(7L * it).toEpochDay() })
                else -> {
                    val a = JalaliDate.of(LocalDate.ofEpochDay(monthAnchor))
                    val first = LocalDate.ofEpochDay(monthAnchor)
                    val last = first.plusDays((a.lengthOfMonth() - 1).toLong())
                    val marked = remember(data.tasks, data.routines, monthAnchor) { Planner.markedDays(data, first, last) }
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            JalaliMonthView(
                                a.year, a.month, selected, today.toEpochDay(), marked,
                                onSelect = { selected = it.toEpochDay() },
                                onPrev = { monthAnchor = JalaliDate(a.year, a.month, 1).plusMonths(-1).toLocalDate().toEpochDay() },
                                onNext = { monthAnchor = JalaliDate(a.year, a.month, 1).plusMonths(1).toLocalDate().toEpochDay() },
                            )
                        }
                    }
                    SectionTitle(num(DateFormats.full(selDate)))
                    EntryList(vm, data, selDate, onEdit = { editing = it }, onMove = { moveTask = it })
                }
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
    entries.forEach { e -> EntryRow(vm, e, day, onEdit, onMove) }
}

@Composable
private fun EntryRow(vm: AppViewModel, e: PlanEntry, day: LocalDate, onEdit: (Task) -> Unit, onMove: (Task) -> Unit) {
    val c = Flow.colors
    val cat = Categories.byId(e.categoryId)
    GlassSurface(
        Modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = { if (e is TaskEntry) onEdit(e.task) }),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
}

@Composable
private fun WeeklyView(
    vm: AppViewModel,
    data: AppData,
    anchor: LocalDate,
    today: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    onShiftWeek: (Int) -> Unit,
) {
    val c = Flow.colors
    val start = JalaliDate.startOfWeek(anchor)
    val end = start.plusDays(6)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassIconButton(FlowIcons.ChevronPrev, { onShiftWeek(-1) }, size = 40.dp, iconSize = 18.dp, contentDescription = "هفته‌ی قبل")
        Text(
            num(DateFormats.short(start) + " تا " + DateFormats.short(end)),
            Modifier.weight(1f), style = Flow.type.heading, color = c.textPrimary, textAlign = TextAlign.Center,
        )
        GlassIconButton(FlowIcons.ChevronNext, { onShiftWeek(1) }, size = 40.dp, iconSize = 18.dp, contentDescription = "هفته‌ی بعد")
    }
    GoalsCard(
        "اهداف هفته", data.goals.filter { it.period == GoalPeriod.WEEKLY && it.periodStartDay == start.toEpochDay() },
        onAdd = { vm.addGoal(it, GoalPeriod.WEEKLY, start.toEpochDay()) }, onToggle = vm::toggleGoal, onDelete = vm::deleteGoal,
    )
    for (i in 0..6) {
        val d = start.plusDays(i.toLong())
        val entries = remember(data.tasks, data.routines, data.routineChecks, d) { Planner.entriesFor(data, d) }
        val isToday = d == today
        GlassSurface(
            Modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = { onOpenDay(d) }),
            shape = RoundedCornerShape(22.dp), tint = if (isToday) c.accent else null,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.size(width = 64.dp, height = 52.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(JalaliDate.weekdayNames[i], style = Flow.type.label, color = if (isToday) c.accent else c.textSecondary, maxLines = 1)
                    Text(num(JalaliDate.of(d).day), style = Flow.type.title, color = c.textPrimary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (entries.isEmpty()) {
                        Text("آزاد", style = Flow.type.caption, color = c.textSecondary)
                    } else {
                        entries.take(3).forEach {
                            Text(
                                num((it.minute?.let { m -> DateFormats.clock(m) + "  " } ?: "") + it.title),
                                style = Flow.type.caption, color = if (it.done) c.textSecondary else c.textPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (entries.size > 3) Text(num("و ${entries.size - 3} مورد دیگر"), style = Flow.type.caption, color = c.textSecondary)
                    }
                }
                FlowIcon(FlowIcons.ChevronNext, tint = c.textSecondary, size = 18.dp)
            }
        }
    }
}
