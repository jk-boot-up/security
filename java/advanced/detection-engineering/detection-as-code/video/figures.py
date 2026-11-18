"""Pictures for the detection as code video, drawn by the `figure` slide kind.

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
STATUS = {"covered": GREEN, "partial": AMBER, "GAP": ROSE, "untested": MUTED}


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


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def cols(ln):
    return re.split(r"\s{2,}", ln.strip())


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


def yes_colour(v):
    return GREEN if v == "yes" else MUTED


# ------------------------------------------------------------------ figures

def rule_anatomy(img, d, cfg):
    """The parts of one Sigma rule: its tags, its selections and its condition."""
    lines = section("ONE", "THE RULE")
    name = re.match(r"THE RULE (\S+) \(level (\w+)\)", heading_of("ONE", "THE RULE")).groups()
    header(d, 60, 180, "THE RULE FILE  %s.yml  (level %s)" % (name[0].replace("-", "_"), name[1]), CYAN)
    tags = lines[0].split(": ", 1)[1]
    y = 250
    panel(d, 60, y, 1180, 70, CYAN)
    label(d, 85, y + 35, "tags", MUTED, 26, True, "lm")
    fit_mono(d, 330, y + 35, tags, CYAN, 28, 880, "lm")
    sels = [(lines[i], lines[i + 1].strip()) for i in range(1, len(lines)) if re.match(r"\w+: \S+\|", lines[i])]
    for k, (head, values) in enumerate(sels):
        sel, field = head.split(": ")
        y = 345 + k * 140
        colour = ROSE if sel.startswith("filter") else GREEN
        panel(d, 60, y, 1180, 120, colour)
        label(d, 85, y + 32, sel, colour, 28, True, "lm")
        fit_mono(d, 520, y + 32, field, TEXT, 26, 700, "lm")
        fit_mono(d, 85, y + 85, values, AMBER, 28, 1120, "lm")
    cond = [ln.strip() for ln in lines if ln.strip().startswith("condition:")][0].split(": ", 1)[1]
    cond += " " + lines[lines.index(next(ln for ln in lines if ln.strip().startswith("condition:"))) + 1].strip()
    y = 345 + len(sels) * 140
    panel(d, 60, y, 1180, 110, AMBER)
    label(d, 85, y + 30, "condition", AMBER, 28, True, "lm")
    fit_mono(d, 85, y + 78, cond, TEXT, 26, 1130, "lm")
    x = 1300
    notes = [("tags", "the ATT&CK techniques", CYAN), ("selection", "fields that must match", GREEN),
             ("filter", "a known harmless case", ROSE), ("condition", "how they combine", AMBER)]
    header(d, x, 180, "WHAT EACH PART DOES", TEXT)
    for k, (word, text, colour) in enumerate(notes):
        yy = 260 + k * 150
        label(d, x, yy, word, colour, 32, True)
        label(d, x, yy + 50, text, TEXT, 28)
    caption(d, "A rule is a text file: it can be reviewed, versioned and tested like code.", AMBER, 945)


def condition_steps(img, d, cfg):
    """Three events, each selection answered yes or no, and the alert that comes out."""
    ev = section("ONE", "THE EVENTS")
    events = [ev[i:i + 3] for i in range(0, len(ev), 3)]
    rows = [cols(ln) for ln in section("ONE", "HOW THE CONDITION")]
    head, rows = rows[0], rows[1:]
    header(d, 60, 180, "THE EVENTS", CYAN)
    for k, (start, cmd, truth) in enumerate(events):
        y = 245 + k * 200
        t = truth.split("truth", 1)[1].strip()
        colour = ROSE if t.startswith("attack") else GREEN
        panel(d, 60, y, 860, 180, colour)
        label(d, 85, y + 15, start.split()[0], colour, 30, True)
        fit_mono(d, 130, y + 20, " ".join(start.split()[1:]), TEXT, 24, 770)
        fit_mono(d, 130, y + 70, cmd.strip(), AMBER, 24, 770)
        label(d, 130, y + 120, "truth: " + t, colour, 26, True)
    header(d, 980, 180, "HOW THE CONDITION IS DECIDED", AMBER)
    xs = [960, 1060, 1180, 1300, 1420, 1560]
    for x, h in zip(xs, head):
        label(d, x + 55, 260, h, MUTED, 24, True, "ma" if h != "verdict" else "la")
    for k, r in enumerate(rows):
        y = 300 + k * 200
        for j, (x, v) in enumerate(zip(xs, r)):
            if j == 0:
                label(d, x + 55, y + 45, v, TEXT, 34, True, "ma")
            elif j < 5:
                bad = (j == 3 and v == "yes")
                cell(d, x, y + 40, 110 if j < 5 else 200, 56, v, ROSE if bad else yes_colour(v), 26)
        verdict = r[5]
        colour = GREEN if verdict in ("caught", "rightly quiet") else ROSE
        label(d, 1560, y + 68, verdict, colour, 30, True, "lm")
    caption(d, "parent and child and not filter: event 3 is an attack the rule never sees.", AMBER, 935)


def score_bars(img, d, cfg):
    """Each rule's caught and missed attacks, and its false alarms, with precision and recall worked out."""
    sec = section("TWO", "EACH RULE")
    rows = [cols(ln) for ln in sec[1:] if not ln.strip().startswith("all rules")]
    total = cols(next(ln for ln in sec if ln.strip().startswith("all rules")))
    header(d, 60, 180, "EACH RULE AGAINST THE TRUTH", AMBER)
    label(d, 60, 245, "green: attacks caught   rose: attacks missed   amber: false alarms", MUTED, 24)
    unit = 46
    for k, r in enumerate(rows):
        name, caught, false = r[0], r[1], int(r[2])
        hit, of = [int(n) for n in caught.split(" of ")]
        y = 290 + k * 62
        mono(d, 60, y + 22, name, TEXT, 26, "lm")
        x = 520
        d.rectangle([x, y + 4, x + hit * unit // 2, y + 40], fill=GREEN)
        d.rectangle([x + hit * unit // 2, y + 4, x + of * unit // 2, y + 40], fill=ROSE)
        mono(d, x + of * unit // 2 + 12, y + 22, caught, TEXT, 24, "lm")
        x2 = 1040
        d.rectangle([x2, y + 4, x2 + false * 18, y + 40], fill=AMBER)
        mono(d, x2 + false * 18 + 12, y + 22, "%d false  P %s  R %s" % (false, r[3], r[4]), TEXT, 22, "lm")
    mono(d, 60, 800, "all rules: caught %s, false alarms %s" % (total[1], total[2]), TEXT, 28)
    how = section("TWO", "HOW PRECISION")
    rule = re.search(r"\(([^)]*)\):?$", heading_of("TWO", "HOW PRECISION")).group(1)
    label(d, 60, 855, "worked out for %s:" % rule, CYAN, 26, True)
    fit_mono(d, 480, 855, how[0].strip(), GREEN, 26, 1380)
    fit_mono(d, 480, 900, "P = " + how[2].strip().lstrip("= ") + "   (alerts on its attacks / all)", AMBER, 26, 1380)
    caption(d, "Recall: how many attacks it catches. Precision: how many alerts are real.", AMBER, 960)


def budget_days(img, d, cfg):
    """log-clearing's false alarms day by day against its budget, and a filter dropped from web-server-shell."""
    head = heading_of("THREE", "HOW A BUDGET")
    rule, budget = re.search(r"\((\S+), budget (\d+)\)", head).groups()
    budget = int(budget)
    how = section("THREE", "HOW A BUDGET")
    days = [int(n) for n in how[0].split(":")[1].split()]
    header(d, 60, 180, "%s: FALSE ALARMS A DAY" % rule.upper(), CYAN)
    x0, base, unit = 120, 700, 110
    for k, n in enumerate(days):
        x = x0 + k * 130
        colour = ROSE if n > budget else GREEN
        d.rectangle([x, base - n * unit, x + 90, base], fill=colour)
        label(d, x + 45, base - n * unit - 40, str(n), colour, 30, True, "ma")
        label(d, x + 45, base + 12, "day %d" % (k + 1), MUTED, 24, False, "ma")
    yb = base - budget * unit
    d.line([x0 - 30, yb, x0 + len(days) * 130, yb], fill=AMBER, width=4)
    label(d, x0 + len(days) * 130 + 10, yb, "budget %d" % budget, AMBER, 26, True, "lm")
    mono(d, 60, 780, how[1].strip(), TEXT, 26)
    mono(d, 60, 825, how[2].strip(), ROSE, 26)
    before = section("THREE", "BEFORE THE FILTER")[0].strip()
    after = section("THREE", "AFTER THE FILTER")[0].strip()
    rule2 = re.search(r"\((\S+),", heading_of("THREE", "BEFORE THE FILTER")).group(1)
    fb = int(re.search(r"false alarms (\d+)", before).group(1))
    fa = int(re.search(r"false alarms (\d+)", after).group(1))
    header(d, 1260, 180, "%s: ONE FILTER" % rule2.upper(), AMBER)
    for k, (tag, text, n, colour) in enumerate([("BEFORE THE FILTER IS DROPPED", before, fb, GREEN),
                                                ("AFTER THE FILTER IS DROPPED", after, fa, ROSE)]):
        y = 270 + k * 300
        label(d, 1260, y, tag, colour, 24, True)
        d.rectangle([1260, y + 50, 1260 + n * 7, y + 120], fill=colour)
        label(d, 1260 + n * 7 + 15, y + 85, str(n), colour, 44, True, "lm")
        fit_mono(d, 1260, y + 140, text.split(", ", 1)[1], TEXT, 24, 600)
    caption(d, "A budget turns noise into a number; a filter keeps a rule within it.", AMBER, 935)


def mutants(img, d, cfg):
    """The test cases of web-server-shell, and each small change to the rule with how many cases fail."""
    cases = [ln.strip().split(None, 1) if not ln.strip().startswith("no match") else ["no match", ln.strip()[9:].strip()]
             for ln in section("FOUR", "THE CASES")]
    header(d, 60, 180, "THE CASES (tests beside the rule)", CYAN)
    for k, (expect, name) in enumerate(cases):
        y = 245 + k * 64
        colour = ROSE if expect == "match" else GREEN
        cell(d, 60, y, 180, 52, expect, colour, 24)
        fit_mono(d, 260, y + 26, name, TEXT, 22, 600, "lm")
    rows = [cols(ln) for ln in section("FOUR", "THE MUTANTS OF") if not ln.strip().startswith("no filter")]
    header(d, 960, 180, "THE MUTANTS (one change each)", AMBER)
    for k, (direction, what, fail) in enumerate(rows):
        y = 245 + k * 64
        colour = AMBER if direction == "loosen" else CYAN
        label(d, 960, y + 26, direction, colour, 26, True, "lm")
        fit_mono(d, 1100, y + 26, what, TEXT, 24, 440, "lm")
        killed = not fail.startswith("0 ")
        cell(d, 1560, y, 300, 52, fail + (" : killed" if killed else ""), GREEN if killed else ROSE, 22)
    every = section("FOUR", "EVERY RULE'S MUTANTS")
    fit_mono(d, 60, 840, "every rule: " + every[0].strip() + ";  " + every[1].strip(), GREEN, 28, 1800)
    caption(d, "A mutant that no case notices would show a missing test.", AMBER, 935)


def coverage_grid(img, d, cfg):
    """The ATT&CK techniques, each coloured by what the labelled attacks showed."""
    rows = [cols(ln) for ln in section("FIVE", "THE MATRIX")[1:]]
    header(d, 60, 180, "THE MATRIX: ONE TILE A TECHNIQUE", CYAN)
    for k, r in enumerate(rows):
        tech, attacks, caught, status = r[0], r[1], r[2], r[3]
        rule = r[4] if len(r) > 4 else "-"
        x, y = 60 + (k % 5) * 362, 245 + (k // 5) * 150
        colour = STATUS[status]
        d.rounded_rectangle([x, y, x + 345, y + 135], radius=12, fill=BG_CELL, outline=colour, width=4)
        label(d, x + 15, y + 12, tech, colour, 28, True)
        label(d, x + 330, y + 14, status, colour, 22, True, "ra")
        fit_mono(d, x + 15, y + 60, rule, TEXT, 20, 315)
        label(d, x + 15, y + 95, "attacks %s, caught %s" % (attacks, caught), MUTED, 22)
    how = [cols(ln) for ln in section("FIVE", "HOW THE STATUS")[:4]]
    for k, (status, text, n) in enumerate(how):
        x = 60 + k * 460
        label(d, x, 860, "%s %s" % (n, status), STATUS[status], 28, True)
        label(d, x, 900, text, MUTED, 22)
    caption(d, "A tag is a claim; only the labelled attacks show what is covered.", AMBER, 955)


def rule_to_query(img, d, cfg):
    """The rule's condition, the Lucene query pySigma made, and the counts from Java and OpenSearch."""
    query = section("SIX", "THE QUERY")
    header(d, 60, 180, "THE QUERY pySigma MADE (Lucene)", AMBER)
    panel(d, 60, 240, 1100, 50 + 60 * len(query), AMBER)
    for k, ln in enumerate(query):
        fit_mono(d, 85, 265 + k * 60, ln.strip(), TEXT, 30, 1050)
    conv = section("SIX", "THE CONVERSION")
    y = 340 + 60 * len(query)
    header(d, 60, y - 10, "THE CONVERSION OF EVERY RULE", GREEN)
    fit_mono(d, 60, y + 60, conv[0].strip(), GREEN, 30, 1100)
    for k, ln in enumerate(conv[1:]):
        fit_mono(d, 60, y + 115 + k * 50, ln.strip(), ROSE, 28, 1100)
    rows = [cols(ln) for ln in section("SIX", "THE QUERIES RUN")[1:] if not ln.strip().startswith("*")]
    header(d, 1220, 180, "JAVA AND OPENSEARCH", CYAN)
    label(d, 1600, 245, "java", GREEN, 24, True, "ma")
    label(d, 1720, 245, "opensearch", CYAN, 24, True, "ma")
    for k, r in enumerate(rows):
        yy = 285 + k * 62
        fit_mono(d, 1220, yy + 25, r[0], TEXT, 24, 320, "lm")
        cell(d, 1560, yy, 80, 50, r[1], GREEN, 24)
        cell(d, 1680, yy, 80, 50, r[2], CYAN, 24)
        label(d, 1840, yy + 25, r[3], GREEN if r[3] == "yes" else ROSE, 26, True, "rm")
    fit_mono(d, 60, 870, heading_of("SIX", "THE RESULT"), GREEN, 30, 1800)
    caption(d, "The same rule, two engines, the same events: the conversion is checked, not trusted.", AMBER, 935)
