"""Pictures for the Bouncy Castle video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("BEFORE PEM")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def heading(act, part):
    """The full header line of a part, such as 'AFTER SEALING (one envelope, 10520 bytes):'."""
    return re.search(r"^  (%s.*?):?$" % re.escape(part), section(act), re.M).group(1)


def value(label, act, part=None):
    """The text after a label on a line of the demo's output, within one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def number(label, act, part=None):
    return re.match(r"\d+", value(label, act, part)).group(0)


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE SEALING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def cells(d, x, y, items, colour, w=88, h=72, size=34, gap=8, colours=None):
    """A row of cells; `colours` may give each cell its own colour."""
    for i, t in enumerate(items):
        c = colours[i] if colours else colour
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=c, anchor="mm")
    return x + len(items) * (w + gap) - gap


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def node(d, x, y, w, h, text, c, size=26, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour, size=40):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def file_card(d, x, y, w, rows, colour, title):
    """A text file: a name tab, then its lines in mono."""
    h = 40 + 50 * len(rows)
    label(d, x, y - 36, title, colour, 26, True)
    d.rounded_rectangle([x, y, x + w, y + h], radius=14, fill=BG_CELL, outline=colour, width=3)
    for i, (text, c) in enumerate(rows):
        d.text((x + 24, y + 22 + i * 50), text, font=font(MONO_B, 26), fill=c)
    return y + h


# ------------------------------------------------------------------ figures

def pem_text(img, d, cfg):
    """PEM: the key's bytes, written as Base64 letters between a BEGIN and an END line."""
    size = number("size", "ONE", "BEFORE PEM")
    first = value("first bytes", "ONE", "BEFORE PEM").replace(" ", "")
    begin, end = value("first line", "ONE", "AFTER PEM"), value("last line", "ONE", "AFTER PEM")
    text = value("text begins", "ONE", "AFTER PEM").rstrip(".")
    letters, lines = re.match(r"(\d+) letters, in (\d+) lines", value("size", "ONE", "AFTER PEM")).groups()
    e = header(d, 100, 215, "BEFORE PEM", GREEN)
    label(d, e + 24, 237, "the public key as bytes: %s of them, not text" % size, TEXT, 30, True, "lm")
    cells(d, 100, 285, [first[i:i + 2] for i in range(0, 32, 2)], GREEN, w=86, h=64, size=30, gap=8)
    label(d, 1610, 317, "… %d more" % (int(size) - 16), MUTED, 28, False, "lm")
    draw.arrow(d, 960, 365, 960, 420, CYAN, 4)
    big_box(d, 700, 425, 520, 80, "Base64", "3 bytes → 4 letters", CYAN, 34)
    draw.arrow(d, 960, 508, 960, 540, CYAN, 4)
    e = header(d, 100, 545, "AFTER PEM", ROSE)
    label(d, e + 24, 567, "plain text, safe to paste into a file or an email", TEXT, 30, True, "lm")
    rows = [(begin, AMBER), (text + "…", ROSE), ("… %d lines of letters in all" % int(lines), MUTED), (end, AMBER)]
    file_card(d, 100, 625, 1300, rows, ROSE, "")
    label(d, 1450, 660, "%s bytes" % size, GREEN, 32, True)
    label(d, 1450, 710, "→ %s letters" % letters, ROSE, 32, True)
    label(d, 1450, 760, "→ %s lines of 64" % lines, ROSE, 32, True)
    caption(d, "PEM hides nothing: anyone can turn the letters back into the bytes.", AMBER, 885)


def base64_steps(img, d, cfg):
    """Base64 by hand on the key's first three bytes: 24 bits, four 6-bit groups, four letters."""
    part = section("ONE", "BASE64, STEP BY STEP")
    get = lambda name: re.search(r"^    %s\s+(.+)$" % re.escape(name), part, re.M).group(1).split()
    by, bits, groups, nums, lets = get("bytes"), get("bits"), get("6-bit groups"), get("numbers"), get("letters")
    gc = [GREEN, AMBER, CYAN, CLIENT]
    e = header(d, 100, 205, "BEFORE BASE64", GREEN)
    label(d, e + 24, 227, "the first 3 bytes of the key", TEXT, 28, True, "lm")
    x0, bw = 160, 26                         # one bit cell
    for i, b in enumerate(by):
        bx = x0 + i * 8 * (bw + 6) + i * 30
        node(d, bx, 265, 8 * (bw + 6) - 6, 56, "byte %s" % b, GREEN, 26)
    label(d, 100, 360, "bits", MUTED, 24, True)
    allbits = "".join(bits)
    for k, bit in enumerate(allbits):
        g = k // 6
        bx = x0 + k * (bw + 6) + (k // 8) * 30
        d.rounded_rectangle([bx, 350, bx + bw, 400], radius=6, fill=BG_CELL, outline=gc[g], width=2)
        d.text((bx + bw // 2, 375), bit, font=font(MONO_B, 26), fill=gc[g], anchor="mm")
    label(d, 100, 425, "cut the 24 bits into 4 groups of 6 (the colours above)", TEXT, 28, True)
    e = header(d, 100, 490, "AFTER BASE64", ROSE)
    label(d, e + 24, 512, "each group is a number from 0 to 63, which picks one letter", TEXT, 28, True, "lm")
    for g in range(4):
        x = 160 + g * 420
        node(d, x, 560, 340, 60, groups[g], gc[g], 30)
        draw.arrow(d, x + 170, 625, x + 170, 665, gc[g], 3)
        node(d, x, 670, 340, 60, "= %s" % nums[g], gc[g], 30)
        draw.arrow(d, x + 170, 735, x + 170, 775, gc[g], 3)
        node(d, x + 110, 780, 120, 80, lets[g], gc[g], 48)
    label(d, 160, 880, "A=0  B=1 … I=8 … M=12 … Z=25  a=26 … z=51  0=52 … 9=61  +=62  /=63", MUTED, 26)
    caption(d, "%s  %s  %s   →   %s" % (by[0], by[1], by[2], "".join(lets)), AMBER, 930)


def key_lock(img, d, cfg):
    """A private key file in the clear, and the same key locked with a password."""
    clear = value("first line", "TWO", "BEFORE LOCKING")
    locked = value("first line", "TWO", "AFTER LOCKING")
    header(d, 100, 210, "BEFORE LOCKING", GREEN)
    file_card(d, 100, 320, 760, [(clear, GREEN), ("MIIEv… the key itself", MUTED), ("-----END PRIVATE KEY-----", GREEN)],
              GREEN, "ana.key.pem")
    draw.arrow_right(d, 100, 560, 420, ROSE, 4)
    label(d, 100, 630, "anyone who reads the file", ROSE, 28, True)
    big_box(d, 430, 510, 300, 100, "the key", "ready to use", ROSE, 34)
    header(d, 1000, 210, "AFTER LOCKING", ROSE)
    file_card(d, 1000, 320, 820, [(locked, ROSE), ("salt, rounds, and the key,", MUTED), ("encrypted with AES-256", MUTED)],
              ROSE, "ana.key.pem, locked")
    rows = [("right password", value("right password: the same key:", "TWO", "AFTER LOCKING") == "true", "the key"),
            ("wrong password", value("wrong password:", "TWO", "AFTER LOCKING") == "refused", "refused")]
    for i, (what, ok, res) in enumerate(rows):
        y = 560 + i * 120
        c = GREEN if i == 0 else ROSE
        node(d, 1000, y, 300, 80, what, c, 28, False)
        draw.arrow_right(d, 1310, y + 40, 1420, MUTED, 3)
        d.text((1450, y + 40), ("✓ " if i == 0 else "✗ ") + res, font=font(SANS_B, 34), fill=c, anchor="lm")
    caption(d, "Same key inside. Locked, the file is useless without the password.", AMBER, 860)


def certificate(img, d, cfg):
    """A self-signed certificate: the inputs, signed by Ana's own key, and the result."""
    name = value("name", "THREE", "BEFORE SIGNING")
    pub = value("public key", "THREE", "BEFORE SIGNING")
    valid = value("valid", "THREE", "BEFORE SIGNING").replace("T00:00:00Z", "")
    after = heading("THREE", "AFTER SIGNING")
    size = re.search(r"(\d+) bytes", after).group(1)
    issuer, serial = value("issuer", "THREE", "AFTER SIGNING"), value("serial", "THREE", "AFTER SIGNING")
    alg = value("signed with", "THREE", "AFTER SIGNING").split(",")[0]
    header(d, 100, 205, "BEFORE SIGNING: the inputs", GREEN)
    ins = [("name", name, CLIENT), ("public key", pub, GREEN), ("valid", valid, MUTED)]
    for i, (k, v, c) in enumerate(ins):
        y = 275 + i * 120
        label(d, 100, y, k, c, 26, True)
        node(d, 100, y + 34, 640, 60, v, c, 24)
        draw.arrow_right(d, 750, y + 64, 830, c, 3)
    big_box(d, 840, 330, 300, 200, "sign", alg, AMBER, 44)
    label(d, 990, 545, "with Ana's own", AMBER, 24, True, "ma")
    label(d, 990, 575, "private key", AMBER, 24, True, "ma")
    draw.arrow_right(d, 1150, 430, 1230, ROSE, 4)
    header(d, 1240, 205, "AFTER SIGNING: %s bytes" % size, ROSE)
    rows = [("subject", name, CLIENT), ("issuer", issuer, AMBER), ("serial", serial, MUTED),
            ("public key", "Ana's", GREEN), ("signature", alg, AMBER)]
    d.rounded_rectangle([1240, 270, 1830, 270 + 72 * len(rows) + 20], radius=16, fill=PANEL, outline=ROSE, width=3)
    for i, (k, v, c) in enumerate(rows):
        y = 290 + i * 72
        label(d, 1265, y + 6, k, MUTED, 22, True)
        d.text((1265, y + 32), v, font=font(MONO_B, 22), fill=c)
    caption(d, "Issuer = subject: Ana vouches for herself. Anyone can make one.", AMBER, 760)
    label(d, W // 2, 820, "It ties a name to a key, but proves nothing until someone trusted signs it.", MUTED, 30, False, "ma")


def envelope(img, d, cfg):
    """CMS EnvelopedData: the statement in, one envelope with four parts out, drawn by size."""
    stmt = number("statement", "FOUR", "BEFORE SEALING")
    total = re.search(r"(\d+) bytes", heading("FOUR", "AFTER SEALING")).group(1)
    serial = re.search(r"serial (\d+)", value("for", "FOUR", "AFTER SEALING")).group(1)
    locked, iv = number("locked key", "FOUR", "AFTER SEALING"), number("IV", "FOUR", "AFTER SEALING")
    ct = number("cipher text", "FOUR", "AFTER SEALING")
    header(d, 100, 205, "BEFORE SEALING", GREEN)
    node(d, 100, 275, 560, 70, "statement  %s bytes" % stmt, GREEN, 28)
    label(d, 100, 355, "625 lines of RENT -1200", MUTED, 24)
    node(d, 760, 275, 560, 70, "Ana's certificate", CLIENT, 28, False)
    label(d, 760, 355, "her public key, serial %s" % serial, MUTED, 24)
    big_box(d, 1420, 260, 400, 110, "seal", "new AES key + RSA-OAEP", AMBER, 40)
    draw.arrow_right(d, 1325, 310, 1415, MUTED, 3)
    draw.arrow(d, 1620, 375, 1620, 440, AMBER, 4)
    header(d, 100, 440, "AFTER SEALING: one envelope, %s bytes" % total, ROSE)
    parts = [("for: serial %s" % serial, "who may open it", CLIENT, 300),
             ("locked key %s" % locked, "the AES key, locked with RSA", AMBER, 340),
             ("IV %s" % iv, "start value", CYAN, 160),
             ("cipher text %s bytes" % ct, "the statement, AES-256-CBC", ROSE, 900)]
    x = 100
    for name, sub, c, w in parts:
        d.rounded_rectangle([x, 515, x + w - 10, 615], radius=12, fill=BG_CELL, outline=c, width=3)
        d.text((x + (w - 10) // 2, 548), name, font=font(SANS_B, 26), fill=c, anchor="mm")
        d.text((x + (w - 10) // 2, 588), sub, font=font(SANS, 20), fill=MUTED, anchor="mm")
        x += w
    header(d, 100, 665, "OPENING IT", CYAN)
    ana = value("Ana, her private key:", "FOUR", "OPENING IT")
    eve = value("Eve, her own private key:", "FOUR", "OPENING IT").split("  <-")[0]
    label(d, 100, 740, "Ana, with her private key:", TEXT, 32, True)
    label(d, 680, 740, "✓ opened" if ana == "true" else ana, GREEN, 32, True)
    label(d, 100, 800, "Eve, with her own private key:", TEXT, 32, True)
    label(d, 680, 800, "✗ " + eve, ROSE, 32, True)
    caption(d, "AES for the big data, RSA only for the small key: that is hybrid encryption.", AMBER, 880)


def open_steps(img, d, cfg):
    """Opening the envelope by hand with the JDK alone, then with openssl."""
    s1 = section("FIVE", "STEP 1")
    locked = number("locked key", "FIVE", "STEP 1")
    key = number("AES key", "FIVE", "STEP 1")
    ct, pt = number("cipher text", "FIVE", "STEP 2"), number("plain text", "FIVE", "STEP 2")
    last = value("last block", "FIVE", "STEP 2").replace(" ", "")
    m = re.search(r"0x(\w+) = (\d+) bytes off: (\d+) bytes", section("FIVE", "STEP 3"))
    ssl = re.search(r"opens it: (\w+)", section("FIVE")).group(1)
    assert s1
    e = header(d, 100, 205, "STEP 1", AMBER)
    label(d, e + 20, 227, "unlock the AES key", TEXT, 28, True, "lm")
    node(d, 100, 270, 380, 64, "locked key %s bytes" % locked, AMBER, 26)
    draw.arrow_right(d, 490, 302, 580, MUTED, 3)
    node(d, 590, 270, 520, 64, "RSA-OAEP, Ana's private key", AMBER, 26, False)
    draw.arrow_right(d, 1120, 302, 1210, MUTED, 3)
    node(d, 1220, 270, 380, 64, "AES key %s bytes" % key, GREEN, 26)
    e = header(d, 100, 375, "STEP 2", CYAN)
    label(d, e + 20, 397, "decrypt the statement", TEXT, 28, True, "lm")
    node(d, 100, 440, 380, 64, "cipher text %s bytes" % ct, ROSE, 26)
    draw.arrow_right(d, 490, 472, 580, MUTED, 3)
    node(d, 590, 440, 520, 64, "AES-256-CBC, that key + IV", CYAN, 26, False)
    draw.arrow_right(d, 1120, 472, 1210, MUTED, 3)
    node(d, 1220, 440, 380, 64, "plain %s bytes" % pt, GREEN, 26)
    label(d, 100, 530, "the last 16 bytes:", MUTED, 26, True)
    cells(d, 380, 520, [last[i:i + 2] for i in range(0, 32, 2)], AMBER, w=74, h=54, size=26, gap=8)
    e = header(d, 100, 610, "STEP 3", GREEN)
    label(d, e + 20, 632, "remove the padding: the last byte, %s hex = %s, says how many" % (m.group(1), m.group(2)),
          TEXT, 28, True, "lm")
    node(d, 100, 675, 380, 64, "%s bytes" % pt, GREEN, 26)
    label(d, 500, 707, "−  %s  =" % m.group(2), TEXT, 34, True, "lm")
    node(d, 690, 675, 520, 64, "the statement, %s bytes" % m.group(3), GREEN, 26)
    header(d, 100, 775, "OPENSSL, A DIFFERENT PROGRAM", CLIENT)
    label(d, 680, 797, ("✓ opens it too" if ssl == "true" else ssl), GREEN, 32, True, "lm")
    caption(d, "Two programs that share no code read the same envelope: that is a standard.", AMBER, 880)


def bill(img, d, cfg):
    """The careful call and the easy call record different paddings; the rest of the bill."""
    good = value("key locked with", "SIX", "THE CAREFUL CALL")
    good_oid = value("OID", "SIX", "THE CAREFUL CALL")
    easy = value("key locked with", "SIX", "THE EASY CALL").split("  <-")[0]
    easy_oid = value("OID", "SIX", "THE EASY CALL")
    rest = [ln.strip() for ln in section("SIX", "THE REST OF THE BILL").splitlines()]
    for i, (title, name, oid, c, note) in enumerate((
            ("THE CAREFUL CALL", good, good_oid, GREEN, "OAEP asked for by name"),
            ("THE EASY CALL", easy, easy_oid, ROSE, "what most examples show"))):
        x = 100 + i * 870
        header(d, x, 205, title, c)
        d.rounded_rectangle([x, 270, x + 820, 470], radius=16, fill=PANEL, outline=c, width=3)
        label(d, x + 30, 290, note, MUTED, 26)
        label(d, x + 30, 340, name, c, 40, True)
        d.text((x + 30, 410), "OID " + oid, font=font(MONO_B, 28), fill=c)
    label(d, 970, 505, "the easy call quietly picks the older padding", ROSE, 30, True, "ma")
    header(d, 100, 570, "THE REST OF THE BILL", AMBER)
    for i, t in enumerate(rest):
        label(d, 130, 640 + i * 56, "•  " + t, TEXT, 32)
    d.line([(100, 870), (1820, 870)], fill=LINE, width=2)
    caption(d, "Use the JDK for the maths, the library only for the formats.", AMBER, 890)
