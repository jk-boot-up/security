"""Pictures for the backups with RustFS video, drawn by the `figure` slide kind.

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


def _rows(name, head):
    """{what: result} for the two-column rows of a section."""
    out = {}
    for ln in part(name, head):
        m = re.match(r"(.+?)\s{2,}(\S.*)$", ln.strip())
        if m and not ln.strip().startswith("<-"):
            out[m.group(1)] = m.group(2)
    return out


def _colour(result):
    return GREEN if result.startswith("allowed") or result == "true" else ROSE


def three_guards(img, d, cfg):
    """One nightly backup on its way into the bucket, and the guards it passes."""
    policy = _rows("THREE", "THE POLICY")
    answers = _rows("THREE", "THE REQUESTS")
    sums = _rows("FOUR", "THE ANSWERS")
    lock = _rows("TWO", "THE ROOT ACCOUNT TRIES THE COMPLIANCE")
    hist = _rows("ONE", "THE HISTORY")
    cell(d, 80, 300, 300, 120, "backup job", CLIENT, 32, False)
    label(d, 230, 450, "key: " + first("THREE", r"THE POLICY OF (\S+)"), MUTED, 24, False, "ma")
    steps = [
        ("1  ONLY ADD", CLIENT, [policy["Action"], "get: " + answers["get nightly.tar"].split(" (")[0],
                                 "delete: " + answers["delete it"].split(" (")[0]]),
        ("2  CHECKSUM", CYAN, ["SHA-256 sent along", "good: " + sums["checked.tar"],
                               "damaged: " + sums["damaged.tar"].replace("refused ", "")]),
        ("3  VERSIONS", AMBER, ["an overwrite adds one", "versions of orders.csv: " + hist["versions kept"],
                                "the old ones stay"]),
        ("4  LOCK", GREEN, ["COMPLIANCE until a date", "root deletes: " + lock["delete the version"].split(" (")[0],
                            "version reads: " + lock["the version still reads"]]),
    ]
    for i, (head, c, lines) in enumerate(steps):
        x = 440 + i * 370
        header(d, x, 230, head, c, 340)
        panel(d, x, 300, 340, 300, c, 3)
        for k, ln in enumerate(lines):
            fit(d, x + 170, 340 + k * 80, ln, TEXT, 28, 315, True, "ma")
        draw.arrow_right(d, x - 50 if i else 390, 450, x - 5, MUTED, 4)
    label(d, 960, 680, "The bucket keeps every version, refuses damaged uploads,", TEXT, 32, False, "ma")
    label(d, 960, 730, "and nobody, not even root, deletes a locked backup before its date.", TEXT, 32, False, "ma")
    caption(d, "The five demos take these guards one at a time.", AMBER, 860)


def versions(img, d, cfg):
    """The versions of orders.csv after each step, newest on top, and what a normal read returns."""
    writes = part("ONE", "THE WRITES")
    size = [re.search(r"(\d+) bytes", w).group(1) for w in writes]
    read_after_delete = _rows("ONE", "A PLAIN DELETE")["a normal read"]
    stacks = [
        ("THE SHOP WRITES", [("1  orders, %s bytes" % size[0], GREEN)]),
        ("RANSOMWARE WRITES", [("2  scrambled, %s bytes" % size[1], ROSE), ("1  orders", GREEN)]),
        ("THE RESTORE", [("3  copy of 1", GREEN), ("2  scrambled", ROSE), ("1  orders", GREEN)]),
        ("A PLAIN DELETE", [("delete marker", AMBER), ("3  copy of 1", GREEN), ("2  scrambled", ROSE),
                            ("1  orders", GREEN)]),
    ]
    reads = ["orders", "scrambled", "orders", read_after_delete.split(" (")[0] + " " +
             re.search(r"\((\d+)", read_after_delete).group(1)]
    for i, (head, stack) in enumerate(stacks):
        x = 80 + i * 450
        header(d, x, 215, head, CYAN, 410)
        for k, (text, c) in enumerate(stack):
            y = 300 + k * 95
            cell(d, x, y, 410, 80, text, c, 26, fill=BG_CELL if k == 0 else DIM, width=4 if k == 0 else 2)
        y = 300 + len(stack) * 95 + 20
        label(d, x + 205, 720, "a normal read:", MUTED, 26, False, "ma")
        label(d, x + 205, 760, reads[i], ROSE if reads[i] in ("scrambled",) or "refused" in reads[i] else GREEN,
              30, True, "ma")
    caption(d, "A read sees only the top; every version below is still there to restore.", AMBER, 880)


def lock_matrix(img, d, cfg):
    """What the root account may do to a COMPLIANCE and a GOVERNANCE version."""
    comp = _rows("TWO", "THE ROOT ACCOUNT TRIES THE COMPLIANCE")
    gov = _rows("TWO", "THE ROOT ACCOUNT TRIES THE GOVERNANCE")
    header(d, 820, 215, "COMPLIANCE", AMBER, 480)
    header(d, 1340, 215, "GOVERNANCE", CYAN, 480)
    for i, what in enumerate(comp):
        y = 290 + i * 100
        label(d, 100, y + 40, what, TEXT, 30, True, "lm")
        cell(d, 820, y, 480, 80, comp[what], _colour(comp[what]), 26)
        if what in gov:
            cell(d, 1340, y, 480, 80, gov[what], _colour(gov[what]), 26)
        else:
            cell(d, 1340, y, 480, 80, "-", LINE, 26, fill=DIM, width=2)
    caption(d, "COMPLIANCE: nobody, not even root, until the date. GOVERNANCE: a special permission can.",
            AMBER, 830)


def writer(img, d, cfg):
    """The backup job's policy, and each request it makes with the answer."""
    policy = _rows("THREE", "THE POLICY")
    answers = _rows("THREE", "THE REQUESTS")
    header(d, 100, 215, "THE POLICY OF " + first("THREE", r"THE POLICY OF (\S+)"), CYAN)
    panel(d, 100, 285, 700, 260, CYAN)
    for k, (f, v) in enumerate(policy.items()):
        label(d, 130, 330 + k * 75, f, MUTED, 28, True, "lm")
        mono(d, 320, 330 + k * 75, v, AMBER if f == "Action" else TEXT, 26, "lm")
    label(d, 450, 590, "one action, on one bucket, nothing else", AMBER, 28, True, "ma")
    header(d, 900, 215, "THE REQUESTS AND THE ANSWERS", GREEN)
    for i, (what, res) in enumerate(answers.items()):
        y = 285 + i * 92
        panel(d, 900, y, 440, 74, CLIENT)
        mono(d, 925, y + 37, what, TEXT, 26, "lm")
        draw.arrow_right(d, 1345, y + 37, 1405, MUTED, 4)
        cell(d, 1410, y, 420, 74, res, _colour(res), 24)
    caption(d, "A stolen backup key can add data; it cannot read, list or destroy.", AMBER, 880)


def checksum(img, d, cfg):
    """The checksum sent with each upload against the bytes that arrive, and the answers."""
    sums = _rows("FOUR", "THE CHECKSUMS")
    ups = _rows("FOUR", "THE UPLOADS")
    answers = _rows("FOUR", "THE ANSWERS")
    stored = _rows("FOUR", "WHAT IS STORED")
    header(d, 100, 215, "THE CHECKSUMS (SHA-256 IN BASE64)", CYAN)
    for k, (what, h) in enumerate(sums.items()):
        label(d, 100, 300 + k * 60, what, TEXT, 28, True, "lm")
        mono(d, 360, 300 + k * 60, h, GREEN if k == 0 else ROSE, 28, "lm")
    for i, (name, desc) in enumerate(ups.items()):
        y = 450 + i * 190
        ok = answers[name].startswith("allowed")
        header(d, 100, y, name, GREEN if ok else ROSE)
        label(d, 100, y + 80, "bytes sent: the backup", TEXT, 28, False, "lm")
        label(d, 100, y + 125, "checksum claimed: " + ("the backup" if ok else "other bytes"), TEXT, 28, False, "lm")
        draw.arrow_right(d, 720, y + 100, 820, MUTED, 4)
        cell(d, 830, y + 60, 400, 80, "store recomputes", CYAN, 26, False)
        draw.arrow_right(d, 1240, y + 100, 1320, MUTED, 4)
        cell(d, 1330, y + 60, 490, 80, answers[name], _colour(answers[name]), 28)
    caption(d, "checked.tar same hash: %s; damaged.tar versions stored: %s"
            % (stored["checked.tar, same hash"], stored["damaged.tar, versions"]), AMBER, 880)


def cost(img, d, cfg):
    """The lifecycle rule, and the storage that history costs, worked out."""
    rule = part("FIVE", "THE LIFECYCLE RULE")
    work = part("FIVE", "THE COST")
    days = int(first("FIVE", r"removed (\d+) days"))
    mb = int(first("FIVE", r"of (\d+) MB"))
    kept = int(first("FIVE", r"= (\d+)$"))
    total = int(first("FIVE", r"= (\d+) MB"))
    header(d, 100, 215, "THE LIFECYCLE RULE", CYAN)
    for k, ln in enumerate(rule):
        label(d, 100, 300 + k * 50, ln.strip(), TEXT, 28, True, "lm")
    header(d, 100, 420, "THE COST, WORKED OUT", AMBER)
    for k, ln in enumerate(work):
        mono(d, 100, 500 + k * 50, ln.strip(), TEXT, 28, "lm")
    x0, x1, y = 100, 1820, 700
    d.rectangle([x0, y, x1, y + 60], fill=DIM, outline=LINE, width=2)
    per = (x1 - x0) / days
    for k in range(days):
        d.rectangle([x0 + k * per + 2, y + 2, x0 + (k + 1) * per - 2, y + 58], fill=(70, 60, 30), outline=AMBER)
    label(d, x0, y + 80, "day 1", MUTED, 22, False, "la")
    label(d, x1, y + 80, "day %d: oldest version removed" % days, MUTED, 22, False, "ra")
    label(d, 960, y + 80, "each block: %d versions x %d MB = %d MB" % (kept // days, mb, kept // days * mb),
          AMBER, 26, True, "ma")
    caption(d, "%d MB of history for one %d MB file: the rule keeps it bounded, not free." % (total, mb), AMBER, 900)
