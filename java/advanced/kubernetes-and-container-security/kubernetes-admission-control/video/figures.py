"""Pictures for the Kubernetes admission control video, drawn by the `figure` slide kind.

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


def section(name, head):
    """The first section of an act whose header starts with `head`, as lines (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:].strip())
    return found[0]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


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


def tone(answer):
    return GREEN if answer.startswith("admitted") or answer in ("pass", "no objection", "yes") else ROSE


def split_answer(line):
    """'request ... answer' where the answer starts with admitted or refused."""
    m = re.match(r"(.*?)\s{2,}((?:admitted|refused).*)$", line)
    return m.group(1), m.group(2)


# ------------------------------------------------------------------ figures

def one_request(img, d, cfg):
    """One request through every policy: which ones fail, and the answer."""
    head = heading("ONE", "HOW ONE REQUEST")
    req = re.search(r"\((.*)\)", head).group(1)
    rows = section("ONE", "HOW ONE REQUEST")
    policies = {cols(ln)[0]: ln for ln in section("ONE", "THE POLICIES")}
    header(d, 60, 175, "THE REQUEST", CYAN)
    fit_mono(d, 60, 245, req, ROSE, 34, 560)
    label(d, 60, 300, "kubectl apply, to the", MUTED, 26)
    label(d, 60, 336, "payments namespace", MUTED, 26)
    draw.arrow_right(d, 60, 420, 560, MUTED, 4)
    label(d, 60, 440, "the API server asks the", MUTED, 26)
    label(d, 60, 476, "admission controller", MUTED, 26)
    header(d, 640, 175, "EVERY POLICY LOOKS AT IT", AMBER)
    for i, ln in enumerate(r for r in rows if not r.startswith("answer")):
        name, result = cols(ln)
        y = 245 + i * 92
        rule = cols(policies[name])[-1]
        d.rounded_rectangle([640, y, 1500, y + 78], radius=10, fill=BG_CELL, outline=tone(result), width=3)
        fit_mono(d, 660, y + 8, name, TEXT, 26, 500)
        fit_mono(d, 660, y + 44, "asks for: " + rule, MUTED, 20, 600)
        fit_mono(d, 1480, y + 39, result, tone(result), 26, 300, "rm")
    answer = next(r for r in rows if r.startswith("answer"))
    header(d, 1560, 175, "THE ANSWER", ROSE)
    word = answer.split()[1]
    label(d, 1700, 330, word, tone(word), 52, True, "ma")
    note = re.search(r"\((.*)\)", answer).group(1)
    label(d, 1700, 420, note.split(" when ")[0], MUTED, 24, False, "ma")
    label(d, 1700, 455, "when " + note.split(" when ")[1], MUTED, 24, False, "ma")
    caption(d, "Every policy is asked; one failure is enough to refuse.", AMBER, 900)


def requests_table(img, d, cfg):
    """Ten requests and their answers, and how no-latest-tag reads an image name."""
    rows = [split_answer(ln) for ln in section("ONE", "THE REQUESTS")]
    header(d, 60, 175, "THE REQUESTS AND THE ANSWERS", CYAN)
    for i, (req, ans) in enumerate(rows):
        y = 240 + i * 62
        fit_mono(d, 60, y + 26, req, TEXT, 24, 560, "lm")
        cell(d, 640, y, 440, 52, ans.replace("refused: ", ""), tone(ans), 22)
    lines = section("ONE", "HOW no-latest-tag")
    head, tags = lines[0].split(), [cols(ln) for ln in lines[1:]]
    header(d, 1160, 175, "HOW no-latest-tag READS A NAME", AMBER)
    for j, h in enumerate(head[1:]):
        label(d, 1530 + j * 105, 245, h, MUTED, 20, True, "ma")
    for i, (image, *marks) in enumerate(tags):
        y = 285 + i * 70
        fit_mono(d, 1160, y + 22, image, TEXT, 20, 320, "lm")
        for j, m in enumerate(marks):
            colour = tone(m) if j == 3 else MUTED
            label(d, 1530 + j * 105, y + 22, m, colour, 22, True, "mm")
    label(d, 1160, 650, "a ':' before the last '/' is a port, not a tag", MUTED, 22)
    label(d, 1160, 690, "green: admitted;  red: refused, the policy is named", MUTED, 22)
    caption(d, "Six refused, each by name; the unlabelled namespace is not checked at all.", AMBER, 900)


def audit_timeline(img, d, cfg):
    """What ran before the policies, a policy in Audit, its report, and the switch to Deny."""
    steps = [
        ("BEFORE THE POLICIES", section("TWO", "BEFORE THE POLICIES"), MUTED),
        ("AFTER THE POLICIES", section("TWO", "AFTER THE POLICIES"), AMBER),
        (heading("TWO", "A NEW POLICY IN AUDIT").split(":")[0], section("TWO", "A NEW POLICY IN AUDIT"), AMBER),
        (heading("TWO", "AFTER THE SWITCH"), section("TWO", "AFTER THE SWITCH"), ROSE),
    ]
    for i, (title, lines, colour) in enumerate(steps):
        x = 60 + i * 460
        header(d, x, 175, title, colour, 22)
        for k, ln in enumerate(lines):
            words, cur, out = ln.split(), "", []
            for w in words:
                if len(cur) + len(w) > 26:
                    out.append(cur)
                    cur = w
                else:
                    cur = (cur + " " + w).strip()
            out.append(cur)
            for m, part in enumerate(out):
                fit_mono(d, x, 250 + (k * 3 + m) * 40, part, TEXT if "refused" not in part else ROSE, 24, 430)
        if i < 3:
            draw.arrow_right(d, x + 400, 470, x + 450, MUTED, 4)
    report = [ln.split() for ln in section("TWO", "THE POLICY REPORT")]
    header(d, 60, 560, "THE POLICY REPORT (it lists, it does not refuse)", CYAN)
    for i, (policy, pod, result) in enumerate(report):
        y = 630 + i * 72
        fit_mono(d, 60, y + 28, policy, TEXT, 28, 520, "lm")
        fit_mono(d, 620, y + 28, pod, TEXT, 28, 360, "lm")
        cell(d, 1000, y, 200, 56, result, ROSE if result == "fail" else GREEN, 28)
    caption(d, "Admission sees only new requests: audit and the report show what is already there.", AMBER, 900)


def mutation(img, d, cfg):
    """The request before and after the mutation: the digest and allowPrivilegeEscalation."""
    before = section("THREE", "BEFORE THE MUTATION")
    after = section("THREE", "AFTER THE MUTATION")
    header(d, 60, 175, "BEFORE THE MUTATION (what the team sent)", CYAN)
    for i, ln in enumerate(before):
        fit_mono(d, 60, 245 + i * 48, ln, TEXT, 28, 1800)
    header(d, 60, 420, "AFTER THE MUTATION (what the API server would store)", GREEN)
    for i, ln in enumerate(after):
        changed = "@" in ln or "allowPrivilegeEscalation" in ln
        fit_mono(d, 60, 490 + i * 48, ln, GREEN if changed else TEXT, 28, 1800)
    priv = section("THREE", "A PRIVILEGED POD")[0]
    header(d, 60, 730, "A PRIVILEGED POD (mutation first, then validation)", ROSE)
    fit_mono(d, 60, 795, priv, ROSE, 30, 1800)
    caption(d, "The tag becomes the digest that was signed; a missing setting gets a safe default.", AMBER, 900)


def exceptions(img, d, cfg):
    """One exception, four requests: only the right pod, from the right namespace, before its date."""
    exc = [cols(ln) for ln in section("FOUR", "THE EXCEPTION")]
    header(d, 60, 175, "THE EXCEPTION", AMBER)
    for i, (k, v) in enumerate(exc):
        label(d, 60, 250 + i * 56, k, MUTED, 26, True)
        fit_mono(d, 280, 250 + i * 56, v, TEXT, 26, 700)
    rows = [split_answer(ln) for ln in section("FOUR", "THE REQUESTS")]
    header(d, 1060, 175, "THE REQUESTS AND THE ANSWERS", CYAN)
    for i, (req, ans) in enumerate(rows):
        y = 245 + i * 120
        fit_mono(d, 1060, y, req, TEXT, 26, 800)
        cell(d, 1060, y + 40, 600, 56, ans, tone(ans), 24)
    label(d, 60, 500, "Only one of four requests gets through:", MUTED, 28)
    label(d, 60, 545, "the named pod, an exception the team", MUTED, 28)
    label(d, 60, 590, "cannot write, and a date not yet passed.", MUTED, 28)
    caption(d, "An exception is one pod, one policy, one namespace, one end date.", AMBER, 900)


def controller_down(img, d, cfg):
    """The same requests while Kyverno runs, while it is stopped, and after it is back."""
    parts = [("BEFORE", GREEN), ("KYVERNO STOPPED", ROSE), ("AFTER IT IS BACK", GREEN)]
    for i, (head, colour) in enumerate(parts):
        x = 60 + i * 620
        header(d, x, 175, heading("FIVE", head), colour, 22)
        for k, ln in enumerate(section("FIVE", head)):
            q, a = ln.split(": ", 1)
            y = 260 + k * 130
            fit_mono(d, x, y, q, TEXT, 24, 560)
            cell(d, x, y + 40, 560, 60, a, tone(a) if not a.isdigit() else ROSE, 26)
        if i < 2:
            draw.arrow_right(d, x + 570, 197, x + 610, MUTED, 4)
    caption(d, "With no webhook left, the API server asks no one: the privileged pod gets in.", AMBER, 900)
