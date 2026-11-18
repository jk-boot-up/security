"""Pictures for the race conditions and business logic video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import math
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


def heading(name, head, n=0):
    return [ln for ln in act(name) if ln.startswith(head)][n].rstrip(":")


def facts(lines):
    """`key   value` lines as a list of (key, value)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\S\s{2,}\S", ln.strip())]


def value(lines, key, n=0):
    return [v for k, v in facts(lines) if k == key][n]


def plain(text):
    """A value without its "  <- NOTE" marker."""
    return text.split("  <-")[0]


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    while f.getlength(text) > (w or 1800) - 30 and f.size > 16:
        f = font(SANS_B, f.size - 1)
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


def arrow(d, x1, y1, x2, y2, colour, width=4):
    """A straight arrow in any direction, with its head at (x2, y2)."""
    d.line([(x1, y1), (x2, y2)], fill=colour, width=width)
    a = math.atan2(y2 - y1, x2 - x1)
    pts = [(x2, y2)] + [(x2 - 20 * math.cos(a + s * 0.45), y2 - 20 * math.sin(a + s * 0.45)) for s in (-1, 1)]
    d.polygon(pts, fill=colour)


def step_colour(step):
    if "waits" in step or "changed" in step or "deadlock" in step:
        return AMBER
    if ": no" in step or "refuse" in step:
        return GREEN
    return TEXT


# ------------------------------------------------------------------ figures

def the_gap(img, d, cfg):
    """Two withdrawals of 80 from 100, step by step in the order ABABAB, with the balance and the money paid out."""
    steps = sections("ONE", "THE STEPS")[0]
    order = re.search(r"ORDER (\w+)", heading("ONE", "THE STEPS")).group(1)
    req = dict(facts(sections("ONE", "THE REQUESTS")[0]))
    start = int(re.search(r"holds (\d+)", heading("ONE", "THE REQUESTS")).group(1))
    end = header(d, 60, 180, "THE REQUESTS", CYAN)
    label(d, end + 20, 202, "A: %s.   B: %s." % (req["A"], req["B"]), TEXT, 26, False, "lm")
    x0, col = 300, 265
    lanes = {"A": 330, "B": 480}
    for name, y in lanes.items():
        label(d, 160, y + 45, name, CYAN, 56, True, "mm")
        d.line([x0 - 20, y + 45, x0 + len(steps) * col, y + 45], fill=LINE, width=2)
    balance, paid = start, 0
    balances, paids = [], []
    for i, (who, step) in enumerate(zip(order, steps)):
        x, y = x0 + i * col, lanes[who]
        verb = step.split(" ", 1)[1]
        m = re.match(r"(\w+) .*?(\d+)(?: covers (\d+))?", verb)
        short = {"reads": "read %s" % m.group(2), "checks": "%s >= %s? yes" % (m.group(2), m.group(3)),
                 "writes": "write %s" % m.group(2)}[m.group(1)]
        colour = ROSE if m.group(1) == "writes" else AMBER if m.group(1) == "checks" else CYAN
        cell(d, x, y, col - 20, 90, short, colour, 28)
        if m.group(1) == "writes":
            balance = int(m.group(2))
            paid += int(re.search(r"pays out (\d+)", step).group(1))
        balances.append(balance)
        paids.append(paid)
    for row, (name, values) in enumerate((("balance", balances), ("paid out", paids))):
        y = 640 + row * 100
        label(d, 160, y + 35, name, MUTED, 28, True, "mm")
        for i, v in enumerate(values):
            bad = (name == "paid out" and v > start)
            cell(d, x0 + i * col, y, col - 20, 70, str(v), ROSE if bad else TEXT, 30)
    # the gap: from A's check to A's write, B runs
    a_check = x0 + [i for i, w in enumerate(order) if w == "A"][1] * col
    a_write = x0 + [i for i, w in enumerate(order) if w == "A"][2] * col
    d.rectangle([a_check + col - 12, 305, a_write - 8, 595], outline=ROSE, width=4)
    label(d, (a_check + col + a_write) // 2 - 10, 600, "the gap: B reads and checks here", ROSE, 26, True, "ma")
    result = sections("ONE", "THE RESULT")[0]
    caption(d, "Paid out " + plain(value(result, "paid out")) + ": both checks saw 100.", ROSE, 930)


def every_order(img, d, cfg):
    """All 20 orders of A's three steps and B's three steps, and what each one pays out."""
    sec = sections("ONE", "EVERY ORDER")[0]
    pairs = re.findall(r"([AB]{6}) (\d+)", " ".join(sec))
    header(d, 60, 180, heading("ONE", "EVERY ORDER"), CYAN)
    for i, (order, paid) in enumerate(pairs):
        x, y = 60 + (i % 5) * 362, 250 + (i // 5) * 105
        colour = ROSE if int(paid) > 100 else GREEN
        d.rounded_rectangle([x, y, x + 342, y + 88], radius=10, fill=BG_CELL, outline=colour, width=3)
        f = font(MONO_B, 34)
        for j, ch in enumerate(order):
            d.text((x + 24 + j * 25, y + 44), ch, font=f, fill=CYAN if ch == "A" else AMBER, anchor="lm")
        label(d, x + 324, y + 44, "pays " + paid, colour, 26, True, "rm")
    header(d, 60, 690, "THE RESULT", ROSE)
    label(d, 60, 770, "pays 160:  " + value(sec, "pays 160"), ROSE, 32, True)
    label(d, 60, 820, "pays 80:  " + value(sec, "pays 80"), GREEN, 32, True)
    coupon = plain(value(sections("ONE", "THE RESULT")[1], "discount"))
    label(d, 1000, 770, "the coupon SUMMER20, in the same gap:", MUTED, 30, True)
    label(d, 1000, 820, "discount " + coupon, ROSE, 32, True)
    caption(d, "Testing one request at a time tries only the two green orders.", AMBER, 930)


def one_step(img, d, cfg):
    """The lock and the compare-and-set, step by step, and how many orders each lets pay out too much."""
    lock = sections("TWO", "WITH A LOCK")[0]
    cas = sections("TWO", "WITH A COMPARE-AND-SET")[0]
    header(d, 60, 180, heading("TWO", "WITH A LOCK"), GREEN, 860, 24)
    for i, step in enumerate(lock):
        fit_mono(d, 80, 245 + i * 40, step.strip(), step_colour(step), 26, 820)
    header(d, 1000, 180, heading("TWO", "WITH A COMPARE-AND-SET"), GREEN, 860, 24)
    for i, step in enumerate(cas):
        fit_mono(d, 1020, 245 + i * 40, step.strip(), step_colour(step), 26, 840)
    decide = dict(facts(sections("TWO", "HOW B'S COMPARE-AND-SET")[0]))
    y = 245 + len(cas) * 40 + 20
    cell(d, 1020, y, 250, 70, "read " + decide["B read"], CYAN, 28)
    cell(d, 1300, y, 250, 70, "now " + decide["now"].split(",")[0], AMBER, 28)
    label(d, 1580, y + 35, "equal? " + decide["100 = 20?"].split(":")[0], ROSE, 30, True, "lm")
    counts = facts(sections("TWO", "EVERY ORDER")[0])
    header(d, 60, 735, heading("TWO", "EVERY ORDER"), AMBER)
    for i, (k, v) in enumerate(counts):
        cell(d, 60 + i * 610, 800, 580, 80, "%s: %s" % (k, v), ROSE if not v.startswith("0 ") else GREEN, 30)
    caption(d, "Check and write as one step: no order pays out more than the balance.", AMBER, 935)


def idempotency(img, d, cfg):
    """A rent payment sent twice: without a key it moves twice; with a key, the second copy gets the first answer."""
    req = dict(facts(sections("THREE", "THE REQUEST")[0]))
    before = facts(sections("THREE", "BEFORE THE FIX")[0])
    after = facts(sections("THREE", "AFTER THE FIX")[0])
    reads = facts(sections("THREE", "HOW THE BANK READS")[0])
    end = header(d, 60, 180, heading("THREE", "THE REQUEST"), CYAN)
    label(d, end + 20, 202, "%s   (balances %s; the reply is lost)" % (req["transfer"], req["balances"]), TEXT, 26, False, "lm")
    for col, (head, rows, colour) in enumerate(((heading("THREE", "BEFORE THE FIX"), before, ROSE),
                                                (heading("THREE", "AFTER THE FIX"), after, GREEN))):
        x = 60 + col * 920
        header(d, x, 270, head, colour, 880, 24)
        for i, (k, v) in enumerate(rows):
            y = 335 + i * 75
            label(d, x + 10, y + 30, k, MUTED, 26, True, "lm")
            bad = "<-" in v
            cell(d, x + 190, y, 690, 60, plain(v), ROSE if bad else (GREEN if col else TEXT), 24)
    header(d, 60, 650, heading("THREE", "HOW THE BANK READS"), AMBER)
    for i, (k, v) in enumerate(reads):
        x, y = 60 + (i % 2) * 920, 715 + (i // 2) * 80
        label(d, x + 10, y + 30, k, AMBER, 26, True, "lm")
        cell(d, x + 190, y, 690, 60, v, TEXT, 24)
    caption(d, "The app picks the key; the bank claims it in one step before moving money.", AMBER, 935)


def server_rules(img, d, cfg):
    """Three rules the server keeps: transfers, prices and an order's states, each before and after the fix."""
    bank_before = facts(sections("FOUR", "BEFORE THE FIX")[0])
    bank_after = dict(facts(sections("FOUR", "AFTER THE FIX")[0]))
    shop_before = value(sections("FOUR", "BEFORE THE FIX")[1], "charged")
    shop_after = value(sections("FOUR", "AFTER THE FIX")[1], "charged")
    flags = facts(sections("FOUR", "AN ORDER OF")[0])
    machine = facts(sections("FOUR", "THE SAME ORDER")[0])
    x_b, x_a = 330, 1120
    header(d, x_b, 180, "BEFORE THE FIX", ROSE, 760)
    header(d, x_a, 180, "AFTER THE FIX", GREEN, 760)
    rows = [("transfer", k, plain(v), bank_after[k]) for k, v in bank_before]
    rows.append(("checkout", "charged", plain(shop_before), shop_after))
    for i, (group, k, b, a) in enumerate(rows):
        y = 250 + i * 85
        label(d, 60, y + 22, group, MUTED, 24, True, "lm")
        label(d, 60, y + 52, k, CYAN, 26, True, "lm")
        cell(d, x_b, y, 760, 70, b, ROSE, 26)
        cell(d, x_a, y, 760, 70, a, GREEN, 24)
    y = 525
    label(d, 60, y + 22, "order", MUTED, 24, True, "lm")
    label(d, 60, y + 52, "of 180", CYAN, 26, True, "lm")
    for i, (k, v) in enumerate(flags):
        cell(d, x_b, y + i * 80, 760, 66, "%s: %s" % (k, plain(v)), ROSE if "<-" in v else TEXT, 26)
    states = ["created"] + [v for k, v in machine if not v.startswith("refused") and k != "refunded"]
    for i, s in enumerate(states):
        x = x_a + i * 195
        cell(d, x, y, 170, 66, s, GREEN, 26)
        if i:
            arrow(d, x - 25, y + 33, x - 2, y + 33, GREEN, 3)
    refused = [(k, v) for k, v in machine if v.startswith("refused")]
    for i, (k, v) in enumerate(refused):
        fit_mono(d, x_a, y + 110 + i * 50, "%s: %s" % (k, v), AMBER, 24, 760)
    fit_mono(d, x_a, y + 210, "refunded " + dict(machine)["refunded"], GREEN, 26, 760)
    caption(d, "The server decides amounts, prices and the next step; the client only asks.", AMBER, 935)


def lock_order(img, d, cfg):
    """Two transfers deadlock when each locks its own sender first; one lock order for everyone ends the deadlock."""
    bad = sections("FIVE", "TWO TRANSFERS")[0]
    good = sections("FIVE", "ONE LOCK ORDER")[0]
    steps = sections("FIVE", "THE STEPS")
    result = dict(facts(sections("FIVE", "THE RESULT")[0]))
    servers = sections("FIVE", "TWO SERVERS")[0]
    for col, (head, rows, trace, colour) in enumerate(((heading("FIVE", "TWO TRANSFERS"), bad, steps[0], ROSE),
                                                       (heading("FIVE", "ONE LOCK ORDER"), good, steps[1], GREEN))):
        x = 60 + col * 920
        header(d, x, 180, head, colour, 880, 22)
        locks = dict(facts(rows))
        for i, who in enumerate("AB"):
            fit_mono(d, x + 10, 245 + i * 38, "%s  %s" % (who, locks[who]), TEXT, 24, 860)
        # who holds which lock after the first two steps, and who waits for which
        lx = {"ANA-1": x + 210, "BEN-2": x + 650}
        held = {}
        for st in trace[:2]:
            m = re.match(r"(\w) locks (\S+)", st.strip())
            if m:
                held[m.group(1)] = m.group(2)
        waits = {}
        for st in trace:
            m = re.match(r"(\w) waits for (\S+)", st.strip())
            if m:
                waits.setdefault(m.group(1), m.group(2))
        for name, cx in lx.items():
            cell(d, cx - 100, 360, 200, 70, name, AMBER, 30)
        for i, who in enumerate("AB"):
            cx = x + 210 + i * 440
            d.ellipse([cx - 45, 520, cx + 45, 610], fill=BG_CELL, outline=CYAN, width=4)
            label(d, cx, 565, who, CYAN, 44, True, "mm")
            if who in held:
                arrow(d, cx, 520, lx[held[who]], 432, GREEN, 4)
                label(d, (cx + lx[held[who]]) // 2 + (-50 if i == 0 else 50), 465, "holds", GREEN, 22, True, "mm")
            if who in waits:
                arrow(d, cx + (25 if i == 0 else -25), 525, lx[waits[who]] + (-60 if i == 0 else 60), 432, ROSE, 4)
                label(d, (cx + lx[waits[who]]) // 2, 500, "waits for", ROSE, 22, True, "mm")
        if col == 0:
            fit_mono(d, x + 10, 640, trace[-1].strip(), ROSE, 26, 860)
        else:
            fit_mono(d, x + 10, 640, "deadlock %s; ana %s, ben %s" % (result["deadlock"], result["ana"].split("= ")[-1],
                                                                     result["ben"].split("= ")[-1]), GREEN, 26, 860)
    header(d, 60, 715, heading("FIVE", "TWO SERVERS"), AMBER)
    for i, st in enumerate(servers[:2]):
        cell(d, 60 + i * 560, 780, 520, 64, st.strip(), AMBER, 24)
    fit_mono(d, 1200, 812, "paid out " + plain(value(servers, "paid out")), ROSE, 30, 680, "lm")
    caption(d, "One lock order for everyone; with many servers, the database must decide.", AMBER, 935)
