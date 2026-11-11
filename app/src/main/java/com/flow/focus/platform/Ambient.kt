package com.flow.focus.platform

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.flow.focus.FlowApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** پخش حلقه‌ای صدای تولیدشده با AudioTrack (حالت ایستا؛ حافظه‌ی کم و CPU ناچیز). */
class AmbientPlayer(private val ctx: Context, private val scope: CoroutineScope) {
    private var track: AudioTrack? = null
    private var job: Job? = null
    private var currentId: String? = null
    private val _playing = MutableStateFlow<String?>(null)
    val playing: StateFlow<String?> = _playing.asStateFlow()

    @Synchronized
    fun play(id: String, volume: Float) {
        if (id == AmbientSynth.NONE) { stop(); return }
        if (currentId == id && track != null) { track?.setVolume(volume); return }
        stopInternal()
        currentId = id
        pendingVolume = volume
        _playing.value = id
        startService()
        job = scope.launch(Dispatchers.Default) {
            val pcm = AmbientSynth.generate(id)
            synchronized(this@AmbientPlayer) {
                if (currentId != id) return@launch
                val t = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setSampleRate(AmbientSynth.RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                t.write(pcm, 0, pcm.size)
                t.setLoopPoints(0, pcm.size, -1)
                t.setVolume(pendingVolume)
                t.play()
                track = t
            }
        }
    }

    @Volatile private var pendingVolume = 0.5f

    @Synchronized
    fun setVolume(v: Float) {
        pendingVolume = v
        track?.setVolume(v)
    }

    @Synchronized
    fun stop() {
        stopInternal()
        stopService()
    }

    private fun stopInternal() {
        job?.cancel(); job = null
        currentId = null
        _playing.value = null
        track?.let { runCatching { it.stop() }; runCatching { it.release() } }
        track = null
    }

    private fun startService() {
        try { ContextCompat.startForegroundService(ctx, Intent(ctx, AmbientService::class.java)) } catch (_: Exception) { }
    }

    private fun stopService() {
        try { ctx.stopService(Intent(ctx, AmbientService::class.java)) } catch (_: Exception) { }
    }
}

/** سرویس پیش‌زمینه‌ی سبک تا پخش صدا هنگام کار در پس‌زمینه قطع نشود. */
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
