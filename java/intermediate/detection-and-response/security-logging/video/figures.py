"""Pictures for the security logging video, drawn by the `figure` slide kind.

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


def section(name, head, n=0):
    return sections(name, head)[n]


def heading(name, head, n=0):
    return [ln for ln in act(name) if ln.startswith(head)][n].rstrip(":")


def facts(lines):
    """`key   value` lines as a dict."""
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


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def marked(d, x, y, text, size, marks, base=TEXT, anchor_mid=False):
    """A mono line with every occurrence of the strings in `marks` ({text: colour}) drawn in their colour."""
    f = font(MONO_B, size)
    pattern = "|".join(re.escape(m) for m in sorted(marks, key=len, reverse=True))
    pos = 0
    for m in re.finditer(pattern, text) if marks else []:
        d.text((x, y), text[pos:m.start()], font=f, fill=base, anchor="lm" if anchor_mid else "la")
        x += f.getlength(text[pos:m.start()])
        d.text((x, y), m.group(), font=f, fill=marks[m.group()], anchor="lm" if anchor_mid else "la")
        x += f.getlength(m.group())
        pos = m.end()
    d.text((x, y), text[pos:], font=f, fill=base, anchor="lm" if anchor_mid else "la")


def caption(d, text, colour=AMBER, y=945):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


# ------------------------------------------------------------------ figures

def one_event(img, d, cfg):
    """One transfer as the old logger wrote it, and as the security log records it: a field per question."""
    old = section("ONE", "THE OLD LOG")
    header(d, 60, 215, "THE OLD LOG (line 6)", ROSE)
    panel(d, 60, 275, 1800, 80, ROSE)
    fit_mono(d, 90, 315, old[1].strip(), TEXT, 36, 1740, "lm")
    label(d, 60, 375, "no who, no account, no address, no request", ROSE, 28, True)
    f = facts(section("ONE", "THE SAME TRANSFER"))
    header(d, 60, 440, "THE SECURITY LOG (line 6, a field per question)", GREEN)
    questions = [("when", "time"), ("what kind", "type"), ("who", "actor"), ("did what", "action"),
                 ("to what", "target"), ("from where", "source"), ("how it ended", "outcome"), ("which request", "correlation")]
    for i, (q, key) in enumerate(questions):
        x, y = 60 + (i % 4) * 455, 520 + (i // 4) * 190
        label(d, x + 4, y, q.upper(), CYAN, 24, True)
        label(d, x + 4, y + 34, key, MUTED, 24)
        colour = AMBER if key in ("actor", "source") else GREEN
        cell(d, x, y + 72, 430, 70, f[key], colour, 30)
    caption(d, "The same fields every time, so a question becomes a search by field.")


def who_did_it(img, d, cfg):
    """Every event as ana, on a time line, one lane per source address."""
    rows = [ln.split() for ln in section("ONE", "THE SECURITY LOG'S ANSWER") if re.match(r"\s*\d\d:\d\d ", ln)]
    verdict = [ln.strip() for ln in section("ONE", "THE SECURITY LOG'S ANSWER") if not re.match(r"\s*\d\d:\d\d ", ln)]
    header(d, 60, 215, heading("ONE", "THE SECURITY LOG'S ANSWER").upper(), CYAN)
    sources = []
    for r in rows:
        if r[1] not in sources:
            sources.append(r[1])
    usual = sources[0]
    times = [int(r[0][:2]) * 60 + int(r[0][3:]) for r in rows]
    x0, x1 = 380, 1860
    lo, hi = min(times), max(times)

    def px(t):
        return x0 + (t - lo) * (x1 - x0 - 190) / (hi - lo)
    for k, src in enumerate(sources):
        y = 360 + k * 230
        colour = GREEN if src == usual else ROSE
        label(d, 60, y + 10, src, colour, 32, True)
        label(d, 60, y + 55, "ana's usual address" if src == usual else "a new address", MUTED, 26)
        d.line([x0, y + 40, x1, y + 40], fill=LINE, width=3)
        for r, t in zip(rows, times):
            if r[1] != src:
                continue
            x = px(t)
            ok = r[3] == "success"
            c = (GREEN if ok else ROSE) if src == usual else (AMBER if ok else ROSE)
            if r[2] == "money-movement":
                c = ROSE if src != usual else GREEN
            cell(d, x, y - 24, 180, 56, r[2].replace("money-movement", "transfer" if src != usual else "card payment"), c, 24)
            label(d, x + 90, y + 44, r[0], TEXT, 26, True, "ma")
            label(d, x + 90, y + 80, r[3], c, 26, False, "ma")
    for i, v in enumerate(verdict):
        fit_mono(d, 60, 820 + i * 48, v, AMBER if i else TEXT, 30, 1800)
    caption(d, "Two failures, then a success, then the money left: someone else had the password.")


def redaction(img, d, cfg):
    """Each secret before and after the redactor, and how it was caught."""
    def rows(head):
        out, lines = [], section("TWO", head)
        for i, ln in enumerate(lines):
            m = re.match(r"\s*line (\d)\s+(\S+)\s+(.*)", ln)
            if m:
                value = m.group(3)
                if value.startswith("Bearer, then"):
                    value = "Bearer " + lines[i + 1].strip()
                out.append((m.group(1), m.group(2), value))
        return out
    before, after = rows("BEFORE THE REDACTOR"), rows("AFTER THE REDACTOR")
    how = section("TWO", "HOW EACH ONE WAS CAUGHT")
    by_name = set(re.split(r",\s*", how[0].split("by field name", 1)[1].strip()))
    header(d, 60, 215, "BEFORE THE REDACTOR", ROSE)
    header(d, 1160, 215, "AFTER THE REDACTOR", GREEN)
    for k, ((n, name, v), (_, _, a)) in enumerate(zip(before, after)):
        y = 290 + k * 92
        label(d, 60, y + 30, "line %s %s" % (n, name), MUTED, 24, True, "lm")
        cell(d, 360, y, 700, 60, v, ROSE, 26)
        draw.arrow_right(d, 1070, y + 30, 1150, MUTED, 4)
        cell(d, 1160, y, 440, 60, a, GREEN, 26)
        label(d, 1620, y + 30, "by field name" if name in by_name else "by its shape", CYAN if name in by_name else AMBER,
              24, True, "lm")
    mask = how[2].strip()
    label(d, 60, 780, "THE CARD, WORKED OUT", AMBER, 26, True)
    fit_mono(d, 60, 830, how[1].replace("by its shape", "").strip(), TEXT, 28, 1800)
    fit_mono(d, 60, 875, mask, AMBER, 28, 1800)
    caption(d, "Removed by the field's name; card numbers also by their shape, even in free text.")


def injection(img, d, cfg):
    """A line break that forges a plain line, and the same text kept inside one JSON field."""
    typed = section("THREE", "THE INPUT")[0].strip()
    old = [re.sub(r"^\d ", "", ln.strip()) for ln in section("THREE", "THE OLD LOG")]
    actor = section("THREE", "THE SECURITY LOG")[0].strip()
    header(d, 60, 215, "THE INPUT (a username, \\n is a line break)", CYAN)
    marked(d, 60, 290, typed, 30, {"\\n": AMBER})
    header(d, 60, 360, "THE OLD LOG: 1 failed login, 2 lines", ROSE)
    for i, ln in enumerate(old):
        y = 430 + i * 70
        bad = "FORGED" in ln
        panel(d, 60, y, 1800, 58, ROSE if bad else LINE, 3 if bad else 2)
        label(d, 90, y + 29, str(i + 1), MUTED, 26, True, "lm")
        fit_mono(d, 140, y + 29, ln.replace("  <- FORGED", ""), ROSE if bad else TEXT, 28, 1400, "lm")
        if bad:
            label(d, 1840, y + 29, "a forged admin login", ROSE, 26, True, "rm")
    header(d, 60, 600, "THE SECURITY LOG: 1 line, the break escaped", GREEN)
    panel(d, 60, 670, 1800, 64, GREEN, 3)
    marked(d, 90, 702, actor, 30, {"\\n": AMBER}, TEXT, True)
    quoted = section("THREE", "THE SECURITY LOG", 1)
    label(d, 60, 770, "Quotes too: the fake field stays inside the actor, and the outcome is still failure.", MUTED, 26)
    marked(d, 60, 815, quoted[0].strip(), 28, {'\\"': AMBER})
    marked(d, 60, 860, quoted[1].strip(), 28, {"failure": GREEN})
    caption(d, "Escaped and kept whole: the text cannot add a line or a field.")


def chain(img, d, cfg):
    """The hash chain of the morning's eight lines, and where an edit to line 6 shows."""
    rows = [re.split(r"\s{2,}", ln.strip()) for ln in section("FOUR", "THE CHAIN")]
    lines = [(r[0], r[1].split()[1]) for r in rows if r[0].startswith("line")]
    head = next(r[1] for r in rows if r[0] == "head")
    header(d, 60, 215, "THE CHAIN: EACH LINE HOLDS THE SHA-256 OF THE LINE BEFORE", CYAN)
    bw, gap = 176, 24
    for i, (name, prev) in enumerate(lines):
        x = 60 + i * (bw + gap)
        edited = name == "line 6"
        broken = name == "line 7"
        panel(d, x, 290, bw, 150, ROSE if broken else AMBER if edited else LINE, 3)
        label(d, x + bw // 2, 302, name, TEXT, 26, True, "ma")
        label(d, x + bw // 2, 345, "prev", MUTED, 22, False, "ma")
        fit_mono(d, x + bw // 2, 395, prev[:8], ROSE if broken else GREEN, 26, bw - 16, "mm")
        if i:
            draw.arrow_right(d, x - gap, 365, x, MUTED, 3)
    hx = 60 + len(lines) * (bw + gap)
    panel(d, hx, 290, 1860 - hx, 150, AMBER, 3)
    label(d, hx + (1860 - hx) // 2, 302, "head", AMBER, 26, True, "ma")
    label(d, hx + (1860 - hx) // 2, 345, "kept elsewhere", MUTED, 22, False, "ma")
    fit_mono(d, hx + (1860 - hx) // 2, 395, head[:8], AMBER, 26, 1860 - hx - 16, "mm")
    edit = facts(section("FOUR", "HOW AN EDIT IS FOUND"))
    header(d, 60, 490, heading("FOUR", "HOW AN EDIT IS FOUND").upper(), ROSE)
    for k, (key, v) in enumerate(edit.items()):
        y = 560 + k * 62
        label(d, 60, y + 25, key, MUTED, 28, True, "lm")
        val = v.split()[0]
        cell(d, 520, y, 300, 50, val, ROSE if "after" in key else GREEN, 28)
    label(d, 860, 647, "line 7 still names the old hash:", TEXT, 28, True, "lm")
    label(d, 860, 690, "the chain is broken at line 7", ROSE, 28, True, "lm")
    checks = facts(section("FOUR", "THE CHECKS"))
    last = next(k for k in checks if "against the head" in k)
    label(d, 60, 780, "An edit to the last line breaks nothing after it, so the head is compared:", TEXT, 28)
    fit_mono(d, 60, 830, "%s  ->  %s" % (last, checks[last]), AMBER, 30, 1800)
    caption(d, "Change or delete a line, and the next line's prev no longer matches.")


def bill(img, d, cfg):
    """A shipped copy that survives, a retention period that keeps the chain checkable, and the volume."""
    before = facts(section("FIVE", "BEFORE THE ATTACKER"))
    after = facts(section("FIVE", "AFTER THE ATTACKER"))
    header(d, 60, 215, "BEFORE THE ATTACKER", CYAN)
    header(d, 760, 215, "AFTER THE ATTACKER DELETES THE LOCAL LOG", ROSE)
    for k, store in enumerate(("local", "shipped")):
        y = 280 + k * 72
        label(d, 60, y + 28, store, MUTED, 30, True, "lm")
        cell(d, 260, y, 380, 56, before[store], GREEN, 28)
        cell(d, 760, y, 600, 56, after[store], ROSE if after[store].startswith("0") else GREEN, 28)
    ret = facts(section("FIVE", "RETENTION"))
    head = heading("FIVE", "RETENTION")
    header(d, 60, 450, head.split(" (")[0] + ": " + head.split("(")[1].rstrip(")"), AMBER)
    removed = int(ret["removed"].split()[0])
    kept = int(ret["kept"].split()[0])
    total = removed + kept
    x0, x1, y = 60, 1860, 525
    step = (x1 - x0) / total
    for i in range(total):
        d.rectangle([x0 + i * step + 3, y, x0 + (i + 1) * step - 3, y + 50], fill=ROSE if i < removed else GREEN)
    cx = x0 + removed * step
    d.line([cx, y - 15, cx, y + 65], fill=TEXT, width=4)
    label(d, cx, y + 62, "cutoff " + ret["cutoff"], TEXT, 26, True, "ma")
    label(d, x0, y + 105, "removed " + ret["removed"].split(",")[0], ROSE, 26, True)
    label(d, x1, y + 105, "kept " + ret["kept"].split(",")[0] + ", checked " + ret["check"], GREEN, 26, True, "ra")
    vol = facts(section("FIVE", "THE VOLUME"))
    header(d, 60, 690, "THE VOLUME, WORKED OUT", AMBER)
    for k, key in enumerate(("the morning", "one line", "bytes a day", "that is about")):
        y = 755 + k * 44
        label(d, 60, y + 20, key, MUTED, 28, True, "lm")
        fit_mono(d, 330, y + 20, vol[key], AMBER if key == "that is about" else TEXT, 30, 1500, "lm")
    caption(d, "Ship it off the host, keep it only as long as needed, and budget the bytes.")
