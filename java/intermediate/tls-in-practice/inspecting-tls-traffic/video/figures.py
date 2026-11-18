"""Pictures for the TLS key log video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and the
tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading."""
    return re.search(r"^%s\. .*\n((?:(?![A-Z]+\. ).*\n?)*)" % name, OUT, re.M).group(1).splitlines()


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


def section(name, head, n=0):
    return sections(name, head)[n]


def facts(lines):
    """`key   value` lines as a dict; a line that only continues the one above is joined to it."""
    merged = []
    for ln in lines:
        if ln.startswith("    ") and merged:
            merged[-1] += " " + ln.strip()
        else:
            merged.append(ln)
    lines = merged
    return dict(tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln.strip()))


def rows(lines):
    """Report rows (id, severity, finding)."""
    return [tuple(re.split(r"\s{2,}", ln.strip(), maxsplit=2)) for ln in lines]


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
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=940):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 20, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def verdict_colour(text):
    return GREEN if text.startswith("pass") else ROSE


# ------------------------------------------------------------------ figures

def opened(lines):
    """Record lines of a result section, or [] when nothing opened."""
    return [ln.strip() for ln in lines if ln.strip().startswith(("client:", "server:"))]


def three_holders(img, d, cfg):
    """One recorded login, and what three people can read from it."""
    conn = facts(section("ONE", "THE CONNECTION"))
    cap = section("ONE", "THE CAPTURE")
    with_log = opened(section("THREE", "THE RESULT"))
    res5 = section("FIVE", "THE RESULT")
    i = next(k for k, ln in enumerate(res5) if ln.startswith("ECDHE"))
    rsa, ecdhe = opened(res5[:i]), opened(res5[i:])
    header(d, 60, 175, "THE RECORDING (one login, as it crossed the network)", CYAN)
    for k, ln in enumerate(cap[2:4]):
        fit_mono(d, 60, 240 + k * 44, ln.strip(), MUTED, 28, 1000)
    label(d, 1120, 240, "inside, unseen:", MUTED, 24, True)
    fit_mono(d, 1120, 280, conn["client sends"], TEXT, 22, 760)
    holders = (("AN OBSERVER, NO KEY", ROSE, ["[] nothing opens"]),
               ("A DEVELOPER, WITH THE KEY LOG", GREEN, with_log),
               ("THE SERVER KEY, LEAKED: STATIC RSA", ROSE, rsa),
               ("THE SERVER KEY, LEAKED: ECDHE", GREEN, ecdhe or ["[] nothing opens"]))
    for k, (title, colour, lines) in enumerate(holders):
        y = 370 + k * 135
        panel(d, 60, y, 1800, 120, colour, 3)
        header(d, 80, y + 38, title, colour, 560, 22)
        for j, ln in enumerate(lines[:2]):
            fit_mono(d, 680, y + 22 + j * 44, ln, TEXT, 28, 1160)
    caption(d, "The key log opens what it lists; a leaked server key opens only the static RSA recording.", AMBER, 935)


def record_bytes(img, d, cfg):
    """The seventy bytes of the client's record: the header worked out, the rest sealed."""
    hdr = section("ONE", "THE RECORD HEADER")
    typ, ver, ln_ = [re.split(r"\s{2,}", h.strip(), maxsplit=1) for h in hdr[:3]]
    total = re.search(r"= (\d+) bytes", hdr[3]).group(1)
    length, text, tag = [int(n) for n in re.search(r"length (\d+) = (\d+) text \+ 1 inner type \+ (\d+) tag", ln_[1]).groups()]
    header(d, 60, 175, "ONE RECORD, %s BYTES, AS THE WIRE SHOWS IT" % total, CYAN)
    y = 260
    parts = [(typ[0], 120, CYAN, "type"), (ver[0], 170, CYAN, "version"), (ln_[0], 170, CYAN, "length"),
             ("%d bytes of text, encrypted" % text, 900, MUTED, "the login, sealed"), ("1", 120, AMBER, "inner type"),
             ("%d bytes" % tag, 320, AMBER, "tag")]
    x = 60
    for name, w, colour, what in parts:
        d.rectangle([x, y, x + w - 8, y + 80], fill=BG_CELL, outline=colour, width=3)
        fit_mono(d, x + (w - 8) / 2, y + 40, name, colour, 30, w - 24, "mm")
        label(d, x + (w - 8) / 2, y + 95, what, colour, 24, True, "ma")
        x += w
    label(d, 60 + 230, y + 140, "5 header bytes: readable", CYAN, 26, True, "ma")
    label(d, 60 + 460 + 670, y + 140, "%d sealed bytes" % length, AMBER, 26, True, "ma")
    header(d, 60, 450, "THE HEADER, WORKED OUT", AMBER)
    for i, (hexes, meaning) in enumerate((typ, ver, ln_)):
        yy = 520 + i * 60
        cell(d, 60, yy, 200, 50, hexes, CYAN, 28)
        fit_mono(d, 290, yy + 25, meaning, TEXT, 28, 1500, "lm")
    fit_mono(d, 60, 720, hdr[3].strip(), AMBER, 30, 1800)
    res = facts(section("ONE", "THE RESULT"))
    fit_mono(d, 60, 800, 'contains "password"? %s     decrypted %s' % (res['contains "password"?'], res["decrypted"]), ROSE, 30, 1800)
    caption(d, "Only the header is readable: the real content type hides inside, after the text.", AMBER, 935)


def keylog_line(img, d, cfg):
    """One key log line, part by part, and when a client may write it."""
    log = section("TWO", "THE KEY LOG")
    part = section("TWO", "THE LINE")
    sett = section("TWO", "THE SETTINGS")
    header(d, 60, 175, "ONE KEY LOG LINE (label, client random, secret)", CYAN)
    words = log[0].split()
    colours = (AMBER, CYAN, GREEN)
    f = font(MONO_B, 40)
    x = 60
    for w_, c in zip(words, colours):
        d.text((x, 250), w_, font=f, fill=c)
        x += f.getlength(w_ + " ")
    notes = [("label", part[0].split(None, 1)[1].strip(), AMBER, "which direction: client to server"),
             ("client random", part[1].split(None, 2)[2].strip(), CYAN, part[2].strip()),
             ("secret", part[3].split(None, 1)[1].strip(), GREEN, "")]
    for i, (k_, v, c, extra) in enumerate(notes):
        y = 330 + i * 80
        label(d, 60, y + 4, k_, c, 28, True)
        fit_mono(d, 330, y + 4, v, TEXT, 26, 800)
        fit_mono(d, 1150, y + 4, extra, MUTED, 24, 720)
    header(d, 60, 600, "THE SETTINGS (may this process write one?)", AMBER)
    rows_ = [("SSLKEYLOGFILE not set", sett[0].split("  ")[-1].strip(), MUTED),
             ("APP_ENV=dev", sett[1].split("  ")[-1].strip(), GREEN),
             ("APP_ENV=production", sett[3].strip().split(";")[0], ROSE)]
    for i, (k_, v, c) in enumerate(rows_):
        y = 670 + i * 70
        label(d, 60, y + 26, k_, TEXT, 28, True, "lm")
        cell(d, 520, y, 1000, 54, v, c, 26)
    caption(d, "One line opens one direction of one connection: a key log is a secret file.", AMBER, 935)


def secret_to_key(img, d, cfg):
    """From the logged secret to the AES key, the IV and each record's nonce, to the text."""
    look = facts(section("THREE", "THE LOOKUP"))
    work = section("THREE", "FROM SECRET")
    key = re.split(r"\s{2,}", work[0].strip())
    iv = re.split(r"\s{2,}", work[1].strip())
    n0 = re.search(r"record 0 ends (.*)", work[2]).group(1)
    n1 = re.search(r"record 1 ends (.*)", work[3]).group(1)
    res = opened(section("THREE", "THE RESULT"))
    header(d, 60, 175, "THE SECRET, FOUND BY THE CLIENT RANDOM", CYAN)
    cell(d, 60, 240, 520, 60, look["client random"], CYAN, 28)
    draw.arrow_right(d, 600, 270, 700, MUTED, 4)
    cell(d, 720, 240, 620, 60, look["CLIENT_TRAFFIC_SECRET_0"].replace(" found", ""), GREEN, 28)
    label(d, 1360, 270, "CLIENT_TRAFFIC_SECRET_0", GREEN, 24, True, "lm")
    header(d, 60, 350, "FROM SECRET TO KEY, WORKED OUT", AMBER)
    for i, step in enumerate((key, iv)):
        y = 420 + i * 80
        label(d, 60, y + 28, step[0], AMBER, 30, True, "lm")
        fit_mono(d, 180, y + 28, step[1], TEXT, 28, 780, "lm")
        cell(d, 980, y, 520, 56, step[2], AMBER, 28)
    y = 590
    label(d, 60, y + 28, "nonce", AMBER, 30, True, "lm")
    fit_mono(d, 180, y + 28, "iv XOR record number", TEXT, 28, 780, "lm")
    cell(d, 980, y, 250, 56, "0: .. " + n0, AMBER, 26)
    cell(d, 1250, y, 250, 56, "1: .. " + n1, AMBER, 26)
    header(d, 60, 690, "AES-128-GCM OPENS THE RECORD", GREEN)
    for i, ln in enumerate(res):
        fit_mono(d, 60, 760 + i * 50, ln, GREEN, 30, 1800)
    caption(d, "The key log holds the secret; the key and the nonce follow from it by fixed steps.", AMBER, 935)


def log_scope(img, d, cfg):
    """Three tries: a key log opens its own connection and direction, nothing else."""
    sec = facts(section("FOUR", "THE SECOND CONNECTION"))
    tries = [(h, opened(section("FOUR", h))) for h in ("TRY 1", "TRY 2", "TRY 3")]
    titles = [ln for ln in act("FOUR") if ln.startswith("TRY")]
    header(d, 60, 175, "TWO CUSTOMERS, TWO CLIENT RANDOMS", CYAN)
    fit_mono(d, 60, 245, "session two " + sec["client random"], TEXT, 28, 1800)
    for i, ((h, lines), title) in enumerate(zip(tries, titles)):
        y = 320 + i * 190
        colour = ROSE if not lines else GREEN if len(lines) == 2 else AMBER
        panel(d, 60, y, 1800, 170, colour, 3)
        label(d, 90, y + 18, title, colour, 30, True)
        shown = lines or ["[] (nothing opens)"]
        for j, ln in enumerate(shown):
            fit_mono(d, 90, y + 70 + j * 44, ln, TEXT, 28, 1740)
        if len(lines) == 1:
            fit_mono(d, 90, y + 114, "server: (no SERVER line: stays closed)", MUTED, 28, 1740)
    caption(d, "A line opens one direction of one connection, no more.", AMBER, 935)


def forward_secrecy(img, d, cfg):
    """The same leaked server key against a static RSA and an ECDHE recording."""
    rec = section("FIVE", "THE RECORDINGS")
    res = section("FIVE", "THE RESULT")
    why = section("FIVE", "WHY")
    i = next(k for k, ln in enumerate(res) if ln.startswith("ECDHE"))
    cols = (("STATIC RSA", ROSE, rec[0:2], opened(res[:i]), why[0]),
            ("ECDHE", GREEN, rec[2:5], opened(res[i:]), " ".join(w.strip() for w in why[1:])))
    for c, (title, colour, wire, out, reason) in enumerate(cols):
        x = 60 + c * 920
        header(d, x, 175, title, colour)
        label(d, x, 245, "on the wire", MUTED, 26, True)
        for j, ln in enumerate(wire):
            fit_mono(d, x, 285 + j * 44, re.sub(r"^(static RSA|ECDHE)\s+", "", ln.strip()), TEXT, 28, 880)
        label(d, x, 430, "the leaked server key opens", MUTED, 26, True)
        panel(d, x, 470, 880, 150, colour, 3)
        for j, ln in enumerate(out or ["[] nothing"]):
            fit_mono(d, x + 20, 495 + j * 50, ln, colour, 26, 840)
        label(d, x, 650, "why", MUTED, 26, True)
        words, lines_, cur = re.sub(r"^(static RSA|ECDHE)\s+", "", reason.strip()).split(), [], ""
        for w_ in words:
            if len(cur) + len(w_) > 46:
                lines_.append(cur)
                cur = ""
            cur += (" " if cur else "") + w_
        lines_.append(cur)
        for j, ln in enumerate(lines_):
            fit_mono(d, x, 690 + j * 50, ln, TEXT, 32, 880)
    caption(d, "Forward secrecy: a key stolen later cannot open an ephemeral recording.", AMBER, 935)
