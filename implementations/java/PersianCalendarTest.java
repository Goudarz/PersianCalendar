// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Checks PersianCalendar against /data. Run: javac *.java && java PersianCalendarTest */
public class PersianCalendarTest {
    static int checks = 0;

    static void check(boolean ok, Object context) {
        checks++;
        if (!ok) throw new AssertionError("failed: " + context);
    }

    static List<String[]> csv(String name) throws Exception {
        Path path = Path.of(System.getProperty("data", "../../data"), name);
        return Files.readAllLines(path).stream().skip(1).filter(l -> !l.isBlank()).map(l -> l.split(",")).toList();
    }

    public static void main(String[] args) throws Exception {
        for (String[] r : csv("years.csv")) {
            int y = Integer.parseInt(r[0]);
            boolean leap = r[1].equals("1");
            check(PersianCalendar.isLeapYear(y) == leap, "leap " + y);
            check(PersianCalendar.daysInMonth(y, 12) == (leap ? 30 : 29), "esfand " + y);
            check(PersianCalendar.toLocalDate(y, 1, 1).equals(LocalDate.parse(r[3])), "nowruz " + y);
            check(PersianCalendar.toJdn(y, 1, 1) == Long.parseLong(r[4]), "jdn " + y);
            // every day of the year against the ground truth, both directions
            for (int doy = 0; doy < Integer.parseInt(r[2]); doy++) {
                int m = doy < 186 ? doy / 31 + 1 : (doy - 186) / 30 + 7;
                int d = doy < 186 ? doy % 31 + 1 : (doy - 186) % 30 + 1;
                long jdn = Long.parseLong(r[4]) + doy;
                check(PersianCalendar.fromJdn(jdn).equals(new PersianCalendar.PersianDate(y, m, d))
                    && PersianCalendar.toJdn(y, m, d) == jdn, y + "/" + m + "/" + d);
            }
            check(PersianCalendar.dayOfWeek(y, 1, 1) == Integer.parseInt(r[5]), "weekday " + y);
        }
        for (String[] r : csv("test-vectors.csv")) {
            int[] v = new int[8];
            for (int i = 0; i < 8; i++) v[i] = Integer.parseInt(r[i]);
            var p = new PersianCalendar.PersianDate(v[0], v[1], v[2]);
            var g = LocalDate.of(v[3], v[4], v[5]);
            check(PersianCalendar.toLocalDate(v[0], v[1], v[2]).equals(g), p);
            check(PersianCalendar.fromLocalDate(g).equals(p), g);
            check(PersianCalendar.toJdn(v[0], v[1], v[2]) == v[6], p);
            check(PersianCalendar.fromJdn(v[6]).equals(p), p);
            check(PersianCalendar.dayOfWeek(v[0], v[1], v[2]) == v[7], p);
        }
        long first = PersianCalendar.toJdn(PersianCalendar.VERIFIED_MIN_YEAR, 1, 1);
        long last = PersianCalendar.toJdn(PersianCalendar.VERIFIED_MAX_YEAR, 12,
            PersianCalendar.daysInMonth(PersianCalendar.VERIFIED_MAX_YEAR, 12));
        var prev = PersianCalendar.fromJdn(first - 1);
        for (long jdn = first; jdn <= last; jdn++) {
            var d = PersianCalendar.fromJdn(jdn);
            check(PersianCalendar.isValidDate(d.year(), d.month(), d.day()), jdn);
            check(PersianCalendar.toJdn(d.year(), d.month(), d.day()) == jdn, jdn);
            boolean consecutive = (d.day() == prev.day() + 1 && d.month() == prev.month() && d.year() == prev.year())
                || (d.day() == 1 && ((d.month() == prev.month() + 1 && d.year() == prev.year())
                    || (d.month() == 1 && prev.month() == 12 && d.year() == prev.year() + 1)));
            check(consecutive, jdn);
            prev = d;
        }
        // Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
        check(PersianCalendar.toJdn(-1000, 1, 1) == 1582713, "-1000");
        check(PersianCalendar.toJdn(0, 1, 1) == 1947955, "0");
        check(PersianCalendar.toJdn(4000, 1, 1) == 3408925, "4000");
        check(PersianCalendar.fromJdn(0).equals(new PersianCalendar.PersianDate(-5334, 9, 3)), "jdn 0");
        check(PersianCalendar.dayOfWeek(-1000, 1, 1) == 1, "weekday -1000");
        check(PersianCalendar.isLeapYear(-3) && !PersianCalendar.isLeapYear(-1000), "negative leap years");
        for (int[] range : new int[][] {{-1000, 1177}, {3001, 4000}}) {
            long a = PersianCalendar.toJdn(range[0], 1, 1);
            long b = PersianCalendar.toJdn(range[1], 12, PersianCalendar.daysInMonth(range[1], 12));
            var p = PersianCalendar.fromJdn(a - 1);
            for (long jdn = a; jdn <= b; jdn++) {
                var d = PersianCalendar.fromJdn(jdn);
                check(PersianCalendar.isValidDate(d.year(), d.month(), d.day())
                    && PersianCalendar.toJdn(d.year(), d.month(), d.day()) == jdn, jdn);
                boolean consecutive = (d.day() == p.day() + 1 && d.month() == p.month() && d.year() == p.year())
                    || (d.day() == 1 && ((d.month() == p.month() + 1 && d.year() == p.year())
                        || (d.month() == 1 && p.month() == 12 && d.year() == p.year() + 1)));
                check(consecutive, jdn);
                p = d;
            }
        }
        check(PersianCalendar.isValidDate(1403, 12, 30), "1403/12/30");
        check(!PersianCalendar.isValidDate(1404, 12, 30), "1404/12/30");
        check(PersianCalendar.isValidDate(1404, 6, 31), "1404/6/31");
        check(!PersianCalendar.isValidDate(1404, 7, 31), "1404/7/31");
        check(!PersianCalendar.isValidDate(1404, 13, 1), "1404/13/1");
        check(!PersianCalendar.isValidDate(1404, 0, 1), "1404/0/1");
        check(!PersianCalendar.isValidDate(1404, 1, 0), "1404/1/0");
        try {
            PersianCalendar.toJdn(1404, 12, 30);
            check(false, "should throw");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
        check(PersianCalendar.fromLocalDate(LocalDate.of(2025, 3, 21)).equals(new PersianCalendar.PersianDate(1404, 1, 1)), "2025-03-21");
        check(PersianCalendar.fromLocalDate(LocalDate.of(1979, 2, 11)).equals(new PersianCalendar.PersianDate(1357, 11, 22)), "1979-02-11");
        check(PersianCalendar.dayOfYear(1404, 7, 1) == 187, "doy");
        check(PersianCalendar.WEEKDAY_NAMES_EN.get(PersianCalendar.dayOfWeek(1404, 1, 1)).equals("Friday"), "friday");
        // Limits: exact boundaries, and fast rejection of anything outside (no hang, no overflow).
        check(PersianCalendar.toJdn(PersianCalendar.MIN_YEAR, 1, 1) == PersianCalendar.MIN_JDN, "MIN_JDN");
        check(PersianCalendar.toJdn(PersianCalendar.MAX_YEAR, 12, 29) == PersianCalendar.MAX_JDN, "MAX_JDN");
        check(PersianCalendar.fromJdn(PersianCalendar.MIN_JDN).equals(new PersianCalendar.PersianDate(-100_000, 1, 1)), "from MIN_JDN");
        check(PersianCalendar.fromJdn(PersianCalendar.MAX_JDN).equals(new PersianCalendar.PersianDate(100_000, 12, 29)), "from MAX_JDN");
        for (long bad : new long[] {PersianCalendar.MIN_JDN - 1, PersianCalendar.MAX_JDN + 1, Long.MAX_VALUE, Long.MIN_VALUE, 1L << 53}) {
            throwsIllegalArgument(() -> PersianCalendar.fromJdn(bad), "fromJdn " + bad);
        }
        for (int bad : new int[] {PersianCalendar.MIN_YEAR - 1, PersianCalendar.MAX_YEAR + 1, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            check(!PersianCalendar.isValidDate(bad, 1, 1), "isValidDate " + bad);
            throwsIllegalArgument(() -> PersianCalendar.toJdn(bad, 1, 1), "toJdn " + bad);
            throwsIllegalArgument(() -> PersianCalendar.isLeapYear(bad), "isLeapYear " + bad);
        }
        throwsIllegalArgument(() -> PersianCalendar.fromLocalDate(LocalDate.MAX), "fromLocalDate(MAX)");
        throwsIllegalArgument(() -> PersianCalendar.fromLocalDate(LocalDate.MIN), "fromLocalDate(MIN)");
        try {
            PersianCalendar.MONTH_NAMES_FA.set(0, "X");
            check(false, "MONTH_NAMES_FA must be immutable");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
        // Every year MIN_YEAR..MAX_YEAR: years are contiguous, first and last day round-trip, and the leap
        // years sit where every other implementation (and ICU 77.1+) puts them (checksums).
        long expected = PersianCalendar.MIN_JDN, leaps = 0, nowruzSum = 0;
        for (int y = PersianCalendar.MIN_YEAR; y <= PersianCalendar.MAX_YEAR; y++) {
            long nowruz = PersianCalendar.toJdn(y, 1, 1);
            int leap = PersianCalendar.isLeapYear(y) ? 1 : 0;
            if (nowruz != expected || !PersianCalendar.fromJdn(nowruz).equals(new PersianCalendar.PersianDate(y, 1, 1))
                    || PersianCalendar.daysInMonth(y, 12) != 29 + leap
                    || !PersianCalendar.fromJdn(nowruz + 364 + leap).equals(new PersianCalendar.PersianDate(y, 12, 29 + leap))) {
                check(false, "year " + y);
            }
            leaps += leap;
            nowruzSum += nowruz;
            expected = nowruz + 365 + leap;
        }
        check(expected - 1 == PersianCalendar.MAX_JDN && leaps == 48_485 && nowruzSum == 389_592_978_180L, "full-range checksums");
        System.out.println("OK: " + checks + " checks passed");
    }

    static void throwsIllegalArgument(Runnable r, String context) {
        try {
            r.run();
            check(false, context + " should throw");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
}
