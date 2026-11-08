"""Pictures for the tcpdump video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CLIENT, CYAN, GREEN, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("BEFORE RECORDING")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def find(pattern, text=OUT):
    return re.search(pattern, text, re.M).groups()


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE RECORDING: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=950):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cell(d, x, y, w, h, text, colour, size=30, mono=True, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="mm")


def cells(d, x, y, items, colour, w=88, h=64, size=30, mark=(), mark_colour=ROSE, gap=8):
    for i, t in enumerate(items):
        cell(d, x + i * (w + gap), y, w, h, t, mark_colour if i in mark else colour, size)
    return x + len(items) * (w + gap) - gap


# ------------------------------------------------------------------ figures

def one_packet(img, d, cfg):
    """The core idea: one real packet, the order, as an IP header, a TCP header and the data."""
    size, data = map(int, find(r"packet 4  \S+\s+(\d+) bytes = IP 20 \+ TCP 20 \+ data (\d+)"))
    src, sport, dst, dport = find(r"^\s+IP ([\d.]+)\.(\d+) > ([\d.]+)\.(\d+):\n\s+Flags \[P\.\]")
    seq, ack = find(r"Flags \[P\.\], seq (\d+):\d+, ack (\d+)")
    order = section("SIX", "WHAT THE CUSTOMER SENT").strip()
    label(d, 100, 215, "One packet from the capture: the customer's order, %d bytes" % size, TEXT, 34, True)
    x, y, h = 100, 290, 120
    widths = [430, 430, 860]
    boxes = [("IP header", "20 bytes", CLIENT), ("TCP header", "20 bytes", AMBER), ("data", "%d bytes" % data, GREEN)]
    for (name, n, c), w in zip(boxes, widths):
        d.rounded_rectangle([x, y, x + w - 10, y + h], radius=14, fill=PANEL, outline=c, width=4)
        d.text((x + (w - 10) // 2, y + 40), name, font=font(SANS_B, 38), fill=c, anchor="mm")
        d.text((x + (w - 10) // 2, y + 88), n, font=font(SANS, 26), fill=MUTED, anchor="mm")
        x += w
    rows = [
        (100, CLIENT, "IP: which computer", ["from  " + src, "to    " + dst]),
        (530, AMBER, "TCP: which program", ["port  %s → %s" % (sport, dport), "seq %s   ack %s" % (seq, ack), "flags  P.  (push, ack)"]),
        (960, GREEN, "data: what the customer typed", [order[:21], order[21:]]),
    ]
    for x, c, head, lines in rows:
        label(d, x, 450, head, c, 28, True)
        for i, t in enumerate(lines):
            label(d, x, 505 + i * 54, t, TEXT, 30 if c != GREEN else 34, True)
    d.line([(100, 700), (W - 100, 700)], fill=(71, 85, 105), width=2)
    label(d, 100, 730, "Each layer wraps the next:  IP carries TCP,  TCP carries the data.", TEXT, 32, True)
    label(d, 100, 790, "tcpdump prints the IP and TCP parts.  The data travels as it is, unless TLS scrambles it.", MUTED, 30)
    caption(d, "Every packet in this video is built like this one.", AMBER, 880)


def pcap_layout(img, d, cfg):
    """Act one: the five packets go in, a 338-byte pcap file comes out."""
    before = re.findall(r"packet (\d)  (\S+)\s+(\d+) bytes", section("ONE", "BEFORE RECORDING"))
    total = find(r"capture\.pcap\s+(.+) bytes")[0]
    header(d, 100, 210, "BEFORE RECORDING: the packets of one purchase", GREEN)
    x = 100
    for n, flags, size in before:
        w = int(size) * 4
        cell(d, x, 275, w, 90, flags, GREEN, 30)
        label(d, x + w // 2, 375, "%s bytes" % size, MUTED, 24, False, "ma")
        x += w + 20
    label(d, x + 10, 300, "= %d bytes" % sum(int(s) for _, _, s in before), TEXT, 34, True)
    draw.arrow(d, 960, 425, 960, 485, CYAN, 4)
    header(d, 100, 500, "AFTER RECORDING: the pcap file", ROSE)
    x, y = 100, 570
    parts = [("file", 24, CYAN)]
    for _, flags, size in before:
        parts += [("rec", 16, AMBER), (flags, int(size), GREEN)]
    scale = (W - 200 - 10 * (len(parts) - 1)) / sum(p[1] for p in parts)
    for name, size, c in parts:
        w = int(size * scale)
        cell(d, x, y, w, 90, name if w > 50 else "", c, 24)
        x += w + 10
    label(d, 100, 680, "file header 24", CYAN, 28, True)
    label(d, 420, 680, "record header 16, before each packet", AMBER, 28, True)
    label(d, 1100, 680, "the packet's own bytes", GREEN, 28, True)
    caption(d, total + " bytes", TEXT, 780)
    label(d, W // 2, 850, "tcpdump, Wireshark and Python all read this one standard file.", MUTED, 30, False, "ma")


def handshake(img, d, cfg):
    """Act two: the five packets as arrows between the customer and the shop, with seq and ack."""
    text = section("TWO", "WHAT TCPDUMP PRINTS")
    pkts = re.findall(r"IP ([\d.]+)\.(\d+) > ([\d.]+)\.(\d+):\n\s+Flags \[(\S*)\], (.*?), win", text)
    cust = pkts[0][0] + ":" + pkts[0][1]
    shop = pkts[0][2] + ":" + pkts[0][3]
    lx, rx = 330, 1590
    header(d, 100, 210, "THE CUSTOMER  " + cust, GREEN, 560)
    header(d, W - 660, 210, "THE SHOP  " + shop, ROSE, 560)
    d.line([(lx, 265), (lx, 900)], fill=GREEN, width=3)
    d.line([(rx, 265), (rx, 900)], fill=ROSE, width=3)
    num = [dict(re.findall(r"(seq|ack) (\d+)", n)) for _, _, _, _, _, n in pkts]
    data = int(find(r"Flags \[P\.\].*length (\d+)", text)[0])
    notes = ["SYN: let's talk, my first number is %s" % num[0]["seq"],
             "SYN-ACK: ack %s = %s + 1, and my first number is %s" % (num[1]["ack"], num[0]["seq"], num[1]["seq"]),
             "ACK: ack %s = %s + 1, the handshake is done" % (num[2]["ack"], num[1]["seq"]),
             "the order: %d bytes of data" % data,
             "ACK: ack %s = %s + %d, I have every byte" % (num[4]["ack"], num[3]["seq"], data)]
    for i, (src, sp, dst, dp, flags, nums) in enumerate(pkts):
        y = 320 + i * 120
        right = src == pkts[0][0]
        c = GREEN if right else ROSE
        if right:
            draw.arrow_right(d, lx + 6, y + 40, rx - 6, c, 4)
        else:
            draw.arrow_left(d, rx - 6, y + 40, lx + 6, c, 4)
        label(d, W // 2, y - 8, "[%s]  %s" % (flags, nums), TEXT, 30, True, "ma")
        label(d, W // 2, y + 50, "%d  " % (i + 1) + notes[i], MUTED, 27, False, "ma")
    caption(d, "Packets 1 to 3 are the handshake.  Packet 4 is the order.", AMBER, 935)


def flood(img, d, cfg):
    """Act three: fourteen packets in, one finished handshake and nine unfinished ones out."""
    cap = section("THREE", "THE CAPTURE")
    counts = [int(n) for n in re.findall(r"(\d+) packets?", cap)]
    found = section("THREE", "THE FINDING")
    finished = find(r"finished\s+([\d.]+)$", found)[0]
    never = re.findall(r"100\.64\.0\.\d+", found)
    header(d, 100, 210, "THE CAPTURE: %d packets" % sum(counts), GREEN)
    x = 100
    for n, c, name in zip(counts, [GREEN, AMBER, ROSE], ["purchase", "SYNs", "1 more"]):
        for _ in range(n):
            cell(d, x, 280, 100, 70, "", c)
            x += 112
        label(d, x - 112 * n + (112 * n - 12) // 2, 360, name, c, 26, True, "ma")
    header(d, 100, 450, "THE FINDING: who started a handshake, who finished it", ROSE)
    cell(d, 100, 520, 460, 70, finished, GREEN, 32)
    label(d, 590, 555, "SYN ✓   ACK ✓   finished: the real customer", GREEN, 30, True, "lm")
    for i, src in enumerate(never):
        cx, cy = 100 + (i % 5) * 340, 620 + (i // 5) * 90
        cell(d, cx, cy, 320, 70, src, AMBER, 30)
    label(d, 100, 810, "SYN ✓   ACK ✗   %d sources never finished" % len(never), AMBER, 30, True)
    caption(d, "A flood shows as handshakes that start and never finish.", TEXT, 890)


def checksum_steps(img, d, cfg):
    """Act four, the derived value: the TCP checksum in four steps."""
    s = section("FOUR")
    pseudo = find(r"pseudo-header\s+(.+)$", s)[0].split()
    tcp = re.findall(r"^\s+TCP header\s+(.+)\n\s+([0-9a-f ]+)$", s, re.M)[0]
    tcp = (tcp[0] + " " + tcp[1]).split()
    total = find(r"sum\s+(\w+)$", s)[0]
    low, carry, folded = find(r"(\w{4}) \+ (\w+) = (\w{4})$", s)
    check = find(r"checksum\s+(\w{4})$", s)[0]
    header(d, 100, 210, "STEP 1  split into 16-bit words", AMBER)
    label(d, 100, 292, "pseudo-header", CLIENT, 28, True)
    cells(d, 370, 275, pseudo, CLIENT, w=130, h=60, size=30)
    label(d, 1225, 305, "addresses, protocol 6, length 20", MUTED, 26, False, "lm")
    label(d, 100, 372, "TCP header", GREEN, 28, True)
    cells(d, 370, 355, tcp, GREEN, w=130, h=60, size=30, mark=(8,), mark_colour=MUTED)
    label(d, 1380, 440, "checksum field: 0000 while adding", MUTED, 26, False, "ma")
    rows = [("STEP 2  add them up", 490), ("STEP 3  fold the carry back in", 610), ("STEP 4  flip every bit", 730)]
    for text, y in rows:
        header(d, 100, y + 10, text, AMBER, 500)
    cell(d, 640, 490, 280, 70, total, TEXT, 38)
    label(d, 950, 525, "five hex digits: too big for 16 bits", MUTED, 28, False, "lm")
    cell(d, 640, 610, 170, 70, low, TEXT, 38)
    label(d, 830, 645, "+", TEXT, 44, True, "lm")
    cell(d, 880, 610, 100, 70, carry, AMBER, 38)
    label(d, 1000, 645, "=", TEXT, 44, True, "lm")
    cell(d, 1050, 610, 170, 70, folded, TEXT, 38)
    cell(d, 640, 730, 170, 70, folded, TEXT, 38)
    draw.arrow_right(d, 825, 765, 950, AMBER, 4)
    cell(d, 960, 730, 170, 70, check, GREEN, 38)
    label(d, 1160, 765, "the checksum, sent in the packet", GREEN, 30, True, "lm")
    caption(d, "Change any byte and the sum changes, so the checksum no longer matches.", AMBER, 880)


def damage(img, d, cfg):
    """Act five: the same checksum before and after the damage."""
    rows = []
    for part in ("BEFORE THE DAMAGE", "AFTER THE DAMAGE"):
        p = section("FIVE", part)
        rows.append((find(r"checksum in the packet\s+(\w+)", p)[0], find(r"worked out again\s+(\w+)  match: (\w+)", p)))
    bad = find(r"does not match: (\d+)")[0]
    for i, (part, c) in enumerate([("BEFORE THE DAMAGE: as it was sent", GREEN), ("AFTER THE DAMAGE: as it arrived", ROSE)]):
        y = 220 + i * 300
        stored, (worked, ok) = rows[i]
        header(d, 100, y, part, c)
        label(d, 100, y + 80, "checksum in the packet", MUTED, 28)
        sb, wb = [stored[:2], stored[2:]], [worked[:2], worked[2:]]
        cells(d, 100, y + 125, sb, c, w=110, h=72, size=36, mark=(0,) if ok == "false" else (), mark_colour=ROSE)
        label(d, 480, y + 80, "worked out again", MUTED, 28)
        cells(d, 480, y + 125, wb, GREEN, w=110, h=72, size=36)
        good = ok == "true"
        label(d, 820, y + 161, "match ✓" if good else "no match ✗", GREEN if good else ROSE, 40, True, "lm")
    label(d, 1180, 330, "One byte changed on the way:", TEXT, 28, True)
    label(d, 1180, 375, "%s became %s." % (rows[0][0][:2], rows[1][0][:2]), TEXT, 28, True)
    label(d, 1180, 420, "The packet still looks normal.", MUTED, 26)
    caption(d, "%s damaged packet in the capture.  tcpdump -v marks it incorrect." % bad, AMBER, 880)


def reveal(img, d, cfg):
    """Act six: what the customer sent, and what anyone with the capture file reads."""
    sent = section("SIX", "WHAT THE CUSTOMER SENT").strip()
    holds = section("SIX", "WHAT THE CAPTURE HOLDS")
    where = find(r"WHAT THE CAPTURE HOLDS \((.+)\):")[0]
    found = holds.strip().splitlines()[0].strip()
    header(d, 100, 210, "WHAT THE CUSTOMER SENT: the order, no TLS", GREEN)
    label(d, 100, 290, sent, TEXT, 44, True)
    header(d, 100, 400, "WHAT THE CAPTURE HOLDS: " + where, ROSE)
    card = re.search(r"\d{16}", found).span()
    chars = list(found)
    for row, (a, b) in enumerate([(0, 21), (21, len(chars))]):
        cells(d, 100, 480 + row * 90, chars[a:b], TEXT, w=70, h=72, size=34, gap=6,
              mark=[i - a for i in range(card[0], card[1]) if a <= i < b])
    caption(d, "A capture file is as sensitive as the traffic.", ROSE, 720)
    label(d, W // 2, 790, "With TLS, these bytes would be scrambled, and the thief would learn nothing.", MUTED, 30, False, "ma")
