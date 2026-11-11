package com.flow.focus

import com.flow.focus.music.MusicCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicCatalogTest {
    @Test
    fun retiredDemoCatalogueDoesNotExposeSyntheticTracks() {
        assertTrue(MusicCatalog.tracks.isEmpty())
    }

    @Test
    fun emptyPlaylistResolvesToNoTracks() {
        assertTrue(MusicCatalog.resolveTracks(emptyList()).isEmpty())
    }
}
