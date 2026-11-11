package com.flow.focus.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NewsFeed(
    val schemaVersion: Int = 1,
    val notifications: List<RemoteNotice> = emptyList(),
)

@Serializable
data class RemoteNotice(
    val id: String,
    val category: String = "News",
    val title: String,
    val message: String,
    val publishedAt: String,
    val url: String? = null,
    val enabled: Boolean = true,
    val expiresAt: String? = null,
)

@Serializable
data class UpdateManifest(
    val versionName: String,
    val versionCode: Long,
    val releaseDate: String,
    val changelog: List<String> = emptyList(),
    val downloadUrl: String = "",
    val fileSize: Long = 0L,
    val sha256: String = "",
    val minimumSupportedVersion: Long = 0L,
    val mandatory: Boolean = false,
    val updateAvailable: Boolean = false,
    val applicationId: String = "com.flow.focus",
)

data class RemoteContentState(
    val notices: List<RemoteNotice> = emptyList(),
    val manifest: UpdateManifest? = null,
    val readIds: Set<String> = emptySet(),
    val lastSyncedAt: Long = 0L,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val latestUpdate: RemoteNotice?
        get() {
            val release = manifest ?: return null
            if (!release.updateAvailable || release.versionCode <= com.flow.focus.BuildConfig.VERSION_CODE.toLong()) return null
            return RemoteNotice(
                id = "update-${release.versionCode}",
                category = "Update",
                title = "MatFocus ${release.versionName} آماده است",
                message = release.changelog.joinToString("\n"),
                publishedAt = release.releaseDate,
                enabled = true,
            )
        }

    val visibleNotices: List<RemoteNotice>
        get() = (notices.filter { it.enabled } + listOfNotNull(latestUpdate))
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt }

    val unreadCount: Int get() = visibleNotices.count { it.id !in readIds }
}

internal val RemoteJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
