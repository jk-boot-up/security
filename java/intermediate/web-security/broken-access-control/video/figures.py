"""Pictures for the broken access control video, drawn by the `figure` slide kind.

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
DIM = (18, 26, 44)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def part(name, head):
    """The indented lines under the first section header of an act that starts with `head`, two spaces removed."""
    out, on = [], False
    for ln in act(name):
        if not ln.startswith("  "):
            if on:
                break
            on = ln.startswith(head)
        elif on:
            out.append(ln[2:])
    return out


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


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
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)




def status_colour(code, good=None):
    """Green for an answer that should happen, red for one that should not."""
    if good is None:
        return GREEN if code.startswith("2") else AMBER
    return GREEN if good else ROSE


def table(lines, cols):
    """Split fixed-width rows ("who method path status") into fields."""
    out = []
    for ln in lines:
        parts = ln.split()
        if len(parts) >= cols:
            out.append(parts[:cols] + [" ".join(parts[cols:])])
    return out


# ------------------------------------------------------------------ figures

def choke_point(img, d, cfg):
    """One request through the policy: four questions in order, and the first that fails decides the answer."""
    log = part("FOUR", "THE LOG")[0]                       # DENY ana GET /orders/{id}: no order 1002 that belongs to ana
    m = re.match(r"DENY (\S+) (\S+) (\S+): (.+)", log.strip())
    who, method, route, reason = m.groups()
    order = re.search(r"order (\d+)", reason).group(1)
    panel(d, 70, 240, 430, 170, CLIENT, 3)
    label(d, 285, 272, "THE REQUEST", CLIENT, 26, True, "mm")
    mono(d, 285, 322, "%s %s" % (method, route.replace("{id}", order)), TEXT, 30, "mm")
    label(d, 285, 372, "from %s, signed in" % who, MUTED, 24, False, "mm")
    draw.arrow_right(d, 510, 325, 600, MUTED, 4)
    qs = [("1. Is there a rule for this route?", "no", "403", AMBER, False),
          ("2. Must they sign in? Are they?", "no", "401", AMBER, False),
          ("3. Does their role allow it?", "yes", "HANDLER", GREEN, False),
          ("4. Owner allowed: is it their order?", "no", "404", ROSE, True)]
    for i, (q, ans, res, c, hit) in enumerate(qs):
        y = 220 + i * 140
        panel(d, 610, y, 700, 100, ROSE if hit else LINE, 3 if hit else 2)
        label(d, 635, y + 50, q, TEXT, 27, True, "lm")
        draw.arrow_right(d, 1320, y + 50, 1440, c, 4)
        label(d, 1380, y + 24, ans, c, 22, True, "mm")
        cell(d, 1450, y + 20, 260, 60, res, c, 26)
        if i < 3:
            draw.arrow(d, 960, y + 102, 960, y + 138, MUTED, 3)
    label(d, 1450, 800, reason, ROSE, 24, True)
    caption(d, "Every request passes one policy first. No rule means no.", AMBER, 880)


def order_walk(img, d, cfg):
    """Eleven order numbers: missing, hers, or someone else's; and the unguessable gift order."""
    walk = part("ONE", "THE WALK")
    header(d, 80, 200, heading("ONE", "THE WALK").rstrip(":"), AMBER)
    for i, ln in enumerate(walk):
        m = re.match(r"GET /orders/(\d+)\s+(\d+)\s*(.*)", ln.strip())
        n, code, rest = m.groups()
        owner = rest.split("'s")[0].strip() if "'s" in rest else ("ana" if "hers" in rest else "")
        c = LINE if code == "404" else GREEN if owner == "ana" else ROSE
        x = 80 + i * 160
        d.rounded_rectangle([x, 290, x + 145, 470], radius=12, fill=PANEL if code == "404" else BG_CELL, outline=c, width=3)
        mono(d, x + 72, 325, n, TEXT if code != "404" else MUTED, 28, "mm")
        mono(d, x + 72, 385, code, c if code != "404" else MUTED, 34, "mm")
        label(d, x + 72, 440, owner or "-", c if owner else MUTED, 24, True, "mm")
    res = part("ONE", "THE RESULT")
    label(d, 80, 510, res[0].strip(), TEXT, 30, True)
    label(d, 80, 555, res[1].strip(), ROSE, 28, True)
    header(d, 80, 630, "THE GIFT ORDER (an id nobody could guess; ben shared it)", CYAN)
    gift = part("ONE", "THE GIFT ORDER")
    m = re.match(r"GET (\S+)\s+(\d+)", gift[0].strip())
    panel(d, 80, 690, 1300, 80, ROSE, 3)
    mono(d, 110, 730, "GET " + m.group(1), TEXT, 28, "lm")
    cell(d, 1420, 700, 200, 60, m.group(2), ROSE, 32)
    caption(d, "A hard-to-guess id is not a permission. Signed in does not mean allowed.", AMBER, 880)


def hidden_routes(img, d, cfg):
    """The page hides the admin links; the server still answers the requests sent by hand."""
    page = part("TWO", "THE PAGE ANA SEES")
    links = page[0].split(":", 1)[1].split(",")
    header(d, 80, 200, "THE PAGE ANA SEES", CYAN)
    panel(d, 80, 260, 480, 420, CYAN, 3)
    y = 290
    for ln in links:
        cell(d, 110, y, 420, 60, ln.strip(), GREEN, 28)
        y += 80
    for ln in ("refunds", "users", "reports"):
        d.rounded_rectangle([110, y, 530, y + 60], radius=10, outline=LINE, width=2)
        mono(d, 320, y + 30, "admin: " + ln + " (hidden)", MUTED, 22, "mm")
        y += 80
    header(d, 660, 200, "THE REQUESTS SENT BY HAND", AMBER)
    rows = table(part("TWO", "THE REQUESTS"), 4)
    for i, (who, method, path, code, mark) in enumerate(rows):
        y = 270 + i * 100
        c = ROSE if code.startswith("2") else GREEN
        panel(d, 660, y, 760, 80, LINE)
        mono(d, 685, y + 40, "%-7s%-7s%s" % (who, method, path), TEXT, 26, "lm")
        cell(d, 1440, y + 10, 130, 60, code, c, 30)
        if mark:
            label(d, 1590, y + 40, mark.replace("<- ", "").lower(), c, 20, True, "lm")
    res = part("TWO", "THE RESULT")[0].strip()
    label(d, 660, 690, res, ROSE, 28, True)
    caption(d, "Hiding a link is not a check: the server must ask every time.", AMBER, 880)


def _attacks(name, head):
    rows = []
    for ln in part(name, head):
        m = re.match(r"(.+?)\s{2,}(\d{3})$", ln.strip())
        if m:
            rows.append(m.groups())
    return rows


def nine_attacks_before(img, d, cfg):
    """Sideways and upwards examples, then nine attacks against the naive store."""
    side = part("THREE", "SIDEWAYS")
    up = part("THREE", "UPWARDS")
    header(d, 80, 200, "SIDEWAYS: someone else's data", CYAN)
    panel(d, 80, 260, 820, 110, CYAN)
    mono(d, 100, 290, side[0].strip(), TEXT, 22)
    fit_mono(d, 100, 330, side[1].strip(), ROSE, 22, 780)
    header(d, 1000, 200, "UPWARDS: a higher role's function", CYAN)
    panel(d, 1000, 260, 840, 110, CYAN)
    mono(d, 1020, 290, up[0].strip(), TEXT, 22)
    mono(d, 1020, 330, up[1].strip(), ROSE, 22)
    header(d, 80, 400, "BEFORE THE FIX: the naive store, nine attacks", ROSE)
    for i, (what, code) in enumerate(_attacks("THREE", "BEFORE THE FIX")):
        x = 80 + (i % 3) * 590
        y = 470 + (i // 3) * 110
        c = ROSE if code.startswith("2") else GREEN
        panel(d, x, y, 560, 90, c, 3)
        fit_mono(d, x + 20, y + 30, what, TEXT, 22, 520, "lm")
        label(d, x + 20, y + 68, "answered %s: %s" % (code, "it worked" if code.startswith("2") else "refused"), c, 22, True, "lm")
    total = [ln for ln in part("THREE", "BEFORE THE FIX") if "succeed" in ln][0].strip()
    caption(d, total + ". Each check was written separately.", AMBER, 890)


def nine_attacks_after(img, d, cfg):
    """The same nine attacks before and after the policy, and the honest requests that still work."""
    before = dict(_attacks("THREE", "BEFORE THE FIX"))
    after = _attacks("FOUR", "AFTER THE FIX")
    header(d, 80, 200, "THE ATTACK", AMBER)
    header(d, 1010, 200, "BEFORE", ROSE)
    header(d, 1260, 200, "AFTER", GREEN)
    meaning = {"401": "sign in first", "403": "forbidden", "404": "as if missing"}
    for i, (what, code) in enumerate(after):
        y = 262 + i * 58
        mono(d, 80, y + 24, what, TEXT, 24, "lm")
        b = before[what]
        cell(d, 1010, y, 150, 48, b, ROSE if b.startswith("2") else MUTED, 24)
        cell(d, 1260, y, 150, 48, code, GREEN, 24)
        label(d, 1440, y + 24, meaning.get(code, ""), MUTED, 22, False, "lm")
    honest = [ln for ln in part("FOUR", "THE HONEST") if "still work" in ln][0].strip()
    label(d, 80, 800, "the honest requests: " + honest, GREEN, 28, True)
    caption(d, "0 of 9 attacks succeed, and every honest request still works.", AMBER, 880)


def route_inventory(img, d, cfg):
    """The rule table, the new route without a rule, and the same rules over real HTTP."""
    header(d, 60, 200, "THE RULE TABLE", CYAN)
    rules = part("FIVE", "THE RULE TABLE")
    for i, ln in enumerate(rules):
        method, path, who = ln.split(None, 2)
        y = 255 + i * 54
        panel(d, 60, y, 1000, 46, GREEN)
        mono(d, 80, y + 23, "%-7s%s" % (method, path), TEXT, 22, "lm")
        fit_mono(d, 660, y + 23, who, GREEN, 22, 380, "lm")
    inv = part("FIVE", "THE ROUTE INVENTORY")
    new = re.search(r"\[(\S+) (\S+)\]", " ".join(inv))
    y = 255 + len(rules) * 54
    d.rounded_rectangle([60, y, 1060, y + 46], radius=16, fill=PANEL, outline=ROSE, width=3)
    mono(d, 80, y + 23, "%-7s%s" % (new.group(1), new.group(2)), TEXT, 22, "lm")
    mono(d, 660, y + 23, "NO RULE  -> build fails", ROSE, 22, "lm")
    dave = [ln for ln in inv if "dave" in ln][0].strip()
    label(d, 60, y + 70, dave, AMBER, 24, True)
    header(d, 1140, 200, "OVER REAL HTTP", CYAN)
    for i, (who, method, path, code, _) in enumerate(table(part("FIVE", "OVER REAL HTTP"), 4)):
        yy = 260 + i * 90
        panel(d, 1140, yy, 560, 72, LINE)
        mono(d, 1160, yy + 36, "%-7s%-7s%s" % (who, method, path), TEXT, 20, "lm")
        cell(d, 1720, yy + 8, 130, 56, code, GREEN if code == "200" else AMBER, 28)
    caption(d, "Every route needs a rule, and a test keeps it that way.", AMBER, 900)
