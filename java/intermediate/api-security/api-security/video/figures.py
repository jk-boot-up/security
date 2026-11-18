"""Pictures for the API security video, drawn by the `figure` slide kind.

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

def three_limits(img, d, cfg):
    """Ana's account record, field by field: may she set it, may the app see it. The third limit: how much she may ask."""
    record = fields(sections("ONE", "THE RESPONSE")[0])
    named = heading("FOUR", "THE CHECK").split("names ")[1].rstrip("):").split(", ")
    view = [n for n, _, _ in fields(sections("FOUR", "THE RESPONSE (200, an AccountView")[0])]
    header(d, 60, 190, "ANA'S ACCOUNT RECORD (8 fields)", CYAN)
    label(d, 1180, 260, "MAY SHE SET IT?", MUTED, 24, True, "ma")
    label(d, 1480, 260, "MAY THE APP SEE IT?", MUTED, 24, True, "ma")
    for i, (name, value, _) in enumerate(record):
        y = 300 + i * 64
        panel(d, 60, y, 1720, 54, LINE)
        mono(d, 84, y + 27, name, TEXT, 26, "lm")
        fit_mono(d, 420, y + 27, value, MUTED, 26, 600, "lm")
        ok_set = name in named
        cell(d, 1090, y + 5, 180, 44, "yes" if ok_set else "no", GREEN if ok_set else ROSE, 24)
        seen = name in view or name == "cardNumber"
        cell(d, 1390, y + 5, 180, 44, ("last 4" if name == "cardNumber" else "yes") if seen else "no",
             GREEN if seen else ROSE, 24)
    page = sections("FOUR", "THE PAGE SIZE")[0][0].strip()
    caption(d, "Three limits: what she may set, what she may see, how much she may ask for.", AMBER, 830)
    label(d, W // 2, 940, "How much: a page of at most %s rows, and a budget of calls." % page.split("= ")[1], AMBER, 28, True, "ma")
    label(d, W // 2, 885, "The first version leaves all three to chance.", ROSE, 28, True, "ma")


def whole_record(img, d, cfg):
    """Act one: a one-field request, and the eight-field record that comes back."""
    req = sections("ONE", "THE REQUEST")[0]
    resp = fields(sections("ONE", "THE RESPONSE")[0])
    header(d, 60, 190, "THE REQUEST", AMBER)
    request_box(d, 60, 250, 720, req)
    label(d, 60, 400, "one field to change", AMBER, 26, True)
    draw.arrow_right(d, 800, 330, 900, MUTED, 4)
    header(d, 920, 190, "THE RESPONSE (200)", CYAN)
    sent = [k.split('"')[1] for k in req[1:]]
    panel(d, 920, 250, 940, 30 + 66 * len(resp), CYAN)
    for i, (name, value, _) in enumerate(resp):
        y = 270 + i * 66
        c = GREEN if name in sent else TEXT
        mono(d, 945, y + 24, name, c, 26, "lm")
        fit_mono(d, 1250, y + 24, value, GREEN if name in sent else MUTED, 26, 590, "lm")
    caption(d, act("ONE")[-1].strip().capitalize() + ".", AMBER, 840)


def mass_assignment(img, d, cfg):
    """Act two: two extra keys in the JSON, and the record before and after."""
    req = sections("TWO", "THE REQUEST")[0]
    before = fields(sections("TWO", "THE RECORD BEFORE")[0])
    after = fields(sections("TWO", "THE RESPONSE")[0])
    header(d, 60, 190, "THE REQUEST", AMBER)
    bottom = request_box(d, 60, 250, 760, req)
    for j, ln in enumerate(req[1:], 1):
        if "displayName" not in ln:
            mono(d, 830, 268 + j * 42 + 10, "<- added", ROSE, 24, "lm")
    label(d, 60, bottom + 30, "the API copies every key onto the record", MUTED, 26, True)
    header(d, 1060, 190, "THE RECORD BEFORE", MUTED)
    header(d, 1060, 470, "THE RECORD AFTER", ROSE)
    for k, (rows, y0, c) in enumerate(((before, 250, GREEN), (after, 530, ROSE))):
        for i, (name, value, mark) in enumerate(rows):
            y = y0 + i * 80
            panel(d, 1060, y, 800, 66, c, 3)
            mono(d, 1085, y + 33, name, TEXT, 26, "lm")
            mono(d, 1400, y + 33, value, c, 28, "lm")
    last = act("TWO")[-1].strip()
    caption(d, last[0].upper() + last[1:] + ": " + after[0][2].lower() + ".", ROSE, 830)


def no_limits(img, d, cfg):
    """Act three: what the record leaks, how big an unlimited page is, and a loop nobody slows."""
    leak = fields(sections("THREE", "THE RESPONSE (200, ")[0])
    reqs = sections("THREE", "THE REQUEST")
    page = sections("THREE", "THE RESPONSE (200):")[0][0].strip().split("  <- ")
    loop = sections("THREE", "THE REQUESTS")[0][0].strip()
    served = sections("THREE", "THE RESPONSES")[0][0].strip()
    header(d, 60, 190, "THE REQUEST  " + reqs[0][0].strip(), AMBER)
    for i, (name, value, mark) in enumerate(leak):
        y = 260 + i * 76
        panel(d, 60, y, 1000, 62, ROSE, 3)
        mono(d, 85, y + 31, name, TEXT, 26, "lm")
        fit_mono(d, 330, y + 31, value, ROSE, 26, 480, "lm")
        label(d, 1040, y + 31, mark.lower(), ROSE, 24, True, "rm")
    header(d, 1120, 190, "THE REQUEST", AMBER)
    fit_mono(d, 1120, 260, reqs[1][0].strip(), TEXT, 26, 740)
    header(d, 1120, 320, "THE RESPONSE", ROSE)
    fit_mono(d, 1120, 390, page[0], ROSE, 28, 740)
    label(d, 1120, 440, page[1].lower(), ROSE, 26, True)
    header(d, 60, 540, "THE REQUESTS", AMBER)
    mono(d, 60, 610, loop, TEXT, 28)
    header(d, 1120, 540, "THE RESPONSES", ROSE)
    fit_mono(d, 1120, 610, served, ROSE, 28, 740)
    caption(d, "Anything stored is shown; any size is served; any rate is answered.", AMBER, 830)


def explicit_models(img, d, cfg):
    """Act four: each key checked against the request model, the five-field view, and the page cap."""
    check = fields(sections("FOUR", "THE CHECK")[0])
    refused = sections("FOUR", "THE RESPONSE:")[0][0].strip()
    view = fields(sections("FOUR", "THE RESPONSE (200, an AccountView")[0])
    cap = sections("FOUR", "THE PAGE SIZE")[0][0].strip()
    named = heading("FOUR", "THE CHECK").split("names ")[1].rstrip("):")
    header(d, 60, 190, "THE CHECK: the model names " + named, CYAN)
    for i, (name, verdict, mark) in enumerate(check):
        y = 260 + i * 76
        ok = not mark
        panel(d, 60, y, 860, 62, GREEN if ok else ROSE, 3)
        mono(d, 85, y + 31, name, TEXT, 26, "lm")
        label(d, 890, y + 31, verdict, GREEN if ok else ROSE, 26, True, "rm")
    header(d, 60, 510, "THE RESPONSE", ROSE)
    mono(d, 60, 580, refused + "; the record is unchanged", ROSE, 28)
    header(d, 1000, 190, "THE RESPONSE: an AccountView", GREEN)
    for i, (name, value, _) in enumerate(view):
        y = 260 + i * 64
        panel(d, 1000, y, 860, 54, GREEN)
        mono(d, 1025, y + 27, name, TEXT, 26, "lm")
        fit_mono(d, 1260, y + 27, value.split("  (")[0], GREEN, 26, 580, "lm")
    header(d, 1000, 600, "THE PAGE SIZE", AMBER)
    mono(d, 1000, 670, cap, AMBER, 30)
    caption(d, "Named fields in, named fields out, and at most %s rows a page." % cap.split()[-1], AMBER, 830)


def token_bucket(img, d, cfg):
    """Act five: the bucket's tokens step by step, the 429, the Retry-After sum, and the refill."""
    budget = [l.strip() for l in sections("FIVE", "THE BUDGET")[0]]
    before = sections("FIVE", "ANA'S TOKENS")[0][0].strip()
    resp = [l.strip() for l in sections("FIVE", "THE RESPONSES")[0]]
    how = [l.strip() for l in sections("FIVE", "HOW Retry-After")[0]]
    later = sections("FIVE", "THE RESPONSE:")[0][0].strip()
    cap = int(re.search(r"holds (\d+)", budget[0]).group(1))
    left = int(before.rsplit("= ", 1)[1])
    header(d, 60, 190, "THE BUDGET", CYAN)
    mono(d, 60, 260, budget[0], TEXT, 26)
    mono(d, 60, 305, budget[1], MUTED, 26)
    # the bucket as a bar: used before the loop, left for the loop
    x0, x1, y = 60, 1860, 370
    d.rounded_rectangle([x0, y, x1, y + 70], radius=12, fill=PANEL, outline=LINE, width=2)
    used_w = int((x1 - x0) * (cap - left) / cap)
    d.rounded_rectangle([x0, y, x0 + used_w, y + 70], radius=12, fill=(60, 50, 30), outline=AMBER, width=2)
    d.rounded_rectangle([x0 + used_w, y, x1, y + 70], radius=12, fill=(20, 60, 50), outline=GREEN, width=2)
    label(d, x0 + used_w // 2, y + 35, "used: %d" % (cap - left), AMBER, 26, True, "mm")
    label(d, (x0 + used_w + x1) // 2, y + 35, "left for the loop: %d" % left, GREEN, 26, True, "mm")
    fit_mono(d, 60, 460, before, AMBER, 26, 1800)
    header(d, 60, 520, "THE RESPONSES", ROSE)
    mono(d, 60, 590, resp[0], GREEN, 26)
    mono(d, 60, 635, resp[1], ROSE, 26)
    header(d, 960, 520, "HOW Retry-After IS WORKED OUT", AMBER)
    fit_mono(d, 960, 590, how[0], TEXT, 24, 900)
    fit_mono(d, 960, 635, how[1], AMBER, 24, 900)
    header(d, 60, 710, "ONE SECOND LATER", GREEN)
    fit_mono(d, 60, 780, later, GREEN, 26, 1800)
    caption(d, "A bucket per caller: a burst, then a steady refill; 429 says when to come back.", AMBER, 860)
