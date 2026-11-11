package com.flow.focus

import android.app.Application
import com.flow.focus.data.FlowRepository
import com.flow.focus.data.JsonStore
import com.flow.focus.music.MusicManager
import com.flow.focus.platform.AlarmScheduler
import com.flow.focus.platform.AmbientPlayer
import com.flow.focus.platform.NotificationHelper
import com.flow.focus.platform.TimerController
import com.flow.focus.remote.RemoteContentRepository
import com.flow.focus.remote.RemoteContentScheduler
import com.flow.focus.remote.UpdateManager
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** ظرف وابستگی‌ها؛ مسیر فایل داده‌ی قدیمی عمداً ثابت مانده است. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Do not rename or move this file: it contains existing tasks, sessions, timer and settings.
    val repo = FlowRepository(JsonStore(File(app.filesDir, "flow_data.json")), scope)
    val notifier = NotificationHelper(app)
    val alarms = AlarmScheduler(app)
    val ambient = AmbientPlayer(app)
    val music = MusicManager(app, scope)
    val timer = TimerController(repo, alarms, notifier, ambient, music)

    val remote = RemoteContentRepository(app, scope)
    val updater = UpdateManager(app)
}

class FlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannels()
        container.music.setVolume(container.repo.current.settings.musicVolume)
        container.timer.restore()
        container.alarms.rescheduleAllReminders(container.repo.current)
        RemoteContentScheduler.schedule(this)
        container.scope.launch { container.remote.refresh(force = false) }
    }
}
