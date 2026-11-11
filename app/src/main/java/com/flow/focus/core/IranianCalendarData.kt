package com.flow.focus.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** An offline observance from the published Iranian calendar for a specific Jalali year. */
data class CalendarOccasion(
    val id: String,
    val epochDay: Long,
    val titleFa: String,
    val isHoliday: Boolean,
    val categories: List<String>,
)

@Serializable
private data class CalendarFeed(
    val year: Int,
    val schemaVersion: Int = 1,
    val dataVersion: String = "",
    val source: String = "",
    val items: List<CalendarItem> = emptyList(),
    val notes: List<String> = emptyList(),
)

@Serializable
private data class CalendarItem(
    val id: String,
    val date: String,
    val title: CalendarTitle,
    val isHoliday: Boolean = false,
    val categories: List<String> = emptyList(),
)

@Serializable
private data class CalendarTitle(
    val fa: String,
    val en: String? = null,
)

/**
 * Bundled official-year calendar data. All dates are stored as Jalali YYYY-MM-DD,
 * then converted through the app's tested Jalali implementation to epoch days.
 *
 * Data is loaded from APK assets, so calendar rendering never requires network access.
 * The supported years are the ones shipped under assets/calendar/.
 */
object IranianCalendarData {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context, jalaliYear: Int): List<CalendarOccasion> {
        val holidayPath = "calendar/$jalaliYear-holidays.json"
        val eventPath = "calendar/$jalaliYear-events.json"
        val holidays = readFeed(context, holidayPath, jalaliYear)
        val events = readFeed(context, eventPath, jalaliYear)
        return (holidays + events)
            .distinctBy { it.id }
            .sortedWith(compareBy<CalendarOccasion>({ it.epochDay }, { !it.isHoliday }, { it.titleFa }))
    }

    private fun readFeed(context: Context, assetPath: String, expectedYear: Int): List<CalendarOccasion> {
        val raw = runCatching {
            context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return emptyList()

        val feed = runCatching { json.decodeFromString<CalendarFeed>(raw) }.getOrNull()
            ?: return emptyList()
        if (feed.schemaVersion != 1 || feed.year != expectedYear) return emptyList()

        return feed.items.mapNotNull { item ->
            if (item.id.isBlank() || item.title.fa.isBlank()) return@mapNotNull null
            val parts = item.date.split('-')
            if (parts.size != 3) return@mapNotNull null
            val year = parts[0].toIntOrNull() ?: return@mapNotNull null
            val month = parts[1].toIntOrNull() ?: return@mapNotNull null
            val day = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (year != expectedYear || month !in 1..12) return@mapNotNull null
            // Reject impossible Jalali dates (for example, Esfand 30 in a non-leap year).
            if (day !in 1..JalaliDate.monthLength(year, month)) return@mapNotNull null

            val epochDay = runCatching { JalaliDate(year, month, day).toLocalDate().toEpochDay() }.getOrNull()
                ?: return@mapNotNull null
            CalendarOccasion(
                id = item.id,
                epochDay = epochDay,
                titleFa = item.title.fa,
                isHoliday = item.isHoliday,
                categories = item.categories,
            )
        }.distinctBy { it.id }
    }
}
