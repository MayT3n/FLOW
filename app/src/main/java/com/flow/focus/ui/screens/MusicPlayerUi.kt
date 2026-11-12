package com.flow.focus.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.core.formatClock
import com.flow.focus.music.MusicCatalog
import com.flow.focus.music.MusicSnapshot
import com.flow.focus.music.MusicTrack
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.FlowSlider
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import androidx.media3.common.Player

@Composable
fun MiniMusicPlayer(
    vm: AppViewModel,
    snapshot: MusicSnapshot,
    onOpen: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTrack = MusicCatalog.tracks.firstOrNull { it.id == snapshot.currentTrackId } ?: return
    GlassSurface(
        modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        level = GlassLevel.Strong,
        elevated = true,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TrackCover(currentTrack, 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(currentTrack.title, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(currentTrack.artist, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                    Box(Modifier.size(3.dp).clip(CircleShape).background(Flow.colors.track))
                    Text(if (snapshot.isPlaying) "در حال پخش" else "متوقف", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
                FlowProgressBar(
                    if (snapshot.durationMs > 0L) snapshot.positionMs.toFloat() / snapshot.durationMs else 0f,
                    Modifier.fillMaxWidth(),
                    height = 2.dp,
                )
            }
            GlassIconButton(
                if (snapshot.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                { vm.toggleMusicPlayback() },
                size = 36.dp,
                iconSize = 18.dp,
                contentDescription = if (snapshot.isPlaying) "توقف موسیقی" else "پخش موسیقی",
            )
            GlassIconButton(FlowIcons.ChevronDown, onCollapse, size = 32.dp, iconSize = 16.dp, contentDescription = "جمع کردن پخش‌کننده")
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
            Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            FlowIcon(FlowIcons.Music, tint = Flow.colors.accent, size = 16.dp)
            Text("نمایش پخش‌کننده", style = Flow.type.caption, color = Flow.colors.textPrimary)
            FlowIcon(FlowIcons.ChevronUp, tint = Flow.colors.textSecondary, size = 14.dp)
        }
    }
}

@Composable
fun MusicPlayerDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val snapshot by vm.musicSnapshot.collectAsStateWithLifecycle()
    val favorites by vm.musicFavorites.collectAsStateWithLifecycle()
    val downloadProgress by vm.musicDownloadProgress.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(0) }
    val allTracks = MusicCatalog.tracks
    val visibleTracks = remember(filter, favorites) {
        if (filter == 0) allTracks else allTracks.filter { it.id in favorites }
    }
    val currentTrack = allTracks.firstOrNull { it.id == snapshot.currentTrackId }
    val totalBytes = allTracks.sumOf { it.fileSize }
    val downloadedBytes = vm.downloadedMusicBytes()

    FlowDialog(onDismiss = onDismiss, title = "موسیقی") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("کتابخانه‌ی تمرکز", style = Flow.type.bodyStrong, color = Flow.colors.textPrimary)
                    Text("قطعه‌های آفلاین، بدون سرویس اضافه", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
                Text(
                    "${num(allTracks.size)} قطعه",
                    style = Flow.type.caption,
                    color = Flow.colors.textSecondary,
                )
            }

            if (currentTrack != null) {
                CurrentTrackPanel(vm, currentTrack, snapshot)
            } else {
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), level = GlassLevel.Subtle) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Flow.colors.accentSoft),
                            contentAlignment = Alignment.Center,
                        ) { FlowIcon(FlowIcons.Music, tint = Flow.colors.accent, size = 21.dp) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("هنوز چیزی پخش نمی‌شود", style = Flow.type.bodyStrong, color = Flow.colors.textPrimary)
                            Text("برای شروع، یکی از قطعه‌های پایین را انتخاب کن.", style = Flow.type.caption, color = Flow.colors.textSecondary)
                        }
                    }
                }
            }

            SegmentedControl(
                listOf("همه‌ی قطعه‌ها", "علاقه‌مندی‌ها"),
                filter,
                { filter = it },
            )

            if (visibleTracks.isEmpty()) {
                EmptyState(
                    FlowIcons.Heart,
                    "هنوز قطعه‌ای ذخیره نشده",
                    "با زدن قلب کنار هر قطعه، آن را اینجا نگه دار.",
                )
            } else {
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), level = GlassLevel.Subtle) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp)) {
                        visibleTracks.forEachIndexed { index, track ->
                            MusicTrackRow(
                                track = track,
                                isCurrent = snapshot.currentTrackId == track.id,
                                isPlaying = snapshot.isPlaying && snapshot.currentTrackId == track.id,
                                isFavorite = track.id in favorites,
                                isDownloaded = vm.isMusicDownloaded(track.id),
                                downloadProgress = downloadProgress[track.id],
                                onPlay = {
                                    if (snapshot.currentTrackId == track.id && snapshot.isPlaying) vm.toggleMusicPlayback()
                                    else vm.playMusic(track.id)
                                },
                                onFavorite = { vm.toggleMusicFavorite(track.id) },
                                onDownload = { vm.downloadMusicTrack(track.id) },
                            )
                            if (index < visibleTracks.lastIndex) {
                                Box(Modifier.fillMaxWidth().padding(start = 54.dp).height(1.dp).background(Flow.colors.track))
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("حافظه‌ی استفاده‌شده", style = Flow.type.caption, color = Flow.colors.textSecondary)
                Text(
                    "${formatBytes(downloadedBytes)} از ${formatBytes(totalBytes)}",
                    style = Flow.type.caption,
                    color = Flow.colors.textSecondary,
                )
            }
            FlowProgressBar(
                if (totalBytes > 0L) downloadedBytes.toFloat() / totalBytes else 0f,
                Modifier.fillMaxWidth(),
                height = 4.dp,
            )
            Text(
                "فایل‌ها در صورت نیاز دانلود می‌شوند و برای پخش آفلاین در حافظه‌ی برنامه می‌مانند. همه‌ی قطعه‌های این فهرست با مجوز CC0 ارائه شده‌اند.",
                style = Flow.type.caption,
                color = Flow.colors.textSecondary,
            )
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
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrackCover(track, 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("اکنون در حال پخش", style = Flow.type.caption, color = Flow.colors.accent)
                    Text(track.title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.mood, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                }
                Text(track.durationLabel, style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            FlowSlider(
                value = if (snapshot.durationMs > 0L) (snapshot.positionMs.toFloat() / snapshot.durationMs).coerceIn(0f, 1f) else 0f,
                onChange = { vm.seekMusic((it * snapshot.durationMs).toLong()) },
                range = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatClock(snapshot.positionMs / 1_000L), style = Flow.type.caption, color = Flow.colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(formatClock(snapshot.durationMs / 1_000L), style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                GlassIconButton(
                    FlowIcons.Shuffle,
                    { vm.toggleMusicShuffle() },
                    size = 34.dp,
                    iconSize = 16.dp,
                    tint = if (snapshot.shuffle) Flow.colors.accent else Flow.colors.textSecondary,
                    contentDescription = "پخش تصادفی",
                )
                GlassIconButton(FlowIcons.ChevronPrev, { vm.previousMusic() }, size = 38.dp, iconSize = 18.dp, contentDescription = "قطعه‌ی قبلی")
                GlassIconButton(
                    if (snapshot.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                    { vm.toggleMusicPlayback() },
                    size = 46.dp,
                    iconSize = 20.dp,
                    contentDescription = if (snapshot.isPlaying) "توقف" else "پخش",
                )
                GlassIconButton(FlowIcons.ChevronNext, { vm.nextMusic() }, size = 38.dp, iconSize = 18.dp, contentDescription = "قطعه‌ی بعدی")
                GlassIconButton(
                    FlowIcons.Repeat,
                    { vm.cycleMusicRepeat() },
                    size = 34.dp,
                    iconSize = 16.dp,
                    tint = if (snapshot.repeatMode != Player.REPEAT_MODE_OFF) Flow.colors.accent else Flow.colors.textSecondary,
                    contentDescription = "تغییر حالت تکرار",
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FlowIcon(FlowIcons.Volume, tint = Flow.colors.textSecondary, size = 17.dp)
                FlowSlider(
                    value = snapshot.volume.coerceIn(0f, 1f),
                    onChange = vm::setMusicVolume,
                    range = 0f..1f,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MusicTrackRow(
    track: MusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrackCover(track, 46.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.mood + " · " + track.durationLabel, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (downloadProgress != null) {
                FlowProgressBar(downloadProgress, Modifier.fillMaxWidth(), height = 3.dp)
            } else if (isDownloaded) {
                Text("آفلاین", style = Flow.type.caption, color = Flow.colors.success, maxLines = 1)
            }
        }
        GlassIconButton(
            FlowIcons.Heart,
            onFavorite,
            size = 32.dp,
            iconSize = 16.dp,
            tint = if (isFavorite) Flow.colors.danger else Flow.colors.textSecondary,
            contentDescription = if (isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
        )
        GlassIconButton(
            if (isPlaying) FlowIcons.Pause else FlowIcons.Play,
            onPlay,
            size = 36.dp,
            iconSize = 17.dp,
            tint = if (isCurrent) Flow.colors.accent else Flow.colors.textPrimary,
            contentDescription = if (isPlaying) "توقف قطعه" else "پخش قطعه",
        )
        if (!isDownloaded && downloadProgress == null) {
            GlassIconButton(
                FlowIcons.Download,
                onDownload,
                size = 32.dp,
                iconSize = 16.dp,
                contentDescription = "دانلود برای پخش آفلاین",
            )
        } else if (isDownloaded) {
            FlowIcon(FlowIcons.Check, tint = Flow.colors.success, size = 17.dp)
        }
    }
}

@Composable
private fun TrackCover(track: MusicTrack, edge: androidx.compose.ui.unit.Dp) {
    val palette = when (track.id) {
        "bedroom-window-outlands" -> listOf(Color(0xFF132E43), Color(0xFF45657B), Color(0xFFB4A9A0))
        "quiet-room" -> listOf(Color(0xFF172B46), Color(0xFF4E7188), Color(0xFF9BB8BA))
        "afterglow-vast" -> listOf(Color(0xFF593B49), Color(0xFFB56D58), Color(0xFFE6BC82))
        else -> listOf(Color(0xFF4B4B55), Color(0xFF8B7568), Color(0xFFD2BFA2))
    }
    Canvas(Modifier.size(edge).clip(RoundedCornerShape(14.dp))) {
        drawRect(Brush.linearGradient(palette, start = Offset.Zero, end = Offset(size.width, size.height)))
        val center = Offset(size.width * 0.66f, size.height * 0.34f)
        val radius = size.minDimension * 0.23f
        drawCircle(Color.White.copy(alpha = 0.28f), radius * 1.5f, center)
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
        drawArc(
            color = Color.White.copy(alpha = 0.28f),
            startAngle = 205f,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = Offset(size.width * 0.42f, size.height * 0.42f),
            size = Size(size.width * 0.45f, size.height * 0.45f),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private fun formatBytes(value: Long): String = when {
    value >= 1024L * 1024L -> "${"%.1f".format(java.util.Locale.US, value / (1024f * 1024f))} MB"
    value >= 1024L -> "${value / 1024L} KB"
    else -> "${value} B"
}


@Composable
fun MusicSettings() {
    Text(
        "کتابخانه‌ی موسیقی شامل قطعه‌های منتخب با مجوز CC0 است. دانلود اختیاری است و کنترل‌های پخش پس‌زمینه از سیستم اندروید استفاده می‌کنند.",
        style = Flow.type.caption,
        color = Flow.colors.textSecondary,
    )
}
