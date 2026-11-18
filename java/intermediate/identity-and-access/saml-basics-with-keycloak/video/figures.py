"""Pictures for the SAML with Keycloak video, drawn by the `figure` slide kind.

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




def kv(ln):
    """'Issuer        <keycloak>/realms/bank  (the realm)' -> ('Issuer', '<keycloak>/realms/bank')."""
    k, v = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
    return k, v.split("  (")[0]


def section_result(name, head, n=0):
    return arrow_result(sec(name, head, n))


# ------------------------------------------------------------------ figures

def trust_anchor(img, d, cfg):
    """The core idea: one key from the bank's metadata, and three validly signed responses."""
    checks = sec("ONE", "HOW THE SIGNATURES ARE CHECKED")
    key = checks[0].split(": ", 1)[1]
    bank = arrow_result(sec("ONE", "THE RESULT"))
    attacker = sec("TWO", "THE RESULT", 1)[0].replace("-> ", "")
    wallet = sec("TWO", "THE RESULT", 2)[0].replace("-> ", "")
    header(d, 60, 200, "WHEN THE SHOP STARTS: THE BANK'S METADATA", CYAN)
    panel(d, 60, 260, 1800, 110, CYAN, 3)
    fit_mono(d, 90, 295, "<keycloak>/realms/bank/protocol/saml/descriptor  ->  " + key, TEXT, 30, 1740)
    header(d, 60, 410, "THREE RESPONSES, EACH WITH A VALID XML SIGNATURE", AMBER)
    data = [("the bank's own response", "signed with the metadata key", bank),
            ("the attacker's response", "signed with the attacker's key", attacker),
            ("the wallet's response", "signed with the wallet's key", wallet)]
    for i, (who, how, res) in enumerate(data):
        y = 480 + i * 120
        colour = GREEN if "signed in" in res else ROSE
        cell(d, 60, y, 560, 90, who, AMBER, 30)
        cell(d, 640, y, 640, 90, how, GREEN if "metadata" in how else ROSE, 28)
        draw.arrow_right(d, 1290, y + 45, 1350, MUTED, 4)
        cell(d, 1360, y, 500, 90, res, colour, 30)
    caption(d, "A signature is only worth the key behind it.", AMBER, 900)


def signin_flow(img, d, cfg):
    """The shop's request, the bank's signed response, and how its two signatures are checked."""
    req = sec("ONE", "THE REQUEST")
    resp = sec("ONE", "THE RESPONSE")
    checks = sec("ONE", "HOW THE SIGNATURES ARE CHECKED")
    res = sec("ONE", "THE RESULT")
    header(d, 60, 195, "THE REQUEST", AMBER)
    panel(d, 60, 250, 900, 250, AMBER, 3)
    rows(d, 80, 275, [kv(ln) for ln in req], 210, TEXT, 24, 56, 860)
    header(d, 990, 195, "THE RESPONSE (SIGNED BY THE BANK)", GREEN)
    panel(d, 990, 250, 870, 460, GREEN, 3)
    rows(d, 1010, 275, [kv(ln) for ln in resp], 210, TEXT, 24, 56, 830)
    header(d, 60, 530, "HOW THE SIGNATURES ARE CHECKED", CYAN)
    for i, ln in enumerate(checks):
        fit_mono(d, 60, 590 + i * 44, ln, TEXT if i < 3 else GREEN, 24, 900)
    header(d, 990, 740, "THE RESULT", GREEN)
    for i, ln in enumerate(res):
        fit_mono(d, 990, 800 + i * 44, ln, GREEN, 24, 870)
    caption(d, "Two signatures, both checked with the key from the bank's metadata.", AMBER, 925)


def whose_key(img, d, cfg):
    """An edit breaks the digests; a valid signature with another key fails RSA with the bank's key."""
    edited = sec("TWO", "ONE NAME EDITED")
    attacker = sec("TWO", "THE ATTACKER'S RESPONSE")
    wallet = sec("TWO", "A GENUINE RESPONSE FROM THE WALLET")
    results = [arrow_result(r) for r in sections("TWO", "THE RESULT")]
    blocks = [("ONE NAME EDITED", edited, results[0], AMBER),
              ("SELF-SIGNED BY THE ATTACKER", attacker, results[1], ROSE),
              ("SIGNED BY THE WALLET", wallet, results[2], ROSE)]
    for i, (title, lines, res, colour) in enumerate(blocks):
        y = 195 + i * 235
        header(d, 60, y, title, colour)
        for j, ln in enumerate(lines[:3]):
            fit_mono(d, 80, y + 58 + j * 44, ln, ROSE if ln.endswith("false") else TEXT, 26, 1180)
        cell(d, 1300, y + 50, 560, 90, res, GREEN, 28)
    caption(d, "The shop trusts the key in the bank's metadata, and no other.", AMBER, 920)


def wrapping_tree(img, d, cfg):
    """The wrapped response, and the other forgeries, each with its refusal."""
    tree = sec("THREE", "THE WRAPPED RESPONSE")
    res = arrow_result(sec("THREE", "THE RESULT (Spring"))
    changes = sec("THREE", "ONE THING CHANGED")
    header(d, 60, 195, "THE INPUT: THE WRAPPED RESPONSE", AMBER)
    panel(d, 60, 255, 1250, 230, AMBER, 3)
    for i, ln in enumerate(tree):
        depth = (len(ln) - len(ln.lstrip())) // 2
        text = ln.strip()
        colour = ROSE if "unsigned" in text else GREEN if "signed by" in text else TEXT
        fit_mono(d, 90 + depth * 50, 280 + i * 64, text, colour, 28, 1180 - depth * 50)
    header(d, 1350, 195, "THE RESULT", GREEN)
    cell(d, 1350, 300, 510, 100, res, GREEN, 26)
    header(d, 60, 520, "ONE THING CHANGED AT A TIME", AMBER)
    pairs = [(changes[i], changes[i + 1].strip()) for i in range(0, len(changes) - 1, 2)]
    for i, (inp, out) in enumerate(pairs):
        yy = 590 + i * 70
        fit_mono(d, 60, yy, inp, TEXT, 24, 1150)
        fit_mono(d, 1250, yy, out, GREEN, 24, 610)
    caption(d, "Only the assertion a trusted key signed is ever read.", AMBER, 910)


def statement_checks(img, d, cfg):
    """A genuine signature, and the statement still refused when it does not fit."""
    once = arrow_result(sec("FOUR", "A GENUINE RESPONSE"))
    changes = sec("FOUR", "ONE THING CHANGED")
    header(d, 60, 195, "A GENUINE RESPONSE, POSTED ONCE", GREEN)
    cell(d, 60, 255, 600, 90, once, GREEN, 30)
    header(d, 60, 390, "ONE THING CHANGED AT A TIME (ALL SIGNED BY THE BANK)", AMBER)
    pairs = [(changes[i], changes[i + 1].strip()) for i in range(0, len(changes) - 1, 2)]
    for i, (inp, out) in enumerate(pairs):
        yy = 460 + i * 100
        panel(d, 60, yy, 1800, 80, LINE, 2)
        fit_mono(d, 85, yy + 25, inp, TEXT, 26, 1000)
        fit_mono(d, 1120, yy + 25, out.replace("-> ", ""), GREEN, 26, 720)
    caption(d, "A signature says who wrote it; these checks say if it is for now.", AMBER, 900)


def key_rotation(img, d, cfg):
    """Three moments: the shop's key matches, the bank rotates, the shop reloads."""
    before = sec("FIVE", "BEFORE THE KEY CHANGE")
    rotate_head = next(ln for ln in act("FIVE") if ln.startswith("THE BANK ADDS"))
    rotate = sec("FIVE", "THE BANK ADDS")
    after = sec("FIVE", "AFTER THE SHOP RELOADS")
    status = re.search(r"HTTP (\d+)", rotate_head).group(1)
    stages = [("1. METADATA READ AT START", "bank signs with key 1", "shop trusts key 1", before[0], GREEN),
              ("2. BANK ADDS KEY 2 (HTTP " + status + ")", "bank signs with key 2", "shop still trusts key 1", rotate[0], ROSE),
              ("3. SHOP RELOADS METADATA", "bank signs with key 2", "shop trusts key 2", after[0], GREEN)]
    for i, (title, bank, shop, res, colour) in enumerate(stages):
        x = 60 + i * 610
        header(d, x, 200, title, colour, 580, 24)
        panel(d, x, 265, 580, 420, colour, 3)
        label(d, x + 30, 300, bank, TEXT, 32)
        label(d, x + 30, 370, shop, TEXT, 32)
        res_text = res.split("-> ", 1)[1]
        fit_mono(d, x + 30, 480, res_text, colour, 30, 520)
        if i == 1:
            fit_mono(d, x + 30, 560, rotate[1].strip().replace("<- ", ""), ROSE, 26, 520)
        if i < 2:
            draw.arrow_right(d, x + 582, 475, x + 608, MUTED, 3)
    caption(d, "Rotate keys only after every shop has read the new metadata.", AMBER, 900)
