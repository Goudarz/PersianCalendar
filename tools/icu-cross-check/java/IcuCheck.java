// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares ICU4J, this repository's Java implementation and the ground truth
// (data/years.csv) for every day of 1178-3000 AP, in both directions.
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the program checks that ICU matches the plain 33-year rule exactly instead.
// Run (see ../README.md): javac -cp icu4j-X.jar -d out IcuCheck.java ../../../implementations/java/PersianCalendar.java
//                         java -cp "icu4j-X.jar:out" IcuCheck ../../../data/years.csv
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class IcuCheck {
    /** Nowruz JDN under the plain 33-year rule (what ICU 50-76 implements). */
    static long plainNowruz(long y) {
        return 1948320 + 365 * (y - 1) + Math.floorDiv(8 * y + 21, 33);
    }

    /** {year, month, day} under the plain 33-year rule. */
    static long[] plainFromJdn(long jdn) {
        long y = Math.floorDiv(jdn - 1948320, 366) + 1;
        while (jdn >= plainNowruz(y + 1)) y++;
        while (jdn < plainNowruz(y)) y--;
        long doy = jdn - plainNowruz(y);
        return doy < 186 ? new long[] {y, doy / 31 + 1, doy % 31 + 1} : new long[] {y, (doy - 186) / 30 + 7, (doy - 186) % 30 + 1};
    }

    public static void main(String[] args) throws Exception {
        List<String[]> years = Files.readAllLines(Path.of(args[0])).stream().skip(1).map(l -> l.split(",")).toList();
        Calendar icu = Calendar.getInstance(TimeZone.GMT_ZONE, new ULocale("fa_IR@calendar=persian"));
        boolean hasCorrections = VersionInfo.ICU_VERSION.getMajor() >= 77;
        long days = 0, referenceWrong = 0, icuToPersianWrong = 0, icuFromPersianWrong = 0, differsFromTruth = 0;
        for (String[] r : years) {
            int year = Integer.parseInt(r[0]);
            long nowruzJdn = Long.parseLong(r[4]);
            int length = r[1].equals("1") ? 366 : 365;
            for (int doy = 0; doy < length; doy++, days++) {
                long jdn = nowruzJdn + doy;
                int month = doy < 186 ? doy / 31 + 1 : (doy - 186) / 30 + 7;
                int day = doy < 186 ? doy % 31 + 1 : (doy - 186) % 30 + 1;
                // this repository's implementation, both directions
                var p = PersianCalendar.fromJdn(jdn);
                if (p.year() != year || p.month() != month || p.day() != day || PersianCalendar.toJdn(year, month, day) != jdn) {
                    referenceWrong++;
                }
                long epochDay = jdn - 2440588; // days since 1970-01-01
                // what ICU is expected to return: the truth (77.1+) or the plain 33-year rule (<= 76.1)
                long[] e = hasCorrections ? new long[] {year, month, day} : plainFromJdn(jdn);
                // ICU: Gregorian instant -> Persian fields
                icu.clear();
                icu.setTimeInMillis(epochDay * 86_400_000L);
                if (icu.get(Calendar.EXTENDED_YEAR) != e[0] || icu.get(Calendar.MONTH) + 1 != e[1]
                        || icu.get(Calendar.DAY_OF_MONTH) != e[2]) {
                    icuToPersianWrong++;
                }
                if (icu.get(Calendar.EXTENDED_YEAR) != year || icu.get(Calendar.MONTH) + 1 != month
                        || icu.get(Calendar.DAY_OF_MONTH) != day) {
                    differsFromTruth++;
                }
                // ICU: Persian fields -> instant. Under the plain rule, 30 Esfand of a correction year
                // does not exist and the following days shift; compare with the plain rule's own date.
                icu.clear();
                icu.set(Calendar.EXTENDED_YEAR, (int) e[0]);
                icu.set(Calendar.MONTH, (int) e[1] - 1);
                icu.set(Calendar.DAY_OF_MONTH, (int) e[2]);
                if (Math.floorDiv(icu.getTimeInMillis(), 86_400_000L) != epochDay) {
                    icuFromPersianWrong++;
                }
            }
        }
        // The whole accepted range, year by year (ICU uses the same arithmetic calendar outside 1178-3000):
        // first day, last day, and year length. ICU 76.1 and older differ exactly in the 78 correction years.
        long fullWrong = 0;
        for (int y = PersianCalendar.MIN_YEAR; y <= PersianCalendar.MAX_YEAR; y++) {
            int last = PersianCalendar.daysInMonth(y, 12);
            long nowruz = PersianCalendar.toJdn(y, 1, 1), end = PersianCalendar.toJdn(y, 12, last);
            icu.clear();
            icu.set(Calendar.EXTENDED_YEAR, y);
            icu.set(Calendar.MONTH, 0);
            icu.set(Calendar.DAY_OF_MONTH, 1);
            boolean ok = icu.get(Calendar.JULIAN_DAY) == nowruz;
            icu.clear();
            icu.set(Calendar.JULIAN_DAY, (int) end);
            ok &= icu.get(Calendar.EXTENDED_YEAR) == y && icu.get(Calendar.MONTH) == 11 && icu.get(Calendar.DAY_OF_MONTH) == last;
            if (!ok) fullWrong++;
        }
        long fullExpected = hasCorrections ? 0 : 78; // the 78 years of the correction table
        System.out.printf("ICU4J %s: years %d..%d: ICU differs in %d years (expected %d)%n", VersionInfo.ICU_VERSION,
            PersianCalendar.MIN_YEAR, PersianCalendar.MAX_YEAR, fullWrong, fullExpected);
        System.out.printf("ICU4J %s: %d days (1178-3000 AP) | implementation wrong: %d | ICU differs from ground truth on %d days | "
            + "ICU matches %s: %s%n", VersionInfo.ICU_VERSION, days, referenceWrong, differsFromTruth,
            hasCorrections ? "the ground truth" : "the plain 33-year rule",
            icuToPersianWrong + icuFromPersianWrong == 0 ? "yes (both directions)"
                : "NO (G->P " + icuToPersianWrong + ", P->G " + icuFromPersianWrong + ")");
        System.exit(referenceWrong + icuToPersianWrong + icuFromPersianWrong == 0 && fullWrong == fullExpected ? 0 : 1);
    }
}
