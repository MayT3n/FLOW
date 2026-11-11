package com.flow.focus.core

import java.time.LocalDate
import java.time.ZoneId

/** Converts a task's optional Jalali-day epoch and minute-of-day into a safe alarm instant. */
object ReminderTime {
    fun epochMillis(epochDay: Long, minuteOfDay: Int, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        if (minuteOfDay !in 0 until 24 * 60) return null
        return runCatching {
            LocalDate.ofEpochDay(epochDay)
                .atTime(minuteOfDay / 60, minuteOfDay % 60)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
}
