# 1. Calendar basics

[Next: Leap years →](02-leap-years.md) · [فارسی](../fa/01-basics.md)

## Names

The official calendar of Iran (and Afghanistan, with different month names) goes by several names in English and Persian.
In software, they all refer to the exact same calendar:

| Name | Where you'll see it |
|---|---|
| **Solar Hijri** (هجری شمسی) | Formal, legal, and academic writing; abbreviated **SH** or **AP** |
| **Persian calendar** | Unicode / CLDR / ICU standard identifier: `persian` (`fa-IR-u-ca-persian`) |
| **Jalali / Jalaali** (جلالی) | Open-source libraries: `jalaali-js`, `jdatetime`, `morilog/jalali`, … |
| **Shamsi / Khorshidi** (شمسی، خورشیدی) | Everyday spoken and written Persian |
| **Iranian calendar** | Wikipedia and general English articles |

In Persian text, the year is marked with **ه‍.ش** (short for هجری شمسی); in English, you'll see **AP** (*Anno Persico*) or **SH** (*Solar Hijri*).

> **A quick historical note:** Strictly speaking, the 11th-century *Jalali* calendar (designed by Omar Khayyam and his colleagues) and today's
> *Solar Hijri* calendar are not identical. However, in the software world, "Jalali", "Persian", and "Shamsi" are used interchangeably
> for the modern official calendar, and we do the same throughout this guide.

## The epoch: Year 1

Like the Islamic lunar calendar, the Persian calendar counts years from the **Hijra** (the Prophet Muhammad's migration to Medina in 622 CE).
The crucial difference is that Persian years are **solar**, so the months stay locked to the seasons just like the Gregorian calendar.

* Year 1 AP began in March 622 CE (1 Farvardin 1 = 22 March 622 in the proleptic Gregorian calendar, or 19 March 622 Julian).
* As a quick rule of thumb: **Persian year ≈ Gregorian year − 621** from Nowruz through 31 December,
  and **Gregorian year − 622** from 1 January until Nowruz.
* For example, **1404 AP** runs from 21 March 2025 to 20 March 2026.

Historical calendars don't have a "year 0". In everyday software, you rarely need dates before 1300 AP anyway—see [what range you can trust](03-algorithm.md#range) for details.

## The year

* Every year begins on **Nowruz** (نوروز), **1 Farvardin**, which is the day of the northern spring (vernal) equinox.
  In the Gregorian calendar, Nowruz always lands on **20, 21, or 22 March** (across the 201 years from 1300 to 1500 AP, it falls on 21 March 133 times,
  on 20 March 56 times, and on 22 March 12 times).
* A normal (common) year has **365 days**, and a leap year has **366 days**. Unlike the Gregorian calendar, leap years do *not* happen on a simple every-4-years schedule; see [Leap years](02-leap-years.md).

## Months

Ever since the Iranian parliamentary law of 11 Farvardin 1304 (31 March 1925) [[1](https://qavanin.ir/Law/TreeText/?IDS=11104622949229649799)], the lengths of the months have been completely fixed:
the first six months have **31 days**, the next five have **30 days**, and the twelfth month (Esfand) has **29 days** (or **30** in a leap year).

Why are the spring and summer months 31 days long while autumn and winter months are shorter? Because Earth's orbit is elliptical: Earth moves more slowly around the Sun near aphelion (in summer), so it takes the Sun more days to travel through the first six zodiac constellations than the last six!

| # | Persian (Iran) | Transliteration (CLDR) | Days | Starts in 1404 AP | Dari (Afghanistan) | Pashto |
|---|---|---|---|---|---|---|
| 1 | فروردین | Farvardin | 31 | 2025-03-21 | حمل (Hamal) | وری |
| 2 | اردیبهشت | Ordibehesht | 31 | 2025-04-21 | ثور (Sawr) | غویی |
| 3 | خرداد | Khordad | 31 | 2025-05-22 | جوزا (Jawza) | غبرگولی |
| 4 | تیر | Tir | 31 | 2025-06-22 | سرطان (Saratan) | چنگاښ |
| 5 | مرداد | Mordad | 31 | 2025-07-23 | اسد (Asad) | زمری |
| 6 | شهریور | Shahrivar | 31 | 2025-08-23 | سنبله (Sonbola) | وږی |
| 7 | مهر | Mehr | 30 | 2025-09-23 | میزان (Mizan) | تله |
| 8 | آبان | Aban | 30 | 2025-10-23 | عقرب (Aqrab) | لړم |
| 9 | آذر | Azar | 30 | 2025-11-22 | قوس (Qaws) | لیندۍ |
| 10 | دی | Dey | 30 | 2025-12-22 | جدی (Jadi) | مرغومی |
| 11 | بهمن | Bahman | 30 | 2026-01-21 | دلو (Dalw) | سلواغه |
| 12 | اسفند | Esfand | 29 / 30 | 2026-02-20 | حوت (Hut) | کب |

You can find month and weekday names in 11 languages inside [`data/locale-names.json`](../../data/locale-names.json)
(extracted directly from Unicode CLDR). See [Localization](05-localization.md) for formatting tips.

This regular month structure gives us three great shortcuts in code:

* **No lookup table needed for month lengths:** Days 1–186 of the year always belong to months 1–6 (6 × 31 = 186), and days 187–365/366 belong to months 7–12 (30 days each).
* **Predictable Gregorian alignment:** Every Persian month starts on almost the exact same Gregorian day every year (varying by at most ±1 day).
* **Seasons match quarters:** Spring is months 1–3, summer is 4–6, autumn is 7–9, and winter is 10–12.

## Weeks

| Index* | Persian | Transliteration | English |
|---|---|---|---|
| 0 | شنبه | Shanbeh | Saturday |
| 1 | یکشنبه | Yekshanbeh | Sunday |
| 2 | دوشنبه | Doshanbeh | Monday |
| 3 | سه‌شنبه | Seshanbeh | Tuesday |
| 4 | چهارشنبه | Chaharshanbeh | Wednesday |
| 5 | پنجشنبه | Panjshanbeh | Thursday |
| 6 | جمعه | Jomeh | Friday |

\* This `0 = Saturday … 6 = Friday` index is used consistently by all 14 implementations in this repository.

* **The week starts on Saturday.** Unicode CLDR specifies the same for `fa-IR`: in JavaScript, `new Intl.Locale('fa-IR').getWeekInfo()`
  returns `{ firstDay: 6, weekend: [5] }` (where ISO weekday 6 = Saturday and 5 = Friday).
* **Friday** is the official weekly holiday in Iran. Because many offices, schools, and businesses are also closed or work half-days on Thursday,
  you should make weekends configurable in your software rather than hard-coding them.
* Watch out for the **zero-width non-joiner** (ZWNJ, `U+200C`) in «سه‌شنبه» (Tuesday). Without it, «سه» and «شنبه» stick together awkwardly (`سهشنبه`).
* **Weekdays are calendar-independent.** A Tuesday is a Tuesday whether you write the date in Gregorian or Persian. Always compute the weekday from a continuous day count
  (like Julian Day Number or Unix epoch days)—never from the Persian year/month/day fields.

## A brief history (what developers need to know)

| Period | What happened | Why it matters for software |
|---|---|---|
| **1079 CE** | The Jalali calendar reform (Omar Khayyam and colleagues) anchors the new year to the spring equinox | Origin of the name "Jalali" |
| **Before 1304 AP (1925)** | The year began at the equinox, but month lengths varied between 29 and 32 days depending on how long the Sun stayed in each zodiac sign (and lunar Hijri dates were often used in daily life) | Historical documents before March 1925 **cannot** be converted accurately using modern fixed 31/30-day month rules |
| **11 Farvardin 1304 (31 March 1925)** | Iranian Parliament passes the calendar law fixing month names and lengths (`6×31`, `5×30`, `29/30`) | Birth of the modern Solar Hijri calendar that software implements today |
| **1355–1357 AP (1976–1978)** | The epoch was briefly switched to the "Imperial" (Shahanshahi) era, adding 1,180 to the year (`1355 AP` became `2535`) [[2](http://www.ensani.ir/storage/Files/20101205130401-Pages%20from%20payam6-30.pdf)] [[3](https://web.archive.org/web/20111130070346/http://calendar.ut.ac.ir/Fa/CalHistory/CalHistory4-3-2.asp)] | Official records printed during those three years may show years `2535–2537` |
| **Today** | Computed astronomically and published annually by the Calendar Center of the University of Tehran's Institute of Geophysics | The single authoritative source of truth for leap years and official holidays in Iran |

Afghanistan uses the exact same calendar structure and leap years, simply substituting the Dari or Pashto month names shown in the table above.

## How it relates to other calendars

* **Gregorian calendar:** Every Persian date maps one-to-one to a Gregorian date. Converting between them just means converting through a shared day counter
  (see [The algorithm](03-algorithm.md)).
* **Lunar Hijri calendar (هجری قمری):** A completely separate, 354/355-day lunar calendar used alongside the Persian calendar for religious holidays.
  Because its months in Iran begin based on actual crescent moon sighting, future lunar dates cannot be computed with 100% certainty by a pure formula. See [Holidays](07-holidays.md).

---

[Next: Leap years →](02-leap-years.md)
