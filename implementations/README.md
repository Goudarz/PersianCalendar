# Implementations · پیاده‌سازی‌ها

Ready-to-use implementations of the algorithm from [docs/en/03-algorithm.md](../docs/en/03-algorithm.md) ([فارسی](../docs/fa/03-algorithm.md)) across 14 programming languages.
Every implementation has zero external dependencies, is licensed under MIT, and is thoroughly tested against [`/data`](../data). Simply copy the file for your language **along with its test file** into your project.

<div dir="rtl">

پیاده‌سازی الگوریتم [بخش ۳ راهنما](../docs/fa/03-algorithm.md) برای ۱۴ زبان برنامه‌نویسی؛ همه بدون وابستگی خارجی (Dependency-free)، با مجوز MIT و تست‌شده با داده‌های مرجع [`/data`](../data).
کافیه فایل زبان مورد نظرتون رو **همراه با فایل تستش** توی پروژهٔ خودتون کپی کنید.

</div>

| Language | Code | Test | Run the test (from that folder) | Tested with |
|---|---|---|---|---|
| Python 3.8+ | [persian_calendar.py](python/persian_calendar.py) | [test](python/test_persian_calendar.py) | `python -m unittest` | Python 3.13 |
| JavaScript (ESM) | [persian-calendar.js](javascript/persian-calendar.js) | [test](javascript/persian-calendar.test.js) | `node --test` | Node.js 26 |
| TypeScript | [persian-calendar.ts](typescript/persian-calendar.ts) | [test](typescript/persian-calendar.test.ts) | `node --test` (Node 23.6+) | Node.js 26, tsc strict |
| Java 16+ | [PersianCalendar.java](java/PersianCalendar.java) | [test](java/PersianCalendarTest.java) | `javac -d out *.java && java -cp out PersianCalendarTest` | Java 26 |
| Kotlin (JVM/Android, KMP core) | [PersianCalendar.kt](kotlin/PersianCalendar.kt) | [test](kotlin/PersianCalendarTest.kt) | `kotlinc *.kt -include-runtime -d t.jar && java -jar t.jar` | Kotlin 2.4 |
| Scala 3 | [PersianCalendar.scala](scala/PersianCalendar.scala) | [test](scala/PersianCalendarTest.scala) | `scala run . --main-class persiancalendar.PersianCalendarTest -- ../../data` | Scala 3.9 |
| C# (.NET 6+) | [SolarHijri.cs](csharp/SolarHijri.cs) | [test](csharp/tests/Program.cs) | `cd tests && dotnet run` | .NET 8 |
| Go 1.21+ | [persiancalendar.go](go/persiancalendar.go) | [test](go/persiancalendar_test.go) | `go test ./...` | Go 1.27 |
| Rust 2024 (1.85+) | [src/lib.rs](rust/src/lib.rs) | [test](rust/tests/data.rs) | `cargo test --release` | Rust 1.98 |
| PHP 8.1+ | [PersianCalendar.php](php/PersianCalendar.php) | [test](php/test.php) | `php test.php` | PHP 8.5 |
| Dart 3 / Flutter | [lib/persian_calendar.dart](dart/lib/persian_calendar.dart) | [test](dart/test/persian_calendar_test.dart) | `dart pub get && dart test` | Dart 3.13 |
| C99 / C++ | [persian_calendar.h](c/persian_calendar.h), [.c](c/persian_calendar.c) | [test](c/test.c) | `cc -std=c99 -O2 persian_calendar.c test.c -o t && ./t` | Zig 0.16 (Clang), also as C++17 |
| Ruby 3+ | [persian_calendar.rb](ruby/persian_calendar.rb) | [test](ruby/test_persian_calendar.rb) | `ruby test_persian_calendar.rb` | Ruby 4.0 |
| PostgreSQL | [postgresql.sql](sql/postgresql.sql) | [test](sql/test-postgresql.mjs) | `npm install && npm test` | PostgreSQL 17 (PGlite) |

## What is verified for each language

Every implementation passes **all** of the following checks in its own test suite (which you can run with the command in the table above):

| # | Test | Scope |
|---|---|---|
| 1 | Every single day of 1178–3000 AP compared against the astronomical calendar (`data/years.csv`), in both directions | 665,837 days, 1,823 years |
| 2 | Leap-year flag, Nowruz date, and Nowruz weekday for every year in 1178–3000 AP | 1,823 years |
| 3 | Gregorian ↔ Persian boundary test vectors (`data/test-vectors.csv`), in both directions | 8,510 dates |
| 4 | Every day of −1000…1177 and 3001…4000 AP round-trips and steps consecutively (exercising negative intermediate values) | 1.16 million days |
| 5 | **Every year across the full accepted range (−100000…100000):** years are contiguous, the first and last day of each year round-trip, and two exact checksums match (48,485 leap years; Nowruz JDN sum = 389,592,978,180)—matching ICU 77.1+, ICU4X, and Temporal | 200,001 years |
| 6 | Date validation: 30 Esfand accepted only in leap years, months 7–12 capped at 30 days, month/day 0 and month 13 rejected | – |
| 7 | Input bounds & type safety: exact first and last accepted days; rejection of 1 day outside the range, huge values (2⁵³ or `MAX_INT`), and non-integers (in dynamic languages). Invalid Gregorian dates (`2025-02-29`, `1900-02-29`) are rejected wherever the API accepts raw `(gy, gm, gd)` numbers (Python, JS/TS, Rust, PHP, C); in the remaining languages, the platform's own date type (`LocalDate`, `DateOnly`, `Date`) enforces Gregorian validity | – |
| 8 | Well-known historical dates (`2025-03-21` = 1 Farvardin 1404, `1979-02-11` = 22 Bahman 1357), plus time-zone tests wherever the API accepts an instant (JS/TS, Go, PHP, PostgreSQL: `2025-03-20T21:00Z` is 1 Farvardin 1404 in Tehran, but still 30 Esfand 1403 in UTC) | – |

On top of that, each language is **cross-checked day by day across 1178–3000 AP against an independent calendar engine**
([`tools/icu-cross-check`](../tools/icu-cross-check)), and tested against language-specific edge cases:

| Language | Tested with | Cross-checked against | Language-specific checks |
|---|---|---|---|
| Ruby | 4.0 | ICU4C 76.1–79.1 (`Fiddle`) | `TypeError` for `Float`/`String`, frozen tables, `Bignum` inputs |
| Python | 3.13 | ICU4C 76.1–79.1 (`ctypes`), every year of the full range | `TypeError` for non-integers, `bool` rejected (`isinstance(True, int)`) |
| JavaScript | Node.js 23, 24, 26 | ICU4C (`Intl`), Temporal; Temporal on every year of the full range | `Object.freeze` tables, bounded `Intl` cache, dates before 1 CE |
| TypeScript | Node.js 26, `tsc --strict` | ICU4C (`Intl`), Temporal | Same tests as JavaScript + strict type checking |
| Java | 26 | ICU4J 76.1–79.1, every year of the full range | `-Xlint:all -Werror`, unmodifiable `List`s, `LocalDate.MIN/MAX` |
| Kotlin | 2.4 (JVM) | ICU4J 76.1–79.1 | Android-compatible (no API 30+ calls), `-Werror` |
| Scala | 3.9 | ICU4J 76.1–79.1 | `LocalDate.MAX` boundary checks |
| C# | .NET 8 | ICU4C 76.1–79.1 (P/Invoke); .NET `PersianCalendar` (diverges from 1535) | Warnings as errors, read-only collections |
| Go | 1.27, amd64 **and 386** | ICU4C 76.1–79.1 (`purego`, no cgo) | 32-bit `int` build, `FromTime` for distant timestamps, `go vet` |
| Rust | 1.98, edition 2024 | ICU4X 2.4 on **every day of the full range** (73,048,850 days) | Full 73M-day sweep in unit test, debug build overflow checks, `clippy` |
| PHP | 8.5 | ICU4C 77.1 (`intl`) | `strict_types=1`, distant `DateTime` objects, negative years |
| Dart | 3.13, VM **and JavaScript** | ICU4C 76.1–79.1 (`dart:ffi`) | Web (JS) integer semantics, `dart analyze --fatal-infos` |
| C / C++ | C99 and C++11/17 (Clang/Zig, GCC in CI) | ICU4C 76.1–79.1 (`dlopen` / `LoadLibrary`) | `-Wconversion -Werror`, UBSan + ASan, 32-bit x86, `INT64_MIN/MAX`, `NULL` output pointers |
| PostgreSQL | 17 (PGlite) | ICU4C (`Intl`) and Temporal, every day | `search_path` injection safety, empty `search_path` (`pg_dump`), indexes, `NULL`, `±infinity` |

<div dir="rtl">

### چه چیزهایی برای هر زبان تست شده؟

هر پیاده‌سازی فایل تست مخصوص خودش رو داره (با دستور جدول بالا اجرا می‌شه) و **تمام** این موارد رو بررسی می‌کنه:

| # | مورد تست | حجم تست |
|---|---|---|
| ۱ | مقایسهٔ تک‌تک روزهای سال ۱۱۷۸ تا ۳۰۰۰ با تقویم نجومی (`data/years.csv`) در هر دو جهت | ۶۶۵٬۸۳۷ روز، ۱٬۸۲۳ سال |
| ۲ | تشخیص سال کبیسه، تاریخ نوروز و روز هفتهٔ اول سال برای تمام سال‌های ۱۱۷۸ تا ۳۰۰۰ | ۱٬۸۲۳ سال |
| ۳ | نمونه‌های تبدیل تاریخ میلادی ↔ شمسی (`data/test-vectors.csv`) در هر دو جهت | ۸٬۵۱۰ تاریخ |
| ۴ | تبدیل دوطرفه و پیوستگی تک‌تک روزهای سال‌های ۱۰۰۰− تا ۱۱۷۷ و ۳۰۰۱ تا ۴۰۰۰ (برای تست مقادیر منفی در محاسبات میانی) | ۱٫۱۶ میلیون روز |
| ۵ | **تمام سال‌های بازهٔ مجاز (۱۰۰٬۰۰۰− تا ۱۰۰٬۰۰۰)**: پیوسته بودن سال‌ها، تبدیل دوطرفهٔ روز اول و آخر هر سال، و تطابق دو جمع کنترلی (۴۸٬۴۸۵ سال کبیسه؛ مجموع JDN نوروزها برابر با ۳۸۹٬۵۹۲٬۹۷۸٬۱۸۰) - دقیقا همون عددهایی که ICU 77.1 به بعد، ICU4X و Temporal می‌دن | ۲۰۰٬۰۰۱ سال |
| ۶ | اعتبارسنجی تاریخ‌ها: قبول کردن ۳۰ اسفند فقط در سال کبیسه، ۳۰ روزه بودن ماه‌های ۷ تا ۱۲، و رد کردن ماه یا روز ۰ و ماه ۱۳ | – |
| ۷ | کنترل محدوده و نوع ورودی: بررسی دقیق اولین و آخرین روز مجاز؛ رد کردن حتی یک روز خارج از بازه، اعداد خیلی بزرگ (۲⁵³ یا بزرگ‌ترین عدد صحیح هر زبان) و ورودی‌های غیرصحیح (در زبان‌های پویا). در زبان‌هایی که سال/ماه/روز میلادی رو به شکل عدد می‌گیرن (Python، JS/TS، Rust، PHP، C) تاریخ‌های میلادی نامعتبر مثل `2025-02-29` و `1900-02-29` هم رد می‌شن؛ در بقیهٔ زبان‌ها خود تایپ تاریخ پلتفرم (`LocalDate`، `DateOnly`، `Date`) جلوی این خطا رو می‌گیره | – |
| ۸ | تاریخ‌های شناخته‌شده (`2025-03-21` = ۱ فروردین ۱۴۰۴، `1979-02-11` = ۲۲ بهمن ۱۳۵۷) و بررسی تایم‌زون در زبان‌هایی که یک لحظهٔ زمانی ورودی می‌گیرن (JS/TS، Go، PHP، PostgreSQL: مثلا لحظهٔ `2025-03-20T21:00Z` به وقت تهران می‌شه ۱ فروردین ۱۴۰۴، ولی به وقت UTC هنوز ۳۰ اسفند ۱۴۰۳ هست) | – |

علاوه بر این، خروجی هر زبان برای تک‌تک روزهای سال ۱۱۷۸ تا ۳۰۰۰ در هر دو جهت **با یک موتور تقویم مستقل مقایسه می‌شه**
([`tools/icu-cross-check`](../tools/icu-cross-check)) و خطاهای احتمالی خاص هر زبان هم جداگانه چک می‌شه:

| زبان | نسخهٔ تست‌شده | مقایسه شده با | بررسی‌های اختصاصی هر زبان |
|---|---|---|---|
| Ruby | 4.0 | ICU4C نسخهٔ 76.1 تا 79.1 (`Fiddle`) | خطای `TypeError` برای `Float` و `String`، جدول‌های فریز شده، ورودی `Bignum` |
| Python | 3.13 | ICU4C نسخهٔ 76.1 تا 79.1 (`ctypes`)، همهٔ سال‌های کل بازه | خطای `TypeError` برای ورودی غیرصحیح، رد کردن `bool` |
| JavaScript | Node.js 23، 24، 26 | ICU4C (`Intl`) و Temporal؛ Temporal روی همهٔ سال‌های کل بازه | جدول‌های تغییرناپذیر (`Object.freeze`)، کش محدود `Intl`، تاریخ‌های قبل از میلاد |
| TypeScript | Node.js 26، `tsc --strict` | ICU4C (`Intl`) و Temporal | تمام تست‌های JavaScript + بررسی سخت‌گیرانهٔ تایپ‌ها |
| Java | 26 | ICU4J نسخهٔ 76.1 تا 79.1، همهٔ سال‌های کل بازه | کامپایل با `-Xlint:all -Werror`، لیست‌های تغییرناپذیر، تست `LocalDate.MIN/MAX` |
| Kotlin | 2.4 (JVM) | ICU4J نسخهٔ 76.1 تا 79.1 | سازگار با نسخه‌های قدیمی‌تر اندروید (بدون نیاز به API 30)، `-Werror` |
| Scala | 3.9 | ICU4J نسخهٔ 76.1 تا 79.1 | تست `LocalDate.MAX` |
| C# | ‎.NET 8 | ICU4C نسخهٔ 76.1 تا 79.1 (P/Invoke)؛ `PersianCalendar` در ‎.NET (که از ۱۵۳۵ خطا داره) | تبدیل هشدارهای کامپایلر به خطا، لیست‌های فقط‌خواندنی |
| Go | 1.27، amd64 **و 386** | ICU4C نسخهٔ 76.1 تا 79.1 (`purego`، بدون نیاز به cgo) | تست با `int` سی‌ودوبیتی، تابع `FromTime` برای تاریخ‌های خیلی دور، `go vet` |
| Rust | 1.98، ویرایش 2024 | ICU4X 2.4 روی **تک‌تک روزهای کل بازه** (۷۳٬۰۴۸٬۸۵۰ روز) | تست تک‌تک روزهای کل بازه، بیلد debug (بررسی سرریز اعداد) و `clippy` |
| PHP | 8.5 | ICU4C 77.1 (`intl`) | فعال بودن `strict_types`، تست `DateTime` برای تاریخ‌های خیلی دور و سال‌های منفی |
| Dart | 3.13، VM **و خروجی وب (JS)** | ICU4C نسخهٔ 76.1 تا 79.1 (`dart:ffi`) | رفتار اعداد صحیح در خروجی وب، `dart analyze --fatal-infos` |
| C / C++ | C99 و C++11/17 (Clang/Zig، و GCC در CI) | ICU4C نسخهٔ 76.1 تا 79.1 (`dlopen` / `LoadLibrary`) | `-Wconversion -Werror`، تست با UBSan و ASan، معماری ۳۲ بیتی x86، `INT64_MIN/MAX` و خروجی `NULL` |
| PostgreSQL | 17 (PGlite) | ICU4C (`Intl`) و Temporal روی تک‌تک روزها | امنیت در برابر دستکاری `search_path`، کار با `search_path` خالی (`pg_dump`)، ایندکس‌ها، `NULL` و `infinity` |

</div>

## Common API · توابع مشترک

| Operation | Python / Ruby / Rust / C | JavaScript / TS / Java / Kotlin / Scala / Dart / PHP | C# | Go |
|---|---|---|---|---|
| Leap year check | `is_leap_year(y)` (Ruby: `leap_year?`) | `isLeapYear(y)` | `IsLeapYear` | `IsLeapYear` |
| Days in month | `days_in_month(y, m)` | `daysInMonth(y, m)` | `DaysInMonth` | `DaysInMonth` |
| Date validation | `is_valid_date(y, m, d)` | `isValidDate(y, m, d)` | `IsValidDate` | `IsValid` |
| Persian → JDN | `to_jdn(y, m, d)` | `toJdn(y, m, d)` | `ToJdn` | `ToJdn` |
| JDN → Persian | `from_jdn(jdn)` | `fromJdn(jdn)` | `FromJdn` | `FromJdn` |
| Gregorian conversion | `to_gregorian` / `from_gregorian` | `toGregorian` / `fromGregorian`, or native date type* | `ToDateOnly` / `FromDateOnly` | `ToTime` / `FromTime` |
| Day of week | `day_of_week(y, m, d)` → `0 = Saturday … 6 = Friday` | `dayOfWeek` | `DayOfWeek` | `Weekday` |
| Day of year | `day_of_year(y, m, d)` → `1…366` | `dayOfYear` | `DayOfYear` | `DayOfYear` |

PostgreSQL uses the same function names with a `persian_` prefix (`persian_is_leap_year`, `persian_days_in_month`, `persian_is_valid`, `persian_to_jdn`, `persian_from_jdn`, `persian_to_date`/`persian_from_date`, `persian_day_of_week(date)`, `persian_day_of_year`, `persian_format`); C also prefixes functions with `persian_` and includes `persian_day_of_week_jdn`.

\* Native date helpers: Java/Kotlin/Scala provide `toLocalDate`/`fromLocalDate`; Dart provides `toDateTimeUtc`/`fromDateTime`; Ruby provides `to_date`/`from_date`; PHP adds `fromDateTime`; JS/TS add `fromDate(date, timeZone)`.

In every language, months are **1-based** (`1 = Farvardin … 12 = Esfand`). Invalid dates raise an exception (or return `0` in C, `None` in Rust, and an `error` in Go).

<div dir="rtl">

### راهنمای توابع (API مشترک)

تمام ۱۴ زبان دقیقا همین مجموعه توابع جدول بالا رو (متناسب با قواعد نام‌گذاری همون زبان، مثل `snake_case`، `camelCase` یا `PascalCase`) در اختیارتون می‌ذارن:

* در **PostgreSQL** و **C**، یک پیشوند `persian_` اول نام توابع قرار می‌گیره (مثل `persian_to_date`، `persian_from_date` و `persian_format`).
* در زبان‌هایی که تایپ تاریخ استاندارد دارن، علاوه بر تبدیل عددی، متدهای کمکی مستقیم هم گذاشته شده: `toLocalDate`/`fromLocalDate` در جاوا، کاتلین و اسکالا؛ `ToDateOnly`/`FromDateOnly` در سی‌شارپ؛ `to_date`/`from_date` در روبی؛ `fromDateTime` در دارت و PHP؛ و `fromDate(date, timeZone)` در جاوااسکریپت و تایپ‌اسکریپت.
* شمارهٔ ماه‌ها در **تمام** پیاده‌سازی‌ها از **۱ تا ۱۲** است (`1` = فروردین تا `12` = اسفند) و روز هفته همیشه از **۰ = شنبه تا ۶ = جمعه** برگردونده می‌شه.

</div>

## Input limits and errors · محدودهٔ ورودی و مدیریت خطاها

Every implementation defines the same range constants:

| Constant | Value | Meaning |
|---|---|---|
| `VERIFIED_MIN_YEAR` … `VERIFIED_MAX_YEAR` | `1178 … 3000` | Years verified day by day against the astronomical calendar |
| `MIN_YEAR` … `MAX_YEAR` | `−100000 … 100000` | Accepted Persian year range |
| `MIN_JDN` … `MAX_JDN` | `−34576287 … 38472562` | Accepted JDN range (`1 Farvardin −100000` … `29 Esfand 100000`) |

*(Constant names follow each language's naming convention: `VerifiedMinYear`, `verifiedMinYear`, `PERSIAN_VERIFIED_MIN_YEAR`, etc.)*

Within the accepted range, every intermediate calculation fits comfortably in a 32-bit signed integer—so results are 100% exact everywhere, including 32-bit builds (C, Go, PHP) and IEEE-754 double-precision numbers (JavaScript, TypeScript, and Dart compiled to JS). Outside that range, functions never overflow, hang, or return bogus dates:

| Language | Out-of-range input | Non-integer input | Invalid calendar date |
|---|---|---|---|
| Python | `ValueError` | `TypeError` | `ValueError` |
| JavaScript / TypeScript | `RangeError` | `TypeError` | `RangeError` |
| Ruby | `RangeError` | `TypeError` | `ArgumentError` |
| Java / Kotlin / Scala | `IllegalArgumentException` | (compile-time type error) | `IllegalArgumentException` |
| C# | `ArgumentOutOfRangeException` | (compile-time type error) | `ArgumentException` |
| PHP | `InvalidArgumentException` | `TypeError` (`strict_types`) | `InvalidArgumentException` |
| Dart | `RangeError` | (compile-time type error) | `ArgumentError` |
| Rust | `None` | (compile-time type error) | `None` |
| Go | `error` | (compile-time type error) | `error` |
| C | Returns `0`, output struct untouched | (compile-time type error) | Returns `0` |
| PostgreSQL | SQLSTATE `22008` | (type error) | SQLSTATE `22008` |

* `is_valid_date` never throws—it simply returns `false` for out-of-range or invalid inputs.
* In Rust, Go, and C, `is_leap_year` returns a plain boolean and is safe for any integer of its parameter type without overflowing; in all other languages, it rejects years outside `MIN_YEAR … MAX_YEAR` just like the other functions.
* Gregorian inputs are strictly validated too (passing `2025-02-30` raises an error instead of silently rolling over to `2025-03-02`).
* All exported tables (month names, correction years) are immutable/frozen so callers cannot accidentally mutate them.

<div dir="rtl">

### محدودهٔ ورودی و مدیریت خطاها در فارسی

در تمام زبان‌ها این سه بازه به صورت ثابت (Constant) تعریف شدن:

* **`VERIFIED_MIN_YEAR` تا `VERIFIED_MAX_YEAR` (`1178` تا `3000`):** بازه‌ای که روزبه‌روز با محاسبات نجومی تطبیق داده شده است.
* **`MIN_YEAR` تا `MAX_YEAR` (`-100000` تا `100000`):** بازهٔ سال‌های مجاز ورودی.
* **`MIN_JDN` تا `MAX_JDN` (`-34576287` تا `38472562`):** بازهٔ شمارهٔ روز جولیانی مجاز (از ۱ فروردین ۱۰۰٬۰۰۰− تا ۲۹ اسفند ۱۰۰٬۰۰۰).

توی این بازه، تمام اعداد میانی فرمول‌ها زیر ۳۲ بیت باقی می‌مونن؛ بنابراین چه روی سیستم‌های ۳۲ بیتی (در C، Go و PHP) و چه با اعداد جاوااسکریپت و خروجی وب دارت، محاسبات کاملا دقیق و بدون سرریز انجام می‌شن. برای هر ورودی خارج از این بازه هم به جای سرریز شدن، گیر کردن در حلقه یا برگردوندن تاریخ اشتباه، خطای مشخص و استاندارد همون زبان (مطابق جدول بالا) برگردونده می‌شه:

* تابع `is_valid_date` هیچ‌وقت استثنا (Exception) پرتاب نمی‌کنه و برای ورودی‌های نامعتبر یا خارج از بازه فقط `false` برمی‌گردونه.
* ورودی‌های میلادی هم کامل اعتبارسنجی می‌شن (مثلا `2025-02-30` خطا می‌ده، نه اینکه بی‌سروصدا به `2025-03-02` تبدیل بشه).
* تمام آرایه‌ها و جدول‌های خروجی (مثل نام ماه‌ها و لیست ۷۸ سال اصلاحی) به صورت تغییرناپذیر (Immutable / Frozen) تعریف شدن تا کدهای بیرونی نتونن اشتباهی تغییرشون بدن.

</div>
