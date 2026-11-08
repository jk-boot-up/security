"""Pictures for the least privilege video, drawn by the `figure` slide kind.

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

def _catalogue():
    """The store's permissions, in order, from act one's catalogue."""
    perms = []
    for ln in part("ONE", "THE CATALOGUE"):
        resource, *actions = ln.split()
        perms += ["%s:%s" % (resource, a) for a in actions]
    return perms


def _roles():
    """role -> (count text, list of patterns), from act one's roles."""
    out = {}
    for ln in part("ONE", "THE ROLES"):
        m = re.match(r"(\w+)\s+(\d+ of \d+)\s+(.+)$", ln)
        out[m.group(1)] = (m.group(2), m.group(3).split())
    return out


def _allows(patterns, perm):
    res, action = perm.split(":")
    for p in patterns:
        if p == "*":
            return True
        r, a = p.split(":")
        if r in ("*", res) and a in ("*", action):
            return True
    return False


def role_matrix(img, d, cfg):
    """Who can do what: every permission of the store against every role."""
    perms, roles = _catalogue(), _roles()
    header(d, 100, 205, "THE CATALOGUE: %d THINGS THAT CAN BE DONE" % len(perms), CYAN)
    x0, y0, rh = 470, 300, 41
    cw = 230
    colours = {"admin": ROSE, "support": GREEN, "warehouse": GREEN, "finance": GREEN}
    for j, role in enumerate(roles):
        cx = x0 + j * cw + cw // 2
        label(d, cx, 278, role, colours.get(role, GREEN), 28, True, "mm")
    for i, perm in enumerate(perms):
        y = y0 + i * rh
        mono(d, 440, y + rh // 2, perm, TEXT if not perm.startswith("secrets") else ROSE, 24, "rm")
        for j, (role, (_, pats)) in enumerate(roles.items()):
            x = x0 + j * cw
            on = _allows(pats, perm)
            c = colours.get(role, GREEN)
            d.rounded_rectangle([x + 20, y + 3, x + cw - 20, y + rh - 3], radius=8,
                                fill=c if on else DIM, outline=c if on else LINE, width=2)
            if on:
                d.text((x + cw // 2, y + rh // 2), "allowed", font=font(SANS_B, 20), fill=INK, anchor="mm")
    for j, (role, (count, _)) in enumerate(roles.items()):
        label(d, x0 + j * cw + cw // 2, y0 + len(perms) * rh + 22, count, colours.get(role, GREEN), 26, True, "mm")
    px = x0 + len(roles) * cw + 30
    panel(d, px, 300, W - 100 - px, 420, AMBER)
    label(d, px + 30, 330, "A stolen key opens", TEXT, 30, True)
    label(d, px + 30, 372, "exactly what its", TEXT, 30, True)
    label(d, px + 30, 414, "role allows.", TEXT, 30, True)
    label(d, px + 30, 480, "admin: " + roles["admin"][0], ROSE, 30, True)
    label(d, px + 30, 525, "support: " + roles["support"][0], GREEN, 30, True)
    label(d, px + 30, 590, "Everything not", MUTED, 26)
    label(d, px + 30, 626, "allowed is denied.", MUTED, 26)


def decisions(img, d, cfg):
    """Two requests checked statement by statement: denied by default, and denied by an explicit deny."""
    reqs = sections("ONE", "THE REQUEST")
    checks = sections("ONE", "THE CHECK")
    decs = [ln for ln in act("ONE") if ln.startswith("THE DECISION")]
    for k in range(2):
        x = 100 + k * 880
        request = reqs[k][0].split(":", 1)[1].strip()
        header(d, x, 210, "THE REQUEST", CLIENT)
        panel(d, x, 268, 840, 80, CLIENT)
        fit(d, x + 20, 308, request, TEXT, 28, 800, True, "lm")
        header(d, x, 380, "THE CHECK, STATEMENT BY STATEMENT", CYAN)
        for i, ln in enumerate(checks[k][1]):
            stmt, verdict = re.split(r"\s{2,}", ln, maxsplit=1)
            y = 440 + i * 86
            hit = verdict.startswith("matches")
            c = (ROSE if stmt.startswith("deny") else GREEN) if hit else MUTED
            cell(d, x, y, 360, 70, stmt, c, 24, width=4 if hit else 2, fill=BG_CELL if hit else DIM)
            fit(d, x + 380, y + 35, verdict, c if hit else MUTED, 24, 460, hit, "lm")
        decision = decs[k].split(":", 1)[1].split("<-")[0].strip()
        note = decs[k].split("<-")[1].strip().lower()
        header(d, x, 730, "THE DECISION", ROSE)
        cell(d, x, 790, 840, 90, decision, ROSE, 40)
        label(d, x + 420, 905, note, AMBER, 28, True, "ma")


def blast(img, d, cfg):
    """The blast radius before and after: which of the store's permissions a stolen token reaches."""
    perms = _catalogue()
    needed = part("TWO", "WHAT THE NIGHTLY")[0].split()
    before = part("TWO", "BEFORE THE FIX")
    after = part("TWO", "AFTER THE FIX")
    for k, (name, lines, c) in enumerate([("BEFORE THE FIX: RUNS AS ADMIN", before, ROSE),
                                         ("AFTER THE FIX: EXPORTER ROLE ONLY", after, GREEN)]):
        x = 100 + k * 880
        reach_all = int(re.search(r"reaches (\d+)", lines[0]).group(1)) == len(perms)
        header(d, x, 205, name, c)
        for i, perm in enumerate(perms):
            col, row = i % 3, i // 3
            cx, cy = x + col * 285, 275 + row * 92
            need = perm in needed
            on = reach_all or need
            fill = (GREEN if need else c) if on else DIM
            d.rounded_rectangle([cx, cy, cx + 265, cy + 76], radius=10, fill=fill if on else DIM,
                                outline=fill if on else LINE, width=2)
            d.text((cx + 132, cy + 38), perm, font=font(MONO_B, 21), fill=INK if on else MUTED, anchor="mm")
        reach = re.search(r"reaches (\d+ of \d+)", lines[0]).group(1)
        extra = lines[1].split()[0]
        label(d, x + 420, 750, "a stolen token reaches " + reach, c, 34, True, "ma")
        label(d, x + 420, 800, extra + " of them the job never needs", MUTED, 28, False, "ma")
    label(d, 960, 860, "green: the two the job needs, " + "  ".join(needed), GREEN, 28, True, "ma")
    caption(d, "Everything else it was given is risk without benefit.", AMBER, 920)


def refund_steps(img, d, cfg):
    """Five requests to the refund desk, and what the desk said to each."""
    reqs = part("THREE", "THE REQUESTS")
    res = part("THREE", "THE RESULTS")
    rule = first("THREE", r"THE RULE: (.+)")
    label(d, 960, 215, "the rule: " + rule, TEXT, 28, True, "ma")
    header(d, 100, 270, "THE REQUESTS", CLIENT)
    header(d, 1060, 270, "THE RESULTS", AMBER)
    for i, (q, r) in enumerate(zip(reqs, res)):
        y = 335 + i * 104
        n, who, what = re.split(r"\s{2,}", q.strip())
        parts = re.split(r"\s{2,}", r.strip())
        outcome, why = parts[1], " ".join(parts[2:]).split("<-")[0].strip()
        c = GREEN if outcome == "DONE" else ROSE
        cell(d, 100, y, 70, 84, n, MUTED, 30)
        panel(d, 190, y, 760, 84, CLIENT)
        label(d, 215, y + 42, who, CLIENT, 28, True, "lm")
        label(d, 520, y + 42, what, TEXT, 28, False, "lm")
        draw.arrow_right(d, 965, y + 42, 1045, MUTED, 4)
        cell(d, 1060, y, 300, 84, outcome, c, 28)
        label(d, 1380, y + 42, why, MUTED if c == GREEN else AMBER, 24, False, "lm")
    caption(d, "Dave is an admin, and still cannot approve his own refund.", AMBER, 875)


def timeline(img, d, cfg):
    """A grant from tick 10 until tick 13, and the four checks along the clock."""
    m = re.search(r"from tick (\d+) until tick (\d+)", " ".join(act("FOUR")))
    start, end = int(m.group(1)), int(m.group(2))
    who = part("FOUR", "THE GRANT")[0]
    checks = [re.match(r"tick (\d+)\s+(\w+)\s+(.+)$", ln).groups() for ln in part("FOUR", "THE CHECKS")]
    header(d, 100, 210, "THE GRANT", CLIENT)
    label(d, 330, 232, who + ", from tick %d until tick %d" % (start, end), TEXT, 30, True, "lm")
    lo, hi = start - 3, end + 2
    x0, x1, y = 160, 1760, 470
    step = (x1 - x0) / (hi - lo)
    xt = lambda t: x0 + (t - lo) * step
    d.rounded_rectangle([xt(start), y - 60, xt(end), y + 0], radius=10, fill=GREEN)
    d.text(((xt(start) + xt(end)) / 2, y - 30), "payments:write", font=font(MONO_B, 28), fill=INK, anchor="mm")
    d.line([(x0, y + 30), (x1, y + 30)], fill=MUTED, width=4)
    for t in range(lo, hi + 1):
        d.line([(xt(t), y + 18), (xt(t), y + 42)], fill=MUTED, width=3)
        label(d, xt(t), y + 62, str(t), MUTED, 24, False, "ma")
    label(d, x1, y + 100, "tick (the demo's clock)", MUTED, 22, False, "ra")
    for i, (t, ok, why) in enumerate(checks):
        c = GREEN if ok == "true" else ROSE
        d.line([(xt(int(t)), y + 30), (xt(int(t)), 720)], fill=c, width=3)
    header(d, 100, 640, "THE CHECKS: CAN CAROL WRITE PAYMENTS?", AMBER)
    for i, (t, ok, why) in enumerate(checks):
        t = int(t)
        c = GREEN if ok == "true" else ROSE
        cx = 100 + i * 440
        cell(d, cx, 720, 400, 80, "tick %d: %s" % (t, ok), c, 30)
        label(d, cx + 200, 820, why, MUTED, 24, False, "ma")
    caption(d, "Tick %d is the first without it: nobody had to remember to remove it." % end, AMBER, 900)


def creep_and_files(img, d, cfg):
    """Left: the audit of unused permissions. Right: the export file's permission letters, before and after."""
    hold = part("FIVE", "THE HOLDINGS")
    audit_head = [ln for ln in act("FIVE") if ln.startswith("THE AUDIT")][0]
    cutoff = re.search(r"= (\d+)\)", audit_head).group(1)
    removed = [ln.split()[:2] for ln in part("FIVE", "THE AUDIT") if "REMOVE" in ln]
    header(d, 100, 205, "THE AUDIT: UNUSED BEFORE DAY " + cutoff, AMBER)
    for i, ln in enumerate(hold):
        who, perm, *rest = ln.split()
        y = 270 + i * 82
        bad = [who, perm] in removed
        c = ROSE if bad else GREEN
        d.rounded_rectangle([100, y, 860, y + 68], radius=10, fill=BG_CELL, outline=c, width=3 if bad else 2)
        label(d, 120, y + 34, who, TEXT, 26, True, "lm")
        mono(d, 230, y + 34, perm, TEXT, 24, "lm")
        label(d, 840, y + 34, " ".join(rest) + ("  REMOVE" if bad else ""), c, 22, bad, "rm")
    label(d, 480, 700, part("FIVE", "THE AUDIT")[-1], AMBER, 26, True, "ma")
    before = part("FIVE", "BEFORE THE FIX")[0].split()
    after = part("FIVE", "AFTER THE FIX")[0].split()
    for k, (name, words, c) in enumerate([("BEFORE THE FIX", before, ROSE), ("AFTER THE FIX", after, GREEN)]):
        y = 205 + k * 330
        header(d, 960, y, name, c)
        perm = words[1]
        groups = [("owner", perm[0:3]), ("group", perm[3:6]), ("others", perm[6:9])]
        for g, (who, letters) in enumerate(groups):
            x = 960 + g * 290
            open_ = letters != "---"
            cc = (c if g else CLIENT) if open_ else MUTED
            cell(d, x, y + 70, 260, 90, letters, cc, 44, fill=BG_CELL if open_ else DIM)
            label(d, x + 130, y + 180, who, MUTED, 24, False, "ma")
        label(d, 960, y + 230, "export.csv  " + perm + "  readable by others: " + words[-1], c, 26, True, "la")
    label(d, 960, 860, "r read   w write   x enter (a folder)   - not allowed", MUTED, 24, False, "la")
