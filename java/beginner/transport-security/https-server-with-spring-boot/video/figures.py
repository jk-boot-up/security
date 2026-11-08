"""Pictures for the HTTPS server with Spring Boot video, drawn by the `figure` slide kind.

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
        text = re.search(r"^  %s\b.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
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
    """A line in mono, with every written-out \\r\\n drawn in rose."""
    f = font(MONO_B, size)
    for piece in re.split(r"(\\r\\n)", text):
        d.text((x, y), piece, font=f, fill=ROSE if piece == "\\r\\n" else colour, anchor="lm")
        x += f.getlength(piece)
    return x


def big_box(d, x, y, w, h, text, sub, colour):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (16 if sub else 0)), text, font=font(SANS_B, 34), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 22), fill=MUTED, anchor="mm")



def kv(act, part, width=13):
    return [(ln[:width].strip(), ln[width:].strip()) for ln in rows(act, part)]


# ------------------------------------------------------------------ figures

def who_checks(img, d, cfg):
    """The server shows its certificate, the client checks it; with mutual TLS the reverse too."""
    server = kv("ONE", "THE CERTIFICATE", 10)
    clients = {ln[:10].strip(): ln[10:].strip() for ln in rows("FOUR", "THE CLIENTS")}
    big_box(d, 100, 230, 420, 140, "client", "Ana's app, or a browser", CLIENT)
    big_box(d, 1400, 230, 420, 140, "the shop's server", "Spring Boot, Tomcat", CYAN)
    draw.arrow_left(d, 1395, 280, 525, GREEN, 5)
    label(d, 960, 245, "1. the server shows its certificate", GREEN, 26, True, "ma")
    node(d, 560, 300, 800, 54, "%s, issued by %s" % (server[0][1], server[1][1].split("   ")[0]), GREEN, 24)
    draw.arrow_right(d, 525, 420, 1395, AMBER, 5)
    label(d, 960, 385, "2. with mutual TLS, the client shows one too", AMBER, 26, True, "ma")
    node(d, 560, 440, 800, 54, clients["ana"], AMBER, 24)
    header(d, 100, 560, "WHO CHECKS WHAT", CYAN)
    for i, t in enumerate(["the client: is the issuer a root I trust, and does it name the host?",
                           "the server (mutual TLS): is the client's issuer a root I trust?",
                           "both trust stores hold the same demo root"]):
        label(d, 130, 640 + i * 56, "•  " + t, TEXT, 28, False, "lm")
    caption(d, "In Spring Boot the keys, the trust and the versions are SSL bundles: properties.", AMBER, 880)


def bundle(img, d, cfg):
    """Three properties, the certificate they serve, and four attempts with their results."""
    props = rows("ONE", "THE CONFIGURATION")
    cert = kv("ONE", "THE CERTIFICATE", 10)
    att = [ln.split(None, 1) for ln in rows("ONE", "THE ATTEMPTS")]
    res = {}
    for ln in rows("ONE", "THE RESULTS"):
        if ln.startswith(" "):
            res["a"] += ", " + ln.strip()
        else:
            res[ln.split(None, 1)[0]] = ln.split(None, 1)[1]
    header(d, 100, 205, "THE CONFIGURATION", GREEN)
    for i, p in enumerate(props):
        k, v = p.split("=", 1)
        f = font(MONO_B, 24)
        d.text((100, 280 + i * 40), k + "=", font=f, fill=MUTED, anchor="lm")
        d.text((100 + f.getlength(k + "="), 280 + i * 40), v, font=f, fill=GREEN, anchor="lm")
    header(d, 1250, 205, "THE CERTIFICATE", GREEN)
    for i, (k, v) in enumerate(cert):
        label(d, 1250, 280 + i * 50, k, MUTED, 24, True, "lm")
        node(d, 1370, 258 + i * 50, 450, 44, v.split("   ")[0], GREEN, 22)
    header(d, 100, 420, "THE ATTEMPTS", GREEN)
    header(d, 1080, 420, "THE RESULTS", ROSE)
    for i, (k, what) in enumerate(att):
        y = 490 + i * 90
        node(d, 100, y, 64, 70, k, CLIENT, 30)
        node(d, 180, y, 870, 70, what, TEXT, 24, False)
        r = res[k]
        ok = not r.startswith("refused")
        head_, _, why = r.partition("  ")
        node(d, 1080, y, 280 if not ok else 740, 70, head_ if not ok else r, AMBER if ok else GREEN, 24, False)
        if not ok:
            label(d, 1380, y + 35, why, TEXT, 22, False, "lm")
    caption(d, "No TLS code: the properties decide the keys, the trust and the versions.", AMBER, 880)


def redirect_cookie(img, d, cfg):
    """The plain request turned away, and the HTTPS answer's HSTS and cookie, worked out."""
    req = rows("TWO", "THE REQUEST")[0]
    resp = [r.strip() for r in rows("TWO", "THE RESPONSE")]
    ans = [r.strip() for r in rows("TWO", "THE HTTPS ANSWER")]
    nxt = [ln.split(None, 2) for ln in rows("TWO", "THE NEXT REQUESTS")]
    header(d, 100, 205, "THE REQUEST (plain HTTP)", GREEN)
    node(d, 100, 270, 820, 58, req, GREEN, 24)
    header(d, 100, 360, "THE RESPONSE", ROSE)
    status, _, loc = resp[0].partition("  ")
    node(d, 100, 425, 120, 58, status, ROSE, 28)
    node(d, 240, 425, 680, 58, loc, ROSE, 18)
    node(d, 100, 500, 820, 54, resp[1], MUTED, 22)
    header(d, 1000, 205, "THE HTTPS ANSWER (GET /login)", ROSE)
    label(d, 1000, 290, ans[0].rstrip(":"), TEXT, 26, True, "lm")
    node(d, 1000, 315, 820, 54, ans[1], GREEN, 24)
    node(d, 1000, 385, 820, 54, ans[2], AMBER, 24)
    label(d, 1000, 470, "Set-Cookie", TEXT, 26, True, "lm")
    node(d, 1000, 495, 820, 54, ans[3].replace("Set-Cookie: ", ""), GREEN, 22)
    node(d, 1000, 560, 820, 54, ans[4], GREEN, 22)
    header(d, 100, 660, "THE NEXT REQUESTS (the cookie)", GREEN)
    for i, parts in enumerate(nxt):
        y = 730 + i * 70
        how = " ".join(parts[:2])
        sent = parts[2].split("  ")[0]
        label(d, 100, y + 29, how, CLIENT, 28, True, "lm")
        node(d, 340, y, 260, 58, sent, GREEN, 26)
    caption(d, "Plain HTTP gets only a redirect. HSTS and the Secure cookie come over HTTPS.", AMBER, 900)


def reload(img, d, cfg):
    """The file and the served certificate, A before and B after, with no restart."""
    prop = rows("THREE", "THE CONFIGURATION")[0]
    before = re.findall(r"on disk\s+(\w)\s+served\s+(\w)", section("THREE", "BEFORE"))[0]
    after_part = section("THREE", "AFTER")
    after = re.findall(r"on disk\s+(\w)\s+served\s+(\w)", after_part)[0]
    renewed = re.search(r"renewed: (.+)", after_part).group(1)
    client = re.search(r"trusts the root: (.+)", after_part).group(1)
    header(d, 100, 205, "THE CONFIGURATION", GREEN)
    d.text((540, 228), prop, font=font(MONO_B, 26), fill=GREEN, anchor="lm")
    for x, title, (disk, served), c in ((100, "BEFORE", before, GREEN), (1260, "AFTER", after, ROSE)):
        header(d, x, 300, title, c)
        for i, (k, v) in enumerate((("shop.p12 on disk", disk), ("served", served))):
            y = 370 + i * 120
            label(d, x, y + 50, k, TEXT, 26, True, "lm")
            node(d, x + 280, y, 100, 100, v, c, 56, False)
    big_box(d, 720, 380, 480, 180, "replace the file", "no restart", AMBER)
    draw.arrow_right(d, 600, 470, 715, AMBER, 4)
    draw.arrow_right(d, 1205, 470, 1255, AMBER, 4)
    label(d, 960, 610, "A and B: SHA-256 fingerprints, shown as letters", MUTED, 24, False, "ma")
    node(d, 100, 680, 1720, 58, "renewed: " + renewed, GREEN, 24)
    node(d, 100, 755, 1720, 58, "a client that trusts the root: " + client, GREEN, 24)
    caption(d, "Same name, same root: clients notice nothing, and nobody restarts the shop.", AMBER, 880)


def mutual_tls(img, d, cfg):
    """Two settings, three clients and their certificates, and who gets in."""
    props = rows("FOUR", "THE CONFIGURATION")
    clients = [(ln[:10].strip(), ln[10:].strip()) for ln in rows("FOUR", "THE CLIENTS")]
    res = {ln[:10].strip(): ln[10:].strip() for ln in rows("FOUR", "THE RESULTS")}
    header(d, 100, 205, "THE CONFIGURATION", GREEN)
    for i, p in enumerate(props):
        d.text((540, 228 + i * 42), p, font=font(MONO_B, 22), fill=GREEN, anchor="lm")
    header(d, 100, 330, "THE CLIENTS (the certificate each shows)", GREEN)
    header(d, 1240, 330, "THE RESULTS", ROSE)
    for i, (who, cert) in enumerate(clients):
        y = 400 + i * 120
        node(d, 100, y, 180, 90, who, CLIENT, 30, False)
        node(d, 300, y, 900, 90, cert, TEXT, 24, False)
        r = res[who]
        ok = not r.startswith("refused")
        node(d, 1240, y, 580, 90, r, AMBER if ok else GREEN, 22 if ok else 30, False)
    caption(d, "The server checks the client's certificate the way a client checks the server's.", AMBER, 790)
    label(d, W // 2, 850, "It names a machine or an app, not a person: it suits service-to-service calls.",
          MUTED, 28, False, "ma")


def left_to_you(img, d, cfg):
    """Three probes and their answers, and the bill."""
    reqs = [ln.split(None, 2) for ln in rows("FIVE", "THE REQUESTS")]
    resp = {ln.split(None, 1)[0]: ln.split(None, 1)[1].strip() for ln in rows("FIVE", "THE RESPONSES")}
    bill = [b.strip() for b in rows("FIVE", "THE BILL")]
    header(d, 100, 205, "THE REQUESTS", GREEN)
    header(d, 1060, 205, "THE RESPONSES", ROSE)
    for i, (_, path, why) in enumerate(reqs):
        y = 275 + i * 110
        node(d, 100, y, 400, 80, path, CLIENT, 26)
        label(d, 520, y + 40, why, MUTED, 22, False, "lm")
        status, _, note = resp[path].partition("  ")
        node(d, 1060, y, 120, 80, status, GREEN, 30)
        label(d, 1200, y + 40, note, TEXT, 24, True, "lm")
    header(d, 100, 640, "THE BILL", AMBER)
    for i, b in enumerate(bill):
        label(d, 130, 720 + i * 60, "•  " + b, AMBER, 30, True, "lm")
    caption(d, "The framework did its part; checking from outside is still yours.", AMBER, 890)
