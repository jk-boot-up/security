"""Pictures for the SQL injection with Hibernate video, drawn by the `figure` slide kind.

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
    """Every section of an act whose header starts with `head`, as lists of lines (a wrapped line keeps its two-space indent)."""
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


def joined(lines):
    """A wrapped value back on one line: the demo wraps only at single spaces."""
    return " ".join(l.strip() for l in lines)


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


def runs(lines, marks, comment=False):
    """The lines as runs of (text, colour): each (substring, colour) in `marks` coloured wherever it occurs in the
    statement (also across a line break), and with `comment`, everything after -- greyed."""
    text = " ".join(lines)
    colours = [TEXT] * len(text)
    for sub, c in marks:
        for m in re.finditer(re.escape(sub), text):
            for k in range(m.start(), m.end()):
                colours[k] = c
    if comment and "--" in text:
        for k in range(text.rindex("--"), len(text)):
            colours[k] = MUTED
    out, pos = [], 0
    for ln in lines:
        row = []
        for i, ch in enumerate(ln):
            c = colours[pos + i]
            if row and row[-1][1] == c:
                row[-1][0] += ch
            else:
                row.append([ch, c])
        out.append(row)
        pos += len(ln) + 1
    return out


def draw_runs(d, x, y, row, size=26):
    f = font(MONO_B, size)
    for text, c in row:
        d.text((x, y), text, font=f, fill=c)
        x += f.getlength(text)


def statement(d, x, y, lines, marks, size, step, comment=False):
    for j, row in enumerate(runs([l.strip() for l in lines], marks, comment)):
        indent = lines[j][:len(lines[j]) - len(lines[j].lstrip())]
        draw_runs(d, x + font(MONO_B, size).getlength(indent), y + j * step, row, size)


# ------------------------------------------------------------------ figures

def jpql_to_sql(img, d, cfg):
    """The core idea on one example: Hibernate turns the JPQL it is given into SQL, and the customer's word rides along."""
    term = sections("ONE", "THE INPUT")[0][0].strip()
    jpql = sections("ONE", "THE JPQL AS BUILT")[0]
    sql = sections("ONE", "THE SQL HIBERNATE SENT")[0]
    bypass = joined(sections("TWO", "THE INPUT")[0][:-1])
    attacked = sections("TWO", "THE JPQL AS BUILT")[0]
    header(d, 80, 190, "THE JPQL AS BUILT (Java objects: Product, p.name)", AMBER)
    panel(d, 80, 245, 1760, 30 + 40 * len(jpql), AMBER, 3)
    statement(d, 110, 262, jpql, [("'%" + term + "%'", GREEN)], 26, 40)
    draw.arrow(d, 960, 380, 960, 420, MUTED, 4)
    label(d, 990, 385, "Hibernate translates", MUTED, 24, True)
    header(d, 80, 425, "THE SQL HIBERNATE SENT (tables: products, p1_0.name)", CYAN)
    panel(d, 80, 480, 1760, 30 + 40 * len(sql), CYAN, 3)
    statement(d, 110, 497, sql, [("'%" + term + "%'", GREEN), ("products", CYAN)], 26, 40)
    header(d, 80, 680, "THE SAME JPQL WITH A QUOTE IN THE SEARCH BOX", ROSE)
    panel(d, 80, 735, 1760, 30 + 36 * len(attacked), ROSE, 3)
    statement(d, 110, 750, attacked, [(bypass, ROSE)], 24, 36)
    caption(d, "Green stays inside the quotes: data. Red closed the quote: it is code.", AMBER, 935)


def products_and_users(img, d, cfg):
    """The two tables, the honest search, and the two rows it returns."""
    rows = sections("ONE", "THE TABLE products")[0]
    users = sections("ONE", "THE TABLE users")[0]
    result = sections("ONE", "THE RESULT")[0][0].strip()
    found = [n.strip() for n in result.strip("[]").split(",")]
    header(d, 60, 190, "THE TABLE products (entity Product)", CYAN)
    for j, h in enumerate(("name", "price (cents)", "listed")):
        label(d, (80, 470, 700)[j], 255, h, MUTED, 22, True)
    for i, ln in enumerate(rows):
        name, price, listed = re.match(r"(.+?)\s+(\d+)\s+(true|false)", ln).groups()
        y = 290 + i * 74
        c = GREEN if name in found else LINE
        panel(d, 60, y, 780, 60, c, 3 if name in found else 2)
        mono(d, 80, y + 30, name, TEXT if listed == "true" else MUTED, 26, "lm")
        mono(d, 560, y + 30, price, TEXT, 26, "rm")
        mono(d, 720, y + 30, listed, ROSE if listed == "false" else TEXT, 26, "mm")
    header(d, 60, 610, "THE TABLE users (entity Account)", CYAN)
    for i, ln in enumerate(users):
        email, hsh = ln.split()
        y = 675 + i * 74
        panel(d, 60, y, 780, 60, LINE)
        mono(d, 80, y + 30, email, TEXT, 24, "lm")
        mono(d, 820, y + 30, hsh, AMBER, 24, "rm")
    term, sort = [v.strip() for v in sections("ONE", "THE INPUT")[0]]
    header(d, 940, 190, "THE INPUT", AMBER)
    cell(d, 940, 250, 420, 64, "search: " + term, AMBER, 26)
    cell(d, 1400, 250, 440, 64, "sort: " + sort, AMBER, 26)
    header(d, 940, 350, "THE JPQL AS BUILT", ROSE)
    jpql = sections("ONE", "THE JPQL AS BUILT")[0]
    panel(d, 940, 410, 900, 40 + 40 * len(jpql), ROSE)
    statement(d, 960, 430, jpql, [("'%" + term + "%'", AMBER)], 22, 40)
    header(d, 940, 600, "THE RESULT", GREEN)
    mono(d, 940, 670, result, GREEN, 30)
    label(d, 940, 730, "the users table is never named by this page", MUTED, 26, True)
    caption(d, "Kettle stays inside the quotes; listed = true hides the toaster.", AMBER, 935)


def quote_breakout(img, d, cfg):
    """The three attacks: what the customer typed (red) inside what was built or sent, and what came back."""
    bypass = joined(sections("TWO", "THE INPUT")[0][:-1])
    jpql = sections("TWO", "THE JPQL AS BUILT")[0]
    hit = sections("TWO", "THE RESULT")[0][0].strip()
    oracle_sql = sections("TWO", "THE SQL HIBERNATE SENT")[0]
    answers = [a.strip() for a in sections("TWO", "THE ANSWERS")[0]]
    union = joined(sections("TWO", "THE INPUT")[2])
    native = sections("TWO", "THE SQL AS BUILT")[0]
    leaked = [r.strip() for r in sections("TWO", "THE RESULT")[1]]
    blocks = [
        ("1  QUOTE: THE JPQL AS BUILT", jpql, [(bypass, ROSE)], False, [hit]),
        ("2  JPQL ASKS ABOUT Account: THE SQL HIBERNATE SENT", oracle_sql,
         [("exists(select a1_0.id from users a1_0", ROSE), ("a1_0.password_hash like 'pbkdf2$ana%'", ROSE)], False, answers),
        ("3  NATIVE UNION: THE SQL AS BUILT", native, [(union[:-3], ROSE)], True, leaked),
    ]
    y = 180
    for title, lines, marks, comment, res in blocks:
        header(d, 60, y, title, AMBER, size=22)
        h = 20 + 32 * len(lines)
        panel(d, 60, y + 50, 1150, h, LINE)
        statement(d, 80, y + 60, lines, marks, 20, 32, comment)
        for k, r in enumerate(res):
            text, _, mark = r.partition("  <- ")
            c = MUTED if mark == "NO" else ROSE
            fit_mono(d, 1240, y + 70 + k * 64, text, c, 22, 620)
            if mark:
                label(d, 1240, y + 100 + k * 64, mark.lower(), c, 20, True)
        y += 50 + h + 24
    caption(d, "Red is the customer's text, now running as query language.", AMBER, 945)


def parameter_slot(img, d, cfg):
    """The fixed JPQL with :term, the SQL with ?, the payloads as values, and the sort list."""
    jpql = sections("THREE", "THE JPQL (")[0]
    sql = sections("THREE", "THE SQL HIBERNATE SENT")[0]
    header(d, 60, 180, "THE JPQL (fixed text)", CYAN)
    panel(d, 60, 235, 1120, 24 + 36 * len(jpql), CYAN, 3)
    statement(d, 80, 248, jpql, [(":term", AMBER)], 22, 36)
    header(d, 60, 400, "THE SQL HIBERNATE SENT", CYAN)
    panel(d, 60, 455, 1120, 24 + 36 * len(sql), CYAN, 3)
    statement(d, 80, 468, sql, [(" ? ", AMBER)], 22, 36)
    vals, cur = [], []
    for ln in sections("THREE", "THE VALUE OF ?")[0]:
        if ln.strip().startswith("->"):
            vals.append(([l.strip() for l in cur], ln.strip()[2:].strip()))
            cur = []
        else:
            cur.append(ln)
    header(d, 60, 610, "THE VALUE OF ?   ->   THE RESULT", AMBER)
    y = 668
    for v, res in vals:
        h = 22 + 32 * len(v)
        d.rounded_rectangle([60, y, 780, y + h], radius=10, fill=BG_CELL, outline=AMBER, width=3)
        for k, part in enumerate(v):
            fit_mono(d, 80, y + 11 + k * 32, part, AMBER, 22, 685)
        draw.arrow_right(d, 790, y + h // 2, 840, MUTED, 4)
        mono(d, 852, y + h // 2, res, GREEN, 22, "lm")
        y += h + 14
    sort = sections("THREE", "THE SORT BOX")[0]
    header(d, 1240, 180, "THE SORT BOX", GREEN)
    panel(d, 1240, 235, 620, 250, GREEN)
    label(d, 1265, 260, "the list:", MUTED, 22, True)
    mono(d, 1265, 300, "name   price", GREEN, 28)
    fit_mono(d, 1265, 360, sort[0].strip(), ROSE, 22, 580)
    fit_mono(d, 1265, 410, sort[1].replace("->", "").strip(), GREEN, 22, 580)
    header(d, 1240, 520, "NATIVE SQL AND CRITERIA", GREEN)
    for i, ln in enumerate(sections("THREE", "THE SAME PAYLOADS")[0]):
        what, res = [p.strip() for p in ln.split("->")]
        fit_mono(d, 1240, 585 + i * 90, what, TEXT, 22, 620)
        mono(d, 1240, 620 + i * 90, "-> " + res, GREEN, 22)
    caption(d, "The text fills the ? as a value; it can never become code.", AMBER, 945)


def db_users(img, d, cfg):
    """What each database user may read, and what the same pasted query reaches as each."""
    users = [ln.strip().split(None, 1) for ln in sections("FOUR", "THE TWO DATABASE USERS")[0]]
    tables = ["products", "users"]
    header(d, 60, 190, "THE TWO DATABASE USERS", CYAN)
    for j, t in enumerate(tables):
        label(d, 450 + j * 200, 270, t, MUTED, 24, True, "mm")
    for i, (who, may) in enumerate(users):
        y = 310 + i * 100
        mono(d, 80, y + 35, who, TEXT, 26, "lm")
        for j, t in enumerate(tables):
            ok = "every table" in may or t in [m.strip() for m in may.split(",")]
            cell(d, 370 + j * 200, y, 160, 70, "read" if ok else "no", GREEN if ok else ROSE, 26)
    union = sections("FOUR", "THE UNION ATTACK")[0]
    header(d, 860, 190, "THE UNION ATTACK, STILL PASTED IN", AMBER)
    for i, ln in enumerate(union):
        _, who, res = ln.strip().split(None, 2)
        y = 255 + i * 90
        c = GREEN if res.startswith("refused") else ROSE
        panel(d, 860, y, 1000, 74, c, 3)
        mono(d, 880, y + 37, who, TEXT, 24, "lm")
        fit_mono(d, 1080, y + 37, res, c, 22, 760, "lm")
    hash_q = sections("FOUR", "THE HASH QUESTION")[0][0].strip()
    header(d, 860, 450, "THE HASH QUESTION, AS shop_search", AMBER)
    panel(d, 860, 510, 1000, 74, GREEN, 3)
    fit_mono(d, 880, 547, hash_q, GREEN, 22, 960, "lm")
    quote = sections("FOUR", "THE QUOTE ATTACK")[0][0].strip()
    header(d, 860, 620, "THE QUOTE ATTACK, AS shop_search", AMBER)
    panel(d, 860, 680, 1000, 74, ROSE, 3)
    mono(d, 880, 717, quote, ROSE, 24, "lm")
    caption(d, "Few rights limit what a mistake can reach; only parameters fix it.", AMBER, 900)
