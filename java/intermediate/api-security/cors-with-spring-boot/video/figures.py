"""Pictures for the CORS with Spring Boot video, drawn by the `figure` slide kind.

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



def stage(d, x, y, w, h, title, sub, colour, width=3):
    panel(d, x, y, w, h, colour, width)
    label(d, x + w // 2, y + 24, title, colour, 26, True, "ma")
    label(d, x + w // 2, y + 64, sub, TEXT, 22, False, "ma")


def arrow_r(d, x1, x2, y, colour):
    d.line([(x1, y), (x2 - 14, y)], fill=colour, width=4)
    d.polygon([(x2 - 18, y - 11), (x2 - 18, y + 11), (x2, y)], fill=colour)


# ------------------------------------------------------------------ figures

def filter_order(img, d, cfg):
    """The core idea for Spring: the checks a request passes, in order, and where each refusal happens."""
    refused = section("THREE", "SPRING'S OWN ANSWER")[0].split(";")[0]
    order = section("FOUR", "THE ORDER")
    mvc_pre = section("FOUR", "REGISTERED IN SPRING MVC")[0].split()[-1]
    sec_pre = section("FOUR", "REGISTERED IN SPRING SECURITY")[0].split()[-1]
    header(d, 60, 210, "SPRING SECURITY", CYAN, 860)
    header(d, 1000, 210, "SPRING MVC", AMBER, 860)
    xs = [(60, "http.cors", "CORS filter", CYAN), (500, "login check", "needs a login", CYAN),
          (1000, "addCorsMappings", "CORS mapping", AMBER), (1440, "controller", "the bank's code", GREEN)]
    for x, t, s, c in xs:
        stage(d, x, 280, 420, 110, t, s, c)
    for a, b in ((480, 500), (920, 1000), (1420, 1440)):
        arrow_r(d, a - 4, b + 4, 335, MUTED)
    label(d, 60, 400, "  ->  ".join(o.split(": ", 1)[0][3:] for o in order) + ": the order every request goes through",
          MUTED, 24)
    # three requests and where they stop
    rows = [("a page not on the list", 60, refused, "stopped by the CORS filter; the controller never runs", ROSE),
            ("a preflight, policy in MVC", 500, mvc_pre, "a preflight has no login, so the login check refuses it", ROSE),
            ("a preflight, policy in Security", 60, sec_pre, "answered by the CORS filter, before the login check", GREEN)]
    for i, (who, x, status, why, c) in enumerate(rows):
        y = 480 + i * 140
        label(d, 60, y, who, TEXT, 28, True)
        cell(d, x, y + 44, 420, 60, status, c, 28)
        label(d, 1000, y + 60, why, c, 26, True)
    caption(d, "Register the policy with http.cors, so it is checked before the login.", AMBER, 930)


def send_not_read(img, d, cfg):
    """Three requests from the attacker's page: which are sent, which reach the bank, what the page reads."""
    reqs = [cols(l) for l in section("ONE", "THE REQUESTS")]
    gets = [cols(l) for l in section("ONE", "WHAT THE PAGE GETS")]
    log = section("ONE", "THE BANK'S LOG")[0].split("  <-")[0]
    pre = section("ONE", "THE PREFLIGHT ANSWER")[0]
    header(d, 60, 210, "THE REQUEST", AMBER)
    header(d, 820, 210, "SIMPLE?", CYAN)
    header(d, 1200, 210, "WHAT THE PAGE GETS", GREEN)
    for i, (r, g) in enumerate(zip(reqs, gets)):
        y = 280 + i * 120
        call, kind, result = " ".join(r[:-1]), r[-1], g[-1]
        sent = not result.startswith("never")
        panel(d, 60, y, 1800, 96, ROSE if sent and "POST" in call else LINE, 3)
        mono(d, 90, y + 48, call, TEXT, 28, "lm", 700)
        mono(d, 820, y + 48, kind, AMBER if kind != "simple" else TEXT, 26, "lm", 360)
        mono(d, 1200, y + 30, result, GREEN if not sent else AMBER, 24, "lm", 640)
        label(d, 1200, y + 70, "the bank ran it" if sent else "preflight: %s, Allow-Origin %s" % (pre.split(",")[0], pre.split(": ")[-1]), ROSE if sent else MUTED, 22, True, "lm")
    header(d, 60, 660, "THE BANK'S LOG", ROSE)
    mono(d, 60, 728, log, ROSE, 34)
    label(d, 60, 790, "With no policy, Spring sends no CORS headers: the page reads nothing,", TEXT, 28)
    label(d, 60, 830, "but the simple POST, sent with the login, still ran.", TEXT, 28)
    caption(d, "Simple: GET, HEAD or POST with a form or plain-text body. Others ask first.", AMBER, 910)


def bad_policies(img, d, cfg):
    """The star with credentials, then three policies against three origins."""
    pols = sections("TWO", "THE POLICY")
    star = section("TWO", "THE ANSWER TO")[0]
    tables = [[cols(r) for r in t[1:]] for t in sections("TWO", "THE ANSWERS")]
    origins = [r[0] for r in tables[0]]
    panel(d, 60, 205, 1800, 70, AMBER, 3)
    mono(d, 90, 240, "policy 1: " + pols[0][0], TEXT, 26, "lm", 780)
    mono(d, 900, 240, "->  " + star, AMBER, 26, "lm", 940)
    x0, cw = 420, 480
    for j, p in enumerate(pols[1:]):
        name = p[0].replace("allowedOriginPatterns", "patterns").replace("allowedOrigins", "origins")
        header(d, x0 + j * cw, 300, "POLICY %d" % (j + 2), ROSE, cw - 30)
        kind, _, value = name.partition(" ")
        label(d, x0 + j * cw + (cw - 30) // 2, 350, kind, MUTED, 22, True, "ma")
        mono(d, x0 + j * cw + (cw - 30) // 2, 378, value, TEXT, 22, "ma", cw - 40)
    label(d, 60, 390, "origin sent", MUTED, 22, True)
    for i, o in enumerate(origins):
        y = 420 + i * 120
        mono(d, 60, y + 50, host(o), TEXT, 26, "lm", 340)
        for j, t in enumerate(tables):
            back, answer = t[i][1], t[i][2]
            reads = answer.endswith("reads")
            x = x0 + j * cw
            d.rounded_rectangle([x, y, x + cw - 30, y + 100], radius=12, fill=BG_CELL, outline=ROSE if reads else LINE, width=3)
            mono(d, x + (cw - 30) // 2, y + 30, back, TEXT if back != "(none)" else MUTED, 22, "mm", cw - 60)
            label(d, x + (cw - 30) // 2, y + 70, answer.replace("reads", "READS THE BALANCE"), ROSE if reads else GREEN, 24, True, "mm")
    pattern = section("TWO", "HOW THE PATTERN")
    hit = next(cols(l)[0] for l in pattern if l.endswith("matches"))
    label(d, 60, 800, "Policy 4: the * in https://*bank.example may be any text, so %s matches." % hit, ROSE, 28, True)
    caption(d, "Each cell: the Access-Control-Allow-Origin Spring sends back, and the status.", AMBER, 910)


def exact_list(img, d, cfg):
    """The exact list as Spring holds it, the preflight answer, and what each page gets."""
    pol = section("THREE", "THE POLICY")
    pre = section("THREE", "THE PREFLIGHT ANSWER")
    rows = [re.match(r"(\S+(?: page)?)\s+(\S+ \S+)\s+(.*)", l).groups() for l in section("THREE", "WHAT EACH PAGE GETS")]
    header(d, 60, 210, "THE POLICY", CYAN)
    panel(d, 60, 270, 760, 30 + 44 * len(pol), CYAN, 3)
    for k, ln in enumerate(pol):
        mono(d, 85, 288 + k * 44, ln, TEXT, 24, "la", 710)
    header(d, 60, 500, "THE PARTNER'S PREFLIGHT: " + pre[0], AMBER)
    for k, ln in enumerate(pre[1:]):
        mono(d, 60, 565 + k * 42, ln.replace("Access-Control-", ""), TEXT, 24, "la", 760)
    label(d, 60, 790, "(each header starts with Access-Control-)", MUTED, 24)
    header(d, 880, 210, "WHAT EACH PAGE GETS", GREEN)
    for i, (site, call, res) in enumerate(rows):
        y = 270 + i * 76
        blocked = not res.startswith("read")
        panel(d, 880, y, 980, 62, GREEN if not blocked else LINE, 2)
        mono(d, 900, y + 31, site, TEXT, 24, "lm", 250)
        mono(d, 1150, y + 31, call, TEXT, 24, "lm", 210)
        mono(d, 1370, y + 31, res, GREEN, 24, "lm", 480)
    caption(d, "Only the listed partner reads; Spring answers every other origin 403.", AMBER, 910)


def where_registered(img, d, cfg):
    """The same list registered in Spring MVC and in Spring Security: the preflight's status and the PUT."""
    for j, head in enumerate(("REGISTERED IN SPRING MVC", "REGISTERED IN SPRING SECURITY")):
        rows = [cols(l) for l in section("FOUR", head)]
        title = next(l for l in act("FOUR") if l.startswith(head))
        ok = rows[0][-1].startswith("2")
        x = 60 + j * 920
        header(d, x, 210, title.rstrip(":").replace("REGISTERED IN ", ""), GREEN if ok else ROSE)
        panel(d, x, 280, 880, 420, GREEN if ok else ROSE, 3)
        label(d, x + 30, 310, "preflight for PUT, no login", TEXT, 28, True)
        label(d, x + 440, 360, rows[0][-1], GREEN if ok else ROSE, 72, True, "ma")
        for k, r in enumerate(rows[1:]):
            label(d, x + 30, 500 + k * 80, " ".join(r[:-1]), MUTED, 26, True)
            mono(d, x + 30, 536 + k * 80, r[-1], GREEN if r[-1].startswith("read") else ROSE, 26, "la", 820)
    note = act("FOUR")[-1]
    label(d, W // 2, 760, note, TEXT, 30, False, "ma")
    caption(d, "In MVC the login check runs first; with http.cors the CORS filter runs first.", AMBER, 910)


def the_bill(img, d, cfg):
    """A wildcard checked with and without credentials, and the same POST from a page and from a server script."""
    star = section("FIVE", "THE PUBLIC ANSWER")[0]
    checks = [cols(l) for l in section("FIVE", "THE BROWSER'S CHECK")]
    page = section("FIVE", "A POST")[0]
    script = section("FIVE", "THE SAME POST")[0].split("  <-")[0]
    log = section("FIVE", "THE BANK'S LOG")[0]
    header(d, 60, 210, "PUBLIC DATA: GET /rates", CYAN)
    mono(d, 60, 280, star, TEXT, 28)
    for k, (who, verdict) in enumerate(checks):
        verdict = verdict.replace("-> ", "")
        label(d, 60, 340 + k * 50, who, TEXT, 28)
        mono(d, 460, 340 + k * 50, verdict, GREEN if verdict == "accepted" else ROSE, 28)
    header(d, 60, 480, "A POST /transfer", AMBER)
    for k, (who, what, c) in enumerate((("from a page on evil.example", page, GREEN),
                                       ("from a server script, no Origin header", script, ROSE))):
        y = 550 + k * 110
        panel(d, 60, y, 1800, 90, c, 3)
        label(d, 90, y + 45, who, TEXT, 28, True, "lm")
        mono(d, 900, y + 45, what, c, 28, "lm", 920)
    label(d, 60, 790, "The bank's log: ", TEXT, 28, True)
    mono(d, 340, 792, log, ROSE, 28)
    caption(d, "CORS is a rule inside the browser. Without a browser, nothing checks it.", AMBER, 910)
