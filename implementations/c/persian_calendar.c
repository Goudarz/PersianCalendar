// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/* Solar Hijri (Persian) calendar. See persian_calendar.h. */
#include <stddef.h>
#include "persian_calendar.h"

#define EPOCH_JDN 1948320

/* Sorted. Years the 33-year rule calls leap but are not; the next year is leap instead. */
static const int16_t NON_LEAP_CORRECTION[78] = {
    1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
    1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
    2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
    2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
    2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
    2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
    2946, 2950, 2954, 2979, 2983, 2987,
};

const char *const PERSIAN_MONTH_NAMES_FA[12] = {
    "\xD9\x81\xD8\xB1\xD9\x88\xD8\xB1\xD8\xAF\xDB\x8C\xD9\x86",                 /* فروردین */
    "\xD8\xA7\xD8\xB1\xD8\xAF\xDB\x8C\xD8\xA8\xD9\x87\xD8\xB4\xD8\xAA",         /* اردیبهشت */
    "\xD8\xAE\xD8\xB1\xD8\xAF\xD8\xA7\xD8\xAF",                                 /* خرداد */
    "\xD8\xAA\xDB\x8C\xD8\xB1",                                                 /* تیر */
    "\xD9\x85\xD8\xB1\xD8\xAF\xD8\xA7\xD8\xAF",                                 /* مرداد */
    "\xD8\xB4\xD9\x87\xD8\xB1\xDB\x8C\xD9\x88\xD8\xB1",                         /* شهریور */
    "\xD9\x85\xD9\x87\xD8\xB1",                                                 /* مهر */
    "\xD8\xA2\xD8\xA8\xD8\xA7\xD9\x86",                                         /* آبان */
    "\xD8\xA2\xD8\xB0\xD8\xB1",                                                 /* آذر */
    "\xD8\xAF\xDB\x8C",                                                         /* دی */
    "\xD8\xA8\xD9\x87\xD9\x85\xD9\x86",                                         /* بهمن */
    "\xD8\xA7\xD8\xB3\xD9\x81\xD9\x86\xD8\xAF",                                 /* اسفند */
};
const char *const PERSIAN_MONTH_NAMES_EN[12] = {
    "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
    "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand",
};
const char *const PERSIAN_WEEKDAY_NAMES_FA[7] = {
    "\xD8\xB4\xD9\x86\xD8\xA8\xD9\x87",                                         /* شنبه */
    "\xDB\x8C\xDA\xA9\xD8\xB4\xD9\x86\xD8\xA8\xD9\x87",                         /* یکشنبه */
    "\xD8\xAF\xD9\x88\xD8\xB4\xD9\x86\xD8\xA8\xD9\x87",                         /* دوشنبه */
    "\xD8\xB3\xD9\x87\xE2\x80\x8C\xD8\xB4\xD9\x86\xD8\xA8\xD9\x87",             /* سه‌شنبه (with ZWNJ) */
    "\xDA\x86\xD9\x87\xD8\xA7\xD8\xB1\xD8\xB4\xD9\x86\xD8\xA8\xD9\x87",         /* چهارشنبه */
    "\xD9\xBE\xD9\x86\xD8\xAC\xD8\xB4\xD9\x86\xD8\xA8\xD9\x87",                 /* پنجشنبه */
    "\xD8\xAC\xD9\x85\xD8\xB9\xD9\x87",                                         /* جمعه */
};
const char *const PERSIAN_WEEKDAY_NAMES_EN[7] = {
    "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
};

/* C's / and % truncate toward zero; the formulas need floor semantics. */
static int64_t fdiv(int64_t a, int64_t b) {
    int64_t q = a / b;
    return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q;
}

/* Floor modulo for b > 0; defined for every a, including INT64_MIN (no multiplication). */
static int64_t fmod64(int64_t a, int64_t b) {
    int64_t r = a % b;
    return r < 0 ? r + b : r;
}

static int is_correction(int64_t year) {
    int lo = 0, hi = 77;
    if (year < NON_LEAP_CORRECTION[0] || year > NON_LEAP_CORRECTION[77]) return 0;
    while (lo <= hi) {
        int mid = (lo + hi) / 2;
        if (NON_LEAP_CORRECTION[mid] == year) return 1;
        if (NON_LEAP_CORRECTION[mid] < year) lo = mid + 1; else hi = mid - 1;
    }
    return 0;
}

int persian_is_leap_year(int32_t year) {
    if (is_correction(year)) return 0;
    if (is_correction((int64_t)year - 1)) return 1;
    return fmod64(25 * (int64_t)year + 11, 33) < 8;
}

int persian_days_in_month(int32_t year, int32_t month) {
    if (month < 1 || month > 12) return 0;
    if (month <= 6) return 31;
    if (month <= 11) return 30;
    return persian_is_leap_year(year) ? 30 : 29;
}

int persian_is_valid(int32_t year, int32_t month, int32_t day) {
    return year >= PERSIAN_MIN_YEAR && year <= PERSIAN_MAX_YEAR && month >= 1 && month <= 12 && day >= 1 && day <= persian_days_in_month(year, month);
}

static int64_t nowruz_jdn(int64_t year) {
    int64_t jdn = EPOCH_JDN + 365 * (year - 1) + fdiv(8 * year + 21, 33);
    if (is_correction(year - 1)) jdn -= 1;
    return jdn;
}

static int64_t days_before_month(int32_t month) {
    return month <= 7 ? 31 * (month - 1) : 30 * (month - 1) + 6;
}

int persian_to_jdn(int32_t year, int32_t month, int32_t day, int64_t *jdn) {
    if (jdn == NULL || !persian_is_valid(year, month, day)) return 0;
    *jdn = nowruz_jdn(year) + days_before_month(month) + day - 1;
    return 1;
}

int persian_from_jdn(int64_t jdn, persian_date *out) {
    persian_date r;
    int64_t year, doy;
    if (out == NULL || jdn < PERSIAN_MIN_JDN || jdn > PERSIAN_MAX_JDN) return 0;
    year = 1 + fdiv(33 * (jdn - EPOCH_JDN) + 3, 12053);
    while (jdn >= nowruz_jdn(year + 1)) year++;
    while (jdn < nowruz_jdn(year)) year--;
    doy = jdn - nowruz_jdn(year); /* 0-based */
    r.year = (int32_t)year;
    if (doy < 186) {
        r.month = (int32_t)(doy / 31 + 1);
        r.day = (int32_t)(doy % 31 + 1);
    } else {
        doy -= 186;
        r.month = (int32_t)(doy / 30 + 7);
        r.day = (int32_t)(doy % 30 + 1);
    }
    *out = r;
    return 1;
}

static int gregorian_days_in_month(int32_t year, int32_t month) {
    static const int8_t days[12] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    int leap = fmod64(year, 4) == 0 && (fmod64(year, 100) != 0 || fmod64(year, 400) == 0);
    return month == 2 ? 28 + leap : days[month - 1];
}

int gregorian_to_jdn(int32_t year, int32_t month, int32_t day, int64_t *jdn) {
    int64_t a, y, m, result;
    if (jdn == NULL || year < -99379 || year > 100622 || month < 1 || month > 12 ||
        day < 1 || day > gregorian_days_in_month(year, month)) {
        return 0;
    }
    a = fdiv(14 - month, 12);
    y = (int64_t)year + 4800 - a;
    m = month + 12 * a - 3;
    result = day + fdiv(153 * m + 2, 5) + 365 * y + fdiv(y, 4) - fdiv(y, 100) + fdiv(y, 400) - 32045;
    if (result < PERSIAN_MIN_JDN || result > PERSIAN_MAX_JDN) return 0;
    *jdn = result;
    return 1;
}

int jdn_to_gregorian(int64_t jdn, int32_t *year, int32_t *month, int32_t *day) {
    int64_t a, b, c, d, e, m;
    if (year == NULL || month == NULL || day == NULL || jdn < PERSIAN_MIN_JDN || jdn > PERSIAN_MAX_JDN) return 0;
    a = jdn + 32044;
    b = fdiv(4 * a + 3, 146097);
    c = a - fdiv(146097 * b, 4);
    d = fdiv(4 * c + 3, 1461);
    e = c - fdiv(1461 * d, 4);
    m = fdiv(5 * e + 2, 153);
    *year = (int32_t)(100 * b + d - 4800 + fdiv(m, 10));
    *month = (int32_t)(m + 3 - 12 * fdiv(m, 10));
    *day = (int32_t)(e - fdiv(153 * m + 2, 5) + 1);
    return 1;
}

/* fmod64(jdn, 7) first, so jdn + 2 cannot overflow even for INT64_MAX. */
int persian_day_of_week_jdn(int64_t jdn) { return (int)((fmod64(jdn, 7) + 2) % 7); }

int persian_day_of_week(int32_t year, int32_t month, int32_t day) {
    int64_t jdn;
    return persian_to_jdn(year, month, day, &jdn) ? persian_day_of_week_jdn(jdn) : -1;
}

int persian_day_of_year(int32_t year, int32_t month, int32_t day) {
    return persian_is_valid(year, month, day) ? (int)(days_before_month(month) + day) : 0;
}
