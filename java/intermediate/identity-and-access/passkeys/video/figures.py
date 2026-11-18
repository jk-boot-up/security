"""Pictures for the passkeys video, drawn by the `figure` slide kind.

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

def one_key_one_site(img, d, cfg):
    """The core idea: ana's phone holds a key made for bank.example; the real page gets a signature, the look-alike gets nothing."""
    rp = field(part("ONE", "THE REQUEST"), "rp id")
    fake = part("THREE", "THE PAGE")[0].strip()
    real = re.search(r'"origin":"([^"]+)"', OUT).group(1)
    ok, okc = verdict(part("TWO", "THE BANK'S CHECK")[-1])
    no, noc = verdict(part("THREE", "REQUEST 1.")[0])
    # the phone in the middle
    panel(d, 760, 330, 400, 360, AMBER, 3)
    label(d, 960, 360, "ANA'S PHONE", AMBER, 30, True, "ma")
    cell(d, 800, 430, 320, 100, "key for", AMBER, 26, rp)
    label(d, 960, 570, "the private key", MUTED, 26, False, "ma")
    label(d, 960, 610, "never leaves it", MUTED, 26, False, "ma")
    # the real bank page
    header(d, 80, 220, "THE REAL BANK", GREEN)
    panel(d, 80, 280, 560, 190, GREEN, 3)
    fit_mono(d, 110, 310, real, TEXT, 30, 500)
    label(d, 110, 370, "asks: sign this challenge", MUTED, 26)
    label(d, 110, 412, "for " + rp, MUTED, 26)
    draw.arrow_right(d, 640, 375, 760, GREEN, 4)
    header(d, 1240, 220, "THE ANSWER", GREEN)
    panel(d, 1240, 280, 600, 190, GREEN, 3)
    label(d, 1270, 310, "a signature, made for " + rp, TEXT, 26, True)
    fit_label(d, 1270, 370, ok, okc, 26, 540)
    draw.arrow_right(d, 1160, 375, 1240, GREEN, 4)
    # the look-alike page
    header(d, 80, 560, "THE LOOK-ALIKE", ROSE)
    panel(d, 80, 620, 560, 190, ROSE, 3)
    fit_mono(d, 110, 650, fake, TEXT, 30, 500)
    label(d, 110, 710, "asks for a passkey,", MUTED, 26)
    label(d, 110, 752, "on a name that looks the same", MUTED, 26)
    draw.arrow_right(d, 640, 715, 760, ROSE, 4)
    header(d, 1240, 560, "THE ANSWER", ROSE)
    panel(d, 1240, 620, 600, 190, ROSE, 3)
    label(d, 1270, 650, "no key for that name", TEXT, 26, True)
    fit_label(d, 1270, 710, no.split(" (")[0], noc, 30, 540)
    draw.arrow_right(d, 1160, 715, 1240, ROSE, 4)
    caption(d, "A key cut for one door: it only turns in " + rp + ".", AMBER, 880)


def registration(img, d, cfg):
    """Registration: the request in, the new key pair out, and what each side keeps."""
    req = part("ONE", "THE REQUEST")
    resp = part("ONE", "THE RESPONSE")
    stores = part("ONE", "WHAT THE BANK STORES")
    rp = field(req, "rp id")
    header(d, 80, 200, "THE REQUEST", CLIENT)
    panel(d, 80, 260, 820, 230, CLIENT, 3)
    label(d, 110, 285, "challenge", MUTED, 26)
    fit_mono(d, 110, 325, field(req, "challenge"), TEXT, 26, 760)
    label(d, 110, 370, req[1].strip(), MUTED, 24)
    label(d, 110, 425, "rp id", MUTED, 26)
    fit_mono(d, 260, 422, rp, TEXT, 28, 600)
    draw.arrow_right(d, 900, 375, 1010, MUTED, 4)
    header(d, 1010, 200, "THE RESPONSE", GREEN)
    panel(d, 1010, 260, 830, 230, GREEN, 3)
    label(d, 1040, 285, "credential id", MUTED, 26)
    fit_mono(d, 1040, 325, field(resp, "credential id"), TEXT, 26, 770)
    label(d, 1040, 380, "public key", MUTED, 26)
    fit_mono(d, 1040, 420, field(resp, "public key"), TEXT, 26, 770)
    # who keeps what
    header(d, 80, 560, "ANA'S PHONE KEEPS", AMBER)
    panel(d, 80, 620, 820, 200, AMBER, 3)
    label(d, 110, 650, "the private key, for " + rp + " only", TEXT, 30, True)
    label(d, 110, 710, field(resp, "private key"), MUTED, 26)
    label(d, 110, 755, "it can sign; nobody else can", MUTED, 26)
    header(d, 1010, 560, "THE BANK STORES", CYAN)
    panel(d, 1010, 620, 830, 200, CYAN, 3)
    fit_label(d, 1040, 650, stores[0].strip(), TEXT, 28, 770)
    label(d, 1040, 710, stores[1].strip(), GREEN, 30, True)
    no, noc = verdict(part("ONE", "A COPY OF THE RECORDS")[0])
    fit_label(d, 1040, 760, "a copy of it, used to sign: " + no, noc, 24, 770, False)
    caption(d, "The bank keeps a lock, not a key: a stolen copy of its records cannot sign in.", AMBER, 880)


def signed_bytes(img, d, cfg):
    """How the signature is made: client data hash plus 37 bytes of authenticator data, 69 bytes, signed."""
    cdata = part("TWO", "STEP 1.")
    chash = part("TWO", "STEP 2.")
    adata = part("TWO", "STEP 3.")
    total = part("TWO", "STEP 4.")[0].strip()
    sig = part("TWO", "STEP 5.")[0].strip()
    check = part("TWO", "THE BANK'S CHECK")
    n_json = re.search(r"\((\d+) bytes", heading("TWO", "STEP 1.")).group(1)
    # client data
    header(d, 60, 190, "1  CLIENT DATA, " + n_json + " BYTES (THE BROWSER)", CLIENT)
    panel(d, 60, 245, 1000, 160, CLIENT, 2)
    for j, ln in enumerate(cdata):
        fit_mono(d, 80, 262 + j * 46, ln.strip(), TEXT, 24, 960)
    draw.arrow_right(d, 1060, 325, 1140, MUTED, 4)
    header(d, 1140, 190, "2  SHA-256 OF IT, 32 BYTES", CLIENT)
    panel(d, 1140, 245, 720, 160, CLIENT, 2)
    for j, ln in enumerate(chash):
        fit_mono(d, 1170, 280 + j * 50, ln.strip(), CLIENT, 28, 670)
    # authenticator data, as cells
    header(d, 60, 440, "3  AUTHENTICATOR DATA, 37 BYTES (THE PHONE)", AMBER)
    rh = field(adata, "rp id hash").split("  ")
    fl = field(adata, "flags").split("  ")
    ct = field(adata, "counter").split("  ")
    cell(d, 60, 500, 620, 110, rh[0], AMBER, 26, "rp id hash, 32 bytes: " + rh[1])
    cell(d, 690, 500, 330, 110, fl[0], AMBER, 30, "flags, 1 byte: " + fl[1].split(",")[0])
    cell(d, 1030, 500, 290, 110, ct[0], AMBER, 28, "counter, 4 bytes " + ct[1])
    label(d, 1340, 538, "+", TEXT, 48, True)
    cell(d, 1400, 500, 460, 110, chash[0].strip()[:12] + "...", CLIENT, 26, "hash of client data, 32 bytes")
    # signed bytes and signature
    header(d, 60, 650, "4  THE SIGNED BYTES", GREEN)
    fit_mono(d, 60, 715, total, GREEN, 34, 600)
    draw.arrow_right(d, 620, 735, 720, MUTED, 4)
    header(d, 720, 650, "5  SIGNATURE, PRIVATE KEY", GREEN)
    fit_mono(d, 735, 715, sig, TEXT, 26, 620)
    draw.arrow_right(d, 1370, 735, 1440, MUTED, 4)
    header(d, 1440, 650, "THE BANK'S CHECK", CYAN)
    fit_mono(d, 1455, 712, check[0].strip(), GREEN, 28, 405)
    label(d, 1455, 765, verdict(check[1])[0].split(" (")[0], GREEN, 32, True)
    caption(d, "The challenge, the origin, the site's name, the flags and the counter are all inside one signature.", AMBER, 860)
    no, _ = verdict(part("TWO", "FLAGS CHANGED")[0])
    fit_label(d, W // 2, 915, heading("TWO", "FLAGS CHANGED").rstrip(":").lower() + ": " + no, ROSE, 28, W - 200, True, "ma")


def origin_check(img, d, cfg):
    """The browser's site rule worked out on the two host names, and the origin the bank finds in the signed client data."""
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
    exp = skipped[1].split(":", 1)
    label(d, 110, 750, signed[0].strip(), MUTED, 30)
    fit_mono(d, 520, 746, signed[1].strip(), ROSE, 34, 1290)
    label(d, 110, 815, exp[0].strip(), MUTED, 30)
    fit_mono(d, 520, 811, exp[1].strip(), GREEN, 34, 1290)
    fit_label(d, 110, 880, verdict(skipped[2])[0], ROSE, 32, 1700)
    caption(d, "The page cannot choose the origin: the browser writes it, and the phone signs it.", AMBER, 975)


def counters(img, d, cfg):
    """User verification flags for a payment, and the counter that catches a copied key."""
    pay = part("FOUR", "A PAYMENT")
    cnt = part("FOUR", "THE COUNTERS")
    header(d, 60, 195, "A PAYMENT: THE FLAGS BYTE", CLIENT, size=28)
    for j in range(2):
        y = 265 + j * 135
        who = pay[2 * j].strip().rstrip(":")
        flags = who.rsplit(" ", 1)[1]
        text, c = verdict(pay[2 * j + 1])
        cell(d, 60, y, 120, 105, flags, c, 40)
        fit_label(d, 205, y + 8, who.rsplit(",", 1)[0], TEXT, 32, 680)
        fit_label(d, 205, y + 58, text, c, 26, 680, False)
    label(d, 60, 560, "01 = user present (a touch)", MUTED, 30)
    label(d, 60, 610, "04 = user verified (fingerprint or PIN)", MUTED, 30)
    label(d, 60, 665, "01 + 04 = 05", AMBER, 34, True)
    header(d, 960, 195, "THE COUNTER AGAINST COPIES", AMBER, size=28)
    seen = re.search(r"seen (\d+)", cnt[0]).group(1)
    rows = [("the bank has seen", seen, None)]
    for k in (1, 3):
        name = cnt[k].split(":")[0].strip()
        num = re.search(r"counter (\d+)", cnt[k]).group(1)
        rows.append((name, num, verdict(cnt[k + 1])))
    again = cnt[5].strip().rstrip(":")
    rows.append((again.split(",")[0], re.search(r"counter (\d+)", again).group(1), verdict(cnt[6])))
    for j, (name, num, v) in enumerate(rows):
        y = 265 + j * 135
        c = MUTED if v is None else v[1]
        cell(d, 960, y, 120, 105, num, c, 44)
        fit_label(d, 1105, y + 8, name, TEXT, 32, 760)
        if v:
            fit_label(d, 1105, y + 58, v[0], c, 26, 760, False)
    caption(d, "Each signature must count higher than the last one the bank saw.", AMBER, 830)
    fit_label(d, W // 2, 895, part("FOUR", "A SYNCED PASSKEY")[0].strip().split(" (")[0] + "  ->  " + act("FOUR")[-1].strip(),
              MUTED, 28, W - 200, False, "ma")


def ways_in(img, d, cfg):
    """Every way into an account, and the weakest one that is still switched on."""
    ben = part("FIVE", "BEN'S WAYS IN")
    lost = part("FIVE", "BEN'S PHONE IS LOST")
    on = part("FIVE", "ANA'S OLD PASSWORD")[0]
    off = part("FIVE", "ANA'S PASSWORD, SWITCHED OFF")[0]
    header(d, 60, 195, "BEN LOSES HIS PHONE", CLIENT, size=28)
    fit_label(d, 60, 260, ben[0].strip(), MUTED, 28, 860, False)
    fit_label(d, 60, 300, ben[1].strip().split(";")[0], MUTED, 28, 860, False)
    for j in range(4):
        y = 355 + j * 140
        name = lost[2 * j].strip().rstrip(":")
        text, c = verdict(lost[2 * j + 1])
        panel(d, 60, y, 860, 120, c, 3)
        label(d, 85, y + 14, name, TEXT, 32, True)
        fit_label(d, 85, y + 66, text, c, 26, 810, False)
    header(d, 1000, 195, "ANA'S OLD PASSWORD", AMBER, size=28)
    for j, (title, ln) in enumerate((("still switched on", on), ("switched off", off))):
        y = 270 + j * 190
        text, c = verdict(ln)
        c = ROSE if c == GREEN else GREEN          # a working password is the bad outcome here
        panel(d, 1000, y, 860, 160, c, 3)
        label(d, 1025, y + 18, title, TEXT, 34, True)
        fit_label(d, 1025, y + 85, text, c, 28, 810, False)
    label(d, 1000, 680, "a password typed into a look-alike page", MUTED, 30)
    label(d, 1000, 725, "works at the bank: the weakest way in", MUTED, 30)
    label(d, 1000, 770, "sets the level for the whole account", MUTED, 30)
    caption(d, "Recovery codes and old passwords are now the way in to guard.", AMBER, 935)
