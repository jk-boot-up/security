"""Pictures for the authentication vs authorization video, drawn by the `figure` slide kind.

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
DARK = (15, 23, 42)


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("THE RESPONSES")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def rows(act, part):
    """The numbered lines of a section: {"1": "refund O-1 ...", ...}."""
    return dict(re.findall(r"^    (\d+)  (.+)$", section(act, part), re.M))


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def status(text):
    """'403 FORBIDDEN  <- KNOWN' -> '403 FORBIDDEN'"""
    return text.split("  <-")[0].strip()


def status_colour(text):
    return GREEN if text.startswith("200") else ROSE if text.startswith(("401", "403")) else MUTED


def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUESTS: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cell(d, x, y, w, h, text, colour, size=30, mono=True, anchor="mm"):
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=colour, width=3)
    tx = x + w // 2 if anchor == "mm" else x + 24
    d.text((tx, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor=anchor)


def big_box(d, x, y, w, h, text, sub, colour, size=40):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


def hexbytes(text):
    h = text.replace(" ", "").replace("...", "")
    return [h[i:i + 2] for i in range(0, len(h), 2)]


# ------------------------------------------------------------------ figures

def two_questions(img, d, cfg):
    """The whole idea on three real requests: who are you (401), then may you (403), then 200."""
    req, res = rows("FIVE", "THE REQUESTS"), rows("FIVE", "THE RESPONSES")
    picks = [("no token", "refund O-1", status(res["1"])),
             ("ben, tok-1", "view O-1 (ana's)", status(res["3"])),
             ("ben, tok-1", "view O-2 (his own)", status(res["4"]))]
    header(d, 100, 205, "THE REQUEST", GREEN, 360)
    header(d, 1480, 205, "THE RESPONSE", ROSE, 340)
    big_box(d, 530, 210, 400, 120, "1. Who are you?", "authentication", CYAN, 34)
    big_box(d, 1010, 210, 400, 120, "2. May you do this?", "authorization", AMBER, 34)
    for i, (who, what, ans) in enumerate(picks):
        y = 380 + i * 170
        cell(d, 100, y, 380, 110, "", CLIENT)
        label(d, 290, y + 34, who, CLIENT, 30, True, "mm")
        label(d, 290, y + 76, what, TEXT, 26, False, "mm")
        c = status_colour(ans)
        stop = 1 if ans.startswith("401") else 2 if ans.startswith("403") else 3
        draw.arrow_right(d, 490, y + 55, 720 if stop == 1 else 1200 if stop == 2 else 1470, MUTED, 4)
        for q, cx in ((1, 730), (2, 1210)):
            if q <= stop:
                ok = q < stop
                d.ellipse([cx - 34, y + 21, cx + 34, y + 89], fill=BG_CELL, outline=GREEN if ok else ROSE, width=4)
                d.text((cx, y + 55), "✓" if ok else "✗", font=font(SANS_B, 40), fill=GREEN if ok else ROSE, anchor="mm")
        if stop < 3:
            x0 = 730 if stop == 1 else 1210
            draw.arrow_right(d, x0 + 36, y + 55, 1470, c, 4)
        cell(d, 1480, y, 340, 110, ans, c, 22, False)
    caption(d, "401: I do not know who you are.    403: I know who you are, and no.", AMBER, 900)


def no_checks(img, d, cfg):
    """Act one: no gate at all, so both anonymous requests get 200 OK."""
    req, res = rows("ONE", "THE REQUESTS"), rows("ONE", "THE RESPONSES")
    header(d, 100, 215, "THE REQUESTS   (no login, no token)", GREEN)
    header(d, 1300, 215, "THE RESPONSES", ROSE)
    big_box(d, 800, 380, 340, 220, "the gate", "asks nothing", MUTED)
    for i, n in enumerate(sorted(req)):
        y = 330 + i * 200
        cell(d, 100, y, 620, 110, n + "   " + req[n].split("  (")[0], GREEN, 34, False)
        if "(" in req[n]:
            label(d, 410, y + 128, "(" + req[n].split("(")[1], MUTED, 26, False, "ma")
        draw.arrow_right(d, 730, y + 55, 790, MUTED, 4)
        draw.arrow_right(d, 1150, y + 55, 1290, ROSE, 4)
        cell(d, 1300, y, 520, 110, n + "   " + status(res[n]), ROSE, 38, False)
    caption(d, "Nobody asked who. Anyone can refund Ana's laptop.", ROSE, 820)


def login_tokens(img, d, cfg):
    """Act two: a password in, a session token out; requests with no or unknown tokens get 401."""
    req, res = rows("TWO", "THE REQUESTS"), rows("TWO", "THE RESPONSES")
    header(d, 100, 205, "THE REQUESTS", GREEN)
    header(d, 1060, 205, "THE RESPONSES", ROSE)
    for i, n in enumerate(sorted(req)):
        y = 270 + i * 112 + (40 if i >= 2 else 0)
        text = req[n].replace("log in: ", "log in ").replace("with ", "")
        cell(d, 100, y, 820, 86, n + "  " + text, GREEN if i < 2 else CLIENT, 28, False, "lm")
        draw.arrow_right(d, 930, y + 43, 1050, MUTED, 4)
        ans = status(res[n]) if not res[n].startswith("session") else res[n]
        c = AMBER if ans.startswith("session") else status_colour(ans) if ans[0].isdigit() else ROSE
        cell(d, 1060, y, 760, 86, n + "  " + ans, c, 26 if i == 1 else 30, False, "lm")
    label(d, 100, 498, "then, with or without a token:", MUTED, 26)
    caption(d, "The token stands in for the password. No token, or a fake one: 401.", AMBER, 905)


def password_hash(img, d, cfg):
    """Act three: password + salt through PBKDF2 gives the stored hash; a login hashes again and compares."""
    pw, salt = value("password", "THREE"), value("salt", "THREE")
    rounds = value("rounds", "THREE").split()[0]
    h1, h2 = value("bytes 1-16", "THREE"), value("bytes 17-32", "THREE")
    tries = re.findall(r"^    (\S+)\s+([0-9a-f ]+) \.\.\.  <- (.+)$", section("THREE"), re.M)
    header(d, 100, 205, "BEFORE HASHING", GREEN)
    label(d, 100, 270, "password", GREEN, 26, True)
    cell(d, 100, 305, 420, 70, pw, GREEN, 32)
    label(d, 100, 395, "salt  (different for every account)", CLIENT, 26, True)
    cell(d, 100, 430, 420, 70, salt, CLIENT, 30)
    draw.arrow_right(d, 530, 340, 640, GREEN, 4)
    draw.arrow_right(d, 530, 465, 640, CLIENT, 4)
    big_box(d, 650, 290, 400, 230, "PBKDF2", "HMAC-SHA256, %s rounds" % rounds, AMBER)
    draw.arrow_right(d, 1060, 405, 1150, ROSE, 4)
    header(d, 1160, 205, "AFTER HASHING   (all the shop keeps)", ROSE)
    label(d, 1160, 270, "32 bytes", ROSE, 26, True)
    cell(d, 1160, 305, 660, 70, h1, ROSE, 26)
    cell(d, 1160, 390, 660, 70, h2, ROSE, 26)
    label(d, 1160, 480, "the password itself is stored nowhere", MUTED, 26)
    header(d, 100, 575, "AT LOGIN: THE TYPED PASSWORD, HASHED THE SAME WAY", AMBER)
    stored = hexbytes(h1)[:8]
    for i, (typed, hx, verdict) in enumerate(tries):
        y = 645 + i * 110
        same = verdict.startswith("THE SAME")
        c = GREEN if same else ROSE
        label(d, 100, y + 40, "typed  " + typed, TEXT, 32, True, "lm")
        for k, b in enumerate(hexbytes(hx)):
            cell(d, 480 + k * 82, y, 74, 80, b, GREEN if b == stored[k] else ROSE, 28)
        label(d, 1160, y + 40, ("✓  " if same else "✗  ") + verdict.lower().replace(": ", ", "), c, 32, True, "lm")
    caption(d, "Same password + same salt → the same hash. Anything else → a different one.", AMBER, 885)


def login_only(img, d, cfg):
    """Act four: the gate asks only 'logged in?', so a customer refunds someone else's order."""
    req, res = rows("FOUR", "THE REQUESTS"), rows("FOUR", "THE RESPONSES")
    header(d, 100, 205, "THE REQUESTS   (ben, a customer, token tok-1)", GREEN)
    header(d, 1300, 205, "THE RESPONSES", ROSE)
    big_box(d, 770, 340, 400, 250, "logged in?", "the only question", AMBER)
    d.text((970, 530), "yes ✓", font=font(SANS_B, 34), fill=GREEN, anchor="mm")
    for i, n in enumerate(sorted(req)):
        y = 300 + i * 200
        what, whose = [t.strip() for t in req[n].rstrip(")").split("(")]
        cell(d, 100, y, 600, 120, "", GREEN)
        label(d, 400, y + 40, n + "   " + what, GREEN, 36, True, "mm")
        label(d, 400, y + 88, whose, MUTED, 26, False, "mm")
        draw.arrow_right(d, 710, y + 60, 760, MUTED, 4)
        draw.arrow_right(d, 1180, y + 60, 1290, ROSE, 4)
        r = res[n].split("  <- ")
        cell(d, 1300, y, 520, 120, "", ROSE)
        label(d, 1560, y + 40, n + "   " + r[0], ROSE, 38, True, "mm")
        label(d, 1560, y + 88, r[1].lower(), MUTED, 24, False, "mm")
    caption(d, "Being logged in proved who Ben is. Nothing decided what he may do.", ROSE, 830)


def policy_requests(img, d, cfg):
    """Act five: eight requests, eight responses, coloured by where the gate stopped them."""
    req, res = rows("FIVE", "THE REQUESTS"), rows("FIVE", "THE RESPONSES")
    header(d, 100, 205, "THE REQUESTS", GREEN, 1000)
    header(d, 1220, 205, "THE RESPONSES", ROSE, 600)
    for i, n in enumerate(sorted(req)):
        y = 266 + i * 78
        who, what = re.split(r"\s{2,}", req[n], 1)
        ans = status(res[n])
        c = status_colour(ans)
        cell(d, 100, y, 1000, 64, "", LINE)
        label(d, 130, y + 32, n, MUTED, 28, True, "lm")
        label(d, 180, y + 32, who, CLIENT, 28, True, "lm")
        label(d, 560, y + 32, what, TEXT, 28, False, "lm")
        draw.arrow_right(d, 1110, y + 32, 1210, c, 3)
        cell(d, 1220, y, 600, 64, ans, c, 30, False)
    caption(d, "No token: 401, stopped at question one.   Known but not allowed: 403.", AMBER, 905)


def gate_steps(img, d, cfg):
    """Act six: the rules table, then one request decided in two steps."""
    rules = re.findall(r"^    (CUSTOMER|SUPPORT|ADMIN)\s+(own only|any|no)\s+(own only|any|no)\s+(own only|any|no)$",
                       section("SIX"), re.M)
    req = section("SIX", "THE REQUEST").strip()
    sess = section("SIX", "STEP 1").strip().split("\n")
    step2 = [t.strip() for t in section("SIX", "STEP 2").strip().split("\n")]
    answer = status(section("SIX", "THE RESPONSE").strip())
    header(d, 100, 205, "THE RULES", AMBER)
    for j, col in enumerate(("VIEW", "CANCEL", "REFUND")):
        label(d, 460 + j * 200, 268, col, MUTED, 24, True, "ma")
    for i, r in enumerate(rules):
        y = 305 + i * 62
        label(d, 100, y + 26, r[0], CLIENT, 26, True, "lm")
        for j, v in enumerate(r[1:]):
            c = GREEN if v == "any" else AMBER if v == "own only" else ROSE
            hl = r[0] == "CUSTOMER" and j == 0
            cell(d, 370 + j * 200, y, 180, 52, v, c, 24 if hl else 22, False)
            if hl:
                d.rounded_rectangle([366 + j * 200, y - 4, 554 + j * 200, y + 56], radius=14, outline=TEXT, width=3)
    header(d, 1060, 205, "THE REQUEST", GREEN)
    cell(d, 1060, 270, 760, 80, req, GREEN, 32)
    header(d, 1060, 390, "THE RESPONSE", ROSE)
    cell(d, 1060, 450, 760, 80, answer, ROSE, 36, False)

    y = 530 + 60
    e = header(d, 100, y, "STEP 1", CYAN)
    label(d, e + 20, y + 22, "authentication: who is this?", TEXT, 28, True, "lm")
    cell(d, 100, y + 60, 830, 64, sess[0].strip(), CYAN, 26)
    cell(d, 100, y + 136, 830, 64, sess[1].strip(), GREEN, 26)
    e = header(d, 990, y, "STEP 2", AMBER)
    label(d, e + 20, y + 22, "authorization: may ben do this?", TEXT, 28, True, "lm")
    cell(d, 990, y + 60, 830, 64, step2[0], AMBER, 26)
    cell(d, 990, y + 136, 830, 64, step2[1], ROSE, 26)
    caption(d, "Known (step 1 passes), but not the owner (step 2 fails): 403.", AMBER, 900)


def the_bill(img, d, cfg):
    """Act seven: a session's lifetime on a timeline, and a stale role against a looked-up one."""
    exp = section("SEVEN", "SESSIONS EXPIRE")
    life = int(re.search(r"lifetime (\d+) ticks", exp).group(1))
    at = int(re.search(r"at tick (\d+) the age", exp).group(1))
    refund = re.search(r"refunds O-1: (.+)$", exp, re.M).group(1)
    before = section("SEVEN", "BEFORE THE DEMOTION").strip()
    after = re.findall(r"^    refund, role (.+?):\s+(\d{3} [A-Z]+)", section("SEVEN", "AFTER THE DEMOTION"), re.M)
    lookups = re.findall(r"(\d+) for the", OUT)
    header(d, 100, 205, "SESSIONS EXPIRE", AMBER)
    x0, step = 200, 300
    d.line([(x0, 340), (x0 + step * (at + 0.4), 340)], fill=LINE, width=4)
    d.rounded_rectangle([x0, 318, x0 + step * life, 362], radius=14, fill=(20, 60, 50), outline=GREEN, width=3)
    label(d, x0 + step * life // 2, 340, "session tok-1 is live: %d ticks" % life, GREEN, 26, True, "mm")
    for t in range(at + 1):
        x = x0 + t * step
        d.line([(x, 372), (x, 392)], fill=MUTED, width=3)
        label(d, x, 400, "tick %d" % t, MUTED, 24, False, "ma")
    label(d, x0, 280, "sam logs in", GREEN, 26, True, "ma")
    xa = x0 + at * step
    d.text((xa, 340), "✗", font=font(SANS_B, 48), fill=ROSE, anchor="mm")
    label(d, xa, 280, "refund: " + refund, ROSE, 26, True, "ma")
    label(d, x0, 450, "at tick %d the age is %d, not under %d: expired" % (at, at, life), MUTED, 26)

    header(d, 100, 520, "BEFORE THE DEMOTION", GREEN)
    cell(d, 100, 580, 800, 70, before.split(": ", 1)[1], GREEN, 26)
    header(d, 1000, 520, "AFTER THE DEMOTION   (now a CUSTOMER)", ROSE)
    for i, (how, ans) in enumerate(after):
        y = 580 + i * 100
        c = status_colour(ans) if i else AMBER
        cell(d, 1000, y, 820, 80, "", c)
        label(d, 1030, y + 40, "role " + how, TEXT, 28, True, "lm")
        label(d, 1400, y + 40, ans, c, 28, True, "lm")
        label(d, 1800, y + 40, "%s lookup%s" % (lookups[i], "" if lookups[i] == "1" else "s"), MUTED, 24, False, "rm")
    label(d, 100, 690, "the session still says SUPPORT", MUTED, 26)
    label(d, 1020, 790, "200 with the copied role is a stale role", AMBER, 26, True)
    caption(d, "Short sessions cost logins. Current roles cost a lookup per request.", AMBER, 900)
