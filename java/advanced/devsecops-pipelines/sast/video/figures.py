"""Pictures for the static analysis video, drawn by the `figure` slide kind.

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


def section(name, head):
    """The section of an act whose header starts with `head`, as stripped lines."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln.strip())
    return found[0]


def facts(lines):
    """`key   value` lines as a dict."""
    return dict(re.split(r"\s{2,}", ln, maxsplit=1) for ln in lines if re.search(r"\s{2,}", ln))


def after(line, key):
    """The word after `key: ` in a line."""
    return re.search(re.escape(key) + r":? (\S+?)[,.]?(?:\s|$)", line).group(1)


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


def caption(d, text, colour=AMBER, y=935):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=2)


def tone(answer, good="yes"):
    return GREEN if answer == good else ROSE


# ------------------------------------------------------------------ figures

def cols(line):
    return re.split(r"\s{2,}", line.strip())


def verdict_colour(v):
    return {"found": GREEN, "quiet": GREEN, "pass": GREEN, "missed": ROSE, "flagged": ROSE, "FAIL": ROSE}.get(v, TEXT)


def code_box(d, x, y, w, lines, colours, size=28, step=56):
    panel(d, x, y, w, step * len(lines) + 30)
    for i, (ln, c) in enumerate(zip(lines, colours)):
        fit_mono(d, x + 24, y + 18 + i * step, ln, c, size, w - 48)


def two_readers(img, d, cfg):
    """One safe line, read by a pattern rule and by a taint rule."""
    why = next(k for k in act("FOUR") if k.startswith("WHY LINE"))
    code = [ln for ln in section("FOUR", "WHY LINE") if not ln.startswith("//")]
    row = next(cols(ln) for ln in section("TWO", "THE 8 SQL") if "sql-parsed" in ln)
    rule = facts(section("ONE", "THE RULE"))
    header(d, 60, 180, "THE INPUT: A NUMBER PARSED FROM THE REQUEST (" + why.split(" (")[1].rstrip("):") + ")", CYAN)
    code_box(d, 60, 240, 1800, code, [TEXT, TEXT], 32, 60)
    for x, title, verdict, lines in ((60, "THE PATTERN RULE", row[1], ["sees: executeQuery( ... + n )", "a query built with +", "it cannot see where n came from"]),
                                     (980, "THE TAINT RULE", row[2], ["source: " + rule["source"], "sanitizer on the way:", first_sanitizer(rule)])):
        colour = verdict_colour(verdict)
        header(d, x, 430, title, colour)
        panel(d, x, 490, 880, 260, colour)
        for i, t in enumerate(lines):
            fit_mono(d, x + 30, 520 + i * 55, t, TEXT, 28, 820)
        cell(d, x + 280, 770, 320, 64, verdict, colour, 34)
    caption(d, "The pattern looks at the shape; the taint rule follows the data.", AMBER, 900)


def first_sanitizer(rule):
    return rule["sanitizer"].split(", ")[0]


def taint_path(img, d, cfg):
    """Three lines of code, the rule's parts, and the taint followed from source to sink."""
    head = next(k for k in act("ONE") if k.startswith("THE CODE"))
    code = section("ONE", "THE CODE")
    rule = facts(section("ONE", "THE RULE"))
    steps = [cols(ln) for ln in section("ONE", "HOW THE TAINT")]
    reported = section("ONE", "WHAT SEMGREP")[0]
    first = int(re.search(r"lines (\d+)", head).group(1))
    header(d, 60, 180, "THE INPUT: " + head.split(" (")[1].rstrip("):"), CYAN)
    colours = [CYAN, AMBER, ROSE]
    for i, ln in enumerate(code):
        y = 250 + i * 110
        label(d, 60, y + 32, str(first + i), MUTED, 28, True, "lm")
        cell(d, 130, y, 1180, 64, ln, colours[i], 24)
        what, why = steps[i][1], steps[i][2]
        label(d, 130, y + 72, what + ": " + why, colours[i], 24, True)
        if i < 2:
            draw.arrow(d, 720, y + 66, 720, y + 108, MUTED, 3)
    header(d, 1380, 180, "THE RULE sql-taint", AMBER)
    for i, k in enumerate(("source", "sink", "sanitizer")):
        y = 250 + i * 120
        label(d, 1380, y, k, MUTED, 26, True)
        for j, v in enumerate(rule[k].split(", ")):
            fit_mono(d, 1380, y + 36 + j * 36, v, (CYAN, ROSE, GREEN)[i], 26, 480)
    header(d, 60, 640, "THE RESULT: WHAT SEMGREP REPORTS", GREEN)
    fit_mono(d, 60, 710, reported, GREEN, 34, 1800)
    caption(d, "Source, then sink, with no sanitizer between: a finding.", AMBER, 900)


def pattern_vs_taint(img, d, cfg):
    """The eight SQL lines with each rule's verdict, and recall and precision."""
    sec = section("TWO", "THE 8 SQL")
    rows = [cols(ln) for ln in sec[1:]]
    score = section("TWO", "HOW THE SCORE")
    header(d, 60, 175, "THE 8 SQL LINES (the truth, from the markers)", CYAN)
    label(d, 920, 240, "pattern", MUTED, 26, True, "ma")
    label(d, 1180, 240, "taint", MUTED, 26, True, "ma")
    for i, (tag, p, t) in enumerate(rows):
        y = 280 + i * 62
        fit_mono(d, 60, y + 26, tag, ROSE if tag.startswith("VULN") else GREEN, 26, 680)
        cell(d, 810, y, 220, 52, p, verdict_colour(p), 26)
        cell(d, 1070, y, 220, 52, t, verdict_colour(t), 26)
    header(d, 1360, 175, "HOW THE SCORE IS MADE", AMBER)
    for i, ln in enumerate(score[1:]):
        name, p, t = cols(ln)
        y = 270 + i * 150
        label(d, 1360, y, name, TEXT, 26, True)
        fit_mono(d, 1360, y + 45, "pattern " + p, MUTED, 26, 520)
        fit_mono(d, 1360, y + 85, "taint   " + t, MUTED, 26, 520)
    caption(d, "Found and quiet are right; missed and flagged are wrong.", AMBER, 900)


def class_grid(img, d, cfg):
    """The seven findings matched with the marker on their line, then the score by class."""
    found = [cols(ln) for ln in section("THREE", "WHAT THEY REPORT")]
    by = [cols(ln) for ln in section("THREE", "BY CLASS")[1:]]
    header(d, 60, 175, "WHAT THE TAINT RULES REPORT, AND THE TRUTH ON THAT LINE", CYAN)
    for i, (rule, where, truth) in enumerate(found):
        y = 240 + i * 58
        colour = GREEN if truth.startswith("VULN") else ROSE
        fit_mono(d, 60, y + 24, rule, TEXT, 26, 300)
        fit_mono(d, 380, y + 24, where, MUTED, 26, 480)
        cell(d, 900, y, 520, 48, truth, colour, 24)
        label(d, 1450, y + 24, "real" if colour == GREEN else "false alarm", colour, 26, True, "lm")
    header(d, 60, 660, "BY CLASS: FLAWS FOUND, SAFE LINES FLAGGED", AMBER)
    for i, (kind, f, s) in enumerate(by):
        x = 60 + i * 365
        a, b = [int(v) for v in f.split(" of ")]
        cell(d, x, 720, 340, 56, kind, TEXT, 28)
        label(d, x + 170, 790, "found " + f, GREEN if a == b else AMBER, 26, True, "ma")
        label(d, x + 170, 830, "flagged " + s, ROSE if not s.startswith("0") else GREEN, 26, True, "ma")
    caption(d, "Six of seven flaws found; one safe line flagged.", AMBER, 910)


def test_lines(img, d, cfg):
    """Expected against reported lines per rule, before and after the sanitizer is removed."""
    before = [cols(ln) for ln in section("FOUR", "BEFORE THE CHANGE") if re.match(r"\S+\s{2,}(pass|FAIL)", ln)]
    after = {c[0]: c for c in (cols(ln) for ln in section("FOUR", "AFTER THE CHANGE") if re.match(r"\S+\s{2,}(pass|FAIL)", ln))}
    change = section("FOUR", "THE CHANGE")[0]
    totals = [section("FOUR", "BEFORE THE CHANGE")[-1].split(";")[0], section("FOUR", "AFTER THE CHANGE")[-1]]
    for k, (title, rows, colour, y0) in enumerate((("BEFORE THE CHANGE", before, GREEN, 175), ("AFTER THE CHANGE", [after.get(r[0], r) for r in before], ROSE, 560))):
        header(d, 60, y0, title + ": " + totals[k], colour)
        for i, (rule, status, lines) in enumerate(rows):
            y = y0 + 60 + i * 56
            exp, rep = re.match(r"expected (.*), reported (.*)", lines).groups()
            fit_mono(d, 60, y + 24, rule, TEXT, 26, 300, "lm")
            cell(d, 380, y, 140, 48, status, verdict_colour(status), 26)
            label(d, 560, y + 24, "expected", MUTED, 24, True, "lm")
            fit_mono(d, 700, y + 24, exp, TEXT, 26, 300, "lm")
            label(d, 1020, y + 24, "reported", MUTED, 24, True, "lm")
            extra = [n for n in rep.split() if n not in exp.split()]
            fit_mono(d, 1160, y + 24, rep, ROSE if extra else TEXT, 26, 300, "lm")
            if extra:
                label(d, 1480, y + 24, "line %s: marked ok" % extra[0], ROSE, 26, True, "lm")
    label(d, 60, 495, "the change: " + change, AMBER, 28, True)
    caption(d, "A broken rule fails its own test before it reaches a pipeline.", AMBER, 935)


def triage_flow(img, d, cfg):
    """Seven findings, a suppression with a rule, a reason and an owner, and what the build does."""
    before = section("FIVE", "BEFORE THE SUPPRESSION")[0]
    sup = facts(section("FIVE", "THE SUPPRESSION"))
    after = section("FIVE", "AFTER THE SUPPRESSION")
    bad = section("FIVE", "A SECOND SUPPRESSION")[1]
    clean = section("FIVE", "A FILE WITH NO FLAW")[0]
    header(d, 60, 175, "BEFORE THE SUPPRESSION", ROSE)
    panel(d, 60, 240, 560, 260, ROSE)
    m = re.match(r"findings (\d+) \((.*)\), exit code (\d+): (.*)", before)
    label(d, 340, 300, m.group(1) + " findings", ROSE, 44, True, "ma")
    label(d, 340, 370, m.group(2), TEXT, 28, True, "ma")
    label(d, 340, 420, "exit code " + m.group(3) + ": " + m.group(4), ROSE, 26, True, "ma")
    draw.arrow_right(d, 640, 370, 700, MUTED, 4)
    header(d, 720, 175, "THE SUPPRESSION", AMBER)
    for i, k in enumerate(("rule", "why", "owner")):
        y = 240 + i * 90
        label(d, 720, y + 30, k, MUTED, 26, True, "lm")
        cell(d, 840, y, 620, 64, sup[k], AMBER, 22)
    draw.arrow_right(d, 1480, 370, 1540, MUTED, 4)
    header(d, 1560, 175, "AFTER", GREEN)
    m2 = re.match(r"findings (\d+), exit code (\d+): (.*)", after[0])
    panel(d, 1560, 240, 300, 260, GREEN)
    label(d, 1710, 300, m2.group(1) + " findings", GREEN, 40, True, "ma")
    label(d, 1710, 370, "exit code " + m2.group(2), ROSE, 28, True, "ma")
    label(d, 1710, 420, "the real flaws", MUTED, 24, True, "ma")
    header(d, 60, 560, "A SECOND SUPPRESSION, WITH NO REASON", ROSE)
    fit_mono(d, 60, 630, bad, ROSE, 28, 1800)
    header(d, 60, 710, "A FILE WITH NO FLAW", GREEN)
    fit_mono(d, 60, 780, clean, GREEN, 28, 1800)
    caption(d, "Every suppression says which rule, why, and who decided.", AMBER, 900)
