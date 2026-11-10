// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/**
 * Solar Hijri (Persian, Jalali, Shamsi) calendar.
 *
 * Algorithm: 33-year arithmetic rule + 78-year correction table,
 * the same algorithm as ICU 77.1+ and ICU4X.
 * Matches the astronomical calendar (52.5 E meridian) for 1178-3000 AP.
 *
 * Months are 1-based (1 = Farvardin). Weekdays: 0 = Saturday ... 6 = Friday.
 * Inputs must be integers within MIN_YEAR..MAX_YEAR (or MIN_JDN..MAX_JDN):
 * non-integers throw TypeError, values out of range throw RangeError.
 */

export interface PersianDate {
  readonly year: number
  /** 1 = Farvardin ... 12 = Esfand */
  readonly month: number
  readonly day: number
}

export interface GregorianDate {
  readonly year: number
  /** 1 = January ... 12 = December (not 0-based like JS Date) */
  readonly month: number
  readonly day: number
}

/** Years checked against the astronomical calendar. */
export const VERIFIED_MIN_YEAR = 1178
export const VERIFIED_MAX_YEAR = 3000

/** Accepted input range. Within it every intermediate value fits in 32 bits. */
export const MIN_YEAR = -100000
export const MAX_YEAR = 100000
export const MIN_JDN = -34576287 // 1 Farvardin -100000 (Gregorian -99379-03-29)
export const MAX_JDN = 38472562 // 29 Esfand 100000 (Gregorian 100622-03-13)

const EPOCH_JDN = 1948320
const GREGORIAN_MIN_YEAR = -99379
const GREGORIAN_MAX_YEAR = 100622

/** Years the 33-year rule calls leap but are not; the next year is leap instead. */
export const NON_LEAP_CORRECTION: readonly number[] = Object.freeze([
  1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
  1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
  2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
  2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
  2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
  2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
  2946, 2950, 2954, 2979, 2983, 2987,
])
const CORRECTIONS: ReadonlySet<number> = new Set(NON_LEAP_CORRECTION) // private

export const MONTH_NAMES_FA = Object.freeze(['فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور',
  'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'] as const)
export const MONTH_NAMES_EN = Object.freeze(['Farvardin', 'Ordibehesht', 'Khordad', 'Tir', 'Mordad', 'Shahrivar',
  'Mehr', 'Aban', 'Azar', 'Dey', 'Bahman', 'Esfand'] as const)
export const WEEKDAY_NAMES_FA = Object.freeze(['شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه', 'جمعه'] as const)
export const WEEKDAY_NAMES_EN = Object.freeze(['Saturday', 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'] as const)

// Floor division / modulo: JS % and Math.trunc round toward zero.
const fdiv = (a: number, b: number): number => Math.floor(a / b)
const fmod = (a: number, b: number): number => a - b * Math.floor(a / b)

function checkInt (name: string, value: unknown): asserts value is number {
  if (!Number.isInteger(value)) throw new TypeError(`${name} must be an integer: ${String(value)}`)
}

function checkYear (year: number): void {
  checkInt('year', year)
  if (year < MIN_YEAR || year > MAX_YEAR) throw new RangeError(`year out of range [${MIN_YEAR}, ${MAX_YEAR}]: ${year}`)
}

function checkJdn (jdn: number): void {
  checkInt('jdn', jdn)
  if (jdn < MIN_JDN || jdn > MAX_JDN) throw new RangeError(`jdn out of range [${MIN_JDN}, ${MAX_JDN}]: ${jdn}`)
}

/** True if the Persian year has 366 days (Esfand has 30 days). */
export function isLeapYear (year: number): boolean {
  checkYear(year)
  if (CORRECTIONS.has(year)) return false
  if (CORRECTIONS.has(year - 1)) return true
  return fmod(25 * year + 11, 33) < 8
}

/** 29, 30 or 31. */
export function daysInMonth (year: number, month: number): number {
  checkYear(year)
  checkInt('month', month)
  if (month < 1 || month > 12) throw new RangeError(`month out of range [1, 12]: ${month}`)
  if (month <= 6) return 31
  if (month <= 11) return 30
  return isLeapYear(year) ? 30 : 29
}

/** True for a real Persian date in the accepted range; never throws. */
export function isValidDate (year: number, month: number, day: number): boolean {
  return Number.isInteger(year) && Number.isInteger(month) && Number.isInteger(day) &&
    year >= MIN_YEAR && year <= MAX_YEAR &&
    month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth(year, month)
}

function checkDate (year: number, month: number, day: number): void {
  if (isValidDate(year, month, day)) return
  checkInt('year', year)
  checkInt('month', month)
  checkInt('day', day)
  throw new RangeError(`invalid Persian date: ${year}/${month}/${day}`)
}

function nowruzJdn (year: number): number {
  let jdn = EPOCH_JDN + 365 * (year - 1) + fdiv(8 * year + 21, 33)
  if (CORRECTIONS.has(year - 1)) jdn -= 1
  return jdn
}

const daysBeforeMonth = (month: number): number => month <= 7 ? 31 * (month - 1) : 30 * (month - 1) + 6

/** Julian Day Number of a Persian date. */
export function toJdn (year: number, month: number, day: number): number {
  checkDate(year, month, day)
  return nowruzJdn(year) + daysBeforeMonth(month) + day - 1
}

/** Persian date of a Julian Day Number. */
export function fromJdn (jdn: number): PersianDate {
  checkJdn(jdn)
  let year = 1 + fdiv(33 * (jdn - EPOCH_JDN) + 3, 12053)
  while (jdn >= nowruzJdn(year + 1)) year++
  while (jdn < nowruzJdn(year)) year--
  let doy = jdn - nowruzJdn(year) // 0-based
  if (doy < 186) return { year, month: fdiv(doy, 31) + 1, day: (doy % 31) + 1 }
  doy -= 186
  return { year, month: fdiv(doy, 30) + 7, day: (doy % 30) + 1 }
}

function gregorianDaysInMonth (year: number, month: number): number {
  if (month === 2) return (fmod(year, 4) === 0 && (fmod(year, 100) !== 0 || fmod(year, 400) === 0)) ? 29 : 28
  return month === 4 || month === 6 || month === 9 || month === 11 ? 30 : 31
}

/** JDN of a proleptic Gregorian date (month 1..12). Throws RangeError for an invalid date. */
export function gregorianToJdn (year: number, month: number, day: number): number {
  checkInt('year', year)
  checkInt('month', month)
  checkInt('day', day)
  if (year < GREGORIAN_MIN_YEAR || year > GREGORIAN_MAX_YEAR || month < 1 || month > 12 ||
      day < 1 || day > gregorianDaysInMonth(year, month)) {
    throw new RangeError(`invalid Gregorian date: ${year}-${month}-${day}`)
  }
  const a = fdiv(14 - month, 12)
  const y = year + 4800 - a
  const m = month + 12 * a - 3
  const jdn = day + fdiv(153 * m + 2, 5) + 365 * y + fdiv(y, 4) - fdiv(y, 100) + fdiv(y, 400) - 32045
  checkJdn(jdn)
  return jdn
}

/** Proleptic Gregorian date of a JDN (month 1..12). */
export function jdnToGregorian (jdn: number): GregorianDate {
  checkJdn(jdn)
  const a = jdn + 32044
  const b = fdiv(4 * a + 3, 146097)
  const c = a - fdiv(146097 * b, 4)
  const d = fdiv(4 * c + 3, 1461)
  const e = c - fdiv(1461 * d, 4)
  const m = fdiv(5 * e + 2, 153)
  return {
    year: 100 * b + d - 4800 + fdiv(m, 10),
    month: m + 3 - 12 * fdiv(m, 10),
    day: e - fdiv(153 * m + 2, 5) + 1,
  }
}

export const toGregorian = (year: number, month: number, day: number): GregorianDate =>
  jdnToGregorian(toJdn(year, month, day))

export const fromGregorian = (year: number, month: number, day: number): PersianDate =>
  fromJdn(gregorianToJdn(year, month, day))

/** 0 = Saturday (شنبه) ... 6 = Friday (جمعه). */
export const dayOfWeek = (year: number, month: number, day: number): number =>
  fmod(toJdn(year, month, day) + 2, 7)

/** 1 ... 365/366. */
export function dayOfYear (year: number, month: number, day: number): number {
  checkDate(year, month, day)
  return daysBeforeMonth(month) + day
}

// Creating an Intl.DateTimeFormat is expensive; keep one per time zone. The cache is bounded:
// zone strings may come from user input, and Intl accepts many spellings of valid zones
// ('asia/tehran', 'ASIA/TEHRAN', '+03:30', '+0330', ...), so an unbounded cache could grow without limit.
const MAX_FORMATTERS = 32
const formatters = new Map<string, Intl.DateTimeFormat>()
function formatterFor (timeZone: string): Intl.DateTimeFormat {
  let f = formatters.get(timeZone)
  if (!f) {
    f = new Intl.DateTimeFormat('en-US-u-ca-gregory', {
      timeZone, era: 'short', year: 'numeric', month: 'numeric', day: 'numeric',
    })
    if (formatters.size >= MAX_FORMATTERS) formatters.delete(formatters.keys().next().value!) // oldest
    formatters.set(timeZone, f)
  }
  return f
}

/** Persian date of a JS Date as seen in an IANA time zone (default Asia/Tehran). */
export function fromDate (date: Date, timeZone = 'Asia/Tehran'): PersianDate {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) throw new TypeError('not a valid Date')
  const parts: Partial<Record<Intl.DateTimeFormatPartTypes, string>> = {}
  for (const p of formatterFor(timeZone).formatToParts(date)) parts[p.type] = p.value
  const year = parts.era === 'BC' ? 1 - Number(parts.year) : Number(parts.year) // 1 BC = year 0
  return fromGregorian(year, Number(parts.month), Number(parts.day))
}
