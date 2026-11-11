package com.flow.focus

import com.flow.focus.music.MusicCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicCatalogTest {
    @Test
    fun playlistResolutionPreservesOrderAndDropsUnknownOrDuplicateIds() {
        val first = MusicCatalog.tracks[3].id
        val second = MusicCatalog.tracks[0].id

        val resolved = MusicCatalog.resolveTracks(listOf(first, "removed-track-id", second, first))

        assertEquals(listOf(first, second), resolved.map { it.id })
    }

    @Test
    fun emptyPlaylistResolvesToNoTracks() {
        assertEquals(emptyList<String>(), MusicCatalog.resolveTracks(emptyList()).map { it.id })
    }
}
