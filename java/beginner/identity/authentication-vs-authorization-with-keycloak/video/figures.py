"""Pictures for the Keycloak video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints and
the tests check, so a picture can never disagree with the code.
"""

import re
from pathlib import Path

from videokit.slides import draw
from videokit.slides.theme import (ACCENT, AMBER, CLIENT, CYAN, GREEN, MONO_B, MUTED, PANEL, ROSE,
                                   SANS, SANS_B, TEXT, W, font)

OUT = (Path(__file__).resolve().parent.parent / "docs" / "expected-output.txt").read_text()
BG_CELL = (22, 32, 54)
INK = (15, 23, 42)


def section(act, part=None):
    """One act of the output ("TWO"), or one part of it ("THE RESPONSES")."""
    text = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % act, OUT, re.M).group(1)
    if part:
        text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(part), text, re.M).group(1)
    return text


def numbered(act, part):
    """The lines '    1  ...' of one part, without their numbers."""
    return [m.strip() for m in re.findall(r"^    \d  (.+)$", section(act, part), re.M)]


def value(label, text):
    return re.search(r"(?:^|\s)" + re.escape(label) + r"\s+(.+)$", text, re.M).group(1).strip()


def header(d, x, y, text, colour, w=None):
    """A section label such as THE REQUEST: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=INK, anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def row(d, x, y, w, text, colour, size=28, h=58, mono=True):
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BG_CELL, outline=colour, width=3)
    d.text((x + 24, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size), fill=colour, anchor="lm")


def code_colour(text):
    return GREEN if text.startswith("200") else ROSE if text.startswith("401") else AMBER


def big_box(d, x, y, w, h, text, sub, colour, size=40):
    d.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + w // 2, y + h // 2 - (18 if sub else 0)), text, font=font(SANS_B, size), fill=colour, anchor="mm")
    if sub:
        d.text((x + w // 2, y + h // 2 + 28), sub, font=font(SANS, 24), fill=MUTED, anchor="mm")


def gate(d, x, y, text, colour):
    """A box with its name at the top, for the three lines drawn inside it."""
    d.rounded_rectangle([x, y, x + 500, y + 230], radius=18, fill=PANEL, outline=colour, width=4)
    d.text((x + 250, y + 36), text, font=font(SANS_B, 34), fill=colour, anchor="mm")


# ------------------------------------------------------------------ figures

def two_questions(img, d, cfg):
    """The core idea on one request: authentication answers who, authorization answers what."""
    claims = section("TWO", "AFTER DECODING")
    who = value("preferred_username", claims)
    roles = re.search(r'"roles":\[([^\]]*)\]', claims).group(1).replace('"', "")
    answer = numbered("THREE", "THE RESPONSES")[1].split()[0]
    header(d, 100, 205, "THE REQUEST", GREEN)
    row(d, 100, 265, 560, "GET /orders/B-2", GREEN, 32)
    label(d, 100, 340, "Bearer token, signed by the login server:", MUTED, 24)
    for i, (k, v) in enumerate([("iss", value("iss", claims)), ("aud", value("aud", claims)),
                                ("sub", who), ("roles", roles), ("exp", value("exp", claims).split(" = ")[1])]):
        label(d, 110, 380 + i * 44, k, CYAN, 26, True)
        label(d, 220, 380 + i * 44, v, TEXT, 26)
    gate(d, 760, 230, "1. AUTHENTICATION", CLIENT)
    label(d, 1010, 310, "who is asking?", TEXT, 28, True, "ma")
    label(d, 1010, 355, "signature, expiry, issuer, audience", MUTED, 22, False, "ma")
    label(d, 1010, 400, "answer: %s" % who, GREEN, 30, True, "ma")
    gate(d, 760, 520, "2. AUTHORIZATION", ACCENT)
    label(d, 1010, 600, "may %s read B-2?" % who, TEXT, 28, True, "ma")
    label(d, 1010, 645, "role %s, and B-2 is Ben's" % roles, MUTED, 22, False, "ma")
    label(d, 1010, 690, "answer: no", ROSE, 30, True, "ma")
    draw.arrow_right(d, 670, 294, 752, GREEN, 4)
    draw.arrow(d, 1010, 462, 1010, 515, CLIENT, 4)
    header(d, 1340, 205, "THE RESPONSE", ROSE)
    draw.arrow_right(d, 1265, 635, 1340, ROSE, 4)
    row(d, 1350, 600, 440, "%s Forbidden" % answer, ROSE, 32, 70)
    draw.arrow_right(d, 1265, 345, 1340, AMBER, 3)
    label(d, 1350, 300, "fails here: 401", AMBER, 30, True)
    label(d, 1350, 345, "the shop does not know you", MUTED, 24)
    caption(d, "401: who are you?   403: I know you, and the answer is no", AMBER, 850)


def tokens_only(img, d, cfg):
    """Act one: five requests with different tokens, and the five responses."""
    reqs = numbered("ONE", "THE REQUESTS")
    resps = numbered("ONE", "THE RESPONSES")
    header(d, 100, 205, "THE REQUESTS: GET /me with ...", GREEN, 860)
    header(d, 1080, 205, "THE RESPONSES", CYAN, 740)
    for i, (q, a) in enumerate(zip(reqs, resps)):
        y = 275 + i * 112
        c = code_colour(a)
        row(d, 100, y, 860, q, TEXT, 26, 76, False)
        draw.arrow_right(d, 970, y + 38, 1070, c, 4)
        code, _, rest = a.partition("  ")
        row(d, 1080, y, 740, code, c, 34, 76)
        label(d, 1200, y + 38, rest.split("  <-")[0], c, 26, True, "lm")
    caption(d, "The expiry is inside the signed token: no call to the login server", AMBER, 850)


def signature_steps(img, d, cfg):
    """Act two: how the shop checks the RS256 signature, with the real hashes."""
    t = section("TWO")
    signed = re.search(r"STEP 1.*\n    (.+)", t).group(1)
    h = re.search(r"    hash     (.+)\n\s+(.+)", t)
    s = re.search(r"    last 32  (.+)\n\s+(.+)", t)
    pad = re.search(r"then (\d+) bytes of ff", t).group(1)
    bad = re.search(r"AFTER TAMPERING.*\n    hash     (.+)", t).group(1)
    head, claims = signed.split(".", 1)
    header(d, 100, 200, "BEFORE: the token = header . claims . signature", GREEN, 900)
    d.text((100, 268), head, font=font(MONO_B, 26), fill=CLIENT)
    w = font(MONO_B, 26).getlength(head + ".")
    d.text((100 + w, 268), claims.rstrip(".") + "  .  [256-byte signature]", font=font(MONO_B, 26), fill=ACCENT)
    steps = [("1", "signed text = header . claims", "the first two parts, as sent", TEXT),
             ("2", "SHA-256(signed text)", h.group(1) + " ...", CYAN),
             ("3", "signature ^ e mod n  (public key)", "00 01, %s x ff, 00, \"SHA-256\", then: %s ..." % (pad, s.group(1)[:17]), AMBER),
             ("4", "compare the two hashes", "equal: the login server signed exactly this text", GREEN)]
    for i, (n, what, val, c) in enumerate(steps):
        y = 330 + i * 112
        d.ellipse([100, y, 160, y + 60], outline=c, width=4)
        d.text((130, y + 30), n, font=font(SANS_B, 30), fill=c, anchor="mm")
        label(d, 190, y + 2, what, c, 30, True)
        label(d, 190, y + 46, val, MUTED, 26)
    header(d, 100, 790, "AFTER TAMPERING: customer -> admin", ROSE, 640)
    label(d, 780, 800, "new hash %s ...  =/=  %s ..." % (bad[:17], s.group(1)[:17]), ROSE, 26, True)
    caption(d, "Change one character and the hashes differ: 401", AMBER, 880)


def roles_to_authorities(img, d, cfg):
    """Act three: realm_access.roles become ROLE_ authorities, then the shop's rules decide."""
    t = section("THREE")
    token_roles = re.search(r"IN THE TOKEN.*\n    (.+)", t).group(1).split("   ")
    shop_roles = re.search(r"IN THE SHOP.*\n    (.+)", t).group(1).split("   ")
    reqs = numbered("THREE", "THE REQUESTS")
    resps = numbered("THREE", "THE RESPONSES")
    header(d, 100, 200, "IN THE TOKEN: realm_access.roles", GREEN, 620)
    header(d, 1100, 200, "IN THE SHOP: authorities", CYAN, 620)
    for i, (a, b) in enumerate(zip(token_roles, shop_roles)):
        y = 260 + i * 70
        row(d, 100, y, 620, a, GREEN, 28, 56)
        draw.arrow_right(d, 730, y + 28, 1090, MUTED, 3)
        label(d, 910, y + 2, '"ROLE_" + capitals', MUTED, 20, False, "ma")
        row(d, 1100, y, 620, b, CYAN, 28, 56)
    header(d, 100, 490, "THE REQUESTS", GREEN)
    header(d, 1100, 490, "THE RESPONSES", CYAN)
    for i, (q, a) in enumerate(zip(reqs, resps)):
        y = 548 + i * 56
        parts = q.split()
        label(d, 100, y, "%-5s %s" % (parts[0], parts[2]), TEXT, 26, True)
        a = a.split("  <-")[0]
        label(d, 1100, y, a, code_colour(a), 26, True)
    caption(d, "Logged in is not the same as allowed: Ana gets 403 for Ben's order", AMBER, 900)


def issuer_audience(img, d, cfg):
    """Act four: correctly signed tokens with the wrong issuer or audience."""
    reqs = numbered("FOUR", "THE REQUESTS")
    resps = numbered("FOUR", "THE RESPONSES")
    want = re.search(r"THE RESPONSES \(wanted: (.+)\):", section("FOUR")).group(1)
    header(d, 100, 205, "THE REQUESTS: signed with the right key", GREEN, 900)
    header(d, 1120, 205, "THE RESPONSES", CYAN, 700)
    for i, (q, a) in enumerate(zip(reqs, resps)):
        y = 290 + i * 160
        row(d, 100, y, 900, q, AMBER, 26, 80)
        draw.arrow_right(d, 1010, y + 40, 1110, ROSE, 4)
        row(d, 1120, y, 700, a.split("  <-")[0], ROSE, 30, 80)
    d.rounded_rectangle([100, 640, 1820, 760], radius=18, fill=PANEL, outline=CYAN, width=3)
    label(d, 960, 660, "the shop accepts only", MUTED, 26, False, "ma")
    label(d, 960, 700, want, CYAN, 32, True, "ma")
    caption(d, "Signature, expiry, issuer, audience: all four, every time", AMBER, 850)


def stale_copy(img, d, cfg):
    """Act five: the roles in a token are a copy, until it expires."""
    t = section("FIVE")
    before = re.search(r"BEFORE THE DEMOTION.*\n    GET /admin/report\s+(\d+)", t).group(1)
    same = re.search(r"the same token:\s+GET /admin/report\s+(\d+)", t).group(1)
    later = re.search(r"301 seconds later:\s+GET /admin/report\s+(\d+)", t).group(1)
    fresh = re.search(r"a new token says.*\n\s+GET /admin/report\s+(\d+)", t).group(1)
    life = re.search(r"lifetime: (\d+) s", t).group(1)
    header(d, 100, 205, "BEFORE THE DEMOTION", GREEN)
    header(d, 760, 205, "AFTER THE DEMOTION", ROSE)
    d.line([(120, 520), (1800, 520)], fill=MUTED, width=4)
    points = [(250, "token says admin", before, GREEN), (700, "demoted, same token", same, ROSE),
              (1150, "301 s: token expired", later, AMBER), (1600, "new token: customer", fresh, CYAN)]
    for x, what, code, c in points:
        d.ellipse([x - 14, 506, x + 14, 534], fill=c)
        label(d, x, 430, what, c, 26, True, "ma")
        row(d, x - 150, 570, 300, "report " + code, code_colour(code), 30, 64)
    d.rounded_rectangle([560, 300, 1010, 360], radius=12, outline=ROSE, width=3)
    label(d, 785, 315, "the stale window", ROSE, 26, True, "ma")
    caption(d, "The shop only sees the copy: stale for at most the lifetime, %s s" % life, AMBER, 760)


def revocation(img, d, cfg):
    """Act six: a revocation list ends a token at once, and is read on every request."""
    t = section("SIX")
    jti = re.search(r"jti (\S+)\)", t).group(1)
    before = re.findall(r"^    GET /admin/report  (\d+)", section("SIX", "BEFORE REVOKING"), re.M)
    after = re.search(r"^    GET /admin/report  (\d+)", section("SIX", "AFTER REVOKING"), re.M).group(1)
    reads = re.search(r"read (\d+) times for (\d+) requests", t)
    header(d, 100, 205, "BEFORE REVOKING", GREEN)
    header(d, 1000, 205, "AFTER REVOKING", ROSE)
    for i, code in enumerate(before):
        row(d, 100, 280 + i * 90, 720, "GET /admin/report  " + code, GREEN, 30, 70)
    row(d, 1000, 280, 820, "GET /admin/report  " + after, ROSE, 30, 70)
    big_box(d, 640, 520, 640, 150, "revocation list", "holds %s" % jti, AMBER, 36)
    draw.arrow(d, 460, 445, 760, 515, AMBER, 3)
    draw.arrow(d, 1400, 355, 1160, 515, AMBER, 3)
    label(d, 960, 700, "read %s times for %s requests" % (reads.group(1), reads.group(2)), AMBER, 30, True, "ma")
    caption(d, "Ended at once, but the shop now asks on every request", AMBER, 820)
