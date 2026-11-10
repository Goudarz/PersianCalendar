// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Checks SolarHijri against /data. Run: dotnet run --project implementations/csharp/tests
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using SolarHijriCalendar;

int checks = 0;
void Check(bool ok, object context)
{
    checks++;
    if (!ok) throw new Exception($"failed: {context}");
}

string dataDir = Environment.GetEnvironmentVariable("DATA_DIR")
    ?? Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "..", "..", "data");
string[][] Csv(string name) => File.ReadAllLines(Path.Combine(dataDir, name))
    .Skip(1).Where(l => l.Length > 0).Select(l => l.Split(',')).ToArray();

foreach (var r in Csv("years.csv"))
{
    int y = int.Parse(r[0]);
    bool leap = r[1] == "1";
    Check(SolarHijri.IsLeapYear(y) == leap, $"leap {y}");
    Check(SolarHijri.DaysInMonth(y, 12) == (leap ? 30 : 29), $"esfand {y}");
    Check(SolarHijri.ToDateOnly(y, 1, 1) == DateOnly.Parse(r[3], System.Globalization.CultureInfo.InvariantCulture), $"nowruz {y}");
    Check(SolarHijri.ToJdn(y, 1, 1) == long.Parse(r[4]), $"jdn {y}");
    // every day of the year against the ground truth, both directions
    for (int doy = 0; doy < int.Parse(r[2]); doy++)
    {
        int m = doy < 186 ? doy / 31 + 1 : (doy - 186) / 30 + 7;
        int d = doy < 186 ? doy % 31 + 1 : (doy - 186) % 30 + 1;
        long jdn = long.Parse(r[4]) + doy;
        Check(SolarHijri.FromJdn(jdn) == new PersianDate(y, m, d) && SolarHijri.ToJdn(y, m, d) == jdn, $"{y}/{m}/{d}");
    }
    Check(SolarHijri.DayOfWeek(y, 1, 1) == int.Parse(r[5]), $"weekday {y}");
}

foreach (var r in Csv("test-vectors.csv"))
{
    var v = r.Select(int.Parse).ToArray();
    var p = new PersianDate(v[0], v[1], v[2]);
    var g = new DateOnly(v[3], v[4], v[5]);
    Check(SolarHijri.ToDateOnly(v[0], v[1], v[2]) == g, p);
    Check(SolarHijri.FromDateOnly(g) == p, g);
    Check(SolarHijri.ToJdn(v[0], v[1], v[2]) == v[6], p);
    Check(SolarHijri.FromJdn(v[6]) == p, p);
    Check(SolarHijri.DayOfWeek(v[0], v[1], v[2]) == v[7], p);
}

long first = SolarHijri.ToJdn(SolarHijri.VerifiedMinYear, 1, 1);
long last = SolarHijri.ToJdn(SolarHijri.VerifiedMaxYear, 12, SolarHijri.DaysInMonth(SolarHijri.VerifiedMaxYear, 12));
var prev = SolarHijri.FromJdn(first - 1);
for (long jdn = first; jdn <= last; jdn++)
{
    var d = SolarHijri.FromJdn(jdn);
    Check(SolarHijri.IsValidDate(d.Year, d.Month, d.Day), jdn);
    Check(SolarHijri.ToJdn(d.Year, d.Month, d.Day) == jdn, jdn);
    bool consecutive = (d.Day == prev.Day + 1 && d.Month == prev.Month && d.Year == prev.Year)
        || (d.Day == 1 && ((d.Month == prev.Month + 1 && d.Year == prev.Year)
            || (d.Month == 1 && prev.Month == 12 && d.Year == prev.Year + 1)));
    Check(consecutive, jdn);
    prev = d;
}

// Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
Check(SolarHijri.ToJdn(-1000, 1, 1) == 1582713, "-1000");
Check(SolarHijri.ToJdn(0, 1, 1) == 1947955, "0");
Check(SolarHijri.ToJdn(4000, 1, 1) == 3408925, "4000");
Check(SolarHijri.FromJdn(0) == new PersianDate(-5334, 9, 3), "jdn 0");
Check(SolarHijri.DayOfWeek(-1000, 1, 1) == 1, "weekday -1000");
Check(SolarHijri.IsLeapYear(-3) && !SolarHijri.IsLeapYear(-1000), "negative leap years");
foreach (var (firstYear, lastYear) in new[] { (-1000, 1177), (3001, 4000) })
{
    long a = SolarHijri.ToJdn(firstYear, 1, 1);
    long b = SolarHijri.ToJdn(lastYear, 12, SolarHijri.DaysInMonth(lastYear, 12));
    var p = SolarHijri.FromJdn(a - 1);
    for (long jdn = a; jdn <= b; jdn++)
    {
        var d = SolarHijri.FromJdn(jdn);
        Check(SolarHijri.IsValidDate(d.Year, d.Month, d.Day) && SolarHijri.ToJdn(d.Year, d.Month, d.Day) == jdn, jdn);
        bool consecutive = (d.Day == p.Day + 1 && d.Month == p.Month && d.Year == p.Year)
            || (d.Day == 1 && ((d.Month == p.Month + 1 && d.Year == p.Year)
                || (d.Month == 1 && p.Month == 12 && d.Year == p.Year + 1)));
        Check(consecutive, jdn);
        p = d;
    }
}
Check(SolarHijri.IsValidDate(1403, 12, 30), "1403/12/30");
Check(!SolarHijri.IsValidDate(1404, 12, 30), "1404/12/30");
Check(SolarHijri.IsValidDate(1404, 6, 31), "1404/6/31");
Check(!SolarHijri.IsValidDate(1404, 7, 31), "1404/7/31");
Check(!SolarHijri.IsValidDate(1404, 13, 1), "1404/13/1");
Check(!SolarHijri.IsValidDate(1404, 0, 1), "1404/0/1");
Check(!SolarHijri.IsValidDate(1404, 1, 0), "1404/1/0");
try { SolarHijri.ToJdn(1404, 12, 30); Check(false, "should throw"); }
catch (ArgumentException) { checks++; }
Check(SolarHijri.FromDateOnly(new DateOnly(2025, 3, 21)) == new PersianDate(1404, 1, 1), "2025-03-21");
Check(SolarHijri.FromDateOnly(new DateOnly(1979, 2, 11)) == new PersianDate(1357, 11, 22), "1979-02-11");
Check(SolarHijri.DayOfYear(1404, 7, 1) == 187, "doy");
Check(SolarHijri.WeekdayNamesEn[SolarHijri.DayOfWeek(1404, 1, 1)] == "Friday", "friday");
// Limits: exact boundaries, and fast rejection of anything outside (no hang, no overflow).
Check(SolarHijri.ToJdn(SolarHijri.MinYear, 1, 1) == SolarHijri.MinJdn, "MinJdn");
Check(SolarHijri.ToJdn(SolarHijri.MaxYear, 12, 29) == SolarHijri.MaxJdn, "MaxJdn");
Check(SolarHijri.FromJdn(SolarHijri.MinJdn) == new PersianDate(-100_000, 1, 1), "from MinJdn");
Check(SolarHijri.FromJdn(SolarHijri.MaxJdn) == new PersianDate(100_000, 12, 29), "from MaxJdn");
bool Throws<TException>(Action a) where TException : Exception
{
    try { a(); return false; } catch (TException) { return true; }
}
foreach (long bad in new[] { SolarHijri.MinJdn - 1, SolarHijri.MaxJdn + 1, long.MaxValue, long.MinValue, 1L << 53 })
    Check(Throws<ArgumentOutOfRangeException>(() => SolarHijri.FromJdn(bad)), $"FromJdn {bad}");
foreach (int bad in new[] { SolarHijri.MinYear - 1, SolarHijri.MaxYear + 1, int.MaxValue, int.MinValue })
{
    Check(!SolarHijri.IsValidDate(bad, 1, 1), $"IsValidDate {bad}");
    Check(Throws<ArgumentException>(() => SolarHijri.ToJdn(bad, 1, 1)), $"ToJdn {bad}");
    Check(Throws<ArgumentOutOfRangeException>(() => SolarHijri.IsLeapYear(bad)), $"IsLeapYear {bad}");
}
Check(Throws<NotSupportedException>(() => ((IList<string>)SolarHijri.MonthNamesFa)[0] = "X"), "immutable names");
Check(SolarHijri.MonthNamesFa is not string[], "names are not a writable array");
// Every year MinYear..MaxYear: years are contiguous, first and last day round-trip, and the leap
// years sit where every other implementation (and ICU 77.1+) puts them (checksums).
long expectedNowruz = SolarHijri.MinJdn, leaps = 0, nowruzSum = 0;
for (int y = SolarHijri.MinYear; y <= SolarHijri.MaxYear; y++)
{
    long nowruz = SolarHijri.ToJdn(y, 1, 1);
    int leap = SolarHijri.IsLeapYear(y) ? 1 : 0;
    if (nowruz != expectedNowruz || SolarHijri.FromJdn(nowruz) != new PersianDate(y, 1, 1)
        || SolarHijri.DaysInMonth(y, 12) != 29 + leap || SolarHijri.FromJdn(nowruz + 364 + leap) != new PersianDate(y, 12, 29 + leap))
        Check(false, $"year {y}");
    leaps += leap;
    nowruzSum += nowruz;
    expectedNowruz = nowruz + 365 + leap;
}
Check(expectedNowruz - 1 == SolarHijri.MaxJdn && leaps == 48_485 && nowruzSum == 389_592_978_180, "full-range checksums");
Console.WriteLine($"OK: {checks} checks passed");
