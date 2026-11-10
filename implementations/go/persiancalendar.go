// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Package persiancalendar implements the Solar Hijri
// (Persian, Jalali, Shamsi) calendar.
//
// Algorithm: 33-year arithmetic rule + 78-year correction table, the
// same algorithm as ICU 77.1+ and ICU4X. It matches the
// astronomical calendar (52.5 E meridian) for 1178-3000 AP. Months are
// 1-based; weekdays 0 = Saturday ... 6 = Friday.
package persiancalendar

import (
	"fmt"
	"time"
)

// Date is a Persian calendar date.
type Date struct {
	Year  int
	Month int // 1 = Farvardin ... 12 = Esfand
	Day   int
}

const (
	// VerifiedMinYear and VerifiedMaxYear bound the years checked against the astronomical calendar.
	VerifiedMinYear = 1178
	VerifiedMaxYear = 3000

	// MinYear..MaxYear and MinJdn..MaxJdn are the accepted input range. Within it every
	// intermediate value fits in 32 bits, so the package is also safe where int is 32 bits.
	MinYear = -100000
	MaxYear = 100000
	MinJdn  = -34576287 // 1 Farvardin -100000 (Gregorian -99379-03-29)
	MaxJdn  = 38472562  // 29 Esfand 100000 (Gregorian 100622-03-13)

	epochJdn     = 1948320
	unixEpochJdn = 2440588 // JDN of 1970-01-01
)

// Years the 33-year rule calls leap but are not; the next year is leap instead.
var nonLeapCorrection = map[int]bool{}

func init() {
	for _, y := range []int{
		1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
		1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
		2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
		2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
		2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
		2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
		2946, 2950, 2954, 2979, 2983, 2987,
	} {
		nonLeapCorrection[y] = true
	}
}

var (
	monthNamesFa   = [12]string{"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"}
	monthNamesEn   = [12]string{"Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"}
	weekdayNamesFa = [7]string{"شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"}
	weekdayNamesEn = [7]string{"Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"}
)

// The name tables are returned as array values (copies), so callers cannot change them.

// MonthNamesFa returns the Persian month names; index 0 is Farvardin.
func MonthNamesFa() [12]string { return monthNamesFa }

// MonthNamesEn returns the CLDR English month names; index 0 is Farvardin.
func MonthNamesEn() [12]string { return monthNamesEn }

// WeekdayNamesFa returns the Persian weekday names; index 0 is Saturday.
func WeekdayNamesFa() [7]string { return weekdayNamesFa }

// WeekdayNamesEn returns the English weekday names; index 0 is Saturday.
func WeekdayNamesEn() [7]string { return weekdayNamesEn }

// Go's / and % truncate toward zero; the formulas need floor semantics.
func floorDiv(a, b int) int {
	q := a / b
	if (a%b != 0) && ((a < 0) != (b < 0)) {
		q--
	}
	return q
}

func floorMod(a, b int) int { return a - b*floorDiv(a, b) }

// IsLeapYear reports whether the Persian year has 366 days. It never overflows:
// (25*year + 11) mod 33 is computed from year mod 33.
func IsLeapYear(year int) bool {
	if year >= 1502 && year <= 2988 {
		if nonLeapCorrection[year] {
			return false
		}
		if nonLeapCorrection[year-1] {
			return true
		}
	}
	return floorMod(25*floorMod(year, 33)+11, 33) < 8
}

// DaysInMonth returns 29, 30 or 31. It panics if month is not 1..12 (a programming error;
// use IsValid to check user input).
func DaysInMonth(year, month int) int {
	switch {
	case month < 1 || month > 12:
		panic(fmt.Sprintf("month out of range: %d", month))
	case month <= 6:
		return 31
	case month <= 11:
		return 30
	case IsLeapYear(year):
		return 30
	default:
		return 29
	}
}

// IsValid reports whether year/month/day is a real Persian date in MinYear..MaxYear.
func IsValid(year, month, day int) bool {
	return year >= MinYear && year <= MaxYear && month >= 1 && month <= 12 && day >= 1 && day <= DaysInMonth(year, month)
}

func nowruzJdn(year int) int {
	jdn := epochJdn + 365*(year-1) + floorDiv(8*year+21, 33)
	if nonLeapCorrection[year-1] {
		jdn--
	}
	return jdn
}

func daysBeforeMonth(month int) int {
	if month <= 7 {
		return 31 * (month - 1)
	}
	return 30*(month-1) + 6
}

// ToJdn returns the Julian Day Number of a Persian date.
func ToJdn(year, month, day int) (int, error) {
	if !IsValid(year, month, day) {
		return 0, fmt.Errorf("invalid Persian date: %d/%d/%d", year, month, day)
	}
	return nowruzJdn(year) + daysBeforeMonth(month) + day - 1, nil
}

// FromJdn returns the Persian date of a Julian Day Number in MinJdn..MaxJdn.
func FromJdn(jdn int) (Date, error) {
	if jdn < MinJdn || jdn > MaxJdn {
		return Date{}, fmt.Errorf("jdn out of range [%d, %d]: %d", MinJdn, MaxJdn, jdn)
	}
	year := 1 + floorDiv(33*(jdn-epochJdn)+3, 12053)
	for jdn >= nowruzJdn(year+1) {
		year++
	}
	for jdn < nowruzJdn(year) {
		year--
	}
	doy := jdn - nowruzJdn(year) // 0-based
	if doy < 186 {
		return Date{year, doy/31 + 1, doy%31 + 1}, nil
	}
	doy -= 186
	return Date{year, doy/30 + 7, doy%30 + 1}, nil
}

// FromTime returns the Persian date of t in t's own location.
// Use t.In(loc) first to choose the time zone (e.g. Asia/Tehran).
func FromTime(t time.Time) (Date, error) {
	y, m, d := t.Date()
	civil := time.Date(y, m, d, 0, 0, 0, 0, time.UTC)
	jdn := civil.Unix()/86400 + unixEpochJdn // int64: checked before converting to int
	if jdn < MinJdn || jdn > MaxJdn {
		return Date{}, fmt.Errorf("date out of range: %s", civil.Format("2006-01-02"))
	}
	return FromJdn(int(jdn))
}

// ToTime returns midnight of the Persian date in loc.
func ToTime(year, month, day int, loc *time.Location) (time.Time, error) {
	jdn, err := ToJdn(year, month, day)
	if err != nil {
		return time.Time{}, err
	}
	g := time.Unix(int64(jdn-unixEpochJdn)*86400, 0).UTC()
	return time.Date(g.Year(), g.Month(), g.Day(), 0, 0, 0, 0, loc), nil
}

// Weekday returns 0 = Saturday (شنبه) ... 6 = Friday (جمعه).
func Weekday(year, month, day int) (int, error) {
	jdn, err := ToJdn(year, month, day)
	if err != nil {
		return 0, err
	}
	return floorMod(jdn+2, 7), nil
}

// DayOfYear returns 1 ... 365/366.
func DayOfYear(year, month, day int) (int, error) {
	if !IsValid(year, month, day) {
		return 0, fmt.Errorf("invalid Persian date: %d/%d/%d", year, month, day)
	}
	return daysBeforeMonth(month) + day, nil
}
