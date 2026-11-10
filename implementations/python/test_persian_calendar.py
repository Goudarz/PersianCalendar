# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT

"""Checks persian_calendar.py against the ground-truth data in /data.

Run:  python -m unittest implementations/python/test_persian_calendar.py
"""
import csv
import os
import unittest

import persian_calendar as pc

DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "data")


def rows(name):
    with open(os.path.join(DATA, name), encoding="utf-8") as f:
        return [{k: int(v) if k != "nowruz_gregorian" else v for k, v in r.items()}
                for r in csv.DictReader(f)]


class TestPersianCalendar(unittest.TestCase):
    def test_years(self):
        for r in rows("years.csv"):
            y = r["year"]
            self.assertEqual(pc.is_leap_year(y), r["is_leap"] == 1, y)
            self.assertEqual(pc.days_in_month(y, 12), 30 if r["is_leap"] else 29, y)
            gy, gm, gd = (int(x) for x in r["nowruz_gregorian"].split("-"))
            self.assertEqual(pc.to_gregorian(y, 1, 1), (gy, gm, gd), y)
            self.assertEqual(pc.to_jdn(y, 1, 1), r["nowruz_jdn"], y)
            self.assertEqual(pc.day_of_week(y, 1, 1), r["nowruz_weekday"], y)
            # every day of the year against the ground truth, both directions
            for doy in range(r["days_in_year"]):
                m, d = (doy // 31 + 1, doy % 31 + 1) if doy < 186 else ((doy - 186) // 30 + 7, (doy - 186) % 30 + 1)
                jdn = r["nowruz_jdn"] + doy
                if pc.from_jdn(jdn) != (y, m, d) or pc.to_jdn(y, m, d) != jdn:
                    self.fail(f"{y}/{m}/{d} (JDN {jdn})")

    def test_vectors(self):
        for r in rows("test-vectors.csv"):
            p = (r["jy"], r["jm"], r["jd"])
            g = (r["gy"], r["gm"], r["gd"])
            self.assertEqual(pc.to_gregorian(*p), g, p)
            self.assertEqual(pc.from_gregorian(*g), p, g)
            self.assertEqual(pc.to_jdn(*p), r["jdn"], p)
            self.assertEqual(pc.from_jdn(r["jdn"]), p, p)
            self.assertEqual(pc.day_of_week(*p), r["weekday"], p)

    def test_every_day_round_trip(self):
        first = pc.to_jdn(pc.VERIFIED_MIN_YEAR, 1, 1)
        last = pc.to_jdn(pc.VERIFIED_MAX_YEAR, 12, pc.days_in_month(pc.VERIFIED_MAX_YEAR, 12))
        prev = pc.from_jdn(first - 1)
        for jdn in range(first, last + 1):
            y, m, d = pc.from_jdn(jdn)
            self.assertTrue(pc.is_valid_date(y, m, d), jdn)
            self.assertEqual(pc.to_jdn(y, m, d), jdn)
            self.assertEqual(pc.gregorian_to_jdn(*pc.jdn_to_gregorian(jdn)), jdn)
            # consecutive days: next day of month, or first day of next month/year
            self.assertTrue((y, m, d - 1) == prev or (d == 1 and (
                (y, m - 1) == prev[:2] or (m == 1 and prev[0] == y - 1 and prev[1] == 12))), jdn)
            prev = (y, m, d)

    def test_extrapolated_years(self):
        # Outside 1178-3000 AP the calendar is an arithmetic extrapolation. Years <= 0 make
        # intermediate values negative, which is exactly where truncating division breaks.
        self.assertEqual(pc.to_jdn(-1000, 1, 1), 1582713)
        self.assertEqual(pc.to_jdn(0, 1, 1), 1947955)
        self.assertEqual(pc.to_jdn(4000, 1, 1), 3408925)
        self.assertEqual(pc.from_jdn(0), (-5334, 9, 3))
        self.assertEqual(pc.day_of_week(-1000, 1, 1), 1)
        self.assertTrue(pc.is_leap_year(-3))
        self.assertFalse(pc.is_leap_year(-1000))
        for first_year, last_year in ((-1000, 1177), (3001, 4000)):
            first = pc.to_jdn(first_year, 1, 1)
            last = pc.to_jdn(last_year, 12, pc.days_in_month(last_year, 12))
            prev = pc.from_jdn(first - 1)
            for jdn in range(first, last + 1):
                y, m, d = pc.from_jdn(jdn)
                self.assertTrue(pc.is_valid_date(y, m, d), jdn)
                self.assertEqual(pc.to_jdn(y, m, d), jdn)
                self.assertEqual(pc.gregorian_to_jdn(*pc.jdn_to_gregorian(jdn)), jdn)
                self.assertTrue((y, m, d - 1) == prev or (d == 1 and (
                    (y, m - 1) == prev[:2] or (m == 1 and prev[0] == y - 1 and prev[1] == 12))), jdn)
                prev = (y, m, d)

    def test_validation(self):
        self.assertTrue(pc.is_valid_date(1403, 12, 30))   # leap year
        self.assertFalse(pc.is_valid_date(1404, 12, 30))  # common year
        self.assertTrue(pc.is_valid_date(1404, 6, 31))
        self.assertFalse(pc.is_valid_date(1404, 7, 31))
        self.assertFalse(pc.is_valid_date(1404, 13, 1))
        self.assertFalse(pc.is_valid_date(1404, 0, 1))
        self.assertFalse(pc.is_valid_date(1404, 1, 0))
        self.assertFalse(pc.is_valid_date(1404, 1.5, 1))   # not an integer
        self.assertFalse(pc.is_valid_date(1404, True, 1))
        with self.assertRaises(ValueError):
            pc.to_jdn(1404, 12, 30)

    def test_limits_and_bad_input(self):
        # The accepted range ends exactly at MIN_JDN / MAX_JDN.
        self.assertEqual(pc.to_jdn(pc.MIN_YEAR, 1, 1), pc.MIN_JDN)
        self.assertEqual(pc.to_jdn(pc.MAX_YEAR, 12, pc.days_in_month(pc.MAX_YEAR, 12)), pc.MAX_JDN)
        self.assertEqual(pc.from_jdn(pc.MIN_JDN), (pc.MIN_YEAR, 1, 1))
        self.assertEqual(pc.from_jdn(pc.MAX_JDN), (pc.MAX_YEAR, 12, 29))
        self.assertEqual(pc.jdn_to_gregorian(pc.MIN_JDN), (-99379, 3, 29))
        self.assertEqual(pc.jdn_to_gregorian(pc.MAX_JDN), (100622, 3, 13))
        self.assertEqual(pc.gregorian_to_jdn(100622, 3, 13), pc.MAX_JDN)
        for bad in (pc.MIN_JDN - 1, pc.MAX_JDN + 1, 2**63 - 1, -2**63, 10**100):
            with self.assertRaises(ValueError):
                pc.from_jdn(bad)
            with self.assertRaises(ValueError):
                pc.jdn_to_gregorian(bad)
        for year in (pc.MIN_YEAR - 1, pc.MAX_YEAR + 1, 2**31):
            self.assertFalse(pc.is_valid_date(year, 1, 1))
            with self.assertRaises(ValueError):
                pc.to_jdn(year, 1, 1)
            with self.assertRaises(ValueError):
                pc.is_leap_year(year)
        with self.assertRaises(ValueError):
            pc.gregorian_to_jdn(100622, 3, 14)          # one day after MAX_JDN
        # Invalid Gregorian dates are rejected, not silently normalized.
        for bad in ((2025, 2, 29), (2025, 2, 30), (2025, 13, 1), (2025, 0, 1), (2025, 4, 31), (2025, 1, 0)):
            with self.assertRaises(ValueError):
                pc.gregorian_to_jdn(*bad)
            with self.assertRaises(ValueError):
                pc.from_gregorian(*bad)
        self.assertEqual(pc.from_gregorian(2024, 2, 29), (1402, 12, 10))  # Gregorian leap day
        # Non-integers raise TypeError.
        for call in (lambda: pc.is_leap_year(1.5), lambda: pc.is_leap_year("1403"), lambda: pc.is_leap_year(True),
                     lambda: pc.days_in_month(1404, 6.5), lambda: pc.to_jdn(1404, 1.0, 1),
                     lambda: pc.from_jdn(2460756.5), lambda: pc.from_jdn("2460756"),
                     lambda: pc.gregorian_to_jdn(2025, 3, 21.0)):
            with self.assertRaises(TypeError):
                call()

    def test_every_year_of_the_full_range(self):
        # Every year MIN_YEAR..MAX_YEAR: years are contiguous, first and last day round-trip, and the
        # leap years sit where every other implementation (and ICU 77.1+) puts them (checksums).
        expected, leaps, nowruz_sum = pc.MIN_JDN, 0, 0
        for y in range(pc.MIN_YEAR, pc.MAX_YEAR + 1):
            nowruz = pc.to_jdn(y, 1, 1)
            leap = pc.is_leap_year(y)
            last = 30 if leap else 29
            if (nowruz != expected or pc.from_jdn(nowruz) != (y, 1, 1)
                    or pc.days_in_month(y, 12) != last or pc.from_jdn(nowruz + 364 + leap) != (y, 12, last)):
                self.fail(f"year {y}")
            leaps += leap
            nowruz_sum += nowruz
            expected = nowruz + 365 + leap
        self.assertEqual(expected - 1, pc.MAX_JDN)
        self.assertEqual((leaps, nowruz_sum), (48_485, 389_592_978_180))

    def test_known_dates(self):
        self.assertEqual(pc.from_gregorian(2025, 3, 21), (1404, 1, 1))
        self.assertEqual(pc.from_gregorian(2025, 3, 20), (1403, 12, 30))
        self.assertEqual(pc.from_gregorian(1979, 2, 11), (1357, 11, 22))
        self.assertEqual(pc.to_gregorian(1304, 1, 1), (1925, 3, 21))
        self.assertEqual(pc.day_of_year(1404, 12, 29), 365)
        self.assertEqual(pc.day_of_year(1404, 7, 1), 187)
        self.assertEqual(pc.WEEKDAY_NAMES_EN[pc.day_of_week(1404, 1, 1)], "Friday")


if __name__ == "__main__":
    unittest.main()
