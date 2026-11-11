package com.flow.focus

import com.flow.focus.data.AppData
import com.flow.focus.data.FlowRepository
import com.flow.focus.data.JsonStore
import com.flow.focus.data.Routine
import com.flow.focus.data.Settings
import com.flow.focus.data.Task
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedDataSanitizationTest {
    @Test
    fun importedSettingsAndScheduleValuesAreBoundedWithoutLosingValidContent() {
        val directory = createTempDir(prefix = "matfocus-import-sanitize-")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val file = File(directory, "flow_data.json")
            val repository = FlowRepository(JsonStore(file), scope)
            val today = 20_000L
            val data = AppData(
                version = 2,
                tasks = listOf(
                    Task(
                        id = "invalid-date",
                        title = "Malformed date",
                        notes = "keep notes",
                        dueDay = Long.MAX_VALUE,
                        dueMinute = Int.MAX_VALUE,
                        estimatedPomodoros = Int.MAX_VALUE,
                    ),
                    Task(
                        id = "valid-date",
                        title = "Valid schedule",
                        dueDay = today,
                        dueMinute = 9 * 60,
                        estimatedPomodoros = 3,
                    ),
                ),
                routines = listOf(
                    Routine(
                        id = "routine",
                        title = "Exercise",
                        weekdays = listOf(-1, 0, 3, 3, 8),
                        minute = 24 * 60,
                        pomodoros = Int.MAX_VALUE,
                    ),
                ),
                settings = Settings(
                    focusMin = Int.MAX_VALUE,
                    shortBreakMin = 0,
                    longBreakMin = -10,
                    rounds = Int.MAX_VALUE,
                    dailyGoalMin = Int.MAX_VALUE,
                    glassOpacity = 9f,
                    glassBlurDp = Int.MAX_VALUE,
                    glassShine = -2f,
                    ambientVolume = 3f,
                    musicVolume = -1f,
                    wallpaperDim = 2f,
                    wallpaperBlurDp = Int.MAX_VALUE,
                    ambientSound = "unknown-sound",
                    wallpaperId = "unknown-wallpaper",
                ),
            )
            val backup = Json { encodeDefaults = true }.encodeToString(data)

            assertTrue(repository.importJson(backup))

            val migrated = repository.current
            assertEquals("keep notes", migrated.tasks.first().notes)
            assertNull(migrated.tasks.first().dueDay)
            assertNull(migrated.tasks.first().dueMinute)
            assertEquals(20, migrated.tasks.first().estimatedPomodoros)
            assertEquals(today, migrated.tasks.last().dueDay)
            assertEquals(9 * 60, migrated.tasks.last().dueMinute)
            assertEquals(3, migrated.tasks.last().estimatedPomodoros)

            assertEquals(listOf(0, 3), migrated.routines.single().weekdays)
            assertNull(migrated.routines.single().minute)
            assertEquals(20, migrated.routines.single().pomodoros)

            val s = migrated.settings
            assertEquals(180, s.focusMin)
            assertEquals(1, s.shortBreakMin)
            assertEquals(1, s.longBreakMin)
            assertEquals(12, s.rounds)
            assertEquals(720, s.dailyGoalMin)
            assertEquals(1.6f, s.glassOpacity, 0f)
            assertEquals(48, s.glassBlurDp)
            assertEquals(0f, s.glassShine, 0f)
            assertEquals(1f, s.ambientVolume, 0f)
            assertEquals(0f, s.musicVolume, 0f)
            assertEquals(0.5f, s.wallpaperDim, 0f)
            assertEquals(24, s.wallpaperBlurDp)
            assertEquals("none", s.ambientSound)
            assertEquals("gradient", s.wallpaperId)
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }
}
