package com.flow.focus.ui.glass

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.flow.focus.data.GlassQuality
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.FlowColors
import com.flow.focus.ui.theme.LocalGlass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * سیستم مرکزی Liquid Glass. همه‌ی پنل‌ها، Dock، دیالوگ‌ها و دکمه‌ها از [GlassSurface] استفاده می‌کنند، پس
 * شفافیت، Blur، نور و کیفیت از یک نقطه (تنظیمات) کنترل می‌شود.
 *
 *  - LOW:    سطح تخت و کم‌شفاف (کم‌مصرف‌ترین)
 *  - MEDIUM: شیشه‌ی نیمه‌شفاف با درخشش و لبه‌ی نورانی
 *  - HIGH:   MEDIUM + تاری واقعی پس‌زمینه (RenderEffect، اندروید ۱۲+)
 *
 * پس‌زمینه یک بار در ریشه رسم می‌شود و پنل‌های HIGH همان را (با جابه‌جایی هم‌ترازشده) محو می‌کنند؛
 * بنابراین هنگام اسکرول و تغییر صفحه جدا شدن یا پرش دیده نمی‌شود.
 */
enum class GlassLevel { Subtle, Regular, Strong }

val LocalRootSize = compositionLocalOf<State<IntSize>?> { null }

fun DrawScope.drawFlowBackground(c: FlowColors, phase: Float, w: Float, h: Float) {
    drawRect(Brush.verticalGradient(listOf(c.bgTop, c.bgMid, c.bgBottom), startY = 0f, endY = h), size = androidx.compose.ui.geometry.Size(w, h))
    val a = if (c.isDark) 0.55f else 0.75f
    fun blob(color: Color, cx: Float, cy: Float, r: Float) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = a), color.copy(alpha = 0f)), center = Offset(cx, cy), radius = r),
            radius = r, center = Offset(cx, cy),
        )
    }
    val t = (phase * 2 * PI).toFloat()
    blob(c.blobA, w * (0.12f + 0.06f * sin(t)), h * (0.14f + 0.03f * cos(t)), w * 0.95f)
    blob(c.blobB, w * (0.95f + 0.05f * cos(t)), h * (0.48f + 0.04f * sin(t)), w * 0.9f)
    blob(c.blobC, w * (0.25f + 0.06f * cos(t + 1f)), h * (0.92f + 0.03f * sin(t)), w * 0.85f)
}

/** پس‌زمینه‌ی مشترک برنامه؛ فقط یک بار در ریشه ساخته می‌شود. */
@Composable
fun GlassBackdropHost(content: @Composable BoxScope.() -> Unit) {
    val c = Flow.colors
    val g = LocalGlass.current
    val size = remember { mutableStateOf(IntSize.Zero) }
    // فاز انیمیشن فقط در مرحله‌ی رسم خوانده می‌شود، پس هر فریم Recomposition ندارد.
    val phase: State<Float> = if (g.animations && g.quality != GlassQuality.LOW) {
        rememberInfiniteTransition(label = "bg")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "bgPhase")
    } else remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxSize().onSizeChanged { size.value = it }) {
        Canvas(Modifier.fillMaxSize()) { drawFlowBackground(c, phase.value, this.size.width, this.size.height) }
        CompositionLocalProvider(LocalRootSize provides size) { content() }
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    level: GlassLevel = GlassLevel.Regular,
    tint: Color? = null,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = Flow.colors
    val g = LocalGlass.current
    val rootSize = LocalRootSize.current
    val blurOn = g.quality == GlassQuality.HIGH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        rootSize != null && g.blurDp > 0 && level != GlassLevel.Subtle
    var origin by remember { mutableStateOf(Offset.Zero) }

    val baseFill = when {
        g.quality == GlassQuality.LOW -> c.glassFillStrong
        level == GlassLevel.Strong -> c.glassFillStrong
        level == GlassLevel.Subtle -> c.glassFill.copy(alpha = c.glassFill.alpha * 0.6f)
        else -> c.glassFill
    }
    val fill = baseFill.copy(alpha = (baseFill.alpha * g.opacity).coerceIn(0f, 1f))
    val shine = if (g.quality == GlassQuality.LOW) 0.15f else g.shine

    val outer = modifier
        .then(if (elevated && g.quality != GlassQuality.LOW) Modifier.shadow(18.dp, shape, ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
        .clip(shape)
        .then(if (blurOn) Modifier.onGloballyPositioned { origin = it.positionInRoot() } else Modifier)

    Box(outer) {
        if (blurOn) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val r = g.blurDp.dp.toPx()
                        renderEffect = BlurEffect(r, r)
                    }
                    .drawBehind {
                        val s = rootSize!!.value
                        translate(-origin.x, -origin.y) {
                            drawFlowBackground(c, 0f, s.width.toFloat(), s.height.toFloat())
                        }
                    },
            )
        }
        Box(
            Modifier.matchParentSize().drawBehind {
                drawRect(fill)
                if (tint != null) drawRect(tint.copy(alpha = 0.16f))
                if (g.quality != GlassQuality.LOW) {
                    drawRect(
                        Brush.verticalGradient(
                            0f to c.glassTintTop.copy(alpha = c.glassTintTop.alpha * shine),
                            0.55f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    )
                }
            },
        )
        Box(
            Modifier.matchParentSize().border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        c.glassEdge.copy(alpha = 0.60f * shine),
                        c.glassEdge.copy(alpha = 0.06f),
                        c.glassEdge.copy(alpha = 0.02f),
                        c.glassEdge.copy(alpha = 0.30f * shine),
                    ),
                    start = Offset.Zero, end = Offset.Infinite,
                ),
                shape,
            ),
        )
        content()
    }
}

/** پاسخ فنری ظریف به لمس. */
fun Modifier.springClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.95f,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val anim = LocalGlass.current.animations
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = if (anim) spring(dampingRatio = 0.5f, stiffness = 600f) else snap(),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/** مشخصات انیمیشن فنری یا بدون انیمیشن بر اساس تنظیمات. */
@Composable
fun floatSpring(damping: Float = 0.7f, stiffness: Float = 380f): androidx.compose.animation.core.AnimationSpec<Float> =
    if (LocalGlass.current.animations) spring(damping, stiffness) else snap()

@Composable
fun dpSpring(): androidx.compose.animation.core.AnimationSpec<Dp> =
    if (LocalGlass.current.animations) spring(0.75f, 420f) else snap()
