"""Pictures for the detecting attacks with OpenSearch video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import ACCENT, AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

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

def one_query(img, d, cfg):
    """The core idea: brute force as a query, its four parts left to right, and the buckets it returns."""
    parts = [columns(ln) for ln in section("TWO", "THE QUERY")]
    header(d, 60, 215, "THE QUERY (brute-force(3), read back from what is sent)", CYAN)
    colours = [CYAN, ACCENT, AMBER, GREEN]
    bw, gap = 420, 40
    for k, (name, value) in enumerate(parts):
        x = 60 + k * (bw + gap)
        panel(d, x, 290, bw, 170, colours[k], 3)
        label(d, x + 20, 305, name.upper(), colours[k], 28, True)
        what, _, how = value.partition(" (")
        for i, piece in enumerate(what.replace(" and ", " and\n").split("\n")):
            fit_mono(d, x + 20, 352 + i * 38, piece, TEXT, 28, bw - 40)
        if how:
            label(d, x + 20, 418, how.rstrip(")"), MUTED, 24)
        if k:
            draw.arrow_right(d, x - gap + 4, 375, x - 4, MUTED, 4)
    label(d, 60, 480, "failed logins  ->  per account  ->  per 5 minutes of the clock  ->  only the full buckets", MUTED, 26)
    header(d, 60, 550, "THE BUCKETS IT RETURNS", GREEN)
    rows = [columns(ln) for ln in section("TWO", "THE BUCKETS IT RETURNS")]
    biggest = max(int(r[2]) for r in rows)
    for k, r in enumerate(rows):
        y = 620 + k * 90
        false = "FALSE" in r[3]
        colour = AMBER if false else ROSE
        label(d, 60, y + 8, "%s  %s" % (r[0], r[1]), TEXT, 30, True)
        w = 1100 * int(r[2]) / biggest
        d.rounded_rectangle([360, y, 360 + w, y + 54], radius=8, fill=colour)
        label(d, 380 + w, y + 10, "%s failures, %s" % (r[2], "false" if false else r[3]), colour, 28, True)
    caption(d, "Every rule here is a filter, a group, a bucket and a threshold.")


def the_index(img, d, cfg):
    """The input events, the mapping read back, and one event as a document."""
    events = columns(section("ONE", "THE INPUT")[0])[1]
    header(d, 60, 215, "THE INPUT", CYAN)
    panel(d, 60, 280, 420, 200, CYAN, 3)
    label(d, 270, 300, events, TEXT, 72, True, "ma")
    label(d, 270, 400, "events, one bulk request", MUTED, 26, False, "ma")
    draw.arrow_right(d, 490, 380, 560, MUTED, 4)
    header(d, 570, 215, "THE MAPPING (read back)", CYAN)
    y = 280
    for ln in section("ONE", "THE MAPPING"):
        typ, fields = columns(ln)
        label(d, 570, y, typ, AMBER, 28, True)
        names = fields.split(", ")
        for i in range(0, len(names), 3):
            fit_mono(d, 730, y + (i // 3) * 40, ", ".join(names[i:i + 3]), TEXT, 26, 520)
        y += 40 * ((len(names) + 2) // 3) + 18
    draw.arrow_right(d, 1265, 380, 1325, MUTED, 4)
    header(d, 1335, 215, "ONE EVENT AS A DOCUMENT", CYAN)
    doc = [columns(ln) for ln in section("ONE", "ONE EVENT")]
    panel(d, 1335, 280, 525, 46 * len(doc) + 30, LINE, 2)
    for k, row in enumerate(doc):
        key, value = row[0].split(None, 1) if len(row) == 1 else row[:2]
        y = 300 + k * 46
        scoring = key == "attack"
        label(d, 1360, y, key, AMBER if scoring else MUTED, 26, True)
        fit_mono(d, 1490, y, value.split("  <-")[0], AMBER if scoring else TEXT, 26, 350)
    label(d, 1335, 300 + 46 * len(doc) + 40, "attack: for scoring only", AMBER, 26, True)
    header(d, 60, 720, "THE RESULT (counted by OpenSearch)", GREEN)
    res = [columns(ln) for ln in section("ONE", "THE RESULT")]
    for k, (key, value) in enumerate(res):
        x = 60 + k * 600
        cell(d, x, 790, 560, 90, "%s: %s" % (key, value.split(",")[0]), GREEN, 32)
    caption(d, "Keywords for what a rule groups by, a date for time, and the truth kept apart.")


def first_queries(img, d, cfg):
    """The first queries' alerts as bars, true in green and false in amber."""
    rows = [columns(ln) for ln in section("TWO", "THE RESULT")[1:]]
    total = [r for r in rows if r[0] == "together"][0]
    rows = [r for r in rows if r[0] != "together"]
    header(d, 60, 215, "THE RESULT (alerts per query)", GREEN)
    biggest = max(int(r[1]) for r in rows)
    x0, unit = 560, 1000 / biggest
    for k, r in enumerate(rows):
        alerts, tp, fp = int(r[1]), int(r[2]), int(r[3])
        y = 300 + k * 100
        label(d, 60, y + 10, r[0], TEXT, 30, True)
        if tp:
            d.rounded_rectangle([x0, y, x0 + tp * unit, y + 56], radius=6, fill=GREEN)
        if fp:
            d.rounded_rectangle([x0 + tp * unit, y, x0 + alerts * unit, y + 56], radius=6, fill=AMBER)
        label(d, x0 + alerts * unit + 14, y + 12, "%d true, %d false" % (tp, fp), MUTED, 28)
    header(d, 60, 720, "WHY success-after-failures(1) IS NOISY", ROSE)
    why = [columns(ln) for ln in section("TWO", "WHY")]
    parts = [r for r in why if len(r) == 2 and r[0] in ("filter", "group by", "bucket", "threshold")]
    for k, (name, value) in enumerate(parts):
        x = 60 + k * 460
        label(d, x, 790, name, MUTED, 24, True)
        fit_mono(d, x, 825, value, TEXT, 24, 440)
    label(d, 60, 880, "a typo and a success from one address in the same hour is enough: no \"then\"", ROSE, 26, True)
    caption(d, "%s alerts, %s true and %s false, and no query for impossible travel." % (total[1], total[2], total[3]))


def sweep(img, d, cfg):
    """A ruler of failures per bucket: the largest normal bucket, the attack's, the safe range, and the thresholds tried."""
    b = {r[0]: r[1] for r in (columns(ln) for ln in section("THREE", "THE BUCKETS"))}
    normal = int(b["largest normal"].split()[0])
    attack = int(b["largest attack"].split()[0])
    safe = section("THREE", "THE SAFE RANGE")[0]
    lo, hi = [int(x) for x in re.search(r"min_doc_count (\d+) to (\d+)", safe).groups()]
    chosen = int(safe.split()[-1])
    tried = [ln.split() for ln in section("THREE", "THE SWEEP")[1:]]
    top = max(int(t[0]) for t in tried) + 2
    x0, x1, y = 120, 1800, 540

    def px(v):
        return x0 + (x1 - x0) * v / top
    header(d, 60, 215, "FAILURES IN ONE ACCOUNT'S 5-MINUTE BUCKET (%s buckets)" % b["buckets"], CYAN)
    d.rounded_rectangle([px(lo) - 10, y - 140, px(hi) + 10, y + 10], radius=10, fill=(20, 70, 60))
    label(d, (px(lo) + px(hi)) / 2, y - 130, "THE SAFE RANGE: %d to %d" % (lo, hi), GREEN, 28, True, "ma")
    d.line([x0, y, x1, y], fill=LINE, width=4)
    for v in range(0, top + 1, 5):
        d.line([px(v), y - 8, px(v), y + 8], fill=LINE, width=2)
        label(d, px(v), y + 14, str(v), MUTED, 24, False, "ma")
    d.ellipse([px(normal) - 14, y - 14, px(normal) + 14, y + 14], fill=AMBER)
    label(d, px(normal) - 14, y + 50, "largest normal: %d (%s)" % (normal, b["largest normal"].split("e.g. ")[1].rstrip(")")), AMBER, 26, True)
    d.ellipse([px(attack) - 16, y - 16, px(attack) + 16, y + 16], fill=ROSE)
    label(d, px(attack), y - 60, "largest attack: %d" % attack, ROSE, 26, True, "ma")
    d.line([px(chosen), y - 175, px(chosen), y + 20], fill=TEXT, width=4)
    label(d, px(chosen), y - 215, "chosen: %d" % chosen, TEXT, 28, True, "ma")
    header(d, 60, 680, "THE SWEEP (min_doc_count tried)", AMBER)
    for k, t in enumerate(tried):
        x = 60 + k * 365
        th, alerts, tp, fp, res = t
        good = fp == "0" and res == "noticed"
        colour = GREEN if good else (ROSE if res != "noticed" else AMBER)
        cell(d, x, 745, 340, 70, "min_doc_count %s" % th, colour, 26)
        label(d, x + 170, 828, "%s false" % fp if res == "noticed" else "attack B MISSED", colour, 26, True, "ma")
        label(d, x + 170, 866, "%s alert%s" % (alerts, "" if alerts == "1" else "s"), MUTED, 24, False, "ma")
    caption(d, "Tuning is one number in the query, measured on the normal day.")


def monitors(img, d, cfg):
    """One monitor as saved, then every alert of the tuned set on the day's clock."""
    m = {r[0]: r[1] for r in (columns(ln) for ln in section("FOUR", "THE MONITOR"))}
    header(d, 60, 215, "THE MONITOR", CYAN)
    for k, key in enumerate(("schedule", "input", "trigger")):
        x = 60 + k * 610
        panel(d, x, 280, 580, 116, CYAN, 2)
        label(d, x + 20, 290, key, MUTED, 24, True)
        for i, piece in enumerate(m[key].split(", ")):
            fit_mono(d, x + 20, 322 + i * 30, piece, TEXT, 24, 540)
    alerts = [columns(ln) for ln in section("FOUR", "THE ALERTS")]
    x0, x1, top = 420, 1860, 418
    x_of = clock(d, x0, x1, top + len(alerts) * 46 + 12)
    for k, a in enumerate(alerts):
        when, rule, subject, mark = a
        y = top + k * 46
        false = "FALSE" in mark
        colour = AMBER if false else GREEN
        x = x_of(minutes(when))
        d.ellipse([x - 11, y + 6, x + 11, y + 28], fill=colour)
        label(d, 60, y + 2, when, TEXT, 26, True)
        label(d, 150, y + 4, mark.replace("<- ", ""), colour, 24, True)
        text = "%s  %s%s" % (rule, subject, "  (code)" if rule == "impossible-travel" else "")
        f = font(SANS, 24)
        tx = x + 20 if x + 20 + f.getlength(text) < x1 else x - 20 - f.getlength(text)
        d.text((tx, y + 4), text, font=f, fill=colour)
    total = [columns(ln) for ln in section("FOUR", "THE RUN") if ln.strip().startswith("together")][0]
    caption(d, "%s alerts, %s true and %s false: monitors plus a few lines of code." % (total[1], total[2], total[3]))


def straddle(img, d, cfg):
    """A mapping that refuses a group, then thirty failures split by a bucket edge, and a sliding window that sees them."""
    bad = {r[0]: r[1] for r in (columns(ln) for ln in section("FIVE", "THE INPUT (50")) if len(r) == 2}
    good = columns(section("FIVE", "THE SAME")[0])[1]
    header(d, 60, 215, "NO MAPPING: GROUP BY user", ROSE)
    fit_mono(d, 60, 280, "status %s: %s" % (bad["status"], bad["reason"]), ROSE, 26, 860)
    header(d, 1000, 215, "BY user.keyword", GREEN)
    fit_mono(d, 1000, 280, "status " + good.split("  <-")[0], GREEN, 26, 860)
    span = section("FIVE", "THE INPUT (30")[0].split()
    start, end = span[1], span[3].rstrip(",")
    buckets = [re.match(r"\s*bucket (\S+)-(\S+)\s+(\d+)", ln).groups() for ln in section("FIVE", "FIXED BUCKETS") if "bucket " in ln]
    sliding = section("FIVE", "A SLIDING WINDOW")[0]
    fire = re.search(r"at (\d\d:\d\d:\d\d)", sliding).group(1)

    def secs(t):
        p = [int(v) for v in (t + ":00")[:8].split(":")]
        return p[0] * 3600 + p[1] * 60 + p[2]
    t0, t1 = secs(buckets[0][0]), secs(buckets[-1][1])
    x0, x1 = 120, 1800

    def px(t):
        return x0 + (x1 - x0) * (t - t0) / (t1 - t0)
    header(d, 60, 360, "THE INPUT: 30 FAILURES, %s TO %s" % (start, end), CYAN)
    y = 470
    s0, s1 = secs(start), secs(end)
    n = 30
    for i in range(n):
        t = s0 + i * (s1 - s0) / (n - 1)
        d.rounded_rectangle([px(t) - 7, y, px(t) + 7, y + 60], radius=4, fill=ROSE)
    for k, (a, b2, count) in enumerate(buckets):
        xa, xb = px(secs(a)), px(secs(b2))
        d.rounded_rectangle([xa + 4, y + 90, xb - 4, y + 160], radius=10, outline=AMBER, width=3, fill=BG_CELL)
        label(d, (xa + xb) / 2, y + 108, "bucket %s-%s: %s failures, under 20" % (a, b2, count), AMBER, 26, True, "ma")
    edge = px(secs(buckets[1][0]))
    d.line([edge, y - 30, edge, y + 170], fill=TEXT, width=4)
    label(d, edge, y - 66, "edge " + buckets[1][0], TEXT, 26, True, "ma")
    label(d, 60, 670, "fixed buckets: 0 alerts, the attack falls between two buckets", ROSE, 28, True)
    fx = px(secs(fire))
    d.rounded_rectangle([px(secs(fire) - 300), 730, fx, 790], radius=10, outline=GREEN, width=3)
    label(d, fx + 14, 742, "sliding 5-minute window: 20 failures at %s, 1 alert" % fire, GREEN, 26, True)
    label(d, 60, 830, "and a monitor on a 1-minute schedule notices up to a minute and a bucket late", MUTED, 26)
    caption(d, "A mapping decides what a rule can say; bucket edges decide what it can see.")
