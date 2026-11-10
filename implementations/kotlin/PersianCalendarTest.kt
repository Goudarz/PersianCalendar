// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Checks PersianCalendar.kt against /data.
// Run: kotlinc *.kt -include-runtime -d test.jar && java -jar test.jar ../../data
package persiancalendar

import java.io.File
import java.time.LocalDate

private var checks = 0

private fun check(ok: Boolean, context: () -> Any) {
    checks++
    if (!ok) throw AssertionError("failed: ${context()}")
}

fun main(args: Array<String>) {
    val dataDir = args.firstOrNull() ?: "../../data"
    fun csv(name: String) = File(dataDir, name).readLines().drop(1).filter { it.isNotBlank() }.map { it.split(",") }
    val pc = PersianCalendar

    for (r in csv("years.csv")) {
        val y = r[0].toInt()
        val leap = r[1] == "1"
        check(pc.isLeapYear(y) == leap) { "leap $y" }
        check(pc.daysInMonth(y, 12) == if (leap) 30 else 29) { "esfand $y" }
        check(pc.toLocalDate(y, 1, 1) == LocalDate.parse(r[3])) { "nowruz $y" }
        check(pc.toJdn(y, 1, 1) == r[4].toLong()) { "jdn $y" }
        // every day of the year against the ground truth, both directions
        for (doy in 0 until r[2].toInt()) {
            val m = if (doy < 186) doy / 31 + 1 else (doy - 186) / 30 + 7
            val d = if (doy < 186) doy % 31 + 1 else (doy - 186) % 30 + 1
            val jdn = r[4].toLong() + doy
            check(pc.fromJdn(jdn) == PersianDate(y, m, d) && pc.toJdn(y, m, d) == jdn) { "$y/$m/$d" }
        }
        check(pc.dayOfWeek(y, 1, 1) == r[5].toInt()) { "weekday $y" }
    }
    for (r in csv("test-vectors.csv")) {
        val v = r.map { it.toInt() }
        val p = PersianDate(v[0], v[1], v[2])
        val g = LocalDate.of(v[3], v[4], v[5])
        check(pc.toLocalDate(v[0], v[1], v[2]) == g) { p }
        check(pc.fromLocalDate(g) == p) { g }
        check(pc.toJdn(v[0], v[1], v[2]) == v[6].toLong()) { p }
        check(pc.fromJdn(v[6].toLong()) == p) { p }
        check(pc.dayOfWeek(v[0], v[1], v[2]) == v[7]) { p }
    }
    val first = pc.toJdn(PersianCalendar.VERIFIED_MIN_YEAR, 1, 1)
    val lastYear = PersianCalendar.VERIFIED_MAX_YEAR
    val last = pc.toJdn(lastYear, 12, pc.daysInMonth(lastYear, 12))
    var prev = pc.fromJdn(first - 1)
    for (jdn in first..last) {
        val d = pc.fromJdn(jdn)
        check(pc.toJdn(d.year, d.month, d.day) == jdn) { jdn }
        val consecutive = (d.day == prev.day + 1 && d.month == prev.month && d.year == prev.year) ||
            (d.day == 1 && ((d.month == prev.month + 1 && d.year == prev.year) ||
                (d.month == 1 && prev.month == 12 && d.year == prev.year + 1)))
        check(consecutive) { jdn }
        prev = d
    }
    // Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
    check(pc.toJdn(-1000, 1, 1) == 1582713L) { "-1000" }
    check(pc.toJdn(0, 1, 1) == 1947955L) { "0" }
    check(pc.toJdn(4000, 1, 1) == 3408925L) { "4000" }
    check(pc.fromJdn(0) == PersianDate(-5334, 9, 3)) { "jdn 0" }
    check(pc.dayOfWeek(-1000, 1, 1) == 1) { "weekday -1000" }
    check(pc.isLeapYear(-3) && !pc.isLeapYear(-1000)) { "negative leap years" }
    for ((firstYear, lastYear) in listOf(-1000 to 1177, 3001 to 4000)) {
        val a = pc.toJdn(firstYear, 1, 1)
        val b = pc.toJdn(lastYear, 12, pc.daysInMonth(lastYear, 12))
        var p = pc.fromJdn(a - 1)
        for (jdn in a..b) {
            val d = pc.fromJdn(jdn)
            check(pc.isValidDate(d.year, d.month, d.day) && pc.toJdn(d.year, d.month, d.day) == jdn) { jdn }
            val consecutive = (d.day == p.day + 1 && d.month == p.month && d.year == p.year) ||
                (d.day == 1 && ((d.month == p.month + 1 && d.year == p.year) ||
                    (d.month == 1 && p.month == 12 && d.year == p.year + 1)))
            check(consecutive) { jdn }
            p = d
        }
    }
    check(pc.isValidDate(1403, 12, 30)) { "1403/12/30" }
    check(!pc.isValidDate(1404, 12, 30)) { "1404/12/30" }
    check(pc.isValidDate(1404, 6, 31)) { "1404/6/31" }
    check(!pc.isValidDate(1404, 7, 31)) { "1404/7/31" }
    check(!pc.isValidDate(1404, 13, 1)) { "1404/13/1" }
    check(!pc.isValidDate(1404, 0, 1)) { "1404/0/1" }
    check(!pc.isValidDate(1404, 1, 0)) { "1404/1/0" }
    check(runCatching { pc.toJdn(1404, 12, 30) }.isFailure) { "should throw" }
    check(pc.fromLocalDate(LocalDate.of(2025, 3, 21)) == PersianDate(1404, 1, 1)) { "2025-03-21" }
    check(pc.fromLocalDate(LocalDate.of(1979, 2, 11)) == PersianDate(1357, 11, 22)) { "1979-02-11" }
    check(pc.dayOfYear(1404, 7, 1) == 187) { "doy" }
    check(PersianCalendar.WEEKDAY_NAMES_EN[pc.dayOfWeek(1404, 1, 1)] == "Friday") { "friday" }
    // Limits: exact boundaries, and fast rejection of anything outside (no hang, no overflow).
    check(pc.toJdn(PersianCalendar.MIN_YEAR, 1, 1) == PersianCalendar.MIN_JDN) { "MIN_JDN" }
    check(pc.toJdn(PersianCalendar.MAX_YEAR, 12, 29) == PersianCalendar.MAX_JDN) { "MAX_JDN" }
    check(pc.fromJdn(PersianCalendar.MIN_JDN) == PersianDate(-100_000, 1, 1)) { "from MIN_JDN" }
    check(pc.fromJdn(PersianCalendar.MAX_JDN) == PersianDate(100_000, 12, 29)) { "from MAX_JDN" }
    for (bad in listOf(PersianCalendar.MIN_JDN - 1, PersianCalendar.MAX_JDN + 1, Long.MAX_VALUE, Long.MIN_VALUE, 1L shl 53)) {
        check(runCatching { pc.fromJdn(bad) }.exceptionOrNull() is IllegalArgumentException) { "fromJdn $bad" }
    }
    for (bad in listOf(PersianCalendar.MIN_YEAR - 1, PersianCalendar.MAX_YEAR + 1, Int.MAX_VALUE, Int.MIN_VALUE)) {
        check(!pc.isValidDate(bad, 1, 1)) { "isValidDate $bad" }
        check(runCatching { pc.toJdn(bad, 1, 1) }.exceptionOrNull() is IllegalArgumentException) { "toJdn $bad" }
        check(runCatching { pc.isLeapYear(bad) }.exceptionOrNull() is IllegalArgumentException) { "isLeapYear $bad" }
    }
    check(runCatching { pc.fromLocalDate(LocalDate.MAX) }.exceptionOrNull() is IllegalArgumentException) { "LocalDate.MAX" }
    @Suppress("UNCHECKED_CAST")
    check(runCatching { (PersianCalendar.MONTH_NAMES_FA as MutableList<String>)[0] = "X" }.isFailure) { "immutable names" }
    @Suppress("UNCHECKED_CAST")
    check(runCatching { (PersianCalendar.NON_LEAP_CORRECTION as MutableList<Int>)[0] = 1 }.isFailure) { "immutable table" }
    check(PersianCalendar.WEEKDAY_NAMES_FA[3] == "سه‌شنبه") { "ZWNJ" }
    // Every year MIN_YEAR..MAX_YEAR: years are contiguous, first and last day round-trip, and the leap
    // years sit where every other implementation (and ICU 77.1+) puts them (checksums).
    var expected = PersianCalendar.MIN_JDN
    var leaps = 0L
    var nowruzSum = 0L
    for (y in PersianCalendar.MIN_YEAR..PersianCalendar.MAX_YEAR) {
        val nowruz = pc.toJdn(y, 1, 1)
        val leap = if (pc.isLeapYear(y)) 1 else 0
        if (nowruz != expected || pc.fromJdn(nowruz) != PersianDate(y, 1, 1) || pc.daysInMonth(y, 12) != 29 + leap ||
            pc.fromJdn(nowruz + 364 + leap) != PersianDate(y, 12, 29 + leap)
        ) {
            check(false) { "year $y" }
        }
        leaps += leap
        nowruzSum += nowruz
        expected = nowruz + 365 + leap
    }
    check(expected - 1 == PersianCalendar.MAX_JDN && leaps == 48_485L && nowruzSum == 389_592_978_180L) { "full-range checksums" }
    println("OK: $checks checks passed")
}
