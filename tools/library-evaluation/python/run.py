# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT

"""Evaluates Python libraries. Writes ../results/<library>.txt and <library>.everyday.json."""
import datetime
import importlib.metadata as md
import json
import os
import re
import sys

import convertdate.persian as cp
import jdatetime
import khayyam
import persiantools.jdatetime as pt

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "..", "implementations", "python"))
import persian_calendar as ref  # noqa: E402

RESULTS = os.path.join(HERE, "..", "results")


def ymd(d):
    return (d.year, d.month, d.day)


LIBS = {
    f"jdatetime {md.version('jdatetime')}": (
        lambda y, m, d: jdatetime.date(y, m, d).togregorian(),
        lambda g: ymd(jdatetime.date.fromgregorian(date=g))),
    f"persiantools {md.version('persiantools')}": (
        lambda y, m, d: pt.JalaliDate(y, m, d).to_gregorian(),
        lambda g: ymd(pt.JalaliDate(g))),
    f"convertdate {md.version('convertdate')}": (
        lambda y, m, d: datetime.date(*cp.to_gregorian(y, m, d)),
        lambda g: tuple(cp.from_gregorian(g.year, g.month, g.day))),
    f"khayyam {md.version('khayyam')}": (
        lambda y, m, d: khayyam.JalaliDate(y, m, d).todate(),
        lambda g: ymd(khayyam.JalaliDate(g))),
}


def safe(f, *a):
    try:
        return f(*a)
    except Exception:
        return None


for name, (p2g, g2p) in LIBS.items():
    file = re.sub(r"[^\w.+-]+", "_", name)
    lines = [f"# {name}"]
    for y in range(1100, 3301):
        g = safe(p2g, y, 1, 1)
        if g:
            lines.append(f"{y} {g.isoformat()}")
    with open(os.path.join(RESULTS, file + ".txt"), "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    days = g2p_wrong = p2g_wrong = 0
    for jdn in range(ref.to_jdn(1300, 1, 1), ref.to_jdn(1500, 12, 29) + 1):
        g = datetime.date(*ref.jdn_to_gregorian(jdn))
        p = ref.from_jdn(jdn)
        days += 1
        g2p_wrong += safe(lambda: tuple(g2p(g))) != p
        p2g_wrong += safe(p2g, *p) != g
    with open(os.path.join(RESULTS, file + ".everyday.json"), "w", newline="\n") as f:
        json.dump({"library": name, "days": days, "g2pWrong": g2p_wrong, "p2gWrong": p2g_wrong}, f)
        f.write("\n")
    print(name, days, g2p_wrong, p2g_wrong)
