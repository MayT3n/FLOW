package com.flow.focus.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val mood: String,
    val durationLabel: String,
    val fileName: String,
    val fileSize: Long,
    val url: String,
    val license: String = "CC0 1.0",
    val licenseUrl: String = "https://creativecommons.org/publicdomain/zero/1.0/",
    val sourceUrl: String = "https://github.com/btahir/open-lofi",
)

object MusicCatalog {
    const val ARTIST = "OpenLo-Fi"
    const val LICENSE_NAME = "CC0 1.0 Universal"
    const val SOURCE = "https://github.com/btahir/open-lofi"

    private const val RAW = "https://raw.githubusercontent.com/MayT3n/FLOW/music-assets/tracks"

    // آهنگ‌های لوفای CC0 از پروژه‌ی OpenLo-Fi (۳ تا ۵ دقیقه)؛ روی شاخه‌ی music-assets مخزن برنامه میزبانی می‌شوند.
    val tracks = listOf(
        MusicTrack("polar-afterglow", "Polar Afterglow", ARTIST, "رویایی و آرام", "۴:۵۹", "polar-afterglow.mp3", 6_799_710L,
            "$RAW/polar-afterglow.mp3"),
        MusicTrack("lazy-love-letter-afternoon", "Lazy Love Letter Afternoon", ARTIST, "گرم و آرام", "۴:۵۱", "lazy-love-letter-afternoon.mp3", 6_322_686L,
            "$RAW/lazy-love-letter-afternoon.mp3"),
        MusicTrack("cursor-after-midnight", "Cursor After Midnight", ARTIST, "تمرکز و مطالعه", "۳:۵۲", "cursor-after-midnight.mp3", 4_627_158L,
            "$RAW/cursor-after-midnight.mp3"),
        MusicTrack("hour-between-clicks", "Hour Between Clicks", ARTIST, "تمرکز و مطالعه", "۳:۴۳", "hour-between-clicks.mp3", 4_808_142L,
            "$RAW/hour-between-clicks.mp3"),
        MusicTrack("sea-glass-evening", "Sea Glass Evening", ARTIST, "رویایی و آرام", "۳:۴۴", "sea-glass-evening.mp3", 5_000_358L,
            "$RAW/sea-glass-evening.mp3"),
        MusicTrack("soft-weightless-hours", "Soft Weightless Hours", ARTIST, "رویایی و آرام", "۳:۴۲", "soft-weightless-hours.mp3", 4_640_502L,
            "$RAW/soft-weightless-hours.mp3"),
        MusicTrack("dust-and-hardcovers", "Dust and Hardcovers", ARTIST, "جز و کتابخانه", "۳:۳۲", "dust-and-hardcovers.mp3", 4_943_310L,
            "$RAW/dust-and-hardcovers.mp3"),
        MusicTrack("autumn-on-the-window-glass", "Autumn on the Window Glass", ARTIST, "باران و پاییز", "۳:۱۲", "autumn-on-the-window-glass.mp3", 4_360_038L,
            "$RAW/autumn-on-the-window-glass.mp3"),
        MusicTrack("underwater-dreamscape", "Underwater Dreamscape", ARTIST, "رویایی و آرام", "۳:۳۱", "underwater-dreamscape.mp3", 4_687_302L,
            "$RAW/underwater-dreamscape.mp3"),
        MusicTrack("exhale-the-morning", "Exhale the Morning", ARTIST, "تمرکز و مطالعه", "۳:۱۹", "exhale-the-morning.mp3", 4_046_982L,
            "$RAW/exhale-the-morning.mp3"),
        MusicTrack("mat-and-morning-light", "Mat and Morning Light", ARTIST, "تمرکز و مطالعه", "۳:۱۸", "mat-and-morning-light.mp3", 4_154_622L,
            "$RAW/mat-and-morning-light.mp3"),
        MusicTrack("almost-floating", "Almost Floating", ARTIST, "رویایی و آرام", "۳:۲۲", "almost-floating.mp3", 4_530_654L,
            "$RAW/almost-floating.mp3"),
        MusicTrack("ghosts-on-the-hillside", "Ghosts on the Hillside", ARTIST, "رویایی و آرام", "۳:۱۹", "ghosts-on-the-hillside.mp3", 4_307_478L,
            "$RAW/ghosts-on-the-hillside.mp3"),
        MusicTrack("tide-pools-at-twilight", "Tide Pools at Twilight", ARTIST, "رویایی و آرام", "۳:۰۹", "tide-pools-at-twilight.mp3", 4_256_598L,
            "$RAW/tide-pools-at-twilight.mp3"),
        MusicTrack("cathedral-hiss", "Cathedral Hiss", ARTIST, "رویایی و آرام", "۳:۲۷", "cathedral-hiss.mp3", 4_500_942L,
            "$RAW/cathedral-hiss.mp3"),
        MusicTrack("midnight-notes-on-the-floor", "Midnight Notes on the Floor", ARTIST, "گرم و آرام", "۳:۳۲", "midnight-notes-on-the-floor.mp3", 4_336_782L,
            "$RAW/midnight-notes-on-the-floor.mp3"),
    )

    fun mediaItem(track: MusicTrack, localFile: java.io.File): MediaItem {
        val uri = Uri.fromFile(localFile)
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle("MatFocus • Focus Sessions")
            .setDisplayTitle(track.title)
            .setSubtitle(track.mood)
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
