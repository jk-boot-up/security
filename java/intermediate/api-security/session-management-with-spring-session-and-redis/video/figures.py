"""Pictures for the Spring Session and Redis video, drawn by the `figure` slide kind.

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

def cookie_steps():
    """The worked example of act ONE: the id, its first 3 characters as bytes and 6-bit groups, and the cookie."""
    lines = section("ONE", "HOW THE COOKIE IS MADE")
    the_id = lines[0].split(": ")[1]
    first, hex_bytes = re.match(r"the first 3 characters, (\S+), are the bytes (.+)", lines[1]).groups()
    groups = [g.split("=") for g in after(lines[2]).split()]
    cookie = lines[4]
    return the_id, first, hex_bytes.split(), groups, cookie


# ------------------------------------------------------------------ figures

def id_in_redis(img, d, cfg):
    """The core idea: the cookie is only the Base64 of the id that Redis keeps as part of its key."""
    the_id, _, _, _, cookie = cookie_steps()
    keys = section("ONE", "WHAT REDIS HOLDS")
    header(d, 60, 180, "REDIS KEEPS THE ID AS ITSELF", ROSE)
    panel(d, 60, 240, 1800, 60 + 46 * len(keys), ROSE, 3)
    for j, k in enumerate(keys):
        fit_mono(d, 90, 262 + j * 46, k, TEXT if "<id>" in k else MUTED, 30, 1000)
    label(d, 1180, 270, "<id> is the real session id,", MUTED, 28)
    label(d, 1180, 312, "for example:", MUTED, 28)
    fit_mono(d, 1180, 356, the_id, AMBER, 28, 660)
    header(d, 60, 560, "THE COOKIE IS THE SAME ID, IN BASE64", CLIENT)
    label(d, 60, 625, "the id", MUTED, 28, True)
    fit_mono(d, 380, 622, the_id, AMBER, 32, 1400)
    label(d, 60, 680, "the cookie", MUTED, 28, True)
    fit_mono(d, 380, 677, cookie, CLIENT, 32, 1400)
    opened = after([ln for ln in act("ONE") if ln.startswith("THE ANSWER:")][-1]).split("  <- ")[0]
    stolen = heading("ONE", "THE REQUEST (a stolen").split("(")[1].rstrip("):")
    label(d, 60, 740, "So " + stolen + ":", TEXT, 30)
    label(d, 1860, 740, opened, ROSE, 34, True, "ra")
    caption(d, "Anyone who can read Redis's keys can make the cookies.", AMBER, 900)


def base64_cookie(img, d, cfg):
    """How an id becomes the cookie: 3 characters, 3 bytes, 24 bits, 4 groups of 6 bits, 4 characters."""
    the_id, first, hex_bytes, groups, cookie = cookie_steps()
    header(d, 60, 180, "BEFORE ENCODING: THE ID", CYAN)
    fit_mono(d, 60, 245, the_id, AMBER, 40, 1700)
    header(d, 60, 330, "THE FIRST 3 CHARACTERS, AS BYTES AND BITS", AMBER)
    bits = "".join(g for g, _ in groups)
    for i, ch in enumerate(first):
        x = 60 + i * 330
        cell(d, x, 390, 300, 56, "'%s' = %s" % (ch, hex_bytes[i]), AMBER, 28)
        cell(d, x, 460, 300, 56, bits[i * 8:i * 8 + 8], TEXT, 28)
    for i, (g, ch) in enumerate(groups):
        x = 60 + i * 248
        cell(d, x, 550, 228, 56, g, TEXT, 28)
        draw.arrow(d, x + 114, 610, x + 114, 645, MUTED, 3)
        cell(d, x + 74, 650, 80, 56, ch, GREEN, 30)
    label(d, 1100, 400, "24 bits in one row, cut into", MUTED, 28)
    label(d, 1100, 442, "4 groups of 6; each group,", MUTED, 28)
    label(d, 1100, 484, "0 to 63, picks a character", MUTED, 28)
    label(d, 1100, 526, "of A-Z, a-z, 0-9, + and /.", MUTED, 28)
    label(d, 1100, 600, "36 characters -> 36 / 3 x 4 = 48", TEXT, 28, True)
    header(d, 60, 750, "AFTER ENCODING: THE COOKIE", GREEN)
    fit_mono(d, 60, 815, cookie, GREEN, 40, 1760)
    caption(d, "Base64 hides nothing: it is a way of writing, not a lock.", AMBER, 910)


def fixation(img, d, cfg):
    """Session fixation: the same planted cookie, a bank that keeps the id and Spring Security's default."""
    planted = section("TWO", "BEFORE ANA SIGNS IN")
    keeps = section("TWO", "AFTER SIGN-IN, A BANK THAT KEEPS")
    fresh = section("TWO", "AFTER SIGN-IN, THE DEFAULT")
    header(d, 60, 180, "BEFORE ANA SIGNS IN", AMBER)
    panel(d, 60, 240, 1800, 110, AMBER, 3)
    fit_label(d, 85, 260, "1. " + planted[0], TEXT, 28, 1750, bold=False)
    fit_label(d, 85, 305, "2. " + planted[1], TEXT, 28, 1750, bold=False)
    lanes = [(heading("TWO", "AFTER SIGN-IN, A BANK THAT KEEPS").rstrip(":"), ROSE, keeps),
             (heading("TWO", "AFTER SIGN-IN, THE DEFAULT").rstrip(":"), GREEN, fresh)]
    for i, (title, colour, lines) in enumerate(lanes):
        x = 60 + i * 920
        fit_label(d, x, 400, title, colour, 26, 880)
        panel(d, x, 450, 880, 380, colour, 3)
        y = 470
        for ln in lines:
            text = ln.split("  <- ")[0]
            what, ans = text.rsplit(": ", 1)
            fit_label(d, x + 25, y, what + ":", TEXT, 26, 830, bold=False)
            ok = ans == "allowed" or ans.startswith("refused")
            fit_label(d, x + 45, y + 36, ans, answer_colour(ans) if ok else MUTED, 26, 820)
            y += 86
        if any("SESSION FIXATION" in ln for ln in lines):
            label(d, x + 855, 790, "SESSION FIXATION", ROSE, 30, True, "ra")
    caption(d, "Spring Security's default gives a new id at sign-in: keep it on.", AMBER, 900)


def timeouts(img, d, cfg):
    """The idle limit in seconds, Redis's time to live, and the absolute limit our filter adds."""
    idle = section("THREE", "IDLE")
    ttl = section("THREE", "WHAT REDIS DOES")[0]
    absolute = section("THREE", "ABSOLUTE")
    header(d, 60, 175, "IDLE: SECONDS SINCE THE LAST REQUEST (LIMIT 2)", CYAN)
    x0, x1, y = 120, 900, 320
    scale = (x1 - x0) / 3
    d.line([x0, y, x1, y], fill=LINE, width=6)
    lx = x0 + 2 * scale
    d.line([lx, y - 40, lx, y + 40], fill=AMBER, width=5)
    label(d, lx, y - 80, "2 s", AMBER, 28, True, "ma")
    for ln in idle[:2]:
        s, ans = [p.strip() for p in ln.split("->")]
        px = x0 + float(s) * scale
        d.ellipse([px - 14, y - 14, px + 14, y + 14], fill=answer_colour(ans))
        label(d, px, y + 30, s, TEXT, 26, True, "ma")
        label(d, px, y + 70, ans, answer_colour(ans), 26, True, "ma")
    fit_label(d, 120, y + 130, idle[2], MUTED, 26, 800, bold=False)
    header(d, 1000, 175, "WHAT REDIS DOES", CYAN)
    panel(d, 1000, 240, 860, 230, CYAN, 3)
    label(d, 1025, 260, "the record's time to live:", MUTED, 28)
    label(d, 1025, 305, "2 s idle + 300 s = 302 s at most", TEXT, 30, True)
    label(d, 1025, 360, "so an ended session's data", MUTED, 28)
    label(d, 1025, 400, "stays five more minutes, unused", MUTED, 28)
    header(d, 60, 560, "ABSOLUTE: A BUSY SESSION, THE CLOCK MOVED FORWARD", CYAN)
    xa0, xa1, ya = 120, 1800, 720
    d.line([xa0, ya, xa1, ya], fill=LINE, width=6)
    sx = (xa1 - xa0) / (8 * 60 + 30)
    for h in range(0, 9):
        label(d, xa0 + h * 60 * sx, ya + 20, "%d:00" % h, MUTED, 24, False, "ma")
    ex = xa0 + 480 * sx
    d.line([ex, ya - 60, ex, ya + 10], fill=AMBER, width=5)
    for ln in absolute[:2]:
        t, ans = [p.strip() for p in ln.split("->")]
        hh, mm = t.split()[0].split(":")
        px = xa0 + (int(hh) * 60 + int(mm)) * sx
        d.ellipse([px - 14, ya - 14, px + 14, ya + 14], fill=answer_colour(ans))
    label(d, ex - 30, ya - 120, absolute[0].replace(" ->", ":"), GREEN, 28, True, "ra")
    label(d, ex - 30, ya - 80, absolute[1].replace(" ->", ":"), ROSE, 28, True, "ra")
    fit_label(d, 120, ya + 70, absolute[2], TEXT, 28, 1700, bold=False)
    caption(d, "Spring Session ends idle sessions; the 8-hour limit is ours to add.", AMBER, 910)


def ending(img, d, cfg):
    """Ana's devices, and which still work after sign out everywhere, a password change and a fourth sign-in."""
    def cells(lines):
        found = {}
        for ln in lines:
            for part in ln.split("; "):
                m = re.match(r"(?:the )?(phone|tablet|laptop's new id|laptop|watch)(?:'s)?(?:, the oldest)?: (.+)", part.strip())
                if m:
                    found[m.group(1).replace("'s new id", "")] = m.group(2)
        return found
    rows = [("SIGN OUT EVERYWHERE", section("FOUR", "SIGN OUT EVERYWHERE"), {"laptop": "refused (ended 3)"}),
            ("PASSWORD CHANGE", section("FOUR", "A PASSWORD CHANGE"), {"tablet": "refused (ended 2)"}),
            ("LIMIT 3, 4TH SIGN-IN", section("FOUR", "A LIMIT OF 3"), {"tablet": "-"})]
    devices = ["phone", "laptop", "tablet", "watch"]
    for j, dev in enumerate(devices):
        label(d, 760 + j * 280, 190, dev, TEXT, 30, True, "ma")
    for i, (title, lines, extra) in enumerate(rows):
        y = 250 + i * 170
        header(d, 60, y + 20, title, AMBER)
        found = cells(lines)
        first = lines[0]
        fit_label(d, 60, y + 80, first if not first.startswith("status") else "sign-in " + first, MUTED, 26, 560, bold=False)
        for j, dev in enumerate(devices):
            ans = found.get(dev) or extra.get(dev)
            if ans is None or ans == "-":
                continue
            cell(d, 640 + j * 280, y + 20, 240, 70, ans.split(" (")[0], answer_colour(ans), 28)
    caption(d, "Every session ends on the server; the index finds all of ana's.", AMBER, 900)


def redis_down(img, d, cfg):
    """Two servers and one Redis: shared sessions, a Redis that stops answering, and records made by strangers."""
    own = section("FIVE", "THE ANSWER FROM B, EACH")[0].split("  <- ")[0]
    shared = section("FIVE", "THE ANSWER FROM B, BOTH")[0]
    down = section("FIVE", "WHILE REDIS")
    cache = section("FIVE", "20 REQUESTS")
    header(d, 60, 175, "ANA SIGNS IN ON A; THE NEXT REQUEST LANDS ON B", CLIENT)
    draw.box(d, 60, 250, 300, 100, "Server A", "sign-in", TEXT, 34, 28)
    draw.box(d, 480, 250, 300, 100, "Server B", "next request", TEXT, 34, 28)
    draw.box(d, 270, 430, 300, 100, "One Redis", "with a password", GREEN, 34, 28)
    draw.arrow(d, 210, 350, 360, 425, GREEN, 3)
    draw.arrow(d, 630, 350, 480, 425, GREEN, 3)
    fit_label(d, 60, 570, "own stores: " + own, ROSE, 28, 820)
    fit_label(d, 60, 615, "one Redis: " + shared, GREEN, 28, 820)
    header(d, 960, 175, "WHILE REDIS STOPS ANSWERING", ROSE)
    panel(d, 960, 235, 900, 150, ROSE, 3)
    for j, ln in enumerate(down):
        fit_label(d, 985, 255 + j * 55, ln.split("  <- ")[0], ROSE, 28, 850, bold=False)
    label(d, 1835, 345, "FAILS CLOSED", GREEN, 28, True, "ra")
    header(d, 960, 430, "20 STRANGERS, NO COOKIE: RECORDS IN REDIS", AMBER)
    for j, ln in enumerate(cache):
        name, n = ln.split("  <- ")[0].rsplit(": ", 1)
        y = 500 + j * 90
        label(d, 985, y + 10, name, TEXT, 28)
        w = int(int(n) / 20 * 260)
        d.rounded_rectangle([1500, y, 1500 + max(w, 6), y + 50], radius=8, fill=ROSE if int(n) else GREEN)
        label(d, 1520 + max(w, 6), y + 10, n, TEXT, 28, True)
    caption(d, "A shared store, a bank that refuses when it is down, a cache off.", AMBER, 900)
