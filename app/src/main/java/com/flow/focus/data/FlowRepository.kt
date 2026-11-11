package com.flow.focus.data

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * منبع یگانه‌ی حقیقت برای داده‌ها. تغییرات هم‌زمان (UI، Alarm، Receiver) با قفل ترتیب می‌گیرند
 * و ذخیره‌سازی روی دیسک در پس‌زمینه (با ادغام درخواست‌ها) انجام می‌شود.
 */
class FlowRepository(private val store: JsonStore, scope: CoroutineScope) {
    private val lock = Any()
    private val loadedData = store.load()
    private val initialData = migrateLegacySettings(loadedData)
    private val _data = MutableStateFlow(initialData)
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val saveSignal = Channel<Unit>(Channel.CONFLATED)

    init {
        // Keep all existing tasks/sessions/timer/settings; only retire ambient IDs that no longer exist.
        if (initialData != loadedData) saveSignal.trySend(Unit)
        scope.launch(Dispatchers.IO) {
            for (ignored in saveSignal) {
                runCatching { store.save(_data.value) }
            }
        }
    }

    val current: AppData get() = _data.value

    fun update(block: (AppData) -> AppData) {
        synchronized(lock) { _data.value = block(_data.value) }
        saveSignal.trySend(Unit)
    }

    /** تغییر اتمیک که نتیجه هم برمی‌گرداند (برای انتقال‌های تایمر). */
    fun <R> mutate(block: (AppData) -> Pair<AppData, R>): R {
        val result: R
        synchronized(lock) {
            val (next, r) = block(_data.value)
            _data.value = next
            result = r
        }
        saveSignal.trySend(Unit)
        return result
    }

    /** ذخیره‌ی هم‌زمان؛ برای Receiverها که ممکن است بلافاصله بعد از کار پروسه بسته شود. */
    fun flush() {
        runCatching { store.save(_data.value) }
    }

    // ---- وظایف ----
    fun upsertTask(task: Task) = update { d ->
        val exists = d.tasks.any { it.id == task.id }
        d.copy(tasks = if (exists) d.tasks.map { if (it.id == task.id) task else it } else d.tasks + task)
    }

    fun deleteTask(id: String) = update { d ->
        d.copy(
            tasks = d.tasks.filterNot { it.id == id },
            sessions = d.sessions.map { if (it.taskId == id) it.copy(taskId = null) else it },
            timer = if (d.timer.taskId == id) d.timer.copy(taskId = null) else d.timer,
        )
    }

    fun setTaskDone(id: String, done: Boolean, now: Long) = update { d ->
        d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(done = done, doneAt = if (done) now else null) else it })
    }

    // ---- روتین‌ها ----
    fun upsertRoutine(r: Routine) = update { d ->
        val exists = d.routines.any { it.id == r.id }
        d.copy(routines = if (exists) d.routines.map { if (it.id == r.id) r else it } else d.routines + r)
    }

    fun deleteRoutine(id: String) = update { d ->
        d.copy(routines = d.routines.filterNot { it.id == id }, routineChecks = d.routineChecks.filterNot { it.startsWith("$id|") })
    }

    fun toggleRoutineCheck(routineId: String, epochDay: Long) = update { d ->
        val key = "$routineId|$epochDay"
        d.copy(routineChecks = if (key in d.routineChecks) d.routineChecks - key else d.routineChecks + key)
    }

    // ---- اهداف ----
    fun addGoal(goal: Goal) = update { it.copy(goals = it.goals + goal) }
    fun toggleGoal(id: String) = update { d -> d.copy(goals = d.goals.map { if (it.id == id) it.copy(done = !it.done) else it }) }
    fun deleteGoal(id: String) = update { d -> d.copy(goals = d.goals.filterNot { it.id == id }) }

    // ---- تنظیمات ----
    fun updateSettings(block: (Settings) -> Settings) = update { it.copy(settings = block(it.settings)) }

    // ---- داده‌ها ----
    fun clearHistory() = update { it.copy(sessions = emptyList()) }

    fun exportJson(): String = store.encode(_data.value)

    /** بازیابی نسخه‌ی پشتیبان؛ در صورت معتبر نبودن فایل false برمی‌گرداند و چیزی تغییر نمی‌کند. */
    fun importJson(text: String): Boolean {
        val parsed = store.decodeOrNull(text) ?: return false
        update { migrateLegacySettings(parsed).copy(timer = TimerState()) }
        return true
    }

    private fun migrateLegacySettings(data: AppData): AppData {
        val supportedAmbientIds = setOf("none", "rain", "forest", "wind", "waves", "fire", "shore")
        val settings = data.settings
        fun bounded(value: Float, range: ClosedFloatingPointRange<Float>, fallback: Float): Float =
            if (value.isFinite()) value.coerceIn(range) else fallback

        val migratedSettings = settings.copy(
            focusMin = settings.focusMin.coerceIn(1, 180),
            shortBreakMin = settings.shortBreakMin.coerceIn(1, 60),
            longBreakMin = settings.longBreakMin.coerceIn(1, 90),
            rounds = settings.rounds.coerceIn(1, 12),
            dailyGoalMin = settings.dailyGoalMin.coerceIn(10, 720),
            glassOpacity = bounded(settings.glassOpacity, 0.4f..1.6f, 1f),
            glassBlurDp = settings.glassBlurDp.coerceIn(4, 48),
            glassShine = bounded(settings.glassShine, 0f..1f, 0.7f),
            ambientVolume = bounded(settings.ambientVolume, 0f..1f, 0.5f),
            musicVolume = bounded(settings.musicVolume, 0f..1f, 0.8f),
            wallpaperDim = bounded(settings.wallpaperDim, 0f..0.5f, 0.18f),
            wallpaperBlurDp = settings.wallpaperBlurDp.coerceIn(0, 24),
            ambientSound = settings.ambientSound.takeIf { it in supportedAmbientIds } ?: "none",
            wallpaperId = settings.wallpaperId.takeIf { it in setOf("gradient", "aurora", "midnight", "dusk", "custom") } ?: "gradient",
        )
        val migratedTasks = data.tasks.map { task ->
            val validDay = task.dueDay?.takeIf { epoch ->
                runCatching { java.time.LocalDate.ofEpochDay(epoch) }.isSuccess
            }
            task.copy(
                dueDay = validDay,
                dueMinute = validDay?.let { task.dueMinute?.takeIf { minute -> minute in 0 until 24 * 60 } },
                estimatedPomodoros = task.estimatedPomodoros.coerceIn(1, 20),
            )
        }
        val migratedRoutines = data.routines.map { routine ->
            routine.copy(
                weekdays = routine.weekdays.filter { it in 0..6 }.distinct().sorted(),
                minute = routine.minute?.takeIf { it in 0 until 24 * 60 },
                pomodoros = routine.pomodoros.coerceIn(1, 20),
            )
        }
        // Preserve IDs, notes, sessions, checks, goals and active timer while normalizing values
        // that could otherwise crash date conversion or create unbounded UI loops/effects.
        return data.copy(
            version = maxOf(data.version, 2),
            tasks = migratedTasks,
            routines = migratedRoutines,
            settings = migratedSettings,
        )
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
