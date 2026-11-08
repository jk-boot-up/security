"""Pictures for the DNS with CoreDNS video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("THE ANSWER")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % part, text, re.M).group(1)
    return text


def find(pattern, act):
    return re.search(pattern, section(act), re.M).groups()


def wire(part):
    """The rows of THE QUESTION or THE ANSWER: [(field, bytes, note)]."""
    rows = []
    for ln in section("ONE", part).splitlines():
        m = re.match(r"    (\S*)\s+((?:[0-9a-f]{2} ?)+?|the same \d+ bytes|03 .*? 00)(?:  (.*))?$", ln)
        rows.append((m.group(1), m.group(2).strip(), (m.group(3) or "").strip()))
    return rows


def size(part):
    return int(re.search(r"^  %s.*?(\d+) bytes" % part, section("ONE"), re.M).group(1))


def zone():
    """zone, name, address, time to live, from the first line of act one."""
    return find(r"zone file for (\S+) says (\S+) is (\S+) for (\d+) seconds", "ONE")


# ------------------------------------------------------------------ drawing helpers

def header(d, x, y, text, colour, w=None):
    """A section label such as THE QUESTION: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cell(d, x, y, w, h, text, colour, size=30, mono=True, sub=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=colour, width=3)
    cy = y + h // 2 - (14 if sub else 0)
    d.text((x + w // 2, cy), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, cy + 34), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


def field(d, x, y, name, text, colour, size=26):
    """A field of a message: its name above, its bytes in a box. Returns the x after it."""
    f = font(MONO_B, size)
    w = max(int(f.getlength(text)), int(font(SANS_B, 20).getlength(name))) + 30
    label(d, x + w // 2, y, name, colour, 20, True, "ma")
    d.rounded_rectangle([x, y + 28, x + w, y + 82], radius=10, fill=BG_CELL, outline=colour, width=3)
    d.text((x + w // 2, y + 55), text, font=f, fill=colour, anchor="mm")
    return x + w + 12


def zone_line(d, x, y, name, ttl, address, colour, w=760):
    cell(d, x, y, w, 66, "%-4s %-6s IN  A  %s" % (name, ttl, address), colour, 28)


# ------------------------------------------------------------------ figures

def one_lookup(img, d, cfg):
    """The core idea: a name in, the zone file's line out."""
    z, name, address, ttl = zone()
    short = name.split(".")[0]
    header(d, 100, 215, "THE QUESTION", GREEN)
    cell(d, 100, 285, 520, 120, name + " ?", GREEN, 32, sub="which IPv4 address? (type A)")
    draw.arrow_right(d, 635, 345, 760, MUTED, 4)
    d.rounded_rectangle([775, 250, 1145, 440], radius=18, fill=PANEL, outline=CYAN, width=4)
    d.text((960, 300), "CoreDNS", font=font(SANS_B, 44), fill=CYAN, anchor="mm")
    d.text((960, 350), "reads its zone file", font=font(SANS, 24), fill=MUTED, anchor="mm")
    d.text((960, 390), "for " + z, font=font(MONO_B, 24), fill=MUTED, anchor="mm")
    draw.arrow_right(d, 1160, 345, 1285, MUTED, 4)
    header(d, 1300, 215, "THE ANSWER", ROSE)
    cell(d, 1300, 285, 520, 120, address, ROSE, 36, sub="keep it for %s seconds" % ttl)
    label(d, 100, 500, "The line in the zone file that gives the answer:", TEXT, 30, True)
    parts = [(short, "the name", GREEN, 200), (ttl, "time to live, seconds", AMBER, 300),
             ("IN", "class: internet", MUTED, 260), ("A", "type: IPv4 address", CYAN, 260),
             (address, "the address", ROSE, 420)]
    x = 100
    for text, note, c, w in parts:
        cell(d, x, 560, w, 80, text, c, 34)
        label(d, x + w // 2, 655, note, c, 22, True, "ma")
        x += w + 20
    caption(d, "Like a phone book: the server answers exactly what the file says.", AMBER, 760)
    label(d, W // 2, 820, "Nothing in the answer proves the file is right.", MUTED, 30, False, "ma")


def wire_bytes(img, d, cfg):
    """The question and the answer of act one, field by field, and how two answer fields are read."""
    q, a = wire("THE QUESTION"), wire("THE ANSWER")
    qs, asz = size("THE QUESTION"), size("THE ANSWER")
    header(d, 100, 205, "THE QUESTION: %d bytes" % qs, GREEN)
    x = 100
    qc = [r for r in q if r[0] != ""]
    counts = [r[1] for r in q if r[0] in ("counts", "")]
    for name, data, _ in qc:
        if name == "counts":
            data = " ".join(counts)
        x = field(d, x, 262, name, data, GREEN, 28)
    header(d, 100, 375, "THE ANSWER: %d bytes" % asz, ROSE)
    rows = {r[0]: r[1] for r in a if r[0]}
    acounts = " ".join(r[1] for r in a if r[0] in ("counts", ""))
    x = 100
    for name, data, c in (("id", rows["id"], AMBER), ("flags", rows["flags"], ROSE),
                          ("counts", acounts, ROSE), ("question", rows["question"], MUTED)):
        x = field(d, x, 432, name, data, c, 28)
    x = 100
    for name, c in (("name", ROSE), ("type+class", ROSE), ("ttl", AMBER), ("length", ROSE), ("address", CYAN)):
        x = field(d, x, 532, name, rows[name], c, 28)
    label(d, 100, 650, "How two of the answer's fields are read (the first byte counts most):", TEXT, 28, True)
    ttl = rows["ttl"].split()
    nonzero = [(b, 256 ** (len(ttl) - 1 - i)) for i, b in enumerate(ttl) if b != "00"]
    sum_text = " + ".join(("%d × %d" % (int(b, 16), p)) if p > 1 else "%d" % int(b, 16) for b, p in nonzero)
    total = sum(int(b, 16) * p for b, p in nonzero)
    cell(d, 100, 700, 1720, 70, "ttl      %s  →  %s = %d seconds" % (rows["ttl"], sum_text, total), AMBER, 28)
    addr = rows["address"].split()
    cell(d, 100, 790, 1720, 70, "address  %s  →  %s" % (
        rows["address"], " . ".join(str(int(b, 16)) for b in addr)), CYAN, 28)
    caption(d, "The answer repeats the id %s: that, and the question, is all the client checks." % rows["id"],
            MUTED, 895)


def udp_tcp(img, d, cfg):
    """The same message over UDP and over TCP: TCP adds a two-byte length."""
    qs = size("THE QUESTION")
    longer = int(find(r"the answer is (\d+) bytes longer", "TWO")[0])
    same = find(r"both give the same address: (\w+)", "TWO")[0]
    prefix = "%02x %02x" % (qs >> 8, qs & 0xff)
    e = header(d, 100, 210, "THE QUESTION", GREEN)
    label(d, e + 24, 220, "the same %d-byte DNS message, sent two ways" % qs, TEXT, 28, True)
    label(d, 100, 300, "UDP", CYAN, 36, True)
    label(d, 100, 345, "one datagram", MUTED, 22)
    cell(d, 340, 290, 900, 80, "DNS message: %d bytes" % qs, GREEN, 30, mono=False)
    label(d, 100, 430, "TCP", CLIENT, 36, True)
    label(d, 100, 475, "a stream", MUTED, 22)
    cell(d, 340, 420, 220, 80, prefix, AMBER, 32)
    cell(d, 580, 420, 900, 80, "DNS message: %d bytes" % qs, GREEN, 30, mono=False)
    label(d, 340, 515, "length: 0x%02x = %d" % (qs, qs), AMBER, 24, True)
    header(d, 100, 600, "THE ANSWER", ROSE)
    cell(d, 100, 665, 820, 110, "same address: %s" % same, ROSE, 32, mono=False, sub="over UDP and over TCP")
    cell(d, 1000, 665, 820, 110, "TCP answer: %d bytes longer" % longer, AMBER, 32, mono=False,
         sub="its own two-byte length in front")
    caption(d, "Neither one signs or encrypts anything: anyone on the path can read it.", AMBER, 860)


def three_names(img, d, cfg):
    """Three questions the zone cannot answer as asked: missing, outside, rewritten."""
    nope, code1, n1 = find(r"^  (\S+): (\w+) with (\d+) answers", "THREE")
    other, code2, z = find(r"^  (\S+): (\w+), because this server only answers for (\S+)", "THREE")
    legacy, www, addr, under = find(r"^  (\S+) is rewritten to (\S+) and the answer is (\S+) under the name asked for: (\S+)",
                                    "THREE")
    header(d, 100, 210, "THE QUESTION", GREEN, 480)
    label(d, 960, 222, "what the server does", MUTED, 28, True, "ma")
    header(d, 1340, 210, "THE ANSWER", ROSE, 480)
    rows = [(nope, "in %s, but no such record" % z, "%s, %s answers" % (code1, n1), AMBER),
            (other, "not in %s: not my zone" % z, code2, AMBER),
            (legacy, "rule: rewrite to %s" % www, "%s, named %s" % (addr, under.split(".")[0]), GREEN)]
    for i, (q, why, ans, c) in enumerate(rows):
        y = 290 + i * 170
        cell(d, 100, y, 480, 100, q, GREEN, 28)
        draw.arrow_right(d, 590, y + 50, 660, MUTED, 3)
        cell(d, 670, y, 580, 100, why, MUTED, 24, mono=False)
        draw.arrow_right(d, 1260, y + 50, 1330, MUTED, 3)
        cell(d, 1340, y, 480, 100, ans, c, 26)
    caption(d, "A rewrite is a rule inside the server: the client never sees it.", AMBER, 830)


def cache_ttl(img, d, cfg):
    """The cache cuts the zone's time to live and counts it down."""
    cap, ttl, cut = find(r"at most (\d+) seconds, so the zone's (\d+) is cut to (\d+)", "FOUR")
    z, name, address, _ = zone()
    header(d, 100, 210, "BEFORE THE CACHE", GREEN)
    zone_line(d, 100, 280, name.split(".")[0], ttl, address, GREEN, 700)
    label(d, 100, 360, "the zone file: keep it %s seconds" % ttl, MUTED, 24)
    draw.arrow_right(d, 815, 313, 920, MUTED, 4)
    d.rounded_rectangle([935, 250, 1335, 380], radius=18, fill=PANEL, outline=CYAN, width=4)
    d.text((1135, 295), "cache %s" % cap, font=font(MONO_B, 40), fill=CYAN, anchor="mm")
    d.text((1135, 345), "the Corefile's cache plugin", font=font(SANS, 22), fill=MUTED, anchor="mm")
    draw.arrow_right(d, 1350, 313, 1450, MUTED, 4)
    cell(d, 1465, 270, 355, 90, "at most %s" % cut, AMBER, 32)
    cell(d, 100, 420, 1720, 70, "smaller of %s and %s  =  %s seconds" % (ttl, cap, cut), AMBER, 30)
    header(d, 100, 530, "AFTER THE CACHE: three answers in a row", ROSE)
    bars = [("1st: from the zone file", 1.0, GREEN), ("2nd: from the cache, at most %s" % cut, 0.8, AMBER),
            ("3rd: smaller than the 2nd", 0.6, ROSE)]
    for i, (text, frac, c) in enumerate(bars):
        y = 600 + i * 85
        label(d, 100, y + 32, text, c, 26, True, "lm")
        d.rounded_rectangle([760, y + 8, 760 + int(1060 * frac), y + 56], radius=10, fill=BG_CELL, outline=c, width=3)
    label(d, 760, 860, "time to live left: counted down every second", MUTED, 22)
    caption(d, "Fast, and a false answer stays until its time runs out.", AMBER, 905)


def wrong_record(img, d, cfg):
    """A wrong record is served like a right one; its long time to live outlasts the fix."""
    z, name, good, good_ttl = zone()
    wrong, wrong_ttl = find(r"to (\S+) with a time to live of (\d+) seconds", "FIVE")
    served = find(r"serves it exactly as it serves the right one: (\S+)", "FIVE")[0]
    cap = find(r"cuts the \d+ seconds to (\d+)", "FIVE")[0]
    short = name.split(".")[0]
    header(d, 100, 210, "BEFORE THE CHANGE", GREEN)
    zone_line(d, 100, 270, short, good_ttl, good, GREEN, 760)
    draw.arrow_right(d, 875, 303, 990, MUTED, 4)
    cell(d, 1005, 270, 815, 66, "served: " + good, GREEN, 30)
    header(d, 100, 380, "AFTER THE CHANGE", ROSE)
    zone_line(d, 100, 440, short, wrong_ttl, wrong, ROSE, 760)
    draw.arrow_right(d, 875, 473, 990, MUTED, 4)
    cell(d, 1005, 440, 815, 66, "served: %s  THE SERVER CANNOT TELL" % served, ROSE, 26)
    secs = int(wrong_ttl)
    cell(d, 100, 550, 1720, 70, "%d seconds = %d hours × 60 × 60 = %s  (this cache hands out at most %s)" % (
        secs, secs // 3600, "one day" if secs == 86400 else "%d days" % (secs // 86400), cap), AMBER, 27)
    label(d, 100, 660, "What protects the name: around the file", TEXT, 30, True)
    defences = [("who may change it", GREEN), ("review of changes", GREEN), ("short time to live", GREEN),
                ("DNSSEC: catches forgery on the way", CYAN)]
    x = 100
    for text, c in defences:
        w = int(font(SANS_B, 26).getlength(text)) + 50
        d.rounded_rectangle([x, 715, x + w, 775], radius=30, fill=PANEL, outline=c, width=3)
        d.text((x + w // 2, 745), text, font=font(SANS_B, 26), fill=c, anchor="mm")
        x += w + 20
    caption(d, "Fixing the file fixes the server, not the caches that already hold the wrong answer.", MUTED, 850)
