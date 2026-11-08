"""Pictures for the HTTP basics and security headers with nginx video, drawn by the `figure` slide kind.

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



def points(text):
    """'0 + 1 + 1 + 1 + 1 = 4 of 5' found in a line."""
    return re.search(r"[01] \+ [01] \+ [01] \+ [01] \+ [01] = \d of 5", text).group(0)


# ------------------------------------------------------------------ figures

def front_door(img, d, cfg):
    """Browser, nginx and the application, and the five add_header lines nginx writes."""
    five = list(headers_of("TWO", "THE FIVE HEADERS").items())
    big_box(d, 100, 230, 340, 130, "browser", "the customer", CLIENT)
    big_box(d, 790, 230, 340, 130, "nginx", "the front door", CYAN)
    big_box(d, 1480, 230, 340, 130, "application", "the store", CLIENT)
    draw.arrow_right(d, 445, 275, 785, GREEN, 4)
    label(d, 615, 245, "request", GREEN, 24, True, "ma")
    draw.arrow_left(d, 785, 320, 445, ROSE, 4)
    label(d, 615, 330, "response + headers", ROSE, 24, True, "ma")
    draw.arrow_right(d, 1135, 275, 1475, GREEN, 4)
    draw.arrow_left(d, 1475, 320, 1135, MUTED, 4)
    label(d, 1305, 330, "response, unchanged code", MUTED, 22, False, "ma")
    header(d, 100, 420, "THE CONFIGURATION (five add_header lines in nginx.conf)", GREEN)
    for i, (name, value) in enumerate(five):
        y = 490 + i * 70
        d.rounded_rectangle([100, y, 1820, y + 58], radius=10, fill=BG_CELL, outline=LINE, width=2)
        f = font(MONO_B, 28)
        x = 125
        for piece, c in (("add_header ", MUTED), (name, CYAN), (' "%s"' % value, GREEN), (" always;", AMBER)):
            d.text((x, y + 29), piece, font=f, fill=c, anchor="lm")
            x += f.getlength(piece)
    caption(d, "One place adds the same headers to every page, and the application does not change.", AMBER, 875)


def bare_response(img, d, cfg):
    """The default response's headers: an exact version, and five missing security headers."""
    req = rows("ONE", "THE REQUEST")[0]
    resp = [(ln[:27].strip(), ln[27:].split("<-")[0].strip()) for ln in rows("ONE", "THE RESPONSE")]
    status = re.search(r"THE RESPONSE \(status (\d+)\)", section("ONE")).group(1)
    total = points(section("ONE", "THE AUDIT"))
    e = header(d, 100, 205, "THE REQUEST", GREEN)
    node(d, e + 30, 200, 360, 54, req, GREEN, 26)
    label(d, e + 420, 227, "to nginx with its default settings", MUTED, 26, False, "lm")
    header(d, 100, 290, "THE RESPONSE (status %s)" % status, ROSE)
    for i, (name, value) in enumerate(resp):
        y = 360 + i * 68
        label(d, 100, y + 28, name, TEXT, 28, True, "lm")
        c = AMBER if name == "Server" else ROSE
        node(d, 600, y, 520, 56, value, c, 26)
        note = "the exact version: a free hint" if name == "Server" else "the browser gets no rule"
        label(d, 1150, y + 28, note, c, 24, name == "Server", "lm")
    header(d, 100, 790, "THE AUDIT", ROSE)
    node(d, 600, 785, 520, 58, total, AMBER, 26)
    label(d, 1150, 814, "one point per security header", MUTED, 24, False, "lm")
    caption(d, "As it comes, nginx tells the browser nothing, and tells the attacker its version.", AMBER, 890)


def always(img, d, cfg):
    """The five headers, the two ways to write them, and four real requests with their scores."""
    five = list(headers_of("TWO", "THE FIVE HEADERS").items())
    conf = {ln.split(None, 1)[0]: ln.split(None, 1)[1] for ln in rows("TWO", "THE CONFIGURATION")}
    resp = [re.match(r"(with|without)\s+GET (\S+)\s+(\d+)\s+(.+?)(?:\s+<-.*)?$", ln).groups()
            for ln in rows("TWO", "THE RESPONSES")]
    header(d, 100, 205, "THE FIVE HEADERS", GREEN)
    for i, (name, value) in enumerate(five):
        d.text((100, 285 + i * 36), name, font=font(MONO_B, 24), fill=CYAN, anchor="lm")
        d.text((560, 285 + i * 36), value, font=font(MONO_B, 24), fill=TEXT, anchor="lm")
    header(d, 1200, 205, "THE CONFIGURATION (one of the five)", GREEN)
    for i, k in enumerate(("without", "with")):
        y = 275 + i * 90
        label(d, 1200, y + 10, k + " always", CYAN, 24, True, "lm")
        node(d, 1200, y + 30, 620, 46, conf[k], GREEN if k == "with" else MUTED, 22)
    header(d, 100, 480, "THE RESPONSES", ROSE)
    label(d, 790, 500, "GET /", CLIENT, 26, True, "ma")
    label(d, 1390, 500, "GET /missing (a 404 page)", CLIENT, 26, True, "ma")
    for i, k in enumerate(("without", "with")):
        y = 560 + i * 100
        label(d, 100, y + 32, k + " always", CYAN, 28, True, "lm")
        for path, status, pts in [(r[1], r[2], r[3]) for r in resp if r[0] == k]:
            x = 520 if path == "/" else 1120
            good = pts.endswith("5 of 5")
            node(d, x, y, 560, 64, "%s   %s" % (status, pts), GREEN if good else ROSE, 24)
    caption(d, "Without always, error pages such as a 404 go out with no security headers.", AMBER, 870)


def inheritance(img, d, cfg):
    """A server block with five headers, a location with its own add_header, and what each path gets."""
    conf = rows("THREE", "THE CONFIGURATION")
    resp = rows("THREE", "THE RESPONSES")
    top = points(resp[0])
    account = points(resp[1])
    cache = resp[2].strip()
    fixed = points(section("THREE", "AFTER THE FIX"))
    header(d, 100, 205, "THE CONFIGURATION", GREEN)
    d.rounded_rectangle([100, 270, 1000, 640], radius=14, fill=BG_CELL, outline=GREEN, width=3)
    d.text((130, 310), "server {", font=font(MONO_B, 28), fill=TEXT, anchor="lm")
    d.text((170, 360), "add_header ... always;  × 5", font=font(MONO_B, 28), fill=GREEN, anchor="lm")
    d.rounded_rectangle([160, 410, 970, 600], radius=12, fill=PANEL, outline=AMBER, width=3)
    d.text((190, 450), conf[3].strip(), font=font(MONO_B, 28), fill=TEXT, anchor="lm")
    d.text((230, 505), conf[4].strip(), font=font(MONO_B, 24), fill=AMBER, anchor="lm")
    label(d, 230, 560, "its own add_header: the five above no longer apply", ROSE, 22, True, "lm")
    header(d, 1060, 205, "THE RESPONSES", ROSE)
    label(d, 1060, 300, "GET /", CLIENT, 28, True, "lm")
    node(d, 1300, 270, 520, 60, top, GREEN, 24)
    label(d, 1060, 400, "GET /account", CLIENT, 28, True, "lm")
    node(d, 1300, 370, 520, 60, account, ROSE, 24)
    node(d, 1300, 440, 520, 50, cache, AMBER, 22)
    header(d, 1060, 540, "AFTER THE FIX", GREEN)
    label(d, 1300, 680, "the five repeated inside /account", MUTED, 22, False, "lm")
    node(d, 1300, 600, 520, 60, fixed, GREEN, 24)
    caption(d, "A location inherits add_header only if it has none of its own.", AMBER, 760)
    label(d, W // 2, 820, "So audit every location, not just the top page.", MUTED, 30, False, "ma")


def limits(img, d, cfg):
    """Two limit lines, five raw requests and the five real answers."""
    conf = [ln.split(";")[0] + ";" for ln in rows("FOUR", "THE CONFIGURATION")]
    reqs = [ln.split(None, 1)[1] for ln in rows("FOUR", "THE REQUESTS")]
    resp = [re.match(r"(\w)  (\d+)  (.+)", ln).groups() for ln in rows("FOUR", "THE RESPONSES")]
    header(d, 100, 205, "THE CONFIGURATION", GREEN)
    for i, c in enumerate(conf):
        d.text((500, 228 + i * 46), c, font=font(MONO_B, 28), fill=GREEN, anchor="lm")
    label(d, 1300, 250, "1k = 1024 bytes", AMBER, 30, True, "lm")
    header(d, 100, 330, "THE REQUESTS", GREEN)
    header(d, 1260, 330, "THE RESPONSES", ROSE)
    for i, (r, (key, status, why)) in enumerate(zip(reqs, resp)):
        y = 400 + i * 86
        if i == 3:
            label(d, 100, y - 6, "ambiguous: where does the body end?", AMBER, 22, True, "lm")
        yy = y + (14 if i >= 3 else 0)
        node(d, 100, yy, 70, 60, key, CLIENT, 28)
        node(d, 190, yy, 1040, 60, r, TEXT, 24, False)
        node(d, 1260, yy, 120, 60, status, GREEN if status == "200" else ROSE, 28)
        label(d, 1400, yy + 30, why, MUTED, 22, False, "lm")
    caption(d, "Refuse what is too big, and refuse what two programs could read differently.", AMBER, 880)


def hide_upstream(img, d, cfg):
    """The application's version headers passed on by nginx, then hidden."""
    before = [(ln[:18].strip(), ln[18:].strip()) for ln in rows("FIVE", "THE RESPONSE")]
    after = [(ln[:18].strip(), ln[18:].strip()) for ln in rows("FIVE", "AFTER THE FIX")]
    fix = re.search(r"AFTER THE FIX \((.+)\)", section("FIVE")).group(1)
    big_box(d, 100, 230, 380, 130, "application", "behind nginx", CLIENT)
    big_box(d, 760, 230, 380, 130, "nginx", "the front door", CYAN)
    big_box(d, 1440, 230, 380, 130, "browser", "or an attacker", CLIENT)
    draw.arrow_right(d, 485, 295, 755, MUTED, 4)
    draw.arrow_right(d, 1145, 295, 1435, MUTED, 4)
    for x, title, rows_, colour in ((100, "THE RESPONSE (as it comes)", before, ROSE),
                                    (1000, "AFTER THE FIX", after, GREEN)):
        header(d, x, 420, title, colour)
        for i, (name, value) in enumerate(rows_):
            y = 490 + i * 72
            label(d, x, y + 29, name, TEXT, 26, True, "lm")
            c = GREEN if value in ("nginx", "hidden") else ROSE
            node(d, x + 300, y, 480, 58, value, c, 26)
    label(d, 1000, 730, fix, GREEN, 24, True, "lm")
    caption(d, "Hiding is a speed bump, not a lock: keep the software up to date.", AMBER, 840)
