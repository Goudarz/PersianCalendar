// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! Day counts. A "fixed date" is a Rata Die (R.D.) day number: R.D. 1 is 1 January 1 CE
//! in the proleptic Gregorian calendar. JDN = R.D. + 1721425.

/// Add this to an R.D. day number to get the Julian Day Number of the same day.
pub const RD_TO_JDN: i64 = 1_721_425;

fn is_gregorian_leap(year: i64) -> bool {
    year.rem_euclid(4) == 0 && !matches!(year.rem_euclid(400), 100 | 200 | 300)
}

#[cfg(test)]
fn is_julian_leap(year: i64) -> bool {
    year.rem_euclid(4) == if year > 0 { 0 } else { 3 }
}

/// Days in the months before `month`, as if February had 30 days, then corrected.
fn days_before_month(month: i64, leap: bool) -> i64 {
    let assuming_30_day_feb = (367 * month - 362).div_euclid(12);
    let correction = match (month <= 2, leap) {
        (true, _) => 0,
        (false, true) => -1,
        (false, false) => -2,
    };
    assuming_30_day_feb + correction
}

/// R.D. of a proleptic Gregorian date.
pub fn from_gregorian(year: i64, month: i64, day: i64) -> i64 {
    let y = year - 1;
    365 * y + y.div_euclid(4) - y.div_euclid(100)
        + y.div_euclid(400)
        + days_before_month(month, is_gregorian_leap(year))
        + day
}

/// Proleptic Gregorian year containing R.D. `date`.
pub fn gregorian_year(date: i64) -> i64 {
    let d0 = date - 1;
    let n400 = d0.div_euclid(146_097);
    let d1 = d0.rem_euclid(146_097);
    let n100 = d1.div_euclid(36_524);
    let d2 = d1.rem_euclid(36_524);
    let n4 = d2.div_euclid(1_461);
    let d3 = d2.rem_euclid(1_461);
    let n1 = d3.div_euclid(365);
    let year = 400 * n400 + 100 * n100 + 4 * n4 + n1;
    if n100 == 4 || n1 == 4 {
        year // the last day (366th) of a leap year
    } else {
        year + 1
    }
}

/// Proleptic Gregorian (year, month, day) of R.D. `date`.
pub fn to_gregorian(date: i64) -> (i64, i64, i64) {
    let year = gregorian_year(date);
    let prior_days = date - from_gregorian(year, 1, 1);
    let correction = if date < from_gregorian(year, 3, 1) {
        0
    } else if is_gregorian_leap(year) {
        1
    } else {
        2
    };
    let month = (12 * (prior_days + correction) + 373).div_euclid(367);
    let day = date - from_gregorian(year, month, 1) + 1;
    (year, month, day)
}

/// R.D. of a Julian-calendar date (no year 0: 1 BCE is year -1). Used to check the epoch.
#[cfg(test)]
pub fn from_julian(year: i64, month: i64, day: i64) -> i64 {
    const JULIAN_EPOCH: i64 = -1; // R.D. of 1 January 1 CE (Julian) = 30 December 0 (Gregorian)
    let y = if year < 0 { year + 1 } else { year };
    JULIAN_EPOCH - 1
        + 365 * (y - 1)
        + (y - 1).div_euclid(4)
        + days_before_month(month, is_julian_leap(year))
        + day
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn known_dates() {
        assert_eq!(from_gregorian(1, 1, 1), 1);
        assert_eq!(from_gregorian(2000, 1, 1), 730_120);
        assert_eq!(from_gregorian(2016, 4, 11) + RD_TO_JDN, 2_457_490);
        assert_eq!(from_julian(622, 3, 19), 226_896); // epoch of the Persian calendar
        assert_eq!(from_julian(1582, 10, 5), from_gregorian(1582, 10, 15));
    }

    #[test]
    fn gregorian_round_trip() {
        for date in from_gregorian(-2000, 1, 1)..from_gregorian(4000, 1, 1) {
            let (y, m, d) = to_gregorian(date);
            assert_eq!(from_gregorian(y, m, d), date);
        }
    }
}
