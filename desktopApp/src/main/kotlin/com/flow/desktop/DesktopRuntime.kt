package com.flow.desktop

import com.flow.focus.data.AppData
import com.flow.focus.data.FlowRepository
import com.flow.focus.data.JsonStore
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Goal
import com.flow.focus.data.Routine
import com.flow.focus.data.Task
import com.flow.focus.data.Priority
import com.flow.focus.data.TimerStatus
import com.flow.focus.domain.PomodoroEngine
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop-owned lifecycle around the exact same persisted models and domain timer engine as Android.
 * The clock is only a UI refresh signal; timer truth remains endAtMs in the persisted state.
 */
class DesktopRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = JsonStore(dataFile())
    val repository = FlowRepository(store, scope)
    val music = DesktopMusicPlayer()
    val data = repository.data

    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                val nowMs = System.currentTimeMillis()
                val snapshot = repository.current
                if (snapshot.timer.status == TimerStatus.RUNNING && snapshot.timer.endAtMs <= nowMs) {
                    val (timer, sessions) = PomodoroEngine.catchUp(snapshot.timer, snapshot.settings, nowMs)
                    repository.update { current ->
                        current.copy(timer = timer, sessions = current.sessions + sessions)
                    }
                }
                _now.value = nowMs
                delay(250)
            }
        }
    }

    fun start() = repository.update { current ->
        current.copy(timer = PomodoroEngine.start(current.timer, System.currentTimeMillis()))
    }

    fun pause() = repository.update { current ->
        current.copy(timer = PomodoroEngine.pause(current.timer, System.currentTimeMillis()))
    }

    fun reset() = repository.update { current ->
        val result = PomodoroEngine.reset(current.timer, current.settings, System.currentTimeMillis())
        current.copy(timer = result.state, sessions = current.sessions + listOfNotNull(result.session))
    }

    fun skip() = repository.update { current ->
        val result = PomodoroEngine.skip(current.timer, current.settings, System.currentTimeMillis())
        current.copy(timer = result.state, sessions = current.sessions + listOfNotNull(result.session))
    }

    fun selectPhase(phase: PhaseType) = repository.update { current ->
        val result = PomodoroEngine.switchPhase(current.timer, phase, current.settings, System.currentTimeMillis())
        current.copy(timer = result.state, sessions = current.sessions + listOfNotNull(result.session))
    }

    fun addTask(
        title: String,
        notes: String = "",
        dueDate: LocalDate? = null,
        dueMinute: Int? = null,
        priority: Priority = Priority.MEDIUM,
        categoryId: String = "personal",
        estimatedPomodoros: Int = 1,
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val nowMs = System.currentTimeMillis()
        repository.upsertTask(Task(
            id = FlowRepository.newId(),
            title = clean,
            notes = notes.trim(),
            dueDay = dueDate?.toEpochDay(),
            dueMinute = dueMinute,
            priority = priority,
            categoryId = categoryId,
            estimatedPomodoros = estimatedPomodoros.coerceIn(1, 20),
            createdAt = nowMs
        ))
    }

    fun addRoutine(title: String, weekdays: List<Int>, minute: Int? = null, categoryId: String = "personal", pomodoros: Int = 1) {
        val clean = title.trim()
        if (clean.isEmpty() || weekdays.isEmpty()) return
        repository.upsertRoutine(Routine(
            id = FlowRepository.newId(), title = clean, weekdays = weekdays.distinct().filter { it in 0..6 },
            minute = minute, categoryId = categoryId, pomodoros = pomodoros.coerceIn(1, 20)
        ))
    }

    fun deleteRoutine(id: String) = repository.deleteRoutine(id)
    fun toggleRoutine(routineId: String, day: LocalDate) = repository.toggleRoutineCheck(routineId, day.toEpochDay())

    fun addGoal(title: String, period: com.flow.focus.data.GoalPeriod, startDay: Long) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        repository.addGoal(Goal(FlowRepository.newId(), clean, period, startDay))
    }

    fun toggleGoal(id: String) = repository.toggleGoal(id)
    fun deleteGoal(id: String) = repository.deleteGoal(id)

    fun saveTask(task: Task) = repository.upsertTask(task)

    fun setTaskDone(id: String, done: Boolean) =
        repository.setTaskDone(id, done, System.currentTimeMillis())

    fun deleteTask(id: String) = repository.deleteTask(id)

    fun updateSettings(block: (com.flow.focus.data.Settings) -> com.flow.focus.data.Settings) =
        repository.updateSettings(block)

    fun exportJson(): String = repository.exportJson()

    fun importJson(json: String): Boolean = repository.importJson(json)

    fun close() {
        repository.flush()
        music.close()
        scope.cancel()
    }

    private fun dataFile(): File {
        val root = System.getenv("APPDATA")
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?: File(System.getProperty("user.home"), "AppData/Roaming")
        val directory = File(root, "MedFlow")
        if (!directory.exists()) directory.mkdirs()
        return File(directory, "flow_data.json")
    }
}
