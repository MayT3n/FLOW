package com.flow.focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.music.MusicCatalog
import com.flow.focus.music.MusicSnapshot
import com.flow.focus.music.MusicTrack
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.FlowSlider
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MiniMusicPlayer(
    vm: AppViewModel,
    snapshot: MusicSnapshot,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = MusicCatalog.tracks.firstOrNull { it.id == snapshot.currentTrackId }
        ?: MusicCatalog.tracks.firstOrNull()
        ?: return
    GlassSurface(
        modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MusicCover(track, Modifier.size(42.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                FlowProgressBar(
                    if (snapshot.durationMs > 0L) snapshot.positionMs.toFloat() / snapshot.durationMs else 0f,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    height = 2.dp,
                )
            }
            GlassIconButton(FlowIcons.ChevronPrev, { vm.previousMusic() }, size = 36.dp, iconSize = 17.dp, contentDescription = "آهنگ قبلی")
            GlassIconButton(
                if (snapshot.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                { vm.toggleMusicPlayback() },
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = if (snapshot.isPlaying) "توقف موسیقی" else "پخش موسیقی",
            )
            GlassIconButton(FlowIcons.ChevronNext, { vm.nextMusic() }, size = 36.dp, iconSize = 17.dp, contentDescription = "آهنگ بعدی")
        }
    }
}

@Composable
fun MusicPlayerDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val snapshot by vm.musicSnapshot.collectAsStateWithLifecycle()
    val favorites by vm.musicFavorites.collectAsStateWithLifecycle()
    val downloads by vm.musicDownloadProgress.collectAsStateWithLifecycle()
    val track = MusicCatalog.tracks.firstOrNull { it.id == snapshot.currentTrackId }
        ?: MusicCatalog.tracks.first()
    val duration = snapshot.durationMs.coerceAtLeast(0L)

    FlowDialog(onDismiss, "پخش موسیقی") {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MusicCover(track, Modifier.size(196.dp))
            Text(track.title, style = Flow.type.title, color = Flow.colors.textPrimary, maxLines = 1)
            Text(track.artist + " · " + track.mood, style = Flow.type.caption, color = Flow.colors.textSecondary)
            FlowSlider(
                value = if (duration > 0L) snapshot.positionMs.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                onChange = { if (duration > 0L) vm.seekMusic(it.toLong()) },
                range = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clock(snapshot.positionMs), style = Flow.type.caption, color = Flow.colors.textSecondary)
                Text("-" + clock((duration - snapshot.positionMs).coerceAtLeast(0L)), style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                GlassIconButton(FlowIcons.Repeat, { vm.cycleMusicRepeat() }, size = 42.dp, iconSize = 20.dp, contentDescription = "تکرار")
                GlassIconButton(FlowIcons.ChevronPrev, { vm.previousMusic() }, size = 48.dp, iconSize = 23.dp, contentDescription = "قبلی")
                GlassIconButton(
                    if (snapshot.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                    { vm.toggleMusicPlayback() },
                    size = 58.dp,
                    iconSize = 28.dp,
                    contentDescription = "پخش یا توقف",
                )
                GlassIconButton(FlowIcons.ChevronNext, { vm.nextMusic() }, size = 48.dp, iconSize = 23.dp, contentDescription = "بعدی")
                GlassIconButton(FlowIcons.Sparkles, { vm.toggleMusicShuffle() }, size = 42.dp, iconSize = 20.dp, contentDescription = "پخش تصادفی")
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FlowIcon(FlowIcons.Volume, tint = Flow.colors.textSecondary, size = 20.dp)
                FlowSlider(
                    value = snapshot.volume,
                    onChange = vm::setMusicVolume,
                    range = 0f..1f,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("فهرست پخش", style = Flow.type.heading, color = Flow.colors.textPrimary)
        MusicCatalog.tracks.forEach { item ->
            val selected = item.id == snapshot.currentTrackId
            GlassSurface(
                Modifier.fillMaxWidth().springClick(onClick = { vm.playMusic(item.id) }),
                shape = RoundedCornerShape(17.dp),
                level = if (selected) com.flow.focus.ui.glass.GlassLevel.Subtle else com.flow.focus.ui.glass.GlassLevel.Subtle,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    MusicCover(item, Modifier.size(38.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(item.title, style = Flow.type.label, color = if (selected) Flow.colors.accent else Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.mood, style = Flow.type.caption, color = Flow.colors.textSecondary)
                    }
                    GlassIconButton(
                        FlowIcons.Flame,
                        { vm.toggleMusicFavorite(item.id) },
                        size = 34.dp,
                        iconSize = 16.dp,
                        contentDescription = if (item.id in favorites) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                    )
                    val progress = downloads[item.id]
                    when {
                        progress != null -> Text(num("${(progress * 100).toInt()}٪"), style = Flow.type.caption, color = Flow.colors.accent)
                        vm.isMusicDownloaded(item.id) -> GlassIconButton(FlowIcons.Trash, { vm.removeMusicDownload(item.id) }, size = 34.dp, iconSize = 16.dp, contentDescription = "حذف دانلود")
                        else -> GlassIconButton(FlowIcons.Export, { vm.downloadMusicTrack(item.id) }, size = 34.dp, iconSize = 16.dp, contentDescription = "دانلود برای پخش آفلاین")
                    }
                }
            }
        }
        Text(
            "آهنگ‌ها با مجوز CC0 1.0 از AERIS MUSIC ارائه شده‌اند. حجم دانلودشده: ${formatBytes(vm.downloadedMusicBytes())}",
            style = Flow.type.caption,
            color = Flow.colors.textSecondary,
        )
        PrimaryButton("بستن", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MusicCover(track: MusicTrack, modifier: Modifier = Modifier) {
    val palettes = listOf(
        listOf(Color(0xFF203047), Color(0xFF6C7F9E)),
        listOf(Color(0xFF463A55), Color(0xFFB68A9D)),
        listOf(Color(0xFF1E4B4A), Color(0xFF86B5A6)),
        listOf(Color(0xFF604632), Color(0xFFD0A26E)),
        listOf(Color(0xFF293C5B), Color(0xFF7C9CCB)),
    )
    val colors = palettes[(track.id.hashCode().ushr(1) % palettes.size)]
    Box(
        modifier.clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        FlowIcon(FlowIcons.Music, tint = Color.White.copy(alpha = 0.94f), size = 26.dp)
    }
}

private fun clock(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
