"""Pictures for the network isolation video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints from
real Docker networks and the tests check, so a picture can never disagree with the code.
"""

import math
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


def section(name, head, n=0):
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


def cols(line):
    return re.split(r"\s{2,}", line.strip())


def layout(name):
    """The networks of an act's THE LAYOUT section: (network, members, internal)."""
    head = "THE LAYOUT"
    out = []
    for ln in section(name, head):
        net, members, way = cols(ln)
        out.append((net, members.split(), way == "internal"))
    return out


def extras(name):
    for ln in section(name, "AGAINST THE DESIGN"):
        c = ln.split()
        if c[0] == "extra":
            return [tuple(p.split("-")) for p in c[2:]]
    return []


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


def node(d, x, y, name, colour, r=58):
    d.ellipse([x - r, y - r, x + r, y + r], fill=BG_CELL, outline=colour, width=5)
    d.text((x, y), name, font=font(SANS_B, 30 if len(name) <= 4 else 24), fill=colour, anchor="mm")


# Where the services sit: the front alone on the left, the rest fanned out (flat), or laid out along the intent's pairs.
FAN = {"web": (0.08, 0.5), "app": (0.75, 0.05), "db": (0.75, 0.275), "cache": (0.75, 0.5), "admin": (0.75, 0.725),
       "ci": (0.75, 0.95)}
TREE = {"web": (0.05, 0.5), "app": (0.35, 0.5), "db": (0.65, 0.12), "cache": (0.35, 0.92), "admin": (0.65, 0.85),
        "ci": (0.95, 0.85)}


def graph(d, x0, y0, w, h, names, edges, highlight=None, spot=TREE):
    """A graph of services: edges is a list of (a, b, colour, width); highlight maps a name to a node colour."""
    pos = {n: (x0 + spot[n][0] * w, y0 + spot[n][1] * h) for n in names}
    for a, b, colour, width in edges:
        d.line([pos[a], pos[b]], fill=colour, width=width)
    for n in names:
        node(d, *pos[n], n, (highlight or {}).get(n, CYAN))
    return pos


# ------------------------------------------------------------------ figures

def pairs_formula(img, d, cfg):
    """n services on one network make n x (n-1) / 2 pairs: six on one network, and two on one network."""
    flat = [cols(ln) for ln in section("ONE", "HOW MANY PAIRS") if ln.split()[0] == "flat"][0]
    names = layout("ONE")[0][1]
    n = len(names)
    header(d, 60, 190, "ONE NETWORK, %d SERVICES" % n, ROSE)
    cx, cy, r = 470, 590, 270
    pos = {m: (cx + r * math.cos(2 * math.pi * k / n - math.pi / 2), cy + r * math.sin(2 * math.pi * k / n - math.pi / 2))
           for k, m in enumerate(names)}
    for i in range(n):
        for j in range(i + 1, n):
            d.line([pos[names[i]], pos[names[j]]], fill=ROSE, width=3)
    for m in names:
        node(d, *pos[m], m, CYAN, 56)
    header(d, 1000, 190, "HOW THE PAIRS ARE COUNTED", AMBER)
    steps = ["each of the %d services" % n, "can reach the other %d:" % (n - 1), "%d x %d = %d arrows" % (n, n - 1, n * (n - 1)),
             "an arrow each way is one pair,", "so divide by 2:"]
    for k, s in enumerate(steps):
        label(d, 1000, 270 + k * 56, s, TEXT, 34)
    cell(d, 1000, 560, 820, 90, flat[-1], ROSE, 40)
    measured = section("ONE", "HOW MANY PAIRS")[-1].strip()
    label(d, 1000, 690, measured, MUTED, 32)
    wanted = re.search(r"\((\d+) pairs wanted\)", next(ln for ln in act("ONE") if ln.startswith("AGAINST"))).group(1)
    label(d, 1000, 750, "the design wants only %s" % wanted, GREEN, 32, True)
    caption(d, "Every service added to a flat network adds a pair with each one already there.", AMBER, 950)


def flat_reach(img, d, cfg):
    """The taken-over front on the flat network reaches every other service by name."""
    reached = section("ONE", "WHAT web REACHES")[0].split("<-")[0].split()
    names = layout("ONE")[0][1]
    header(d, 60, 190, "THE LAYOUT: one network, flat", CYAN)
    edges = [("web", m, ROSE, 8) for m in reached]
    graph(d, 120, 300, 1100, 560, names, edges, {"web": ROSE}, FAN)
    header(d, 1320, 190, "WHAT web REACHES", ROSE)
    for k, m in enumerate(reached):
        cell(d, 1320, 270 + k * 92, 520, 70, "web -> " + m, ROSE, 30)
    caption(d, "Taken over, the front reaches the database, the cache, the admin panel and the build server.", AMBER, 950)


def tiers_layout(img, d, cfg):
    """Three networks: the pairs each one makes, and the two that nobody asked for."""
    nets = layout("TWO")
    counts = {cols(ln)[0]: cols(ln)[-1] for ln in section("TWO", "HOW MANY PAIRS") if cols(ln)[0] in [n for n, _, _ in nets]}
    extra = extras("TWO")
    header(d, 60, 190, "THE LAYOUT", CYAN)
    xs = [60, 560, 1360]
    widths = [440, 740, 500]
    for (net, members, internal), x, w in zip(nets, xs, widths):
        panel(d, x, 260, w, 330, AMBER if internal else ROSE, 3)
        label(d, x + 24, 280, net, TEXT, 34, True)
        label(d, x + w - 24, 288, "internal" if internal else "has a way out", MUTED, 24, False, "ra")
        for k, m in enumerate(members):
            cell(d, x + 24 + (k % 2) * ((w - 60) // 2 + 12), 350 + (k // 2) * 90, (w - 60) // 2, 70, m, CYAN, 30)
        k = counts[net].split("= ")[-1]
        label(d, x + w // 2, 545, k + (" pair" if k == "1" else " pairs") + "  (" + counts[net].split(" = ")[0] + ")", AMBER, 26, True, "ma")
    header(d, 60, 640, "AGAINST THE DESIGN", ROSE)
    total = section("TWO", "HOW MANY PAIRS")[-1].strip()
    label(d, 60, 710, total, TEXT, 32)
    label(d, 60, 770, "extra, which nobody asked for:", MUTED, 30)
    for k, (a, b) in enumerate(extra):
        cell(d, 560 + k * 360, 760, 320, 64, "%s - %s" % (a, b), ROSE, 30)
    caption(d, "Everything on one network reaches everything else on it.", AMBER, 950)


def intent_links(img, d, cfg):
    """One network for each pair of the intent; the drift adds a pair nobody asked for."""
    nets = [n for n in layout("THREE") if n[0].startswith("link")]
    names = ["web", "app", "db", "cache", "admin", "ci"]
    drift = re.search(r"extra (\S+)", section("THREE", "THE MEASUREMENT AFTER")[0]).group(1).split("-")
    measured = section("THREE", "THE MEASUREMENT")[0].strip()
    header(d, 60, 190, "THE LAYOUT BUILT FROM THE INTENT", GREEN)
    edges = [(m[0], m[1], GREEN, 7) for _, m, _ in nets] + [(drift[0], drift[1], ROSE, 5)]
    pos = graph(d, 120, 300, 1100, 560, names, edges)
    for net, (a, b), _ in nets:
        (x1, y1), (x2, y2) = pos[a], pos[b]
        label(d, (x1 + x2) / 2, (y1 + y2) / 2 - 34, net, GREEN, 22, True, "ma")
    (x1, y1), (x2, y2) = pos[drift[0]], pos[drift[1]]
    label(d, (x1 + x2) / 2 + 40, (y1 + y2) / 2 - 60, "oops (drift)", ROSE, 24, True, "ma")
    header(d, 1320, 190, "THE MEASUREMENT", GREEN)
    label(d, 1320, 260, measured, TEXT, 30)
    header(d, 1320, 340, "AFTER THE DRIFT", ROSE)
    after = section("THREE", "THE MEASUREMENT AFTER")[0].split("<-")[0].strip()
    label(d, 1320, 410, after, ROSE, 30)
    label(d, 1320, 480, "web and db were joined", MUTED, 28)
    label(d, 1320, 520, "to one more network", MUTED, 28)
    caption(d, "Green: a network per allowed pair. Rose: the pair the drift added, found by measuring.", AMBER, 950)


def edge_ways(img, d, cfg):
    """The way in (published ports) and the way out (who reaches a server on the internet side)."""
    ins = [cols(ln) for ln in section("FOUR", "THE WAY IN")]
    outs = [ln.split() for ln in section("FOUR", "WHO REACHES IT")]
    header(d, 60, 190, "THE WAY IN: published to the host", CYAN)
    draw.box(d, 60, 260, 300, 120, "the host", "127.0.0.1", TEXT)
    for k, row in enumerate(ins):
        name, state = row[0], row[1]
        y = 260 + k * 92
        ok = state.startswith("published")
        cell(d, 460, y, 180, 70, name, CYAN, 30)
        cell(d, 660, y, 320, 70, state.split(",")[0], ROSE if ok else GREEN, 26)
    draw.arrow_right(d, 370, 320, 450, ROSE, 5)
    header(d, 1060, 190, "THE WAY OUT: who reaches evil:4444", ROSE)
    draw.box(d, 1560, 260, 300, 120, "evil", "on the internet side", ROSE)
    for k, row in enumerate(outs):
        name, ans = row[0], row[1]
        y = 260 + k * 92
        cell(d, 1060, y, 180, 70, name, CYAN, 30)
        cell(d, 1260, y, 180, 70, ans, ROSE if ans == "yes" else GREEN, 30)
    draw.arrow_right(d, 1450, 295, 1550, ROSE, 5)
    how = [ln.strip() for ln in section("FOUR", "HOW IT IS WORKED OUT")]
    for k, s in enumerate(how):
        label(d, 60, 840 + k * 46, s, AMBER if k == 0 else TEXT, 28, k == 0)
    caption(d, "Only the front is published, and only the front can leave: internal networks have no way out.", AMBER, 950)


def pivots(img, d, cfg):
    """How many networks each pivot is on."""
    rows = [cols(ln) for ln in section("FIVE", "THE PIVOTS") if re.match(r"\S+\s+\d+ networks", ln)]
    header(d, 60, 190, "THE PIVOTS: services on two or more networks", AMBER)
    most = max(int(r[1].split()[0]) for r in rows)
    for k, (name, count, nets) in enumerate(rows):
        y = 280 + k * 130
        n = int(count.split()[0])
        label(d, 60, y + 35, name, TEXT, 38, True, "lm")
        for j, net in enumerate(nets.split()):
            cell(d, 260 + j * 330, y, 300, 70, net, ROSE if n == most else AMBER, 30)
        label(d, 260 + n * 330 + 20, y + 35, count, MUTED, 30, False, "lm")
    caption(d, "A taken-over pivot reaches every network it is on.", AMBER, 950)
