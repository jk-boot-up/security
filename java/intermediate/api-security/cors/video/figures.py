"""Pictures for the CORS video, drawn by the `figure` slide kind.

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


def section(name, head):
    return sections(name, head)[0]


def cols(line):
    """A table row split on runs of two or more spaces."""
    return re.split(r"\s{2,}", line.strip())


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la", max_w=None):
    f = font(MONO_B, size)
    while max_w and f.getlength(text) > max_w and f.size > 16:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, "mm", w - 16)


def host(origin):
    return origin.replace("https://", "")


# ------------------------------------------------------------------ figures

def read_gate(img, d, cfg):
    """The core idea on one request: the browser sends it, the bank runs it, the browser keeps the answer from the page."""
    evil = re.search(r"a script on (\S+),", next(l for l in act("TWO") if l.startswith("THE REQUESTS"))).group(1)
    bank = re.search(r"a script on (\S+),", next(l for l in act("ONE") if l.startswith("THE REQUEST "))).group(1)
    balance = section("ONE", "WHAT THE PAGE GETS")[0].replace("read: ", "")
    got = section("TWO", "WHAT THE PAGE GETS")[0]
    allow = section("TWO", "THE PREFLIGHT ANSWER")[0].split(": ", 1)[1]
    # three boxes: the page, the browser, the bank
    for x, title, sub, c in ((60, "THE PAGE", evil, ROSE), (720, "THE BROWSER", "applies the rules", CYAN),
                             (1380, "THE BANK'S API", bank, GREEN)):
        panel(d, x, 230, 480, 150, c, 3)
        label(d, x + 240, 262, title, c, 30, True, "ma")
        mono(d, x + 240, 322, sub, TEXT, 26, "ma", 440)
    # the request goes all the way
    header(d, 60, 420, "1. SENDING", AMBER)
    d.line([(540, 520), (1360, 520)], fill=AMBER, width=4)
    d.polygon([(1360, 508), (1360, 532), (1380, 520)], fill=AMBER)
    mono(d, 950, 478, "GET /account  + the customer's cookie", AMBER, 26, "ma")
    label(d, 950, 540, "the browser sends it: the bank runs it", TEXT, 26, False, "ma")
    # the answer comes back to the browser, which checks a header
    header(d, 60, 620, "2. READING", AMBER)
    d.line([(1380, 720), (980, 720)], fill=GREEN, width=4)
    d.polygon([(980, 708), (980, 732), (960, 720)], fill=GREEN)
    mono(d, 1170, 676, balance, GREEN, 26, "ma")
    mono(d, 1200, 776, "Access-Control-Allow-Origin: " + allow, MUTED, 24, "ma", 640)
    d.rounded_rectangle([720, 680, 960, 760], radius=14, fill=BG_CELL, outline=ROSE, width=4)
    label(d, 840, 720, "BLOCKED", ROSE, 30, True, "mm")
    d.line([(540, 720), (720, 720)], fill=LINE, width=4)
    label(d, 630, 772, "nothing", MUTED, 24, False, "ma")
    label(d, 60, 830, "What the page gets:", TEXT, 28, True)
    mono(d, 420, 832, got, ROSE, 28)
    caption(d, "CORS decides who may read the answer, not who may send the request.", AMBER, 910)


def origin_parts(img, d, cfg):
    """The four origins split into scheme, host and port, compared with the bank's API."""
    rows = [cols(l) for l in section("ONE", "THE ORIGINS")]
    header(d, 60, 210, "THE ORIGINS: SCHEME + HOST + PORT", CYAN)
    xs = (80, 600, 780, 1240, 1430)
    for x, h in zip(xs, ("origin", "scheme", "host", "port", "same as the API?")):
        label(d, x, 280, h, MUTED, 24, True)
    api = rows[0]
    for i, r in enumerate(rows):
        y = 320 + i * 82
        same = r[4] == "yes"
        panel(d, 60, y, 1800, 66, GREEN if same else LINE, 3 if same else 2)
        mono(d, xs[0], y + 33, r[0], TEXT, 26, "lm")
        for j in (1, 2, 3):
            mono(d, xs[j], y + 33, r[j], TEXT if r[j] == api[j] else ROSE, 26, "lm")
        mono(d, xs[4] + 60, y + 33, r[4], GREEN if same else ROSE, 28, "lm")
    req = section("ONE", "THE REQUEST")[0]
    got = section("ONE", "WHAT THE PAGE GETS")[0]
    header(d, 60, 670, "THE REQUEST", AMBER)
    mono(d, 60, 735, req + "   (a script on " + api[0] + ")", TEXT, 26)
    header(d, 60, 790, "WHAT THE PAGE GETS", GREEN)
    mono(d, 60, 855, got, GREEN, 28)
    caption(d, "Red parts differ from the API. Any difference makes a page cross-origin.", AMBER, 920)


def send_not_read(img, d, cfg):
    """Three requests from the attacker's page: which are sent, which reach the bank, what the page reads."""
    reqs = [cols(l) for l in section("TWO", "THE REQUESTS")]
    gets = [cols(l) for l in section("TWO", "WHAT THE PAGE GETS")]
    log = section("TWO", "THE BANK'S LOG")[0].split("  <-")[0]
    allow = section("TWO", "THE PREFLIGHT ANSWER")[0].split(": ", 1)[1]
    header(d, 60, 210, "THE REQUEST", AMBER)
    header(d, 820, 210, "SIMPLE?", CYAN)
    header(d, 1200, 210, "WHAT THE PAGE GETS", GREEN)
    for i, (r, g) in enumerate(zip(reqs, gets)):
        y = 280 + i * 120
        call = " ".join(r[:-1])
        kind = r[-1]
        result = g[-1]
        sent = not result.startswith("never")
        panel(d, 60, y, 1800, 96, ROSE if sent and "POST" in call else LINE, 3)
        mono(d, 90, y + 48, call, TEXT, 28, "lm", 700)
        mono(d, 820, y + 48, kind, AMBER if kind != "simple" else TEXT, 26, "lm", 360)
        mono(d, 1200, y + 30, result, GREEN if not sent else AMBER, 24, "lm", 640)
        label(d, 1200, y + 70, "the bank ran it" if sent else "preflight answer: Allow-Origin " + allow,
              ROSE if sent else MUTED, 22, True, "lm")
    header(d, 60, 660, "THE BANK'S LOG", ROSE)
    mono(d, 60, 728, log, ROSE, 34)
    label(d, 60, 790, "A simple POST is sent with the cookie, and the bank runs it. The page", TEXT, 28)
    label(d, 60, 830, "only loses the answer. Stopping forged requests is the CSRF defence's job.", TEXT, 28)
    caption(d, "Simple: GET, HEAD or POST with a form or plain-text body. Others ask first.", AMBER, 910)


def bad_policies(img, d, cfg):
    """Three policies against three origins: what Access-Control-Allow-Origin comes back, and who reads."""
    names = [l.split(": ", 1)[1] for l in act("THREE") if l.startswith("THE POLICY")]
    tables = [[cols(r) for r in t[1:]] for t in sections("THREE", "THE ANSWERS")]
    origins = [r[0] for r in tables[0]]
    x0, cw = 420, 480
    for j, n in enumerate(names):
        header(d, x0 + j * cw, 210, "POLICY %d" % (j + 1), ROSE, cw - 30)
        label(d, x0 + j * cw + (cw - 30) // 2, 270, n, TEXT, 22, True, "ma")
    for i, o in enumerate(origins):
        y = 320 + i * 130
        mono(d, 60, y + 55, host(o), TEXT, 26, "lm", 340)
        for j, t in enumerate(tables):
            back, page = t[i][1], t[i][2]
            reads = page == "reads"
            x = x0 + j * cw
            d.rounded_rectangle([x, y, x + cw - 30, y + 110], radius=12, fill=BG_CELL,
                                outline=ROSE if reads else LINE, width=3)
            mono(d, x + (cw - 30) // 2, y + 34, back, TEXT if back != "(none)" else MUTED, 22, "mm", cw - 60)
            label(d, x + (cw - 30) // 2, y + 78, "READS THE BALANCE" if reads else "blocked",
                  ROSE if reads else GREEN, 24, True, "mm")
    label(d, 60, 290, "origin sent", MUTED, 22, True)
    ends = section("THREE", "HOW THE SUFFIX TEST DECIDES")
    for k, e in enumerate(ends[1:]):
        mono(d, 60, 735 + k * 44, e, ROSE if e.endswith("true") else TEXT, 26, "la", 1800)
    caption(d, "Each cell: the Access-Control-Allow-Origin the bank sends back.", AMBER, 910)


def exact_list(img, d, cfg):
    """The exact list, the browser's check for the partner, and what each page gets."""
    lst = section("FOUR", "THE LIST")
    check = [cols(l) for l in section("FOUR", "THE BROWSER'S CHECK")]
    rows = [re.match(r"(\S+)\s+(\S+ \S+)\s+(.*)", l).groups() for l in section("FOUR", "WHAT EACH PAGE GETS")]
    header(d, 60, 210, "THE LIST", CYAN)
    panel(d, 60, 270, 560, 30 + 44 * len(lst), CYAN, 3)
    for k, o in enumerate(lst):
        mono(d, 90, 290 + k * 44, o, TEXT, 28)
    label(d, 60, 370, "whole strings, case-sensitive", MUTED, 26)
    header(d, 60, 440, "THE BROWSER'S CHECK", AMBER)
    for k, (q, a) in enumerate(check):
        label(d, 60, 510 + k * 50, q, TEXT, 26)
        a = a.replace("-> ", "")
        mono(d, 600, 510 + k * 50, a, GREEN if a == "yes" else ROSE, 28)
    label(d, 60, 670, "for the partner's GET /account,", MUTED, 26)
    label(d, 60, 706, "with the cookie", MUTED, 26)
    header(d, 700, 210, "WHAT EACH PAGE GETS", GREEN)
    for i, r in enumerate(rows):
        y = 270 + i * 82
        good = r[0] == "partner.example" or "public" in r[-1]
        blocked = not r[2].startswith("read")
        panel(d, 700, y, 1160, 66, GREEN if not blocked else LINE, 2)
        mono(d, 720, y + 33, r[0], TEXT, 26, "lm", 270)
        mono(d, 1000, y + 33, r[1], TEXT, 26, "lm", 230)
        mono(d, 1240, y + 33, r[2], GREEN if (good or blocked) else ROSE, 26, "lm", 600)
    caption(d, "Only the listed partner reads with cookies; public rates use * without cookies.", AMBER, 910)


def wildcard_cookies(img, d, cfg):
    """The same answer with * and credentials, checked for a request with cookies and one without."""
    hdrs = section("FIVE", "THE ANSWER")
    withc = sections("FIVE", "THE BROWSER'S CHECK")[0]
    without = sections("FIVE", "THE BROWSER'S CHECK")[1]
    header(d, 60, 210, "THE ANSWER (the server sends both)", CYAN)
    panel(d, 60, 270, 1000, 40 + 46 * len(hdrs), CYAN, 3)
    for k, h in enumerate(hdrs):
        mono(d, 90, 290 + k * 46, h, TEXT, 28)
    for j, (title, lines) in enumerate((("A REQUEST WITH COOKIES", withc), ("A REQUEST WITHOUT COOKIES", without))):
        x = 60 + j * 920
        verdict = lines[-1].replace("-> ", "")
        ok = verdict == "accepted"
        header(d, x, 450, title, AMBER)
        panel(d, x, 510, 880, 230, GREEN if ok else ROSE, 3)
        label(d, x + 30, 540, lines[0], TEXT, 26)
        label(d, x + 440, 640, verdict.upper(), GREEN if ok else ROSE, 48, True, "ma")
    caption(d, "So a wildcard is for public data only. With cookies, the bank must name the page.", AMBER, 910)
