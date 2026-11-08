"""Pictures for the keystore basics video, drawn by the `figure` slide kind.

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
DARK = (15, 23, 42)


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("BEFORE THE FIX")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"[\s:]+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE SAVING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def hexbytes(text):
    """'68617264 2d63 ...' -> ['68', '61', '72', '64', '2d', '63']"""
    h = text.replace("...", "").replace(" ", "")
    return [h[i:i + 2] for i in range(0, len(h), 2)]


def cells(d, x, y, items, colour, w=88, h=72, size=34, gap=8):
    for i, t in enumerate(items):
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=colour, anchor="mm")
    return x + len(items) * (w + gap) - gap


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def field(d, x, y, w, h, text, colour, size=28):
    """A value in a dark box, in mono."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B, size), fill=colour, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour, size=40):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (16 if sub else 0)), text, font=font(SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 26), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def mark(d, x, y, ok):
    d.text((x, y), "✓" if ok else "✗", font=font(SANS_B, 52), fill=GREEN if ok else ROSE, anchor="mm")


# ------------------------------------------------------------------ figures

def key_homes(img, d, cfg):
    """The core idea on real values: where a key lives decides who can read it."""
    src = value("KEY_TEXT =", "ONE")
    in_class = value("in the class file", "ONE").split()[0]
    perms = value("open.key", "TWO", "BEFORE THE FIX").split()[0]
    others = value("readable by others", "TWO", "BEFORE THE FIX").split()[0]
    hidden = value("the key's bytes are in the file", "THREE", "AFTER SAVING").split()[0]
    derived = value("key", "FOUR", "AFTER DERIVING")
    rows = [("In the code", src, "found in the class file: %s" % in_class, "anyone with the program", ROSE),
            ("In a file", "open.key   %s" % perms, "readable by others: %s" % others, "anyone with a login", AMBER),
            ("In a keystore", "bank.p12  (password)", "key's bytes in the file: %s" % hidden, "only with the password", GREEN),
            ("Derived", derived, "made from a password + salt", "made when needed", CYAN)]
    label(d, 100, 215, "where the key lives", MUTED, 26, True)
    label(d, 470, 215, "a real example from the demo", MUTED, 26, True)
    label(d, 1390, 215, "who can read the key", MUTED, 26, True)
    for i, (home, val, fact, who, c) in enumerate(rows):
        y = 262 + i * 152
        d.rounded_rectangle([100, y, 430, y + 120], radius=16, fill=PANEL, outline=c, width=4)
        d.text((265, y + 60), home, font=font(SANS_B, 34), fill=c, anchor="mm")
        field(d, 470, y + 4, 840, 64, val, c, 26)
        label(d, 480, y + 82, fact, MUTED, 26)
        draw.arrow_right(d, 1320, y + 36, 1380, c, 4)
        d.rounded_rectangle([1390, y + 4, 1820, y + 68], radius=32, fill=c)
        d.text((1605, y + 36), who, font=font(SANS_B, 26), fill=DARK, anchor="mm")
    caption(d, "The cipher is the same in every row. Only the key's home changes who can read it.", AMBER, 885)


def compiled_key(img, d, cfg):
    """A key written in the code is still there, byte for byte, in the compiled class."""
    src = value("KEY_TEXT =", "ONE")
    key_hex = value("key bytes", "ONE")
    found = value("in the class file", "ONE").split()[0]
    header(d, 100, 210, "BEFORE COMPILING", GREEN)
    label(d, 470, 222, "HardCodedKey.java, the source code", TEXT, 28, True)
    field(d, 100, 272, 1720, 78, 'KEY_TEXT = "%s"' % src.strip('"'), GREEN, 34)
    draw.arrow(d, 960, 356, 960, 404, CYAN, 4)
    big_box(d, 760, 406, 400, 76, "the compiler", "", CYAN, 34)
    draw.arrow(d, 960, 486, 960, 528, CYAN, 4)
    header(d, 100, 535, "AFTER COMPILING", ROSE)
    label(d, 450, 547, "HardCodedKey.class, the program everyone gets", TEXT, 28, False)
    text = src.strip('"')
    items = hexbytes(key_hex)
    end = cells(d, 100, 600, [("·" if ch == " " else ch) for ch in text[:len(items)]], ROSE, w=96, h=64, size=32, gap=8)
    cells(d, 100, 672, items, MUTED, w=96, h=48, size=24, gap=8)
    label(d, end + 20, 690, "...", MUTED, 34, True)
    label(d, 100, 745, "the first 16 of the key's bytes, as the class file holds them  ·  in the class file: %s" % found,
          TEXT, 28)
    caption(d, "Anyone with the program has the key: also the repository, every copy, every build.", AMBER, 850)


def _perm_row(d, y, name, perms, colour):
    """rw-r--r-- as nine cells in three groups: owner, group, others."""
    label(d, 100, y + 36, name, colour, 30, True, "lm")
    x = 420
    for g, who in enumerate(("owner", "group", "others")):
        part = perms[g * 3:g * 3 + 3]
        hot = g > 0 and "r" in part
        c = ROSE if hot else (GREEN if g == 0 else MUTED)
        cells(d, x, y, list(part), c, w=72, h=72, size=38, gap=6)
        label(d, x + 111, y + 84, who, c, 24, True, "ma")
        x += 260
    return x


def permissions(img, d, cfg):
    """A file is only as private as its permissions: rw-r--r-- before, rw------- after."""
    before = value("open.key", "TWO", "BEFORE THE FIX").split()[0]
    after = value("locked.key", "TWO", "AFTER THE FIX").split()[0]
    r_before = value("readable by others", "TWO", "BEFORE THE FIX").split()[0]
    r_after = value("readable by others", "TWO", "AFTER THE FIX").split()[0]
    held = section("TWO", "WHAT THE FILE HOLDS").strip()
    header(d, 100, 205, "BEFORE THE FIX: a new file", ROSE)
    x = _perm_row(d, 265, "open.key", before, ROSE)
    label(d, x + 20, 300, "others can read: %s" % r_before, ROSE, 30, True, "lm")
    header(d, 100, 420, "AFTER THE FIX: owner only", GREEN)
    x = _perm_row(d, 480, "locked.key", after, GREEN)
    label(d, x + 20, 515, "others can read: %s" % r_after, GREEN, 30, True, "lm")
    label(d, 100, 628, "r = read     w = write     - = not allowed", MUTED, 28)
    header(d, 100, 690, "WHAT THE FILE HOLDS", AMBER)
    field(d, 470, 680, 900, 66, held, AMBER, 30)
    label(d, 1395, 713, "the key, in the clear", AMBER, 28, True, "lm")
    caption(d, "Permissions decide who reads the file. Whoever reads it, or its backup, has the key.", TEXT, 830)


def keystore_file(img, d, cfg):
    """What goes into the keystore file, what can be found in it, and the rounds it records."""
    name = value("name", "THREE", "BEFORE SAVING")
    key = value("key", "THREE", "BEFORE SAVING")
    pw = value("password", "THREE", "BEFORE SAVING")
    hidden = value("the key's bytes are in the file", "THREE", "AFTER SAVING").split()[0]
    shown = value("the name, as UTF-16, is in the file", "THREE", "AFTER SAVING").split()[0]
    header(d, 100, 205, "BEFORE SAVING", GREEN)
    for i, (k, v, c) in enumerate((("name", name, CLIENT), ("key", key, AMBER), ("password", pw, CYAN))):
        y = 268 + i * 86
        label(d, 100, y + 32, k, c, 28, True, "lm")
        field(d, 260, y, 640, 64, v, c, 24)
    draw.arrow_right(d, 915, 400, 1000, MUTED, 4)
    big_box(d, 1010, 330, 240, 140, "PKCS12", "save", TEXT, 38)
    draw.arrow_right(d, 1260, 400, 1330, MUTED, 4)
    header(d, 1340, 205, "AFTER SAVING: bank.p12", ROSE)
    facts = [("key's bytes in it", hidden, hidden == "false"), ("name, as UTF-16", shown, False),
             ("password in it", "no", True)]
    for i, (k, v, ok) in enumerate(facts):
        y = 268 + i * 86
        d.rounded_rectangle([1340, y, 1820, y + 64], radius=10, fill=BG_CELL, outline=LINE, width=2)
        label(d, 1360, y + 32, k, TEXT, 26, False, "lm")
        label(d, 1730, y + 32, v, GREEN if ok else AMBER, 28, True, "rm")
        mark(d, 1770, y + 32, ok)
    # the rounds the file records: key lock (made at put) and integrity check (made at save)
    rows = [("default", section("THREE", "BEFORE THE FIX"), "default", ROSE),
            ("raised at save", section("THREE", "BEFORE THE FIX"), "raised at save", ROSE),
            ("put and save", section("THREE", "AFTER THE FIX"), "put and save", GREEN)]
    label(d, 100, 560, "rounds the file records", TEXT, 30, True)
    label(d, 760, 564, "key lock (made at put)", MUTED, 22, True, "ma")
    label(d, 1160, 564, "integrity check (made at save)", MUTED, 22, True, "ma")
    for i, (k, text, lab, c) in enumerate(rows):
        lock, check = re.search(re.escape(lab) + r"\s+\[(\d+), (\d+)\]", text).groups()
        y = 610 + i * 82
        label(d, 100, y + 32, k, c, 28, True, "lm")
        field(d, 600, y, 320, 64, lock, ROSE if lock == "10000" else GREEN, 30)
        field(d, 1000, y, 320, 64, check, ROSE if check == "10000" else GREEN, 30)
    label(d, 1370, 700, "The key is locked when", AMBER, 28, True)
    label(d, 1370, 740, "it is put, so raise its", AMBER, 28, True)
    label(d, 1370, 780, "rounds there, too.", AMBER, 28, True)
    caption(d, "The file hides the key, not its name. Its protection is only as slow as its rounds.", TEXT, 880)


def pbkdf2_rounds(img, d, cfg):
    """PBKDF2 by hand: HMAC of salt + 1, then HMAC of the last result, all of them XORed together."""
    pw = value("password", "FOUR", "BEFORE DERIVING")
    salt = value("salt", "FOUR", "BEFORE DERIVING")
    rounds = value("rounds", "FOUR", "BEFORE DERIVING")
    u1 = value("U1", "FOUR")
    u2 = value("U2", "FOUR")
    key = value("key", "FOUR", "AFTER DERIVING")
    same = value("the same key the JDK's PBKDF2 gave", "FOUR", "AFTER DERIVING")
    header(d, 100, 205, "BEFORE DERIVING", GREEN)
    for i, (k, v, c) in enumerate((("password", pw, CYAN), ("salt", salt, CLIENT), ("rounds", rounds, AMBER))):
        x = 100 + i * 590
        label(d, x, 270, k, c, 26, True)
        field(d, x, 306, 540, 60, v, c, 26)
    steps = [("round 1", "HMAC(password, salt + 00000001)", u1),
             ("round 2", "HMAC(password, U1)", u2),
             ("...", "each round: HMAC of the last result", "..."),
             ("round %s" % rounds, "HMAC(password, U%d)" % (int(rounds) - 1), "U%s" % rounds)]
    for i, (k, how, v) in enumerate(steps):
        y = 400 + i * 78
        label(d, 100, y + 30, k, AMBER, 28, True, "lm")
        label(d, 330, y + 30, how, TEXT, 26, False, "lm")
        draw.arrow_right(d, 900, y + 30, 970, MUTED, 3)
        name = "U%d" % (i + 1) if i < 2 else ""
        label(d, 985, y + 30, name, CYAN, 26, True, "lm")
        field(d, 1050, y, 770, 60, v, CYAN if i < 2 else MUTED, 24)
    label(d, 100, 728, "KEY = U1 xor U2 xor ... xor U%s" % rounds, AMBER, 30, True)
    header(d, 100, 785, "AFTER DERIVING: the key", ROSE)
    field(d, 560, 775, 780, 64, key, ROSE, 26)
    label(d, 1360, 807, "same as the JDK's: %s" % same, GREEN, 28, True, "lm")
    caption(d, "Every guess at the password costs %s HMACs. That is why there are rounds." % rounds, AMBER, 880)


def rotation(img, d, cfg):
    """A message names its key; removing the old key loses the old messages."""
    five = section("FIVE")
    total = re.search(r"A STORED MESSAGE \((\d+) bytes\)", five).group(1)
    length = re.search(r"1 byte\s+(\w+)", five).group(1)
    name_n, name = re.search(r"(\d+) bytes\s+(statements-v\d)", five).groups()
    sealed_n = re.search(r"(\d+) bytes\s+cipher text", five).group(1)
    label(d, 100, 208, "a stored message, %s bytes, part by part" % total, TEXT, 30, True)
    parts = [("1", length, "name length", MUTED, 140), (name_n, name, "the key's name", CLIENT, 480),
             ("12", "nonce", "new each time", CYAN, 420), (sealed_n, "cipher text + tag", "the statement", ROSE, 640)]
    x = 100
    for n, v, note, c, w in parts:
        field(d, x, 255, w - 12, 70, v, c, 28)
        label(d, x + (w - 12) // 2, 335, "%s byte%s: %s" % (n, "" if n == "1" else "s", note), c, 22, True, "ma")
        x += w
    before = section("FIVE", "BEFORE REMOVING")
    after = section("FIVE", "AFTER REMOVING")
    for i, (title, text, vault, c) in enumerate((("BEFORE REMOVING statements-v1", before, ["statements-v1", "statements-v2"], GREEN),
                                                 ("AFTER REMOVING statements-v1", after, ["statements-v2"], ROSE))):
        x = 100 + i * 880
        header(d, x, 410, title, c)
        label(d, x, 480, "the keystore holds: " + ", ".join(vault), MUTED, 24, True)
        for j, m in enumerate(re.findall(r"^\s+(old|new), names (\S+): (.+?)(?:  <-.*)?$", text, re.M)):
            y = 530 + j * 100
            ok = m[2] != "refused"
            d.rounded_rectangle([x, y, x + 820, y + 80], radius=12, fill=BG_CELL, outline=GREEN if ok else ROSE, width=3)
            label(d, x + 24, y + 40, "%s message (%s)" % m[:2], TEXT, 26, False, "lm")
            label(d, x + 770, y + 40, m[2], GREEN if ok else ROSE, 26, True, "rm")
            mark(d, x + 795, y + 40, ok)
    caption(d, "Seal old messages again with the new key, before the old key is removed.", AMBER, 790)
    label(d, W // 2, 850, "And the bill: the keystore's password needs a home, and a lost file is lost data.", MUTED, 28,
          False, "ma")
