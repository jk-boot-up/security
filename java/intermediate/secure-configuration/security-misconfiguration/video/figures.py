"""Pictures for the security misconfiguration video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B,
                                   TEXT, W, font)

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


def section(name, head):
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
    while f.getlength(text) > max_w and f.size > 16:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def settings(lines):
    """key=value lines as an ordered dict."""
    out = {}
    for ln in lines:
        k, _, v = ln.strip().partition("=")
        out[k] = v
    return out


def marked(text):
    """('200  welcome, admin', 'ANYONE IS ADMIN') from '200  welcome, admin  <- ANYONE IS ADMIN'."""
    body, _, mark = text.partition("  <- ")
    return body.strip(), mark.strip()


def checks(name):
    """The checker's lines of one act as (verdict, check, seen)."""
    return [(ln[:4], ln[6:26].strip(), ln[26:].strip()) for ln in section(name, "THE 8 CHECKS")]


# ------------------------------------------------------------------ figures

def two_files(img, d, cfg):
    """The shipped settings file next to the hardened one, key by key."""
    shipped = settings(section("ONE", "THE SETTINGS FILE"))
    hardened = settings(section("TWO", "THE SETTINGS FILE"))
    header(d, 80, 200, "AS SHIPPED (development)", ROSE, 820)
    header(d, 1020, 200, "HARDENED (production)", GREEN, 820)
    for i, key in enumerate(shipped):
        y = 270 + i * 76
        panel(d, 80, y, 820, 62, ROSE if key != "profile" else LINE)
        fit_mono(d, 100, y + 31, "%s=%s" % (key, shipped[key]), TEXT, 28, 780, "lm")
        draw.arrow_right(d, 912, y + 31, 1008, MUTED, 4)
        if key in hardened:
            panel(d, 1020, y, 820, 62, GREEN if key != "profile" else LINE)
            fit_mono(d, 1040, y + 31, "%s=%s" % (key, hardened[key]), TEXT, 28, 780, "lm")
        else:
            panel(d, 1020, y, 820, 62, LINE)
            label(d, 1040, y + 31, "removed: no test settings in production", MUTED, 26, True, "lm")
    caption(d, "Same store, same keys: every risky switch is stated, and off.", AMBER, 900)


def scanner_probes(img, d, cfg):
    """The four requests a scanner tries first, and what the shipped store answers."""
    reqs = [ln.strip() for ln in section("ONE", "THE REQUESTS")]
    replies = []
    for ln in section("ONE", "THE REPLIES"):
        if ln[:1].isdigit():
            replies.append(ln.strip())
        else:
            replies[-1] += " " + ln.strip()
    header(d, 60, 200, "THE REQUEST", AMBER, 700)
    header(d, 860, 200, "THE REPLY (status, and what the page shows)", ROSE, 1000)
    for i, (req, rep) in enumerate(zip(reqs, replies)):
        y = 280 + i * 140
        cell(d, 60, y, 700, 100, req, AMBER, 30)
        draw.arrow_right(d, 770, y + 50, 850, MUTED, 4)
        body, mark = marked(rep)
        status, _, shown = body.partition("  ")
        cell(d, 860, y, 120, 100, status, ROSE, 36)
        panel(d, 1000, y, 860, 100, ROSE, 3)
        fit_mono(d, 1025, y + 34, shown.strip(), TEXT, 26, 810, "lm")
        label(d, 1025, y + 74, mark.lower(), ROSE, 24, True, "lm")
    score = marked(section("ONE", "THE SCORE")[0])[0]
    caption(d, "The checker's score for the shipped store: %s." % score, AMBER, 880)


def checks_and_gate(img, d, cfg):
    """The eight checks against both stores, and the gate's verdict on four admin passwords."""
    before, after = checks("ONE"), checks("TWO")
    header(d, 60, 200, "THE 8 CHECKS", CYAN, 420)
    header(d, 500, 200, "SHIPPED", ROSE, 170)
    header(d, 690, 200, "HARDENED", GREEN, 190)
    for i, ((v1, name, _), (v2, _, _)) in enumerate(zip(before, after)):
        y = 262 + i * 66
        label(d, 70, y + 26, name, TEXT, 28, False, "lm")
        cell(d, 500, y, 170, 52, v1, ROSE if v1 == "FAIL" else GREEN, 26)
        cell(d, 690, y, 190, 52, v2, GREEN if v2 == "pass" else ROSE, 26)
    y = 262 + len(before) * 66 + 10
    label(d, 70, y + 26, "score", AMBER, 28, True, "lm")
    for x, w, name, c in ((500, 170, "ONE", ROSE), (690, 190, "TWO", GREEN)):
        fit_mono(d, x + w // 2, y + 26, section(name, "THE SCORE")[0].split("  ")[0].strip(), c, 28, w, "mm")
    header(d, 980, 200, "THE GATE: admin.password set to ...", AMBER, 880)
    res = section("TWO", "THE INPUTS AND RESULTS")
    for i in range(0, len(res), 2):
        y = 270 + (i // 2) * 150
        value, result = res[i].strip(), res[i + 1].replace("->", "").strip()
        ok = result == "started"
        cell(d, 980, y, 880, 56, value, AMBER, 28)
        draw.arrow(d, 1100, y + 60, 1100, y + 100, MUTED, 4)
        fit_mono(d, 1130, y + 82, result, GREEN if ok else ROSE, 26, 730, "lm")
    caption(d, "The checks run from outside; the gate runs before the port opens.", AMBER, 900)


def drift_table(img, d, cfg):
    """Development, the copied production file, and the drift check's verdict, setting by setting."""
    dev = settings(section("ONE", "THE SETTINGS FILE"))
    prod = dict(dev)
    prod.update(settings(section("THREE", "THE INPUT")))
    verdicts = {}
    for ln in section("THREE", "THE CHECK, SETTING BY SETTING"):
        key, _, rest = ln.strip().partition(" ")
        verdicts[key] = marked(rest.strip())
    for ln in section("THREE", "THE CHECK, KEY BY KEY"):
        key, mark = marked(ln)
        verdicts[key] = ("present", mark)
    cols = ((60, 400, "SETTING", CYAN), (480, 470, "DEVELOPMENT", MUTED), (970, 470, "PRODUCTION COPY", AMBER),
            (1460, 400, "DRIFT CHECK", ROSE))
    for x, w, t, c in cols:
        header(d, x, 200, t, c, w)
    for i, (key, (seen, mark)) in enumerate(verdicts.items()):
        y = 265 + i * 76
        bad = bool(mark)
        fit_mono(d, 70, y + 31, key, TEXT, 28, 390, "lm")
        cell(d, 480, y, 470, 62, dev.get(key, "(none)") or "(empty)", MUTED, 26, TEXT)
        cell(d, 970, y, 470, 62, prod.get(key, "(none)") or "(empty)", ROSE if bad else GREEN, 26, TEXT)
        cell(d, 1460, y, 400, 62, mark.lower() if bad else seen, ROSE if bad else GREEN, 26)
    result = section("THREE", "THE RESULT")
    caption(d, result[0].strip(), ROSE, 820)
    caption(d, "deployed anyway: " + result[2].replace("->", "").strip(), AMBER, 880)


def small_leaks(img, d, cfg):
    """The Server header, the TRACE echo, and the settings file's mode bits."""
    server = [marked(ln.strip()) for ln in section("FOUR", "THE REPLY'S Server HEADER")]
    header(d, 60, 200, "THE Server HEADER (GET /)", CYAN, 580)
    for i, (text, mark) in enumerate(server):
        who, _, value = text.partition(" ")
        y = 280 + i * 200
        label(d, 70, y, who, MUTED, 28, True)
        cell(d, 60, y + 44, 580, 76, value.strip(), ROSE if mark else GREEN, 28, TEXT)
        if mark:
            label(d, 70, y + 132, mark.lower(), ROSE, 26, True)
    req = sections("FOUR", "THE REQUEST")[1][0].strip()
    header(d, 680, 200, "TRACE, WITH A COOKIE", CYAN, 560)
    cell(d, 680, 270, 560, 76, req, AMBER, 28)
    for i, ln in enumerate(section("FOUR", "THE REPLY (")):
        text, mark = marked(ln.strip())
        who, _, value = text.partition(" ")
        y = 390 + i * 200
        label(d, 690, y, who, MUTED, 28, True)
        cell(d, 680, y + 44, 560, 76, value.strip(), ROSE if mark else GREEN, 28, TEXT)
        if mark:
            label(d, 690, y + 132, mark.lower(), ROSE, 26, True)
    header(d, 1280, 200, "THE FILE'S MODE", CYAN, 580)
    files = section("FOUR", "THE SETTINGS FILE ON DISK")
    for i in range(0, len(files), 2):
        y = 280 + (i // 2) * 310
        mode = files[i].split("=")[0].strip()
        readable = files[i + 1].strip().endswith("true")
        fit_mono(d, 1290, y + 20, mode, TEXT, 30, 560, "lm")
        for j, who in enumerate(("owner", "group", "others")):
            x = 1280 + j * 196
            bits = mode[j * 3:j * 3 + 3]
            c = (ROSE if "r" in bits else GREEN) if j else CYAN
            label(d, x + 90, y + 60, who, MUTED, 24, True, "ma")
            cell(d, x, y + 95, 180, 62, bits, c, 30, TEXT)
        label(d, 1290, y + 180, "others can read the secret: %s" % str(readable).lower(), ROSE if readable else GREEN, 24, True)
    caption(d, "Small leaks: a version to look up, a cookie echoed, a secret others can read.", AMBER, 900)


def check_ladder(img, d, cfg):
    """A new setting flagged by the drift check, and where each check runs."""
    new = section("FIVE", "THE INPUT")[0].strip()
    flagged = section("FIVE", "THE DRIFT CHECK")[0].strip()
    header(d, 60, 200, "A NEW FEATURE ADDS A SETTING", AMBER)
    cell(d, 60, 260, 640, 70, new, AMBER, 30, TEXT)
    draw.arrow_right(d, 715, 295, 800, MUTED, 4)
    fit_mono(d, 820, 295, flagged, ROSE, 28, 1040, "lm")
    steps = (("in CI", "every change", "the drift check"), ("at startup", "every start", "the gate"),
             ("live store", "from outside", "the checker"), ("on a schedule", "every night", "the checker, again"))
    for i, (where, when, what) in enumerate(steps):
        x = 60 + i * 460
        draw.box(d, x, 450, 400, 220, where, when, (CYAN, GREEN, AMBER, ROSE)[i], 34, 26)
        label(d, x + 200, 700, what, TEXT, 28, True, "ma")
        if i < 3:
            draw.arrow_right(d, x + 405, 560, x + 455, MUTED, 4)
    caption(d, section("FIVE", "THE BILL")[-1].strip(), AMBER, 880)
