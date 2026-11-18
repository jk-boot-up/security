"""Pictures for the testssl.sh video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and the
tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)
SEV = {"CRITICAL": ROSE, "HIGH": ROSE, "MEDIUM": AMBER, "LOW": AMBER, "INFO": MUTED, "OK": GREEN}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading."""
    return re.search(r"^%s\. .*\n((?:(?![A-Z]+\. ).*\n?)*)" % name, OUT, re.M).group(1).splitlines()


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


def facts(lines):
    """`key   value` lines as a dict; a line that only continues the one above is joined to it."""
    merged = []
    for ln in lines:
        if ln.startswith("    ") and merged:
            merged[-1] += " " + ln.strip()
        else:
            merged.append(ln)
    lines = merged
    return dict(tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip()))


def rows(lines):
    """Report rows (id, severity, finding)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=2)) for ln in lines]


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


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 20, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def verdict_colour(text):
    return GREEN if text.startswith("pass") else ROSE


# ------------------------------------------------------------------ figures

def one_finding(img, d, cfg):
    """One finding of the guided server, from testssl's JSON row to the shop's gate."""
    report = {r[0]: r for r in rows(section("ONE", "THE REPORT"))}
    rid, sev, text = report["cert_revocation"]
    rule = next(ln.split() for ln in section("ONE", "THE GATE") if ln.split()[1] == rid)
    exc = facts(section("ONE", "THE EXCEPTION"))
    res = facts(section("ONE", "THE RESULT"))
    steps = [
        ("WHAT testssl.sh WROTE", CYAN, ['{"id": "%s",' % rid, ' "severity": "%s",' % sev, ' "finding": "%s"}' % text]),
        ("THE SHOP'S RULE", AMBER, ["rule %s reads %s" % (rule[0], rule[1]), "it says HIGH, so the rule is: %s" % rule[-1]]),
        ("THE EXCEPTION", AMBER, ["rule    " + exc["rule"], "reason  " + exc["reason"]]),
        ("THE GATE", verdict_colour(res["the gate"]), ["rules broken %s, excepted %s" % (res["rules broken"], res["of them excepted"]),
                                                      "the gate: " + res["the gate"]]),
    ]
    y = 180
    for i, (title, colour, lines) in enumerate(steps):
        h = 40 + 46 * len(lines)
        panel(d, 420, y, 1440, h, colour, 3)
        header(d, 60, y + h // 2 - 22, title, colour, 330, 22)
        for k, ln in enumerate(lines):
            fit_mono(d, 450, y + 24 + k * 46, ln, TEXT, 28, 1380)
        if i < len(steps) - 1:
            draw.arrow(d, 1140, y + h + 4, 1140, y + h + 30, MUTED, 4)
        y += h + 36
    caption(d, "The tool reports with its own severity; the shop's rule decides; an exception carries a reason.", AMBER, 950)


def rule_table(img, d, cfg):
    """The shop's eleven rules, each reading one testssl id, over the guided server's report."""
    rules = [ln.split() for ln in section("ONE", "THE GATE")]
    res = facts(section("ONE", "THE RESULT"))
    excepted = res["of them excepted"].strip("[]").split(", ")
    header(d, 60, 175, "THE SHOP'S %d RULES, OVER THE GUIDED SERVER'S REPORT" % len(rules), CYAN)
    for i, r in enumerate(rules):
        rule, reads, result = r[0], " ".join(r[1:-1]), r[-1]
        x, y = 60 + (i % 2) * 610, 245 + (i // 2) * 105
        colour = GREEN if result == "holds" else AMBER if rule in excepted else ROSE
        panel(d, x, y, 590, 90, colour, 3)
        label(d, x + 22, y + 12, rule, colour, 30, True)
        fit_mono(d, x + 22, y + 52, "reads " + reads, MUTED, 22, 420)
        label(d, x + 570, y + 45, result if rule not in excepted else "broken, excepted", colour, 24, True, "rm")
    gate = res["the gate"]
    cx, cy = 1600, 470
    d.ellipse([cx - 150, cy - 150, cx + 150, cy + 150], fill=BG_CELL, outline=verdict_colour(gate), width=8)
    label(d, cx, cy, gate.upper(), verdict_colour(gate), 80, True, "mm")
    label(d, cx, cy + 190, "the gate", MUTED, 30, True, "ma")
    fit_mono(d, cx, cy + 240, "testssl HIGH: " + res["testssl HIGH or CRITICAL"], ROSE, 24, 560, "ma")
    caption(d, "Green: the rule holds. Amber: broken, with a written exception.", AMBER, 950)


def copied_vs_guided(img, d, cfg):
    """The guided lines against the copied ones, the HSTS time worked out, and the four broken rules."""
    guided = facts(section("ONE", "THE SERVER"))
    copied = facts(section("TWO", "THE COPIED LINES"))
    hsts = facts(section("TWO", "THE HSTS TIME"))
    res = facts(section("TWO", "THE RESULT"))
    header(d, 60, 175, "BEFORE: THE GUIDED LINES", GREEN)
    header(d, 980, 175, "AFTER: THE COPIED LINES", ROSE)
    for i, key in enumerate(("ssl_ciphers", "add_header", "server_tokens")):
        y = 240 + i * 70
        label(d, 60, y + 26, key, MUTED, 26, True, "lm")
        cell(d, 290, y, 640, 54, guided[key], GREEN, 24)
        cell(d, 980, y, 880, 54, copied[key], ROSE, 24)
    header(d, 60, 470, "THE HSTS TIME, WORKED OUT", AMBER)
    for i, key in enumerate(("max-age", "in days", "minimum", "result", "copied")):
        y = 535 + i * 52
        label(d, 60, y, key, MUTED, 26, True)
        fit_mono(d, 260, y, hsts[key], AMBER if key in ("result", "copied") else TEXT, 28, 780)
    header(d, 1120, 470, "THE RULES IT BREAKS", ROSE)
    for i, rule in enumerate(res["broken"].split(", ")):
        cell(d, 1120 + (i % 2) * 380, 535 + (i // 2) * 80, 360, 62, rule, ROSE, 28)
    label(d, 1120, 720, "the gate: " + res["the gate"], TEXT, 28, True)
    label(d, 1120, 770, "testssl HIGH: " + res["testssl HIGH or CRITICAL"], MUTED, 26, True)
    caption(d, "A browser still shows the padlock: none of these four is visible to a customer.", AMBER, 950)


def two_gates(img, d, cfg):
    """The same three servers through a gate on testssl's severity and through the shop's rules."""
    by_sev = facts(section("THREE", "A GATE ON"))
    shop = facts(section("THREE", "THE SHOP'S GATE"))
    rating = [ln.split() for ln in section("THREE", "THE RATING")]
    header(d, 60, 175, "ONE FINDING, TWO READERS", CYAN)
    for i, r in enumerate(rating):
        y = 245 + i * 70
        cell(d, 60, y, 200, 56, r[0], TEXT, 28)
        cell(d, 300, y, 330, 56, "testssl: " + r[2], AMBER, 26)
        draw.arrow_right(d, 650, y + 28, 740, MUTED, 4)
        cell(d, 760, y, 560, 56, "rule %s: %s" % (r[5].rstrip(":"), r[6]), ROSE, 26)
    header(d, 60, 420, "GATE ON testssl's SEVERITY", AMBER, 820)
    header(d, 1000, 420, "THE SHOP'S GATE", GREEN, 860)
    for i, server in enumerate(("guided", "copied", "legacy")):
        y = 490 + i * 100
        label(d, 60, y + 32, server, TEXT, 30, True, "lm")
        cell(d, 260, y, 620, 66, by_sev[server], MUTED, 28)
        verdict = shop[server]
        cell(d, 1000, y, 860, 66, verdict, verdict_colour(verdict), 28)
    parts = shop["broken"].split(", ")
    label(d, 1000, 800, "legacy breaks:", MUTED, 26, True)
    fit_mono(d, 1000, 840, ", ".join(parts[:3]) + ",", ROSE, 28, 860)
    fit_mono(d, 1000, 880, ", ".join(parts[3:]), ROSE, 28, 860)
    caption(d, "On severity alone the three look the same; the shop's rules tell them apart.", AMBER, 950)


def cert_checks(img, d, cfg):
    """The two certificates clients refuse: the expiry and the name, worked out."""
    exp = section("FOUR", "THE EXPIRY")
    good_h = int(re.search(r"good\s+(\d+) hours", exp[0]).group(1))
    days_left = re.search(r"days left (.*), so (\S+)", exp[1]).groups()
    bad_h = int(re.search(r"expired\s+(\d+) hours", exp[2]).group(1))
    name = facts(section("FOUR", "THE NAME"))
    res = facts(section("FOUR", "THE RESULT"))
    header(d, 60, 175, "THE EXPIRY (not after - not before), each on its own scale", AMBER)
    for i, (who, before, after, colour, note) in enumerate((
            ("good", -1, good_h - 1, GREEN, "%d hours = %d days; %s left, so %s" % (good_h, good_h // 24, days_left[0].split(" >=")[0] + " days", days_left[1])),
            ("expired", -(bad_h + 24), -24, ROSE, "%d hours, and it ended before the scan" % bad_h))):
        y = 255 + i * 120
        lo, hi = min(before, 0) - abs(after - before) * 0.1, max(after, 0) + abs(after - before) * 0.1
        x0, x1 = 300, 1840

        def px(h):
            return x0 + (h - lo) * (x1 - x0) / (hi - lo)
        label(d, 60, y + 22, who, TEXT, 30, True, "lm")
        d.line([x0, y + 22, x1, y + 22], fill=LINE, width=3)
        d.rectangle([px(before), y, px(after), y + 44], fill=colour)
        d.line([px(0), y - 14, px(0), y + 58], fill=TEXT, width=4)
        label(d, px(0) + 10, y - 14, "the scan", TEXT, 22, True, "ld")
        label(d, x0, y + 58, note, colour, 26, True, "la")
    header(d, 60, 520, "THE NAME (is the name asked for listed?)", CYAN)
    for i, key in enumerate(("asked for", "certificate", "listed")):
        y = 590 + i * 70
        label(d, 60, y + 27, key, MUTED, 28, True, "lm")
        colour = ROSE if key == "listed" else TEXT
        cell(d, 300, y, 760, 56, name[key], colour, 28)
    header(d, 1160, 520, "THE RESULT", ROSE)
    for i, key in enumerate(("expired", "another name")):
        y = 590 + i * 90
        label(d, 1160, y, key, MUTED, 28, True)
        fit_mono(d, 1160, y + 38, res[key], ROSE, 28, 700)
    caption(d, "Every browser refuses both: nothing else about the server matters until they are fixed.", AMBER, 950)


def false_alarm(img, d, cfg):
    """The same server scanned by name and by address: only the name asked for changes."""
    by_addr = facts(section("FIVE", "THE NAME"))
    res = facts(section("FIVE", "THE RESULT"))
    asked_name = facts(section("FOUR", "THE NAME"))["asked for"]
    cmd = section("FIVE", "THE SCAN BY ADDRESS")
    for i, (title, asked, cert, listed, verdict) in enumerate((
            ("BY NAME", asked_name, by_addr["certificate"], "yes", res["by name"]),
            ("BY ADDRESS", by_addr["asked for"], by_addr["certificate"], by_addr["listed"], res["by address"].split("  <-")[0]))):
        y = 190 + i * 300
        colour = verdict_colour(verdict)
        header(d, 60, y, title, colour, 260)
        for k, (key, value) in enumerate((("asked for", asked), ("certificate", cert))):
            label(d, 360, y + 22 + k * 76, key, MUTED, 26, True, "lm")
            cell(d, 560, y + k * 76, 640, 54, value, TEXT, 28)
        draw.arrow_right(d, 1220, y + 60, 1300, MUTED, 4)
        cell(d, 1320, y + 30, 240, 60, "listed: " + listed.split(",")[0], colour, 28)
        cell(d, 1590, y + 30, 270, 60, verdict, colour, 26)
    label(d, 1725, 640, "A FALSE ALARM", ROSE, 30, True, "ma")
    fit_mono(d, 60, 720, " ".join(ln.strip() for ln in cmd), MUTED, 26, 1800)
    caption(d, "The server did not change, only the name asked for: scan by the name customers use.", AMBER, 950)
