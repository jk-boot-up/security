"""Pictures for the patching and dependency hygiene video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, LINE, MONO, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
DIM = (18, 26, 44)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def sections(name, head):
    """Every section of an act whose header starts with `head`: (header line, its indented lines)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = (ln, [])
                found.append(cur)
        elif cur is not None:
            cur[1].append(ln[2:])
    return found


def part(name, head):
    """The indented lines under the first section header of an act that starts with `head`."""
    return sections(name, head)[0][1]


def first(name, pattern):
    """The first match of a regular expression in one act's lines (group 1 if any)."""
    for ln in act(name):
        m = re.search(pattern, ln)
        if m:
            return m.group(1) if m.groups() else m.group(0)
    raise ValueError(pattern)


def header(d, x, y, text, colour, w=None, size=26):
    """A section label such as BEFORE THE FIX: a filled pill with dark text."""
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, mono_font=True, width=3, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=width)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono_font else SANS_B, size),
           fill=text_colour or colour, anchor="mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def fit(d, x, y, text, colour, size, max_w, bold=True, anchor="la"):
    """Text shrunk until it fits max_w."""
    f = font(SANS_B if bold else SANS, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


# ------------------------------------------------------------------ figures
SEV = {"Critical": ROSE, "High": AMBER, "Medium": CYAN, "Low": GREEN}


def _matches():
    """[(id, ref, via, fix)] from act two's matches."""
    lines = part("TWO", "THE MATCHES")
    return [(lines[i].split()[0], lines[i].split()[1], lines[i + 1].strip()[4:], lines[i + 2].strip()[5:])
            for i in range(0, len(lines), 3)]


def _findings():
    """{id: (score, severity, feature, reachable)} from act three."""
    out = {}
    for ln in part("THREE", "THE FINDINGS"):
        m = re.match(r"(\S+)\s+([\d.]+) (\w+)\s+(\S+)\s+(.+)$", ln.strip())
        out[m.group(1)] = (m.group(2), m.group(3), m.group(4), m.group(5))
    return out


def one_finding(img, d, cfg):
    """One advisory followed all the way: the advisory, the path, the score, the action."""
    adv = part("TWO", "THE ADVISORIES")[0].split()
    aid, ref, via, fix = _matches()[0]
    score, sev, feature, reach = _findings()[aid]
    order = dict((ln.split()[1], ln.split("  ", 2)[-1].strip()) for ln in part("THREE", "THE ORDER"))
    steps = [("1. THE ADVISORY", [aid, "%s %s %s" % (adv[1], adv[2], adv[3]), "fixed in " + adv[-1]], CLIENT),
             ("2. THE MATCH", [ref, "via " + via.replace(" > ", " >"), "the version is in the range"], AMBER),
             ("3. HOW BAD", ["%s %s" % (score, sev), feature, "reachable" if reach == "yes" else "not reachable"],
             SEV[sev]),
             ("4. THE ACTION", [order[aid], fix.split(" (")[0], "(" + fix.split(" (")[1]], GREEN)]
    for i, (head, lines, c) in enumerate(steps):
        x = 100 + i * 440
        header(d, x, 230, head, c, w=400)
        panel(d, x, 300, 400, 330, c, 3)
        for j, t in enumerate(lines):
            fit(d, x + 200, 360 + j * 90, t, TEXT if j == 0 else MUTED, 30 if j == 0 else 24, 370, j == 0, "mm")
        if i < 3:
            draw.arrow_right(d, x + 404, 465, x + 436, MUTED, 4)
    label(d, 960, 700, "An inventory, a list of known problems, a score, a deadline and a pin:", TEXT, 30, True, "ma")
    caption(d, "each step answers one question, and the answers are written down.", AMBER, 760)


def tree(img, d, cfg):
    """The store's dependency tree: three libraries chosen, three that came with them."""
    entries = part("ONE", "THE INVENTORY")
    direct = part("ONE", "WHO CHOSE THEM")[0].split(":")[1].split()
    header(d, 100, 205, "THE INVENTORY", CYAN)
    pos = {}
    for i, e in enumerate(entries):
        depth = (len(e) - len(e.lstrip())) // 2
        ref = e.strip()
        name = ref.split("@")[0]
        x, y = 140 + depth * 300, 280 + i * 84
        c = CLIENT if depth == 0 else GREEN if name in direct else AMBER
        cell(d, x, y, 360, 64, ref, c, 26)
        pos[i] = (depth, x, y)
        if depth:
            parent = max(k for k in pos if k < i and pos[k][0] == depth - 1)
            px, py = pos[parent][1], pos[parent][2]
            d.line([(px + 40, py + 64), (px + 40, y + 32), (x, y + 32)], fill=MUTED, width=3)
    panel(d, 1180, 290, 640, 380, LINE)
    label(d, 1210, 330, "green: direct", GREEN, 30, True)
    label(d, 1210, 370, "chosen by the team, in its build file", MUTED, 24)
    label(d, 1210, 440, "amber: transitive", AMBER, 30, True)
    label(d, 1210, 480, "came on their own, as dependencies", MUTED, 24)
    label(d, 1210, 512, "of dependencies", MUTED, 24)
    label(d, 1210, 590, act("ONE")[-1].split(":")[0], TEXT, 28, True)
    caption(d, "The inventory is a software bill of materials: an SBOM.", AMBER, 880)


def versions(img, d, cfg):
    """Why versions are compared as numbers, and how an affected range is read."""
    cmp = part("TWO", "COMPARING VERSIONS")
    header(d, 100, 205, "AS TEXT: CHARACTER BY CHARACTER", ROSE)
    a, b = "1.10.0", "1.9.0"
    for k, v in enumerate([a, b]):
        for j, ch in enumerate(v):
            c = ROSE if j == 2 else MUTED
            cell(d, 100 + j * 76, 275 + k * 90, 66, 70, ch, c, 34, fill=BG_CELL if j == 2 else DIM)
    label(d, 600, 310, "'1' comes before '9'", ROSE, 28, True, "lm")
    label(d, 600, 400, cmp[0].strip().split("  <-")[0], MUTED, 24, False, "lm")
    header(d, 100, 480, "AS NUMBERS: PART BY PART", GREEN)
    for k, v in enumerate([a, b]):
        for j, part_ in enumerate(v.split(".")):
            c = GREEN if j == 1 else MUTED
            cell(d, 100 + j * 130, 545 + k * 90, 110, 70, part_, c, 34, fill=BG_CELL if j == 1 else DIM)
    label(d, 520, 580, "10 is more than 9", GREEN, 28, True, "lm")
    label(d, 520, 670, cmp[1].strip(), MUTED, 24, False, "lm")
    m = re.search(r"in (>=\S+ <\S+): (\w+), (\S+) is: (\w+)", cmp[2])
    rng, in1, v2, in2 = m.groups()
    lo, hi = rng.split()[0][2:], rng.split()[1][1:]
    header(d, 1120, 205, "AN AFFECTED RANGE", AMBER)
    mono(d, 1120, 290, rng, AMBER, 34)
    ticks = ["1.4.0", lo, "1.8.2", hi, "1.9.0"]
    x0, step, y = 1140, 170, 470
    d.rounded_rectangle([x0 + step, y - 30, x0 + 3 * step, y + 30], radius=10, fill=(70, 50, 20), outline=AMBER, width=3)
    label(d, x0 + 2 * step, y - 60, "affected", AMBER, 24, True, "mm")
    for i, t in enumerate(ticks):
        d.line([(x0 + i * step, y - 40), (x0 + i * step, y + 40)], fill=MUTED, width=3)
        label(d, x0 + i * step, y + 60, t, TEXT, 24, True, "ma")
    cell(d, 1120, 600, 330, 70, "1.8.2: " + in1, ROSE, 28)
    cell(d, 1470, 600, 330, 70, v2 + ": " + in2, GREEN, 28)
    label(d, 1120, 700, ">= means from, and including", MUTED, 24)
    label(d, 1120, 735, "<  means up to, not including", MUTED, 24)
    caption(d, "1.8.3 is the fixed version: the first one outside the range.", AMBER, 860)


def cvss_steps(img, d, cfg):
    """How 9.8 is made from the vector, with the real weights."""
    vector = part("THREE", "THE VECTOR")[0].strip()
    steps = [ln.strip() for ln in part("THREE", "THE SCORE")]
    names = {"AV": "attack vector", "AC": "complexity", "PR": "privileges", "UI": "user action",
             "S": "scope", "C": "confidentiality", "I": "integrity", "A": "availability"}
    cia = {"H": "high", "L": "low", "N": "none"}
    words = {"AV": {"N": "network", "A": "adjacent", "L": "local", "P": "physical"}, "AC": {"L": "low", "H": "high"},
             "PR": {"N": "none", "L": "low", "H": "high"}, "UI": {"N": "none", "R": "required"},
             "S": {"U": "unchanged", "C": "changed"}, "C": cia, "I": cia, "A": cia}
    header(d, 100, 205, "THE VECTOR", CLIENT)
    for i, metric in enumerate(vector.split("/")):
        k, v = metric.split(":")
        x = 100 + i * 215
        c = ROSE if k in "CIA" and v == "H" else CLIENT
        cell(d, x, 270, 195, 64, metric, c, 28)
        label(d, x + 97, 350, names[k], MUTED, 19, False, "ma")
        val = words[k][v]
        label(d, x + 97, 378, val, TEXT, 20, True, "ma")
    header(d, 100, 440, "THE SCORE, STEP BY STEP", AMBER)
    labels = ["how much is lost (C, I, A)", "the impact", "how easy it is", "add the two", "round up, one decimal"]
    for i, st in enumerate(steps):
        name, formula = st.split(None, 1)
        y = 505 + i * 82
        cell(d, 100, y, 180, 66, name, AMBER, 26)
        mono(d, 300, y + 33, formula, TEXT if i < 4 else ROSE, 28, "lm")
        label(d, 1820, y + 33, labels[i], MUTED, 22, False, "rm")
    caption(d, "Weights: H is 0.56 lost; network is 0.85; low complexity 0.77; none needed 0.85.", MUTED, 930)


def windows(img, d, cfg):
    """Each finding's patch window on a day line, and where today falls."""
    rows = part("FOUR", "THE FINDINGS ON DAY")[1:]
    today = int(first("FOUR", r"THE FINDINGS ON DAY (\d+)"))
    sev = {k: v[1] for k, v in _findings().items()}
    hx = header(d, 100, 205, "THE DEADLINES", CYAN)
    label(d, hx + 20, 227, part("FOUR", "THE DEADLINES")[0].strip() + " days", TEXT, 28, True, "lm")
    x0, x1 = 420, 1560
    span = 110
    xd = lambda day: x0 + (x1 - x0) * day / span
    for i, ln in enumerate(rows):
        m = re.match(r"(\S+)\s+day (\d+)\s+day (\d+)\s+(.+)$", ln.strip())
        aid, pub, dl, state = m.group(1), int(m.group(2)), int(m.group(3)), m.group(4)
        y = 330 + i * 150
        c = SEV[sev[aid]]
        label(d, 100, y + 30, aid, TEXT, 26, True, "lm")
        label(d, 100, y + 66, sev[aid], c, 24, True, "lm")
        d.rounded_rectangle([xd(pub), y + 10, xd(dl), y + 60], radius=10, fill=DIM, outline=c, width=3)
        label(d, xd(dl) + 10, y + 35, "deadline day %d" % dl, c, 22, True, "lm")
        bad = "BREACHED" in state
        sc = ROSE if bad else GREEN
        label(d, xd(pub) + 10, y + 90, state, sc, 24, True, "la")
        p = re.search(r"patched day (\d+)", state)
        if p:
            px = xd(int(p.group(1)))
            d.ellipse([px - 12, y + 23, px + 12, y + 47], fill=GREEN)
    d.line([(xd(today), 300), (xd(today), 770)], fill=AMBER, width=4)
    label(d, xd(today), 780, "today: day %d" % today, AMBER, 26, True, "ma")
    for day in range(0, span + 1, 10):
        label(d, xd(day), 830, str(day), MUTED, 20, False, "ma")
    caption(d, "The breach is the finding judged not reachable: low priority still needs a deadline.", AMBER, 900)


def pin_check(img, d, cfg):
    """A pinned hash against a download with one bit flipped, and the three results."""
    flip = [ln for ln in act("FIVE") if ln.startswith("ONE BIT FLIPPED")][0]
    hashes = part("FIVE", "ONE BIT FLIPPED")
    downloads = part("FIVE", "THE DOWNLOADS")
    m = re.search(r"0x(\w\w) '(.)' becomes 0x(\w\w) '(.)'", flip)
    header(d, 100, 205, "ONE BIT FLIPPED IN THE DOWNLOAD", AMBER)
    b1, b2 = format(int(m.group(1), 16), "08b"), format(int(m.group(3), 16), "08b")
    for k, (bits, ch, hx, c) in enumerate([(b1, m.group(2), m.group(1), GREEN), (b2, m.group(4), m.group(3), ROSE)]):
        y = 270 + k * 90
        label(d, 100, y + 35, ["reviewed", "download"][k], c, 26, True, "lm")
        for j, bit in enumerate(bits):
            diff = b1[j] != b2[j]
            cell(d, 300 + j * 66, y, 58, 70, bit, (ROSE if k else GREEN) if diff else MUTED, 32,
                 fill=BG_CELL if diff else DIM)
        label(d, 860, y + 35, "0x%s  '%s'" % (hx, ch), TEXT, 28, True, "lm")
    header(d, 1120, 205, "THE SHA-256 OF EACH", CYAN)
    for k, ln in enumerate(hashes):
        who, h = ln.split()
        y = 270 + k * 90
        c = GREEN if k == 0 else ROSE
        panel(d, 1120, y, 700, 70, c)
        mono(d, 1140, y + 35, h[:24] + "...", c, 26, "lm")
    label(d, 1470, 450, "one bit in, a completely different hash out", AMBER, 24, True, "ma")
    header(d, 100, 520, "THE DOWNLOADS, AND THE CHECK", CLIENT)
    for i, ln in enumerate(downloads):
        what, res = re.match(r"(.+?)\s+(VERIFIED|MISMATCH|NOT_PINNED)", ln.strip()).groups()
        y = 585 + i * 90
        c = GREEN if res == "VERIFIED" else ROSE
        panel(d, 100, y, 820, 70, CLIENT)
        mono(d, 125, y + 35, what, TEXT, 26, "lm")
        draw.arrow_right(d, 930, y + 35, 1010, MUTED, 4)
        cell(d, 1020, y, 300, 70, res, c, 30)
        label(d, 1340, y + 35, "used" if res == "VERIFIED" else "refused", c, 28, True, "lm")
    caption(d, "Only a download whose hash matches a reviewed pin is used.", AMBER, 900)
