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

    // The bundled demo catalogue has been retired. Only tracks with verified
    // distribution rights and clear attribution may be added in a future release.
    val tracks: List<MusicTrack> = emptyList()

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
