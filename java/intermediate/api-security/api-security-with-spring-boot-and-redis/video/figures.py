"""Pictures for the API security with Spring Boot and Redis video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def fields(lines):
    """Record lines "name   value  <- MARK" as (name, value, mark)."""
    out = []
    for ln in lines:
        m = re.match(r"(\S+)\s+(.*?)(?:  <- (.*))?$", ln)
        out.append(m.groups(""))
    return out


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 18:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def request_box(d, x, y, w, lines, colour=AMBER, size=26):
    """A request: the method and path, then the JSON body, in a panel. Returns the bottom."""
    h = 30 + 42 * len(lines)
    panel(d, x, y, w, h, colour, 3)
    for j, ln in enumerate(lines):
        fit_mono(d, x + 24, y + 18 + j * 42, ln, TEXT if j == 0 else colour, size, w - 40)
    return y + h


# ------------------------------------------------------------------ figures

def three_gates(img, d, cfg):
    """Where each limit sits in the Spring Boot request path, each with the real answer it gives."""
    no_login = sections("THREE", "THE RESPONSE:")[-1][0].split(",")[0].strip()
    limited = sections("FOUR", "THE RESPONSES")[0][1].strip().split("with ")[1]
    unknown = sections("TWO", "THE REQUESTS AND RESPONSES")[0][3].replace("->", "").strip()
    card = [v for n, v, _ in fields(sections("THREE", "THE RESPONSE (200, an AccountView")[0][:5]) if n == "cardLast4"][0]
    gates = [("Spring Security", "who is calling?", "no login: " + no_login, CYAN),
             ("RateLimitFilter", "how much? (Redis)", limited, AMBER),
             ("Jackson + Validation", "what may be set?", unknown, ROSE),
             ("AccountView", "what may be seen?", "card " + card, GREEN)]
    header(d, 60, 190, "ONE REQUEST, FROM THE APP TO THE ANSWER", CYAN)
    w, gap, y = 400, 40, 300
    for i, (name, q, ans, c) in enumerate(gates):
        x = 60 + i * (w + gap)
        panel(d, x, y, w, 300, c, 3)
        label(d, x + w // 2, y + 40, name, TEXT, 28, True, "ma")
        label(d, x + w // 2, y + 100, q, c, 26, True, "ma")
        fit_mono(d, x + w // 2, y + 220, ans, c, 26, w - 30, "mm")
        if i < 3:
            draw.arrow_right(d, x + w + 4, y + 150, x + w + gap - 4, MUTED, 4)
    label(d, 60, 650, "Each gate refuses on its own; a refused request never reaches the next one.", MUTED, 28, True)
    caption(d, "Who, how much, what in, what out: four checks, four places in Spring.", AMBER, 830)


def bound_record(img, d, cfg):
    """Act one: two hand-added keys bound onto the stored record, and the nine fields that come back."""
    rec = fields(sections("ONE", "THE RESPONSE (200, the stored")[0])
    bad = {n: v for n, v, _ in fields(sections("ONE", "THE RESPONSE (200, every key")[0])}
    req = sections("ONE", "THE REQUEST (two keys")[0]
    page = sections("ONE", "THE RESPONSE (200):")[0][0].strip().split("  <- ")
    header(d, 60, 190, "THE REQUEST", AMBER)
    request_box(d, 60, 250, 820, req, AMBER, 24)
    label(d, 60, 390, "@RequestBody Account: every key is bound", MUTED, 26, True)
    header(d, 60, 470, "THE LIST, size=1000000", AMBER)
    fit_mono(d, 60, 540, page[0], ROSE, 26, 820)
    label(d, 60, 590, page[1].lower(), ROSE, 26, True)
    header(d, 960, 190, "THE RESPONSE: THE STORED RECORD", ROSE)
    panel(d, 960, 250, 900, 20 + 58 * len(rec), ROSE)
    for i, (name, value, _) in enumerate(rec):
        y = 265 + i * 58
        changed = name in bad
        mono(d, 985, y + 22, name, ROSE if changed else TEXT, 26, "lm")
        shown = bad.get(name, value)
        fit_mono(d, 1290, y + 22, shown + ("  (was %s)" % value if changed else ""), ROSE if changed or name in ("cardNumber", "pinHash") else MUTED, 26, 550, "lm")
    caption(d, "Every key in, every field out, every customer on one page.", AMBER, 860)


def request_model(img, d, cfg):
    """Act two: the request model's limits, and each request with its answer."""
    model = fields(sections("TWO", "THE MODEL")[0])
    pairs = sections("TWO", "THE REQUESTS AND RESPONSES")[0]
    header(d, 60, 190, "THE MODEL: UpdateProfileRequest", CYAN)
    for i, ln in enumerate(sections("TWO", "THE MODEL")[0]):
        name, rule = ln[:18].strip(), ln[18:].strip()
        y = 250 + i * 50
        mono(d, 60, y, name, TEXT, 26)
        fit_mono(d, 400, y, rule, CYAN, 26, 1460)
    header(d, 60, 430, "THE REQUEST   ->   THE RESPONSE", AMBER)
    for i in range(0, len(pairs), 2):
        y = 490 + (i // 2) * 62
        req, res = pairs[i].strip(), pairs[i + 1].replace("->", "").strip()
        ok = res.startswith("200")
        cell(d, 60, y, 1000, 52, req, AMBER, 24)
        draw.arrow_right(d, 1075, y + 26, 1150, MUTED, 4)
        mono(d, 1170, y + 26, res, GREEN if ok else ROSE, 26, "lm")
    caption(d, "Two named fields with limits; anything else is a 400, and nothing is applied.", AMBER, 900)


def response_model(img, d, cfg):
    """Act three: the five-field view, the page ceiling, and the two refusals."""
    view = fields(sections("THREE", "THE RESPONSE (200, an AccountView")[0][:5])
    cap = sections("THREE", "THE PAGE SIZE")[0][0].strip()
    capline = heading("THREE", "THE PAGE SIZE").split("(")[1].rstrip("):")
    rows = sections("THREE", "THE RESPONSE (200, a Directory")[0][0].strip()
    zero = sections("THREE", "THE RESPONSE:")[0][0].strip()
    nolog = sections("THREE", "THE RESPONSE:")[1][0].strip()
    header(d, 60, 190, "THE RESPONSE: an AccountView", GREEN)
    for i, (name, value, _) in enumerate(view):
        y = 250 + i * 66
        panel(d, 60, y, 860, 56, GREEN)
        mono(d, 85, y + 28, name, TEXT, 26, "lm")
        fit_mono(d, 360, y + 28, value, GREEN, 26, 540, "lm")
    label(d, 60, 600, sections("THREE", "THE RESPONSE (200, an AccountView")[0][5].strip(), MUTED, 26, True)
    header(d, 1000, 190, "THE PAGE SIZE (" + capline + ")", AMBER)
    mono(d, 1000, 260, cap, AMBER, 30)
    fit_mono(d, 1000, 320, rows, GREEN, 26, 860)
    header(d, 1000, 420, "size=0", ROSE)
    fit_mono(d, 1000, 490, zero, ROSE, 26, 860)
    header(d, 1000, 560, "no login", ROSE)
    fit_mono(d, 1000, 630, nolog, ROSE, 26, 860)
    caption(d, "Five fields out, at most fifty rows a page, and no answer without a login.", AMBER, 830)


def redis_bucket(img, d, cfg):
    """Act four: the bucket in Redis emptied by a loop, the Retry-After sum, and the refill."""
    budget = sections("FOUR", "THE BUDGET")[0][0].strip()
    resp = [l.strip() for l in sections("FOUR", "THE RESPONSES")[0]]
    how = [l.strip() for l in sections("FOUR", "HOW Retry-After")[0]]
    later = sections("FOUR", "THE RESPONSE:")[0][0].strip()
    keys = sections("FOUR", "THE KEYS IN REDIS")[0][0].strip()
    answered = int(resp[0].split()[0])
    refused = int(resp[1].split()[0])
    header(d, 60, 190, "THE BUDGET, IN REDIS", CYAN)
    mono(d, 60, 260, budget, TEXT, 26)
    x0, x1, y = 60, 1860, 320
    split = x0 + int((x1 - x0) * answered / (answered + refused))
    d.rounded_rectangle([x0, y, split, y + 70], radius=12, fill=(20, 60, 50), outline=GREEN, width=2)
    d.rounded_rectangle([split, y, x1, y + 70], radius=12, fill=(70, 30, 40), outline=ROSE, width=2)
    label(d, (x0 + split) // 2, y + 35, str(answered), GREEN, 26, True, "mm")
    label(d, (split + x1) // 2, y + 35, "%d refused: 429" % refused, ROSE, 26, True, "mm")
    label(d, 60, 410, "1000 requests at one moment: the bucket held %d tokens" % answered, MUTED, 26, True)
    header(d, 60, 480, "HOW Retry-After IS WORKED OUT", AMBER)
    for i, ln in enumerate(how):
        mono(d, 60, 545 + i * 44, ln, AMBER if i else TEXT, 26)
    header(d, 1100, 480, "ONE SECOND LATER", GREEN)
    fit_mono(d, 1100, 545, later, GREEN, 26, 760)
    header(d, 1100, 620, "THE KEYS IN REDIS", CYAN)
    mono(d, 1100, 690, keys, CYAN, 26)
    caption(d, "One bucket per caller, one script per request, a wait the app can read.", AMBER, 860)


def shared_budget(img, d, cfg):
    """Act five: two servers with a budget each, two servers sharing Redis, and Redis stopping."""
    local = sections("FIVE", "TWO SERVERS, A BUDGET")[0][1].replace("->", "").strip().split("  <- ")
    shared = sections("FIVE", "TWO SERVERS, ONE BUDGET")[0][1].replace("->", "").strip()
    down = sections("FIVE", "REDIS STOPS")[0][0].split("->")[1].strip()
    up = sections("FIVE", "REDIS ANSWERS")[0][0].split("->")[1].strip()
    for k, (title, res, c, shared_store) in enumerate((("A BUDGET IN EACH SERVER'S MEMORY", local[0], ROSE, False),
                                                       ("ONE BUDGET IN REDIS", shared, GREEN, True))):
        x = 60 + k * 920
        header(d, x, 190, title, c)
        for j, name in enumerate(("server A", "server B")):
            bx = x + j * 440
            panel(d, bx, 270, 380, 90, LINE)
            label(d, bx + 190, 315, name, TEXT, 28, True, "mm")
            if not shared_store:
                cell(d, bx + 90, 400, 200, 60, "bucket 50", AMBER, 24)
            else:
                draw.arrow(d, bx + 190, 362, x + 410, 420, MUTED, 3)
        if shared_store:
            cell(d, x + 290, 420, 260, 64, "Redis: bucket 50", CYAN, 24)
        mono(d, x, 540, res, c, 30)
        if not shared_store:
            label(d, x, 590, local[1].lower(), ROSE, 26, True)
    header(d, 60, 670, "REDIS STOPS ANSWERING", ROSE)
    mono(d, 60, 740, down, ROSE, 28)
    header(d, 980, 670, "REDIS ANSWERS AGAIN", GREEN)
    mono(d, 980, 740, up, GREEN, 28)
    caption(d, "Count in one place every server shares, and refuse when it cannot count.", AMBER, 860)
