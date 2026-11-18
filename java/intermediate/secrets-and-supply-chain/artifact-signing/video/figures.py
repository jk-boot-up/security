"""Pictures for the artifact signing video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
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
    """The core idea on one release: the digest of the exact bytes, signed; the deployment checks with the pinned key."""
    before = facts(section("ONE", "BEFORE THE MIRROR"))
    after = facts(section("ONE", "AFTER THE MIRROR"))
    signed = facts(section("TWO", "AFTER SIGNING"))
    results = verdicts(section("TWO", "THE RESULTS"))
    real = next(r for r in results if r[0] == "the real jar")
    changed = next(r for r in results if r[0] == "one byte changed")
    header(d, 60, 215, "THE RELEASE TEAM SIGNS", CYAN)
    stops = [("the jar", before["file"].split(",")[0], before["file"].split(", ")[1], TEXT),
             ("sha256 of its bytes", hex16(before["sha256"]), "32 bytes, shown 8", CYAN),
             ("the statement", "name, version,", "channel, sha256, day", TEXT),
             ("Ed25519 signature", hex16(signed["signature"]), "64 bytes, key stable-2026a", AMBER)]
    for i, (top, main, sub, colour) in enumerate(stops):
        x = 60 + i * 460
        label(d, x + 190, 285, top, MUTED, 24, True, "ma")
        panel(d, x, 320, 380, 120, colour, 3)
        fit_mono(d, x + 190, 360, main, colour, 28, 350, "mm")
        label(d, x + 190, 405, sub, MUTED, 22, False, "mm")
        if i < 3:
            draw.arrow_right(d, x + 390, 380, x + 450, MUTED, 4)
    header(d, 60, 500, "THE DEPLOYMENT CHECKS, WITH THE PINNED PUBLIC KEY", GREEN)
    mono(d, 60, 575, "public key " + hex16(signed["public key"]) + "...", MUTED, 26)
    for i, (what, digest, verdict) in enumerate(((real[0], hex16(before["sha256"]), real),
                                                 (changed[0], hex16(after["sha256"]), changed))):
        y = 640 + i * 130
        colour = word_colour(verdict[1])
        panel(d, 60, y, 1800, 104, colour, 3)
        label(d, 90, y + 52, what, TEXT, 30, True, "lm")
        mono(d, 440, y + 52, "sha256 " + digest, CYAN if verdict[1] == "accepted" else ROSE, 28, "lm")
        mono(d, 920, y + 52, "=" if verdict[1] == "accepted" else "≠", colour, 40, "mm")
        mono(d, 960, y + 52, "signed " + hex16(before["sha256"]), CYAN, 28, "lm")
        cell(d, 1500, y + 20, 320, 64, verdict[1], colour, 30)
    caption(d, "One changed byte gives a new digest, and the signed digest no longer matches.", AMBER, 935)


def one_byte(img, d, cfg):
    """One bit flipped on a mirror: the byte, its bits and the digest before and after, and a step that runs it anyway."""
    before = facts(section("ONE", "BEFORE THE MIRROR"))
    after = facts(section("ONE", "AFTER THE MIRROR"))
    key = next(k for k in before if k.startswith("byte "))
    trusting = verdicts(section("ONE", "THE TRUSTING"))[0] if verdicts(section("ONE", "THE TRUSTING")) else None
    trust_line = section("ONE", "THE TRUSTING")[0].strip()
    for i, (title, facts_, colour) in enumerate((("BEFORE THE MIRROR", before, GREEN), ("AFTER THE MIRROR", after, ROSE))):
        x = 60 + i * 930
        header(d, x, 215, title, colour)
        panel(d, x, 280, 870, 420, colour, 3)
        label(d, x + 30, 320, "file", MUTED, 26, True)
        mono(d, x + 200, 320, facts_["file"], TEXT, 26)
        label(d, x + 30, 390, key, MUTED, 26, True)
        value = re.match(r"'(.)' \(0x([0-9a-f]{2})\)", facts_[key])
        mono(d, x + 200, 390, "'%s'  0x%s" % value.groups(), TEXT, 30)
        bits = format(int(value.group(2), 16), "08b")
        other = format(int(re.search(r"0x([0-9a-f]{2})", (after if i == 0 else before)[key]).group(1), 16), "08b")
        label(d, x + 30, 460, "its bits", MUTED, 26, True)
        for j, (b, o) in enumerate(zip(bits, other)):
            cell(d, x + 200 + j * 70, 450, 60, 60, b, AMBER if b != o else LINE, 30,
                 text_colour=AMBER if b != o else TEXT)
        label(d, x + 30, 560, "sha256", MUTED, 26, True)
        mono(d, x + 200, 560, hex16(facts_["sha256"]) + "...", colour, 34)
        if i == 1:
            label(d, x + 200, 620, "every hex digit changed, from one bit", ROSE, 24, True)
    draw.arrow_right(d, 940, 490, 985, MUTED, 5)
    header(d, 60, 740, "THE TRUSTING DEPLOYMENT STEP", ROSE)
    word = trust_line.split()[0]
    cell(d, 60, 810, 200, 64, word, ROSE, 28)
    fit_mono(d, 290, 842, trust_line[len(word):].strip(), TEXT, 28, 1500, "lm")
    caption(d, "Nothing checked the bytes, so an altered jar is running.", ROSE, 920)


CHECK_OF = [("no signature", 1), ("not a release statement", 1), ("unknown key", 2), ("bad signature", 3),
            (" signs ", 4), ("signed day", 5), ("signed for", 6), ("jar differs", 6)]


def stops_at(reason):
    """The number of the check that refuses with this reason, or 0 when every check passes."""
    return next((n for text, n in CHECK_OF if text in reason), 0)


def check_grid(img, d, cfg):
    """The six checks in order, and where each of the six attempts is stopped."""
    checks = [re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in section("TWO", "HOW THE DEPLOYMENT CHECKS")]
    rows = verdicts(section("TWO", "THE RESULTS"))
    header(d, 60, 210, "THE CHECKS, IN ORDER", CYAN)
    header(d, 990, 210, "WHERE EACH ATTEMPT STOPS", AMBER)
    for i, (num, what) in enumerate(checks):
        y = 275 + i * 78
        cell(d, 60, y, 200, 62, num, CYAN, 26)
        fit_mono(d, 280, y + 31, what, TEXT, 24, 660, "lm")
    x0, step = 1265, 84
    for j in range(len(checks)):
        label(d, x0 + 30 + j * step, 290, str(j + 1), CYAN, 28, True, "ma")
    for i, (what, word, reason) in enumerate(rows):
        y = 340 + i * 78
        stop = stops_at(reason)
        label(d, 990, y + 27, what, TEXT, 24, True, "lm")
        for j in range(len(checks)):
            n = j + 1
            if stop == 0 or n < stop:
                colour, mark = GREEN, "✓"
            elif n == stop:
                colour, mark = ROSE, "✗"
            else:
                colour, mark = LINE, "·"
            d.rounded_rectangle([x0 + j * step, y, x0 + j * step + 60, y + 54], radius=10, fill=BG_CELL, outline=colour, width=3)
            label(d, x0 + 30 + j * step, y + 27, mark, colour, 30, True, "mm")
        label(d, x0 + len(checks) * step + 4, y + 27, "deploy" if stop == 0 else "", GREEN, 26, True, "lm")
    caption(d, "Only the real jar passes all six; each change is caught at its own check.", AMBER, 935)


def key_timeline(img, d, cfg):
    """The two stable keys by day, the overlap, the signatures, and a thief's backdated one."""
    rot = facts(section("THREE", "THE ROTATION"))
    names = [k for k in rot if k.startswith("stable-")]
    a_from, a_revoked = map(int, re.findall(r"day (\d+)", rot[names[0]]))
    b_from = int(re.search(r"day (\d+)", rot[names[1]]).group(1))
    overlap = re.search(r"= (\d+) days", rot["the overlap"]).group(1)
    signed = verdicts(section("THREE", "SIGNED ON A DAY"))
    thief = facts(section("THREE", "BEFORE: A THIEF"))
    thief_day = int(re.search(r"ON DAY (\d+)", heading_of("THREE", "BEFORE: A THIEF")).group(1))
    claimed = int(re.search(r"signed-day=(\d+)", thief["writes"]).group(1))
    thief_word = verdicts(section("THREE", "AFTER: THE VERDICT"))[0] if verdicts(section("THREE", "AFTER: THE VERDICT")) else None
    thief_word = thief_word[1] if thief_word else section("THREE", "AFTER: THE VERDICT")[0].split()[0]
    x0, x1, lo, hi = 380, 1840, 0, 170

    def px(day):
        return x0 + (day - lo) * (x1 - x0) / (hi - lo)
    header(d, 60, 205, "THE STABLE KEYS, DAY BY DAY", CYAN)
    for day in range(0, hi + 1, 10):
        d.line([px(day), 300, px(day), 310], fill=LINE, width=2)
        if day % 20 == 0 or day in (a_revoked, b_from):
            label(d, px(day), 315, str(day), MUTED, 20, False, "ma")
    d.line([x0, 300, x1, 300], fill=LINE, width=3)
    for i, name in enumerate(names):
        y = 360 + i * 90
        mono(d, 60, y + 25, name, TEXT, 30, "lm")
        start = a_from if i == 0 else b_from
        end = a_revoked if i == 0 else hi
        d.rectangle([px(start), y, px(end), y + 50], fill=GREEN)
        if i == 0:
            d.rectangle([px(a_revoked), y, px(hi), y + 50], fill=BG_CELL, outline=ROSE, width=2)
            label(d, px(a_revoked) + 12, y + 25, "revoked on day %d" % a_revoked, ROSE, 24, True, "lm")
    d.rectangle([px(b_from), 345, px(a_revoked), 510], outline=AMBER, width=4)
    label(d, (px(b_from) + px(a_revoked)) / 2, 520, "overlap: %s days" % overlap, AMBER, 26, True, "ma")
    header(d, 60, 585, "SIGNED ON A DAY, THE VERDICT", AMBER)
    for i, (what, word, reason) in enumerate(signed):
        day = int(re.search(r"day (\d+)", what).group(1))
        y = 660 + i * 50
        colour = word_colour(word)
        d.ellipse([px(day) - 13, y - 13, px(day) + 13, y + 13], fill=colour)
        mono(d, 60, y, what, TEXT, 24, "lm")
        label(d, px(day) + 26, y, word, colour, 24, True, "lm")
    y = 885
    mono(d, 60, y, "a thief, day %d" % thief_day, ROSE, 24, "lm")
    d.ellipse([px(thief_day) - 13, y - 13, px(thief_day) + 13, y + 13], outline=ROSE, width=4)
    d.line([px(thief_day) - 18, y, px(claimed) + 22, y], fill=ROSE, width=4)
    d.polygon([(px(claimed) + 16, y - 10), (px(claimed) + 16, y + 10), (px(claimed), y)], fill=ROSE)
    label(d, px(claimed) - 20, y, "writes day %d: %s" % (claimed, thief_word), ROSE, 24, True, "rm")
    caption(d, "The signer writes the day itself, so a stolen key can backdate.", ROSE, 945)


def merkle_log(img, d, cfg):
    """The log's two leaves, how its root is made, the proof for entry 1, and the two forked views of size three."""
    root_rows = facts(section("FOUR", "HOW THE ROOT IS MADE"))
    leaf0 = root_rows["leaf 0"].split("= ")[1]
    leaf1 = root_rows["leaf 1"].split("= ")[1]
    root = root_rows["root"].split("= ")[1]
    entries = facts(section("FOUR", "THE LOG ("))
    views = facts(section("FOUR", "HOW EACH VIEW EXTENDS"))
    shown = facts(section("FOUR", "A LOG THAT HIDES"))
    compare = facts(section("FOUR", "THE MONITOR AND THE CUSTOMER"))
    header(d, 60, 210, "THE SIGNED HEAD OF SIZE 2", CYAN)
    cell(d, 250, 290, 400, 76, "root " + root[:8], CYAN, 32)
    for i, (leaf, entry) in enumerate(((leaf0, "entry 0"), (leaf1, "entry 1"))):
        x = 40 + i * 440
        colour = AMBER if i == 0 else TEXT
        d.line([x + 200, 470, 450, 366], fill=LINE, width=3)
        cell(d, x, 470, 400, 70, "leaf %d  %s" % (i, leaf[:8]), colour, 28)
        d.line([x + 200, 540, x + 200, 590], fill=LINE, width=3)
        cell(d, x, 590, 400, 64, entry, MUTED, 26)
        fit_mono(d, x + 200, 670, entries[entry].split(",")[0], MUTED, 24, 400, "ma")
    mono(d, 40, 740, "leaf = sha256(00 || entry)", TEXT, 26)
    mono(d, 40, 785, "node = sha256(01 || left || right)", TEXT, 26)
    label(d, 40, 835, "the proof for entry 1 is one hash: leaf 0, in amber", AMBER, 26, True)
    d.line([945, 215, 945, 890], fill=LINE, width=2)
    header(d, 985, 210, "TWO VIEWS OF SIZE 3, BOTH SIGNED", ROSE)
    for i, who in enumerate(("monitor", "customer")):
        x = 985 + i * 470
        m = re.search(r"\|\| (\w+) \|\| (\w+)\) = (\w+)", views[who])
        old, leaf2, new = m.groups()
        colour = GREEN if i == 0 else ROSE
        cell(d, x + 40, 290, 360, 76, "root " + new[:8], colour, 30)
        d.line([x + 110, 366, x + 110, 470], fill=LINE, width=3)
        d.line([x + 330, 366, x + 330, 470], fill=LINE, width=3)
        cell(d, x, 470, 215, 70, old[:8], CYAN, 26)
        cell(d, x + 230, 470, 200, 70, leaf2[:8], colour, 26)
        label(d, x + 107, 555, "old root", MUTED, 24, True, "ma")
        label(d, x + 330, 555, "leaf 2", MUTED, 24, True, "ma")
        label(d, x + 215, 610, "to the " + who, colour, 28, True, "ma")
        fit_mono(d, x + 215, 655, shown[who].split(",")[0].replace("entry 2 = ", ""), MUTED, 24, 440, "ma")
    label(d, 985, 740, "both extend the old head: " + views["both"].split(": ")[1], TEXT, 28, True)
    label(d, 985, 790, "same size " + compare["same size"].split(",")[0] + ", two different roots", ROSE, 28, True)
    label(d, 985, 840, "fork " + compare["fork"].split("  <-")[0], ROSE, 30, True)
    caption(d, "A log cannot show two histories without signing two roots for one size.", AMBER, 935)


def gate_paths(img, d, cfg):
    """The path a release takes, where the key and the log sit, and the path around the gate."""
    gate = verdicts(section("FIVE", "THE GATE"))[0]
    jewel = facts(section("FIVE", "THE SIGNING KEY"))
    bypass = facts(section("FIVE", "THE GATE"))
    rotation = " ".join(ln.strip() for ln in section("FIVE", "ROTATION"))
    stops = [("build", "the jar", TEXT), ("sign", "key in hardware", AMBER), ("log", "public, append-only", CYAN),
             ("gate", "checks all", GREEN), ("server", "runs it", TEXT)]
    header(d, 60, 205, "THE PATH A RELEASE TAKES", CYAN)
    for i, (name, sub, colour) in enumerate(stops):
        x = 60 + i * 370
        draw.box(d, x, 290, 300, 130, name, sub, colour, 34, 24)
        if i < 4:
            draw.arrow_right(d, x + 305, 355, x + 365, MUTED, 4)
    dashed(d, [(210, 420), (210, 520), (1690, 520), (1690, 425)], ROSE, 4)
    d.polygon([(1680, 440), (1700, 440), (1690, 422)], fill=ROSE)
    label(d, 950, 535, "copied by hand: " + bypass["copied by hand"].split(": ")[0], ROSE, 26, True, "ma")
    header(d, 60, 610, "THE GATE ON ITS PATH", GREEN)
    cell(d, 60, 670, 200, 60, gate[1], word_colour(gate[1]), 26)
    label(d, 280, 700, "%s: %s" % (gate[0], gate[2]), TEXT, 26, True, "lm")
    header(d, 980, 610, "THE SIGNING KEY", AMBER)
    label(d, 980, 680, "here: " + jewel["here"], MUTED, 26, True)
    label(d, 980, 725, "in practice: " + jewel["in practice"], AMBER, 26, True)
    header(d, 60, 780, "ROTATION", CYAN)
    fit_mono(d, 60, 850, rotation, TEXT, 24, 1800)
    caption(d, "The gate protects only the paths that pass through it; lock the others.", AMBER, 935)
