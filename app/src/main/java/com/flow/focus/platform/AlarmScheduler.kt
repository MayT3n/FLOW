package com.flow.focus.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.flow.focus.data.AppData
import com.flow.focus.data.Task
import com.flow.focus.core.ReminderTime

class AlarmScheduler(private val ctx: Context) {
    private val am = ctx.getSystemService(AlarmManager::class.java)

    private fun pi(action: String, code: Int, taskId: String? = null): PendingIntent {
        val i = Intent(ctx, TimerReceiver::class.java).setAction(action)
        if (taskId != null) {
            i.data = Uri.parse("matfocus://reminder/${Uri.encode(taskId)}")
            i.putExtra(TimerReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun scheduleExact(atMs: Long, pi: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    fun scheduleTimer(atMs: Long) = scheduleExact(atMs, pi(TimerReceiver.ACTION_TIMER_END, CODE_TIMER))
    fun cancelTimer() = am.cancel(pi(TimerReceiver.ACTION_TIMER_END, CODE_TIMER))

    fun reminderTime(task: Task): Long? {
        val day = task.dueDay ?: return null
        val minute = task.dueMinute ?: return null
        return ReminderTime.epochMillis(day, minute)
    }

    /** یادآوری وظیفه در ساعت سررسید؛ برای وظیفه‌ی انجام‌شده یا گذشته لغو می‌شود. */
    fun syncReminder(task: Task, enabled: Boolean, now: Long = System.currentTimeMillis()) {
        val code = CODE_REMINDER_BASE
        val at = reminderTime(task)
        if (!enabled || task.done || at == null || at <= now) {
            am.cancel(pi(TimerReceiver.ACTION_REMINDER, code, task.id))
        } else {
            scheduleExact(at, pi(TimerReceiver.ACTION_REMINDER, code, task.id))
        }
    }

    fun cancelReminder(taskId: String) {
        am.cancel(pi(TimerReceiver.ACTION_REMINDER, CODE_REMINDER_BASE, taskId))
    }

    fun rescheduleAllReminders(d: AppData) {
        d.tasks.forEach { syncReminder(it, d.settings.notifications) }
    }

    private companion object {
        const val CODE_TIMER = 100
        const val CODE_REMINDER_BASE = 10_000
    }
}
