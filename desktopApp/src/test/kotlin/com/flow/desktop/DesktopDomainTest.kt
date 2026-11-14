package com.flow.desktop

import com.flow.focus.core.JalaliDate
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Settings
import com.flow.focus.data.TimerStatus
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
    fun jalaliWeekStartsOnSaturday() {
        assertEquals(0, JalaliDate.weekdayIndex(LocalDate.of(2026, 11, 7)))
        assertEquals(LocalDate.of(2026, 11, 7), JalaliDate.startOfWeek(LocalDate.of(2026, 11, 10)))
    }
}
