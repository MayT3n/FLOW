package com.flow.desktop

import com.flow.focus.core.JalaliDate
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class DesktopOccasion(
    val id: String,
    val epochDay: Long,
    val titleFa: String,
    val isHoliday: Boolean,
    val categories: List<String>
)

@Serializable
private data class OccasionFeed(
    val year: Int,
    val schemaVersion: Int = 1,
    val items: List<OccasionItem> = emptyList()
)

@Serializable
private data class OccasionItem(
    val id: String,
    val date: String,
    val title: OccasionTitle,
    val isHoliday: Boolean = false,
    val categories: List<String> = emptyList()
)

@Serializable
private data class OccasionTitle(val fa: String, val en: String? = null)

/** Offline calendar feeds bundled from the existing Android app assets. */
object DesktopIranianCalendar {
    private val json = Json { ignoreUnknownKeys = true }
    private val supportedYears = listOf(1405)
    private val cached: List<DesktopOccasion> by lazy(LazyThreadSafetyMode.PUBLICATION) { supportedYears.flatMap { year ->
        listOf("holidays", "events").flatMap { kind ->
            val path = "/calendar/$year-$kind.json"
            val raw = DesktopIranianCalendar::class.java.getResourceAsStream(path)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: return@flatMap emptyList()
            parse(raw, year)
        }
    }.distinctBy { it.id }.sortedWith(compareBy({ it.epochDay }, { !it.isHoliday }, { it.titleFa })) }

    fun supportedJalaliYears(): List<Int> = supportedYears

    fun all(): List<DesktopOccasion> = cached

    fun on(date: LocalDate): List<DesktopOccasion> =
        all().filter { it.epochDay == date.toEpochDay() }

    internal fun parse(raw: String, expectedYear: Int): List<DesktopOccasion> {
        val feed = runCatching { json.decodeFromString<OccasionFeed>(raw) }.getOrNull() ?: return emptyList()
        if (feed.schemaVersion != 1 || feed.year != expectedYear) return emptyList()
        return feed.items.mapNotNull { item ->
            if (item.id.isBlank() || item.title.fa.isBlank()) return@mapNotNull null
            val parts = item.date.split('-')
            if (parts.size != 3) return@mapNotNull null
            val y = parts[0].toIntOrNull() ?: return@mapNotNull null
            val m = parts[1].toIntOrNull() ?: return@mapNotNull null
            val d = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (y != expectedYear || m !in 1..12 || d !in 1..JalaliDate.monthLength(y, m)) return@mapNotNull null
            val epoch = runCatching { JalaliDate(y, m, d).toLocalDate().toEpochDay() }.getOrNull()
                ?: return@mapNotNull null
            DesktopOccasion(item.id, epoch, item.title.fa, item.isHoliday, item.categories)
        }.distinctBy { it.id }
    }
}
