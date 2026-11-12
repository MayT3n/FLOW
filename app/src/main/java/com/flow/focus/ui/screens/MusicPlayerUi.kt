package com.flow.focus.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow

private data class MusicDestination(val title: String, val description: String, val url: String)

private val streamingDestinations = listOf(
    MusicDestination("YouTube Music", "جست‌وجوی میکس‌های طولانی و موسیقی لوفای", "https://music.youtube.com/search?q=lofi%20study"),
    MusicDestination("Spotify", "پلی‌لیست‌های Lo-fi و Focus", "https://open.spotify.com/search/lofi"),
    MusicDestination("SoundCloud", "ترک‌ها و میکس‌های هنرمندان مستقل", "https://soundcloud.com/search/sounds?q=lofi%20study"),
)

private val downloadDestinations = listOf(
    MusicDestination("Pixabay Music", "دانلود ترک‌های رایگان؛ مجوز هر ترک را بررسی کن", "https://pixabay.com/music/"),
    MusicDestination("StorybookSounds", "مجموعه‌های طولانی Lo-fi و BGM با فایل MP3", "https://storybookSounds.com/en"),
    MusicDestination("Bandcamp", "دانلود مستقیم از هنرمند؛ رایگان یا پولی بسته به انتشار", "https://bandcamp.com/search?q=lofi"),
)

/** Music opens in an external service; MatFocus does not embed or download MP3 tracks. */
@Composable
fun MusicDiscoveryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    FlowDialog(onDismiss, "موسیقی برای مطالعه") {
        Text(
            "برای پخش آنلاین یا دانلود، سرویس دلخواهت را بیرون از MatFocus باز کن.",
            style = Flow.type.body,
            color = Flow.colors.textSecondary,
        )
        MusicDestinationSection("پخش آنلاین", streamingDestinations) { destination ->
            openMusicDestination(context, destination.url)
        }
        MusicDestinationSection("منابع دانلود", downloadDestinations) { destination ->
            openMusicDestination(context, destination.url)
        }
        Text(
            "حجم و کیفیت فایل را در همان سرویس انتخاب کن. مجوز و شرایط استفاده‌ی هر ترک ممکن است متفاوت باشد.",
            style = Flow.type.caption,
            color = Flow.colors.textSecondary,
        )
        PrimaryButton("بستن", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MusicDestinationSection(
    title: String,
    destinations: List<MusicDestination>,
    onOpen: (MusicDestination) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = Flow.type.heading, color = Flow.colors.textPrimary)
        destinations.forEach { destination ->
            GlassSurface(
                modifier = Modifier.fillMaxWidth().springClick(onClick = { onOpen(destination) }),
                shape = RoundedCornerShape(17.dp),
                level = GlassLevel.Subtle,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    FlowIcon(FlowIcons.Music, tint = Flow.colors.accent, size = 20.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(destination.title, style = Flow.type.label, color = Flow.colors.textPrimary, maxLines = 1)
                        Text(destination.description, style = Flow.type.caption, color = Flow.colors.textSecondary)
                    }
                    FlowIcon(FlowIcons.Export, tint = Flow.colors.textSecondary, size = 17.dp)
                }
            }
        }
    }
}

private fun openMusicDestination(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "باز کردن سرویس ناموفق بود.", Toast.LENGTH_SHORT).show()
    }
}
