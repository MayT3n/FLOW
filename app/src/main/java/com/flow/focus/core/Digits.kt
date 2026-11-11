package com.flow.focus.core

/** تبدیل ارقام به فارسی یا انگلیسی؛ همه‌ی اعداد رابط کاربری از این مسیر عبور می‌کنند. */
fun String.withDigits(persian: Boolean): String {
    val sb = StringBuilder(length)
    for (ch in this) {
        sb.append(
            when {
                persian && ch in '0'..'9' -> '۰' + (ch - '0')
                !persian && ch in '۰'..'۹' -> '0' + (ch - '۰')
                else -> ch
            },
        )
    }
    return sb.toString()
}

fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/** مثلاً ۹۰ دقیقه → «۱ ساعت و ۳۰ دقیقه» */
fun formatMinutes(minutes: Long): String {
    val m = minutes.coerceAtLeast(0)
    val h = m / 60
    val r = m % 60
    return when {
        h == 0L -> "$r دقیقه"
        r == 0L -> "$h ساعت"
        else -> "$h ساعت و $r دقیقه"
    }
}
