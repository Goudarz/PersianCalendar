// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! The astronomical Solar Hijri calendar.
//!
//! Definition used by the Calendar Center of the University of Tehran: 1 Farvardin is the day
//! whose true noon on the 52.5° E meridian (Iran Standard Time) comes after the March equinox.
//! Equivalently: the first day whose apparent solar longitude at true noon is in [0°, 2°].

use crate::astronomy::{
    MEAN_TROPICAL_YEAR, estimate_prior_solar_longitude, solar_longitude, true_noon,
};

/// Meridian of Iran Standard Time (UTC+3:30), degrees east.
pub const IRAN_LONGITUDE: f64 = 52.5;

/// R.D. of 1 Farvardin 1 AP = 19 March 622 (Julian).
pub const EPOCH: i64 = 226_896;

fn noon_longitude(date: i64) -> f64 {
    solar_longitude(true_noon(date, IRAN_LONGITUDE))
}

/// R.D. of the Persian new year on or before fixed date `date`.
pub fn new_year_on_or_before(date: i64) -> i64 {
    let approx = estimate_prior_solar_longitude(0.0, true_noon(date, IRAN_LONGITUDE));
    let mut day = approx.floor() as i64 - 1;
    while noon_longitude(day) > 2.0 {
        day += 1;
    }
    day
}

/// R.D. of 1 Farvardin of Persian year `year` (year >= 1).
pub fn nowruz(year: i64) -> i64 {
    // Start the search in the autumn before, so that "on or before" finds the right Nowruz.
    new_year_on_or_before(EPOCH + 180 + (MEAN_TROPICAL_YEAR * (year - 1) as f64).floor() as i64)
}

/// Days in months 1..month-1 (fixed month lengths since 1304 AP).
pub fn days_before_month(month: i64) -> i64 {
    if month <= 7 {
        31 * (month - 1)
    } else {
        30 * (month - 1) + 6
    }
}

pub fn month_length(month: i64, leap: bool) -> i64 {
    match month {
        1..=6 => 31,
        7..=11 => 30,
        _ => {
            if leap {
                30
            } else {
                29
            }
        }
    }
}

/// 0 = Saturday … 6 = Friday.
pub fn weekday(jdn: i64) -> i64 {
    (jdn + 2).rem_euclid(7)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::fixed;

    #[test]
    fn known_new_years() {
        assert_eq!(fixed::to_gregorian(nowruz(1304)), (1925, 3, 21));
        assert_eq!(fixed::to_gregorian(nowruz(1403)), (2024, 3, 20));
        assert_eq!(fixed::to_gregorian(nowruz(1404)), (2025, 3, 21)); // equinox at 12:31, after noon
        assert_eq!(fixed::to_gregorian(nowruz(1408)), (2029, 3, 20));
        assert_eq!(nowruz(1404) - nowruz(1403), 366); // 1403 is a leap year
    }

    /// How close does the equinox come to true noon? When the margin is only seconds or minutes,
    /// the result depends on the precision of the solar theory and on ΔT, which cannot be
    /// predicted that precisely centuries ahead. Prints every year closer than 10 minutes.
    #[test]
    fn equinox_margin_to_noon() {
        let margin = |year: i64| {
            let day = nowruz(year);
            let after = noon_longitude(day);
            let before = 360.0 - noon_longitude(day - 1);
            after.min(before) / 360.0 * crate::astronomy::MEAN_TROPICAL_YEAR * 86400.0
        };
        let close: Vec<(i64, i64)> = (1178..=3000)
            .map(|y| (y, margin(y).round() as i64))
            .filter(|&(_, m)| m < 600)
            .collect();
        println!("years whose equinox is within 10 minutes of true noon: {close:?}");
        // In the range of the official table the closest call is 1470 AP (about 2.7 minutes).
        assert!((1206..=1498).all(|y| margin(y) > 120.0));
    }
}
