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
        val migratedSettings = if (data.settings.ambientSound in supportedAmbientIds) {
            data.settings
        } else {
            data.settings.copy(ambientSound = "none")
        }
        // New fields on Settings are decoded via defaults; preserve all user-owned collections and timer state.
        return data.copy(
            version = maxOf(data.version, 2),
            settings = migratedSettings,
        )
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
