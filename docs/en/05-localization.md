# 5. Localization

[← Implementation guide](04-implementation-guide.md) · [Next: Languages & libraries →](06-languages-and-libraries.md) · [فارسی](../fa/05-localization.md)

Getting the calendar math right is only half the battle. The other half is typography and text: digits, Persian vs. Arabic characters, half-spaces (ZWNJ), right-to-left (RTL) layout, and month names.

## Standard locale identifiers

| Locale Tag | What it means |
|---|---|
| `fa-IR` | Persian (Iran). In Unicode CLDR, the default calendar for `fa` is `persian` and the default digits are Persian (`arabext`: `۰–۹`) |
| `fa-AF` | Dari (Afghanistan): uses the Persian calendar with Dari zodiac month names (`حمل`, `ثور`, …) |
| `ps-AF` | Pashto (Afghanistan): uses the Persian calendar with Pashto month names (`وری`, `غویی`, …) |
| `en-u-ca-persian` | English language using the Persian calendar (`Farvardin 1, 1404 AP`) |
| `fa-IR-u-nu-latn` | Persian language and Persian calendar, but with ASCII/Latin digits (`1404/1/1`) |
| `fa-IR-u-ca-gregory` | Persian language and Persian digits, but using the Gregorian calendar (`۲۱ مارس ۲۰۲۵`) |

The `-u-ca-…` (calendar) and `-u-nu-…` (numbering system) suffixes are standard [Unicode locale extensions](https://unicode.org/reports/tr35/#u_Extension).
They work out of the box in JavaScript `Intl`, ICU (C/C++/Java), PHP `intl`, and most modern platforms.

## What platform formatters output (ICU 78 / CLDR 48)

| Options (`Intl.DateTimeFormat`, `timeZone: 'Asia/Tehran'`) | Output for `2025-03-21` |
|---|---|
| `fa-IR`, `{}` | ۱۴۰۴/۱/۱ |
| `fa-IR`, `{ year: 'numeric', month: '2-digit', day: '2-digit' }` | ۱۴۰۴/۰۱/۰۱ |
| `fa-IR`, `{ dateStyle: 'long' }` | ۱ فروردین ۱۴۰۴ |
| `fa-IR`, `{ weekday: 'long', day: 'numeric', month: 'long' }` | جمعه ۱ فروردین |
| `fa-IR`, `{ dateStyle: 'full' }` | ۱۴۰۴ فروردین ۱, جمعه ⚠️ |
| `fa-IR-u-nu-latn`, `{ dateStyle: 'short' }` | 1404/1/1 |
| `en-u-ca-persian`, `{ dateStyle: 'full' }` | Friday, Farvardin 1, 1404 AP |
| `en-GB-u-ca-persian`, `{ day: 'numeric', month: 'long', year: 'numeric' }` | 1 Farvardin 1404 AP |
| `fa-AF`, `{ dateStyle: 'full' }` | ۱۴۰۴ حمل ۱, جمعه |

⚠️ **Watch out for `dateStyle: 'full'` in `fa-IR`:** CLDR's default `full` pattern for Persian outputs an awkward word order with an ASCII comma (`۱۴۰۴ فروردین ۱, جمعه`). Native Persian readers expect
**«جمعه ۱ فروردین ۱۴۰۴»**. You can easily assemble the natural order using `formatToParts`:

```js
const parts = (d) => Object.fromEntries(new Intl.DateTimeFormat('fa-IR', {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Asia/Tehran',
}).formatToParts(d).map((p) => [p.type, p.value]))
const p = parts(new Date())
const text = `${p.weekday} ${p.day} ${p.month} ${p.year}`   // "جمعه ۱ فروردین ۱۴۰۴"
```

## Digits: Persian vs. Arabic-Indic vs. ASCII

| Digit Set | Characters | Unicode Range | When to use |
|---|---|---|---|
| **Persian** (Extended Arabic-Indic, `arabext`) | ۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹ | `U+06F0 – U+06F9` | **All Persian/Dari/Pashto UI text** |
| **Arabic-Indic** (`arab`) | ٠ ١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩ | `U+0660 – U+0669` | Arabic text only; **never** display these in Persian UIs |
| **ASCII / Latin** (`latn`) | 0 1 2 3 4 5 6 7 8 9 | `U+0030 – U+0039` | Databases, JSON APIs, URLs, logs, and English UIs |

* Notice that **۴, ۵, and ۶** (`U+06F4..U+06F6`) have different glyphs from Arabic **٤, ٥, and ٦** (`U+0664..U+0666`). Showing Arabic digits in a Persian app immediately looks foreign to Persian users.
* **When parsing input:** Always accept all three digit sets and normalize them to ASCII `0–9` before calling `parseInt()` or `int()` (many mobile keyboards emit Arabic-Indic digits depending on the user's keyboard layout).
* **Years are not quantities:** Never format a year with a plain number formatter! Calling `Intl.NumberFormat('fa-IR').format(1404)` inserts a thousands separator (`«۱٬۴۰۴»`). Either pass `{ useGrouping: false }` or format the date with `Intl.DateTimeFormat`.
* **In databases, APIs, logs, and filenames:** Always use plain ASCII digits (`0–9`).

```js
const toAscii = (s) => s.replace(/[۰-۹]/g, (c) => c.charCodeAt(0) - 0x06F0).replace(/[٠-٩]/g, (c) => c.charCodeAt(0) - 0x0660)
const toPersian = (s) => String(s).replace(/\d/g, (c) => '۰۱۲۳۴۵۶۷۸۹'[c])
```

## Persian vs. Arabic letters: ی and ک

Standard Persian uses **ی** (`U+06CC`, Arabic Letter Farsi Yeh) and **ک** (`U+06A9`, Arabic Letter Keheh).
However, Arabic keyboards, older Windows layouts, and some external datasets produce **ي** (`U+064A`, Arabic Yeh with two dots underneath in final form) and **ك** (`U+0643`, Arabic Kaf).
To a human reader they look almost identical in many fonts, but to your code `"دي" !== "دی"` and `"يكشنبه" !== "یکشنبه"`.

* **In your output:** Always emit standard Persian `ی` (`U+06CC`) and `ک` (`U+06A9`). (Every string in this repository and in Unicode CLDR follows this rule.)
* **In your input & search:** Normalize `ي → ی`, `ى → ی`, and `ك → ک` before comparing strings or looking up month and weekday names.

## Zero-width non-joiner (ZWNJ / نیم‌فاصله)

The Persian word for Tuesday—**«سه‌شنبه»**—contains a **Zero-Width Non-Joiner** (`U+200C`, ZWNJ, known in Persian as *nim-faseleh*) between «سه» and «شنبه».
Without it, the letters either join together into an ugly ligature («سهشنبه») or get separated by a full space («سه شنبه»).
Always output the proper ZWNJ form (`سه‌شنبه`), and when parsing user input, accept all three variants (ZWNJ, regular space, or no space).

## Right-to-left (RTL) text and bidirectional (BiDi) layout

Persian text flows **right to left (RTL)**, while numbers inside it flow **left to right (LTR)**.
When a date like `۱۴۰۴/۰۱/۰۱` sits inside a purely RTL paragraph, the Unicode BiDi algorithm renders it properly on its own.
Trouble starts when a date is embedded next to text of the **opposite direction**—for example, a Persian date inside an English sentence, an ASCII date `1404/01/01` inside a Persian sentence, or a date next to an LTR icon or punctuation mark. Suddenly, the slashes and numbers jump around and render as `01/01/1404`!

How to prevent BiDi scrambling:

* **In HTML:** Wrap dates in a bidirectional isolate element: `<bdi>۱۴۰۴/۰۱/۰۱</bdi>` or `<span dir="rtl">…</span>`.
* **In plain text (notifications, SMS, emails, terminal output):** Wrap the date in Unicode directional isolate characters: `U+2068` (First Strong Isolate) before the date, and `U+2069` (Pop Directional Isolate) after it.
* **Page & grid layout:** Set `dir="rtl"` and `lang="fa"` on Persian pages, and mirror your calendar grid so **Saturday (شنبه)** sits in the rightmost column and **Friday (جمعه)** sits in the leftmost column.

## Month and weekday names across 11 languages

The complete set of localized month names, weekday names, and era strings across 11 locales (Persian, Dari, Pashto, Central Kurdish, English, Arabic, Turkish, Azerbaijani, German, French, and Russian)—including standalone, formatting, and abbreviated forms—is available in [`data/locale-names.json`](../../data/locale-names.json),
generated directly from Unicode CLDR 48 by [`tools/generate_locale_data.mjs`](../../tools/generate_locale_data.mjs).

* **English transliteration:** You'll see slight spelling variations in the wild (`Farvardin`/`Farwardin`, `Mordad`/`Amordad`, `Dey`/`Dei`, `Esfand`/`Isfand`). Unless your organization has its own house style, stick to the **CLDR standard**: `Farvardin`, `Ordibehesht`, `Khordad`, `Tir`, `Mordad`, `Shahrivar`, `Mehr`, `Aban`, `Azar`, `Dey`, `Bahman`, `Esfand`.
* **Mordad vs. Amordad:** The 5th month is traditionally also called «امرداد» (*Amordad*). Both CLDR and Iran's official calendar use **«مرداد»** (*Mordad*).

## Era abbreviations and ordinal days

| Language | Era suffix | Example |
|---|---|---|
| Persian | **ه‍.ش** (هجری شمسی), or occasionally **ش** | ۱۴۰۴ ه‍.ش |
| English | **AP** (*Anno Persico*) or **SH** (*Solar Hijri*) | 1404 AP |

*(Tip: Notice the Zero-Width Joiner `U+200D` after `ه` in «ه‍.ش»—that's the standard CLDR representation so `ه` renders in its initial joining form `ه‍` rather than an isolated `ه`.)*

In spoken Persian and formal invitations, days of the month are often written as ordinals: «یکم فروردین» (or «اول فروردین»), «دوم اردیبهشت», «سوم خرداد», and so on. Standard numeric/long CLDR formats use cardinal numbers (`۱ فروردین`).

## Localization checklist

- [ ] Persian UI uses locale `fa-IR`, Persian digits (`۰–۹`), RTL layout, and Saturday as the first day of the week.
- [ ] Input parsers accept Persian (`۰–۹`), Arabic-Indic (`٠–٩`), and ASCII (`0–9`) digits, and normalize `ي`/`ك` to `ی`/`ک`.
- [ ] Years are never formatted with a thousands separator (`۱۴۰۴`, not `۱٬۴۰۴`).
- [ ] Dates embedded in mixed-direction (RTL/LTR) text are wrapped in `<bdi>` or `U+2068…U+2069` isolates.
- [ ] «سه‌شنبه» uses a proper ZWNJ (`U+200C`), and all Persian names use `U+06CC` (`ی`) and `U+06A9` (`ک`).

---

[← Implementation guide](04-implementation-guide.md) · [Next: Languages & libraries →](06-languages-and-libraries.md)
