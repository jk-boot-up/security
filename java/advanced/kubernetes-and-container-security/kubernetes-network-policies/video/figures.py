"""Pictures for the Kubernetes network policies video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
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
    """Every section of an act whose header starts with `head`, as lists of lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found


def section(name, head, n=0):
    return sections(name, head)[n]


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


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


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


# ------------------------------------------------------------------ values

def pods():
    """The pods as (id, name, app) from act ONE."""
    rows = [re.split(r"\s+", ln.strip(), maxsplit=2) for ln in section("ONE", "THE PODS")]
    return [(r[0], r[0].split("/")[1], r[1]) for r in rows]


def design():
    return [ln.strip() for ln in section("ONE", "THE DESIGN")]


def matrix(lines):
    """A printed matrix as the set of reachable pairs 'ns/a -> ns/b'."""
    ids = {name: pid for pid, name, _ in pods()}
    cols = lines[0].split()[1:]
    reach = set()
    for ln in lines[1:]:
        parts = ln.split()
        for to, mark in zip(cols, parts[1:]):
            if mark == "o":
                reach.add("%s -> %s" % (ids[parts[0]], ids[to]))
    return reach


def against(lines):
    """reachable, total, wanted, unwanted and the unwanted and missing pairs from AGAINST THE DESIGN lines."""
    m = re.search(r"reachable\s+(\d+) of (\d+) = (\d+) wanted \+ (\d+) unwanted", lines[0])
    lists, key = {"unwanted": [], "missing": []}, None
    for ln in lines[1:]:
        k = re.match(r"(unwanted|missing)\b(?: \d+)?\s*(.*)", ln.strip())
        if k:
            key = k.group(1)
            rest = k.group(2)
        else:
            rest = ln.strip()
        if key and "->" in rest:
            lists[key].append(rest)
    return dict(reachable=int(m.group(1)), total=int(m.group(2)), wanted=int(m.group(3)),
                unwanted=lists["unwanted"], missing=lists["missing"])


def reach_of(a):
    """The reachable pairs implied by a comparison: the design, less what is missing, plus what is unwanted."""
    return (set(design()) - set(a["missing"])) | set(a["unwanted"])


def draw_matrix(d, x, y, reach, size=88, show_names=True):
    """A pods x pods grid: green a wanted open pair, rose an unwanted one, amber a design pair that is closed."""
    ps = pods()
    want = set(design())
    for j, (_, name, _) in enumerate(ps):
        if show_names:
            label(d, x + (j + 1) * size + size // 2, y + size // 2, name, MUTED, 22, True, "mm")
    for i, (fid, fname, _) in enumerate(ps):
        label(d, x + size - 10, y + (i + 1) * size + size // 2, fname, MUTED, 22, True, "rm")
        for j, (tid, _, _) in enumerate(ps):
            cx, cy = x + (j + 1) * size, y + (i + 1) * size
            pair = "%s -> %s" % (fid, tid)
            if fid == tid:
                colour, fill, text = LINE, PANEL, ""
            elif pair in reach:
                colour, fill, text = (GREEN, (20, 70, 58), "o") if pair in want else (ROSE, (90, 30, 45), "o")
            else:
                colour, fill, text = (AMBER, BG_CELL, "?") if pair in want else (LINE, BG_CELL, ".")
            d.rounded_rectangle([cx + 4, cy + 4, cx + size - 4, cy + size - 4], radius=8, fill=fill, outline=colour, width=3)
            d.text((cx + size // 2, cy + size // 2), text, font=font(MONO_B, size // 3), fill=colour, anchor="mm")
    return x + 6 * size, y + 6 * size


def legend(d, x, y):
    for k, (text, colour) in enumerate((("wanted, open", GREEN), ("unwanted, open", ROSE),
                                         ("wanted, closed", AMBER), ("closed", LINE))):
        d.rounded_rectangle([x, y + k * 50, x + 34, y + k * 50 + 34], radius=6, fill=BG_CELL, outline=colour, width=3)
        label(d, x + 50, y + k * 50 + 17, text, colour if colour != LINE else MUTED, 24, True, "lm")


# ------------------------------------------------------------------ figures

def allow_anatomy(img, d, cfg):
    """One allow, line by line: which pods it protects, who may call them, and on which port."""
    lines = section("TWO", "ONE ALLOW")
    name = re.search(r"\((.*)\)", heading_of("TWO", "ONE ALLOW")).group(1)
    header(d, 60, 170, "ONE ALLOW: " + name, CYAN)
    colour, part = TEXT, {}
    for i, ln in enumerate(lines):
        s = ln.strip()
        indent = len(ln) - len(ln.lstrip())
        if s == "podSelector:" and indent == 2:
            colour = CYAN
        elif s.startswith("- namespaceSelector") or (s == "podSelector:" and indent > 2):
            colour = AMBER
        elif s.startswith("ports:"):
            colour = GREEN
        elif s.startswith(("policyTypes", "ingress", "- from", "spec", "metadata", "kind", "apiVersion", "name:")):
            colour = TEXT
        if "app:" in s or "metadata.name" in s or "port:" in s:
            part.setdefault(colour, []).append(re.search(r"(?:app|metadata\.name|port): (\w+)", s).group(1))
        fit_mono(d, 80, 230 + i * 40, ln, colour if colour != TEXT else MUTED, 26, 1020)
        if s.startswith("ports:"):
            colour = TEXT
    ns = re.search(r"namespace: (\w+)", "\n".join(lines)).group(1)
    x0, y = 1160, 230
    boxes = [("WHICH PODS IT PROTECTS", CYAN, ["namespace %s" % ns, "label app=%s" % part[CYAN][0]]),
             ("WHO MAY CALL THEM (both must hold)", AMBER, ["namespace %s" % part[AMBER][0], "AND label app=%s" % part[AMBER][1]]),
             ("ON WHICH PORT", GREEN, ["port %s" % part[GREEN][0]])]
    for title, c, rows in boxes:
        header(d, x0, y, title, c, 700, 24)
        for k, r in enumerate(rows):
            fit_mono(d, x0 + 20, y + 62 + k * 44, r, TEXT, 28, 660)
        y += 80 + 44 * len(rows) + 40
    label(d, x0, y, "Everything else stays denied:", MUTED, 26, True)
    label(d, x0, y + 40, "the default deny already selects the pod.", MUTED, 26)
    caption(d, "A policy selects pods by label; a selected pod takes only what an allow lists.", AMBER, 950)


def flat_matrix(img, d, cfg):
    """Act ONE: the measured matrix with no policy, and how 20 = 3 + 17 is made."""
    reach = matrix(section("ONE", "THE MEASUREMENT"))
    header(d, 60, 170, "THE MEASUREMENT: NO POLICY (row calls column)", CYAN)
    draw_matrix(d, 60, 230, reach, 104)
    a = against(section("ONE", "AGAINST THE DESIGN"))
    x = 1120
    header(d, x, 250, "THE DESIGN", GREEN)
    for k, pair in enumerate(design()):
        fit_mono(d, x, 320 + k * 46, pair, GREEN, 28, 760)
    count = section("ONE", "HOW MANY PAIRS")[0].strip()
    header(d, x, 500, "THE COUNT", AMBER)
    fit_mono(d, x, 570, count, TEXT, 28, 760)
    fit_mono(d, x, 620, "%d reachable = %d wanted + %d unwanted" % (a["reachable"], a["wanted"], a["reachable"] - a["wanted"]),
             ROSE, 28, 760)
    legend(d, x, 700)
    caption(d, "With no policy every pod reaches every other pod: %d of %d pairs." % (a["reachable"], a["total"]), AMBER, 950)


def deny_then_allow(img, d, cfg):
    """Act TWO: after the default deny, then after the three allows."""
    step1 = against(section("TWO", "STEP 1, THE MEASUREMENT"))
    step2 = matrix(section("TWO", "STEP 2, THE MEASUREMENT"))
    a2 = against(section("TWO", "AGAINST THE DESIGN"))
    header(d, 60, 170, "STEP 1: DEFAULT DENY", ROSE)
    draw_matrix(d, 40, 230, reach_of(step1), 84)
    fit_mono(d, 60, 760, "%d of %d reachable, %d missing" % (step1["reachable"], step1["total"], len(step1["missing"])),
             AMBER, 28, 820)
    draw.arrow_right(d, 900, 480, 1000, MUTED, 5)
    header(d, 1010, 170, "STEP 2: ONE ALLOW PER LINE OF THE DESIGN", GREEN)
    draw_matrix(d, 1000, 230, step2, 84)
    equal = section("TWO", "AGAINST THE DESIGN")[-1].strip()
    fit_mono(d, 1020, 760, "%d of %d reachable; %s" % (a2["reachable"], a2["total"], equal), GREEN, 28, 860)
    caption(d, "The deny closes the design's pairs too; each allow opens exactly one.", AMBER, 950)


def three_mistakes(img, d, cfg):
    """Act THREE: each mistake's policy and the grid it gives."""
    for n in range(3):
        pol = section("THREE", "MISTAKE %d, THE POLICY" % (n + 1))
        a = against(section("THREE", "MISTAKE %d, THE MEASUREMENT" % (n + 1)))
        x = 40 + n * 625
        name = pol[0].split()[0]
        header(d, x, 170, "MISTAKE %d" % (n + 1), ROSE if a["unwanted"] else AMBER)
        fit_mono(d, x, 235, name, TEXT, 24, 590)
        sel = next(ln.strip() for ln in pol if ln.strip().startswith("selects"))
        fit_mono(d, x, 275, sel, MUTED, 22, 590)
        draw_matrix(d, x - 20, 320, reach_of(a), 92 if n else 92, True)
        res = "%d unwanted" % len(a["unwanted"]) if a["unwanted"] else "%d missing" % len(a["missing"])
        fit_mono(d, x, 880, "%d of %d reachable, %s" % (a["reachable"], a["total"], res),
                 ROSE if a["unwanted"] else AMBER, 26, 590)
    caption(d, "The API server said 'created' to all three; only the measurement shows them.", AMBER, 950)


def egress_dns(img, d, cfg):
    """Act FOUR: what the api reaches before egress rules, after them, and after DNS is allowed."""
    def probes(lines):
        return [re.split(r"\s{2,}", ln.strip()) for ln in lines if "->" in ln and len(re.split(r"\s{2,}", ln.strip())) == 2]
    stages = [("BEFORE", probes(section("FOUR", "BEFORE"))),
              ("AFTER THE EGRESS DENY", probes(section("FOUR", "AFTER:"))),
              ("AFTER ALLOWING DNS", probes(section("FOUR", "AFTER ALLOWING DNS")))]
    for k, (title, rows) in enumerate(stages):
        x = 50 + k * 620
        header(d, x, 170, title, (CYAN, ROSE, GREEN)[k], 580, 24)
        for i, (what, result) in enumerate(rows):
            y = 250 + i * 120
            colour = GREEN if result == "answered" else ROSE
            fit_mono(d, x, y, what, TEXT, 26, 580)
            cell(d, x, y + 40, 260, 56, result, colour, 26)
    why = section("FOUR", "WHY THE NAME FAILS")
    header(d, 50, 640, "WHY THE NAME FAILS", AMBER)
    for i, ln in enumerate(why):
        fit_mono(d, 50, 705 + i * 46, ln.strip(), TEXT, 26, 1800)
    caption(d, "A name is looked up before the call: an egress deny needs an allow for DNS.", AMBER, 950)


def label_credential(img, d, cfg):
    """Act FIVE: the jobs pod's label before and after, the allow's two questions, and the pair it opens."""
    before = section("FIVE", "BEFORE")
    after = section("FIVE", "AFTER:")
    a = against(after[1:])
    header(d, 60, 170, "BEFORE", CYAN)
    fit_mono(d, 60, 240, before[0].strip(), TEXT, 32, 800)
    header(d, 60, 320, "THE CHANGE", AMBER)
    fit_mono(d, 60, 390, section("FIVE", "THE CHANGE")[0].strip(), AMBER, 26, 1000)
    header(d, 60, 470, "AFTER", ROSE)
    fit_mono(d, 60, 540, after[0].strip(), ROSE, 32, 800)
    why = section("FIVE", "WHY")
    header(d, 60, 630, heading_of("FIVE", "WHY"), GREEN)
    for i, ln in enumerate(why):
        fit_mono(d, 60, 700 + i * 50, ln.strip(), TEXT, 28, 1000)
    draw_matrix(d, 1150, 160, reach_of(a), 100)
    caption(d, "%d of %d reachable, %d unwanted: %s" % (a["reachable"], a["total"], len(a["unwanted"]),
                                                          ", ".join(a["unwanted"])), ROSE, 950)
