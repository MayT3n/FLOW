package com.flow.desktop.design

import androidx.compose.runtime.Composable

/**
 * Digit and bidi helpers. The user can choose Persian or Latin digits (Settings.persianDigits);
 * every number the UI prints goes through here so the choice is applied uniformly.
 */
internal fun toPersianDigits(s: String): String = buildString(s.length) {
    for (ch in s) append(if (ch in '0'..'9') ('۰'.code + (ch - '0')).toChar() else ch)
}

internal fun toLatinDigits(s: String): String = buildString(s.length) {
    for (ch in s) append(
        when (ch) {
            in '۰'..'۹' -> '0' + (ch - '۰')
            in '٠'..'٩' -> '0' + (ch - '٠')
            else -> ch
        }
    )
}

internal fun digits(s: String, persian: Boolean): String = if (persian) toPersianDigits(toLatinDigits(s)) else toLatinDigits(s)

/** Wraps Latin/number runs in an LTR isolate so they keep their order inside RTL text. */
internal fun ltr(s: String): String = "⁦$s⁩"

internal fun mmss(totalSeconds: Long): Pair<String, String> {
    val s = totalSeconds.coerceAtLeast(0L)
    return "%02d".format(s / 60) to "%02d".format(s % 60)
}

internal fun clockText(minuteOfDay: Int, persian: Boolean): String =
    digits("%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60), persian)

@Composable
internal fun num(value: Any): String = digits(value.toString(), LocalPersianDigits.current)

@Composable
internal fun numText(text: String): String = digits(text, LocalPersianDigits.current)

/** Parses Jalali `yyyy/mm/dd` with either digit family. */
internal fun parseJalali(input: String): java.time.LocalDate? {
    val parts = toLatinDigits(input).trim().split('/', '-', '.')
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (y !in 1200..1700 || m !in 1..12) return null
    if (d !in 1..com.flow.focus.core.JalaliDate.monthLength(y, m)) return null
    return runCatching { com.flow.focus.core.JalaliDate(y, m, d).toLocalDate() }.getOrNull()
}

internal fun parseClock(input: String): Int? {
    val parts = toLatinDigits(input).trim().split(':')
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

internal fun jalaliText(date: java.time.LocalDate): String {
    val j = com.flow.focus.core.JalaliDate.of(date)
    return "%04d/%02d/%02d".format(j.year, j.month, j.day)
}
