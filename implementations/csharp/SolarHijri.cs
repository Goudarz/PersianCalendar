// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Solar Hijri (Persian, Jalali, Shamsi) calendar.
//
// Algorithm: 33-year arithmetic rule + 78-year correction table,
// the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
// for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
//
// Named SolarHijri to avoid a clash with System.Globalization.PersianCalendar
// (which is correct only up to 1535 AP; see docs).
// Inputs outside MinYear..MaxYear / MinJdn..MaxJdn throw ArgumentOutOfRangeException;
// invalid dates throw ArgumentException. Thread-safe. Requires .NET 6+ (DateOnly).

using System;
using System.Collections.Generic;

namespace SolarHijriCalendar;

public readonly record struct PersianDate(int Year, int Month, int Day);

public static class SolarHijri
{
    /// <summary>Years checked against the astronomical calendar.</summary>
    public const int VerifiedMinYear = 1178;
    public const int VerifiedMaxYear = 3000;

    /// <summary>Accepted input range. Within it every intermediate value fits in 32 bits.</summary>
    public const int MinYear = -100_000;
    public const int MaxYear = 100_000;
    /// <summary>1 Farvardin -100000 (Gregorian -99379-03-29).</summary>
    public const long MinJdn = -34_576_287;
    /// <summary>29 Esfand 100000 (Gregorian 100622-03-13).</summary>
    public const long MaxJdn = 38_472_562;

    private const long EpochJdn = 1948320;
    // JDN of 0001-01-01 (DateOnly.DayNumber == 0).
    private const long DayNumberEpochJdn = 1721426;

    // Years the 33-year rule calls leap but are not; the next year is leap instead.
    private static readonly HashSet<long> NonLeapCorrection = new()
    {
        1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
        1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
        2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
        2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
        2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
        2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
        2946, 2950, 2954, 2979, 2983, 2987,
    };

    public static readonly IReadOnlyList<string> MonthNamesFa =
        Array.AsReadOnly(new[] { "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند" });
    public static readonly IReadOnlyList<string> MonthNamesEn =
        Array.AsReadOnly(new[] { "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand" });
    public static readonly IReadOnlyList<string> WeekdayNamesFa =
        Array.AsReadOnly(new[] { "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه" });
    public static readonly IReadOnlyList<string> WeekdayNamesEn =
        Array.AsReadOnly(new[] { "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" });

    // C# / and % truncate toward zero; the formulas need floor semantics.
    private static long FloorDiv(long a, long b) => a / b - ((a % b != 0) && ((a < 0) != (b < 0)) ? 1 : 0);
    private static long FloorMod(long a, long b) => a - b * FloorDiv(a, b);

    private static void CheckYear(int year)
    {
        if (year < MinYear || year > MaxYear)
            throw new ArgumentOutOfRangeException(nameof(year), year, $"year out of range [{MinYear}, {MaxYear}]");
    }

    private static void CheckJdn(long jdn)
    {
        if (jdn < MinJdn || jdn > MaxJdn)
            throw new ArgumentOutOfRangeException(nameof(jdn), jdn, $"jdn out of range [{MinJdn}, {MaxJdn}]");
    }

    /// <summary>True if the Persian year has 366 days (Esfand has 30 days).</summary>
    public static bool IsLeapYear(int year)
    {
        CheckYear(year);
        if (NonLeapCorrection.Contains(year)) return false;
        if (NonLeapCorrection.Contains(year - 1L)) return true;
        return FloorMod(25L * year + 11, 33) < 8;
    }

    /// <summary>29, 30 or 31.</summary>
    public static int DaysInMonth(int year, int month)
    {
        CheckYear(year);
        if (month < 1 || month > 12) throw new ArgumentOutOfRangeException(nameof(month), month, "month out of range [1, 12]");
        if (month <= 6) return 31;
        if (month <= 11) return 30;
        return IsLeapYear(year) ? 30 : 29;
    }

    /// <summary>True for a real Persian date in the accepted range; never throws.</summary>
    public static bool IsValidDate(int year, int month, int day) =>
        year >= MinYear && year <= MaxYear && month >= 1 && month <= 12 && day >= 1 && day <= DaysInMonth(year, month);

    private static long NowruzJdn(long year)
    {
        long jdn = EpochJdn + 365 * (year - 1) + FloorDiv(8 * year + 21, 33);
        if (NonLeapCorrection.Contains(year - 1)) jdn -= 1;
        return jdn;
    }

    private static int DaysBeforeMonth(int month) => month <= 7 ? 31 * (month - 1) : 30 * (month - 1) + 6;

    /// <summary>Julian Day Number of a Persian date.</summary>
    public static long ToJdn(int year, int month, int day)
    {
        if (!IsValidDate(year, month, day))
            throw new ArgumentException($"invalid Persian date: {year}/{month}/{day}");
        return NowruzJdn(year) + DaysBeforeMonth(month) + day - 1;
    }

    /// <summary>Persian date of a Julian Day Number in <see cref="MinJdn"/>..<see cref="MaxJdn"/>.</summary>
    public static PersianDate FromJdn(long jdn)
    {
        CheckJdn(jdn);
        long year = 1 + FloorDiv(33 * (jdn - EpochJdn) + 3, 12053);
        while (jdn >= NowruzJdn(year + 1)) year++;
        while (jdn < NowruzJdn(year)) year--;
        int doy = (int)(jdn - NowruzJdn(year)); // 0-based
        if (doy < 186) return new PersianDate((int)year, doy / 31 + 1, doy % 31 + 1);
        doy -= 186;
        return new PersianDate((int)year, doy / 30 + 7, doy % 30 + 1);
    }

    public static PersianDate FromDateOnly(DateOnly date) => FromJdn(date.DayNumber + DayNumberEpochJdn);

    public static DateOnly ToDateOnly(int year, int month, int day) =>
        DateOnly.FromDayNumber((int)(ToJdn(year, month, day) - DayNumberEpochJdn));

    /// <summary>0 = Saturday (شنبه) ... 6 = Friday (جمعه).</summary>
    public static int DayOfWeek(int year, int month, int day) => (int)FloorMod(ToJdn(year, month, day) + 2, 7);

    /// <summary>1 ... 365/366.</summary>
    public static int DayOfYear(int year, int month, int day)
    {
        if (!IsValidDate(year, month, day))
            throw new ArgumentException($"invalid Persian date: {year}/{month}/{day}");
        return DaysBeforeMonth(month) + day;
    }
}
