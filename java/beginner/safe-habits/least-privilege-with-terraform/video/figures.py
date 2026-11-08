"""Pictures for the least privilege with Terraform video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import fnmatch
import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
DIM = (18, 26, 44)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def part(name, head):
    """The indented lines under the first section header of an act that starts with `head`, two spaces removed."""
    out, on = [], False
    for ln in act(name):
        if not ln.startswith("  "):
            if on:
                break
            on = ln.startswith(head)
        elif on:
            out.append(ln[2:])
    return out


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def statements(lines):
    """Join wrapped statement lines: [(allow, [actions], [resources])]."""
    out, cur = [], None
    for ln in lines:
        if ln.startswith(("Allow ", "Deny ")):
            cur = [ln]
            out.append(cur)
        elif cur is not None and ln.startswith("  "):
            cur.append(ln.strip())
    result = []
    for parts in out:
        text = " ".join(parts)
        m = re.match(r"(Allow|Deny) (.+?) on (.+)$", text)
        result.append((m.group(1) == "Allow", [a.strip() for a in m.group(2).split(",")],
                       [r.strip() for r in m.group(3).split(",")]))
    return result


def requests(lines):
    """[(action, resource, decision)] from lines such as "s3:GetObject on shop-reports/q3.csv   ALLOW"."""
    out = []
    for ln in lines:
        m = re.match(r"(\S+) on (\S+)\s+(\w+)$", ln.strip())
        if m:
            out.append(m.groups())
    return out


def matches(st, action, resource):
    """Which statement a request matches: actions ignore case, resources do not."""
    _, actions, resources = st
    return (any(fnmatch.fnmatchcase(action.lower(), a.lower()) for a in actions)
            and any(fnmatch.fnmatchcase(resource, r) for r in resources))


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def statement_cell(d, x, y, w, st, n=None, size=22):
    allow, actions, resources = st
    c = GREEN if allow else ROSE
    text = "%s%s %s on %s" % ("%d  " % n if n else "", "Allow" if allow else "Deny", ", ".join(actions), ", ".join(resources))
    cell(d, x, y, w, 60, text, c, size)


# ------------------------------------------------------------------ figures

def request_flow(img, d, cfg):
    """How one request is decided: deny first, then an allow, then the boundary."""
    action, resource, decision = requests(part("TWO", "THE REQUESTS"))[0]
    panel(d, 90, 230, 520, 150, CLIENT, 3)
    label(d, 350, 262, "THE REQUEST", CLIENT, 26, True, "mm")
    fit_mono(d, 350, 310, action, TEXT, 30, 480, "mm")
    fit_mono(d, 350, 350, "on " + resource, MUTED, 24, 480, "mm")
    steps = [("1. Does any statement DENY it?", "yes", "EXPLICIT_DENY", ROSE),
             ("2. Does a policy ALLOW it?", "no", "IMPLICIT_DENY", AMBER),
             ("3. Boundary set? Does it ALLOW it too?", "no", "IMPLICIT_DENY", AMBER)]
    y = 240
    for q, ans, result, c in steps:
        panel(d, 760, y, 640, 110, LINE)
        label(d, 785, y + 55, q, TEXT, 26, True, "lm")
        draw.arrow_right(d, 1410, y + 55, 1500, c, 4)
        label(d, 1455, y + 22, ans, c, 22, True, "mm")
        cell(d, 1510, y + 25, 330, 60, result, c, 24)
        if y < 500:
            draw.arrow(d, 1080, y + 112, 1080, y + 150, MUTED, 4)
            label(d, 1100, y + 130, "otherwise", MUTED, 20, False, "lm")
        y += 150
    draw.arrow_right(d, 620, 305, 750, MUTED, 4)
    draw.arrow(d, 1080, 692, 1080, 750, GREEN, 4)
    cell(d, 900, 760, 360, 70, decision, GREEN, 30)
    label(d, 1290, 795, "passes all three: allowed", GREEN, 26, True, "lm")
    caption(d, "Nothing is allowed until a statement says so, and a deny always wins.", AMBER, 880)


def _services():
    """[(service, count)] from "s3 8, dynamodb 10, ..."."""
    return [(s.split()[0], int(s.split()[1])) for s in part("ONE", "THE ACCOUNT")[0].split(", ")]


def _grid(d, x, y, lit, colour, everything=False):
    """One row per service, one cell per thing it offers; `lit` is {service: cells filled}."""
    for i, (service, n) in enumerate(_services()):
        yy = y + i * 72
        mono(d, x + 150, yy + 30, service, TEXT, 24, "rm")
        for j in range(n):
            on = everything or j < lit.get(service, 0)
            c = colour if on else LINE
            d.rounded_rectangle([x + 165 + j * 66, yy + 4, x + 165 + j * 66 + 58, yy + 58], radius=8,
                                fill=c if on else DIM, outline=c, width=2)


def blast_grid(img, d, cfg):
    """25 things a stolen key could do: all of them before the fix, three after."""
    before = heading("ONE", "BEFORE")
    after = heading("ONE", "AFTER")
    sts = statements(part("ONE", "AFTER"))
    lit = {}
    for _, actions, _ in sts:
        svc = actions[0].split(":")[0]
        lit[svc] = lit.get(svc, 0) + 1
    header(d, 60, 205, before.rstrip(":"), ROSE)
    header(d, 1000, 205, after.rstrip(":"), GREEN)
    _grid(d, 40, 280, {}, ROSE, everything=True)
    _grid(d, 980, 280, lit, GREEN)
    bline = next(ln for ln in part("ONE", "BEFORE") if "reaches" in ln)
    aline = next(ln for ln in part("ONE", "AFTER") if "reaches" in ln)
    label(d, 480, 725, bline.split("<-")[0].strip(), ROSE, 28, True, "ma")
    label(d, 1420, 725, aline.strip(), GREEN, 28, True, "ma")
    for i, (_, actions, resources) in enumerate(sts):
        mono(d, 1145, 780 + i * 36, "%s on %s" % (actions[0], resources[0]), GREEN, 22)
    caption(d, part("ONE", "THE ACCOUNT")[1].strip() + ": one cell each.", AMBER, 900)


def matching(img, d, cfg):
    """Six requests against the exporter's two S3 statements, and which statement each one matched."""
    sts = statements(part("TWO", "THE POLICY AS WRITTEN"))
    header(d, 80, 200, "THE POLICY AS WRITTEN", CYAN)
    for i, st in enumerate(sts):
        statement_cell(d, 80 + i * 900, 260, 860, st, i + 1, 24)
    header(d, 80, 350, "THE REQUESTS", AMBER)
    header(d, 1420, 350, "THE DECISION", AMBER)
    for i, (action, resource, decision) in enumerate(requests(part("TWO", "THE REQUESTS"))):
        y = 410 + i * 76
        hit = next((n for n, st in enumerate(sts, 1) if matches(st, action, resource)), None)
        c = GREEN if decision == "ALLOW" else AMBER
        panel(d, 80, y, 820, 62, LINE)
        mono(d, 100, y + 31, "%s on %s" % (action, resource), TEXT, 24, "lm")
        draw.arrow_right(d, 910, y + 31, 1000, MUTED, 3)
        label(d, 1010, y + 31, "matches statement %d" % hit if hit else "matches nothing", c, 24, True, "lm")
        cell(d, 1420, y + 4, 400, 54, decision, c, 24)
    caption(d, "/* is the files in a bucket, never the bucket; action names ignore case.", AMBER, 885)


def deny_wins(img, d, cfg):
    """Allow * on * and a deny on the secrets: for each request, which statement matched, and who wins."""
    sts = statements(part("THREE", "THE POLICY AS WRITTEN"))
    header(d, 80, 200, "THE POLICY AS WRITTEN", CYAN)
    for i, st in enumerate(sts):
        statement_cell(d, 80, 260 + i * 74, 1100, st, i + 1, 24)
    header(d, 80, 430, "THE REQUESTS", AMBER)
    label(d, 1010, 452, "1 allow", GREEN, 24, True, "mm")
    label(d, 1180, 452, "2 deny", ROSE, 24, True, "mm")
    for i, (action, resource, decision) in enumerate(requests(part("THREE", "THE REQUESTS"))):
        y = 490 + i * 80
        panel(d, 80, y, 820, 64, LINE)
        mono(d, 100, y + 32, "%s on %s" % (action, resource), TEXT, 24, "lm")
        for j, st in enumerate(sts):
            hit = matches(st, action, resource)
            c = (GREEN if st[0] else ROSE) if hit else LINE
            cell(d, 950 + j * 170, y + 6, 120, 52, "yes" if hit else "-", c, 26)
        c = GREEN if decision == "ALLOW" else ROSE
        cell(d, 1330, y + 6, 420, 52, decision, c, 26)
    counts = part("THREE", "THE COUNT")
    for i, ln in enumerate(counts):
        m = re.match(r"(.+?)\s+(\d+) of (\d+)", ln.strip())
        y = 760 + i * 60
        label(d, 80, y + 22, m.group(1), MUTED, 26, True, "lm")
        n, total = int(m.group(2)), int(m.group(3))
        c = ROSE if n == total else AMBER
        d.rounded_rectangle([380, y, 380 + n * 50, y + 44], radius=8, fill=c)
        d.rounded_rectangle([380, y, 380 + total * 50, y + 44], radius=8, outline=LINE, width=2)
        label(d, 1660, y + 22, "%d of %d" % (n, total), c, 28, True, "lm")
    caption(d, "The deny wins, whatever the order; it still leaves %s things open." % re.search(r"(\d+) of", counts[-1]).group(1),
            AMBER, 900)


def boundary(img, d, cfg):
    """A boundary is a ceiling: the policy and the boundary must both allow, and only the overlap is left."""
    policy = statements(part("ONE", "BEFORE"))
    bound = statements(part("FOUR", "THE BOUNDARY"))
    left = part("FOUR", "THE %s THINGS LEFT" % re.search(r"THE (\d+) THINGS", " ".join(act("FOUR"))).group(1))
    header(d, 60, 200, "THE POLICY (wildcard)", ROSE)
    panel(d, 60, 260, 440, 120, ROSE, 3)
    for st in policy:
        mono(d, 280, 320, "%s %s on %s" % ("Allow" if st[0] else "Deny", ", ".join(st[1]), ", ".join(st[2])), ROSE, 28, "mm")
    label(d, 560, 320, "AND", AMBER, 36, True, "mm")
    header(d, 620, 200, "THE BOUNDARY (a ceiling)", CYAN)
    panel(d, 620, 260, 520, 330, CYAN, 3)
    y = 280
    for allow, actions, resources in bound:
        c = GREEN if allow else ROSE
        label(d, 645, y, ("Allow" if allow else "Deny") + "  on " + ", ".join(resources), c, 22, True)
        y += 34
        for a in actions:
            mono(d, 670, y, a, TEXT, 22)
            y += 30
        y += 8
    label(d, 1200, 320, "=", AMBER, 44, True, "mm")
    header(d, 1260, 200, "THE %d THINGS LEFT" % len(left), GREEN)
    panel(d, 1260, 260, 600, 330, GREEN, 3)
    for i, ln in enumerate(left):
        mono(d, 1285, 278 + i * 38, ln.strip(), GREEN, 22)
    header(d, 60, 630, "THE COUNT (what a stolen key reaches)", AMBER)
    for i, ln in enumerate(part("FOUR", "THE COUNT")):
        m = re.match(r"(.+?)\s+(\d+) of (\d+)", ln.strip())
        n, total = int(m.group(2)), int(m.group(3))
        x = 60 + (i % 2) * 920
        y = 700 + (i // 2) * 70
        c = ROSE if n == total else GREEN
        label(d, x, y + 22, m.group(1), MUTED, 22, True, "lm")
        for k in range(total):
            d.rounded_rectangle([x + 410 + k * 16, y + 6, x + 410 + k * 16 + 12, y + 38], radius=3,
                                fill=c if k < n else DIM, outline=c if k < n else LINE)
        label(d, x + 850, y + 22, str(n), c, 28, True, "mm")
    caption(d, "Allowed only if a policy AND the boundary allow it. A boundary alone grants nothing.", AMBER, 880)


def escalation(img, d, cfg):
    """The linter's findings, and the deployer's three steps from 3 of 25 to any role."""
    header(d, 60, 200, "THE FINDINGS", AMBER)
    for i, ln in enumerate(part("FIVE", "THE FINDINGS")):
        name, codes = ln.strip().split(None, 1)
        y = 260 + i * 70
        c = GREEN if codes == "none" else ROSE if ("ESCALATION" in codes or "ACTION_STAR" in codes) else AMBER
        panel(d, 60, y, 700, 58, c)
        mono(d, 80, y + 29, name, TEXT, 22, "lm")
        fit_mono(d, 300, y + 29, codes, c, 20, 440, "lm")
    sts = statements(part("FIVE", "THE DEPLOYER"))
    reach = next(ln for ln in part("FIVE", "THE DEPLOYER") if "reaches" in ln)
    header(d, 840, 200, "THE DEPLOYER AS WRITTEN", CYAN)
    for i, st in enumerate(sts):
        statement_cell(d, 840, 260 + i * 70, 1020, st, None, 20)
    label(d, 840, 410, reach.strip(), AMBER, 26, True)
    header(d, 840, 470, "THE WAY TO MORE POWER", ROSE)
    steps = part("FIVE", "THE WAY TO MORE POWER")
    for i, ln in enumerate(steps):
        y = 530 + i * 95
        what, why = ln.strip().split(": ", 1)
        panel(d, 840, y, 1020, 76, ROSE)
        fit_mono(d, 865, y + 8, what, TEXT, 24, 980)
        label(d, 900, y + 56, why, MUTED, 22, True, "lm")
        if i < len(steps) - 1:
            draw.arrow(d, 1350, y + 77, 1350, y + 94, ROSE, 3)
    caption(d, "3 of 25 by count, and still any role in the account: read the policy too.", AMBER, 880)
