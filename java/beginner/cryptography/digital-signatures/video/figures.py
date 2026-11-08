"""Pictures for the digital signatures video, drawn by the `figure` slide kind.

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


def section(act):
    """One act of the output ("TWO"), without its title line."""
    return re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)


def value(label, act):
    """The text after a label on a line of one act's output."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", section(act), re.M)
    return m.group(1).strip()


def result(label, act):
    """The word after "label:" on a line of one act's output (OK, BAD_SIGNATURE, true, refused...)."""
    m = re.search(re.escape(label) + r":?\s+(\S+)", section(act))
    return m.group(1)


def short(v, groups=2):
    """'a83b53e9 ba4d832c fa721a3f ...' -> 'a83b53e9 ba4d832c …'"""
    return " ".join(v.split()[:groups]) + " …"


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE SIGNING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def node(d, x, y, w, h, text, c, size=26, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def lines_box(d, x, y, w, rows, c, size=28, lh=36):
    """A box of mono lines, such as the price list one field per line."""
    h = lh * len(rows) + 24
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=c, width=3)
    for i, r in enumerate(rows):
        d.text((x + 24, y + 12 + i * lh), r, font=font(MONO_B, size), fill=c)
    return y + h


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, 40), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def verdict(d, x, y, text, good):
    """A tick or a cross and the answer: green when the right thing happened."""
    c = GREEN if good else ROSE
    d.text((x, y), "✓" if good else "✗", font=font(SANS_B, 56), fill=c, anchor="mm")
    d.text((x + 50, y), text, font=font(MONO_B, 32), fill=c, anchor="lm")


def check_row(d, y, what, box, answer, good, box_colour=CYAN):
    """what goes in  →  [the check]  →  ✓/✗ answer"""
    d.rounded_rectangle([100, y, 880, y + 84], radius=14, fill=BG_CELL, outline=LINE, width=2)
    label(d, 130, y + 42, what, TEXT, 30, False, "lm")
    draw.arrow_right(d, 895, y + 42, 1000, MUTED, 4)
    big_box(d, 1015, y + 2, 400, 80, box, "", box_colour)
    draw.arrow_right(d, 1430, y + 42, 1510, GREEN if good else ROSE, 4)
    verdict(d, 1545, y + 42, answer, good)


def fields(price_list):
    """'version=2 validUntilDay=100 laptop=1200 mouse=25' -> its four lines."""
    return price_list.split()


# ------------------------------------------------------------------ figures

def one_signature(img, d, cfg):
    """The whole idea on one price list: sign with the private key, check with the public key."""
    plist, priv = value("price list", "TWO"), value("private key", "TWO")
    sig = re.findall(r"^    bytes\s+\d+-\d+\s+(.+)$", section("TWO"), re.M)
    pub, ok = value("public key", "TWO"), result("the real list", "TWO")
    header(d, 100, 205, "BEFORE SIGNING", GREEN)
    label(d, 100, 270, "the price list", GREEN, 26, True)
    lines_box(d, 100, 305, 540, fields(plist), GREEN)
    draw.arrow_right(d, 655, 390, 735, GREEN, 4)
    big_box(d, 745, 320, 270, 140, "sign", "Ed25519", AMBER)
    node(d, 640, 520, 480, 56, "private key " + short(priv.split("(")[0], 2), AMBER, 22)
    label(d, 880, 586, "only the shop has it", MUTED, 22, False, "ma")
    draw.arrow_up(d, 880, 518, 466, AMBER, 4)
    draw.arrow_right(d, 1025, 390, 1105, ROSE, 4)
    header(d, 1115, 205, "AFTER SIGNING", ROSE)
    label(d, 1115, 270, "the signature: 64 bytes", ROSE, 26, True)
    lines_box(d, 1115, 305, 705, sig, ROSE, 28)
    header(d, 100, 650, "CHECKING", CYAN)
    node(d, 100, 715, 560, 70, "price list + signature", TEXT, 28, False)
    draw.arrow_right(d, 670, 750, 745, CYAN, 4)
    big_box(d, 755, 705, 300, 90, "verify", "", CYAN)
    node(d, 1150, 715, 470, 70, "public key " + short(pub.split("(")[0], 2), CYAN, 22)
    draw.arrow_left(d, 1145, 750, 1062, CYAN, 4)
    verdict(d, 1670, 750, ok, True)
    caption(d, "The private key makes the signature. The public key only checks it.", AMBER, 865)


def hmac_forge(img, d, cfg):
    """A shared key: the warehouse can check the shop's tag, so it can make one too."""
    plist, key = value("price list", "ONE"), value("shared key", "ONE")
    tag, its_list, its_tag = value("tag", "ONE"), value("its list", "ONE"), value("its tag", "ONE")
    answer = result("the shop's check", "ONE")
    header(d, 100, 205, "BEFORE TAGGING", GREEN)
    end = lines_box(d, 100, 265, 520, fields(plist), GREEN, 26, 34)
    draw.arrow_right(d, 635, 340, 735, GREEN, 4)
    big_box(d, 745, 290, 250, 110, "HMAC", "", AMBER)
    node(d, 640, 445, 460, 56, key, AMBER, 22)
    draw.arrow_up(d, 870, 443, 406, AMBER, 4)
    label(d, 870, 510, "the shared key: shop AND warehouse", MUTED, 22, False, "ma")
    draw.arrow_right(d, 1005, 345, 1105, ROSE, 4)
    header(d, 1115, 205, "AFTER TAGGING", ROSE)
    label(d, 1115, 275, "the tag: 32 bytes", ROSE, 26, True)
    node(d, 1115, 315, 705, 60, short(tag, 4), ROSE, 26)
    header(d, 100, 580, "THE ATTACK: THE WAREHOUSE USES ITS COPY OF THE KEY", ROSE)
    y = 650
    node(d, 100, y, 700, 60, its_list, TEXT, 22)
    draw.arrow_right(d, 810, y + 30, 900, MUTED, 4)
    big_box(d, 910, y - 5, 170, 70, "HMAC", "", AMBER)
    draw.arrow_right(d, 1090, y + 30, 1180, MUTED, 4)
    node(d, 1190, y, 630, 60, short(its_tag, 3), TEXT, 24)
    label(d, 100, y + 110, "the shop's check:", TEXT, 32, True, "lm")
    verdict(d, 450, y + 110, answer, False)
    caption(d, "Anyone who can check a tag can also make one.", AMBER, 920)


def sign_check(img, d, cfg):
    """Three checks with only the public key: the real list, a changed list, Mallory's list."""
    pub = value("public key", "TWO").split("(")[0]
    rows = [("the real list + its signature", result("the real list", "TWO"), True),
            ("laptop changed to 12, same signature", result("laptop changed to 12, same sig", "TWO"), False),
            ("Mallory's list, signed with her key", result("Mallory's list, signed by her", "TWO"), False)]
    header(d, 100, 205, "THE SHOP", AMBER)
    node(d, 100, 265, 620, 60, "private key: signs (only the shop)", AMBER, 26, False)
    header(d, 900, 205, "THE WAREHOUSE", CYAN)
    node(d, 900, 265, 920, 60, "public key %s : checks only" % short(pub, 2), CYAN, 24)
    header(d, 100, 385, "CHECKING WITH THE PUBLIC KEY", CYAN)
    for i, (what, answer, real) in enumerate(rows):
        # the right outcome: OK for the real list, BAD_SIGNATURE for the others
        check_row(d, 455 + i * 120, what, "verify", answer, real == (answer == "OK"))
    caption(d, "The public key checks. It cannot sign, so the warehouse cannot forge.", AMBER, 840)


def signature_steps(img, d, cfg):
    """The five steps that make the 64 bytes, with the real values, and the check."""
    a, prefix = value("secret a", "THREE"), value("prefix", "THREE")
    pub = value("A = a x B", "THREE")
    r, big_r, k, s = (value(n, "THREE") for n in ("r", "R", "k", "S"))
    same = result("R + S is Java's own signature", "THREE")
    real = result("the real list", "THREE")

    e = header(d, 100, 205, "STEP 1", AMBER)
    label(d, e + 24, 227, "hash the private key with SHA-512: 64 bytes, cut in two", TEXT, 28, True, "lm")
    node(d, 100, 270, 520, 56, "a  " + short(a), ROSE, 24)
    label(d, 360, 332, "first half: the secret number", MUTED, 20, False, "ma")
    node(d, 660, 270, 520, 56, "prefix  " + short(prefix), MUTED, 24)
    label(d, 920, 332, "second half", MUTED, 20, False, "ma")
    node(d, 1220, 270, 600, 56, "A = a × B  " + short(pub), CYAN, 24)
    label(d, 1520, 332, "the public key", MUTED, 20, False, "ma")
    steps = [("STEP 2", "r = SHA-512(prefix + message) mod L", "r  " + short(r), MUTED, "a one-time number"),
             ("STEP 3", "R = r × B", "R  " + short(big_r), ROSE, "signature bytes 1-32"),
             ("STEP 4", "k = SHA-512(R + A + message) mod L", "k  " + short(k), MUTED, "ties R, A and the message"),
             ("STEP 5", "S = (r + k × a) mod L", "S  " + short(s), ROSE, "signature bytes 33-64")]
    for i, (step, formula, val, c, note) in enumerate(steps):
        y = 385 + i * 95
        e = header(d, 100, y, step, AMBER)
        label(d, e + 24, y + 22, formula, TEXT, 28, True, "lm")
        node(d, 1220, y - 6, 600, 56, val, c, 24)
        label(d, 1210, y + 22, note, MUTED, 20, False, "rm")
    y = 775
    label(d, 100, y + 32, "signature", TEXT, 28, True, "lm")
    node(d, 270, y, 560, 64, "R  (32 bytes)", ROSE, 28, False)
    node(d, 840, y, 560, 64, "S  (32 bytes)", ROSE, 28, False)
    label(d, 1430, y + 32, "= Java's own: " + same, GREEN, 28, True, "lm")
    label(d, W // 2, 875, "CHECK with the public key only:   S × B  =  R + k × A   →   " + real, CYAN, 30, True, "ma")
    label(d, W // 2, 925, "× B is quick to do and cannot be undone, so A does not give away a.", MUTED, 28, False, "ma")


def signed_bytes(img, d, cfg):
    """The signed bytes: label, zero byte, list; and a label for another purpose."""
    lab, plist = value("label", "FOUR"), value("list", "FOUR")
    around = value("around 00", "FOUR")
    left, right = around.split("|")[0].strip(), around.split("|")[2].strip()
    same = result("built in another order, the same bytes", "FOUR")
    refund, offered = result("checked as a refund order", "FOUR"), result("offered as a price list", "FOUR")
    header(d, 100, 205, "BEFORE SIGNING: THE SIGNED BYTES", GREEN)
    node(d, 100, 290, 470, 70, lab, AMBER, 28)
    label(d, 335, 268, "the label (the purpose)", AMBER, 22, True, "mm")
    d.text((600, 325), "+", font=font(SANS_B, 44), fill=TEXT, anchor="mm")
    node(d, 630, 290, 110, 70, "00", ROSE, 32)
    label(d, 685, 268, "zero byte", ROSE, 22, True, "mm")
    d.text((770, 325), "+", font=font(SANS_B, 44), fill=TEXT, anchor="mm")
    label(d, 1040, 268, "the price list, products in name order", GREEN, 22, True, "mm")
    lines_box(d, 800, 290, 480, fields(plist), GREEN, 26, 32)
    label(d, 1320, 300, "built in another order:", MUTED, 24)
    label(d, 1320, 336, "the same bytes: " + same, GREEN, 26, True)
    # the bytes around the zero byte, with their letters
    hexes = [left[i:i + 2] for i in range(0, 8, 2)] + ["00"] + [right[i:i + 2] for i in range(0, 8, 2)]
    chars = [chr(int(h, 16)) for h in hexes]
    label(d, 100, 482, "around the zero byte:", MUTED, 24, False, "lm")
    for i, (h, ch) in enumerate(zip(hexes, chars)):
        x = 380 + i * 92
        c = ROSE if h == "00" else (AMBER if i < 4 else GREEN)
        node(d, x, 455, 80, 56, h, c, 28)
        label(d, x + 40, 520, ch if h != "00" else "", c, 24, True, "ma")
    header(d, 100, 600, "AFTER SIGNING WITH ANOTHER LABEL: shop.refund.v1", ROSE)
    check_row(d, 665, "checked as a refund order", "verify", refund, True, AMBER)
    check_row(d, 775, "offered as a price list", "verify", offered, True, AMBER)
    caption(d, "The label is signed too, so a signature cannot be used for another purpose.", AMBER, 905)


def replay(img, d, cfg):
    """A genuine old list replayed; the version and the expiry, inside the signed bytes, stop it."""
    v1, v2 = value("version 1", "FIVE"), value("version 2", "FIVE")
    naive = result("signature checked only", "FIVE")
    old, expired = result("a checkout that has seen version 2", "FIVE"), result("valid until day 40, offered on day 50", "FIVE")
    price = lambda v: re.search(r"laptop=(\d+)", v).group(1)
    header(d, 100, 205, "BEFORE: BOTH SIGNED BY THE SHOP", GREEN)
    node(d, 100, 270, 820, 64, "version 1   laptop %s dollars   ✓ signed" % price(v1), MUTED, 26)
    node(d, 1000, 270, 820, 64, "version 2   laptop %s dollars   ✓ signed" % price(v2), GREEN, 26)
    draw.arrow_right(d, 925, 302, 995, MUTED, 3)
    label(d, 960, 345, "later", MUTED, 20, False, "ma")
    header(d, 100, 400, "THE ATTACK: VERSION 1 SENT AGAIN ON DAY 50", ROSE)
    check_row(d, 465, "version 1, with its genuine signature", "signature only", naive, False, MUTED)
    header(d, 100, 590, "AFTER THE FIX: VERSION AND EXPIRY ARE CHECKED", GREEN)
    check_row(d, 655, "version 1, after version 2 was seen", "version check", old, True, GREEN)
    check_row(d, 765, "valid until day 40, on day 50", "expiry check", expired, True, GREEN)
    caption(d, "Version and expiry are inside the signed bytes: nobody can change them.", AMBER, 900)


def whose_key(img, d, cfg):
    """The check is only as good as the public key; a stolen private key signs anything."""
    fooled = re.findall(r"Mallory's list: (\S+)", section("SIX"))
    thief = result("stop a stolen private key", "SIX")
    sizes = re.search(r"bytes: signature (\d+), public key (\d+)", section("SIX")).groups()
    header(d, 100, 205, "THE ATTACK: MALLORY'S PUBLIC KEY, SAID TO BE THE SHOP'S", ROSE)
    check_row(d, 270, "her list, laptop at 1 dollar", "Mallory's key", fooled[0], False, ROSE)
    header(d, 100, 390, "AFTER THE FIX: THE SHOP'S REAL PUBLIC KEY", GREEN)
    check_row(d, 455, "her list, laptop at 1 dollar", "the shop's key", fooled[1], True, GREEN)
    header(d, 100, 580, "WHAT A SIGNATURE CANNOT DO", AMBER)
    check_row(d, 645, "a thief with the shop's private key", "the shop's key", thief, False, GREEN)
    label(d, 100, 775, "hide anything: the price list is readable by anyone", TEXT, 30, False, "lm")
    x, unit = 100, 9
    label(d, 100, 835, "sizes:", MUTED, 26, True, "lm")
    x = 200
    for name, n, c in (("signature %s bytes" % sizes[0], int(sizes[0]), ROSE), ("public key %s" % sizes[1], int(sizes[1]), CYAN)):
        d.rounded_rectangle([x, 810, x + n * unit, 860], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((x + n * unit // 2, 835), name, font=font(SANS_B, 22), fill=c, anchor="mm")
        x += n * unit + 20
    caption(d, "The maths is fine; the key was wrong. Certificates answer whose key it is.", AMBER, 905)
