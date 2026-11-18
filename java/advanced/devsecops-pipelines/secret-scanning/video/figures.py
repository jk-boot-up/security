"""Pictures for the secret scanning video, drawn by the `figure` slide kind.

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
    """The full header line of a section."""
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


def verdict_colour(v):
    return {"found": GREEN, "quiet": GREEN, "accepted": GREEN, "refused": GREEN, "rejected": GREEN,
            "missed": ROSE, "flagged": ROSE, "made": ROSE}.get(v, TEXT)


def planted():
    """(real, name, added, removed or 0) for each planted string."""
    rows = []
    for ln in section("TWO", "THE PLANTED"):
        m = re.match(r"(real|fake)\s+(.+?)\s{2,}added (\d+), (?:removed (\d+)|still there)", ln)
        rows.append((m.group(1) == "real", m.group(2), int(m.group(3)), int(m.group(4) or 0)))
    return rows


def commits():
    return [re.match(r"commit (\d+)\s+(.*)", ln).groups() for ln in section("TWO", "THE REPOSITORY")]


# ------------------------------------------------------------------ figures

def still_in_history(img, d, cfg):
    """The core idea: a key removed by a later commit is gone from the files, not from the history."""
    key = next(p for p in planted() if p[1] == "payment key")
    clone = section("TWO", "A FRESH CLONE")[0]
    cs = commits()
    header(d, 60, 175, "THE INPUT: FIVE COMMITS OF THE BANK'S REPOSITORY", CYAN)
    step = 360
    for i, (n, subject) in enumerate(cs):
        x = 80 + i * step
        colour = ROSE if int(n) == key[2] else GREEN if int(n) == key[3] else MUTED
        cell(d, x, 250, 300, 64, "commit " + n, colour, 28)
        words = subject.split()
        half = (len(words) + 1) // 2
        label(d, x + 150, 330, " ".join(words[:half]), MUTED, 22, False, "ma")
        label(d, x + 150, 360, " ".join(words[half:]), MUTED, 22, False, "ma")
        if i < len(cs) - 1:
            draw.arrow_right(d, x + 304, 282, x + step - 4, MUTED, 3)
    xa, xr = 80 + (key[2] - 1) * step, 80 + (key[3] - 1) * step
    label(d, xa + 150, 420, "+ payment.key=sk_live_...", ROSE, 24, True, "ma")
    label(d, xr + 150, 420, "- payment.key=sk_live_...", GREEN, 24, True, "ma")
    d.line([xa + 150, 470, xr + 150, 470], fill=ROSE, width=8)
    label(d, (xa + xr) // 2 + 150, 490, "the key is in the history from commit %d on, for good" % key[2], ROSE, 26, True, "ma")
    header(d, 60, 580, "THE RESULT", AMBER)
    panel(d, 60, 640, 880, 200, GREEN)
    label(d, 500, 670, "the files now (commit %s)" % cs[-1][0], MUTED, 28, True, "ma")
    label(d, 500, 730, "no payment key", GREEN, 40, True, "ma")
    panel(d, 980, 640, 880, 200, ROSE)
    label(d, 1420, 670, "a fresh clone (git clone)", MUTED, 28, True, "ma")
    answer = clone.rsplit(": ", 1)[1]
    label(d, 1420, 730, "holds the payment key: " + answer, ROSE, 40, True, "ma")
    label(d, 1420, 790, "every clone carries the whole history", MUTED, 24, False, "ma")
    caption(d, "Removing a file does not remove a secret: revoke the key.", AMBER, 900)


def entropy_steps(img, d, cfg):
    """Entropy worked out for the base64 key, and the ceiling that keeps hex below the threshold."""
    inp = section("ONE", "THE INPUT")[0]
    groups = [ln for ln in section("ONE", "HOW THE ENTROPY") if "=" in ln and "x" in ln and not ln.startswith("H =")]
    total = next(ln for ln in section("ONE", "HOW THE ENTROPY") if ln.startswith("H = ") and "threshold" in ln)
    h = float(re.search(r"H = ([\d.]+)", total).group(1))
    threshold = float(re.search(r"threshold ([\d.]+)", total).group(1))
    look = section("ONE", "A LOOK-ALIKE")
    hh = float(re.search(r"H = ([\d.]+)", look[1]).group(1))
    ceiling = float(re.search(r"= ([\d.]+),", look[1]).group(1))
    header(d, 60, 175, "THE INPUT: " + heading("ONE", "THE INPUT").split("(")[1].rstrip("):"), CYAN)
    fit_mono(d, 60, 240, inp, TEXT, 30, 1100)
    header(d, 60, 310, "HOW THE ENTROPY IS MADE", AMBER)
    for i, g in enumerate(groups):
        name, work = g.split(":", 1)
        bits = float(work.rsplit("= ", 1)[1])
        y = 375 + i * 70
        label(d, 60, y, name, TEXT, 26, True)
        fit_mono(d, 400, y + 2, work.strip(), MUTED, 24, 560)
        d.rounded_rectangle([980, y, 980 + int(bits * 150), y + 36], radius=6, fill=AMBER)
    label(d, 60, 375 + len(groups) * 70, "H = %.3f bits per character (the bars added up)" % h, AMBER, 28, True)
    # the scale on the right, from 3 bits (the floor) to 5
    x0, y0, y1, floor, top = 1420, 820, 300, 3.0, 5.0

    def yv(v):
        return int(y0 - (y0 - y1) * (v - floor) / (top - floor))
    header(d, 1380, 175, "AGAINST THE THRESHOLD", GREEN)
    d.line([x0 - 20, y0, x0 + 320, y0], fill=LINE, width=2)
    label(d, x0 - 30, y0, "%.1f" % floor, MUTED, 22, True, "rm")
    for x, v, name, colour in ((x0, h, "base64 key", ROSE), (x0 + 170, hh, "checksum", GREEN)):
        d.rectangle([x, yv(v), x + 130, y0], fill=colour)
        label(d, x + 65, yv(v) - 36, "%.3f" % v, colour, 26, True, "ma")
        label(d, x + 65, y0 + 10, name, MUTED, 22, True, "ma")
    d.line([x0 - 20, yv(threshold), x0 + 320, yv(threshold)], fill=AMBER, width=4)
    label(d, x0 + 330, yv(threshold), "%.1f" % threshold, AMBER, 26, True, "lm")
    d.line([x0 + 150, yv(ceiling), x0 + 320, yv(ceiling)], fill=CYAN, width=3)
    label(d, x0 + 330, yv(ceiling), "hex %.1f" % ceiling, CYAN, 22, True, "lm")
    caption(d, "Random-looking text scores high; hex can never pass %.1f." % threshold, AMBER, 910)


def history_bars(img, d, cfg):
    """Each planted string as a bar from the commit that added it to the one that removed it."""
    rows = planted()
    n = len(commits())
    now = section("TWO", "A SCAN OF THE FILES NOW")[0]
    hist = section("TWO", "A SCAN OF THE WHOLE HISTORY")[0]
    header(d, 60, 170, "THE INPUT: 9 PLANTED STRINGS, FROM THE COMMIT THAT ADDED EACH ONE", CYAN)
    x0, step = 640, 200
    for c in range(1, n + 1):
        label(d, x0 + (c - 1) * step + step // 2, 230, "commit %d" % c, MUTED, 22, True, "ma")
    for i, (real, name, added, removed) in enumerate(rows):
        y = 270 + i * 54
        colour = ROSE if real else GREEN
        label(d, 60, y + 4, ("real  " if real else "fake  ") + name, colour, 24, True)
        end = (removed - 1) if removed else n
        d.rounded_rectangle([x0 + (added - 1) * step + 10, y, x0 + end * step - 10, y + 34], radius=8, fill=colour if real else BG_CELL,
                            outline=colour, width=3)
        if removed:
            label(d, x0 + end * step, y + 4, "removed", MUTED, 20, False)
    xn = x0 + (n - 1) * step
    d.rectangle([xn, 255, xn + step, 270 + len(rows) * 54], outline=AMBER, width=4)
    label(d, xn + step // 2, 270 + len(rows) * 54 + 8, "the files now", AMBER, 22, True, "ma")
    header(d, 60, 800, "THE RESULT: REAL SECRETS FOUND", AMBER)
    fit_mono(d, 60, 860, "files now:  " + now, AMBER, 26, 900)
    fit_mono(d, 980, 860, "history:  " + hist, GREEN, 26, 880)
    caption(d, "Only the strings that reach the last commit are in the files.", AMBER, 935)


def verdict_grid(img, d, cfg):
    """The nine strings with each scanner's verdict, and recall and precision."""
    rows = [cols(ln) for ln in section("THREE", "THE 9 STRINGS")[1:]]
    score = [cols(ln) for ln in section("THREE", "HOW THE SCORE")[1:]]
    header(d, 60, 175, "THE 9 STRINGS (the truth, from what was planted)", CYAN)
    label(d, 900, 240, "ours", MUTED, 26, True, "ma")
    label(d, 1150, 240, "gitleaks", MUTED, 26, True, "ma")
    for i, (truth, a, b) in enumerate(rows):
        y = 280 + i * 62
        fit_mono(d, 60, y + 26, truth, ROSE if truth.startswith("real") else GREEN, 26, 700, "lm")
        cell(d, 790, y, 220, 52, a, verdict_colour(a), 26)
        cell(d, 1040, y, 220, 52, b, verdict_colour(b), 26)
    header(d, 1320, 175, "HOW THE SCORE IS MADE", AMBER)
    for i, (name, a, b) in enumerate(score):
        y = 260 + i * 150
        label(d, 1320, y, name, TEXT, 26, True)
        fit_mono(d, 1320, y + 45, "ours      " + a, MUTED, 26, 560)
        fit_mono(d, 1320, y + 85, "gitleaks  " + b, MUTED, 26, 560)
    caption(d, "Found and quiet are right; missed and flagged are wrong.", AMBER, 900)


def allow_and_baseline(img, d, cfg):
    """An allowlist removes the look-alike; a baseline leaves only the new finding."""
    before = section("FOUR", "BEFORE THE ALLOWLIST")[0]
    after = section("FOUR", "AFTER THE ALLOWLIST")[0]
    allow = section("FOUR", "THE ALLOWLIST")[0]
    base_head = heading("FOUR", "THE BASELINE")
    base = [cols(ln) for ln in section("FOUR", "THE BASELINE")]
    new = section("FOUR", "AFTER ONE MORE")
    header(d, 60, 175, "THE ALLOWLIST", CYAN)
    fit_mono(d, 60, 240, allow, TEXT, 24, 760)
    header(d, 60, 310, "BEFORE", ROSE)
    fit_mono(d, 60, 375, before, ROSE, 24, 760)
    header(d, 60, 440, "AFTER", GREEN)
    fit_mono(d, 60, 505, after, GREEN, 24, 760)
    header(d, 940, 175, "THE BASELINE: " + re.search(r"\((.*)\)", base_head).group(1).upper(), AMBER)
    for i, (commit, file, rule) in enumerate(base):
        y = 240 + i * 50
        fit_mono(d, 940, y, commit, MUTED, 24, 160)
        fit_mono(d, 1110, y, file, TEXT, 24, 300)
        fit_mono(d, 1430, y, rule, MUTED, 24, 440)
    y = 240 + len(base) * 50 + 20
    header(d, 940, y, "AFTER ONE MORE COMMIT", ROSE)
    fit_mono(d, 940, y + 65, new[0], ROSE, 26, 900)
    for j, ln in enumerate(new[1:]):
        c = cols(ln)
        fit_mono(d, 940, y + 110 + j * 45, c[0], ROSE, 24, 160)
        fit_mono(d, 1110, y + 110 + j * 45, c[1], ROSE, 24, 300)
        fit_mono(d, 1430, y + 110 + j * 45, c[2], ROSE, 24, 440)
    label(d, 60, 600, "a known finding is listed once,", MUTED, 26, True)
    label(d, 60, 640, "and only a new one fails the build", MUTED, 26, True)
    caption(d, "Look-alikes are allowlisted; known findings are baselined.", AMBER, 935)


def hooks(img, d, cfg):
    """A commit with a key against the laptop's hook and the server's hook."""
    laptop = section("FIVE", "THE INPUT")
    push = section("FIVE", "THE RESULT")[0]
    clean = section("FIVE", "AFTER THE KEY")[0]
    first = section("FIVE", "A CLEAN FIRST PUSH")[0]
    hooks_ = [cols(ln) for ln in section("FIVE", "THE HOOKS")]
    header(d, 60, 175, "THE INPUT: A COMMIT WITH THE PAYMENT KEY", CYAN)
    panel(d, 60, 240, 820, 520, CYAN)
    label(d, 470, 260, "the laptop", TEXT, 32, True, "ma")
    label(d, 470, 310, hooks_[0][0] + ": " + hooks_[0][1].split(", ")[1], MUTED, 24, False, "ma")
    for i, ln in enumerate(laptop):
        what, rest = ln.split(": ", 1)
        verdict = rest.split(",")[0]
        y = 380 + i * 170
        label(d, 100, y, what, TEXT, 28, True)
        cell(d, 100, y + 50, 300, 64, verdict, verdict_colour(verdict), 30)
        label(d, 430, y + 82, rest.split(", ")[1], MUTED, 24, False, "lm")
    draw.arrow_right(d, 890, 500, 1010, AMBER, 5)
    label(d, 950, 450, "git push", AMBER, 26, True, "ma")
    panel(d, 1020, 240, 840, 520, GREEN)
    label(d, 1440, 260, "the shared repository", TEXT, 32, True, "ma")
    label(d, 1440, 310, hooks_[1][0] + ": " + hooks_[1][1].split(", ")[1], MUTED, 24, False, "ma")
    what, rest = push.split(": ", 1)
    verdict = rest.split(",")[0]
    label(d, 1060, 380, "the push, " + what, TEXT, 28, True)
    cell(d, 1060, 430, 300, 64, verdict, verdict_colour(verdict), 30)
    label(d, 1390, 462, rest.split(", ")[1], MUTED, 24, False, "lm")
    label(d, 1060, 560, "a clean first push: " + first.split(",")[0], MUTED, 26, True)
    label(d, 1060, 610, "a clean change later: " + clean.split(",")[0], MUTED, 26, True)
    caption(d, "A hook on the laptop can be skipped; one on the server cannot.", AMBER, 900)
