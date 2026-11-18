"""Pictures for the secrets in logs with Logback video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

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
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip())]


def log_lines(lines):
    """A THE LOG FILE section as a list of (number, [wrapped parts])."""
    out = []
    for ln in lines:
        m = re.match(r"(\d+) (.*)", ln)
        if m:
            out.append((m.group(1), [m.group(2)]))
        else:
            out[-1][1].append(ln.strip())
    return out


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


def marked(d, x, y, text, size, marks, base=TEXT):
    """A mono line with every occurrence of the strings in `marks` ({text: colour}) drawn in their colour."""
    f = font(MONO_B, size)
    pos = 0
    while pos < len(text):
        hits = [(text.find(m, pos), m) for m in marks if text.find(m, pos) >= 0]
        k, m = min(hits) if hits else (len(text), "")
        d.text((x, y), text[pos:k], font=f, fill=base)
        x += f.getlength(text[pos:k])
        if m:
            d.text((x, y), m, font=f, fill=marks[m])
            x += f.getlength(m)
        pos = k + len(m)


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def log_block(d, x, y, entries, marks, size=22, step=34, max_w=1200):
    """Numbered log lines; continuation parts of the same line are indented under it."""
    for num, parts in entries:
        label(d, x, y + 2, num, CYAN, size + 2, True)
        for p in parts:
            marked(d, x + 40, y, p, size, marks)
            y += step
        y += 8
    return y



def found_table(d, x, y, rows, w_name=270, w_cell=200, step=62):
    """(name, verdict) rows; a verdict that means a leak is rose, the others green."""
    for i, (name, v) in enumerate(rows):
        yy = y + i * step
        label(d, x, yy + 26, name, TEXT, 26, True, "lm")
        bad = v in ("found", "missed")
        cell(d, x + w_name, yy, w_cell, 52, v, ROSE if bad else GREEN, 26)


def luhn_rows(d, x, y, name, rows):
    """Digit cells for one number: the digits, then what each adds (doubled ones amber), and the sum."""
    digits, steps, total = (v for _, v in rows)
    dg, st = digits.split(), steps.split()
    label(d, x, y + 23, name, CYAN, 26, True, "lm")
    for i, (a, b) in enumerate(zip(dg, st)):
        doubled = (len(dg) - i) % 2 == 0
        cell(d, x + 150 + i * 56, y, 50, 46, a, LINE, 24, text_colour=TEXT)
        cell(d, x + 150 + i * 56, y + 52, 50, 46, b, AMBER if doubled else LINE, 24, text_colour=AMBER if doubled else TEXT)
    ok = "not" not in total
    cell(d, x + 1080, y + 8, 300, 52, "sum " + total.split(",")[0], GREEN if ok else ROSE, 28)
    label(d, x + 1230, y + 70, total.split(", ", 1)[1], GREEN if ok else ROSE, 22, True, "ma")


# ------------------------------------------------------------------ figures

def four_ways(img, d, cfg):
    """One card number through the four ways of the video, each value read from the output."""
    plain = re.search(r"card=([\d ]{19})", OUT).group(0)
    masked = re.search(r"card=\[CARD \*{4}\d{4}\]", OUT).group(0)
    typed = dict(facts(sections("THREE", "HOW EACH ONE PRINTS")[0]))["Card"].split()[0]
    after = sections("FOUR", "AFTER THE MASKING")[0]
    json_masked = re.search(r'"number":"[^"]+"', "".join(after)).group(0)
    header(d, 60, 180, "THE INPUT: one test card number", AMBER)
    d.text((80, 245), plain.split("=")[1], font=font(MONO_B, 44), fill=ROSE)
    rows = [("1  a plain layout", "%level %msg", plain, ROSE),
            ("2  a mask in the layout", "%level %mask", masked, GREEN),
            ("3  a type that prints safely", "Card.toString()", typed, GREEN),
            ("4  JSON and a decorator", "path /card/number", json_masked, GREEN)]
    header(d, 60, 340, "FOUR WAYS", CYAN)
    header(d, 1180, 340, "WHAT THE LOG FILE GETS", GREEN)
    for i, (way, how, value, colour) in enumerate(rows):
        y = 410 + i * 120
        panel(d, 60, y, 1800, 100, colour)
        label(d, 90, y + 32, way, TEXT, 30, True, "lm")
        fit_mono(d, 90, y + 72, how, MUTED, 24, 900, "lm")
        cell(d, 1180, y + 22, 650, 58, value, colour, 28)
    caption(d, "Only the type keeps the number out without knowing what a card looks like.", AMBER, 905)


def plain_log(img, d, cfg):
    """The three log calls, the four lines a plain layout writes, and the canary search."""
    events = sections("ONE", "THE EVENTS")[0]
    header(d, 60, 180, heading("ONE", "THE EVENTS"), CYAN)
    for i, ln in enumerate(events):
        marked(d, 80, 240 + i * 34, ln.strip(), 23, {"\\n": AMBER})
    marks = {"4111 1111 1111 1111": ROSE, "cvv=737": ROSE, "s3cr3t-db-password": ROSE,
             "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2lnbmF0dXJl": ROSE,
             "INFO login succeeded user=admin ip=10.0.0.1": ROSE}
    header(d, 60, 400, heading("ONE", "THE LOG FILE"), ROSE)
    log_block(d, 60, 462, log_lines(sections("ONE", "THE LOG FILE")[0]), marks, 23, 36)
    rows = facts(sections("ONE", "CANARY SEARCH")[0])
    header(d, 1340, 400, "CANARY SEARCH", AMBER)
    found_table(d, 1340, 462, rows[:-1])
    label(d, 1340, 735, rows[-1][1], ROSE, 32, True)
    caption(d, "Every fake secret is in the file, and the customer wrote line 4.", AMBER, 945)


def mask_layout(img, d, cfg):
    """The same calls through %mask and %maskedEx, the canary search, and the Luhn check that tells a card."""
    marks = {"[CARD ****1111]": GREEN, "[REDACTED]": GREEN, "\\n": AMBER}
    header(d, 60, 180, heading("TWO", "THE LOG FILE"), GREEN)
    log_block(d, 60, 242, log_lines(sections("TWO", "THE LOG FILE")[0]), marks, 23, 34)
    rows = facts(sections("TWO", "CANARY SEARCH")[0])
    header(d, 1340, 180, "CANARY SEARCH", GREEN)
    found_table(d, 1340, 242, rows[:-1], w_cell=240, step=58)
    label(d, 1340, 485, rows[-1][1], GREEN, 30, True)
    luhn = facts(sections("TWO", "HOW THE LUHN CHECK")[0])
    header(d, 60, 585, heading("TWO", "HOW THE LUHN CHECK"), CYAN)
    luhn_rows(d, 60, 650, "card", luhn[0:3])
    luhn_rows(d, 60, 790, "order", luhn[3:6])
    caption(d, "Amber digits are doubled; a sum ending in 0 is a card, so the order number stays.", AMBER, 945)


def types_print(img, d, cfg):
    """What the code holds, how each value prints, and the log file with no masking."""
    held = facts(sections("THREE", "THE OBJECTS")[0])
    prints = facts(sections("THREE", "HOW EACH ONE PRINTS")[0])
    header(d, 60, 180, heading("THREE", "THE OBJECTS"), CYAN)
    for i, (k, v) in enumerate(held):
        y = 245 + i * 62
        label(d, 80, y + 26, k, MUTED, 26, True, "lm")
        cell(d, 220, y, 680, 52, v, ROSE if "4111" in v or "737" in v else TEXT, 26)
    header(d, 1000, 180, heading("THREE", "HOW EACH ONE PRINTS"), GREEN)
    for i, (k, v) in enumerate(prints):
        y = 245 + i * 82
        label(d, 1020, y + 26, k, MUTED, 26, True, "lm")
        value, _, note = v.partition("  ")
        cell(d, 1180, y, 300, 52, value, AMBER if k == "reveal" else GREEN, 28)
        label(d, 1500, y + 26, note.strip("()"), MUTED, 22, False, "lm")
    marks = {"****1111": GREEN, "[REDACTED]": GREEN, "ref-0001": CYAN}
    header(d, 60, 530, heading("THREE", "THE LOG FILE"), GREEN)
    y = log_block(d, 60, 592, log_lines(sections("THREE", "THE LOG FILE")[0]), marks, 25, 38)
    header(d, 60, y + 10, heading("THREE", "A LIBRARY'S EXCEPTION"), CYAN)
    log_block(d, 60, y + 72, log_lines(sections("THREE", "A LIBRARY'S EXCEPTION")[0]), marks, 25, 38)
    caption(d, "No masking at all: the secret never reaches the line.", AMBER, 950)


def json_events(img, d, cfg):
    """The forged name as JSON, a record written as its fields, and the masking decorator."""
    marks = {"\\n": AMBER, "4111111111111111": ROSE, '"cvv":737': ROSE, "****1111": GREEN, "[REDACTED]": GREEN}
    header(d, 60, 175, "A FORGED NAME AS JSON: 1 event, 1 line", AMBER)
    y = log_block(d, 60, 232, log_lines(sections("FOUR", "JSON EVENTS")[0])[:1], marks, 23, 33)
    header(d, 60, y + 4, heading("FOUR", "A RECORD AS JSON"), ROSE)
    y = log_block(d, 60, y + 62, log_lines(sections("FOUR", "A RECORD AS JSON")[0]), marks, 23, 33)
    header(d, 60, y + 4, heading("FOUR", "AFTER THE MASKING"), GREEN)
    after = log_lines(sections("FOUR", "AFTER THE MASKING")[0])
    y = log_block(d, 60, y + 62, after[:1], marks, 23, 33)
    pw = next(p for p in after[1][1] if "password" in p)
    marked(d, 100, y, "and in line 2: " + pw, 23, marks)
    caption(d, "A record is written as its fields; paths and shapes cover what types did not.", AMBER, 950)


def mask_misses(img, d, cfg):
    """A sentence the mask leaves as it was, what the mask knows, and the order of defence."""
    before = " ".join(ln.strip() for ln in sections("FIVE", "BEFORE MASKING")[0])
    after = " ".join(ln.strip() for ln in sections("FIVE", "AFTER MASKING")[0])
    marks = {"4921": ROSE, "hunter2": ROSE, "078-05-1120": ROSE}
    header(d, 60, 180, "BEFORE MASKING", AMBER)
    marked(d, 80, 240, before, 26, marks)
    header(d, 60, 300, "AFTER MASKING", ROSE)
    marked(d, 80, 360, after, 26, marks)
    header(d, 60, 440, heading("FIVE", "WHAT THE MASK KNOWS").split(" (")[0], CYAN)
    found_table(d, 60, 500, facts(sections("FIVE", "WHAT THE MASK KNOWS")[0]), w_name=260, w_cell=220, step=58)
    header(d, 1000, 440, heading("FIVE", "THE ORDER OF DEFENCE"), GREEN)
    for i, ln in enumerate(sections("FIVE", "THE ORDER OF DEFENCE")[0]):
        num, text = ln.strip().split(" ", 1)
        y = 510 + i * 95
        cell(d, 1000, y, 64, 64, num, GREEN, 32)
        label(d, 1090, y + 32, text, TEXT, 28, True, "lm")
    caption(d, "A mask cannot cover a shape it has never seen.", AMBER, 940)
