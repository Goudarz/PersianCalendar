// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/*
 * Solar Hijri (Persian, Jalali, Shamsi) calendar.
 *
 * Algorithm: 33-year arithmetic rule + 78-year correction table,
 * the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
 * for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
 *
 * Inputs outside MIN_YEAR..MAX_YEAR / MIN_JDN..MAX_JDN throw IllegalArgumentException.
 * JVM and Android (the java.time helpers need API 26+ or core library desugaring).
 */
package persiancalendar

import java.time.LocalDate

data class PersianDate(val year: Int, val month: Int, val day: Int)

object PersianCalendar {
    /** Years checked against the astronomical calendar. */
    const val VERIFIED_MIN_YEAR = 1178
    const val VERIFIED_MAX_YEAR = 3000

    /** Accepted input range. Within it every intermediate value fits in 32 bits. */
    const val MIN_YEAR = -100_000
    const val MAX_YEAR = 100_000
    /** 1 Farvardin -100000 (Gregorian -99379-03-29). */
    const val MIN_JDN = -34_576_287L
    /** 29 Esfand 100000 (Gregorian 100622-03-13). */
    const val MAX_JDN = 38_472_562L

    private const val EPOCH_JDN = 1948320L
    private const val UNIX_EPOCH_JDN = 2440588L // JDN of 1970-01-01

    private val CORRECTIONS = intArrayOf(
        1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
        1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
        2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
        2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
        2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
        2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
        2946, 2950, 2954, 2979, 2983, 2987,
    )

    /** Years the 33-year rule calls leap but are not; the next year is leap instead (immutable copy). */
    val NON_LEAP_CORRECTION: List<Int> = java.util.Collections.unmodifiableList(CORRECTIONS.toList())

    // Unmodifiable lists: cannot be changed even when cast to MutableList (works on every Android API level).
    val MONTH_NAMES_FA: List<String> = names("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val MONTH_NAMES_EN: List<String> = names("Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand")
    val WEEKDAY_NAMES_FA: List<String> = names("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val WEEKDAY_NAMES_EN: List<String> = names("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    private fun names(vararg n: String): List<String> = java.util.Collections.unmodifiableList(n.toList())

    private fun isCorrection(year: Long) =
        year in 1502L..2987L && CORRECTIONS.binarySearch(year.toInt()) >= 0

    private fun checkYear(year: Int) =
        require(year in MIN_YEAR..MAX_YEAR) { "year out of range [$MIN_YEAR, $MAX_YEAR]: $year" }

    private fun checkJdn(jdn: Long) =
        require(jdn in MIN_JDN..MAX_JDN) { "jdn out of range [$MIN_JDN, $MAX_JDN]: $jdn" }

    /** True if the Persian year has 366 days (Esfand has 30 days). */
    fun isLeapYear(year: Int): Boolean {
        checkYear(year)
        return when {
            isCorrection(year.toLong()) -> false
            isCorrection(year - 1L) -> true
            else -> (25L * year + 11).mod(33L) < 8
        }
    }

    /** 29, 30 or 31. */
    fun daysInMonth(year: Int, month: Int): Int {
        checkYear(year)
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isLeapYear(year)) 30 else 29
            else -> throw IllegalArgumentException("month out of range [1, 12]: $month")
        }
    }

    /** True for a real Persian date in the accepted range; never throws. */
    fun isValidDate(year: Int, month: Int, day: Int): Boolean =
        year in MIN_YEAR..MAX_YEAR && month in 1..12 && day >= 1 && day <= daysInMonth(year, month)

    private fun nowruzJdn(year: Long): Long {
        val jdn = EPOCH_JDN + 365 * (year - 1) + (8 * year + 21).floorDiv(33L)
        return if (isCorrection(year - 1)) jdn - 1 else jdn
    }

    private fun daysBeforeMonth(month: Int): Int = if (month <= 7) 31 * (month - 1) else 30 * (month - 1) + 6

    /** Julian Day Number of a Persian date. */
    fun toJdn(year: Int, month: Int, day: Int): Long {
        require(isValidDate(year, month, day)) { "invalid Persian date: $year/$month/$day" }
        return nowruzJdn(year.toLong()) + daysBeforeMonth(month) + day - 1
    }

    /** Persian date of a Julian Day Number in [MIN_JDN]..[MAX_JDN]. */
    fun fromJdn(jdn: Long): PersianDate {
        checkJdn(jdn)
        var year = 1 + (33 * (jdn - EPOCH_JDN) + 3).floorDiv(12053L)
        while (jdn >= nowruzJdn(year + 1)) year++
        while (jdn < nowruzJdn(year)) year--
        val doy = (jdn - nowruzJdn(year)).toInt() // 0-based
        return if (doy < 186) PersianDate(year.toInt(), doy / 31 + 1, doy % 31 + 1)
        else PersianDate(year.toInt(), (doy - 186) / 30 + 7, (doy - 186) % 30 + 1)
    }

    /** 0 = Saturday (شنبه) ... 6 = Friday (جمعه). */
    fun dayOfWeek(year: Int, month: Int, day: Int): Int = (toJdn(year, month, day) + 2).mod(7L).toInt()

    /** 1 ... 365/366. */
    fun dayOfYear(year: Int, month: Int, day: Int): Int {
        require(isValidDate(year, month, day)) { "invalid Persian date: $year/$month/$day" }
        return daysBeforeMonth(month) + day
    }

    // JVM / Android (API 26+, or with desugaring) interop
    fun fromLocalDate(date: LocalDate): PersianDate = fromJdn(date.toEpochDay() + UNIX_EPOCH_JDN)
    fun toLocalDate(year: Int, month: Int, day: Int): LocalDate =
        LocalDate.ofEpochDay(toJdn(year, month, day) - UNIX_EPOCH_JDN)
}
