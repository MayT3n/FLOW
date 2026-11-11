package com.flow.focus

import android.app.Application
import com.flow.focus.data.FlowRepository
import com.flow.focus.data.JsonStore
import com.flow.focus.platform.AlarmScheduler
import com.flow.focus.platform.AmbientPlayer
import com.flow.focus.platform.NotificationHelper
import com.flow.focus.platform.TimerController
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** ظرف وابستگی‌ها (تزریق دستی؛ بدون کتابخانه‌ی اضافی). */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repo = FlowRepository(JsonStore(File(app.filesDir, "flow_data.json")), scope)
    val notifier = NotificationHelper(app)
    val alarms = AlarmScheduler(app)
    val ambient = AmbientPlayer(app, scope)
    val timer = TimerController(repo, alarms, notifier, ambient)
}

class FlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannels()
        container.timer.restore()
        container.alarms.rescheduleAllReminders(container.repo.current)
    }
}
