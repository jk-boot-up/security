"""Pictures for the secrets in errors and logs video, drawn by the `figure` slide kind.

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


def canary_rows(name):
    """(secret, in page, in log) from an act's CANARY SEARCH section."""
    rows = [re.split(r"\s{2,}", ln.strip()) for ln in sections(name, "CANARY SEARCH")[0][1:]]
    return [(r[0], r[1], r[2]) for r in rows]


def secrets():
    """The fake secrets, as they appear in the request and in what a visitor learns."""
    req = dict(facts(sections("ONE", "THE REQUEST")[0]))
    learned = dict(facts(sections("ONE", "WHAT A VISITOR LEARNS")[0]))
    return [req["card"], "cvv=" + req["cvv"], req["token"], learned["password"]]


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


def canary_table(d, x, y, name, colour_yes=ROSE):
    """The canary search of an act: one row per fake secret, a yes/no cell for the page and the log."""
    label(d, x + 300, y, "page", MUTED, 24, True, "ma")
    label(d, x + 440, y, "log", MUTED, 24, True, "ma")
    for i, (secret, page, log) in enumerate(canary_rows(name)):
        yy = y + 40 + i * 62
        label(d, x, yy + 26, secret, TEXT, 26, True, "lm")
        for cx, v in ((x + 240, page), (x + 380, log)):
            cell(d, cx, yy, 120, 52, v, colour_yes if v == "yes" else GREEN, 26)


def log_block(d, x, y, entries, marks, size=22, step=34, max_w=1200):
    """Numbered log lines; continuation parts of the same line are indented under it."""
    for num, parts in entries:
        label(d, x, y + 2, num, CYAN, size + 2, True)
        for p in parts:
            marked(d, x + 40, y, p, size, marks)
            y += step
        y += 8
    return y


# ------------------------------------------------------------------ figures

def one_failure(img, d, cfg):
    """One exception, and where its database password ends up, before and after the fix."""
    page = sections("ONE", "THE RESPONSE")[0]
    learned = dict(facts(sections("ONE", "WHAT A VISITOR LEARNS")[0]))
    pw = "password=" + learned["password"]
    header(d, 60, 180, "THE EXCEPTION (the database is down)", AMBER)
    for i, ln in enumerate(page[:4]):
        marked(d, 80, 245 + i * 40, ln.strip(), 26, {pw: ROSE})
    naive_log = log_lines(sections("TWO", "THE LOG FILE")[0])[1][1]
    fixed_page = sections("THREE", "THE RESPONSE")[0][0].strip()
    fixed_log = log_lines(sections("THREE", "THE LOG FILE")[0])[1][1]
    naive_rows = dict((s, (p, l)) for s, p, l in canary_rows("TWO"))
    fixed_rows = dict((s, (p, l)) for s, p, l in canary_rows("THREE"))
    cols = [("BEFORE THE FIX", ROSE, [page[1].strip(), page[2].strip()], naive_log[1:3], naive_rows["db password"]),
            ("AFTER THE FIX", GREEN, [fixed_page], fixed_log[:3], fixed_rows["db password"])]
    for j, (title, colour, page_lines, log, (in_page, in_log)) in enumerate(cols):
        x = 60 + j * 920
        header(d, x, 430, title, colour)
        panel(d, x, 490, 880, 410, colour)
        label(d, x + 25, 505, "the page: the customer, and anyone", MUTED, 24, True)
        for i, ln in enumerate(page_lines):
            marked(d, x + 25, 545 + i * 38, ln, 23, {pw: ROSE, "ref-0001": CYAN, "[REDACTED]": GREEN})
        label(d, x + 25, 650, "the log: support, vendors, backups", MUTED, 24, True)
        for i, ln in enumerate(log):
            marked(d, x + 25, 690 + i * 38, ln, 23, {pw: ROSE, "[ref-0001]": CYAN, "[REDACTED]": GREEN})
        label(d, x + 25, 838, "db password in", MUTED, 24, True)
        for k, (where, v) in enumerate((("page", in_page), ("log", in_log))):
            label(d, x + 280 + k * 270, 838, where, TEXT, 24, True)
            cell(d, x + 360 + k * 270, 824, 110, 50, v, ROSE if v == "yes" else GREEN, 24)
    caption(d, "The page gets a reference; the detail goes to the log, with the password masked.", AMBER, 935)


def page_gives_away(img, d, cfg):
    """The naive error page, and what a visitor reads out of it."""
    page = sections("ONE", "THE RESPONSE")[0]
    learned = facts(sections("ONE", "WHAT A VISITOR LEARNS")[0])
    values = {v: (ROSE if k == "password" else AMBER) for k, v in learned if k != "code"}
    values.update({"Database.saveOrder": AMBER, "Checkout.pay": AMBER})
    header(d, 60, 180, heading("ONE", "THE RESPONSE"), ROSE)
    panel(d, 60, 240, 1800, 300, ROSE)
    for i, ln in enumerate(page):
        marked(d, 90, 258 + i * 44, ln.strip(), 28, values)
    header(d, 60, 575, heading("ONE", "WHAT A VISITOR LEARNS"), AMBER)
    for i, (k, v) in enumerate(learned):
        x, y = 60 + (i % 2) * 920, 640 + (i // 2) * 72
        label(d, x, y + 28, k, MUTED, 26, True, "lm")
        cell(d, x + 170, y, 700, 56, v, ROSE if k == "password" else AMBER, 26)
    caption(d, "One failed payment gives a visitor the host, the user, the password and the code.", AMBER, 945)


def log_keeps(img, d, cfg):
    """The naive log with its secrets marked, the canary search, and a forged line."""
    marks = {s: ROSE for s in secrets()}
    header(d, 60, 180, heading("TWO", "THE LOG FILE"), ROSE)
    log_block(d, 60, 245, log_lines(sections("TWO", "THE LOG FILE")[0]), marks, 24, 36)
    header(d, 1340, 180, "CANARY SEARCH", AMBER)
    canary_table(d, 1340, 245, "TWO")
    req = dict(facts(sections("TWO", "THE REQUEST")[0]))
    header(d, 60, 575, "THE REQUEST: a name with a line break (\\n)", AMBER)
    marked(d, 80, 638, "name = " + req["name"], 26, {"\\n": AMBER})
    forged = log_lines(sections("TWO", "THE LOG FILE")[1])
    header(d, 60, 700, heading("TWO", "THE LOG FILE", 1), ROSE)
    log_block(d, 60, 765, forged, {"\\n": AMBER, "INFO login succeeded user=admin ip=10.0.0.1": ROSE}, 26, 40)
    label(d, 1340, 812, "line 2 looks like a real login", ROSE, 30, True)
    caption(d, "Every fake secret is in the log, and a customer wrote a line of their own.", AMBER, 945)


def fixed_outputs(img, d, cfg):
    """The fixed page and log, linked by the reference, the canary search and the forged name on one line."""
    marks = {"ref-0001": CYAN, "[REDACTED]": GREEN, "\\n": AMBER}
    header(d, 60, 180, heading("THREE", "THE RESPONSE"), GREEN)
    marked(d, 80, 245, sections("THREE", "THE RESPONSE")[0][0].strip(), 28, marks)
    header(d, 60, 310, heading("THREE", "THE LOG FILE"), GREEN)
    log_block(d, 60, 375, log_lines(sections("THREE", "THE LOG FILE")[0]), marks, 25, 38)
    how = dict(facts(sections("THREE", "HOW SUPPORT FINDS")[0]))
    label(d, 60, 625, "the customer quotes %s, support finds log line %s" % (how["the customer quotes"],
                                                                            how["the log line with it"]), CYAN, 28, True)
    header(d, 1340, 180, "CANARY SEARCH", GREEN)
    canary_table(d, 1340, 245, "THREE")
    header(d, 60, 700, heading("THREE", "THE LOG FILE", 1), GREEN)
    log_block(d, 60, 765, log_lines(sections("THREE", "THE LOG FILE")[1]), marks, 28, 44)
    caption(d, "No secret in the page or the log, and the forged name stays on one line.", AMBER, 945)


def luhn_and_rules(img, d, cfg):
    """The Luhn check worked out for a and b, and what each rule did to the samples."""
    luhn = facts(sections("FOUR", "HOW THE LUHN CHECK")[0])
    header(d, 60, 180, heading("FOUR", "HOW THE LUHN CHECK"), CYAN)
    for n in range(2):
        digits, doubled, total = (v for _, v in luhn[n * 3:n * 3 + 3])
        dg, db = digits.split(), doubled.split()
        y = 245 + n * 150
        label(d, 60, y + 25, "a" if n == 0 else "b", CYAN, 34, True, "lm")
        for i, (a, b) in enumerate(zip(dg, db)):
            changed = (len(dg) - i) % 2 == 0
            cell(d, 110 + i * 62, y, 54, 50, a, LINE, 26, text_colour=TEXT)
            cell(d, 110 + i * 62, y + 58, 54, 50, b, AMBER if changed else LINE, 26, text_colour=AMBER if changed else TEXT)
        ok = "not" not in total
        label(d, 1110, y + 25, "digits", MUTED, 24, True, "lm")
        label(d, 1110, y + 83, "every 2nd from right x2", AMBER, 24, True, "lm")
        cell(d, 1450, y + 10, 410, 56, "sum " + total.split(",")[0], GREEN if ok else ROSE, 30)
        label(d, 1655, y + 78, total.split(", ", 1)[1], GREEN if ok else ROSE, 24, True, "ma")
    before = sections("FOUR", "BEFORE REDACTION")[0]
    after = sections("FOUR", "AFTER REDACTION")[0]
    rules = sections("FOUR", "THE RULE THAT MASKED IT")[0]
    header(d, 60, 555, "BEFORE REDACTION", AMBER)
    header(d, 890, 555, "AFTER REDACTION", GREEN)
    header(d, 1590, 555, "THE RULE", CYAN)
    for i, (b, a, r) in enumerate(zip(before, after, rules)):
        y = 618 + i * 44
        hit = "none" not in r
        fit_mono(d, 60, y, b.strip(), TEXT, 23, 810)
        fit_mono(d, 890, y, a.strip()[3:], GREEN if hit else AMBER, 23, 680)
        fit_mono(d, 1590, y, r.strip()[3:].split(":")[0], GREEN if hit else AMBER, 23, 290)
    caption(d, "b is rightly left alone; g is a secret with no name and no shape, and is missed.", AMBER, 945)
