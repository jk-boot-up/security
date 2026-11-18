"""Pictures for the input validation video, drawn by the `figure` slide kind.

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

def quantity_steps(img, d, cfg):
    """How the server reads quantity "-3" and quantity "2", one check at a time."""
    heads = [h for h in act("ONE") if h.startswith("HOW THE SERVER READS")]
    reads = sections("ONE", "HOW THE SERVER READS")
    honest = dict(facts(sections("ONE", "AFTER THE FIX")[0]))["honest"]
    for i, (head, lines) in enumerate(zip(heads, reads)):
        y = 190 + i * 370
        raw = re.search(r'"(.*)"', head).group(1)
        header(d, 60, y, "THE INPUT", CYAN)
        cell(d, 60, y + 75, 260, 110, 'quantity "%s"' % raw, CYAN, 30)
        x = 380
        steps = facts(lines)
        for k, (name, result) in enumerate(steps):
            colour = ROSE if result.endswith("refused") else GREEN if name != "value" else CYAN
            draw.arrow_right(d, x - 50, y + 115, x - 8, MUTED, 4)
            w = 310
            panel(d, x, y + 60, w, 140, colour, 3)
            label(d, x + w // 2, y + 74, name, colour, 30, True, "ma")
            text = result.replace(": ok", "").replace(": refused", "")
            fit_mono(d, x + w // 2, y + 150, text, TEXT, 26, w - 20, "mm")
            label(d, x + w // 2, y + 212, "refused" if result.endswith("refused") else "ok" if name != "value" else "",
                  colour, 28, True, "ma")
            x += w + 55
        header(d, 60, y + 260, "THE RESULT", ROSE if steps[-1][1].endswith("refused") else GREEN)
        last = steps[-1]
        text = ("refused at the %s check: nothing else runs" % last[0]) if last[1].endswith("refused") \
            else "%s, so the order costs %s" % (last[1], honest.split(", ", 1)[1].split(" from")[0])
        fit_mono(d, 300, y + 266, text, ROSE if last[1].endswith("refused") else GREEN, 28, 1540)
    caption(d, "Length first, then the characters, then the range: only a good value becomes a Quantity.", AMBER, 960)


def boundary(img, d, cfg):
    """The request without the page, the shop that trusts the page, and the server that checks."""
    req = dict(facts(sections("ONE", "THE REQUEST")[0]))
    cheap = dict(facts(sections("ONE", "THE REQUEST")[1]))
    before = [dict(facts(s)) for s in sections("ONE", "BEFORE THE FIX")]
    after = facts(sections("ONE", "AFTER THE FIX")[0])
    cols = [(60, "THE REQUEST (no page)", CYAN), (680, "BEFORE THE FIX", ROSE), (1300, "AFTER THE FIX", GREEN)]
    for x, title, colour in cols:
        header(d, x, 185, title, colour, 560)
        panel(d, x, 245, 560, 620, colour, 3)
    # the requests
    for k, f in enumerate((req, cheap)):
        y = 275 + k * 310
        label(d, 90, y, "request %d" % (k + 1), MUTED, 28, True)
        for j, part in enumerate(f["fields"].split()):
            fit_mono(d, 90, y + 45 + j * 46, part, ROSE if part.endswith(("-3", "0.01")) else TEXT, 32, 500)
    label(d, 90, 790, "the page " + req["the page"].split(":")[0].split(" (")[0], MUTED, 26)
    label(d, 90, 826, "... but nobody has to use the page", AMBER, 26, True)
    # the trusting shop
    for k, f in enumerate(before):
        y = 275 + k * 310
        label(d, 710, y, "a shop that trusts the page", MUTED, 26, True)
        fit_mono(d, 710, y + 50, "price " + f["price"], TEXT, 28, 500)
        total = f["total"].split("  <-")[0]
        fit_mono(d, 710, y + 105, total.split(" instead")[0], ROSE, 40, 500)
        note = "a refund" if "-" in total.split("=")[1] else "instead of " + total.split("instead of ")[1]
        label(d, 710, y + 170, note, ROSE, 30, True)
    # the server that validates
    for k, (key, value) in enumerate(after):
        y = 275 + k * 195
        fit_mono(d, 1330, y, key, TEXT, 32, 500)
        colour = verdict_colour(value)
        for j, ln in enumerate(value.replace("accepted, ", "accepted\n").replace(" (", "\n(").replace(" from", "\nfrom").splitlines()):
            fit_mono(d, 1330, y + 48 + j * 40, ln, colour, 28, 500)
    caption(d, "The page helps honest customers; only the server can refuse a request.", AMBER, 920)


def parse_types(img, d, cfg):
    """One accepted and one refused raw string per field, and the type or the rule that comes out."""
    rows = []
    for head in ("QUANTITY", "SKU", "EMAIL", "COUNTRY", "AMOUNT"):
        lines = sections("TWO", head)[0]
        rows += [re.match(r'\s*(".*?")\s+(.*)', ln).groups() for ln in lines[:2]]
    header(d, 60, 185, "THE INPUT (raw text)", CYAN, 600)
    header(d, 820, 185, "THE RESULT (a type, or the broken rule)", GREEN, 1040)
    for i, (raw, result) in enumerate(rows):
        y = 250 + i * 66
        cell(d, 60, y, 600, 54, raw, CYAN, 28, TEXT)
        draw.arrow_right(d, 680, y + 27, 800, MUTED, 4)
        colour = verdict_colour(result)
        cell(d, 820, y, 1040, 54, result.replace("-> ", ""), colour, 28)
    form = sections("TWO", "THE RESULT")[0]
    n = re.search(r"THE RESULT, (\d+)", next(h for h in act("TWO") if h.startswith("THE RESULT"))).group(1)
    label(d, 60, 910, "A form with %s bad fields gets all %s problems at once: %s ..." % (n, n, form[0].strip()),
          MUTED, 26, True)
    caption(d, "Green: a typed value that cannot hold anything else. Rose: refused, naming the rule.", AMBER, 965)


def canonical_names(img, d, cfg):
    """Three spellings of one name, their code points, the canonical form, and the two registries."""
    spell = facts(sections("THREE", "THE INPUT, THREE")[0])
    before = dict(facts(sections("THREE", "BEFORE THE FIX")[0]))
    steps = facts(sections("THREE", "HOW THE CANONICAL")[0])
    after = dict(facts(sections("THREE", "AFTER THE FIX")[0]))
    header(d, 60, 185, "THE INPUT: THREE SPELLINGS", CYAN, 760)
    for i, (name, points) in enumerate(spell):
        y = 255 + i * 205
        label(d, 60, y, name, MUTED, 26, True)
        for j, cp in enumerate(points.split()):
            ch = chr(int(cp[2:], 16))
            shown = chr(int(cp[2:], 16) - 0xFF41 + 0x61) if 0xFF41 <= ord(ch) <= 0xFF5A else ch
            x = 60 + j * 250
            cell(d, x, y + 40, 230, 80, shown, CYAN if name != "full-width" else AMBER, 44, TEXT)
            label(d, x + 115, y + 130, cp, CYAN if name != "full-width" else AMBER, 24, True, "ma")
    label(d, 60, 870, "full-width letters are other characters (drawn here as plain letters)", AMBER, 22)
    # the steps
    header(d, 880, 185, "HOW IT IS MADE", AMBER, 460)
    for k, (name, value) in enumerate(steps):
        y = 260 + k * 150
        panel(d, 880, y, 460, 110, AMBER if k else LINE, 3)
        label(d, 1110, y + 12, name, AMBER, 26, True, "ma")
        fit_mono(d, 1110, y + 70, value, TEXT, 24, 430, "mm")
        if k:
            draw.arrow(d, 1110, y - 38, 1110, y - 4, MUTED, 4)
    # the registries
    header(d, 1400, 185, "THE RESULT", GREEN, 460)
    label(d, 1400, 255, "BEFORE THE FIX: raw text", ROSE, 26, True)
    for k, (name, value) in enumerate(before.items()):
        fit_mono(d, 1400, 300 + k * 50, "%-10s %s" % (name, "account" if value.startswith("new account: true") else "no"),
                 ROSE, 26, 460)
    n_before = sum(v.startswith("new account: true") for v in before.values())
    label(d, 1400, 460, "%d accounts for one name" % n_before, ROSE, 28, True)
    label(d, 1400, 560, "AFTER THE FIX: canonical", GREEN, 26, True)
    for k, (name, value) in enumerate(after.items()):
        fit_mono(d, 1400, 605 + k * 50, "%-10s %s" % (name, value.replace("new account: ", "account ")
                                                        .replace("refused: ", "")), GREEN, 26, 460)
    n_after = sum(v.startswith("new account") for v in after.values())
    label(d, 1400, 765, "%d account for one name" % n_after, GREEN, 28, True)
    caption(d, "Compare names only after the canonical form, or one name gets several accounts.")


def redos_steps(img, d, cfg):
    """The splits (a+)+$ tries on aaa!, and how the steps grow with the number of a's."""
    how = sections("FOUR", "HOW (a+)+$")[0]
    head = next(h for h in act("FOUR") if h.startswith("HOW (a+)+$"))
    splits = how[0].split()
    header(d, 60, 185, "THE INPUT " + re.search(r"READS (\S+)", head).group(1) + ": EVERY SPLIT OF THE a's IS TRIED", CYAN)
    for i, s in enumerate(splits):
        x = 60 + i * 455
        cell(d, x, 250, 430, 80, s, ROSE, 36, TEXT)
        label(d, x + 215, 340, "fails at the !", ROSE, 22, True, "ma")
    fit_mono(d, 60, 390, how[1].strip(), AMBER, 30, 1800)
    before = facts(sections("FOUR", "BEFORE THE FIX")[0])
    before = [(k, int(v.split()[0]), v) for k, v in before if re.match(r"\d", v)]
    after = facts(sections("FOUR", "AFTER THE FIX")[0])[0]
    after = (after[0] + " (a+$)", int(after[1].split()[0]), after[1])
    header(d, 60, 460, "THE RESULT: STEPS (bar length on a log scale)", AMBER)
    import math
    top = math.log10(max(s for _, s, _ in before))
    for i, (k, steps, text) in enumerate(before + [after]):
        y = 530 + i * 90
        colour = ROSE if i < len(before) else GREEN
        label(d, 60, y + 22, k, colour, 28, True, "lm")
        w = int(1150 * math.log10(steps) / top)
        d.rounded_rectangle([300, y, 300 + w, y + 44], radius=8, fill=colour)
        growth = re.search(r"\(x [\d.]+\)", text)
        fit_mono(d, 320 + w, y + 22, "%d steps %s" % (steps, growth.group(0) if growth else ""), colour, 28, 1600 - w, "lm")
    caption(d, "Five more a's: about 32 times the work. Without nesting: one pass.")


def valid_not_safe(img, d, cfg):
    """Two accepted display names that still need their own defence, and a rule stricter than the business."""
    names = [ln.strip() for ln in sections("FIVE", "THE INPUT, TWO")[0]]
    results = sections("FIVE", "THE RESULT")
    wholesale = dict(facts(results[2]))["quantity"]
    message = dict(facts(results[1]))
    header(d, 60, 185, "THE INPUT", CYAN, 560)
    header(d, 700, 185, "THE RESULT", GREEN, 1160)
    rows = [(n, r.strip()[len(n):].strip()) for n, r in zip(names, results[0])]
    for i, (name, res) in enumerate(rows):
        y = 260 + i * 160
        cell(d, 60, y, 560, 100, name, CYAN, 36, TEXT)
        draw.arrow_right(d, 630, y + 50, 690, MUTED, 4)
        verdict, still = res.split(", ", 1)
        panel(d, 700, y, 1160, 100, AMBER, 3)
        label(d, 730, y + 18, verdict + ": a valid name", GREEN, 28, True)
        label(d, 730, y + 58, "but " + still, AMBER, 28, True)
    y = 600
    cell(d, 60, y, 560, 100, "quantity 150", CYAN, 36, TEXT)
    draw.arrow_right(d, 630, y + 50, 690, MUTED, 4)
    panel(d, 700, y, 1160, 100, ROSE, 3)
    label(d, 730, y + 18, wholesale.split("  <-")[0], ROSE, 28, True)
    label(d, 730, y + 58, "a wholesale customer turned away: a business decision", AMBER, 28, True)
    fit_mono(d, 60, 760, "error message: \"%s\"   repeats the input? %s" % (message["message"], message["repeats it?"]),
             MUTED, 28, 1800)
    caption(d, "Valid is not safe: encode on output, bind parameters, write each limit down.", AMBER, 900)
