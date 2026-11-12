package com.flow.focus.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.core.formatClock
import com.flow.focus.music.MusicCatalog
import com.flow.focus.music.MusicSnapshot
import com.flow.focus.music.MusicTrack
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalGlass

@Composable
fun MiniMusicPlayer(
    vm: AppViewModel,
    snapshot: MusicSnapshot,
    onOpen: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTrack = snapshot.currentTrackId != null
    GlassSurface(
        modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        level = GlassLevel.Strong,
        elevated = true,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassSurface(Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), level = GlassLevel.Subtle) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FlowIcon(FlowIcons.Music, tint = Flow.colors.accent, size = 21.dp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (hasTrack) "موسیقی در حال پخش" else "موسیقی آنلاین",
                    style = Flow.type.label,
                    color = Flow.colors.textPrimary,
                    maxLines = 1,
                )
                Text(
                    if (hasTrack) "برای کنترل لمس کن" else "جست‌وجو در سرویس‌های رسمی",
                    style = Flow.type.caption,
                    color = Flow.colors.textSecondary,
                    maxLines = 1,
                )
                if (hasTrack) {
                    FlowProgressBar(
                        if (snapshot.durationMs > 0L) snapshot.positionMs.toFloat() / snapshot.durationMs else 0f,
                        Modifier.fillMaxWidth(),
                        height = 2.dp,
                    )
                }
            }
            if (hasTrack) {
                GlassIconButton(
                    if (snapshot.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                    { vm.toggleMusicPlayback() },
                    size = 38.dp,
                    iconSize = 19.dp,
                    contentDescription = if (snapshot.isPlaying) "توقف موسیقی" else "پخش موسیقی",
                )
            } else {
                FlowIcon(FlowIcons.Export, tint = Flow.colors.accent, size = 20.dp)
            }
            GlassIconButton(
                FlowIcons.ChevronDown,
                onCollapse,
                size = 34.dp,
                iconSize = 17.dp,
                contentDescription = "جمع کردن پخش‌کننده",
            )
        }
    }
}

@Composable
fun MusicShelfRestoreButton(onExpand: () -> Unit) {
    GlassSurface(
        Modifier.springClick(scaleDown = 0.97f, onClick = onExpand),
        shape = RoundedCornerShape(50),
        level = GlassLevel.Strong,
        elevated = true,
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowIcon(FlowIcons.Music, tint = Flow.colors.accent, size = 17.dp)
            Text("نمایش پخش‌کننده", style = Flow.type.caption, color = Flow.colors.textPrimary)
            FlowIcon(FlowIcons.ChevronUp, tint = Flow.colors.textSecondary, size = 15.dp)
        }
    }
}

@Composable
fun MusicPlayerDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val animations = LocalGlass.current.animations
    val snapshot by vm.musicSnapshot.collectAsStateWithLifecycle()
    val downloadProgress by vm.musicDownloadProgress.collectAsStateWithLifecycle()
    val currentTrack = MusicCatalog.tracks.firstOrNull { it.id == snapshot.currentTrackId }
    val totalBytes = MusicCatalog.tracks.sumOf { it.fileSize }
    val downloadedBytes = vm.downloadedMusicBytes()
    var visible by remember { mutableStateOf(true) }
    val requestDismiss = { if (visible) visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            if (animations) delay(175)
            onDismiss()
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = requestDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = if (animations) fadeIn(tween(160)) + scaleIn(initialScale = 0.97f, animationSpec = tween(190)) else EnterTransition.None,
            exit = if (animations) fadeOut(tween(115)) + scaleOut(targetScale = 0.97f, animationSpec = tween(140)) else ExitTransition.None,
        ) {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(30.dp),
                level = GlassLevel.Strong,
                elevated = true,
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 660.dp).verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("کتابخانهٔ موسیقی", style = Flow.type.title, color = Flow.colors.textPrimary)
                            Text("آفلاین، سبک و مناسب زمان تمرکز", style = Flow.type.caption, color = Flow.colors.textSecondary)
                        }
                        GlassIconButton(
                            FlowIcons.Close,
                            requestDismiss,
                            size = 38.dp,
                            iconSize = 18.dp,
                            contentDescription = "بستن پخش‌کننده",
                        )
                    }
                    if (currentTrack != null) {
                        CurrentTrackPanel(vm, currentTrack, snapshot)
                    }
                    Text("موسیقی‌های آفلاین", style = Flow.type.heading, color = Flow.colors.textPrimary)
                    Text(
                        "چهار قطعهٔ کوتاه با مجوز CC0؛ مجموع فایل‌ها حدود ۱۱٫۹ مگابایت است.",
                        style = Flow.type.caption,
                        color = Flow.colors.textSecondary,
                    )
                    MusicCatalog.tracks.forEach { track ->
                        OfflineTrackCard(
                            track = track,
                            isCurrent = snapshot.currentTrackId == track.id,
                            isPlaying = snapshot.isPlaying && snapshot.currentTrackId == track.id,
                            isDownloaded = vm.isMusicDownloaded(track.id),
                            downloadProgress = downloadProgress[track.id],
                            onPlay = { vm.playMusic(track.id) },
                            onDownload = { vm.downloadMusicTrack(track.id) },
                            onRemove = { vm.removeMusicDownload(track.id) },
                        )
                    }
                    Text(
                        "حجم دانلودشده: ${formatBytes(downloadedBytes)} از ${formatBytes(50L * 1024L * 1024L)} · آهنگ‌های Spotify و YouTube Music در سرویس رسمی خودشان باز می‌شوند.",
                        style = Flow.type.caption,
                        color = Flow.colors.textSecondary,
                    )
                    Text("سرویس‌های آنلاین", style = Flow.type.heading, color = Flow.colors.textPrimary)
                    OnlineMusicSearchPanel()
                    PrimaryButton("بستن", requestDismiss, Modifier.fillMaxWidth())
                }
            }
        }
    }
}


@Composable
private fun CurrentTrackPanel(
    vm: AppViewModel,
    track: MusicTrack,
    snapshot: MusicSnapshot,
) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), level = GlassLevel.Strong) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                TrackCover(track, 54.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("اکنون در حال پخش", style = Flow.type.caption, color = Flow.colors.textSecondary)
                    Text(track.title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
            }
            FlowProgressBar(
                if (snapshot.durationMs > 0L) (snapshot.positionMs.toFloat() / snapshot.durationMs).coerceIn(0f, 1f) else 0f,
                Modifier.fillMaxWidth(),
                height = 4.dp,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                GlassIconButton(FlowIcons.ChevronBack, { vm.previousMusic() }, size = 40.dp, contentDescription = "آهنگ قبلی")
                GlassIconButton(
                    if (snapshot.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                    { vm.toggleMusicPlayback() },
                    size = 48.dp,
                    iconSize = 21.dp,
                    contentDescription = if (snapshot.isPlaying) "توقف" else "پخش",
                )
                GlassIconButton(FlowIcons.ChevronNext, { vm.nextMusic() }, size = 40.dp, contentDescription = "آهنگ بعدی")
            }
        }
    }
}

@Composable
private fun OfflineTrackCard(
    track: MusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), level = GlassLevel.Regular) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrackCover(track, 58.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(track.title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.mood, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                    Text("${track.durationLabel} · ${formatBytes(track.fileSize.toLong())} · CC0", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
                GlassIconButton(
                    if (isCurrent && isPlaying) FlowIcons.Pause else FlowIcons.Play,
                    onPlay,
                    size = 40.dp,
                    iconSize = 18.dp,
                    contentDescription = if (isCurrent && isPlaying) "پخش مجدد یا ادامه" else "پخش آهنگ",
                )
            }
            if (downloadProgress != null) {
                FlowProgressBar(downloadProgress, Modifier.fillMaxWidth(), height = 4.dp)
                Text("در حال دانلود · ${(downloadProgress * 100).toInt()}٪", style = Flow.type.caption, color = Flow.colors.textSecondary)
            } else if (isDownloaded) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ذخیره‌شده برای پخش آفلاین", Modifier.weight(1f), style = Flow.type.caption, color = Flow.colors.success)
                    GlassButton("حذف دانلود", onRemove)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("دانلود برای استفادهٔ آفلاین", Modifier.weight(1f), style = Flow.type.caption, color = Flow.colors.textSecondary)
                    GlassButton("دانلود", onDownload)
                }
            }
        }
    }
}

/** Original in-app cover art, using a distinct calm motif for each licensed track. */
@Composable
private fun TrackCover(track: MusicTrack, edge: androidx.compose.ui.unit.Dp) {
    val palette = when (track.id) {
        "bedroom-window-outlands" -> listOf(Color(0xFF132E43), Color(0xFF45657B), Color(0xFFB4A9A0))
        "quiet-room" -> listOf(Color(0xFF172B46), Color(0xFF4E7188), Color(0xFF9BB8BA))
        "afterglow-vast" -> listOf(Color(0xFF593B49), Color(0xFFB56D58), Color(0xFFE6BC82))
        else -> listOf(Color(0xFF4B4B55), Color(0xFF8B7568), Color(0xFFD2BFA2))
    }
    Canvas(Modifier.size(edge).clip(RoundedCornerShape(15.dp))) {
        drawRect(Brush.linearGradient(palette, start = Offset.Zero, end = Offset(size.width, size.height)))
        val center = Offset(size.width * 0.66f, size.height * 0.34f)
        val radius = size.minDimension * 0.23f
        drawCircle(Color.White.copy(alpha = 0.28f), radius = radius * 1.5f, center = center)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.72f), palette[2]), center, radius), radius, center)
        if (track.id == "bedroom-window-outlands") {
            val w = size.width * 0.34f
            val h = size.height * 0.54f
            drawRect(Color(0xFF16293B).copy(alpha = 0.78f), Offset(size.width * 0.12f, size.height * 0.28f), Size(w, h))
            drawLine(Color.White.copy(alpha = 0.52f), Offset(size.width * 0.12f + w / 2, size.height * 0.28f), Offset(size.width * 0.12f + w / 2, size.height * 0.28f + h), 1.5.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.52f), Offset(size.width * 0.12f, size.height * 0.55f), Offset(size.width * 0.12f + w, size.height * 0.55f), 1.5.dp.toPx())
        } else {
            drawLine(Color.White.copy(alpha = 0.22f), Offset(size.width * 0.08f, size.height * 0.78f), Offset(size.width * 0.92f, size.height * 0.78f), 1.3.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.17f), Offset(size.width * 0.12f, size.height * 0.84f), Offset(size.width * 0.75f, size.height * 0.84f), 1.dp.toPx())
        }
    }
}

private fun formatBytes(value: Long): String = when {
    value >= 1024L * 1024L -> "${"%.1f".format(java.util.Locale.US, value / (1024f * 1024f))} MB"
    value >= 1024L -> "${value / 1024L} KB"
    else -> "${value} B"
}

@Composable
fun MusicSettings() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "عبارت دلخواهت را بنویس و همان جست‌وجو را در یکی از سرویس‌های رسمی باز کن.",
            style = Flow.type.caption,
            color = Flow.colors.textSecondary,
        )
        OnlineMusicSearchPanel()
        Text(
            "پخش مستقیم داخل MatFocus فعال نیست؛ هیچ آهنگ نمایشی یا دانلود بی‌مجوزی جایگزین نشده است.",
            style = Flow.type.caption,
            color = Flow.colors.textSecondary,
        )
    }
}

@Composable
private fun OnlineMusicSearchPanel() {
    val uriHandler = LocalUriHandler.current
    var query by rememberSaveable { mutableStateOf("Deep Focus instrumental") }
    val encodedQuery = Uri.encode(query.trim().ifBlank { "focus music" })

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GlassSurface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            level = GlassLevel.Regular,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("جست‌وجوی موسیقی", style = Flow.type.label, color = Flow.colors.textSecondary)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp),
                    singleLine = true,
                    textStyle = Flow.type.body.copy(color = Flow.colors.textPrimary),
                    cursorBrush = SolidColor(Flow.colors.accent),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isBlank()) {
                                Text(
                                    "مثلاً lofi، تمرکز یا صدای باران",
                                    style = Flow.type.body,
                                    color = Flow.colors.textSecondary,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
        MusicServiceCard("YouTube Music", "جست‌وجوی موسیقی و میکس‌های تمرکز", FlowIcons.Play) {
            runCatching { uriHandler.openUri("https://music.youtube.com/search?q=$encodedQuery") }
        }
        MusicServiceCard("Spotify", "جست‌وجوی پلی‌لیست‌ها و آهنگ‌ها", FlowIcons.Music) {
            runCatching { uriHandler.openUri("https://open.spotify.com/search/$encodedQuery") }
        }
        MusicServiceCard("YouTube", "میکس‌های طولانی و صدای محیط", FlowIcons.Play) {
            runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$encodedQuery") }
        }
    }
}

@Composable
private fun MusicServiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    GlassSurface(
        Modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        level = GlassLevel.Regular,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassSurface(Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), level = GlassLevel.Subtle) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FlowIcon(icon, tint = Flow.colors.accent, size = 21.dp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary)
                Text(subtitle, style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            FlowIcon(FlowIcons.ChevronNext, tint = Flow.colors.textSecondary, size = 18.dp)
        }
    }
}
