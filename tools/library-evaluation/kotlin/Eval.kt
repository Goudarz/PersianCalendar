// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Evaluates persian-calendar/calendar against this repository's implementation. See run.sh.
import io.github.persiancalendar.calendar.CivilDate
import java.io.File
import io.github.persiancalendar.calendar.PersianDate as LibDate
import persiancalendar.PersianCalendar as Ref

fun main(args: Array<String>) {
    val name = "persian-calendar/calendar ${args[0]} (Kotlin)"
    val file = File(args[1], "persian-calendar_calendar_${args[0]}")
    val lines = StringBuilder("# $name\n")
    for (y in 1100..3300) {
        runCatching {
            val g = CivilDate(LibDate(y, 1, 1).toJdn())
            lines.append("%d %04d-%02d-%02d\n".format(y, g.year, g.month, g.dayOfMonth))
        }
    }
    File("$file.txt").writeText(lines.toString())

    // Every day from 1 Farvardin 1304 (the modern calendar; earlier years use the historical
    // Borji calendar in this library, by design) to the end of 1500.
    var days = 0
    var g2p = 0
    var p2g = 0
    for (jdn in Ref.toJdn(1304, 1, 1)..Ref.toJdn(1500, 12, 29)) {
        days++
        val expected = Ref.fromJdn(jdn)
        val lib = LibDate(jdn)
        if (lib.year != expected.year || lib.month != expected.month || lib.dayOfMonth != expected.day) g2p++
        if (runCatching { LibDate(expected.year, expected.month, expected.day).toJdn() }.getOrNull() != jdn) p2g++
    }
    File("$file.everyday.json").writeText(
        """{"library": "$name", "range": "1304-1500", "days": $days, "g2pWrong": $g2p, "p2gWrong": $p2g}""" + "\n"
    )
    println("$name: $days days, wrong G->P $g2p, P->G $p2g")
}
