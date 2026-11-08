"""Pictures for the CIA triad video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (ACCENT, AMBER, CLIENT, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL,
                                   ROSE, SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
DARK = (15, 23, 42)
CONF, INTEG, AVAIL = CLIENT, AMBER, ACCENT     # one colour per promise


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("THE RESPONSES"), without the indent."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return [ln.strip() for ln in text.rstrip("\n").split("\n")]


def value(label, act, part=None):
    """The text after a label on a line of one act's output."""
    for ln in section(act, part):
        if ln.startswith(label):
            return ln[len(label):].strip()
    raise KeyError(label)


def marker(text):
    """'raj gets ...  <- LEAK' -> ('raj gets ...', 'LEAK')"""
    m = re.match(r"(.*?)\s+<- (.+)$", text)
    return (m.group(1), m.group(2)) if m else (text, "")


def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUESTS: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=DARK, anchor="mm")
    return x + w


def fit(d, xy, text, max_w, size, colour, bold=True, mono=False, anchor="lm"):
    """Text at the largest size up to `size` that fits `max_w`."""
    path = (MONO_B if mono else SANS_B) if bold else SANS
    while font(path, size).getlength(text) > max_w and size > 16:
        size -= 1
    d.text(xy, text, font=font(path, size), fill=colour, anchor=anchor)


def box(d, x, y, w, h, text, colour, size=28, mono=False, outline=None):
    """A framed value; text too wide for one line is split over two when the box is tall enough."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=outline or colour, width=3)
    path = MONO_B if mono else SANS_B
    if font(path, size).getlength(text) > w - 28 and h >= size * 2.8 and " " in text:
        words = text.split()
        cut = min(range(1, len(words)), key=lambda i: abs(len(" ".join(words[:i])) - len(" ".join(words[i:]))))
        top, bottom = " ".join(words[:cut]), " ".join(words[cut:])
        gap = int(size * 0.65)
        fit(d, (x + w // 2, y + h // 2 - gap), top, w - 28, size, colour, mono=mono, anchor="mm")
        fit(d, (x + w // 2, y + h // 2 + gap), bottom, w - 28, size, colour, mono=mono, anchor="mm")
        return
    fit(d, (x + w // 2, y + h // 2), text, w - 28, size, colour, mono=mono, anchor="mm")


def label(d, x, y, text, colour=MUTED, size=28, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    fit(d, (W // 2, y), text, W - 200, 34, colour, anchor="ma")


def hexbytes(text):
    h = text.replace(" ", "")
    return [h[i:i + 2] for i in range(0, len(h), 2)]


def cells(d, x, y, items, colour, w=88, h=64, size=30, gap=8):
    for i, t in enumerate(items):
        cx = x + i * (w + gap)
        d.rounded_rectangle([cx, y, cx + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
        d.text((cx + w // 2, y + h // 2), t, font=font(MONO_B, size), fill=colour, anchor="mm")


def split_text(d, x, y, text, tail, colour, tail_colour, size=26):
    """An order's text with its last field (the amount) in another colour, vertically centred on y."""
    f = font(MONO_B, size)
    head = text[:len(text) - len(tail)]
    d.text((x, y), head, font=f, fill=colour, anchor="lm")
    d.text((x + f.getlength(head), y), tail, font=f, fill=tail_colour, anchor="lm")


def responses():
    """The act-two table: {'header': [...], 'raj': [...], 'ana': [...], 'mallory': 'denied, ...'}"""
    rows = section("TWO", "THE RESPONSES")
    out = {"header": re.split(r"\s{2,}", rows[0])}
    for ln in rows[1:]:
        if ln.startswith("to mallory"):
            out["mallory"] = ln[len("to mallory"):].strip()
        else:
            cols = re.split(r"\s{2,}", ln)
            out[cols[0][3:]] = cols[1:]
    return out


# ------------------------------------------------------------------ figures

def one_order(img, d, cfg):
    """The core idea on one real order: each promise asks one question about it."""
    t = responses()
    full, hidden = dict(zip(t["header"], t["ana"])), dict(zip(t["header"], t["raj"]))
    order_id = re.search(r"reads (O-\d+)", section("TWO", "THE REQUESTS")[0]).group(1)
    d.rounded_rectangle([100, 215, 800, 760], radius=18, fill=PANEL, outline=LINE, width=3)
    label(d, 450, 240, "order " + order_id, TEXT, 40, True, "ma")
    marks = {"address": [CONF], "amount": [CONF, INTEG]}
    for i, name in enumerate(t["header"]):
        y = 320 + i * 105
        label(d, 140, y + 36, name, MUTED, 28, False, "lm")
        c = marks.get(name, [TEXT])[0]
        box(d, 330, y, 430, 72, full[name], c if name in marks else TEXT, 32, outline=c if name in marks else LINE)
        for mc in marks.get(name, [])[1:]:
            d.rounded_rectangle([330 - 8, y - 8, 760 + 8, y + 80], radius=16, outline=mc, width=3)
    promises = [
        ("CONFIDENTIALITY", CONF, "Who must not see it?",
         "support: address %s, amount %s" % (hidden["address"], hidden["amount"])),
        ("INTEGRITY", INTEG, "Who must not change it?",
         "amount stays %s unless finance changes it" % full["amount"]),
        ("AVAILABILITY", AVAIL, "Who must always reach it?",
         "%s, at checkout, even in a flood" % full["customer"]),
    ]
    for i, (name, c, question, answer) in enumerate(promises):
        y = 215 + i * 190
        d.rounded_rectangle([900, y, 1820, y + 165], radius=18, fill=PANEL, outline=c, width=4)
        header(d, 930, y + 20, name, c)
        label(d, 930, y + 78, question, TEXT, 34, True)
        fit(d, (930, y + 138), answer, 860, 30, TEXT, bold=False)
    caption(d, "One order, three questions. Each control in the demo answers one of them.", AMBER, 830)


def no_controls(img, d, cfg):
    """The first version: three requests, and what each one does when nothing is checked."""
    req = section("ONE", "THE REQUESTS")
    res = section("ONE", "THE RESULTS")
    rows = [(req[0], [res[0]], "confidentiality fails", CONF),
            (req[1], [res[1]], "integrity fails", INTEG),
            (req[2], [res[2], res[3]], "availability fails", AVAIL)]
    header(d, 100, 205, "THE REQUESTS", GREEN)
    header(d, 980, 205, "THE RESULTS  (nothing is checked)", ROSE)
    for i, (rq, rs, verdict, c) in enumerate(rows):
        y = 270 + i * 215
        box(d, 100, y, 740, 160, rq, TEXT, 32, outline=GREEN)
        draw.arrow_right(d, 850, y + 80, 970, MUTED, 4)
        label(d, 910, y + 42, "no check", MUTED, 22, False, "ma")
        d.rounded_rectangle([980, y, 1820, y + 160], radius=14, fill=BG_CELL, outline=ROSE, width=3)
        lines = []
        for r in rs:
            text, tag = marker(r)
            lines.append((text, TEXT, False))
            if tag:
                lines.append(("✗ " + tag + "   (" + verdict + ")", c, True))
        top = y + 80 - (len(lines) - 1) * 23
        for k, (text, colour, bold) in enumerate(lines):
            fit(d, (1005, top + k * 46), text, 790, 30, colour, bold=bold)
    caption(d, "One system, three broken promises.", AMBER, 925)


def role_filter(img, d, cfg):
    """Confidentiality: the response depends on the asker's role."""
    req = section("TWO", "THE REQUESTS")
    t = responses()
    header(d, 100, 205, "THE REQUESTS", GREEN)
    for i, r in enumerate(req):
        label(d, 140, 270 + i * 46, "•  " + r, TEXT, 30)
    header(d, 100, 425, "THE RESPONSES  (filtered by role)", ROSE)
    xs, ws = [340, 680, 960, 1360], [320, 260, 380, 300]
    for name, x, w in zip(t["header"], xs, ws):
        label(d, x + w // 2, 492, name, MUTED, 26, True, "ma")
    for i, who in enumerate(["raj", "ana"]):
        y = 535 + i * 100
        label(d, 110, y + 40, "to " + who, TEXT, 32, True, "lm")
        role = re.search(who + r" \((\w+)\)", " ".join(req)).group(1)
        label(d, 110, y + 76, role, MUTED, 22, False, "lm")
        for v, x, w in zip(t[who], xs, ws):
            c = ROSE if v == "[hidden]" else GREEN
            box(d, x, y, w - 16, 76, v, c, 30, mono=True)
    y = 760
    label(d, 110, y + 38, "to mallory", TEXT, 32, True, "lm")
    d.rounded_rectangle([340, y, 1644, y + 76], radius=12, fill=BG_CELL, outline=ROSE, width=3)
    fit(d, (370, y + 38), "✗  " + t["mallory"], 1240, 32, ROSE)
    caption(d, "Least privilege: each role gets only what its job needs.", AMBER, 900)


def seal_made(img, d, cfg):
    """Integrity: the order's text and a key go in, a 32-byte seal comes out."""
    text = value("order text", "THREE", "BEFORE SEALING")
    key = value("key", "THREE", "BEFORE SEALING")
    seal_rows = [r for r in section("THREE", "AFTER SEALING")]
    seal = [re.sub(r"^seal\s+", "", r) for r in seal_rows]
    header(d, 100, 205, "BEFORE SEALING  (the inputs)", GREEN)
    label(d, 100, 272, "the order, as one line of text", GREEN, 26, True)
    box(d, 100, 310, 900, 72, text, GREEN, 30, mono=True)
    label(d, 1060, 272, "the secret key", AMBER, 26, True)
    box(d, 1060, 310, 760, 72, key, AMBER, 30, mono=True)
    draw.arrow(d, 550, 388, 860, 450, GREEN, 4)
    draw.arrow(d, 1440, 388, 1060, 450, AMBER, 4)
    d.rounded_rectangle([760, 455, 1160, 545], radius=18, fill=PANEL, outline=TEXT, width=4)
    d.text((960, 500), "HMAC-SHA256", font=font(SANS_B, 38), fill=TEXT, anchor="mm")
    draw.arrow(d, 960, 550, 960, 595, ROSE, 4)
    e = header(d, 100, 600, "AFTER SEALING  (the seal)", ROSE)
    label(d, e + 24, 622, "32 bytes, written as 64 hex characters", TEXT, 28, True, "lm")
    for i, row in enumerate(seal):
        cells(d, 100, 670 + i * 80, hexbytes(row), ROSE, w=92, h=66, size=30, gap=7)
    caption(d, "Storage keeps the order and its seal side by side.", AMBER, 860)
    label(d, W // 2, 915, "Change one character, and the whole seal changes. Without the key, nobody can make a new one.",
          MUTED, 28, False, "ma")


def seal_steps(img, d, cfg):
    """How HMAC-SHA256 makes the seal: pad the key, hash with key xor 36, hash again with key xor 5c."""
    pad = value("key", "FOUR", "STEP 1")
    n_key, n_zero = [int(x) for x in re.findall(r"(\d+) (?:bytes|zero)", pad)[:2]]
    k36 = value("key xor 36", "FOUR", "STEP 2")
    k5c = value("key xor 5c", "FOUR", "STEP 3")
    s2, s3 = section("FOUR", "STEP 2"), section("FOUR", "STEP 3")
    inner = re.sub(r"^inner\s+", "", s2[1]) + " " + s2[2]
    seal = re.sub(r"^seal\s+", "", s3[1]) + " " + s3[2]
    text = value("order text", "THREE", "BEFORE SEALING")
    same = s3[-1]

    e = header(d, 100, 205, "STEP 1", AMBER)
    label(d, e + 20, 227, "pad the key with zero bytes to 64 bytes", TEXT, 30, True, "lm")
    unit = 22
    d.rounded_rectangle([100, 270, 100 + n_key * unit - 4, 334], radius=10, fill=BG_CELL, outline=AMBER, width=3)
    d.text((100 + n_key * unit // 2, 302), "key: %d bytes" % n_key, font=font(SANS_B, 30), fill=AMBER, anchor="mm")
    x0 = 100 + n_key * unit
    d.rounded_rectangle([x0, 270, x0 + n_zero * unit - 4, 334], radius=10, fill=BG_CELL, outline=MUTED, width=3)
    d.text((x0 + n_zero * unit // 2, 302), "%d zero bytes" % n_zero, font=font(SANS_B, 30), fill=MUTED, anchor="mm")
    label(d, x0 + n_zero * unit + 20, 302, "= %d bytes" % (n_key + n_zero), TEXT, 32, True, "lm")

    def step(y, name, colour, title, key_text, then_text, then_colour, out_name, out_value, out_colour):
        e = header(d, 100, y, name, colour)
        label(d, e + 20, y + 22, title, TEXT, 30, True, "lm")
        box(d, 100, y + 62, 660, 68, key_text, colour, 28, mono=True)
        d.text((785, y + 96), "+", font=font(SANS_B, 44), fill=TEXT, anchor="mm")
        box(d, 810, y + 62, 640, 68, then_text, then_colour, 26, mono=True)
        draw.arrow_right(d, 1460, y + 96, 1525, MUTED, 4)
        box(d, 1530, y + 62, 290, 68, "SHA-256", TEXT, 32)
        draw.arrow(d, 1675, y + 134, 1675, y + 160, out_colour, 3)
        box(d, 100, y + 162, 1720, 68, "%s  %s" % (out_name, out_value), out_colour, 28, mono=True)

    step(365, "STEP 2", CYAN, "the inner hash", "key ⊕ 36  " + k36, text, GREEN, "inner hash", inner, CYAN)
    step(625, "STEP 3", ROSE, "the seal", "key ⊕ 5c  " + k5c, "inner hash  " + inner.split()[0] + " …", CYAN,
         "seal", seal, ROSE)
    caption(d, same[0].upper() + same[1:], GREEN, 895)
    label(d, W // 2, 948, "A plain hash needs no key. HMAC mixes the key in twice, so only the key's owner can seal.",
          MUTED, 30, False, "ma")


def edit_caught(img, d, cfg):
    """An edit made directly in storage leaves the old seal, which no longer matches."""
    before_text = value("order", "FIVE", "BEFORE THE EDIT")
    after_text = value("order", "FIVE", "AFTER THE EDIT")
    stored = value("seal", "FIVE", "BEFORE THE EDIT")
    new = value("new seal", "FIVE", "ANA READS")
    reads = section("FIVE", "ANA READS")
    matches = [r for r in reads if r.startswith("matches")][0]
    refused, tag = marker([r for r in reads if r.startswith("refused")][0])
    old_amount, new_amount = before_text.split("|")[-1], after_text.split("|")[-1]

    def row(y, pill, colour, text, amount, amount_colour, seal_note):
        header(d, 100, y, pill, colour)
        label(d, 100, y + 58, "the order", MUTED, 24)
        d.rounded_rectangle([100, y + 90, 1060, y + 162], radius=12, fill=BG_CELL, outline=colour, width=3)
        split_text(d, 125, y + 126, text, amount, TEXT, amount_colour, 32)
        label(d, 1110, y + 58, "stored seal" + seal_note, MUTED, 24)
        box(d, 1110, y + 90, 710, 72, stored, AMBER, 32, mono=True)

    row(205, "BEFORE THE EDIT  (in storage)", GREEN, before_text, old_amount, GREEN, "")
    row(400, "AFTER THE EDIT  (mallory, directly in storage)", ROSE, after_text, new_amount, ROSE, "  (left as it was)")
    header(d, 100, 600, "ANA READS O-101  (the seal is worked out again)", AMBER)
    label(d, 100, 660, "new seal, from the order found in storage", MUTED, 24)
    box(d, 100, 692, 710, 72, new, ROSE, 32, mono=True)
    d.text((960, 728), "≠", font=font(SANS_B, 64), fill=ROSE, anchor="mm")
    label(d, 1110, 660, "stored seal", MUTED, 24)
    box(d, 1110, 692, 710, 72, stored, AMBER, 32, mono=True)
    label(d, 100, 815, matches + "   →   ✗ " + refused + "   <- " + tag, GREEN, 32, True, "lm")
    caption(d, "The seal does not stop the edit. It makes sure the edit cannot go unnoticed.", AMBER, 905)


def rate_limit(img, d, cfg):
    """Availability: the limiter counts each client's requests in a tick and allows at most 3."""
    req = section("SIX", "THE REQUESTS")
    trace = section("SIX", "HOW THE LIMIT DECIDES")
    limit = int(re.search(r"\((\d+) per client", [l for l in OUT.split("\n") if "HOW THE LIMIT" in l][0]).group(1))
    res = section("SIX", "THE RESULTS")
    e = header(d, 100, 205, "THE REQUESTS", GREEN)
    label(d, e + 24, 227, "  ·  ".join(req), TEXT, 30, False, "lm")
    header(d, 100, 285, "HOW THE LIMIT DECIDES  (%d per client per tick)" % limit, AMBER)
    for i, ln in enumerate(trace):
        y = 350 + i * 60
        m = re.match(r"(\w+)\s+request (\d+)\s+count (\d+), .*: (\w+)$", ln)
        if not m:
            label(d, 140, y + 25, ln, MUTED, 28, False, "lm")
            continue
        client, n, count, verdict = m.group(1), int(m.group(2)), int(m.group(3)), m.group(4)
        ok = verdict == "allowed"
        c = CYAN if client == "maya" else ROSE
        label(d, 140, y + 25, "%s  request %d" % (client, n), c, 30, True, "lm")
        for k in range(count):
            cx = 520 + k * 62
            d.rounded_rectangle([cx, y, cx + 52, y + 50], radius=8, fill=BG_CELL, outline=GREEN if k < limit else ROSE, width=3)
            d.text((cx + 26, y + 25), str(k + 1), font=font(MONO_B, 26), fill=GREEN if k < limit else ROSE, anchor="mm")
        lx = 520 + limit * 62 - 6
        d.line([(lx, y - 5), (lx, y + 55)], fill=AMBER, width=3)
        d.text((850, y + 25), "count %d" % count, font=font(SANS, 28), fill=MUTED, anchor="lm")
        d.text((1030, y + 25), ("✓ " if ok else "✗ ") + verdict, font=font(SANS_B, 30), fill=GREEN if ok else ROSE, anchor="lm")
    label(d, 520 + limit * 62, 340, "limit", AMBER, 20, True, "la")

    capacity = int(re.search(r"can do (\d+)", " ".join(req)).group(1))
    bot_without = int(re.search(r"bot (\d+) served", res[0]).group(1))
    bot_with = int(re.search(r"bot (\d+) served", res[1]).group(1))
    maya_with = int(re.search(r"maya (\d+) served", res[2]).group(1))
    header(d, 100, 725, "THE RESULTS  (%d places in one tick)" % capacity, ROSE)
    for i, (name, slots, note) in enumerate([
            ("without the limit", ["bot"] * bot_without, "maya refused"),
            ("with the limit", ["bot"] * bot_with + ["maya"] * maya_with, "maya served")]):
        y = 790 + i * 80
        label(d, 100, y + 30, name, TEXT, 30, True, "lm")
        for k in range(capacity):
            who = slots[k] if k < len(slots) else ""
            c = ROSE if who == "bot" else CYAN if who == "maya" else LINE
            cx = 420 + k * 96
            d.rounded_rectangle([cx, y, cx + 88, y + 60], radius=8, fill=BG_CELL, outline=c, width=3)
            d.text((cx + 44, y + 30), who or "free", font=font(SANS_B, 22), fill=c if who else MUTED, anchor="mm")
        label(d, 420 + capacity * 96 + 20, y + 30, ("✗ " if "refused" in note else "✓ ") + note,
              ROSE if "refused" in note else GREEN, 28, True, "lm")


def break_glass(img, d, cfg):
    """The bill: emergency access needs a reason, and every use is logged."""
    req = section("SEVEN", "THE REQUESTS")
    res = section("SEVEN", "THE RESPONSES")
    log = section("SEVEN", "THE AUDIT LOG")
    log_title = [l.strip() for l in OUT.split("\n") if "THE AUDIT LOG" in l][0].rstrip(":")
    header(d, 100, 205, "THE REQUESTS", GREEN)
    header(d, 1000, 205, "THE RESPONSES", ROSE)
    for i, (rq, rs) in enumerate(zip(req, res)):
        y = 270 + i * 120
        box(d, 100, y, 800, 100, rq, TEXT, 30, outline=GREEN)
        draw.arrow_right(d, 910, y + 50, 990, MUTED, 4)
        text, tag = marker(rs)
        c = GREEN if i == 2 else ROSE
        box(d, 1000, y, 820, 100, text + ("   <- " + tag if tag else ""), c, 30)
    header(d, 100, 655, log_title, AMBER)
    d.rounded_rectangle([100, 715, 1820, 845], radius=14, fill=BG_CELL, outline=AMBER, width=3)
    label(d, 130, 752, log[0], AMBER, 32, True, "lm")
    label(d, 170, 805, log[1], TEXT, 32, False, "lm")
    caption(d, "The promises pull against each other: the exception must exist, and it must be watched.", AMBER, 900)
