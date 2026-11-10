# Jalali (Persian / Shamsi) Calendar for Developers

**[فارسی](README.fa.md)** · English

[![test](https://github.com/Goudarz/PersianCalendar/actions/workflows/test.yml/badge.svg)](https://github.com/Goudarz/PersianCalendar/actions/workflows/test.yml)

A practical, developer-to-developer guide for adding the Jalali calendar (Solar Hijri, Persian, Shamsi — تقویم هجری شمسی)
to your software. Everything you need is gathered and tested in one place: how the calendar actually works, how to
compute it accurately, which existing libraries you can trust, and ready-to-copy implementations in 14 languages. The goal is simple:
**spend less time debugging calendar quirks, and ship accurate dates in your apps.**

> **Why does this matter?** A surprising number of popular implementations get the calendar subtly wrong. While writing this guide, I tested
> common libraries against the official calendar: one widely used library gets *last* year (1403/1404) wrong, another shifts
> every date by one day during the first two months of every Gregorian leap year, and a third breaks starting in March 2029.
> See the full breakdown in [Languages & libraries](docs/en/06-languages-and-libraries.md).

## The calendar in 60 seconds

* **New Year:** The year starts on **Nowruz** (1 Farvardin), the day of the March equinox—which falls on 20, 21, or 22 March. For example, 1404 AP began on 21 March 2025.
* **Months:** The first 6 months have 31 days, the next 5 have 30 days, and the last month (Esfand) has 29 days—or 30 in a leap year.
* **Weeks:** The week starts on **Saturday**, and Friday is the official weekly holiday.
* **Leap years are astronomical:** A new year starts on the day the spring equinox occurs before true noon at 52.5° E (Iran Standard Time).
  This works out to 8 leap years every 33 years—usually 4 years apart, and occasionally 5 years apart (for example, 1403 → **1408**).
* **The arithmetic formula:** This simple rule matches the official astronomical calendar across the entire 1178–3000 AP range:

  ```
  leap(y) = y ∉ C  and  (y−1 ∈ C  or  (25y + 11) mod 33 < 8)
  C = {1502, 1601, 1634, … 78 years …, 2987}          (data/corrections.txt)
  ```

* **Warning:** Do **not** use the popular **2820-year cycle** (Birashk's algorithm)—it already disagrees with the official calendar for 1403/1404.

## What should I use?

| If you want to… | Best choice |
|---|---|
| Format and display Persian dates in the browser or Node.js | Built-in `Intl.DateTimeFormat('fa-IR', …)` — [example](docs/en/06-languages-and-libraries.md#javascript--typescript) |
| Do date math or conversions in JavaScript / TypeScript | `Temporal` with `calendar: 'persian'`, `jalaali-js`, or `date-fns-jalali` |
| Work in Python | `persiantools` or `jdatetime` |
| Work in Java or Android | ICU4J / `android.icu` with `fa_IR@calendar=persian` |
| Work in Scala | ICU4J, or the [Scala implementation](implementations/scala) in this repository |
| Work in C# / .NET | Built-in `System.Globalization.PersianCalendar` with culture `fa-IR` |
| Work in Go | `go-jalaali` or `go-persian-calendar` |
| Work in Rust | `icu_calendar` (ICU4X) |
| Work in PHP | Built-in `intl` (`IntlDateFormatter` with `@calendar=persian`) or `morilog/jalali` |
| Work in Swift / iOS / macOS | Built-in `Calendar(identifier: .persian)` |
| Work in Dart / Flutter | `shamsi_date` |
| Work in Ruby, C/C++, SQL, or want zero dependencies | Copy the [implementation](implementations) for your language from this repository |

Built-in ICU APIs are accurate all the way to 3000 AP starting from **ICU 77.1** (Node.js 24+ and modern browsers); older ICU versions are accurate up to March 2124 (end of 1502 AP).

## What's in this repository

### Guide

| # | English | فارسی |
|---|---|---|
| 1 | [Calendar basics](docs/en/01-basics.md) — names, months, weeks, history | [مبانی تقویم](docs/fa/01-basics.md) |
| 2 | [Leap years](docs/en/02-leap-years.md) — the astronomical rule, the 33-year rule, why not 2820 | [سال‌های کبیسه](docs/fa/02-leap-years.md) |
| 3 | [The algorithm](docs/en/03-algorithm.md) — step-by-step math with a worked example | [الگوریتم](docs/fa/03-algorithm.md) |
| 4 | [Implementation guide](docs/en/04-implementation-guide.md) — database storage, time zones, arithmetic, parsing, UI, checklist | [راهنمای پیاده‌سازی](docs/fa/04-implementation-guide.md) |
| 5 | [Localization](docs/en/05-localization.md) — digits, ی/ک, ZWNJ, RTL layout, month names | [بومی‌سازی](docs/fa/05-localization.md) |
| 6 | [Languages & libraries](docs/en/06-languages-and-libraries.md) — test verdicts and code examples for 14 languages | [زبان‌ها و کتابخانه‌ها](docs/fa/06-languages-and-libraries.md) |
| 7 | [Holidays](docs/en/07-holidays.md) — official holidays, the lunar Hijri challenge, data modeling | [تعطیلات](docs/fa/07-holidays.md) |
| 8 | [Common pitfalls](docs/en/08-pitfalls.md) — 33 real-world bugs and how to fix them | [خطاهای رایج](docs/fa/08-pitfalls.md) |
| 9 | [Testing](docs/en/09-testing.md) — reference datasets and the essential test suite | [آزمون](docs/fa/09-testing.md) |
| 10 | [References](docs/en/10-references.md) — official sources, papers, and standards | [منابع](docs/fa/10-references.md) |

### Ready-to-use implementations — [`implementations/`](implementations)

The exact same algorithm implemented in **14** languages: Python, JavaScript, TypeScript, Java, Kotlin, Scala, C#, Go, Rust, PHP, Dart, C (and C++),
Ruby, and PostgreSQL. Every implementation has zero external dependencies, exposes the same clean API (`is_leap_year`, `days_in_month`, `to_jdn`, `from_jdn`,
`to_gregorian`, `from_gregorian`, `day_of_week`, `day_of_year`), and passes the same rigorous test suite:

* **Every single day from 1178 to 3000 AP** (665,837 days) verified in both directions against the astronomical calendar;
* 8,510 Gregorian ↔ Persian boundary test cases, plus every day of the extrapolated years −1000…1177 and 3001…4000;
* **Every year across the full supported range (−100000…100000)**, verified with checksums that match ICU 77.1+, ICU4X, and Temporal;
* **Strict input validation:** out-of-range dates, huge numbers, non-integers, and invalid Gregorian dates (like Feb 30) are cleanly rejected with an error.

Every language implementation is also cross-checked day by day across 1178–3000 AP against an independent calendar engine:

| Language | Cross-checked against | Language | Cross-checked against |
|---|---|---|---|
| Python | ICU4C 76.1–79.1 | Go | ICU4C 76.1–79.1 (including 32-bit) |
| JavaScript | ICU4C (`Intl`), Temporal | Rust | ICU4X 2.4 (every day of the full range) |
| TypeScript | ICU4C (`Intl`), Temporal | PHP | ICU4C 77.1 (`intl`) |
| Java | ICU4J 76.1–79.1 | Dart | ICU4C 76.1–79.1 (VM and web) |
| Kotlin | ICU4J 76.1–79.1 | C / C++ | ICU4C 76.1–79.1 (UBSan, 32-bit) |
| Scala | ICU4J 76.1–79.1 | Ruby | ICU4C 76.1–79.1 |
| C# | ICU4C 76.1–79.1, .NET `PersianCalendar` | PostgreSQL | ICU4C (`Intl`), Temporal |

For the full breakdown of what is verified per language, see [implementations/README.md](implementations/README.md#what-is-verified-for-each-language).

### Reference test data — [`data/`](data)

Ground-truth dates computed astronomically and verified against the official table published by the University of Tehran Calendar Center
(1206–1498 AP). It includes leap years and Nowruz dates for 1178–3000 AP, 8,510 conversion test vectors, the 78-year correction table, and
CLDR month and weekday names in 11 languages. You can drop these files straight into your own project's test suite.

### Tools — [`tools/`](tools)

* [`calendar-data/`](tools/calendar-data) (Rust): the astronomical engine that generates and verifies `data/`, calculates exact equinox times,
  and scores third-party libraries.
* [`icu-cross-check/`](tools/icu-cross-check): tests every day of 1178–3000 AP across all 14 implementations against ICU4X, ICU4J, ICU4C,
  Temporal, and .NET's `PersianCalendar`.
* [`library-evaluation/`](tools/library-evaluation): automated benchmarks and accuracy tests for 23 popular libraries and built-in APIs ([see summary](tools/library-evaluation/results/SUMMARY.md)).

## How everything was verified

1. Our astronomical engine ([`tools/calendar-data`](tools/calendar-data), calculated on the 52.5° E meridian) matches all 293 years of the official
   University of Tehran Calendar Center table (1206–1498 AP) and produces the exact same output as an independent astronomical implementation.
2. All reference test files in `data/` are generated directly from that astronomical engine.
3. All 14 language implementations are tested against `data/` on current runtimes: Python 3.13, Node.js 26, Java 26, Kotlin 2.4, Scala 3.9, .NET 8, Go 1.27, Rust 1.98,
   PHP 8.5, Dart 3.13, C99 (Zig/Clang), Ruby 4.0, and PostgreSQL 17.
4. Across all 14 implementations, every day of 1178–3000 AP is cross-checked in both directions against ICU4X 2.4, ICU4J 76.1–79.1,
   ICU4C 76.1–79.1, and Temporal. ICU 77.1+, ICU4X, and Temporal agree on every single day; ICU 76.1 lacks the correction table and diverges starting in
   1503 AP; .NET's `PersianCalendar` diverges starting in 1535 AP ([`tools/icu-cross-check`](tools/icu-cross-check)).
5. Inputs outside the supported range (years −100000…100000) are rejected with an explicit error instead of overflowing, hanging, or
   silently returning bogus dates. Within that range, every year is tested in every language, every single day (73,048,850 days) is tested in Rust, and the
   results match ICU4X (every day) as well as ICU4C/ICU4J 77.1+ and Temporal (every year).
6. 23 third-party libraries and built-in APIs were tested against the exact same dataset.

## Contributing

Bug fixes, improvements, new language implementations, and additional library evaluations are very welcome! Check out [CONTRIBUTING.md](CONTRIBUTING.md).
Please include a reliable source for any calendar fact and tests for any code changes.

## License

* Code (implementations, tools, tests): [MIT](LICENSE)
* Documentation (`docs/`, READMEs): [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
* Generated data (`data/*.csv`, `data/*.txt`, `data/*.json`): [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/)
* [`data/official/kabise-1206-1498.txt`](data/official) is a CC0 plain-text transcription of the official table prepared by Roozbeh Pournader.

## Author

**Goudarz Jafari** — <hi@goudarzjafari.com>

## Acknowledgements

* **Ebrahim Byagowi**, creator of [persian-calendar/calendar](https://github.com/persian-calendar/calendar),
  [persian-calendar/events](https://github.com/persian-calendar/events), and the [persian-calendar](https://github.com/persian-calendar)
  project—for the calendar library and open holiday datasets I learned so much from while writing this guide.
* **Roozbeh Pournader**—for the astronomical Persian calendar implementation on the 52.5° E meridian, the 78-year correction table for the 33-year rule,
  and the plain-text transcription of the official leap-year table.
* The [jalaali](https://github.com/jalaali) project, Kazimierz Borkowski, Edward Reingold and Nachum Dershowitz,
  the Calendar Center of the University of Tehran, and the Unicode CLDR/ICU teams.
