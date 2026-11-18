"""Pictures for the policy-as-code video, drawn by the `figure` slide kind.

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
    """The first section of an act whose header starts with `head`, as lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[0]


def cols(line):
    return re.split(r"\s{2,}", line.strip())


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


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def value_colour(v):
    return GREEN if v == "true" else ROSE if v == "undefined" else TEXT


def rules():
    """The twelve rule labels, in the order of policy/main.rego."""
    out = []
    for ln in section("ONE", "THE RULES"):
        out += cols(ln)
    return out


# ------------------------------------------------------------------ figures

def rule_by_rule(img, d, cfg):
    """One request through every rule: which rules make a message, and the answer allow = false."""
    req = [cols(ln) for ln in section("ONE", "THE REQUEST")]
    answer = section("ONE", "THE ANSWER")
    messages = [m.strip() for m in answer if re.match(r"\s*R\d+ ", m)]
    fired = {m.split()[0] for m in messages}
    header(d, 60, 175, "THE REQUEST: the good one, changed", CYAN)
    for i, (k, v) in enumerate(req):
        label(d, 60 + i * 900, 245, k, MUTED, 26, True)
        fit_mono(d, 60 + i * 900 + 290, 245, v, ROSE, 28, 560)
    header(d, 60, 310, "THE TWELVE DENY RULES (red: it matches)", AMBER)
    for i, r in enumerate(rules()):
        x, y = 60 + (i % 4) * 455, 375 + (i // 4) * 70
        cell(d, x, y, 435, 56, r, ROSE if r.split(":")[0] in fired else GREEN, 24)
    header(d, 60, 600, "THE ANSWER: the set of deny messages", ROSE)
    for i, m in enumerate(messages):
        fit_mono(d, 60, 665 + i * 48, m, TEXT, 28, 1300)
    allow = cols(answer[-1])
    fit_mono(d, 1420, 680, "allow = " + allow[1].split()[0], ROSE, 48, 460)
    fit_mono(d, 1420, 750, allow[1].split(" ", 1)[1], MUTED, 22, 460)
    caption(d, "Every rule that matches adds a message; allow is true only when there are none.", AMBER, 900)


def decisions(img, d, cfg):
    """Thirteen requests: what a reviewer expects next to what the policy said."""
    rows = [cols(ln) for ln in section("ONE", "THE DECISIONS")]
    head, rows = rows[0], rows[1:]
    header(d, 60, 175, "THE DECISIONS", CYAN)
    for x0 in (0, 960):
        label(d, x0 + 70, 240, head[0], MUTED, 24, True)
        label(d, x0 + 510, 240, head[1], MUTED, 24, True, "ma")
        label(d, x0 + 730, 240, head[2], MUTED, 24, True, "ma")
    for i, (name, want, got) in enumerate(rows):
        x0 = 0 if i < 7 else 960
        y = 285 + (i % 7) * 72
        same = want == got
        fit_mono(d, x0 + 70, y + 30, name, TEXT, 24, 330, "lm")
        cell(d, x0 + 410, y, 200, 58, want, MUTED, 22)
        cell(d, x0 + 630, y, 200, 58, got, GREEN if got == "allow" else ROSE, 22)
        label(d, x0 + 880, y + 30, "=" if same else "≠", GREEN if same else ROSE, 34, True, "mm")
    result = section("ONE", "THE RESULT")[0].strip()
    caption(d, result, GREEN, 820)
    label(d, W // 2, 880, "A missing field and an unknown kind are in the table too.", MUTED, 28, False, "ma")


def missing_field(img, d, cfg):
    """How the draft read two requests with a missing field, step by step, and the same trap in naive.rego."""
    before = [cols(ln) for ln in section("TWO", "BEFORE THE FIX")]
    header(d, 60, 175, "BEFORE THE FIX: the draft allows", ROSE)
    for i, (name, want, got) in enumerate(before[1:3]):
        y = 240 + i * 66
        fit_mono(d, 60, y, name, TEXT, 26, 400)
        fit_mono(d, 470, y, "reviewer: " + want, MUTED, 26, 400)
        fit_mono(d, 830, y, "draft: " + got, ROSE, 26, 260)
    header(d, 60, 400, "HOW REGO READS THE MISSING FIELD", AMBER)
    steps = [cols(ln) for ln in section("TWO", "HOW THE EMPTY OBJECT")]
    steps += [cols(ln) for ln in section("TWO", "HOW THE DEPLOYMENT")]
    for i, (expr, v) in enumerate(steps):
        y = 465 + i * 62
        fit_mono(d, 60, y, expr, TEXT, 26, 700)
        cell(d, 800, y - 10, 200, 50, v, value_colour(v), 24)
    label(d, 60, 465 + len(steps) * 62 + 5, "first three: the empty object;  last two: no containers", MUTED, 22)
    trap = section("TWO", "THE SAME TRAP")
    header(d, 1120, 175, "THE SAME TRAP: naive.rego", AMBER)
    fit_mono(d, 1120, 240, trap[0].strip(), MUTED, 22, 760)
    for i, ln in enumerate(trap[1:4]):
        expr, v = cols(ln)
        y = 300 + i * 90
        fit_mono(d, 1120, y, expr, TEXT, 24, 740)
        fit_mono(d, 1120, y + 36, "= " + v, value_colour(v), 26, 740)
    for i, ln in enumerate(trap[4:6]):
        who, verdict = cols(ln)
        y = 600 + i * 80
        fit_mono(d, 1120, y, who, MUTED, 24, 430)
        cell(d, 1560, y - 12, 300, 56, verdict, GREEN if verdict.startswith("deny") else ROSE, 24)
    after = [cols(ln) for ln in section("TWO", "AFTER THE FIX")]
    header(d, 1120, 760, "AFTER THE FIX", GREEN)
    fit_mono(d, 1120, 825, after[-1][0], GREEN, 26, 760)
    caption(d, "Undefined is not false: ask for what is required, and a missing field is denied.", AMBER, 945)


def mutant_grid(img, d, cfg):
    """Each of the twelve rules switched off in turn, and which test suites notice."""
    rows = [cols(ln) for ln in section("THREE", "WHICH MUTANTS")]
    head, rows = rows[0], rows[1:]
    total = cols(section("THREE", "THE RESULT")[0])
    tests = section("THREE", "THE TESTS")[0].strip()
    header(d, 60, 175, "ONE RULE SWITCHED OFF AT A TIME (deny becomes deny_off)", AMBER)
    xs = (760, 1130, 1500)
    for x, h in zip(xs, head[1:]):
        label(d, x + 160, 245, h, MUTED, 26, True, "ma")
    for i, (rule, *marks) in enumerate(rows):
        y = 285 + i * 47
        fit_mono(d, 60, y + 20, rule, TEXT, 24, 660, "lm")
        for x, m in zip(xs, marks):
            ok = m == "yes"
            d.rounded_rectangle([x, y, x + 320, y + 40], radius=8, fill=BG_CELL, outline=GREEN if ok else ROSE, width=3)
            label(d, x + 160, y + 20, "noticed" if ok else "missed", GREEN if ok else ROSE, 22, True, "mm")
    y = 285 + len(rows) * 47 + 10
    label(d, 60, y + 22, total[0], AMBER, 28, True, "lm")
    for x, n in zip(xs, total[1:]):
        label(d, x + 160, y + 22, n, GREEN if n == total[0].split()[-1] else AMBER, 34, True, "mm")
    caption(d, tests + "; a mutant no test notices is a rule nobody tested.", AMBER, 945)


def cluster_and_build(img, d, cfg):
    """The same policy as an admission review, in the build with Conftest, and a new rule rolled out as a warning."""
    req = [cols(ln) for ln in section("FOUR", "THE REQUEST")]
    resp = section("FOUR", "THE RESPONSE")
    header(d, 60, 175, "THE REQUEST: AdmissionReview", CYAN)
    for i, (k, v) in enumerate(req):
        y = 240 + i * 46
        label(d, 60, y, k, MUTED, 22, True)
        fit_mono(d, 200, y, v, TEXT, 22, 640)
    header(d, 60, 400, "THE RESPONSE", ROSE)
    allowed = cols(resp[0])
    fit_mono(d, 60, 465, "allowed: " + allowed[1], ROSE, 32, 780)
    for i, ln in enumerate(r for r in resp[1:] if re.match(r"\s*R\d", r)):
        fit_mono(d, 60, 520 + i * 44, ln.strip(), TEXT, 22, 800)
    fit_mono(d, 60, 620, resp[-1].strip().strip("()"), GREEN, 24, 780)
    files = [cols(ln) for ln in section("FOUR", "THE FILES")]
    says = section("FOUR", "WHAT CONFTEST SAYS")
    header(d, 980, 175, "IN THE BUILD: conftest test", CYAN)
    verdicts = dict(cols(ln) for ln in says if not ln.strip().startswith("exit"))
    for i, (name, image) in enumerate(files):
        y = 240 + i * 80
        v = verdicts[name]
        fit_mono(d, 980, y, name, TEXT, 22, 300)
        fit_mono(d, 980, y + 32, image, MUTED, 20, 520)
        cell(d, 1560, y, 300, 56, v, GREEN if v == "pass" else ROSE, 24)
    exit_line = next(ln.strip() for ln in says if ln.strip().startswith("exit"))
    fit_mono(d, 980, 490, exit_line, ROSE, 28, 880)
    rule = act("FOUR")
    new = next(ln for ln in rule if ln.startswith("A NEW RULE"))
    header(d, 60, 700, new, AMBER, 24)
    warn = section("FOUR", "FIRST, AS A WARNING")[0].strip()
    deny = section("FOUR", "LATER, AS A DENIAL")[0].strip()
    label(d, 60, 775, "first, as a warning", AMBER, 24, True)
    fit_mono(d, 60, 810, warn, AMBER, 24, 820)
    draw.arrow_right(d, 900, 815, 980, MUTED, 4)
    label(d, 1000, 775, "later, as a denial", ROSE, 24, True)
    fit_mono(d, 1000, 810, deny, ROSE, 24, 860)
    caption(d, "One file serves the cluster and the build; a new rule warns before it denies.", AMBER, 940)
