package com.flow.focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.core.formatClock
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.data.FlowRepository
import com.flow.focus.data.Routine
import com.flow.focus.data.RoutineStep
import com.flow.focus.domain.RoutineStats
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.rememberTabScrollState
import com.flow.focus.ui.components.CheckCircle
import com.flow.focus.ui.components.Chip
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.Fab
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.FlowSheet
import com.flow.focus.ui.components.FlowTextField
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.ScreenHeader
import com.flow.focus.ui.components.SectionTitle
import com.flow.focus.ui.components.Stepper
import com.flow.focus.ui.components.SwitchRow
import com.flow.focus.ui.components.TimeEditor
import com.flow.focus.ui.components.num
import com.flow.focus.ui.durationLabel
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.motion.Appear
import com.flow.focus.ui.rememberNowMs
import com.flow.focus.ui.rememberToday
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalDockContentPadding
import kotlinx.coroutines.delay

private data class RoutinePreset(val label: String, val title: String, val categoryId: String, val steps: List<RoutineStep>)

private val presets = listOf(
    RoutinePreset(
        "مطالعه", "مطالعه‌ی روزانه", "study",
        listOf(RoutineStep("تمرکز", 25), RoutineStep("استراحت", 5), RoutineStep("تمرکز", 25), RoutineStep("استراحت", 5)),
    ),
    RoutinePreset("استراحت", "استراحت و تنفس", "personal", listOf(RoutineStep("تنفس آرام", 5), RoutineStep("کشش", 5))),
    RoutinePreset("ورزش", "ورزش", "sport", listOf(RoutineStep("گرم‌کردن", 5), RoutineStep("تمرین", 25), RoutineStep("سردکردن", 5))),
    RoutinePreset("شخصی", "مرور شبانه", "personal", listOf(RoutineStep("مرور روز و برنامه‌ی فردا", 10))),
)

/**
 * روتین‌ها: فرایندهای تکرارشونده‌ی چندمرحله‌ای که با یک لمس اجرا می‌شوند، در برنامه‌ی روز دیده می‌شوند
 * و سابقه و پیاپی‌بودن اجرا نگه‌داری می‌شود. برخلاف برنامه‌ریزی (کارهای یک‌باره‌ی تاریخ‌دار) تمرکز این صفحه
 * عادت‌ها و توالی مراحل است.
 */
@Composable
fun RoutinesScreen(vm: AppViewModel, data: AppData, modifier: Modifier = Modifier) {
    val today = rememberToday()
    var editing by remember { mutableStateOf<Routine?>(null) }
    var creating by remember { mutableStateOf<Routine?>(null) }
    var running by remember { mutableStateOf<Routine?>(null) }
    val todayWd = JalaliDate.weekdayIndex(today)
    val todays = data.routines.filter { todayWd in it.weekdays }
    val doneToday = todays.count { RoutineStats.isChecked(data, it.id, today.toEpochDay()) }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberTabScrollState()).statusBarsPadding()
                .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = LocalDockContentPadding.current + 72.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("روتین‌ها")

            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("امروز", style = Flow.type.heading, color = Flow.colors.textPrimary)
                    FlowProgressBar(if (todays.isEmpty()) 0f else doneToday.toFloat() / todays.size, color = Flow.colors.success, height = 5.dp)
                    Text(
                        if (todays.isEmpty()) "برای امروز روتینی زمان‌بندی نشده" else num("$doneToday از ${todays.size} روتین امروز انجام شد"),
                        style = Flow.type.caption, color = Flow.colors.textSecondary,
                    )
                }
            }

            SectionTitle("قالب‌های آماده")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    Chip(preset.label, false, {
                        creating = Routine(
                            id = "", title = preset.title, weekdays = listOf(0, 1, 2, 3, 4, 5, 6),
                            categoryId = preset.categoryId, steps = preset.steps,
                        )
                    }, dot = Color(Categories.byId(preset.categoryId).color))
                }
            }

            SectionTitle("روتین‌های من")
            if (data.routines.isEmpty()) {
                EmptyState(
                    FlowIcons.Repeat, "هنوز روتینی نساخته‌ای",
                    "از یک قالب آماده شروع کن یا با دکمه‌ی + روتین مراحل دلخواه بساز.",
                )
            } else {
                data.routines.forEachIndexed { index, routine ->
                    Appear(delayMs = index.coerceAtMost(8) * 24) {
                        RoutineCard(
                            routine, data, today,
                            onStart = { running = routine },
                            onToggleToday = { vm.toggleRoutine(routine.id, today.toEpochDay()) },
                            onEdit = { editing = routine },
                        )
                    }
                }
            }
        }
        Fab(
            {
                creating = Routine(id = "", title = "", weekdays = listOf(0, 1, 2, 3, 4, 5, 6), steps = listOf(RoutineStep("", 10)))
            },
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = LocalDockContentPadding.current - 8.dp),
        )
    }

    creating?.let { draft ->
        RoutineEditorSheet(draft, isNew = true, onSave = { vm.saveRoutine(it); creating = null }, onDelete = null, onDismiss = { creating = null })
    }
    editing?.let { r ->
        RoutineEditorSheet(r, isNew = false, onSave = { vm.saveRoutine(it); editing = null }, onDelete = { vm.deleteRoutine(it.id); editing = null }, onDismiss = { editing = null })
    }
    running?.let { r ->
        RoutineRunnerDialog(r, data.settings.focusMin, onFinished = { vm.markRoutineDone(r.id, today.toEpochDay()) }, onDismiss = { running = null })
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    data: AppData,
    today: java.time.LocalDate,
    onStart: () -> Unit,
    onToggleToday: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = Flow.colors
    val cat = Categories.byId(routine.categoryId)
    val focusMin = data.settings.focusMin
    val scheduledToday = JalaliDate.weekdayIndex(today) in routine.weekdays
    val checkedToday = RoutineStats.isChecked(data, routine.id, today.toEpochDay())
    val streak = RoutineStats.streak(data, routine, today)
    val done30 = RoutineStats.doneInLast(data, routine.id, today, 30)
    val scheduled30 = RoutineStats.scheduledInLast(routine, today, 30)
    val days = if (routine.weekdays.size == 7) "هر روز" else routine.weekdays.joinToString("، ") { JalaliDate.weekdayNames[it] }

    GlassSurface(
        Modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = onEdit),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(cat.color)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(routine.title, style = Flow.type.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        num(days + (routine.minute?.let { " · " + DateFormats.clock(it) } ?: "")),
                        style = Flow.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        num(durationLabel(routine.totalMinutes(focusMin)) + " · " + routine.effectiveSteps(focusMin).size + " مرحله"),
                        style = Flow.type.caption, color = c.textSecondary, maxLines = 1,
                    )
                }
                if (scheduledToday) {
                    CheckCircle(checkedToday, onToggleToday, size = 26.dp, color = c.success, contentDescription = "ثبت انجام ${routine.title} برای امروز")
                }
                GlassIconButton(FlowIcons.Play, onStart, size = 40.dp, iconSize = 18.dp, contentDescription = "شروع ${routine.title}")
            }
            // سابقه‌ی ۷ روز اخیر
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    for (back in 6 downTo 0) {
                        val day = today.minusDays(back.toLong())
                        val scheduled = JalaliDate.weekdayIndex(day) in routine.weekdays
                        val checked = RoutineStats.isChecked(data, routine.id, day.toEpochDay())
                        val color = when {
                            checked -> c.success
                            scheduled && back > 0 -> c.danger.copy(alpha = 0.55f)
                            scheduled -> c.track
                            else -> c.track.copy(alpha = c.track.alpha * 0.45f)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.size(if (scheduled) 12.dp else 8.dp).clip(CircleShape).background(color))
                            Text(JalaliDate.weekdayInitials[JalaliDate.weekdayIndex(day)], style = Flow.type.caption.copy(fontSize = 9.5.sp), color = c.textSecondary, maxLines = 1)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FlowIcon(FlowIcons.Flame, tint = if (streak > 0) c.warning else c.textSecondary, size = 15.dp)
                        Text(num("$streak روز پیاپی"), style = Flow.type.caption, color = c.textPrimary, maxLines = 1)
                    }
                    Text(
                        if (scheduled30 == 0) "—" else num("$done30 از $scheduled30 در ۳۰ روز اخیر"),
                        style = Flow.type.caption, color = c.textSecondary, maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineEditorSheet(
    draft: Routine,
    isNew: Boolean,
    onSave: (Routine) -> Unit,
    onDelete: ((Routine) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val focusMinFallback = 25
    var title by remember { mutableStateOf(draft.title) }
    var days by remember { mutableStateOf(draft.weekdays.toSet()) }
    var minute by remember { mutableStateOf(draft.minute?.takeIf { it in 0 until 24 * 60 }) }
    var category by remember { mutableStateOf(draft.categoryId) }
    val steps = remember {
        mutableStateListOf<RoutineStep>().also { list ->
            list.addAll(draft.steps.ifEmpty { listOf(RoutineStep("", (draft.pomodoros * focusMinFallback).coerceAtLeast(1))) })
        }
    }
    val total = steps.sumOf { it.minutes }
    val validSteps = steps.filter { it.title.isNotBlank() }

    FlowSheet(onDismiss, if (isNew) "روتین جدید" else "ویرایش روتین") {
        FlowTextField(title, { title = it }, "نام روتین (مثلاً مطالعه‌ی صبحگاهی)")
        Text("روزهای تکرار", style = Flow.type.label, color = Flow.colors.textSecondary)
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            (0..2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    (row * 3..row * 3 + 2).forEach { i ->
                        if (i < JalaliDate.weekdayNames.size) {
                            Chip(
                                JalaliDate.weekdayNames[i],
                                i in days,
                                { days = if (i in days) days - i else days + i },
                                Modifier.weight(1f),
                            )
                        }
                    }
                    if (row == 2) Spacer(Modifier.weight(1f))
                }
            }
        }
        SwitchRow("ساعت شروع", minute != null, { minute = if (it) (minute ?: 7 * 60) else null }, subtitle = "برای نمایش در جدول برنامه‌ریزی روز")
        minute?.let { m -> TimeEditor(m, { minute = it }) }
        Text("دسته‌بندی", style = Flow.type.label, color = Flow.colors.textSecondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Categories.all.forEach { cat -> Chip(cat.name, category == cat.id, { category = cat.id }, dot = Color(cat.color)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("مراحل", Modifier.weight(1f), style = Flow.type.label, color = Flow.colors.textSecondary)
            Text(num("مجموع " + durationLabel(total)), style = Flow.type.caption, color = Flow.colors.textSecondary)
        }
        steps.forEachIndexed { index, step ->
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), level = GlassLevel.Subtle) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(num("${index + 1}"), style = Flow.type.label, color = Flow.colors.accent)
                        FlowTextField(step.title, { steps[index] = step.copy(title = it) }, "نام مرحله", Modifier.weight(1f))
                        if (steps.size > 1) {
                            GlassIconButton(FlowIcons.Close, { steps.removeAt(index) }, size = 32.dp, iconSize = 15.dp, contentDescription = "حذف مرحله")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("مدت", Modifier.weight(1f), style = Flow.type.body, color = Flow.colors.textPrimary)
                        Stepper(step.minutes, { steps[index] = step.copy(minutes = it) }, 1..240, suffix = "دقیقه")
                    }
                }
            }
        }
        if (steps.size < 12) {
            GlassButton("افزودن مرحله", { steps.add(RoutineStep("", 10)) }, Modifier.fillMaxWidth(), icon = FlowIcons.Plus)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                "ذخیره",
                {
                    onSave(
                        draft.copy(
                            id = draft.id.ifEmpty { FlowRepository.newId() },
                            title = title.trim(), weekdays = days.sorted(), minute = minute, categoryId = category,
                            steps = validSteps.map { it.copy(title = it.title.trim()) },
                        ),
                    )
                },
                Modifier.weight(1f), enabled = title.isNotBlank() && days.isNotEmpty() && validSteps.isNotEmpty(),
            )
            GlassButton("انصراف", onDismiss, Modifier.weight(1f))
        }
        if (!isNew && onDelete != null) {
            GlassButton("حذف روتین", { onDelete(draft) }, Modifier.fillMaxWidth(), icon = FlowIcons.Trash, danger = true)
        }
    }
}

/** اجرای مرحله‌به‌مرحله‌ی روتین با زمان واقعی (ساعت دیواری). پایان کامل روتین، انجام امروز را ثبت می‌کند. */
@Composable
fun RoutineRunnerDialog(routine: Routine, focusMin: Int, onFinished: () -> Unit, onDismiss: () -> Unit) {
    val c = Flow.colors
    val steps = remember(routine, focusMin) { routine.effectiveSteps(focusMin) }
    var index by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var endAt by remember { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(steps.first().minutes * 60_000L) }
    val now by rememberNowMs(active = running, periodMs = 250L)
    val left = if (running) (endAt - now).coerceAtLeast(0L) else remaining
    val total = steps[index].minutes * 60_000L

    fun startStep(i: Int) {
        index = i
        remaining = steps[i].minutes * 60_000L
        endAt = System.currentTimeMillis() + remaining
        started = true
        running = true
    }

    fun finish() {
        running = false
        finished = true
        onFinished()
    }

    fun next() {
        if (index + 1 < steps.size) startStep(index + 1) else finish()
    }

    LaunchedEffect(running, endAt) {
        if (running) {
            delay((endAt - System.currentTimeMillis()).coerceAtLeast(0L) + 30L)
            next()
        }
    }

    FlowDialog(onDismiss, routine.title) {
        if (finished) {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowIcon(FlowIcons.Award, tint = c.success, size = 44.dp)
                Text("روتین کامل شد", style = Flow.type.title, color = c.textPrimary)
                Text("انجام امروز برای این روتین ثبت شد.", style = Flow.type.caption, color = c.textSecondary, textAlign = TextAlign.Center)
            }
            PrimaryButton("بستن", onDismiss, Modifier.fillMaxWidth())
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(num("مرحله ${index + 1} از ${steps.size}"), style = Flow.type.caption, color = c.textSecondary)
                Text(steps[index].title, style = Flow.type.heading, color = c.accent, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(num(formatClock((left + 999L) / 1000L)), style = Flow.type.display, color = c.textPrimary, maxLines = 1)
                FlowProgressBar(1f - left.toFloat() / total.coerceAtLeast(1L), color = c.accent, height = 6.dp)
            }
            steps.forEachIndexed { i, step ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val color = when {
                        i < index -> c.success
                        i == index -> c.accent
                        else -> c.track
                    }
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text(
                        step.title, Modifier.weight(1f), style = Flow.type.body,
                        color = if (i == index) c.textPrimary else c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(num(durationLabel(step.minutes)), style = Flow.type.caption, color = c.textSecondary, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    when {
                        running -> "توقف"
                        started -> "ادامه"
                        else -> "شروع"
                    },
                    {
                        when {
                            running -> { remaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0L); running = false }
                            started -> { endAt = System.currentTimeMillis() + remaining; running = true }
                            else -> startStep(0)
                        }
                    },
                    Modifier.weight(1f),
                    icon = if (running) FlowIcons.Pause else FlowIcons.Play,
                )
                GlassButton(if (index + 1 < steps.size) "مرحله‌ی بعد" else "پایان", { next() }, Modifier.weight(1f))
            }
        }
    }
}
