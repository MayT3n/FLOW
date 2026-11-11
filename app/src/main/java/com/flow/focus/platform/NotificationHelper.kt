package com.flow.focus.platform

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings as AndroidSettings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.flow.focus.MainActivity
import com.flow.focus.R
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Settings
import com.flow.focus.data.Task
import com.flow.focus.data.TimerState

class NotificationHelper(private val ctx: Context) {
    private val nm = NotificationManagerCompat.from(ctx)

    fun ensureChannels() {
        val m = ctx.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(
            NotificationChannel(CH_RUNNING, "تایمر در حال اجرا", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
        )
        m.createNotificationChannel(
            NotificationChannel(CH_AMBIENT, "صدای محیط", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
        )
        // کانال صدا/لرزش بعد از ساخت قابل تغییر نیست؛ برای هر ترکیب یک کانال داریم.
        for (sound in listOf(true, false)) for (vib in listOf(true, false)) {
            val importance = if (sound || vib) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
            m.createNotificationChannel(
                NotificationChannel(doneChannelId(sound, vib), "پایان جلسه" + suffix(sound, vib), importance).apply {
                    if (sound) {
                        setSound(
                            AndroidSettings.System.DEFAULT_NOTIFICATION_URI,
                            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                        )
                    } else setSound(null, null)
                    enableVibration(vib)
                    if (vib) vibrationPattern = longArrayOf(0, 350, 150, 350)
                },
            )
        }
    }

    private fun suffix(sound: Boolean, vib: Boolean) = when {
        sound && vib -> " (صدا و لرزش)"
        sound -> " (فقط صدا)"
        vib -> " (فقط لرزش)"
        else -> " (بی‌صدا)"
    }

    private fun doneChannelId(sound: Boolean, vib: Boolean) = "flow_done_${if (sound) 1 else 0}${if (vib) 1 else 0}"

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, TimerReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun phaseTitle(p: PhaseType) = when (p) {
        PhaseType.FOCUS -> "زمان تمرکز"
        PhaseType.SHORT_BREAK -> "استراحت کوتاه"
        PhaseType.LONG_BREAK -> "استراحت بلند"
    }

    @SuppressLint("MissingPermission")
    private fun safeNotify(id: Int, n: android.app.Notification) {
        if (!nm.areNotificationsEnabled()) return
        try { nm.notify(id, n) } catch (_: SecurityException) { }
    }

    fun showRunning(state: TimerState, taskTitle: String?, enabled: Boolean) {
        if (!enabled) { nm.cancel(ID_RUNNING); return }
        val b = NotificationCompat.Builder(ctx, CH_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle(phaseTitle(state.phase))
            .setContentText(taskTitle ?: "MatFocus")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openApp())
            .setWhen(state.endAtMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .addAction(0, "توقف", action(TimerReceiver.ACTION_PAUSE, 11))
            .addAction(0, "رد کردن", action(TimerReceiver.ACTION_SKIP, 12))
        safeNotify(ID_RUNNING, b.build())
    }

    fun showPaused(state: TimerState, taskTitle: String?, enabled: Boolean) {
        if (!enabled) { nm.cancel(ID_RUNNING); return }
        val mins = (state.remainingMs + 59_999) / 60_000
        val b = NotificationCompat.Builder(ctx, CH_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle(phaseTitle(state.phase) + " — متوقف")
            .setContentText((taskTitle ?: "MatFocus") + " · حدود $mins دقیقه مانده")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(openApp())
            .addAction(0, "ادامه", action(TimerReceiver.ACTION_RESUME, 13))
        safeNotify(ID_RUNNING, b.build())
    }

    fun cancelRunning() = nm.cancel(ID_RUNNING)

    fun showFinished(finished: PhaseType, next: PhaseType, s: Settings) {
        if (!s.notifications) return
        val (title, text) = if (finished == PhaseType.FOCUS) {
            "جلسه‌ی تمرکز تمام شد" to if (next == PhaseType.LONG_BREAK) "وقت یک استراحت بلند است." else "وقت یک استراحت کوتاه است."
        } else {
            "استراحت تمام شد" to "آماده‌ی جلسه‌ی تمرکز بعدی هستی؟"
        }
        val b = NotificationCompat.Builder(ctx, doneChannelId(s.sound, s.vibration))
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
        safeNotify(ID_DONE, b.build())
    }

    fun showReminder(task: Task, s: Settings) {
        if (!s.notifications) return
        val b = NotificationCompat.Builder(ctx, doneChannelId(s.sound, s.vibration))
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle("یادآوری وظیفه")
            .setContentText(task.title)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
        safeNotify(ID_REMINDER_BASE + (task.id.hashCode() and 0xFFFF), b.build())
    }

    fun ambientNotification(): android.app.Notification =
        NotificationCompat.Builder(ctx, CH_AMBIENT)
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle("صدای محیط در حال پخش است")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp())
            .build()

    companion object {
        const val CH_RUNNING = "flow_running"
        const val CH_AMBIENT = "flow_ambient"
        const val ID_RUNNING = 1
        const val ID_DONE = 2
        const val ID_AMBIENT = 3
        const val ID_REMINDER_BASE = 1000
    }
}
