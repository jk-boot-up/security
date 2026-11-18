"""Pictures for the SBOMs with syft and grype video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The lines of the first section of an act whose header starts with `head` (two spaces removed)."""
    found, cur = None, None
    for ln in act(name):
        if not ln.startswith("  "):
            if found is not None:
                break
            if ln.startswith(head):
                found = []
        elif found is not None:
            found.append(ln[2:])
    return found


def facts(lines):
    """`key   value` lines as (key, value); a continuation line (blank key column) gives ("", value)."""
    out = []
    for ln in lines:
        parts = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        if ln.startswith(" "):
            out.append(("", ln.strip()))
        elif len(parts) == 2:
            out.append(tuple(parts))
    return out


def value(name, head, key, n=0):
    return [v for k, v in facts(section(name, head)) if k == key][n]


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


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def hash_of(text):
    return re.search(r"sha256 ([0-9a-f]+)", text).group(1)


# ------------------------------------------------------------------ figures

def purl_parts(img, d, cfg):
    """The core idea: one package URL split into its parts, and the jar's fingerprint checked."""
    sec = section("ONE", "ONE PACKAGE URL")
    purl = sec[0].strip()
    parts = facts(sec[1:])
    header(d, 60, 180, "ONE PACKAGE URL, PART BY PART", CYAN)
    colours = {"type": CYAN, "namespace": AMBER, "name": GREEN, "version": ROSE}
    f = font(MONO_B, 44)
    runs, pos = [], 0
    for k, v in parts:
        i = purl.find(v, pos)
        runs += [(purl[pos:i], MUTED), (v, colours[k])]
        pos = i + len(v)
    runs.append((purl[pos:], MUTED))
    x = 60
    for text, c in runs:
        d.text((x, 260), text, font=f, fill=c)
        x += f.getlength(text)
    notes = {"type": "the ecosystem", "namespace": "the Maven group", "name": "the library", "version": "the release"}
    for j, (k, v) in enumerate(parts):
        x = 60 + j * 455
        label(d, x + 210, 350, k, colours[k], 28, True, "ma")
        cell(d, x, 395, 420, 64, v, colours[k], 28)
        label(d, x + 210, 470, notes[k], MUTED, 24, False, "ma")
    fp = [r for r in facts(section("ONE", "ITS FILE'S FINGERPRINT")) if r[0] != "licence"]
    header(d, 60, 540, "ITS FILE'S FINGERPRINT, CHECKED", AMBER)
    for i, (k, v) in enumerate(fp):
        y = 600 + i * 76
        label(d, 80, y + 30, k, MUTED, 28, True, "lm")
        cell(d, 330, y, 1530, 60, v, GREEN if k != "the file" else TEXT, 28)
    caption(d, "A scanner matches advisories on these four parts; the hash pins the exact jar.")


def image_to_bill(img, d, cfg):
    """The image goes in, syft writes the bill; the bill says what is inside, not who built it."""
    image = facts(section("ONE", "THE IMAGE"))
    bill = facts(section("ONE", "THE BILL OF MATERIALS"))
    header(d, 60, 180, "THE IMAGE (the input)", CYAN)
    for i, (k, v) in enumerate(image):
        y = 250 + i * 110
        panel(d, 60, y, 780, 96)
        label(d, 85, y + 14, k, MUTED, 24, True)
        fit_mono(d, 85, y + 52, v, TEXT, 28, 730)
    draw.arrow_right(d, 870, 460, 1010, MUTED, 5)
    label(d, 940, 490, "syft", MUTED, 26, True, "ma")
    title = next(ln for ln in act("ONE") if ln.startswith("THE BILL OF MATERIALS")).rstrip(":")
    header(d, 1040, 180, title, GREEN)
    for i, (k, v) in enumerate(bill):
        y = 250 + i * 110
        panel(d, 1040, y, 820, 96, GREEN if k == "libraries" else LINE)
        label(d, 1065, y + 14, k, MUTED, 24, True)
        fit_mono(d, 1065, y + 52, v, TEXT, 28, 770)
    missing = facts(section("ONE", "WHAT THE BILL DOES NOT SAY"))[0]
    header(d, 1040, 600, "WHAT THE BILL DOES NOT SAY", ROSE)
    label(d, 1060, 670, missing[0], TEXT, 30, True)
    label(d, 1060, 715, missing[1].split("<-")[0].strip(), ROSE, 30, True)
    caption(d, "The bill lists what is inside the image. Who built it needs provenance.")


def version_line(img, d, cfg):
    """The installed log4j version on a line with each advisory's fix: every fix lies after it, so all three match."""
    rows = [ln.split() for ln in section("TWO", "THE CRITICAL AND HIGH")]
    adv = [(r[0], r[1], r[-1]) for r in rows]
    installed = facts(section("TWO", "HOW ONE MATCH"))[0][1].split()[-1]
    versions = sorted({installed} | {f for _, _, f in adv}, key=lambda v: [int(n) for n in re.findall(r"\d+", v)])
    header(d, 60, 180, "THE ADVISORIES, AND WHERE EACH ONE IS FIXED", ROSE)
    x0, x1, y = 200, 1720, 560
    d.line([x0 - 60, y, x1 + 60, y], fill=LINE, width=4)
    pos = {v: x0 + i * (x1 - x0) / (len(versions) - 1) for i, v in enumerate(versions)}
    for v, x in pos.items():
        mine = v == installed
        d.ellipse([x - 16, y - 16, x + 16, y + 16], fill=AMBER if mine else GREEN)
        label(d, x, y + 30, v, AMBER if mine else GREEN, 32, True, "ma")
    label(d, pos[installed], y + 80, "installed", AMBER, 26, True, "ma")
    for i, (sev, cve, fix) in enumerate(adv):
        c = ROSE if sev == "CRITICAL" else AMBER
        top = 260 + i * 80
        d.rectangle([pos[installed], top + 14, pos[fix], top + 50], fill=(60, 30, 40))
        d.line([pos[fix], top + 50, pos[fix], y - 18], fill=c, width=2)
        label(d, pos[installed] + 12, top + 32, "%s  %s" % (sev, cve), c, 26, True, "lm")
        label(d, pos[fix] + 12, top + 32, "fixed in " + fix, c, 24, False, "lm")
    how = dict(facts(section("TWO", "HOW ONE MATCH")))
    fit_mono(d, 60, 760, "compared: " + how["compared"], TEXT, 30, 1800)
    same = dict(facts(section("TWO", "THE SAME SCAN")))
    fit_mono(d, 60, 820, "scanning the bill alone: " + same["log4j"], TEXT, 28, 1800)
    caption(d, "An advisory matches while the installed version is below its fix.")


def bill_diff(img, d, cfg):
    """The two bills compared: what the upgrade removed and added, and the scans before and after."""
    up = dict(facts(section("THREE", "THE UPGRADE")))
    cmp_ = dict(facts(section("THREE", "THE TWO BILLS")))
    after = facts(section("THREE", "THE NEW SCAN"))[0]
    header(d, 60, 180, "THE UPGRADE (the input)", CYAN)
    label(d, 80, 260, "fixes needed: " + up["fixes needed"], TEXT, 30, True)
    label(d, 80, 310, "so at least " + up["so at least"] + ": the team builds " + up["new image"], TEXT, 30, True)
    header(d, 60, 390, "THE TWO BILLS COMPARED (the result)", AMBER)
    for i, (k, c) in enumerate((("removed", ROSE), ("added", GREEN))):
        y = 460 + i * 110
        label(d, 80, y + 35, k, c, 30, True, "lm")
        for j, name in enumerate(cmp_[k].split(", ")):
            cell(d, 300 + j * 560, y, 530, 70, name, c, 30)
    label(d, 80, 700, "OS packages: " + cmp_["OS packages"], MUTED, 28, True)
    nums = re.findall(r"(before|after) (\d+)", after[1])
    for i, (k, n) in enumerate(nums):
        c = ROSE if n != "0" else GREEN
        x = 500 + i * 600
        cell(d, x, 770, 420, 110, "%s: %s" % (k, n), c, 40)
    label(d, 80, 815, "critical and high", MUTED, 26, True)
    label(d, 80, 850, "in log4j", MUTED, 26, True)
    caption(d, "Two bills, compared: only the libraries that changed differ, and the scan is clean.", AMBER, 945)


def gate_answers(img, d, cfg):
    """Six requests to deploy, and the gate's answer to each."""
    exc = dict(facts(section("FOUR", "THE EXCEPTION")))
    header(d, 60, 180, "THE EXCEPTION (written down)", AMBER)
    label(d, 80, 250, exc["advisory"] + ", ends " + exc["ends"], TEXT, 30, True)
    label(d, 80, 295, exc["reason"], MUTED, 26)
    header(d, 60, 360, "SIX REQUESTS TO DEPLOY, AND THE ANSWERS", CYAN)
    for i, ln in enumerate(section("FOUR", "SIX REQUESTS")):
        req, ans = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        y = 430 + i * 78
        c = GREEN if ans.startswith("deployed") else ROSE
        fit_mono(d, 80, y + 32, req, TEXT, 30, 760, "lm")
        cell(d, 880, y, 980, 64, ans, c, 30)
    caption(d, "A critical advisory is never excepted; an exception for a high one runs out.", AMBER, 945)


def blind_spots(img, d, cfg):
    """What a scan cannot see: a renamed jar, a stale bill, and what it cannot know."""
    seen = facts(section("FIVE", "WHAT THE TOOLS SEE"))
    stale = dict(facts(section("FIVE", "A STALE BILL")))
    cannot = [ln.strip().split(":")[0] for ln in section("FIVE", "WHAT A SCAN CANNOT DO")]
    cards = [
        ("A RENAMED JAR", ["log4j-core as blob.dat"] + ["%s: %s" % (k, v.split()[0]) for k, v in seen], "IT SEES ONLY WHAT SYFT RECOGNISES"),
        ("A STALE BILL", ["for " + stale["written for"], "offered " + stale["offered for"], "describes it: " + stale["describes it"].split()[0]],
         stale["describes it"].split("<- ")[1]),
        ("WHAT A SCAN CANNOT DO", cannot, "RESCAN, TRIAGE, AND CHECK PROVENANCE"),
    ]
    for i, (title, lines, lesson) in enumerate(cards):
        x = 60 + i * 610
        panel(d, x, 180, 580, 700, ROSE, 3)
        header(d, x + 20, 200, title, ROSE, 540)
        for k, ln in enumerate(lines):
            fit_mono(d, x + 30, 300 + k * 75, ln, TEXT, 28, 520)
        words = lesson.split()
        half = (len(words) + 1) // 2
        for k, part in enumerate((" ".join(words[:half]), " ".join(words[half:]))):
            label(d, x + 290, 740 + k * 50, part, AMBER, 30, True, "ma")
    caption(d, "A scan sees only what was catalogued, on the day it ran.", AMBER, 935)
