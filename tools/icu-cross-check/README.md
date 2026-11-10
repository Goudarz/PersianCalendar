# ICU / ICU4X cross-check · تطبیق متقابل با موتورهای ICU و ICU4X

This test suite compares three independent sources **across every single day of 1178–3000 AP** (665,837 days, 1,823 years):

1. **Astronomical ground truth:** [`data/years.csv`](../../data/years.csv), which matches the official University of Tehran Calendar Center table;
2. **Our implementations:** all 14 language [implementations](../../implementations) in this repository;
3. **Every flavor of Unicode ICU:**
   * **ICU4X** in Rust (`icu_calendar` and `calendrical_calculations`),
   * **ICU4J** on the JVM (called from Java, Kotlin, and Scala), and
   * **ICU4C**, tested through JavaScript/TypeScript (`Intl`), PHP (`intl`), and directly via its C FFI from **Python** (`ctypes`), **Ruby** (`Fiddle`), **Go** (`purego`, without cgo), **C#** (P/Invoke), **Dart** (`dart:ffi`), and **C** (`dlopen` / `LoadLibrary`).

Where available, two additional independent engines are verified in the same run: **ECMAScript `Temporal`** (which in Node.js 26 / V8 is powered by `temporal_rs` and ICU4X rather than ICU4C) and **.NET's `System.Globalization.PersianCalendar`** (pure managed code, independent of ICU).
Since PostgreSQL has no built-in Persian calendar, our SQL functions are executed on an embedded PostgreSQL engine (`PGlite`) and cross-checked day by day against ICU and Temporal.

* **ICU 77.1 and newer** must match the astronomical ground truth on every single day in both directions.
* **ICU 76.1 and older** do not have the 78-year correction table, so our scripts verify that they match the **plain 33-year rule** on every single day—proving and documenting the exact behavior of older runtimes.

<div dir="rtl">

این مجموعه تست، برای **تک‌تک روزهای سال ۱۱۷۸ تا ۳۰۰۰ ه‍.ش** (۶۶۵٬۸۳۷ روز در ۱٬۸۲۳ سال)، سه مرجع مستقل رو در هر دو جهت (شمسی به میلادی و میلادی به شمسی) با هم مقایسه می‌کنه:

1. **تاریخ درست (مرجع نجومی):** فایل [`data/years.csv`](../../data/years.csv) که با جدول رسمی مرکز تقویم دانشگاه تهران مطابقت کامل داره؛
2. **پیاده‌سازی‌های این مخزن:** هر **۱۴ زبان** موجود در پوشهٔ [`implementations/`](../../implementations)؛
3. **تمام نسخه‌ها و پیاده‌سازی‌های استاندارد ICU:** شامل **ICU4X** (در Rust)، **ICU4J** (در جاوا، کاتلین و اسکالا) و **ICU4C** از طریق `Intl` در جاوااسکریپت/تایپ‌اسکریپت، `intl` در PHP، و فراخوانی مستقیم C API از داخل **Python** (`ctypes`)، **Ruby** (`Fiddle`)، **Go** (`purego` بدون نیاز به cgo)، **C#‎** (`P/Invoke`)، **Dart** (`dart:ffi`) و **C** (`dlopen`/`LoadLibrary`).

علاوه بر این‌ها، دو موتور مستقل دیگه هم چک می‌شن: **`Temporal`** در جاوااسکریپت (که در Node.js 26 و موتور V8 بر پایهٔ `temporal_rs` و ICU4X ساخته شده، نه ICU4C) و کلاس **`System.Globalization.PersianCalendar`** در دات‌نت. از اونجایی که خود PostgreSQL تقویم شمسی داخلی نداره، توابع SQL ما روی یک موتور واقعی PostgreSQL (`PGlite`) اجرا می‌شن و روزبه‌روز با ICU و Temporal مقایسه می‌شن.

در این تست‌ها، نسخه‌های **ICU 77.1 به بعد** باید در تمام روزها دقیقا با تقویم نجومی یکی باشن؛ و برای نسخه‌های **ICU 76.1 و قدیمی‌تر** (که جدول اصلاحی ۷۸ ساله رو ندارن)، اسکریپت‌ها چک می‌کنن که خروجیِ اون‌ها دقیقا با **قاعدهٔ ۳۳ سالهٔ ساده** مطابقت داشته باشه.

</div>

## Results (October 2026) · نتایج تست‌ها

| Engine | Called from | Version | Result across all 665,837 days (1178–3000 AP) |
|---|---|---|---|
| ICU4X `icu_calendar` (Date API) | Rust | 2.4.0 | ✅ 100% match on every day, both directions |
| ICU4X `calendrical_calculations` (`fast_persian_from_fixed`, `fixed_from_fast_persian`, `is_leap_year`) | Rust | 0.2.4 | ✅ 100% match on every day, both directions; identical to our `from_jdn` across **all 73,048,850 days of the full accepted range (−100000…100000 AP)** |
| ICU4J | Java, Kotlin, Scala | **76.1** | ❌ Differs on 28,548 days: matches the plain 33-year rule (first wrong Nowruz is 1503 AP) |
| ICU4J | Java | 77.1, 78.1, 78.2, 78.3, 79.1 | ✅ 100% match on every day, both directions |
| ICU4J | Kotlin, Scala | 78.3, 79.1 | ✅ 100% match on every day, both directions |
| ICU4C | JavaScript & TypeScript `Intl` | **76.1** (Node.js 23.3) | ❌ Differs on 28,548 days: matches the plain 33-year rule |
| ICU4C | JavaScript & TypeScript `Intl` | 77.1 (Node.js 24.0), 78.3 (Node.js 26.8) | ✅ 100% match on every day |
| ICU4C | PHP `intl` | 77.1 (PHP 8.5.12) | ✅ 100% match on every day, both directions |
| ICU4C | Python, Ruby, Go, C#, Dart, C | **76.1** | ❌ Differs on 28,548 days: matches the plain 33-year rule, both directions |
| ICU4C | Python, Ruby, Go, C#, Dart, C | 77.1, 78.3, 79.1 | ✅ 100% match on every day, both directions |
| Temporal (`temporal_rs`, ICU4X) | JavaScript, TypeScript, PostgreSQL harness | Node.js 26.8 | ✅ 100% match on every day, both directions |
| .NET `System.Globalization.PersianCalendar` | C# | .NET 8.0 | ❌ Differs on 2,196 days across 12 years (first discrepancy in **1535 AP**) |
| This repository's implementations | All 14 languages | – | ✅ 100% match on every day, both directions |

**Full accepted range check (−100000…100000 AP):** Outside 1178–3000 AP there is no astronomical ground truth, but because ICU 77.1+, ICU4X, Temporal, and our code all implement the same arithmetic calendar, they must agree across the entire range. Our Python (ICU4C), Java (ICU4J), and JavaScript (Temporal) runners also compare the first day, last day, and length of **all 200,001 years**:

| Engine | Version | Years in −100000…100000 that differ |
|---|---|---|
| ICU4C, ICU4J | 77.1, 79.1 | **0** |
| ICU4C, ICU4J | 76.1 | 78 — the exact 78 years of the correction table |
| Temporal (`temporal_rs`) | Node.js 26.8 | **0** |
| ICU4X `calendrical_calculations` | 0.2.4 | **0** (verified on every single day, not just every year) |

**When did ICU get the 78-year correction table?** In **ICU 77.1**. Although ticket `ICU-22736` was committed in September 2024, it was not included in the ICU 76.1 release (verified in the `release-76-1` and `release-77-1` source tags for both ICU4C and ICU4J). Note that ICU 76.1 is still 100% accurate for every date up through **29 Esfand 1502 (19 March 2124)**.

### How ICU4X's `fast_persian_from_fixed` compares to our `from_jdn`

ICU4X ([`persian.rs`](https://github.com/unicode-org/icu4x/blob/main/utils/calendrical_calculations/src/persian.rs)) and this repository implement the exact same calendar rule: the same epoch, the 33-year formula, and the same 78 correction years. The only implementation detail that differs is how `from_jdn` resolves the Persian year from a day count:

* **ICU4X** computes the linear year estimate `1 + ⌊(33 × days + 3) / 12053⌋` and applies a single special-case check: if the day lands on day 366 of a correction year, it advances to `1 Farvardin` of `y + 1`.
* **Our implementations** compute the same linear estimate and then verify it against `nowruz_jdn(y)` and `nowruz_jdn(y + 1)` using two `while` loops (which run at most once).

Tested across **all 73,048,850 days of `−100000 … 100000 AP`**, both methods produce identical results for every single day! Our explicit boundary check costs virtually nothing at runtime and makes it self-evident that `from_jdn` can never return an out-of-bounds day-of-year.

<div dir="rtl">

### نکات مهم دربارهٔ نتایج بالا

* **اضافه شدن جدول اصلاحی ۷۸ ساله در ICU 77.1:** تغییر مربوط به جدول اصلاحی (`ICU-22736`) در سپتامبر ۲۰۲۴ ثبت شد و از نسخهٔ **ICU 77.1** (مارس ۲۰۲۵) وارد ICU4C و ICU4J شد. نسخهٔ ICU 76.1 این جدول رو نداره، اما تا پایان **۲۹ اسفند ۱۵۰۲ (۱۹ مارس ۲۱۲۴)** برای تمام تاریخ‌ها کاملا دقیقه.
* **مقایسهٔ کل بازهٔ ۲۰۰٬۰۰۱ ساله (`-100000` تا `100000`):** در کل این بازه (۷۳٬۰۴۸٬۸۵۰ روز)، خروجی پیاده‌سازی‌های ما با **ICU 77.1 به بعد**، **ICU4X** و **Temporal** بدون حتی یک روز اختلاف، ۱۰۰٪ یکسان است.

</div>

## How ICU4C is called from Python, Ruby, Go, C#, Dart, and C

None of these test scripts require third-party ICU wrapper packages—they call the ICU4C shared library directly via each language's built-in FFI (Go uses [`purego`](https://github.com/ebitengine/purego) so it doesn't even need `cgo`).
Using ICU's `UCAL_JULIAN_DAY` field (which uses the exact same Julian Day Number definition as our `to_jdn`/`from_jdn`), the entire cross-check only needs five C functions and pure integer arguments:

```c
UCalendar* cal = ucal_open(NULL, 0, "fa_IR@calendar=persian", UCAL_DEFAULT, &status);

// 1. JDN -> Persian (year, month, day)
ucal_clear(cal);
ucal_set(cal, UCAL_JULIAN_DAY, 2460756);              // 2025-03-21
ucal_get(cal, UCAL_EXTENDED_YEAR, &status);           // 1404
ucal_get(cal, UCAL_MONTH, &status);                   // 0   (ICU months are 0-based!)
ucal_get(cal, UCAL_DATE, &status);                    // 1

// 2. Persian (year, month, day) -> JDN
ucal_clear(cal);
ucal_set(cal, UCAL_EXTENDED_YEAR, 1403);
ucal_set(cal, UCAL_MONTH, 11);                        // 11 = Esfand
ucal_set(cal, UCAL_DATE, 30);
ucal_get(cal, UCAL_JULIAN_DAY, &status);              // 2460755 (= 2025-03-20)
```

*(Because ICU appends its major version to exported C symbol names—such as `ucal_open_77`—our FFI loaders automatically probe both unsuffixed and version-suffixed symbols.)*

## Running the cross-checks · نحوهٔ اجرا

| Language | Command (run from its subfolder) | Requirements |
|---|---|---|
| Rust (ICU4X) | `cargo run --release` | Rust 1.85+ |
| JavaScript (`Intl`, Temporal) | `node check.mjs` (run with each Node.js version you want to test) | Node.js |
| TypeScript (`Intl`, Temporal) | `node check.mjs typescript` (from `javascript/`) | Node.js 23.6+ |
| Java (ICU4J) | `javac -cp icu4j-X.jar -d out IcuCheck.java ../../../implementations/java/PersianCalendar.java`<br>`java -cp "icu4j-X.jar:out" IcuCheck ../../../data/years.csv` (use `;` instead of `:` on Windows) | JDK 17+, ICU4J `.jar` from Maven Central |
| Kotlin (ICU4J) | `kotlinc -cp icu4j-X.jar IcuCheck.kt ../../../implementations/kotlin/PersianCalendar.kt -include-runtime -d check.jar`<br>`java -cp "icu4j-X.jar:check.jar" IcuCheckKt ../../../data/years.csv` | Kotlin 2, JDK 17+ |
| Scala (ICU4J) | `scala run IcuCheck.scala ../../../implementations/scala/PersianCalendar.scala --dep com.ibm.icu:icu4j:79.1 --main-class IcuCheck -- ../../../data/years.csv` | Scala 3 (`scala-cli`) |
| PHP (`intl`) | `php check.php` | PHP with `intl` enabled |
| Python (`ctypes`) | `python check.py [path/to/icu/bin_or_lib]` | Python 3.8+, ICU4C shared libraries |
| Ruby (`Fiddle`) | `ruby check.rb [path/to/icu/bin_or_lib]` | Ruby 3+, ICU4C shared libraries |
| Go (`purego`) | `go run . [path/to/icu/bin_or_lib]` | Go 1.25+, ICU4C shared libraries |
| C# (P/Invoke + `.NET PersianCalendar`) | `dotnet run -c Release -- [path/to/icu/bin_or_lib]` | .NET 8 SDK, ICU4C shared libraries |
| Dart (`dart:ffi`) | `dart pub get && dart run bin/check.dart [path/to/icu/bin_or_lib]` | Dart 3, ICU4C shared libraries |
| C (`dlopen`/`LoadLibrary`) | `cc -std=c99 -O2 check.c ../../../implementations/c/persian_calendar.c -o check`<br>`./check <ICU folder> <ICU major version>` | C99 compiler, ICU4C shared libraries |
| PostgreSQL (`PGlite`) | `npm install && node check.mjs` | Node.js 22+ |

If you don't pass an ICU folder path, the Python, Ruby, Go, C#, and Dart runners automatically search for the system's installed ICU4C library (e.g., `/usr/lib/…/libicui18n.so.NN` on Linux or Homebrew `icu4c` on macOS).
To test against a specific ICU release, download the prebuilt ICU4C binaries from [GitHub Releases](https://github.com/unicode-org/icu/releases) (for example, `icu4c-79.1-Win64-MSVC2026.zip`) and pass the directory containing `icuinNN.dll` / `libicui18n.so.NN`:

```sh
python check.py                                   # Ubuntu 24.04: uses system ICU 74 (verified against the plain 33-year rule)
python check.py C:\icu\icu4c-79.1\bin64           # Windows with ICU 79.1
ruby check.rb /opt/homebrew/opt/icu4c/lib         # macOS with Homebrew ICU
go run . ./icu-76.1/lib                           # Any downloaded ICU release folder
```

Every runner prints a single summary line and exits with a non-zero code if a single day mismatches.
