// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Evaluates System.Globalization.PersianCalendar and two popular NuGet libraries. Run: dotnet run
using System.Globalization;
using DNTPersianUtils.Core;
using SolarHijriCalendar;

var pc = new PersianCalendar();
var libs = new (string Name, Func<int, int, int, DateTime> P2G, Func<DateTime, (int, int, int)> G2P)[]
{
    ($"System.Globalization.PersianCalendar ({System.Runtime.InteropServices.RuntimeInformation.FrameworkDescription})",
        (y, m, d) => pc.ToDateTime(y, m, d, 0, 0, 0, 0),
        g => (pc.GetYear(g), pc.GetMonth(g), pc.GetDayOfMonth(g))),
    ("DNTPersianUtils.Core 7.0.2",
        (y, m, d) => $"{y:0000}/{m:00}/{d:00}".ToGregorianDateTime() ?? throw new ArgumentException(),
        g => { var p = g.ToShortPersianDateString().Split('/').Select(int.Parse).ToArray(); return (p[0], p[1], p[2]); }),
    ("MD.PersianDateTime.Standard 2.6.0",
        (y, m, d) => new MD.PersianDateTime.Standard.PersianDateTime(y, m, d).ToDateTime(),
        g => { var p = new MD.PersianDateTime.Standard.PersianDateTime(g); return (p.Year, p.Month, p.Day); }),
};

foreach (var (name, p2g, g2p) in libs)
{
    var file = "../results/" + System.Text.RegularExpressions.Regex.Replace(name, @"[^A-Za-z0-9.+-]+", "_").Trim('_');
    var sb = new System.Text.StringBuilder($"# {name}\n");
    for (int y = 1100; y <= 3300; y++)
    {
        try { sb.Append($"{y} {p2g(y, 1, 1):yyyy-MM-dd}\n"); } catch { }
    }
    File.WriteAllText(file + ".txt", sb.ToString());

    int days = 0, g2pWrong = 0, p2gWrong = 0;
    for (long jdn = SolarHijri.ToJdn(1300, 1, 1); jdn <= SolarHijri.ToJdn(1500, 12, 29); jdn++, days++)
    {
        var p = SolarHijri.FromJdn(jdn);
        var g = SolarHijri.ToDateOnly(p.Year, p.Month, p.Day).ToDateTime(TimeOnly.MinValue);
        try { if (g2p(g) != (p.Year, p.Month, p.Day)) g2pWrong++; } catch { g2pWrong++; }
        try { if (p2g(p.Year, p.Month, p.Day).Date != g) p2gWrong++; } catch { p2gWrong++; }
    }
    File.WriteAllText(file + ".everyday.json",
        $"{{\"library\": \"{name}\", \"days\": {days}, \"g2pWrong\": {g2pWrong}, \"p2gWrong\": {p2gWrong}}}\n");
    Console.WriteLine($"{name}: {days} days, wrong G->P {g2pWrong}, P->G {p2gWrong}");
}
