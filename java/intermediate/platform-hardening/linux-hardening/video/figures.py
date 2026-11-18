"""Pictures for the Linux hardening video, drawn by the `figure` slide kind.

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
SEV = {"CRITICAL": ROSE, "HIGH": ROSE, "MEDIUM": AMBER, "LOW": MUTED}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The first section of an act whose header starts with `head`, as lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = [] if ln.startswith(head) and not found else None
            if cur is not None:
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[0]


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def facts(lines):
    """`key   value` lines as a dict."""
    return dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in lines if re.search(r"\s{2,}", ln.strip()))


def report(lines):
    """Report lines as (severity, rule, subject, auto)."""
    rows = []
    for ln in lines:
        p = ln.split()
        if p and p[0] in SEV:
            rows.append((p[0], p[1], p[2] + (" " + p[3] if len(p) > 3 and p[3] != "auto" else ""), p[-1] == "auto"))
    return rows


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


def caption(d, text, colour=AMBER, y=935):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


# ------------------------------------------------------------------ figures

def mode_digits(img, d, cfg):
    """Two file modes, digit by digit: what each digit means, and what the policy allows."""
    rows = {}
    for ln in section("ONE", "HOW A MODE IS READ"):
        key = ln[:9].strip()
        rows[key] = (ln[9:30].strip(), ln[30:].strip())
    parts = ["special", "user", "group", "other"]
    for i in range(2):
        x0 = 60 + i * 930
        path, mode = rows["file"][i], rows["mode"][i]
        bad = i == 0
        end = header(d, x0, 185, "THE FILE", CYAN)
        fit_mono(d, end + 24, 207, path, TEXT, 34, 820 - end + x0, "lm")
        label(d, x0, 265, "mode", MUTED, 28, True)
        fit_mono(d, x0 + 110, 262, mode, ROSE if bad else AMBER, 48, 300)
        header(d, x0, 345, "WHAT EACH DIGIT MEANS", AMBER)
        for j, part in enumerate(parts):
            digit, meaning = rows[part][i].split(" ", 1)
            y = 410 + j * 100
            label(d, x0, y + 40, part, MUTED, 28, True, "lm")
            hot = (part == "special" and digit != "0") or (not bad and part == "other" and "r" in meaning)
            cell(d, x0 + 160, y, 90, 80, digit, ROSE if hot else TEXT, 44)
            draw.arrow_right(d, x0 + 265, y + 40, x0 + 340, MUTED, 4)
            cell(d, x0 + 355, y, 470, 80, meaning, ROSE if hot else GREEN, 34)
        verdict = rows["policy"][i]
        label(d, x0, 830, "policy: " + verdict, ROSE, 30, True)
    d.line([960, 190, 960, 880], fill=LINE, width=3)
    caption(d, "rwx = read, write, execute; set-user-id runs a program with its owner's rights.", AMBER, 920)


def report_grid(img, d, cfg):
    """The twenty findings, coloured by severity; the ones a machine can fix are marked."""
    rows = report(section("ONE", "THE REPORT"))
    count = facts(section("ONE", "THE COUNT"))
    header(d, 60, 180, "THE REPORT: %d FINDINGS, WORST FIRST" % len(rows), ROSE)
    for k, (sev, rule, subject, auto) in enumerate(rows):
        x, y = 60 + (k % 4) * 455, 245 + (k // 4) * 105
        colour = SEV[sev]
        d.rounded_rectangle([x, y, x + 435, y + 92], radius=10, fill=BG_CELL, outline=GREEN if auto else colour, width=5 if auto else 3)
        fit_mono(d, x + 14, y + 28, rule, colour, 24, 300, "lm")
        label(d, x + 421, y + 28, sev.lower(), colour, 20, True, "rm")
        fit_mono(d, x + 14, y + 66, subject, TEXT, 20, 330, "lm")
        if auto:
            label(d, x + 421, y + 66, "auto", GREEN, 22, True, "rm")
    label(d, 60, 772, "severity    " + count["severity"], TEXT, 30, True)
    label(d, 60, 815, "a machine  " + count["machine"], GREEN, 30, True)
    label(d, 60, 858, "a person    " + count["person"], AMBER, 30, True)
    caption(d, "Green border: a permission a machine can fix. The rest needs a person.", AMBER, 920)


def fix_math(img, d, cfg):
    """Each permission fix: the mode before, the bits taken away or kept, and the mode after."""
    fixes = []
    for ln in section("TWO", "HOW EACH NEW MODE"):
        m = re.match(r"(\S+)\s+(\d{4}) ([-&]) (\d{4}) = (\d{4})\s+(.*)", ln.strip())
        fixes.append(m.groups())
    after = facts(section("TWO", "AFTER THE FIX"))
    header(d, 60, 180, "BEFORE THE FIX", ROSE, 380)
    header(d, 900, 180, "THE CHANGE", AMBER, 260)
    header(d, 1245, 180, "AFTER THE FIX", GREEN, 330)
    for k, (path, before, op, bits, new, why) in enumerate(fixes):
        y = 250 + k * 82
        fit_mono(d, 60, y + 34, path, TEXT, 26, 600, "lm")
        cell(d, 670, y, 140, 68, before, ROSE, 32)
        label(d, 900, y + 34, "remove" if op == "-" else "keep only", MUTED, 22, True, "mm")
        cell(d, 990, y, 160, 68, op + " " + bits, AMBER, 30)
        draw.arrow_right(d, 1165, y + 34, 1230, MUTED, 4)
        cell(d, 1245, y, 140, 68, new, GREEN, 32)
        label(d, 1410, y + 34, why, GREEN, 26, True, "lm")
    label(d, 60, 840, "changes " + after["changes"] + "     findings " + after["findings"], TEXT, 30, True)
    caption(d, "Each digit is three bits: write for others is 2, for the group 20, set-user-id 4000.", AMBER, 920)


def port_decode(img, d, cfg):
    """How the checker reads an account line and the kernel's port table, in hex."""
    acc = section("THREE", "HOW THE ACCOUNT LINES")
    header(d, 60, 180, "AN ACCOUNT LINE", CYAN)
    line = acc[0].strip()
    fields = line.split(":")
    f = font(MONO_B, 34)
    x = 60
    for i, field in enumerate(fields):
        colour = ROSE if i in (0, 2) else TEXT
        d.text((x, 250), field, font=f, fill=colour)
        x += f.getlength(field)
        if i < len(fields) - 1:
            d.text((x, 250), ":", font=f, fill=LINE)
            x += f.getlength(":")
    label(d, 60, 305, acc[1].strip(), ROSE, 26, True)
    fit_mono(d, 1000, 250, acc[2].strip(), TEXT, 34, 860)
    label(d, 1000, 305, acc[3].strip(), ROSE, 26, True)
    header(d, 60, 370, "THE PORT TABLE, HEX TO NUMBER", AMBER)
    cols = [(60, "local address"), (420, "address"), (720, "hex port"), (1080, "port"), (1260, "verdict")]
    for x, t in cols:
        label(d, x, 432, t, MUTED, 24, True)
    for k, ln in enumerate(section("THREE", "HOW THE PORT TABLE")):
        m = re.match(r"(\S+)\s+(\S+)\s+(0x\S+) = (\d+)\s+(.*)", ln.strip())
        local, addr, hexp, port, verdict = m.groups()
        y = 470 + k * 68
        colour = ROSE if verdict == "not allowed" else GREEN if verdict == "allowed" else CYAN
        fit_mono(d, 60, y + 28, local, TEXT, 28, 340, "lm")
        fit_mono(d, 420, y + 28, addr, CYAN if addr != "0.0.0.0" else TEXT, 28, 260, "lm")
        fit_mono(d, 720, y + 28, hexp, MUTED, 28, 200, "lm")
        draw.arrow_right(d, 930, y + 28, 1040, MUTED, 3)
        cell(d, 1060, y, 150, 56, port, colour, 28)
        label(d, 1260, y + 28, verdict, colour, 28, True, "lm")
    caption(d, "0.0.0.0 is every interface; 0100007F, read backwards, is 127.0.0.1: this host only.", AMBER, 935)


def regression(img, d, cfg):
    """A clean image passes the build; a later change brings two findings back and fails it."""
    rule = facts(section("FOUR", "THE BUILD RULE"))["fail at"]
    clean = facts(section("FOUR", "THE HARDENED IMAGE"))["findings"]
    change = [ln.strip() for ln in section("FOUR", "A LATER CHANGE")]
    scan = section("FOUR", "THE SCAN IN THE BUILD")
    found = report(scan)
    result = facts(scan[len(found):])["findings"]
    header(d, 60, 180, "THE BUILD RULE: fail at " + rule, AMBER)
    boxes = [("THE HARDENED IMAGE", ["findings " + clean.split(" -> ")[0]], GREEN, clean.split(" -> ")[1]),
             ("A LATER CHANGE", change, AMBER, None),
             ("THE SCAN IN THE BUILD", ["%s %s" % (r, s) for _, r, s, _ in found], ROSE, result.split(" -> ")[1])]
    for k, (title, lines, colour, verdict) in enumerate(boxes):
        x = 60 + k * 620
        panel(d, x, 260, 560, 600, colour, 3)
        label(d, x + 280, 290, title, colour, 30, True, "ma")
        y = 370
        for ln in lines:
            text, rows = "", []
            for w in ln.split():
                if len(text + " " + w) > 22:
                    rows.append(text)
                    text = w
                else:
                    text = (text + " " + w).strip()
            rows.append(text)
            for t in rows:
                fit_mono(d, x + 30, y, t, TEXT, 32, 500)
                y += 46
            y += 30
        if verdict:
            cell(d, x + 105, 760, 350, 70, verdict, ROSE if "FAILS" in verdict else GREEN, 34)
        if k < 2:
            draw.arrow_right(d, x + 568, 560, x + 612, MUTED, 5)
    caption(d, "The same scan runs in every build, so a regression fails it the day it appears.", AMBER, 920)
