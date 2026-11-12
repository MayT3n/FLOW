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

    // Four short CC0 tracks from AERIS MUSIC's public-domain free catalogue.
    // Total download size is 12,438,896 bytes (about 11.9 MiB), well below 50 MB.
    // Track artwork is rendered locally from a track-specific cover style because
    // the source repository distributes audio and license text, but no per-track covers.
    val tracks: List<MusicTrack> = listOf(
        MusicTrack(
            id = "bedroom-window-outlands",
            title = "Bedroom Window (Outlands)",
            artist = "AERIS MUSIC",
            mood = "Lo-fi · آرام",
            durationLabel = "0:58",
            fileName = "bedroom-window-outlands.mp3",
            fileSize = 2_337_644L,
            url = "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/bedroom-window-outlands.mp3",
            sourceUrl = "https://farmmer938-lgtm.github.io/aeris-store/free/",
        ),
        MusicTrack(
            id = "quiet-room",
            title = "Quiet Room",
            artist = "AERIS MUSIC",
            mood = "Ambient · آرام",
            durationLabel = "0:57",
            fileName = "quiet-room.mp3",
            fileSize = 2_287_724L,
            url = "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/quiet-room.mp3",
            sourceUrl = "https://farmmer938-lgtm.github.io/aeris-store/free/",
        ),
        MusicTrack(
            id = "afterglow-vast",
            title = "Afterglow Vast",
            artist = "AERIS MUSIC",
            mood = "Chill · گرم",
            durationLabel = "1:31",
            fileName = "afterglow-vast.mp3",
            fileSize = 3_659_564L,
            url = "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/afterglow-vast.mp3",
            sourceUrl = "https://farmmer938-lgtm.github.io/aeris-store/free/",
        ),
        MusicTrack(
            id = "old-photos-outlands",
            title = "Old Photos (Outlands)",
            artist = "AERIS MUSIC",
            mood = "Lo-fi · نوستالژیک",
            durationLabel = "1:44",
            fileName = "old-photos-outlands.mp3",
            fileSize = 4_153_964L,
            url = "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/old-photos-outlands.mp3",
            sourceUrl = "https://farmmer938-lgtm.github.io/aeris-store/free/",
        ),
    )

    /** Resolves playlist IDs in saved order, dropping stale IDs and duplicate entries safely. */
    fun resolveTracks(trackIds: List<String>): List<MusicTrack> =
        trackIds.distinct().mapNotNull { id -> tracks.firstOrNull { it.id == id } }

    fun mediaItem(track: MusicTrack, localFile: java.io.File? = null): MediaItem {
        val uri = localFile?.takeIf { it.isFile && it.length() > 0L }?.let { Uri.fromFile(it) }
            ?: Uri.parse(track.url)
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
