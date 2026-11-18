"""Pictures for the dependency confusion with Gradle video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints from real
Gradle builds and the integration test checks, so a picture can never disagree with the code.
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


def resolved(lines):
    """`group:name:version   from origin` lines as (coordinates, origin); a refusal as ("refused", reason)."""
    rows = []
    for ln in lines:
        m = re.match(r"(\S+)\s+from (.+)", ln.strip())
        if m and ":" in m.group(1):
            rows.append(m.groups())
        elif ln.startswith("refused: "):
            rows.append(("refused", ln[len("refused: "):]))
    return rows


def origin_colour(origin):
    return ROSE if "attacker" in origin else GREEN if origin == "private" else MUTED


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


def result_rows(d, x, y, rows, w, size=28, step=66):
    for i, (coord, origin) in enumerate(rows):
        if coord == "refused":
            cell(d, x, y + i * step, w, 54, "refused: " + origin, GREEN, size)
        else:
            cell(d, x, y + i * step, w, 54, "%s  from %s" % (coord, origin), origin_colour(origin), size)


# ------------------------------------------------------------------ figures

def every_repository(img, d, cfg):
    """The core idea: for a range, Gradle reads the version list of every repository and takes the highest."""
    head, how = sections("TWO", "HOW GRADLE PICKS")[0]
    asked = re.search(r"\((.*)\)", head).group(1)
    lists = [re.match(r"(\S+)\s+lists (.+) in maven-metadata.xml", ln.strip()).groups() for ln in how if " lists " in ln]
    top = next(ln.strip() for ln in how if ln.strip().startswith("the highest"))
    coord, origin = next(r for r in resolved(lines_of("TWO", "THE RESULT")) if "pricing" in r[0])
    header(d, 760, 180, "THE BUILD ASKS FOR", CYAN, 400)
    cell(d, 600, 240, 720, 70, asked, CYAN, 34)
    for i, (repo, versions) in enumerate(lists):
        x, colour = (80, GREEN) if repo == "private" else (1080, ROSE)
        panel(d, x, 380, 760, 270, colour, 3)
        label(d, x + 30, 400, "the %s repository" % repo, colour, 34, True)
        label(d, x + 30, 450, "only the store publishes" if repo == "private" else "anyone can publish", MUTED, 26)
        label(d, x + 30, 510, "its maven-metadata.xml lists", MUTED, 26)
        cell(d, x + 30, 560, 700, 64, versions, colour, 34)
    draw.arrow(d, 860, 315, 460, 375, MUTED, 4)
    draw.arrow(d, 1060, 315, 1460, 375, MUTED, 4)
    label(d, 960, 400, "both", MUTED, 26, True, "ma")
    label(d, 960, 435, "asked", MUTED, 26, True, "ma")
    header(d, 80, 690, "THE RESULT", ROSE)
    fit_mono(d, 80, 760, top, AMBER, 32, 1760)
    fit_mono(d, 80, 815, "resolved %s from %s" % (coord, origin), origin_colour(origin), 32, 1760)
    caption(d, "A range asks every repository; the highest number wins, whoever published it.", AMBER, 935)


def build_works(img, d, cfg):
    """Act ONE: the repositories in order, the dependencies, and who published each jar."""
    header(d, 60, 180, "THE REPOSITORIES (in order)", CYAN)
    for i, ln in enumerate(lines_of("ONE", "THE REPOSITORIES")):
        n, note = cols(ln)
        y = 250 + i * 80
        cell(d, 60, y, 300, 60, n, GREEN if "private" in n else MUTED, 30)
        label(d, 390, y + 30, note, MUTED, 26, False, "lm")
    header(d, 60, 440, "THE DEPENDENCIES", CYAN)
    for i, ln in enumerate(lines_of("ONE", "THE DEPENDENCIES")):
        fit_mono(d, 60, 510 + i * 60, ln.strip(), TEXT, 32, 760)
    header(d, 1000, 180, "THE RESULT", GREEN)
    label(d, 1000, 245, "who published each jar (origin.txt inside it)", MUTED, 24)
    result_rows(d, 1000, 300, resolved(lines_of("ONE", "THE RESULT")), 860, 28, 80)
    caption(d, "Internal names come from private, the public library from public.", AMBER, 935)


def first_or_highest(img, d, cfg):
    """Act TWO: a range takes the highest of every repository; an exact version, the first that has it."""
    setups = [ln.strip() for ln in lines_of("TWO", "THE SETUP")]
    results = sections("TWO", "THE RESULT")
    for i, (head, rows) in enumerate(results):
        x = 60 + i * 930
        header(d, x, 180, ("A RANGE" if i == 0 else "AN EXACT VERSION, PUBLIC FIRST"), AMBER)
        label(d, x, 245, re.search(r"\((.*)\)", head).group(1), MUTED, 24, True)
        result_rows(d, x, 300, resolved(rows), 870, 26, 72)
    fit_mono(d, 60, 560, lines_of("TWO", "HOW GRADLE PICKS")[-1].strip(), AMBER, 28, 860)
    for j, s in enumerate(setups):
        fit_mono(d, 990, 560 + j * 45, s, MUTED, 24, 870)
    label(d, 60, 700, "Rose: the attacker's jar. Neither a range nor a pin says where a name may come from.", MUTED, 28)
    caption(d, "A range picks the highest anywhere; a pin takes the first copy found.", AMBER, 935)


def filter_log(img, d, cfg):
    """Act THREE: the filters, and what the public repository's log saw with and without them."""
    filters = [cols(ln) for ln in lines_of("THREE", "THE FILTERS")]
    header(d, 60, 180, "THE FILTERS", CYAN)
    for i, (repo, rule) in enumerate(filters):
        y = 250 + i * 80
        cell(d, 60, y, 200, 60, repo, GREEN if repo == "private" else ROSE, 30)
        fit_mono(d, 290, y + 30, rule, TEXT, 28, 640, "lm")
    with_filters = lines_of("THREE", "THE PUBLIC REPOSITORY'S LOG")[0].strip()
    newlib = resolved(lines_of("THREE", "THE RESULT", 1))[0]
    received = next(ln.strip() for ln in lines_of("THREE", "THE RESULT", 1) if "received" in ln)
    without = [ln.strip() for ln in lines_of("THREE", "WITHOUT THE FILTERS") if ln.strip().startswith("GET")]
    header(d, 1000, 180, "THE PUBLIC REPOSITORY'S LOG", AMBER)
    label(d, 1000, 250, "with the filters, the usual build:", MUTED, 26)
    fit_mono(d, 1000, 290, with_filters + " for com/shop", GREEN, 32, 860)
    label(d, 1000, 360, "with the filters, the unpublished library:", MUTED, 26)
    fit_mono(d, 1000, 400, received, GREEN, 26, 860)
    header(d, 60, 470, "AN INTERNAL LIBRARY NOT PUBLISHED YET", CYAN)
    cell(d, 60, 540, 860, 60, "refused: " + newlib[1], GREEN, 28)
    label(d, 60, 620, "with the filters the build stops; nobody else is asked", GREEN, 26, True)
    header(d, 1000, 470, "WITHOUT THE FILTERS", ROSE)
    for i, g in enumerate(without):
        cell(d, 1000, 540 + i * 70, 860, 60, g, ROSE, 28)
    label(d, 1000, 620 + (len(without) - 1) * 70, "the internal name leaks to the public repository", ROSE, 26, True)
    caption(d, "A filter keeps the name at home: zero requests, and a clear failure.", AMBER, 935)


def checksum_check(img, d, cfg):
    """Act FOUR: the checksum in the metadata against the attacker's jar and a replaced jar."""
    meta = [ln.strip() for ln in lines_of("FOUR", "THE METADATA")]
    header(d, 60, 180, "THE METADATA (gradle/verification-metadata.xml)", CYAN)
    fit_mono(d, 60, 250, meta[1], TEXT, 30, 1800)
    fit_mono(d, 60, 300, meta[2], GREEN, 26, 1800)
    rows = [cols(ln) for ln in lines_of("FOUR", "HOW THE CHECK") + lines_of("FOUR", "THE SETUP")]
    refusals = [resolved(r)[0][1] for h, r in sections("FOUR", "THE RESULT")[1:]]
    header(d, 60, 370, "HOW THE CHECK IS MADE (com.shop:pricing jar, first 16 hex digits)", AMBER)
    for k, t in enumerate(("jar", "version", "sha256")):
        label(d, (60, 520, 760)[k], 435, t, MUTED, 24, True)
    for i, c in enumerate(rows):
        y = 475 + i * 120
        colour = CYAN if i == 0 else ROSE
        fit_mono(d, 60, y + 30, c[0], TEXT, 30, 440, "lm")
        cell(d, 520, y, 200, 60, c[1], colour if i == 0 or "listed" in c[-1] else TEXT, 30)
        cell(d, 760, y, 520, 60, c[2], colour, 28)
        if len(c) > 3:
            cell(d, 1310, y, 550, 60, c[3], ROSE, 26)
            fit_mono(d, 760, y + 72, "the build: refused: " + refusals[i - 1], GREEN, 24, 1100)
    caption(d, "Not listed, or listed with other bytes: either way the build stops.", AMBER, 935)


def filter_subgroups(img, d, cfg):
    """Act FIVE: what includeGroup covers, what includeGroupAndSubgroups covers, and checksums written too late."""
    setup = [ln.strip() for ln in lines_of("FIVE", "THE SETUP")]
    head, how = sections("FIVE", "HOW A FILTER MATCHES")[0]
    group = re.search(r"the group (\S+?)\?", head).group(1)
    header(d, 60, 180, "DOES THE FILTER COVER %s ?" % group, AMBER)
    results = sections("FIVE", "THE RESULT")
    for i, ln in enumerate(how):
        rule, ans = cols(ln)
        y = 260 + i * 150
        colour = GREEN if ans == "yes" else ROSE
        cell(d, 60, y, 660, 60, rule, TEXT, 28)
        cell(d, 750, y, 120, 60, ans, colour, 30)
        coord, origin = resolved(results[i][1])[0]
        draw.arrow_right(d, 890, y + 30, 960, MUTED, 4)
        cell(d, 980, y, 880, 60, "%s  from %s" % (coord, origin), origin_colour(origin), 28)
    fit_mono(d, 60, 560, sections("FIVE", "THE SETUP")[0][0].split("(", 1)[1].rstrip(")") + "; " + setup[0], MUTED, 26, 1800)
    late = resolved(results[2][1])
    header(d, 60, 620, "CHECKSUMS WRITTEN AFTER THE ATTACKER PUBLISHED", ROSE)
    bad = next(r for r in late if "attacker" in r[1])
    fit_mono(d, 60, 690, "the metadata now trusts %s from %s" % bad, ROSE, 30, 1800)
    fit_mono(d, 60, 745, next(ln.strip() for ln in results[2][1] if ln.strip().startswith("<-")), AMBER, 28, 1800)
    caption(d, "A filter is only as good as its list; checksums only as good as the build that wrote them.", AMBER, 935)
