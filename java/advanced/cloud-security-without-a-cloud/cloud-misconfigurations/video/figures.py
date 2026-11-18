"""Pictures for the cloud misconfigurations video, drawn by the `figure` slide kind.

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


def section(name, head, n=0):
    return sections(name, head)[n]


def facts(lines):
    """`key   value` lines as a dict (the key is the text before the first run of two spaces)."""
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


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def findings(lines):
    """`SEVERITY rule resource` lines as tuples."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=2)) for ln in lines if ln.split()[0] in SEV]


def bucket_table(lines):
    """The bucket table of a snapshot: {name: {column: value}}."""
    cols = lines[0].split()
    rows = {}
    for ln in lines[1:]:
        parts = ln.split()
        if not parts[0].startswith("bank-"):
            break
        rows[parts[0]] = dict(zip(cols[1:], parts[1:]))
    return rows


def snapshot_rest(lines):
    """The non-bucket lines of a snapshot, as {key: value}."""
    return facts([ln for ln in lines[1:] if not ln.startswith("bank-")])


# ------------------------------------------------------------------ figures

RULE_OF = {"public": "s3-public-acl", "block": "s3-no-public-access-block", "encryption": "s3-no-encryption",
           "versions": "s3-no-versioning"}
GOOD = {"public": "no", "block": "all", "versions": "on"}


def one_bucket(img, d, cfg):
    """The core idea: one bucket's settings, read by the SDK, and the rule each bad setting fires."""
    account = section("ONE", "THE ACCOUNT")
    table = bucket_table(account)
    scan = findings(section("ONE", "THE SCAN"))
    header(d, 60, 180, "THE SETTINGS (read by the SDK from Moto)", CYAN)
    header(d, 1120, 180, "THE FINDINGS (a rule fired)", ROSE)
    for k, name in enumerate(("bank-statements", "bank-audit")):
        top = 250 + k * 340
        label(d, 60, top, "bucket " + name, TEXT, 32, True)
        for i, (col, value) in enumerate(table[name].items()):
            y = top + 50 + i * 62
            ok = value == GOOD.get(col, value) if col != "encryption" else value != "none"
            label(d, 80, y + 25, col, MUTED, 28, True, "lm")
            cell(d, 330, y, 260, 50, value, GREEN if ok else ROSE, 28)
            rule = RULE_OF[col]
            hit = [f for f in scan if f[1] == rule and f[2] == name]
            if hit:
                draw.arrow_right(d, 610, y + 25, 1100, MUTED, 3)
                sev = hit[0][0]
                cell(d, 1120, y, 740, 50, "%s  %s" % (sev, rule), SEV[sev], 26)
            else:
                label(d, 620, y + 25, "no rule fires", GREEN, 26, True, "lm")
    n = len([f for f in scan if f[2] == "bank-statements"])
    caption(d, "One setting in, one finding out: bank-statements has %d, bank-audit none." % n, AMBER, 950)


def first_scan(img, d, cfg):
    """The 13 findings of the first version, worst first, and how they add up."""
    scan = findings(section("ONE", "THE SCAN"))
    add = facts(section("ONE", "HOW THE FINDINGS ADD UP"))
    header(d, 60, 180, "THE SCAN: %d FINDINGS, WORST FIRST" % len(scan), ROSE)
    for i, (sev, rule, res) in enumerate(scan):
        x, y = 60 + (i % 3) * 610, 250 + (i // 3) * 104
        d.rounded_rectangle([x, y, x + 590, y + 90], radius=10, fill=BG_CELL, outline=SEV[sev], width=3)
        fit_mono(d, x + 18, y + 26, rule, SEV[sev], 26, 560, "lm")
        label(d, x + 18, y + 66, "%s  %s" % (sev, res), MUTED, 22, False, "lm")
    total = int(add["by severity"].split("=")[1])
    parts = re.findall(r"(\d+) (\w+)", add["by severity"].split("=")[0])
    x, y = 60, 790
    for n, sev in parts:
        w = int(1800 * int(n) / total)
        d.rectangle([x, y, x + w - 6, y + 52], fill=SEV[sev] if sev != "LOW" else LINE)
        label(d, x + (w - 6) // 2, y + 26, "%s %s" % (n, sev), INK if sev != "LOW" else TEXT, 26, True, "mm")
        x += w
    fit_mono(d, 60, 860, "by rule: " + add["by rule"], TEXT, 28, 1800)
    caption(d, "Three critical: two buckets anyone can read, and a policy that allows everything.", AMBER, 950)


def fixes(img, d, cfg):
    """The fixes, edit or replace, and the scan before and after."""
    rows = [ln.split(None, 1) for ln in section("TWO", "THE FIXES")]
    before = section("TWO", "BEFORE THE FIXES")[0].strip()
    after_scan = section("TWO", "THE SCAN AFTER")
    header(d, 60, 180, "THE FIXES", CYAN)
    for i, (kind, what) in enumerate(rows):
        y = 250 + i * 70
        colour = GREEN if kind == "edit" else AMBER
        cell(d, 60, y, 170, 56, kind, colour, 26)
        fit_mono(d, 250, y + 28, what, TEXT, 26, 900, "lm")
    total = int(before.split("=")[1].split()[0])
    after = int(after_scan[0].split()[0])
    header(d, 1260, 180, "BEFORE", ROSE)
    label(d, 1360, 330, str(total), ROSE, 150, True, "mm")
    label(d, 1360, 440, "findings", MUTED, 28, True, "mm")
    draw.arrow_right(d, 1480, 330, 1620, MUTED, 5)
    header(d, 1660, 180, "AFTER", GREEN)
    label(d, 1740, 330, str(after), GREEN, 150, True, "mm")
    label(d, 1740, 440, "findings", MUTED, 28, True, "mm")
    fit_mono(d, 1260, 540, after_scan[1].strip(), AMBER, 28, 600)
    label(d, 1260, 610, "a disk is encrypted only", MUTED, 28)
    label(d, 1260, 655, "when it is made, so the", MUTED, 28)
    label(d, 1260, 700, "server and the disk are new", MUTED, 28)
    caption(d, "Green: changed in place. Amber: deleted and created again.", AMBER, 950)


def tool_grid(img, d, cfg):
    """Our nine rules against the checks Checkov and Trivy fired on the first files."""
    lines = section("THREE", "OUR NINE RULES")
    reports = section("THREE", "THE REPORTS")
    comp = section("THREE", "HOW THE COMPARISON")
    header(d, 60, 180, "OUR RULE  ->  THE CHECK EACH TOOL FIRED", CYAN)
    label(d, 640, 250, "checkov", MUTED, 26, True, "ma")
    label(d, 920, 250, "trivy", MUTED, 26, True, "ma")
    for i, ln in enumerate(lines[1:]):
        rule, ck, tv = ln.split()
        y = 295 + i * 64
        fit_mono(d, 60, y + 26, rule, TEXT, 26, 440, "lm")
        cell(d, 520, y, 240, 52, ck, GREEN if ck != "-" else ROSE, 24)
        cell(d, 800, y, 240, 52, tv, GREEN if tv != "-" else ROSE, 24)
    header(d, 1140, 180, "FAILED CHECKS", AMBER)
    for i, ln in enumerate(reports):
        m = re.match(r"(\S+ \S+)\s+first\s+(\d+)\s+fixed\s+(\d+)", ln.strip())
        y = 260 + i * 70
        mono(d, 1140, y, m.group(1), TEXT, 28)
        mono(d, 1500, y, "first " + m.group(2), ROSE, 28)
        mono(d, 1700, y, "fixed " + m.group(3), AMBER, 28)
    f = facts(comp)
    header(d, 1140, 430, "THE COMPARISON", GREEN)
    mono(d, 1140, 500, "found by both  " + f["found by both"], GREEN, 28)
    label(d, 1140, 560, "Checkov only:", MUTED, 26, True)
    for i, r in enumerate(f["checkov only"].split(", ")):
        mono(d, 1160, 600 + i * 40, r, AMBER, 26)
    ports = [ln.strip() for ln in comp if ln.strip().startswith("port")]
    label(d, 1140, 700, "port open to 0.0.0.0/0:", MUTED, 26, True)
    for i, p in enumerate(ports):
        m = re.match(r"port (\d+)\s+on (\S+)\s+checkov (\S+)\s+trivy (\S+)", p)
        y = 745 + i * 64
        mono(d, 1160, y + 10, "%s (%s)" % (m.group(1), m.group(2)), TEXT, 26)
        for j, ans in enumerate((m.group(3), m.group(4))):
            cell(d, 1480 + j * 200, y, 180, 50, ("checkov " if j == 0 else "trivy ") + ans, GREEN if ans == "yes" else ROSE, 22)
    caption(d, "Each tool misses something, and neither checks port 5432: use more than one.", AMBER, 950)


def imds_paths(img, d, cfg):
    """The three ways a web feature reaches the metadata service, and what each version answers."""
    resp = sections("FOUR", "THE RESPONSE")
    lanes = [
        ("A LINK FETCH: GET, no headers", resp[0]),
        ("A WEBHOOK TESTER: PUT for a token, then GET with it", resp[1]),
        ("THE SAME, THROUGH A PROXY (adds X-Forwarded-For)", resp[2]),
    ]
    for i, (title, lines) in enumerate(lanes):
        y = 180 + i * 250
        header(d, 60, y, title, CYAN)
        version = ""
        for j, ln in enumerate(lines):
            m = re.match(r"\s*(IMDSv\d)?\s*(.*)", ln)
            version = m.group(1) or version
            text = m.group(2)
            leaked = "leaked" in text and "nothing" not in text
            colour = ROSE if leaked else GREEN if re.search(r"\b(401|403)\b|nothing", text) else AMBER
            yy = y + 64 + j * 56
            if m.group(1):
                label(d, 80, yy + 24, version, TEXT, 30, True, "lm")
            cell(d, 260, yy, 1000, 48, text, colour, 26)
    label(d, 1320, 300, "IMDSv1: any GET", ROSE, 30, True)
    label(d, 1320, 345, "gets the credentials.", MUTED, 28)
    label(d, 1320, 430, "IMDSv2: a token first,", GREEN, 30, True)
    label(d, 1320, 475, "fetched with a PUT and", MUTED, 28)
    label(d, 1320, 520, "sent back in a header.", MUTED, 28)
    label(d, 1320, 605, "A feature that sets the", AMBER, 30, True)
    label(d, 1320, 650, "method and a header", MUTED, 28)
    label(d, 1320, 695, "still gets through.", MUTED, 28)
    caption(d, "Version 2 stops a simple link fetch, not a request whose method and headers are chosen.", AMBER, 950)


def emulator_gap(img, d, cfg):
    """Moto accepts what AWS refuses, and the scan of the account still sees it."""
    answers = [ln.strip() for ln in section("FIVE", "THE ANSWER FROM MOTO") if not ln.strip().startswith("(")]
    aws = re.search(r"AWS answers (\d+ \w+)", OUT).group(1)
    after = findings(section("FIVE", "THE SCAN AFTERWARDS"))
    header(d, 60, 180, "THE CALL, on bank-audit (public access blocked)", CYAN)
    label(d, 900, 260, "Moto", MUTED, 28, True, "ma")
    label(d, 1400, 260, "AWS", MUTED, 28, True, "ma")
    for i, ln in enumerate(answers):
        call, answer = re.split(r"\s{2,}", ln, maxsplit=1)
        y = 310 + i * 90
        mono(d, 60, y + 30, call, TEXT, 30, "lm")
        cell(d, 700, y, 400, 60, answer, AMBER, 28)
        cell(d, 1200, y, 400, 60, aws, GREEN, 28)
    header(d, 60, 540, "THE SCAN OF THE ACCOUNT AFTERWARDS", ROSE)
    for i, (sev, rule, res) in enumerate(after):
        cell(d, 60 + i * 620, 610, 600, 60, "%s %s" % (sev, rule), SEV[sev], 26)
    label(d, 60, 720, "The emulator stores the settings; it does not enforce them all.", MUTED, 30)
    label(d, 60, 770, "Test the code on the emulator, and scan the real account on a schedule.", MUTED, 30)
    caption(d, "An emulator tests the code, not the cloud.", AMBER, 950)
