"""Pictures for the certificate pinning video, drawn by the `figure` slide kind.

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
    """The lines of one act of the output ("TWO"), without the act's own heading."""
    body = re.search(r"^%s\. .*\n((?:(?![A-Z]+\. ).*\n?)*)" % name, OUT, re.M).group(1)
    return body.splitlines()


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


def section(name, head):
    return sections(name, head)[0]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def pairs(lines):
    """Lines of two columns separated by two or more spaces, as (left, right)."""
    out = []
    for ln in lines:
        parts = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        out.append((parts[0], parts[1] if len(parts) > 1 else ""))
    return out


def note_free(text):
    return text.split("  <-")[0]


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
    while f.getlength(text) > max_w and f.size > 16:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def state_colour(text):
    t = text.lower()
    return ROSE if t.startswith(("opened", "visible", "yes")) else GREEN


# ------------------------------------------------------------------ figures

def ok_colour(text):
    return GREEN if text.startswith(("ACCEPTED", "accepted", "yes")) and "REPORT" not in text else (
        AMBER if "REPORT" in text else ROSE)


def trust_vs_pin(img, d, cfg):
    """The core idea: the same two chains, for an app that trusts the device and for an app with a pin."""
    chains = pairs(section("ONE", "THE CHAINS"))
    plain = dict(pairs(section("ONE", "THE RESULT")))
    pinned = dict(pairs(section("TWO", "THE RESULT")))
    header(d, 60, 200, "THE CHAINS THE APP RECEIVES (leaf <- issuer)", CYAN)
    for i, (who, chain) in enumerate(chains):
        y = 265 + i * 90
        label(d, 60, y + 35, who, TEXT, 30, True, "lm")
        cell(d, 330, y, 1530, 70, chain, CYAN if who == "genuine" else ROSE, 30)
    cols = [("THE APP TRUSTS THE DEVICE", plain, AMBER), ("THE APP PINS THE LEAF'S KEY", pinned, GREEN)]
    for b, (title, res, c) in enumerate(cols):
        x = 60 + b * 930
        header(d, x, 470, title, c, 870)
        panel(d, x, 530, 870, 300, c, 3)
        for j, who in enumerate(("genuine", "interception")):
            y = 560 + j * 130
            label(d, x + 30, y + 40, who, TEXT, 30, True, "lm")
            v = note_free(res[who])
            cell(d, x + 300, y, 540, 80, v, ROSE if who == "interception" and v == "ACCEPTED" else ok_colour(v), 30)
    caption(d, "A valid chain from the wrong root passes, until the app asks for one key.", AMBER, 880)


def device_roots(img, d, cfg):
    """Each chain walked up to a root on the phone, by hand, and what the app without pins did."""
    roots = pairs(section("ONE", "THE DEVICE'S ROOTS"))
    chains = pairs(section("ONE", "THE CHAINS"))
    checks = dict(pairs(section("ONE", "THE CHECK")))
    res = dict(pairs(section("ONE", "THE RESULT")))
    header(d, 60, 200, "THE PHONE'S ROOTS", CYAN)
    for i, (name, why) in enumerate(roots):
        y = 265 + i * 100
        cell(d, 60, y, 520, 76, name, CYAN if i == 0 else ROSE, 30)
        label(d, 610, y + 38, why, MUTED, 28, False, "lm")
    header(d, 60, 500, "THE CHAIN  ->  SIGNED BY A ROOT ON THE PHONE?  ->  THE RESULT", AMBER)
    for i, (who, chain) in enumerate(chains):
        y = 570 + i * 150
        c = CYAN if who == "genuine" else ROSE
        label(d, 60, y, who, TEXT, 30, True)
        fit_mono(d, 60, y + 50, chain, c, 28, 700)
        cell(d, 800, y + 10, 560, 76, checks[who], AMBER, 28)
        draw.arrow_right(d, 1375, y + 48, 1450, MUTED, 4)
        v = note_free(res[who])
        cell(d, 1470, y + 10, 390, 76, v, ROSE if who == "interception" else GREEN, 30)
    caption(d, "Both chains end at a root the phone trusts, so both pass.", AMBER, 905)


def pin_worked_out(img, d, cfg):
    """The leaf's key bytes, their SHA-256, the Base64 pin, and the first Base64 group by hand."""
    inp = section("TWO", "THE INPUT")[0]
    work = section("TWO", "THE PIN, WORKED OUT")
    b64 = pairs(section("TWO", "THE BASE64"))
    spki = re.search(r"(\d+) bytes = (\d+) header \+ (\d+) point", inp).groups()
    sha = re.split(r"\s{2,}", work[0])
    pin = work[2].strip()
    header(d, 60, 200, "THE PIN, WORKED OUT (the leaf's public key)", CYAN)
    steps = [("SubjectPublicKeyInfo", spki[0] + " bytes", "%s header + %s point" % (spki[1], spki[2]), CYAN),
             ("SHA-256", sha[1], sha[2], AMBER),
             ("Base64", str(len(pin)) + " chars", "the pin", GREEN)]
    for j, (name, size, note, c) in enumerate(steps):
        x = 60 + j * 620
        label(d, x + 260, 265, name, MUTED, 26, True, "ma")
        cell(d, x, 305, 520, 80, size, c, 36)
        label(d, x + 260, 400, note, MUTED, 24, False, "ma")
        if j < 2:
            label(d, x + 570, 345, "->", TEXT, 40, True, "mm")
    cell(d, 60, 460, 1800, 70, pin, GREEN, 32)
    header(d, 60, 560, "THE BASE64, BY HAND: 3 BYTES -> 4 LETTERS OF 6 BITS", AMBER)
    rows = dict(b64[:4])
    label(d, 60, 645, "bytes", MUTED, 28, True, "lm")
    for j, byte in enumerate(rows["bytes"].split()):
        cell(d, 260 + j * 420, 617, 380, 56, byte, CYAN, 30)
    for r, key in enumerate(("6 bits", "values", "letters")):
        y = 685 + r * 64
        label(d, 60, y + 30, key, MUTED, 28, True, "lm")
        for j, v in enumerate(rows[key].split()):
            cell(d, 260 + j * 315, y, 285, 54, v, GREEN if key == "letters" else AMBER, 30)
    fit_mono(d, 60, 885, b64[4][0], TEXT, 26, 1800)
    caption(d, "The pin is the hash of the key, not of the certificate.", AMBER, 935)


def renewal_outage(img, d, cfg):
    """The new key's pin against the app's pin, and the fleet worked out."""
    ren = pairs(section("THREE", "THE RENEWAL"))
    res = pairs(section("THREE", "THE RESULT"))[0]
    fleet = pairs(section("THREE", "THE FLEET"))
    head_fleet = heading("THREE", "THE FLEET").replace("THE FLEET, WORKED OUT ", "")
    header(d, 60, 200, "DAY 80: THE BANK RENEWS WITH A NEW KEY", CYAN)
    for i, (who, pin) in enumerate(ren):
        y = 265 + i * 95
        label(d, 60, y + 38, who, TEXT, 30, True, "lm")
        cell(d, 420, y, 620, 76, pin, CYAN if i == 0 else AMBER, 30)
    label(d, 1080, 340, "different", ROSE, 34, True, "lm")
    draw.arrow_right(d, 1280, 340, 1360, MUTED, 4)
    cell(d, 1380, 300, 480, 80, note_free(res[1]), ROSE, 30)
    header(d, 60, 500, "THE FLEET, WORKED OUT " + head_fleet, AMBER)
    total = int(fleet[0][1])
    after = int(fleet[1][1].split("= ")[-1])
    for i, (who, n) in enumerate(((fleet[0][0], total), (fleet[1][0], after))):
        y = 580 + i * 120
        label(d, 60, y + 40, who, TEXT, 30, True, "lm")
        w = int(1300 * n / total)
        d.rounded_rectangle([420, y, 420 + w, y + 80], radius=10, fill=ROSE)
        label(d, 440 + w, y + 40, str(n), ROSE, 36, True, "lm")
    fit_mono(d, 420, 830, fleet[1][1], AMBER, 32, 1440)
    caption(d, "Every copy fails at once; a week later, the slow updaters still do.", AMBER, 915)


def rotation_pins(img, d, cfg):
    """Three pin choices: what each ships, which key of the chain matches by hand, and the result."""
    pins = pairs(section("FOUR", "THE PINS"))
    checks = pairs(section("FOUR", "THE CHECK"))
    res = dict(pairs(section("FOUR", "THE RESULT")))
    header(d, 60, 200, "THE CASE", CYAN, 560)
    header(d, 650, 200, "BY HAND: WHICH KEY IS PINNED?", AMBER, 700)
    header(d, 1380, 200, "THE RESULT", GREEN, 480)
    for i, (case, check) in enumerate(checks):
        y = 270 + i * 120
        label(d, 60, y + 40, case, TEXT, 28, True, "lm")
        cell(d, 650, y, 700, 80, check, GREEN if check.startswith("yes") else ROSE, 26)
        v = res[case]
        cell(d, 1380, y, 480, 80, v, ok_colour(v), 26)
    label(d, 60, 770, "the pins:", MUTED, 26, True)
    for i, (who, what) in enumerate(pins):
        fit_mono(d, 220, 770 + i * 40, who + ": " + what, MUTED, 24, 1640)
    caption(d, "Pin what changes rarely, keep a spare, and test before enforcing.", AMBER, 925)


def hpkp_year(img, d, cfg):
    """The meddler's header, max-age worked out, and the visits on a timeline of days."""
    hdr = section("FIVE", "THE HEADER")
    work = section("FIVE", "THE max-age")[0]
    res = pairs(section("FIVE", "THE RESULT"))
    own = pairs(section("FIVE", "THE SITE'S OWN"))[0]
    header(d, 60, 200, "THE HEADER (one answer, on a first visit)", ROSE)
    for i, ln in enumerate(hdr):
        fit_mono(d, 60, 265 + i * 46, ln, TEXT, 28, 1800)
    header(d, 60, 380, "THE max-age, WORKED OUT", AMBER)
    m = re.match(r"(\d+) s / (\d+) s a day = (\d+) days", work)
    secs, day, days = m.groups()
    for j, (v, note) in enumerate(((secs, "seconds"), (day, "seconds a day"), (days, "days"))):
        x = 60 + j * 600
        cell(d, x, 440, 420, 80, v, AMBER, 38)
        label(d, x + 210, 530, note, MUTED, 24, True, "ma")
        if j < 2:
            label(d, x + 510, 480, "/" if j == 0 else "=", TEXT, 48, True, "mm")
    header(d, 60, 590, "THE GENUINE BANK SERVER, ON A TIMELINE OF DAYS", CYAN)
    x0, x1, y = 120, 1800, 720
    scale = (x1 - x0) / 420
    d.rounded_rectangle([x0, y - 8, x0 + int(int(days) * scale), y + 8], radius=8, fill=ROSE)
    d.line([x0 + int(int(days) * scale), y, x1, y], fill=GREEN, width=8)
    pts = [(0, "day 0: the header", MUTED, "la", True)]
    for (w, v), anchor in zip(res, ("la", "ra")):
        pts.append((int(w.split()[1]), w + ": " + note_free(v), ok_colour(v), anchor, False))
    pts.append((int(days), "day " + days + ": the pins expire", AMBER, "ra", True))
    for dd, text, c, anchor, up in pts:
        x = x0 + int(dd * scale)
        d.ellipse([x - 12, y - 12, x + 12, y + 12], fill=c)
        tx = x - 10 if anchor == "la" else x + 10
        d.text((tx, y - 56 if up else y + 26), text, font=font(SANS_B, 24), fill=c, anchor=anchor)
    fit_mono(d, 60, 830, "the site's own pins, then a new key, day " + own[0].split()[1] + ": " + note_free(own[1]), ROSE, 26, 1800)
    caption(d, "One answer could lock a site out for a year, so browsers removed it.", AMBER, 905)
