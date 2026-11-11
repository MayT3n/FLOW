package com.flow.focus

import com.flow.focus.ui.relativeDay
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDateValidationTest {
    @Test
    fun invalidEpochDayDoesNotCrashTaskDateLabel() {
        assertEquals("تاریخ نامعتبر", relativeDay(Long.MAX_VALUE, LocalDate.of(2026, 11, 11)))
        assertEquals("تاریخ نامعتبر", relativeDay(Long.MIN_VALUE, LocalDate.of(2026, 11, 11)))
    }
}
