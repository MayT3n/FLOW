package com.flow.focus.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.music.MusicSnapshot
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
fun MusicPlayerDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.97f, animationSpec = tween(190)),
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
                            Text("موسیقی آنلاین", style = Flow.type.title, color = Flow.colors.textPrimary)
                            Text("جست‌وجوی سریع در سرویس‌های موسیقی", style = Flow.type.caption, color = Flow.colors.textSecondary)
                        }
                        GlassIconButton(
                            FlowIcons.Close,
                            onDismiss,
                            size = 38.dp,
                            iconSize = 18.dp,
                            contentDescription = "بستن پخش‌کننده",
                        )
                    }
                    OnlineMusicSearchPanel()
                    Text(
                        "MatFocus جست‌وجو را به اپ یا وب سرویس انتخابی می‌فرستد؛ پخش، حساب کاربری و کنترل صدا در همان سرویس انجام می‌شود.",
                        style = Flow.type.caption,
                        color = Flow.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    PrimaryButton("بستن", onDismiss, Modifier.fillMaxWidth())
                }
            }
        }
    }
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
