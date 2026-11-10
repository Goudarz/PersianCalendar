// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares this repository's PostgreSQL functions (run on a real PostgreSQL engine: PGlite, PostgreSQL
// compiled to WASM), ICU4C (Node.js Intl), Temporal (temporal_rs/ICU4X, when available) and the ground
// truth (data/years.csv) for every day of 1178-3000 AP. PostgreSQL has no Persian calendar of its own.
//
// ICU 77.1+ must match the ground truth exactly; with an older ICU in Node.js the ICU column is
// only reported. Run: npm install && node check.mjs
import { PGlite } from '@electric-sql/pglite'
import { readFileSync } from 'node:fs'

const db = new PGlite()
await db.exec(readFileSync(new URL('../../../implementations/sql/postgresql.sql', import.meta.url), 'utf8'))
const years = readFileSync(new URL('../../../data/years.csv', import.meta.url), 'utf8').trim().split('\n').slice(1).map((l) => l.split(','))

// Every day, both directions, in one query: date -> Persian and Persian -> date.
const { rows } = await db.query(`
  SELECT g::date::text AS g, (p).year AS y, (p).month AS m, (p).day AS d,
         persian_to_date((p).year, (p).month, (p).day)::text AS back
  FROM generate_series(DATE '1799-03-21', DATE '3622-03-20', '1 day') AS g,
       LATERAL (SELECT persian_from_date(g::date) AS p) x
  ORDER BY g`, [], { rowMode: 'object' })

const fmt = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', { timeZone: 'UTC', year: 'numeric', month: 'numeric', day: 'numeric' })
const hasCorrections = Number(process.versions.icu.split('.')[0]) >= 77
const hasTemporal = typeof Temporal !== 'undefined'
let i = 0, sqlWrong = 0, icuWrong = 0, temporalWrong = 0
for (const [y, leap, , , nowruz] of years) {
  const year = Number(y)
  for (let doy = 0; doy < (leap === '1' ? 366 : 365); doy++, i++) {
    const month = doy < 186 ? Math.floor(doy / 31) + 1 : Math.floor((doy - 186) / 30) + 7
    const day = doy < 186 ? (doy % 31) + 1 : ((doy - 186) % 30) + 1
    const r = rows[i]
    const iso = new Date((Number(nowruz) + doy - 2440588) * 86400000).toISOString().slice(0, 10)
    if (!r || r.g !== iso || r.y !== year || r.m !== month || r.d !== day || r.back !== iso) sqlWrong++
    const parts = Object.fromEntries(fmt.formatToParts(new Date(iso)).map((p) => [p.type, p.value]))
    if (Number(parts.year ?? parts.relatedYear) !== year || Number(parts.month) !== month || Number(parts.day) !== day) icuWrong++
    if (hasTemporal) {
      const t = Temporal.PlainDate.from(iso).withCalendar('persian')
      if (t.year !== year || t.month !== month || t.day !== day) temporalWrong++
    }
  }
}
const pg = (await db.query(`SELECT current_setting('server_version') AS v`)).rows[0].v
console.log(`PostgreSQL ${pg} (PGlite): ${i} days (1178-3000 AP) | SQL functions wrong: ${sqlWrong + (rows.length - i)} | ` +
  `ICU ${process.versions.icu} (Intl) differs from ground truth on ${icuWrong} days` +
  (hasTemporal ? ` | Temporal differs on ${temporalWrong} days` : ''))
process.exitCode = sqlWrong + (rows.length - i) + temporalWrong + (hasCorrections ? icuWrong : 0) === 0 ? 0 : 1
