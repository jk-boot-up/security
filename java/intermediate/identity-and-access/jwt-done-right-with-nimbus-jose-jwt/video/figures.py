"""Pictures for the JWT with Nimbus video, drawn by the `figure` slide kind.

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


def section(name, head, k=0):
    return sections(name, head)[k]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def numbered(lines):
    """'1. text' lines as a list of texts; a wrapped line is joined to the item above it."""
    items = []
    for ln in lines:
        if re.match(r"\d+\.\s", ln.strip()):
            items.append(re.sub(r"^\d+\.\s+", "", ln.strip()))
        else:
            items[-1] += " " + ln.strip()
    return items


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


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


def result_colour(text):
    return GREEN if text.startswith("accepted") else ROSE


def token_parts():
    """The three base64url parts of ana's token, joined from the wrapped lines."""
    tok = section("ONE", "THE TOKEN")
    parts, cur = [], None
    for ln in tok:
        if ln.startswith("PART"):
            cur = []
            parts.append(cur)
        else:
            cur.append(ln.strip())
    return ["".join(p) for p in parts]


def signing_part():
    """The signature part as printed: a placeholder, because ES256 signatures differ on every run."""
    return token_parts()[2]


# ------------------------------------------------------------------ figures

def token_anatomy(img, d, cfg):
    """Ana's real token, its three parts coloured, and what each part decodes to."""
    parts = token_parts()
    colours = (AMBER, CYAN, ROSE)
    header(d, 60, 190, heading("ONE", "THE TOKEN").rstrip(":"), TEXT)
    # the token as one stream, wrapped, each part in its colour, with the dots in white
    f = font(MONO_B, 24)
    cw = f.getlength("M")
    per = int(1780 // cw)
    runs = []
    for i, (p, c) in enumerate(zip(parts, colours)):
        runs += [(ch, c) for ch in p]
        if i < 2:
            runs.append((".", TEXT))
    panel(d, 60, 250, 1800, 30 + 34 * ((len(runs) + per - 1) // per), LINE)
    for k, (ch, c) in enumerate(runs):
        d.text((80 + (k % per) * cw, 265 + (k // per) * 34), ch, font=f, fill=c)
    hdr = section("ONE", "THE HEADER")[0].strip()
    claims = [ln.strip().split("  ")[0] for ln in section("ONE", "THE CLAIMS")]
    sig = section("ONE", "STEP 2")[2].strip()
    y = 520
    boxes = [("1. THE HEADER", AMBER, [m + ("," if i < 2 else "") for i, m in enumerate(hdr.strip("{}").split(","))]),
             ("2. THE CLAIMS", CYAN, claims),
             ("3. THE SIGNATURE", ROSE, ["ES256: R and S,", "32 bytes each = 64 bytes", "", "a fresh random number", "each time, so it is", "new on every run"])]
    for i, (title, c, lines) in enumerate(boxes):
        x = 60 + i * 610
        header(d, x, y, title, c)
        panel(d, x, y + 56, 580, 300, c, 3)
        for j, ln in enumerate(lines):
            fit_mono(d, x + 20, y + 76 + j * 38, ln, TEXT, 24, 540)
    caption(d, "Parts 1 and 2 are readable by anyone; part 3 proves who wrote them.", AMBER, 920)


def es256_steps(img, d, cfg):
    """How the token is signed: the sizes, the SHA-256 of the signing input, and two signatures of the same claims."""
    sizes = section("ONE", "STEP 1")[0].strip()
    hb, hc, cb, cc = re.match(r"header (\d+) bytes -> (\d+) characters, claims (\d+) bytes -> (\d+)", sizes).groups()
    step2 = section("ONE", "STEP 2")
    sin = re.search(r"(\d+) ASCII bytes", step2[0]).group(1)
    sha = step2[1].split(":", 1)[1].strip()
    sb, sc = re.search(r"= (\d+) bytes -> (\d+) characters", step2[2]).groups()
    total = re.search(r"\((\d+) characters", heading("ONE", "THE TOKEN")).group(1)
    header(d, 60, 190, "BEFORE SIGNING", AMBER)
    stages = [("header + claims", "%s + %s bytes" % (hb, cb), AMBER),
              ("base64url", "%s + %s chars" % (hc, cc), CYAN),
              ("signing input", "%s bytes" % sin, CYAN),
              ("SHA-256", "32 bytes", CYAN),
              ("ECDSA, P-256", "%s bytes -> %s chars" % (sb, sc), ROSE)]
    for i, (name, val, c) in enumerate(stages):
        x = 60 + i * 370
        cell(d, x, 250, 320, 70, name, c, 24)
        mono(d, x + 160, 360, val, TEXT, 24, "mm")
        if i < len(stages) - 1:
            draw.arrow_right(d, x + 326, 285, x + 364, MUTED, 4)
    label(d, 60, 420, "SHA-256 of the signing input:", MUTED, 26, True)
    mono(d, 520, 420, sha, CYAN, 26)
    label(d, 60, 470, "ECDSA mixes in the bank's private key and a fresh random number k.", MUTED, 26)
    header(d, 60, 540, "AFTER: THE SAME CLAIMS SIGNED TWICE", GREEN)
    for j, ln in enumerate(section("ONE", "THE SAME CLAIMS")):
        text, val = ln.strip().rsplit(": ", 1)
        y = 610 + j * 80
        panel(d, 60, y, 1300, 64, GREEN if val == "true" else AMBER, 3)
        label(d, 85, y + 32, text, TEXT, 28, False, "lm")
        mono(d, 1330, y + 32, val, GREEN if val == "true" else AMBER, 30, "rm")
    label(d, 1400, 640, "the token:", MUTED, 26, True)
    mono(d, 1400, 690, "%s characters" % total, GREEN, 30)
    caption(d, "Different bytes each time, yet both verify: so the signature is not printed.", AMBER, 900)


def forged_token(img, d, cfg):
    """The edited claims with ana's signature kept, and what each reader makes of it."""
    edit = section("TWO", "THE EDIT")[0].strip()
    sent = section("TWO", "THE TOKEN SENT")
    header(d, 60, 190, "THE EDIT", AMBER)
    mono(d, 260, 212, edit, TEXT, 28, "lm")
    header(d, 60, 270, "THE TOKEN SENT", CYAN)
    for i, ln in enumerate(sent):
        name, rest = re.match(r"(part \d, the \w+)\s+(.*)", ln.strip()).groups()
        changed = "unchanged" not in rest
        x = 60 + i * 610
        c = ROSE if changed else MUTED
        panel(d, x, 330, 580, 130, c, 3)
        label(d, x + 20, 350, name, c, 26, True)
        fit_mono(d, x + 20, 400, rest, TEXT, 24, 540)
    naive = section("TWO", "SignedJWT.parse")
    valid = section("TWO", "THE VALIDATOR")
    header(d, 60, 510, "SignedJWT.parse ALONE", ROSE)
    panel(d, 60, 570, 880, 220, ROSE, 3)
    label(d, 85, 595, "reads the claims of any token,", MUTED, 26)
    label(d, 85, 635, "never checks part 3", MUTED, 26)
    fit_mono(d, 85, 700, naive[0].strip(), ROSE, 26, 830)
    header(d, 980, 510, "THE VALIDATOR (DefaultJWTProcessor)", GREEN)
    panel(d, 980, 570, 880, 220, GREEN, 3)
    label(d, 1005, 595, "verifies part 3 over parts 1 and 2 first", MUTED, 26)
    fit_mono(d, 1005, 650, valid[0].strip(), TEXT, 24, 830)
    fit_mono(d, 1005, 710, " ".join(l.strip() for l in valid[1:]).replace("->", "").strip(), GREEN, 26, 830)
    caption(d, "The signature was made over ana's claims, so it cannot match mallory's.", AMBER, 880)


def header_check(img, d, cfg):
    """The API's fixed settings, the four headers sent, and the result of each."""
    settings = [ln.strip() for ln in section("THREE", "THE API'S SETTINGS")]
    alg = settings[0].split(": ")[1].split()[0]
    typ = settings[1].split(": ")[1].split()[0]
    header(d, 60, 190, "THE API'S SETTINGS (in AccessTokenValidator)", CYAN)
    for j, ln in enumerate(settings):
        fit_mono(d, 60, 255 + j * 40, ln, CYAN, 26, 900)
    jdk = section("THREE", "CHECKED WITHOUT")[0].strip()
    header(d, 1040, 190, "CHECKED WITHOUT NIMBUS", GREEN)
    fit_mono(d, 1040, 255, jdk.split(": ")[0] + ":", TEXT, 24, 820)
    mono(d, 1040, 295, jdk.split(": ")[1], GREEN, 26)
    sent = numbered(section("THREE", "THE HEADERS SENT"))
    res = numbered(section("THREE", "THE RESULTS"))
    header(d, 60, 380, "THE HEADERS SENT", AMBER)
    header(d, 1000, 380, "THE RESULTS", GREEN)
    label(d, 130, 450, "alg", MUTED, 24, True, "mm")
    label(d, 310, 450, "typ", MUTED, 24, True, "mm")
    for i, (s_, r) in enumerate(zip(sent, res)):
        a, t, why = s_.split(None, 2)
        y = 475 + i * 100
        cell(d, 60, y, 150, 64, a, GREEN if a == alg else ROSE, 26)
        cell(d, 230, y, 160, 64, t, GREEN if t == typ else ROSE, 26)
        fit_mono(d, 410, y + 32, why, MUTED, 22, 520, "lm")
        draw.arrow_right(d, 940, y + 32, 990, MUTED, 4)
        words, lines = r.split(), [""]
        for w in words:
            if len(lines[-1]) + len(w) > 46:
                lines.append("")
            lines[-1] += (" " if lines[-1] else "") + w
        for k, ln in enumerate(lines[:2]):
            mono(d, 1000, y + 32 + (k - (len(lines[:2]) - 1) / 2) * 32, ln, result_colour(r), 24, "lm")
    caption(d, "Green matches the API's own settings; the token cannot change them.", AMBER, 900)


def clock_line(img, d, cfg):
    """The token's times on a line, the three moments tried, and the four changed claims."""
    times = section("FOUR", "THE TIMES")
    iat, nbf, exp = [int(v) for v in re.findall(r"\d{10}", times[0])]
    limit = int(times[1].rsplit("=", 1)[1])
    moments = [int(re.search(r"now (\d+)", ln).group(1)) for ln in section("FOUR", "THE MOMENTS")]
    res = numbered(section("FOUR", "THE RESULTS", 0))
    header(d, 60, 190, "THE TIMES IN ana's TOKEN (seconds after iat)", CYAN)
    x0, x1 = 120, 1800
    span = (limit + 30) - iat
    xs = lambda t: x0 + (t - iat) * (x1 - x0) / span
    y = 360
    d.rectangle([xs(iat), y - 8, xs(exp), y + 8], fill=GREEN)
    d.rectangle([xs(exp), y - 8, xs(limit), y + 8], fill=AMBER)
    d.rectangle([xs(limit), y - 8, x1, y + 8], fill=ROSE)
    for t, name, anchor in ((iat, "iat = nbf = 0", "la"), (exp, "exp = %d" % (exp - iat), "ra"),
                            (limit, "exp + 30 = %d" % (limit - iat), "la")):
        d.line([xs(t), y - 30, xs(t), y + 30], fill=TEXT, width=3)
        label(d, xs(t) + (8 if anchor == "la" else -8), y - 70, name, TEXT, 24, True, anchor)
    label(d, (xs(exp) + xs(limit)) / 2, y + 22, "skew", AMBER, 24, True, "ma")
    for i, (t, r) in enumerate(zip(moments, res)):
        c = result_colour(r)
        d.polygon([(xs(t), y + 60), (xs(t) - 14, y + 84), (xs(t) + 14, y + 84)], fill=c)
        label(d, xs(t) + (-20 if i == 1 else 20 if i == 2 else 0), y + 90, str(i + 1), c, 24, True, "ma")
        mono(d, 60 + i * 600, y + 150, "%d. now %d: %s" % (i + 1, t - iat, r), c, 24)
    changed = numbered(section("FOUR", "ONE CLAIM CHANGED"))
    res2 = numbered(section("FOUR", "THE RESULTS", 1))
    header(d, 60, 590, "ONE CLAIM CHANGED PER TOKEN", AMBER)
    header(d, 1060, 590, "THE RESULTS", GREEN)
    for i, (c_, r) in enumerate(zip(changed, res2)):
        yy = 650 + i * 60
        fit_mono(d, 60, yy + 25, c_, AMBER, 24, 900, "lm")
        draw.arrow_right(d, 980, yy + 25, 1040, MUTED, 3)
        fit_mono(d, 1060, yy + 25, r, ROSE, 24, 800, "lm")
    caption(d, "Inside the green and amber band, and for this API only: else refused.", AMBER, 920)


def key_source(img, d, cfg):
    """The one key set URL, the attacks a token can try, and the rotation, with the fetch counts."""
    url = heading("FIVE", "THE KEY SET AT").split(" AT ")[1].split(" (")[0]
    keys = [ln.split()[0] for ln in section("FIVE", "THE KEY SET AT")]
    src = section("FIVE", "THE API'S KEY SOURCE")[0].strip()
    header(d, 60, 190, "THE ONE URL IN THE API'S SETTINGS", CYAN)
    cell(d, 60, 245, 760, 56, url, CYAN, 24)
    fit_mono(d, 850, 273, "keys: " + ", ".join(keys) + ";  " + src.split("; ", 1)[1], TEXT, 22, 1010, "lm")
    sent = numbered(section("FIVE", "THE TOKENS SENT")) + numbered(section("FIVE", "THE ROTATION"))
    res = numbered(section("FIVE", "THE RESULTS", 0)) + numbered(section("FIVE", "THE RESULTS", 1))
    header(d, 60, 330, "THE TOKENS SENT", AMBER)
    header(d, 1000, 330, "THE RESULTS (fetches of the URL)", GREEN)
    for i, (s_, r) in enumerate(zip(sent, res)):
        y = 395 + i * 56
        if i == 5:
            d.line([60, y - 6, 1860, y - 6], fill=LINE, width=2)
        fit_mono(d, 60, y + 22, "%d. %s" % (i + 1, s_), AMBER if i < 5 else CYAN, 24, 920, "lm")
        short = (r.replace("(Signed JWT rejected: Another algorithm expected, or no matching key(s) found)", "(no matching key)")
                 .replace("(Signed JWT rejected: Invalid signature)", "(invalid signature)")
                 .replace("(the key set could not be asked again so soon (RateLimitReachedException))", "(rate limited)")
                 .replace("the attacker's server was asked", "attacker's server asked"))
        c = AMBER if "<-" in r else result_colour(r)
        fit_mono(d, 1000, y + 22, short, c, 24, 860, "lm")
    caption(d, "Keys come only from that URL; a retired key lives on until the cache ends.", AMBER, 925)


def revocation_timeline(img, d, cfg):
    """Ana's access token and refresh token over time, and what each check says after revocation."""
    stop = int(re.search(r"= (\d+)$", section("SIX", "WHAT ana HOLDS")[0]).group(1))
    event = section("SIX", "THE EVENT")[0].strip()
    revoke = int(re.search(r"at (\d+)", event).group(1))
    res = [re.match(r"at (\d+) the (\w+) token:\s+(.*)", ln.strip()).groups() for ln in section("SIX", "THE RESULTS")]
    x0, x1 = 360, 1800
    end = stop + 60
    xs = lambda t: x0 + t * (x1 - x0) / end
    header(d, 60, 190, "WHAT ana HOLDS, OVER TIME (seconds after issue)", CYAN)
    rows = [("access token", 330, stop, GREEN), ("refresh token", 470, revoke, GREEN)]
    for name, y, until, c in rows:
        label(d, 60, y, name, TEXT, 28, True, "lm")
        d.rounded_rectangle([xs(0), y - 22, xs(until), y + 22], radius=10, fill=c)
        d.rounded_rectangle([xs(until), y - 22, xs(end), y + 22], radius=10, fill=(60, 30, 40))
    ticks = {0, revoke} | {int(r[0]) for r in res}
    if all(abs(stop - t) > 10 for t in ticks):
        ticks.add(stop)
    for t in sorted(ticks):
        d.line([xs(t), 280, xs(t), 530], fill=LINE, width=2)
        mono(d, xs(t), 560, str(t), TEXT, 24, "ma")
    label(d, xs(revoke), 600, "the bank revokes", AMBER, 24, True, "ma")
    label(d, xs(stop), 600, "exp + skew", AMBER, 24, True, "ma")
    header(d, 60, 680, "THE RESULTS", GREEN)
    for i, (t, which, r) in enumerate(res):
        y = 750 + i * 48
        bad = "<-" in r
        mono(d, 60, y, "at %s, the %s token:" % (t, which), TEXT, 26)
        mono(d, 560, y, r.split("  <-")[0] + ("   <- STILL WORKS" if bad else ""), ROSE if bad else GREEN, 26)
    caption(d, "Only what the server looks up can be taken back; keep access tokens short.", AMBER, 930)
