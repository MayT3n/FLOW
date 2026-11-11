package com.flow.focus

import com.flow.focus.core.ReminderTime
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTimeTest {
    @Test
    fun convertsValidMinuteOfDayToEpochMillis() {
        val day = LocalDate.of(2026, 11, 11).toEpochDay()
        val expected = LocalDate.of(2026, 11, 11).atTime(9, 30)
            .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()

        assertEquals(expected, ReminderTime.epochMillis(day, 9 * 60 + 30, ZoneId.of("UTC")))
    }

    @Test
    fun rejectsOutOfRangeMinuteOfDay() {
        val day = LocalDate.of(2026, 11, 11).toEpochDay()

        assertNull(ReminderTime.epochMillis(day, -1, ZoneId.of("UTC")))
        assertNull(ReminderTime.epochMillis(day, 24 * 60, ZoneId.of("UTC")))
        assertNull(ReminderTime.epochMillis(day, 99_999, ZoneId.of("UTC")))
    }

    @Test
    fun rejectsEpochDaysOutsideLocalDateRange() {
        assertNull(ReminderTime.epochMillis(Long.MAX_VALUE, 9 * 60, ZoneId.of("UTC")))
        assertNull(ReminderTime.epochMillis(Long.MIN_VALUE, 9 * 60, ZoneId.of("UTC")))
    }
}
