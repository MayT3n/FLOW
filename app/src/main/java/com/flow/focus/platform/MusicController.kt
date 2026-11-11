package com.flow.focus.platform

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicPlaybackState(
    val trackId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val volume: Float = 0.8f,
    val favorites: Set<String> = emptySet(),
    val connected: Boolean = false,
)

/** App-facing MediaController; playback itself remains owned by MatMusicService. */
class MusicController(context: Context) : Player.Listener {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = appContext.getSharedPreferences("matfocus_music", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        MusicPlaybackState(favorites = preferences.getStringSet("favorites", emptySet()).orEmpty()),
    )
    val state: StateFlow<MusicPlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null

    init {
        connect()
        scope.launch {
            while (isActive) {
                publish()
                delay(if (_state.value.isPlaying) 500L else 1_500L)
            }
        }
    }

    private fun connect() {
        if (future != null || controller != null) return
        val token = SessionToken(appContext, ComponentName(appContext, MatMusicService::class.java))
        val pending = MediaController.Builder(appContext, token).buildAsync()
        future = pending
        pending.addListener({
            runCatching { pending.get() }.onSuccess { mediaController ->
                controller = mediaController
                mediaController.addListener(this)
                if (mediaController.mediaItemCount == 0) {
                    mediaController.setMediaItems(
                        MusicCatalog.tracks.map { track ->
                            MediaItem.Builder()
                                .setMediaId(track.id)
                                .setUri(track.sourceUrl)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(track.title)
                                        .setArtist("AERIS MUSIC · CC0 1.0")
                                        .setDisplayTitle(track.title)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                        .build(),
                                )
                                .build()
                        },
                    )
                    mediaController.prepare()
                }
                publish()
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun playTrack(index: Int) {
        val player = controller ?: run { connect(); return }
        if (index !in MusicCatalog.tracks.indices) return
        player.seekTo(index, 0L)
        player.play()
        publish()
    }

    fun togglePlayPause() {
        val player = controller ?: run { connect(); return }
        if (player.isPlaying) player.pause() else {
            if (player.mediaItemCount == 0) {
                player.setMediaItems(MusicCatalog.tracks.map { track ->
                    MediaItem.Builder().setMediaId(track.id).setUri(track.sourceUrl)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist("AERIS MUSIC · CC0 1.0").build())
                        .build()
                })
                player.prepare()
            }
            if (player.currentMediaItemIndex < 0) player.seekToDefaultPosition(0)
            player.play()
        }
        publish()
    }

    fun next() { controller?.seekToNextMediaItem(); controller?.play(); publish() }

    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > 3_000L) player.seekTo(0L) else player.seekToPreviousMediaItem()
        player.play()
        publish()
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs); publish() }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
        publish()
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
        publish()
    }

    fun setVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        controller?.volume = v
        _state.value = _state.value.copy(volume = v)
    }

    fun toggleFavorite(trackId: String) {
        val next = _state.value.favorites.toMutableSet().apply {
            if (!add(trackId)) remove(trackId)
        }
        preferences.edit().putStringSet("favorites", next).apply()
        _state.value = _state.value.copy(favorites = next)
    }

    private fun publish() {
        val player = controller ?: return
        _state.value = _state.value.copy(
            trackId = player.currentMediaItem?.mediaId,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L && it != androidx.media3.common.C.TIME_UNSET } ?: 0L,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            volume = player.volume,
            connected = true,
        )
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
    override fun onPlaybackStateChanged(playbackState: Int) = publish()
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = publish()
    override fun onRepeatModeChanged(repeatMode: Int) = publish()
}
