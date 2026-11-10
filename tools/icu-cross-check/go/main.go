// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Command icucrosscheck compares ICU4C (called directly, without cgo, through purego), this
// repository's Go implementation and the ground truth (data/years.csv) for every day of
// 1178-3000 AP, in both directions.
//
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the program checks that ICU matches the plain 33-year rule exactly instead.
//
// Usage:
//
//	go run .                     # finds the system ICU (Linux, macOS/Homebrew)
//	go run . /path/to/icu/lib    # folder with libicui18n.so.NN / icuinNN.dll (Windows)
package main

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"unsafe"

	"github.com/ebitengine/purego"

	pc "github.com/Goudarz/PersianCalendar/implementations/go"
)

const (
	ucalMonth        = 2
	ucalDate         = 5
	ucalExtendedYear = 19
	ucalJulianDay    = 20
)

func findICU(folder string) (i18n, uc string, major int) {
	dirs := []string{"/usr/lib/x86_64-linux-gnu", "/usr/lib/aarch64-linux-gnu", "/usr/lib64", "/usr/lib",
		"/opt/homebrew/opt/icu4c/lib", "/usr/local/opt/icu4c/lib"}
	if folder != "" {
		dirs = []string{folder}
	}
	re := regexp.MustCompile(`(?:icuin|so\.|i18n\.)(\d+)`)
	for _, dir := range dirs {
		var candidates []string
		for _, pattern := range []string{"icuin[0-9]*.dll", "libicui18n.so.[0-9]*", "libicui18n.[0-9]*.dylib"} {
			m, _ := filepath.Glob(filepath.Join(dir, pattern))
			candidates = append(candidates, m...)
		}
		sort.Sort(sort.Reverse(sort.StringSlice(candidates)))
		for _, c := range candidates {
			m := re.FindStringSubmatch(filepath.Base(c))
			u := strings.Replace(strings.Replace(c, "icuin", "icuuc", 1), "libicui18n", "libicuuc", 1)
			if _, err := os.Stat(u); m != nil && err == nil {
				major, _ = strconv.Atoi(m[1])
				return c, u, major
			}
		}
	}
	fmt.Fprintln(os.Stderr, "ICU not found; pass the folder containing the ICU libraries")
	os.Exit(2)
	return
}

// register binds fn to an ICU function, trying the version-suffixed name first (ucal_open_77).
func register(lib uintptr, fn any, name string, major int) {
	for _, s := range []string{fmt.Sprintf("%s_%d", name, major), name} {
		if addr, err := symbol(lib, s); err == nil && addr != 0 {
			purego.RegisterFunc(fn, addr)
			return
		}
	}
	fmt.Fprintf(os.Stderr, "symbol %s not found\n", name)
	os.Exit(2)
}

func main() {
	folder := ""
	if len(os.Args) > 1 {
		folder = os.Args[1]
	}
	i18nPath, ucPath, major := findICU(folder)
	uc, err := openLibrary(ucPath)
	if err != nil {
		panic(err)
	}
	i18n, err := openLibrary(i18nPath)
	if err != nil {
		panic(err)
	}

	var (
		getVersion func(*[4]byte)
		ucalOpen   func(zone uintptr, zoneLen int32, locale *byte, typ int32, status *int32) uintptr
		ucalClear  func(cal uintptr)
		ucalSet    func(cal uintptr, field int32, value int32)
		ucalGet    func(cal uintptr, field int32, status *int32) int32
	)
	register(uc, &getVersion, "u_getVersion", major)
	register(i18n, &ucalOpen, "ucal_open", major)
	register(i18n, &ucalClear, "ucal_clear", major)
	register(i18n, &ucalSet, "ucal_set", major)
	register(i18n, &ucalGet, "ucal_get", major)

	var v [4]byte
	getVersion(&v)
	version := fmt.Sprintf("%d.%d.%d.%d", v[0], v[1], v[2], v[3])
	for strings.HasSuffix(version, ".0") { // 79.1.0.0 -> 79.1, 80.0.0.0 -> 80
		version = strings.TrimSuffix(version, ".0")
	}
	var status int32
	locale := append([]byte("fa_IR@calendar=persian"), 0)
	cal := ucalOpen(0, 0, (*byte)(unsafe.Pointer(&locale[0])), 0, &status)
	if status > 0 || cal == 0 {
		panic(fmt.Sprintf("ucal_open failed: %d", status))
	}
	icuFromJdn := func(jdn int) pc.Date {
		ucalClear(cal)
		ucalSet(cal, ucalJulianDay, int32(jdn))
		return pc.Date{Year: int(ucalGet(cal, ucalExtendedYear, &status)),
			Month: int(ucalGet(cal, ucalMonth, &status)) + 1, // ICU months are 0-based
			Day:   int(ucalGet(cal, ucalDate, &status))}
	}
	icuToJdn := func(d pc.Date) int {
		ucalClear(cal)
		ucalSet(cal, ucalExtendedYear, int32(d.Year))
		ucalSet(cal, ucalMonth, int32(d.Month-1))
		ucalSet(cal, ucalDate, int32(d.Day))
		return int(ucalGet(cal, ucalJulianDay, &status))
	}

	// The plain 33-year rule (what ICU 50-76 implements). Years here are positive.
	plainNowruz := func(y int) int { return 1948320 + 365*(y-1) + (8*y+21)/33 }
	monthDay := func(y, doy int) pc.Date {
		if doy < 186 {
			return pc.Date{Year: y, Month: doy/31 + 1, Day: doy%31 + 1}
		}
		return pc.Date{Year: y, Month: (doy-186)/30 + 7, Day: (doy-186)%30 + 1}
	}
	plainFromJdn := func(jdn int) pc.Date {
		y := (jdn-1948320)/366 + 1
		for jdn >= plainNowruz(y+1) {
			y++
		}
		for jdn < plainNowruz(y) {
			y--
		}
		return monthDay(y, jdn-plainNowruz(y))
	}

	hasCorrections := major >= 77
	data, err := os.ReadFile(filepath.Join("..", "..", "..", "data", "years.csv"))
	if err != nil {
		panic(err)
	}
	var days, refWrong, toPersianWrong, fromPersianWrong, differs int
	for _, line := range strings.Split(strings.TrimSpace(string(data)), "\n")[1:] {
		f := strings.Split(line, ",")
		year, _ := strconv.Atoi(f[0])
		nowruz, _ := strconv.Atoi(f[4])
		length := 365
		if f[1] == "1" {
			length = 366
		}
		for doy := 0; doy < length; doy++ {
			days++
			jdn := nowruz + doy
			truth := monthDay(year, doy)
			back, err := pc.ToJdn(truth.Year, truth.Month, truth.Day)
			ours, err2 := pc.FromJdn(jdn)
			if ours != truth || err != nil || err2 != nil || back != jdn {
				refWrong++
			}
			expected := truth
			if !hasCorrections {
				expected = plainFromJdn(jdn)
			}
			icu := icuFromJdn(jdn)
			if icu != expected {
				toPersianWrong++
			}
			if icu != truth {
				differs++
			}
			if icuToJdn(expected) != jdn {
				fromPersianWrong++
			}
		}
	}
	rule, ok := "the ground truth", "yes (both directions)"
	if !hasCorrections {
		rule = "the plain 33-year rule"
	}
	if toPersianWrong+fromPersianWrong != 0 {
		ok = fmt.Sprintf("NO (G->P %d, P->G %d)", toPersianWrong, fromPersianWrong)
	}
	fmt.Printf("Go + ICU4C %s (purego): %d days (1178-3000 AP) | implementation wrong: %d | ICU differs from ground truth on %d days | ICU matches %s: %s\n",
		version, days, refWrong, differs, rule, ok)
	if refWrong+toPersianWrong+fromPersianWrong != 0 {
		os.Exit(1)
	}
}
