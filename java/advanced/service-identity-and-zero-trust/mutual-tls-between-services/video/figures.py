"""Pictures for the mutual TLS video, drawn by the `figure` slide kind.

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


def issued():
    """(identity, subject, san) for each certificate step-ca issued."""
    return [tuple(cols(ln)) for ln in section("ONE", "THE INPUT")]


def answers():
    """(caller, request, status, body) for each request of act THREE."""
    rows = []
    for ln in section("THREE", "THE REQUESTS"):
        m = re.match(r"(\S+)\s+(\S+ \S+)\s+(\d+)\s+(.*)", ln)
        rows.append(m.groups())
    return rows


def after_colon(ln):
    return ln.rsplit(": ", 1)[1]


# ------------------------------------------------------------------ figures

def badge(img, d, cfg):
    """The core idea: the certificate payments shows, how the ledger turns it into an identity, and what the rules answer."""
    who = next(r for r in issued() if r[0] == "payments")
    chain = section("ONE", "THE CHAIN")
    reads = section("ONE", "HOW THE LEDGER")
    rows = [r for r in answers() if r[0] == "payments"]
    header(d, 60, 170, "THE INPUT: PAYMENTS' CERTIFICATE", CYAN)
    panel(d, 60, 240, 600, 420, CYAN)
    label(d, 90, 265, "subject", MUTED, 24, True)
    fit_mono(d, 90, 300, who[1], TEXT, 28, 540)
    label(d, 90, 370, "subject alternative name", MUTED, 24, True)
    fit_mono(d, 90, 405, who[2], AMBER, 26, 540)
    label(d, 90, 475, "issued by", MUTED, 24, True)
    fit_mono(d, 90, 510, chain[1].split(",")[0].replace("issuer ", ""), TEXT, 24, 540)
    label(d, 90, 580, "chain checked by hand: " + after_colon(next(l for l in chain if l.startswith("trusted"))), GREEN, 24, True)
    draw.arrow_right(d, 670, 450, 740, AMBER, 4)
    header(d, 750, 170, "HOW THE LEDGER READS IT", AMBER)
    steps = [reads[1].replace("the SAN ", ""), reads[2].split(": ")[0], "identity: " + after_colon(reads[2])]
    for i, s in enumerate(steps):
        cell(d, 750, 250 + i * 130, 560, 70, s, AMBER if i < 2 else GREEN, 26)
        if i < 2:
            draw.arrow(d, 1030, 322 + i * 130, 1030, 378 + i * 130, MUTED, 3)
    label(d, 1030, 640, reads[0].replace("the subject ", "").replace(": not used", "  is not used"), MUTED, 22, True, "ma")
    draw.arrow_right(d, 1320, 450, 1390, AMBER, 4)
    header(d, 1400, 170, "THE RESULT", GREEN)
    for i, (caller, req, status, body) in enumerate(rows):
        y = 260 + i * 200
        label(d, 1400, y, req, TEXT, 28, True)
        cell(d, 1400, y + 50, 160, 70, status, GREEN if status == "200" else ROSE, 32)
        fit_mono(d, 1580, y + 72, body, MUTED, 20, 320)
    caption(d, "The certificate says who is calling; the rules say what it may do.", AMBER, 900)


def chain_by_hand(img, d, cfg):
    """Every certificate payments has, step by step: the chain up to the root, then the identity."""
    who = issued()
    chain = section("ONE", "THE CHAIN")
    reads = section("ONE", "HOW THE LEDGER")
    key = section("ONE", "THE KEY")
    header(d, 60, 170, "THE INPUT: WHAT STEP-CA ISSUED", CYAN)
    for i, (ident, subj, san) in enumerate(who):
        y = 240 + i * 52
        label(d, 80, y, ident, AMBER, 24, True)
        fit_mono(d, 300, y + 2, subj, TEXT, 22, 330)
        fit_mono(d, 640, y + 2, san, MUTED, 22, 400)
    header(d, 60, 470, "THE CHAIN, CHECKED BY HAND", AMBER)
    links = [(chain[0], chain[1]), (chain[2], chain[3])]
    for i, (subj, iss) in enumerate(links):
        y = 540 + i * 110
        cell(d, 80, y, 460, 60, subj.split(" ", 1)[1], TEXT, 24)
        fit_mono(d, 560, y + 18, iss, GREEN if iss.endswith("yes") else ROSE, 22, 620)
        draw.arrow(d, 310, y + 62, 310, y + 108, MUTED, 3)
    cell(d, 80, 760, 460, 60, chain[4].replace("the root ", "").split(":")[0], GREEN, 24)
    label(d, 560, 778, "in trust.pem: " + chain[4].rsplit("(", 1)[1].rstrip(")"), GREEN, 22, True)
    header(d, 1220, 170, "THE RESULT: THE IDENTITY", GREEN)
    for i, ln in enumerate(reads):
        fit_mono(d, 1220, 250 + i * 60, ln, ROSE if "not used" in ln else (GREEN if i == 2 else TEXT), 22, 660)
    label(d, 1220, 470, "the key and the lifetime of payments", MUTED, 24, True)
    for i, ln in enumerate(key):
        fit_mono(d, 1220, 520 + i * 50, ln, TEXT, 22, 660)
    label(d, 1220, 700, chain[-1], GREEN, 30, True)
    caption(d, "Each certificate names, and is signed by, the next one up.", AMBER, 900)


def handshake_door(img, d, cfg):
    """Three calls that the handshake refuses, and how many reach the ledger's code."""
    res = section("TWO", "THE RESULT")
    calls = section("TWO", "THE CALLS")
    header(d, 60, 170, "THE INPUT: THREE CALLS", CYAN)
    texts = [calls[0][2:], calls[1][2:] + " " + calls[2].strip(), calls[3][2:] + " " + calls[4].strip()]
    for i, t in enumerate(texts):
        y = 250 + i * 170
        panel(d, 60, y, 760, 130, LINE)
        label(d, 80, y + 15, str(i + 1), AMBER, 34, True)
        words = t.split()
        half = (len(words) + 1) // 2
        fit_mono(d, 130, y + 25, " ".join(words[:half]), TEXT, 22, 670)
        fit_mono(d, 130, y + 70, " ".join(words[half:]), TEXT, 22, 670)
        draw.arrow_right(d, 830, y + 65, 960, ROSE, 4)
    d.rounded_rectangle([970, 240, 1040, 740], radius=12, fill=(60, 22, 34), outline=ROSE, width=4)
    label(d, 1005, 760, "the handshake", ROSE, 24, True, "ma")
    header(d, 1100, 170, "THE RESULT", AMBER)
    rows = [r for r in res if r[0].isdigit()]
    for i, r in enumerate(sorted(rows)):
        y = 250 + i * 170
        verdict, why = r[2:].split(" (", 1) if " (" in r else (r[2:], "")
        cell(d, 1100, y + 20, 520, 64, verdict, GREEN, 26)
        label(d, 1100, y + 100, "(" + why if why else "", MUTED, 22, False)
    reached = next(r for r in res if r.startswith("requests"))
    caption(d, reached[0].upper() + reached[1:] + ".", GREEN, 840)
    caption(d, "A refused handshake never reaches the application.", AMBER, 900)


def rules_grid(img, d, cfg):
    """Who may call what: the rules, the answers, and the network rule next to the identity rule."""
    rules = section("THREE", "THE LEDGER")
    rows = answers()
    net = section("THREE", "THE SAME TWO")
    header(d, 60, 170, "THE INPUT: THE LEDGER'S RULES", CYAN)
    for i, ln in enumerate(rules):
        ep, who = cols(ln)
        fit_mono(d, 80, 240 + i * 50, ep, TEXT, 26, 360)
        label(d, 440, 240 + i * 50, "only " + who, AMBER, 26, True)
    header(d, 60, 370, "THE RESULT: EACH CALLER, EACH CALL", AMBER)
    callers = list(dict.fromkeys(r[0] for r in rows))
    eps = [cols(ln)[0] for ln in rules]
    for k, ep in enumerate(eps):
        fit_mono(d, 520 + k * 330 + 150, 440, ep, MUTED, 22, 300, "ma")
    for i, c in enumerate(callers):
        y = 480 + i * 90
        label(d, 80, y + 16, c, TEXT, 28, True)
        for k, ep in enumerate(eps):
            hit = next((r for r in rows if r[0] == c and r[1] == ep), None)
            x = 520 + k * 330
            if hit:
                cell(d, x, y, 300, 62, hit[2], GREEN if hit[2] == "200" else ROSE, 30)
            else:
                cell(d, x, y, 300, 62, "-", LINE, 24)
    header(d, 1220, 370, "THE SAME HOST, POST /transfers", ROSE)
    for i, ln in enumerate(net):
        what, n = ln.rsplit(":", 1)
        y = 450 + i * 160
        label(d, 1220, y, what.replace("allowed ", "").strip(), TEXT, 24, True)
        got = int(n.strip().split()[0])
        for j in range(2):
            cell(d, 1220 + j * 200, y + 45, 180, 62, "in" if j < got else "out", ROSE if (j < got and i == 0 and j == 1) else (GREEN if j < got else MUTED), 28)
    caption(d, "A valid certificate with no rule gets nothing; the network lets everyone in.", AMBER, 900)


def rotation_timeline(img, d, cfg):
    """A certificate that lives 8 seconds: renewed halfway, the server reloaded, the old one refused after expiry."""
    inp = section("FOUR", "THE INPUT")
    ren = section("FOUR", "THE RENEWAL")[0]
    rel = section("FOUR", "THE SERVER RELOADS")
    res = section("FOUR", "THE RESULT")
    life = int(re.search(r"= (\d+) seconds", inp[0]).group(1))
    header(d, 60, 170, "THE INPUT: " + inp[0].replace("payments' certificate, ", "").upper(), CYAN)
    x0, x1, y = 120, 1800, 330
    sx = (x1 - x0) / (life + 3)
    d.line([x0, y, x1, y], fill=LINE, width=4)
    for t in range(0, life + 3):
        x = x0 + t * sx
        d.line([x, y - 10, x, y + 10], fill=LINE, width=2)
        label(d, x, y + 18, "%ds" % t, MUTED, 20, False, "ma")
    d.rounded_rectangle([x0, y - 70, x0 + life * sx, y - 30], radius=8, fill=(22, 60, 50), outline=GREEN, width=3)
    label(d, x0 + life * sx / 2, y - 62, "the first certificate", GREEN, 22, True, "ma")
    xh = x0 + (life / 2) * sx
    d.rounded_rectangle([xh, y - 120, x1, y - 80], radius=8, fill=(22, 50, 70), outline=CYAN, width=3)
    label(d, xh + 20, y - 112, "the renewed certificate", CYAN, 22, True)
    d.line([xh, y - 130, xh, y + 60], fill=AMBER, width=3)
    label(d, xh, y + 66, "renew and reload", AMBER, 22, True, "ma")
    xe = x0 + life * sx
    d.line([xe, y - 70, xe, y + 60], fill=ROSE, width=3)
    label(d, xe, y + 66, "expiry", ROSE, 22, True, "ma")
    header(d, 60, 440, "THE RENEWAL AND THE RELOAD", AMBER)
    for i, ln in enumerate([ren] + rel):
        fit_mono(d, 80, 505 + i * 48, ln, TEXT, 22, 860)
    header(d, 1000, 440, "THE RESULT", GREEN)
    for i, ln in enumerate(res):
        bad = "refused" in ln
        fit_mono(d, 1020, 505 + i * 48, ln.strip(), GREEN if not bad else GREEN, 22, 860)
    caption(d, "Short lives, renewed early, reloaded live: no call failed.", AMBER, 900)


def revocation_steps(img, d, cfg):
    """A stolen certificate: revoked at the CA, still accepted until the ledger reads the list."""
    inp = section("FIVE", "THE INPUT")
    ca = section("FIVE", "REVOKED AT THE CA")[0]
    res = section("FIVE", "THE RESULT AT THE LEDGER")
    header(d, 60, 170, "THE INPUT: A COPY OF PAYMENTS' CERTIFICATE AND KEY", CYAN)
    label(d, 80, 240, inp[0], MUTED, 24, False)
    stages = [("stolen, in use", after_colon(inp[1]), ROSE),
              ("revoked at the CA", "on the list: " + ca.split(": ")[1].split(",")[0], AMBER),
              ("ledger, list not read", after_colon(res[0]), ROSE),
              ("ledger, list read", res[1].split(": ", 1)[1], GREEN)]
    header(d, 60, 320, "THE RESULT, STEP BY STEP", AMBER)
    for i, (title, value, colour) in enumerate(stages):
        x = 60 + i * 465
        panel(d, x, 390, 430, 240, colour)
        label(d, x + 215, 415, title, TEXT, 26, True, "ma")
        cell(d, x + 30, 480, 370, 80, value, colour, 28)
        if i < 3:
            draw.arrow_right(d, x + 432, 510, x + 462, MUTED, 3)
    label(d, 80, 680, res[2], GREEN, 26, True)
    bill = section("FIVE", "THE BILL")
    for i, ln in enumerate(bill):
        label(d, 80, 740 + i * 44, ln, MUTED, 24, True)
    caption(d, "A revocation works only where the list is read.", AMBER, 910)
