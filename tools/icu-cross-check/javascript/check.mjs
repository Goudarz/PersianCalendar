// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares ICU4C (through the JavaScript Intl API), Temporal (V8 implements it with temporal_rs,
// which is built on ICU4X, not ICU4C), this repository's JavaScript or TypeScript implementation
// and the ground truth (data/years.csv) for every day of 1178-3000 AP.
// Intl can only convert Gregorian -> Persian; Temporal is checked in both directions.
//
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the script checks that ICU matches the plain 33-year rule exactly instead.
// Run: node check.mjs              (the result depends on the ICU version built into Node.js)
//      node check.mjs typescript   (checks the TypeScript implementation; Node.js 23.6+)
import { readFileSync } from 'node:fs'

const language = process.argv[2] === 'typescript' ? 'TypeScript' : 'JavaScript'
const ref = await import(language === 'TypeScript'
  ? '../../../implementations/typescript/persian-calendar.ts'
  : '../../../implementations/javascript/persian-calendar.js')

const years = readFileSync(new URL('../../../data/years.csv', import.meta.url), 'utf8').trim().split('\n').slice(1).map((l) => l.split(','))
const icuMajor = Number(process.versions.icu.split('.')[0])
const hasCorrections = icuMajor >= 77

// The plain 33-year rule (what ICU 50-76 implements): Nowruz JDN of year y.
const plainNowruz = (y) => 1948320 + 365 * (y - 1) + Math.floor((8 * y + 21) / 33)
const toPersian = (nowruz, jdn) => {
  const doy = jdn - nowruz
  return doy < 186
    ? [Math.floor(doy / 31) + 1, (doy % 31) + 1]
    : [Math.floor((doy - 186) / 30) + 7, ((doy - 186) % 30) + 1]
}
const plainFromJdn = (jdn) => {
  let y = Math.floor((jdn - 1948320) / 365.25) + 1
  while (jdn >= plainNowruz(y + 1)) y++
  while (jdn < plainNowruz(y)) y--
  return [y, ...toPersian(plainNowruz(y), jdn)]
}

const fmt = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', { timeZone: 'UTC', year: 'numeric', month: 'numeric', day: 'numeric' })
const hasTemporal = typeof Temporal !== 'undefined'
let days = 0, referenceWrong = 0, icuWrong = 0, icuDiffersFromTruth = 0, temporalWrong = 0
for (const [y, leap, , , nowruz] of years) {
  const year = Number(y)
  const length = leap === '1' ? 366 : 365
  for (let doy = 0; doy < length; doy++, days++) {
    const jdn = Number(nowruz) + doy
    const [month, day] = toPersian(Number(nowruz), jdn)
    const p = ref.fromJdn(jdn)
    if (p.year !== year || p.month !== month || p.day !== day || ref.toJdn(year, month, day) !== jdn) referenceWrong++
    const parts = {}
    for (const part of fmt.formatToParts(new Date((jdn - 2440588) * 86400000))) parts[part.type] = part.value
    const icu = [Number(parts.year ?? parts.relatedYear), Number(parts.month), Number(parts.day)]
    const expected = hasCorrections ? [year, month, day] : plainFromJdn(jdn)
    if (String(icu) !== String(expected)) icuWrong++
    if (String(icu) !== String([year, month, day])) icuDiffersFromTruth++
    if (hasTemporal) {
      const iso = new Date((jdn - 2440588) * 86400000).toISOString().slice(0, 10)
      const t = Temporal.PlainDate.from(iso).withCalendar('persian')
      const back = Temporal.PlainDate.from({ calendar: 'persian', year, month, day }, { overflow: 'reject' })
      if (t.year !== year || t.month !== month || t.day !== day || back.withCalendar('iso8601').toString() !== iso) temporalWrong++
    }
  }
}
// Temporal over the whole accepted range, year by year: first and last day of every year.
let temporalYearsWrong = 0
if (hasTemporal) {
  const iso = (jdn) => { const g = ref.jdnToGregorian(jdn); return Temporal.PlainDate.from({ year: g.year, month: g.month, day: g.day }) }
  for (let y = ref.MIN_YEAR; y <= ref.MAX_YEAR; y++) {
    const last = ref.daysInMonth(y, 12)
    const first = Temporal.PlainDate.from({ calendar: 'persian', year: y, month: 1, day: 1 }, { overflow: 'reject' })
    const end = iso(ref.toJdn(y, 12, last)).withCalendar('persian')
    if (!first.withCalendar('iso8601').equals(iso(ref.toJdn(y, 1, 1))) || end.year !== y || end.month !== 12 || end.day !== last) temporalYearsWrong++
  }
}
console.log(`Node ${process.version}, ICU ${process.versions.icu}: ${days} days (1178-3000 AP) | ${language} implementation wrong: ${referenceWrong} | ` +
  `ICU differs from ground truth on ${icuDiffersFromTruth} days | ` +
  `ICU matches ${hasCorrections ? 'the ground truth' : 'the plain 33-year rule'}: ${icuWrong === 0 ? 'yes' : `NO (${icuWrong} days)`}` +
  (hasTemporal ? ` | Temporal (temporal_rs/ICU4X) matches the ground truth: ${temporalWrong === 0 ? 'yes (both directions)' : `NO (${temporalWrong} days)`}` +
    `; years ${ref.MIN_YEAR}..${ref.MAX_YEAR}: Temporal differs in ${temporalYearsWrong} years` : ' | Temporal: not available'))
process.exitCode = referenceWrong + icuWrong + temporalWrong + temporalYearsWrong === 0 ? 0 : 1
