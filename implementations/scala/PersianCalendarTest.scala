// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Checks PersianCalendar.scala against /data.
// Run: scala run . --main-class persiancalendar.PersianCalendarTest -- ../../data
package persiancalendar

import java.nio.file.{Files, Path}
import java.time.LocalDate
import scala.jdk.CollectionConverters._

object PersianCalendarTest {
  private var checks = 0L

  private def check(ok: Boolean, context: => Any): Unit = {
    checks += 1
    if (!ok) throw new AssertionError(s"failed: $context")
  }

  def main(args: Array[String]): Unit = {
    val dataDir = args.headOption.getOrElse("../../data")
    def csv(name: String): Seq[Array[String]] =
      Files.readAllLines(Path.of(dataDir, name)).asScala.toSeq.drop(1).filter(_.nonEmpty).map(_.split(","))
    val pc = PersianCalendar

    for (r <- csv("years.csv")) {
      val y = r(0).toInt
      val leap = r(1) == "1"
      check(pc.isLeapYear(y) == leap, s"leap $y")
      check(pc.daysInMonth(y, 12) == (if (leap) 30 else 29), s"esfand $y")
      check(pc.toLocalDate(y, 1, 1) == LocalDate.parse(r(3)), s"nowruz $y")
      check(pc.toJdn(y, 1, 1) == r(4).toLong, s"jdn $y")
      // every day of the year against the ground truth, both directions
      for (doy <- 0 until r(2).toInt) {
        val m = if (doy < 186) doy / 31 + 1 else (doy - 186) / 30 + 7
        val d = if (doy < 186) doy % 31 + 1 else (doy - 186) % 30 + 1
        val jdn = r(4).toLong + doy
        check(pc.fromJdn(jdn) == PersianDate(y, m, d) && pc.toJdn(y, m, d) == jdn, s"$y/$m/$d")
      }
      check(pc.dayOfWeek(y, 1, 1) == r(5).toInt, s"weekday $y")
    }

    for (r <- csv("test-vectors.csv")) {
      val v = r.map(_.toInt)
      val p = PersianDate(v(0), v(1), v(2))
      val g = LocalDate.of(v(3), v(4), v(5))
      check(pc.toLocalDate(v(0), v(1), v(2)) == g, p)
      check(pc.fromLocalDate(g) == p, g)
      check(pc.toJdn(v(0), v(1), v(2)) == v(6).toLong, p)
      check(pc.fromJdn(v(6).toLong) == p, p)
      check(pc.dayOfWeek(v(0), v(1), v(2)) == v(7), p)
    }

    val first = pc.toJdn(pc.VerifiedMinYear, 1, 1)
    val last = pc.toJdn(pc.VerifiedMaxYear, 12, pc.daysInMonth(pc.VerifiedMaxYear, 12))
    var prev = pc.fromJdn(first - 1)
    var jdn = first
    while (jdn <= last) {
      val d = pc.fromJdn(jdn)
      check(pc.toJdn(d.year, d.month, d.day) == jdn, jdn)
      val consecutive = (d.day == prev.day + 1 && d.month == prev.month && d.year == prev.year) ||
        (d.day == 1 && ((d.month == prev.month + 1 && d.year == prev.year) ||
          (d.month == 1 && prev.month == 12 && d.year == prev.year + 1)))
      check(consecutive, jdn)
      prev = d
      jdn += 1
    }

    // Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
    check(pc.toJdn(-1000, 1, 1) == 1582713L, "-1000")
    check(pc.toJdn(0, 1, 1) == 1947955L, "0")
    check(pc.toJdn(4000, 1, 1) == 3408925L, "4000")
    check(pc.fromJdn(0) == PersianDate(-5334, 9, 3), "jdn 0")
    check(pc.dayOfWeek(-1000, 1, 1) == 1, "weekday -1000")
    check(pc.isLeapYear(-3) && !pc.isLeapYear(-1000), "negative leap years")
    for ((firstYear, lastYear) <- Seq((-1000, 1177), (3001, 4000))) {
      val a = pc.toJdn(firstYear, 1, 1)
      val b = pc.toJdn(lastYear, 12, pc.daysInMonth(lastYear, 12))
      var p = pc.fromJdn(a - 1)
      var j = a
      while (j <= b) {
        val d = pc.fromJdn(j)
        check(pc.isValidDate(d.year, d.month, d.day) && pc.toJdn(d.year, d.month, d.day) == j, j)
        val consecutive = (d.day == p.day + 1 && d.month == p.month && d.year == p.year) ||
          (d.day == 1 && ((d.month == p.month + 1 && d.year == p.year) ||
            (d.month == 1 && p.month == 12 && d.year == p.year + 1)))
        check(consecutive, j)
        p = d
        j += 1
      }
    }
    check(pc.isValidDate(1403, 12, 30), "1403/12/30")
    check(!pc.isValidDate(1404, 12, 30), "1404/12/30")
    check(pc.isValidDate(1404, 6, 31), "1404/6/31")
    check(!pc.isValidDate(1404, 7, 31), "1404/7/31")
    check(!pc.isValidDate(1404, 13, 1), "1404/13/1")
    check(!pc.isValidDate(1404, 0, 1), "1404/0/1")
    check(!pc.isValidDate(1404, 1, 0), "1404/1/0")
    check(scala.util.Try(pc.toJdn(1404, 12, 30)).isFailure, "should throw")
    check(pc.fromLocalDate(LocalDate.of(2025, 3, 21)) == PersianDate(1404, 1, 1), "2025-03-21")
    check(pc.fromLocalDate(LocalDate.of(1979, 2, 11)) == PersianDate(1357, 11, 22), "1979-02-11")
    check(pc.dayOfYear(1404, 7, 1) == 187, "doy")
    check(pc.WeekdayNamesEn(pc.dayOfWeek(1404, 1, 1)) == "Friday", "friday")
    // Limits: exact boundaries, and fast rejection of anything outside (no hang, no overflow).
    check(pc.toJdn(pc.MinYear, 1, 1) == pc.MinJdn, "MinJdn")
    check(pc.toJdn(pc.MaxYear, 12, 29) == pc.MaxJdn, "MaxJdn")
    check(pc.fromJdn(pc.MinJdn) == PersianDate(-100000, 1, 1), "from MinJdn")
    check(pc.fromJdn(pc.MaxJdn) == PersianDate(100000, 12, 29), "from MaxJdn")
    def illegal(f: => Any): Boolean = scala.util.Try(f).failed.toOption.exists(_.isInstanceOf[IllegalArgumentException])
    for (bad <- Seq(pc.MinJdn - 1, pc.MaxJdn + 1, Long.MaxValue, Long.MinValue, 1L << 53)) check(illegal(pc.fromJdn(bad)), s"fromJdn $bad")
    for (bad <- Seq(pc.MinYear - 1, pc.MaxYear + 1, Int.MaxValue, Int.MinValue)) {
      check(!pc.isValidDate(bad, 1, 1), s"isValidDate $bad")
      check(illegal(pc.toJdn(bad, 1, 1)), s"toJdn $bad")
      check(illegal(pc.isLeapYear(bad)), s"isLeapYear $bad")
    }
    check(illegal(pc.fromLocalDate(LocalDate.MAX)), "LocalDate.MAX")
    // Every year MinYear..MaxYear: years are contiguous, first and last day round-trip, and the leap
    // years sit where every other implementation (and ICU 77.1+) puts them (checksums).
    var expected = pc.MinJdn
    var leaps = 0L
    var nowruzSum = 0L
    for (y <- pc.MinYear to pc.MaxYear) {
      val nowruz = pc.toJdn(y, 1, 1)
      val leap = if (pc.isLeapYear(y)) 1 else 0
      if (nowruz != expected || pc.fromJdn(nowruz) != PersianDate(y, 1, 1) || pc.daysInMonth(y, 12) != 29 + leap ||
          pc.fromJdn(nowruz + 364 + leap) != PersianDate(y, 12, 29 + leap)) check(false, s"year $y")
      leaps += leap
      nowruzSum += nowruz
      expected = nowruz + 365 + leap
    }
    check(expected - 1 == pc.MaxJdn && leaps == 48485L && nowruzSum == 389592978180L, "full-range checksums")
    println(s"OK: $checks checks passed")
  }
}
