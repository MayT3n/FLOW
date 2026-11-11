package com.flow.focus.ui.glass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * شیشه‌ی نوری واقعی (HIGH، اندروید ۱۳+): یک Shader از نوع AGSL روی بک‌دراپ نمونه‌برداری‌شده‌ی همان والپیپر اجرا می‌شود:
 *  - شکست نور و جابه‌جایی (Refraction) فقط در لبه‌ی خمیده (متن هرگز تحریف نمی‌شود چون روی شیشه رسم می‌شود)
 *  - انحراف رنگی (Chromatic aberration) بسیار ظریف
 *  - روشنایی لبه به‌روش Fresnel، هایلایت جهت‌دار، سایه‌ی درونی و بازتاب نرم لبه
 *  - عبور نور وابسته به روشنایی پیکسل پشت شیشه (خوانایی روی والپیپر روشن و تیره)
 *
 * Shader‌ها بر اساس (اندازه + پارامترها) کش می‌شوند و بین کارت‌های هم‌اندازه به‌اشتراک گذاشته می‌شوند؛
 * یکنواخت‌ها فقط هنگام ساخت مقدار می‌گیرند، پس تغییر یک کارت روی بقیه اثر نمی‌گذارد.
 */
@RequiresApi(33)
object GlassShaders {
    private const val SRC = """
uniform shader src;
uniform float2 res;
uniform float radius;
uniform float thick;
uniform float refr;
uniform float aber;
uniform float rim;
uniform float spec;
uniform float depth;
uniform float refl;
uniform float frost;
uniform float dark;
uniform float2 lightDir;
uniform float4 tint;

float sdRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
}

float2 clampC(float2 c) {
    return clamp(c, float2(0.5), res - float2(0.5));
}

half4 main(float2 fc) {
    float2 hs = res * 0.5;
    float2 p = fc - hs;
    float r = min(radius, min(hs.x, hs.y));
    float d = sdRect(p, hs, r);
    float inner = max(-d, 0.0);
    float cov = clamp(0.5 - d, 0.0, 1.0);

    float2 g = float2(
        sdRect(p + float2(1.0, 0.0), hs, r) - sdRect(p - float2(1.0, 0.0), hs, r),
        sdRect(p + float2(0.0, 1.0), hs, r) - sdRect(p - float2(0.0, 1.0), hs, r));
    float2 n = g / max(length(g), 0.0001);

    float bev = clamp(1.0 - inner / thick, 0.0, 1.0);
    float prof = bev * bev * (3.0 - 2.0 * bev);
    float2 off = -n * prof * thick * refr * 0.85;

    float3 col;
    float3 rc = float3(0.0);
    if (inner > thick * 1.6) {
        col = src.eval(fc).rgb;
    } else {
        float2 oR = off * (1.0 + aber * 4.0);
        float2 oB = off * (1.0 - aber * 4.0);
        col = float3(
            src.eval(clampC(fc + oR)).r,
            src.eval(clampC(fc + off)).g,
            src.eval(clampC(fc + oB)).b);
        rc = src.eval(clampC(fc - n * (inner * 2.0 + thick * 0.6))).rgb;
    }

    // عبور نور وابسته به روشنایی پشت شیشه
    float luma = dot(col, float3(0.2126, 0.7152, 0.0722));
    float hi = smoothstep(0.36, 0.85, luma);
    float lo = 1.0 - smoothstep(0.18, 0.62, luma);
    float3 dim = col * (1.0 - 0.45 * hi);
    float3 lift = mix(col, float3(1.0), 0.5 * lo);
    col = mix(lift, dim, dark);
    col = mix(col, float3(luma), frost * 0.30);
    col = mix(col, tint.rgb, tint.a);

    float facing = dot(n, lightDir);
    float lit = max(facing, 0.0);
    float back = max(-facing, 0.0);
    float edgeT = clamp(1.0 - inner / (thick * 0.55), 0.0, 1.0);
    float fres = edgeT * edgeT;
    float rimK = 1.0 - 0.35 * (1.0 - dark);

    // بازتاب نرم لبه
    float rmask = prof * (0.25 + 0.75 * back) * refl;
    col = mix(col, rc * 1.15, clamp(rmask * 0.30, 0.0, 0.5) * step(0.5, length(rc)));

    // روشنایی Fresnel
    col += rim * rimK * fres * (0.12 + 0.55 * lit + 0.18 * back * refl);

    // هایلایت جهت‌دار روی لبه‌ی رو به نور
    float bx = (inner - 1.6) / 1.5;
    float band = exp(-bx * bx);
    col += spec * band * (0.15 + 0.85 * lit * lit) * 0.9;

    // درخشش ملایم سطح
    float2 pn = p / max(res.x, res.y);
    col += spec * 0.07 * smoothstep(-0.1, 0.55, dot(pn, lightDir));

    // سایه‌ی درونی و ضخامت
    float sh = (1.0 - smoothstep(0.0, thick * 1.4, inner)) * (0.35 + 0.65 * back);
    col *= 1.0 - depth * 0.30 * sh;

    col = clamp(col, 0.0, 1.0);
    return half4(half3(col * cov), half(cov));
}
"""

    data class Key(
        val w: Int, val h: Int, val r: Int, val thick: Int,
        val refr: Int, val aber: Int, val rim: Int, val spec: Int,
        val depth: Int, val refl: Int, val frost: Int,
        val dark: Boolean, val tint: Int,
    )

    private val cache = object : LinkedHashMap<Key, RuntimeShader>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, RuntimeShader>?): Boolean = size > 48
    }

    @Synchronized
    fun shader(k: Key): RuntimeShader {
        cache[k]?.let { return it }
        val s = create(k)
        cache[k] = s
        return s
    }

    private fun create(k: Key): RuntimeShader {
        val s = RuntimeShader(SRC)
        s.setFloatUniform("res", k.w.toFloat(), k.h.toFloat())
        s.setFloatUniform("radius", k.r.toFloat())
        s.setFloatUniform("thick", k.thick.coerceAtLeast(1).toFloat())
        s.setFloatUniform("refr", k.refr / 100f)
        s.setFloatUniform("aber", k.aber / 100f)
        s.setFloatUniform("rim", k.rim / 100f)
        s.setFloatUniform("spec", k.spec / 100f)
        s.setFloatUniform("depth", k.depth / 100f)
        s.setFloatUniform("refl", k.refl / 100f)
        s.setFloatUniform("frost", k.frost / 100f)
        s.setFloatUniform("dark", if (k.dark) 1f else 0f)
        s.setFloatUniform("lightDir", -0.55f, -0.83f)
        val a = ((k.tint ushr 24) and 0xFF) / 255f
        val rr = ((k.tint ushr 16) and 0xFF) / 255f
        val gg = ((k.tint ushr 8) and 0xFF) / 255f
        val bb = (k.tint and 0xFF) / 255f
        s.setFloatUniform("tint", rr, gg, bb, a)
        return s
    }
}

/** افکت نهایی لایه‌ی پس‌زمینه: [blurPx] (در صورت نیاز) و سپس شیدر نوری (در صورت [key] غیر null). */
@RequiresApi(31)
internal fun buildGlassEffect(blurPx: Float, key: GlassShaders.Key?): RenderEffect? {
    val blur = if (blurPx > 0.5f) AndroidRenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP) else null
    val shader = if (key != null && Build.VERSION.SDK_INT >= 33) {
        AndroidRenderEffect.createRuntimeShaderEffect(GlassShaders.shader(key), "src")
    } else null
    val e = when {
        blur != null && shader != null -> AndroidRenderEffect.createChainEffect(shader, blur)
        shader != null -> shader
        else -> blur
    }
    return e?.asComposeRenderEffect()
}
