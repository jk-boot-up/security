"""Pictures for the crypto agility video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (ACCENT, AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT,
                                   W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
STATUS = {"approved": GREEN, "to migrate": AMBER, "forbidden": ROSE, "not on the list": MUTED}
VERSION = {"v1": AMBER, "v2": CYAN, "v3": GREEN}


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


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def census(lines):
    """`v1/key1   10000 records` lines as a list of (version, key, count)."""
    return [(m.group(1), m.group(2), int(m.group(3))) for m in
            (re.match(r"(v\d)/(key\d)\s+(\d+) records", ln) for ln in lines) if m]


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


def caption(d, text, colour=AMBER, y=945):
    label(d, W // 2, y, text, colour, 30, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def header_bytes(d, x, y, hexes, colours, size=34, w=96):
    """The three header bytes as cells, with what each one means underneath."""
    for i, (h, c, what) in enumerate(zip(hexes.split(), colours, ("magic", "version", "key id"))):
        cell(d, x + i * (w + 10), y, w, 60, h, c, size)
        label(d, x + i * (w + 10) + w // 2, y + 70, what, MUTED, 20, True, "ma")


def census_bar(d, x, y, w, rows, total):
    """One bar split by version/key, each part as wide as its share of the records."""
    pos = x
    for v, k, n in rows:
        part = int(w * n / total)
        d.rounded_rectangle([pos, y, pos + max(part, 6), y + 64], radius=8, fill=VERSION[v])
        text = "%s/%s %d" % (v, k, n)
        f = font(MONO_B, 26)
        if f.getlength(text) < part - 16:
            d.text((pos + part // 2, y + 32), text, font=f, fill=INK, anchor="mm")
        else:
            d.text((pos + max(part, 6) + 10, y + 32), text, font=f, fill=VERSION[v], anchor="lm")
        pos += part


# ------------------------------------------------------------------ figures

def envelope_bytes(img, d, cfg):
    """One record sealed in each suite: where every byte of the envelope goes."""
    text, size = re.match(r"(.*) \((\d+) bytes\)", sec("TWO", "ONE RECORD")[0].strip()).groups()
    rows = [re.match(r"(v\d) (.+?)\s+(\d+) \+ (\d+) \+ (\d+) \+ (\d+) = (\d+)", ln.strip()).groups()
            for ln in sec("TWO", "THE SAME RECORD") if ln.strip().startswith("v")]
    pads = re.search(r"CBC pads (\d+) to (\d+)", "\n".join(sec("TWO", "THE SAME RECORD"))).groups()
    header(d, 60, 215, "BEFORE SEALING: ONE RECORD", CYAN)
    fit_mono(d, 60, 280, text, TEXT, 40, 1100)
    label(d, 60 + font(MONO_B, 40).getlength(text) + 30, 290, "%s bytes of text" % size, MUTED, 28, True)
    header(d, 60, 370, "AFTER SEALING: THE SAME RECORD IN EACH SUITE (bytes)", GREEN)
    parts = (("header", ACCENT), ("IV / nonce", CYAN), ("ciphertext", AMBER), ("tag", GREEN))
    longest = max(int(r[6]) for r in rows)
    x0, scale = 60, 1600 / longest
    for i, (v, suite, *nums) in enumerate(rows):
        y = 470 + i * 115
        label(d, 60, y - 40, v + "  " + suite, VERSION[v], 28, True)
        pos = x0
        for (name, colour), n in zip(parts, nums[:4]):
            wpx = int(n) * scale
            d.rectangle([pos, y, pos + wpx - 3, y + 56], fill=colour)
            f = font(MONO_B, 24)
            if f.getlength(n) < wpx - 6:
                d.text((pos + wpx / 2, y + 28), n, font=f, fill=INK, anchor="mm")
            pos += wpx
        label(d, pos + 14, y + 28, "= %s" % nums[4], TEXT, 30, True, "lm")
    lx = 60
    for name, colour in parts:
        d.rectangle([lx, 815, lx + 28, 843], fill=colour)
        label(d, lx + 40, 829, name, colour, 26, True, "lm")
        lx += 60 + font(SANS_B, 26).getlength(name) + 30
    label(d, 60, 870, "CBC pads %s bytes to whole 16-byte blocks: %s. GCM and ChaCha20 keep %s." % (pads[0], pads[1], pads[0]),
          MUTED, 26, True)
    caption(d, "The 3-byte header says which suite and which key sealed the record.", AMBER, 940)


def inventory_map(img, d, cfg):
    """The ten files of the bank's source, each name coloured by what the policy says about it."""
    status = {}
    for ln in sec("ONE", "THE INVENTORY"):
        m = re.match(r"\s*(\S+)\s+x(\d+)\s+(.+)$", ln)
        status[m.group(1)] = m.group(3)
    files = [re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in sec("ONE", "THE BANK'S SOURCE")]
    header(d, 60, 210, "BEFORE: THE NAMES IN THE SOURCE", CYAN)
    for i, (f, names) in enumerate(files):
        x, y = 60 + (i % 2) * 610, 275 + (i // 2) * 120
        panel(d, x, y, 590, 108)
        label(d, x + 18, y + 10, f + ".java", TEXT, 24, True)
        for j, n in enumerate(names.split(", ")):
            cell(d, x + 14 + j * 286, y + 46, 276, 48, n, STATUS[status[n]], 20)
    count = dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in sec("ONE", "THE COUNT"))
    header(d, 1300, 210, "AFTER: THE COUNT", AMBER)
    label(d, 1300, 285, "uses: " + count["uses"], TEXT, 32, True)
    y = 350
    for s, c in STATUS.items():
        n = int(count[s])
        cell(d, 1300, y, 120, 64, str(n), c, 34)
        label(d, 1440, y + 32, s, c, 30, True, "lm")
        y += 84
    label(d, 1300, 700, "fix first:", ROSE, 26, True)
    for i, f in enumerate(sec("ONE", "FILES TO FIX FIRST")[0].strip().split(", ")):
        label(d, 1300, 740 + i * 40, f, ROSE, 26)
    caption(d, "Green: approved. Amber: to migrate. Red: forbidden.", AMBER, 900)


def two_readers(img, d, cfg):
    """The store before and after the new suite, and two readers meeting one new record."""
    before = census(sec("TWO", "THE STORE, WRITTEN"))
    after = census(sec("TWO", "THE STORE, AFTER"))
    total = sum(n for *_, n in after)
    header(d, 60, 210, "BEFORE: THE STORE OF THE OLD SYSTEM", CYAN)
    census_bar(d, 60, 275, 1500, before, total)
    header(d, 60, 375, heading_of("TWO", "THE STORE, AFTER").rstrip(":").replace("THE STORE, ", ""), CYAN)
    census_bar(d, 60, 440, 1500, after, total)
    hexes = re.search(r"header ([0-9a-f ]+)\)", heading_of("TWO", "TWO READERS")).group(1)
    header(d, 60, 560, "ONE NEW RECORD", ACCENT)
    header_bytes(d, 60, 625, hexes, (ACCENT, CYAN, CYAN))
    label(d, 60, 735, "version 2 = AES-GCM, key 2", CYAN, 26, True)
    header(d, 800, 560, "THE RESULT: TWO READERS", GREEN)
    rows = [re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in sec("TWO", "TWO READERS")]
    for i, (who, res) in enumerate(rows):
        y = 630 + i * 100
        colour = ROSE if res.startswith("refused") else GREEN
        label(d, 800, y + 28, who, TEXT, 28, True, "lm")
        cell(d, 1300, y, 560, 60, res, colour, 26)
    caption(d, "The reader picks the suite from the header, so old and new live side by side.", AMBER, 900)


def migration_bars(img, d, cfg):
    """How read-repair and a batch move the records from key 1 to key 2, step by step."""
    steps = [("BEFORE THE MOVE", census(sec("THREE", "BEFORE THE MOVE"))),
             ("STEP 1: READ-REPAIR", census(sec("THREE", "STEP 1"))),
             ("STEP 2: A BATCH", census(sec("THREE", "STEP 2")))]
    total = sum(n for *_, n in steps[0][1])
    for i, (name, rows) in enumerate(steps):
        y = 220 + i * 150
        header(d, 60, y, name, AMBER if i else CYAN)
        census_bar(d, 60, y + 60, 1500, rows, total)
    adds = [ln.strip() for ln in sec("THREE", "HOW THE COUNTS ADD UP")]
    header(d, 60, 690, "HOW THE COUNTS ADD UP", AMBER)
    for i, ln in enumerate(adds):
        fit_mono(d, 60, 750 + i * 50, ln, VERSION["v%d" % (i + 1)], 30, 1100)
    res = sec("THREE", "AFTER THE MOVE")[0].strip()
    header(d, 1250, 690, "AFTER THE MOVE", GREEN)
    fit_mono(d, 1250, 755, res, GREEN, 30, 620)
    caption(d, "Rewrite what you read, then batch the rest: nothing is lost.", AMBER, 900)


def retire_backup(img, d, cfg):
    """The next rotation leaves no live record under keys 1 and 2, but a backup still needs key 1."""
    after = census(sec("FOUR", "AFTER A BATCH"))
    total = sum(n for *_, n in after)
    header(d, 60, 210, "AFTER THE NEXT ROTATION (ChaCha20-Poly1305, key 3)", GREEN)
    census_bar(d, 60, 275, 1500, after, total)
    live = [re.match(r"key (\d)\s+(\d+)", ln.strip()).groups() for ln in sec("FOUR", "LIVE RECORDS") if ln.strip().startswith("key")]
    header(d, 60, 390, "LIVE RECORDS PER KEY", CYAN)
    for i, (k, n) in enumerate(live):
        cell(d, 60 + i * 260, 455, 240, 80, "key %s: %s" % (k, n), GREEN if n != "0" else MUTED, 28)
    backup = sec("FOUR", "THE BACKUP, TAKEN")
    hexes = re.search(r"header ([0-9a-f ]+) ", backup[0]).group(1)
    header(d, 60, 590, "BEFORE: THE BACKUP (taken before the first move)", AMBER)
    label(d, 60, 660, backup[0].strip().split(",")[0], TEXT, 28, True)
    header_bytes(d, 360, 650, hexes, (ACCENT, AMBER, AMBER), 30, 86)
    fit_mono(d, 60, 770, re.sub(r"\s{2,}", ": ", backup[1].strip()), GREEN, 28, 800)
    retired = sec("FOUR", "THE BACKUP, AFTER")
    header(d, 1000, 590, "AFTER: KEY 1 IS RETIRED", ROSE)
    readable = re.sub(r"\s{2,}", ": ", retired[0].strip())
    cell(d, 1000, 650, 500, 80, readable, ROSE, 34)
    fit_mono(d, 1000, 770, re.sub(r"\s{2,}", ": ", retired[1].strip()), ROSE, 26, 860)
    caption(d, "No live record needs key 1, but the backup does: migrate it or keep the key.", AMBER, 900)


def tamper_checks(img, d, cfg):
    """Three changed headers and why each one is refused."""
    stored = sec("FIVE", "THE RECORD, AS STORED")[0]
    good = re.search(r"header ([0-9a-f ]+),", stored).group(1)
    header(d, 60, 210, "THE RECORD, AS STORED", CYAN)
    header_bytes(d, 60, 275, good, (ACCENT, GREEN, GREEN))
    label(d, 400, 290, stored.strip().split(", ", 1)[1], GREEN, 28, True)
    policy = [ln.strip() for ln in sec("FIVE", "THE POLICY")]
    header(d, 1100, 210, "THE POLICY", AMBER)
    for i, p in enumerate(policy):
        label(d, 1100, 280 + i * 44, p, TEXT, 26, True)
    changes = [re.match(r"(\d)\s+header ([0-9a-f ]+), (.*)", ln.strip()).groups() for ln in sec("FIVE", "THREE CHANGES")]
    results = [re.match(r"(\d)\s+(.*)", ln.strip()).group(2) for ln in sec("FIVE", "THE RESULT")]
    header(d, 60, 410, "BEFORE THE READ: THREE CHANGES", ROSE)
    header(d, 1000, 410, "THE RESULT", GREEN)
    for i, ((n, hexes, what), res) in enumerate(zip(changes, results)):
        y = 480 + i * 140
        cols = [ROSE if a != b else MUTED for a, b in zip(hexes.split(), good.split())]
        if hexes.split()[2] != good.split()[2]:
            cols = [MUTED, AMBER, AMBER]
        header_bytes(d, 60, y, hexes, cols, 30, 86)
        label(d, 360, y + 14, what, TEXT, 26, True)
        cell(d, 1000, y, 860, 60, res, GREEN, 26)
    caption(d, "The tag covers the header, and the policy says which versions are still read.", AMBER, 920)
