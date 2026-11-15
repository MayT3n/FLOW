package com.flow.desktop.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Where a glass surface sits in the hierarchy.
 * Floating surfaces live in popups (separate native layers that cannot sample the window
 * backdrop), so they use a denser, non-sampled fill.
 */
internal enum class GlassLevel(val fillBoost: Float, val shadowScale: Float, val rimScale: Float, val sampled: Boolean) {
    Panel(1f, 1f, 1f, true),
    Chrome(1.12f, .9f, 1f, true),
    Control(.9f, 0f, .75f, true),
    Floating(2.4f, 1.2f, 1f, false),
}

private val grainBitmap by lazy {
    val img = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    val rnd = java.util.Random(7)
    for (y in 0 until 64) for (x in 0 until 64) {
        val v = rnd.nextInt(256)
        img.setRGB(x, y, (0xFF shl 24) or (v shl 16) or (v shl 8) or v)
    }
    img.toComposeImageBitmap()
}

/**
 * The single glass material used by every surface in the app (sidebar, toolbar, panels, search,
 * buttons, dialogs, menus). What it draws depends on [GlassSpec]:
 *
 *  - LOW:    flat dense fill + hairline (no backdrop sampling, no shadow)
 *  - MEDIUM: cached backdrop blur + veil + rim + soft shadow
 *  - HIGH:   stronger blur/saturation, diagonal sheen, brighter rim, deeper shadow
 *  - ULTRA:  highest blur resolution, grain, strongest rim and shadow
 *
 * The blur is a cached bitmap slice, never a live full-screen blur.
 */
@Composable
internal fun Modifier.glass(
    radius: Dp = FlowDims.panelRadius,
    level: GlassLevel = GlassLevel.Panel,
    tint: Color? = null,
): Modifier {
    val spec = LocalGlass.current
    val pal = LocalFlowPalette.current
    val backdrop = LocalBackdrop.current
    val origin = remember { mutableStateOf(Offset.Zero) }
    val saturation = remember(spec.saturation) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(spec.saturation) })
    }
    val sampling = spec.samplesBackdrop && level.sampled && backdrop != null
    val positioned = if (sampling) Modifier.onGloballyPositioned { origin.value = it.positionInRoot() } else Modifier

    return this.then(positioned).drawWithCache {
        val w = size.width
        val h = size.height
        val r = min(radius.toPx(), min(w, h) / 2f)
        val corner = CornerRadius(r, r)
        val path = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, corner)) }
        val hair = 1f * density
        val boost = level.fillBoost
        val baseFill = (if (pal.dark) spec.fillAlphaDark else spec.fillAlphaLight) * boost
        val veilTop: Brush
        val veilBottom: Float
        if (pal.dark) {
            val t = tint ?: pal.glassTint
            veilTop = Brush.verticalGradient(
                listOf(
                    t.copy(alpha = (baseFill + .10f).coerceIn(0f, 1f)),
                    t.copy(alpha = (baseFill - .02f).coerceIn(0f, 1f)),
                ),
                startY = 0f, endY = h,
            )
            veilBottom = .06f
        } else {
            val t = tint ?: pal.glassTint
            veilTop = Brush.verticalGradient(
                listOf(
                    t.copy(alpha = (baseFill + .12f).coerceIn(0f, 1f)),
                    t.copy(alpha = (baseFill - .04f).coerceIn(0f, 1f)),
                ),
                startY = 0f, endY = h,
            )
            veilBottom = .0f
        }
        val flat = (tint ?: pal.flatSurface).copy(alpha = (if (pal.dark) .86f else .80f).coerceAtMost(.97f))
        val sheen = if (spec.sheenAlpha > 0f && level != GlassLevel.Control) Brush.linearGradient(
            listOf(pal.glassLight.copy(alpha = spec.sheenAlpha * (if (pal.dark) .55f else .9f)), Color.Transparent),
            start = Offset(0f, 0f), end = Offset(w * .55f, h * .95f),
        ) else null
        val rimAlpha = spec.rimAlpha * level.rimScale
        val rim = Brush.linearGradient(
            listOf(
                pal.glassLight.copy(alpha = (if (pal.dark) .34f else .95f) * rimAlpha),
                pal.accent.copy(alpha = .16f * rimAlpha),
                pal.glassLight.copy(alpha = (if (pal.dark) .08f else .35f) * rimAlpha),
            ),
            start = Offset(0f, 0f), end = Offset(w, h),
        )
        val shadowColor = if (pal.dark) Color.Black else Color(0xFF1B2A4A)
        val rings = if (level.shadowScale > 0f) spec.shadowRings else 0
        val ringAlpha = if (rings > 0) spec.shadowAlpha * level.shadowScale * (if (pal.dark) 1.7f else 1f) / rings else 0f
        val grain = if (spec.grain && level != GlassLevel.Control) ShaderBrush(ImageShader(grainBitmap, TileMode.Repeated, TileMode.Repeated)) else null

        onDrawBehind {
            // 1) Soft shadow, drawn only outside the panel so translucent glass never shows a dark smudge.
            if (rings > 0) {
                clipPath(path, clipOp = ClipOp.Difference) {
                    val step = 2.4f * density
                    val yOff = 4f * density
                    for (i in rings downTo 1) {
                        val grow = i * step
                        drawRoundRect(
                            color = shadowColor.copy(alpha = ringAlpha),
                            topLeft = Offset(-grow, -grow + yOff),
                            size = Size(w + 2 * grow, h + 2 * grow),
                            cornerRadius = CornerRadius(r + grow),
                        )
                    }
                }
            }
            clipPath(path) {
                // 2) Backdrop: this panel's slice of the cached blurred wallpaper.
                val blurred = backdrop?.bitmaps?.blurred
                val root = backdrop?.rootSize
                if (sampling && blurred != null && root != null && root.width > 0 && root.height > 0) {
                    val kx = blurred.width / root.width.toFloat()
                    val ky = blurred.height / root.height.toFloat()
                    val sx = (origin.value.x * kx).roundToInt().coerceIn(0, blurred.width - 1)
                    val sy = (origin.value.y * ky).roundToInt().coerceIn(0, blurred.height - 1)
                    val sw = (w * kx).roundToInt().coerceIn(1, blurred.width - sx)
                    val sh = (h * ky).roundToInt().coerceIn(1, blurred.height - sy)
                    drawImage(
                        blurred,
                        srcOffset = IntOffset(sx, sy),
                        srcSize = IntSize(sw, sh),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                        colorFilter = saturation,
                        filterQuality = FilterQuality.Medium,
                    )
                } else {
                    drawRect(flat)
                }
                // 3) Veil — brightens (light) / deepens (dark) the sampled backdrop.
                drawRect(veilTop)
                if (veilBottom > 0f) {
                    drawRect(
                        Brush.verticalGradient(listOf(pal.glassLight.copy(alpha = veilBottom), Color.Transparent), startY = 0f, endY = h * .6f),
                    )
                }
                // 4) Sheen (HIGH+) and grain (ULTRA).
                if (sheen != null) drawRect(sheen)
                if (grain != null) drawRect(grain, alpha = if (pal.dark) .05f else .035f)
            }
            // 5) Specular rim — a lit upper-left edge fading to a faint lower-right edge.
            val inset = hair / 2f
            drawRoundRect(
                brush = rim,
                topLeft = Offset(inset, inset),
                size = Size(w - hair, h - hair),
                cornerRadius = CornerRadius(maxOf(0f, r - inset)),
                style = Stroke(width = hair),
            )
        }
    }
}
