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
            NotificationChannel(CH_AMBIENT, "صدای محیط MatFocus", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
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

    private fun doneChannelId(sound: Boolean, vib: Boolean) = "matfocus_done_${if (sound) 1 else 0}${if (vib) 1 else 0}"

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, TimerReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** رنگ هر فاز؛ هم رنگ اعلان و هم حلقه‌ی آیکون بزرگ است. */
    private fun phaseColor(p: PhaseType): Int = when (p) {
        PhaseType.FOCUS -> 0xFF79A9E4.toInt()
        PhaseType.SHORT_BREAK -> 0xFF55B98A.toInt()
        PhaseType.LONG_BREAK -> 0xFFB48CE0.toInt()
    }

    /** آیکون بزرگ: حلقه‌ی رنگی فاز با نقطه‌ی درخشان، مشابه حلقه‌ی تایمر داخل برنامه. */
    private fun ringBitmap(color: Int, sweep: Float): android.graphics.Bitmap {
        val size = 192
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val stroke = size * 0.1f
        val pad = stroke / 2f + 6f
        val rect = android.graphics.RectF(pad, pad, size - pad, size - pad)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE; strokeWidth = stroke; strokeCap = android.graphics.Paint.Cap.ROUND
        }
        p.color = (color and 0x00FFFFFF) or 0x33000000
        c.drawArc(rect, 0f, 360f, false, p)
        p.color = color
        c.drawArc(rect, -90f, sweep.coerceIn(8f, 360f), false, p)
        val f = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = (color and 0x00FFFFFF) or 0x55000000 }
        c.drawCircle(size / 2f, size / 2f, size * 0.2f, f)
        f.color = color
        c.drawCircle(size / 2f, size / 2f, size * 0.1f, f)
        return bmp
    }

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
            .setSubText("MatFocus")
            .setColor(phaseColor(state.phase))
            .setLargeIcon(ringBitmap(phaseColor(state.phase), 300f))
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
            .setSubText("MatFocus")
            .setColor(phaseColor(state.phase))
            .setLargeIcon(ringBitmap(phaseColor(state.phase), 150f))
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSubText("MatFocus")
            .setColor(phaseColor(next))
            .setLargeIcon(ringBitmap(phaseColor(finished), 360f))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
        safeNotify(ID_DONE, b.build())
    }

    /** اعلان خوش‌آمدگویی؛ همین اعلان برای «ارسال آزمایشی» در تنظیمات هم استفاده می‌شود. */
    fun showWelcome(s: Settings, test: Boolean = false) {
        if (!s.notifications && !test) return
        val text = if (test) "اعلان‌ها درست کار می‌کنند. وقتی جلسه‌ی تمرکز تمام شود، همین‌جا خبرت می‌کنیم."
        else "از امروز هر جلسه‌ی تمرکز را آرام و روان دنبال می‌کنیم. وقتی آماده‌ای، یک جلسه شروع کن."
        val b = NotificationCompat.Builder(ctx, doneChannelId(s.sound, s.vibration))
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle(if (test) "اعلان آزمایشی MatFocus" else "به MatFocus خوش آمدی")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSubText("MatFocus")
            .setColor(phaseColor(PhaseType.FOCUS))
            .setLargeIcon(ringBitmap(phaseColor(PhaseType.FOCUS), 360f))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp())
        safeNotify(ID_WELCOME, b.build())
    }

    fun showReminder(task: Task, s: Settings) {
        if (!s.notifications) return
        val b = NotificationCompat.Builder(ctx, doneChannelId(s.sound, s.vibration))
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle("یادآوری وظیفه")
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.title))
            .setSubText("MatFocus")
            .setColor(phaseColor(PhaseType.FOCUS))
            .setLargeIcon(ringBitmap(phaseColor(PhaseType.FOCUS), 360f))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
        // Namespace the full stable task hash away from fixed timer/media notification IDs.
        val notificationId = 0x40000000 or (task.id.hashCode() and 0x3FFFFFFF)
        safeNotify(notificationId, b.build())
    }

    fun ambientNotification(): android.app.Notification =
        NotificationCompat.Builder(ctx, CH_AMBIENT)
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setContentTitle("MatFocus · صدای محیط در حال پخش است")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp())
            .build()

    companion object {
        const val CH_RUNNING = "matfocus_running"
        const val CH_AMBIENT = "matfocus_ambient"
        const val ID_RUNNING = 1
        const val ID_DONE = 2
        const val ID_AMBIENT = 3
        const val ID_WELCOME = 4
    }
}
