"""Pictures for the OpenBao secrets video, drawn by the `figure` slide kind.

Every value shown is read from docs/expected-output.txt, which the demo prints against a
real OpenBao and PostgreSQL and the integration test checks, so a picture can never
disagree with the code.
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
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def section(name, head):
    """The lines of the first section of an act whose header starts with `head`, stripped."""
    found = None
    for ln in act(name):
        if not ln.startswith("  "):
            if found is not None:
                break
            if ln.startswith(head):
                found = []
        elif found is not None:
            found.append(ln.strip())
    return found


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head)).rstrip(":")


def facts(lines):
    return dict(tuple(re.split(r"\s{2,}", ln, maxsplit=1)) for ln in lines if re.search(r"\s{2,}", ln))


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
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, colour, size, w - 16, "mm")


def verdict(text):
    return GREEN if text.startswith("allowed") or text == "true" else ROSE


# ------------------------------------------------------------------ figures

def approle_login(img, d, cfg):
    """The AppRole login on one example: what goes in, what comes out, and the same secret id a second time."""
    req = facts(section("ONE", "THE REQUEST"))
    res = facts(section("ONE", "THE RESPONSE"))
    again = facts(section("ONE", "THE SAME SECRET ID"))["status"]
    nobody = facts(section("ONE", "A SECRET ID NOBODY"))["status"]
    header(d, 60, 180, heading("ONE", "THE REQUEST"), CYAN)
    for i, (k, v) in enumerate(req.items()):
        y = 260 + i * 90
        d.rounded_rectangle([60, y, 820, y + 72], radius=12, fill=PANEL, outline=LINE, width=2)
        fit_mono(d, 85, y + 36, k, AMBER, 28, 200, "lm")
        label(d, 300, y + 36, v, TEXT, 24, False, "lm")
    draw.arrow_right(d, 840, 345, 1000, MUTED, 6)
    label(d, 920, 380, "OpenBao", MUTED, 24, True, "ma")
    header(d, 1020, 180, "THE RESPONSE", GREEN)
    for i, (k, v) in enumerate(res.items()):
        y = 260 + i * 90
        d.rounded_rectangle([1020, y, 1860, y + 72], radius=12, fill=PANEL, outline=verdict(v) if k == "status" else LINE, width=2)
        fit_mono(d, 1045, y + 36, k, AMBER, 28, 150, "lm")
        fit_mono(d, 1200, y + 36, v, verdict(v) if k == "status" else TEXT, 26, 640, "lm")
    header(d, 60, 520, "THE SAME SECRET ID A SECOND TIME", ROSE)
    cell(d, 60, 590, 760, 80, "status " + again, verdict(again), 30)
    header(d, 1020, 520, "A SECRET ID NOBODY ISSUED", ROSE)
    cell(d, 1020, 590, 840, 80, "status " + nobody, verdict(nobody), 30)
    label(d, 60, 720, "A secret id works once: a thief who saw it after the application used it gets nothing.", TEXT, 28)
    label(d, 60, 770, "The token carries only the application's own policy, and ends by itself.", TEXT, 28)
    caption(d, "Role id + one-time secret id  ->  a short-lived token with one policy.", AMBER, 940)


def policy_check(img, d, cfg):
    """The policy text, and six reads decided by the rule and by the real server."""
    rules = section("ONE", "THE POLICY FOR")
    header(d, 60, 180, heading("ONE", "THE POLICY FOR").split(" (")[0], CYAN)
    for i, r in enumerate(rules):
        fit_mono(d, 60, 250 + i * 46, r, TEXT, 26, 1800)
    rows = [re.split(r"\s{2,}", ln) for ln in section("ONE", "HOW THE POLICY DECIDES")]
    head, rows = rows[0], rows[1:]
    header(d, 60, 370, "HOW THE POLICY DECIDES A READ", AMBER)
    xs = [60, 300, 1090, 1440]
    for x, h in zip(xs, head):
        label(d, x, 440, h, MUTED, 26, True)
    for i, (who, path, rule, real) in enumerate(rows):
        y = 485 + i * 66
        label(d, xs[0], y + 26, who, TEXT, 26, True, "lm")
        fit_mono(d, xs[1], y + 26, path, TEXT, 26, 760, "lm")
        cell(d, xs[2], y, 300, 54, rule, verdict(rule), 26)
        cell(d, xs[3], y, 400, 54, real, verdict(real), 26)
    caption(d, "A star covers every path below it; anything no rule names is refused.", AMBER, 905)


def versions_audit(img, d, cfg):
    """The payment key's versions through a changeover, and how the audit trail hides a value."""
    before = facts(section("TWO", "BEFORE THE ROTATION"))
    after = facts(section("TWO", "AFTER THE PROVIDER"))
    destroy = facts(section("TWO", "DESTROYING VERSION 1"))
    header(d, 60, 180, "THE PAYMENT KEY'S VERSIONS", CYAN)
    cols = [("before", {"version 1": before["current"]}, "current: version 1"),
            ("changeover", {"version 1": after["version 1"], "version 2": after["version 2"]}, "current: version 2"),
            ("after destroying v1", {"version 1": destroy["then version 1"], "version 2": destroy["then version 2"]},
             "the operator only: " + destroy["by the operator"])]
    for i, (name, vs, note) in enumerate(cols):
        x = 60 + i * 610
        d.rounded_rectangle([x, 250, x + 570, 600], radius=16, fill=PANEL, outline=LINE, width=2)
        label(d, x + 285, 270, name, TEXT, 30, True, "ma")
        for k, (v, value) in enumerate(vs.items()):
            colour = ROSE if value.startswith("refused") else GREEN
            label(d, x + 30, 350 + k * 100, v, MUTED, 24, True)
            cell(d, x + 30, 380 + k * 100, 510, 56, value, colour, 26)
        label(d, x + 285, 560, note, AMBER, 24, True, "ma")
        if i < 2:
            draw.arrow_right(d, x + 575, 425, x + 605, MUTED, 4)
    hide = section("TWO", "HOW THE AUDIT TRAIL HIDES")
    header(d, 60, 650, heading("TWO", "HOW THE AUDIT TRAIL HIDES"), AMBER)
    m = re.match(r"(\S+) becomes\s+(.*)", hide[0])
    cell(d, 60, 720, 260, 64, m.group(1), TEXT, 30)
    draw.arrow_right(d, 340, 752, 470, MUTED, 5)
    label(d, 405, 790, "HMAC", MUTED, 22, True, "ma")
    cell(d, 490, 720, 1370, 64, m.group(2), AMBER, 26)
    fit_mono(d, 60, 820, hide[1], GREEN, 26, 1800)
    caption(d, "Old versions stay readable until destroyed; the audit trail never holds a value.", AMBER, 940)


def leased_logins(img, d, cfg):
    """Two leased database logins: what each may read, and which still connects after the time to live."""
    role = section("THREE", "THE ROLE")
    ttl, maxi = [int(n) for n in re.findall(r"(\d+) s", role[-1])]
    later = float(re.search(r"AFTER ([\d.]+) SECONDS", heading("THREE", "AFTER")).group(1))
    after = facts(section("THREE", "AFTER"))
    can = facts(section("THREE", "WHAT THE LOGINS"))
    header(d, 60, 180, "TWO LEASED LOGINS: TIME TO LIVE %d s, MAXIMUM %d s" % (ttl, maxi), CYAN)
    x0, x1, end = 420, 1840, maxi * 1.6

    def px(t):
        return x0 + t * (x1 - x0) / end
    rows = [("reports", ttl, "never renewed", list(after.values())[0]),
            ("shop-api", maxi, "renewed, at most the maximum", list(after.values())[1])]
    for i, (who, until, note, ok) in enumerate(rows):
        y = 290 + i * 150
        label(d, 60, y + 25, who, TEXT, 30, True, "lm")
        d.rectangle([px(0), y, px(until), y + 50], fill=GREEN if i else AMBER)
        label(d, max(px(until), px(later)) + 16, y + 25, note, MUTED, 24, True, "lm")
        label(d, 60, y + 80, "connects after %s s: %s" % (later, ok), verdict(ok), 26, True)
    tx = px(later)
    d.line([tx, 260, tx, 560], fill=TEXT, width=4)
    label(d, tx, 570, "t = %s s" % later, TEXT, 24, True, "ma")
    for t in (0, ttl, maxi):
        label(d, px(t), 610, "%d s" % t, MUTED, 22, False, "ma")
    header(d, 60, 670, "WHAT shop-api's LOGIN MAY READ", AMBER)
    for i, (k, v) in enumerate((k, v) for k, v in can.items() if k.startswith("shop-api")):
        table = k.split()[-1]
        cell(d, 60 + i * 620, 740, 580, 70, "%s: %s" % (table, "yes" if v == "true" else "no"), verdict(v), 30)
    fit_mono(d, 60, 840, role[2], MUTED, 24, 1800)
    caption(d, "Each login is new, belongs to one application, and ends with its lease.", AMBER, 940)


def blast_radius(img, d, cfg):
    """A stolen static password before and after rotation, and three stolen leased logins before and after a revoke."""
    rot = facts(section("FOUR", "AFTER THE ROTATION"))
    before = section("FOUR", "BEFORE THE ROTATION")[0].rsplit(": ", 1)[1]
    rev = facts(section("FOUR", "AFTER REVOKING"))
    n_still, n_all = re.match(r"(\d+) of (\d+)", rev["logins that still connect"]).groups()
    header(d, 60, 180, "A STATIC PASSWORD: ROTATE IT", CYAN)
    cell(d, 60, 260, 820, 72, "thief's copy, before: connects " + before, verdict(before), 28)
    draw.arrow(d, 470, 345, 470, 400, MUTED, 5)
    for i, (k, v) in enumerate(rot.items()):
        cell(d, 60, 420 + i * 90, 820, 72, "%s: %s" % (k, v), GREEN if (v == "true") != ("thief" in k) else ROSE, 28)
    header(d, 1000, 180, "LEASED LOGINS: REVOKE THEM", CYAN)
    for i in range(int(n_all)):
        x = 1000 + i * 290
        cell(d, x, 260, 260, 72, "login %d" % (i + 1), GREEN, 28)
        cell(d, x, 420, 260, 72, "ended" if i >= int(n_still) else "works", ROSE if i >= int(n_still) else GREEN, 28)
    draw.arrow(d, 1420, 345, 1420, 400, MUTED, 5)
    label(d, 1440, 360, "revoke-prefix: " + rev["status"], MUTED, 24, True, "lm")
    label(d, 1000, 530, "still connect: %s of %s" % (n_still, n_all), ROSE if n_still != "0" else GREEN, 30, True)
    label(d, 1000, 590, "reports still gets one: " + rev["reports still gets one"], verdict(rev["reports still gets one"]), 28, True)
    caption(d, "A rotation ends one copied password; a revoke ends every login of one application.", AMBER, 900)


def sealed(img, d, cfg):
    """OpenBao open, sealed and unsealed again: which requests still work."""
    seal = facts(section("FIVE", "THE OPERATOR SEALS"))
    while_ = facts(section("FIVE", "WHILE IT IS SEALED"))
    unseal = facts(section("FIVE", "AFTER UNSEALING"))
    cols = [("SEAL", "the operator seals", {"status": seal["status"]}, AMBER),
            ("SEALED", "the store asks", {k: v.split("  <-")[0] for k, v in while_.items()}, ROSE),
            ("UNSEALED", "with the unseal key", unseal, GREEN)]
    for i, (name, sub, rows, colour) in enumerate(cols):
        x = 60 + i * 610
        header(d, x, 180, name, colour)
        label(d, x, 250, sub, MUTED, 26, True)
        for k, (what, v) in enumerate(rows.items()):
            y = 310 + k * 130
            label(d, x, y, what, TEXT, 26, True)
            ok = v.startswith("allowed") or v.endswith("true")
            cell(d, x, y + 40, 560, 64, v, GREEN if ok else ROSE, 26)
        if i < 2:
            draw.arrow_right(d, x + 570, 420, x + 600, MUTED, 4)
    label(d, W // 2, 760, "While it is sealed, no server of the store can get a secret or a new login,", TEXT, 30, False, "ma")
    label(d, W // 2, 810, "so no new server can start. Logins already held work until their lease ends.", TEXT, 30, False, "ma")
    caption(d, "The secrets service is now the system the whole store depends on.", AMBER, 920)
