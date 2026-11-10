// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Run: node --test implementations/javascript/
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import * as pc from './persian-calendar.js'

const csv = (name) => {
  const [head, ...lines] = readFileSync(new URL(`../../data/${name}`, import.meta.url), 'utf8').trim().split('\n')
  const keys = head.split(',')
  return lines.map((l) => Object.fromEntries(l.split(',').map((v, i) => [keys[i], keys[i] === 'nowruz_gregorian' ? v : Number(v)])))
}

test('years.csv: leap years, Nowruz, weekday, every day 1178-3000', () => {
  for (const r of csv('years.csv')) {
    assert.equal(pc.isLeapYear(r.year), r.is_leap === 1, `${r.year}`)
    assert.equal(pc.daysInMonth(r.year, 12), r.is_leap ? 30 : 29)
    const [gy, gm, gd] = r.nowruz_gregorian.split('-').map(Number)
    assert.deepEqual(pc.toGregorian(r.year, 1, 1), { year: gy, month: gm, day: gd })
    assert.equal(pc.toJdn(r.year, 1, 1), r.nowruz_jdn)
    assert.equal(pc.dayOfWeek(r.year, 1, 1), r.nowruz_weekday)
    // every day of the year against the ground truth, both directions
    for (let doy = 0; doy < r.days_in_year; doy++) {
      const [month, day] = doy < 186 ? [Math.floor(doy / 31) + 1, (doy % 31) + 1] : [Math.floor((doy - 186) / 30) + 7, ((doy - 186) % 30) + 1]
      const jdn = r.nowruz_jdn + doy
      const p = pc.fromJdn(jdn)
      if (p.year !== r.year || p.month !== month || p.day !== day || pc.toJdn(r.year, month, day) !== jdn) {
        assert.fail(`${r.year}/${month}/${day} (JDN ${jdn})`)
      }
    }
  }
})

test('test-vectors.csv: both directions', () => {
  for (const r of csv('test-vectors.csv')) {
    const p = { year: r.jy, month: r.jm, day: r.jd }
    const g = { year: r.gy, month: r.gm, day: r.gd }
    assert.deepEqual(pc.toGregorian(r.jy, r.jm, r.jd), g)
    assert.deepEqual(pc.fromGregorian(r.gy, r.gm, r.gd), p)
    assert.equal(pc.toJdn(r.jy, r.jm, r.jd), r.jdn)
    assert.deepEqual(pc.fromJdn(r.jdn), p)
    assert.equal(pc.dayOfWeek(r.jy, r.jm, r.jd), r.weekday)
  }
})

test('every day 1178..3000 round-trips', () => {
  const first = pc.toJdn(pc.VERIFIED_MIN_YEAR, 1, 1)
  const last = pc.toJdn(pc.VERIFIED_MAX_YEAR, 12, pc.daysInMonth(pc.VERIFIED_MAX_YEAR, 12))
  let prev = pc.fromJdn(first - 1)
  for (let jdn = first; jdn <= last; jdn++) {
    const d = pc.fromJdn(jdn)
    assert.ok(pc.isValidDate(d.year, d.month, d.day))
    assert.equal(pc.toJdn(d.year, d.month, d.day), jdn)
    const g = pc.jdnToGregorian(jdn)
    assert.equal(pc.gregorianToJdn(g.year, g.month, g.day), jdn)
    const consecutive = (d.day === prev.day + 1 && d.month === prev.month && d.year === prev.year) ||
      (d.day === 1 && ((d.month === prev.month + 1 && d.year === prev.year) || (d.month === 1 && prev.month === 12 && d.year === prev.year + 1)))
    assert.ok(consecutive, `${jdn}`)
    prev = d
  }
})

test('extrapolated years (negative intermediate values)', () => {
  // Outside 1178-3000 AP the calendar is an arithmetic extrapolation. Years <= 0 make
  // intermediate values negative, which is exactly where truncating division breaks.
  assert.equal(pc.toJdn(-1000, 1, 1), 1582713)
  assert.equal(pc.toJdn(0, 1, 1), 1947955)
  assert.equal(pc.toJdn(4000, 1, 1), 3408925)
  assert.deepEqual(pc.fromJdn(0), { year: -5334, month: 9, day: 3 })
  assert.equal(pc.dayOfWeek(-1000, 1, 1), 1)
  assert.ok(pc.isLeapYear(-3))
  assert.ok(!pc.isLeapYear(-1000))
  for (const [firstYear, lastYear] of [[-1000, 1177], [3001, 4000]]) {
    const first = pc.toJdn(firstYear, 1, 1)
    const last = pc.toJdn(lastYear, 12, pc.daysInMonth(lastYear, 12))
    let prev = pc.fromJdn(first - 1)
    for (let jdn = first; jdn <= last; jdn++) {
      const d = pc.fromJdn(jdn)
      assert.ok(pc.isValidDate(d.year, d.month, d.day))
      assert.equal(pc.toJdn(d.year, d.month, d.day), jdn)
      const g = pc.jdnToGregorian(jdn)
      assert.equal(pc.gregorianToJdn(g.year, g.month, g.day), jdn)
      const consecutive = (d.day === prev.day + 1 && d.month === prev.month && d.year === prev.year) ||
        (d.day === 1 && ((d.month === prev.month + 1 && d.year === prev.year) || (d.month === 1 && prev.month === 12 && d.year === prev.year + 1)))
      assert.ok(consecutive, `${jdn}`)
      prev = d
    }
  }
})

test('validation', () => {
  assert.ok(pc.isValidDate(1403, 12, 30))
  assert.ok(!pc.isValidDate(1404, 12, 30))
  assert.ok(pc.isValidDate(1404, 6, 31))
  assert.ok(!pc.isValidDate(1404, 7, 31))
  assert.ok(!pc.isValidDate(1404, 13, 1))
  assert.ok(!pc.isValidDate(1404, 0, 1))
  assert.ok(!pc.isValidDate(1404, 1, 0))
  assert.ok(!pc.isValidDate(1404, 1, 1.5))
  assert.throws(() => pc.toJdn(1404, 12, 30), RangeError)
  assert.throws(() => pc.fromJdn(2460756.5), TypeError)
})

test('limits and bad input', () => {
  assert.equal(pc.toJdn(pc.MIN_YEAR, 1, 1), pc.MIN_JDN)
  assert.equal(pc.toJdn(pc.MAX_YEAR, 12, pc.daysInMonth(pc.MAX_YEAR, 12)), pc.MAX_JDN)
  assert.deepEqual(pc.fromJdn(pc.MIN_JDN), { year: pc.MIN_YEAR, month: 1, day: 1 })
  assert.deepEqual(pc.fromJdn(pc.MAX_JDN), { year: pc.MAX_YEAR, month: 12, day: 29 })
  assert.deepEqual(pc.jdnToGregorian(pc.MIN_JDN), { year: -99379, month: 3, day: 29 })
  assert.deepEqual(pc.jdnToGregorian(pc.MAX_JDN), { year: 100622, month: 3, day: 13 })
  assert.equal(pc.gregorianToJdn(100622, 3, 13), pc.MAX_JDN)
  for (const bad of [pc.MIN_JDN - 1, pc.MAX_JDN + 1, 2 ** 52, -(2 ** 52), Number.MAX_SAFE_INTEGER]) {
    assert.throws(() => pc.fromJdn(bad), RangeError)
    assert.throws(() => pc.jdnToGregorian(bad), RangeError)
  }
  for (const year of [pc.MIN_YEAR - 1, pc.MAX_YEAR + 1, 1e15]) {
    assert.ok(!pc.isValidDate(year, 1, 1))
    assert.throws(() => pc.toJdn(year, 1, 1), RangeError)
    assert.throws(() => pc.isLeapYear(year), RangeError)
  }
  assert.throws(() => pc.gregorianToJdn(100622, 3, 14), RangeError) // one day after MAX_JDN
  // Invalid Gregorian dates are rejected, not silently normalized.
  for (const bad of [[2025, 2, 29], [2025, 2, 30], [2025, 13, 1], [2025, 0, 1], [2025, 4, 31], [2025, 1, 0]]) {
    assert.throws(() => pc.gregorianToJdn(...bad), RangeError)
    assert.throws(() => pc.fromGregorian(...bad), RangeError)
  }
  assert.deepEqual(pc.fromGregorian(2024, 2, 29), { year: 1402, month: 12, day: 10 })
  // Non-integers throw TypeError.
  for (const call of [() => pc.isLeapYear(1.5), () => pc.isLeapYear('1403'), () => pc.daysInMonth(1404, 6.5),
    () => pc.daysInMonth(1404, '12'), () => pc.toJdn(1404, '1', 1), () => pc.fromJdn(2460756.5), () => pc.fromJdn('2460756'),
    () => pc.fromJdn(NaN), () => pc.gregorianToJdn(2025, 3, 21.5), () => pc.fromDate('2025-03-21'), () => pc.fromDate(new Date(NaN))]) {
    assert.throws(call, TypeError)
  }
  // Exported tables are read-only.
  assert.ok(Object.isFrozen(pc.NON_LEAP_CORRECTION) && Object.isFrozen(pc.MONTH_NAMES_FA) && Object.isFrozen(pc.WEEKDAY_NAMES_EN))
  assert.throws(() => { 'use strict'; pc.MONTH_NAMES_FA[0] = 'X' }, TypeError)
  // Dates before 1 CE: 1 BC is year 0.
  assert.deepEqual(pc.fromDate(new Date(Date.UTC(-499, 2, 21)), 'UTC'), pc.fromGregorian(-499, 3, 21))
})

// Every year MIN_YEAR..MAX_YEAR: years are contiguous, first and last day round-trip, and the leap
// years sit where every other implementation (and ICU 77.1+) puts them (checksums).
test('every year of the full range', () => {
  let expected = pc.MIN_JDN, leaps = 0, nowruzSum = 0
  for (let y = pc.MIN_YEAR; y <= pc.MAX_YEAR; y++) {
    const nowruz = pc.toJdn(y, 1, 1)
    const leap = pc.isLeapYear(y) ? 1 : 0
    const first = pc.fromJdn(nowruz), last = pc.fromJdn(nowruz + 364 + leap)
    if (nowruz !== expected || first.year !== y || first.month !== 1 || first.day !== 1 ||
        pc.daysInMonth(y, 12) !== 29 + leap || last.year !== y || last.month !== 12 || last.day !== 29 + leap) {
      assert.fail(`year ${y}`)
    }
    leaps += leap
    nowruzSum += nowruz // stays below 2^53, so exact
    expected = nowruz + 365 + leap
  }
  assert.equal(expected - 1, pc.MAX_JDN)
  assert.deepEqual([leaps, nowruzSum], [48485, 389592978180])
})

test('known dates', () => {
  assert.deepEqual(pc.fromGregorian(2025, 3, 21), { year: 1404, month: 1, day: 1 })
  assert.deepEqual(pc.fromGregorian(2025, 3, 20), { year: 1403, month: 12, day: 30 })
  assert.deepEqual(pc.fromGregorian(1979, 2, 11), { year: 1357, month: 11, day: 22 })
  assert.equal(pc.dayOfYear(1404, 7, 1), 187)
  assert.equal(pc.WEEKDAY_NAMES_EN[pc.dayOfWeek(1404, 1, 1)], 'Friday')
  // 2025-03-20T21:00Z is already 1 Farvardin in Tehran (UTC+3:30), still 30 Esfand in UTC.
  const instant = new Date('2025-03-20T21:00:00Z')
  assert.deepEqual(pc.fromDate(instant, 'Asia/Tehran'), { year: 1404, month: 1, day: 1 })
  assert.deepEqual(pc.fromDate(instant, 'UTC'), { year: 1403, month: 12, day: 30 })
})

test('agrees with Intl (ICU 77.1+) where available', () => {
  // ICU 76.1 and older lack the correction table (they use the plain 33-year rule), so only
  // compare with ICU 77.1+. Years 1300-1600 include the first correction year, 1502.
  const [major] = (process.versions.icu ?? '0').split('.').map(Number)
  if (major < 77) return
  const fmt = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', { timeZone: 'UTC', year: 'numeric', month: 'numeric', day: 'numeric' })
  for (let jdn = pc.toJdn(1300, 1, 1); jdn < pc.toJdn(1600, 1, 1); jdn += 13) {
    const g = pc.jdnToGregorian(jdn)
    const parts = Object.fromEntries(fmt.formatToParts(new Date(Date.UTC(g.year, g.month - 1, g.day))).map((p) => [p.type, p.value]))
    assert.deepEqual(pc.fromJdn(jdn), { year: Number(parts.year), month: Number(parts.month), day: Number(parts.day) })
  }
})

test('fromDate: many time zone spellings (bounded formatter cache stays correct)', () => {
  const instant = new Date('2025-03-20T21:00:00Z') // 00:30 on 1 Farvardin 1404 in Tehran
  for (const zone of ['Asia/Tehran', 'asia/tehran', 'ASIA/TEHRAN', '+03:30', '+0330']) {
    assert.deepEqual(pc.fromDate(instant, zone), { year: 1404, month: 1, day: 1 }, zone)
  }
  for (let h = -12; h <= 12; h++) {
    for (const m of ['00', '30']) {
      const zone = `${h < 0 ? '-' : '+'}${String(Math.abs(h)).padStart(2, '0')}:${m}`
      const local = new Date(instant.getTime() + Math.sign(h || 1) * (Math.abs(h) * 60 + Number(m)) * 60000)
      const g = { year: local.getUTCFullYear(), month: local.getUTCMonth() + 1, day: local.getUTCDate() }
      assert.deepEqual(pc.fromDate(instant, zone), pc.fromGregorian(g.year, g.month, g.day), zone)
    }
  }
  assert.deepEqual(pc.fromDate(instant, 'Asia/Tehran'), { year: 1404, month: 1, day: 1 }) // evicted and re-created
  assert.throws(() => pc.fromDate(instant, 'Not/AZone'), RangeError)
})
