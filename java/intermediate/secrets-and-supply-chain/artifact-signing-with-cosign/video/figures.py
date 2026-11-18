"""Pictures for the cosign signing video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def section(name, head, n=0):
    """The lines of the n-th section of an act whose header starts with `head`, two spaces removed."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[n]


def facts(lines):
    """`key   value` lines as a dict."""
    return dict(tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip()))


def verdicts(lines):
    """`what   accepted|refused   reason` lines as (what, word, reason)."""
    rows = []
    for ln in lines:
        m = re.match(r"\s*(.*?)\s{2,}(accepted|refused)\s+(.*)", ln)
        if m:
            rows.append(m.groups())
    return rows


def hex16(text):
    return re.search(r"[0-9a-f]{16}", text).group(0)


# ------------------------------------------------------------------ drawing helpers

def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def dashed(d, points, colour, width=4, dash=18):
    """A dashed polyline."""
    for (x1, y1), (x2, y2) in zip(points, points[1:]):
        length = max(abs(x2 - x1), abs(y2 - y1))
        for s in range(0, int(length), dash * 2):
            a, b = s / length, min(s + dash, length) / length
            d.line([(x1 + (x2 - x1) * a, y1 + (y2 - y1) * a), (x1 + (x2 - x1) * b, y1 + (y2 - y1) * b)],
                   fill=colour, width=width)


def word_colour(word):
    return GREEN if word == "accepted" else ROSE


# ------------------------------------------------------------------ figures

def seal(img, d, cfg):
    """The core idea: cosign signs the digest, the signature sits next to the image, the gate verifies the digest the tag resolves to."""
    before = facts(section("ONE", "BEFORE"))
    payload = facts(section("TWO", "BEFORE SIGNING"))
    stored = facts(section("TWO", "AFTER SIGNING"))
    rows = verdicts(section("TWO", "THE GATE"))
    header(d, 60, 210, "THE RELEASE PIPELINE SIGNS", CYAN)
    stops = [("the tag", before["tag 2.4.0"].split(",")[0].replace("-> ", "2.4.0 -> "), TEXT),
             ("the payload names", payload["digest"].split("= ")[1], CYAN),
             ("signed with", "the release key", AMBER),
             ("stored as", stored["stored as"].split(": ")[1], AMBER)]
    for i, (top, main, colour) in enumerate(stops):
        x = 60 + i * 460
        label(d, x + 190, 280, top, MUTED, 24, True, "ma")
        panel(d, x, 315, 380, 100, colour, 3)
        fit_mono(d, x + 190, 365, main, colour, 28, 350, "mm")
        if i < 3:
            draw.arrow_right(d, x + 390, 365, x + 450, MUTED, 4)
    header(d, 60, 470, "THE GATE: PINNED PUBLIC KEY, TAG RESOLVED TO A DIGEST", GREEN)
    for i, (what, word, reason) in enumerate(rows):
        y = 545 + i * 88
        colour = word_colour(word)
        panel(d, 60, y, 1800, 72, colour, 3)
        mono(d, 90, y + 36, what, TEXT, 28, "lm")
        cell(d, 760, y + 10, 200, 52, word, colour, 26)
        label(d, 990, y + 36, reason, colour, 28, True, "lm")
    caption(d, "The signature covers the digest, never the tag.", AMBER, 935)


def tag_moves(img, d, cfg):
    """A tag points at digest A, then at digest B; a pull by digest still gets the release."""
    before = facts(section("ONE", "BEFORE"))
    after = facts(section("ONE", "AFTER"))
    a = re.search(r"digest \w", before["tag 2.4.0"]).group(0)
    b = re.search(r"digest \w", after["tag 2.4.0"]).group(0)
    by_digest = next(k for k in after if k.startswith("pull by"))
    for i, (title, facts_, target, colour) in enumerate((("BEFORE", before, a, GREEN), ("AFTER", after, b, ROSE))):
        x = 60 + i * 930
        header(d, x, 210, title + (": THE RELEASE IS PUSHED" if i == 0 else ": THE TAG IS REUSED"), colour)
        cell(d, x + 40, 300, 300, 80, "tag 2.4.0", AMBER, 32)
        draw.arrow_right(d, x + 350, 340, x + 470, colour, 5)
        cell(d, x + 480, 300, 340, 80, target, colour, 32)
        label(d, x + 650, 400, facts_["tag 2.4.0"].split(", ")[1], MUTED, 24, True, "ma")
        pull = facts_["pull 2.4.0"].split("  <-")[0]
        label(d, x + 40, 470, "pull 2.4.0 " + pull, GREEN if pull.endswith("true") else ROSE, 30, True)
    label(d, 990, 530, "the same? " + after["the same?"], ROSE, 28, True)
    header(d, 60, 620, "A DIGEST IS A CHECKSUM", CYAN)
    label(d, 60, 690, "a digest = " + before["a digest"], TEXT, 30, True)
    label(d, 60, 750, by_digest + ": " + after[by_digest], GREEN, 30, True)
    label(d, 60, 810, "but " + section("ONE", "WHY A DIGEST")[0].strip(), AMBER, 30, True)
    caption(d, "A tag is a name that anyone with push rights can move.", ROSE, 935)


def gate_flow(img, d, cfg):
    """The signed payload's fields, and the gate's four answers."""
    payload = facts(section("TWO", "BEFORE SIGNING"))
    stored = facts(section("TWO", "AFTER SIGNING"))
    rows = verdicts(section("TWO", "THE GATE"))
    header(d, 60, 210, "BEFORE SIGNING: THE PAYLOAD COSIGN SIGNS", CYAN)
    for i, k in enumerate(("type", "digest", "for", "claims")):
        y = 280 + i * 60
        label(d, 60, y, k, MUTED, 26, True)
        fit_mono(d, 220, y, payload[k], CYAN if k == "digest" else TEXT, 26, 800)
    header(d, 1080, 210, "AFTER SIGNING", AMBER)
    for i, k in enumerate(("signed", "stored as", "the tag")):
        y = 280 + i * 60
        label(d, 1080, y, k, MUTED, 26, True)
        fit_mono(d, 1260, y, stored[k], AMBER if k == "the tag" else TEXT, 24, 600)
    header(d, 60, 540, "THE GATE'S ANSWERS", GREEN)
    for i, (what, word, reason) in enumerate(rows):
        x, y = 60 + (i % 2) * 920, 610 + (i // 2) * 150
        colour = word_colour(word)
        panel(d, x, y, 880, 128, colour, 3)
        mono(d, x + 30, y + 40, what, TEXT, 30, "lm")
        cell(d, x + 30, y + 70, 180, 46, word, colour, 24)
        label(d, x + 230, y + 93, reason, colour, 26, True, "lm")
    caption(d, "Only the digest the release key signed passes.", AMBER, 935)


def claims_keys(img, d, cfg):
    """Claims against what each gate requires, the keys each gate trusts, and the thief's undated signature."""
    claims = facts(section("THREE", "BEFORE ("))
    gates = verdicts(section("THREE", "AFTER (gates"))
    rotation = verdicts(section("THREE", "THE ROTATION"))
    thief = facts(section("THREE", "BEFORE: A THIEF"))
    late = verdicts(section("THREE", "AFTER: THE GATES"))
    header(d, 60, 210, "SIGNED CLAIMS", CYAN)
    fit_mono(d, 60, 280, "digest A: " + claims["claims"], CYAN, 26, 860)
    fit_mono(d, 60, 325, claims["a beta"], MUTED, 26, 860)
    for i, (what, word, reason) in enumerate(gates):
        y = 375 + i * 95
        colour = word_colour(word)
        mono(d, 60, y + 28, what, TEXT, 26, "lm")
        cell(d, 480, y, 180, 56, word, colour, 24)
        fit_mono(d, 60, y + 72, reason, colour, 22, 860, "lm")
    d.line([960, 215, 960, 640], fill=LINE, width=2)
    header(d, 1000, 210, "THE KEYS A GATE TRUSTS", AMBER)
    for i, (what, word, reason) in enumerate(rotation):
        y = 280 + i * 110
        colour = word_colour(word)
        mono(d, 1000, y + 28, what, TEXT, 26, "lm")
        cell(d, 1420, y, 180, 56, word, colour, 24)
        fit_mono(d, 1000, y + 78, reason, colour, 22, 820, "lm")
    header(d, 60, 670, "A THIEF WITH THE RETIRED old KEY", ROSE)
    label(d, 60, 740, thief["image"] + "; its time: " + thief["time"].replace(": ", ", ", 1), TEXT, 28, True)
    for i, (what, word, reason) in enumerate(late):
        x = 60 + i * 920
        colour = ROSE if word == "accepted" else GREEN
        cell(d, x, 795, 200, 56, word, colour, 26)
        label(d, x + 220, 823, "%s: %s" % (what, reason), colour, 26, True, "lm")
    caption(d, "A signature carries no date: a retired key signs today as well as yesterday.", ROSE, 935)


def provenance_rule(img, d, cfg):
    """Provenance through two checks: the signature, then the gate's own rule on the builder."""
    before = facts(section("FOUR", "BEFORE"))
    table = section("FOUR", "AFTER")
    rows = [re.split(r"\s{2,}|\s(?=accepted|refused|https)", ln.strip()) for ln in table[1:] if ln.strip().startswith("digest")]
    header(d, 60, 210, "BEFORE: THE PROVENANCE THE CI SYSTEM SIGNS", CYAN)
    for i, k in enumerate(("image", "builder", "source")):
        label(d, 60, 280 + i * 50, k, MUTED, 26, True)
        fit_mono(d, 220, 280 + i * 50, before[k], TEXT, 26, 1600)
    header(d, 60, 460, "AFTER: TWO CHECKS", GREEN)
    label(d, 560, 530, "1. the signature", MUTED, 26, True, "ma")
    label(d, 1060, 530, "builder", MUTED, 26, True, "ma")
    label(d, 1600, 530, "2. the rule: builder = CI", MUTED, 26, True, "ma")
    for i, row in enumerate(rows):
        digest, sig, builder, rule = [c.strip() for c in row if c.strip()]
        y = 590 + i * 120
        mono(d, 60, y + 35, digest, TEXT, 32, "lm")
        cell(d, 440, y, 240, 70, sig, word_colour(sig), 28)
        draw.arrow_right(d, 690, y + 35, 760, MUTED, 4)
        cell(d, 770, y, 580, 70, builder, GREEN if rule == "accepted" else ROSE, 26)
        draw.arrow_right(d, 1360, y + 35, 1470, MUTED, 4)
        cell(d, 1480, y, 240, 70, rule, word_colour(rule), 28)
    caption(d, "A signature says who signed; the gate's rule checks what they signed.", AMBER, 935)


def gate_paths(img, d, cfg):
    """The path an image takes, the gate on it, and the direct pull around it."""
    gate = verdicts(section("FIVE", "THE GATE"))[0]
    guard = facts(section("FIVE", "THE GATE"))
    key = facts(section("FIVE", "THE SIGNING KEY"))
    keyless = " ".join(ln.strip() for ln in section("FIVE", "KEYLESS"))
    stops = [("build", "the image", TEXT), ("sign", "key + passphrase", AMBER), ("registry", "image and signature", CYAN),
             ("gate", "cosign verify", GREEN), ("run", "the store", TEXT)]
    header(d, 60, 205, "THE PATH AN IMAGE TAKES", CYAN)
    for i, (name, sub, colour) in enumerate(stops):
        x = 60 + i * 370
        draw.box(d, x, 290, 300, 130, name, sub, colour, 34, 24)
        if i < 4:
            draw.arrow_right(d, x + 305, 355, x + 365, MUTED, 4)
    dashed(d, [(950, 420), (950, 520), (1690, 520), (1690, 425)], ROSE, 4)
    d.polygon([(1680, 440), (1700, 440), (1690, 422)], fill=ROSE)
    label(d, 1320, 535, "a direct pull " + guard["a direct pull"], ROSE, 26, True, "ma")
    header(d, 60, 610, "THE GATE ON ITS PATH", GREEN)
    cell(d, 60, 670, 200, 60, gate[1], word_colour(gate[1]), 26)
    label(d, 280, 700, "%s: %s" % (gate[0], gate[2]), TEXT, 26, True, "lm")
    header(d, 980, 610, "THE SIGNING KEY", AMBER)
    label(d, 980, 680, "wrong passphrase: " + key["wrong passphrase"], AMBER, 28, True)
    header(d, 60, 780, "KEYLESS SIGNING", CYAN)
    fit_mono(d, 60, 850, keyless, TEXT, 24, 1800)
    caption(d, "The gate protects only the paths that pass through it; lock the others.", AMBER, 935)
