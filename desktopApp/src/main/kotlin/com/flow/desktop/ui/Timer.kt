package com.flow.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.flow.desktop.DesktopRuntime
import com.flow.desktop.design.*
import com.flow.focus.data.AppData
import com.flow.focus.data.PhaseType
import com.flow.focus.data.TimerStatus
import com.flow.focus.domain.PomodoroEngine

internal fun phaseTitle(p: PhaseType) = when (p) {
    PhaseType.FOCUS -> "تمرکز"
    PhaseType.SHORT_BREAK -> "استراحت کوتاه"
    PhaseType.LONG_BREAK -> "استراحت بلند"
}

/**
 * Timer numerals drawn in fixed-width cells. A second ticking changes only the last digit's cell
 * (which slides), so the dial never shifts or jitters.
 */
@Composable
internal fun TimerDigits(totalSeconds: Long, sizeSp: Int, modifier: Modifier = Modifier) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val persian = LocalPersianDigits.current
    val (m, s) = mmss(totalSeconds)
    val text = "$m:$s"
    val cell = (sizeSp * .62f).dp
    val style = FlowType.timerDigit(sizeSp)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            text.forEachIndexed { i, ch ->
                if (ch == ':') {
                    Box(Modifier.width((sizeSp * .3f).dp).height((sizeSp * 1.2f).dp), contentAlignment = Alignment.Center) {
                        Column(verticalArrangement = Arrangement.spacedBy((sizeSp * .16f).dp)) {
                            repeat(2) { Box(Modifier.size((sizeSp * .085f).dp).clip(CircleShape).background(pal.textSecondary)) }
                        }
                    }
                } else {
                    Box(Modifier.width(cell), contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = ch,
                            transitionSpec = {
                                if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                else (slideInVertically(tween(220)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                                    (slideOutVertically(tween(180)) { it / 3 } + fadeOut(tween(140)))
                            },
                            label = "digit-$i",
                        ) { c ->
                            FlowText(digits(c.toString(), persian), style = style, color = pal.text, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TimerCard(
    data: AppData,
    now: Long,
    runtime: DesktopRuntime,
    modifier: Modifier = Modifier,
    ringSize: Dp = 230.dp,
    digitSp: Int = 46,
    showPhaseSwitcher: Boolean = true,
    showTaskPicker: Boolean = true,
) {
    val pal = Flow.colors
    val timer = data.timer
    val running = timer.status == TimerStatus.RUNNING
    val remaining = PomodoroEngine.remaining(timer, now)
    // Ceil so the dial shows 25:00 until the first full second has elapsed.
    val seconds = (remaining + 999L) / 1000L
    val progress = PomodoroEngine.progress(timer, now)
    val statusLabel = when (timer.status) {
        TimerStatus.RUNNING -> "در حال اجرا"
        TimerStatus.PAUSED -> "مکث شده"
        TimerStatus.IDLE -> "آماده شروع"
    }
    val statusColor = if (running) pal.success else pal.textSecondary
    GlassPanel(modifier, verticalSpacing = 14.dp) {
        if (showPhaseSwitcher) {
            Segmented(
                PhaseType.entries.map { it to phaseTitle(it) }, timer.phase,
                { runtime.selectPhase(it) }, Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f, fill = false), contentAlignment = Alignment.Center) {
            ProgressRing(progress, size = ringSize, stroke = (ringSize.value * .045f).dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FlowIconView(if (timer.phase == PhaseType.FOCUS) FlowIcon.Leaf else FlowIcon.Coffee, pal.accent, size = 20.dp)
                    TimerDigits(seconds, digitSp)
                    FlowText(phaseTitle(timer.phase), style = FlowType.callout, color = pal.textSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        FlowText(statusLabel, style = FlowType.caption, color = statusColor)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowIconButton(FlowIcon.Reset, "بازنشانی (Ctrl+R)", { runtime.reset() }, size = 40.dp)
                FlowButton(
                    label = when (timer.status) { TimerStatus.RUNNING -> "مکث"; TimerStatus.PAUSED -> "ادامه"; else -> "شروع تمرکز" }.let {
                        if (timer.status == TimerStatus.IDLE && timer.phase != PhaseType.FOCUS) "شروع استراحت" else it
                    },
                    onClick = { if (running) runtime.pause() else runtime.start() },
                    kind = ButtonKind.Primary,
                    icon = if (running) FlowIcon.Pause else FlowIcon.Play,
                    modifier = Modifier.widthIn(min = 150.dp).height(44.dp),
                )
                FlowIconButton(FlowIcon.Skip, "رد کردن و رفتن به فاز بعدی", { runtime.skip() }, size = 40.dp)
            }
        }
        if (showTaskPicker) {
            val open = data.tasks.filterNot { it.done }
            val selected = open.firstOrNull { it.id == timer.taskId }
            FlowDropdown(
                selectedLabel = selected?.title ?: "بدون وظیفه‌ی مرتبط",
                options = listOf<Pair<String?, String>>(null to "بدون وظیفه‌ی مرتبط") + open.map { it.id to it.title },
                selected = selected?.id,
                onSelect = { runtime.selectTask(it) },
                modifier = Modifier.fillMaxWidth(), leading = FlowIcon.Tasks, menuWidth = 300.dp,
            )
        }
        // Cycle: filled pills for rounds reached, plus the exact durations in use.
        Hairline()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(data.settings.rounds.coerceIn(1, 12)) { i ->
                    val reached = i + 1 < timer.round || (i + 1 == timer.round && timer.phase == PhaseType.FOCUS && timer.status != TimerStatus.IDLE)
                    Box(
                        Modifier.width(if (i + 1 == timer.round) 22.dp else 10.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (reached) Brush.linearGradient(listOf(pal.accentFillA, pal.accentFillB)) else Brush.linearGradient(listOf(pal.fillStrong, pal.fillStrong))),
                    )
                }
                FlowText("دور ${num(timer.round)} از ${num(data.settings.rounds)}", Modifier.padding(start = 6.dp), FlowType.caption, pal.textSecondary)
            }
            FlowText(
                "${num(data.settings.focusMin)} · ${num(data.settings.shortBreakMin)} · ${num(data.settings.longBreakMin)} دقیقه",
                style = FlowType.caption, color = pal.textTertiary,
            )
        }
    }
}
