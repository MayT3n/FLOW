package com.flow.focus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import com.flow.focus.ui.motion.Motion
import com.flow.focus.ui.motion.rememberEnterState
import com.flow.focus.ui.glass.LocalBackdropLayer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * تقویم ماهانه‌ی شمسی؛ شنبه در ستون راست. همیشه ۶ ردیف رسم می‌شود تا ارتفاع بین ماه‌ها ثابت بماند و
 * هیچ اطلاعاتی (نقطه‌ی رویداد، انتخاب) اندازه‌ی تقویم را تغییر ندهد. هدر ثابت است و فقط عنوان و شبکه
 * با حرکت جهت‌دار عوض می‌شوند.
 */
@Composable
fun JalaliMonthView(
    year: Int,
    month: Int,
    selectedEpochDay: Long?,
    todayEpochDay: Long,
    marked: Set<Long>,
    onSelect: (LocalDate) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    eventColors: Map<Long, List<Color>> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val c = Flow.colors
    val animations = Flow.glass.animations
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val monthKey = year * 12 + month - 1

    fun monthTransition(distanceDivisor: Int): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
        if (animations) {
            val forward = targetState > initialState
            val enterSign = if (forward xor isRtl) 1 else -1
            (fadeIn(Motion.enter(Motion.Medium)) + slideInHorizontally(Motion.enter(Motion.Medium)) { it * enterSign / distanceDivisor }) togetherWith
                (fadeOut(Motion.exit(Motion.Short)) + slideOutHorizontally(Motion.exit(Motion.Short)) { -it * enterSign / distanceDivisor })
        } else {
            ContentTransform(EnterTransition.None, ExitTransition.None)
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(FlowIcons.ChevronPrev, onPrev, size = 38.dp, iconSize = 18.dp, contentDescription = "ماه قبل")
            AnimatedContent(
                targetState = monthKey,
                modifier = Modifier.weight(1f),
                transitionSpec = monthTransition(3),
                label = "jalali-month-title",
            ) { key ->
                Text(
                    num(DateFormats.monthYear(JalaliDate(Math.floorDiv(key, 12), Math.floorMod(key, 12) + 1, 1))),
                    Modifier.fillMaxWidth(),
                    style = Flow.type.heading,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            GlassIconButton(FlowIcons.ChevronNext, onNext, size = 38.dp, iconSize = 18.dp, contentDescription = "ماه بعد")
        }

        Row(Modifier.fillMaxWidth()) {
            JalaliDate.weekdayInitials.forEach {
                Text(it, Modifier.weight(1f), style = Flow.type.caption, color = c.textSecondary, textAlign = TextAlign.Center, maxLines = 1)
            }
        }

        AnimatedContent(
            targetState = monthKey,
            transitionSpec = monthTransition(5),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(monthKey, isRtl) {
                    var dragDistance = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, delta -> dragDistance += delta },
                        onDragEnd = {
                            if (abs(dragDistance) > 56f) {
                                val towardNext = if (isRtl) dragDistance > 0f else dragDistance < 0f
                                if (towardNext) onNext() else onPrev()
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                },
            label = "jalali-month-grid",
        ) { key ->
            MonthGrid(
                Math.floorDiv(key, 12), Math.floorMod(key, 12) + 1,
                selectedEpochDay, todayEpochDay, marked, eventColors, onSelect,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    selectedEpochDay: Long?,
    todayEpochDay: Long,
    marked: Set<Long>,
    eventColors: Map<Long, List<Color>>,
    onSelect: (LocalDate) -> Unit,
) {
    val c = Flow.colors
    val animations = Flow.glass.animations
    val first = JalaliDate(year, month, 1).toLocalDate()
    val offset = JalaliDate.weekdayIndex(first)
    val length = JalaliDate.monthLength(year, month)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val dayNum = row * 7 + column - offset + 1
                    if (dayNum < 1 || dayNum > length) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = JalaliDate(year, month, dayNum).toLocalDate()
                        val epochDay = date.toEpochDay()
                        val selected = epochDay == selectedEpochDay
                        val isToday = epochDay == todayEpochDay
                        val selectionScale by animateFloatAsState(
                            targetValue = if (selected) 1f else 0f,
                            animationSpec = if (animations) Motion.snappy() else snap(),
                            label = "selected-day",
                        )
                        val labelColor by animateColorAsState(
                            targetValue = if (selected) c.textOnAccent else c.textPrimary,
                            animationSpec = if (animations) tween(Motion.Short) else snap(),
                            label = "day-label",
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .semantics { this.selected = selected }
                                .springClick(role = Role.RadioButton, scaleDown = 0.9f, onClick = { onSelect(date) })
                                .drawBehind {
                                    if (selectionScale > 0.001f) {
                                        drawCircle(
                                            Brush.linearGradient(listOf(c.accent, c.accentAlt)),
                                            radius = size.minDimension * 0.48f * selectionScale,
                                        )
                                    }
                                    if (isToday && selectionScale < 0.999f) {
                                        drawCircle(c.accent.copy(alpha = 1f - selectionScale), style = Stroke(2f))
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(num(dayNum), style = Flow.type.label, color = labelColor, maxLines = 1)
                            val dayDots = eventColors[epochDay].orEmpty().take(3)
                            if (epochDay in marked || dayDots.isNotEmpty()) {
                                Row(
                                    Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    val dots = dayDots.ifEmpty { listOf(c.accent) }
                                    dots.forEach { dotColor ->
                                        Box(
                                            Modifier.size(4.dp).clip(CircleShape)
                                                .background(if (selected) c.textOnAccent else dotColor),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JalaliDatePickerDialog(initialEpochDay: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val init = JalaliDate.of(LocalDate.ofEpochDay(initialEpochDay ?: today.toEpochDay()))
    var anchor by remember { mutableLongStateOf(JalaliDate(init.year, init.month, 1).toLocalDate().toEpochDay()) }
    val a = JalaliDate.of(LocalDate.ofEpochDay(anchor))
    FlowDialog(onDismiss, "انتخاب تاریخ") {
        JalaliMonthView(
            year = a.year, month = a.month, selectedEpochDay = initialEpochDay, todayEpochDay = today.toEpochDay(),
            marked = emptySet(),
            onSelect = { onPick(it.toEpochDay()) },
            onPrev = { anchor = JalaliDate(a.year, a.month, 1).plusMonths(-1).toLocalDate().toEpochDay() },
            onNext = { anchor = JalaliDate(a.year, a.month, 1).plusMonths(1).toLocalDate().toEpochDay() },
        )
        GlassButton("امروز", { onPick(today.toEpochDay()) }, Modifier.fillMaxWidth())
    }
}

/** انتخاب ساعت با دو شمارنده (بدون وابستگی به TimePicker سیستمی تا ظاهر یکپارچه بماند). */
@Composable
fun TimeEditor(minuteOfDay: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ساعت", style = Flow.type.caption, color = Flow.colors.textSecondary)
            Stepper(h, { onChange(it * 60 + m) }, 0..23)
        }
        Text(num(DateFormats.clock(minuteOfDay)), style = Flow.type.title, color = Flow.colors.textPrimary)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("دقیقه", style = Flow.type.caption, color = Flow.colors.textSecondary)
            Stepper(m, { onChange(h * 60 + it) }, 0..55, step = 5)
        }
    }
}

@Composable
fun Fab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Flow.colors
    Box(
        modifier
            .size(58.dp)
            .springClick(onClick = onClick)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c.accent, c.accentAlt)))
            .drawBehind { drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.55f to Color.Transparent)) },
        contentAlignment = Alignment.Center,
    ) { FlowIcon(FlowIcons.Plus, tint = Color.White, size = 28.dp, contentDescription = "افزودن") }
}

/** منوی شناور یکپارچه با دیالوگ‌ها؛ ردیف‌ها، آیکون‌ها و وضعیت مخرب/انتخاب‌شده استانداردند. */
data class MenuAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val subtitle: String? = null,
    val destructive: Boolean = false,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun GlassMenu(expanded: Boolean, onDismiss: () -> Unit, actions: List<MenuAction>) {
    var keepPopup by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded) {
            keepPopup = true
        } else if (keepPopup) {
            // Keep the popup mounted until its exit transition finishes.
            delay(155)
            keepPopup = false
        }
    }
    val menuState = rememberEnterState(expanded)
    if (!keepPopup) return

    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.ui.window.Popup(
        alignment = Alignment.TopEnd,
        offset = androidx.compose.ui.unit.IntOffset(0, with(density) { 50.dp.roundToPx() }),
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val origin = androidx.compose.ui.graphics.TransformOrigin(if (rtl) 0f else 1f, 0f)
        val animations = com.flow.focus.ui.theme.LocalGlass.current.animations
        CompositionLocalProvider(LocalBackdropLayer provides null) {
        AnimatedVisibility(
            visibleState = menuState,
            enter = if (animations) {
                fadeIn(Motion.enter(Motion.Short)) +
                    scaleIn(initialScale = 0.82f, transformOrigin = origin, animationSpec = Motion.snappy()) +
                    slideInVertically(Motion.enter(Motion.Medium)) { -it / 10 }
            } else EnterTransition.None,
            exit = if (animations) {
                fadeOut(Motion.exit(Motion.Fast)) +
                    scaleOut(targetScale = 0.9f, transformOrigin = origin, animationSpec = Motion.exit(Motion.Short))
            } else ExitTransition.None,
        ) {
            com.flow.focus.ui.glass.GlassSurface(
                Modifier.padding(4.dp).width(246.dp),
                shape = RoundedCornerShape(24.dp),
                level = com.flow.focus.ui.glass.GlassLevel.Strong,
                elevated = true,
            ) {
                Column(Modifier.padding(vertical = 5.dp)) {
                    actions.forEachIndexed { index, action ->
                        val tint = when {
                            action.destructive -> Flow.colors.danger
                            action.selected -> Flow.colors.accent
                            else -> Flow.colors.textSecondary
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    onDismiss()
                                    action.onClick()
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(tint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                FlowIcon(action.icon, tint = tint, size = 18.dp)
                            }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    action.label,
                                    style = Flow.type.bodyStrong,
                                    color = if (action.destructive) Flow.colors.danger else Flow.colors.textPrimary,
                                    maxLines = 1,
                                )
                                action.subtitle?.let {
                                    Text(it, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                                }
                            }
                            if (action.selected) {
                                FlowIcon(FlowIcons.Check, tint = Flow.colors.accent, size = 17.dp)
                            }
                        }
                        if (index != actions.lastIndex) {
                            Box(
                                Modifier.padding(start = 53.dp, end = 12.dp)
                                    .fillMaxWidth().height(1.dp).background(Flow.colors.track),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
