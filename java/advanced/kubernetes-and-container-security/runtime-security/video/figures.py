"""Pictures for the runtime security video, drawn by the `figure` slide kind.

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


def sections(name, head):
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
    return [ln.strip() for ln in sections(name, head)[n]]


def heading_of(name, head, n=0):
    return [ln for ln in act(name) if ln.startswith(head)][n].rstrip(":")


def kv(lines):
    """`key   value` lines as a dict (the first value of each key)."""
    out = {}
    for ln in lines:
        parts = re.split(r"\s{2,}", ln, maxsplit=1)
        if len(parts) == 2:
            out.setdefault(parts[0], parts[1])
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


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def alert_box(d, x, y, w, lines, colour=ROSE):
    """An alert (rule, pod, detail lines) drawn as a card; 'none' as a green card."""
    if lines == ["none"]:
        cell(d, x, y, w, 70, "no alert", GREEN, 30)
        return y + 70
    a = kv(lines)
    d.rounded_rectangle([x, y, x + w, y + 230], radius=14, fill=BG_CELL, outline=colour, width=4)
    label(d, x + 24, y + 18, "ALERT", colour, 26, True)
    fit_mono(d, x + 24, y + 62, a["rule"], colour, 32, w - 48)
    fit_mono(d, x + 24, y + 116, "pod     " + a["pod"], TEXT, 30, w - 48)
    fit_mono(d, x + 24, y + 166, "detail  " + a["detail"], TEXT, 30, w - 48)
    return y + 230


# ------------------------------------------------------------------ figures

def record_reading(img, d, cfg):
    """The core idea: one audit record of a connect, read step by step into ip:port and a pod, then judged."""
    how = section("TWO", "HOW ONE RECORD IS READ")
    rows = [re.split(r"\s{2,}|\s(?=\S)", ln, maxsplit=1) for ln in how]
    header(d, 60, 170, "ONE AUDIT RECORD: THE CALL TO THE LEDGER", CYAN)
    saddr = kv(how)["saddr"]
    hexes = saddr.split(",")[0].split()
    x = 60
    for k, (h, colour, what) in enumerate(((hexes[0], AMBER, "family"), (hexes[1], GREEN, "port"),
                                           ("<4 bytes>", MUTED, "address"))):
        cell(d, x, 250, 260, 80, h, colour, 40)
        label(d, x + 130, 345, what, colour, 26, True, "ma")
        x += 290
    colours = {"family": AMBER, "port": GREEN, "process": CYAN, "pod": CYAN, "baseline": GREEN}
    for i, (key, value) in enumerate(rows[1:]):
        y = 430 + i * 82
        d.rounded_rectangle([60, y, 1860, y + 66], radius=12, fill=PANEL, outline=LINE, width=2)
        label(d, 90, y + 33, key, colours.get(key, MUTED), 28, True, "lm")
        fit_mono(d, 300, y + 33, value.strip(), TEXT, 28, 1520, "lm")
    caption(d, "The kernel writes bytes; the sensor turns them into a pod, an address and a verdict.", AMBER, 950)


def falco_check(img, d, cfg):
    """Act ONE: the rules Falco accepts, the driver that cannot start, and the three tests on audit records."""
    rules = section("ONE", "THE RULES")
    header(d, 60, 170, "THE RULES", CYAN)
    for i, r in enumerate(rules):
        fit_mono(d, 60, 240 + i * 50, r, TEXT, 28, 1000)
    acc = kv(section("ONE", "FALCO"))["accepted"]
    cell(d, 1180, 230, 640, 70, heading_of("ONE", "FALCO").split(" (")[0] + ": " + acc, GREEN if acc == "yes" else ROSE, 26)
    drv = kv(section("ONE", "FALCO'S DRIVER"))
    header(d, 60, 420, "FALCO'S DRIVER ON THIS KERNEL", ROSE)
    fit_mono(d, 60, 490, "BTF     " + drv["BTF"], TEXT, 28, 1800)
    fit_mono(d, 60, 535, "driver  " + drv["driver"], TEXT, 28, 1800)
    fit_mono(d, 60, 580, "result  " + drv["result"], ROSE, 28, 1800)
    header(d, 60, 660, "SO: THE SAME THREE TESTS ON LINUX AUDIT RECORDS", GREEN)
    for i, ln in enumerate(section("ONE", "SO THE SAME")):
        key, rest = ln.split(None, 1)
        what, rule = [s.strip() for s in rest.split("->")]
        y = 730 + i * 58
        cell(d, 60, y, 200, 48, key, CYAN, 26)
        fit_mono(d, 290, y + 8, what, TEXT, 26, 600)
        draw.arrow_right(d, 920, y + 24, 1010, MUTED, 4)
        fit_mono(d, 1030, y + 8, rule, ROSE, 26, 820)
    caption(d, "The rules are valid; the kernel cannot load the driver, so audit stands in.", AMBER, 950)


def baseline_work(img, d, cfg):
    """Act TWO: the baseline, the three normal commands, what the sensor saw, and no alert."""
    base = kv(section("TWO", "THE BASELINE"))
    header(d, 60, 170, "THE BASELINE (the input)", CYAN)
    for i, k in enumerate(("writes", "connects", "shells")):
        fit_mono(d, 60, 240 + i * 50, "%-9s %s" % (k, base[k]), TEXT, 26, 1000)
    header(d, 60, 420, "THE WORK", CYAN)
    for i, ln in enumerate(section("TWO", "THE WORK")):
        cmd, what = re.split(r"\s{2,}", ln, maxsplit=1)
        fit_mono(d, 60, 490 + i * 56, cmd, TEXT, 28, 640)
        label(d, 720, 490 + i * 56, what, MUTED, 26)
    seen = kv(section("TWO", "WHAT THE SENSOR SAW"))
    header(d, 1180, 170, "WHAT THE SENSOR SAW", GREEN)
    for i, (k, v) in enumerate(seen.items()):
        cell(d, 1180 + i * 220, 240, 200, 70, "%s %s" % (k, v), GREEN if v == "yes" else ROSE, 26)
    header(d, 1180, 420, "THE ALERTS (the result)", GREEN)
    alert_box(d, 1180, 490, 680, section("TWO", "THE ALERTS"))
    caption(d, "Everything the service does is seen; what is on the baseline raises nothing.", AMBER, 950)


def shell_alert(img, d, cfg):
    """Act THREE: kubectl exec into a shell, the answer, and the alert with its reason."""
    header(d, 60, 170, "THE ACTION", CYAN)
    for i, ln in enumerate(section("THREE", "THE ACTION")):
        fit_mono(d, 60, 240 + i * 50, ln, TEXT, 30, 1000)
    ans = kv(section("THREE", "WHAT THE SHELL ANSWERED"))
    header(d, 60, 380, "WHAT THE SHELL ANSWERED", AMBER)
    fit_mono(d, 60, 450, "id -u  ->  " + ans["id -u"], AMBER, 30, 1000)
    header(d, 60, 560, "WHY", MUTED)
    for i, ln in enumerate(section("THREE", "WHY")):
        fit_mono(d, 60, 630 + i * 50, ln, TEXT, 26, 1000)
    header(d, 1180, 170, "THE ALERT (the result)", ROSE)
    alert_box(d, 1180, 240, 680, section("THREE", "THE ALERTS"))
    caption(d, "The service never starts a shell, so any shell in it is an alert.", AMBER, 950)


def write_prevent(img, d, cfg):
    """Act FOUR: the same write in two pods: written and alerted, refused and still alerted."""
    header(d, 60, 170, heading_of("FOUR", "THE ACTION").split(" (")[0] + ": " + section("FOUR", "THE ACTION")[0].split("  ")[0], CYAN)
    pods = [heading_of("FOUR", "THE ACTION"), heading_of("FOUR", "THE SAME ACTION")]
    names = [re.search(r"in ([\w-]+)", p).group(1) for p in pods]
    kernel = section("FOUR", "WHAT THE KERNEL ANSWERED")[0]
    for k in range(2):
        x = 60 + k * 920
        header(d, x, 250, names[k] + (", read-only root" if k else ""), AMBER if k else ROSE, 860, 24)
        result = "written" if k == 0 else kernel
        cell(d, x, 320, 860, 70, "the kernel: " + result, ROSE if k == 0 else GREEN, 26)
        alert_box(d, x, 420, 860, section("FOUR", "THE ALERTS", k))
    why = kv(section("FOUR", "WHY"))
    fit_mono(d, 60, 700, "allowed  " + why["allowed"], MUTED, 26, 1800)
    fit_mono(d, 60, 750, "path     " + why["path"] + "  " + why["starts"], TEXT, 26, 1800)
    label(d, 60, 820, "A refused open names only the folder, so the refused alert shows /usr/bin/.", MUTED, 26)
    caption(d, "Prevent with a read-only root; detect the attempt all the same.", AMBER, 950)


def connect_alert(img, d, cfg):
    """Act FIVE: a connection to an address off the baseline, read from its record."""
    header(d, 60, 170, "THE ACTION", CYAN)
    fit_mono(d, 60, 240, section("FIVE", "THE ACTION")[0], TEXT, 28, 1000)
    how = section("FIVE", "HOW THE RECORD IS READ")
    header(d, 60, 340, "HOW THE RECORD IS READ", AMBER)
    for i, ln in enumerate(how):
        fit_mono(d, 60, 410 + i * 56, ln, GREEN if ln.startswith("port") else TEXT, 26, 1040)
    header(d, 1180, 170, "THE ALERT (the result)", ROSE)
    alert_box(d, 1180, 240, 680, section("FIVE", "THE ALERTS"))
    caption(d, "The ledger's address is on the baseline; the outside server's is not.", AMBER, 950)
