package com.flow.desktop.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.flow.focus.data.WallpaperFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
internal class BackdropBitmaps(val sharp: ImageBitmap, val blurred: ImageBitmap?)

/** Holder shared by the wallpaper layer and every glass surface. Updated only when inputs change. */
@Stable
internal class BackdropState {
    var bitmaps by mutableStateOf<BackdropBitmaps?>(null)
    var rootSize by mutableStateOf(IntSize.Zero)
}

internal val LocalBackdrop = staticCompositionLocalOf<BackdropState?> { null }

internal data class BackdropRequest(
    val width: Int,
    val height: Int,
    val dark: Boolean,
    val wallpaperId: String,
    val customPath: String?,
    val fit: WallpaperFit,
    val dim: Float,
    val wallpaperBlur: Int,
    val sampleBlur: Boolean,
    val bakeScale: Float,
    val blurSigma: Float,
)

/**
 * Real frosted glass without per-frame cost: the wallpaper is rendered once into a bitmap,
 * blurred once at reduced resolution, and each glass surface simply draws its own slice of that
 * cached blur. Re-baking only happens when size / theme / wallpaper / quality change (debounced).
 */
internal object BackdropRenderer {
    private const val MAX_SHARP_EDGE = 1920f

    fun bake(req: BackdropRequest, custom: ImageBitmap?): BackdropBitmaps {
        val sharpScale = min(1f, MAX_SHARP_EDGE / max(req.width, req.height).toFloat())
        val w = max(2, (req.width * sharpScale).roundToInt())
        val h = max(2, (req.height * sharpScale).roundToInt())
        val sharp = ImageBitmap(w, h)
        val canvas = androidx.compose.ui.graphics.Canvas(sharp)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
            drawRect(if (req.dark) FlowColors.dark.wallBase else FlowColors.light.wallBase)
            if (custom != null) drawCustom(custom, req.fit) else WallpaperArt.draw(this, req.wallpaperId, req.dark)
            if (req.dim > 0f) drawRect(Color.Black.copy(alpha = req.dim.coerceIn(0f, .65f)))
        }
        val finalSharp = if (req.wallpaperBlur > 0) blurImage(sharp, 1f, req.wallpaperBlur * sharpScale * .5f) else sharp
        val blurred = if (req.sampleBlur) {
            val rel = (req.bakeScale / sharpScale).coerceIn(.05f, 1f)
            blurImage(finalSharp, rel, req.blurSigma)
        } else null
        return BackdropBitmaps(finalSharp, blurred)
    }

    private fun DrawScope.drawCustom(img: ImageBitmap, fit: WallpaperFit) {
        val scale = if (fit == WallpaperFit.CROP) max(size.width / img.width, size.height / img.height)
        else min(size.width / img.width, size.height / img.height)
        val dw = max(1, (img.width * scale).roundToInt())
        val dh = max(1, (img.height * scale).roundToInt())
        drawImage(
            img,
            dstOffset = IntOffset((size.width.roundToInt() - dw) / 2, (size.height.roundToInt() - dh) / 2),
            dstSize = IntSize(dw, dh),
        )
    }

    /** Down-scales then Gaussian-blurs with Skia. [sigma] is in output pixels. */
    fun blurImage(src: ImageBitmap, scale: Float, sigma: Float): ImageBitmap {
        val w = max(2, (src.width * scale).roundToInt())
        val h = max(2, (src.height * scale).roundToInt())
        val image = org.jetbrains.skia.Image.makeFromBitmap(src.asSkiaBitmap())
        val surface = Surface.makeRasterN32Premul(w, h)
        val paint = Paint().apply { imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP) }
        try {
            surface.canvas.drawImageRect(
                image,
                Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                Rect.makeWH(w.toFloat(), h.toFloat()),
                SamplingMode.LINEAR,
                paint,
                true,
            )
            return surface.makeImageSnapshot().toComposeImageBitmap()
        } finally {
            paint.close()
            surface.close()
            image.close()
        }
    }
}

/** Procedural wallpapers: soft colour fields plus flowing ribbons, authored per theme. */
internal object WallpaperArt {
    private class Art(val base: List<Color>, val blobs: List<Color>, val ribbons: List<Color>)

    val ids = listOf("gradient", "aurora", "midnight", "dusk")

    private fun art(id: String, dark: Boolean): Art = when (id) {
        "aurora" -> if (dark) Art(
            listOf(Color(0xFF04121A), Color(0xFF08202C)),
            listOf(Color(0xFF0E9E86), Color(0xFF2A62F0), Color(0xFF7438D6)),
            listOf(Color(0xFF3DDBB8), Color(0xFF6A8CFF), Color(0xFFA77CFF)),
        ) else Art(
            listOf(Color(0xFFEAF7F4), Color(0xFFD9EDF4)),
            listOf(Color(0xFF9EE3D2), Color(0xFFA5C2F8), Color(0xFFD3C0F4)),
            listOf(Color(0xFF7ED9C2), Color(0xFF9DB8F5), Color(0xFFC9B2F2)),
        )
        "midnight" -> if (dark) Art(
            listOf(Color(0xFF03050D), Color(0xFF0B1220)),
            listOf(Color(0xFF1B2B52), Color(0xFF26325F), Color(0xFF16203D)),
            listOf(Color(0xFF3A4C86), Color(0xFF2C3A6B), Color(0xFF55659F)),
        ) else Art(
            listOf(Color(0xFFEEF0F6), Color(0xFFDCE1EC)),
            listOf(Color(0xFFC4CDE3), Color(0xFFD3D4EA), Color(0xFFB9C6DE)),
            listOf(Color(0xFFAFBBD6), Color(0xFFC3C6E2), Color(0xFFA4B3D1)),
        )
        "dusk" -> if (dark) Art(
            listOf(Color(0xFF190F2F), Color(0xFF2A1740)),
            listOf(Color(0xFF8A3CD0), Color(0xFFCF4F80), Color(0xFF3A56D6)),
            listOf(Color(0xFFFF7FA8), Color(0xFFB07CFF), Color(0xFF6F8CFF)),
        ) else Art(
            listOf(Color(0xFFFCF0F3), Color(0xFFF1E3F3)),
            listOf(Color(0xFFF6C3CF), Color(0xFFD9C4F4), Color(0xFFBFCDF6)),
            listOf(Color(0xFFF2A9BC), Color(0xFFCDB4F2), Color(0xFFAFC0F4)),
        )
        else -> if (dark) Art( // "gradient" — FLOW default
            listOf(Color(0xFF050916), Color(0xFF0A1230)),
            listOf(Color(0xFF2B3FD0), Color(0xFF6A38D8), Color(0xFF1C6BC7)),
            listOf(Color(0xFF6D7DFF), Color(0xFFA070FF), Color(0xFF3F8CFF)),
        ) else Art(
            listOf(Color(0xFFEFF4FF), Color(0xFFDCE6FA)),
            listOf(Color(0xFFA9C2FA), Color(0xFFCDBBF4), Color(0xFFB4DDF6)),
            listOf(Color(0xFF9DB9F7), Color(0xFFC7B1F3), Color(0xFFA6D3F2)),
        )
    }

    fun draw(scope: DrawScope, id: String, dark: Boolean) = with(scope) {
        val a = art(id, dark)
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(a.base))

        val blobAlpha = if (dark) .50f else .62f
        val centers = listOf(Offset(w * .82f, h * .12f), Offset(w * .12f, h * .86f), Offset(w * .55f, h * .62f))
        val radii = listOf(max(w, h) * .62f, max(w, h) * .55f, max(w, h) * .42f)
        a.blobs.forEachIndexed { i, c ->
            drawCircle(
                brush = Brush.radialGradient(listOf(c.copy(alpha = blobAlpha), Color.Transparent), center = centers[i], radius = radii[i]),
                radius = radii[i],
                center = centers[i],
            )
        }

        fun ribbon(y0: Float, c1y: Float, c2y: Float, y1: Float, color: Color, alpha: Float) {
            val p = Path().apply {
                moveTo(-w * .1f, h * y0)
                cubicTo(w * .28f, h * c1y, w * .58f, h * c2y, w * 1.1f, h * y1)
                lineTo(w * 1.1f, h * 1.1f)
                lineTo(-w * .1f, h * 1.1f)
                close()
            }
            drawPath(p, Brush.linearGradient(listOf(color.copy(alpha = alpha), Color.Transparent), Offset(0f, h * y0), Offset(w * .4f, h * 1.05f)))
            val edge = Path().apply {
                moveTo(-w * .1f, h * y0)
                cubicTo(w * .28f, h * c1y, w * .58f, h * c2y, w * 1.1f, h * y1)
            }
            drawPath(edge, color.copy(alpha = alpha * .55f), style = Stroke(width = max(1.5f, h * .0035f)))
        }
        val ra = if (dark) .34f else .42f
        ribbon(.62f, .22f, .98f, .38f, a.ribbons[0], ra)
        ribbon(.78f, .46f, 1.1f, .58f, a.ribbons[1], ra * .85f)
        ribbon(.9f, .7f, 1.05f, .8f, a.ribbons[2], ra * .7f)
    }
}

/**
 * Hosts the wallpaper layer and provides [LocalBackdrop]. `mica` mode (Windows only) leaves the
 * layer transparent so the native DWM backdrop shows through; panels then use flat tint (nothing
 * to sample), and the UI says so honestly.
 */
@Composable
internal fun BackdropHost(
    state: BackdropState,
    dark: Boolean,
    wallpaperId: String,
    wallpaperPath: String?,
    fit: WallpaperFit,
    dim: Float,
    wallpaperBlur: Int,
    spec: GlassSpec,
    useNativeBackdrop: Boolean,
    content: @Composable () -> Unit,
) {
    val custom by produceState<ImageBitmap?>(null, wallpaperPath) {
        value = if (wallpaperPath == null) null else withContext(Dispatchers.IO) {
            runCatching { File(wallpaperPath).inputStream().use { loadImageBitmap(it) } }.getOrNull()
        }
    }
    val rootSize = state.rootSize
    val qw = (rootSize.width + 15) / 16 * 16
    val qh = (rootSize.height + 15) / 16 * 16
    val native = useNativeBackdrop && custom == null && wallpaperPath == null
    val request = BackdropRequest(
        qw, qh, dark, wallpaperId, wallpaperPath, fit, dim, wallpaperBlur,
        sampleBlur = spec.samplesBackdrop, bakeScale = spec.bakeScale, blurSigma = spec.blurSigma,
    )
    LaunchedEffect(request, custom, native) {
        if (qw < 64 || qh < 64) return@LaunchedEffect
        if (native) {
            state.bitmaps = null
            return@LaunchedEffect
        }
        // Wait for a custom image to finish decoding before baking.
        if (wallpaperPath != null && custom == null) return@LaunchedEffect
        if (state.bitmaps != null) delay(160) // debounce live window resizing
        val baked = withContext(Dispatchers.Default) { BackdropRenderer.bake(request, custom) }
        state.bitmaps = baked
    }
    val base = if (dark) FlowColors.dark.wallBase else FlowColors.light.wallBase
    CompositionLocalProvider(LocalBackdrop provides state) {
        Box(Modifier.fillMaxSize().onSizeChanged { state.rootSize = it }) {
            Canvas(Modifier.fillMaxSize()) {
                val b = state.bitmaps
                if (b != null) {
                    drawImage(b.sharp, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
                } else if (!native) {
                    drawRect(base)
                }
            }
            content()
        }
    }
}
