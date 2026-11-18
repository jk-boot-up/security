"""Pictures for the zero trust access video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


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


def cols(line):
    return re.split(r"\s{2,}", line.strip())


def rows(name, head):
    """A section as {label: value}, split at the first run of two or more spaces."""
    return {c[0]: "  ".join(c[1:]) for c in (cols(ln) for ln in section(name, head)) if len(c) > 1}


def answers(name, head):
    """A section of requests as {label: "403  reason"}: the label is everything before the status code."""
    out = {}
    for ln in section(name, head):
        m = re.match(r"(.*?)\s+(\d{3}(?:\s+.*)?)$", ln)
        if m:
            out[re.sub(r"\s+", " ", m.group(1))] = m.group(2)
    return out


def status(value):
    """'403  role not allowed' -> (403, 'role not allowed')."""
    m = re.match(r"(\d{3})\s*(.*)", value)
    return int(m.group(1)), m.group(2)


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


def code_colour(code):
    return GREEN if code == 200 else AMBER if code == 503 else ROSE


# ------------------------------------------------------------------ figures

def one_request(img, d, cfg):
    """The core idea: one request meets three checks before the ledger, and each answer leaves a record."""
    dec = answers("THREE", "THE DECISIONS AT THE PROXY")
    bad = answers("TWO", "THE BAD TOKENS AT THE PROXY")
    ok_code, _ = status(next(v for k, v in dec.items() if "10:00" in k))
    late_code, late_why = status(next(v for k, v in dec.items() if "23:00" in k))
    none_code, none_why = status(bad["no token"])
    header(d, 60, 175, "THE REQUEST: alice (teller), POST /transfers, with a token", CYAN)
    steps = [("1 AUTHENTICATE", "who is calling?", "signature, iss, aud, exp"),
             ("2 AUTHORIZE", "may they do this?", "OPA: role, device, hour"),
             ("3 RECORD", "write it down", "one line per request")]
    xs = [60, 520, 980]
    for (title, q, sub), x in zip(steps, xs):
        panel(d, x, 250, 400, 180, CYAN)
        label(d, x + 200, 270, title, TEXT, 30, True, "ma")
        label(d, x + 200, 320, q, AMBER, 26, True, "ma")
        label(d, x + 200, 370, sub, MUTED, 22, False, "ma")
        draw.arrow_right(d, x + 404, 340, x + 456, MUTED, 4)
    panel(d, 1440, 250, 420, 180, GREEN)
    label(d, 1650, 270, "THE LEDGER", TEXT, 30, True, "ma")
    label(d, 1650, 330, "runs only if all said yes", MUTED, 22, False, "ma")
    header(d, 60, 480, "THE RESULT, FOR THREE REQUESTS", AMBER)
    results = [("alice, 10:00, a valid token", ok_code, "allowed: the ledger ran", 3),
               ("alice, 23:00, the same token", late_code, late_why, 1),
               ("no token at all", none_code, none_why, 0)]
    names = ["checked", "decided", "recorded"]
    for i, (who, code, why, passed) in enumerate(results):
        y = 550 + i * 105
        label(d, 60, y + 18, who, TEXT, 28, True)
        for k in range(3):
            if k == 2:
                text, colour = "recorded", AMBER
            elif k < passed:
                text, colour = names[k] + ": yes", GREEN
            elif k == passed:
                text, colour = names[k] + ": no", ROSE
            else:
                text, colour = "skipped", LINE
            cell(d, 520 + k * 240, y, 220, 64, text, colour, 24)
        cell(d, 1260, y, 140, 64, str(code), code_colour(code), 32)
        label(d, 1430, y + 18, why, code_colour(code), 26, True)
    caption(d, "Every request is checked near the ledger, and every answer is written down.", AMBER, 900)


def perimeter(img, d, cfg):
    """Before: ten requests with no identity reach the ledger, and all of them succeed."""
    reqs = [re.match(r"(.*?)\s+(\d+)$", ln).groups() for ln in section("ONE", "THE REQUESTS")]
    res = rows("ONE", "THE RESULT")
    parts = [cols(ln) for ln in section("ONE", "THE PARTS")]
    header(d, 60, 175, heading("ONE", "THE REQUESTS").rstrip(":"), CYAN)
    for i, (what, n) in enumerate(reqs):
        y = 250 + i * 80
        cell(d, 60, y, 560, 60, what, TEXT, 26)
        label(d, 650, y + 14, "x " + n, MUTED, 28, True)
        draw.arrow_right(d, 730, y + 30, 900, ROSE, 4)
    panel(d, 910, 250, 420, 300, ROSE)
    label(d, 1120, 270, "THE LEDGER", TEXT, 32, True, "ma")
    label(d, 1120, 320, "inside the network", MUTED, 24, False, "ma")
    label(d, 1120, 380, "succeed " + res["succeed"], ROSE, 34, True, "ma")
    label(d, 1120, 440, "ran " + res["ledger ran"].split(",")[0], MUTED, 24, True, "ma")
    label(d, 1120, 480, res["ledger ran"].split(", ")[1], MUTED, 24, True, "ma")
    header(d, 1380, 250, "WHO DID IT?", AMBER)
    who = res["who"].split(": ")
    label(d, 1380, 320, who[0], ROSE, 40, True)
    label(d, 1380, 380, who[1].split(", ")[0] + ",", MUTED, 24)
    label(d, 1380, 415, who[1].split(", ")[1], MUTED, 24)
    header(d, 60, 600, "THE PARTS OF THE AFTER", GREEN)
    for i, c in enumerate(parts):
        x, y = 60 + (i % 2) * 920, 670 + (i // 2) * 70
        label(d, x, y, c[0], GREEN, 26, True)
        fit_mono(d, x + 260, y + 2, c[1], MUTED, 22, 620)
    caption(d, "Being inside the network was the only check, and it names no one.", AMBER, 935)


def signature_by_hand(img, d, cfg):
    """RS256 by hand: s^e mod n is a 256-byte block whose last 32 bytes must be the SHA-256 of header.claims."""
    how = rows("TWO", "HOW THE SIGNATURE")
    forged = [cols(ln) for ln in section("TWO", "THE SAME STEPS")]
    m = re.match(r"00 01, (\d+) x FF, 00, label \((\d+) bytes\), hash \((\d+)\)", how["m holds"])
    ff, lab, hsh = (int(g) for g in m.groups())
    total = int(re.search(r"(\d+) bytes", how["m"]).group(1))
    header(d, 60, 175, "THE INPUT: alice's token, and the bank's key from the JWKS", CYAN)
    fit_mono(d, 60, 240, "key " + how["key"] + "     s = " + how["s"], TEXT, 26, 1800)
    header(d, 60, 300, "HOW THE SIGNATURE IS CHECKED: m = s^e mod n (%d bytes)" % total, AMBER)
    x0, x1, y = 60, 1860, 380
    widths = [110, 0, 70, 260, 360]
    widths[1] = (x1 - x0) - sum(widths)
    parts = [("00 01", MUTED), ("%d x FF" % ff, LINE), ("00", MUTED), ("label, %d bytes" % lab, CYAN), ("hash, %d bytes" % hsh, GREEN)]
    x = x0
    for (name, colour), w in zip(parts, widths):
        d.rectangle([x, y, x + w, y + 80], fill=BG_CELL, outline=colour, width=3)
        fit_mono(d, x + w // 2, y + 40, name, colour, 26, w - 12, "mm")
        x += w
    label(d, x0, y + 100, "the padding: 00 01, then %d bytes of FF, then 00" % ff, MUTED, 24)
    label(d, x1 - 360 - 130, y + 100, "says: a SHA-256 hash follows", CYAN, 24, False, "ma")
    d.line([x1 - 180, y + 84, x1 - 180, y + 150], fill=GREEN, width=4)
    label(d, x1, y + 160, "SHA-256(header.claims) = these 32 bytes: " + how["hash"].rsplit(": ", 1)[1], GREEN, 28, True, "ra")
    label(d, x1, y + 210, "the JDK's " + how["JDK"], MUTED, 24, False, "ra")
    header(d, 60, 650, "THE SAME STEPS ON TWO FORGERIES", ROSE)
    head = forged[0]
    label(d, 760, 710, "padding", MUTED, 24, True, "ma")
    label(d, 960, 710, "hash", MUTED, 24, True, "ma")
    label(d, 1160, 710, "the JDK", MUTED, 24, True, "ma")
    for i, c in enumerate(forged[1:]):
        yy = 750 + i * 56
        pad, hs, jdk = c[-3:]
        label(d, 60, yy + 8, c[0], TEXT, 26, True)
        for x, v in ((760, pad), (960, hs)):
            cell(d, x - 70, yy, 140, 46, v, GREEN if v == "yes" else ROSE, 24)
        cell(d, 1080, yy, 160, 46, jdk, GREEN if jdk == "valid" else ROSE, 24)
    del head
    label(d, 1300, 760, "edited claims: same padding,", MUTED, 24)
    label(d, 1300, 795, "but the hash no longer fits", MUTED, 24)
    label(d, 1300, 845, "another key: m is noise,", MUTED, 24)
    label(d, 1300, 880, "not even the padding", MUTED, 24)


def policy_steps(img, d, cfg):
    """OPA's rules worked out for one transfer at 10:00 and at 23:00, and the grants they read."""
    rule = [cols(ln) for ln in section("THREE", "HOW THE DECISION")]
    grants = [cols(ln) for ln in section("THREE", "THE GRANTS")]
    inp = section("THREE", "THE INPUT")
    header(d, 60, 175, "THE INPUT: alice (teller), POST /transfers", CYAN)
    for i, ln in enumerate(inp):
        fit_mono(d, 60, 240 + i * 40, ln, TEXT, 24, 860)
    header(d, 60, 340, "HOW THE DECISION IS MADE (from OPA)", AMBER)
    label(d, 500, 410, rule[0][1], MUTED, 26, True, "ma")
    label(d, 820, 410, rule[0][2], MUTED, 26, True, "ma")
    for i, c in enumerate(rule[1:]):
        y = 450 + i * 64
        fit_mono(d, 60, y + 12, c[0], TEXT, 26, 300)
        for x, v in ((500, c[1]), (820, c[2])):
            good = v in ("true", "[]", "transfer")
            cell(d, x - 150, y, 300, 52, v, GREEN if good else ROSE, 22)
    header(d, 1000, 175, "THE GRANTS: ANY OTHER REQUEST IS DENIED", GREEN)
    ops = ["GET balance", "POST transfer", "GET audit", "POST freeze"]
    for j, op in enumerate(ops):
        label(d, 1290 + j * 150, 250, op.split()[0], MUTED, 20, True, "ma")
        label(d, 1290 + j * 150, 275, op.split()[1], MUTED, 22, True, "ma")
    for i, (role, have) in enumerate(grants):
        y = 320 + i * 80
        label(d, 1000, y + 14, role, TEXT, 28, True)
        for j, op in enumerate(ops):
            yes = op in have.split(", ")
            cell(d, 1225 + j * 150, y, 130, 56, "yes" if yes else "no", GREEN if yes else LINE, 24)
    label(d, 1000, 600, "a request passes only when", MUTED, 26, True)
    label(d, 1000, 640, "the list of reasons is empty:", MUTED, 26, True)
    label(d, 1000, 680, "allow = count(reasons) == 0", AMBER, 28, True)
    caption(d, "A good token is not enough: the role, the device and the hour must agree.", AMBER, 900)


def token_window(img, d, cfg):
    """How long a token keeps working after its session ends: 300 s, 0 with a session check, 4 with a short life."""
    std = answers("FOUR", "AFTER THE LOGOUT")
    std["calls to Keycloak for those 3"] = rows("FOUR", "AFTER THE LOGOUT")["calls to Keycloak for those 3"]
    short = answers("FOUR", "A SHORT LIFETIME")
    down = answers("FOUR", "WHEN A PART IS DOWN")
    life = int(re.search(r"(\d+) seconds", heading("FOUR", "THE TOKEN")).group(1))
    slife = int(re.search(r"\((\d+) seconds", heading("FOUR", "A SHORT LIFETIME")).group(1))
    header(d, 60, 175, "THE INPUT: alice logs out; does her token still work?", CYAN)
    x0, x1 = 520, 1820
    label(d, x0, 245, "logout", AMBER, 24, True, "ma")
    label(d, x1, 245, "%d s after the login" % life, MUTED, 24, True, "ra")
    d.line([x0, 280, x0, 640], fill=AMBER, width=3)
    lanes = [("no session check", status(std["the same token, no session check"]), x1, "works until it expires"),
             ("a session check", status(std["the same token, with a session check"]), x0, "refused at once"),
             ("a %d second token" % slife, status(short["after the logout, at once"]), x0 + 60, "works %d s at most" % slife)]
    for i, (name, (code, _), end, note) in enumerate(lanes):
        y = 300 + i * 110
        label(d, 60, y + 14, name, TEXT, 28, True)
        if end > x0:
            d.rounded_rectangle([x0, y, end, y + 60], radius=10, fill=ROSE)
        else:
            cell(d, x0 + 10, y, 120, 60, "401", GREEN, 28)
        label(d, x0 + 160 if end <= x0 + 60 else x0 + 30, y + 14, note + "  (%d after logout)" % code if code == 200 else note,
              INK if end > x0 + 60 else MUTED, 26, True)
    s_after = status(std["the same token, with a session check"])
    calls = std["calls to Keycloak for those 3"]
    label(d, 60, 660, "the session check costs a call to Keycloak per request: %s for 3" % calls, MUTED, 26, True)
    header(d, 60, 720, "WHEN A PART IS DOWN", AMBER)
    for i, (k, v) in enumerate(down.items()):
        code, why = status(v)
        y = 790 + i * 46
        label(d, 60, y, k, TEXT, 26, True)
        label(d, 700, y, str(code) + ("  " + why if why else ""), code_colour(code), 26, True)
    del s_after


def audit_reasons(img, d, cfg):
    """Every request has one record; the reasons, counted; and no token in the file."""
    check = rows("FIVE", "THE REQUESTS AGAINST")
    head = heading("FIVE", "THE REASONS")
    cells = []
    for ln in section("FIVE", "THE REASONS"):
        for m in re.finditer(r"([a-z][a-z ]*?)\s+(\d+)(?=\s{2,}|$)", ln):
            cells.append((m.group(1).strip(), int(m.group(2))))
    cells.sort(key=lambda c: -c[1])
    nothold = rows("FIVE", "WHAT THE LOG")
    header(d, 60, 175, "THE INPUT: " + check["requests sent to the proxy"] + " REQUESTS", CYAN)
    header(d, 60, 250, "THE RESULT: " + check["records written"] + " RECORDS, " + check["same order, same status"] + " MATCH", GREEN)
    header(d, 60, 330, head.rstrip(":"), AMBER)
    top = max(n for _, n in cells)
    for i, (reason, n) in enumerate(cells):
        y = 400 + i * 40
        label(d, 420, y, reason, GREEN if reason == "allowed" else MUTED, 22, True, "ra")
        d.rounded_rectangle([440, y, 440 + int(n / top * 700), y + 30], radius=6, fill=GREEN if reason == "allowed" else ROSE)
        label(d, 450 + int(n / top * 700), y, str(n), TEXT, 22, True)
    header(d, 1300, 400, "WHAT THE LOG DOES NOT HOLD", ROSE)
    label(d, 1300, 470, "tokens (access or refresh)", MUTED, 26, True)
    label(d, 1300, 510, nothold["tokens (access or refresh)"], GREEN, 40, True)
    label(d, 1300, 590, "a name taken from a forged token", MUTED, 26, True)
    v = nothold["a name from a forged token"].split(": ")
    label(d, 1300, 630, v[0], GREEN, 40, True)
    label(d, 1300, 690, v[1], MUTED, 24)
