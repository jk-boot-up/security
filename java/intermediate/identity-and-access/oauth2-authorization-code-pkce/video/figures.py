"""Pictures for the OAuth 2.0 authorization code flow with PKCE video, drawn by the `figure` slide kind.

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


def part(name, head):
    return [ln.strip() for ln in sections(name, head)[0]]


def notes(name):
    """The act's own summary lines: not indented, not a section header."""
    return [ln for ln in act(name) if not ln.startswith(" ") and not re.match(r"[A-Z0-9 ,'()]{6,}", ln.split(":")[0])]


def param(lines, key):
    """The value of key=value in a list of lines such as '&code_challenge=E9Mel...'."""
    for ln in lines:
        m = re.match(r"[?&]?%s=(\S+)" % re.escape(key), ln)
        if m:
            return m.group(1)
    raise KeyError(key)


def refusals(lines):
    """(what was tried, error, reason) for each '-> refused' in a section; 'tried' is the line above, or ''."""
    return [(lines[i - 1] if i else "", ln.split(": ", 1)[1], lines[i + 1])
            for i, ln in enumerate(lines) if ln.startswith("-> refused: ")]


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


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def card(d, x, y, w, title, colour, lines, size=24):
    """A panel with a coloured title and a few lines of mono text."""
    h = 70 + 40 * len(lines)
    panel(d, x, y, w, h, colour, 3)
    label(d, x + 22, y + 18, title, colour, 26, True)
    for i, (text, c) in enumerate(lines):
        fit_mono(d, x + 22, y + 62 + 40 * i, text, c, size, w - 44)
    return y + h


def short(value, n=10):
    return value if len(value) <= n + 3 else value[:n] + "..."


# ------------------------------------------------------------------ figures

def pkce_idea(img, d, cfg):
    """The core idea: the hash travels through the browser, the secret travels directly, and the bank compares."""
    step1 = part("ONE", "STEP 1")
    step4 = part("ONE", "STEP 4")
    challenge = param(step1, "code_challenge")
    verifier = param(step4, "code_verifier")
    leak = refusals(part("TWO", "THE LEAK"))[0]
    # the app and its secret
    header(d, 60, 210, "THE APP MAKES A SECRET", CYAN)
    cell(d, 60, 275, 820, 64, "verifier  " + verifier, CYAN, 24)
    draw.arrow(d, 470, 345, 470, 410, MUTED, 4)
    label(d, 500, 362, "SHA-256, then base64url", MUTED, 24, True)
    cell(d, 60, 415, 820, 64, "challenge " + challenge, AMBER, 24)
    # the two roads
    header(d, 980, 210, "STEP 1, THROUGH THE BROWSER", AMBER)
    panel(d, 980, 270, 880, 140, AMBER, 3)
    fit_mono(d, 1005, 300, "code_challenge=" + challenge, AMBER, 24, 830)
    label(d, 1005, 350, "anyone may see it: a hash cannot be turned back", MUTED, 24)
    header(d, 980, 440, "STEP 4, DIRECTLY TO THE BANK", GREEN)
    panel(d, 980, 500, 880, 140, GREEN, 3)
    fit_mono(d, 1005, 530, "code_verifier=" + verifier, GREEN, 24, 830)
    label(d, 1005, 580, "only the app ever had it", MUTED, 24)
    # the bank's comparison and the thief
    header(d, 60, 540, "THE BANK COMPARES", GREEN)
    panel(d, 60, 600, 820, 150, GREEN, 3)
    label(d, 85, 625, "SHA-256(verifier) = the stored challenge?", TEXT, 26, True)
    label(d, 85, 680, "yes: tokens issued", GREEN, 26, True)
    header(d, 980, 680, "A THIEF WITH THE CODE, NO VERIFIER", ROSE)
    panel(d, 980, 740, 880, 110, ROSE, 3)
    fit_mono(d, 1005, 770, "refused: " + leak[1], ROSE, 26, 830)
    label(d, 1005, 812, leak[2], MUTED, 24)
    caption(d, "The hash travels the risky road; the secret travels the safe one.", AMBER, 900)


def flow_steps(img, d, cfg):
    """The six steps as arrows between four parties, the values taken from the output."""
    step1, step3 = part("ONE", "STEP 1"), part("ONE", "STEP 3")
    step4, step5, step6 = part("ONE", "STEP 4"), part("ONE", "STEP 5"), part("ONE", "STEP 6")
    lifetime = re.search(r"works for (\d+) seconds", " ".join(step3)).group(1)
    expires = param([step5[0].split("  ")[1]], "expires_in")
    lanes = [("Ana's browser", 230, AMBER), ("budgeting app", 720, CYAN), ("bank sign-in", 1210, GREEN), ("bank API", 1690, CLIENT)]
    for name, x, c in lanes:
        cell(d, x - 190, 205, 380, 60, name, c, 26)
        d.line([(x, 268), (x, 880)], fill=LINE, width=2)
    rows = [
        ("1", 720, 1210, "GET /authorize  scope=" + param(step1, "scope") + "  challenge=" + short(param(step1, "code_challenge"), 6), AMBER),
        ("2", 230, 1210, "Ana signs in at the bank and agrees", AMBER),
        ("3", 1210, 720, "302 back: code=<code> (" + lifetime + " s, once)  state=<state>", AMBER),
        ("4", 720, 1210, "POST /token  code + verifier=" + short(param(step4, "code_verifier"), 6), GREEN),
        ("5", 1210, 720, "access token (" + expires + " s) + refresh token", GREEN),
        ("6", 720, 1690, "GET /accounts  " + step6[1].replace("-> ", "-> "), GREEN),
    ]
    for i, (n, x1, x2, text, c) in enumerate(rows):
        y = 330 + i * 95
        if x2 > x1:
            draw.arrow_right(d, x1, y, x2, c, 4)
        else:
            draw.arrow_left(d, x1, y, x2, c, 4)
        d.ellipse([60 - 22, y - 22, 60 + 22, y + 22], fill=c)
        label(d, 60, y, n, INK, 26, True, "mm")
        lo = min(x1, x2)
        fit_mono(d, lo + 20 if lo > 300 else 270, y - 40, text, TEXT, 24, 1580 - (lo + 20 if lo > 300 else 270) + 300)
    label(d, 140, 905, "amber: through the browser, which anyone may watch", AMBER, 26, True)
    label(d, 140, 950, "green: directly between the app and the bank", GREEN, 26, True)


def s256_steps(img, d, cfg):
    """How the S256 challenge is made from the verifier, with the first 3 bytes worked out bit by bit."""
    how = part("TWO", "HOW THE S256")
    verifier = part("TWO", "THE VERIFIER")[0]
    ascii_hex, hash1, hash2 = how[1], how[3], how[4]
    group = how[6]
    challenge = how[8]
    rows = [("THE VERIFIER (43 characters)", CYAN, [verifier]),
            ("1. ASCII BYTES", MUTED, [ascii_hex]),
            ("2. SHA-256: 32 BYTES", AMBER, [hash1, hash2])]
    y = 205
    for title, c, lines in rows:
        label(d, 60, y + 8, title, c, 24, True)
        for j, ln in enumerate(lines):
            fit_mono(d, 560, y + 4 + j * 40, ln, TEXT, 28, 1300)
        y += 40 * len(lines) + 30
    # 3. the first group of 3 bytes, bit by bit
    hexes, bits, nums, chars = [p.strip() for p in group.split("=")]
    label(d, 60, y + 8, "3. BASE64URL: 3 BYTES = 24 BITS = 4 x 6 BITS", ROSE, 24, True)
    y += 60
    colours = [CYAN, AMBER, GREEN, ROSE]
    for k, (b, n, ch) in enumerate(zip(bits.split(), nums.split(), chars)):
        x = 560 + k * 300
        cell(d, x, y, 260, 60, b, colours[k], 30)
        label(d, x + 130, y + 80, "= " + n, MUTED, 26, True, "ma")
        cell(d, x + 80, y + 125, 100, 64, ch, colours[k], 36)
    fit_mono(d, 60, y + 15, "bytes " + hexes, TEXT, 28, 470)
    label(d, 60, y + 140, "6-bit number -> letter", MUTED, 24, True)
    y += 220
    label(d, 60, y + 8, "4. ALL 32 BYTES, NO = PADDING", GREEN, 24, True)
    cell(d, 560, y - 8, 1300, 64, challenge, GREEN, 30)
    caption(d, "The bank redoes these steps with the verifier and compares.", AMBER, 935)


def redirect_check(img, d, cfg):
    """Exact matching against a prefix check for five addresses, then the state check and single-use codes."""
    rows = part("THREE", "THE ADDRESS IN THE REQUEST")
    header(d, 60, 205, "REDIRECT ADDRESS IN THE REQUEST", CYAN)
    label(d, 1290, 227, "EXACT", GREEN, 26, True, "mm")
    label(d, 1570, 227, "PREFIX", ROSE, 26, True, "mm")
    for i, ln in enumerate(rows):
        addr, exact, prefix = ln.split()
        y = 270 + i * 62
        fit_mono(d, 80, y + 26, addr, TEXT, 30, 1060, "lm")
        for x, v in ((1290, exact), (1570, prefix)):
            ok = v == "refused" or i == 0
            cell(d, x - 120, y, 240, 52, v, GREEN if ok else ROSE, 28)
    check = refusals(part("THREE", "THE APP'S CHECK"))[0]
    twice = part("THREE", "ONE CODE, REDEEMED TWICE")
    late = refusals(part("THREE", "ONE CODE, REDEEMED 6"))[0]
    late_head = next(ln for ln in act("THREE") if ln.startswith("ONE CODE, REDEEMED 6"))
    y = 610
    card(d, 60, y, 580, "STATE: MALLORY'S LINK", AMBER,
         [("refused: " + check[1], GREEN), (check[2], MUTED)], 24)
    card(d, 670, y, 580, "ONE CODE, TWICE", AMBER,
         [(twice[0].split(", ")[1], TEXT), ("2nd: " + refusals(twice)[0][1], GREEN), (twice[-1].replace("the first access token now: ", "1st token: "), GREEN)], 24)
    secs = re.search(r"(\d+) SECONDS", late_head).group(1)
    card(d, 1280, y, 580, "A CODE " + secs + " SECONDS OLD", AMBER,
         [("refused: " + late[1], GREEN), (late[2], MUTED)], 24)
    caption(d, notes("THREE")[0].replace("sends the code away", "would send the code away"), ROSE, 900)


def token_family(img, d, cfg):
    """What one scope allows, and how a replayed refresh token revokes its whole family."""
    scope = part("FOUR", "THE SCOPE")[0]
    calls = part("FOUR", "THE API CALLS")
    header(d, 60, 205, "THE TOKEN'S SCOPE: " + scope, CYAN)
    for i, ln in enumerate(calls):
        req, res = ln.split(" -> ")
        y = 275 + i * 80
        ok = res.startswith("2")
        cell(d, 60, y, 330, 60, req, TEXT if ok else MUTED, 24, text_colour=TEXT)
        draw.arrow_right(d, 400, y + 30, 470, MUTED, 4)
        cell(d, 480, y, 330, 60, res, GREEN if ok else ROSE, 24)
    greedy = refusals(part("FOUR", "ASKING FOR"))[0]
    greedy_head = next(ln for ln in act("FOUR") if ln.startswith("ASKING FOR"))
    card(d, 60, 450, 750, greedy_head.split(" (")[0], AMBER,
         [("refused: " + greedy[1], GREEN), (greedy[2].replace("the client may not ask for ", "not allowed: "), MUTED)], 24)
    # the family
    header(d, 900, 205, "THE REFRESH TOKEN FAMILY", AMBER)
    cell(d, 900, 275, 180, 64, "R1", CYAN, 32)
    draw.arrow_right(d, 1090, 307, 1180, MUTED, 4)
    cell(d, 1190, 275, 180, 64, "A2", CYAN, 32)
    cell(d, 1400, 275, 180, 64, "R2", CYAN, 32)
    label(d, 900, 360, "R1 is spent once it is used: " + part("FOUR", "THE REFRESH")[1], MUTED, 24)
    theft = part("FOUR", "THE THEFT")
    refs = refusals(theft)
    y = 430
    header(d, 900, y, "A THIEF REPLAYS R1", ROSE)
    for i, (tried, err, why) in enumerate(refs):
        yy = y + 65 + i * 125
        who = ["R1 again (the thief)", "R2 (the real app)"][i] if i < 2 else tried
        label(d, 900, yy, who, TEXT, 26, True)
        fit_mono(d, 930, yy + 38, "refused: " + err, GREEN, 26, 930)
        fit_mono(d, 930, yy + 76, why, MUTED, 24, 930)
    last = theft[-1].split("  <- ")
    fit_mono(d, 900, y + 320, last[0], ROSE, 28, 960)
    label(d, 900, y + 365, last[1].lower(), AMBER, 28, True)
    caption(d, "One replay revokes the family: the theft is contained, Ana signs in again.", AMBER, 920)


def grants_today(img, d, cfg):
    """The two grants current practice removes, a confidential client's secret, and what a phone app relies on."""
    implicit = refusals(part("FIVE", "THE IMPLICIT FLOW"))[0]
    password = refusals(part("FIVE", "THE PASSWORD GRANT"))[0]
    conf = part("FIVE", "A CONFIDENTIAL CLIENT")
    conf_refs = refusals(conf)
    conf_head = next(ln for ln in act("FIVE") if ln.startswith("A CONFIDENTIAL CLIENT"))
    removed, phone = notes("FIVE")[0], notes("FIVE")[1]
    card(d, 60, 210, 880, "IMPLICIT FLOW: token in the address bar", ROSE,
         [("refused: " + implicit[1], GREEN), (implicit[2], MUTED)], 26)
    card(d, 980, 210, 880, "PASSWORD GRANT: the app holds the password", ROSE,
         [("refused: " + password[1], GREEN), (password[2], MUTED)], 26)
    label(d, W // 2, 400, removed, AMBER, 28, True, "ma")
    name = re.search(r"\((\S+),", conf_head).group(1)
    card(d, 60, 470, 880, "CONFIDENTIAL CLIENT: " + name, CYAN,
         [(conf_refs[0][0] + ": " + conf_refs[0][1], GREEN), (conf_refs[1][0], TEXT), ("-> " + conf_refs[1][1] + ": " + conf_refs[1][2], GREEN)], 26)
    card(d, 980, 470, 880, "PUBLIC CLIENT: an app on a phone", GREEN,
         [("no secret it could keep", MUTED), ("code flow + PKCE, S256", GREEN), ("a fresh verifier per sign-in", GREEN)], 26)
    caption(d, phone, AMBER, 800)
    leaves = part("FIVE", "WHAT THIS SMALL SERVER")
    label(d, W // 2, 870, leaves[1], ROSE, 28, True, "ma")
