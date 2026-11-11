package com.flow.focus

import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.core.withDigits
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliTest {
    private fun j(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).let { JalaliDate.of(it) }

    @Test fun nowruzDates() {
        assertEquals(JalaliDate(1403, 1, 1), j(2024, 3, 20))
        assertEquals(JalaliDate(1404, 1, 1), j(2025, 3, 21))
        assertEquals(JalaliDate(1405, 1, 1), j(2026, 3, 21))
    }

    @Test fun knownDates() {
        assertEquals(JalaliDate(1405, 8, 19), j(2026, 11, 10))
        assertEquals(JalaliDate(1404, 8, 19), j(2025, 11, 10))
        assertEquals(JalaliDate(1399, 11, 22), j(2021, 2, 10))
        assertEquals(JalaliDate(1357, 11, 22), j(1979, 2, 11))
    }

    @Test fun leapYears() {
        assertTrue(JalaliDate.isLeap(1403))
        assertFalse(JalaliDate.isLeap(1404))
        assertFalse(JalaliDate.isLeap(1405))
        assertEquals(30, JalaliDate.monthLength(1403, 12))
        assertEquals(29, JalaliDate.monthLength(1404, 12))
        assertEquals(31, JalaliDate.monthLength(1405, 6))
        assertEquals(30, JalaliDate.monthLength(1405, 7))
    }

    @Test fun roundTripEveryDayFor40Years() {
        var d = LocalDate.of(1990, 1, 1)
        val end = LocalDate.of(2030, 12, 31)
        while (!d.isAfter(end)) {
            val back = JalaliDate.of(d).toLocalDate()
            assertEquals(d, back)
            d = d.plusDays(1)
        }
    }

    @Test fun weekdayIndexStartsSaturday() {
        assertEquals(0, JalaliDate.weekdayIndex(LocalDate.of(2026, 11, 7))) // شنبه
        assertEquals(3, JalaliDate.weekdayIndex(LocalDate.of(2026, 11, 10))) // سه‌شنبه
        assertEquals(6, JalaliDate.weekdayIndex(LocalDate.of(2026, 11, 13))) // جمعه
        assertEquals(LocalDate.of(2026, 11, 7), JalaliDate.startOfWeek(LocalDate.of(2026, 11, 10)))
    }

    @Test fun monthArithmetic() {
        assertEquals(JalaliDate(1406, 1, 15), JalaliDate(1405, 12, 15).plusMonths(1))
        assertEquals(JalaliDate(1405, 11, 29), JalaliDate(1405, 12, 29).plusMonths(-1))
        assertEquals(JalaliDate(1405, 12, 29), JalaliDate(1405, 11, 30).plusMonths(1))
    }

    @Test fun formats() {
        assertEquals("سه‌شنبه، 19 آبان 1405", DateFormats.full(LocalDate.of(2026, 11, 10)))
        assertEquals("۲۴:۳۸", "24:38".withDigits(true))
        assertEquals("24:38", "۲۴:۳۸".withDigits(false))
    }
}
