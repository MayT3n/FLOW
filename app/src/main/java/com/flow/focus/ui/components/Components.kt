package com.flow.focus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flow.focus.core.withDigits
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.LocalRootSize
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
    Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = modifier.size(size))
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
    GlassSurface(
        modifier = modifier.size(size).alpha(if (enabled) 1f else 0.4f).springClick(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        tint = if (active) Flow.colors.accent else null,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FlowIcon(icon, tint = if (active) Flow.colors.accent else tint, size = iconSize, contentDescription = contentDescription)
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
            Text(text, style = Flow.type.bodyStrong, color = c.textOnAccent, maxLines = 1)
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
            Text(text, style = Flow.type.bodyStrong, color = color, maxLines = 1)
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
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
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
            Text(title, style = Flow.type.body, color = Flow.colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = Flow.type.caption, color = Flow.colors.textSecondary)
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
        GlassIconButton(FlowIcons.Minus, { onChange((value - step).coerceIn(range)) }, size = 36.dp, iconSize = 18.dp, enabled = value > range.first)
        Text(
            num(value) + if (suffix.isNotEmpty()) " $suffix" else "",
            style = Flow.type.bodyStrong, color = Flow.colors.textPrimary,
            modifier = Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center, maxLines = 1,
        )
        GlassIconButton(FlowIcons.Plus, { onChange((value + step).coerceIn(range)) }, size = 36.dp, iconSize = 18.dp, enabled = value < range.last)
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
                        if (value.isEmpty()) Text(placeholder, style = Flow.type.body, color = c.textSecondary.copy(alpha = 0.75f))
                        inner()
                    }
                }
            }
        },
    )
}

@Composable
fun CheckCircle(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, size: Dp = 28.dp, color: Color = Flow.colors.accent) {
    val c = Flow.colors
    val fill by animateFloatAsState(if (checked) 1f else 0f, floatSpring(0.55f, 500f), label = "check")
    Box(
        modifier
            .size(size)
            .springClick(role = Role.Checkbox, onClick = onToggle)
            .drawBehind {
                drawCircle(c.textSecondary.copy(alpha = 0.55f * (1f - fill)), radius = this.size.minDimension / 2f - 1.5f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawCircle(color, radius = (this.size.minDimension / 2f) * fill)
            },
        contentAlignment = Alignment.Center,
    ) {
        FlowIcon(FlowIcons.Check, tint = Color.White, size = size * 0.62f, modifier = Modifier.alpha(fill).scale(0.6f + 0.4f * fill))
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
        Text(text, style = Flow.type.heading, color = Flow.colors.textPrimary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowIcon(icon, tint = Flow.colors.textSecondary.copy(alpha = 0.8f), size = 40.dp)
        Text(title, style = Flow.type.heading, color = Flow.colors.textPrimary, textAlign = TextAlign.Center)
        Text(subtitle, style = Flow.type.caption, color = Flow.colors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dot: Color? = null) {
    val c = Flow.colors
    val bg by animateColorAsState(if (selected) c.accent else Color.Transparent, anim(), label = "chipBg")
    val fg by animateColorAsState(if (selected) c.textOnAccent else c.textPrimary, anim(), label = "chipFg")
    GlassSurface(modifier.springClick(onClick = onClick), shape = RoundedCornerShape(18.dp), level = GlassLevel.Subtle) {
        Row(
            Modifier.background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text(text, style = Flow.type.label, color = fg, maxLines = 1)
        }
    }
}

/** دیالوگ شیشه‌ای. پس‌زمینه‌ی Blur در پنجره‌ی جدا ممکن نیست، پس پنل کمی مات‌تر است. */
@Composable
fun FlowDialog(onDismiss: () -> Unit, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalRootSize provides null) {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp).widthIn(max = 460.dp),
                shape = RoundedCornerShape(30.dp), level = GlassLevel.Strong, elevated = true,
            ) {
                Column(
                    Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (title != null) Text(title, style = Flow.type.title, color = Flow.colors.textPrimary)
                    content()
                }
            }
        }
    }
}

data class DockItem(val label: String, val icon: ImageVector)

/** Dock شناور Liquid Glass با انیمیشن فنری برای بخش فعال. */
@Composable
fun FlowDock(items: List<DockItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Flow.colors
    GlassSurface(
        modifier.fillMaxWidth().widthIn(max = 440.dp).height(66.dp),
        shape = RoundedCornerShape(33.dp), level = GlassLevel.Strong, elevated = true,
    ) {
        Row(Modifier.fillMaxSize().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { i, item ->
                val sel = i == selected
                val p by animateFloatAsState(if (sel) 1f else 0f, floatSpring(0.6f, 450f), label = "dock")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .springClick(role = Role.Tab, scaleDown = 0.9f, onClick = { onSelect(i) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .graphicsLayer { alpha = p; scaleX = 0.7f + 0.3f * p; scaleY = 0.7f + 0.3f * p }
                            .clip(RoundedCornerShape(28.dp)).background(c.accentSoft),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FlowIcon(
                            item.icon, tint = if (sel) c.accent else c.textSecondary, size = 22.dp,
                            modifier = Modifier.graphicsLayer { translationY = -3f * p },
                        )
                        Text(item.label, style = Flow.type.caption, color = if (sel) c.accent else c.textSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Flow.type.title, color = Flow.colors.textPrimary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

val ScreenPadding = PaddingValues(horizontal = 18.dp)
