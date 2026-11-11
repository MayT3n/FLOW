package com.flow.focus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.BuildConfig
import com.flow.focus.remote.NewsItem
import com.flow.focus.remote.UpdateDownloadState
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MatFocusNotificationsDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val items by vm.newsItems.collectAsStateWithLifecycle()
    val readIds by vm.newsReadIds.collectAsStateWithLifecycle()
    val manifest by vm.updateManifest.collectAsStateWithLifecycle()
    val downloadState by vm.updateDownloadState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var selected by remember { mutableStateOf<NewsItem?>(null) }

    val currentUpdate = manifest?.takeIf {
        it.updateAvailable && it.versionCode > BuildConfig.VERSION_CODE
    }
    val unread = items.count { it.id !in readIds } +
        (currentUpdate?.let { if ("update:${it.versionCode}" !in readIds) 1 else 0 } ?: 0)

    FlowDialog(
        onDismiss,
        selected?.title ?: "اعلان‌های MatFocus",
    ) {
        if (selected != null) {
            val item = selected!!
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(categoryLabel(item.category), style = Flow.type.caption, color = Flow.colors.accent, modifier = Modifier.weight(1f))
                Text(dateLabel(item.publishedAt), style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            Text(item.body, style = Flow.type.body, color = Flow.colors.textPrimary)
            item.url?.takeIf { it.startsWith("https://") }?.let { url ->
                PrimaryButton("باز کردن لینک", { runCatching { uriHandler.openUri(url) } }, Modifier.fillMaxWidth())
            }
            PrimaryButton(
                if (item.id in readIds) "علامت‌گذاری به‌عنوان خوانده‌نشده" else "علامت‌گذاری به‌عنوان خوانده‌شده",
                {
                    if (item.id in readIds) vm.markNewsUnread(item.id) else vm.markNewsRead(item.id)
                },
                Modifier.fillMaxWidth(),
            )
            PrimaryButton("بازگشت به فهرست", { selected = null }, Modifier.fillMaxWidth())
        } else {
            if (unread > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "خواندن همه",
                        Modifier.springClick(onClick = vm::markAllNewsRead).padding(5.dp),
                        style = Flow.type.caption,
                        color = Flow.colors.accent,
                    )
                }
            }

            currentUpdate?.let { update ->
                val id = "update:${update.versionCode}"
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), level = GlassLevel.Strong) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowIcon(FlowIcons.Sparkles, tint = Flow.colors.accent, size = 22.dp)
                            Column(Modifier.weight(1f)) {
                                Text("MatFocus ${update.versionName}", style = Flow.type.heading, color = Flow.colors.textPrimary)
                                Text("نسخه‌ی جدید · کد ${update.versionCode}", style = Flow.type.caption, color = Flow.colors.textSecondary)
                            }
                            if (id !in readIds) Text("جدید", Modifier.springClick(onClick = { vm.markUpdateRead(update.versionCode) }).padding(4.dp), style = Flow.type.caption, color = Flow.colors.accent)
                        }
                        update.changelog.forEach { line ->
                            Text("• $line", style = Flow.type.caption, color = Flow.colors.textSecondary)
                        }
                        Text(
                            "حجم: ${formatBytes(update.fileSize)} · انتشار: ${update.releaseDate}",
                            style = Flow.type.caption,
                            color = Flow.colors.textSecondary,
                        )
                        when (val state = downloadState) {
                            is UpdateDownloadState.Downloading -> {
                                FlowProgressBar(state.progress, color = Flow.colors.accent)
                                Text("دانلود ${(state.progress * 100).toInt()}٪", style = Flow.type.caption, color = Flow.colors.textSecondary)
                            }
                            is UpdateDownloadState.Ready -> {
                                Text("فایل دانلود و اعتبارسنجی شده است.", style = Flow.type.caption, color = Flow.colors.success)
                                PrimaryButton("شروع نصب", { vm.installDownloadedUpdate() }, Modifier.fillMaxWidth())
                            }
                            is UpdateDownloadState.Failed -> {
                                Text(state.message, style = Flow.type.caption, color = Flow.colors.danger)
                                PrimaryButton("تلاش دوباره", vm::downloadUpdate, Modifier.fillMaxWidth())
                            }
                            UpdateDownloadState.Idle -> PrimaryButton("دانلود و بررسی نسخه", vm::downloadUpdate, Modifier.fillMaxWidth())
                        }
                        Text(
                            if (update.mandatory) "این به‌روزرسانی ضروری اعلام شده است." else "نصب با تأیید خودت در نصب‌کننده‌ی رسمی اندروید انجام می‌شود.",
                            style = Flow.type.caption,
                            color = Flow.colors.textSecondary,
                        )
                    }
                }
            } ?: run {
                val latest = manifest
                if (latest != null) {
                    Text("نسخه‌ی نصب‌شده: ${BuildConfig.VERSION_NAME}", style = Flow.type.caption, color = Flow.colors.textSecondary)
                    Text("آخرین نسخه‌ی اعلام‌شده: ${latest.versionName}", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
            }

            if (items.isEmpty()) {
                Text(
                    "فعلاً اطلاعیه‌ی تازه‌ای وجود ندارد.",
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    style = Flow.type.body,
                    color = Flow.colors.textSecondary,
                )
            } else {
                items.forEach { item ->
                    val isRead = item.id in readIds
                    GlassSurface(
                        Modifier.fillMaxWidth().springClick(onClick = {
                            selected = item
                            vm.markNewsRead(item.id)
                        }),
                        shape = RoundedCornerShape(18.dp),
                        level = GlassLevel.Subtle,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FlowIcon(categoryIcon(item.category), tint = if (isRead) Flow.colors.textSecondary else Flow.colors.accent, size = 21.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.title, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(dateLabel(item.publishedAt), style = Flow.type.caption, color = Flow.colors.textSecondary)
                            }
                            if (!isRead) {
                                FlowIcon(FlowIcons.Bell, tint = Flow.colors.accent, size = 16.dp)
                            } else {
                                GlassIconButton(FlowIcons.Check, { vm.markNewsUnread(item.id) }, size = 30.dp, iconSize = 15.dp, contentDescription = "علامت‌گذاری به‌عنوان خوانده‌نشده")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            PrimaryButton("بستن", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

private fun categoryLabel(category: String): String = when (category.lowercase()) {
    "update" -> "به‌روزرسانی"
    "feature" -> "قابلیت جدید"
    "maintenance" -> "تعمیر و نگه‌داری"
    "important" -> "مهم"
    else -> "خبر"
}

private fun categoryIcon(category: String) = when (category.lowercase()) {
    "update" -> FlowIcons.Export
    "feature" -> FlowIcons.Sparkles
    "maintenance" -> FlowIcons.Settings
    "important" -> FlowIcons.Flag
    else -> FlowIcons.Info
}

private fun dateLabel(epochMs: Long): String = runCatching {
    DateTimeFormatter.ofPattern("yyyy/MM/dd · HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))
}.getOrDefault("")

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
