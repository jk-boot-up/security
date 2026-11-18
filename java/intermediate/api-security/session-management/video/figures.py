"""Pictures for the session management video, drawn by the `figure` slide kind.

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


def section(name, head):
    """The indented lines under the first section header of an act that starts with `head`, stripped."""
    out, on = [], False
    for ln in act(name):
        if not ln.startswith("  "):
            if on:
                break
            on = ln.startswith(head)
        elif on:
            out.append(ln.strip())
    return out


def heading(name, head):
    """A section header line itself, for headers that carry their value ("THE ANSWER: 0 sessions opened")."""
    return next(ln for ln in act(name) if ln.startswith(head))


def after(text, sep=": "):
    return text.split(sep, 1)[1]


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=28, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 18:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def fit_label(d, x, y, text, colour, size, max_w, anchor="la", bold=True):
    f = font(SANS_B if bold else SANS, size)
    while f.getlength(text) > max_w and f.size > 18:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def answer_colour(text):
    return GREEN if text.startswith("allowed") else ROSE if text.startswith("refused") else AMBER


# ------------------------------------------------------------------ values

def example():
    """The worked example of act ONE: bytes, the 3-byte walk, the id and its hash."""
    hex_bytes = section("ONE", "BEFORE ENCODING")[0].split("  (")[0].split()
    walk = section("ONE", "HOW BASE64URL WORKS")
    bits = walk[0].split()
    groups = [g.split("=") for g in after(walk[1]).split()]
    the_id = section("ONE", "AFTER ENCODING")[0].split()[0]
    key = section("ONE", "WHAT THE STORE KEEPS")[0]
    return hex_bytes, bits, groups, the_id, key


# ------------------------------------------------------------------ figures

def id_and_hash(img, d, cfg):
    """The core idea: the browser keeps the id, the bank keeps only its hash; a stolen copy of the store opens nothing."""
    _, _, _, the_id, key = example()
    header(d, 60, 190, "ANA'S BROWSER KEEPS THE ID", CLIENT)
    panel(d, 60, 250, 760, 150, CLIENT, 3)
    label(d, 85, 270, "cookie __Host-session", MUTED, 26, True)
    fit_mono(d, 85, 320, the_id, CLIENT, 36, 700)
    header(d, 1000, 190, "THE BANK'S STORE KEEPS ITS SHA-256", GREEN)
    panel(d, 1000, 250, 860, 260, GREEN, 3)
    label(d, 1025, 270, "key (64 hex characters)", MUTED, 26, True)
    fit_mono(d, 1025, 315, key[:32], GREEN, 30, 800)
    fit_mono(d, 1025, 355, key[32:], GREEN, 30, 800)
    label(d, 1025, 420, "-> user ana, signed in at, last request at", TEXT, 26)
    draw.arrow_right(d, 830, 325, 990, AMBER, 5)
    label(d, 910, 280, "SHA-256", AMBER, 26, True, "ma")

    stolen = heading("ONE", "THE REQUEST (a stolen").split("(")[1].rstrip("):")
    opened = after(heading("ONE", "THE ANSWER:"))
    header(d, 60, 560, "A STOLEN COPY OF THE STORE", ROSE)
    panel(d, 60, 620, 1800, 220, ROSE, 3)
    label(d, 85, 645, stolen, TEXT, 28)
    label(d, 85, 700, "the bank hashes what arrives, so the key becomes the hash of a hash:", MUTED, 26)
    label(d, 85, 745, "no session is stored under that, and nothing matches.", MUTED, 26)
    label(d, 1835, 780, opened, GREEN, 34, True, "ra")
    caption(d, "The id works only in the browser; the store holds nothing that works.", AMBER, 900)


def id_encoding(img, d, cfg):
    """How 16 random bytes become a 22-character id, and the hash the store keeps."""
    hex_bytes, bits, groups, the_id, key = example()
    header(d, 60, 180, "BEFORE ENCODING: 16 BYTES, 128 BITS", CYAN)
    for i, b in enumerate(hex_bytes):
        cell(d, 60 + i * 112, 240, 96, 60, b, AMBER if i < 3 else CYAN, 28)
    header(d, 60, 340, "HOW BASE64URL WORKS: THE FIRST 3 BYTES", AMBER)
    for i, b in enumerate(bits):
        x = 60 + i * 300
        cell(d, x, 400, 280, 56, b, AMBER, 28)
    for i, (g, ch) in enumerate(groups):
        x = 60 + i * 225
        cell(d, x, 480, 205, 56, g, TEXT, 28)
        draw.arrow(d, x + 102, 540, x + 102, 575, MUTED, 3)
        cell(d, x + 62, 580, 80, 56, ch, GREEN, 30)
    label(d, 990, 420, "24 bits read as one row,", MUTED, 26)
    label(d, 990, 460, "cut into 4 groups of 6 bits;", MUTED, 26)
    label(d, 990, 500, "each group (0 to 63) picks one", MUTED, 26)
    label(d, 990, 540, "of 64 letters, digits, - and _.", MUTED, 26)
    label(d, 990, 590, "16 bytes x 8 = 128 bits; 128 / 6 -> 22 characters", TEXT, 26, True)
    header(d, 60, 680, "AFTER ENCODING: THE ID", GREEN)
    fit_mono(d, 60, 740, the_id, GREEN, 40, 860)
    header(d, 980, 680, "WHAT THE STORE KEEPS: ITS SHA-256", CLIENT)
    fit_mono(d, 980, 740, key[:32], CLIENT, 30, 880)
    fit_mono(d, 980, 780, key[32:], CLIENT, 30, 880)
    caption(d, "Real ids take all 16 bytes from SecureRandom; this example is fixed.", AMBER, 900)


def fixation(img, d, cfg):
    """Session fixation: the same planted id, a bank that keeps it and a bank that replaces it."""
    planted = section("TWO", "BEFORE ANA SIGNS IN")
    keeps = section("TWO", "AFTER SIGN-IN, A BANK THAT KEEPS")
    fresh = section("TWO", "AFTER SIGN-IN, A BANK THAT ISSUES")
    header(d, 60, 180, "BEFORE ANA SIGNS IN", AMBER)
    panel(d, 60, 240, 1800, 110, AMBER, 3)
    fit_label(d, 85, 260, "1. " + planted[0], TEXT, 28, 1750, bold=False)
    fit_label(d, 85, 305, "2. " + planted[1], TEXT, 28, 1750, bold=False)
    lanes = [("AFTER SIGN-IN, A BANK THAT KEEPS THE ID", ROSE, keeps),
             ("AFTER SIGN-IN, A BANK THAT ISSUES A NEW ID", GREEN, fresh)]
    for i, (title, colour, lines) in enumerate(lanes):
        x = 60 + i * 920
        header(d, x, 400, title, colour)
        panel(d, x, 460, 880, 300, colour, 3)
        y = 485
        for ln in lines:
            text, mark = (ln.split("  <- ") + [""])[:2]
            what, ans = text.rsplit(": ", 1) if ": " in text else (text, "")
            fit_label(d, x + 25, y, what + ":", TEXT, 26, 830, bold=False)
            fit_label(d, x + 45, y + 38, ans, answer_colour(ans) if ans == "allowed" or ans.startswith("refused") else MUTED, 26, 820)
            y += 90
        if any("SESSION FIXATION" in ln for ln in lines):
            label(d, x + 855, 715, "SESSION FIXATION", ROSE, 30, True, "ra")
    caption(d, "A new id at sign-in makes any id known before it worthless.", AMBER, 900)


def lifetimes(img, d, cfg):
    """Three clocks: idle minutes, the absolute limit for a busy session, and the payment window."""
    settings = section("THREE", "THE SETTINGS")
    idle = section("THREE", "IDLE")
    absolute = section("THREE", "ABSOLUTE")
    pay = section("THREE", "PAYMENTS")
    idle_limit = int(settings[0].split()[2])
    reauth = int(re.search(r"(\d+) minutes", settings[2]).group(1))

    # idle: a 0..30 minute bar with the limit
    header(d, 60, 175, "IDLE: MINUTES SINCE THE LAST REQUEST", CYAN)
    x0, x1, y = 120, 900, 300
    scale = (x1 - x0) / 20
    d.line([x0, y, x1, y], fill=LINE, width=6)
    lx = x0 + idle_limit * scale
    d.line([lx, y - 40, lx, y + 40], fill=AMBER, width=5)
    label(d, lx, y - 75, "%d min" % idle_limit, AMBER, 26, True, "ma")
    for ln in idle[:2]:
        m, ans = ln.split(" -> ")
        px = x0 + int(m) * scale
        d.ellipse([px - 14, y - 14, px + 14, y + 14], fill=answer_colour(ans))
        label(d, px, y + 30, m, TEXT, 26, True, "ma")
    label(d, x0, y + 80, "14: " + after(idle[0], " -> "), GREEN, 26, True)
    fit_label(d, x0, y + 118, "16: " + after(idle[1], " -> "), ROSE, 26, 800)

    # payments
    header(d, 1000, 175, "A PAYMENT: MINUTES SINCE THE PASSWORD", CYAN)
    x0p, x1p = 1060, 1840
    scale_p = (x1p - x0p) / 8
    d.line([x0p, y, x1p, y], fill=LINE, width=6)
    rx = x0p + reauth * scale_p
    d.line([rx, y - 40, rx, y + 40], fill=AMBER, width=5)
    label(d, rx, y - 75, "%d min" % reauth, AMBER, 26, True, "ma")
    for ln in pay[:2]:
        m, ans = ln.split(" -> ")
        px = x0p + int(m) * scale_p
        d.ellipse([px - 14, y - 14, px + 14, y + 14], fill=GREEN if ans.endswith("allowed") else AMBER)
        label(d, px, y + 30, m, TEXT, 26, True, "ma")
    label(d, x0p, y + 80, "3: " + pay[0].split(" -> ")[1], GREEN, 26, True)
    fit_label(d, x0p, y + 118, "6: " + pay[1].split(" -> ")[1], AMBER, 26, 780)

    # absolute: a request every 10 minutes for 8 hours
    header(d, 60, 520, "ABSOLUTE: A BUSY SESSION, A REQUEST EVERY 10 MINUTES", CYAN)
    n_ok = int(re.search(r"requests 1 to (\d+)", absolute[1]).group(1))
    total = n_ok + 1
    xa0, xa1, ya = 120, 1800, 680
    step = (xa1 - xa0) / total
    d.line([xa0, ya, xa1, ya], fill=LINE, width=4)
    for i in range(1, total + 1):
        px = xa0 + i * step
        c = GREEN if i <= n_ok else ROSE
        d.rectangle([px - 6, ya - 30, px + 6, ya], fill=c)
    for i in range(0, total + 1, 6):
        px = xa0 + i * step
        label(d, px, ya + 15, "%d:00" % (i // 6), MUTED, 24, False, "ma")
    label(d, xa0, ya + 70, absolute[0], TEXT, 28, True)
    fit_label(d, xa0, ya + 115, absolute[1] + ";   " + absolute[2], TEXT, 26, 1700, bold=False)
    caption(d, "Idle ends a forgotten session; absolute ends even a busy one.", AMBER, 900)


def cookie_attributes(img, d, cfg):
    """The session cookie's attributes, the weak cookie's problems, and what sign-out really does."""
    meaning = {"Path=/": "sent to every page of the bank", "Secure": "sent only over HTTPS",
               "HttpOnly": "no script can read it", "SameSite=Strict": "not sent from other sites"}
    header(d, 60, 175, "THE SIGN-IN RESPONSE'S COOKIE", GREEN)
    attrs = ["__Host-session=<id>"] + [a.strip().rstrip(";") for ln in section("FOUR", "THE SIGN-IN")[1:3]
                                       for a in ln.replace("Set-Cookie:", "").split(";") if a.strip()][1:]
    y = 235
    for a in attrs:
        cell(d, 60, y, 420, 62, a, GREEN, 30)
        label(d, 505, y + 31, meaning.get(a, "the prefix: Path=/, Secure, no Domain"), TEXT, 28, False, "lm")
        y += 76
    problems = section("FOUR", "ITS PROBLEMS")[0].split(", ")
    weak = section("FOUR", "A WEAK COOKIE")[0].replace("Set-Cookie: ", "")
    header(d, 1260, 175, "A WEAK COOKIE", ROSE)
    fit_mono(d, 1260, 245, weak, ROSE, 30, 600)
    for i, p in enumerate(problems):
        cell(d, 1260, 311 + i * 76, 520, 62, p, ROSE, 30)
    header(d, 60, 640, "AFTER SIGN-OUT, A COPY OF THE COOKIE IS SENT", AMBER)
    real = section("FOUR", "A COPY MADE BEFORE")[0]
    clears = section("FOUR", "A BANK WHOSE SIGN-OUT")[0]
    panel(d, 60, 700, 880, 160, GREEN, 3)
    label(d, 85, 720, "the session ended on the server", MUTED, 28, True)
    fit_label(d, 85, 775, real.split("  <- ")[0].replace("status", "HTTP status"), GREEN, 36, 830)
    panel(d, 980, 700, 880, 160, ROSE, 3)
    label(d, 1005, 720, "sign-out only cleared the browser", MUTED, 28, True)
    fit_label(d, 1005, 775, "the copy: " + clears.split(": ")[1].split("  <- ")[0] + ", still signed in", ROSE, 34, 830)
    caption(d, "Sign-out must end the session on the server, not only in the browser.", AMBER, 910)


def two_servers(img, d, cfg):
    """Two servers with separate memories refuse each other's sessions; one shared store fixes it."""
    own = section("FIVE", "THE ANSWER FROM B, EACH")[0].split("  <- ")[0]
    shared = section("FIVE", "THE ANSWER FROM B, BOTH")[0].split(" (")[0]
    for i, (title, colour, ans, one) in enumerate([("EACH SERVER WITH ITS OWN MEMORY", ROSE, own, False),
                                                   ("BOTH SERVERS SHARING ONE STORE", GREEN, shared, True)]):
        x = 60 + i * 920
        header(d, x, 180, title, colour)
        draw.box(d, x + 280, 250, 320, 110, "Ana", "signs in on A", CLIENT, 36, 28)
        draw.box(d, x + 40, 420, 320, 110, "Server A", "sign-in", TEXT, 36, 28)
        draw.box(d, x + 520, 420, 320, 110, "Server B", "next request", TEXT, 36, 28)
        draw.arrow(d, x + 380, 360, x + 220, 415, CLIENT, 3)
        draw.arrow(d, x + 500, 360, x + 660, 415, CLIENT, 3)
        if one:
            draw.box(d, x + 280, 600, 320, 110, "One store", "both read it", GREEN, 36, 28)
            draw.arrow(d, x + 200, 530, x + 380, 595, GREEN, 3)
            draw.arrow(d, x + 680, 530, x + 500, 595, GREEN, 3)
        else:
            draw.box(d, x + 40, 600, 320, 110, "Memory A", "ana's session", MUTED, 36, 28)
            draw.box(d, x + 520, 600, 320, 110, "Memory B", "empty", MUTED, 36, 28)
            draw.arrow(d, x + 200, 530, x + 200, 595, MUTED, 3)
            draw.arrow(d, x + 680, 530, x + 680, 595, MUTED, 3)
        fit_label(d, x + 440, 760, "B answers: " + ans, colour, 34, 860, "ma")
    caption(d, "More than one server needs one shared session store, such as Redis.", AMBER, 900)
