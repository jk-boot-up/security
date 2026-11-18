"""Pictures for the fuzzing-with-Jazzer video, drawn by the `figure` slide kind.

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


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def cols(line):
    """A table row split at runs of two or more spaces."""
    return re.split(r"\s{2,}", line.strip())


def facts(lines):
    """`key   value` lines as a dict; lines without a key are skipped."""
    return dict(c[:2] for c in (cols(ln) for ln in lines) if len(c) >= 2)


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


def caption(d, text, colour=AMBER, y=950):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def findings(lines):
    """Two-line findings ("Kind" then "  in Place") as (kind, place) pairs."""
    out = []
    for ln in lines:
        s = ln.strip()
        if s.startswith("in "):
            out[-1] = (out[-1][0], s[3:])
        else:
            out.append((s, ""))
    return out


# ------------------------------------------------------------------ figures

def jazzer_loop(img, d, cfg):
    """The core idea: a fuzz target, seeds, Jazzer's loop, and the findings it saves."""
    run = facts(sections("TWO", "THE RUN")[0])
    found = findings(sections("TWO", "THE FINDINGS")[0])
    header(d, 60, 190, "WHAT YOU GIVE IT", CYAN)
    panel(d, 60, 250, 560, 110, CYAN)
    label(d, 85, 270, "a fuzz target", TEXT, 30, True)
    fit_mono(d, 85, 315, run["target"], MUTED, 24, 520)
    panel(d, 60, 380, 560, 150, CYAN)
    label(d, 85, 400, "seed inputs", TEXT, 30, True)
    for i, s in enumerate(run["seeds"].split(", ")):
        fit_mono(d, 85, 445 + i * 38, s, MUTED, 26, 520)
    draw.arrow_right(d, 640, 400, 760, MUTED, 5)
    header(d, 780, 190, "WHAT JAZZER DOES", AMBER)
    steps = ["adds coverage counters to the bytecode", "changes an input a little", "runs the target",
             "keeps it if it reached new code", "saves it if it crashed"]
    for i, s in enumerate(steps):
        y = 260 + i * 66
        cell(d, 780, y, 560, 54, "", LINE)
        label(d, 800, y + 27, "%d. %s" % (i + 1, s), TEXT, 24, True, "lm")
    draw.arrow(d, 1365, 520, 1365, 280, AMBER, 4)
    label(d, 1060, 610, run["runs"].split(",")[0] + " runs", AMBER, 28, True, "ma")
    draw.arrow_right(d, 1385, 400, 1445, MUTED, 5)
    header(d, 1460, 190, "WHAT COMES BACK", ROSE)
    for i, (kind, place) in enumerate(found):
        y = 260 + i * 130
        cell(d, 1460, y, 420, 110, "", ROSE)
        fit_mono(d, 1480, y + 20, kind, ROSE, 22, 380)
        fit_mono(d, 1480, y + 62, place, MUTED, 20, 380)
    panel(d, 60, 690, 1820, 140, LINE)
    label(d, 90, 712, "No hand-written counters:", TEXT, 30, True)
    label(d, 90, 765, "the plain fuzzing project called coverage.hit(...) by hand; Jazzer adds them as it loads the classes.",
          MUTED, 26)
    caption(d, "You write three lines and give it your examples; it finds the inputs nobody thought of.", AMBER, 920)


def examples_pass(img, d, cfg):
    """The six example tests pass; the one-field line nobody tested crashes."""
    rows = [cols(r) for r in sections("ONE", "THE EXAMPLE TESTS")[0]]
    header(d, 60, 180, heading_of("ONE", "THE EXAMPLE TESTS").rstrip(":"), GREEN)
    for i, r in enumerate(rows):
        y = 250 + i * 70
        panel(d, 60, y, 1000, 58, LINE)
        fit_mono(d, 85, y + 29, r[0], TEXT, 24, 420, "lm")
        fit_mono(d, 1040, y + 29, r[1], GREEN, 24, 500, "rm")
    nobody = sections("ONE", "THE INPUT NOBODY")[0][0].strip().strip('"')
    res = cols(sections("ONE", "THE RESULT")[0][0])[1].split("  <-")[0]
    header(d, 1140, 180, "THE INPUT NOBODY TESTED", AMBER)
    fit_mono(d, 1140, 250, '"%s"' % nobody, AMBER, 44, 720)
    label(d, 1140, 330, 'line.split(",") gives:', MUTED, 26)
    parts = nobody.split(",")
    for i in range(3):
        have = i < len(parts)
        cell(d, 1140 + i * 240, 380, 220, 70, parts[i] if have else "missing", GREEN if have else ROSE, 26)
        label(d, 1140 + i * 240 + 110, 460, "fields[%d]" % i, MUTED, 22, True, "ma")
    header(d, 1140, 540, "THE RESULT", ROSE)
    kind, line = re.match(r"(\S+) \((.*)\)", res).groups()
    fit_mono(d, 1140, 610, kind, ROSE, 30, 720)
    fit_mono(d, 1140, 660, "at OrderLine.java " + line, ROSE, 26, 720)
    label(d, 1140, 740, "a refusal would be fine;", MUTED, 28)
    label(d, 1140, 785, "an exception like this is a bug", MUTED, 28)
    caption(d, "Every example passes. The input nobody thought of crashes.", AMBER, 940)


def which_line(img, d, cfg):
    """Jazzer's three findings, and which missing field each line number means."""
    found = findings(sections("TWO", "THE FINDINGS")[0])
    header(d, 60, 180, heading_of("TWO", "THE FINDINGS").rstrip(":"), ROSE)
    for i, (kind, place) in enumerate(found):
        y = 250 + i * 110
        cell(d, 60, y, 820, 90, "", ROSE)
        fit_mono(d, 85, y + 14, kind, ROSE, 26, 770)
        fit_mono(d, 85, y + 52, "in " + place, MUTED, 24, 770)
    header(d, 980, 180, "WHICH LINE IS WHICH", AMBER)
    for i, ln in enumerate(sections("TWO", "WHICH LINE")[0]):
        inp, rest = re.match(r'\s*("[^"]*")\s+(.*)', ln).groups()
        y = 260 + i * 220
        fit_mono(d, 980, y, inp, TEXT, 34, 860)
        parts = inp.strip('"').split(",")
        for k in range(3):
            have = k < len(parts)
            cell(d, 980 + k * 240, y + 60, 220, 64, parts[k] if have else "missing", GREEN if have else ROSE, 26)
        fit_mono(d, 980, y + 150, rest, AMBER, 26, 860)
    named = facts(sections("TWO", "HOW A FINDING")[0])
    panel(d, 60, 620, 820, 230, LINE)
    label(d, 85, 640, "How a finding is named", TEXT, 28, True)
    for i, k in enumerate(("kind", "place", "repeats", "input")):
        label(d, 85, 690 + i * 38, k, CYAN, 24, True)
        fit_mono(d, 220, 690 + i * 38, named[k], MUTED, 22, 640)
    caption(d, "Each line number is a different missing field, so each is a distinct finding.", AMBER, 920)


def seeds_matter(img, d, cfg):
    """Coverage with and without seeds, and why random bytes almost never make a product code."""
    no = facts(sections("THREE", "NO SEED INPUTS")[0])
    yes = facts(sections("THREE", "THE TWO EXAMPLE LINES")[0])
    runs = re.search(r"(\d+) RUNS", heading_of("THREE", "NO SEED INPUTS")).group(1)
    header(d, 60, 180, "COVERAGE POINTS REACHED (%s RUNS)" % runs, CYAN)
    vals = [("no seeds", no, ROSE), ("two example lines", yes, GREEN)]
    top = max(int(v["coverage"].split()[0]) for _, v, _ in vals)
    for i, (name, v, colour) in enumerate(vals):
        n = int(v["coverage"].split()[0])
        h = int(330 * n / top)
        x = 120 + i * 300
        d.rectangle([x, 620 - h, x + 180, 620], fill=colour)
        label(d, x + 90, 620 - h - 50, str(n), colour, 40, True, "ma")
        label(d, x + 90, 640, name, colour, 26, True, "ma")
        label(d, x + 90, 680, "findings " + v["findings"].split()[0], colour, 26, True, "ma")
    odds = facts(sections("THREE", "HOW LIKELY")[0])
    header(d, 800, 180, "HOW LIKELY A RANDOM PRODUCT CODE IS", AMBER)
    for i, k in enumerate(("letters", "dash", "digits")):
        y = 260 + i * 70
        label(d, 800, y, k, CYAN, 28, True)
        fit_mono(d, 960, y + 4, odds[k], TEXT, 26, 900)
    expr, one_in = odds["together"].split(" = ")
    fit_mono(d, 800, 480, expr, TEXT, 28, 1000)
    fit_mono(d, 800, 530, "= " + one_in, AMBER, 44, 1000)
    label(d, 800, 610, "and the fuzzer had only %s runs" % runs, ROSE, 30, True)
    cart = sections("THREE", "WITH ONE MORE SEED")[0]
    seed = cols(cart[0])[1]
    kind = cols(cart[1])[1]
    plain = facts(sections("THREE", "THE CART PARSER")[0])
    panel(d, 60, 740, 1820, 140, LINE)
    label(d, 90, 760, "The cart parser: seeds without a note, findings %s; one more seed with a note field:" % plain["findings"],
          TEXT, 26, True)
    fit_mono(d, 90, 810, seed, GREEN, 26, 900)
    fit_mono(d, 1020, 810, "-> " + kind, ROSE, 26, 840)
    caption(d, "The seeds take the fuzzer past checks it could never guess.", AMBER, 920)


def minimize_steps(img, d, cfg):
    """The minimizer makes a crash shorter, then plainer; the fixed parser refuses each smallest input."""
    steps = [cols(r) for r in sections("FOUR", "HOW THE MINIMIZER")[0]]
    header(d, 60, 180, "MAKING A CRASH SMALL AND PLAIN", AMBER)
    for i, (inp, what) in enumerate(steps):
        y = 250 + i * 62
        colour = TEXT if i == 0 else (CYAN if what.startswith("shorter") else GREEN)
        fit_mono(d, 60, y, inp, colour, 30, 400)
        fit_mono(d, 480, y + 4, what, MUTED if i else ROSE, 26, 520)
    before = [cols(r) for r in sections("FOUR", "BEFORE THE FIX")[0]]
    after = [cols(r) for r in sections("FOUR", "AFTER THE FIX")[0] if r.strip().startswith('"')]
    header(d, 1040, 180, "BEFORE THE FIX", ROSE)
    header(d, 1460, 180, "AFTER THE FIX", GREEN)
    for i, (b, a) in enumerate(zip(before, after)):
        y = 260 + i * 140
        fit_mono(d, 1040, y, b[0], TEXT, 28, 820)
        cell(d, 1040, y + 45, 400, 60, b[1], ROSE, 20)
        cell(d, 1460, y + 45, 400, 60, a[1], GREEN, 20)
    fixed = [cols(r) for r in sections("FOUR", "THE FIXED PARSERS")[0]]
    fit_mono(d, 1040, 700, "same fuzzer, fixed parsers:", MUTED, 24, 820)
    for i, r in enumerate(fixed):
        fit_mono(d, 1040, 740 + i * 40, "%s  %s" % (r[0], r[1]), GREEN, 24, 820)
    caption(d, "Shorter, then plainer, the same crash each time: then it is a regression test.", AMBER, 920)


def sanitizer(img, d, cfg):
    """A sanitizer reports a regular expression injection; a negative quantity is no crash; deep nesting is past the limit."""
    careless = sections("FIVE", "THE SEARCH BOX, CARELESS")[0]
    fixed = facts(sections("FIVE", "THE SEARCH BOX, FIXED")[0])
    kind = cols(careless[0])[1]
    place = careless[1].strip()
    bracket_careless = cols(careless[2])[1]
    header(d, 60, 180, "THE SEARCH BOX, CARELESS", ROSE)
    fit_mono(d, 60, 250, "the customer's words become a pattern", MUTED, 26, 800)
    cell(d, 60, 300, 820, 70, kind, ROSE, 30)
    fit_mono(d, 60, 390, place, MUTED, 24, 820)
    fit_mono(d, 60, 440, 'query "(" -> ' + bracket_careless, ROSE, 26, 820)
    header(d, 1000, 180, "THE SEARCH BOX, FIXED", GREEN)
    fit_mono(d, 1000, 250, "the words are only text", MUTED, 26, 800)
    cell(d, 1000, 300, 820, 70, "findings " + fixed["findings"].split("  <-")[0], GREEN, 30)
    fit_mono(d, 1000, 440, 'query "(" -> ' + fixed['"("'], GREEN, 26, 820)
    cart = sections("FIVE", "THE INPUT")[0][0].strip()
    res = sections("FIVE", "THE RESULT")[0][0].strip().split("  <-")[0]
    header(d, 60, 530, "A CART WITH A QUANTITY OF -5", CYAN)
    fit_mono(d, 60, 600, cart, TEXT, 30, 820)
    cell(d, 60, 650, 300, 64, res, GREEN, 30)
    label(d, 390, 682, "no crash, so no report", AMBER, 28, True, "lm")
    nest = facts(sections("FIVE", "THE NESTING")[0])
    header(d, 1000, 530, "THE NESTING CRASH", AMBER)
    for i, k in enumerate(("limit", "careless", "fixed")):
        label(d, 1000, 600 + i * 55, k, CYAN, 26, True)
        fit_mono(d, 1160, 602 + i * 55, nest[k], ROSE if k == "careless" else TEXT, 24, 720)
    caption(d, "Sanitizers find some vulnerabilities; business rules and long inputs need you.", AMBER, 920)
