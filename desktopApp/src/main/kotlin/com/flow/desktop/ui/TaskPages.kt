package com.flow.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.flow.desktop.DesktopRuntime
import com.flow.desktop.design.*
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.data.Goal
import com.flow.focus.data.GoalPeriod
import com.flow.focus.data.Priority
import com.flow.focus.data.Task
import com.flow.focus.domain.TaskFilter
import com.flow.focus.domain.TaskQuery
import com.flow.focus.domain.TaskSort
import java.time.LocalDate

/** Delete button that asks for a second click (turns red and says "حذف؟") so nothing is lost by accident. */
@Composable
internal fun ConfirmDeleteButton(onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { kotlinx.coroutines.delay(3000); armed = false } }
    if (armed) FlowButton("حذف؟", onConfirm, modifier, kind = ButtonKind.Danger, compact = true, icon = FlowIcon.Trash)
    else FlowIconButton(FlowIcon.Trash, "حذف", { armed = true }, modifier, size = 32.dp, iconSize = 17.dp)
}

private fun dueLabel(task: Task, persian: Boolean): String {
    val day = task.dueDay?.let { DateFormats.short(LocalDate.ofEpochDay(it)) } ?: "بدون سررسید"
    val time = task.dueMinute?.let { " · " + clockText(it, persian) } ?: ""
    return digits(day, persian) + time
}

// ───────────────────────── Tasks ─────────────────────────

@Composable
internal fun TasksPage(data: AppData, runtime: DesktopRuntime) {
    val pal = Flow.colors
    var query by remember { mutableStateOf("") }
    var quick by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var sort by remember { mutableStateOf(TaskSort.DUE) }
    var category by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    val todayEpoch = LocalDate.now().toEpochDay()
    val tasks = remember(data.tasks, filter, sort, category, query) { TaskQuery.apply(data.tasks, filter, sort, category, query, todayEpoch) }
    val openCount = data.tasks.count { !it.done }
    val persian = LocalPersianDigits.current

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
        GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 12.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowTextField(
                    quick, { quick = it }, "افزودن سریع وظیفه… (Enter)", Modifier.weight(1f), leading = FlowIcon.Plus,
                    onEnter = { if (quick.isNotBlank()) { runtime.addTask(quick); quick = "" } },
                )
                FlowButton("وظیفه‌ی کامل", { creating = true }, icon = FlowIcon.Edit)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowTextField(query, { query = it }, "جست‌وجو در عنوان و یادداشت", Modifier.weight(1f), leading = FlowIcon.Search)
                FlowDropdown(
                    selectedLabel = when (sort) { TaskSort.DUE -> "مرتب‌سازی: سررسید"; TaskSort.PRIORITY -> "مرتب‌سازی: اولویت"; TaskSort.NEWEST -> "مرتب‌سازی: جدیدترین" },
                    options = listOf(TaskSort.DUE to "سررسید", TaskSort.PRIORITY to "اولویت", TaskSort.NEWEST to "جدیدترین"),
                    selected = sort, onSelect = { sort = it }, menuWidth = 190.dp, modifier = Modifier.width(210.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(TaskFilter.ALL to "همه", TaskFilter.TODAY to "امروز و عقب‌افتاده", TaskFilter.UPCOMING to "پیش‌رو", TaskFilter.DONE to "انجام‌شده").forEach { (f, l) ->
                    FlowChip(l, filter == f, { filter = f })
                }
                Spacer(Modifier.weight(1f))
                Categories.all.forEach { c ->
                    FlowChip(c.name, category == c.id, { category = if (category == c.id) null else c.id }, dot = Color(c.color))
                }
            }
        }
        GlassPanel(Modifier.fillMaxWidth().weight(1f), padding = PaddingValues(horizontal = 10.dp, vertical = 14.dp), verticalSpacing = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowText("فهرست وظایف", Modifier.weight(1f), FlowType.title2)
                FlowText("${num(openCount)} باز · ${num(data.tasks.size - openCount)} انجام‌شده", style = FlowType.caption, color = pal.textSecondary)
            }
            if (tasks.isEmpty()) {
                EmptyState(
                    FlowIcon.Tasks,
                    if (data.tasks.isEmpty()) "هنوز وظیفه‌ای نداری" else "موردی پیدا نشد",
                    if (data.tasks.isEmpty()) "از کادر بالا اولین وظیفه را اضافه کن." else "فیلتر یا عبارت جست‌وجو را تغییر بده.",
                    Modifier.weight(1f),
                )
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(task, persian, runtime, onEdit = { editor = task })
                    }
                }
            }
        }
    }
    if (creating || editor != null) {
        TaskEditorDialog(editor, runtime, onDismiss = { creating = false; editor = null })
    }
}

@Composable
private fun TaskRow(task: Task, persian: Boolean, runtime: DesktopRuntime, onEdit: () -> Unit) {
    val pal = Flow.colors
    val ph = rememberPressHover()
    val cat = Categories.byId(task.categoryId)
    ContextMenuBox(
        listOf(
            MenuEntry("ویرایش", FlowIcon.Edit) { onEdit() },
            MenuEntry(if (task.done) "علامت‌گذاری به‌عنوان باز" else "انجام شد", FlowIcon.Check) { runtime.setTaskDone(task.id, !task.done) },
            MenuEntry("تایمر برای این وظیفه", FlowIcon.Focus) { runtime.selectTask(task.id) },
            MenuEntry("حذف", FlowIcon.Trash, danger = true) { runtime.deleteTask(task.id) },
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(FlowDims.cardRadius))
                .background(if (ph.hovered) pal.fillSubtle else Color.Transparent)
                .androidxHover(ph).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowCheck(task.done, { runtime.setTaskDone(task.id, it) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FlowText(task.title, style = FlowType.bodyMedium, color = if (task.done) pal.textTertiary else pal.text, maxLines = 1)
                if (task.notes.isNotBlank()) FlowText(task.notes, style = FlowType.caption, color = pal.textSecondary, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(priorityColor(task.priority, pal)))
                        FlowText(task.priority.label, style = FlowType.caption, color = pal.textSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        FlowIconView(FlowIcon.Clock, pal.textTertiary, size = 13.dp)
                        FlowText(dueLabel(task, persian), style = FlowType.caption, color = pal.textSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(cat.color)))
                        FlowText(cat.name, style = FlowType.caption, color = pal.textSecondary)
                    }
                }
            }
            FlowIconButton(FlowIcon.Focus, "تمرکز روی این وظیفه", { runtime.selectTask(task.id) }, size = 32.dp, iconSize = 17.dp)
            FlowIconButton(FlowIcon.Edit, "ویرایش", onEdit, size = 32.dp, iconSize = 17.dp)
            ConfirmDeleteButton({ runtime.deleteTask(task.id) })
        }
    }
}

private fun Modifier.androidxHover(ph: PressHover): Modifier = this.then(Modifier.hoverable(ph.source))

@Composable
private fun TaskEditorDialog(existing: Task?, runtime: DesktopRuntime, onDismiss: () -> Unit) {
    val pal = Flow.colors
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var date by remember { mutableStateOf(existing?.dueDay?.let { jalaliText(LocalDate.ofEpochDay(it)) } ?: "") }
    var time by remember { mutableStateOf(existing?.dueMinute?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var category by remember { mutableStateOf(existing?.categoryId ?: "personal") }
    var pomos by remember { mutableStateOf(existing?.estimatedPomodoros ?: 1) }
    var error by remember { mutableStateOf("") }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    FlowDialog(onDismiss, width = 560.dp) {
        FlowText(if (existing == null) "وظیفه‌ی جدید" else "ویرایش وظیفه", style = FlowType.title1)
        FlowTextField(title, { title = it; error = "" }, "عنوان وظیفه", focusRequester = focus)
        FlowTextField(notes, { notes = it }, "یادداشت (اختیاری)", singleLine = false, minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowTextField(date, { date = it; error = "" }, "تاریخ جلالی  ${numText("1405/08/23")}", Modifier.weight(1f), leading = FlowIcon.Calendar, ltrContent = true)
            FlowTextField(time, { time = it; error = "" }, "ساعت  ${numText("18:30")}", Modifier.weight(1f), leading = FlowIcon.Clock, ltrContent = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowText("اولویت", Modifier.width(56.dp), FlowType.callout, pal.textSecondary)
            Segmented(Priority.entries.map { it to it.label }, priority, { priority = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowText("دسته", Modifier.width(56.dp), FlowType.callout, pal.textSecondary)
            Categories.all.forEach { c -> FlowChip(c.name, category == c.id, { category = c.id }, dot = Color(c.color)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowText("پومودورو", Modifier.width(56.dp), FlowType.callout, pal.textSecondary)
            FlowIconButton(FlowIcon.Minus, "کمتر", { pomos = (pomos - 1).coerceAtLeast(1) }, size = 30.dp, iconSize = 15.dp)
            FlowText(num(pomos), Modifier.widthIn(min = 28.dp), FlowType.headline, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            FlowIconButton(FlowIcon.Plus, "بیشتر", { pomos = (pomos + 1).coerceAtMost(20) }, size = 30.dp, iconSize = 15.dp)
        }
        if (error.isNotBlank()) FlowText(error, style = FlowType.caption, color = pal.danger)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            FlowButton("انصراف", onDismiss, kind = ButtonKind.Ghost)
            FlowButton(if (existing == null) "افزودن" else "ذخیره", {
                val d = if (date.isBlank()) null else parseJalali(date)
                val t = if (time.isBlank()) null else parseClock(time)
                when {
                    title.isBlank() -> error = "عنوان وظیفه را وارد کن."
                    date.isNotBlank() && d == null -> error = "تاریخ را به شکل سال/ماه/روز جلالی وارد کن."
                    time.isNotBlank() && t == null -> error = "ساعت را به شکل ساعت:دقیقه وارد کن."
                    else -> {
                        if (existing == null) runtime.addTask(title, notes, d, t, priority, category, pomos)
                        else runtime.saveTask(existing.copy(
                            title = title.trim(), notes = notes.trim(), dueDay = d?.toEpochDay(), dueMinute = t,
                            priority = priority, categoryId = category, estimatedPomodoros = pomos,
                        ))
                        onDismiss()
                    }
                }
            }, kind = ButtonKind.Primary, icon = FlowIcon.Check)
        }
    }
}

// ───────────────────────── Routines ─────────────────────────

@Composable
internal fun RoutinesPage(data: AppData, runtime: DesktopRuntime) {
    val pal = Flow.colors
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val days = remember { mutableStateListOf(0, 1, 2, 3, 4, 5, 6) }
    var category by remember { mutableStateOf("personal") }
    val today = LocalDate.now()
    val todayIdx = JalaliDate.weekdayIndex(today)
    val persian = LocalPersianDigits.current
    val timeOk = time.isBlank() || parseClock(time) != null
    val canAdd = title.isNotBlank() && days.isNotEmpty() && timeOk
    fun add() {
        if (!canAdd) return
        runtime.addRoutine(title, days.toList(), if (time.isBlank()) null else parseClock(time), category)
        title = ""; time = ""
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= 1000.dp
        val form: @Composable (Modifier) -> Unit = { m ->
            GlassPanel(m, verticalSpacing = 12.dp) {
                PanelHeader("روتین جدید", FlowIcon.Routine)
                FlowTextField(title, { title = it }, "عنوان روتین", onEnter = ::add)
                FlowTextField(time, { time = it }, "ساعت اختیاری  ${numText("07:30")}", leading = FlowIcon.Clock, ltrContent = true, onEnter = ::add)
                if (!timeOk) FlowText("ساعت را به شکل ساعت:دقیقه وارد کن.", style = FlowType.caption, color = pal.danger)
                FlowText("روزهای تکرار", style = FlowType.callout, color = pal.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JalaliDate.weekdayInitials.forEachIndexed { i, d ->
                        FlowChip(d, i in days, { if (i in days) days.remove(i) else days.add(i) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Categories.all.forEach { c -> FlowChip(c.name, category == c.id, { category = c.id }, dot = Color(c.color)) }
                }
                FlowButton("افزودن روتین", ::add, kind = ButtonKind.Primary, icon = FlowIcon.Plus, enabled = canAdd)
            }
        }
        val list: @Composable (Modifier) -> Unit = { m ->
            GlassPanel(m, verticalSpacing = 6.dp) {
                PanelHeader("روتین‌های من", FlowIcon.Layers) {
                    FlowText("${num(data.routines.count { todayIdx in it.weekdays })} مورد امروز", style = FlowType.caption, color = pal.textSecondary)
                }
                if (data.routines.isEmpty()) EmptyState(FlowIcon.Routine, "هنوز روتینی ثبت نشده", "برای کارهای تکرارشونده‌ی هفتگی روتین بساز.")
                data.routines.forEach { r ->
                    val doneToday = "${r.id}|${today.toEpochDay()}" in data.routineChecks
                    val cat = Categories.byId(r.categoryId)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (todayIdx in r.weekdays) FlowCheck(doneToday, { runtime.toggleRoutine(r.id, today) })
                        else Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(5.dp).clip(CircleShape).background(pal.textTertiary)) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(cat.color)))
                                FlowText(r.title, style = FlowType.bodyMedium, maxLines = 1)
                                r.minute?.let { FlowText(clockText(it, persian), style = FlowType.caption, color = pal.textSecondary) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                JalaliDate.weekdayInitials.forEachIndexed { i, d ->
                                    val on = i in r.weekdays
                                    Box(
                                        Modifier.size(22.dp).clip(CircleShape)
                                            .background(if (i == todayIdx && on) pal.accent.copy(alpha = .22f) else if (on) pal.fillStrong else Color.Transparent),
                                        contentAlignment = Alignment.Center,
                                    ) { FlowText(d, style = FlowType.caption, color = if (on) pal.text else pal.textTertiary.copy(alpha = .5f)) }
                                }
                            }
                        }
                        ConfirmDeleteButton({ runtime.deleteRoutine(r.id) })
                    }
                    Hairline()
                }
            }
        }
        if (split) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            list(Modifier.weight(1.35f).fillMaxHeight().verticalScroll(rememberScrollState()))
            form(Modifier.weight(1f).verticalScroll(rememberScrollState()))
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            form(Modifier.fillMaxWidth()); list(Modifier.fillMaxWidth())
        }
    }
}

// ───────────────────────── Goals ─────────────────────────

@Composable
internal fun GoalsPage(data: AppData, runtime: DesktopRuntime) {
    val pal = Flow.colors
    var title by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(GoalPeriod.DAILY) }
    val today = LocalDate.now()
    val weekStart = JalaliDate.startOfWeek(today).toEpochDay()
    fun startOf(p: GoalPeriod) = if (p == GoalPeriod.DAILY) today.toEpochDay() else weekStart
    fun add() { if (title.isNotBlank()) { runtime.addGoal(title, period, startOf(period)); title = "" } }
    val active = data.goals.filter { it.periodStartDay == startOf(it.period) }
    val daily = active.filter { it.period == GoalPeriod.DAILY }
    val weekly = active.filter { it.period == GoalPeriod.WEEKLY }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            listOf("امروز" to daily, "این هفته" to weekly).forEach { (label, goals) ->
                val done = goals.count { it.done }
                GlassPanel(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProgressRing(if (goals.isEmpty()) 0f else done.toFloat() / goals.size, size = 72.dp, stroke = 7.dp) {
                            FlowText(if (goals.isEmpty()) "—" else num((done * 100 / goals.size)) + "٪", style = FlowType.headline)
                        }
                        Column {
                            FlowText("هدف‌های $label", style = FlowType.title2)
                            FlowText(if (goals.isEmpty()) "هدفی ثبت نشده" else "${num(done)} از ${num(goals.size)} انجام شده", style = FlowType.caption, color = pal.textSecondary)
                        }
                    }
                }
            }
        }
        GlassPanel(Modifier.fillMaxWidth()) {
            PanelHeader("هدف جدید", FlowIcon.Target)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowTextField(title, { title = it }, "عنوان هدف… (Enter)", Modifier.weight(1f), onEnter = ::add)
                Segmented(listOf(GoalPeriod.DAILY to "روزانه", GoalPeriod.WEEKLY to "هفتگی"), period, { period = it })
                FlowButton("ثبت هدف", ::add, kind = ButtonKind.Primary, icon = FlowIcon.Plus, enabled = title.isNotBlank())
            }
        }
        GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 4.dp) {
            PanelHeader("هدف‌های فعال", FlowIcon.Flag)
            if (active.isEmpty()) EmptyState(FlowIcon.Target, "برای این بازه هدفی ثبت نشده", "یک هدف روزانه یا هفتگی اضافه کن.")
            active.forEach { g -> GoalRow(g, runtime) }
        }
    }
}

@Composable
private fun GoalRow(goal: Goal, runtime: DesktopRuntime) {
    val pal = Flow.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowCheck(goal.done, { runtime.toggleGoal(goal.id) })
        FlowText(goal.title, Modifier.weight(1f), FlowType.bodyMedium, if (goal.done) pal.textTertiary else pal.text, maxLines = 1)
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(pal.fillSubtle).padding(horizontal = 9.dp, vertical = 3.dp)) {
            FlowText(if (goal.period == GoalPeriod.DAILY) "روزانه" else "هفتگی", style = FlowType.caption, color = pal.textSecondary)
        }
        ConfirmDeleteButton({ runtime.deleteGoal(goal.id) })
    }
}
