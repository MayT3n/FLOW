package com.flow.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.flow.desktop.DesktopMusicCatalog
import com.flow.desktop.DesktopMusicState
import com.flow.desktop.DesktopRuntime
import com.flow.desktop.design.*
import com.flow.focus.core.DateFormats
import com.flow.focus.data.TimerStatus
import com.flow.focus.domain.PomodoroEngine

internal enum class Page(val title: String, val subtitle: String, val icon: FlowIcon) {
    Home("خانه", "نمای کلی و وضعیت امروز", FlowIcon.Home),
    Focus("تمرکز", "زمان‌سنج پومودورو", FlowIcon.Focus),
    Tasks("وظایف", "جست‌وجو و مدیریت وظایف", FlowIcon.Tasks),
    Routines("روتین‌ها", "برنامه‌ی هفتگی تکرارشونده", FlowIcon.Routine),
    Goals("اهداف", "هدف‌های روزانه و هفتگی", FlowIcon.Target),
    Planning("برنامه‌ریزی", "تقویم جلالی و برنامه", FlowIcon.Calendar),
    Stats("آمار", "گزارش زمان تمرکز", FlowIcon.Chart),
    Music("موسیقی", "کتابخانه و پخش‌کننده", FlowIcon.Music),
    Settings("تنظیمات", "ظاهر، تایمر، داده‌ها و ویندوز", FlowIcon.Settings);

    val shortcut: String get() = "Ctrl+${ordinal + 1}"
}

/** The FLOW mark: an infinity-like ribbon drawn as a vector (no bitmap, crisp at every DPI). */
@Composable
internal fun FlowMark(size: androidx.compose.ui.unit.Dp = 30.dp) {
    val pal = Flow.colors
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * .50f, h * .50f)
            cubicTo(w * .36f, h * .18f, w * .06f, h * .20f, w * .06f, h * .50f)
            cubicTo(w * .06f, h * .80f, w * .36f, h * .82f, w * .50f, h * .50f)
            cubicTo(w * .64f, h * .18f, w * .94f, h * .20f, w * .94f, h * .50f)
            cubicTo(w * .94f, h * .80f, w * .64f, h * .82f, w * .50f, h * .50f)
        }
        drawPath(
            p,
            Brush.linearGradient(listOf(pal.accentFillA, pal.accentFillB), Offset(0f, 0f), Offset(w, h)),
            style = Stroke(width = w * .13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

// ───────────────────────── Sidebar ─────────────────────────

@Composable
internal fun Sidebar(
    selected: Page,
    onSelect: (Page) -> Unit,
    compact: Boolean,
    onToggleCompact: () -> Unit,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val width by animateDpAsState(
        if (compact) FlowDims.sidebarCollapsed else FlowDims.sidebarExpanded,
        tween(FlowMotion.ms(FlowMotion.expand, reduce), easing = FlowMotion.emphasized),
        label = "sidebar-width",
    )
    Column(
        modifier.width(width).fillMaxHeight().glass(FlowDims.sidebarRadius, GlassLevel.Chrome)
            .padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(10.dp),
        ) {
            FlowMark(30.dp)
            if (!compact) {
                Column(Modifier.weight(1f)) {
                    FlowText("FLOW", style = FlowType.title2.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
                    FlowText("فضای کار و تمرکز", style = FlowType.caption, color = pal.textTertiary, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Page.entries.forEach { page ->
                SidebarItem(page, page == selected, compact) { onSelect(page) }
            }
        }
        Hairline(Modifier.padding(vertical = 10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowIconButton(
                if (dark) FlowIcon.Sun else FlowIcon.Moon,
                if (dark) "حالت روشن" else "حالت تیره",
                onToggleTheme,
            )
            if (!compact) Spacer(Modifier.weight(1f))
            FlowIconButton(FlowIcon.SidebarToggle, if (compact) "باز کردن نوار کناری" else "جمع کردن نوار کناری", onToggleCompact)
        }
    }
}

@Composable
private fun SidebarItem(page: Page, active: Boolean, compact: Boolean, onClick: () -> Unit) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val ph = rememberPressHover()
    val bg by animateColorAsState(
        when {
            active -> pal.accent.copy(alpha = if (pal.dark) .20f else .14f)
            ph.hovered -> pal.fillSubtle
            else -> Color.Transparent
        },
        tween(FlowMotion.ms(FlowMotion.hover, reduce)),
        label = "sidebar-bg",
    )
    val fg by animateColorAsState(
        if (active) pal.accent else pal.textSecondary,
        tween(FlowMotion.ms(FlowMotion.hover, reduce)),
        label = "sidebar-fg",
    )
    val body: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(FlowDims.controlRadius)).background(bg)
                .flowClickable(ph, true, onClick).padding(horizontal = if (compact) 0.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(11.dp),
        ) {
            FlowIconView(page.icon, fg, size = 20.dp, weight = if (active) 1.9f else 1.65f)
            if (!compact) {
                FlowText(
                    page.title, Modifier.weight(1f),
                    if (active) FlowType.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else FlowType.bodyMedium,
                    if (active) pal.text else pal.textSecondary, maxLines = 1,
                )
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (active) pal.accent else Color.Transparent))
            }
        }
    }
    if (compact) Tip("${page.title}  ·  ${page.shortcut}") { body() } else body()
}

// ───────────────────────── Toolbar ─────────────────────────

@Composable
internal fun Toolbar(
    page: Page,
    greeting: String?,
    now: Long,
    runtime: DesktopRuntime,
    onOpenSearch: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    narrow: Boolean = false,
) {
    val pal = Flow.colors
    val data by runtime.data.collectAsState()
    val timer = data.timer
    val running = timer.status == TimerStatus.RUNNING
    val paused = timer.status == TimerStatus.PAUSED
    Row(
        modifier.fillMaxWidth().height(FlowDims.toolbarHeight).glass(FlowDims.panelRadius - 2.dp, GlassLevel.Chrome)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            FlowText(greeting ?: page.title, style = FlowType.title1, maxLines = 1)
            if (!narrow) FlowText(
                DateFormats.full(java.time.LocalDate.now()).let { numText(it) } + "  ·  " + page.subtitle,
                style = FlowType.caption, color = pal.textTertiary, maxLines = 1,
            )
        }
        // Search
        val ph = rememberPressHover()
        Row(
            Modifier.width(if (narrow) 40.dp else 250.dp).height(36.dp).glass(FlowDims.controlRadius, GlassLevel.Control)
                .clip(RoundedCornerShape(FlowDims.controlRadius))
                .background(if (ph.hovered) pal.fillSubtle else Color.Transparent)
                .flowClickable(ph, true, onOpenSearch).padding(horizontal = if (narrow) 0.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (narrow) Arrangement.Center else Arrangement.spacedBy(8.dp),
        ) {
            FlowIconView(FlowIcon.Search, pal.textTertiary, size = 17.dp)
            if (!narrow) {
                FlowText("جست‌وجو در صفحه‌ها…", Modifier.weight(1f), FlowType.callout, pal.textTertiary, maxLines = 1)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(pal.fillSubtle).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    FlowText("Ctrl K", style = FlowType.caption, color = pal.textTertiary)
                }
            }
        }
        // Focus status / action
        if (running || paused) {
            val remaining = PomodoroEngine.remaining(timer, now) / 1000
            val (m, s) = mmss(remaining)
            FlowButton(
                label = (if (paused) "مکث · " else "") + numText(ltr("$m:$s")),
                onClick = onFocus, kind = ButtonKind.Primary, icon = FlowIcon.Focus,
            )
        } else {
            FlowButton("شروع تمرکز", onFocus, kind = ButtonKind.Primary, icon = FlowIcon.Play)
        }
    }
}

// ───────────────────────── Mini player ─────────────────────────

@Composable
internal fun MiniPlayer(runtime: DesktopRuntime, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val pal = Flow.colors
    val state by runtime.music.state.collectAsState()
    val track = DesktopMusicCatalog.tracks.firstOrNull { it.id == state.trackId } ?: return
    Row(
        modifier.fillMaxWidth().glass(FlowDims.panelRadius - 4.dp, GlassLevel.Chrome).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val ph = rememberPressHover()
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).flowClickable(ph, true, onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            CoverArt(track.id, size = 38.dp, radius = 10.dp, playing = state.playing)
            Column(Modifier.weight(1f)) {
                FlowText(track.title, style = FlowType.headline, maxLines = 1)
                FlowText(DesktopMusicCatalog.artist + " · " + track.mood, style = FlowType.caption, color = pal.textSecondary, maxLines = 1)
            }
        }
        MiniProgress(runtime, Modifier.width(220.dp))
        FlowIconButton(FlowIcon.Previous, "قبلی", { runtime.music.previous() }, filledIcon = true)
        FlowIconButton(
            if (state.playing) FlowIcon.Pause else FlowIcon.Play,
            if (state.playing) "مکث" else "پخش",
            { runtime.music.togglePlayback() },
            selected = true,
        )
        FlowIconButton(FlowIcon.Next, "بعدی", { runtime.music.next() }, filledIcon = true)
        FlowIconButton(FlowIcon.Close, "بستن پخش‌کننده", { runtime.music.stop() }, size = 30.dp, iconSize = 15.dp)
    }
}

/** Isolated so the 4 Hz position updates only recompose this tiny subtree. */
@Composable
private fun MiniProgress(runtime: DesktopRuntime, modifier: Modifier = Modifier) {
    val state by runtime.music.state.collectAsState()
    val pal = Flow.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowText(timeLabel(state.positionMs), style = FlowType.caption, color = pal.textTertiary)
        FlowSlider(
            value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
            onChange = { runtime.music.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.weight(1f), showThumb = false,
        )
        FlowText(timeLabel(state.durationMs), style = FlowType.caption, color = pal.textTertiary)
    }
}

@Composable
internal fun timeLabel(ms: Long): String {
    val (m, s) = mmss(ms / 1000)
    return numText(ltr("${m.trimStart('0').ifEmpty { "0" }}:$s"))
}

// ───────────────────────── Command palette ─────────────────────────

@Composable
internal fun CommandPalette(onSelect: (Page) -> Unit, onDismiss: () -> Unit) {
    val pal = Flow.colors
    var query by remember { mutableStateOf("") }
    var index by remember { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    val results = Page.entries.filter { query.isBlank() || it.title.contains(query.trim(), true) || it.subtitle.contains(query.trim(), true) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(query) { index = 0 }
    FlowDialog(onDismiss, width = 520.dp, modifier = Modifier.onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (e.key) {
            Key.Escape -> { onDismiss(); true }
            Key.DirectionDown -> { if (results.isNotEmpty()) index = (index + 1) % results.size; true }
            Key.DirectionUp -> { if (results.isNotEmpty()) index = (index - 1 + results.size) % results.size; true }
            else -> false
        }
    }) {
        FlowTextField(
            value = query, onValueChange = { query = it }, placeholder = "نام صفحه یا قابلیت…",
            leading = FlowIcon.Search, focusRequester = focus,
            onEnter = { results.getOrNull(index)?.let(onSelect) },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (results.isEmpty()) {
                FlowText("موردی پیدا نشد.", Modifier.padding(vertical = 14.dp), FlowType.body, pal.textSecondary)
            }
            results.forEachIndexed { i, page ->
                val ph = rememberPressHover()
                val active = i == index
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(FlowDims.controlRadius))
                        .background(if (active) pal.accent.copy(alpha = .16f) else if (ph.hovered) pal.fillSubtle else Color.Transparent)
                        .flowClickable(ph, true) { onSelect(page) }.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FlowIconView(page.icon, if (active) pal.accent else pal.textSecondary, size = 19.dp)
                    Column(Modifier.weight(1f)) {
                        FlowText(page.title, style = FlowType.bodyMedium)
                        FlowText(page.subtitle, style = FlowType.caption, color = pal.textSecondary)
                    }
                    FlowText(ltr(page.shortcut), style = FlowType.caption, color = pal.textTertiary)
                }
            }
        }
    }
}
