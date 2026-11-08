"""Pictures for the SQL injection video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
DIM = (18, 26, 44)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def part(name, head):
    """The indented lines under the first section header of an act that starts with `head`, two spaces removed."""
    out, on = [], False
    for ln in act(name):
        if not ln.startswith("  "):
            if on:
                break
            on = ln.startswith(head)
        elif on:
            out.append(ln[2:])
    return out


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


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


def statement_runs(lines, term):
    """The statement lines as runs of (text, colour): the program's text, the customer's input, and the commented-out tail."""
    joined = " ".join(lines)
    start = joined.find(term) if term else -1
    comment = joined.find("--", start if start >= 0 else 0) if "--" in (term or "") else -1
    out, pos = [], 0
    for ln in lines:
        runs = []
        for i, ch in enumerate(ln):
            k = pos + i
            c = MUTED if comment >= 0 and k >= comment + 2 else ROSE if start >= 0 and start <= k < start + len(term) else TEXT
            if runs and runs[-1][1] == c:
                runs[-1][0] += ch
            else:
                runs.append([ch, c])
        out.append(runs)
        pos += len(ln) + 1
    return out


def draw_runs(d, x, y, runs, size=26):
    f = font(MONO_B, size)
    for text, c in runs:
        d.text((x, y), text, font=f, fill=c)
        x += f.getlength(text)


# ------------------------------------------------------------------ figures

def code_vs_data(img, d, cfg):
    """The same statement with an honest word and with a quote: data stays inside the quotes, or becomes code."""
    honest = sections("ONE", "THE STATEMENT")[0]
    bypass_in = sections("TWO", "THE INPUT")[0][0].rsplit("    ", 1)[0].strip()
    bypass = sections("TWO", "THE STATEMENT")[0]
    rows = [("AN HONEST WORD: kettle", GREEN, honest, "kettle", "the word stays inside the quotes: data"),
            ("A QUOTE: " + bypass_in, ROSE, bypass, bypass_in, "the quote closes the string: the rest is code")]
    for i, (title, c, lines, term, note) in enumerate(rows):
        y = 210 + i * 340
        header(d, 80, y, title, c)
        panel(d, 80, y + 60, 1760, 60 + 46 * len(lines), c, 3)
        for j, runs in enumerate(statement_runs([l.strip() for l in lines], term)):
            draw_runs(d, 110, y + 85 + j * 46, [(t, GREEN if (col == ROSE and c == GREEN) else col) for t, col in runs], 28)
        label(d, 80, y + 140 + 46 * len(lines), note, c, 26, True)
    caption(d, "Red is what the customer typed; grey is commented out.", AMBER, 920)


def products_table(img, d, cfg):
    """The products table, the honest search, and the two rows it returns."""
    rows = [ln.strip() for ln in sections("ONE", "THE TABLE")[0]]
    result = sections("ONE", "THE RESULT")[0][0].strip()
    header(d, 60, 200, "THE TABLE products", CYAN)
    for j, h in enumerate(("name", "price (cents)", "listed")):
        label(d, (80, 470, 690)[j], 270, h, MUTED, 22, True)
    found = [n.strip() for n in result.strip("[]").split(",")]
    for i, ln in enumerate(rows):
        m = re.match(r"(.+?)\s+(\d+)\s+(\d)", ln)
        name, price, listed = m.groups()
        y = 310 + i * 80
        c = GREEN if name in found else (MUTED if listed == "0" else LINE)
        panel(d, 60, y, 780, 64, c, 3 if name in found else 2)
        mono(d, 80, y + 32, name, TEXT if listed == "1" else MUTED, 26, "lm")
        mono(d, 560, y + 32, price, TEXT, 26, "rm")
        mono(d, 720, y + 32, listed, ROSE if listed == "0" else TEXT, 26, "mm")
    inp = sections("ONE", "THE INPUT")[0][0].split()
    header(d, 940, 200, "THE INPUT", AMBER)
    cell(d, 940, 260, 400, 64, "search: " + inp[0], AMBER, 26)
    cell(d, 1380, 260, 400, 64, "sort: " + inp[1], AMBER, 26)
    header(d, 940, 360, "THE STATEMENT AS BUILT", CYAN)
    lines = [l.strip() for l in sections("ONE", "THE STATEMENT")[0]]
    panel(d, 940, 420, 900, 50 + 44 * len(lines), CYAN)
    for j, runs in enumerate(statement_runs(lines, inp[0])):
        draw_runs(d, 965, 440 + j * 44, [(t, AMBER if col == ROSE else col) for t, col in runs], 24)
    header(d, 940, 640, "THE RESULT", GREEN)
    mono(d, 940, 710, result, GREEN, 30)
    caption(d, "The kettle stays inside the quotes; listed = 1 hides the toaster.", AMBER, 900)


def quote_breakout(img, d, cfg):
    """The three attacks: each statement as built, coloured by who wrote which part, and its result."""
    inputs = [s[0].rsplit("    ", 1) for s in sections("TWO", "THE INPUT")]
    stmts = sections("TWO", "THE STATEMENT")
    results = [s[0].strip() for s in sections("TWO", "THE RESULT")]
    y = 200
    for (term, sort), lines, res in zip(inputs, stmts, results):
        term, sort = term.strip(), sort.strip()
        typed = term if term != "kettle" else sort
        lines = [l.strip() for l in lines]
        h = 30 + 36 * len(lines)
        panel(d, 60, y, 1180, h, LINE)
        for j, runs in enumerate(statement_runs(lines, typed)):
            draw_runs(d, 80, y + 15 + j * 36, runs, 22)
        bad = "<-" in res
        c = ROSE if bad else AMBER
        fit_mono(d, 1270, y + h // 2 - 14, res.split("  <-")[0], c, 22, 600, "lm")
        if bad:
            label(d, 1270, y + h // 2 + 18, res.split("<- ")[1].lower(), c, 20, True, "lm")
        y += h + 30
    caption(d, "Red is the customer's text; grey is commented out, listed = 1 included.", AMBER, 920)


def parameter_slot(img, d, cfg):
    """The fixed statement with one slot, the three inputs as values, and the sort list."""
    lines = [l.strip() for l in sections("THREE", "THE STATEMENT")[0]]
    header(d, 60, 200, "THE STATEMENT (fixed text)", CYAN)
    panel(d, 60, 260, 1100, 40 + 44 * len(lines), CYAN, 3)
    for j, ln in enumerate(lines):
        runs = []
        for k, part in enumerate(ln.split("?")):
            if k:
                runs.append(("?", AMBER))
            runs.append((part, TEXT))
        draw_runs(d, 85, 280 + j * 44, runs, 26)
    vals = sections("THREE", "THE INPUTS AND RESULTS")[0]
    header(d, 60, 460, "THE VALUE OF ?   ->   THE RESULT", AMBER)
    for i in range(0, len(vals), 2):
        y = 520 + (i // 2) * 90
        cell(d, 60, y, 760, 64, vals[i].strip(), AMBER, 22)
        draw.arrow_right(d, 830, y + 32, 900, MUTED, 4)
        res = vals[i + 1].replace("->", "").strip()
        mono(d, 920, y + 32, res, GREEN, 24, "lm")
    sort = sections("THREE", "THE SORT BOX")[0]
    header(d, 1240, 200, "THE SORT BOX", GREEN)
    panel(d, 1240, 260, 620, 220, GREEN)
    label(d, 1265, 290, "the list:", MUTED, 22, True)
    mono(d, 1265, 330, "name   price", GREEN, 28)
    fit_mono(d, 1265, 390, sort[0].strip(), ROSE, 22, 580)
    mono(d, 1265, 430, sort[1].replace("->", "").strip(), GREEN, 22)
    caption(d, "The input fills the slot as a value; it can never become code.", AMBER, 900)


def db_users(img, d, cfg):
    """Which tables each database user may read, and what the same pasted statement reaches as each."""
    users = [ln.split(None, 1) for ln in sections("FOUR", "THE TWO DATABASE USERS")[0]]
    tables = [t.strip() for t in users[0][1].split(",")]
    header(d, 60, 200, "THE TWO DATABASE USERS", CYAN)
    for j, t in enumerate(tables):
        label(d, 380 + j * 200, 280, t, MUTED, 24, True, "mm")
    for i, (who, may) in enumerate(users):
        y = 320 + i * 100
        mono(d, 80, y + 35, who, TEXT, 28, "lm")
        allowed = [t.strip() for t in may.split(",")]
        for j, t in enumerate(tables):
            ok = t in allowed
            cell(d, 300 + j * 200, y, 160, 70, "read" if ok else "no", GREEN if ok else ROSE, 26)
    union = sections("FOUR", "THE UNION ATTACK")[0]
    header(d, 940, 200, "THE UNION ATTACK, STILL PASTED IN", AMBER)
    for i, ln in enumerate(union):
        who, res = ln.strip().split(None, 2)[1], ln.strip().split(None, 2)[2]
        y = 270 + i * 100
        c = GREEN if res.startswith("refused") else ROSE
        panel(d, 940, y, 920, 80, c, 3)
        mono(d, 960, y + 40, who, TEXT, 26, "lm")
        fit_mono(d, 1100, y + 40, res, c, 22, 740, "lm")
    quote = sections("FOUR", "THE QUOTE ATTACK")[0][0].strip()
    header(d, 940, 500, heading("FOUR", "THE QUOTE ATTACK").rstrip(":"), AMBER)
    panel(d, 940, 560, 920, 80, ROSE, 3)
    mono(d, 960, 600, quote, ROSE, 26, "lm")
    caption(d, "Few rights limit what a mistake can reach; only parameters fix it.", AMBER, 880)
