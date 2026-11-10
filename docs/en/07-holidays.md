# 7. Holidays and events

[← Languages & libraries](06-languages-and-libraries.md) · [Next: Common pitfalls →](08-pitfalls.md) · [فارسی](../fa/07-holidays.md)

While leap years can be computed with a mathematical formula, **most official holidays in Iran cannot**.
In your architecture, always treat holidays as **versioned data with an explicit source**, never as hard-coded logic.

## The three types of holidays in Iran

| Category | Underlying Calendar | Can it be computed by formula? | Examples |
|---|---|---|---|
| **Fixed solar holidays** | Solar Hijri (Persian) | ✅ **Yes:** falls on the exact same Persian date every year | Nowruz (1–4 Farvardin), 12 & 13 Farvardin, 14 & 15 Khordad, 22 Bahman, 29 Esfand |
| **Religious holidays** | **Lunar Hijri** (as sighted in Iran) | ❌ **Not with 100% certainty:** lunar months begin based on crescent moon sighting | Tasua & Ashura, Eid al-Fitr, Eid al-Adha (Qurban), Eid al-Ghadir, Arbaeen… |
| **Weekly holiday** | — | ✅ **Yes** | Friday (plus Thursday in many organizations) |

### Official public holidays in Iran (as published for 1405 AP)

*Source: University of Tehran Calendar Center, Official Calendar for 1405 AP, via the open CC0 dataset
[persian-calendar/events](https://github.com/persian-calendar/events) (commit `3bfbcc5`, June 2026).
Because parliament occasionally adds or changes public holidays, always check the latest official calendar for production use.*

**1. Fixed Solar Hijri holidays (same Persian date every year)**

| Persian Date | Occasion |
|---|---|
| 1, 2, 3, 4 Farvardin | Nowruz (New Year holidays) |
| 12 Farvardin | Islamic Republic Day |
| 13 Farvardin | Nature Day (Sizdah Bedar) |
| 14 Khordad | Anniversary of the passing of Ayatollah Khomeini |
| 15 Khordad | Uprising of 15 Khordad (1342 AP) |
| 22 Bahman | Victory of the Islamic Revolution (1357 AP) |
| 29 Esfand | Nationalization of the Iranian Oil Industry (1329 AP) |

**2. Lunar Hijri holidays** *(because the lunar year is ~11 days shorter than the solar year, these dates shift ~11 days earlier in the Persian calendar each year)*

| Lunar Hijri Date | Occasion |
|---|---|
| 9 and 10 Muharram | Tasua and Ashura |
| 20 Safar | Arbaeen |
| 28 Safar | Passing of the Prophet Muhammad; martyrdom of Imam Hasan |
| Last day of Safar (29 or 30 Safar) | Martyrdom of Imam Reza |
| 8 Rabi' al-Awwal | Martyrdom of Imam Hasan al-Askari |
| 17 Rabi' al-Awwal | Birth of the Prophet Muhammad and Imam Ja'far al-Sadiq |
| 3 Jumada al-Thani | Martyrdom of Fatimah al-Zahra |
| 13 Rajab | Birth of Imam Ali |
| 27 Rajab | Mab'ath (the Prophet's first revelation) |
| 15 Sha'ban | Birth of the Twelfth Imam (Mahdi) |
| 21 Ramadan | Martyrdom of Imam Ali |
| 1 and 2 Shawwal | Eid al-Fitr (end of Ramadan) |
| 25 Shawwal | Martyrdom of Imam Ja'far al-Sadiq |
| 10 Dhu al-Hijjah | Eid al-Adha (Feast of Sacrifice / Qurban) |
| 18 Dhu al-Hijjah | Eid al-Ghadir |

⚠️ **A classic trap on 29 Esfand:** Oil Nationalization Day is fixed to **29 Esfand** every year. In a leap year, 29 Esfand is *not* the last day of the year—**30 Esfand** is! Never code that holiday as "the last day of Esfand", and check whether your users also treat 30 Esfand as a non-working day in leap years.

## Why Lunar Hijri holidays are tricky in software

The Lunar Hijri calendar used in Iran is **not** the tabular (arithmetic) Islamic calendar, and it is **not** Saudi Arabia's Umm al-Qura calendar.
In Iran, a new lunar month begins when the crescent moon is sighted (or determined to be sightable) on the Iranian horizon as announced by official authorities. That has three major consequences for software:

* The official lunar month lengths for the upcoming Persian year are published by the University of Tehran Calendar Center when the next year's calendar comes out (usually in winter before Nowruz).
* Even after the printed calendar is published, actual crescent sighting at the end of a month (most notably at the end of Ramadan for **Eid al-Fitr**, 1 Shawwal) can shift the start of a lunar month by ±1 day!
* Built-in `Intl` / ICU calendars like `islamic`, `islamic-civil`, `islamic-tbla`, and `islamic-umalqura` **frequently disagree with Iran's official lunar calendar by a day**.

### How to handle lunar holidays in production

1. **Use a lookup table of Iranian lunar month start dates** (storing the Gregorian or Persian date of the 1st of each lunar month for known years).
   Two great CC0/open sources are [roozbehp/qamari](https://github.com/roozbehp/qamari) (historical and current Iranian lunar month records) and the table maintained in [persian-calendar/calendar](https://github.com/persian-calendar/calendar) (`IranianIslamicDateConverter`).
2. **Fall back to an astronomical/tabular estimate for future years** that haven't been published yet, and **clearly label those future religious holiday dates as "approximate / subject to moon sighting"** in your UI.
3. **Make the holiday table updatable** without requiring a full code rewrite—both annually when the Calendar Center publishes the new year's calendar, and whenever a month start is adjusted at moon sighting.

## How to model holidays as data

The JSON schema used by the CC0 project [persian-calendar/events](https://github.com/persian-calendar/events) is a great blueprint:

```json
{ "calendar": "Persian", "month": 1, "day": 13, "holiday": true, "type": "Iran", "title": "روز طبیعت" }
{ "calendar": "Hijri", "month": 2, "rule": "end of month", "holiday": true, "type": "Iran", "title": "…" }
{ "calendar": "Persian", "month": 12, "rule": "last weekday of month", "weekday": 3, "type": "AncientIran",
  "title": "چهارشنبه‌سوری (آخرین سه‌شنبهٔ سال)" }
```

When designing your own holiday table or JSON config:

* **Fields to store:** `calendar` (`Persian` / `Hijri` / `Gregorian`), `month`, `day` (or `rule`), `is_holiday` (boolean), `region`/`category`, localized `title`s, **source URL**, and optional `valid_from_year` / `valid_to_year`.
* **Rules you'll need beyond fixed `month/day`:**
  * *"Last day of the month"* (e.g., Martyrdom of Imam Reza on the last day of Safar, which is either the 29th or 30th).
  * *"Last [weekday] of the month"* (e.g., **Chaharshanbe Suri**, celebrated on the evening of the last Tuesday of Esfand).
  * *"One-off / ad-hoc holiday"* (for government-declared holidays due to heatwaves, air pollution, or special events).
* **Separate public holidays from non-holiday observances:** Users want to distinguish actual days off (`holiday: true`, shown in red on calendars) from commemorative days like Programmer's Day or Mother's Day (`holiday: false`).
* **Keep countries separate:** Iran and Afghanistan both use the Solar Hijri calendar, but their public holidays are completely different.
  *(Note: The Afghanistan holiday list in `persian-calendar/events` dates from 2020 and should be verified against current local regulations if you serve Afghan users.)*

## Weekends

In Iran, **Friday** is the official weekly holiday by law. However, whether **Thursday** (or, in recent legislative debates, Saturday) is also a non-working day varies across government ministries, banks, schools, Tehran vs. other provinces, and private companies—and has changed multiple times over the years.
**Always make weekend days a configuration setting**, never a hard-coded constant.

---

[← Languages & libraries](06-languages-and-libraries.md) · [Next: Common pitfalls →](08-pitfalls.md)
