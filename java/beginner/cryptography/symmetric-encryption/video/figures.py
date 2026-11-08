"""Pictures for the symmetric encryption video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CONSOLE, CYAN, GREEN, LINE, MONO, MONO_B,
                                   MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)


def value(label, act=None):
    """The text after a label on a line of the demo's output, optionally within one act."""
    text = section(act) if act else OUT
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", text, re.M)
    return m.group(1).strip()


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("BEFORE ENCRYPTION")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % part, text, re.M).group(1)
    return text


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


def cells(d, x, y, items, colour, w=88, h=72, size=34, mono=True, mark=(), mark_colour=ROSE, gap=8):
    """A row of byte cells. `mark` holds the indexes to highlight."""
    for i, t in enumerate(items):
        c = mark_colour if i in mark else colour
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")
    return x + len(items) * (w + gap) - gap


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def pill(d, x, y, w, h, text, colour, size=30, mono=False):
    d.rounded_rectangle([x, y, x + w, y + h], radius=h // 2, fill=PANEL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, 44), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 30), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


# ------------------------------------------------------------------ figures

def aes_block(img, d, cfg):
    """AES turns one 16-byte block into another, with a key."""
    line = "RENT      -1200\n"
    after = re.findall(r"line \d+  ([0-9a-f ]+)$", section("TWO", "AFTER ENCRYPTION"), re.M)[1]
    e = header(d, 100, 215, "BEFORE ENCRYPTION", GREEN)
    label(d, e + 24, 237, "plain text: one line, 16 bytes", TEXT, 30, True, "lm")
    cells(d, 100, 280, [shown(c) for c in line], GREEN)
    cells(d, 100, 358, ["%02x" % ord(c) for c in line], MUTED, h=44, size=24)
    draw.arrow(d, 860, 412, 860, 470, CYAN, 4)
    big_box(d, 620, 475, 480, 110, "AES", "scrambles all 16 bytes together", CYAN)
    pill(d, 1180, 497, 600, 66, "key: demo-key-16bytes", AMBER, 30)
    draw.arrow_left(d, 1178, 530, 1104, AMBER, 4)
    draw.arrow(d, 860, 590, 860, 640, CYAN, 4)
    e = header(d, 100, 640, "AFTER ENCRYPTION", ROSE)
    label(d, e + 24, 662, "cipher text: 16 bytes that look random", TEXT, 30, True, "lm")
    cells(d, 100, 705, hexbytes(after), ROSE, size=30)
    caption(d, "Same key and same block in  →  always the same block out", AMBER, 830)
    label(d, W // 2, 885, "Without the key, nobody can turn it back.", MUTED, 30, False, "ma")


def ecb_pattern(img, d, cfg):
    """ECB: every line encrypted on its own, so equal lines give equal blocks."""
    before = re.findall(r"line \d+  (.+)$", section("TWO", "BEFORE ENCRYPTION"), re.M)
    after = re.findall(r"line \d+  (.+)$", section("TWO", "AFTER ENCRYPTION"), re.M)
    colours = {}
    palette = [GREEN, AMBER, CYAN, CLIENT]
    for text in before:
        colours.setdefault(text, palette[len(colours)])
    header(d, 100, 210, "BEFORE ENCRYPTION", GREEN, 420)
    header(d, 710, 210, "AFTER ENCRYPTION", ROSE, 1100)
    for i, (text, block) in enumerate(zip(before, after)):
        y = 270 + i * 78
        c = colours[text]
        d.rounded_rectangle([100, y, 520, y + 62], radius=12, fill=BG_CELL, outline=c, width=3)
        d.text((310, y + 31), text, font=font(MONO_B, 32), fill=c, anchor="mm")
        draw.arrow_right(d, 540, y + 31, 690, MUTED, 3)
        d.text((615, y + 4), "AES", font=font(SANS_B, 20), fill=MUTED, anchor="ma")
        d.rounded_rectangle([710, y, 1810, y + 62], radius=12, fill=BG_CELL, outline=c, width=3)
        d.text((1260, y + 31), block, font=font(MONO_B, 32), fill=c, anchor="mm")
    caption(d, "3 colours in, 3 colours out: the pattern of the statement shows through", AMBER, 917)


def gcm_seal(img, d, cfg):
    """GCM: four inputs, two outputs, and what is stored."""
    key, nonce = value("key", "THREE"), value("nonce", "THREE")
    account, ct, tag = value("account", "THREE"), value("cipher text", "THREE"), value("tag", "THREE")
    header(d, 100, 205, "BEFORE ENCRYPTION: the inputs", GREEN, 660)
    ins = [("key", key, AMBER, "secret"), ("nonce", nonce, CYAN, "new every time"),
           ("account", account, CLIENT, "not hidden, but checked"), ("plain text", "RENT  -1200", GREEN, "the secret line")]
    for i, (name, val, c, note) in enumerate(ins):
        y = 262 + i * 110
        label(d, 100, y, name, c, 28, True)
        label(d, 330, y + 2, note, MUTED, 22)
        d.rounded_rectangle([100, y + 36, 760, y + 92], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((430, y + 64), val, font=font(MONO_B, 26), fill=c, anchor="mm")
        draw.arrow_right(d, 770, y + 64, 880, c, 3)
    big_box(d, 890, 340, 300, 250, "AES-GCM", "seal", TEXT)
    header(d, 1300, 205, "AFTER ENCRYPTION: the outputs", ROSE, 530)
    outs = [("cipher text", ct, ROSE, "the line, unreadable"), ("tag", tag, AMBER, "a check of everything")]
    for i, (name, val, c, note) in enumerate(outs):
        y = 320 + i * 160
        draw.arrow_right(d, 1200, y + 67, 1290, c, 3)
        label(d, 1300, y, name, c, 28, True)
        label(d, 1500, y + 2, note, MUTED, 22)
        d.rounded_rectangle([1300, y + 38, 1830, y + 96], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((1565, y + 67), val, font=font(MONO_B, 23), fill=c, anchor="mm")
    # what is stored, drawn to scale: 12 + 16 + 16 bytes
    label(d, 100, 728, "what the bank stores: 44 bytes", TEXT, 30, True)
    x, unit = 100, 38
    for name, n, c in (("nonce 12", 12, CYAN), ("cipher text 16", 16, ROSE), ("tag 16", 16, AMBER)):
        d.rounded_rectangle([x, 775, x + n * unit - 6, 840], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((x + (n * unit - 6) // 2, 807), name, font=font(SANS_B, 28), fill=c, anchor="mm")
        x += n * unit
    caption(d, "The key never leaves the bank. Everything else may be seen.", MUTED, 885)


def two_nonces(img, d, cfg):
    """The same line sealed twice, with two nonces, gives two unrelated cipher texts."""
    n1, n2 = value("nonce 1", "FIVE"), value("nonce 2", "FIVE")
    c1, c2 = value("with nonce 1", "FIVE"), value("with nonce 2", "FIVE")
    header(d, 100, 210, "BEFORE ENCRYPTION", GREEN)
    label(d, 100, 290, "plain text (the same both times)", MUTED, 26, True)
    d.rounded_rectangle([100, 330, 560, 400], radius=10, fill=BG_CELL, outline=GREEN, width=3)
    d.text((330, 365), "RENT  -1200", font=font(MONO_B, 34), fill=GREEN, anchor="mm")
    for i, (n, c) in enumerate(((n1, CYAN), (n2, CLIENT))):
        x = 700 + i * 570
        label(d, x, 290, "nonce %d" % (i + 1), c, 26, True)
        d.rounded_rectangle([x, 330, x + 520, 400], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((x + 260, 365), n, font=font(MONO_B, 26), fill=c, anchor="mm")
    header(d, 100, 480, "AFTER ENCRYPTION", ROSE)
    for i, (ct, c) in enumerate(((c1, CYAN), (c2, CLIENT))):
        y = 560 + i * 120
        label(d, 100, y + 35, "with nonce %d" % (i + 1), c, 30, True, "lm")
        cells(d, 380, y, hexbytes(ct), ROSE, w=82, h=70, size=28, gap=8)
    caption(d, "Same line, new nonce: nothing in common. Nobody can tell they are the same payment.", AMBER, 880)


def tag_check(img, d, cfg):
    """Opening checks the tag first: any change, and the message is refused."""
    rows = [("right key, right account", "RENT  -1200", True),
            ("one bit flipped: first byte 08 → 09", "refused", False),
            ("account changed to account=MAL-2", "refused", False),
            ("opened with another key", "refused", False)]
    header(d, 100, 205, "OPENING", GREEN)
    header(d, 100, 400, "THREE CHANGES", ROSE)
    for i, (what, result, ok) in enumerate(rows):
        y = 262 + i * 140 + (60 if i else 0)
        c = GREEN if ok else ROSE
        d.rounded_rectangle([100, y, 900, y + 96], radius=14, fill=BG_CELL, outline=LINE, width=2)
        label(d, 130, y + 48, what, TEXT, 32, False, "lm")
        draw.arrow_right(d, 915, y + 48, 1040, MUTED, 4)
        big_box(d, 1055, y + 4, 300, 88, "check tag", "", AMBER)
        draw.arrow_right(d, 1370, y + 48, 1460, c, 4)
        d.text((1500, y + 48), "✓" if ok else "✗", font=font(SANS_B, 64), fill=c, anchor="mm")
        d.text((1560, y + 48), result, font=font(MONO_B if ok else SANS_B, 36), fill=c, anchor="lm")
    caption(d, "The tag covers the key, the nonce, the account and every bit of the cipher text.", AMBER, 905)


def nonce_reuse(img, d, cfg):
    """With one nonce twice, the secret keystream cancels out."""
    p1, p2 = "balance=000100", "balance=900100"
    c1, c2 = hexbytes(value("cipher text 1", "SEVEN")), hexbytes(value("cipher text 2", "SEVEN"))
    x12 = hexbytes(value("cipher 1 XOR cipher 2", "SEVEN"))
    diff = {i for i in range(len(p1)) if p1[i] != p2[i]}
    w, gap, x0, h = 86, 6, 470, 58
    groups = [("BEFORE ENCRYPTION   (same key, same nonce for both)", GREEN,
               [("plain text 1  (known)", list(p1), GREEN), ("plain text 2  (secret)", list(p2), CLIENT)]),
              ("AFTER ENCRYPTION", ROSE,
               [("cipher text 1", c1, ROSE), ("cipher text 2", c2, ROSE)]),
              ("THE ATTACK, WITHOUT THE KEY", AMBER,
               [("cipher 1 ⊕ cipher 2", x12, AMBER), ("⊕ plain text 1  =", list(p2), ROSE)])]
    y = 205
    for title, hc, rows in groups:
        header(d, 100, y, title, hc)
        y += 56
        for name, items, c in rows:
            label(d, 100, y + h // 2, name, c, 27, True, "lm")
            cells(d, x0, y, items, c, w=w, h=h, size=28, mark=diff, mark_colour=AMBER if c != AMBER else ROSE, gap=gap)
            y += h + 10
        y += 12
    caption(d, "Same nonce → same keystream. XOR the two cipher texts and it cancels: no key needed.", AMBER, 900)
    label(d, W // 2, 948, "c1 ⊕ c2  =  (p1 ⊕ k) ⊕ (p2 ⊕ k)  =  p1 ⊕ p2", MUTED, 28, False, "ma")


def sizes(img, d, cfg):
    """Encryption hides what a message says, not how long it is."""
    header(d, 100, 205, "BEFORE ENCRYPTION", GREEN)
    label(d, 100, 290, "message 1:  \"hello\"  =  5 bytes", TEXT, 32, True, "lm")
    label(d, 100, 345, "message 2:  500 letters  =  500 bytes", TEXT, 32, True, "lm")
    header(d, 100, 405, "AFTER ENCRYPTION   (nonce + cipher text + tag)", ROSE)
    x, unit = 100, 30
    for name, n, c in (("nonce 12", 12, CYAN), ("5", 5, ROSE), ("tag 16", 16, AMBER)):
        d.rounded_rectangle([x, 475, x + n * unit - 6, 540], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((x + (n * unit - 6) // 2, 507), name, font=font(SANS_B, 24), fill=c, anchor="mm")
        x += n * unit
    label(d, x + 20, 507, "message 1  =  33 bytes", TEXT, 32, True, "lm")
    x, unit = 100, 2.4
    for name, n, c in (("", 12, CYAN), ("500 bytes of cipher text", 500, ROSE), ("", 16, AMBER)):
        x1 = x + max(int(n * unit) - 4, 26)
        d.rounded_rectangle([x, 575, x1, 640], radius=10, fill=BG_CELL, outline=c, width=3)
        if name:
            d.text(((x + x1) // 2, 607), name, font=font(SANS_B, 28), fill=c, anchor="mm")
        x = x1 + 4
    label(d, x + 20, 607, "message 2  =  528 bytes", TEXT, 32, True, "lm")
    caption(d, "Always 28 bytes more. The content is hidden, the length is not.", AMBER, 715)
    label(d, W // 2, 790, "Random 12-byte nonces: at most 4,294,967,296 messages per key.", MUTED, 32, False, "ma")
    label(d, W // 2, 845, "Lose the key, and every message is lost with it.", MUTED, 32, False, "ma")


def tag_steps(img, d, cfg):
    """How the GCM tag is made: hash key, stirring, mask, xor."""
    act = section("FOUR")
    h = value("H", "FOUR")
    rows = re.findall(r"^    (account|cipher|lengths)\s+([0-9a-f ]+)\n      X =\s+([0-9a-f ]+)$", act, re.M)
    counter, mask, tag = value("counter", "FOUR"), value("mask", "FOUR"), value("tag", "FOUR")
    short = lambda v: v.replace(" ", "")[:8] + "…"

    def node(x, y, w, h_, text, c, size=26, mono=True):
        d.rounded_rectangle([x, y, x + w, y + h_], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((x + w // 2, y + h_ // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")

    # STEP 1: H = AES(key, zeros)
    e = header(d, 100, 205, "STEP 1", AMBER)
    label(d, e + 20, 227, "a hash key H, from the key", TEXT, 28, True, "lm")
    node(100, 265, 300, 56, "16 zero bytes", MUTED, 24, False)
    draw.arrow_right(d, 405, 293, 480, MUTED, 3)
    node(485, 265, 260, 56, "AES with key", AMBER, 24, False)
    draw.arrow_right(d, 750, 293, 825, AMBER, 3)
    node(830, 265, 620, 56, "H = " + h, AMBER, 24)

    # STEP 2: X = (X xor block) times H, for each block
    e = header(d, 100, 355, "STEP 2", CYAN)
    label(d, e + 20, 377, "stir each block into X:   X = (X ⊕ block) × H", TEXT, 28, True, "lm")
    node(100, 495, 130, 56, "X = 0", MUTED, 24)
    names = {"account": ("account", CLIENT), "cipher": ("cipher text", ROSE), "lengths": ("lengths", MUTED)}
    prev = 230
    for i, (name, block, x) in enumerate(rows):
        bx = 270 + i * 540
        nm, c = names[name]
        node(bx, 415, 330, 56, "%s %s" % (nm, short(block)), c, 22)
        cx, cy = bx + 60, 523
        draw.arrow(d, cx, 472, cx, cy - 30, c, 3)
        draw.arrow_right(d, prev + 5, cy, cx - 30, CYAN, 3)
        d.ellipse([cx - 26, cy - 26, cx + 26, cy + 26], fill=BG_CELL, outline=CYAN, width=3)
        d.text((cx, cy), "⊕", font=font(SANS_B, 34), fill=CYAN, anchor="mm")
        draw.arrow_right(d, cx + 28, cy, cx + 85, CYAN, 3)
        node(cx + 90, cy - 28, 120, 56, "× H", AMBER, 28, False)
        label(d, cx + 150, cy + 40, "X = " + short(x), MUTED, 22, False, "ma")
        prev = cx + 210
    draw.arrow_right(d, prev + 5, 523, prev + 60, CYAN, 3)
    node(prev + 65, 495, 190, 56, short(rows[-1][2]), CYAN, 24)

    # STEP 3: mask = AES(key, nonce + 00000001)
    e = header(d, 100, 615, "STEP 3", CLIENT)
    label(d, e + 20, 637, "a mask, from the key and the nonce", TEXT, 28, True, "lm")
    node(100, 675, 640, 56, counter, CLIENT, 24)
    label(d, 330, 737, "the nonce (12 bytes)", MUTED, 20, False, "ma")
    label(d, 655, 737, "+ counter 1", MUTED, 20, False, "ma")
    draw.arrow_right(d, 745, 703, 820, MUTED, 3)
    node(825, 675, 260, 56, "AES with key", AMBER, 24, False)
    draw.arrow_right(d, 1090, 703, 1165, CLIENT, 3)
    node(1170, 675, 650, 56, "mask = " + mask, CLIENT, 24)

    # STEP 4: tag = X xor mask
    e = header(d, 100, 765, "STEP 4", GREEN)
    label(d, e + 20, 787, "the tag", TEXT, 28, True, "lm")
    node(100, 825, 300, 56, "X  " + short(rows[-1][2]), CYAN, 24)
    d.text((440, 853), "⊕", font=font(SANS_B, 40), fill=TEXT, anchor="mm")
    node(480, 825, 330, 56, "mask  " + short(mask), CLIENT, 24)
    d.text((850, 853), "=", font=font(SANS_B, 40), fill=TEXT, anchor="mm")
    node(890, 825, 640, 56, "tag " + tag, GREEN, 24)
    caption(d, "Change one bit of the account or the cipher text: X changes, so the tag changes.", AMBER, 915)
