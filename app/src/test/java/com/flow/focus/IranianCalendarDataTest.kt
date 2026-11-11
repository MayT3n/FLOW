package com.flow.focus

import com.flow.focus.core.IranianCalendarData
import com.flow.focus.core.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianCalendarDataTest {
    @Test
    fun rejectsInvalidDatesAndKeepsValidEvents() {
        val json = """
            {
              "year": 1404,
              "schemaVersion": 1,
              "source": "unit test",
              "items": [
                {
                  "id": "valid",
                  "date": "1404-12-29",
                  "title": { "fa": "آخرین روز سال" },
                  "isHoliday": false,
                  "categories": ["national"]
                },
                {
                  "id": "invalid-esfand-30",
                  "date": "1404-12-30",
                  "title": { "fa": "تاریخ نامعتبر" },
                  "isHoliday": true
                },
                {
                  "id": "invalid-month",
                  "date": "1404-13-01",
                  "title": { "fa": "ماه نامعتبر" }
                },
                {
                  "id": "blank-title",
                  "date": "1404-01-01",
                  "title": { "fa": " " }
                }
              ]
            }
        """.trimIndent()

        val result = IranianCalendarData.parseFeed(json, 1404)

        assertEquals(1, result.size)
        assertEquals("آخرین روز سال", result.single().titleFa)
        assertEquals(JalaliDate(1404, 12, 29).toLocalDate().toEpochDay(), result.single().epochDay)
        assertTrue(result.single().categories.contains("national"))
    }

    @Test
    fun acceptsEsfand30OnlyInLeapYears() {
        val leapYear = """
            {
              "year": 1403,
              "schemaVersion": 1,
              "items": [
                {
                  "id": "leap-day",
                  "date": "1403-12-30",
                  "title": { "fa": "روز پایانی سال کبیسه" }
                }
              ]
            }
        """.trimIndent()

        val result = IranianCalendarData.parseFeed(leapYear, 1403)

        assertEquals(1, result.size)
        assertEquals(JalaliDate(1403, 12, 30).toLocalDate().toEpochDay(), result.single().epochDay)
    }

    @Test
    fun rejectsWrongYearAndUnsupportedSchema() {
        val feed = """
            {
              "year": 1405,
              "schemaVersion": 2,
              "items": []
            }
        """.trimIndent()

        assertTrue(IranianCalendarData.parseFeed(feed, 1405).isEmpty())
        assertTrue(IranianCalendarData.parseFeed(feed, 1404).isEmpty())
    }
}
