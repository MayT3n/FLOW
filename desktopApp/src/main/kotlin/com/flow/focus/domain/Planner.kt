package com.flow.focus.domain

import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.Priority
import com.flow.focus.data.Routine
import com.flow.focus.data.Task
import java.time.LocalDate
import java.time.LocalDateTime

/** یک ردیف در برنامه‌ی روز: وظیفه یا روتین تکرارشونده. */
sealed interface PlanEntry {
    val key: String
    val title: String
    val minute: Int?
    val categoryId: String
    val done: Boolean
}

data class TaskEntry(val task: Task) : PlanEntry {
    override val key get() = "t:${task.id}"
    override val title get() = task.title
    override val minute get() = task.dueMinute
    override val categoryId get() = task.categoryId
    override val done get() = task.done
}

data class RoutineEntry(val routine: Routine, val day: LocalDate, val checked: Boolean) : PlanEntry {
    override val key get() = "r:${routine.id}:${day.toEpochDay()}"
    override val title get() = routine.title
    override val minute get() = routine.minute
    override val categoryId get() = routine.categoryId
    override val done get() = checked
}

object Planner {
    private val order = compareBy<PlanEntry>({ it.minute ?: Int.MAX_VALUE }, { it.title })

    fun entriesFor(data: AppData, day: LocalDate): List<PlanEntry> {
        val epoch = day.toEpochDay()
        val wd = JalaliDate.weekdayIndex(day)
        val tasks = data.tasks.filter { it.dueDay == epoch }.map { TaskEntry(it) }
        val routines = data.routines.filter { wd in it.weekdays }
            .map { RoutineEntry(it, day, "${it.id}|$epoch" in data.routineChecks) }
        return (tasks + routines).sortedWith(order)
    }

    /** نزدیک‌ترین برنامه‌ی انجام‌نشده از همین لحظه به بعد (تا ۶۰ روز آینده). */
    fun next(data: AppData, now: LocalDateTime): Pair<PlanEntry, LocalDate>? {
        val today = now.toLocalDate()
        val nowMinute = now.hour * 60 + now.minute
        for (i in 0..60) {
            val day = today.plusDays(i.toLong())
            val hit = entriesFor(data, day).firstOrNull { e ->
                !e.done && !(i == 0 && e.minute != null && e.minute!! < nowMinute)
            }
            if (hit != null) return hit to day
        }
        return null
    }

    fun countOn(data: AppData, day: LocalDate): Int = entriesFor(data, day).size

    fun markedDays(data: AppData, from: LocalDate, to: LocalDate): Set<Long> {
        val out = HashSet<Long>()
        var d = from
        while (!d.isAfter(to)) {
            if (data.tasks.any { it.dueDay == d.toEpochDay() } ||
                data.routines.any { JalaliDate.weekdayIndex(d) in it.weekdays }
            ) out.add(d.toEpochDay())
            d = d.plusDays(1)
        }
        return out
    }
}

enum class TaskFilter { ALL, TODAY, UPCOMING, DONE }
enum class TaskSort { DUE, PRIORITY, NEWEST }

object TaskQuery {
    fun normalize(s: String): String = s.lowercase()
        .replace('ي', 'ی').replace('ك', 'ک')
        .replace("‌", " ")
        .let { t -> buildString { for (ch in t) append(if (ch in '۰'..'۹') '0' + (ch - '۰') else if (ch in '٠'..'٩') '0' + (ch - '٠') else ch) } }
        .trim()

    fun apply(
        tasks: List<Task>,
        filter: TaskFilter,
        sort: TaskSort,
        categoryId: String?,
        query: String,
        today: Long,
    ): List<Task> {
        val q = normalize(query)
        val filtered = tasks.filter { t ->
            val byFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.TODAY -> !t.done && t.dueDay != null && t.dueDay <= today
                TaskFilter.UPCOMING -> !t.done && t.dueDay != null && t.dueDay > today
                TaskFilter.DONE -> t.done
            }
            byFilter &&
                (categoryId == null || t.categoryId == categoryId) &&
                (q.isEmpty() || normalize(t.title).contains(q) || normalize(t.notes).contains(q))
        }
        val cmp: Comparator<Task> = when (sort) {
            TaskSort.DUE -> compareBy<Task>({ it.dueDay ?: Long.MAX_VALUE }, { it.dueMinute ?: Int.MAX_VALUE }, { -it.priority.ordinal })
            TaskSort.PRIORITY -> compareBy<Task>({ -it.priority.ordinal }, { it.dueDay ?: Long.MAX_VALUE }, { it.dueMinute ?: Int.MAX_VALUE })
            TaskSort.NEWEST -> compareByDescending<Task> { it.createdAt }
        }
        // انجام‌شده‌ها همیشه پایین فهرست می‌روند.
        return filtered.sortedWith(compareBy<Task> { it.done }.then(cmp))
    }

    /** پیشرفت امروز: (انجام‌شده، کل) وظایف با سررسید امروز */
    fun todayProgress(tasks: List<Task>, today: Long): Pair<Int, Int> {
        val t = tasks.filter { it.dueDay == today }
        return t.count { it.done } to t.size
    }

    fun priorityLabel(p: Priority) = p.label
}
