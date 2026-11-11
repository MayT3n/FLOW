package com.flow.focus.ui.glass

import android.graphics.BitmapFactory
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.Settings
import com.flow.focus.data.WallpaperFit
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.FlowColors
import com.flow.focus.ui.theme.LocalGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * سیستم مرکزی Liquid Glass. همه‌ی پنل‌ها، Dock، دیالوگ‌ها و دکمه‌ها از [GlassSurface] استفاده می‌کنند، پس
 * شفافیت، Blur، نور و کیفیت از یک نقطه (تنظیمات) کنترل می‌شود.
 *
 *  - LOW:    سطح تخت و کم‌شفاف (کم‌مصرف‌ترین)
 *  - MEDIUM: تاری ملایم پس‌زمینه + Tint، Highlight و لبه‌ی نورانی
 *  - HIGH:   تاری قوی‌تر و عمق بیشتر (RenderEffect، اندروید ۱۲+)
 *
 * پس‌زمینه یک بار در ریشه رسم می‌شود و هر سطح فقط ناحیه‌ی هم‌اندازه‌ی خودش را از همان
 * فریم رسم می‌کند؛ بنابراین اسکرول و تغییر صفحه باعث جداافتادگی تصویر پس‌زمینه نمی‌شود.
 */
enum class GlassLevel { Subtle, Regular, Strong }

val LocalRootSize = compositionLocalOf<State<IntSize>?> { null }

data class RootBackdropFrame(
    val image: State<ImageBitmap?>,
    val phase: State<Float>,
    val settings: Settings,
)

val LocalRootBackdrop = compositionLocalOf<RootBackdropFrame?> { null }

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

private fun DrawScope.drawBuiltinWallpaper(c: FlowColors, phase: Float, w: Float, h: Float, id: String) {
    if (id == "gradient" || id == "custom") {
        drawFlowBackground(c, phase, w, h)
        return
    }
    val palette = when (id) {
        "aurora" -> listOf(Color(0xFF07172B), Color(0xFF173B59), Color(0xFF382B65))
        "midnight" -> listOf(Color(0xFF070D1B), Color(0xFF172A42), Color(0xFF26364B))
        "dusk" -> listOf(Color(0xFF241B37), Color(0xFF69435E), Color(0xFF273D5C))
        else -> listOf(c.bgTop, c.bgMid, c.bgBottom)
    }
    drawRect(Brush.verticalGradient(palette, startY = 0f, endY = h), size = androidx.compose.ui.geometry.Size(w, h))
    val motion = phase * 2f * PI.toFloat()
    val glow = when (id) {
        "aurora" -> Color(0xFF68C8D6)
        "midnight" -> Color(0xFF547DB4)
        else -> Color(0xFFD18B9A)
    }
    val center = Offset(w * (0.78f + 0.04f * cos(motion)), h * 0.2f)
    drawCircle(
        Brush.radialGradient(listOf(glow.copy(alpha = 0.36f), glow.copy(alpha = 0f)), center = center, radius = w * 0.78f),
        radius = w * 0.78f, center = center,
    )
    drawCircle(
        Brush.radialGradient(listOf(c.blobB.copy(alpha = 0.26f), Color.Transparent), center = Offset(w * 0.1f, h * 0.84f), radius = w * 0.72f),
        radius = w * 0.72f, center = Offset(w * 0.1f, h * 0.84f),
    )
}

private fun DrawScope.drawWallpaperImage(image: ImageBitmap, w: Float, h: Float, fit: WallpaperFit, fallback: Color) {
    drawRect(fallback, size = androidx.compose.ui.geometry.Size(w, h))
    val scale = if (fit == WallpaperFit.CROP) maxOf(w / image.width, h / image.height) else minOf(w / image.width, h / image.height)
    val dstWidth = (image.width * scale).toInt().coerceAtLeast(1)
    val dstHeight = (image.height * scale).toInt().coerceAtLeast(1)
    drawImage(
        image = image,
        dstOffset = IntOffset((w.toInt() - dstWidth) / 2, (h.toInt() - dstHeight) / 2),
        dstSize = IntSize(dstWidth, dstHeight),
        filterQuality = FilterQuality.Medium,
    )
}

private fun DrawScope.drawBackdrop(
    c: FlowColors,
    phase: Float,
    w: Float,
    h: Float,
    image: ImageBitmap?,
    settings: Settings,
) {
    if (settings.wallpaperId == "custom" && image != null) {
        drawWallpaperImage(image, w, h, settings.wallpaperFit, c.bgBottom)
    } else {
        drawBuiltinWallpaper(c, phase, w, h, settings.wallpaperId)
    }
    if (settings.wallpaperId != "gradient" && settings.wallpaperDim > 0f) {
        drawRect(Color.Black.copy(alpha = settings.wallpaperDim.coerceIn(0f, 0.65f)))
    }
}

/** Shared wallpaper renderer. The decoded image is loaded once and reused by glass sampling. */
@Composable
fun GlassBackdropHost(settings: Settings, content: @Composable BoxScope.() -> Unit) {
    val c = Flow.colors
    val g = LocalGlass.current
    val size = remember { mutableStateOf(IntSize.Zero) }
    val image = remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(settings.wallpaperId, settings.wallpaperPath) {
        val path = settings.wallpaperPath
        image.value = if (settings.wallpaperId == "custom" && !path.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (java.io.File(path).isFile) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
                }.getOrNull()
            }
        } else null
    }

    // The phase is read by drawing, not by the composition on every frame.
    val phase: State<Float> = if (g.animations && g.quality != GlassQuality.LOW) {
        rememberInfiniteTransition(label = "bg")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "bgPhase")
    } else remember { mutableStateOf(0f) }

    val frame = RootBackdropFrame(image, phase, settings)
    Box(Modifier.fillMaxSize().onSizeChanged { size.value = it }) {
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settings.wallpaperBlurDp > 0) {
                    val radius = settings.wallpaperBlurDp.dp.toPx()
                    BlurEffect(radius, radius)
                } else null
            },
        ) {
            drawBackdrop(c, phase.value, this.size.width, this.size.height, image.value, settings)
        }
        CompositionLocalProvider(
            LocalRootSize provides size,
            LocalRootBackdrop provides frame,
        ) { content() }
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
    val rootBackdrop = LocalRootBackdrop.current
    // LOW avoids RenderEffect, MEDIUM uses a restrained blur, HIGH uses the full requested blur.
    // The exact same root backdrop is sampled by every surface to keep glass coherent while scrolling.
    val blurStrength = when (g.quality) {
        GlassQuality.LOW -> 0f
        GlassQuality.MEDIUM -> 0.48f
        GlassQuality.HIGH -> 1f
    }
    val blurOn = blurStrength > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        rootSize != null && rootBackdrop != null && g.blurDp > 0
    val levelBlur = when (level) {
        GlassLevel.Subtle -> 0.55f
        GlassLevel.Regular -> 1f
        GlassLevel.Strong -> 1.15f
    }
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
                        val extra = rootBackdrop!!.settings.wallpaperBlurDp.dp.toPx() * 0.35f
                        // Prevent extreme settings from turning each surface into an expensive full-screen blur pass.
                        val requested = g.blurDp.dp.toPx() * blurStrength * levelBlur + extra * blurStrength
                        val maxRadius = if (g.quality == GlassQuality.MEDIUM) 20.dp.toPx() else 36.dp.toPx()
                        val radius = requested.coerceAtMost(maxRadius)
                        renderEffect = BlurEffect(radius, radius)
                    }
                    .drawBehind {
                        val s = rootSize!!.value
                        val frame = rootBackdrop!!
                        translate(-origin.x, -origin.y) {
                            drawBackdrop(c, frame.phase.value, s.width.toFloat(), s.height.toFloat(), frame.image.value, frame.settings)
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
        val edgeStrength = when (level) {
            GlassLevel.Subtle -> 0.58f
            GlassLevel.Regular -> 0.82f
            GlassLevel.Strong -> 1f
        }
        Box(
            Modifier.matchParentSize().border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        c.glassEdge.copy(alpha = 0.62f * shine * edgeStrength),
                        c.glassEdge.copy(alpha = 0.06f * edgeStrength),
                        c.glassEdge.copy(alpha = 0.025f * edgeStrength),
                        c.glassEdge.copy(alpha = 0.32f * shine * edgeStrength),
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
