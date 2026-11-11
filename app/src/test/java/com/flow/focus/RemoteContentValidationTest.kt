package com.flow.focus

import com.flow.focus.remote.NewsItem
import com.flow.focus.remote.RemoteContentValidation
import com.flow.focus.remote.UpdateManifest
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteContentValidationTest {
    @Test
    fun newsFeedDropsExpiredInvalidAndDuplicateItems() {
        val now = 2_000L
        val raw = """
            {
              "schemaVersion": 1,
              "items": [
                {
                  "id": "release-note",
                  "title": "خبر معتبر",
                  "body": "متن اطلاعیه",
                  "category": "feature",
                  "publishedAt": 1000,
                  "expiresAt": 5000,
                  "enabled": true,
                  "url": "https://example.com/news"
                },
                {
                  "id": "release-note",
                  "title": "خبر تکراری",
                  "body": "نباید دوباره نمایش داده شود",
                  "publishedAt": 1100
                },
                {
                  "id": "expired",
                  "title": "منقضی",
                  "body": "متن",
                  "publishedAt": 1000,
                  "expiresAt": 1500
                },
                {
                  "id": "unsafe-link",
                  "title": "لینک نامعتبر",
                  "body": "متن",
                  "publishedAt": 1000,
                  "url": "javascript:alert(1)"
                },
                {
                  "id": "blank-title",
                  "title": " ",
                  "body": "متن",
                  "publishedAt": 1000
                }
              ]
            }
        """.trimIndent()

        val result = RemoteContentValidation.parseNewsFeed(raw, now)

        assertEquals(listOf("release-note"), result?.items?.map { it.id })
        assertEquals("خبر معتبر", result?.items?.single()?.title)
    }

    @Test
    fun malformedAndOversizedFeedsAreRejected() {
        assertNull(RemoteContentValidation.parseNewsFeed("{broken", 2_000L))
        assertNull(
            RemoteContentValidation.parseNewsFeed(
                """{"schemaVersion":1,"items":[]}""" + " ".repeat(RemoteContentValidation.MAX_JSON_CHARS),
                2_000L,
            ),
        )
        assertNull(RemoteContentValidation.parseNewsFeed("""{"schemaVersion":2,"items":[]}""", 2_000L))
    }

    @Test
    fun onlyHttpsLinksWithHostsAreAccepted() {
        assertTrue(RemoteContentValidation.isHttpsUrl("https://example.com/path"))
        assertFalse(RemoteContentValidation.isHttpsUrl("http://example.com"))
        assertFalse(RemoteContentValidation.isHttpsUrl("javascript:alert(1)"))
        assertFalse(RemoteContentValidation.isHttpsUrl("https:///missing-host"))
    }

    @Test
    fun updateManifestRequiresBoundedApkAndValidHash() {
        val valid = UpdateManifest(
            versionName = "1.3",
            versionCode = 1200,
            downloadUrl = "https://example.com/MatFocus.apk",
            fileSize = 2_000_000,
            sha256 = "a".repeat(64),
            minimumSupportedVersion = 1000,
            updateAvailable = true,
            applicationId = "com.flow.focus",
        )
        assertTrue(RemoteContentValidation.isValidManifest(valid))
        assertFalse(RemoteContentValidation.isValidManifest(valid.copy(downloadUrl = "http://example.com/app.apk")))
        assertFalse(RemoteContentValidation.isValidManifest(valid.copy(sha256 = "bad")))
        assertFalse(
            RemoteContentValidation.isValidManifest(
                valid.copy(fileSize = RemoteContentValidation.MAX_APK_BYTES + 1),
            ),
        )
        assertFalse(RemoteContentValidation.isValidManifest(valid.copy(applicationId = "another.package")))
    }

    @Test
    fun boundedReaderStopsBeforeAllocatingAnOversizedResponse() {
        assertEquals("small", RemoteContentValidation.readBounded(StringReader("small"), 10))
        assertNull(RemoteContentValidation.readBounded(StringReader("too large"), 4))
    }
}
