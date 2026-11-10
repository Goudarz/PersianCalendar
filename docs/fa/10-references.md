<div dir="rtl">

# ۱۰. منابع و مراجع

[→ تست](09-testing.md) · [بازگشت به صفحهٔ اصلی](../../README.fa.md) · [English](../en/10-references.md)

## مرجع رسمی تقویم در ایران

* **مرکز تقویم مؤسسهٔ ژئوفیزیک دانشگاه تهران** (<https://calendar.ut.ac.ir>): مرجع رسمی استخراج و انتشار تقویم، سال‌های کبیسه و تعطیلات رسمی کشور.
  * جدول رسمی سال‌های کبیسه از ۱۲۰۶ تا ۱۴۹۸ (لینک آرشیو اینترنت):
    <https://web.archive.org/web/20230712052459if_/https://calendar.ut.ac.ir/Fa/News/Data/Doc/KabiseShamsi1206-1498-new.pdf>
  * از اونجایی که سایت مرکز تقویم گاهی از خارج از ایران باز نمی‌شه، نسخهٔ متنی این جدول (با مجوز آزاد CC0) در فایل [`data/official/kabise-1206-1498.txt`](../../data/official/kabise-1206-1498.txt) داخل همین مخزن قرار داده شده است.

## پروژه‌ها و مخزن‌هایی که در نوشتن این راهنما بررسی شدند

| پروژه / مخزن | معرفی کوتاه | مجوز (License) | چطور در این راهنما استفاده شد؟ |
|---|---|---|---|
| [roozbehp/persiancalendar](https://github.com/roozbehp/persiancalendar) | پیاده‌سازی پایتونی تقویم نجومی (بر اساس کتاب Calendrical Calculations روی نصف‌النهار ۵۲٫۵ درجه) و الگوریتم سریع ۳۳ ساله + جدول اصلاح | Apache-2.0 (کد)، CC0 (داده) | مقایسهٔ مستقل با خروجی ابزار نجومی ما ([`tools/calendar-data`](../../tools/calendar-data) که خروجی کاملا یکسانی تولید می‌کنه)، جدول اصلاح استثناها و نسخهٔ متنی جدول رسمی مرکز تقویم |
| [persian-calendar/persian-calendar](https://github.com/persian-calendar/persian-calendar) | اپلیکیشن اندرویدی «تقویم فارسی» (به سرپرستی ابراهیم بیاگوی) | GPL-3.0 | بررسی نحوهٔ مدیریت تقویم‌ها، مناسبت‌ها و تایم‌زون در یک اپلیکیشن واقعی و پرکاربرد |
| [persian-calendar/calendar](https://github.com/persian-calendar/calendar) | کتابخونهٔ کاتلین همون اپلیکیشن (شامل تقویم شمسی، قمری، نپالی، تقویم تاریخی قبل از ۱۳۰۴ و جدول رسمی کبیسه‌ها) | GPL-2.0-or-later | مقایسهٔ لیست کبیسه‌های سال ۱ تا ۹۹۹۹ (`leaps.txt`)، نکات مربوط به تقویم قبل از ۱۳۰۴ و تقویم قمری ایران |
| [persian-calendar/events](https://github.com/persian-calendar/events) | دیتاست آزاد و ماشین‌خوان از تعطیلات و مناسبت‌های رسمی (ایران، افغانستان و…)، ساختهٔ ابراهیم بیاگوی | CC0-1.0 | استفاده در بخش [۷. تعطیلات و مناسبت‌ها](07-holidays.md) |
| [roozbehp/qamari](https://github.com/roozbehp/qamari) | تاریخ شروع ماه‌های تقویم هجری قمری در ایران | CC0-1.0 | استفاده در بخش [۷. تعطیلات و مناسبت‌ها](07-holidays.md) |
| [jalaali](https://github.com/jalaali) (`jalaali-js`، `go-jalaali`، `jalaali-python` و…) | پیاده‌سازی الگوریتم بورکوفسکی در زبان‌های مختلف | MIT | تست و ارزیابی شد (دقیق تا سال ۱۵۰۲ شمسی) |

## الگوریتم‌ها، کتاب‌ها و مقالات علمی

* E. M. Reingold, N. Dershowitz, *Calendrical Calculations*, 4th ed., Cambridge University Press, 2016 (و ویرایش *The Ultimate Edition*, 2018) — فصل «The Persian Calendar»: بررسی هر دو روش نجومی و حسابی (چرخهٔ ۲۸۲۰ ساله).
* K. M. Borkowski, "The Persian calendar for 3000 years", *Earth, Moon, and Planets* 74 (1996) 223–230:
  <https://www.astro.uni.torun.pl/~kb/Papers/EMP/PersianC-EMP.htm>
* روزبه پورنادر، فرمول ۳۳ ساله همراه با جدول اصلاح استثناها، در مخزن [roozbehp/persiancalendar](https://github.com/roozbehp/persiancalendar) (`persiancalendar_fast.py`) و [بحث شمارهٔ 4713 در مخزن ICU4X](https://github.com/unicode-org/icu4x/issues/4713).
* مستندات کلاس `PersianCalendar` در ICU4J:
  <https://unicode-org.github.io/icu-docs/apidoc/dev/icu4j/com/ibm/icu/util/PersianCalendar.html>
* پروژهٔ ICU: اصلاح الگوریتم تقویم شمسی در نسخهٔ ICU 50 (تیکت ICU-8952 در سال ۲۰۱۲) و اضافه شدن جدول اصلاح ۷۸ ساله در نسخهٔ ICU 77.1 (تیکت ICU-22736: ثبت در شهریور ۱۴۰۳ و انتشار در اسفند ۱۴۰۳؛ نسخهٔ 76.1 فاقد این جدول است که هم در سورس‌کد و هم با ابزار [`tools/icu-cross-check`](../../tools/icu-cross-check) بررسی و تایید شد):
  [`icu4c/source/i18n/persncal.cpp`](https://github.com/unicode-org/icu/blob/main/icu4c/source/i18n/persncal.cpp).
* J. Meeus, *Astronomical Algorithms*, 2nd ed., Willmann-Bell, 1998 (محاسبات اعتدال بهاری، پارامتر ΔT و معادلهٔ زمان).
* P. Bretagnon, J.-L. Simon, *Planetary Programs and Tables from −4000 to +2800*, Willmann-Bell, 1986 (سری ۴۹ جمله‌ای طول دایره‌البروجی خورشید).
* F. Espenak, J. Meeus، چندجمله‌ای‌های محاسبهٔ ΔT در وب‌سایت کسوف و خسوف ناسا (NASA):
  <https://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html>

## استانداردها و داده‌های پایه

* پروژهٔ Unicode CLDR، مشخصات تقویم `persian` و داده‌های بومی‌سازی زبان‌های `fa`، `fa-AF` و `ps`: <https://cldr.unicode.org>
* استاندارد فنی یونیکد شمارهٔ ۳۵ (UTS #35 برای شناسه‌های زبان و پسوندهای `-u-ca-` و `-u-nu-`): <https://unicode.org/reports/tr35/>
* دیتابیس تایم‌زون‌های IANA (`Asia/Tehran` و `Asia/Kabul`): <https://www.iana.org/time-zones>
* مشخصات استاندارد `Intl` (ECMA-402) و `Temporal` در جاوااسکریپت: <https://tc39.es/ecma402/> و <https://tc39.es/proposal-temporal/>

## مطالعهٔ بیشتر

* ویکی‌پدیا: [گاه‌شماری هجری خورشیدی](https://fa.wikipedia.org/wiki/گاه‌شماری_هجری_خورشیدی) · [Solar Hijri calendar](https://en.wikipedia.org/wiki/Solar_Hijri_calendar)
* دانشنامهٔ ایرانیکا (Encyclopædia Iranica): مقالهٔ [Calendars](https://www.iranicaonline.org/articles/calendars)

---

[→ تست](09-testing.md) · [بازگشت به صفحهٔ اصلی](../../README.fa.md)

</div>
