package com.flow.focus.platform

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.flow.focus.FlowApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Ambient recordings use Media3, repeat-one playback and a private offline audio cache. */
class AmbientPlayer(
    private val ctx: Context,
    private val assets: AmbientAssetStore = AmbientAssetStore(ctx),
) {
    private var player: ExoPlayer? = null
    private var currentId: String? = null
    private val _playing = MutableStateFlow<String?>(null)
    val playing: StateFlow<String?> = _playing.asStateFlow()
    val downloadProgress: StateFlow<Map<String, Float>> get() = assets.progress

    @Synchronized
    fun play(id: String, volume: Float) {
        if (id == AmbientSynth.NONE) { stop(); return }
        val recording = AmbientLibrary.byId(id) ?: return
        if (currentId == id && player != null) {
            player?.volume = volume.coerceIn(0f, 1f)
            return
        }
        stopInternal()
        currentId = id
        _playing.value = id
        startService()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val uri = assets.localFile(id)?.let { Uri.fromFile(it) } ?: Uri.parse(recording.url)
        player = ExoPlayer.Builder(ctx).build().apply {
            setAudioAttributes(audioAttributes, false) // music player owns audio focus; ambient can be mixed
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_ONE
            this.volume = volume.coerceIn(0f, 1f)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    @Synchronized
    fun setVolume(value: Float) {
        player?.volume = value.coerceIn(0f, 1f)
    }

    suspend fun downloadAsset(id: String): File {
        val file = assets.download(id)
        synchronized(this) {
            if (currentId == id && player != null) {
                val volume = player?.volume ?: 0.5f
                player?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                player?.prepare()
                player?.volume = volume
                player?.playWhenReady = true
            }
        }
        return file
    }

    fun isDownloaded(id: String): Boolean = assets.isDownloaded(id)
    fun downloadedBytes(): Long = assets.totalBytes()
    fun deleteDownload(id: String): Boolean {
        if (currentId == id && player != null) {
            val recording = AmbientLibrary.byId(id)
            if (recording != null) {
                player?.setMediaItem(MediaItem.fromUri(recording.url))
                player?.prepare()
                player?.playWhenReady = true
            }
        }
        return assets.delete(id)
    }

    @Synchronized
    fun stop() {
        stopInternal()
        stopService()
    }

    private fun stopInternal() {
        currentId = null
        _playing.value = null
        player?.runCatching { stop() }
        player?.release()
        player = null
    }

    private fun startService() {
        try { ContextCompat.startForegroundService(ctx, Intent(ctx, AmbientService::class.java)) } catch (_: Exception) { }
    }

    private fun stopService() {
        try { ctx.stopService(Intent(ctx, AmbientService::class.java)) } catch (_: Exception) { }
    }
}

/** Foreground notification keeps ambient playback alive while the app is backgrounded. */
class AmbientService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = (application as FlowApp).container.notifier.ambientNotification()
        try {
            ServiceCompat.startForeground(this, NotificationHelper.ID_AMBIENT, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } catch (_: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }
}
