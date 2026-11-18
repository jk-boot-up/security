"""Pictures for the attack trees video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and the
tests check, so a picture can never disagree with the code.
"""

import math
import re
from pathlib import Path

from videokit.slides.theme import AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
TOP = 205


def act(name):
    return re.search(r"^%s\. .*\n((?:(?![A-Z]+\. ).*\n?)*)" % name, OUT, re.M).group(1).splitlines()


def sections(name, head):
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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def facts(lines):
    return dict(tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip()))


def cols(ln):
    return re.split(r"\s{2,}", ln.strip())


def euros(text):
    return int(re.sub(r"[^\d]", "", text) or 0)


# ------------------------------------------------------------------ drawing helpers

def header(d, x, y, text, colour, w=None, size=24):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 42], radius=21, fill=colour)
    d.text((x + w // 2, y + 21), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=28, bold=False, anchor="la", max_w=None):
    f = font(SANS_B if bold else SANS, size)
    while max_w and f.getlength(text) > max_w and f.size > 12:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def mono(d, x, y, text, colour, size, max_w=None, anchor="la"):
    f = font(MONO_B, size)
    while max_w and f.getlength(text) > max_w and f.size > 12:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=965):
    label(d, W // 2, y, text, colour, 30, True, "ma")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def line_arrow(d, x1, y1, x2, y2, colour, width=3):
    d.line([(x1, y1), (x2, y2)], fill=colour, width=width)
    a = math.atan2(y2 - y1, x2 - x1)
    d.polygon([(x2, y2), (x2 - 16 * math.cos(a) + 9 * math.sin(a), y2 - 16 * math.sin(a) - 9 * math.cos(a)),
               (x2 - 16 * math.cos(a) - 9 * math.sin(a), y2 - 16 * math.sin(a) + 9 * math.cos(a))], fill=colour)


# ------------------------------------------------------------------ the tree, parsed from act ONE

def tree():
    """The tree as nested dicts, from the indented lines of act ONE."""
    lines = [(0, ln) for ln in section("ONE", "THE GOAL")]
    lines += [(1, ln) for ln in section("ONE", "THE FIRST BRANCH") + section("ONE", "THE OTHER BRANCHES")]
    root, stack = None, []
    for extra, ln in lines:
        depth = (len(ln) - len(ln.lstrip())) // 2 + extra
        text = ln.strip()
        m = re.match(r"(AND|OR)  (.*)", text)
        if m:
            node = {"kind": m.group(1), "name": m.group(2), "children": []}
        else:
            p = cols(text)
            node = {"kind": "leaf", "id": p[0], "cost": p[1], "p": p[2], "name": p[3]}
        if depth == 0:
            root, stack = node, [node]
            continue
        stack = stack[:depth]
        stack[-1]["children"].append(node)
        stack.append(node)
    return root


def leaves(n):
    return [n] if n["kind"] == "leaf" else [x for c in n["children"] for x in leaves(c)]


# ------------------------------------------------------------------ figures

def bank_tree(img, d, cfg):
    """The whole tree: gates above, the fourteen steps at the bottom with cost and chance."""
    root = tree()
    all_leaves = leaves(root)
    lw, gap = 124, 6
    x0 = (W - len(all_leaves) * (lw + gap)) // 2
    pos = {}
    for i, lf in enumerate(all_leaves):
        pos[id(lf)] = x0 + i * (lw + gap) + lw // 2
    ys = [TOP + 30, TOP + 205, TOP + 380]
    leaf_y = TOP + 560
    cheapest = section("TWO", "THE RESULT")[0]
    cheap_ids = re.search(r"\[(.*)\]", cheapest).group(1).split(", ")

    def place(n, depth):
        if n["kind"] == "leaf":
            return pos[id(n)], leaf_y
        xs = [place(c, depth + 1)[0] for c in n["children"]]
        cx, cy = (min(xs) + max(xs)) // 2, ys[min(depth, 2)]
        pos[id(n)] = cx
        n["xy"] = (cx, cy)
        return cx, cy

    place(root, 0)

    def draw_lines(n):
        if n["kind"] == "leaf":
            return
        cx, cy = n["xy"]
        for c in n["children"]:
            tx, ty = (pos[id(c)], leaf_y) if c["kind"] == "leaf" else c["xy"]
            d.line([(cx, cy + 40), (tx, ty - (60 if c["kind"] == "leaf" else 40))], fill=LINE, width=3)
            draw_lines(c)

    def draw_box(n):
        if n["kind"] == "leaf":
            return
        cx, cy = n["xy"]
        span = [pos[id(x)] for x in leaves(n)]
        w = max(240, min(560, max(span) - min(span) + lw))
        colour = CYAN if n["kind"] == "OR" else AMBER
        d.rounded_rectangle([cx - w // 2, cy - 40, cx + w // 2, cy + 40], radius=14, fill=BG_CELL, outline=colour, width=3)
        f = font(SANS_B, 26)
        if f.getlength(n["name"]) <= w - 20:
            label(d, cx, cy - 32, n["kind"], colour, 24, True, "ma")
            label(d, cx, cy - 2, n["name"], TEXT, 26, True, "ma")
        else:
            words = n["name"].split()
            half = (len(words) + 1) // 2
            label(d, cx, cy - 36, n["kind"], colour, 20, True, "ma")
            label(d, cx, cy - 12, " ".join(words[:half]), TEXT, 20, True, "ma", w - 16)
            label(d, cx, cy + 12, " ".join(words[half:]), TEXT, 20, True, "ma", w - 16)
        for c in n["children"]:
            draw_box(c)

    draw_lines(root)
    draw_box(root)
    for lf in all_leaves:
        cx = pos[id(lf)]
        colour = ROSE if lf["id"] in cheap_ids else GREEN
        d.rounded_rectangle([cx - lw // 2, leaf_y - 60, cx + lw // 2, leaf_y + 120], radius=12, fill=BG_CELL, outline=colour, width=4 if lf["id"] in cheap_ids else 2)
        label(d, cx, leaf_y - 46, lf["id"], colour, 28, True, "ma")
        label(d, cx, leaf_y + 0, lf["cost"].replace(" EUR", ""), TEXT, 26, True, "ma", lw - 10)
        label(d, cx, leaf_y + 34, "EUR", MUTED, 18, False, "ma")
        label(d, cx, leaf_y + 66, lf["p"], AMBER, 28, True, "ma")
    label(d, 60, leaf_y + 135, "each step: cost to the attacker, chance it works.  Cyan: OR, any child.  Amber: AND, every child.", MUTED, 26)
    caption(d, "The cheapest way in is %s: %s." % (", ".join(cheap_ids), next(l["name"] for l in all_leaves if l["id"] == cheap_ids[0])), ROSE, 965)


def two_gates(img, d, cfg):
    """The core idea: one AND gate and one OR gate worked out with the real numbers."""
    and_head = heading("TWO", "AN AND GATE")
    and_name = re.search(r"\((.*)\)", and_head).group(1)
    a = facts(section("TWO", "AN AND GATE"))
    phish = next(c for c in tree()["children"][0]["children"] if c["name"] == and_name)
    or_lines = section("TWO", "AN OR GATE")
    or_name = re.search(r"\((.*)\)", heading("TWO", "AN OR GATE")).group(1)
    rows = [cols(ln) for ln in or_lines if ln.startswith("  ") and len(cols(ln)) == 3]
    o = facts(or_lines)
    chance_end = next(ln for ln in or_lines if ln.strip().startswith("= ")).strip()

    panel(d, 60, TOP + 20, 780, 700, AMBER, 3)
    header(d, 80, TOP, "AND: EVERY STEP IS NEEDED", AMBER)
    label(d, 90, TOP + 70, and_name, TEXT, 30, True, max_w=720)
    y = TOP + 130
    for lf in phish["children"]:
        d.rounded_rectangle([90, y, 810, y + 116], radius=12, fill=BG_CELL, outline=GREEN, width=2)
        label(d, 110, y + 14, lf["id"], GREEN, 30, True)
        label(d, 210, y + 16, lf["name"], TEXT, 28, max_w=580)
        mono(d, 110, y + 64, "%s   %s" % (lf["cost"], lf["p"]), AMBER, 32)
        y += 136
    label(d, 90, y + 20, "costs add", MUTED, 28)
    mono(d, 90, y + 62, a["cost"], TEXT, 40, 720)
    label(d, 90, y + 140, "chances multiply", MUTED, 28)
    mono(d, 90, y + 182, a["chance"], TEXT, 40, 720)

    panel(d, 900, TOP + 20, 960, 700, CYAN, 3)
    header(d, 920, TOP, "OR: ANY ONE WILL DO", CYAN)
    label(d, 930, TOP + 70, or_name + ", over its four AND gates", TEXT, 30, True, max_w=900)
    y = TOP + 125
    for name, cost, p in rows:
        label(d, 930, y, name, TEXT, 28, max_w=540)
        mono(d, 1680, y, cost, AMBER, 30, None, "ra")
        mono(d, 1830, y, p, AMBER, 30, None, "ra")
        y += 60
    label(d, 930, y + 20, "the cheapest child wins", MUTED, 28)
    mono(d, 930, y + 62, o["cost"], TEXT, 36, 900)
    label(d, 930, y + 140, "some child works: 1 minus none works", MUTED, 28)
    mono(d, 930, y + 182, o["chance"], TEXT, 34, 900)
    mono(d, 930, y + 240, chance_end, GREEN, 44)
    caption(d, "AND: costs add, chances multiply. OR: the cheapest wins, chances combine.", AMBER, 965)


def root_branches(img, d, cfg):
    """The four branches of the root: what each costs and how likely it is, and the result."""
    rows = [cols(ln) for ln in section("TWO", "THE ROOT")]
    res = facts(section("TWO", "THE RESULT"))
    header(d, 60, TOP, "THE FOUR WAYS IN, EACH WORKED OUT LIKE THE GATES BEFORE", CYAN)
    cheapest = min(euros(r[1]) for r in rows)
    top_cost = max(euros(r[1]) for r in rows)
    for i, (name, cost, p) in enumerate(rows):
        y = TOP + 80 + i * 125
        colour = ROSE if euros(cost) == cheapest else MUTED
        label(d, 60, y + 10, name, TEXT, 30, True, max_w=600)
        w = max(14, int(math.log10(max(euros(cost), 1) * 10) / math.log10(top_cost * 10) * 560))
        d.rounded_rectangle([680, y + 4, 680 + w, y + 54], radius=8, fill=colour)
        label(d, 680 + w + 16, y + 10, cost, colour, 30, True)
        pw = int(int(p.rstrip("%")) / 100 * 360)
        d.rounded_rectangle([1460, y + 4, 1820, y + 54], radius=8, outline=LINE, width=2, fill=BG_CELL)
        d.rounded_rectangle([1460, y + 4, 1460 + max(pw, 8), y + 54], radius=8, fill=AMBER)
        label(d, 1460, y + 62, "works " + p, AMBER, 24, True)
    label(d, 680, TOP + 50, "cost to the attacker (log scale)", MUTED, 22)
    label(d, 1460, TOP + 50, "chance it works", MUTED, 22)
    panel(d, 60, TOP + 590, 1800, 120, ROSE, 3)
    label(d, 100, TOP + 615, "cheapest way in: " + res["cheapest way in"], ROSE, 34, True)
    label(d, 100, TOP + 665, "some way works: " + res["some way works"], AMBER, 30, True)
    caption(d, "The attacker does not need the hard way: one missing check costs 50 EUR.", AMBER, 965)


def one_defence(img, d, cfg):
    """Each defence alone, and OWN worked out on the transfer branch."""
    alone = [cols(ln) for ln in section("THREE", "EACH ALONE")]
    prices = {cols(ln)[0]: cols(ln)[1] for ln in section("THREE", "THE DEFENCES")}
    before = facts(section("THREE", "BEFORE THE DEFENCE"))
    after_head = heading("THREE", "AFTER THE DEFENCE")
    after = facts(section("THREE", "AFTER THE DEFENCE"))
    header(d, 60, TOP, "EACH DEFENCE ALONE: THE CHEAPEST WAY AFTER IT", CYAN)
    for i, (did, cost, path, p) in enumerate(alone):
        y = TOP + 70 + i * 82
        good = euros(cost) > min(euros(r[1]) for r in alone)
        colour = GREEN if good else ROSE
        panel(d, 60, y, 960, 68, colour, 2)
        label(d, 80, y + 34, did, colour, 28, True, "lm")
        label(d, 240, y + 34, prices[did], MUTED, 24, False, "lm")
        label(d, 520, y + 34, cost, colour, 28, True, "lm")
        mono(d, 720, y + 34, path, TEXT, 24, 200, "lm")
        label(d, 1000, y + 34, p, AMBER, 26, True, "rm")
    x = 1080
    panel(d, x, TOP + 20, 780, 300, ROSE, 3)
    header(d, x + 20, TOP, "BEFORE THE DEFENCE", ROSE)
    label(d, x + 30, TOP + 80, re.search(r"\((.*)\)", heading("THREE", "BEFORE THE DEFENCE")).group(1), TEXT, 34, True)
    mono(d, x + 30, TOP + 160, before["cheapest"], ROSE, 34, 720)
    panel(d, x, TOP + 380, 780, 330, GREEN, 3)
    header(d, x + 20, TOP + 360, "AFTER THE DEFENCE", GREEN)
    label(d, x + 30, TOP + 440, re.search(r"\((.*)\)", after_head).group(1), TEXT, 34, True)
    mono(d, x + 30, TOP + 520, after["left"], TEXT, 30, 720)
    mono(d, x + 30, TOP + 600, after["cheapest"], GREEN, 32, 720)
    caption(d, "Only the defence on the cheapest path moves it; the others leave 50 EUR.", AMBER, 965)


def plan_steps(img, d, cfg):
    """The plan: the cheapest way after each step against the target, with the chance and the failing abuse cases."""
    rows = [cols(ln) for ln in section("FOUR", "THE STEPS") if re.match(r"\s*\d", ln)]
    target = euros(section("FOUR", "THE TARGET")[0])
    header(d, 60, TOP, "CUT THE CHEAPEST PATH, THEN LOOK AGAIN", CYAN)
    base_y, top_y = TOP + 570, TOP + 110
    top_cost = max(target, max(euros(r[3]) for r in rows)) * 1.15

    def ypos(v):
        return base_y - int(v / top_cost * (base_y - top_y))

    ty = ypos(target)
    for x0 in range(140, 1840, 30):
        d.line([(x0, ty), (x0 + 15, ty)], fill=GREEN, width=3)
    label(d, 150, ty - 40, "target: at least " + section("FOUR", "THE TARGET")[0].split("least ")[-1], GREEN, 28, True)
    bw = 300
    for i, r in enumerate(rows):
        step, did, spent, cost, via, p, fail = r
        x = 150 + i * 340
        v = euros(cost)
        colour = GREEN if v >= target else ROSE if i == 0 else AMBER
        d.rounded_rectangle([x, ypos(v), x + bw, base_y], radius=10, fill=colour)
        label(d, x + bw // 2, ypos(v) - 44, cost, colour, 30, True, "ma")
        label(d, x + bw // 2, base_y + 14, did, TEXT, 30, True, "ma")
        label(d, x + bw // 2, base_y + 54, "spent " + spent, MUTED, 26, False, "ma")
        label(d, x + bw // 2, base_y + 88, "via " + via + ",  p " + p, MUTED, 26, False, "ma")
        label(d, x + bw // 2, base_y + 122, "failing " + fail, ROSE if fail.split("/")[0] != "0" else GREEN, 26, True, "ma")
    caption(d, "Each defence moves the cheapest path somewhere else; four steps reach the target.", AMBER, 975)


def abuse_grid(img, d, cfg):
    """Eight abuse cases before and after the plan, as test results."""
    before = [cols(ln) for ln in section("FIVE", "BEFORE THE PLAN")]
    after = {r[0]: r[1] for r in (cols(ln) for ln in section("FIVE", "AFTER THE PLAN"))}
    res = facts(section("FIVE", "THE RESULT"))
    on = re.search(r"on: (.*)\)", heading("FIVE", "AFTER THE PLAN")).group(1)
    header(d, 60, TOP, "THE ABUSE CASES AS TESTS", CYAN)
    label(d, 1360, TOP + 10, "BEFORE", ROSE, 26, True, "ma")
    label(d, 1680, TOP + 10, "AFTER", GREEN, 26, True, "ma")
    for i, (cid, result, story) in enumerate(before):
        y = TOP + 60 + i * 76
        panel(d, 60, y, 1800, 64, LINE, 2)
        label(d, 80, y + 32, cid, CYAN, 28, True, "lm")
        label(d, 180, y + 32, story, TEXT, 26, False, "lm", 1000)
        for x, r in ((1360, result), (1680, after[cid])):
            c = GREEN if r == "PASSES" else ROSE
            d.rounded_rectangle([x - 110, y + 10, x + 110, y + 54], radius=22, fill=c)
            label(d, x, y + 32, r, INK, 24, True, "mm")
    label(d, 60, TOP + 680, "after the plan, on: " + on, GREEN, 26, True)
    label(d, 60, TOP + 720, "steps with a test: %s;  without one: %s" % (res["steps with a test"], res["without one"]), MUTED, 26)
    caption(d, "A test that fails today is a defence still missing; green when it is built.", AMBER, 975)
