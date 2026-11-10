# Library evaluation · ارزیابی کتابخانه‌ها

## What this folder is for

While [`implementations/`](../../implementations) contains **our own** reference code, this folder tests **third-party libraries and built-in platform APIs** that developers already use in the wild (`jalaali-js`, `jdatetime`, `persiantools`, ICU, .NET's `PersianCalendar`, etc.).
Every library is benchmarked against the exact same astronomical ground truth ([`data/years.csv`](../../data/years.csv)), and the output drives the recommendations and warnings in
[docs/en/06-languages-and-libraries.md](../../docs/en/06-languages-and-libraries.md) ([فارسی](../../docs/fa/06-languages-and-libraries.md)).
This test suite is how we caught the real-world conversion bugs in `jalaliday`, `khayyam`, and `parsi-date`.

<div dir="rtl">

پوشهٔ [`implementations/`](../../implementations) کدهای **خود ما** رو نگه می‌داره، اما این‌جا کدهای **بقیه** رو تست می‌کنیم: یعنی همون کتابخونه‌ها و APIهای داخلی‌ای که برنامه‌نویس‌ها همین الان دارن توی پروژه‌هاشون استفاده می‌کنن (`jalaali-js`، `jdatetime`، ICU، `PersianCalendar` در ‎.NET و…). همهٔ این کتابخونه‌ها رو با داده‌های مرجع یکسان محک زدیم و خروجی همین تست‌ها شد مبنای پیشنهادها و هشدارهایی که توی [صفحهٔ ۶ مستندات](../../docs/fa/06-languages-and-libraries.md) نوشتیم؛ باگ‌های `jalaliday`، `khayyam` و `parsi-date` هم دقیقا همین‌جا پیدا شدن.

</div>

Full score table: [`results/SUMMARY.md`](results/SUMMARY.md).

## Why not every language? · چرا بعضی زبان‌ها این‌جا نیستن؟

A language has a folder here only if it has a **distinct third-party library or built-in calendar engine** that we can run and evaluate:

| Language | What we evaluated | Notes |
|---|---|---|
| JavaScript / TypeScript | `Intl` (ICU), `jalaali-js`, `moment-jalaali`, `dayjs` + `jalaliday`, `date-fns-jalali` | TypeScript uses the exact same npm packages, so it shares the JS runner |
| Python | `jdatetime`, `persiantools`, `convertdate`, `khayyam` | |
| Java | `ICU4J`, `samanzamani/PersianDate` | JDK's `java.time` has no built-in Persian chronology |
| Kotlin / Android | `persian-calendar/calendar` | Android's built-in `android.icu` is ICU (tested under Java) |
| C# / .NET | `System.Globalization.PersianCalendar` (.NET 8 and .NET Framework 4.8), `DNTPersianUtils.Core`, `MD.PersianDateTime.Standard` | |
| Go | `go-jalaali`, `go-persian-calendar` (`ptime`) | |
| Rust | `ICU4X` (`icu_calendar`) | |
| PHP | `intl` (ICU), `morilog/jalali` | |
| Dart / Flutter | `shamsi_date` | |
| Ruby | `parsi-date` | Note: the old `jalaali` gem is not published on RubyGems |
| Scala | – | No standalone Scala-only library exists; Scala apps use JVM libraries (tested under Java) |
| C / C++ | – | The standard C/C++ library is ICU4C, which runs the exact same engine as ICU4J/`Intl`/PHP `intl` (and is tested directly in [`tools/icu-cross-check`](../icu-cross-check)) |
| Swift | – | Apple's `Foundation.Calendar(.persian)` wraps ICU and requires macOS/iOS |
| SQL | – | Databases have no native Persian calendar except SQL Server's `FORMAT(…, 'fa-IR')`, which calls .NET's `PersianCalendar` |

Remember: **all 14 languages** have a tested, zero-dependency implementation in [`implementations/`](../../implementations).

<div dir="rtl">

یک زبان فقط وقتی توی این پوشه اسکریپت داره که **کتابخونهٔ خارجیِ مستقل یا API داخلیِ مجزایی** برای تست کردن داشته باشه:

* **TypeScript** از همون پکیج‌های npm جاوااسکریپت استفاده می‌کنه؛
* **Scala** کتابخونهٔ مجزای مخصوص به خودش رو نداره و از همون کتابخونه‌های جاوا (مثل ICU4J) استفاده می‌کنه؛
* **C و C++‎** از کتابخونهٔ استاندارد ICU4C استفاده می‌کنن که موتور محاسباتیش با ICU4J و `Intl` یکیه (و توی [`tools/icu-cross-check`](../icu-cross-check) مستقیما تست شده)؛
* **Swift** در فریم‌ورک `Foundation` در واقع همون موتور ICU رو صدا می‌زنه؛
* در **SQL** هم دیتابیس‌ها تقویم شمسی داخلی ندارن (به‌جز `FORMAT(…, 'fa-IR')` در SQL Server که از همون `PersianCalendar` دات‌نت استفاده می‌کنه).

در عین حال، برای **هر ۱۴ زبان** کد کامل، بدون وابستگی و تست‌شده توی پوشهٔ [`implementations/`](../../implementations) قرار داده شده است.

</div>

## Method · روش تست و امتیازدهی

1. **Nowruz boundary test (all libraries):** For every Persian year from `1100` to `3300`, we ask the library for the Gregorian date of `1 Farvardin` and write the output to `results/<library>.txt` (`<year> <YYYY-MM-DD>` per line). Then [`tools/calendar-data`](../calendar-data) (`score-libraries`) computes the longest unbroken range of accurate years around `1404 AP` and lists any wrong years in `1300–1600 AP`.
2. **Every-day conversion test (73,414 days):** We convert every single day from `1 Farvardin 1300` to `29 Esfand 1500` in both directions (Gregorian → Persian and Persian → Gregorian) and record the count of wrong dates in `results/<library>.everyday.json`. This catches mid-year conversion bugs that a Nowruz-only check would miss—most notably the `jalaliday` bug that shifts all of January and February in Gregorian leap years!

All evaluated libraries are pinned to the exact versions listed in the summary table.

<div dir="rtl">

هر کتابخونه با دو آزمایش محک زده می‌شه:

1. **تست روز اول سال (نوروز) برای سال‌های ۱۱۰۰ تا ۳۳۰۰:** از کتابخونه می‌خوایم تاریخ میلادیِ `۱ فروردین` تمام این سال‌ها رو حساب کنه و توی `results/<library>.txt` بنویسه. بعد ابزار [`tools/calendar-data`](../calendar-data) (`score-libraries`) این خروجی رو با تقویم نجومی مقایسه می‌کنه تا بازهٔ پیوستهٔ سال‌های درست (حول‌وحوش سال ۱۴۰۴) و لیست سال‌های اشتباه در بازهٔ ۱۳۰۰ تا ۱۶۰۰ مشخص بشه.
2. **تست روزبه‌روز (۷۳٬۴۱۴ روز):** تک‌تک روزهای بازهٔ ۱ فروردین ۱۳۰۰ تا ۲۹ اسفند ۱۵۰۰ رو در هر دو جهت (میلادی به شمسی و شمسی به میلادی) تبدیل می‌کنیم و تعداد خطاها رو در `results/<library>.everyday.json` ثبت می‌کنیم. این تست باگ‌هایی رو شکار می‌کنه که در روز اول فروردین خودشون رو نشون نمی‌دن—مثل باگ عجیب پلاگین `jalaliday` که تمام روزهای ژانویه و فوریهٔ سال‌های کبیسهٔ میلادی رو یک روز اشتباه حساب می‌کنه!

</div>

## Running · نحوهٔ اجرا

| Runner folder | Command (run inside that folder) | Requirements |
|---|---|---|
| [`javascript/`](javascript) | `npm install && node run.mjs` | Node.js 22+ |
| [`python/`](python) | `pip install -r requirements.txt && python run.py` | Python 3.9+ |
| [`java/`](java) | `java -cp icu4j-79.1.jar Icu4j.java` (download `com.ibm.icu:icu4j` from Maven Central); `bash run-saman.sh` | Java 17+, git |
| [`kotlin/`](kotlin) | `bash run.sh` (clones and builds `persian-calendar/calendar` from source) | Kotlin, Java, git |
| [`dotnet/`](dotnet) | `dotnet run -c Release` | .NET 8 SDK |
| [`go/`](go) | `go mod tidy && go run .` | Go 1.21+ |
| [`rust/`](rust) | `cargo run --release` | Rust 1.85+ |
| [`php/`](php) | `composer install && php run.php` | PHP 8.1+ with `intl` |
| [`dart/`](dart) | `dart pub get && dart run` | Dart 3 |
| [`ruby/`](ruby) | `bundle install && ruby run.rb` | Ruby 3+ |

After running any test script, regenerate `results/SUMMARY.md` with:
`cd ../calendar-data && cargo run --release -- score-libraries`.

*(The `.NET Framework 4.8` result file was generated on Windows using PowerShell 5.1:)*

```powershell
$p = New-Object System.Globalization.PersianCalendar
1100..3300 | ForEach-Object { try { "{0} {1:yyyy-MM-dd}" -f $_, $p.ToDateTime($_,1,1,0,0,0,0) } catch {} }
```

## Adding a library · اضافه کردن یک کتابخانهٔ جدید

To evaluate another library, generate `results/<name>_<version>.txt` starting with a header line `# <name> <version>` followed by one `<year> <YYYY-MM-DD>` line per Persian year (and optionally `<same_name>.everyday.json` containing `days`, `g2pWrong`, and `p2gWrong`), run `cargo run --release -- score-libraries` inside `tools/calendar-data`, and add the library to page 6 of the guide.

<div dir="rtl">

برای اضافه کردن یک کتابخونهٔ جدید به این بنچمارک: کافیه اسکریپتی بنویسید که فایل `results/<name>_<version>.txt` رو تولید کنه (خط اول `# <name> <version>` و در هر خط بعدی `<year> <YYYY-MM-DD>` برای تاریخ میلادی ۱ فروردین اون سال؛ و در صورت تمایل فایل `<same_name>.everyday.json` شامل تعداد روزهای غلط در تبدیل‌های روزبه‌روز). در نهایت توی پوشهٔ `tools/calendar-data` دستور `cargo run --release -- score-libraries` رو اجرا کنید تا جدول `results/SUMMARY.md` به‌روز بشه.

</div>
