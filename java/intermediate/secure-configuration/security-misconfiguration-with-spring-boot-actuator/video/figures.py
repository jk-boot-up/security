"""Pictures for the Spring Boot Actuator misconfiguration video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B,
                                   TEXT, W, font)

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


def section(name, head, i=0):
    return sections(name, head)[i]


def joined(lines):
    """Lines of one section with continuation lines (deeper indent) joined to the line before."""
    out = []
    for ln in lines:
        if ln.startswith("  ") and out:
            out[-1] += " " + ln.strip()
        else:
            out.append(ln.strip())
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
    while f.getlength(text) > max_w and f.size > 16:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def marked(text):
    body, _, mark = text.partition("  <- ")
    return body.strip(), mark.strip()


def checks(name):
    return [(ln[:4], ln[6:26].strip(), ln[26:].strip()) for ln in section(name, "THE 8 CHECKS")]


# ------------------------------------------------------------------ figures

def one_port(img, d, cfg):
    """The shipped store: one port for customers and operations, and what a stranger reads through it."""
    shared = section("ONE", "THE PORTS")[0].strip()
    replies = joined(section("ONE", "THE REPLIES"))
    key = marked(replies[1])[0].split("  ", 1)[1]
    count = replies[0].split("  ", 1)[1].split(",")[0]
    label(d, 160, 230, "a customer", TEXT, 30, True, "ma")
    label(d, 160, 560, "a stranger", ROSE, 30, True, "ma")
    draw.arrow_right(d, 260, 250, 600, GREEN, 4)
    draw.arrow_right(d, 260, 580, 600, ROSE, 4)
    panel(d, 620, 200, 640, 520, AMBER, 3)
    label(d, 940, 220, "ONE PORT", AMBER, 30, True, "ma")
    cell(d, 660, 280, 560, 70, "GET /products", GREEN, 28, TEXT)
    cell(d, 660, 380, 560, 70, "GET /actuator  (%s)" % count, ROSE, 28, TEXT)
    cell(d, 660, 480, 560, 70, "GET /actuator/env/...", ROSE, 28, TEXT)
    cell(d, 660, 580, 560, 70, "GET /actuator/heapdump", ROSE, 28, TEXT)
    draw.arrow_right(d, 1270, 515, 1340, MUTED, 4)
    header(d, 1350, 440, "THE REPLY", ROSE, 510)
    panel(d, 1350, 500, 510, 100, ROSE, 3)
    fit_mono(d, 1370, 550, key, TEXT, 26, 470, "lm")
    label(d, 940, 760, shared, AMBER, 28, True, "ma")
    caption(d, "Actuator was built to be looked into: whoever reaches it, sees inside.", AMBER, 880)


def stranger_requests(img, d, cfg):
    """The seven requests a stranger sends to the shipped store, and the replies."""
    reqs = [ln.strip() for ln in section("ONE", "THE REQUESTS")]
    reps = joined(section("ONE", "THE REPLIES"))
    header(d, 60, 200, "THE REQUEST (no login)", AMBER, 760)
    header(d, 860, 200, "THE REPLY", ROSE, 1000)
    for i, (req, rep) in enumerate(zip(reqs, reps)):
        y = 262 + i * 88
        cell(d, 60, y, 760, 70, req, AMBER, 26, TEXT)
        draw.arrow_right(d, 826, y + 35, 856, MUTED, 3)
        body, mark = marked(rep)
        status, _, shown = body.partition("  ")
        cell(d, 860, y, 110, 70, status, ROSE, 30)
        panel(d, 990, y, 870, 70, ROSE if mark else LINE, 3 if mark else 2)
        fit_mono(d, 1010, y + 35, shown.strip() + ("   <- " + mark.lower() if mark else ""), TEXT, 24, 830, "lm")
    score = marked(section("ONE", "THE SCORE")[0])[0]
    caption(d, "No login, no attack: the checker scores the shipped store %s." % score, AMBER, 900)


def boot_defaults(img, d, cfg):
    """What Spring Boot does with no settings, with include=*, and with one more line."""
    bare = joined(section("TWO", "THE REPLIES", 0))
    masked = section("TWO", "THE REPLIES", 1)
    more = section("TWO", "THE REPLIES", 2)
    cols = (("NO SETTINGS", GREEN), ("include=*", AMBER), ("+ heapdump unrestricted", ROSE))
    for j, (t, c) in enumerate(cols):
        header(d, 60 + j * 610, 200, t, c, 580)
    rows = [
        ("the list", bare[0].split("->")[1].strip(), "all endpoints", "all endpoints"),
        ("env value", bare[1].split("->")[1].strip(), masked[0].split("->")[1].strip(), more[0].split("->")[1].strip()),
        ("route map", "not exposed", marked(masked[1].split("->")[1])[0], marked(masked[1].split("->")[1])[0]),
        ("heap dump", "not exposed", masked[2].split("->")[1].strip(), more[1].split("->")[1].strip()),
    ]
    bad = {(2, 1), (2, 2), (3, 2)}
    for i, row in enumerate(rows):
        y = 280 + i * 140
        for j in range(3):
            x = 60 + j * 610
            label(d, x + 10, y, row[0], MUTED, 24, True)
            c = ROSE if (i, j) in bad else GREEN
            cell(d, x, y + 36, 580, 76, row[j + 1], c, 26, TEXT)
    caption(d, act("TWO")[-1].strip().capitalize() + "; names and routes stay open.", AMBER, 880)


def hardened_layers(img, d, cfg):
    """The hardened store's two ports, who gets what on each, and the score before and after."""
    rows = joined(section("THREE", "THE REQUESTS AND REPLIES"))
    header(d, 60, 200, "PORT  LOGIN  REQUEST", CYAN, 1000)
    header(d, 1080, 200, "STATUS", AMBER, 200)
    for i, r in enumerate(rows):
        y = 262 + i * 70
        left, _, right = r.partition("->")
        status = right.strip().split(" ")[0]
        port, login, req = left.split(None, 2)
        c = GREEN if status == "200" else CYAN          # every reply here is the one the rules intend
        fit_mono(d, 70, y + 28, port, MUTED, 26, 220, "lm")
        fit_mono(d, 300, y + 28, login, MUTED, 26, 80, "lm")
        fit_mono(d, 400, y + 28, req.strip(), TEXT, 26, 660, "lm")
        cell(d, 1080, y, 200, 56, status, c, 28)
    header(d, 1340, 200, "SCORE", CYAN, 520)
    for k, (name, c) in enumerate((("ONE", ROSE), ("THREE", GREEN))):
        score = marked(section(name, "THE SCORE")[0])[0]
        label(d, 1360, 290 + k * 150, ("shipped" if k == 0 else "hardened"), MUTED, 28, True)
        cell(d, 1360, 330 + k * 150, 480, 80, score, c, 40)
    rules = [r.strip() for ln in section("THREE", "THE RULES") for r in ln.split(";")]
    label(d, 1360, 600, "the rules, first match wins", MUTED, 26, True)
    for k, ln in enumerate(rules):
        fit_mono(d, 1360, 660 + k * 48, ln, TEXT, 26, 520, "lm")
    caption(d, "Another port, a short list, and rules: each layer refuses on its own.", AMBER, 900)


def who_may(img, d, cfg):
    """Who may read the loggers, who sees health details, and what a write by an operator gets."""
    logs = joined(section("FOUR", "THE REPLIES", 0))
    health = joined(section("FOUR", "THE REPLIES", 1))
    post = section("FOUR", "THE REPLY:")[0].strip()
    header(d, 60, 200, "LOGIN", CYAN, 360)
    header(d, 440, 200, "GET .../loggers/ROOT", AMBER, 560)
    header(d, 1020, 200, "GET /actuator/health", AMBER, 840)
    stranger = joined(section("THREE", "THE REQUESTS AND REPLIES"))[3].split()[-1]
    who = [("no login", logs[0].split()[-1], stranger)]
    who.append(("customer ana", logs[1].split()[-1], health[0].split("components:")[1].strip()))
    ops = logs[2].split(None, 3)
    who.append(("operator ops", ops[2], health[1].split("components:")[1].strip()))
    for i, (w, status, comps) in enumerate(who):
        y = 270 + i * 120
        label(d, 70, y + 38, w, TEXT, 30, True, "lm")
        cell(d, 440, y, 560, 76, status, GREEN if status == "200" else CYAN, 30, TEXT)
        cell(d, 1020, y, 840, 76, comps, CYAN if i == 2 else GREEN, 24, TEXT)
    header(d, 60, 660, "THE OPERATOR WRITES: POST .../loggers/ROOT {DEBUG}", ROSE)
    cell(d, 60, 720, 1800, 80, post, GREEN, 30, TEXT)
    caption(d, "Reading is not changing: even the operator cannot write.", AMBER, 880)


def one_line(img, d, cfg):
    """One added line costs one check; the four layers, and which one held."""
    change = section("FIVE", "THE CHANGE")[0].strip()
    checker = section("FIVE", "THE CHECKER")
    held = section("FIVE", "THE LAYER THAT HELD")[0].strip()
    header(d, 60, 200, "THE CHANGE", AMBER)
    cell(d, 60, 260, 900, 70, change, AMBER, 28, TEXT)
    draw.arrow_right(d, 970, 295, 1040, MUTED, 4)
    cell(d, 1050, 260, 300, 70, checker[0].strip(), ROSE, 34)
    fit_mono(d, 60, 380, checker[1].strip(), ROSE, 26, 1800, "lm")
    layers = (("port", "operations apart", GREEN), ("list", "include=* again", ROSE),
              ("rules", "login for the rest", GREEN), ("access", "read-only loggers", GREEN))
    for i, (name, sub, c) in enumerate(layers):
        draw.box(d, 60 + i * 460, 470, 400, 180, name, sub, c, 36, 26)
    label(d, W // 2, 690, "the layer that held: " + held, GREEN, 28, True, "ma")
    caption(d, section("FIVE", "THE BILL")[-1].strip(), AMBER, 880)
