"""Pictures for the security architecture review video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import math
import re
from pathlib import Path

from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head, n=0):
    """The n-th section of an act whose header starts with `head`, as a list of lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[n]


def cols(line):
    return re.split(r"\s{2,}", line.strip())


def table(name, head, n=0):
    """A section whose first line names the columns, as a list of dicts."""
    rows = section(name, head, n)
    keys = cols(rows[0])
    return [dict(zip(keys, cols(r))) for r in rows[1:] if len(cols(r)) == len(keys)]


def pairs(name, head, n=0):
    """A section of 'label   value' lines, as a dict."""
    return {c[0]: c[1] for c in (cols(r) for r in section(name, head, n)) if len(c) == 2}


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


def fit_sans(d, x, y, text, colour, size, max_w, anchor="la", bold=False):
    f = font(SANS_B if bold else SANS, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=950):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def arrow(d, x1, y1, x2, y2, colour, width=4):
    """A straight arrow with its head at (x2, y2), in any direction."""
    d.line([(x1, y1), (x2, y2)], fill=colour, width=width)
    a = math.atan2(y2 - y1, x2 - x1)
    pts = [(x2, y2)]
    for s in (-1, 1):
        pts.append((x2 - 20 * math.cos(a) + 10 * s * math.sin(a), y2 - 20 * math.sin(a) - 10 * s * math.cos(a)))
    d.polygon(pts, fill=colour)


def edge(box, tx, ty):
    """Where the line from the centre of a box towards (tx, ty) leaves the box."""
    x, y, w, h = box
    cx, cy = x + w / 2, y + h / 2
    dx, dy = tx - cx, ty - cy
    t = min((w / 2) / abs(dx) if dx else 1e9, (h / 2) / abs(dy) if dy else 1e9)
    return cx + dx * t, cy + dy * t


# ------------------------------------------------------------------ figures

# where each zone sits: column, first row, last row
ZONES = {"internet": (0, 0, 0), "office": (0, 1, 2), "dmz": (1, 0, 2), "data": (2, 0, 2),
         "app": (3, 0, 2), "external": (4, 1, 1)}
SPOT = {"customer": 0, "staff": 2, "web": 1, "sessions": 0, "ledgerdb": 2, "reports": 0, "api": 1,
        "ledger": 2, "cards": 1}


def flow_colour(f, parts):
    src, dst = f["flow"].split(" -> ")
    outer = parts[src]["zone"] in ("dmz", "internet") and parts[dst]["data"] == "confidential"
    if f["encrypted"] == "no" or f["caller check"] == "none" or outer:
        return ROSE
    return GREEN


def design_map(img, d, cfg):
    """The parts in their zones and the nine flows, rose where a flow rule would object."""
    parts = {p["id"]: p for p in table("ONE", "THE PARTS")}
    flows = table("ONE", "THE FLOWS")
    colx = lambda c: 60 + c * 370
    rowy = lambda r: 260 + r * 215
    for z, (c, r0, r1) in ZONES.items():
        x, y0, y1 = colx(c), rowy(r0) - 58, rowy(r1) + 118
        d.rounded_rectangle([x, y0, x + 340, y1], radius=18, outline=LINE, width=3)
        label(d, x + 16, y0 + 8, "zone " + z, CYAN, 24, True)
    boxes = {}
    for pid, p in parts.items():
        c = ZONES[p["zone"]][0]
        x, y = colx(c) + 25, rowy(SPOT[pid])
        boxes[pid] = (x, y, 290, 100)
        colour = AMBER if p["data"] == "confidential" else LINE
        d.rounded_rectangle([x, y, x + 290, y + 100], radius=14, fill=PANEL, outline=colour, width=3)
        label(d, x + 145, y + 14, pid, TEXT, 30, True, "ma")
        label(d, x + 145, y + 58, p["kind"] + ("  " + p["data"] if p["data"] != "-" else ""), MUTED, 22, False, "ma")
    for f in flows:
        src, dst = f["flow"].split(" -> ")
        b1, b2 = boxes[src], boxes[dst]
        c2 = (b2[0] + b2[2] / 2, b2[1] + b2[3] / 2)
        c1 = (b1[0] + b1[2] / 2, b1[1] + b1[3] / 2)
        x1, y1 = edge(b1, *c2)
        x2, y2 = edge(b2, *c1)
        arrow(d, x1, y1, x2, y2, flow_colour(f, parts))
    x = colx(4) + 10
    total = section("ONE", "THE FLOWS")[-1]
    m = re.match(r"(\d+) flows: (\d+) cross a zone, (\d+) inside", total)
    label(d, x, 200, "%d parts, %d zones" % (len(parts), len(ZONES)), TEXT, 26, True)
    label(d, x, 240, "%s flows:" % m.group(1), TEXT, 26, True)
    label(d, x, 280, "%s cross a zone," % m.group(2), MUTED, 24)
    label(d, x, 314, "%s inside one" % m.group(3), MUTED, 24)
    arrow(d, x, 650, x + 60, 650, GREEN)
    label(d, x + 75, 636, "encrypted, caller", GREEN, 22)
    label(d, x + 75, 662, "checked", GREEN, 22)
    arrow(d, x, 715, x + 60, 715, ROSE)
    label(d, x + 75, 701, "a flow rule", ROSE, 22)
    label(d, x + 75, 727, "objects", ROSE, 22)
    d.rounded_rectangle([x, 768, x + 60, 806], radius=10, outline=AMBER, width=3)
    label(d, x + 75, 774, "confidential", AMBER, 22)
    stride = section("ONE", "HOW THE STRIDE")[-1]
    label(d, x, 840, "STRIDE questions", TEXT, 24, True)
    label(d, x, 875, cols(stride)[-1], AMBER, 50, True)


def flow_check(img, d, cfg):
    """Two flows walk the questions of the flow rules; the rules that fire come out at the bottom."""
    rows = [cols(ln) for ln in section("TWO", "HOW TWO FLOWS")]
    q = [r for r in rows if len(r) == 3]
    fire = [r for r in rows if len(r) == 2]
    names = q[0][1:]
    header(d, 60, 190, "THE FLOWS", CYAN)
    for k, n in enumerate(names):
        cell(d, 900 + k * 480, 180, 440, 64, n, CYAN, 32)
    header(d, 60, 270, "THE QUESTIONS THE FLOW RULES ASK", AMBER)
    bad = {"encrypted?": "no", "caller check?": "none", "to confidential data?": "yes"}
    for i, r in enumerate(q[1:]):
        y = 335 + i * 76
        panel(d, 60, y, 800, 64)
        label(d, 90, y + 32, r[0], TEXT, 30, True, "lm")
        for k, a in enumerate(r[1:]):
            colour = ROSE if bad.get(r[0]) == a else MUTED if r[0] in ("zones", "from an outer zone?") else GREEN
            cell(d, 900 + k * 480, y, 440, 64, a, colour, 28)
    header(d, 60, 815, "THE RULES THAT FIRE", ROSE)
    for k, r in enumerate(fire):
        cell(d, 900 + k * 480, 800, 440, 72, r[1], ROSE, 28)
    caption(d, "Same zones, different answers: each rule reads one property of the flow.", AMBER, 920)


def score_grid(img, d, cfg):
    """The ten planted weaknesses, and which finding of the first rules reports each one."""
    planted = [cols(ln) for ln in section("TWO", "THE PLANTED")]
    found = {f["planted"]: f["rule"] for f in table("TWO", "THE FINDINGS") if f["planted"] != "-"}
    header(d, 60, 175, "THE PLANTED WEAKNESSES (THE TRUTH)", CYAN)
    header(d, 1230, 175, "THE FIRST RULES REPORT", GREEN)
    for i, (n, subject, text) in enumerate(planted):
        y = 235 + i * 66
        panel(d, 60, y, 1130, 56)
        label(d, 80, y + 28, n, MUTED, 26, True, "lm")
        fit_mono(d, 130, y + 28, subject, CYAN, 24, 260, "lm")
        fit_sans(d, 400, y + 28, text, TEXT, 26, 770, "lm")
        if n in found:
            cell(d, 1230, y, 630, 56, found[n], GREEN, 24)
        else:
            cell(d, 1230, y, 630, 56, "missed", ROSE, 26)
    s = pairs("TWO", "THE SCORE")
    caption(d, "%s of %s found; %s finding matched no planted one" % (
        s["planted ones found (rule, subject)"], s["planted weaknesses"], s["findings that match no planted one"]), AMBER, 915)


def one_more_rule(img, d, cfg):
    """The flow inside one zone that only the new rule sees, and the score before and after."""
    inside = section("THREE", "THE FLOWS INSIDE")[0]
    m = re.match(r"(\S+ -> \S+)\s+(\S+ -> \S+)\s+encrypted (\w+), caller check (\w+)", inside)
    rule = section("THREE", "THE NEW RULE")[0]
    header(d, 60, 180, "THE NEW RULE", CYAN)
    fit_mono(d, 60, 245, rule, TEXT, 30, 900)
    header(d, 60, 310, "THE FLOW INSIDE ONE ZONE", AMBER)
    zone = m.group(2).split(" -> ")[0]
    d.rounded_rectangle([60, 380, 900, 640], radius=18, outline=LINE, width=3)
    label(d, 80, 390, "zone " + zone, CYAN, 24, True)
    a, b = m.group(1).split(" -> ")
    for x, name in ((110, a), (610, b)):
        d.rounded_rectangle([x, 450, x + 240, 550], radius=14, fill=PANEL, outline=LINE, width=3)
        label(d, x + 120, 500, name, TEXT, 32, True, "mm")
    arrow(d, 350, 500, 610, 500, ROSE)
    label(d, 480, 570, "encrypted %s, caller check %s" % (m.group(3), m.group(4)), ROSE, 26, True, "ma")
    label(d, 60, 670, "The cross-zone rules skip it: from and to are in one zone.", MUTED, 26)
    before, after = pairs("TWO", "THE SCORE"), pairs("THREE", "THE SCORE")
    header(d, 1000, 180, "THE SCORE", GREEN)
    total = int(before["planted weaknesses"])
    for i, (name, s) in enumerate((("first 8 rules", before), ("all 9 rules", after))):
        y = 270 + i * 190
        hit = int(s["planted ones found (rule, subject)"])
        label(d, 1000, y, name, TEXT, 30, True)
        for k in range(total):
            colour = GREEN if k < hit else ROSE
            d.rounded_rectangle([1000 + k * 85, y + 50, 1070 + k * 85, y + 110], radius=10, fill=BG_CELL, outline=colour, width=3)
            label(d, 1035 + k * 85, y + 80, str(k + 1), colour, 24, True, "mm")
        label(d, 1000, y + 125, "%d of %d found, %s other findings" % (hit, total, s["findings that match no planted one"]), MUTED, 26)
    left = cols(section("THREE", "NOT REPORTED")[0])
    header(d, 1000, 690, "STILL MISSED", ROSE)
    fit_sans(d, 1000, 755, "%s  %s" % (left[0], left[2]), TEXT, 28, 860)
    fit_sans(d, 1000, 800, section("THREE", "NOT REPORTED")[-1], MUTED, 26, 860)
    caption(d, "Zero trust: check the calls inside a zone too.", AMBER, 925)


def before_after(img, d, cfg):
    """The same five numbers before and after the review, and how many things changed."""
    b, a = pairs("FOUR", "BEFORE THE REVIEW"), pairs("FOUR", "AFTER THE REVIEW")
    header(d, 760, 180, "BEFORE THE REVIEW", ROSE, 420)
    header(d, 1280, 180, "AFTER THE REVIEW", GREEN, 420)
    for i, k in enumerate(b):
        y = 250 + i * 92
        panel(d, 60, y, 660, 76)
        label(d, 90, y + 38, k, TEXT, 28, True, "lm")
        same = b[k] == a[k]
        cell(d, 760, y, 420, 76, b[k], MUTED if same else ROSE, 30)
        cell(d, 1280, y, 420, 76, a[k], MUTED if same else GREEN, 30)
    n = [len(section("FOUR", h)) for h in ("THE PARTS THAT", "THE FLOWS THAT", "THE DECISION RECORDS THAT")]
    header(d, 60, 735, "THE CHANGES IN THE DESIGN FILES", AMBER)
    for k, (count, what) in enumerate(zip(n, ("part properties", "flows", "decision records"))):
        cell(d, 60 + k * 560, 800, 520, 80, "%d %s" % (count, what), AMBER, 30)
    caption(d, "The web zone now reaches the ledger only through the payments API.", AMBER, 925)


def bill(img, d, cfg):
    """What the rules gave, and what is still left to people."""
    gave, left = pairs("FIVE", "WHAT THE RULES GAVE"), section("FIVE", "WHAT IS STILL LEFT")
    header(d, 60, 180, "WHAT THE RULES GAVE", GREEN)
    header(d, 1000, 180, "WHAT IS STILL LEFT TO PEOPLE", ROSE)
    for i, (k, v) in enumerate(gave.items()):
        y = 260 + i * 160
        panel(d, 60, y, 860, 130, GREEN)
        label(d, 90, y + 65, k, TEXT, 30, True, "lm")
        label(d, 890, y + 65, v, GREEN, 48, True, "rm")
    for i, ln in enumerate(left):
        y = 260 + i * 120
        c = cols(ln)
        panel(d, 1000, y, 860, 100, ROSE)
        if len(c) == 2:
            label(d, 1030, y + 50, c[0], TEXT, 28, True, "lm")
            label(d, 1830, y + 50, c[1], ROSE, 44, True, "rm")
        else:
            fit_sans(d, 1030, y + 50, c[0], TEXT, 28, 800, "lm", True)
    caption(d, section("FIVE", "THE LESSON")[-1], AMBER, 920)
