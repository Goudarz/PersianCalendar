// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/*
 * Solar Hijri (Persian, Jalali, Shamsi) calendar (C99, also C++).
 *
 * Algorithm: 33-year arithmetic rule + 78-year correction table,
 * the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
 * for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
 *
 * No allocation, no libc dependency beyond <stdint.h>; suitable for embedded use.
 */
#ifndef PERSIAN_CALENDAR_H
#define PERSIAN_CALENDAR_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Years checked against the astronomical calendar. */
#define PERSIAN_VERIFIED_MIN_YEAR 1178
#define PERSIAN_VERIFIED_MAX_YEAR 3000

/* Accepted input range. Within it every intermediate value fits in 32 bits.
 * Functions return 0 (or -1 / false) for anything outside it; no input causes
 * undefined behavior, overflow or unbounded loops. */
#define PERSIAN_MIN_YEAR (-100000)
#define PERSIAN_MAX_YEAR 100000
#define PERSIAN_MIN_JDN (-34576287LL) /* 1 Farvardin -100000 = Gregorian -99379-03-29 */
#define PERSIAN_MAX_JDN 38472562LL    /* 29 Esfand 100000 = Gregorian 100622-03-13 */

typedef struct {
    int32_t year;
    int32_t month; /* 1 = Farvardin ... 12 = Esfand */
    int32_t day;
} persian_date;

extern const char *const PERSIAN_MONTH_NAMES_FA[12];   /* UTF-8 */
extern const char *const PERSIAN_MONTH_NAMES_EN[12];
extern const char *const PERSIAN_WEEKDAY_NAMES_FA[7];  /* UTF-8, index 0 = Saturday */
extern const char *const PERSIAN_WEEKDAY_NAMES_EN[7];

/* 1 if the Persian year has 366 days. Defined for every int32_t (arithmetic extrapolation
 * outside PERSIAN_MIN_YEAR..PERSIAN_MAX_YEAR). */
int persian_is_leap_year(int32_t year);
/* 29, 30 or 31; 0 if month is not 1..12. */
int persian_days_in_month(int32_t year, int32_t month);
/* 1 for a real Persian date in PERSIAN_MIN_YEAR..PERSIAN_MAX_YEAR, else 0. */
int persian_is_valid(int32_t year, int32_t month, int32_t day);

/* The functions below return 1 on success and 0 for invalid or out-of-range input
 * (or a NULL output pointer); on failure the outputs are left unchanged. */

/* Persian date -> Julian Day Number. */
int persian_to_jdn(int32_t year, int32_t month, int32_t day, int64_t *jdn);
/* Julian Day Number (PERSIAN_MIN_JDN..PERSIAN_MAX_JDN) -> Persian date. */
int persian_from_jdn(int64_t jdn, persian_date *out);

/* Proleptic Gregorian calendar (month 1..12). Invalid dates (e.g. 2025-02-30) are rejected. */
int gregorian_to_jdn(int32_t year, int32_t month, int32_t day, int64_t *jdn);
int jdn_to_gregorian(int64_t jdn, int32_t *year, int32_t *month, int32_t *day);

/* 0 = Saturday ... 6 = Friday, for any int64_t. */
int persian_day_of_week_jdn(int64_t jdn);
/* 0 = Saturday ... 6 = Friday; -1 if the date is invalid. */
int persian_day_of_week(int32_t year, int32_t month, int32_t day);
/* 1 ... 365/366; 0 if the date is invalid. */
int persian_day_of_year(int32_t year, int32_t month, int32_t day);

#ifdef __cplusplus
}
#endif

#endif /* PERSIAN_CALENDAR_H */
