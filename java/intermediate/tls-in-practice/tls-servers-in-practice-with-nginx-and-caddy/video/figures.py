"""Pictures for the TLS servers video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the integration test checks, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (AMBER, CYAN, GREEN, LINE, MONO_B, MUTED, PANEL, ROSE, SANS, SANS_B, TEXT, W,
                                   font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading."""
    body = re.search(r"^%s\. .*\n((?:(?![A-Z]+\. ).*\n?)*)" % name, OUT, re.M).group(1)
    return body.splitlines()


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


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def pairs(lines):
    """Lines of two columns separated by two or more spaces, as (left, right)."""
    out = []
    for ln in lines:
        parts = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        out.append((parts[0], parts[1] if len(parts) > 1 else ""))
    return out


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def fit_mono(d, x, y, text, colour, size, max_w, anchor="la"):
    f = font(MONO_B, size)
    while f.getlength(text) > max_w and f.size > 18:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


def code_block(d, x, y, w, lines, colour=CYAN, size=26, step=40, text=TEXT):
    panel(d, x, y, w, 30 + step * len(lines), colour, 3)
    for j, ln in enumerate(lines):
        fit_mono(d, x + 22, y + 15 + j * step, ln, text, size, w - 40)
    return y + 30 + step * len(lines)


def result_colour(text):
    return ROSE if text.startswith("refused") or "(none)" in text or text == "none" else GREEN


# ------------------------------------------------------------------ figures

def two_servers(img, d, cfg):
    """The same five choices on nginx (a line you write each) and on Caddy (what it decided), all from the output."""
    nginx_lines = sections("ONE", "THE NGINX LINES")[0]
    protocols = sections("TWO", "BEFORE THE CHANGE")[0][0]
    tokens = heading("ONE", "AFTER THE FIX").split("(", 1)[1].rstrip(")")
    hsts = sections("THREE", "THE LINE ADDED")[0][0]
    n_redirect = sections("ONE", "THE RESPONSE")[0][0].split()[0]
    caddy = dict(pairs(sections("FOUR", "THE REQUESTS AND RESPONSES")[0]))
    lifetime = sections("FOUR", "THE LIFETIME")[0][0].split("= ")[-1]
    c_default = dict(l.split(": ", 1) for l in sections("FOUR", "BEFORE THE FIX")[0])
    rows = [("certificate", nginx_lines[1].split()[0] + " ...", "made by itself, " + lifetime),
            ("protocol versions", protocols, "TLS 1.2 client: " + caddy["offers only TLS 1.2"]),
            ("http to https", "return 301 https://...", "plain http: " + caddy["plain http GET /cart"]),
            ("server version", tokens, "Server: " + c_default["Server"]),
            ("HSTS", hsts, "Strict-Transport-Security: " + c_default["Strict-Transport-Security"])]
    header(d, 380, 205, "NGINX: A LINE YOU WRITE", CYAN, 700)
    header(d, 1120, 205, "CADDY: WHAT IT DECIDED", AMBER, 720)
    for i, (what, n, c) in enumerate(rows):
        y = 275 + i * 122
        label(d, 60, y + 46, what, TEXT, 30, True, "lm")
        cell(d, 380, y, 700, 92, n, CYAN, 30)
        cell(d, 1120, y, 720, 92, c, ROSE if "(none)" in c else AMBER, 28)
    caption(d, "Same shop, same choices: written by hand, or made by default.", AMBER, 905)


def chain_check(img, d, cfg):
    """The three clients: what each trusts and asks, the two checks done by hand, and the real response."""
    cert = dict(pairs(sections("ONE", "THE CERTIFICATE")[0]))
    reqs = [re.split(r"\s{2,}", l.strip()) for l in sections("ONE", "THE REQUESTS")[0]]
    checks = [re.split(r"\s{2,}", l.strip(), maxsplit=1)[1] for l in sections("ONE", "THE CHECKS")[0]]
    resp = [re.split(r"\s{2,}", l.strip(), maxsplit=1)[1] for l in sections("ONE", "THE RESPONSES")[0]]
    header(d, 60, 205, "THE CERTIFICATE NGINX SENDS", CYAN)
    panel(d, 60, 265, 580, 280, CYAN, 3)
    for j, k in enumerate(("subject", "names", "issuer", "lifetime")):
        label(d, 85, 290 + j * 62, k, MUTED, 26, True)
        fit_mono(d, 225, 290 + j * 62, cert[k], TEXT, 28, 400)
    for j, t in enumerate(("Check 1: is the issuer one of", "my roots, and does its", "signature verify?", "",
                           "Check 2: is the name I asked", "for in the certificate?")):
        label(d, 60, 580 + j * 40, t, AMBER if t.startswith("Check") else MUTED, 26, t.startswith("Check"))
    header(d, 680, 205, "THE REQUEST", AMBER, 540)
    header(d, 1240, 205, "BY HAND  ->  THE RESPONSE", GREEN, 620)
    for i, ((_, trusts, name), check, r) in enumerate(zip(reqs, checks, resp)):
        y = 270 + i * 210
        c = result_colour(r)
        panel(d, 680, y, 540, 190, AMBER)
        label(d, 700, y + 30, "trusts: " + trusts, TEXT, 28, True)
        fit_mono(d, 700, y + 100, "asks:   " + name, AMBER, 28, 500)
        panel(d, 1240, y, 620, 190, c, 3)
        for j, part in enumerate(check.split("; ")):
            ok = part.endswith("yes")
            fit_mono(d, 1260, y + 18 + j * 44, part, GREEN if ok else ROSE, 24, 580)
        fit_mono(d, 1260, y + 128, r, c, 28, 580)
    caption(d, "Both checks must pass; the client hangs up before any page is sent.", AMBER, 915)


def protocol_overlap(img, d, cfg):
    """Server versions against client offers: the highest version in both, before and after the one-line change."""
    clients = pairs(sections("TWO", "THE CLIENTS")[0])
    blocks = [(sections("TWO", "BEFORE THE CHANGE")[0][0], "BEFORE THE CHANGE"),
              (sections("TWO", "AFTER THE CHANGE")[0][0], "AFTER THE CHANGE")]
    agreements = sections("TWO", "THE AGREEMENT")
    responses = sections("TWO", "THE RESPONSES")
    for b, ((line, title), agree, resp) in enumerate(zip(blocks, agreements, responses)):
        x = 60 + b * 930
        header(d, x, 205, title, CYAN if b == 0 else AMBER)
        cell(d, x, 265, 870, 66, line, CYAN if b == 0 else AMBER, 28)
        server = re.search(r"\[(.*?)\]", agree[0]).group(1)
        label(d, x, 360, "the server speaks: " + server, TEXT, 28, True)
        for i, ((who, offer), a, r) in enumerate(zip(clients, agree, resp)):
            y = 420 + i * 220
            got = a.split("-> ")[1]
            rr = re.split(r"\s{2,}", r.strip(), maxsplit=1)[1]
            c = result_colour(got)
            panel(d, x, y, 870, 200, c, 3)
            label(d, x + 25, y + 22, who + " offers " + offer, TEXT, 30, True)
            label(d, x + 25, y + 80, "in common, highest first:", MUTED, 28)
            fit_mono(d, x + 440, y + 78, got, c, 32, 400)
            fit_mono(d, x + 25, y + 140, rr, c, 28, 820)
    caption(d, "One line turns the old client away: the price of TLS 1.3 only.", AMBER, 900)


def header_trap(img, d, cfg):
    """Where each add_header line sits, which one nginx applies to /account, and what the response carries."""
    line = sections("THREE", "THE LINE ADDED")[0]
    loc = sections("THREE", "THE TRAP")[0]
    rule = dict(pairs(sections("THREE", "THE RULE")[0]))
    resp = sections("THREE", "THE RESPONSE FROM")[0]
    fixed = sections("THREE", "AFTER THE FIX (the line")[0]
    header(d, 60, 205, "THE SERVER BLOCK", CYAN)
    y = code_block(d, 60, 265, 900, ["server {"] + ["    " + l for l in line] + ["    ..."], CYAN, 26, 48)
    header(d, 60, y + 30, "INSIDE IT: THE /account LOCATION", AMBER)
    code_block(d, 60, y + 90, 900, ["    " + l for l in loc], AMBER, 26, 48)
    header(d, 1010, 205, "WHICH LINES APPLY TO /account", GREEN)
    for i, k in enumerate(("server", "location", "applied")):
        yy = 270 + i * 90
        label(d, 1010, yy + 36, k, MUTED, 28, True, "lm")
        cell(d, 1180, yy, 680, 72, rule[k], GREEN if k == "applied" else LINE, 28, text_colour=TEXT if k != "applied" else GREEN)
    header(d, 1010, 550, "THE RESPONSE", ROSE)
    for i, l in enumerate(resp):
        fit_mono(d, 1010, 615 + i * 46, l.split("  <-")[0], ROSE if "(none)" in l else TEXT, 26, 850)
    header(d, 1010, 725, "AFTER THE FIX (the line repeated)", GREEN)
    for i, l in enumerate(fixed):
        fit_mono(d, 1010, 790 + i * 46, l, GREEN, 26, 840)
    caption(d, "A location with its own add_header drops every server-level one.", AMBER, 905)


def hsts_year(img, d, cfg):
    """What the HSTS value means: the max-age worked out in days, and over which scheme it was sent."""
    after = pairs(sections("THREE", "AFTER THE FIX (Strict")[0])
    work = sections("THREE", "THE max-age")[0]
    header(d, 60, 205, "THE RESPONSE HEADER Strict-Transport-Security", CYAN)
    for i, (how, value) in enumerate(after):
        y = 270 + i * 95
        label(d, 60, y + 35, how, TEXT, 28, True, "lm")
        cell(d, 420, y, 1440, 70, value, result_colour(value) if "(none)" in value else GREEN, 28)
    header(d, 60, 480, "THE max-age, WORKED OUT", AMBER)
    m = re.match(r"(\d+) s / (\d+) s a day = (\d+) days", work[0])
    secs, day, days = m.groups()
    for j, (v, note) in enumerate(((secs, "seconds"), (day, "seconds a day"), (days, "days"))):
        x = 60 + j * 600
        cell(d, x, 545, 420, 90, v, AMBER, 40)
        label(d, x + 210, 650, note, MUTED, 26, True, "ma")
        if j < 2:
            label(d, x + 510, 590, "/" if j == 0 else "=", TEXT, 48, True, "mm")
    fit_mono(d, 60, 720, work[1], TEXT, 26, 1800)
    caption(d, "For a year, a browser that saw it uses only https for this name.", AMBER, 860)


def caddy_chain(img, d, cfg):
    """The Caddyfile on the left; the chain Caddy made, its lifetime and the redirect on the right."""
    caddyfile = sections("FOUR", "THE CADDYFILE")[0]
    chain = [l.strip() for l in sections("FOUR", "THE CHAIN")[0]]
    life = sections("FOUR", "THE LIFETIME")[0][0]
    reqs = sections("FOUR", "THE REQUESTS AND RESPONSES")[0]
    header(d, 60, 205, "THE CADDYFILE (the site block)", AMBER)
    code_block(d, 60, 265, 640, caddyfile, AMBER, 30, 50)
    header(d, 780, 205, "THE CHAIN CADDY MADE BY ITSELF", CYAN)
    for i, ln in enumerate(chain):
        name, where = ln.replace("signed by ", "").rsplit("  (", 1)
        y = 265 + i * 120
        c = GREEN if i == 0 else CYAN
        cell(d, 780, y, 860, 80, name, c, 28)
        label(d, 1665, y + 40, "kept" if where.startswith("root") else "sent", MUTED, 28, True, "lm")
        if i:
            draw.arrow_up(d, 1210, y, y - 40, MUTED, 4)
    header(d, 60, 600, "THE LIFETIME, WORKED OUT", GREEN)
    fit_mono(d, 60, 670, life.split(" = ")[0] + " =", GREEN, 30, 640)
    fit_mono(d, 60, 720, life.split(" = ")[1], GREEN, 40, 640)
    header(d, 780, 625, "THE REQUESTS AND RESPONSES", AMBER)
    for i, (who, r) in enumerate(pairs(reqs[:4])):
        y = 685 + i * 46
        label(d, 780, y, who, TEXT, 26, True)
        fit_mono(d, 1120, y, r, result_colour(r), 26, 740)
    fit_mono(d, 1120, 869, reqs[4].strip(), MUTED, 24, 740)
    caption(d, "A short site block, and a whole local authority behind it.", AMBER, 930)


def renewal_reload(img, d, cfg):
    """Stapling needs a responder in the certificate; a new certificate on disk waits for a reload."""
    certs = pairs(sections("FIVE", "THE CERTIFICATES")[0])
    stapled = pairs(sections("FIVE", "THE RESULT")[0])
    log = sections("FIVE", "NGINX'S ERROR LOG")[0][0]
    renew = pairs(sections("FIVE", "THE RENEWAL")[0])
    after = pairs(sections("FIVE", "AFTER nginx")[0])
    header(d, 60, 205, "STAPLING: A RESPONDER NAMED?  ->  ANSWERS STAPLED", CYAN)
    for i, ((who, named), (_, got)) in enumerate(zip(certs, stapled)):
        x = 60 + i * 930
        label(d, x, 300, who, TEXT, 30, True, "lm")
        cell(d, x + 180, 265, 220, 72, named, ROSE, 30)
        draw.arrow_right(d, x + 420, 301, x + 500, MUTED, 4)
        cell(d, x + 520, 265, 220, 72, got.split("  <-")[0], ROSE, 30)
    label(d, 60, 375, "nginx's error log:", MUTED, 26, True)
    fit_mono(d, 330, 375, log, AMBER, 26, 1530)
    header(d, 60, 480, heading("FIVE", "THE RENEWAL").split(" (")[0].replace("THE ", "THE CERTIFICATE "), AMBER)
    cols = [("BEFORE THE RELOAD", renew, AMBER), ("AFTER nginx -s reload", [renew[0]] + after, GREEN)]
    for b, (title, rows, c) in enumerate(cols):
        x = 60 + b * 930
        header(d, x, 545, title, c)
        for i, (k, v) in enumerate(rows):
            y = 615 + i * 105
            v = v.split("  <-")[0]
            label(d, x, y + 40, k, TEXT, 30, True, "lm")
            cell(d, x + 300, y, 500, 80, v, GREEN if v == "<new serial>" else ROSE, 30)
    caption(d, "nginx reads the certificate file only when it starts or reloads.", AMBER, 905)
