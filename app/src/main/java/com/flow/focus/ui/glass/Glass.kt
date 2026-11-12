package com.flow.focus.ui.glass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
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
    val tone: State<Color?>,
    val palette: State<List<Color>>,
    /** مختصات صفحه‌ی گوشه‌ی بالا-چپ ریشه؛ برای محاسبه‌ی دقیق offset سطح‌ها در پنجره‌های جدا */
    val rootOrigin: State<Offset>,
    val settings: Settings,
)

val LocalRootBackdrop = compositionLocalOf<RootBackdropFrame?> { null }

/**
 * لایه‌ی زنده‌ی پشت شیشه: پس‌زمینه + محتوای صفحه‌ی فعلی. فقط عناصر شناور (Dock، مینی‌پلیر) آن را می‌بینند،
 * پس محتوای واقعی که زیرشان اسکرول می‌شود تار می‌شود. دیالوگ‌ها و منوها (پنجره‌ی جدا) null می‌گیرند و
 * از پس‌زمینه‌ی ثابت نمونه می‌گیرند.
 */
val LocalBackdropLayer = compositionLocalOf<GraphicsLayer?> { null }

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

/** Samples a compact grid once per wallpaper change to give glass a subtle matching tint. */
private fun Bitmap.sampledAverageColor(): Color {
    var red = 0L
    var green = 0L
    var blue = 0L
    var samples = 0
    val stepX = (width / 6).coerceAtLeast(1)
    val stepY = (height / 6).coerceAtLeast(1)
    for (y in stepY until height step stepY) {
        for (x in stepX until width step stepX) {
            val pixel = getPixel(x, y)
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            samples++
        }
    }
    if (samples == 0) return Color.Transparent
    return Color(
        red = red.toFloat() / samples / 255f,
        green = green.toFloat() / samples / 255f,
        blue = blue.toFloat() / samples / 255f,
    )
}

private fun Bitmap.sampledPalette(): List<Color> {
    val samples = ArrayList<Pair<Float, Color>>(64)
    val stepX = (width / 10).coerceAtLeast(1)
    val stepY = (height / 10).coerceAtLeast(1)
    val hsv = FloatArray(3)
    for (y in stepY until height step stepY) {
        for (x in stepX until width step stepX) {
            val pixel = getPixel(x, y)
            android.graphics.Color.colorToHSV(pixel, hsv)
            if (hsv[1] > 0.22f && hsv[2] in 0.18f..0.94f) {
                val score = hsv[1] * (0.55f + (1f - kotlin.math.abs(hsv[2] - 0.62f)))
                samples += score to Color(pixel)
            }
        }
    }
    val selected = ArrayList<Color>(3)
    for ((_, color) in samples.sortedByDescending { it.first }) {
        val distinct = selected.all { old ->
            val dr = color.red - old.red
            val dg = color.green - old.green
            val db = color.blue - old.blue
            dr * dr + dg * dg + db * db > 0.075f
        }
        if (distinct) selected += color
        if (selected.size == 3) break
    }
    listOf(Color(0xFF7896B6), Color(0xFF8FAEAA), Color(0xFF9A8EAE)).forEach {
        if (selected.size < 3 && it !in selected) selected += it
    }
    return selected
}

private fun builtinWallpaperPalette(id: String): List<Color> = when (id) {
    "aurora" -> listOf(Color(0xFF68C8D6), Color(0xFF6B7DD1), Color(0xFF254B72))
    "midnight" -> listOf(Color(0xFF547DB4), Color(0xFF7D8FA8), Color(0xFF26364B))
    "dusk" -> listOf(Color(0xFFD18B9A), Color(0xFF9C739A), Color(0xFF344E70))
    else -> listOf(Color(0xFF77899D), Color(0xFF789D9A), Color(0xFF8C80A8))
}

private fun decodeStoredWallpaper(context: Context, path: String): Bitmap? {
    val candidate = File(path).canonicalFile
    val allowedDirectory = File(context.filesDir, "wallpapers").canonicalFile
    if (
        candidate.parentFile != allowedDirectory ||
        !candidate.name.startsWith("wallpaper-") ||
        !candidate.isFile ||
        candidate.length() !in 1L..MAX_WALLPAPER_BYTES
    ) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(candidate.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_WALLPAPER_DIMENSION) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        candidate.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        },
    )
}

private const val MAX_WALLPAPER_BYTES = 32L * 1024L * 1024L
private const val MAX_WALLPAPER_DIMENSION = 2048

private fun builtinWallpaperTone(id: String): Color? = when (id) {
    "aurora" -> Color(0xFF24556D)
    "midnight" -> Color(0xFF263A55)
    "dusk" -> Color(0xFF765268)
    "gradient" -> Color(0xFF77899D)
    else -> null
}

/**
 * ریشه‌ی Liquid Glass. [source] (پس‌زمینه + صفحه‌ها) در یک GraphicsLayer ضبط می‌شود و [chrome]
 * (Dock و مینی‌پلیر) روی آن قرار می‌گیرد و از همان لایه‌ی زنده برای تاری استفاده می‌کند.
 */
@Composable
fun GlassBackdropHost(
    settings: Settings,
    source: @Composable BoxScope.() -> Unit,
    chrome: @Composable BoxScope.() -> Unit,
) {
    val c = Flow.colors
    val g = LocalGlass.current
    val context = LocalContext.current.applicationContext
    val size = remember { mutableStateOf(IntSize.Zero) }
    val rootOrigin = remember { mutableStateOf(Offset.Zero) }
    val image = remember { mutableStateOf<ImageBitmap?>(null) }
    val tone = remember { mutableStateOf<Color?>(null) }
    val palette = remember { mutableStateOf(builtinWallpaperPalette(settings.wallpaperId)) }
    val layer = rememberGraphicsLayer()

    LaunchedEffect(settings.wallpaperId, settings.wallpaperPath) {
        val path = settings.wallpaperPath
        val (decoded, sampledTone, sampledColors) = if (settings.wallpaperId == "custom" && !path.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = decodeStoredWallpaper(context, path)
                    Triple(bitmap, bitmap?.sampledAverageColor(), bitmap?.sampledPalette().orEmpty())
                }.getOrDefault(Triple(null, null, emptyList()))
            }
        } else {
            Triple(null, builtinWallpaperTone(settings.wallpaperId), builtinWallpaperPalette(settings.wallpaperId))
        }
        image.value = decoded?.asImageBitmap()
        tone.value = sampledTone ?: builtinWallpaperTone(settings.wallpaperId)
        palette.value = sampledColors.ifEmpty { builtinWallpaperPalette(settings.wallpaperId) }
    }

    // The phase is read by drawing, not by the composition on every frame.
    val phase: State<Float> = if (g.animations && g.quality != GlassQuality.LOW) {
        rememberInfiniteTransition(label = "bg")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "bgPhase")
    } else remember { mutableStateOf(0f) }

    val frame = RootBackdropFrame(image, phase, tone, palette, rootOrigin, settings)
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { size.value = it }
            .onGloballyPositioned { rootOrigin.value = it.positionOnScreen() },
    ) {
        CompositionLocalProvider(
            LocalRootSize provides size,
            LocalRootBackdrop provides frame,
        ) {
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    // The live layer is recorded only when floating chrome can actually sample it.
                    if (g.quality != GlassQuality.LOW) {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    } else {
                        drawContent()
                    }
                },
            ) {
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
                source()
            }
            CompositionLocalProvider(LocalBackdropLayer provides if (g.quality != GlassQuality.LOW) layer else null) {
                chrome()
            }
        }
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
    val liveLayer = LocalBackdropLayer.current
    val hasLive = liveLayer != null && rootBackdrop != null
    // LOW: no blur at all. MEDIUM: restrained blur, only where it is visible (live content or a photo wallpaper).
    // HIGH: full blur on every non-subtle surface plus depth highlights.
    val blurStrength = when (g.quality) {
        GlassQuality.LOW -> 0f
        GlassQuality.MEDIUM -> 0.55f
        GlassQuality.HIGH -> 1f
    }
    val photoWallpaper = rootBackdrop?.let { it.settings.wallpaperId == "custom" && it.image.value != null } == true
    // Subtle content rows avoid a separate RenderEffect allocation on every scrolling item.
    val blurOn = blurStrength > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        rootSize != null && rootBackdrop != null && g.blurDp > 0 &&
        when (g.quality) {
            GlassQuality.MEDIUM -> hasLive || (photoWallpaper && level != GlassLevel.Subtle)
            GlassQuality.HIGH -> hasLive || level != GlassLevel.Subtle
            GlassQuality.LOW -> false
        }
    val levelBlur = when (level) {
        GlassLevel.Subtle -> 0.55f
        GlassLevel.Regular -> 1f
        GlassLevel.Strong -> 1.15f
    }
    var origin by remember { mutableStateOf(Offset.Zero) }

    val wallpaperTone = rootBackdrop?.tone?.value
    val wallpaperLuminance = wallpaperTone?.let { tone ->
        0.2126f * tone.red + 0.7152f * tone.green + 0.0722f * tone.blue
    }
    // Bright wallpaper needs a slightly stronger dark surface, while dark wallpaper
    // needs a little more light glass. Only the difficult extremes receive this boost.
    val contrastBoost = when {
        c.isDark && wallpaperLuminance != null && wallpaperLuminance > 0.58f -> 0.14f
        !c.isDark && wallpaperLuminance != null && wallpaperLuminance > 0.62f -> 0.19f
        !c.isDark && wallpaperLuminance != null && wallpaperLuminance < 0.30f -> 0.22f
        !c.isDark && wallpaperLuminance != null -> 0.15f
        else -> 0f
    }
    val baseFill = when {
        g.quality == GlassQuality.LOW -> c.glassFillStrong
        level == GlassLevel.Strong -> c.glassFillStrong
        level == GlassLevel.Subtle -> c.glassFill.copy(alpha = c.glassFill.alpha * 0.6f)
        else -> c.glassFill
    }.let { surface -> surface.copy(alpha = (surface.alpha + contrastBoost).coerceAtMost(0.76f)) }
    val fill = baseFill.copy(alpha = (baseFill.alpha * g.opacity.coerceIn(0.65f, 1.18f)).coerceIn(0f, 0.88f))
    val shine = if (g.quality == GlassQuality.LOW) 0.15f else g.shine
    val animationSpec = if (g.animations) tween<Color>(220) else snap()
    val animatedFill by animateColorAsState(fill, animationSpec, label = "glass-fill")
    val targetTint = tint?.copy(alpha = 0.16f) ?: Color.Transparent
    val animatedTint by animateColorAsState(targetTint, animationSpec, label = "glass-surface-tint")
    val paletteTint = rootBackdrop?.palette?.value?.firstOrNull() ?: rootBackdrop?.tone?.value
    val paletteOverlay = paletteTint?.copy(alpha = if (c.isDark) 0.055f else 0.035f) ?: Color.Transparent
    val animatedPaletteOverlay by animateColorAsState(paletteOverlay, animationSpec, label = "glass-wallpaper-tint")
    val animatedShine by animateFloatAsState(
        targetValue = shine,
        animationSpec = if (g.animations) tween(240) else snap(),
        label = "glass-shine",
    )

    val outer = modifier
        .then(if (elevated && g.quality != GlassQuality.LOW) Modifier.shadow(18.dp, shape, ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
        .clip(shape)
        .then(
            if (blurOn) {
                Modifier.onGloballyPositioned { coords ->
                    val root = rootBackdrop?.rootOrigin?.value ?: Offset.Zero
                    origin = coords.positionOnScreen() - root
                }
            } else Modifier,
        )

    Box(outer) {
        if (blurOn) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val extra = rootBackdrop!!.settings.wallpaperBlurDp.dp.toPx() * 0.35f
                        // Prevent extreme settings from turning each surface into an expensive full-screen blur pass.
                        val requested = g.blurDp.dp.toPx() * blurStrength * levelBlur + extra * blurStrength
                        val maxRadius = if (g.quality == GlassQuality.MEDIUM) 22.dp.toPx() else 40.dp.toPx()
                        val radius = requested.coerceAtMost(maxRadius)
                        renderEffect = BlurEffect(radius, radius)
                    }
                    .drawBehind {
                        val s = rootSize!!.value
                        val frame = rootBackdrop!!
                        translate(-origin.x, -origin.y) {
                            if (liveLayer != null) {
                                drawLayer(liveLayer)
                            } else {
                                drawBackdrop(c, frame.phase.value, s.width.toFloat(), s.height.toFloat(), frame.image.value, frame.settings)
                            }
                        }
                    },
            )
        }
        Box(
            Modifier.matchParentSize().drawBehind {
                drawRect(animatedFill)
                // A restrained wallpaper tint blends with the glass when the backdrop changes.
                if (animatedPaletteOverlay.alpha > 0.001f) drawRect(animatedPaletteOverlay)
                if (animatedTint.alpha > 0.001f) drawRect(animatedTint)
                if (g.quality != GlassQuality.LOW) {
                    drawRect(
                        Brush.verticalGradient(
                            0f to c.glassTintTop.copy(alpha = c.glassTintTop.alpha * animatedShine),
                            0.55f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    )
                }
                if (g.quality == GlassQuality.HIGH) {
                    // Specular glint from the top corner and a soft inner shade at the bottom give real depth.
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = (if (c.isDark) 0.10f else 0.22f) * animatedShine), Color.Transparent),
                            center = Offset(size.width * 0.18f, 0f),
                            radius = size.maxDimension * 0.85f,
                        ),
                    )
                    drawRect(
                        Brush.verticalGradient(
                            0.6f to Color.Transparent,
                            1f to c.shadow.copy(alpha = c.shadow.alpha * 0.22f),
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
                        c.glassEdge.copy(alpha = 0.62f * animatedShine * edgeStrength),
                        c.glassEdge.copy(alpha = 0.06f * edgeStrength),
                        c.glassEdge.copy(alpha = 0.025f * edgeStrength),
                        c.glassEdge.copy(alpha = 0.32f * animatedShine * edgeStrength),
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
