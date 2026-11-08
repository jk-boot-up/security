"""Pictures for the one-time codes with java-otp video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL,
                                   ROSE, SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
DARK = (15, 23, 42)


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("AFTER: THE CODES")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act's output."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE / AFTER: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def node(d, x, y, w, h, text, c, size=28, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def big_box(d, x, y, w, h, text, sub, colour, size=40):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (16 if sub else 0)), text, font=font(SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 26), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def cells(d, x, y, items, colour, w=72, h=60, size=28, mark=(), mark_colour=AMBER, gap=6):
    for i, t in enumerate(items):
        c = mark_colour if i in mark else colour
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=8, fill=BG_CELL, outline=c, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=c, anchor="mm")


def act_one_codes():
    """[(time, step, code)] from act ONE's result section."""
    return [(int(t), int(s), c) for t, s, c in
            re.findall(r"time (\d+)\s+step (\d+)\s+(\d{6})", section("ONE", "AFTER"))]


def steps_axis(d, y, codes, x0=160, step_w=400):
    """Four 30-second steps as boxes on a time line, each with its code."""
    for step, code in codes.items():
        x = x0 + step * step_w
        node(d, x + 6, y, step_w - 12, 70, code, CLIENT, 34)
        label(d, x + step_w // 2, y - 40, "step %d" % step, MUTED, 24, True, "ma")
    d.line([x0, y + 100, x0 + 4 * step_w, y + 100], fill=LINE, width=3)
    for k in range(5):
        x = x0 + k * step_w
        d.line([x, y + 90, x, y + 110], fill=LINE, width=3)
        label(d, x, y + 118, "%d s" % (k * 30), MUTED, 24, False, "ma")
    return lambda t: x0 + t * step_w / 30


# ------------------------------------------------------------------ figures

def phone_and_bank(img, d, cfg):
    """The core idea: phone and bank share a secret and a clock, so both make the same code."""
    key = value("key", "ONE").split()[0]
    code = next(c for t, s, c in act_one_codes() if t == 59)
    header(d, 100, 205, "BEFORE: WHAT BOTH SIDES HAVE", GREEN)
    header(d, 1240, 205, "AFTER: THE CODE", ROSE)
    for i, (who, c) in enumerate((("Dana's phone", CLIENT), ("the bank", AMBER))):
        y = 290 + i * 330
        label(d, 100, y, who, c, 32, True)
        node(d, 100, y + 50, 560, 60, "secret " + key, GREEN, 24)
        node(d, 100, y + 130, 560, 60, "clock: time 59", CYAN, 26)
        draw.arrow_right(d, 670, y + 80, 760, GREEN, 3)
        draw.arrow_right(d, 670, y + 160, 760, CYAN, 3)
        big_box(d, 770, y + 45, 330, 150, "TOTP", "secret + time", c)
        draw.arrow_right(d, 1110, y + 120, 1230, c, 4)
        node(d, 1240, y + 80, 300, 80, code, ROSE, 46)
    big_box(d, 1600, 430, 240, 190, "same?", "", GREEN, 40)
    draw.arrow(d, 1545, 410, 1620, 470, MUTED, 3)
    draw.arrow(d, 1545, 740, 1620, 580, MUTED, 3)
    label(d, 1720, 640, "✓ let Dana in", GREEN, 28, True, "ma")
    caption(d, "Nothing is sent but the six digits, and they change every 30 seconds.", AMBER, 915)


def timeline(img, d, cfg):
    """Act one: the library's codes for each 30-second step, against the standard."""
    rows = act_one_codes()
    key = value("key", "ONE").split()[0]
    e = header(d, 100, 205, "BEFORE: THE INPUTS", GREEN)
    label(d, e + 24, 227, "the RFC test key %s, and a time" % key, TEXT, 28, True, "lm")
    e = header(d, 100, 300, "AFTER: THE LIBRARY'S CODES", ROSE)
    label(d, e + 24, 322, "one code per 30-second step", TEXT, 28, True, "lm")
    x_of = steps_axis(d, 440, {s: c for t, s, c in rows if t != 59})
    t, s, c = next(r for r in rows if r[0] == 59)
    x = x_of(t)
    d.line([x, 415, x, 540], fill=AMBER, width=4)
    label(d, x, 610, "time %d: still step %d, so %s" % (t, s, c), AMBER, 30, True, "ma")
    label(d, W // 2, 760, "Every time inside one step gives the same code.", TEXT, 32, False, "ma")
    caption(d, "These are the published values of RFC 6238: the library matches.", AMBER, 860)


def code_steps(img, d, cfg):
    """Act two: how 287082 is made from the key and time 59."""
    step = value("step", "TWO")
    counter = value("counter", "TWO").replace(" ", "")
    hmac = value("hmac", "TWO").replace(" ", "")
    offset = int(re.search(r"offset (\d+)", value("last", "TWO")).group(1))
    four = value("4 bytes", "TWO")
    number, code = value("number", "TWO"), value("code", "TWO")
    hb = [hmac[i:i + 2] for i in range(0, len(hmac), 2)]

    e = header(d, 100, 200, "STEP 1", GREEN)
    label(d, e + 20, 222, "59 / 30 = %s, written as 8 bytes" % step, TEXT, 28, True, "lm")
    cells(d, 900, 196, [counter[i:i + 2] for i in range(0, 16, 2)], GREEN, w=60, h=52, size=24, mark={7})

    e = header(d, 100, 290, "STEP 2", CYAN)
    label(d, e + 20, 312, "HMAC-SHA1 of the counter, with the secret key: 20 bytes", TEXT, 28, True, "lm")
    marks = set(range(offset, offset + 4))
    cells(d, 100, 360, hb, CYAN, w=78, h=60, size=26, mark=marks | {19}, gap=8)
    for i in range(20):
        label(d, 100 + i * 86 + 39, 428, str(i), MUTED, 18, False, "ma")

    e = header(d, 100, 480, "STEP 3", AMBER)
    label(d, e + 20, 502, "last byte %s: its last half-byte %s = %d, so start at byte %d"
          % (hb[19], hb[19][1], offset, offset), TEXT, 28, True, "lm")
    node(d, 100, 545, 380, 64, "4 bytes " + four, AMBER, 28)

    e = header(d, 100, 640, "STEP 4", ROSE)
    label(d, e + 20, 662, "drop the top bit, read it as a number, keep the last 6 digits", TEXT, 28, True, "lm")
    node(d, 100, 705, 360, 70, four, AMBER, 32)
    draw.arrow_right(d, 470, 740, 560, MUTED, 3)
    node(d, 570, 705, 420, 70, number, TEXT, 36)
    draw.arrow_right(d, 1000, 740, 1090, MUTED, 3)
    node(d, 1100, 695, 300, 90, code, ROSE, 48)
    label(d, 1250, 795, "the code", ROSE, 26, True, "ma")
    caption(d, "Exactly the library's code. Without the secret key, nobody can make the HMAC.", AMBER, 880)


def window(img, d, cfg):
    """Act three: the library accepts one step; the bank's window accepts one either side."""
    lib = re.findall(r"(\d{6}) at time (\d+), step (\d+): (\w+)", section("THREE", "BEFORE"))
    bank = re.findall(r"(\d{6}) at time (\d+), step (\d+): (\w+)", section("THREE", "AFTER"))
    code = lib[0][0]
    x0, sw = 520, 330

    def row(y, title, colour, ok_steps, checks):
        header(d, 100, y, title, colour)
        for s in range(4):
            c = GREEN if s in ok_steps else ROSE
            node(d, x0 + s * sw + 6, y + 70, sw - 12, 60, "step %d  %s" % (s, "accept" if s in ok_steps else "refuse"),
                 c, 26, False)
        for i, (t, s, res) in enumerate(sorted(checks, key=lambda r: int(r[0]))):
            ok = res in ("true", "OK")
            x = x0 + int(t) * sw / 30
            c = GREEN if ok else ROSE
            d.line([x, y + 132, x, y + 145 + i * 34], fill=c, width=4)
            label(d, x - 10, y + 138 + i * 34, "time %s: %s" % (t, res), c, 24, True, "ra")

    label(d, 100, 215, "code %s, made at time 59 in step 1" % code, TEXT, 30, True)
    row(280, "BEFORE: THE LIBRARY ALONE", AMBER, {1}, [(t, s, r) for _, t, s, r in lib])
    row(560, "AFTER: THE BANK'S WINDOW OF 1", GREEN, {0, 1, 2}, [(t, s, r) for _, t, s, r in bank])
    for k in range(5):
        label(d, x0 + k * sw, 840, "%d s" % (k * 30), MUTED, 22, False, "ma")
    caption(d, "Phone clocks drift, so the bank accepts one step either side, and no more.", AMBER, 905)


def hashes(img, d, cfg):
    """Act four: three hashes, longer keys, eight digits."""
    before = re.findall(r"(SHA-\d+)\s+(\d+)-byte key", section("FOUR", "BEFORE"))
    after = dict(re.findall(r"(SHA-\d+)\s+(\d{8})", section("FOUR", "AFTER")))
    header(d, 100, 205, "BEFORE: THE SETTINGS, AT TIME 59", GREEN)
    header(d, 1300, 205, "AFTER: THE CODES", ROSE)
    colours = [CLIENT, CYAN, AMBER]
    for i, ((name, n), c) in enumerate(zip(before, colours)):
        y = 290 + i * 170
        label(d, 100, y, name, c, 34, True)
        label(d, 330, y + 6, "%s-byte key, 8 digits" % n, MUTED, 26)
        d.rounded_rectangle([100, y + 55, 100 + int(n) * 15, y + 105], radius=8, fill=BG_CELL, outline=c, width=3)
        draw.arrow_right(d, 1100, y + 80, 1290, c, 3)
        node(d, 1300, y + 45, 380, 76, after[name], ROSE, 40)
    caption(d, "All three match RFC 6238. But many authenticator apps take only SHA-1, 6 digits, 30 s.", AMBER, 830)
    label(d, W // 2, 890, "Change the settings only if the bank controls the app.", MUTED, 30, False, "ma")


def otpauth(img, d, cfg):
    """Act five: what the bank puts in, and the text the QR code carries."""
    after = section("FIVE", "AFTER")
    parts = [p.strip() for p in after.splitlines() if p.strip()]
    ins = [("issuer", value("issuer", "FIVE"), CLIENT), ("account", value("account", "FIVE"), CYAN),
           ("secret", value("secret", "FIVE"), GREEN), ("settings", value("settings", "FIVE"), AMBER)]
    header(d, 100, 205, "BEFORE: WHAT THE BANK PUTS IN", GREEN)
    for i, (name, v, c) in enumerate(ins):
        y = 275 + i * 120
        label(d, 100, y, name, c, 26, True)
        node(d, 100, y + 36, 640, 60, v, c, 24)
    draw.arrow_right(d, 760, 520, 860, MUTED, 4)
    header(d, 880, 205, "AFTER: THE TEXT IN THE QR CODE", ROSE)
    pc = [CYAN, GREEN, CLIENT, AMBER]
    for i, (p, c) in enumerate(zip(parts, pc)):
        y = 280 + i * 82
        node(d, 880 if i == 0 else 940, y, 960 if i == 0 else 700, 64, p, c, 24)
    caption(d, "Base32: 20 bytes = 160 bits = 32 letters of 5 bits each.", AMBER, 820)
    label(d, W // 2, 880, "The app reads it once and keeps the secret: show it once, in private.", MUTED, 30, False, "ma")


def replay(img, d, cfg):
    """Act six: the same code twice, before and after the bank remembers the last step."""
    rows = {}
    for part in ("BEFORE THE FIX", "AFTER THE FIX"):
        rows[part] = re.findall(r"login at time (\d+) with (\d{6}): (\w+)", section("SIX", part))
    for k, (part, colour, sub) in enumerate((("BEFORE THE FIX", ROSE, "the library alone"),
                                             ("AFTER THE FIX", GREEN, "the bank remembers the last step"))):
        y = 205 + k * 320
        e = header(d, 100, y, part, colour)
        label(d, e + 24, y + 22, sub, TEXT, 28, True, "lm")
        for i, (t, code, res) in enumerate(rows[part]):
            ry = y + 75 + i * 110
            ok = res == "OK"
            c = GREEN if ok else ROSE
            label(d, 130, ry + 40, "login %d" % (i + 1), MUTED, 28, True, "lm")
            node(d, 290, ry + 6, 420, 68, "time %s, code %s" % (t, code), TEXT, 26)
            draw.arrow_right(d, 725, ry + 40, 840, MUTED, 3)
            d.text((880, ry + 40), "✓" if ok else "✗", font=font(SANS_B, 56), fill=c, anchor="mm")
            d.text((930, ry + 40), res, font=font(MONO_B, 34), fill=c, anchor="lm")
            if k == 0 and i == 1:
                label(d, 1150, ry + 40, "the same code used twice", ROSE, 28, True, "lm")
    caption(d, "Also the bank's: secret storage, wrong-code counts, attempt limits.", AMBER, 880)
