// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/*
 * Solar Hijri (Persian, Jalali, Shamsi) calendar (Scala 3).
 *
 * Algorithm: 33-year arithmetic rule + 78-year correction table,
 * the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
 * for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
 * Inputs outside MinYear..MaxYear / MinJdn..MaxJdn throw IllegalArgumentException.
 */
package persiancalendar

import java.time.LocalDate

final case class PersianDate(year: Int, month: Int, day: Int)

object PersianCalendar {
  /** Years checked against the astronomical calendar. */
  val VerifiedMinYear = 1178
  val VerifiedMaxYear = 3000

  /** Accepted input range. Within it every intermediate value fits in 32 bits. */
  val MinYear: Int = -100000
  val MaxYear: Int = 100000
  /** 1 Farvardin -100000 (Gregorian -99379-03-29). */
  val MinJdn: Long = -34576287L
  /** 29 Esfand 100000 (Gregorian 100622-03-13). */
  val MaxJdn: Long = 38472562L

  private val EpochJdn = 1948320L
  private val UnixEpochJdn = 2440588L // JDN of 1970-01-01

  /** Years the 33-year rule calls leap but are not; the next year is leap instead. */
  val NonLeapCorrection: Set[Int] = Set(
    1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
    1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
    2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
    2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
    2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
    2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
    2946, 2950, 2954, 2979, 2983, 2987
  )

  val MonthNamesFa: Vector[String] = Vector("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
  val MonthNamesEn: Vector[String] = Vector("Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
    "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand")
  val WeekdayNamesFa: Vector[String] = Vector("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
  val WeekdayNamesEn: Vector[String] = Vector("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

  private def isCorrection(year: Long): Boolean =
    year >= 1502 && year <= 2987 && NonLeapCorrection.contains(year.toInt)

  private def checkYear(year: Int): Unit =
    require(year >= MinYear && year <= MaxYear, s"year out of range [$MinYear, $MaxYear]: $year")

  private def checkJdn(jdn: Long): Unit =
    require(jdn >= MinJdn && jdn <= MaxJdn, s"jdn out of range [$MinJdn, $MaxJdn]: $jdn")

  /** True if the Persian year has 366 days (Esfand has 30 days). */
  def isLeapYear(year: Int): Boolean =
    checkYear(year)
    if (isCorrection(year.toLong)) false
    else if (isCorrection(year - 1L)) true
    else Math.floorMod(25L * year + 11, 33L) < 8

  /** 29, 30 or 31. */
  def daysInMonth(year: Int, month: Int): Int = { checkYear(year); month } match {
    case m if m >= 1 && m <= 6  => 31
    case m if m >= 7 && m <= 11 => 30
    case 12                     => if (isLeapYear(year)) 30 else 29
    case m                      => throw new IllegalArgumentException(s"month out of range [1, 12]: $m")
  }

  /** True for a real Persian date in the accepted range; never throws. */
  def isValidDate(year: Int, month: Int, day: Int): Boolean =
    year >= MinYear && year <= MaxYear && month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth(year, month)

  private def nowruzJdn(year: Long): Long = {
    val jdn = EpochJdn + 365 * (year - 1) + Math.floorDiv(8 * year + 21, 33L)
    if (isCorrection(year - 1)) jdn - 1 else jdn
  }

  private def daysBeforeMonth(month: Int): Int =
    if (month <= 7) 31 * (month - 1) else 30 * (month - 1) + 6

  /** Julian Day Number of a Persian date. */
  def toJdn(year: Int, month: Int, day: Int): Long = {
    require(isValidDate(year, month, day), s"invalid Persian date: $year/$month/$day")
    nowruzJdn(year.toLong) + daysBeforeMonth(month) + day - 1
  }

  /** Persian date of a Julian Day Number in MinJdn..MaxJdn. */
  def fromJdn(jdn: Long): PersianDate = {
    checkJdn(jdn)
    var year = 1 + Math.floorDiv(33 * (jdn - EpochJdn) + 3, 12053L)
    while (jdn >= nowruzJdn(year + 1)) year += 1
    while (jdn < nowruzJdn(year)) year -= 1
    val doy = (jdn - nowruzJdn(year)).toInt // 0-based
    if (doy < 186) PersianDate(year.toInt, doy / 31 + 1, doy % 31 + 1)
    else PersianDate(year.toInt, (doy - 186) / 30 + 7, (doy - 186) % 30 + 1)
  }

  def fromLocalDate(date: LocalDate): PersianDate = fromJdn(date.toEpochDay + UnixEpochJdn)

  def toLocalDate(year: Int, month: Int, day: Int): LocalDate =
    LocalDate.ofEpochDay(toJdn(year, month, day) - UnixEpochJdn)

  /** 0 = Saturday (شنبه) ... 6 = Friday (جمعه). */
  def dayOfWeek(year: Int, month: Int, day: Int): Int =
    Math.floorMod(toJdn(year, month, day) + 2, 7L).toInt

  /** 1 ... 365/366. */
  def dayOfYear(year: Int, month: Int, day: Int): Int = {
    require(isValidDate(year, month, day), s"invalid Persian date: $year/$month/$day")
    daysBeforeMonth(month) + day
  }
}
