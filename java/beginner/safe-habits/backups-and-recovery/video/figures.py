"""Pictures for the backups and recovery video, drawn by the `figure` slide kind.

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


def _stored():
    """{day: [file stems stored by that backup]} from THE BACKUPS."""
    out = {}
    for ln in part("ONE", "THE BACKUPS"):
        m = re.match(r"(day \d)\s+(\S+)\s+(.*)$", ln.strip())
        if m:
            out[m.group(1)] = (m.group(2), m.group(3).split())
    return out


def _restore():
    rows = {}
    for ln in part("ONE", "THE RESTORE OF DAY 3"):
        m = re.match(r"(\S+\.csv)\s+from (day \d)", ln.strip())
        if m:
            rows[m.group(1)] = m.group(2)
    return rows


def _tests():
    rows = part("TWO", "THE RESTORE TEST RESULTS")
    return [ln.split() for ln in rows[1:] if not ln.startswith("<-")]


def backup_life(img, d, cfg):
    """One file, orders.csv, from the shop to a tested restore, in four steps."""
    manifest = dict(reversed(ln.split()) for ln in part("ONE", "THE MANIFEST OF DAY 3"))
    restored = _restore()
    after = part("THREE", "AFTER THE FIX")
    score = re.search(r"(\d of 4)", after[-1]).group(1)
    good = [t for t in _tests() if t[0] == "good"][0]
    steps = [
        ("1  COPY", CLIENT, ["orders.csv changed on day 2", "so the day 2 backup", "stores a copy of it"]),
        ("2  FINGERPRINT", CYAN, ["the manifest records", "its SHA-256:", manifest["orders.csv"] + "..."]),
        ("3  KEEP ELSEWHERE", AMBER, ["%d places, one off site" % (len(after) - 2), "and locked", "3-2-1 score: " + score]),
        ("4  RESTORE AND TEST", GREEN, ["restored from " + restored["orders.csv"], "%s orders, usable" % good[3],
                                        "test passed: " + good[4]]),
    ]
    for i, (head, c, lines) in enumerate(steps):
        x = 70 + i * 450
        header(d, x, 230, head, c, 400)
        panel(d, x, 310, 400, 380, c, 3)
        for k, ln in enumerate(lines):
            if ln.endswith("..."):
                mono(d, x + 200, 400 + k * 90, ln, c, 26, "ma")
            else:
                fit(d, x + 200, 400 + k * 90, ln, TEXT, 30, 370, True, "ma")
        if i < 3:
            draw.arrow_right(d, x + 405, 500, x + 445, MUTED, 4)
    label(d, 960, 740, "A backup counts only when it is complete, kept apart from the data,", TEXT, 32, False, "ma")
    label(d, 960, 790, "and a restore has proved it works.", TEXT, 32, False, "ma")
    caption(d, "The five demos take these steps one at a time.", AMBER, 890)


def chain(img, d, cfg):
    """Days by files: which backup stored each file, and where the restore takes it from."""
    stored = _stored()
    restored = _restore()
    files = list(restored)
    days = list(stored)
    header(d, 100, 205, "THE BACKUPS", CYAN)
    header(d, 1380, 205, "THE RESTORE OF DAY 3", GREEN)
    x0, y0, cw, ch = 420, 300, 300, 110
    for j, day in enumerate(days):
        kind = stored[day][0]
        label(d, x0 + j * cw + cw // 2, y0 - 10, day, TEXT, 30, True, "ms")
        label(d, x0 + j * cw + cw // 2, y0 + 30, kind, CLIENT if kind == "full" else AMBER, 24, True, "ms")
    for i, f in enumerate(files):
        y = y0 + 60 + i * ch
        mono(d, 100, y + 40, f, TEXT, 30, "lm")
        for j, day in enumerate(days):
            here = f[:-4] in stored[day][1]
            used = restored[f] == day
            c = GREEN if used else (CLIENT if here else LINE)
            cell(d, x0 + j * cw + 20, y, cw - 40, 80, "stored" if here else "-", c, 28,
                 fill=BG_CELL if here else DIM, width=5 if used else 2)
        label(d, 1400, y + 40, "from " + restored[f], GREEN, 30, True, "lm")
    label(d, 960, 840, "Green: the newest copy of each file in the chain. Day 3 alone holds one file.", TEXT, 28, False, "ma")
    caption(d, "Incremental backups are small; a restore needs the whole chain.", AMBER, 900)


def trust(img, d, cfg):
    """The manifest finds damage; only the restore test finds a backup that never held the file."""
    damage = part("TWO", "THE DAMAGE")
    finds = part("TWO", "WHAT THE MANIFEST")
    header(d, 100, 205, "THE DAMAGE", ROSE)
    header(d, 1000, 205, "WHAT THE MANIFEST CHECK FINDS", CYAN)
    for k, ln in enumerate(damage):
        panel(d, 100, 275 + k * 90, 820, 70, ROSE)
        label(d, 125, 310 + k * 90, ln.strip(), TEXT, 28, True, "lm")
        draw.arrow_right(d, 925, 310 + k * 90, 995, MUTED, 4)
        cell(d, 1000, 275 + k * 90, 640, 70, finds[k].strip(), GREEN, 30)
    header(d, 100, 490, "THE RESTORE TEST RESULTS", AMBER)
    heads = re.split(r"\s{2,}", part("TWO", "THE RESTORE TEST RESULTS")[0].strip())
    xs = [100, 410, 720, 1030, 1340]
    for x, h in zip(xs, heads):
        label(d, x + 20, 580, h, MUTED, 26, True, "lm")
    for r, row in enumerate(_tests()):
        y = 620 + r * 100
        ok = row[4] == "true"
        for k, (x, v) in enumerate(zip(xs, row)):
            c = GREEN if v in ("ok", "usable", "true") else ROSE if v in ("missing", "false", "0") else TEXT
            cell(d, x, y, 280, 76, v, c, 30)
    caption(d, "The partial backup passes the manifest check and fails the restore test.", AMBER, 880)


def rule_and_retention(img, d, cfg):
    """The 3-2-1 check before and after, and which of 400 snapshots retention keeps."""
    for k, (head, c) in enumerate([("BEFORE THE FIX", ROSE), ("AFTER THE FIX", GREEN)]):
        rows = part("THREE", head)
        x = 100 + k * 880
        header(d, x, 205, head, c)
        for i, ln in enumerate(rows[:-2]):
            place, rest = re.match(r"(.+?)\s{2,}(.+)$", ln.strip()).groups()
            cell(d, x, 265 + i * 66, 360, 56, place, CLIENT, 22, False)
            label(d, x + 380, 293 + i * 66, rest, TEXT, 24, True, "lm")
        checks = re.findall(r"(\d [a-z ]+?) (true|false)", " ".join(rows[-2:]))
        for j, (name, val) in enumerate(checks):
            cell(d, x + j * 165, 480, 155, 56, name, GREEN if val == "true" else ROSE, 22, False)
        label(d, x + 820, 508, re.search(r"\d of 4", rows[-1]).group(0), c, 34, True, "rm")
    snaps = part("THREE", "THE SNAPSHOTS")[0]
    a, b = re.findall(r"\d{4}-\d\d-\d\d", snaps)
    import datetime as dt
    start, end = dt.date.fromisoformat(a), dt.date.fromisoformat(b)
    total = (end - start).days + 1
    header(d, 100, 580, "THE SNAPSHOTS KEPT, OUT OF %d" % total, CYAN)
    x0, x1, y = 120, 1800, 720
    xd = lambda day: x0 + (x1 - x0) * (day - start).days / (total - 1)
    d.line([(x0, y), (x1, y)], fill=LINE, width=4)
    kept = part("THREE", "WHAT IS KEPT")
    colours = {"daily": GREEN, "weekly": AMBER, "monthly": CYAN}
    for r, ln in enumerate(kept[:3]):
        kind, n, s, e = re.match(r"(\w+)\s+(\d+)\s+(\S+) to (\S+)", ln.strip()).groups()
        s, e = dt.date.fromisoformat(s), dt.date.fromisoformat(e)
        step = {"daily": 1, "weekly": 7}.get(kind)
        days = []
        cur = s
        while cur <= e:
            days.append(cur)
            cur = cur + dt.timedelta(days=step) if step else (cur.replace(day=28) + dt.timedelta(days=4)).replace(day=1)
        c = colours[kind]
        for day in days:
            d.line([(xd(day), y - 50 + r * 12), (xd(day), y + 10 + r * 12)], fill=c, width=4)
        label(d, 120 + r * 420, 790, "%s %s" % (n, kind), c, 30, True, "la")
    label(d, x0, y + 20, a, MUTED, 22, False, "la")
    label(d, x1, y + 20, b, MUTED, 22, False, "ra")
    label(d, 1800, 790, kept[3].strip().split("= ")[1], TEXT, 30, True, "ra")
    caption(d, "Three copies, two media, one off site, one locked; a year of history in %s snapshots."
            % re.search(r"= (\d+)", kept[3]).group(1), AMBER, 880)


def ransomware(img, d, cfg):
    """Four copies before and after the attack, and what each one gives back."""
    before = part("FOUR", "THE COPIES BEFORE")
    after = part("FOUR", "AFTER THE ATTACK")
    rec = part("FOUR", "WHAT CAN BE RESTORED")
    header(d, 100, 205, "BEFORE THE ATTACK", CYAN)
    header(d, 1000, 205, "AFTER THE ATTACK", ROSE)
    for i, (b, a) in enumerate(zip(before, after)):
        name, rest = re.match(r"(.+?)\s{2,}(.+)$", b.strip()).groups()
        res = re.match(r".+?\s{2,}(.+)$", a.strip()).group(1).replace("  <- WENT WITH THE DATA", "")
        y = 275 + i * 110
        bad = "encrypted" in res and not res.startswith("0")
        if "versions" in res:
            bad = False
        c = ROSE if bad else GREEN
        cell(d, 100, y, 330, 86, name, CLIENT, 28, False)
        label(d, 450, y + 43, rest.replace("  ", ", "), TEXT, 26, False, "lm")
        draw.arrow_right(d, 900, y + 43, 990, MUTED, 4)
        cell(d, 1000, y, 820, 86, res, c, 26)
    header(d, 100, 735, "THE RECOVERY", AMBER)
    label(d, 100, 810, rec[0].strip(), GREEN, 28, True, "la")
    label(d, 100, 855, rec[1].strip() + " " + rec[2].strip().split("  <-")[0], ROSE, 28, True, "la")
    caption(d, "Only a copy the shop's own account cannot change survives.", AMBER, 920)


def objectives(img, d, cfg):
    """A day on a clock line: the backups, the failure, the restore, and RPO and RTO as spans."""
    tl = part("FIVE", "THE TIMELINE")
    t = [re.match(r"(\d\d):(\d\d)", ln.strip()) for ln in tl[:3]]
    mins = [int(m.group(1)) * 60 + int(m.group(2)) for m in t]
    nightly, fail, seen = mins
    restore = int(re.match(r"(\d+) minutes", tl[3].strip()).group(1))
    back = seen + restore
    hourly_txt = first("FIVE", r"WITH AN HOURLY BACKUP, THE LAST AT (\d\d:\d\d)")
    hourly = int(hourly_txt[:2]) * 60 + int(hourly_txt[3:])
    rpo = part("FIVE", "THE RECOVERY POINT")[0].strip()
    rto = part("FIVE", "THE RECOVERY TIME")[0].strip()
    hrly = part("FIVE", "WITH AN HOURLY")[0].strip()
    x0, x1, y = 120, 1800, 470
    xm = lambda m: x0 + (x1 - x0) * m / (24 * 60)
    header(d, 100, 205, "THE TIMELINE OF ONE DAY", CYAN)
    d.line([(x0, y), (x1, y)], fill=LINE, width=6)
    for h in range(0, 25, 3):
        label(d, xm(h * 60), y + 20, "%02d:00" % h, MUTED, 22, False, "ma")
    def mark(m, text, c, up, anchor=None):
        d.line([(xm(m), y - 20), (xm(m), y + 20)], fill=c, width=6)
        dx = {"ra": -12, "la": 12}.get(anchor, 0)
        label(d, xm(m) + dx, y - 40 if up else y + 60, text, c, 26, True, anchor or ("ms" if up else "ma"))
    mark(nightly, "nightly backup " + tl[0].strip()[:5], CLIENT, True)
    mark(hourly, "hourly " + hourly_txt, GREEN, True)
    mark(fail, "fails " + tl[1].strip()[:5], ROSE, False, "ra")
    mark(back, "back %02d:%02d" % (back // 60, back % 60), GREEN, False, "la")
    d.rounded_rectangle([xm(nightly), 300, xm(fail), 340], radius=10, fill=DIM, outline=ROSE, width=3)
    label(d, (xm(nightly) + xm(fail)) / 2, 320, "RPO: work lost", ROSE, 24, True, "mm")
    d.rounded_rectangle([xm(fail), 590, xm(back), 630], radius=10, fill=DIM, outline=AMBER, width=3)
    label(d, xm(fail) - 15, 610, "RTO: until the shop is back", AMBER, 24, True, "rm")
    header(d, 100, 650, "WORKED OUT", AMBER)
    for k, (name, txt, c) in enumerate([("RPO, nightly", rpo, ROSE), ("RTO", rto, AMBER), ("RPO, hourly", hrly[4:], GREEN)]):
        label(d, 100, 740 + k * 60, name, c, 28, True, "lm")
        mono(d, 400, 740 + k * 60, txt, TEXT, 28, "lm")
    caption(d, "More frequent backups shrink the RPO; only a faster restore shrinks the RTO.", AMBER, 930)


def sealed(img, d, cfg):
    """The archive sealed with AES-256-GCM, and what each attempt to open it gets."""
    inp = part("FIVE", "THE ARCHIVE TO SEAL")
    res = part("FIVE", "THE SEALED ARCHIVE")
    header(d, 100, 205, "THE INPUT", CYAN)
    for k, ln in enumerate(inp):
        label(d, 100, 290 + k * 60, ln.strip(), TEXT, 30, True, "lm")
    cell(d, 1180, 260, 300, 120, "AES-256-GCM", AMBER, 30)
    draw.arrow_right(d, 1000, 320, 1170, MUTED, 4)
    draw.arrow_right(d, 1490, 320, 1620, MUTED, 4)
    cell(d, 1630, 260, 200, 120, "archive", CLIENT, 30, False)
    header(d, 100, 480, "THE RESULTS", GREEN)
    for k, ln in enumerate(res[:-1]):
        what, val = re.match(r"(.+?)\s{2,}(.+)$", ln.strip()).groups()
        y = 550 + k * 66
        good = (what.startswith("shows") and val == "false") or what.startswith("the right") or what.startswith("the key")
        c = GREEN if good else ROSE
        if "refused" in val:
            c = GREEN
        panel(d, 100, y, 640, 56, CLIENT)
        label(d, 125, y + 28, what, TEXT, 26, True, "lm")
        draw.arrow_right(d, 750, y + 28, 830, MUTED, 4)
        cell(d, 840, y, 700, 56, val, c, 26)
    caption(d, res[-1].strip()[3:].capitalize().replace("backup:", "backup:"), AMBER, 900)
