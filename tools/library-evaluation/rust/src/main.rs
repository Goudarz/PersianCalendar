// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! Evaluates ICU4X (icu_calendar). Run: cargo run --release
use icu_calendar::{Date, Gregorian};
use std::io::Write;

fn main() {
    let name = "ICU4X icu_calendar 2.4.0";
    let mut f = std::fs::File::create(format!("../results/{}.txt", name.replace(' ', "_"))).unwrap();
    writeln!(f, "# {name}").unwrap();
    for y in 1100..=3300 {
        if let Ok(d) = Date::try_new_persian(y, 1, 1) {
            let g = d.to_calendar(Gregorian);
            writeln!(f, "{} {:04}-{:02}-{:02}", y, g.era_year().year, g.month().ordinal, g.day_of_month().0).unwrap();
        }
    }
    println!("{name}");
}
