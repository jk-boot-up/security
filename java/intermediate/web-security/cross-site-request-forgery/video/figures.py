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


# ------------------------------------------------------------------ figures

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


def cookie_rides_along(img, d, cfg):
    """The core idea: two pages send to the same bank, and the browser attaches the same cookie to both."""
    req = sections("ONE", "THE REQUEST")[0]
    cookie = next(l for l in req if l.startswith("Cookie:"))
    panel(d, 60, 220, 600, 240, GREEN, 3)
    label(d, 90, 250, "tab 1: https://bank.example", GREEN, 30, True)
    label(d, 90, 320, "the bank's own transfer form", TEXT, 28)
    label(d, 90, 375, "Ana fills it in and presses Send", MUTED, 26)
    panel(d, 60, 560, 600, 240, ROSE, 3)
    label(d, 90, 590, "tab 2: https://evil.example", ROSE, 30, True)
    label(d, 90, 660, "a hidden form that posts itself", TEXT, 28)
    label(d, 90, 715, "Ana only opened the page", MUTED, 26)
    panel(d, 760, 360, 480, 300, CYAN, 3)
    label(d, 1000, 390, "Ana's browser", CYAN, 32, True, "ma")
    label(d, 1000, 460, "cookie jar for bank.example:", MUTED, 24, False, "ma")
    fit_mono(d, 1000, 515, cookie.split(": ", 1)[1], AMBER, 28, 440, "ma")
    label(d, 1000, 590, "added to every request to the bank", MUTED, 22, False, "ma")
    draw.arrow(d, 660, 340, 760, 440, GREEN, 5)
    draw.arrow(d, 660, 680, 760, 580, ROSE, 5)
    panel(d, 1340, 360, 520, 300, LINE, 3)
    label(d, 1600, 390, "https://bank.example", TEXT, 30, True, "ma")
    fit_mono(d, 1600, 470, "POST /transfer", TEXT, 28, 480, "ma")
    fit_mono(d, 1600, 525, cookie, AMBER, 26, 480, "ma")
    label(d, 1600, 595, "both requests look logged in", ROSE, 24, True, "ma")
    draw.arrow_right(d, 1240, 480, 1340, GREEN, 5)
    draw.arrow_right(d, 1240, 540, 1340, ROSE, 5)
    caption(d, "The cookie says who is asking. It cannot say which page sent the request.", AMBER, 880)


def forged_request(img, d, cfg):
    """Act one: Ana logs in, a page on evil.example posts the transfer, and the bank answers."""
    login = sections("ONE", "ANA LOGS IN")[0]
    req = sections("ONE", "THE REQUEST")[0]
    res = sections("ONE", "THE RESPONSE")[0]
    header(d, 60, 200, "ANA LOGS IN", CYAN)
    panel(d, 60, 260, 520, 450, CYAN)
    setc = login[1].split(": ", 1)[1].split("; ")
    lines_in(d, 85, 285, 470, [login[0], "Set-Cookie:"] + ["  " + p for p in setc] + [login[2]],
             [GREEN, MUTED] + [AMBER] * len(setc) + [TEXT], 26, 48)
    header(d, 640, 200, "THE REQUEST (from evil.example)", ROSE)
    panel(d, 640, 260, 700, 450, ROSE, 3)
    lines_in(d, 665, 300, 650, req, [TEXT, TEXT, AMBER, ROSE, ROSE], 28, 80)
    header(d, 1400, 200, "THE RESPONSE", AMBER)
    panel(d, 1400, 260, 460, 450, ROSE, 3)
    resp, note = res[0].split("  <- ")
    lines_in(d, 1425, 300, 420, [resp, "", res[1]], [ROSE, TEXT, ROSE], 28, 80)
    label(d, 1425, 380, note.lower(), ROSE, 24, True)
    draw.arrow_right(d, 580, 485, 640, MUTED, 4)
    draw.arrow_right(d, 1340, 485, 1400, MUTED, 4)
    label(d, 960, 760, "The browser chose the cookie by where the request went: bank.example.", TEXT, 30, False, "ma")
    label(d, 960, 810, "It did not ask which page started it.", TEXT, 30, False, "ma")
    caption(d, "A valid cookie proves who is asking, not what Ana meant.", AMBER, 890)


def token_check(img, d, cfg):
    """Act two: how the token is made, and the three requests, their check and their response."""
    form = sections("TWO", "THE BANK'S FORM")[0][0]
    made = sections("TWO", "HOW THE TOKEN IS MADE")[0]
    reqs = numbered("TWO", "THE REQUESTS")
    checks = numbered("TWO", "THE CHECK")
    resps = numbered("TWO", "THE RESPONSES")
    header(d, 60, 195, "THE BANK'S FORM", CYAN)
    fit_mono(d, 60, 265, form, AMBER, 26, 900)
    header(d, 1000, 195, "HOW THE TOKEN IS MADE", CYAN)
    lines_in(d, 1000, 255, 860, made, [TEXT, TEXT, GREEN], 24, 38)
    cols = [(60, "THE REQUEST", AMBER), (720, "THE CHECK", CYAN), (1300, "THE RESPONSE", GREEN)]
    for x, t, c in cols:
        header(d, x, 400, t, c)
    for i, n in enumerate(sorted(reqs)):
        y = 465 + i * 135
        who, rest = reqs[n][:17].strip(), reqs[n][17:].strip()
        c = GREEN if resps[n].startswith(("200", "403")) else ROSE
        panel(d, 60, y, 1800, 115, c, 3)
        label(d, 85, y + 18, n + "  " + who, TEXT, 28, True)
        fit_mono(d, 85, y + 66, re.sub(r"\s{2,}", "   ", rest), AMBER, 24, 600)
        lines_in(d, 720, y + 22, 560, wrap(checks[n], 36), [CYAN] * 3, 24, 36)
        lines_in(d, 1300, y + 22, 540, wrap(resps[n], 34), [c] * 3, 26, 38)
    caption(d, "Another site can send the form, but it cannot read the token.", AMBER, 890)


def samesite_rules(img, d, cfg):
    """Act three: how the browser finds the site, and the six requests with and without the cookie."""
    find = sections("THREE", "HOW THE BROWSER FINDS THE SITE")[0]
    rule = sections("THREE", "THE RULE")[0]
    heads = [ln for ln in act("THREE") if ln.startswith("THE REQUEST")]
    reqs = numbered("THREE", "THE REQUEST")
    resps = numbered("THREE", "THE RESPONSE")
    header(d, 60, 195, "THE SITE: the last two labels of the host", CYAN)
    lines_in(d, 60, 260, 820, find, [ROSE, GREEN, AMBER], 24, 40)
    header(d, 940, 195, "THE RULE for a cross-site request", CYAN)
    lines_in(d, 940, 260, 920, rule, [ROSE, AMBER, GREEN], 24, 40)
    groups = [("1", "2", "3"), ("4", "5"), ("6",)]
    y = 400
    for g, h in zip(groups, heads):
        label(d, 60, y, re.search(r"\((.*)\)", h).group(1), CYAN, 22, True)
        y += 36
        for n in g:
            what, sent = reqs[n].rsplit("cookie: ", 1)
            c = AMBER if n == "4" else GREEN
            panel(d, 60, y, 1800, 48, LINE)
            fit_mono(d, 80, y + 24, n + " " + what.strip(), TEXT, 22, 760, "lm")
            fit_mono(d, 880, y + 24, "cookie: " + sent, GREEN if sent == "sent" else ROSE, 22, 300, "lm")
            fit_mono(d, 1200, y + 24, resps[n], c, 22, 640, "lm")
            y += 56
        y += 10
    caption(d, "Lax keeps the cookie off forged posts, and still lets links work.", AMBER, 935)


def where_from(img, d, cfg):
    """Act four: how Sec-Fetch-Site is picked, the three checked requests, and the state-changing GET."""
    picks = sections("FOUR", "HOW THE BROWSER PICKS")[0]
    reqs = numbered("FOUR", "THE REQUESTS")
    resps = numbered("FOUR", "THE RESPONSES")
    get = sections("FOUR", "THE REQUEST (")[0]
    get_res = sections("FOUR", "THE RESPONSES")[1]
    header(d, 60, 195, "HOW THE BROWSER PICKS Sec-Fetch-Site", CYAN)
    for j, ln in enumerate(picks):
        page, how, val = re.match(r"(\S+)\s+(.+?)\s+-> (\S+)", ln).groups()
        y = 265 + j * 60
        fit_mono(d, 60, y, page, TEXT, 24, 470)
        label(d, 540, y, how, MUTED, 24)
        fit_mono(d, 720, y, val, GREEN if val == "same-origin" else AMBER if val == "same-site" else ROSE, 24, 200)
    header(d, 960, 195, "THE CHECKED REQUESTS -> THE RESPONSES", AMBER)
    for i, n in enumerate(sorted(reqs)):
        y = 260 + i * 90
        who, hdr = re.split(r"\s{2,}", reqs[n], maxsplit=1)
        fit_mono(d, 960, y, n + " " + who + ":  " + hdr, TEXT, 22, 900)
        fit_mono(d, 1000, y + 36, resps[n], GREEN if resps[n].startswith(("2", "4")) else ROSE, 22, 860)
    header(d, 60, 540, "A LINK THAT MOVES MONEY", ROSE)
    panel(d, 60, 600, 1800, 270, ROSE, 3)
    lines_in(d, 85, 620, 1000, get, [TEXT, AMBER, MUTED], 26, 44)
    lines_in(d, 85, 760, 1000, [get_res[0], get_res[1].strip()], [MUTED, ROSE], 26, 44)
    fit_mono(d, 1100, 760, get_res[2], MUTED, 26, 740)
    fit_mono(d, 1100, 804, get_res[3].strip(), GREEN, 26, 740)
    caption(d, "Headers say where a request came from. GET must never change anything.", AMBER, 905)


def the_bill(img, d, cfg):
    """Act five: what each defence still misses, and why a bearer-token API is different."""
    reqs = numbered("FIVE", "THE REQUESTS")
    reqs.update(numbered("FIVE", "THE API REQUESTS"))
    resps = numbered("FIVE", "THE RESPONSES")
    header(d, 60, 195, "THE REQUEST", AMBER)
    header(d, 1080, 195, "THE RESPONSE", GREEN)
    for i, n in enumerate(sorted(reqs)):
        y = 260 + i * 100 + (30 if int(n) > 4 else 0)
        res, _, note = resps[n].partition("  <- ")
        c = ROSE if res.startswith("200") and n != "6" else (AMBER if n == "4" else GREEN)
        panel(d, 60, y, 1800, 84, c, 3)
        fit_mono(d, 85, y + 42, n + " " + reqs[n], TEXT, 22, 960, "lm")
        fit_mono(d, 1080, y + 28, res, c, 24, 760, "lm")
        if note:
            label(d, 1080, y + 60, note.lower(), c, 20, True, "lm")
    label(d, 60, 655, "the bank's API (POST /api/transfer), with a token in a header:", CYAN, 22, True)
    caption(d, "SameSite alone leaks; the token must reach every form; headers protect APIs.", AMBER, 930)
