"""Pictures for the cross-site request forgery video, drawn by the `figure` slide kind.

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


def numbered(name, head):
    """The numbered lines ("1 ...") of every section starting with `head`, as {number: text}."""
    out = {}
    for sec in sections(name, head):
        for ln in sec:
            m = re.match(r"(\d) (.*)", ln)
            if m:
                out[m.group(1)] = m.group(2)
    return out


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


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def lines_in(d, x, y, w, lines, colours, size=24, step=40):
    for j, (ln, c) in enumerate(zip(lines, colours)):
        fit_mono(d, x, y + j * step, ln, c, size, w)


def verdict_colour(response):
    return GREEN if response.startswith(("401", "403", "405")) else ROSE




def wrap(text, width):
    """Split text into lines of at most `width` characters, at spaces."""
    out, cur = [], ""
    for w in text.split(" "):
        if cur and len(cur) + 1 + len(w) > width:
            out.append(cur)
            cur = w
        else:
            cur = (cur + " " + w).strip()
    return out + [cur]


def rows(d, reqs, resps, y0, step=96, split=1000, size=24):
    """Numbered requests on the left and their responses on the right, one panel per pair."""
    for i, n in enumerate(sorted(reqs)):
        y = y0 + i * step
        res, _, note = resps[n].partition("  <- ")
        res, _, aside = res.partition("  (")
        c = ROSE if note else GREEN
        panel(d, 60, y, 1800, step - 14, c, 3)
        fit_mono(d, 85, y + (step - 14) // 2, n + " " + reqs[n], TEXT, size, split - 110, "lm")
        fit_mono(d, split, y + (step - 14) // 2 - (14 if note or aside else 0), res, c, size, 1840 - split, "lm")
        extra = note.lower() if note else aside.rstrip(")")
        if extra:
            label(d, split, y + (step - 14) // 2 + 20, extra, c if note else MUTED, 20, bool(note), "lm")


# ------------------------------------------------------------------ figures

def spring_filter(img, d, cfg):
    """The core idea: Spring Security's CSRF filter compares the request's token with the session's, before the controller runs."""
    reqs = numbered("TWO", "THE REQUESTS (POST /transfer")
    resps = numbered("TWO", "THE RESPONSES")
    cases = [("2", ROSE), ("4", GREEN)]
    header(d, 60, 200, "THE REQUEST", AMBER)
    header(d, 720, 200, "SPRING SECURITY'S CSRF FILTER", CYAN)
    header(d, 1440, 200, "THE RESPONSE", GREEN)
    panel(d, 720, 270, 640, 470, CYAN, 3)
    steps = ["GET, HEAD, OPTIONS, TRACE?", "  -> no check: they must not act", "POST, PUT, PATCH, DELETE:",
             "  token from the X-CSRF-TOKEN header", "  or the _csrf form field", "  unmasked, then compared with",
             "  the token kept in the session", "  missing or different -> 403"]
    lines_in(d, 745, 295, 600, steps, [MUTED, MUTED, TEXT, TEXT, TEXT, TEXT, TEXT, ROSE], 24, 54)
    for i, (n, c) in enumerate(cases):
        y = 290 + i * 250
        panel(d, 60, y, 600, 190, c, 3)
        lines_in(d, 85, y + 25, 560, wrap(reqs[n], 30), [TEXT] * 3, 26, 40)
        label(d, 85, y + 145, "with Ana's session cookie", MUTED, 22)
        draw.arrow_right(d, 660, y + 95, 720, c, 4)
        draw.arrow_right(d, 1360, y + 95, 1440, c, 4)
        panel(d, 1440, y, 420, 190, c, 3)
        fit_mono(d, 1460, y + 95, resps[n], c, 26, 380, "lm")
    caption(d, "The cookie says who is asking; only the bank's own page holds the token.", AMBER, 820)


def csrf_off(img, d, cfg):
    """Act one: the switch that turns the check off, the forged request, and the response."""
    conf = sections("ONE", "THE CONFIGURATION")[0][0]
    sign = sections("ONE", "ANA SIGNS IN")[0]
    req = sections("ONE", "THE REQUEST")[0]
    res = sections("ONE", "THE RESPONSE")[0]
    header(d, 60, 195, "THE CONFIGURATION", CYAN)
    fit_mono(d, 60, 265, conf, ROSE, 34, 900)
    header(d, 1000, 195, "ANA SIGNS IN", CYAN)
    lines_in(d, 1000, 260, 860, sign, [AMBER, TEXT], 24, 40)
    header(d, 60, 380, "THE REQUEST (a hidden form on evil.example)", ROSE)
    panel(d, 60, 440, 1100, 260, ROSE, 3)
    lines_in(d, 85, 470, 1050, req, [TEXT, AMBER, ROSE], 28, 70)
    header(d, 1220, 380, "THE RESPONSE", AMBER)
    panel(d, 1220, 440, 640, 260, ROSE, 3)
    resp, note = res[0].split("  <- ")
    lines_in(d, 1245, 470, 600, [resp, note.lower(), res[1]], [ROSE, ROSE, TEXT], 28, 70)
    draw.arrow_right(d, 1160, 570, 1220, MUTED, 4)
    caption(d, "With the check off, the cookie alone moves the money.", AMBER, 790)


def token_mask(img, d, cfg):
    """Act two: how a page's token is masked, one example byte, and the token masked by hand."""
    steps = sections("TWO", "HOW THE TOKEN IS MASKED")[0]
    byte = ";   ".join(sections("TWO", "ONE BYTE OF IT")[0])
    reqs = numbered("TWO", "THE REQUESTS (POST, each")
    resps = numbered("TWO", "THE RESPONSES")
    m = re.search(r"(\d+) random bytes \+ \(token XOR random\) = (\d+) bytes", steps[1])
    n, total = m.groups()
    header(d, 60, 195, "ONE PAGE'S MASKED TOKEN", CYAN)
    cells = [(n + " random bytes", MUTED), ("token XOR random: " + n + " bytes", AMBER)]
    x = 60
    for t, c in cells:
        w = 600 if c == MUTED else 760
        d.rounded_rectangle([x, 260, x + w, 340], radius=12, fill=BG_CELL, outline=c, width=3)
        label(d, x + w // 2, 300, t, c, 28, True, "mm")
        x += w + 20
    label(d, 1480, 300, "= " + total + " bytes", TEXT, 30, True, "lm")
    lines_in(d, 60, 380, 1800, [steps[0], steps[2], steps[3], steps[4]], [TEXT, TEXT, GREEN, GREEN], 26, 46)
    header(d, 60, 590, "ONE BYTE OF IT", AMBER)
    fit_mono(d, 60, 655, byte, AMBER, 28, 1800)
    header(d, 60, 730, "A TOKEN MASKED BY HAND", GREEN)
    fit_mono(d, 60, 795, reqs["5"] + "  ->  " + resps["5"], GREEN, 26, 1800)
    caption(d, "A new mask on every page; the same token underneath.", AMBER, 880)


def spa_cookie(img, d, cfg):
    """Act three: the readable XSRF-TOKEN cookie, the header the script copies it into, and the three requests."""
    cookie = sections("THREE", "THE COOKIE THE BANK SETS")[0]
    reqs = numbered("THREE", "THE REQUESTS")
    resps = numbered("THREE", "THE RESPONSES")
    header(d, 60, 195, "THE COOKIE THE BANK SETS", CYAN)
    fit_mono(d, 60, 265, cookie[0], AMBER, 28, 900)
    lines_in(d, 60, 320, 900, [cookie[1], cookie[2]], [ROSE, ROSE], 24, 38)
    header(d, 1000, 195, "THE RULE", CYAN)
    lines_in(d, 1000, 260, 860, ["the X-XSRF-TOKEN header must", "equal the XSRF-TOKEN cookie;", "only the bank's own script",
                                 "can read the cookie to copy it"], [TEXT, TEXT, GREEN, GREEN], 26, 40)
    header(d, 60, 440, "THE REQUEST", AMBER)
    header(d, 1100, 440, "THE RESPONSE", GREEN)
    rows(d, reqs, resps, 500, 110, 1100, 26)
    caption(d, "Another site's page cannot read the bank's cookie, so it cannot copy it.", AMBER, 860)


def default_gaps(img, d, cfg):
    """Act four: the exempt webhook, the GET routes, and one line of configuration that undoes the defence."""
    reqs = numbered("FOUR", "THE REQUESTS")
    resps = numbered("FOUR", "THE RESPONSES")
    conf = sections("FOUR", "THE CONFIGURATION, AFTER")[0][0]
    last = sections("FOUR", "THE RESPONSE:")[0][0]
    header(d, 60, 195, "THE REQUEST", AMBER)
    header(d, 1000, 195, "THE RESPONSE", GREEN)
    rows(d, reqs, resps, 255, 100, 1000, 24)
    header(d, 60, 670, "ONE CHANGE TO PASS A TEST", ROSE)
    panel(d, 60, 730, 1800, 110, ROSE, 3)
    fit_mono(d, 85, 785, conf, ROSE, 28, 800, "lm")
    res, _, note = last.partition("  <- ")
    fit_mono(d, 1000, 770, res, ROSE, 28, 840, "lm")
    label(d, 1000, 810, note.lower(), ROSE, 22, True, "lm")
    caption(d, "Exemptions need their own proof, and GET must never act.", AMBER, 890)


def the_bill(img, d, cfg):
    """Act five: the session cookie's flags, the API chain with Basic credentials, and every route refused."""
    cookie = sections("FIVE", "THE SESSION COOKIE")[0]
    api = sections("FIVE", "THE API REQUESTS")[0]
    resps = numbered("FIVE", "THE RESPONSES")
    routes = "   ->  ".join(sections("FIVE", "EACH STATE-CHANGING")[0])
    lax = sections("FIVE", "WITH server")[0]
    header(d, 60, 195, "THE SESSION COOKIE BY DEFAULT", CYAN)
    lines_in(d, 60, 260, 880, cookie, [AMBER, ROSE], 24, 40)
    header(d, 1000, 195, "WITH same-site=lax", CYAN)
    lines_in(d, 1000, 260, 860, lax, [GREEN, GREEN, MUTED], 22, 34)
    header(d, 60, 370, "THE API CHAIN: a header, no session", AMBER)
    for i, n in enumerate(("1", "2")):
        y = 430 + i * 130
        c = GREEN
        panel(d, 60, y, 1800, 110, c, 3)
        txt = api[i if n == "1" else 1][2:]
        fit_mono(d, 85, y + 35, txt, TEXT, 24, 1100, "lm")
        if n == "2":
            fit_mono(d, 85, y + 75, api[2].strip(), MUTED, 22, 1100, "lm")
        fit_mono(d, 1260, y + 55, resps[n], c, 26, 580, "lm")
    header(d, 60, 710, "EVERY STATE-CHANGING ROUTE, FROM evil.example, NO TOKEN", GREEN)
    fit_mono(d, 60, 775, routes, GREEN, 28, 1800)
    caption(d, "Keep the token on; SameSite and header-only APIs are extra layers.", AMBER, 880)
