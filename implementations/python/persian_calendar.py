# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT

"""Solar Hijri (Persian, Jalali, Shamsi) calendar.

Algorithm: the 33-year arithmetic rule plus a table of 78 corrections,
the same algorithm used by ICU 77.1+ and ICU4X.
It matches the astronomical calendar computed on the 52.5 E meridian for
every year from 1178 to 3000 AP (1799-3621 CE), including the official
University of Tehran Calendar Center table (1206-1498 AP).

Outside 1178-3000 AP the functions still work (arithmetic extrapolation),
but the results are not guaranteed to match the astronomical calendar.
Inputs are accepted for years MIN_YEAR..MAX_YEAR; anything else raises
ValueError, and non-integers raise TypeError.

Pure integer arithmetic, no dependencies. Python's // is floor division,
which the formulas require.
"""

# Years checked against the astronomical calendar.
VERIFIED_MIN_YEAR = 1178
VERIFIED_MAX_YEAR = 3000

# Accepted input range. Within it every intermediate value fits in 32 bits.
MIN_YEAR = -100_000
MAX_YEAR = 100_000
MIN_JDN = -34_576_287   # 1 Farvardin -100000 (Gregorian -99379-03-29)
MAX_JDN = 38_472_562    # 29 Esfand 100000 (Gregorian 100622-03-13)

# Julian Day Number of 1 Farvardin 1 as the 33-year formula computes it.
# A calibration constant: the historical (astronomical) Nowruz of year 1 was
# one day later; only years 1178-3000 are verified against astronomy.
_EPOCH_JDN = 1948320

# Years the 33-year rule calls leap but are not; the next year is leap instead.
NON_LEAP_CORRECTION = frozenset((
    1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
    1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
    2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
    2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
    2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
    2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
    2946, 2950, 2954, 2979, 2983, 2987,
))

MONTH_NAMES_FA = ("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                  "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
MONTH_NAMES_EN = ("Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
                  "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand")
# Index 0 = Saturday, the first day of the Iranian week.
WEEKDAY_NAMES_FA = ("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
WEEKDAY_NAMES_EN = ("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

_GREGORIAN_MIN_YEAR = -99_379
_GREGORIAN_MAX_YEAR = 100_622


def _is_int(value):
    return isinstance(value, int) and not isinstance(value, bool)


def _check_int(name, value):
    if not _is_int(value):
        raise TypeError(f"{name} must be an int, not {type(value).__name__}")


def _check_year(year):
    _check_int("year", year)
    if not MIN_YEAR <= year <= MAX_YEAR:
        raise ValueError(f"year out of range [{MIN_YEAR}, {MAX_YEAR}]: {year}")


def is_leap_year(year):
    """True if the Persian year has 366 days (Esfand has 30 days)."""
    _check_year(year)
    if year in NON_LEAP_CORRECTION:
        return False
    if year - 1 in NON_LEAP_CORRECTION:
        return True
    return (25 * year + 11) % 33 < 8


def days_in_month(year, month):
    """29, 30 or 31."""
    _check_year(year)
    _check_int("month", month)
    if not 1 <= month <= 12:
        raise ValueError(f"month out of range [1, 12]: {month}")
    if month <= 6:
        return 31
    if month <= 11:
        return 30
    return 30 if is_leap_year(year) else 29


def is_valid_date(year, month, day):
    """True for a real Persian date in the accepted range; never raises."""
    if not (_is_int(year) and _is_int(month) and _is_int(day)):
        return False
    if not (MIN_YEAR <= year <= MAX_YEAR and 1 <= month <= 12):
        return False
    return 1 <= day <= days_in_month(year, month)


def _nowruz_jdn(year):
    """JDN of 1 Farvardin of the given year."""
    jdn = _EPOCH_JDN + 365 * (year - 1) + (8 * year + 21) // 33
    if year - 1 in NON_LEAP_CORRECTION:
        jdn -= 1
    return jdn


def _days_before_month(month):
    return 31 * (month - 1) if month <= 7 else 30 * (month - 1) + 6


def _check_date(year, month, day):
    if not is_valid_date(year, month, day):
        for name, value in (("year", year), ("month", month), ("day", day)):
            _check_int(name, value)
        raise ValueError(f"invalid Persian date: {year}/{month}/{day}")


def to_jdn(year, month, day):
    """Julian Day Number of a Persian date."""
    _check_date(year, month, day)
    return _nowruz_jdn(year) + _days_before_month(month) + day - 1


def _check_jdn(jdn):
    _check_int("jdn", jdn)
    if not MIN_JDN <= jdn <= MAX_JDN:
        raise ValueError(f"jdn out of range [{MIN_JDN}, {MAX_JDN}]: {jdn}")


def from_jdn(jdn):
    """Persian (year, month, day) of a Julian Day Number."""
    _check_jdn(jdn)
    year = 1 + (33 * (jdn - _EPOCH_JDN) + 3) // 12053  # estimate, then correct
    while jdn >= _nowruz_jdn(year + 1):
        year += 1
    while jdn < _nowruz_jdn(year):
        year -= 1
    day_of_year = jdn - _nowruz_jdn(year)  # 0-based
    if day_of_year < 186:
        return year, day_of_year // 31 + 1, day_of_year % 31 + 1
    day_of_year -= 186
    return year, day_of_year // 30 + 7, day_of_year % 30 + 1


def _gregorian_days_in_month(year, month):
    if month == 2:
        return 29 if year % 4 == 0 and (year % 100 != 0 or year % 400 == 0) else 28
    return 30 if month in (4, 6, 9, 11) else 31


def gregorian_to_jdn(year, month, day):
    """JDN of a proleptic Gregorian date. Raises ValueError for an invalid date."""
    for name, value in (("year", year), ("month", month), ("day", day)):
        _check_int(name, value)
    if not (_GREGORIAN_MIN_YEAR <= year <= _GREGORIAN_MAX_YEAR and 1 <= month <= 12
            and 1 <= day <= _gregorian_days_in_month(year, month)):
        raise ValueError(f"invalid Gregorian date: {year}-{month}-{day}")
    a = (14 - month) // 12
    y = year + 4800 - a
    m = month + 12 * a - 3
    jdn = day + (153 * m + 2) // 5 + 365 * y + y // 4 - y // 100 + y // 400 - 32045
    _check_jdn(jdn)
    return jdn


def jdn_to_gregorian(jdn):
    """Proleptic Gregorian (year, month, day) of a JDN."""
    _check_jdn(jdn)
    a = jdn + 32044
    b = (4 * a + 3) // 146097
    c = a - 146097 * b // 4
    d = (4 * c + 3) // 1461
    e = c - 1461 * d // 4
    m = (5 * e + 2) // 153
    return (100 * b + d - 4800 + m // 10,
            m + 3 - 12 * (m // 10),
            e - (153 * m + 2) // 5 + 1)


def to_gregorian(year, month, day):
    return jdn_to_gregorian(to_jdn(year, month, day))


def from_gregorian(year, month, day):
    return from_jdn(gregorian_to_jdn(year, month, day))


def day_of_week(year, month, day):
    """0 = Saturday (شنبه) ... 6 = Friday (جمعه)."""
    return (to_jdn(year, month, day) + 2) % 7


def day_of_year(year, month, day):
    """1 ... 365/366."""
    _check_date(year, month, day)
    return _days_before_month(month) + day


if __name__ == "__main__":
    import datetime
    t = datetime.date.today()
    y, m, d = from_gregorian(t.year, t.month, t.day)
    print(f"{WEEKDAY_NAMES_FA[day_of_week(y, m, d)]} {d} {MONTH_NAMES_FA[m - 1]} {y}")
