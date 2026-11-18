"""Pictures for the Workload Identity video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W, font

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def heading(name, head):
    """The full header line of a section, for the values it carries in brackets."""
    return next(ln for ln in act(name) if ln.startswith(head))


def section(name, head):
    """The section of an act whose header starts with `head`, as stripped lines."""
    found, cur = [], None
    for ln in act(name):
        if not ln.startswith("  "):
            cur = None
            if ln.startswith(head):
                cur = []
                found.append(cur)
        elif cur is not None:
            cur.append(ln.strip())
    return found[0]


def facts(lines):
    """`key   value` lines as a dict."""
    return dict(re.split(r"\s{2,}", ln, maxsplit=1) for ln in lines if re.search(r"\s{2,}", ln))


def header(d, x, y, text, colour, size=26):
    f = font(SANS_B, size)
    w = int(f.getlength(text)) + 44
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


def caption(d, text, colour=AMBER, y=935):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=2)


def tone(answer, good="yes"):
    return GREEN if answer == good else ROSE



def minus_key(lives):
    """The key of the back-dating line ("minus 10 s back-dating")."""
    return next(k for k in lives if k.startswith("minus"))


def cols(line):
    """A table line split on runs of two or more spaces."""
    return re.split(r"\s{2,}", line.strip())


def table(d, x, y, widths, rows, colours, size=28, h=58):
    for i, (row, colour) in enumerate(zip(rows, colours)):
        cx = x
        for w, v in zip(widths, row):
            cell(d, cx, y + i * (h + 12), w - 16, h, v, colour, size)
            cx += w


# ------------------------------------------------------------------ figures

def who_decides(img, d, cfg):
    """The agent decides a process's ID by the user it runs as, never by its claim."""
    users = section("TWO", "THE SAME PROGRAM")
    entries = section("TWO", "THE REGISTRATION ENTRIES")
    liar_who, liar_id = cols(users[-1])
    uid = re.search(r"user (\d+)", liar_who).group(1)
    entry = next(e for e in entries if "uid:" + uid in e)
    header(d, 60, 180, "THE INPUT: A PROCESS", CYAN)
    panel(d, 60, 240, 520, 300, CYAN)
    label(d, 90, 270, "runs as user", MUTED, 28, True)
    fit_mono(d, 90, 315, uid, TEXT, 44, 460)
    label(d, 90, 395, "says it is", MUTED, 28, True)
    fit_mono(d, 90, 440, "spiffe://bank.example", ROSE, 30, 470)
    fit_mono(d, 90, 480, "/payments", ROSE, 30, 470)
    draw.arrow_right(d, 600, 390, 690, MUTED, 5)
    header(d, 700, 180, "HOW THE AGENT DECIDES", AMBER)
    panel(d, 700, 240, 620, 300, AMBER)
    label(d, 730, 270, "1. ask the kernel: who owns the process?", TEXT, 26, True)
    fit_mono(d, 760, 315, "uid " + uid, AMBER, 32, 500)
    label(d, 730, 380, "2. find the entry with that selector", TEXT, 26, True)
    fit_mono(d, 760, 425, cols(entry)[0], AMBER, 32, 520)
    label(d, 730, 480, "the claim is never read", ROSE, 26, True)
    draw.arrow_right(d, 1340, 390, 1420, MUTED, 5)
    header(d, 1430, 180, "THE RESULT: ITS SVID", GREEN)
    panel(d, 1430, 240, 430, 300, GREEN)
    label(d, 1460, 270, "SPIFFE ID", MUTED, 28, True)
    domain, path = liar_id.rsplit("/", 1)
    fit_mono(d, 1460, 315, domain, GREEN, 30, 390)
    fit_mono(d, 1460, 355, "/" + path, GREEN, 30, 390)
    lives = facts(section("TWO", "HOW LONG"))
    label(d, 1460, 420, "lives", MUTED, 28, True)
    fit_mono(d, 1460, 465, lives[minus_key(lives)].split(" = ")[1].split(",")[0], GREEN, 32, 380)
    header(d, 60, 600, "THE SAME PROGRAM, RUN AS EACH USER", CYAN)
    for i, ln in enumerate(users[:3]):
        who, sid = cols(ln)
        cell(d, 60 + i * 610, 670, 580, 64, "%s: %s" % (who.replace("user ", ""), sid.split("/")[-1]), CYAN, 30)
    caption(d, "The platform decides who a workload is, not the workload.", AMBER, 880)


def shared_secret(img, d, cfg):
    """One key in three services, the ledger believing a header, and the leaked copy."""
    secret = section("ONE", "THE SECRET")[0]
    reqs = [cols(r) for r in section("ONE", "THE REQUESTS")[1:5]]
    leak = [cols(r) for r in section("ONE", "THE LEAK")[1:4]]
    header(d, 60, 175, "THE INPUT: " + secret.upper(), CYAN, 24)
    widths = [300, 300, 400, 200]
    for name, x in (("caller", 60), ("says", 360), ("request", 660), ("answer", 1060)):
        label(d, x + 8, 245, name, MUTED, 26, True)
    def colour(r):
        return ROSE if r[0] != r[1] or r[0] in ("attacker",) else (GREEN if r[3] in ("200", "403", "401") else ROSE)
    table(d, 60, 285, widths, reqs, [colour(r) for r in reqs], 28)
    header(d, 1280, 285, "INSIDE THE BANK", AMBER)
    label(d, 1280, 350, "rose: a caller that lied", ROSE, 26, True)
    label(d, 1280, 395, "and was believed", ROSE, 26, True)
    header(d, 60, 585, "THE LEAK: FRAUD-ENGINE'S COPY, OUTSIDE THE BANK", ROSE)
    table(d, 60, 645, widths, leak, [ROSE, ROSE, GREEN], 28)
    caption(d, "The key says someone who holds it is calling, never which service.", AMBER, 900)


def svid_parts(img, d, cfg):
    """A SPIFFE ID taken apart, and an SVID's dates: 10 s back-dated, 20 s to live."""
    parts = facts(section("TWO", "ONE SPIFFE ID"))
    lives = facts(section("TWO", "HOW LONG"))
    span = int(lives["Not After - Not Before"].split()[0])
    back = int(re.search(r"minus (\d+) s", minus_key(lives)).group(1))
    ttl = span - back
    header(d, 60, 175, "ONE SPIFFE ID, PART BY PART", CYAN)
    x = 60
    for name, w, c in (("scheme", 420, MUTED), ("trust domain", 520, AMBER), ("path", 420, GREEN)):
        cell(d, x, 245, w, 80, parts[name], c, 40)
        label(d, x + w // 2, 340, name, c, 26, True, "ma")
        x += w + 20
    label(d, 60, 400, "trust domain: who vouches for the name;   path: the workload inside it", MUTED, 26, True)
    header(d, 60, 470, "THE SVID'S DATES (seconds)", AMBER)
    x0, x1, y = 160, 1760, 620
    k = (x1 - x0) / span
    d.rectangle([x0, y - 25, x0 + back * k, y + 25], fill=MUTED)
    d.rectangle([x0 + back * k, y - 25, x1, y + 25], fill=GREEN)
    label(d, x0, y + 40, "Not Before", TEXT, 26, True, "ma")
    label(d, x0 + back * k, y + 40, "issued", TEXT, 26, True, "ma")
    label(d, x1, y + 40, "Not After", TEXT, 26, True, "ma")
    label(d, x0 + back * k / 2, y - 70, "%d s back-dated" % back, MUTED, 24, True, "ma")
    label(d, x0 + back * k + (x1 - x0 - back * k) / 2, y - 70, "%d s to live (the TTL)" % ttl, GREEN, 28, True, "ma")
    label(d, W // 2, 730, "%d s on the certificate - %d s back-dated = %d s" % (span, back, ttl), TEXT, 32, True, "ma")
    caption(d, "Twenty seconds after it is issued, this certificate proves nothing.", AMBER, 900)


def mutual_check(img, d, cfg):
    """The ledger authorizes by the ID in the certificate; two impostors are refused."""
    reqs = [cols(r) for r in section("THREE", "THE REQUESTS")[1:5]]
    imp = section("THREE", "TWO IMPOSTORS")
    header(d, 60, 175, "THE REQUESTS: MUTUAL TLS, THE ID FROM THE CERTIFICATE", CYAN)
    table(d, 60, 240, [320, 420, 200], reqs, [GREEN] * 4, 28)
    rules = section("ONE", "THE LEDGER'S RULES")
    label(d, 1060, 250, "the ledger's rules:", MUTED, 26, True)
    for i, r in enumerate(rules):
        fit_mono(d, 1060, 295 + i * 50, re.sub(r"\s{2,}", "  ", r), TEXT, 28, 800)
    header(d, 60, 560, "TWO IMPOSTORS", ROSE)
    for i, (what, why) in enumerate(((imp[0], imp[1]), (imp[2], imp[3]))):
        x = 60 + i * 920
        panel(d, x, 620, 880, 200, ROSE)
        head, result = what.rsplit(": ", 1)
        label(d, x + 25, 645, head, TEXT, 26, True)
        cell(d, x + 25, 695, 300, 56, result, GREEN, 30)
        fit_mono(d, x + 25, 768, why, TEXT, 28, 830)
    caption(d, "A name in a certificate counts only when SPIRE signed it.", AMBER, 900)


def rotation_timeline(img, d, cfg):
    """Fifty seconds of calls against SVIDs that live twenty seconds each."""
    test = facts(section("FOUR", "THE TEST"))
    res = facts(section("FOUR", "THE RESULT"))
    seconds = int(test["for"].split()[0])
    life = int(test["SVID lifetime"].split()[0])
    header(d, 60, 175, "THE TEST: " + test["for"].upper(), CYAN)
    x0, x1 = 160, 1760
    k = (x1 - x0) / seconds
    y = 300
    d.rectangle([x0, y - 6, x1, y + 6], fill=CYAN)
    for s in range(0, seconds + 1, 10):
        label(d, x0 + s * k, y + 20, "%d s" % s, MUTED, 24, True, "ma")
    for j in range(0, seconds // (life // 2) + 1):
        start = j * life / 2
        end = min(start + life, seconds)
        yy = 380 + (j % 2) * 50
        if start >= seconds:
            break
        d.rounded_rectangle([x0 + start * k, yy, x0 + end * k, yy + 36], radius=10, fill=BG_CELL, outline=GREEN, width=3)
    label(d, 60, 500, "one bar: one SVID, at most %d s long; the next is handed out before it ends (%s)" % (life, test["SVID lifetime"].split(", so ")[1]), MUTED, 26, True)
    header(d, 60, 570, "THE RESULT", GREEN)
    items = [("calls, at least 100", res["calls, at least 100"]), ("calls that failed", res["calls that failed"]),
             ("payments' SVID replaced", res["payments' SVID replaced"]), ("the ledger's, as seen", res["the ledger's, as seen"])]
    for i, (what, value) in enumerate(items):
        x = 60 + i * 460
        label(d, x + 210, 640, what, MUTED, 24, True, "ma")
        cell(d, x, 680, 420, 64, value, GREEN, 26)
    label(d, 60, 790, "renewal code in the services: " + res["renewal code in services"], GREEN, 30, True)
    caption(d, "Short-lived certificates, and nobody had to restart anything.", AMBER, 900)


def stolen_timeline(img, d, cfg):
    """The stolen copy works while it lives and is refused after; the real workload and the old secret go on."""
    theft = section("FIVE", "THE THEFT")[0]
    calls = facts(section("FIVE", "THE ATTACKER'S CALLS"))
    same = facts(section("FIVE", "AT THE SAME MOMENT"))
    later_key = next(k for k in calls if k.startswith("after"))
    after_s = int(re.search(r"after (\d+) s", later_key).group(1))
    life = int(re.search(r"> (\d+)", later_key).group(1))
    header(d, 60, 175, "THE THEFT: " + theft.upper(), ROSE, 24)
    header(d, 60, 260, "THE ATTACKER'S CALLS WITH THE COPY (seconds after the copy)", AMBER)
    x0, x1, y = 160, 1300, 430
    k = (x1 - x0) / 25
    d.rectangle([x0, y - 20, x0 + life * k, y + 20], fill=GREEN)
    d.rectangle([x0 + life * k, y - 20, x1, y + 20], fill=ROSE)
    for s in range(0, 26, 5):
        label(d, x0 + s * k, y + 30, str(s), MUTED, 24, True, "ma")
    label(d, x0 + life * k, y - 70, "%d s: the SVID ends" % life, AMBER, 26, True, "ma")
    cell(d, x0 - 80, y + 80, 260, 56, "at once: " + calls["at once"], ROSE, 26)
    cell(d, x0 + after_s * k - 130, y + 80, 300, 56, "%d s: %s" % (after_s, calls[later_key]), GREEN, 26)
    label(d, 60, 640, "the audit names: " + calls["the audit names"], ROSE, 28, True)
    header(d, 1380, 330, "AT THE SAME MOMENT", CYAN)
    panel(d, 1380, 390, 480, 260, CYAN)
    label(d, 1400, 410, "payments itself", MUTED, 26, True)
    cell(d, 1400, 450, 200, 50, same["payments itself"], GREEN, 26)
    label(d, 1400, 525, "the leaked shared secret", MUTED, 26, True)
    cell(d, 1400, 565, 200, 50, same["the leaked shared secret"], ROSE, 26)
    caption(d, "A copy is as good as the original while it lives; then it is worth nothing.", AMBER, 900)


def no_entry(img, d, cfg):
    """A user no entry selects gets no SVID; deleting an entry takes the identity away."""
    att = facts(section("SIX", "THE ATTACKER'S OWN USER"))
    deleted = facts(section("SIX", "THE PAYMENTS ENTRY IS DELETED"))
    uid = re.search(r"\((\d+)\)", heading("SIX", "THE ATTACKER'S OWN USER")).group(1)
    entries = [cols(e)[0] for e in section("TWO", "THE REGISTRATION ENTRIES")]
    header(d, 60, 175, "THE INPUT: USER %s, SAYING IT IS PAYMENTS" % uid, CYAN)
    label(d, 60, 250, "the entries select:", MUTED, 28, True)
    for i, e in enumerate(entries):
        cell(d, 380 + i * 380, 235, 350, 56, e, MUTED, 26)
    cell(d, 1520, 235, 340, 56, "unix:uid:%s ?" % uid, ROSE, 26)
    for i, (what, value) in enumerate(att.items()):
        x = 60 + i * 610
        label(d, x + 290, 330, what, MUTED, 26, True, "ma")
        cell(d, x, 370, 580, 64, value, GREEN, 30)
    header(d, 60, 500, "THE PAYMENTS ENTRY IS DELETED", AMBER)
    for i, (what, value) in enumerate(deleted.items()):
        x = 60 + i * 920
        label(d, x + 440, 575, what, MUTED, 26, True, "ma")
        cell(d, x, 615, 880, 64, value, GREEN, 30)
    caption(d, "No entry, no identity: a process cannot claim a name, and the operator can take one back.", AMBER, 880)


def eviction(img, d, cfg):
    """Evicting the attacker: change a shared secret everywhere, or wait out a short SVID."""
    sec = facts(section("SEVEN", "THE SHARED SECRET IS CHANGED"))
    svid = facts(section("SEVEN", "THE STOLEN SVID"))
    for x, title, colour, rows in ((60, "THE SHARED SECRET IS CHANGED", ROSE, list(sec.items())),
                                   (980, "THE STOLEN SVID", GREEN, list(svid.items()))):
        header(d, x, 180, title, colour)
        for i, (k, v) in enumerate(rows):
            y = 260 + i * 170
            panel(d, x, y, 880, 140, colour)
            label(d, x + 25, y + 20, k, MUTED, 28, True)
            fit_mono(d, x + 25, y + 75, v, TEXT, 30, 830)
    broken = sec["services that fail"].split(",")[0]
    label(d, 60, 650, "secret: the leaked copy %s, and %s services break until each is redeployed" % (sec["the leaked copy"], broken), ROSE, 28, True)
    label(d, 60, 700, "SVID: %s" % svid["services changed"].replace(":", " services changed:"), GREEN, 28, True)
    caption(d, "A short lifetime is the revocation that always works.", AMBER, 880)
