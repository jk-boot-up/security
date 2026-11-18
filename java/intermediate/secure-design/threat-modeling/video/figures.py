"""Pictures for the threat modeling video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code. Only the positions
of the boxes in the data-flow picture are chosen here.
"""

import math
import re
from pathlib import Path

from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
KIND = {"external entity": AMBER, "process": CYAN, "data store": GREEN}


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


def section(name, head):
    return sections(name, head)[0]


def containing(name, word):
    """The header of the act's section whose header contains `word`."""
    return next(ln for ln in act(name) if not ln.startswith(" ") and word in ln)


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


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


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None, anchor="mm", fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    tx = x + w // 2 if anchor == "mm" else x + 16
    fit_mono(d, tx, y + h // 2, text, text_colour or colour, size, w - 24, anchor)


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def dashed(d, a, b, colour, width=3, dash=16):
    (x1, y1), (x2, y2) = a, b
    n = max(1, int(math.hypot(x2 - x1, y2 - y1) // dash))
    for k in range(0, n, 2):
        t1, t2 = k / n, min(1, (k + 1) / n)
        d.line([x1 + (x2 - x1) * t1, y1 + (y2 - y1) * t1, x1 + (x2 - x1) * t2, y1 + (y2 - y1) * t2], fill=colour, width=width)


def arrow(d, a, b, colour, width=4, dash=False):
    (x1, y1), (x2, y2) = a, b
    (dashed if dash else lambda dd, p, q, c, w: dd.line([p, q], fill=c, width=w))(d, a, b, colour, width)
    ang = math.atan2(y2 - y1, x2 - x1)
    tip = [(x2, y2), (x2 - 22 * math.cos(ang - 0.4), y2 - 22 * math.sin(ang - 0.4)),
           (x2 - 22 * math.cos(ang + 0.4), y2 - 22 * math.sin(ang + 0.4))]
    d.polygon(tip, fill=colour)


def elements():
    """id -> (zone, trust, name, kind) from act ONE."""
    out = {}
    for ln in section("ONE", "THE ELEMENTS"):
        m = re.match(r"\s*(\S+)\s+(\d)\s+(\S+)\s+(.+?)\s{2,}(.+)", ln)
        out[m.group(3)] = (m.group(1), int(m.group(2)), m.group(4), m.group(5))
    return out


def flows():
    """(id, from, to, data, crossed) from act ONE."""
    rows = []
    for ln in section("ONE", "THE FLOWS"):
        m = re.match(r"\s*(\S+)\s+(\S+) -> (\S+)\s+(.+?)\s{2,}(\S+)$", ln)
        rows.append(m.groups())
    return rows


# ------------------------------------------------------------------ figures

def one_flow(img, d, cfg):
    """The core idea: STRIDE's six questions asked of one real flow, F2."""
    el = elements()
    head = heading_of("TWO", "SIX QUESTIONS")
    fid, data = re.search(r"\((\S+) (.*)\)", head).groups()
    f = next(x for x in flows() if x[0] == fid)
    a, b = el[f[1]], el[f[2]]
    header(d, 60, 170, "THE INPUT: ONE FLOW", CYAN)
    for (x, (zone, trust, name, kind), fid_) in ((160, a, f[1]), (1260, b, f[2])):
        cell(d, x, 260, 500, 100, "%s %s" % (fid_, name), KIND[kind.split(",")[0]], 34)
        label(d, x + 250, 375, "%s zone, trust %d" % (zone, trust), MUTED, 26, True, "ma")
    arrow(d, (660, 310), (1255, 310), AMBER, 5)
    label(d, 960, 262, "%s %s" % (fid, data), AMBER, 28, True, "md")
    dashed(d, (960, 330), (960, 420), ROSE, 4, 10)
    label(d, 960, 425, f[4] + " boundary", ROSE, 24, True, "ma")
    header(d, 60, 470, "THE RESULT: SIX QUESTIONS, %d APPLY" % sum("yes" in q for q in section("TWO", "SIX QUESTIONS")), AMBER)
    for k, ln in enumerate(section("TWO", "SIX QUESTIONS")):
        letter, name, rest = ln.split(None, 2)
        ans, why = rest.split(None, 1)
        x, y = 60 + (k % 3) * 610, 540 + (k // 3) * 190
        c = GREEN if ans == "yes" else LINE
        panel(d, x, y, 580, 170, c, 3)
        label(d, x + 30, y + 20, letter, c if ans == "yes" else MUTED, 80, True)
        label(d, x + 130, y + 30, name, TEXT, 30, True)
        label(d, x + 130, y + 72, ans, c if ans == "yes" else MUTED, 30, True)
        fit_mono(d, x + 30, y + 130, why, TEXT if ans == "yes" else MUTED, 22, 530)


POS = {"E1": (250, 300), "P1": (250, 600), "P2": (720, 600), "P3": (1170, 600), "S1": (1170, 330),
       "S2": (1170, 840), "E2": (1720, 600), "S3": (1360, 730)}
BOX_W, BOX_H = 250, 80


def _edge(c, other, dy=0):
    """Where a line from box centre c towards other leaves the box."""
    (x, y), (ox, oy) = c, other
    dx, dyy = ox - x, oy - y
    s = min(BOX_W / 2 / abs(dx) if dx else 9e9, BOX_H / 2 / abs(dyy) if dyy else 9e9)
    return x + dx * s, y + dyy * s + dy


def dfd(img, d, cfg):
    """The bank as a data-flow model, with the draft's two mistakes drawn in rose."""
    el = elements()
    zones = {}
    for eid, (zone, trust, _, _) in el.items():
        zones.setdefault(zone, (trust, []))[1].append(POS[eid][0])
    bounds = [re.split(r"\s{2,}", ln.strip()) for ln in section("ONE", "THE TRUST BOUNDARIES")]
    for zone, (trust, xs) in zones.items():
        cx = sum(xs) / len(xs)
        label(d, cx, 196, "%s (trust %d)" % (zone, trust), TEXT, 26, True, "ma")
    for name, pair in bounds:
        za, zb = [z.strip() for z in pair.split("|")]
        x = (max(zones[za][1]) + min(zones[zb][1])) / 2 if max(zones[za][1]) < min(zones[zb][1]) else \
            (max(zones[zb][1]) + min(zones[za][1])) / 2
        x = 1540 if za == "internal" and zb == "partner" else x
        dashed(d, (x, 245), (x, 890), ROSE, 3, 14)
        label(d, x, 238, name, ROSE, 22, True, "md")
    pairs = {}
    for fid, a, b, data, crossed in flows():
        key = tuple(sorted((a, b)))
        off = 22 if key in pairs else (-22 if (b, a) in [(x[1], x[2]) for x in flows() if x[0] != fid] else 0)
        pairs[key] = True
        p, q = _edge(POS[a], POS[b], off), _edge(POS[b], POS[a], off)
        c = AMBER if crossed != "-" else MUTED
        arrow(d, p, q, c, 4)
        mx, my = (p[0] + q[0]) / 2, (p[1] + q[1]) / 2 + off * 1.6
        d.rounded_rectangle([mx - 28, my - 17, mx + 28, my + 17], radius=8, fill=INK)
        label(d, mx, my, fid, c, 22, True, "mm")
    # the draft's additions
    draft = [ln.strip() for ln in section("ONE", "BEFORE THE CHECK")]
    sid, sname = re.match(r"(\S+)\s+([^,]+)", draft[0]).groups()
    did, dfrom, dto = re.match(r"(\S+)\s+(\S+) -> (\S+)", draft[1]).groups()
    p, q = _edge(POS[dfrom], POS[dto]), _edge(POS[dto], POS[dfrom])
    arrow(d, p, q, ROSE, 4, dash=True)
    label(d, (p[0] + q[0]) / 2 - 120, (p[1] + q[1]) / 2 + 30, did + " (draft)", ROSE, 22, True, "mm")
    for eid, (zone, trust, name, kind) in el.items():
        x, y = POS[eid]
        cell(d, x - BOX_W // 2, y - BOX_H // 2, BOX_W, BOX_H, "%s %s" % (eid, name), KIND[kind.split(",")[0]], 26)
    x, y = POS[sid]
    d.rounded_rectangle([x - 120, y - 40, x + 120, y + 40], radius=10, fill=BG_CELL, outline=ROSE, width=3)
    fit_mono(d, x, y, "%s %s" % (sid, sname), ROSE, 24, 220, "mm")
    built = section("ONE", "THE CHECK")[0].strip()
    problems = [ln.strip() for ln in section("ONE", "AFTER THE CHECK")]
    header(d, 60, 900, "THE RESULT OF THE CHECK", AMBER)
    fit_mono(d, 520, 908, "the bank as built: " + re.sub(r"\s+", " ", built.replace("the bank as built", "")).strip(), GREEN, 26, 1340)
    fit_mono(d, 520, 948, "the draft: " + "; ".join(problems), ROSE, 26, 1340)


def stride_rules(img, d, cfg):
    """The six rules on the whole model: how many threats each letter finds, and where."""
    names = {ln.split()[0]: ln.split()[1] for ln in section("TWO", "SIX QUESTIONS")}
    rows = section("TWO", "THE RULES")
    total = rows[-1].strip()
    header(d, 60, 170, "THE INPUT: 7 ELEMENTS AND 8 FLOWS  ->  THE RESULT: " + total.upper(), CYAN)
    for k, ln in enumerate(rows[:-1]):
        letter, n = ln.split()[:2]
        targets = re.findall(r"\b[A-Z]\d\b", ln[5:])
        asked = ln[21:].strip()
        x = 60 + k * 305
        panel(d, x, 240, 285, 640, LINE, 2)
        label(d, x + 142, 250, letter, AMBER, 90, True, "ma")
        label(d, x + 142, 360, names[letter], TEXT, 28, True, "ma")
        label(d, x + 142, 400, "%s found" % n, GREEN, 30, True, "ma")
        for j, t in enumerate(targets):
            cell(d, x + 52, 450 + j * 62, 180, 50, t, CYAN, 28)
        words = asked.split()
        lines, cur = [], ""
        for w in words:
            if len(cur) + len(w) > 16:
                lines.append(cur)
                cur = w
            else:
                cur = (cur + " " + w).strip()
        lines.append(cur)
        for j, t in enumerate(lines):
            label(d, x + 142, 780 + j * 32, t, MUTED, 24, False, "ma")
    caption(d, "The rules only make sure no question is forgotten; people decide.", AMBER, 925)


def risk_matrix(img, d, cfg):
    """The 20 threats on the likelihood x impact grid, and the decisions taken."""
    head = containing("THREE", "RISK MATRIX")
    grid = section("THREE", head)[1:]
    header(d, 60, 170, "THE INPUT: %s" % head.split(" (")[0].replace("THE ", "", 1), CYAN)
    x0, y0, cw, ch = 380, 260, 150, 112
    for i, ln in enumerate(grid):
        like = int(ln.strip()[0])
        zone = re.search(r"\((.*?)\)", ln)
        label(d, x0 - 20, y0 + i * ch + ch // 2, "%d %s" % (like, zone.group(1) if zone else ""), MUTED, 26, True, "rm")
        counts = ln.split()[-5:]
        for j, v in enumerate(counts):
            risk = like * (j + 1)
            c = ROSE if risk >= 20 else AMBER if risk >= 10 else LINE
            fill = (70, 30, 45) if risk >= 20 else (70, 55, 25) if risk >= 10 else BG_CELL
            x, y = x0 + j * cw, y0 + i * ch
            d.rectangle([x, y, x + cw - 6, y + ch - 6], fill=fill, outline=c, width=2)
            label(d, x + cw // 2 - 3, y + ch // 2 - 3, "" if v == "." else v, TEXT, 44, True, "mm")
    for j in range(5):
        label(d, x0 + j * cw + cw // 2 - 3, y0 + 5 * ch + 6, str(j + 1), MUTED, 26, True, "ma")
    label(d, x0 + 375, y0 + 5 * ch + 44, "impact", MUTED, 26, True, "ma")
    label(d, 60, 222 + 8, "likelihood", MUTED, 26, True, "la")
    rated = {ln.split()[0]: ln for ln in (s.strip() for s in section("THREE", "HOW ONE THREAT"))}
    risk = rated["risk"].split(None, 1)[1]
    d.rectangle([x0 + 4 * cw, y0, x0 + 5 * cw - 6, y0 + ch - 6], outline=TEXT, width=5)
    label(d, x0 + 5 * cw + 10, y0 + 10, "T:F2 " + risk, TEXT, 26, True, "la")
    dec = section("THREE", "THE DECISIONS")
    header(d, 1260, 330, "THE RESULT: THE DECISIONS", GREEN)
    for k, ln in enumerate(l for l in dec if re.match(r"\s*\w+\s+\d", l)):
        what, n = ln.split()[:2]
        y = 400 + k * 80
        label(d, 1260, y + 22, what, TEXT, 28, True, "lm")
        w = 22 * int(n)
        d.rectangle([1420, y, 1420 + max(w, 6), y + 44], fill=GREEN if what == "mitigate" else AMBER if what != "undecided" else LINE)
        label(d, 1430 + w, y + 22, n, TEXT, 28, True, "lm")
    caption(d, "Risk is likelihood times impact; the top cell holds the internet-boundary threats.", AMBER, 935)


def checks_grid(img, d, cfg):
    """Eight checks on three versions of the model: as built, with the refund flow, and the early draft."""
    rows = section("FOUR", containing("FOUR", "CHECKS ON"))
    cols = rows[0].split()
    header(d, 60, 170, "THE INPUT: THREE VERSIONS OF THE MODEL", CYAN)
    colours = [GREEN, ROSE, AMBER]
    for j, c in enumerate(cols):
        label(d, 1180 + j * 230 + 100, 240, c, colours[j], 30, True, "ma")
    for i, ln in enumerate(rows[1:]):
        name, vals = ln[:42].strip(), ln[42:].split()
        y = 290 + i * 66
        label(d, 60, y + 26, name, TEXT, 28, True, "lm")
        for j, v in enumerate(vals):
            cell(d, 1180 + j * 230, y, 200, 54, v, GREEN if v == "pass" else ROSE, 28)
    after = [ln.strip() for ln in section("FOUR", "AFTER THE CHANGE")]
    fail = next(ln for ln in after if ln.startswith("F9"))
    caption(d, "THE RESULT: +F9 fails one check (%s) before the release." % fail, ROSE, 850)
    label(d, W // 2, 900, after[-1], AMBER, 28, True, "ma")


def review_clock(img, d, cfg):
    """The accepted risk's review day against today, and the four questions."""
    f = {ln.split()[0]: ln.split(None, 1)[1] for ln in (s.strip() for s in section("FIVE", "THE ACCEPTED"))}
    review = int(re.search(r"review on day (\d+)", f["accepted"]).group(1))
    today = int(f["today"].split()[-1])
    overdue = f["overdue"].split("<-")[0].strip()
    header(d, 60, 170, "THE INPUT: " + heading_of("FIVE", "THE ACCEPTED").replace("THE ", "", 1), CYAN)
    x0, x1, y = 160, 1760, 360

    def px(day):
        return x0 + day * (x1 - x0) / (today + 20)
    d.line([x0, y, x1, y], fill=LINE, width=4)
    d.rectangle([px(0), y - 20, px(review), y + 20], fill=(30, 80, 60))
    d.rectangle([px(review), y - 20, px(today), y + 20], fill=(110, 40, 55))
    for day, text, c in ((0, "accepted, day 0", GREEN), (review, "review due, day %d" % review, AMBER), (today, "today, day %d" % today, ROSE)):
        d.line([px(day), y - 50, px(day), y + 50], fill=c, width=4)
        label(d, px(day), y - 60, text, c, 26, True, "md")
    label(d, (px(review) + px(today)) / 2, y + 40, "overdue " + overdue, ROSE, 28, True, "ma")
    header(d, 60, 500, "THE RESULT: THE FOUR QUESTIONS, EVERY CHANGE", GREEN)
    for k, ln in enumerate(section("FIVE", "THE FOUR QUESTIONS")):
        q, part = re.split(r"\s{2,}", ln.strip())
        yy = 570 + k * 78
        cell(d, 60, yy, 760, 60, q, CYAN, 28, anchor="lm")
        label(d, 860, yy + 30, "->", MUTED, 30, True, "lm")
        cell(d, 940, yy, 600, 60, part, GREEN, 28, anchor="lm")
    caption(d, "An accepted risk is a decision with an expiry date.", AMBER, 920)
