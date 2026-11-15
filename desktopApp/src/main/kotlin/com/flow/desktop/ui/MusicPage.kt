package com.flow.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flow.desktop.DesktopMusicCatalog
import com.flow.desktop.DesktopRuntime
import com.flow.desktop.design.*
import com.flow.focus.data.AppData

@Composable
internal fun MusicPage(runtime: DesktopRuntime, data: AppData) {
    val pal = Flow.colors
    val state by runtime.music.state.collectAsState()
    val downloads by runtime.music.downloads.collectAsState()
    val favorites by runtime.music.favorites.collectAsState()
    val playlists by runtime.music.playlists.collectAsState()
    var query by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }
    val current = DesktopMusicCatalog.tracks.firstOrNull { it.id == state.trackId }
    val shown = remember(query, onlyFavorites, favorites) {
        DesktopMusicCatalog.tracks.filter {
            (query.isBlank() || it.title.contains(query.trim(), true) || it.mood.contains(query.trim(), true)) &&
                (!onlyFavorites || it.id in favorites)
        }
    }
    val playlist = playlists.firstOrNull { it.id == selectedPlaylist }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= 1000.dp
        val player: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 14.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CoverArt(current?.id ?: "flow", size = 120.dp, radius = 20.dp, playing = state.playing)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            FlowText(if (current != null) "در حال پخش" else "پخش‌کننده‌ی FLOW", style = FlowType.captionMedium, color = pal.accent)
                            FlowText(current?.title ?: "آهنگی انتخاب نشده", style = FlowType.title1, maxLines = 2)
                            FlowText(if (current != null) DesktopMusicCatalog.artist + " · " + current.mood else "از کتابخانه یک آهنگ انتخاب کن.", style = FlowType.callout, color = pal.textSecondary, maxLines = 2)
                        }
                        if (current != null) FlowIconButton(
                            FlowIcon.Heart, "علاقه‌مندی", { runtime.music.toggleFavorite(current.id) },
                            selected = current.id in favorites, filledIcon = current.id in favorites,
                        )
                    }
                    MusicProgress(runtime, current != null)
                    LtrRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FlowIconButton(FlowIcon.Shuffle, if (state.shuffle) "تصادفی: روشن" else "تصادفی: خاموش", { runtime.music.toggleShuffle() }, selected = state.shuffle, size = 40.dp)
                        FlowIconButton(FlowIcon.Previous, "قبلی", { runtime.music.previous() }, enabled = current != null, size = 44.dp, iconSize = 22.dp, filledIcon = true)
                        FlowButton(
                            if (state.playing) "مکث" else "پخش",
                            { if (current != null) runtime.music.togglePlayback() else DesktopMusicCatalog.tracks.firstOrNull { it.id == data.settings.musicTrackId }?.let { playOrDownload(runtime, it) } },
                            kind = ButtonKind.Primary, icon = if (state.playing) FlowIcon.Pause else FlowIcon.Play, modifier = Modifier.widthIn(min = 120.dp).height(44.dp),
                        )
                        FlowIconButton(FlowIcon.Next, "بعدی", { runtime.music.next() }, enabled = current != null, size = 44.dp, iconSize = 22.dp, filledIcon = true)
                        FlowIconButton(
                            if (state.repeatMode == 2) FlowIcon.RepeatOne else FlowIcon.Repeat,
                            when (state.repeatMode) { 1 -> "تکرار فهرست"; 2 -> "تکرار آهنگ"; else -> "تکرار: خاموش" },
                            { runtime.music.cycleRepeatMode() }, selected = state.repeatMode != 0, size = 40.dp,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        FlowIconView(FlowIcon.Volume, pal.textSecondary, size = 18.dp)
                        FlowSlider(state.volume, { runtime.music.setVolume(it) }, Modifier.weight(1f))
                        FlowText(num((state.volume * 100).toInt()) + "٪", Modifier.width(40.dp), FlowType.caption, pal.textSecondary)
                        FlowIconButton(FlowIcon.Close, "توقف", { runtime.music.stop() }, enabled = current != null, size = 32.dp, iconSize = 16.dp)
                    }
                    if (!state.error.isNullOrBlank()) FlowText(state.error ?: "", style = FlowType.caption, color = pal.danger)
                }
                GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 10.dp) {
                    PanelHeader("پلی‌لیست‌های من", FlowIcon.Layers)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        FlowTextField(newName, { newName = it }, "نام پلی‌لیست جدید", Modifier.weight(1f), onEnter = {
                            if (newName.isNotBlank()) { selectedPlaylist = runtime.music.createPlaylist(newName) ?: selectedPlaylist; newName = "" }
                        })
                        FlowButton("ساخت", { selectedPlaylist = runtime.music.createPlaylist(newName) ?: selectedPlaylist; newName = "" }, icon = FlowIcon.Plus, enabled = newName.isNotBlank())
                    }
                    if (playlists.isEmpty()) FlowText("هنوز پلی‌لیستی نساخته‌ای.", style = FlowType.caption, color = pal.textSecondary)
                    playlists.forEach { p ->
                        val sel = selectedPlaylist == p.id
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(FlowDims.controlRadius))
                                .background(if (sel) pal.accent.copy(alpha = .12f) else Color.Transparent).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                FlowText(p.name, style = FlowType.bodyMedium, maxLines = 1)
                                FlowText(num(p.trackIds.size) + " آهنگ", style = FlowType.caption, color = pal.textSecondary)
                            }
                            FlowChip(if (sel) "انتخاب‌شده" else "انتخاب", sel, { selectedPlaylist = if (sel) null else p.id })
                            FlowIconButton(FlowIcon.Play, "پخش پلی‌لیست", { runtime.music.playPlaylist(p.id) }, enabled = p.trackIds.isNotEmpty(), size = 32.dp, iconSize = 16.dp, filledIcon = true)
                            ConfirmDeleteButton({ runtime.music.deletePlaylist(p.id); if (selectedPlaylist == p.id) selectedPlaylist = null })
                        }
                    }
                    if (playlist != null) {
                        Hairline()
                        FlowText("آهنگ‌های «${playlist.name}»", style = FlowType.callout, color = pal.textSecondary)
                        if (playlist.trackIds.isEmpty()) FlowText("از کتابخانه با دکمه‌ی + آهنگ اضافه کن.", style = FlowType.caption, color = pal.textTertiary)
                        playlist.trackIds.forEach { id ->
                            val t = DesktopMusicCatalog.tracks.firstOrNull { it.id == id } ?: return@forEach
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                FlowText(t.title, Modifier.weight(1f), FlowType.body, maxLines = 1)
                                FlowIconButton(FlowIcon.Minus, "حذف از پلی‌لیست", { runtime.music.removeFromPlaylist(playlist.id, id) }, size = 30.dp, iconSize = 15.dp)
                            }
                        }
                    }
                }
            }
        }
        val library: @Composable (Modifier) -> Unit = { m ->
            GlassPanel(m, padding = PaddingValues(horizontal = 10.dp, vertical = 14.dp), verticalSpacing = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowText("کتابخانه", Modifier.weight(1f), FlowType.title2)
                    FlowChip("علاقه‌مندی‌ها", onlyFavorites, { onlyFavorites = !onlyFavorites }, icon = FlowIcon.Heart)
                }
                FlowTextField(query, { query = it }, "جست‌وجوی آهنگ یا حال‌وهوا", Modifier.fillMaxWidth().padding(horizontal = 8.dp), leading = FlowIcon.Search)
                if (shown.isEmpty()) EmptyState(FlowIcon.Music, "آهنگی پیدا نشد", "عبارت جست‌وجو یا فیلتر را تغییر بده.", Modifier.weight(1f))
                else LazyColumn(Modifier.weight(1f, fill = true).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(shown, key = { it.id }) { track ->
                        val progress = downloads[track.id]
                        val downloaded = remember(track.id, progress != null) { runtime.music.isDownloaded(track) }
                        val isCur = state.trackId == track.id
                        val ph = rememberPressHover()
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(FlowDims.cardRadius))
                                .background(if (isCur) pal.accent.copy(alpha = .10f) else if (ph.hovered) pal.fillSubtle else Color.Transparent)
                                .hoverableRow(ph).padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            CoverArt(track.id, size = 42.dp, radius = 11.dp, playing = isCur && state.playing)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                FlowText(track.title, style = FlowType.bodyMedium, color = if (isCur) pal.accent else pal.text, maxLines = 1)
                                FlowText(track.mood + " · " + numText(track.durationLabel), style = FlowType.caption, color = pal.textSecondary, maxLines = 1)
                                if (progress != null) HBar(progress, pal.accent, height = 4.dp)
                            }
                            FlowIconButton(FlowIcon.Heart, "علاقه‌مندی", { runtime.music.toggleFavorite(track.id) }, selected = track.id in favorites, filledIcon = track.id in favorites, size = 32.dp, iconSize = 17.dp)
                            if (playlist != null) {
                                val inList = track.id in playlist.trackIds
                                FlowIconButton(if (inList) FlowIcon.Check else FlowIcon.Plus, if (inList) "در پلی‌لیست هست" else "افزودن به «${playlist.name}»", { runtime.music.addToPlaylist(playlist.id, track.id) }, enabled = !inList, size = 32.dp, iconSize = 17.dp)
                            }
                            if (downloaded) {
                                FlowIconButton(
                                    if (isCur && state.playing) FlowIcon.Pause else FlowIcon.Play,
                                    if (isCur && state.playing) "مکث" else "پخش",
                                    { if (isCur) runtime.music.togglePlayback() else runtime.music.play(track) },
                                    selected = isCur, size = 34.dp, filledIcon = true,
                                )
                            } else {
                                FlowIconButton(FlowIcon.Download, "دانلود (${numText(track.fileSize.div(1024 * 1024).toString())} MB)", { runtime.music.download(track) }, enabled = progress == null, size = 34.dp)
                            }
                        }
                    }
                }
                FlowText(
                    "مجوز ${DesktopMusicCatalog.licenseName} · فایل‌ها در پوشه‌ی داده‌ی برنامه ذخیره می‌شوند و بعد از دانلود آفلاین پخش می‌شوند.",
                    Modifier.padding(horizontal = 8.dp), FlowType.caption, pal.textTertiary,
                )
            }
        }
        if (split) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            player(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
            library(Modifier.weight(1f).fillMaxHeight())
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            player(Modifier.fillMaxWidth()); library(Modifier.fillMaxWidth().height(520.dp))
        }
    }
}

private fun Modifier.hoverableRow(ph: PressHover): Modifier = this.then(Modifier.hoverable(ph.source))
