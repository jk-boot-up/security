"""Pictures for the failing-closed video, drawn by the `figure` slide kind.

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

def open_vs_closed(img, d, cfg):
    """One payment and one broken fraud check, through a rule that starts from yes and one that starts from no."""
    payment = first("ONE", "THE PAYMENT")[0]
    naive = first("ONE", "THE RESULT, FRAUD CHECK DOWN")[0].split("  <-")[0]
    row = next(r for r in first("TWO", "THE RESULTS") if r.startswith("3 "))
    safe = row.split(None, 2)[2]
    header(d, 660, 205, "THE PAYMENT", AMBER)
    cell(d, 560, 260, 800, 64, payment, AMBER, 28)
    lanes = [("STARTS FROM YES (NaiveAuthorizer)", ROSE, "decision = yes", naive, "an error became a yes"),
             ("STARTS FROM NO (SafeAuthorizer)", GREEN, "decision = no", safe, "an error stays a no")]
    for i, (title, c, start, result, note) in enumerate(lanes):
        x = 80 + i * 900
        header(d, x, 370, title, c)
        panel(d, x, 430, 860, 420, c, 3)
        steps = [(start, TEXT), ("fraud check: DOWN (it throws)", AMBER), (result, c)]
        for j, (text, tc) in enumerate(steps):
            y = 460 + j * 130
            cell(d, x + 40, y, 780, 80, text, tc, 28)
            if j < 2:
                draw.arrow(d, x + 430, y + 84, x + 430, y + 126, MUTED, 4)
        label(d, x + 430, 870, note, c, 28, True, "ma")
    caption(d, "The same failure: one rule turns it into a yes, the other into a no.", AMBER, 935)


def failing_open(img, d, cfg):
    """The two payments of act one: what was asked, and the answer when the check works and when it breaks."""
    pays = sections("ONE", "THE PAYMENT")
    rows = [(pays[0][0], [("fraud check working", first("ONE", "THE RESULT, FRAUD CHECK WORKING")[0]),
                          ("fraud check DOWN", first("ONE", "THE RESULT, FRAUD CHECK DOWN")[0])], None),
            (pays[1][0], [('spent-today read as "0"', first("ONE", "THE RESULT, SPENT-TODAY READ")[0]),
                          ('spent-today "n/a"', first("ONE", "THE RESULT, SPENT-TODAY UNREADABLE")[0])],
             first("ONE", "THE DAILY LIMIT")[0])]
    header(d, 80, 205, "THE PAYMENT", AMBER)
    header(d, 760, 205, "WHAT HAPPENS", CYAN)
    header(d, 1300, 205, "THE RESULT", GREEN)
    for i, (pay, cases, limit) in enumerate(rows):
        y = 280 + i * 330
        cell(d, 80, y + 50, 560, 80, pay, AMBER, 28)
        if limit:
            label(d, 80, y + 150, "the daily limit, worked out:", MUTED, 24, True)
            fit_mono(d, 80, y + 190, limit.replace("spent today ", ""), AMBER, 24, 560)
        for j, (what, res) in enumerate(cases):
            yy = y + j * 120
            draw.arrow_right(d, 650, yy + 40, 750, MUTED, 4)
            cell(d, 760, yy, 480, 80, what, AMBER if "DOWN" in what or "n/a" in what else TEXT, 26)
            draw.arrow_right(d, 1250, yy + 40, 1290, MUTED, 4)
            main, _, mark = res.partition("  <- ")
            c = ROSE if mark else GREEN
            cell(d, 1300, yy, 540, 80, main, c, 28)
            if mark:
                label(d, 1570, yy + 88, mark.lower(), ROSE, 24, True, "ma")
    caption(d, "Starting from yes, a check that never ran took nothing away.", AMBER, 935)


def closed_table(img, d, cfg):
    """Five payments, what goes wrong with each, the answer with its reference, and the time budget worked out."""
    inputs = first("TWO", "THE INPUTS")
    results = first("TWO", "THE RESULTS")
    header(d, 80, 205, "THE INPUT: payment, what goes wrong", AMBER)
    header(d, 1060, 205, "THE RESULT, with its reference", GREEN)
    for i, (inp, res) in enumerate(zip(inputs, results)):
        y = 270 + i * 82
        parts = inp.split(None, 3)
        bad = parts[3] != "nothing"
        panel(d, 80, y, 900, 68, AMBER if bad else LINE, 3 if bad else 2)
        mono(d, 105, y + 34, "%s  %-7s%8s" % (parts[0], parts[1], parts[2]), TEXT, 26, "lm")
        mono(d, 560, y + 34, parts[3], AMBER if bad else MUTED, 26, "lm")
        draw.arrow_right(d, 990, y + 34, 1050, MUTED, 4)
        ref, verdict = res.split(None, 2)[1:]
        c = GREEN if verdict == "APPROVED" else AMBER
        panel(d, 1060, y, 780, 68, c, 3)
        mono(d, 1085, y + 34, ref, MUTED, 24, "lm")
        fit_mono(d, 1260, y + 34, verdict, c, 26, 560, "lm")
    budget = first("TWO", "THE TIME BUDGET")
    needs, limit = [int(n) for n in re.findall(r"(\d+) ms", budget[0])]
    header(d, 80, 700, "THE TIME BUDGET, WORKED OUT (row 4)", CYAN)
    x0, x1 = 80, 1840
    px = (x1 - x0) / needs
    d.rounded_rectangle([x0, 770, x1, 820], radius=10, fill=PANEL, outline=LINE, width=2)
    d.rounded_rectangle([x0, 770, x0 + int(limit * px) + 20, 820], radius=10, fill=AMBER)
    label(d, x0 + int(limit * px) + 40, 795, "given up at %d ms" % limit, AMBER, 26, True, "lm")
    label(d, x1 - 10, 795, "the answer would come at %d ms" % needs, MUTED, 26, False, "rm")
    caption(d, "Only row 1 ran every check and passed: it is the only yes.", AMBER, 900)


def half_transfer(img, d, cfg):
    """Ana's transfer to a frozen account done two ways, and three failed updates with and without try-with-resources."""
    def balances(line):
        return re.findall(r"(ana|ben|total) (\d+\.\d\d)", line)
    states = [("BEFORE", first("THREE", "THE ACCOUNTS BEFORE")[0], CYAN),
              ("DEBIT THEN CREDIT", first("THREE", "AFTER, DEBIT THEN CREDIT")[2], ROSE),
              ("ONE UNIT OF WORK", first("THREE", "AFTER, ONE UNIT OF WORK")[1], GREEN)]
    transfer = first("THREE", "THE TRANSFER")[0]
    vanished = first("THREE", "AFTER, DEBIT THEN CREDIT")[3]
    header(d, 80, 205, "THE TRANSFER: " + transfer + " (frozen)", AMBER)
    for j, h in enumerate(("ana", "ben", "total")):
        label(d, 520 + j * 190, 285, h, MUTED, 24, True, "ma")
    for i, (name, line, c) in enumerate(states):
        y = 320 + i * 110
        label(d, 80, y + 40, name, c, 26, True, "lm")
        for j, (who, amount) in enumerate(balances(line)):
            cell(d, 440 + j * 190, y, 170, 80, amount, c if (who == "total" or i == 0) else TEXT, 28)
    fit_mono(d, 80, 670, vanished.replace("  <- ", "  ").replace("VANISHED", "vanished"), ROSE, 28, 900)
    label(d, 80, 720, "One unit of work checks first and moves nothing.", GREEN, 26, True)
    header(d, 1120, 205, "THE CONNECTION POOL", CYAN)
    pools = [("given back only on success", first("THREE", "AFTER 3 FAILED UPDATES, GIVEN BACK")[0], ROSE),
             ("try-with-resources", first("THREE", "AFTER 3 FAILED UPDATES, WITH")[0], GREEN)]
    for i, (name, line, c) in enumerate(pools):
        y = 290 + i * 230
        label(d, 1120, y, name, c, 26, True)
        free = [int(n) for n in line.split(":")[1].split(",")]
        for k, n in enumerate(free):
            x = 1120 + k * 240
            label(d, x, y + 50, "after failure %d" % (k + 1), MUTED, 24)
            for s in range(3):
                on = s < n
                d.rounded_rectangle([x + s * 66, y + 90, x + s * 66 + 54, y + 144], radius=8,
                                    fill=c if on else BG_CELL, outline=c, width=3)
    nxt = first("THREE", "AFTER 3 FAILED UPDATES, GIVEN BACK")[1]
    fit_mono(d, 1120, 760, nxt, ROSE, 24, 720)
    caption(d, "A filled square is a free connection.", AMBER, 935)


def page_vs_log(img, d, cfg):
    """The same failure as the raw page shows it, as the customer is told, and as the log keeps it."""
    failure = first("FOUR", "THE FAILURE")
    flags = first("FOUR", "THE RAW ERROR PAGE")
    safe = first("FOUR", "THE SAFE PAGE")
    log = first("FOUR", "THE LOG LINE")
    redaction = first("FOUR", "THE REDACTION")[0]
    cols = [("THE RAW ERROR PAGE", ROSE, failure, flags),
            ("THE SAFE PAGE", GREEN, safe, []),
            ("THE LOG LINE", CYAN, log, [redaction])]
    for i, (title, c, lines, notes) in enumerate(cols):
        x = 60 + i * 610
        header(d, x, 205, title, c)
        panel(d, x, 265, 580, 560, c, 3)
        y = 300
        for piece in wrap_px(" ".join(lines), 530, 28):
            fit_mono(d, x + 25, y, piece, TEXT, 28, 540)
            y += 46
        y += 30
        for n in notes:
            for piece in wrap_px(n, 530, 28, sans=True):
                label(d, x + 25, y, piece, c, 28, True)
                y += 42
            y += 8
    caption(d, "Outside, a reference; inside, the detail, with the card masked.", AMBER, 900)


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


def breaker_bill(img, d, cfg):
    """Ten coffees during a slow outage, with and without a breaker, drawn as time spent waiting."""
    outage = first("FIVE", "THE OUTAGE")
    budget = int(re.search(r"budget is (\d+) ms", outage[0]).group(1))
    without = first("FIVE", "WITHOUT A BREAKER")
    withb = first("FIVE", "WITH A BREAKER")
    calls_without = int(re.search(r"calls to the fraud check (\d+)", without[0]).group(1))
    calls_with = int(re.search(r"calls to the fraud check (\d+)", withb[0]).group(1))
    reasons = first("FIVE", "THE REFUSALS BY REASON")
    after = first("FIVE", "AFTER THE")[0]
    header(d, 80, 205, "THE OUTAGE: " + outage[0], AMBER)
    x0, px = 80, 1500 / (calls_without * budget)
    rows = [("WITHOUT A BREAKER", without, calls_without, ROSE), ("WITH A BREAKER (opens after 3 failures)", withb, calls_with, GREEN)]
    for i, (name, lines, calls, c) in enumerate(rows):
        y = 280 + i * 225
        label(d, x0, y, name, c, 26, True)
        x = x0
        for k in range(10):
            w = int(budget * px) - 6 if k < calls else 16
            d.rounded_rectangle([x, y + 42, x + w, y + 102], radius=6, fill=AMBER if k < calls else c)
            x += w + 6
        mono(d, x0, y + 120, lines[0], TEXT, 26)
        mono(d, x0, y + 158, lines[1], c, 26)
    label(d, 1660, 322, "amber: waited", MUTED, 24, False, "la")
    label(d, 1660, 352, "%d ms" % budget, MUTED, 24, False, "la")
    label(d, 1660, 562, "thin: refused", MUTED, 24, False, "la")
    label(d, 1660, 592, "at once", MUTED, 24, False, "la")
    header(d, 80, 720, "THE REFUSALS BY REASON", CYAN)
    for j, r in enumerate(reasons):
        mono(d, 80, 780 + j * 38, r, AMBER if j == len(reasons) - 1 else TEXT, 26)
    header(d, 1060, 720, "AFTER THE COOL-DOWN", GREEN)
    for j, piece in enumerate(wrap_px(after, 780, 26)):
        mono(d, 1060, 780 + j * 38, piece, GREEN, 26)
    caption(d, "An open breaker still says no, but at once, and someone is alerted.", AMBER, 935)
