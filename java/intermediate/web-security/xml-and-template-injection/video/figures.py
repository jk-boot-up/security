"""Pictures for the XXE and template injection video, drawn by the `figure` slide kind.

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


def marked(d, x, y, text, marks, size=26, base=TEXT):
    """One line of mono text, with every piece in `marks` ({piece: colour}) drawn in its own colour."""
    f = font(MONO_B, size)
    pattern = "|".join(re.escape(m) for m in sorted(marks, key=len, reverse=True))
    for piece in re.split("(%s)" % pattern, text) if pattern else [text]:
        if piece:
            d.text((x, y), piece, font=f, fill=marks.get(piece, base))
            x += f.getlength(piece)


def mark_split(text):
    """'value  <- NOTE' -> ('value', 'NOTE')."""
    head, _, note = text.partition("  <- ")
    return head.strip(), note.strip()


# ------------------------------------------------------------------ figures

def reader_obeys(img, d, cfg):
    """The core idea: an XML file and a template both carry an instruction, and the reader carries it out."""
    upload = [l.strip() for l in sections("TWO", "THE UPLOAD")[0]]
    entity = next(l for l in upload if l.startswith("<!ENTITY"))
    note_line = next(l for l in upload if "&xxe;" in l)
    note = mark_split(sections("TWO", "THE RESULT")[0][2].split(None, 1)[1])[0]
    name = sections("FOUR", "THE CUSTOMER'S NAME")[0][0].strip()
    built = sections("FOUR", "THE TEMPLATE AS BUILT")[0][0].strip()
    result = mark_split(sections("FOUR", "THE RESULT")[1][0])[0]
    rows = [("AN INVOICE UPLOAD (XML)", [entity, note_line], {"&xxe;": ROSE, entity: ROSE}, "XML parser",
             ["note =", note]),
            ("AN E-MAIL TEMPLATE, WITH THE NAME PASTED IN", [built], {name: ROSE}, "template engine",
             [result.split(", ", 1)[0] + ",", result.split(", ", 1)[1]])]
    for i, (title, lines, marks, reader, res) in enumerate(rows):
        y = 200 + i * 340
        header(d, 60, y, title, AMBER)
        panel(d, 60, y + 60, 880, 70 + 44 * len(lines), ROSE, 3)
        for j, ln in enumerate(lines):
            fs = 22 if max(len(l) for l in lines) > 50 else 24
            marked(d, 85, y + 90 + j * 44, ln, marks, fs)
        mid = y + 95 + 22 * len(lines)
        draw.arrow_right(d, 950, mid, 1030, MUTED, 4)
        cell(d, 1040, mid - 45, 300, 90, reader, CLIENT, 26)
        draw.arrow_right(d, 1350, mid, 1430, MUTED, 4)
        panel(d, 1440, mid - 60, 420, 120, ROSE, 3)
        for j, t in enumerate(res):
            fit_mono(d, 1650, mid - 22 + j * 44, t, ROSE, 24, 390, "mm")
    label(d, 60, 440, "the file names a server file; the parser opens it", MUTED, 24)
    label(d, 60, 735, "the name is an expression; the engine runs it", MUTED, 24)
    caption(d, "Red is what the attacker wrote; the reader follows it.", AMBER, 920)


def honest_upload(img, d, cfg):
    """The honest invoice: the XML lines, the parser, and the three stored fields."""
    xml = sections("ONE", "THE UPLOAD")[0]
    fields = [ln.split(None, 1) for ln in sections("ONE", "THE RESULT")[0]]
    rule = sections("ONE", "WHAT THE PARSER DOES")[0][0].strip()
    header(d, 60, 200, "THE UPLOAD", AMBER)
    panel(d, 60, 260, 860, 60 + 52 * len(xml), AMBER, 3)
    for j, ln in enumerate(xml):
        marked(d, 90, 290 + j * 52, ln, {"&amp;": CYAN}, 28)
    draw.arrow_right(d, 940, 420, 1040, MUTED, 4)
    cell(d, 1060, 375, 220, 90, "parser", CLIENT, 28)
    draw.arrow_right(d, 1290, 420, 1360, MUTED, 4)
    header(d, 1380, 200, "THE RESULT", GREEN)
    for i, (k, v) in enumerate(fields):
        y = 270 + i * 100
        label(d, 1380, y + 32, k, MUTED, 24, True, "lm")
        cell(d, 1510, y, 360, 64, v, GREEN, 24)
    header(d, 60, 680, "WHAT THE PARSER DOES", CYAN)
    marked(d, 60, 750, rule, {"&amp;": CYAN}, 28)
    caption(d, "An entity is a name the parser replaces; &amp; is the built-in one for &.", AMBER, 900)


def entity_fetch(img, d, cfg):
    """The external entity reads a server file into the note; the entity bomb multiplies by ten six times."""
    upload = [l.strip() for l in sections("TWO", "THE UPLOAD")[0]]
    entity = next(l for l in upload if l.startswith("<!ENTITY"))
    file_name = re.search(r'\.\.\./([^"]+)"', entity).group(1)
    note = mark_split(sections("TWO", "THE RESULT")[0][2].split(None, 1)[1])[0]
    header(d, 60, 190, "THE UPLOAD", ROSE)
    panel(d, 60, 245, 820, 120, ROSE, 3)
    marked(d, 85, 265, entity, {"xxe": ROSE}, 22)
    marked(d, 85, 315, next(l for l in upload if "&xxe;" in l), {"&xxe;": ROSE}, 22)
    draw.arrow_right(d, 890, 305, 980, MUTED, 4)
    cell(d, 990, 260, 200, 90, "parser", CLIENT, 26)
    draw.arrow_right(d, 1200, 305, 1280, MUTED, 4)
    header(d, 1290, 190, "A FILE ON THE SERVER", CYAN)
    panel(d, 1290, 245, 570, 120, CYAN, 3)
    mono(d, 1310, 265, file_name, MUTED, 22)
    fit_mono(d, 1310, 310, note, TEXT, 24, 530)
    x = header(d, 60, 390, "THE RESULT", ROSE)
    mono(d, x + 24, 412, "note = " + note, ROSE, 26, "lm")
    label(d, 1290, 412, "the uploader chose the file", ROSE, 24, True, "lm")

    worked = [l.strip() for l in sections("TWO", "WHAT IT WOULD EXPAND TO")[0]]
    levels = worked[0].split("=")[1].count("10")
    copies = int(worked[0].split("=")[2].split()[0].replace(",", ""))
    assert 10 ** levels == copies
    header(d, 60, 480, heading("TWO", "THE UPLOAD (an entity bomb").rstrip(":").replace("THE UPLOAD ", "THE BOMB "), AMBER)
    cw = 250
    for k in range(levels + 1):
        x = 60 + k * (cw + 10)
        n = 10 ** k
        nm = "a%d" % (levels - k)
        cell(d, x, 545, cw, 60, nm, AMBER, 26)
        mono(d, x + cw // 2, 640, "{:,}".format(n), TEXT, 24, "mm")
        label(d, x + cw // 2, 672, "copy" if n == 1 else "copies", MUTED, 20, False, "mm")
    label(d, 60, 710, "each name holds ten of the name below it", MUTED, 24)
    mono(d, 60, 755, worked[1], AMBER, 26)
    res = sections("TWO", "THE RESULT")[1][0].strip()
    luck = act("TWO")[-1].strip()
    mono(d, 60, 810, res, AMBER, 24)
    caption(d, luck[0].upper() + luck[1:] + ".", AMBER, 900)


def doctype_refused(img, d, cfg):
    """The safe parser's settings, read back from the code, and what happens to each upload."""
    settings = sections("THREE", "THE PARSER SETTINGS")[0]
    header(d, 60, 190, "THE PARSER SETTINGS", CYAN)
    for i, ln in enumerate(settings):
        name, value = ln[:26].strip(), ln[26:].strip()
        value, note = mark_split(value)
        y = 250 + i * 72
        fix = bool(note)
        panel(d, 60, y, 860, 60, GREEN if fix else LINE, 3 if fix else 2)
        mono(d, 80, y + 30, name, TEXT, 22, "lm")
        fit_mono(d, 520, y + 30, value, GREEN if fix else TEXT, 22, 300, "lm")
        if fix:
            label(d, 905, y + 30, "the fix", GREEN, 22, True, "rm")
    ups = sections("THREE", "THE UPLOADS AND RESULTS")[0]
    header(d, 980, 190, "THE UPLOADS AND RESULTS", AMBER)
    for i in range(0, len(ups), 2):
        y = 250 + (i // 2) * 135
        res = ups[i + 1].replace("->", "").strip()
        ok = res.startswith("accepted")
        mono(d, 980, y + 10, ups[i].strip(), TEXT, 24)
        cell(d, 980, y + 50, 880, 60, res, GREEN, 22, text_colour=GREEN if ok else AMBER)
    caption(d, "An invoice never needs a DOCTYPE, so both attacks stop at the first line.", AMBER, 900)


def expression_walk(img, d, cfg):
    """The pasted name, walked by the engine one method call at a time, ending at the secret."""
    name = sections("FOUR", "THE CUSTOMER'S NAME")[0][0].strip()
    built = sections("FOUR", "THE TEMPLATE AS BUILT")[0][0].strip()
    walks = sections("FOUR", "HOW THE ENGINE WORKS IT OUT")
    result = mark_split(sections("FOUR", "THE RESULT")[1][0])[0]
    header(d, 60, 190, "THE TEMPLATE AS BUILT (the name pasted in)", AMBER)
    panel(d, 60, 245, 1800, 70, ROSE, 3)
    marked(d, 85, 262, built, {name: ROSE}, 28)
    for r, walk in enumerate(walks):
        y = 350 + r * 220
        header(d, 60, y, "HOW THE ENGINE WORKS IT OUT: " + "".join(s.split(" -> ")[0] for s in walk), CYAN, size=22)
        x = 60
        bw = 560
        for k, step in enumerate(walk):
            parts = [p.strip() for p in step.split(" -> ")]
            call = parts[1] if len(parts) == 3 else parts[0]
            value = parts[-1]
            last = k == len(walk) - 1
            c = ROSE if last else CLIENT
            cell(d, x, y + 60, bw, 56, call, c, 24)
            fit_mono(d, x + bw // 2, y + 145, value, ROSE if last else TEXT, 24, bw - 10, "mm")
            if not last:
                draw.arrow_right(d, x + bw + 6, y + 88, x + bw + 54, MUTED, 4)
            x += bw + 60
    x = header(d, 60, 800, "THE RESULT", ROSE)
    mono(d, x + 24, 822, result, ROSE, 28, "lm")
    caption(d, "Each dot calls a method on whatever the last one returned.", AMBER, 900)


def logic_less(img, d, cfg):
    """The logic-less engine: two named holes, three templates, and the escaping table."""
    holes = sections("FIVE", "THE VALUES")[0][0].split()
    tpl = sections("FIVE", "THE TEMPLATES AND RESULTS")[0]
    markup = sections("FIVE", "THE NAME")[0][0].strip()
    table = re.findall(r"(\S+) -> (\S+)", " ".join(sections("FIVE", "HOW EACH CHARACTER")[0]))
    result = sections("FIVE", "THE RESULT")[0][0].strip()
    header(d, 60, 190, "THE ONLY HOLES", CYAN)
    for i, h in enumerate(holes):
        cell(d, 60 + i * 230, 250, 210, 64, h, CYAN, 26)
    header(d, 560, 190, "THE TEMPLATES AND RESULTS", GREEN)
    for i in range(0, len(tpl), 2):
        y = 250 + (i // 2) * 80
        label(d, 560, y + 4, tpl[i].strip(), MUTED, 22)
        fit_mono(d, 560, y + 36, tpl[i + 1].replace("->", "").strip(), GREEN, 24, 1300)
    header(d, 60, 510, "THE NAME (with markup)", AMBER)
    mono(d, 60, 575, markup, AMBER, 28)
    header(d, 60, 640, "HOW EACH CHARACTER IS ESCAPED", CYAN)
    for i, (ch, esc) in enumerate(table):
        x = 60 + i * 300
        cell(d, x, 700, 80, 60, ch, AMBER, 28)
        draw.arrow_right(d, x + 88, 730, x + 130, MUTED, 3)
        cell(d, x + 138, 700, 140, 60, esc, GREEN, 24)
    x = header(d, 60, 795, "THE RESULT", GREEN)
    mono(d, x + 24, 817, result, GREEN, 28, "lm")
    caption(d, "No expressions, no objects, escaped values: text stays text.", AMBER, 900)
