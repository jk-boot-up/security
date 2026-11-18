"""Pictures for the privacy engineering video, drawn by the `figure` slide kind.

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

def bar(d, x, y, w, h, share, colour, text):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=LINE, width=2)
    if share > 0:
        d.rounded_rectangle([x, y, x + max(int(w * share), 20), y + h], radius=10, fill=colour)
    label(d, x + w + 20, y + h // 2, text, colour, 30, True, "lm")


def linking(img, d, cfg):
    """Customer 17's row without a name, an outside list with the name, and how many rows match on full and on coarse values."""
    vals = dict((cols(l)[0], cols(l)[1]) for l in section("TWO", "THE VALUES LEFT"))
    coarse = {cols(l)[0]: cols(l)[2].replace("-> ", "") for l in section("TWO", "HOW THE VALUES ARE MADE")}
    groups = table("TWO", "THE GROUPS")
    who = re.search(r"\(customer (\d+)\)", next(l for l in act("TWO") if l.startswith("THE VALUES LEFT"))).group(1)
    name = re.search(r"customer %s, (.+)\)" % who, OUT).group(1)
    header(d, 60, 180, "THE PUBLISHED ROW (NO NAME)", CYAN)
    header(d, 1060, 180, "AN OUTSIDE LIST (WITH THE NAME)", AMBER)
    for i, k in enumerate(vals):
        y = 250 + i * 80
        cell(d, 60, y, 260, 64, k, MUTED, 26)
        cell(d, 340, y, 560, 64, vals[k], CYAN, 30)
        cell(d, 1060, y, 260, 64, k, MUTED, 26)
        cell(d, 1340, y, 520, 64, vals[k], AMBER, 30)
        arrow(d, 905, y + 32, 1055, y + 32, GREEN)
    cell(d, 1060, 490, 260, 64, "name", MUTED, 26)
    cell(d, 1340, 490, 520, 64, name, AMBER, 30)
    full = next(g for g in groups if g["values"] == "full")
    rough = next(g for g in groups if g["values"] == "coarse")
    header(d, 60, 600, "THE RESULT", GREEN)
    panel(d, 60, 670, 860, 200, ROSE, 3)
    label(d, 90, 690, "full values: %s" % ", ".join(vals.values()), TEXT, 28, True)
    label(d, 90, 745, "customers with these values: %s" % full["customer %s's" % who], ROSE, 30, True)
    label(d, 90, 800, "so the row is %s's" % name, ROSE, 30, True)
    panel(d, 1000, 670, 860, 200, GREEN, 3)
    label(d, 1030, 690, "coarse values: %s" % ", ".join(coarse.values()), TEXT, 28, True)
    label(d, 1030, 745, "customers with these values: %s" % rough["customer %s's" % who], GREEN, 30, True)
    label(d, 1030, 800, "the row could be any of them", GREEN, 30, True)
    caption(d, "No name is needed: three ordinary values point at one person.", AMBER, 915)


def purpose_matrix(img, d, cfg):
    """Which purpose needs which personal column; two columns are needed by none."""
    rows = [cols(l) for l in section("ONE", "WHICH PURPOSE")]
    names = dict(cols(l) for l in section("ONE", "THE 5 PURPOSES"))
    head = rows[0]
    header(d, 60, 175, "THE PERSONAL COLUMNS", CYAN)
    header(d, 560, 175, "THE PURPOSES THAT NEED THEM", AMBER)
    for k, code in enumerate(head[1:-1]):
        x = 560 + k * 220
        words, lines = names[code].split(), [""]
        for w in words:
            if lines[-1] and font(SANS_B, 22).getlength(lines[-1] + " " + w) > 210:
                lines.append(w)
            else:
                lines[-1] = (lines[-1] + " " + w).strip()
        for j, ln in enumerate(lines[-2:] if len(lines) <= 2 else [lines[0], " ".join(lines[1:])]):
            fit_sans(d, x + 100, 254 - 26 * (min(len(lines), 2) - 1) + 26 * j, ln, AMBER, 22, 210, "ma", True)
    for i, r in enumerate(rows[1:]):
        y = 285 + i * 68
        none = r[-1] == "0"
        cell(d, 60, y, 460, 56, r[0], ROSE if none else TEXT, 26)
        for k, mark in enumerate(r[1:-1]):
            x = 560 + k * 220
            if mark == "x":
                d.rounded_rectangle([x + 40, y, x + 160, y + 56], radius=10, fill=GREEN)
            else:
                d.rounded_rectangle([x + 40, y, x + 160, y + 56], radius=10, outline=LINE, width=2)
        label(d, 1700, y + 28, "no purpose" if none else r[-1], ROSE if none else MUTED, 26, True, "lm")
    caption(d, section("ONE", "THE RESULT")[0], AMBER, 920)


def groups(img, d, cfg):
    """How many groups the values allow, and the share of customers alone or in a small group."""
    g = section("TWO", "HOW MANY GROUPS")
    t = table("TWO", "THE GROUPS")
    named = pairs("TWO", "THE LINKING ATTACK")
    header(d, 60, 180, "HOW MANY GROUPS THERE CAN BE", CYAN)
    for i, l in enumerate(g[:2]):
        kind, rest = l.split(":", 1)
        cell(d, 60, 245 + i * 80, 180, 64, kind, CYAN if i == 0 else GREEN, 28)
        fit_mono(d, 270, 277 + i * 80, rest.strip(), TEXT, 30, 1500, "lm")
    label(d, 270, 410, g[2], MUTED, 28)
    header(d, 60, 480, "THE CUSTOMERS ALONE IN THEIR GROUP", ROSE)
    for i, row in enumerate(t):
        y = 550 + i * 90
        label(d, 60, y + 30, row["values"], TEXT, 30, True, "lm")
        bar(d, 240, y, 1100, 60, float(row["alone"].rstrip("%")) / 100, ROSE if i == 0 else GREEN, "%s alone" % row["alone"])
    header(d, 60, 750, "THE LINKING ATTACK", AMBER)
    for i, (k, v) in enumerate(named.items()):
        cell(d, 60 + i * 610, 815, 580, 70, "%s: %s" % (k.replace("customers ", ""), v), AMBER, 26)
    caption(d, "Fewer, bigger groups: nobody is alone, and the outside list names nobody.", AMBER, 925)


def pseudonym_steps(img, d, cfg):
    """How a pseudonym is made from the customer number and the key, and what it does not hide."""
    inp = dict((cols(l)[0], cols(l)[1]) for l in section("THREE", "THE INPUT"))
    steps = section("THREE", "HOW THE PSEUDONYM")
    keys = [cols(l) for l in section("THREE", "THE SAME CUSTOMER")]
    tied = section("THREE", "THE LINKING ATTACK ON")[-1]
    header(d, 60, 180, "THE INPUT", CYAN)
    cell(d, 60, 245, 500, 64, inp["text"], CYAN, 30)
    cell(d, 60, 325, 500, 64, "key: " + inp["key"].split(" (")[0], AMBER, 28)
    arrow(d, 580, 317, 690, 317, TEXT)
    header(d, 710, 180, "HMAC-SHA256: 32 BYTES", AMBER)
    fit_mono(d, 710, 255, steps[1].strip(), MUTED, 34, 1150)
    fit_mono(d, 710, 305, steps[2].strip(), MUTED, 34, 1150)
    pseud = steps[3].split(": ")[1]
    d.rounded_rectangle([704, 250, 712 + int(font(MONO_B, 34).getlength(pseud)), 296], radius=8, outline=GREEN, width=3)
    label(d, 710, 365, "the pseudonym: the first 8 bytes", GREEN, 28, True)
    header(d, 60, 450, "THE SAME CUSTOMER, AGAIN AND UNDER ANOTHER KEY", GREEN)
    for i, k in enumerate(keys):
        y = 515 + i * 76
        cell(d, 60, y, 440, 62, k[0], AMBER, 28)
        cell(d, 520, y, 520, 62, k[1], GREEN if i < 2 else ROSE, 30)
        if len(k) > 2:
            label(d, 1070, y + 31, k[2], GREEN if i < 2 else ROSE, 28, True, "lm")
    header(d, 60, 760, "WHAT IT DOES NOT HIDE", ROSE)
    label(d, 60, 825, section("THREE", "THE LINKING ATTACK ON")[0].rstrip(";"), TEXT, 28)
    label(d, 60, 870, tied, ROSE, 30, True)
    caption(d, "A pseudonym replaces the name; it does not hide the person.", AMBER, 930)


def retention(img, d, cfg):
    """The three retention rules with their cut-off dates, and the transactions before and after the job."""
    rules = table("FOUR", "THE RULES")
    b, a = pairs("FOUR", "BEFORE THE JOB"), pairs("FOUR", "AFTER THE JOB")
    untouched = section("FOUR", "WHAT THE JOB DID NOT")[0]
    day = re.search(r"^FOUR\. .* on (\S+)\.$", OUT, re.M).group(1)
    header(d, 60, 180, "THE RULES, RUN ON " + day, CYAN)
    for i, r in enumerate(rules):
        y = 245 + i * 76
        cell(d, 60, y, 400, 62, r["data"], TEXT, 28)
        cell(d, 480, y, 260, 62, r["kept for"], AMBER, 28)
        cell(d, 760, y, 380, 62, "before " + r["removed if before"], ROSE, 28)
    header(d, 1200, 180, "BEFORE THE JOB", ROSE)
    total = int(b["transactions"])
    old, hold = re.match(r"(\d+) \((\d+)", b["transactions older than 7 years"]).groups()
    label(d, 1200, 250, "transactions: %d" % total, TEXT, 30, True)
    label(d, 1200, 300, "older than 7 years: %s" % old, ROSE, 28)
    label(d, 1200, 345, "of them under a legal hold: %s" % hold, AMBER, 28)
    label(d, 1200, 390, "old consents: %s, old notes: %s" % (b["consents older than 2 years"], b["support notes older than 1 year"]), MUTED, 26)
    header(d, 60, 500, "AFTER THE JOB", GREEN)
    left = int(a["transactions left"].split()[0])
    deleted = int(a["transactions deleted"])
    w = 1400
    d.rounded_rectangle([60, 570, 60 + int(w * left / total), 650], radius=10, fill=GREEN)
    d.rounded_rectangle([60 + int(w * left / total), 570, 60 + w, 650], radius=10, outline=ROSE, width=3)
    label(d, 80, 610, "kept %d" % left, INK, 30, True, "lm")
    label(d, 60 + int(w * left / total) + 20, 610, "deleted %d" % deleted, ROSE, 30, True, "lm")
    label(d, 1490, 610, "= %d" % total, TEXT, 30, True, "lm")
    label(d, 60, 680, "transactions left = %s" % a["transactions left"].split("= ")[0] + " = " + a["transactions left"].split("= ")[1], MUTED, 28)
    header(d, 60, 760, "WHAT THE JOB DID NOT TOUCH", ROSE)
    label(d, 60, 825, untouched, ROSE, 32, True)
    caption(d, "A retention job reaches only the copies it knows about.", AMBER, 925)


def erasure(img, d, cfg):
    """The export, what the erasure did, what still mentions the customer, and the scrub."""
    exp = [cols(l) for l in section("FIVE", "THE EXPORT")]
    era = [cols(l) for l in section("FIVE", "THE ERASURE")]
    rem = [cols(l) for l in section("FIVE", "WHAT STILL MENTIONS") if len(cols(l)) == 2]
    scrub = section("FIVE", "AFTER THE SCRUB")[0]
    head = next(l for l in act("FIVE") if l.startswith("THE EXPORT")).rstrip(":")
    header(d, 60, 180, head, CYAN)
    for i, r in enumerate(exp):
        y = 245 + i * 72
        cell(d, 60, y, 300, 60, r[0], TEXT, 28)
        cell(d, 375, y, 400, 60, r[1], CYAN, 28)
    header(d, 820, 180, "THE ERASURE", AMBER)
    for i, r in enumerate(era):
        y = 245 + i * 72
        cell(d, 820, y, 300, 60, r[0], TEXT, 28)
        cell(d, 1135, y, 725, 60, r[1], GREEN if "kept" not in r[1] else AMBER, 28)
    header(d, 820, 560, "WHAT STILL MENTIONS THE CUSTOMER", ROSE)
    for i, r in enumerate(rem):
        y = 625 + i * 72
        cell(d, 820, y, 300, 60, r[0], TEXT, 28)
        cell(d, 1135, y, 725, 60, r[1], ROSE, 28)
    header(d, 60, 745, "AFTER THE SCRUB", GREEN)
    label(d, 60, 810, scrub, GREEN, 32, True)
    caption(d, "Erasure has to search everywhere, not only where the customer's id is.", AMBER, 925)
