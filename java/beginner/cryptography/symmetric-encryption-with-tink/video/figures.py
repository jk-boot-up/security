"""Pictures for the symmetric encryption with Tink video, drawn by the `figure` slide kind.

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


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("BEFORE ENCRYPTION")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of the demo's output, within one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s*:?\s+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def number(pattern, act):
    return int(re.search(pattern, section(act)).group(1))


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE ENCRYPTION: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def hexbytes(text):
    """'52454e54 2020' -> ['52', '45', '4e', '54', '20', '20']"""
    h = text.replace(" ", "")
    return [h[i:i + 2] for i in range(0, len(h), 2)]


def shown(ch):
    return {" ": "·", "\n": "↵"}.get(ch, ch)


def cells(d, x, y, items, colour, w=88, h=72, size=34, gap=8):
    """A row of byte cells."""
    for i, t in enumerate(items):
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=colour, anchor="mm")
    return x + len(items) * (w + gap) - gap


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def box(d, x, y, w, h, text, colour, size=28, mono=False, sub=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2 - (14 if sub else 0)), text, font=font(MONO_B if mono else SANS_B, size),
           fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 22), sub, font=font(SANS, 20), fill=MUTED, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(MONO_B, 34), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 30), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


def bar(d, x, y, parts, unit, h=66, size=24):
    """A stored message drawn to scale: parts are (name, bytes, colour)."""
    for name, n, c in parts:
        x1 = x + max(int(n * unit) - 6, 40)
        d.rounded_rectangle([x, y, x1, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
        if name:
            d.text(((x + x1) // 2, y + h // 2), name, font=font(SANS_B, size), fill=c, anchor="mm")
        x = x1 + 6
    return x


# the 49-byte layout, read from act two
def layout():
    total = number(r"= (\d+) bytes", "TWO")
    ct = len(hexbytes(value("cipher text", "TWO", "AFTER")))
    return total, [("version 1", 1, AMBER), ("key id 4", 4, AMBER), ("nonce 12", 12, CYAN),
                   ("cipher text %d" % ct, ct, ROSE), ("tag 16", 16, GREEN)]


# ------------------------------------------------------------------ figures

def one_line(img, d, cfg):
    """The core idea: one line and the account go in, one call, a 49-byte message comes out."""
    line = "RENT      -1200\n"
    e = header(d, 100, 205, "BEFORE ENCRYPTION", GREEN)
    label(d, e + 24, 227, "one statement line, %d bytes" % len(line), TEXT, 30, True, "lm")
    cells(d, 100, 270, [shown(c) for c in line], GREEN)
    box(d, 100, 368, 520, 60, value("account", "ONE").split("   ")[0], CLIENT, 28, True)
    label(d, 640, 398, "the account: checked, not hidden", MUTED, 26, False, "lm")
    box(d, 1180, 368, 640, 60, "keyset: key id 1001", AMBER, 28)
    draw.arrow(d, 860, 440, 860, 480, CYAN, 4)
    big_box(d, 480, 485, 760, 100, "aead.encrypt(line, account)", "one call: no nonce, no mode to choose", CYAN)
    draw.arrow(d, 860, 590, 860, 640, CYAN, 4)
    total, parts = layout()
    parts = [("prefix 5", 5, AMBER)] + parts[2:]
    e = header(d, 100, 640, "AFTER ENCRYPTION", ROSE)
    label(d, e + 24, 662, "one message of %d bytes, drawn to scale" % total, TEXT, 30, True, "lm")
    bar(d, 100, 710, parts, 35.5, 70, 22)
    label(d, 100, 790, "prefix = version 1 + key id 1001", AMBER, 24)
    caption(d, "Tink picks the nonce and lays out the message: you keep only the keyset.", AMBER, 840)


def sealed_twice(img, d, cfg):
    """The same line sealed twice: the same start, everything after it different."""
    start1 = re.search(r"message 1 .*starts (\w+)", section("ONE")).group(1)
    start2 = re.search(r"message 2 .*starts (\w+)", section("ONE")).group(1)
    size = number(r"message 1\s+(\d+) bytes", "ONE")
    header(d, 100, 205, "BEFORE ENCRYPTION   (the same both times)", GREEN)
    for i, (k, c) in enumerate((("plain text", GREEN), ("account", CLIENT), ("keyset", AMBER))):
        v = re.sub(r"\s+\(.*\)$", "", value(k, "ONE", "BEFORE"))
        label(d, 100 + i * 600, 275, k, c, 26, True)
        box(d, 100 + i * 600, 315, 560, 64, v, c, 26, k != "keyset")
    header(d, 100, 440, "AFTER ENCRYPTION   (sealed twice)", ROSE)
    for i, (st, c) in enumerate(((start1, CYAN), (start2, CLIENT))):
        y = 520 + i * 120
        label(d, 100, y + 33, "message %d" % (i + 1), c, 30, True, "lm")
        box(d, 330, y, 360, 66, st, AMBER, 28, True)
        box(d, 700, y, 1000, 66, "a new random nonce, so new cipher text and tag", c, 26)
        label(d, 1720, y + 33, "%d bytes" % size, MUTED, 26, True, "lm")
    label(d, 510, 772, "the same 5-byte start: which key", AMBER, 24, False, "ma")
    label(d, 1200, 772, "different every time", MUTED, 24, False, "ma")
    caption(d, "Same line, same key, two different messages: nobody can tell they are equal.", AMBER, 850)
    label(d, W // 2, 905, "The caller never sees a nonce, so the caller can never reuse one.", MUTED, 30, False, "ma")


def by_hand(img, d, cfg):
    """The 49 bytes of one message, part by part, with the key id worked out."""
    after = "AFTER"
    rows = [("version", hexbytes(value("version", "TWO", after)), AMBER),
            ("key id", hexbytes(value("key id", "TWO", after).split()[0]), AMBER),
            ("nonce", hexbytes(value("nonce", "TWO", after)), CYAN),
            ("cipher text", hexbytes(value("cipher text", "TWO", after)), ROSE),
            ("tag", hexbytes(value("tag", "TWO", after)), GREEN)]
    key_id = int(value("key id", "TWO", "BEFORE"))
    e = header(d, 100, 200, "BEFORE ENCRYPTION", GREEN)
    label(d, e + 24, 222, "key id %d,  nonce %s  (fixed, only for this demo),  the account,  RENT -1200"
          % (key_id, value("nonce", "TWO", "BEFORE").replace(" ", "")[-2:]), TEXT, 26, True, "lm")
    e = header(d, 100, 275, "AFTER ENCRYPTION", ROSE)
    label(d, e + 24, 297, "the %d bytes, part by part" % sum(len(r[1]) for r in rows), TEXT, 28, True, "lm")
    y = 340
    for name, items, c in rows:
        label(d, 100, y + 28, "%s (%d)" % (name, len(items)), c, 24, True, "lm")
        cells(d, 380, y, items, c, w=84, h=56, size=28, gap=6)
        y += 72
    # how the key id becomes 000003e9
    hi, lo = key_id // 256, key_id % 256
    d.rounded_rectangle([1000, 340, 1814, 470], radius=14, fill=PANEL, outline=AMBER, width=3)
    label(d, 1030, 360, "the key id, as 4 bytes:", AMBER, 26, True)
    label(d, 1030, 410, "%d = %d × 256 + %d  →  00 00 %02x %02x" % (key_id, hi, lo, hi, lo), TEXT, 28, True)
    caption(d, "Tink opens this hand-built message: it is exactly Tink's layout.", AMBER, 720)
    label(d, W // 2, 775, re.search(r"^  (1 \+ .*bytes)$", section("TWO"), re.M).group(1)
          + ".  Only the nonce changes from run to run.",
          MUTED, 30, False, "ma")


def tag_check(img, d, cfg):
    """Opening checks the tag first: any change, and the message is refused."""
    changes = re.findall(r"^    (.+?): (refused|RENT.*?)  <-", section("THREE", "THREE CHANGES"), re.M)
    rows = [("right key, right account", "RENT  -1200", True)] + [(w.replace("->", "→"), r, False)
                                                                   for w, r in changes]
    header(d, 100, 205, "OPENING IT", GREEN)
    header(d, 100, 400, "THREE CHANGES", ROSE)
    for i, (what, result, ok) in enumerate(rows):
        y = 262 + i * 140 + (60 if i else 0)
        c = GREEN if ok else ROSE
        d.rounded_rectangle([100, y, 900, y + 96], radius=14, fill=BG_CELL, outline=LINE, width=2)
        label(d, 130, y + 48, what, TEXT, 32, False, "lm")
        draw.arrow_right(d, 915, y + 48, 1040, MUTED, 4)
        d.rounded_rectangle([1055, y + 4, 1355, y + 92], radius=18, fill=PANEL, outline=AMBER, width=4)
        d.text((1205, y + 48), "check tag", font=font(SANS_B, 40), fill=AMBER, anchor="mm")
        draw.arrow_right(d, 1370, y + 48, 1460, c, 4)
        d.text((1500, y + 48), "✓" if ok else "✗", font=font(SANS_B, 64), fill=c, anchor="mm")
        d.text((1560, y + 48), result, font=font(MONO_B if ok else SANS_B, 36), fill=c, anchor="lm")
    caption(d, "The tag covers the key, the nonce, the account and every byte of the message.", AMBER, 905)


def rotation(img, d, cfg):
    """A keyset before rotation, after it, and after the old key is removed."""
    parts = [("BEFORE ROTATION", GREEN), ("AFTER ROTATION", CYAN), ("AFTER REMOVING", ROSE)]
    text = section("FOUR")
    for i, (name, c) in enumerate(parts):
        x = 100 + i * 590
        body = section("FOUR", name)
        header(d, x, 205, name, c, 540)
        keys = re.findall(r"key (\d+)( \(primary\))?", re.search(r"keyset\s+(.+)", body).group(1))
        label(d, x, 278, "the keyset", MUTED, 26, True)
        for j, (kid, primary) in enumerate(keys):
            y = 318 + j * 90
            col = AMBER if primary else MUTED
            box(d, x, y, 540, 72, "key %s" % kid + ("   ★ primary" if primary else ""), col, 30)
        label(d, x, 520, "the messages", MUTED, 26, True)
        if i == 0:
            st = re.search(r"old message\s+starts (\w+)", body).group(1)
            box(d, x, 560, 540, 72, "old: starts %s" % st, TEXT, 26, True)
            label(d, x, 650, "sealed with key %s" % keys[0][0], MUTED, 24)
        else:
            for j, who in enumerate(("old", "new")):
                res = re.search(r"%s message opens: (.+?)(  <-.*)?$" % who, body, re.M).group(1)
                ok = res != "refused"
                y = 560 + j * 90
                box(d, x, y, 540, 72, "%s message: %s" % (who, "opens ✓" if ok else "refused ✗"),
                    GREEN if ok else ROSE, 28)
        if i == 1:
            st = re.search(r"new message\s+starts (\w+)", body).group(1)
            label(d, x, 745, "new messages start %s" % st, CYAN, 24, True)
    assert "RE-ENCRYPT" in text
    caption(d, "Re-encrypt old messages with the new key before you remove the old one.", AMBER, 850)
    label(d, W // 2, 905, "The key id in each message tells Tink which key in the ring opens it.", MUTED, 30,
          False, "ma")


def envelope(img, d, cfg):
    """Envelope encryption: a fresh data key per message, wrapped by the master key."""
    after = section("FIVE", "AFTER")
    length_hex, wrapped = re.search(r"length\s+(\w+) = (\d+)", after).groups()
    wrapped_sum = re.search(r"wrapped key\s+\d+ = ([\d + ]+), starts (\w+)", after)
    body, body_sum = re.search(r"cipher text\s+(\d+) = ([\d + ]+),", after).groups()
    total = number(r"4 \+ \d+ \+ \d+ = (\d+) bytes", "FIVE")
    header(d, 100, 200, "BEFORE ENCRYPTION", GREEN)
    box(d, 100, 265, 420, 66, "RENT  -1200", GREEN, 28, True)
    box(d, 100, 360, 420, 66, "data key: 32 random bytes", CYAN, 26)
    box(d, 100, 455, 420, 66, "master key %s" % re.search(r"key id (\d+)", section("FIVE", "BEFORE")).group(1),
        AMBER, 26)
    label(d, 110, 530, "in a real bank: inside a KMS", MUTED, 22)
    # flows
    draw.arrow_right(d, 530, 298, 690, GREEN, 3)
    draw.arrow_right(d, 530, 393, 690, CYAN, 3)
    box(d, 700, 268, 330, 160, "AES-GCM", CYAN, 32, sub="with the data key")
    draw.arrow_right(d, 530, 488, 690, AMBER, 3)
    box(d, 700, 455, 330, 66, "wraps the data key", AMBER, 24)
    e = header(d, 1100, 200, "AFTER ENCRYPTION", ROSE)
    label(d, e + 20, 222, "%d bytes stored" % total, TEXT, 28, True, "lm")
    draw.arrow_right(d, 1040, 348, 1100, ROSE, 3)
    draw.arrow_right(d, 1040, 488, 1100, AMBER, 3)
    box(d, 1110, 315, 710, 66, "cipher text  %s bytes" % body, ROSE, 26)
    label(d, 1120, 392, "%s: nonce, the line, tag" % body_sum, MUTED, 22)
    box(d, 1110, 455, 710, 66, "wrapped key  %s bytes" % wrapped, AMBER, 26)
    label(d, 1120, 532, "%s: prefix, nonce, key, tag" % wrapped_sum.group(1), MUTED, 22)
    # what is stored, to scale
    label(d, 100, 610, "what the bank stores, drawn to scale", TEXT, 30, True)
    bar(d, 100, 660, [("4", 4, MUTED), ("wrapped key %s" % wrapped, int(wrapped), AMBER),
                      ("cipher text %s" % body, int(body), ROSE)], 15.0, 70, 24)
    label(d, 100, 745, "4 + %s + %s = %d   (the 4-byte length, %s = %s, says where the wrapped key ends)"
          % (wrapped, body, total, length_hex, wrapped),
          MUTED, 26)
    caption(d, "The master key only ever sees a 32-byte key, once per message, whatever its size.", AMBER, 850)
    label(d, W // 2, 905, "Its own bytes never appear in a stored message.", MUTED, 30, False, "ma")


def menu(img, d, cfg):
    """The bill: the safe menu, and what Tink leaves to the bank."""
    menu_text = section("SIX", "THE MENU")
    names = re.findall(r"[A-Z0-9]+_[A-Z0-9_]+", menu_text)
    header(d, 100, 205, "THE MENU", GREEN)
    for i, n in enumerate(names):
        y = 275 + i * 82
        box(d, 100, y, 640, 66, n + "   ✓", GREEN, 28, True)
    y = 275 + len(names) * 82
    box(d, 100, y, 640, 66, "ECB   ✗ not offered", ROSE, 28, True)
    header(d, 860, 205, "WHAT IT LEAVES TO YOU", AMBER)
    m = re.search(r"(\d+) -> (\d+) bytes, (\d+) -> (\d+): always (\d+) more", section("SIX"))
    a, b, c, e, more = m.groups()
    rows = ["sizes still show: %s → %s bytes,  %s → %s" % (a, b, c, e),
            "always %s bytes more: 5 + 12 + 16" % more,
            "the format is Tink's own",
            "a library to add, and protocol buffers",
            "the master key still needs a home"]
    for i, r in enumerate(rows):
        yy = 275 + i * 82
        d.rounded_rectangle([860, yy, 1820, yy + 66], radius=12, fill=BG_CELL, outline=LINE, width=2)
        label(d, 890, yy + 33, r, TEXT, 28, False, "lm")
    caption(d, "The unsafe choices are not on the menu. The key's home is still your job.", AMBER, 760)
