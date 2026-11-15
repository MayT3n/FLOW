package com.flow.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.flow.desktop.design.*
import kotlin.math.max
import kotlin.math.min

// ───────────────────────────── Text ─────────────────────────────

@Composable
internal fun FlowText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = FlowType.body,
    color: Color = Flow.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign ?: style.textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
internal fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Flow.colors.hairline))
}

// ───────────────────────── Interaction ─────────────────────────

internal class PressHover(val source: MutableInteractionSource, val hovered: Boolean, val pressed: Boolean)

@Composable
internal fun rememberPressHover(): PressHover {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    return PressHover(source, hovered, pressed)
}

/** Click target with hover + press tracking and a hand cursor. Never shows the default ripple. */
@Composable
internal fun Modifier.flowClickable(ph: PressHover, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.hoverable(ph.source)
        .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
        .clickable(interactionSource = ph.source, indication = null, enabled = enabled, onClick = onClick)

@Composable
private fun Modifier.pressScale(pressed: Boolean, amount: Float = .965f): Modifier {
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (pressed && !reduce) amount else 1f,
        tween(FlowMotion.ms(FlowMotion.press, reduce), easing = FlowMotion.standard),
        label = "press",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

// ───────────────────────── Tooltips ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Tip(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            CompositionLocalProvider(LocalBackdrop provides null) {
                FlowText(
                    text,
                    Modifier.glass(9.dp, GlassLevel.Floating).padding(horizontal = 10.dp, vertical = 5.dp),
                    FlowType.caption,
                    Flow.colors.text,
                )
            }
        },
        modifier = modifier,
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 20.dp)),
        content = content,
    )
}

// ───────────────────────── Buttons ─────────────────────────

internal enum class ButtonKind { Primary, Secondary, Ghost, Danger }

@Composable
internal fun FlowButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Secondary,
    icon: FlowIcon? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val pal = Flow.colors
    val ph = rememberPressHover()
    val reduce = LocalReduceMotion.current
    val radius = if (compact) 10.dp else FlowDims.controlRadius
    val height = if (compact) 30.dp else 38.dp
    val contentColor = when (kind) {
        ButtonKind.Primary -> pal.onAccent
        ButtonKind.Danger -> pal.danger
        else -> pal.text
    }
    val hoverAlpha by animateFloatAsState(
        if (ph.hovered && enabled) 1f else 0f,
        tween(FlowMotion.ms(FlowMotion.hover, reduce)),
        label = "btn-hover",
    )
    val base = modifier
        .height(height)
        .alpha(if (enabled) 1f else .45f)
        .pressScale(ph.pressed && enabled)
    val styled = when (kind) {
        ButtonKind.Primary -> base
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(pal.accentFillA, pal.accentFillB)))
            .drawHoverVeil(hoverAlpha, Color.White, .14f)
        ButtonKind.Ghost -> base
            .clip(RoundedCornerShape(radius))
            .drawHoverVeil(hoverAlpha, pal.text, .08f)
        ButtonKind.Danger -> base
            .clip(RoundedCornerShape(radius))
            .background(pal.danger.copy(alpha = .10f))
            .drawHoverVeil(hoverAlpha, pal.danger, .14f)
        ButtonKind.Secondary -> base
            .glass(radius, GlassLevel.Control)
            .clip(RoundedCornerShape(radius))
            .drawHoverVeil(hoverAlpha, pal.text, .07f)
    }
    Row(
        styled.flowClickable(ph, enabled, onClick).padding(horizontal = if (compact) 11.dp else 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) FlowIconView(icon, contentColor, size = if (compact) 15.dp else 17.dp)
        FlowText(label, style = if (compact) FlowType.captionMedium else FlowType.bodyMedium, color = contentColor, maxLines = 1)
    }
}

private fun Modifier.alpha(a: Float): Modifier = this.graphicsLayer { alpha = a }

private fun Modifier.drawHoverVeil(alpha: Float, color: Color, max: Float): Modifier =
    this.drawBehind { if (alpha > 0.001f) drawRect(color.copy(alpha = max * alpha)) }

@Composable
internal fun FlowIconButton(
    icon: FlowIcon,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    size: Dp = 36.dp,
    iconSize: Dp = 19.dp,
    tint: Color? = null,
    enabled: Boolean = true,
    filledIcon: Boolean = false,
) {
    val pal = Flow.colors
    val ph = rememberPressHover()
    val reduce = LocalReduceMotion.current
    val bg by animateColorAsState(
        when {
            selected -> pal.accent.copy(alpha = .16f)
            ph.hovered && enabled -> pal.fillHover
            else -> Color.Transparent
        },
        tween(FlowMotion.ms(FlowMotion.hover, reduce)),
        label = "icon-btn-bg",
    )
    val color = tint ?: if (selected) pal.accent else pal.textSecondary
    Tip(tooltip, modifier) {
        Box(
            Modifier.size(size)
                .alpha(if (enabled) 1f else .4f)
                .pressScale(ph.pressed && enabled)
                .clip(RoundedCornerShape(FlowDims.controlRadius))
                .background(bg)
                .flowClickable(ph, enabled, onClick),
            contentAlignment = Alignment.Center,
        ) { FlowIconView(icon, color, size = iconSize, filled = filledIcon) }
    }
}

// ───────────────────────── Panels ─────────────────────────

@Composable
internal fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = FlowDims.panelRadius,
    level: GlassLevel = GlassLevel.Panel,
    padding: PaddingValues = PaddingValues(FlowDims.panelPadding),
    verticalSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.glass(radius, level).padding(padding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

@Composable
internal fun PanelHeader(
    title: String,
    icon: FlowIcon? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val pal = Flow.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (icon != null) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(pal.accent.copy(alpha = .13f)),
                contentAlignment = Alignment.Center,
            ) { FlowIconView(icon, pal.accent, size = 16.dp) }
        }
        FlowText(title, Modifier.weight(1f), FlowType.title2, maxLines = 1)
        trailing()
    }
}

@Composable
internal fun EmptyState(icon: FlowIcon, title: String, subtitle: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val pal = Flow.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(pal.fillSubtle),
            contentAlignment = Alignment.Center,
        ) { FlowIconView(icon, pal.textTertiary, size = 22.dp) }
        FlowText(title, style = FlowType.headline, textAlign = TextAlign.Center)
        FlowText(subtitle, style = FlowType.caption, color = pal.textSecondary, textAlign = TextAlign.Center)
        if (action != null) Box(Modifier.padding(top = 4.dp)) { action() }
    }
}

// ───────────────────────── Chips / segmented ─────────────────────────

@Composable
internal fun FlowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    icon: FlowIcon? = null,
) {
    val pal = Flow.colors
    val ph = rememberPressHover()
    val reduce = LocalReduceMotion.current
    val bg by animateColorAsState(
        when {
            selected -> pal.accent.copy(alpha = .18f)
            ph.hovered -> pal.fillHover
            else -> pal.fillSubtle
        },
        tween(FlowMotion.ms(FlowMotion.hover, reduce)),
        label = "chip",
    )
    Row(
        modifier.height(30.dp).pressScale(ph.pressed).clip(RoundedCornerShape(999.dp)).background(bg)
            .then(if (selected) Modifier.border(1.dp, pal.accent.copy(alpha = .45f), RoundedCornerShape(999.dp)) else Modifier)
            .flowClickable(ph, true, onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        if (icon != null) FlowIconView(icon, if (selected) pal.accent else pal.textSecondary, size = 14.dp)
        FlowText(label, style = FlowType.captionMedium, color = if (selected) pal.accent else pal.textSecondary, maxLines = 1)
    }
}

@Composable
internal fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    equalWidth: Boolean = false,
) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    Row(
        modifier.glass(FlowDims.controlRadius + 2.dp, GlassLevel.Control).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val ph = rememberPressHover()
            val bg by animateColorAsState(
                when {
                    active -> if (pal.dark) pal.fillStrong else Color.White.copy(alpha = .95f)
                    ph.hovered -> pal.fillSubtle
                    else -> Color.Transparent
                },
                tween(FlowMotion.ms(FlowMotion.hover, reduce)),
                label = "seg",
            )
            Box(
                Modifier.then(if (equalWidth) Modifier.weight(1f) else Modifier)
                    .height(30.dp).clip(RoundedCornerShape(FlowDims.controlRadius - 1.dp)).background(bg)
                    .flowClickable(ph, true) { onSelect(value) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                FlowText(
                    label,
                    style = if (active) FlowType.callout.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else FlowType.callout,
                    color = if (active) pal.text else pal.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

// ───────────────────────── Switch / checkbox ─────────────────────────

@Composable
internal fun FlowSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val ph = rememberPressHover()
    val thumbX by animateDpAsState(if (checked) 18.dp else 2.dp, tween(FlowMotion.ms(FlowMotion.hover, reduce), easing = FlowMotion.emphasized), label = "thumb")
    val track by animateColorAsState(
        if (checked) pal.accentFillA else pal.fillStrong,
        tween(FlowMotion.ms(FlowMotion.hover, reduce)),
        label = "track",
    )
    // The switch is a physical control: thumb travels left→right regardless of layout direction.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier.size(width = 40.dp, height = 24.dp).clip(CircleShape).background(track)
                .flowClickable(ph, true) { onChange(!checked) },
        ) {
            Box(
                Modifier.offset(x = thumbX, y = 2.dp).size(20.dp).clip(CircleShape).background(Color.White)
                    .pressScale(ph.pressed, .92f),
            )
        }
    }
}

@Composable
internal fun FlowCheck(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val ph = rememberPressHover()
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(FlowMotion.ms(FlowMotion.hover + 40, reduce), easing = FlowMotion.emphasized), label = "check")
    Box(
        modifier.size(size).pressScale(ph.pressed, .9f).clip(CircleShape).flowClickable(ph, true) { onChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.6.dp.toPx()
            val ringColor = if (ph.hovered) pal.accent else pal.textTertiary
            drawCircle(ringColor.copy(alpha = 1f - fill), radius = this.size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
            if (fill > 0f) {
                drawCircle(
                    Brush.linearGradient(listOf(pal.accentFillA, pal.accentFillB)),
                    radius = this.size.minDimension / 2f * fill,
                    alpha = fill,
                )
            }
        }
        if (fill > .02f) FlowIconView(FlowIcon.Check, pal.onAccent.copy(alpha = fill), size = size * .62f, weight = 2.4f)
    }
}

// ───────────────────────── Slider ─────────────────────────

@Composable
internal fun FlowSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    showThumb: Boolean = true,
) {
    val pal = Flow.colors
    var widthPx by remember { mutableStateOf(1f) }
    val current by rememberUpdatedState(value)
    val cb by rememberUpdatedState(onChange)
    val fin by rememberUpdatedState(onFinished)
    val span = (range.endInclusive - range.start).coerceAtLeast(1e-6f)
    fun valueAt(x: Float): Float = range.start + (x / widthPx).coerceIn(0f, 1f) * span
    val fraction = ((current - range.start) / span).coerceIn(0f, 1f)
    // Sliders (like media transports) always run left → right.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier.height(22.dp).onSizeChanged { widthPx = max(1f, it.width.toFloat()) }
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .then(
                    if (!enabled) Modifier else Modifier
                        .pointerInput(Unit) { detectTapGestures { cb(valueAt(it.x)); fin?.invoke() } }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { cb(valueAt(it.x)) },
                                onDragEnd = { fin?.invoke() },
                                onDragCancel = { fin?.invoke() },
                            ) { change, _ -> change.consume(); cb(valueAt(change.position.x)) }
                        },
                ),
        ) {
            val trackH = 4.dp.toPx()
            val cy = size.height / 2f
            val thumbR = 7.dp.toPx()
            val x = thumbR + (size.width - 2 * thumbR) * fraction
            drawRoundRect(pal.fillStrong, Offset(0f, cy - trackH / 2), Size(size.width, trackH), androidx.compose.ui.geometry.CornerRadius(trackH))
            drawRoundRect(
                Brush.horizontalGradient(listOf(pal.accentFillA, pal.accentFillB), 0f, max(size.width, 1f)),
                Offset(0f, cy - trackH / 2), Size(x, trackH), androidx.compose.ui.geometry.CornerRadius(trackH),
            )
            if (showThumb) {
                drawCircle(Color.Black.copy(alpha = .22f), thumbR + 1.dp.toPx(), Offset(x, cy + 1.dp.toPx()))
                drawCircle(Color.White, thumbR, Offset(x, cy))
            }
        }
    }
}

// ───────────────────────── Text field ─────────────────────────

@Composable
internal fun FlowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: FlowIcon? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onEnter: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    trailing: (@Composable () -> Unit)? = null,
    ltrContent: Boolean = false,
) {
    val pal = Flow.colors
    var focused by remember { mutableStateOf(false) }
    val ring by animateColorAsState(
        if (focused) pal.accent.copy(alpha = .85f) else Color.Transparent,
        tween(FlowMotion.ms(FlowMotion.hover, LocalReduceMotion.current)),
        label = "field-ring",
    )
    val shape = RoundedCornerShape(FlowDims.controlRadius)
    Row(
        modifier.glass(FlowDims.controlRadius, GlassLevel.Control).border(1.5.dp, ring, shape)
            .heightIn(min = 38.dp).padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 8.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) FlowIconView(leading, pal.textTertiary, Modifier.padding(top = if (singleLine) 0.dp else 3.dp), size = 17.dp)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) FlowText(placeholder, style = FlowType.body, color = pal.textTertiary, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                textStyle = FlowType.body.copy(
                    color = pal.text,
                    textDirection = if (ltrContent) TextDirection.Ltr else TextDirection.ContentOrRtl,
                ),
                cursorBrush = SolidColor(pal.accent),
                modifier = Modifier.fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { e ->
                        if (onEnter != null && e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                            onEnter(); true
                        } else false
                    },
            )
        }
        if (trailing != null) trailing()
    }
}

// ───────────────────────── Dialog (in-window overlay) ─────────────────────────

/**
 * Modal sheet rendered inside the window (not a native dialog) so it shares the same backdrop
 * and glass material as every other surface. Escape / scrim click dismiss.
 */
@Composable
internal fun FlowDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 480.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(FlowMotion.ms(FlowMotion.dialog, reduce), easing = FlowMotion.emphasized), label = "dialog")
    Box(
        Modifier.fillMaxSize().background(pal.scrim.copy(alpha = pal.scrim.alpha * a))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier.widthIn(max = width).fillMaxWidth(.94f)
                .graphicsLayer { alpha = a; scaleX = .97f + .03f * a; scaleY = .97f + .03f * a }
                .glass(FlowDims.panelRadius, GlassLevel.Chrome)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

// ───────────────────────── Menus (popups) ─────────────────────────

internal data class MenuEntry(val label: String, val icon: FlowIcon? = null, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
private fun MenuSurface(width: Dp, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalBackdrop provides null) {
        Column(
            Modifier.width(width).glass(14.dp, GlassLevel.Floating).padding(6.dp)
                .heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

@Composable
private fun MenuRow(label: String, icon: FlowIcon?, selected: Boolean, danger: Boolean, onClick: () -> Unit) {
    val pal = Flow.colors
    val ph = rememberPressHover()
    val color = if (danger) pal.danger else pal.text
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
            .background(if (ph.hovered) pal.fillHover else Color.Transparent)
            .flowClickable(ph, true, onClick).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (icon != null) FlowIconView(icon, if (danger) pal.danger else pal.textSecondary, size = 16.dp)
        FlowText(label, Modifier.weight(1f), FlowType.callout, color, maxLines = 1)
        if (selected) FlowIconView(FlowIcon.Check, pal.accent, size = 15.dp)
    }
}

private class BelowAnchor(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        // Align the menu's end edge with the trigger's end edge (right edge in RTL), keep inside the window.
        val rawX = if (layoutDirection == LayoutDirection.Rtl) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val x = rawX.coerceIn(8, max(8, windowSize.width - popupContentSize.width - 8))
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height - 8) below
        else max(8, anchorBounds.top - gapPx - popupContentSize.height)
        return IntOffset(x, y)
    }
}

private class AtPoint(private val point: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (anchorBounds.left + point.x).coerceIn(8, max(8, windowSize.width - popupContentSize.width - 8))
        val y = (anchorBounds.top + point.y).coerceIn(8, max(8, windowSize.height - popupContentSize.height - 8))
        return IntOffset(x, y)
    }
}

@Composable
internal fun <T> FlowDropdown(
    selectedLabel: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leading: FlowIcon? = null,
    menuWidth: Dp = 230.dp,
) {
    val pal = Flow.colors
    var open by remember { mutableStateOf(false) }
    val ph = rememberPressHover()
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.roundToPx() }
    Box(modifier) {
        Row(
            Modifier.height(36.dp).glass(FlowDims.controlRadius, GlassLevel.Control)
                .clip(RoundedCornerShape(FlowDims.controlRadius))
                .background(if (ph.hovered) pal.fillSubtle else Color.Transparent)
                .flowClickable(ph, true) { open = !open }.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (leading != null) FlowIconView(leading, pal.textSecondary, size = 16.dp)
            FlowText(selectedLabel, Modifier.weight(1f, fill = false), FlowType.callout, maxLines = 1)
            FlowIconView(FlowIcon.ChevronDown, pal.textTertiary, size = 14.dp)
        }
        if (open) {
            Popup(popupPositionProvider = BelowAnchor(gap), onDismissRequest = { open = false }, properties = PopupProperties(focusable = true)) {
                MenuSurface(menuWidth) {
                    options.forEach { (value, label) ->
                        MenuRow(label, null, value == selected, false) { onSelect(value); open = false }
                    }
                }
            }
        }
    }
}

/** Our own right-click menu (the platform one cannot take the glass material). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ContextMenuBox(entries: List<MenuEntry>, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var point by remember { mutableStateOf<IntOffset?>(null) }
    Box(
        modifier.onPointerEvent(PointerEventType.Press) { ev ->
            if (ev.buttons.isSecondaryPressed) {
                val p = ev.changes.first().position
                point = IntOffset(p.x.toInt(), p.y.toInt())
            }
        },
    ) {
        content()
        val at = point
        if (at != null) {
            Popup(popupPositionProvider = AtPoint(at), onDismissRequest = { point = null }, properties = PopupProperties(focusable = true)) {
                MenuSurface(200.dp) {
                    entries.forEach { e -> MenuRow(e.label, e.icon, false, e.danger) { point = null; e.onClick() } }
                }
            }
        }
    }
}

// ───────────────────────── Progress ─────────────────────────

@Composable
internal fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val pal = Flow.colors
    val reduce = LocalReduceMotion.current
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(FlowMotion.ms(FlowMotion.progress, reduce), easing = FlowMotion.emphasized), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f
            val d = this.size.minDimension - sw
            drawArc(pal.fillStrong, 0f, 360f, false, Offset(inset, inset), Size(d, d), style = Stroke(sw))
            if (p > 0.001f) {
                drawArc(
                    Brush.sweepGradient(listOf(pal.accentFillA, pal.accentFillB, pal.accentFillA)),
                    -90f, 360f * p, false, Offset(inset, inset), Size(d, d), style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Small "cover art" swatch generated from a seed (the catalogue has no artwork). */
@Composable
internal fun CoverArt(seed: String, modifier: Modifier = Modifier, size: Dp = 48.dp, radius: Dp = 12.dp, playing: Boolean = false) {
    val h = seed.hashCode()
    fun hue(n: Int) = ((n % 360) + 360) % 360
    val c1 = Color.hsv(hue(h).toFloat(), .55f, .95f)
    val c2 = Color.hsv(hue(h / 7 + 140).toFloat(), .6f, .72f)
    Box(
        modifier.size(size).clip(RoundedCornerShape(radius)).background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.Transparent))))
        FlowIconView(if (playing) FlowIcon.Volume else FlowIcon.Music, Color.White.copy(alpha = .92f), size = size * .42f, weight = 1.8f)
    }
}
