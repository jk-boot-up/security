"""Pictures for the HTTP security headers with Spring Boot video, drawn by the `figure` slide kind.

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
FIVE = ["Strict-Transport-Security", "Content-Security-Policy", "X-Content-Type-Options",
        "X-Frame-Options", "Referrer-Policy"]


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("THE REQUEST")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s\b.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def rows(act, part):
    return [ln[4:] for ln in section(act, part).splitlines()]


def headers_of(act, part):
    """The five headers of a response as {name: value}, a policy split over lines joined again."""
    found, last = {}, None
    for ln in rows(act, part):
        name, value = ln[:27].strip(), ln[27:].strip()
        if name:
            found[name], last = value, name
        else:
            found[last] += " " + value
    return found


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


def audit_sums():
    return dict(re.findall(r"^    (bare store|hardened store)\s+([01 +]+= \d of 5)", section("ONE"), re.M))


# ------------------------------------------------------------------ figures

def the_chain(img, d, cfg):
    """One request through Tomcat, the security filter chain and the controller, and back with headers."""
    req = rows("ONE", "THE REQUEST")[0]
    hard = headers_of("ONE", "THE RESPONSE OF THE HARDENED STORE")
    status = re.search(r"HARDENED STORE \(status (\d+)\)", OUT).group(1)
    header(d, 100, 205, "THE REQUEST", GREEN)
    label(d, 100, 290, "browser", CLIENT, 28, True, "lm")
    node(d, 100, 320, 330, 70, req.split(" HTTP")[0], GREEN, 28)
    boxes = [("Tomcat", "reads and checks the bytes", 500, 340), ("Spring Security", "the filter chain", 920, 400),
             ("ShopController", "returns the page", 1400, 400)]
    for name, sub, x, w in boxes:
        big_box(d, x, 290, w, 130, name, sub, CYAN)
    draw.arrow_right(d, 435, 355, 495, GREEN, 4)
    draw.arrow_right(d, 845, 355, 915, GREEN, 4)
    draw.arrow_right(d, 1325, 355, 1395, GREEN, 4)
    label(d, 1120, 440, "↓ its HeaderWriterFilter adds the headers", AMBER, 26, True, "ma")
    header(d, 100, 500, "THE RESPONSE (status %s, the hardened store, plain HTTP)" % status, ROSE)
    for i, n in enumerate(FIVE):
        y = 570 + i * 64
        v = hard[n]
        c = ROSE if v == "missing" else GREEN
        label(d, 100, y + 26, n, TEXT, 26, True, "lm")
        node(d, 560, y, 900, 52, v, c, 24)
        label(d, 1490, y + 26, "written by the filter" if v != "missing" else "only over HTTPS (act 2)",
              c, 24, False, "lm")
    caption(d, "The controller writes no header: the configuration decides them for every page.", AMBER, 905)


def two_stores(img, d, cfg):
    """The five headers of the bare and the hardened store side by side, and the points added up."""
    req = rows("ONE", "THE REQUEST")[0]
    bare = headers_of("ONE", "THE RESPONSE OF THE BARE STORE")
    hard = headers_of("ONE", "THE RESPONSE OF THE HARDENED STORE")
    sums = audit_sums()
    e = header(d, 100, 205, "THE REQUEST", GREEN)
    node(d, e + 30, 200, 520, 54, req, GREEN, 26)
    label(d, e + 580, 227, "the same to both stores, plain HTTP", MUTED, 26, False, "lm")
    header(d, 100, 290, "THE RESPONSES", ROSE)
    label(d, 790, 345, "BARE STORE", CLIENT, 24, True, "ma")
    label(d, 1400, 345, "HARDENED STORE", CLIENT, 24, True, "ma")
    for i, n in enumerate(FIVE):
        y = 385 + i * 74
        label(d, 100, y + 29, n, TEXT, 26, True, "lm")
        for x, w, vals in ((560, 460, bare), (1060, 760, hard)):
            v = vals[n]
            node(d, x, y, w, 58, v, ROSE if v == "missing" else GREEN, 22)
    header(d, 100, 770, "THE AUDIT", ROSE)
    label(d, 100, 845, "one point per header", MUTED, 26, False, "lm")
    node(d, 560, 770, 460, 62, sums["bare store"], AMBER, 26)
    node(d, 1060, 770, 760, 62, sums["hardened store"], AMBER, 26)
    caption(d, "Strict-Transport-Security is left out over plain HTTP: a browser must ignore it there.",
            AMBER, 905)


def behind_proxy(img, d, cfg):
    """Browser to proxy over HTTPS, proxy to store over HTTP with X-Forwarded-Proto, and the HSTS max-age worked out."""
    req = rows("TWO", "THE REQUEST")
    fwd = req[1].split("<-")[0].strip()
    resp = headers_of("TWO", "THE RESPONSE")
    total = re.search(r"hardened store\s+([01 +]+= \d of 5)", section("TWO")).group(1)
    days = re.search(r"max-age (\d+) / (\d+) seconds a day = (\d+) days", section("TWO")).groups()
    header(d, 100, 205, "THE REQUEST", GREEN)
    big_box(d, 100, 280, 300, 120, "browser", "the customer", CLIENT)
    big_box(d, 760, 280, 300, 120, "proxy", "ends HTTPS", CYAN)
    big_box(d, 1520, 280, 300, 120, "store", "hardened, loopback", CYAN)
    draw.arrow_right(d, 405, 340, 755, GREEN, 5)
    label(d, 580, 300, "HTTPS (encrypted)", GREEN, 26, True, "ma")
    draw.arrow_right(d, 1065, 340, 1515, MUTED, 5)
    label(d, 1290, 300, "plain HTTP", MUTED, 26, True, "ma")
    node(d, 1065, 360, 450, 52, fwd, AMBER, 24)
    label(d, 1290, 425, "added by the proxy", AMBER, 22, False, "ma")
    header(d, 100, 480, "THE RESPONSE", ROSE)
    hsts = resp["Strict-Transport-Security"]
    label(d, 100, 570, "Strict-Transport-Security", TEXT, 28, True, "lm")
    node(d, 560, 540, 900, 60, hsts, GREEN, 26)
    label(d, 1490, 570, "now sent", GREEN, 26, True, "lm")
    label(d, 100, 645, "the other four headers as before", MUTED, 26, False, "lm")
    header(d, 100, 700, "THE AUDIT", ROSE)
    node(d, 560, 695, 560, 58, total, AMBER, 26)
    node(d, 1160, 695, 660, 58, "%s / %s = %s days" % days, AMBER, 26)
    label(d, 1490, 775, "max-age is in seconds", MUTED, 22, False, "ma")
    caption(d, "Safe only if nobody but the proxy can reach the store and send that header.", AMBER, 865)


def session_cookies(img, d, cfg):
    """The two JSESSIONID cookies and what each misses, attribute by attribute."""
    raw = rows("THREE", "THE COOKIES")
    cookies = [(raw[i][:8].strip(), raw[i][10:], raw[i + 1].strip()) for i in range(0, len(raw), 2)]
    verdict = section("THREE", "THE VERDICT")
    counts = re.findall(r"^    (\w+)\s+(\d+) problem", verdict, re.M)
    props = rows("THREE", "THE FIX")
    header(d, 100, 205, "THE COOKIES", GREEN, 860)
    header(d, 1000, 205, "THE VERDICT", ROSE, 820)
    cols = ["HttpOnly", "Secure", "SameSite"]
    xs = [1090, 1300, 1510]
    for x, n in zip(xs, cols):
        label(d, x, 280, n, TEXT, 26, True, "ma")
    label(d, 1730, 280, "problems", TEXT, 26, True, "ma")
    for i, ((name, first, attrs), (_, n)) in enumerate(zip(cookies, counts)):
        y = 330 + i * 160
        d.rounded_rectangle([100, y, 1840, y + 135], radius=12, fill=BG_CELL, outline=LINE, width=2)
        label(d, 125, y + 30, name, CLIENT, 26, True, "lm")
        d.text((125, y + 72), first, font=font(MONO_B, 22), fill=TEXT, anchor="lm")
        d.text((125, y + 108), attrs, font=font(MONO_B, 22), fill=TEXT, anchor="lm")
        for x, col in zip(xs, cols):
            ok = col in attrs
            d.text((x, y + 68), "✓" if ok else "✗", font=font(SANS_B, 54), fill=GREEN if ok else ROSE, anchor="mm")
        d.text((1730, y + 68), n, font=font(SANS_B, 54), fill=GREEN if n == "0" else ROSE, anchor="mm")
    header(d, 100, 670, "THE FIX", GREEN)
    for i, p in enumerate(props):
        d.text((330, 692 + i * 50), p, font=font(MONO_B, 28), fill=GREEN, anchor="lm")
    caption(d, "HttpOnly: no script reads it.  Secure: HTTPS only.  SameSite: not sent from other sites.",
            AMBER, 880)


def line_break(img, d, cfg):
    """The normal and the attack search text, and what the container really answered."""
    text = dict(ln.split(None, 1) for ln in rows("FOUR", "THE SEARCH TEXT"))
    resp = rows("FOUR", "THE RESPONSES")
    normal = resp[0].split(None, 1)[1]
    attack = [p.strip() for p in resp[1].split(None, 1)[1].split(",")] + [resp[2].strip()]
    raw = rows("FOUR", "THE REQUEST")[0]
    raw_status = rows("FOUR", "THE RESPONSE")[0]
    e = header(d, 100, 205, "THE SEARCH TEXT", GREEN)
    label(d, e + 24, 227, "the store puts it into the X-Echo header", TEXT, 28, True, "lm")
    for i, key in enumerate(("normal", "attack")):
        y = 270 + i * 76
        label(d, 100, y + 30, key, CLIENT if key == "normal" else ROSE, 28, True, "lm")
        d.rounded_rectangle([260, y, 1100, y + 60], radius=10, fill=BG_CELL, outline=LINE, width=2)
        wire_line(d, 285, y + 30, text[key], TEXT, 28)
    label(d, 1140, 376, "← a line break, then a header", ROSE, 26, True, "lm")
    header(d, 100, 440, "IF IT WERE WRITTEN AS IT IS", AMBER)
    d.text((130, 520), "X-Echo: shoes", font=font(MONO_B, 28), fill=TEXT, anchor="lm")
    d.text((130, 565), "Set-Cookie: evil=1", font=font(MONO_B, 28), fill=ROSE, anchor="lm")
    label(d, 520, 565, "← a cookie the attacker chose", ROSE, 24, False, "lm")
    header(d, 1000, 440, "THE RESPONSES", ROSE)
    label(d, 1000, 520, "normal", CLIENT, 26, True, "lm")
    node(d, 1150, 492, 680, 56, normal, GREEN, 24)
    label(d, 1000, 600, "attack", ROSE, 26, True, "lm")
    for i, a in enumerate(attack):
        node(d, 1150, 567 + i * 50, 680, 44, a, GREEN, 24)
    header(d, 100, 680, "THE REQUEST (raw bytes, a space in a name)", GREEN)
    node(d, 100, 745, 500, 58, raw, TEXT, 28)
    header(d, 1000, 730, "THE RESPONSE", ROSE)
    node(d, 1270, 725, 300, 58, raw_status, GREEN, 28)
    label(d, 1600, 754, "a bad request", MUTED, 26, False, "lm")
    caption(d, "Tomcat refuses both. Still: never write customer text into a header unchecked.", AMBER, 880)


def header_limit(img, d, cfg):
    """A 4013-byte header against the default 8192-byte and the hardened 1024-byte limit, to scale."""
    req = rows("FIVE", "THE REQUEST")
    padding = re.search(r"(\d+) \+ (\d+) \+ (\d+) = (\d+) bytes", req[0]).groups()
    whole = re.search(r"(\d+) bytes", req[1]).group(1)
    stores = re.findall(r"^    (\w+ store)\s+limit (\d+) bytes\s+status (\d+)", section("FIVE"), re.M)
    header(d, 100, 205, "THE REQUEST", GREEN)
    wire_line(d, 100, 300, "X-Padding: aaaa...aaaa\\r\\n", TEXT, 30)
    label(d, 700, 300, "%s + %s + %s = %s bytes" % padding, AMBER, 30, True, "lm")
    label(d, 1300, 300, "whole request: %s bytes" % whole, MUTED, 28, False, "lm")
    header(d, 100, 380, "THE RESPONSES", ROSE)
    label(d, 100, 450, "drawn to scale: the green bar is the request, the frame is the limit", MUTED, 26, False, "lm")
    biggest = max(int(s[1]) for s in stores)
    scale = 1150 / biggest
    x0 = 420
    for i, (name, limit, status) in enumerate(stores):
        y = 510 + i * 170
        limit = int(limit)
        ok = status == "200"
        label(d, 100, y + 40, name, CLIENT, 28, True, "lm")
        label(d, 100, y + 80, "limit %d bytes" % limit, MUTED, 24, False, "lm")
        lw = int(limit * scale)
        rw = int(int(whole) * scale)
        d.rounded_rectangle([x0, y, x0 + rw, y + 100], radius=10, fill=(30, 90, 70))
        d.rounded_rectangle([x0, y, x0 + lw, y + 100], radius=10, outline=AMBER, width=5)
        label(d, x0 + rw - 12, y + 50, "%s bytes" % whole, TEXT, 24, True, "rm")
        node(d, 1620, y + 20, 200, 60, "status " + status, GREEN if ok else ROSE, 26)
        label(d, x0 + lw + (10 if lw < rw else -12), y + 118, "limit", AMBER, 22, True, "la" if lw < rw else "ra")
    caption(d, "Small enough that one customer cannot make the server hold megabytes; big enough for cookies.",
            AMBER, 900)
