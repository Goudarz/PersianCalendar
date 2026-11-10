-- Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
-- SPDX-License-Identifier: MIT

-- Solar Hijri (Persian, Jalali, Shamsi) calendar for PostgreSQL.
--
-- Algorithm: 33-year arithmetic rule + 78-year correction table,
-- the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
-- for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
--
-- Store dates as `date`/`timestamptz` and convert with these functions when you query or display.
-- All functions are IMMUTABLE, so you can use them in indexes and generated columns.
--
-- Accepted range: years -100000..100000 (JDN -34576287..38472562). Outside it, and for invalid
-- dates, the functions raise an error (SQLSTATE 22008); persian_is_valid returns false.
--
-- SQL functions use SQL-standard bodies (BEGIN ATOMIC), which bind the objects they use when they
-- are created: they do not depend on search_path at call time (safe against search_path hijacking,
-- and indexes on them survive pg_dump/restore). The PL/pgSQL functions pin search_path instead.
--
-- Functions whose body is a CASE are not declared STRICT (PostgreSQL would then refuse to inline
-- them); they return NULL for NULL arguments explicitly instead.
--
-- Requires PostgreSQL 14+. Tested with PostgreSQL 17.

CREATE TYPE persian_date AS (year integer, month integer, day integer);

-- Floor division for b > 0; PostgreSQL's integer / truncates toward zero.
CREATE OR REPLACE FUNCTION persian_fdiv(a bigint, b bigint) RETURNS bigint
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
BEGIN ATOMIC
  SELECT (a - (((a % b) + b) % b)) / b;
END;

-- Years the 33-year rule calls leap but are not; the next year is leap instead.
CREATE OR REPLACE FUNCTION persian_is_correction(y integer) RETURNS boolean
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
BEGIN ATOMIC
  SELECT y = ANY ('{1502,1601,1634,1667,1700,1733,1766,1799,1832,1865,1898,1931,1964,1997,2030,
    2059,2063,2096,2129,2158,2162,2191,2195,2224,2228,2257,2261,2290,2294,2323,2327,2356,2360,2389,
    2393,2422,2426,2455,2459,2488,2492,2521,2525,2554,2558,2587,2591,2620,2624,2653,2657,2686,2690,
    2719,2723,2748,2752,2756,2781,2785,2789,2818,2822,2847,2851,2855,2880,2884,2888,2913,2917,2921,
    2946,2950,2954,2979,2983,2987}'::integer[]);
END;

-- Raises the out-of-range error; called only on the error path.
CREATE OR REPLACE FUNCTION persian_year_out_of_range(y integer) RETURNS boolean
LANGUAGE plpgsql IMMUTABLE STRICT PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
BEGIN
  RAISE EXCEPTION 'year out of range [-100000, 100000]: %', y USING ERRCODE = '22008';
END
$$;

-- Leap-year rule without the range check (internal).
CREATE OR REPLACE FUNCTION persian_leap_rule(y integer) RETURNS boolean
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT CASE
    WHEN persian_is_correction(y) THEN false
    WHEN persian_is_correction(y - 1) THEN true
    ELSE (((25 * y::bigint + 11) % 33) + 33) % 33 < 8
  END;
END;

CREATE OR REPLACE FUNCTION persian_is_leap_year(y integer) RETURNS boolean
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT CASE
    WHEN y IS NULL THEN NULL
    WHEN y < -100000 OR y > 100000 THEN persian_year_out_of_range(y)
    ELSE persian_leap_rule(y)
  END;
END;

-- 29, 30 or 31 (NULL for a month outside 1..12).
CREATE OR REPLACE FUNCTION persian_days_in_month(y integer, m integer) RETURNS integer
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT CASE
    WHEN y IS NULL OR m IS NULL THEN NULL
    WHEN m < 1 OR m > 12 THEN NULL
    WHEN m <= 6 THEN 31
    WHEN m <= 11 THEN 30
    WHEN persian_is_leap_year(y) THEN 30
    ELSE 29
  END;
END;

-- True for a real Persian date in the accepted range; never raises.
CREATE OR REPLACE FUNCTION persian_is_valid(y integer, m integer, d integer) RETURNS boolean
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT CASE
    WHEN y IS NULL OR m IS NULL OR d IS NULL THEN NULL
    WHEN y < -100000 OR y > 100000 OR m < 1 OR m > 12 OR d < 1 THEN false
    WHEN m <= 6 THEN d <= 31
    WHEN m <= 11 THEN d <= 30
    ELSE d <= CASE WHEN persian_leap_rule(y) THEN 30 ELSE 29 END
  END;
END;

-- Julian Day Number of 1 Farvardin (internal: the caller checks the year).
CREATE OR REPLACE FUNCTION persian_nowruz_jdn(y integer) RETURNS bigint
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
BEGIN ATOMIC
  SELECT 1948320 + 365 * (y::bigint - 1) + persian_fdiv(8 * y::bigint + 21, 33)
         - persian_is_correction(y - 1)::integer;
END;

-- Raises the invalid-date error; called only on the error path.
CREATE OR REPLACE FUNCTION persian_invalid_date(y integer, m integer, d integer) RETURNS bigint
LANGUAGE plpgsql IMMUTABLE STRICT PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
BEGIN
  RAISE EXCEPTION 'invalid Persian date: %/%/%', y, m, d USING ERRCODE = '22008';
END
$$;

CREATE OR REPLACE FUNCTION persian_to_jdn(y integer, m integer, d integer) RETURNS bigint
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT CASE
    WHEN y IS NULL OR m IS NULL OR d IS NULL THEN NULL
    WHEN NOT persian_is_valid(y, m, d) THEN persian_invalid_date(y, m, d)
    ELSE persian_nowruz_jdn(y)
         + CASE WHEN m <= 7 THEN 31 * (m - 1) ELSE 30 * (m - 1) + 6 END
         + d - 1
  END;
END;

CREATE OR REPLACE FUNCTION persian_from_jdn(jdn bigint) RETURNS persian_date
LANGUAGE plpgsql IMMUTABLE STRICT PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
DECLARE
  y integer;
  doy integer;
BEGIN
  IF jdn < -34576287 OR jdn > 38472562 THEN
    RAISE EXCEPTION 'jdn out of range [-34576287, 38472562]: %', jdn USING ERRCODE = '22008';
  END IF;
  y := 1 + persian_fdiv(33 * (jdn - 1948320) + 3, 12053);  -- estimate, then correct
  WHILE jdn >= persian_nowruz_jdn(y + 1) LOOP y := y + 1; END LOOP;
  WHILE jdn < persian_nowruz_jdn(y) LOOP y := y - 1; END LOOP;
  doy := jdn - persian_nowruz_jdn(y);  -- 0-based
  IF doy < 186 THEN
    RETURN ROW(y, doy / 31 + 1, doy % 31 + 1)::persian_date;
  END IF;
  doy := doy - 186;
  RETURN ROW(y, doy / 30 + 7, doy % 30 + 1)::persian_date;
END
$$;

-- date <-> Persian. JDN of 2000-01-01 is 2451545. Infinite dates raise an error.
CREATE OR REPLACE FUNCTION persian_from_date(g date) RETURNS persian_date
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
BEGIN ATOMIC
  SELECT persian_from_jdn((g - DATE '2000-01-01') + 2451545);
END;

CREATE OR REPLACE FUNCTION persian_to_date(y integer, m integer, d integer) RETURNS date
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT DATE '2000-01-01' + (persian_to_jdn(y, m, d) - 2451545)::integer;
END;

-- 0 = Saturday (شنبه) ... 6 = Friday (جمعه)
CREATE OR REPLACE FUNCTION persian_day_of_week(g date) RETURNS integer
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
BEGIN ATOMIC
  SELECT (extract(dow FROM g)::integer + 1) % 7;
END;

-- 1 ... 365/366 (NULL for an invalid date).
CREATE OR REPLACE FUNCTION persian_day_of_year(y integer, m integer, d integer) RETURNS integer
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT CASE WHEN persian_is_valid(y, m, d)
    THEN CASE WHEN m <= 7 THEN 31 * (m - 1) ELSE 30 * (m - 1) + 6 END + d
  END;
END;

-- '1404/01/01' style text, sortable for years 0..9999. Years are never truncated
-- ('10000/01/01'), and negative years keep their sign ('-0005/01/01').
-- For a timestamptz, convert to the user's zone first:
--   persian_format((created_at AT TIME ZONE 'Asia/Tehran')::date)
CREATE OR REPLACE FUNCTION persian_format(g date) RETURNS text
LANGUAGE plpgsql IMMUTABLE STRICT PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
DECLARE
  p persian_date := persian_from_date(g);
  y text := abs(p.year)::text;
BEGIN
  RETURN CASE WHEN p.year < 0 THEN '-' ELSE '' END || lpad(y, greatest(4, length(y)), '0')
         || '/' || lpad(p.month::text, 2, '0') || '/' || lpad(p.day::text, 2, '0');
END
$$;

-- NULL for a month outside 1..12. lang 'fa' (default) or anything else for English.
CREATE OR REPLACE FUNCTION persian_month_name(m integer, lang text DEFAULT 'fa') RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE
BEGIN ATOMIC
  SELECT CASE
    WHEN m IS NULL OR lang IS NULL THEN NULL
    ELSE CASE lang
    WHEN 'fa' THEN (ARRAY['فروردین','اردیبهشت','خرداد','تیر','مرداد','شهریور','مهر','آبان','آذر','دی','بهمن','اسفند'])[m]
    ELSE (ARRAY['Farvardin','Ordibehesht','Khordad','Tir','Mordad','Shahrivar','Mehr','Aban','Azar','Dey','Bahman','Esfand'])[m]
    END
  END;
END;
