"""Pictures for the broken access control with Spring Security video, drawn by the `figure` slide kind.

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
DIM = (18, 26, 44)
INK = (15, 23, 42)


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading, two spaces removed."""
    body = re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)
    return [ln[2:] for ln in body.splitlines()]


def part(name, head):
    """The indented lines under the first section header of an act that starts with `head`, two spaces removed."""
    out, on = [], False
    for ln in act(name):
        if not ln.startswith("  "):
            if on:
                break
            on = ln.startswith(head)
        elif on:
            out.append(ln[2:])
    return out


def heading(name, head):
    return next(ln for ln in act(name) if ln.startswith(head))


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
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def caption(d, text, colour=AMBER, y=930):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)




def rows(lines):
    """Fixed-width request rows: (who, method, path, status, mark)."""
    out = []
    for ln in lines:
        m = re.match(r"\s*(\S+)\s+(GET|POST|PUT|DELETE)\s+(\S+)\s+(\d{3})(?:\s+<- (.+))?$", ln)
        if m:
            out.append(m.groups())
    return out


def matches(pattern, path):
    """Spring-style path matching for the patterns shown: ** matches the rest, otherwise exact."""
    if pattern.endswith("/**"):
        return path.startswith(pattern[:-3] + "/") or path == pattern[:-3]
    return path == pattern


# ------------------------------------------------------------------ figures

def spring_layers(img, d, cfg):
    """Three places a request can be refused: the filter chain, the method rule, the query."""
    req = rows(part("TWO", "THE REQUESTS"))
    chain401 = next(r for r in req if r[3] == "401")
    chain403 = next(r for r in req if r[3] == "403")
    job = part("FOUR", "A NIGHTLY JOB")[0].strip()
    query = next(ln for ln in part("THREE", "THE ATTACKS") if "ben's order 1002" in ln)
    steps = [("1. THE FILTER CHAIN", "method and path, before any controller",
              ["%s %s %s -> %s" % (chain401[0], chain401[1], chain401[2], chain401[3]),
               "%s %s %s -> %s" % (chain403[0], chain403[1], chain403[2], chain403[3])], AMBER),
             ("2. METHOD SECURITY", "a rule on the service method itself", [job], CYAN),
             ("3. THE QUERY", "the order is looked up with its owner",
              ["%s -> %s" % (re.sub(r"\s{2,}\d{3}$", "", query.strip()), query.split()[-1])], GREEN)]
    for i, (name, what, examples, c) in enumerate(steps):
        y = 220 + i * 210
        panel(d, 100, y, 720, 170, c, 3)
        label(d, 130, y + 40, name, c, 30, True, "lm")
        label(d, 130, y + 90, what, TEXT, 24, False, "lm")
        if i < 2:
            draw.arrow(d, 460, y + 172, 460, y + 208, MUTED, 4)
        for j, ex in enumerate(examples):
            fit_mono(d, 900, y + 55 + j * 50, ex, TEXT, 26, 950, "lm")
    caption(d, "A URL rule cannot know whose order it is; the query can.", AMBER, 880)


def naive_rules(img, d, cfg):
    """The naive rules in order, and which rule each request matched."""
    rules = []
    for ln in part("ONE", "THE RULES AS WRITTEN"):
        pats, rule = re.split(r"\s{2,}", ln.strip())
        rules.append(([p.strip() for p in pats.split(",")], rule))
    header(d, 60, 200, "THE RULES AS WRITTEN (in order)", CYAN)
    for i, (pats, rule) in enumerate(rules):
        y = 260 + i * 90
        c = ROSE if "permitAll" in rule else CYAN
        panel(d, 60, y, 700, 72, c, 3)
        mono(d, 80, y + 36, ", ".join(pats), TEXT, 24, "lm")
        mono(d, 740, y + 36, rule, c, 24, "rm")
    header(d, 860, 200, "THE REQUESTS, AND THE RULE EACH ONE MATCHED", AMBER)
    for i, (who, method, path, code, mark) in enumerate(rows(part("ONE", "THE REQUESTS"))):
        y = 260 + i * 100
        hit = next(k for k, (pats, _) in enumerate(rules) if any(p == "anyRequest()" or matches(p, path) for p in pats))
        c = ROSE if mark else GREEN
        panel(d, 860, y, 680, 80, c)
        mono(d, 880, y + 40, "%-7s%-7s%s" % (who, method, path), TEXT, 24, "lm")
        cell(d, 1560, y + 10, 110, 60, code, c, 28)
        label(d, 1690, y + 40, "rule %d" % (hit + 1), MUTED, 22, True, "lm")
        if i == 0:
            pass
    for i in range(len(rules)):
        label(d, 40, 296 + i * 90, str(i + 1), MUTED, 22, True, "mm")
    res = part("ONE", "THE RESULT")
    label(d, 60, 560, res[0].strip(), ROSE, 26, True)
    label(d, 60, 600, res[1].strip(), ROSE, 26, True)
    caption(d, "Each rule did exactly what it said; permitAll() caught what nobody listed.", AMBER, 880)


def deny_all(img, d, cfg):
    """Requests that match a rule and fail it, and requests that fall through to denyAll()."""
    header(d, 60, 200, "THE RULES AS WRITTEN", CYAN)
    panel(d, 60, 260, 560, 120, GREEN, 3)
    label(d, 340, 320, part("TWO", "THE RULES AS WRITTEN")[0].strip(), GREEN, 26, True, "mm")
    panel(d, 60, 400, 560, 90, ROSE, 3)
    mono(d, 340, 445, "anyRequest().denyAll()", ROSE, 28, "mm")
    label(d, 340, 520, "the last rule: everything else", MUTED, 24, False, "mm")
    header(d, 720, 200, "THE REQUESTS", AMBER)
    for i, (who, method, path, code, mark) in enumerate(rows(part("TWO", "THE REQUESTS"))):
        y = 260 + i * 78
        fell = mark and "NO RULE" in mark
        c = ROSE if fell else AMBER
        panel(d, 720, y, 640, 64, c)
        mono(d, 740, y + 32, "%-7s%-7s%s" % (who, method, path), TEXT, 24, "lm")
        cell(d, 1380, y + 6, 110, 52, code, c, 26)
        label(d, 1510, y + 32, "fell to denyAll()" if fell else ("not signed in" if code == "401" else "rule, wrong role"),
              c, 22, True, "lm")
    gap = " ".join(ln.strip() for ln in part("TWO", "WHAT A URL RULE"))
    caption(d, "The gap that is left: " + gap.replace("GET /orders/{id} can only say: signed in it", "a URL rule"), AMBER, 880)


def query_vs_post(img, d, cfg):
    """The owner check in the query (404, nothing loaded) against @PostAuthorize (403, after the lookup)."""
    why = heading("THREE", "WHY 404")
    m = re.search(r"(findBy\w+)\(([^)]*)\)", why)
    find = "%s(%s)" % (m.group(1), m.group(2))
    attack = next(ln for ln in part("THREE", "THE ATTACKS") if "ben's order 1002" in ln).split()[-1]
    post = part("THREE", "THE SAME CHECK")
    code_post = re.search(r": (\d{3})", post[0]).group(1)
    lanes = [("IN THE QUERY", GREEN, [("ask", find.replace("(id, ana)", "(1002, ana)")), ("database", "no such row"),
                                      ("answer", attack + "  like a missing order")]),
             ("AFTER THE METHOD (@PostAuthorize)", AMBER, [("ask", "findById(1002)"), ("database", "ben's order, loaded"),
                                                            ("then", "owner == ana?  no"),
                                                            ("answer", code_post + "  the order exists")])]
    for i, (name, c, steps) in enumerate(lanes):
        y = 220 + i * 330
        header(d, 60, y, name, c)
        x = 60
        for j, (what, val) in enumerate(steps):
            w = 400
            panel(d, x, y + 70, w, 150, c if j == len(steps) - 1 else LINE, 3 if j == len(steps) - 1 else 2)
            label(d, x + w // 2, y + 110, what, MUTED, 22, True, "mm")
            fit_mono(d, x + w // 2, y + 165, val, TEXT if j < len(steps) - 1 else c, 26, w - 30, "mm")
            if j < len(steps) - 1:
                draw.arrow_right(d, x + w + 6, y + 145, x + w + 50, MUTED, 4)
            x += w + 56
    caption(d, "Put the owner in the query: nothing is loaded, and nothing is confirmed.", AMBER, 900)


def method_rule(img, d, cfg):
    """A nightly job calls the refund method directly; the rule on the method decides; the log keeps every refusal."""
    rule = [ln.strip() for ln in part("FOUR", "THE RULE ON THE METHOD")]
    runs = part("FOUR", "A NIGHTLY JOB")
    header(d, 60, 200, "A NIGHTLY JOB (no URL, no filter chain)", CYAN)
    for i, ln in enumerate(runs):
        who, result = ln.strip().split(": ")
        y = 270 + i * 150
        c = GREEN if result == "recorded" else ROSE
        panel(d, 60, y, 440, 110, LINE)
        label(d, 280, y + 55, who, TEXT, 26, True, "mm")
        draw.arrow_right(d, 510, y + 55, 610, MUTED, 4)
        draw.arrow_right(d, 1100, y + 55, 1180, c, 4)
        cell(d, 1190, y + 25, 220, 60, result, c, 28)
    panel(d, 620, 260, 470, 280, CYAN, 3)
    mono(d, 855, 310, rule[0].replace(" carries", ""), TEXT, 24, "mm")
    head, _, rest = rule[1].partition("(")
    mono(d, 855, 370, head + "(", CYAN, 24, "mm")
    fit_mono(d, 855, 410, rest, CYAN, 22, 440, "mm")
    label(d, 855, 480, "checked before it runs", MUTED, 22, True, "mm")
    head = heading("FOUR", "THE LOG")
    header(d, 60, 600, head.split(":")[0].replace(" (", ": ").rstrip(")"), AMBER)
    for i, ln in enumerate(part("FOUR", "THE LOG")[:4]):
        mono(d, 80, 670 + i * 40, ln.strip(), TEXT, 22)
    caption(d, "The rule travels with the method, and every refusal is one log line.", AMBER, 900)


def spring_inventory(img, d, cfg):
    """Routes read from Spring's handler mapping, the one without a rule, and the bill."""
    inv = part("FIVE", "THE ROUTE INVENTORY")
    total = int(re.search(r"serve: (\d+)", inv[0]).group(1))
    missing = re.search(r"\[(.+)\]", " ".join(inv)).group(1)
    header(d, 60, 200, "ROUTES SPRING SERVES: %d" % total, CYAN)
    for i in range(total):
        x = 60 + (i % 6) * 140
        y = 270 + (i // 6) * 130
        last = i == total - 1
        c = ROSE if last else GREEN
        d.rounded_rectangle([x, y, x + 125, y + 110], radius=12, fill=BG_CELL, outline=c, width=3)
        label(d, x + 62, y + 55, "no rule" if last else "rule", c, 22, True, "mm")
    mono(d, 60, 560, missing + "  ->  build fails", ROSE, 28)
    dave = next(ln for ln in inv if "dave" in ln).strip()
    label(d, 60, 610, dave, AMBER, 26, True)
    header(d, 1000, 200, "THE BILL", AMBER)
    for i, ln in enumerate(part("FIVE", "THE BILL")):
        panel(d, 1000, 260 + i * 90, 860, 72, LINE)
        fit_mono(d, 1020, 296 + i * 90, ln.strip(), TEXT, 22, 820, "lm")
    caption(d, "Spring lists every route, so a forgotten one fails the build.", AMBER, 880)
