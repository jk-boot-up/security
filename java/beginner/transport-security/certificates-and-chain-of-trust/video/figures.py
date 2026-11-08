"""Pictures for the certificates and chain of trust video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("BEFORE THE CHECK")."""
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


def card(d, x, y, w, h, title, lines, colour):
    """A certificate drawn as a card: a title and a few lines."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=4)
    label(d, x + 20, y + 16, title, colour, 28, True)
    for k, ln in enumerate(lines):
        label(d, x + 20, y + 60 + k * 36, ln, TEXT, 24)


def chain_rows():
    """The three certificates of act ONE: (role, name, fact)."""
    text = section("ONE", "THE CHAIN")
    return [re.match(r"\s*(\w+)\s+(.+?)\s{2,}(.+)$", l).groups() for l in text.strip().split("\n")]


# ------------------------------------------------------------------ figures

def chain_core(img, d, cfg):
    """The core idea on real values: trust flows down from a root the browser already has."""
    rows = chain_rows()
    store = value("already trusted", "TWO", "BEFORE THE CHECK")
    verdict = value("by hand", "TWO", "AFTER THE CHECK")
    label(d, 100, 215, "the browser's trust store", MUTED, 26, True)
    card(d, 100, 260, 420, 150, "trusted root", [store, "installed with the browser"], GREEN)
    colours = [GREEN, AMBER, CLIENT]
    for i, (role, name, fact) in enumerate(rows):
        y = 230 + i * 210
        card(d, 760, y, 640, 150, "%s: %s" % (role, name), [fact], colours[i])
        if i:
            draw.arrow(d, 1080, y - 58, 1080, y - 4, colours[i - 1], 4)
            label(d, 1100, y - 50, "signs", colours[i - 1], 24, True)
    draw.arrow_right(d, 530, 325, 750, GREEN, 4)
    label(d, 640, 290, "the same key", GREEN, 22, True, "ma")
    d.rounded_rectangle([1450, 650, 1820, 760], radius=16, fill=PANEL, outline=CLIENT, width=3)
    label(d, 1635, 672, "the browser checks", MUTED, 24, True, "ma")
    label(d, 1635, 712, "upwards, to the root", CLIENT, 26, True, "ma")
    draw.arrow_left(d, 1445, 705, 1405, CLIENT, 4)
    label(d, W // 2, 875, "by hand: " + verdict, TEXT, 28, True, "ma")
    caption(d, "A certificate is trusted only because a key the browser already trusts signed its way down.", AMBER, 925)


def certificate_card(img, d, cfg):
    """What the shop asks for, and what signing adds."""
    before = section("ONE", "BEFORE SIGNING")
    after = section("ONE", "AFTER SIGNING")
    name = value("name", "ONE", "BEFORE SIGNING")
    key = value("public key", "ONE", "BEFORE SIGNING")
    valid = value("valid", "ONE", "BEFORE SIGNING")
    issuer = re.search(r"issuer\s+(.+)$", after, re.M).group(1)
    sig = value("signature", "ONE", "AFTER SIGNING")
    checks = value("anyone checks it with the issuer's public key", "ONE", "AFTER SIGNING")
    serial = re.search(r"serial (\d+)", section("ONE")).group(1)
    header(d, 100, 205, "BEFORE SIGNING: the shop asks", GREEN)
    for i, (k, v) in enumerate((("name", name), ("public key", key), ("valid", valid))):
        y = 275 + i * 80
        label(d, 100, y + 28, k, MUTED, 26, True, "lm")
        field(d, 280, y, 560, 58, v if len(v) < 34 else v.split(",")[0], CLIENT, 22)
    draw.arrow_right(d, 860, 390, 980, AMBER, 5)
    label(d, 920, 340, "the issuer", AMBER, 24, True, "ma")
    label(d, 920, 420, "signs", AMBER, 24, True, "ma")
    header(d, 1000, 205, "AFTER SIGNING: certificate, serial %s" % serial, ROSE)
    d.rounded_rectangle([1000, 270, 1820, 640], radius=16, fill=PANEL, outline=ROSE, width=4)
    for i, (k, v, c) in enumerate((("name", name.split(",")[0], CLIENT), ("public key", key.split(",")[0] + ", " + key.split(",")[1].strip(), CLIENT),
                                   ("valid", valid, CLIENT), ("issuer", issuer, AMBER), ("signature", sig.split(",")[0], AMBER))):
        y = 295 + i * 66
        label(d, 1025, y + 22, k, MUTED, 24, True, "lm")
        label(d, 1200, y + 22, v, c, 26, True, "lm")
    label(d, 1410, 600, "the same three, plus the issuer and its signature", MUTED, 22, False, "ma")
    label(d, 100, 560, "anyone checks the signature", TEXT, 28, True)
    label(d, 100, 600, "with the issuer's public key: %s" % checks, GREEN, 28, True)
    caption(d, "Change any byte and the check fails. The private key never leaves the issuer.", TEXT, 720)


def check_steps(img, d, cfg):
    """The browser's check, step by step, with each check's result."""
    two = section("TWO")
    steps = re.findall(r"STEP (\d)\. (.+?) AGAINST (.+?):\n((?:    .*\n?)*)", two)
    by_hand = value("by hand", "TWO", "AFTER THE CHECK")
    root = re.search(r"    (.+) is in the trust store: (\w+)", two).groups()
    name = re.search(r"    (\S+) is in the certificate: (\w+)", two).groups()
    cols = ["names it as issuer", "signature checks", "inside its dates", "may issue (CA)"]
    for k, c in enumerate(cols):
        label(d, 900 + k * 230, 215, c, MUTED, 22, True, "ma")
    y = 260
    for n, cert, against, body in steps:
        vals = re.findall(r": (true|false)$", body, re.M)
        d.rounded_rectangle([100, y, 1820, y + 100], radius=14, fill=PANEL, outline=CLIENT, width=3)
        label(d, 125, y + 32, "step %s: %s" % (n, cert), TEXT, 28, True, "lm")
        label(d, 125, y + 70, "checked against %s" % against, MUTED, 22, False, "lm")
        for k in range(4):
            if k < len(vals):
                mark(d, 900 + k * 230, y + 50, vals[k] == "true")
            else:
                label(d, 900 + k * 230, y + 50, "not needed", MUTED, 22, False, "mm")
        y += 125
    for text, ok in (("step 3: %s is in the trust store" % root[0], root[1]), ("step 3: %s is in the certificate" % name[0], name[1])):
        d.rounded_rectangle([100, y, 1820, y + 70], radius=14, fill=PANEL, outline=GREEN, width=3)
        label(d, 125, y + 35, text, TEXT, 28, True, "lm")
        mark(d, 1590, y + 35, ok == "true")
        y += 90
    header(d, 100, y + 10, "AFTER THE CHECK", ROSE)
    label(d, 450, y + 32, by_hand, GREEN, 30, True, "lm")
    caption(d, "Every link must hold. The name is a separate check, after the chain.", AMBER, 900)


def refusals(img, d, cfg):
    """Three chains, the step that fails in each, and the JDK's reason."""
    before = [l.strip()[3:] for l in section("THREE", "BEFORE THE CHECK").strip().split("\n")]
    after = section("THREE", "AFTER THE CHECK")
    v1 = re.search(r"1\. by hand: (.+?)\s+JDK: (\S+)", after).groups()
    v2 = re.search(r"2\. by hand: (.+?)\s+JDK: (\S+)", after).groups()
    v3 = re.search(r"3\. chain accepted (\w+), the name (\S+) matches (\w+)", after).groups()
    header(d, 100, 205, "BEFORE THE CHECK: three chains", GREEN)
    header(d, 1060, 205, "AFTER THE CHECK: the failed step", ROSE)
    results = [("fails at: " + v1[0], "JDK: " + v1[1]), ("fails at: " + v2[0], "JDK: " + v2[1]),
               ("chain accepted %s" % v3[0], "name %s matches: %s" % (v3[1], v3[2]))]
    for i, (b, (r1, r2)) in enumerate(zip(before, results)):
        y = 275 + i * 150
        d.rounded_rectangle([100, y, 960, y + 120], radius=14, fill=PANEL, outline=CLIENT, width=3)
        label(d, 125, y + 60, "%d. %s" % (i + 1, b), TEXT, 23, True, "lm")
        draw.arrow_right(d, 970, y + 60, 1050, ROSE, 4)
        d.rounded_rectangle([1060, y, 1820, y + 120], radius=14, fill=PANEL, outline=ROSE, width=3)
        label(d, 1085, y + 40, r1, ROSE, 28, True, "lm")
        label(d, 1085, y + 84, r2, AMBER, 24, True, "lm")
    caption(d, "The rogue root has the real root's name. Names prove nothing; only the key's signature does.", AMBER, 760)


def tamper_overstep(img, d, cfg):
    """One changed letter breaks the signature; a server key cannot issue certificates."""
    four = section("FOUR")
    before = re.search(r"BEFORE THE CHANGE \(the signed name\):\s+(\S+)", four).group(1)
    after = re.search(r"AFTER THE CHANGE \(one letter\):\s+(\S+)", four).group(1)
    v = re.findall(r"by hand: (.+?)\s+JDK: (\S+)", four)
    ca = value("the shop's certificate is a CA", "FOUR", "BEFORE THE CHECK")
    path = re.search(r"allows (\d+) more CAs", four).group(1)
    header(d, 100, 205, "BEFORE THE CHANGE", GREEN)
    cells(d, 100, 270, list(before), GREEN, w=52, h=64, size=30, gap=6)
    header(d, 100, 360, "AFTER THE CHANGE: one letter", ROSE)
    cells(d, 100, 425, [ch for ch in after], ROSE, w=52, h=64, size=30, gap=6)
    diff = next(i for i, (a, b) in enumerate(zip(before, after)) if a != b)
    d.rounded_rectangle([100 + diff * 58 - 6, 419, 100 + diff * 58 + 58, 495], radius=8, outline=AMBER, width=5)
    label(d, 100, 520, "fails at: %s    JDK: %s" % v[0], ROSE, 28, True)
    header(d, 1060, 205, "BEFORE THE CHECK: a server key signs", GREEN)
    card(d, 1060, 270, 380, 110, "shop.example", ["CA: %s" % ca], CLIENT)
    draw.arrow(d, 1250, 386, 1250, 440, ROSE, 4)
    card(d, 1060, 445, 380, 80, "other.example", [], ROSE)
    label(d, 1470, 470, "signed by the", MUTED, 24, True)
    label(d, 1470, 500, "shop's own key", MUTED, 24, True)
    header(d, 1060, 560, "AFTER THE CHECK", ROSE)
    label(d, 1060, 630, "fails at: %s" % v[1][0], ROSE, 28, True)
    label(d, 1060, 670, "JDK: %s" % v[1][1], AMBER, 26, True)
    label(d, 100, 640, "the signature covers every byte,", TEXT, 28)
    label(d, 100, 680, "so no letter can change unseen", TEXT, 28)
    caption(d, "Only a CA may sign certificates, and the intermediate allows %s more CAs below it." % path, AMBER, 820)


def revocation(img, d, cfg):
    """A stolen intermediate key forges a chain; revoking the intermediate's serial stops it."""
    before = section("FIVE", "BEFORE REVOCATION")
    after = section("FIVE", "AFTER REVOCATION")
    serial = re.search(r"serial (\d+)", before).group(1)
    forged_ok = value("the forged chain accepted", "FIVE", "BEFORE REVOCATION").split()[0]
    rows = re.findall(r"serial (\d+) \((.+?)\)\n\s+the shop's chain accepted (\w+), (?:the forged (\w+)|(\w+))", after)
    header(d, 100, 205, "BEFORE REVOCATION: a thief has the intermediate's key", GREEN)
    card(d, 100, 270, 520, 110, "Example Issuing CA", ["its private key: stolen"], ROSE)
    draw.arrow_right(d, 630, 325, 760, ROSE, 4)
    card(d, 770, 270, 520, 110, "shop.example, serial %s" % serial, ["issued by the thief"], ROSE)
    label(d, 1330, 325, "accepted: %s" % forged_ok, ROSE, 32, True, "lm")
    header(d, 100, 440, "AFTER REVOCATION: the client's list", ROSE)
    label(d, 900, 505, "the shop's chain", MUTED, 24, True, "ma")
    label(d, 1300, 505, "the forged chain", MUTED, 24, True, "ma")
    for i, (sn, whose, shop, forged, reason) in enumerate(rows):
        y = 545 + i * 100
        d.rounded_rectangle([100, y, 1500, y + 80], radius=14, fill=PANEL, outline=AMBER, width=3)
        label(d, 125, y + 40, "serial %s on the list (%s)" % (sn, whose), TEXT, 26, True, "lm")
        label(d, 900, y + 40, "accepted: %s" % shop, ROSE, 26, True, "mm")
        label(d, 1300, y + 40, ("accepted: %s" % forged) if forged else "-", ROSE if forged else MUTED, 26, True, "mm")
    caption(d, "Revoke the intermediate and everything it signed falls, but only for clients with the latest list.", AMBER, 790)
