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
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
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
    onCollapse: () -> Unit = {},
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            GlassIconButton(FlowIcons.ChevronDown, onCollapse, size = 30.dp, iconSize = 15.dp, contentDescription = "کوچک‌کردن پلیر")
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
    var alphabetical by remember { mutableStateOf(false) }
    val downloadErrors by vm.musicDownloadErrors.collectAsStateWithLifecycle()
    val track = MusicCatalog.tracks.firstOrNull { it.id == snapshot.currentTrackId }
        ?: MusicCatalog.tracks.first()
    val duration = snapshot.durationMs.coerceAtLeast(0L)
    val visibleTracks = remember(favoritesOnly, alphabetical, favorites, downloadRevision) {
        MusicCatalog.tracks
        .filter { item ->
            when {
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
            options = listOf("همه", "علاقه‌مندی‌ها"),
            selected = if (favoritesOnly) 1 else 0,
            onSelect = { favoritesOnly = it == 1 },
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
                                ?: if (vm.isMusicDownloaded(item.id)) item.mood else "${item.mood} · برای پخش لمس کن تا دانلود شود",
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
                        else -> GlassIconButton(FlowIcons.Download, { vm.playMusic(item.id) }, size = 32.dp, iconSize = 15.dp, contentDescription = "دانلود و پخش")
                    }
                }
            }
        }
        Text(
            "آهنگ‌ها لوفای OpenLo-Fi با مجوز CC0 1.0 اند و بعد از دانلود پخش می‌شوند. حجم دانلودشده: ${formatBytes(vm.downloadedMusicBytes())}",
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

private val coverPalettes = listOf(
    listOf(Color(0xFF14283D), Color(0xFF5E8BB8), Color(0xFFBFE0FF)),
    listOf(Color(0xFF3A2250), Color(0xFFB4709D), Color(0xFFFFD1EA)),
    listOf(Color(0xFF0F3A36), Color(0xFF5FB59E), Color(0xFFCFFFEF)),
    listOf(Color(0xFF52301F), Color(0xFFD58A52), Color(0xFFFFE2BF)),
    listOf(Color(0xFF1B2757), Color(0xFF6F7FE0), Color(0xFFDCE0FF)),
    listOf(Color(0xFF4A1F2C), Color(0xFFD9637C), Color(0xFFFFD6DD)),
    listOf(Color(0xFF233A1A), Color(0xFF86B85A), Color(0xFFEEFFCC)),
    listOf(Color(0xFF2A2F3A), Color(0xFF8E9BB3), Color(0xFFF0F4FF)),
    listOf(Color(0xFF402B12), Color(0xFFE0B04F), Color(0xFFFFF3C2)),
    listOf(Color(0xFF0E3350), Color(0xFF3FB6D9), Color(0xFFD2F6FF)),
    listOf(Color(0xFF3B1F4A), Color(0xFF8E5BD6), Color(0xFFE8D6FF)),
    listOf(Color(0xFF123B2A), Color(0xFF3DBE8B), Color(0xFFD1FFE9)),
    listOf(Color(0xFF4E2A12), Color(0xFFE5733F), Color(0xFFFFDCC4)),
    listOf(Color(0xFF1E2F4F), Color(0xFFE08FA3), Color(0xFFFFE3EA)),
    listOf(Color(0xFF2E2433), Color(0xFFA98BBF), Color(0xFFF4E8FF)),
    listOf(Color(0xFF0B2D3A), Color(0xFF6BD1B4), Color(0xFFE0FFF6)),
)

/** کاور تولیدی؛ هر آهنگ پالت و طرح (خورشید و تپه، حلقه، اکولایزر، موج) و جزئیات مخصوص خود را دارد. */
@Composable
internal fun MusicCover(track: MusicTrack, modifier: Modifier = Modifier, corner: androidx.compose.ui.unit.Dp = 15.dp) {
    val seed = remember(track.id) {
        var h = 7
        track.id.forEach { h = h * 31 + it.code }
        (h xor (h ushr 13)) and 0x7FFFFFFF
    }
    val pal = coverPalettes[seed % coverPalettes.size]
    val motif = (seed / 16) % 4
    val jitter = ((seed / 64) % 10) / 10f
    Box(modifier.clip(RoundedCornerShape(corner))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val u = size.minDimension
            val rnd = kotlin.random.Random(seed)
            drawRect(Brush.linearGradient(listOf(pal[0], pal[1]), start = Offset(0f, 0f), end = Offset(w * 0.8f, h)))
            fun hill(y: Float, amp: Float, phase: Float, alpha: Float, color: Color) {
                val path = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 4f) {
                        lineTo(x, y + kotlin.math.sin(x / w * 6.28f * 1.1f + phase) * amp * h)
                        x += w / 24f
                    }
                    lineTo(w, h); close()
                }
                drawPath(path, color.copy(alpha = alpha))
            }
            when (motif) {
                0 -> {
                    val c = Offset(w * (0.3f + 0.4f * jitter), h * 0.36f)
                    drawCircle(Brush.radialGradient(listOf(pal[2].copy(alpha = 0.55f), Color.Transparent), c, u * 0.7f), u * 0.7f, c)
                    drawCircle(pal[2].copy(alpha = 0.92f), u * 0.17f, c)
                    hill(h * 0.62f, 0.05f, jitter * 5f, 0.45f, pal[0])
                    hill(h * 0.74f, 0.045f, jitter * 3f + 2f, 0.7f, pal[0])
                    hill(h * 0.86f, 0.03f, jitter * 7f, 0.95f, pal[0])
                }
                1 -> {
                    val c = Offset(w * (0.35f + 0.3f * jitter), h * 0.5f)
                    for (i in 1..5) drawCircle(pal[2].copy(alpha = 0.34f - i * 0.04f), u * (0.1f + i * 0.1f), c, style = Stroke(width = u * 0.016f))
                    drawCircle(pal[2].copy(alpha = 0.9f), u * 0.055f, c)
                    drawArc(
                        pal[2].copy(alpha = 0.85f), 200f + jitter * 90f, 110f, false,
                        topLeft = Offset(c.x - u * 0.42f, c.y - u * 0.42f), size = Size(u * 0.84f, u * 0.84f),
                        style = Stroke(width = u * 0.03f, cap = StrokeCap.Round),
                    )
                }
                2 -> {
                    drawCircle(pal[2].copy(alpha = 0.85f), u * 0.1f, Offset(w * (0.25f + 0.5f * jitter), h * 0.24f))
                    val n = 9
                    val gap = w * 0.035f
                    val bw = (w * 0.82f - gap * (n - 1)) / n
                    for (i in 0 until n) {
                        val bh = h * (0.18f + 0.46f * rnd.nextFloat())
                        drawRoundRect(
                            pal[2].copy(alpha = 0.35f + 0.5f * (i / (n - 1f))),
                            Offset(w * 0.09f + i * (bw + gap), h * 0.92f - bh), Size(bw, bh),
                            androidx.compose.ui.geometry.CornerRadius(bw / 2f),
                        )
                    }
                }
                else -> {
                    drawCircle(pal[2].copy(alpha = 0.9f), u * 0.075f, Offset(w * (0.2f + 0.6f * jitter), h * 0.22f))
                    for (i in 0 until 5) hill(h * (0.42f + i * 0.11f), 0.035f + i * 0.004f, jitter * 4f + i * 1.3f, 0.2f + i * 0.14f, pal[2])
                }
            }
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)), size = Size(w, h * 0.45f))
        }
    }
}

/** حالت کوچک‌شده‌ی پلیر بالای داک: کاور چرخان؛ لمس آن نوار مینی را برمی‌گرداند. */
@Composable
fun MusicChip(vm: AppViewModel, snapshot: MusicSnapshot, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val track = MusicCatalog.tracks.firstOrNull { it.id == snapshot.currentTrackId }
    val animations = Flow.glass.animations
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "chip")
    val spin = infinite.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(14_000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spin",
    )
    GlassSurface(modifier.size(56.dp).springClick(onClick = onClick), shape = androidx.compose.foundation.shape.CircleShape) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (track != null) {
                MusicCover(
                    track,
                    Modifier.size(44.dp).graphicsLayer { rotationZ = if (snapshot.isPlaying && animations) spin.value else 0f },
                    corner = 22.dp,
                )
            } else {
                FlowIcon(FlowIcons.Music, tint = Flow.colors.accent, size = 24.dp)
            }
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
