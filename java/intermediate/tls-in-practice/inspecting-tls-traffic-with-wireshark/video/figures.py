"""Pictures for the TLS capture video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading."""
    body = re.search(r"^%s\. .*\n((?:(?![A-Z]+\. ).*\n?)*)" % name, OUT, re.M).group(1)
    return body.splitlines()


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


def section(name, head):
    return sections(name, head)[0]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def pairs(lines):
    """Lines of two columns separated by two or more spaces, as (left, right)."""
    out = []
    for ln in lines:
        parts = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        out.append((parts[0], parts[1] if len(parts) > 1 else ""))
    return out


def note_free(text):
    return text.split("  <-")[0]


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
    while f.getlength(text) > max_w and f.size > 16:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def state_colour(text):
    t = text.lower()
    return ROSE if t.startswith(("opened", "visible", "yes")) else GREEN


# ------------------------------------------------------------------ figures

def wire_and_key_log(img, d, cfg):
    """The core idea: the same captured login, read with no secrets and with the key log."""
    login = section("ONE", "THE LOGIN")
    wire = pairs(section("ONE", "THE CAPTURE"))
    size = section("ONE", "THE REQUEST'S SIZE")[1]
    req = section("THREE", "THE REQUEST")
    resp = section("THREE", "THE RESPONSE")
    cert = pairs(section("THREE", "THE CERTIFICATE"))
    header(d, 60, 200, "THE LOGIN (sent inside TLS 1.3)", CYAN)
    panel(d, 60, 255, 1800, 70, CYAN, 3)
    fit_mono(d, 90, 272, login[0] + "     " + login[1], TEXT, 28, 1740)
    header(d, 60, 360, "READ WITH NO SECRETS", GREEN, 870)
    header(d, 990, 360, "READ WITH THE KEY LOG", ROSE, 870)
    left = [("server name", wire[0][1].replace("server name ", ""), ROSE),
            ("cipher", wire[1][1], MUTED),
            ("certificate", wire[2][1], GREEN),
            ("HTTP", wire[3][1], GREEN),
            ("login record", size.split()[-2] + " bytes", AMBER)]
    right = [("request", req[0], ROSE),
             ("form", req[1].replace(" ", "").replace("=", " = ") + ",  " + req[2].replace(" ", "").replace("=", " = "), ROSE),
             ("response", resp[0], ROSE),
             ("cookie", resp[1].replace("Set-Cookie: ", ""), ROSE),
             ("certificate", "for " + note_free(cert[0][1]), ROSE)]
    for col, rows in ((60, left), (990, right)):
        panel(d, col, 420, 870, 470, LINE)
        for i, (k, v, c) in enumerate(rows):
            y = 445 + i * 88
            label(d, col + 25, y + 30, k, MUTED, 26, True, "lm")
            fit_mono(d, col + 250, y + 30, v, c, 28, 600, "lm")
    caption(d, "Same bytes. Without the log: a name, a cipher, sizes. With it: everything.", AMBER, 915)


def client_hello_bytes(img, d, cfg):
    """The first record, field by field, and the size of the login record worked out."""
    by_hand = pairs(section("ONE", "THE CLIENT HELLO"))
    size = section("ONE", "THE REQUEST'S SIZE")
    header(d, 60, 200, "THE CLIENT HELLO, BY HAND (in the clear)", CYAN)
    fields = [("record type", by_hand[0][1], 260, CYAN),
              ("version, length", "4 bytes", 260, LINE),
              ("message type", by_hand[1][1], 290, CYAN),
              ("random", by_hand[2][1].split(",")[0], 290, AMBER),
              ("suites, keys...", "...", 250, LINE),
              ("server_name", note_free(by_hand[3][1]), 390, ROSE)]
    x = 60
    for name, value, w, c in fields:
        label(d, x + w // 2, 270, name, MUTED, 22, True, "ma")
        cell(d, x, 305, w - 10, 90, value, c, 26)
        x += w
    label(d, 60, 420, "random: " + by_hand[2][1].split(", ")[1] + ";  the name: anyone on the path reads it",
          TEXT, 28)
    header(d, 60, 500, "THE LOGIN RECORD'S SIZE, WORKED OUT", AMBER)
    m = re.match(r"request (\d+) \+ type (\d+) \+ tag (\d+) = (\d+) bytes", size[0])
    parts = [(m.group(1), "request bytes"), (m.group(2), "content type"), (m.group(3), "AES-GCM tag"),
             (m.group(4), "on the wire")]
    for j, (v, note) in enumerate(parts):
        x = 60 + j * 470
        cell(d, x, 570, 330, 100, v, GREEN if j == 3 else AMBER, 44)
        label(d, x + 165, 685, note, MUTED, 26, True, "ma")
        if j < 3:
            label(d, x + 400, 620, "+" if j < 2 else "=", TEXT, 52, True, "mm")
    fit_mono(d, 60, 770, "tshark, the largest record the client sent: " + size[1].split()[-2] + " bytes", GREEN, 28, 1800)
    caption(d, "Encryption hides the bytes, not how many there are.", AMBER, 880)


def key_log_line(img, d, cfg):
    """One key log line taken apart, the secret's size worked out, and what each of the five secrets opens."""
    parts = pairs(section("TWO", "ONE LINE"))
    work = section("TWO", "THE SECRET'S SIZE")[0]
    labels = pairs(section("TWO", "THE KEY LOG"))
    header(d, 60, 200, "ONE LINE OF keys.log, TAKEN APART", CYAN)
    cols = [("label", parts[0][1], 620, CYAN), ("client random", parts[1][1], 520, AMBER),
            ("secret", parts[2][1], 620, ROSE)]
    x = 60
    for name, value, w, c in cols:
        label(d, x + w // 2, 265, name, MUTED, 24, True, "ma")
        cell(d, x, 300, w - 20, 80, value, c, 26)
        x += w
    header(d, 60, 420, "THE SECRET'S SIZE, WORKED OUT", AMBER)
    steps = work.replace(" bytes", "").split(" -> ")
    nums = steps[2].split(" = ")
    chain = [(steps[0], CYAN, 620), (steps[1], CYAN, 220), (nums[0], AMBER, 260), (nums[1] + " bytes", GREEN, 260)]
    x = 60
    for j, (t, c, w) in enumerate(chain):
        cell(d, x, 485, w, 76, t, c, 28)
        if j < 3:
            label(d, x + w + 40, 523, "=" if j == 2 else "->", TEXT, 36, True, "mm")
        x += w + 80
    header(d, 60, 600, "THE FIVE LABELS, AND WHAT EACH SECRET OPENS", GREEN)
    for i, (lab, opens) in enumerate(labels):
        y = 660 + i * 50
        fit_mono(d, 60, y, lab, TEXT, 26, 640)
        label(d, 760, y, opens, ROSE if "data" in opens else MUTED, 28, "data" in opens)
    caption(d, "Every line names one connection by its client random.", AMBER, 925)


def opened_by_secret(img, d, cfg):
    """Which secret of the log opened which part of the capture, with the real values."""
    req = section("THREE", "THE REQUEST")
    resp = section("THREE", "THE RESPONSE")
    cert = section("THREE", "THE CERTIFICATE")
    rows = [(heading("THREE", "THE REQUEST"), req, ROSE),
            (heading("THREE", "THE RESPONSE"), resp, AMBER),
            (heading("THREE", "THE CERTIFICATE"), [note_free(c) for c in cert], CYAN)]
    header(d, 60, 200, "THE SAME CAPTURE, WITH tshark -o tls.keylog_file:keys.log", CYAN)
    y = 270
    for title, lines, c in rows:
        what, secret = title.split(" (opened with ")
        h = 40 + 58 * len(lines)
        panel(d, 60, y, 1800, h, c, 3)
        label(d, 90, y + 24, what.replace("THE ", "the ").lower(), c, 34, True)
        label(d, 90, y + 74, "opened with", MUTED, 22)
        fit_mono(d, 90, y + 102, secret.rstrip(")"), MUTED, 24, 580)
        for j, ln in enumerate(lines):
            fit_mono(d, 720, y + 24 + j * 58, ln, TEXT, 34, 1110)
        y += h + 28
    caption(d, "In TLS 1.3 even the certificate is encrypted; the log opens it too.", AMBER, 905)


def one_connection(img, d, cfg):
    """A log matches a capture only by client random; the client's lines open only the client's side."""
    inputs = section("FOUR", "THE INPUT")
    match = pairs(section("FOUR", "THE MATCH"))
    result = pairs(section("FOUR", "THE RESULT"))
    before = section("FOUR", "BEFORE")[0]
    after = note_free(section("FOUR", "AFTER")[0])
    header(d, 60, 200, "THE LOG HAS login2's CLIENT RANDOM?  ->  tshark ON login2.pcap", CYAN)
    for i, ((log, m), (_, r)) in enumerate(zip(match, result)):
        y = 270 + i * 100
        label(d, 60, y + 38, log, TEXT, 30, True, "lm")
        cell(d, 260, y, 300, 76, m.split("  (")[0], GREEN if m.startswith("yes") else ROSE, 30)
        draw.arrow_right(d, 590, y + 38, 680, MUTED, 4)
        cell(d, 700, y, 1160, 76, r, ROSE if r.startswith("closed") else GREEN, 28)
    fit_mono(d, 60, 575, "both.log = keys.log and keys2.log in one file;  " + inputs[0].split("  ")[0] + " is the first login", MUTED, 24, 1800)
    header(d, 60, 640, heading("FOUR", "BEFORE"), CYAN)
    cell(d, 60, 700, 1800, 64, before, GREEN, 28)
    header(d, 60, 790, heading("FOUR", "AFTER"), AMBER)
    cell(d, 60, 850, 1800, 64, after, AMBER, 28)
    caption(d, "A log opens the connections it names, and only the sides it holds.", AMBER, 945)


def key_exchange(img, d, cfg):
    """Ephemeral keys against RSA key exchange: what the server's key does, and what its leak opens."""
    rec = dict(pairs(section("FIVE", "THE RECORDINGS")))
    kex = dict(pairs(section("FIVE", "THE KEY EXCHANGE")))
    rule = dict(pairs(section("FIVE", "THE RULE")))
    leak = dict(pairs(section("FIVE", "THE LEAK")))
    cols = [("TLS 1.2, nginx", "ECDHE: BOTH SIDES MAKE A ONE-TIME KEY", GREEN),
            ("TLS 1.2, old", "RSA: THE SECRET TRAVELS LOCKED", ROSE)]
    for b, (who, title, c) in enumerate(cols):
        x = 60 + b * 930
        header(d, x, 200, title, c, 870, 24)
        fit_mono(d, x, 270, rec[who], TEXT, 26, 870)
        panel(d, x, 320, 870, 300, c, 3)
        if b == 0:
            steps = ["client: one-time key  ->", "<-  server: one-time key, signed", "each side works out the secret",
                     "the secret never crosses the wire"]
        else:
            steps = ["client picks the secret", "locks it with the server's public key  ->",
                     "server unlocks it with its private key", "the secret crosses the wire, locked"]
        for j, s in enumerate(steps):
            label(d, x + 25, 345 + j * 66, s, TEXT if j < 3 else c, 28, j == 3)
        label(d, x, 650, "by hand: " + rule[who], MUTED, 26, True)
        label(d, x, 700, "THE LEAK (the server's private key):", MUTED, 24, True)
        cell(d, x, 745, 870, 90, note_free(leak[who]), c, 30)
    fit_mono(d, 60, 860, "TLS 1.3: " + kex["TLS 1.3, nginx"], GREEN, 26, 1800)
    caption(d, "A leaked key opens every old recording made with RSA key exchange.", AMBER, 920)
