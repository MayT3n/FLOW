package com.flow.focus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.flow.focus.core.DateFormats
import com.flow.focus.data.Task
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

/** ساعت دیواری؛ فقط وقتی برنامه روی صفحه است و [active] برقرار است به‌روز می‌شود. */
@Composable
fun rememberNowMs(active: Boolean, periodMs: Long = 200L): State<Long> {
    val state = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(active, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            state.longValue = System.currentTimeMillis()
            while (active) {
                delay(periodMs)
                state.longValue = System.currentTimeMillis()
            }
        }
    }
    return state
}

/** تاریخ امروز؛ با عبور از نیمه‌شب خودکار عوض می‌شود. */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                today = LocalDate.now()
                delay(30_000L)
            }
        }
    }
    return today
}

fun relativeDay(epochDay: Long, today: LocalDate): String {
    val d = runCatching { LocalDate.ofEpochDay(epochDay) }.getOrNull()
        ?: return "تاریخ نامعتبر"
    return when (epochDay - today.toEpochDay()) {
        0L -> "امروز"
        1L -> "فردا"
        -1L -> "دیروز"
        else -> DateFormats.short(d)
    }
}

/** برچسب سررسید (بدون ارقام تبدیل‌شده؛ نمایش با num()). */
fun dueLabel(t: Task, today: LocalDate): String? {
    val day = t.dueDay ?: return null
    val base = relativeDay(day, today)
    return if (t.dueMinute != null) "$base · ${DateFormats.clock(t.dueMinute)}" else base
}

fun isOverdue(t: Task, today: LocalDate): Boolean = !t.done && t.dueDay != null && t.dueDay < today.toEpochDay()

    LocalDate.ofEpochDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
