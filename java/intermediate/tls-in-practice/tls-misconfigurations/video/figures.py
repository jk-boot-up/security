"""Pictures for the TLS misconfigurations video, drawn by the `figure` slide kind.

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
SEV = {"CRITICAL": ROSE, "HIGH": ROSE, "MEDIUM": AMBER, "LOW": MUTED}


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


def facts(lines):
    """`key   value` lines as a list of (key, value)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip())]


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


def grade_badge(d, cx, cy, grade, score, colour):
    d.ellipse([cx - 90, cy - 90, cx + 90, cy + 90], fill=BG_CELL, outline=colour, width=8)
    d.text((cx, cy - 8), grade, font=font(SANS_B, 110), fill=colour, anchor="mm")
    label(d, cx, cy + 120, "score " + score, colour, 30, True, "ma")


def report_rows(name, n=0):
    """The (severity, rule, penalty) rows of the n-th report of an act."""
    return [tuple(ln.split()) for ln in sections(name, "THE REPORT")[n] if ln.split()[0] in SEV]


def grade_of(name, n=0):
    g = dict(facts(sections(name, "HOW THE GRADE")[n]))
    return g["grade"].split()[0], g["score"].split()[0], g["points"]


def colour_of(grade):
    return GREEN if grade in "AB" else AMBER if grade in "CD" else ROSE


# ------------------------------------------------------------------ figures

def suite_name(img, d, cfg):
    """Two cipher suite names, split into the parts the checker reads."""
    sec = sections("ONE", "ONE SUITE NAME")[0]
    names = [sec[0].strip(), sec[1].strip()]
    rows = [re.split(r"\s{2,}", ln.strip()) for ln in sec[2:]]
    cols = [(r[0], r[1], r[2]) for r in rows]
    parts = {"key exchange": CYAN, "authentication": TEXT, "cipher": AMBER, "mode": GREEN, "hash": MUTED}
    hosts = re.search(r"\((.*), (.*)\)", heading_of("ONE", "ONE SUITE NAME")).groups()
    for i, (name, host, c) in enumerate(zip(names, hosts, (GREEN, ROSE))):
        y = 190 + i * 360
        header(d, 60, y, ("A GOOD SUITE, on " if i == 0 else "A BROKEN SUITE, on ") + host, c)
        # the name, each part in the colour of its column
        runs, pos = [], 0
        for part, colour in parts.items():
            value = next(v[1 + i] for v in cols if v[0] == part)
            k = name.find(value, pos)
            if k < 0:
                continue
            runs += [(name[pos:k], LINE), (value, colour)]
            pos = k + len(value)
        runs.append((name[pos:], LINE))
        f = font(MONO_B, 44)
        x = 60
        for text, colour in runs:
            d.text((x, y + 70), text, font=f, fill=colour)
            x += f.getlength(text)
        for j, (part, g, b) in enumerate(cols):
            value = (g, b)[i]
            x = 60 + j * 258
            colour = parts.get(part, GREEN if (value == "yes") == (part == "forward secret") else ROSE)
            label(d, x + 120, y + 160, part, MUTED, 24, True, "ma")
            cell(d, x, y + 200, 240, 64, value, colour, 28)
    caption(d, "ECDHE makes a fresh key for every connection; RSA key exchange reuses one key.", AMBER, 920)


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def good_checklist(img, d, cfg):
    """What the scan learned about shop.example, and the report that comes out."""
    rows = facts(sections("ONE", "THE SERVER")[0])
    header(d, 60, 190, heading_of("ONE", "THE SERVER").rstrip(":"), CYAN)
    for i, (k, v) in enumerate(rows):
        y = 260 + i * 66
        panel(d, 60, y, 1220, 56, LINE)
        label(d, 85, y + 28, k, MUTED, 26, True, "lm")
        fit_mono(d, 300, y + 28, v, TEXT, 26, 900, "lm")
        label(d, 1250, y + 28, "✓", GREEN, 34, True, "rm")
    grade, score, points = grade_of("ONE")
    header(d, 1380, 190, "THE REPORT", GREEN)
    grade_badge(d, 1600, 450, grade, score, colour_of(grade))
    label(d, 1600, 640, sections("ONE", "THE REPORT")[0][0].strip(), GREEN, 30, True, "ma")
    mono(d, 1600, 700, "points " + points, MUTED, 26, "ma")
    caption(d, "Every setting follows the guidance, so nothing is taken off.", AMBER, 940)


def legacy_points(img, d, cfg):
    """How the legacy server's 13 findings take its 100 points below zero."""
    rows = report_rows("TWO")
    grade, score, points = grade_of("TWO")
    nums = [int(n) for n in re.findall(r"-?\d+", points)]
    start, total = nums[0], nums[-1]
    groups = re.findall(r"(\d+) x (\d+)", points)
    header(d, 60, 180, "THE REPORT: %d FINDINGS, WORST FIRST" % len(rows), ROSE)
    for i, (sev, rule, pen) in enumerate(rows):
        x, y = 60 + (i % 4) * 455, 240 + (i // 4) * 62
        cell(d, x, y, 435, 52, "%s %s" % (rule, pen), SEV[sev], 24)
    header(d, 60, 520, "HOW THE GRADE IS MADE", AMBER)
    sev_names = [s for s in ("CRITICAL", "HIGH", "MEDIUM", "LOW") if any(r[0] == s for r in rows)]
    x0, x1 = 600, 1840
    k = (x1 - x0) / (start - min(total, 0))
    zero = x0 + int(-min(total, 0) * k)
    bars = [("start", start, GREEN)] + [("%s x %s %s" % (n, p, s), -int(n) * int(p), SEV[s]) for (n, p), s in zip(groups, sev_names)]
    level, y = 0, 590
    for name, delta, colour in bars:
        a, b = (0, start) if name == "start" else (level + delta, level)
        d.rectangle([zero + a * k, y, zero + b * k, y + 44], fill=colour)
        label(d, 60, y + 22, "start   %d" % start if name == "start" else "%s   %+d" % (name, delta), colour, 28, True, "lm")
        level = start if name == "start" else level + delta
        y += 66
    d.line([zero, 575, zero, y], fill=TEXT, width=3)
    label(d, zero, 570, "0", TEXT, 26, True, "md")
    fit_mono(d, 60, 880, "%s  ->  score %s  ->  grade %s" % (points, score, grade), colour_of(grade), 32, 1800)
    caption(d, "Each finding costs points by severity: HIGH 20, MEDIUM 7, LOW 2.", AMBER, 950)


def cert_timeline(img, d, cfg):
    """The two certificates' validity around today, and the name checks."""
    servers = sections("THREE", "THE SERVER")
    hosts = [re.match(r"THE SERVER (\S+)", h).group(1) for h in act("THREE") if h.startswith("THE SERVER")]
    header(d, 60, 180, "THE CERTIFICATES' DATES (the bar is the validity)", CYAN)
    for i, (host, lines) in enumerate(zip(hosts, servers)):
        f = dict(facts(lines))
        a, b, today = [date.fromisoformat(s) for s in re.findall(r"\d{4}-\d{2}-\d{2}", f["valid"])]
        lo, hi = today.toordinal() - 40, today.toordinal() + 30
        x0, x1, y = 560, 1840, 300 + i * 170

        def px(day):
            return x0 + (min(max(day, lo), hi) - lo) * (x1 - x0) / (hi - lo)
        mono(d, 60, y + 25, host, TEXT, 30, "lm")
        d.line([x0, y + 20, x1, y + 20], fill=LINE, width=3)
        left = (today - b).days
        colour = ROSE if left > 0 else AMBER
        d.rectangle([px(a.toordinal()), y, px(b.toordinal()), y + 50], fill=colour)
        mono(d, px(b.toordinal()), y + 62, "ends " + b.isoformat(), colour, 26, "ma")
        tx = px(today.toordinal())
        d.line([tx, y - 20, tx, y + 60], fill=TEXT, width=4)
        label(d, tx + 12, y - 40, "today " + today.isoformat(), TEXT, 26, True, "la")
        label(d, 60, y + 62, ("expired %d days ago" % left) if left > 0 else ("expires in %d days" % -left), colour, 24, True, "la")
    header(d, 60, 640, "THE NAME CHECKS (pay.shop.example)", AMBER)
    checks = facts(sections("THREE", "HOW THE CERTIFICATE CHECKS")[0])
    for i, (k, v) in enumerate(c for c in checks if c[0] != "expiry"):
        y = 715 + i * 70
        q, ans = v.rsplit("? ", 1)
        mono(d, 60, y, k, MUTED, 26)
        fit_mono(d, 280, y, q + "?", TEXT, 26, 1300)
        cell(d, 1640, y - 8, 180, 52, ans, GREEN if ans == "yes" else ROSE, 26)
    caption(d, "A wildcard covers one label; an expired or misnamed certificate is refused.", AMBER, 945)


def policy_before_after(img, d, cfg):
    """The 13 findings before the policy: which the policy fixed, and which are left for a new certificate and HTTP settings."""
    before = report_rows("TWO")
    after = sections("FOUR", "AFTER THE POLICY")[0]
    summary_after = after[0].strip()
    i = next(k for k, ln in enumerate(after) if ln.startswith("left to fix"))
    left = {r.strip() for ln in after[i + 1:] for r in ln.split(",") if r.strip()}
    summary_before = sections("FOUR", "BEFORE THE POLICY")[0][0].strip()
    header(d, 60, 180, "BEFORE THE POLICY", ROSE)
    fit_mono(d, 60, 245, summary_before, ROSE, 26, 1100)
    for k, (sev, rule, pen) in enumerate(before):
        x, y = 60 + (k % 2) * 430, 300 + (k // 2) * 80
        cell(d, x, y, 410, 64, rule, AMBER if rule in left else GREEN, 26)
    draw.arrow_right(d, 930, 560, 1080, MUTED, 5)
    label(d, 1005, 590, "the policy", MUTED, 24, True, "ma")
    header(d, 1120, 180, "AFTER THE POLICY", GREEN)
    fit_mono(d, 1120, 245, summary_after, AMBER, 26, 760)
    for k, rule in enumerate(r for _, r, _ in before if r in left):
        cell(d, 1120, 300 + k * 80, 410, 64, rule, AMBER, 26)
    label(d, 1120, 640, "not protocol settings: they need a new", MUTED, 28, False)
    label(d, 1120, 685, "certificate and new HTTP settings", MUTED, 28, False)
    caption(d, "Green: fixed by the policy. Amber: left to fix by hand.", AMBER, 920)
