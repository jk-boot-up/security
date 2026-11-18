"""Pictures for the server-side request forgery with Spring Boot video, drawn by the `figure` slide kind.

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
    for ln in sections("ONE", "THE INVENTED INTERNET")[0]:
        m = re.match(r"(\S+)\s+(\(no name\)|\S+)\s+(.+)", ln)
        out.append(m.groups())
    return out


# ------------------------------------------------------------------ figures

def inside_reach(img, d, cfg):
    """The core idea: the staff member is outside, the Spring Boot shop is inside, and it fetches for them."""
    draw.box(d, 60, 420, 300, 150, "Staff form", "POST /import url=...", ROSE)
    dashed_box(d, 470, 190, 1400, 690, GREEN)
    label(d, 500, 205, "the company network: the shop can reach all of it", GREEN, 24, True)
    draw.box(d, 520, 420, 320, 150, "Spring Boot shop", "RestClient fetches it", CLIENT)
    draw.arrow_right(d, 370, 470, 510, ROSE, 4)
    label(d, 440, 490, "an address", ROSE, 22, True, "ma")
    for i, (address, name, what) in enumerate(hosts()):
        y = 260 + i * 120
        c = ROSE if inside(address) else GREEN
        panel(d, 1010, y, 840, 96, c, 3)
        d.text((1035, y + 30), address, font=font(MONO_B, 28), fill=c, anchor="lm")
        d.text((1035, y + 70), (name + ", " if name != "(no name)" else "") + what, font=font(SANS, 22), fill=TEXT, anchor="lm")
        d.text((1830, y + 48), "inside" if inside(address) else "public", font=font(SANS_B, 22), fill=c, anchor="rm")
        d.line([(850, 495), (1000, y + 48)], fill=MUTED, width=3)
    caption(d, "Outside, nobody reaches the red hosts. The shop can, and fetches for whoever asks.", AMBER, 900)


def form_to_fetch(img, d, cfg):
    """Act one: the form as posted, how its value is decoded, the importer's log, and the answer."""
    req = sections("ONE", "THE REQUEST")[0]
    dec = sections("ONE", "HOW THE FORM VALUE")[0]
    log = sections("ONE", "HOW THE SHOP FETCHES")[0]
    res = sections("ONE", "THE RESPONSE")[0][0]
    header(d, 60, 190, "THE REQUEST (the form)", AMBER)
    panel(d, 60, 250, 860, 120, AMBER, 3)
    for i, ln in enumerate(req):
        fit_mono(d, 85, 280 + i * 50, ln, AMBER, 28, 810, "lm")
    header(d, 1000, 190, "HOW THE FORM VALUE IS DECODED", CYAN)
    panel(d, 1000, 250, 860, 40 + 50 * len(dec), CYAN)
    for i, ln in enumerate(dec):
        fit_mono(d, 1025, 280 + i * 50, ln, GREEN if ln.startswith("->") else TEXT, 26, 810, "lm")
    header(d, 60, 470, "HOW THE SHOP FETCHES IT (its log)", CYAN)
    for i, ln in enumerate(log):
        y = 530 + i * 80
        cell(d, 60, y, 1300, 62, ln, CYAN, 26)
        if i < len(log) - 1:
            draw.arrow(d, 710, y + 62, 710, y + 80, MUTED, 3)
    header(d, 1420, 470, "THE RESPONSE", GREEN)
    cell(d, 1420, 530, 440, 80, res, GREEN, 28)
    caption(d, "The shop looks up the name, connects to the address, and imports the picture.", AMBER, 880)


def server_reaches_inside(img, d, cfg):
    """Act two: three addresses that point inside, and what the shop answered for each."""
    reqs = keyed(sections("TWO", "THE REQUESTS")[0])
    ress = dict(keyed(sections("TWO", "THE RESPONSES")[0]))
    header(d, 60, 200, "THE REQUEST (the url the form posts)", AMBER)
    header(d, 1000, 200, "THE RESPONSE (status, then body)", ROSE)
    for i, (k, addr) in enumerate(reqs):
        y = 280 + i * 130
        cell(d, 60, y, 840, 90, addr, AMBER, 26)
        draw.arrow_right(d, 910, y + 45, 990, MUTED, 4)
        cell(d, 1000, y, 860, 90, ress[k], ROSE, 26)
    log = sections("TWO", "HOW a WAS FETCHED")[0]
    header(d, 60, 680, "HOW a WAS FETCHED (the shop's log)", CYAN)
    for i, ln in enumerate(log):
        fit_mono(d, 80, 750 + i * 40, ln, TEXT, 24, 1760, "lm")
    caption(d, "Nobody outside can reach these hosts. The shop did, and answered 200.", AMBER, 900)


def many_spellings(img, d, cfg):
    """Act three: each request, what the blocklist decided from the text, the real address, and the result."""
    blocked = sections("THREE", "THE BLOCKLIST")[0][0]
    reqs = keyed(sections("THREE", "THE REQUESTS")[0])
    ress = dict(keyed(sections("THREE", "THE RESPONSES")[0]))
    java = sections("THREE", "WHAT JAVA CONNECTS TO")[0]
    real = {ln[0]: ln.rsplit(" ", 1)[1] for ln in java if " -> " in ln}
    real["f"] = java[-1].split(" to ")[1].split(":")[0]
    header(d, 60, 190, "THE BLOCKLIST, read as text", CYAN)
    fit_mono(d, 560, 212, blocked, CYAN, 26, 1300, "lm")
    for j, h in enumerate(("THE REQUEST (host)", "THE BLOCKLIST", "WHERE IT GOES", "THE RESPONSE")):
        label(d, (60, 640, 900, 1260)[j], 262, h, MUTED, 20, True)
    for i, (k, addr) in enumerate(reqs):
        y = 295 + i * 74
        host = re.match(r"http://([^/]+)", addr).group(1)
        res = ress[k]
        stopped = " refused" in res
        cell(d, 60, y, 560, 60, k + "  " + host, AMBER, 24)
        cell(d, 640, y, 220, 60, "blocked" if stopped else "passes", GREEN if stopped else ROSE, 24)
        addr_real = real.get(k, host)
        cell(d, 900, y, 320, 60, addr_real, ROSE if inside(addr_real) else GREEN, 24)
        fit_mono(d, 1260, y + 30, res, GREEN if stopped else ROSE, 22, 600, "lm")
    steps = sections("THREE", "HOW 2852039166")[0]
    panel(d, 60, 755, 1800, 120, CYAN)
    label(d, 85, 770, "how b becomes an address: each part times its weight in base 256", CYAN, 22, True)
    parts = [re.match(r"(\d+) x (\d+)\s+=\s+(\d+)", s).groups() for s in steps[:-1]]
    working = " + ".join("%s x %s" % (p[0], p[1]) for p in parts) + " = " + steps[-1].split("=")[1].strip()
    fit_mono(d, 85, 830, working, TEXT, 26, 1750, "lm")
    caption(d, "Five spellings pass the list; the connection still goes inside.", AMBER, 900)


def checked_address(img, d, cfg):
    """Act four: the importer's checks on the redirect, and every request refused by its real address."""
    trace = sections("FOUR", "THE CHECKS")[0]
    header(d, 60, 190, "THE CHECKS FOR f, AS LOGGED", CYAN)
    for i, t in enumerate(trace):
        y = 252 + i * 76
        c = ROSE if t.startswith("refused") else AMBER if "redirect" in t else GREEN
        cell(d, 60, y, 880, 60, t, c, 22)
        if i < len(trace) - 1:
            draw.arrow(d, 500, y + 60, 500, y + 76, MUTED, 3)
    reqs = keyed(sections("FOUR", "THE REQUESTS")[0])
    ress = dict(keyed(sections("FOUR", "THE RESPONSES")[0]))
    header(d, 980, 190, "THE REQUESTS AND THE SHOP'S ANSWER", GREEN)
    for i, (k, addr) in enumerate(reqs):
        y = 255 + i * 66
        host = re.match(r"http://([^/]+)", addr).group(1)
        fit_mono(d, 990, y + 26, k + "  " + host, AMBER, 22, 330, "lm")
        fit_mono(d, 1330, y + 26, ress[k].replace("refused: ", ""), GREEN, 22, 530, "lm")
    caption(d, "Look the name up once, test every answer, connect to the address you tested.", AMBER, 900)


def limits(img, d, cfg):
    """Act five: each limit in the code, the request that tests it, and the shop's answer."""
    lims = [ln.split(None, 1) for ln in sections("FIVE", "THE LIMITS")[0]]
    reqs = keyed(sections("FIVE", "THE REQUESTS")[0])
    ress = dict(keyed(sections("FIVE", "THE RESPONSES")[0]))
    for j, (h, c) in enumerate((("THE LIMIT", CYAN), ("THE REQUEST", AMBER), ("THE RESPONSE", GREEN))):
        header(d, (60, 620, 1240)[j], 200, h, c)
    for i, ((name, rule), (k, addr)) in enumerate(zip(lims, reqs)):
        y = 280 + i * 140
        panel(d, 60, y, 520, 100, CYAN, 3)
        label(d, 85, y + 20, name, CYAN, 24, True)
        fit_mono(d, 85, y + 68, rule, TEXT, 26, 470, "lm")
        cell(d, 620, y, 580, 100, addr, AMBER, 24)
        cell(d, 1240, y, 620, 100, ress[k], GREEN, 24)
    caption(d, "Web ports only, images only, small, and quick: anything else is refused.", AMBER, 880)


def two_lookups(img, d, cfg):
    """Act six: check-then-connect asks twice and gets two answers; the guarded importer asks once."""
    twice = sections("SIX", "CHECK, THEN CONNECT")[0]
    res = [s[0] for s in sections("SIX", "THE RESPONSE")]
    once = sections("SIX", "THE GUARDED IMPORTER")[0]
    req = sections("SIX", "THE REQUEST")[0][0]
    header(d, 60, 190, "THE REQUEST", AMBER)
    fit_mono(d, 330, 212, req, AMBER, 30, 1500, "lm")
    for col, (title, lines, result, c) in enumerate((
            ("CHECK, THEN CONNECT BY NAME", twice[:3], res[0], ROSE),
            ("THE GUARDED IMPORTER", once[1:4], res[1], GREEN))):
        x = 60 + col * 920
        header(d, x, 280, title, c)
        for i, ln in enumerate(lines):
            y = 350 + i * 100
            colour = GREEN if (i == 0 or col == 1) else ROSE
            cell(d, x, y, 860, 72, ln, colour, 22)
            if i < len(lines) - 1:
                draw.arrow(d, x + 430, y + 72, x + 430, y + 100, MUTED, 3)
        draw.arrow(d, x + 430, 622, x + 430, 668, MUTED, 3)
        body, mark = result.split("  <- ")
        cell(d, x, 670, 860, 90, body, c, 26)
        label(d, x + 430, 775, mark.lower(), c, 24, True, "ma")
    caption(d, "A check is only as good as the address it checked: connect to that one.", AMBER, 880)
