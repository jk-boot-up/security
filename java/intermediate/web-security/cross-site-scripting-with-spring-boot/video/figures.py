"""Pictures for the cross-site scripting with Spring Boot video, drawn by the `figure` slide kind.

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

def runs_line(d, x, y, text, colour=GREEN, size=26):
    return draw_runs(d, x, y, encoded_runs(text, colour), size, True)


def pipeline(img, d, cfg):
    """One value from the model to the browser: model, template, page (escaped), what the customer sees."""
    value = first("ONE", "THE VALUE")[0].strip()
    tline = first("ONE", "THE TEMPLATE LINE")[0].strip()
    page = first("ONE", "THE PAGE")[0].strip()
    zero = after(first("ONE", "THE RESULT"), "b elements:").split(",")[0].strip()
    steps = [("THE MODEL", AMBER, "review =", [(value, AMBER)]),
             ("THE TEMPLATE", CYAN, "product.html", mark(tline, 'th:text="${review}"', CYAN)),
             ("THYMELEAF WRITES", GREEN, "the page", encoded_runs(page)),
             ("THE BROWSER SHOWS", TEXT, "the customer sees", None)]
    for i, (title, c, note, runs) in enumerate(steps):
        y = 195 + i * 175
        header(d, 60, y, title, c if c != TEXT else LINE, 380)
        label(d, 60, y + 60, note, MUTED, 22, True)
        panel(d, 470, y - 5, 1390, 110, c if c != TEXT else LINE, 3)
        if runs is None:
            label(d, 500, y + 50, value, TEXT, 40, False, "lm")
            label(d, 1830, y + 50, "b elements: " + zero, GREEN, 26, True, "rm")
        else:
            draw_runs(d, 500, y + 50, runs, 26, True)
        if i < 3:
            draw.arrow(d, 250, y + 95, 250, y + 165, MUTED, 4)
    caption(d, "Green is the escaping Thymeleaf did on its own.", AMBER, 905)


def escaped_by_default(img, d, cfg):
    """th:text: the value, the template line, the escaped page line, and the elements a browser finds."""
    value = first("ONE", "THE VALUE")[0].strip()
    tline = first("ONE", "THE TEMPLATE LINE")[0].strip()
    page = first("ONE", "THE PAGE")[0].strip()
    res = first("ONE", "THE RESULT")
    found = res[0].split()
    zero = after(res, "b elements:").split(",")[0].strip()
    header(d, 60, 190, "THE VALUE", AMBER)
    cell(d, 60, 250, 520, 70, value, AMBER, 28)
    draw.arrow_right(d, 590, 285, 650, MUTED, 4)
    header(d, 660, 190, "THE TEMPLATE LINE", CYAN)
    panel(d, 660, 250, 1200, 70, CYAN)
    draw_runs(d, 685, 285, mark(tline, 'th:text="${review}"', CYAN), 26, True)
    header(d, 60, 380, "THE PAGE (as the server sent it)", GREEN)
    panel(d, 60, 440, 1800, 80, GREEN, 3)
    runs_line(d, 90, 480, page, GREEN, 30)
    header(d, 60, 580, "THE ELEMENTS A BROWSER FINDS", LINE)
    chips(d, 60, 640, found, lambda w: LINE, 26, 56)
    label(d, 60, 740, "b elements: " + zero + ", so the review stayed text", GREEN, 30, True)
    caption(d, "th:text escapes the value; nothing had to be remembered.", AMBER, 900)


def utext_trap(img, d, cfg):
    """th:utext: the template line, the page with the review as markup, the extra b element, and the guard."""
    tline = first("TWO", "THE TEMPLATE LINE")[0].strip()
    value = first("ONE", "THE VALUE")[0].strip()
    page = first("TWO", "THE PAGE")[0].strip()
    found = first("TWO", "THE RESULT")[0].split()
    plain = first("ONE", "THE RESULT")[0].split()
    guard = [ln.strip() for ln in first("TWO", "THE GUARD")]
    header(d, 60, 190, "THE TEMPLATE LINE", ROSE)
    panel(d, 60, 250, 1100, 70, ROSE)
    draw_runs(d, 85, 285, mark(tline, 'th:utext="${review}"', ROSE), 26, True)
    header(d, 60, 360, "THE PAGE", ROSE)
    panel(d, 60, 420, 1100, 70, ROSE)
    draw_runs(d, 85, 455, mark(page, value, ROSE), 28, True)
    header(d, 60, 530, "THE ELEMENTS A BROWSER FINDS", LINE)
    chips(d, 60, 590, found, lambda w: ROSE if w not in plain else LINE, 26, 56)
    label(d, 60, 680, first("TWO", "THE RESULT")[1].strip().split("  <-")[0], ROSE, 28, True)
    header(d, 1220, 190, "THE GUARD (a test)", AMBER)
    panel(d, 1220, 250, 640, 250, AMBER)
    label(d, 1245, 270, "templates that use th:utext", MUTED, 22, True)
    for j, ln in enumerate(guard[:2]):
        name, use = re.split(r"\s{2,}|\s(?=uses it$)", ln, 1)
        mono(d, 1245, 330 + j * 60, name, TEXT, 24)
        mono(d, 1835, 330 + j * 60, use, ROSE if use != "no" else GREEN, 24, "ra")
    label(d, 1245, 450, "any other: the test fails", AMBER, 22, True)
    caption(d, "th:utext is for the shop's own markup, never for customer text.", AMBER, 900)


def three_places(img, d, cfg):
    """Three values: the value, how the template writes it, and what Thymeleaf wrote, escapes in green."""
    vals = [re.split(r"\s{2,}", ln.strip(), 1) for ln in first("THREE", "THE VALUES")]
    tmpl = [ln.strip() for ln in first("THREE", "THE TEMPLATE")]
    page = [ln.strip() for ln in first("THREE", "THE PAGE")]
    res = [re.split(r"\s{2,}", ln.strip(), 1)[1] for ln in first("THREE", "THE RESULT")]
    places = ("attribute", "link", "script")
    header(d, 60, 190, "THE VALUE", AMBER, 420)
    header(d, 500, 190, "THE TEMPLATE", CYAN, 600)
    header(d, 1120, 190, "WHAT THYMELEAF WROTE", GREEN, 740)
    for i in range(3):
        y = 260 + i * 170
        label(d, 60, y, places[i], MUTED, 22, True)
        cell(d, 60, y + 30, 420, 70, vals[i][1], AMBER, 26)
        cell(d, 500, y + 30, 600, 70, tmpl[i], CYAN, 22)
        d.rounded_rectangle([1120, y + 30, 1860, y + 100], radius=10, fill=BG_CELL, outline=GREEN, width=3)
        text = page[i]
        size = 24
        while font(MONO_B, size).getlength(text) > 700 and size > 16:
            size -= 1
        draw_runs(d, 1140, y + 65, _script_runs(text), size, True)
        label(d, 1860, y + 110, res[i], GREEN, 20, True, "ra")
    caption(d, "Green is the escaping Thymeleaf chose for each place.", AMBER, 900)


def _script_runs(text):
    """Escaped characters in green: entities, %XX codes and backslash-escaped quotes."""
    runs, pos = [], 0
    for m in re.finditer(r'&[a-z]+;|%[0-9A-F]{2}|\\"', text):
        runs += [(text[pos:m.start()], TEXT), (m.group(0), GREEN)]
        pos = m.end()
    return runs + [(text[pos:], TEXT)]


def nonce_flow(img, d, cfg):
    """One request: the filter makes the nonce, the header writer and the template both read it, the browser decides."""
    how = [ln.strip() for ln in first("FOUR", "HOW THE NONCE")]
    resp = first("FOUR", "THE RESPONSE")
    src = next(ln.strip() for ln in resp if ln.strip().startswith("script-src"))
    script = resp[-1].strip()
    runs = first("FOUR", "WHICH SCRIPTS")
    shop = runs[0].split()[-1]
    other = runs[1].split("<-")[0].split()[-1]
    header(d, 60, 190, "1. NonceFilter", AMBER, 420)
    panel(d, 60, 250, 420, 250, AMBER, 3)
    fit_mono(d, 80, 275, how[0].split(": ", 1)[1], TEXT, 20, 350)
    fit_mono(d, 80, 320, how[1], TEXT, 20, 350)
    fit_mono(d, 80, 365, "runs before the headers", MUTED, 20, 350)
    cell(d, 80, 410, 380, 64, "cspNonce = <nonce>", AMBER, 24)
    draw.arrow(d, 490, 330, 600, 290, MUTED, 4)
    draw.arrow(d, 490, 440, 600, 470, MUTED, 4)
    header(d, 610, 190, "2. Spring Security header", CYAN)
    panel(d, 610, 250, 1250, 80, CYAN)
    draw_runs(d, 635, 290, mark(src, "'nonce-<nonce>'", AMBER), 26, True)
    header(d, 610, 370, "3. The template, on the shop's script", CYAN)
    panel(d, 610, 430, 1250, 80, CYAN)
    draw_runs(d, 635, 470, mark(script, "<nonce>", AMBER), 26, True)
    same = after(how, "the header's nonce is the script's nonce:").strip()
    label(d, 610, 530, "the header's nonce is the script's nonce: " + same, GREEN, 24, True)
    header(d, 60, 600, "4. WHAT THE BROWSER DECIDES", LINE)
    cell(d, 60, 660, 880, 70, script, GREEN, 24)
    label(d, 960, 695, "the shop's script: " + ("runs" if shop == "yes" else "blocked"), GREEN, 26, True, "lm")
    cell(d, 60, 760, 880, 70, "<script>...</script>  (no nonce)", ROSE, 24)
    label(d, 960, 795, "a script without the nonce: " + ("runs" if other == "yes" else "blocked"), ROSE, 26, True, "lm")
    caption(d, "A new nonce on every page; the header and the page always agree.", AMBER, 900)
