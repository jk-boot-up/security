"""Pictures for the HTTP basics and security headers video, drawn by the `figure` slide kind.

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
    """One act of the output ("TWO"), or one part of it ("THE REQUEST")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def rows(act, part):
    return [ln[4:] for ln in section(act, part).splitlines()]


def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUEST: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def node(d, x, y, w, h, text, c, size=28, mono=True, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=c, width=3)
    f = draw.fitted_font(MONO_B if mono else SANS_B, text, w - 20, size, 14)
    d.text((x + w // 2, y + h // 2), text, font=f, fill=c, anchor="mm")


def wire_line(d, x, y, text, colour, size=30):
    """A line of the message in mono, with every written-out \\r\\n drawn in rose."""
    f = font(MONO_B, size)
    for i, piece in enumerate(re.split(r"(\\r\\n)", text)):
        d.text((x, y), piece, font=f, fill=ROSE if piece == "\\r\\n" else colour, anchor="lm")
        x += f.getlength(piece)
    return x


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (16 if sub else 0)), text, font=font(SANS_B, 38), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")


# ------------------------------------------------------------------ figures

def request_text(img, d, cfg):
    """One request, line by line, with the characters of each line counted up to the total."""
    lines = rows("ONE", "THE REQUEST")
    parts = [re.match(r"(\S+(?: \S+)*)\s+(\d+)(?: \+ 2)?\s+=\s+(\d+)", ln).groups() for ln in lines[:-1]]
    total = re.search(r"(\d+) characters", lines[-1]).group(1)
    roles = [("request line", CYAN), ("header", CLIENT), ("header", CLIENT), ("blank line", MUTED), ("body", GREEN)]
    e = header(d, 100, 205, "THE REQUEST", GREEN)
    label(d, e + 24, 227, "what the browser sends: plain text, five lines", TEXT, 30, True, "lm")
    for i, ((text, chars, size), (role, c)) in enumerate(zip(parts, roles)):
        y = 280 + i * 80
        label(d, 100, y + 32, role, c, 28, True, "lm")
        d.rounded_rectangle([340, y, 1250, y + 64], radius=10, fill=BG_CELL, outline=c, width=3)
        wire_line(d, 370, y + 32, text, TEXT if c != MUTED else MUTED, 32)
        said = "%s + 2 = %s" % (chars, size) if text.endswith("\\r\\n") else "%s" % size
        label(d, 1290, y + 32, said, c, 32, True, "lm")
    sums = " + ".join(p[2] for p in parts)
    label(d, 100, 700, "\\r\\n ends each line: 2 invisible characters.", MUTED, 28, False, "lm")
    label(d, 1290, 700, "%s = %s" % (sums, total), AMBER, 30, True, "lm")
    body = parts[-1][0]
    label(d, 100, 770, "Content-Length: %s  =  these %s body bytes" % (parts[2][0].split()[-1][:-4], len(body)),
          GREEN, 28, True, "lm")
    for i, ch in enumerate(body):
        node(d, 100 + i * 70, 812, 62, 62, ch, GREEN, 30)
    caption(d, "The blank line ends the headers. Content-Length says where the body ends.", AMBER, 920)


def parse_roundtrip(img, d, cfg):
    """The text goes into the parser and comes out as fields, then is written back unchanged."""
    lines = [re.match(r"(.+?)\s{2,}", ln + "  ").group(1) for ln in rows("ONE", "THE REQUEST")[:-1]]
    fields = [ln.split(None, 1) for ln in rows("ONE", "THE PARSED REQUEST")]
    same = re.search(r"same text: (\w+)", section("ONE")).group(1)
    header(d, 100, 205, "THE REQUEST", GREEN)
    d.rounded_rectangle([100, 270, 760, 640], radius=14, fill=BG_CELL, outline=GREEN, width=3)
    for i, text in enumerate(lines):
        wire_line(d, 130, 310 + i * 68, text, TEXT, 28)
    draw.arrow_right(d, 775, 455, 860, CYAN, 4)
    big_box(d, 870, 380, 300, 150, "HttpParser", "reads strictly", CYAN)
    draw.arrow_right(d, 1180, 455, 1265, CYAN, 4)
    header(d, 1275, 205, "THE PARSED REQUEST", ROSE)
    for i, (name, val) in enumerate(fields):
        y = 270 + i * 92
        label(d, 1275, y + 36, name, MUTED, 28, True, "lm")
        node(d, 1420, y, 420, 72, val.replace(", as Content-Length said", ""), ROSE, 28)
    draw.arrow(d, 1555, 650, 1555, 720, AMBER, 4)
    node(d, 430, 730, 1410, 80, "written out again  →  the same text: %s" % same, AMBER, 32, False)
    draw.arrow_left(d, 428, 770, 300, AMBER, 4)
    draw.arrow_up(d, 300, 770, 650, AMBER, 4)
    caption(d, "Every line was chosen by the sender, so the server must check every line.", AMBER, 900)


def smuggling(img, d, cfg):
    """Content-Length says 4, but 47 bytes follow: a second request hides after the body."""
    req = rows("TWO", "THE REQUEST")
    pieces = [re.match(r"(body|hidden)\s+(.+?)\s+(\d+) bytes", ln).groups() for ln in req if "bytes" in ln and "+" not in ln]
    sum_line = re.search(r"after the blank line: (.+)", section("TWO")).group(1)
    answer = rows("TWO", "THE PARSER'S ANSWER")
    left = re.search(r"other (\d+) as", section("TWO")).group(1)
    e = header(d, 100, 205, "THE REQUEST", GREEN)
    label(d, e + 24, 227, "headers say Content-Length: 4, then the body and more", TEXT, 28, True, "lm")
    x, unit, y = 100, 36, 300
    for kind, text, n in pieces:
        n = int(n)
        c = GREEN if kind == "body" else ROSE
        w = n * unit - 6
        d.rounded_rectangle([x, y, x + w, y + 80], radius=10, fill=BG_CELL, outline=c, width=3)
        f = draw.fitted_font(MONO_B, text, w - 14, 26, 14)
        d.text((x + w // 2, y + 40), text, font=f, fill=c, anchor="mm")
        label(d, x + w // 2, y + 92, "%d bytes" % n, c, 24, True, "ma")
        x += n * unit
    label(d, 100, 440, "after the blank line: " + sum_line, AMBER, 32, True, "lm")
    e = header(d, 100, 510, "TWO READERS, TWO ANSWERS", ROSE)
    label(d, 100, 600, "a proxy that trusts Content-Length", TEXT, 28, True, "lm")
    node(d, 700, 568, 4 * unit - 6, 64, "abcd", GREEN, 26)
    label(d, 700 + 4 * unit + 10, 600, "request 1 ends", GREEN, 24, False, "lm")
    node(d, 1060, 568, 760, 64, "%s bytes left = the next request: GET /admin" % left, ROSE, 26, False)
    label(d, 100, 710, "this strict parser", TEXT, 28, True, "lm")
    node(d, 700, 678, 1120, 64, answer[0], GREEN, 28, False)
    caption(d, "Two programs, two ends for one request: that gap is request smuggling.", AMBER, 820)
    label(d, W // 2, 880, "So anything ambiguous is refused: both length headers, two lengths, odd names.", MUTED, 28, False, "ma")


def injection(img, d, cfg):
    """A line break in the search text adds a header, unless the writer refuses it."""
    text = dict(ln.split(None, 1) for ln in rows("THREE", "THE SEARCH TEXT"))
    before = [ln.split("  <-")[0] for ln in rows("THREE", "BEFORE THE FIX")]
    after = dict(ln.split(None, 1) for ln in rows("THREE", "AFTER THE FIX"))
    e = header(d, 100, 205, "THE SEARCH TEXT", GREEN)
    label(d, e + 24, 227, "typed by the customer, put into the redirect's Location", TEXT, 28, True, "lm")
    for i, key in enumerate(("normal", "attack")):
        y = 270 + i * 76
        label(d, 100, y + 30, key, CLIENT if key == "normal" else ROSE, 28, True, "lm")
        d.rounded_rectangle([260, y, 1100, y + 60], radius=10, fill=BG_CELL, outline=LINE, width=2)
        wire_line(d, 285, y + 30, text[key], TEXT, 28)
    header(d, 100, 440, "BEFORE THE FIX", ROSE)
    label(d, 100, 510, "the browser reads three lines:", MUTED, 26, False, "lm")
    for i, ln in enumerate(before):
        y = 540 + i * 70
        c = ROSE if ln.startswith("Set-Cookie") else TEXT
        d.rounded_rectangle([100, y, 900, y + 58], radius=10, fill=BG_CELL, outline=c if c == ROSE else LINE, width=3)
        label(d, 125, y + 29, ln, c, 28, True, "lm")
    label(d, 100, 770, "↑ a header the attacker wrote", ROSE, 26, True, "lm")
    header(d, 1000, 440, "AFTER THE FIX", GREEN)
    label(d, 1000, 525, "normal search", CLIENT, 26, True, "lm")
    node(d, 1000, 550, 820, 58, after["normal"], GREEN, 26)
    label(d, 1000, 640, "attack", ROSE, 26, True, "lm")
    node(d, 1000, 665, 820, 58, after["attack"], GREEN, 24, False)
    caption(d, "Refuse or encode line breaks in anything that reaches a header.", AMBER, 900)


def cookies(img, d, cfg):
    """Four Set-Cookie headers and what each misses, worked out attribute by attribute."""
    act = section("FOUR")
    raw = [ln[4:] for block in re.findall(r"^  THE COOKIES.*\n((?:    .*\n?)*)", act, re.M) for ln in block.splitlines()]
    texts = []
    for ln in raw:
        m = re.match(r"cookie (\d)  (.+)", ln)
        if m:
            texts.append([m.group(2)])
        else:
            texts[-1].append(ln.strip())
    verdict = "".join(re.findall(r"^  THE VERDICT.*\n((?:    .*\n?)*)", act, re.M))
    probs = [re.findall(r"^      - (.+)$", block, re.M)
             for block in re.split(r"^    cookie \d.*\n", verdict, flags=re.M)[1:]]
    counts = re.findall(r"cookie \d  (\d+) problem", verdict)
    cols = [("HttpOnly", lambda p: "no HttpOnly" in p),
            ("Secure", lambda p: p.startswith("no Secure")),
            ("SameSite", lambda p: "SameSite" in p and "HttpOnly" not in p),
            ("__Host- rule", lambda p: "__Host-" in p)]
    header(d, 100, 205, "THE COOKIES", GREEN, 760)
    header(d, 900, 205, "THE VERDICT", ROSE, 920)
    xs = [990, 1180, 1370, 1570]
    for x, (name, _) in zip(xs, cols):
        label(d, x, 280, name, TEXT, 26, True, "ma")
    label(d, 1760, 280, "problems", TEXT, 26, True, "ma")
    for i, (lines, p, n) in enumerate(zip(texts, probs, counts)):
        y = 330 + i * 130
        d.rounded_rectangle([100, y, 1840, y + 112], radius=12, fill=BG_CELL, outline=LINE, width=2)
        f = font(MONO_B, 22)
        for j, ln in enumerate(lines):
            d.text((125, y + 56 + (j - (len(lines) - 1) / 2) * 34), ln, font=f, fill=TEXT, anchor="lm")
        for x, (name, bad) in zip(xs, cols):
            if name.startswith("__Host") and not lines[0].startswith("__Host-"):
                mark, c = "–", MUTED
            else:
                hit = any(bad(q) for q in p)
                mark, c = ("✗", ROSE) if hit else ("✓", GREEN)
            d.text((x, y + 56), mark, font=font(SANS_B, 52), fill=c, anchor="mm")
        d.text((1760, y + 56), n, font=font(SANS_B, 52), fill=GREEN if n == "0" else ROSE, anchor="mm")
    caption(d, "A login cookie:  Secure; HttpOnly; SameSite=Lax; Path=/", AMBER, 875)
    label(d, W // 2, 930, "HttpOnly: no script reads it.  Secure: HTTPS only.  SameSite: not sent from other sites.",
          MUTED, 28, False, "ma")


def header_audit(img, d, cfg):
    """Five headers on three responses, and how each score is added up."""
    names = ["Strict-Transport-Security", "Content-Security-Policy", "X-Content-Type-Options",
             "X-Frame-Options", "Referrer-Policy"]

    def table(part):
        return {ln[:26].strip(): ln[26:].strip() for ln in rows("FIVE", part) if ln[:26].strip() in names}

    hard = table("THE RESPONSES")
    hard_pts = re.search(r"hardened server\s+([01 +]+) = (\d) of 5", section("FIVE")).groups()
    hard_ok = dict(zip(names, (p == "1" for p in hard_pts[0].split(" + "))))
    weak = table("HEADERS SET BADLY")
    weak_ok = {k: v.startswith("ok") for k, v in table("THE AUDIT, HEADER BY HEADER").items()}
    plain_score = re.search(r"plain server\s+[01 +]+ = (\d) of 5", section("FIVE")).group(1)
    weak_score = re.search(r"score (.+) of 5", section("FIVE")).group(1)
    cols = [("PLAIN SERVER", 600, {n: "missing" for n in names}, {n: False for n in names}),
            ("HARDENED SERVER", 1020, hard, hard_ok),
            ("SET BADLY", 1460, weak, weak_ok)]
    e = header(d, 100, 205, "THE HEADERS", GREEN)
    label(d, e + 24, 227, "three real responses, the same five names", TEXT, 28, True, "lm")
    for title, x, _, _ in cols:
        label(d, x + 200, 285, title, CLIENT, 24, True, "ma")
    for i, n in enumerate(names):
        y = 325 + i * 78
        label(d, 100, y + 30, n, TEXT, 26, True, "lm")
        for _, x, vals, ok in cols:
            c = GREEN if ok[n] else ROSE
            node(d, x, y, 400, 62, vals[n], c, 22)
    header(d, 100, 730, "THE AUDIT", ROSE)
    label(d, 100, 810, "one point per header", MUTED, 26, False, "lm")
    scores = [" + ".join("1" if ok[n] else "0" for n in names) for _, _, _, ok in cols]
    totals = [plain_score, hard_pts[1], weak_score.split("=")[-1].strip()]
    for (_, x, _, _), s, t in zip(cols, scores, totals):
        node(d, x, 780, 400, 62, "%s = %s of 5" % (s, t), AMBER, 24)
    caption(d, "A header is only worth its value: present but weak still fails.", AMBER, 900)
