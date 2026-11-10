<div dir="rtl">

# ۹. تست و بررسی درستی کد

[→ خطاهای رایج](08-pitfalls.md) · [بعدی: منابع ←](10-references.md) · [English](../en/09-testing.md)

چه الگوریتم تبدیل تاریخ رو خودتون بنویسید و چه از یک کتابخونهٔ آماده استفاده کنید، حتما باید اون رو با داده‌های مرجع مستقل تست کنید. توی این مخزن تمام داده‌های لازم برای این کار آماده شده است.

## فایل‌های دادهٔ مرجع (`data/`)

تمام فایل‌های این پوشه با موتور محاسبهٔ نجومی [`tools/calendar-data`](../../tools/calendar-data) (به زبان Rust) تولید شدن، **نه** با الگوریتمی که قراره تست بشه!
این ابزار قبل از ساختن فایل‌ها، خروجی نجومی خودش رو با تمام سال‌های جدول رسمی مرکز تقویم دانشگاه تهران چک می‌کنه و خروجی نهاییش هم یکسانه.

| فایل | تعداد سطرها | ستون‌ها و محتوا | کاربرد |
|---|---|---|---|
| [`data/years.csv`](../../data/years.csv) | ۱٬۸۲۳ (سال ۱۱۷۸ تا ۳۰۰۰) | `year, is_leap, days_in_year, nowruz_gregorian, nowruz_jdn, nowruz_weekday` | تست کبیسه بودن سال‌ها و تاریخ دقیق نوروز |
| [`data/test-vectors.csv`](../../data/test-vectors.csv) | ۸٬۵۱۰ | `jy, jm, jd, gy, gm, gd, jdn, weekday` | روز اول و آخر هر ماه در بازهٔ ۱۳۰۰ تا ۱۵۰۰، روز اول و آخر هر سال در ۱۱۷۸ تا ۳۰۰۰، و ۲۹ و ۳۰ اسفند تمام سال‌های کبیسه |
| [`data/leap-years.txt`](../../data/leap-years.txt) | ۴۴۲ | یک سال کبیسه در هر خط | لیست سریع سال‌های کبیسه در بازهٔ ۱۱۷۸ تا ۳۰۰۰ |
| [`data/corrections.txt`](../../data/corrections.txt) | ۷۸ | یک سال استثنا در هر خط | جدول ۷۸ سال اصلاح فرمول ۳۳ ساله (استخراج‌شده به شکل مستقل) |
| [`data/official/kabise-1206-1498.txt`](../../data/official/kabise-1206-1498.txt) | ۲۹۳ | سال (همراه با علامت `*` یا `**`) و تاریخ نوروز | نسخهٔ متنی جدول رسمی مرکز تقویم دانشگاه تهران (با مجوز CC0، استخراج‌شده توسط روزبه پورنادر) |
| [`data/locale-names.json`](../../data/locale-names.json) | ۱۱ زبان | نام ماه‌ها، روزهای هفته و پسوند تقویم | نام‌های استاندارد استخراج‌شده از CLDR 48 |

قراردادهای فایل‌ها: ماه‌ها از ۱ تا ۱۲؛ روز هفته (`weekday`) از ۰ = شنبه تا ۶ = جمعه؛ `jdn` = عدد روز ژولینی؛ تاریخ‌های میلادی بر اساس تقویم میلادی پیش‌گستر (Proleptic Gregorian)؛ فرمت UTF-8 با پایان خط `LF`.

## ده تستی که هر پیاده‌سازی باید پاس کنه

1. **تمام سطرهای `years.csv`**: بررسی اینکه خروجی `is_leap_year(year)` و تبدیل `to_gregorian(year, 1, 1)` دقیقا با ستون‌های `is_leap` و `nowruz_gregorian` یکی باشه.
2. **تمام سطرهای `test-vectors.csv` در هر دو جهت**: تبدیل شمسی به میلادی و میلادی به شمسی.
3. **تبدیل دوطرفهٔ (Round-trip) تک‌تک روزها** در کل بازهٔ پشتیبانی: چک کردن اینکه برای هر روز `n`، رابطهٔ `to_jdn(from_jdn(n)) == n` برقرار باشه، تاریخ خروجی معتبر باشه و دو روز متوالی دقیقا به دو تاریخ متوالی تبدیل بشن. این تست باگ‌های یک‌روزه در مرز ماه‌ها و سال‌ها رو (که ممکنه از چشم تست‌های نمونه‌ای دور بمونن) شکار می‌کنه.
4. **اعتبارسنجی تاریخ‌ها**: تاریخ `1403/12/30` معتبره؛ اما `1404/12/30`، `1404/07/31`، `1404/13/01`، `1404/00/01` و `1404/01/00` همگی باید نامعتبر تشخیص داده بشن.
5. **تایم‌زون**: لحظهٔ زمانی `2025-03-20T21:00:00Z` در تایم‌زون `Asia/Tehran` باید بشه **۱ فروردین ۱۴۰۴** و در تایم‌زون `UTC` بشه **۳۰ اسفند ۱۴۰۳**.
6. **محاسبات ماه و سال** (اگر پیاده‌سازی کردید): ۳۱ شهریور ۱۴۰۴ + ۱ ماه = ۳۰ مهر ۱۴۰۴؛ و ۳۰ اسفند ۱۴۰۳ + ۱ سال = ۲۹ اسفند ۱۴۰۴.
7. **تست اعداد میانی منفی**: تبدیل دوطرفهٔ تک‌تک روزهای سال‌های ۱۰۰۰− تا ۱۱۷۷. برای سال‌های صفر و منفی، مقادیر میانی فرمول‌ها منفی می‌شن و اگر تقسیم کف (Floor division) رو اشتباه نوشته باشید، دقیقا اینجا لو می‌ره! چند مقدار ثابت که تمام پیاده‌سازی‌های این مخزن با اون‌ها چک می‌شن:
   `to_jdn(-1000, 1, 1) = 1582713`، `to_jdn(0, 1, 1) = 1947955`، `to_jdn(4000, 1, 1) = 3408925`، `from_jdn(0) = (-5334, 9, 3)` و `is_leap_year(-3) = true`.
8. **رد کردن ورودی‌های غیرصحیح** در زبان‌های داینامیک (مثلا `is_valid(1404, 1.5, 1)` باید `false` برگردونه یا خطا بده).
9. **کنترل مرزهای ورودی**: اولین و آخرین روز بازهٔ مجاز باید درست تبدیل بشن؛ اما حتی یک روز خارج از بازه، اعداد خیلی بزرگ (۲⁵³ یا `INT64_MAX`) و تاریخ‌های میلادی نامعتبر (`2025-02-29`، `2025-02-30`، `1900-02-29`) باید بدون سرریز یا هنگ کردن، با خطا رد بشن. در زبان C هم چک کنید که در صورت بروز خطا، متغیرهای خروجی دست‌نخورده باقی بمونن و کد رو حتما با `-fsanitize=undefined` تست کنید.
10. **تست کل بازهٔ مجاز، سال‌به‌سال** (یک حلقهٔ سریع روی تمام سال‌ها): برای هر سال از ابتدا تا انتهای بازهٔ مجاز، بررسی کنید که ۱ فروردین دقیقا فردای آخرین روز سال قبل باشه و روز اول و آخر هر سال درست تبدیل بشن. برای بازهٔ ۱۰۰٬۰۰۰− تا ۱۰۰٬۰۰۰ در این مخزن، دو جمع کنترلی (Checksum) جای تمام کبیسه‌ها رو تایید می‌کنن: **۴۸٬۴۸۵ سال کبیسه** و **مجموع JDN نوروزها برابر با ۳۸۹٬۵۹۲٬۹۷۸٬۱۸۰** (که چون کمتر از ۲⁵³ است، حتی با اعداد جاوااسکریپت هم بدون خطا حساب می‌شه). موتورهای ICU 77.1+‎، ‏ICU4X و Temporal هم دقیقا همین دو عدد رو می‌دن.

تمام ۱۴ پیاده‌سازی موجود در پوشهٔ [`implementations`](../../implementations) تست‌های ۱ تا ۴، ۷، ۹ و ۱۰ رو دارن و تست شمارهٔ ۱ رو برای **تک‌تک ۶۶۵٬۸۳۷ روز** بازهٔ ۱۱۷۸ تا ۳۰۰۰ اجرا می‌کنن. وقتی کد هر زبانی رو برمی‌دارید، فایل تستش رو هم همراهش کپی کنید.

## مقایسهٔ متقابل با موتورهای ICU و ICU4X

ابزار [`tools/icu-cross-check`](../../tools/icu-cross-check) تک‌تک روزهای سال ۱۱۷۸ تا ۳۰۰۰ رو بین داده‌های مرجع نجومی، هر ۱۴ پیاده‌سازی این مخزن، کتابخونهٔ ICU4X (`icu_calendar` 2.4 و `calendrical_calculations`)، کتابخونهٔ ICU4J نسخه‌های 76.1 تا 79.1 (در Java، ‏Kotlin و Scala)، کتابخونهٔ ICU4C نسخه‌های 76.1 تا 79.1 (`Intl` در Node.js، افزونهٔ `intl` در PHP و فراخوانی مستقیم C API از طریق Python، ‏Ruby، ‏Go، ‏C#‎، ‏Dart و C)، موتور Temporal (`temporal_rs` در Node.js 26) و کلاس `PersianCalendar` در ‎.NET مقایسه می‌کنه. (چون PostgreSQL تقویم شمسی داخلی نداره، توابع SQL ما روی موتور واقعی PGlite اجرا می‌شن و خروجیشون با ICU و Temporal مقایسه می‌شه.)
نتیجه: ICU 77.1 به بعد، ICU4X و Temporal در تمام روزها صددرصد منطبق هستن؛ ICU 76.1 دقیقا فرمول سادهٔ ۳۳ ساله رو اجرا می‌کنه (از سال ۱۵۰۳ خطا داره) و `PersianCalendar` در ‎.NET هم در ۱۲ سال (مجموعا ۲٬۱۹۶ روز، از سال ۱۵۳۵ به بعد) اختلاف داره.

## مقایسه با Formatter داخلی پلتفرم

اگر روی پلتفرمی کار می‌کنید که ICU 77.1 یا جدیدتر داره (مثل Node.js 24+‎ یا مرورگرهای به‌روز)، می‌تونید خروجی کدتون رو در یک حلقه با `Intl.DateTimeFormat` مقایسه کنید؛ دقیقا مثل کاری که در [`implementations/javascript/persian-calendar.test.js`](../../implementations/javascript/persian-calendar.test.js) انجام شده است:

<div dir="ltr">

```js
const fmt = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', { timeZone: 'UTC', year: 'numeric', month: 'numeric', day: 'numeric' })
```

</div>

## چطور یک کتابخانهٔ جدید رو تست و ارزیابی کنیم؟

توی پوشهٔ [`tools/library-evaluation`](../../tools/library-evaluation) اسکریپت‌های آماده برای تست کتابخونه‌های JavaScript، ‏Python، ‏Java، ‏Rust، ‏Dart، ‏PHP، ‏Go، ‏.NET و Ruby قرار دارن.
برای اضافه کردن یک کتابخونهٔ جدید، کافیه تاریخ میلادی نوروز رو برای سال‌های ۱۱۰۰ تا ۳۳۰۰ از اون کتابخونه بگیرید و توی فایل `results/<name>.txt` (در هر خط به فرمت `<year> <YYYY-MM-DD>`) ذخیره کنید، و بعد توی پوشهٔ `tools/calendar-data` دستور `cargo run --release -- score-libraries` رو اجرا کنید.

## اجرای تمام تست‌های این مخزن

<div dir="ltr">

```sh
cd tools/calendar-data && cargo run --release -- --check      # data files are up to date
python -m unittest discover -s implementations/python        # Python
node --test implementations/javascript/                      # JavaScript
cd implementations/typescript && node --test                 # TypeScript (Node 23.6+)
cd implementations/java && javac -d out *.java && java -cp out PersianCalendarTest
cd implementations/kotlin && kotlinc *.kt -include-runtime -d t.jar && java -jar t.jar
cd implementations/scala && scala run . --main-class persiancalendar.PersianCalendarTest -- ../../data
cd implementations/csharp/tests && dotnet run
cd implementations/go && go test ./...
cd implementations/rust && cargo test --release
cd implementations/php && php test.php
cd implementations/dart && dart pub get && dart test
cd implementations/c && cc -std=c99 -O2 persian_calendar.c test.c -o t && ./t
cd implementations/ruby && ruby test_persian_calendar.rb
cd implementations/sql && npm install && npm test             # PostgreSQL (via PGlite)
```

</div>

همچنین می‌تونید با اجرای اسکریپت `bash tools/run-all-tests.sh` تمام تست‌هایی رو که کامپایلر/مفسرشون روی سیستمتون نصبه، یک‌جا اجرا کنید. همین دستورات در سرویس CI گیت‌هاب هم اجرا می‌شن: [`.github/workflows/test.yml`](../../.github/workflows/test.yml).

---

[→ خطاهای رایج](08-pitfalls.md) · [بعدی: منابع ←](10-references.md)

</div>
