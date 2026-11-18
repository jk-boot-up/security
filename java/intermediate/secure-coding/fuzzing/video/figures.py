"""Pictures for the fuzzing video, drawn by the `figure` slide kind.

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


def quoted(line):
    """The first "..." input on a line, as printed (quotes included)."""
    return re.search(r'"[^ ]*"(?: \d+ chars)?', line).group(0)


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


def result_colour(text):
    return ROSE if ("Exception" in text or "Error" in text or "crash" in text) else GREEN


# ------------------------------------------------------------------ figures

def fuzz_loop(img, d, cfg):
    """The core idea on one real run: a seed, one change, the parser, and a crash nobody wrote a test for."""
    crash = sections("TWO", "THE CRASHES, ORDER LINES")[0]
    run = crash[0].split()[1]
    kind = cols(crash[0])[1]
    where = crash[1].strip()
    found = crash[2].strip().strip('"')
    seeds = re.findall(r'"([^"]+)"', heading_of("TWO", "THE FIRST RUNS"))
    # the seed this input came from, and the one character the mutation removed
    seed, at = next((s, i) for s in seeds for i in range(len(s)) if s[:i] + s[i + 1:] == found)
    runs = re.search(r"\((\d+) runs", heading_of("TWO", "THE CRASHES, ORDER LINES")).group(1)
    header(d, 60, 190, "1. A SEED, A REAL INPUT", CYAN)
    f = font(MONO_B, 46)
    x = 60
    for i, ch in enumerate(seed):
        colour = ROSE if i == at else TEXT
        d.text((x, 270), ch, font=f, fill=colour)
        x += f.getlength(ch)
    label(d, 60 + f.getlength(seed[:at]) + f.getlength(seed[at]) / 2, 330, "delete", ROSE, 24, True, "ma")
    draw.arrow(d, 300, 380, 300, 450, MUTED, 5)
    header(d, 60, 460, "2. AFTER ONE CHANGE (run %s)" % run, AMBER)
    d.text((60, 540), found, font=f, fill=AMBER)
    draw.arrow_right(d, 640, 565, 820, MUTED, 5)
    panel(d, 840, 490, 360, 150, CYAN, 3)
    label(d, 1020, 530, "the parser", TEXT, 32, True, "ma")
    label(d, 1020, 580, "OrderLine", MUTED, 26, False, "ma")
    outcomes = [("parsed", "a valid order", GREEN), ("refused", "ParseException: expected", GREEN),
                ("crash", kind, ROSE)]
    for i, (name, note, colour) in enumerate(outcomes):
        y = 210 + i * 150
        cell(d, 1320, y, 540, 110, "", colour if name == "crash" else LINE)
        label(d, 1350, y + 18, name, colour, 34, True)
        fit_mono(d, 1350, y + 66, note, colour if name == "crash" else MUTED, 24, 490)
    draw.arrow_diag_up(d, 1210, 560, 1310, 520, ROSE, 5)
    fit_mono(d, 1320, 680, where, ROSE, 24, 540)
    panel(d, 60, 720, 1800, 150, LINE)
    label(d, 90, 745, "3. Repeat", TEXT, 32, True)
    label(d, 90, 800, "%s runs, each from a seed with one or two random changes; every crash is kept." % runs, MUTED, 28)
    caption(d, "A refusal is the parser doing its job. A crash is a bug nobody wrote a test for.", AMBER, 920)


def examples_pass(img, d, cfg):
    """The six example tests pass; the one-field line nobody tested crashes."""
    rows = [cols(r) for r in sections("ONE", "THE EXAMPLE TESTS")[0]]
    header(d, 60, 180, heading_of("ONE", "THE EXAMPLE TESTS").rstrip(":"), GREEN)
    for i, r in enumerate(rows):
        y = 250 + i * 70
        panel(d, 60, y, 1000, 58, LINE)
        label(d, 80, y + 29, r[0], MUTED, 24, True, "lm")
        fit_mono(d, 170, y + 29, r[1], TEXT, 22, 520, "lm")
        fit_mono(d, 1040, y + 29, r[2], GREEN, 22, 330, "rm")
    nobody = quoted(sections("ONE", "THE INPUT NOBODY")[0][0]).strip('"')
    res = dict((c[0], c[1]) for c in (cols(r) for r in sections("ONE", "THE RESULT")[0]))
    header(d, 1140, 180, "THE INPUT NOBODY TESTED", AMBER)
    fit_mono(d, 1140, 250, '"%s"' % nobody, AMBER, 44, 720)
    label(d, 1140, 330, 'line.split(",") gives:', MUTED, 26)
    parts = nobody.split(",")
    for i in range(3):
        have = i < len(parts)
        cell(d, 1140 + i * 240, 380, 220, 70, parts[i] if have else "missing", GREEN if have else ROSE, 26)
        label(d, 1140 + i * 240 + 110, 460, "fields[%d]" % i, MUTED, 22, True, "ma")
    header(d, 1140, 540, "THE RESULT", ROSE)
    fit_mono(d, 1140, 610, res["crash"], ROSE, 30, 720)
    fit_mono(d, 1140, 660, "in " + res["in"].split("  <-")[0], ROSE, 26, 720)
    label(d, 1140, 740, "a refusal would be fine;", MUTED, 28)
    label(d, 1140, 785, "an exception like this is a bug", MUTED, 28)
    caption(d, "Every example passes. The input nobody thought of crashes.", AMBER, 940)


def first_runs(img, d, cfg):
    """How the first inputs are made, and how a quantity overflows the int total."""
    header(d, 60, 180, "THE FIRST RUNS: BEFORE AND AFTER THE CHANGE", CYAN)
    rows = [cols(r) for r in sections("TWO", "THE FIRST RUNS")[0]]
    seeds = re.findall(r'"([^"]+)"', heading_of("TWO", "THE FIRST RUNS"))
    label(d, 60, 240, "run   change", MUTED, 24, True)
    label(d, 470, 240, "input after the change", MUTED, 24, True)
    for i, (run, how, inp, res) in enumerate(rows):
        y = 280 + i * 54
        fit_mono(d, 60, y, run, TEXT, 26, 60)
        fit_mono(d, 120, y, how, AMBER, 26, 330)
        fit_mono(d, 470, y, inp, TEXT, 26, 640)
        fit_mono(d, 1120, y, res, GREEN, 26, 150)
    label(d, 60, 620, "seeds: " + ", ".join(seeds), MUTED, 24)
    steps = dict((c[0], c[1]) for c in (cols(r) for r in sections("TWO", "HOW THE OVERFLOW")[0]))
    header(d, 1300, 180, "HOW THE OVERFLOW IS MADE", ROSE)
    y = 260
    for k in ("exact", "low 32 bits", "as an int"):
        label(d, 1300, y, k, MUTED, 26, True)
        expr, value = steps[k].rsplit(" = ", 1)
        fit_mono(d, 1300, y + 40, expr, TEXT, 24, 580)
        fit_mono(d, 1300, y + 76, "= " + value, AMBER if k != "as an int" else ROSE, 30, 580)
        y += 140
    fit_mono(d, 1300, 690, "Money: " + steps["Money"].split("  <-")[0], ROSE, 24, 580)
    crashes = [r.strip() for r in sections("TWO", "THE CRASHES, ORDER LINES")[0]]
    found = "; ".join("%s %s" % (crashes[i].split()[0], crashes[i].split()[1]) for i in range(0, len(crashes), 3))
    panel(d, 60, 760, 1800, 110, LINE)
    label(d, 90, 785, "found in the order lines: " + found, TEXT, 30, True)
    label(d, 90, 830, "nobody guessed these inputs; random small changes reached them", MUTED, 26)
    caption(d, "An int keeps only 32 bits, so a big total wraps round to a negative one.", AMBER, 920)


def letter_by_letter(img, d, cfg):
    """Coverage guidance keeps each input that matches one more letter of "note"."""
    kept = [cols(r) for r in sections("THREE", "GUIDED, THE INPUTS KEPT")[0]]
    letters = [k for k in kept if "matched" in k[2]]
    g = sections("THREE", "GUIDED, ")[1]
    blind = dict(cols(r) for r in sections("THREE", "BLIND")[0])
    branches_g = re.search(r"branches\s+(\d+)", g[0]).group(1)
    found_run = re.search(r"run (\d+)", g[1]).group(1)
    found_input = quoted(g[2])
    header(d, 60, 180, "GUIDED: EACH INPUT KEPT MATCHES ONE MORE LETTER", GREEN)
    for i, (run, inp, why) in enumerate(letters):
        y = 260 + i * 105
        x = 60 + i * 60
        fit_mono(d, x, y, "run " + run, MUTED, 24, 200)
        cell(d, x + 190, y - 12, 560, 60, inp, GREEN, 24)
        label(d, x + 770, y + 18, why, GREEN, 26, True, "lm")
    y = 260 + len(letters) * 105
    x = 60 + len(letters) * 60
    fit_mono(d, x, y, "run " + found_run, ROSE, 24, 200)
    cell(d, x + 190, y - 12, 560, 60, found_input, ROSE, 24)
    label(d, x + 770, y + 18, "# : the bug", ROSE, 26, True, "lm")
    header(d, 1440, 180, "BRANCHES REACHED", AMBER)
    top = max(int(blind["branches"]), int(branches_g))
    for i, (name, n, colour) in enumerate((("blind", blind["branches"], MUTED), ("guided", branches_g, GREEN))):
        h = int(300 * int(n) / top)
        x = 1480 + i * 200
        d.rectangle([x, 640 - h, x + 140, 640], fill=colour)
        label(d, x + 70, 640 - h - 50, n, colour, 36, True, "ma")
        label(d, x + 70, 660, name, colour, 28, True, "ma")
    label(d, 1580, 720, "blind, bug found: " + blind["found"], MUTED, 26, True, "ma")
    caption(d, "Each letter matched is a new branch, so the fuzzer keeps the input and builds on it.", AMBER, 940)


def shrink_steps(img, d, cfg):
    """Delta debugging shrinks the gift-message crash; the fixed parsers refuse every smallest input."""
    steps = [cols(r) for r in sections("FOUR", "HOW DELTA DEBUGGING")[0]]
    header(d, 60, 180, "SHRINKING THE CRASH, STEP BY STEP", AMBER)
    longest = int(steps[0][0])
    for i, s in enumerate(steps):
        n, text = int(s[0]), s[1]
        y = 250 + i * 62
        w = int(560 * n / longest)
        last = i == len(steps) - 1
        d.rectangle([60, y, 60 + w, y + 46], fill=GREEN if last else LINE)
        label(d, 70, y + 23, str(n), INK if last else TEXT, 24, True, "lm")
        fit_mono(d, 640, y + 23, text, GREEN if last else TEXT, 24, 400, "lm")
    before = [cols(r) for r in sections("FOUR", "BEFORE THE FIX")[0]]
    after = [cols(r) for r in sections("FOUR", "AFTER THE FIX")[0] if r.strip().startswith('"')]
    header(d, 1100, 180, "BEFORE THE FIX", ROSE)
    header(d, 1500, 180, "AFTER THE FIX", GREEN)
    for i, (b, a) in enumerate(zip(before, after)):
        y = 260 + i * 130
        fit_mono(d, 1100, y, b[0], TEXT, 24, 760)
        cell(d, 1100, y + 40, 380, 56, b[1], ROSE, 22)
        cell(d, 1500, y + 40, 380, 56, a[1], GREEN, 20)
    fixed = [cols(r) for r in sections("FOUR", "THE FIXED PARSERS")[0]]
    fit_mono(d, 60, 800, "fixed parsers, same fuzzer: " + "; ".join("%s %s" % (r[0], r[1]) for r in fixed), GREEN, 26, 1800)
    caption(d, "Each smallest input becomes a regression test, so the bug cannot come back.", AMBER, 940)


def no_report(img, d, cfg):
    """A cart with quantity -5 parses: no crash, so the fuzzer says nothing."""
    cart = sections("FIVE", "THE INPUT")[0][0].strip()
    res = sections("FIVE", "THE RESULT")[0][0].strip().split("  <-")[0]
    header(d, 60, 190, "THE INPUT", CYAN)
    fit_mono(d, 60, 270, cart, TEXT, 40, 900)
    draw.arrow_right(d, 980, 295, 1120, MUTED, 5)
    header(d, 1160, 190, "THE RESULT", GREEN)
    cell(d, 1160, 250, 300, 90, res, GREEN, 40)
    label(d, 1500, 295, "no crash, so no report", AMBER, 30, True, "lm")
    cannot = [r.strip() for r in sections("FIVE", "WHAT A FUZZER CANNOT")[0]]
    header(d, 60, 420, "WHAT A FUZZER CANNOT KNOW", AMBER)
    for i, t in enumerate(cannot):
        label(d, 60, 490 + i * 50, t, TEXT, 30)
    costs = [cols(r) for r in sections("FIVE", "WHAT IT COSTS")[0]]
    header(d, 60, 640, "WHAT IT COSTS", MUTED)
    for i, (k, v) in enumerate(costs):
        label(d, 60, 710 + i * 48, k, CYAN, 28, True)
        label(d, 300, 710 + i * 48, v, TEXT, 28)
    caption(d, "A fuzzer finds crashes and broken properties, not wrong business decisions.", AMBER, 940)
