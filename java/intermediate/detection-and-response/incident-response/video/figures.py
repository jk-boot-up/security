"""Pictures for the incident response video, drawn by the `figure` slide kind.

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


def section(name, head):
    return sections(name, head)[0]


def heading_of(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def facts(lines):
    """`key   value` lines (not indented further) as a dict."""
    return dict(tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines
                if not ln.startswith(" ") and re.search(r"\s{2,}", ln.strip()))


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


def caption(d, text, colour=AMBER, y=935):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None, anchor="mm"):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    tx = x + w // 2 if anchor == "mm" else x + 16
    fit_mono(d, tx, y + h // 2, text, text_colour or colour, size, w - 24, anchor)


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def ways():
    """The five ways back in and what closes each, from act ONE."""
    return [tuple(re.split(r"\s{2,}", ln.strip())) for ln in section("ONE", "THE WAYS BACK IN")]


def minute(hhmm):
    h, m = hhmm.split(":")
    return (int(h) - 1) * 60 + int(m)


# ------------------------------------------------------------------ figures

def ways_back_in(img, d, cfg):
    """The core idea on the real example: five ways back in, and the one action that closes each."""
    rows = ways()
    header(d, 100, 180, "THE WAYS BACK IN, AT 01:40", ROSE, 640)
    header(d, 1080, 180, "WHAT CLOSES EACH ONE", GREEN, 640)
    for i, (way, closer) in enumerate(rows):
        y = 260 + i * 122
        cell(d, 100, y, 640, 90, way, ROSE, 28)
        draw.arrow_right(d, 770, y + 45, 1050, MUTED, 5)
        cell(d, 1080, y, 640, 90, closer, GREEN, 28)
    caption(d, "%d ways, %d different actions: leave one open and the attacker comes back." % (len(rows), len(set(c for _, c in rows))))


def attack_timeline(img, d, cfg):
    """The attacker's script on a clock, and the rows copied as time goes by."""
    script = [(ln[:5], ln[7:]) for ln in (s.strip() for s in section("ONE", "THE ATTACKER'S SCRIPT"))]
    f = facts(section("ONE", "HOW FAST"))
    rate, start = re.search(r"(\d+) rows a minute, from (\d\d:\d\d)", f["rate"]).groups()
    total = int(f["table"].split()[0])
    end = f["all gone"].split()[-1]
    x0, x1 = 160, 1760
    t_end = minute(end) + 10

    def px(t):
        return x0 + t * (x1 - x0) / t_end
    header(d, 60, 175, "THE INPUT: THE ATTACKER'S SCRIPT", ROSE)
    for k, (t, what) in enumerate(script):
        y = 240 + k * 54
        label(d, 80, y, t, ROSE, 30, True)
        fit_mono(d, 200, y + 2, what, TEXT, 28, 1600)
    header(d, 60, 470, "THE RESULT: ROWS COPIED (%s A MINUTE FROM %s)" % (rate, start), AMBER)
    base, top = 880, 600
    for k, (t, _) in enumerate(script):
        x = px(minute(t))
        d.line([x, 560, x, base], fill=(120, 50, 70), width=2)
        label(d, x, 550, t, ROSE, 22, True, "md")
    xs, xe = px(minute(start)), px(minute(end))
    d.polygon([(xs, base), (xe, top), (xe, base)], fill=(90, 60, 20), outline=AMBER)
    d.line([x0, base, x1, base], fill=LINE, width=3)
    label(d, xe - 14, top - 10, "all %d at %s" % (total, end), ROSE, 28, True, "rd")
    marks = [(facts(section("FOUR", "BEFORE"))["rows copied"], "the alert"), (facts(section("FOUR", "AFTER"))["copying stopped"], "isolated")]
    for k, (text, what) in enumerate(marks):
        t, rows = re.search(r"\((\d\d:\d\d) - .*= (\d+)", text).groups()
        x, h = px(minute(t)), (base - top) * int(rows) / total
        d.line([x, base - h - 60 - k * 70, x, base], fill=GREEN, width=3)
        label(d, x + 10, base - h - 60 - k * 70, "%s %s: %s rows" % (t, what, rows), GREEN, 26, True, "lm")
    label(d, px(minute(end)), base + 12, end, MUTED, 24, True, "ma")
    caption(d, "Every minute nobody acts costs %s more rows." % rate, AMBER, 950)


def checklist(img, d, cfg):
    """The six things, three organisations, and when each one finds out."""
    lines = section("TWO", "THE SIX THINGS")
    cols = lines[0].split()
    rows = [(ln[:32].strip(), ln[32:].split()) for ln in lines[1:-1]]
    score = re.findall(r"\d of 6", lines[-1])
    header(d, 60, 170, "THE INPUT: " + heading_of("TWO", "THE SIX THINGS").split(",")[0], CYAN)
    for j, c in enumerate(cols):
        label(d, 1000 + j * 290 + 130, 235, c, TEXT, 30, True, "ma")
    for i, (thing, vals) in enumerate(rows):
        y = 280 + i * 64
        label(d, 60, y + 26, thing, MUTED, 28, True, "lm")
        for j, v in enumerate(vals):
            cell(d, 1000 + j * 290, y, 260, 52, v, GREEN if v == "yes" else ROSE, 26)
    y = 280 + len(rows) * 64 + 6
    label(d, 60, y + 26, "in place", TEXT, 28, True, "lm")
    for j, s in enumerate(score):
        cell(d, 1000 + j * 290, y, 260, 52, s, AMBER, 28)
    found = section("TWO", "WHEN THE BREACH")
    header(d, 60, 760, "THE RESULT: WHEN THE BREACH IS FOUND", AMBER)
    for j, ln in enumerate(found):
        who, rest = ln[:12].strip(), ln[12:].strip()
        by, when = rest[:14].strip(), rest[14:].strip()
        clock = when.split(", ")[-1]
        cell(d, 1000 + j * 290, 830, 260, 52, clock, ROSE if "customer" in by else GREEN, 26)
        label(d, 1000 + j * 290 + 130, 890, by, MUTED, 22, False, "ma")
    label(d, 60, 856, "found at", TEXT, 28, True, "lm")


def _step_colour(effect):
    if "BACK" in effect or "lost" in effect or "not done" in effect:
        return ROSE
    if "by hand" in effect or "no tooling" in effect or "gone:" in effect:
        return AMBER
    return CYAN


def response(name):
    """Before the response, its steps, and after it: the five ways back in, closed or open."""
    def draw_it(img, d, cfg):
        before = facts(section(name, "BEFORE"))
        steps = [(ln[:5], ln[7:27].strip(), ln[27:].strip()) for ln in section(name, "THE RESPONSE")]
        after_lines = section(name, "AFTER")
        after = facts(after_lines)
        still_open = {ln.strip() for ln in after_lines if ln.startswith("  ")}
        when = re.search(r"\((.*)\)", heading_of(name, "BEFORE")).group(1)
        # before
        header(d, 40, 170, "BEFORE (%s)" % when, ROSE, 460)
        panel(d, 40, 230, 460, 300, ROSE)
        rows = before["rows copied"]
        n = re.findall(r"(\d+) of (\d+)", rows)[0]
        label(d, 270, 255, "rows copied", MUTED, 26, True, "ma")
        label(d, 270, 295, "%s of %s" % n, ROSE, 40, True, "ma")
        fit_mono(d, 270, 360, rows.split(" = ")[0] if "=" in rows else rows.split(", ")[1], MUTED, 22, 420, "ma")
        label(d, 270, 420, "ways back in", MUTED, 26, True, "ma")
        label(d, 270, 460, before["ways back in"], ROSE, 36, True, "ma")
        # steps
        header(d, 540, 170, "THE RESPONSE: %d STEPS" % len(steps), CYAN, 700)
        h = min(60, (900 - 235) // len(steps))
        for k, (t, action, effect) in enumerate(steps):
            y = 235 + k * h
            c = _step_colour(effect)
            cell(d, 540, y, 700, h - 8, "%s  %s" % (t, action), c, 26, anchor="lm")
        # after
        header(d, 1280, 170, "AFTER: THE WAYS BACK IN", GREEN if not still_open else ROSE, 600)
        for k, (way, _) in enumerate(ways()):
            y = 235 + k * 62
            is_open = way in still_open
            cell(d, 1280, y, 600, 52, ("OPEN  " if is_open else "closed  ") + way, ROSE if is_open else GREEN, 22, anchor="lm")
        for k, key in enumerate(("copying stopped", "evidence", "out of service", "the attacker")):
            y = 560 + k * 80
            label(d, 1280, y, key, MUTED, 24, True)
            v = after[key]
            bad = v.startswith(("never", "came back", "memory no"))
            fit_mono(d, 1280, y + 32, v, ROSE if bad else GREEN, 24, 600)
        caption(d, CAPTIONS[name], AMBER, 935)
    return draw_it


CAPTIONS = {
    "THREE": "A reboot feels like a fix: it wipes the evidence and keeps the cron job.",
    "FOUR": "The alert saved rows, but nobody knew about svc-backup.",
    "FIVE": "Contain, capture, scope, then close every way back in.",
}

response_a = response("THREE")
response_b = response("FOUR")
response_c = response("FIVE")


def review(img, d, cfg):
    """The review table, each cell coloured good or bad, and the step where B and C part ways."""
    lines = section("SIX", "THE THREE RESPONSES")
    cols = lines[0].split()
    rows = [(ln[:28].strip(), [ln[28 + 8 * j:36 + 8 * j].strip() for j in range(3)]) for ln in lines[1:]]
    good = {"evidence kept": "yes", "root cause known": "yes", "recovered": "yes", "the attacker came back": "no"}
    header(d, 60, 170, "THE REVIEW: THE SAME BREACH, THREE RESPONSES", CYAN)
    for j, c in enumerate(cols):
        label(d, 860 + j * 330 + 150, 235, "response " + c, TEXT, 28, True, "ma")
    for i, (key, vals) in enumerate(rows):
        y = 280 + i * 62
        label(d, 60, y + 25, key, MUTED, 28, True, "lm")
        nums = [int(re.sub(r"\D", "", v) or 10 ** 9) * (1440 if "days" in v else 1) for v in vals]
        for j, v in enumerate(vals):
            if key in good:
                c = GREEN if v == good[key] else ROSE
            elif "out of service" in key:
                c = TEXT
            else:
                c = GREEN if nums[j] == min(nums) else ROSE
            cell(d, 860 + j * 330, y, 300, 50, v, c, 26)
    part = section("SIX", "WHERE B AND C")
    y = 800
    header(d, 60, y, "WHERE B AND C PART WAYS", AMBER)
    for k, ln in enumerate(part[1:]):
        who, step = ln[:14].strip(), ln[14:].strip()
        cell(d, 700 + k * 600, y - 6, 560, 56, "%s: %s" % (who, step), ROSE if k == 0 else GREEN, 26)
    fit_mono(d, 60, 880, "same first steps: " + part[0][14:].strip(), MUTED, 26, 1800)
