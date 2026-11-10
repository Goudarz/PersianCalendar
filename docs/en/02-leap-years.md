# 2. Leap years

[← Basics](01-basics.md) · [Next: The algorithm →](03-algorithm.md) · [فارسی](../fa/02-leap-years.md)

Leap years are the single trickiest part of the Persian calendar—and the source of almost every bug you'll encounter in existing libraries.

## The official rule is astronomical, not arithmetic

Unlike the Gregorian calendar (which uses a fixed 4/100/400-year math rule), the Solar Hijri calendar has **no official arithmetic formula**. Its official definition is purely astronomical:

> **1 Farvardin (Nowruz) is the day on which the March equinox occurs before true (apparent) solar noon on the
> 52.5° E meridian** (the reference meridian for Iran Standard Time, `UTC+3:30`). If the equinox happens at or after
> true noon, Nowruz falls on the following day.

A year is a **leap year** (366 days long, with a 30-day Esfand) whenever there are 366 days between its Nowruz and the next year's Nowruz.

In Iran, the **Calendar Center of the Institute of Geophysics at the University of Tehran** (مرکز تقویم مؤسسه ژئوفیزیک دانشگاه تهران)
is the official body that runs these astronomical calculations and publishes the calendar. They have published an official leap-year table covering **1206–1498 AP** [[1](https://calendar.ut.ac.ir/documents/2139738/7092644/Kabise+Shamsi+1206-1498.pdf)].
A plain-text transcription of that official document is included in this repository at [`data/official/kabise-1206-1498.txt`](../../data/official/kabise-1206-1498.txt).

### Real-world example: why 1403 was a leap year

Let's look at the exact spring equinox times for 1403–1405 AP:

| Persian Year | Spring Equinox (`Asia/Tehran`) | Before true noon? | 1 Farvardin (Nowruz) |
|---|---|---|---|
| 1403 | 2024-03-20 at 06:36 | Yes | 2024-03-20 |
| 1404 | 2025-03-20 at **12:31** | **No** (after noon) | 2025-03-**21** |
| 1405 | 2026-03-20 at 18:15 | No | 2026-03-21 |

Nowruz 1403 fell on 20 March 2024, while Nowruz 1404 was pushed to 21 March 2025 because the 2025 equinox occurred about half an hour after noon.
That makes the span between them **366 days**—which means **1403 was a leap year** (with a 30 Esfand 1403 on 20 March 2025), and **1404 is a normal 365-day year**.
Notice how close the 2025 equinox was to noon: any approximate formula that drifts by even half an hour gets 1403 and 1404 wrong!

*(You can reproduce the equinox times above using this repository's astronomical tool [`tools/calendar-data`](../../tools/calendar-data):
`cargo run --release -- equinox 2024 2025 2026`. They match the officially announced turn-of-the-year moments to within 30 seconds.)*

## The natural pattern: 8 leap years every 33 years (mostly)

The average time between two spring equinoxes (the vernal equinox year) is about **365.2424 days**. Adding 8 leap days every 33 years gives an average year length of
`365 + 8/33 = 365.242424…` days—an remarkably close match!

Because of how Earth's orbit and `8/33` work out, leap years usually happen **every 4 years**, and once per ~33-year cycle the gap stretches to **5 years** (a *panje* or 5-year leap interval):

```
Leap years in 1300–1500 AP:
1300 1304 1309 1313 1317 1321 1325 1329 1333 1337 1342 1346 1350 1354 1358
1362 1366 1370 1375 1379 1383 1387 1391 1395 1399 1403 1408 1412 1416 1420
1424 1428 1432 1436 1441 1445 1449 1453 1457 1461 1465 1469 1474 1478 1482
1486 1490 1494 1498
```

Look at where the 5-year gaps fall: `1304→1309`, `1337→1342`, `1370→1375`, **`1403→1408`**, `1436→1441`, `1469→1474`.
Notice that **right now we are in the middle of a 5-year gap**: 1403 was a leap year, 1407 is *not*, and the next leap year is **1408**!
Across the entire 1178–3000 AP range, there are 386 four-year gaps and 55 five-year gaps
(see [`data/leap-years.txt`](../../data/leap-years.txt) for the full list).

### The 33-year arithmetic rule

A single line of integer math captures that 33-year pattern:

```
leap(y) = ((25 × y + 11) mod 33) < 8          (where mod is floor modulo, always 0..32)
```

This simple formula agrees with astronomy for every single year from **1178 to 1501 AP**, meaning every date from 1178 AP through
**29 Esfand 1502 (19 March 2124)** is 100% accurate.

Where does it first miss? In **1502 AP**: the 33-year formula says 1502 is a leap year, whereas astronomically the leap year is delayed by one year to **1503 AP**.
Why? Because `365.242424…` is still a tiny bit longer than the actual spring equinox year, so over the centuries the formula slowly drifts relative to true solar noon.

### The 78-year correction table (Pournader, 2024)

Roozbeh Pournader discovered a neat property: across the entire span from **1178 to 3000 AP**, every single disagreement between the plain 33-year rule and true astronomy has the exact same shape—the 33-year rule predicts the leap year in year `y`, while the actual astronomical leap year happens one year later in `y + 1`.

That means we only need a list of those **78 years** where the leap year gets pushed back by a year:

```
1502 1601 1634 1667 1700 1733 1766 1799 1832 1865 1898 1931 1964 1997 2030 2059
2063 2096 2129 2158 2162 2191 2195 2224 2228 2257 2261 2290 2294 2323 2327 2356
2360 2389 2393 2422 2426 2455 2459 2488 2492 2521 2525 2554 2558 2587 2591 2620
2624 2653 2657 2686 2690 2719 2723 2748 2752 2756 2781 2785 2789 2818 2822 2847
2851 2855 2880 2884 2888 2913 2917 2921 2946 2950 2954 2979 2983 2987
```

With that set (`CORRECTIONS`), the leap-year check becomes three lines:

```
if y in CORRECTIONS:       not leap
elif y - 1 in CORRECTIONS: leap
else:                      ((25 × y + 11) mod 33) < 8
```

Our astronomical tool ([`tools/calendar-data`](../../tools/calendar-data) → [`data/corrections.txt`](../../data/corrections.txt)) independently computes the astronomical calendar from scratch and arrives at the exact same 78 years.
Today, **ICU 77.1+** (which powers C/C++, Java, modern browsers, Node.js 24+, PHP `intl`, etc.), **ICU4X** (Rust), and **Temporal** all use this exact algorithm.
(ICU 76.1 and older use the plain 33-year rule without the correction table—which we verified day by day in [`tools/icu-cross-check`](../../tools/icu-cross-check).)
**This 33-year rule + 78-year correction table is the algorithm recommended throughout this guide.**

## Comparing the algorithms you'll see in the wild

| Algorithm | Accurate range | Used by | Verdict |
|---|---|---|---|
| **33-year rule + correction table** (Pournader) | **1178–3000 AP** (1799–3621 CE) | ICU ≥ 77.1, ICU4X, Temporal, `persiantools`, all code in this repo | ✅ **Recommended** |
| **Plain 33-year rule** | 1178 AP – 29 Esfand 1502 (March 2124) | ICU 50–76 | ✅ Great for any date before 2124 |
| **Borkowski (1996)** (break-year table for 33-year subcycles) | 753 AP – 29 Esfand 1502 (March 2124) | `jalaali-js` and its ports, `jdatetime`, `moment-jalaali`, `date-fns-jalali`, `shamsi_date`, `morilog/jalali`, `go-jalaali` | ✅ Great for any date before 2124 |
| **.NET `PersianCalendar`** (internal astronomical table) | Up to 29 Esfand 1535 (March 2157) | .NET Framework 4.6+, .NET Core / .NET 5+ | ✅ Great for any date before 2157 |
| **2820-year cycle (Birashk)** | **Only 1404–1435 AP** in the current era | `khayyam` (Python), older textbooks, many copy-pasted blog snippets | ❌ **Avoid!** Already wrong for **1403/1404**, and fails again in 1436/1437, 1469/1470… |
| **Official leap-year lookup table** | Whatever years are in the table | `persian-calendar` Android app (table for 1304–1498) | ✅ 100% exact inside the table, needs a fallback outside it |
| **Full astronomical calculation** | Any era (within ΔT uncertainty) | *Calendrical Calculations*, `roozbehp/persiancalendar`, [`tools/calendar-data`](../../tools/calendar-data) | ✅ Ground truth for generating test data, but too heavy for everyday runtime use |

All of the accuracy ranges above were verified by automated tests in this repository; see [Languages & libraries](06-languages-and-libraries.md) and
[`tools/library-evaluation`](../../tools/library-evaluation).

### Why you must avoid the 2820-year cycle

In the 1970s, Ahmad Birashk proposed a purely arithmetic calendar based on a grand **2820-year cycle** (made of 128-year and 132-year subcycles). Because it was easy to code and appeared in early articles (including the arithmetic section of *Calendrical Calculations*), it spread across the internet.

However, **the 2820-year cycle was never adopted as Iran's official calendar**, and it disagrees with the official calendar right now:

| Persian Year | Official (Astronomical) Calendar | 2820-Year Cycle (Birashk) |
|---|---|---|
| **1403** | **Leap year** (30 Esfand 1403 = 20 March 2025) | Common year (29 days in Esfand) |
| **1404** | Common year (Nowruz = 21 March 2025) | **Leap year** (Nowruz = 20 March 2025) |

Any software using the 2820-year cycle was **off by one full day for every single date between 20 March 2025 and 20 March 2026**! For instance, the Python library `khayyam` (tested on v3.0.17) suffers from this exact bug.

## How far into the future can *anyone* know for sure?

Astronomical leap-year calculations depend on two things: Earth's orbital position and **ΔT** (the cumulative drift between atomic/uniform time and Earth's slightly irregular rotation speed).
Because small changes in Earth's rotation (from tidal friction, glacial rebound, and core-mantle dynamics) cannot be predicted to the exact second centuries in advance, *any* calendar table for the distant future (say, 2500 AP) is an astronomical best estimate.

What does that mean in practice for your code?

* **1300–1500 AP (1921–2122 CE):** Every good algorithm (33-year, Borkowski, .NET, ICU, and full astronomy) agrees 100% with the official Calendar Center table. **You have zero ambiguity here.**
* **After 29 Esfand 1502 (March 2124):** The plain 33-year rule and Borkowski's algorithm start drifting; use the 78-year correction table (or ICU 77.1+ / ICU4X / Temporal) if you need dates beyond 2124.
* **Knife-edge years in the distant future:** Occasionally, the spring equinox lands within seconds of true noon in Iran—for example, in **1536 AP** (31 seconds before noon), **2027 AP** (39 seconds), **2093 AP** (42 seconds), and **2584 AP** (just 6 seconds!). Slight differences in solar ephemeris models or ΔT polynomials can flip those borderline years by a day (which is why .NET's internal table first differs in 1535/1536 AP; see [the full analysis](../../tools/calendar-data#how-certain-is-the-future)).
* **Beyond 3000 AP:** Treat any date as an extrapolation, and document the supported range of your software.

---

[← Basics](01-basics.md) · [Next: The algorithm →](03-algorithm.md)
