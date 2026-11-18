"""Pictures for the TLS attacks and downgrades video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
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
    """The lines of the first section of an act whose header starts with `head` (two spaces removed)."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = [] if ln.startswith(head) and not found else None
            if cur is not None:
                found.append(cur)
        elif cur is not None:
            cur.append(ln[2:])
    return found[0]


def split(ln, n=1):
    """A line split at runs of two or more spaces."""
    return re.split(r"\s{2,}", ln.strip(), maxsplit=n)


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


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def attempts(name, head):
    """The (offered, result) attempts of one client, and its result line."""
    lines = section(name, head)
    rows = [split(ln, 2)[1:] for ln in lines if ln.startswith("attempt")]
    result = next(split(ln)[1] for ln in lines if ln.startswith("result"))
    return rows, result


def colour_of(result):
    return GREEN if result.startswith("refused") else ROSE if result.startswith("connected") else MUTED


# ------------------------------------------------------------------ figures

def downgrade_ladder(img, d, cfg):
    """One client that retries with lower versions, step by step, until the network lets TLS 1.0 through."""
    rows, result = attempts("THREE", "A CLIENT THAT FALLS BACK")
    rule = section("THREE", "THE NETWORK")[0]
    header(d, 60, 180, "THE CLIENT TRIES", CYAN)
    header(d, 860, 180, "THE NETWORK", AMBER)
    header(d, 1340, 180, "THE SERVER (could speak TLS 1.3)", GREEN)
    label(d, 860, 240, rule, MUTED, 22)
    d.rectangle([1040, 280, 1060, 820], fill=AMBER)
    for i, (offered, res) in enumerate(rows):
        y = 300 + i * 130
        cell(d, 60, y, 330, 70, "attempt %d" % (i + 1), LINE, 26, text_colour=TEXT)
        cell(d, 420, y, 380, 70, offered, CYAN, 30)
        ok = res.startswith("connected")
        end = 1500 if ok else 1030
        draw.arrow_right(d, 810, y + 35, end, GREEN if ok else ROSE, 5)
        if ok:
            cell(d, 1520, y, 340, 70, res.split(", ")[1], ROSE, 30)
            label(d, 1690, y + 85, "an old CBC suite, no forward secrecy", MUTED, 22, False, "ma")
        else:
            label(d, 1090, y + 35, res, ROSE, 28, True, "lm")
    fit_mono(d, W // 2, 875, "result: " + result, ROSE, 32, 1800, "ma")
    caption(d, "Every failure is answered with an older version, and the network chooses which.", AMBER, 945)


def accept_grid(img, d, cfg):
    """What each of the three servers accepts: four versions and eight suites."""
    lines = section("ONE", "WHAT EACH SERVER ACCEPTS")
    cols = ["weak", "strong", "1.3 only"]
    meaning = dict(split(ln) for ln in section("ONE", "WHAT THE SUITE NAMES SAY"))
    header(d, 60, 175, "ASKED FOR", CYAN)
    header(d, 1040, 175, "THE ANSWER: DID IT CONNECT?", GREEN, 820)
    for j, c in enumerate(cols):
        label(d, 1120 + j * 250, 245, c, TEXT, 28, True, "ma")
    for i, ln in enumerate(lines):
        what, rest = ln.split(None, 1)
        answers = rest.split()
        y = 285 + i * 52
        fit_mono(d, 60, y + 22, what, TEXT, 26, 560, "lm")
        label(d, 640, y + 22, meaning.get(what, "protocol version"), MUTED, 22, False, "lm")
        m = meaning.get(what, "")
        weak_row = what in ("TLSv1.0", "TLSv1.1") or any(k in m for k in ("static RSA", "CBC", "finite-field", "no encryption"))
        for j, a in enumerate(answers):
            colour = LINE if a != "yes" else ROSE if weak_row else GREEN
            cell(d, 1045 + j * 250, y, 150, 44, a, colour, 24)
    caption(d, "Red: an old or weak setting was accepted. The two controls accept only modern rows.", AMBER, 935)


def scorecard(img, d, cfg):
    """Each planted weakness, and how our probes and testssl.sh found it."""
    ours = [split(ln) for ln in section("TWO", "OUR PROBES")]
    theirs = dict(split(ln) for ln in section("TWO", "TESTSSL.SH"))
    score = [split(ln) for ln in section("TWO", "THE SCORE OF TESTSSL")]
    header(d, 60, 175, "PLANTED", ROSE, 300)
    header(d, 400, 175, "OUR PROBES (s_client)", CYAN, 640)
    header(d, 1080, 175, "TESTSSL.SH REPORT", AMBER, 780)
    for i, (name, how) in enumerate(ours):
        y = 245 + i * 90
        cell(d, 60, y, 300, 70, name, ROSE, 28)
        cell(d, 400, y, 640, 70, how, GREEN, 24)
        t = theirs[name]
        cell(d, 1080, y, 780, 70, t, AMBER if t.startswith("only") else GREEN, 24)
    fit_mono(d, 60, 800, "testssl.sh: " + score[0][1] + "; " + score[1][0], TEXT, 28, 1800)
    caption(d, "Two are found only by reading the key exchange column of the cipher table.", AMBER, 900)


def three_clients(img, d, cfg):
    """The DOWNGRD marker, then three clients through the same network: no signal, the SCSV, a minimum version."""
    marker = section("THREE", "THE SERVER HELLO RANDOM")
    tail = re.search(r"last 8 bytes (.*)", marker[0]).group(1)
    header(d, 60, 170, "THE SERVER HELLO, ASKED FOR TLS 1.2", CYAN)
    for k, b in enumerate(tail.split()):
        ch = chr(int(b, 16)) if 32 < int(b, 16) < 127 else b
        cell(d, 760 + k * 110, 165, 96, 54, b, AMBER if k < 7 else ROSE, 24)
        label(d, 808 + k * 110, 232, ch, TEXT, 24, True, "ma")
    label(d, 60, 240, marker[1].strip(), MUTED, 24)
    clients = [("NO SIGNAL", "A CLIENT THAT FALLS BACK"), ("SENDS THE SCSV", "THE SAME CLIENT"), ("MINIMUM TLS 1.2", "A CLIENT WITH A MINIMUM")]
    for c, (title, head) in enumerate(clients):
        rows, result = attempts("THREE", head)
        x = 60 + c * 610
        bad = result.startswith("connected yes")
        header(d, x, 300, title, ROSE if bad else GREEN, 580)
        for i, (offered, res) in enumerate(rows):
            y = 370 + i * 105
            cell(d, x, y, 580, 44, offered, CYAN, 24)
            fit_mono(d, x + 290, y + 66, res, colour_of(res), 22, 570, "mm")
        fit_mono(d, x + 290, 815, result.split(",")[0], ROSE if bad else GREEN, 28, 570, "mm")
    caption(d, "The signal turns the last retry into a refusal; a minimum never offers TLS 1.0.", AMBER, 900)


def leaked_key(img, d, cfg):
    """What each key exchange message carried, and how the leaked key opens only the static RSA session."""
    sent = [split(ln) for ln in section("FOUR", "WHAT THE CLIENT SENT")]
    steps = section("FOUR", "HOW THE LEAKED KEY")
    result = [split(ln) for ln in section("FOUR", "THE RESULT")]
    request = section("FOUR", "THE RECORDING")[1].strip()
    header(d, 60, 170, "IN THE RECORDING: THE KEY EXCHANGE MESSAGE", CYAN)
    for i, (kx, what) in enumerate(sent):
        y = 240 + i * 120
        opened = next(r[1] for r in result if r[0] == kx)
        colour = ROSE if opened == "opened" else GREEN
        cell(d, 60, y, 250, 80, kx, colour, 30)
        size, text = what.split(": ", 1)
        label(d, 335, y + 8, size, TEXT, 28, True)
        label(d, 335, y + 46, text, MUTED, 24)
        label(d, 800, y + 8, opened, colour, 26, True, "ra")
    header(d, 860, 170, "THE LEAKED SERVER KEY, STEP BY STEP (static RSA)", ROSE)
    for i, st in enumerate(steps):
        y = 240 + i * 82
        num, rest = st.split(" ", 1)
        d.ellipse([860, y, 912, y + 52], fill=ROSE)
        label(d, 886, y + 26, num, INK, 26, True, "mm")
        fit_mono(d, 930, y + 26, rest, TEXT, 23, 960, "lm")
    fit_mono(d, 60, 770, "opened: " + request, ROSE, 30, 1800)
    caption(d, "Static RSA sends the secret itself; DHE and ECDHE send public values only.", AMBER, 900)


def tls13_removed(img, d, cfg):
    """What a client asked of the TLS 1.3 only server, and the answer."""
    asked = [split(ln) for ln in section("FIVE", "WHAT A CLIENT ASKED")]
    suites = [split(ln) for ln in section("FIVE", "THE TLS 1.3 SUITES")]
    header(d, 60, 170, "A CLIENT ASKED FOR", CYAN, 560)
    header(d, 660, 170, "THE TLS 1.3 ONLY SERVER ANSWERED", GREEN, 1200)
    for i, (what, answer) in enumerate(asked):
        y = 240 + i * 80
        cell(d, 60, y, 560, 62, what, CYAN, 28)
        fit_mono(d, 680, y + 31, answer, GREEN, 28, 1180, "lm")
    header(d, 60, 735, "THE ONLY SUITES TLS 1.3 HAS", AMBER)
    for i, s in enumerate(x for x in suites if x[0].startswith("TLS_")):
        cell(d, 60 + i * 610, 795, 590, 56, s[0], AMBER, 24)
    caption(d, "No old versions, no fixed key exchange, no CBC, no compression, no renegotiation.", AMBER, 945)
