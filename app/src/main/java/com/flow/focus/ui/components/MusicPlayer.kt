package com.flow.focus.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flow.focus.platform.MusicCatalog
import com.flow.focus.platform.MusicController
import com.flow.focus.platform.MusicPlaybackState
import com.flow.focus.platform.MusicTrack
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow

@Composable
fun MusicMiniPlayer(
    state: MusicPlaybackState,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = MusicCatalog.tracks.firstOrNull { it.id == state.trackId } ?: return
    GlassSurface(
        modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = onExpand),
        shape = RoundedCornerShape(22.dp),
        level = GlassLevel.Strong,
        elevated = true,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            MusicArtwork(track, Modifier.size(42.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1)
                Text("AERIS MUSIC · CC0", style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
            }
            GlassIconButton(
                if (state.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                onToggle,
                size = 36.dp,
                iconSize = 18.dp,
                contentDescription = if (state.isPlaying) "مکث موسیقی" else "پخش موسیقی",
            )
            GlassIconButton(FlowIcons.Skip, onNext, size = 36.dp, iconSize = 17.dp, contentDescription = "آهنگ بعدی")
        }
    }
}

@Composable
fun MusicPlayerDialog(
    state: MusicPlaybackState,
    controller: MusicController,
    onDismiss: () -> Unit,
) {
    val current = MusicCatalog.tracks.firstOrNull { it.id == state.trackId } ?: MusicCatalog.tracks.first()
    FlowDialog(onDismiss, "پخش موسیقی") {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MusicArtwork(current, Modifier.size(196.dp))
            Text(current.title, style = Flow.type.title, color = Flow.colors.textPrimary)
            Text("AERIS MUSIC · CC0 1.0 · ${current.mood}", style = Flow.type.caption, color = Flow.colors.textSecondary)

            Slider(
                value = if (state.durationMs > 0L) state.positionMs.coerceIn(0L, state.durationMs).toFloat() else 0f,
                onValueChange = { controller.seekTo(it.toLong()) },
                valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
                enabled = state.durationMs > 0L,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(timeText(state.positionMs), style = Flow.type.caption, color = Flow.colors.textSecondary)
                Text("-${timeText((state.durationMs - state.positionMs).coerceAtLeast(0L))}", style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(FlowIcons.Shuffle, controller::toggleShuffle, size = 42.dp, contentDescription = "تصادفی")
                GlassIconButton(FlowIcons.ChevronPrev, controller::previous, size = 48.dp, iconSize = 22.dp, contentDescription = "قبلی")
                GlassIconButton(
                    if (state.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                    controller::togglePlayPause,
                    size = 58.dp,
                    iconSize = 25.dp,
                    contentDescription = if (state.isPlaying) "مکث" else "پخش",
                )
                GlassIconButton(FlowIcons.Skip, controller::next, size = 48.dp, iconSize = 22.dp, contentDescription = "بعدی")
                GlassIconButton(
                    FlowIcons.Repeat,
                    controller::cycleRepeat,
                    size = 42.dp,
                    contentDescription = when (state.repeatMode) {
                        1 -> "تکرار همه"
                        2 -> "تکرار آهنگ"
                        else -> "بدون تکرار"
                    },
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowIcon(FlowIcons.Volume, tint = Flow.colors.textSecondary, size = 19.dp)
                Slider(value = state.volume, onValueChange = controller::setVolume, modifier = Modifier.weight(1f))
                Text("${(state.volume * 100).toInt()}%", style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            val favorite = current.id in state.favorites
            GlassButton(
                if (favorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                { controller.toggleFavorite(current.id) },
                Modifier.fillMaxWidth(),
                icon = FlowIcons.Award,
            )
            Spacer(Modifier.height(2.dp))
            Text("فهرست پخش", Modifier.fillMaxWidth(), style = Flow.type.heading, color = Flow.colors.textPrimary)
            MusicCatalog.tracks.forEachIndexed { index, track ->
                val selected = track.id == state.trackId
                GlassSurface(
                    Modifier.fillMaxWidth().springClick(scaleDown = 0.99f, onClick = { controller.playTrack(index) }),
                    shape = RoundedCornerShape(16.dp),
                    level = if (selected) GlassLevel.Medium else GlassLevel.Subtle,
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MusicArtwork(track, Modifier.size(40.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1)
                            Text(track.mood, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 1)
                        }
                        if (selected && state.isPlaying) FlowIcon(FlowIcons.Volume, tint = Flow.colors.accent, size = 18.dp)
                        else Text(track.durationLabel, style = Flow.type.caption, color = Flow.colors.textSecondary)
                        if (track.id in state.favorites) FlowIcon(FlowIcons.Award, tint = Flow.colors.accent, size = 16.dp)
                    }
                }
            }
            Text(
                "پخش اینترنتی است؛ برای دریافت فایل‌های آفلاین باید اتصال اینترنت برقرار باشد.",
                Modifier.fillMaxWidth(),
                style = Flow.type.caption,
                color = Flow.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MusicArtwork(track: MusicTrack, modifier: Modifier = Modifier) {
    val index = MusicCatalog.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    val palettes = listOf(
        listOf(Color(0xFF172C42), Color(0xFF63879B)),
        listOf(Color(0xFF3A2448), Color(0xFFDB9B91)),
        listOf(Color(0xFF293B36), Color(0xFF9BB68B)),
        listOf(Color(0xFF323047), Color(0xFF9C8CB8)),
        listOf(Color(0xFF334050), Color(0xFFB6A47A)),
    )
    val colors = palettes[index % palettes.size]
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(27.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            FlowIcon(FlowIcons.Music, tint = Color.White, size = 17.dp)
        }
    }
}

private fun timeText(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
