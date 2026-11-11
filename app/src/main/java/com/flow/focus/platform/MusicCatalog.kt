package com.flow.focus.platform

/** Original instrumental catalogue published by AERIS MUSIC under CC0 1.0. */
data class MusicTrack(
    val id: String,
    val title: String,
    val mood: String,
    val durationLabel: String,
    val fileName: String,
    val sourceUrl: String,
)

object MusicCatalog {
    private const val ROOT = "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/"

    val tracks = listOf(
        MusicTrack("quiet-room", "Quiet Room", "Ambient · آرام", "00:57", "quiet-room.mp3", ROOT + "quiet-room.mp3"),
        MusicTrack("afterglow-vast", "Afterglow Vast", "Chill · گرم", "01:31", "afterglow-vast.mp3", ROOT + "afterglow-vast.mp3"),
        MusicTrack("bedroom-window-outlands", "Bedroom Window", "Lo-fi · آرام", "00:58", "bedroom-window-outlands.mp3", ROOT + "bedroom-window-outlands.mp3"),
        MusicTrack("old-photos-outlands", "Old Photos", "Lo-fi · نوستالژیک", "01:44", "old-photos-outlands.mp3", ROOT + "old-photos-outlands.mp3"),
        MusicTrack("evergreen-open-road", "Evergreen", "Instrumental · روشن", "00:39", "evergreen-open-road.mp3", ROOT + "evergreen-open-road.mp3"),
        MusicTrack("horizon-monolith", "Horizon Monolith", "Cinematic · امیدوار", "01:09", "horizon-monolith.mp3", ROOT + "horizon-monolith.mp3"),
        MusicTrack("ash", "Ash", "Minimal · تیره", "01:27", "ash.mp3", ROOT + "ash.mp3"),
        MusicTrack("colder-air", "Colder Air", "Ambient · سرد", "00:55", "colder-air.mp3", ROOT + "colder-air.mp3"),
        MusicTrack("beneath", "Beneath", "Cinematic · عمیق", "01:20", "beneath.mp3", ROOT + "beneath.mp3"),
        MusicTrack("signal-lost", "Signal Lost", "Cinematic · پرتنش", "01:20", "signal-lost.mp3", ROOT + "signal-lost.mp3"),
        MusicTrack("the-long-walk-signal-lost", "The Long Walk", "Cinematic · آرام", "00:50", "the-long-walk-signal-lost.mp3", ROOT + "the-long-walk-signal-lost.mp3"),
        MusicTrack("overdrive-signal-lost", "Overdrive", "Instrumental · پرانرژی", "01:00", "overdrive-signal-lost.mp3", ROOT + "overdrive-signal-lost.mp3"),
        MusicTrack("bedroom-window-vast", "Bedroom Window Vast", "Lo-fi · مه‌آلود", "01:44", "bedroom-window-vast.mp3", ROOT + "bedroom-window-vast.mp3"),
    )
}
