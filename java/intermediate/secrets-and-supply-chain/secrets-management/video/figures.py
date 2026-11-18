"""Pictures for the secrets management video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
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


def section(name, head):
    """The lines of the first section of an act whose header starts with `head` (four spaces removed)."""
    found, cur = None, None
    for ln in act(name):
        if not ln.startswith("  "):
            if found is not None:
                break
            if ln.startswith(head):
                found = []
                cur = ln
        elif found is not None:
            found.append(ln[2:])
    return found


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


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


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def ok(value):
    return GREEN if value == "true" else ROSE


# ------------------------------------------------------------------ figures

def blast_radius(img, d, cfg):
    """One login copied by a thief at the same moment: how long each kind keeps working."""
    thief = section("FOUR", "A LOGIN A THIEF")
    copied = int(re.search(r"t=(\d+)", heading("FOUR", "A LOGIN A THIEF")).group(1))
    leased = re.search(r"t=(\d+): .* = (\d+) s", thief[0]).groups()
    renewed = re.search(r"t=(\d+): .* = (\d+) s", thief[1]).groups()
    revoked = thief[2].rsplit(": ", 1)[1]
    static = section("FOUR", "A STATIC PASSWORD")[0].split(": ")[1].split()[0]
    header(d, 60, 180, "A LOGIN A THIEF COPIES AT t=%d, AND HOW LONG IT STILL WORKS" % copied, CYAN)
    x0, x1, end = 520, 1700, 4000

    def px(t):
        return x0 + t * (x1 - x0) / end
    rows = [("a leased login", 0, int(leased[0]), "%s s more" % leased[1], AMBER),
            ("renewed by the thief", 0, int(renewed[0]), "%s s more, then the maximum" % renewed[1], AMBER),
            ("revoked when noticed", 0, copied, "works right after: " + revoked, GREEN),
            ("a static password", 0, end, "three years later: " + static, ROSE)]
    for i, (name, a, b, note, colour) in enumerate(rows):
        y = 290 + i * 140
        label(d, 60, y + 25, name, TEXT, 30, True, "lm")
        d.rectangle([px(a), y, px(b), y + 50], fill=LINE)
        d.rectangle([px(copied), y, px(b), y + 50], fill=colour)
        if b == end:
            draw.arrow_right(d, px(b) - 10, y + 25, x1 + 160, colour, 6)
        label(d, px(copied) + 10, y + 62, note, colour, 26, True, "la")
    tx = px(copied)
    d.line([tx, 260, tx, 860], fill=TEXT, width=4)
    label(d, tx, 255, "copied at t=%d" % copied, TEXT, 26, True, "md")
    for t in (0, int(leased[0]), int(renewed[0])):
        label(d, px(t), 870, "t=%d" % t, MUTED, 24, False, "ma")
    caption(d, "A lease limits a leak by itself; revoking ends it at once; a static password never ends.", AMBER, 940)


def leak_places(img, d, cfg):
    """What the scanner finds, how a value is judged random-looking, and the image whose layer keeps the key."""
    header(d, 60, 180, "WHAT THE SCANNER FINDS", ROSE)
    for i, ln in enumerate(section("ONE", "WHAT THE SCANNER FINDS")):
        where, rule = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        y = 245 + i * 60
        fit_mono(d, 60, y, where, TEXT, 24, 520)
        label(d, 600, y + 2, rule, ROSE, 24, True)
    header(d, 60, 450, "ENTROPY, BITS PER CHARACTER", AMBER)
    bars = [("the token", section("ONE", "HOW THE TOKEN")), ("a class name", section("ONE", "THE SAME SUM"))]
    x0, scale, cols = 300, 150, (CYAN, GREEN, AMBER, ROSE)
    for i, (name, lines) in enumerate(bars):
        y = 530 + i * 130
        parts = [float(v) for v in re.findall(r"= (\d\.\d\d)$", "\n".join(lines), re.M)[:-1]]
        total = re.search(r"= (\d\.\d\d), (.*)$", lines[-1]).groups()
        label(d, 60, y + 25, name, TEXT, 28, True, "lm")
        x = x0
        for k, p in enumerate(parts):
            d.rectangle([x, y, x + p * scale, y + 50], fill=cols[k % 4], outline=INK, width=2)
            x += p * scale
        flagged = "not flagged" not in total[1]
        label(d, x + 14, y + 25, total[0], ROSE if flagged else GREEN, 30, True, "lm")
        label(d, x0, y + 62, total[1].split(": ")[1], ROSE if flagged else GREEN, 24, True, "la")
    tx = x0 + 4.0 * scale
    d.line([tx, 505, tx, 790], fill=TEXT, width=4)
    label(d, tx, 800, "threshold 4.0", TEXT, 24, True, "ma")
    header(d, 1140, 180, heading("ONE", "THE IMAGE"), CYAN)
    layers = [re.split(r"\s{2,}", ln.strip()) for ln in section("ONE", "THE IMAGE")]
    for i, parts in enumerate(reversed(layers)):
        y = 250 + i * 110
        hides = len(parts) > 2
        key = "payment.key" in parts[1] and not hides
        d.rounded_rectangle([1140, y, 1840, y + 90], radius=12, fill=PANEL, outline=ROSE if key else LINE, width=3)
        label(d, 1165, y + 45, parts[0], MUTED, 26, True, "lm")
        fit_mono(d, 1300, y + 45, parts[1], ROSE if key else TEXT, 26, 520, "lm")
    seen = section("ONE", "WHAT THE RUNNING")[0].strip()
    kept = section("ONE", "WHAT ANYONE")[0].split("  <-")[0].strip()
    label(d, 1140, 600, "the running container:", MUTED, 26, True)
    fit_mono(d, 1140, 640, seen, GREEN, 26, 700)
    label(d, 1140, 700, "anyone with the image file:", MUTED, 26, True)
    fit_mono(d, 1140, 740, kept, ROSE, 26, 700)
    caption(d, "Scanners read names and randomness; a later layer only hides a file.", AMBER, 940)


def permission_bits(img, d, cfg):
    """The mounted secret file before and after the fix, permission letter by letter, and the redacted log line."""
    who = [("owner", CYAN), ("group", AMBER), ("others", ROSE)]
    for row, (head, colour) in enumerate((("BEFORE THE FIX", ROSE), ("AFTER THE FIX", GREEN))):
        facts = dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in section("TWO", head))
        y = 180 + row * 300
        header(d, 60, y, head, colour)
        perms = facts["permissions"]
        for i, ch in enumerate(perms):
            name, c = who[i // 3]
            x = 60 + i * 100 + (i // 3) * 30
            if i % 3 == 0:
                label(d, x + 145, y + 70, name, c, 26, True, "ma")
            cell(d, x, y + 110, 88, 80, ch, c if ch != "-" else LINE, 40)
        res = facts["at startup"]
        label(d, 1080, y + 120, "at startup", MUTED, 26, True)
        bad = res.startswith("refused")
        words = res.split(", ")
        for k, w in enumerate(words[:2] if not bad else [res.split(": ")[0], res.split(": ")[1]]):
            label(d, 1080, y + 160 + k * 44, w, ROSE if bad and k == 1 else GREEN if not bad else TEXT, 28, True)
    log = dict(re.split(r"\s{2,}", ln.strip(), maxsplit=1) for ln in section("TWO", "THE SECRET WRITTEN"))
    header(d, 60, 790, "THE CODE", CYAN)
    fit_mono(d, 280, 812, log["the code"], TEXT, 26, 1560, "lm")
    header(d, 60, 860, "THE LOG", GREEN)
    fit_mono(d, 280, 882, log["the log"], GREEN, 26, 1560, "lm")
    caption(d, "Read only if no one else can; the Secret type prints [redacted].", AMBER, 950)


def lease_timeline(img, d, cfg):
    """Both leases on one time line: the reports lease ends unrenewed, the shop's renewals stop at the maximum."""
    lines = section("THREE", "THE LEASES OVER TIME")
    reqs = section("THREE", "THE REQUESTS")
    users = [re.search(r"(v-\S+) until t=(\d+)", ln).groups() for ln in reqs if "until" in ln]
    renewals = [tuple(int(v) for v in re.match(r"t=(\d+)\s+min.* = (\d+)$", ln.strip()).groups())
                for ln in lines if "min(" in ln]
    maxi = int(re.search(r"t=(\d+)", heading("THREE", "AT t=")).group(1))
    x0, x1, end = 380, 1840, maxi + 400

    def px(t):
        return x0 + t * (x1 - x0) / end
    header(d, 60, 180, heading("THREE", "THE LEASES OVER TIME"), CYAN)
    # the reports lease: issued at 0, never renewed
    y = 300
    label(d, 60, y + 25, users[1][0], TEXT, 28, True, "lm")
    d.rectangle([px(0), y, px(int(users[1][1])), y + 50], fill=AMBER)
    label(d, px(int(users[1][1])) + 12, y + 25, "ends at t=%s, never renewed: removed" % users[1][1], AMBER, 26, True, "lm")
    # the shop lease: one segment per renewal
    y = 440
    label(d, 60, y + 25, users[0][0], TEXT, 28, True, "lm")
    start, until = 0, int(users[0][1])
    d.rectangle([px(0), y, px(until), y + 50], fill=GREEN)
    for k, (t, new_end) in enumerate(renewals):
        d.rectangle([px(until), y, px(new_end), y + 50], fill=GREEN if k % 2 else (40, 170, 120))
        d.line([px(t), y - 18, px(t), y + 68], fill=TEXT, width=3)
        label(d, px(t) - (8 if k == len(renewals) - 1 else 0), y + 78 + (k % 2) * 34, "renew t=%d" % t, MUTED, 22, True,
              "ra" if k == len(renewals) - 1 else "ma")
        until = new_end
    tx = px(maxi)
    d.line([tx, 270, tx, 640], fill=ROSE, width=5)
    label(d, tx, 650, "maximum t=%d" % maxi, ROSE, 26, True, "ma")
    header(d, 60, 720, "HOW EACH NEW END IS WORKED OUT", AMBER)
    for k, ln in enumerate(l for l in lines if "min(" in l):
        fit_mono(d, 60 + (k % 2) * 900, 790 + (k // 2) * 44, ln.strip(), TEXT if not ln.strip().endswith(str(maxi)) else ROSE,
                 24, 860)
    caption(d, "Renewals extend a lease by its time to live, never past the maximum.", AMBER, 945)


def rotation_steps(img, d, cfg):
    """The three steps of a key rotation, and which key versions work at each."""
    rows = [re.split(r"\s{2,}", ln.strip()) for ln in section("FOUR", "THE PAYMENT KEY")]
    header(d, 60, 180, heading("FOUR", "THE PAYMENT KEY"), CYAN)
    for i, parts in enumerate(rows):
        step, versions = parts[0], parts[1]
        status = dict(re.findall(r"(v\d) (true|false)", " ".join(parts[2:])))
        x = 60 + i * 610
        d.rounded_rectangle([x, 270, x + 570, 800], radius=16, fill=PANEL, outline=LINE, width=2)
        num, text = step.split(" ", 1)
        label(d, x + 285, 300, "step " + num, MUTED, 26, True, "ma")
        label(d, x + 285, 345, text, TEXT, 30, True, "ma")
        label(d, x + 285, 430, "the provider accepts", MUTED, 24, False, "ma")
        fit_mono(d, x + 285, 480, versions, AMBER, 36, 520, "ma")
        for k, (v, works) in enumerate(sorted(status.items())):
            cell(d, x + 60, 580 + k * 100, 450, 76, "%s %s" % (v, "works" if works == "true" else "refused"), ok(works), 30)
        if i < 2:
            draw.arrow_right(d, x + 575, 535, x + 605, MUTED, 4)
    caption(d, "Two versions overlap during the changeover, so there is never a moment with no working key.", AMBER, 900)


def the_bill(img, d, cfg):
    """What the gains cost: four lines of the demo's bill."""
    costs = [ln.strip() for ln in section("FIVE", "WHAT IT COSTS")]
    header(d, 60, 180, "WHAT IT COSTS", AMBER)
    for i, c in enumerate(costs):
        head, _, rest = c.partition(": ")
        x, y = 60 + (i % 2) * 910, 270 + (i // 2) * 290
        d.rounded_rectangle([x, y, x + 880, y + 250], radius=16, fill=PANEL, outline=AMBER, width=3)
        f, words, rows = font(SANS_B, 32), head.split(), [""]
        for w in words:
            rows[-1:] = [rows[-1] + " " + w] if f.getlength(rows[-1] + " " + w) < 820 else [rows[-1], w]
        for k, r in enumerate(rows):
            label(d, x + 30, y + 40 + k * 44, r.strip(), TEXT, 32, True)
        label(d, x + 30, y + 150, rest, AMBER, 30, False)
    nxt = section("FIVE", "NEXT")[0].strip()
    label(d, W // 2, 880, "next: " + nxt, CYAN, 28, True, "ma")
    caption(d, "The secrets service becomes the most valuable system you run.", AMBER, 945)
