package com.flow.focus.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flow.focus.FlowApp
import com.flow.focus.core.CalendarOccasion
import com.flow.focus.core.IranianCalendarData
import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.Goal
import com.flow.focus.data.GoalPeriod
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Routine
import com.flow.focus.data.Settings
import com.flow.focus.data.Task
import com.flow.focus.data.FlowRepository
import com.flow.focus.music.MusicCatalog
import com.flow.focus.music.MusicSnapshot
import com.flow.focus.platform.AmbientSynth
import com.flow.focus.platform.WallpaperStore
import com.flow.focus.remote.NewsItem
import com.flow.focus.remote.UpdateDownloadState
import com.flow.focus.remote.UpdateManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** تنها نقطه‌ی تماس رابط کاربری با منطق؛ UI مستقیماً به سرویس‌ها و مخزن‌ها دست نمی‌زند. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as FlowApp).container
    private val wallpaperStore = WallpaperStore(app)
    private val _offlineCalendarOccasions = MutableStateFlow<List<CalendarOccasion>>(emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _offlineCalendarOccasions.value = IranianCalendarData.load(app, JalaliDate.today().year)
        }
    }

    val data: StateFlow<AppData> get() = c.repo.data
    val offlineCalendarOccasions: StateFlow<List<CalendarOccasion>> get() = _offlineCalendarOccasions
    val ambientPlaying: StateFlow<String?> get() = c.ambient.playing
    val ambientDownloadProgress get() = c.ambient.downloadProgress
    // News, updates and persistent read state
    val newsItems: StateFlow<List<NewsItem>> get() = c.remote.items
    val newsReadIds: StateFlow<Set<String>> get() = c.remote.readIds
    val updateManifest: StateFlow<UpdateManifest?> get() = c.remote.manifest
    val updateDownloadState: StateFlow<UpdateDownloadState> get() = c.updater.state

    fun refreshRemoteContent(force: Boolean = false) {
        viewModelScope.launch { c.remote.refresh(force = force) }
    }
    fun markNewsRead(id: String) { viewModelScope.launch { c.remote.markRead(id) } }
    fun markNewsUnread(id: String) { viewModelScope.launch { c.remote.markUnread(id) } }
    fun markAllNewsRead() { viewModelScope.launch { c.remote.markAllRead() } }
    fun markUpdateRead(versionCode: Int) { viewModelScope.launch { c.remote.markRead("update:$versionCode") } }
    fun markUpdateUnread(versionCode: Int) { viewModelScope.launch { c.remote.markUnread("update:$versionCode") } }

    fun downloadUpdate() {
        val manifest = c.remote.manifest.value ?: return
        viewModelScope.launch { runCatching { c.updater.download(manifest) } }
    }
    fun installDownloadedUpdate(): Boolean = runCatching { c.updater.installDownloaded() }.getOrDefault(false)

    // Media3 playback
    val musicSnapshot: StateFlow<MusicSnapshot> get() = c.music.snapshot
    val musicFavorites: StateFlow<Set<String>> get() = c.music.favorites
    val musicPlaylists get() = c.music.playlists
    val musicDownloadProgress get() = c.music.downloadProgress
    val musicTracks get() = MusicCatalog.tracks
    fun playMusic(id: String) = c.music.play(id)
    fun toggleMusicPlayback() = c.music.togglePlayPause()
    fun nextMusic() = c.music.next()
    fun previousMusic() = c.music.previous()
    fun seekMusic(positionMs: Long) = c.music.seekTo(positionMs)
    fun setMusicVolume(value: Float) = c.music.setVolume(value)
    fun toggleMusicShuffle() = c.music.toggleShuffle()
    fun cycleMusicRepeat() = c.music.cycleRepeat()
    fun toggleMusicFavorite(id: String) = c.music.toggleFavorite(id)
    fun createMusicPlaylist(name: String) = c.music.createPlaylist(name)
    fun renameMusicPlaylist(id: String, name: String) = c.music.renamePlaylist(id, name)
    fun deleteMusicPlaylist(id: String) = c.music.deletePlaylist(id)
    fun addMusicTrackToPlaylist(playlistId: String, trackId: String) = c.music.addTrackToPlaylist(playlistId, trackId)
    fun removeMusicTrackFromPlaylist(playlistId: String, trackId: String) = c.music.removeTrackFromPlaylist(playlistId, trackId)
    fun playMusicPlaylist(playlistId: String) = c.music.playPlaylist(playlistId)
    fun isMusicDownloaded(id: String) = c.music.isDownloaded(id)
    fun downloadedMusicBytes() = c.music.downloadedBytes()
    fun removeMusicDownload(id: String) = c.music.removeDownload(id)
    fun downloadMusicTrack(id: String) {
        viewModelScope.launch { runCatching { c.music.downloadTrack(id) } }
    }

    // ---- تایمر ----
    fun start() = c.timer.start()
    fun pause() = c.timer.pause()
    fun skip() = c.timer.skip()
    fun reset() = c.timer.reset()
    fun selectPhase(p: PhaseType) = c.timer.selectPhase(p)
    fun selectTask(id: String?) = c.timer.selectTask(id)
    fun onTimerEnd() = c.timer.catchUp()
    fun onResume() = c.timer.catchUp()

    // ---- وظایف ----
    fun saveTask(t: Task) {
        c.repo.upsertTask(t)
        c.alarms.syncReminder(t, c.repo.current.settings.notifications)
    }

    fun deleteTask(id: String) {
        c.repo.deleteTask(id)
        c.alarms.cancelReminder(id)
        c.timer.refresh()
    }

    fun setTaskDone(id: String, done: Boolean) {
        c.repo.setTaskDone(id, done, System.currentTimeMillis())
        c.repo.current.tasks.firstOrNull { it.id == id }?.let { c.alarms.syncReminder(it, c.repo.current.settings.notifications) }
    }

    fun moveTask(id: String, epochDay: Long) {
        val t = c.repo.current.tasks.firstOrNull { it.id == id } ?: return
        saveTask(t.copy(dueDay = epochDay))
    }

    // ---- روتین و هدف ----
    fun saveRoutine(r: Routine) = c.repo.upsertRoutine(r)
    fun deleteRoutine(id: String) = c.repo.deleteRoutine(id)
    fun toggleRoutine(routineId: String, epochDay: Long) = c.repo.toggleRoutineCheck(routineId, epochDay)

    fun addGoal(title: String, period: GoalPeriod, periodStartDay: Long) {
        if (title.isBlank()) return
        c.repo.addGoal(Goal(FlowRepository.newId(), title.trim(), period, periodStartDay))
    }

    fun toggleGoal(id: String) = c.repo.toggleGoal(id)
    fun deleteGoal(id: String) = c.repo.deleteGoal(id)

    // ---- تنظیمات ----
    fun updateSettings(block: (Settings) -> Settings) {
        val before = c.repo.current.settings
        c.repo.updateSettings(block)
        val after = c.repo.current.settings
        val durationsChanged = before.focusMin != after.focusMin || before.shortBreakMin != after.shortBreakMin ||
            before.longBreakMin != after.longBreakMin || before.rounds != after.rounds
        val effectsChanged = before.notifications != after.notifications || before.ambientSound != after.ambientSound ||
            before.ambientAuto != after.ambientAuto
        if (durationsChanged) c.timer.syncIdle() else if (effectsChanged) c.timer.refresh()
        if (before.notifications != after.notifications) c.alarms.rescheduleAllReminders(c.repo.current)
        if (before.ambientVolume != after.ambientVolume) c.ambient.setVolume(after.ambientVolume)
        if (before.musicVolume != after.musicVolume) c.music.setVolume(after.musicVolume)
    }

    /** پیش‌شنوایی صدای محیط در تنظیمات (جدا از پخش خودکار تایمر) */
    fun previewAmbient(id: String) {
        val s = c.repo.current.settings
        if (c.ambient.playing.value == id) c.ambient.stop() else c.ambient.play(id, s.ambientVolume)
    }

    fun isAmbientDownloaded(id: String) = c.ambient.isDownloaded(id)
    fun downloadedAmbientBytes() = c.ambient.downloadedBytes()
    fun deleteAmbientDownload(id: String) = c.ambient.deleteDownload(id)
    fun downloadAmbient(id: String) { viewModelScope.launch { runCatching { c.ambient.downloadAsset(id) } } }

    fun stopAmbientPreview() {
        val d = c.repo.current
        val timerOwnsAmbient = d.timer.status == com.flow.focus.data.TimerStatus.RUNNING &&
            d.timer.phase == PhaseType.FOCUS && d.settings.ambientAuto && d.settings.ambientSound != AmbientSynth.NONE
        if (!timerOwnsAmbient) c.ambient.stop()
    }

    // ---- پس‌زمینه ----
    fun selectWallpaper(id: String) {
        updateSettings { it.copy(wallpaperId = id) }
    }

    /** تصویر انتخابی ابتدا به نسخه‌ی بهینه‌شده در حافظه‌ی خصوصی برنامه تبدیل می‌شود. */
    fun importWallpaper(uri: Uri, onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { wallpaperStore.import(uri) }
            onComplete(path)
        }
    }

    /** Apply only after the user accepts the preview. */
    fun applyCustomWallpaper(path: String): Boolean {
        if (!wallpaperStore.isStored(path)) return false
        val previous = c.repo.current.settings.wallpaperPath
        updateSettings { it.copy(wallpaperId = "custom", wallpaperPath = path) }
        if (previous != null && previous != path) {
            viewModelScope.launch(Dispatchers.IO) { wallpaperStore.delete(previous) }
        }
        return true
    }

    fun discardWallpaperPreview(path: String) {
        viewModelScope.launch(Dispatchers.IO) { wallpaperStore.delete(path) }
    }

    // ---- داده‌ها ----
    fun exportJson(): String = c.repo.exportJson()

    fun importJson(text: String): Boolean {
        val ok = c.repo.importJson(text)
        if (ok) {
            c.timer.syncIdle()
            c.alarms.rescheduleAllReminders(c.repo.current)
        }
        return ok
    }

    fun clearHistory() = c.repo.clearHistory()

    fun clearAll() {
        c.repo.update { AppData(settings = it.settings) }
        c.timer.syncIdle()
    }

}
