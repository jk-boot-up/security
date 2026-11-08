"""Pictures for the rate limiting with nginx video, drawn by the `figure` slide kind.

Every count shown is read from docs/expected-output.txt, which the real nginx produced and the
tests check, and every header value, address and nginx variable is read from the Java source,
so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

ROOT = Path(__file__).resolve().parent.parent
OUT = (ROOT / "docs" / "expected-output.txt").read_text()
SRC = ROOT / "src" / "main" / "java" / "com" / "jk" / "explore" / "security" / "attacksnginx"
DEMO = (SRC / "NginxAttacksDemo.java").read_text()
CONFIG = (SRC / "NginxConfig.java").read_text()
BG_CELL = (22, 32, 54)
DARK = (15, 23, 42)


# ------------------------------------------------------------------ values

def act(number):
    """One act of the output, by its number word ("TWO"), header line included."""
    return re.search(r"^%s\. .*\n(?:  .*\n?)*" % number, OUT, re.M).group(0)


def counts(number, n=0):
    """The n-th 'A answered, R refused' of an act, as two ints."""
    found = re.findall(r"(\d+) answered, (\d+) refused", act(number))[n]
    return int(found[0]), int(found[1])


def guesses(number):
    return int(re.search(r"^  (\d+) guesses", act(number), re.M).group(1))


def rate_and_burst():
    m = re.search(r"limit_req on the client's address: (\d+)r/m, burst (\d+)", OUT)
    return int(m.group(1)), int(m.group(2))


def variable(key):
    """The nginx variable a NginxConfig.Key counts on, e.g. ADDRESS -> $binary_remote_addr."""
    return re.search(r'%s\("(\$\w+)"\)' % key, CONFIG).group(1)


def forwarded(i):
    """The X-Forwarded-For value the attacker sends with guess i (0-based), as the demo writes it."""
    return re.search(r'i -> "([\d.]+)" \+ i', DEMO).group(1) + str(i)


def owner_address():
    return re.search(r'attempt\("dana", "([\d.]+)"\)', DEMO).group(1)


def spray_user(i):
    return re.search(r'i -> "(\w+)" \+ i, i -> null', DEMO).group(1) + str(i)


def excess_steps(n, burst):
    """nginx's rule for n guesses at one key within a second (as LeakyBucket does it): the count after each
    guess, and whether it was let through. A drain of 1 a minute is too small to matter here."""
    out, excess = [], None
    for _ in range(n):
        now = 0 if excess is None else excess + 1
        if now > burst:
            out.append((now, False))
        else:
            excess = now
            out.append((now, True))
    return out


# ------------------------------------------------------------------ drawing helpers

def header(d, x, y, text, colour, w=None, note=None):
    """A section label such as THE REQUESTS: a filled pill with dark text, and a note beside it."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    if note:
        d.text((x + w + 24, y + 22), note, font=font(SANS_B, 30), fill=TEXT, anchor="lm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la", mono=False):
    face = (MONO_B if mono else (SANS_B if bold else SANS))
    d.text((x, y), text, font=font(face, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cells(d, x, y, items, colours, w=76, h=56, size=24, gap=8, per_row=20, row_gap=10):
    """A grid of small cells; `colours` is one colour or one per item."""
    for i, t in enumerate(items):
        c = colours[i] if isinstance(colours, list) else colours
        cx = x + (i % per_row) * (w + gap)
        cy = y + (i // per_row) * (h + row_gap)
        d.rounded_rectangle([cx, cy, cx + w, cy + h], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((cx + w // 2, cy + h // 2), t, font=font(MONO_B, size), fill=c, anchor="mm")


def box(d, x, y, w, h, title, sub, colour, size=32):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + (h // 2 - 18 if sub else h // 2)), title, font=font(MONO_B, size), fill=colour,
           anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 24), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


def tally(d, x, y, answered, refused, w=380):
    """Two pills: how many were answered by the login (401) and how many were refused (429)."""
    for i, (text, c) in enumerate((("%d answered: 401" % answered, GREEN), ("%d refused: 429" % refused, ROSE))):
        cx = x + i * (w + 30)
        d.rounded_rectangle([cx, y, cx + w, y + 60], radius=30, fill=PANEL, outline=c, width=3)
        d.text((cx + w // 2, y + 30), text, font=font(SANS_B, 30), fill=c, anchor="mm")


def answers(answered, refused):
    return ["401"] * answered + ["429"] * refused, [GREEN] * answered + [ROSE] * refused


# ------------------------------------------------------------------ figures

def key_choice(img, d, cfg):
    """The core idea on one real request: three things nginx could count, and who chooses each."""
    header(d, 100, 215, "THE REQUEST", GREEN, note="one guess at dana's login")
    d.rounded_rectangle([100, 285, 820, 545], radius=14, fill=BG_CELL, outline=GREEN, width=3)
    for i, (t, c) in enumerate((("POST /login?user=dana", GREEN), ("X-Forwarded-For: " + forwarded(7), ROSE),
                                ("password=guess", MUTED))):
        label(d, 130, 315 + i * 62, t, c, 30, mono=True)
    label(d, 130, 500, "sent over a connection from one address", MUTED, 24)
    rows = [(variable("ADDRESS"), "the connection's address", "the network decides", GREEN),
            (variable("FORWARDED_HEADER"), forwarded(7), "the client writes it", ROSE),
            (variable("USERNAME"), "dana", "the client names an account", AMBER)]
    label(d, 900, 237, "three keys nginx could count", TEXT, 30, True, "lm")
    for i, (var, val, who, c) in enumerate(rows):
        y = 285 + i * 90
        draw.arrow_right(d, 830, y + 36, 892, c, 3)
        d.rounded_rectangle([900, y, 1820, y + 72], radius=12, fill=PANEL, outline=c, width=3)
        label(d, 925, y + 36, var, c, 28, anchor="lm", mono=True)
        label(d, 1430, y + 22, val, TEXT, 24, True, "lm")
        label(d, 1430, y + 52, who, MUTED, 22, anchor="lm")
    header(d, 100, 610, "THE RESPONSE", ROSE, note="nginx keeps one count for each value of the key")
    for i, (code, text, c) in enumerate((("401", "room left: passed to the login, which says wrong password", GREEN),
                                         ("429", "no room left: refused by nginx, too many requests", ROSE))):
        y = 690 + i * 86
        d.rounded_rectangle([100, y, 1820, y + 70], radius=35, fill=PANEL, outline=c, width=3)
        label(d, 170, y + 35, code, c, 34, anchor="mm", mono=True)
        label(d, 240, y + 35, text, TEXT, 28, anchor="lm")
    caption(d, "The same limit stops the attacker, or not, depending on what it counts", AMBER, 900)


def no_limit(img, d, cfg):
    n = guesses("ONE")
    answered, refused = counts("ONE")
    header(d, 100, 215, "THE REQUESTS", GREEN, note="%d guesses at dana's login, one straight after another" % n)
    cells(d, 124, 285, [str(i + 1) for i in range(n)], CLIENT)
    draw.arrow(d, 960, 350, 960, 400, MUTED, 4)
    box(d, 560, 405, 800, 100, "nginx: no limit_req", "every request is passed to the login", MUTED)
    draw.arrow(d, 960, 510, 960, 560, MUTED, 4)
    header(d, 100, 570, "THE RESPONSES", ROSE, note="what came back for each guess")
    a, c = answers(answered, refused)
    cells(d, 124, 640, a, c)
    tally(d, 560, 740, answered, refused)
    caption(d, "Every guess reaches the password check: anyone can keep guessing", AMBER, 880)


def bucket_steps(img, d, cfg):
    """How limit_req decides, guess by guess: the count for the key, then the answer."""
    n = guesses("TWO")
    rate, burst = rate_and_burst()
    answered, refused = counts("TWO")
    steps = excess_steps(n, burst)
    if sum(ok for _, ok in steps) != answered:          # the rule must give what the real nginx gave
        raise ValueError("the rule gives %d, nginx gave %d" % (sum(ok for _, ok in steps), answered))
    header(d, 100, 210, "THE REQUESTS", GREEN, note="%d guesses, all from one address: one key" % n)
    cells(d, 124, 270, [str(i + 1) for i in range(n)], CLIENT)
    label(d, 124, 345, "the count for that key, if the guess were let in:", MUTED, 26)
    cells(d, 124, 385, [str(e) if ok else "%d>%d" % (e, burst) for e, ok in steps],
          [AMBER if ok else ROSE for _, ok in steps], size=22)
    header(d, 100, 470, "THE RESPONSES", ROSE)
    a, c = answers(answered, refused)
    cells(d, 124, 530, a, c)
    rules = [("1", "A key seen for the first time starts at 0: guess 1 is let through.", GREEN),
             ("2", "Each guess adds 1. The rate, %d a minute, takes %d away per minute:" % (rate, rate)
              + " almost nothing in a second.", AMBER),
             ("3", "A guess that would take it over the burst of %d is refused: guess %d would make %d."
              % (burst, burst + 2, burst + 1), ROSE)]
    for i, (num, text, col) in enumerate(rules):
        y = 625 + i * 66
        d.ellipse([124, y, 172, y + 48], fill=col)
        label(d, 148, y + 24, num, DARK, 26, True, "mm")
        label(d, 196, y + 24, text, TEXT, 27, anchor="lm")
    caption(d, "1 + the burst of %d = %d answered, %d refused with 429" % (burst, answered, refused), AMBER, 860)


def header_key(img, d, cfg):
    n = guesses("THREE")
    answered, refused = counts("THREE")
    header(d, 100, 210, "THE REQUESTS", GREEN, note="%d guesses, each with its own X-Forwarded-For value" % n)
    cells(d, 124, 270, [forwarded(i) for i in range(n)], ROSE, w=160, size=22, per_row=10)
    box(d, 360, 420, 1200, 100, "key = " + variable("FORWARDED_HEADER"),
        "%d different values, so %d different counts, each at 0" % (n, n), ROSE)
    draw.arrow(d, 960, 525, 960, 565, MUTED, 4)
    header(d, 100, 570, "THE RESPONSES", ROSE)
    a, c = answers(answered, refused)
    cells(d, 124, 630, a, c, w=160, per_row=10)
    tally(d, 560, 790, answered, refused)
    caption(d, "The limit is in place, and does nothing: the attacker chose the key", AMBER, 890)


def real_address(img, d, cfg):
    n = guesses("FOUR")
    answered, refused = counts("FOUR")
    header(d, 100, 210, "THE REQUESTS", GREEN, note="the same %d different X-Forwarded-For values" % n)
    cells(d, 124, 270, [forwarded(i) for i in range(n)], MUTED, w=160, size=22, per_row=10)
    box(d, 360, 420, 1200, 100, "key = " + variable("ADDRESS"),
        "the header is not looked at: one address, so one count", GREEN)
    draw.arrow(d, 960, 525, 960, 565, MUTED, 4)
    header(d, 100, 570, "THE RESPONSES", ROSE)
    a, c = answers(answered, refused)
    cells(d, 124, 630, a, c, w=160, per_row=10)
    tally(d, 560, 790, answered, refused)
    caption(d, "Changing the header buys nothing: the client cannot choose its address", AMBER, 890)


def username_key(img, d, cfg):
    n = guesses("FIVE")
    on_dana = counts("FIVE", 0)
    spray = counts("FIVE", 1)
    owner = re.search(r"dana herself tries from another address: (\d+)", OUT).group(1)
    for x, title in ((100, "ON ONE ACCOUNT"), (990, "A SPRAY")):
        d.rounded_rectangle([x, 205, x + 830, 850], radius=18, fill=None, outline=PANEL, width=3)
        label(d, x + 415, 220, title, TEXT, 30, True, "ma")
    # left: the attack on dana, then dana herself
    header(d, 130, 275, "THE REQUESTS", GREEN)
    label(d, 130, 335, "%d guesses with ?user=dana" % n, TEXT, 28, True)
    label(d, 130, 375, "key = %s: one count, for dana" % variable("USERNAME"), AMBER, 24, mono=True)
    label(d, 130, 420, "then dana herself, from " + owner_address(), CLIENT, 28, True)
    header(d, 130, 490, "THE RESPONSES", ROSE)
    a, c = answers(*on_dana)
    cells(d, 130, 550, a, c, w=72, h=48, size=20, gap=7, per_row=10)
    label(d, 130, 680, "%d answered, %d refused" % on_dana, TEXT, 26, True)
    d.rounded_rectangle([130, 730, 800, 800], radius=35, fill=PANEL, outline=ROSE, width=3)
    label(d, 465, 765, "dana herself: %s  (locked out)" % owner, ROSE, 30, True, "mm")
    # right: one guess at each of many usernames
    header(d, 1020, 275, "THE REQUESTS", GREEN)
    label(d, 1020, 335, "one guess at each of %d usernames" % n, TEXT, 28, True)
    label(d, 1020, 375, "%s, %s ... %s" % (spray_user(0), spray_user(1), spray_user(n - 1)), AMBER, 24, mono=True)
    label(d, 1020, 420, "%d counts, each with only 1 in it" % n, CLIENT, 28, True)
    header(d, 1020, 490, "THE RESPONSES", ROSE)
    a, c = answers(*spray)
    cells(d, 1020, 550, a, c, w=72, h=48, size=20, gap=7, per_row=10)
    label(d, 1020, 680, "%d answered, %d refused" % spray, TEXT, 26, True)
    d.rounded_rectangle([1020, 730, 1790, 800], radius=35, fill=PANEL, outline=ROSE, width=3)
    label(d, 1405, 765, "never reaches the limit", ROSE, 30, True, "mm")
    caption(d, "A limit per username locks the owner out, and misses a spray", AMBER, 890)
