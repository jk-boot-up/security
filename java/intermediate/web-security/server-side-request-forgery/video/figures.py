"""Pictures for the server-side request forgery video, drawn by the `figure` slide kind.

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


def keyed(lines):
    """Lines like "a  text" as (key, text)."""
    return [(ln[0], ln[3:]) for ln in lines]


def inside(address):
    """The address kinds the demo refuses, by prefix: loopback, link-local and the private ranges."""
    return address.startswith(("127.", "169.254.", "10.", "192.168.", "172.16."))


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


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def dashed_box(d, x, y, w, h, colour, dash=18):
    for x0 in range(x, x + w, 2 * dash):
        d.line([(x0, y), (min(x0 + dash, x + w), y)], fill=colour, width=3)
        d.line([(x0, y + h), (min(x0 + dash, x + w), y + h)], fill=colour, width=3)
    for y0 in range(y, y + h, 2 * dash):
        d.line([(x, y0), (x, min(y0 + dash, y + h))], fill=colour, width=3)
        d.line([(x + w, y0), (x + w, min(y0 + dash, y + h))], fill=colour, width=3)


def hosts():
    """The network table of act one: (address, name, what)."""
    out = []
    for ln in sections("ONE", "THE NETWORK")[0]:
        m = re.match(r"(\S+)\s+(\(no name\)|\S+)\s+(.+)", ln)
        out.append(m.groups())
    return out


# ------------------------------------------------------------------ figures

def inside_reach(img, d, cfg):
    """The core idea: the customer is outside, the server is inside, and the server fetches for the customer."""
    draw.box(d, 60, 420, 300, 150, "Customer", "outside: the internet", ROSE)
    dashed_box(d, 470, 190, 1400, 690, GREEN)
    label(d, 500, 205, "the company network: the server can reach all of it", GREEN, 24, True)
    draw.box(d, 520, 420, 320, 150, "Store server", "fetches the address", CLIENT)
    draw.arrow_right(d, 370, 470, 510, ROSE, 4)
    label(d, 440, 490, "an address", ROSE, 22, True, "ma")
    rows = hosts()
    for i, (address, name, what) in enumerate(rows):
        y = 260 + i * 120
        c = ROSE if inside(address) else GREEN
        panel(d, 1010, y, 840, 96, c, 3)
        d.text((1035, y + 30), address, font=font(MONO_B, 28), fill=c, anchor="lm")
        d.text((1035, y + 70), (name + ", " if name != "(no name)" else "") + what, font=font(SANS, 22), fill=TEXT, anchor="lm")
        d.text((1830, y + 48), "inside" if inside(address) else "public", font=font(SANS_B, 22), fill=c, anchor="rm")
        d.line([(850, 495), (1000, y + 48)], fill=MUTED, width=3)
    caption(d, "The customer cannot reach the red hosts. The server can, and fetches for the customer.", AMBER, 900)


def honest_import(img, d, cfg):
    """Act one: the address, the lookup, the GET to the address, and the picture that comes back."""
    req = sections("ONE", "THE REQUEST")[0][0]
    steps = sections("ONE", "HOW THE SERVER FETCHES IT")[0]
    res = sections("ONE", "THE RESPONSE")[0][0]
    lookup = re.match(r"look up (\S+): (\S+)", steps[0]).groups()
    get = steps[1]
    header(d, 80, 210, "THE REQUEST", AMBER)
    cell(d, 80, 270, 1760, 80, req, AMBER, 34)
    header(d, 80, 400, "HOW THE SERVER FETCHES IT", CYAN)
    draw.box(d, 80, 470, 520, 170, "1. Look up the name", lookup[0], CYAN)
    draw.arrow_right(d, 610, 555, 700, MUTED, 4)
    draw.box(d, 710, 470, 480, 170, "Its address", lookup[1], CYAN)
    draw.arrow_right(d, 1200, 555, 1290, MUTED, 4)
    draw.box(d, 1300, 470, 540, 170, "2. Connect and ask", get, CYAN)
    header(d, 80, 690, "THE RESPONSE", GREEN)
    cell(d, 80, 750, 760, 80, res, GREEN, 34)
    label(d, 880, 790, "a public image host: this is the feature working", MUTED, 26, False, "lm")
    caption(d, "The server, not the customer, makes the request.", AMBER, 900)


def server_reaches_inside(img, d, cfg):
    """Act two: three addresses that point inside, and what the server imported from each."""
    reqs = keyed(sections("TWO", "THE REQUESTS")[0])
    ress = dict(keyed(sections("TWO", "THE RESPONSES")[0]))
    header(d, 60, 200, "THE REQUEST (the address the seller pastes)", AMBER)
    header(d, 1060, 200, "THE RESPONSE (what was imported)", ROSE)
    for i, (k, addr) in enumerate(reqs):
        y = 290 + i * 180
        body, mark = ress[k].split("  <- ")
        cell(d, 60, y, 900, 90, addr, AMBER, 24)
        draw.arrow_right(d, 970, y + 45, 1050, MUTED, 4)
        cell(d, 1060, y, 800, 90, body, ROSE, 28)
        label(d, 1060, y + 105, mark.lower(), ROSE, 24, True)
    caption(d, "The customer never reached these hosts. The server did, and showed the answer.", AMBER, 880)


def many_spellings(img, d, cfg):
    """Act three: each request, what the blocklist decided from the text, the real address, and the result."""
    blocked = sections("THREE", "THE BLOCKLIST")[0][0]
    reqs = keyed(sections("THREE", "THE REQUESTS")[0])
    ress = dict(keyed(sections("THREE", "THE RESPONSES")[0]))
    real = {}
    for k in "bcd":
        real[k] = next(s[-1] for s in sections("THREE", "HOW %s," % k)).split()[1]
    inside_lines = sections("THREE", "HOW e AND f")[0]
    real["e"] = inside_lines[0].split(": ")[1]
    real["f"] = inside_lines[1].rsplit(" ", 1)[1]
    header(d, 60, 190, "THE BLOCKLIST, read as text", CYAN)
    fit_mono(d, 560, 212, blocked, CYAN, 26, 1300, "lm")
    for j, h in enumerate(("THE REQUEST (host)", "THE BLOCKLIST", "THE REAL ADDRESS", "THE RESPONSE")):
        label(d, (60, 640, 900, 1260)[j], 262, h, MUTED, 20, True)
    for i, (k, addr) in enumerate(reqs):
        y = 295 + i * 74
        host = re.match(r"http://([^/]+)", addr).group(1)
        res = ress[k]
        stopped = res.startswith("refused")
        cell(d, 60, y, 560, 60, k + "  " + host, AMBER, 24)
        cell(d, 640, y, 220, 60, "blocked" if stopped else "passes", GREEN if stopped else ROSE, 24)
        addr_real = real.get(k, host)
        cell(d, 900, y, 320, 60, addr_real, ROSE if inside(addr_real) else GREEN, 24)
        fit_mono(d, 1260, y + 30, res.replace("imported: ", ""), GREEN if stopped else ROSE, 22, 600, "lm")
    steps = sections("THREE", "HOW b,")[0]
    panel(d, 60, 755, 1800, 120, CYAN)
    label(d, 85, 770, "how b becomes an address: divide by 256 three times", CYAN, 22, True)
    parts = [re.match(r"(\d+) / (\d+) = (\d+), remainder (\d+)", s).groups() for s in steps[:-1]]
    working = "   ".join("%s / %s = %s r %s" % p for p in parts) + "   -> " + steps[-1].split()[1]
    fit_mono(d, 85, 830, working, TEXT, 24, 1750, "lm")
    caption(d, "Five spellings pass the list; the connection still goes inside.", AMBER, 900)


def checked_address(img, d, cfg):
    """Act four: the importer's checks on the redirect, and every request refused by its real address."""
    trace = sections("FOUR", "THE CHECKS")[0]
    header(d, 60, 190, "THE CHECKS FOR f, AS LOGGED", CYAN)
    for i, t in enumerate(trace):
        y = 255 + i * 82
        c = ROSE if t.startswith("refused") else AMBER if "redirect" in t else GREEN
        cell(d, 60, y, 860, 64, t, c, 22)
        if i < len(trace) - 1:
            draw.arrow(d, 490, y + 64, 490, y + 82, MUTED, 3)
    reqs = keyed(sections("FOUR", "THE REQUESTS")[0])
    ress = dict(keyed(sections("FOUR", "THE RESPONSES")[0]))
    header(d, 980, 190, "THE REQUESTS AND WHY EACH IS REFUSED", GREEN)
    for i, (k, addr) in enumerate(reqs):
        y = 255 + i * 66
        res = ress[k]
        host = re.match(r"http://([^/]+)", addr).group(1)
        fit_mono(d, 990, y + 26, k + "  " + host, AMBER, 22, 330, "lm")
        fit_mono(d, 1330, y + 26, res.replace("refused: ", "").replace("imported: ", "imported "), GREEN, 22, 530, "lm")
    caption(d, "Look the name up once, test every answer, connect to the address you tested.", AMBER, 900)


def two_lookups(img, d, cfg):
    """Act five: check-then-connect asks twice and gets two answers; the safe importer asks once."""
    twice = sections("FIVE", "CHECK, THEN CONNECT")[0]
    res = [s[0] for s in sections("FIVE", "THE RESPONSE")]
    once = sections("FIVE", "THE SAFE IMPORTER")[0]
    req = sections("FIVE", "THE REQUEST")[0][0]
    header(d, 60, 190, "THE REQUEST", AMBER)
    fit_mono(d, 330, 212, req, AMBER, 30, 1500, "lm")
    for col, (title, lines, result, c) in enumerate((
            ("CHECK, THEN CONNECT", twice, res[0], ROSE),
            ("THE SAFE IMPORTER", [once[1], once[2]], res[1], GREEN))):
        x = 60 + col * 920
        header(d, x, 280, title, c)
        for i, ln in enumerate(lines):
            y = 350 + i * 130
            first = i == 0
            colour = GREEN if (first or col == 1) else ROSE
            cell(d, x, y, 860, 90, ln, colour, 24)
            if i < len(lines) - 1:
                draw.arrow(d, x + 430, y + 90, x + 430, y + 130, MUTED, 3)
        draw.arrow(d, x + 430, 610, x + 430, 660, MUTED, 3)
        body, mark = result.split("  <- ")
        cell(d, x, 670, 860, 90, body, c, 26)
        label(d, x + 430, 775, mark.lower(), c, 24, True, "ma")
    caption(d, "The check is only as good as the address it checked: connect to that one.", AMBER, 880)
