package com.flow.focus.ui.glass

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

internal val refractionSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * شکست نور شیشه (AGSL): لبه‌ی هر سطح مثل یک عدسی محدب، پس‌زمینه‌ی از-پیش-تارشده را با فاصله‌ی وابسته به
 * فاصله از لبه (SDF گوشه‌گرد) جابه‌جا نمونه می‌گیرد؛ پراکندگی رنگی ظریف و نور لبه (rim) هم در همین یک عبور است.
 * هر سطح فقط یک RuntimeShader سبک دارد؛ تصویر تار یک بار برای همه ساخته می‌شود.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class RefractionPainter {
    private val shader = RuntimeShader(SRC)
    private var lastBitmap: ImageBitmap? = null
    private var bitmapShader: BitmapShader? = null

    fun draw(
        scope: DrawScope,
        blurred: ImageBitmap,
        origin: Offset,
        rootWidth: Int,
        radiusPx: Float,
        bevelPx: Float,
        pullPx: Float,
        dispersion: Float,
        rim: Float,
    ) {
        if (lastBitmap !== blurred) {
            bitmapShader = BitmapShader(blurred.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also {
                it.filterMode = BitmapShader.FILTER_MODE_LINEAR
            }
            lastBitmap = blurred
            shader.setInputShader("bg", bitmapShader!!)
        }
        val s = scope.size
        shader.setFloatUniform("size", s.width, s.height)
        shader.setFloatUniform("origin", origin.x, origin.y)
        shader.setFloatUniform("scale", blurred.width.toFloat() / rootWidth.coerceAtLeast(1))
        shader.setFloatUniform("radius", radiusPx.coerceAtMost(minOf(s.width, s.height) / 2f))
        shader.setFloatUniform("bevel", bevelPx.coerceAtLeast(2f))
        shader.setFloatUniform("pull", pullPx)
        shader.setFloatUniform("disp", dispersion)
        shader.setFloatUniform("rim", rim)
        scope.drawRect(ShaderBrush(shader), size = s)
    }

    private companion object {
        const val SRC = """
uniform shader bg;
uniform float2 size;
uniform float2 origin;
uniform float scale;
uniform float radius;
uniform float bevel;
uniform float pull;
uniform float disp;
uniform float rim;

float sdBox(float2 p, float2 hb, float r) {
    float2 q = abs(p) - hb + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 fc) {
    float2 hb = size * 0.5;
    float2 p = fc - hb;
    float d = sdBox(p, hb, radius);
    float2 gx = float2(sdBox(p + float2(1.0, 0.0), hb, radius) - sdBox(p - float2(1.0, 0.0), hb, radius),
                       sdBox(p + float2(0.0, 1.0), hb, radius) - sdBox(p - float2(0.0, 1.0), hb, radius));
    float2 n = gx / max(length(gx), 0.0001);
    float t = clamp(-d / bevel, 0.0, 1.0);
    float prof = pow(1.0 - t, 2.2);
    float2 off = -n * prof * pull;
    float2 base = fc + origin;
    half4 c = bg.eval((base + off) * scale);
    if (disp > 0.0) {
        half4 cr = bg.eval((base + off * (1.0 + disp)) * scale);
        half4 cb = bg.eval((base + off * (1.0 - disp)) * scale);
        c = half4(cr.r, c.g, cb.b, 1.0);
    }
    float edge = pow(1.0 - t, 3.0);
    float facing = dot(n, normalize(float2(-0.6, -0.8)));
    float hi = edge * (0.45 + 0.55 * max(facing, 0.0)) * rim;
    float lo = edge * max(-facing, 0.0) * rim * 0.4;
    half3 rgb = c.rgb + (half3(1.0) - c.rgb) * half(hi);
    rgb *= half(1.0 - lo * 0.5);
    return half4(rgb, 1.0);
}
"""
    }
}
