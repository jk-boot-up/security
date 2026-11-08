"""Pictures for the DNS basics video, drawn by the `figure` slide kind.

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
INK = (15, 23, 42)


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("THE ANSWER")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def value(label, act, part=None):
    """The text after a label on a line of one act (or one part of it)."""
    m = re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", section(act, part), re.M)
    return m.group(1).strip()


def num(pattern, text):
    return re.search(pattern, text).group(1)


def header(d, x, y, text, colour, w=None):
    """A section label such as THE QUESTION: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cell(d, x, y, w, h, text, colour, size=28, sub=None):
    """One box of bytes, with an optional note under it."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h + 10), sub, font=font(SANS, 22), fill=MUTED, anchor="ma")
    return x + w


def box(d, x, y, w, h, title, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), title, font=font(SANS_B, 38), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 26), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


# ------------------------------------------------------------------ figures

def query_bytes(img, d, cfg):
    """The core idea: the question for shop.example.com, all 34 bytes, field by field."""
    q = section("ONE", "THE QUESTION")
    size = num(r"THE QUESTION \((\d+) bytes", section("ONE"))
    head = value("header", "ONE", "THE QUESTION").split()
    name = value("name", "ONE", "THE QUESTION").split()
    qtype = value("type", "ONE", "THE QUESTION").split()[0]
    qclass = value("class", "ONE", "THE QUESTION").split()[0]
    e = header(d, 100, 205, "THE QUESTION", GREEN)
    label(d, e + 24, 227, "shop.example.com ?   one message of %s bytes" % size, TEXT, 30, True, "lm")
    # the header: six 2-byte fields
    label(d, 100, 285, "HEADER  (12 bytes)", CYAN, 26, True)
    notes = ["id", "flags", "questions", "answers", "authority", "extra"]
    x = 100
    for i, (h, n) in enumerate(zip(head, notes)):
        cell(d, x, 325, 190, 66, h, AMBER if i == 0 else CYAN, 32, n)
        x += 210
    label(d, 1370, 330, "id: the caller's ticket number", AMBER, 26, True)
    label(d, 1370, 368, "flags 0100: \"please look it up\"", MUTED, 24)
    # the name as labels
    label(d, 100, 450, "NAME  (each part: its length, then its letters)", GREEN, 26, True)
    x = 100
    parts = [(name[i], name[i + 1]) for i in range(0, len(name) - 1, 2)]
    words = re.search(r"asks for (\S+)", OUT).group(1).split(".")
    for (n, letters), word in zip(parts, words):
        x = cell(d, x, 490, 80, 66, n, AMBER, 30, "length %d" % int(n, 16)) + 10
        w = 30 + 19 * len(letters)
        x = cell(d, x, 490, w, 66, letters, GREEN, 26, '"%s"' % word) + 24
    cell(d, x, 490, 80, 66, name[-1], AMBER, 30, "end")
    # type and class
    label(d, 100, 625, "WHAT IS ASKED", CLIENT, 26, True)
    cell(d, 100, 665, 190, 66, qtype, CLIENT, 32, "type A: an IPv4 address")
    cell(d, 520, 665, 190, 66, qclass, CLIENT, 32, "class IN: the internet")
    label(d, 920, 680, "12 + 18 + 2 + 2 = %s bytes" % size, TEXT, 34, True)
    caption(d, "No sender, no signature: just a ticket number and a question", AMBER, 830)
    label(d, W // 2, 885, "Whoever replies with the same id and question is believed.", MUTED, 30, False, "ma")


def message(img, d, cfg):
    """The answer repeats the header and question, then adds one record; the name is a pointer."""
    one = section("ONE")
    qsize = num(r"THE QUESTION \((\d+) bytes", one)
    asize = num(r"THE ANSWER \((\d+) bytes", one)
    full = num(r"would be (\d+) bytes", one)
    ah = value("header", "ONE", "THE ANSWER").split()
    ptr = value("name", "ONE", "THE ANSWER").split()[0]
    ttl_hex, ttl = re.search(r"ttl\s+(\S+) = (\d+)", one).groups()
    addr = re.search(r"address\s+(\S+ \S+) = (\S+)", one).groups()
    e = header(d, 100, 205, "THE QUESTION", GREEN)
    label(d, e + 24, 227, "%s bytes" % qsize, TEXT, 30, True, "lm")
    cell(d, 100, 270, 460, 70, "header  abcd 0100 ...", GREEN, 26, "id abcd, \"please look it up\"")
    cell(d, 580, 270, 700, 70, "shop.example.com  A  IN", GREEN, 28, "22 bytes, starting at byte 12")
    e = header(d, 100, 400, "THE ANSWER", CYAN)
    label(d, e + 24, 422, "%s bytes: the same start, then one record" % asize, TEXT, 30, True, "lm")
    cell(d, 100, 465, 460, 70, "header  %s %s ..." % (ah[0], ah[1]), CYAN, 26, "same id; 8180 = \"this is a reply\"")
    cell(d, 580, 465, 700, 70, "shop.example.com  A  IN", CYAN, 28, "copied from the question")
    # the record
    label(d, 100, 600, "THE NEW RECORD  (16 bytes)", AMBER, 26, True)
    x = 100
    for text, note, c, w in [(ptr, "name: back to byte 12", ROSE, 240), ("0001 0001", "A  IN", MUTED, 260),
                             (ttl_hex, "keep %s seconds" % ttl, AMBER, 280), (addr[0], addr[1], GREEN, 360)]:
        x = cell(d, x, 640, w, 70, text, c, 28, note) + 30
    label(d, 1360, 650, "c0 = \"a pointer\"", ROSE, 26, True)
    label(d, 1360, 688, "0c = 12: the name is at byte 12", ROSE, 26, True)
    caption(d, "The pointer saves 16 bytes: %s, not %s" % (asize, full), AMBER, 830)
    label(d, W // 2, 885, "Nothing in it says who sent it: only the id and the question must match.", MUTED, 30, False, "ma")


def cache(img, d, cfg):
    """A timeline: the answer is kept for its TTL, then the resolver asks again."""
    two = section("TWO")
    kept = re.search(r"kept until (.+)", two).group(1)
    end = int(num(r"= second (\d+)", two))
    rows = re.findall(r"second (\d+)\s+(from the cache|expired, asked again)\s+(?:(\S+)\s+)?upstream (\d+)", two)
    addr = num(r"(\d+\.\d+\.\d+\.\d+)", two)
    e = header(d, 100, 205, "THE ANSWER", CYAN)
    label(d, e + 24, 227, "second 0: shop.example.com is %s, ttl %d" % (addr, end), TEXT, 30, True, "lm")
    label(d, 100, 280, "kept until " + kept, AMBER, 30, True)
    x0, x1, y = 160, 1640, 470
    scale = (x1 - x0) / (end + 60)
    d.rounded_rectangle([x0, y - 40, x0 + int(end * scale), y + 40], radius=12, fill=BG_CELL, outline=GREEN, width=3)
    d.text((x0 + int(end * scale) // 2, y - 64), "in the cache: %s" % addr, font=font(SANS_B, 28), fill=GREEN, anchor="mm")
    d.line([x0, y + 70, x1 + 60, y + 70], fill=MUTED, width=3)
    last = 99999
    for s, mark in reversed([(0, "0")] + [(int(r[0]), r[0]) for r in rows]):
        xx = x0 + int(s * scale)
        d.line([xx, y + 60, xx, y + 80], fill=MUTED, width=3)
        if last - xx > 90:
            d.text((xx, y + 92), mark, font=font(MONO_B, 24), fill=MUTED, anchor="ma")
            last = xx
    label(d, x1 + 60, y + 92, "second", MUTED, 22, False, "ra")
    header(d, 100, 640, "LATER QUESTIONS", AMBER)
    for i, (s, what, a, up) in enumerate(rows):
        c = GREEN if what.startswith("from") else ROSE
        text = "second %s: %s%s" % (s, what, ", " + a if a else "")
        label(d, 140, 705 + i * 50, text, c, 28, True)
        label(d, 1000, 709 + i * 50, "questions sent upstream so far: %s" % up, MUTED, 26)
    caption(d, "Fast for everyone, and a false answer is repeated just as fast", AMBER, 885)


def exchange(img, d, cfg):
    """One datagram out, one back; the name is readable on the way."""
    three = section("THREE")
    qsize = num(r"THE QUERY \(one datagram, (\d+) bytes", three)
    rsize = num(r"THE RESPONSE \(one datagram, (\d+) bytes", three)
    seen = num(r"seen on the path: (\S+)", three)
    qid = num(r"id (\w+), asks", three)
    addr = num(r"says (\S+)", three)
    box(d, 100, 330, 360, 200, "Client", "the customer's computer", GREEN)
    box(d, 1460, 330, 360, 200, "Server", "answers DNS questions", CYAN)
    e = header(d, 560, 230, "THE QUERY", GREEN)
    label(d, e + 24, 252, "one UDP datagram, %s bytes" % qsize, TEXT, 28, True, "lm")
    draw.arrow_right(d, 480, 340, 1440, GREEN, 4)
    label(d, 560, 300, "id %s, shop.example.com ?" % qid, GREEN, 26, True)
    e = header(d, 560, 470, "THE RESPONSE", CYAN)
    label(d, e + 24, 492, "one UDP datagram, %s bytes" % rsize, TEXT, 28, True, "lm")
    draw.arrow_left(d, 1440, 540, 480, CYAN, 4)
    label(d, 560, 560, "id %s, %s" % (qid, addr), CYAN, 26, True)
    label(d, 100, 650, "WHAT SOMEONE ON THE PATH SEES IN THE QUERY", ROSE, 26, True)
    d.rounded_rectangle([100, 690, 1820, 770], radius=12, fill=BG_CELL, outline=ROSE, width=3)
    d.text((960, 730), seen, font=font(MONO_B, 40), fill=ROSE, anchor="mm")
    label(d, W // 2, 785, "a dot is a byte that is not a letter", MUTED, 22, False, "ma")
    caption(d, "No connection, no signature: a reply that looks right is believed", AMBER, 850)


def poisoning(img, d, cfg):
    """Counting ids are guessed at once; a keyed id and port are worked out from a hash."""
    four = section("FOUR")
    bad = num(r"cache says (\S+)  <-", four)
    first = num(r"after (\d+) forged answer ", four)
    many = num(r"after (\d+) forged answers the cache says nothing", four)
    mac = re.search(r"(HMAC-SHA-256\(secret, \d+\)) = (\w+) (\w+)", four).groups()
    pid = num(r"first 2 bytes \w+ = (\d+)", four)
    port = num(r"\) = (\d+)\n", four)
    pairs = num(r"= (\d+) pairs", four)
    odds = num(r"1 time in (\d+)", four)
    header(d, 100, 205, "BEFORE THE FIX", ROSE)
    label(d, 100, 270, "ids count up, every query from port 53", TEXT, 28, True)
    for i, (t, c, n) in enumerate([("4000", MUTED, "last id seen"), ("4001 ?", ROSE, "the next one")]):
        cell(d, 100 + i * 250, 320, 220, 70, t, c, 32, n)
    cell(d, 600, 320, 160, 70, "53", ROSE, 32, "port")
    label(d, 100, 440, "believed after %s forged answer" % first, ROSE, 30, True)
    label(d, 100, 485, "the cache now says %s" % bad, ROSE, 30, True)
    d.line([880, 210, 880, 760], fill=MUTED, width=2)
    header(d, 940, 205, "AFTER THE FIX", GREEN)
    label(d, 940, 270, mac[0], TEXT, 28, True)
    cell(d, 940, 320, 190, 70, mac[1], AMBER, 32, "bytes 0-1")
    cell(d, 1150, 320, 260, 70, mac[2], CYAN, 32, "bytes 2-5")
    cell(d, 1430, 320, 150, 70, "...", MUTED, 32)
    draw.arrow(d, 1035, 425, 1035, 470, AMBER, 3)
    draw.arrow(d, 1280, 425, 1280, 470, CYAN, 3)
    label(d, 940, 480, "id = %s" % pid, AMBER, 30, True)
    label(d, 1180, 480, "port = 1024 + (%s mod 64512) = %s" % (mac[2], port), CYAN, 26, True)
    label(d, 940, 560, "%s forged answers: the cache says nothing" % many, GREEN, 30, True)
    label(d, 940, 610, "65536 ids x 64512 ports = %s pairs" % pairs, MUTED, 28)
    label(d, 940, 655, "%s guesses hit about 1 time in %s" % (many, odds), MUTED, 28)
    caption(d, "The secret key makes the next id and port impossible to predict", AMBER, 830)


def lifetime(img, d, cfg):
    """The same false answer under two caps, seen at the two-hour mark."""
    five = section("FIVE")
    caps = re.findall(r"kept until min\((\d+), (\d+)\) = second (\d+)", five)
    believed = re.findall(r"still believed after 2 hours \(second (\d+)\): (\w+)", five)
    bad = num(r"THE FALSE ANSWER: (\S+),", five)
    e = header(d, 100, 205, "THE FALSE ANSWER", ROSE)
    label(d, e + 24, 227, "%s, \"keep me for 86400 seconds, a day\"" % bad, TEXT, 30, True, "lm")
    x0, x1 = 660, 1760
    day = int(caps[0][2])
    scale = (x1 - x0) / day
    for i, ((ttl, cap, end), (at, yes), (title, c)) in enumerate(zip(caps, believed, [("BEFORE THE FIX", ROSE), ("AFTER THE FIX", GREEN)])):
        y = 320 + i * 190
        header(d, 100, y, title, c, 300)
        label(d, 100, y + 60, "cap %s: kept until second %s" % (cap, end), TEXT, 26, True)
        w = max(int(int(end) * scale), 30)
        d.rounded_rectangle([x0, y, x0 + w, y + 60], radius=10, fill=BG_CELL, outline=ROSE, width=3)
        if w >= 200:
            d.text((x0 + w // 2, y + 30), "poisoned", font=font(SANS_B, 24), fill=ROSE, anchor="mm")
        mark = x0 + int(int(at) * scale)
        d.line([mark, y - 10, mark, y + 70], fill=AMBER, width=4)
        label(d, mark + 20, y + 90, "after 2 hours: %s" % ("still believed" if yes == "true" else "gone"),
              ROSE if yes == "true" else GREEN, 26, True)
    header(d, 100, 700, "WHAT ELSE STOPS IT", CYAN)
    for i, t in enumerate(["DNSSEC: signed answers", "Encrypted DNS: a sealed path", "TLS: the certificate check"]):
        label(d, 100 + i * 600, 770, t, CYAN, 28, True)
    caption(d, "A cap does not stop the first lie, but it makes it short", AMBER, 850)
