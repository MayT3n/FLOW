package com.flow.focus.domain

import com.flow.focus.data.FocusSession
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Settings
import com.flow.focus.data.TimerState
import com.flow.focus.data.TimerStatus
import java.util.UUID

/**
 * موتور خالص (بدون وابستگی به Android) پومودورو. همه‌ی زمان‌ها بر پایه‌ی زمان دیواری هستند،
 * پس محاسبه‌ی زمان به شمارنده‌ی رابط کاربری وابسته نیست و بعد از بستن برنامه نیز درست می‌ماند.
 */
object PomodoroEngine {

    data class Result(val state: TimerState, val session: FocusSession? = null, val naturalFinish: Boolean = false)

    fun idle(phase: PhaseType, settings: Settings, round: Int, taskId: String?): TimerState {
        val d = settings.durationMs(phase)
        return TimerState(
            phase = phase, status = TimerStatus.IDLE, durationMs = d, remainingMs = d,
            round = round.coerceIn(1, settings.rounds.coerceAtLeast(1)), taskId = taskId,
        )
    }

    fun remaining(s: TimerState, now: Long): Long = when (s.status) {
        TimerStatus.RUNNING -> (s.endAtMs - now).coerceAtLeast(0)
        else -> s.remainingMs
    }

    fun elapsed(s: TimerState, now: Long): Long = (s.durationMs - remaining(s, now)).coerceIn(0, s.durationMs)

    /** نسبت پیشرفت ۰..۱ */
    fun progress(s: TimerState, now: Long): Float =
        if (s.durationMs <= 0) 0f else (elapsed(s, now).toFloat() / s.durationMs).coerceIn(0f, 1f)

    fun start(s: TimerState, now: Long): TimerState = when (s.status) {
        TimerStatus.RUNNING -> s
        TimerStatus.IDLE -> s.copy(status = TimerStatus.RUNNING, endAtMs = now + s.remainingMs, startedAtMs = now)
        TimerStatus.PAUSED -> s.copy(status = TimerStatus.RUNNING, endAtMs = now + s.remainingMs)
    }

    fun pause(s: TimerState, now: Long): TimerState =
        if (s.status != TimerStatus.RUNNING) s
        else s.copy(status = TimerStatus.PAUSED, remainingMs = remaining(s, now), endAtMs = 0L)

    /**
     * بازنشانی: فاز فعلی از اول (IDLE). اگر بخشی از تمرکز گذشته باشد (≥ ۱ دقیقه) به‌صورت جلسه‌ی ناقص ثبت می‌شود.
     */
    fun reset(s: TimerState, settings: Settings, now: Long): Result {
        val partial = partialSession(s, now)
        return Result(idle(s.phase, settings, s.round, s.taskId), partial)
    }

    /** انتخاب فاز دلخواه (مثلاً کاربر روی «استراحت کوتاه» می‌زند). */
    fun switchPhase(s: TimerState, phase: PhaseType, settings: Settings, now: Long): Result {
        val partial = partialSession(s, now)
        return Result(idle(phase, settings, s.round, s.taskId), partial)
    }

    /** رد کردن جلسه‌ی فعلی و رفتن به فاز بعدی. */
    fun skip(s: TimerState, settings: Settings, now: Long): Result =
        // فقط تمرکزِ ناقص ثبت می‌شود؛ رد کردن استراحت چیزی ثبت نمی‌کند.
        Result(next(s, settings, now, autoStart = false, startAt = now), partialSession(s, now))

    /**
     * پایان طبیعی فاز در لحظه‌ی endAtMs. جلسه ثبت می‌شود و فاز بعد ساخته می‌شود
     * (و در صورت فعال بودن شروع خودکار، بلافاصله از لحظه‌ی endAtMs اجرا می‌شود).
     */
    fun finishNatural(s: TimerState, settings: Settings): Result {
        val end = s.endAtMs
        val session = FocusSession(
            id = UUID.randomUUID().toString(), taskId = s.taskId, type = s.phase,
            startedAt = s.startedAtMs.takeIf { it > 0 } ?: (end - s.durationMs), endedAt = end,
            actualMs = s.durationMs, plannedMs = s.durationMs, completed = true,
        )
        val auto = if (s.phase == PhaseType.FOCUS) settings.autoStartBreak else settings.autoStartFocus
        return Result(next(s, settings, end, auto, end), session, naturalFinish = true)
    }

    /**
     * اگر تایمر در حال اجرا و زمانش گذشته باشد، همه‌ی فازهای تمام‌شده را به ترتیب می‌بندد
     * (برای زمانی که برنامه مدتی بسته بوده و شروع خودکار فعال است).
     */
    fun catchUp(s0: TimerState, settings: Settings, now: Long): Pair<TimerState, List<FocusSession>> {
        var s = s0
        val sessions = ArrayList<FocusSession>()
        var guard = 0
        while (s.status == TimerStatus.RUNNING && s.endAtMs <= now && guard < 50) {
            val r = finishNatural(s, settings)
            r.session?.let { sessions.add(it) }
            s = r.state
            guard++
        }
        return s to sessions
    }

    fun phaseAfter(s: TimerState, settings: Settings): PhaseType = when (s.phase) {
        PhaseType.FOCUS -> if (s.round >= settings.rounds) PhaseType.LONG_BREAK else PhaseType.SHORT_BREAK
        else -> PhaseType.FOCUS
    }

    private fun next(s: TimerState, settings: Settings, now: Long, autoStart: Boolean, startAt: Long): TimerState {
        val nextPhase = phaseAfter(s, settings)
        val nextRound = when (s.phase) {
            PhaseType.FOCUS -> s.round
            PhaseType.SHORT_BREAK -> (s.round + 1).coerceAtMost(settings.rounds)
            PhaseType.LONG_BREAK -> 1
        }
        val base = idle(nextPhase, settings, nextRound, s.taskId)
        return if (autoStart) {
            base.copy(status = TimerStatus.RUNNING, startedAtMs = startAt, endAtMs = startAt + base.durationMs)
        } else base
    }

    private fun partialSession(s: TimerState, now: Long): FocusSession? {
        if (s.phase != PhaseType.FOCUS || s.status == TimerStatus.IDLE) return null
        val e = elapsed(s, now)
        if (e < 60_000L) return null
        return FocusSession(
            id = UUID.randomUUID().toString(), taskId = s.taskId, type = s.phase,
            startedAt = s.startedAtMs.takeIf { it > 0 } ?: (now - e), endedAt = now,
            actualMs = e, plannedMs = s.durationMs, completed = false,
        )
    }
}
