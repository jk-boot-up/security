"""Pictures for the hashing and integrity video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("BEFORE HASHING")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The value after a label, with any continuation lines (hex over two or three lines) joined."""
    m = re.search(r"^    " + re.escape(label) + r"\s+(.+)\n((?: {15}.+\n?)*)", section(act, part), re.M)
    return (m.group(1) + " " + " ".join(m.group(2).split())).strip()


def after(label, act, part=None):
    """The text after a label and a colon: 'refused', 'OK, REPLAYED'."""
    m = re.search(re.escape(label) + r":?\s+(\S.*?)(?:  <-.*)?$", section(act, part), re.M)
    return m.group(1).strip()


def hexdigits(text):
    return re.sub(r"[^0-9a-f]", "", text.split("...")[0])


def nbytes(text):
    h = hexdigits(text)
    return [h[i:i + 2] for i in range(0, len(h), 2)]


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE HASHING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def cells(d, x, y, items, colour, w=88, h=72, size=34, mark=(), mark_colour=ROSE, gap=8):
    """A row of cells. `mark` holds the indexes to highlight."""
    for i, t in enumerate(items):
        c = mark_colour if i in mark else colour
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=8, fill=BG_CELL, outline=c, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=c, anchor="mm")
    return x + len(items) * (w + gap) - gap


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def node(d, x, y, w, h, text, c, size=26, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour, size=40):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (16 if sub else 0)), text, font=font(SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 26), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def mark(d, x, y, ok, size=56):
    d.text((x, y), "✓" if ok else "✗", font=font(SANS_B, size), fill=GREEN if ok else ROSE, anchor="mm")


# ------------------------------------------------------------------ figures

def hash_one(img, d, cfg):
    """SHA-256 turns one invoice, of any length, into 32 bytes."""
    text = value("invoice 1", "TWO", "BEFORE HASHING")
    digest = nbytes(value("hash 1", "TWO", "AFTER HASHING"))
    e = header(d, 100, 210, "BEFORE HASHING", GREEN)
    label(d, e + 24, 232, "the invoice: %d characters, %d bytes" % (len(text), len(text)), TEXT, 30, True, "lm")
    cells(d, 100, 272, list(text), GREEN, w=64, h=64, size=32, gap=6)
    cells(d, 100, 342, ["%02x" % ord(c) for c in text], MUTED, w=64, h=36, size=20, gap=6)
    draw.arrow(d, 960, 386, 960, 440, CYAN, 4)
    big_box(d, 720, 442, 480, 100, "SHA-256", "mixes every byte into every output byte", CYAN)
    draw.arrow(d, 960, 546, 960, 596, CYAN, 4)
    e = header(d, 100, 598, "AFTER HASHING", ROSE)
    label(d, e + 24, 620, "the hash: always 32 bytes, 64 hex digits", TEXT, 30, True, "lm")
    for row in range(2):
        cells(d, 100, 660 + row * 82, digest[row * 16:row * 16 + 16], ROSE, w=100, h=70, size=32, gap=8)
    caption(d, "Same input  →  always the same hash.   Any other input  →  a completely different one.", AMBER, 840)
    label(d, W // 2, 895, "Nothing secret goes in: anyone can work it out, and nobody can turn it back.", MUTED, 30, False, "ma")


def crc_check(img, d, cfg):
    """A CRC catches a flipped bit, but an attacker simply stores a new one."""
    rows = [("BEFORE THE CHANGE", GREEN, None),
            ("AFTER AN ACCIDENT", AMBER, (True, "the CRC no longer matches the stored %s: the damage is detected")),
            ("AFTER AN ATTACK", ROSE, (False, "the attacker stores the new CRC too: it matches, so the forgery passes"))]
    stored = value("CRC-32", "ONE", "BEFORE THE CHANGE")
    for i, (part, c, verdict) in enumerate(rows):
        y = 205 + i * 215
        e = header(d, 100, y, part, c)
        note = re.search(r"^  %s \((.*)\):" % part, section("ONE"), re.M).group(1)
        label(d, e + 20, y + 22, note, MUTED, 26, False, "lm")
        text, crc = value("invoice", "ONE", part), value("CRC-32", "ONE", part).split()[0]
        node(d, 100, y + 60, 660, 70, text, c, 32)
        draw.arrow_right(d, 770, y + 95, 850, MUTED, 3)
        node(d, 860, y + 60, 200, 70, "CRC-32", CYAN, 28, False)
        draw.arrow_right(d, 1070, y + 95, 1150, MUTED, 3)
        node(d, 1160, y + 60, 260, 70, crc, c, 34)
        if verdict:
            ok, words = verdict
            mark(d, 1480, y + 95, ok)
            label(d, 1520, y + 95, "DETECTED" if ok else "PASSES", GREEN if ok else ROSE, 34, True, "lm")
            label(d, 100, y + 150, words.replace("%s", stored), TEXT, 26)
        else:
            label(d, 1460, y + 95, "stored beside the invoice", MUTED, 28, False, "lm")
    caption(d, "A CRC has no secret: anyone can work one out, the attacker too.", AMBER, 875)


def avalanche(img, d, cfg):
    """One digit changed in the invoice: almost every digit of the hash changes."""
    t1, t2 = value("invoice 1", "TWO"), value("invoice 2", "TWO")
    h1, h2 = hexdigits(value("hash 1", "TWO")), hexdigits(value("hash 2", "TWO"))
    digits, bits = re.search(r"(\d+) of the 64 hex digits differ, (\d+) of the 256 bits", OUT).groups()
    same = {i for i in range(64) if h1[i] == h2[i]}
    header(d, 100, 205, "BEFORE HASHING   (one digit apart)", GREEN)
    for i, (name, t, c) in enumerate((("invoice 1", t1, GREEN), ("invoice 2", t2, CLIENT))):
        y = 265 + i * 70
        label(d, 100, y + 30, name, c, 28, True, "lm")
        cells(d, 330, y, list(t), c, w=56, h=60, size=30, gap=6,
              mark={k for k in range(len(t)) if t1[k] != t2[k]}, mark_colour=AMBER)
    header(d, 100, 420, "AFTER HASHING   (64 hex digits each)", ROSE)
    for i, (name, h, c) in enumerate((("hash 1", h1, GREEN), ("hash 2", h2, CLIENT))):
        for row in range(2):
            y = 478 + i * 130 + row * 58
            if row == 0:
                label(d, 100, y + 52, name, c, 28, True, "lm")
            cells(d, 330, y, list(h[row * 32:row * 32 + 32]), c, w=42, h=50, size=28, gap=4,
                  mark={k - row * 32 for k in same if row * 32 <= k < row * 32 + 32}, mark_colour=AMBER)
    caption(d, "%s of the 64 hex digits differ  ·  %s of the 256 bits: about half" % (digits, bits), AMBER, 760)
    label(d, W // 2, 815, "The %d amber digits match only by chance. Nothing of the old hash is left to guess from."
          % len(same), MUTED, 30, False, "ma")
    label(d, W // 2, 865, "A line or a whole book: the hash is always 64 hex digits.", MUTED, 30, False, "ma")


def hash_beside(img, d, cfg):
    """A hash next to the data is rewritten with it; a hash that comes by another road is not."""
    cols = [("BEFORE THE ATTACK", GREEN, None), ("AFTER ATTACK 1", AMBER, True), ("AFTER ATTACK 2", ROSE, False)]
    for i, (part, c, caught) in enumerate(cols):
        x = 100 + i * 590
        header(d, x, 205, part, c, 540)
        d.rounded_rectangle([x, 265, x + 540, 495], radius=16, fill=PANEL, outline=LINE, width=2)
        inv, h = value("invoice", "THREE", part), value("hash", "THREE", part).split("...")[0]
        label(d, x + 20, 280, "invoice", MUTED, 24)
        node(d, x + 20, 312, 500, 58, inv, TEXT if caught is None else c, 26)
        label(d, x + 20, 385, "hash beside it", MUTED, 24)
        node(d, x + 20, 417, 500, 58, h + "…", TEXT if i < 2 else c, 28)
        if caught is not None:
            mark(d, x + 40, 535, caught, 48)
            label(d, x + 75, 535, "intact? %s: %s" % (str(not caught).lower(), "CAUGHT" if caught else "NOT CAUGHT"),
                  GREEN if caught else ROSE, 30, True, "lm")
    header(d, 100, 600, "A HASH BY ANOTHER ROAD   (the shop's installer)", CYAN)
    site = after("on the shop's own site, over HTTPS", "THREE").split("...")[0]
    mirror = after("the file from a mirror hashes to", "THREE").split("...")[0]
    for i, (where, h, c) in enumerate((("the shop's own site, over HTTPS", site, GREEN),
                                       ("the file from a mirror, hashed", mirror, ROSE))):
        y = 665 + i * 85
        node(d, 100, y, 560, 64, where, c, 28, False)
        draw.arrow_right(d, 670, y + 32, 760, c, 3)
        node(d, 770, y, 360, 64, h + "…", c, 30)
    d.text((1200, 747), "≠", font=font(SANS_B, 80), fill=AMBER, anchor="mm")
    draw.arrow_right(d, 1250, 747, 1340, MUTED, 3)
    verdict = re.search(r"^    (accepted|refused)", section("THREE"), re.M).group(1)
    mark(d, 1385, 747, verdict == "refused", 52)
    label(d, 1420, 747, verdict, GREEN if verdict == "refused" else ROSE, 36, True, "lm")
    caption(d, "A hash only helps when the attacker cannot reach it.", AMBER, 875)


def hmac_tag(img, d, cfg):
    """HMAC: the key and the invoice bytes go in, a 32-byte tag comes out; only the key holder can make it."""
    key, inv = value("key", "FOUR"), value("as bytes", "FOUR")
    n = len(nbytes(inv))
    tag = value("tag", "FOUR", "AFTER TAGGING")
    header(d, 100, 205, "BEFORE TAGGING", GREEN)
    label(d, 100, 290, "key (secret)", AMBER, 28, True, "lm")
    node(d, 420, 262, 760, 56, key, AMBER, 26)
    label(d, 100, 358, "invoice bytes", CLIENT, 28, True, "lm")
    node(d, 420, 330, 760, 56, " ".join(inv.split()[:4]) + "…  (%d bytes)" % n, CLIENT, 24)
    draw.arrow_right(d, 1190, 290, 1265, AMBER, 3)
    draw.arrow_right(d, 1190, 358, 1265, CLIENT, 3)
    big_box(d, 1270, 255, 340, 140, "HMAC-SHA-256", "the key mixed in", CYAN, 34)
    draw.arrow(d, 1440, 398, 1440, 452, CYAN, 4)
    header(d, 100, 455, "AFTER TAGGING", ROSE)
    node(d, 420, 455, 1420, 56, tag, ROSE, 26)
    header(d, 100, 545, "THE CHECKS", AMBER)
    checks = [("the invoice with its tag", "the invoice with its tag"),
              ("amount changed to 12, the old tag", "amount changed to 12, old tag"),
              ("a plain SHA-256 used as the tag", "a plain SHA-256 as the tag"),
              ("an HMAC made with a guessed key", "HMAC made with a guessed key")]
    for i, (words, line) in enumerate(checks):
        y = 605 + i * 70
        result = after(line, "FOUR", "THE CHECKS")
        ok = result == "OK"
        label(d, 100, y + 30, words, TEXT, 30, False, "lm")
        node(d, 1000, y, 260, 60, result, GREEN if ok else ROSE, 30)
        mark(d, 1320, y + 30, ok, 48)
        label(d, 1360, y + 30, "accepted" if ok else "refused", GREEN if ok else ROSE, 28, True, "lm")
    caption(d, "Without the key, nobody can make a tag that checks out.", AMBER, 900)


def hmac_steps(img, d, cfg):
    """How the HMAC tag is made: the key goes in twice, around two SHA-256 hashes."""
    act = "FIVE"
    short = lambda v, k=2: " ".join(v.split()[:k]) + "…"
    key, ik = value("key", act), value("inner key", act)
    ih, ok_, tag = value("inner hash", act), value("outer key", act), value("tag", act)
    inv = value("as bytes", "FOUR")
    same = re.search(r"act four: (\w+)", section(act)).group(1)
    steps = [("STEP 1", "inner key = the key (padded with zeros to 64 bytes) ⊕ 36 36 36 …", AMBER),
             ("STEP 2", "inner hash = SHA-256( inner key + invoice bytes )", CYAN),
             ("STEP 3", "outer key = the key ⊕ 5c 5c 5c …", AMBER),
             ("STEP 4", "tag = SHA-256( outer key + inner hash )", GREEN)]
    for i, (pill, words, c) in enumerate(steps):
        y = 205 + i * 150
        e = header(d, 100, y, pill, c)
        label(d, e + 20, y + 22, words, TEXT, 28, True, "lm")
    def xor_row(y, pad, name, val, c):
        node(d, 100, y, 420, 60, "key " + short(key), AMBER, 24)
        d.text((560, y + 30), "⊕", font=font(SANS_B, 40), fill=TEXT, anchor="mm")
        node(d, 600, y, 260, 60, pad, MUTED, 24)
        d.text((900, y + 30), "=", font=font(SANS_B, 40), fill=TEXT, anchor="mm")
        node(d, 940, y, 560, 60, name + " " + short(val), c, 24)
    def hash_row(y, a, b, name, val, c):
        node(d, 100, y, 420, 60, a, CYAN, 22)
        d.text((560, y + 30), "+", font=font(SANS_B, 40), fill=TEXT, anchor="mm")
        node(d, 600, y, 420, 60, b, CLIENT, 22)
        draw.arrow_right(d, 1030, y + 30, 1100, MUTED, 3)
        node(d, 1105, y, 220, 60, "SHA-256", CYAN, 26, False)
        draw.arrow_right(d, 1335, y + 30, 1400, MUTED, 3)
        node(d, 1405, y, 440, 60, name + " " + short(val), c, 22)
    xor_row(262, "36 36 36 …", "inner key", ik, CYAN)
    hash_row(412, "inner key " + short(ik, 1), "invoice " + short(inv), "inner hash", ih, CYAN)
    xor_row(562, "5c 5c 5c …", "outer key", ok_, AMBER)
    hash_row(712, "outer key " + short(ok_, 1), "inner hash " + short(ih, 1), "tag", tag, GREEN)
    caption(d, "The same tag Java's Mac gave in act four: %s" % same, GREEN, 820)
    label(d, W // 2, 875, "The key goes in twice. Without it, neither hash can be made.", MUTED, 30, False, "ma")


def bill(img, d, cfg):
    """Three things around a good HMAC: boundaries, repeats, and a shared key."""
    rows = re.findall(r"^    (\(\w+, \w+\))\s+bytes (\S+)\s+tag (\w+)", section("SIX"), re.M)
    fixed = re.findall(r"^    (\(\w+, \w+\))\s+tag (\w+)", section("SIX"), re.M)
    moved_before = after("boundary moved, old tag", "SIX", "BEFORE THE FIX (fields")
    moved_after = after("boundary moved, old tag", "SIX", "AFTER THE FIX (each")
    header(d, 100, 205, "BEFORE THE FIX   (fields joined, nothing between)", ROSE)
    for i, (name, data, tag) in enumerate(rows):
        y = 262 + i * 74
        label(d, 100, y + 30, name, TEXT, 30, True, "lm")
        fields = name.strip("()").split(", ")
        cells(d, 300, y, list(data), CLIENT, w=50, h=60, size=30, gap=6)
        cuts = [5, 5 + len(fields[0]), 5 + len(fields[0]) + len(fields[1])]
        for k in cuts:
            bx = 300 + k * 56 - 3
            d.rectangle([bx - 2, y - 6, bx + 2, y + 66], fill=AMBER)
        draw.arrow_right(d, 820, y + 30, 900, MUTED, 3)
        node(d, 910, y, 300, 60, tag, ROSE, 28)
    mark(d, 1290, 336 + 30, False, 48)
    label(d, 1330, 366, "moved, old tag: %s" % moved_before, ROSE, 30, True, "lm")
    label(d, 1250, 292, "the same bytes, the same tag", MUTED, 26, False, "lm")
    header(d, 100, 420, "AFTER THE FIX   (each field with its length in front)", GREEN)
    for i, (name, tag) in enumerate(fixed):
        y = 476 + i * 70
        label(d, 100, y + 28, name, TEXT, 30, True, "lm")
        node(d, 910, y, 300, 56, tag, GREEN, 28)
    mark(d, 1290, 546 + 28, True, 48)
    label(d, 1330, 574, "moved, old tag: %s" % moved_after, GREEN, 30, True, "lm")
    label(d, 300, 504, "different bytes, so different tags", MUTED, 26, False, "lm")
    header(d, 100, 640, "THE SAME INVOICE SENT TWICE", AMBER)
    forgets = after("one invoice sent twice", "SIX", "BEFORE THE FIX (the service")
    remembers = after("one invoice sent twice", "SIX", "AFTER THE FIX (it")
    label(d, 100, 720, "a service that forgets:  " + forgets, ROSE, 30, True, "lm")
    label(d, 100, 770, "a service that remembers numbers:  " + remembers, GREEN, 30, True, "lm")
    label(d, 1000, 745, "a tag has no date", MUTED, 28, False, "lm")
    header(d, 100, 815, "THE SHARED KEY   (no fix with HMAC)", CYAN)
    made = after("it tags INV-9000, a laptop for 1 dollar", "SIX")
    label(d, 100, 895, "the warehouse holds the key, and tags INV-9000, a laptop for 1 dollar:  " + made,
          ROSE, 30, True, "lm")
    label(d, 100, 945, "Key holders look alike. Proving who made a tag needs a signature.", MUTED, 28, False, "lm")
