"""Pictures for the local certificate authority with step-ca video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from datetime import datetime
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def sections(name, head):
    """Every section of an act whose header starts with `head`, as lists of stripped lines."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln.strip())
    return found


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def kv(lines):
    """'subject      Shop Issuing CA' -> {'subject': 'Shop Issuing CA'}."""
    out = {}
    for ln in lines:
        m = re.match(r"(.+?)\s{2,}(.+)$", ln)
        if m:
            out[m.group(1)] = m.group(2)
    return out


def step(ln):
    """'4. x is inside y: true' -> ('4. x is inside y', True)."""
    text, _, value = ln.rpartition(": ")
    return text, value == "true"


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


def fit_label(d, x, y, text, colour, size, max_w, bold=False, anchor="la"):
    f = font(SANS_B if bold else SANS, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 30, True, "ma")


def cell(d, x, y, w, h, text, colour, size=24, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def mark(d, x, y, ok):
    """A small true/false pill."""
    cell(d, x, y, 96, 40, "true" if ok else "false", GREEN if ok else ROSE, 22)


def when(text):
    return datetime.strptime(text, "%Y-%m-%d %H:%M")


# ------------------------------------------------------------------ figures

def result_colour(text):
    return ROSE if "refused" in text or "false" in text else GREEN


def token_anatomy(img, d, cfg):
    """The one-time token opened up: header, payload claims and signature, with the values the demo read from a real token."""
    tok = kv(sections("TWO", "THE TOKEN")[0])
    header(d, 60, 210, "THE TOKEN: " + heading("TWO", "THE TOKEN").split("(")[1].rstrip("):"), AMBER)
    fit_mono(d, 60, 275, tok["parts"], MUTED, 26, 1800)
    cols = [("HEADER", CYAN, [("alg / typ", tok["header"].replace("alg ", "").replace(", typ ", " / "))]),
            ("PAYLOAD (the claims)", AMBER, [(k, tok[k]) for k in ("sub", "sans", "iss", "aud", "exp - nbf", "sha", "jti")]),
            ("SIGNATURE", GREEN, [("signed by", "the provisioner " + tok["iss"].split()[0]), ("checked by", "the CA, before it signs")])]
    xs = [(60, 420), (510, 900), (1440, 420)]
    for (title, c, rows), (x, w) in zip(cols, xs):
        header(d, x, 330, title, c, w)
        panel(d, x, 385, w, 470, c, 3)
        for j, (k, v) in enumerate(rows):
            y = 405 + j * 64
            label(d, x + 20, y, k, MUTED, 24, True)
            fit_mono(d, x + 20, y + 30, v, TEXT, 24, w - 40)
    caption(d, "The claims say for which names, which CA, how long; the jti makes it one-time.", AMBER, 900)


def ca_setup(img, d, cfg):
    """The root and the issuing authority step ca init made, the name policy, and two clients asking for /health."""
    root = kv(sections("ONE", "THE ROOT")[0])
    issuing = kv(sections("ONE", "THE ISSUING AUTHORITY")[0])
    policy = kv(sections("ONE", "THE CA'S POLICY")[0])
    answers = sections("ONE", "THE RESPONSES")[0]
    cards = [("THE ROOT", CYAN, root["subject"], [("self-signed", root["self-signed"]), ("valid", root["valid"]), ("key file", root["key file"])]),
             ("THE ISSUING AUTHORITY", AMBER, issuing["subject"], [("signed by", issuing["signed by"]), ("path length", issuing["path length"]),
                                                                  ("valid", issuing["valid"])])]
    for i, (title, c, name, rows) in enumerate(cards):
        x = 60 + i * 640
        header(d, x, 210, title, c, 580)
        panel(d, x, 265, 580, 360, c, 3)
        fit_label(d, x + 290, 290, name, TEXT, 30, 540, True, "ma")
        for j, (k, v) in enumerate(rows):
            label(d, x + 25, 360 + j * 85, k, MUTED, 24, True)
            fit_mono(d, x + 25, 392 + j * 85, v, TEXT, 24, 530)
    draw.arrow_right(d, 645, 445, 695, MUTED, 4)
    header(d, 1340, 210, "THE CA'S POLICY", GREEN, 520)
    panel(d, 1340, 265, 520, 360, GREEN, 3)
    label(d, 1365, 290, "allowed names", MUTED, 24, True)
    for j, n in enumerate(policy["allowed names"].split(", ")):
        fit_mono(d, 1365, 335 + j * 50, n, GREEN, 30, 470)
    header(d, 60, 670, heading("ONE", "THE REQUEST").replace("THE REQUEST: ", "THE REQUEST: "), AMBER)
    for j, ln in enumerate(answers):
        who, res = ln.split(": ", 1)
        y = 740 + j * 60
        fit_label(d, 60, y, who, TEXT, 26, 700)
        fit_mono(d, 780, y + 2, res, result_colour(res) if res != "ok" else GREEN, 26, 1080)
    caption(d, "A client trusts the CA only if it was given the root.", AMBER, 900)


def token_flow(img, d, cfg):
    """A token buys one certificate for its own name; four ways of misusing it, and the CA's answer to each."""
    cert = sections("TWO", "THE CERTIFICATE")[0]
    refused = sections("TWO", "THE TOKENS THE CA REFUSES")[0]
    header(d, 60, 210, "BEFORE: THE TOKEN", AMBER)
    cell(d, 60, 270, 520, 80, "token for " + kv(sections("TWO", "THE TOKEN")[0])["sub"], AMBER, 26)
    draw.arrow_right(d, 590, 310, 680, MUTED, 4)
    header(d, 690, 210, "AFTER: THE CERTIFICATE", GREEN)
    panel(d, 690, 265, 1170, 180, GREEN, 3)
    for j, ln in enumerate(cert):
        fit_mono(d, 715, 285 + j * 50, ln, GREEN if j != 1 else TEXT, 26, 1120)
    header(d, 60, 480, "THE TOKENS THE CA REFUSES", ROSE)
    for i in range(0, len(refused), 2):
        y = 545 + (i // 2) * 80
        fit_label(d, 60, y, refused[i], TEXT, 26, 760)
        fit_mono(d, 840, y + 2, refused[i + 1].replace("-> ", ""), ROSE, 24, 1020)
    caption(d, "One token, one use, one name, within the policy and the longest lifetime.", AMBER, 900)


def acme_flow(img, d, cfg):
    """ACME: the service asks, the CA calls the name back on port 80, then signs; a name outside the policy is refused."""
    req = sections("THREE", "THE REQUEST")[0][0]
    name, how = req.split("; ")
    cert = sections("THREE", "THE CERTIFICATE")[0]
    evil = sections("THREE", "ACME FOR A NAME")[0][0].replace("-> ", "")
    evil_name = heading("THREE", "ACME FOR A NAME").split("(")[1].rstrip("):")
    header(d, 60, 210, "THE REQUEST", AMBER)
    cell(d, 60, 270, 520, 90, "service " + name, AMBER, 26)
    cell(d, 1340, 270, 520, 90, "step-ca, provisioner acme", CYAN, 26)
    draw.arrow_right(d, 590, 300, 1330, AMBER, 4)
    label(d, 960, 260, "1. new order for " + name, AMBER, 24, True, "ma")
    draw.arrow_left(d, 1330, 345, 590, CYAN, 4)
    label(d, 960, 355, "2. " + how.replace("the CA ", "challenge: the CA "), CYAN, 24, True, "ma")
    header(d, 60, 450, "THE CERTIFICATE (3. signed after the challenge)", GREEN)
    panel(d, 60, 505, 1800, 170, GREEN, 3)
    for j, ln in enumerate(cert):
        fit_mono(d, 85, 520 + j * 50, ln, GREEN if j != 1 else TEXT, 26, 1750)
    header(d, 60, 710, "ACME FOR " + evil_name, ROSE)
    fit_mono(d, 60, 775, evil, ROSE, 26, 1800)
    caption(d, "ACME proves control of the name now; the policy decides which names at all.", AMBER, 900)


def renewal(img, d, cfg):
    """Before and after step ca renew and rekey: what changes, what stays; and a service that lost its key."""
    before = sections("FOUR", "BEFORE THE RENEWAL")[0]
    renew = sections("FOUR", "AFTER step ca renew")[0]
    rekey = sections("FOUR", "AFTER step ca rekey")[0]
    lost = sections("FOUR", "A SERVICE THAT LOST")[0]
    cols = [("BEFORE", CYAN, before[0].split(", ")),
            ("AFTER RENEW", GREEN, [renew[0]] + renew[1].split("; ")),
            ("AFTER REKEY", GREEN, rekey[0].split("; "))]
    for i, (title, c, rows) in enumerate(cols):
        x = 60 + i * 610
        header(d, x, 210, title, c, 570)
        panel(d, x, 265, 570, 330, c, 3)
        for j, r in enumerate(rows):
            fit_mono(d, x + 25, 290 + j * 70, r, result_colour(r) if ": " in r else TEXT, 26, 520)
        if i < 2:
            draw.arrow_right(d, x + 575, 430, x + 605, MUTED, 4)
    header(d, 60, 640, heading("FOUR", "A SERVICE THAT LOST").rstrip(":"), ROSE)
    for j, ln in enumerate(lost):
        fit_mono(d, 60, 705 + j * 50, ln.replace("-> ", ""), ROSE if j == 0 else AMBER, 26, 1800)
    caption(d, "Renewal is authenticated by the certificate and its key, so only the holder renews.", AMBER, 900)


def revocation_checks(img, d, cfg):
    """The CA's signed revocation list, and what four clients decide with it."""
    rev = sections("FIVE", "THE REVOCATION (")[0]
    crl = kv(sections("FIVE", "THE REVOCATION LIST")[0])
    checks = sections("FIVE", "THE CHECKS")[0]
    header(d, 60, 210, "THE REVOCATION", AMBER, 560)
    panel(d, 60, 265, 560, 230, AMBER, 3)
    fit_mono(d, 85, 290, rev[0], GREEN, 26, 510)
    fit_label(d, 85, 350, rev[1], TEXT, 26, 510)
    fit_mono(d, 85, 395, rev[2].replace("-> ", ""), ROSE, 24, 510)
    header(d, 60, 530, "THE REVOCATION LIST", CYAN, 560)
    panel(d, 60, 585, 560, 270, CYAN, 3)
    for j, (k, v) in enumerate(crl.items()):
        label(d, 85, 600 + j * 85, k, MUTED, 24, True)
        fit_mono(d, 85, 632 + j * 85, v, TEXT, 24, 510)
    header(d, 680, 210, "THE CHECKS", AMBER)
    rows, i = [], 0
    while i < len(checks):
        who = checks[i]
        res = checks[i + 1].replace("-> ", "")
        note = checks[i + 2].replace("<- ", "") if i + 2 < len(checks) and checks[i + 2].startswith("<-") else ""
        rows.append((who, res, note))
        i += 3 if note else 2
    for j, (who, res, note) in enumerate(rows):
        y = 270 + j * 150
        if "  <- " in res:
            res, note = res.split("  <- ")
        ok = res == "accepted"
        bad = ok and note
        c = ROSE if bad else (GREEN if ok or "REVOKED" in res else AMBER)
        panel(d, 680, y, 1180, 130, c, 3)
        fit_label(d, 705, y + 18, who, TEXT, 26, 1130)
        fit_mono(d, 705, y + 62, res + ("   " + note.lower() if note else ""), c, 24, 1130)
    caption(d, "The list helps only the clients that read it; one that wants it and has none fails closed.", AMBER, 900)
