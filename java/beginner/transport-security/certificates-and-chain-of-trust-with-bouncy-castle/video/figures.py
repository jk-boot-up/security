"""Pictures for the certificates and chain of trust with Bouncy Castle video, drawn by the `figure` slide kind.

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


# ------------------------------------------------------------------ figures

def rules_core(img, d, cfg):
    """The core idea on real values: the rules a CA carries travel inside its certificates, and every client checks them."""
    after = section("ONE", "AFTER BUILDING")
    rule = re.search(r"name rule: (\S+)", after).group(1)
    usage = section("ONE", "KEY USAGE")
    mid_bits = re.search(r"intermediate\s+(\d{9})", usage).group(1)
    srv_bits = re.search(r"server\s+(\d{9})", usage).group(1)
    two = section("TWO", "AFTER THE CHECK")
    other = re.search(r"other\.example\s+accepted (\w+), (\S+)", two).groups()
    three = section("THREE", "AFTER REVOCATION")
    revoked = re.search(r"the chain accepted: (\w+), (\S+)", three).groups()
    rows = [("CA flag + path length", "intermediate: CAs below: 0", "no deeper chain", CLIENT),
            ("key usage bits", "intermediate %s, server %s" % (mid_bits, srv_bits), "a server key cannot sign certificates", AMBER),
            ("name constraint", "only names under %s" % rule, "other.example: %s, %s" % other, GREEN),
            ("revocation list", "the issuer signs the list", "revoked: %s, %s" % revoked, ROSE)]
    label(d, 100, 215, "the rule, written by a builder", MUTED, 26, True)
    label(d, 560, 215, "the real value in the demo", MUTED, 26, True)
    label(d, 1300, 215, "what the client then refuses", MUTED, 26, True)
    for i, (k, v, r, c) in enumerate(rows):
        y = 262 + i * 140
        d.rounded_rectangle([100, y, 520, y + 104], radius=16, fill=PANEL, outline=c, width=4)
        d.text((310, y + 52), k, font=font(SANS_B, 28), fill=c, anchor="mm")
        field(d, 560, y + 22, 700, 60, v, c, 22)
        draw.arrow_right(d, 1270, y + 52, 1300, c, 4)
        d.rounded_rectangle([1310, y + 20, 1820, y + 84], radius=32, fill=c)
        d.text((1565, y + 52), r, font=font(SANS_B, 21), fill=DARK, anchor="mm")
    caption(d, "The library writes the rules into the certificates; the client enforces them.", AMBER, 860)


def built_chain(img, d, cfg):
    """What each builder was asked for, what the certificates say, and the key usage bits."""
    before = [re.match(r"\s*(\w+)\s+(.+)$", l).groups() for l in section("ONE", "BEFORE BUILDING").strip().split("\n")]
    after_text = section("ONE", "AFTER BUILDING")
    after = {m[0]: m[1] for m in re.findall(r"^    (\w+)\s+(.+)$", after_text, re.M)}
    rule = re.search(r"name rule: (\S+)", after_text).group(1)
    usage = section("ONE", "KEY USAGE")
    linked = value("each names its parent's key identifier", "ONE")
    header(d, 100, 205, "BEFORE BUILDING: asked for", GREEN)
    header(d, 1000, 205, "AFTER BUILDING: read back", ROSE)
    for i, (role, asked) in enumerate(before):
        y = 270 + i * 100
        label(d, 100, y + 30, role, TEXT, 26, True, "lm")
        label(d, 330, y + 30, asked, MUTED, 22, False, "lm")
        draw.arrow_right(d, 930, y + 30, 990, MUTED, 3)
        got = after.get(role, "") + ("   rule: %s" % rule if role == "intermediate" else "")
        label(d, 1000, y + 30, got, CLIENT, 22, True, "lm")
    label(d, 100, 600, "key usage, bits 0 to 8", AMBER, 28, True)
    names = ["data", "", "", "", "", "certs", "lists", "", ""]
    for i in range(9):
        label(d, 560 + i * 74 + 32, 600, str(i), MUTED, 22, True, "ma")
        label(d, 560 + i * 74 + 32, 760, names[i], MUTED, 20, True, "ma")
    for j, (who, bits) in enumerate(re.findall(r"(intermediate|server)\s+(\d{9})", usage)):
        y = 635 + j * 60
        label(d, 100, y + 26, who, TEXT, 26, True, "lm")
        cells(d, 560, y, list(bits), AMBER if who == "intermediate" else CLIENT, w=64, h=52, size=28, gap=10)
    label(d, 100, 800, "each certificate names its parent's key identifier: %s" % linked, GREEN, 26, True)
    caption(d, "Every rule is a named extension, written by the library, not by hand.", AMBER, 870)


def name_rule(img, d, cfg):
    """The name constraint, worked out by hand for four names, and the JDK's verdict."""
    rule = re.search(r"STEP\. THE RULE BY HAND \(the name, or ending in (\S+)\):", section("TWO")).group(1)
    steps = re.findall(r"^    (\S+)\s+(.+?): (true|false)$", section("TWO", "STEP."), re.M)
    jdk = {m[0]: (m[1], m[2]) for m in re.findall(r"^    (\S+)\s+accepted (\w+)(?:, (\S+))?$", section("TWO", "AFTER THE CHECK"), re.M)}
    header(d, 100, 205, "BEFORE THE CHECK: the intermediate's rule", GREEN)
    field(d, 100, 265, 700, 56, "the name, or ending in %s" % rule, GREEN, 24)
    label(d, 700, 345, "STEP: the rule by hand", AMBER, 24, True, "ma")
    label(d, 1450, 345, "AFTER: the JDK's PKIX", ROSE, 24, True, "ma")
    for i, (host, why, ok) in enumerate(steps):
        y = 385 + i * 105
        good = ok == "true"
        d.rounded_rectangle([100, y, 1820, y + 88], radius=14, fill=PANEL, outline=GREEN if good else ROSE, width=3)
        label(d, 125, y + 44, host, TEXT, 30, True, "lm")
        label(d, 520, y + 44, why, MUTED, 26, False, "lm")
        mark(d, 1000, y + 44, good)
        if host in jdk:
            acc, reason = jdk[host]
            label(d, 1450, y + 44, "accepted %s%s" % (acc, ", " + reason if reason else ""), GREEN if acc == "true" else ROSE,
                  26, True, "mm")
        else:
            label(d, 1450, y + 44, "(by hand only: the dot matters)", MUTED, 22, False, "mm")
    caption(d, "A stolen intermediate key can sign only names under %s." % rule[1:], AMBER, 830)


def crl_check(img, d, cfg):
    """A signed list: empty, then naming the shop's serial; and what happens without a good list."""
    before = section("THREE", "BEFORE REVOCATION")
    after = section("THREE", "AFTER REVOCATION")
    without = section("THREE", "WITHOUT A GOOD LIST")
    n0, ok0 = re.search(r"entries (\d+), the chain accepted: (\w+)", before).groups()
    n1, ent = re.search(r"entries (\d+): (.+)$", after, re.M).groups()
    by_mid, by_root = re.search(r"signed by the intermediate: (\w+), by the root: (\w+)", after).groups()
    acc, reason = re.search(r"the chain accepted: (\w+), (\S+)", after).groups()
    header(d, 100, 205, "BEFORE REVOCATION", GREEN)
    card(d, 100, 270, 820, 160, "the intermediate's list", ["entries: %s" % n0, "the chain accepted: %s" % ok0], GREEN)
    header(d, 1000, 205, "AFTER REVOCATION: the shop's key is lost", ROSE)
    card(d, 1000, 270, 820, 160, "the intermediate's list", ["entries: %s, %s" % (n1, ent), "the chain accepted: %s, %s" % (acc, reason)], ROSE)
    label(d, 1000, 455, "the list's signature checks with", MUTED, 24, True)
    label(d, 1000, 490, "the intermediate's key: %s    the root's key: %s" % (by_mid, by_root), AMBER, 24, True)
    header(d, 100, 560, "WITHOUT A GOOD LIST", AMBER)
    for i, (who, ok, why) in enumerate(re.findall(r"^    (.+?)\s{2,}(\w+), (\S+)$", without, re.M)):
        y = 625 + i * 80
        d.rounded_rectangle([100, y, 1820, y + 64], radius=12, fill=PANEL, outline=AMBER, width=3)
        label(d, 125, y + 32, who, TEXT, 26, True, "lm")
        label(d, 700, y + 32, "accepted %s, %s" % (ok, why), ROSE, 26, True, "lm")
    caption(d, "This client refuses when it cannot find out. Many browsers go ahead.", AMBER, 830)


def csr_flow(img, d, cfg):
    """The shop signs a request with its own key; the authority checks it and its policy, then signs."""
    req = section("FOUR", "THE REQUEST")
    res = section("FOUR", "THE RESPONSE")
    names = re.search(r"names (\[.+?\])", req).group(1)
    proves = value("the signature proves the sender holds the key", "FOUR", "THE REQUEST")
    changed = value("one letter changed on the way, it still proves it", "FOUR", "THE REQUEST")
    signed = re.search(r"shop\.example: (.+)$", res, re.M).group(1)
    bank = re.search(r"bank\.example: (.+)$", res, re.M).group(1)
    policy = re.search(r"policy: (.+?)\)", section("FOUR")).group(1)
    header(d, 100, 205, "THE REQUEST: signed by the shop", GREEN)
    card(d, 100, 270, 760, 230, "the shop", ["makes its own key pair", "keeps the private key", "sends names %s" % names,
                                            "and its public key, signed"], GREEN)
    label(d, 100, 530, "signature proves the key: %s" % proves, GREEN, 26, True)
    label(d, 100, 570, "one letter changed: proves it: %s" % changed, ROSE, 26, True)
    draw.arrow_right(d, 880, 385, 1010, AMBER, 5)
    label(d, 945, 340, "request", AMBER, 22, True, "ma")
    header(d, 1020, 205, "THE RESPONSE: the authority", ROSE)
    card(d, 1020, 270, 800, 120, "policy", [policy], AMBER)
    for i, (k, v, c) in enumerate((("shop.example", signed, GREEN), ("bank.example", bank, ROSE))):
        y = 420 + i * 90
        d.rounded_rectangle([1020, y, 1820, y + 74], radius=12, fill=PANEL, outline=c, width=3)
        label(d, 1040, y + 24, k, TEXT, 24, True, "lm")
        label(d, 1040, y + 54, v, c, 20, True, "lm")
    caption(d, "The authority never sees the private key. It certifies a public key, after checking the names.", AMBER, 700)


def stolen_key(img, d, cfg):
    """A stolen intermediate key forges a chain; the root's list revokes the intermediate."""
    before = section("FIVE", "BEFORE REVOCATION")
    after = section("FIVE", "AFTER REVOCATION")
    serial = re.search(r"serial (\d+)", before).group(1)
    forged = re.search(r"the forged chain accepted: (\w+)", before).group(1)
    listed = re.search(r"AFTER REVOCATION \(the root's list: (.+?)\):", section("FIVE")).group(1)
    f_after = re.search(r"the forged chain accepted: (\w+)", after).group(1)
    real = re.search(r"the shop's real chain accepted: (\w+)", after).group(1)
    header(d, 100, 205, "BEFORE REVOCATION: a thief has the intermediate's key", GREEN)
    card(d, 100, 270, 520, 110, "Example Issuing CA", ["private key: stolen"], ROSE)
    draw.arrow_right(d, 630, 325, 760, ROSE, 4)
    card(d, 770, 270, 520, 110, "shop.example, serial %s" % serial, ["signed by the thief"], ROSE)
    label(d, 1330, 325, "accepted: %s" % forged, ROSE, 32, True, "lm")
    header(d, 100, 440, "AFTER REVOCATION: the root's list names %s" % listed, ROSE)
    for i, (k, v) in enumerate((("the forged chain", f_after), ("the shop's real chain", real))):
        y = 510 + i * 90
        d.rounded_rectangle([100, y, 1100, y + 70], radius=12, fill=PANEL, outline=AMBER, width=3)
        label(d, 125, y + 35, k, TEXT, 26, True, "lm")
        label(d, 1080, y + 35, "accepted: %s" % v, ROSE, 26, True, "rm")
    label(d, 1150, 545, "the shop needs a new", AMBER, 28, True)
    label(d, 1150, 585, "certificate from a new", AMBER, 28, True)
    label(d, 1150, 625, "intermediate", AMBER, 28, True)
    caption(d, "Everything below a stolen CA key falls, and so does everything below its revocation.", AMBER, 780)
