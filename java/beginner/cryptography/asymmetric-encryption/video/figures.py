"""Pictures for the asymmetric encryption video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("BEFORE ENCRYPTION")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, text):
    """The text after a label on a line of `text`."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", text, re.M)
    return m.group(1).strip()


def num(pattern, text):
    """The first number after a regex pattern."""
    return re.search(pattern + r"\D*(\d+)", text).group(1)


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE ENCRYPTION: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def node(d, x, y, w, h, text, c, size=28, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour, size=40):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (16 if sub else 0)), text, font=font(SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def verdict(d, x, y, ok, text):
    c = GREEN if ok else ROSE
    d.text((x, y), "✓" if ok else "✗", font=font(SANS_B, 46), fill=c, anchor="lm")
    label(d, x + 50, y, text, c, 30, True, "lm")


# ------------------------------------------------------------------ figures

def rsa_by_hand(img, d, cfg):
    """RSA with tiny numbers: make the keys, lock with the public key, open with the private key."""
    hand = section("TWO", "BY HAND")
    p, q = re.search(r"p = (\d+), q = (\d+)", hand).groups()
    n, e = re.search(r"n = p x q = (\d+), e = (\d+)", hand).groups()
    dd, prod = re.search(r"d = (\d+), as \d+ x \d+ = (\d+)", hand).groups()
    m = num(r"m =", section("TWO", "BEFORE ENCRYPTION"))
    c = num(r"mod n =", section("TWO", "AFTER ENCRYPTION"))
    back = num(r"c\^d mod n =", section("TWO", "OPENING"))
    phi = (int(p) - 1) * (int(q) - 1)

    e_ = header(d, 100, 200, "THE KEYS", AMBER)
    label(d, e_ + 24, 222, "Ana makes them once, from two secret primes", TEXT, 28, True, "lm")
    node(d, 100, 265, 330, 64, "p = %s   q = %s" % (p, q), MUTED, 28)
    draw.arrow_right(d, 440, 297, 520, MUTED, 3)
    node(d, 530, 265, 520, 64, "public: n = %s, e = %s" % (n, e), CLIENT, 26)
    node(d, 1070, 265, 750, 64, "private: d = %s, as %s × %s = %s = %d + 1" % (dd, e, dd, prod, phi), ROSE, 24)
    label(d, 790, 340, "n = %s × %s, published: anyone may lock" % (p, q), CLIENT, 22, False, "ma")
    label(d, 1445, 340, "kept by Ana: only she opens", ROSE, 22, False, "ma")

    header(d, 100, 400, "BEFORE ENCRYPTION", GREEN)
    node(d, 100, 460, 300, 90, m, GREEN, 54)
    label(d, 250, 560, "the message, a PIN", MUTED, 22, False, "ma")
    draw.arrow_right(d, 410, 505, 520, CLIENT, 4)
    big_box(d, 520, 450, 420, 110, "%s^%s mod %s" % (m, e, n), "lock with the public key", CLIENT, 30)
    draw.arrow_right(d, 940, 505, 1050, CLIENT, 4)
    header(d, 1060, 400, "AFTER ENCRYPTION", ROSE)
    node(d, 1060, 460, 300, 90, c, ROSE, 54)
    label(d, 1210, 560, "the locked number", MUTED, 22, False, "ma")

    header(d, 100, 620, "OPENING", AMBER)
    node(d, 100, 680, 300, 90, c, ROSE, 54)
    draw.arrow_right(d, 410, 725, 520, ROSE, 4)
    big_box(d, 520, 670, 420, 110, "%s^%s mod %s" % (c, dd, n), "open with the private key", ROSE, 30)
    draw.arrow_right(d, 940, 725, 1050, GREEN, 4)
    node(d, 1060, 680, 300, 90, back, GREEN, 54)
    verdict(d, 1400, 725, True, "the PIN is back")
    caption(d, "Locking needs only the public key. Opening needs d, and d needs the secret primes.", AMBER, 840)
    label(d, W // 2, 895, "Real RSA does exactly this, with numbers of 617 digits.", MUTED, 30, False, "ma")


def key_in_clear(img, d, cfg):
    """The bank sends the key in the clear; Eve copies it on the wire."""
    sent = value("key", section("ONE", "WHAT THE BANK SENDS"))
    seen = value("key", section("ONE", "WHAT EVE SEES"))
    big_box(d, 100, 300, 300, 140, "Bank", "has the key", CLIENT)
    big_box(d, 1520, 300, 300, 140, "Ana", "needs the key", GREEN)
    d.line([410, 370, 1510, 370], fill=LINE, width=6)
    draw.arrow_right(d, 1400, 370, 1510, LINE, 6)
    label(d, 960, 330, "the open network", MUTED, 26, False, "ma")
    draw.arrow(d, 960, 380, 960, 470, ROSE, 4)
    big_box(d, 810, 480, 300, 110, "Eve", "copies every byte", ROSE)
    header(d, 100, 640, "WHAT THE BANK SENDS", GREEN)
    node(d, 100, 700, 820, 70, sent, GREEN, 32)
    header(d, 1000, 640, "WHAT EVE SEES", ROSE)
    node(d, 1000, 700, 820, 70, seen, ROSE, 32)
    caption(d, "The same 32 bytes. Eve now opens anything locked with this key.", AMBER, 830)
    label(d, W // 2, 885, "And the bank and Ana have never met to hand it over in person.", MUTED, 30, False, "ma")


def rsa_real(img, d, cfg):
    """Real RSA-2048 with OAEP: sizes, who can open it, and why two locks differ."""
    real = OUT.split("REAL RSA-2048", 1)[1].split("THREE.", 1)[0]
    pub = num(r"Ana's public key", real)
    key = num(r"the key", real)
    wire = num(r"on the wire", real)
    most = num(r"at most", real)
    big_box(d, 100, 215, 760, 90, "Ana's public key: %s bytes" % pub, "", CLIENT, 32)
    label(d, 880, 260, "published, so the bank (and Eve) can have it", MUTED, 26, False, "lm")
    header(d, 100, 345, "BEFORE ENCRYPTION", GREEN)
    node(d, 100, 405, 320, 80, "key: %s bytes" % key, GREEN, 32, False)
    draw.arrow_right(d, 430, 445, 520, CLIENT, 4)
    big_box(d, 530, 395, 420, 100, "RSA + OAEP", "public key + fresh random padding", CLIENT, 34)
    draw.arrow_right(d, 960, 445, 1050, ROSE, 4)
    header(d, 1060, 345, "AFTER ENCRYPTION", ROSE)
    d.rounded_rectangle([1060, 405, 1060 + int(256 * 2.9), 485], radius=10, fill=BG_CELL, outline=ROSE, width=3)
    d.text((1060 + int(128 * 2.9), 445), "%s bytes on the wire" % wire, font=font(SANS_B, 32), fill=ROSE, anchor="mm")
    header(d, 100, 545, "OPENING", AMBER)
    verdict(d, 100, 630, True, "Ana's private key: opens it")
    verdict(d, 100, 700, False, "Eve's private key: refused")
    verdict(d, 1000, 630, True, "locked twice: different bytes (OAEP)")
    verdict(d, 1000, 700, False, "%d bytes or more: refused" % (int(most) + 1))
    caption(d, "RSA locks at most %s bytes: just right for a key, far too small for a statement." % most, AMBER, 800)
    label(d, W // 2, 860, "Without OAEP, the same message always locks the same: textbook RSA.", MUTED, 30, False, "ma")


def hybrid_envelope(img, d, cfg):
    """Hybrid: AES-GCM for the data, RSA only for the AES key."""
    before, after = section("THREE", "BEFORE ENCRYPTION"), section("THREE", "AFTER ENCRYPTION")
    stmt, aes = num(r"statement", before), num(r"fresh AES key", before)
    locked, nonce, ct, total = (num(r"locked key", after), num(r"nonce", after),
                                num(r"cipher text", after), num(r"in all", after))
    header(d, 100, 205, "BEFORE ENCRYPTION", GREEN)
    node(d, 100, 270, 640, 80, "statement: %s bytes" % stmt, GREEN, 32, False)
    node(d, 100, 380, 640, 80, "fresh AES key: %s bytes" % aes, AMBER, 32, False)
    draw.arrow_right(d, 750, 310, 1050, ROSE, 4)
    label(d, 900, 272, "AES-GCM, with the AES key", MUTED, 22, False, "ma")
    draw.arrow_right(d, 750, 420, 1050, CLIENT, 4)
    label(d, 900, 434, "RSA, with Ana's public key", MUTED, 22, False, "ma")
    node(d, 1060, 270, 760, 80, "cipher text + tag: %s bytes" % ct, ROSE, 30, False)
    node(d, 1060, 380, 760, 80, "locked AES key: %s bytes" % locked, CLIENT, 30, False)
    header(d, 100, 520, "AFTER ENCRYPTION: the envelope, to scale", ROSE)
    x, unit = 100, 1720 / int(total)
    for name, n, c in (("", int(locked), CLIENT), ("", int(nonce), CYAN), ("cipher text and tag  %s" % ct, int(ct), ROSE)):
        x1 = x + max(int(n * unit), 10)
        d.rounded_rectangle([x, 590, x1, 670], radius=8, fill=BG_CELL, outline=c, width=3)
        if name:
            d.text(((x + x1) // 2, 630), name, font=font(SANS_B, 30), fill=c, anchor="mm")
        x = x1 + 4
    label(d, 100, 685, "locked key %s  +  nonce %s  +  cipher text %s  =  %s bytes" % (locked, nonce, ct, total),
          TEXT, 30, True)
    caption(d, "AES does the heavy work. RSA carries only the small key.", AMBER, 790)
    label(d, W // 2, 850, "Ana unlocks the AES key with her private key, then opens the statement.", MUTED, 30, False, "ma")


def key_agreement(img, d, cfg):
    """Key agreement by hand with tiny numbers, and the same with real X25519 keys."""
    hand = section("FOUR", "BY HAND")
    g, p = re.search(r"g = (\d+), p = (\d+)", OUT).groups()
    b, bs = re.search(r"bank secret (\d+)\s+sends .* = (\d+)", hand).groups()
    a, as_ = re.search(r"Ana secret (\d+)\s+sends .* = (\d+)", hand).groups()
    bg = re.search(r"bank gets .* = (\d+)", hand).group(1)
    ag = re.search(r"Ana gets .* = (\d+)", hand).group(1)
    swap = section("FOUR", "AFTER THE SWAP")
    real = value("bank gets", swap).replace(" ...", "…")
    header(d, 100, 200, "BY HAND, TINY NUMBERS:  everyone knows g = %s and p = %s" % (g, p), AMBER)
    for i, (who, sec, pub, c) in enumerate((("Bank", b, bs, CLIENT), ("Ana", a, as_, GREEN))):
        y = 270 + i * 120
        big_box(d, 100, y, 220, 90, who, "", c, 34)
        node(d, 340, y + 10, 260, 70, "secret %s" % sec, ROSE, 28, False)
        draw.arrow_right(d, 610, y + 45, 680, c, 3)
        node(d, 690, y + 10, 360, 70, "%s^%s mod %s = %s" % (g, sec, p, pub), c, 28)
    label(d, 1175, 362, "swapped in the open", MUTED, 22, False, "ma")
    node(d, 1300, 280, 520, 70, "bank:  %s^%s mod %s = %s" % (as_, b, p, bg), CLIENT, 28)
    node(d, 1300, 400, 520, 70, "Ana:  %s^%s mod %s = %s" % (bs, a, p, ag), GREEN, 28)
    draw.arrow_right(d, 1060, 315, 1290, MUTED, 3)
    draw.arrow_right(d, 1060, 435, 1290, MUTED, 3)
    label(d, W // 2, 500, "Eve sees %s and %s, but not %s or %s, so she cannot make %s." % (bs, as_, b, a, bg),
          ROSE, 28, True, "ma")
    header(d, 100, 570, "BEFORE THE SWAP: real X25519 keys", GREEN)
    label(d, 100, 640, "bank public  " + value("bank public", section("FOUR", "BEFORE THE SWAP")).replace(" ...", "…"),
          CLIENT, 26, True)
    label(d, 100, 680, "Ana public   " + value("Ana public", section("FOUR", "BEFORE THE SWAP")).replace(" ...", "…"),
          GREEN, 26, True)
    header(d, 100, 735, "AFTER THE SWAP: the same secret on both sides", ROSE)
    label(d, 100, 805, "both get   " + real, AMBER, 26, True)
    caption(d, "No key crosses the wire, only public keys. HKDF then makes the key to use.", AMBER, 870)


def man_in_middle(img, d, cfg):
    """Mallory swaps the public key, reads the message and passes it on."""
    sent = value("message", section("FIVE", "WHAT THE BANK SENDS"))
    read = value("reads", section("FIVE", "IN THE MIDDLE"))
    got = value("opened", section("FIVE", "WHAT ANA RECEIVES")).split("  <-")[0]
    big_box(d, 100, 220, 300, 130, "Bank", "thinks it has Ana's key", CLIENT)
    big_box(d, 810, 220, 300, 130, "Mallory", "swapped in her key", ROSE)
    big_box(d, 1520, 220, 300, 130, "Ana", "sees nothing wrong", GREEN)
    draw.arrow_right(d, 410, 285, 800, ROSE, 4)
    label(d, 605, 245, "locked to Mallory's key", MUTED, 22, False, "ma")
    draw.arrow_right(d, 1120, 285, 1510, GREEN, 4)
    label(d, 1315, 245, "locked again to Ana's key", MUTED, 22, False, "ma")
    for i, (title, text, c) in enumerate((("WHAT THE BANK SENDS", sent, GREEN), ("IN THE MIDDLE, MALLORY", read, ROSE),
                                         ("WHAT ANA RECEIVES", got, AMBER))):
        x = 100 + i * 610
        header(d, x, 420, title, c, 500)
        node(d, x, 490, 500, 80, text, c, 32)
    label(d, 960, 600, "reads it", ROSE, 24, True, "ma")
    caption(d, "Nothing in the maths says whose public key it is.", AMBER, 690)
    label(d, W // 2, 750, "Signatures and certificates, in the next videos, are the answer.", MUTED, 30, False, "ma")
    label(d, W // 2, 810, "And a stolen private key opens every message ever locked to it.", MUTED, 30, False, "ma")
