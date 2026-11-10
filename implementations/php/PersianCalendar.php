<?php
// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/**
 * Solar Hijri (Persian, Jalali, Shamsi) calendar.
 *
 * Algorithm: 33-year arithmetic rule + 78-year correction table,
 * the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
 * for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
 *
 * Inputs outside MIN_YEAR..MAX_YEAR / MIN_JDN..MAX_JDN and invalid dates throw
 * InvalidArgumentException. Within that range every intermediate value fits in
 * 32 bits, so 32-bit PHP builds work too. Requires PHP 8.1+.
 */

declare(strict_types=1);

namespace PersianCalendar;

final class PersianCalendar
{
    /** Years checked against the astronomical calendar. */
    public const VERIFIED_MIN_YEAR = 1178;
    public const VERIFIED_MAX_YEAR = 3000;

    /** Accepted input range. */
    public const MIN_YEAR = -100000;
    public const MAX_YEAR = 100000;
    /** 1 Farvardin -100000 (Gregorian -99379-03-29). */
    public const MIN_JDN = -34576287;
    /** 29 Esfand 100000 (Gregorian 100622-03-13). */
    public const MAX_JDN = 38472562;

    private const EPOCH_JDN = 1948320;

    /** Years the 33-year rule calls leap but are not; the next year is leap instead (keys for O(1) lookup). */
    private const NON_LEAP_CORRECTION = [
        1502 => 1, 1601 => 1, 1634 => 1, 1667 => 1, 1700 => 1, 1733 => 1, 1766 => 1, 1799 => 1,
        1832 => 1, 1865 => 1, 1898 => 1, 1931 => 1, 1964 => 1, 1997 => 1, 2030 => 1, 2059 => 1,
        2063 => 1, 2096 => 1, 2129 => 1, 2158 => 1, 2162 => 1, 2191 => 1, 2195 => 1, 2224 => 1,
        2228 => 1, 2257 => 1, 2261 => 1, 2290 => 1, 2294 => 1, 2323 => 1, 2327 => 1, 2356 => 1,
        2360 => 1, 2389 => 1, 2393 => 1, 2422 => 1, 2426 => 1, 2455 => 1, 2459 => 1, 2488 => 1,
        2492 => 1, 2521 => 1, 2525 => 1, 2554 => 1, 2558 => 1, 2587 => 1, 2591 => 1, 2620 => 1,
        2624 => 1, 2653 => 1, 2657 => 1, 2686 => 1, 2690 => 1, 2719 => 1, 2723 => 1, 2748 => 1,
        2752 => 1, 2756 => 1, 2781 => 1, 2785 => 1, 2789 => 1, 2818 => 1, 2822 => 1, 2847 => 1,
        2851 => 1, 2855 => 1, 2880 => 1, 2884 => 1, 2888 => 1, 2913 => 1, 2917 => 1, 2921 => 1,
        2946 => 1, 2950 => 1, 2954 => 1, 2979 => 1, 2983 => 1, 2987 => 1,
    ];

    public const MONTH_NAMES_FA = ['فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور',
        'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'];
    public const MONTH_NAMES_EN = ['Farvardin', 'Ordibehesht', 'Khordad', 'Tir', 'Mordad', 'Shahrivar',
        'Mehr', 'Aban', 'Azar', 'Dey', 'Bahman', 'Esfand'];
    public const WEEKDAY_NAMES_FA = ['شنبه', 'یکشنبه', 'دوشنبه', "سه\u{200C}شنبه", 'چهارشنبه', 'پنجشنبه', 'جمعه'];
    public const WEEKDAY_NAMES_EN = ['Saturday', 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];

    /** intdiv() and % truncate toward zero; the formulas need floor semantics. */
    private static function fdiv(int $a, int $b): int
    {
        $q = intdiv($a, $b);
        return ($a % $b !== 0 && (($a < 0) !== ($b < 0))) ? $q - 1 : $q;
    }

    /** Floor modulo for $b > 0. */
    private static function fmod(int $a, int $b): int
    {
        $r = $a % $b;
        return $r < 0 ? $r + $b : $r;
    }

    private static function checkYear(int $year): void
    {
        if ($year < self::MIN_YEAR || $year > self::MAX_YEAR) {
            throw new \InvalidArgumentException(
                sprintf('year out of range [%d, %d]: %d', self::MIN_YEAR, self::MAX_YEAR, $year)
            );
        }
    }

    private static function checkJdn(int $jdn): void
    {
        if ($jdn < self::MIN_JDN || $jdn > self::MAX_JDN) {
            throw new \InvalidArgumentException(
                sprintf('jdn out of range [%d, %d]: %d', self::MIN_JDN, self::MAX_JDN, $jdn)
            );
        }
    }

    public static function isLeapYear(int $year): bool
    {
        self::checkYear($year);
        if (isset(self::NON_LEAP_CORRECTION[$year])) {
            return false;
        }
        if (isset(self::NON_LEAP_CORRECTION[$year - 1])) {
            return true;
        }
        return self::fmod(25 * $year + 11, 33) < 8;
    }

    public static function daysInMonth(int $year, int $month): int
    {
        self::checkYear($year);
        if ($month < 1 || $month > 12) {
            throw new \InvalidArgumentException("month out of range: $month");
        }
        if ($month <= 6) {
            return 31;
        }
        if ($month <= 11) {
            return 30;
        }
        return self::isLeapYear($year) ? 30 : 29;
    }

    /** True for a real Persian date in the accepted range; never throws. */
    public static function isValidDate(int $year, int $month, int $day): bool
    {
        return $year >= self::MIN_YEAR && $year <= self::MAX_YEAR
            && $month >= 1 && $month <= 12 && $day >= 1 && $day <= self::daysInMonth($year, $month);
    }

    private static function nowruzJdn(int $year): int
    {
        $jdn = self::EPOCH_JDN + 365 * ($year - 1) + self::fdiv(8 * $year + 21, 33);
        if (isset(self::NON_LEAP_CORRECTION[$year - 1])) {
            $jdn--;
        }
        return $jdn;
    }

    private static function daysBeforeMonth(int $month): int
    {
        return $month <= 7 ? 31 * ($month - 1) : 30 * ($month - 1) + 6;
    }

    /** Julian Day Number of a Persian date. */
    public static function toJdn(int $year, int $month, int $day): int
    {
        if (!self::isValidDate($year, $month, $day)) {
            throw new \InvalidArgumentException("invalid Persian date: $year/$month/$day");
        }
        return self::nowruzJdn($year) + self::daysBeforeMonth($month) + $day - 1;
    }

    /** @return array{int, int, int} [year, month, day] */
    public static function fromJdn(int $jdn): array
    {
        self::checkJdn($jdn);
        $year = 1 + self::fdiv(33 * ($jdn - self::EPOCH_JDN) + 3, 12053);
        while ($jdn >= self::nowruzJdn($year + 1)) {
            $year++;
        }
        while ($jdn < self::nowruzJdn($year)) {
            $year--;
        }
        $doy = $jdn - self::nowruzJdn($year); // 0-based
        if ($doy < 186) {
            return [$year, intdiv($doy, 31) + 1, $doy % 31 + 1];
        }
        $doy -= 186;
        return [$year, intdiv($doy, 30) + 7, $doy % 30 + 1];
    }

    private static function gregorianDaysInMonth(int $year, int $month): int
    {
        if ($month === 2) {
            return ($year % 4 === 0 && ($year % 100 !== 0 || $year % 400 === 0)) ? 29 : 28;
        }
        return in_array($month, [4, 6, 9, 11], true) ? 30 : 31;
    }

    /** JDN of a proleptic Gregorian date (astronomical year numbering: 0 = 1 BC). */
    public static function gregorianToJdn(int $year, int $month, int $day): int
    {
        // Year bounds first, so the arithmetic below cannot overflow.
        if ($year < -99379 || $year > 100622 || $month < 1 || $month > 12
            || $day < 1 || $day > self::gregorianDaysInMonth($year, $month)) {
            throw new \InvalidArgumentException("invalid Gregorian date: $year-$month-$day");
        }
        $a = self::fdiv(14 - $month, 12);
        $y = $year + 4800 - $a;
        $m = $month + 12 * $a - 3;
        $jdn = $day + self::fdiv(153 * $m + 2, 5) + 365 * $y + self::fdiv($y, 4)
            - self::fdiv($y, 100) + self::fdiv($y, 400) - 32045;
        self::checkJdn($jdn);
        return $jdn;
    }

    /** @return array{int, int, int} proleptic Gregorian [year, month, day] */
    public static function jdnToGregorian(int $jdn): array
    {
        self::checkJdn($jdn);
        $a = $jdn + 32044;
        $b = self::fdiv(4 * $a + 3, 146097);
        $c = $a - self::fdiv(146097 * $b, 4);
        $d = self::fdiv(4 * $c + 3, 1461);
        $e = $c - self::fdiv(1461 * $d, 4);
        $m = self::fdiv(5 * $e + 2, 153);
        return [
            100 * $b + $d - 4800 + self::fdiv($m, 10),
            $m + 3 - 12 * self::fdiv($m, 10),
            $e - self::fdiv(153 * $m + 2, 5) + 1,
        ];
    }

    /** @return array{int, int, int} */
    public static function toGregorian(int $year, int $month, int $day): array
    {
        return self::jdnToGregorian(self::toJdn($year, $month, $day));
    }

    /** @return array{int, int, int} */
    public static function fromGregorian(int $year, int $month, int $day): array
    {
        return self::fromJdn(self::gregorianToJdn($year, $month, $day));
    }

    /**
     * Persian date of a DateTimeInterface, read in its own time zone.
     * Call ->setTimezone(new DateTimeZone('Asia/Tehran')) first if needed.
     * @return array{int, int, int}
     */
    public static function fromDateTime(\DateTimeInterface $dt): array
    {
        return self::fromGregorian((int) $dt->format('Y'), (int) $dt->format('n'), (int) $dt->format('j'));
    }

    /** 0 = Saturday (شنبه) ... 6 = Friday (جمعه). */
    public static function dayOfWeek(int $year, int $month, int $day): int
    {
        return self::fmod(self::toJdn($year, $month, $day) + 2, 7);
    }

    /** 1 ... 365/366. */
    public static function dayOfYear(int $year, int $month, int $day): int
    {
        if (!self::isValidDate($year, $month, $day)) {
            throw new \InvalidArgumentException("invalid Persian date: $year/$month/$day");
        }
        return self::daysBeforeMonth($month) + $day;
    }
}
