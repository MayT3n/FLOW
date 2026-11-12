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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.music.MusicCatalog
import com.flow.focus.music.MusicSnapshot
import com.flow.focus.music.MusicTrack
import com.flow.focus.music.MusicPlaylist
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.FlowSlider
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.SectionTitle
import com.flow.focus.ui.components.SettingRow
import com.flow.focus.ui.components.SwitchRow
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import com.flow.focus.data.Settings
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

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
    val playlists by vm.musicPlaylists.collectAsStateWithLifecycle()
    val downloads by vm.musicDownloadProgress.collectAsStateWithLifecycle()
    val downloadRevision by vm.musicDownloadRevision.collectAsStateWithLifecycle()
    var playlistsOpen by remember { mutableStateOf(false) }
    var pickerTrackId by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var downloadedOnly by remember { mutableStateOf(false) }
    var alphabetical by remember { mutableStateOf(false) }
    val downloadErrors by vm.musicDownloadErrors.collectAsStateWithLifecycle()
    val track = MusicCatalog.tracks.firstOrNull { it.id == snapshot.currentTrackId }
        ?: MusicCatalog.tracks.first()
    val duration = snapshot.durationMs.coerceAtLeast(0L)
    val visibleTracks = remember(favoritesOnly, downloadedOnly, alphabetical, favorites, downloadRevision) {
        MusicCatalog.tracks
        .filter { item ->
            when {
                downloadedOnly -> vm.isMusicDownloaded(item.id)
                favoritesOnly -> item.id in favorites
                else -> true
            }
        }
        .let { tracks -> if (alphabetical) tracks.sortedBy { it.title.lowercase() } else tracks }
    }

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
                GlassIconButton(FlowIcons.Repeat, { vm.cycleMusicRepeat() }, size = 42.dp, iconSize = 20.dp, active = snapshot.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF, contentDescription = "تکرار")
                GlassIconButton(FlowIcons.ChevronPrev, { vm.previousMusic() }, size = 48.dp, iconSize = 23.dp, contentDescription = "قبلی")
                GlassIconButton(
                    if (snapshot.isPlaying) FlowIcons.Pause else FlowIcons.Play,
                    { vm.toggleMusicPlayback() },
                    size = 58.dp,
                    iconSize = 28.dp,
                    contentDescription = "پخش یا توقف",
                )
                GlassIconButton(FlowIcons.ChevronNext, { vm.nextMusic() }, size = 48.dp, iconSize = 23.dp, contentDescription = "بعدی")
                GlassIconButton(FlowIcons.Shuffle, { vm.toggleMusicShuffle() }, size = 42.dp, iconSize = 20.dp, active = snapshot.shuffle, contentDescription = "پخش تصادفی")
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

        GlassButton(
            "مدیریت فهرست‌های پخش",
            { playlistsOpen = true },
            Modifier.fillMaxWidth(),
            icon = FlowIcons.Layers,
        )
        Text("کتابخانه", style = Flow.type.heading, color = Flow.colors.textPrimary)
        SegmentedControl(
            options = listOf("همه", "دانلودشده‌ها", "علاقه‌مندی‌ها"),
            selected = when {
                downloadedOnly -> 1
                favoritesOnly -> 2
                else -> 0
            },
            onSelect = {
                downloadedOnly = it == 1
                favoritesOnly = it == 2
            },
            modifier = Modifier.fillMaxWidth(),
        )
        SegmentedControl(
            options = listOf("ترتیب اصلی", "الفبایی"),
            selected = if (alphabetical) 1 else 0,
            onSelect = { alphabetical = it == 1 },
            modifier = Modifier.fillMaxWidth(),
        )
        if (visibleTracks.isEmpty()) {
            Text(
                when {
                    downloadedOnly -> "هنوز آهنگی دانلود نکرده‌ای."
                    favoritesOnly -> "هنوز آهنگی به علاقه‌مندی‌ها اضافه نشده است."
                    else -> "کتابخانه‌ی موسیقی خالی است."
                },
                style = Flow.type.caption,
                color = Flow.colors.textSecondary,
            )
        }
        visibleTracks.forEach { item ->
            val selected = item.id == snapshot.currentTrackId
            GlassSurface(
                Modifier.fillMaxWidth().springClick(onClick = { vm.playMusic(item.id) }),
                shape = RoundedCornerShape(17.dp),
                level = com.flow.focus.ui.glass.GlassLevel.Subtle,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MusicCover(item, Modifier.size(42.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(item.title, style = Flow.type.label, color = if (selected) Flow.colors.accent else Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            downloadErrors[item.id]?.let { "خطا: $it" }
                                ?: if (vm.isMusicDownloaded(item.id)) "دانلودشده · ${item.mood}" else item.mood,
                            style = Flow.type.caption,
                            color = if (downloadErrors[item.id] != null) Flow.colors.danger else Flow.colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    GlassIconButton(
                        FlowIcons.Check,
                        { pickerTrackId = item.id },
                        size = 32.dp,
                        iconSize = 15.dp,
                        contentDescription = "افزودن به فهرست پخش",
                    )
                    GlassIconButton(
                        FlowIcons.Award,
                        { vm.toggleMusicFavorite(item.id) },
                        size = 32.dp,
                        iconSize = 15.dp,
                        active = item.id in favorites,
                        contentDescription = if (item.id in favorites) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                    )
                    val progress = downloads[item.id]
                    when {
                        progress != null -> Text(num("${(progress * 100).toInt()}٪"), style = Flow.type.caption, color = Flow.colors.accent)
                        vm.isMusicDownloaded(item.id) -> GlassIconButton(FlowIcons.Trash, { vm.removeMusicDownload(item.id) }, size = 32.dp, iconSize = 15.dp, contentDescription = "حذف دانلود")
                        else -> GlassIconButton(FlowIcons.Export, { vm.downloadMusicTrack(item.id) }, size = 32.dp, iconSize = 15.dp, contentDescription = "دانلود برای پخش آفلاین")
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

    if (playlistsOpen) {
        MusicPlaylistsDialog(vm, playlists, onDismiss = { playlistsOpen = false })
    }
    pickerTrackId?.let { id ->
        TrackPlaylistPickerDialog(
            track = MusicCatalog.tracks.first { it.id == id },
            playlists = playlists,
            onAdd = { playlistId -> vm.addMusicTrackToPlaylist(playlistId, id); pickerTrackId = null },
            onDismiss = { pickerTrackId = null },
        )
    }
}

@Composable
private fun MusicPlaylistsDialog(
    vm: AppViewModel,
    playlists: List<MusicPlaylist>,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var editingPlaylistId by remember { mutableStateOf<String?>(null) }
    var editingName by remember { mutableStateOf("") }
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    FlowDialog(onDismiss, selectedPlaylist?.name ?: "فهرست‌های من") {
        if (selectedPlaylist != null) {
            Text("${selectedPlaylist.trackIds.size} آهنگ در این فهرست", style = Flow.type.caption, color = Flow.colors.textSecondary)
            selectedPlaylist.trackIds.forEach { trackId ->
                val track = MusicCatalog.tracks.firstOrNull { it.id == trackId } ?: return@forEach
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MusicCover(track, Modifier.size(36.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1)
                            Text(track.mood, style = Flow.type.caption, color = Flow.colors.textSecondary)
                        }
                        GlassIconButton(FlowIcons.Trash, { vm.removeMusicTrackFromPlaylist(selectedPlaylist.id, trackId) }, size = 32.dp, iconSize = 15.dp, contentDescription = "حذف آهنگ از فهرست")
                    }
                }
            }
            if (selectedPlaylist.trackIds.isEmpty()) {
                Text("این فهرست هنوز خالی است. از کتابخانه آهنگ‌ها را به آن اضافه کن.", style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            GlassButton("بازگشت به فهرست‌ها", { selectedPlaylistId = null }, Modifier.fillMaxWidth(), icon = FlowIcons.ChevronPrev)
        } else {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Flow.colors.track.copy(alpha = 0.28f)).padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(48) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    cursorBrush = SolidColor(Flow.colors.accent),
                    textStyle = Flow.type.body.copy(color = Flow.colors.textPrimary),
                    decorationBox = { inner ->
                        Box {
                            if (name.isEmpty()) Text("نام فهرست جدید…", style = Flow.type.body, color = Flow.colors.textSecondary)
                            inner()
                        }
                    },
                )
                GlassIconButton(
                    FlowIcons.Plus,
                    { vm.createMusicPlaylist(name)?.let { name = "" } },
                    size = 34.dp,
                    iconSize = 17.dp,
                    enabled = name.isNotBlank(),
                    contentDescription = "ساخت فهرست",
                )
            }
            if (playlists.isEmpty()) {
                Text("هنوز فهرستی نساخته‌ای. می‌توانی برای Focus، Break یا هر حال‌وهوایی فهرست جدا بسازی.", style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            playlists.forEach { playlist ->
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(playlist.name, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1)
                                Text("${playlist.trackIds.size} آهنگ", style = Flow.type.caption, color = Flow.colors.textSecondary)
                            }
                            GlassIconButton(FlowIcons.Play, { vm.playMusicPlaylist(playlist.id) }, size = 34.dp, iconSize = 16.dp, contentDescription = "پخش فهرست")
                            GlassIconButton(FlowIcons.Edit, {
                                editingPlaylistId = if (editingPlaylistId == playlist.id) null else playlist.id
                                editingName = playlist.name
                            }, size = 34.dp, iconSize = 16.dp, contentDescription = "تغییر نام")
                            GlassIconButton(FlowIcons.Trash, { vm.deleteMusicPlaylist(playlist.id) }, size = 34.dp, iconSize = 16.dp, contentDescription = "حذف فهرست")
                            GlassIconButton(FlowIcons.ChevronNext, { selectedPlaylistId = playlist.id }, size = 34.dp, iconSize = 16.dp, contentDescription = "مشاهده آهنگ‌ها")
                        }
                        if (editingPlaylistId == playlist.id) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Flow.colors.track.copy(alpha = 0.22f)).padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BasicTextField(
                                    value = editingName,
                                    onValueChange = { editingName = it.take(48) },
                                    modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Flow.colors.accent),
                                    textStyle = Flow.type.caption.copy(color = Flow.colors.textPrimary),
                                )
                                GlassIconButton(FlowIcons.Check, {
                                    vm.renameMusicPlaylist(playlist.id, editingName)
                                    editingPlaylistId = null
                                }, size = 30.dp, iconSize = 14.dp, enabled = editingName.isNotBlank(), contentDescription = "ذخیره نام")
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton("بستن", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrackPlaylistPickerDialog(
    track: MusicTrack,
    playlists: List<MusicPlaylist>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    FlowDialog(onDismiss, "افزودن «${track.title}»") {
        if (playlists.isEmpty()) {
            Text("ابتدا از بخش مدیریت فهرست‌ها یک فهرست بساز.", style = Flow.type.caption, color = Flow.colors.textSecondary)
        }
        playlists.forEach { playlist ->
            GlassSurface(
                Modifier.fillMaxWidth().springClick(onClick = { onAdd(playlist.id) }),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(playlist.name, Modifier.weight(1f), style = Flow.type.label, color = Flow.colors.textPrimary)
                    Text("${playlist.trackIds.size} آهنگ", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
            }
        }
        PrimaryButton("بستن", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MusicCover(track: MusicTrack, modifier: Modifier = Modifier) {
    val palettes = listOf(
        listOf(Color(0xFF18283A), Color(0xFF7489A4)),
        listOf(Color(0xFF3C294C), Color(0xFFB98FA3)),
        listOf(Color(0xFF153E3B), Color(0xFF86B5A6)),
        listOf(Color(0xFF563B2D), Color(0xFFD0A26E)),
        listOf(Color(0xFF1F3156), Color(0xFF7C9CCB)),
    )
    val index = (track.id.hashCode().ushr(1) % palettes.size)
    val colors = palettes[index]
    Box(modifier.clip(RoundedCornerShape(15.dp))) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height)))
            val unit = size.minDimension
            val center = Offset(size.width * (if (index % 2 == 0) 0.66f else 0.42f), size.height * 0.48f)
            drawCircle(Color.White.copy(alpha = 0.09f), unit * 0.39f, center)
            drawCircle(Color.White.copy(alpha = 0.06f), unit * 0.29f, center)
            drawArc(
                color = Color.White.copy(alpha = 0.82f),
                startAngle = 205f + index * 11f,
                sweepAngle = 225f,
                useCenter = false,
                topLeft = Offset(center.x - unit * 0.34f, center.y - unit * 0.34f),
                size = Size(unit * 0.68f, unit * 0.68f),
                style = Stroke(width = (unit * 0.018f).coerceAtLeast(1f), cap = StrokeCap.Round),
            )
            val path = Path().apply {
                moveTo(size.width * 0.08f, size.height * (0.58f + index * 0.018f))
                cubicTo(
                    size.width * 0.28f, size.height * 0.36f,
                    size.width * 0.55f, size.height * 0.82f,
                    size.width * 0.91f, size.height * 0.43f,
                )
            }
            drawPath(path, Color.White.copy(alpha = 0.72f), style = Stroke(width = (unit * 0.018f).coerceAtLeast(1f), cap = StrokeCap.Round))
            drawCircle(Color.White.copy(alpha = 0.88f), unit * 0.026f, Offset(size.width * 0.72f, size.height * 0.24f))
            drawRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)),
                size = Size(size.width, size.height * 0.42f),
            )
        }
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
