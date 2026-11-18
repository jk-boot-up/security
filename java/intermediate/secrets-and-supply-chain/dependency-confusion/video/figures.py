"""Pictures for the dependency confusion video, drawn by the `figure` slide kind.

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


def sections(name, head):
    """Every section of an act whose header starts with `head`, as (header, lines) with two spaces removed."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append((ln.rstrip(":"), cur))
        elif cur is not None:
            cur.append(ln[2:])
    return found


def lines_of(name, head, n=0):
    return sections(name, head)[n][1]


def cols(ln):
    return re.split(r"\s{2,}", ln.strip())


def installed(lines):
    """`installed NAME VERSION from REPO` lines as (name, version, repo)."""
    return [re.match(r"installed (\S+) (\S+) from (\S+)", ln).groups() for ln in lines if ln.startswith("installed")]


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


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def source_colour(name, repo):
    """An internal name from the public registry is the attack; everything else is as intended."""
    return ROSE if name.startswith("shop-") and repo == "public-registry" else GREEN


# ------------------------------------------------------------------ figures

def one_name_two_places(img, d, cfg):
    """The core idea: one internal name in two repositories, and the highest version wins."""
    asks = dict((c[0], c[1]) for c in map(cols, lines_of("ONE", "THE BUILD ASKS")))
    holds = dict((c[0], c[1]) for c in map(cols, lines_of("ONE", "THE REPOSITORIES")))
    attacker = [cols(ln) for ln in lines_of("TWO", "THE ATTACKER PUBLISHES") if ln.startswith("shop-pricing")][0]
    script = next(ln for ln in lines_of("TWO", "THE ATTACKER PUBLISHES") if ln.startswith("the script")).split(": ", 1)[1]
    name, ver, repo = next(r for r in installed(lines_of("TWO", "THE RESULT")) if r[0] == "shop-pricing")
    ours = re.search(r"shop-pricing ([0-9.]*[0-9])", holds["company-private"]).group(1)
    header(d, 760, 180, "THE BUILD ASKS FOR", CYAN, 400)
    cell(d, 660, 240, 600, 70, "shop-pricing " + asks["shop-pricing"], CYAN, 34)
    for x, title, who, v, extra, colour in ((80, "company-private", "only the store can publish", ours, "the real library", GREEN),
                                            (1080, "public-registry", "anyone can publish", attacker[1], attacker[2], ROSE)):
        panel(d, x, 380, 760, 300, colour, 3)
        label(d, x + 30, 400, title, colour, 34, True)
        label(d, x + 30, 450, who, MUTED, 26)
        cell(d, x + 30, 510, 700, 70, "shop-pricing " + v, colour, 34)
        label(d, x + 30, 610, extra, colour if colour == ROSE else MUTED, 26, True)
    draw.arrow(d, 860, 315, 460, 375, MUTED, 4)
    draw.arrow(d, 1060, 315, 1460, 375, MUTED, 4)
    label(d, 960, 400, "both are", MUTED, 26, True, "ma")
    label(d, 960, 435, "in range", MUTED, 26, True, "ma")
    header(d, 80, 720, "THE RESULT (highest version wins)", ROSE)
    fit_mono(d, 80, 790, "installed %s %s from %s" % (name, ver, repo), source_colour(name, repo), 34, 1760)
    fit_mono(d, 80, 845, "its install script runs: " + script, ROSE, 30, 1760)
    caption(d, "The attacker needs only the name, never the store's code.", AMBER, 935)


def digest_steps(img, d, cfg):
    """Act ONE: the build that works, its lockfile, and how one digest is made."""
    header(d, 60, 180, "THE RESULT", GREEN)
    for i, (n, v, r) in enumerate(installed(lines_of("ONE", "THE RESULT"))):
        cell(d, 60, 240 + i * 70, 820, 56, "%s %s from %s" % (n, v, r), source_colour(n, r), 26)
    head, lock = sections("ONE", "THE LOCKFILE")[0]
    header(d, 60, 480, "THE LOCKFILE", CYAN)
    label(d, 60, 540, "version and SHA-256 digest (first 16 hex digits)", MUTED, 24)
    for i, ln in enumerate(lock):
        n, v, dig = cols(ln)
        y = 590 + i * 70
        fit_mono(d, 60, y, n, TEXT, 28, 260)
        fit_mono(d, 330, y, v, TEXT, 28, 120)
        fit_mono(d, 470, y, dig, CYAN, 28, 420)
    head, steps = sections("ONE", "HOW A DIGEST IS MADE")[0]
    header(d, 980, 180, head.replace("HOW A DIGEST IS MADE", "ONE DIGEST"), AMBER)
    rows = [cols(ln) for ln in steps]
    for i, r in enumerate(rows[:3]):
        y = 250 + i * 80
        label(d, 980, y + 30, r[0], MUTED, 24, True, "lm")
        text, note = (r[1], r[2]) if len(r) == 3 else ("(empty)", r[1])
        cell(d, 1100, y, 400, 60, text, TEXT, 26)
        label(d, 1520, y + 30, note, MUTED, 22, False, "lm")
    nbytes = dict((r[0], r[1]) for r in rows if len(r) == 2)["bytes"]
    label(d, 1100, 500, nbytes, MUTED, 24)
    draw.arrow(d, 1300, 545, 1300, 600, AMBER, 4)
    label(d, 1330, 560, "SHA-256", AMBER, 28, True)
    sha = [r[1] for r in rows if r[0] == "SHA-256"] + [steps[-1].strip()]
    for i, half in enumerate(sha):
        fit_mono(d, 1000, 620 + i * 50, half, GREEN, 34, 860)
    label(d, 1000, 740, "64 hex digits: change one byte, and they all change", MUTED, 24)
    caption(d, "The lockfile records the exact bytes a trusted build installed.", AMBER, 935)


def version_race(img, d, cfg):
    """Act TWO: the two versions compared number by number, and the script that runs."""
    head, rows = sections("TWO", "HOW THE HIGHEST VERSION")[0]
    header(d, 60, 180, head, AMBER)
    parts = ("major", "minor", "patch")
    reps = [cols(ln) for ln in rows if not ln.startswith("compare")]
    compare = next(cols(ln)[1] for ln in rows if ln.startswith("compare"))
    decides = compare.split()[0]
    for i, (repo, ver, rng) in enumerate(reps):
        y = 270 + i * 140
        colour = GREEN if repo == "company-private" else ROSE
        label(d, 60, y + 35, repo, colour, 32, True, "lm")
        for j, (p, num) in enumerate(zip(parts, ver.split("."))):
            x = 420 + j * 230
            if i == 0:
                label(d, x + 100, y - 40, p, MUTED, 24, True, "ma")
            cell(d, x, y, 200, 70, num, AMBER if p == decides else colour, 40)
        label(d, 1140, y + 35, rng, MUTED, 28, False, "lm")
    fit_mono(d, 60, 560, "compare: " + compare, AMBER, 34, 1800)
    script_lines = lines_of("TWO", "THE RESULT")
    k = next(i for i, ln in enumerate(script_lines) if ln.startswith("INSTALL SCRIPT RAN"))
    header(d, 60, 640, "THE RESULT", ROSE)
    fit_mono(d, 60, 710, script_lines[k], ROSE, 30, 1800)
    fit_mono(d, 100, 760, script_lines[k + 1].strip(), ROSE, 30, 1760)
    label(d, 60, 830, "on the build machine, which holds the CI secrets, signing keys and deploy keys", MUTED, 26)
    caption(d, "Numbers compare left to right: the first one that differs decides.", AMBER, 935)


def order_fallthrough(img, d, cfg):
    """Act THREE: three setups with a repository order, and where the name came from."""
    setups = [h for h, _ in sections("THREE", "THE SETUP")]
    results = sections("THREE", "THE RESULT")
    order = ["company-private", "public-registry"]
    picks = ["shop-pricing", "shop-newlib", "shop-audit"]
    notes = ["the name is in the first repository",
             "the name is missing there, so it falls through",
             "the first repository has a copy of the pinned version"]
    header(d, 60, 180, "THE SETUP: the order", CYAN, 560)
    header(d, 1000, 180, "THE RESULT: where the name came from", GREEN)
    for i, (setup, (rh, rl)) in enumerate(zip(setups, results)):
        y = 270 + i * 210
        first = re.search(r"\((\S+) first", setup)
        if first:
            order = [first.group(1)] + [o for o in order if o != first.group(1)]
        for j, repo in enumerate(order):
            cell(d, 60 + j * 450, y, 380, 60, "%d. %s" % (j + 1, repo), GREEN if repo == "company-private" else MUTED, 26)
        draw.arrow_right(d, 445, y + 30, 505, MUTED, 4)
        n, v, r = next(x for x in installed(rl) if x[0] == picks[i])
        ran = any(ln.startswith("INSTALL SCRIPT RAN for " + n) for ln in rl)
        colour = ROSE if ran else GREEN
        cell(d, 1000, y, 860, 60, "%s %s from %s" % (n, v, r), colour, 28)
        label(d, 1000, y + 80, notes[i] + ("; its script ran" if ran else ""), colour, 24, True)
    caption(d, "Order protects only names that the first repository already has.", AMBER, 935)


def name_routing(img, d, cfg):
    """Act FOUR: each name may come from one repository only, and a missing internal name fails closed."""
    rules = [cols(ln) for ln in lines_of("FOUR", "THE RULE")]
    refused = " ".join(ln.strip() for ln in lines_of("FOUR", "THE RESULT", 1) if not ln.startswith("installed"))
    attacker = [cols(ln)[:2] for ln in lines_of("TWO", "THE ATTACKER PUBLISHES") if not ln.startswith("the script")]
    holds = dict((c[0], c[1]) for c in map(cols, lines_of("ONE", "THE REPOSITORIES")))
    header(d, 60, 180, sections("FOUR", "THE RULE")[0][0], CYAN)
    ys = {"company-private": 270, "public-registry": 560}
    panel(d, 1060, 270, 800, 240, GREEN, 3)
    label(d, 1090, 290, "company-private", GREEN, 32, True)
    fit_mono(d, 1090, 345, holds["company-private"], TEXT, 26, 740)
    panel(d, 1060, 560, 800, 240, ROSE, 3)
    label(d, 1090, 580, "public-registry", ROSE, 32, True)
    fit_mono(d, 1090, 635, holds["public-registry"], TEXT, 26, 740)
    fit_mono(d, 1090, 690, "and the attacker's " + ", ".join(" ".join(a) for a in attacker), MUTED, 24, 740)
    label(d, 1090, 740, "never asked for a shop- name", ROSE, 24, True)
    for i, (n, kind, to) in enumerate(rules):
        y = 280 + i * 130
        repo = to.split()[1]
        missing = n not in holds[repo]
        colour = ROSE if missing else (GREEN if kind == "internal" else MUTED)
        cell(d, 60, y, 330, 60, n, colour, 28)
        label(d, 410, y + 30, kind, MUTED, 24, True, "lm")
        draw.arrow(d, 560, y + 30, 1050, ys[repo] + 120, colour, 3)
        if missing:
            label(d, 60, y + 75, "not there: refused, the build stops", ROSE, 24, True)
    fit_mono(d, 60, 850, refused, ROSE, 26, 1800)
    caption(d, "The name picks the repository; a missing internal name fails closed.", AMBER, 935)


def lock_check(img, d, cfg):
    """Act FIVE: what the lockfile says against what the build found, and the name watch."""
    head, rows = sections("FIVE", "HOW THE LOCKFILE CHECK")[0]
    header(d, 60, 180, head, CYAN)
    for k, t in enumerate(("source", "version", "digest (first 16 hex digits)", "check")):
        label(d, (60, 520, 760, 1440)[k], 250, t, MUTED, 24, True)
    for i, ln in enumerate(rows):
        c = cols(ln)
        y = 300 + i * 90
        colour = CYAN if i == 0 else ROSE
        fit_mono(d, 60, y + 30, c[0], TEXT, 30, 440, "lm")
        cell(d, 520, y, 200, 60, c[1], colour if i == 0 or "version" in c[3] else TEXT, 30)
        cell(d, 760, y, 640, 60, c[2], colour if i == 0 or "digest" in c[3] else TEXT, 28)
        if len(c) > 3:
            cell(d, 1440, y, 420, 60, c[3] + ": refused", ROSE, 28)
    watch = dict((c[0], c[1]) for c in map(cols, lines_of("FIVE", "THE NAME WATCH")))
    also = [n.strip() for n in watch["also public"].split(",")]
    header(d, 60, 600, "THE NAME WATCH", AMBER)
    for j, n in enumerate(watch["internal names"].split(", ")):
        cell(d, 60 + j * 420, 670, 380, 60, n, AMBER if n in also else GREEN, 28)
        label(d, 250 + j * 420, 745, "also public: alert" if n in also else "only ours", AMBER if n in also else GREEN, 24, True, "ma")
    caption(d, "Same version is not enough: the bytes must match, or the build stops.", AMBER, 935)
