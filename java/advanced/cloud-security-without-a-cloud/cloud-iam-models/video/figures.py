"""Pictures for the cloud IAM models video, drawn by the `figure` slide kind.

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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def cols(line):
    return re.split(r"\s{2,}", line.strip())


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


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def answer_colour(text):
    t = text.strip()
    return GREEN if t.startswith(("ALLOW", "yes")) else ROSE if t.startswith(("DENY", "no")) else MUTED


# ------------------------------------------------------------------ figures

def decision_ladder(img, d, cfg):
    """Requests 1 and 3 of ana walk the seven questions of the evaluation order, top to bottom."""
    asks = {cols(ln)[0]: cols(ln)[1:] for ln in section("ONE", "THE REQUESTS")}
    rows = [cols(ln) for ln in section("ONE", "HOW REQUESTS")]
    names = rows[0][1:]
    header(d, 60, 205, "THE REQUESTS", CYAN)
    for k, n in enumerate(names):
        x = 980 + k * 450
        label(d, x + 200, 200, "request %s: %s" % (n, asks[n][0]), CYAN, 26, True, "ma")
        fit_mono(d, x + 200, 236, asks[n][1], TEXT, 24, 430, "ma")
    header(d, 60, 275, "THE QUESTIONS, ASKED TOP TO BOTTOM", AMBER)
    for i, r in enumerate(rows[1:-1]):
        y = 340 + i * 70
        panel(d, 60, y, 880, 60)
        label(d, 90, y + 30, "%d  %s" % (i + 1, r[0]), TEXT, 28, True, "lm")
        for k, a in enumerate(r[1:]):
            if a == "-":
                colour, a = LINE, "not asked"
            elif a in ("none", "skipped"):
                colour = MUTED
            elif i == 0:
                colour = ROSE if a == "yes" else GREEN
            else:
                colour = answer_colour(a)
            cell(d, 980 + k * 450, y, 400, 60, a, colour, 26)
    header(d, 60, 850, "THE ANSWER", GREEN)
    for k, a in enumerate(rows[-1][1:]):
        cell(d, 980 + k * 450, 840, 400, 70, a, answer_colour(a), 34)
    caption(d, "A matching Deny ends the walk at once; otherwise something must allow.", AMBER, 940)


def identity_requests(img, d, cfg):
    """ana's policy on the left, her six requests and their answers on the right."""
    pol = section("ONE", "THE POLICY reports-read")
    header(d, 60, 180, "THE POLICY reports-read", CYAN)
    y = 250
    for ln in pol:
        effect = ln.split()[0] if ln.split()[0] in ("Allow", "Deny") else None
        if effect:
            panel(d, 60, y, 640, 150 if effect == "Allow" else 110, GREEN if effect == "Allow" else ROSE, 3)
            label(d, 85, y + 18, effect, GREEN if effect == "Allow" else ROSE, 32, True)
            yy = y + 62
            y += 170 if effect == "Allow" else 130
        fit_mono(d, 85, yy, ln.replace(effect or "", "", 1).strip(), TEXT, 24, 590)
        yy += 40
    asks = [cols(ln) for ln in section("ONE", "THE REQUESTS")]
    answers = [cols(ln) for ln in section("ONE", "THE ANSWERS")]
    header(d, 760, 180, "THE REQUESTS", CYAN)
    header(d, 1560, 180, "THE ANSWERS", GREEN)
    for i, (q, a) in enumerate(zip(asks, answers)):
        y = 250 + i * 95
        panel(d, 760, y, 760, 76)
        fit_mono(d, 780, y + 8, "%s  %s" % (q[0], q[1]), MUTED, 24, 720)
        fit_mono(d, 820, y + 40, q[2], TEXT, 24, 680)
        cell(d, 1560, y + 6, 300, 64, a[1], answer_colour(a[1]), 30)
    why = section("ONE", "WHY 2 AND 6")
    for i, ln in enumerate(why):
        fit_mono(d, 60, 820 + i * 46, ln.strip(), AMBER, 26, 1800)
    caption(d, "No Allow means DENY; a matching Deny beats every Allow.", AMBER, 950)


def accounts_grid(img, d, cfg):
    """The six cases of ben's request: one account against two, and what the rule works out to."""
    rows = [cols(ln) for ln in section("TWO", "THE ANSWERS (which")][1:]
    rule = [cols(ln) for ln in section("TWO", "HOW THE RULE")]
    for k, where in enumerate(("same account", "other account")):
        x = 60 + k * 920
        r = rule[k]
        panel(d, x, 180, 880, 580, GREEN if k == 0 else AMBER, 3)
        label(d, x + 30, 200, "THE BUCKET IN THE %s" % where.upper(), GREEN if k == 0 else AMBER, 30, True)
        label(d, x + 30, 250, "allowed when identity %s bucket policy" % r[1].split()[1], TEXT, 28)
        label(d, x + 200, 310, "identity", MUTED, 24, True, "ma")
        label(d, x + 420, 310, "bucket", MUTED, 24, True, "ma")
        label(d, x + 690, 310, "answer", MUTED, 24, True, "ma")
        for i, c in enumerate(rr for rr in rows if rr[0] == where):
            y = 360 + i * 110
            cell(d, x + 100, y, 200, 80, c[1], answer_colour(c[1]), 30)
            cell(d, x + 320, y, 200, 80, c[2], answer_colour(c[2]), 30)
            draw.arrow_right(d, x + 535, y + 40, x + 570, MUTED, 4)
            cell(d, x + 580, y, 220, 80, c[3], answer_colour(c[3]), 32)
        label(d, x + 440, 700, r[2], GREEN if k == 0 else AMBER, 30, True, "ma")
    pub = [cols(ln) for ln in section("TWO", "THE ANSWERS for")]
    header(d, 60, 780, "THE BUCKET POLICY SAYS Principal *", ROSE)
    for i, p in enumerate(pub):
        x = 60 + i * 620
        who, what = p[0].split(", ", 1)
        label(d, x, 840, who, TEXT, 28, True)
        label(d, x, 880, what, MUTED, 24)
        cell(d, x + 410, 840, 170, 64, p[1], answer_colour(p[1]), 28)
    caption(d, "One account: either side is enough. Two accounts: both must say yes.", AMBER, 950)


def layers_funnel(img, d, cfg):
    """Eight actions through the role's policy, the boundary and the session policy: 6, then 4, then 2."""
    rows = [cols(ln) for ln in section("THREE", "THE EIGHT ACTIONS")]
    head, body = rows[0], [r for r in rows[1:] if len(r) == 4 and r[0] != "allowed"]
    total = next(r for r in rows if r[0] == "allowed")
    layers = [cols(ln) for ln in section("THREE", "THE TWO LAYERS")]
    header(d, 60, 205, "THE ACTIONS", CYAN)
    names = ["the role's policy", "+ the boundary", "+ the session policy"]
    subs = [section("THREE", "THE ROLE")[2].split("Allow ")[1], layers[0][1].split("Allow ")[1], layers[1][1].split("Allow ")[1]]
    for k in range(3):
        x = 620 + k * 430
        label(d, x + 190, 196, names[k], AMBER, 28, True, "ma")
        fit_mono(d, x + 190, 236, subs[k], MUTED, 22, 420, "ma")
    for i, r in enumerate(body):
        y = 280 + i * 66
        panel(d, 60, y, 520, 58)
        fit_mono(d, 85, y + 29, r[0], TEXT, 28, 480, "lm")
        for k in range(3):
            cell(d, 620 + k * 430, y, 380, 58, "allowed" if r[1 + k] == "yes" else "denied", answer_colour(r[1 + k]), 26)
    y = 280 + len(body) * 66 + 20
    label(d, 85, y + 30, "allowed", AMBER, 32, True, "lm")
    for k in range(3):
        cell(d, 620 + k * 430, y, 380, 66, total[1 + k] + " of %d" % len(body), AMBER, 34)
        if k:
            draw.arrow_right(d, 620 + k * 430 - 45, y + 33, 620 + k * 430 - 5, AMBER, 4)
    caption(d, "Each layer can only take away: what is left must be allowed by all three.", AMBER, 950)


def deputy_flow(img, d, cfg):
    """Before the fix ReportCo reads the bank's files for Mallory; after it, the external id stops her."""
    before = [cols(ln) for ln in section("FOUR", "THE ANSWERS before")][:2]
    after = [cols(ln) for ln in section("FOUR", "THE ANSWERS after")][:2]
    cond = section("FOUR", "AFTER THE FIX")[0].strip()
    service = cols(section("FOUR", "THE SETUP")[0])
    for k, (title, colour) in enumerate((("BEFORE THE FIX", ROSE), ("AFTER THE FIX", GREEN))):
        y0 = 180 + k * 380
        header(d, 60, y0, title, colour)
        if k == 0:
            fit_mono(d, 400, y0 + 22, "trust: " + section("FOUR", "BEFORE THE FIX")[0].strip(), MUTED, 24, 1400, "lm")
        else:
            fit_mono(d, 400, y0 + 22, "trust also: " + cond, MUTED, 24, 1400, "lm")
        for i in range(2):
            y = y0 + 80 + i * 140
            who = ("the bank", "Mallory")[i]
            note = "names the bank's role" if k == 0 else "sends " + after[i][1].split()[1]
            draw.box(d, 60, y, 320, 110, who, note, CYAN if i == 0 else ROSE)
            draw.arrow_right(d, 390, y + 55, 450, MUTED, 4)
            draw.box(d, 460, y, 360, 110, service[0], "the deputy, " + service[1].split(", ")[-1], AMBER)
            draw.arrow_right(d, 830, y + 55, 890, MUTED, 4)
            draw.box(d, 900, y, 400, 110, "ReportCoAccess", "the bank's role: bank-reports/*", CYAN)
            ans = (before if k == 0 else after)[i]
            word = next(w for w in ans if w.startswith(("ALLOW", "DENY")))
            cell(d, 1340, y + 15, 200, 80, word.split()[0], answer_colour(word), 34)
            if k == 0 and i == 1:
                label(d, 1570, y + 55, "FOOLED", ROSE, 34, True, "lm")
            if k == 1:
                label(d, 1570, y + 55, after[i][2] + " ext-bank-7f3a", answer_colour(word), 26, True, "lm")
    caption(d, "The deputy uses its own rights: the id it issued tells it whose request this is.", AMBER, 950)


def escalation_paths(img, d, cfg):
    """The paths the search found from ana, ben and carol to administrator."""
    lines = section("FIVE", "THE PATHS")
    paths, who = {}, None
    for ln in lines:
        if not ln.startswith(" "):
            who = ln.split()[0]
            paths[who] = [ln[len(who):].strip()]
        else:
            paths[who].append(ln.strip())
    policies, name = {}, None
    for ln in section("FIVE", "THE POLICIES"):
        if not ln.startswith(" "):
            name = cols(ln)[0]
            policies[name] = cols(ln)[1]
        else:
            policies[name] += " " + ln.strip()
    header(d, 60, 200, "WHO CAN MAKE THEMSELVES AN ADMINISTRATOR", AMBER)
    for i, (name, steps) in enumerate(paths.items()):
        y = 280 + i * 215
        draw.box(d, 60, y, 220, 120, name, "user", CYAN)
        fit_mono(d, 60, y + 135, "policy: " + policies[name], MUTED, 24, 1800)
        x = 300
        for s in steps:
            text = s.lstrip("+-=> ").strip()
            colour = ROSE if s.startswith("=>") else GREEN if s == "no path found" else TEXT
            w = max(220, 15 * len(text) + 40)
            draw.arrow_right(d, x, y + 60, x + 40, MUTED, 4)
            cell(d, x + 50, y + 25, w, 70, text, colour, 24)
            x += w + 60
    wild = section("FIVE", "THE WILDCARDS")[-1].strip()
    caption(d, wild[0].upper() + wild[1:] + ".", AMBER, 950)
