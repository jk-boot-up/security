"""Pictures for the HTTPS server video, drawn by the `figure` slide kind.

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
DARK = (15, 23, 42)


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("THE REQUEST")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s\b.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def rows(act, part):
    return [ln[4:] for ln in section(act, part).splitlines()]


def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUEST: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def node(d, x, y, w, h, text, c, size=28, mono=True, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=c, width=3)
    f = draw.fitted_font(MONO_B if mono else SANS_B, text, w - 20, size, 14)
    d.text((x + w // 2, y + h // 2), text, font=f, fill=c, anchor="mm")


def wire_line(d, x, y, text, colour, size=30):
    """A line in mono, with every written-out \\r\\n drawn in rose."""
    f = font(MONO_B, size)
    for piece in re.split(r"(\\r\\n)", text):
        d.text((x, y), piece, font=f, fill=ROSE if piece == "\\r\\n" else colour, anchor="lm")
        x += f.getlength(piece)
    return x


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (16 if sub else 0)), text, font=font(SANS_B, 34), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")



def kv(act, part, width=13):
    return [(ln[:width].strip(), ln[width:].strip()) for ln in rows(act, part)]


# ------------------------------------------------------------------ figures

def two_doors(img, d, cfg):
    """The shop's plain door and HTTPS door, and the client that trusts one certificate."""
    loc = re.search(r"Location: (\S+)", section("TWO")).group(1)
    status = rows("TWO", "THE RESPONSE")[0]
    cookie = rows("THREE", "THE COOKIE")[0]
    proto = re.search(r"THE RESPONSE \(over (\S+)\)", section("ONE")).group(1)
    subject = kv("ONE", "THE CERTIFICATE")[0][1]
    d.rounded_rectangle([100, 210, 1820, 640], radius=20, fill=PANEL, outline=LINE, width=3)
    label(d, 960, 230, "the shop: one program, two ports on this machine", MUTED, 26, True, "ma")
    big_box(d, 160, 290, 700, 150, "plain HTTP door", "readable by anyone on the way", AMBER)
    big_box(d, 1060, 290, 700, 150, "HTTPS door", "encrypted with %s" % proto, GREEN)
    node(d, 160, 470, 700, 60, status, AMBER, 26)
    d.rounded_rectangle([160, 545, 860, 605], radius=10, fill=BG_CELL, outline=AMBER, width=3)
    d.text((510, 575), "Location: " + loc, font=font(MONO_B, 18), fill=AMBER, anchor="mm")
    node(d, 1060, 470, 700, 60, "cart, login, five security headers", GREEN, 24, False)
    node(d, 1060, 545, 700, 60, cookie, GREEN, 20)
    big_box(d, 660, 720, 600, 130, "customer's client", "trusts one certificate: " + subject, CLIENT)
    draw.arrow(d, 760, 715, 510, 645, AMBER, 4)
    draw.arrow(d, 1160, 715, 1410, 645, GREEN, 4)
    caption(d, "The plain door serves nothing: it only points to the HTTPS door.", AMBER, 885)


def checked_request(img, d, cfg):
    """The certificate's fields, the client's three checks, then the response."""
    cert = kv("ONE", "THE CERTIFICATE")
    checks = rows("ONE", "THE CLIENT'S THREE CHECKS")
    merged = []
    for ln in checks:
        if ln[:13].strip():
            merged.append([ln[:13].strip(), ln[13:].strip()])
        else:
            merged[-1][1] += "  →  " + ln.strip()
    resp = rows("ONE", "THE RESPONSE")
    proto = re.search(r"THE RESPONSE \(over (\S+)\)", section("ONE")).group(1)
    header(d, 100, 205, "THE CERTIFICATE (what the shop presents)", GREEN)
    for i, (k, v) in enumerate(cert):
        x, y = 100 + (i % 2) * 860, 275 + (i // 2) * 66
        label(d, x, y + 26, k, MUTED, 26, True, "lm")
        node(d, x + 160, y, 660, 52, v, GREEN, 24)
    header(d, 100, 425, "THE CLIENT'S THREE CHECKS", CYAN)
    for i, (k, v) in enumerate(merged):
        y = 495 + i * 70
        node(d, 100, y, 70, 56, str(i + 1), CYAN, 28)
        label(d, 195, y + 28, k, TEXT, 28, True, "lm")
        ok = v.endswith("yes")
        label(d, 380, y + 28, v[:-4].rstrip(": "), MUTED, 24, False, "lm")
        d.text((1780, y + 28), "✓" if ok else "✗", font=font(SANS_B, 48), fill=GREEN if ok else ROSE, anchor="mm")
    header(d, 100, 720, "THE RESPONSE (over %s)" % proto, ROSE)
    for i, r in enumerate(resp):
        node(d, 100 + i * 640, 780, 600 if i == 0 else 1080, 60, r, ROSE if i == 0 else AMBER, 26)
    caption(d, "If any of the three checks fails, the client stops before sending a byte.", AMBER, 895)


def redirect(img, d, cfg):
    """A plain request, the 301 answer, and the HSTS max-age worked out."""
    req = rows("TWO", "THE REQUEST")[0]
    resp = rows("TWO", "THE RESPONSE")
    nxt = rows("TWO", "THE NEXT VISIT")
    days = re.search(r"(\d+) / (\d+) seconds a day = (\d+) days", section("TWO")).groups()
    header(d, 100, 205, "THE REQUEST (plain HTTP)", GREEN)
    node(d, 100, 270, 900, 60, req, GREEN, 26)
    header(d, 100, 370, "THE RESPONSE (from the plain port)", ROSE)
    for i, r in enumerate(resp):
        node(d, 100, 435 + i * 70, 1100, 56, r, ROSE if i < 2 else MUTED, 24)
    header(d, 1260, 205, "THE NEXT VISIT", GREEN)
    label(d, 1260, 290, nxt[0].strip(), TEXT, 26, True, "lm")
    node(d, 1260, 320, 560, 56, nxt[1].strip(), GREEN, 22)
    label(d, 1260, 420, "max-age is in seconds:", MUTED, 24, False, "lm")
    node(d, 1260, 450, 560, 60, "%s / %s" % days[:2], AMBER, 28)
    node(d, 1260, 525, 560, 60, "= %s days of HTTPS only" % days[2], AMBER, 28, False)
    draw.arrow_right(d, 1005, 300, 1250, MUTED, 3)
    label(d, 1125, 260, "later visits", MUTED, 22, False, "ma")
    caption(d, "The first request still crossed the network in the clear.", AMBER, 720)
    label(d, W // 2, 790, "HSTS protects only the visits after it.", MUTED, 30, False, "ma")


def secure_cookie(img, d, cfg):
    """The Secure cookie, which requests carry it, and the cookie without Secure that leaks."""
    good = rows("THREE", "THE COOKIE")[0]
    nxt = [re.match(r"(over \w+)\s+(sent|not sent)\s*(.*)", ln).groups() for ln in rows("THREE", "THE NEXT REQUESTS")]
    before = rows("THREE", "BEFORE THE FIX")
    bad = before[0]
    leak = re.match(r"(over \w+)\s+(sent|not sent)", before[1]).groups()
    header(d, 100, 205, "THE COOKIE (the careful shop's login sets)", GREEN)
    node(d, 100, 270, 1300, 64, good, GREEN, 28)
    header(d, 100, 380, "THE NEXT REQUESTS", ROSE)
    for i, (how, sent, why) in enumerate(nxt):
        y = 450 + i * 80
        label(d, 100, y + 32, how, CLIENT, 30, True, "lm")
        node(d, 360, y, 300, 64, sent, GREEN if (sent == "sent") == (how == "over https") else ROSE, 28)
        label(d, 700, y + 32, why, MUTED, 26, False, "lm")
    header(d, 100, 640, "BEFORE THE FIX (no Secure)", ROSE)
    node(d, 100, 705, 700, 60, bad, MUTED, 26)
    label(d, 850, 735, leak[0], CLIENT, 30, True, "lm")
    node(d, 1110, 705, 300, 60, leak[1], ROSE, 28)
    label(d, 1440, 735, "← a leak", ROSE, 30, True, "lm")
    caption(d, "Secure: never on plain HTTP.  HttpOnly: no script reads it.  SameSite: not from other sites.",
            AMBER, 860)


def refusals(img, d, cfg):
    """Four connection attempts and what the client did with each."""
    att = [ln.split(None, 1) for ln in rows("FOUR", "THE ATTEMPTS")]
    res = {}
    for ln in rows("FOUR", "THE RESULTS"):
        m = re.match(r"(\w)  (refused|ok) \((\S+)\)\s*(.*)", ln)
        res[m.group(1)] = m.groups()[1:]
    header(d, 100, 205, "THE ATTEMPTS", GREEN)
    header(d, 1180, 205, "THE RESULTS", ROSE)
    for i, (key, what) in enumerate(att):
        y = 280 + i * 120
        node(d, 100, y, 70, 80, key, CLIENT, 32)
        node(d, 190, y, 960, 80, what, TEXT, 26)
        verdict, detail, why = res[key]
        ok = verdict == "ok"
        node(d, 1180, y, 260, 80, verdict, AMBER if ok else GREEN, 30, False)
        label(d, 1470, y + 24, why or "connected over " + detail, AMBER if ok else TEXT, 26, True, "lm")
        label(d, 1470, y + 60, detail if not ok else "the older version", MUTED, 20, False, "lm")
    caption(d, "Allowing the older version is the client's choice: it is what a downgrade asks for.", AMBER, 880)


def scorecard(img, d, cfg):
    """Five outside checks on the careful and the careless shop, and the two sums."""
    lines = rows("FIVE", "THE CHECKS")[1:]
    checks = [(ln[:37].strip(), ln[37:46].strip(), ln[46:].strip()) for ln in lines]
    sums = dict(re.findall(r"^    (careful|careless)\s+(.+)$", section("FIVE", "THE SCORES"), re.M))
    header(d, 100, 205, "THE CHECKS (from outside)", GREEN)
    label(d, 1270, 285, "CAREFUL SHOP", CLIENT, 26, True, "ma")
    label(d, 1640, 285, "CARELESS SHOP", CLIENT, 26, True, "ma")
    for i, (name, a, b) in enumerate(checks):
        y = 330 + i * 80
        d.rounded_rectangle([100, y, 1820, y + 66], radius=10, fill=BG_CELL, outline=LINE, width=2)
        label(d, 130, y + 33, name, TEXT, 28, True, "lm")
        for x, v in ((1270, a), (1640, b)):
            d.text((x, y + 33), "✓" if v == "yes" else "✗", font=font(SANS_B, 48),
                   fill=GREEN if v == "yes" else ROSE, anchor="mm")
    header(d, 100, 750, "THE SCORES", ROSE)
    label(d, 420, 772, "careful", CLIENT, 28, True, "lm")
    node(d, 560, 740, 480, 64, sums["careful"], AMBER, 26)
    label(d, 1110, 772, "careless", CLIENT, 28, True, "lm")
    node(d, 1260, 740, 480, 64, sums["careless"], AMBER, 26)
    caption(d, "A padlock proves who the server is and that the line is private. Nothing more.", AMBER, 880)
