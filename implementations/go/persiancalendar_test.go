// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

package persiancalendar

import (
	"math"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"
)

func csv(t *testing.T, name string) [][]string {
	b, err := os.ReadFile("../../data/" + name)
	if err != nil {
		t.Fatal(err)
	}
	var rows [][]string
	for _, l := range strings.Split(strings.TrimSpace(string(b)), "\n")[1:] {
		rows = append(rows, strings.Split(l, ","))
	}
	return rows
}

func atoi(s string) int { n, _ := strconv.Atoi(s); return n }

func must(v int, err error) int {
	if err != nil {
		panic(err)
	}
	return v
}

// fj and ft unwrap FromJdn / FromTime for inputs that are known to be valid.
func fj(jdn int) Date      { return mustDate(FromJdn(jdn)) }
func ft(tm time.Time) Date { return mustDate(FromTime(tm)) }

func mustDate(d Date, err error) Date {
	if err != nil {
		panic(err)
	}
	return d
}

// Limits: exact boundaries, and fast rejection of anything outside (no hang, no overflow).
func TestLimits(t *testing.T) {
	if must(ToJdn(MinYear, 1, 1)) != MinJdn || must(ToJdn(MaxYear, 12, 29)) != MaxJdn {
		t.Fatal("limits")
	}
	if mustDate(FromJdn(MinJdn)) != (Date{MinYear, 1, 1}) || mustDate(FromJdn(MaxJdn)) != (Date{MaxYear, 12, 29}) {
		t.Fatal("limit dates")
	}
	for _, bad := range []int{MinJdn - 1, MaxJdn + 1, math.MaxInt, math.MinInt, math.MaxInt / 2} {
		if _, err := FromJdn(bad); err == nil {
			t.Fatalf("FromJdn(%d) should fail", bad)
		}
	}
	for _, bad := range []int{MinYear - 1, MaxYear + 1, math.MaxInt, math.MinInt} {
		if IsValid(bad, 1, 1) {
			t.Fatalf("IsValid(%d)", bad)
		}
		if _, err := ToJdn(bad, 1, 1); err == nil {
			t.Fatalf("ToJdn(%d) should fail", bad)
		}
		_ = IsLeapYear(bad) // must not panic or overflow
	}
	if IsLeapYear(math.MaxInt) != IsLeapYear(math.MaxInt%33+33*1000) {
		t.Fatal("IsLeapYear must reduce mod 33 without overflow")
	}
	for _, far := range []time.Time{time.Date(math.MaxInt32, 1, 1, 0, 0, 0, 0, time.UTC), time.Date(math.MinInt32, 1, 1, 0, 0, 0, 0, time.UTC)} {
		if _, err := FromTime(far); err == nil {
			t.Fatalf("FromTime(%v) should fail", far)
		}
	}
	names := MonthNamesFa()
	names[0] = "X"
	if MonthNamesFa()[0] != "فروردین" {
		t.Fatal("name tables must not be shared")
	}
}

func TestYears(t *testing.T) {
	for _, r := range csv(t, "years.csv") {
		y, leap := atoi(r[0]), r[1] == "1"
		if IsLeapYear(y) != leap {
			t.Fatalf("leap %d", y)
		}
		g, _ := ToTime(y, 1, 1, time.UTC)
		if g.Format("2006-01-02") != r[3] {
			t.Fatalf("nowruz %d: %s != %s", y, g.Format("2006-01-02"), r[3])
		}
		if must(ToJdn(y, 1, 1)) != atoi(r[4]) || must(Weekday(y, 1, 1)) != atoi(r[5]) {
			t.Fatalf("jdn/weekday %d", y)
		}
		// every day of the year against the ground truth, both directions
		for doy := 0; doy < atoi(r[2]); doy++ {
			m, d := doy/31+1, doy%31+1
			if doy >= 186 {
				m, d = (doy-186)/30+7, (doy-186)%30+1
			}
			jdn := atoi(r[4]) + doy
			if fj(jdn) != (Date{y, m, d}) || must(ToJdn(y, m, d)) != jdn {
				t.Fatalf("%d/%d/%d (JDN %d)", y, m, d, jdn)
			}
		}
	}
}

func TestVectors(t *testing.T) {
	for _, r := range csv(t, "test-vectors.csv") {
		p := Date{atoi(r[0]), atoi(r[1]), atoi(r[2])}
		g := time.Date(atoi(r[3]), time.Month(atoi(r[4])), atoi(r[5]), 0, 0, 0, 0, time.UTC)
		if ft(g) != p {
			t.Fatalf("FromTime %v", g)
		}
		if tt, _ := ToTime(p.Year, p.Month, p.Day, time.UTC); !tt.Equal(g) {
			t.Fatalf("ToTime %v", p)
		}
		if must(ToJdn(p.Year, p.Month, p.Day)) != atoi(r[6]) || fj(atoi(r[6])) != p {
			t.Fatalf("jdn %v", p)
		}
		if must(Weekday(p.Year, p.Month, p.Day)) != atoi(r[7]) {
			t.Fatalf("weekday %v", p)
		}
	}
}

func TestEveryDay(t *testing.T) {
	first := must(ToJdn(VerifiedMinYear, 1, 1))
	last := must(ToJdn(VerifiedMaxYear, 12, DaysInMonth(VerifiedMaxYear, 12)))
	prev := fj(first - 1)
	for jdn := first; jdn <= last; jdn++ {
		d := fj(jdn)
		if !IsValid(d.Year, d.Month, d.Day) || must(ToJdn(d.Year, d.Month, d.Day)) != jdn {
			t.Fatalf("round trip %d", jdn)
		}
		consecutive := (d.Day == prev.Day+1 && d.Month == prev.Month && d.Year == prev.Year) ||
			(d.Day == 1 && ((d.Month == prev.Month+1 && d.Year == prev.Year) ||
				(d.Month == 1 && prev.Month == 12 && d.Year == prev.Year+1)))
		if !consecutive {
			t.Fatalf("not consecutive at %d", jdn)
		}
		prev = d
	}
}

// Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
func TestExtrapolatedYears(t *testing.T) {
	if must(ToJdn(-1000, 1, 1)) != 1582713 || must(ToJdn(0, 1, 1)) != 1947955 || must(ToJdn(4000, 1, 1)) != 3408925 {
		t.Fatal("fixed values")
	}
	if fj(0) != (Date{-5334, 9, 3}) || must(Weekday(-1000, 1, 1)) != 1 || !IsLeapYear(-3) || IsLeapYear(-1000) {
		t.Fatal("fixed values")
	}
	for _, r := range [][2]int{{-1000, 1177}, {3001, 4000}} {
		first := must(ToJdn(r[0], 1, 1))
		last := must(ToJdn(r[1], 12, DaysInMonth(r[1], 12)))
		prev := fj(first - 1)
		for jdn := first; jdn <= last; jdn++ {
			d := fj(jdn)
			if !IsValid(d.Year, d.Month, d.Day) || must(ToJdn(d.Year, d.Month, d.Day)) != jdn {
				t.Fatalf("round trip %d", jdn)
			}
			consecutive := (d.Day == prev.Day+1 && d.Month == prev.Month && d.Year == prev.Year) ||
				(d.Day == 1 && ((d.Month == prev.Month+1 && d.Year == prev.Year) ||
					(d.Month == 1 && prev.Month == 12 && d.Year == prev.Year+1)))
			if !consecutive {
				t.Fatalf("not consecutive at %d", jdn)
			}
			prev = d
		}
	}
}

func TestValidationAndKnownDates(t *testing.T) {
	cases := []struct {
		y, m, d int
		ok      bool
	}{{1403, 12, 30, true}, {1404, 12, 30, false}, {1404, 6, 31, true}, {1404, 7, 31, false},
		{1404, 13, 1, false}, {1404, 0, 1, false}, {1404, 1, 0, false}}
	for _, c := range cases {
		if IsValid(c.y, c.m, c.d) != c.ok {
			t.Fatalf("IsValid %v", c)
		}
	}
	if _, err := ToJdn(1404, 12, 30); err == nil {
		t.Fatal("expected error")
	}
	if ft(time.Date(2025, 3, 21, 0, 0, 0, 0, time.UTC)) != (Date{1404, 1, 1}) {
		t.Fatal("2025-03-21")
	}
	if ft(time.Date(1979, 2, 11, 0, 0, 0, 0, time.UTC)) != (Date{1357, 11, 22}) {
		t.Fatal("1979-02-11")
	}
	// Time zone matters: 2025-03-20T21:00Z is 1 Farvardin in Tehran, 30 Esfand in UTC.
	instant := time.Date(2025, 3, 20, 21, 0, 0, 0, time.UTC)
	if tehran, err := time.LoadLocation("Asia/Tehran"); err == nil {
		if ft(instant.In(tehran)) != (Date{1404, 1, 1}) {
			t.Fatal("Tehran time zone")
		}
	}
	if ft(instant) != (Date{1403, 12, 30}) {
		t.Fatal("UTC")
	}
	if WeekdayNamesEn()[must(Weekday(1404, 1, 1))] != "Friday" || must(DayOfYear(1404, 7, 1)) != 187 {
		t.Fatal("weekday/day of year")
	}
}

// Every year MinYear..MaxYear: years are contiguous, first and last day round-trip, and the leap
// years sit where every other implementation (and ICU 77.1+) puts them (checksums). int64 sums:
// the Nowruz sum does not fit in a 32-bit int.
func TestEveryYearOfTheFullRange(t *testing.T) {
	expected := MinJdn
	var leaps, nowruzSum int64
	for y := MinYear; y <= MaxYear; y++ {
		nowruz := must(ToJdn(y, 1, 1))
		leap := 0
		if IsLeapYear(y) {
			leap = 1
		}
		if nowruz != expected || fj(nowruz) != (Date{y, 1, 1}) || DaysInMonth(y, 12) != 29+leap ||
			fj(nowruz+364+leap) != (Date{y, 12, 29 + leap}) {
			t.Fatalf("year %d", y)
		}
		leaps += int64(leap)
		nowruzSum += int64(nowruz)
		expected = nowruz + 365 + leap
	}
	if expected-1 != MaxJdn || leaps != 48485 || nowruzSum != 389592978180 {
		t.Fatalf("checksums: last %d, leaps %d, sum %d", expected-1, leaps, nowruzSum)
	}
}
