"""Pictures for the infrastructure-as-code scanning video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from datetime import date
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

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
                cur = [ln]
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found


def section(name, head, n=0):
    """The body lines of the n-th section (its header is dropped)."""
    return sections(name, head)[n][1:]


def heading(name, head, n=0):
    return sections(name, head)[n][0]


def facts(lines):
    out = {}
    for ln in lines:
        parts = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        if len(parts) == 2:
            out[parts[0]] = parts[1]
    return out


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


def rules():
    """{id: title} from THE RULES."""
    return dict(ln.strip().split(None, 1) for ln in section("ONE", "THE RULES"))


def matrix(lines):
    """The matrix lines as (rule ids, {container: (marks, count)})."""
    ids = lines[0].split()
    rows = {}
    for ln in lines[1:]:
        parts = ln.split()
        if len(parts) == len(ids) + 2:
            rows[parts[0]] = (parts[1:-1], int(parts[-1]))
    return ids, rows


def container_settings(name):
    """The settings items of one container from THE MANIFESTS (continuation lines joined)."""
    text, cur = {}, None
    for ln in section("ONE", "THE MANIFESTS"):
        if not ln.startswith(" "):
            cur = ln.split()[0]
            text[cur] = ln[len(cur):].strip()
        else:
            text[cur] += " " + ln.strip()
    return [i.strip() for i in text[name].split(",")]


# ------------------------------------------------------------------ figures

def one_container(img, d, cfg):
    """The core idea: one container's settings, and the eight rules asked of it."""
    items = container_settings("web")
    ids, rows = matrix(section("ONE", "THE FINDINGS"))
    marks, count = rows["web"]
    titles = rules()
    header(d, 60, 180, "THE CONTAINER web (what the file sets)", CYAN)
    for i, item in enumerate(items):
        k, v = item.split("=", 1)
        y = 260 + i * 80
        label(d, 80, y + 28, k, MUTED, 30, True, "lm")
        cell(d, 330, y, 380, 56, v, AMBER, 28)
    label(d, 80, 460, "everything else is not set,", MUTED, 28)
    label(d, 80, 505, "and a missing setting is a default:", MUTED, 28)
    label(d, 80, 550, "root, no limits, writable, escalation", MUTED, 28)
    header(d, 800, 180, "THE RULES (x: broken)", ROSE)
    for i, (rid, mark) in enumerate(zip(ids, marks)):
        y = 250 + i * 76
        bad = mark == "x"
        cell(d, 800, y, 90, 60, rid, ROSE if bad else GREEN, 28)
        label(d, 915, y + 30, titles[rid], TEXT if bad else MUTED, 28, bad, "lm")
        label(d, 1840, y + 30, "x" if bad else "-", ROSE if bad else GREEN, 34, True, "rm")
    caption(d, "Two settings and six defaults: web breaks %d of the %d rules." % (count, len(ids)), AMBER, 900)


def rule_matrix(img, d, cfg):
    """The findings of the first and the fixed manifests, container by rule."""
    ids, first = matrix(section("ONE", "THE FINDINGS"))
    _, fixed = matrix(section("ONE", "AFTER THE FIXES"))
    total = section("ONE", "HOW THE FINDINGS ADD UP")[0].strip()
    for k, (title, rows, colour, x0) in enumerate((("BEFORE THE FIXES", first, ROSE, 60), ("AFTER THE FIXES", fixed, GREEN, 1000))):
        header(d, x0, 180, title, colour)
        for j, rid in enumerate(ids):
            label(d, x0 + 200 + j * 76 + 32, 250, rid, MUTED, 26, True, "ma")
        for i, (name, (marks, count)) in enumerate(rows.items()):
            y = 300 + i * 90
            label(d, x0, y + 32, name, TEXT, 30, True, "lm")
            for j, m in enumerate(marks):
                x = x0 + 200 + j * 76
                bad = m == "x"
                d.rounded_rectangle([x, y, x + 64, y + 64], radius=8, fill=ROSE if bad else BG_CELL, outline=ROSE if bad else LINE, width=2)
            label(d, x0 + 200 + len(ids) * 76 + 16, y + 32, str(count), ROSE if count else GREEN, 32, True, "lm")
    fit_mono(d, 60, 620, "before: " + total, ROSE, 30, 900)
    after = section("ONE", "AFTER THE FIXES")[-1].strip()
    fit_mono(d, 1000, 620, "after: " + after, AMBER, 30, 860)
    label(d, 60, 720, "Each red square is one finding: one rule, one container.", MUTED, 30)
    label(d, 60, 770, "The agent keeps one: it reads node counters from the host network.", MUTED, 30)
    caption(d, "Thirteen findings before the fixes, one after.", AMBER, 900)


def rule_test(img, d, cfg):
    """The K3 rule with a mistake, against its own bad sample, and how the real rule reads the image."""
    result = section("TWO", "THE RESULT")[0].strip()
    wrong = section("TWO", "THE RESULT", 1)[0].strip()
    mistake = heading("TWO", "A RULE WITH A MISTAKE")
    sample = section("TWO", "A RULE WITH A MISTAKE")[0].split("  ", 1)[1].strip()
    image = sample.split(": ", 1)[1]
    steps = facts(section("TWO", "HOW THE REAL K3 READS"))
    header(d, 60, 180, "THE BAD SAMPLE (it must fire)", CYAN)
    mono(d, 60, 260, sample, TEXT, 40)
    header(d, 60, 350, "THE RULE WITH A MISTAKE", ROSE)
    fit_mono(d, 60, 420, re.search(r"\((.*)\)", mistake).group(1), MUTED, 28, 840)
    cell(d, 60, 480, 840, 64, "%s ends with :latest?  no" % image, ROSE, 28)
    fit_mono(d, 60, 575, wrong, ROSE, 26, 860)
    header(d, 1000, 350, "THE REAL K3, STEP BY STEP", GREEN)
    y = 420
    for k, v in steps.items():
        label(d, 1000, y + 30, k, MUTED, 28, True, "lm")
        cell(d, 1220, y, 640, 60, v, GREEN if k != "tag" else AMBER, 26)
        y += 80
    header(d, 60, 700, "ALL EIGHT RULES, EACH WITH A BAD AND A GOOD SAMPLE", AMBER)
    mono(d, 60, 770, result, GREEN, 32)
    caption(d, "A rule is a program: its own samples catch its mistakes.", AMBER, 900)


def scanner_grid(img, d, cfg):
    """Our eight rules against the check each tool fired on the first files."""
    lines = section("THREE", "OUR RULE")
    titles = rules()
    comp = facts(section("THREE", "HOW THE COMPARISON"))
    header(d, 60, 180, "OUR RULE  ->  THE CHECK EACH TOOL FIRED", CYAN)
    for j, tool in enumerate(("checkov", "trivy", "kics")):
        label(d, (620, 840, 1250)[j], 245, tool, MUTED, 26, True, "ma")
    for i, ln in enumerate(lines[1:]):
        rid, rest = ln.strip().split(None, 1)
        ck, tv, kx = re.match(r"(\S+)\s+(\S+)\s+(.*)", rest).groups()
        y = 285 + i * 66
        label(d, 60, y + 27, rid, TEXT, 28, True, "lm")
        fit_mono(d, 110, y + 27, titles[rid], MUTED, 22, 400, "lm")
        for x, w, v in ((520, 200, ck), (740, 200, tv), (960, 580, kx)):
            cell(d, x, y, w, 54, v, GREEN if v != "-" else ROSE, 22)
    label(d, 1730, 300, "found by", MUTED, 28, True, "ma")
    label(d, 1730, 340, "all three", MUTED, 28, True, "ma")
    label(d, 1730, 420, comp["found by all three"].split()[0], GREEN, 90, True, "ma")
    label(d, 1730, 530, "of " + comp["found by all three"].split()[-1], MUTED, 30, True, "ma")
    caption(d, "All three miss a password written in the file.", AMBER, 900)


def waiver_timeline(img, d, cfg):
    """The waiver's end date on a timeline, and what each waiver leaves."""
    w = facts(section("FOUR", "THE WAIVER"))
    rows = [re.split(r"\s{2,}", ln.strip()) for ln in section("FOUR", "THE RESULT")]
    end = date.fromisoformat(w["ends"])
    days = sorted({date.fromisoformat(r[0]) for r in rows})
    header(d, 60, 180, "THE WAIVER: %s, owner %s" % (w["for"], w["owner"]), CYAN)
    lo, hi = days[0].toordinal() - 10, days[-1].toordinal() + 10
    x0, x1, y = 120, 1800, 340

    def px(day):
        return x0 + (day.toordinal() - lo) * (x1 - x0) / (hi - lo)
    d.line([x0, y, x1, y], fill=LINE, width=4)
    d.rectangle([px(days[0]) - 40, y - 18, px(end), y + 18], fill=GREEN)
    label(d, px(end), y + 40, "ends " + w["ends"], GREEN, 28, True, "ma")
    for day in days:
        d.line([px(day), y - 50, px(day), y + 30], fill=TEXT, width=4)
        label(d, px(day), y - 90, str(day), TEXT, 28, True, "ma")
    header(d, 60, 470, "THE RESULT (findings left)", AMBER)
    for i, r in enumerate(rows):
        yy = 540 + i * 74
        n = int(r[2].split(",")[0])
        mono(d, 60, yy + 26, r[0], TEXT, 28, "lm")
        label(d, 320, yy + 26, r[1], MUTED, 28, True, "lm")
        cell(d, 700, yy, 560, 54, r[2], ROSE if n else GREEN, 26)
    caption(d, "A waiver ends, and needs an owner and the right resource, or it hides nothing.", AMBER, 900)


def baseline(img, d, cfg):
    """Accepted findings, the scan now, and the new ones that fail the build."""
    accepted = section("FIVE", "THE BASELINE")
    now = section("FIVE", "THE SCAN NOW")[0].strip()
    decide = section("FIVE", "HOW THE BUILD DECIDES")
    sum_line = decide[0].strip()
    new = [ln.strip()[3:].strip() for ln in decide if ln.strip().startswith("new ")]
    header(d, 60, 180, "ACCEPTED (THE BASELINE)", GREEN)
    for i, ln in enumerate(accepted):
        cell(d, 60, 250 + i * 70, 760, 56, re.sub(r"\s+", " ", ln.strip()), GREEN, 26)
    header(d, 60, 380, "THE CHANGE", CYAN)
    fit_mono(d, 60, 450, heading("FIVE", "THE CHANGE").split(": ", 1)[1], TEXT, 28, 800)
    header(d, 60, 530, "THE SCAN NOW", AMBER)
    label(d, 60, 600, now, AMBER, 44, True)
    header(d, 1000, 180, "NEW, SO THE BUILD FAILS", ROSE)
    for i, f in enumerate(new):
        cell(d, 1000, 250 + i * 76, 860, 60, re.sub(r"\s+", " ", f), ROSE, 26)
    fit_mono(d, 1000, 670, sum_line, ROSE, 30, 860)
    caption(d, "Old findings do not block anyone; a new one does.", AMBER, 900)
