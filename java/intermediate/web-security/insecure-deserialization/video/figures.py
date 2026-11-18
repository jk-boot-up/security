"""Pictures for the insecure deserialization video, drawn by the `figure` slide kind.

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
    """Every section of an act whose header starts with `head`, as lists of lines (two more spaces removed)."""
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


def first(name, head):
    return sections(name, head)[0]


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
    while f.getlength(text) > max_w and f.size > 16:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def cart_rows(lines):
    """'KTL-001  x2  2500' -> (sku, quantity, price)."""
    return [re.match(r"(\S+)\s+x(\d+)\s+(\d+)", ln.strip()).groups() for ln in lines]


# ------------------------------------------------------------------ figures

def bytes_choose_classes(img, d, cfg):
    """The core idea: the cookie's bytes name the classes, and the server builds whatever they name."""
    cookie = first("ONE", "THE COOKIE")[0].split("...")[0]
    honest = first("ONE", "THE CLASSES")[0]
    gadget = first("TWO", "THE CLASSES")[0]
    after = sections("TWO", "THE RESULT")[1][1].split("  <-")[0]
    cols = [(60, "THE COOKIE", CYAN), (700, "THE CLASSES IT NAMES", AMBER), (1300, "WHAT THE SERVER BUILDS", GREEN)]
    for x, t, c in cols:
        header(d, x, 200, t, c)
    rows = [("an honest cart", cookie + "...", honest.split(", "), GREEN, "a cart: " + first("ONE", "THE RESULT")[0]),
            ("a forged cookie", cookie[:8] + "...  (written by the customer)", gadget.split(", "), ROSE,
             "TempCleaner.readObject runs: " + after.replace("marker file exists afterwards: false", "the file is gone"))]
    for i, (who, text, classes, c, built) in enumerate(rows):
        y = 290 + i * 300
        label(d, 60, y, who, c, 26, True)
        panel(d, 60, y + 44, 580, 180, c, 3)
        fit_mono(d, 80, y + 90, text.split("  (")[0], TEXT, 26, 540)
        if "(" in text:
            label(d, 80, y + 150, "(" + text.split("  (")[1], MUTED, 24)
        else:
            label(d, 80, y + 150, "Base64 of Java's serialized bytes", MUTED, 24)
        draw.arrow_right(d, 645, y + 134, 695, MUTED, 4)
        for j, name in enumerate(classes):
            cell(d, 700 + (j % 2) * 280, y + 44 + (j // 2) * 92, 260, 76, name, AMBER, 26)
        draw.arrow_right(d, 1245, y + 134, 1295, MUTED, 4)
        panel(d, 1300, y + 44, 560, 180, c, 3)
        words, line, ly = built.split(), "", y + 70
        for w_ in words:
            if font(SANS_B, 26).getlength(line + " " + w_) > 520:
                label(d, 1320, ly, line.strip(), c, 26, True)
                line, ly = "", ly + 40
            line += " " + w_
        label(d, 1320, ly, line.strip(), c, 26, True)
    caption(d, "The bytes choose the classes; the server builds them before it can look.", AMBER, 900)


def cart_cookie(img, d, cfg):
    """Act one: the cart, the cookie it becomes, what its first bytes mean, and the total worked out."""
    rows = cart_rows(first("ONE", "THE CART"))
    header(d, 60, 200, "BEFORE: THE CART", CYAN)
    for j, h in enumerate(("code", "qty", "price (cents)")):
        label(d, (80, 330, 470)[j], 270, h, MUTED, 24, True)
    for i, (sku, q, p) in enumerate(rows):
        y = 310 + i * 80
        panel(d, 60, y, 640, 64, CYAN)
        mono(d, 80, y + 32, sku, TEXT, 28, "lm")
        mono(d, 360, y + 32, "x" + q, TEXT, 28, "mm")
        mono(d, 660, y + 32, p, TEXT, 28, "rm")
    res = first("ONE", "THE RESULT")
    header(d, 60, 520, "THE RESULT, READ BACK", GREEN)
    mono(d, 60, 590, res[0], GREEN, 28)
    mono(d, 60, 640, res[1], TEXT, 28)
    cookie = first("ONE", "THE COOKIE")[0]
    header(d, 800, 200, "AFTER: THE COOKIE (Base64)", AMBER)
    panel(d, 800, 260, 1060, 110, AMBER)
    mono(d, 825, 285, cookie.split("  (")[0], AMBER, 28)
    label(d, 825, 330, cookie.split("  (")[1].rstrip(")"), MUTED, 24)
    hexes = first("ONE", "HOW IT STARTS")[0].split("  ->")[0].split()
    header(d, 800, 410, "ITS FIRST SIX BYTES (hex)", CYAN)
    meaning = ["magic", "magic", "version", "version", "object", "class"]
    for j, (hx, m) in enumerate(zip(hexes, meaning)):
        x = 800 + j * 175
        cell(d, x, 470, 155, 70, hx, CYAN, 32)
        label(d, x + 77, 560, m, MUTED, 24, False, "ma")
    label(d, 800, 620, first("ONE", "HOW IT STARTS")[0].split("-> ")[1] + " in Base64:", TEXT, 26, True)
    label(d, 800, 660, "every serialized Java cookie starts with rO0", MUTED, 26)
    caption(d, "The cookie is the whole object, with each line's price inside it.", AMBER, 900)


def gadget_runs(img, d, cfg):
    """Act two: a forged price is believed, and a TempCleaner deletes a file before the type check."""
    forged = cart_rows(first("TWO", "THE FORGED CART"))[0]
    res = first("TWO", "THE RESULT")
    header(d, 60, 200, "A FORGED PRICE", ROSE)
    panel(d, 60, 260, 640, 64, ROSE)
    mono(d, 80, 292, forged[0], TEXT, 28, "lm")
    mono(d, 360, 292, "x" + forged[1], TEXT, 28, "mm")
    mono(d, 660, 292, forged[2], ROSE, 28, "rm")
    label(d, 60, 350, "the price per unit, in cents", MUTED, 24)
    header(d, 60, 420, "THE RESULT", AMBER)
    mono(d, 60, 490, res[0].split("  <-")[0], ROSE, 28)
    mono(d, 60, 540, res[1], TEXT, 28)
    label(d, 60, 600, "the server believes the cookie", ROSE, 26, True)

    gadget = first("TWO", "THE CLASSES")[0]
    before = first("TWO", "THE MARKER FILE BEFORE")[0]
    after = sections("TWO", "THE RESULT")[1]
    header(d, 800, 200, "A COOKIE THAT HOLDS A " + gadget.upper(), ROSE)
    steps = [("marker file before:  " + before, GREEN),
             ("1. readObject() builds a " + gadget, AMBER),
             ("2. " + gadget + ".readObject() deletes the file", ROSE),
             ("3. the server checks the type: " + after[0], AMBER),
             (after[1].split("  <-")[0], ROSE)]
    for i, (t, c) in enumerate(steps):
        y = 270 + i * 105
        panel(d, 800, y, 1060, 80, c, 3)
        fit_mono(d, 825, y + 40, t, c, 26, 1010, "lm")
        if i < len(steps) - 1:
            draw.arrow(d, 1330, y + 82, 1330, y + 103, MUTED, 3)
    caption(d, "Step two runs inside readObject, before step three can refuse it.", AMBER, 900)


def filter_checks(img, d, cfg):
    """Act three: the filter's answer for every class, before it is built; the forged price passes."""
    allow = [ln.strip() for ln in first("THREE", "THE ALLOW-LIST")]
    header(d, 60, 200, "THE ALLOW-LIST", CYAN)
    panel(d, 60, 260, 1800, 110, CYAN)
    mono(d, 85, 280, allow[0] + " " + allow[1], TEXT, 26)
    mono(d, 85, 325, allow[2] + " " + allow[3].split("  (")[0], MUTED, 26)
    cols = [(60, "THE HONEST CART", GREEN, first("THREE", "THE HONEST CART")),
            (680, "THE TEMPCLEANER COOKIE", ROSE, first("THREE", "THE TEMPCLEANER")),
            (1300, "THE FORGED PRICE", AMBER, first("THREE", "THE FORGED PRICE"))]
    for x, t, c, lines in cols:
        header(d, x, 410, t, c)
    for i, ln in enumerate(l for l in cols[0][3] if not l.startswith("->")):
        name, status = [s.strip() for s in ln.split("->")]
        cell(d, 60, 470 + i * 70, 260, 58, name, TEXT, 24)
        cell(d, 340, 470 + i * 70, 220, 58, status, GREEN, 24)
    mono(d, 60, 770, cols[0][3][-1].replace("->", "").strip(), GREEN, 24)
    name, status = [s.strip() for s in cols[1][3][0].split("->")]
    cell(d, 680, 470, 280, 58, name, TEXT, 24)
    cell(d, 980, 470, 240, 58, status, ROSE, 24)
    res = first("THREE", "THE RESULT")
    label(d, 680, 560, "never built, so", MUTED, 24)
    mono(d, 680, 600, res[1].replace("marker file exists afterwards", "file exists"), GREEN, 26)
    names, verdict = cols[2][3][0].split(" -> ")
    for j, n in enumerate(names.split(", ")):
        cell(d, 1300 + (j % 2) * 280, 470 + (j // 2) * 70, 260, 58, n, TEXT, 24)
    label(d, 1300, 625, verdict, GREEN, 26, True)
    lie = cols[2][3][1].replace("->", "").split("  <-")
    mono(d, 1300, 680, lie[0].strip(), ROSE, 26)
    label(d, 1300, 725, lie[1].strip().lower(), ROSE, 26, True)
    caption(d, "The filter stops unknown classes before they are built; it cannot see a lie.", AMBER, 900)


def signed_cookie(img, d, cfg):
    """Act four: text, its Base64 body, the HMAC, and a changed quantity that fails the check."""
    text = first("FOUR", "THE COOKIE TEXT")[0]
    body = first("FOUR", "THE BODY")[0]
    sig = first("FOUR", "THE SIGNATURE")[0]
    header(d, 60, 200, "BEFORE SIGNING: THE TEXT", CYAN)
    cell(d, 60, 260, 620, 70, text, CYAN, 28)
    draw.arrow_right(d, 690, 295, 760, MUTED, 4)
    header(d, 770, 200, "THE BODY (Base64)", CYAN)
    cell(d, 770, 260, 620, 70, body, TEXT, 26)
    header(d, 60, 370, "AFTER SIGNING: HMAC-SHA-256(server's key, body)", GREEN)
    cell(d, 60, 430, 900, 70, sig, GREEN, 28)
    label(d, 990, 465, "cookie = body . signature", MUTED, 26, False, "lm")
    changed = sections("FOUR", "THE QUANTITY CHANGED")[0]
    new_text = changed[0].split("  (")[0]
    server, given = changed[2].strip(), changed[4].strip()
    header(d, 60, 550, "THE CUSTOMER CHANGES x2 TO x9", ROSE)
    cell(d, 60, 610, 620, 70, new_text, ROSE, 28)
    label(d, 720, 600, "the server's HMAC of the new body", MUTED, 24)
    mono(d, 720, 635, server, AMBER, 26)
    label(d, 720, 690, "the signature in the cookie", MUTED, 24)
    mono(d, 720, 725, given, TEXT, 26)
    verdict = changed[5].replace("->", "").strip()
    label(d, 720, 785, "they differ, so: " + verdict, ROSE, 28, True)
    caption(d, "Prices come from the server; a changed byte changes the HMAC.", AMBER, 900)
