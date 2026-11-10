// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Evaluates JavaScript libraries. Writes ../results/<library>.txt (Nowruz per year) and
// ../results/<library>.everyday.json (every day 1300-1500 AP, both directions).
import { writeFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import * as ref from '../../../implementations/javascript/persian-calendar.js'

const require = createRequire(import.meta.url)
const jalaali = require('jalaali-js')
const moment = require('moment-jalaali')
const dayjs = require('dayjs')
const jalaliday = require('jalaliday')
dayjs.extend(jalaliday.default ?? jalaliday)
const dfj = require('date-fns-jalali')
const version = (name) => require(`./node_modules/${name}/package.json`).version

const pad = (n, w = 2) => String(n).padStart(w, '0')
const iso = (y, m, d) => `${pad(y, 4)}-${pad(m)}-${pad(d)}`
const intlFmt = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', { timeZone: 'UTC', year: 'numeric', month: 'numeric', day: 'numeric' })
const intl = (g) => {
  const p = Object.fromEntries(intlFmt.formatToParts(new Date(Date.UTC(g.year, g.month - 1, g.day))).map((x) => [x.type, x.value]))
  return [+p.year, +p.month, +p.day]
}

// Each library: p2g(y, m, d) -> [gy, gm, gd]; g2p({year, month, day}) -> [jy, jm, jd]
const libs = {
  [`jalaali-js ${version('jalaali-js')}`]: {
    p2g: (y, m, d) => { const r = jalaali.toGregorian(y, m, d); return [r.gy, r.gm, r.gd] },
    g2p: (g) => { const r = jalaali.toJalaali(g.year, g.month, g.day); return [r.jy, r.jm, r.jd] },
  },
  [`moment-jalaali ${version('moment-jalaali')}`]: {
    p2g: (y, m, d) => { const r = moment.utc(`${y}/${m}/${d}`, 'jYYYY/jM/jD'); return r.isValid() ? [r.year(), r.month() + 1, r.date()] : null },
    g2p: (g) => { const r = moment.utc([g.year, g.month - 1, g.day]); return [r.jYear(), r.jMonth() + 1, r.jDate()] },
  },
  [`dayjs ${version('dayjs')} + jalaliday ${version('jalaliday')}`]: {
    p2g: (y, m, d) => { const r = dayjs(`${pad(y, 4)}-${pad(m)}-${pad(d)}`, { jalali: true }).calendar('gregory'); return [r.year(), r.month() + 1, r.date()] },
    g2p: (g) => { const r = dayjs(iso(g.year, g.month, g.day)).calendar('jalali'); return [r.year(), r.month() + 1, r.date()] },
  },
  [`date-fns-jalali ${version('date-fns-jalali')}`]: {
    p2g: (y, m, d) => { const r = dfj.newDate(y, m - 1, d); return [r.getFullYear(), r.getMonth() + 1, r.getDate()] },
    g2p: (g) => dfj.format(new Date(g.year, g.month - 1, g.day, 12), 'yyyy-M-d').split('-').map(Number),
  },
  [`Intl (Node ${process.versions.node}, ICU ${process.versions.icu})`]: { p2g: null, g2p: intl },
}

const safe = (f) => (...a) => { try { return f(...a) } catch { return null } }
for (const [name, lib] of Object.entries(libs)) {
  const file = name.replace(/[^\w.+-]+/g, '_')
  // Nowruz per year
  let lines = ''
  if (lib.p2g) {
    for (let y = 1100; y <= 3300; y++) { const g = safe(lib.p2g)(y, 1, 1); if (g) lines += `${y} ${iso(...g)}\n` }
  } else {
    // Only Gregorian -> Persian available: find the Gregorian day that maps to 1/1.
    for (let gy = 1721; gy <= 3921; gy++) {
      for (let d = 17; d <= 24; d++) {
        const p = safe(lib.g2p)({ year: gy, month: 3, day: d })
        if (p && p[1] === 1 && p[2] === 1) { lines += `${p[0]} ${iso(gy, 3, d)}\n`; break }
      }
    }
  }
  writeFileSync(`../results/${file}.txt`, `# ${name}\n${lines}`)
  // Every day 1300..1500
  let g2pWrong = 0, p2gWrong = 0, days = 0
  for (let jdn = ref.toJdn(1300, 1, 1); jdn <= ref.toJdn(1500, 12, 29); jdn++, days++) {
    const g = ref.jdnToGregorian(jdn), p = ref.fromJdn(jdn)
    if (String(safe(lib.g2p)(g)) !== String([p.year, p.month, p.day])) g2pWrong++
    if (lib.p2g && String(safe(lib.p2g)(p.year, p.month, p.day)) !== String([g.year, g.month, g.day])) p2gWrong++
  }
  writeFileSync(`../results/${file}.everyday.json`, JSON.stringify({ library: name, days, g2pWrong, p2gWrong: lib.p2g ? p2gWrong : null }) + '\n')
  console.log(name, { days, g2pWrong, p2gWrong })
}
