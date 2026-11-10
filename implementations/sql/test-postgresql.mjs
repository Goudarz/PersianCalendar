// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Tests postgresql.sql on a real PostgreSQL engine (PGlite = PostgreSQL compiled to WASM).
// Run: cd implementations/sql && npm install && npm test
import { PGlite } from '@electric-sql/pglite'
import { readFileSync } from 'node:fs'

const db = new PGlite()
await db.exec(readFileSync(new URL('./postgresql.sql', import.meta.url), 'utf8'))
const dataUrl = (name) => new URL(`../../data/${name}`, import.meta.url)

let checks = 0
const check = (ok, ctx) => { checks++; if (!ok) { console.error('failed:', ctx); process.exit(1) } }

// Load the ground truth into tables and compare everything in SQL.
await db.exec(`CREATE TABLE years (year int, is_leap int, days int, nowruz date, jdn bigint, weekday int);
               CREATE TABLE vectors (jy int, jm int, jd int, gy int, gm int, gd int, jdn bigint, weekday int);`)
const load = async (table, name) => {
  const lines = readFileSync(dataUrl(name), 'utf8').trim().split('\n').slice(1)
  for (let i = 0; i < lines.length; i += 1000) {
    const values = lines.slice(i, i + 1000).map((l) => '(' + l.split(',').map((v) => v.includes('-') ? `'${v}'` : v).join(',') + ')')
    await db.exec(`INSERT INTO ${table} VALUES ${values.join(',')}`)
  }
  return lines.length
}
check(await load('years', 'years.csv') === 1823, 'years loaded')
check(await load('vectors', 'test-vectors.csv') > 8000, 'vectors loaded')

const bad = async (sql) => (await db.query(sql)).rows
const noRows = async (sql, ctx) => { const rows = await bad(sql); check(rows.length === 0, `${ctx}: ${JSON.stringify(rows.slice(0, 3))}`) }

await noRows(`SELECT year FROM years WHERE persian_is_leap_year(year) <> (is_leap = 1)
              OR persian_days_in_month(year, 12) <> 29 + is_leap
              OR persian_to_date(year, 1, 1) <> nowruz OR persian_to_jdn(year, 1, 1) <> jdn
              OR persian_day_of_week(nowruz) <> weekday`, 'years')
// Every day of every year 1178-3000 against the ground truth, both directions.
await noRows(`SELECT y.year, doy FROM years y, generate_series(0, y.days - 1) AS doy,
              LATERAL (SELECT CASE WHEN doy < 186 THEN doy / 31 + 1 ELSE (doy - 186) / 30 + 7 END AS m,
                              CASE WHEN doy < 186 THEN doy % 31 + 1 ELSE (doy - 186) % 30 + 1 END AS d) md
              WHERE persian_from_jdn(y.jdn + doy) <> ROW(y.year, md.m, md.d)::persian_date
                 OR persian_to_jdn(y.year, md.m, md.d) <> y.jdn + doy`, 'every day 1178-3000')
await noRows(`SELECT * FROM vectors v, LATERAL (SELECT persian_from_date(make_date(gy, gm, gd)) AS p) x
              WHERE persian_to_date(jy, jm, jd) <> make_date(gy, gm, gd)
                 OR (x.p).year <> jy OR (x.p).month <> jm OR (x.p).day <> jd
                 OR persian_to_jdn(jy, jm, jd) <> jdn
                 OR persian_from_jdn(jdn) <> ROW(jy, jm, jd)::persian_date
                 OR persian_day_of_week(make_date(gy, gm, gd)) <> weekday`, 'vectors')
// Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
const x = (await db.query(`SELECT persian_to_jdn(-1000, 1, 1) AS a, persian_to_jdn(0, 1, 1) AS b, persian_to_jdn(4000, 1, 1) AS c,
  persian_from_jdn(0) = ROW(-5334, 9, 3)::persian_date AS d, persian_is_leap_year(-3) AND NOT persian_is_leap_year(-1000) AS e`)).rows[0]
check(Number(x.a) === 1582713 && Number(x.b) === 1947955 && Number(x.c) === 3408925 && x.d && x.e, JSON.stringify(x))
// Every day of -1000..1177 and 3001..4000 round-trips and is consecutive (the day before, by lag, is the previous date).
for (const [a, b] of [[-1000, 1177], [3001, 4000]]) {
  await noRows(`WITH d AS (SELECT j, persian_from_jdn(j) AS p FROM generate_series(persian_to_jdn(${a}, 1, 1) - 1,
                  persian_to_jdn(${b}, 12, persian_days_in_month(${b}, 12))) AS j),
                l AS (SELECT j, p, lag(p) OVER (ORDER BY j) AS q FROM d)
                SELECT j FROM l WHERE q IS NOT NULL AND (
                  persian_to_jdn((p).year, (p).month, (p).day) <> j
                  OR NOT ((p).day = (q).day + 1 AND (p).month = (q).month AND (p).year = (q).year
                       OR (p).day = 1 AND ((p).month = (q).month + 1 AND (p).year = (q).year
                                        OR (p).month = 1 AND (q).month = 12 AND (p).year = (q).year + 1)))`,
               `extrapolated ${a}..${b}`)
}
// Every day of 1300..1600 round-trips and is consecutive.
await noRows(`WITH d AS (SELECT g::date AS g, persian_from_date(g::date) AS p FROM generate_series(DATE '1921-03-21', DATE '2222-03-21', '1 day') AS g)
              SELECT g FROM d WHERE persian_to_date((p).year, (p).month, (p).day) <> g`, 'round trip')
const r = (await db.query(`SELECT persian_format(DATE '2025-03-21') AS a, persian_format(DATE '2025-03-20') AS b,
  persian_is_valid(1403,12,30) AS c, persian_is_valid(1404,12,30) AS d, persian_is_valid(1404,13,1) AS e,
  persian_month_name(1) AS f, persian_day_of_year(1404, 7, 1) AS h, persian_day_of_year(1403, 12, 30) AS i, persian_day_of_year(1404, 12, 30) AS k, persian_format((TIMESTAMPTZ '2025-03-20 21:00Z' AT TIME ZONE 'Asia/Tehran')::date) AS g`)).rows[0]
check(r.a === '1404/01/01' && r.b === '1403/12/30' && r.c === true && r.d === false && r.e === false && r.f === 'فروردین' && r.h === 187 && r.i === 366 && r.k === null && r.g === '1404/01/01', JSON.stringify(r))
const k = (await db.query(`SELECT persian_from_date(DATE '1979-02-11') = ROW(1357, 11, 22)::persian_date AS a,
  persian_to_date(1304, 1, 1) = DATE '1925-03-21' AS b`)).rows[0]
check(k.a && k.b, `known dates: ${JSON.stringify(k)}`)
let threw = false
try { await db.query('SELECT persian_to_jdn(1404, 12, 30)') } catch { threw = true }
check(threw, 'invalid date raises')

// Limits: exact boundaries; everything outside raises 22008 (no bigint overflow, no hang).
const raises = async (sql) => { try { await db.query(sql); return false } catch (e) { return e.code === '22008' } }
const l = (await db.query(`SELECT persian_to_jdn(-100000, 1, 1) AS a, persian_to_jdn(100000, 12, 29) AS b,
  persian_from_jdn(-34576287) = ROW(-100000, 1, 1)::persian_date AS c, persian_from_jdn(38472562) = ROW(100000, 12, 29)::persian_date AS d,
  NOT persian_is_valid(100001, 1, 1) AND NOT persian_is_valid(-2147483648, 1, 1) AS e`)).rows[0]
check(Number(l.a) === -34576287 && Number(l.b) === 38472562 && l.c && l.d && l.e, JSON.stringify(l))
for (const sql of ['persian_from_jdn(-34576288)', 'persian_from_jdn(38472563)', 'persian_from_jdn(9223372036854775807)',
  'persian_from_jdn(-9223372036854775808)', 'persian_to_jdn(100001, 1, 1)', 'persian_to_jdn(2147483647, 1, 1)',
  'persian_is_leap_year(2147483647)', 'persian_is_leap_year(-2147483648)', 'persian_from_date(DATE \'5874897-12-31\')']) {
  check(await raises(`SELECT ${sql}`), sql)
}
let infinite = false
try { await db.query(`SELECT persian_from_date('infinity'::date)`) } catch { infinite = true }
check(infinite, 'infinite date raises')
// persian_format never truncates and keeps the sign (lpad alone would give '1000' and '00-5').
const f = (await db.query(`SELECT persian_format(persian_to_date(10000, 1, 1)) AS a, persian_format(persian_to_date(-5, 1, 1)) AS b,
  persian_format(persian_to_date(5, 1, 1)) AS c, persian_format(DATE '4714-11-24 BC') AS d`)).rows[0]
check(f.a === '10000/01/01' && f.b === '-0005/01/01' && f.c === '0005/01/01' && f.d === '-5334/09/03', JSON.stringify(f))
// The functions do not depend on search_path at call time: shadowing helpers or an empty
// search_path (as pg_dump uses while restoring indexes) changes nothing.
await db.exec(`CREATE SCHEMA evil;
  CREATE FUNCTION evil.persian_is_correction(y integer) RETURNS boolean LANGUAGE sql AS 'SELECT true';
  CREATE FUNCTION evil.persian_fdiv(a bigint, b bigint) RETURNS bigint LANGUAGE sql AS 'SELECT 0::bigint';
  CREATE TABLE events (id int, at date);
  INSERT INTO events SELECT i, DATE '2025-03-21' + i FROM generate_series(0, 999) i;
  CREATE INDEX events_persian ON events (persian_format(at));`)
await db.exec(`SET search_path = evil, public`)
const s = (await db.query(`SELECT public.persian_format(DATE '2025-03-21') AS a, public.persian_is_leap_year(1404) AS b,
  public.persian_to_jdn(1404, 1, 1) AS c, (public.persian_from_jdn(2460756)).year AS d`)).rows[0]
check(s.a === '1404/01/01' && s.b === false && Number(s.c) === 2460756 && s.d === 1404, `search_path hijack: ${JSON.stringify(s)}`)
await db.exec(`SET search_path = ''`)
const e = (await db.query(`SELECT public.persian_format(DATE '2025-03-21') AS a, count(*)::int AS n FROM public.events
  WHERE public.persian_format(at) LIKE '1404/01/%'`)).rows[0]
check(e.a === '1404/01/01' && e.n === 31, `empty search_path: ${JSON.stringify(e)}`)
await db.exec(`REINDEX TABLE public.events; SET search_path = "$user", public`)
// NULL in, NULL out (also for the functions that are not STRICT so they can be inlined).
const n = (await db.query(`SELECT persian_is_leap_year(NULL) IS NULL AND persian_days_in_month(1404, NULL) IS NULL
  AND persian_is_valid(NULL, 0, 1) IS NULL AND persian_to_jdn(1404, NULL, 1) IS NULL AND persian_to_date(NULL, 1, 1) IS NULL
  AND persian_day_of_year(1404, 1, NULL) IS NULL AND persian_month_name(NULL) IS NULL AND persian_month_name(1, NULL) IS NULL
  AND persian_from_jdn(NULL) IS NULL AND persian_from_date(NULL) IS NULL AND persian_format(NULL) IS NULL AS ok`)).rows[0]
check(n.ok, 'NULL handling')

// Every year -100000..100000: years are contiguous, first and last day round-trip, and the leap years
// sit where every other implementation (and ICU 77.1+) puts them (checksums).
const full = (await db.query(`
  WITH t AS (SELECT y, persian_to_jdn(y, 1, 1) AS nowruz, persian_is_leap_year(y)::int AS leap
             FROM generate_series(-100000, 100000) AS y),
       y AS (SELECT *, lead(nowruz, 1, 38472563::bigint) OVER (ORDER BY y) AS next_nowruz FROM t)
  SELECT count(*) FILTER (WHERE nowruz + 365 + leap <> next_nowruz) AS gaps,
         count(*) FILTER (WHERE persian_from_jdn(nowruz) <> ROW(y, 1, 1)::persian_date
                             OR persian_from_jdn(nowruz + 364 + leap) <> ROW(y, 12, 29 + leap)::persian_date
                             OR persian_days_in_month(y, 12) <> 29 + leap) AS wrong,
         min(nowruz) AS first, sum(leap)::int AS leaps, sum(nowruz)::text AS nowruz_sum
  FROM y`)).rows[0]
check(full.gaps === 0 && full.wrong === 0 && Number(full.first) === -34576287 && full.leaps === 48485 &&
  full.nowruz_sum === '389592978180', `full range: ${JSON.stringify(full)}`)
console.log(`OK: ${checks} checks passed`)
