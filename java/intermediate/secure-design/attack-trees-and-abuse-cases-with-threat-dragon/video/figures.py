"""Pictures for the Threat Dragon video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code. Only the positions
of the cells in the diagram picture are chosen here.
"""

import math
import re
from pathlib import Path

from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
SEV = {"High": ROSE, "Medium": AMBER, "Low": GREEN}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def section(name, head):
    """The lines under the act's section whose header starts with `head` (two spaces removed)."""
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


def cols(ln):
    return re.split(r"\s{2,}", ln.strip())


def header(d, x, y, text, colour, size=26):
    f = font(SANS_B, size)
    w = int(f.getlength(text)) + 44
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
    label(d, W // 2, y, text, colour, 30, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None, anchor="mm", fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    tx = x + w // 2 if anchor == "mm" else x + 16
    fit_mono(d, tx, y + h // 2, text, text_colour or colour, size, w - 24, anchor)


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def dashed_box(d, x, y, w, h, colour, dash=16):
    for (a, b) in (((x, y), (x + w, y)), ((x + w, y), (x + w, y + h)), ((x + w, y + h), (x, y + h)), ((x, y + h), (x, y))):
        (x1, y1), (x2, y2) = a, b
        n = max(1, int(math.hypot(x2 - x1, y2 - y1) // dash))
        for k in range(0, n, 2):
            t1, t2 = k / n, min(1, (k + 1) / n)
            d.line([x1 + (x2 - x1) * t1, y1 + (y2 - y1) * t1, x1 + (x2 - x1) * t2, y1 + (y2 - y1) * t2], fill=colour, width=3)


def arrow(d, a, b, colour, width=4):
    (x1, y1), (x2, y2) = a, b
    d.line([a, b], fill=colour, width=width)
    ang = math.atan2(y2 - y1, x2 - x1)
    d.polygon([(x2, y2), (x2 - 22 * math.cos(ang - 0.4), y2 - 22 * math.sin(ang - 0.4)),
               (x2 - 22 * math.cos(ang + 0.4), y2 - 22 * math.sin(ang + 0.4))], fill=colour)


def threats():
    """id -> (cell, type, status) from act ONE."""
    return {c[0]: (c[1], c[2], c[3]) for c in (cols(ln) for ln in section("ONE", "THE RESULT"))}


def scored():
    """id -> (cost, chance, severity, because) from act TWO."""
    out = {}
    for ln in section("TWO", "THE RESULT: ALL"):
        m = re.match(r"(\S+)\s+([\d,]+) EUR\s+(\d+)%\s+(\S+)\s+(.+)", ln.strip())
        out[m.group(1)] = (int(m.group(2).replace(",", "")), int(m.group(3)), m.group(4), m.group(5))
    return out


# ------------------------------------------------------------------ figures

def one_case(img, d, cfg):
    """The core idea on one example: abuse case AC1, from two steps of the tree to a threat in the file."""
    steps = section("TWO", "THE INPUT")
    work = {c[0]: c[1] for c in (cols(ln) for ln in section("TWO", "HOW THE SCORE"))}
    case = re.search(r"FOR (\S+)", heading("TWO", "THE INPUT")).group(1)
    held = [ln.strip() for ln in section("TWO", "THE RESULT FOR")]
    place = threats()[case]
    after = next(ln.strip() for ln in section("FOUR", "BEFORE THE PLAN") if ln.strip().startswith(case))
    header(d, 60, 170, "THE INPUT: %s IN THE TREE, AN AND OF TWO STEPS" % case, CYAN)
    cell(d, 60, 240, 300, 70, "AND", AMBER, 34)
    for k, ln in enumerate(steps):
        sid, cost, chance, what = re.match(r"(\S+)\s+([\d,]+ EUR)\s+(\d+%)\s+(.+)", ln.strip()).groups()
        y = 250 + k * 150
        arrow(d, (210, 310), (390, y + 40), AMBER, 3)
        cell(d, 400, y, 380, 76, "%s  %s  %s" % (sid, cost, chance), TEXT, 30)
        fit_mono(d, 400, y + 92, what, MUTED, 26, 740)
    header(d, 60, 590, "HOW THE SCORE IS WORKED OUT", AMBER)
    for k, key in enumerate(("cost", "chance", "severity")):
        label(d, 80, 664 + k * 76, key, MUTED, 34, True)
        fit_mono(d, 300, 666 + k * 76, work[key], TEXT, 34, 820)
    header(d, 1180, 170, "THE RESULT: A THREAT IN THE FILE", GREEN)
    panel(d, 1180, 240, 680, 400, GREEN, 3)
    label(d, 1210, 262, "on the cell: %s" % place[0], TEXT, 30, True)
    for k, ln in enumerate(held):
        fit_mono(d, 1210, 320 + k * 70, ln, SEV.get(ln.split('"')[3], CYAN) if "severity" in ln else CYAN, 28, 630)
    header(d, 1180, 680, "AFTER THE PLAN", GREEN)
    fit_mono(d, 1210, 750, re.sub(r"\s+", " ", after.split(None, 1)[1]), GREEN, 30, 650)
    caption(d, "The tree does the arithmetic; the file holds the result, pinned to a cell.", AMBER, 950)


POS = {"Ana (customer)": (260, 400), "Phone carrier": (260, 800), "Web app": (760, 400),
       "Transfer API": (1330, 400), "Approval console": (1330, 800), "Accounts database": (1690, 620)}
BOX_W, BOX_H = 280, 84


def _edge(c, other):
    (x, y), (ox, oy) = c, other
    dx, dy = ox - x, oy - y
    s = min(BOX_W / 2 / abs(dx) if dx else 9e9, BOX_H / 2 / abs(dy) if dy else 9e9)
    return x + dx * s, y + dy * s


def diagram(img, d, cfg):
    """The 13 cells of the file, with each abuse case's threat pinned where it hits."""
    rows = [(ln[:11].strip(), ln[12:29].strip(), ln[30:].strip()) for ln in section("ONE", "THE DIAGRAM")]
    cases = threats()
    on = {}
    for cid, (where, _, _) in cases.items():
        on.setdefault(where, []).append(cid)
    zones = {}
    for kind, name, where in rows:
        if where.startswith("in "):
            zones.setdefault(where[3:], []).append(name)
    for zone, names in zones.items():
        xs = [POS[n][0] for n in names]
        ys = [POS[n][1] for n in names]
        x0, x1 = min(xs) - BOX_W // 2 - 40, max(xs) + BOX_W // 2 + 40
        if zone != "Internet":
            x1 = max(x1, 1880)
        dashed_box(d, x0, 240, x1 - x0, 680, ROSE)
        label(d, x0 + 20, 250, zone + " (trust boundary)", ROSE, 24, True)
    tags = []
    for kind, name, where in rows:
        if kind == "tm.Flow":
            a, b = [s.strip() for s in where.split("->")]
            p, q = _edge(POS[a], POS[b]), _edge(POS[b], POS[a])
            hit = on.get(name)
            arrow(d, p, q, ROSE if hit else MUTED, 4)
            mx, my = (p[0] + q[0]) / 2, (p[1] + q[1]) / 2
            if abs(p[1] - q[1]) < 5:
                my -= 34
            tags.append((mx, my, name + ("  " + " ".join(hit) if hit else ""), ROSE if hit else MUTED))
    colour = {"tm.Actor": AMBER, "tm.Process": CYAN, "tm.Store": GREEN}
    for kind, name, where in rows:
        if kind in colour:
            x, y = POS[name]
            cell(d, x - BOX_W // 2, y - BOX_H // 2, BOX_W, BOX_H, name, colour[kind], 26)
            label(d, x - BOX_W // 2, y - BOX_H // 2 - 6, kind, MUTED, 20, False, "ld")
            if on.get(name):
                label(d, x, y + BOX_H // 2 + 8, " ".join(on[name]), ROSE, 26, True, "ma")
    for mx, my, text, c in tags:
        w = font(SANS_B, 24).getlength(text) + 20
        d.rounded_rectangle([mx - w / 2, my - 19, mx + w / 2, my + 19], radius=8, fill=INK)
        label(d, mx, my, text, c, 24, True, "mm")
    caption(d, "%s: %d cells; rose = a threat sits there." % (heading("ONE", "THE FILE").split(":")[0].title(), len(rows)), AMBER, 950)


def severity_map(img, d, cfg):
    """The eight cases by cost and chance, and the rule that turns them into a severity."""
    s = scored()
    x0, x1, y0, y1 = 200, 1300, 230, 880

    def px(cost):
        return x0 + (math.log10(max(cost, 10)) - 1) / (math.log10(50000) - 1) * (x1 - x0)

    def py(chance):
        return y1 - chance / 100 * (y1 - y0)
    d.rectangle([x0, y0, px(5000), y1], fill=(70, 55, 25))
    d.rectangle([px(5000), y0, x1, y1], fill=(25, 60, 50))
    d.rectangle([x0, y0, px(500), y1], fill=(80, 30, 45))
    d.rectangle([x0, y0, x1, py(50)], fill=(80, 30, 45))
    for cost in (10, 100, 500, 1000, 5000, 20000):
        label(d, px(cost), y1 + 10, "{:,}".format(cost), MUTED, 22, False, "ma")
    label(d, (x0 + x1) / 2, y1 + 44, "cost to the attacker, EUR (log scale)", MUTED, 24, True, "ma")
    for p in (0, 25, 50, 75, 100):
        label(d, x0 - 12, py(p), "%d%%" % p, MUTED, 22, False, "rm")
    label(d, 60, 190, "chance", MUTED, 24, True)
    for cid, (cost, chance, sev, _) in s.items():
        x, y = px(cost), py(chance)
        d.ellipse([x - 12, y - 12, x + 12, y + 12], fill=SEV[sev], outline=INK, width=2)
        off = 18 if cid not in ("AC3",) else -70
        label(d, x + off, y - 14, cid, TEXT, 24, True)
    header(d, 1360, 230, "THE SEVERITY RULE", CYAN)
    for k, ln in enumerate(section("TWO", "THE SEVERITY RULE")):
        sev, rule = cols(ln)
        label(d, 1360, 300 + k * 90, sev, SEV[sev], 30, True)
        fit_mono(d, 1360, 340 + k * 90, rule, TEXT, 24, 520)
    counts = {}
    for _, _, sev, _ in s.values():
        counts[sev] = counts.get(sev, 0) + 1
    header(d, 1360, 600, "THE RESULT", GREEN)
    for k, sev in enumerate(("High", "Medium", "Low")):
        ids = [c for c, v in s.items() if v[2] == sev]
        label(d, 1360, 670 + k * 70, "%s %d" % (sev, counts.get(sev, 0)), SEV[sev], 28, True)
        fit_mono(d, 1560, 674 + k * 70, " ".join(ids), TEXT, 24, 320)
    caption(d, "Cheap or likely is High: four of the eight cases.", AMBER, 950)


def validator(img, d, cfg):
    """Five files into the image's own validator: one valid, four refused, each with its message."""
    image = section("THREE", "THE VALIDATOR")[0].strip()
    inputs = {c[0]: c[1] for c in (cols(ln) for ln in section("THREE", "THE INPUT"))}
    res, cur = {}, None
    for ln in section("THREE", "THE RESULT"):
        if not ln.startswith("  "):
            cur = cols(ln)[0]
            res[cur] = [cols(ln)[1], "", ""]
        else:
            k, v = ln.strip().split(None, 1)
            res[cur][1 if k == "at" else 2] = v.strip()
    header(d, 60, 170, "THE INPUT: FIVE FILES", CYAN)
    header(d, 1000, 170, "THE RESULT: " + image, GREEN)
    for k, (name, what) in enumerate(inputs.items()):
        y = 240 + k * 140
        ok = res[name][0] == "valid"
        c = GREEN if ok else ROSE
        cell(d, 60, y, 360, 60, name, CYAN, 26)
        fit_mono(d, 60, y + 76, what, MUTED, 26, 840)
        arrow(d, (440, y + 30), (980, y + 30), LINE, 3)
        panel(d, 1000, y, 860, 120, c, 3)
        label(d, 1030, y + 12, res[name][0], c, 28, True)
        if not ok:
            fit_mono(d, 1200, y + 18, res[name][1], MUTED, 24, 640)
            fit_mono(d, 1030, y + 68, res[name][2], TEXT, 28, 800)
    caption(d, "The editor's own schema and checker, taken from the image as they are.", AMBER, 950)


def plan_steps(img, d, cfg):
    """Open threats in the file and failing abuse cases, step by step, then threat by threat."""
    rows = [cols(ln) for ln in section("FOUR", "THE RESULT")[1:]]
    header(d, 60, 170, "THE RESULT, STEP BY STEP", GREEN)
    base, top = 820, 270
    most = max(int(r[2]) for r in rows)
    for k, (step, defence, opened, failing, valid) in enumerate(rows):
        x = 100 + k * 190
        h = int(opened) / most * (base - top)
        d.rectangle([x, base - h, x + 70, base], fill=ROSE)
        h2 = int(failing) / most * (base - top)
        d.rectangle([x + 80, base - h2, x + 150, base], fill=AMBER)
        label(d, x + 35, base - h - 8, opened, TEXT, 26, True, "md")
        label(d, x + 115, base - h2 - 8, failing, TEXT, 26, True, "md")
        label(d, x + 75, base + 12, defence, TEXT, 24, True, "ma")
        label(d, x + 75, base + 48, valid, GREEN, 22, False, "ma")
    d.rectangle([100, 920, 130, 945], fill=ROSE)
    label(d, 140, 918, "open threats in the file", MUTED, 22)
    d.rectangle([460, 920, 490, 945], fill=AMBER)
    label(d, 500, 918, "abuse cases failing", MUTED, 22)
    header(d, 1080, 170, "THREAT BY THREAT, BEFORE AND AFTER", CYAN)
    for k, ln in enumerate(section("FOUR", "BEFORE THE PLAN")):
        m = re.match(r"(\S+)\s+(\S+) -> (\S+)\s*(.*)", ln.strip())
        y = 240 + k * 80
        c = GREEN if m.group(3) == "Mitigated" else ROSE
        label(d, 1080, y + 10, m.group(1), TEXT, 28, True)
        cell(d, 1190, y, 260, 60, m.group(3), c, 26)
        fit_mono(d, 1470, y + 30, m.group(4).replace("In place: ", "by "), MUTED, 24, 400, "lm")
    still = "; ".join(re.sub(r"\s+", " ", ln.strip()) for ln in section("FOUR", "STILL OPEN"))
    caption(d, "Still open: " + still, ROSE, 900 + 50)


def schema_vs_rules(img, d, cfg):
    """Four files broken on purpose: the schema says valid, a rule of its own finds each one."""
    what = {c[0]: c[1] for c in (cols(ln) for ln in section("FIVE", "BEFORE THE CHECK"))}
    res, cur = {}, None
    for ln in section("FIVE", "AFTER THE CHECK"):
        if not ln.startswith("  "):
            cur = cols(ln)[0]
            res[cur] = [cols(ln)[1].split(",")[0], []]
        else:
            res[cur][1].append(ln.strip())
    plan = heading("FIVE", "THE PLAN'S FILES").split(": ", 1)[1]
    header(d, 60, 170, "THE INPUT: THE LAST FILE, BROKEN FOUR WAYS", CYAN)
    label(d, 1040, 182, "THE SCHEMA", MUTED, 26, True)
    label(d, 1360, 182, "THE RULES", MUTED, 26, True)
    for k, (name, desc) in enumerate(what.items()):
        y = 250 + k * 160
        cell(d, 60, y, 330, 64, name, CYAN, 28)
        fit_mono(d, 60, y + 84, desc, MUTED, 28, 900)
        schema = res[name][0]
        cell(d, 1000, y, 280, 64, schema, GREEN if schema == "schema valid" else ROSE, 26)
        rule, where = res[name][1][0].split(": ", 1)
        cell(d, 1320, y, 560, 64, rule, ROSE, 24)
        fit_mono(d, 1320, y + 84, where, TEXT, 26, 560)
    caption(d, "The plan's own files: %s. A valid file can still be wrong." % plan, AMBER, 900 + 50)


def wrap(text, width):
    lines, cur = [], ""
    for w in text.split():
        if cur and len(cur) + 1 + len(w) > width:
            lines.append(cur)
            cur = w
        else:
            cur = (cur + " " + w).strip()
    return lines + [cur]


def editor_bill(img, d, cfg):
    """The editor's server answering four requests, and what the file cannot hold."""
    resp = section("SIX", "THE RESPONSES")
    providers = [cols(ln) for ln in resp if ln.startswith("  ")]
    main = [ln for ln in resp if not ln.startswith("  ")]
    header(d, 60, 170, "THE REQUESTS AND THE RESPONSES", CYAN)
    for k, ln in enumerate(main):
        path, answer = re.match(r"(\S+)\s+(.+)", ln.strip()).groups()
        y = 250 + k * 150
        cell(d, 60, y, 960, 64, "GET " + path, CYAN, 28, anchor="lm")
        c = ROSE if answer.startswith("4") else GREEN
        fit_mono(d, 90, y + 102, answer.rstrip(":"), c, 28, 930, "lm")
    for k, (name, val) in enumerate(providers):
        x = 60 + k * 196
        cell(d, x, 850, 180, 64, name.replace("Enabled", ""), GREEN if val == "true" else LINE, 26,
             text_colour=GREEN if val == "true" else MUTED)
    header(d, 1100, 170, "THE BILL", ROSE)
    for k, ln in enumerate(section("SIX", "THE BILL")):
        what, why = cols(ln)
        y = 250 + k * 220
        panel(d, 1100, y, 780, 190, ROSE, 2)
        label(d, 1130, y + 16, what, ROSE, 32, True)
        for j, part in enumerate(wrap(why, 40)):
            fit_mono(d, 1130, y + 70 + j * 40, part, TEXT, 28, 730)
    caption(d, "The file is a picture of the threats; the abuse cases in the code are the tests.", AMBER, 950)
