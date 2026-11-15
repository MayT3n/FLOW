package com.flow.desktop

import com.flow.desktop.design.FlowFonts
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Priority
import com.flow.focus.data.ThemeMode
import com.flow.focus.data.TimerStatus
import java.io.File
import java.time.LocalDate

/**
 * Non-visual verification of the installed application, run by CI against the MSI-installed exe:
 * `MedFlow.exe --selftest <report-file>`. Uses an isolated data directory, never the user's data.
 * Checks bundled Persian fonts, persistence round trip, theme + glass-quality settings, timer logic and playlists.
 */
internal object SelfTest {
    fun run(reportPath: String?): Int {
        val lines = mutableListOf<String>()
        var failures = 0
        fun check(name: String, block: () -> Boolean) {
            val ok = runCatching(block).getOrElse { lines += "  error: ${it.message}"; false }
            lines += (if (ok) "PASS " else "FAIL ") + name
            if (!ok) failures++
        }
        val dir = java.nio.file.Files.createTempDirectory("flow-selftest").toFile()
        System.setProperty("flow.data.dir", dir.absolutePath)

        check("Vazirmatn fonts are bundled (4 weights)") {
            FlowFonts.resourceNames.all { name ->
                (SelfTest::class.java.classLoader.getResourceAsStream(name)?.use { it.readBytes().size } ?: 0) > 50_000
            }
        }
        var runtime = DesktopRuntime()
        check("add task and persist to disk") {
            runtime.addTask("وظیفه‌ی آزمایشی", "یادداشت", LocalDate.now(), 9 * 60, Priority.HIGH)
            runtime.repository.flush()
            File(dir, "flow_data.json").let { it.isFile && it.readText().contains("وظیفه‌ی آزمایشی") }
        }
        check("theme and glass quality settings persist") {
            runtime.updateSettings { it.copy(theme = ThemeMode.LIGHT, glassQuality = GlassQuality.ULTRA) }
            runtime.repository.flush(); runtime.close()
            runtime = DesktopRuntime()
            val s = runtime.repository.current.settings
            s.theme == ThemeMode.LIGHT && s.glassQuality == GlassQuality.ULTRA && runtime.repository.current.tasks.size == 1
        }
        check("timer start / pause / reset") {
            runtime.start()
            val running = runtime.repository.current.timer.status == TimerStatus.RUNNING
            runtime.pause()
            val paused = runtime.repository.current.timer.status == TimerStatus.PAUSED
            runtime.reset()
            running && paused && runtime.repository.current.timer.status == TimerStatus.IDLE
        }
        check("phase switch keeps data") {
            runtime.selectPhase(PhaseType.SHORT_BREAK)
            runtime.repository.current.timer.phase == PhaseType.SHORT_BREAK && runtime.repository.current.tasks.size == 1
        }
        check("backup export / import round trip") {
            val json = runtime.exportJson()
            runtime.deleteTask(runtime.repository.current.tasks.first().id)
            val ok = runtime.importJson(json)
            ok && runtime.repository.current.tasks.size == 1
        }
        check("music playlist create / persist") {
            val id = runtime.music.createPlaylist("آزمایش")
            id != null && runtime.music.playlists.value.any { it.name == "آزمایش" }
        }
        check("music catalogue and audio engine initialised") { DesktopMusicCatalog.tracks.size >= 10 }
        runtime.close()

        val report = lines.joinToString("\n") + "\nfailures=$failures\n"
        reportPath?.let { runCatching { File(it).writeText(report) } }
        println(report)
        return if (failures == 0) 0 else 1
    }
}
