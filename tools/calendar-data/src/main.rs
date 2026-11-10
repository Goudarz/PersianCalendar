// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! calendar-data: generates and verifies the ground-truth data of this repository.
//!
//! ```text
//! cargo run --release                          # regenerate data/*.csv, data/*.txt
//! cargo run --release -- --check               # verify they are up to date (CI)
//! cargo run --release -- equinox 2025 2026     # March equinox and Nowruz of Gregorian years
//! cargo run --release -- score-libraries       # rewrite tools/library-evaluation/results/SUMMARY.md
//! ```

mod astronomy;
mod data;
mod fixed;
mod persian;
mod score;

use std::path::{Path, PathBuf};
use std::process::ExitCode;

fn repo_root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR")).join("..").join("..")
}

fn read(path: &Path) -> Result<String, String> {
    std::fs::read_to_string(path).map_err(|e| format!("{}: {e}", path.display()))
}

fn generate(check: bool) -> Result<(), String> {
    let data_dir = repo_root().join("data");
    let nowruz = data::Nowruz::compute();
    let official = read(&data_dir.join("official").join("kabise-1206-1498.txt"))?;
    let matched = data::check_official(&official, &nowruz)?;
    let files = data::build(&nowruz)?;
    let mut stale = Vec::new();
    for (name, content) in &files {
        let path = data_dir.join(name);
        if check {
            if read(&path).ok().as_deref() != Some(content.as_str()) {
                stale.push(*name);
            }
        } else {
            std::fs::write(&path, content).map_err(|e| format!("{}: {e}", path.display()))?;
        }
    }
    println!("official table: {matched} years match the astronomical calendar");
    if !stale.is_empty() {
        return Err(format!(
            "stale data files (run `cargo run --release`): {}",
            stale.join(", ")
        ));
    }
    println!(
        "{} data files {}",
        files.len(),
        if check { "are up to date" } else { "written" }
    );
    Ok(())
}

fn equinox(years: &[String]) -> Result<(), String> {
    println!("Gregorian year | March equinox (Iran time, UTC+3:30) | Nowruz (1 Farvardin)");
    for y in years {
        let year: i64 = y.parse().map_err(|_| format!("not a year: {y}"))?;
        let local = astronomy::march_equinox(year) + 3.5 / 24.0;
        let date = local.floor() as i64;
        let seconds = ((local - date as f64) * 86400.0).round() as i64;
        let (gy, gm, gd) = fixed::to_gregorian(date);
        let persian_year = year - 621;
        let (ny, nm, nd) = fixed::to_gregorian(persian::nowruz(persian_year));
        println!(
            "{year} | {gy:04}-{gm:02}-{gd:02} {:02}:{:02}:{:02} | {persian_year} AP = {ny:04}-{nm:02}-{nd:02}",
            seconds / 3600,
            seconds % 3600 / 60,
            seconds % 60
        );
    }
    Ok(())
}

fn score_libraries(dir: Option<&String>) -> Result<(), String> {
    let root = repo_root();
    let results = dir.map(PathBuf::from).unwrap_or_else(|| {
        root.join("tools")
            .join("library-evaluation")
            .join("results")
    });
    let summary = score::summary(&read(&root.join("data").join("years.csv"))?, &results)?;
    let path = results.join("SUMMARY.md");
    std::fs::write(&path, &summary).map_err(|e| format!("{}: {e}", path.display()))?;
    print!("{summary}");
    Ok(())
}

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let result = match args.first().map(String::as_str) {
        None | Some("generate") => generate(false),
        Some("--check") | Some("check") => generate(true),
        Some("equinox") => equinox(&args[1..]),
        Some("score-libraries") => score_libraries(args.get(1)),
        Some(other) => Err(format!(
            "unknown command: {other} (use generate, --check, equinox, score-libraries)"
        )),
    };
    match result {
        Ok(()) => ExitCode::SUCCESS,
        Err(e) => {
            eprintln!("error: {e}");
            ExitCode::FAILURE
        }
    }
}
