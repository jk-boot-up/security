"""Pictures for the Linux hardening with Docker video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints from real
containers, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
SEV = {"CRITICAL": ROSE, "HIGH": ROSE, "MEDIUM": AMBER, "LOW": MUTED}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The first section of an act whose header starts with `head`, as lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = [] if ln.startswith(head) and not found else None
            if cur is not None:
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[0]


def pairs(lines):
    """`key   value` lines as a list of (key, value)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip())]


def facts(lines):
    return dict(pairs(lines))


def report(lines):
    """Report lines as (severity, rule, where, auto)."""
    rows = []
    for ln in lines:
        p = ln.split()
        if p and p[0] in SEV:
            rows.append((p[0], p[1], " ".join(x for x in p[2:] if x != "auto"), p[-1] == "auto"))
    return rows


def first_number(text):
    return int(re.search(r"\d+", text).group())


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


def caption(d, text, colour=AMBER, y=935):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


# ------------------------------------------------------------------ figures

def layers(img, d, cfg):
    """Where a fix made in a running container lives: its own thin layer, on top of the image's read-only layers."""
    diff = [ln.strip() for ln in section("TWO", "WHERE THE FIX LIVES") if ln.strip().startswith("C ")]
    parents = next(ln.strip() for ln in section("TWO", "WHERE THE FIX LIVES") if ln.strip().startswith("and "))
    fresh = first_number(facts(section("TWO", "A NEW CONTAINER"))["findings"])
    image = re.search(r"\((.*)\)", next(ln for ln in act("ONE") if ln.startswith("THE CONTAINER"))).group(1)
    # the old container
    header(d, 60, 180, "THE RUNNING CONTAINER, AFTER THE FIX", ROSE)
    panel(d, 60, 245, 820, 420, ROSE, 3)
    label(d, 90, 265, "its own writable layer (docker diff)", ROSE, 26, True)
    for k, ln in enumerate(diff):
        fit_mono(d, 90, 310 + k * 44, ln, TEXT, 28, 760)
    label(d, 90, 625, parents, MUTED, 24, True)
    # the image
    for k in range(3):
        y = 700 + k * 52
        d.rounded_rectangle([60, y, 1860, y + 44], radius=10, fill=BG_CELL, outline=CYAN, width=3)
    label(d, 960, 722 + 52, "THE IMAGE %s: read-only layers, unchanged" % image, CYAN, 28, True, "mm")
    # the new container
    header(d, 1040, 180, "A NEW CONTAINER FROM THE SAME IMAGE", AMBER)
    panel(d, 1040, 245, 820, 420, AMBER, 3)
    label(d, 1070, 265, "a fresh, empty writable layer", AMBER, 26, True)
    label(d, 1450, 430, str(fresh), ROSE, 140, True, "mm")
    label(d, 1450, 540, "findings again", ROSE, 34, True, "mm")
    draw.arrow_right(d, 890, 450, 1030, MUTED, 5)
    caption(d, "A fix in a running container is not a fix: the next container starts from the image.", AMBER, 940)


def report_grid(img, d, cfg):
    """The findings, coloured by severity; the ones a machine can fix are marked."""
    rows = report(section("ONE", "THE REPORT"))
    count = facts(section("ONE", "THE COUNT"))
    header(d, 60, 180, "THE REPORT: %d FINDINGS, WORST FIRST" % len(rows), ROSE)
    for k, (sev, rule, where, auto) in enumerate(rows):
        x, y = 60 + (k % 4) * 455, 245 + (k // 4) * 105
        colour = SEV[sev]
        d.rounded_rectangle([x, y, x + 435, y + 92], radius=10, fill=BG_CELL, outline=GREEN if auto else colour, width=5 if auto else 3)
        fit_mono(d, x + 14, y + 28, rule, colour, 24, 300, "lm")
        label(d, x + 421, y + 28, sev.lower(), colour, 20, True, "rm")
        fit_mono(d, x + 14, y + 66, where, TEXT, 20, 330, "lm")
        if auto:
            label(d, x + 421, y + 66, "auto", GREEN, 22, True, "rm")
    label(d, 60, 772, "severity    " + count["severity"], TEXT, 30, True)
    label(d, 60, 815, "a machine  " + count["machine"], GREEN, 30, True)
    label(d, 60, 858, "a person    " + count["person"], AMBER, 30, True)
    caption(d, "Green border: a permission a machine can fix. The rest needs a person.", AMBER, 920)


def findings_bars(img, d, cfg):
    """The number of findings: the typical container, after the fix, a new container."""
    before = first_number(next(ln for ln in act("ONE") if ln.startswith("THE REPORT")))
    after = facts(section("TWO", "AFTER THE FIX"))["findings"]
    left = int(re.search(r"= (\d+)", after).group(1))
    fresh = first_number(facts(section("TWO", "A NEW CONTAINER"))["findings"])
    commands = [ln.strip() for ln in section("TWO", "THE FIX COMMANDS")]
    header(d, 60, 180, "THE FIX COMMANDS", AMBER)
    for k, c in enumerate(commands):
        fit_mono(d, 60, 250 + k * 62, c, TEXT, 28, 760)
    header(d, 960, 180, "FINDINGS", CYAN)
    bars = [("the typical container", before, ROSE), ("after the fix, same container", left, AMBER), ("a new container", fresh, ROSE)]
    top, base = 340, 760
    for k, (name, n, colour) in enumerate(bars):
        x = 980 + k * 300
        h = (base - top) * n / max(b[1] for b in bars)
        d.rectangle([x, base - h, x + 200, base], fill=colour)
        label(d, x + 100, base - h - 50, str(n), colour, 40, True, "ma")
        for j, word in enumerate(re.findall(r"\S+(?: \S+)?", name)):
            label(d, x + 100, base + 20 + j * 34, word, MUTED, 24, True, "ma")
    label(d, 60, 720, "%d - %d = %d in the running container" % (before, before - left, left), AMBER, 30, True)
    caption(d, "The seven fixes lived in the old container; the new one has %d findings again." % fresh, AMBER, 935)


def before_after(img, d, cfg):
    """The typical and the hardened container, as the same script read them."""
    typical = pairs(section("ONE", "THE CONTAINER"))
    hardened = pairs(section("THREE", "THE CONTAINER"))
    scan = facts(section("THREE", "THE SCAN"))
    names = [re.search(r"\((.*)\)", h).group(1) for h in (next(ln for ln in act(a) if ln.startswith("THE CONTAINER")) for a in ("ONE", "THREE"))]
    for i, (rows, name, colour) in enumerate(((typical, names[0], ROSE), (hardened, names[1], GREEN))):
        x0 = 60 + i * 930
        header(d, x0, 180, ("BEFORE: " if i == 0 else "AFTER: ") + name, colour)
        keep = [r for r in rows if r[0] in ("UID 0", "no password", "set-user-id", "world-write", "mode", "sshd", "sysctl")]
        for k, (key, value) in enumerate(keep[:15]):
            y = 245 + k * 41
            label(d, x0, y, key, MUTED, 25, True)
            fit_mono(d, x0 + 215, y, value, TEXT, 25, 660)
    label(d, 60, 878, "findings: %s -> %s     process: %s" % (first_number(next(ln for ln in act("ONE") if ln.startswith("THE REPORT"))),
                                                         scan["findings"], scan["process"]), GREEN, 28, True)
    caption(d, "The decisions live in the recipe, so every new container starts hardened.", AMBER, 935)


def drift(img, d, cfg):
    """A config file loosened inside a running container: the mode digits, and the bits that are too many."""
    m = re.search(r"(\d{4}) & ~(\d{4}) = (\d{4})", facts(section("FOUR", "AFTER THE DRIFT"))["too open"])
    mode, allowed, extra = m.groups()
    command = section("FOUR", "THE DRIFT")[0].strip()
    rows = report(section("FOUR", "AFTER THE DRIFT"))
    fresh = facts(section("FOUR", "A NEW CONTAINER"))["findings"]
    end = header(d, 60, 180, "THE DRIFT", AMBER)
    fit_mono(d, end + 24, 202, command, TEXT, 32, 1500, "lm")
    names = ["special", "owner", "group", "others"]
    for r, (title, value, colour) in enumerate((("the mode now", mode, ROSE), ("the policy allows", allowed, GREEN), ("too open", extra, ROSE))):
        y = 280 + r * 120
        label(d, 60, y + 40, title, colour, 30, True, "lm")
        for k, digit in enumerate(value):
            x = 420 + k * 130
            cell(d, x, y, 110, 80, digit, colour if digit != "0" else MUTED, 44)
            if r == 0:
                label(d, x + 55, y - 26, names[k], MUTED, 22, True, "ma")
    header(d, 1000, 280, "THE SAME SCAN FINDS", ROSE)
    for k, (sev, rule, where, auto) in enumerate(rows):
        cell(d, 1000, 350 + k * 90, 860, 70, rule + "  " + sev.lower(), SEV[sev], 28)
    header(d, 1000, 560, "A NEW CONTAINER", GREEN)
    fit_mono(d, 1000, 640, "findings " + fresh, GREEN, 28, 860)
    caption(d, "6 is read + write: the group and everyone else can now change the shop's config.", AMBER, 935)


def kernel(img, d, cfg):
    """A container shares the host's kernel: it reads the host's value and cannot change it."""
    f = facts(section("FIVE", "THE KERNEL"))
    panel(d, 60, 190, 1800, 640, CYAN, 3)
    label(d, 90, 210, "THE HOST", CYAN, 30, True)
    panel(d, 120, 270, 760, 300, AMBER, 3)
    label(d, 150, 290, "THE CONTAINER", AMBER, 28, True)
    fit_mono(d, 150, 360, "read:", MUTED, 28, 700)
    fit_mono(d, 150, 405, f["read"], TEXT, 26, 700)
    fit_mono(d, 150, 470, "write 0:", MUTED, 28, 700)
    fit_mono(d, 150, 515, f["write 0"], ROSE, 26, 700)
    d.rounded_rectangle([120, 640, 1830, 790], radius=14, fill=BG_CELL, outline=GREEN, width=3)
    label(d, 975, 690, "ONE KERNEL, SHARED BY THE HOST AND EVERY CONTAINER", GREEN, 32, True, "mm")
    label(d, 975, 745, "kernel.randomize_va_space lives here", MUTED, 26, True, "mm")
    draw.arrow(d, 500, 575, 500, 635, MUTED, 4)
    for k, ln in enumerate(section("FIVE", "WHAT IT MEANS")[:2]):
        fit_mono(d, 940, 320 + k * 60, ln.strip(), TEXT, 26, 900)
    caption(d, "Kernel settings are the host's to fix; the image cannot answer them.", AMBER, 935)
