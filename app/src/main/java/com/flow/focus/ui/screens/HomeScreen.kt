package com.flow.focus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.core.DateFormats
import com.flow.focus.core.formatClock
import com.flow.focus.data.AppData
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Task
import com.flow.focus.data.ThemeMode
import com.flow.focus.data.TimerState
import com.flow.focus.data.TimerStatus
import com.flow.focus.domain.PomodoroEngine
import com.flow.focus.domain.Planner
import com.flow.focus.domain.Stats
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.relativeDay
import com.flow.focus.ui.rememberNowMs
import com.flow.focus.ui.components.CheckCircle
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.GlassMenu
import com.flow.focus.ui.components.MenuAction
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.Stepper
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalGlass
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class HomeDialog { PICK_TASK, DURATION, AMBIENT, REMINDER, NEW_TASK }

@Composable
fun HomeScreen(vm: AppViewModel, data: AppData, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Flow.colors
    val timer = data.timer
    val settings = data.settings
    val running = timer.status == TimerStatus.RUNNING
    val now = rememberNowMs(active = running)
    val clock by rememberNowMs(active = true, periodMs = 30_000L)
    val dateTime = remember(clock) { LocalDateTime.ofInstant(Instant.ofEpochMilli(clock), ZoneId.systemDefault()) }
    val today = dateTime.toLocalDate()
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }
    var menu by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var musicOpen by remember { mutableStateOf(false) }
    val playing by vm.ambientPlaying.collectAsStateWithLifecycle()
    val newsItems by vm.newsItems.collectAsStateWithLifecycle()
    val newsReadIds by vm.newsReadIds.collectAsStateWithLifecycle()
    val remoteUpdate by vm.updateManifest.collectAsStateWithLifecycle()
    val updateNoticeUnread = remoteUpdate?.let {
        it.updateAvailable && it.versionCode > com.flow.focus.BuildConfig.VERSION_CODE && "update:${it.versionCode}" !in newsReadIds
    } == true
    val unreadNotices = newsItems.count { it.id !in newsReadIds } + if (updateNoticeUnread) 1 else 0

    LaunchedEffect(Unit) { vm.refreshRemoteContent() }

    // پایان فاز در پیش‌زمینه: دقیقاً در لحظه‌ی پایان (Alarm سیستم برای پس‌زمینه جداگانه ثبت شده است)
    LaunchedEffect(timer.status, timer.endAtMs) {
        if (timer.status == TimerStatus.RUNNING) {
            kotlinx.coroutines.delay((timer.endAtMs - System.currentTimeMillis()).coerceAtLeast(0L) + 30L)
            vm.onTimerEnd()
        }
    }

    val task = remember(data.tasks, timer.taskId) { data.tasks.firstOrNull { it.id == timer.taskId } }
    val doneForTask = remember(data.sessions, task?.id) { task?.let { Stats.pomodorosDone(data.sessions, it.id) } ?: 0 }
    val sessionsToday = remember(data.sessions, today) { Stats.completedOn(data.sessions, today) }
    val minutesToday = remember(data.sessions, today) { Stats.focusMinutesOn(data.sessions, today) }
    val next = remember(data.tasks, data.routines, data.routineChecks, clock) { Planner.next(data, dateTime) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- سربرگ ----
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassSurface(Modifier.size(42.dp), shape = CircleShape, level = GlassLevel.Subtle) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FlowIcon(FlowIcons.MatFocus, tint = c.accent, size = 23.dp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("MatFocus", style = Flow.type.title.copy(letterSpacing = 0.6.sp), color = c.textPrimary)
                    Text(num(DateFormats.weekdayAndDay(today)), style = Flow.type.label, color = c.textSecondary)
                }
            }
            Box {
                GlassIconButton(FlowIcons.Bell, {
                    notificationsOpen = true
                    vm.refreshRemoteContent()
                }, contentDescription = "اعلان‌ها")
                AnimatedVisibility(
                    visible = unreadNotices > 0,
                    enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.55f, animationSpec = tween(190)),
                    exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.65f, animationSpec = tween(110)),
                ) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 1.dp)
                            .size(17.dp).clip(CircleShape).background(c.danger),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            num(unreadNotices.coerceAtMost(9)),
                            style = Flow.type.caption.copy(fontSize = 9.sp),
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
            GlassIconButton(
                if (c.isDark) FlowIcons.Sun else FlowIcons.Moon,
                { vm.updateSettings { it.copy(theme = if (c.isDark) ThemeMode.LIGHT else ThemeMode.DARK) } },
                contentDescription = "تغییر تم",
            )
            Box {
                GlassIconButton(FlowIcons.More, { menu = true }, contentDescription = "منو")
                GlassMenu(
                    menu, { menu = false },
                    listOf(
                        MenuAction("تغییر وظیفه", FlowIcons.Edit) { dialog = HomeDialog.PICK_TASK },
                        MenuAction("مدت تمرکز", FlowIcons.Clock) { dialog = HomeDialog.DURATION },
                        MenuAction("صدای محیط", FlowIcons.Music) { dialog = HomeDialog.AMBIENT },
                        MenuAction("موزیک پلیر", FlowIcons.Music) { musicOpen = true },
                        MenuAction("افزودن یادآور", FlowIcons.Bell) { dialog = HomeDialog.REMINDER },
                        MenuAction("آمار جلسه", FlowIcons.Stats) { onNavigate(3) },
                        MenuAction("تنظیمات تایمر", FlowIcons.Settings) { onNavigate(4) },
                    ),
                )
            }
        }

        // ---- حلقه‌ی تایمر ----
        Column(
            Modifier.fillMaxWidth().padding(top = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimerRing(timer, now, settings.rounds, Modifier.size(276.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val totalRounds = settings.rounds.coerceAtLeast(1)
                repeat(totalRounds) { index ->
                    val roundIndex = index + 1
                    val done = roundIndex < timer.round
                    val current = roundIndex == timer.round
                    Box(
                        Modifier.size(if (current) 9.dp else 7.dp).clip(CircleShape)
                            .background(if (done) c.textSecondary else if (current) c.accent else c.track),
                    )
                }
                Text(num("دور ${timer.round} از $totalRounds"), style = Flow.type.caption, color = c.textSecondary)
            }
        }

        // ---- کنترل‌ها ----
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundControl(FlowIcons.Skip, "رد کردن جلسه", { vm.skip() })
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MainControl(running) { if (running) vm.pause() else vm.start() }
                Text(
                    when (timer.status) {
                        TimerStatus.RUNNING -> "توقف"
                        TimerStatus.PAUSED -> "ادامه"
                        TimerStatus.IDLE -> "شروع"
                    },
                    style = Flow.type.label, color = c.textSecondary,
                )
            }
            RoundControl(FlowIcons.Reset, "بازنشانی", { vm.reset() })
        }

        SegmentedControl(
            listOf("تمرکز", "استراحت کوتاه", "استراحت بلند"),
            timer.phase.ordinal,
            { vm.selectPhase(PhaseType.entries[it]) },
            enabled = !running,
        )

        // ---- وظیفه‌ی فعال ----
        GlassSurface(
            Modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = { dialog = HomeDialog.PICK_TASK }),
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (task != null) {
                    CheckCircle(task.done, { vm.setTaskDone(task.id, !task.done) }, size = 30.dp)
                } else {
                    FlowIcon(FlowIcons.Target, tint = c.textSecondary, size = 28.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        task?.title ?: "وظیفه‌ای انتخاب نشده",
                        style = Flow.type.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (task != null) num("هدف: $doneForTask از ${task.estimatedPomodoros} جلسه") else "برای انتخاب وظیفه لمس کن",
                        style = Flow.type.caption, color = c.textSecondary,
                    )
                }
                FlowIcon(FlowIcons.ChevronNext, tint = c.textSecondary, size = 20.dp)
            }
        }

        // ---- آمار امروز ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(FlowIcons.Target, num(sessionsToday), "جلسه‌ی امروز", c.success, Modifier.weight(1f))
            StatTile(FlowIcons.Clock, num(minutesToday), "دقیقه تمرکز امروز", c.accent, Modifier.weight(1f), goal = minutesToday.toFloat() / settings.dailyGoalMin.coerceAtLeast(1))
        }

        // ---- برنامه‌ی بعدی ----
        GlassSurface(
            Modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = { onNavigate(1) }),
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowIcon(FlowIcons.Calendar, tint = c.accent, size = 28.dp)
                Column(Modifier.weight(1f)) {
                    Text("برنامه‌ی بعدی", style = Flow.type.caption, color = c.textSecondary)
                    Text(
                        next?.first?.title ?: "برنامه‌ای در پیش نیست",
                        style = Flow.type.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (next != null) {
                    val (entry, day) = next
                    val whenText = relativeDay(day.toEpochDay(), today) + (entry.minute?.let { " · " + DateFormats.clock(it) } ?: "")
                    Text(num(whenText), style = Flow.type.label, color = c.textSecondary)
                }
                FlowIcon(FlowIcons.ChevronNext, tint = c.textSecondary, size = 20.dp)
            }
        }
    }

    // ---- دیالوگ‌ها ----
    when (dialog) {
        HomeDialog.PICK_TASK -> TaskPickerDialog(
            data.tasks, timer.taskId,
            onPick = { vm.selectTask(it); dialog = null },
            onCreate = { dialog = HomeDialog.NEW_TASK },
            onDismiss = { dialog = null },
        )
        HomeDialog.NEW_TASK -> TaskEditorDialog(
            initial = null,
            template = Task("", "", dueDay = today.toEpochDay()),
            onSave = { vm.saveTask(it); vm.selectTask(it.id); dialog = null },
            onDelete = null,
            onDismiss = { dialog = null },
        )
        HomeDialog.REMINDER -> TaskEditorDialog(
            initial = null,
            template = Task("", "", dueDay = today.toEpochDay(), dueMinute = ((dateTime.hour + 1) % 24) * 60),
            onSave = { vm.saveTask(it); dialog = null },
            onDelete = null,
            onDismiss = { dialog = null },
        )
        HomeDialog.DURATION -> FlowDialog({ dialog = null }, "مدت‌ها") {
            DurationRow("تمرکز", settings.focusMin, 1..180) { v -> vm.updateSettings { it.copy(focusMin = v) } }
            DurationRow("استراحت کوتاه", settings.shortBreakMin, 1..60) { v -> vm.updateSettings { it.copy(shortBreakMin = v) } }
            DurationRow("استراحت بلند", settings.longBreakMin, 1..90) { v -> vm.updateSettings { it.copy(longBreakMin = v) } }
            PrimaryButton("تأیید", { dialog = null }, Modifier.fillMaxWidth())
        }
        HomeDialog.AMBIENT -> FlowDialog({ vm.stopAmbientPreview(); dialog = null }, "صدای محیط") {
            AmbientPicker(
                settings, playing,
                onSelect = { id -> vm.updateSettings { it.copy(ambientSound = id) } },
                onPreview = { vm.previewAmbient(it) },
                onVolume = { v -> vm.updateSettings { it.copy(ambientVolume = v) } },
                onAuto = { v -> vm.updateSettings { it.copy(ambientAuto = v) } },
            )
            PrimaryButton("تأیید", { vm.stopAmbientPreview(); dialog = null }, Modifier.fillMaxWidth())
        }
        null -> Unit
    }

    if (notificationsOpen) MatFocusNotificationsDialog(vm) { notificationsOpen = false }
    if (musicOpen) MusicPlayerDialog(vm) { musicOpen = false }
}

@Composable
private fun DurationRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = Flow.type.body, color = Flow.colors.textPrimary)
        Stepper(value, onChange, range, suffix = "دقیقه")
    }
}

@Composable
private fun StatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, caption: String, tint: Color, modifier: Modifier, goal: Float? = null) {
    GlassSurface(modifier, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowIcon(icon, tint = tint, size = 26.dp)
            Text(value, style = Flow.type.title, color = Flow.colors.textPrimary)
            Text(caption, style = Flow.type.caption, color = Flow.colors.textSecondary)
            if (goal != null) FlowProgressBar(goal, color = tint, height = 4.dp)
        }
    }
}

@Composable
private fun RoundControl(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    GlassIconButton(icon, onClick, size = 48.dp, iconSize = 21.dp, contentDescription = description)
}

@Composable
private fun MainControl(running: Boolean, onClick: () -> Unit) {
    val c = Flow.colors
    Box(
        Modifier
            .size(72.dp)
            .springClick(scaleDown = 0.93f, onClick = onClick)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c.accent, c.accentAlt)))
            .drawBehind { drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.32f), 0.55f to Color.Transparent)) },
        contentAlignment = Alignment.Center,
    ) {
        FlowIcon(if (running) FlowIcons.Pause else FlowIcons.Play, tint = Color.White, size = 38.dp, contentDescription = if (running) "توقف" else "شروع")
    }
}

@Composable
private fun TimerRing(timer: TimerState, now: State<Long>, rounds: Int, modifier: Modifier) {
    val c = Flow.colors
    val glass = LocalGlass.current
    val breakPhase = timer.phase != PhaseType.FOCUS
    val c1 by animateColorAsState(if (breakPhase) c.success else c.accent, tween(500), label = "ringA")
    val c2 by animateColorAsState(if (breakPhase) c.accent else c.accentAlt, tween(500), label = "ringB")
    // فقط وقتی «ثانیه» عوض می‌شود Recomposition رخ می‌دهد، نه با هر تیک
    val seconds by remember(timer) { derivedStateOf { (PomodoroEngine.remaining(timer, now.value) + 999L) / 1000L } }
    val progress by animateFloatAsState(
        targetValue = PomodoroEngine.progress(timer, now.value),
        animationSpec = if (glass.animations) tween(900, easing = LinearEasing) else snap(),
        label = "timer-ring-progress",
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f + 4.dp.toPx()
            val arc = Size(size.width - 2 * inset, size.height - 2 * inset)
            val tl = Offset(inset, inset)
            drawArc(c.track, 0f, 360f, false, tl, arc, style = Stroke(stroke))
            val p = progress
            if (p > 0f) {
                val sweep = 360f * p
                rotate(-90f, center) {
                    if (glass.quality != GlassQuality.LOW) {
                        drawArc(c1.copy(alpha = 0.22f), 0f, sweep, false, tl, arc, style = Stroke(stroke * 2.1f, cap = StrokeCap.Round))
                    }
                    drawArc(Brush.sweepGradient(listOf(c1, c2, c1), center), 0f, sweep, false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                val ang = (-90f + sweep) * (PI.toFloat() / 180f)
                val r = arc.width / 2f
                drawCircle(Color.White, stroke * 0.28f, Offset(center.x + r * cos(ang), center.y + r * sin(ang)))
            }
        }
        GlassSurface(Modifier.size(222.dp), shape = CircleShape, level = GlassLevel.Subtle) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                FlowIcon(
                    when (timer.phase) {
                        PhaseType.FOCUS -> FlowIcons.Leaf
                        PhaseType.SHORT_BREAK -> FlowIcons.Wind
                        PhaseType.LONG_BREAK -> FlowIcons.Moon
                    },
                    tint = c1, size = 26.dp,
                )
                Text(
                    when (timer.phase) {
                        PhaseType.FOCUS -> "تمرکز"
                        PhaseType.SHORT_BREAK -> "استراحت کوتاه"
                        PhaseType.LONG_BREAK -> "استراحت بلند"
                    },
                    style = Flow.type.label,
                    color = c1,
                )
                Spacer(Modifier.height(2.dp))
                Text(num(formatClock(seconds)), style = Flow.type.display, color = c.textPrimary, maxLines = 1)
                Text(
                    when (timer.status) {
                        TimerStatus.IDLE -> "آماده‌ی شروع"
                        TimerStatus.PAUSED -> "متوقف شده"
                        TimerStatus.RUNNING -> "زمان باقی‌مانده"
                    },
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.track.copy(alpha = if (c.isDark) 0.72f else 0.9f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    style = Flow.type.caption,
                    color = c.textSecondary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}
