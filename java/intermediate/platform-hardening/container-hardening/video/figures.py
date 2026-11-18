"""Pictures for the container hardening video, drawn by the `figure` slide kind.

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
LEVEL = {"PRIVILEGED": ROSE, "BASELINE": AMBER, "RESTRICTED": GREEN}


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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def facts(lines):
    """`key   value` lines as a list of (key, value)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip())]


def attacks(name):
    """The attack table of an act: a list of (group, id, possible, reason)."""
    rows, group = [], None
    for ln in sections(name, "THE ATTACKS")[0]:
        if not ln.startswith(" "):
            group = ln.strip().rstrip(":")
            continue
        m = re.match(r"\s*(\S+)\s+(POSSIBLE|blocked)\s+(.*)", ln)
        rows.append((group, m.group(1), m.group(2) == "POSSIBLE", m.group(3)))
    return rows


def checks(name):
    """The level checks of an act: a list of (level, check, passes), and the level line."""
    lines = sections(name, "HOW THE LEVEL")[0]
    rows = [re.match(r"(\S+)\s+(.*?)\s+(pass|FAIL)$", ln.strip()).groups() for ln in lines if not ln.startswith("level")]
    level = next(ln for ln in lines if ln.startswith("level")).split(None, 1)[1]
    return [(a, b, c == "pass") for a, b, c in rows], level


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
    label(d, W // 2, y, text, colour, 30, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def level_badge(d, cx, y, level, w=360):
    colour = LEVEL[level]
    d.rounded_rectangle([cx - w // 2, y, cx + w // 2, y + 70], radius=35, fill=BG_CELL, outline=colour, width=5)
    d.text((cx, y + 35), level, font=font(SANS_B, 34), fill=colour, anchor="mm")


def count_of(line):
    """'... = 7 of 13' -> (7, 13)."""
    a, b = re.search(r"(\d+) of (\d+)\s*$", line).groups()
    return int(a), int(b)


# ------------------------------------------------------------------ figures

def reach_map(img, d, cfg):
    """The core idea: one hijacked process, thirteen questions, grouped by what they reach."""
    rows = attacks("ONE")
    groups = list(dict.fromkeys(g for g, *_ in rows))
    cx, cy = W // 2, 560
    d.rounded_rectangle([cx - 210, cy - 70, cx + 210, cy + 70], radius=20, fill=BG_CELL, outline=ROSE, width=5)
    label(d, cx, cy - 22, "hijacked process", ROSE, 32, True, "mm")
    label(d, cx, cy + 24, "in " + re.search(r"docker run (\S+)\)", heading("ONE", "THE SETTINGS")).group(1), MUTED, 24, False, "mm")
    spots = [(60, 190), (1230, 190), (60, 640), (1230, 640)]
    for (x, y), g in zip(spots, groups):
        mine = [r for r in rows if r[0] == g]
        h = 70 + len(mine) * 64
        panel(d, x, y, 630, h, LINE)
        label(d, x + 24, y + 18, g, CYAN, 28, True)
        for k, (_, aid, pos, _) in enumerate(mine):
            cell(d, x + 24, y + 64 + k * 64, 582, 52, aid + ("  POSSIBLE" if pos else "  blocked"), ROSE if pos else GREEN, 26)
        tx = x + 630 if x < cx else x
        draw.arrow(d, cx - 210 if x < cx else cx + 210, cy, tx, y + h // 2, MUTED, 3)
    n, total = count_of(sections("ONE", "HOW THE COUNT")[0][0])
    caption(d, "docker run with no options: %d of the %d questions are answered yes." % (n, total), AMBER, 950)


def defaults_settings(img, d, cfg):
    """Act one: the default settings (input), and the count and the level they give (result)."""
    header(d, 60, 180, heading("ONE", "THE SETTINGS"), CYAN)
    rows = facts(sections("ONE", "THE SETTINGS")[0])
    risky = {"user": "root", "root files": "writable", "new privileges": "allowed", "limits": "none", "secrets": "in the environment"}
    for i, (k, v) in enumerate(rows):
        y = 245 + i * 70
        bad = risky.get(k) == v or v.startswith("14")
        panel(d, 60, y, 860, 58, ROSE if bad else LINE)
        label(d, 85, y + 29, k, MUTED, 26, True, "lm")
        fit_mono(d, 330, y + 29, v, ROSE if bad else TEXT, 26, 570, "lm")
    header(d, 1000, 180, "THE RESULT", AMBER)
    count = sections("ONE", "HOW THE COUNT")[0][0].strip()
    parts = re.findall(r"(\w+) (\d+)", count.split("=")[0])
    groups = {}
    for g, aid, pos, _ in attacks("ONE"):
        groups.setdefault(g, []).append(pos)
    for i, ((name, n), allp) in enumerate(zip(parts, groups.values())):
        y = 260 + i * 80
        label(d, 1000, y + 22, name, MUTED, 28, True, "lm")
        for k, p in enumerate(allp):
            x = 1200 + k * 120
            d.rounded_rectangle([x, y, x + 105, y + 44], radius=8, fill=ROSE if p else BG_CELL, outline=ROSE if p else GREEN, width=3)
        label(d, 1700, y + 22, "%s of %d" % (n, len(allp)), ROSE if int(n) else GREEN, 28, True, "lm")
    n, of = count_of(count)
    label(d, 1000, 600, "%d of %d attacks possible" % (n, of), ROSE, 34, True)
    rows, level = checks("ONE")
    lv = level.split()[0]
    level_badge(d, 1180, 680, lv, 340)
    fails = [c for _, c, ok in rows if not ok]
    label(d, 1380, 690, "fails restricted:", MUTED, 24, True)
    for k, c in enumerate(fails):
        label(d, 1380, 725 + k * 34, c, AMBER, 24)
    caption(d, "Root, a writable file system, raw sockets and no limits: the defaults are generous.", AMBER, 940)


def tutorial_flags(img, d, cfg):
    """Act two: the tutorial's flags (input) and the six reasons it is not even Baseline (result)."""
    cmd = [ln.strip().rstrip("\\").strip() for ln in sections("TWO", "THE COMMAND")[0]]
    flags = cmd[1:-1]
    header(d, 60, 180, "THE COMMAND: docker run ... " + cmd[-1], CYAN)
    for i, f in enumerate(flags):
        cell(d, 60, 250 + i * 72, 760, 58, f, ROSE, 26)
    draw.arrow_right(d, 850, 520, 960, MUTED, 5)
    why = sections("TWO", "WHY IT IS NOT")[0]
    reasons = [ln.strip() for ln in why if not ln.strip().startswith("level")]
    level = next(ln for ln in why if ln.strip().startswith("level")).split()[-1]
    header(d, 990, 180, heading("TWO", "WHY IT IS NOT"), ROSE)
    for i, r in enumerate(reasons):
        cell(d, 990, 250 + i * 72, 870, 58, r, AMBER, 25)
    level_badge(d, 1425, 700, level, 380)
    count = sections("TWO", "HOW THE COUNT")[0][0].strip()
    fit_mono(d, 990, 800, count, ROSE, 28, 870)
    caption(d, "Each flag opens a door; the Docker socket alone is root on the host.", AMBER, 940)


def step_matrix(img, d, cfg):
    """Act three: which attacks are still possible after each step, one column per step."""
    ids = [a for _, a, _, _ in attacks("ONE")]
    before = sections("THREE", "BEFORE THE STEPS")[0][0]
    steps, cur = [], None
    for ln in sections("THREE", "THE STEPS")[0]:
        m = re.match(r"\s*(\d) (.*?)\s+(\d+) of (\d+)\s+(\w+)$", ln)
        if m:
            cur = [m.group(2), int(m.group(3)), m.group(5), []]
            steps.append(cur)
        else:
            cur[3] += [w.strip(",") for w in ln.replace("blocks", "").split() if w.strip(",") != "nothing"]
    alive = set(ids)
    cols = [("start", len(alive), before.split()[-1], set(alive))]
    for name, n, lv, blocked in steps:
        alive -= set(blocked)
        assert len(alive) == n, (name, n, alive)
        cols.append((name, n, lv, set(alive)))
    x0, y0, cw, rh = 360, 278, 186, 41
    header(d, 60, 180, "AFTER EACH STEP: rose = still possible", CYAN)
    for j, (name, n, lv, live) in enumerate(cols):
        x = x0 + j * cw
        label(d, x + cw // 2 - 4, y0 - 8, "start" if j == 0 else "step %d" % j, MUTED, 22, True, "md")
        for i, a in enumerate(ids):
            y = y0 + i * rh
            on = a in live
            d.rounded_rectangle([x + 6, y + 4, x + cw - 14, y + rh - 4], radius=6,
                                fill=ROSE if on else BG_CELL, outline=ROSE if on else LINE, width=2)
        y = y0 + len(ids) * rh + 10
        label(d, x + cw // 2 - 4, y, str(n), ROSE if n > 1 else AMBER, 34, True, "ma")
        label(d, x + cw // 2 - 4, y + 46, lv.lower(), LEVEL[lv], 20, True, "ma")
    for i, a in enumerate(ids):
        fit_mono(d, 60, y0 + i * rh + rh // 2, a, TEXT, 22, 280, "lm")
    y = y0 + len(ids) * rh + 10
    label(d, 60, y + 4, "possible", MUTED, 26, True)
    label(d, 60, y + 46, "level", MUTED, 22, True)
    names = "  ".join("%d %s" % (k, s[0]) for k, s in enumerate(steps, 1))
    fit_mono(d, 60, 935, names, MUTED, 20, 1800)
    caption(d, "Step 4, a user that is not root, blocks nothing alone; step 5 makes it count.", AMBER, 975)


def level_checks(img, d, cfg):
    """Act four: every Pod Security check for the defaults (before) and the hardened container (after)."""
    before, lb = checks("ONE")
    after, la = checks("FOUR")
    header(d, 760, 180, "DOCKER DEFAULTS", AMBER, 360)
    header(d, 1220, 180, "HARDENED", GREEN, 360)
    for i, ((lvl, name, ok1), (_, _, ok2)) in enumerate(zip(before, after)):
        y = 250 + i * 64
        if i == 0 or before[i - 1][0] != lvl:
            label(d, 60, y + 26, lvl, CYAN, 26, True, "lm")
        panel(d, 230, y, 1350, 54, LINE)
        fit_mono(d, 255, y + 27, name, TEXT, 26, 480, "lm")
        for x, ok in ((760, ok1), (1220, ok2)):
            cell(d, x + 60, y + 6, 240, 42, "pass" if ok else "FAIL", GREEN if ok else ROSE, 24)
    y = 250 + len(before) * 64 + 20
    label(d, 60, y + 35, "level", MUTED, 28, True, "lm")
    level_badge(d, 940, y, lb.split()[0], 330)
    level_badge(d, 1400, y, la.split()[0], 330)
    caption(d, "Baseline keeps the host out; Restricted adds: not root, no escalation, no capabilities.", AMBER, 950)


def harmless(img, d, cfg):
    """Act five: two small additions to the hardened container, and what each one undoes."""
    sec = act("FIVE")
    blocks, cur = [], None
    for ln in sec:
        if not ln.startswith(" "):
            cur = [ln.split(": ", 1)[0], ln.split(": ", 1)[1], []]
            blocks.append(cur)
        else:
            cur[2].append(ln.strip())
    for i, (tag, what, lines) in enumerate(blocks):
        x = 60 + i * 610
        header(d, x, 180, tag, GREEN if tag == "BEFORE" else AMBER, 580)
        label(d, x + 290, 250, what, TEXT, 28, True, "ma")
        m = re.match(r"(\d+) of (\d+) possible, level (\w+)", lines[0])
        n, of, lv = m.groups()
        panel(d, x, 300, 580, 600, LEVEL[lv], 3)
        label(d, x + 290, 330, "%s of %s possible" % (n, of), ROSE if int(n) > 1 else AMBER, 34, True, "ma")
        level_badge(d, x + 290, 400, lv, 400)
        y = 510
        for ln in lines[1:]:
            if ln.startswith("newly possible"):
                label(d, x + 30, y, "newly possible:", MUTED, 24, True)
                v = ln.split(": ", 1)[1]
                fit_mono(d, x + 30, y + 36, v, ROSE if v != "nothing" else GREEN, 24, 520)
                y += 90
            elif ln.startswith("now FAIL"):
                lvl, name = re.match(r"now FAIL\s+(\S+)\s+(.*)", ln).groups()
                cell(d, x + 30, y, 520, 50, "FAIL %s: %s" % (lvl, name), ROSE, 22)
                y += 62
            else:
                aid, state, why = re.match(r"(\S+)\s+(\S+)\s+(.*)", ln).groups()
                cell(d, x + 30, y, 520, 50, "%s %s" % (aid, state), GREEN if state == "blocked" else ROSE, 22)
                label(d, x + 290, y + 58, why, MUTED, 22, False, "ma")
                y += 100
    caption(d, "NET_RAW matters only to root; root with SYS_ADMIN undoes Baseline.", AMBER, 940)
