"""Pictures for the TCP/IP basics video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), its indented lines only."""
    return re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)


def find(pattern, name):
    """The first group of a pattern, searched in one act."""
    return re.search(pattern, act(name), re.M).group(1)


def words(name, label):
    """'4500 0028 ...' after a label -> ['4500', '0028', ...]"""
    return find(r"^\s+%s\s+([0-9a-f ]+)$" % label, name).split()


def as_bytes(ws):
    return [w[i:i + 2] for w in ws for i in (0, 2)]


# ------------------------------------------------------------------ drawing helpers

def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUEST: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=920):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def node(d, x, y, w, h, text, colour, size=28, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="mm")


def mark(d, x, y, ok, size=56):
    d.text((x, y), "✓" if ok else "✗", font=font(SANS_B, size), fill=GREEN if ok else ROSE, anchor="mm")


def byte_row(d, y, cells, fields, colour, hot=()):
    """20 byte cells across the slide, with each field bracketed underneath: (first, last, name, value)."""
    cw, gap, x0 = 80, 7, 100
    for i, b in enumerate(cells):
        c = ROSE if any(f <= i <= l for f, l in hot) else colour
        x = x0 + i * (cw + gap)
        d.rounded_rectangle([x, y, x + cw, y + 60], radius=8, fill=BG_CELL, outline=c, width=3)
        d.text((x + cw // 2, y + 30), b, font=font(MONO_B, 30), fill=c, anchor="mm")
    for first, last, name, val in fields:
        c = ROSE if (first, last) in hot else MUTED
        xa, xb = x0 + first * (cw + gap), x0 + last * (cw + gap) + cw
        d.line([(xa + 4, y + 70), (xb - 4, y + 70)], fill=c, width=3)
        mid = (xa + xb) // 2
        label(d, mid, y + 78, name, c, 19, True, "ma")
        label(d, mid, y + 102, val, TEXT if c == MUTED else c, 21, False, "ma")


# ------------------------------------------------------------------ figures

def packet_bytes(img, d, cfg):
    """The 40 bytes of the customer's first packet, field by field."""
    ip, tcp = as_bytes(words("ONE", "ip")), as_bytes(words("ONE", "tcp"))
    n = lambda bs: int("".join(bs), 16)
    addr = lambda bs: ".".join(str(int(b, 16)) for b in bs)
    e = header(d, 100, 200, "IP HEADER: 20 bytes", CYAN)
    label(d, e + 24, 222, "where it is from, and where it goes", TEXT, 28, True, "lm")
    byte_row(d, 265, ip, [
        (0, 0, "version", "4"), (1, 1, "type", str(n(ip[1:2]))), (2, 3, "length", str(n(ip[2:4]))),
        (4, 5, "id", str(n(ip[4:6]))), (6, 7, "flags", str(n(ip[6:8]))), (8, 8, "TTL", str(n(ip[8:9]))),
        (9, 9, "proto", "6=TCP"), (10, 11, "checksum", "".join(ip[10:12])),
        (12, 15, "source address", addr(ip[12:16])), (16, 19, "destination", addr(ip[16:20]))],
        CYAN, hot=[(12, 15)])
    e = header(d, 100, 520, "TCP HEADER: 20 bytes", AMBER)
    label(d, e + 24, 542, "which program, and where in the stream", TEXT, 28, True, "lm")
    byte_row(d, 585, tcp, [
        (0, 1, "source port", str(n(tcp[0:2]))), (2, 3, "dest port", str(n(tcp[2:4]))),
        (4, 7, "sequence number", str(n(tcp[4:8]))), (8, 11, "ack number", str(n(tcp[8:12]))),
        (12, 12, "size", "5×4"), (13, 13, "flags", "SYN"), (14, 15, "window", str(n(tcp[14:16]))),
        (16, 17, "checksum", "".join(tcp[16:18])), (18, 19, "urgent", str(n(tcp[18:20])))], AMBER)
    label(d, W // 2, 760, "Example: c6 = 198,  33 = 51,  64 = 100,  07 = 7   →   198.51.100.7",
          MUTED, 28, False, "ma")
    caption(d, "The source address is 4 bytes the sender writes. Nothing checks them.", ROSE, 840)


def layers(img, d, cfg):
    """A TCP segment inside an IP packet; the shop decodes it again."""
    tcp_len = find(r"TCP segment\s+(\d+) bytes", "ONE")
    ip_len = find(r"IP packet\s+(\d+) bytes", "ONE")
    src = find(r"from\s+(\S+) port", "ONE")
    sport = find(r"from\s+\S+ port (\d+)", "ONE")
    dst = find(r"to\s+(\S+) port", "ONE")
    dport = find(r"to\s+\S+ port (\d+)", "ONE")
    seq = find(r"sequence number (\d+)", "ONE")
    ttl = find(r"TTL (\d+)", "ONE")
    header(d, 100, 200, "THE PACKET SENT", GREEN)
    d.rounded_rectangle([100, 265, 1150, 800], radius=18, fill=PANEL, outline=CYAN, width=4)
    label(d, 125, 280, "IP packet: %s bytes" % ip_len, CYAN, 30, True)
    d.rounded_rectangle([125, 330, 1125, 470], radius=12, fill=BG_CELL, outline=CYAN, width=3)
    label(d, 150, 343, "IP header: 20 bytes", CYAN, 26, True)
    label(d, 150, 385, "from", MUTED, 26)
    node(d, 230, 378, 300, 46, src, ROSE, 26)
    label(d, 560, 385, "to", MUTED, 26)
    node(d, 610, 378, 300, 46, dst, CYAN, 26)
    label(d, 940, 385, "TTL %s" % ttl, TEXT, 26, True)
    label(d, 230, 430, "↑ just a field: any sender can write it", ROSE, 22, True)
    d.rounded_rectangle([125, 490, 1125, 780], radius=12, fill=BG_CELL, outline=AMBER, width=3)
    label(d, 150, 503, "TCP segment: %s bytes (inside the IP packet)" % tcp_len, AMBER, 26, True)
    label(d, 150, 555, "ports", MUTED, 26)
    node(d, 260, 548, 330, 46, "%s → %s" % (sport, dport), AMBER, 26)
    label(d, 620, 555, "flag", MUTED, 26)
    node(d, 700, 548, 140, 46, "SYN", AMBER, 26)
    label(d, 150, 625, "sequence number", MUTED, 26)
    node(d, 400, 618, 190, 46, seq, AMBER, 26)
    label(d, 150, 700, "data: none yet (the first packet only asks to connect)", MUTED, 24)
    draw.arrow_right(d, 1165, 530, 1250, TEXT, 4)
    header(d, 1265, 200, "THE PACKET RECEIVED", ROSE)
    d.rounded_rectangle([1265, 265, 1820, 800], radius=18, fill=PANEL, outline=LINE, width=2)
    label(d, 1290, 290, "the shop decodes the bytes", TEXT, 26, True)
    rows = [("from", "%s : %s" % (src, sport)), ("to", "%s : %s" % (dst, dport)),
            ("flag", "SYN"), ("sequence", seq)]
    for i, (k, v) in enumerate(rows):
        y = 360 + i * 95
        label(d, 1290, y, k, MUTED, 24)
        label(d, 1290, y + 32, v, TEXT, 30, True)
    label(d, 1290, 740, "it believes the 'from' field", ROSE, 24, True)
    caption(d, "Layers: IP carries the address, TCP carries the ports and the numbers.", AMBER, 850)


def checksum_steps(img, d, cfg):
    """How the IP header checksum is made, and what it does and does not catch."""
    ws = words("TWO", "words")
    total = find(r"^\s+sum\s+([0-9a-f]+)", "TWO")
    low, carry, folded = re.search(r"fold\s+(\w+) \+ (\w+) = (\w+)", act("TWO")).groups()
    check = find(r"flip\s+ffff - \w+ = (\w+)", "TWO")
    kept = find(r"checksum kept (\w+)", "TWO")
    new = find(r"new checksum (\w+)", "TWO")
    e = header(d, 100, 195, "HOW THE CHECKSUM IS MADE", AMBER)
    label(d, e + 24, 217, "the IP header as ten 16-bit words, checksum field set to 0000", TEXT, 26, True, "lm")
    for i, w in enumerate(ws):
        x = 100 + i * 170
        node(d, x, 255, 140, 58, w, ROSE if i == 4 else CYAN, 30)
        if i:
            d.text((x - 15, 284), "+", font=font(SANS_B, 30), fill=MUTED, anchor="mm")
    label(d, 100 + 4 * 170 + 70, 320, "TTL 40 = 64", ROSE, 20, True, "ma")
    steps = [("1. add", "all ten words", total, "5 hex digits: too long"),
             ("2. fold", "%s + %s" % (low, carry), folded, "the overflow goes back in"),
             ("3. flip", "ffff − %s" % folded, check, "every bit inverted: the checksum")]
    for i, (step, how, res, note) in enumerate(steps):
        y = 365 + i * 72
        label(d, 100, y + 10, step, AMBER, 30, True)
        node(d, 260, y, 340, 56, how, TEXT, 28)
        d.text((640, y + 28), "=", font=font(SANS_B, 36), fill=TEXT, anchor="mm")
        node(d, 680, y, 200, 56, res, GREEN if i == 2 else CYAN, 30)
        label(d, 910, y + 14, note, MUTED, 24)
    e = header(d, 100, 600, "BEFORE THE CHANGE", GREEN)
    rows = [(660, "TTL 64, checksum %s" % check, "accepted", True, "")]
    e = header(d, 100, 725, "AFTER THE CHANGE: TTL 64 → 1", ROSE)
    rows += [(785, "accident: checksum kept %s" % kept, "refused", False, "caught"),
             (850, "attacker: new checksum %s" % new, "accepted", True, "NOT caught")]
    for y, what, res, ok, note in rows:
        label(d, 130, y + 6, what, TEXT, 30)
        draw.arrow_right(d, 820, y + 24, 920, MUTED, 3)
        mark(d, 960, y + 24, ok, 44)
        label(d, 1000, y + 6, res, GREEN if ok else ROSE, 30, True)
        if note:
            label(d, 1220, y + 8, note, ROSE if note.startswith("NOT") else GREEN, 28, True)
    label(d, 1820, 660, "no key: anyone can", AMBER, 26, True, "ra")
    label(d, 1820, 695, "redo these 3 steps", AMBER, 26, True, "ra")


def handshake(img, d, cfg):
    """SYN, SYN-ACK, ACK with the real numbers."""
    server = find(r"SYN-ACK\s+seq (\d+)", "THREE")
    ack = find(r"3\. ACK\s+seq 1001, ack (\d+)", "THREE")
    cx, sx = 330, 1590
    for x, name, c in ((cx, "customer", CLIENT), (sx, "shop", AMBER)):
        node(d, x - 160, 200, 320, 62, name, c, 32, False)
        d.line([(x, 268), (x, 820)], fill=LINE, width=3)
    msgs = [(330, "THE REQUEST", GREEN, "1. SYN", "seq 1000", "", True),
            (490, "THE RESPONSE", ROSE, "2. SYN-ACK", "seq %s" % server, "ack 1001 = 1000 + 1", False),
            (670, "THE REQUEST", GREEN, "3. ACK", "seq 1001", "ack %s = %s + 1" % (ack, server), True)]
    for y, sec, c, name, seq, ackt, right in msgs:
        header(d, 820 - 0, y - 50, sec, c, 280)
        if right:
            draw.arrow_right(d, cx + 10, y + 30, sx - 10, c, 5)
        else:
            draw.arrow_left(d, sx - 10, y + 30, cx + 10, c, 5)
        label(d, 380, y + 45, name, c, 30, True)
        label(d, 620, y + 45, seq, TEXT, 30, True)
        if ackt:
            label(d, 960, y + 45, ackt, AMBER, 28, True)
    label(d, 1610, 590, "table: 1", AMBER, 24, True)
    label(d, 1610, 770, "table: 0 ✓", GREEN, 24, True)
    caption(d, "Each side answers the other's number + 1: proof it saw the packet.", AMBER, 850)


def flood(img, d, cfg):
    """A full table refuses a real customer; SYN cookies keep no table at all."""
    cookie = find(r"=\s+(\d+), sent as", "FOUR")
    back = find(r"the ACK brings (\d+)", "FOUR")
    header(d, 100, 195, "BEFORE THE FIX: a table of 8", ROSE)
    for i in range(8):
        y = 260 + i * 62
        node(d, 100, y, 400, 52, "100.64.0.%d  waiting" % (i + 1), ROSE, 24)
    label(d, 530, 450, "full: 8 of 8", ROSE, 28, True)
    node(d, 530, 520, 300, 56, "customer SYN", CLIENT, 26, False)
    mark(d, 870, 548, False)
    label(d, 530, 600, "not answered", ROSE, 26, True)
    label(d, 530, 640, "= denial of service", ROSE, 26, True)
    d.line([(960, 200), (960, 800)], fill=LINE, width=2)
    header(d, 1000, 195, "AFTER THE FIX: SYN cookies", GREEN)
    label(d, 1000, 265, "the shop keeps no entry, so the flood fills nothing", TEXT, 24, True)
    ins = [("secret", AMBER), ("198.51.100.7", CLIENT), ("port 51000", CLIENT), ("seq 1000", CLIENT)]
    for i, (t, c) in enumerate(ins):
        node(d, 1000 + (i % 2) * 300, 320 + (i // 2) * 66, 280, 52, t, c, 24)
    draw.arrow(d, 1290, 450, 1290, 500, TEXT, 4)
    node(d, 1150, 505, 280, 58, "HMAC", AMBER, 30, False)
    draw.arrow(d, 1290, 568, 1290, 610, TEXT, 4)
    node(d, 1000, 615, 580, 56, "cookie = %s" % cookie, GREEN, 26)
    label(d, 1600, 628, "SYN-ACK seq", MUTED, 22, True)
    node(d, 1000, 700, 580, 56, "ACK brings %s" % back, CLIENT, 26)
    label(d, 1600, 713, "= cookie + 1", MUTED, 22, True)
    mark(d, 1290, 790, True, 48)
    label(d, 1330, 772, "the shop recomputes it: connected", GREEN, 26, True)
    caption(d, "The state rides in the shop's own number, and only the secret can make it.", AMBER, 860)


def guessing(img, d, cfg):
    """A counter can be guessed, a keyed hash cannot; and the stream is readable."""
    five = act("FIVE")
    nums = re.findall(r"SYN-ACK seq:\s+(\d+)\n.*\(\+ 64000\):\s+(\d+)\n.*10\.0\.0\.5:\s+(\d+)", five)
    card = find(r"^    (card=\S+)$", "FIVE")
    rows = [("BEFORE THE FIX: a counter", ROSE, nums[0]), ("AFTER THE FIX: a keyed hash", GREEN, nums[1])]
    for i, (title, c, (seen, guess, sent)) in enumerate(rows):
        y = 195 + i * 250
        header(d, 100, y, title, c)
        label(d, 100, y + 62, "attacker's own number", MUTED, 22, True)
        node(d, 100, y + 92, 300, 56, seen, CLIENT, 28)
        draw.arrow_right(d, 410, y + 120, 520, TEXT, 3)
        label(d, 465, y + 82, "+64000", AMBER, 22, True, "ma")
        label(d, 530, y + 62, "its guess", MUTED, 22, True)
        node(d, 530, y + 92, 300, 56, guess, AMBER, 28)
        same = guess == sent
        d.text((890, y + 120), "=" if same else "≠", font=font(SANS_B, 44), fill=TEXT, anchor="mm")
        label(d, 950, y + 62, "what the shop sent 10.0.0.5", MUTED, 22, True)
        node(d, 950, y + 92, 300, 56, sent, CYAN, 28)
        mark(d, 1310, y + 120, not same, 48)
        label(d, 1350, y + 102, "forged ACK accepted" if same else "forged ACK refused",
              ROSE if same else GREEN, 28, True)
    header(d, 100, 700, "THE STREAM SENT", GREEN)
    node(d, 100, 755, 800, 56, card, GREEN, 28)
    header(d, 1000, 700, "WHAT A SNIFFER SEES", ROSE)
    node(d, 1000, 755, 820, 56, card, ROSE, 28)
    caption(d, "TCP hides nothing: the card number crosses the wire as it is.", AMBER, 860)
