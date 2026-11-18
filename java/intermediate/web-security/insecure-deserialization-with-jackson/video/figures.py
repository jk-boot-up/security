"""Pictures for the insecure deserialization with Jackson video, drawn by the `figure` slide kind.

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

def joined(lines):
    """JSON printed over several lines, joined back into one string."""
    return "".join(ln.strip() for ln in lines)


def json_rows(d, x, y, lines, colour=TEXT, size=26, mark=None, mark_colour=AMBER, step=40):
    """Draws JSON lines as printed; a line containing `mark` is drawn in `mark_colour`."""
    for j, ln in enumerate(lines):
        c = mark_colour if mark and any(m in ln for m in ((mark,) if isinstance(mark, str) else mark)) else colour
        mono(d, x, y + j * step, ln.rstrip(), c, size)
    return y + len(lines) * step


def results(lines):
    """'  -> refused (x)' lines, without the arrows."""
    return [ln.replace("->", "").strip() for ln in lines if "->" in ln]


def json_names_class(img, d, cfg):
    """The core idea: a field typed by class name lets the JSON choose which class Jackson builds."""
    honest = first("ONE", "THE COOKIE'S JSON")
    forged = first("TWO", "THE FORGED NOTE'S JSON")
    honest_cls = re.search(r'"@class":"([^"]+)"', joined(honest)).group(1)
    forged_cls = re.search(r'"@class":"([^"]+)"', joined(forged)).group(1)
    res = results(sections("TWO", "THE RESULT")[1])
    rows = [("AN HONEST NOTE", GREEN, honest[-3:], honest_cls, [first("ONE", "THE CART")[-1].strip()]),
            ("A FORGED NOTE", ROSE, forged, forged_cls, [r.split("  <-")[0] for r in res])]
    mark = ('"@class"', "jacksondeser.")
    for i, (title, c, js, cls, built) in enumerate(rows):
        y = 200 + i * 340
        header(d, 60, y, title + ": THE COOKIE'S JSON", c)
        panel(d, 60, y + 60, 1100, 40 + 46 * len(js), c, 3)
        json_rows(d, 85, y + 80, js, TEXT, 28, mark, AMBER, 46)
        draw.arrow_right(d, 1170, y + 130, 1230, MUTED, 4)
        header(d, 1240, y, "JACKSON BUILDS", c)
        cell(d, 1240, y + 60, 620, 64, cls.rsplit(".", 1)[1], c, 30)
        for k, b in enumerate(built):
            fit_mono(d, 1240, y + 160 + k * 40, b, c, 24, 620, "lm")
    caption(d, "The value of @class is a class name the cookie chose; Jackson builds it.", AMBER, 900)


def cart_json(img, d, cfg):
    """Act one: the cart, its JSON with the note's class name, the Base64 start, and the total worked out."""
    rows = cart_rows(first("ONE", "THE CART")[:-1])
    header(d, 60, 200, "BEFORE: THE CART", CYAN)
    for i, (sku, q, p) in enumerate(rows):
        y = 270 + i * 76
        panel(d, 60, y, 520, 62, CYAN)
        mono(d, 80, y + 31, sku, TEXT, 28, "lm")
        mono(d, 330, y + 31, "x" + q, TEXT, 28, "mm")
        mono(d, 550, y + 31, p, TEXT, 28, "rm")
    label(d, 60, 435, first("ONE", "THE CART")[-1].strip(), MUTED, 26)
    res = first("ONE", "THE RESULT")
    header(d, 60, 520, "THE RESULT, READ BACK", GREEN)
    mono(d, 60, 590, res[0].strip(), GREEN, 28)
    mono(d, 60, 640, res[1].strip(), TEXT, 28)
    header(d, 680, 200, "AFTER: THE COOKIE'S JSON", AMBER)
    js = first("ONE", "THE COOKIE'S JSON")
    panel(d, 680, 260, 1180, 40 + 44 * len(js), AMBER)
    json_rows(d, 700, 280, js, TEXT, 24, ('"@class"', "jacksondeser."), AMBER, 44)
    b64 = first("ONE", "THE COOKIE (")
    header(d, 680, 520, "THE COOKIE (Base64 of the JSON)", CYAN)
    mono(d, 680, 590, b64[0].strip(), CYAN, 26)
    mono(d, 680, 640, b64[1].strip(), MUTED, 26)
    caption(d, "The prices are in the cookie, and the note names its own class.", AMBER, 900)


def class_name_trap(img, d, cfg):
    """Act two: a forged price, then which note types let a cookie's class name through."""
    forged = joined(first("TWO", "THE FORGED CART'S JSON"))
    price = re.search(r'"price":(\d+)', forged).group(1)
    cheap = first("TWO", "THE RESULT")
    header(d, 60, 200, "A FORGED PRICE", ROSE)
    mono(d, 60, 270, '"price":' + price, ROSE, 30)
    mono(d, 60, 320, cheap[0].split("  <-")[0].strip(), ROSE, 26)
    mono(d, 60, 365, cheap[1].strip(), TEXT, 26)
    steps = [ln.strip() for ln in first("TWO", "WHAT JACKSON DOES")]
    header(d, 60, 440, "A FORGED NOTE: WHAT JACKSON DOES", ROSE)
    for i, s in enumerate(steps):
        fit_mono(d, 60, 500 + i * 50, s, AMBER if i < 2 else ROSE, 24, 760)
    gone = results(sections("TWO", "THE RESULT")[1])
    mono(d, 60, 670, gone[1].split("  <-")[0], ROSE, 24)
    other = first("TWO", "OTHER TYPES")
    rows = [("the note field, typed Note", " / ".join(results(sections("TWO", "THE RESULT")[1])).split("  <-")[0])]
    i = 0
    while i < len(other):
        if "->" not in other[i]:
            res = []
            j = i + 1
            while j < len(other) and "->" in other[j]:
                res.append(other[j].replace("->", "").strip().split("  <-")[0])
                j += 1
            rows.append((other[i].strip(), " / ".join(res)))
            i = j
        else:
            i += 1
    header(d, 900, 200, "THE TYPE OF THE NOTE -> THE FORGED NOTE", AMBER)
    for k, (what, res) in enumerate(rows):
        y = 270 + k * 175
        bad = "false" in res
        c = ROSE if bad else GREEN
        panel(d, 900, y, 960, 145, c, 3)
        label(d, 925, y + 20, what, TEXT, 26, True)
        fit_mono(d, 925, y + 95, res, c, 24, 910, "lm")
    caption(d, "Jackson 3 refuses an Object base type; any other interface lets the cookie choose.", AMBER, 900)


def kinds_or_classes(img, d, cfg):
    """Act three: short names from a list, or a validator that lists classes; both stop the gadget, neither stops the price."""
    kinds = first("THREE", "THE LIST OF KINDS")[0].strip()
    honest = joined(first("THREE", "THE HONEST NOTE'S JSON")[:-1])
    forged = first("THREE", "THE FORGED NOTE'S JSON")
    forged_js = [ln for ln in forged if "->" not in ln]
    header(d, 60, 200, "A LIST OF KINDS", GREEN)
    mono(d, 60, 265, kinds, GREEN, 26)
    label(d, 60, 320, "the honest note:", MUTED, 24)
    fit_mono(d, 60, 360, honest, TEXT, 24, 860)
    label(d, 60, 420, "the forged note:", MUTED, 24)
    json_rows(d, 60, 460, forged_js, ROSE, 24, None, ROSE, 40)
    for k, r in enumerate(results(forged)):
        mono(d, 60, 600 + k * 45, r, GREEN, 26)
    classes = first("THREE", "THE LIST OF CLASSES")[0].strip()
    header(d, 1000, 200, "A LIST OF CLASSES (a validator)", GREEN)
    mono(d, 1000, 265, classes, GREEN, 26)
    ins = first("THREE", "THE INPUTS")
    y = 330
    for ln in ins:
        if "->" in ln:
            r = ln.replace("->", "").strip()
            c = AMBER if "STILL" in r else GREEN
            mono(d, 1030, y, r.split("  <-")[0], c, 24)
            if "<-" in r:
                label(d, 1030, y + 38, r.split("<- ")[1].lower(), AMBER, 24, True)
                y += 38
            y += 44
        else:
            label(d, 1000, y, ln.strip(), TEXT, 24, True)
            y += 42
    caption(d, "Both lists stop the gadget; the price is still the cookie's to say.", AMBER, 900)


def priced_by_shop(img, d, cfg):
    """Act four: a request of codes and quantities, priced from the catalogue; forged requests refused."""
    req = first("FOUR", "THE REQUEST'S JSON")
    header(d, 60, 200, "BEFORE: THE REQUEST'S JSON", CYAN)
    json_rows(d, 60, 265, req, TEXT, 26, None, TEXT, 42)
    res = first("FOUR", "THE RESULT")
    header(d, 60, 380, "AFTER: PRICED BY THE SHOP'S CATALOGUE", GREEN)
    for i, ln in enumerate(res):
        mono(d, 60, 445 + i * 48, ln.strip(), GREEN if "total" in ln else TEXT, 28)
    forged = first("FOUR", "THE FORGED REQUESTS")
    header(d, 980, 200, "FORGED REQUESTS -> THE RESULT", ROSE)
    for i in range(0, len(forged), 2):
        y = 265 + (i // 2) * 135
        panel(d, 980, y, 880, 115, ROSE)
        fit_mono(d, 1000, y + 35, forged[i].strip(), TEXT, 24, 840, "lm")
        mono(d, 1000, y + 80, forged[i + 1].replace("->", "").strip(), GREEN, 24, "lm")
    caption(d, "No price and no class name may be sent; the shop does the sums.", AMBER, 900)


def stream_limits(img, d, cfg):
    """Act five: the stream limits, default and tightened, and what each refuses."""
    limits = [re.match(r"\s*(.+?)\s{2,}([\d,]+)\s+->\s+(\d+)", ln).groups() for ln in first("FIVE", "THE LIMITS")]
    header(d, 60, 200, "THE LIMITS", CYAN)
    for j, h in enumerate(("", "default", "tightened")):
        label(d, (60, 560, 760)[j], 270, h, MUTED, 24, True, "ra" if j else "la")
    for i, (what, dflt, tight) in enumerate(limits):
        y = 310 + i * 80
        panel(d, 60, y, 740, 64, CYAN)
        label(d, 80, y + 32, what, TEXT, 26, True, "lm")
        mono(d, 560, y + 32, dflt, MUTED, 26, "rm")
        mono(d, 760, y + 32, tight, GREEN, 28, "rm")
    ins = first("FIVE", "THE INPUTS")
    header(d, 900, 200, "THE INPUTS -> THE RESULTS", AMBER)
    for i in range(0, len(ins), 2):
        y = 270 + (i // 2) * 135
        panel(d, 900, y, 960, 115, GREEN)
        fit_mono(d, 920, y + 35, ins[i].strip(), TEXT, 24, 920, "lm")
        mono(d, 920, y + 80, ins[i + 1].replace("->", "").strip(), GREEN, 24, "lm")
    caption(d, "Limits protect memory and the stack, whatever the types.", AMBER, 900)
