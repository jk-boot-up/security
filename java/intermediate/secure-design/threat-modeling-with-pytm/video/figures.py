"""Pictures for the pytm video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and the
tests check, so a picture can never disagree with the code.
"""

import math
import re
from pathlib import Path

from videokit.slides.theme import AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
SEV = {"Very High": ROSE, "High": ROSE, "Medium": AMBER, "Low": CYAN, "Very Low": MUTED}
KIND = {"Actor": CLIENT, "ExternalEntity": AMBER, "Process": CYAN, "Server": GREEN, "Datastore": ROSE, "Dataflow": MUTED}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading."""
    return re.search(r"^%s\. .*\n((?:(?![A-Z]+\. ).*\n?)*)" % name, OUT, re.M).group(1).splitlines()


def sections(name, head):
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


def heading(name, head):
    """The header line itself, e.g. "THE MODEL (Python written by the program: 66 lines):"."""
    return next(ln for ln in act(name) if ln.startswith(head))


def facts(lines):
    return dict(tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip()))


def cols(ln):
    return re.split(r"\s{2,}", ln.strip())


def num(text):
    return int(re.search(r"\d+", text).group())


# ------------------------------------------------------------------ drawing helpers

def header(d, x, y, text, colour, w=None, size=24):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 42], radius=21, fill=colour)
    d.text((x + w // 2, y + 21), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=28, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour, size, max_w=None, anchor="la"):
    f = font(MONO_B, size)
    while max_w and f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=955):
    label(d, W // 2, y, text, colour, 30, True, "ma")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def node(d, cx, cy, w, h, name, kind):
    c = KIND[kind]
    d.rounded_rectangle([cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2], radius=12, fill=BG_CELL, outline=c, width=3)
    f = font(SANS_B, 26)
    while f.getlength(name) > w - 16 and f.size > 14:
        f = font(SANS_B, f.size - 1)
    d.text((cx, cy - 14), name, font=f, fill=TEXT, anchor="mm")
    label(d, cx, cy + 18, kind, c, 20, False, "mm")


def line_arrow(d, x1, y1, x2, y2, colour, width=3):
    d.line([(x1, y1), (x2, y2)], fill=colour, width=width)
    a = math.atan2(y2 - y1, x2 - x1)
    p = [(x2, y2), (x2 - 16 * math.cos(a) + 9 * math.sin(a), y2 - 16 * math.sin(a) - 9 * math.cos(a)),
         (x2 - 16 * math.cos(a) - 9 * math.sin(a), y2 - 16 * math.sin(a) + 9 * math.cos(a))]
    d.polygon(p, fill=colour)


def dashed_rect(d, x, y, w, h, colour):
    for x0 in range(x, x + w, 24):
        d.line([(x0, y), (min(x0 + 12, x + w), y)], fill=colour, width=3)
        d.line([(x0, y + h), (min(x0 + 12, x + w), y + h)], fill=colour, width=3)
    for y0 in range(y, y + h, 24):
        d.line([(x, y0), (x, min(y0 + 12, y + h))], fill=colour, width=3)
        d.line([(x + w, y0), (x + w, min(y0 + 12, y + h))], fill=colour, width=3)


# ------------------------------------------------------------------ figures

TOP = 205


def one_threat(img, d, cfg):
    """The core idea: one threat's condition, one element, before and after one answer."""
    lib = facts(section("ONE", "THE LIBRARY"))
    tid, title = lib["one"].split(" ", 1)
    worked = heading("THREE", "ONE FINDING, WORKED OUT")
    target = re.search(r" on (.*)\):", worked).group(1)
    cond = [ln for ln in section("ONE", "THE LIBRARY") if ln.startswith("condition") or ln.startswith("            ")]
    cond_text = " ".join(c.replace("condition", "").strip() for c in cond)
    before = section("THREE", "BEFORE THE ANSWER")
    after = section("THREE", "AFTER THE ANSWER")
    stage_b = re.search(r"\((\w+)\)", heading("THREE", "BEFORE THE ANSWER")).group(1)
    stage_a = re.search(r"\((\w+)\)", heading("THREE", "AFTER THE ANSWER")).group(1)

    panel(d, 60, TOP + 20, 1800, 150, CYAN, 3)
    header(d, 80, TOP, "THE THREAT, IN pytm'S LIBRARY", CYAN)
    label(d, 100, TOP + 72, "%s  %s  (%s, %s)" % (tid, title.split(",")[0], lib["applies to"], title.split(", ")[-1]), TEXT, 34, True)
    mono(d, 100, TOP + 122, "condition: " + cond_text, AMBER, 32, 1740)

    def column(x, head, colour, stage, lines):
        y0 = TOP + 225
        panel(d, x, y0, 860, 500, colour, 3)
        header(d, x + 20, y0 - 20, head, colour)
        label(d, x + 30, y0 + 40, "%s: the %s's answers" % (stage, target), MUTED, 26)
        y = y0 + 100
        for ln in lines:
            if "->" in ln:
                left, right = [s.strip() for s in ln.split("->")]
                mono(d, x + 30, y, left, TEXT, 32)
                mono(d, x + 820, y, right, GREEN if right == "False" else ROSE, 34, None, "ra")
            elif "=" in ln and ":" in ln:
                expr, verdict = ln.split(":", 1)
                y += 16
                mono(d, x + 30, y, expr.strip(), TEXT, 34)
                rep = verdict.strip()
                label(d, x + 30, y + 64, rep.upper(), ROSE if rep == "reported" else GREEN, 46, True)
            else:
                mono(d, x + 30, y, ln, CYAN, 30, 800)
            y += 62

    column(60, "BEFORE THE ANSWER", ROSE, stage_b, before)
    line_arrow(d, 928, TOP + 475, 992, TOP + 475, MUTED, 6)
    column(1000, "AFTER THE ANSWER", GREEN, stage_a, after)
    caption(d, "pytm reports a threat when its condition is True for the answers given; one answer turns it off.", AMBER, 965)


def bank_model(img, d, cfg):
    """The design as pytm sees it: boundaries, elements and numbered flows, and what it becomes."""
    elements = [cols(ln) for ln in section("ONE", "THE DESIGN")]
    flows = [cols(ln) for ln in section("ONE", "THE FLOWS")]
    where = {e[1]: e for e in elements}
    order, seen_el = [], []
    for f in flows:
        for el in f[1].split(" -> "):
            if where[el][0] not in order:
                order.append(where[el][0])
            if el not in seen_el:
                seen_el.append(el)
    bw, gap = 420, 40
    xs = {b: 60 + i * (bw + gap) for i, b in enumerate(order)}
    pos = {}
    top, bottom = TOP + 50, 700
    for b in order:
        inside = [where[e] for e in seen_el if where[e][0] == b]
        dashed_rect(d, xs[b], top, bw, bottom - top, ROSE)
        label(d, xs[b] + bw // 2, top + 12, b, ROSE, 28, True, "ma")
        # the second row: elements reached from another boundary first, so their arrows do not cross the others
        rest = sorted(inside[1:], key=lambda e: -sum(1 for f in flows if f[1].split(" -> ")[1] == e[1] and where[f[1].split(" -> ")[0]][0] != b))
        rows = [inside[:1], rest] if len(inside) > 1 else [inside]
        for r, row in enumerate(rows):
            cy = top + 150 + r * 250 if len(rows) > 1 else (top + bottom) // 2 + 20
            w = 340 if len(row) == 1 else 185
            for k, e in enumerate(row):
                cx = xs[b] + bw // 2 if len(row) == 1 else xs[b] + 110 + k * 200
                pos[e[1]] = (cx, cy, w)
                node(d, cx, cy, w, 110, e[3], e[2])
    pairs = {tuple(f[1].split(" -> ")) for f in flows}
    for f in flows:
        a, b = f[1].split(" -> ")
        (x1, y1, w1), (x2, y2, w2) = pos[a], pos[b]
        off = 22 if (b, a) in pairs and a > b else -22 if (b, a) in pairs else 0
        if abs(x1 - x2) < 220 and y1 != y2:
            down = 1 if y2 > y1 else -1
            sx, sy, ex, ey = x1 + off, y1 + down * 55, x2 + off, y2 - down * 55
        else:
            dirx = 1 if x2 > x1 else -1
            sx, sy, ex, ey = x1 + dirx * w1 // 2, y1 + off, x2 - dirx * w2 // 2, y2 + off
        line_arrow(d, sx, sy, ex, ey, CYAN, 4)
        mx, my = (sx + ex) // 2, (sy + ey) // 2
        d.rounded_rectangle([mx - 26, my - 18, mx + 26, my + 18], radius=9, fill=INK, outline=AMBER, width=2)
        label(d, mx, my, f[0], AMBER, 24, True, "mm")
    lines = re.search(r"(\d+) lines", heading("ONE", "THE MODEL")).group(1)
    lib = facts(section("ONE", "THE LIBRARY"))
    steps = [("Java records", "%d elements, %d flows" % (len(elements), len(flows)), CLIENT),
             ("Writer", "%s lines of Python" % lines, CYAN),
             ("pytm %s" % re.search(r"pytm ([\d.]+)", heading("ONE", "THE LIBRARY")).group(1), "%s threats checked" % lib["threats"].split(",")[0], AMBER)]
    x = 130
    for i, (a, b, c) in enumerate(steps):
        panel(d, x, 760, 460, 120, c, 3)
        label(d, x + 230, 778, a, c, 32, True, "ma")
        label(d, x + 230, 830, b, TEXT, 28, False, "ma")
        if i < 2:
            line_arrow(d, x + 470, 820, x + 580, 820, MUTED, 5)
        x += 600
    caption(d, "Dashed boxes are trust boundaries; every arrow is a flow that pytm checks too.", AMBER, 950)


def findings_per_element(img, d, cfg):
    """Findings per element against the threats that apply to its kind, and by severity."""
    rows, total = [], None
    for ln in section("TWO", "THE FINDINGS PER ELEMENT"):
        if ln.startswith("sum"):
            total = ln.split(None, 1)[1]
            continue
        p = cols(ln)
        n, of = re.match(r"(\d+) of (\d+)", p[1]).groups()
        rows.append((p[0], int(n), int(of), p[2]))
    header(d, 60, TOP, "FINDINGS PER ELEMENT, OUT OF THE THREATS FOR ITS KIND", CYAN)
    top, scale = TOP + 70, 900 / max(r[2] for r in rows)
    for i, (name, n, of, kind) in enumerate(rows):
        y = top + i * 78
        label(d, 60, y + 4, name, TEXT, 28, True)
        label(d, 60, y + 40, kind, KIND[kind], 22)
        x0 = 400
        if of:
            d.rounded_rectangle([x0, y + 6, x0 + int(of * scale), y + 60], radius=8, outline=LINE, width=2, fill=BG_CELL)
        if n:
            d.rounded_rectangle([x0, y + 6, x0 + max(int(n * scale), 12), y + 60], radius=8, fill=KIND[kind])
        label(d, x0 + int(max(of, 1) * scale) + 18, y + 16, "%d of %d" % (n, of), TEXT, 28, True)
    sev = [cols(ln) for ln in section("TWO", "THE FINDINGS BY SEVERITY")]
    x = 1500
    header(d, x, TOP, "BY SEVERITY", ROSE, 360)
    for i, (s, n) in enumerate(sev):
        y = TOP + 70 + i * 92
        panel(d, x, y, 360, 74, SEV[s], 3)
        label(d, x + 22, y + 37, s, SEV[s], 30, True, "lm")
        label(d, x + 338, y + 37, n, TEXT, 36, True, "rm")
    mono(d, 60, top + len(rows) * 78 + 14, "sum  " + total, AMBER, 34, 1800)
    caption(d, "With no answers every control is False: pytm starts from the worst case.", AMBER, 965)


def stages(img, d, cfg):
    """Each stage of answers, the findings left and how many went; and the hand check."""
    rows = [cols(ln) for ln in section("THREE", "THE STAGES")][1:]
    check = facts(section("THREE", "THE CHECK"))
    header(d, 60, TOP, "THE SAME BANK, MORE ANSWERS AT EACH STAGE", CYAN)
    top = TOP + 75
    maxf = max(int(r[2]) for r in rows)
    for i, (stage, answers, found, gone, new) in enumerate(rows):
        y = top + i * 132
        label(d, 60, y + 10, stage, TEXT, 32, True)
        label(d, 60, y + 56, "%s answers in the model" % answers, MUTED, 26)
        w = int(int(found) / maxf * 900)
        colour = ROSE if i == 0 else GREEN if i == len(rows) - 1 else AMBER
        d.rounded_rectangle([450, y + 6, 450 + w, y + 96], radius=12, fill=colour)
        label(d, 474, y + 51, "%s findings" % found, INK, 36, True, "lm")
        if gone != "-":
            label(d, 450 + w + 22, y + 51, "-%s" % gone, GREEN, 38, True, "lm")
    x = 1500
    panel(d, x, TOP + 75, 360, 560, CYAN, 3)
    label(d, x + 180, TOP + 100, "THE CHECK BY HAND", CYAN, 28, True, "ma")
    label(d, x + 180, TOP + 150, "class Condition", TEXT, 30, True, "ma")
    for k, (a, b) in enumerate([("simple conditions", check["simple conditions"].replace(" threats", "")), ("checks", check["checks"].split()[0]),
                                ("same as pytm", check["same as pytm"])]):
        label(d, x + 180, TOP + 240 + k * 125, a, MUTED, 28, False, "ma")
        label(d, x + 180, TOP + 282 + k * 125, b, GREEN, 38, True, "ma")
    caption(d, "Every answer removes findings, none adds one. An untrue answer removes them too.", AMBER, 965)


def one_model_outputs(img, d, cfg):
    """One model, three outputs: the findings, the diagram and the report."""
    dia = facts(section("FOUR", "THE DIAGRAM"))
    dot_lines = re.search(r"(\d+) lines", heading("FOUR", "THE DIAGRAM")).group(1)
    rep = section("FOUR", "THE REPORT")
    rep_lines = re.search(r"(\d+) lines", heading("FOUR", "THE REPORT")).group(1)
    items = re.search(r"(\d+) lines in all", rep[-1]).group(1)
    final = [cols(ln) for ln in section("THREE", "THE STAGES")][-1]
    panel(d, 710, TOP + 5, 500, 130, CYAN, 3)
    label(d, 960, TOP + 25, "bank.py", CYAN, 36, True, "ma")
    label(d, 960, TOP + 80, "the model, %s answers" % final[1], TEXT, 28, False, "ma")
    outs = [
        ("--json", "THE FINDINGS", AMBER, ["%s findings" % final[2], "each one: threat,", "element, severity,", "title"]),
        ("--dfd, then dot", "THE DIAGRAM", GREEN, ["%s lines of graph text" % dot_lines, "boundaries  " + dia["boundaries"].split(":")[0],
                                                   "boxes       " + dia["boxes"].split()[0], "arrows      " + dia["arrows"].split(",")[0]]),
        ("--report", "THE REPORT", ROSE, ["%s lines of Markdown" % rep_lines, "%s of them findings:" % items] + rep[1:3]),
    ]
    for i, (opt, title, colour, lines) in enumerate(outs):
        x = 60 + i * 610
        line_arrow(d, 960, TOP + 140, x + 290, TOP + 260, MUTED, 4)
        label(d, x + 290, TOP + 205, opt, colour, 28, True, "ma") if i != 1 else label(d, 985, TOP + 218, opt, colour, 28, True, "la")
        panel(d, x, TOP + 285, 580, 420, colour, 3)
        header(d, x + 20, TOP + 265, title, colour)
        for k, ln in enumerate(lines):
            mono(d, x + 30, TOP + 345 + k * 76, ln, TEXT, 32, 520)
    caption(d, "The findings, the diagram and the report come from one model, so they cannot disagree.", AMBER, 960)


def the_bill(img, d, cfg):
    """A design change caught only because the model was regenerated, and an XML finding worked out."""
    change = section("FIVE", "THE CHANGE")
    res = section("FIVE", "THE RESULT")
    xml = section("FIVE", "THE XML FINDINGS")
    head = facts(res[:1])
    before, after = re.match(r"(\d+) before, (\d+) after", head["findings"]).groups()
    y0 = TOP + 20
    panel(d, 60, y0, 880, 700, AMBER, 3)
    header(d, 80, y0 - 20, "BEFORE AND AFTER THE CHANGE", AMBER)
    mono(d, 90, y0 + 45, " ".join(cols(change[0])), CYAN, 32, 820)
    label(d, 90, y0 + 100, change[1].split("  ", 1)[1].strip(), MUTED, 28)
    label(d, 290, y0 + 150, before, GREEN, 84, True, "ma")
    label(d, 290, y0 + 250, "findings before", MUTED, 26, False, "ma")
    line_arrow(d, 410, y0 + 200, 570, y0 + 200, MUTED, 6)
    label(d, 710, y0 + 150, after, ROSE, 84, True, "ma")
    label(d, 710, y0 + 250, "findings after", MUTED, 26, False, "ma")
    for k, ln in enumerate(res[1:]):
        tid, sev, title = cols(ln)
        mono(d, 90, y0 + 320 + k * 72, tid, SEV[sev], 32)
        label(d, 230, y0 + 322 + k * 72, title, TEXT, 30)
    panel(d, 980, y0, 880, 700, ROSE, 3)
    header(d, 1000, y0 - 20, "AN XML FINDING, WORKED OUT", ROSE)
    f = facts(xml)
    label(d, 1010, y0 + 45, "%s mention XML; the bank parses none" % f["findings"].split()[0], TEXT, 32, True)
    label(d, 1010, y0 + 105, f["one"], AMBER, 30, True)
    parts = f["condition"].split(" or ")
    for k, part in enumerate(parts):
        mono(d, 1010, y0 + 170 + k * 46, part + (" or" if k < len(parts) - 1 else ""), TEXT, 30)
    y = y0 + 300
    for ln in xml:
        if "->" in ln:
            left, right = [s.strip() for s in ln.split("->")]
            mono(d, 1010, y, left, TEXT, 30)
            mono(d, 1820, y, right, ROSE, 32, None, "ra")
            y += 66
        elif ln.startswith("True") or ln.startswith("False"):
            mono(d, 1010, y + 10, ln, ROSE, 32, 820)
            y += 96
    label(d, 1010, y + 10, "a person: " + f["a person"].replace("reads it and says: ", ""), GREEN, 36, True)
    caption(d, "The model catches a change only if it is regenerated; a person still says what applies.", AMBER, 965)
