"""Pictures for the threat hunting video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The section of an act whose header starts with `head`: (header line, body lines without indent)."""
    lines, cur, title = act(name), None, None
    for ln in lines:
        if not ln.startswith("  "):
            if cur is not None:
                break
            if ln.startswith(head):
                cur, title = [], ln
        elif cur is not None:
            cur.append(ln[2:])
    return title, cur


def body(name, head):
    return section(name, head)[1]


def cols(line):
    return re.split(r"\s{2,}", line.strip())


def num(text):
    return int(re.search(r"\d+", text).group())


def header(d, x, y, text, colour, size=26):
    f = font(SANS_B, size)
    w = int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour, size, max_w=None, anchor="la"):
    f = font(MONO_B, size)
    while max_w and f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=950):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def verdict_colour(text):
    return ROSE if "ATTACK" in text or "true" in text else GREEN if "not an attack" in text or "false" in text else MUTED


def lead_rows(name, head):
    """The leads listed under a section: (subject, verdict)."""
    return [tuple(cols(ln)) for ln in body(name, head) if "ATTACK" in ln or "not an attack" in ln or "alarm" in ln]


# ------------------------------------------------------------------ figures

def hunt_funnel(img, d, cfg):
    """The core idea on hunt 1: a hypothesis, a query, fewer and fewer leads, and a person's judgement."""
    hyp = body("TWO", "THE HYPOTHESIS")[0]
    before = num(body("TWO", "BEFORE RARITY")[0])
    rare_title, _ = section("TWO", "AFTER RARITY")
    rare = num(rare_title.split(":")[-1])
    allow_title, _ = section("TWO", "AFTER THE ALLOWLIST")
    allowed = num(allow_title.split(":")[-1])
    header(d, 60, 215, "THE HYPOTHESIS", CYAN)
    panel(d, 60, 275, 1800, 70, CYAN)
    label(d, 960, 310, hyp, TEXT, 32, True, "mm")
    header(d, 60, 375, "THE LEADS, AS THE QUERY GROWS", AMBER)
    steps = [("regular calls: 50+ calls, variation under 0.30", before, AMBER),
             ("and rare: at most 3 hosts call the domain", rare, AMBER),
             ("and the benign heartbeat allowlisted", allowed, ROSE)]
    for i, (text, n, colour) in enumerate(steps):
        y = 440 + i * 120
        w = max(120, int(860 * n / before))
        label(d, 60, y + 50, text, TEXT, 28, False, "lm")
        d.rounded_rectangle([760, y + 10, 760 + w, y + 90], radius=12, fill=BG_CELL, outline=colour, width=3)
        label(d, 760 + w + 24, y + 50, "%d lead%s" % (n, "" if n == 1 else "s"), colour, 34, True, "lm")
    header(d, 60, 800, "A PERSON JUDGES EACH ONE", GREEN)
    x = 60
    for subject, verdict in lead_rows("TWO", "AFTER RARITY"):
        colour = verdict_colour(verdict)
        cell(d, x, 860, 880, 64, "%s  %s" % (subject, verdict), colour, 24)
        x += 920
    caption(d, "Each condition is a guess about the intruder; each lead costs a person's time.", AMBER, 960)


def week_overview(img, d, cfg):
    """The input: what the week holds, the five planted attacks, and the five benign look-alikes."""
    week = body("ONE", "THE WEEK")
    header(d, 60, 215, "THE WEEK", CYAN)
    records = [ln for ln in week if re.search(r"\d+ (name|process|logins)", ln)]
    y = 280
    for ln in week[:2]:
        k, v = cols(ln)
        label(d, 60, y, k, MUTED, 26)
        label(d, 200, y, v, TEXT, 26)
        y += 44
    counts = [(re.search(r"(\d+) (.*)", ln).group(1), re.search(r"\d+ (.*)", ln).group(1)) for ln in records]
    top = max(int(c) for c, _ in counts)
    for c, what in counts:
        w = max(8, int(560 * int(c) / top))
        d.rounded_rectangle([60, y + 6, 60 + w, y + 46], radius=8, fill=BG_CELL, outline=CYAN, width=2)
        label(d, 80 + w, y + 26, "%s %s" % (c, what), TEXT, 26, True, "lm")
        y += 60
    header(d, 980, 215, "THE PLANTED ATTACKS (THE TRUTH)", ROSE)
    for i, ln in enumerate(body("ONE", "THE PLANTED ATTACKS")):
        m = re.match(r"(\d) (\w+)\s+(.*)", ln)
        y = 280 + i * 62
        hidden = "in no log" in ln
        colour = MUTED if hidden else ROSE
        panel(d, 980, y, 880, 52, colour)
        label(d, 1000, y + 26, "%s %s" % (m.group(1), m.group(2)), colour, 26, True, "lm")
        mono(d, 1160, y + 26, m.group(3), TEXT if not hidden else MUTED, 21, 690, "lm")
    header(d, 60, 620, "THE LOOK-ALIKES (BENIGN, PLANTED ON PURPOSE)", GREEN)
    for i, ln in enumerate(body("ONE", "THE LOOK-ALIKES")):
        host, what = cols(ln)
        x, y = 60 + (i % 2) * 920, 685 + (i // 2) * 70
        panel(d, x, y, 880, 56, GREEN)
        label(d, x + 20, y + 28, host, GREEN, 26, True, "lm")
        mono(d, x + 150, y + 28, what, TEXT, 21, 710, "lm")
    caption(d, "Four attacks leave traces; the fifth leaves none. Benign things look like attacks.", AMBER, 920)


def gap_variation(img, d, cfg):
    """How even the gaps are, worked out for four pairs, on a scale from a perfect timer to random."""
    rows = [re.split(r"\s+", ln.strip()) for ln in body("TWO", "HOW EVEN")[1:]]
    header(d, 60, 215, "THE INPUT: CALL TIMES PER HOST AND DOMAIN", CYAN)
    header(d, 60, 280, "WORKED OUT: VARIATION = SD / MEAN OF THE GAPS", AMBER)
    x0, x1 = 1010, 1700
    for i, r in enumerate(rows):
        pair, calls, mean, sd, var = " ".join(r[:3]), r[3], r[4], r[5], r[6]
        y = 360 + i * 120
        panel(d, 60, y, 880, 100)
        mono(d, 80, y + 30, pair, TEXT, 24, 840, "lm")
        mono(d, 80, y + 72, "%s calls  mean %s s  sd %s s" % (calls, mean, sd), MUTED, 22, 600, "lm")
        mono(d, 920, y + 72, "%s / %s = %s" % (sd, mean, var), AMBER, 22, 300, "rm")
        d.line([(x0, y + 50), (x1, y + 50)], fill=LINE, width=3)
        v = float(var)
        cx = x0 + int((x1 - x0) * min(v, 1.0))
        colour = GREEN if v < 0.3 else MUTED
        d.ellipse([cx - 16, y + 34, cx + 16, y + 66], fill=colour)
        mono(d, cx + 26, y + 22, var, colour, 24, None, "lm")
    for v, text, colour in ((0.0, "0: a perfect timer", GREEN), (0.3, "0.30: the limit", AMBER), (1.0, "about 1: random", MUTED)):
        x = x0 + int((x1 - x0) * v)
        d.line([(x, 345), (x, 830)], fill=colour, width=2)
        label(d, x, 840 + (34 if v == 0.3 else 0), text, colour, 24, True, "ma")
    caption(d, "The beacon's jitter gives 0.06: under 0.30 it is found, under 0.01 it slips through.", AMBER, 945)


def pairs_and_logins(img, d, cfg):
    """Hunt 2: the commonest and rarest program pairs. Hunt 3: an account's hours before and the new login."""
    counts = [ln for ln in body("THREE", "THE COUNTS") if ln.strip() != "..."]
    header(d, 60, 215, "HUNT 2: PARENT -> CHILD, COUNTED", CYAN)
    top = max(num(cols(ln)[1]) for ln in counts)
    for i, ln in enumerate(counts):
        pair, n = cols(ln)
        y = 280 + i * 62 + (30 if i >= 3 else 0)
        rare = int(n) <= 2
        colour = ROSE if rare else MUTED
        mono(d, 60, y + 22, pair, TEXT, 24, 380, "lm")
        w = max(10, int(380 * int(n) / top))
        d.rounded_rectangle([450, y + 4, 450 + w, y + 40], radius=6, fill=BG_CELL, outline=colour, width=2)
        label(d, 470 + w, y + 22, n, colour, 26, True, "lm")
        if i == 2:
            label(d, 60, y + 62, "...", MUTED, 26)
    leads = lead_rows("THREE", "THE LEADS (seen")
    label(d, 60, 720, "The leads, judged:", AMBER, 26, True)
    for i, (subject, verdict) in enumerate(leads):
        mono(d, 60, 770 + i * 40, "%-24s %s" % (subject, verdict), verdict_colour(verdict), 22, 880)
    header(d, 980, 215, "HUNT 3: THE BASELINE (DAYS 0 TO 3)", CYAN)
    base = {cols(ln)[0]: ln for ln in body("THREE", "THE BASELINE")}
    logins = body("THREE", "THE LOGINS")
    news = {}
    for ln in logins:
        m = re.match(r"(\S+) from (\S+)\s+day (\d), hour (\d+)", ln.strip())
        if m:
            news[m.group(1)] = (m.group(2), int(m.group(4)))
    for i, user in enumerate(base):
        y = 290 + i * 300
        hours_text = base[user].split("at hours ")[1]
        if " to " in hours_text:
            a, b = (int(h) for h in hours_text.split(" to "))
            hours = set(range(a, b + 1))
        else:
            hours = {int(h) for h in hours_text.split(", ")}
        ip = re.search(r"from (\S+)", base[user]).group(1).rstrip(",")
        label(d, 980, y, user, TEXT, 30, True)
        label(d, 1200, y + 4, "usual address " + ip, MUTED, 24)
        new_ip, new_hour = news[user]
        for h in range(24):
            x = 980 + h * 36
            colour = GREEN if h in hours else LINE
            fill = BG_CELL
            if h == new_hour:
                colour = ROSE if h not in hours else AMBER
                fill = colour
            d.rounded_rectangle([x, y + 60, x + 32, y + 110], radius=6, fill=fill, outline=colour, width=2)
            if h % 3 == 0:
                label(d, x + 16, y + 120, "%02d" % h, MUTED, 18, False, "ma")
        verdict = "new address AND new hour: a lead" if new_hour not in hours else "new address, usual hour: not a lead"
        label(d, 980, y + 160, "day 4 to 6: from %s at hour %02d" % (new_ip, new_hour), TEXT, 24, True)
        label(d, 980, y + 200, verdict, ROSE if new_hour not in hours else AMBER, 26, True)
    caption(d, "Rare is suspicious, not guilty: cmd -> ipconfig was an administrator.", AMBER, 920)


def volume_days(img, d, cfg):
    """Hunt 4: three servers' days in MB, each host's own limit, and the scoreboard of all four hunts."""
    rows = [re.split(r"\s+", ln.strip()) for ln in body("FOUR", "HOW EACH HOST")[1:3]]
    leads = {s for s, _ in lead_rows("FOUR", "THE LEADS")}
    header(d, 60, 215, "THE INPUT: MB OUT PER DAY", CYAN)
    header(d, 1260, 215, "WORKED OUT", AMBER)
    for k, h in enumerate(["d0", "d1", "d2", "d3", "d4", "d5", "d6"]):
        label(d, 260 + k * 140 + 60, 280, h, MUTED, 24, True, "ma")
    for k, h in enumerate(["median", "MAD", "limit"]):
        label(d, 1270 + k * 200 + 90, 280, h, AMBER, 24, True, "ma")
    for i, r in enumerate(rows):
        y = 320 + i * 80
        host, days, med, mad, lim = r[0], r[1:8], r[8], r[9], r[10]
        label(d, 60, y + 32, host, TEXT, 30, True, "lm")
        for k, v in enumerate(days):
            over = "%s on day %d" % (host, k) in leads
            colour = ROSE if over else LINE
            cell(d, 260 + k * 140, y, 120, 64, v, colour, 26, ROSE if over else TEXT)
        for k, v in enumerate((med, mad, lim)):
            cell(d, 1270 + k * 200, y, 180, 64, v, AMBER, 26)
    notes = body("FOUR", "HOW EACH HOST")[3:]
    for i, ln in enumerate(notes):
        label(d, 60, 500 + i * 40, ln.strip(), MUTED if i == 0 else GREEN, 26)
    header(d, 60, 630, "THE SCOREBOARD", GREEN)
    score = [cols(ln) for ln in body("FOUR", "THE SCOREBOARD")]
    widths = [520, 200, 200, 200]
    for i, r in enumerate(score):
        y = 690 + i * 44
        x = 60
        for k, v in enumerate(r):
            colour = MUTED if i == 0 else (TEXT if k == 0 else ROSE if k == 2 else GREEN if k == 3 else AMBER)
            if r[0] == "total":
                colour = AMBER
            label(d, x + (0 if k == 0 else widths[k] // 2), y, v, colour, 26, i == 0 or r[0] == "total", "la" if k == 0 else "ma")
            x += widths[k]
    found = section("FOUR", "THE ATTACKS FOUND")[0].split(": ", 1)[1]
    label(d, 1260, 720, "Found:", GREEN, 28, True)
    label(d, 1260, 760, found, TEXT, 26)
    label(d, 1260, 830, "Not found: the scheduled task,", ROSE, 28, True)
    label(d, 1260, 870, "because no log records it", TEXT, 26)


def rule_backtest(img, d, cfg):
    """The beacon hunt as a rule, run on a clean week before and after the allowlist, and on the attack week."""
    rule = body("FIVE", "THE RULE")[0]
    header(d, 60, 215, "THE RULE: HUNT 1, EVERY WEEK", CYAN)
    panel(d, 60, 275, 1800, 64, CYAN)
    label(d, 960, 307, rule, TEXT, 30, True, "mm")
    before_title, before = section("FIVE", "BEFORE THE ALLOWLIST")
    after_title, after = section("FIVE", "AFTER THE ALLOWLIST")
    header(d, 60, 380, "BEFORE THE ALLOWLIST", ROSE)
    label(d, 60, 450, before_title.split(", ", 1)[1].rstrip(":"), MUTED, 26)
    panel(d, 60, 500, 860, 300, ROSE)
    label(d, 490, 560, before[0], ROSE, 44, True, "ma")
    for subject, verdict in lead_rows("FIVE", "BEFORE THE ALLOWLIST"):
        mono(d, 490, 650, subject, TEXT, 26, 820, "ma")
        label(d, 490, 710, verdict, AMBER, 30, True, "ma")
    header(d, 1000, 380, "AFTER THE ALLOWLIST", GREEN)
    label(d, 1000, 450, "allowlisted: " + after_title.split("(")[1].rstrip("):"), MUTED, 26)
    counts = [cols(ln) for ln in after if ln.startswith("the ")]
    for i, (week, n) in enumerate(counts):
        y = 500 + i * 160
        panel(d, 1000, y, 860, 140, GREEN if i == 0 else ROSE)
        label(d, 1030, y + 40, week, TEXT, 30, True, "lm")
        label(d, 1830, y + 40, n, GREEN if i == 0 else ROSE, 36, True, "rm")
    for subject, verdict in lead_rows("FIVE", "AFTER THE ALLOWLIST"):
        mono(d, 1030, 760, subject, TEXT, 24, 520, "lm")
        label(d, 1830, 760, verdict, ROSE, 26, True, "rm")
    caption(d, "The false lead of the hunt became the rule's allowlist, with its reason.", AMBER, 900)
