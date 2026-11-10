# 10. References

[← Testing](09-testing.md) · [Back to main README](../../README.md) · [فارسی](../fa/10-references.md)

## Official calendar authority

* **Calendar Center, Institute of Geophysics, University of Tehran** (مرکز تقویم مؤسسه ژئوفیزیک دانشگاه تهران):
  <https://calendar.ut.ac.ir> — the official authority responsible for computing and publishing Iran's calendar, leap years, and official holidays.
  * Official leap-year table for 1206–1498 AP (direct PDF and WayBack Machine archive):
    <https://calendar.ut.ac.ir/documents/2139738/7092644/Kabise+Shamsi+1206-1498.pdf> ·
    <https://web.archive.org/web/20230712052459if_/https://calendar.ut.ac.ir/Fa/News/Data/Doc/KabiseShamsi1206-1498-new.pdf>
  * Because `calendar.ut.ac.ir` is sometimes unreachable from outside Iran, a plain-text CC0 transcription of the official table is included right in this repository at
    [`data/official/kabise-1206-1498.txt`](../../data/official/kabise-1206-1498.txt).

## Open-source repositories studied for this guide

| Repository | What it is | License | How it helped this project |
|---|---|---|---|
| [roozbehp/persiancalendar](https://github.com/roozbehp/persiancalendar) | Astronomical Persian calendar in Python (adapted from *Calendrical Calculations* on the 52.5° E meridian) plus the fast 33-year + 78-year correction algorithm | Apache-2.0 (code), CC0 (data) | Independent verification of our astronomical tool ([`tools/calendar-data`](../../tools/calendar-data) produces byte-for-byte identical results), the 78-year correction list, and the plain-text copy of the official leap-year table |
| [persian-calendar/persian-calendar](https://github.com/persian-calendar/persian-calendar) | The widely used Android app «تقویم فارسی» (created by Ebrahim Byagowi) | GPL-3.0 | Real-world reference for how a mature production app handles multiple calendars, events, and time zones |
| [persian-calendar/calendar](https://github.com/persian-calendar/calendar) | The Kotlin calendar engine powering the Android app, by Ebrahim Byagowi (covers Persian, Islamic, and Nepali calendars, pre-1304 historical months, and the official lookup table) | GPL-2.0-or-later | Cross-checking leap years (`leaps.txt` covering years 1–9999), plus details on pre-1304 zodiac month lengths and Iran's lunar Hijri calendar |
| [persian-calendar/events](https://github.com/persian-calendar/events) | Machine-readable JSON dataset of official holidays and observances (Iran, Afghanistan, etc.), by Ebrahim Byagowi | CC0-1.0 | Primary data reference for [Holidays](07-holidays.md) |
| [roozbehp/qamari](https://github.com/roozbehp/qamari) | Historical and current month-start records for the Lunar Hijri calendar as observed in Iran | CC0-1.0 | Reference for solving the lunar holiday problem in [Holidays](07-holidays.md) |
| [jalaali](https://github.com/jalaali) (`jalaali-js`, `go-jalaali`, `jalaali-python`, …) | Implementations of Borkowski's algorithm across multiple languages | MIT | Evaluated in our benchmark suite; verified accurate through 1502 AP |

## Papers, books, and algorithm sources

* E. M. Reingold and N. Dershowitz, *Calendrical Calculations*, 4th ed., Cambridge University Press, 2016 (and *The Ultimate Edition*, 2018),
  Chapter "The Persian Calendar" — covers both the astronomical definition and the arithmetic 2820-year cycle.
* K. M. Borkowski, "The Persian calendar for 3000 years", *Earth, Moon, and Planets* 74 (1996), pp. 223–230:
  <https://www.astro.uni.torun.pl/~kb/Papers/EMP/PersianC-EMP.htm>
* R. Pournader, the 33-year rule with a 78-year correction table, published in [roozbehp/persiancalendar](https://github.com/roozbehp/persiancalendar)
  (`persiancalendar_fast.py`) and discussed in Unicode ICU4X issue [#4713](https://github.com/unicode-org/icu4x/issues/4713).
* ICU4J `PersianCalendar` API documentation:
  <https://unicode-org.github.io/icu-docs/apidoc/dev/icu4j/com/ibm/icu/util/PersianCalendar.html>
* Unicode ICU source history: the Persian calendar was switched to the 33-year rule in ICU 50 (ticket `ICU-8952`, 2012), and upgraded with the 78-year correction table in **ICU 77.1** (ticket `ICU-22736`, committed September 2024 and released March 2025; ICU 76.1 still used the plain 33-year rule, as confirmed in its source and by [`tools/icu-cross-check`](../../tools/icu-cross-check)):
  [`icu4c/source/i18n/persncal.cpp`](https://github.com/unicode-org/icu/blob/main/icu4c/source/i18n/persncal.cpp).
* J. Meeus, *Astronomical Algorithms*, 2nd ed., Willmann-Bell, 1998 — solar longitude, equinoxes, ΔT, and the equation of time.
* P. Bretagnon and J.-L. Simon, *Planetary Programs and Tables from −4000 to +2800*, Willmann-Bell, 1986 — the 49-term periodic solar longitude series used in our astronomical tool.
* F. Espenak and J. Meeus, "Polynomial Expressions for Delta T (ΔT)", NASA Eclipse Web Site:
  <https://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html>

## Standards & specifications

* Unicode CLDR (`persian` calendar data and locale rules for `fa`, `fa-AF`, `ps`): <https://cldr.unicode.org>
* Unicode Technical Standard #35 (LDML — locale identifiers, `-u-ca-`, `-u-nu-`): <https://unicode.org/reports/tr35/>
* IANA Time Zone Database (`Asia/Tehran`, `Asia/Kabul`): <https://www.iana.org/time-zones>
* ECMAScript `Intl` (ECMA-402) and `Temporal` specifications: <https://tc39.es/ecma402/>, <https://tc39.es/proposal-temporal/>

## Further background reading

* Wikipedia: [Solar Hijri calendar](https://en.wikipedia.org/wiki/Solar_Hijri_calendar) ·
  [گاه‌شماری هجری خورشیدی](https://fa.wikipedia.org/wiki/گاه‌شماری_هجری_خورشیدی)
* Encyclopaedia Iranica: [Calendars](https://www.iranicaonline.org/articles/calendars)

---

[← Testing](09-testing.md) · [Back to main README](../../README.md)
