package com.flow.focus.domain

import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.FocusSession
import com.flow.focus.data.PhaseType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class StatsRange { DAY, WEEK, MONTH }

data class Bucket(val label: String, val minutes: Long)

data class CategoryStat(val categoryId: String?, val minutes: Long)

data class StatsSummary(
    val focusMinutes: Long,
    val completedSessions: Int,
    val avgSessionMinutes: Long,
    val activeDays: Int,
    val tasksDone: Int,
    val buckets: List<Bucket>,
    /** مجموع دقایق تمرکز هر هفته، از قدیمی به جدید (۸ هفته‌ی اخیر) */
    val weeklyTrend: List<Long>,
    val categories: List<CategoryStat>,
)

object Stats {
    private fun day(ms: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    private fun focus(sessions: List<FocusSession>) = sessions.filter { it.type == PhaseType.FOCUS }

    fun focusMinutesOn(sessions: List<FocusSession>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        focus(sessions).filter { day(it.endedAt, zone) == date }.sumOf { it.actualMs } / 60_000L

    fun completedOn(sessions: List<FocusSession>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int =
        focus(sessions).count { it.completed && day(it.endedAt, zone) == date }

    /** ابتدا و انتهای بازه (شامل) برای هر نوع بازه. */
    fun rangeDays(range: StatsRange, today: LocalDate): Pair<LocalDate, LocalDate> = when (range) {
        StatsRange.DAY -> today to today
        StatsRange.WEEK -> today.minusDays(6) to today
        StatsRange.MONTH -> today.minusDays(29) to today
    }

    fun summarize(
        data: AppData,
        range: StatsRange,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): StatsSummary {
        val (from, to) = rangeDays(range, today)
        val fs = focus(data.sessions).filter { day(it.endedAt, zone) in from..to }
        val minutes = fs.sumOf { it.actualMs } / 60_000L
        val completed = fs.count { it.completed }
        val completedMs = fs.filter { it.completed }.sumOf { it.actualMs }
        val avg = if (completed == 0) 0L else completedMs / completed / 60_000L
        val activeDays = fs.map { day(it.endedAt, zone) }.distinct().size
        val tasksDone = data.tasks.count { t ->
            t.done && t.doneAt != null && day(t.doneAt, zone) in from..to
        }

        val buckets: List<Bucket> = when (range) {
            StatsRange.DAY -> (0 until 24 step 3).map { h ->
                val m = fs.filter { Instant.ofEpochMilli(it.endedAt).atZone(zone).hour in h until h + 3 }
                    .sumOf { it.actualMs } / 60_000L
                Bucket(h.toString(), m)
            }
            StatsRange.WEEK -> (0..6).map { i ->
                val d = from.plusDays(i.toLong())
                Bucket(JalaliDate.weekdayInitials[JalaliDate.weekdayIndex(d)], fs.filter { day(it.endedAt, zone) == d }.sumOf { it.actualMs } / 60_000L)
            }
            StatsRange.MONTH -> (0..29).map { i ->
                val d = from.plusDays(i.toLong())
                Bucket(JalaliDate.of(d).day.toString(), fs.filter { day(it.endedAt, zone) == d }.sumOf { it.actualMs } / 60_000L)
            }
        }

        val thisWeekStart = JalaliDate.startOfWeek(today)
        val trend = (7 downTo 0).map { w ->
            val ws = thisWeekStart.minusWeeks(w.toLong())
            val we = ws.plusDays(6)
            focus(data.sessions).filter { day(it.endedAt, zone) in ws..we }.sumOf { it.actualMs } / 60_000L
        }

        val taskCat = data.tasks.associate { it.id to it.categoryId }
        val cats = fs.groupBy { it.taskId?.let { id -> taskCat[id] } }
            .map { (cat, list) -> CategoryStat(cat, list.sumOf { it.actualMs } / 60_000L) }
            .filter { it.minutes > 0 }
            .sortedByDescending { it.minutes }

        return StatsSummary(minutes, completed, avg, activeDays, tasksDone, buckets, trend, cats)
    }

    /** تعداد جلسه‌های کامل‌شده برای یک وظیفه */
    fun pomodorosDone(sessions: List<FocusSession>, taskId: String): Int =
        sessions.count { it.taskId == taskId && it.type == PhaseType.FOCUS && it.completed }
}
