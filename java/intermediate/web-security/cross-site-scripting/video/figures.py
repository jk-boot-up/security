"""Pictures for the cross-site scripting video, drawn by the `figure` slide kind.

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
ENCODED = re.compile(r"&[a-z]+;|&#\d+;|%[0-9A-F]{2}|\\u[0-9a-f]{4}")


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


def first(name, head):
    return sections(name, head)[0]


def after(lines, prefix):
    """The rest of the first line that starts with `prefix`."""
    return next(ln.strip()[len(prefix):] for ln in lines if ln.strip().startswith(prefix))


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


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def draw_runs(d, x, y, runs, size=26, anchor_mid=False):
    """Draws (text, colour) runs left to right; returns the x after the last run."""
    f = font(MONO_B, size)
    for text, c in runs:
        d.text((x, y), text, font=f, fill=c, anchor="lm" if anchor_mid else "la")
        x += f.getlength(text)
    return x


def mark(text, part, colour, rest=TEXT):
    """`text` as runs, with the first occurrence of `part` in `colour`."""
    i = text.find(part) if part else -1
    if i < 0:
        return [(text, rest)]
    return [(text[:i], rest), (part, colour), (text[i + len(part):], rest)]


def encoded_runs(text, colour=GREEN, rest=TEXT):
    """`text` as runs, with every encoded character (&lt; %26 \\u0027 ...) in `colour`."""
    runs, pos = [], 0
    for m in ENCODED.finditer(text):
        runs += [(text[pos:m.start()], rest), (m.group(0), colour)]
        pos = m.end()
    return runs + [(text[pos:], rest)]


def chips(d, x, y, words, colour_of, size=24, h=50):
    f = font(MONO_B, size)
    for w in words:
        c = colour_of(w)
        width = int(f.getlength(w)) + 30
        d.rounded_rectangle([x, y, x + width, y + h], radius=12, fill=BG_CELL, outline=c, width=3)
        d.text((x + width // 2, y + h // 2), w, font=f, fill=c, anchor="mm")
        x += width + 14
    return x


# ------------------------------------------------------------------ figures

def text_or_markup(img, d, cfg):
    """Ana's review pasted in (a b element, bold text) and encoded (the characters themselves, no new element)."""
    typed = first("ONE", "THE REVIEW")[0].strip()
    pasted = next(ln.strip() for ln in first("ONE", "THE PAGE AS BUILT") if ln.strip().startswith("<p>"))
    encoded = after(first("TWO", "AFTER ENCODING"), "between tags").strip()
    found = after(first("ONE", "THE RESULT"), "Ana's review").split()
    plain = after(first("ONE", "THE RESULT"), "plain text").split()
    zero = after(first("TWO", "THE RESULT"), "Ana's review, encoded: b elements").strip()
    header(d, W // 2 - 140, 190, "ANA TYPES", AMBER, 280)
    cell(d, W // 2 - 300, 250, 600, 70, typed, AMBER, 30)
    for i, (title, c) in enumerate((("PASTED AS IT IS", ROSE), ("ENCODED FIRST", GREEN))):
        x = 60 + i * 920
        draw.arrow(d, W // 2 + (-200 if i == 0 else 200), 330, x + 440, 380, MUTED, 4)
        header(d, x, 385, title, c)
        panel(d, x, 445, 880, 440, c, 3)
        label(d, x + 25, 465, "the page holds:", MUTED, 22, True)
        if i == 0:
            draw_runs(d, x + 25, 505, [("<p>", TEXT), (typed, ROSE), ("</p>", TEXT)], 26)
        else:
            draw_runs(d, x + 25, 505, [("<p>", TEXT)] + encoded_runs(encoded) + [("</p>", TEXT)], 24)
        label(d, x + 25, 575, "the browser finds:", MUTED, 22, True)
        if i == 0:
            chips(d, x + 25, 610, found, lambda w: ROSE if w not in plain else LINE)
        else:
            chips(d, x + 25, 610, plain, lambda w: LINE)
        label(d, x + 25, 690, "the customer sees:", MUTED, 22, True)
        if i == 0:
            inner = re.sub(r"<[^>]+>", "", typed)
            label(d, x + 25, 730, inner, TEXT, 44, True)
            label(d, x + 25, 810, "a b element the shop never wrote", ROSE, 26, True)
        else:
            label(d, x + 25, 730, typed, TEXT, 40)
            label(d, x + 25, 810, "exactly what Ana typed; b elements: " + zero, GREEN, 26, True)
    caption(d, "Same text: pasted, it is markup; encoded, it stays text.", AMBER, 920)


def review_pasted(img, d, cfg):
    """The review box, the page as built with the review in red, and the elements a browser finds; then Eve's script."""
    typed = first("ONE", "THE REVIEW")[0].strip()
    page = [ln for ln in first("ONE", "THE PAGE AS BUILT")]
    res = first("ONE", "THE RESULT")
    plain = after(res, "plain text").split()
    found = after(res, "Ana's review").split()
    header(d, 60, 190, "THE REVIEW BOX", AMBER)
    cell(d, 60, 250, 440, 70, typed, AMBER, 26)
    draw.arrow_right(d, 510, 285, 570, MUTED, 4)
    header(d, 580, 190, "THE PAGE AS BUILT", ROSE)
    panel(d, 580, 250, 1280, 40 + 44 * len(page), LINE)
    for j, ln in enumerate(page):
        draw_runs(d, 600, 270 + j * 44, mark(ln.rstrip(), typed, ROSE), 26)
    header(d, 60, 460, "WHAT A BROWSER FINDS", CYAN)
    label(d, 60, 535, "plain text", MUTED, 26, True)
    chips(d, 300, 520, plain, lambda w: LINE)
    label(d, 60, 615, "Ana's review", MUTED, 26, True)
    chips(d, 300, 600, found, lambda w: ROSE if w not in plain else LINE)
    extra = next(ln.strip() for ln in res if ln.strip()[0].isdigit()).split("  <-")[0]
    label(d, 300, 670, extra, ROSE, 26, True)
    eve = first("ONE", "THE REVIEW (typed by Eve")[0].strip()
    script = sections("ONE", "THE RESULT")[1][0].strip().split("  <-")[0]
    header(d, 60, 740, "EVE TYPES", AMBER)
    cell(d, 60, 800, 760, 70, eve, ROSE, 26)
    draw.arrow_right(d, 830, 835, 900, MUTED, 4)
    cell(d, 910, 800, 420, 70, script, ROSE, 26)
    label(d, 1350, 835, "runs as the shop's own code", ROSE, 26, True, "lm")
    caption(d, "The browser cannot tell the shop's markup from the customer's.", AMBER, 920)


def encoders(img, d, cfg):
    """Four places: the text before and after its encoder, encoded characters in green, and how a code is made."""
    before = first("TWO", "BEFORE ENCODING")
    after_ = first("TWO", "AFTER ENCODING")
    header(d, 60, 190, "THE PLACE", CYAN, 380)
    header(d, 470, 190, "BEFORE ENCODING", AMBER, 560)
    header(d, 1120, 190, "AFTER ENCODING", GREEN, 740)
    for i, (b, a) in enumerate(zip(before, after_)):
        place = re.split(r"\s{2,}", b.strip())[0]
        bv = re.split(r"\s{2,}", b.strip(), 1)[1]
        av = re.split(r"\s{2,}", a.strip(), 1)[1]
        y = 260 + i * 110
        label(d, 60, y + 40, place, TEXT, 28, True, "lm")
        d.rounded_rectangle([470, y, 1030, y + 80], radius=10, fill=BG_CELL, outline=AMBER, width=3)
        risky = set("<>\"'&") | ({" "} if place.startswith("URL") else set())
        runs = [(ch, ROSE if ch in risky else TEXT) for ch in bv]
        draw_runs(d, 495, y + 40, runs, 26, True)
        draw.arrow_right(d, 1040, y + 40, 1110, MUTED, 4)
        d.rounded_rectangle([1120, y, 1860, y + 80], radius=10, fill=BG_CELL, outline=GREEN, width=3)
        draw_runs(d, 1145, y + 40, encoded_runs(av), 26, True)
    hexline = next(ln.strip() for ln in first("TWO", "HOW EACH CHARACTER") if ln.strip().startswith('" is'))
    quote = next(ln.split() for ln in first("TWO", "HOW EACH CHARACTER") if ln.strip().startswith('"'))
    panel(d, 60, 720, 1800, 150, CYAN)
    label(d, 90, 745, "how a code is made", MUTED, 22, True)
    mono(d, 90, 790, hexline, TEXT, 28)
    mono(d, 90, 830, "URL: %s    script: %s    HTML: %s" % (quote[2], quote[3], quote[1]), GREEN, 28)
    caption(d, "Red characters could end the text; green is their encoded form.", AMBER, 910)


def filter_misses(img, d, cfg):
    """The filter changes Ben's honest words, misses Eve's attribute attack, and what the div ends up with."""
    bf = first("THREE", "BEFORE THE FILTER")
    af = first("THREE", "AFTER A FILTER")
    header(d, 60, 190, "BEFORE THE FILTER", AMBER, 820)
    header(d, 1000, 190, "AFTER A FILTER THAT DELETES < AND >", ROSE, 860)
    who = ("Ben's review", "Eve's fit note")
    for i in range(2):
        y = 260 + i * 130
        label(d, 60, y, who[i], MUTED, 22, True)
        before = bf[i].strip()
        afterv, note = [s.strip() for s in af[i].split("<-")]
        d.rounded_rectangle([60, y + 30, 880, y + 100], radius=10, fill=BG_CELL, outline=AMBER, width=3)
        draw_runs(d, 85, y + 65, [(ch, ROSE if ch in "<>" else TEXT) for ch in before], 26, True)
        draw.arrow_right(d, 890, y + 65, 990, MUTED, 4)
        d.rounded_rectangle([1000, y + 30, 1860, y + 100], radius=10, fill=BG_CELL, outline=ROSE, width=3)
        draw_runs(d, 1025, y + 65, [(afterv, TEXT)], 26, True)
        label(d, 1860, y, note.lower(), ROSE, 22, True, "ra")
    pasted = first("THREE", "THE RESULT")[0].strip().split("  ")
    encoded = sections("THREE", "THE RESULT")[1][0].strip().split("  ")
    header(d, 60, 540, "THE DIV, WITH EVE'S FILTERED NOTE PASTED IN", ROSE)
    chips(d, 60, 600, pasted, lambda w: ROSE if w.startswith("on") else LINE, 26, 56)
    header(d, 60, 700, "THE DIV, WITH EVE'S NOTE ENCODED FOR THE ATTRIBUTE", GREEN)
    chips(d, 60, 760, encoded, lambda w: GREEN if w.startswith("data-fit") else LINE, 26, 56)
    caption(d, "A filter guesses at the place; an encoder is chosen for it.", AMBER, 900)


def csp_nonce(img, d, cfg):
    """How 16 bytes become a 24-character nonce, and which of two scripts the policy lets run."""
    made = first("FOUR", "HOW THE NONCE")
    example = after(made, "example, bytes 00 01 02 ... 0f ->").strip()
    nbytes = int(made[0].split()[0])
    header(d, 60, 190, "HOW THE NONCE IS MADE", CYAN)
    label(d, 560, 212, made[0].strip() + "; every 3 bytes become 4 characters", MUTED, 24, False, "lm")
    groups = [list(range(nbytes))[k:k + 3] for k in range(0, nbytes, 3)]
    x = 60
    for g, grp in enumerate(groups):
        c = (GREEN, AMBER)[g % 2]
        gx = x
        for b in grp:
            cell(d, x, 270, 86, 56, "%02x" % b, c, 24)
            x += 92
        chars = example[g * 4:g * 4 + 4]
        mid = (gx + x - 6) // 2
        draw.arrow(d, mid, 334, mid, 372, MUTED, 3)
        cell(d, mid - 80, 378, 160, 56, chars, c, 26)
        x += 26
    groups_line = next(ln.strip() for ln in made if "groups" in ln)
    label(d, 60, 460, groups_line + "   (example bytes 00 to 0f; a real nonce is random)", MUTED, 22, True)
    resp = first("FOUR", "THE RESPONSE")
    src = next(ln.strip() for ln in resp if ln.strip().startswith("script-src"))
    script = resp[-1].strip()
    header(d, 60, 530, "THE RESPONSE HEADER", CYAN)
    panel(d, 60, 590, 900, 80, CYAN)
    draw_runs(d, 85, 630, mark("script-src " + src.split(" ", 1)[1], "'nonce-<nonce>'", AMBER), 26, True)
    runs = first("FOUR", "WHICH SCRIPTS")
    shop = runs[0].split()[-1]
    eve = runs[1].split("<-")[0].split()[-1]
    header(d, 1020, 530, "THE SCRIPTS ON THE PAGE", AMBER)
    cell(d, 1020, 590, 840, 70, script.split(">")[0] + ">", GREEN, 24)
    label(d, 1040, 680, "the shop's script carries the nonce: runs " + shop, GREEN, 24, True)
    cell(d, 1020, 730, 840, 70, first("ONE", "THE REVIEW (typed by Eve")[0].strip(), ROSE, 24)
    label(d, 1040, 820, "Eve's script has no nonce: runs " + eve + ", blocked", ROSE, 24, True)
    caption(d, "A new nonce on every page; a script without it does not run.", AMBER, 910)
