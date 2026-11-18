"""Pictures for the SBOM and provenance video, drawn by the `figure` slide kind.

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

def sbom_entry(img, d, cfg):
    """The core idea on one library: its bytes give a SHA-256, the SBOM records it, a changed jar no longer matches."""
    entry = dict(facts(section("ONE", "HOW ONE ENTRY IS MADE")))
    checks = facts(section("ONE", "CHECKING THE SBOM"))
    name = re.search(r"\((.*)\)", next(ln for ln in act("ONE") if ln.startswith("HOW ONE ENTRY"))).group(1)
    recorded = next(v for k, v in checks if k.endswith("in the SBOM"))
    now = next(v for k, v in checks if k.endswith("on disk now"))
    changed = next(v for k, v in checks if k.startswith("changed"))
    header(d, 60, 180, "BEFORE: THE LIBRARY AT BUILD TIME", CYAN)
    panel(d, 60, 250, 1800, 300)
    label(d, 90, 275, "its name, as a package URL", MUTED, 26, True)
    fit_mono(d, 90, 315, entry["purl"], TEXT, 34, 1740)
    label(d, 90, 375, "its bytes", MUTED, 26, True)
    fit_mono(d, 90, 410, entry["bytes"], TEXT, 32, 900)
    draw.arrow_right(d, 1010, 430, 1130, MUTED, 5)
    label(d, 1070, 455, "SHA-256", MUTED, 24, True, "ma")
    label(d, 1160, 375, "its fingerprint, in the SBOM", MUTED, 26, True)
    fit_mono(d, 1160, 410, entry["sha256"], GREEN, 30, 680)
    label(d, 90, 485, "One changed byte gives a completely different fingerprint.", MUTED, 26)
    header(d, 60, 590, "AFTER: THE SAME LIBRARY, CHECKED LATER", AMBER)
    rows = [("in the SBOM", recorded, GREEN), ("on disk now", now, ROSE)]
    for i, (k, v, c) in enumerate(rows):
        y = 660 + i * 80
        label(d, 90, y + 30, name + " " + k, MUTED, 28, True, "lm")
        cell(d, 560, y, 700, 60, v, c, 28)
    cell(d, 1330, 660, 530, 140, "changed: " + changed, ROSE, 28)
    caption(d, "The SBOM lists every library by name and fingerprint, so a changed jar is found.")


def sbom_answers(img, d, cfg):
    """What the SBOM records, and which questions it answers and which it cannot."""
    sbom = facts(section("ONE", "THE SBOM"))
    header(d, 60, 180, next(ln for ln in act("ONE") if ln.startswith("THE SBOM")).rstrip(":"), CYAN)
    entries = []                                 # (kind, name, hash): a hash is on the same line or the next one
    for i, (k, v) in enumerate(sbom):
        if k:
            text = v if "sha256" in v else sbom[i + 1][1]
            entries.append((k, v.split(",")[0].replace("pkg:maven/com.acme/", ""), hash_of(text)))
    for i, (kind, name, h) in enumerate(entries):
        y = 250 + i * 150
        panel(d, 60, y, 900, 130, GREEN if kind == "application" else LINE)
        label(d, 85, y + 16, kind, MUTED, 24, True)
        fit_mono(d, 85, y + 52, name, TEXT, 32, 820)
        fit_mono(d, 85, y + 92, "sha256 " + h + "...", GREEN, 26, 820)
    header(d, 1040, 180, "WHAT IT ANSWERS", GREEN)
    checks = dict(facts(section("ONE", "CHECKING THE SBOM")))
    missing = facts(section("ONE", "WHAT THE SBOM DOES NOT SAY"))
    answers = [
        ("what is inside the release?", "yes", GREEN),
        ("is it the SBOM for this jar?", checks["application hash = jar?"].split(",")[0], GREEN),
        ("was a library changed?", "found", GREEN),
    ] + [(k + "?", "no", ROSE) for k, _ in missing]
    for i, (q, a, c) in enumerate(answers):
        yy = 250 + i * 100
        if i == 3:
            header(d, 1040, yy - 10, "WHAT IT CANNOT SAY", ROSE)
        yy += 50 if i >= 3 else 0
        label(d, 1040, yy + 30, q, TEXT, 28, False, "lm")
        cell(d, 1700, yy, 160, 60, a, c, 28)
    caption(d, "An SBOM says what is inside. Who built it, and from what, needs provenance.")


def statement_envelope(img, d, cfg):
    """The statement's fields, the bytes that are signed (DSSE PAE), and the envelope that is published."""
    st = facts(section("TWO", "THE STATEMENT"))
    header(d, 60, 180, "THE STATEMENT (what the builder says)", CYAN)
    keep = ["subject", "", "repository", "ref", "gitCommit", "builder.id"]
    rows = [(k, v) for k, v in st if k in keep]
    for i, (k, v) in enumerate(rows):
        y = 250 + i * 56
        label(d, 80, y + 22, k, MUTED, 26, True, "lm")
        fit_mono(d, 300, y + 22, v, GREEN if "sha256" in v else TEXT, 26, 640, "lm")
    pae = dict(facts(section("TWO", "HOW THE SIGNED BYTES")))
    header(d, 1000, 180, "THE SIGNED BYTES (DSSE PAE)", AMBER)
    parts = re.match(r"(DSSEv1) (\d+) (\S+) (\d+) (.*)", pae["signed"]).groups()
    tlen = re.search(r"= (\d+ bytes)", pae["type"]).group(1)
    blen = re.search(r"= (\d+ bytes)", pae["body"]).group(1)
    notes = ["a fixed word", "the type's length: " + tlen, "the type", "the body's length: " + blen, "the body: the statement"]
    for k, (p, n, c) in enumerate(zip(parts, notes, (MUTED, AMBER, CYAN, AMBER, TEXT))):
        y = 245 + k * 52
        cell(d, 1000, y, 470, 44, p, c, 26)
        label(d, 1490, y + 22, n, MUTED, 24, False, "lm")
    draw.arrow(d, 1235, 510, 1235, 552, MUTED, 4)
    label(d, 1260, 515, "signed with Ed25519", MUTED, 24, True)
    env = dict(facts(section("TWO", "THE ENVELOPE")))
    header(d, 1000, 560, "THE ENVELOPE (published)", GREEN)
    for i, k in enumerate(["payloadType", "payload", "keyid", "sig"]):
        y = 625 + i * 58
        label(d, 1020, y + 22, k, MUTED, 26, True, "lm")
        fit_mono(d, 1240, y + 22, env[k], TEXT, 24, 620, "lm")
    other = dict(facts(section("TWO", "THE SAME BYTES")))
    header(d, 60, 620, "THE SAME BYTES AS ANOTHER TYPE", ROSE)
    fit_mono(d, 80, 700, other["signed bytes"], ROSE, 26, 860)
    label(d, 80, 750, "Ed25519 " + other["Ed25519"], ROSE, 26, True)
    label(d, 80, 800, "the gate: " + other["the gate"], ROSE, 24)
    caption(d, "The signature covers the type and the statement, so neither can be swapped.", AMBER, 935)


def gate_matrix(img, d, cfg):
    """Eight releases against the gate's seven checks: passed, failed, or never reached."""
    checks = re.findall(r"(\d) (\w+)", " ".join(section("THREE", "THE CHECKS")))
    offers = {ln.split()[0]: ln.split(None, 1)[1] for ln in section("THREE", "EIGHT RELEASES")}
    answers = {ln.split()[0]: ln.split(None, 1)[1] for ln in section("THREE", "THE GATE'S ANSWERS")}
    x0, cw = 900, 138
    for j, (n, name) in enumerate(checks):
        label(d, x0 + j * cw + cw // 2, 190, n, MUTED, 26, True, "ma")
        label(d, x0 + j * cw + cw // 2, 225, name, MUTED, 20, True, "ma")
    for i, (letter, text) in enumerate(offers.items()):
        y = 270 + i * 80
        ans = answers[letter]
        failed = int(re.search(r"refused at (\d)", ans).group(1)) if ans.startswith("refused") else 0
        colour = GREEN if not failed else ROSE
        label(d, 60, y + 30, letter, colour, 30, True, "lm")
        fit_mono(d, 110, y + 30, text, TEXT, 22, 770, "lm")
        for j in range(len(checks)):
            n = j + 1
            if failed == 0 or n < failed:
                cell(d, x0 + j * cw + 8, y + 6, cw - 16, 50, "pass", GREEN, 22)
            elif n == failed:
                cell(d, x0 + j * cw + 8, y + 6, cw - 16, 50, "FAIL", ROSE, 22)
            else:
                d.rounded_rectangle([x0 + j * cw + 8, y + 6, x0 + (j + 1) * cw - 8, y + 56], radius=10, outline=LINE, width=2)
    caption(d, "Only A passes all seven checks; every other release stops at one.", AMBER, 935)


def levels_and_builds(img, d, cfg):
    """The SLSA build levels of four pipelines, and careless against reproducible builds."""
    levels = section("FOUR", "THEIR SLSA BUILD LEVELS")
    header(d, 60, 180, "SLSA BUILD LEVELS", CYAN)
    for i, ln in enumerate(levels):
        m = re.match(r"(.*?)\s{2,}level (\d)\s*(.*)", ln.strip())
        name, lvl, why = m.groups()
        lvl = int(lvl)
        y = 760 - i * 140
        colour = (ROSE, AMBER, AMBER, GREEN)[lvl]
        d.rounded_rectangle([60 + i * 40, y, 900, y + 120], radius=14, fill=PANEL, outline=colour, width=3)
        label(d, 90 + i * 40, y + 18, "level %d" % lvl, colour, 30, True)
        label(d, 260 + i * 40, y + 20, name, TEXT, 28, True)
        label(d, 90 + i * 40, y + 70, ("held back: " + why) if why else "nothing holds it back", MUTED, 24)
    jars = [ln for ln in act("FOUR") if ln.startswith("THEIR JARS")]
    blocks = []
    cur = None
    for ln in act("FOUR"):
        if ln.startswith("THEIR JARS"):
            cur = []
            blocks.append(cur)
        elif not ln.startswith("  "):
            cur = None
        elif cur is not None:
            cur.append(ln[2:])
    assert len(blocks) == len(jars) == 2
    for b, (title, colour, x) in enumerate((("TWO CARELESS BUILDS", ROSE, 980), ("TWO REPRODUCIBLE BUILDS", GREEN, 980))):
        y = 180 + b * 370
        header(d, x, y, title, colour)
        f = dict(facts(blocks[b]))
        rows = facts(blocks[b])
        for k, (key, v) in enumerate(r for r in rows if r[0].startswith("build")):
            fit_mono(d, x + 10, y + 75 + k * 60, key + "  " + v, TEXT, 28, 860)
        same = f["same jar"]
        cell(d, x + 10, y + 200, 420, 60, "same jar: " + same, GREEN if same == "yes" else ROSE, 28)
    inputs = dict(facts(section("FOUR", "TWO CARELESS")))
    label(d, 1450, 405, "own clock, own file order", MUTED, 24, True)
    label(d, 1450, 775, value("FOUR", "TWO REPRODUCIBLE", "both").split(",")[0] + ",", MUTED, 22, True)
    label(d, 1450, 805, "entries sorted by name", MUTED, 22, True)
    assert inputs
    caption(d, "Higher levels keep the key away from builds; reproducible builds let anyone check.", AMBER, 935)


def three_bills(img, d, cfg):
    """What still goes wrong: no check, a stolen key, a stale SBOM."""
    nogate = section("FIVE", "NO GATE")[0].strip()
    stolen = dict(facts(section("FIVE", "A STOLEN BUILDER KEY")))
    stale = dict(facts(section("FIVE", "A STALE SBOM")))
    cards = [
        ("NO CHECK AT DEPLOY", [re.split(r"\s{2,}", nogate)[0], "no check runs", "deployed"], nogate.split("<- ")[1]),
        ("A STOLEN BUILDER KEY", ["subject: " + stolen["subject"].split(", ")[0],
                                  "claims main's repository", "signed with " + stolen["signed with"],
                                  "the gate: " + stolen["the gate"].split()[0]], stolen["the gate"].split("<- ")[1]),
        ("A STALE SBOM", ["records 1.4.0 " + hash_of(stale["SBOM records"])[:8] + "...",
                          "1.4.1 jar " + hash_of(stale["1.4.1 jar"])[:8] + "...",
                          "describes it: " + stale["describes it"].split()[0]], stale["describes it"].split("<- ")[1]),
    ]
    for i, (title, lines, lesson) in enumerate(cards):
        x = 60 + i * 610
        panel(d, x, 180, 580, 700, ROSE, 3)
        header(d, x + 20, 200, title, ROSE, 540)
        for k, ln in enumerate(lines):
            fit_mono(d, x + 30, 300 + k * 75, ln, TEXT, 30, 520)
        words = lesson.split()
        half = (len(words) + 1) // 2
        for k, part in enumerate((" ".join(words[:half]), " ".join(words[half:]))):
            label(d, x + 290, 740 + k * 50, part, AMBER, 30, True, "ma")
    caption(d, "Provenance and an SBOM only protect a store that checks them and guards the key.", AMBER, 935)
