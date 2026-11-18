"""Pictures for the OpenID Connect with Spring Security and Keycloak video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
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


def sec(name, head):
    """The lines of the first section of an act whose header starts with `head`, indentation removed."""
    out, on = [], False
    for ln in act(name):
        if not ln.startswith("  "):
            if on:
                break
            on = ln.startswith(head)
        elif on:
            out.append(ln[2:])
    return out


def field(lines, key):
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


def lines_in(d, x, y, lines, w, size=24, step=50):
    for i, ln in enumerate(lines):
        s = ln.strip()
        c = AMBER if s.startswith("<-") else GREEN if s.startswith("-> accepted") else ROSE if "refused" in s else TEXT
        fit_mono(d, x + (30 if s.startswith(("->", "<-")) else 0), y + i * step, s, c, size, w)


# ------------------------------------------------------------------ figures

def two_tokens(img, d, cfg):
    """The core idea: one sign-in at Keycloak, two tokens, two different jobs."""
    toks = sec("ONE", "THE TWO TOKENS")
    claims = sec("ONE", "THE ID TOKEN'S CLAIMS")
    refused = sec("ONE", "THE ACCESS TOKEN")[0]
    idt, at = toks[0].split(), toks[1].split()
    header(d, 80, 210, "ID TOKEN: WHO SIGNED IN", GREEN)
    panel(d, 80, 270, 860, 420, GREEN, 3)
    rows(d, 110, 300, [("typ", idt[3]), ("aud", field(claims, "aud").split(",")[0]),
                       ("iss", field(claims, "iss")), ("sub", field(claims, "sub")),
                       ("nonce", "the one sent in this sign-in")], 120, TEXT, 26, 64, 800)
    label(d, 110, 640, "read by the store; never sent to an API", GREEN, 26, True)
    header(d, 980, 210, "ACCESS TOKEN: WHAT IS ALLOWED", CLIENT)
    panel(d, 980, 270, 860, 420, CLIENT, 3)
    rows(d, 1010, 300, [("typ", at[3]), ("aud", "no aud claim"), ("use", "Bearer, for the bank's API")], 120, TEXT, 26, 64, 800)
    label(d, 1010, 640, "says nothing of who is at the browser", CLIENT, 26, True)
    fit_mono(d, 80, 740, "the access token offered as a sign-in:", TEXT, 26, 1760)
    fit_mono(d, 80, 790, refused, ROSE, 26, 1760)
    caption(d, "An access token opens a door; an ID token says who is at the door.", AMBER, 900)


def signin_request(img, d, cfg):
    """The authorization request Spring Security builds, and the claims of the ID token that comes back."""
    req = sec("ONE", "THE REQUEST")
    claims = sec("ONE", "THE ID TOKEN'S CLAIMS")
    result = sec("ONE", "THE RESULT")
    header(d, 60, 210, "THE REQUEST", AMBER)
    panel(d, 60, 270, 900, 470, AMBER, 3)
    fit_mono(d, 85, 290, "GET " + req[0], TEXT, 24, 850)
    rows(d, 85, 345, [(ln.split(None, 1)[0], ln.split(None, 1)[1]) for ln in req[1:]], 330, TEXT, 24, 48, 850)
    draw.arrow_right(d, 970, 500, 1010, MUTED, 4)
    header(d, 1020, 210, "THE RESPONSE: ID TOKEN CLAIMS", GREEN)
    panel(d, 1020, 270, 840, 470, GREEN, 3)
    keys = ["iss", "sub", "aud", "nonce", "email"]
    rows(d, 1045, 295, [(k, field(claims, k)) for k in keys], 110, TEXT, 24, 84, 800)
    fit_mono(d, 60, 780, result[0] + " " + result[1], GREEN, 26, 1800)
    caption(d, "Spring Security keeps the state, nonce and PKCE verifier in her session.", AMBER, 880)


def decoder_checks(img, d, cfg):
    """The store's settings, how a token is signed, and the order Spring Security checks it in."""
    settings = sec("TWO", "THE STORE'S SETTINGS")
    steps = sec("TWO", "HOW IT IS SIGNED")
    checks = sec("TWO", "HOW SPRING SECURITY CHECKS IT")
    header(d, 60, 205, "THE STORE'S SETTINGS", AMBER)
    panel(d, 60, 260, 880, 230, AMBER, 3)
    rows(d, 80, 280, [(ln.split(None, 1)[0] if not ln.startswith("clock") else "skew", ln.split(None, 1)[1] if not ln.startswith("clock") else ln[12:].strip())
                      for ln in settings], 160, TEXT, 24, 52, 840)
    header(d, 60, 520, "HOW IT IS SIGNED", CYAN)
    panel(d, 60, 575, 880, 300, CYAN, 3)
    for i, ln in enumerate(steps):
        fit_mono(d, 80, 595 + i * 45, ln, TEXT if ln[0].isdigit() else MUTED, 22, 840)
    header(d, 1000, 205, "HOW SPRING SECURITY CHECKS IT", GREEN)
    panel(d, 1000, 260, 860, 300, GREEN, 3)
    for i, ln in enumerate(checks):
        fit_mono(d, 1020, 290 + i * 80, ln, TEXT, 24, 820)
    changed = sec("TWO", "ONE THING CHANGED")
    header(d, 1000, 590, "ONE THING CHANGED: REFUSED", ROSE)
    inputs = [changed[i].strip() for i in range(0, len(changed), 2)]
    for i, ln in enumerate(inputs[:5]):
        fit_mono(d, 1020, 650 + i * 44, ln, ROSE, 22, 840)
    caption(d, "The signature first; only then are the claims believed.", AMBER, 900)


def discovery_rotation(img, d, cfg):
    """One issuer-uri, the metadata it leads to, and a key rotation the store follows on its own."""
    conf = sec("THREE", "THE CONFIGURATION")[0]
    meta = sec("THREE", "THE RESPONSE")
    rot = sec("THREE", "KEY ROTATION")
    header(d, 60, 205, "THE CONFIGURATION", AMBER)
    cell(d, 60, 260, 880, 64, conf, AMBER, 26)
    draw.arrow(d, 500, 330, 500, 380, MUTED, 4)
    header(d, 60, 390, "THE RESPONSE (metadata)", GREEN)
    panel(d, 60, 445, 880, 290, GREEN, 3)
    rows(d, 80, 465, [(ln.split(None, 1)[0], ln.split(None, 1)[1]) for ln in meta], 330, TEXT, 22, 54, 840)
    header(d, 1000, 205, "KEY ROTATION", CYAN)
    panel(d, 1000, 260, 860, 475, CYAN, 3)
    for i, ln in enumerate(rot):
        c = TEXT if ln[0].isdigit() else (GREEN if "accepted" in ln else MUTED)
        fit_mono(d, 1020, 285 + i * 72, ln, c, 24, 820)
    label(d, 60, 770, "<iss> stands for the issuer, " + conf.split()[-1], MUTED, 24)
    caption(d, "The metadata must name the configured issuer; new keys are fetched by kid.", AMBER, 880)


def email_takeover(img, d, cfg):
    """Two realms, one e-mail address: one account by e-mail, two by issuer and subject."""
    say = sec("FOUR", "THE TWO PROVIDERS SAY")
    before = sec("FOUR", "BEFORE THE FIX")
    after = sec("FOUR", "AFTER THE FIX")
    header(d, 60, 205, "THE TWO PROVIDERS SAY", CYAN)
    for i in range(2):
        realm_sub, mail = say[2 * i].split(), say[2 * i + 1].strip()
        y = 265 + i * 90
        ok = mail.endswith("true")
        c = GREEN if ok else ROSE
        panel(d, 60, y, 1800, 74, c, 3)
        fit_mono(d, 80, y + 37, "%-7s %s   %s" % (realm_sub[0], realm_sub[1], mail.split(",")[0]), TEXT, 26, 1450, "lm")
        label(d, 1840, y + 37, "verified" if ok else "NOT verified", c, 26, True, "rm")
    header(d, 60, 470, "BEFORE THE FIX: KEYED BY E-MAIL", ROSE)
    panel(d, 60, 530, 880, 260, ROSE, 3)
    lines_in(d, 80, 560, before, 840, 24, 60)
    header(d, 980, 470, "AFTER THE FIX: ISSUER AND SUBJECT", GREEN)
    panel(d, 980, 530, 880, 260, GREEN, 3)
    lines_in(d, 1000, 560, after, 840, 24, 60)
    caption(d, "Same e-mail address, different people: it is not an identity.", AMBER, 880)


def logout_paths(img, d, cfg):
    """The two ways out: the browser sent to Keycloak, and Keycloak telling the store directly."""
    rp = sec("FIVE", "ANA SIGNS OUT AT THE STORE")
    token = sec("FIVE", "THE LOGOUT TOKEN")
    after = sec("FIVE", "AFTER THE LOGOUT TOKEN")[0]
    header(d, 60, 205, "RP-INITIATED: THROUGH THE BROWSER", AMBER)
    panel(d, 60, 260, 880, 520, AMBER, 3)
    for i, ln in enumerate(rp):
        fit_mono(d, 80, 290 + i * 80, ln, GREEN if ln.endswith("true") else TEXT, 26, 840)
    header(d, 1000, 205, "BACK-CHANNEL: SERVER TO SERVER", CYAN)
    panel(d, 1000, 260, 860, 260, CYAN, 3)
    for i, ln in enumerate(token):
        fit_mono(d, 1020, 290 + i * 72, ln, TEXT, 26, 820)
    laptop, phone = re.search(r"laptop signed in: (\w+); phone signed in: (\w+)", after).groups()
    header(d, 1000, 560, "AFTER THE LOGOUT TOKEN", GREEN)
    cell(d, 1000, 620, 410, 90, "laptop: " + ("signed in" if laptop == "true" else "signed out"), MUTED if laptop == "false" else ROSE, 26)
    cell(d, 1450, 620, 410, 90, "phone: " + ("signed in" if phone == "true" else "signed out"), GREEN, 26)
    caption(d, "The store's cookie, Keycloak's session, and every other app: all must end.", AMBER, 880)
