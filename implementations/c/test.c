// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/* Checks persian_calendar.c against /data.
 * Build & run: cc -std=c99 -Wall -Wextra -O2 persian_calendar.c test.c -o test && ./test ../../data */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "persian_calendar.h"

static long checks = 0;
#define CHECK(ok, ...) do { checks++; if (!(ok)) { fprintf(stderr, "failed: " __VA_ARGS__); fputc('\n', stderr); exit(1); } } while (0)

/* Wrappers for inputs that are known to be valid: they also check that the call succeeds. */
static persian_date pfj(int64_t jdn) {
    persian_date p;
    CHECK(persian_from_jdn(jdn, &p), "persian_from_jdn(%lld) failed", (long long)jdn);
    return p;
}
static int64_t g2j(int32_t y, int32_t m, int32_t d) {
    int64_t jdn = 0;
    CHECK(gregorian_to_jdn(y, m, d, &jdn), "gregorian_to_jdn(%d-%d-%d) failed", y, m, d);
    return jdn;
}
static void j2g(int64_t jdn, int32_t *y, int32_t *m, int32_t *d) {
    CHECK(jdn_to_gregorian(jdn, y, m, d), "jdn_to_gregorian(%lld) failed", (long long)jdn);
}

static FILE *open_csv(const char *dir, const char *name) {
    char path[1024], header[256];
    FILE *f;
    snprintf(path, sizeof path, "%s/%s", dir, name);
    f = fopen(path, "r");
    if (!f) { fprintf(stderr, "cannot open %s\n", path); exit(1); }
    if (!fgets(header, sizeof header, f)) exit(1);
    return f;
}

int main(int argc, char **argv) {
    const char *dir = argc > 1 ? argv[1] : "../../data";
    FILE *f;
    int y, leap, days, gy, gm, gd, wd, jy, jm, jd, n = 0;
    int32_t ry, rm, rd;
    long long jdn_in;
    int64_t jdn, first, last;
    persian_date p, prev;

    f = open_csv(dir, "years.csv");
    while (fscanf(f, "%d,%d,%d,%d-%d-%d,%lld,%d", &y, &leap, &days, &gy, &gm, &gd, &jdn_in, &wd) == 8) {
        CHECK(persian_is_leap_year(y) == leap, "leap %d", y);
        CHECK(persian_days_in_month(y, 12) == (leap ? 30 : 29), "esfand %d", y);
        CHECK(persian_to_jdn(y, 1, 1, &jdn) && jdn == jdn_in, "jdn %d", y);
        j2g(jdn, &ry, &rm, &rd);
        CHECK(ry == gy && rm == gm && rd == gd, "nowruz %d", y);
        CHECK(persian_day_of_week_jdn(jdn) == wd, "weekday %d", y);
        { /* every day of the year against the ground truth, both directions */
            int doy;
            for (doy = 0; doy < days; doy++) {
                int32_t m = doy < 186 ? doy / 31 + 1 : (doy - 186) / 30 + 7;
                int32_t d = doy < 186 ? doy % 31 + 1 : (doy - 186) % 30 + 1;
                int64_t back;
                persian_date p = pfj(jdn_in + doy);
                CHECK(p.year == y && p.month == m && p.day == d, "from_jdn %d/%d/%d", y, m, d);
                CHECK(persian_to_jdn(y, m, d, &back) && back == jdn_in + doy, "to_jdn %d/%d/%d", y, m, d);
            }
        }
        n++;
    }
    fclose(f);
    CHECK(n == 1823, "years.csv rows: %d", n);

    n = 0;
    f = open_csv(dir, "test-vectors.csv");
    while (fscanf(f, "%d,%d,%d,%d,%d,%d,%lld,%d", &jy, &jm, &jd, &gy, &gm, &gd, &jdn_in, &wd) == 8) {
        CHECK(persian_to_jdn(jy, jm, jd, &jdn) && jdn == jdn_in, "to_jdn %d/%d/%d", jy, jm, jd);
        CHECK(g2j(gy, gm, gd) == jdn_in, "gregorian_to_jdn %d-%d-%d", gy, gm, gd);
        j2g(jdn, &ry, &rm, &rd);
        CHECK(ry == gy && rm == gm && rd == gd, "to gregorian %d/%d/%d", jy, jm, jd);
        p = pfj(jdn_in);
        CHECK(p.year == jy && p.month == jm && p.day == jd, "from_jdn %lld", jdn_in);
        CHECK(persian_day_of_week_jdn(jdn) == wd, "weekday %d/%d/%d", jy, jm, jd);
        n++;
    }
    fclose(f);
    CHECK(n > 8000, "test-vectors.csv rows: %d", n);

    persian_to_jdn(PERSIAN_VERIFIED_MIN_YEAR, 1, 1, &first);
    persian_to_jdn(PERSIAN_VERIFIED_MAX_YEAR, 12, persian_days_in_month(PERSIAN_VERIFIED_MAX_YEAR, 12), &last);
    prev = pfj(first - 1);
    for (jdn = first; jdn <= last; jdn++) {
        int64_t back;
        int consecutive;
        p = pfj(jdn);
        CHECK(persian_to_jdn(p.year, p.month, p.day, &back) && back == jdn, "round trip %lld", (long long)jdn);
        consecutive = (p.day == prev.day + 1 && p.month == prev.month && p.year == prev.year) ||
                      (p.day == 1 && ((p.month == prev.month + 1 && p.year == prev.year) ||
                                      (p.month == 1 && prev.month == 12 && p.year == prev.year + 1)));
        CHECK(consecutive, "consecutive %lld", (long long)jdn);
        prev = p;
    }

    /* Extrapolated years: years <= 0 make intermediate values negative (floor division matters). */
    CHECK(persian_to_jdn(-1000, 1, 1, &jdn) && jdn == 1582713, "-1000");
    CHECK(persian_to_jdn(0, 1, 1, &jdn) && jdn == 1947955, "0");
    CHECK(persian_to_jdn(4000, 1, 1, &jdn) && jdn == 3408925, "4000");
    p = pfj(0);
    CHECK(p.year == -5334 && p.month == 9 && p.day == 3, "jdn 0");
    CHECK(persian_day_of_week_jdn(1582713) == 1, "weekday -1000");
    CHECK(persian_is_leap_year(-3) && !persian_is_leap_year(-1000), "negative leap years");
    {
        static const int32_t ranges[2][2] = {{-1000, 1177}, {3001, 4000}};
        int r;
        for (r = 0; r < 2; r++) {
            int64_t a, b, back;
            persian_to_jdn(ranges[r][0], 1, 1, &a);
            persian_to_jdn(ranges[r][1], 12, persian_days_in_month(ranges[r][1], 12), &b);
            prev = pfj(a - 1);
            for (jdn = a; jdn <= b; jdn++) {
                int consecutive;
                p = pfj(jdn);
                CHECK(persian_to_jdn(p.year, p.month, p.day, &back) && back == jdn, "round trip %lld", (long long)jdn);
                j2g(jdn, &ry, &rm, &rd);
                CHECK(g2j(ry, rm, rd) == jdn, "gregorian %lld", (long long)jdn);
                consecutive = (p.day == prev.day + 1 && p.month == prev.month && p.year == prev.year) ||
                              (p.day == 1 && ((p.month == prev.month + 1 && p.year == prev.year) ||
                                              (p.month == 1 && prev.month == 12 && p.year == prev.year + 1)));
                CHECK(consecutive, "consecutive %lld", (long long)jdn);
                prev = p;
            }
        }
    }
    CHECK(persian_is_valid(1403, 12, 30), "1403/12/30");
    CHECK(!persian_is_valid(1404, 12, 30), "1404/12/30");
    CHECK(persian_is_valid(1404, 6, 31), "1404/6/31");
    CHECK(!persian_is_valid(1404, 7, 31), "1404/7/31");
    CHECK(!persian_is_valid(1404, 13, 1), "1404/13/1");
    CHECK(!persian_is_valid(1404, 0, 1), "1404/0/1");
    CHECK(!persian_is_valid(1404, 1, 0), "1404/1/0");
    CHECK(!persian_to_jdn(1404, 12, 30, &jdn), "invalid date must fail");
    p = pfj(g2j(2025, 3, 21));
    CHECK(p.year == 1404 && p.month == 1 && p.day == 1, "2025-03-21");
    p = pfj(g2j(1979, 2, 11));
    CHECK(p.year == 1357 && p.month == 11 && p.day == 22, "1979-02-11");
    CHECK(persian_day_of_week(1404, 1, 1) == 6 && persian_day_of_week(1404, 12, 30) == -1, "day_of_week");
    CHECK(persian_day_of_year(1404, 7, 1) == 187 && persian_day_of_year(1403, 12, 30) == 366 && persian_day_of_year(1404, 13, 1) == 0, "day_of_year");
    persian_to_jdn(1404, 1, 1, &jdn);
    CHECK(strcmp(PERSIAN_WEEKDAY_NAMES_EN[persian_day_of_week_jdn(jdn)], "Friday") == 0, "friday");
    CHECK(strcmp(PERSIAN_MONTH_NAMES_FA[0], "فروردین") == 0, "utf-8 month name");
    /* Limits: exact boundaries; everything outside is rejected (no UB, no hang). */
    CHECK(persian_to_jdn(PERSIAN_MIN_YEAR, 1, 1, &jdn) && jdn == PERSIAN_MIN_JDN, "MIN_JDN");
    CHECK(persian_to_jdn(PERSIAN_MAX_YEAR, 12, 29, &jdn) && jdn == PERSIAN_MAX_JDN, "MAX_JDN");
    p = pfj(PERSIAN_MIN_JDN);
    CHECK(p.year == PERSIAN_MIN_YEAR && p.month == 1 && p.day == 1, "from MIN_JDN");
    p = pfj(PERSIAN_MAX_JDN);
    CHECK(p.year == PERSIAN_MAX_YEAR && p.month == 12 && p.day == 29, "from MAX_JDN");
    j2g(PERSIAN_MIN_JDN, &ry, &rm, &rd);
    CHECK(ry == -99379 && rm == 3 && rd == 29, "gregorian MIN_JDN");
    j2g(PERSIAN_MAX_JDN, &ry, &rm, &rd);
    CHECK(ry == 100622 && rm == 3 && rd == 13, "gregorian MAX_JDN");
    {
        static const int64_t bad_jdn[] = {PERSIAN_MIN_JDN - 1, PERSIAN_MAX_JDN + 1, INT64_MAX, INT64_MIN, (int64_t)1 << 53};
        static const int32_t bad_year[] = {PERSIAN_MIN_YEAR - 1, PERSIAN_MAX_YEAR + 1, INT32_MAX, INT32_MIN};
        static const int32_t bad_greg[][3] = {{2025, 2, 29}, {2025, 2, 30}, {2025, 13, 1}, {2025, 0, 1},
                                              {2025, 4, 31}, {2025, 1, 0}, {100622, 3, 14}, {INT32_MAX, 1, 1}};
        size_t i;
        persian_date untouched = {7, 7, 7};
        for (i = 0; i < sizeof bad_jdn / sizeof bad_jdn[0]; i++) {
            CHECK(!persian_from_jdn(bad_jdn[i], &untouched), "from_jdn %lld", (long long)bad_jdn[i]);
            CHECK(!jdn_to_gregorian(bad_jdn[i], &ry, &rm, &rd), "jdn_to_gregorian %lld", (long long)bad_jdn[i]);
            CHECK(persian_day_of_week_jdn(bad_jdn[i]) >= 0 && persian_day_of_week_jdn(bad_jdn[i]) <= 6, "weekday");
        }
        CHECK(untouched.year == 7 && untouched.month == 7 && untouched.day == 7, "output untouched on failure");
        for (i = 0; i < sizeof bad_year / sizeof bad_year[0]; i++) {
            CHECK(!persian_is_valid(bad_year[i], 1, 1), "is_valid %d", bad_year[i]);
            CHECK(!persian_to_jdn(bad_year[i], 1, 1, &jdn), "to_jdn %d", bad_year[i]);
            (void)persian_is_leap_year(bad_year[i]); /* defined for every int32_t */
        }
        for (i = 0; i < sizeof bad_greg / sizeof bad_greg[0]; i++) {
            CHECK(!gregorian_to_jdn(bad_greg[i][0], bad_greg[i][1], bad_greg[i][2], &jdn), "gregorian %d", (int)i);
        }
        CHECK(!persian_from_jdn(2460756, NULL) && !persian_to_jdn(1404, 1, 1, NULL) &&
              !gregorian_to_jdn(2025, 3, 21, NULL) && !jdn_to_gregorian(2460756, NULL, &rm, &rd), "NULL outputs");
        CHECK(g2j(2024, 2, 29) == 2460370, "Gregorian leap day");
    }
    /* Every year MIN..MAX: years are contiguous, first and last day round-trip, and the leap years
     * sit where every other implementation (and ICU 77.1+) puts them (checksums). */
    {
        int64_t expected = PERSIAN_MIN_JDN, leaps = 0, nowruz_sum = 0;
        int32_t y;
        for (y = PERSIAN_MIN_YEAR; y <= PERSIAN_MAX_YEAR; y++) {
            int32_t leap = persian_is_leap_year(y) ? 1 : 0;
            persian_date first, last;
            CHECK(persian_to_jdn(y, 1, 1, &jdn) && jdn == expected, "nowruz %d", (int)y);
            first = pfj(jdn);
            last = pfj(jdn + 364 + leap);
            CHECK(first.year == y && first.month == 1 && first.day == 1 && persian_days_in_month(y, 12) == 29 + leap &&
                      last.year == y && last.month == 12 && last.day == 29 + leap,
                  "year %d", (int)y);
            leaps += leap;
            nowruz_sum += jdn;
            expected = jdn + 365 + leap;
        }
        CHECK(expected - 1 == PERSIAN_MAX_JDN && leaps == 48485 && nowruz_sum == INT64_C(389592978180), "full-range checksums");
    }
    printf("OK: %ld checks passed\n", checks);
    return 0;
}
