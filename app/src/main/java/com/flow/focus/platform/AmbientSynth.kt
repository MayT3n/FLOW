package com.flow.focus.platform

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * صداهای محیط با پردازش سیگنال در خود برنامه تولید می‌شوند؛ پس هیچ فایل صوتی با مجوز نامشخص در برنامه نیست.
 * «کافه» و «کتابخانه» تقریب ساختگی (سنتزی) از فضای واقعی‌اند، نه ضبط واقعی.
 */
data class AmbientSoundInfo(val id: String, val label: String, val hint: String)

object AmbientSynth {
    const val RATE = 22050
    const val NONE = "none"

    val sounds = AmbientLibrary.recordings.map { recording ->
        AmbientSoundInfo(recording.id, recording.title, recording.hint)
    }

    private const val XFADE = RATE / 2

    /** حلقه‌ی بدون درز: به اندازه‌ی XFADE اضافه تولید و با هم‌پوشانی هم‌توان به ابتدا چسبانده می‌شود. */
    fun generate(id: String, seconds: Int = 12): ShortArray {
        val loopN = RATE * seconds
        val n = loopN + XFADE
        val rnd = Random(id.hashCode())
        val x = FloatArray(n)
        var targetRms = 0.14f
        fun cyc(k: Int, i: Int, phase: Double = 0.0) = sin(2.0 * PI * k * i / loopN + phase).toFloat()

        when (id) {
            "white" -> for (i in 0 until n) x[i] = rnd.nextFloat() * 2f - 1f
            "pink" -> pink(x, rnd)
            "brown" -> brown(x, rnd)
            "wind" -> {
                pink(x, rnd)
                var lp = 0f
                for (i in 0 until n) {
                    lp += 0.08f * (x[i] - lp)
                    x[i] = lp * 3f * (0.55f + 0.30f * cyc(3, i) + 0.15f * cyc(7, i))
                }
                for (i in 0 until n) {
                    val rustle = maxOf(0f, cyc(5, i)) * 0.12f
                    x[i] += (rnd.nextFloat() * 2f - 1f) * rustle
                }
            }
            "cafe" -> {
                pink(x, rnd)
                var lp1 = 0f
                var lp2 = 0f
                for (i in 0 until n) {
                    lp1 += 0.30f * (x[i] - lp1)
                    lp2 += 0.02f * (lp1 - lp2)
                    val murmur = 0.55f + 0.22f * cyc(7, i) + 0.15f * cyc(17, i, 1.3) + 0.08f * cyc(41, i, 0.4)
                    x[i] = (lp1 - lp2) * 2.4f * murmur
                }
                clinks(x, rnd, RATE * 2, RATE * 5)
            }
            "library" -> {
                brown(x, rnd)
                var lp = 0f
                for (i in 0 until n) {
                    lp += 0.06f * (x[i] - lp)
                    x[i] = lp * 0.8f
                }
                pages(x, rnd)
                targetRms = 0.07f
            }
            else -> { // rain
                pink(x, rnd)
                var hp = 0f
                for (i in 0 until n) {
                    hp += 0.02f * (x[i] - hp)
                    x[i] = (x[i] - hp) * 0.9f
                }
                var i = 0
                while (i < n) {
                    i += 400 + rnd.nextInt(2600)
                    if (i >= n) break
                    val amp = 0.15f + rnd.nextFloat() * 0.35f
                    val len = minOf(260, n - i)
                    for (k in 0 until len) x[i + k] += amp * (rnd.nextFloat() * 2f - 1f) * exp(-k / 45.0).toFloat()
                }
                targetRms = 0.16f
            }
        }

        // هم‌پوشانی انتهای اضافه با ابتدا → حلقه‌ی پیوسته
        val out = FloatArray(loopN)
        for (i in 0 until loopN) {
            out[i] = if (i < XFADE) {
                val t = i.toFloat() / XFADE
                x[i] * sqrt(t) + x[loopN + i] * sqrt(1f - t)
            } else x[i]
        }
        var sum = 0.0
        for (v in out) sum += v * v
        val rms = sqrt(sum / loopN).toFloat().coerceAtLeast(1e-6f)
        val g = targetRms / rms
        return ShortArray(loopN) { i -> (tanh((out[i] * g).toDouble()) * 0.9 * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun pink(x: FloatArray, rnd: Random) {
        var b0 = 0f; var b1 = 0f; var b2 = 0f; var b3 = 0f; var b4 = 0f; var b5 = 0f; var b6 = 0f
        for (i in x.indices) {
            val w = rnd.nextFloat() * 2f - 1f
            b0 = 0.99886f * b0 + w * 0.0555179f
            b1 = 0.99332f * b1 + w * 0.0750759f
            b2 = 0.96900f * b2 + w * 0.1538520f
            b3 = 0.86650f * b3 + w * 0.3104856f
            b4 = 0.55000f * b4 + w * 0.5329522f
            b5 = -0.7616f * b5 - w * 0.0168980f
            x[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f) * 0.11f
            b6 = w * 0.115926f
        }
    }

    private fun brown(x: FloatArray, rnd: Random) {
        var last = 0f
        for (i in x.indices) {
            val w = rnd.nextFloat() * 2f - 1f
            last = (last + 0.02f * w) / 1.02f
            x[i] = last * 3.5f
        }
    }

    /** برخورد نرم فنجان/قاشق: سینوس میرا در ۲.۸ تا ۴.۲ کیلوهرتز */
    private fun clinks(x: FloatArray, rnd: Random, minGap: Int, maxGap: Int) {
        var i = rnd.nextInt(minGap)
        while (i < x.size) {
            val f = 2800.0 + rnd.nextInt(1400)
            val amp = 0.05f + rnd.nextFloat() * 0.10f
            val len = minOf(3200, x.size - i)
            for (k in 0 until len) {
                val env = exp(-k / 520.0).toFloat()
                x[i + k] += amp * env * (sin(2 * PI * f * k / RATE).toFloat() + 0.4f * sin(2 * PI * f * 1.51 * k / RATE).toFloat())
            }
            i += minGap + rnd.nextInt(maxGap - minGap)
        }
    }

    /** ورق زدن: انفجار کوتاه نویز روشن با پوش هان */
    private fun pages(x: FloatArray, rnd: Random) {
        var i = RATE * 2 + rnd.nextInt(RATE * 2)
        while (i < x.size) {
            val len = minOf((RATE * 0.35).toInt(), x.size - i)
            var prev = 0f
            for (k in 0 until len) {
                val w = rnd.nextFloat() * 2f - 1f
                val hp = w - prev; prev = w
                val env = (0.5 * (1 - kotlin.math.cos(2 * PI * k / len))).toFloat()
                x[i + k] += hp * env * 0.12f
            }
            i += RATE * 4 + rnd.nextInt(RATE * 5)
        }
    }
}
