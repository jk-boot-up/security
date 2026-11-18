"""Pictures for the controls and evidence video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
import textwrap
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
STATUS = {"PASSING": GREEN, "PARTIAL": AMBER, "FAILING": ROSE, "STALE": ROSE, "ATTESTED": CYAN, "NONE": MUTED}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def section(name, head):
    """The first section of an act whose header starts with `head`, as lines (two more spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[0]


def facts(lines):
    """`key   value` lines as a dict."""
    return dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in lines if re.search(r"\s{2,}", ln.strip()))


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


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def card(d, x, y, w, h, title, lines, colour, size=24):
    panel(d, x, y, w, h, colour, 3)
    label(d, x + w // 2, y + 18, title, colour, 26, True, "ma")
    for i, ln in enumerate(lines):
        fit_mono(d, x + w // 2, y + 70 + i * (size + 16), ln, TEXT, size, w - 24, "ma")


def catalogue_line():
    return facts(section("ONE", "THE INPUT"))


def report():
    return facts(section("THREE", "WHAT THE READER"))


def decision():
    """The status steps of act THREE: (question, answer, status or None)."""
    rows = []
    for ln in section("THREE", "HOW THE STATUS"):
        m = re.match(r"\s*(\S.*?\?)\s{2,}(.*?)(?:\s+->\s+(\w+))?$", ln)
        if m:
            rows.append(m.groups())
    return rows


# ------------------------------------------------------------------ figures

def control_trail(img, d, cfg):
    """The core idea on one control: a promise, its two framework references, its evidence and its status today."""
    c = catalogue_line()
    r = report()
    steps = decision()
    status = steps[-1][2]
    iso = c["iso"].split(";")
    age = re.search(r"= (\d+) days", " ".join(section("THREE", "HOW THE STATUS"))).group(1)
    today = re.search(r"TODAY (\S+):", heading_of("THREE", "HOW THE STATUS")).group(1)
    card(d, 40, 330, 400, 340, "THE CONTROL", [c["id"]] + textwrap.wrap(c["name"], 20) + ["owner " + c["owner"]], CYAN, 28)
    names = [re.split(r"\s{2,}", ln.strip())[2] for ln in section("ONE", "HOW THE NIST")[:2]]
    draw.arrow_right(d, 450, 500, 520, MUTED, 4)
    card(d, 530, 200, 400, 250, "NIST CSF 2.0", [c["csf"]] + names, AMBER, 28)
    card(d, 530, 550, 400, 250, "ISO 27001 ANNEX A", iso, AMBER, 28)
    draw.arrow_right(d, 940, 500, 1010, MUTED, 4)
    card(d, 1020, 330, 440, 340, "THE EVIDENCE", [c["evidence"], "ran " + r["ran"].split()[0], "tests " + r["tests"].split(",")[0], "failed", r["failed"]], GREEN, 26)
    draw.arrow_right(d, 1470, 500, 1540, MUTED, 4)
    panel(d, 1550, 330, 330, 340, STATUS[status], 4)
    label(d, 1715, 350, "TODAY", MUTED, 26, True, "ma")
    mono(d, 1715, 400, today, MUTED, 26, "ma")
    mono(d, 1715, 480, status, STATUS[status], 48, "ma")
    label(d, 1715, 570, "evidence %s days old" % age, TEXT, 26, False, "ma")
    label(d, 960, 850, "One line of the catalogue answers two frameworks; one test report proves it, or not.", TEXT, 30, False, "ma")
    caption(d, "A control is a promise with an owner; evidence says whether it holds today.", AMBER, 940)


def id_parts(img, d, cfg):
    """The two kinds of identifier, taken apart: a CSF 2.0 subcategory and the Annex A controls."""
    csf_id = re.search(r"READS, (\S+):", heading_of("ONE", "HOW THE NIST")).group(1)
    parts = [re.split(r"\s{2,}", ln.strip()) for ln in section("ONE", "HOW THE NIST")]
    header(d, 60, 170, "NIST CSF 2.0: " + csf_id, CYAN)
    pieces = [(p[1], p[0], p[2]) for p in parts]
    seps = [".", "-"]
    x = 120
    f = font(MONO_B, 120)
    for i, (code, what, name) in enumerate(pieces):
        colour = (CYAN, AMBER, GREEN)[i]
        d.text((x, 240), code, font=f, fill=colour)
        w = f.getlength(code)
        label(d, x + w / 2, 400, what, MUTED, 24, True, "ma")
        label(d, x + w / 2, 440, name, colour, 26, True, "ma")
        x += w
        if i < len(seps):
            d.text((x, 240), seps[i], font=f, fill=LINE)
            x += f.getlength(seps[i]) + 120
    iso = section("ONE", "HOW THE ISO")
    header(d, 60, 520, "ISO/IEC 27001:2022 ANNEX A", AMBER)
    for k in range(0, len(iso), 2):
        a, rest = iso[k].strip().split(None, 1)
        title = iso[k + 1].strip()
        y = 600 + k * 75
        theme, control = re.search(r"theme (\d+ \(\w+\)), control (\d+)", rest).groups()
        letters = a.split(".")
        fit_mono(d, 120, y, letters[0] + "." + letters[1] + "." + letters[2], TEXT, 64, 400)
        label(d, 520, y + 6, "Annex A, theme " + theme + ", control " + control, MUTED, 28, False)
        label(d, 520, y + 46, title, GREEN, 30, True)
    caption(d, "Both ids name a promise; neither says how to prove it: that is the evidence.", AMBER, 940)


def coverage_map(img, d, cfg):
    """The 12 controls in the 6 CSF functions, and the 14 chosen Annex A controls, with the one gap."""
    rows = [re.split(r"\s+", ln.strip()) for ln in section("TWO", "NIST CSF")]
    header(d, 60, 170, "NIST CSF 2.0: CONTROLS IN EACH FUNCTION", CYAN)
    for i, r in enumerate(rows):
        code, name, ids = r[0], r[1], r[3:]
        x = 60 + i * 150
        cell(d, x, 240, 140, 70, code, CYAN, 30)
        label(d, x + 70, 320, name, MUTED, 22, True, "ma")
        for j, cid in enumerate(ids):
            cell(d, x + 10, 365 + j * 60, 120, 50, cid, GREEN, 24)
    iso = section("TWO", "ISO 27001")
    served = (iso[1] + " " + iso[2]).split()
    gap = re.search(r"not served\s+(\S+)\s+(.*)", "\n".join(iso)).groups()
    both = [re.search(r"(C\d+) serves (\S+) and (\S+)", ln).groups() for ln in iso if "two at once" in ln]
    header(d, 990, 170, heading_of("TWO", "ISO 27001").rstrip(":"), AMBER)
    ids = sorted(served + [gap[0]], key=lambda a: [int(x) for x in a.split(".")[1:]])
    multi = {a for _, x, y in both for a in (x, y)}
    for k, a in enumerate(ids):
        x, y = 990 + (k % 4) * 225, 240 + (k // 4) * 80
        c = ROSE if a == gap[0] else AMBER if a in multi else GREEN
        cell(d, x, y, 210, 64, a, c, 28)
    label(d, 990, 580, "rose: not served, %s %s" % gap, ROSE, 26, True)
    for i, (cid, a, b) in enumerate(both):
        label(d, 990, 625 + i * 40, "amber: %s serves %s and %s" % (cid, a, b), AMBER, 26, False)
    res = facts(section("TWO", "THE RESULT"))
    fit_mono(d, 60, 780, "CSF functions with a control: %s" % res["CSF functions with a control"], GREEN, 32, 900)
    fit_mono(d, 990, 780, "Annex A served: %s" % res["Annex A controls served"], ROSE, 30, 880)
    caption(d, "Every function has a control, but one chosen Annex A control has none.", AMBER, 920)


def xml_to_status(img, d, cfg):
    """The test report as read, and the rules asked in order until one ends it."""
    xml = section("THREE", "THE INPUT")
    header(d, 60, 170, heading_of("THREE", "THE INPUT").replace("THE INPUT, ", "THE INPUT: ").rstrip(":"), CYAN)
    panel(d, 60, 235, 1000, 520)
    keys = ("name=", "tests=", "failures=", "timestamp=", "<failure")
    for i, ln in enumerate(xml):
        colour = ROSE if "failure" in ln else AMBER if any(k in ln for k in keys) else MUTED
        fit_mono(d, 85, 260 + i * 58, ln, colour, 24, 960)
    r = report()
    header(d, 1110, 170, "WHAT THE READER TAKES", GREEN)
    for i, (k, v) in enumerate(r.items()):
        mono(d, 1110, 240 + i * 46, k, MUTED, 24)
        fit_mono(d, 1230, 240 + i * 46, v, TEXT, 24, 640)
    header(d, 1110, 440, "THE RULES, IN ORDER", AMBER)
    for i, (q, a, end) in enumerate(decision()):
        y = 510 + i * 90
        c = STATUS.get(end, GREEN) if end else GREEN
        cell(d, 1110, y, 300, 64, q, c, 24)
        fit_mono(d, 1430, y + 20, a, TEXT, 22, 450)
        if end:
            mono(d, 1430, y + 50, "-> " + end, c, 26)
    caption(d, "The rules stop at the first that decides: here, a failed test.", AMBER, 920)


def status_board(img, d, cfg):
    """Each control's evidence age against the two limits, coloured by its status today."""
    rows = []
    for ln in section("FOUR", "EACH CONTROL"):
        m = re.match(r"\s*(C\d+)\s+(\w+)\s+(\S+)\s+(.+?)\s{2,}(\w+)$", ln)
        rows.append(m.groups())
    rules = section("FOUR", "THE RULES")
    auto_days, manual_days = [int(x) for x in re.search(r"older than (\d+) days \((\d+)", " ".join(rules)).groups()]
    oldest = max(int(r[2][:-1]) for r in rows if r[2] != "-")
    x0, x1 = 560, 1700
    top = max(oldest, manual_days) * 1.05

    def px(days):
        return x0 + (x1 - x0) * days / top
    header(d, 60, 170, "EVIDENCE AGE IN DAYS, AND THE STATUS TODAY", CYAN)
    for i, (cid, fn, age, what, status) in enumerate(rows):
        y = 240 + i * 52
        c = STATUS[status]
        mono(d, 60, y + 20, "%s %s" % (cid, fn), TEXT, 24, "lm")
        fit_mono(d, 220, y + 20, what, MUTED, 22, 320, "lm")
        if age != "-":
            days = int(age[:-1])
            d.rectangle([x0, y + 6, max(px(days), x0 + 4), y + 34], fill=c)
            mono(d, max(px(days), x0 + 4) + 10, y + 20, age, c, 22, "lm")
        mono(d, 1890, y + 20, status, c, 24, "rm")
    for days, text in ((auto_days, "%d days: a test report" % auto_days), (manual_days, "%d days: a signature" % manual_days)):
        d.line([px(days), 225, px(days), 870], fill=AMBER, width=3)
        label(d, px(days) + 8, 868, text, AMBER, 22, True, "la")
    res = section("FOUR", "THE RESULT")
    fit_mono(d, 1890, 182, res[1].strip(), GREEN, 26, 800, "ra")
    caption(d, "A passing test goes stale too: C04 passed, but %s days ago." % next(r[2][:-1] for r in rows if r[0] == "C04"), AMBER, 950)


def one_failure(img, d, cfg):
    """C01 before and after one test fails, and what that does to the Protect function."""
    before = section("FIVE", "BEFORE")
    after = section("FIVE", "AFTER")
    title = heading_of("FIVE", "BEFORE").split(", ", 1)[1].rstrip(":")
    m = re.match(r"\s*(\S+), (\d+) tests, (\d+) failed: (\w+)", before[0])
    suite, tests, failed, status0 = m.groups()
    q = re.match(r"\s*(\S.*?\?)\s{2,}(.*?)\s+->\s+(\w+)$", after[0])
    answer, status1 = q.group(2), q.group(3)
    test = answer.split(": ")[1]
    p0 = re.search(r"(\d+) of (\d+) \((.*)\)", before[1]).groups()
    p1 = re.search(r"(\d+) of (\d+) \((.*)\)", after[1]).groups()
    change = heading_of("FIVE", "THE CHANGE").split(": ", 1)[1]
    header(d, 60, 170, "BEFORE", GREEN)
    card(d, 60, 240, 700, 260, title, [suite, "%s tests, %s failed" % (tests, failed), status0], STATUS[status0])
    header(d, 1160, 170, "AFTER", ROSE)
    card(d, 1160, 240, 700, 260, title, [suite, answer.replace(": " + test, " failed"), test, status1], STATUS[status1])
    draw.arrow_right(d, 780, 370, 1140, MUTED, 5)
    label(d, 960, 400, change.split(" lets ")[0], MUTED, 22, False, "ma")
    label(d, 960, 430, "lets " + change.split(" lets ")[1], MUTED, 22, False, "ma")
    protect = sorted(set(p0[2].split(", ")) | set(re.findall(r"C\d+", heading_of("FOUR", "EACH") + " ".join(
        ln for ln in section("FOUR", "EACH CONTROL") if " PR " in ln))))
    for k, (p, x) in enumerate(((p0, 60), (p1, 1160))):
        label(d, x, 560, "Protect with passing evidence: %s of %s" % (p[0], p[1]), TEXT, 28, True)
        good = p[2].split(", ")
        for j, cid in enumerate(protect):
            cell(d, x + j * 150, 620, 135, 64, cid, GREEN if cid in good else LINE, 28, text_colour=TEXT if cid in good else MUTED)
    label(d, 960, 760, "The next build changed the status: %s" % after[2].strip(), TEXT, 30, False, "ma")
    caption(d, "Evidence from every build turns a broken control into a red line the same day.", AMBER, 930)
