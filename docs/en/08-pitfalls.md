# 8. Common pitfalls

[← Holidays](07-holidays.md) · [Next: Testing →](09-testing.md) · [فارسی](../fa/08-pitfalls.md)

Here are 33 real-world bugs that show up again and again in Persian calendar code, along with how to spot them and how to fix them. Almost all of these were reproduced firsthand while testing libraries for this guide.

## 1. Calendar math & leap years

| # | The Bug | How it shows up in your app | How to fix it |
|---|---|---|---|
| 1 | **Using the 2820-year cycle** (Birashk's algorithm) | Every single date from 20 March 2025 to 20 March 2026 is off by 1 day; `30 Esfand 1403` is rejected as invalid! | Use the 33-year rule + 78-year correction table, or pick a ✅ library from [page 6](06-languages-and-libraries.md) |
| 2 | Assuming leap years happen strictly every 4 years (`y % 4 == 3`, etc.) | Breaks starting in 1407/1408 AP because `1403 → 1408` is a 5-year leap gap | Use the proper leap-year rule above |
| 3 | Using truncating integer division (`/`, `%`) on negative numbers | Dates or intermediate values before the epoch (or near boundaries) silently shift by 1 | Always use **floor division** and **floor modulo** ([see page 3](03-algorithm.md#floor-division-the-one-trap)) |
| 4 | Adding "1 month" by converting to Gregorian, adding a Gregorian month, and converting back | `1 Mehr + 1 month` lands on the wrong day instead of `1 Aban` | Do month/year math directly on Persian `(year, month, day)` fields |
| 5 | Adding months/years without clamping the day of the month | `31 Shahrivar + 1 month` throws an exception (in `shamsi_date`) or spills over into `1 Aban` | Clamp the day to `days_in_month(target_year, target_month)` |
| 6 | Hard-coding 29 Esfand as the end of the year | Reports and date pickers miss **30 Esfand** in leap years (like 1403 and 1408) | Always query `days_in_month(y, 12)` |
| 7 | Mixing up month lengths (e.g., giving Mehr 31 days) | Every date from autumn onward is off by 1 day | Remember the rule: **6 × 31 days**, **5 × 30 days**, **Esfand = 29 or 30** |
| 8 | Converting pre-1304 AP (pre-1925) historical dates with modern month rules | Dates in Qajar-era historical records are off by 1–2 days | Before 1304 AP, months followed true solar zodiac transits (29–32 days); use a specialized historical converter like [persian-calendar/calendar](https://github.com/persian-calendar/calendar) |
| 9 | Passing unconstrained inputs outside a library's valid range | Integer overflow, infinite loops, or exceptions (e.g., `jalaali-js` throws after 3177 AP) | Validate input bounds upfront and document your supported range |

## 2. Time zones & midnight boundaries

| # | The Bug | How it shows up in your app | How to fix it |
|---|---|---|---|
| 10 | Converting timestamps in `UTC` (or server local time) instead of the user's time zone | Between `00:00` and `03:30` AM in Iran, your app thinks "today" is still yesterday! | Convert the instant to `Asia/Tehran` (or the user's time zone) *before* extracting the date |
| 11 | **Outdated server/container `tzdata`** (older than `2022b`) | Clocks jump ahead 1 hour in spring/summer; dates around midnight (`23:00–24:00`) are off by a day | Update OS, JDK, ICU, Node.js, and Docker images; Iran permanently abolished DST after September 2022 |
| 12 | Hard-coding `+03:30` instead of `Asia/Tehran` | Historical timestamps from spring/summer before 2023 are off by 1 hour | Always use the IANA zone identifier `Asia/Tehran` |
| 13 | Storing a date-only value (like a birthday) as `00:00 UTC` and formatting it in local time | Users in Europe or the Americas see their birthday shift back by 1 day | Keep date-only values as pure dates (`LocalDate`, `DateOnly`, SQL `DATE`) with no time zone |
| 14 | Parsing a Persian date string into a timestamp in one zone and reading it in another | `IntlDateFormatter::parse('1404/01/01')` in `Asia/Tehran` read back via `date('Y-m-d')` in `UTC` returns `2025-03-20`! | Always read a parsed timestamp in the exact same time zone it was parsed in |
| 15 | Mixing `new Date('2025-03-21')` (UTC midnight) with `new Date(2025, 2, 21)` (local midnight) in JS | Dates randomly shift by 1 day depending on where the browser/server runs | Be consistent, or use `Temporal.PlainDate` |

## 3. Third-party library traps

| # | The Bug | How it shows up in your app | How to fix it |
|---|---|---|---|
| 16 | Using `dayjs` + `jalaliday` (v3.1.1) | **Gregorian → Persian is off by 1 day for all of January and February in every Gregorian leap year** (e.g., `2024-01-20` becomes `1402/11/01` instead of `1402/10/30`!) | Switch to `Temporal`, `jalaali-js`, `date-fns-jalali`, or this repo's JS implementation |
| 17 | Using `khayyam` (Python) | Off by 1 day for all of 1403/1404 (uses the 2820-year cycle) | Switch to `persiantools` or `jdatetime` |
| 18 | Using `parsi-date` (Ruby v0.5.1) | Breaks in leap years like 1408 AP (March 2029), producing `"1407/12/30"` | Use [this repository's Ruby implementation](../../implementations/ruby) |
| 19 | Older ICU (≤ 76.1) in older browsers, Node.js ≤ 23, or older Android phones | Uses the plain 33-year rule without the 78-year correction table | Totally fine for dates before 1502 AP (March 2124); only matters for far-future dates |
| 20 | Pulling a GPL library into a closed-source commercial app | Licensing compliance issue | Check library licenses first (`persian-calendar/calendar` is GPL-2.0+; all code in this repo is MIT) |

## 4. Localization, text & UI

| # | The Bug | How it shows up in your app | How to fix it |
|---|---|---|---|
| 21 | Not normalizing Arabic `ي`/`ك` to Persian `ی`/`ک` | Searching for `"دی"` or `"یکشنبه"` fails when the user's keyboard types `"دي"` or `"يكشنبه"` | Normalize `ي → ی`, `ى → ی`, and `ك → ک` on all text input |
| 22 | Showing Arabic-Indic digits (`٤٥٦`) instead of Persian digits (`۴۵۶`) | Looks noticeably wrong to Iranian and Afghan users | Use `fa-IR` / `arabext` (`U+06F0–U+06F9`) |
| 23 | Passing Persian digits straight to `parseInt('۱۴۰۴')` | Returns `NaN` in JavaScript! | Convert Persian and Arabic-Indic digits to ASCII `0–9` before parsing |
| 24 | Formatting a year with a general number formatter | Year `1404` renders with a thousands separator: `«۱٬۴۰۴»` | Pass `useGrouping: false` or use a date formatter |
| 25 | Writing Tuesday as `«سه شنبه»` (with a space) or `«سهشنبه»` (joined) | Broken typography | Use a Zero-Width Non-Joiner (`U+200C`): `«سه‌شنبه»` |
| 26 | Starting a Persian calendar grid on Sunday or Monday | Users misread which column is which day of the week | Start the week on **Saturday (شنبه)** in an RTL grid |
| 27 | Dropping a date directly into mixed RTL/LTR text without isolation | `1404/01/01` flips visually into `01/01/1404` next to Persian text | Wrap dates in `<bdi>` or Unicode `U+2068 … U+2069` isolates |
| 28 | Using `Intl.DateTimeFormat('fa-IR', { dateStyle: 'full' })` as-is | Outputs `«۱۴۰۴ فروردین ۱, جمعه»` (backwards order with an English comma) | Assemble `«جمعه ۱ فروردین ۱۴۰۴»` using `formatToParts` ([see page 5](05-localization.md)) |
| 29 | Sorting unpadded Persian date strings alphabetically | `"1404/1/9"` sorts *after* `"1404/1/10"` | Sort by the underlying Gregorian date / integer `14040109`, or zero-pad `YYYY/MM/DD` |

## 5. Database & API design

| # | The Bug | How it shows up in your app | How to fix it |
|---|---|---|---|
| 30 | Storing Persian date strings as your primary database column | Broken sorting, no native SQL date math, painful range queries | Store standard Gregorian `DATE` or UTC `TIMESTAMPTZ`; convert at the edges |
| 31 | Putting `"1404-01-01"` into an API field documented as ISO 8601 | Standard JSON/ISO parsers interpret it as 1 January 1404 CE (in the Middle Ages!) | Keep ISO fields strictly Gregorian (`"2025-03-21"`), and add a separate `"date_fa"` field if needed |
| 32 | Hard-coding holidays in source code | Religious holidays shift ~11 days every year, and laws change | Load holidays from a versioned data table ([see page 7](07-holidays.md)) |
| 33 | Using `islamic` or `islamic-umalqura` to compute Iranian religious holidays | Eid al-Fitr, Ashura, or Tasua are off by 1 day in some years | Use an Iranian lunar month-start table ([see page 7](07-holidays.md#the-lunar-hijri-problem)) |

---

[← Holidays](07-holidays.md) · [Next: Testing →](09-testing.md)
