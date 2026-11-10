// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! Compares, for every single day, three things:
//!   1. the ground truth (astronomical calendar, data/years.csv),
//!   2. this repository's Rust implementation,
//!   3. ICU4X: the `icu_calendar` Date API and the low-level `calendrical_calculations`
//!      functions (`fast_persian_from_fixed`, `fixed_from_fast_persian`, `is_leap_year`).
//!
//! Run: cargo run --release

use calendrical_calculations::persian as cc;
use calendrical_calculations::rata_die::RataDie;
use icu_calendar::cal::Persian;
use icu_calendar::{Date, Gregorian};
use persian_calendar as reference;

/// JDN = R.D. + 1721425
const RD_TO_JDN: i64 = 1_721_425;

fn truth_table() -> Vec<(i32, i64, bool)> {
    let path = concat!(env!("CARGO_MANIFEST_DIR"), "/../../../data/years.csv");
    std::fs::read_to_string(path)
        .unwrap()
        .lines()
        .skip(1)
        .map(|l| {
            let f: Vec<&str> = l.split(',').collect();
            (f[0].parse().unwrap(), f[4].parse().unwrap(), f[1] == "1")
        })
        .collect()
}

/// (year, month, day) for the `doy`-th day (0-based) of a year.
fn month_day(doy: i64) -> (u8, u8) {
    if doy < 186 {
        ((doy / 31 + 1) as u8, (doy % 31 + 1) as u8)
    } else {
        ((((doy - 186) / 30) + 7) as u8, ((doy - 186) % 30 + 1) as u8)
    }
}

#[derive(Default)]
struct Counts {
    days: u64,
    reference_wrong: u64,
    icu4x_date_wrong: u64,
    cc_from_fixed_wrong: u64,
    cc_to_fixed_wrong: u64,
    leap_wrong: u64,
}

fn main() {
    let truth = truth_table();
    let mut c = Counts::default();

    // 1178..=3000, every day, both directions.
    for (year, nowruz_jdn, leap) in &truth {
        let (year, leap) = (*year, *leap);
        if reference::is_leap_year(year) != leap || cc::is_leap_year(year) != leap {
            c.leap_wrong += 1;
        }
        let length = if leap { 366 } else { 365 };
        for doy in 0..length {
            c.days += 1;
            let jdn = nowruz_jdn + doy;
            let (month, day) = month_day(doy);
            let expected = reference::PersianDate { year, month, day };

            // Our implementation, both directions.
            if reference::from_jdn(jdn) != Some(expected)
                || reference::to_jdn(year, month, day) != Some(jdn)
            {
                c.reference_wrong += 1;
            }

            // ICU4X Date API, both directions (via the proleptic Gregorian/ISO calendar).
            let (gy, gm, gd) = reference::jdn_to_gregorian(jdn).unwrap();
            let p = Date::try_new_iso(gy, gm, gd).unwrap().to_calendar(Persian);
            let back = Date::try_new_persian(year, month, day).map(|d| d.to_calendar(Gregorian));
            let back_ok = back.is_ok_and(|g| {
                (g.era_year().year, g.month().ordinal, g.day_of_month().0) == (gy, gm, gd)
            });
            if (p.era_year().year, p.month().ordinal, p.day_of_month().0) != (year, month, day)
                || !back_ok
            {
                c.icu4x_date_wrong += 1;
            }

            // calendrical_calculations, the functions ICU4X is built on.
            let rd = RataDie::new(jdn - RD_TO_JDN);
            if cc::fast_persian_from_fixed(rd).ok() != Some((year, month, day)) {
                c.cc_from_fixed_wrong += 1;
            }
            if cc::fixed_from_fast_persian(year, month, day).to_i64_date() + RD_TO_JDN != jdn {
                c.cc_to_fixed_wrong += 1;
            }
        }
    }
    println!(
        "1178-3000 AP, every day ({} days, {} years):\n  \
         implementation wrong: {}\n  \
         ICU4X icu_calendar 2.4.0 Date API wrong: {}\n  \
         calendrical_calculations 0.2.4 fast_persian_from_fixed wrong: {}\n  \
         calendrical_calculations 0.2.4 fixed_from_fast_persian wrong: {}\n  \
         leap years wrong (reference or ICU4X): {}",
        c.days,
        truth.len(),
        c.reference_wrong,
        c.icu4x_date_wrong,
        c.cc_from_fixed_wrong,
        c.cc_to_fixed_wrong,
        c.leap_wrong
    );

    // Outside the verified range there is no ground truth, but both implementations are the same
    // arithmetic calendar, so they must agree everywhere. ICU4X computes the year from a single
    // estimate in fast_persian_from_fixed; we also correct the estimate. Do they ever differ?
    let first = reference::MIN_JDN;
    let last = reference::MAX_JDN + 1;
    let mut differ = Vec::new();
    for jdn in first..last {
        let ours = reference::from_jdn(jdn).unwrap();
        let theirs = cc::fast_persian_from_fixed(RataDie::new(jdn - RD_TO_JDN)).unwrap();
        if (ours.year, ours.month, ours.day) != theirs {
            differ.push((jdn, ours, theirs));
        }
        let ours_leap = reference::is_leap_year(ours.year);
        if ours.month == 1 && ours.day == 1 && ours_leap != cc::is_leap_year(ours.year) {
            differ.push((jdn, ours, (ours.year, 0, 0)));
        }
    }
    println!(
        "{}..{} AP (the whole accepted range), every day ({} days): this implementation vs calendrical_calculations differ on {} days",
        reference::MIN_YEAR,
        reference::MAX_YEAR,
        last - first,
        differ.len()
    );
    for (jdn, ours, theirs) in differ.iter().take(10) {
        println!(
            "  JDN {jdn}: ours {}/{}/{}, ICU4X {:?}",
            ours.year, ours.month, ours.day, theirs
        );
    }

    let failed = c.reference_wrong
        + c.icu4x_date_wrong
        + c.cc_from_fixed_wrong
        + c.cc_to_fixed_wrong
        + c.leap_wrong;
    std::process::exit(if failed == 0 && differ.is_empty() {
        0
    } else {
        1
    });
}
