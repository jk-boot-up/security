"""Pictures for the Spring Security Crypto password hashing video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS,
                                   SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
DARK = (15, 23, 42)


def act(name):
    """One act of the output ("TWO"), without its title line."""
    return re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)


def part(name, head):
    """The lines under one section header of an act, such as BEFORE HASHING, stripped."""
    text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(head), act(name), re.M).group(1)
    return [ln.strip() for ln in text.splitlines()]


def after(name, head, label):
    """The text after a label on a line of one section."""
    for ln in part(name, head):
        if ln.startswith(label):
            return ln[len(label):].strip()
    raise KeyError(label)


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE HASHING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=900):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cell(d, x, y, w, h, text, colour, size=32, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, 40), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


def segments(d, x, y, parts, size=30):
    """A stored text drawn as coloured pieces, each with a short note under it."""
    f = font(MONO_B, size)
    for text, colour, note in parts:
        w = int(f.getlength(text)) + 24
        d.rounded_rectangle([x, y, x + w, y + 66], radius=10, fill=BG_CELL, outline=colour, width=3)
        d.text((x + w // 2, y + 33), text, font=f, fill=colour, anchor="mm")
        if note:
            for i, ln in enumerate(note.split("\n")):
                label(d, x + w // 2, y + 80 + i * 30, ln, colour, 24, i == 0, "ma")
        x += w + 10
    return x


def bcrypt_parts():
    p = dict(id=after("TWO", "THE PARTS", "id").split()[0], version=after("TWO", "THE PARTS", "version").split()[0],
             cost=after("TWO", "THE PARTS", "cost").split()[0], salt=after("TWO", "THE PARTS", "salt"),
             hash=after("TWO", "THE PARTS", "hash"))
    p["rounds"] = re.search(r"= (\d+) rounds", after("TWO", "THE PARTS", "cost")).group(1)
    p["length"] = re.search(r"STORED TEXT, (\d+) CHARACTERS", act("TWO")).group(1)
    return p


# ------------------------------------------------------------------ figures

def stored_text(img, d, cfg):
    """What bcrypt makes from one password: the stored text, piece by piece."""
    p = bcrypt_parts()
    pw = after("TWO", "BEFORE HASHING", "ana's password")
    header(d, 100, 215, "BEFORE HASHING", GREEN)
    ins = [("password", pw, GREEN, "typed by ana"), ("salt", p["salt"], CYAN, "16 random bytes, new each time"),
           ("cost", p["cost"], AMBER, "2^%s = %s rounds" % (p["cost"], p["rounds"]))]
    for i, (name, val, c, note) in enumerate(ins):
        x = [100, 520, 1300][i]
        w = [380, 720, 480][i]
        label(d, x, 280, name, c, 28, True)
        label(d, x, 316, note, MUTED, 22)
        cell(d, x, 352, w, 66, val, c, 30)
    for x in (290, 880, 1540):
        draw.arrow(d, x, 425, 960, 470, MUTED, 3)
    big_box(d, 720, 475, 480, 100, "bcrypt", "%s rounds of slow work" % p["rounds"], CLIENT)
    draw.arrow(d, 960, 580, 960, 625, CLIENT, 4)
    e = header(d, 100, 600, "AFTER HASHING", ROSE)
    label(d, e + 24, 622, "the stored text, %s characters" % p["length"], TEXT, 30, True, "lm")
    segments(d, 100, 670, [(p["id"], ROSE, "id\npicks the\nalgorithm"), (p["version"], MUTED, "version"),
                           (p["cost"] + "$", AMBER, "cost"), (p["salt"], CYAN, "salt\n22 characters"),
                           (p["hash"], GREEN, "hash\n31 characters")], 28)
    caption(d, "The salt and the cost are kept with the hash, so a login can redo it.", AMBER, 900)


def weak_encoders(img, d, cfg):
    """Two weak ways to store welcome1, and what the attack costs against them."""
    pw = after("ONE", "BEFORE HASHING", "ana's password")
    plain = after("ONE", "AFTER HASHING: PLAIN TEXT", "stored").split()[0]
    salt = after("ONE", "AFTER HASHING: FAST SHA-256", "salt")
    hsh = after("ONE", "AFTER HASHING: FAST SHA-256", "hash")
    attack = part("ONE", "THE ATTACK")[0].split("  <-")[0]
    header(d, 100, 215, "BEFORE HASHING", GREEN)
    cell(d, 100, 285, 380, 80, pw, GREEN, 36)
    label(d, 290, 380, "ana's password", MUTED, 24, False, "ma")
    header(d, 640, 215, "AFTER HASHING", ROSE)
    draw.arrow_right(d, 490, 325, 620, MUTED, 3)
    rows = [("plain text", ROSE, [(plain, ROSE, "the password itself")]),
            ("fast SHA-256 + salt", AMBER, [(salt, CYAN, "salt"), (hsh, AMBER, "hash, one quick step")])]
    for i, (name, c, parts) in enumerate(rows):
        y = 285 + i * 200
        label(d, 640, y - 4, name, c, 28, True)
        segments(d, 640, y + 36, parts, 26)
    draw.arrow(d, 290, 420, 290, 560, MUTED, 3)
    draw.arrow_right(d, 290, 560, 620, MUTED, 3)
    header(d, 100, 700, "THE ATTACK", ROSE)
    label(d, 100, 770, attack, TEXT, 34, True)
    caption(d, "A salt alone is not enough: each guess must also be slow.", AMBER, 900)


def login_check(img, d, cfg):
    """A login redoes the hash with the cost and salt from the stored text, and compares."""
    p = bcrypt_parts()
    rows = []
    for typed in ("welcome1", "guess"):
        lines = part("TWO", "LOGIN: ANA TYPES " + typed)
        again = next(l for l in lines if l.startswith("STEP 2.")).split()[-1]
        same = next(l for l in lines if l.startswith("STEP 3.")).split()[-1]
        rows.append((typed, again, same))
    header(d, 100, 210, "BEFORE: THE STORED TEXT", GREEN)
    segments(d, 100, 270, [(p["id"], ROSE, ""), (p["version"], MUTED, ""), (p["cost"] + "$", AMBER, "cost"),
                           (p["salt"], CYAN, "salt"), (p["hash"], GREEN, "stored hash")], 26)
    label(d, 100, 400, "Step 1: read the cost and the salt.   Step 2: hash the typed password with them.", TEXT, 28, True)
    label(d, 100, 440, "Step 3: compare with the stored hash.", TEXT, 28, True)
    header(d, 100, 510, "AFTER: TWO LOGINS", ROSE)
    for i, (typed, again, same) in enumerate(rows):
        y = 580 + i * 140
        ok = same == "true"
        c = GREEN if ok else ROSE
        cell(d, 100, y, 260, 72, typed, TEXT, 30)
        label(d, 230, y + 80, "typed", MUTED, 22, False, "ma")
        draw.arrow_right(d, 370, y + 36, 470, MUTED, 3)
        big_box(d, 480, y, 220, 72, "bcrypt", "", CLIENT)
        draw.arrow_right(d, 710, y + 36, 800, MUTED, 3)
        cell(d, 810, y, 640, 72, again, c, 26)
        label(d, 1130, y + 80, "hash again", MUTED, 22, False, "ma")
        d.text((1500, y + 36), ("= stored  →  " if ok else "≠ stored  →  ") + same,
               font=font(SANS_B, 34), fill=c, anchor="lm")
    caption(d, "No password is ever stored or decrypted: the hash is made again.", AMBER, 900)


def four_algorithms(img, d, cfg):
    """Four algorithms behind one interface: what each writes into its stored text."""
    pw = after("THREE", "BEFORE HASHING", "the password")
    lines = part("THREE", "AFTER HASHING")
    checks = after("THREE", "THE LOGIN CHECK", "each one matches welcome1:").split(", ")
    header(d, 100, 210, "BEFORE HASHING", GREEN)
    cell(d, 100, 270, 320, 76, pw, GREEN, 34)
    header(d, 560, 210, "AFTER HASHING", ROSE)
    notes = {"bcrypt": "cost 10", "pbkdf2": "settings kept in the code", "scrypt": "CPU, memory, lanes",
             "argon2": "memory, passes, lanes"}
    colours = [CLIENT, CYAN, AMBER, GREEN]
    for i, ln in enumerate(lines):
        name, rest = ln.split(None, 1)
        length, settings = rest.split("  ", 1)
        y = 270 + i * 140
        c = colours[i]
        draw.arrow_right(d, 430, 308, 540, MUTED, 3) if i == 0 else None
        d.rounded_rectangle([560, y, 1820, y + 118], radius=14, fill=PANEL, outline=c, width=3)
        label(d, 590, y + 18, name, c, 34, True)
        label(d, 590, y + 68, length, MUTED, 24)
        label(d, 840, y + 18, settings, TEXT, 30, True)
        label(d, 840, y + 68, notes.get(name, ""), MUTED, 24)
        label(d, 1790, y + 68, "matches: " + checks[i], GREEN if checks[i] == "true" else ROSE, 26, True, "ra")
    caption(d, "Same two calls, encode and matches, for all four. Swapping is one line.", AMBER, 860)


def attack_costs(img, d, cfg):
    """The same eight guesses against bcrypt at cost 4 and cost 10."""
    tries = dict(re.findall(r"(\w+) (\d+)", after("FOUR", "THE ATTACK", "guesses:")))
    cracked = after("FOUR", "THE RESULT", "cracked:").split("  <-")[0].split(", ")
    res = part("FOUR", "THE RESULT")
    total = re.search(r"in (\d+) guesses", res[0]).group(1)
    r4, r10 = (re.search(r"(\d+) rounds", res[i]).group(1) for i in (0, 1))
    times = re.search(r"is (\d+) times", res[2]).group(1)
    header(d, 100, 210, "THE ATTACK: 8 COMMON PASSWORDS, ONE ACCOUNT AT A TIME", ROSE)
    for i, (who, n) in enumerate(tries.items()):
        x = 100 + i * 345
        ok = who in cracked
        c = ROSE if ok else GREEN
        d.rounded_rectangle([x, 280, x + 320, 440], radius=14, fill=PANEL, outline=c, width=3)
        label(d, x + 160, 300, who, TEXT, 34, True, "ma")
        label(d, x + 160, 350, "%s guesses" % n, MUTED, 28, False, "ma")
        label(d, x + 160, 392, "cracked" if ok else "survives", c, 30, True, "ma")
    label(d, 100, 460, "%s guesses in all, and 4 of 5 accounts fall at any cost." % total, TEXT, 30, True)
    header(d, 100, 540, "THE RESULT: THE WORK BEHIND EACH GUESS", AMBER)
    for i, (cost, rounds, c) in enumerate((("cost 4", r4, CYAN), ("cost 10", r10, AMBER))):
        y = 620 + i * 110
        label(d, 100, y + 22, cost, c, 32, True)
        width = max(16, int(1500 * int(rounds) / int(r10)))
        d.rounded_rectangle([300, y, 300 + width, y + 70], radius=10, fill=BG_CELL, outline=c, width=3)
        label(d, 300 + width + 20 if width < 1300 else 320, y + 18, "%s rounds per guess" % rounds, c, 30, True)
    caption(d, "Cost 10 is %s times the work. Slow helps; a weak password still falls." % times, AMBER, 880)


def migration(img, d, cfg):
    """Erin's {bcrypt} text checks, is flagged for upgrade, and is stored again as Argon2."""
    old = after("FIVE", "BEFORE THE LOGIN", "erin's stored text")
    login = part("FIVE", "THE LOGIN: ERIN TYPES")
    new = part("FIVE", "AFTER THE LOGIN")
    header(d, 100, 210, "BEFORE THE LOGIN", GREEN)
    cell(d, 100, 275, 560, 72, old, CLIENT, 28)
    label(d, 100, 362, "default now: argon2", MUTED, 26)
    draw.arrow_right(d, 680, 311, 780, MUTED, 3)
    header(d, 800, 210, "THE LOGIN: ERIN TYPES letmein", AMBER)
    for i, ln in enumerate(login):
        d.rounded_rectangle([800, 275 + i * 90, 1820, 345 + i * 90], radius=12, fill=PANEL, outline=AMBER, width=2)
        label(d, 830, 293 + i * 90, ln, TEXT, 28, True)
    draw.arrow(d, 1300, 460, 1300, 520, MUTED, 3)
    header(d, 100, 540, "AFTER THE LOGIN: STORED AGAIN", ROSE)
    segments(d, 100, 605, [(new[0], GREEN, "the new id"), (new[1], CYAN, "Argon2's settings, then salt and hash")], 28)
    label(d, 100, 760, new[2], GREEN, 32, True)
    caption(d, "Nobody reset a password. Customers who come back are upgraded one by one.", AMBER, 880)


def the_bill(img, d, cfg):
    """A planted {noop} value, and the memory Argon2 needs."""
    planted = after("SIX", "THE ATTACK", "erin's stored text")
    deflt = after("SIX", "THE DEFAULT ENCODER", "letmein matches:").split("  <-")[0]
    strict = part("SIX", "THE ALLOW-LIST ENCODER")[0]
    one = after("SIX", "THE MEMORY", "one argon2 hash")
    many = after("SIX", "THE MEMORY", "100 logins at once").split("  <-")[0]
    header(d, 100, 210, "THE ATTACK: A VALUE WRITTEN INTO THE TABLE", ROSE)
    cell(d, 100, 275, 440, 76, planted, ROSE, 32)
    label(d, 320, 362, "erin's row, written by an attacker", MUTED, 22, False, "ma")
    for i, (name, result, c) in enumerate((("default encoder", "matches: " + deflt + "   no hashing at all", ROSE),
                                           ("allow-list: bcrypt, argon2", strict, GREEN))):
        y = 260 + i * 110
        draw.arrow_right(d, 560, 313, 680, MUTED, 3) if i == 0 else None
        d.rounded_rectangle([700, y, 1820, y + 90], radius=12, fill=PANEL, outline=c, width=3)
        label(d, 730, y + 14, name, c, 28, True)
        label(d, 730, y + 52, result, TEXT, 26)
    header(d, 100, 520, "THE MEMORY: ARGON2'S PRICE", AMBER)
    mib = int(re.search(r"= (\d+) MiB", one).group(1))
    total = int(re.search(r"(\d+) MiB", many).group(1))
    label(d, 100, 600, "one hash", TEXT, 30, True)
    d.rounded_rectangle([420, 590, 420 + max(14, 1300 * mib // total), 650], radius=8, fill=AMBER)
    label(d, 460, 600, one, AMBER, 30, True)
    label(d, 100, 700, "100 logins at once", TEXT, 30, True)
    d.rounded_rectangle([420, 690, 1720, 750], radius=8, fill=BG_CELL, outline=AMBER, width=3)
    label(d, 1070, 720, many, AMBER, 32, True, "mm")
    caption(d, "Accept only the algorithms you chose, and size the servers for the memory.", AMBER, 860)
