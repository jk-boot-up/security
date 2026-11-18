"""Pictures for the Kubernetes Secrets video, drawn by the `figure` slide kind.

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


def after(line, key):
    """The word after `key: ` in a line."""
    return re.search(re.escape(key) + r":? (\S+?)[,.]?(?:\s|$)", line).group(1)


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


# ------------------------------------------------------------------ figures

def base64_steps(img, d, cfg):
    """Four base64 characters become three bytes, step by step, with no key."""
    s = facts(section("ONE", "HOW BASE64"))
    encoded = section("ONE", "WHAT THE API RETURNS")[0]
    chars = s["characters"].split(", ")
    places = s["place in"].split()[:4]
    six = s["6 bits"].split()
    eight = s["8 bits"].split()
    nums, text = re.match(r"(.*) = \"(.*)\"", s["bytes"]).groups()
    nums = nums.split()
    header(d, 60, 180, "THE INPUT: WHAT THE API RETURNS", CYAN)
    fit_mono(d, 60, 250, encoded, TEXT, 40, 1500)
    d.rounded_rectangle([56, 246, 56 + font(MONO_B, 40).getlength(encoded[:4]) + 8, 300], radius=8, outline=AMBER, width=3)
    rows = [("character", chars, 4, AMBER), ("place in the alphabet", places, 4, TEXT), ("6 bits", six, 4, CYAN)]
    for r, (name, vals, n, colour) in enumerate(rows):
        y = 350 + r * 95
        label(d, 60, y + 30, name, MUTED, 28, True, "lm")
        for i, v in enumerate(vals):
            cell(d, 520 + i * 330, y, 300, 60, v, colour, 30)
    label(d, 60, 670, "the same 24 bits, cut into 8", MUTED, 28, True, "lm")
    for i, v in enumerate(eight):
        cell(d, 520 + i * 440, 640, 410, 60, v, GREEN, 30)
    header(d, 60, 740, "THE RESULT", GREEN)
    for i, (n, ch) in enumerate(zip(nums, text)):
        cell(d, 520 + i * 440, 740, 410, 60, "%s = '%s'" % (n, ch), GREEN, 30)
    label(d, 60, 840, "all of it:", MUTED, 30, True)
    fit_mono(d, 260, 838, s["all of it"], ROSE, 36, 1500)
    caption(d, "Every step is a fixed table and a regrouping of bits: there is no key to know.", AMBER, 935)


def who_reads(img, d, cfg):
    """The Secret as it was created, as the API returns it, and who may read it."""
    sec = facts(section("ONE", "THE SECRET"))
    encoded = section("ONE", "WHAT THE API RETURNS")[0]
    readers = section("ONE", "WHO CAN READ IT")
    header(d, 60, 180, "THE SECRET (what was typed)", CYAN)
    panel(d, 60, 240, 820, 150)
    label(d, 90, 270, "name", MUTED, 28, True)
    fit_mono(d, 230, 270, sec["name"], TEXT, 30, 620)
    label(d, 90, 330, "key", MUTED, 28, True)
    fit_mono(d, 230, 330, sec["key"], ROSE, 30, 620)
    draw.arrow_right(d, 900, 315, 1020, MUTED, 5)
    header(d, 1040, 180, "WHAT THE API RETURNS", AMBER)
    panel(d, 1040, 240, 820, 150)
    label(d, 1070, 270, "data.key", MUTED, 28, True)
    fit_mono(d, 1070, 330, encoded, AMBER, 30, 760)
    header(d, 60, 460, "WHO CAN READ IT", ROSE)
    for i, ln in enumerate(readers):
        who, rest = re.split(r"\s{2,}", ln, maxsplit=1)
        ans = rest.rsplit(" ", 1)[1]
        y = 530 + i * 150
        panel(d, 60, y, 1800, 120, tone(ans, "no"))
        label(d, 90, y + 60, who, TEXT, 32, True, "lm")
        label(d, 1300, y + 60, "reads the value", MUTED, 30, False, "lm")
        cell(d, 1620, y + 30, 200, 60, ans, tone(ans, "no"), 32)
    caption(d, "The right to list secrets is the right to read them: base64 hides nothing.", AMBER, 900)


def etcd_bytes(img, d, cfg):
    """The bytes etcd holds in front of the card key: a field number, a length, the name, then the value."""
    holds = facts(section("TWO", "WHAT ETCD HOLDS"))
    sec = section("TWO", "THE 7 BYTES")
    count = facts(section("TWO", "EVERY SECRET"))["encrypted"]
    header(d, 60, 180, "THE INPUT: etcdctl " + section("TWO", "THE READ")[0], CYAN)
    label(d, 60, 260, "starts with", MUTED, 28, True)
    fit_mono(d, 300, 258, holds["starts with"].split("  ")[0], TEXT, 32, 700)
    label(d, 1060, 262, "(. = a byte that is not a letter)", MUTED, 26)
    header(d, 60, 340, "THE RESULT: THE 7 BYTES IN FRONT OF THE CARD KEY", ROSE)
    groups = [re.split(r"\s{2,}", ln, maxsplit=1) for ln in sec]
    x = 60
    widths = [380, 460, 460, 520]
    colours = [CYAN, AMBER, CYAN, ROSE]
    for (bytes_, meaning), w, colour in zip(groups, widths, colours):
        cell(d, x, 420, w - 20, 80, bytes_ if bytes_ != "then" else meaning.split(" (")[0], colour, 32)
        lines = [meaning] if bytes_ != "then" else ["the value, " + meaning.split(" (")[1].rstrip(")")]
        for k, t in enumerate(lines):
            label(d, x + (w - 20) // 2, 520 + k * 40, t, colour, 24, True, "ma")
        x += w
    length = re.search(r"= (\d+)", groups[2][1]).group(1)
    label(d, 60, 640, "The second byte of each pair is a length: 0x17 is %s, the length of the card key." % length, TEXT, 30)
    label(d, 60, 700, "the card key as plain text: " + after(holds["the card key"], "as plain text"), ROSE, 30, True)
    label(d, 60, 760, "secrets in etcd that are encrypted: " + count, ROSE, 30, True)
    caption(d, "Anyone with a copy of etcd reads the value straight off the disk.", AMBER, 900)


def encryption_counts(img, d, cfg):
    """How many secrets etcd holds encrypted at each step, and how the encrypted prefix reads."""
    before = facts(section("TWO", "EVERY SECRET"))["encrypted"]
    mid = facts(section("THREE", "BEFORE THE REWRITE"))["secrets encrypted"]
    end = facts(section("THREE", "AFTER THE REWRITE"))["secrets encrypted"]
    header(d, 60, 180, "SECRETS IN ETCD THAT ARE ENCRYPTED", CYAN)
    steps = [("before the configuration", before), ("after the restart", mid), ("after the rewrite", end)]
    for i, (name, value) in enumerate(steps):
        k, n = [int(v) for v in value.split(" of ")]
        y = 250 + i * 110
        label(d, 60, y + 30, name, TEXT, 30, True, "lm")
        for j in range(n):
            colour = GREEN if j < k else ROSE
            cell(d, 520 + j * 150, y, 130, 60, "enc" if j < k else "plain", colour, 26)
        label(d, 1000, y + 30, value, GREEN if k == n else ROSE, 32, True, "lm")
    marker = section("THREE", "WHAT ETCD HOLDS FOR")
    header(d, 60, 600, "THE START OF AN ENCRYPTED VALUE", AMBER)
    parts = [re.split(r"\s{2,}", ln, maxsplit=1) for ln in marker[1:5]]
    for i, (part, meaning) in enumerate(parts):
        x = 60 + i * 460
        cell(d, x, 670, 430, 64, part, AMBER, 32)
        short = meaning.replace("the provider: ", "").replace("the provider's ", "").replace(" in the configuration", "")
        label(d, x + 215, 750, short, MUTED, 24, True, "ma")
    caption(d, "New writes are encrypted at once; old secrets only when they are written again.", AMBER, 900)


def openbao_timeline(img, d, cfg):
    """The login and its answer, then the token's ten seconds on a time line."""
    role = facts(section("FOUR", "THE ROLE"))
    login = facts(section("FOUR", "THE LOGIN"))
    resp = section("FOUR", "THE RESPONSE")
    reads = facts(section("FOUR", "READING THE PASSWORD"))
    other = section("FOUR", "ANOTHER POD")
    ttl = int(role["token"].split()[0])
    header(d, 60, 180, "THE REQUEST: THE POD'S TOKEN", CYAN)
    panel(d, 60, 240, 880, 140)
    label(d, 90, 270, "sub", MUTED, 26, True)
    fit_mono(d, 190, 270, login["sub"], TEXT, 26, 720)
    label(d, 90, 325, "aud", MUTED, 26, True)
    fit_mono(d, 190, 325, login["aud"], TEXT, 26, 720)
    draw.arrow_right(d, 960, 310, 1060, MUTED, 5)
    header(d, 1080, 180, "THE RESPONSE FROM OPENBAO", GREEN)
    panel(d, 1080, 240, 780, 140)
    fit_mono(d, 1110, 270, resp[0], GREEN, 28, 720)
    fit_mono(d, 1110, 325, "policies " + facts(resp[1:])["policies"], TEXT, 26, 720)
    header(d, 60, 440, "THE TOKEN ON A TIME LINE (seconds)", AMBER)
    x0, x1, y = 160, 1760, 600
    k = (x1 - x0) / 14
    d.rectangle([x0, y - 20, x0 + ttl * k, y + 20], fill=GREEN)
    d.rectangle([x0 + ttl * k, y - 20, x1, y + 20], fill=ROSE)
    for s in range(0, 15, 2):
        label(d, x0 + s * k, y + 30, str(s), MUTED, 24, True, "ma")
    label(d, x0 + ttl * k, y - 70, "%d s: the token ends" % ttl, AMBER, 26, True, "ma")
    for s, (name, ans) in ((0, ("at once", reads["at once"])), (12, ("12 s later", reads["12 s later"]))):
        code = re.search(r"HTTP (\d+)", ans).group(1)
        cell(d, x0 + s * k - 110, y + 80, 220, 56, "HTTP " + code, GREEN if code == "200" else ROSE, 28)
    who = other[0].split()[-1].rsplit(":", 1)[1]
    code = re.search(r"HTTP (\d+)", other[1]).group(1)
    label(d, 60, 830, "%s logs in with its own token: HTTP %s, it is not the role's ServiceAccount." % (who, code), ROSE, 30, True)
    caption(d, "A stolen OpenBao token is worth %d seconds." % ttl, AMBER, 910)


def tokens_compare(img, d, cfg):
    """The bound token and the old token Secret side by side, and the API server's answers."""
    bound = facts(section("FIVE", "THE BOUND TOKEN"))
    old = facts(section("FIVE", "THE OLD TOKEN"))
    review = [re.split(r"\s{2,}", ln, maxsplit=1) for ln in section("FIVE", "THE API SERVER")]
    for x, title, colour, rows in ((60, "THE BOUND TOKEN", GREEN, [("lifetime", bound["lifetime"]), ("audience", bound["audience"]), ("pod", bound["pod"])]),
                                   (980, "THE OLD TOKEN SECRET", ROSE, [("lifetime", old["lifetime"]), ("audience", "none (aud: %s)" % after(old["claims"], "aud")), ("pod", old["pod"])])):
        header(d, x, 180, title, colour)
        for i, (k, v) in enumerate(rows):
            y = 250 + i * 80
            panel(d, x, y, 880, 64, colour)
            label(d, x + 25, y + 32, k, MUTED, 26, True, "lm")
            fit_mono(d, x + 220, y + 32, v, TEXT, 26, 640, "lm")
    header(d, 60, 520, "THE API SERVER'S ANSWER (TokenReview)", AMBER)
    label(d, 900, 590, "bound", GREEN, 28, True, "ma")
    label(d, 1300, 590, "old", ROSE, 28, True, "ma")
    for i, (when, ans) in enumerate(review):
        y = 640 + i * 90
        label(d, 60, y + 30, when, TEXT, 30, True, "lm")
        b, o = after(ans, "bound"), after(ans, "old")
        cell(d, 800, y, 200, 60, b, CYAN if i == 0 else tone(b, "no"), 30)
        cell(d, 1200, y, 200, 60, o, CYAN if i == 0 else tone(o, "no"), 30)
    caption(d, "Delete the pod and its token dies with it; the old token never does.", AMBER, 900)
