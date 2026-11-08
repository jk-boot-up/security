"""Pictures for the digital signatures with Bouncy Castle video, drawn by the `figure` slide kind.

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


def section(act):
    """One act of the output ("TWO"), without its title line."""
    return re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)


def value(act, label):
    """The value after a label; a 32-byte number printed over two lines comes back joined."""
    m = re.search(r"^    %s\s{2,}(.+)\n(?:\s{17}(\S.*)\n)?" % re.escape(label), section(act), re.M)
    return (m.group(1) + (" " + m.group(2) if m.group(2) else "")).strip()


def has(act, text):
    """True when a line of the act contains the text."""
    return text in section(act)


def short(v):
    """'6e158e90 b7240b23 ... 324fb981' -> '6e158e90…324fb981'"""
    h = v.replace(" ", "")
    return h[:8] + "…" + h[-8:]


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE SIGNING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def node(d, x, y, w, h, text, colour, size=28, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, 40), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


def verdict(d, x, y, ok, text):
    c = GREEN if ok else ROSE
    d.text((x, y), "✓" if ok else "✗", font=font(SANS_B, 56), fill=c, anchor="mm")
    label(d, x + 40, y, text, c, 32, True, "lm")


# ------------------------------------------------------------------ figures

def sign_verify(img, d, cfg):
    """The core idea on one statement: sign with the private key, check with the public key."""
    msg = value("TWO", "message")
    key, r, s = value("TWO", "private key"), value("TWO", "r"), value("TWO", "s")
    header(d, 100, 210, "BEFORE SIGNING", GREEN)
    label(d, 100, 280, "the statement", GREEN, 26, True)
    node(d, 100, 315, 640, 64, msg, GREEN, 28)
    label(d, 100, 400, "the bank's private key (secret)", AMBER, 26, True)
    node(d, 100, 435, 640, 64, short(key), AMBER, 28)
    draw.arrow_right(d, 750, 347, 830, GREEN, 4)
    draw.arrow_right(d, 750, 467, 830, AMBER, 4)
    big_box(d, 840, 330, 300, 150, "ECDSA", "sign", CYAN)
    draw.arrow_right(d, 1150, 405, 1230, CYAN, 4)
    header(d, 1240, 210, "AFTER SIGNING", ROSE)
    label(d, 1240, 280, "the statement, unchanged", GREEN, 26, True)
    node(d, 1240, 315, 580, 64, msg, GREEN, 26)
    label(d, 1240, 400, "plus the signature: r and s", ROSE, 26, True)
    node(d, 1240, 435, 580, 56, "r " + short(r), ROSE, 26)
    node(d, 1240, 500, 580, 56, "s " + short(s), ROSE, 26)
    d.line([(100, 600), (W - 100, 600)], fill=LINE, width=2)
    header(d, 100, 625, "THE CHECK, BY ANYONE", CYAN)
    label(d, 100, 700, "statement + signature + the bank's PUBLIC key", TEXT, 30, True)
    node(d, 100, 750, 640, 60, msg, GREEN, 26)
    verdict(d, 820, 780, has("TWO", "x = r: verifies"), "verifies")
    node(d, 1100, 750, 640, 60, msg.replace("1200", "9200"), ROSE, 26)
    verdict(d, 1790, 780, not has("TWO", "balance 9200: refused"), "")
    label(d, 1420, 830, "balance changed: refused", ROSE, 26, True, "ma")
    caption(d, "Only the private key can sign. Anyone with the public key can check.", AMBER, 905)


def cms_envelope(img, d, cfg):
    """CMS: the signature covers the attributes, and the attributes cover the statement through its digest."""
    msg, sha = value("ONE", "statement"), value("ONE", "SHA-256")
    signer, digest, time = value("ONE", "signer"), value("ONE", "digest"), value("ONE", "time")
    header(d, 100, 210, "BEFORE SIGNING", GREEN)
    node(d, 100, 280, 560, 64, msg, GREEN, 26)
    draw.arrow(d, 380, 350, 380, 410, GREEN, 4)
    label(d, 400, 365, "SHA-256", MUTED, 24, True)
    node(d, 100, 415, 560, 64, short(sha), CYAN, 28)
    label(d, 100, 500, "a 32-byte fingerprint of the statement", MUTED, 24)
    label(d, 100, 560, "signing time  " + time, AMBER, 26, True)
    draw.arrow_right(d, 680, 447, 790, CYAN, 4)
    draw.arrow_right(d, 680, 573, 790, AMBER, 4)
    header(d, 800, 210, "AFTER SIGNING: the CMS message", ROSE)
    d.rounded_rectangle([800, 270, 1820, 840], radius=18, fill=PANEL, outline=ROSE, width=4)
    rows = [("content", "the statement (attached), or nothing (detached)", GREEN),
            ("certificate", signer, CLIENT),
            ("signed attributes", "message digest  " + short(digest), CYAN),
            ("", "signing time  " + time, AMBER),
            ("signature", "ECDSA over the signed attributes", ROSE)]
    for i, (name, text, c) in enumerate(rows):
        y = 295 + i * 105
        if name:
            label(d, 830, y + 30, name, c, 26, True, "lm")
        node(d, 1110, y, 680, 62, text, c, 22, mono=name == "signed attributes" or not name)
    caption(d, "Change one byte of the statement: its digest changes, so the check says refused.", AMBER, 890)


def ecdsa_steps(img, d, cfg):
    """How the signature r, s is made from the key and the statement, and how it is checked."""
    v = {k: value("TWO", k) for k in ("private key", "h", "k", "r", "s", "x")}
    rows = [("STEP 1", "h = SHA-256(message)", "h", GREEN, "the fingerprint of the statement"),
            ("STEP 2", "k from the key and h (RFC 6979)", "k", AMBER, "secret, one time only"),
            ("STEP 3", "r = x of the point k × G", "r", CYAN, "G: the curve's fixed start point"),
            ("STEP 4", "s = (h + r × key) / k, mod n", "s", ROSE, "mixes in the private key")]
    e = header(d, 100, 205, "BEFORE SIGNING", GREEN)
    label(d, e + 20, 227, "the statement + the private key " + short(v["private key"]), TEXT, 28, True, "lm")
    for i, (step, rule, name, c, note) in enumerate(rows):
        y = 275 + i * 112
        header(d, 100, y + 8, step, c, 140)
        label(d, 270, y + 30, rule, TEXT, 30, True, "lm")
        label(d, 270, y + 72, note, MUTED, 22, False, "lm")
        node(d, 1050, y + 2, 740, 62, "%s = %s" % (name, short(v[name])), c, 28)
    e = header(d, 100, 735, "AFTER SIGNING", ROSE)
    label(d, e + 20, 757, "the signature is r and s: 64 bytes", TEXT, 30, True, "lm")
    e = header(d, 100, 805, "THE CHECK", CYAN)
    label(d, e + 20, 827, "public key Q:  x of (h/s)×G + (r/s)×Q", TEXT, 30, True, "lm")
    node(d, 1050, 797, 740, 62, "x = " + short(v["x"]), CYAN, 28)
    caption(d, "x equals r, so the signature verifies. A changed statement gives another h, so another x.", AMBER, 900)


def nonce_reuse(img, d, cfg):
    """One k for two payments: the shared r shows it, and two equations give the key."""
    m1, m2 = value("THREE", "message 1"), value("THREE", "message 2")
    k, key = value("THREE", "k"), value("THREE", "key")
    bank = value("TWO", "private key")
    header(d, 100, 205, "THE MISTAKE: one k for two payments", ROSE)
    for i, m in enumerate((m1, m2)):
        y = 270 + i * 80
        node(d, 100, y, 420, 62, m, CLIENT, 28)
        draw.arrow_right(d, 530, y + 31, 640, MUTED, 3)
        node(d, 650, y, 420, 62, "same k  →  r, s%d" % (i + 1), AMBER, 26, False)
    label(d, 1110, 330, "the same r twice: the reuse shows", ROSE, 30, True, "lm")
    header(d, 100, 450, "THE ATTACK, WITHOUT THE KEY", AMBER)
    label(d, 100, 535, "k = (h1 − h2) / (s1 − s2)", TEXT, 34, True, "lm")
    node(d, 900, 505, 520, 62, "k = " + short(k), AMBER, 28)
    label(d, 100, 625, "key = (s1 × k − h1) / r", TEXT, 34, True, "lm")
    node(d, 900, 595, 520, 62, "key = " + short(key), ROSE, 28)
    verdict(d, 1480, 626, key == bank, "the bank's key")
    header(d, 100, 710, "THE FIX", GREEN)
    label(d, 100, 790, "a deterministic k, from the key and the message (RFC 6979)", GREEN, 30, True, "lm")
    label(d, 100, 845, "or Ed25519 and Ed448, which need no random number at all", GREEN, 30, True, "lm")
    caption(d, "Two signatures, one k: anyone can compute the private key.", AMBER, 910)


def jws_token(img, d, cfg):
    """A JWS: header and payload in base64url, a signature over both, joined by dots."""
    hj, pj = value("FOUR", "header"), value("FOUR", "payload")
    after = section("FOUR").split("AFTER SIGNING")[1]
    hb = re.search(r"header\s+(\S+)", after).group(1)
    pb = re.search(r"payload\s+(\S+)", after).group(1)
    sb = re.search(r"signature\s+(\S+)", after).group(1)
    header(d, 100, 205, "BEFORE SIGNING", GREEN)
    label(d, 100, 285, "header", CLIENT, 26, True)
    node(d, 260, 265, 520, 60, hj, CLIENT, 28)
    label(d, 100, 365, "payload", GREEN, 26, True)
    node(d, 260, 345, 520, 60, pj, GREEN, 26)
    draw.arrow_right(d, 790, 295, 900, CLIENT, 3)
    draw.arrow_right(d, 790, 375, 900, GREEN, 3)
    label(d, 845, 252, "base64url", MUTED, 22, False, "ma")
    node(d, 910, 265, 460, 60, hb, CLIENT, 24)
    node(d, 910, 345, 900, 60, pb, GREEN, 24)
    label(d, 910, 430, "signature = Ed25519( header . payload )", AMBER, 28, True)
    header(d, 100, 500, "AFTER SIGNING: one token, three parts", ROSE)
    f = font(MONO_B, 26)
    x, y = 100, 575
    for text, c in ((hb, CLIENT), (".", TEXT), (pb, GREEN), (".", TEXT), (sb[:22] + "…", AMBER)):
        d.text((x, y), text, font=f, fill=c)
        x += int(f.getlength(text))
    header(d, 100, 660, "THE CHECKS", CYAN)
    verdict(d, 130, 750, has("FOUR", "it verifies: true"), "the token as signed: verifies")
    verdict(d, 130, 820, False, 'header says {"alg":"none"}, no signature: refused')
    caption(d, "The verifier expects EdDSA, always: the token does not get to choose.", AMBER, 900)


def pem_bill(img, d, cfg):
    """PEM armor turns bytes into text; the bill lists what a valid signature does not say."""
    first, last = value("FIVE", "first line"), value("FIVE", "last line")
    header(d, 100, 205, "BEFORE ARMOR", GREEN)
    node(d, 100, 270, 700, 70, "the signed CMS message: binary", GREEN, 28, False)
    label(d, 100, 355, "bytes that email and tickets can break", MUTED, 24)
    draw.arrow_right(d, 820, 305, 940, MUTED, 4)
    label(d, 880, 260, "Base64", MUTED, 22, False, "ma")
    header(d, 960, 205, "AFTER ARMOR (PEM)", ROSE)
    d.rounded_rectangle([960, 265, 1820, 400], radius=12, fill=BG_CELL, outline=ROSE, width=3)
    for i, (t, c) in enumerate(((first, ROSE), ("Base64 text, at most 64 per line", MUTED), (last, ROSE))):
        d.text((990, 280 + i * 40), t, font=font(MONO_B, 26), fill=c)
    header(d, 100, 460, "THE BILL: what a valid signature does not tell you", AMBER)
    cards = [("Who owns the key?", "a chain of certificates", CLIENT),
             ("When was it signed?", "a timestamp authority", CYAN),
             ("Where does the key live?", "a keystore: the next project", GREEN)]
    for i, (q, a, c) in enumerate(cards):
        x = 100 + i * 580
        d.rounded_rectangle([x, 530, x + 540, 760], radius=16, fill=PANEL, outline=c, width=3)
        label(d, x + 270, 575, q, c, 32, True, "ma")
        label(d, x + 270, 650, "needs", MUTED, 26, False, "ma")
        label(d, x + 270, 695, a, TEXT, 28, True, "ma")
    caption(d, "A valid signature proves that a key signed these bytes. Nothing more.", AMBER, 830)
