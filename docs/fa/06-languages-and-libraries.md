<div dir="rtl">

# ۶. زبان‌ها و کتابخانه‌ها

[→ بومی‌سازی](05-localization.md) · [بعدی: تعطیلات ←](07-holidays.md) · [English](../en/06-languages-and-libraries.md)

در این بخش، وضعیت هر زبان برنامه‌نویسی رو بررسی کردیم: ابزارهای داخلی خود زبان، کتابخونه‌های معروفی که توی همین مخزن **تست و محک زده شدن**، و نمونه‌کدهای کاربردی.
روش تست و خروجی‌های خام رو می‌تونید توی پوشهٔ [`tools/library-evaluation`](../../tools/library-evaluation) ببینید. ما هر کتابخونه رو برای سال‌های ۱۱۰۰ تا ۳۳۰۰ با تقویم نجومی مقایسه کردیم و اکثرشون رو روی **تک‌تک روزهای** سال ۱۳۰۰ تا ۱۵۰۰ در هر دو جهت (شمسی به میلادی و برعکس) تست کردیم.
(اگر براتون سواله که چرا برای بعضی از زبان‌ها کتابخونهٔ جانبی تست نشده، توضیحش رو در [`tools/library-evaluation`](../../tools/library-evaluation#why-not-every-language) بخونید.)

## خلاصهٔ نتایج تست کتابخانه‌ها

| کتابخانه / API | نسخهٔ تست‌شده | بازهٔ عملکرد درست | توضیحات |
|---|---|---|---|
| **ICU** (`Intl` در Node.js و مرورگرها، ICU4C، ‏ICU4J و افزونهٔ `intl` در PHP) | ICU4C 76.1، 77.1، 78.3، 79.1؛ ICU4J 76.1، 77.1، 78.1 تا 78.3، 79.1 | ✅ ۱۱۷۸ تا ۳۰۰۰ | نسخه‌های ICU 77.1 به بالا جدول اصلاح رو دارن؛ نسخه‌های ۵۰ تا ۷۶ (از جمله 76.1) تا سال ۱۵۰۲ دقیق هستن |
| **ICU4X** یعنی `icu_calendar` (Rust) | 2.4.0 | ✅ ۱۱۷۸ تا ۳۰۰۰ | دقیقا از همون الگوریتم ICU 77.1+‎ استفاده می‌کنه |
| **Temporal** (JavaScript) | Node.js 26.8 (موتور V8 بر پایهٔ `temporal_rs`) | ✅ ۱۱۷۸ تا ۳۰۰۰ | مبتنی بر ICU4X است (نه ICU4C) و توی همهٔ روزها و در هر دو جهت کاملا دقیقه |
| **persiantools** (Python) | 6.2.0 | ✅ ۱۱۷۸ تا ۳۰۰۰ | تمام روزهای بازهٔ ۱۳۰۰ تا ۱۵۰۰ رو بدون خطا تبدیل کرد |
| **persian-calendar/calendar** (Kotlin) | 1.4.0 | ✅ ۱۱۷۸ تا ۳۰۰۰ | تمام روزهای ۱۳۰۴ تا ۱۵۰۰ کاملا درست؛ برای قبل از ۱۳۰۴ عمدا از طول ماه‌های تاریخی استفاده می‌کنه؛ مجوز: GPL-2.0+‎ |
| **jalaali-js** (JS) | 2.0.1 | ✅ تا ۱۵۰۲ | تمام روزهای ۱۳۰۰ تا ۱۵۰۰ کاملا درست |
| **date-fns-jalali** (JS) | 4.4.0-0 | ✅ تا ۱۵۰۲ | تمام روزهای ۱۳۰۰ تا ۱۵۰۰ کاملا درست |
| **moment-jalaali** (JS) | 0.10.5 | ✅ تا ۱۵۰۲ | دقیق کار می‌کنه، اما خود کتابخونهٔ Moment.js منسوخ (Maintenance mode) شده است |
| **jdatetime** (Python) | 6.1.0 | ✅ تا ۱۵۰۲ | تمام روزهای ۱۳۰۰ تا ۱۵۰۰ کاملا درست |
| **morilog/jalali** (PHP) | 3.5.0 | ✅ تا ۱۵۰۲ | |
| **go-jalaali** (Go) | 0.1.0 | ✅ تا ۱۵۰۲ | |
| **go-persian-calendar** (ptime) (Go) | 1.3.0 | ✅ تا ۱۵۰۲ | |
| **samanzamani/PersianDate** (Java/Android) | 1.7.1 | ✅ تا ۱۵۰۲ | تمام روزهای ۱۳۰۰ تا ۱۵۰۰ کاملا درست |
| **shamsi_date** (Dart/Flutter) | 1.1.1 | ✅ تا ۱۵۰۲ | متد `addMonths` وقتی روز در ماه مقصد وجود نداشته باشه (مثلا ۳۱ شهریور + ۱ ماه) خطا (Exception) می‌ده |
| **‎.NET** (`System.Globalization.PersianCalendar`) | ‎.NET 8.0.31 و ‎.NET Framework 4.8 | ✅ تا ۱۵۳۵ | کلاس داخلی دات‌نت که فرهنگ `fa-IR` به صورت پیش‌فرض ازش استفاده می‌کنه؛ تمام روزهای ۱۳۰۰ تا ۱۵۰۰ کاملا درست |
| **DNTPersianUtils.Core** (‎.NET) | 7.0.2 | ✅ تا ۱۵۳۵ | تمام روزهای ۱۳۰۰ تا ۱۵۰۰ کاملا درست (درون خودش از تقویم داخلی ‎.NET استفاده می‌کنه) |
| **MD.PersianDateTime.Standard** (‎.NET) | 2.6.0 | ✅ تا ۱۵۳۵ | تمام روزهای ۱۳۰۰ تا ۱۵۰۰ کاملا درست |
| **convertdate** (Python) | 2.5.1 | ⚠️ ۱۲۷۷ تا ۲۳۷۸ | برای تاریخ‌های امروزی درسته، اما برای سال‌های ۱۲۴۳، ۱۲۵۵، ۱۲۵۹ و… اشتباه حساب می‌کنه |
| **dayjs + jalaliday** (JS) | dayjs 1.11.23، jalaliday 3.1.1 | ❌ دارای باگ | **در تمام سال‌های کبیسهٔ میلادی، تبدیل میلادی به شمسی از ۱ ژانویه تا ۲۹ فوریه یک روز خطا داره!** (۲٬۹۴۰ روز اشتباه بین سال‌های ۱۳۰۰ تا ۱۵۰۰؛ مثلا تاریخ `2024-01-20` رو به جای `۱۴۰۲/۱۰/۳۰` به اشتباه `۱۴۰۲/۱۱/۰۱` برمی‌گردونه). تبدیل برعکس (شمسی به میلادی) درسته |
| **khayyam** (Python) | 3.0.17 | ❌ دارای باگ | از چرخهٔ ۲۸۲۰ ساله استفاده می‌کنه و **برای همین سال‌های ۱۴۰۳/۱۴۰۴**، ۱۴۳۶/۱۴۳۷ و… جواب اشتباه می‌ده |
| **parsi-date** (Ruby) | 0.5.1 | ❌ دارای باگ | در سال‌های کبیسه‌ای مثل ۱۳۸۳، **۱۴۰۸** (مارس ۲۰۲۹) و ۱۴۱۲ اشتباه می‌کنه؛ مثلا `2029-03-20` رو «۱۴۰۷/۱۲/۳۰» برمی‌گردونه که حتی متد `valid_date?` خودش هم اون رو نامعتبر می‌دونه! |

نکته: عبارت «تا ۱۵۳۵» (در ‎.NET) به سالی اشاره داره که لحظهٔ تحویلش فقط ۳۱ ثانیه با ظهر فاصله داره؛ یعنی یک اختلاف نظر نجومی لب‌مرز است، نه یک باگ برنامه‌نویسی ساده ([توضیحات بیشتر](../../tools/calendar-data#how-certain-is-the-future)). همچنین «تا ۱۵۰۲» یعنی تمام تاریخ‌ها تا ۲۹ اسفند ۱۵۰۲ (۱۹ مارس ۲۱۲۴ میلادی) کاملا دقیق هستن و اولین اختلافشون در کبیسهٔ سال ۱۵۰۲/۱۵۰۳ اتفاق می‌افته ([سال‌های کبیسه](02-leap-years.md)). بنابراین برای تمام تاریخ‌های قرن حاضر، هیچ‌کدام از این دو مورد مشکلی ایجاد نمی‌کنن.

**پیشنهاد خلاصه**: برای نمایش تاریخ از ابزار داخلی پلتفرم (`ICU` / `Intl` / `.NET` / `Foundation`)، برای محاسبات تاریخ از یکی از کتابخونه‌های علامت‌خورده با ✅، و اگر کد بدون وابستگی (Zero-dependency) می‌خواید از [پیاده‌سازی‌های همین مخزن](../../implementations) استفاده کنید. تا زمانی که باگ کتابخونه‌های ❌ برطرف نشده، سراغشون نرید.

---

## JavaScript / TypeScript

**امکانات داخلی**: `Intl.DateTimeFormat` (برای فرمت کردن و نمایش) و `Temporal` (برای محاسبات کامل تقویمی، در محیط‌هایی که پشتیبانی می‌شه).

<div dir="ltr">

```js
// Display
new Intl.DateTimeFormat('fa-IR', { dateStyle: 'long', timeZone: 'Asia/Tehran' }).format(new Date())
// → "۱ فروردین ۱۴۰۴"

// Read Persian year/month/day of a date (numbers)
const parts = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', {
  timeZone: 'Asia/Tehran', year: 'numeric', month: 'numeric', day: 'numeric',
}).formatToParts(new Date())
const get = (t) => Number(parts.find((p) => p.type === t).value)
const [jy, jm, jd] = [get('year'), get('month'), get('day')]

// Temporal (Node.js 26 and engines that ship Temporal): full calendar arithmetic
const d = Temporal.PlainDate.from({ calendar: 'persian', year: 1403, month: 12, day: 30 })
d.withCalendar('iso8601').toString()   // "2025-03-20"
d.inLeapYear                           // true
Temporal.PlainDate.from({ calendar: 'persian', year: 1404, month: 6, day: 31 }).add({ months: 1 })
                                       // 30 Mehr 1404 (clamped)
```

</div>

خود `Intl` نمی‌تونه تاریخ شمسی رو بگیره و به میلادی تبدیل کنه؛ برای این کار می‌تونید از `Temporal` یا یکی از این دو کتابخونه استفاده کنید:

<div dir="ltr">

```js
// jalaali-js (small, no dependencies)
import { toJalaali, toGregorian, isLeapJalaaliYear, jalaaliMonthLength } from 'jalaali-js'
toJalaali(2025, 3, 21)        // { jy: 1404, jm: 1, jd: 1 }
toGregorian(1403, 12, 30)     // { gy: 2025, gm: 3, gd: 20 }

// date-fns-jalali (date-fns API, Persian calendar)
import { format, newDate, addMonths } from 'date-fns-jalali'
import { faIR } from 'date-fns-jalali/locale'
format(new Date(2025, 2, 21), 'EEEE d MMMM yyyy', { locale: faIR })  // "جمعه 1 فروردین 1404"
format(addMonths(newDate(1404, 5, 31), 1), 'yyyy/MM/dd')             // "1404/07/30" (monthIndex is 0-based)
```

</div>

⚠️ **حواستون باشه**: در کلاس `Date` جاوااسکریپت شمارهٔ ماه‌ها از صفر شروع می‌شه (یعنی `new Date(2025, 2, 21)` می‌شه ۲۱ مارس!). بیشتر کتابخونه‌های شمسی ماه‌ها رو طبیعی (از ۱ تا ۱۲) می‌شمارن، اما تابع `newDate` در `date-fns-jalali` از همون قرارداد صفر-مبنای جاوااسکریپت پیروی می‌کنه (یعنی ۰ = فروردین، ۵ = شهریور).

کد آماده در این مخزن: [`implementations/javascript`](../../implementations/javascript) و [`implementations/typescript`](../../implementations/typescript).

## Python

**امکانات داخلی**: کتابخونهٔ استاندارد پایتون تقویم شمسی نداره.

<div dir="ltr">

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

</div>

هر دو کتابخونهٔ `persiantools` و `jdatetime` برای تاریخ‌های امروزی کاملا دقیق هستن (`persiantools` تا سال ۳۰۰۰ هم جدول اصلاح رو داره). **اصلا سراغ کتابخونهٔ `khayyam` نرید** چون از چرخهٔ ۲۸۲۰ ساله استفاده می‌کنه و همین الان برای سال‌های ۱۴۰۳ و ۱۴۰۴ خطا داره.
اگر در پایتون به فرمت کردن با ICU نیاز دارید، پکیج `PyICU` دقیقا مشابه نمونهٔ ICU4J در بخش جاوا عمل می‌کنه.

کد آماده در این مخزن: [`implementations/python`](../../implementations/python).

## Java

**امکانات داخلی**: نداره (`java.time` فقط `HijrahChronology` برای تقویم قمری داره و تقویم شمسی نداره).

**کتابخانهٔ ICU4J** (`com.ibm.icu:icu4j`): از متد `Calendar.getInstance` همراه با لوکیل استفاده کنید (چون فراخوانی مستقیم کلاس `PersianCalendar` از نسخهٔ ICU4J 78 به بعد منسوخ یا deprecated شده است):

<div dir="ltr">

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

</div>

کد بدون وابستگی در این مخزن: [`implementations/java`](../../implementations/java) (مستقیما با `java.time.LocalDate` کار می‌کنه).

## Kotlin / Android

* **در اندروید (API 24 به بالا)**: کتابخونهٔ ICU به صورت داخلی در پکیج `android.icu` وجود داره:
  `android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))`.
  البته نسخهٔ ICU به نسخهٔ اندروید دستگاه بستگی داره؛ گوشی‌های قدیمی‌تر فرمول سادهٔ ۳۳ ساله رو دارن که تا سال ۱۵۰۲ کاملا دقیقه.
* [`implementations/kotlin`](../../implementations/kotlin): پیاده‌سازی کاتلین خالص بدون وابستگی (مناسب برای پروژه‌های Kotlin Multiplatform و همراه با متدهای کمکی برای `java.time`).
* [persian-calendar/calendar](https://github.com/persian-calendar/calendar): موتور تقویم اپلیکیشن محبوب «تقویم فارسی» که هم جدول رسمی کبیسه‌ها و محاسبهٔ نجومی رو داره و هم از تقویم تاریخی قبل از ۱۳۰۴ پشتیبانی می‌کنه. ⚠️ فقط دقت کنید که مجوز این کتابخونه **GPL-2.0-or-later** است؛ قبل از استفاده مطمئن بشید که با لایسنس پروژهٔ شما همخوانی داره.

## Scala

**امکانات داخلی**: نداره؛ می‌تونید از همون ابزارهای جاوا (مثل ICU4J) استفاده کنید.
همچنین در پوشهٔ [`implementations/scala`](../../implementations/scala) یک پیاده‌سازی اختصاصی Scala 3 بدون وابستگی خارجی (همراه با متدهای کمکی برای `java.time.LocalDate`) قرار دادیم:

<div dir="ltr">

```scala
import persiancalendar.*
PersianCalendar.fromLocalDate(java.time.LocalDate.of(2025, 3, 21))   // PersianDate(1404, 1, 1)
PersianCalendar.toLocalDate(1403, 12, 30)                             // 2025-03-20
PersianCalendar.isLeapYear(1403)                                      // true
```

</div>

## C# / .NET

**امکانات داخلی**: کلاس `System.Globalization.PersianCalendar` (که تا سال ۱۵۳۵ کاملا دقیقه). فرهنگ `fa-IR` در دات‌نت به صورت پیش‌فرض از همین تقویم استفاده می‌کنه.

<div dir="ltr">

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

</div>

نکته: دات‌نت به صورت پیش‌فرض اعداد تاریخ شمسی رو با ارقام لاتین (`1404`) فرمت می‌کنه. همچنین از نسخهٔ ‎.NET 6 به بعد، `TimeZoneInfo.FindSystemTimeZoneById("Asia/Tehran")` روی تمام سیستم‌عامل‌ها (از جمله ویندوز) کار می‌کنه (در نسخه‌های قدیمی‌تر ویندوز باید از `"Iran Standard Time"` استفاده می‌کردید).
کد آماده در این مخزن: [`implementations/csharp`](../../implementations/csharp) (همراه با متدهای کمکی برای `DateOnly`).

## Go

**امکانات داخلی**: نداره.

<div dir="ltr">

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

</div>

کد آماده در این مخزن: [`implementations/go`](../../implementations/go) (همراه با متدهای کمکی برای `time.Time`).

## Rust

کتابخونهٔ استاندارد **ICU4X** (`icu_calendar`) دقیقا الگوریتم پیشنهادی این راهنما رو پیاده‌سازی کرده است:

<div dir="ltr">

```rust
use icu_calendar::cal::Persian;
use icu_calendar::{Date, Gregorian};

let p = Date::try_new_iso(2025, 3, 21)?.to_calendar(Persian);
(p.era_year().year, p.month().ordinal, p.day_of_month().0);      // (1404, 1, 1)
let g = Date::try_new_persian(1403, 12, 30)?.to_calendar(Gregorian); // 2025-03-20
Date::try_new_persian(1404, 12, 30).is_err();                     // true
```

</div>

برای فرمت کردن تاریخ هم می‌تونید از کریت `icu::datetime` با لوکیل `fa-IR` استفاده کنید.
کد بدون وابستگی در این مخزن: [`implementations/rust`](../../implementations/rust).

## PHP

**امکانات داخلی** (از طریق افزونهٔ `intl` که مبتنی بر ICU است):

<div dir="ltr">

```php
$fmt = new IntlDateFormatter('fa_IR@calendar=persian', IntlDateFormatter::FULL, IntlDateFormatter::NONE,
    'Asia/Tehran', IntlDateFormatter::TRADITIONAL, 'EEEE d MMMM y');
echo $fmt->format(new DateTimeImmutable('now', new DateTimeZone('Asia/Tehran')));  // جمعه ۱ فروردین ۱۴۰۴

$num = new IntlDateFormatter('fa_IR@calendar=persian;numbers=latn', IntlDateFormatter::SHORT,
    IntlDateFormatter::NONE, 'Asia/Tehran', IntlDateFormatter::TRADITIONAL, 'yyyy/MM/dd');
$ts = $num->parse('1404/01/01');   // Unix timestamp of 00:00 in Tehran (= 2025-03-20 20:30 UTC!)
(new DateTimeImmutable("@$ts"))->setTimezone(new DateTimeZone('Asia/Tehran'))->format('Y-m-d');  // 2025-03-21
```

</div>

⚠️ **نکتهٔ مهم**: متد `parse()` یک Unix timestamp (بر اساس ساعت ۰۰:۰۰ بامداد به وقت تهران، یعنی ساعت ۲۰:۳۰ شب روز قبل به وقت UTC!) برمی‌گردونه؛ بنابراین موقع تبدیل اون به تاریخ میلادی حتما باید تایم‌زون رو دوباره روی `Asia/Tehran` ست کنید، وگرنه تاریخ یک روز عقب می‌افته!
همچنین در PHP 8.5 صدا زدن `IntlCalendar::set()` با ۳ آرگومان یا بیشتر منسوخ (Deprecated) شده و باید به جاش از `setDate()` استفاده کنید.

**پکیج `morilog/jalali`** (انتخاب عالی برای پروژه‌های Laravel و سازگار با Carbon):

<div dir="ltr">

```php
use Morilog\Jalali\Jalalian;
Jalalian::fromDateTime(new DateTimeImmutable('2025-03-21'))->format('Y/m/d');  // 1404/01/01
Jalalian::fromFormat('Y/m/d', '1404/01/01')->toCarbon();                        // 2025-03-21
```

</div>

کد بدون وابستگی در این مخزن: [`implementations/php`](../../implementations/php).

## Ruby

متأسفانه هیچ‌کدام از gemهایی که در روبی تست کردیم بی‌نقص نبودن (مثلا `parsi-date` نسخهٔ 0.5.1 در سال ۱۴۰۸ خطا داره). پیشنهاد می‌کنیم مستقیم از فایل [`implementations/ruby`](../../implementations/ruby) در همین مخزن استفاده کنید (که با متد داخلی `Date#jd` روبی کار می‌کنه)، یا اگر سراغ gem دیگه‌ای رفتید، اول اون رو با فایل `data/years.csv` تست کنید.

## Swift / پلتفرم‌های اپل (iOS و macOS)

**امکانات داخلی**: فریم‌ورک `Foundation` اپل (که در زیرساخت خودش از ICU استفاده می‌کنه) به صورت پیش‌فرض تقویم شمسی رو داره:

<div dir="ltr">

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

</div>

## Dart / Flutter

کلاس `DateFormat` در پکیج رسمی `intl` دارت فقط تقویم میلادی رو ساپورت می‌کنه. برای دارت و فلاتر می‌تونید از پکیج محبوب **shamsi_date** استفاده کنید:

<div dir="ltr">

```dart
import 'package:shamsi_date/shamsi_date.dart';
final j = Jalali.fromDateTime(DateTime(2025, 3, 21));   // 1404/1/1
final g = Jalali(1403, 12, 30).toGregorian();           // 2025-03-20
final f = j.formatter;
'${f.wN} ${f.d} ${f.mN} ${f.yyyy}';                     // جمعه 1 فروردین 1404
```

</div>

⚠️ **دقت کنید**: در این پکیج، صدا زدن `Jalali(1404, 6, 31).addMonths(1)` به جای محدود کردن روز به ۳۰ مهر، خطای `DateException` پرتاب می‌کنه؛ پس موقع اضافه کردن ماه، روز رو خودتون کنترل کنید.
کد بدون وابستگی در این مخزن: [`implementations/dart`](../../implementations/dart).

## C / C++

کتابخونهٔ استاندارد C و C++‎ پشتیبانی داخلی از تقویم شمسی نداره. اگر توی پروژه‌تون امکان استفاده از ICU4C رو دارید، می‌تونید از `icu::Calendar::createInstance(icu::Locale("fa_IR@calendar=persian"), status)` استفاده کنید.
برای داشتن یک کد سبک، سریع و بدون هیچ وابستگی خارجی (به زبان C99 که بدون تغییر در C++‎ هم کامپایل می‌شه)، به پوشهٔ [`implementations/c`](../../implementations/c) سر بزنید.

## دیتابیس‌ها (SQL)

| دیتابیس | راه‌حل پیشنهادی |
|---|---|
| **PostgreSQL** | اسکریپت آمادهٔ [`implementations/sql/postgresql.sql`](../../implementations/sql/postgresql.sql): شامل توابع `persian_from_date()`، `persian_to_date()` و `persian_format()` که همگی `IMMUTABLE` هستن و مستقیم داخل ایندکس‌ها هم کار می‌کنن |
| **SQL Server** | تابع داخلی `FORMAT(@d, 'yyyy/MM/dd', 'fa-IR')` که از همون `PersianCalendar` دات‌نت استفاده می‌کنه |
| **MySQL / MariaDB / SQLite** | تابع داخلی برای تقویم شمسی ندارن. تبدیل رو در کد برنامه انجام بدید، یا یک جدول بعد تاریخ (Date Dimension) با کمک کدهای این مخزن بسازید و در کوئری‌ها `JOIN` بزنید |
| **توصیهٔ کلی برای همه** | همیشه تاریخ میلادی/UTC ذخیره کنید و تبدیل به شمسی رو در لایهٔ برنامه انجام بدید ([راهنمای پیاده‌سازی](04-implementation-guide.md)) |

---

[→ بومی‌سازی](05-localization.md) · [بعدی: تعطیلات ←](07-holidays.md)

</div>
