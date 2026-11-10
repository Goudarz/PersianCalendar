// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Evaluates ICU4J's Persian calendar. Run (see README): java -cp icu4j.jar Icu4j.java
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import java.io.PrintWriter;

public class Icu4j {
    public static void main(String[] args) throws Exception {
        String name = "ICU4J " + VersionInfo.ICU_VERSION;
        try (PrintWriter w = new PrintWriter("../results/" + name.replaceAll("[^A-Za-z0-9.+-]+", "_") + ".txt", "UTF-8")) {
            w.print("# " + name + "\n");
            // The public way to get the Persian calendar; the PersianCalendar class itself is deprecated.
            Calendar p = Calendar.getInstance(TimeZone.GMT_ZONE, new ULocale("fa_IR@calendar=persian"));
            GregorianCalendar g = new GregorianCalendar(TimeZone.GMT_ZONE);
            g.setGregorianChange(new java.util.Date(Long.MIN_VALUE)); // proleptic Gregorian
            for (int y = 1100; y <= 3300; y++) {
                p.clear();
                p.set(y, 0, 1); // month is 0-based in ICU
                g.setTimeInMillis(p.getTimeInMillis());
                w.printf("%d %04d-%02d-%02d\n", y, g.get(Calendar.YEAR),
                    g.get(Calendar.MONTH) + 1, g.get(Calendar.DAY_OF_MONTH));
            }
        }
        System.out.println(name);
    }
}
