// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! Solar Hijri (Persian, Jalali, Shamsi) calendar.
//!
//! Algorithm: 33-year arithmetic rule + 78-year correction table,
//! the same algorithm as ICU 77.1+ and ICU4X (`icu_calendar`). Matches the astronomical calendar
//! (52.5 E meridian) for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
//!
//! For production Rust code prefer `icu_calendar` (ICU4X), which uses the same algorithm.

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct PersianDate {
    pub year: i32,
    /// 1 = Farvardin ... 12 = Esfand
    pub month: u8,
    pub day: u8,
}

/// Years checked against the astronomical calendar.
pub const VERIFIED_MIN_YEAR: i32 = 1178;
pub const VERIFIED_MAX_YEAR: i32 = 3000;

/// Accepted input range. Within it every intermediate value fits in 32 bits.
/// Functions return `None` for anything outside it.
pub const MIN_YEAR: i32 = -100_000;
pub const MAX_YEAR: i32 = 100_000;
/// 1 Farvardin -100000 (Gregorian -99379-03-29).
pub const MIN_JDN: i64 = -34_576_287;
/// 29 Esfand 100000 (Gregorian 100622-03-13).
pub const MAX_JDN: i64 = 38_472_562;
const GREGORIAN_MIN_YEAR: i32 = -99_379;
const GREGORIAN_MAX_YEAR: i32 = 100_622;

const EPOCH_JDN: i64 = 1_948_320;

/// Sorted. Years the 33-year rule calls leap but are not; the next year is leap instead.
pub const NON_LEAP_CORRECTION: [i32; 78] = [
    1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931, 1964, 1997, 2030, 2059,
    2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224, 2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356,
    2360, 2389, 2393, 2422, 2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
    2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785, 2789, 2818, 2822, 2847,
    2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921, 2946, 2950, 2954, 2979, 2983, 2987,
];

pub const MONTH_NAMES_FA: [&str; 12] = [
    "فروردین",
    "اردیبهشت",
    "خرداد",
    "تیر",
    "مرداد",
    "شهریور",
    "مهر",
    "آبان",
    "آذر",
    "دی",
    "بهمن",
    "اسفند",
];
pub const MONTH_NAMES_EN: [&str; 12] = [
    "Farvardin",
    "Ordibehesht",
    "Khordad",
    "Tir",
    "Mordad",
    "Shahrivar",
    "Mehr",
    "Aban",
    "Azar",
    "Dey",
    "Bahman",
    "Esfand",
];
pub const WEEKDAY_NAMES_FA: [&str; 7] = [
    "شنبه",
    "یکشنبه",
    "دوشنبه",
    "سه\u{200c}شنبه",
    "چهارشنبه",
    "پنجشنبه",
    "جمعه",
];
pub const WEEKDAY_NAMES_EN: [&str; 7] = [
    "Saturday",
    "Sunday",
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
];

fn is_correction(year: i64) -> bool {
    i32::try_from(year).is_ok_and(|y| NON_LEAP_CORRECTION.binary_search(&y).is_ok())
}

/// True if the Persian year has 366 days. Total: never panics or overflows for any `i32`
/// (outside `MIN_YEAR..=MAX_YEAR` the answer is the arithmetic extrapolation).
pub fn is_leap_year(year: i32) -> bool {
    let year = i64::from(year);
    if is_correction(year) {
        return false;
    }
    if is_correction(year - 1) {
        return true;
    }
    (25 * year + 11).rem_euclid(33) < 8
}

/// 29, 30 or 31; `None` if `month` is not 1..=12.
pub fn days_in_month(year: i32, month: u8) -> Option<u8> {
    match month {
        1..=6 => Some(31),
        7..=11 => Some(30),
        12 => Some(if is_leap_year(year) { 30 } else { 29 }),
        _ => None,
    }
}

/// True for a real Persian date in `MIN_YEAR..=MAX_YEAR`.
pub fn is_valid_date(year: i32, month: u8, day: u8) -> bool {
    (MIN_YEAR..=MAX_YEAR).contains(&year)
        && days_in_month(year, month).is_some_and(|n| day >= 1 && day <= n)
}

fn nowruz_jdn(year: i64) -> i64 {
    let mut jdn = EPOCH_JDN + 365 * (year - 1) + (8 * year + 21).div_euclid(33);
    if is_correction(year - 1) {
        jdn -= 1;
    }
    jdn
}

fn days_before_month(month: u8) -> i64 {
    let m = i64::from(month);
    if m <= 7 {
        31 * (m - 1)
    } else {
        30 * (m - 1) + 6
    }
}

/// Julian Day Number of a Persian date; `None` if the date is invalid.
pub fn to_jdn(year: i32, month: u8, day: u8) -> Option<i64> {
    is_valid_date(year, month, day)
        .then(|| nowruz_jdn(i64::from(year)) + days_before_month(month) + i64::from(day) - 1)
}

/// Persian date of a Julian Day Number; `None` outside `MIN_JDN..=MAX_JDN`.
pub fn from_jdn(jdn: i64) -> Option<PersianDate> {
    if !(MIN_JDN..=MAX_JDN).contains(&jdn) {
        return None;
    }
    let mut year = 1 + (33 * (jdn - EPOCH_JDN) + 3).div_euclid(12053);
    while jdn >= nowruz_jdn(year + 1) {
        year += 1;
    }
    while jdn < nowruz_jdn(year) {
        year -= 1;
    }
    let doy = jdn - nowruz_jdn(year); // 0-based
    let (month, day) = if doy < 186 {
        (doy / 31 + 1, doy % 31 + 1)
    } else {
        ((doy - 186) / 30 + 7, (doy - 186) % 30 + 1)
    };
    Some(PersianDate {
        year: year as i32,
        month: month as u8,
        day: day as u8,
    })
}

fn gregorian_days_in_month(year: i32, month: u8) -> u8 {
    match month {
        2 if year.rem_euclid(4) == 0
            && (year.rem_euclid(100) != 0 || year.rem_euclid(400) == 0) =>
        {
            29
        }
        2 => 28,
        4 | 6 | 9 | 11 => 30,
        _ => 31,
    }
}

/// JDN of a proleptic Gregorian date (month 1..=12); `None` for an invalid date or one
/// outside the accepted range.
pub fn gregorian_to_jdn(year: i32, month: u8, day: u8) -> Option<i64> {
    if !(GREGORIAN_MIN_YEAR..=GREGORIAN_MAX_YEAR).contains(&year)
        || !(1..=12).contains(&month)
        || day < 1
        || day > gregorian_days_in_month(year, month)
    {
        return None;
    }
    let (month, day) = (i64::from(month), i64::from(day));
    let a = (14 - month).div_euclid(12);
    let y = i64::from(year) + 4800 - a;
    let m = month + 12 * a - 3;
    let jdn = day + (153 * m + 2).div_euclid(5) + 365 * y + y.div_euclid(4) - y.div_euclid(100)
        + y.div_euclid(400)
        - 32045;
    (MIN_JDN..=MAX_JDN).contains(&jdn).then_some(jdn)
}

/// Proleptic Gregorian (year, month, day) of a JDN; `None` outside `MIN_JDN..=MAX_JDN`.
pub fn jdn_to_gregorian(jdn: i64) -> Option<(i32, u8, u8)> {
    if !(MIN_JDN..=MAX_JDN).contains(&jdn) {
        return None;
    }
    let a = jdn + 32044;
    let b = (4 * a + 3).div_euclid(146097);
    let c = a - (146097 * b).div_euclid(4);
    let d = (4 * c + 3).div_euclid(1461);
    let e = c - (1461 * d).div_euclid(4);
    let m = (5 * e + 2).div_euclid(153);
    let year = 100 * b + d - 4800 + m / 10;
    let month = m + 3 - 12 * (m / 10);
    let day = e - (153 * m + 2).div_euclid(5) + 1;
    Some((year as i32, month as u8, day as u8))
}

pub fn to_gregorian(year: i32, month: u8, day: u8) -> Option<(i32, u8, u8)> {
    to_jdn(year, month, day).and_then(jdn_to_gregorian)
}

/// Persian date of a proleptic Gregorian date; `None` for an invalid or out-of-range date.
pub fn from_gregorian(year: i32, month: u8, day: u8) -> Option<PersianDate> {
    gregorian_to_jdn(year, month, day).and_then(from_jdn)
}

/// 0 = Saturday (شنبه) ... 6 = Friday (جمعه).
pub fn day_of_week(year: i32, month: u8, day: u8) -> Option<u8> {
    to_jdn(year, month, day).map(|jdn| (jdn + 2).rem_euclid(7) as u8)
}

/// 1 ... 365/366.
pub fn day_of_year(year: i32, month: u8, day: u8) -> Option<u16> {
    is_valid_date(year, month, day).then(|| (days_before_month(month) + i64::from(day)) as u16)
}
