package com.flow.focus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.Routine
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.CheckCircle
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.ScreenHeader
import com.flow.focus.ui.components.SectionTitle
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.rememberToday
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalDockContentPadding

@Composable
fun RoutinesScreen(vm: AppViewModel, data: AppData, modifier: Modifier = Modifier) {
    val today = rememberToday()
    val epochDay = today.toEpochDay()
    val weekday = JalaliDate.weekdayIndex(today)
    val todayRoutines = remember(data.routines, weekday) {
        data.routines.filter { weekday in it.weekdays }.sortedWith(compareBy({ it.minute ?: Int.MAX_VALUE }, { it.title }))
    }
    val completed = remember(data.routineChecks, epochDay) {
        todayRoutines.count { "${it.id}|$epochDay" in data.routineChecks }
    }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Routine?>(null) }

    Column(
        modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = LocalDockContentPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("روتین‌ها", trailing = {
            GlassIconButton(FlowIcons.Plus, { creating = true }, contentDescription = "روتین جدید")
        })
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), level = GlassLevel.Subtle) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ریتم امروز", style = Flow.type.heading, color = Flow.colors.textPrimary)
                Text(
                    num("$completed از ${todayRoutines.size} روتین انجام شده"),
                    style = Flow.type.body,
                    color = Flow.colors.textSecondary,
                )
                val ratio = if (todayRoutines.isEmpty()) 0f else completed.toFloat() / todayRoutines.size
                Canvas(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    val h = 5.dp.toPx()
                    drawRoundRect(
                        color = Flow.colors.track,
                        size = Size(size.width, h),
                        cornerRadius = CornerRadius(h / 2f),
                    )
                    drawRoundRect(
                        color = Flow.colors.accent,
                        size = Size(size.width * ratio, h),
                        cornerRadius = CornerRadius(h / 2f),
                    )
                }
            }
        }

        SectionTitle("روتین‌های امروز")
        if (todayRoutines.isEmpty()) {
            EmptyState(FlowIcons.Repeat, "برای امروز روتینی نداری", "یک روتین بساز و روزهای تکرارش را مشخص کن.")
        } else {
            todayRoutines.forEachIndexed { index, routine ->
                val done = "${routine.id}|$epochDay" in data.routineChecks
                AnimatedVisibility(
                    visible = true,
                    enter = if (Flow.glass.animations) fadeIn(tween(170, delayMillis = index.coerceAtMost(6) * 20)) +
                        slideInVertically(tween(190, delayMillis = index.coerceAtMost(6) * 20)) { it / 18 }
                    else androidx.compose.animation.EnterTransition.None,
                ) {
                    RoutineCard(
                        routine = routine,
                        scheduledToday = true,
                        doneToday = done,
                        onToggleToday = { vm.toggleRoutine(routine.id, epochDay) },
                        onEdit = { editing = routine },
                    )
                }
            }
        }

        val otherRoutines = data.routines.filterNot { weekday in it.weekdays }
        if (otherRoutines.isNotEmpty()) {
            SectionTitle("سایر روتین‌ها")
            otherRoutines.forEach { routine ->
                RoutineCard(
                    routine = routine,
                    scheduledToday = false,
                    doneToday = false,
                    onToggleToday = {},
                    onEdit = { editing = routine },
                )
            }
        }
    }

    if (creating) {
        RoutineEditorDialog(
            initial = null,
            onSave = { vm.saveRoutine(it); creating = false },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }
    editing?.let { routine ->
        RoutineEditorDialog(
            initial = routine,
            onSave = { vm.saveRoutine(it); editing = null },
            onDelete = { vm.deleteRoutine(it.id); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    scheduledToday: Boolean,
    doneToday: Boolean,
    onToggleToday: () -> Unit,
    onEdit: () -> Unit,
) {
    var expanded by rememberSaveable(routine.id) { mutableStateOf(false) }
    val weekdays = if (routine.weekdays.size == 7) "هر روز"
        else routine.weekdays.sorted().joinToString("، ") { JalaliDate.weekdayNames[it] }
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), level = GlassLevel.Subtle) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = { expanded = !expanded }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (scheduledToday) {
                    CheckCircle(doneToday, onToggleToday, size = 29.dp)
                } else {
                    FlowIcon(FlowIcons.Repeat, tint = Flow.colors.accent, size = 23.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(routine.title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary, maxLines = 1)
                    val time = routine.minute?.let { " · " + DateFormats.clock(it) } ?: ""
                    Text(num(weekdays + time), style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                }
                FlowIcon(if (expanded) FlowIcons.ChevronUp else FlowIcons.ChevronDown, tint = Flow.colors.textSecondary, size = 18.dp)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = if (Flow.glass.animations) fadeIn(tween(150)) + slideInVertically(tween(180)) { it / 10 }
                    else androidx.compose.animation.EnterTransition.None,
                exit = if (Flow.glass.animations) fadeOut(tween(110))
                    else androidx.compose.animation.ExitTransition.None,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (routine.steps.isEmpty()) {
                        Text("هنوز مرحله‌ای برای این روتین تعریف نشده است.", style = Flow.type.caption, color = Flow.colors.textSecondary)
                    } else {
                        routine.steps.forEachIndexed { index, step ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text(num("${index + 1}"), style = Flow.type.caption, color = Flow.colors.accent)
                                Text(step.title, Modifier.weight(1f), style = Flow.type.body, color = Flow.colors.textPrimary)
                                Text(num("${step.durationMin} دقیقه"), style = Flow.type.caption, color = Flow.colors.textSecondary)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton("ویرایش", onEdit, Modifier.weight(1f), icon = FlowIcons.Edit)
                        if (scheduledToday) {
                            PrimaryButton(
                                if (doneToday) "لغو ثبت امروز" else "ثبت انجام امروز",
                                onToggleToday,
                                Modifier.weight(1f),
                                icon = if (doneToday) FlowIcons.Reset else FlowIcons.Check,
                            )
                        }
                    }
                }
            }
        }
    }
}