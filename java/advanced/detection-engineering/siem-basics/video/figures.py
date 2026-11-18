"""Pictures for the SIEM basics video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
SRC = {"web": CYAN, "database": AMBER, "db": AMBER, "host": GREEN}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The first section of an act whose header starts with `head`, its lines with two more spaces removed."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[0]


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def cols(ln):
    return re.split(r"\s{2,}", ln.strip())


def minutes(hms):
    h, m, s = [int(x) for x in hms.split(":")]
    return h * 60 + m + s / 60


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


# ------------------------------------------------------------------ figures

def one_shape(img, d, cfg):
    """One raw line of each source on the left, the one common event shape on the right."""
    lines = section("ONE", "THE LINES")
    blocks, cur = [], None
    for ln in lines:
        if not ln.startswith("  "):
            cur = [ln.strip(), []]
            blocks.append(cur)
        else:
            cur[1].append(ln.strip())
    header(d, 60, 180, "THE LINES (three formats)", CYAN)
    for k, (name, raw) in enumerate(blocks):
        colour = SRC[name.split()[0]]
        y = 245 + k * 215
        panel(d, 60, y, 960, 195, colour)
        label(d, 85, y + 14, name, colour, 26, True)
        for j, r in enumerate(raw):
            fit_mono(d, 85, y + 62 + j * 42, r, TEXT, 22, 910)
        draw.arrow_right(d, 1030, y + 98, 1090, MUTED, 4)
    rows = [cols(ln) for ln in section("ONE", "THE EVENTS")]
    header(d, 1110, 180, "THE EVENTS (one shape, UTC)", GREEN)
    xs = [1110, 1290, 1500, 1710]
    for j, h in enumerate(rows[0]):
        label(d, xs[j], 250, h, SRC.get(h, MUTED), 26, True)
    for k, r in enumerate(rows[1:]):
        y = 300 + k * 72
        d.line([1110, y - 10, 1880, y - 10], fill=LINE, width=1)
        label(d, xs[0], y + 18, r[0], MUTED, 26, True, "lm")
        for j, v in enumerate(r[1:], 1):
            fit_mono(d, xs[j], y + 18, v, TEXT, 24, 200, "lm")
    caption(d, "Same names, same clock: one query can now read all three sources.", AMBER, 935)


def day_counts(img, d, cfg):
    """Lines in, events out, and the lines that did not fit, per source."""
    sec = section("ONE", "THE WHOLE DAY")
    rows = [cols(ln) for ln in sec[1:4]]
    dead = sec[-1].strip()
    header(d, 60, 180, "THE WHOLE DAY: LINES IN, EVENTS OUT", CYAN)
    top = max(int(r[1]) for r in rows)
    unit = 1100 / top
    for k, (src, n_lines, n_events, n_dead) in enumerate(rows):
        y = 270 + k * 170
        colour = SRC[src]
        label(d, 60, y + 40, src, colour, 34, True, "lm")
        d.rectangle([330, y, 330 + int(n_lines) * unit, y + 36], fill=LINE)
        mono(d, 330 + int(n_lines) * unit + 15, y + 18, "%s lines" % n_lines, MUTED, 26, "lm")
        d.rectangle([330, y + 46, 330 + int(n_events) * unit, y + 82], fill=colour)
        end = 330 + int(n_lines) * unit + 15
        mono(d, end, y + 64, "%s events" % n_events, colour, 26, "lm")
        if int(n_dead):
            x = 330 + int(n_events) * unit
            d.rectangle([x, y + 46, 330 + int(n_lines) * unit, y + 82], fill=ROSE)
            mono(d, end + 220, y + 64, "+ %s dead letters" % n_dead, ROSE, 26, "lm")
    header(d, 60, 790, "A DEAD LETTER, KEPT AND COUNTED", ROSE)
    fit_mono(d, 60, 860, dead, TEXT, 28, 1800)
    caption(d, "A line that does not fit is kept and counted, never dropped in silence.", AMBER, 935)


def one_query(img, d, cfg):
    """A query on the web's own fields against one query on the common fields."""
    before = section("TWO", "BEFORE NORMALIZING")
    after = section("TWO", "AFTER NORMALIZING")
    per = [cols(ln) for ln in after[1:4]]
    n_before = int(re.search(r"found: (\d+)", before[1]).group(1))
    n_after = int(re.search(r"found: (\d+)", after[-1]).group(1))
    unit = 1300 / n_after
    for k, (tag, colour, q, parts, total, note) in enumerate([
            ("BEFORE NORMALIZING", ROSE, before[0].strip(), [("web", n_before)], n_before, before[1].strip()),
            ("AFTER NORMALIZING", GREEN, after[0].strip(), [(s, int(n)) for s, n in per], n_after, after[-1].strip())]):
        y = 190 + k * 360
        header(d, 60, y, tag, colour)
        panel(d, 60, y + 65, 1000, 70, colour)
        mono(d, 85, y + 100, q, TEXT, 30, "lm")
        x = 60
        for src, n in parts:
            if n == 0:
                continue
            d.rectangle([x, y + 170, x + n * unit, y + 240], fill=SRC[src])
            label(d, x + n * unit / 2, y + 205, "%s %d" % (src, n), INK, 28, True, "mm")
            x += n * unit
        fit_mono(d, 60, y + 270, note, colour, 30, 1600)
    caption(d, "The same failures were always there: only the names stood in the way.", AMBER, 935)


def clock_timeline(img, d, cfg):
    """The database time honoured or ignored, against the web login and the rule's window."""
    how = section("THREE", "HOW THE TIME")
    rule = section("THREE", "THE RULE, BOTH WAYS")
    stamp = re.search(r"(\d\d:\d\d:\d\d) - (\d\d:\d\d) = (\d\d:\d\d:\d\d)", how[0])
    local, offset, utc = stamp.groups()
    wrong = re.search(r"= (\d\d:\d\d:\d\d)", how[1]).group(1)
    login = re.search(r"login (\d\d:\d\d:\d\d)", rule[0]).group(1)
    window = int(re.search(r"within (\d+) min", section("THREE", "THE DATABASE LINE")[1]).group(1))
    header(d, 60, 180, "HOW THE TIME IS MADE", AMBER)
    mono(d, 60, 260, "%s  -  %s  =  %s UTC   (offset honoured)" % (local, offset, utc), GREEN, 32)
    mono(d, 60, 315, "%s  read as UTC  =  %s UTC   (offset ignored)" % (local, wrong), ROSE, 32)
    lo, hi = 9 * 60, 15 * 60 + 30
    x0, x1, y = 120, 1800, 560

    def px(m):
        return x0 + (m - lo) * (x1 - x0) / (hi - lo)
    d.line([x0, y, x1, y], fill=LINE, width=4)
    for h in range(9, 16):
        d.line([px(h * 60), y - 10, px(h * 60), y + 10], fill=MUTED, width=2)
        label(d, px(h * 60), y + 20, "%02d:00" % h, MUTED, 24, False, "ma")
    lm = minutes(login)
    d.rectangle([px(lm), y - 60, px(lm + window), y - 4], fill=(40, 70, 60), outline=GREEN, width=2)
    label(d, px(lm + window) + 10, y - 50, "the %d min window" % window, GREEN, 24, True)
    for t, colour, text, dy in [(login, CYAN, "web login " + login, -150), (utc, GREEN, "read " + utc + " (honoured)", 80),
                                (wrong, ROSE, "read " + wrong + " (ignored)", 80)]:
        x = px(minutes(t))
        d.ellipse([x - 12, y - 12, x + 12, y + 12], fill=colour)
        d.line([x, y, x, y + dy + (30 if dy < 0 else 0)], fill=colour, width=3)
        label(d, x + (10 if t != wrong else -10), y + dy + (40 if t == utc else 0), text, colour, 26, True, "la" if t != wrong else "ra")
    fit_mono(d, 60, 800, rule[0].strip(), GREEN, 28, 1800)
    fit_mono(d, 60, 850, rule[1].strip(), ROSE, 28, 1800)
    caption(d, "One ignored offset moves the read hours away, and the rule goes quiet.", AMBER, 935)


def joined_timeline(img, d, cfg):
    """alice's failed logins from two sources inside the ten-minute window, counted apart and together."""
    ev = section("FOUR", "THE EVENTS FOR")
    how = section("FOUR", "HOW THE RULE")
    res = section("FOUR", "THE RESULT")
    rows = []
    for ln in ev:
        m = re.match(r"\s*(\w+)\s+(\d+) failed logins, (\S+) to (\S+)", ln)
        if m:
            rows.append(m.groups())
    success = re.search(r"at (\S+)", ev[-1]).group(1)
    win_from, win_to = re.search(r"window: (\S+) to (\S+)", how[0]).groups()
    header(d, 60, 180, heading_of("FOUR", "THE EVENTS FOR"), CYAN)
    lo, hi = minutes(win_from) - 1, minutes(win_to) + 1
    x0, x1 = 300, 1780

    def px(m):
        return x0 + (m - lo) * (x1 - x0) / (hi - lo)
    d.rectangle([px(minutes(win_from)), 250, px(minutes(win_to)), 640], outline=AMBER, width=3)
    label(d, px(minutes(win_from)) + 10, 255, "the window %s to %s" % (win_from, win_to), AMBER, 24, True)
    for k, (src, n, a, b) in enumerate(rows):
        y = 340 + k * 120
        colour = SRC[src]
        label(d, 60, y, "%s  %s" % (src, n), colour, 34, True, "lm")
        n, ta, tb = int(n), minutes(a), minutes(b)
        for i in range(n):
            x = px(ta + (tb - ta) * i / max(n - 1, 1))
            d.ellipse([x - 13, y - 13, x + 13, y + 13], fill=ROSE, outline=colour, width=3)
    xs = px(minutes(success))
    d.ellipse([xs - 18, 340 - 18, xs + 18, 340 + 18], fill=GREEN)
    label(d, xs - 20, 300, "success " + success, GREEN, 26, True, "ra")
    for k, ln in enumerate(how[1:]):
        bad = "no alert" in ln
        fit_mono(d, 60, 580 + k * 50 + 90, ln.strip(), MUTED if bad else GREEN, 30, 1800)
    fit_mono(d, 60, 870, res[1].strip(), GREEN, 30, 1800)
    caption(d, "Each source alone stays under ten; joined, the burst is plain.", AMBER, 935)


def alerts_to_case(img, d, cfg):
    """The two alerts, grouped by user into one case."""
    alerts = [cols(ln) for ln in section("FIVE", "THE ALERTS")]
    case = section("FIVE", "THE CASES")[0].strip()
    header(d, 60, 180, "THE ALERTS", ROSE)
    for k, (rule, user, n) in enumerate(alerts):
        y = 260 + k * 230
        panel(d, 60, y, 900, 190, ROSE)
        label(d, 90, y + 25, rule, TEXT, 32, True)
        label(d, 90, y + 90, "user " + user, CYAN, 30, True)
        label(d, 90, y + 135, n, MUTED, 28)
        draw.arrow_right(d, 980, y + 95, 1180, MUTED, 5)
    header(d, 1220, 180, "THE CASE", GREEN)
    panel(d, 1220, 300, 640, 360, GREEN, 4)
    user, rest = case.split(": ", 1)
    label(d, 1540, 380, user, GREEN, 64, True, "ma")
    for k, part in enumerate(rest.split(", ")):
        label(d, 1540, 490 + k * 50, part, TEXT, 30, True, "ma")
    caption(d, "An analyst reads one story about alice, not two unrelated alerts.", AMBER, 935)
