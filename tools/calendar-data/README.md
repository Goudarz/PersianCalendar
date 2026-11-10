# calendar-data · ابزار محاسبات نجومی و تولید دادهٔ مرجع

Generates and verifies the ground-truth reference data in [`/data`](../../data) directly from the astronomical definition of the
Solar Hijri calendar, and scores the [library evaluation](../library-evaluation) results. Written in pure Rust with zero external dependencies.

<div dir="rtl">

این ابزار (نوشته‌شده به زبان Rust و بدون هیچ وابستگی خارجی) داده‌های مرجع پوشهٔ [`/data`](../../data) رو مستقیما بر اساس تعریف نجومیِ تقویم هجری شمسی تولید و راستی‌آزمایی می‌کنه و خروجی تست‌های [ارزیابی کتابخانه‌ها](../library-evaluation) رو هم امتیازدهی می‌کنه.

</div>

```sh
cd tools/calendar-data
cargo run --release                          # Regenerate data/*.csv and data/*.txt
cargo run --release -- --check               # Verify data/ files match astronomical calculations and the official table (run in CI)
cargo run --release -- equinox 2025 2026     # Print exact March equinox times (Iran time) and Nowruz dates for Gregorian years
cargo run --release -- score-libraries       # Regenerate tools/library-evaluation/results/SUMMARY.md
cargo test --release                         # Run unit tests + verify files on disk match generated data byte-for-byte
```

## What it computes

By official definition, **1 Farvardin (Nowruz) is the day on which the March equinox occurs before true (apparent) solar noon on the 52.5° E meridian** (Iran Standard Time, `UTC+3:30`). To compute the calendar from first principles, this tool:

1. **Computes the Sun's apparent celestial longitude** ([`src/astronomy.rs`](src/astronomy.rs)):
   * Evaluates the 49-term periodic series from Bretagnon & Simon, *Planetary Programs and Tables from −4000 to +2800* (1986), including nutation and aberration corrections;
   * Computes **ΔT** (Dynamical Time − Universal Time) using Meeus's table for 1600–1986 and the Espenak & Meeus (NASA) polynomials for other eras;
   * Computes the **equation of time** and the **obliquity of the ecliptic** from Meeus, *Astronomical Algorithms* (2nd ed., 1998) to find exact apparent solar noon.
2. **Finds Nowruz for each year** by locating the first day on which the Sun's apparent longitude at true noon on the `52.5° E` meridian lies in `[0°, 2°]` ([`src/persian.rs`](src/persian.rs)).
3. **Derives leap years, the 78-year correction table, and the 8,510 test vectors** ([`src/data.rs`](src/data.rs)).

The astronomical model follows E. M. Reingold and N. Dershowitz, *Calendrical Calculations* (4th ed., Cambridge University Press, 2016), evaluated on the **52.5° E meridian** (Iran Standard Time) rather than Tehran's local meridian (`51.42° E`). Using `52.5° E` is essential to match the official calendar: using Tehran's local longitude gets the leap year of 1469/1470 AP wrong! The Rust code here is an independent implementation written from scratch for this repository.

## Verification against independent sources

| Reference | Result |
|---|---|
| Official leap-year table of the University of Tehran Calendar Center for 1206–1498 AP ([`data/official`](../../data/official)) | All 293 Nowruz dates and leap years match 100% (checked automatically on every run) |
| Independent Python astronomical implementation ([roozbehp/persiancalendar](https://github.com/roozbehp/persiancalendar)) | Produces byte-for-byte identical output across 1178–3000 AP for all four generated data files |
| Independent leap-year table for 1–9999 AP in [persian-calendar/calendar](https://github.com/persian-calendar/calendar) (`leaps.txt`) | Matches 100% across 1178–3000 AP |
| Officially announced turn-of-the-year moments for 1403 and 1404 AP | Agrees to within 30 seconds (`06:36:07` vs. `06:36:26` in 1403; `12:31:08` vs. `12:31:30` in 1404) |

### How certain is the future?

Running `cargo test -- --nocapture` lists every year in 1178–3000 AP where the spring equinox falls within 10 minutes of true solar noon at `52.5° E`. In those borderline years, even a tiny shift in the solar ephemeris model or in **ΔT** (Earth's rotational drift, which cannot be predicted to the exact second centuries ahead) could flip Nowruz by a day:

| Persian Year (AP) | 1309 | 1470 | 1503 | **1536** | 1569 | **1602** | 1635 | 1701 | 1734 | **2027** | 2060 | **2093** | … | **2584** |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Distance from noon | 8 min | 2.7 min | 3.6 min | **31 s** | 5.8 min | **37 s** | 9 min | 7 min | 8 min | **39 s** | 9.6 min | **42 s** | | **6 s** |

The official Calendar Center table covers 1470 AP (where the equinox is 2.7 minutes from noon), and this tool matches the official ruling. Notice that the years where .NET's `PersianCalendar` differs (`1536`, `2027`, `2093`) are the exact years where the equinox is only ~30–40 seconds away from noon! That's why leap years beyond ~1500 AP should be understood as the best astronomical prediction available today rather than physical certainties.

License: MIT.

---

<div dir="rtl">

## توضیحات فارسی

### این ابزار چه کاری انجام می‌ده؟

طبق تعریف رسمی تقویم در ایران، **۱ فروردین (نوروز) روزی است که لحظهٔ تحویل سال (اعتدال بهاری) قبل از ظهر حقیقی در نصف‌النهار ۵۲٫۵ درجهٔ شرقی (نصف‌النهار رسمی زمان ایران، `UTC+3:30`) رخ بده.** این ابزار برای محاسبهٔ مستقیم این قاعده، سه مرحله رو انجام می‌ده:

1. **محاسبهٔ طول دایرةالبروجی ظاهری خورشید** ([`src/astronomy.rs`](src/astronomy.rs)):
   * محاسبهٔ سری ۴۹ جمله‌ایِ Bretagnon و Simon از کتاب *Planetary Programs and Tables from −4000 to +2800* (سال ۱۹۸۶) همراه با تصحیحات رقص محوری (Nutation) و ابیراهی نور (Aberration)؛
   * محاسبهٔ **ΔT** (اختلاف زمان دینامیکی و زمان جهانی ناشی از کند و تند شدن چرخش زمین) با جدول Meeus برای سال‌های ۱۶۰۰ تا ۱۹۸۶ میلادی و چندجمله‌ای‌های Espenak و Meeus (ناسا) برای سایر دوره‌ها؛
   * محاسبهٔ **تعدیل زمان** (Equation of Time، برای پیدا کردن لحظهٔ دقیق ظهر حقیقی) و میل کلی دایرةالبروج بر اساس کتاب *Astronomical Algorithms* نوشتهٔ Jean Meeus (ویرایش دوم، ۱۹۹۸).
2. **پیدا کردن روز نوروز برای هر سال** ([`src/persian.rs`](src/persian.rs)): پیدا کردن اولین روزی که در ظهر حقیقیِ نصف‌النهار ۵۲٫۵ درجهٔ شرقی، طول سماوی خورشید در بازهٔ `[0°, 2°]` قرار گرفته باشه.
3. **استخراج سال‌های کبیسه، جدول اصلاحی ۷۸ ساله و ۸٬۵۱۰ نمونهٔ تست** ([`src/data.rs`](src/data.rs)).

روش محاسبه بر اساس کتاب *Calendrical Calculations* (نوشتهٔ Reingold و Dershowitz، انتشارات دانشگاه کمبریج، ۲۰۱۶) پیاده‌سازی شده، با این نکتهٔ کلیدی که نصف‌النهار مرجع روی **۵۲٫۵ درجهٔ شرقی** (زمان رسمی ایران) تنظیم شده است، نه طول جغرافیایی شهر تهران (`51.42° E`). استفاده از نصف‌النهار ۵۲٫۵ درجه برای تطابق با جدول رسمی مرکز تقویم ضروریه (چون با نصف‌النهار محلی تهران، کبیسهٔ سال ۱۴۶۹/۱۴۷۰ اشتباه درمی‌آد). کدهای Rust این بخش به صورت مستقل و از صفر برای همین مخزن نوشته شدن.

### راستی‌آزمایی و تطابق با منابع مستقل

| منبع مقایسه | نتیجه |
|---|---|
| جدول رسمی کبیسه‌های مرکز تقویم دانشگاه تهران برای سال‌های ۱۲۰۶ تا ۱۴۹۸ ه‍.ش ([`data/official`](../../data/official)) | تطابق ۱۰۰٪ در تمام ۲۹۳ سال (در هر بار اجرای برنامه به صورت خودکار چک می‌شه) |
| پیاده‌سازی نجومی مستقل به زبان پایتون ([roozbehp/persiancalendar](https://github.com/roozbehp/persiancalendar)) | خروجیِ بایت‌به‌بایت یکسان در کل بازهٔ ۱۱۷۸ تا ۳۰۰۰ برای هر چهار فایل داده |
| فهرست کبیسه‌های سال ۱ تا ۹۹۹۹ در کتابخانهٔ [persian-calendar/calendar](https://github.com/persian-calendar/calendar) (`leaps.txt`) | تطابق ۱۰۰٪ در کل بازهٔ ۱۱۷۸ تا ۳۰۰۰ |
| لحظهٔ رسمی اعلام‌شده برای تحویل سال‌های ۱۴۰۳ و ۱۴۰۴ | تطابق با دقت بهتر از ۳۰ ثانیه (`06:36:07` در برابر `06:36:26` در سال ۱۴۰۳؛ و `12:31:08` در برابر `12:31:30` در سال ۱۴۰۴) |

### آینده رو تا کجا می‌شه با قطعیت پیش‌بینی کرد؟

اگر دستور `cargo test -- --nocapture` رو اجرا کنید، تمام سال‌هایی که در اون‌ها لحظهٔ تحویل سال کمتر از ۱۰ دقیقه با ظهر حقیقی فاصله داره چاپ می‌شن. در این سال‌های لبِ مرز، حتی یک تغییر کوچک در مدل نجومی یا در تخمین **ΔT** (که به خاطر تغییرات نامنظم چرخش زمین، قرن‌ها جلوتر قابل پیش‌بینی دقیق در حد ثانیه نیست) می‌تونه روز نوروز رو یک روز جابه‌جا کنه (جدول بالا فاصلهٔ دقیق این سال‌ها مثل **۱۵۳۶** با ۳۱ ثانیه، **۱۶۰۲** با ۳۷ ثانیه، **۲۰۲۷** با ۳۹ ثانیه و **۲۵۸۴** با فقط ۶ ثانیه تا ظهر رو نشون می‌ده).

جدول رسمی مرکز تقویم سال ۱۴۷۰ (با فاصلهٔ ۲٫۷ دقیقه تا ظهر) رو پوشش داده و خروجی این ابزار هم دقیقا با اون یکیه. جالبه بدونید سال‌هایی که `PersianCalendar` در ‎.NET با محاسبات ما تفاوت داره (یعنی ۱۵۳۶، ۲۰۲۷ و ۲۰۹۳) دقیقا همین سال‌های لبِ مرز با فاصلهٔ ۳۰ تا ۴۰ ثانیه تا ظهر هستن! به همین دلیل، کبیسه‌های بعد از حدود سال ۱۵۰۰ ه‍.ش رو باید به عنوان بهترین پیش‌بینی نجومیِ فعلی در نظر گرفت.

</div>
