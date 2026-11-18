"""Pictures for the Tokens in the Browser with Spring Boot video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
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
    """Every section of an act whose header starts with `head`, as lists of lines (stripped)."""
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


def sec(name, head):
    return sections(name, head)[0]


def after_arrow(line):
    return line.split("->", 1)[1].strip()


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


def fit_label(d, x, y, text, colour, size, max_w, bold=True, anchor="la"):
    path = SANS_B if bold else SANS
    f = font(path, size)
    while f.getlength(text) > max_w and f.size > 14:
        f = font(path, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def status_colour(text, theft):
    """Green when the defence holds, rose when the thief gets in; `theft` says which answer is the bad one."""
    good = text.startswith(("401", "403", "never", "refused", "the script is no longer"))
    return ROSE if (not good) == theft else GREEN


# ------------------------------------------------------------------ figures

def what_scripts_read(img, d, cfg):
    """The same script on the page, before (tokens in localStorage) and after (tokens on the BFF)."""
    signin = sec("ONE", "THE SIGN-IN")
    took = after_arrow(sec("ONE", "THE SCRIPT ON THE PAGE")[0])
    signin2 = sec("TWO", "THE SIGN-IN")
    http_only = next(ln for ln in sec("TWO", "THE COOKIE'S RULES") if ln.startswith("HttpOnly")).split(None, 1)[1]
    seen = sec("TWO", "THE SAME SCRIPT")[0]
    readable, took_after = seen.split(" -> ")[0], after_arrow(seen).split(";")[0]
    store = re.split(r"\s{2,}", sec("TWO", "WHERE THE TOKENS ARE")[0])
    cols = [(60, "BEFORE: TOKENS IN localStorage", ROSE), (990, "AFTER: TOKENS ON THE BFF", GREEN)]
    for x, title, c in cols:
        header(d, x, 200, title, c)
        panel(d, x, 260, 870, 330, c, 3)
        label(d, x + 25, 278, "the bank's page, in Ana's browser", MUTED, 26, True)
    label(d, 85, 330, "localStorage", CYAN, 30, True)
    for i, ln in enumerate(signin):
        name, value = ln.split(None, 1)
        fit_mono(d, 105, 380 + i * 54, name, TEXT, 30, 260)
        fit_mono(d, 360, 384 + i * 54, value, AMBER, 26, 560)
    label(d, 1015, 330, "localStorage", CYAN, 30, True)
    fit_mono(d, 1035, 380, "(empty)", MUTED, 30, 300)
    label(d, 1015, 440, "cookies", CYAN, 30, True)
    fit_mono(d, 1035, 482, signin2[0].replace("Set-Cookie: ", ""), TEXT, 26, 800)
    fit_label(d, 1035, 518, "HttpOnly: " + http_only, GREEN, 26, 800)
    fit_mono(d, 1035, 552, "XSRF-TOKEN=<random>  (readable on purpose)", MUTED, 24, 800)
    for x, c, text in ((60, ROSE, took), (990, GREEN, took_after + " taken")):
        cell(d, x + 25, 620, 380, 70, "third-party script", AMBER, 30)
        draw.arrow_right(d, x + 420, 655, x + 500, c, 4)
        fit_label(d, x + 515, 655, text, c, 28, 340, anchor="lm")
    label(d, 85, 730, "read by any script on the page,", MUTED, 28)
    label(d, 85, 772, "sent to the script's own server", MUTED, 28)
    fit_label(d, 1015, 730, store[0] + ": " + store[1], GREEN, 28, 840)
    fit_label(d, 1015, 772, "document.cookie shows only " + readable, MUTED, 28, 840, bold=False)
    caption(d, "Anything a script on the page can read, it can send away.", AMBER, 880)


def token_timeline(img, d, cfg):
    """The access token's claims, and what the stolen tokens do while the session lives."""
    claims = [ln.split(None, 1) for ln in sec("ONE", "THE ACCESS TOKEN'S CLAIMS")]
    refresh = sec("ONE", "THE REFRESH TOKEN'S CLAIMS")[0].split(None, 1)[1]
    stolen = sec("ONE", "THE STOLEN TOKENS")
    header(d, 60, 200, "THE ACCESS TOKEN'S CLAIMS (decoded)", CYAN)
    panel(d, 60, 260, 1800, 200, CYAN)
    for i, (k, v) in enumerate(claims):
        y = 280 + i * 44
        fit_mono(d, 90, y, k, CYAN, 30, 120)
        fit_mono(d, 230, y, v, TEXT, 30, 1600)
    header(d, 60, 490, "THE STOLEN TOKENS, OVER TIME (Ana closed the page)", ROSE)
    y0 = 660
    d.line([(120, y0), (1800, y0)], fill=LINE, width=6)
    access_life = re.search(r"\+ (\d+) seconds", claims[3][1]).group(1)
    refresh_life = re.search(r"\+ (\d+) seconds", refresh).group(1)
    later = re.search(r"(\d+ s) later", stolen[1]).group(1)
    marks = [(250, "0 s", "signed in (iat)"), (620, access_life + " s", "access token ends"),
             (900, later, "the thief tries"), (1720, refresh_life + " s", "refresh token ends")]
    for x, when, what in marks:
        d.ellipse([x - 14, y0 - 14, x + 14, y0 + 14], fill=AMBER)
        label(d, x, y0 - 70, when, AMBER, 32, True, "ma")
        fit_label(d, x, y0 + 26, what, MUTED, 26, 300, bold=False, anchor="ma")
    results = [(250, "the access token", after_arrow(stolen[0])),
               (900, "the access token", after_arrow(stolen[1])),
               (1450, "refresh -> new token", after_arrow(stolen[3]))]
    for x, who, res in results:
        c = status_colour(res, theft=True)
        d.rounded_rectangle([x - 225, 730, x + 225, 850], radius=14, fill=PANEL, outline=c, width=3)
        label(d, x, 745, who, MUTED, 26, True, "ma")
        fit_mono(d, x, 800, res, c, 28, 420, "ma")
    caption(d, "One page visit, as long as the session lives.", AMBER, 900)


def bff_layout(img, d, cfg):
    """The browser holds a cookie, the BFF holds the tokens, and a script on the open page still rides along."""
    store = [re.split(r"\s{2,}", ln) for ln in sec("TWO", "WHERE THE TOKENS ARE")]
    rules = sec("TWO", "THE COOKIE'S RULES")
    still = sec("TWO", "WHAT THE SCRIPT CAN STILL DO")
    signin = sec("TWO", "THE SIGN-IN")
    boxes = [(60, "Ana's browser", store[1][1] + ", 2 cookies", CYAN),
             (700, "the BFF (Spring Boot)", store[0][1] + " in its session", GREEN),
             (1340, "the accounts API", "Spring's resource server", MUTED)]
    for x, name, sub, c in boxes:
        draw.box(d, x, 210, 520, 150, name, sub, c, 34, 26)
    draw.arrow_right(d, 585, 285, 695, CYAN, 4)
    draw.arrow_right(d, 1225, 285, 1335, GREEN, 4)
    label(d, 640, 380, "only the cookie", CYAN, 26, True, "ma")
    label(d, 1280, 380, "Bearer <access token>", GREEN, 26, True, "ma")
    header(d, 60, 450, "THE SESSION COOKIE", CYAN)
    panel(d, 60, 510, 1000, 340, CYAN)
    fit_mono(d, 85, 528, signin[0].replace("Set-Cookie: ", ""), TEXT, 28, 950)
    for i, ln in enumerate(rules):
        k, v = re.split(r"\s{2,}", ln, 1)
        y = 590 + i * 62
        fit_mono(d, 85, y, k, AMBER, 28, 300)
        fit_label(d, 400, y, v, TEXT, 28, 640, bold=False)
    header(d, 1120, 450, "A SCRIPT ON THE PAGE", ROSE)
    open_res = after_arrow(still[0])
    closed = re.split(r"\s{2,}", still[2], 1)
    rows = (("page open", open_res, ROSE), (closed[0], closed[1].replace(" ->", ":"), GREEN))
    for i, (when, res, c) in enumerate(rows):
        y = 510 + i * 170
        panel(d, 1120, y, 740, 150, c, 3)
        label(d, 1145, y + 20, when, MUTED, 28, True)
        fit_label(d, 1145, y + 72, res, c, 32, 690)
    caption(d, "Nothing to steal in the browser; the open page can still act.", AMBER, 900)


def cross_site(img, d, cfg):
    """Who starts the request, what the browser attaches, and the answer."""
    results = [after_arrow(ln) for ln in act("THREE") if "->" in ln]
    made = next(ln for ln in act("THREE") if ln.startswith("transfers made"))
    cols = [(60, 520, "WHO STARTS IT"), (600, 260, "COOKIE"), (880, 300, "X-XSRF-TOKEN"), (1200, 660, "THE ANSWER")]
    for x, w, t in cols:
        label(d, x + 10, 205, t, MUTED, 24, True)
    rows = [("a link on evil.example", "modern browser", "not sent", "not sent"),
            ("a form on evil.example", "modern browser", "not sent", "not sent"),
            ("a form on evil.example", "older browser", "sent", "not sent"),
            ("a script on evil.example", "adds the header", "-", "preflight refused"),
            ("the bank's own page", "app.bank.example", "sent", "sent")]
    for i, ((who, sub, ck, hd), res) in enumerate(zip(rows, results)):
        y = 245 + i * 122
        panel(d, 60, y, 1800, 106, GREEN, 3)
        label(d, 85, y + 14, who, TEXT, 28, True)
        label(d, 85, y + 60, sub, MUTED, 24)
        for (x, w, _), v in zip(cols[1:3], (ck, hd)):
            fit_label(d, x + 10, y + 53, v, AMBER if v == "sent" else MUTED, 26, w - 20, anchor="lm")
        fit_mono(d, 1210, y + 53, res, GREEN, 26, 630, "lm")
    caption(d, made, AMBER, 880)


def thumbprint(img, d, cfg):
    """How the key's thumbprint is made, and what one proof carries."""
    key = sec("FOUR", "THE APP'S PUBLIC KEY")
    steps = sec("FOUR", "THE THUMBPRINT")
    jkt = next(ln for ln in sec("FOUR", "THE TOKEN FROM KEYCLOAK") if ln.startswith("cnf.jkt")).split()[1]
    proof = sec("FOUR", "THE PROOF FOR ONE REQUEST")
    header(d, 60, 200, "THE THUMBPRINT, STEP BY STEP", CYAN)
    panel(d, 60, 260, 1000, 150, CYAN)
    for i, ln in enumerate(key):
        fit_mono(d, 85, 274 + i * 44, ln, TEXT, 26, 950)
    n = re.search(r"(\d+) characters", steps[0]).group(1)
    label(d, 85, 420, n + " characters, members a to z, no spaces", MUTED, 26)
    draw.arrow(d, 560, 460, 560, 500, CYAN, 4)
    cell(d, 230, 505, 660, 56, "SHA-256  ->  32 bytes", AMBER, 28)
    draw.arrow(d, 560, 566, 560, 606, CYAN, 4)
    cell(d, 230, 611, 660, 56, "base64url  ->  43 characters", AMBER, 28)
    draw.arrow(d, 560, 672, 560, 712, CYAN, 4)
    d.rounded_rectangle([60, 717, 1060, 790], radius=12, fill=PANEL, outline=GREEN, width=3)
    fit_mono(d, 560, 753, steps[2], GREEN, 28, 960, "mm")
    label(d, 560, 805, "= cnf.jkt in Keycloak's token: " + jkt[:10] + "...", GREEN, 26, True, "ma")
    header(d, 1120, 200, "ONE PROOF (signed by the key)", AMBER)
    panel(d, 1120, 260, 740, 560, AMBER)
    fit_mono(d, 1145, 280, proof[0].replace(", jwk <the public key above>", ""), MUTED, 26, 690)
    fit_mono(d, 1145, 322, "jwk  the public key", MUTED, 26, 690)
    for i, ln in enumerate(proof[1:]):
        k, v = ln.split(None, 1)
        y = 385 + i * 62
        fit_mono(d, 1145, y, k, AMBER, 28, 80)
        fit_mono(d, 1225, y + 2, v, TEXT, 24, 610)
    label(d, 1145, 720, "ath = SHA-256 of the access token", MUTED, 26)
    label(d, 1145, 760, "htm + htu: for this request only", MUTED, 26)
    caption(d, "The token names the key; only that key can sign a fresh proof.", AMBER, 880)


def the_bill(img, d, cfg):
    """Four costs, each with the demo's own answer."""
    two = sec("FIVE", "TWO BFF SERVERS")
    more = sec("FIVE", "ONE MORE SERVER")
    cleared = sec("FIVE", "THE BROWSER'S STORAGE CLEARED")
    script = sec("FIVE", "A SCRIPT ON THE OPEN PAGE")
    cards = [
        ("Two BFF servers", [two[0]], after_arrow(two[1]), AMBER),
        ("One more server", [" ".join(more)], "", AMBER),
        ("Browser storage cleared", [re.sub(r"\s{2,}", "  ", cleared[0]), re.sub(r"\s{2,}", "  ", cleared[1])],
         after_arrow(cleared[2]), AMBER),
        ("A script on the open page", ["it asks the key for a proof"], after_arrow(script[0]), ROSE),
    ]
    for i, (title, lines, res, c) in enumerate(cards):
        x = 60 + (i % 2) * 920
        y = 200 + (i // 2) * 330
        panel(d, x, y, 880, 300, c, 3)
        label(d, x + 25, y + 20, title, c, 32, True)
        text = " ".join(lines)
        words, rows, cur = text.split(), [], ""
        for w in words:
            if len(cur) + len(w) + 1 > 48:
                rows.append(cur)
                cur = w
            else:
                cur = (cur + " " + w).strip()
        rows.append(cur)
        for j, r in enumerate(rows[:3]):
            fit_label(d, x + 25, y + 80 + j * 44, r, TEXT, 30, 830, bold=False)
        if res:
            rc = ROSE if res.startswith("2") else AMBER
            fit_mono(d, x + 25, y + 230, "-> " + res, rc, 28, 830)
    caption(d, "DPoP stops theft, not use while the page is open.", AMBER, 880)
