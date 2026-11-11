package com.flow.focus.ui.glass

import android.annotation.SuppressLint
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerBasedShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.flow.focus.data.GlassMaterial
import com.flow.focus.data.GlassQuality
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.FlowColors
import com.flow.focus.ui.theme.LocalGlass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * سیستم مرکزی Liquid Glass. همه‌ی پنل‌ها، Dock، دیالوگ‌ها و دکمه‌ها از [GlassSurface] استفاده می‌کنند؛
 * بک‌دراپ همان والپیپر واقعی است ([drawWallpaper]) و با مختصات ریشه هم‌تراز می‌شود.
 *
 *  - LOW:    سطح تخت و کم‌شفاف (بدون نمونه‌برداری از بک‌دراپ)
 *  - MEDIUM: بک‌دراپ + تاری انتخابی (اندروید ۱۲+) + لبه‌ی Fresnel، هایلایت، سایه‌ی درونی، رنگ‌آمیزی وابسته به روشنایی والپیپر
 *  - HIGH:   اندروید ۱۳+: شیدر AGSL (شکست نور، انحراف رنگی، بازتاب، عمق)؛ اندروید ۱۲: تاری + جلوه‌های نوری MEDIUM؛ پایین‌تر: MEDIUM
 */
enum class GlassLevel { Subtle, Regular, Strong }

val LocalRootSize = compositionLocalOf<State<IntSize>?> { null }

/** مبدأ میزبان بک‌دراپ در مختصات ریشه؛ فقط برای میزبان‌های محلی مثل پیش‌نمایش (پیش‌فرض: ریشه، یعنی صفر). */
val LocalBackdropOrigin = compositionLocalOf<State<Offset>?> { null }
val LocalWallpaper = compositionLocalOf<WallpaperState?> { null }

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

/** پس‌زمینه‌ی مشترک برنامه؛ فقط یک بار در ریشه ساخته می‌شود و همه‌ی شیشه‌ها از همین والپیپر نمونه می‌گیرند. */
@Composable
fun GlassBackdropHost(wallpaper: WallpaperState, content: @Composable BoxScope.() -> Unit) {
    val c = Flow.colors
    val g = LocalGlass.current
    val size = remember { mutableStateOf(IntSize.Zero) }
    // حلقه‌ی انیمیشن فقط برای گرادیان پیش‌فرض و فقط در کیفیت‌های MEDIUM/HIGH؛ فاز فقط در مرحله‌ی رسم خوانده می‌شود.
    val phase: State<Float> = if (g.animations && g.quality != GlassQuality.LOW && wallpaper.usesGradient) {
        rememberInfiniteTransition(label = "bg")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "bgPhase")
    } else remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxSize().onSizeChanged { size.value = it }) {
        Canvas(Modifier.fillMaxSize()) { drawWallpaper(wallpaper, c, phase.value, this.size.width, this.size.height) }
        CompositionLocalProvider(LocalRootSize provides size, LocalWallpaper provides wallpaper) { content() }
    }
}

/** میزبان محلی بک‌دراپ (پیش‌نمایش تنظیمات): همان موتور رسم و همان GlassSurface، فقط در قاب کوچک. */
@Composable
fun GlassPreviewHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val ws = LocalWallpaper.current
    val c = Flow.colors
    val size = remember { mutableStateOf(IntSize.Zero) }
    val origin = remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier.clip(RoundedCornerShape(28.dp))
            .onSizeChanged { size.value = it }
            .onGloballyPositioned { origin.value = it.positionInRoot() },
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (ws != null) drawWallpaper(ws, c, 0f, this.size.width, this.size.height)
        }
        CompositionLocalProvider(LocalRootSize provides size, LocalBackdropOrigin provides origin) { content() }
    }
}

private class MatSpec(
    val blur: Float, val refr: Float, val aber: Float, val rim: Float, val spec: Float,
    val depth: Float, val refl: Float, val frost: Float, val fill: Float, val thick: Float,
)

private fun materialSpec(m: GlassMaterial) = when (m) {
    GlassMaterial.CRYSTAL -> MatSpec(blur = 0.35f, refr = 0.55f, aber = 0.04f, rim = 1.0f, spec = 1.0f, depth = 0.5f, refl = 1.0f, frost = 0.05f, fill = 0.75f, thick = 1.0f)
    GlassMaterial.FROSTED -> MatSpec(blur = 1.0f, refr = 0.18f, aber = 0.0f, rim = 0.6f, spec = 0.5f, depth = 0.4f, refl = 0.5f, frost = 0.55f, fill = 1.15f, thick = 0.8f)
    GlassMaterial.LIQUID -> MatSpec(blur = 0.6f, refr = 1.0f, aber = 0.07f, rim = 1.1f, spec = 0.9f, depth = 1.0f, refl = 0.9f, frost = 0.12f, fill = 0.9f, thick = 1.6f)
}

private data class EffectKey(val blur: Int, val shader: GlassShaders.Key?)

private class EffectCache {
    private var key: EffectKey? = null
    private var effect: RenderEffect? = null
    fun get(k: EffectKey, build: () -> RenderEffect?): RenderEffect? {
        if (k != key) {
            effect = build()
            key = k
        }
        return effect
    }
}

/** لایه‌های نوری سبک (بدون شیدر): Fresnel، درخشش جهت‌دار، بازتاب نرم لبه‌ی پایین و سایه‌ی درونی. */
private fun DrawScope.drawGlassOptics(shape: Shape, c: FlowColors, m: MatSpec, shine: Float, reflect: Float, full: Boolean) {
    val outline = shape.createOutline(size, layoutDirection, this)
    val px = 1.dp.toPx()
    val edge = c.glassEdge
    val e = shine * m.rim
    if (full) {
        drawRect(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = (0.16f * shine * m.spec).coerceIn(0f, 1f)), Color.Transparent),
                center = Offset(size.width * 0.15f, 0f), radius = max(size.width, size.height) * 0.9f,
            ),
        )
        drawOutline(
            outline,
            Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.White.copy(alpha = (0.10f * reflect * m.refl).coerceIn(0f, 1f))),
            style = Stroke(px * 14f),
        )
        val sh = if (c.isDark) Color.Black else c.shadow
        drawOutline(
            outline,
            Brush.linearGradient(
                0f to Color.Transparent, 0.5f to Color.Transparent, 1f to sh.copy(alpha = (0.20f * m.depth).coerceIn(0f, 1f)),
                start = Offset.Zero, end = Offset(size.width, size.height),
            ),
            style = Stroke(px * 8f),
        )
    }
    drawOutline(
        outline,
        Brush.linearGradient(
            0f to edge.copy(alpha = (0.70f * e).coerceIn(0f, 1f)),
            0.35f to edge.copy(alpha = 0.10f),
            0.65f to edge.copy(alpha = 0.05f),
            1f to edge.copy(alpha = (0.34f * e * reflect).coerceIn(0f, 1f)),
            start = Offset.Zero, end = Offset(size.width, size.height),
        ),
        style = Stroke(px * 1.4f),
    )
}

@SuppressLint("NewApi")
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
    val ws = LocalWallpaper.current
    val hostOrigin = LocalBackdropOrigin.current
    val sdk = Build.VERSION.SDK_INT
    val m = remember(g.material) { materialSpec(g.material) }

    val sampling = rootSize != null && ws != null && g.quality != GlassQuality.LOW && level != GlassLevel.Subtle
    val useShader = sampling && g.quality == GlassQuality.HIGH && sdk >= Build.VERSION_CODES.TIRAMISU
    val blurDp = if (g.quality == GlassQuality.HIGH) g.blurDp * m.blur else g.blurDp * 0.45f * m.blur
    val useLayer = useShader || (sampling && sdk >= Build.VERSION_CODES.S && blurDp >= 1f && g.blurDp > 0)
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    val cache = remember { EffectCache() }

    val baseFill = when {
        g.quality == GlassQuality.LOW -> c.glassFillStrong
        level == GlassLevel.Strong -> c.glassFillStrong
        level == GlassLevel.Subtle -> c.glassFill.copy(alpha = c.glassFill.alpha * 0.6f)
        else -> c.glassFill
    }
    val fillAlpha = (baseFill.alpha * g.opacity * (if (g.quality == GlassQuality.LOW) 1f else m.fill)).coerceIn(0f, 1f)
    val fillColor = baseFill.copy(alpha = fillAlpha)
    val fillArgb = fillColor.toArgb()
    val shine = if (g.quality == GlassQuality.LOW) 0.15f else g.shine

    val outer = modifier
        .then(if (elevated && g.quality != GlassQuality.LOW) Modifier.shadow(18.dp, shape, ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
        .clip(shape)
        .then(if (useLayer) Modifier.onGloballyPositioned { rootPos = it.positionInRoot() } else Modifier)

    Box(outer) {
        if (useLayer) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val w = size.width
                        val h = size.height
                        if (w > 1f && h > 1f) {
                            val r = (shape as? CornerBasedShape)?.topStart?.toPx(size, this) ?: 26.dp.toPx()
                            val blurPx = if (blurDp >= 1f) blurDp.dp.toPx() else 0f
                            val shaderKey = if (useShader) {
                                val thick = min(14.dp.toPx() * m.thick, min(w, h) * 0.45f)
                                GlassShaders.Key(
                                    w = w.roundToInt(), h = h.roundToInt(), r = r.roundToInt(), thick = thick.roundToInt(),
                                    refr = (m.refr * g.refraction * 100f).roundToInt(),
                                    aber = (m.aber * 100f).roundToInt(),
                                    rim = (m.rim * g.shine * 100f).roundToInt(),
                                    spec = (m.spec * g.shine * 100f).roundToInt(),
                                    depth = (m.depth * 100f).roundToInt(),
                                    refl = (m.refl * g.reflection * 100f).roundToInt(),
                                    frost = (m.frost * 100f).roundToInt(),
                                    dark = c.isDark, tint = fillArgb,
                                )
                            } else null
                            renderEffect = cache.get(EffectKey(blurPx.roundToInt(), shaderKey)) { buildGlassEffect(blurPx, shaderKey) }
                        }
                    }
                    .drawBehind {
                        val s = rootSize!!.value
                        val o = hostOrigin?.value ?: Offset.Zero
                        translate(o.x - rootPos.x, o.y - rootPos.y) {
                            drawWallpaper(ws!!, c, 0f, s.width.toFloat(), s.height.toFloat())
                        }
                    },
            )
        }
        Box(
            Modifier.matchParentSize().drawBehind {
                if (!useShader) {
                    drawRect(fillColor)
                    if (g.quality != GlassQuality.LOW) {
                        // عبور نور وابسته به والپیپر: روی تصویر روشن در تم تیره تیره‌تر، روی تصویر تیره در تم روشن روشن‌تر
                        val lum = ws?.luma(c.isDark) ?: (if (c.isDark) 0.14f else 0.88f)
                        val scrim = if (c.isDark) (lum - 0.35f).coerceAtLeast(0f) * 0.5f else (0.65f - lum).coerceAtLeast(0f) * 0.5f
                        if (scrim > 0.005f) drawRect((if (c.isDark) Color.Black else Color.White).copy(alpha = scrim))
                    }
                }
                if (tint != null) drawRect(tint.copy(alpha = 0.16f))
                when {
                    g.quality == GlassQuality.LOW -> drawOutline(
                        shape.createOutline(size, layoutDirection, this),
                        SolidColor(c.glassEdge.copy(alpha = 0.15f)), style = Stroke(1.dp.toPx()),
                    )
                    useShader -> drawOutline(
                        shape.createOutline(size, layoutDirection, this),
                        SolidColor(c.glassEdge.copy(alpha = (0.20f * shine).coerceIn(0f, 1f))), style = Stroke(1.dp.toPx()),
                    )
                    else -> drawGlassOptics(shape, c, m, shine, g.reflection, full = level != GlassLevel.Subtle || g.quality == GlassQuality.HIGH)
                }
            },
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
