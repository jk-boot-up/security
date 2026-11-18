"""Pictures for the ASVS video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
HOW = {"automated": CYAN, "configuration": AMBER, "manual": ROSE}
STATUS = {"met": GREEN, "failed": ROSE, "not applicable": LINE, "manual": AMBER}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def section(name, head):
    """The lines under the act's section whose header starts with `head` (two spaces removed)."""
    found = None
    for ln in act(name):
        if not ln.startswith("  "):
            if found is not None:
                break
            if ln.startswith(head):
                found = []
        elif found is not None:
            found.append(ln[2:])
    return found


def cols(ln):
    return re.split(r"\s{2,}", ln.strip())


def header(d, x, y, text, colour, size=26):
    f = font(SANS_B, size)
    w = int(f.getlength(text)) + 44
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


def caption(d, text, colour=AMBER, y=950):
    label(d, W // 2, y, text, colour, 30, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None, anchor="mm", fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    tx = x + w // 2 if anchor == "mm" else x + 16
    fit_mono(d, tx, y + h // 2, text, text_colour or colour, size, w - 24, anchor)


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def arrow_down(d, x, y1, y2, colour):
    d.line([x, y1, x, y2 - 16], fill=colour, width=4)
    d.polygon([(x, y2), (x - 12, y2 - 18), (x + 12, y2 - 18)], fill=colour)


def requirements():
    """id -> (level, verification, chapter) from act TWO."""
    rows = section("TWO", "THE INPUT")
    return {c[0]: (c[1], c[2], c[3]) for c in (cols(ln) for ln in rows)}


def kv(name, head):
    """A section of `key   value` lines as a dict."""
    return {c[0]: c[1] for c in (cols(ln) for ln in section(name, head)) if len(c) > 1}


# ------------------------------------------------------------------ figures

def one_requirement(img, d, cfg):
    """The core idea on one example: SR-03, from a row of the list to a check, and its verdict before and after."""
    rid = "SR-03"
    level, how, chapter = requirements()[rid]
    check = next(c for c in (cols(ln) for ln in section("THREE", "THE QUESTIONS")) if c[0] == "password-storage")
    before = kv("THREE", "BEFORE")
    after = kv("THREE", "AFTER:")
    header(d, 60, 170, "THE INPUT: ONE ROW OF THE TEAM'S LIST", CYAN)
    for k, (text, c) in enumerate(((rid, TEXT), (level, CYAN), (how, HOW[how]), (chapter, TEXT))):
        cell(d, 60 + k * 300 if k < 3 else 960, 240, 280 if k < 3 else 900, 70, text, c, 30)
    arrow_down(d, 960, 320, 380, MUTED)
    header(d, 60, 390, "THE CHECK IT NAMES, AND ITS QUESTION", AMBER)
    cell(d, 60, 450, 520, 70, check[0], AMBER, 30)
    fit_mono(d, 610, 485, check[1], TEXT, 32, 1250, "lm")
    arrow_down(d, 960, 530, 590, MUTED)
    for k, (title, vals, c) in enumerate((("BEFORE: THE STORE TODAY", before, GREEN),
                                           ("AFTER: " + heading("THREE", "AFTER:").split(": ", 1)[1], after, ROSE))):
        x = 60 + k * 920
        verdict = vals["check"].rsplit(": ", 1)[1]
        panel(d, x, 600, 880, 300, c, 3)
        label(d, x + 30, 620, title.title().replace("Sha", "SHA"), c, 28, True)
        fit_mono(d, x + 30, 690, vals["stored"], TEXT, 26, 820)
        fit_mono(d, x + 30, 750, vals["check"].rsplit(": ", 1)[0], MUTED, 26, 820)
        label(d, x + 440, 810, verdict.upper() if verdict == "pass" else verdict, c, 56, True, "ma")
    caption(d, "A sentence in a list became a test that runs on every build.", AMBER, 940)


def levels(img, d, cfg):
    """Three applications, the rules that raise the level, and what each level asks for."""
    rows = [cols(ln) for ln in section("ONE", "THE INPUT")]
    names, apps = rows[0], rows[1:]
    result = {}
    cur = None
    for ln in section("ONE", "THE RESULT"):
        if not ln.startswith("  "):
            cur, lvl = ln.strip().split(": ")
            result[cur] = [lvl, []]
        else:
            result[cur][1].append(cols(ln))
    header(d, 60, 170, "THE INPUT: THREE APPLICATIONS", CYAN)
    header(d, 1000, 170, "THE RESULT", GREEN)
    for k, app in enumerate(apps):
        y = 240 + k * 150
        panel(d, 60, y, 900, 130, LINE, 2)
        label(d, 90, y + 14, app[0], TEXT, 32, True)
        for j, (n, v) in enumerate(zip(names[1:], app[1:])):
            x = 90 + j * 215
            c = (ROSE if v in ("yes", "HIGH") else AMBER if v == "MEDIUM" else MUTED)
            label(d, x, y + 74, "%s: %s" % (n, v), c, 24, True)
        lvl, reasons = result[app[0]]
        cell(d, 1000, y + 20, 140, 90, lvl, GREEN, 48)
        fit_mono(d, 1160, y + 65, reasons[-1][1], TEXT, 26, 720, "lm")
    header(d, 60, 720, heading("ONE", "WHAT EACH").rstrip(":"), AMBER)
    asks = [cols(ln) for ln in section("ONE", "WHAT EACH")]
    total = max(int(a[1]) for a in asks)
    x0, x1 = 60, 1860
    prev = 0
    for k, a in enumerate(asks):
        lvl, n = a[0], int(a[1])
        xa, xb = x0 + prev / total * (x1 - x0), x0 + n / total * (x1 - x0)
        c = (GREEN, AMBER, ROSE)[k]
        d.rectangle([xa, 790, xb - 6, 860], fill=c)
        label(d, (xa + xb) / 2, 825, "%s: %d" % (lvl, n), INK, 30, True, "mm")
        added = a[2].split(" + ")[-1]
        fit_mono(d, (xa + xb) / 2, 880, added, TEXT, 22, xb - xa - 12, "ma")
        prev = n
    caption(d, "Each level includes the ones below it.", AMBER, 940)


def catalogue(img, d, cfg):
    """Fourteen requirements as tiles, coloured by how each is verified, and one row of the file."""
    reqs = requirements()
    header(d, 60, 170, "THE INPUT: THE TEAM'S %d REQUIREMENTS" % len(reqs), CYAN)
    for k, (rid, (lvl, how, chapter)) in enumerate(reqs.items()):
        x, y = 60 + (k % 7) * 260, 240 + (k // 7) * 150
        d.rounded_rectangle([x, y, x + 240, y + 130], radius=12, fill=BG_CELL, outline=HOW[how], width=4)
        label(d, x + 120, y + 14, rid, TEXT, 30, True, "ma")
        label(d, x + 120, y + 60, lvl, HOW[how], 28, True, "ma")
        label(d, x + 120, y + 96, how, HOW[how], 22, True, "ma")
    for k, ln in enumerate(section("TWO", "HOW THEY ARE")):
        how, n = cols(ln)[0], cols(ln)[1]
        x = 60 + k * 420
        d.rectangle([x, 560, x + 30, 590], fill=HOW[how])
        label(d, x + 44, 556, "%s: %s" % (how, n), HOW[how], 28, True)
    header(d, 60, 630, "THE RESULT: ONE ROW OF " + heading("TWO", "THE RESULT").split(": ")[1], GREEN)
    row = [cols(ln) for ln in section("TWO", "ONE ROW")]
    for k, (name, val) in enumerate(row):
        x, y = 60 + (k % 2) * 920, 700 + (k // 2) * 56
        label(d, x, y, name, MUTED, 26, True)
        fit_mono(d, x + 200, y + 2, val, TEXT, 26, 700)
    caption(d, "A plain file next to the code, reviewed and versioned like code.", AMBER, 940)


def answers(img, d, cfg):
    """The ten answers, and the password check worked out before and after."""
    ans = [cols(ln) for ln in section("THREE", "THE ANSWERS") if len(cols(ln)) == 2]
    total = section("THREE", "THE ANSWERS")[-1].strip()
    header(d, 60, 170, "THE RESULT: " + total.upper(), GREEN)
    for k, (name, verdict) in enumerate(ans):
        y = 240 + k * 66
        c = GREEN if verdict == "pass" else ROSE
        cell(d, 60, y, 400, 54, name, c, 26)
        label(d, 490, y + 27, verdict, c, 28, True, "lm")
    rule = kv("THREE", "HOW THE PASSWORD")
    header(d, 760, 170, "HOW THE PASSWORD CHECK DECIDES", AMBER)
    for k, (key, val) in enumerate(rule.items()):
        label(d, 780, 240 + k * 56, key, MUTED, 26, True)
        fit_mono(d, 1060, 242 + k * 56, val, TEXT, 26, 800)
    for k, (title, head, c) in enumerate((("BEFORE: THE STORE TODAY", "BEFORE", GREEN),
                                          (heading("THREE", "AFTER:"), "AFTER:", ROSE))):
        vals = kv("THREE", head)
        y = 380 + k * 270
        panel(d, 760, y, 1100, 240, c, 3)
        label(d, 790, y + 18, title, c, 26, True)
        fit_mono(d, 790, y + 80, vals["stored"], TEXT, 26, 1040)
        fit_mono(d, 790, y + 140, vals["check"], c, 28, 1040)
    note = section("THREE", "AFTER:")[-1].strip()
    caption(d, note[0].upper() + note[1:] + ".", ROSE, 930)


def coverage(img, d, cfg):
    """The store at L2 as one bar of 14 requirements, and the release gate before and after."""
    groups = []
    for ln in section("FOUR", "THE RESULT"):
        m = re.match(r"(\D+?)\s+(\d+)\s+(.*)", ln.strip())
        groups.append((m.group(1), int(m.group(2)), m.group(3).split()))
    total = sum(g[1] for g in groups)
    header(d, 60, 170, heading("FOUR", "THE RESULT").replace(", REQUIREMENT BY REQUIREMENT", ""), GREEN)
    x0, x1, prev = 60, 1860, 0
    for status, n, ids in groups:
        xa, xb = x0 + prev / total * (x1 - x0), x0 + (prev + n) / total * (x1 - x0)
        d.rectangle([xa, 240, xb - 6, 330], fill=STATUS[status])
        label(d, (xa + xb) / 2, 285, str(n), INK, 40, True, "mm")
        fit_mono(d, (xa + xb) / 2, 342, status, STATUS[status] if status != "not applicable" else MUTED, 26, xb - xa - 10, "ma")
        for j, rid in enumerate(ids):
            fit_mono(d, (xa + xb) / 2, 382 + j * 30, rid, TEXT, 24, xb - xa - 10, "ma")
        prev += n
    gate = kv("FOUR", "AFTER A RELEASE")
    before = heading("FOUR", "BEFORE THE RELEASE").split(": ")[1]
    fixed = kv("FOUR", "AFTER STAFF")
    header(d, 60, 640, "THE RELEASE GATE", AMBER)
    cells = (("BEFORE THE RELEASE", before, GREEN),
             ("AFTER: DEBUG FLAG ON", "failing " + gate["failing"], ROSE),
             ("NEWLY FAILING", gate["newly failing"].split("<-")[0].strip() + ": REFUSED", ROSE),
             ("AFTER THE FIX", "failing %s; people: %s" % (fixed["failing"], fixed["left to people"]), GREEN))
    for k, (title, text, c) in enumerate(cells):
        x = 60 + k * 455
        panel(d, x, 700, 435, 180, c, 3)
        label(d, x + 20, 716, title, c, 24, True)
        words, lines, cur = text.split(), [], ""
        for w in words:
            if cur and len(cur) + len(w) > 22:
                lines.append(cur)
                cur = w
            else:
                cur = (cur + " " + w).strip()
        for j, part in enumerate(lines + [cur]):
            fit_mono(d, x + 20, 766 + j * 36, part, TEXT, 28, 400)
    caption(d, "A known gap is reported; a new failure stops the release.", AMBER, 935)


def two_levels(img, d, cfg):
    """The same store at L2 and L3, the four requirements that need a person, and the old mapping."""
    rows = [cols(ln) for ln in section("FIVE", "THE SAME STORE")]
    names, data = rows[0], rows[1:]
    header(d, 60, 170, "THE SAME STORE, JUDGED AT TWO LEVELS", CYAN)
    top = max(int(r[1]) for r in data)
    for k, r in enumerate(data):
        lvl, applies, met, failed, manual = r[0], int(r[1]), int(r[2]), int(r[3]), int(r[4])
        y = 260 + k * 170
        label(d, 60, y + 40, lvl, TEXT, 44, True, "lm")
        unit = 760 / top
        x = 180
        for n, c, name in ((met, GREEN, "met"), (failed, ROSE, "failed"), (manual, AMBER, "manual")):
            if n:
                d.rectangle([x, y, x + n * unit - 4, y + 80], fill=c)
                label(d, x + n * unit / 2, y + 40, "%s %d" % (name, n), INK, 26, True, "mm")
                x += n * unit
        label(d, 180, y + 96, "%d apply" % applies, MUTED, 24, True)
    header(d, 1000, 170, heading("FIVE", "NEEDS A PERSON").rstrip(":").replace(", AND NO TEST REPLACES IT", ""), ROSE)
    for k, ln in enumerate(section("FIVE", "NEEDS A PERSON")):
        rid, text = ln.strip().split(None, 1)
        y = 240 + k * 110
        cell(d, 1000, y, 160, 80, rid, ROSE, 28)
        words, lines, cur = text.split(), [], ""
        for w in words:
            if cur and len(cur) + len(w) > 32:
                lines.append(cur)
                cur = w
            else:
                cur = (cur + " " + w).strip()
        for j, part in enumerate(lines + [cur]):
            fit_mono(d, 1180, y + 6 + j * 36, part, TEXT, 24, 680)
    mapped = section("FIVE", "MAPPED")[0].strip()
    header(d, 60, 700, heading("FIVE", "MAPPED").rstrip(":"), AMBER)
    fit_mono(d, 60, 770, mapped, AMBER, 30, 1800)
    not_said = [ln.strip() for ln in section("FIVE", "WHAT A PASSING")]
    label(d, 60, 840, "A passing report does not say " + not_said[0].split(":")[0] + ", nor " + not_said[1].split(":")[0] + ".",
          ROSE, 28, True)
    caption(d, "A higher level means more work for people, not only more tests.", AMBER, 935)
