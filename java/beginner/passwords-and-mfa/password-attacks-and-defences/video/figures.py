"""Pictures for the password attacks and defences video, drawn by the `figure` slide kind.

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


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own title line."""
    return re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)


def part(name, head, n=0):
    """The indented lines under the n-th section header of an act that starts with `head`."""
    blocks = re.findall(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(head), act(name), re.M)
    return [ln.strip() for ln in blocks[n].splitlines()]


def head_line(name, head, n=0):
    """The text of the n-th section header line itself, after its label."""
    return re.findall(r"^  %s:? ?(.*)$" % re.escape(head), act(name), re.M)[n]


def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUEST: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cell(d, x, y, w, h, text, colour, size=28, mono=True, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="mm")


# ------------------------------------------------------------------ the tick-by-tick runs

RUN = re.compile(r"ticks? (\d+)(?:-(\d+))?\s+(no guess|guess(?:es)? [\d-]+)\s+(\w+)")
COLOUR = {"WRONG_PASSWORD": CLIENT, "LOCKED": AMBER, "RATE_LIMITED": AMBER, "OK": ROSE}


def runs(lines):
    out = []
    for ln in lines:
        m = RUN.match(ln)
        if m:
            a = int(m.group(1))
            out.append((a, int(m.group(2) or a), m.group(3), m.group(4)))
    return out


X0, X1, LAST = 300, 1800, 34          # ticks 0..33 across the slide


def tx(t):
    return X0 + (X1 - X0) * t / LAST


def timeline(d, y, name, rs, sub=None, notes=True):
    """One row of ticks: a bar for every run, coloured by the login's answer."""
    label(d, 100, y + 8, name, TEXT, 30, True)
    if sub:
        label(d, 100, y + 46, sub, MUTED, 22)
    for a, b, guessed, outcome in rs:
        c = COLOUR[outcome]
        x, x2 = tx(a), tx(b + 1) - 4
        d.rounded_rectangle([x, y, x2, y + 70], radius=8, fill=BG_CELL, outline=c, width=3)
        word = {"WRONG_PASSWORD": "wrong", "OK": "IN", "LOCKED": "locked", "RATE_LIMITED": "limited"}[outcome]
        size = 24 if x2 - x > 110 else 20
        d.text(((x + x2) / 2, y + 24), word, font=font(SANS_B, size), fill=c, anchor="mm")
        if notes and x2 - x > 110:
            d.text(((x + x2) / 2, y + 52), guessed.replace("guesses", "guess"), font=font(SANS, 20), fill=MUTED,
                   anchor="mm")


def tick_axis(d, y, marks):
    d.line([(X0, y), (X1, y)], fill=LINE, width=2)
    for t in marks:
        d.line([(tx(t), y - 8), (tx(t), y + 8)], fill=MUTED, width=2)
        d.text((tx(t), y + 14), str(t), font=font(SANS, 22), fill=MUTED, anchor="ma")
    label(d, X1, y + 44, "tick", MUTED, 22, False, "ra")


def legend(d, y):
    x = 300
    for text, c in (("a guess, wrong", CLIENT), ("refused: no guess used", AMBER), ("attacker gets in", ROSE)):
        d.rounded_rectangle([x, y, x + 34, y + 34], radius=6, fill=BG_CELL, outline=c, width=3)
        label(d, x + 48, y + 17, text, TEXT, 26, False, "lm")
        x += 120 + int(font(SANS, 26).getlength(text))


# ------------------------------------------------------------------ figures

def one_guess(img, d, cfg):
    """One login attempt: the request, the salted-hash check, and the response."""
    req = part("ONE", "THE REQUEST")[0]
    check = part("ONE", "THE CHECK")
    guess = re.search(r"guess hash\s+(\S+)", check[0]).group(1)
    stored = re.search(r"stored hash\s+(\S+)", check[1]).group(1)
    same = check[2].split()[-1]
    resp = part("ONE", "THE RESPONSE")[0]
    name = re.search(r"name (\S+)", req).group(1)
    pw = re.search(r"password (\S+)", req).group(1)
    sender = re.search(r"from (\S+)", req).group(1)

    header(d, 100, 210, "THE REQUEST", GREEN)
    for i, (k, v) in enumerate((("from", sender), ("name", name), ("password", pw))):
        y = 275 + i * 72
        label(d, 100, y + 28, k, MUTED, 26, False, "lm")
        cell(d, 250, y, 300, 58, v, GREEN, 28)
    draw.arrow_right(d, 570, 380, 660, MUTED, 4)

    header(d, 680, 210, "THE CHECK", AMBER)
    label(d, 680, 278, 'SHA-256 of "demo-salt:%s:" + password' % name, TEXT, 28, True)
    rows = (("hash of the guess", pw, guess, CLIENT), ("hash stored at sign-up", "midnight7", stored, CYAN))
    for i, (k, p, h, c) in enumerate(rows):
        y = 330 + i * 120
        label(d, 680, y, "%s  (%s)" % (k, p), c, 26, True)
        cell(d, 680, y + 38, 760, 64, h, c, 28)
    cell(d, 1470, 400, 330, 120, "the same? %s" % same, ROSE if same == "false" else GREEN, 30, False)
    draw.arrow(d, 1635, 525, 1635, 600, MUTED, 4)

    header(d, 1360, 610, "THE RESPONSE", ROSE)
    outcome, msg = resp.split(None, 1)
    cell(d, 1180, 670, 620, 64, outcome, ROSE, 30)
    label(d, 1490, 760, msg, TEXT, 28, False, "ma")

    label(d, 100, 640, "Before the check, two defences may refuse", TEXT, 28, True)
    label(d, 100, 685, "the attempt without looking at the password:", TEXT, 28, True)
    cell(d, 100, 740, 440, 64, "rate limit: per address", AMBER, 26, False)
    cell(d, 560, 740, 440, 64, "lock: per account", AMBER, 26, False)
    caption(d, "Every guess costs the attacker one attempt. Defences decide how many attempts they get.", AMBER, 880)


def dict_vs_spray(img, d, cfg):
    """A dictionary attack goes deep on one account; a spray goes wide over all of them."""
    dic = runs(part("ONE", "THE DICTIONARY"))
    accounts = part("ONE", "THE SPRAY")[0].split()[1:]
    spray = []
    for ln in part("ONE", "THE SPRAY")[1:]:
        m = re.match(r"(\S+)\s+(\d+) tries\s+taken: (.+)$", ln)
        spray.append((m.group(1), int(m.group(2)), [] if m.group(3) == "nobody" else m.group(3).split(", ")))
    result = head_line("ONE", "THE RESULT", 1)
    most = part("ONE", "THE RESULT", 1)[0].split()[-1]

    header(d, 100, 210, "THE DICTIONARY: one account, many words", GREEN)
    wrong = dic[0][1] - dic[0][0] + 1
    for i in range(wrong + 1):
        c = ROSE if i == wrong else CLIENT
        x, y = 100 + (i % 7) * 92, 280 + (i // 7) * 92
        cell(d, x, y, 80, 80, "IN" if i == wrong else str(i + 1), c, 26)
    label(d, 100, 480, "dana: %d wrong, then guess %d is right" % (wrong, wrong + 1), TEXT, 28, True)
    label(d, 100, 522, "at tick %d, with nothing in the way" % dic[-1][0], MUTED, 26)

    header(d, 820, 210, "THE SPRAY: few passwords, every account", GREEN)
    gx, gy, cw, ch = 1010, 330, 72, 52
    for j, a in enumerate(accounts):
        d.text((gx + j * (cw + 6) + cw // 2, gy - 18), a, font=font(SANS, 20), fill=MUTED, anchor="mm")
    done = set()
    for i, (pw, tries, taken) in enumerate(spray):
        y = gy + i * (ch + 10)
        label(d, 820, y + ch // 2, pw, TEXT, 26, True, "lm")
        for j, a in enumerate(accounts):
            x = gx + j * (cw + 6)
            if a in done:
                continue
            c = ROSE if a in taken else CLIENT
            cell(d, x, y, cw, ch, "IN" if a in taken else "x", c, 22)
        done |= set(taken)
    label(d, 820, 540, "%s guesses in all, at most %s on one account" % (re.search(r"in (\d+) guesses", result).group(1), most),
          TEXT, 28, True)

    header(d, 100, 640, "THE RESULT", ROSE)
    label(d, 330, 662, "dictionary: dana taken      spray: %s taken (%s)"
          % (len(done), ", ".join(sorted(done, key=accounts.index))), TEXT, 30, True, "lm")
    legend(d, 740)
    caption(d, "Deep on one account, or wide over all of them: with no defences, both just work.", AMBER, 860)


def lock_timeline(img, d, cfg):
    """How the lockout turns tick 13 into tick 33, run by run."""
    none = runs(part("ONE", "THE DICTIONARY"))
    lock = runs(part("TWO", "THE DICTIONARY"))
    header(d, 100, 210, "THE DICTIONARY on dana, tick by tick", GREEN)
    timeline(d, 290, "no defence", none, "tick %d" % none[-1][0])
    timeline(d, 420, "lockout", lock, "tick %d" % lock[-1][0])
    tick_axis(d, 515, [0, 5, 10, 15, 20, 25, 30, 33])
    header(d, 100, 600, "HOW TICK %d IS MADE" % lock[-1][0], AMBER)
    first = lock[0]
    lk = lock[1]
    steps = ["the 5th wrong guess is at tick %d" % first[1],
             "so ticks %d to %d are locked: %d ticks, no guess used" % (lk[0], lk[1], lk[1] - lk[0] + 1),
             "the same again: 5 wrong at %d-%d, locked %d-%d" % (lock[2][0], lock[2][1], lock[3][0], lock[3][1]),
             "the last 4 guesses take ticks %d-%d: IN at tick %d" % (lock[4][0], lock[5][0], lock[5][0])]
    for i, s in enumerate(steps):
        label(d, 130, 665 + i * 46, "%d.  %s" % (i + 1, s), TEXT, 28)
    legend(d, 870)
    locked = sum(b - a + 1 for a, b, _, o in lock if o == "LOCKED")
    caption(d, "The same 14 guesses, plus %d locked ticks: tick %d. Slowed, not stopped." % (locked, lock[-1][0]),
            AMBER, 940)


def limit_windows(img, d, cfg):
    """A rate limit: 5 attempts per address in every window of 10 ticks."""
    none = runs(part("ONE", "THE DICTIONARY"))
    lim = runs(part("THREE", "THE DICTIONARY"))
    spray_old = re.search(r"at tick (\d+)", head_line("ONE", "THE RESULT", 1)).group(1)
    spray_new = re.search(r"by tick (\d+)", head_line("THREE", "THE RESULT", 1)).group(1)
    header(d, 100, 210, "THE DICTIONARY on dana, from one address", GREEN)
    for w in range(0, 31, 10):                               # the windows of 10 ticks
        x, x2 = tx(w), tx(min(w + 10, LAST))
        d.rounded_rectangle([x, 395, x2 - 6, 520], radius=8, fill=PANEL)
        d.text(((x + x2) / 2, 412), "window %d-%d" % (w, w + 9), font=font(SANS, 20), fill=MUTED, anchor="mm")
    timeline(d, 280, "no defence", none, "tick %d" % none[-1][0])
    timeline(d, 435, "rate limit", lim, "tick %d" % lim[-1][0])
    tick_axis(d, 540, [0, 5, 10, 15, 20, 23, 30, 33])
    header(d, 100, 620, "THE RESULT", ROSE)
    label(d, 130, 690, "each window: 5 guesses, then 5 ticks refused", TEXT, 28)
    label(d, 130, 736, "dana: IN at tick %d instead of %d" % (lim[-1][0], none[-1][0]), TEXT, 28)
    label(d, 130, 782, "the spray: done at tick %s instead of %s" % (spray_new, spray_old), TEXT, 28)
    legend(d, 850)
    caption(d, "About half the speed. A rate limit buys time; it does not close the door.", AMBER, 930)


def stuffing_table(img, d, cfg):
    """Ten leaked pairs, each tried once, against three sets of defences."""
    lines = act("FOUR")
    taken1 = re.search(r"taken: ([a-z, ]+?)\s+<-", lines).group(1).split(", ")
    mfa_ok = part("FOUR", "THE RESULT", 1)[0].split(": ")[1].split(", ")
    mfa_stop = part("FOUR", "THE RESULT", 1)[1].split(": ")[1].split(", ")
    reset = part("FOUR", "THE RESULT", 2)[1].split(": ")[1].split(", ")
    names = part("ONE", "THE SPRAY")[0].split()[1:]          # the bank's ten customers
    header(d, 100, 210, "THE LEAK: 10 pairs, 1 try each, 10 addresses", GREEN)
    header(d, 1000, 210, "THE RESULT", ROSE)
    cols = [("lockout + rate limit", lambda n: ("IN", ROSE) if n in taken1 else ("wrong", MUTED)),
            ("+ 2nd factor on dana, joel", lambda n: ("IN", ROSE) if n in mfa_ok else
             ("code?", GREEN) if n in mfa_stop else ("wrong", MUTED)),
            ("+ breach check", lambda n: ("new pw", GREEN) if n in reset else ("wrong", MUTED))]
    cx = [560, 960, 1400]
    for j, (title, _) in enumerate(cols):
        label(d, cx[j] + 170, 285, title, TEXT, 24, True, "ma")
    for i, n in enumerate(names):
        y = 325 + i * 54
        reused = n in taken1
        label(d, 110, y + 22, n, TEXT, 26, True, "lm")
        label(d, 230, y + 22, "same password here" if reused else "different here", CYAN if reused else MUTED, 22,
              False, "lm")
        for j, (_, f) in enumerate(cols):
            text, c = f(n)
            cell(d, cx[j], y, 340, 44, text, c, 22, False)
    totals = ["%d taken" % len(taken1), "%d taken" % len(mfa_ok), "0 taken"]
    for j, t in enumerate(totals):
        label(d, cx[j] + 170, 875, t, ROSE if not t.startswith("0") else GREEN, 30, True, "ma")
    caption(d, "One try per account and per address: no lock or limit ever counts to five.", AMBER, 935)


def the_bill(img, d, cfg):
    """What the defences cost: a lock an outsider can trigger, and messages that leak names."""
    resp = part("FIVE", "THE RESPONSE", 0)
    leak = part("FIVE", "THE RESPONSE", 1)
    same = part("FIVE", "THE RESPONSE", 2)
    e = header(d, 100, 210, "THE REQUEST", GREEN)
    label(d, e + 24, 232, "attacker: 5 wrong passwords for dana.  Then dana, right password, own phone.", TEXT, 26,
          True, "lm")
    header(d, 100, 285, "THE RESPONSE", ROSE)
    for i, ln in enumerate(resp[:2]):
        k, v = ln.split(":", 1)
        out = v.split()[0]
        y = 350 + i * 76
        label(d, 130, y + 30, k, TEXT, 28, False, "lm")
        cell(d, 640, y, 260, 60, out, ROSE if out == "LOCKED" else GREEN, 28)
        label(d, 930, y + 30, "denial of service: the owner is out" if out == "LOCKED"
              else "dana is in; only the attacker's pair is locked", MUTED, 24, False, "lm")
    label(d, 130, 520, resp[2], AMBER, 26, True)

    e = header(d, 100, 590, "THE REQUEST", GREEN)
    label(d, e + 24, 612, head_line("FIVE", "THE REQUEST", 1), TEXT, 26, True, "lm")
    for j, (title, rows, c) in enumerate((("a message for each case", leak, ROSE), ("one message for both", same, GREEN))):
        x = 100 + j * 870
        header(d, x, 665, "THE RESPONSE: " + title, c)
        for i, ln in enumerate(rows[:2]):
            who, said = ln.split(":", 1)
            said = re.sub(r"\s+<-.*$", "", said).strip()
            label(d, x + 20, 745 + i * 60, who, TEXT, 26, True, "lm")
            cell(d, x + 110, 718 + i * 60, 680, 52, said, c, 22, False)
    caption(d, "Different words, or different work, tell an outsider which names exist.", AMBER, 900)
    label(d, W // 2, 950, same[2], MUTED, 26, False, "ma")
