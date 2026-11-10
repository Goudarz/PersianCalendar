// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares, for every day of 1178-3000 AP and in both directions:
//   1. the ground truth (data/years.csv),
//   2. this repository's C# implementation (SolarHijri),
//   3. ICU4C, called directly through its C API (loaded at run time, no bindings),
//   4. .NET's own System.Globalization.PersianCalendar (managed code, not ICU).
//
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the program checks that ICU matches the plain 33-year rule exactly instead.
// .NET's PersianCalendar is only reported (it is known to be wrong from 1535 AP on).
//
// Usage: dotnet run -c Release -- [folder with icuinNN.dll / libicui18n.so.NN]
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text.RegularExpressions;
using SolarHijriCalendar;

const int UcalMonth = 2, UcalDate = 5, UcalExtendedYear = 19, UcalJulianDay = 20;

string root = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "..", ".."));
var years = File.ReadAllLines(Path.Combine(root, "data", "years.csv")).Skip(1).Where(l => l.Length > 0)
    .Select(l => l.Split(',')).ToArray();

// Finds ICU: (i18n library, common library, major version).
(string, string, int) FindIcu(string? folder)
{
    string[] dirs = folder != null ? new[] { folder } : new[] { "/usr/lib/x86_64-linux-gnu", "/usr/lib/aarch64-linux-gnu",
        "/usr/lib64", "/usr/lib", "/opt/homebrew/opt/icu4c/lib", "/usr/local/opt/icu4c/lib" };
    foreach (var dir in dirs.Where(Directory.Exists))
    {
        var candidates = new[] { "icuin*.dll", "libicui18n.so.*", "libicui18n.*.dylib" }
            .SelectMany(p => Directory.GetFiles(dir, p)).OrderByDescending(f => f, StringComparer.Ordinal);
        foreach (var i18n in candidates)
        {
            var m = Regex.Match(Path.GetFileName(i18n), @"(?:icuin|so\.|i18n\.)(\d+)");
            string uc = i18n.Replace("icuin", "icuuc").Replace("libicui18n", "libicuuc");
            if (m.Success && File.Exists(uc)) return (i18n, uc, int.Parse(m.Groups[1].Value, CultureInfo.InvariantCulture));
        }
    }
    Console.Error.WriteLine("ICU not found; pass the folder containing the ICU libraries");
    Environment.Exit(2);
    return default;
}

var (i18nPath, ucPath, major) = FindIcu(args.Length > 0 ? args[0] : null);
IntPtr ucLib = NativeLibrary.Load(ucPath), i18nLib = NativeLibrary.Load(i18nPath);
// ICU usually suffixes its symbols with the major version (ucal_open_77).
IntPtr Symbol(IntPtr lib, string name)
{
    foreach (var s in new[] { $"{name}_{major}", name })
        if (NativeLibrary.TryGetExport(lib, s, out var address)) return address;
    throw new EntryPointNotFoundException(name);
}

unsafe
{
    var getVersion = (delegate* unmanaged<byte*, void>)Symbol(ucLib, "u_getVersion");
    var ucalOpen = (delegate* unmanaged<IntPtr, int, byte*, int, int*, IntPtr>)Symbol(i18nLib, "ucal_open");
    var ucalClear = (delegate* unmanaged<IntPtr, void>)Symbol(i18nLib, "ucal_clear");
    var ucalSet = (delegate* unmanaged<IntPtr, int, int, void>)Symbol(i18nLib, "ucal_set");
    var ucalGet = (delegate* unmanaged<IntPtr, int, int*, int>)Symbol(i18nLib, "ucal_get");

    byte* v = stackalloc byte[4];
    getVersion(v);
    string version = Regex.Replace($"{v[0]}.{v[1]}.{v[2]}.{v[3]}", @"(\.0)+$", "");
    int status = 0;
    byte[] locale = "fa_IR@calendar=persian\0"u8.ToArray();
    IntPtr cal;
    fixed (byte* l = locale) cal = ucalOpen(IntPtr.Zero, 0, l, 0, &status);
    if (status > 0 || cal == IntPtr.Zero) throw new InvalidOperationException($"ucal_open failed: {status}");

    PersianDate IcuFromJdn(long jdn)
    {
        int s = 0;
        ucalClear(cal);
        ucalSet(cal, UcalJulianDay, (int)jdn);
        return new PersianDate(ucalGet(cal, UcalExtendedYear, &s), ucalGet(cal, UcalMonth, &s) + 1, ucalGet(cal, UcalDate, &s));
    }
    long IcuToJdn(PersianDate d)
    {
        int s = 0;
        ucalClear(cal);
        ucalSet(cal, UcalExtendedYear, d.Year);
        ucalSet(cal, UcalMonth, d.Month - 1); // ICU months are 0-based
        ucalSet(cal, UcalDate, d.Day);
        return ucalGet(cal, UcalJulianDay, &s);
    }

    // The plain 33-year rule (what ICU 50-76 implements). Years here are positive.
    static long PlainNowruz(long y) => 1948320 + 365 * (y - 1) + (8 * y + 21) / 33;
    static PersianDate MonthDay(int y, int doy) => doy < 186
        ? new PersianDate(y, doy / 31 + 1, doy % 31 + 1)
        : new PersianDate(y, (doy - 186) / 30 + 7, (doy - 186) % 30 + 1);
    static PersianDate PlainFromJdn(long jdn)
    {
        long y = (jdn - 1948320) / 366 + 1;
        while (jdn >= PlainNowruz(y + 1)) y++;
        while (jdn < PlainNowruz(y)) y--;
        return MonthDay((int)y, (int)(jdn - PlainNowruz(y)));
    }

    const long DayNumberEpochJdn = 1721426; // JDN of 0001-01-01
    var dotnet = new PersianCalendar();
    bool hasCorrections = major >= 77;
    int days = 0, referenceWrong = 0, toPersianWrong = 0, fromPersianWrong = 0, differs = 0, dotnetWrong = 0;
    var dotnetWrongYears = new SortedSet<int>();
    foreach (var r in years)
    {
        int year = int.Parse(r[0], CultureInfo.InvariantCulture);
        long nowruz = long.Parse(r[4], CultureInfo.InvariantCulture);
        int length = r[1] == "1" ? 366 : 365;
        for (int doy = 0; doy < length; doy++, days++)
        {
            long jdn = nowruz + doy;
            var truth = MonthDay(year, doy);
            if (SolarHijri.FromJdn(jdn) != truth || SolarHijri.ToJdn(truth.Year, truth.Month, truth.Day) != jdn) referenceWrong++;

            var expected = hasCorrections ? truth : PlainFromJdn(jdn);
            var icu = IcuFromJdn(jdn);
            if (icu != expected) toPersianWrong++;
            if (icu != truth) differs++;
            if (IcuToJdn(expected) != jdn) fromPersianWrong++;

            // .NET: both directions; a date .NET considers invalid (30 Esfand) counts as wrong.
            var g = DateOnly.FromDayNumber((int)(jdn - DayNumberEpochJdn)).ToDateTime(TimeOnly.MinValue);
            var n = new PersianDate(dotnet.GetYear(g), dotnet.GetMonth(g), dotnet.GetDayOfMonth(g));
            bool backOk = truth.Day <= dotnet.GetDaysInMonth(truth.Year, truth.Month)
                && dotnet.ToDateTime(truth.Year, truth.Month, truth.Day, 0, 0, 0, 0) == g;
            if (n != truth || !backOk)
            {
                dotnetWrong++;
                dotnetWrongYears.Add(year);
            }
        }
    }
    string rule = hasCorrections ? "the ground truth" : "the plain 33-year rule";
    string ok = toPersianWrong + fromPersianWrong == 0 ? "yes (both directions)" : $"NO (G->P {toPersianWrong}, P->G {fromPersianWrong})";
    Console.WriteLine($"C# + ICU4C {version} (.NET {Environment.Version}): {days} days (1178-3000 AP) | implementation wrong: {referenceWrong} | "
        + $"ICU differs from ground truth on {differs} days | ICU matches {rule}: {ok}");
    Console.WriteLine($"  System.Globalization.PersianCalendar: wrong on {dotnetWrong} days in {dotnetWrongYears.Count} years"
        + (dotnetWrongYears.Count > 0 ? $", first wrong year {dotnetWrongYears.Min}" : ""));
    Environment.Exit(referenceWrong + toPersianWrong + fromPersianWrong == 0 ? 0 : 1);
}
