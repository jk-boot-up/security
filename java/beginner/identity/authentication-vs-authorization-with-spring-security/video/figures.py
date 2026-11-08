"""Pictures for the Spring Security authentication vs authorization video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and the tests check, and the
rules are read from ShopController.java, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS,
                                   SANS_B, TEXT, W, font)

ROOT = Path(__file__).resolve().parent.parent
OUT = (ROOT / "docs" / "expected-output.txt").read_text()
SRC = (ROOT / "src" / "main" / "java" / "com" / "jk" / "explore" / "security" / "authspring"
       / "ShopController.java").read_text()
BG_CELL = (22, 32, 54)


# ------------------------------------------------------------------ reading the output

def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("THE REQUESTS")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def requests(act):
    """[(who, method, path, note)] from an act's THE REQUESTS section."""
    return re.findall(r"^    \d  (\S+)\s+(GET|POST)\s+(\S+)[ \t]*(.*)$", section(act, "THE REQUESTS"), re.M)


def responses(act):
    """['401 UNAUTHENTICATED  0 password checks', ...] from an act's THE RESPONSES section."""
    return [r.split("  <-")[0].strip() for r in re.findall(r"^    \d  (.+)$", section(act, "THE RESPONSES"), re.M)]


def rule(method):
    """The @PreAuthorize text written on one ShopController method."""
    return re.search(r'@PreAuthorize\("(.+?)"\)\s*public String %s\(' % method, SRC).group(1)


def status_colour(text):
    return GREEN if text.startswith("200") else AMBER if text.startswith("401") else ROSE


# ------------------------------------------------------------------ drawing helpers

def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUESTS: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def node(d, x, y, w, h, text, c, size=26, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=c, anchor="mm")


def two_line(d, x, y, w, h, top, bottom, c, top_size=30, bottom_size=22):
    """A box with a bold first line and a smaller, monospaced second line."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=c, width=3)
    d.text((x + w // 2, y + h // 2 - 16), top, font=font(SANS_B, top_size), fill=c, anchor="mm")
    d.text((x + w // 2, y + h // 2 + 20), bottom, font=font(MONO_B, bottom_size), fill=MUTED, anchor="mm")


def gate(d, x, y, w, h, title, sub, c):
    """A tall checkpoint every lane passes through."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=c, width=4)
    d.text((x + w // 2, y + 30), title, font=font(SANS_B, 28), fill=c, anchor="mm")
    d.text((x + w // 2, y + 66), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def mark(d, x, y, ok, text, colour=None):
    """A tick or a cross with a short word, centred at (x, y)."""
    c = colour or (GREEN if ok else ROSE)
    if ok is None:
        d.text((x, y), text, font=font(SANS, 24), fill=MUTED, anchor="mm")
        return
    d.text((x - 8, y), ("✓ " if ok else "✗ ") + text, font=font(SANS_B, 28), fill=c, anchor="mm")


def answer(d, x, y, w, text, wrong=False):
    """A response box coloured by its status; `wrong` adds a note that this answer is a mistake."""
    c = status_colour(text)
    node(d, x, y - 32, w, 64, text, c, 26)
    if wrong:
        label(d, x + w // 2, y + 38, "should not be allowed", ROSE, 20, True, "ma")


# ------------------------------------------------------------------ figures

def two_questions(img, d, cfg):
    """The core idea on one real example: one request meets two questions, and stops at the first no."""
    reqs, res = requests("SIX"), responses("SIX")
    lanes = [(0, (False, "no login"), (None, "not asked")),
             (1, (True, reqs[1][0]), (False, reqs[1][3])),
             (5, (True, reqs[5][0]), (True, reqs[5][3]))]
    header(d, 100, 205, "THE REQUEST", GREEN)
    header(d, 1500, 205, "THE RESPONSE", ROSE)
    gate(d, 560, 270, 400, 600, "1. WHO ARE YOU?", "authentication", CLIENT)
    gate(d, 1010, 270, 420, 600, "2. MAY YOU DO THIS?", "authorization", AMBER)
    for row, (i, who_ok, may_ok) in enumerate(lanes):
        y = 420 + row * 150
        who, method, path, note = reqs[i]
        two_line(d, 100, y - 55, 420, 110, who + (" (%s)" % note if note else ""), method + " " + path, GREEN, 28)
        draw.arrow_right(d, 525, y, 600, MUTED, 3)
        mark(d, 760, y, *who_ok)
        if who_ok[0]:
            draw.arrow_right(d, 900, y, 1050, MUTED, 3)
            mark(d, 1220, y, *may_ok)
        else:
            mark(d, 1220, y, *may_ok)
        draw.arrow_right(d, 1380, y, 1495, MUTED, 3)
        answer(d, 1500, y, 330, res[i])
    label(d, 1220, 800, "refund needs:", MUTED, 22, False, "ma")
    label(d, 1220, 832, rule("refund"), AMBER, 20, True, "ma")
    caption(d, "401: we do not know who you are.    403: we know you, and the answer is no.", AMBER, 905)


def open_door(img, d, cfg):
    """Act one: the framework is installed, but it lets every request through."""
    reqs, res = requests("ONE"), responses("ONE")
    header(d, 100, 205, "THE REQUESTS", GREEN)
    header(d, 1500, 205, "THE RESPONSES", ROSE)
    gate(d, 640, 290, 640, 440, "SPRING SECURITY", "filter chain set to permitAll()", CLIENT)
    for i, ((who, method, path, _), r) in enumerate(zip(reqs, res)):
        y = 430 + i * 180
        two_line(d, 100, y - 55, 460, 110, who, method + " " + path, GREEN)
        draw.arrow_right(d, 565, y, 1490, MUTED, 3)
        mark(d, 960, y - 30, True, "let through, nobody asked", AMBER)
        answer(d, 1500, y, 330, r, wrong=True)
    caption(d, "Installing a security framework protects nothing until it is told what to check.", AMBER, 860)


def login_check(img, d, cfg):
    """Act two: the Basic filter reads the login, BCrypt checks the password, and nobody known means 401."""
    reqs, res = requests("TWO"), responses("TWO")
    header(d, 100, 205, "THE REQUESTS", GREEN)
    header(d, 1440, 205, "THE RESPONSES", ROSE)
    gate(d, 560, 270, 380, 640, "BASIC FILTER", "reads the Authorization header", CLIENT)
    gate(d, 990, 270, 380, 640, "PASSWORD CHECK", "BCrypt, against the stored line", AMBER)
    seen = ["nothing to read", "ben", "ben"]
    for i, ((who, method, path, note), r) in enumerate(zip(reqs, res)):
        y = 440 + i * 170
        status, checks = re.match(r"(\d+ \S+)\s+(\d+) password", r).groups()
        two_line(d, 100, y - 55, 420, 110, who + ", " + note, method + " " + path, GREEN, 26)
        draw.arrow_right(d, 525, y, 600, MUTED, 3)
        mark(d, 750, y, i > 0, seen[i]) if i else mark(d, 750, y, None, seen[i])
        draw.arrow_right(d, 900, y, 1010, MUTED, 3)
        if checks == "0":
            mark(d, 1180, y, None, "0 checks: skipped")
        else:
            mark(d, 1180, y, status.startswith("200"), "match" if status.startswith("200") else "no match")
        draw.arrow_right(d, 1335, y, 1435, MUTED, 3)
        answer(d, 1440, y, 400, status)
    caption(d, "Nobody known, or a wrong password: 401. The right password: through.", AMBER, 930)


def basic_header(img, d, cfg):
    """Act three: how the login is written into the request, three bytes at a time."""
    joined = re.search(r"joined\s+(\S+)", OUT).group(1)
    steps = re.findall(r"^    (\S{1,3})  ->  (\S+)$", section("THREE", "STEP BY STEP"), re.M)
    hdr = value("Authorization:", "THREE", "AFTER ENCODING")
    decoded = re.search(r"anyone can decode it: (\S+)", OUT).group(1)
    e = header(d, 100, 205, "BEFORE ENCODING", GREEN)
    label(d, e + 24, 227, "what ben types, joined by a colon: %d bytes" % len(joined), TEXT, 28, True, "lm")
    gw, cw, gap = 330, 102, 12
    for g, (three, four) in enumerate(steps):
        gx = 100 + g * (gw + 40)
        for k, ch in enumerate(three):
            node(d, gx + k * (cw + gap), 280, cw, 76, ch, GREEN, 40)
        d.line([(gx, 372), (gx + gw, 372)], fill=MUTED, width=3)
        draw.arrow(d, gx + gw // 2, 376, gx + gw // 2, 440, CYAN, 4)
        lw = (gw - 3 * 8) // 4
        for k, ch in enumerate(four):
            node(d, gx + k * (lw + 8), 450, lw, 70, ch, ROSE, 36)
    label(d, 100, 535, "Base64: every 3 bytes in become 4 letters out", MUTED, 24)
    e = header(d, 100, 590, "AFTER ENCODING", ROSE)
    label(d, e + 24, 612, "what every request carries", TEXT, 28, True, "lm")
    node(d, 100, 650, 1300, 80, "Authorization: " + hdr, ROSE, 36)
    label(d, 100, 770, "anyone who sees it decodes it back:", MUTED, 28, True)
    node(d, 720, 755, 420, 64, decoded, ROSE, 32)
    caption(d, "Base64 is an encoding, not encryption. Only HTTPS hides it on the way.", AMBER, 880)


def bcrypt_parts(img, d, cfg):
    """Act four: what BCrypt stores, and how a login is checked against it."""
    before = section("FOUR", "BEFORE HASHING")
    password = re.search(r"password\s+(\S+)", before).group(1)
    salt = re.search(r"salt\s+(\S+)", before).group(1)
    cost = re.search(r"cost\s+(.+)$", before, re.M).group(1).split(",")[0]
    version, cost2 = value("version", "FOUR", "AFTER HASHING"), value("cost", "FOUR", "AFTER HASHING")
    hsh = value("hash", "FOUR", "AFTER HASHING")
    checks = re.findall(r"^    (\S+)\s+(\S+)  (same|different)$", section("FOUR", "CHECKING A LOGIN"), re.M)

    header(d, 100, 205, "BEFORE HASHING", GREEN)
    ins = [("password", password, GREEN), ("salt (random in real use)", salt, AMBER),
           ("cost", "%s  →  2^%s = %d rounds" % (cost, cost, 2 ** int(cost)), CYAN)]
    for i, (name, val, c) in enumerate(ins):
        x = 100 + i * 470
        label(d, x, 265, name, c, 24, True)
        node(d, x, 298, 440, 60, val, c, 26)
    draw.arrow_right(d, 1515, 328, 1575, MUTED, 3)
    node(d, 1580, 298, 240, 60, "BCrypt", TEXT, 28, False)

    header(d, 100, 470, "AFTER HASHING", ROSE)
    label(d, 420, 492, "the one line the shop stores, in four parts", TEXT, 26, True, "lm")
    x, f = 100, font(MONO_B, 34)
    for name, text, c in (("version", version, MUTED), ("cost", cost2 + "$", CYAN), ("salt", salt, AMBER), ("hash", hsh, ROSE)):
        w = int(f.getlength(text)) + 30
        d.rounded_rectangle([x, 540, x + w, 610], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((x + w // 2, 575), text, font=f, fill=c, anchor="mm")
        label(d, x + w // 2, 618, name, c, 22, True, "ma")
        x += w + 10

    header(d, 100, 680, "CHECKING A LOGIN", AMBER)
    label(d, 440, 702, "hash the typed password again, with the stored salt and cost", TEXT, 26, True, "lm")
    for i, (typed, again, equal) in enumerate(checks):
        y = 760 + i * 76
        ok = equal == "same"
        node(d, 100, y, 230, 60, typed, GREEN if ok else ROSE, 26)
        draw.arrow_right(d, 335, y + 30, 420, MUTED, 3)
        node(d, 425, y, 760, 60, again, ROSE, 26)
        mark(d, 1420, y + 30, ok, "same as stored hash" if ok else "different: 401")
    caption(d, "The password itself is never stored. Only the salt and the hash.", AMBER, 935)


def login_not_enough(img, d, cfg):
    """Act five: the login question is answered, the second question is never asked."""
    reqs, res = requests("FIVE"), responses("FIVE")
    header(d, 100, 205, "THE REQUESTS", GREEN)
    header(d, 1500, 205, "THE RESPONSES", ROSE)
    gate(d, 560, 270, 400, 520, "1. WHO ARE YOU?", "login checked", CLIENT)
    d.rounded_rectangle([1010, 270, 1430, 790], radius=18, outline=LINE, width=3)
    d.text((1220, 300), "2. MAY YOU DO THIS?", font=font(SANS_B, 28), fill=MUTED, anchor="mm")
    d.text((1220, 336), "no rules yet", font=font(SANS, 22), fill=MUTED, anchor="mm")
    for i, ((who, method, path, note), r) in enumerate(zip(reqs, res)):
        y = 460 + i * 190
        two_line(d, 100, y - 55, 420, 110, who + (", " + note if note else ", a customer"), method + " " + path, GREEN, 26)
        draw.arrow_right(d, 525, y, 600, MUTED, 3)
        mark(d, 760, y, True, "it is ben")
        draw.arrow_right(d, 900, y, 1495, MUTED, 3)
        mark(d, 1220, y - 30, None, "nobody asks")
        answer(d, 1500, y, 330, r, wrong=True)
    caption(d, "Spring knows it is ben. Nothing decides what ben may do.", AMBER, 860)


def rules_table(img, d, cfg):
    """Act six: the rules on the endpoints, the eight requests, and what each one got back."""
    reqs, res = requests("SIX"), responses("SIX")
    header(d, 100, 205, "THE RULES, written on ShopController", AMBER)
    for i, (name, method) in enumerate((("view", "view"), ("cancel", "cancel"), ("refund", "refund"))):
        y = 268 + i * 40
        label(d, 100, y, name, AMBER, 24, True)
        label(d, 230, y + 2, rule(method), TEXT, 22)
    header(d, 100, 400, "THE REQUESTS", GREEN)
    header(d, 1300, 400, "THE RESPONSES", ROSE)
    for i, ((who, method, path, note), r) in enumerate(zip(reqs, res)):
        y = 458 + i * 56
        c = status_colour(r)
        label(d, 100, y + 24, "%d" % (i + 1), MUTED, 24, True, "lm")
        label(d, 140, y + 24, who, TEXT, 26, True, "lm")
        d.text((280, y + 24), "%-4s %s" % (method, path), font=font(MONO_B, 24), fill=GREEN, anchor="lm")
        label(d, 760, y + 24, note, MUTED, 22, False, "lm")
        draw.arrow_right(d, 1000, y + 24, 1290, LINE, 3)
        d.rounded_rectangle([1300, y + 2, 1700, y + 46], radius=10, fill=BG_CELL, outline=c, width=2)
        d.text((1500, y + 24), r, font=font(MONO_B, 24), fill=c, anchor="mm")
    caption(d, "Each rule is checked before its method runs. No login: 401. A rule says no: 403.", AMBER, 920)


def two_layers(img, d, cfg):
    """Act seven: every request pays for the password check, and a forgotten rule needs a second layer."""
    checks = re.search(r"(\d+) requests: (\d+) password", OUT).groups()
    before = re.findall(r"^    (\S+)\s+GET (\S+): (\d+ \S+)", section("SEVEN", "BEFORE THE FIX"), re.M)
    after = re.findall(r"^    (\S+)\s+GET (\S+): (\d+ \S+)", section("SEVEN", "AFTER THE FIX"), re.M)
    header(d, 100, 205, "EVERY REQUEST PAYS", AMBER)
    for i in range(int(checks[0])):
        x = 100 + i * 330
        node(d, x, 268, 300, 56, "sam, request %d" % (i + 1), GREEN, 24, False)
        label(d, x + 150, 332, "+1 BCrypt check", AMBER, 22, True, "ma")
    label(d, 1110, 296, "=  %s password checks" % checks[1], AMBER, 32, True, "lm")

    e = header(d, 100, 400, "BEFORE THE FIX", ROSE)
    label(d, e + 24, 422, "export() has no @PreAuthorize", TEXT, 26, True, "lm")
    who, path, r = before[0]
    y = 500
    two_line(d, 100, y - 45, 380, 90, who + ", a customer", "GET " + path, GREEN, 26)
    draw.arrow_right(d, 485, y, 560, MUTED, 3)
    node(d, 565, y - 32, 380, 64, "logged in?  ✓", CLIENT, 26, False)
    draw.arrow_right(d, 950, y, 1025, MUTED, 3)
    node(d, 1030, y - 32, 380, 64, "rule on method: none", MUTED, 24, False)
    draw.arrow_right(d, 1415, y, 1495, MUTED, 3)
    answer(d, 1500, y, 330, r, wrong=True)

    e = header(d, 100, 600, "AFTER THE FIX", GREEN)
    label(d, e + 24, 622, "a second rule on the address: %s needs ADMIN" % path, TEXT, 26, True, "lm")
    for i, (who, path, r) in enumerate(after):
        y = 700 + i * 110
        ok = r.startswith("200")
        two_line(d, 100, y - 45, 380, 90, who + (", a customer" if not ok else ", the admin"), "GET " + path, GREEN, 26)
        draw.arrow_right(d, 485, y, 560, MUTED, 3)
        node(d, 565, y - 32, 845, 64, "address rule: needs ADMIN   %s" % ("✓" if ok else "✗"), GREEN if ok else ROSE, 26, False)
        draw.arrow_right(d, 1415, y, 1495, MUTED, 3)
        answer(d, 1500, y, 330, r)
    caption(d, "A rule protects only what it is written on. A second layer catches the forgotten one.", AMBER, 925)
