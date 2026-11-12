package com.flow.focus.platform

import com.flow.focus.data.AppData
import com.flow.focus.data.FlowRepository
import com.flow.focus.data.FocusSession
import com.flow.focus.data.PhaseType
import com.flow.focus.data.TimerState
import com.flow.focus.data.TimerStatus
import com.flow.focus.domain.PomodoroEngine

/**
 * پل بین موتور خالص و Android: وضعیت را در مخزن ذخیره می‌کند، Alarm دقیق می‌گذارد،
 * اعلان و صدای محیط را همگام می‌کند. وضعیت پایدار است، پس بعد از بسته شدن برنامه هم درست می‌ماند.
 */
class TimerController(
    private val repo: FlowRepository,
    private val alarms: AlarmScheduler,
    private val notifier: NotificationHelper,
    private val ambient: AmbientPlayer,
) {
    private class Outcome(val finished: List<PhaseType>)

    @Volatile private var ambientByTimer = false

    private fun transact(block: (AppData, Long) -> Triple<TimerState, List<FocusSession>, List<PhaseType>>) {
        val now = System.currentTimeMillis()
        val outcome = repo.mutate { d ->
            val (timer, sessions, finished) = block(d, now)
            d.copy(timer = timer, sessions = d.sessions + sessions) to Outcome(finished)
        }
        applyEffects(outcome.finished)
    }

    fun start() = transact { d, now ->
        // اگر زمان فاز گذشته (مثلاً برنامه مدت‌ها بسته بوده) ابتدا فازهای تمام‌شده بسته می‌شوند.
        val (t, ss) = PomodoroEngine.catchUp(d.timer, d.settings, now)
        Triple(PomodoroEngine.start(t, now), ss, ss.map { it.type })
    }

    fun pause() = transact { d, now -> Triple(PomodoroEngine.pause(d.timer, now), emptyList(), emptyList()) }

    fun skip() = transact { d, now ->
        val r = PomodoroEngine.skip(d.timer, d.settings, now)
        Triple(r.state, listOfNotNull(r.session), emptyList())
    }

    fun reset() = transact { d, now ->
        val r = PomodoroEngine.reset(d.timer, d.settings, now)
        Triple(r.state, listOfNotNull(r.session), emptyList())
    }

    fun selectPhase(phase: PhaseType) = transact { d, now ->
        if (d.timer.status == TimerStatus.RUNNING || d.timer.phase == phase) {
            Triple(d.timer, emptyList(), emptyList())
        } else {
            val r = PomodoroEngine.switchPhase(d.timer, phase, d.settings, now)
            Triple(r.state, listOfNotNull(r.session), emptyList())
        }
    }

    fun selectTask(taskId: String?) = transact { d, _ -> Triple(d.timer.copy(taskId = taskId), emptyList(), emptyList()) }

    /** پس از تغییر تنظیمات یا بازیابی نسخه‌ی پشتیبان: تایمرِ بیکار با مدت‌های جدید هماهنگ می‌شود. */
    fun syncIdle() = transact { d, _ ->
        val t = d.timer
        val next = if (t.status == TimerStatus.IDLE) PomodoroEngine.idle(t.phase, d.settings, t.round, t.taskId) else t
        Triple(next, emptyList(), emptyList())
    }

    /** از Alarm، یا هنگام باز شدن برنامه / پایان زمان در پیش‌زمینه صدا زده می‌شود؛ تکرارش بی‌اثر است. */
    fun catchUp() = transact { d, now ->
        val t = d.timer
        // Alarm ممکن است چند میلی‌ثانیه زودتر برسد؛ در این حالت دقیقاً لحظه‌ی پایان در نظر گرفته می‌شود.
        val eff = if (t.status == TimerStatus.RUNNING && t.endAtMs - now in 1..2000) t.endAtMs else now
        val (state, ss) = PomodoroEngine.catchUp(t, d.settings, eff)
        Triple(state, ss, ss.map { it.type })
    }

    fun onAlarm() = catchUp()

    /** بازتاب تغییر تنظیمات (اعلان، صدا) روی تایمر در حال اجرا */
    fun refresh() = transact { d, _ -> Triple(d.timer, emptyList(), emptyList()) }

    /** هنگام راه‌اندازی برنامه یا بوت دستگاه */
    fun restore() = catchUp()

    private fun taskTitle(d: AppData): String? = d.timer.taskId?.let { id -> d.tasks.firstOrNull { it.id == id }?.title }

    private fun applyEffects(finished: List<PhaseType>) {
        val d = repo.current
        val t = d.timer
        val s = d.settings
        when (t.status) {
            TimerStatus.RUNNING -> {
                alarms.scheduleTimer(t.endAtMs)
                notifier.showRunning(t, taskTitle(d), s.notifications)
            }
            TimerStatus.PAUSED -> {
                alarms.cancelTimer()
                notifier.showPaused(t, taskTitle(d), s.notifications)
            }
            TimerStatus.IDLE -> {
                alarms.cancelTimer()
                notifier.cancelRunning()
            }
        }
        val shouldPlay = t.status == TimerStatus.RUNNING && t.phase == PhaseType.FOCUS &&
            s.ambientAuto && s.ambientSound != AmbientSynth.NONE
        if (shouldPlay) {
            ambient.play(s.ambientSound, s.ambientVolume)
            ambientByTimer = true
        } else if (ambientByTimer) {
            // فقط صدایی را خاموش می‌کنیم که خودِ تایمر روشن کرده؛ پیش‌شنوایی تنظیمات دست‌نخورده می‌ماند.
            ambient.stop()
            ambientByTimer = false
        }
        if (finished.isNotEmpty()) notifier.showFinished(finished.last(), t.phase, s)
    }
}
