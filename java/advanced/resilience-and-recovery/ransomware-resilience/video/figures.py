"""Pictures for the ransomware resilience video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints from two real
RustFS object stores and the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head, n=0):
    """The n-th section of an act whose header starts with `head`: its header line and its lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = [ln]
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[n]


def rows(name, head, n=0):
    """The labelled rows of a section as {label: value} (label and value are separated by two or more spaces)."""
    out = {}
    for ln in section(name, head, n)[1:]:
        parts = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        if len(parts) == 2:
            out[parts[0]] = parts[1]
    return out


def num(text, k=0):
    return int(re.findall(r"\d+", text)[k])


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


def caption(d, text, colour=AMBER, y=950):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def grid(d, x, y, n, colour, cols=12, size=46, gap=10, fill=None):
    """n small squares, one per backup."""
    for k in range(n):
        cx, cy = x + (k % cols) * (size + gap), y + (k // cols) * (size + gap)
        d.rounded_rectangle([cx, cy, cx + size, cy + size], radius=6, fill=fill or BG_CELL, outline=colour, width=3)


def version(d, x, y, w, text, colour, note="", note_colour=MUTED):
    """One version of an object as a card, with a note on its right."""
    cell(d, x, y, w, 62, text, colour, 26)
    if note:
        label(d, x + w + 20, y + 31, note, note_colour, 26, True, "lm")


# ------------------------------------------------------------------ figures

def one_backup(img, d, cfg):
    """The core idea on one backup, ledger-00.csv: what an overwrite and a delete by id do in each kind of bucket."""
    setup = rows("ONE", "THE SETUP")
    kept = rows("ONE", "AFTER THE OVERWRITE")["versions kept per backup"]
    lock = rows("THREE", "THE ATTACK")
    ex = setup["example"].split()
    label(d, 60, 210, "ONE BACKUP: %s, %s bytes, SHA-256 %s" % (ex[0], ex[1], ex[-1]), TEXT, 30, True)
    xs, w = [60, 680, 1300], 560
    titles = [("PLAIN BUCKET", ROSE), ("VERSIONING ONLY", AMBER), ("OBJECT LOCK, COMPLIANCE", GREEN)]
    for x, (t, c) in zip(xs, titles):
        header(d, x, 270, t, c, w)
        panel(d, x, 330, w, 560, c, 3)
        label(d, x + 24, 350, "BEFORE THE ATTACK", CYAN, 24, True)
        label(d, x + 24, 520, "AFTER OVERWRITE AND DELETE BY ID", ROSE, 22, True)
    # before: one good version in each
    for x in xs:
        version(d, x + 24, 390, 300, "v1  the ledger", GREEN, "SHA-256 ok", GREEN)
    # plain: one version kept, overwritten, then deleted
    version(d, xs[0] + 24, 560, 300, "v1  random bytes", ROSE, "overwritten", ROSE)
    label(d, xs[0] + 24, 650, "versions kept: %s" % kept, MUTED, 26)
    cell(d, xs[0] + 24, 700, 512, 70, "deleted: nothing left", ROSE, 28)
    # versioned: two versions, both deleted by id
    version(d, xs[1] + 24, 560, 300, "v2  random bytes", ROSE, "deleted by id", ROSE)
    version(d, xs[1] + 24, 632, 300, "v1  the ledger", GREEN, "deleted by id", ROSE)
    cell(d, xs[1] + 24, 720, 512, 70, "0 versions left", ROSE, 28)
    # locked: two versions, delete refused
    version(d, xs[2] + 24, 560, 300, "v2  random bytes", ROSE, "locked too", AMBER)
    version(d, xs[2] + 24, 632, 300, "v1  the ledger", GREEN, "AccessDenied", GREEN)
    cell(d, xs[2] + 24, 720, 512, 70, "root: " + lock["root administrator"].split("  ")[0], GREEN, 28)
    label(d, xs[2] + 24, 810, "restore by version: v1 is still there", GREEN, 24, True)
    caption(d, "A version that cannot be deleted before its date is a backup the attacker cannot destroy.", AMBER, 950)


def plain_attack(img, d, cfg):
    """24 backups before the attack, after the overwrite, and after the delete."""
    before = rows("ONE", "BEFORE THE ATTACK")["backups that restore"]
    attack = rows("ONE", "THE ATTACK")["overwrite, random bytes"]
    after = rows("ONE", "AFTER THE OVERWRITE")["backups that restore"]
    gone = rows("ONE", "THE DELETE")
    n = num(before, 1)
    acct = rows("ONE", "THE SETUP")["account"]
    label(d, 60, 210, "the backup account: " + acct, TEXT, 30)
    cols = [(60, "BEFORE THE ATTACK", CYAN), (680, "AFTER THE OVERWRITE", ROSE), (1300, "AFTER THE DELETE", ROSE)]
    for x, t, c in cols:
        header(d, x, 280, t, c, 560)
    grid(d, 70, 360, num(before), GREEN, 8, 58, 10)
    grid(d, 690, 360, n, ROSE, 8, 58, 10)
    label(d, 80, 580, "restore: " + before.split(" (")[0], GREEN, 30, True)
    label(d, 80, 630, "(SHA-256 matches the manifest)", MUTED, 26)
    label(d, 700, 580, "overwrite: " + attack, ROSE, 30, True)
    label(d, 700, 630, "restore: " + after.split("  ")[0], ROSE, 30, True)
    panel(d, 1320, 360, 520, 180, ROSE, 3)
    label(d, 1580, 420, "objects left: " + gone["objects left"], ROSE, 36, True, "ma")
    label(d, 1320, 580, "delete: " + gone["delete every key"], ROSE, 30, True)
    caption(d, "Whoever holds the backup credentials can destroy every backup in a plain bucket.", AMBER, 950)


def versions_stack(img, d, cfg):
    """Versioning: the overwrite adds a version and a delete adds a marker, but a delete by version id is permanent."""
    ow = rows("TWO", "THE OVERWRITE")
    rs = rows("TWO", "THE RESTORE")
    dk = rows("TWO", "THE DELETE BY KEY")
    dv = rows("TWO", "THE DELETE BY VERSION ID")
    header(d, 60, 210, "ONE KEY, ledger-00.csv, STEP BY STEP", CYAN)
    steps = [("1. stored", [("v1  the ledger", GREEN)]),
             ("2. overwritten", [("v2  random bytes", ROSE), ("v1  the ledger", GREEN)]),
             ("3. delete by key", [("delete marker", AMBER), ("v2  random bytes", ROSE), ("v1  the ledger", GREEN)]),
             ("4. delete by id", [])]
    for k, (t, stack) in enumerate(steps):
        x = 60 + k * 450
        label(d, x, 290, t, TEXT, 30, True)
        for j, (text, c) in enumerate(stack):
            cell(d, x, 350 + j * 80, 400, 64, text, c, 26)
        if not stack:
            cell(d, x, 350, 400, 64, "nothing", ROSE, 28)
    header(d, 60, 640, "THE WHOLE BUCKET, 24 KEYS", AMBER)
    facts = [("versions after the overwrite", ow["versions in the bucket"], AMBER),
             ("restore, latest version", rs["take the latest version"], ROSE),
             ("restore, through the versions", rs["look through the versions"].split("  ")[0], GREEN),
             ("delete markers, versions still there", "%s, %s" % (dk["delete markers added"], dk["versions still there"]), AMBER),
             ("delete by version id", "%s, left %s" % (dv["delete every version"], dv["versions left"]), ROSE)]
    for k, (a, b, c) in enumerate(facts):
        label(d, 60, 710 + k * 46, a, MUTED, 28)
        label(d, 760, 710 + k * 46, b, c, 28, True)
    caption(d, "Versions survive an overwrite, but whoever may delete a version id can still erase them all.", AMBER, 950)


def lock_refusals(img, d, cfg):
    """Object lock in COMPLIANCE mode: who is refused, what root cannot loosen, and the gap."""
    atk = rows("THREE", "THE ATTACK")
    loosen = rows("THREE", "ROOT TRIES")
    gap = rows("THREE", "THE GAP")
    restore = rows("THREE", "THE RESTORE")["look through the versions"].split("  ")[0]
    header(d, 60, 210, "DELETE A LOCKED VERSION BY ID", ROSE, 860)
    for k, (who, res) in enumerate(atk.items()):
        y = 280 + k * 84
        cell(d, 60, y, 400, 64, who.lstrip("+ ") if who.startswith("+") else who, CYAN, 24)
        cell(d, 480, y, 440, 64, res, GREEN, 24)
    label(d, 60, 540, "restore by version: " + restore, GREEN, 30, True)
    header(d, 1000, 210, "ROOT TRIES TO LOOSEN THE LOCK", AMBER, 860)
    for k, (what, res) in enumerate(loosen.items()):
        y = 280 + k * 76
        ok = res.startswith("ok")
        cell(d, 1000, y, 440, 60, what, CYAN, 24)
        cell(d, 1460, y, 400, 60, res.split(" (")[0], AMBER if ok else GREEN, 24)
    header(d, 60, 640, "THE GAP: " + section("THREE", "THE GAP")[0].split("THE GAP: ")[1], ROSE)
    for k, (what, res) in enumerate(gap.items()):
        y = 710 + k * 70
        bad = "NOT LOCKED" in res
        cell(d, 60, y, 560, 58, what, CYAN, 24)
        cell(d, 640, y, 560, 58, res.split("  <-")[0], ROSE if bad else GREEN, 24)
    label(d, 1240, 735, "Set the retention on each", AMBER, 28, True)
    label(d, 1240, 780, "object too, not only as", AMBER, 28, True)
    label(d, 1240, 825, "the bucket's default.", AMBER, 28, True)
    caption(d, "COMPLIANCE mode: nobody, not even root, can delete or shorten a lock before its date.", AMBER, 950)


def isolation_map(img, d, cfg):
    """Two accounts with minimal policies, and a second store with its own credentials."""
    tries = rows("FOUR", "THE WRITER TRIES")
    rec = rows("FOUR", "THE RECOVERY ACCOUNT TRIES")
    second = rows("FOUR", "THE SECOND STORE")
    asks = rows("FOUR", "THE ATTACKER ASKS")
    header(d, 60, 210, "THE WRITER: may add and list", CYAN, 560)
    for k, (what, res) in enumerate(list(tries.items())[:-1]):
        y = 280 + k * 70
        cell(d, 60, y, 330, 60, what, CYAN, 26)
        cell(d, 400, y, 220, 60, res, GREEN, 26)
    label(d, 60, 790, "refused " + tries["refused"].split("  ")[0], GREEN, 30, True)
    header(d, 680, 210, "THE RECOVERY ACCOUNT: may read", CYAN, 560)
    for k, (what, res) in enumerate(rec.items()):
        y = 280 + k * 70
        cell(d, 680, y, 310, 60, what, CYAN, 26)
        cell(d, 1000, y, 240, 60, res, AMBER if res == "ok" else GREEN, 26)
    header(d, 1300, 210, "THE SECOND STORE", GREEN, 560)
    panel(d, 1300, 280, 560, 200, GREEN, 3)
    label(d, 1320, 300, "copied: " + second["checked and copied"].split(" (")[0], TEXT, 28, True)
    label(d, 1320, 350, "(each checked against the manifest)", MUTED, 24)
    label(d, 1320, 400, "restorable: " + second["restorable from it"], GREEN, 28, True)
    label(d, 1300, 520, "THE ATTACKER ASKS IT:", ROSE, 26, True)
    for k, (who, res) in enumerate(asks.items()):
        y = 570 + k * 76
        cell(d, 1300, y, 270, 60, who, CYAN, 24)
        cell(d, 1580, y, 280, 60, res.split(" (")[0], GREEN, 24)
    caption(d, "Each account can do one job, and the copy lives where the first store's keys mean nothing.", AMBER, 950)


def entropy_terms(img, d, cfg):
    """The entropy sum worked out on one 4 KiB block, before and after encryption, against the limit."""
    def terms(head):
        lines = section("FIVE", head)[1:]
        top = [re.split(r"\s+", ln.strip()) for ln in lines if "count" in ln]
        total = re.search(r"(\d+) byte values in all, H = ([\d.]+)", lines[-1])
        return top, total.group(1), float(total.group(2))
    formula = section("FIVE", "HOW ENTROPY")[1].strip()
    limit = float(re.search(r"above ([\d.]+)", rows("FIVE", "THE DETECTORS")["file"]).group(1))
    label(d, 60, 210, formula, TEXT, 32, True)
    for k, (head, t, colour) in enumerate([("THE LEDGER TEXT", "BEFORE ENCRYPTION: the ledger text", CYAN),
                                           ("THE SAME BLOCK", "AFTER AES ENCRYPTION: the same block", ROSE)]):
        x = 60 + k * 620
        top, distinct, h = terms(head)
        header(d, x, 280, t, colour, 580, 24)
        label(d, x, 350, "byte    count   p        term", MUTED, 24)
        for j, row in enumerate(top):
            fit_mono(d, x, 395 + j * 50, "%-6s  %4s  %s  %s" % (row[0], row[2], row[4], row[-1]), TEXT, 28, 580)
        label(d, x, 555, "... %s byte values in all" % distinct, MUTED, 26)
        label(d, x, 600, "H = %.2f bits per byte" % h, colour, 34, True)
    # the scale from 0 to 8 bits
    x0, x1, y = 1300, 1840, 760
    header(d, 1300, 280, "THE SCALE, 0 TO 8 BITS", AMBER, 540)
    _, _, h_text = terms("THE LEDGER TEXT")
    _, _, h_aes = terms("THE SAME BLOCK")
    for k, (name, h, c) in enumerate([("text", h_text, CYAN), ("AES", h_aes, ROSE)]):
        yy = 380 + k * 120
        d.rounded_rectangle([x0, yy, x0 + (x1 - x0) * h / 8, yy + 60], radius=8, fill=c)
        label(d, x0 + 10, yy + 30, "%s  %.2f" % (name, h), INK, 28, True, "lm")
    lx = x0 + (x1 - x0) * limit / 8
    d.line([(lx, 350), (lx, 640)], fill=AMBER, width=5)
    label(d, lx, 650, "limit %s" % limit, AMBER, 26, True, "ma")
    table = [ln for ln in section("FIVE", "THE DIRECTORIES")[1:] if re.search(r"\d", ln)]
    header(d, 60, 700, "THE GUARD ON FIVE DIRECTORIES", AMBER)
    for k, ln in enumerate(table):
        name = re.split(r"\s{2,}", ln.strip())[0]
        verdict = ln.split()[-1]
        y = 770 + (k // 3) * 76
        x = 60 + (k % 3) * 610
        cell(d, x, y, 590, 64, "%s: %s" % (name, verdict), GREEN if verdict == "allowed" else ROSE, 26)
    caption(d, "Text uses a few byte values and scores low; ciphertext uses all 256 and scores near 8.", AMBER, 960)


def poisoned_timeline(img, d, cfg):
    """The hourly runs, the infection, the poisoned 10:00 backup and the run that is restored."""
    before = rows("SIX", "BEFORE THE INFECTION")
    inf = section("SIX", "THE INFECTION")[0]
    at = re.search(r"AT (\d\d:\d\d)", inf).group(1)
    backup = rows("SIX", "THE 10:00 BACKUP")
    chk = rows("SIX", "THE POISONED BACKUP")
    rs = rows("SIX", "THE RESTORE")
    first, last = re.findall(r"\d\d:\d\d", before["runs"])[:2]
    hours = list(range(int(first[:2]), int(last[:2]) + 1))
    x0, step, y = 120, 330, 330
    header(d, 60, 210, "THE HOURLY RUNS", CYAN)
    d.line([(x0 - 40, y + 40), (x0 + step * len(hours) + 120, y + 40)], fill=LINE, width=4)
    for k, hr in enumerate(hours):
        cell(d, x0 + k * step, y, 220, 80, "%02d:00 good" % hr, GREEN, 26)
    ix = x0 + (len(hours) - 1) * step + 250
    d.line([(ix, y - 40), (ix, y + 120)], fill=ROSE, width=6)
    label(d, ix, y + 130, at + " ransomware", ROSE, 26, True, "ma")
    cell(d, x0 + len(hours) * step, y - 60, 300, 70, "10:00 unguarded", ROSE, 24)
    cell(d, x0 + len(hours) * step, y + 30, 300, 70, "10:00 guarded", GREEN, 24)
    label(d, 60, 520, "without the guard: " + backup["without guard"], ROSE, 28, True)
    label(d, 60, 565, "with the guard: " + backup["with the guard"], GREEN, 28, True)
    header(d, 60, 630, "THE POISONED BACKUP, CHECKED", ROSE)
    for k, key in enumerate(["SHA-256 vs manifest", "looks like a ledger", "why"]):
        label(d, 60, 700 + k * 48, key, MUTED, 28)
        label(d, 430, 700 + k * 48, chk[key].split("  <-")[0], ROSE if k < 2 else TEXT, 28, True)
    header(d, 1200, 630, "THE RESTORE", GREEN)
    chosen = re.search(r"chose (\S+)", rs["unguarded"]).group(1)
    label(d, 1200, 700, "newest run passing both checks:", MUTED, 28)
    label(d, 1200, 748, chosen, GREEN, 34, True)
    label(d, 1200, 800, rs["result"].replace("identical to the directory at", "identical to"), GREEN, 26, True)
    caption(d, "A checksum proves a backup is intact, not that what was backed up is good.", AMBER, 950)


def rpo_schedule(img, d, cfg):
    """The recovery point worked out, and how it shrinks with the backup schedule."""
    lines = section("SIX", "HOW THE DATA LOST")
    rate = re.search(r"\((\d+) transactions", lines[0]).group(1)
    sched = [re.findall(r"\d+(?::\d+)?", ln) for ln in lines[3:]]
    label(d, 60, 210, lines[1].strip(), TEXT, 32, True)
    label(d, 60, 260, lines[2].strip(), GREEN, 32, True)
    header(d, 60, 330, "TRANSACTIONS LOST BY SCHEDULE (%s an hour)" % rate, AMBER)
    most = max(int(s[2]) for s in sched)
    for k, (every, lastgood, lost) in enumerate(sched):
        y = 410 + k * 120
        label(d, 60, y + 30, "every %s min" % every, TEXT, 30, True, "lm")
        label(d, 330, y + 30, "last good " + lastgood, MUTED, 26, False, "lm")
        wbar = 1100 * int(lost) / most
        d.rounded_rectangle([650, y, 650 + wbar, y + 60], radius=8, fill=ROSE if int(lost) > 3000 else AMBER)
        label(d, 670 + wbar, y + 30, lost, TEXT, 30, True, "lm")
    caption(d, "Data lost runs from the last good backup to the cut-off: back up more often than you can afford to lose.", AMBER, 950)


def rto_bars(img, d, cfg):
    """The recovery time of both restores as stacked bars against the target."""
    def parts(n):
        values = rows("SEVEN", "HOW THE RECOVERY TIME", n).values()
        mins = [float(v.split()[0]) for v in values]
        speed = re.search(r"at (\d+) MiB/s", "\n".join(section("SEVEN", "HOW THE RECOVERY TIME", n))).group(1)
        return mins, speed
    target = int(re.search(r"target of (\d+)", "\n".join(section("SEVEN", "HOW THE RECOVERY TIME")[1:])).group(1))
    steps = rows("SEVEN", "RESTORE 1")["steps"]
    names = ["declare", "build", "steps", "transfer"]
    colours = [MUTED, CYAN, AMBER, ROSE]
    x0, scale = 60, 1500 / 250
    for k, (title, n) in enumerate([("RESTORE 1, FROM THE FIRST STORE", 0), ("RESTORE 2, FROM THE AIR-GAPPED COPY", 1)]):
        mins, speed = parts(n)
        y = 330 + k * 250
        header(d, 60, y - 70, "%s (transfer at %s MiB/s)" % (title, speed), CYAN if k == 0 else GREEN)
        x = x0
        for part, c, m in zip(names, colours, mins[:4]):
            w = m * scale
            d.rectangle([x, y, x + w, y + 110], fill=c, outline=INK, width=3)
            if w > 120:
                label(d, x + w / 2, y + 22, part, INK, 26, True, "ma")
                label(d, x + w / 2, y + 62, "%g min" % m, INK, 26, True, "ma")
            x += w
        label(d, x + 20, y + 55, "= %.1f min" % mins[4], TEXT, 34, True, "lm")
    tx = x0 + target * scale
    d.line([(tx, 230), (tx, 820)], fill=GREEN, width=6)
    label(d, tx - 12, 770, "target %d min" % target, GREEN, 30, True, "ra")
    label(d, 60, 830, "the thin bar: steps = " + steps + ", 3 s each", MUTED, 28)
    caption(d, "The time is arithmetic over what the restore really did, so it is the same on every run.", AMBER, 950)
