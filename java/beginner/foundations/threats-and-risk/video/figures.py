"""Pictures for the threats and risk video, drawn by the `figure` slide kind.

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
RATING = {"LOW": GREEN, "MEDIUM": AMBER, "HIGH": ROSE}
EXPOSURE = {"public": ROSE, "authenticated": AMBER, "internal": GREEN}


def act(name):
    """The lines of one act of the output ("TWO"), without the act's own heading."""
    return re.search(r"^%s\. .*\n((?:  .*\n?)*)" % name, OUT, re.M).group(1)


def part(name, head):
    """The indented lines under one section header of an act, such as "BEFORE THE PLAN"."""
    text = re.search(r"^  %s.*\n((?:    .*\n?)*)" % re.escape(head), act(name), re.M).group(1)
    return [ln[4:] for ln in text.splitlines()]


def value(name, label):
    """The text after a label on a line of one act, such as "asset" in act TWO."""
    return re.search(r"^\s*" + re.escape(label) + r"\s+(.+)$", act(name), re.M).group(1).strip()


def header(d, x, y, text, colour, w=None):
    """A section label such as BEFORE THE RELEASE: a filled pill with dark text."""
    f = font(SANS_B, 26)
    w = w or int(f.getlength(text)) + 44
    d.rounded_rectangle([x, y, x + w, y + 44], radius=22, fill=colour)
    d.text((x + w // 2, y + 22), text, font=f, fill=(15, 23, 42), anchor="mm")
    return x + w


def label(d, x, y, text, colour=MUTED, size=30, bold=False, anchor="la"):
    d.text((x, y), text, font=font(SANS_B if bold else SANS, size), fill=colour, anchor=anchor)


def caption(d, text, colour=TEXT, y=930):
    label(d, W // 2, y, text, colour, 34, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, mono=True, width=3, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=width)
    d.text((x + w // 2, y + h // 2), text, font=font(MONO_B if mono else SANS_B, size),
           fill=text_colour or colour, anchor="mm")


def tag(d, x, y, text, colour, w=190, h=40, size=20):
    d.rounded_rectangle([x, y, x + w, y + h], radius=h // 2, fill=PANEL, outline=colour, width=2)
    d.text((x + w // 2, y + h // 2), text, font=font(SANS_B, size), fill=colour, anchor="mm")


def wrapped(d, cx, cy, text, colour, size, max_w, bold=True):
    """Centred text over one or two lines, split at the word nearest the middle."""
    f = font(SANS_B if bold else SANS, size)
    if f.getlength(text) <= max_w:
        d.text((cx, cy), text, font=f, fill=colour, anchor="mm")
        return
    words = text.split()
    cut = min(range(1, len(words)), key=lambda i: abs(len(" ".join(words[:i])) - len(" ".join(words[i:]))))
    d.text((cx, cy - size * 0.65), " ".join(words[:cut]), font=f, fill=colour, anchor="mm")
    d.text((cx, cy + size * 0.65), " ".join(words[cut:]), font=f, fill=colour, anchor="mm")


# ------------------------------------------------------------------ figures

def four_words(img, d, cfg):
    """One real risk named in its four words: a threat uses a vulnerability to harm an asset."""
    risk = part("TWO", "THE RISK")
    rid = re.search(r"THE RISK \((R\d)\)", act("TWO")).group(1)
    asset, threat, vuln = value("TWO", "asset"), value("TWO", "threat"), value("TWO", "vulnerability")
    way_in = next(ln for ln in part("ONE", "BEFORE THE RELEASE") if "/orders/" in ln).split()
    likelihood = value("TWO", "step 1  likelihood").split()[0]
    impact = value("TWO", "step 2  impact").split()[0]
    score = value("TWO", "step 3  score").split("=")[1].strip()
    rating = value("TWO", "step 4  rating").split()[-1]
    assert len(risk) == 4
    header(d, 100, 210, "THE RISK (%s), IN FOUR WORDS" % rid, AMBER)
    boxes = [("THREAT", "who or what could harm it", threat, ROSE),
             ("VULNERABILITY", "the weakness it would use", vuln, AMBER),
             ("ASSET", "what is worth protecting", asset, GREEN)]
    for i, (name, note, val, c) in enumerate(boxes):
        x = 100 + i * 610
        d.rounded_rectangle([x, 285, x + 500, 495], radius=18, fill=PANEL, outline=c, width=4)
        d.text((x + 250, 322), name, font=font(SANS_B, 34), fill=c, anchor="mm")
        d.text((x + 250, 362), note, font=font(SANS, 22), fill=MUTED, anchor="mm")
        wrapped(d, x + 250, 440, val, TEXT, 30, 460)
        if i < 2:
            draw.arrow_right(d, x + 510, 390, x + 600, MUTED, 4)
            d.text((x + 555, 352), "uses" if i == 0 else "to harm", font=font(SANS_B, 22), fill=MUTED, anchor="mm")
    # the way in, from act one's attack surface
    label(d, 855, 531, "the way in:", MUTED, 26, True, "rm")
    cell(d, 870, 508, 420, 46, "%s %s" % (way_in[1], way_in[2]), AMBER, 24)
    tag(d, 1310, 511, way_in[0], EXPOSURE[way_in[0]], w=200)
    header(d, 100, 600, "RISK: HOW LIKELY, AND HOW BAD", CYAN)
    parts = [("likelihood", likelihood, CLIENT), ("×", None, TEXT), ("impact", impact, CLIENT),
             ("=", None, TEXT), ("score", score, RATING[rating])]
    x = 100
    for name, val, c in parts:
        if val is None:
            d.text((x + 30, 735), name, font=font(SANS_B, 56), fill=c, anchor="mm")
            x += 60
            continue
        d.rounded_rectangle([x, 670, x + 260, 800], radius=16, fill=BG_CELL, outline=c, width=4)
        d.text((x + 130, 712), val, font=font(MONO_B, 54), fill=c, anchor="mm")
        d.text((x + 130, 770), name, font=font(SANS, 24), fill=MUTED, anchor="mm")
        x += 280
    draw.arrow_right(d, x + 10, 735, x + 110, RATING[rating], 4)
    cell(d, x + 120, 690, 300, 90, rating, RATING[rating], 48, mono=False)
    label(d, x + 440, 735, "rated in words", MUTED, 24, False, "lm")
    caption(d, "A weakness alone is not a risk: it needs a threat, and an asset worth the trouble.", AMBER, 880)


def surface(img, d, cfg):
    """The attack surface before and after a release: two new ways in, one open to everyone."""
    before = part("ONE", "BEFORE THE RELEASE")
    after = part("ONE", "AFTER THE RELEASE")
    old = [ln.split(None, 1) for ln in before if not ln.startswith(tuple("0123456789"))]
    new = [ln[2:].split(None, 1) for ln in after if ln.startswith("+ ")]
    old_sum = next(ln for ln in before if "entry points:" in ln)
    new_sum = next(ln for ln in after if "entry points:" in ln)

    def row(x, y, exposure, rest, bright, is_new=False):
        route, data = re.split(r"\s{2,}", rest.strip())
        c = EXPOSURE[exposure]
        d.rounded_rectangle([x, y, x + 820, y + 56], radius=10, fill=BG_CELL if bright else (18, 26, 44),
                            outline=c if is_new else LINE, width=4 if is_new else 2)
        tag(d, x + 12, y + 8, exposure, c if bright else MUTED, w=180)
        d.text((x + 210, y + 28), route, font=font(MONO_B, 25), fill=TEXT if bright else MUTED, anchor="lm")
        d.text((x + 808 - (70 if is_new else 0), y + 28), data, font=font(SANS, 22), fill=MUTED, anchor="rm")
        if is_new:
            d.text((x + 790, y + 28), "NEW", font=font(SANS_B, 20), fill=c, anchor="mm")

    header(d, 100, 205, "BEFORE THE RELEASE", GREEN)
    header(d, 1000, 205, "AFTER THE RELEASE", ROSE)
    for i, (exposure, rest) in enumerate(old):
        row(100, 268 + i * 66, exposure, rest, True)
        row(1000, 268 + i * 66, exposure, rest, False)
    for j, (exposure, rest) in enumerate(new):
        row(1000, 268 + (len(old) + j) * 66, exposure, rest, True, True)
    draw.arrow_right(d, 930, 470, 990, MUTED, 4)
    label(d, 100, 870, old_sum, TEXT, 26, True)
    label(d, 1000, 870, new_sum, TEXT, 26, True)
    last = [ln for ln in act("ONE").splitlines() if "<-" in ln][0].split("<-")[0].strip()
    caption(d, "Nobody decided to add the debug page. Count again: " + last + ".", AMBER, 930)


def score_steps(img, d, cfg):
    """How the score is made: likelihood, impact, multiply, then find the band."""
    rid = re.search(r"THE RISK \((R\d)\)", act("TWO")).group(1)
    steps = part("TWO", "THE SCORE")
    likelihood = int(value("TWO", "step 1  likelihood").split()[0])
    impact = int(value("TWO", "step 2  impact").split()[0])
    rating = value("TWO", "step 4  rating").split()[-1]
    bands = re.findall(r"(LOW|MEDIUM|HIGH) (\d+) to (\d+)", value("TWO", "bands:"))
    band_of = lambda s: next(n for n, lo, hi in bands if int(lo) <= s <= int(hi))

    header(d, 100, 205, "THE RISK (%s)" % rid, AMBER)
    label(d, 370, 227, "%s: %s" % (value("TWO", "asset"), value("TWO", "vulnerability")), TEXT, 26, False, "lm")
    header(d, 100, 290, "THE SCORE, STEP BY STEP", CYAN)
    notes = ["how likely is it?", "how bad would it be?", "multiply the two", "find its band"]
    for i, ln in enumerate(steps):
        y = 355 + i * 112
        m = re.match(r"(step \d)\s+(\w+)\s+(.+)$", ln)
        c = RATING[rating] if i >= 2 else CLIENT
        cell(d, 100, y, 150, 90, m.group(1), MUTED, 26, mono=False)
        label(d, 280, y + 22, m.group(2), c, 30, True, "lm")
        label(d, 280, y + 64, notes[i], MUTED, 22, False, "lm")
        d.rounded_rectangle([560, y, 1040, y + 90], radius=12, fill=BG_CELL, outline=c, width=3)
        d.text((800, y + 45), m.group(3), font=font(MONO_B, 28), fill=c, anchor="mm")

    # the grid of every likelihood against every impact
    x0, y0, cw, ch = 1180, 330, 124, 92
    label(d, x0 + 2.5 * cw, 268, "every score: likelihood × impact", TEXT, 26, True, "ma")
    for li in range(5, 0, -1):
        y = y0 + (5 - li) * ch
        label(d, x0 - 24, y + ch // 2, str(li), MUTED, 26, True, "rm")
        for im in range(1, 6):
            x = x0 + (im - 1) * cw
            s = li * im
            c = RATING[band_of(s)]
            hit = li == likelihood and im == impact
            d.rounded_rectangle([x + 4, y + 4, x + cw - 4, y + ch - 4], radius=8,
                                fill=c if hit else BG_CELL, outline=c, width=5 if hit else 2)
            d.text((x + cw // 2, y + ch // 2), str(s), font=font(MONO_B, 30 if hit else 26),
                   fill=(15, 23, 42) if hit else c, anchor="mm")
    for im in range(1, 6):
        label(d, x0 + (im - 1) * cw + cw // 2, y0 + 5 * ch + 8, str(im), MUTED, 26, True, "ma")
    label(d, x0 + 2.5 * cw, y0 + 5 * ch + 48, "impact →", MUTED, 24, False, "ma")
    label(d, x0 - 10, y0 - 22, "likelihood ↑", MUTED, 24, False, "rm")
    x = 100
    for name, lo, hi in bands:
        x = header(d, x, 820, "%s  %s to %s" % (name, lo, hi), RATING[name]) + 20
    caption(d, "%s sits at likelihood %d, impact %d: score %d, %s." % (rid, likelihood, impact,
                                                                       likelihood * impact, rating), AMBER, 900)


def ranking(img, d, cfg):
    """The register in the order it was written, then sorted with a fixed rule for ties."""
    before = [re.match(r"(R\d)\s+(\d) x (\d) =\s+(\d+)\s+(.+)$", ln).groups()
              for ln in part("THREE", "BEFORE RANKING")]
    after = [re.match(r"(\d)\. (R\d)\s+(\d+) (\w+)\s+(.+)$", ln).groups()
             for ln in part("THREE", "AFTER RANKING")]
    tie = value("THREE", "tie:")
    total = value("THREE", "total score")
    rating = {rid: r for _, rid, _, r, _ in after}
    e = header(d, 100, 205, "BEFORE RANKING", GREEN)
    label(d, e + 24, 227, "in the order they were written", MUTED, 24, False, "lm")
    e = header(d, 1080, 205, "AFTER RANKING", ROSE)
    label(d, e + 24, 227, "highest score first", MUTED, 24, False, "lm")
    pos = {}
    for i, (rid, li, im, s, threat) in enumerate(before):
        y = 272 + i * 80
        c = RATING[rating[rid]]
        d.rounded_rectangle([100, y, 840, y + 64], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((122, y + 32), "%s  %s × %s = %2s" % (rid, li, im, s), font=font(MONO_B, 26), fill=c, anchor="lm")
        d.text((828, y + 32), threat, font=font(SANS, 22), fill=MUTED, anchor="rm")
        pos[rid] = y + 32
    for i, (n, rid, s, r, threat) in enumerate(after):
        y = 272 + i * 80
        c = RATING[r]
        d.line([(850, pos[rid]), (1070, y + 32)], fill=c, width=3)
        d.rounded_rectangle([1080, y, 1820, y + 64], radius=10, fill=BG_CELL, outline=c, width=3)
        d.text((1102, y + 32), "%s. %s %2s" % (n, rid, s), font=font(MONO_B, 26), fill=c, anchor="lm")
        tag(d, 1290, y + 13, r, c, w=130, h=38, size=20)
        d.text((1808, y + 32), threat, font=font(SANS, 22), fill=MUTED, anchor="rm")
    label(d, W // 2, 845, "tie: " + tie, TEXT, 28, True, "ma")
    caption(d, "Total score %s: one number for the whole list, to bring down." % total, AMBER, 905)


def budget(img, d, cfg):
    """Value = score removed / cost; the budget buys the best value first."""
    opts = [re.match(r"(R\d) (\w+)\s+(\d+) -> (\d+)\s+removes\s+(\d+) / cost (\d) =\s+([\d.]+)$", ln).groups()
            for ln in part("FOUR", "BEFORE THE PLAN")]
    plan = part("FOUR", "AFTER THE PLAN")
    funded = [re.match(r"funded\s+(R\d) cost (\d)", ln).groups() for ln in plan if ln.startswith("funded")]
    waiting = [re.match(r"waiting\s+(R\d) cost (\d), (.+?):", ln).groups() for ln in plan if ln.startswith("waiting")]
    spent = next(ln for ln in plan if ln.startswith("spent"))
    total = next(ln for ln in plan if ln.startswith("total score")).replace("->", "→")
    budget_n = int(spent.split()[-1])
    strategy = {o[0]: o[1] for o in opts}

    e = header(d, 100, 205, "BEFORE THE PLAN", GREEN)
    label(d, e + 24, 227, "value = score removed / cost", TEXT, 26, True, "lm")
    for i, (rid, strat, now, left, removed, cost, val) in enumerate(opts):
        y = 265 + i * 64
        cell(d, 100, y, 330, 52, "%s %s" % (rid, strat), CLIENT, 24)
        cell(d, 450, y, 220, 52, "%s → %s" % (now, left), MUTED, 24)
        cell(d, 690, y, 260, 52, "removes %s" % removed, GREEN, 24)
        d.text((985, y + 26), "÷", font=font(SANS_B, 36), fill=TEXT, anchor="mm")
        cell(d, 1020, y, 200, 52, "cost %s" % cost, AMBER, 24)
        d.text((1255, y + 26), "=", font=font(SANS_B, 36), fill=TEXT, anchor="mm")
        cell(d, 1290, y, 180, 52, val, CYAN, 26)
        if i == 0:
            label(d, 1500, y + 26, "← best value first", CYAN, 24, True, "lm")

    e = header(d, 100, 600, "AFTER THE PLAN", ROSE)
    label(d, e + 24, 622, "a budget of %d, spent best value first" % budget_n, TEXT, 26, True, "lm")
    unit, x0, y = 120, 100, 675
    x = x0
    for rid, cost in funded:
        w = int(cost) * unit
        cell(d, x, y, w - 6, 70, rid if int(cost) == 1 else "%s  %s" % (rid, strategy[rid]), GREEN, 22, False)
        x += w
    left = budget_n * unit - (x - x0)
    if left > 0:
        d.rounded_rectangle([x, y, x + left - 6, y + 70], radius=10, outline=LINE, width=2)
    edge = x0 + budget_n * unit
    d.line([(edge, y - 18), (edge, y + 175)], fill=ROSE, width=4)
    label(d, edge, y - 22, "budget %d" % budget_n, ROSE, 22, True, "md")
    for rid, cost, why in waiting:
        w = int(cost) * unit
        d.rounded_rectangle([x, y + 92, x + w - 6, y + 162], radius=10, fill=BG_CELL, outline=ROSE, width=3)
        d.text((x + (w - 6) // 2, y + 127), "%s %s  cost %s" % (rid, strategy[rid], cost), font=font(SANS_B, 22),
               fill=ROSE, anchor="mm")
        label(d, x - 20, y + 127, "%s waits: %s" % (rid, why), ROSE, 24, True, "rm")
    label(d, 1360, y + 18, spent, TEXT, 28, True)
    label(d, 1360, y + 52, total, GREEN, 28, True)
    caption(d, "The most score removed for each unit spent, until the budget runs out.", AMBER, 905)


def judgment(img, d, cfg):
    """Two reviewers, the same rules, different estimates, a different top three."""
    reviews = [re.match(r"(R\d)\s+engineering (\d) x (\d) =\s+(\d+)\s+finance (\d) x (\d) =\s+(\d+)$", ln).groups()
               for ln in part("FIVE", "THE TWO REVIEWS")]
    tops = {}
    for ln in part("FIVE", "THE TWO RANKINGS"):
        who, rest = ln.split(" top three:")
        tops[who] = re.findall(r"(R\d) \((\d+)\)", rest)
    accepted = re.search(r"^  accepted (.+?):", act("FIVE"), re.M).group(1)
    e = header(d, 100, 205, "THE TWO REVIEWS", AMBER)
    label(d, e + 24, 227, "same rules, different estimates", TEXT, 26, True, "lm")
    for j, (who, c) in enumerate((("engineering", CLIENT), ("finance", AMBER))):
        label(d, 520 + j * 520 + 220, 290, who, c, 30, True, "ma")
    for i, (rid, el, ei, es, fl, fi, fs) in enumerate(reviews):
        y = 340 + i * 90
        cell(d, 100, y, 340, 70, rid, TEXT, 30)
        cell(d, 520, y, 440, 70, "%s × %s = %s" % (el, ei, es), CLIENT, 30)
        cell(d, 1040, y, 440, 70, "%s × %s = %s" % (fl, fi, fs), AMBER, 30)
    e = header(d, 100, 545, "THE TWO RANKINGS", CYAN)
    label(d, e + 24, 567, "top three", TEXT, 26, True, "lm")
    eng, fin = [r for r, _ in tops["engineering"]], [r for r, _ in tops["finance"]]
    for j, (who, c) in enumerate((("engineering", CLIENT), ("finance", AMBER))):
        label(d, 100, 640 + j * 100, who, c, 30, True, "lm")
        other = fin if who == "engineering" else eng
        for k, (rid, s) in enumerate(tops[who]):
            x = 420 + k * 300
            diff = rid not in other
            cell(d, x, 605 + j * 100, 270, 70, "%d. %s (%s)" % (k + 1, rid, s), ROSE if diff else c, 28,
                 width=5 if diff else 3)
    label(d, 1340, 640, "different", ROSE, 28, True, "lm")
    label(d, 1340, 680, "in rose", ROSE, 24, False, "lm")
    caption(d, "Same rules, different ranking: write down who scored what, and why.", AMBER, 850)
    label(d, W // 2, 910, "Accepted risks %s: each needs an owner and a review date." % accepted, MUTED, 30, False, "ma")
