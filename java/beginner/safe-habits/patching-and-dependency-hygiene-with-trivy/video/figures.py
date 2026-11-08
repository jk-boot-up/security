"""Pictures for the patching with Trivy and Gradle video, drawn by the `figure` slide kind.

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
SEV = {"CRITICAL": ROSE, "HIGH": AMBER, "MEDIUM": CYAN, "LOW": GREEN}


def pipeline(img, d, cfg):
    """From the build to a decision: lock file, bill of materials, scanner, report, one move per library."""
    entries = int(first("ONE", r"THE LOCK FILE \(gradle.lockfile, (\d+) entries"))
    comps = int(first("TWO", r"one of (\d+) components"))
    spec = first("TWO", r"(CycloneDX [\d.]+)")
    trivy = first("THREE", r"(Trivy [\d.]+)")
    problems = part("THREE", "THE REPORT")[0].strip()
    moves = len(part("FOUR", "ONE MOVE PER LIBRARY"))
    steps = [("gradle.lockfile", "%d exact versions" % entries, CLIENT),
             ("online-store.cdx.json", "%s, %d components" % (spec, comps), CYAN),
             (trivy, "looks up each package URL", AMBER),
             ("trivy-report.json", problems, ROSE),
             ("the upgrades", "%d moves, one per library" % moves, GREEN)]
    for i, (name, note, c) in enumerate(steps):
        x = 100 + i * 352
        panel(d, x, 330, 310, 200, c, 3)
        fit(d, x + 155, 395, name, c, 28, 290, True, "mm")
        fit(d, x + 155, 460, note, TEXT, 24, 290, False, "mm")
        if i < 4:
            draw.arrow_right(d, x + 314, 430, x + 348, MUTED, 4)
    labels = ["what the build uses", "a list a scanner reads", "the known problems", "what it found", "what to do"]
    for i, t in enumerate(labels):
        label(d, 100 + i * 352 + 155, 560, t, MUTED, 22, False, "ma")
    caption(d, "Each file is checked into the project, so every step can be repeated and compared.", AMBER, 700)


def lockfile(img, d, cfg):
    """The lock file's five lines, and the one library nobody asked for."""
    rows = part("ONE", "THE LOCK FILE")[1:]
    header(d, 100, 205, "THE LOCK FILE: gradle.lockfile", CLIENT)
    for i, ln in enumerate(rows):
        g, a, v = ln.split()
        y = 280 + i * 92
        c = AMBER if a == "commons-lang3" else CLIENT
        panel(d, 100, y, 1000, 74, c)
        mono(d, 125, y + 37, g + ":", MUTED, 24, "lm")
        mono(d, 560, y + 37, a, TEXT, 28, "lm")
        cell(d, 900, y + 10, 180, 54, v, c, 28)
    text_y = 280 + 2 * 92 + 37
    lang_y = 280 + 1 * 92 + 37
    d.line([(1110, text_y), (1160, text_y), (1160, lang_y), (1110, lang_y)], fill=AMBER, width=4)
    d.polygon([(1110, lang_y), (1126, lang_y - 10), (1126, lang_y + 10)], fill=AMBER)
    who = part("ONE", "WHO ASKED FOR THEM")
    label(d, 1190, lang_y - 10, "came on its own,", AMBER, 26, True, "lm")
    label(d, 1190, lang_y + 26, "with commons-text", AMBER, 26, True, "lm")
    label(d, 1190, 560, who[0].strip(), TEXT, 26, True, "lm")
    before = [ln for ln in act("ONE") if ln.startswith("BEFORE")][0].split(": ", 1)[1]
    after = [ln for ln in act("ONE") if ln.startswith("AFTER")][0].split(": ", 1)[1]
    header(d, 100, 760, "BEFORE THE LOCK", ROSE)
    label(d, 420, 782, before, TEXT, 28, False, "lm")
    header(d, 100, 830, "AFTER THE LOCK", GREEN)
    label(d, 420, 852, after, TEXT, 28, False, "lm")


def purl(img, d, cfg):
    """One lock file line becomes one component with a package URL, the name a scanner looks up."""
    inp = part("TWO", "THE INPUT")[0].strip()
    out = part("TWO", "THE OUTPUT")[0].strip()
    parts_ = part("TWO", "THE PACKAGE URL")
    checks = part("TWO", "THE CHECKS")
    header(d, 100, 205, "THE INPUT: ONE LINE OF THE LOCK FILE", CLIENT)
    cell(d, 100, 265, 760, 76, inp, CLIENT, 32)
    draw.arrow_right(d, 880, 303, 1000, MUTED, 5)
    header(d, 1020, 205, "THE OUTPUT: ITS PACKAGE URL", CYAN)
    cell(d, 1020, 265, 800, 76, out, CYAN, 30)
    header(d, 100, 400, "THE PACKAGE URL, PART BY PART", AMBER)
    vals, names = parts_[0].split(), parts_[1].split()
    colours = [MUTED, CLIENT, GREEN, AMBER]
    x = 100
    for v, n, c in zip(vals, names, colours):
        w = max(220, int(font(MONO_B, 34).getlength(v)) + 60)
        cell(d, x, 465, w, 86, v, c, 34)
        label(d, x + w // 2, 575, n, c, 26, True, "ma")
        x += w + 30
    header(d, 100, 660, "THE CHECKS", GREEN)
    for i, ln in enumerate(checks):
        q, a = ln.strip().rsplit(": ", 1)
        y = 720 + i * 70
        mono(d, 120, y + 28, q, TEXT, 24, "lm")
        cell(d, 1300, y, 220, 56, a, GREEN if a in ("true", "refused") else ROSE, 26)


def report(img, d, cfg):
    """What Trivy reported: problems by severity, and by library."""
    rep = part("THREE", "THE REPORT")
    sev = re.findall(r"([A-Z]+) (\d+)", rep[1])
    libs = [(ln.split()[0], int(ln.split()[1])) for ln in rep[2:]]
    header(d, 100, 205, first("THREE", r"THE INPUT: (.+)").upper(), CLIENT)
    header(d, 100, 290, "BY SEVERITY", AMBER)
    top = max(int(n) for _, n in sev) or 1
    for i, (s_, n) in enumerate(sev):
        y = 360 + i * 100
        label(d, 100, y + 35, s_, SEV[s_], 28, True, "lm")
        w = int(560 * int(n) / top)
        if w:
            d.rounded_rectangle([300, y + 8, 300 + w, y + 62], radius=8, fill=SEV[s_])
        label(d, 310 + w, y + 35, n, TEXT, 30, True, "lm")
    header(d, 1000, 290, "BY LIBRARY", AMBER)
    top = max(n for _, n in libs)
    for i, (name, n) in enumerate(libs):
        y = 360 + i * 100
        mono(d, 1000, y + 35, name, TEXT, 26, "lm")
        w = int(420 * n / top)
        d.rounded_rectangle([1250, y + 8, 1250 + w, y + 62], radius=8, fill=ROSE if n > 1 else AMBER)
        label(d, 1260 + w, y + 35, str(n), TEXT, 30, True, "lm")
    caption(d, rep[0].strip() + ". " + act("THREE")[-1].capitalize() + ".", AMBER, 840)


def targets(img, d, cfg):
    """log4j-core's fixes on a version line: the highest one fixes all seven at once."""
    fixes = first("FOUR", r"fixes (.+)$").split(", ")
    highest = first("FOUR", r"highest ([\d.]+)")
    allx = first("FOUR", r"one move fixes all (\d+)")
    moves = part("FOUR", "ONE MOVE PER LIBRARY")
    installed = [ln.split()[1] for ln in moves if ln.strip().startswith("log4j-core")][0]
    header(d, 100, 205, "log4j-core %s: ITS FIXES, ON A VERSION LINE" % installed, CYAN)
    pts = [installed] + fixes
    x0, x1, y = 160, 1760, 400
    d.line([(x0, y), (x1, y)], fill=MUTED, width=4)
    for i, v in enumerate(pts):
        x = x0 + (x1 - x0) * i / (len(pts) - 1)
        c = ROSE if i == 0 else GREEN if v == highest else AMBER
        d.ellipse([x - 14, y - 14, x + 14, y + 14], fill=c)
        label(d, x, y + 30, v, c, 26, True, "ma")
    label(d, x0, y - 60, "installed", ROSE, 24, True, "ma")
    label(d, x1, y - 60, "highest: fixes all %s" % allx, GREEN, 24, True, "ra")
    label(d, x0 + (x1 - x0) / (len(pts) - 1), y - 60, "first fix", AMBER, 24, True, "ma")
    header(d, 100, 530, "ONE MOVE PER LIBRARY", GREEN)
    for i, ln in enumerate(moves):
        m = re.match(r"(\S+)\s+(\S+)\s+-> (\S+)(.*)$", ln.strip())
        name, frm, to, note = m.groups()
        yy = 595 + i * 80
        mono(d, 100, yy + 30, name, TEXT, 28, "lm")
        cell(d, 460, yy, 180, 62, frm, ROSE, 28)
        draw.arrow_right(d, 650, yy + 31, 720, MUTED, 4)
        cell(d, 730, yy, 180, 62, to, GREEN, 28)
        if note.strip():
            label(d, 940, yy + 31, "a new major version: run the tests", AMBER, 26, True, "lm")


def pins(img, d, cfg):
    """The pinned checksums, and the one copy whose checksum no longer matches."""
    rows = part("FIVE", "THE PINS")
    changed = part("FIVE", "BEFORE THE CHECK")[0].split()
    after = first("FIVE", r"AFTER THE CHECK: (.+)$")
    header(d, 100, 205, "THE PINS: SHA-256 OF EACH FILE", CLIENT)
    for i, ln in enumerate(rows):
        name, h = ln.split()
        y = 270 + i * 92
        bad = name == changed[0]
        panel(d, 100, y, 1000, 74, ROSE if bad else CLIENT)
        mono(d, 125, y + 37, name, TEXT, 28, "lm")
        mono(d, 660, y + 37, h, GREEN, 26, "lm")
    header(d, 1180, 205, "THE CHANGED COPY", ROSE)
    mono(d, 1180, 290, changed[0], TEXT, 26)
    cell(d, 1180, 340, 640, 64, changed[1], ROSE, 26)
    label(d, 1180, 440, "AFTER THE CHECK:", ROSE, 26, True)
    fit(d, 1180, 480, after, TEXT, 26, 640, True)
    label(d, 1180, 540, "Gradle refuses to build.", AMBER, 30, True)
    caption(d, "A download is used only when its SHA-256 matches the pin.", AMBER, 880)
