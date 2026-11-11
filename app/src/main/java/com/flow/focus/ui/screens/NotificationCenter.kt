package com.flow.focus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.flow.focus.remote.NewsItem
import com.flow.focus.remote.UpdateDownloadState
import com.flow.focus.remote.UpdateManifest
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.GlassSurface
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationCenterDialog(
    items: List<NewsItem>,
    manifest: UpdateManifest?,
    readIds: Set<String>,
    downloadState: UpdateDownloadState,
    onRefresh: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDownloadUpdate: (UpdateManifest) -> Unit,
    onInstallUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val updateAvailable = manifest?.let {
        it.updateAvailable && it.versionCode > com.flow.focus.BuildConfig.VERSION_CODE
    } == true

    FlowDialog(onDismiss, "اعلان‌ها") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("اطلاعات از مخزن رسمی MatFocus دریافت می‌شود.", Modifier.weight(1f), style = Flow.type.caption, color = Flow.colors.textSecondary)
            GlassIconButton(FlowIcons.Reset, onRefresh, size = 34.dp, iconSize = 16.dp, contentDescription = "به‌روزرسانی اعلان‌ها")
        }
        if (items.isEmpty() && !updateAvailable) {
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), level = GlassLevel.Subtle) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("فعلاً اعلان تازه‌ای نیست", style = Flow.type.heading, color = Flow.colors.textPrimary)
                    Text("وقتی اطلاعیه‌ای در GitHub منتشر شود، اینجا نمایش داده می‌شود.", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
            }
        }
        if (updateAvailable && manifest != null) {
            UpdateCard(
                manifest = manifest,
                state = downloadState,
                onDownload = { onDownloadUpdate(manifest) },
                onInstall = onInstallUpdate,
            )
        }
        items.forEach { item ->
            val expanded = expandedId == item.id
            val read = item.id in readIds
            GlassSurface(
                Modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = {
                    expandedId = if (expanded) null else item.id
                    if (!read) onMarkRead(item.id)
                }),
                shape = RoundedCornerShape(18.dp),
                level = if (read) GlassLevel.Subtle else GlassLevel.Medium,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowIcon(categoryIcon(item.category), tint = Flow.colors.accent, size = 18.dp)
                        Text(categoryLabel(item.category), style = Flow.type.caption, color = Flow.colors.accent)
                        if (!read) {
                            GlassSurface(Modifier.size(7.dp), shape = RoundedCornerShape(50), level = GlassLevel.Medium) {}
                        }
                        Text(dateLabel(item.publishedAt), Modifier.weight(1f), style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                    }
                    Text(item.title, style = Flow.type.heading, color = Flow.colors.textPrimary)
                    if (expanded) {
                        Text(item.body, style = Flow.type.body, color = Flow.colors.textSecondary)
                        item.url?.takeIf { it.startsWith("https://") }?.let { url ->
                            GlassButton("باز کردن لینک", { uriHandler.openUri(url) }, Modifier.fillMaxWidth(), icon = FlowIcons.Move)
                        }
                        GlassButton(
                            if (read) "علامت‌گذاری به‌عنوان خوانده‌نشده" else "علامت‌گذاری به‌عنوان خوانده‌شده",
                            { if (read) onMarkUnread(item.id) else onMarkRead(item.id) },
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
            GlassButton("علامت‌گذاری همه به‌عنوان خوانده‌شده", onMarkAllRead, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun UpdateCard(
    manifest: UpdateManifest,
    state: UpdateDownloadState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), level = GlassLevel.Strong, elevated = true) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowIcon(FlowIcons.Sparkles, tint = Flow.colors.accent, size = 20.dp)
                Text("نسخه‌ی جدید MatFocus", style = Flow.type.heading, color = Flow.colors.textPrimary)
            }
            Text("نسخه ${manifest.versionName} · کد ${manifest.versionCode}", style = Flow.type.label, color = Flow.colors.textSecondary)
            Text("حجم فایل: ${formatBytes(manifest.fileSize)}", style = Flow.type.caption, color = Flow.colors.textSecondary)
            manifest.changelog.forEach { change ->
                Text("• $change", style = Flow.type.body, color = Flow.colors.textPrimary)
            }
            when (state) {
                is UpdateDownloadState.Downloading -> {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("دریافت فایل: ${(state.progress * 100).toInt()}٪", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
                is UpdateDownloadState.Ready -> {
                    Text("فایل دریافت و اعتبارسنجی شد.", style = Flow.type.caption, color = Flow.colors.success)
                    PrimaryButton("نصب نسخه‌ی جدید", onInstall, Modifier.fillMaxWidth())
                }
                is UpdateDownloadState.Failed -> {
                    Text(state.message, style = Flow.type.caption, color = Flow.colors.danger)
                    PrimaryButton("تلاش دوباره برای دانلود", onDownload, Modifier.fillMaxWidth())
                }
                UpdateDownloadState.Idle -> PrimaryButton(
                    if (manifest.mandatory) "دریافت به‌روزرسانی ضروری" else "دانلود نسخه‌ی جدید",
                    onDownload,
                    Modifier.fillMaxWidth(),
                )
            }
            if (manifest.minimumSupportedVersion > 0) {
                Text("حداقل کد نسخه‌ی پشتیبانی‌شده: ${manifest.minimumSupportedVersion}", style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
        }
    }
}

private fun categoryIcon(category: String) = when (category.lowercase()) {
    "update" -> FlowIcons.Import
    "feature" -> FlowIcons.Sparkles
    "maintenance" -> FlowIcons.Settings
    "important" -> FlowIcons.Info
    else -> FlowIcons.Bell
}

private fun categoryLabel(category: String) = when (category.lowercase()) {
    "update" -> "به‌روزرسانی"
    "feature" -> "قابلیت جدید"
    "maintenance" -> "تعمیر و نگهداری"
    "important" -> "مهم"
    else -> "اخبار"
}

private fun dateLabel(epochMs: Long): String = runCatching {
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy/MM/dd · HH:mm"))
}.getOrDefault("")

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "نامشخص"
    val megabytes = bytes / (1024f * 1024f)
    return if (megabytes >= 1f) String.format("%.1f MB", megabytes) else "${(bytes / 1024L).coerceAtLeast(1)} KB"
}
