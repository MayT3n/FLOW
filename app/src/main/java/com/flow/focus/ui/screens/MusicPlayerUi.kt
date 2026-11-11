package com.flow.focus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.data.Settings
import com.flow.focus.music.MusicSnapshot
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.GlassButton
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
            MusicArtwork(Modifier.size(42.dp), 42.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (hasTrack) "موسیقی در حال پخش" else "موسیقی آنلاین",
                    style = Flow.type.label,
                    color = Flow.colors.textPrimary,
                    maxLines = 1,
                )
                Text(
                    if (hasTrack) "برای کنترل لمس کن" else "سرویس‌های رسمی موسیقی",
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
            GlassIconButton(FlowIcons.ChevronDown, onCollapse, size = 34.dp, iconSize = 17.dp, contentDescription = "جمع کردن پخش‌کننده")
        }
    }
}

@Composable
fun MusicPlayerDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val snapshot by vm.musicSnapshot.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.97f, animationSpec = tween(190)),
        ) {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(30.dp),
                level = GlassLevel.Strong,
                elevated = true,
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("موسیقی آنلاین", style = Flow.type.title, color = Flow.colors.textPrimary)
                            Text("سرویس‌های رسمی پخش موسیقی", style = Flow.type.caption, color = Flow.colors.textSecondary)
                        }
                        GlassIconButton(FlowIcons.Close, onDismiss, size = 38.dp, iconSize = 18.dp, contentDescription = "بستن پخش‌کننده")
                    }
                    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), level = GlassLevel.Subtle) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MusicArtwork()
                            Text("کتابخانه‌ی آزمایشی حذف شد", style = Flow.type.heading, color = Flow.colors.textPrimary)
                            Text(
                                "آهنگ‌های کوتاه قبلی دیگر در MatFocus نمایش داده نمی‌شوند. برای ارائه‌ی موسیقی واقعی، منبع باید مجوز پخش درون‌برنامه‌ای داشته باشد.",
                                style = Flow.type.caption,
                                color = Flow.colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Text("باز کردن سرویس موسیقی", style = Flow.type.heading, color = Flow.colors.textPrimary)
                    MusicServiceCard(
                        "YouTube Music", "موسیقی آنلاین در سرویس رسمی", FlowIcons.Play,
                    ) { runCatching { uriHandler.openUri("https://music.youtube.com/") } }
                    MusicServiceCard(
                        "Spotify", "جست‌وجو و پخش در سرویس رسمی", FlowIcons.Music,
                    ) { runCatching { uriHandler.openUri("https://open.spotify.com/") } }
                    Text(
                        if (snapshot.connected && snapshot.currentTrackId != null) "یک منبع قدیمی هنوز به نشست پخش متصل است."
                        else "پخش و دانلود درون‌برنامه‌ای فعلاً خاموش است؛ هیچ فایل ساختگی یا دانلود بی‌مجوزی جایگزین نشده است.",
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
fun MusicSettings(vm: AppViewModel, settings: Settings) {
    val snapshot by vm.musicSnapshot.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "برای دسترسی به آهنگ‌های واقعی، سرویس رسمی را باز کن. پخش یا دانلود مستقیم داخل MatFocus تا زمان تأمین مجوز و API معتبر فعال نمی‌شود.",
            style = Flow.type.caption,
            color = Flow.colors.textSecondary,
        )
        MusicServiceCard(
            "YouTube Music", "باز کردن سرویس آنلاین", FlowIcons.Play,
        ) { runCatching { uriHandler.openUri("https://music.youtube.com/") } }
        MusicServiceCard(
            "Spotify", "باز کردن سرویس آنلاین", FlowIcons.Music,
        ) { runCatching { uriHandler.openUri("https://open.spotify.com/") } }
        Text(
            if (snapshot.connected) "وضعیت نشست رسانه: متصل" else "وضعیت نشست رسانه: آماده‌ی اتصال",
            style = Flow.type.caption,
            color = Flow.colors.textSecondary,
        )
        GlassButton(
            "نمایش سرویس‌های موسیقی",
            { runCatching { uriHandler.openUri("https://music.youtube.com/") } },
            Modifier.fillMaxWidth(),
            icon = FlowIcons.Export,
        )
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

@Composable
private fun MusicArtwork(modifier: Modifier = Modifier, size: Dp = 104.dp) {
    Canvas(modifier.size(size).clip(RoundedCornerShape(28.dp))) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF162D49), Color(0xFF3B6280), Color(0xFF90B4B4))))
        drawCircle(Color.White.copy(alpha = 0.10f), this.size.minDimension * 0.40f, center)
        drawCircle(Color.White.copy(alpha = 0.05f), this.size.minDimension * 0.30f, center)
        drawArc(
            color = Color.White.copy(alpha = 0.88f),
            startAngle = 215f,
            sweepAngle = 245f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(this.size.width * 0.18f, this.size.height * 0.18f),
            size = androidx.compose.ui.geometry.Size(this.size.width * 0.64f, this.size.height * 0.64f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = this.size.minDimension * 0.035f),
        )
        drawLine(
            Color.White.copy(alpha = 0.90f),
            start = androidx.compose.ui.geometry.Offset(this.size.width * 0.5f, this.size.height * 0.5f),
            end = androidx.compose.ui.geometry.Offset(this.size.width * 0.69f, this.size.height * 0.31f),
            strokeWidth = this.size.minDimension * 0.035f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
