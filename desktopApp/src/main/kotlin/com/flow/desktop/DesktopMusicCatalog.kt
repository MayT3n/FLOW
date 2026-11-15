package com.flow.desktop

data class DesktopTrack(
    val id: String,
    val title: String,
    val mood: String,
    val durationLabel: String,
    val fileName: String,
    val fileSize: Long,
    val url: String,
    val license: String = "CC0 1.0",
    val sourceUrl: String = "https://github.com/farmmer938-lgtm/aeris-cc0-music"
)

/** Tracks mirrored from the Android app's CC0 music catalogue. */
object DesktopMusicCatalog {
    private const val ARTIST = "AERIS MUSIC"
    val tracks = listOf(
        DesktopTrack("quiet-room", "Quiet Room", "آرام و مینیمال", "۰:۵۷", "quiet-room.mp3", 2_287_724L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/quiet-room.mp3"),
        DesktopTrack("afterglow-vast", "Afterglow Vast", "گرم و آرام", "۱:۳۱", "afterglow-vast.mp3", 3_659_564L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/afterglow-vast.mp3"),
        DesktopTrack("bedroom-window-vast", "Bedroom Window Vast", "Lo-fi و خلوت", "۱:۴۴", "bedroom-window-vast.mp3", 4_153_964L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/bedroom-window-vast.mp3"),
        DesktopTrack("old-photos-outlands", "Old Photos (Outlands)", "نوستالژیک", "۱:۴۴", "old-photos-outlands.mp3", 4_153_964L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/old-photos-outlands.mp3"),
        DesktopTrack("horizon-monolith", "Horizon Monolith", "روشن و انگیزشی", "۱:۰۹", "horizon-monolith.mp3", 2_745_644L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/horizon-monolith.mp3"),
        DesktopTrack("ash", "Ash", "تیره و مینیمال", "۱:۲۷", "ash.mp3", 3_493_484L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/ash.mp3"),
        DesktopTrack("signal-lost", "Signal Lost", "سینمایی", "۱:۲۰", "signal-lost.mp3", 3_202_604L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/signal-lost.mp3"),
        DesktopTrack("beneath", "Beneath", "عمیق و سینمایی", "۱:۲۰", "beneath.mp3", 3_202_604L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/beneath.mp3"),
        DesktopTrack("colder-air", "Colder Air", "سرد و خلوت", "۰:۵۵", "colder-air.mp3", 2_184_044L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/colder-air.mp3"),
        DesktopTrack("evergreen-open-road", "Evergreen (Open Road)", "روشن و سبک", "۰:۳۹", "evergreen-open-road.mp3", 1_545_644L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/evergreen-open-road.mp3"),
        DesktopTrack("overdrive-signal-lost", "Overdrive Signal Lost", "انرژی‌بخش", "۱:۰۰", "overdrive-signal-lost.mp3", 2_401_964L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/overdrive-signal-lost.mp3"),
        DesktopTrack("bedroom-window-outlands", "Bedroom Window (Outlands)", "Lo-fi و آرام", "۰:۵۸", "bedroom-window-outlands.mp3", 2_337_644L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/bedroom-window-outlands.mp3"),
        DesktopTrack("the-long-walk-signal-lost", "The Long Walk", "سینمایی و آرام", "۰:۵۰", "the-long-walk-signal-lost.mp3", 2_002_604L, "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/the-long-walk-signal-lost.mp3")
    )

    const val artist = ARTIST
    const val licenseName = "CC0 1.0 Universal"
}
