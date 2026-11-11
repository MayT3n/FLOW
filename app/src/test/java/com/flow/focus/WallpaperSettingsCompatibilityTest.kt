package com.flow.focus

import com.flow.focus.data.AppData
import com.flow.focus.data.WallpaperFit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperSettingsCompatibilityTest {
    @Test
    fun olderBackupWithoutWallpaperFieldsUsesSafeDefaults() {
        val data = Json { ignoreUnknownKeys = true }.decodeFromString<AppData>(
            """{"version":1,"settings":{"focusMin":25}}"""
        )

        assertEquals("gradient", data.settings.wallpaperId)
        assertEquals(null, data.settings.wallpaperPath)
        assertEquals(WallpaperFit.CROP, data.settings.wallpaperFit)
        assertEquals(0.18f, data.settings.wallpaperDim)
        assertEquals(0, data.settings.wallpaperBlurDp)
    }
}
