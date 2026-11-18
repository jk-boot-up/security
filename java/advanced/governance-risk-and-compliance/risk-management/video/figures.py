"""Pictures for the risk management video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


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


def section(name, head):
    return sections(name, head)[0]


def money(s):
    """'$500k' -> 500000.0, '$3.0M' -> 3000000.0."""
    m = re.fullmatch(r"\+?\$(-?[\d.]+)([kM])", s.strip())
    return float(m.group(1)) * (1e3 if m.group(2) == "k" else 1e6)


def after_eq(line):
    return line.rsplit("=", 1)[1].strip()


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


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def dashed(d, x, y0, y1, colour, width=3, dash=14):
    y = y0
    while y < y1:
        d.line([x, y, x, min(y + dash, y1)], fill=colour, width=width)
        y += dash * 2


def triangle(d, x0, x1, base, top, lo, mode, hi, colour, upto=None, fill=None):
    """A triangle range drawn from lo to hi on x0..x1, its peak at mode; shaded up to `upto` when given."""
    def px(v):
        return x0 + (v - lo) * (x1 - x0) / (hi - lo)

    def height(v):
        if v <= mode:
            return 0 if mode == lo else (v - lo) / (mode - lo)
        return (hi - v) / (hi - mode)

    if upto is not None:
        pts = [(px(lo), base)]
        if upto > mode:
            pts.append((px(mode), top))
        pts += [(px(upto), base - (base - top) * height(upto)), (px(upto), base)]
        d.polygon(pts, fill=fill)
    d.line([(px(lo), base), (px(mode), top), (px(hi), base)], fill=colour, width=4)
    d.line([(px(lo), base), (px(hi), base)], fill=LINE, width=3)
    return px


# ------------------------------------------------------------------ figures

def input_ranges():
    rows = {k.strip(): v.split() for k, v in (re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in section("ONE", "THE INPUT"))}
    return rows


def ranges_to_money(img, d, cfg):
    """The core idea on ransomware: two ranges, their averages, and the expected loss a year."""
    rows = input_ranges()
    avg = section("ONE", "THE AVERAGE")
    f_mean, l_mean = after_eq(avg[0]), after_eq(avg[1])
    expected = avg[2].split("=", 1)[1].strip()
    risk = act_title("ONE").split(": ")[-1].rstrip(".")
    freq = [float(x) for x in rows["events a year"]]
    loss = rows["loss of one"]
    lossv = [money(x) for x in loss]
    panels = [("HOW OFTEN: EVENTS IN A YEAR", freq, [str(x).rstrip("0").rstrip(".") for x in rows["events a year"]], float(f_mean), f_mean, CYAN),
              ("HOW MUCH: THE LOSS OF ONE EVENT", lossv, loss, money(l_mean), l_mean, AMBER)]
    for i, (title, (lo, mode, hi), texts, mean, mean_text, colour) in enumerate(panels):
        x = 60 + i * 920
        panel(d, x, 180, 880, 520)
        header(d, x + 30, 200, title, colour)
        px = triangle(d, x + 80, x + 800, 600, 300, lo, mode, hi, colour)
        for v, t, name in ((lo, texts[0], "least"), (hi, texts[2], "most")):
            mono(d, px(v), 615, t, TEXT, 28, "ma")
            label(d, px(v), 652, name, MUTED, 22, False, "ma")
        mono(d, px(mode), 255, texts[1], TEXT, 28, "ma")
        label(d, px(mode) + 60, 262, "most likely", MUTED, 22, False, "la")
        dashed(d, px(mean), 330, 600, GREEN)
        label(d, px(mean) + 12, 545, "average " + mean_text, GREEN, 28, True, "la")
    header(d, 60, 730, "THE AVERAGE OF A RANGE = (least + most likely + most) / 3", MUTED)
    fit_mono(d, 60, 800, "%s events x %s = %s  expected loss" % (f_mean, l_mean, expected), GREEN, 40, 1800)
    hm = rows["heat map"]
    label(d, 60, 870, "%s on the heat map: %s" % (risk, " ".join(hm)), MUTED, 30)
    caption(d, "A range says how often and how much; the average turns it into money.", AMBER, 940)


def act_title(name):
    return re.search(r"^%s\. (.*)$" % name, OUT, re.M).group(1)


def one_year(img, d, cfg):
    """One simulated year of ransomware: three draws read on the ranges, then 50,000 such years."""
    rows = input_ranges()
    lines = section("ONE", "ONE SIMULATED YEAR")
    year = re.search(r"\(year (\d+)\)", heading_of("ONE", "ONE SIMULATED")).group(1)
    u1, s1 = re.findall(r"[\d.]+(?=\D)", lines[0].split("draw ")[1])[:2]
    rate = float(after_eq(lines[1]).split()[0])
    u2 = float(lines[2].split("draw ")[1])
    p0, p1 = [float(x) for x in re.findall(r"= ([\d.]+)", lines[3])[-2:]]
    events = re.search(r"so (\d+) event", lines[4]).group(1)
    u3 = float(lines[5].split("draw ")[1].split()[0])
    l3 = after_eq(lines[6])
    total = heading_of("ONE", "THE YEAR'S LOSS").split(": ")[1]
    freq = [float(x) for x in rows["events a year"]]
    lossv = [money(x) for x in rows["loss of one"]]

    header(d, 60, 170, "ONE SIMULATED YEAR (year %s): THREE DRAWS BETWEEN 0 AND 1" % year, CYAN)
    # 1. how often
    panel(d, 60, 235, 580, 520)
    label(d, 90, 255, "1. HOW OFTEN", CYAN, 28, True)
    px = triangle(d, 100, 600, 560, 340, *freq, CYAN, upto=rate, fill=(30, 80, 100))
    label(d, 90, 300, "draw %s = the shaded share" % u1, TEXT, 24)
    mono(d, px(rate), 575, "%s" % rate, CYAN, 30, "ma")
    label(d, 350, 640, "events a year this year", MUTED, 24, False, "ma")
    # 2. how many
    panel(d, 670, 235, 580, 520)
    label(d, 700, 255, "2. HOW MANY", AMBER, 28, True)
    y0, y1 = 320, 700
    bx = 760

    def py(p):
        return y0 + (y1 - y0) * p
    for a, b, text, c in ((0, p0, "0 events", LINE), (p0, p1, "1 event", AMBER), (p1, 1, "2 or more", LINE)):
        d.rectangle([bx, py(a), bx + 90, py(b)], fill=c, outline=PANEL, width=2)
        label(d, bx + 110, (py(a) + py(b)) / 2 + (14 if c == AMBER else 0), text, TEXT if c == AMBER else MUTED, 24, c == AMBER, "lm")
    for p in (p0, p1):
        mono(d, bx - 10, py(p), "%.3f" % p, MUTED, 20, "rm")
    d.line([bx - 10, py(u2), bx + 100, py(u2)], fill=TEXT, width=4)
    label(d, 1010, py(u2), "draw %.3f" % u2, TEXT, 26, True, "lm")
    d.line([bx + 100, py(u2), 1000, py(u2)], fill=TEXT, width=2)
    label(d, 960, 300, "chance of 0, 1, 2+", MUTED, 22, False, "ma")
    # 3. how much
    panel(d, 1280, 235, 580, 520)
    label(d, 1310, 255, "3. HOW MUCH", GREEN, 28, True)
    px = triangle(d, 1320, 1820, 560, 340, *lossv, GREEN, upto=money(l3), fill=(25, 90, 70))
    label(d, 1310, 300, "draw %s = the shaded share" % u3, TEXT, 24)
    mono(d, px(money(l3)), 575, l3, GREEN, 30, "ma")
    label(d, 1570, 640, "the loss of that event", MUTED, 24, False, "ma")
    # the result
    res = dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in section("ONE", "THE RESULT"))
    fit_mono(d, 60, 790, "%s event, the year's loss: %s" % (events, total), GREEN, 36, 1800)
    keys = list(res)
    fit_mono(d, 60, 855, "%s years: %s %s, %s %s, 1 in 10 over %s" % (
        re.search(r"OF (\d+)", heading_of("ONE", "THE RESULT")).group(1), keys[0], res[keys[0]],
        "average", res[keys[1]], res[keys[2]]), TEXT, 28, 1800)
    caption(d, "Most years have no event; a few have a big one. The average hides both.", AMBER, 940)


def heat_vs_money(img, d, cfg):
    """The eight risks ranked by heat map score and by expected loss, joined by a line."""
    rows = [ln.split() for ln in section("TWO", "THE RESULT")[1:]]
    data = [(r[0], int(r[1]), r[2]) for r in rows]
    by_score = sorted(data, key=lambda r: (-r[1], r[0]))
    by_loss = sorted(data, key=lambda r: -money(r[2]))
    top_s = {r[0] for r in by_score[:3]}
    top_l = {r[0] for r in by_loss[:3]}
    header(d, 60, 170, "BY HEAT MAP SCORE", AMBER)
    header(d, 1160, 170, "BY EXPECTED LOSS A YEAR", GREEN)
    ys = {}
    biggest = money(by_loss[0][2])
    for i, (rid, score, loss) in enumerate(by_score):
        y = 240 + i * 82
        c = GREEN if rid in top_s and rid in top_l else AMBER if rid in top_s else LINE
        cell(d, 60, y, 520, 64, "%-14s %2d" % (rid, score), c, 28, text_colour=TEXT if c != LINE else MUTED)
        ys.setdefault(rid, [0, 0])[0] = y + 32
    for i, (rid, score, loss) in enumerate(by_loss):
        y = 240 + i * 82
        c = GREEN if rid in top_s and rid in top_l else ROSE if rid in top_l else LINE
        cell(d, 1160, y, 330, 64, rid, c, 28, text_colour=TEXT if c != LINE else MUTED)
        d.rectangle([1510, y + 12, 1510 + 330 * money(loss) / biggest, y + 52], fill=c if c != LINE else MUTED)
        if money(loss) / biggest < 0.75:
            mono(d, 1520 + 330 * money(loss) / biggest, y + 32, loss, TEXT, 24, "lm")
        else:
            mono(d, 1500 + 330 * money(loss) / biggest, y + 32, loss, INK, 24, "rm")
        ys[rid][1] = y + 32
    for rid, (a, b) in ys.items():
        c = GREEN if rid in top_s and rid in top_l else AMBER if rid in top_s else ROSE if rid in top_l else LINE
        d.line([(590, a), (1150, b)], fill=c, width=4 if c != LINE else 2)
    both = section("TWO", "TWO RANKINGS")[2].split(None, 3)[-1].strip()
    caption(d, "The top three share only %s: the colours rank by feeling, the money ranks by loss." % both, AMBER, 920)


def bad_years(img, d, cfg):
    """How the register's loss spreads over 50,000 years, with the mean and the tail marked."""
    spread = [re.match(r"\s*(.+?)\s{2,}([\d.]+)%", ln).groups() for ln in section("THREE", "HOW THE YEARS")]
    res = {k.strip(): v.strip() for k, v in (re.match(r"\s*(.*?)\s+(\$[\d.]+[kM]|[\d.]+%)$", ln).groups() for ln in section("THREE", "THE RESULT"))}
    mean = money(res["expected loss a year"])
    p90 = money(res["1 year in 10 loses more than"])
    p95 = money(res["1 year in 20 loses more than"])
    over_key = next(k for k in res if k.startswith("years losing more than"))
    limit = money(over_key.split()[-1])
    over = res[over_key]
    x0, x1, base, top = 140, 1820, 800, 260
    n = len(spread)
    bw = (x1 - x0) / n
    biggest = max(float(p) for _, p in spread)
    step = money("$" + re.search(r"to \$(\d+)M", spread[0][0]).group(1) + "M")

    def px(v):
        return x0 + v / step * bw
    header(d, 60, 170, "THE REGISTER'S LOSS IN A YEAR: SHARE OF THE YEARS", CYAN)
    for i, (lab, p) in enumerate(spread):
        h = (base - top) * float(p) / biggest
        lo = i * step
        d.rectangle([x0 + i * bw + 6, base - h, x0 + (i + 1) * bw - 6, base], fill=ROSE if lo >= limit else LINE)
        mono(d, x0 + (i + 0.5) * bw, base - h - 34, p + "%", TEXT, 22, "ma")
        if i < n - 1:
            mono(d, x0 + i * bw, base + 12, "$%dM" % round(lo / 1e6), MUTED, 22, "ma")
    mono(d, x0 + (n - 1) * bw, base + 12, "$%dM" % round((n - 1) * step / 1e6), MUTED, 22, "ma")
    mono(d, x0 + (n - 0.5) * bw, base + 44, "or more", MUTED, 20, "ma")
    # the bin that holds the limit: the share right of the limit inside it is coloured too
    i = int(limit // step)
    h = (base - top) * float(spread[i][1]) / biggest
    d.rectangle([px(limit), base - h, x0 + (i + 1) * bw - 6, base], fill=ROSE)
    d.line([x0, base, x1, base], fill=LINE, width=3)
    for v, text, c, y in ((limit, "%s: %s of years over it" % (over_key.split()[-1], over), ROSE, 220),
                          (mean, "average " + res["expected loss a year"], GREEN, 300),
                          (p90, "1 in 10 over " + res["1 year in 10 loses more than"], AMBER, 380),
                          (p95, "1 in 20 over " + res["1 year in 20 loses more than"], AMBER, 450)):
        k = min(int(v // step), n - 1)
        dashed(d, px(v), y + 34, base - (base - top) * float(spread[k][1]) / biggest - 44, c)
        label(d, px(v) + 10, y, text, c, 26, True, "la")
    caption(d, "The average is not a bad year: the tail on the right is what hurts.", AMBER, 900)


def two_sets(img, d, cfg):
    """Each control alone, and the best set against cheapest first, as bars that add up to the loss before."""
    alone = [re.match(r"\s*(\S+)\s+(\$[\d.]+[kM])\s+-\s+(\$[\d.]+[kM])\s+=\s+(-?\$-?[\d.]+[kM])", ln).groups()
             for ln in section("FOUR", "EACH CONTROL ALONE")]
    best = dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in section("FOUR", "THE BEST SET"))
    cheap = dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in section("FOUR", "CHEAPEST FIRST"))
    gain = re.search(r"(\$[\d.]+[kM])", section("FOUR", "THE DIFFERENCE")[0]).group(1)
    in_best = best["controls"].split(", ")
    in_cheap = cheap["controls"].split(", ")
    header(d, 60, 170, "EACH CONTROL ALONE: LOSS AVOIDED AND COST", CYAN)
    top = max(money(a[1]) for a in alone)
    for i, (cid, avoided, cost, net) in enumerate(alone):
        y = 240 + i * 92
        label(d, 60, y + 30, cid, TEXT, 28, True, "lm")
        d.rectangle([250, y + 4, 250 + 520 * money(avoided) / top, y + 30], fill=GREEN)
        d.rectangle([250, y + 34, 250 + 520 * money(cost) / top, y + 60], fill=ROSE)
        mono(d, 260 + 520 * money(avoided) / top, y + 17, avoided, GREEN, 20, "lm")
        mono(d, 260 + 520 * money(cost) / top, y + 47, cost, ROSE, 20, "lm")
        for k, (members, text, c) in enumerate(((in_best, "best", GREEN), (in_cheap, "cheap", AMBER))):
            if cid in members:
                cell(d, 900 + k * 115, y + 6, 105, 50, text, c, 22)
    label(d, 60, 890, "green: loss avoided a year   rose: cost a year", MUTED, 24)
    # the two sets as stacked bars: what is left + what it costs + what is gained = the loss before
    header(d, 1180, 170, "TWO SETS WITHIN THE BUDGET", AMBER)
    before = money(best["expected loss"].split(" -> ")[0])
    bars = [("best set", best, GREEN), ("cheapest first", cheap, AMBER)]
    y_base, y_top = 860, 300
    for k, (name, c, colour) in enumerate(bars):
        x = 1240 + k * 320
        left = money(c["expected loss"].split(" -> ")[1])
        cost = money(c["cost"])
        net = money(c["net benefit"].split("= ")[-1])
        y = y_base
        for value, text, fill in ((left, "left " + c["expected loss"].split(" -> ")[1], ROSE),
                                  (cost, "cost " + c["cost"], AMBER), (net, "gain " + c["net benefit"].split("= ")[-1], GREEN)):
            h = (y_base - y_top) * value / before
            d.rectangle([x, y - h, x + 200, y], fill=fill, outline=PANEL, width=2)
            mono(d, x + 100, y - h / 2, text, INK, 22, "mm")
            y -= h
        label(d, x + 100, y_base + 14, name, colour, 26, True, "ma")
    label(d, 1560, 250, "each bar adds up to %s" % best["expected loss"].split(" -> ")[0], MUTED, 24, False, "ma")
    caption(d, "Trying every set beats buying the cheapest first by %s a year." % gain, AMBER, 940)


def appetite(img, d, cfg):
    """The share of years over the limit before, after the best set and with every control, against the appetite."""
    want = section("FIVE", "THE APPETITE")[0].strip()
    chance = float(re.search(r"([\d.]+)%", want).group(1))
    rows = []
    for head, name in (("BEFORE ANY", "no control"), ("AFTER THE BEST", "the best set"), ("WITH ALL", "all controls")):
        h = heading_of("FIVE", head)
        cost = re.search(r"\((\$[\d.]+[kM])\)", h)
        line = section("FIVE", head)[0]
        p = float(re.search(r"([\d.]+)%", line).group(1))
        rows.append((name + (" (%s)" % cost.group(1) if cost else ""), p, line.split()[-1]))
    header(d, 60, 170, "THE APPETITE: " + want.upper(), CYAN)
    x0, x1 = 560, 1480
    top = max(p for _, p, _ in rows)

    def px(p):
        return x0 + (x1 - x0) * p / top
    for i, (name, p, verdict) in enumerate(rows):
        y = 270 + i * 120
        c = GREEN if verdict == "WITHIN" else ROSE
        label(d, 60, y + 40, name, TEXT, 30, True, "lm")
        d.rectangle([x0, y + 10, px(p), y + 70], fill=c)
        mono(d, max(px(p), px(chance)) + 14, y + 40, "%.1f%%  %s" % (p, verdict), c, 28, "lm")
    dashed(d, px(chance), 250, 640, AMBER, 4)
    label(d, px(chance) + 10, 640, "appetite %.1f%%" % chance, AMBER, 26, True, "la")
    meets = section("FIVE", "THE CHEAPEST SET")
    header(d, 60, 700, "THE CHEAPEST SET WITHIN THE APPETITE", GREEN)
    fit_mono(d, 60, 770, meets[0].strip() + "; " + meets[1].strip(), TEXT, 28, 1800)
    options = section("FIVE", "THE DECISION")[0].strip()
    for i, opt in enumerate(re.split(r", or |, ", options)):
        cell(d, 60 + i * 610, 830, 580, 64, opt, AMBER, 28)
    caption(d, "The budget buys a lot, but not the appetite: the rest is the board's choice.", AMBER, 945)
