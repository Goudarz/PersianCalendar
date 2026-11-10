/*
 * Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
 * SPDX-License-Identifier: MIT
 *
 * Compares ICU4C (loaded at run time, no ICU headers needed), this repository's C implementation
 * and the ground truth (data/years.csv) for every day of 1178-3000 AP, in both directions.
 *
 * ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
 * for them the program checks that ICU matches the plain 33-year rule exactly instead.
 *
 * Build and run:
 *   cc -std=c99 -O2 check.c ../../../implementations/c/persian_calendar.c -o check   (add -ldl on older glibc)
 *   ./check <folder with libicui18n.so.NN / icuinNN.dll> <ICU major version> [path to data/years.csv]
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../../../implementations/c/persian_calendar.h"

#ifdef _WIN32
#include <windows.h>
typedef HMODULE lib_t;
static lib_t lib_open(const char *path) { return LoadLibraryExA(path, NULL, LOAD_WITH_ALTERED_SEARCH_PATH); }
static void *lib_sym(lib_t lib, const char *name) { return (void *)(size_t)GetProcAddress(lib, name); }
#define I18N_NAME "%s\\icuin%d.dll"
#define UC_NAME "%s\\icuuc%d.dll"
#else
#include <dlfcn.h>
typedef void *lib_t;
static lib_t lib_open(const char *path) { return dlopen(path, RTLD_NOW | RTLD_LOCAL); }
static void *lib_sym(lib_t lib, const char *name) { return dlsym(lib, name); }
#ifdef __APPLE__
#define I18N_NAME "%s/libicui18n.%d.dylib"
#define UC_NAME "%s/libicuuc.%d.dylib"
#else
#define I18N_NAME "%s/libicui18n.so.%d"
#define UC_NAME "%s/libicuuc.so.%d"
#endif
#endif

enum { UCAL_MONTH = 2, UCAL_DATE = 5, UCAL_EXTENDED_YEAR = 19, UCAL_JULIAN_DAY = 20 };

typedef void (*get_version_fn)(unsigned char version[4]);
typedef void *(*ucal_open_fn)(const void *zone, int32_t zone_length, const char *locale, int32_t type, int32_t *status);
typedef void (*ucal_clear_fn)(void *cal);
typedef void (*ucal_set_fn)(void *cal, int32_t field, int32_t value);
typedef int32_t (*ucal_get_fn)(const void *cal, int32_t field, int32_t *status);

static int major;

/* ICU usually suffixes its symbols with the major version (ucal_open_77). Function pointers are
 * copied through memcpy because ISO C does not allow converting void * to a function pointer. */
static void symbol(lib_t lib, const char *name, void *fn, size_t size) {
    char versioned[64];
    void *p;
    snprintf(versioned, sizeof versioned, "%s_%d", name, major);
    p = lib_sym(lib, versioned);
    if (!p) p = lib_sym(lib, name);
    if (!p) {
        fprintf(stderr, "symbol %s not found\n", name);
        exit(2);
    }
    memcpy(fn, &p, size);
}

/* The plain 33-year rule (what ICU 50-76 implements). Years here are positive. */
static int64_t plain_nowruz(int64_t y) { return 1948320 + 365 * (y - 1) + (8 * y + 21) / 33; }

static persian_date month_day(int32_t y, int64_t doy) {
    persian_date p;
    p.year = y;
    p.month = (int32_t)(doy < 186 ? doy / 31 + 1 : (doy - 186) / 30 + 7);
    p.day = (int32_t)(doy < 186 ? doy % 31 + 1 : (doy - 186) % 30 + 1);
    return p;
}

static persian_date plain_from_jdn(int64_t jdn) {
    int64_t y = (jdn - 1948320) / 366 + 1;
    while (jdn >= plain_nowruz(y + 1)) y++;
    while (jdn < plain_nowruz(y)) y--;
    return month_day((int32_t)y, jdn - plain_nowruz(y));
}

static int same(persian_date a, persian_date b) { return a.year == b.year && a.month == b.month && a.day == b.day; }

int main(int argc, char **argv) {
    char path[4096];
    lib_t uc, i18n;
    get_version_fn get_version;
    ucal_open_fn ucal_open;
    ucal_clear_fn ucal_clear;
    ucal_set_fn ucal_set;
    ucal_get_fn ucal_get;
    unsigned char v[4];
    int32_t status = 0;
    void *cal;
    FILE *f;
    char line[256];
    long days = 0, reference_wrong = 0, to_persian_wrong = 0, from_persian_wrong = 0, differs = 0;
    int has_corrections;

    if (argc < 3) {
        fprintf(stderr, "usage: %s <ICU library folder> <ICU major version> [years.csv]\n", argv[0]);
        return 2;
    }
    major = atoi(argv[2]);
    has_corrections = major >= 77;
    snprintf(path, sizeof path, UC_NAME, argv[1], major);
    uc = lib_open(path);
    snprintf(path, sizeof path, I18N_NAME, argv[1], major);
    i18n = lib_open(path);
    if (!uc || !i18n) {
        fprintf(stderr, "cannot load ICU %d from %s\n", major, argv[1]);
        return 2;
    }
    symbol(uc, "u_getVersion", &get_version, sizeof get_version);
    symbol(i18n, "ucal_open", &ucal_open, sizeof ucal_open);
    symbol(i18n, "ucal_clear", &ucal_clear, sizeof ucal_clear);
    symbol(i18n, "ucal_set", &ucal_set, sizeof ucal_set);
    symbol(i18n, "ucal_get", &ucal_get, sizeof ucal_get);
    get_version(v);
    cal = ucal_open(NULL, 0, "fa_IR@calendar=persian", 0, &status);
    if (status > 0 || !cal) {
        fprintf(stderr, "ucal_open failed: %d\n", (int)status);
        return 2;
    }

    f = fopen(argc > 3 ? argv[3] : "../../../data/years.csv", "r");
    if (!f || !fgets(line, sizeof line, f)) { /* skip the header */
        fprintf(stderr, "cannot read years.csv\n");
        return 2;
    }
    while (fgets(line, sizeof line, f)) {
        int year, leap, length;
        long long nowruz;
        int64_t doy;
        if (sscanf(line, "%d,%d,%d,%*10[^,],%lld", &year, &leap, &length, &nowruz) != 4) continue;
        for (doy = 0; doy < length; doy++, days++) {
            int64_t jdn = nowruz + doy, back = 0;
            persian_date truth = month_day(year, doy), ours, expected, icu;
            if (!persian_from_jdn(jdn, &ours) || !same(ours, truth) ||
                !persian_to_jdn(truth.year, truth.month, truth.day, &back) || back != jdn) {
                reference_wrong++;
            }
            expected = has_corrections ? truth : plain_from_jdn(jdn);
            ucal_clear(cal);
            ucal_set(cal, UCAL_JULIAN_DAY, (int32_t)jdn);
            icu.year = ucal_get(cal, UCAL_EXTENDED_YEAR, &status);
            icu.month = ucal_get(cal, UCAL_MONTH, &status) + 1; /* ICU months are 0-based */
            icu.day = ucal_get(cal, UCAL_DATE, &status);
            if (!same(icu, expected)) to_persian_wrong++;
            if (!same(icu, truth)) differs++;
            ucal_clear(cal);
            ucal_set(cal, UCAL_EXTENDED_YEAR, expected.year);
            ucal_set(cal, UCAL_MONTH, expected.month - 1);
            ucal_set(cal, UCAL_DATE, expected.day);
            if (ucal_get(cal, UCAL_JULIAN_DAY, &status) != jdn) from_persian_wrong++;
        }
    }
    fclose(f);
    printf("C + ICU4C %d.%d", v[0], v[1]);
    if (v[2] || v[3]) printf(".%d", v[2]);
    if (v[3]) printf(".%d", v[3]);
    printf(": %ld days (1178-3000 AP) | implementation wrong: %ld | ICU differs from ground truth on %ld days | ICU matches %s: ",
           days, reference_wrong, differs, has_corrections ? "the ground truth" : "the plain 33-year rule");
    if (to_persian_wrong + from_persian_wrong == 0) printf("yes (both directions)\n");
    else printf("NO (G->P %ld, P->G %ld)\n", to_persian_wrong, from_persian_wrong);
    return reference_wrong + to_persian_wrong + from_persian_wrong == 0 && days == 665837 ? 0 : 1;
}
