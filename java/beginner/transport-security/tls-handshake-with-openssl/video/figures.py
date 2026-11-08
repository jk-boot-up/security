"""Pictures for the TLS handshake with OpenSSL video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("BEFORE PEM")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"[\s:]+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUEST: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def cells(d, x, y, items, colour, w=88, h=72, size=34, gap=8):
    for i, t in enumerate(items):
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=8, fill=BG_CELL, outline=colour, width=3)
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


def mark(d, x, y, ok):
    d.text((x, y), "✓" if ok else "✗", font=font(SANS_B, 48), fill=GREEN if ok else ROSE, anchor="mm")


# ------------------------------------------------------------------ figures

def two_programs(img, d, cfg):
    """The core idea on real values: Java and openssl, written by different people, agree the same handshake."""
    proto = value("protocol", "ONE", "THE RESPONSE")
    suite = value("cipher suite", "ONE", "THE RESPONSE")
    reply = value("reply", "ONE", "THE RESPONSE")
    seen = section("FIVE", "THE RESPONSE")
    verified = re.search(r"Verification: (\w+)", seen).group(1)
    rows = [("Java client", "Java server", "the demo", CLIENT),
            ("openssl s_client", "Java server", "a test", AMBER),
            ("Java client", "openssl s_server", "a test", GREEN)]
    label(d, 100, 215, "client", MUTED, 26, True)
    label(d, 1230, 215, "server", MUTED, 26, True)
    for i, (cl, sv, who, c) in enumerate(rows):
        y = 262 + i * 130
        d.rounded_rectangle([100, y, 520, y + 96], radius=16, fill=PANEL, outline=c, width=4)
        d.text((310, y + 48), cl, font=font(SANS_B, 30), fill=c, anchor="mm")
        draw.arrow_right(d, 530, y + 48, 1210, c, 4)
        label(d, 870, y + 10, "TLS handshake  (%s)" % who, MUTED, 22, True, "ma")
        d.rounded_rectangle([1230, y, 1650, y + 96], radius=16, fill=PANEL, outline=c, width=4)
        d.text((1440, y + 48), sv, font=font(SANS_B, 30), fill=c, anchor="mm")
    header(d, 100, 665, "EVERY PAIR AGREES", GREEN)
    for k, (key, val) in enumerate((("protocol", proto), ("cipher suite", suite), ("reply", reply),
                                    ("openssl verification", verified))):
        x, y = 100 + (k % 2) * 860, 730 + (k // 2) * 72
        label(d, x, y + 28, key, MUTED, 26, True, "lm")
        field(d, x + 300, y, 520, 56, val, GREEN, 24)
    caption(d, "Two programs by different people agree: the setup follows the standard.", AMBER, 900)


def socket_exchange(img, d, cfg):
    """One order over a real socket: the request, the handshake, the response."""
    req = section("ONE", "THE REQUEST")
    to = value("connects to", "ONE", "THE REQUEST")
    sends = value("sends", "ONE", "THE REQUEST")
    works = value("handshake works", "ONE", "THE RESPONSE")
    proto = value("protocol", "ONE", "THE RESPONSE")
    suite = value("cipher suite", "ONE", "THE RESPONSE")
    reply = value("reply", "ONE", "THE RESPONSE")
    header(d, 100, 205, "THE REQUEST: the Java client", GREEN)
    for i, (k, v) in enumerate((("connects to", to), ("sends", sends))):
        y = 275 + i * 84
        label(d, 100, y + 30, k, MUTED, 26, True, "lm")
        field(d, 300, y, 560, 60, v, CLIENT, 24)
    big = (960, 250, 1820, 430)
    d.rounded_rectangle(big, radius=18, fill=PANEL, outline=AMBER, width=4)
    label(d, 1390, 280, "the handshake, over the loopback", AMBER, 28, True, "ma")
    label(d, 1390, 330, "ClientHello  ->  ServerHello, certificate", TEXT, 26, False, "ma")
    label(d, 1390, 375, "the client checks: trusted? the name?", TEXT, 26, False, "ma")
    draw.arrow(d, 960, 450, 960, 500, MUTED, 4)
    header(d, 100, 515, "THE RESPONSE: the Java server", ROSE)
    for i, (k, v) in enumerate((("handshake works", works), ("protocol", proto), ("cipher suite", suite),
                                ("reply", reply))):
        x, y = 100 + (i % 2) * 860, 585 + (i // 2) * 84
        label(d, x, y + 30, k, MUTED, 26, True, "lm")
        field(d, x + 290, y, 520, 60, v, GREEN, 24)
    caption(d, "Real sockets, real certificates: anything that speaks TLS could take either side.", TEXT, 850)


def pem_steps(img, d, cfg):
    """How PEM text is made: 3 bytes, 24 bits, 4 groups of 6, 4 letters; then lines of 64 between labels."""
    before = section("TWO", "BEFORE PEM")
    size, first = re.search(r"(\d+) bytes, starting\s+((?:\w\w ?){3})", before).groups()
    bits = value("bits", "TWO").split()
    groups_line = value("groups", "TWO")
    groups = groups_line.split("=")[0].split()
    numbers = groups_line.split("=")[1].split()
    letters = list(value("letters", "TWO").split("(")[0].strip())
    letters_total = re.search(r"STEP 2\. (\d+) LETTERS", section("TWO")).group(1)
    after = section("TWO", "AFTER PEM")
    lines = [l.strip() for l in after.strip().split("\n")]
    n_lines = re.search(r"AFTER PEM \(shop-cert.pem, (\d+) lines\)", section("TWO")).group(1)
    header(d, 100, 205, "BEFORE PEM: %s DER bytes" % size, GREEN)
    label(d, 100, 285, "bytes", MUTED, 26, True, "lm")
    for i, (hx, b) in enumerate(zip(first.split(), bits)):
        x = 330 + i * 300
        field(d, x, 255, 280, 60, hx, CLIENT, 30)
        label(d, x + 140, 335, b, TEXT, 26, True, "ma")
    label(d, 100, 350, "24 bits", MUTED, 26, True, "lm")
    label(d, 100, 425, "6-bit groups", MUTED, 26, True, "lm")
    label(d, 100, 505, "numbers", MUTED, 26, True, "lm")
    label(d, 100, 585, "letters", MUTED, 26, True, "lm")
    for i, (g, n, ch) in enumerate(zip(groups, numbers, letters)):
        x = 330 + i * 225
        field(d, x, 395, 205, 60, g, AMBER, 26)
        field(d, x, 475, 205, 60, n, AMBER, 28)
        field(d, x, 555, 205, 60, ch, ROSE, 34)
    label(d, 1260, 410, "STEP 1. Base64", AMBER, 28, True)
    label(d, 1260, 455, "0 = A ... 25 = Z", MUTED, 26)
    label(d, 1260, 495, "26 = a ... 51 = z", MUTED, 26)
    label(d, 1260, 535, "52 = 0 ... 63 = /", MUTED, 26)
    label(d, 100, 660, "STEP 2. %s bytes -> %s letters, in lines of 64, between two labels" % (size, letters_total), AMBER, 28, True)
    header(d, 100, 715, "AFTER PEM: shop-cert.pem, %s lines" % n_lines, ROSE)
    d.rounded_rectangle([100, 770, 1000, 890], radius=10, fill=BG_CELL, outline=ROSE, width=3)
    for k, ln in enumerate(lines):
        label(d, 120, 778 + k * 36, ln, TEXT if k != 1 else ROSE, 26)
    label(d, 1040, 830, "openssl reads what Java wrote", GREEN, 30, True, "lm")


def identity_checks(img, d, cfg):
    """Three servers answer; the client checks the certificate is trusted, and the name matches."""
    asks = value("asks for", "THREE", "THE REQUEST")
    trusts = value("trusts", "THREE", "THE REQUEST").split()[-1]
    after = section("THREE", "THE RESPONSE")
    works = re.findall(r"works: (\w+)", after)
    fake = re.search(r"impostor, same name, certificate (\w+)", after).group(1)
    other = re.search(r"asked for (\S+)", after).group(1)
    header(d, 100, 205, "THE REQUEST: the client", GREEN)
    label(d, 100, 275, "asks for", MUTED, 26, True)
    field(d, 240, 262, 520, 56, asks, CLIENT, 26)
    label(d, 800, 275, "trusts the certificate", MUTED, 26, True)
    field(d, 1110, 262, 520, 56, trusts, CLIENT, 26)
    header(d, 100, 360, "THE RESPONSE: three servers, over sockets", ROSE)
    label(d, 760, 425, "certificate", MUTED, 24, True, "ma")
    label(d, 1180, 425, "trusted?", MUTED, 24, True, "ma")
    label(d, 1390, 425, "name matches?", MUTED, 24, True, "ma")
    label(d, 1680, 425, "works", MUTED, 24, True, "ma")
    rows = [("the shop", "asked " + asks, trusts, True, True, works[0]),
            ("an impostor", "asked " + asks, fake, False, True, works[1]),
            ("the shop", "asked " + other, trusts, True, False, works[2])]
    for i, (who, sub, fp, trusted, named, ok) in enumerate(rows):
        y = 465 + i * 110
        c = GREEN if ok == "true" else ROSE
        d.rounded_rectangle([100, y, 1820, y + 90], radius=14, fill=PANEL, outline=c, width=3)
        label(d, 125, y + 30, who, TEXT, 26, True, "lm")
        label(d, 125, y + 64, sub, MUTED, 22, False, "lm")
        field(d, 590, y + 15, 340, 60, fp, CLIENT if trusted else ROSE, 24)
        mark(d, 1180, y + 45, trusted)
        mark(d, 1390, y + 45, named)
        label(d, 1680, y + 45, ok, c, 30, True, "mm")
    caption(d, "openssl makes the same two checks with -CAfile and -verify_hostname.", AMBER, 830)


def version_matrix(img, d, cfg):
    """Which client offers work against a default server, and against one that allows only TLS 1.3."""
    panels = [("BEFORE TIGHTENING: JDK defaults", section("FOUR", "BEFORE TIGHTENING"), GREEN),
              ("AFTER TIGHTENING: TLS 1.3 only", section("FOUR", "AFTER TIGHTENING"), ROSE)]
    for i, (title, text, c) in enumerate(panels):
        x = 100 + i * 880
        header(d, x, 205, title, c)
        for j, (ver, ok, agreed) in enumerate(re.findall(r"offers (\S+)\s+works: (\w+)(?:, agreed (\S+))?", text)):
            y = 285 + j * 110
            good = ok == "true"
            d.rounded_rectangle([x, y, x + 820, y + 88], radius=14, fill=PANEL, outline=GREEN if good else ROSE, width=3)
            label(d, x + 24, y + 44, "client offers", MUTED, 26, False, "lm")
            label(d, x + 210, y + 44, ver, TEXT, 30, True, "lm")
            label(d, x + 720, y + 44, ("agreed " + agreed) if good else "refused", GREEN if good else ROSE, 28, True, "rm")
            mark(d, x + 770, y + 44, good)
    label(d, 100, 560, "the same offers from openssl:", MUTED, 28, True)
    for k, (flag, ver) in enumerate((("-tls1_1", "TLSv1.1"), ("-tls1_2", "TLSv1.2"), ("-tls1_3", "TLSv1.3"))):
        field(d, 100 + k * 420, 610, 380, 64, "s_client %s" % flag, AMBER, 26)
        label(d, 290 + k * 420, 690, "offers %s" % ver, MUTED, 24, True, "ma")
    caption(d, "Old versions stay off. Allowing only TLS 1.3 also shuts out TLS 1.2 clients.", AMBER, 820)


def openssl_check(img, d, cfg):
    """openssl as the client: the command, the lines it prints, and the bill."""
    cmd = [l.strip() for l in section("FIVE", "THE REQUEST").strip().split("\n")]
    seen = [l.strip() for l in section("FIVE", "THE RESPONSE").strip().split("\n")]
    bill = [l.strip() for l in section("FIVE", "THE BILL").strip().split("\n")]
    header(d, 100, 205, "THE REQUEST: the command", GREEN)
    d.rounded_rectangle([100, 265, 1100, 265 + 46 * len(cmd) + 24], radius=10, fill=BG_CELL, outline=GREEN, width=3)
    for k, ln in enumerate(cmd):
        d.text((124 + (0 if k == 0 else 40), 280 + k * 46), ln, font=font(MONO_B, 26), fill=TEXT)
    header(d, 1160, 205, "THE RESPONSE: what it prints", ROSE)
    for k, ln in enumerate(seen):
        y = 265 + k * 62
        field(d, 1160, y, 600, 52, ln, GREEN, 22)
    label(d, 100, 445, "-verify_hostname: the name must match", AMBER, 26, True)
    label(d, 100, 485, "-verify_return_error: stop on a failed check", AMBER, 26, True)
    header(d, 100, 560, "THE BILL", AMBER)
    for k, ln in enumerate(bill):
        label(d, 130, 620 + k * 46, "· " + ln, TEXT, 28)
    caption(d, "openssl checks only what the command asks. Check its exit status too.", AMBER, 880)
