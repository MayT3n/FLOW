package com.flow.focus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flow.focus.core.withDigits
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.LocalBackdropLayer
import com.flow.focus.ui.motion.Motion
import com.flow.focus.ui.motion.rememberEnterState
import androidx.compose.runtime.CompositionLocalProvider
import com.flow.focus.ui.glass.dpSpring
import com.flow.focus.ui.glass.floatSpring
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalGlass
import com.flow.focus.ui.theme.LocalPersianDigits

/** اعداد با توجه به تنظیم «ارقام فارسی/انگلیسی» نمایش داده می‌شوند. */
@Composable
fun num(x: Any): String = x.toString().withDigits(LocalPersianDigits.current)

@Composable
fun FlowIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Flow.colors.textPrimary,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val animations = LocalGlass.current.animations
    val iconTint by animateColorAsState(
        targetValue = tint,
        animationSpec = if (animations) tween(180) else snap(),
        label = "flow-icon-tint",
    )
    if (animations) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(tween(130)) + scaleIn(initialScale = 0.86f, animationSpec = tween(150))) togetherWith
                    (fadeOut(tween(95)) + scaleOut(targetScale = 0.94f, animationSpec = tween(110)))
            },
            label = "flow-icon-swap",
        ) { displayedIcon ->
            Icon(
                imageVector = displayedIcon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = modifier.size(size),
            )
        }
    } else {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = iconTint, modifier = modifier.size(size))
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Flow.colors.textPrimary,
    contentDescription: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .then(modifier)
            .alpha(if (enabled) 1f else 0.4f)
            .springClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Keep the visual diameter compact while exposing an accessible touch target.
        GlassSurface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            tint = if (active) Flow.colors.accent else null,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                FlowIcon(icon, tint = if (active) Flow.colors.accent else tint, size = iconSize, contentDescription = contentDescription)
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = Flow.colors
    Box(
        modifier
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .springClick(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.horizontalGradient(listOf(c.accent, c.accentAlt)))
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(0f to Color.White.copy(alpha = 0.28f), 0.5f to Color.Transparent),
                )
            }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) FlowIcon(icon, tint = c.textOnAccent, size = 20.dp)
            Text(text, style = Flow.type.bodyStrong, color = c.textOnAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Flow.colors.textPrimary,
    danger: Boolean = false,
) {
    val c = Flow.colors
    val color = if (danger) c.danger else tint
    GlassSurface(
        modifier = modifier.height(48.dp).springClick(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        tint = if (danger) c.danger else null,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (icon != null) FlowIcon(icon, tint = color, size = 20.dp)
            Text(text, style = Flow.type.bodyStrong, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun <T> anim(): androidx.compose.animation.core.AnimationSpec<T> =
    if (LocalGlass.current.animations) tween(220) else snap()

/** کنترل چندگزینه‌ای (مثل «تمرکز / استراحت کوتاه / استراحت بلند»). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Flow.colors
    GlassSurface(modifier.alpha(if (enabled) 1f else 0.55f), shape = RoundedCornerShape(22.dp), level = GlassLevel.Subtle) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { i, label ->
                val sel = i == selected
                val bg by animateColorAsState(if (sel) c.accent else Color.Transparent, anim(), label = "segBg")
                val fg by animateColorAsState(if (sel) c.textOnAccent else c.textSecondary, anim(), label = "segFg")
                val emphasis by animateFloatAsState(
                    targetValue = if (sel) 1f else 0f,
                    animationSpec = floatSpring(0.82f, 520f),
                    label = "segment-emphasis",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .graphicsLayer {
                            scaleX = 0.975f + emphasis * 0.025f
                            scaleY = 0.95f + emphasis * 0.05f
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .semantics { this.selected = sel }
                        .clickable(enabled = enabled, role = Role.Tab, onClick = { onSelect(i) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = Flow.type.label, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun FlowSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Flow.colors
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = c.accent, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = c.textSecondary, uncheckedTrackColor = c.track, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Flow.type.body, color = Flow.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) =
    SettingRow(title, subtitle = subtitle) { FlowSwitch(checked, onChange) }

@Composable
fun FlowSlider(value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier, steps: Int = 0) {
    val c = Flow.colors
    Slider(
        value = value, onValueChange = onChange, valueRange = range, steps = steps, modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.track,
            activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
        ),
    )
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, range: IntRange, modifier: Modifier = Modifier, step: Int = 1, suffix: String = "") {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassIconButton(FlowIcons.Minus, { onChange((value - step).coerceIn(range)) }, size = 36.dp, iconSize = 18.dp, enabled = value > range.first, contentDescription = "کاهش مقدار")
        val displayValue = num(value) + if (suffix.isNotEmpty()) " $suffix" else ""
        if (LocalGlass.current.animations) {
            AnimatedContent(
                targetState = displayValue,
                transitionSpec = {
                    (fadeIn(tween(120)) + slideInVertically(tween(145)) { it / 4 }) togetherWith
                        (fadeOut(tween(90)) + slideOutVertically(tween(115)) { -it / 4 })
                },
                label = "stepper-value",
            ) { shownValue ->
                Text(
                    shownValue,
                    style = Flow.type.bodyStrong, color = Flow.colors.textPrimary,
                    modifier = Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        } else {
            Text(
                displayValue,
                style = Flow.type.bodyStrong, color = Flow.colors.textPrimary,
                modifier = Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center, maxLines = 1,
            )
        }
        GlassIconButton(FlowIcons.Plus, { onChange((value + step).coerceIn(range)) }, size = 36.dp, iconSize = 18.dp, enabled = value < range.last, contentDescription = "افزایش مقدار")
    }
}

@Composable
fun FlowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leading: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = Flow.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        textStyle = Flow.type.body.copy(color = c.textPrimary),
        cursorBrush = SolidColor(c.accent),
        modifier = modifier,
        decorationBox = { inner ->
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), level = GlassLevel.Subtle) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (leading != null) FlowIcon(leading, tint = c.textSecondary, size = 20.dp)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(
                            placeholder,
                            style = Flow.type.body,
                            color = c.textSecondary.copy(alpha = 0.75f),
                            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis,
                        )
                        inner()
                    }
                }
            }
        },
    )
}

@Composable
fun CheckCircle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = Flow.colors.accent,
    contentDescription: String = "وضعیت تکمیل",
) {
    val c = Flow.colors
    val fill by animateFloatAsState(if (checked) 1f else 0f, floatSpring(0.55f, 500f), label = "check")
    Box(
        modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .springClick(role = Role.Checkbox, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size).drawBehind {
                drawCircle(c.textSecondary.copy(alpha = 0.55f * (1f - fill)), radius = this.size.minDimension / 2f - 1.5f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawCircle(color, radius = (this.size.minDimension / 2f) * fill)
            },
            contentAlignment = Alignment.Center,
        ) {
            FlowIcon(FlowIcons.Check, tint = Color.White, size = size * 0.62f, modifier = Modifier.alpha(fill).scale(0.6f + 0.4f * fill))
        }
    }
}

@Composable
fun FlowProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Flow.colors.accent, height: Dp = 6.dp) {
    val c = Flow.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), floatSpring(1f, 200f), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(c.track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(color))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Flow.type.heading, color = Flow.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val animations = LocalGlass.current.animations
    AnimatedVisibility(
        visibleState = rememberEnterState(true),
        enter = if (animations) {
            fadeIn(Motion.enter(Motion.Medium)) + scaleIn(initialScale = 0.96f, animationSpec = Motion.enter(Motion.Medium)) +
                slideInVertically(Motion.enter(Motion.Medium)) { it / 14 }
        } else EnterTransition.None,
    ) {
        Column(
            modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowIcon(icon, tint = Flow.colors.textSecondary.copy(alpha = 0.8f), size = 40.dp)
            Text(title, style = Flow.type.heading, color = Flow.colors.textPrimary, textAlign = TextAlign.Center)
            Text(subtitle, style = Flow.type.caption, color = Flow.colors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dot: Color? = null) {
    val c = Flow.colors
    val bg by animateColorAsState(if (selected) c.accent else Color.Transparent, anim(), label = "chipBg")
    val fg by animateColorAsState(if (selected) c.textOnAccent else c.textPrimary, anim(), label = "chipFg")
    GlassSurface(modifier.semantics { this.selected = selected }.springClick(onClick = onClick), shape = RoundedCornerShape(18.dp), level = GlassLevel.Subtle) {
        Row(
            Modifier.background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text(text, style = Flow.type.label, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** پوسته‌ی واحد پنجره‌ها؛ عنوان، بستن، فاصله‌گذاری و سطح شیشه‌ای در تمام دیالوگ‌ها یکسان است. */
@Composable
fun FlowDialog(onDismiss: () -> Unit, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val animations = LocalGlass.current.animations
    var visible by remember { mutableStateOf(true) }
    val enterState = rememberEnterState(visible)
    val requestDismiss = {
        if (visible) visible = false
    }
    LaunchedEffect(visible) {
        if (!visible) {
            if (animations) delay(175)
            onDismiss()
        }
    }
    Dialog(onDismissRequest = requestDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalBackdropLayer provides null) {
        AnimatedVisibility(
            visibleState = enterState,
            enter = if (animations) fadeIn(Motion.enter(Motion.Short)) + scaleIn(initialScale = 0.94f, animationSpec = Motion.soft()) else EnterTransition.None,
            exit = if (animations) fadeOut(Motion.exit(Motion.Fast)) + scaleOut(targetScale = 0.965f, animationSpec = Motion.exit(Motion.Short)) else ExitTransition.None,
        ) {
            GlassSurface(
                Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp),
                shape = RoundedCornerShape(28.dp),
                level = GlassLevel.Strong,
                elevated = true,
            ) {
                Column(
                    Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    if (title != null) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                title,
                                Modifier.weight(1f),
                                style = Flow.type.title,
                                color = Flow.colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            GlassIconButton(
                                FlowIcons.Close,
                                requestDismiss,
                                size = 34.dp,
                                iconSize = 16.dp,
                                contentDescription = "بستن پنجره",
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Flow.colors.track))
                    }
                    content()
                }
            }
        }
        }
    }
}

data class DockItem(val label: String, val icon: ImageVector)

/** نوار شناور پایین با زبان بصری iOS، سطح شیشه‌ای مشترک و وضعیت انتخاب دسترس‌پذیر. */
@Composable
fun FlowDock(items: List<DockItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Flow.colors
    GlassSurface(
        Modifier.widthIn(max = 440.dp).then(modifier).fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(34.dp),
        level = GlassLevel.Strong,
        elevated = true,
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            val count = items.size.coerceAtLeast(1)
            val slotWidth = maxWidth / count
            val activeIndex = if (items.isEmpty()) 0 else selected.coerceIn(items.indices)
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * activeIndex,
                animationSpec = dpSpring(),
                label = "dock-glass-indicator",
            )

            if (items.isNotEmpty()) {
                // One continuous glass pill travels between tabs instead of painting a flat tint per item.
                GlassSurface(
                    Modifier.offset(x = indicatorOffset)
                        .width(slotWidth)
                        .height(60.dp)
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    shape = RoundedCornerShape(25.dp),
                    level = GlassLevel.Regular,
                    tint = c.accent,
                ) {
                    Box(
                        Modifier.fillMaxSize().drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = if (c.isDark) 0.13f else 0.24f),
                                    0.48f to Color.Transparent,
                                    1f to c.accent.copy(alpha = if (c.isDark) 0.035f else 0.06f),
                                ),
                            )
                        },
                    )
                }
            }

            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == activeIndex
                    val emphasis by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = floatSpring(0.78f, 560f),
                        label = "dock-icon-emphasis",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = item.label
                                this.selected = isSelected
                            }
                            .springClick(
                                role = Role.Tab,
                                scaleDown = 0.94f,
                                onClick = { onSelect(index) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            FlowIcon(
                                item.icon,
                                tint = if (isSelected) c.accent else c.textSecondary,
                                size = 21.dp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = 1f + 0.045f * emphasis
                                    scaleY = 1f + 0.045f * emphasis
                                },
                            )
                            Text(
                                item.label,
                                style = Flow.type.caption.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
                                color = if (isSelected) c.accent else c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Flow.type.title, color = Flow.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
    }
}

val ScreenPadding = PaddingValues(horizontal = 18.dp)
