package com.flow.focus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.motion.Motion
import com.flow.focus.ui.theme.Flow
import java.time.LocalDate
import kotlin.math.abs

/**
 * تقویم برنامه‌ریزی با دو حالت: ماهانه (باز) و نوار هفتگی (جمع‌شده).
 * هر دو حالت ارتفاع قطعی دارند؛ تغییر حالت با انیمیشن اندازه‌ی نرم انجام می‌شود و دکمه‌ی افزودن
 * (که خارج از این ساختار است) هرگز روی ابعاد تقویم اثر ندارد.
 */
@Composable
fun PlannerCalendar(
    expanded: Boolean,
    selected: LocalDate,
    today: LocalDate,
    monthAnchor: Long,
    dotsByDay: Map<Long, List<Color>>,
    onSelect: (LocalDate) -> Unit,
    onMonthAnchor: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animations = Flow.glass.animations
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                if (animations) {
                    (fadeIn(Motion.enter(Motion.Medium, 50)) togetherWith fadeOut(Motion.exit(Motion.Fast))) using
                        SizeTransform(clip = true) { _, _ -> Motion.soft() }
                } else {
                    ContentTransform(EnterTransition.None, ExitTransition.None)
                }
            },
            label = "planner-calendar-mode",
        ) { isExpanded ->
            if (isExpanded) {
                val a = JalaliDate.of(LocalDate.ofEpochDay(monthAnchor))
                Column(Modifier.padding(14.dp)) {
                    JalaliMonthView(
                        a.year, a.month, selected.toEpochDay(), today.toEpochDay(), emptySet(),
                        onSelect = onSelect,
                        onPrev = { onMonthAnchor(JalaliDate(a.year, a.month, 1).plusMonths(-1).toLocalDate().toEpochDay()) },
                        onNext = { onMonthAnchor(JalaliDate(a.year, a.month, 1).plusMonths(1).toLocalDate().toEpochDay()) },
                        eventColors = dotsByDay,
                    )
                }
            } else {
                WeekStrip(selected, today, dotsByDay, onSelect)
            }
        }
    }
}

private fun weekTransition(isRtl: Boolean, animations: Boolean): AnimatedContentTransitionScope<Long>.() -> ContentTransform = {
    if (animations) {
        val forward = targetState > initialState
        val sign = if (forward xor isRtl) 1 else -1
        (fadeIn(Motion.enter(Motion.Medium)) + slideInHorizontally(Motion.enter(Motion.Medium)) { it * sign / 4 }) togetherWith
            (fadeOut(Motion.exit(Motion.Short)) + slideOutHorizontally(Motion.exit(Motion.Short)) { -it * sign / 4 })
    } else {
        ContentTransform(EnterTransition.None, ExitTransition.None)
    }
}

@Composable
private fun WeekStrip(
    selected: LocalDate,
    today: LocalDate,
    dots: Map<Long, List<Color>>,
    onSelect: (LocalDate) -> Unit,
) {
    val c = Flow.colors
    val animations = Flow.glass.animations
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val start = JalaliDate.startOfWeek(selected)
    val selectedEpoch = selected.toEpochDay()
    val spec = weekTransition(isRtl, animations)

    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(FlowIcons.ChevronPrev, { onSelect(selected.minusDays(7)) }, size = 38.dp, iconSize = 18.dp, contentDescription = "هفته‌ی قبل")
            AnimatedContent(
                targetState = start.toEpochDay(),
                modifier = Modifier.weight(1f),
                transitionSpec = spec,
                label = "week-title",
            ) { weekStart ->
                val from = LocalDate.ofEpochDay(weekStart)
                Text(
                    num(DateFormats.short(from) + " تا " + DateFormats.short(from.plusDays(6))),
                    Modifier.fillMaxWidth(),
                    style = Flow.type.heading, color = c.textPrimary, textAlign = TextAlign.Center, maxLines = 1,
                )
            }
            GlassIconButton(FlowIcons.ChevronNext, { onSelect(selected.plusDays(7)) }, size = 38.dp, iconSize = 18.dp, contentDescription = "هفته‌ی بعد")
        }
        AnimatedContent(
            targetState = start.toEpochDay(),
            transitionSpec = spec,
            modifier = Modifier.fillMaxWidth().pointerInput(selectedEpoch, isRtl) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, delta -> drag += delta },
                    onDragEnd = {
                        if (abs(drag) > 56f) {
                            val towardNext = if (isRtl) drag > 0f else drag < 0f
                            onSelect(if (towardNext) selected.plusDays(7) else selected.minusDays(7))
                        }
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                )
            },
            label = "week-days",
        ) { weekStart ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 0..6) {
                    val day = LocalDate.ofEpochDay(weekStart + i)
                    val epoch = day.toEpochDay()
                    val isSelected = epoch == selectedEpoch
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) c.accent else Color.Transparent,
                        animationSpec = if (animations) tween(Motion.Short) else snap(),
                        label = "week-day-bg",
                    )
                    val fg by animateColorAsState(
                        targetValue = if (isSelected) c.textOnAccent else c.textPrimary,
                        animationSpec = if (animations) tween(Motion.Short) else snap(),
                        label = "week-day-fg",
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .semantics { this.selected = isSelected }
                            .clip(RoundedCornerShape(17.dp))
                            .background(bg)
                            .springClick(role = Role.Tab, scaleDown = 0.94f, onClick = { onSelect(day) })
                            .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            JalaliDate.weekdayInitials[i],
                            style = Flow.type.caption,
                            color = if (isSelected) c.textOnAccent.copy(alpha = 0.82f) else c.textSecondary,
                        )
                        Text(num(JalaliDate.of(day).day), style = Flow.type.bodyStrong, color = fg)
                        Row(Modifier.padding(top = 1.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            val dayDots = dots[epoch].orEmpty()
                            if (dayDots.isEmpty()) {
                                Box(Modifier.size(4.dp).clip(CircleShape).background(if (day == today && !isSelected) c.accent else Color.Transparent))
                            } else {
                                dayDots.take(3).forEach { dot ->
                                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) c.textOnAccent else dot))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
