package com.flow.focus

import com.flow.focus.data.AppData
import com.flow.focus.data.FocusSession
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Settings
import com.flow.focus.data.Task
import com.flow.focus.data.TimerStatus
import com.flow.focus.domain.PomodoroEngine
import com.flow.focus.domain.Stats
import com.flow.focus.domain.StatsRange
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroEngineTest {
    private val s = Settings(focusMin = 25, shortBreakMin = 5, longBreakMin = 15, rounds = 2)
    private val t0 = 1_000_000_000_000L
    private val min = 60_000L

    @Test fun startPauseResumeKeepsRemaining() {
        var st = PomodoroEngine.idle(PhaseType.FOCUS, s, 1, null)
        st = PomodoroEngine.start(st, t0)
        assertEquals(TimerStatus.RUNNING, st.status)
        assertEquals(25 * min, PomodoroEngine.remaining(st, t0))
        st = PomodoroEngine.pause(st, t0 + 10 * min)
        assertEquals(TimerStatus.PAUSED, st.status)
        assertEquals(15 * min, st.remainingMs)
        // مدت توقف روی زمان باقی‌مانده اثر ندارد
        assertEquals(15 * min, PomodoroEngine.remaining(st, t0 + 500 * min))
        st = PomodoroEngine.start(st, t0 + 500 * min)
        assertEquals(15 * min, PomodoroEngine.remaining(st, t0 + 500 * min))
        assertEquals(10 * min, PomodoroEngine.remaining(st, t0 + 505 * min))
    }

    @Test fun remainingIsBasedOnWallClockNotCounter() {
        var st = PomodoroEngine.idle(PhaseType.FOCUS, s, 1, null)
        st = PomodoroEngine.start(st, t0)
        // برنامه ۲۰ دقیقه بسته بوده؛ باقی‌مانده هنوز درست محاسبه می‌شود
        assertEquals(5 * min, PomodoroEngine.remaining(st, t0 + 20 * min))
        assertEquals(0L, PomodoroEngine.remaining(st, t0 + 99 * min))
    }

    @Test fun naturalFinishLogsSessionAndMovesToBreak() {
        var st = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, s, 1, "task1"), t0)
        val r = PomodoroEngine.finishNatural(st, s)
        assertNotNull(r.session)
        assertTrue(r.session!!.completed)
        assertEquals("task1", r.session!!.taskId)
        assertEquals(25 * min, r.session!!.actualMs)
        assertEquals(PhaseType.SHORT_BREAK, r.state.phase)
        assertEquals(TimerStatus.IDLE, r.state.status)
        assertEquals(5 * min, r.state.remainingMs)
    }

    @Test fun longBreakAfterLastRoundThenCycleResets() {
        var st = PomodoroEngine.idle(PhaseType.FOCUS, s, 2, null)
        st = PomodoroEngine.start(st, t0)
        var r = PomodoroEngine.finishNatural(st, s)
        assertEquals(PhaseType.LONG_BREAK, r.state.phase)
        st = PomodoroEngine.start(r.state, t0 + 25 * min)
        r = PomodoroEngine.finishNatural(st, s)
        assertEquals(PhaseType.FOCUS, r.state.phase)
        assertEquals(1, r.state.round)
    }

    @Test fun shortBreakAdvancesRound() {
        var st = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, s, 1, null), t0)
        var r = PomodoroEngine.finishNatural(st, s)
        st = PomodoroEngine.start(r.state, t0)
        r = PomodoroEngine.finishNatural(st, s)
        assertEquals(PhaseType.FOCUS, r.state.phase)
        assertEquals(2, r.state.round)
    }

    @Test fun autoStartChainsAndCatchUpHandlesLongAbsence() {
        val auto = s.copy(autoStartBreak = true, autoStartFocus = true)
        val st = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, auto, 1, null), t0)
        // ۶۰ دقیقه بعد: تمرکز(۲۵) + استراحت(۵) + تمرکز(۲۵) تمام شده و ۵ دقیقه از استراحت بلند گذشته
        val (state, sessions) = PomodoroEngine.catchUp(st, auto, t0 + 60 * min)
        assertEquals(3, sessions.size)
        assertEquals(PhaseType.LONG_BREAK, state.phase)
        assertEquals(TimerStatus.RUNNING, state.status)
        assertEquals(10 * min, PomodoroEngine.remaining(state, t0 + 60 * min))
        // جلسه‌ها پشت سر هم و بدون شکاف ثبت می‌شوند
        assertEquals(sessions[0].endedAt, sessions[1].startedAt)
    }

    @Test fun catchUpWithoutAutoStopsAtIdle() {
        val st = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, s, 1, null), t0)
        val (state, sessions) = PomodoroEngine.catchUp(st, s, t0 + 300 * min)
        assertEquals(1, sessions.size)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(PhaseType.SHORT_BREAK, state.phase)
    }

    @Test fun skipFocusRecordsPartialOnlyIfOverOneMinute() {
        var st = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, s, 1, null), t0)
        assertNull(PomodoroEngine.skip(st, s, t0 + 30_000L).session)
        val r = PomodoroEngine.skip(st, s, t0 + 10 * min)
        assertNotNull(r.session)
        assertEquals(false, r.session!!.completed)
        assertEquals(10 * min, r.session!!.actualMs)
        assertEquals(PhaseType.SHORT_BREAK, r.state.phase)
        // رد کردن استراحت چیزی ثبت نمی‌کند
        val br = PomodoroEngine.start(r.state, t0)
        assertNull(PomodoroEngine.skip(br, s, t0 + 2 * min).session)
    }

    @Test fun resetReturnsToIdleSamePhase() {
        val st = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, s, 1, null), t0)
        val r = PomodoroEngine.reset(st, s, t0 + 5 * min)
        assertEquals(TimerStatus.IDLE, r.state.status)
        assertEquals(25 * min, r.state.remainingMs)
        assertEquals(5 * min, r.session!!.actualMs)
    }

    @Test fun progressRange() {
        val st = PomodoroEngine.start(PomodoroEngine.idle(PhaseType.FOCUS, s, 1, null), t0)
        assertEquals(0f, PomodoroEngine.progress(st, t0), 0.0001f)
        assertEquals(0.5f, PomodoroEngine.progress(st, t0 + (25 * min) / 2), 0.0001f)
        assertEquals(1f, PomodoroEngine.progress(st, t0 + 900 * min), 0.0001f)
    }

    @Test fun statsHaveNoInventedData() {
        val empty = Stats.summarize(AppData(), StatsRange.WEEK, LocalDate.of(2026, 11, 10))
        assertEquals(0L, empty.focusMinutes)
        assertEquals(0, empty.completedSessions)
        assertTrue(empty.buckets.all { it.minutes == 0L })
        assertTrue(empty.categories.isEmpty())
    }

    @Test fun statsAggregateRealSessions() {
        val zone = ZoneId.of("Asia/Tehran")
        val today = LocalDate.of(2026, 11, 10)
        val noon = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        fun sess(id: String, endMs: Long, minutes: Long, done: Boolean, task: String?) = FocusSession(
            id, task, PhaseType.FOCUS, endMs - minutes * min, endMs, minutes * min, 25 * min, done,
        )
        val data = AppData(
            tasks = listOf(Task("a", "درس", categoryId = "study", done = true, doneAt = noon)),
            sessions = listOf(
                sess("1", noon, 25, true, "a"),
                sess("2", noon + 40 * min, 25, true, "a"),
                sess("3", noon - 24 * 60 * min, 10, false, null),
                FocusSession("b", null, PhaseType.SHORT_BREAK, noon, noon + 5 * min, 5 * min, 5 * min, true),
            ),
        )
        val w = Stats.summarize(data, StatsRange.WEEK, today, zone)
        assertEquals(60L, w.focusMinutes)
        assertEquals(2, w.completedSessions)
        assertEquals(25L, w.avgSessionMinutes)
        assertEquals(2, w.activeDays)
        assertEquals(1, w.tasksDone)
        assertEquals(50L, Stats.focusMinutesOn(data.sessions, today, zone))
        assertEquals("study", w.categories.first().categoryId)
        assertEquals(50L, w.categories.first().minutes)
        assertEquals(2, Stats.pomodorosDone(data.sessions, "a"))
    }

    @Test fun dataRoundTripsThroughJson() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val d = AppData(tasks = listOf(Task("x", "عنوان فارسی", notes = "یادداشت")))
        val back = json.decodeFromString<AppData>(json.encodeToString(d))
        assertEquals(d, back)
    }
}
