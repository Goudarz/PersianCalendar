// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! Checks the implementation against /data. Run: cargo test --release
use persian_calendar::*;

fn csv(name: &str) -> Vec<Vec<String>> {
    let path = format!("{}/../../data/{}", env!("CARGO_MANIFEST_DIR"), name);
    let text = std::fs::read_to_string(path).unwrap();
    text.trim()
        .lines()
        .skip(1)
        .map(|l| l.split(',').map(String::from).collect())
        .collect()
}

fn n<T: std::str::FromStr>(s: &str) -> T
where
    T::Err: std::fmt::Debug,
{
    s.parse().unwrap()
}

#[test]
fn years() {
    for r in csv("years.csv") {
        let y: i32 = n(&r[0]);
        let leap = r[1] == "1";
        assert_eq!(is_leap_year(y), leap, "{y}");
        assert_eq!(days_in_month(y, 12), Some(if leap { 30 } else { 29 }));
        let (gy, gm, gd) = to_gregorian(y, 1, 1).unwrap();
        assert_eq!(format!("{gy:04}-{gm:02}-{gd:02}"), r[3]);
        assert_eq!(to_jdn(y, 1, 1), Some(n(&r[4])));
        assert_eq!(day_of_week(y, 1, 1), Some(n(&r[5])));
        // every day of the year against the ground truth, both directions
        let nowruz: i64 = n(&r[4]);
        for doy in 0..n::<i64>(&r[2]) {
            let (m, d) = if doy < 186 {
                (doy / 31 + 1, doy % 31 + 1)
            } else {
                ((doy - 186) / 30 + 7, (doy - 186) % 30 + 1)
            };
            let (m, d) = (m as u8, d as u8);
            let expected = PersianDate {
                year: y,
                month: m,
                day: d,
            };
            assert_eq!(from_jdn(nowruz + doy), Some(expected));
            assert_eq!(to_jdn(y, m, d), Some(nowruz + doy));
        }
    }
}

#[test]
fn vectors() {
    for r in csv("test-vectors.csv") {
        let (jy, jm, jd): (i32, u8, u8) = (n(&r[0]), n(&r[1]), n(&r[2]));
        let g: (i32, u8, u8) = (n(&r[3]), n(&r[4]), n(&r[5]));
        let p = PersianDate {
            year: jy,
            month: jm,
            day: jd,
        };
        assert_eq!(to_gregorian(jy, jm, jd), Some(g));
        assert_eq!(from_gregorian(g.0, g.1, g.2), Some(p));
        assert_eq!(to_jdn(jy, jm, jd), Some(n(&r[6])));
        assert_eq!(from_jdn(n(&r[6])), Some(p));
        assert_eq!(day_of_week(jy, jm, jd), Some(n(&r[7])));
    }
}

#[test]
fn every_day_round_trips() {
    let first = to_jdn(VERIFIED_MIN_YEAR, 1, 1).unwrap();
    let last_day = days_in_month(VERIFIED_MAX_YEAR, 12).unwrap();
    let last = to_jdn(VERIFIED_MAX_YEAR, 12, last_day).unwrap();
    let mut prev = from_jdn(first - 1).unwrap();
    for jdn in first..=last {
        let d = from_jdn(jdn).unwrap();
        assert_eq!(to_jdn(d.year, d.month, d.day), Some(jdn));
        let (gy, gm, gd) = jdn_to_gregorian(jdn).unwrap();
        assert_eq!(gregorian_to_jdn(gy, gm, gd), Some(jdn));
        let consecutive = (d.day == prev.day + 1 && d.month == prev.month && d.year == prev.year)
            || (d.day == 1
                && ((d.month == prev.month + 1 && d.year == prev.year)
                    || (d.month == 1 && prev.month == 12 && d.year == prev.year + 1)));
        assert!(consecutive, "{jdn}");
        prev = d;
    }
}

/// Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
#[test]
fn extrapolated_years() {
    assert_eq!(to_jdn(-1000, 1, 1), Some(1_582_713));
    assert_eq!(to_jdn(0, 1, 1), Some(1_947_955));
    assert_eq!(to_jdn(4000, 1, 1), Some(3_408_925));
    assert_eq!(
        from_jdn(0),
        Some(PersianDate {
            year: -5334,
            month: 9,
            day: 3
        })
    );
    assert_eq!(day_of_week(-1000, 1, 1), Some(1));
    assert!(is_leap_year(-3) && !is_leap_year(-1000));
    for (first_year, last_year) in [(-1000, 1177), (3001, 4000)] {
        let first = to_jdn(first_year, 1, 1).unwrap();
        let last = to_jdn(last_year, 12, days_in_month(last_year, 12).unwrap()).unwrap();
        let mut prev = from_jdn(first - 1).unwrap();
        for jdn in first..=last {
            let d = from_jdn(jdn).unwrap();
            assert_eq!(to_jdn(d.year, d.month, d.day), Some(jdn));
            let (gy, gm, gd) = jdn_to_gregorian(jdn).unwrap();
            assert_eq!(gregorian_to_jdn(gy, gm, gd), Some(jdn));
            let consecutive =
                (d.day == prev.day + 1 && d.month == prev.month && d.year == prev.year)
                    || (d.day == 1
                        && ((d.month == prev.month + 1 && d.year == prev.year)
                            || (d.month == 1 && prev.month == 12 && d.year == prev.year + 1)));
            assert!(consecutive, "{jdn}");
            prev = d;
        }
    }
}

#[test]
fn validation_and_known_dates() {
    assert!(is_valid_date(1403, 12, 30));
    assert!(!is_valid_date(1404, 12, 30));
    assert!(is_valid_date(1404, 6, 31));
    assert!(!is_valid_date(1404, 7, 31));
    assert!(!is_valid_date(1404, 13, 1));
    assert!(!is_valid_date(1404, 0, 1));
    assert!(!is_valid_date(1404, 1, 0));
    assert_eq!(to_jdn(1404, 12, 30), None);
    assert_eq!(
        from_gregorian(2025, 3, 21),
        Some(PersianDate {
            year: 1404,
            month: 1,
            day: 1
        })
    );
    assert_eq!(
        from_gregorian(1979, 2, 11),
        Some(PersianDate {
            year: 1357,
            month: 11,
            day: 22
        })
    );
    assert_eq!(day_of_year(1404, 7, 1), Some(187));
    assert_eq!(
        WEEKDAY_NAMES_EN[day_of_week(1404, 1, 1).unwrap() as usize],
        "Friday"
    );
}

/// Limits: exact boundaries, and `None` for anything outside (no panic, no overflow, no hang).
#[test]
fn limits_and_bad_input() {
    assert_eq!(to_jdn(MIN_YEAR, 1, 1), Some(MIN_JDN));
    assert_eq!(to_jdn(MAX_YEAR, 12, 29), Some(MAX_JDN));
    assert_eq!(
        from_jdn(MIN_JDN),
        Some(PersianDate {
            year: MIN_YEAR,
            month: 1,
            day: 1
        })
    );
    assert_eq!(
        from_jdn(MAX_JDN),
        Some(PersianDate {
            year: MAX_YEAR,
            month: 12,
            day: 29
        })
    );
    assert_eq!(jdn_to_gregorian(MIN_JDN), Some((-99379, 3, 29)));
    assert_eq!(jdn_to_gregorian(MAX_JDN), Some((100622, 3, 13)));
    assert_eq!(gregorian_to_jdn(100622, 3, 13), Some(MAX_JDN));
    assert_eq!(gregorian_to_jdn(100622, 3, 14), None);
    for bad in [MIN_JDN - 1, MAX_JDN + 1, i64::MAX, i64::MIN, 1 << 53] {
        assert_eq!(from_jdn(bad), None, "{bad}");
        assert_eq!(jdn_to_gregorian(bad), None, "{bad}");
    }
    for bad in [MIN_YEAR - 1, MAX_YEAR + 1, i32::MAX, i32::MIN] {
        assert!(!is_valid_date(bad, 1, 1));
        assert_eq!(to_jdn(bad, 1, 1), None);
        let _ = is_leap_year(bad); // total: must not panic
    }
    for (y, m, d) in [
        (2025, 2, 29),
        (2025, 2, 30),
        (2025, 13, 1),
        (2025, 0, 1),
        (2025, 4, 31),
        (2025, 1, 0),
    ] {
        assert_eq!(gregorian_to_jdn(y, m, d), None, "{y}-{m}-{d}");
        assert_eq!(from_gregorian(y, m, d), None, "{y}-{m}-{d}");
    }
    assert_eq!(
        from_gregorian(2024, 2, 29),
        Some(PersianDate {
            year: 1402,
            month: 12,
            day: 10
        })
    );
}

/// Every day of the whole accepted range (73 million days): Persian and Gregorian dates are
/// consecutive, both directions round-trip, and the leap years sit where every other
/// implementation (and ICU 77.1+) puts them (checksums shared by all tests).
#[test]
fn every_day_of_the_full_range() {
    let mut prev = from_jdn(MIN_JDN).unwrap();
    let mut prev_g = jdn_to_gregorian(MIN_JDN).unwrap();
    let (mut leaps, mut nowruz_sum) = (0_i64, MIN_JDN);
    for jdn in MIN_JDN + 1..=MAX_JDN {
        let d = from_jdn(jdn).unwrap();
        if d.month == 1 && d.day == 1 {
            nowruz_sum += jdn;
        }
        if d.month == 12 && d.day == 30 {
            leaps += 1;
        }
        assert_eq!(to_jdn(d.year, d.month, d.day), Some(jdn));
        let consecutive = (d.day == prev.day + 1 && d.month == prev.month && d.year == prev.year)
            || (d.day == 1
                && ((d.month == prev.month + 1 && d.year == prev.year)
                    || (d.month == 1 && prev.month == 12 && d.year == prev.year + 1)));
        assert!(consecutive, "{jdn}");
        let g = jdn_to_gregorian(jdn).unwrap();
        assert_eq!(gregorian_to_jdn(g.0, g.1, g.2), Some(jdn));
        let g_consecutive = (g.2 == prev_g.2 + 1 && g.1 == prev_g.1 && g.0 == prev_g.0)
            || (g.2 == 1
                && ((g.1 == prev_g.1 + 1 && g.0 == prev_g.0)
                    || (g.1 == 1 && prev_g.1 == 12 && g.0 == prev_g.0 + 1)));
        assert!(g_consecutive, "{jdn}");
        prev = d;
        prev_g = g;
    }
    assert_eq!(
        prev,
        PersianDate {
            year: MAX_YEAR,
            month: 12,
            day: 29
        }
    );
    assert_eq!((leaps, nowruz_sum), (48_485, 389_592_978_180));
    assert_eq!(
        (MIN_YEAR..=MAX_YEAR).filter(|&y| is_leap_year(y)).count(),
        48_485
    );
}
