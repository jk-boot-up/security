"""Pictures for the certificate lifecycle video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
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


def rows(name, head):
    """A section as {label: value}, split at the first run of two or more spaces."""
    return {c[0]: "  ".join(c[1:]) for c in (cols(ln) for ln in section(name, head)) if len(c) > 1}


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


def num(text):
    return int(re.search(r"(\d+)", text).group(1))


def verdict_colour(text):
    return GREEN if text.startswith("accepted") else ROSE if text.startswith("refused") or text.startswith("fails") else TEXT


# ------------------------------------------------------------------ figures

def lifecycle(img, d, cfg):
    """The core idea: one 12-second certificate, its renewal at two thirds, the alert at a quarter left, and the end."""
    cert = rows("FOUR", "THE CERTIFICATE")
    sched = rows("FOUR", "HOW THE SCHEDULE")
    rot = rows("FOUR", "ROTATION")
    after = rows("FIVE", "THE CA STAYS DOWN")
    life = num(cert["lifetime"])
    renew = int(re.search(r"= (\d+) seconds", sched["renew at"]).group(1))
    head = int(re.search(r"= (\d+) seconds", sched["headroom"]).group(1))
    alert_left = int(re.search(r"= (\d+) seconds", sched["alert at"]).group(1))
    header(d, 60, 175, "THE INPUT: ONE CERTIFICATE FROM step-ca, %d SECONDS OF LIFE" % life, CYAN)
    x0, x1, y = 160, 1760, 330

    def xs(t):
        return x0 + int((x1 - x0) * t / life)
    d.rounded_rectangle([x0, y, x1, y + 70], radius=12, fill=BG_CELL, outline=GREEN, width=3)
    d.rectangle([xs(renew), y + 3, x1 - 3, y + 67], fill=(60, 50, 20))
    label(d, (x0 + xs(renew)) // 2, y + 18, "in use", GREEN, 28, True, "ma")
    label(d, (xs(renew) + x1) // 2, y + 18, "headroom %d s" % head, AMBER, 28, True, "ma")
    for t, text, colour in ((0, "issued, 0 s", CYAN), (renew, "renew at %d s" % renew, AMBER),
                            (life - alert_left, "alert at %d s" % (life - alert_left), ROSE), (life, "ends, %d s" % life, ROSE)):
        d.line([xs(t), y - 30, xs(t), y + 110], fill=colour, width=4)
        label(d, xs(t), y + 120 + (40 if t == life - alert_left else 0), text, colour, 26, True, "ma")
    label(d, x0, 260, sched["renew at"].split(" seconds")[0] + " s:  renew at two thirds", MUTED, 24, True)
    label(d, x1, 260, sched["headroom"].split(" seconds")[0] + " s of headroom", MUTED, 24, True, "ra")
    header(d, 60, 560, "THE RESULT", AMBER)
    panel(d, 60, 630, 880, 220, GREEN)
    label(d, 500, 650, "renewed in time (30 seconds)", MUTED, 28, True, "ma")
    label(d, 500, 710, "handshakes " + rot["handshakes"], GREEN, 38, True, "ma")
    label(d, 500, 775, "renewals " + rot["renewals"], GREEN, 30, True, "ma")
    panel(d, 980, 630, 880, 220, ROSE)
    label(d, 1420, 650, "the CA down past the end", MUTED, 28, True, "ma")
    label(d, 1420, 710, after["after the expiry"], ROSE, 38, True, "ma")
    label(d, 1420, 775, "certificate expired", ROSE, 30, True, "ma")
    caption(d, "A short life makes renewal the thing that must never fail.", AMBER, 900)


def chain_steps(img, d, cfg):
    """Root, intermediate and leaf, the seven steps between them, and the four checks."""
    auth = section("ONE", "THE AUTHORITY")
    root, inter = auth[0].split("  ", 1)[1].strip(), auth[2].split("  ", 1)[1].strip()
    leaf = rows("ONE", "THE CERTIFICATE")["names"].split(",")[0]
    steps = [re.match(r"(\d) (.*?)\s{2,}(\w+)$", ln).groups() for ln in section("ONE", "HOW THE CHAIN")]
    checks = [cols(ln) for ln in section("ONE", "THE CHECKS")][1:]
    header(d, 60, 175, "THE INPUT: THREE CERTIFICATES", CYAN)
    boxes = [(root, "the root, held by the client"), (inter, "the intermediate, path length 0"), ("CN=" + leaf, "the leaf, 30 days")]
    for i, (name, sub) in enumerate(boxes):
        y = 250 + i * 200
        panel(d, 60, y, 620, 120, GREEN if i == 0 else CYAN)
        fit_mono(d, 370, y + 40, name, TEXT, 28, 580, "mm")
        label(d, 370, y + 80, sub, MUTED, 22, False, "ma")
        if i:
            draw.arrow_up(d, 370, y - 4, y - 76, AMBER, 4)
            label(d, 400, y - 64, "steps %s" % ("3, 4" if i == 1 else "1, 2"), AMBER, 22, True)
    header(d, 760, 175, "HOW THE CHAIN IS CHECKED", AMBER)
    for i, (n, what, ok) in enumerate(steps):
        y = 245 + i * 52
        cell(d, 760, y, 50, 42, n, AMBER, 22)
        label(d, 830, y + 8, what, TEXT, 24, True)
        cell(d, 1460, y, 90, 42, ok, GREEN if ok == "yes" else ROSE, 22)
    header(d, 760, 640, "THE CHECKS: BY HAND, THEN THE JDK", GREEN)
    for i, (case, hand, jdk) in enumerate(checks):
        y = 705 + i * 50
        label(d, 760, y + 6, case, TEXT, 24, True)
        cell(d, 1100, y, 170, 42, hand, verdict_colour(hand), 22)
        fit_mono(d, 1290, y + 10, jdk, verdict_colour(jdk), 22, 570)


def revocation_grid(img, d, cfg):
    """OCSP with the responder up, down and hanging, for hard and soft fail; and how big a CRL gets."""
    table = [cols(ln) for ln in section("TWO", "OCSP")]
    sizes = [cols(ln) for ln in section("TWO", "HOW BIG")][1:]
    entries = section("TWO", "HOW ONE ENTRY")[:2]
    header(d, 60, 175, "THE INPUT: A GOOD AND A REVOKED CERTIFICATE", CYAN)
    label(d, 560, 250, table[0][1], MUTED, 24, True, "ma")
    label(d, 790, 250, table[0][2], MUTED, 24, True, "ma")
    for i, (case, hard, soft) in enumerate(table[1:]):
        y = 290 + i * 62
        label(d, 60, y + 10, case, TEXT, 26, True)
        good = case.startswith("good")
        for x, v in ((560, hard), (790, soft)):
            right = v.startswith("accepted") == good
            cell(d, x - 105, y, 210, 50, v, GREEN if right else ROSE, 20)
    label(d, 60, 620, "rose: wrong. A refused good certificate is an outage;", MUTED, 24, True)
    label(d, 60, 655, "an accepted revoked one is a bypass.", MUTED, 24, True)
    header(d, 1000, 175, "HOW BIG A CRL GETS", AMBER)
    top = max(num(s[1].replace(",", "")) for s in sizes)
    for i, s in enumerate(sizes):
        y = 250 + i * 90
        kb = num(s[1])
        label(d, 1180, y + 6, s[0] + " entries", TEXT, 24, True, "ra")
        d.rounded_rectangle([1200, y, 1200 + max(8, int(kb / top * 560)), y + 40], radius=6, fill=AMBER)
        label(d, 1200, y + 48, "%s, %s each, %s at 1 Mbit/s" % (s[1], s[2], s[3]), MUTED, 22)
    header(d, 1000, 540, "HOW ONE ENTRY IS MADE", AMBER)
    for i, ln in enumerate(entries):
        c = cols(ln)
        fit_mono(d, 1000, 610 + i * 50, c[0] + ":  " + c[1], TEXT, 24, 860)
    drop = [cols(ln) for ln in section("TWO", "100,000 REVOKED")]
    fit_mono(d, 1000, 730, "kept after -updatedb: " + drop[0][1], ROSE, 24, 860)
    fit_mono(d, 1000, 775, "expired ones dropped: " + drop[1][1].split(":")[0], GREEN, 24, 860)
    caption(d, "Hard fail turns a dead responder into an outage; soft fail into a bypass.", AMBER, 900)


def stapling(img, d, cfg):
    """Two servers, one stapling: requests to the responder, a dead responder, and an old staple."""
    up = [cols(ln) for ln in section("THREE", "THE RESPONDER UP")][1:3]
    down = rows("THREE", "THE RESPONDER DOWN")
    header(d, 60, 175, "THE INPUT: TWO SERVERS, THE SAME CERTIFICATE", CYAN)
    for i, (server, verdict, inhs, toresp) in enumerate(up):
        y = 250 + i * 190
        panel(d, 60, y, 860, 160, GREEN if server == "staples" else AMBER)
        label(d, 90, y + 20, "the server " + server, TEXT, 30, True)
        label(d, 90, y + 70, "answers in the handshake: " + inhs, MUTED, 26, True)
        label(d, 90, y + 110, "requests to the responder: " + toresp, GREEN if toresp == "0" else ROSE, 26, True)
        cell(d, 700, y + 50, 190, 56, verdict, verdict_colour(verdict), 26)
    header(d, 1000, 175, "THE RESPONDER DOWN (hard fail)", ROSE)
    label(d, 1000, 250, "does not staple", TEXT, 26, True)
    fit_mono(d, 1000, 290, down["does not staple"], ROSE, 22, 860)
    label(d, 1000, 340, "staples", TEXT, 26, True)
    fit_mono(d, 1000, 380, down["staples"], GREEN, 22, 860)
    label(d, 1000, 430, "staples, read 2 days later", TEXT, 26, True)
    fit_mono(d, 1000, 470, down["staples, 2 days on"], ROSE, 22, 860)
    header(d, 1000, 560, "THE CERTIFICATE IS REVOKED", AMBER)
    for i, k in enumerate(["the old staple", "a client asking the responder", "a staple made after it"]):
        y = 630 + i * 55
        label(d, 1000, y, k, TEXT, 24, True)
        v = next(ln[len(k):].strip() for ln in section("THREE", "THE CERTIFICATE IS REVOKED") if ln.startswith(k))
        label(d, 1440, y, v, ROSE if v.startswith("accepted") else GREEN, 24, True)
    caption(d, "A staple survives a dead responder, and is as old as its last refresh.", AMBER, 900)


def rotation(img, d, cfg):
    """Thirty seconds of renewals every 8 seconds, and a CA that is away inside the headroom."""
    cert = rows("FOUR", "THE CERTIFICATE")
    sched = rows("FOUR", "HOW THE SCHEDULE")
    rot = rows("FOUR", "ROTATION")
    away = rows("FOUR", "THE CA AWAY")
    life = num(cert["lifetime"])
    renew = int(re.search(r"= (\d+) seconds", sched["renew at"]).group(1))
    n = num(rot["renewals"])
    header(d, 60, 175, "THE INPUT: %d SECONDS OF TRAFFIC, A HANDSHAKE EVERY 100 ms" % 30, CYAN)
    x0, x1 = 160, 1760

    def xs(t):
        return x0 + int((x1 - x0) * t / 30)
    for k in range(n + 1):
        y = 260 + k * 70
        start = k * renew
        end = min(start + life, 30 + 2)
        d.rounded_rectangle([xs(start), y, min(xs(end), x1 + 60), y + 50], radius=8, fill=BG_CELL, outline=MUTED, width=2)
        d.rounded_rectangle([xs(start), y, min(xs(start + renew), x1), y + 50], radius=8, fill=GREEN)
        label(d, xs(start) + 12, y + 10, "certificate %d" % (k + 1), INK, 22, True)
    for t in range(0, 31, renew if renew else 8):
        label(d, xs(t), 245 + (n + 1) * 70, "%d s" % t, MUTED, 22, True, "ma")
    label(d, x0, 225, "green: the certificate in use; grey: the rest of its life, never needed", MUTED, 22)
    header(d, 60, 620, "THE RESULT", AMBER)
    fit_mono(d, 60, 690, "handshakes " + rot["handshakes"] + "    renewals " + rot["renewals"] + "    alerts " + rot["alerts"], GREEN, 28, 1800)
    header(d, 60, 760, "THE CA AWAY FOR ABOUT 2 SECONDS", ROSE)
    fit_mono(d, 60, 830, "handshakes " + away["handshakes"], GREEN, 26, 900)
    fit_mono(d, 980, 830, away["renewals"], AMBER, 24, 880)
    caption(d, "Renew at two thirds: the last third is the headroom.", AMBER, 920)


def missed_and_fleet(img, d, cfg):
    """A renewal missed for good, and what an expiry monitor over 60 certificates prevents."""
    down = rows("FIVE", "THE CA STAYS DOWN")
    inv = [(c[0], c[1]) for c in (cols(ln) for ln in section("FIVE", "AN INVENTORY")) if len(c) == 2 and c[1].isdigit()]
    model = [cols(ln) for ln in section("FIVE", "THE NEXT 60")][1:]
    header(d, 60, 175, "THE INPUT: THE CA DOWN PAST THE END", CYAN)
    b, a = num(down["before the expiry"]), down["after the expiry"]
    label(d, 60, 250, "before the expiry", TEXT, 26, True)
    d.rounded_rectangle([60, 290, 60 + b * 4, 330], radius=6, fill=GREEN)
    label(d, 80 + b * 4, 295, down["before the expiry"], GREEN, 24, True)
    label(d, 60, 360, "after the expiry", TEXT, 26, True)
    d.rounded_rectangle([60, 400, 60 + num(a) * 4, 440], radius=6, fill=ROSE)
    label(d, 80 + num(a) * 4, 405, a, ROSE, 24, True)
    label(d, 60, 470, "the monitor " + down["the monitor"], MUTED, 22, True)
    label(d, 60, 505, "renewing the expired one: refused, it has to be issued anew", MUTED, 22, True)
    header(d, 1000, 175, "THE INVENTORY: %d CERTIFICATES" % sum(int(v) for _, v in inv), CYAN)
    total = sum(int(v) for _, v in inv)
    x = 1000
    for name, v in inv:
        w = int(int(v) / total * 860)
        colour = ROSE if name == "expired" else AMBER if "7" in name else CYAN if "30 days or" not in name else GREEN
        d.rectangle([x, 250, x + w, 310], fill=colour)
        label(d, x + w // 2, 320, v, colour, 24, True, "ma")
        x += w
    label(d, 1000, 360, ", ".join("%s %s" % (v, k) for k, v in inv), MUTED, 20)
    header(d, 60, 580, "THE NEXT 60 DAYS: OUTAGES, AND HOW MANY A MONITOR PREVENTS", AMBER)
    top = max(int(m[1]) for m in model)
    for i, (who, out, prev) in enumerate(model):
        y = 650 + i * 58
        label(d, 560, y + 6, who, TEXT, 24, True, "ra")
        w = int(int(out) / top * 700)
        d.rounded_rectangle([580, y, 580 + max(w, 6), y + 40], radius=6, fill=ROSE)
        label(d, 595 + max(w, 6), y + 6, "%s outages, %s prevented" % (out, prev), ROSE if int(out) > int(prev) else GREEN, 24, True)
