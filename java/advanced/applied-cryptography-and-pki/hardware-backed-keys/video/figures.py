"""Pictures for the hardware-backed keys video, drawn by the `figure` slide kind.

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


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def sec(name, head, n=0):
    """The n-th section of an act whose header starts with `head`, as a list of lines (two spaces removed)."""
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


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def pairs(lines):
    """`key   value` lines (two or more spaces between) as a list of (key, value)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip())]


def numbered(lines):
    """`1  text` lines as {number: text}; a line without a number continues the one before."""
    out, last = {}, None
    for ln in lines:
        m = re.match(r"\s*(\d)\s+(.*)", ln)
        if m:
            last = m.group(1)
            out[last] = m.group(2)
        elif last:
            out[last] += "; " + ln.strip()
    return out


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


def good_bad(text):
    return ROSE if re.search(r"\byes\b|signed|CKR_OK|^\d+ of \d+ signed", text) else GREEN


# ------------------------------------------------------------------ figures

def file_in_signature_out(img, d, cfg):
    """One payment file goes into the token, a signature comes out, and the key stays inside."""
    head = heading_of("TWO", "ONE PAYMENT FILE")
    size = re.search(r"\((\d+) bytes\)", head).group(1)
    lines = [ln.strip() for ln in sec("TWO", "ONE PAYMENT FILE")]
    after = dict(pairs(sec("TWO", "AFTER SIGNING")))
    alg = re.search(r", (\S+)\)", heading_of("TWO", "AFTER SIGNING")).group(1)
    header(d, 60, 215, "BEFORE: ONE PAYMENT FILE (%s bytes)" % size, CYAN)
    panel(d, 60, 280, 620, 70 + 46 * len(lines), CYAN)
    for i, ln in enumerate(lines):
        fit_mono(d, 90, 305 + i * 46, ln, TEXT, 28, 560)
    draw.arrow_right(d, 700, 470, 800, MUTED, 5)
    panel(d, 820, 280, 420, 380, AMBER, 4)
    label(d, 1030, 300, "THE TOKEN", AMBER, 32, True, "ma")
    cell(d, 870, 380, 320, 70, "private key", AMBER, 30)
    label(d, 1030, 470, "RSA 2048", TEXT, 28, True, "ma")
    label(d, 1030, 515, "never leaves", ROSE, 28, True, "ma")
    label(d, 1030, 590, alg, MUTED, 24, True, "ma")
    draw.arrow_right(d, 1260, 470, 1360, MUTED, 5)
    header(d, 1380, 215, "AFTER: A SIGNATURE", GREEN)
    cell(d, 1380, 300, 480, 80, after["signature size"].split(" = ")[0], GREEN, 34)
    label(d, 1380, 400, "= " + after["signature size"].split(" = ")[1], MUTED, 28, True)
    label(d, 1380, 480, "verified, exported key:", TEXT, 26, True)
    label(d, 1860, 480, after["verified with the exported public key"], GREEN, 26, True, "ra")
    label(d, 1380, 530, "one bit changed:", TEXT, 26, True)
    label(d, 1860, 530, after["the same signatures, one bit changed"] + " verify", ROSE, 26, True, "ra")
    caption(d, "The file goes in, the signature comes out, and the key stays in the token.", AMBER, 900)


def key_attributes(img, d, cfg):
    """What the bank asks the token for, and the attributes the token reports back."""
    token = pairs(sec("ONE", "THE TOKEN"))
    asked = pairs(sec("ONE", "WHAT THE BANK ASKS"))
    made = pairs(sec("ONE", "THE KEY THE TOKEN MADE"))
    header(d, 60, 215, "THE TOKEN", CYAN)
    for i, (k, v) in enumerate(token):
        label(d, 60, 285 + i * 44, k, MUTED, 26, True)
        fit_mono(d, 240, 285 + i * 44, v, TEXT, 26, 700)
    header(d, 60, 440, "BEFORE: WHAT THE BANK ASKS FOR", AMBER)
    for i, (k, v) in enumerate(asked):
        y = 510 + i * 80
        fit_mono(d, 60, y + 18, k, TEXT, 28, 330)
        cell(d, 400, y, 420, 62, v, AMBER, 28)
    header(d, 1000, 215, "AFTER: THE KEY THE TOKEN MADE", GREEN)
    for i, (k, v) in enumerate(made):
        y = 285 + i * 80
        if k.startswith("CKA_"):
            fit_mono(d, 1000, y + 18, k, TEXT, 28, 420)
            want = v == ("no" if "EXTRACTABLE" in k and "NEVER" not in k else "yes")
            cell(d, 1450, y, 200, 62, v, GREEN if want else ROSE, 30)
        else:
            fit_mono(d, 1000, y + 18, "%s: %s" % (k, v), GREEN, 22, 860)
    caption(d, "Sensitive and never extractable: the token will not hand out the key.", AMBER, 900)


def five_ways(img, d, cfg):
    """Five attempts to get the private key out, and the token's answer to each."""
    tries = numbered(sec("TWO", "FIVE WAYS"))
    answers = sec("TWO", "THE TOKEN'S ANSWERS")
    got = dict(pairs([ln for ln in answers if not re.match(r"\s*\d", ln)]))
    ans = numbered([ln for ln in answers if re.match(r"\s*\d", ln)])
    header(d, 60, 215, "BEFORE: FIVE WAYS TO GET THE KEY OUT", ROSE)
    header(d, 1180, 215, "THE TOKEN'S ANSWERS", GREEN)
    for i, n in enumerate(sorted(tries)):
        y = 290 + i * 110
        cell(d, 60, y, 70, 70, n, ROSE, 32)
        fit_mono(d, 150, y + 35, tries[n], TEXT, 26, 950, "lm")
        draw.arrow_right(d, 1080, y + 35, 1160, MUTED, 4)
        cell(d, 1180, y, 680, 70, ans[n], GREEN, 26)
    label(d, W // 2, 850, "private key obtained: " + got["private key obtained"], GREEN, 36, True, "ma")
    caption(d, "Every way out is refused, by the token or by Java.", AMBER, 920)


def pin_and_copies(img, d, cfg):
    """The token never locks, the bank's own lockout, a key in a file, and a copied token."""
    wrong = dict(pairs(sec("THREE", "12 WRONG PINS")))
    guard = dict(pairs(sec("THREE", "THE BANK'S OWN LOCKOUT")))
    header(d, 60, 215, "12 WRONG PINS, THE TOKEN", CYAN)
    label(d, 60, 280, "refused " + wrong["refused"], GREEN, 28, True)
    label(d, 60, 325, "locked: " + wrong["token locked"], ROSE, 28, True)
    header(d, 760, 215, "THE BANK'S OWN LOCKOUT (3 tries)", AMBER)
    for i, a in enumerate(guard["answers"].split(", ")):
        cell(d, 760 + i * 270, 280, 250, 60, a, ROSE if a == "locked" else AMBER, 28)
    label(d, 760, 360, "token asked " + guard["token asked"], TEXT, 26, True)
    label(d, 760, 400, "PIN straight to the provider: " + guard["PIN straight to the provider"], ROSE, 26, True)
    filekey = dict(pairs(sec("THREE", "THE SAME KIND OF KEY")))
    found, total = map(int, re.search(r"guess (\d+) of (\d+)", filekey["password found"]).groups())
    header(d, 60, 490, "A KEY IN A PKCS12 FILE, COPIED", ROSE)
    for i in range(total):
        cell(d, 60 + i * 95, 560, 85, 60, str(i + 1), ROSE if i + 1 == found else LINE, 28)
    label(d, 60, 640, "password found at guess %d of %d, offline, no limit" % (found, total), ROSE, 26, True)
    copy = sec("THREE", "A COPY OF THE TOKEN")
    header(d, 900, 490, "A COPY OF THE TOKEN DIRECTORY", ROSE)
    for i, ln in enumerate(copy[:3]):
        fit_mono(d, 900, 565 + i * 50, re.sub(r"\s{2,}", ": ", ln.strip()), ROSE if i else TEXT, 26, 960)
    caption(d, "SoftHSM2 never locks, and a file or a copied token can be guessed offline.", AMBER, 900)


def decisions_and_chain(img, d, cfg):
    """The 14 requests and the bank's decision for each, and how the audit log's hash chain is made."""
    lines = sec("FOUR", "THE DECISIONS")
    nums = lines[0].split()[1:]
    answers = lines[1].split()[1:]
    colours = {"S": GREEN, "R": AMBER, "A": ROSE}
    header(d, 60, 215, "THE DECISIONS FOR 14 REQUESTS", GREEN)
    for i, (n, a) in enumerate(zip(nums, answers)):
        x = 60 + i * 128
        label(d, x + 56, 280, n, MUTED, 24, True, "ma")
        cell(d, x, 315, 112, 64, a, colours[a], 30)
    label(d, 60, 400, "S signed   R refused, rate (8 in 60 s)   A refused, needs 2 approvers", MUTED, 26, True)
    fit_mono(d, 60, 450, lines[2].strip(), TEXT, 28, 900)
    header(d, 60, 530, "HOW AN AUDIT ENTRY IS MADE", AMBER)
    rows = [ln.split() for ln in sec("FOUR", "HOW AN AUDIT ENTRY")[1:]]
    fit_mono(d, 60, 595, sec("FOUR", "HOW AN AUDIT ENTRY")[0].strip(), AMBER, 26, 1000)
    for i, r in enumerate(rows):
        x = 60 + i * 610
        panel(d, x, 650, 560, 150, CYAN)
        label(d, x + 20, 665, "%s  %s %s" % (r[0], r[1], r[2]), TEXT, 26, True)
        fit_mono(d, x + 20, 715, "previous " + r[4], MUTED, 24, 520)
        fit_mono(d, x + 20, 755, "hash     " + r[6], GREEN, 24, 520)
        if i < len(rows) - 1:
            draw.arrow_right(d, x + 565, 760, x + 605, MUTED, 4)
    chain = dict(pairs(sec("FOUR", "THE CHAIN CHECKED")))
    label(d, 60, 830, "  ".join("%s: %s" % kv for kv in chain.items()), CYAN, 26, True)
    caption(d, "Each entry carries the hash of the one before: edit or remove one and the chain breaks.", AMBER, 900)


def host_attacks(img, d, cfg):
    """Five things an attacker on the host tries, and what each one gets."""
    tries = numbered(sec("FIVE", "FIVE THINGS"))
    res = numbered(sec("FIVE", "THE RESULT"))
    header(d, 60, 215, "BEFORE: AN ATTACKER ON THE HOST TRIES", ROSE)
    header(d, 1060, 215, "THE RESULT", AMBER)
    for i, n in enumerate(sorted(tries)):
        y = 290 + i * 120
        cell(d, 60, y, 64, 80, n, ROSE, 30)
        fit_mono(d, 140, y + 40, tries[n], TEXT, 24, 880, "lm")
        bad = bool(re.search(r"signed|signs yes", res[n]))
        parts = res[n].split("; ")
        cell(d, 1060, y, 800, 100 if len(parts) > 1 else 80, "", ROSE if bad else GREEN, 22)
        for j, part in enumerate(parts):
            fit_mono(d, 1460, y + (40 if len(parts) == 1 else 28 + j * 44), part, ROSE if bad else GREEN, 24, 770, "mm")
    caption(d, "Red: the token signs for anyone already inside the logged-in process.", AMBER, 900)


def backup_wrap(img, d, cfg):
    """A key made extractable for backup: the wrap, the blob size worked out, and who else can open it."""
    cycle = dict(pairs(sec("SIX", "THE BACKUP CYCLE")))
    header(d, 60, 215, "THE BACKUP CYCLE", CYAN)
    for i, (k, v) in enumerate(cycle.items()):
        y = 285 + i * 75
        label(d, 60, y + 30, k, TEXT, 26, True, "lm")
        cell(d, 330, y, 560, 60, v, GREEN, 22)
    attacker = sec("SIX", "AN ATTACKER WITH")
    n, padded, total = map(int, re.search(r"(\d+) key bytes, padded to (\d+), \+ 8 = (\d+)", attacker[1]).groups())
    header(d, 1000, 215, "HOW BIG THE BLOB IS", AMBER)
    scale = 860 / total
    x = 1000
    for text, width, colour in (("key %d" % n, n, AMBER), ("pad %d" % (padded - n), padded - n, MUTED), ("+8", total - padded, ACCENT)):
        d.rectangle([x, 290, x + width * scale - 3, 350], fill=colour)
        f = font(MONO_B, 22)
        if f.getlength(text) < width * scale - 6:
            d.text((x + width * scale / 2, 320), text, font=f, fill=INK, anchor="mm")
        x += width * scale
    label(d, 1000, 370, "%d bytes, rounded up to %d, plus 8 = %d" % (n, padded, total), TEXT, 26, True)
    header(d, 60, 540, "AN ATTACKER WITH A LOGGED-IN SESSION", ROSE)
    for i, ln in enumerate(attacker):
        fit_mono(d, 60, 610 + i * 46, re.sub(r"\s{2,}", ": ", ln.strip()), ROSE, 24, 860)
    lost = dict(pairs(sec("SIX", "THE WRAPPING KEY LOST")))
    header(d, 1000, 540, "THE WRAPPING KEY LOST", AMBER)
    fit_mono(d, 1000, 610, "restores the key: " + lost["restores the key"], AMBER, 24, 860)
    protects = pairs(sec("SIX", "WHAT THE TOKEN PROTECTS"))
    label(d, 1000, 680, "ways out that worked:", TEXT, 26, True)
    for i, (k, v) in enumerate(protects):
        label(d, 1000, 725 + i * 40, k, MUTED, 24, True)
        label(d, 1860, 725 + i * 40, v, GREEN if v.startswith("0") else ROSE, 24, True, "ra")
    caption(d, "A key that can be wrapped for backup can be wrapped by an attacker too.", AMBER, 900)
