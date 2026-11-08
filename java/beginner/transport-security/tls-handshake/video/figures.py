"""Pictures for the TLS handshake video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("BEFORE THE KEYS")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"[\s:]+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE TLS: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def cells(d, x, y, items, colour, w=88, h=72, size=34, gap=8):
    for i, t in enumerate(items):
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=8, fill=BG_CELL, outline=colour, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=colour, anchor="mm")
    return x + len(items) * (w + gap) - gap


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def field(d, x, y, w, h, text, colour, size=28):
    """A value in a dark box, in mono."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B, size), fill=colour, anchor="mm")


def mark(d, x, y, ok):
    d.text((x, y), "✓" if ok else "✗", font=font(SANS_B, 48), fill=GREEN if ok else ROSE, anchor="mm")


def record_bar(d, x, y, total_w, h, parts):
    """A record drawn roughly to scale: parts are (bytes, text, note, colour). A part too small for its
    text gets the width it needs, and the others share the rest. Returns the x where each part starts."""
    need = [max(font(MONO_B, 24).getlength(t), font(SANS_B, 22).getlength(n)) + 30 for _, t, n, _ in parts]
    fixed = [i for i, p in enumerate(parts) if total_w * p[0] / sum(q[0] for q in parts) < need[i]]
    rest = total_w - sum(need[i] for i in fixed)
    share = sum(p[0] for i, p in enumerate(parts) if i not in fixed)
    starts = []
    for i, (n, text, note, c) in enumerate(parts):
        w = int(need[i]) if i in fixed else int(rest * n / share)
        starts.append((x, w))
        d.rounded_rectangle([x, y, x + w - 6, y + h], radius=8, fill=BG_CELL, outline=c, width=3)
        d.text((x + (w - 6) // 2, y + h // 2), text, font=font(MONO_B, 24), fill=c, anchor="mm")
        label(d, x + (w - 6) // 2, y + h + 12, note, c, 22, True, "ma")
        x += w
    return starts


def record_parts():
    """The 72 bytes of the order's record, read from act ONE."""
    made = section("ONE", "HOW THE")
    head = value("header", "ONE", "AFTER TLS").split("  ")[0]
    order, typ, pad, tag = map(int, re.search(r"(\d+) order \+ (\d+) type \+ (\d+) padding \+ (\d+) tag", made).groups())
    return head, order, typ, pad, tag


# ------------------------------------------------------------------ figures

def tls_jobs(img, d, cfg):
    """The core idea on real values: TLS agrees keys, checks who is there, and protects every record."""
    chosen = value("chosen", "TWO", "BEFORE THE KEYS")
    trusts = value("trusts", "THREE", "BEFORE THE HANDSHAKE").split()[-1]
    head, order, typ, pad, tag = record_parts()
    total = re.search(r"one record of (\d+) bytes", section("ONE")).group(1)
    refused = value("refused,", "FOUR", "AFTER THE CHECK").split("  <-")[0]
    rows = [("1. Agree keys", "the handshake", chosen, "keys made, never sent", CLIENT),
            ("2. Check who", "the certificate", "trusted: %s..." % trusts[:8], "an impostor is refused", AMBER),
            ("3. Protect data", "every record", "%s + %d encrypted bytes" % (head, int(total) - 5), "one changed bit is refused", GREEN)]
    label(d, 100, 215, "the job", MUTED, 26, True)
    label(d, 470, 215, "a real example from the demo", MUTED, 26, True)
    label(d, 1390, 215, "what it gives", MUTED, 26, True)
    for i, (job, sub, val, gives, c) in enumerate(rows):
        y = 265 + i * 190
        d.rounded_rectangle([100, y, 430, y + 140], radius=16, fill=PANEL, outline=c, width=4)
        d.text((265, y + 52), job, font=font(SANS_B, 34), fill=c, anchor="mm")
        d.text((265, y + 98), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")
        field(d, 470, y + 30, 840, 80, val, c, 28)
        draw.arrow_right(d, 1320, y + 70, 1380, c, 4)
        d.rounded_rectangle([1390, y + 38, 1820, y + 102], radius=32, fill=c)
        d.text((1605, y + 70), gives, font=font(SANS_B, 26), fill=DARK, anchor="mm")
    label(d, W // 2, 845, "(the third check, on the tampered record: %s)" % refused, MUTED, 26, False, "ma")
    caption(d, "Agree keys in public, check who answered, then protect every record.", AMBER, 900)


def sniffer_view(img, d, cfg):
    """The same order before TLS, readable, and after TLS, one record of 72 bytes."""
    order = section("ONE", "BEFORE TLS").split("\n")[0].strip()
    readable = re.search(r"readable: (\w+)", section("ONE", "BEFORE TLS")).group(1)
    hidden = value("the card number readable", "ONE", "AFTER TLS")
    head, n_order, typ, pad, tag = record_parts()
    total = re.search(r"one record of (\d+) bytes", section("ONE")).group(1)
    header(d, 100, 205, "BEFORE TLS: what the sniffer sees", GREEN)
    card = re.search(r"\d{16}", order).span()
    x = 100
    for i, ch in enumerate(order):
        c = ROSE if card[0] <= i < card[1] else TEXT
        d.rounded_rectangle([x, 270, x + 44, 334], radius=6, fill=BG_CELL, outline=c, width=2)
        d.text((x + 22, 302), ch, font=font(MONO_B, 28), fill=c, anchor="mm")
        x += 50
    label(d, 100, 350, "%d bytes; the card number (rose) is readable: %s" % (len(order), readable), ROSE, 28, True)
    header(d, 100, 440, "AFTER TLS: one record of %s bytes" % total, ROSE)
    parts = [(5, head, "header, in the clear", AMBER), (n_order, "the order, encrypted", "%d bytes" % n_order, CYAN),
             (typ, "", "%d type" % typ, MUTED), (pad, "padding", "%d bytes" % pad, MUTED), (tag, "tag", "%d bytes" % tag, GREEN)]
    record_bar(d, 100, 505, 1720, 80, parts)
    label(d, 100, 640, "17 = record type 23, application data    03 03 = version label    00 43 = %d bytes follow"
          % (int(total) - 5), TEXT, 28)
    label(d, 100, 700, "%d header + %d order + %d type + %d padding + %d tag = %s bytes" % (5, n_order, typ, pad, tag, total),
          AMBER, 30, True)
    label(d, 100, 760, "the card number readable: %s" % hidden, GREEN, 30, True)
    caption(d, "The sniffer sees a type, a length and noise. The tag lets the server spot any change.", TEXT, 860)


def handshake_flow(img, d, cfg):
    """Who sends what, in order: two readable hellos, then everything encrypted."""
    hello = section("TWO", "BEFORE THE KEYS")
    host = re.search(r"the site name (\S+) readable: (\w+)", hello)
    proto, suite = value("chosen", "TWO", "BEFORE THE KEYS").split(", ")
    cert = value("the certificate readable", "TWO", "AFTER THE KEYS").split()[0]
    label(d, 330, 210, "Client", CLIENT, 32, True, "ma")
    label(d, 1110, 210, "Server", GREEN, 32, True, "ma")
    for x, c in ((330, CLIENT), (1110, GREEN)):
        d.line([(x, 255), (x, 830)], fill=c, width=3)
    msgs = [(True, "22", "ClientHello: versions, a temporary key, the name", AMBER),
            (False, "22", "ServerHello: %s, its temporary key" % proto, AMBER),
            (False, "20", "change cipher spec (for old boxes)", MUTED),
            (False, "23", "encrypted: certificate, proof, Finished", GREEN),
            (True, "23", "encrypted: Finished", GREEN),
            (True, "23", "encrypted: the order", GREEN)]
    for i, (right, typ, text, c) in enumerate(msgs):
        y = 285 + i * 92
        if right:
            draw.arrow_right(d, 335, y + 30, 1105, c, 4)
        else:
            draw.arrow_left(d, 1105, y + 30, 335, c, 4)
        d.rounded_rectangle([350, y - 16, 410, y + 18], radius=8, fill=c)
        d.text((380, y + 1), typ, font=font(MONO_B, 22), fill=DARK, anchor="mm")
        label(d, 425, y + 1, text, c if c != MUTED else TEXT, 24, True, "lm")
    d.line([(100, 438), (1150, 438)], fill=ROSE, width=2)
    label(d, 100, 400, "keys from here", ROSE, 24, True)
    header(d, 1300, 215, "THE CIPHER SUITE", AMBER)
    label(d, 1300, 275, suite, TEXT, 28, True)
    bits = suite.split("_")
    parts = [(bits[0], "the protocol"), ("%s %s" % (bits[1], bits[2]), "cipher, %s-bit key" % bits[2]),
             (bits[3], "mode: encrypt + tag"), (bits[4], "hash: makes keys")]
    for i, (k, v) in enumerate(parts):
        y = 330 + i * 76
        field(d, 1300, y, 200, 60, k, CYAN, 26)
        label(d, 1520, y + 30, v, TEXT, 26, False, "lm")
    label(d, 1300, 650, "readable: the site name, %s" % host.group(2), AMBER, 26, True)
    label(d, 1300, 700, "readable: the certificate, %s" % cert, GREEN, 26, True)
    caption(d, "Only the two hellos are in the clear. Both sides mix the temporary keys into one secret.", TEXT, 880)


def identity_checks(img, d, cfg):
    """Three servers answer; the client checks the certificate is trusted, and the name matches."""
    asks = value("asks for", "THREE", "BEFORE THE HANDSHAKE")
    trusts = value("trusts", "THREE", "BEFORE THE HANDSHAKE").split()[-1]
    after = section("THREE", "AFTER THE HANDSHAKE")
    works = re.findall(r"works: (\w+)", after)
    fake = re.search(r"impostor, same name, certificate (\w+)", after).group(1)
    other = re.search(r"asked for (\S+)", after).group(1)
    header(d, 100, 205, "BEFORE THE HANDSHAKE: the client", GREEN)
    label(d, 100, 275, "asks for", MUTED, 26, True)
    field(d, 240, 262, 520, 56, asks, CLIENT, 26)
    label(d, 800, 275, "trusts the certificate", MUTED, 26, True)
    field(d, 1110, 262, 520, 56, trusts, CLIENT, 26)
    header(d, 100, 360, "AFTER THE HANDSHAKE: three servers", ROSE)
    label(d, 760, 425, "certificate", MUTED, 24, True, "ma")
    label(d, 1180, 425, "trusted?", MUTED, 24, True, "ma")
    label(d, 1390, 425, "name matches?", MUTED, 24, True, "ma")
    label(d, 1680, 425, "works", MUTED, 24, True, "ma")
    rows = [("the shop", "asked " + asks, trusts, True, True, works[0]),
            ("an impostor", "asked " + asks, fake, False, True, works[1]),
            ("the shop", "asked " + other, trusts, True, False, works[2])]
    for i, (who, sub, fp, trusted, named, ok) in enumerate(rows):
        y = 465 + i * 110
        c = GREEN if ok == "true" else ROSE
        d.rounded_rectangle([100, y, 1820, y + 90], radius=14, fill=PANEL, outline=c, width=3)
        label(d, 125, y + 30, who, TEXT, 26, True, "lm")
        label(d, 125, y + 64, sub, MUTED, 22, False, "lm")
        field(d, 590, y + 15, 340, 60, fp, CLIENT if trusted else ROSE, 24)
        mark(d, 1180, y + 45, trusted)
        mark(d, 1390, y + 45, named)
        label(d, 1680, y + 45, ok, c, 30, True, "mm")
    caption(d, "Same name, other fingerprint: refused. Trusted, but the wrong name: refused too.", AMBER, 830)


def tamper_check(img, d, cfg):
    """One flipped bit in a record: the tag no longer matches, so the server refuses it."""
    head, n_order, typ, pad, tag = record_parts()
    at, total = re.search(r"byte (\d+) of (\d+)", section("FOUR", "BEFORE THE CHECK")).groups()
    refused = value("refused,", "FOUR", "AFTER THE CHECK").split("  <-")[0]
    same = value("the same bytes on the wire both times", "FOUR", "AFTER SENDING").split()[0]
    back = value("the server opens it, and gets the order back", "FOUR", "AFTER SENDING")
    header(d, 100, 205, "AFTER SENDING: the same order, two connections", GREEN)
    label(d, 100, 275, "the same bytes on the wire both times: %s    (fresh keys each time)" % same, TEXT, 28)
    label(d, 100, 320, "untouched, the server opens it and gets the order back: %s" % back, TEXT, 28)
    header(d, 100, 400, "BEFORE THE CHECK: an attacker flips one bit", ROSE)
    parts = [(5, head, "header", AMBER), (n_order + typ + pad, "encrypted", "%d bytes" % (n_order + typ + pad), CYAN),
             (tag, "tag", "%d bytes" % tag, GREEN)]
    (ex, ew) = record_bar(d, 100, 470, 1720, 76, parts)[1]
    x = ex + int(ew * (int(at) - 5) / (n_order + typ + pad))
    d.rounded_rectangle([x - 8, 462, x + 30, 554], radius=6, outline=ROSE, width=5)
    label(d, x + 11, 590, "byte %s of %s, xor 01" % (at, total), ROSE, 26, True, "ma")
    header(d, 100, 660, "AFTER THE CHECK: the server recomputes the tag", AMBER)
    field(d, 100, 725, 1100, 70, refused, ROSE, 30)
    mark(d, 1250, 760, False)
    caption(d, "The key makes the tag, so no one without the key can fix it. TLS spots every change.", TEXT, 860)


def version_matrix(img, d, cfg):
    """Which client offers work against a default server, and against one that requires TLS 1.3."""
    panels = [("BEFORE TIGHTENING: a default server", section("FIVE", "BEFORE TIGHTENING"), GREEN),
              ("AFTER TIGHTENING: TLS 1.3 required", section("FIVE", "AFTER TIGHTENING"), ROSE)]
    for i, (title, text, c) in enumerate(panels):
        x = 100 + i * 880
        header(d, x, 205, title, c)
        for j, (ver, ok, agreed) in enumerate(re.findall(r"offers (\S+)\s+works: (\w+)(?:, agreed (\S+))?", text)):
            y = 285 + j * 110
            good = ok == "true"
            d.rounded_rectangle([x, y, x + 820, y + 88], radius=14, fill=PANEL, outline=GREEN if good else ROSE, width=3)
            label(d, x + 24, y + 44, "client offers", MUTED, 26, False, "lm")
            label(d, x + 210, y + 44, ver, TEXT, 30, True, "lm")
            label(d, x + 720, y + 44, ("agreed " + agreed) if good else "refused", GREEN if good else ROSE, 28, True, "rm")
            mark(d, x + 770, y + 44, good)
    header(d, 100, 540, "THE BILL", AMBER)
    for k, ln in enumerate(l.strip() for l in section("FIVE", "THE BILL").strip().split("\n")):
        label(d, 130 + (k % 2) * 880, 610 + (k // 2) * 60, "· " + ln, TEXT, 28)
    caption(d, "Old versions stay off. Requiring TLS 1.3 also shuts out TLS 1.2 clients.", AMBER, 830)
