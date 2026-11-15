package com.flow.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.flow.desktop.design.BackdropState
import com.flow.desktop.ui.Page
import com.flow.focus.data.*
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Renders real frames of the real UI (same composables, same runtime, real Skia pipeline) into PNG files,
 * without needing a display. Used by CI so every build ships screenshots that can be reviewed.
 * Data comes from a throw-away temp directory, never from the user's profile.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal object ScreenshotRenderer {
    private data class Shot(val name: String, val w: Int, val h: Int, val scale: Float, val page: Page, val dark: Boolean, val quality: GlassQuality = GlassQuality.HIGH, val compact: Boolean = false)

    fun demoData(): AppData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val day = today.toEpochDay()
        fun at(d: LocalDate, hour: Int) = d.atTime(hour, 10).atZone(zone).toInstant().toEpochMilli()
        val tasks = listOf(
            Task("t1", "تکمیل گزارش پروژه", "نسخه‌ی نهایی برای تیم", day, 11 * 60, Priority.HIGH, "work", 3, done = true, doneAt = at(today, 9), createdAt = 1),
            Task("t2", "مطالعه کتاب", "فصل چهارم", day, 16 * 60, Priority.MEDIUM, "study", 2, done = true, doneAt = at(today, 10), createdAt = 2),
            Task("t3", "ورزش صبحگاهی", "", day, 8 * 60, Priority.LOW, "sport", 1, createdAt = 3),
            Task("t4", "مرور ایمیل‌ها", "", day + 1, 9 * 60, Priority.MEDIUM, "work", 1, createdAt = 4),
            Task("t5", "طراحی صفحه‌ی فرود", "پیش‌نویس اول", day + 3, null, Priority.HIGH, "project", 4, createdAt = 5),
            Task("t6", "تماس با خانواده", "", null, null, Priority.LOW, "personal", 1, createdAt = 6),
        )
        val sessions = buildList {
            val mins = listOf(95, 50, 75, 25, 100, 60, 45)
            mins.forEachIndexed { i, m ->
                val d = today.minusDays(i.toLong())
                var left = m
                var n = 0
                while (left > 0) {
                    val len = minOf(25, left)
                    add(FocusSession("s$i-$n", if (n % 2 == 0) "t1" else "t2", PhaseType.FOCUS, at(d, 9 + n), at(d, 9 + n) + len * 60_000L, len * 60_000L, 25 * 60_000L, len == 25))
                    left -= len; n++
                }
            }
            for (w in 1..7) add(FocusSession("w$w", "t5", PhaseType.FOCUS, at(today.minusWeeks(w.toLong()), 14), at(today.minusWeeks(w.toLong()), 14) + 3_000_000L, (40 + w * 12 % 70) * 60_000L, 25 * 60_000L, true))
        }
        return AppData(
            tasks = tasks, sessions = sessions,
            routines = listOf(
                Routine("r1", "مرور روزانه", listOf(0, 1, 2, 3, 4, 5, 6), 7 * 60 + 30, "personal"),
                Routine("r2", "ورزش", listOf(0, 2, 4), 18 * 60, "sport"),
            ),
            goals = listOf(
                Goal("g1", "۴ جلسه‌ی تمرکز", GoalPeriod.DAILY, day, true),
                Goal("g2", "خواندن ۳۰ صفحه", GoalPeriod.DAILY, day),
                Goal("g3", "پایان طراحی پروژه", GoalPeriod.WEEKLY, com.flow.focus.core.JalaliDate.startOfWeek(today).toEpochDay()),
            ),
            settings = Settings(theme = ThemeMode.DARK),
        )
    }

    fun run(outDir: File) {
        outDir.mkdirs()
        val tmp = java.nio.file.Files.createTempDirectory("flow-shots").toFile()
        System.setProperty("flow.data.dir", tmp.absolutePath)
        val runtime = DesktopRuntime()
        runtime.repository.update { demoData() }
        val shots = buildList {
            val sizes = listOf(Triple(1366, 768, 1f), Triple(1600, 900, 1f), Triple(1920, 1080, 1f), Triple(2560, 1440, 1f), Triple(3840, 2160, 1.5f))
            for (dark in listOf(false, true)) {
                val t = if (dark) "dark" else "light"
                for ((w, h, s) in sizes) add(Shot("home-$t-${w}x$h", w, h, s, Page.Home, dark))
                for (p in Page.entries) for ((w, h) in listOf(1366 to 768, 1920 to 1080)) add(Shot("${p.name.lowercase()}-$t-${w}x$h", w, h, 1f, p, dark))
                for (q in GlassQuality.entries) add(Shot("quality-${q.name.lowercase()}-$t", 1600, 900, 1f, Page.Home, dark, q))
            }
        }
        var failed = 0
        for (shot in shots) {
            try {
                runtime.updateSettings { it.copy(theme = if (shot.dark) ThemeMode.DARK else ThemeMode.LIGHT, glassQuality = shot.quality) }
                val backdrop = BackdropState()
                val scene = ImageComposeScene(shot.w, shot.h, Density(shot.scale)) {
                    FlowApp(runtime, null, shot.page, shot.compact, backdrop)
                }
                var image: org.jetbrains.skia.Image? = null
                // Several frames: lets the backdrop bake (background thread) and entry animations settle.
                for (i in 0 until 70) {
                    image?.close()
                    image = scene.render(i * 33_000_000L)
                    if (i < 40) Thread.sleep(40)
                }
                val data = image!!.encodeToData(EncodedImageFormat.PNG)!!
                File(outDir, shot.name + ".png").writeBytes(data.bytes)
                scene.close()
            } catch (t: Throwable) {
                failed++
                File(outDir, shot.name + ".error.txt").writeText(t.stackTraceToString())
            }
        }
        runtime.close()
        File(outDir, "summary.txt").writeText("shots=${shots.size} failed=$failed\n")
        if (failed > 0) System.exit(2)
    }
}
