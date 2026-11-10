# 4. Implementation guide

[← The algorithm](03-algorithm.md) · [Next: Localization →](05-localization.md) · [فارسی](../fa/04-implementation-guide.md)

This page is a practical architectural guide for adding the Persian calendar to a real-world app, website, backend API, or database.
We'll walk through how to choose the right tool, how to store dates without shooting yourself in the foot, how to handle time zones and date math, and finish with a pre-launch checklist.

## Step 1: decide what you actually need

```
Do you only DISPLAY dates in the Persian calendar?
├─ yes → use the platform's formatter (Intl / ICU / .NET culture / Foundation). No library, no algorithm.
└─ no, you also parse input, do date arithmetic, build a calendar UI, or compute ranges
   ├─ is there a well-tested library for your language? (see page 6)
   │   └─ yes → use it
   └─ no / you need zero dependencies / SQL / embedded
       └─ copy the implementation for your language from /implementations
          and its test, then run the test against /data
```

* **Only displaying dates?** Use your platform's built-in locale formatter (`Intl` in JavaScript, ICU in Java/C++/PHP, `fa-IR` culture in .NET, or `Foundation` on Apple platforms). You don't need any third-party library or custom algorithm.
* **Parsing user input, doing date math, building a date picker, or querying date ranges?** Check if your language has a verified library in [Languages & libraries](06-languages-and-libraries.md). If not—or if you want zero dependencies—copy the single-file implementation for your language from [`implementations/`](../../implementations) along with its test suite, and run it against [`data/`](../../data).

Most applications only need display! For example, in any browser or Node.js:

```js
new Intl.DateTimeFormat('fa-IR', { dateStyle: 'long', timeZone: 'Asia/Tehran' }).format(new Date())
// → "۱ فروردین ۱۴۰۴" (computed natively by ICU)
```

## Step 2: keep one source of truth — store Gregorian/instants

**Never store Persian date strings as your primary data in a database.** Instead, always store:

* **Instants in time** (when an order was placed, when a log event happened) as **UTC timestamps** (`timestamptz`, `Instant`, `DateTimeOffset`, or Unix timestamps);
* **Calendar dates without a time of day** (a birth date, an invoice due date) as **standard ISO / Gregorian dates** (`date`, `LocalDate`, `DateOnly`, or `YYYY-MM-DD`).

Convert to and from the Persian calendar **only at the edges of your system**—when formatting a date for the UI, or when parsing input typed by a user:

```
 user input "۱۴۰۴/۰۱/۰۱" ─parse→ 2025-03-21 ─store→ DB (date)
 DB 2025-03-21 ─format→ "۱ فروردین ۱۴۰۴" ─display→ user
```

Why is this the golden rule of calendar architecture?

* Every database engine, SQL query, index, sorting rule, and standard library already understands ISO/Gregorian dates natively.
* You can display the exact same stored data in Persian, Gregorian, or any other calendar and language without migrating your database.
* Because Persian ↔ Gregorian conversion is strictly one-to-one and deterministic, zero information is lost.

**When *should* you store a Persian date?** Only when a legal or financial document explicitly records the Persian date as part of a contract (e.g., *"contract signed on 1404/01/15"*). Even then, store it **alongside** the primary ISO date, not instead of it.
And if you ever need to index or sort by a Persian date column directly, store it as an integer `yyyymmdd` (like `14040115`)—plain strings break sorting whenever months/days lack leading zeros or mix Persian and ASCII digits.

**In APIs:** Always exchange ISO 8601 (Gregorian) dates in your JSON payloads. If a client specifically wants a pre-formatted Persian date, include it in a separate, clearly named field like `"date_fa": "1404/01/01"`. Never stuff a Persian date string into a field documented as ISO 8601!

## Step 3: get the time zone right

Just like in the Gregorian calendar, what Persian date an instant falls on depends on the **time zone**.

* **Iran:** Use the IANA time zone identifier **`Asia/Tehran`** (`UTC+3:30`).
  **Crucial note: Iran permanently abolished Daylight Saving Time (DST) by law in 2022** [[1](https://rc.majlis.ir/fa/law/show/1745106)]. After clocks were set back on 30 Shahrivar 1401 (21 September 2022), Iran has stayed on `UTC+3:30` year-round starting from Nowruz 1402 (March 2023).
  However, servers, containers, OSes, or runtimes with outdated time zone databases—anything older than [`tzdata 2022b`](https://data.iana.org/time-zones/tzdb-2022b/NEWS) from the [IANA Time Zone Database](https://www.iana.org/time-zones)—still jump ahead to `+04:30` every spring! That makes all your timestamps off by an hour for half the year and shifts dates by a full day between 23:00 and 24:00. Always keep `tzdata`, ICU, your OS, JDK, and Docker base images up to date.
* **Afghanistan:** Use `Asia/Kabul` (`UTC+4:30`).
* **Never hard-code a fixed `+03:30` offset.** Always use the IANA zone name `Asia/Tehran`, because historical timestamps before 2023 *did* observe `+04:30` in spring and summer (and `+04:00`/`+05:00` in 1977–1978).

```js
// The exact same moment in time, two different Persian dates:
const t = new Date('2025-03-20T21:00:00Z')
fmt('Asia/Tehran') // 1 Farvardin 1404   (00:30 AM local time in Tehran)
fmt('UTC')         // 30 Esfand 1403     (21:00 PM in UTC)
```

Three rules of thumb that prevent 99% of time-zone bugs:

* **"Today"** always means today's date **in the user's time zone**—determine the local Gregorian year/month/day in the user's zone first, then convert to Persian.
* **Date-only values** (like a birth date stored as `date` or `LocalDate`) have no time zone at all. Convert their `(y, m, d)` directly! Never convert a date-only value into a UTC midnight timestamp and format it in local time, or users west of the [Prime Meridian (Greenwich)](https://en.wikipedia.org/wiki/Prime_meridian_(Greenwich)) will see their birthday shift back by a day.
* In JavaScript, remember that `new Date(2025, 2, 21)` creates **local midnight**, whereas `new Date('2025-03-21')` creates **UTC midnight**. Mixing the two is a classic source of off-by-one-day bugs.

## Step 4: date arithmetic (adding days, months, and years)

Adding or subtracting **days** is effortless: convert the date to a day counter (`JDN`), add `N`, and convert back.

Adding **months** or **years**, however, requires a clear rule for what happens when the starting day of the month doesn't exist in the target month (for instance, adding 1 month to the 31st of a 31-day month when the next month only has 30 days):

| Operation | Expected Result | Rule |
|---|---|---|
| 31 Shahrivar 1404 + 1 month | **30 Mehr 1404** | Clamp the day to the maximum length of the target month |
| 30 Esfand 1403 + 1 year | **29 Esfand 1404** | Clamp to 29 (since 1404 is not a leap year) |
| 31 Farvardin + 6 months | **30 Mehr** | Clamp to 30 |
| 1 Farvardin 1404 − 1 day | 30 Esfand 1403 | Pure day-count subtraction (`JDN − 1`); no clamping rule needed |

Clamping to the end of the target month is the standard behavior in ICU, JavaScript `Temporal` (`overflow: 'constrain'`), and .NET's `PersianCalendar.AddMonths`:

```
function add_months(y, m, d, n):
    total = (y * 12 + (m - 1)) + n
    y2 = floor_div(total, 12);  m2 = floor_mod(total, 12) + 1
    return (y2, m2, min(d, days_in_month(y2, m2)))
```

**Never** implement "+1 Persian month" by converting to Gregorian, adding 1 Gregorian month, and converting back! Because Gregorian and Persian month boundaries don't align, doing that will land on the wrong day of the Persian month.

## Step 5: date ranges, reports, and database queries

Suppose a user asks for *"all orders placed in Mehr 1404"*. Convert the start and end of the Persian range into Gregorian dates in the application, and query your indexed Gregorian/UTC column:

```
start = to_gregorian(1404, 7, 1)                     # 2025-09-23
end   = to_gregorian(1404, 8, 1)                     # 2025-10-23 (exclusive upper bound!)
SELECT … WHERE created_at >= start AND created_at < end    -- evaluated in the user's time zone!
```

* **Use half-open intervals `[start, next_start)`:** By using `< first_day_of_next_month` instead of `<= last_day_of_month`, you don't even have to worry about whether the month has 29, 30, or 31 days (or where 23:59:59.999 ends).
* **Quarters** in the Persian calendar are simply months `1–3` (Spring), `4–6` (Summer), `7–9` (Autumn), and `10–12` (Winter).
* **Iran's fiscal and tax year** starts on 1 Farvardin and ends on the last day of Esfand, so the fiscal year matches the Persian calendar year.
* **For heavy BI / analytics reporting:** Create a **date dimension table** (`dim_date`) with one row per day containing the Gregorian date alongside precomputed Persian `year`, `month`, `day`, `month_name`, `weekday`, and `is_holiday`. You can populate 100 years of rows in milliseconds using any implementation in this repo. Or, on PostgreSQL, use the `IMMUTABLE` SQL functions in [`implementations/sql`](../../implementations/sql) directly in expression indexes or generated columns.

## Step 6: weeks

* In Iran and Afghanistan, the week **starts on Saturday**. Any calendar grid UI should have columns running from **شنبه (Saturday)** to **جمعه (Friday)** (right-to-left in Persian UIs).
* **Week-of-year numbering** has no single formal national standard in Iran, though a common convention defines Week 1 as the Saturday–Friday week containing 1 Farvardin. Whatever rule you use, document it clearly—and don't use ISO 8601 week numbers (which are Monday-based and tied to January).
* Keep **weekend days configurable** (Friday is the official holiday, while Thursday is also off for many organizations).

## Step 7: parsing user input

When users type a Persian date into an input field, be generous in what you accept and strict in how you validate it:

* **Digits:** Accept Persian digits `۰–۹` (`U+06F0–U+06F9`), Arabic-Indic digits `٠–٩` (`U+0660–U+0669`), and ASCII digits `0–9`. Normalize them all to ASCII before parsing.
* **Separators:** Accept `/`, `-`, `.`, and whitespace.
* **Field order:** In Persian, numeric dates are always written **Year / Month / Day** (`۱۴۰۴/۰۱/۱۵`). Never guess `D/M/Y` or `M/D/Y` for Persian dates.
* **Two-digit years** (like `04/01/15`) are ambiguous—either reject them and ask for a 4-digit year, or document a clear expansion rule (`04 → 1404`).
* **Validate the calendar date** with `is_valid(y, m, d)`: reject `1404/12/30` (since 1404 is not a leap year) and `1404/07/31` (since Mehr has 30 days).
* **Month names in text:** Normalize Arabic `ي`/`ك` to Persian `ی`/`ک` and treat ZWNJ (`U+200C`) and spaces interchangeably when matching text (see [Localization](05-localization.md)).

```python
PERSIAN_DIGITS = str.maketrans('۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩', '01234567890123456789')
def parse_persian_date(text):
    parts = re.split(r'[/\-.\s]+', text.strip().translate(PERSIAN_DIGITS))
    y, m, d = (int(p) for p in parts)
    if not is_valid_date(y, m, d):
        raise ValueError('invalid Persian date')
    return y, m, d
```

## Step 8: formatting and display

* Prefer your platform's built-in formatter with locale `fa-IR`—it gives you Persian digits and standard month names out of the box. See [Localization](05-localization.md) for important nuances (like avoiding `dateStyle: 'full'`).
* Standard display patterns in Persian UIs: `۱۴۰۴/۰۱/۰۱`, `۱ فروردین ۱۴۰۴`, and `جمعه ۱ فروردین ۱۴۰۴`. In English UIs: `1 Farvardin 1404` or `1404-01-01`.
* Whenever a date appears inside text of the opposite direction (an RTL date inside an LTR sentence, or vice versa), wrap it in a bidirectional isolate (`<bdi>`, `dir="auto"`, or Unicode `U+2068 … U+2069`) so the year, month, and day don't get visually scrambled.
* Showing the equivalent Gregorian date in a subtle tooltip or secondary label is a great UX touch for users coordinating with international teams.

## Step 9: UI components (date pickers & calendars)

If you're building or choosing a Persian date picker, make sure it gets these essentials right:

* A month grid starting on **Saturday (شنبه)**, laid out **right-to-left (RTL)**, with 31-day, 30-day, and 29/30-day months.
* Month and year navigation that steps through **Persian** months and years (never Gregorian months).
* "Today" highlighted according to the **user's time zone**.
* Emits its selected value to your application state as a standard **ISO date (`YYYY-MM-DD`) or UTC instant** (remember Step 2!).
* Keyboard input that accepts Persian, Arabic, and ASCII digits and validates leap-year Esfand 30.
* Holidays loaded from a versioned data source (see [Holidays](07-holidays.md)), never hard-coded inside the component.

## Step 10: test against ground-truth data

Test your implementation or chosen library against [`data/years.csv`](../../data/years.csv) and [`data/test-vectors.csv`](../../data/test-vectors.csv).
See [Testing](09-testing.md) for the full test plan, or start with these four smoke tests that catch the most common bugs immediately:

1. **Leap years:** Verify that `1399`, `1403`, and **`1408`** (a 5-year gap!) are leap years, and that **`1404`** and **`1407`** are *not*.
2. **Boundary around Nowruz 1404:** Verify that `30 Esfand 1403 = 2025-03-20` and `1 Farvardin 1404 = 2025-03-21`.
3. **Full round-trip:** Convert every single day in a multi-decade range from Persian → Gregorian → Persian and confirm you get the exact same date back.
4. **Time-zone midnight test:** Verify that `2025-03-20T21:00:00Z` formats as `1 Farvardin 1404` in `Asia/Tehran`, but `30 Esfand 1403` in `UTC`.

## Pre-launch checklist

- [ ] Database and APIs store and exchange **Gregorian/ISO dates or UTC timestamps**; Persian dates are formatted/parsed at the edges.
- [ ] Leap years use a verified algorithm (never the 2820-year cycle) and pass tests for **1403, 1404, 1407, and 1408**.
- [ ] Time zone uses the IANA identifier **`Asia/Tehran`**, and the server/runtime `tzdata` is **2022b or newer** (no false summer DST).
- [ ] Calendar grids start the week on **Saturday**, and weekend days are configurable.
- [ ] Input parser accepts Persian, Arabic-Indic, and ASCII digits, and rejects `30 Esfand` in non-leap years.
- [ ] Adding months/years clamps the day of the month to `days_in_month`.
- [ ] Database range queries use half-open `[start, next_start)` intervals computed from Persian month boundaries.
- [ ] Display uses `fa-IR` or CLDR names with proper ZWNJ (`سه‌شنبه`) and Persian `ی`/`ک`.
- [ ] Holidays are loaded from updatable data with a documented source, not hard-coded.
- [ ] Automated tests run against `/data`.

---

[← The algorithm](03-algorithm.md) · [Next: Localization →](05-localization.md)
