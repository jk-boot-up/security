"""Pictures for the OpenID Connect video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


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


def sec(name, head):
    return sections(name, head)[0]


def field(lines, key):
    """The value after a left-hand key in lines such as 'iss        https://bank.example  (who says so)'."""
    for ln in lines:
        if ln.startswith(key + " "):
            return ln[len(key):].strip()
    raise KeyError(key)


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


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def rows(d, x, y, pairs, key_w, colour=TEXT, size=26, step=46, max_w=900):
    for i, (k, v) in enumerate(pairs):
        fit_mono(d, x, y + i * step, k, MUTED, size, key_w - 10)
        fit_mono(d, x + key_w, y + i * step, v, colour, size, max_w - key_w)


def result_colour(text):
    return GREEN if "accepted" in text else ROSE


# ------------------------------------------------------------------ figures

def two_tokens(img, d, cfg):
    """The core idea: one sign-in, two tokens, two different jobs."""
    resp = sec("ONE", "THE RESPONSE")
    claims = sec("ONE", "THE ID TOKEN'S CLAIMS")
    idt = resp[0].split()
    at = resp[1].split()
    who = sec("ONE", "THE RESULT")[0]
    refused = sec("ONE", "THE ACCESS TOKEN")[0]
    header(d, 80, 210, "ID TOKEN: WHO SIGNED IN", GREEN)
    panel(d, 80, 270, 860, 420, GREEN, 3)
    rows(d, 110, 300, [("typ", idt[3]), ("aud", idt[5]), ("iss", field(claims, "iss").split()[0]),
                       ("sub", field(claims, "sub").split()[0]), ("acr", field(claims, "acr, amr").split(",")[0]),
                       ("nonce", "the one kept for this browser")], 130, TEXT, 28, 60, 800)
    label(d, 110, 650, "read by the store; never sent to an API", GREEN, 24, True)
    header(d, 980, 210, "ACCESS TOKEN: WHAT IS ALLOWED", CLIENT)
    panel(d, 980, 270, 860, 420, CLIENT, 3)
    rows(d, 1010, 300, [("typ", at[3]), ("aud", at[5]), ("scope", "openid email")], 130, TEXT, 28, 60, 800)
    label(d, 1010, 650, "sent to the bank's API; says nothing of who", CLIENT, 24, True)
    fit_mono(d, 80, 740, who, GREEN, 28, 1760)
    fit_mono(d, 80, 800, "access token offered as a sign-in -> " + refused, ROSE, 26, 1760)
    caption(d, "An access token opens a door; an ID token says who is at the door.", AMBER, 900)


def signin_flow(img, d, cfg):
    """The request the store makes, and the claims in the ID token that comes back."""
    req = sec("ONE", "THE REQUEST")
    claims = sec("ONE", "THE ID TOKEN'S CLAIMS")
    header(d, 60, 210, "THE REQUEST", AMBER)
    panel(d, 60, 270, 840, 380, AMBER, 3)
    fit_mono(d, 85, 295, "GET " + req[0], TEXT, 26, 790)
    rows(d, 85, 350, [(ln.split(None, 1)[0], ln.split(None, 1)[1]) for ln in req[1:]], 200, TEXT, 24, 48, 790)
    draw.arrow_right(d, 910, 460, 990, MUTED, 4)
    header(d, 1000, 210, "THE RESPONSE: ID TOKEN CLAIMS", GREEN)
    panel(d, 1000, 270, 860, 580, GREEN, 3)
    keys = ["iss", "sub", "aud", "iat", "exp", "nonce", "acr, amr", "sid", "email"]
    pairs = []
    for k in keys:
        v = field(claims, k)
        pairs.append((k, v.split("  (")[0]))
    rows(d, 1025, 295, pairs, 160, TEXT, 23, 60, 820)
    label(d, 60, 690, "state: checked when the browser comes back", MUTED, 24)
    label(d, 60, 735, "nonce: must come back inside the ID token", MUTED, 24)
    fit_mono(d, 60, 800, sec("ONE", "THE RESULT")[0], GREEN, 24, 900)
    caption(d, "The customer is the issuer and the subject together.", AMBER, 900)


def token_anatomy(img, d, cfg):
    """How the bank makes the ID token, part by part, and the checks the store runs."""
    before = sec("TWO", "BEFORE SIGNING")
    steps = sec("TWO", "HOW THE BANK SIGNS IT")
    after = sec("TWO", "AFTER SIGNING")
    checks = sec("TWO", "HOW THE STORE CHECKS IT")
    header(d, 60, 205, "BEFORE SIGNING", AMBER)
    fit_mono(d, 60, 265, before[0], TEXT, 22, 880)
    fit_mono(d, 60, 300, before[1], TEXT, 22, 880)
    label(d, 60, 340, before[2].strip().strip("()"), MUTED, 22)
    y = 390
    parts = [("1. header part", steps[1].strip(), CYAN), ("2. claims part", steps[3].strip(), AMBER),
             ("4. signature", steps[7].strip(), ROSE)]
    for name, val, c in parts:
        panel(d, 60, y, 880, 90, c, 3)
        label(d, 80, y + 12, name, c, 24, True)
        fit_mono(d, 80, y + 50, val, TEXT, 22, 840)
        y += 105
    label(d, 60, y + 5, steps[4].strip() + ": " + steps[5].strip(), MUTED, 22)
    header(d, 60, y + 50, "AFTER SIGNING", GREEN)
    fit_mono(d, 60, y + 110, after[0] + "   " + after[1], GREEN, 22, 880)
    header(d, 1000, 205, "HOW THE STORE CHECKS IT", CYAN)
    panel(d, 1000, 265, 860, 560, CYAN, 3)
    for i, ln in enumerate(checks):
        text, ok = ln.rsplit(": ", 1)
        fit_mono(d, 1020, 290 + i * 74, text, TEXT, 23, 720)
        label(d, 1840, 290 + i * 74, "yes" if ok == "true" else "no", GREEN if ok == "true" else ROSE, 26, True, "ra")
    caption(d, "Signature first; only then are the claims believed.", AMBER, 900)


def discovery_keys(img, d, cfg):
    """One configured issuer, the metadata it leads to, the key set, and a forged document."""
    conf = sec("THREE", "THE CONFIGURATION")[0]
    req = sec("THREE", "THE REQUEST")[0]
    meta = sec("THREE", "THE RESPONSE")
    keys = sec("THREE", "THE KEYS")
    forged = sec("THREE", "FORGED METADATA")
    header(d, 60, 205, "THE CONFIGURATION", AMBER)
    cell(d, 60, 260, 560, 64, conf, AMBER, 24)
    draw.arrow(d, 340, 330, 340, 380, MUTED, 4)
    header(d, 60, 390, "THE REQUEST", CYAN)
    fit_mono(d, 60, 450, req, TEXT, 20, 860)
    header(d, 60, 510, "THE RESPONSE", GREEN)
    panel(d, 60, 565, 860, 300, GREEN, 3)
    rows(d, 80, 585, [(ln.split(None, 1)[0], ln.split(None, 1)[1]) for ln in meta[:6]], 330, TEXT, 22, 46, 830)
    header(d, 980, 205, "THE KEYS (jwks.json)", CYAN)
    panel(d, 980, 260, 880, 230, CYAN, 3)
    for i, ln in enumerate(keys[:4]):
        fit_mono(d, 1000, 280 + i * 50, ln, TEXT if i < 2 else MUTED, 22, 840)
    header(d, 980, 520, "FORGED METADATA", ROSE)
    panel(d, 980, 575, 880, 290, ROSE, 3)
    y = 595
    for ln in forged:
        if ln.startswith("  <-"):
            continue
        is_result = ln.startswith("  ->")
        fit_mono(d, 1000 + (30 if is_result else 0), y, ln.strip(), (GREEN if is_result else TEXT), 22, 820)
        y += 55 if is_result else 45
    caption(d, "The document must name the configured issuer, and keep its keys at home.", AMBER, 905)


def email_takeover(img, d, cfg):
    """Two providers, one email address: one account by email, two accounts by issuer and subject."""
    say = sec("FOUR", "THE TWO PROVIDERS SAY")
    before = sec("FOUR", "BEFORE THE FIX")
    after = sec("FOUR", "AFTER THE FIX")
    header(d, 60, 205, "THE TWO PROVIDERS SAY", CYAN)
    for i, ln in enumerate(say):
        iss, sub, email, *ver = ln.split()
        y = 265 + i * 80
        c = GREEN if ver[0] == "verified" else ROSE
        panel(d, 60, y, 1800, 64, c, 3)
        fit_mono(d, 80, y + 32, "%-24s %-10s %s" % (iss, sub, email), TEXT, 26, 1300, "lm")
        label(d, 1840, y + 32, " ".join(ver), c, 26, True, "rm")
    header(d, 60, 450, "BEFORE THE FIX: KEYED BY EMAIL", ROSE)
    panel(d, 60, 510, 880, 300, ROSE, 3)
    for i, ln in enumerate(before):
        fit_mono(d, 80, 535 + i * 62, ln.strip(), ROSE if "<-" in ln else TEXT, 22, 840)
    header(d, 980, 450, "AFTER THE FIX: ISSUER | SUBJECT", GREEN)
    panel(d, 980, 510, 880, 300, GREEN, 3)
    for i, ln in enumerate(after):
        fit_mono(d, 1000, 535 + i * 80, ln.strip(), TEXT, 22, 840)
    caption(d, "Same email, different people: the email is a contact detail, not an identity.", AMBER, 880)


def backchannel_logout(img, d, cfg):
    """Two devices, one logout token with the laptop's session id, and which session ends."""
    before = sec("FIVE", "BEFORE THE LOGOUT TOKEN")[0]
    token = sec("FIVE", "THE LOGOUT TOKEN")
    after = sec("FIVE", "AFTER THE LOGOUT TOKEN")
    sids = dict(re.findall(r"(laptop|phone) (sid-\d+)", before))
    left = re.search(r"laptop signed in: (\w+); phone: (\w+)", after[1]).groups()
    header(d, 60, 205, "BEFORE: " + before.split(";")[0].upper(), AMBER)
    for i, dev in enumerate(("laptop", "phone")):
        y = 270 + i * 120
        cell(d, 60, y, 520, 90, "%s  %s" % (dev, sids[dev]), AMBER, 28)
    header(d, 660, 205, "THE LOGOUT TOKEN (from the bank)", CYAN)
    panel(d, 660, 265, 1200, 340, CYAN, 3)
    for i, ln in enumerate(token):
        fit_mono(d, 680, 285 + i * 62, ln.strip(), TEXT, 26, 1160)
    header(d, 60, 640, "AFTER: " + after[0].upper(), GREEN)
    for i, (dev, on) in enumerate(zip(("laptop", "phone"), left)):
        c = GREEN if on == "true" else MUTED
        cell(d, 60 + i * 560, 705, 520, 80, "%s signed in: %s" % (dev, on), c, 26)
    caption(d, "The session id in the token names one session; the other device stays signed in.", AMBER, 900)
