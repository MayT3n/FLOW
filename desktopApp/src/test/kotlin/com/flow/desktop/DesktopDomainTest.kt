package com.flow.desktop

import com.flow.focus.core.JalaliDate
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Settings
import com.flow.focus.data.TimerStatus
import com.flow.focus.data.AppData
import com.flow.focus.data.Task
import com.flow.focus.data.JsonStore
import com.flow.focus.data.FlowRepository
import com.flow.focus.domain.Planner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.nio.file.Files
import com.flow.focus.domain.PomodoroEngine
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopDomainTest {
    @Test
    fun timerTransitionsPreserveRemainingTime() {
        val settings = Settings(focusMin = 25, shortBreakMin = 5, rounds = 2)
        val start = 1_000_000_000_000L
        val running = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, settings, 1, null), start)
        val paused = PomodoroEngine.pause(running, start + 10 * 60_000L)
        assertEquals(TimerStatus.PAUSED, paused.status)
        assertEquals(15 * 60_000L, paused.remainingMs)
        val resumed = PomodoroEngine.start(paused, start + 60 * 60_000L)
        assertEquals(15 * 60_000L, PomodoroEngine.remaining(resumed, start + 60 * 60_000L))
    }

    @Test
    fun naturalFocusFinishCreatesSessionAndBreak() {
        val settings = Settings(focusMin = 25, shortBreakMin = 5, rounds = 2)
        val start = 1_000_000_000_000L
        val running = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, settings, 1, "task"), start)
        val result = PomodoroEngine.finishNatural(running, settings)
        assertTrue(result.session?.completed == true)
        assertEquals(PhaseType.SHORT_BREAK, result.state.phase)
        assertEquals(5 * 60_000L, result.state.remainingMs)
    }

    @Test
    fun jalaliConversionRoundTripsAcrossFortyYears() {
        var date = LocalDate.of(1990, 1, 1)
        val end = LocalDate.of(2030, 12, 31)
        while (!date.isAfter(end)) {
            assertEquals(date, JalaliDate.of(date).toLocalDate())
            date = date.plusDays(1)
        }
    }

    @Test
    fun bundledIranianCalendarIsAvailableOfflineAndDatesAreValid() {
        val events = DesktopIranianCalendar.all()
        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it.isHoliday })
        assertTrue(events.any { it.titleFa.contains("نوروز") })
        events.forEach { event ->
            assertTrue(event.titleFa.isNotBlank())
            assertEquals(event.epochDay, LocalDate.ofEpochDay(event.epochDay).toEpochDay())
        }
    }

    @Test
    fun plannerIncludesDueTasksAndRecurringRoutinesOnTheCorrectJalaliWeekday() {
        val day = LocalDate.of(2026, 11, 7)
        val task = Task(id = "task", title = "تمرین", dueDay = day.toEpochDay())
        val routine = com.flow.focus.data.Routine(id = "routine", title = "مطالعه", weekdays = listOf(JalaliDate.weekdayIndex(day)))
        val entries = Planner.entriesFor(AppData(tasks = listOf(task), routines = listOf(routine)), day)
        assertEquals(2, entries.size)
    }

    @Test
    fun jsonBackupRoundTripsAndRejectsInvalidInputWithoutDataLoss() {
        val directory = Files.createTempDirectory("medflow-test").toFile()
        val file = java.io.File(directory, "data.json")
        val scope = CoroutineScope(SupervisorJob())
        try {
            val repo = FlowRepository(JsonStore(file), scope)
            repo.upsertTask(Task(id = "a", title = "پروژه", notes = "یادداشت"))
            repo.flush()
            val backup = repo.exportJson()
            assertTrue(repo.importJson(backup))
            assertEquals("پروژه", repo.current.tasks.single().title)
            assertEquals(false, repo.importJson("not-json"))
            assertEquals("پروژه", repo.current.tasks.single().title)
            repo.flush()
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            directory.deleteRecursively()
        }
    }


    @Test
    fun musicPlaylistsPersistAndRejectDuplicateTracks() {
        val directory = Files.createTempDirectory("medflow-playlists").toFile()
        try {
            val first = DesktopMusicPlayer(directory)
            val id = first.createPlaylist("Study Mix")
            assertTrue(id != null)
            first.addToPlaylist(id!!, "quiet-room")
            first.addToPlaylist(id, "quiet-room")
            assertEquals(listOf("quiet-room"), first.playlists.value.single().trackIds)
            first.close()

            val reopened = DesktopMusicPlayer(directory)
            assertEquals("Study Mix", reopened.playlists.value.single().name)
            assertEquals(listOf("quiet-room"), reopened.playlists.value.single().trackIds)
            reopened.close()
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun jalaliWeekStartsOnSaturday() {
        assertEquals(0, JalaliDate.weekdayIndex(LocalDate.of(2026, 11, 7)))
        assertEquals(LocalDate.of(2026, 11, 7), JalaliDate.startOfWeek(LocalDate.of(2026, 11, 10)))
    }
}
