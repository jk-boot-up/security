"""Pictures for the least privilege with Spring Security video, drawn by the `figure` slide kind.

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
STATUS = {"200": GREEN, "401": AMBER, "403": ROSE}


def _rows(name, head):
    """Numbered lines of a section: {"1": ["no credentials", "GET /orders"], ...}."""
    out = {}
    for ln in part(name, head):
        m = re.match(r"(\d)\s+(.+)$", ln)
        if m:
            out[m.group(1)] = re.split(r"\s{2,}", m.group(2).strip())
    return out


def _pairs(name):
    """(number, who, what, status, why) for every request of an act."""
    req, res = _rows(name, "THE REQUESTS"), _rows(name, "THE RESPONSES")
    return [(n, req[n][0], " ".join(req[n][1:]), res[n][0], " ".join(res[n][1:]).split("<-")[0].strip()) for n in req]


def _table(d, x, y, rows, w=1720, rh=92, who_w=330, what_w=520):
    """Request on the left, response on the right, one row per request."""
    header(d, x, y, "THE REQUESTS", CLIENT)
    header(d, x + who_w + what_w + 160, y, "THE RESPONSES", AMBER)
    for i, (n, who, what, status, why) in enumerate(rows):
        yy = y + 62 + i * rh
        c = STATUS.get(status, MUTED)
        cell(d, x, yy, 60, rh - 16, n, MUTED, 26)
        panel(d, x + 76, yy, who_w + what_w, rh - 16, CLIENT)
        fit(d, x + 96, yy + (rh - 16) // 2, who, CLIENT, 26, who_w - 30, True, "lm")
        fit(d, x + 96 + who_w, yy + (rh - 16) // 2, what, TEXT, 26, what_w - 30, False, "lm")
        draw.arrow_right(d, x + 86 + who_w + what_w, yy + (rh - 16) // 2, x + who_w + what_w + 150, MUTED, 4)
        cell(d, x + who_w + what_w + 160, yy, 110, rh - 16, status, c, 32)
        fit(d, x + who_w + what_w + 290, yy + (rh - 16) // 2, why, c if status != "200" else MUTED, 24,
            w - (who_w + what_w + 290), status != "200", "lm")


def request_path(img, d, cfg):
    """One request's way through Spring Security, and a real request stopped at each gate."""
    one, two = _pairs("ONE"), _pairs("TWO")
    stops = [("WHO ARE YOU?", "sign in (HTTP Basic)", one[0], AMBER),
             ("A RULE FOR THE PATH", "Shop.chain, top to bottom", one[3], ROSE),
             ("A RULE ON THE METHOD", "@PreAuthorize on create()", two[0], ROSE)]
    y = 260
    cell(d, 100, y, 190, 120, "REQUEST", CLIENT, 30, mono_font=False)
    x = 340
    for i, (gate, how, ex, c) in enumerate(stops):
        draw.arrow_right(d, x - 40, y + 60, x - 6, MUTED, 4)
        panel(d, x, y, 330, 120, c, 3)
        fit(d, x + 165, y + 42, gate, c, 26, 310, True, "mm")
        fit(d, x + 165, y + 84, how, MUTED, 22, 310, False, "mm")
        # below each gate: the real request that stopped there
        draw.arrow(d, x + 165, y + 128, x + 165, y + 210, c, 4)
        cell(d, x + 65, y + 220, 200, 90, ex[3], c, 44)
        fit(d, x + 165, y + 345, ex[1], TEXT, 28, 330, True, "ma")
        fit(d, x + 165, y + 390, ex[2], CLIENT, 26, 330, False, "ma")
        fit(d, x + 165, y + 435, ex[4].split(",")[0], c, 24, 330, True, "ma")
        x += 380
    draw.arrow_right(d, x - 40, y + 60, x - 6, MUTED, 4)
    panel(d, x, y, 200, 120, GREEN, 3)
    label(d, x + 100, y + 60, "the code runs", GREEN, 24, True, "mm")
    ok = one[2]
    draw.arrow(d, x + 100, y + 128, x + 100, y + 210, GREEN, 4)
    cell(d, x, y + 220, 200, 90, ok[3], GREEN, 44)
    fit(d, x + 100, y + 345, ok[1], TEXT, 28, 230, True, "ma")
    fit(d, x + 100, y + 390, ok[2], CLIENT, 26, 230, False, "ma")
    label(d, 960, 790, first("ONE", r"^(401: .+)$"), TEXT, 30, True, "ma")
    caption(d, "Every gate can say no. Only a request that passes all of them reaches the code.", AMBER, 860)


def rule_walk(img, d, cfg):
    """Dave, an admin, asks for a path nobody wrote a rule for: every rule is checked, the last one denies."""
    rules = part("ONE", "THE RULES")
    n, who, what, status, why = _pairs("ONE")[4]
    header(d, 100, 205, "THE REQUEST", CLIENT)
    cell(d, 100, 262, 560, 70, "%s: %s" % (who, what), CLIENT, 26)
    header(d, 100, 360, "THE RULES, TOP TO BOTTOM", CYAN)
    path = what.split()[1]
    for i, ln in enumerate(rules):
        pat, need = re.split(r"\s{2,}", ln, maxsplit=1)
        y = 420 + i * 66
        last = i == len(rules) - 1
        c = ROSE if last else MUTED
        d.rounded_rectangle([100, y, 1060, y + 56], radius=10, fill=BG_CELL if last else DIM,
                            outline=c, width=4 if last else 2)
        mono(d, 120, y + 28, pat, TEXT if last else MUTED, 24, "lm")
        label(d, 440, y + 28, need, c if last else MUTED, 22, last, "lm")
        label(d, 1040, y + 28, "matches " + path if last else "does not match", c, 22, last, "rm")
    header(d, 1160, 205, "THE RESPONSE", ROSE)
    cell(d, 1160, 262, 300, 120, status, STATUS[status], 64)
    label(d, 1160, 410, why, ROSE, 28, True)
    label(d, 1160, 470, "even for an admin:", MUTED, 26)
    label(d, 1160, 508, "no rule means no", TEXT, 30, True)
    panel(d, 1160, 590, 660, 250, AMBER)
    label(d, 1190, 620, "If the last rule said", TEXT, 26, True)
    mono(d, 1190, 665, "anyRequest().authenticated()", AMBER, 24)
    label(d, 1190, 715, "any login could reach a page", MUTED, 24)
    label(d, 1190, 750, "that someone forgot to protect.", MUTED, 24)


def pre_post(img, d, cfg):
    """@PreAuthorize stops a call before the method runs; @PostAuthorize runs it, then withholds the answer."""
    rules = part("TWO", "THE RULES ON THE METHODS")
    pairs = _pairs("TWO")
    pre, post = pairs[0], pairs[4]
    ran = first("TWO", r"run for bob before his \d+\? (\w+)")
    lanes = [("@PreAuthorize", rules[0], pre, ["check the rule", "STOP", "create() never runs"], ROSE),
             ("@PostAuthorize", rules[1], post, ["view() runs", "check the result", "answer withheld"], AMBER)]
    for k, (ann, rule, ex, steps, c) in enumerate(lanes):
        y = 220 + k * 340
        header(d, 100, y, ann.upper(), c)
        mono(d, 100 + 380, y + 22, re.split(r"\s{2,}", rule)[0] + "  " + re.split(r"\s{2,}", rule)[-1], TEXT, 24, "lm")
        cell(d, 100, y + 80, 380, 110, ex[1], CLIENT, 26, mono_font=False)
        label(d, 290, y + 215, ex[2], MUTED, 24, False, "ma")
        x = 540
        for j, st in enumerate(steps):
            draw.arrow_right(d, x - 50, y + 135, x - 6, MUTED, 4)
            ran_step = (k == 1 and j == 0)
            cc = c if j > 0 or ran_step else CYAN
            cell(d, x, y + 80, 300, 110, st, cc, 26, mono_font=False)
            x += 350
        draw.arrow_right(d, x - 50, y + 135, x - 6, MUTED, 4)
        cell(d, x, y + 80, 150, 110, ex[3], STATUS[ex[3]], 40)
    caption(d, "Did view() run for bob before his 403? %s. Keep side effects out of it." % ran, AMBER, 900)


def refund_steps(img, d, cfg):
    """The separation of duties rule, then four approval requests and their responses."""
    rule = part("THREE", "THE RULE")
    label(d, 960, 215, " ".join(r.strip() for r in rule), TEXT, 26, True, "ma")
    _table(d, 100, 280, _pairs("THREE"), rh=110)
    caption(d, "Dave is an admin, and still cannot approve his own refund.", AMBER, 840)


def blast(img, d, cfg):
    """The staff queue and the role hierarchy, then what one stolen token reaches before and after."""
    hier = first("FOUR", r"THE HIERARCHY: (.+)")
    queue = part("FOUR", "GET /staff/queue")
    before = part("FOUR", "BEFORE THE FIX")
    after = part("FOUR", "AFTER THE FIX")
    hx = header(d, 100, 205, "THE HIERARCHY", CYAN)
    label(d, hx + 20, 227, hier, TEXT, 28, True, "lm")
    for i, ln in enumerate(queue):
        who, status = re.match(r"(.+?\))\s+(\d+)", ln).groups()
        x = 100 + i * 440
        cell(d, x, 280, 300, 60, who, CLIENT, 22, mono_font=False)
        cell(d, x + 310, 280, 100, 60, status, STATUS[status], 28)
    label(d, 100, 360, "GET /staff/queue needs the role SUPPORT", MUTED, 24)
    total = int(re.search(r"of (\d+)", before[0]).group(1))
    for k, (name, lines, c) in enumerate([("BEFORE THE FIX: THE EXPORT RUNS AS DAVE, THE ADMIN", before, ROSE),
                                         ("AFTER THE FIX: THE EXPORTER SERVICE ACCOUNT", after, GREEN)]):
        y = 430 + k * 230
        header(d, 100, y, name, c)
        reach_line = next(ln for ln in lines if "reaches" in ln)
        n = int(re.search(r"reaches (\d+)", reach_line).group(1))
        for i in range(total):
            x = 100 + i * 82
            d.rounded_rectangle([x, y + 64, x + 70, y + 134], radius=8, fill=c if i < n else DIM,
                                outline=c if i < n else LINE, width=2)
        label(d, 100 + total * 82 + 20, y + 99, "%d of %d" % (n, total), c, 40, True, "lm")
        extra = [ln for ln in lines if ln.startswith("it holds") or ln.startswith("GET")]
        label(d, 100, y + 150, "   ".join(extra) if extra else reach_line.split("<-")[0].strip(), MUTED, 24)
    caption(d, first("FOUR", r"^(each count .+)$"), MUTED, 910)


def timeline(img, d, cfg):
    """A grant from tick 10 to tick 12, both included, and the requests made along the clock."""
    m = re.search(r"from tick (\d+) to tick (\d+)", " ".join(act("FIVE")))
    start, end = int(m.group(1)), int(m.group(2))
    who = part("FIVE", "THE GRANT")[0]
    checks = [re.match(r"tick (\d+)\s+(\w+)\s+(\d+)\s+(.+)$", ln).groups() for ln in part("FIVE", "THE REQUESTS")]
    header(d, 100, 210, "THE GRANT", CLIENT)
    label(d, 330, 232, who + ", ticks %d to %d, both included" % (start, end), TEXT, 30, True, "lm")
    lo, hi = start - 3, end + 3
    x0, x1, y = 160, 1760, 470
    step = (x1 - x0) / (hi - lo)
    xt = lambda t: x0 + (t - lo) * step
    d.rounded_rectangle([xt(start) - step / 2 + 4, y - 60, xt(end) + step / 2 - 4, y], radius=10, fill=GREEN)
    d.text(((xt(start) + xt(end)) / 2, y - 30), "carol: payments:write", font=font(MONO_B, 26), fill=INK, anchor="mm")
    d.line([(x0, y + 30), (x1, y + 30)], fill=MUTED, width=4)
    for t in range(lo, hi + 1):
        d.line([(xt(t), y + 18), (xt(t), y + 42)], fill=MUTED, width=3)
        label(d, xt(t), y + 62, str(t), MUTED, 24, False, "ma")
    label(d, x1, y + 100, "tick (the demo's clock)", MUTED, 22, False, "ra")
    for t, user, status, why in checks:
        d.line([(xt(int(t)), y + 30), (xt(int(t)), 720)], fill=STATUS[status], width=3)
    header(d, 100, 640, "THE REQUESTS: PUT /payments/P-1", AMBER)
    for i, (t, user, status, why) in enumerate(sorted(checks, key=lambda c: int(c[0]))):
        c = STATUS[status]
        cx = 100 + i * 350
        cell(d, cx, 720, 320, 80, "tick %s %s: %s" % (t, user, status), c, 26)
        label(d, cx + 160, 820, why, MUTED, 24, False, "ma")
    caption(d, "After tick %d the grant is gone: nobody had to remember to remove it." % end, AMBER, 900)
