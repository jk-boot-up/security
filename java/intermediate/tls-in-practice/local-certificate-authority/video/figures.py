"""Pictures for the local certificate authority video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from datetime import datetime
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
    """Every section of an act whose header starts with `head`, as lists of stripped lines."""
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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def kv(lines):
    """'subject      Shop Issuing CA' -> {'subject': 'Shop Issuing CA'}."""
    out = {}
    for ln in lines:
        m = re.match(r"(.+?)\s{2,}(.+)$", ln)
        if m:
            out[m.group(1)] = m.group(2)
    return out


def step(ln):
    """'4. x is inside y: true' -> ('4. x is inside y', True)."""
    text, _, value = ln.rpartition(": ")
    return text, value == "true"


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


def fit_label(d, x, y, text, colour, size, max_w, bold=False, anchor="la"):
    f = font(SANS_B if bold else SANS, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 30, True, "ma")


def cell(d, x, y, w, h, text, colour, size=24, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def mark(d, x, y, ok):
    """A small true/false pill."""
    cell(d, x, y, 96, 40, "true" if ok else "false", GREEN if ok else ROSE, 22)


def when(text):
    return datetime.strptime(text, "%Y-%m-%d %H:%M")


# ------------------------------------------------------------------ figures

def chain_of_trust(img, d, cfg):
    """The three certificates of the chain, side by side: who they are, how long they live, and what limits they carry."""
    root = kv(sections("ONE", "THE ROOT")[0])
    issuing = kv(sections("ONE", "THE ISSUING AUTHORITY")[0])
    leaf = kv(sections("ONE", "THE CERTIFICATE")[0])
    cards = [
        ("THE ROOT", CYAN, root["subject"].split(", serial"), "serial " + root["subject"].split("serial ")[1], root["valid"],
         ["may certify " + root["may certify"].split()[0] + " level below", "kept offline"]),
        ("THE ISSUING CA", AMBER, issuing["subject"].split(", serial"), "serial " + issuing["subject"].split("serial ")[1], issuing["valid"],
         ["path length " + issuing["path length"].split(":")[0] + ": no more CAs", "names " + issuing["names"]]),
        ("THE CERTIFICATE", GREEN, [leaf["names"]], "serial " + leaf["serial"].split(",")[0], leaf["valid"].split(" (")[0],
         [leaf["purpose"].split(",")[0] + ", " + leaf["valid"].split("(")[1].rstrip(")"), "authority: " + leaf["authority"].split(":")[0]]),
    ]
    x = 60
    for i, (title, c, name, serial, valid, limits) in enumerate(cards):
        header(d, x, 210, title, c, 520)
        panel(d, x, 270, 520, 560, c, 3)
        fit_label(d, x + 260, 300, name[0], TEXT, 32, 480, True, "ma")
        label(d, x + 260, 350, serial, MUTED, 26, False, "ma")
        frm, to = valid.split(" to ")
        label(d, x + 30, 420, "valid from", MUTED, 24, True)
        fit_mono(d, x + 30, 455, frm, TEXT, 26, 460)
        label(d, x + 30, 510, "valid until", MUTED, 24, True)
        fit_mono(d, x + 30, 545, to, TEXT, 26, 460)
        label(d, x + 30, 620, "limits", MUTED, 24, True)
        for j, lim in enumerate(limits):
            fit_mono(d, x + 30, 655 + j * 45, lim, c, 24, 460)
        if i < 2:
            draw.arrow_right(d, x + 530, 550, x + 640, MUTED, 4)
            label(d, x + 585, 500, "signs", MUTED, 24, True, "ma")
        x += 650
    caption(d, "Each certificate is signed by the one on its left; clients trust only the root.", AMBER, 880)


def chain_check(img, d, cfg):
    """The client's check of www.shop.internal, step by step, next to the same check of a name signed with the policy skipped."""
    steps = [step(ln) for ln in sections("ONE", "THE CHAIN CHECK BY HAND")[0]]
    day = heading("ONE", "THE CHAIN CHECK BY HAND").split(" on ")[1].rstrip("):")
    results = sections("ONE", "THE RESULT")
    header(d, 60, 210, "BEFORE: www.shop.internal, CHECKED ON " + day, GREEN)
    panel(d, 60, 270, 1000, 40 + 58 * len(steps) + 100, GREEN, 3)
    for i, (text, ok) in enumerate(steps):
        y = 290 + i * 58
        fit_mono(d, 85, y + 20, text, TEXT, 24, 840, "lm")
        mark(d, 940, y, ok)
    for j, ln in enumerate(results[0]):
        fit_mono(d, 85, 310 + 58 * len(steps) + 40 * j, ln, GREEN, 26, 940, "lm")
    stray = sections("ONE", "THE POLICY SKIPPED")[0][0]
    bad_text, bad_ok = step(sections("ONE", "THE CLIENT'S CHECK")[0][0])
    header(d, 1100, 210, "AFTER: THE POLICY SKIPPED", ROSE)
    panel(d, 1100, 270, 760, 546, ROSE, 3)
    fit_label(d, 1125, 300, stray, TEXT, 28, 710, True)
    label(d, 1125, 365, "the failed step:", MUTED, 24, True)
    fit_mono(d, 1125, 410, bad_text, ROSE, 24, 590)
    mark(d, 1740, 402, bad_ok)
    for j, ln in enumerate(results[1]):
        fit_mono(d, 1125, 490 + 46 * j, ln, ROSE, 24, 710)
    for j, ln in enumerate(("the constraint is inside the issuing CA's", "own certificate, so every client enforces it")):
        fit_label(d, 1125, 660 + 40 * j, ln, MUTED, 26, 710)
    caption(d, "Every step must be true; the first false step decides the refusal.", AMBER, 900)


def signing_request(img, d, cfg):
    """What a signing request carries, the authority's checks in order, and the certificate it signs."""
    req = kv(sections("TWO", "THE REQUEST")[0])
    checks = [step(ln) for ln in sections("TWO", "THE AUTHORITY'S CHECKS")[0]]
    cert = sections("TWO", "THE CERTIFICATE")[0][0]
    header(d, 60, 205, "THE REQUEST (made and signed by payments-team)", AMBER)
    panel(d, 60, 258, 1800, 112, AMBER, 3)
    for j, (k, v) in enumerate(req.items()):
        x = 85 + j * 600
        label(d, x, 270, k, MUTED, 24, True)
        fit_label(d, x, 310, v, ROSE if k == "private key" else TEXT, 26, 570, True)
    draw.arrow(d, 960, 372, 960, 410, MUTED, 4)
    header(d, 60, 400, "THE AUTHORITY'S CHECKS, IN ORDER", CYAN)
    panel(d, 60, 452, 1800, 20 + 52 * len(checks), CYAN, 3)
    for i, (text, ok) in enumerate(checks):
        y = 464 + i * 52
        fit_mono(d, 85, y + 20, text, TEXT, 26, 1580, "lm")
        mark(d, 1740, y, ok)
    serial, same = cert.split("; ")
    forged = sections("TWO", "THE FORGED REQUEST")[0][0].replace("-> ", "")
    wrong = sections("TWO", "THE WRONG TEAM")[0]
    boxes = [("THE CERTIFICATE", GREEN, serial, same),
             ("THE FORGED REQUEST", ROSE, "another key signed it", forged),
             ("THE WRONG TEAM: web-team", ROSE, "asks for " + wrong[0], wrong[1].replace("-> ", ""))]
    for i, (title, c, top, bottom) in enumerate(boxes):
        x = 60 + i * 610
        header(d, x, 745, title, c)
        d.rounded_rectangle([x, 797, x + 580, 887], radius=12, fill=BG_CELL, outline=c, width=3)
        fit_label(d, x + 20, 805, top, TEXT, 26, 540, True)
        fit_label(d, x + 20, 845, bottom, c, 26, 540, True)
    caption(d, "The request carries the public key; its signature proves the private key.", AMBER, 920)


def revocation_timeline(img, d, cfg):
    """Days 0 to 21: the revocation lists and how long each is current, and what clients decide on day 11 and day 20."""
    issuing = kv(sections("THREE", "THE ISSUING CA'S REVOCATION LIST")[0])
    rootl = kv(sections("THREE", "THE ROOT'S REVOCATION LIST")[0])
    day_of = lambda v: int(re.search(r"day (\d+)", v).group(1))
    x0, per, last = 140, 78, 21
    X = lambda dd: x0 + min(dd, last + 0.6) * per
    day11 = sections("THREE", "THE CHECKS ON DAY 11")[0]
    day20 = sections("THREE", "THE CHECKS ON DAY 20")[0]
    fresh_day = int(re.search(r"from day (\d+)", " ".join(day20)).group(1))
    bars = [("root's list: revoked " + rootl["revoked"], CYAN, day_of(rootl["this update"]), day_of(rootl["next update"]), 210),
            ("issuing CA's list: " + issuing["revoked"], AMBER, day_of(issuing["this update"]), day_of(issuing["next update"]), 290),
            ("a fresh list", GREEN, fresh_day, fresh_day + 7, 370)]
    for text, c, a, b, y in bars:
        d.rounded_rectangle([X(a), y + 36, X(b), y + 66], radius=10, fill=BG_CELL, outline=c, width=3)
        right = a > 12
        fit_label(d, X(last + 0.6) if right else X(a), y, text + "  (day %d to %d)" % (a, b), c, 24, 1500, True, "ra" if right else "la")
    axis = 470
    d.line([(x0, axis), (X(last + 0.6), axis)], fill=LINE, width=3)
    for dd in range(0, last + 1):
        d.line([(X(dd), axis - 8), (X(dd), axis + 8)], fill=LINE, width=2)
        if dd % 5 == 0 or dd in (11, 17, 20):
            label(d, X(dd), axis + 12, str(dd), MUTED, 24, True, "ma")
    label(d, 60, axis + 12, "day", MUTED, 24, True)
    for dd in (11, 20):
        d.line([(X(dd), 205), (X(dd), axis)], fill=TEXT, width=2)
    header(d, 60, 550, "THE CHECKS ON DAY 11", AMBER)
    for i in range(3):
        who, res = day11[2 * i], day11[2 * i + 1].replace("-> ", "")
        c = ROSE if "<-" in res or res.startswith("refused") else GREEN
        y = 610 + i * 84
        fit_label(d, 60, y, who, TEXT, 24, 860)
        fit_mono(d, 90, y + 36, res.replace("  <- ", "  ").lower() if "<-" in res else res, c, 24, 830)
    header(d, 980, 550, heading("THREE", "THE CHECKS ON DAY 20").split(" (")[0], AMBER)
    rows = [(day20[0], day20[1]), (day20[3], day20[4])]
    for i, (who, res) in enumerate(rows):
        res = res.replace("-> ", "")
        y = 610 + i * 126
        fit_label(d, 980, y, who, TEXT, 24, 880)
        fit_mono(d, 1010, y + 36, res, ROSE if res.startswith("refused") else GREEN, 24, 850)
        if i == 0:
            fit_mono(d, 1010, y + 72, day20[2].replace("<- ", "").lower(), AMBER, 24, 850)
    caption(d, "A list is current until its next update; after that, a strict client fails closed.", AMBER, 900)


def renewal_timeline(img, d, cfg):
    """Hours of the 24-hour certificates: when each one starts, when it is renewed, and when the stopped one runs out."""
    first = sections("FOUR", "THE FIRST CERTIFICATE")[0][0]
    work = kv(sections("FOUR", "HOW THE RENEWAL TIME IS WORKED OUT")[0])
    renew = sections("FOUR", "THE 72 HOURS")[0]
    stop = sections("FOUR", "THE RENEWAL JOB STOPS")[0]
    start, end = re.search(r"valid (\S+ \S+) to (\S+ \S+)", first).groups()
    life = int((when(end) - when(start)).total_seconds() // 3600)
    hours = [0] + [int(h) for h in re.search(r"hours ([\d, ]+):", renew[0]).group(1).split(", ")]
    stop_end = int((when(re.search(r"ends (\S+ \S+)", stop[0]).group(1)) - when(start)).total_seconds() // 3600)
    after = int(re.search(r"(\d+) hours after", stop[1]).group(1))
    verdict = stop[1].split(": ", 1)[1]
    header(d, 60, 210, "HOW THE RENEWAL TIME IS WORKED OUT", CYAN)
    fit_mono(d, 60, 275, "two thirds: " + work["two thirds"], TEXT, 26, 900)
    fit_mono(d, 60, 315, "renew at:   " + work["renew at"], TEXT, 26, 1800)
    x0, per = 220, 16.5
    X = lambda h: x0 + h * per
    y0 = 400
    for i, h in enumerate(hours):
        y = y0 + i * 62
        c = GREEN if i < len(hours) - 1 else AMBER
        label(d, 60, y + 20, "cert %d" % (i + 1), MUTED, 24, True, "lm")
        d.rounded_rectangle([X(h), y, X(h + life), y + 40], radius=8, fill=BG_CELL, outline=c, width=3)
        d.line([(X(h + life * 2 / 3), y - 4), (X(h + life * 2 / 3), y + 44)], fill=AMBER, width=3)
    yb = y0 + len(hours) * 62 + 10
    d.line([(x0, yb), (X(96), yb)], fill=LINE, width=3)
    for h in range(0, 97, 8):
        d.line([(X(h), yb - 8), (X(h), yb + 8)], fill=LINE, width=2)
        label(d, X(h), yb + 14, str(h), MUTED, 24, True, "ma")
    label(d, 60, yb + 14, "hour", MUTED, 24, True)
    xs = X(hours[-1] + after)
    d.line([(xs, y0 - 20), (xs, yb)], fill=ROSE, width=3)
    fit_label(d, xs - 10, y0 - 50, "job stopped; hour %d: %s" % (hours[-1] + after, verdict), ROSE, 24, 900, True, "ra")
    label(d, X(stop_end), y0 + (len(hours) - 1) * 62 + 20, "  ends hour %d" % stop_end, AMBER, 24, True, "lm")
    fit_label(d, 60, yb + 60, renew[0].split(": ")[1] + " at hours " + ", ".join(map(str, hours[1:])) + " (amber marks); " + renew[1], GREEN, 26, 1800, True)


def root_key_uses(img, d, cfg):
    """The offline root key's few signatures, and what revoking the issuing CA does to everything below it."""
    uses = sections("FIVE", "THE ROOT KEY'S SIGNATURES")[0]
    revoked = kv(sections("FIVE", "THE ROOT'S LIST OF DAY")[0])["revoked"]
    check = sections("FIVE", "THE CHECK ON DAY 41")[0]
    audit = heading("FIVE", "THE AUDIT LOG").split("(")[1].rstrip("):")
    header(d, 60, 210, "THE ROOT KEY'S SIGNATURES", CYAN)
    panel(d, 60, 270, 900, 40 + 64 * len(uses), CYAN, 3)
    for i, u in enumerate(uses):
        fit_mono(d, 85, 295 + i * 64, u, AMBER if "revoked" in u else TEXT, 26, 850)
    label(d, 60, 340 + 64 * len(uses), "the audit log: " + audit, MUTED, 28, True)
    header(d, 1010, 210, heading("FIVE", "THE ROOT'S LIST OF DAY").split(" (")[0].replace("THE ", ""), ROSE)
    cell(d, 1135, 280, 600, 70, "Shop Internal Root CA", CYAN, 28)
    draw.arrow(d, 1435, 350, 1435, 420, MUTED, 4)
    cell(d, 1135, 420, 600, 70, revoked.split(", ")[0], ROSE, 28)
    fit_label(d, 1435, 500, "revoked: " + revoked.split("), ")[1], ROSE, 26, 700, True, "ma")
    for x in (1010, 1460):
        draw.arrow(d, 1435, 545, x + 195, 610, MUTED, 3)
    www, res = check[0].split(" -> ")
    cell(d, 1010, 610, 390, 70, www, TEXT, 26)
    cell(d, 1460, 610, 390, 70, "every certificate below", TEXT, 26)
    label(d, 1205, 700, res, ROSE, 26, True, "ma")
    label(d, 1655, 700, res.split(" (")[0] + " too", ROSE, 26, True, "ma")
    caption(d, "Every root signature is a rare, offline ceremony; revoking an issuing CA ends all below it.", AMBER, 900)