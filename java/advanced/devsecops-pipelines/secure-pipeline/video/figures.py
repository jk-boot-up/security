"""Pictures for the secure pipeline video, drawn by the `figure` slide kind.

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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


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


def commit():
    return re.search(r"commit (\w{12})", heading("ONE", "THE INPUT")).group(1)


def answers():
    """(release, checks passed, total, answer) for each release offered at deploy."""
    rows = []
    for ln in section("SIX", "THE ANSWERS"):
        m = re.match(r"(genuine|forged \d)\s+(\d+) of (\d+)\s+(.*)", ln)
        if m:
            rows.append((m.group(1), int(m.group(2)), int(m.group(3)), m.group(4)))
    return rows


def checks():
    """The verifier's checks in order, from the two-column listing."""
    head = next(ln for ln in act("SIX") if re.match(r"THE \d+ CHECKS", ln))
    found = {}
    for ln in section("SIX", head):
        for n, name in re.findall(r"(\d+) ([a-z ]+?)(?=\s{2,}\d|\s*$)", ln):
            found[int(n)] = name.strip()
    return [found[k] for k in sorted(found)]


# ------------------------------------------------------------------ figures

def release_chain(img, d, cfg):
    """The core idea: a release is four files, and deploy checks how they hang together before it trusts the jar."""
    policy = section("SIX", "THE POLICY")
    wanted = re.search(r"commit (\w+)", policy[2]).group(1)
    stmt = section("THREE", "THE INPUTS")[0]
    named = re.search(r"commit (\w+)", stmt).group(1)
    genuine = answers()[0]
    replay = answers()[3]
    header(d, 60, 170, "THE INPUT: ONE RELEASE, FOUR FILES", CYAN)
    files = [("bank-ledger.jar", "the code that runs", 80), ("bank-ledger.jar.sig", "signature of the jar", 520),
             ("provenance.json", "what, from what, by whom", 960), ("provenance.json.sig", "signature of the statement", 1400)]
    for name, what, x in files:
        cell(d, x, 240, 400, 70, name, TEXT, 28)
        label(d, x + 200, 325, what, MUTED, 24, False, "ma")
    header(d, 60, 400, "HOW DEPLOY TIES THEM TOGETHER", AMBER)
    links = [
        ("jar + jar.sig + cosign.pub", "the jar's signature verifies with the pipeline's public key"),
        ("jar + provenance.json", "the SHA-256 of the jar = the subject named in the statement"),
        ("provenance.json + policy", "it names commit %s; deploy wants %s" % (named, wanted)),
    ]
    for i, (files_, text) in enumerate(links):
        y = 470 + i * 72
        cell(d, 60, y, 520, 56, files_, AMBER, 24)
        draw.arrow_right(d, 590, y + 28, 650, AMBER, 3)
        label(d, 670, y + 12, text, TEXT, 28, True)
    header(d, 60, 700, "THE RESULT", GREEN)
    panel(d, 60, 760, 880, 130, GREEN)
    label(d, 500, 780, "the release the pipeline made", MUTED, 26, True, "ma")
    label(d, 500, 825, "%d of %d checks: %s" % (genuine[1], genuine[2], genuine[3]), GREEN, 34, True, "ma")
    panel(d, 980, 760, 880, 130, ROSE)
    label(d, 1420, 780, "the genuine previous release, replayed", MUTED, 26, True, "ma")
    label(d, 1420, 825, "%d of %d checks: %s" % (replay[1], replay[2], replay[3]), ROSE, 30, True, "ma")
    caption(d, "Trust the jar only when every file agrees with every other.", AMBER, 925)


def build_twice(img, d, cfg):
    """One commit, two checkouts, three settings: which pairs of jars are identical, and why the others differ."""
    settings = section("ONE", "THE INPUT")[2:]
    result = section("ONE", "THE RESULT")[1:]
    times = section("ONE", "THE FILE TIMES")
    path = section("ONE", "THE BUILD PATH")
    header(d, 60, 170, "THE INPUT: COMMIT %s, TWO CLEAN CHECKOUTS" % commit(), CYAN)
    for i, name in enumerate(("checkout 1", "checkout 2")):
        cell(d, 80, 240 + i * 90, 260, 64, name, TEXT, 26)
        draw.arrow_right(d, 344, 272 + i * 90, 470, MUTED, 3)
        label(d, 405, 230 + i * 90, "gradlew jar", MUTED, 20, False, "ma")
        cell(d, 476, 240 + i * 90, 220, 64, "jar %d" % (i + 1), TEXT, 26)
    for i, ln in enumerate(settings):
        name, rest = cols(ln)[0], "  ".join(cols(ln)[1:])
        label(d, 780, 245 + i * 52, name, AMBER, 26, True)
        fit_mono(d, 960, 247 + i * 52, rest, MUTED, 22, 900)
    header(d, 60, 430, "THE RESULT: JAR 1 AGAINST JAR 2", AMBER)
    heads = ["setting", "same SHA-256", "entries", "other bytes", "file times"]
    xs = [80, 420, 760, 1060, 1360]
    for x, h in zip(xs, heads):
        label(d, x, 495, h, MUTED, 24, True)
    for r, ln in enumerate(result):
        c = cols(ln)
        y = 535 + r * 76
        label(d, xs[0], y + 14, c[0], TEXT, 28, True)
        for k in range(1, 5):
            good = (k == 1 and c[1] == "yes") or (k == 3 and c[3] == "0") or (k == 4 and c[4] == "same") or k == 2
            cell(d, xs[k], y, 260, 58, c[k], GREEN if good else ROSE, 26)
    label(d, 80, 780, times[0], GREEN, 26, True)
    label(d, 80, 820, times[1], ROSE, 26, True)
    fit_mono(d, 1060, 780, path[0], AMBER, 22, 780)
    fit_mono(d, 1060, 815, path[1], ROSE, 22, 780)
    fit_mono(d, 1060, 850, path[2], ROSE, 22, 780)
    caption(d, "Same bytes only when nothing about the time or the machine gets in.", AMBER, 925)


def sign_and_check(img, d, cfg):
    """How cosign makes the signature, and the four checks with cosign and with the JDK alone."""
    inp = section("TWO", "THE INPUT")
    steps = section("TWO", "HOW THE SIGNATURE")
    rows = section("TWO", "THE CHECK")[1:]
    header(d, 60, 170, "THE INPUT", CYAN)
    label(d, 60, 235, inp[0], TEXT, 26, True)
    label(d, 60, 275, inp[3], MUTED, 24, True)
    header(d, 60, 330, "HOW THE SIGNATURE IS MADE", AMBER)
    boxes = [("jar bytes", "bank-ledger.jar"), ("SHA-256", "one digest"), ("ECDSA", "with cosign.key"), (".sig", "base64")]
    for i, (big, small) in enumerate(boxes):
        x = 60 + i * 225
        cell(d, x, 400, 190, 70, big, AMBER if i == 2 else TEXT, 28)
        label(d, x + 95, 482, small, MUTED, 20, False, "ma")
        if i < 3:
            draw.arrow_right(d, x + 192, 435, x + 223, MUTED, 3)
    for i, s in enumerate(steps):
        fit_mono(d, 60, 560 + i * 44, s, MUTED, 22, 880)
    header(d, 1000, 170, "THE RESULT: DOES IT VERIFY?", GREEN)
    label(d, 1560, 240, "cosign", MUTED, 24, True, "ma")
    label(d, 1760, 240, "JDK", MUTED, 24, True, "ma")
    for i, ln in enumerate(rows):
        what, cosign, jdk = cols(ln)
        y = 290 + i * 120
        label(d, 1000, y + 18, what.replace("the jar, ", ""), TEXT, 26, True)
        exp = i in (0, 3)
        for x, v in ((1480, cosign), (1680, jdk)):
            cell(d, x, y, 160, 64, v, GREEN if (v == "yes") == exp else ROSE, 30)
    caption(d, "The signature belongs to these exact bytes and this one key.", AMBER, 925)


def provenance_card(img, d, cfg):
    """The provenance statement, the inputs it records, and the checks against the jar and the commit."""
    stmt = section("THREE", "THE STATEMENT")
    inputs = section("THREE", "THE INPUTS")
    chk = section("THREE", "THE CHECKS")
    bad = section("THREE", "TWO THAT DO NOT")
    header(d, 60, 170, "THE INPUT: PROVENANCE.JSON", CYAN)
    panel(d, 60, 235, 1000, 560, CYAN)
    for i, ln in enumerate(stmt):
        k, v = cols(ln)[0], "  ".join(cols(ln)[1:])
        label(d, 90, 260 + i * 52, k, AMBER, 24, True)
        fit_mono(d, 330, 262 + i * 52, v, TEXT, 22, 710)
    label(d, 90, 540, heading("THREE", "THE INPUTS").strip(":").lower(), AMBER, 24, True)
    for i, ln in enumerate(inputs):
        k, v = cols(ln)[0], "  ".join(cols(ln)[1:])
        fit_mono(d, 90, 590 + i * 50, k + ": " + v, TEXT, 22, 950)
    header(d, 1110, 170, "THE RESULT: THE CHECKS", GREEN)
    for i, ln in enumerate(chk):
        what, v = ln.rsplit(" ", 1)
        y = 245 + i * 90
        label(d, 1110, y + 16, what.strip(), TEXT, 24, True)
        cell(d, 1720, y, 130, 58, v, GREEN if v == "yes" else ROSE, 28)
    header(d, 1110, 540, "TWO THAT DO NOT MATCH", ROSE)
    prev = bad[0]
    label(d, 1110, 610, prev.split(":")[0].replace("the previous release names", "previous release:"), TEXT, 24, True)
    cell(d, 1110, 650, 300, 54, prev.split(": ")[1], ROSE, 24)
    label(d, 1110, 730, bad[1].replace("the statement edited to name", "edited to name"), TEXT, 24, True)
    cell(d, 1110, 770, 420, 54, bad[2], ROSE, 22)
    caption(d, "Provenance ties one jar to one commit, and its signature keeps it honest.", AMBER, 925)


def gate_ladder(img, d, cfg):
    """Each change against the four gates: passed, stopped, or never reached."""
    gates = [cols(ln)[0].split(" ", 1)[1] for ln in section("FOUR", "THE GATES")]
    clean = section("FOUR", "THE CLEAN CHANGE")[0]
    stops = [cols(ln) for ln in section("FOUR", "WHERE EACH ONE") if not ln.startswith("stopped")]
    total = next(ln for ln in section("FOUR", "WHERE EACH ONE") if ln.startswith("stopped"))
    header(d, 60, 170, "THE INPUT: A CLEAN CHANGE AND SIX PLANTED FLAWS", CYAN)
    header(d, 1500, 170, "THE RESULT", AMBER)
    x0, cw = 470, 350
    for g, name in enumerate(gates):
        label(d, x0 + g * cw + cw // 2, 245, "%d %s" % (g + 1, name), MUTED, 24, True, "ma")
    rows = [("clean change", None, int(re.search(r"(\d+) of", clean).group(1)))] + [(s[0], s[1], int(s[2])) for s in stops]
    for r, (name, stopped, passed) in enumerate(rows):
        y = 290 + r * 80
        label(d, 60, y + 16, name, TEXT, 26, True)
        for g in range(len(gates)):
            x = x0 + g * cw + 10
            if g < passed:
                cell(d, x, y, cw - 20, 58, "pass", GREEN, 24)
            elif g == passed and stopped:
                cell(d, x, y, cw - 20, 58, "STOP", ROSE, 26, (60, 22, 34))
            else:
                cell(d, x, y, cw - 20, 58, "-", LINE, 24)
    caption(d, total[0].upper() + total[1:] + ".", AMBER, 885)
    caption(d, "A flaw is stopped at the first gate that owns it; nothing after it runs.", MUTED, 935)


def closed_or_open(img, d, cfg):
    """Three broken tools: what each gave back, and what each kind of pipeline did with it."""
    inp = section("FIVE", "THE INPUT")
    gave = section("FIVE", "WHAT EACH TOOL")
    res = [cols(ln) for ln in section("FIVE", "THE RESULT")[1:-1]]
    total = section("FIVE", "THE RESULT")[-1]
    header(d, 60, 170, "THE INPUT: THREE BROKEN TOOLS", CYAN)
    for i, ln in enumerate(inp):
        name, what = cols(ln)[0], cols(ln)[1]
        y = 240 + i * 60
        label(d, 80, y, name, AMBER, 26, True)
        label(d, 380, y + 2, what, MUTED, 24)
    header(d, 60, 440, "WHAT THE GATE SEES", AMBER)
    header(d, 760, 440, "FAILS CLOSED", GREEN)
    header(d, 1340, 440, "FAILS OPEN", ROSE)
    for i, ln in enumerate(gave):
        c = cols(ln)
        y = 510 + i * 110
        label(d, 80, y - 4, c[0], TEXT, 24, True)
        cell(d, 80, y + 30, 280, 56, c[1], AMBER, 24)
        cell(d, 380, y + 30, 300, 56, c[2].replace("outcome ", ""), ROSE, 24)
        closed, opened = res[i][1], res[i][2]
        cell(d, 760, y + 30, 540, 56, closed, GREEN if closed.startswith("stopped") else ROSE, 24)
        cell(d, 1340, y + 30, 500, 56, opened, ROSE if opened == "released" else GREEN, 26)
    caption(d, total[0].upper() + total[1:] + ".", AMBER, 875)
    caption(d, "A gate that cannot answer must say no.", MUTED, 930)


def deploy_checks(img, d, cfg):
    """Four releases against the verifier's checks, in order: how far each one got."""
    names = checks()
    offered = {cols(ln)[0]: "  ".join(cols(ln)[1:]) for ln in section("SIX", "THE RELEASES")}
    prod = next(ln for ln in section("SIX", "THE ANSWERS") if ln.startswith("jars"))
    header(d, 60, 170, "THE INPUT: FOUR RELEASES OFFERED TO DEPLOY", CYAN)
    header(d, 1060, 170, "THE RESULT: THE CHECKS IN ORDER", AMBER)
    x0, cw = 560, 190
    for k, name in enumerate(names):
        words = name.split()
        label(d, x0 + k * cw + cw // 2, 240, "%d %s" % (k + 1, words[0]), MUTED, 22, True, "ma")
        label(d, x0 + k * cw + cw // 2, 270, " ".join(words[1:]), MUTED, 22, True, "ma")
    for r, (rel, passed, total, ans) in enumerate(answers()):
        y = 320 + r * 135
        label(d, 60, y + 2, rel, TEXT, 28, True)
        fit_mono(d, 60, y + 44, offered.get(rel, ""), MUTED, 20, 480)
        for k in range(total):
            x = x0 + k * cw + 8
            if k < passed:
                cell(d, x, y, cw - 16, 58, "ok", GREEN, 24)
            elif k == passed:
                cell(d, x, y, cw - 16, 58, "NO", ROSE, 26, (60, 22, 34))
            else:
                cell(d, x, y, cw - 16, 58, "-", LINE, 24)
        label(d, x0, y + 70, ans, GREEN if ans == "accepted" else ROSE, 22, True)
    caption(d, prod[0].upper() + prod[1:] + ": only the genuine release got in.", AMBER, 905)
