"""Pictures for the path traversal, file upload and open redirect video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import posixpath
import re
from pathlib import Path
from urllib.parse import urlsplit

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
    """Every section of an act whose header starts with `head`, as lists of lines (indent removed)."""
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


def header(d, x, y, text, colour, w=None, size=26):
    f = font(SANS_B, size)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def mono(d, x, y, text, colour=TEXT, size=26, anchor="la"):
    d.text((x, y), text, font=font(MONO_B, size), fill=colour, anchor=anchor)


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


def elements(d, x, y, parts, colours, size=26, h=60, gap=14):
    """A path drawn as one cell per element; returns the x of each cell's centre."""
    f = font(MONO_B, size)
    centres = []
    for p, c in zip(parts, colours):
        w = int(f.getlength(p)) + 40
        cell(d, x, y, w, h, p, c, size)
        centres.append(x + w // 2)
        x += w + gap
    return centres


def strike(d, x1, x2, y, colour=ROSE):
    d.line([x1, y, x2, y], fill=colour, width=5)


def folders():
    """The folders of act ONE, as (folder, [files])."""
    out = []
    for ln in sections("ONE", "THE FOLDERS")[0]:
        name, files = re.split(r"\s{2,}", ln.strip(), 1)
        out.append((name, [] if files == "(empty)" else files.split("  ")))
    return out


# ------------------------------------------------------------------ figures

def climb_out(img, d, cfg):
    """The server's folders as a tree, and the walk a name with .. takes out of the invoices folder."""
    tree = folders()
    request = sections("TWO", "THE REQUEST")[0][0].strip()
    joined = sections("TWO", "THE PATH, JOINED")[0][0].strip()
    opened = sections("TWO", "THE PATH THE DISK")[0][0].strip()
    target_dir = opened.split("/")[1]
    header(d, 60, 190, "THE SERVER'S FOLDERS", CYAN)
    cell(d, 60, 260, 200, 60, ".../", MUTED, 28)
    ys = {}
    for i, (name, files) in enumerate(tree):
        y = 340 + i * 90
        top = name.split("/")[0]
        ys.setdefault(top, y)
        d.line([120, 320, 120, y + 30], fill=LINE, width=3)
        d.line([120, y + 30, 160, y + 30], fill=LINE, width=3)
        c = GREEN if top == "invoices" else ROSE if top == target_dir else LINE
        cell(d, 160, y, 340, 60, name, c if c != LINE else TEXT, 26, text_colour=TEXT if c == LINE else c)
        fit_mono(d, 520, y + 30, files[0] if files else "(empty)", MUTED, 24, 380, "lm")
    yi, ys_ = ys["invoices"] + 30, ys[target_dir] + 30
    # the walk: up from invoices to .../, then down into the target folder
    draw.arrow(d, 150, yi - 20, 75, 330, AMBER, 5)
    draw.arrow(d, 75, 330, 150, ys_ - 20, AMBER, 5)
    label(d, 92, (330 + yi) // 2 - 10, "..", AMBER, 34, True, "lm")
    header(d, 960, 190, "THE REQUEST", AMBER)
    cell(d, 960, 250, 880, 64, request, AMBER, 30)
    header(d, 960, 350, "THE PATH, JOINED", CYAN)
    parts = joined.split("/")
    cs = elements(d, 960, 410, parts, [ROSE if p == ".." or (k + 1 < len(parts) and parts[k + 1] == "..") else TEXT
                                        for k, p in enumerate(parts)], 26)
    k = parts.index("..")
    strike(d, cs[k - 1] - 60, cs[k] + 30, 440)
    label(d, 960, 490, '".." cancels the folder before it', ROSE, 26, True)
    header(d, 960, 560, "THE PATH THE DISK OPENS", ROSE)
    elements(d, 960, 620, opened.split("/"), [MUTED] + [ROSE] * (len(opened.split("/")) - 1), 26)
    res = sections("TWO", "THE RESPONSE")[0][0].strip().split("  <-")[0]
    header(d, 960, 730, "THE RESPONSE", ROSE)
    fit_mono(d, 960, 795, res, ROSE, 28, 880)
    caption(d, "Two dots go up one folder; the name walks out of invoices.", AMBER, 930)


def folder_tree(img, d, cfg):
    """Act one: the honest request, the path it becomes, and the file it reads."""
    tree = folders()
    request = sections("ONE", "THE REQUEST")[0][0].strip()
    path = sections("ONE", "THE PATH")[0][0].strip()
    res = sections("ONE", "THE RESPONSE")[0][0].strip()
    header(d, 60, 190, "THE FOLDERS (under .../)", CYAN)
    for i, (name, files) in enumerate(tree):
        y = 260 + i * 92
        ok = name.startswith("invoices/")
        panel(d, 60, y, 860, 74, GREEN if ok else LINE, 3 if ok else 2)
        mono(d, 85, y + 37, name, GREEN if ok else TEXT, 26, "lm")
        fit_mono(d, 400, y + 37, "  ".join(files) or "(empty)", TEXT if ok else MUTED, 24, 500, "lm")
    header(d, 1000, 190, "THE REQUEST", AMBER)
    cell(d, 1000, 250, 840, 64, request, AMBER, 30)
    header(d, 1000, 360, "THE PATH (folder + name)", CYAN)
    folder, name = path.rsplit("/", 1)
    panel(d, 1000, 420, 840, 70, CYAN)
    f = font(MONO_B, 28)
    mono(d, 1025, 455, folder + "/", TEXT, 28, "lm")
    mono(d, 1025 + f.getlength(folder + "/"), 455, name, AMBER, 28, "lm")
    header(d, 1000, 540, "THE RESPONSE", GREEN)
    fit_mono(d, 1000, 610, res, GREEN, 28, 840)
    label(d, 1000, 680, "The folder is the server's text;", MUTED, 26)
    label(d, 1000, 720, "the name is the customer's text.", AMBER, 26)
    caption(d, "Whatever the name says, the server joins it to the folder and reads it.", AMBER, 930)


def dotdot_steps(img, d, cfg):
    """Act two: both requests, their joined paths element by element, and the path the disk really opens."""
    reqs = [s[0].strip() for s in sections("TWO", "THE REQUEST")]
    joins = [s[0].strip() for s in sections("TWO", "THE PATH, JOINED")]
    opens = [s[0].strip() for s in sections("TWO", "THE PATH THE DISK")]
    ress = [s[0].strip() for s in sections("TWO", "THE RESPONSE")]
    for i, (req, joined, opened, res) in enumerate(zip(reqs, joins, opens, ress)):
        y = 190 + i * 370
        x = header(d, 60, y, "THE REQUEST", AMBER)
        mono(d, x + 30, y + 22, req, AMBER, 30, "lm")
        parts = joined.split("/")
        k = parts.index("..")
        label(d, 60, y + 95, "joined", MUTED, 26, True, "lm")
        cs = elements(d, 220, y + 65, parts, [ROSE if j in (k - 1, k) else TEXT for j in range(len(parts))], 26)
        strike(d, cs[k - 1] - 70, cs[k] + 25, y + 95)
        label(d, 60, y + 185, "opens", MUTED, 26, True, "lm")
        elements(d, 220, y + 155, opened.split("/"), [MUTED] + [ROSE] * (len(opened.split("/")) - 1), 26)
        text, mark = (res.split("  <- ") + [""])[:2]
        x = header(d, 60, y + 240, "THE RESPONSE", ROSE)
        fit_mono(d, x + 30, y + 262, text, ROSE, 28, 900, "lm")
        if mark:
            label(d, 1280, y + 262, mark.lower(), ROSE, 26, True, "lm")
    caption(d, "Nothing checks the name, so .. leaves the invoices folder.", AMBER, 940)


def text_vs_path(img, d, cfg):
    """Act three: the same pair compared letter by letter, and element by element."""
    base = re.search(r'"(.*)"', sections("THREE", "THE CHECK")[0][0]).group(1)
    path = sections("THREE", "THE PATH")[1][0].strip()
    comp = sections("THREE", "THE COMPARISON")[1]
    as_text = comp[0].split("?")[1].split("<-")[0].strip()
    as_path = comp[1].split("?")[1].strip()
    res = sections("THREE", "THE RESPONSE")[1][0].strip().split("  <-")[0]
    header(d, 60, 190, "AS TEXT: does the path start with the folder's letters?", AMBER)
    f = font(MONO_B, 40)
    x0 = 100
    n = len(base)
    for row, (text, y) in enumerate(((base, 280), (path, 350))):
        x = x0
        for j, ch in enumerate(text):
            w = f.getlength(ch)
            if j < n:
                d.rectangle([x, y - 4, x + w, y + 50], fill=(20, 70, 60))
            d.text((x, y), ch, font=f, fill=GREEN if j < n else TEXT)
            x += w
    label(d, x0 + f.getlength(path) + 40, 375, as_text, GREEN if as_text == "yes" else ROSE, 40, True, "lm")
    label(d, x0, 430, "all %d letters of the folder match, so the text check reads the file" % n, AMBER, 28, True)
    header(d, 60, 520, "AS A PATH: is each element of the folder an element of the path?", CYAN)
    bparts, pparts = base.split("/"), path.split("/")
    label(d, 100, 625, "folder", MUTED, 26, True, "lm")
    label(d, 100, 715, "path", MUTED, 26, True, "lm")
    cs = elements(d, 220, 595, bparts, [MUTED] + [GREEN] * (len(bparts) - 1), 30)
    ps = elements(d, 220, 685, pparts, [MUTED, ROSE] + [TEXT] * (len(pparts) - 2), 30)
    label(d, ps[-1] + 200, 670, "%s is not %s:  %s" % (pparts[1], bparts[1], as_path), ROSE if as_path == "no" else GREEN, 30, True, "lm")
    x = header(d, 60, 800, "THE RESPONSE (of the text check)", ROSE)
    fit_mono(d, x + 30, 822, res, ROSE, 28, 900, "lm")
    caption(d, "Path.startsWith compares whole folder names: invoices-private is not invoices.", AMBER, 920)


STOPS = {"not an invoice name": 2, "outside the invoice folder": 3,
         "link leads outside the invoice folder": 4, "no such file": 4}


def four_layers(img, d, cfg):
    """Act four: every request against the four layers; where each one stops, and why."""
    layers = [re.split(r"\s{2,}", ln.strip(), 1) for ln in sections("FOUR", "THE LAYERS")[0]]
    reqs = [sections("FOUR", "THE REQUEST")[0][0].strip()]
    decoded = {}
    for ln in sections("FOUR", "THE REQUESTS")[0]:
        if ln.strip().startswith("decoded once: "):
            decoded[reqs[-1]] = ln.split("decoded once: ")[1]
        else:
            reqs.append(ln.strip())
    link_req = sections("FOUR", "THE REQUEST (a good")[0][0].strip()
    ress = [sections("FOUR", "THE RESPONSE")[0][0].strip()] + [ln.strip() for ln in sections("FOUR", "THE RESPONSES")[0]]
    fixed = next(ln for ln in sections("FOUR", "THE RESPONSES")[1] if ln.strip().startswith("fixed"))
    reqs.append(link_req + " (a link)")
    ress.append(fixed.strip()[len("fixed"):].strip())
    xs, cw = [560, 750, 940, 1130], 175
    for j, (num, what) in enumerate(layers):
        cell(d, xs[j], 190, cw, 70, num.split(" ", 1)[0], CYAN, 28)
        label(d, xs[j] + cw // 2, 278, num.split(" ", 1)[1], CYAN, 24, True, "mm")
    label(d, 60, 225, "THE REQUEST", AMBER, 26, True, "lm")
    label(d, 1335, 225, "THE RESPONSE", AMBER, 26, True, "lm")
    for i, (req, res) in enumerate(zip(reqs, ress)):
        y = 305 + i * 96
        fit_mono(d, 60, y + (18 if req in decoded else 35), req, TEXT, 24, 480, "lm")
        if req in decoded:
            fit_mono(d, 60, y + 54, "decoded: " + decoded[req], AMBER, 24, 480, "lm")
        reason = res.split("refused: ")[1] if res.startswith("refused") else None
        stop = STOPS.get(reason, 5)
        for j in range(4):
            if j + 1 < stop:
                cell(d, xs[j], y, cw, 70, "pass", GREEN, 24)
            elif j + 1 == stop:
                cell(d, xs[j], y, cw, 70, "stop", ROSE, 24)
        head, _, tail = res.partition(": ")
        c = GREEN if res.startswith("read") and i == 0 else ROSE if res.startswith("read") else AMBER
        label(d, 1335, y + 18, head, c, 24, True, "lm")
        fit_mono(d, 1335, y + 52, tail, c, 24, 545, "lm")
    caption(d, "Each layer stops what the ones before it let through.", AMBER, 905)


def naive_upload(img, d, cfg):
    """Act five: what the customer sends, and where the naive upload puts it."""
    ups = [re.split(r"\s+", ln.strip(), 1) for ln in sections("FIVE", "THE UPLOADS")[0]]
    folder = re.search(r"saved in (.*?)\):", "\n".join(act("FIVE"))).group(1)
    resp = sections("FIVE", "THE RESPONSES")[0]
    results = [resp[k + 1].strip().lstrip("-> ").strip() for k in range(0, len(resp), 2)]
    header(d, 60, 190, "THE UPLOADS (name, first bytes)", AMBER)
    header(d, 1000, 190, "THE RESPONSES (saved in %s)" % folder, CYAN)
    for i, ((name, raw), res) in enumerate(zip(ups, results)):
        y = 270 + i * 210
        panel(d, 60, y, 860, 150, AMBER)
        fit_mono(d, 85, y + 40, name, AMBER, 28, 810, "lm")
        hexes, chars = raw.rsplit("  ", 1)
        mono(d, 85, y + 105, hexes, TEXT, 28, "lm")
        mono(d, 520, y + 105, '= "%s"' % chars, MUTED, 28, "lm")
        draw.arrow_right(d, 930, y + 75, 990, MUTED, 4)
        text, mark = (res.split("  <- ") + [""])[:2]
        if ".." in name:
            walk = posixpath.normpath(folder + name)
            fit_mono(d, 1000, y + 30, folder + name, MUTED, 24, 860, "lm")
            fit_mono(d, 1000, y + 75, "= " + walk, ROSE, 26, 860, "lm")
            fit_mono(d, 1000, y + 120, "now reads: " + text.split(": ")[1] + "  (was admin=ana)", ROSE, 26, 860, "lm")
        else:
            fit_mono(d, 1000, y + 55, text, ROSE if mark else GREEN, 28, 860, "lm")
            if mark:
                label(d, 1000, y + 105, "a page on our own site: its script runs as ours", ROSE, 26, True, "lm")
    caption(d, "The customer chose the name, the folder and the type.", AMBER, 930)


def safe_upload(img, d, cfg):
    """Act six: the rules, and each upload's size and first bytes deciding its fate."""
    rules = dict(re.split(r"\s{2,}", ln.strip(), 1) for ln in sections("SIX", "THE RULES")[0])
    limit = int(re.search(r"to (\d+)", rules["size"]).group(1))
    magic = {k: rules[k].replace("first bytes ", "") for k in ("PNG", "JPEG")}
    ups = []
    for ln in sections("SIX", "THE UPLOADS")[0]:
        m = re.match(r"(.+?)\s+([0-9a-f]{2}(?: [0-9a-f]{2}){3})  (\S{4})\s+(\d+) bytes", ln.strip())
        ups.append(m.groups())
    results = [ln.strip() for ln in sections("SIX", "THE RESPONSES")[0]]
    cols = [(60, "THE UPLOAD (name)"), (640, "FIRST BYTES"), (960, "SIZE"), (1200, "THE RESPONSE")]
    for x, t in cols:
        label(d, x, 200, t, AMBER, 24, True)
    for i, ((name, hexes, chars, size), res) in enumerate(zip(ups, results)):
        y = 245 + i * 92
        kind = next((k for k, v in magic.items() if hexes.startswith(v)), None)
        size_ok = 0 < int(size) <= limit
        fit_mono(d, 60, y + 35, name, TEXT, 24, 560, "lm")
        cell(d, 640, y, 300, 70, hexes + (" = " + kind if kind and size_ok else ""), GREEN if kind else ROSE, 22)
        cell(d, 960, y, 220, 70, size + (" ok" if size_ok else " > %d" % limit), GREEN if size_ok else ROSE, 22)
        fit_mono(d, 1200, y + 35, res, GREEN, 24, 660, "lm")
    head = sections("SIX", "THE DOWNLOAD")[0]
    title = re.search(r"THE DOWNLOAD OF (\S+)", "\n".join(act("SIX"))).group(1)
    header(d, 60, 720, "THE DOWNLOAD OF " + title, CYAN)
    panel(d, 60, 780, 900, 40 + 44 * len(head), CYAN)
    for j, ln in enumerate(head):
        k, v = ln.strip().split(": ", 1)
        mono(d, 85, 800 + j * 44, k + ": ", MUTED, 24)
        mono(d, 85 + font(MONO_B, 24).getlength(k + ": "), 800 + j * 44, v, GREEN, 24)
    place = rules["place"].split(",")[0]
    label(d, 1020, 790, "stored in " + place, TEXT, 26, True)
    label(d, 1020, 840, "outside the web root, under the server's name", MUTED, 26)
    label(d, 1020, 890, "the customer's name is never used", MUTED, 26)


def return_links(img, d, cfg):
    """Act seven: each return link, what the naive and the fixed redirect send back, and the exact host check."""
    links = [ln.strip() for ln in sections("SEVEN", "THE REQUESTS")[0]]
    naive = [ln.strip() for ln in sections("SEVEN", "THE NAIVE RESPONSES")[0]]
    fixed = [ln.strip() for ln in sections("SEVEN", "THE FIXED RESPONSES")[0]]
    for x, t in ((60, "THE REQUEST (returnTo)"), (620, "NAIVE: Location"), (1100, "FIXED: Location (why)")):
        label(d, x, 190, t, AMBER, 26, True)
    for i, (link, n, f) in enumerate(zip(links, naive, fixed)):
        y = 240 + i * 74
        mono(d, 60, y + 28, link, TEXT, 28, "lm")
        off = "<-" in n
        fit_mono(d, 620, y + 28, n.split("  <-")[0].strip(), ROSE if off else GREEN, 28, 460, "lm")
        loc, why = (f.split("  (", 1) + [""])[:2]
        mono(d, 1100, y + 14, loc, GREEN, 28, "lm")
        if why:
            fit_mono(d, 1100, y + 48, why.rstrip(")"), MUTED, 24, 760, "lm")
    hosts = sections("SEVEN", "THE HOST CHECK")[0]
    header(d, 60, 625, "THE HOST CHECK: the host must be exactly shop.example", CYAN, size=28)
    for i in range(0, len(hosts), 2):
        addr = hosts[i].strip()
        verdict = hosts[i + 1].split("-> ")[1].strip()
        parts = urlsplit(addr)
        host, user = parts.hostname, parts.username
        y = 695 + (i // 2) * 70
        f = font(MONO_B, 30)
        x = 60
        start = addr.index(host, len("https://") + (len(user) + 1 if user else 0))
        for seg, c in ((addr[:start], MUTED), (host, GREEN if verdict == "allowed" else ROSE), (addr[start + len(host):], MUTED)):
            d.text((x, y), seg, font=f, fill=c)
            x += f.getlength(seg)
        note = "host " + host + ("; %s@ is a user name" % user if user else "")
        label(d, 900, y + 18, note, TEXT, 26, False, "lm")
        label(d, 1840, y + 18, verdict, GREEN if verdict == "allowed" else ROSE, 30, True, "rm")
    caption(d, "Only a path on our own site is followed; a host is matched exactly.", AMBER, 925)
