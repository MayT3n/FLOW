package com.flow.focus.data

import kotlinx.serialization.Serializable

@Serializable
enum class PhaseType { FOCUS, SHORT_BREAK, LONG_BREAK }

@Serializable
enum class TimerStatus { IDLE, RUNNING, PAUSED }

@Serializable
enum class Priority(val label: String) { LOW("کم"), MEDIUM("متوسط"), HIGH("زیاد") }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class GlassQuality { LOW, MEDIUM, HIGH }

@Serializable
enum class GoalPeriod { DAILY, WEEKLY }

@Serializable
enum class WallpaperFit { CROP, FIT }

/** دسته‌بندی‌های پیش‌فرض برنامه (ثابت؛ داده‌ی کاربر نیستند). */
data class Category(val id: String, val name: String, val color: Long)

object Categories {
    val all = listOf(
        Category("study", "درس", 0xFF5B7CFA),
        Category("work", "کار", 0xFF8B5CF6),
        Category("sport", "ورزش", 0xFFF59E0B),
        Category("personal", "شخصی", 0xFF10B981),
        Category("project", "پروژه", 0xFFEC6A8E),
    )

    fun byId(id: String?): Category = all.firstOrNull { it.id == id } ?: all.last()
}

@Serializable
data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    /** روز سررسید (epochDay) و ساعت (دقیقه از نیمه‌شب) */
    val dueDay: Long? = null,
    val dueMinute: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val categoryId: String = "personal",
    val estimatedPomodoros: Int = 1,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val createdAt: Long = 0L,
)

@Serializable
data class FocusSession(
    val id: String,
    val taskId: String? = null,
    val type: PhaseType,
    val startedAt: Long,
    val endedAt: Long,
    val actualMs: Long,
    val plannedMs: Long,
    val completed: Boolean,
)

/** روتین تکرارشونده؛ weekdays: ۰ = شنبه … ۶ = جمعه */
@Serializable
data class Routine(
    val id: String,
    val title: String,
    val weekdays: List<Int>,
    val minute: Int? = null,
    val categoryId: String = "personal",
    val pomodoros: Int = 1,
)

@Serializable
data class Goal(
    val id: String,
    val title: String,
    val period: GoalPeriod,
    /** روز (روزانه) یا شنبه‌ی هفته (هفتگی) به‌صورت epochDay */
    val periodStartDay: Long,
    val done: Boolean = false,
)

@Serializable
data class Settings(
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val rounds: Int = 4,
    val autoStartBreak: Boolean = false,
    val autoStartFocus: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val persianDigits: Boolean = true,
    val notifications: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val animations: Boolean = true,
    val glassQuality: GlassQuality = GlassQuality.MEDIUM,
    val glassOpacity: Float = 1f,
    val glassBlurDp: Int = 24,
    val glassShine: Float = 0.7f,
    val ambientSound: String = "none",
    val ambientVolume: Float = 0.5f,
    val ambientAuto: Boolean = true,
    val musicTrackId: String = "quiet-room",
    val breakMusicTrackId: String = "afterglow-vast",
    val musicVolume: Float = 0.8f,
    val musicAutoFocus: Boolean = false,
    val musicSwitchOnBreak: Boolean = false,
    val musicPauseWithTimer: Boolean = false,
    val dailyGoalMin: Int = 120,
    /** شناسه‌ی پس‌زمینه: gradient, aurora, midnight, dusk یا custom */
    val wallpaperId: String = "gradient",
    /** مسیر نسخه‌ی بهینه‌شده‌ی تصویر شخصی در حافظه‌ی خصوصی برنامه */
    val wallpaperPath: String? = null,
    val wallpaperFit: WallpaperFit = WallpaperFit.CROP,
    val wallpaperDim: Float = 0.18f,
    val wallpaperBlurDp: Int = 0,
) {
    fun durationMs(phase: PhaseType): Long = when (phase) {
        PhaseType.FOCUS -> focusMin
        PhaseType.SHORT_BREAK -> shortBreakMin
        PhaseType.LONG_BREAK -> longBreakMin
    } * 60_000L
}

@Serializable
data class TimerState(
    val phase: PhaseType = PhaseType.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    /** مدت برنامه‌ریزی‌شده‌ی فاز فعلی */
    val durationMs: Long = 25 * 60_000L,
    /** زمان باقی‌مانده وقتی IDLE یا PAUSED است */
    val remainingMs: Long = 25 * 60_000L,
    /** زمان پایان (ساعت دیواری) وقتی RUNNING است */
    val endAtMs: Long = 0L,
    val startedAtMs: Long = 0L,
    /** شماره‌ی دور تمرکز در چرخه (۱..rounds) */
    val round: Int = 1,
    val taskId: String? = null,
)

@Serializable
data class AppData(
    val version: Int = 2,
    val tasks: List<Task> = emptyList(),
    val sessions: List<FocusSession> = emptyList(),
    val routines: List<Routine> = emptyList(),
    /** هر عضو: "routineId|epochDay" */
    val routineChecks: List<String> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val settings: Settings = Settings(),
    val timer: TimerState = TimerState(),
)
