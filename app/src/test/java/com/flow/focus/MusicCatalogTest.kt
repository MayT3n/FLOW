package com.flow.focus

import com.flow.focus.music.MusicCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicCatalogTest {
    @Test
    fun curatedCatalogueContainsOnlyVerifiedCc0TracksWithinStorageBudget() {
        val tracks = MusicCatalog.tracks
        assertEquals(4, tracks.size)
        assertTrue(tracks.all { it.fileSize > 0L })
        assertTrue(tracks.all { it.license == "CC0 1.0" })
        assertTrue(tracks.all { it.licenseUrl == "https://creativecommons.org/publicdomain/zero/1.0/" })
        assertTrue(tracks.all { it.url.startsWith("https://raw.githubusercontent.com/") })
        assertTrue(tracks.sumOf { it.fileSize } <= 50L * 1024L * 1024L)
    }

    @Test
    fun playlistResolverDropsUnknownAndDuplicateIds() {
        val id = MusicCatalog.tracks.first().id
        assertEquals(listOf(id), MusicCatalog.resolveTracks(listOf(id, "unknown-track", id)).map { it.id })
    }

    @Test
    fun emptyPlaylistResolvesToNoTracks() {
        assertTrue(MusicCatalog.resolveTracks(emptyList()).isEmpty())
    }
}
