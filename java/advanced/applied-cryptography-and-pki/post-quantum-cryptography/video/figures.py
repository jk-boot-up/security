"""Pictures for the post-quantum cryptography video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, ROSE, SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The lines of the first section of an act whose header starts with `head` (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = [] if ln.startswith(head) and not found else None
            if cur is not None:
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[0]


def split(ln, n=1):
    """A line split at runs of two or more spaces."""
    return re.split(r"\s{2,}", ln.strip(), maxsplit=n)


def facts(name, head):
    """`key  value` lines of a section as a dict (continuation lines are skipped)."""
    return dict(split(ln) for ln in section(name, head) if not ln.startswith(" ") and len(split(ln)) == 2)


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


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def table(name, head):
    """A table section: rows of whitespace-separated columns, the first row (column names) skipped."""
    rows = []
    for ln in section(name, head)[1:]:
        m = re.match(r"(.+?)\s{2,}(\d+)\s+(\d+)(?:\s+(\d+))?$", ln.strip())
        rows.append((m.group(1), int(m.group(2)), int(m.group(3))))
    return rows


def yes_no_colour(text, good):
    return GREEN if text == good else ROSE


# ------------------------------------------------------------------ figures

def mosca_timeline(img, d, cfg):
    """Mosca's inequality on one data class: the statement archive, against a threat 8 years away."""
    data = {split(ln)[0]: split(ln)[1] for ln in section("FIVE", "THE BANK'S DATA")}
    name = "customer statement archive"
    x, y, total = [int(n) for n in re.findall(r"\d+", data[name])]
    grid = section("FIVE", "THE RESULT FOR EACH")
    zs = [int(z) for z in re.findall(r"z=(\d+)", grid[0])]
    row = next(r for r in grid if r.startswith("statement archive"))
    verdicts = re.split(r"\s{2,}", row.strip())[1:]
    z, verdict = zs[0], verdicts[0]
    x0, x1, years = 160, 1800, 16
    k = (x1 - x0) / years

    def px(yr):
        return x0 + yr * k
    header(d, 60, 175, "ONE KIND OF DATA: THE " + name.upper(), CYAN)
    for yr in range(0, years + 1, 2):
        d.line([px(yr), 300, px(yr), 760], fill=(40, 52, 75), width=2)
        label(d, px(yr), 770, str(yr), MUTED, 24, False, "ma")
    label(d, x0 + (x1 - x0) // 2, 810, "years from today", MUTED, 24, False, "ma")
    label(d, 60, 315, "x", TEXT, 34, True)
    d.rectangle([px(0), 300, px(x), 360], fill=AMBER)
    label(d, px(x / 2), 330, "must stay secret: %d years" % x, INK, 26, True, "mm")
    label(d, 60, 415, "y", TEXT, 34, True)
    d.rectangle([px(0), 400, px(y), 460], fill=CYAN)
    label(d, px(y) + 20, 430, "migration: %d years" % y, CYAN, 26, True, "lm")
    label(d, 60, 515, "x+y", TEXT, 30, True)
    d.rectangle([px(0), 500, px(total), 560], fill=LINE)
    d.rectangle([px(z), 500, px(total), 560], fill=ROSE)
    label(d, px(total) + 20, 530, "= %d" % total, TEXT, 28, True, "lm")
    d.line([px(z), 280, px(z), 640], fill=ROSE, width=6)
    label(d, px(z), 650, "z = %d: the threat arrives (a scenario)" % z, ROSE, 28, True, "ma")
    label(d, px(0), 600, "recorded today", MUTED, 24, False, "la")
    fit_mono(d, W // 2, 860, "%d + %d = %d > %d  ->  %s years late" % (x, y, total, z, verdict.split()[1]), ROSE, 34, 1700, "ma")
    caption(d, "Data recorded today is exposed if secrecy plus migration outlasts the threat.", AMBER, 940)


def size_bars(img, d, cfg):
    """Bytes on the wire: key exchange public keys and replies, signature keys and signatures."""
    kx = [r for r in table("ONE", "KEY EXCHANGE") if r[0] in ("X25519", "ECDH P-256", "ML-KEM-768")]
    sg = [r for r in table("ONE", "SIGNATURES") if r[0] in ("Ed25519", "ECDSA P-256", "ML-DSA-65")]
    big = max(max(r[1], r[2]) for r in kx + sg)
    x0, x1 = 420, 1560
    k = (x1 - x0) / big

    def block(y0, title, rows, a, b):
        header(d, 60, y0, title, CYAN)
        for i, (name, p, q) in enumerate(rows):
            y = y0 + 65 + i * 110
            colour = AMBER if name.startswith("ML-") else GREEN
            label(d, 60, y + 22, name, TEXT, 28, True, "lm")
            d.rectangle([x0, y, x0 + max(4, p * k), y + 36], fill=colour)
            label(d, x0 + max(4, p * k) + 12, y + 18, "%s %d" % (a, p), colour, 24, True, "lm")
            d.rectangle([x0, y + 44, x0 + max(4, q * k), y + 80], fill=LINE)
            label(d, x0 + max(4, q * k) + 12, y + 62, "%s %d" % (b, q), MUTED, 24, True, "lm")
    block(170, "KEY EXCHANGE (bytes)", kx, "public key", "reply")
    block(540, "SIGNATURES (bytes)", sg, "public key", "signature")
    larger = facts("ONE", "HOW MUCH LARGER")
    caption(d, "ML-KEM-768's public key is %s; ML-DSA-65 adds kilobytes per signature." % larger["public key"].split("= ")[1].replace("times", "times X25519's"), AMBER, 935)


def kem_flow(img, d, cfg):
    """An ML-KEM exchange with its real sizes, and what a flipped bit does."""
    sender = facts("TWO", "THE SENDER")
    bank = facts("TWO", "THE BANK")
    tamper = facts("TWO", "A TAMPERED")
    header(d, 60, 170, "THE SENDER", CYAN, 500)
    header(d, 1360, 170, "THE BANK", GREEN, 500)
    cell(d, 60, 240, 500, 64, "bank's public key", CYAN, 26)
    label(d, 310, 315, sender["public key"].split(",")[0], MUTED, 24, False, "ma")
    cell(d, 60, 370, 500, 64, "encapsulate", CYAN, 28)
    draw.arrow_right(d, 570, 402, 1350, AMBER, 5)
    label(d, 960, 360, "ciphertext, " + sender["ciphertext"].split(",")[0], AMBER, 28, True, "ma")
    cell(d, 1360, 370, 500, 64, "decapsulate", GREEN, 28)
    cell(d, 60, 480, 500, 64, "shared key " + sender["shared key"].split(",")[0], CYAN, 26)
    cell(d, 1360, 480, 500, 64, "shared key " + bank["shared key"].split(",")[0], GREEN, 26)
    same = bank["shared key"].rsplit(": ", 1)[1]
    label(d, 960, 500, "the same: " + same, GREEN, 30, True, "ma")
    header(d, 60, 600, "A TAMPERED CIPHERTEXT (one bit flipped)", ROSE)
    steps = [("error", tamper["error"]), ("key", tamper["key"]), ("first use", tamper["first use"])]
    for i, (k, v) in enumerate(steps):
        x = 60 + i * 610
        cell(d, x, 670, 580, 110, "", ROSE if k == "first use" else AMBER)
        label(d, x + 290, 690, k, MUTED, 24, True, "ma")
        fit_mono(d, x + 290, 745, v, TEXT, 24, 560, "mm")
    caption(d, "A wrong ciphertext gives a wrong key without an error; the first use fails.", AMBER, 920)


def dsa_checks(img, d, cfg):
    """One signed statement and the five checks."""
    st = section("THREE", "THE STATEMENT")
    sizes = facts("THREE", "THE STATEMENT")
    checks = [split(ln) for ln in section("THREE", "THE CHECKS")]
    header(d, 60, 170, "THE STATEMENT, SIGNED WITH ML-DSA-65", CYAN)
    fit_mono(d, 60, 250, st[0].strip(), TEXT, 30, 1800)
    cell(d, 60, 310, 560, 64, "public key " + sizes["public key"], CYAN, 26)
    cell(d, 660, 310, 560, 64, "signature " + sizes["signature"], AMBER, 26)
    header(d, 60, 420, "THE CHECKS", GREEN)
    for i, (what, answer) in enumerate(checks):
        y = 485 + i * 78
        cell(d, 60, y, 560, 60, what, CYAN, 26)
        good = what in ("as signed",) or what.startswith("signed a second")
        bad_answer = answer.endswith("no")
        colour = GREEN if (good and not bad_answer) or (not good and bad_answer) else ROSE
        fit_mono(d, 660, y + 30, answer, colour, 28, 1200, "lm")
    caption(d, "Green is the right answer: only the signed statement, with its own key, verifies.", AMBER, 925)


def hybrid_combine(img, d, cfg):
    """Two secrets joined by HKDF with the transcript, and what an attacker who breaks one part gets."""
    how = facts("FOUR", "HOW THE KEY IS MADE")
    attack = [ln for ln in section("FOUR", "A RECORDING")]
    sec = re.findall(r"(X25519|ML-KEM) (\d+)", how["secrets"])
    header(d, 60, 170, "HOW THE KEY IS MADE (HKDF-SHA256)", CYAN)
    cell(d, 60, 240, 300, 64, "%s %s bytes" % sec[0], GREEN, 26)
    cell(d, 60, 330, 300, 64, "%s %s bytes" % sec[1], AMBER, 26)
    draw.arrow_right(d, 370, 317, 440, MUTED, 4)
    cell(d, 450, 285, 280, 64, "secrets " + how["secrets"].split("= ")[1], TEXT, 24)
    draw.arrow_right(d, 740, 317, 800, MUTED, 4)
    cell(d, 810, 285, 280, 64, "extract", CYAN, 28)
    label(d, 950, 360, how["extract"].split("= ")[1], MUTED, 22, False, "ma")
    draw.arrow_right(d, 1100, 317, 1160, MUTED, 4)
    cell(d, 1170, 285, 280, 64, "expand", CYAN, 28)
    label(d, 1310, 360, "context " + how["context"].split("= ")[1], MUTED, 22, False, "ma")
    draw.arrow_right(d, 1460, 317, 1520, MUTED, 4)
    cell(d, 1530, 285, 330, 64, "key " + how["expand"].split("= ")[1], GREEN, 26)
    fit_mono(d, 60, 420, how["result"], GREEN, 26, 1800)
    header(d, 60, 500, "A RECORDING, ATTACKED YEARS LATER", ROSE)
    who = ""
    for i, ln in enumerate(attack):
        parts = split(ln)
        if not ln.startswith(" "):
            who, text = parts[0], parts[1]
        else:
            text = ln.strip()
        y = 570 + i * 62
        if not ln.startswith(" "):
            cell(d, 60, y, 330, 52, who, ROSE if who.endswith("broken") else AMBER, 24)
        colour = ROSE if "X25519-only" in text else MUTED if "recovered" in text else GREEN
        fit_mono(d, 420, y + 26, text, colour, 26, 1440, "lm")
    caption(d, "Breaking one part is not enough: the hybrid key needs both secrets.", AMBER, 935)


def plan_grid(img, d, cfg):
    """Mosca's result for each data class and threat date, and the inventory in migration order."""
    grid = section("FIVE", "THE RESULT FOR EACH")
    zs = re.findall(r"z=\d+", grid[0])
    inv = [re.split(r"\s{2,}", ln.strip()) for ln in section("FIVE", "THE INVENTORY")]
    header(d, 60, 170, "MOSCA: DATA RECORDED TODAY", CYAN, 860)
    for j, z in enumerate(zs):
        label(d, 450 + j * 165 + 75, 240, z, TEXT, 26, True, "ma")
    for i, ln in enumerate(grid[1:]):
        parts = re.split(r"\s{2,}", ln.strip())
        y = 285 + i * 78
        label(d, 60, y + 30, parts[0], TEXT, 26, True, "lm")
        for j, v in enumerate(parts[1:]):
            colour = ROSE if v.startswith("LATE") or parts[0].startswith("exposed") else GREEN
            cell(d, 450 + j * 165, y, 150, 60, v, colour, 24)
    header(d, 1000, 170, "INVENTORY: WHAT TO MOVE FIRST", AMBER, 860)
    order = sorted(inv, key=lambda r: r[2])
    for i, (path, algs, use) in enumerate(order):
        y = 240 + i * 82
        colour = ROSE if use.startswith("1") else AMBER if use.startswith("2") else GREEN
        cell(d, 1000, y, 70, 64, use[0], colour, 30)
        fit_mono(d, 1090, y + 16, path, TEXT, 24, 760, "lm")
        label(d, 1090, y + 48, algs + " - " + use[2:], MUTED, 22, False, "lm")
    caption(d, "Long-lived data and key exchange first; symmetric keys stay.", AMBER, 940)
