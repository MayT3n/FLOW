package com.flow.focus.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flow.focus.FlowApp

/** دریافت Alarm پایان جلسه، یادآوری وظیفه و دکمه‌های اعلان. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = (context.applicationContext as FlowApp).container
        when (intent.action) {
            ACTION_TIMER_END -> c.timer.onAlarm()
            ACTION_PAUSE -> c.timer.pause()
            ACTION_RESUME -> c.timer.start()
            ACTION_SKIP -> c.timer.skip()
            ACTION_REMINDER -> {
                val id = intent.getStringExtra(EXTRA_TASK_ID)
                val d = c.repo.current
                val task = d.tasks.firstOrNull { it.id == id }
                if (task != null && !task.done) c.notifier.showReminder(task, d.settings)
            }
        }
        c.repo.flush()
    }

    companion object {
        const val ACTION_TIMER_END = "com.flow.focus.TIMER_END"
        const val ACTION_PAUSE = "com.flow.focus.PAUSE"
        const val ACTION_RESUME = "com.flow.focus.RESUME"
        const val ACTION_SKIP = "com.flow.focus.SKIP"
        const val ACTION_REMINDER = "com.flow.focus.REMINDER"
        const val EXTRA_TASK_ID = "task_id"
    }
}

/** بعد از روشن شدن دستگاه یا به‌روزرسانی برنامه، Alarmها دوباره ثبت می‌شوند. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = (context.applicationContext as FlowApp).container
        c.timer.restore()
        c.alarms.rescheduleAllReminders(c.repo.current)
        c.repo.flush()
    }
}
