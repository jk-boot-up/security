"""Pictures for the Hibernate Validator video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

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


def facts(lines):
    """`key   value` lines as a list of (key, value)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip())]


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


def caption(d, text, colour=AMBER, y=950):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def verdict_colour(text):
    return ROSE if "refused" in text or "<-" in text else GREEN


# ------------------------------------------------------------------ figures

def rows_of(name, head, n=0):
    """`"input"   result` rows of a section, as (input, result)."""
    return [re.match(r'\s*(\S+|".*?")\s+(.*)', ln).groups() for ln in sections(name, head)[n]]


def one_rule(img, d, cfg):
    """The rule on Line.quantity, three inputs, and the result of each."""
    rule = dict(facts(sections("TWO", "THE RULES")[0]))["quantity"]
    rows = rows_of("TWO", "QUANTITY")
    header(d, 60, 190, "THE RULE (on the type)", AMBER, 560)
    panel(d, 60, 260, 560, 300, AMBER, 3)
    fit_mono(d, 90, 300, "record Line(", MUTED, 30, 500)
    fit_mono(d, 120, 360, rule, AMBER, 34, 480)
    fit_mono(d, 120, 420, "int quantity)", TEXT, 30, 480)
    label(d, 90, 600, "read back from the code", MUTED, 26)
    header(d, 700, 190, "THE INPUT", CYAN, 260)
    header(d, 1040, 190, "THE RESULT", GREEN, 820)
    for i, (raw, result) in enumerate(rows):
        y = 270 + i * 150
        cell(d, 700, y, 260, 100, raw, CYAN, 44, TEXT)
        draw.arrow_right(d, 970, y + 50, 1030, MUTED, 4)
        colour = verdict_colour(result)
        cell(d, 1040, y, 820, 100, result, colour, 30)
    caption(d, "Written once, on the record; run at the controller, the service or a test.")


def boundary(img, d, cfg):
    """The request, the shop that trusts the page, and the service whose parameter has constraints."""
    req = dict(facts(sections("ONE", "THE REQUEST")[0]))
    before = dict(facts(sections("ONE", "BEFORE THE FIX")[0]))
    after = sections("ONE", "AFTER THE FIX")[0]
    parts = facts(sections("ONE", "HOW THE PATH")[0])
    cols = [(60, "THE REQUEST (no page)", CYAN), (680, "BEFORE THE FIX", ROSE), (1300, "AFTER THE FIX", GREEN)]
    for x, title, colour in cols:
        header(d, x, 185, title, colour, 560)
        panel(d, x, 245, 560, 360, colour, 3)
    label(d, 90, 275, "cart", MUTED, 26, True)
    fit_mono(d, 90, 315, req["cart"], ROSE, 40, 500)
    label(d, 90, 400, "form fields", MUTED, 26, True)
    for j, f in enumerate(req["fields"].split(", ")):
        fit_mono(d, 90 + (j % 3) * 170, 440 + (j // 3) * 44, f, TEXT, 26, 160)
    label(d, 90, 540, "no price field", AMBER, 26, True)
    label(d, 710, 275, "a shop that trusts the page", MUTED, 26, True)
    total = before["total"].split("  <-")[0]
    fit_mono(d, 710, 340, total, ROSE, 40, 500)
    label(d, 710, 420, "a refund", ROSE, 32, True)
    label(d, 1330, 275, "@NotNull @Valid Checkout", MUTED, 26, True)
    fit_mono(d, 1330, 330, after[0].split(None, 2)[2], GREEN, 26, 500)
    fit_mono(d, 1330, 375, after[1].strip(), GREEN, 26, 500)
    no_form = after[2].split(None, 2)[2]
    fit_mono(d, 1330, 440, "no form: " + no_form.split(": ", 1)[1], GREEN, 26, 500)
    fit_mono(d, 1330, 500, "honest: " + after[3].split(None, 1)[1], GREEN, 26, 500)
    header(d, 60, 650, "HOW THE PATH IS BUILT", AMBER)
    x = 60
    for name, kind in parts:
        if name == "message":
            continue
        w = 420
        panel(d, x, 720, w, 140, AMBER, 3)
        label(d, x + w // 2, 740, name, TEXT, 34, True, "ma")
        fit_mono(d, x + w // 2, 815, kind.replace(" of the list before it", ""), AMBER, 24, w - 20, "mm")
        x += w + 30
    caption(d, "The rules travel with the type: the service refuses what the page never saw.", AMBER, 920)


def rules_results(img, d, cfg):
    """The rule of each field, an input, and its result; ben@localhost passes the wide built-in."""
    rules = dict(facts(sections("TWO", "THE RULES")[0]))
    rows = []
    for head, field in (("SKU", "sku"), ("EMAIL", "email"), ("COUNTRY", "country")):
        rows += [(field, raw, res) for raw, res in rows_of("TWO", head)]
    header(d, 60, 185, "THE RULE", AMBER, 640)
    header(d, 730, 185, "THE INPUT", CYAN, 380)
    header(d, 1140, 185, "THE RESULT", GREEN, 720)
    last = None
    for i, (field, raw, res) in enumerate(rows):
        y = 250 + i * 72
        if field != last:
            fit_mono(d, 60, y + 10, field, MUTED, 24, 640)
            fit_mono(d, 60, y + 40, rules[field], AMBER, 22, 640)
            last = field
        cell(d, 730, y, 380, 58, raw, CYAN, 26, TEXT)
        colour = AMBER if "<-" in res else verdict_colour(res)
        cell(d, 1140, y, 720, 58, res.replace("<- ", ""), colour, 24)
    caption(d, "Amber: the built-in @Email is wider than the shop's rule; add a @Pattern.", AMBER, 935)


def group_order(img, d, cfg):
    """The group sequence, and how far two product codes get, with the catalogue lookups counted."""
    groups = facts(sections("THREE", "HOW THE GROUPS")[0])
    inputs = dict(facts(sections("THREE", "THE INPUT, TWO PRODUCT")[0]))
    results = facts(sections("THREE", "THE RESULT")[-1])
    header(d, 60, 185, "HOW THE GROUPS ORDER THE CHECKS", AMBER)
    for i, (name, what) in enumerate(groups):
        x = 60 + i * 620
        panel(d, x, 250, 560, 130, AMBER, 3)
        label(d, x + 280, 268, name, AMBER, 34, True, "ma")
        fit_mono(d, x + 280, 345, what, TEXT, 24, 530, "mm")
        if i:
            draw.arrow_right(d, x - 55, 315, x - 8, MUTED, 5)
    header(d, 60, 430, "THE INPUT", CYAN)
    header(d, 760, 430, "THE RESULT", GREEN)
    for i, (sku, res) in enumerate(results):
        y = 510 + i * 190
        cell(d, 60, y, 640, 110, sku, CYAN, 44, TEXT)
        label(d, 380, y + 120, inputs[sku], MUTED, 24, True, "ma")
        lookups, message = res.split(":", 1)
        stop = 1 if lookups.startswith("0") else 2
        x0 = 60 + stop * 620
        panel(d, 760, y, 1100, 110, ROSE, 3)
        label(d, 790, y + 18, "stopped in " + groups[stop][0].split()[1] + ", " + lookups.strip(), ROSE, 30, True)
        fit_mono(d, 790, y + 62, message.strip(), TEXT, 26, 1040)
    caption(d, "A wrong shape never costs a catalogue lookup.", AMBER, 935)


def redos_growth(img, d, cfg):
    """Steps of the nested pattern, of the size limit, and of the JDK on (a+)+$, on a log scale."""
    import math
    nested = [(k, v) for k, v in facts(sections("FOUR", "BEFORE THE FIX")[0]) if re.match(r"\d", v)]
    after = facts(sections("FOUR", "AFTER THE FIX")[0])[0]
    simple = [(k, v) for k, v in facts(sections("FOUR", "WHY TWO LEVELS")[0]) if re.match(r"\d", v)]
    bars = [(k + "  ((a+)+)+b", v, ROSE) for k, v in nested]
    bars.append((after[0] + "  @Size first", after[1].rsplit(", ", 1)[1], GREEN))
    bars += [(k + "  (a+)+$", v, AMBER) for k, v in simple]
    top = max(math.log10(int(v.split()[0])) for _, v, _ in bars if int(v.split()[0]) > 0)
    header(d, 60, 185, "THE INPUT: n a's AND A !     THE RESULT: STEPS (log scale)", CYAN)
    for i, (name, value, colour) in enumerate(bars):
        y = 260 + i * 88
        steps = int(value.split()[0])
        label(d, 60, y + 24, name, colour, 28, True, "lm")
        w = 0 if steps == 0 else max(8, int(850 * math.log10(steps) / top))
        if w:
            d.rounded_rectangle([520, y, 520 + w, y + 48], radius=8, fill=colour)
        fit_mono(d, 540 + w, y + 24, value, colour, 28, 1820 - 540 - w, "lm")
    caption(d, "Rose: before the fix. Green: a size limit first, 0 steps. Amber: the JDK's simpler case.", AMBER, 935)


def template_injection(img, d, cfg):
    """A visitor's coupon inside a message template: evaluated before the fix, kept as text after."""
    coupons = [ln.strip() for ln in sections("FIVE", "THE INPUT, A COUPON")[0]]
    before = [re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in sections("FIVE", "BEFORE THE FIX")[0]
              if ln.strip().startswith("$")]
    after = facts(sections("FIVE", "AFTER THE FIX")[0])
    header(d, 60, 185, "THE INPUT: A COUPON, PUT INTO A TEMPLATE", CYAN)
    for i, c in enumerate(coupons):
        cell(d, 60 + i * 620, 250, 580, 80, c, CYAN, 34, TEXT)
    fit_mono(d, 60, 370, '"the coupon " + value + " does not exist"', MUTED, 28, 1800)
    header(d, 60, 440, "BEFORE THE FIX: templates may call methods", ROSE)
    for i, (c, msg) in enumerate(before):
        y = 510 + i * 80
        fit_mono(d, 60, y + 30, c, TEXT, 30, 520, "lm")
        draw.arrow_right(d, 600, y + 30, 680, MUTED, 4)
        cell(d, 700, y, 1160, 64, msg, ROSE, 30)
    header(d, 60, 690, "AFTER THE FIX", GREEN)
    for i, (how, msg) in enumerate(after):
        y = 760 + i * 80
        label(d, 60, y + 30, how, GREEN, 30, True, "lm")
        cell(d, 700, y, 1160, 64, msg, GREEN, 30)
    caption(d, "Never build a template from the visitor's text: pass it as a parameter.", AMBER, 950)
