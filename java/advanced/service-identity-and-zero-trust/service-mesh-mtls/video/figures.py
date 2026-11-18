"""Pictures for the Service Mesh mTLS video, drawn by the `figure` slide kind.

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


def code_of(text):
    return re.search(r"HTTP (\d+)", text).group(1)


def pod(d, x, y, name, colour, proxy):
    """A pod: its app, and a proxy beside it when it has one."""
    panel(d, x, y, 520, 120, colour)
    label(d, x + 20, y + 12, "pod " + name, colour, 24, True)
    cell(d, x + 20, y + 50, 220, 54, "app", TEXT, 26)
    if proxy:
        cell(d, x + 280, y + 50, 220, 54, "proxy", AMBER, 26)


# ------------------------------------------------------------------ figures

def identity_parts(img, d, cfg):
    """One pod's ServiceAccount and namespace become the name in its certificate, part by part."""
    spec = facts(section("TWO", "THE INPUT"))
    parts = facts(section("TWO", "HOW THE NAME IS MADE"))
    certs = section("TWO", "THE CERTIFICATES")
    header(d, 60, 180, "THE INPUT: THE PAYMENTS POD", CYAN)
    panel(d, 60, 240, 900, 140)
    label(d, 90, 270, "namespace", MUTED, 28, True)
    fit_mono(d, 470, 268, spec["namespace"], TEXT, 32, 460)
    label(d, 90, 330, "serviceAccountName", MUTED, 28, True)
    fit_mono(d, 470, 328, spec["serviceAccountName"], TEXT, 32, 460)
    label(d, 1000, 260, "No key, no password, no code change:", MUTED, 28, True)
    label(d, 1000, 305, "the proxy asks the control plane for a", MUTED, 28)
    label(d, 1000, 345, "certificate, and proves the pod's account.", MUTED, 28)
    header(d, 60, 430, "HOW THE NAME IS MADE: THE PARTS, JOINED BY DOTS", AMBER)
    names = ["ServiceAccount", "namespace", "fixed", "Linkerd namespace", "trust domain"]
    widths = [270, 200, 440, 230, 340]
    colours = [CYAN, CYAN, MUTED, MUTED, AMBER]
    x = 60
    for n, w, c in zip(names, widths, colours):
        cell(d, x, 500, w, 70, parts[n], c, 30)
        label(d, x + w // 2, 585, n, c, 22, True, "ma")
        x += w + 40
        if n != names[-1]:
            label(d, x - 20, 535, ".", TEXT, 40, True, "mm")
    header(d, 60, 660, "THE RESULT: THE SUBJECT OF PAYMENTS' CERTIFICATE", GREEN)
    mine = next(c for c in certs if c.startswith(parts["ServiceAccount"] + "."))
    fit_mono(d, 60, 730, mine, GREEN, 40, 1800)
    label(d, 60, 810, "all four workloads: " + certs[-1], TEXT, 30, True)
    caption(d, "Whoever may run a pod as that ServiceAccount, in that namespace, gets that name.", AMBER, 900)


def mesh_path(img, d, cfg):
    """The same call before and after the proxies: what the wire shows, and who each end saw."""
    before = facts(section("ONE", "BEFORE THE PROXIES"))
    after = facts(section("ONE", "AFTER THE PROXIES"))
    seen_by_server = section("ONE", "WHAT PAYMENTS' PROXY COUNTED")[0].split()[0]
    seen_by_client = section("ONE", "WHAT WEB'S PROXY COUNTED")[0].split()[0]
    n_before = re.search(r"\((\d+ of \d+)", heading("ONE", "BEFORE THE PROXIES")).group(1)
    n_after = re.search(r"\((\d+ of \d+)", heading("ONE", "AFTER THE PROXIES")).group(1)
    header(d, 60, 175, "BEFORE THE PROXIES (%s pods have one)" % n_before, ROSE)
    pod(d, 60, 235, "web", CYAN, False)
    pod(d, 1340, 235, "payments", CYAN, False)
    draw.arrow_right(d, 590, 315, 1330, ROSE, 5)
    label(d, 960, 245, "on the wire, read by a capture on the node", MUTED, 24, True, "ma")
    fit_mono(d, 960, 330, before["request on wire"], ROSE, 26, 700, "ma")
    label(d, 960, 380, "%s   account readable: %s" % (before["answer"], before["account readable"]), ROSE, 26, True, "ma")
    header(d, 60, 450, "AFTER THE PROXIES (%s pods)" % n_after, GREEN)
    pod(d, 60, 510, "web", CYAN, True)
    pod(d, 1340, 510, "payments", CYAN, True)
    draw.arrow_right(d, 590, 590, 1330, GREEN, 5)
    label(d, 960, 520, "mutual TLS between the proxies, port 4143", MUTED, 24, True, "ma")
    fit_mono(d, 960, 605, "request on wire: " + after["request on wire"], GREEN, 26, 700, "ma")
    label(d, 960, 655, "%s   account readable: %s" % (after["answer"], after["account readable"]), GREEN, 26, True, "ma")
    label(d, 60, 720, "web's proxy saw the server's certificate:", MUTED, 26, True)
    fit_mono(d, 60, 760, seen_by_client, GREEN, 28, 1800)
    label(d, 60, 815, "payments' proxy saw the client's certificate:", MUTED, 26, True)
    fit_mono(d, 60, 855, seen_by_server, GREEN, 28, 1800)
    caption(d, "Same call, same answer: the wire is encrypted, and each end knows the other.", AMBER, 925)


def certificates(img, d, cfg):
    """The subject of each workload's certificate, with the ServiceAccount part picked out."""
    certs = section("TWO", "THE CERTIFICATES")
    header(d, 60, 180, "THE CERTIFICATES: linkerd identity, THE SUBJECT OF EACH", CYAN)
    rows = [c for c in certs if "." in c and " " not in c]
    for i, name in enumerate(rows):
        y = 260 + i * 120
        sa, rest = name.split(".", 1)
        panel(d, 60, y, 1800, 90, GREEN)
        f = font(MONO_B, 34)
        d.text((100, y + 45), sa, font=f, fill=CYAN, anchor="lm")
        d.text((100 + f.getlength(sa), y + 45), "." + rest, font=f, fill=TEXT, anchor="lm")
    label(d, 60, 760, "cyan: the ServiceAccount the pod runs as; the rest is the namespace and fixed parts.", MUTED, 28, True)
    label(d, 60, 815, "made by the rule: " + certs[-1].split(": ")[1], GREEN, 32, True)
    caption(d, "Four workloads, four ServiceAccounts, four names, issued with no code in the apps.", AMBER, 900)


def grid(d, rows, y0, highlight=None):
    """The `WHO ANSWERED` table as coloured cells: green when the design allows the pair, rose when it does not."""
    head = rows[0].split()
    allowed = {("bank/web", "payments"), ("bank/payments", "ledger")}
    label(d, 60, y0 + 25, "caller \\ service", MUTED, 24, True, "lm")
    for j, name in enumerate(head[1:]):
        label(d, 520 + j * 330 + 140, y0 + 25, name, TEXT, 28, True, "mm")
    for i, ln in enumerate(rows[1:]):
        parts = ln.split()
        y = y0 + 60 + i * 74
        label(d, 60, y + 30, parts[0], TEXT, 28, True, "lm")
        for j, v in enumerate(parts[1:]):
            x = 520 + j * 330
            if v == "-":
                cell(d, x, y, 280, 60, "-", LINE, 26)
                continue
            ok = (parts[0], head[1 + j]) in allowed
            colour = GREEN if (v == "yes") == ok else ROSE
            cell(d, x, y, 280, 60, "answers" if v == "yes" else "refused", colour, 26)


def matrix_open(img, d, cfg):
    """Every caller calls every bank service with mutual TLS and no policy."""
    calls = section("THREE", "THE CALLS")
    res = facts(section("THREE", "THE RESULT"))
    header(d, 60, 175, "THE CALLS: " + calls[0], CYAN)
    grid(d, section("THREE", "WHO ANSWERED"), 235)
    label(d, 60, 690, "answered %s;  not in the design: %s" % (res["answered"], res["not designed"].split()[0]), ROSE, 32, True)
    label(d, 60, 745, "from legacy/batch, which has no proxy: %s;  the node calls ledger: %s"
          % (res["from legacy/batch, no proxy"], res["the node calls ledger"]), ROSE, 30, True)
    label(d, 60, 800, "green: what the design wants;  rose: what it does not.", MUTED, 28, True)
    caption(d, "Linkerd's default policy is %s: encrypted, and open to everyone." % res["Linkerd's default policy"], AMBER, 900)


def matrix_policy(img, d, cfg):
    """The same calls with a Server and an AuthorizationPolicy per service, and ledger's refusals."""
    res = facts(section("FOUR", "THE RESULT"))
    refusals = facts(section("FOUR", "LEDGER'S PROXY COUNTED REFUSALS"))
    header(d, 60, 175, "THE POLICY: A SERVER PER SERVICE, ONE CALLER NAMED", CYAN)
    grid(d, section("FOUR", "WHO ANSWERED"), 235)
    label(d, 60, 690, "answered %s;  not in the design: %s;  missing: %s"
          % (res["answered"], res["not designed"].split()[0], res["missing"].split()[0]), GREEN, 32, True)
    header(d, 60, 750, "LEDGER'S PROXY COUNTED REFUSALS", AMBER)
    for i, (who, n) in enumerate(refusals.items()):
        cell(d, 60 + i * 520, 815, 490, 60, "%s: %s" % (who, n), AMBER, 26)
    caption(d, "Only the two pairs of the design answer; everyone else gets HTTP 403.", AMBER, 920)


def gaps(img, d, cfg):
    """What the mesh still lets through, each measured."""
    root = facts(section("FIVE", "ROOT ON THE NODE"))
    outside = facts(section("FIVE", "A POD OUTSIDE THE MESH"))
    hostile = section("FIVE", "AN ALLOWED CALLER")
    take = facts(section("FIVE", "WHO MAY TAKE AN IDENTITY"))
    create = facts(section("FIVE", "WHO MAY CREATE PODS"))
    inside = root["inside payments, proxy to app"]
    rows = [
        ("root on the node: a capture inside payments", "account readable: " + inside, ROSE),
        ("web calls legacy/batch, outside the mesh", "account readable: " + outside["account readable on the wire"], ROSE),
        ("web sends ' OR '1'='1 to payments", "HTTP %s, intact: %s" % (code_of(hostile[0]), hostile[1].rsplit(" ", 1)[1]), ROSE),
        ("a new pod in bank runs as payments, calls ledger", take["new pod in bank, runs as payments"], ROSE),
        ("a payments ServiceAccount in namespace shadow", take["payments in namespace shadow"], GREEN),
        ("a developer with the edit role creates pods", create["a developer with the edit role"], ROSE),
    ]
    header(d, 60, 175, "WHAT THE MESH DOES NOT PROTECT, MEASURED", ROSE)
    for i, (what, result, colour) in enumerate(rows):
        y = 240 + i * 100
        panel(d, 60, y, 1800, 80, colour)
        label(d, 90, y + 40, what, TEXT, 28, True, "lm")
        cell(d, 1340, y + 12, 490, 56, result, colour, 26)
    label(d, 60, 850, "the node itself calls ledger: " + root["the node calls ledger"] + " (the policy holds at the proxy)", GREEN, 28, True)
    caption(d, "The mesh judges the caller, never the request, and trusts who may create pods.", AMBER, 920)


def lifetime_bar(img, d, cfg):
    """A workload certificate's dates: the lifetime, the skew allowance at both ends, and the renewal."""
    setting = facts(section("SIX", "THE SETTING"))
    read = facts(section("SIX", "HOW THE LIFETIME IS READ"))
    renew = facts(section("SIX", "AFTER THE RENEWAL"))
    span = int(read["Not After - Not Before"].split()[0])
    m = re.match(r"(\d+) - 2 x (\d+) = (\d+) s = (.*)", read["minus the clock skew, twice"])
    skew, life, words = int(m.group(2)), int(m.group(3)), m.group(4)
    header(d, 60, 175, "THE SETTING: %s IN THIS RUN (LINKERD'S DEFAULT %s)" % (setting["this run"], setting["Linkerd's default lifetime"]), CYAN)
    header(d, 60, 260, "HOW THE LIFETIME IS READ FROM PAYMENTS' CERTIFICATE", AMBER)
    x0, x1, y = 160, 1760, 400
    k = (x1 - x0) / span
    d.rectangle([x0, y - 25, x0 + skew * k, y + 25], fill=MUTED)
    d.rectangle([x0 + skew * k, y - 25, x1 - skew * k, y + 25], fill=GREEN)
    d.rectangle([x1 - skew * k, y - 25, x1, y + 25], fill=MUTED)
    label(d, x0, y + 40, "Not Before", TEXT, 26, True, "ma")
    label(d, x1, y + 40, "Not After", TEXT, 26, True, "ma")
    label(d, x0 + skew * k / 2, y - 70, "%d s" % skew, MUTED, 24, True, "ma")
    label(d, x1 - skew * k / 2, y - 70, "%d s" % skew, MUTED, 24, True, "ma")
    label(d, (x0 + x1) / 2, y - 70, "%d s = %s" % (life, words), GREEN, 28, True, "ma")
    label(d, (x0 + x1) / 2, y + 90, "%d s on the certificate - 2 x %d s clock skew = %d s" % (span, skew, life), TEXT, 30, True, "ma")
    header(d, 60, 560, "AFTER THE RENEWAL", GREEN)
    items = [("a newer certificate", renew["a newer certificate"], "yes"), ("the same name", renew["the same name"], "yes"),
             ("the pod restarted", renew["the pod restarted"], "no"), ("payments calls ledger", renew["payments calls ledger"], "HTTP 200")]
    for i, (what, value, good) in enumerate(items):
        x = 60 + i * 460
        label(d, x + 210, 630, what, MUTED, 26, True, "ma")
        cell(d, x, 670, 420, 64, value, GREEN if value == good else ROSE, 30)
    label(d, 60, 790, "the trust anchor that signs it all: valid for " + read["the trust anchor, valid for"], MUTED, 28, True)
    caption(d, "A stolen certificate is useful for %s; the mesh renews it on its own." % words, AMBER, 900)
