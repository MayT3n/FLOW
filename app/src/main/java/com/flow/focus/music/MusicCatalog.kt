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
    val sourceUrl: String = "https://github.com/farmmer938-lgtm/aeris-cc0-music",
)

object MusicCatalog {
    const val ARTIST = "AERIS MUSIC"
    const val LICENSE_NAME = "CC0 1.0 Universal"
    const val SOURCE = "https://github.com/farmmer938-lgtm/aeris-cc0-music"

    // Original instrumental compositions. The linked repository publishes these files under CC0 1.0.
    val tracks = listOf(
        MusicTrack("quiet-room", "Quiet Room", ARTIST, "آرام و مینیمال", "۰:۵۷", "quiet-room.mp3", 2_287_724L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/quiet-room.mp3"),
        MusicTrack("afterglow-vast", "Afterglow Vast", ARTIST, "گرم و آرام", "۱:۳۱", "afterglow-vast.mp3", 3_659_564L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/afterglow-vast.mp3"),
        MusicTrack("bedroom-window-vast", "Bedroom Window Vast", ARTIST, "Lo-fi و خلوت", "۱:۴۴", "bedroom-window-vast.mp3", 4_153_964L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/bedroom-window-vast.mp3"),
        MusicTrack("old-photos-outlands", "Old Photos (Outlands)", ARTIST, "نوستالژیک", "۱:۴۴", "old-photos-outlands.mp3", 4_153_964L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/old-photos-outlands.mp3"),
        MusicTrack("horizon-monolith", "Horizon Monolith", ARTIST, "روشن و انگیزشی", "۱:۰۹", "horizon-monolith.mp3", 2_745_644L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/horizon-monolith.mp3"),
        MusicTrack("ash", "Ash", ARTIST, "تیره و مینیمال", "۱:۲۷", "ash.mp3", 3_493_484L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/ash.mp3"),
        MusicTrack("signal-lost", "Signal Lost", ARTIST, "سینمایی", "۱:۲۰", "signal-lost.mp3", 3_202_604L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/signal-lost.mp3"),
        MusicTrack("beneath", "Beneath", ARTIST, "عمیق و سینمایی", "۱:۲۰", "beneath.mp3", 3_202_604L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/beneath.mp3"),
        MusicTrack("colder-air", "Colder Air", ARTIST, "سرد و خلوت", "۰:۵۵", "colder-air.mp3", 2_184_044L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/colder-air.mp3"),
        MusicTrack("evergreen-open-road", "Evergreen (Open Road)", ARTIST, "روشن و سبک", "۰:۳۹", "evergreen-open-road.mp3", 1_545_644L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/evergreen-open-road.mp3"),
        MusicTrack("overdrive-signal-lost", "Overdrive Signal Lost", ARTIST, "انرژی‌بخش", "۱:۰۰", "overdrive-signal-lost.mp3", 2_401_964L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/overdrive-signal-lost.mp3"),
        MusicTrack("bedroom-window-outlands", "Bedroom Window (Outlands)", ARTIST, "Lo-fi و آرام", "۰:۵۸", "bedroom-window-outlands.mp3", 2_337_644L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/bedroom-window-outlands.mp3"),
        MusicTrack("the-long-walk-signal-lost", "The Long Walk", ARTIST, "سینمایی و آرام", "۰:۵۰", "the-long-walk-signal-lost.mp3", 2_002_604L,
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/the-long-walk-signal-lost.mp3"),
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
