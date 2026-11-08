"""Pictures for the one-time codes video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("BEFORE ENCODING")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
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


def node(d, x, y, w, h, text, c, size=30, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def cells(d, x, y, items, colour, w=72, h=60, size=28, mark=(), mark_colour=AMBER, gap=6):
    """A row of cells; `mark` holds the indexes to highlight."""
    for i, t in enumerate(items):
        c = mark_colour if i in mark else colour
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=8, fill=BG_CELL, outline=c, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=c, anchor="mm")
    return x + len(items) * (w + gap) - gap


def panel(d, x, y, w, h, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=3)


def pairs(text):
    h = text.replace(" ", "")
    return [h[i:i + 2] for i in range(0, len(h), 2)]


# ------------------------------------------------------------------ figures

def one_code(img, d, cfg):
    """The core idea: the phone and the bank make the same code from the secret and the time."""
    secret = value("text", "TWO", "BEFORE ENCODING")
    code = value("code", "THREE", "AFTER").split(",")[0]
    for text, y, colour in (("BEFORE: WHAT EACH SIDE HAS", 215, GREEN), ("AFTER: WHAT EACH SIDE WORKS OUT", 680, ROSE)):
        header(d, W // 2 - (int(font(SANS_B, 26).getlength(text)) + 44) // 2, y, text, colour)
    for x, who, colour in ((100, "Dana's phone", CLIENT), (1100, "The bank", CYAN)):
        panel(d, x, 280, 720, 330, colour)
        label(d, x + 360, 300, who, colour, 38, True, "ma")
        label(d, x + 40, 380, "secret", MUTED, 28)
        node(d, x + 180, 365, 500, 64, secret, GREEN, 30)
        label(d, x + 40, 480, "time", MUTED, 28)
        node(d, x + 180, 465, 500, 64, "59 seconds", GREEN, 30)
        label(d, x + 360, 555, "the same recipe: HMAC, then 6 digits", MUTED, 26, False, "ma")
        draw.arrow(d, x + 360, 612, x + 360, 700, colour, 4)
        node(d, x + 160, 705, 400, 100, code, ROSE, 64)
    label(d, W // 2, 445, "no message", MUTED, 26, True, "ma")
    label(d, W // 2, 480, "between them", MUTED, 26, True, "ma")
    caption(d, "Same secret + same time  →  the same code, on both sides", AMBER, 850)
    label(d, W // 2, 905, "The secret is shared once. After that, only the clocks need to agree.", MUTED, 30, False, "ma")


def password_alone(img, d, cfg):
    """One factor: the leaked password is all an attacker needs."""
    password = value("password", "ONE", "THE LOGIN").split(",")[0]
    answer = value("attacker", "ONE", "THE ANSWER").split()[0]
    header(d, 100, 220, "THE LOGIN", GREEN)
    panel(d, 100, 290, 760, 300, GREEN)
    for i, (k, v, c) in enumerate((("user", "dana", TEXT), ("password", password, AMBER), ("code", "none", MUTED))):
        label(d, 140, 330 + i * 85, k, MUTED, 30)
        node(d, 360, 315 + i * 85, 460, 62, v, c, 30)
    label(d, 480, 610, "the password leaked from another site", AMBER, 28, False, "ma")
    draw.arrow_right(d, 880, 440, 1080, MUTED, 4)
    label(d, 980, 400, "attacker", ROSE, 28, True, "ma")
    header(d, 1100, 220, "THE ANSWER", ROSE)
    panel(d, 1100, 290, 720, 300, ROSE)
    label(d, 1460, 330, "the bank checks the password only", MUTED, 28, False, "ma")
    node(d, 1310, 400, 300, 120, answer, ROSE, 72)
    caption(d, "One factor: one leak is enough.", AMBER, 760)
    label(d, W // 2, 820, "A second factor adds something the attacker does not have: Dana's phone.", MUTED, 30, False, "ma")


def base32(img, d, cfg):
    """The secret's bytes become Base32 letters: 5 bytes of 8 bits = 40 bits = 8 letters of 5 bits."""
    text = value("text", "TWO", "BEFORE ENCODING")
    b32 = value("Base32", "TWO", "AFTER ENCODING").split()
    first = text[:5]
    bits = "".join("%08d" % int(bin(ord(c))[2:]) for c in first)
    groups = [bits[i:i + 5] for i in range(0, 40, 5)]
    alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    letters = [alphabet[int(g, 2)] for g in groups]
    assert "".join(letters) == b32[0], "the picture's Base32 must match the demo's"
    e = header(d, 100, 205, "BEFORE ENCODING", GREEN)
    label(d, e + 20, 227, "the secret, 20 bytes; here its first 5", TEXT, 28, True, "lm")
    for i, c in enumerate(first):
        x = 100 + i * 340
        node(d, x, 270, 320, 56, "'%s' = %02x" % (c, ord(c)), GREEN, 26)
        label(d, x + 160, 336, bits[i * 8:i * 8 + 8], MUTED, 26, False, "ma")
    draw.arrow(d, 960, 380, 960, 425, CYAN, 4)
    label(d, 990, 390, "cut the 40 bits into 8 groups of 5", CYAN, 26, True)
    e = header(d, 100, 440, "AFTER ENCODING", ROSE)
    label(d, e + 20, 462, "each group of 5 bits is one of 32 letters", TEXT, 28, True, "lm")
    for i, (g, ch) in enumerate(zip(groups, letters)):
        x = 100 + i * 212
        label(d, x + 96, 510, g, MUTED, 26, False, "ma")
        node(d, x, 548, 192, 64, ch, ROSE, 36)
    label(d, 100, 640, "all 32 letters:  " + " ".join(b32), TEXT, 30, True)
    header(d, 100, 715, "THE CODES IT GIVES, ONE PER 30-SECOND STEP", AMBER)
    rows = re.findall(r"step (\d)\s+time\s+(\d+) to\s+(\d+)\s+(\d{6})", section("TWO", "THE CODES"))
    for i, (step, t0, t1, code) in enumerate(rows):
        x = 100 + i * 435
        node(d, x, 780, 415, 70, code, AMBER, 36)
        label(d, x + 207, 862, "step %s: time %s to %s" % (step, t0, t1), MUTED, 26, False, "ma")
    caption(d, "The app gets the letters once, by QR code or typing. The codes change every step.", AMBER, 925)


def code_steps(img, d, cfg):
    """How the six digits are made from the secret and the time."""
    counter = value("counter", "THREE", "STEP 1")
    hmac = pairs(re.search(r"^      ([0-9a-f ]+)$", section("THREE", "STEP 2"), re.M).group(1))
    offset = int(re.search(r"= (\d+)", value("last byte", "THREE", "STEP 3")).group(1))
    picked = pairs(re.search(r"bytes \d+-\d+\s+(\w+)", section("THREE", "STEP 3")).group(1))
    number = value("number", "THREE", "STEP 4")
    code = value("last 6", "THREE", "STEP 4")
    assert hmac[offset:offset + 4] == picked

    e = header(d, 100, 205, "BEFORE", GREEN)
    label(d, e + 20, 227, "secret 12345678901234567890,  time 59", TEXT, 28, True, "lm")
    e = header(d, 100, 270, "STEP 1", CLIENT)
    label(d, e + 20, 292, "counter = 59 / 30 = " + counter.replace(" = ", ",  as 8 bytes:  "), TEXT, 28, True, "lm")
    e = header(d, 100, 335, "STEP 2", CYAN)
    label(d, e + 20, 357, "HMAC-SHA1 of the counter, with the secret: 20 bytes", TEXT, 28, True, "lm")
    mark = set(range(offset, offset + 4))
    cells(d, 100, 400, hmac, CYAN, w=80, mark=mark | {19}, mark_colour=AMBER)
    for i in range(20):
        label(d, 100 + i * 86 + 40, 466, str(i), MUTED, 20, False, "ma")
    lx = 100 + 19 * 86
    e = header(d, 100, 515, "STEP 3", AMBER)
    label(d, e + 20, 537, "the last byte is %s; its last half-byte, %s, is %d: read 4 bytes from byte %d"
          % (hmac[19], hmac[19][1], offset, offset), TEXT, 28, True, "lm")
    d.line([(lx + 40, 495), (lx + 40, 505), (100 + offset * 86 + 40, 505), (100 + offset * 86 + 40, 495)], fill=AMBER, width=3)
    e = header(d, 100, 600, "STEP 4", ROSE)
    label(d, e + 20, 622, "drop the top bit (never negative), keep the last 6 digits", TEXT, 28, True, "lm")
    x = cells(d, 100, 670, picked, AMBER, w=80)
    draw.arrow_right(d, x + 20, 700, x + 110, MUTED, 3)
    node(d, x + 120, 670, 330, 60, number, TEXT, 32)
    draw.arrow_right(d, x + 470, 700, x + 560, MUTED, 3)
    label(d, x + 515, 735, "last 6", MUTED, 22, False, "ma")
    header(d, x + 570, 745, "AFTER: THE CODE", ROSE)
    node(d, x + 570, 660, 330, 80, code, ROSE, 50)
    caption(d, "A new time step gives a new counter, a new HMAC, and new digits.", AMBER, 800)
    label(d, W // 2, 855, "Without the secret, nobody can work out the HMAC, so nobody can make the code.", MUTED, 30, False, "ma")


def clock_window(img, d, cfg):
    """The bank accepts its own step and one either side; a phone too far out is refused."""
    codes = re.findall(r"time\s+\d+ to\s+\d+\s+(\d{6})", section("TWO", "THE CODES"))
    attempts = dict(re.findall(r"^    ([a-e])\s+(.+)$", section("FOUR", "THE ATTEMPTS"), re.M))
    answers = dict(re.findall(r"^    ([a-e])\s+(\S+)", section("FOUR", "THE ANSWERS"), re.M))
    fast70 = attempts["e"].split()[0]
    codes = codes + [fast70]        # step 4's code, as the phone 70 seconds fast shows it
    header(d, 100, 205, "THE BANK'S CLOCK: TIME 59, STEP 1", GREEN)
    x0, sw = 160, 320
    d.rounded_rectangle([x0 - 10, 270, x0 + 3 * sw - 10, 470], radius=18, fill=(20, 52, 48), outline=GREEN, width=3)
    label(d, x0 + int(1.5 * sw) - 10, 280, "accepted: steps 0 to 2 (one step either side)", GREEN, 26, True, "ma")
    for i, c in enumerate(codes):
        x = x0 + i * sw
        node(d, x, 330, sw - 20, 70, c, GREEN if i < 3 else MUTED, 34)
        label(d, x + (sw - 20) // 2, 412, "step %d: %d to %d s" % (i, i * 30, i * 30 + 29), MUTED, 24, False, "ma")
    header(d, 100, 510, "THE ATTEMPTS", CLIENT)
    header(d, 1120, 510, "THE ANSWERS", ROSE)
    rows = [("b", "on time", 1), ("d", "25 seconds fast", 2), ("e", "70 seconds fast", 4)]
    for n, (k, what, step) in enumerate(rows):
        y = 580 + n * 100
        code = attempts[k].split()[0]
        label(d, 100, y + 18, "phone %s shows %s  (step %d)" % (what, code, step), TEXT, 30, True)
        ok = answers[k] == "OK"
        node(d, 1120, y, 380, 66, answers[k], GREEN if ok else ROSE, 32)
    caption(d, "A window forgives small clock errors. A wider one gives an attacker more codes to try.", AMBER, 905)


def replay(img, d, cfg):
    """The same code twice: refused only if the bank remembers the step it accepted."""
    code = value("dana", "FIVE", "THE LOGINS").split()[0]
    without = section("FIVE", "WITHOUT")
    with_ = section("FIVE", "WITH REPLAY")
    get = lambda txt, who: re.search(r"%s\s+(\S+)" % who, txt).group(1)
    header(d, 100, 205, "THE LOGINS", GREEN)
    node(d, 100, 270, 820, 70, "dana       %s at time 59" % code, CLIENT, 30)
    node(d, 1000, 270, 820, 70, "observer   %s at time 64" % code, ROSE, 30)
    label(d, 1410, 350, "saw Dana type it, 5 seconds earlier", MUTED, 24, False, "ma")
    for n, (title, txt, memory, colour) in enumerate((("WITHOUT REPLAY PROTECTION", without, "remembers nothing", ROSE),
                                                      ("WITH REPLAY PROTECTION", with_, "remembers: last step = 1", GREEN))):
        y = 420 + n * 230
        header(d, 100, y, title, colour)
        label(d, 100, y + 64, "the bank " + memory, MUTED, 28)
        for i, who in enumerate(("dana", "observer")):
            v = get(txt, who)
            c = GREEN if v == "OK" and who == "dana" else (ROSE if v == "OK" else AMBER)
            label(d, 1000 + i * 460, y + 20, who, MUTED, 26, False, "ma")
            node(d, 820 + i * 460, y + 60, 360, 70, v, c, 34)
    caption(d, "Step 1 was used once, so a second code from step 1 is refused: a one-time code.", AMBER, 900)


def bill(img, d, cfg):
    """Three things a code cannot do."""
    code = value("code", "SIX", "A FAKE BANK PAGE")
    login = value("login", "SIX", "THE REAL BANK").split()[0]
    secret = value("secret", "SIX", "A STOLEN SECRETS TABLE")
    stolen_code = value("code", "SIX", "THE ATTACKER'S OWN APP").split()[0]
    answers = section("SIX", "THE BANK'S ANSWERS")
    first = re.search(r"first\s+(\S+)", answers).group(1)
    second = re.search(r"second\s+(\S+)", answers).group(1)
    cols = (("1. A FAKE BANK PAGE", AMBER), ("2. A STOLEN TABLE", AMBER), ("3. A LOST PHONE", AMBER))
    for i, (t, c) in enumerate(cols):
        panel(d, 100 + i * 580, 210, 560, 620, c)
        label(d, 380 + i * 580, 230, t, c, 32, True, "ma")
    # 1: the fake page passes the code on
    header(d, 130, 300, "BEFORE: DANA TYPES", GREEN)
    node(d, 130, 360, 500, 60, "midnight7 + " + code, GREEN, 28)
    label(d, 380, 440, "into the fake page, which", MUTED, 26, False, "ma")
    label(d, 380, 475, "passes both on at time 61", MUTED, 26, False, "ma")
    header(d, 130, 540, "AFTER: THE REAL BANK", ROSE)
    node(d, 230, 600, 300, 80, login, ROSE, 50)
    label(d, 380, 720, "it cannot tell", TEXT, 28, True, "ma")
    label(d, 380, 760, "the fake page", TEXT, 28, True, "ma")
    # 2: the secret lives on the server too
    header(d, 710, 300, "BEFORE: THE TABLE HOLDS", GREEN)
    node(d, 710, 360, 500, 60, secret[:16] + "…", GREEN, 28)
    label(d, 960, 440, "Dana's secret, which the", MUTED, 26, False, "ma")
    label(d, 960, 475, "bank needs to check codes", MUTED, 26, False, "ma")
    header(d, 710, 540, "AFTER: ATTACKER'S APP", ROSE)
    node(d, 810, 600, 300, 80, stolen_code, ROSE, 50)
    label(d, 960, 720, "the same code", TEXT, 28, True, "ma")
    label(d, 960, 760, "as Dana's phone", TEXT, 28, True, "ma")
    # 3: recovery codes
    header(d, 1290, 300, "BEFORE: A RECOVERY CODE", GREEN)
    node(d, 1290, 360, 500, 60, value("first", "SIX", "A LOST PHONE"), GREEN, 28)
    label(d, 1540, 440, "typed once, then", MUTED, 26, False, "ma")
    label(d, 1540, 475, "typed again", MUTED, 26, False, "ma")
    header(d, 1290, 540, "AFTER: THE BANK", ROSE)
    node(d, 1290, 600, 240, 80, first, GREEN, 36)
    node(d, 1550, 600, 240, 80, second, ROSE, 26)
    label(d, 1540, 720, "a second way in:", TEXT, 28, True, "ma")
    label(d, 1540, 760, "guard it like a password", TEXT, 28, True, "ma")
    caption(d, "Against a fake page, only a hardware security key helps: it checks the site.", AMBER, 875)
