// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares ICU4J, this repository's Kotlin implementation and the ground truth
// (data/years.csv) for every day of 1178-3000 AP, in both directions.
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the program checks that ICU matches the plain 33-year rule exactly instead.
// Run (see ../README.md):
//   kotlinc -cp icu4j-X.jar IcuCheck.kt ../../../implementations/kotlin/PersianCalendar.kt -include-runtime -d check.jar
//   java -cp "icu4j-X.jar:check.jar" IcuCheckKt ../../../data/years.csv
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import com.ibm.icu.util.VersionInfo
import java.io.File
import kotlin.system.exitProcess
import persiancalendar.PersianCalendar
import persiancalendar.PersianDate

/** Nowruz JDN under the plain 33-year rule (what ICU 50-76 implements). */
fun plainNowruz(y: Int): Long = 1948320L + 365L * (y - 1) + Math.floorDiv(8L * y + 21, 33L)

fun monthDay(year: Int, doy: Int): PersianDate =
    if (doy < 186) PersianDate(year, doy / 31 + 1, doy % 31 + 1)
    else PersianDate(year, (doy - 186) / 30 + 7, (doy - 186) % 30 + 1)

fun plainFromJdn(jdn: Long): PersianDate {
    var y = (Math.floorDiv(jdn - 1948320, 366L) + 1).toInt()
    while (jdn >= plainNowruz(y + 1)) y++
    while (jdn < plainNowruz(y)) y--
    return monthDay(y, (jdn - plainNowruz(y)).toInt())
}

fun main(args: Array<String>) {
    val years = File(args[0]).readLines().drop(1).filter { it.isNotEmpty() }.map { it.split(",") }
    val icu = Calendar.getInstance(TimeZone.GMT_ZONE, ULocale("fa_IR@calendar=persian"))
    val hasCorrections = VersionInfo.ICU_VERSION.major >= 77
    var days = 0
    var referenceWrong = 0
    var toPersianWrong = 0
    var fromPersianWrong = 0
    var differs = 0
    for (r in years) {
        val year = r[0].toInt()
        val nowruz = r[4].toLong()
        for (doy in 0 until if (r[1] == "1") 366 else 365) {
            days++
            val jdn = nowruz + doy
            val truth = monthDay(year, doy)
            if (PersianCalendar.fromJdn(jdn) != truth || PersianCalendar.toJdn(year, truth.month, truth.day) != jdn) {
                referenceWrong++
            }
            val epochDay = jdn - 2440588 // days since 1970-01-01
            val expected = if (hasCorrections) truth else plainFromJdn(jdn)
            icu.clear()
            icu.timeInMillis = epochDay * 86_400_000L
            val got = PersianDate(icu.get(Calendar.EXTENDED_YEAR), icu.get(Calendar.MONTH) + 1, icu.get(Calendar.DAY_OF_MONTH))
            if (got != expected) toPersianWrong++
            if (got != truth) differs++
            // Persian fields -> instant; under the plain rule, compare with the plain rule's own date.
            icu.clear()
            icu.set(Calendar.EXTENDED_YEAR, expected.year)
            icu.set(Calendar.MONTH, expected.month - 1)
            icu.set(Calendar.DAY_OF_MONTH, expected.day)
            if (Math.floorDiv(icu.timeInMillis, 86_400_000L) != epochDay) fromPersianWrong++
        }
    }
    val rule = if (hasCorrections) "the ground truth" else "the plain 33-year rule"
    val ok = if (toPersianWrong + fromPersianWrong == 0) "yes (both directions)" else "NO (G->P $toPersianWrong, P->G $fromPersianWrong)"
    println(
        "Kotlin + ICU4J ${VersionInfo.ICU_VERSION}: $days days (1178-3000 AP) | implementation wrong: $referenceWrong | " +
            "ICU differs from ground truth on $differs days | ICU matches $rule: $ok",
    )
    exitProcess(if (referenceWrong + toPersianWrong + fromPersianWrong == 0) 0 else 1)
}
