"""Pictures for the container hardening video with Docker and kind, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints from real containers and a
real kind cluster, so a picture can never disagree with what was measured.
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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def facts(lines):
    """`key   value` lines as a list of (key, value)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip())]


def attempts(name):
    """The attempts of an act: a list of (group, id, possible, reason)."""
    rows, group = [], None
    for ln in sections(name, "THE ATTEMPTS")[0]:
        if not ln.startswith(" "):
            group = ln.strip().rstrip(":")
            continue
        m = re.match(r"\s*(\S+)\s+(POSSIBLE|blocked)\s+(.*)", ln)
        rows.append((group, m.group(1), m.group(2) == "POSSIBLE", m.group(3)))
    return rows


def count(name):
    """'container 4 + ... = 7 of 13' -> the line, 7, 13."""
    line = sections(name, "HOW THE COUNT")[0][0].strip()
    a, b = re.search(r"(\d+) of (\d+)$", line).groups()
    return line, int(a), int(b)


def options(name):
    """The docker run options of an act, one per entry, the image last."""
    return [ln.strip().rstrip("\\").strip() for ln in sections(name, "THE OPTIONS")[0][1:]]


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


def caption(d, text, colour=AMBER, y=945):
    label(d, W // 2, y, text, colour, 30, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def groups(d, rows, x0, y0, w, cell_h=48):
    """The attempts in four group panels (two by two), rose when possible."""
    names = list(dict.fromkeys(g for g, *_ in rows))
    col_w = (w - 30) // 2
    tops = [y0, y0]
    for k, g in enumerate(names):
        mine = [r for r in rows if r[0] == g]
        x = x0 + (k % 2) * (col_w + 30)
        y = tops[k % 2]
        h = 56 + len(mine) * (cell_h + 8)
        panel(d, x, y, col_w, h, LINE)
        label(d, x + 20, y + 14, g, CYAN, 24, True)
        for i, (_, aid, pos, _) in enumerate(mine):
            cell(d, x + 20, y + 50 + i * (cell_h + 8), col_w - 40, cell_h, aid, ROSE if pos else GREEN, 24)
        tops[k % 2] = y + h + 20


# ------------------------------------------------------------------ figures

def capability_mask(img, d, cfg):
    """The core idea: how a probe reads the capability mask, bit by bit, with Docker's default value."""
    sec = sections("ONE", "HOW THE CAPABILITY MASK")[0]
    mask = re.search(r"CapEff (\w+)", heading("ONE", "HOW THE CAPABILITY MASK")).group(1)
    text = " ".join(ln.strip() for ln in sec if ">>" not in ln)
    bits = [int(b) for b in re.findall(r"\d+", text.split("names")[0].replace("bits set", ""))]
    names = re.findall(r"[A-Z_]+", text.split("names")[1])
    tests = [re.match(r"\s*(\S+)\s+\(0x(\w+) >> (\d+)\) & 1 = (\d), (.*)", ln).groups() for ln in sec if ">>" in ln]
    header(d, 60, 180, "THE INPUT: CapEff " + mask + ", read from /proc/self/status", CYAN)
    value = int(mask, 16)
    named = dict(zip(bits, names))
    tested = {int(n): name for name, _, n, _, _ in tests}
    cw, x0, y0 = 54, 60, 300
    for n in range(32):
        on = value >> n & 1
        x = x0 + (31 - n) * cw
        colour = AMBER if n in tested else (GREEN if on else LINE)
        d.rounded_rectangle([x, y0, x + cw - 6, y0 + 64], radius=8, fill=colour if on else BG_CELL, outline=colour, width=3)
        d.text((x + (cw - 6) // 2, y0 + 32), str(on), font=font(MONO_B, 30), fill=INK if on else MUTED, anchor="mm")
        d.text((x + (cw - 6) // 2, y0 + 82), str(n), font=font(MONO_B, 18), fill=MUTED, anchor="mm")
    label(d, x0, y0 - 36, "bit 31", MUTED, 22, True)
    label(d, x0 + 32 * cw - 6, y0 - 36, "bit 0", MUTED, 22, True, "ra")
    header(d, 60, 440, "THE RESULT: %d BITS SET, ONE CAPABILITY EACH" % len(bits), GREEN)
    for k, n in enumerate(bits):
        x, y = 60 + (k % 4) * 455, 505 + (k // 4) * 58
        cell(d, x, y, 435, 48, "%d %s" % (n, named.get(n, "?")), AMBER if n in tested else GREEN, 23)
    for k, (name, hx, n, one, verdict) in enumerate(tests):
        fit_mono(d, 60, 760 + k * 56, "%s: (0x%s >> %s) & 1 = %s, %s" % (name, hx, n, one, verdict),
                 GREEN if one == "1" else ROSE, 30, 1800)
    caption(d, "The sniff-network probe reads bit 13 exactly this way.", AMBER, 905)


def defaults_attempts(img, d, cfg):
    """Act one: what the probes read in the default container (input), and the attempts (result)."""
    header(d, 60, 180, "WHAT THE PROBES READ", CYAN)
    for i, (k, v) in enumerate(facts(sections("ONE", "WHAT THE PROBES READ")[0])):
        y = 245 + i * 74
        panel(d, 60, y, 700, 62, LINE)
        label(d, 80, y + 31, k, MUTED, 24, True, "lm")
        fit_mono(d, 270, y + 31, v, TEXT, 24, 470, "lm")
    line, n, total = count("ONE")
    header(d, 820, 180, "THE ATTEMPTS: rose = possible", ROSE)
    groups(d, attempts("ONE"), 820, 245, 1040, 56)
    fit_mono(d, 60, 640, line, AMBER, 26, 720)
    caption(d, "Root, 14 capabilities, a writable root and no limits: %d of %d work." % (n, total), AMBER, 945)


def tutorial_options(img, d, cfg):
    """Act two: the tutorial's options (input) and every attempt working (result)."""
    opts = options("TWO")
    header(d, 60, 180, "THE OPTIONS", CYAN)
    for i, o in enumerate(opts[:-1]):
        cell(d, 60, 245 + i * 70, 760, 56, o, ROSE, 24)
    fit_mono(d, 60, 245 + (len(opts) - 1) * 70 + 14, "image " + opts[-1], MUTED, 24, 760)
    line, n, total = count("TWO")
    header(d, 870, 180, "THE ATTEMPTS: %d OF %d WORK" % (n, total), ROSE)
    groups(d, attempts("TWO"), 870, 245, 990, 56)
    caption(d, "--privileged, no filter, the host's network, processes, files and Docker socket.", AMBER, 945)


def step_matrix(img, d, cfg):
    """Act three: which attempts still work after each step; a step can block one and open another."""
    ids = [a for _, a, _, _ in attempts("ONE")]
    start = set(a for _, a, p, _ in attempts("TWO") if p)
    steps, cur, mode = [], None, None
    for ln in sections("THREE", "THE STEPS")[0]:
        m = re.match(r"\s*(\d) (.*?)\s+(\d+) of (\d+)$", ln)
        if m:
            cur = [m.group(2), int(m.group(3)), [], []]
            steps.append(cur)
            continue
        s = ln.strip()
        if s.startswith("blocks"):
            mode, s = 2, s[len("blocks"):]
        elif s.startswith("opens"):
            mode, s = 3, s[len("opens"):]
        cur[mode] += [w.strip(",") for w in s.split() if w.strip(",") != "nothing"]
    alive = set(start)
    cols = [("start", len(alive), set(alive), set())]
    for name, n, blocked, opened in steps:
        alive = (alive - set(blocked)) | set(opened)
        assert len(alive) == n, (name, n, alive)
        cols.append((name, n, set(alive), set(opened)))
    x0, y0, cw, rh = 360, 278, 186, 41
    header(d, 60, 180, "AFTER EACH STEP: rose = still works, amber = opened by the step", CYAN)
    for j, (name, n, live, opened) in enumerate(cols):
        x = x0 + j * cw
        label(d, x + cw // 2 - 4, y0 - 8, "start" if j == 0 else "step %d" % j, MUTED, 22, True, "md")
        for i, a in enumerate(ids):
            y = y0 + i * rh
            colour = AMBER if a in opened else ROSE
            on = a in live
            d.rounded_rectangle([x + 6, y + 4, x + cw - 14, y + rh - 4], radius=6,
                                fill=colour if on else BG_CELL, outline=colour if on else LINE, width=2)
        y = y0 + len(ids) * rh + 10
        label(d, x + cw // 2 - 4, y, str(n), ROSE if n > 1 else AMBER, 34, True, "ma")
    for i, a in enumerate(ids):
        fit_mono(d, 60, y0 + i * rh + rh // 2, a, TEXT, 22, 280, "lm")
    label(d, 60, y0 + len(ids) * rh + 14, "possible", MUTED, 26, True)
    names = "  ".join("%d %s" % (k, s[0]) for k, s in enumerate(steps, 1))
    fit_mono(d, 60, 900, names, MUTED, 20, 1800)
    caption(d, "The number stays when a step blocks one attempt and opens another.", AMBER, 950)


def before_after_facts(img, d, cfg):
    """Act four: what the probes read in the default container (before) and the hardened one (after)."""
    before = facts(sections("ONE", "WHAT THE PROBES READ")[0])
    after = dict(facts(sections("FOUR", "WHAT THE PROBES READ")[0]))
    header(d, 360, 180, "BEFORE: NO OPTIONS", ROSE, 700)
    header(d, 1100, 180, "AFTER: HARDENED", GREEN, 760)
    for i, (k, v) in enumerate(before):
        y = 250 + i * 84
        label(d, 60, y + 32, k, MUTED, 28, True, "lm")
        cell(d, 360, y, 700, 64, v, ROSE, 24)
        cell(d, 1100, y, 760, 64, after[k], GREEN, 24)
    _, n0, total = count("ONE")
    _, n1, _ = count("FOUR")
    y = 250 + len(before) * 84 + 10
    label(d, 60, y + 32, "attempts", MUTED, 28, True, "lm")
    cell(d, 360, y, 700, 64, "%d of %d work" % (n0, total), ROSE, 28)
    cell(d, 1100, y, 760, 64, "%d of %d work" % (n1, total), GREEN, 28)
    caption(d, "Not root, no capabilities, a filter, 512 MiB and 200 processes: the kernel enforces each.", AMBER, 945)


def harmless(img, d, cfg):
    """Act five: small additions to the hardened container, and what each one opens again."""
    blocks, cur = [], None
    for ln in act("FIVE"):
        if not ln.startswith(" "):
            if ln.startswith("WHAT IT SHOWS"):
                cur = None
                continue
            cur = [ln.split(": ", 1)[0], ln.split(": ", 1)[1], []]
            blocks.append(cur)
        elif cur is not None:
            cur[2].append(ln.strip())
    for i, (tag, what, lines) in enumerate(blocks):
        x = 60 + i * 455
        header(d, x, 180, tag, GREEN if tag == "BEFORE" else AMBER, 430)
        panel(d, x, 240, 430, 640, LINE, 2)
        fit_mono(d, x + 215, 290, what.replace("hardened + ", "+ "), TEXT, 24, 400, "mm")
        n = int(re.match(r"(\d+) of", lines[0]).group(1))
        label(d, x + 215, 330, "%s" % lines[0], ROSE if n > 1 else AMBER, 30, True, "ma")
        line = next((ln for ln in lines if ln.startswith("newly possible:")), None)
        m = line is not None
        opened = [w.strip(",") for w in line.split(":", 1)[1].split()] if m else []
        y = 400
        if m:
            label(d, x + 25, y, "newly possible:", MUTED, 24, True)
            y += 40
            for o in opened:
                cell(d, x + 25, y, 380, 48, o, GREEN if o == "nothing" else ROSE, 22)
                y += 58
        for ln in lines[1:]:
            mm = re.match(r"(sniff-network|mount-host)\s+(\S+)\s+(.*)", ln)
            if mm:
                cell(d, x + 25, y + 10, 380, 48, mm.group(1) + " " + mm.group(2), GREEN if mm.group(2) == "blocked" else ROSE, 22)
                label(d, x + 215, y + 68, mm.group(3), MUTED, 20, False, "ma")
    caption(d, sections("FIVE", "WHAT IT SHOWS")[0][0].strip().capitalize() + ".", AMBER, 920)


def admission_grid(img, d, cfg):
    """Act six: which namespace admits which pod, and what the admitted pods can still do."""
    rows = [re.split(r"\s{2,}", ln.strip()) for ln in sections("SIX", "THE ADMISSION")[0]]
    head, body = rows[0], rows[1:]
    x0, y0, cw, rh = 400, 260, 470, 86
    header(d, 60, 180, "THE ADMISSION DECISIONS (kubectl apply --dry-run=server)", CYAN)
    for j, ns in enumerate(head[1:]):
        label(d, x0 + j * cw + (cw - 20) // 2, y0, "enforces " + ns, CYAN, 26, True, "ma")
    for i, row in enumerate(body):
        y = y0 + 50 + i * rh
        label(d, 60, y + 32, row[0] + " pod", TEXT, 28, True, "lm")
        for j, v in enumerate(row[1:]):
            cell(d, x0 + j * cw, y, cw - 20, 64, v, GREEN if v == "admitted" else ROSE, 28)
    probed = sections("SIX", "THE RUNNING PODS")[0]
    header(d, 60, 600, "THE ADMITTED PODS, PROBED FROM INSIDE", AMBER)
    y = 670
    for ln in probed:
        if re.search(r"\d+ of \d+$", ln.strip()):
            name, n = re.match(r"\s*(.*?)\s{2,}(\d+ of \d+)$", ln).groups()
            label(d, 60, y + 4, name, TEXT, 28, True)
            label(d, 520, y + 4, n, ROSE if not n.startswith("1 ") else AMBER, 28, True)
        else:
            fit_mono(d, 700, y + 6, ln.strip(), MUTED, 22, 1160)
            y += 60
    caption(d, "Admission judges the spec once; a pod it admits can still run a dropped script.", AMBER, 945)
