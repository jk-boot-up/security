"""Pictures for the failing-closed-with-Spring-Boot video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def sections(name, head):
    """Every section of an act whose header starts with `head`, as lists of lines (stripped)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln.strip())
    return found


def first(name, head):
    return sections(name, head)[0]


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


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def verdict_colour(text):
    return ROSE if text.startswith("APPROVED") and "<-" in text else GREEN if text.startswith("APPROVED") else AMBER


# ------------------------------------------------------------------ figures

def wrap_px(text, max_w, size, sans=False):
    f = font(SANS_B if sans else MONO_B, size)
    out, cur = [], ""
    for word in text.split(" "):
        trial = (cur + " " + word).strip()
        if cur and f.getlength(trial) > max_w:
            out.append(cur)
            cur = word
        else:
            cur = trial
    return out + [cur]



def numbered(name, head):
    """The rows of a numbered section ("1  ..."), as (number, rest) pairs."""
    return [tuple(r.split(None, 1)) for r in first(name, head)]


def open_vs_closed(img, d, cfg):
    """One payment and a dropped connection, through the version that starts from yes and the one that starts from no."""
    payment = first("ONE", "THE PAYMENT")[0]
    checks = dict(numbered("ONE", "THE FRAUD CHECK"))
    drop = next(k for k, v in checks.items() if "drops" in v)
    naive = dict(numbered("ONE", "THE RESULTS"))[drop].split("  <-")[0]
    inputs = dict(numbered("TWO", "THE INPUTS"))
    row = next(k for k, v in inputs.items() if "drops" in v)
    safe = dict(numbered("TWO", "THE RESULTS"))[row]
    header(d, 660, 205, "THE PAYMENT", AMBER)
    cell(d, 560, 260, 800, 64, payment, AMBER, 28)
    lanes = [("STARTS FROM YES (OpenPaymentService)", ROSE, "decision = yes", "HTTP " + naive, "an error became a yes"),
             ("STARTS FROM NO (ClosedPaymentService)", GREEN, "decision = no", "HTTP " + safe, "an error stays a no")]
    for i, (title, c, start, result, note) in enumerate(lanes):
        x = 80 + i * 900
        header(d, x, 370, title, c)
        panel(d, x, 430, 860, 420, c, 3)
        steps = [(start, TEXT), ("fraud check: " + checks[drop], AMBER), (result, c)]
        for j, (text, tc) in enumerate(steps):
            y = 460 + j * 130
            cell(d, x + 40, y, 780, 80, text, tc, 28)
            if j < 2:
                draw.arrow(d, x + 430, y + 84, x + 430, y + 126, MUTED, 4)
        label(d, x + 430, 870, note, c, 28, True, "ma")
    caption(d, "The same failure: one version turns it into a yes, the other into a no.", AMBER, 935)


def open_results(img, d, cfg):
    """One risky payment against five fraud-check behaviours, and the bank's status and decision for each."""
    payment = first("ONE", "THE PAYMENT")[0]
    checks = numbered("ONE", "THE FRAUD CHECK")
    results = dict(numbered("ONE", "THE RESULTS"))
    budget = heading_value("ONE", "THE FRAUD CHECK")
    header(d, 80, 205, "THE PAYMENT", AMBER)
    panel(d, 80, 270, 460, 560, AMBER, 3)
    mono(d, 310, 500, payment.split(" at ")[0], AMBER, 44, "mm")
    label(d, 310, 570, "at " + payment.split(" at ")[1], AMBER, 26, True, "mm")
    header(d, 620, 205, "THE FRAUD CHECK " + budget, CYAN)
    header(d, 1380, 205, "THE RESULT", GREEN)
    for i, (n, what) in enumerate(checks):
        y = 270 + i * 120
        broken = not what.startswith("works")
        draw.arrow_right(d, 550, y + 40, 610, MUTED, 4)
        cell(d, 620, y, 700, 80, n + "  " + what, AMBER if broken else TEXT, 26)
        draw.arrow_right(d, 1325, y + 40, 1370, MUTED, 4)
        res, _, mark = results[n].partition("  <- ")
        c = ROSE if "APPROVED" in res else GREEN
        cell(d, 1380, y, 460, 80, "HTTP " + res, c, 28)
    caption(d, "Four broken checks, four approvals: the empty catch kept the yes.", AMBER, 935)


def heading_value(name, head):
    """The part in brackets of a section header, e.g. "(budget 500 ms)"."""
    ln = next(l for l in act(name) if l.startswith(head))
    return ln[ln.index("("):ln.rindex(")") + 1]


def closed_results(img, d, cfg):
    """Six payments, the fraud check for each, the status and decision, and the time budget worked out."""
    inputs = numbered("TWO", "THE INPUTS")
    results = dict(numbered("TWO", "THE RESULTS"))
    header(d, 80, 205, "THE INPUT: payment, fraud check", AMBER)
    header(d, 1060, 205, "THE RESULT: HTTP status, decision", GREEN)
    for i, (n, inp) in enumerate(inputs):
        y = 265 + i * 74
        bad = not inp.endswith("works")
        panel(d, 80, y, 900, 62, AMBER if bad else LINE, 3 if bad else 2)
        fit_mono(d, 105, y + 31, n + "  " + inp, AMBER if bad else TEXT, 26, 860, "lm")
        draw.arrow_right(d, 990, y + 31, 1050, MUTED, 4)
        res = results[n]
        c = GREEN if "APPROVED" in res else AMBER
        panel(d, 1060, y, 780, 62, c, 3)
        fit_mono(d, 1085, y + 31, res, c, 26, 730, "lm")
    budget = first("TWO", "THE TIME BUDGET")
    attempts, each, total, needs = [int(v) for v in re.findall(r"\d+", budget[0])]
    header(d, 80, 720, "THE TIME BUDGET, WORKED OUT (row 6)", CYAN)
    x0, x1 = 80, 1840
    px = (x1 - x0) / needs
    d.rounded_rectangle([x0, 785, x1, 835], radius=10, fill=PANEL, outline=LINE, width=2)
    for k in range(attempts):
        d.rounded_rectangle([x0 + int(k * each * px) + 3, 785, x0 + int((k + 1) * each * px) - 3, 835], radius=10, fill=AMBER)
        label(d, x0 + int((k + 0.5) * each * px), 810, "attempt %d: %d ms" % (k + 1, each), INK, 24, True, "mm")
    label(d, x0 + int(total * px) + 20, 810, "refused at %d ms" % total, AMBER, 26, True, "lm")
    label(d, x1 - 10, 810, "the check answers at %d ms" % needs, MUTED, 24, False, "rm")
    caption(d, budget[1].replace("true", "yes") + ".", AMBER, 900)


def four_transfers(img, d, cfg):
    """The same transfer written four ways, and what is left in each account afterwards."""
    before = first("THREE", "THE ACCOUNTS BEFORE")[0]
    transfer = first("THREE", "THE TRANSFER")[0]
    rows = first("THREE", "THE RESULTS")
    loss = first("THREE", "THE LOSS")[0]
    header(d, 80, 205, "THE TRANSFER: " + transfer + " (frozen)", AMBER)
    names = ["BEFORE"] + rows[0::2]
    states = [before] + [r.split("  <- ")[0] for r in rows[1::2]]
    marks = [""] + [r.partition("  <- ")[2] for r in rows[1::2]]
    for j, h in enumerate(("ana", "ben", "total")):
        label(d, 1090 + j * 230, 280, h, MUTED, 24, True, "ma")
    for i, (name, state, mark) in enumerate(zip(names, states, marks)):
        y = 315 + i * 105
        c = CYAN if i == 0 else ROSE if mark else GREEN
        fit_mono(d, 80, y + 40, name, c, 26, 880, "lm")
        if mark:
            label(d, 80, y + 80, mark.lower(), ROSE, 24, True, "lm")
        for j, amount in enumerate(re.findall(r"\d+\.\d\d", state)):
            cell(d, 990 + j * 230, y, 200, 76, amount, c if j == 2 or i == 0 else TEXT, 28)
    fit_mono(d, 80, 870, loss, ROSE, 28, 1760)
    caption(d, "Only rollbackFor = Exception.class, called from outside, undoes the debit.", AMBER, 935)


def responses(img, d, cfg):
    """The same failure as Boot's debugging response, as the one error handler answers it, and as the log keeps it."""
    leaky = first("FOUR", "THE RESPONSE, DEBUGGING")
    handled = first("FOUR", "THE RESPONSE, WITH")
    told = first("FOUR", "THE CUSTOMER SEES")
    log = first("FOUR", "THE LOG LINE")
    failure = first("FOUR", "THE FAILURE")[0]
    header(d, 80, 205, "THE FAILURE: " + failure, AMBER)
    cols = [("BOOT'S RESPONSE, DEBUGGING ON", ROSE, [l.split("  <-")[0] for l in leaky], ""),
            ("ONE ERROR HANDLER", GREEN, handled, " ".join(told)),
            ("THE LOG LINE", CYAN, [], " ".join(log))]
    for i, (title, c, flags, text) in enumerate(cols):
        x = 60 + i * 610
        header(d, x, 280, title, c, size=24)
        panel(d, x, 340, 580, 520, c, 3)
        y = 370
        for f in flags:
            for piece in wrap_px(f, 530, 26):
                fit_mono(d, x + 25, y, piece, c if f.endswith("true") or "true;" in f else TEXT, 26, 540)
                y += 42
        y += 20
        for piece in wrap_px(text, 530, 26):
            fit_mono(d, x + 25, y, piece, TEXT, 26, 540)
            y += 42
    caption(d, "Outside, a reference; inside, the reason, with the card cut to four digits.", AMBER, 900)


def breaker_calls(img, d, cfg):
    """Ten coffees during a slow outage: the calls that reach the fraud check, with and without a breaker."""
    outage = first("FIVE", "THE OUTAGE")
    without = first("FIVE", "WITHOUT A BREAKER")
    withb = first("FIVE", "WITH A BREAKER")
    reasons = first("FIVE", "THE REFUSALS BY REASON")
    after = first("FIVE", "AFTER THE")[0]
    calls = [int(re.search(r"= (\d+)", l[-1]).group(1)) for l in (without, withb)]
    attempts = int(re.search(r"x (\d+)", without[-1]).group(1))
    header(d, 80, 205, "THE OUTAGE: " + outage[0], AMBER)
    rows = [("WITHOUT A BREAKER", without, calls[0], ROSE), ("WITH A BREAKER", withb, calls[1], GREEN)]
    for i, (name, lines, n, c) in enumerate(rows):
        y = 280 + i * 220
        label(d, 80, y, name, c, 26, True)
        x = 80
        for k in range(10):
            tried = k < n // attempts
            for a in range(attempts if tried else 0):
                d.rounded_rectangle([x, y + 45, x + 56, y + 100], radius=6, fill=AMBER)
                x += 62
            if not tried:
                d.rounded_rectangle([x, y + 45, x + 16, y + 100], radius=6, fill=c)
                x += 22
            x += 14
        mono(d, 80, y + 118, lines[0], TEXT, 26)
        mono(d, 80, y + 156, lines[-1], c, 26)
    label(d, 1600, 330, "amber: one call", MUTED, 24)
    label(d, 1600, 560, "thin: refused", MUTED, 24)
    label(d, 1600, 590, "at once", MUTED, 24)
    header(d, 80, 720, "THE REFUSALS BY REASON", CYAN)
    for j, r in enumerate(reasons):
        mono(d, 80, 780 + j * 38, r, AMBER if j == len(reasons) - 1 else TEXT, 26)
    header(d, 1060, 720, "AFTER THE COOL-DOWN", GREEN)
    for j, piece in enumerate(wrap_px(after, 780, 26)):
        mono(d, 1060, 780 + j * 38, piece, GREEN, 26)
    caption(d, "An open breaker still says no, but at once, and the counters raise an alert.", AMBER, 935)
