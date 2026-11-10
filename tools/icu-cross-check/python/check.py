# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT

"""Compares ICU4C (called directly through ctypes), this repository's Python implementation and the
ground truth (data/years.csv) for every day of 1178-3000 AP, in both directions.

ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table; for
them the script checks that ICU matches the plain 33-year rule exactly instead.

Usage:
    python check.py                      # finds the system ICU (Linux, macOS/Homebrew)
    python check.py /path/to/icu/lib     # folder with libicui18n.so.NN / icuinNN.dll (Windows)

No packages needed: ICU's C API is called with ctypes, using only integer arguments.
"""
import ctypes
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..", "..")
sys.path.insert(0, os.path.join(ROOT, "implementations", "python"))
import persian_calendar as ref  # noqa: E402

UCAL_MONTH, UCAL_DATE, UCAL_EXTENDED_YEAR, UCAL_JULIAN_DAY = 2, 5, 19, 20


def find_icu(folder):
    """Returns (i18n library path, common library path, major version)."""
    patterns = [folder] if folder else [
        "/usr/lib/x86_64-linux-gnu", "/usr/lib/aarch64-linux-gnu", "/usr/lib64", "/usr/lib",
        "/opt/homebrew/opt/icu4c/lib", "/usr/local/opt/icu4c/lib",
    ]
    for d in patterns:
        for i18n in sorted(glob.glob(os.path.join(d, "icuin[0-9]*.dll")) + glob.glob(os.path.join(d, "libicui18n.so.[0-9]*"))
                           + glob.glob(os.path.join(d, "libicui18n.[0-9]*.dylib")), reverse=True):
            major = re.search(r"(?:icuin|so\.|i18n\.)(\d+)", os.path.basename(i18n)).group(1)
            uc = i18n.replace("icuin", "icuuc").replace("libicui18n", "libicuuc")
            if os.path.exists(uc):
                return i18n, uc, int(major)
    sys.exit("ICU not found; pass the folder containing the ICU libraries")


def main():
    i18n_path, uc_path, major = find_icu(sys.argv[1] if len(sys.argv) > 1 else None)
    if os.name == "nt":
        os.add_dll_directory(os.path.dirname(os.path.abspath(i18n_path)))
    uc, i18n = ctypes.CDLL(uc_path), ctypes.CDLL(i18n_path)

    def fn(lib, name, restype, *argtypes):
        for symbol in (f"{name}_{major}", name):  # ICU usually suffixes symbols with its major version
            if hasattr(lib, symbol):
                f = getattr(lib, symbol)
                f.restype, f.argtypes = restype, argtypes
                return f
        sys.exit(f"symbol {name} not found")

    i32, ptr = ctypes.c_int32, ctypes.c_void_p
    status = i32(0)
    u_get_version = fn(uc, "u_getVersion", None, ctypes.c_char * 4)
    ucal_open = fn(i18n, "ucal_open", ptr, ptr, i32, ctypes.c_char_p, i32, ctypes.POINTER(i32))
    ucal_clear = fn(i18n, "ucal_clear", None, ptr)
    ucal_set = fn(i18n, "ucal_set", None, ptr, i32, i32)
    ucal_get = fn(i18n, "ucal_get", i32, ptr, i32, ctypes.POINTER(i32))

    v = (ctypes.c_char * 4)()
    u_get_version(v)
    version = re.sub(r"(\.0)+$", "", ".".join(str(b) for b in bytes(v)))  # 79.1.0.0 -> 79.1, 80.0.0.0 -> 80
    cal = ucal_open(None, 0, b"fa_IR@calendar=persian", 0, ctypes.byref(status))
    if status.value > 0:
        sys.exit(f"ucal_open failed: {status.value}")

    def icu_from_jdn(jdn):
        ucal_clear(cal)
        ucal_set(cal, UCAL_JULIAN_DAY, jdn)
        y, m, d = (ucal_get(cal, f, ctypes.byref(status)) for f in (UCAL_EXTENDED_YEAR, UCAL_MONTH, UCAL_DATE))
        return y, m + 1, d  # ICU months are 0-based

    def icu_to_jdn(y, m, d):
        ucal_clear(cal)
        ucal_set(cal, UCAL_EXTENDED_YEAR, y)
        ucal_set(cal, UCAL_MONTH, m - 1)
        ucal_set(cal, UCAL_DATE, d)
        return ucal_get(cal, UCAL_JULIAN_DAY, ctypes.byref(status))

    # The plain 33-year rule (what ICU 50-76 implements).
    def plain_nowruz(y):
        return 1948320 + 365 * (y - 1) + (8 * y + 21) // 33

    def month_day(y, doy):
        return (y, doy // 31 + 1, doy % 31 + 1) if doy < 186 else (y, (doy - 186) // 30 + 7, (doy - 186) % 30 + 1)

    def plain_from_jdn(jdn):
        y = (jdn - 1948320) // 366 + 1
        while jdn >= plain_nowruz(y + 1):
            y += 1
        while jdn < plain_nowruz(y):
            y -= 1
        return month_day(y, jdn - plain_nowruz(y))

    has_corrections = major >= 77
    days = ref_wrong = to_persian_wrong = from_persian_wrong = differs = 0
    with open(os.path.join(ROOT, "data", "years.csv"), encoding="utf-8") as f:
        rows = [line.split(",") for line in f.read().split()[1:]]
    for year, leap, _, _, nowruz, _ in rows:
        year, nowruz = int(year), int(nowruz)
        for doy in range(366 if leap == "1" else 365):
            days += 1
            jdn = nowruz + doy
            truth = month_day(year, doy)
            if ref.from_jdn(jdn) != truth or ref.to_jdn(*truth) != jdn:
                ref_wrong += 1
            expected = truth if has_corrections else plain_from_jdn(jdn)
            icu = icu_from_jdn(jdn)
            to_persian_wrong += icu != expected
            differs += icu != truth
            from_persian_wrong += icu_to_jdn(*expected) != jdn

    # The whole accepted range, year by year (ICU uses the same arithmetic calendar outside 1178-3000):
    # first day, last day, and year length. ICU 76.1 and older differ exactly in the 78 correction years.
    full_wrong = 0
    for y in range(ref.MIN_YEAR, ref.MAX_YEAR + 1):
        nowruz, last = ref.to_jdn(y, 1, 1), ref.days_in_month(y, 12)
        if icu_to_jdn(y, 1, 1) != nowruz or icu_from_jdn(nowruz) != (y, 1, 1) or icu_from_jdn(ref.to_jdn(y, 12, last)) != (y, 12, last):
            full_wrong += 1
    full_expected = 0 if has_corrections else len(ref.NON_LEAP_CORRECTION)

    rule = "the ground truth" if has_corrections else "the plain 33-year rule"
    ok = "yes (both directions)" if to_persian_wrong + from_persian_wrong == 0 else f"NO (G->P {to_persian_wrong}, P->G {from_persian_wrong})"
    print(f"Python {sys.version.split()[0]} + ICU4C {version} (ctypes): {days} days (1178-3000 AP) | implementation wrong: {ref_wrong} | "
          f"ICU differs from ground truth on {differs} days | ICU matches {rule}: {ok} | "
          f"years {ref.MIN_YEAR}..{ref.MAX_YEAR}: ICU differs in {full_wrong} years (expected {full_expected})")
    sys.exit(0 if ref_wrong + to_persian_wrong + from_persian_wrong == 0 and full_wrong == full_expected else 1)


if __name__ == "__main__":
    main()
