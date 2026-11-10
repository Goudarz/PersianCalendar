// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Solar Hijri (Persian, Jalali, Shamsi) calendar.
 *
 * <p>Algorithm: 33-year arithmetic rule + 78-year correction table,
 * the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
 * for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
 *
 * <p>Accepted input: years {@link #MIN_YEAR}..{@link #MAX_YEAR} and day numbers {@link #MIN_JDN}..{@link #MAX_JDN};
 * anything else throws {@link IllegalArgumentException}. All methods are thread-safe and allocation-free
 * except for the returned records.
 *
 * <p>Requires Java 16+ (records).
 */
public final class PersianCalendar {
    private PersianCalendar() {}

    public record PersianDate(int year, int month, int day) {}

    /** Years checked against the astronomical calendar. */
    public static final int VERIFIED_MIN_YEAR = 1178;
    public static final int VERIFIED_MAX_YEAR = 3000;

    /** Accepted input range. Within it every intermediate value fits in 32 bits. */
    public static final int MIN_YEAR = -100_000;
    public static final int MAX_YEAR = 100_000;
    /** 1 Farvardin -100000 (Gregorian -99379-03-29). */
    public static final long MIN_JDN = -34_576_287;
    /** 29 Esfand 100000 (Gregorian 100622-03-13). */
    public static final long MAX_JDN = 38_472_562;

    private static final long EPOCH_JDN = 1948320;
    /** JDN of 1970-01-01, to convert from/to {@link LocalDate#toEpochDay()}. */
    private static final long UNIX_EPOCH_JDN = 2440588;

    /** Sorted. Years the 33-year rule calls leap but are not; the next year is leap instead. */
    private static final int[] NON_LEAP_CORRECTION = {
        1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
        1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
        2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
        2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
        2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
        2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
        2946, 2950, 2954, 2979, 2983, 2987,
    };

    // Immutable lists: unlike public arrays, callers cannot change them.
    public static final List<String> MONTH_NAMES_FA = List.of("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد",
        "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند");
    public static final List<String> MONTH_NAMES_EN = List.of("Farvardin", "Ordibehesht", "Khordad", "Tir",
        "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand");
    public static final List<String> WEEKDAY_NAMES_FA = List.of("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه",
        "چهارشنبه", "پنجشنبه", "جمعه");
    public static final List<String> WEEKDAY_NAMES_EN = List.of("Saturday", "Sunday", "Monday", "Tuesday",
        "Wednesday", "Thursday", "Friday");

    private static boolean isCorrection(long year) {
        return year >= NON_LEAP_CORRECTION[0] && year <= NON_LEAP_CORRECTION[NON_LEAP_CORRECTION.length - 1]
            && Arrays.binarySearch(NON_LEAP_CORRECTION, (int) year) >= 0;
    }

    private static void checkYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("year out of range [" + MIN_YEAR + ", " + MAX_YEAR + "]: " + year);
        }
    }

    private static void checkJdn(long jdn) {
        if (jdn < MIN_JDN || jdn > MAX_JDN) {
            throw new IllegalArgumentException("jdn out of range [" + MIN_JDN + ", " + MAX_JDN + "]: " + jdn);
        }
    }

    /** True if the Persian year has 366 days (Esfand has 30 days). */
    public static boolean isLeapYear(int year) {
        checkYear(year);
        if (isCorrection(year)) return false;
        if (isCorrection(year - 1L)) return true;
        return Math.floorMod(25L * year + 11, 33) < 8;
    }

    /** 29, 30 or 31. */
    public static int daysInMonth(int year, int month) {
        checkYear(year);
        if (month < 1 || month > 12) throw new IllegalArgumentException("month out of range [1, 12]: " + month);
        if (month <= 6) return 31;
        if (month <= 11) return 30;
        return isLeapYear(year) ? 30 : 29;
    }

    /** True for a real Persian date in the accepted range; never throws. */
    public static boolean isValidDate(int year, int month, int day) {
        return year >= MIN_YEAR && year <= MAX_YEAR && month >= 1 && month <= 12
            && day >= 1 && day <= daysInMonth(year, month);
    }

    private static long nowruzJdn(long year) {
        long jdn = EPOCH_JDN + 365 * (year - 1) + Math.floorDiv(8 * year + 21, 33);
        if (isCorrection(year - 1)) jdn -= 1;
        return jdn;
    }

    private static int daysBeforeMonth(int month) {
        return month <= 7 ? 31 * (month - 1) : 30 * (month - 1) + 6;
    }

    private static void checkDate(int year, int month, int day) {
        if (!isValidDate(year, month, day)) {
            throw new IllegalArgumentException("invalid Persian date: " + year + "/" + month + "/" + day);
        }
    }

    /** Julian Day Number of a Persian date. */
    public static long toJdn(int year, int month, int day) {
        checkDate(year, month, day);
        return nowruzJdn(year) + daysBeforeMonth(month) + day - 1;
    }

    /** Persian date of a Julian Day Number in {@link #MIN_JDN}..{@link #MAX_JDN}. */
    public static PersianDate fromJdn(long jdn) {
        checkJdn(jdn);
        long year = 1 + Math.floorDiv(33 * (jdn - EPOCH_JDN) + 3, 12053);
        while (jdn >= nowruzJdn(year + 1)) year++;
        while (jdn < nowruzJdn(year)) year--;
        int doy = (int) (jdn - nowruzJdn(year)); // 0-based
        if (doy < 186) return new PersianDate((int) year, doy / 31 + 1, doy % 31 + 1);
        doy -= 186;
        return new PersianDate((int) year, doy / 30 + 7, doy % 30 + 1);
    }

    /** Persian date of a {@link LocalDate} (proleptic ISO calendar). */
    public static PersianDate fromLocalDate(LocalDate date) {
        long epochDay = date.toEpochDay(); // at most about ±3.7e11, no overflow below
        return fromJdn(epochDay + UNIX_EPOCH_JDN);
    }

    public static LocalDate toLocalDate(int year, int month, int day) {
        return LocalDate.ofEpochDay(toJdn(year, month, day) - UNIX_EPOCH_JDN);
    }

    /** 0 = Saturday (شنبه) ... 6 = Friday (جمعه). */
    public static int dayOfWeek(int year, int month, int day) {
        return (int) Math.floorMod(toJdn(year, month, day) + 2, 7L);
    }

    /** 1 ... 365/366. */
    public static int dayOfYear(int year, int month, int day) {
        checkDate(year, month, day);
        return daysBeforeMonth(month) + day;
    }
}
