"""Pictures for the race conditions with PostgreSQL video, drawn by the `figure` slide kind.

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




def code_of(text):
    m = re.search(r"\b(\d{5}|\d{2}P\d{2})\b", text)
    return m.group(1) if m else ""


# ------------------------------------------------------------------ figures

def four_ways(img, d, cfg):
    """The same second withdrawal of 80, after A's, under four ways of closing the gap, and what B gets back."""
    fix1 = dict(facts(sections("ONE", "AFTER THE FIX, ONE")[0]))
    fix2 = dict(facts(sections("ONE", "AFTER THE FIX, TWO")[0]))
    lock = [v for k, v in facts(sections("TWO", "THE STEPS")[0]) if k == "B"]
    version = dict(facts(sections("FOUR", "B PAYS 80")[0]))
    cols = [("ONE. A RULE IN THE TABLE", fix1["column"], [fix1["B"]], ROSE),
            ("TWO. A CHECK IN THE UPDATE", fix2["update"].replace("... ", ""), [fix2["B"]], AMBER),
            ("THREE. A ROW LOCK", "select ... for update", lock, CYAN),
            ("FOUR. A VERSION COLUMN", "update ... where id=? and version=?", [version["where"], version["commit"]], GREEN)]
    label(d, 60, 180, "A has just taken 80 from 100. B asks for 80 too. What does B get?", TEXT, 32, True)
    for i, (head, how, got, colour) in enumerate(cols):
        x, y = 60 + (i % 2) * 920, 250 + (i // 2) * 320
        panel(d, x, y, 880, 290, colour)
        header(d, x + 20, y + 18, head, colour)
        label(d, x + 24, y + 92, "told", MUTED, 26, True)
        fit_mono(d, x + 150, y + 92, how, TEXT, 28, 710)
        label(d, x + 24, y + 150, "B gets", MUTED, 26, True)
        for j, part in enumerate(got):
            fit_mono(d, x + 150, y + 150 + j * 48, part, colour, 28, 710)
    caption(d, "Each way makes the check and the write one step; only 80 is paid out in all four.", AMBER, 905)


def check_then_act(img, d, cfg):
    """The four statements of act one in their order, the balance after each, and the two fixes."""
    steps = facts(sections("ONE", "THE STEPS")[0])
    result = dict(facts(sections("ONE", "THE RESULT")[0]))
    header(d, 60, 180, heading("ONE", "THE STEPS"), CYAN)
    col = 300
    lanes = {"A": 270, "B": 380}
    for name, y in lanes.items():
        label(d, 120, y + 40, name, CYAN, 52, True, "mm")
    for i, (k, v) in enumerate(steps):
        who, verb = k.split()
        colour = ROSE if verb == "writes" else AMBER
        text = ("read " + v.split(",")[0]) if verb == "reads" else v.split(" (")[0].replace("balance = ", "write ")
        cell(d, 200 + i * col, lanes[who], col - 20, 80, text, colour, 28)
        if verb == "reads":
            fit_mono(d, 200 + i * col + 10, lanes[who] + 92, v.split(", ")[1], MUTED, 20, col - 30)
    label(d, 1440, 270, "the result", MUTED, 28, True)
    fit_mono(d, 1440, 320, "paid out " + plain(result["paid out"]).split(" from")[0], ROSE, 34, 440)
    fit_mono(d, 1440, 370, "from a balance of 100", ROSE, 26, 440)
    fit_mono(d, 1440, 420, "balance " + result["balance"], TEXT, 30, 440)
    for i, head in enumerate(("AFTER THE FIX, ONE", "AFTER THE FIX, TWO")):
        f = facts(sections("ONE", head)[0])
        x = 60 + i * 920
        header(d, x, 520, heading("ONE", head).split(": ")[1], GREEN, 880, 24)
        for j, (k, v) in enumerate(f):
            y = 590 + j * 60
            label(d, x + 10, y + 22, k, MUTED, 26, True, "lm")
            fit_mono(d, x + 160, y + 22, v, ROSE if k == "B" else TEXT, 26, 720, "lm")
    caption(d, "Both read 100 before either wrote. In the table or in the update, the rule refuses B.", AMBER, 930)


def row_lock(img, d, cfg):
    """A holds the row, B waits in line, then reads what A wrote; and the three ways not to wait."""
    steps = facts(sections("TWO", "THE STEPS")[0])
    result = dict(facts(sections("TWO", "THE RESULT")[0]))
    other = facts(sections("TWO", "WHEN B SHOULD NOT WAIT")[0])
    header(d, 60, 180, heading("TWO", "THE STEPS"), CYAN)
    label(d, 450, 260, "A", CYAN, 44, True, "mm")
    label(d, 1350, 260, "B", CYAN, 44, True, "mm")
    for i, (who, v) in enumerate(steps):
        x, y = (60 if who == "A" else 960), 300 + i * 80
        colour = AMBER if "waiting" in v else GREEN if "refuse" in v else TEXT
        cell(d, x, y, 780, 64, v, colour, 26)
    d.rectangle([1765, 368, 1777, 452], fill=AMBER)
    label(d, 1790, 410, "waits", AMBER, 26, True, "lm")
    label(d, 60, 640, "result: paid out " + result["paid out"], GREEN, 32, True)
    header(d, 60, 700, heading("TWO", "WHEN B SHOULD NOT WAIT").rstrip(":"), AMBER)
    for i, (k, v) in enumerate(other):
        x = 60 + i * 610
        label(d, x, 765, k, AMBER, 28, True)
        for j, part in enumerate(v.split(", ")):
            fit_mono(d, x, 810 + j * 38, part, TEXT, 26, 580)
    caption(d, "The lock makes the second request run after the first, and see what it wrote.", AMBER, 950)


def isolation(img, d, cfg):
    """The same two updates under READ COMMITTED and REPEATABLE READ, then write skew across two rows."""
    for i, head in enumerate(("READ COMMITTED", "REPEATABLE READ")):
        f = [(k, v) for k, v in facts(sections("THREE", head)[0]) if k != "message"]
        x = 60 + i * 920
        header(d, x, 180, heading("THREE", head), CYAN if i == 0 else AMBER, 880, 24)
        for j, (k, v) in enumerate(f):
            y = 245 + j * 52
            label(d, x + 10, y + 20, k, MUTED, 26, True, "lm")
            colour = ROSE if "refused" in v else GREEN if k == "A retries" else TEXT
            fit_mono(d, x + 170, y + 20, v, colour, 26, 700, "lm")
    for i, head in enumerate(("BEFORE THE FIX", "AFTER THE FIX")):
        f = dict(facts(sections("THREE", head)[0]))
        x = 60 + i * 920
        colour = ROSE if i == 0 else GREEN
        header(d, x, 520, heading("THREE", head), colour, 880, 24)
        start = 100
        rows = [("ANA-1", start - 150), ("ANA-2", start - 150 if "commits" in f["B"] else start)]
        zero = x + 520
        for j, (acct, bal) in enumerate(rows):
            y = 590 + j * 75
            label(d, x + 10, y + 28, acct, TEXT, 26, True, "lm")
            label(d, x + 130, y + 28, "%d -> %d" % (start, bal), MUTED, 24, True, "lm")
            k = 2.4
            a, b = sorted((zero, zero + bal * k))
            d.rectangle([a, y + 8, b, y + 48], fill=ROSE if bal < 0 else GREEN)
        d.line([zero, 580, zero, 745], fill=TEXT, width=3)
        fit_mono(d, x + 10, 770, "B: " + f["B"], colour if i else MUTED, 24, 860)
        fit_mono(d, x + 10, 815, "total " + plain(f["total"]), colour, 28, 860)
    caption(d, "Each saw a total of 200 and took 150 from a different row: only SERIALIZABLE notices.", AMBER, 935)


def versions(img, d, cfg):
    """Two sessions load version 0; A's update moves it to 1; B's update still asks for version 0 and changes no row."""
    both = dict(facts(sections("FOUR", "BOTH LOAD")[0]))
    a = facts(sections("FOUR", "A PAYS")[0])
    b = dict(facts(sections("FOUR", "B PAYS")[0]))
    key = facts(sections("FOUR", "ONE PAYMENT KEY")[0])
    sql = " ".join(v for k, v in a if k in ("sql", ""))
    values = dict(a)["values"]
    header(d, 60, 180, "THE ROW hib_account ANA-1", CYAN)
    v0, v1 = re.search(r"version (\d+) \+ 1 = (\d+)", values).groups()
    bal = re.search(r"balance (\d+)", values).group(1)
    cell(d, 60, 250, 380, 80, "balance 100, version " + v0, TEXT, 26)
    arrow(d, 450, 290, 560, 290, GREEN)
    cell(d, 570, 250, 380, 80, "balance %s, version %s" % (bal, v1), GREEN, 26)
    label(d, 1000, 262, "A loaded: " + both["A"], MUTED, 24, True)
    label(d, 1000, 300, "B loaded: " + both["B"], MUTED, 24, True)
    header(d, 60, 380, "A COMMITS", GREEN)
    fit_mono(d, 60, 450, sql, TEXT, 28, 1800)
    fit_mono(d, 60, 495, values + ": 1 row", GREEN, 28, 1800)
    header(d, 60, 560, "B COMMITS ON WHAT IT LOADED", ROSE)
    fit_mono(d, 60, 630, "where " + b["where"], ROSE, 26, 1800)
    fit_mono(d, 60, 675, b["commit"] + "; " + b["B retries"], AMBER, 26, 1800)
    header(d, 60, 740, "ONE PAYMENT KEY, SENT TWICE", CYAN)
    for i, (who, v) in enumerate(key[:4]):
        x, y = 60 + (i % 2) * 920, 805 + (i // 2) * 50
        fit_mono(d, x, y, "%s  %s" % (who, v), AMBER if "waits" in v else TEXT, 26, 880)
    caption(d, "A version number makes every update conditional; a key commits with the money.", AMBER, 950)


def the_bill(img, d, cfg):
    """The deadlock PostgreSQL breaks, and what each error code asks of the application."""
    dl = facts(sections("FIVE", "TWO TRANSFERS")[0])
    f = dict(dl)
    header(d, 60, 180, heading("FIVE", "TWO TRANSFERS"), ROSE, 860, 22)
    locks = {"ANA-1": (210, 330), "BEN-2": (710, 330)}
    for name, (cx, cy) in locks.items():
        cell(d, cx - 100, cy - 35, 200, 70, name, AMBER, 30)
    who = {"A": (210, 560), "B": (710, 560)}
    for w, (cx, cy) in who.items():
        d.ellipse([cx - 45, cy - 45, cx + 45, cy + 45], fill=BG_CELL, outline=CYAN, width=4)
        label(d, cx, cy, w, CYAN, 44, True, "mm")
        held, wanted = re.match(r"locks (\S+), then asks for (\S+)", f[w]).groups()
        hx, hy = locks[held]
        wx, wy = locks[wanted]
        arrow(d, cx, cy - 45, hx, hy + 37, GREEN, 4)
        arrow(d, cx + (30 if w == "A" else -30), cy - 40, wx + (-70 if w == "A" else 70), wy + 37, ROSE, 4)
    label(d, 150, 440, "holds", GREEN, 22, True, "mm")
    label(d, 460, 420, "waits for", ROSE, 22, True, "mm")
    fit_mono(d, 60, 640, "PostgreSQL " + f["PostgreSQL"], ROSE, 24, 860)
    fit_mono(d, 60, 680, "the other " + plain(f["the other"]), GREEN, 24, 860)
    order = dict(facts(sections("FIVE", "AFTER THE FIX")[0]))
    fit_mono(d, 60, 730, "one lock order (order by id): " + order["A and B"], GREEN, 24, 860)
    header(d, 1000, 180, heading("FIVE", "WHAT EACH ERROR CODE").rstrip(":"), AMBER, 860, 22)
    for i, ln in enumerate(sections("FIVE", "WHAT EACH ERROR CODE")[0]):
        code, name, action = re.split(r"\s{2,}", ln.strip())
        y = 260 + i * 110
        colour = GREEN if "retry the" in action else AMBER if "later" in action else ROSE
        cell(d, 1000, y, 170, 80, code, colour, 30)
        label(d, 1200, y + 10, name, TEXT, 28, True)
        label(d, 1200, y + 48, action, colour, 26, True)
    servers = facts(sections("FIVE", "TWO SERVERS")[0])
    fit_mono(d, 1000, 730, "two servers' own locks: " + plain(servers[0][1]), ROSE, 24, 860)
    caption(d, "Retry 40001 and 40P01; refuse 23514; keep transactions short.", AMBER, 935)
