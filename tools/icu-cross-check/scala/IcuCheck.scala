// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares ICU4J, this repository's Scala implementation and the ground truth
// (data/years.csv) for every day of 1178-3000 AP, in both directions.
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the program checks that ICU matches the plain 33-year rule exactly instead.
// Run (see ../README.md):
//   scala run IcuCheck.scala ../../../implementations/scala/PersianCalendar.scala
//     --dep com.ibm.icu:icu4j:78.3 --main-class IcuCheck -- ../../../data/years.csv
import com.ibm.icu.util.{Calendar, TimeZone, ULocale, VersionInfo}
import persiancalendar.{PersianCalendar, PersianDate}

object IcuCheck:
  /** Nowruz JDN under the plain 33-year rule (what ICU 50-76 implements). */
  def plainNowruz(y: Int): Long = 1948320L + 365L * (y - 1) + Math.floorDiv(8L * y + 21, 33L)

  def monthDay(year: Int, doy: Int): PersianDate =
    if doy < 186 then PersianDate(year, doy / 31 + 1, doy % 31 + 1)
    else PersianDate(year, (doy - 186) / 30 + 7, (doy - 186) % 30 + 1)

  def plainFromJdn(jdn: Long): PersianDate =
    var y = (Math.floorDiv(jdn - 1948320, 366L) + 1).toInt
    while jdn >= plainNowruz(y + 1) do y += 1
    while jdn < plainNowruz(y) do y -= 1
    monthDay(y, (jdn - plainNowruz(y)).toInt)

  def main(args: Array[String]): Unit =
    val source = scala.io.Source.fromFile(args(0), "UTF-8")
    val years = try source.getLines().drop(1).filter(_.nonEmpty).map(_.split(",")).toVector finally source.close()
    val icu = Calendar.getInstance(TimeZone.GMT_ZONE, ULocale("fa_IR@calendar=persian"))
    val hasCorrections = VersionInfo.ICU_VERSION.getMajor >= 77
    var days, referenceWrong, toPersianWrong, fromPersianWrong, differs = 0
    for r <- years do
      val year = r(0).toInt
      val nowruz = r(4).toLong
      for doy <- 0 until (if r(1) == "1" then 366 else 365) do
        days += 1
        val jdn = nowruz + doy
        val truth = monthDay(year, doy)
        if PersianCalendar.fromJdn(jdn) != truth || PersianCalendar.toJdn(year, truth.month, truth.day) != jdn then
          referenceWrong += 1
        val epochDay = jdn - 2440588 // days since 1970-01-01
        val expected = if hasCorrections then truth else plainFromJdn(jdn)
        icu.clear()
        icu.setTimeInMillis(epochDay * 86400000L)
        val got = PersianDate(icu.get(Calendar.EXTENDED_YEAR), icu.get(Calendar.MONTH) + 1, icu.get(Calendar.DAY_OF_MONTH))
        if got != expected then toPersianWrong += 1
        if got != truth then differs += 1
        // Persian fields -> instant; under the plain rule, compare with the plain rule's own date.
        icu.clear()
        icu.set(Calendar.EXTENDED_YEAR, expected.year)
        icu.set(Calendar.MONTH, expected.month - 1)
        icu.set(Calendar.DAY_OF_MONTH, expected.day)
        if Math.floorDiv(icu.getTimeInMillis, 86400000L) != epochDay then fromPersianWrong += 1
    val rule = if hasCorrections then "the ground truth" else "the plain 33-year rule"
    val ok =
      if toPersianWrong + fromPersianWrong == 0 then "yes (both directions)"
      else s"NO (G->P $toPersianWrong, P->G $fromPersianWrong)"
    println(s"Scala + ICU4J ${VersionInfo.ICU_VERSION}: $days days (1178-3000 AP) | implementation wrong: $referenceWrong | " +
      s"ICU differs from ground truth on $differs days | ICU matches $rule: $ok")
    sys.exit(if referenceWrong + toPersianWrong + fromPersianWrong == 0 then 0 else 1)
