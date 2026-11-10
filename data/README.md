# Data · دادهٔ مرجع

Ground-truth datasets for testing any Persian (Solar Hijri / Jalali) calendar implementation. For a complete guide on how to use these files in your tests, see [docs/en/09-testing.md](../docs/en/09-testing.md) · [فارسی](../docs/fa/09-testing.md).

| File | Description |
|---|---|
| [`years.csv`](years.csv) | All 1,823 years from 1178 to 3000 AP: `year, is_leap, days_in_year, nowruz_gregorian, nowruz_jdn, nowruz_weekday` |
| [`test-vectors.csv`](test-vectors.csv) | 8,510 boundary conversion test cases: `jy, jm, jd, gy, gm, gd, jdn, weekday` |
| [`leap-years.txt`](leap-years.txt) | All 442 leap years in 1178–3000 AP (one year per line) |
| [`corrections.txt`](corrections.txt) | The 78 years in 1178–3000 AP where the plain 33-year rule must be adjusted |
| [`locale-names.json`](locale-names.json) | Localized month names, weekday names, and era strings from Unicode CLDR 48 across 11 locales |
| [`official/kabise-1206-1498.txt`](official/kabise-1206-1498.txt) | Official leap-year table of the University of Tehran Calendar Center (plain-text CC0 transcription by Roozbeh Pournader) |

**Conventions:** Months are `1..12` (`1 = Farvardin`); weekdays are `0 = Saturday … 6 = Friday`; `jdn` is the standard Julian Day Number; Gregorian dates use the proleptic Gregorian calendar; all files use UTF-8 encoding with LF (`\n`) line endings.

**Do not edit these files by hand.** The `.csv` and `.txt` files are generated automatically by the astronomical engine in [`tools/calendar-data`](../tools/calendar-data)
(`cargo run --release`), which first verifies its output against the official Calendar Center table.
`locale-names.json` is generated from CLDR by [`tools/generate_locale_data.mjs`](../tools/generate_locale_data.mjs).

**License:** All generated files and `official/kabise-1206-1498.txt` are dedicated to the public domain under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/).

---

<div dir="rtl">

## راهنمای فارسی

این پوشه شامل داده‌های مرجع و راستی‌آزمایی‌شده برای تست هر نوع پیاده‌سازی یا کتابخونهٔ تقویم هجری شمسی (جلالی) است. برای اینکه ببینید چطور می‌تونید با این فایل‌ها کد خودتون رو تست کنید، صفحهٔ [۹. آزمون](../docs/fa/09-testing.md) رو ببینید.

| فایل | محتوا |
|---|---|
| [`years.csv`](years.csv) | اطلاعات کامل سال‌های ۱۱۷۸ تا ۳۰۰۰ ه‍.ش (۱٬۸۲۳ سال): شامل کبیسه بودن، تعداد روزهای سال، تاریخ میلادی نوروز، شمارهٔ روز جولیانی (JDN) نوروز و روز هفتهٔ اول سال |
| [`test-vectors.csv`](test-vectors.csv) | ۸٬۵۱۰ نمونهٔ تست مرزی برای تبدیل دوطرفهٔ شمسی و میلادی (`jy, jm, jd, gy, gm, gd, jdn, weekday`) |
| [`leap-years.txt`](leap-years.txt) | فهرست ۴۴۲ سال کبیسه در بازهٔ ۱۱۷۸ تا ۳۰۰۰ ه‍.ش (هر خط یک سال) |
| [`corrections.txt`](corrections.txt) | فهرست ۷۸ سالی که در اون‌ها قاعدهٔ ۳۳ سالهٔ ساده نیاز به اصلاح داره |
| [`locale-names.json`](locale-names.json) | نام ماه‌ها، روزهای هفته و پسوند تقویم برای ۱۱ زبان و فرهنگ، استخراج‌شده از استاندارد Unicode CLDR 48 |
| [`official/kabise-1206-1498.txt`](official/kabise-1206-1498.txt) | نسخهٔ متنی جدول رسمی کبیسه‌های مرکز تقویم دانشگاه تهران برای سال‌های ۱۲۰۶ تا ۱۴۹۸ (تهیه‌شده توسط روزبه پورنادر با مجوز CC0) |

**قراردادهای یکسان در تمام فایل‌ها:** شمارهٔ ماه‌ها از `1` تا `12` (`1` = فروردین)؛ روز هفته از `0` = شنبه تا `6` = جمعه؛ `jdn` برابر با شمارهٔ روز جولیانی؛ تاریخ‌های میلادی بر پایهٔ تقویم میلادی امتدادیافته (Proleptic Gregorian)؛ و انکودینگ فایل‌ها UTF-8 با پایان خط LF (`\n`) است.

**این فایل‌ها رو دستی ویرایش نکنید.** فایل‌های `.csv` و `.txt` مستقیما توسط ابزار محاسبات نجومی در [`tools/calendar-data`](../tools/calendar-data) (با دستور `cargo run --release`) ساخته می‌شن که قبل از نوشتن خروجی، مطابقت کاملش رو با جدول رسمی مرکز تقویم چک می‌کنه. فایل `locale-names.json` هم با اسکریپت [`tools/generate_locale_data.mjs`](../tools/generate_locale_data.mjs) تولید می‌شه.

**مجوز (License):** تمام فایل‌های تولیدشده و همچنین فایل `official/kabise-1206-1498.txt` با مجوز مالکیت عمومی [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) منتشر شدن و استفاده از اون‌ها در هر پروژه‌ای کاملا آزاده.

</div>
