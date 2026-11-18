"""Pictures for the reverse engineering video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The section of an act whose header starts with `head`: (header line, body lines without indent)."""
    cur, title = None, None
    for ln in act(name):
        if not ln.startswith("  "):
            if cur is not None:
                break
            if ln.startswith(head):
                cur, title = [], ln
        elif cur is not None:
            cur.append(ln[2:])
    return title, cur


def body(name, head):
    return section(name, head)[1]


def cols(line):
    return re.split(r"\s{2,}", line.strip())


def paren(title):
    """What a header line says in brackets: "BEFORE THE PATCH (check: 12 ...):" gives "check: 12 ...". """
    m = re.search(r"\((.*)\)", title)
    return m.group(1) if m else ""


def keyed(name, head):
    """The key rows of a section: [(which key, first word of the result)]."""
    return [(k, v.split(":")[0]) for k, v in (re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in body(name, head) if re.match(r"(right|wrong|no) key", ln))]


def header(d, x, y, text, colour, size=26):
    f = font(SANS_B, size)
    w = int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour, size, max_w=None, anchor="la"):
    f = font(MONO_B, size)
    while max_w and f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=950):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def result_colour(word):
    return GREEN if word == "licensed" else AMBER


def code_block(d, x, y, w, lines, colour=TEXT, size=26, step=42, marks=()):
    """Code lines in a panel; a line containing one of `marks` is drawn in amber."""
    panel(d, x, y, w, len(lines) * step + 30)
    for i, ln in enumerate(lines):
        c = AMBER if any(m in ln for m in marks) else colour
        mono(d, x + 24, y + 16 + i * step, ln, c, size, w - 48)
    return y + len(lines) * step + 30


# ------------------------------------------------------------------ figures

def read_in_order_of_cost(img, d, cfg):
    """The core idea: one class file read four ways, cheapest first, and each way shows the key or the decision."""
    key = next(cols(ln)[-1] for ln in body("TWO", "THE CONSTANT POOL") if ln.startswith("*"))
    names = cols(body("FIVE", "BEFORE OBFUSCATION")[0])[1:]
    code = {ln.split()[0]: ln for ln in body("TWO", "THE BYTECODE")}
    bytecode = "   ".join(("6: ldc key", "9: " + code["9:"].split()[-1], " ".join(code["12:"].split())))
    decompiled = body("THREE", "AFTER DECOMPILING")[4].strip()
    rows = [("1  STRINGS", "the constant pool", key, ROSE),
            ("2  NAMES", "classes, methods, fields", "  ".join(names), AMBER),
            ("3  BYTECODE", "javap -c", bytecode, AMBER),
            ("4  DECOMPILER", "CFR", decompiled, ROSE)]
    header(d, 60, 215, "THE INPUT: bank/Licence.class, NEVER RUN", CYAN)
    label(d, 470, 272, "THE COST", AMBER, 26, True)
    label(d, 820, 272, "WHAT IT SHOWS", ROSE, 26, True)
    for i, (step, how, shows, colour) in enumerate(rows):
        y = 315 + i * 145
        panel(d, 60, y, 1800, 128)
        label(d, 90, y + 22, step, TEXT, 30, True)
        label(d, 90, y + 72, how, MUTED, 26)
        bar = 70 * (i + 1)
        d.rounded_rectangle([470, y + 44, 470 + bar, y + 84], radius=10, fill=AMBER)
        mono(d, 820, y + 64, shows, colour, 30, 1010, "lm")
    caption(d, "Cheapest first; every way shows the key or the decision.", AMBER, 915)


def client_run(img, d, cfg):
    """The program: three class files, three keys in, three results out."""
    classes = [cols(ln) for ln in body("ONE", "THE CLIENT")]
    header(d, 60, 215, "THE CLIENT: " + paren(section("ONE", "THE CLIENT")[0]).upper(), CYAN)
    for i, (name, role) in enumerate(classes):
        x = 60 + i * 610
        cell(d, x, 280, 580, 70, name + ".class", CYAN, 28)
        label(d, x + 290, 368, role, MUTED, 26, False, "ma")
    keys = [cols(ln) for ln in body("ONE", "THE INPUT")]
    results = keyed("ONE", "THE RESULT")
    header(d, 60, 450, "THE INPUT: THE KEY TYPED", CYAN)
    header(d, 1100, 450, "THE RESULT: WHAT IT PRINTS", GREEN)
    for i, ((which, typed), (_, word)) in enumerate(zip(keys, results)):
        y = 520 + i * 110
        label(d, 60, y + 40, which, TEXT, 30, True, "lm")
        cell(d, 280, y, 640, 80, typed, MUTED, 28, TEXT)
        d.line([(940, y + 40), (1080, y + 40)], fill=LINE, width=4)
        d.polygon([(1080, y + 28), (1100, y + 40), (1080, y + 52)], fill=LINE)
        cell(d, 1110, y, 740, 80, word, result_colour(word), 30)
    caption(d, "The check runs on the customer's own computer.", AMBER, 880)


def pool_and_bytecode(img, d, cfg):
    """Reading without running: the strings in the constant pool, and the bytecode of check."""
    strings = body("TWO", "THE CONSTANT POOL")
    title = section("TWO", "THE CONSTANT POOL")[0]
    header(d, 60, 215, "THE CONSTANT POOL: %s STRINGS" % re.search(r"\((\d+)", title).group(1), ROSE)
    y = 285
    for ln in strings:
        star = ln.startswith("*")
        cls, text = re.match(r"\*?\s*(\S+)\s+(\".*\")$", ln).groups()
        colour = ROSE if star else MUTED
        mono(d, 60, y, ("* " if star else "  ") + cls.replace("bank/", ""), colour, 24)
        mono(d, 330, y, text, TEXT if star else MUTED, 24, 560)
        y += 52
    label(d, 60, y + 20, "* names a key, a licence or an address", ROSE, 26, True)
    code = body("TWO", "THE BYTECODE")
    header(d, 980, 215, "THE BYTECODE OF check (javap -c)", AMBER)
    code_block(d, 980, 280, 880, code, MUTED, 26, 44, ("6:", "9:", "12:"))
    caption(d, "The key, in plain text; the decision, at offset 12 (ifeq).", AMBER, 900)


def decompile_before_after(img, d, cfg):
    """Decompiling: the method as written, and as CFR gives it back from the class file alone."""
    before = body("THREE", "BEFORE COMPILING")
    after_title, after = section("THREE", "AFTER DECOMPILING")
    header(d, 60, 215, "BEFORE COMPILING: THE SOURCE", CYAN)
    code_block(d, 60, 280, 880, before, TEXT, 26, 42)
    header(d, 980, 215, "AFTER DECOMPILING: " + paren(after_title).split(",")[0], GREEN)
    code_block(d, 980, 280, 880, after, TEXT, 26, 42, ("equals",))
    back = [cols(ln) for ln in body("THREE", "WHAT CAME BACK")]
    header(d, 980, 600, "WHAT CAME BACK", GREEN)
    for i, (what, value) in enumerate(back):
        y = 665 + i * 52
        label(d, 980, y, what, MUTED, 26, True)
        mono(d, 1150, y + 2, value, ROSE if what == "lost" else TEXT, 24, 710)


def patch_before_after(img, d, cfg):
    """Changing it: the check's size and results before and after a two-instruction patch."""
    before_title = section("FOUR", "BEFORE THE PATCH")[0]
    after_title = section("FOUR", "AFTER THE PATCH")[0]
    patch = {cols(ln)[0]: cols(ln)[1] for ln in body("FOUR", "THE PATCH") if len(cols(ln)) > 1}
    header(d, 60, 215, "BEFORE THE PATCH", CYAN)
    label(d, 60, 285, paren(before_title), TEXT, 28, True)
    header(d, 1260, 215, "AFTER THE PATCH", ROSE)
    label(d, 1260, 285, paren(after_title), TEXT, 28, True)
    for x, rows in ((60, keyed("FOUR", "BEFORE THE PATCH")), (1260, keyed("FOUR", "AFTER THE PATCH"))):
        for i, (which, word) in enumerate(rows):
            y = 350 + i * 100
            label(d, x, y + 36, which, TEXT, 28, True, "lm")
            colour = ROSE if word == "licensed" and which != "right key" else result_colour(word)
            cell(d, x + 190, y, 410, 72, word, colour, 28)
    panel(d, 700, 260, 520, 470, AMBER, 3)
    header(d, 730, 280, "THE PATCH (Patcher)", AMBER)
    label(d, 730, 355, "found by its shape", MUTED, 24)
    mono(d, 730, 390, patch["found"], TEXT, 26, 460)
    label(d, 730, 450, "new body", MUTED, 24)
    after_code = [ln for ln in body("FOUR", "AFTER THE PATCH") if re.match(r"\s*\d+:", ln)]
    for i, ln in enumerate(after_code):
        mono(d, 730, 490 + i * 46, ln.strip(), AMBER, 30, 460)
    label(d, 730, 600, patch["new body"].split(": ", 1)[1], TEXT, 26)
    caption(d, body("FOUR", "AFTER THE PATCH")[-1].strip().capitalize() + ".", ROSE, 800)


def xor_decode(img, d, cfg):
    """Obfuscation: names and strings before and after, one string decoded byte by byte, and what is still there."""
    before = {ln.split()[0]: cols(ln.split(None, 1)[1]) for ln in body("FIVE", "BEFORE OBFUSCATION")}
    after = {ln.split()[0]: cols(ln.split(None, 1)[1]) for ln in body("FIVE", "AFTER OBFUSCATION")}
    header(d, 60, 215, "BEFORE OBFUSCATION", CYAN)
    mono(d, 60, 280, "  ".join(before["names"]), TEXT, 24, 860)
    label(d, 60, 325, before["readable"][0], MUTED, 26)
    header(d, 980, 215, "AFTER OBFUSCATION", ROSE)
    mono(d, 980, 280, "  ".join(after["names"]), TEXT, 24, 860)
    label(d, 980, 325, after["readable"][0], MUTED, 26)
    mono(d, 980, 370, after["pool"][0], AMBER, 22, 880)
    title, rows = section("FIVE", "HOW ONE STRING")
    header(d, 60, 430, "HOW ONE STRING IS DECODED: " + paren(title).upper().replace("0X", "0x"), AMBER)
    table = [cols(ln) for ln in rows if re.match(r"(hex|XOR|byte|char)\b", ln)]
    for r, cells in enumerate(table):
        y = 500 + r * 70
        label(d, 60, y + 30, cells[0], TEXT, 28, True, "lm")
        for c, v in enumerate(cells[1:5]):
            colour = GREEN if cells[0] == "char" else AMBER if cells[0] == "XOR" else TEXT
            cell(d, 200 + c * 130, y, 110, 60, v.split()[0], colour, 28)
    whole = next(ln for ln in rows if ln.startswith("all"))
    label(d, 60, 790, whole, GREEN, 28, True)
    still = body("FIVE", "WHAT IS STILL THERE")
    header(d, 980, 500, "WHAT IS STILL THERE", ROSE)
    lines = [cols(still[0])[1], "decoder gives back: " + still[2].strip().split(" ")[0] + " strings",
             "check found by shape: " + still[3].split("patched: ")[1], "wrong key  " + re.split(r"\s{2,}", still[4].strip(), maxsplit=1)[1].split(":")[0]]
    for i, ln in enumerate(lines):
        mono(d, 980, 570 + i * 60, ln, ROSE if i else TEXT, 26, 880)
    caption(d, "Obfuscation raises the cost of reading; it does not stop it.", AMBER, 880)
