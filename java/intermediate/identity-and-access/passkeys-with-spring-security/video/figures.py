"""Pictures for the passkeys-with-spring-security video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code. Random values
(challenges, keys, signatures) appear there as placeholders.
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


def part(name, head):
    return sections(name, head)[0]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def field(lines, key):
    """The text after `key` in the first line that starts with it, spaces trimmed."""
    return next(ln[len(key):].strip() for ln in lines if ln.strip().startswith(key))


def verdict(text):
    """'accepted (...)' or 'refused (...)' from a line, and its colour."""
    m = re.search(r"(accepted|refused|nothing) \((.*)\)", text)
    return m.group(0), (GREEN if m.group(1) == "accepted" else ROSE)


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
    while f.getlength(text) > max_w and f.size > 18:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def fit_label(d, x, y, text, colour, size, max_w, bold=True, anchor="la"):
    f = font(SANS_B if bold else SANS, size)
    while f.getlength(text) > max_w and f.size > 18:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    fit_label(d, W // 2, y, text, colour, 32, W - 160, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, sub=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    if sub:
        fit_mono(d, x + w // 2, y + h // 2 - 18, text, colour, size, w - 16, "mm")
        fit_label(d, x + w // 2, y + h // 2 + 22, sub, MUTED, 24, w - 16, False, "mm")
    else:
        fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


# ------------------------------------------------------------------ figures

def library_and_bank(img, d, cfg):
    """The core idea: what Spring Security checks for the bank, and what its defaults leave to the bank."""
    lib = [("a challenge, used once", "a replayed answer", part("TWO", "THE SAME ANSWER")[1]),
           ("the origin", "a look-alike page", part("THREE", "REQUEST 3.")[2]),
           ("the signature", "another key", part("ONE", "A COPY OF THE RECORDS")[0])]
    defaults = part("FOUR", "SPRING SECURITY'S DEFAULTS")
    hard = part("FOUR", "HARDENED")
    cnt = part("FOUR", "THE COUNTERS")
    header(d, 60, 200, "SPRING SECURITY CHECKS", GREEN, size=28)
    for j, (what, attack, ln) in enumerate(lib):
        y = 265 + j * 125
        text, _ = verdict(ln)
        panel(d, 60, y, 820, 105, GREEN, 3)
        label(d, 85, y + 12, what, TEXT, 32, True)
        fit_label(d, 85, y + 60, attack + ": " + text, GREEN, 26, 770, False)
    header(d, 1000, 200, "LEFT TO THE BANK", AMBER, size=28)
    rows = [("a touch, no fingerprint", verdict(defaults[1])[0], verdict(hard[2])[0]),
            ("a copied key", verdict(defaults[5])[0], verdict(cnt[4])[0].split(":")[0] + ")")]
    label(d, 1000, 265, "Spring Security's defaults", MUTED, 26)
    label(d, 1440, 265, "hardened by the bank", MUTED, 26)
    for j, (what, before, after) in enumerate(rows):
        y = 310 + j * 175
        label(d, 1000, y, what, TEXT, 32, True)
        panel(d, 1000, y + 50, 420, 105, ROSE, 3)
        fit_label(d, 1020, y + 85, before.split(" (")[0], ROSE, 34, 380)
        panel(d, 1440, y + 50, 420, 105, GREEN, 3)
        fit_label(d, 1460, y + 85, after.split(" (")[0], GREEN, 34, 380)
    label(d, 1000, 670, "user verification: " + hard[0].strip().split()[-1] + ", not preferred", AMBER, 28, True)
    label(d, 1000, 715, "and a counter that must go up", AMBER, 28, True)
    caption(d, "The library checks the bytes; the bank still decides how strict to be.", AMBER, 880)


def cose_key(img, d, cfg):
    """Registration: the options the bank sends, and the COSE public key it keeps, label by label."""
    req = part("ONE", "THE REQUEST")
    cose = part("ONE", "THE COSE KEY")
    stores = part("ONE", "WHAT THE BANK STORES")
    header(d, 60, 195, "THE REQUEST (THE OPTIONS)", CLIENT, size=28)
    panel(d, 60, 255, 860, 420, CLIENT, 3)
    keys = ["challenge", "rp", "user handle", "algorithms", "user verification", "attestation"]
    for j, k in enumerate(keys):
        y = 280 + j * 62
        label(d, 85, y, k, MUTED, 28)
        fit_mono(d, 400, y, field(req, k), AMBER if k == "user verification" else TEXT, 28, 500)
    label(d, 85, 640, "random values change on every run", MUTED, 24)
    header(d, 1000, 195, "THE COSE PUBLIC KEY", GREEN, size=28)
    fit_label(d, 1000, 262, field(part("ONE", "THE RESPONSE"), "COSE public key"), MUTED, 26, 860, False)
    for j, ln in enumerate(cose):
        y = 315 + j * 72
        m = re.match(r"\s*(\S+)\s+(\S+)\s+= (.+?)(?:\s{2}\((.*)\))?$", ln)
        cell(d, 1000, y, 100, 60, m.group(1), GREEN, 28)
        label(d, 1125, y + 12, m.group(2) + " = " + m.group(3), TEXT, 30, True)
        fit_label(d, 1400, y + 15, m.group(4) or "", MUTED, 26, 460, False)
    header(d, 60, 715, "THE BANK STORES", CYAN, size=28)
    fit_label(d, 60, 780, stores[0].strip() + "; " + stores[1].strip(), TEXT, 30, 1800)
    caption(d, "A public key and a counter: a lock, not a key. Nothing the bank keeps can sign.", AMBER, 880)


def signed_bytes(img, d, cfg):
    """How the signature is made: client data hash plus 37 bytes of authenticator data, 69 bytes, signed."""
    cdata = part("TWO", "STEP 1.")
    adata = part("TWO", "STEP 2.")
    total = part("TWO", "STEP 3.")[0].strip()
    sig = part("TWO", "STEP 4.")[0].strip()
    resp = part("TWO", "THE RESPONSE")[0].strip()
    header(d, 60, 195, "1  CLIENT DATA (THE BROWSER)", CLIENT, size=28)
    panel(d, 60, 255, 1100, 170, CLIENT, 2)
    for j, ln in enumerate(cdata):
        fit_mono(d, 85, 275 + j * 48, ln.strip(), TEXT, 28, 1050)
    draw.arrow_right(d, 1160, 340, 1240, MUTED, 4)
    header(d, 1240, 195, "ITS SHA-256", CLIENT, size=28)
    cell(d, 1240, 255, 620, 170, "32 bytes", CLIENT, 34, "changes with the challenge")
    header(d, 60, 460, "2  AUTHENTICATOR DATA, 37 BYTES (THE PHONE)", AMBER, size=28)
    rh = field(adata, "rp id hash").split("  ")
    fl = field(adata, "flags").split("  ")
    ct = field(adata, "counter").split("  ")
    cell(d, 60, 520, 640, 115, rh[0], AMBER, 28, "rp id hash: " + rh[1])
    cell(d, 715, 520, 400, 115, fl[0], AMBER, 32, "flags: " + fl[1])
    cell(d, 1130, 520, 300, 115, ct[0], AMBER, 28, "counter " + ct[1])
    label(d, 1450, 552, "+", TEXT, 48, True)
    cell(d, 1510, 520, 350, 115, "32 bytes", CLIENT, 28, "SHA-256 of 1")
    header(d, 60, 675, "3  SIGNED BYTES", GREEN, size=28)
    fit_mono(d, 60, 740, total, GREEN, 36, 560)
    header(d, 680, 675, "4  SIGNATURE", GREEN, size=28)
    fit_mono(d, 680, 745, sig, TEXT, 26, 600)
    header(d, 1340, 675, "SPRING SECURITY", CYAN, size=28)
    fit_mono(d, 1340, 740, resp.split(" (")[0], GREEN, 34, 520)
    caption(d, "The challenge, the origin, the site's name, the flags and the counter are all inside one signature.", AMBER, 850)
    fit_label(d, W // 2, 905, part("TWO", "THE SAME ANSWER")[-1].strip(), ROSE, 28, W - 200, True, "ma")


def origin_check(img, d, cfg):
    """The browser's site rule worked out on the two host names, and the origin Spring Security finds in the signed client data."""
    rule = part("THREE", "REQUEST 2.")
    rp = re.search(r"RP ID (\S+):", heading("THREE", "REQUEST 2.")).group(1)
    host = re.search(r"is (\S+) equal", rule[0]).group(1)
    skipped = part("THREE", "REQUEST 3.")
    header(d, 80, 200, "THE BROWSER'S RULE", CLIENT, size=28)
    panel(d, 80, 260, 1760, 350, CLIENT, 3)
    label(d, 110, 290, "page host", MUTED, 30)
    fit_mono(d, 300, 284, host, ROSE, 38, 620)
    label(d, 990, 290, "asked-for rp id", MUTED, 30)
    fit_mono(d, 1250, 284, rp, GREEN, 38, 560)
    for j, ln in enumerate(rule[:2]):
        q, a = ln.strip().rsplit("  ", 1)
        label(d, 110, 375 + j * 65, q, TEXT, 34)
        label(d, 1780, 375 + j * 65, a, ROSE, 36, True, "ra")
    fit_label(d, 110, 530, verdict(rule[2])[0], ROSE, 34, 1700)
    header(d, 80, 660, "A BROWSER THAT SKIPPED THE RULE", AMBER, size=28)
    panel(d, 80, 720, 1760, 230, AMBER, 3)
    signed = skipped[0].split(":", 1)
    allowed = skipped[1].split(":", 1)
    label(d, 110, 750, signed[0].strip(), MUTED, 30)
    fit_mono(d, 520, 746, signed[1].strip(), ROSE, 34, 1290)
    label(d, 110, 815, allowed[0].strip() + " (the bank's setting)", MUTED, 30)
    fit_mono(d, 900, 811, allowed[1].strip(), GREEN, 34, 900)
    fit_label(d, 110, 880, "Spring Security: " + verdict(skipped[2])[0], ROSE, 32, 1700)
    caption(d, "The page cannot choose the origin: the browser writes it, and the phone signs it.", AMBER, 975)


def counters(img, d, cfg):
    """Spring Security's defaults against the hardened bank: the flags byte and the counter."""
    de = part("FOUR", "SPRING SECURITY'S DEFAULTS")
    ha = part("FOUR", "HARDENED")
    cnt = part("FOUR", "THE COUNTERS")
    header(d, 60, 195, "SPRING SECURITY'S DEFAULTS", ROSE, size=28)
    for j in range(3):
        y = 265 + j * 135
        what = de[2 * j].strip().rstrip(":")
        text, c = verdict(de[2 * j + 1])
        panel(d, 60, y, 860, 115, ROSE, 3)
        fit_label(d, 85, y + 12, what, TEXT, 30, 810)
        fit_label(d, 85, y + 62, text + ("  <- " + de[2 * j + 1].split("<- ")[1] if "<-" in de[2 * j + 1] else ""), ROSE, 26, 810, False)
    label(d, 60, 690, "user verification " + field(part("TWO", "THE REQUEST"), "user verification") + ": a touch is enough", MUTED, 28)
    header(d, 1000, 195, "HARDENED: VERIFIED, AND A RISING COUNTER", GREEN, size=28)
    seen = re.search(r"seen (\d+)", cnt[0]).group(1)
    rows = [(ha[1].strip().rstrip(":"), verdict(ha[2])), (ha[3].strip().rstrip(":"), verdict(ha[4])),
            ("the bank has seen " + seen, None)]
    for k in (1, 3):
        rows.append((cnt[k].strip(), verdict(cnt[k + 1])))
    for j, (what, v) in enumerate(rows):
        y = 265 + j * 120
        c = MUTED if v is None else v[1]
        panel(d, 1000, y, 860, 105, c, 3)
        fit_label(d, 1025, y + 12, what, TEXT, 30, 810)
        if v:
            fit_label(d, 1025, y + 60, v[0], c, 26, 810, False)
    caption(d, "Spring Security asks for a fingerprint and records the counter; the bank must insist and compare.", AMBER, 900)


def ways_in(img, d, cfg):
    """Every way into an account, and the weakest one that is still switched on."""
    ben = part("FIVE", "BEN'S WAYS IN")
    lost = part("FIVE", "BEN'S PHONE IS LOST")
    on = part("FIVE", "ANA'S OLD PASSWORD")[0]
    off = part("FIVE", "ANA'S PASSWORD, SWITCHED OFF")[0]
    keep = part("FIVE", "BEN, WITH NO PASSKEY")[0]
    header(d, 60, 195, "BEN LOSES HIS PHONE", CLIENT, size=28)
    fit_label(d, 60, 260, ben[0].strip(), MUTED, 28, 860, False)
    for j in range(4):
        y = 315 + j * 140
        name = lost[2 * j].strip().rstrip(":")
        nxt = lost[2 * j + 1].strip()
        text, c = verdict(nxt) if re.search(r"accepted|refused|nothing", nxt) else (nxt, GREEN)
        panel(d, 60, y, 860, 120, c, 3)
        fit_label(d, 85, y + 14, name, TEXT, 30, 810)
        fit_label(d, 85, y + 66, text, c, 26, 810, False)
    header(d, 1000, 195, "THE PASSWORD BESIDE THE PASSKEYS", AMBER, size=28)
    rows = (("ana's password, still switched on", on, True), (heading("FIVE", "ANA'S PASSWORD, SWITCHED OFF").strip().rstrip(":").lower(), off, True),
            ("ben, with no passkey, keeps it", keep, False))
    for j, (title, ln, flip) in enumerate(rows):
        y = 265 + j * 165
        text, c = verdict(ln)
        if flip:
            c = ROSE if c == GREEN else GREEN      # a working password is the bad outcome here
        else:
            c = AMBER
        panel(d, 1000, y, 860, 140, c, 3)
        fit_label(d, 1025, y + 18, title, TEXT, 30, 810)
        fit_label(d, 1025, y + 80, text, c, 28, 810, False)
    caption(d, "Recovery and any password left on are now the ways in to guard.", AMBER, 900)
