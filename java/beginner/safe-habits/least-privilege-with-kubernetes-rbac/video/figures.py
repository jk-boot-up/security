"""Pictures for the least privilege with Kubernetes RBAC video, drawn by the `figure` slide kind.

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

def _space():
    """The verbs and resources of the namespace, from act one."""
    lines = part("ONE", "THE NAMESPACE")
    verbs = lines[0].split()[2:]
    resources = lines[1].split()[2:] + lines[2].split()
    return verbs, resources


def _rules(lines):
    """[(verbs, resources, named)] from lines such as "rule 2  get on configmaps named export-settings"."""
    out = []
    for ln in lines:
        m = re.match(r"rule \d+\s+(.+?) on (.+?)(?: named (.+))?$", ln.strip())
        if m:
            out.append(([v.strip() for v in m.group(1).split(",")], [r.strip() for r in m.group(2).split(",")], m.group(3)))
    return out


def _grid(d, x, y, rules, colour, everything=False, cw=88, rh=50, mark=None):
    """Verbs across, resources down; a filled cell is allowed. Named-only cells are outlined in amber."""
    verbs, resources = _space()
    for j, v in enumerate(verbs):
        label(d, x + 190 + j * cw + cw // 2, y, v, MUTED, 20, True, "mm")
    count = 0
    for i, r in enumerate(resources):
        yy = y + 24 + i * rh
        mono(d, x + 176, yy + rh // 2, r, ROSE if r == mark else TEXT, 21, "rm")
        for j, v in enumerate(verbs):
            on = everything or any((v in vs or "*" in vs) and (r in rs or "*" in rs) and not named for vs, rs, named in rules)
            named = any(v in vs and r in rs and named for vs, rs, named in rules)
            c = (ROSE if r == mark else colour) if on else (AMBER if named else LINE)
            count += on
            d.rounded_rectangle([x + 190 + j * cw + 4, yy + 4, x + 190 + (j + 1) * cw - 4, yy + rh - 4], radius=6,
                                fill=c if on else DIM, outline=c, width=3 if named else 1)
    return count


def rbac_chain(img, d, cfg):
    """How RBAC grants a permission: an identity, a binding, a role, its rules, and one question."""
    who = re.search(r"\((\S+), with the role (\S+)\)", " ".join(act("ONE")))
    account, role = who.group(1), who.group(2)
    rules = [ln.strip() for ln in part("ONE", "AFTER THE FIX") if ln.strip().startswith("rule")]
    checks = part("ONE", "THE CHECKS")
    boxes = [("SERVICE ACCOUNT", account, "the job's identity", CLIENT),
             ("ROLE BINDING", "in namespace shop", "joins one to the other", AMBER),
             ("ROLE", role, "a list of rules", GREEN)]
    x = 100
    for i, (kind, name, note, c) in enumerate(boxes):
        panel(d, x, 220, 460, 150, c, 3)
        label(d, x + 230, 255, kind, c, 26, True, "mm")
        fit(d, x + 230, 302, name, TEXT, 32, 430, True, "mm")
        label(d, x + 230, 345, note, MUTED, 22, False, "mm")
        if i < 2:
            draw.arrow_right(d, x + 470, 295, x + 560, MUTED, 4)
        x += 580
    header(d, 1260, 400, "THE RULES", GREEN)
    for i, ln in enumerate(rules):
        cell(d, 1260, 460 + i * 80, 560, 64, ln.split("  ", 1)[1], GREEN, 22)
    header(d, 100, 400, "THE QUESTIONS (can %s ...?)" % account, CYAN)
    for i, ln in enumerate(checks):
        q, a = ln.rsplit(None, 1)
        y = 460 + i * 100
        c = GREEN if a == "true" else ROSE
        panel(d, 100, y, 640, 80, CYAN)
        mono(d, 125, y + 40, q.strip(), TEXT, 24, "lm")
        draw.arrow_right(d, 750, y + 40, 840, MUTED, 4)
        cell(d, 850, y, 200, 80, a, c, 34)
    caption(d, "Nothing is allowed until a rule says so; a rule can even name one object.", AMBER, 840)


def blast_grid(img, d, cfg):
    """56 things a token could do: all of them for cluster-admin, two for the scoped exporter."""
    before = part("ONE", "BEFORE THE FIX")
    after = part("ONE", "AFTER THE FIX")
    header(d, 100, 205, "BEFORE THE FIX: cluster-admin", ROSE)
    header(d, 1010, 205, "AFTER THE FIX: order-exporter", GREEN)
    _grid(d, 60, 290, [], ROSE, everything=True)
    _grid(d, 970, 290, _rules(after), GREEN)
    label(d, 470, 735, before[0].split("<-")[0].strip(), ROSE, 30, True, "ma")
    label(d, 1380, 735, next(ln for ln in after if "reaches" in ln).strip(), GREEN, 28, True, "ma")
    label(d, 1380, 780, "amber outline: allowed for one named object only", AMBER, 22, False, "ma")
    v, r = _space()
    caption(d, "%d verbs x %d resources = %d things a stolen token could do." % (len(v), len(r), len(v) * len(r)), AMBER, 860)


def wildcard(img, d, cfg):
    """A role that says get and list on * reaches every resource, secrets included."""
    rules = _rules(part("TWO", "THE ROLE AS WRITTEN"))
    allows = part("TWO", "WHAT IT REALLY ALLOWS")
    header(d, 100, 205, "THE ROLE AS WRITTEN", CYAN)
    cell(d, 100, 270, 560, 80, "rule 1  %s on %s" % (", ".join(rules[0][0]), ", ".join(rules[0][1])), CYAN, 28)
    label(d, 100, 380, "\"read everything\": it sounds", MUTED, 28)
    label(d, 100, 420, "read-only and harmless", MUTED, 28)
    header(d, 100, 500, "WHAT IT REALLY ALLOWS", ROSE)
    for i, ln in enumerate(allows):
        m = re.match(r"(.+?)\s{2,}(true|false)", ln)
        y = 565 + i * 90
        if m:
            c = ROSE if m.group(2) == "true" else GREEN
            panel(d, 100, y, 460, 70, c)
            mono(d, 120, y + 35, m.group(1), TEXT, 26, "lm")
            cell(d, 580, y, 160, 70, m.group(2), c, 30)
        else:
            label(d, 100, y + 20, ln.strip(), AMBER, 28, True)
    _grid(d, 920, 230, rules, AMBER, mark="secrets")
    label(d, 1415, 680, "secrets, in red: every password and key", ROSE, 24, True, "ma")
    caption(d, "* also covers kinds of object that are added later.", AMBER, 860)


def escalation(img, d, cfg):
    """The deployer's rules, and the four ways each one leads to more power than it says."""
    rules = [ln.strip() for ln in part("THREE", "THE ROLE")]
    paths = part("THREE", "THE ESCALATION PATHS")
    header(d, 100, 205, "THE ROLE AS WRITTEN (deployer)", CYAN)
    for i, ln in enumerate(rules):
        cell(d, 100, 265 + i * 74, 820, 60, ln.split("  ", 1)[1], CYAN, 22)
    head = [ln for ln in act("THREE") if ln.startswith("THE ESCALATION PATHS")][0]
    counts = re.findall(r"([\w-]+) (\d+)", head.split("(")[1])
    header(d, 1000, 205, "THE ESCALATION PATHS", ROSE)
    for i, (name, n) in enumerate(counts):
        c = GREEN if n == "0" else ROSE
        cell(d, 1000 + i * 420, 265, 400, 60, "%s: %s" % (name, n), c, 26)
    leads = [("create pods", "create pods", "mount any secret, then read it"),
             ("create role", "create role bindings", "bind itself to any role"),
             ("bind roles", "bind roles", "hand out a role it does not hold"),
             ("edit roles", "escalate: edit roles", "add any permission to a role")]
    for i, ln in enumerate(paths):
        y = 365 + i * 100
        short, lead = next((s2, l2) for k, s2, l2 in leads if ln.strip().startswith(k))
        cell(d, 1000, y, 330, 80, short, ROSE, 24, mono_font=False)
        draw.arrow_right(d, 1340, y + 40, 1400, MUTED, 4)
        fit(d, 1410, y + 40, lead, AMBER, 26, 410, True, "lm")
    last = [ln for ln in act("THREE") if not ln.startswith("  ") and "<-" in ln][0]
    caption(d, last.split("<-")[1].strip().capitalize().replace("pods = read", "pods is the same as read"), AMBER, 860)


def scope(img, d, cfg):
    """A cluster-wide role, bound in one namespace, reaches only that namespace."""
    rb = part("FOUR", "THE ROLE AND THE BINDING")
    checks = part("FOUR", "THE CHECKS")
    contrast = part("FOUR", "BY CONTRAST")
    role = rb[0].split()[0]
    rule = rb[1].strip().split("  ", 1)[1]
    panel(d, 100, 215, 1720, 110, GREEN, 3)
    label(d, 130, 250, "ClusterRole %s (no namespace)" % role, GREEN, 30, True, "lm")
    mono(d, 130, 295, rule, TEXT, 24, "lm")
    for k, ns in enumerate(["shop", "warehouse"]):
        x = 100 + k * 880
        panel(d, x, 370, 840, 420, CYAN if k == 0 else LINE, 3)
        label(d, x + 30, 405, "namespace " + ns, CYAN if k == 0 else MUTED, 28, True, "lm")
        rows = [ln for ln in checks if ln.split()[-2] == ns]
        if k == 0:
            cell(d, x + 30, 445, 780, 60, "RoleBinding: support-agent -> " + role, AMBER, 24)
        for i, ln in enumerate(rows):
            q, a = ln.rsplit(None, 1)
            c = GREEN if a == "true" else ROSE
            y = 530 + i * 80
            mono(d, x + 30, y + 30, "support-agent: " + q.strip(), TEXT, 24, "lm")
            cell(d, x + 640, y, 170, 60, a, c, 28)
        if k == 1:
            q, a = contrast[0].split("<-")[0].rsplit(None, 1)
            cell(d, x + 30, 445, 780, 60, "ClusterRoleBinding: nightly-export-admin", ROSE, 24)
            mono(d, x + 30, 690, "nightly-export-admin: " + q.strip(), TEXT, 22, "lm")
            cell(d, x + 640, 660, 170, 60, a, ROSE, 28)
    caption(d, "The binding sets the scope, not the role.", AMBER, 840)


def findings(img, d, cfg):
    """What the linter finds in the shop's manifests, grouped by the habit behind it."""
    counts = part("FIVE", "THE MANIFESTS")[0].strip()
    found = part("FIVE", "THE FINDINGS")
    header(d, 100, 205, "THE MANIFESTS", CYAN)
    label(d, 420, 227, counts, TEXT, 28, True, "lm")
    header(d, 100, 290, "THE FINDINGS (%d)" % len(found), ROSE)
    colour = {"binds": ROSE, "wildcard": AMBER, "escalation": ROSE, "token": CYAN}
    for i, ln in enumerate(found):
        obj, problem = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        c = next(v for k, v in colour.items() if problem.startswith(k))
        y = 355 + i * 82
        panel(d, 100, y, 1720, 68, c)
        mono(d, 130, y + 34, obj, TEXT, 26, "lm")
        label(d, 760, y + 34, problem, c, 28, True, "lm")
    caption(d, "A linter finds wildcards and cluster-admin, not a wide role spelled out in full.", AMBER, 870)
