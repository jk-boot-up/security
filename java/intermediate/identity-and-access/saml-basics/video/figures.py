"""Pictures for the SAML basics video, drawn by the `figure` slide kind.

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
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def sections(name, head):
    """Every section of an act whose header starts with `head`, as lists of lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found


def sec(name, head, n=0):
    return sections(name, head)[n]


def field(lines, key):
    """The value after a left-hand key in lines such as 'Issuer        https://idp.bank.example  (who says so)'."""
    for ln in lines:
        if ln.startswith(key + " "):
            return ln[len(key):].strip().split("  (")[0]
    raise KeyError(key)


def arrow_result(lines):
    """The text after '-> ' in the first line that has one."""
    return next(ln.split("-> ", 1)[1] for ln in lines if "-> " in ln)


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def rows(d, x, y, pairs, key_w, colour=TEXT, size=26, step=46, max_w=900):
    for i, (k, v) in enumerate(pairs):
        fit_mono(d, x, y + i * step, k, MUTED, size, key_w - 10)
        fit_mono(d, x + key_w, y + i * step, v, colour, size, max_w - key_w)


def result_colour(text):
    return GREEN if "refused" in text else ROSE


def ok_colour(text):
    return ROSE if "admin" in text else GREEN


# ------------------------------------------------------------------ figures

def two_questions(img, d, cfg):
    """The core idea: a signature answers two questions, and each weak shop skips one of them."""
    trusting = arrow_result(sec("THREE", "BEFORE THE FIX"))
    pinned = arrow_result(sec("FOUR", "BEFORE THE FIX"))
    strict_wrap = arrow_result(sec("FOUR", "AFTER THE FIX"))
    strict_self = arrow_result(sec("FOUR", "THE STRICT PROVIDER AND"))
    covers = sec("TWO", "THE RESULT")[0]
    header(d, 60, 200, "A SIGNATURE ANSWERS TWO QUESTIONS", AMBER)
    panel(d, 60, 260, 1800, 150, AMBER, 3)
    label(d, 100, 285, "1. Whose key made it?  The bank's own key, known in advance.", TEXT, 30, True)
    label(d, 100, 345, "2. Which element does it cover?  " + covers.replace("the signature covers ", ""), TEXT, 30, True)
    cols = [("SHOP", 60, 300), ("KEY IT TRUSTS", 360, 420), ("ELEMENT IT READS", 780, 420), ("ATTACK", 1200, 260), ("RESULT", 1460, 420)]
    for text, x, w in cols:
        header(d, x, 440, text, CYAN, w - 20, 24)
    data = [
        ("trusting", "the one in KeyInfo", "the first assertion", "self-signed", trusting),
        ("pinned-key", "the bank's key", "the first assertion", "wrapped", pinned),
        ("strict", "the bank's key", "the one signed (#a1)", "wrapped", strict_wrap),
    ]
    for i, (shop, key, element, attack, res) in enumerate(data):
        y = 510 + i * 110
        colour = ok_colour(res)
        short = res.split("@")[0] if "@" in res else res.split(":")[0]
        cell(d, 60, y, 280, 84, shop, colour, 28)
        cell(d, 360, y, 400, 84, key, ROSE if "KeyInfo" in key else GREEN, 28)
        cell(d, 780, y, 400, 84, element, ROSE if "first" in element else GREEN, 28)
        cell(d, 1200, y, 240, 84, attack, AMBER, 28)
        cell(d, 1460, y, 400, 84, short, colour, 28)
    fit_mono(d, 60, 845, "strict, wrapped -> " + strict_wrap + ";  self-signed -> " + strict_self, GREEN, 24, 1800)
    caption(d, "Trust only the bank's key, and read only what that key signed.", AMBER, 905)


def signin_flow(img, d, cfg):
    """The request the shop makes, and the fields of the signed assertion that comes back."""
    req = sec("ONE", "THE REQUEST")
    resp = sec("ONE", "THE RESPONSE")
    res = sec("ONE", "THE RESULT")
    header(d, 60, 200, "THE REQUEST", AMBER)
    panel(d, 60, 260, 760, 230, AMBER, 3)
    rows(d, 85, 290, [tuple(re.split(r"\s{2,}", ln, maxsplit=1)) for ln in req], 220, TEXT, 26, 70, 720)
    label(d, 85, 420, "through Ana's browser, to the bank", MUTED, 24)
    draw.arrow_right(d, 830, 375, 930, MUTED, 4)
    header(d, 940, 200, "THE RESPONSE: THE SIGNED ASSERTION", GREEN)
    panel(d, 940, 260, 920, 620, GREEN, 3)
    keys = ["Assertion ID", "Issuer", "NameID", "InResponseTo", "Recipient", "Audience", "NotBefore", "NotOnOrAfter", "Signature"]
    rows(d, 965, 290, [(k, field(resp, k)) for k in keys], 230, TEXT, 24, 64, 880)
    header(d, 60, 560, "THE RESULT", GREEN)
    panel(d, 60, 620, 760, 180, GREEN, 3)
    for i, ln in enumerate(res):
        fit_mono(d, 85, 650 + i * 60, re.sub(r"\s+->", " ->", ln), GREEN, 24, 710)
    caption(d, "The assertion is the statement; the signature sits inside it.", AMBER, 910)


def digest_steps(img, d, cfg):
    """How the bank's signature is made: canonical bytes, a digest, SignedInfo, then RSA."""
    before = sec("TWO", "BEFORE SIGNING")
    steps = sec("TWO", "HOW THE BANK SIGNS IT")
    checks = sec("TWO", "HOW THE SHOP CHECKS IT")
    nbytes = re.search(r"-> (\d+) bytes", steps[1]).group(1)
    digest = steps[3].strip()
    sinfo = re.search(r"-> (\d+) bytes", steps[4]).group(1)
    sig = steps[6].strip().split(" = ")[0]
    header(d, 60, 195, "BEFORE SIGNING: THE ASSERTION", AMBER)
    fit_mono(d, 60, 255, before[0], TEXT, 24, 1800)
    fit_mono(d, 60, 292, before[1], TEXT, 24, 1800)
    header(d, 60, 340, "HOW THE BANK SIGNS IT", CYAN)
    y = 405
    boxes = [("1. drop", "the Signature", AMBER), ("2. canonicalize", nbytes + " bytes", AMBER),
             ("3. SHA-256", "DigestValue", CYAN), ("4. SignedInfo", sinfo + " bytes", CYAN),
             ("5. RSA-SHA256", sig, GREEN)]
    bw, gap = 330, 42
    for i, (top, sub, colour) in enumerate(boxes):
        x = 60 + i * (bw + gap)
        d.rounded_rectangle([x, y, x + bw, y + 130], radius=14, fill=BG_CELL, outline=colour, width=3)
        label(d, x + bw // 2, y + 22, top, colour, 28, True, "ma")
        fit_mono(d, x + bw // 2, y + 90, sub, TEXT, 26, bw - 20, "mm")
        if i < len(boxes) - 1:
            draw.arrow_right(d, x + bw + 4, y + 65, x + bw + gap - 4, MUTED, 3)
    label(d, 60, 560, "DigestValue (the same on every run: fixed ids and clock)", MUTED, 24)
    fit_mono(d, 60, 600, digest, CYAN, 30, 1800)
    label(d, 60, 655, "SignatureValue: a new RSA key on every run, so it is a placeholder", MUTED, 24)
    header(d, 60, 710, "HOW THE SHOP CHECKS IT (BY HAND)", GREEN)
    for i, ln in enumerate(checks):
        fit_mono(d, 60, 770 + i * 44, ln, GREEN if ln.endswith("true") else ROSE, 24, 1800)
    caption(d, "The signature signs SignedInfo; SignedInfo holds the digest of #a1.", AMBER, 920)


def whose_key(img, d, cfg):
    """A self-signed response: valid with the key it carries, invalid with the bank's."""
    attacker = sec("THREE", "THE ATTACKER'S RESPONSE")
    before = sec("THREE", "BEFORE THE FIX")
    after = sec("THREE", "AFTER THE FIX")
    digest = sec("THREE", "THE DIGEST")
    edited = arrow_result(sec("THREE", "THE RESULT"))
    header(d, 60, 195, "THE INPUT: THE ATTACKER'S RESPONSE", AMBER)
    fit_mono(d, 60, 255, attacker[0], TEXT, 26, 1800)
    fit_mono(d, 60, 295, attacker[1], TEXT, 26, 1800)
    header(d, 60, 360, "BEFORE THE FIX: KEY FROM KeyInfo", ROSE)
    panel(d, 60, 420, 880, 230, ROSE, 3)
    fit_mono(d, 85, 450, before[0], TEXT, 24, 830)
    fit_mono(d, 85, 510, before[1], ROSE, 28, 830)
    label(d, 85, 580, "anyone can sign; the key proves nothing", ROSE, 24, True)
    header(d, 980, 360, "AFTER THE FIX: THE BANK'S PINNED KEY", GREEN)
    panel(d, 980, 420, 880, 230, GREEN, 3)
    fit_mono(d, 1005, 450, after[0], TEXT, 24, 830)
    fit_mono(d, 1005, 510, after[1], GREEN, 28, 830)
    label(d, 1005, 580, "the shop knew the bank's key in advance", GREEN, 24, True)
    header(d, 60, 690, "ONE NAME EDITED IN A GENUINE RESPONSE", AMBER)
    fit_mono(d, 60, 750, digest[0], TEXT, 26, 1800)
    fit_mono(d, 60, 795, digest[1], ROSE, 26, 1800)
    fit_mono(d, 60, 845, "-> " + edited, GREEN, 26, 1800)
    caption(d, "The digests differ, so any edit breaks the bank's signature.", AMBER, 915)


def wrapping_tree(img, d, cfg):
    """The wrapped document: the signature covers one assertion, the weak shop reads another."""
    tree = sec("FOUR", "THE WRAPPED RESPONSE")
    before = sec("FOUR", "BEFORE THE FIX")
    after = sec("FOUR", "AFTER THE FIX")
    header(d, 60, 195, "THE INPUT: THE WRAPPED RESPONSE", AMBER)
    panel(d, 60, 255, 1150, 360, AMBER, 3)
    for i, ln in enumerate(tree):
        depth = (len(ln) - len(ln.lstrip())) // 2
        text = ln.strip()
        colour = ROSE if "unsigned" in text else GREEN if "a1" in text else TEXT
        fit_mono(d, 90 + depth * 50, 280 + i * 64, text, colour, 25, 1100 - depth * 50)
    label(d, 1240, 300, "read by the pinned-key shop", ROSE, 26, True)
    draw.arrow_left(d, 1235, 362, 1140, ROSE, 4)
    label(d, 1240, 520, "covered by the signature", GREEN, 26, True)
    draw.arrow_left(d, 1235, 560, 1140, GREEN, 4)
    header(d, 60, 645, "BEFORE THE FIX", ROSE)
    for i, ln in enumerate(before[:3]):
        fit_mono(d, 60, 705 + i * 46, ln, ROSE if i == 2 else TEXT, 24, 920)
    header(d, 1000, 645, "AFTER THE FIX: STRICT", GREEN)
    for i, ln in enumerate(after):
        fit_mono(d, 1000, 705 + i * 46, ln, GREEN if "->" in ln else TEXT, 24, 860)
    caption(d, "Checked one element, read another: read only what is signed.", AMBER, 905)


def time_window(img, d, cfg):
    """The validity window on a time line, and the other changes the strict shop refuses."""
    resp = sec("ONE", "THE RESPONSE")
    changes = sec("FIVE", "ONE THING CHANGED")
    not_before = field(resp, "NotBefore")[11:19]
    not_after = field(resp, "NotOnOrAfter")[11:19]
    now = re.search(r"now \S+T(\d\d:\d\d:\d\d)Z", "\n".join(act("FIVE"))).group(1)
    expired = re.search(r"T(\d\d:\d\d:\d\d)Z", changes[2]).group(1)
    header(d, 60, 195, "THE TIME WINDOW (60 SECONDS OF SKEW EACH SIDE)", CYAN)
    y = 380
    d.line([100, y, 1820, y], fill=LINE, width=4)
    d.line([430, y - 20, 450, y + 20], fill=MUTED, width=3)
    d.line([450, y - 20, 470, y + 20], fill=MUTED, width=3)
    label(d, 450, y + 30, "50 minutes", MUTED, 22, False, "ma")
    d.rounded_rectangle([760, y - 28, 1520, y + 28], radius=10, fill=(20, 70, 60), outline=GREEN, width=3)
    for x, text, colour, up in ((250, "expired NotOnOrAfter " + expired, ROSE, True),
                                (760, "NotBefore " + not_before, GREEN, False),
                                (980, "now " + now, AMBER, True),
                                (1520, "NotOnOrAfter " + not_after, GREEN, False)):
        d.line([x, y - 48, x, y + 48], fill=colour, width=4)
        fit_mono(d, x, y - 100 if up else y + 64, text, colour, 26, 600, "ma")
    label(d, 1820, y + 110, "(not to scale)", MUTED, 22, False, "ra")
    header(d, 60, 520, "ONE THING CHANGED AT A TIME", AMBER)
    pairs = [(changes[i], changes[i + 1].strip()) for i in range(0, len(changes) - 1, 2)]
    for i, (inp, res) in enumerate(pairs):
        yy = 580 + i * 62
        fit_mono(d, 60, yy, inp, TEXT, 24, 980)
        fit_mono(d, 1060, yy, res, GREEN, 24, 800)
    caption(d, "A genuine signature is not enough: the statement must fit this sign-in.", AMBER, 920)
