"""Pictures for the password hashing video, drawn by the `figure` slide kind.

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


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("BEFORE HASHING")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def full_hash(label, act, part):
    """A 32-byte hash printed over two lines: the label's line and the one under it."""
    m = re.search(r"^    " + re.escape(label) + r"\s+([0-9a-f ]{35})\n\s+([0-9a-f ]{35})$", section(act, part), re.M)
    return (m.group(1) + " " + m.group(2)).replace(" ", "")


def number(pattern, act):
    return re.search(pattern, section(act)).group(1)


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE HASHING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def cells(d, x, y, items, colour, w=88, h=72, size=34, gap=8):
    for i, t in enumerate(items):
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=colour, anchor="mm")
    return x + len(items) * (w + gap) - gap


def node(d, x, y, w, h, text, c, size=26, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, 40), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 30), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def hexbytes(h):
    h = h.replace(" ", "")
    return [h[i:i + 2] for i in range(0, len(h), 2)]


def groups(h, n=8):
    h = h.replace(" ", "")
    return " ".join(h[i:i + 8] for i in range(0, len(h), 8)[:n])


# ------------------------------------------------------------------ figures

def hash_one(img, d, cfg):
    """SHA-256 turns one password into 32 bytes, always the same ones."""
    pw = value("ana", "TWO", "BEFORE HASHING")
    h = full_hash("ana", "TWO", "AFTER HASHING")
    e = header(d, 100, 215, "BEFORE HASHING", GREEN)
    label(d, e + 24, 237, "the password ana typed: %d characters" % len(pw), TEXT, 30, True, "lm")
    cells(d, 100, 280, list(pw), GREEN)
    cells(d, 100, 358, ["%02x" % ord(c) for c in pw], MUTED, h=44, size=24)
    label(d, 880, 380, "← each character as a byte, in hex", MUTED, 26, False, "lm")
    draw.arrow(d, 460, 412, 460, 465, CYAN, 4)
    big_box(d, 220, 470, 480, 110, "SHA-256", "a one-way scramble", CYAN)
    label(d, 760, 525, "no key, no secret: anyone can run it, nobody can run it backwards", MUTED, 26, False, "lm")
    draw.arrow(d, 460, 585, 460, 635, CYAN, 4)
    e = header(d, 100, 640, "AFTER HASHING", ROSE)
    label(d, e + 24, 662, "the hash: always 32 bytes, whatever goes in", TEXT, 30, True, "lm")
    b = hexbytes(h)
    cells(d, 100, 705, b[:16], ROSE, w=100, h=56, size=28, gap=10)
    cells(d, 100, 771, b[16:], ROSE, w=100, h=56, size=28, gap=10)
    caption(d, "Same password in  →  always the same 32 bytes out. There is no way back.", AMBER, 870)


def plain_rows(img, d, cfg):
    """Act one: the row holds the password itself."""
    pw = value("password", "ONE", "BEFORE STORING")
    stored = value("stored", "ONE", "AFTER STORING").split()[0]
    readable = number(r"(\d+ of \d+) passwords readable", "ONE")
    header(d, 100, 230, "BEFORE STORING", GREEN)
    label(d, 100, 310, "what ana typed", MUTED, 28, True)
    node(d, 100, 355, 500, 90, pw, GREEN, 44)
    draw.arrow_right(d, 640, 400, 860, MUTED, 4)
    label(d, 750, 350, "stored as it is", MUTED, 24, False, "ma")
    header(d, 900, 230, "AFTER STORING", ROSE)
    label(d, 900, 310, "ana's row in the accounts table", MUTED, 28, True)
    node(d, 900, 355, 300, 90, "ana", TEXT, 40)
    node(d, 1220, 355, 500, 90, stored, ROSE, 44)
    header(d, 100, 540, "THE ATTACK", AMBER)
    label(d, 100, 625, "The table is stolen. The thief reads:", TEXT, 34, True)
    label(d, 100, 690, readable + " passwords, at once, with no work at all.", ROSE, 40, True)
    caption(d, "Nothing to crack. And reused passwords open other shops too.", AMBER, 860)


def table_attack(img, d, cfg):
    """Act two: no salt, so equal passwords give equal hashes and one table cracks them."""
    pw = value("ana", "TWO", "BEFORE HASHING")
    h = full_hash("ana", "TWO", "AFTER HASHING")
    short = h[:8] + "…"
    words = number(r"(\d+) common passwords", "TWO")
    cracked = re.search(r"cracked (\d+ of \d+): (.+)$", section("TWO"), re.M)
    header(d, 100, 205, "BEFORE HASHING", GREEN, 400)
    header(d, 760, 205, "AFTER HASHING (SHA-256)", ROSE, 520)
    for i, who in enumerate(("ana", "ben")):
        y = 275 + i * 90
        node(d, 100, y, 150, 66, who, TEXT, 32)
        node(d, 270, y, 230, 66, pw, GREEN, 30)
        draw.arrow_right(d, 520, y + 33, 740, MUTED, 3)
        label(d, 630, y - 2, "SHA-256", MUTED, 20, True, "ma")
        node(d, 760, y, 520, 66, groups(h, 2) + " …", ROSE, 30)
    label(d, 1310, 340, "identical: anyone can", AMBER, 28, True, "lm")
    label(d, 1310, 378, "see they match", AMBER, 28, True, "lm")
    header(d, 100, 490, "THE ATTACK: A TABLE MADE IN ADVANCE", AMBER)
    label(d, 100, 560, "%s common passwords, each hashed once:" % words, TEXT, 28, True)
    node(d, 100, 605, 260, 62, pw, GREEN, 28)
    draw.arrow_right(d, 375, 636, 450, MUTED, 3)
    node(d, 465, 605, 260, 62, short, ROSE, 28)
    label(d, 100, 690, "+ %d more words, hashed the same way" % (int(words) - 1), MUTED, 24)
    draw.arrow_right(d, 760, 636, 960, AMBER, 4)
    label(d, 860, 600, "look up", AMBER, 22, True, "ma")
    label(d, 990, 610, "cracked " + cracked.group(1) + ":", ROSE, 34, True)
    label(d, 990, 660, cracked.group(2), ROSE, 34, True)
    caption(d, "One table fits every account: no guessing per account.", AMBER, 860)


def salted_hash(img, d, cfg):
    """Act three: how a salted hash is made, and why ana's and ben's now differ."""
    salt = value("salt", "THREE", "BEFORE HASHING")
    pw = value("password", "THREE", "BEFORE HASHING").split(" = ")[0]
    joined = number(r"salt \+ password = (\d+) bytes", "THREE")
    h = full_hash("hash", "THREE", "AFTER HASHING")
    ben_end = number(r"salt ending ([0-9a-f]+)", "THREE")
    ben_h = re.search(r"^    hash\s+([0-9a-f ]+?) \.\.\.$", section("THREE", "BEN"), re.M).group(1)
    e = header(d, 100, 205, "BEFORE HASHING", GREEN)
    label(d, e + 24, 227, "ana: her salt, then her password, joined into %s bytes" % joined, TEXT, 28, True, "lm")
    sb = hexbytes(salt)
    x = cells(d, 100, 270, sb, AMBER, w=62, h=56, size=24, gap=6)
    cells(d, x + 30, 270, list(pw), GREEN, w=62, h=56, size=28, gap=6)
    label(d, 100 + 16 * 34, 335, "salt: 16 bytes, new for each account, not a secret", AMBER, 22, False, "ma")
    label(d, x + 30 + 4 * 68, 335, "password: welcome1", GREEN, 22, False, "ma")
    draw.arrow(d, 760, 372, 760, 420, CYAN, 4)
    big_box(d, 560, 425, 400, 90, "SHA-256", "", CYAN)
    draw.arrow(d, 760, 520, 760, 565, CYAN, 4)
    e = header(d, 100, 570, "AFTER HASHING", ROSE)
    label(d, e + 24, 592, "stored: the salt, a $, and the hash", TEXT, 28, True, "lm")
    node(d, 100, 635, 1720, 62, groups(salt, 4) + "  $  " + groups(h, 4) + " …", ROSE, 28)
    label(d, 100, 740, "ben, same password, salt ending %s:" % ben_end, CLIENT, 30, True)
    node(d, 100, 785, 1720, 62, groups(salt, 3) + " " + ben_end + "  $  " + ben_h + " …", CLIENT, 28)
    caption(d, "One byte of salt changed: a completely different hash. The table no longer fits.", AMBER, 890)


def rounds_chain(img, d, cfg):
    """Act four: what 100000 rounds of PBKDF2 are."""
    sec = section("FOUR")
    rounds = value("rounds", "FOUR", "BEFORE HASHING")
    r = {n: re.search(r"^    round %s\s+([0-9a-f]{8})" % n, sec, re.M).group(1) for n in ("1", "2", "3", rounds)}
    h = full_hash("hash", "FOUR", "AFTER HASHING")
    salt = value("salt", "FOUR", "BEFORE HASHING")
    header(d, 100, 205, "BEFORE HASHING", GREEN)
    label(d, 100, 270, "password  welcome1    salt  %s    rounds  %s" % (groups(salt, 4), rounds), TEXT, 28, True)
    header(d, 100, 330, "THE ROUNDS: HMAC-SHA-256, KEYED WITH THE PASSWORD", CYAN)
    xs = [100, 470, 840, 1210, 1580]
    tops = [("salt + 00000001", MUTED), ("round 1", CYAN), ("round 2", CYAN), ("round 3", CYAN),
            ("round " + rounds, CYAN)]
    vals = ["", r["1"] + "…", r["2"] + "…", r["3"] + "…", r[rounds] + "…"]
    for i, (x, (name, c)) in enumerate(zip(xs, tops)):
        node(d, x, 410, 270, 90, "", c)
        label(d, x + 135, 440, name, c, 24, True, "mm")
        if vals[i]:
            label(d, x + 135, 475, vals[i], TEXT, 24, False, "mm")
        else:
            label(d, x + 135, 475, "the start", TEXT, 22, False, "mm")
        if i < 4:
            draw.arrow_right(d, x + 275, 455, xs[i + 1] - 5, AMBER, 3)
            label(d, (x + 270 + xs[i + 1]) // 2, 505, "HMAC" if i < 3 else "… 99996 more", AMBER, 20, True, "ma")
    label(d, 100, 545, "each round hashes the round before: none can be skipped", MUTED, 26)
    header(d, 100, 610, "AFTER HASHING: XOR ALL THE ROUNDS TOGETHER", ROSE)
    label(d, 100, 690, "round 1  ⊕  round 2  ⊕  round 3  ⊕  …  ⊕  round %s  =" % rounds, TEXT, 30, True)
    node(d, 100, 740, 1720, 62, groups(h, 8), ROSE, 30)
    caption(d, "One hash now costs %s steps, for the shop and for every guess." % rounds, AMBER, 860)


def upgrade(img, d, cfg):
    """Act five: what logins cost, and erin's old hash upgraded at login."""
    one = number(r"one login\s+(\d+) steps", "FIVE")
    many = number(r"1000 wrong logins (\d+) steps", "FIVE")
    old = value("erin", "FIVE", "BEFORE THE UPGRADE")
    ana = value("ana", "FIVE", "BEFORE THE UPGRADE")
    result = number(r"erin logs in: (\w+)", "FIVE")
    new = value("erin", "FIVE", "AFTER THE UPGRADE")
    header(d, 100, 205, "THE COST OF LOGINS", AMBER)
    label(d, 100, 280, "1 login", TEXT, 30, True, "lm")
    d.rounded_rectangle([480, 258, 492, 302], radius=4, fill=AMBER)
    label(d, 520, 280, "%s steps" % format(int(one), ","), AMBER, 30, True, "lm")
    label(d, 100, 350, "1,000 wrong logins", TEXT, 30, True, "lm")
    d.rounded_rectangle([480, 328, 1500, 372], radius=8, fill=ROSE)
    label(d, 1530, 350, "%s steps" % format(int(many), ","), ROSE, 30, True, "lm")
    label(d, 100, 400, "so the login page itself can be attacked: limit the attempts", MUTED, 26)
    header(d, 100, 480, "BEFORE THE UPGRADE", GREEN)
    node(d, 100, 545, 200, 66, "erin", TEXT, 30)
    node(d, 320, 545, 380, 66, old, ROSE, 30)
    node(d, 100, 630, 200, 66, "ana", TEXT, 30)
    node(d, 320, 630, 380, 66, ana, GREEN, 30)
    draw.arrow_right(d, 730, 578, 1040, AMBER, 4)
    label(d, 885, 520, "erin logs in", AMBER, 26, True, "ma")
    label(d, 885, 600, "password known: " + result, AMBER, 24, False, "ma")
    header(d, 1080, 480, "AFTER THE UPGRADE", ROSE)
    node(d, 1080, 545, 200, 66, "erin", TEXT, 30)
    node(d, 1300, 545, 380, 66, new, GREEN, 30)
    caption(d, "The rounds are stored with each hash, so they can be raised at the next login.", AMBER, 800)
