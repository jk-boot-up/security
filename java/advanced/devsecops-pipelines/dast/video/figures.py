"""Pictures for the dynamic analysis video, drawn by the `figure` slide kind.

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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def section(name, head):
    """The section of an act whose header starts with `head`, as stripped lines."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln.strip())
    return found[0]


def cols(line):
    return re.split(r"\s{2,}", line.strip())


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


def caption(d, text, colour=AMBER, y=935):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=2)


def found(name):
    """{kind: (found, planted)} from an act's WHAT IT FOUND section."""
    text = " ".join(section(name, "WHAT IT FOUND"))
    return {k: (int(a), int(b)) for k, a, b in re.findall(r"(script|query|cookie and header) flaws (\d+) of (\d+)", text)}


def reached(name):
    line = next((ln for ln in act(name) if "the admin area reached" in ln), None)
    return line.strip().rsplit(": ", 1)[1] if line else "no"


def score_colour(a, b):
    return GREEN if a == b else ROSE if a == 0 else AMBER


def planted():
    """(kind, name, path) for each planted flaw."""
    return [tuple(cols(ln)) for ln in section("TWO", "THE APPLICATION UNDER TEST")[1:]]


# ------------------------------------------------------------------ figures

def three_scans(img, d, cfg):
    """The core idea: the same site, three scans, and what each one could reach."""
    scans = [("PASSIVE", "reads the responses", "TWO"), ("ACTIVE, NO LOGIN", "sends attacks", "THREE"), ("ACTIVE, LOGGED IN", "attacks, with the session", "FOUR")]
    header(d, 60, 175, "THE INPUT: ONE BANK SITE, %s PLANTED FLAWS, ONE BEHIND THE LOGIN" % re.search(r"planted flaws (\d+)", section("TWO", "THE APPLICATION")[0]).group(1), CYAN)
    for i, (title, what, name) in enumerate(scans):
        x = 60 + i * 610
        res = found(name)
        admin = reached(name) if name != "TWO" else "no"
        total = sum(a for a, _ in res.values())
        colour = GREEN if total == sum(b for _, b in res.values()) else AMBER
        panel(d, x, 250, 580, 600, colour)
        label(d, x + 290, 275, title, TEXT, 30, True, "ma")
        label(d, x + 290, 320, what, MUTED, 24, False, "ma")
        for j, (kind, (a, b)) in enumerate(res.items()):
            y = 390 + j * 95
            label(d, x + 40, y + 10, kind, TEXT, 26, True)
            cell(d, x + 360, y, 180, 56, "%d of %d" % (a, b), score_colour(a, b), 28)
        label(d, x + 40, 690, "admin area reached", TEXT, 26, True)
        cell(d, x + 360, 680, 180, 50, admin, GREEN if admin == "yes" else ROSE, 26)
        label(d, x + 290, 770, "%d of %d planted flaws" % (total, sum(b for _, b in res.values())), colour, 32, True, "ma")
        if i < 2:
            draw.arrow_right(d, x + 584, 550, x + 606, MUTED, 3)
    caption(d, "A scanner finds only what it sends, and only where it can reach.", AMBER, 900)


def attack_by_hand(img, d, cfg):
    """One attack string, the flawed and the fixed answer, and ZAP's report."""
    req = section("ONE", "THE REQUEST")[0]
    flawed = section("ONE", "THE RESPONSE OF THE FLAWED")
    fixed = section("ONE", "THE RESPONSE OF THE FIXED")
    zap = section("ONE", "WHAT ZAP REPORTS")
    header(d, 60, 175, "THE REQUEST (" + heading("ONE", "THE REQUEST").split("(")[1].rstrip("):") + ")", CYAN)
    cell(d, 60, 235, 1800, 70, req, CYAN, 34)
    for x, title, lines, colour in ((60, "THE FLAWED APPLICATION", flawed, ROSE), (980, "THE FIXED APPLICATION", fixed, GREEN)):
        header(d, x, 340, title, colour)
        panel(d, x, 400, 880, 330, colour)
        label(d, x + 30, 420, lines[0], MUTED, 24, True)
        for k, ln in enumerate(lines[1:-1]):
            fit_mono(d, x + 30, 470 + k * 50, ln, TEXT, 26, 820)
        verdict = lines[-1].rsplit(": ", 1)[1]
        label(d, x + 30, 600, "the attack came back unchanged", MUTED, 26, True)
        cell(d, x + 30, 645, 200, 60, verdict, colour, 32)
        label(d, x + 260, 675, "raised" if verdict == "yes" else "not raised", colour, 30, True, "lm")
    header(d, 60, 760, "WHAT ZAP REPORTS", AMBER)
    label(d, 60, 825, zap[0] + ": " + zap[1] + "; " + zap[2], TEXT, 26, True)
    caption(d, "Markup that comes back as markup runs in the customer's browser.", AMBER, 905)


def planted_map(img, d, cfg):
    """The site's pages, the flaws planted on each, and what a passive scan found."""
    rows = planted()
    res = found("TWO")
    header(d, 60, 175, "THE INPUT: THE SITE, AND THE FLAWS PLANTED ON EACH PAGE", CYAN)
    pages = []
    for kind, name, path in rows:
        if path not in pages:
            pages.append(path)
    kind_of = {"script": "script", "query": "query", "cookie": "cookie and header", "header": "cookie and header"}
    y = 245
    for path in pages:
        mine = [r for r in rows if r[2] == path]
        h = 30 + 44 * len(mine)
        locked = path.startswith("/admin")
        panel(d, 60, y, 1180, h, AMBER if locked else LINE)
        label(d, 90, y + 14, path + ("  (behind the login)" if locked else ""), AMBER if locked else TEXT, 26, True)
        for k, (kind, name, _) in enumerate(mine):
            a, b = res[kind_of[kind]]
            colour = GREEN if a == b else ROSE
            label(d, 620, y + 14 + k * 44, kind + ": " + name, colour, 24, True)
        y += h + 14
    header(d, 1300, 175, "THE RESULT: A PASSIVE SCAN", AMBER)
    label(d, 1300, 245, section("TWO", "THE PASSIVE SCAN")[0], MUTED, 26, True)
    for j, (kind, (a, b)) in enumerate(res.items()):
        yy = 310 + j * 110
        label(d, 1300, yy + 14, kind, TEXT, 26, True)
        cell(d, 1300, yy + 50, 260, 52, "%d of %d" % (a, b), score_colour(a, b), 28)
    label(d, 1300, 650, "green: found by reading", GREEN, 24, True)
    label(d, 1300, 690, "rose: needs an attack", ROSE, 24, True)
    caption(d, "Reading answers finds headers and cookies, never injection.", AMBER, 935)


def query_check(img, d, cfg):
    """Three requests that show the database working out 3-2, and what the anonymous scan found."""
    rows = [ln for ln in section("THREE", "HOW THE QUERY") if ln.startswith("GET")]
    rest = [ln for ln in section("THREE", "HOW THE QUERY") if not ln.startswith("GET")]
    res = found("THREE")
    missed = section("THREE", "WHAT IT MISSED")[0]
    header(d, 60, 175, "THE REQUESTS", CYAN)
    header(d, 760, 175, "THE RESPONSES", GREEN)
    for i, ln in enumerate(rows):
        req, resp = re.match(r"(GET \S+)\s+(.*)", ln).groups()
        y = 250 + i * 120
        colour = AMBER if "3-2" in req else TEXT
        cell(d, 60, y, 600, 84, req, colour, 32)
        draw.arrow_right(d, 670, y + 42, 750, MUTED, 3)
        cell(d, 760, y, 420, 84, resp, colour, 32)
    label(d, 60, 640, rest[0].split(": ")[0] + ": " + rest[0].rsplit(": ", 1)[1], AMBER, 30, True)
    label(d, 60, 700, rest[1], GREEN, 28, True)
    header(d, 1260, 175, "THE RESULT: NO LOGIN", AMBER)
    for j, (kind, (a, b)) in enumerate(res.items()):
        y = 250 + j * 120
        label(d, 1260, y + 24, kind, TEXT, 28, True)
        cell(d, 1600, y, 220, 70, "%d of %d" % (a, b), score_colour(a, b), 30)
    label(d, 1260, 620, "missed:", ROSE, 28, True)
    label(d, 1260, 665, missed, ROSE, 26, True)
    label(d, 1260, 720, "the admin area reached: " + reached("THREE"), ROSE, 26, True)
    caption(d, "3-2 came back as the page for 1: the input ran as part of the query.", AMBER, 905)


def logged_in(img, d, cfg):
    """The session cookie opens the admin area; the total, and the report by risk."""
    res = found("FOUR")
    total = section("FOUR", "HOW THE TOTAL")[0]
    plan = section("FOUR", "THE PLAN ADDS")[0]
    report = [ln for ln in section("FOUR", "THE REPORT")[1:]]
    risks = {}
    for ln in report:
        risks.setdefault(ln.split()[0], []).append(ln)
    header(d, 60, 175, "THE PLAN ADDS", CYAN)
    fit_mono(d, 60, 245, plan, TEXT, 26, 860)
    for j, (kind, (a, b)) in enumerate(res.items()):
        y = 310 + j * 80
        label(d, 60, y + 14, kind, TEXT, 26, True)
        cell(d, 420, y, 200, 52, "%d of %d" % (a, b), score_colour(a, b), 28)
    label(d, 60, 560, "the admin area reached: " + reached("FOUR"), GREEN, 28, True)
    header(d, 60, 630, "HOW THE TOTAL IS MADE", AMBER)
    label(d, 60, 700, total, GREEN, 34, True)
    header(d, 1000, 175, heading("FOUR", "THE REPORT").rstrip(":").replace("THE REPORT", "THE REPORT BY RISK"), AMBER)
    colours = {"high": ROSE, "medium": AMBER, "low": CYAN, "info": MUTED}
    for i, (risk, lines) in enumerate(risks.items()):
        y = 250 + i * 150
        label(d, 1000, y + 14, risk, colours[risk], 28, True)
        d.rounded_rectangle([1140, y, 1140 + 90 * len(lines), y + 50], radius=8, fill=colours[risk])
        label(d, 1150 + 90 * len(lines), y + 10, str(len(lines)), colours[risk], 30, True)
        names = [re.sub(r"\s+\S+$", "", ln.split(None, 1)[1]).strip() for ln in lines]
        for k, name in enumerate(names):
            label(d, 1140, y + 58 + k * 26, name, MUTED, 20, False)
    caption(d, "With the session, the scan reaches the admin page: 9 of 9.", AMBER, 905)


def before_after(img, d, cfg):
    """Alerts by risk before and after the fixes, and the one alert left for a person."""
    def counts(head):
        line = section("FIVE", head)[1]
        return [(k, int(v)) for k, v in re.findall(r"(\w+) (\d+)", line)]
    before, after = counts("BEFORE THE FIXES"), counts("AFTER THE FIXES")
    pb, pa = section("FIVE", "BEFORE THE FIXES")[0], section("FIVE", "AFTER THE FIXES")[0]
    left = section("FIVE", "LEFT FOR A PERSON")
    fixes = " ".join(section("FIVE", "THE FIXES"))
    header(d, 60, 175, "THE FIXES", CYAN)
    label(d, 60, 240, fixes, TEXT, 24, True)
    colours = {"high": ROSE, "medium": AMBER, "low": CYAN, "informational": MUTED}
    base = 690
    for g, (title, data, colour, x0) in enumerate((("BEFORE THE FIXES", before, ROSE, 60), ("AFTER THE FIXES", after, GREEN, 700))):
        header(d, x0, 300, title, colour)
        label(d, x0, 360, (pb if g == 0 else pa), colour, 26, True)
        for i, (risk, n) in enumerate(data):
            x = x0 + i * 140
            h = n * 70
            d.rectangle([x, base - h, x + 100, base], fill=colours[risk])
            label(d, x + 50, base - h - 36, str(n), colours[risk], 28, True, "ma")
            label(d, x + 50, base + 10, risk if risk != "informational" else "info", MUTED, 22, True, "ma")
        d.line([x0 - 10, base, x0 + 560, base], fill=LINE, width=2)
    header(d, 1340, 300, "LEFT FOR A PERSON", AMBER)
    for i, ln in enumerate(left):
        fit_mono(d, 1340, 370 + i * 50, ln, AMBER if i % 2 == 0 else MUTED, 24, 540)
    label(d, 1340, 500, "a login form with no token:", TEXT, 24, True)
    label(d, 1340, 540, "a person decides if it matters", TEXT, 24, True)
    caption(d, "A clean scan says these probes found nothing on these pages.", AMBER, 905)
