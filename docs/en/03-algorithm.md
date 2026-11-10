# 3. The algorithm

[← Leap years](02-leap-years.md) · [Next: Implementation guide →](04-implementation-guide.md) · [فارسی](../fa/03-algorithm.md)

Here is the complete, self-contained specification of the algorithm used by all 14 [implementations in this repository](../../implementations).
It uses pure integer math, needs no lookup tables other than the 78 correction years, and takes about 60 lines of code in any language.

> **Do you need to write this yourself?** If your language already has a well-tested library or built-in support (check [Languages & libraries](06-languages-and-libraries.md)), just use that!
> You only need to implement or copy this algorithm when you want zero external dependencies, when your language lacks a reliable library (like Ruby or C), or when you're working inside SQL or an embedded environment.

## The core idea: convert through a day counter

Never try to convert Persian `(year, month, day)` directly to Gregorian `(year, month, day)`. Instead, convert both calendars to a single, continuous **day number** and back:

```
Persian date  ──to_jdn──▶  JDN  ──jdn_to_gregorian──▶  Gregorian date
Persian date  ◀─from_jdn──  JDN  ◀─gregorian_to_jdn──  Gregorian date
```

We use the **Julian Day Number (JDN)**, a standard continuous day count from astronomy where `JDN 0` is 1 January 4713 BCE in the proleptic Julian calendar.
In truth, *any* continuous day counter works (`Unix epoch days`, .NET's `DayNumber`, or Rata Die)—as long as you use the same counter in both directions, they only differ by a fixed offset:

| Day counter | How to get the JDN for the same date |
|---|---|
| Unix days (`LocalDate.toEpochDay()`, days since `1970-01-01`) | `unix_days + 2440588` |
| .NET `DateOnly.DayNumber` (days since `0001-01-01`) | `day_number + 1721426` |
| Rata Die (R.D., used in *Calendrical Calculations*) | `rd + 1721425` |
| PostgreSQL `date` | `(d - DATE '2000-01-01') + 2451545` |
| Ruby `Date#jd`, PHP `gregoriantojd()` | Already a JDN |

Converting through a day number also makes finding the **day of the week**, calculating **days between two dates**, or **adding N days** trivial: they become simple integer addition and subtraction on `jdn`.

## Floor division: the one trap

Every formula below relies on **floor division** (`⌊a / b⌋`) and **floor modulo**, where division always rounds *down toward −∞* (and the remainder `a mod b` for `b > 0` is always in `0 .. b−1`).

In many programming languages (C, C++, Java, C#, Go, Rust, JavaScript, PHP, Swift, SQL), the standard `/` and `%` operators on integers **truncate toward zero** instead. For positive numbers, floor division and truncating division give the exact same answer—which means your tests for modern dates will pass!—but as soon as an intermediate value or year estimate becomes negative, truncating division rounds the wrong way and produces off-by-one bugs.

Here is how to do true floor division and floor modulo in each language:

| Language | Floor division `⌊a / b⌋` (for `b > 0`) | Floor modulo `a mod b` |
|---|---|---|
| Python, Ruby | `a // b` (Python), `a / b` (Ruby) | `a % b` |
| JavaScript / TypeScript | `Math.floor(a / b)` | `a - b * Math.floor(a / b)` |
| Java | `Math.floorDiv(a, b)` | `Math.floorMod(a, b)` |
| Kotlin | `a.floorDiv(b)` | `a.mod(b)` |
| Rust | `a.div_euclid(b)` (identical for `b > 0`) | `a.rem_euclid(b)` |
| C, C++, C#, Go, PHP, Swift, SQL | Helper: `q = a / b; if (a % b != 0 && (a < 0) != (b < 0)) q--;` | `a - b * floor_div(a, b)` |
| Dart | `(a - a % b) ~/ b` (Dart's `%` is already Euclidean for `b > 0`) | `a % b` |

Even standard 32-bit signed integers are plenty wide for the entire accepted range of years `−100000` to `100000` (the largest intermediate expression, `33 × (jdn − EPOCH)`, stays around `1.2 × 10⁹`, safely below `2³¹ − 1`). Still, use 64-bit integers if your language makes them convenient so untrusted inputs can't overflow before validation.

## Constants

```
EPOCH = 1948320        # Calibration constant: the JDN that the 33-year formula assigns to 1 Farvardin 1
                       # (Note: the astronomical Nowruz of year 1 AP was actually JDN 1948321, one day later)
CORRECTIONS = {1502, 1601, 1634, …, 2987}   # The 78 years from data/corrections.txt
```

## 1. Checking for a leap year

```
function is_leap_year(y):
    if y in CORRECTIONS:       return false
    if y - 1 in CORRECTIONS:   return true
    return floor_mod(25*y + 11, 33) < 8
```

## 2. Days in a month & date validation

```
function days_in_month(y, m):          # m = 1..12
    if m <= 6:  return 31
    if m <= 11: return 30
    return is_leap_year(y) ? 30 : 29

function is_valid(y, m, d):
    return 1 <= m <= 12 and 1 <= d <= days_in_month(y, m)
```

## 3. Persian date → JDN

```
function nowruz_jdn(y):                # JDN of 1 Farvardin of year y
    jdn = EPOCH + 365*(y - 1) + floor_div(8*y + 21, 33)
    if y - 1 in CORRECTIONS: jdn = jdn - 1
    return jdn

function days_before_month(m):         # Total days in months 1 .. m-1
    return m <= 7 ? 31*(m - 1) : 30*(m - 1) + 6

function to_jdn(y, m, d):
    require is_valid(y, m, d)
    return nowruz_jdn(y) + days_before_month(m) + d - 1
```

How does `nowruz_jdn(y)` work? Under the pure 33-year cycle, `floor_div(8y + 21, 33)` counts how many leap days occurred before year `y`.
And if `y − 1` is one of the 78 correction years (meaning the 33-year cycle added a leap day in `y − 1` that actually belongs to year `y`), we subtract `1` so year `y` starts a day earlier (making `y − 1` 365 days and `y` 366 days).

## 4. JDN → Persian date

```
function from_jdn(jdn):
    y = 1 + floor_div(33*(jdn - EPOCH) + 3, 12053)    # Initial estimate (12053 = days in 33 years)
    while jdn >= nowruz_jdn(y + 1): y = y + 1          # Adjust by at most ±1 year near Nowruz
    while jdn <  nowruz_jdn(y):     y = y - 1
    doy = jdn - nowruz_jdn(y)                          # 0-based day of year (0..365)
    if doy < 186:                                      # First 6 months have 31 days each (6 × 31 = 186)
        return (y, floor_div(doy, 31) + 1, doy mod 31 + 1)
    doy = doy - 186                                    # Remaining months have 30 days (29/30 for Esfand)
    return (y, floor_div(doy, 30) + 7, doy mod 30 + 1)
```

Why the two `while` loops? Because of the 78-year corrections, the linear year estimate `y` can occasionally be off by 1 year right on the boundary of Nowruz. Each loop runs at most once, guaranteeing that `nowruz_jdn(y) ≤ jdn < nowruz_jdn(y + 1)` for every day in the entire range—so `from_jdn` can never produce an invalid date like "31 Esfand".

## 5. Gregorian ↔ JDN (proleptic Gregorian calendar)

```
function gregorian_to_jdn(y, m, d):
    a = floor_div(14 - m, 12)
    yy = y + 4800 - a
    mm = m + 12*a - 3
    return d + floor_div(153*mm + 2, 5) + 365*yy + floor_div(yy, 4)
             - floor_div(yy, 100) + floor_div(yy, 400) - 32045

function jdn_to_gregorian(jdn):
    a = jdn + 32044
    b = floor_div(4*a + 3, 146097)
    c = a - floor_div(146097*b, 4)
    d = floor_div(4*c + 3, 1461)
    e = c - floor_div(1461*d, 4)
    m = floor_div(5*e + 2, 153)
    day   = e - floor_div(153*m + 2, 5) + 1
    month = m + 3 - 12*floor_div(m, 10)
    year  = 100*b + d - 4800 + floor_div(m, 10)
    return (year, month, day)
```

If your language's standard library already gives you a day counter on its native date type (`LocalDate.toEpochDay` in Java/Kotlin/Scala, `DateOnly.DayNumber` in C#, `Date#jd` in Ruby, etc.), you can use that directly and skip these two Gregorian functions.

## 6. Day of the week and day of the year

```
function day_of_week(jdn):     return floor_mod(jdn + 2, 7)     # 0 = Saturday … 6 = Friday
function day_of_year(y, m, d): return days_before_month(m) + d  # 1 … 366
```

## Worked example: converting 21 March 2025

Let's trace `2025-03-21` step by step:

1. **Gregorian → JDN:** `gregorian_to_jdn(2025, 3, 21)` = **2460756**.
2. **Estimate Persian year:** `1 + floor_div(33 × (2460756 − 1948320) + 3, 12053)` = `1 + 1403` = **1404**.
3. **Check Nowruz boundaries:** `nowruz_jdn(1404)` = `1948320 + 365 × 1403 + floor_div(11253, 33)` = `1948320 + 512095 + 341` = **2460756**
   (1403 is not in `CORRECTIONS`, so no `- 1` adjustment). And `nowruz_jdn(1405)` = `2461121 > 2460756`, so the year is confirmed as **1404**.
4. **Find month and day:** `doy = 2460756 − 2460756 = 0`. Since `0 < 186`, `month = (0 div 31) + 1 = 1` and `day = (0 mod 31) + 1 = 1`.
5. **Final result:** **1 Farvardin 1404**, and the weekday is `(2460756 + 2) mod 7 = 6` → **Friday**. ✔

Similarly, for **23 September 2025**: `JDN = 2460942` → `doy = 2460942 − 2460756 = 186`. Since `186 ≥ 186`, we subtract 186 to get `0`, giving `month = 0 + 7 = 7` and `day = 1` → **1 Mehr 1404**.

## Range

| Persian Year Range (AP) | Gregorian Equivalent | Accuracy & Status |
|---|---|---|
| **1304 – 1498** | 1925 – 2120 | Matches the official University of Tehran Calendar Center table 100% (the official table covers 1206–1498) |
| **1178 – 3000** | 1799 – 3621 | Verified day by day (all 665,837 days) against the full astronomical calculation ([`data/years.csv`](../../data/years.csv)) |
| **< 1178 or > 3000** | | Mathematically consistent (every date round-trips cleanly), but not guaranteed to match astronomical equinoxes |
| **< 1304** | Before March 1925 | Historical documents before 1304 AP used variable zodiac month lengths (29–32 days); do not use modern fixed month lengths for historical conversion |

### Input limits

Always enforce a clear, bounded input range and reject anything outside it with an explicit error. Every implementation in this repository accepts Persian years
**−100000 to 100000** (which corresponds to `JDN −34,576,287` to `38,472,562`, or Gregorian `−99379-03-29` to `100622-03-13`).

Why pick `−100000 … 100000`? Beyond being vastly wider than any real-world application needs, it has a great engineering property: every intermediate value in the formulas above stays safely below `2³¹` (the largest is `33 × (jdn − EPOCH)`, around `1.2 × 10⁹`). That means the math cannot overflow even in 32-bit signed integers or JavaScript floating-point numbers, and within that entire 73,048,850-day span, the initial year estimate in `from_jdn` is never off by more than 1 year.

If you don't bound your inputs, nasty edge-case bugs creep in:
* **64-bit integer overflow** on huge day numbers—which can silently return a wrong date or, in Java, trap the adjustment `while` loop in an infinite loop!
* **Silent precision loss** in JavaScript for integers above `2⁵³` (`Number.MAX_SAFE_INTEGER`), where `x + 1 === x`.
* **Fractional inputs** in dynamically typed languages (like passing `2460756.5`), which produce nonsensical fractional dates if you don't check `Number.isInteger()`.
* **Invalid Gregorian dates:** The classic `gregorian_to_jdn` formula does not validate its inputs on its own—if you pass `2025-02-30`, it will silently convert it as `2025-03-02`! Always validate Gregorian `(y, m, d)` first.

## Alternative approaches

* **Borkowski's algorithm** (used by `jalaali-js` and its many ports): uses a 20-element table of "break" years to track where the 33-year subcycles shift.
  It is completely accurate until **29 Esfand 1502 (19 March 2124)**. If your project already uses a library based on Borkowski, there's no rush to rip it out—but if you're writing new code, the 33-year rule + 78-year correction table above is simpler and lasts a thousand years longer.
* **Direct lookup table:** You can store the Nowruz JDN (or leap-year flag) for each year in your target range—for instance, straight from [`data/years.csv`](../../data/years.csv).
  It's fast and trivial to understand, though you still need a fallback outside the table.
* **Full astronomical calculation** ([`tools/calendar-data`](../../tools/calendar-data) in Rust): computes true solar longitude (using a 49-term periodic series), the equation of time, and ΔT around every spring equinox. It is the ultimate ground truth for **generating test data**, but you shouldn't run floating-point orbital mechanics in production just to format a date.

---

[← Leap years](02-leap-years.md) · [Next: Implementation guide →](04-implementation-guide.md)
