"""Pictures for the detecting attacks video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def sections(name, head):
    """Every section of an act whose header starts with `head`, as lists of lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found


def section(name, head, n=0):
    return sections(name, head)[n]


def columns(ln):
    """A line split on runs of two or more spaces."""
    return re.split(r"\s{2,}", ln.strip())


def minutes(hhmm):
    h, m = hhmm.split(":")[:2]
    return int(h) * 60 + int(m)


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=945):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def clock(d, x0, x1, y, step=3):
    """A day from 00:00 to 24:00 as a line with hour ticks; returns the minutes -> x function."""
    d.line([x0, y, x1, y], fill=LINE, width=4)
    for h in range(0, 25, step):
        x = x0 + (x1 - x0) * h / 24
        d.line([x, y - 10, x, y + 10], fill=LINE, width=3)
        label(d, x, y + 16, "%02d:00" % h, MUTED, 20, False, "ma")
    return lambda m: x0 + (x1 - x0) * m / (24 * 60)


# ------------------------------------------------------------------ figures

def one_rule(img, d, cfg):
    """The core idea: one brute-force rule, its number, and two accounts: a normal one and the attacked one."""
    rule = columns(section("FIVE", "THE CHANGES")[0])
    threshold = int(rule[1].split()[0])
    normal = columns(section("FOUR", "THE NORMAL DAY")[0])
    attack = columns(section("FOUR", "THE ATTACK")[0])
    header(d, 60, 215, "THE RULE (a guess about an attacker, with a number)", CYAN)
    fit_mono(d, 60, 285, "%s: %s" % (rule[0], rule[1]), TEXT, 34, 1800)
    header(d, 60, 360, "THE INPUT (failed logins of one account within 5 minutes)", CYAN)
    rows = [(normal[0].split(",")[0], int(normal[1]), AMBER, normal[2]), (attack[0], int(attack[1]), ROSE, "attack B")]
    sq, gap, x0 = 40, 7, 420
    for k, (who, n, colour, why) in enumerate(rows):
        y = 450 + k * 170
        label(d, 60, y + 4, who, colour, 34, True)
        label(d, 60, y + 48, why, MUTED, 24)
        for i in range(n):
            col = i % 30
            d.rounded_rectangle([x0 + col * (sq + gap), y, x0 + col * (sq + gap) + sq, y + sq], radius=6, fill=colour)
        verdict = "%d failures: %s" % (n, "ALERT" if n >= threshold else "no alert")
        label(d, x0, y + sq + 18, verdict, ROSE if n >= threshold else GREEN, 28, True)
    tx = x0 + threshold * (sq + gap) - gap // 2
    d.line([tx, 430, tx, 790], fill=TEXT, width=5)
    label(d, tx + 12, 795, "threshold %d" % threshold, TEXT, 28, True)
    header(d, 60, 850, "THE RESULT", GREEN)
    label(d, 290, 856, "the normal account stays under the line; the attack crosses it", TEXT, 30)
    caption(d, "A rule is a guess with a number, and the number is measured on a normal day.")


def the_day(img, d, cfg):
    """The whole Monday: the four hidden attacks above the clock, the look-alikes with a time below it."""
    attacks = []
    for ln in section("ONE", "THE HIDDEN ATTACKS"):
        m = re.match(r"\s*([A-D])\s+(.+?)\s+(\d+ events?)\s+(\S+)\s+(.*)", ln)
        if m:
            attacks.append(m.groups())
    looks = [columns(ln) for ln in section("ONE", "THE LOOK-ALIKES")]
    y_clock = 560
    x_of = clock(d, 120, 1800, y_clock)
    header(d, 60, 215, "THE HIDDEN ATTACKS", ROSE)
    cw = 420
    for k, (letter, name, n, when, src) in enumerate(sorted(attacks, key=lambda a: minutes(a[3][:5]))):
        x = 60 + k * (cw + 30)
        panel(d, x, 280, cw, 150, ROSE, 3)
        label(d, x + 20, 292, "%s  %s" % (letter, name), ROSE, 28, True)
        fit_mono(d, x + 20, 340, "%s, %s" % (n, when), TEXT, 24, cw - 40)
        fit_mono(d, x + 20, 380, src, MUTED, 24, cw - 40)
        t = x_of(minutes(when[:5]))
        d.line([x + cw // 2, 430, t, y_clock - 12], fill=ROSE, width=3)
        d.ellipse([t - 12, y_clock - 12, t + 12, y_clock + 12], fill=ROSE)
    timed = sorted((re.findall(r"\d\d:\d\d", desc), name, desc) for name, desc in looks if re.search(r"\d\d:\d\d", desc))
    for k, (times, name, desc) in enumerate(timed):
        x = 60 + k * (cw + 30)
        panel(d, x, 660, cw, 130, AMBER, 3)
        label(d, x + 20, 672, name, AMBER, 26, True)
        who, _, what = desc.partition(": ") if ": " in desc else ("", "", desc)
        fit_mono(d, x + 20, 716, who or what.split(" at ")[0], TEXT, 24, cw - 40)
        fit_mono(d, x + 20, 752, what if who else "at " + what.split(" at ", 1)[1], MUTED, 24, cw - 40)
        for t in times:
            tx = x_of(minutes(t))
            d.line([x + cw // 2, 660, tx, y_clock + 12], fill=AMBER, width=3)
            d.ellipse([tx - 10, y_clock - 10, tx + 10, y_clock + 10], fill=AMBER)
    header(d, 60, 820, "THE LOOK-ALIKES (normal)", AMBER)
    untimed = [name for name, desc in looks if not re.search(r"\d\d:\d\d", desc)]
    label(d, 520, 828, "and all day: " + ", ".join(untimed), AMBER, 26)
    caption(d, "A good rule fires on the red and stays quiet on the amber.")


def worked_out(img, d, cfg):
    """Spraying counted per account and per address; impossible travel worked out as a speed."""
    per_acc = int(section("TWO", "PER ACCOUNT")[0].split()[-1])
    addr = section("TWO", "PER ADDRESS")
    first = re.findall(r"(u\d+) (\d\d:\d\d:\d\d)", addr[0])
    n = int(re.search(r"(\d+) at", addr[1]).group(1))
    alert_at = addr[1].split()[-1]
    step = (minutes(first[1][1][:5]) * 60 + int(first[1][1][6:]) - minutes(first[0][1][:5]) * 60 - int(first[0][1][6:]))
    start = int(first[0][0][1:])
    header(d, 60, 215, "ATTACK A: ONE ADDRESS, MANY ACCOUNTS", ROSE)
    label(d, 60, 285, "PER ACCOUNT (the brute-force rule)", AMBER, 26, True)
    for i in range(10):
        d.rounded_rectangle([60 + i * 52, 330, 104 + i * 52, 374], radius=6, fill=ROSE if i < per_acc else BG_CELL,
                            outline=LINE, width=2)
    label(d, 600, 338, "%d of 10: quiet" % per_acc, AMBER, 28, True)
    label(d, 60, 410, "PER ADDRESS (the spraying rule)", GREEN, 26, True)
    for i in range(n):
        y = 455 + i * 52
        t0 = minutes(first[0][1][:5]) * 60 + int(first[0][1][6:]) + i * step
        stamp = "%02d:%02d:%02d" % (t0 // 3600, t0 // 60 % 60, t0 % 60)
        label(d, 60, y + 6, "u%03d" % (start + i), TEXT, 26, True)
        label(d, 160, y + 6, stamp, MUTED, 24)
        d.rounded_rectangle([320, y, 320 + (i + 1) * 40, y + 38], radius=6, fill=GREEN if i + 1 >= n else LINE)
        label(d, 330 + (i + 1) * 40, y + 6, str(i + 1), GREEN if i + 1 >= n else MUTED, 24, True)
    label(d, 60, 880, "%d different accounts at %s: spraying fires" % (n, alert_at), GREEN, 28, True)

    speeds = []
    for head in ("THE SPEED", "THE NEAR MISS"):
        f = {r[0]: r[1] for r in (columns(ln) for ln in section("TWO", head)) if len(r) == 2}
        verdict = [ln.strip() for ln in section("TWO", head) if " is " in ln][0]
        speeds.append((f, verdict))
    travel = [ln.strip() for ln in section("TWO", "THE INPUT", 1)]
    header(d, 1000, 215, "ATTACK C: A SPEED, WORKED OUT", ROSE)
    for k, ln in enumerate(travel):
        fit_mono(d, 1000, 285 + k * 46, ln, TEXT, 28, 860)
    y = 400
    for k, (f, verdict) in enumerate(speeds):
        colour = ROSE if k == 0 else GREEN
        panel(d, 1000, y, 860, 230, colour, 3)
        label(d, 1030, y + 18, "attack C" if k == 0 else "a real trip (u030)", colour, 26, True)
        fit_mono(d, 1030, y + 62, f["distance"], TEXT, 26, 800)
        fit_mono(d, 1030, y + 102, f["time"], TEXT, 26, 800)
        fit_mono(d, 1030, y + 142, f["speed"], AMBER, 28, 800)
        fit_mono(d, 1030, y + 184, verdict.split("  <-")[0] + ": " + verdict.split("<- ")[1].lower(), colour, 26, 800)
        y += 250
    caption(d, "Count by the right thing: accounts per address, or kilometres per hour.")


def first_version(img, d, cfg):
    """The first rules' alerts as bars, true in green and false in amber, with the look-alikes behind the false ones."""
    rows = [columns(ln) for ln in section("THREE", "THE RESULT")[1:]]
    rows = [r for r in rows if r[0] != "together"]
    total = [columns(ln) for ln in section("THREE", "THE RESULT") if ln.strip().startswith("together")][0]
    why = [columns(ln) for ln in section("THREE", "WHY")]
    header(d, 60, 215, "THE RESULT (alerts per rule)", GREEN)
    biggest = max(int(r[1]) for r in rows)
    x0, unit = 480, 1050 / biggest
    for k, r in enumerate(rows):
        name = r[0]
        alerts, tp, fp = int(r[1]), int(r[2]), int(r[3])
        y = 290 + k * 88
        label(d, 60, y + 10, name, TEXT, 28, True)
        if tp:
            d.rounded_rectangle([x0, y, x0 + tp * unit, y + 50], radius=6, fill=GREEN)
        if fp:
            d.rounded_rectangle([x0 + tp * unit, y, x0 + alerts * unit, y + 50], radius=6, fill=AMBER)
        label(d, x0 + alerts * unit + 14, y + 10, "%d true, %d false" % (tp, fp), MUTED, 26)
    header(d, 60, 740, "WHY THE FALSE ONES (the look-alike behind each)", AMBER)
    for k, w in enumerate(why):
        x, y = 60 + (k % 2) * 920, 800 + (k // 2) * 44
        label(d, x, y, "%s %s, from %s" % (w[1], w[2], w[0]), AMBER, 26)
    caption(d, "%s alerts, %s true and %s false: every attack seen, the analyst buried." % (total[1], total[2], total[3]))


def sweep(img, d, cfg):
    """A ruler of failures: the normal day, the attack, the safe range between them, and the thresholds tried."""
    normal = [columns(ln) for ln in section("FOUR", "THE NORMAL DAY")]
    attack = columns(section("FOUR", "THE ATTACK")[0])
    safe = {r[0]: r[1] for r in (columns(ln) for ln in section("FOUR", "THE SAFE RANGE"))}
    lo, hi = [int(x) for x in re.findall(r"\d+", safe["so the threshold is"])[:2]]
    chosen = int(safe["so the threshold is"].split()[-1])
    tried = [ln.split() for ln in section("FOUR", "THE SWEEP")[1:]]
    top = max(int(t[0]) for t in tried) + 2
    x0, x1, y = 120, 1800, 540

    def px(v):
        return x0 + (x1 - x0) * v / top
    header(d, 60, 215, "FAILURES FOR ONE ACCOUNT IN 5 MINUTES", CYAN)
    d.rounded_rectangle([px(lo) - 10, y - 140, px(hi) + 10, y + 10], radius=10, fill=(20, 70, 60))
    label(d, (px(lo) + px(hi)) / 2, y - 130, "THE SAFE RANGE: %d to %d" % (lo, hi), GREEN, 28, True, "ma")
    d.line([x0, y, x1, y], fill=LINE, width=4)
    for v in range(0, top + 1, 5):
        d.line([px(v), y - 8, px(v), y + 8], fill=LINE, width=2)
        label(d, px(v), y + 14, str(v), MUTED, 24, False, "ma")
    for k, r in enumerate(normal):
        v = int(r[1])
        d.ellipse([px(v) - 14, y - 14, px(v) + 14, y + 14], fill=AMBER)
        label(d, px(v) - 14, y + 50 + k * 40, "%s: %d" % (r[2], v), AMBER, 26, True)
    av = int(attack[1])
    d.ellipse([px(av) - 16, y - 16, px(av) + 16, y + 16], fill=ROSE)
    label(d, px(av), y - 60, "attack B: %d" % av, ROSE, 26, True, "ma")
    d.line([px(chosen), y - 175, px(chosen), y + 20], fill=TEXT, width=4)
    label(d, px(chosen), y - 215, "chosen: %d" % chosen, TEXT, 28, True, "ma")
    header(d, 60, 680, "THE SWEEP (thresholds tried)", AMBER)
    for k, t in enumerate(tried):
        x = 60 + k * 365
        th, alerts, tp, fp, b = t
        good = fp == "0" and b == "noticed"
        colour = GREEN if good else (ROSE if b != "noticed" else AMBER)
        cell(d, x, 745, 340, 70, "threshold %s" % th, colour, 28)
        label(d, x + 170, 828, "%s false" % fp if b == "noticed" else "attack B MISSED", colour, 26, True, "ma")
        label(d, x + 170, 866, "%s alert%s" % (alerts, "" if alerts == "1" else "s"), MUTED, 24, False, "ma")
    caption(d, "Above the worst normal day, not above the attack: a measurement, not a guess.")


def tuned(img, d, cfg):
    """The tuned set's alerts on the day's clock, one row each, true in green and false in amber."""
    alerts = [columns(ln) for ln in section("FIVE", "THE ALERTS")]
    header(d, 60, 215, "THE ALERTS, IN TIME ORDER", GREEN)
    x0, x1 = 360, 1860
    top = 300
    x_of = clock(d, x0, x1, top + len(alerts) * 64 + 10)
    for k, a in enumerate(alerts):
        when, rule, subject, mark = a[0], a[1], a[2], a[3]
        y = top + k * 64
        false = "FALSE" in mark
        colour = AMBER if false else GREEN
        x = x_of(minutes(when))
        d.line([x0, y + 20, x1, y + 20], fill=(40, 52, 72), width=1)
        d.ellipse([x - 12, y + 8, x + 12, y + 32], fill=colour)
        label(d, 60, y + 4, when, TEXT, 28, True)
        label(d, 160, y + 6, mark.replace("<- ", ""), colour, 26, True)
        text = "%s  %s" % (rule, subject)
        f = font(SANS, 26)
        tx = x + 24 if x + 24 + f.getlength(text) < x1 else x - 24 - f.getlength(text)
        d.text((tx, y + 6), text, font=f, fill=colour)
    total = [columns(ln) for ln in section("FIVE", "THE RESULT") if ln.strip().startswith("together")][0]
    caption(d, "%s alerts, %s true and %s false: a day an analyst can read." % (total[1], total[2], total[3]))
