# 6. Languages & libraries

[← Localization](05-localization.md) · [Next: Holidays →](07-holidays.md) · [فارسی](../fa/06-languages-and-libraries.md)

Here is a practical breakdown for 14 programming languages: what comes built into the platform, which third-party libraries we **actually tested** in this repository, and copy-pasteable code examples.

For the full testing methodology and raw output files, see [`tools/library-evaluation`](../../tools/library-evaluation). Every library below was tested against the astronomical ground-truth calendar across **1100–3300 AP**, and most were also checked on **every single day of 1300–1500 AP** in both directions.
(Wondering why some languages only list built-in APIs or this repo's code? See [`tools/library-evaluation`](../../tools/library-evaluation#why-not-every-language).)

## Summary of library test results

| Library / Built-in API | Version tested | Accurate range (AP) | Verdict & Notes |
|---|---|---|---|
| **ICU** (`Intl` in Node.js/browsers, ICU4C, ICU4J, PHP `intl`) | ICU4C 76.1, 77.1, 78.3, 79.1; ICU4J 76.1, 77.1, 78.1–78.3, 79.1 | ✅ **1178–3000** | ICU ≥ 77.1 uses the 78-year correction table; ICU 50–76 (including 76.1) uses the plain 33-year rule (accurate until 1502 AP) |
| **ICU4X** `icu_calendar` (Rust) | 2.4.0 | ✅ **1178–3000** | Uses the exact same 33-year + correction algorithm as ICU 77.1+ |
| **Temporal** (JavaScript) | Node.js 26.8 (V8, `temporal_rs`) | ✅ **1178–3000** | Powered by ICU4X (`temporal_rs`); every single day verified in both directions |
| **persiantools** (Python) | 6.2.0 | ✅ **1178–3000** | Every single day of 1300–1500 AP verified |
| **persian-calendar/calendar** (Kotlin) | 1.4.0 | ✅ **1178–3000** | Every day of 1304–1500 AP verified; intentionally models historical zodiac months before 1304 AP; licensed under **GPL-2.0+** |
| **jalaali-js** (JS) | 2.0.1 | ✅ Until 1502 | Every single day of 1300–1500 AP verified |
| **date-fns-jalali** (JS) | 4.4.0-0 | ✅ Until 1502 | Every single day of 1300–1500 AP verified |
| **moment-jalaali** (JS) | 0.10.5 | ✅ Until 1502 | Accurate, though Moment.js itself is deprecated/in maintenance mode |
| **jdatetime** (Python) | 6.1.0 | ✅ Until 1502 | Every single day of 1300–1500 AP verified |
| **morilog/jalali** (PHP) | 3.5.0 | ✅ Until 1502 | |
| **go-jalaali** (Go) | 0.1.0 | ✅ Until 1502 | |
| **go-persian-calendar** (`ptime`) (Go) | 1.3.0 | ✅ Until 1502 | |
| **samanzamani/PersianDate** (Java/Android) | 1.7.1 | ✅ Until 1502 | Every single day of 1300–1500 AP verified |
| **shamsi_date** (Dart/Flutter) | 1.1.1 | ✅ Until 1502 | Note: `addMonths` throws an exception if the target month has fewer days (e.g., 31 Shahrivar + 1 month) |
| **.NET** `System.Globalization.PersianCalendar` | .NET 8.0.31, .NET Framework 4.8 | ✅ Until 1535 | Built into .NET; default calendar for `fa-IR`; every day of 1300–1500 AP verified |
| **DNTPersianUtils.Core** (.NET) | 7.0.2 | ✅ Until 1535 | Wraps .NET's `PersianCalendar`; every day of 1300–1500 AP verified |
| **MD.PersianDateTime.Standard** (.NET) | 2.6.0 | ✅ Until 1535 | Wraps .NET's `PersianCalendar`; every day of 1300–1500 AP verified |
| **convertdate** (Python) | 2.5.1 | ⚠️ 1277–2378 | Accurate for modern dates, but wrong for older years like 1243, 1255, 1259… |
| **dayjs + jalaliday** (JS) | dayjs 1.11.23, jalaliday 3.1.1 | ❌ **Broken** | **Gregorian → Persian conversion is off by 1 day from 1 January to 29 February of every Gregorian leap year!** (2,940 wrong days in 1300–1500 AP; e.g., `2024-01-20` becomes `1402/11/01` instead of `1402/10/30`). Persian → Gregorian works |
| **khayyam** (Python) | 3.0.17 | ❌ **Broken** | Uses the 2820-year cycle: **wrong for 1403/1404**, 1436/1437, 1469/1470… |
| **parsi-date** (Ruby) | 0.5.1 | ❌ **Broken** | Wrong on leap years like 1383, **1408** (March 2029), 1412…; converts `2029-03-20` to `"1407/12/30"`, a date that its own `valid_date?` method rejects! |

**What do "Until 1502" and "Until 1535" mean in practice?**
* **"Until 1502"** means every single date is 100% accurate up through **29 Esfand 1502 (19 March 2124)**. The very first discrepancy happens at the end of 1502 AP, where the 33-year/Borkowski rule puts the leap year in 1502 instead of 1503 (see [Leap years](02-leap-years.md)).
* **"Until 1535"** (.NET) means every date matches up through **29 Esfand 1535 (19 March 2157)**. In 1536 AP, the spring equinox happens just 31 seconds before solar noon in Iran—a knife-edge astronomical call rather than a bug ([details](../../tools/calendar-data#how-certain-is-the-future)).
* For any date in our lifetime (this century), all ✅ libraries give identical, 100% accurate results.

**Bottom line:** Use your platform's built-in formatter (`Intl` / ICU / .NET / Foundation) for display, pick a ✅ library when you need date math or conversions, or copy [this repository's dependency-free implementation](../../implementations). Stay away from the ❌ libraries until they fix their algorithms.

---

## JavaScript / TypeScript

**Built in:** `Intl.DateTimeFormat` (for formatting and extracting date parts) and `Temporal` (for full calendar math, in modern runtimes).

```js
// 1. Format a date for display
new Intl.DateTimeFormat('fa-IR', { dateStyle: 'long', timeZone: 'Asia/Tehran' }).format(new Date())
// → "۱ فروردین ۱۴۰۴"

// 2. Extract numeric Persian (year, month, day) using Intl alone
const parts = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', {
  timeZone: 'Asia/Tehran', year: 'numeric', month: 'numeric', day: 'numeric',
}).formatToParts(new Date())
const get = (t) => Number(parts.find((p) => p.type === t).value)
const [jy, jm, jd] = [get('year'), get('month'), get('day')]

// 3. Temporal (Node.js 26+ and modern browsers): full two-way conversions and date math
const d = Temporal.PlainDate.from({ calendar: 'persian', year: 1403, month: 12, day: 30 })
d.withCalendar('iso8601').toString()   // "2025-03-20"
d.inLeapYear                           // true
Temporal.PlainDate.from({ calendar: 'persian', year: 1404, month: 6, day: 31 }).add({ months: 1 })
                                       // 30 Mehr 1404 (automatically clamped!)
```

Because `Intl.DateTimeFormat` is one-way (it can't convert a Persian date *back* to a Gregorian `Date`), use `Temporal` or one of these two libraries when you need two-way conversions:

```js
// Option A: jalaali-js (tiny, zero dependencies)
import { toJalaali, toGregorian, isLeapJalaaliYear, jalaaliMonthLength } from 'jalaali-js'
toJalaali(2025, 3, 21)        // { jy: 1404, jm: 1, jd: 1 }
toGregorian(1403, 12, 30)     // { gy: 2025, gm: 3, gd: 20 }

// Option B: date-fns-jalali (drop-in Persian equivalent of date-fns)
import { format, newDate, addMonths } from 'date-fns-jalali'
import { faIR } from 'date-fns-jalali/locale'
format(new Date(2025, 2, 21), 'EEEE d MMMM yyyy', { locale: faIR })  // "جمعه 1 فروردین 1404"
format(addMonths(newDate(1404, 5, 31), 1), 'yyyy/MM/dd')             // "1404/07/30" (note: monthIndex is 0-based!)
```

⚠️ **Watch out for 0-based months in JS:** Standard JavaScript `Date` (and `date-fns-jalali`'s `newDate(year, monthIndex, day)`) uses **0-based months** (`0 = January / Farvardin`, `2 = March`, `5 = Shahrivar`). In contrast, `Temporal`, `jalaali-js`, and this repository's implementations use natural **1-based months** (`1..12`).

Implementations in this repository: [`implementations/javascript`](../../implementations/javascript), [`implementations/typescript`](../../implementations/typescript).

## Python

**Built in:** Nothing in Python's standard library. Use **`persiantools`** (accurate up to 3000 AP) or **`jdatetime`** (accurate until 1502 AP):

```python
import datetime
from persiantools.jdatetime import JalaliDate      # pip install persiantools
JalaliDate(datetime.date(2025, 3, 21))              # JalaliDate(1404, 1, 1, Jomeh)
JalaliDate(1403, 12, 30).to_gregorian()             # datetime.date(2025, 3, 20)
JalaliDate(1404, 1, 1).strftime('%A %d %B %Y', locale='fa')   # "جمعه ۰۱ فروردین ۱۴۰۴"

import jdatetime                                     # pip install jdatetime
jdatetime.date.fromgregorian(date=datetime.date(2025, 3, 21))   # jdatetime.date(1404, 1, 1)
jdatetime.date(1403, 12, 30).togregorian()          # datetime.date(2025, 3, 20)
```

Do **not** use `khayyam`—it uses the 2820-year cycle and gets 1403/1404 wrong. (If you need full CLDR formatting in Python, `PyICU` wraps ICU4C just like the Java example below.)

Implementation in this repository: [`implementations/python`](../../implementations/python).

## Java

**Built in:** Nothing in the JDK (`java.time` includes `HijrahChronology` for the lunar calendar, but no Persian chronology).

**Recommended library:** **ICU4J** (`com.ibm.icu:icu4j`). Always instantiate it via `Calendar.getInstance` with a locale tag (direct use of the `PersianCalendar` class was deprecated in ICU4J 78):

```java
import com.ibm.icu.util.*;
import com.ibm.icu.text.DateFormat;

Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), new ULocale("fa_IR@calendar=persian"));
cal.setTime(new java.util.Date());
int year = cal.get(Calendar.YEAR), month = cal.get(Calendar.MONTH) + 1, day = cal.get(Calendar.DAY_OF_MONTH);

DateFormat f = DateFormat.getDateInstance(DateFormat.LONG, new ULocale("fa_IR@calendar=persian"));
f.setTimeZone(TimeZone.getTimeZone("Asia/Tehran"));
f.format(new java.util.Date());   // "۱ فروردین ۱۴۰۴"
```

Zero-dependency alternative: [`implementations/java`](../../implementations/java) (integrates directly with `java.time.LocalDate`).

## Kotlin / Android

* **Android (API 24+ / Android 7.0+):** ICU is built right into the OS under `android.icu`:
  `android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))`.
  Note that the bundled ICU version depends on the user's Android OS version—older phones use the plain 33-year rule, which is still 100% accurate until 1502 AP (March 2124).
* **Zero-dependency Kotlin:** [`implementations/kotlin`](../../implementations/kotlin) provides a pure Kotlin core plus `java.time.LocalDate` helpers.
* **[persian-calendar/calendar](https://github.com/persian-calendar/calendar):** The calendar engine behind the popular Iranian Android calendar app.
  It combines the official leap-year table with astronomical calculations, and even supports pre-1304 historical zodiac months.
  ⚠️ **License note:** It is licensed under **GPL-2.0-or-later**, so make sure that is compatible with your app's license before adding it.

## Scala

**Built in:** Nothing in the standard library; you can use ICU4J or this repository's native Scala 3 implementation at [`implementations/scala`](../../implementations/scala), which works directly with `java.time.LocalDate`:

```scala
import persiancalendar.*
PersianCalendar.fromLocalDate(java.time.LocalDate.of(2025, 3, 21))   // PersianDate(1404, 1, 1)
PersianCalendar.toLocalDate(1403, 12, 30)                             // 2025-03-20
PersianCalendar.isLeapYear(1403)                                      // true
```

## C# / .NET

**Built in:** `System.Globalization.PersianCalendar` comes built into the .NET BCL (accurate until 1535 AP) and is the default calendar for the `fa-IR` culture:

```csharp
using System.Globalization;
var pc = new PersianCalendar();
var d = new DateTime(2025, 3, 21);
(pc.GetYear(d), pc.GetMonth(d), pc.GetDayOfMonth(d));   // (1404, 1, 1)
pc.ToDateTime(1403, 12, 30, 0, 0, 0, 0);                // 2025-03-20
pc.IsLeapYear(1403);                                     // true
pc.AddMonths(pc.ToDateTime(1404, 6, 31, 0, 0, 0, 0), 1); // 30 Mehr 1404 (clamped)

var fa = new CultureInfo("fa-IR");                       // Calendar = PersianCalendar, FirstDayOfWeek = Saturday
d.ToString("yyyy/MM/dd", fa);                            // "1404/01/01"
d.ToString("dddd d MMMM yyyy", fa);                      // "جمعه 1 فروردین 1404"
```

**Good to know:** .NET's `ToString(..., fa)` outputs ASCII digits (`0–9`) rather than Persian digits (`۰–۹`). Also, `TimeZoneInfo.FindSystemTimeZoneById("Asia/Tehran")` works cross-platform on .NET 6+ (on older .NET Framework on Windows, the zone ID was `"Iran Standard Time"`).

Implementation in this repository: [`implementations/csharp`](../../implementations/csharp) (includes `DateOnly` helpers and full 1178–3000 AP support).

## Go

**Built in:** Nothing in Go's standard `time` package. Two solid third-party libraries are **`go-jalaali`** and **`go-persian-calendar` (`ptime`)**:

```go
import (
    "time"

    jalaali "github.com/jalaali/go-jalaali"
    ptime "github.com/yaa110/go-persian-calendar"
)
gy, gm, gd, _ := jalaali.ToGregorian(1403, jalaali.Esfand, 30)   // 2025 March 20
jy, jm, jd, _ := jalaali.ToJalaali(2025, time.March, 21)          // 1404 Farvardin 1

p := ptime.New(time.Now().In(ptime.Iran()))
p.Year(); p.Month(); p.Day()
```

Implementation in this repository: [`implementations/go`](../../implementations/go) (includes `time.Time` helpers).

## Rust

**Recommended crate:** Unicode's official **ICU4X** (`icu_calendar`), which implements the exact same 33-year + 78-year correction algorithm:

```rust
use icu_calendar::cal::Persian;
use icu_calendar::{Date, Gregorian};

let p = Date::try_new_iso(2025, 3, 21)?.to_calendar(Persian);
(p.era_year().year, p.month().ordinal, p.day_of_month().0);      // (1404, 1, 1)
let g = Date::try_new_persian(1403, 12, 30)?.to_calendar(Gregorian); // 2025-03-20
Date::try_new_persian(1404, 12, 30).is_err();                     // true
```

For localized date formatting in Rust, pair it with `icu::datetime` using locale `fa-IR`.
Zero-dependency alternative: [`implementations/rust`](../../implementations/rust).

## PHP

**Built in:** PHP's **`intl` extension** (powered by ICU):

```php
$fmt = new IntlDateFormatter('fa_IR@calendar=persian', IntlDateFormatter::FULL, IntlDateFormatter::NONE,
    'Asia/Tehran', IntlDateFormatter::TRADITIONAL, 'EEEE d MMMM y');
echo $fmt->format(new DateTimeImmutable('now', new DateTimeZone('Asia/Tehran')));  // جمعه ۱ فروردین ۱۴۰۴

$num = new IntlDateFormatter('fa_IR@calendar=persian;numbers=latn', IntlDateFormatter::SHORT,
    IntlDateFormatter::NONE, 'Asia/Tehran', IntlDateFormatter::TRADITIONAL, 'yyyy/MM/dd');
$ts = $num->parse('1404/01/01');   // Unix timestamp of 00:00 in Tehran (= 2025-03-20 20:30 UTC!)
(new DateTimeImmutable("@$ts"))->setTimezone(new DateTimeZone('Asia/Tehran'))->format('Y-m-d');  // 2025-03-21
```

⚠️ **Watch out when using `$fmt->parse()`:** It returns a Unix timestamp representing **midnight in the formatter's time zone** (`2025-03-20 20:30:00 UTC`). If you format that timestamp back in `UTC` instead of `Asia/Tehran`, you'll get the previous day (`2025-03-20`)!
*(Also note: PHP 8.5 deprecates calling `IntlCalendar::set()` with 3 or more arguments; use `IntlCalendar::setDate()` instead.)*

**Popular Laravel / Carbon library — `morilog/jalali`:**

```php
use Morilog\Jalali\Jalalian;
Jalalian::fromDateTime(new DateTimeImmutable('2025-03-21'))->format('Y/m/d');  // 1404/01/01
Jalalian::fromFormat('Y/m/d', '1404/01/01')->toCarbon();                        // 2025-03-21
```

Implementation in this repository: [`implementations/php`](../../implementations/php).

## Ruby

Unfortunately, the popular `parsi-date` gem (v0.5.1) gets leap years wrong starting in 1408 AP (March 2029).
We recommend copying [`implementations/ruby`](../../implementations/ruby) (which integrates cleanly with Ruby's standard `Date#jd`), or thoroughly testing any gem against `data/years.csv` before using it.

## Swift / Apple platforms (iOS, macOS)

**Built in:** Apple's `Foundation` framework includes ICU-backed Persian calendar support out of the box (not tested in our Linux CI):

```swift
var cal = Calendar(identifier: .persian)
cal.timeZone = TimeZone(identifier: "Asia/Tehran")!
cal.locale = Locale(identifier: "fa_IR")
let c = cal.dateComponents([.year, .month, .day, .weekday], from: Date())

let f = DateFormatter()
f.calendar = cal
f.locale = Locale(identifier: "fa_IR")
f.dateFormat = "EEEE d MMMM y"
f.string(from: Date())   // "جمعه ۱ فروردین ۱۴۰۴"
```

## Dart / Flutter

Dart's official `intl` package only supports the Gregorian calendar. In Flutter and Dart projects, use **`shamsi_date`**:

```dart
import 'package:shamsi_date/shamsi_date.dart';
final j = Jalali.fromDateTime(DateTime(2025, 3, 21));   // 1404/1/1
final g = Jalali(1403, 12, 30).toGregorian();           // 2025-03-20
final f = j.formatter;
'${f.wN} ${f.d} ${f.mN} ${f.yyyy}';                     // جمعه 1 فروردین 1404
```

⚠️ **Watch out:** Calling `Jalali(1404, 6, 31).addMonths(1)` in `shamsi_date` throws a `DateException` because month 7 (Mehr) only has 30 days. Make sure to clamp the day of the month yourself when doing month arithmetic.
Implementation in this repository: [`implementations/dart`](../../implementations/dart).

## C / C++

C and C++ have no Persian calendar in their standard libraries (though if your project already links against **ICU4C**, you can use `icu::Calendar::createInstance(icu::Locale("fa_IR@calendar=persian"), status)`).
For a tiny, dependency-free C99 implementation that also compiles cleanly as C++, grab [`implementations/c`](../../implementations/c).

## Databases

| Database | How to handle Persian dates |
|---|---|
| **PostgreSQL** | Use [`implementations/sql/postgresql.sql`](../../implementations/sql/postgresql.sql): gives you `persian_from_date()`, `persian_to_date()`, and `persian_format()`. All functions are marked `IMMUTABLE`, so you can use them in indexes and generated columns |
| **SQL Server** | `FORMAT(@d, 'yyyy/MM/dd', 'fa-IR')` uses .NET's built-in `PersianCalendar` under the hood (not tested in this repo) |
| **MySQL / MariaDB / SQLite** | No built-in Persian calendar. Convert dates in your application layer, or join against a pre-populated date dimension table (`dim_date`) generated with any implementation in this repository |
| **All databases** | Always store standard Gregorian `DATE` or UTC `TIMESTAMPTZ` as your source of truth, and convert at the edges ([see Implementation guide](04-implementation-guide.md#step-2-keep-one-source-of-truth--store-gregorianinstants)) |

---

[← Localization](05-localization.md) · [Next: Holidays →](07-holidays.md)
