// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Evaluates samanzamani/PersianDate (Java/Android), built from source. See run-saman.sh.
import java.io.PrintWriter;
import saman.zamani.persiandate.PersianDate;

public class Saman {
    public static void main(String[] args) throws Exception {
        String name = "samanzamani/PersianDate " + args[0] + " (Java)";
        String file = args[1] + "/samanzamani_PersianDate_" + args[0];
        try (PrintWriter w = new PrintWriter(file + ".txt", "UTF-8")) {
            w.print("# " + name + "\n");
            for (int y = 1100; y <= 3300; y++) {
                try {
                    PersianDate p = new PersianDate().initJalaliDate(y, 1, 1);
                    w.printf("%d %04d-%02d-%02d\n", y, p.getGrgYear(), p.getGrgMonth(), p.getGrgDay());
                } catch (RuntimeException e) {
                    // year not supported
                }
            }
        }
        int days = 0, g2p = 0, p2g = 0;
        for (long jdn = PersianCalendar.toJdn(1300, 1, 1); jdn <= PersianCalendar.toJdn(1500, 12, 29); jdn++, days++) {
            PersianCalendar.PersianDate e = PersianCalendar.fromJdn(jdn);
            java.time.LocalDate g = PersianCalendar.toLocalDate(e.year(), e.month(), e.day());
            try {
                PersianDate a = new PersianDate().initGrgDate(g.getYear(), g.getMonthValue(), g.getDayOfMonth());
                if (a.getShYear() != e.year() || a.getShMonth() != e.month() || a.getShDay() != e.day()) g2p++;
            } catch (RuntimeException ex) {
                g2p++;
            }
            try {
                PersianDate b = new PersianDate().initJalaliDate(e.year(), e.month(), e.day());
                if (b.getGrgYear() != g.getYear() || b.getGrgMonth() != g.getMonthValue() || b.getGrgDay() != g.getDayOfMonth()) p2g++;
            } catch (RuntimeException ex) {
                p2g++;
            }
        }
        try (PrintWriter w = new PrintWriter(file + ".everyday.json", "UTF-8")) {
            w.printf("{\"library\": \"%s\", \"days\": %d, \"g2pWrong\": %d, \"p2gWrong\": %d}\n", name, days, g2p, p2g);
        }
        System.out.printf("%s: %d days, wrong G->P %d, P->G %d%n", name, days, g2p, p2g);
    }
}
