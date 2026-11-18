"""Pictures for the webhook security video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B,
                                   TEXT, W, font)

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


def section(name, head):
    return sections(name, head)[0]


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


def header(d, x, y, text, colour, size=26):
    f = font(SANS_B, size)
    w = int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=28, bold=False, anchor="la", max_w=None):
    f = font(SANS_B if bold else SANS, size)
    while max_w and f.getlength(text) > max_w and f.size > 18:
        f = font(SANS_B if bold else SANS, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la", max_w=None):
    f = font(MONO_B, size)
    while max_w and f.getlength(text) > max_w and f.size > 16:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def cell(d, x, y, w, h, text, colour, size=26):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=BG_CELL, outline=colour, width=3)
    mono(d, x + w // 2, y + h // 2, text, colour, size, "mm", w - 20)


def wrap(text, size, width):
    """Splits text into lines that fit `width` at a sans `size`."""
    f, lines, cur = font(SANS_B, size), [], ""
    for word in text.split():
        trial = (cur + " " + word).strip()
        if cur and f.getlength(trial) > width:
            lines.append(cur)
            cur = word
        else:
            cur = trial
    return lines + [cur]


def reply_box(d, x, y, w, h, reply, colour):
    """A response: the status code large, the message wrapped below it."""
    status, msg = reply.split(" ", 1)
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=colour, width=3)
    mono(d, x + w // 2, y + 34, status, colour, 34, "mm")
    for i, ln in enumerate(wrap(msg, 24, w - 30)[:3]):
        label(d, x + w // 2, y + 64 + i * 32, ln, TEXT, 24, True, "ma")


def colour_of(reply, good_2xx=True):
    code = int(reply.split()[0])
    if code == 409:
        return AMBER
    if code >= 400:
        return GREEN
    return GREEN if good_2xx else ROSE


def body_lines(lines):
    """The two lines of a printed JSON body, without their indentation."""
    return [ln.strip() for ln in lines if ln.strip().startswith(("{", '"'))]


def v1_parts(line):
    m = re.search(r"v1 sent (\w+)\.\.\.\s+computed (\w+)\.\.\.", line)
    return m.groups() if m else None


# ------------------------------------------------------------------ figures

def signed_message(img, d, cfg):
    """The core idea on one message: v1 is the HMAC of the time and the raw body, and one changed character changes it."""
    inputs = section("TWO", "THE INPUTS")
    secret = inputs[0].split()[1]
    t = inputs[1].split()[2]
    body = body_lines(inputs)
    head = [ln.strip() for ln in section("TWO", "THE HEADER SENT")]
    v1 = head[1][3:] + head[2]
    header(d, 60, 200, "THE MESSAGE AS SENT", CYAN)
    panel(d, 60, 260, 1800, 190, CYAN, 3)
    mono(d, 90, 280, "POST /webhooks/payments", MUTED, 26)
    mono(d, 90, 322, "Shop-Signature: t=" + t + ",v1=" + v1[:32] + "...", AMBER, 26, max_w=1740)
    mono(d, 90, 364, body[0] + body[1], TEXT, 26, max_w=1740)
    header(d, 60, 480, "HOW v1 IS MADE", AMBER)
    cell(d, 60, 540, 470, 70, "secret " + secret, AMBER, 24)
    label(d, 548, 575, "+", MUTED, 40, True, "mm")
    cell(d, 570, 540, 300, 70, t + ".", TEXT, 26)
    label(d, 888, 575, "+", MUTED, 40, True, "mm")
    cell(d, 910, 540, 440, 70, "raw body, " + re.search(r"\((\d+ bytes)\)", inputs[2]).group(1), TEXT, 26)
    draw.arrow_right(d, 1360, 575, 1440, MUTED, 4)
    cell(d, 1450, 540, 410, 70, v1[:16] + "...", GREEN, 26)
    label(d, 1655, 625, "HMAC-SHA256", MUTED, 24, True, "ma")
    changed = section("THREE", "THE MESSAGES AND RESPONSES")
    case2 = next(i for i, ln in enumerate(changed) if ln.startswith("2 "))
    sent, computed = v1_parts(changed[case2 + 1])
    header(d, 60, 690, "ONE CHARACTER CHANGED: A-1001 -> A-1002", ROSE)
    cell(d, 60, 750, 600, 70, "v1 sent      " + sent + "...", GREEN, 24)
    label(d, 700, 785, "differs from", MUTED, 26, True, "lm")
    cell(d, 890, 750, 600, 70, "v1 computed  " + computed + "...", ROSE, 24)
    draw.arrow_right(d, 1500, 785, 1580, MUTED, 4)
    mono(d, 1600, 785, changed[case2 + 2].strip().split()[0] + " refused", ROSE, 30, "lm")
    caption(d, "Only the secret's holder can make v1; any changed byte changes it.", AMBER, 900)


def forged_request(img, d, cfg):
    """Anyone posts a payment event with no signature; the store marks the order paid; the provider has no payment."""
    req = section("ONE", "THE REQUEST")
    resp = section("ONE", "THE RESPONSE")[0].strip()
    records = section("ONE", "THE PROVIDER'S OWN RECORDS")[0].strip()
    header(d, 60, 200, "THE REQUEST (anyone, no signature)", AMBER)
    panel(d, 60, 260, 1000, 200, AMBER, 3)
    for i, ln in enumerate(req):
        mono(d, 90, 285 + i * 50, ln.strip(), MUTED if i == 0 else TEXT, 26, max_w=940)
    draw.arrow_right(d, 1080, 360, 1180, MUTED, 4)
    header(d, 1200, 200, "THE RESPONSE (the store)", ROSE)
    reply_box(d, 1200, 260, 660, 200, resp, ROSE)
    header(d, 60, 530, "THE PROVIDER'S OWN RECORDS FOR A-1001", CYAN)
    what, mark = records.split("  <- ")
    panel(d, 60, 590, 1800, 110, CYAN, 3)
    mono(d, 100, 645, what, TEXT, 34, "lm")
    label(d, 1820, 645, mark, ROSE, 30, True, "rm")
    caption(d, "Anyone can post to the address: the message proves nothing.", AMBER, 820)


def hmac_steps(img, d, cfg):
    """The signature made step by step with the real values: what is signed, the mixed keys, the two hashes, the header."""
    inputs = section("TWO", "THE INPUTS")
    signed = [ln.strip() for ln in section("TWO", "STEP 1")]
    keys = [ln.strip() for ln in section("TWO", "STEP 2")]
    hashes = [ln.strip() for ln in section("TWO", "STEP 3")]
    head = [ln.strip() for ln in section("TWO", "THE HEADER SENT")]
    header(d, 60, 200, "THE INPUTS", CYAN)
    for i, ln in enumerate(inputs[:2]):
        mono(d, 80, 262 + i * 40, re.sub(r"\s+\(.*", "", ln.strip()), TEXT, 26)
    for i, ln in enumerate(body_lines(inputs)):
        mono(d, 80, 342 + i * 36, ln, MUTED, 22, max_w=840)
    header(d, 60, 440, "STEP 1  WHAT IS SIGNED", AMBER)
    panel(d, 60, 495, 880, 110, AMBER, 3)
    for i, ln in enumerate(signed):
        mono(d, 80, 515 + i * 40, ln, TEXT, 22, max_w=840)
    header(d, 1000, 200, "STEP 2  THE KEY, 64 BYTES", AMBER)
    for i, ln in enumerate(keys):
        name, rest = re.match(r"(padded key|XOR \w+ each)\s+(.*?)( \.\.\..*)?$", ln).group(1, 2)
        tail = ln.split("...")[1].strip()
        mono(d, 1020, 262 + i * 52, name, MUTED, 24)
        mono(d, 1230, 262 + i * 52, rest + " ...", TEXT, 24)
        if tail:
            label(d, 1860, 264 + i * 52, tail, CYAN, 24, True, "ra")
    header(d, 1000, 440, "STEP 3  TWO SHA-256 HASHES", AMBER)
    inner = hashes[1].split("=")[1].strip() + hashes[2]
    v1 = hashes[4].split("=")[1].strip() + hashes[5]
    mono(d, 1020, 500, hashes[0].replace("inner = ", "inner = "), MUTED, 22, max_w=840)
    mono(d, 1020, 534, inner[:32] + "...", TEXT, 26)
    mono(d, 1020, 584, hashes[3].replace("    ", " "), MUTED, 22, max_w=840)
    mono(d, 1020, 618, v1[:32] + "...", GREEN, 26)
    header(d, 60, 690, "AFTER SIGNING: THE HEADER SENT WITH THE BODY", GREEN)
    panel(d, 60, 750, 1800, 110, GREEN, 3)
    mono(d, 90, 768, "Shop-Signature: " + head[0], AMBER, 28)
    mono(d, 90, 812, head[1] + head[2], GREEN, 28, max_w=1740)
    caption(d, "Inner hash, then outer hash: without the secret, v1 cannot be made.", AMBER, 900)


def signature_checks(img, d, cfg):
    """Four messages: the v1 sent, the v1 the store computes from the bytes it received, and the response."""
    lines = section("THREE", "THE MESSAGES AND RESPONSES")
    cases, cur = [], None
    for ln in lines:
        if not ln.startswith(" "):
            cur = {"title": ln, "v1": None}
            cases.append(cur)
        elif v1_parts(ln):
            cur["v1"] = v1_parts(ln)
        else:
            cur["reply"] = ln.strip()
    for j, (x, h) in enumerate(((60, "THE MESSAGE"), (930, "v1 SENT"), (1180, "v1 COMPUTED"), (1440, "THE RESPONSE"))):
        label(d, x, 205, h, MUTED, 24, True)
    for i, c in enumerate(cases):
        y = 245 + i * 135
        ok = c["reply"].startswith("2")
        panel(d, 60, y, 1800, 120, GREEN if ok else LINE, 3 if ok else 2)
        label(d, 85, y + 60, c["title"], TEXT, 26, True, "lm", 820)
        if c["v1"]:
            sent, computed = c["v1"]
            same = sent == computed
            cell(d, 930, y + 28, 230, 64, sent[:8] + "...", AMBER, 26)
            cell(d, 1180, y + 28, 230, 64, computed[:8] + "...", GREEN if same else ROSE, 26)
        else:
            cell(d, 930, y + 28, 480, 64, "no header", ROSE, 26)
        status, msg = c["reply"].split(" ", 1)
        mono(d, 1440, y + 42, status, GREEN if ok else ROSE, 32, "lm")
        label(d, 1510, y + 42, msg.split(": ")[0], TEXT, 24, True, "lm", 340)
        if ": " in msg:
            label(d, 1440, y + 86, msg.split(": ", 1)[1], MUTED, 24, False, "lm", 410)
    http = section("THREE", "THE SAME OVER THE LOCAL HTTP SERVER")[0].strip()
    label(d, 60, 800, "Over the local HTTP server:  " + http, CYAN, 28, True)
    caption(d, "The store recomputes v1 from the bytes it received, then compares.", AMBER, 900)


def replay_timeline(img, d, cfg):
    """A real refund sent again: paid twice with signatures only, refused or ignored with a time window and a record of ids."""
    def steps(lines):
        out = []
        for i in range(0, len(lines) - 1):
            if lines[i].startswith("the ") or lines[i].startswith("a copy"):
                if i + 1 < len(lines) and lines[i + 1].startswith("  "):
                    out.append((lines[i].rstrip(":"), lines[i + 1].strip()))
        return out
    only = section("FOUR", "WITH SIGNATURES ONLY")
    full = section("FOUR", "WITH A 300-SECOND WINDOW")
    paid_only = next(ln for ln in only if ln.startswith("refunds paid")).split("  <-")[0]
    paid_full = next(ln for ln in full if ln.startswith("refunds paid"))
    header(d, 60, 200, "WITH SIGNATURES ONLY", ROSE)
    label(d, 1860, 222, paid_only, ROSE, 28, True, "rm")
    for i, (when, reply) in enumerate(steps(only)):
        x = 60 + i * 910
        label(d, x, 262, when, MUTED, 26, True, max_w=880)
        reply_box(d, x, 300, 880, 140, reply, GREEN if i == 0 else ROSE)
    header(d, 60, 480, "WITH A 300-SECOND WINDOW AND A RECORD OF EVENT IDS", GREEN)
    label(d, 1860, 502, paid_full, GREEN, 28, True, "rm")
    for i, (when, reply) in enumerate(steps(full)):
        x = 60 + i * 450
        m = re.match(r"(.*?)\s*\((.*)\)$", when)
        top, sub = (m.group(1), m.group(2)) if m else (when, "")
        label(d, x, 540, top.replace("the copy ", "copy ").replace("with its t edited", "t edited"), MUTED, 24, True, max_w=430)
        label(d, x, 572, sub, AMBER, 24, False, max_w=430)
        reply_box(d, x, 612, 430, 200, reply, colour_of(reply))
    caption(d, "Age = now - t. The time is signed, so it cannot be edited.", AMBER, 880)


def event_order(img, d, cfg):
    """Five arrivals with each order's state before and the reply, then the secrets accepted during a change."""
    events = section("FIVE", "THE EVENTS")
    replies = section("FIVE", "THE RESPONSES")
    label(d, 60, 200, "THE EVENT, AS IT ARRIVES", MUTED, 24, True)
    label(d, 700, 200, "STATE", MUTED, 24, True)
    label(d, 900, 200, "THE REPLY", MUTED, 24, True)
    for i, (ev, rp) in enumerate(zip(events, replies)):
        y = 240 + i * 74
        n, eid, order, typ = ev.split()[:4]
        note = ev.split("  (")[1].rstrip(")") if "  (" in ev else ""
        _, state, reply = rp.split(None, 2)
        c = colour_of(reply)
        panel(d, 60, y, 1800, 62, c if c != GREEN else LINE, 2)
        mono(d, 80, y + 31, n, MUTED, 26, "lm")
        mono(d, 120, y + 31, order + " " + typ.replace("payment.", ""), TEXT, 26, "lm")
        if note:
            label(d, 390, y + 31, note, AMBER, 24, True, "lm")
        cell(d, 700, y + 9, 150, 44, state, CYAN, 24)
        mono(d, 900, y + 31, reply, c, 24, "lm", 950)
    secrets = [ln.split(None, 1) for ln in section("FIVE", "CHANGING THE SECRET")]
    msgs = section("FIVE", "THE MESSAGES AND RESPONSES")
    header(d, 60, 640, "CHANGING THE SECRET", CYAN)
    results = {}
    for i in range(0, len(msgs), 2):
        phase, rest = msgs[i].split(", signed with ")
        results.setdefault(phase, []).append(rest.rstrip(":")[-4:] + ": " + msgs[i + 1].split()[0])
    for i, (phase, keys) in enumerate(secrets):
        x = 60 + i * 610
        panel(d, x, 700, 580, 150, CYAN if phase == "changeover" else LINE, 2)
        label(d, x + 20, 718, phase, CYAN, 26, True)
        label(d, x + 20, 756, "accepts " + ", ".join(k[-4:] for k in keys.split(", ")), TEXT, 24)
        if phase in results:
            label(d, x + 20, 796, "signed " + ",  signed ".join(results[phase]), GREEN if phase != "after" else AMBER, 24, True)
    caption(d, "Each event is applied once, and only in an allowed state.", AMBER, 900)


def raw_bytes(img, d, cfg):
    """The raw body and the same event written again: different bytes, a different v1, a different answer."""
    raw = [ln.strip() for ln in section("SIX", "THE RAW BODY")]
    again = [ln.strip() for ln in section("SIX", "THE SAME EVENT, PARSED")]
    resp = [ln.strip() for ln in section("SIX", "THE RESPONSES")]
    sent = re.search(r"v1 is (\w+)", heading("SIX", "THE RESPONSES")).group(1)
    rows = [(heading("SIX", "THE RAW BODY").rstrip(":"), raw, resp[2], resp[3]),
            (heading("SIX", "THE SAME EVENT, PARSED").rstrip(":"), again, resp[0], resp[1])]
    label(d, 1860, 205, "the header's v1:  " + sent + "...", AMBER, 26, True, "ra")
    for i, (title, body, comp, reply) in enumerate(rows):
        y = 250 + i * 270
        ok = reply.startswith("2")
        c = GREEN if ok else ROSE
        header(d, 60, y, title, c, 24)
        panel(d, 60, y + 56, 1140, 110, c, 3)
        for j, ln in enumerate(body):
            mono(d, 85, y + 72 + j * 42, ln, TEXT, 26, max_w=1090)
        mono(d, 1230, y + 90, "computed " + comp.split("computed ")[1], c, 26)
        mono(d, 1230, y + 140, reply.split(":")[0] if not ok else reply, c, 26, max_w=630)
    api = section("SIX", "THE PROVIDER'S API")[0].strip()
    header(d, 60, 800, "THE PROVIDER'S API, BEFORE SHIPPING", CYAN)
    label(d, 760, 822, api, TEXT, 28, True, "lm")
    caption(d, "Check the raw bytes first; for valuable events, ask the provider too.", AMBER, 910)
