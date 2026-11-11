package com.flow.focus.core

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * تاریخ شمسی (جلالی). تبدیل با الگوریتم حسابی ۳۳ ساله (الگوریتم Pournader/jdf) انجام می‌شود
 * و با تست واحد روی تاریخ‌های مرجع بررسی شده است.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {

    val monthName: String get() = monthNames[month - 1]

    fun toLocalDate(): LocalDate {
        val g = jalaliToGregorian(year, month, day)
        return LocalDate.of(g[0], g[1], g[2])
    }

    val isLeapYear: Boolean get() = isLeap(year)

    fun lengthOfMonth(): Int = monthLength(year, month)

    fun plusMonths(n: Int): JalaliDate {
        val total = year * 12 + (month - 1) + n
        val y = Math.floorDiv(total, 12)
        val m = Math.floorMod(total, 12) + 1
        return JalaliDate(y, m, minOf(day, monthLength(y, m)))
    }

    fun withDay(d: Int) = copy(day = d)

    override fun compareTo(other: JalaliDate): Int =
        compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    companion object {
        val monthNames = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )

        /** نام روزهای هفته؛ ایندکس ۰ = شنبه. */
        val weekdayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
        val weekdayInitials = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

        fun of(date: LocalDate): JalaliDate {
            val j = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
            return JalaliDate(j[0], j[1], j[2])
        }

        fun today(): JalaliDate = of(LocalDate.now())

        fun isLeap(year: Int): Boolean {
            val g = jalaliToGregorian(year, 12, 30)
            return gregorianToJalali(g[0], g[1], g[2]).let { it[0] == year && it[1] == 12 && it[2] == 30 }
        }

        fun monthLength(year: Int, month: Int): Int = when {
            month <= 6 -> 31
            month <= 11 -> 30
            else -> if (isLeap(year)) 30 else 29
        }

        /** ایندکس روز هفته با شروع از شنبه (۰) تا جمعه (۶). */
        fun weekdayIndex(date: LocalDate): Int = (date.dayOfWeek.value + 1) % 7

        fun weekdayIndex(dow: DayOfWeek): Int = (dow.value + 1) % 7

        /** شنبه‌ی هفته‌ای که این تاریخ در آن است. */
        fun startOfWeek(date: LocalDate): LocalDate = date.minusDays(weekdayIndex(date).toLong())

        fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
            val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            val gy2 = if (gm > 2) gy + 1 else gy
            var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
            var jy = -1595 + 33 * (days / 12053)
            days %= 12053
            jy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                jy += (days - 1) / 365
                days = (days - 1) % 365
            }
            val jm: Int
            val jd: Int
            if (days < 186) {
                jm = 1 + days / 31
                jd = 1 + days % 31
            } else {
                jm = 7 + (days - 186) / 30
                jd = 1 + (days - 186) % 30
            }
            return intArrayOf(jy, jm, jd)
        }

        fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
            val jy2 = jy + 1595
            var days = -355668 + (365 * jy2) + ((jy2 / 33) * 8) + (((jy2 % 33) + 3) / 4) + jd +
                (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)
            var gy = 400 * (days / 146097)
            days %= 146097
            if (days > 36524) {
                days--
                gy += 100 * (days / 36524)
                days %= 36524
                if (days >= 365) days++
            }
            gy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                gy += (days - 1) / 365
                days = (days - 1) % 365
            }
            var gd = days + 1
            val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
            val sal = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            var gm = 0
            while (gm < 13 && gd > sal[gm]) {
                gd -= sal[gm]
                gm++
            }
            return intArrayOf(gy, gm, gd)
        }
    }
}

/** قالب‌های نمایش یکپارچه تاریخ در کل برنامه. */
object DateFormats {
    fun full(d: LocalDate): String {
        val j = JalaliDate.of(d)
        return "${JalaliDate.weekdayNames[JalaliDate.weekdayIndex(d)]}، ${j.day} ${j.monthName} ${j.year}"
    }

    fun short(d: LocalDate): String {
        val j = JalaliDate.of(d)
        return "${j.day} ${j.monthName}"
    }

    fun weekdayAndDay(d: LocalDate): String {
        val j = JalaliDate.of(d)
        return "${JalaliDate.weekdayNames[JalaliDate.weekdayIndex(d)]} ${j.day} ${j.monthName}"
    }

    fun monthYear(j: JalaliDate): String = "${j.monthName} ${j.year}"

    fun clock(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
}
