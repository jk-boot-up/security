"""Pictures for the HTTPS server with nginx video, drawn by the `figure` slide kind.

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

def chain(img, d, cfg):
    """The demo root signs the shop's and Ana's certificates; the impostor is signed by nobody trusted."""
    host = re.search(r"GET https://([^/]+)/", OUT).group(1)
    ana = re.search(r"client=(CN=[^\n]+)", section("FOUR", "THE RESULTS (what")).group(1)
    big_box(d, 760, 220, 400, 130, "demo root (a CA)", "in the client's trust store", AMBER)
    big_box(d, 200, 470, 560, 130, "CN=" + host, "the shop's certificate, on nginx", GREEN)
    big_box(d, 1160, 470, 560, 130, ana, "Ana's certificate (mutual TLS)", GREEN)
    draw.arrow(d, 860, 355, 520, 465, AMBER, 4)
    draw.arrow(d, 1060, 355, 1400, 465, AMBER, 4)
    label(d, 600, 390, "signs", AMBER, 26, True, "ma")
    label(d, 1320, 390, "signs", AMBER, 26, True, "ma")
    big_box(d, 200, 690, 560, 120, "CN=" + host, "the impostor: signed by nobody trusted", ROSE)
    label(d, 800, 750, "← same name, no trusted signature: refused", ROSE, 26, True, "lm")
    caption(d, "Trust follows signatures up to a root the client already holds.", AMBER, 880)


def termination(img, d, cfg):
    """Client to nginx over TLS, nginx to the app over plain HTTP, and the response."""
    conf = rows("ONE", "THE CONFIGURATION")
    req = rows("ONE", "THE REQUEST")[0]
    resp = rows("ONE", "THE RESPONSE")
    app = conf[-1].split()[1].rstrip(";")
    proto = re.search(r"over (TLSv[\d.]+)", resp[0]).group(1)
    big_box(d, 100, 220, 380, 130, "client", "trusts the demo root", CLIENT)
    big_box(d, 770, 220, 380, 130, "nginx", "ends the TLS", CYAN)
    big_box(d, 1440, 220, 380, 130, "the app", app, CLIENT)
    draw.arrow_right(d, 485, 285, 765, GREEN, 5)
    label(d, 625, 235, "HTTPS, " + proto, GREEN, 24, True, "ma")
    draw.arrow_right(d, 1155, 285, 1435, MUTED, 5)
    label(d, 1295, 235, "plain HTTP, local", MUTED, 24, True, "ma")
    label(d, 1295, 305, "+ X-Forwarded-Proto", AMBER, 22, True, "ma")
    header(d, 100, 400, "THE CONFIGURATION", GREEN)
    for i, c in enumerate(conf):
        d.text((100 + (i % 2) * 560, 485 + (i // 2) * 44), c, font=font(MONO_B, 24), fill=GREEN, anchor="lm")
    header(d, 1220, 400, "THE REQUEST", GREEN)
    node(d, 1220, 460, 600, 56, req, GREEN, 22)
    header(d, 100, 640, "THE RESPONSE", ROSE)
    for i, r in enumerate(resp):
        node(d, 100, 700 + i * 62, 1720, 52, r, ROSE if i == 0 else AMBER if i == 1 else TEXT, 24)
    caption(d, "The certificate and its key live on nginx; the app only ever speaks plain HTTP.", AMBER, 905)


def versions(img, d, cfg):
    """Two servers, one cipher name taken apart, and four connections."""
    conf = rows("TWO", "THE CONFIGURATIONS")
    servers = [(c[:9].strip(), c[9:].strip()) for c in conf[:2]]
    ciphers = [c.strip() for c in conf[3:]]
    res = [re.match(r"(\w+)\s+(TLS [\d.]+) client\s+(\S+)", r).groups() for r in rows("TWO", "THE RESULTS")]
    header(d, 100, 205, "THE CONFIGURATIONS", GREEN)
    for i, (k, v) in enumerate(servers):
        label(d, 100, 285 + i * 50, k, CLIENT, 26, True, "lm")
        d.text((240, 285 + i * 50), v, font=font(MONO_B, 26), fill=GREEN, anchor="lm")
    label(d, 100, 390, "compat's ciphers:", MUTED, 22, False, "lm")
    for i, c in enumerate(ciphers):
        d.text((330, 390 + i * 34), c, font=font(MONO_B, 22), fill=TEXT, anchor="lm")
    parts = ciphers[0].split("-")
    meaning = {"ECDHE": "a fresh shared key", "ECDSA": "signed with the certificate's key",
               "AES128": "encryption, 128-bit key", "GCM": "detects changes", "SHA256": "the hash"}
    header(d, 1000, 205, "ONE CIPHER NAME, PART BY PART", CYAN)
    for i, p in enumerate(parts):
        y = 270 + i * 52
        node(d, 1000, y, 200, 44, p, CYAN, 24)
        label(d, 1220, y + 22, meaning.get(p, ""), TEXT, 24, False, "lm")
    header(d, 100, 560, "THE RESULTS", ROSE)
    label(d, 960, 580, "TLS 1.2 client", CLIENT, 26, True, "ma")
    label(d, 1500, 580, "TLS 1.3 client", CLIENT, 26, True, "ma")
    for i, server in enumerate(("strict", "compat")):
        y = 630 + i * 90
        label(d, 100, y + 32, server, CLIENT, 28, True, "lm")
        for s, client, got in res:
            if s == server:
                x = 760 if client == "TLS 1.2" else 1300
                node(d, x, y, 400, 64, got, GREEN if got.startswith("TLS") else ROSE, 26)
    caption(d, "Allow TLS 1.2 only while customers need it, and keep the cipher list short.", AMBER, 900)


def redirect(img, d, cfg):
    """The plain request, the 301, and the HSTS max-age worked out."""
    req = rows("THREE", "THE REQUEST")[0]
    resp = [r.strip() for r in rows("THREE", "THE RESPONSE")]
    hsts = rows("THREE", "THE HTTPS ANSWER")[1].strip()
    days = re.search(r"(\d+) / (\d+) seconds a day = (\d+) days", section("THREE")).groups()
    header(d, 100, 205, "THE REQUEST (plain port)", GREEN)
    node(d, 100, 270, 900, 60, req, GREEN, 26)
    header(d, 100, 370, "THE RESPONSE", ROSE)
    status, _, loc = resp[0].partition("  ")
    node(d, 100, 435, 120, 60, status, ROSE, 30)
    node(d, 240, 435, 760, 60, loc, ROSE, 22)
    node(d, 100, 515, 900, 56, resp[1], MUTED, 22)
    header(d, 1080, 205, "THE HTTPS ANSWER", ROSE)
    label(d, 1080, 290, "Strict-Transport-Security", TEXT, 26, True, "lm")
    node(d, 1080, 320, 740, 56, hsts, GREEN, 24)
    label(d, 1080, 420, "max-age is in seconds:", MUTED, 24, False, "lm")
    node(d, 1080, 450, 740, 60, "%s / %s" % days[:2], AMBER, 28)
    node(d, 1080, 525, 740, 60, "= %s days (two years)" % days[2], AMBER, 28, False)
    caption(d, "The first visit is still plain: HSTS protects the visits after it.", AMBER, 700)
    label(d, W // 2, 770, "Preloading the site into browsers protects the first one too.", MUTED, 30, False, "ma")


def trust(img, d, cfg):
    """Four connections where the client checks the server, three where the server checks the client."""
    att = [ln.split(None, 1) for ln in rows("FOUR", "THE ATTEMPTS")]
    res1 = {ln.split(None, 1)[0]: ln.split(None, 1)[1] for ln in rows("FOUR", "THE RESULTS:".rstrip(":"))}
    clients = [ln.split(None, 1) for ln in rows("FOUR", "THE CLIENTS")]
    res2 = {ln.split(None, 1)[0]: ln.split(None, 1)[1] for ln in rows("FOUR", "THE RESULTS (what")}
    note = re.search(r"^  (nginx sets .+)$", section("FOUR"), re.M).group(1)
    header(d, 100, 205, "THE CLIENT CHECKS THE SERVER", GREEN)
    header(d, 1000, 205, "THE SERVER CHECKS THE CLIENT", GREEN)
    for i, (k, what) in enumerate(att):
        y = 275 + i * 140
        node(d, 100, y, 56, 56, k, CLIENT, 26)
        node(d, 170, y, 760, 56, what, TEXT, 20, False)
        r = res1[k]
        verdict, _, why = r.partition("   ")
        node(d, 170, y + 64, 200, 48, verdict, AMBER if verdict == "accepted" else GREEN, 24, False)
        label(d, 390, y + 88, why, MUTED, 20, False, "lm")
    for i, (k, what) in enumerate(clients):
        y = 275 + i * 170
        node(d, 1000, y, 56, 56, k, CLIENT, 26)
        node(d, 1070, y, 750, 56, what, TEXT, 20, False)
        r = res2[k]
        node(d, 1070, y + 66, 750, 56, r, AMBER if r.startswith("200") else GREEN, 22, False)
    label(d, 1000, 790, note, AMBER, 24, True, "lm")
    caption(d, "A name, a trusted signer, and, for mutual TLS, a certificate from the client.", AMBER, 900)


def proxy_trap(img, d, cfg):
    """The forged header and what the app was told, nginx -t, and the seven habits."""
    req = rows("FIVE", "THE REQUEST")[0]
    resp = [(r[:18].strip(), r[18:].strip()) for r in rows("FIVE", "THE RESPONSES")]
    checks = [(r[:30].strip(), r[30:].strip()) for r in rows("FIVE", "THE CONFIG CHECKS")]
    habits = [(r[:26].strip(), r[26:35].strip(), r[35:].strip()) for r in rows("FIVE", "THE SCORE")[1:8]]
    sums = dict(re.findall(r"^    (careful|copied)\s+(.+)$", section("FIVE", "THE SCORE"), re.M))
    header(d, 100, 205, "THE REQUEST (forged header)", GREEN)
    node(d, 100, 265, 800, 56, req, GREEN, 24)
    header(d, 100, 350, "THE RESPONSES (what the app was told)", ROSE)
    for i, (how, told) in enumerate(resp):
        y = 415 + i * 70
        label(d, 100, y + 28, how, TEXT, 24, True, "lm")
        fooled = "FOOLED" in told
        node(d, 480, y, 420, 56, told.split("  <-")[0], ROSE if fooled else GREEN, 22)
    header(d, 100, 580, "THE CONFIG CHECKS (nginx -t)", GREEN)
    for i, (what, got) in enumerate(checks):
        y = 645 + i * 66
        label(d, 100, y + 26, what, TEXT, 24, True, "lm")
        node(d, 600, y, 300, 52, got, GREEN, 24, False)
    header(d, 1000, 205, "THE SCORE (seven TLS habits)", AMBER)
    label(d, 1600, 290, "careful", CLIENT, 22, True, "ma")
    label(d, 1740, 290, "copied", CLIENT, 22, True, "ma")
    for i, (name, a, b) in enumerate(habits):
        y = 330 + i * 54
        label(d, 1000, y + 20, name, TEXT, 24, False, "lm")
        for x, v in ((1600, a), (1740, b)):
            d.text((x, y + 20), "✓" if v == "yes" else "✗", font=font(SANS_B, 36),
                   fill=GREEN if v == "yes" else ROSE, anchor="mm")
    node(d, 1000, 720, 820, 50, "careful  " + sums["careful"], AMBER, 22)
    node(d, 1000, 780, 820, 50, "copied   " + sums["copied"], AMBER, 22)
    caption(d, "Set forwarded headers from what nginx saw, test with nginx -t, keep the habits.", AMBER, 900)
