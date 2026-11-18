"""Pictures for the security chaos engineering video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints from real
connections through Toxiproxy and the tests check, so a picture can never disagree with the code.
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
    """The n-th section of an act whose header starts with `head`: its header line and its lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = [ln]
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[n]


def cols(line):
    return re.split(r"\s{2,}", line.strip())


def runs(name, head):
    """The experiment lines of a section as (label, fault, counted, verdict)."""
    return [tuple(cols(ln)) for ln in section(name, head)[1:] if re.match(r"\s*E\d+\s", ln) and len(cols(ln)) == 4]


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


def cell(d, x, y, w, h, text, colour, size=26):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def squares(d, x, y, n, colour, cols_=10, size=44, gap=8, filled=False):
    for k in range(n):
        cx, cy = x + (k % cols_) * (size + gap), y + (k // cols_) * (size + gap)
        d.rounded_rectangle([cx, cy, cx + size, cy + size], radius=6, fill=colour if filled else BG_CELL, outline=colour, width=3)


def num(text, k=0):
    return int(re.findall(r"\d+", text)[k])


# ------------------------------------------------------------------ figures

def fail_open_closed(img, d, cfg):
    """The core idea on one call: authorization is down, and each version decides what silence means."""
    first = runs("TWO", "THE FIRST VERSION")[0]
    hard = runs("TWO", "THE HARDENED VERSION")[0]
    deadline = section("ONE", "THE DEADLINE")[0].split(": ", 1)[1]
    header(d, 60, 210, "ONE PAYMENT, AUTHORIZATION DOWN (%s)" % first[0], ROSE)
    draw.box(d, 60, 300, 340, 140, "payments service", "asks: may this account pay?", CYAN)
    draw.box(d, 760, 300, 340, 140, "Toxiproxy", "the proxy is disabled", ROSE)
    draw.box(d, 1460, 300, 400, 140, "authorization", "never reached", MUTED)
    draw.arrow_right(d, 410, 370, 750, CYAN, 5)
    draw.arrow_right(d, 1110, 370, 1450, ROSE, 5)
    label(d, 1280, 460, "refused: no answer", ROSE, 28, True, "ma")
    label(d, 60, 470, "deadline: " + deadline, MUTED, 26)
    for k, (title, rule, row, colour) in enumerate([("THE FIRST VERSION", "no answer -> allow", first, ROSE),
                                                    ("THE HARDENED VERSION", "no answer -> deny", hard, GREEN)]):
        x = 60 + k * 920
        header(d, x, 560, title, colour, 880)
        panel(d, x, 620, 880, 260, colour, 3)
        fit_mono(d, x + 40, 650, "if (authz.isEmpty()) " + ("approve" if k == 0 else "deny") + "();", TEXT, 34, 800)
        label(d, x + 40, 720, rule, colour, 32, True)
        label(d, x + 40, 790, row[2] + "  ->  control " + row[3], colour, 34, True)
    caption(d, "Silence from a security check is a decision: someone chose allow or deny.", AMBER, 950)


def controls_map(img, d, cfg):
    """The service, its five controls through their proxies, and the faults that can be injected."""
    controls = [cols(ln) for ln in section("ONE", "THE CONTROLS")[2:]]
    faults = [cols(ln) for ln in section("ONE", "THE FAULTS")[1:]]
    version = re.search(r"Toxiproxy (\S+),", section("ONE", "THE FAULTS")[0]).group(1)
    draw.box(d, 60, 470, 300, 140, "payments", "service", CYAN)
    header(d, 520, 210, "TOXIPROXY %s: ONE PROXY EACH" % version, AMBER, 560)
    header(d, 1180, 210, "THE CONTROL: WHAT V1 DOES IF IT FAILS", ROSE, 680)
    for k, (control, dep, first) in enumerate(controls):
        y = 280 + k * 120
        cell(d, 560, y, 480, 80, "proxy -> " + dep, AMBER, 28)
        cell(d, 1180, y, 680, 80, "%s: %s" % (control, first), ROSE, 28)
        d.line([(360, 540), (560, y + 40)], fill=LINE, width=3)
        draw.arrow_right(d, 1040, y + 40, 1172, AMBER, 4)
    label(d, 60, 860, "faults: " + ", ".join(f[0] for f in faults), ROSE, 30, True)
    caption(d, "Every control is a call over a network, so every control can be hurt on purpose.", AMBER, 950)


def authz_bars(img, d, cfg):
    """Approved payments with authorization down, slow or reset, in each version."""
    first = runs("TWO", "THE FIRST VERSION")
    hard = runs("TWO", "THE HARDENED VERSION")
    header(d, 60, 210, "APPROVED WHILE AUTHORIZATION FAILS", ROSE)
    for k, (a, b) in enumerate(zip(first, hard)):
        y = 300 + k * 190
        label(d, 60, y + 20, "%s  %s" % (a[0], a[1]), TEXT, 34, True)
        total = num(a[2], 1)
        for j, (row, colour, who) in enumerate([(a, ROSE, "first"), (b, GREEN, "hardened")]):
            yy = y + 60 + j * 62
            approved = num(row[2])
            label(d, 60, yy + 26, who, MUTED, 28, False, "lm")
            squares(d, 260, yy, total, MUTED, total, 52, 12)
            squares(d, 260, yy, approved, colour, total, 52, 12, True)
            label(d, 260 + total * 64 + 30, yy + 26, "%s  %s" % (row[2], row[3]), colour, 32, True, "lm")
    caption(d, "Rose squares are payments approved with nobody saying yes.", AMBER, 950)


def fraud_trickle(img, d, cfg):
    """The fraud score hung or on a trickle, and the trickle worked out against the deadline."""
    first = runs("THREE", "THE FIRST VERSION")
    hard = runs("THREE", "THE HARDENED VERSION")
    how = section("THREE", "HOW THE TRICKLE")[1:]
    m = re.search(r"(\d+) bytes / (\d+) bytes a second = ([\d.]+) s > ([\d.]+) s", how[1])
    size, rate, secs, deadline = m.group(1), m.group(2), float(m.group(3)), float(m.group(4))
    header(d, 60, 210, "THE FIRST VERSION", ROSE, 860)
    header(d, 1000, 210, "THE HARDENED VERSION", GREEN, 860)
    for k, row in enumerate(first):
        cell(d, 60, 280 + k * 90, 860, 70, "%s %s: %s" % (row[0], row[1], row[2]), ROSE, 28)
    for k, row in enumerate(hard):
        cell(d, 1000, 280 + k * 90, 860, 70, "%s %s: %s" % (row[0], row[1], row[2]), GREEN, 28)
    header(d, 60, 500, "HOW THE TRICKLE MISSES THE DEADLINE", AMBER)
    label(d, 60, 570, "%s bytes / %s bytes a second = %.1f s" % (size, rate, secs), TEXT, 32, True)
    x0, x1, y = 160, 1760, 720
    scale = (x1 - x0) / 5.0
    d.line([(x0, y), (x1, y)], fill=LINE, width=4)
    for s in range(6):
        d.line([(x0 + s * scale, y - 12), (x0 + s * scale, y + 12)], fill=LINE, width=3)
        label(d, x0 + s * scale, y + 22, "%d s" % s, MUTED, 24, False, "ma")
    d.rounded_rectangle([x0, y - 60, x0 + secs * scale, y - 20], radius=8, fill=ROSE)
    label(d, x0 + secs * scale + 12, y - 40, "the answer arrives: %.1f s" % secs, ROSE, 26, True, "lm")
    dx = x0 + deadline * scale
    d.line([(dx, y - 110), (dx, y + 70)], fill=GREEN, width=6)
    label(d, dx, y + 80, "deadline %.1f s: give up" % deadline, GREEN, 28, True, "ma")
    caption(d, "A deadline on the whole call, body included, turns a trickle into a clear failure.", AMBER, 950)


def audit_paths(img, d, cfg):
    """Twenty decisions while the audit sink is down: dropped from a small buffer, or kept in a journal."""
    first = runs("FOUR", "THE FIRST VERSION")[0]
    hard = runs("FOUR", "THE HARDENED VERSION")[0]
    dropped = [ln for ln in section("FOUR", "THE FIRST VERSION") if "dropped" in ln][0]
    waiting = [ln for ln in section("FOUR", "THE HARDENED VERSION") if "waiting" in ln][0]
    buffer = re.search(r"a buffer of (\d+)", section("FOUR", "THE FIRST VERSION")[0]).group(1)
    total = num(first[2].split("of")[1])
    for k, (title, colour, mid, row, note) in enumerate([
            ("THE FIRST VERSION", ROSE, "buffer of %s in memory" % buffer, first, dropped.split(": ", 1)[1]),
            ("THE HARDENED VERSION", GREEN, "journal on disk", hard, waiting.split(": ", 1)[1])]):
        y = 220 + k * 340
        header(d, 60, y, "%s: %s" % (title, row[2]), colour)
        squares(d, 60, y + 80, total, CYAN, 10, 40, 8)
        draw.arrow_right(d, 590, y + 130, 700, colour, 5)
        draw.box(d, 710, y + 70, 380, 130, mid, "the sink is down", colour)
        draw.arrow_right(d, 1100, y + 130, 1210, colour, 5)
        if k == 0:
            cell(d, 1220, y + 90, 640, 90, "dropped: %d of %d" % (num(note), total), ROSE, 30)
        else:
            cell(d, 1220, y + 90, 640, 90, note.split(",")[1].strip() + " of %d" % total, GREEN, 30)
        label(d, 60, y + 230, note, colour, 26, True)
    caption(d, "An audit trail that drops records fails in silence; only counting finds it.", AMBER, 950)


def limit_burst(img, d, cfg):
    """Forty payments from one account with the rate-limit store resetting, and the revocation check."""
    first = runs("FIVE", "THE FIRST VERSION")
    hard = runs("FIVE", "THE HARDENED VERSION")
    limit = int(re.search(r"more than (\d+)", "\n".join(section("FIVE", "THE HYPOTHESES"))).group(1))
    a, b = first[0], hard[0]
    total = num(a[2], 1)
    header(d, 60, 210, "%s: %d PAYMENTS FROM ONE ACCOUNT, LIMIT %d" % (a[0], total, limit), AMBER)
    for k, (row, colour, who) in enumerate([(a, ROSE, "first"), (b, GREEN, "hardened")]):
        y = 290 + k * 250
        label(d, 60, y, "%s: %s" % (who, row[2]), colour, 30, True)
        squares(d, 60, y + 50, total, MUTED, 20, 40, 8)
        squares(d, 60, y + 50, num(row[2]), colour, 20, 40, 8, True)
    label(d, 1060, 290, "local count: 1 to %d approved," % limit, GREEN, 26, True)
    label(d, 1060, 325, "%d to %d refused" % (limit + 1, total), GREEN, 26, True)
    header(d, 60, 800, "REVOKED CERTIFICATE APPROVED", ROSE)
    for k, (fa, hb) in enumerate(zip(first[1:], hard[1:])):
        x = 700 + k * 600
        cell(d, x, 800, 560, 60, "%s %s: %s / %s" % (fa[0], fa[1], re.sub(r"\D+", " ", fa[2]).split()[0],
                                                      re.sub(r"\D+", " ", hb[2]).split()[0]), AMBER, 26)
    label(d, 700, 880, "first version / hardened, of 6", MUTED, 24)
    caption(d, "When the shared count is gone, a count in the process still holds the limit.", AMBER, 950)


def probe_rounds(img, d, cfg):
    """The monitor's probes round by round: three failures raise the alert, a slow close raises nothing."""
    loud = section("SIX", "THE PROBES IN", 0)
    quiet = section("SIX", "THE PROBES IN", 1)
    alerts = {cols(ln)[0]: cols(ln)[1] for ln in section("SIX", "THE ALERTS")[1:]}
    for k, (sec, colour) in enumerate([(loud, ROSE), (quiet, GREEN)]):
        y = 220 + k * 330
        header(d, 60, y, sec[0].rstrip(":"), colour)
        for j, ln in enumerate(sec[1:]):
            text = ln.strip()
            x = 60 + j * 450
            alert = "ALERT" in text
            what = text.split("  ", 1)[1].replace("-> ALERT", "").strip()
            cell(d, x, y + 70, 420, 110, "", ROSE if alert else (AMBER if "failed" in text else GREEN))
            label(d, x + 210, y + 85, text.split("  ")[0], TEXT, 26, True, "ma")
            label(d, x + 210, y + 130, what, AMBER if "failed" in what else GREEN, 26, True, "ma")
            if alert:
                label(d, x + 210, y + 195, "ALERT", ROSE, 32, True, "ma")
    label(d, 60, 860, "first version: %s;  hardened: %s;  false alarms: %s" % (
        alerts["first version"], alerts["hardened"], alerts["false alarms"]), TEXT, 28, True)
    caption(d, "The alert delay is counted in probes, not seconds, so it is the same on every machine.", AMBER, 950)


def scoreboard_grid(img, d, cfg):
    """The ten experiments, two hypotheses each, in both versions."""
    lines = [ln for ln in section("SEVEN", "THE SCOREBOARD")[1:] if re.match(r"\s*E\d+", ln)]
    rows = [re.split(r"\s+", ln.strip()) for ln in lines]
    header(d, 760, 200, "FIRST VERSION", ROSE, 500)
    header(d, 1320, 200, "HARDENED", GREEN, 500)
    for j, t in enumerate(["control", "alert", "control", "alert"]):
        label(d, 885 + (j % 2) * 250 + (j // 2) * 560, 254, t, MUTED, 22, True, "ma")
    for k, r in enumerate(rows):
        y = 290 + k * 60
        name = " ".join(r[1:-4])
        label(d, 60, y + 25, "%s  %s" % (r[0], name), TEXT, 26, True, "lm")
        for j, v in enumerate(r[-4:]):
            x = 760 + (j % 2) * 250 + (j // 2) * 560
            cell(d, x, y, 240, 50, v, GREEN if v == "held" else ROSE, 24)
    score = [ln.strip() for ln in section("SEVEN", "HOW THE SCORE")[1:]]
    label(d, 60, 900, "%s   |   %s" % (re.sub(r"\s{2,}", ": ", score[1]), re.sub(r"\s{2,}", ": ", score[2])), AMBER, 28, True)
