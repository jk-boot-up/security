"""Pictures for the security logging with Loki and Grafana video, drawn by the `figure` slide kind.

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


def section(name, head, n=0):
    return sections(name, head)[n]


def heading(name, head, n=0):
    return [ln for ln in act(name) if ln.startswith(head)][n].rstrip(":")


def facts(lines):
    """`key   value` lines as a dict."""
    out = {}
    for ln in lines:
        parts = re.split(r"\s{2,}", ln.strip(), maxsplit=1)
        if len(parts) == 2:
            out[parts[0]] = parts[1]
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
    while f.getlength(text) > max_w and f.size > 14:
        f = font(MONO_B, f.size - 1)
    d.text((x, y), text, font=f, fill=colour, anchor=anchor)


def marked(d, x, y, text, size, marks, base=TEXT, anchor_mid=False):
    """A mono line with every occurrence of the strings in `marks` ({text: colour}) drawn in their colour."""
    f = font(MONO_B, size)
    pattern = "|".join(re.escape(m) for m in sorted(marks, key=len, reverse=True))
    pos = 0
    for m in re.finditer(pattern, text) if marks else []:
        d.text((x, y), text[pos:m.start()], font=f, fill=base, anchor="lm" if anchor_mid else "la")
        x += f.getlength(text[pos:m.start()])
        d.text((x, y), m.group(), font=f, fill=marks[m.group()], anchor="lm" if anchor_mid else "la")
        x += f.getlength(m.group())
        pos = m.end()
    d.text((x, y), text[pos:], font=f, fill=base, anchor="lm" if anchor_mid else "la")


def caption(d, text, colour=AMBER, y=945):
    label(d, W // 2, y, text, colour, 32, True, "ma")


def cell(d, x, y, w, h, text, colour, size=26, fill=BG_CELL, text_colour=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=fill, outline=colour, width=3)
    fit_mono(d, x + w // 2, y + h // 2, text, text_colour or colour, size, w - 16, "mm")


def panel(d, x, y, w, h, colour=LINE, width=2):
    d.rounded_rectangle([x, y, x + w, y + h], radius=16, fill=PANEL, outline=colour, width=width)


# ------------------------------------------------------------------ figures

def query_parts(img, d, cfg):
    """One LogQL query, part by part: the stream selector, the parser, the filters, and what comes back."""
    q = [ln.strip() for ln in section("TWO", "QUERY 2")]
    selector = q[0]
    stages = [s.strip() for s in q[1].lstrip("| ").split("|")]
    ans = facts(section("TWO", "ITS ANSWER", 1))
    header(d, 60, 215, "ONE QUESTION: WHO MOVED 500 OR MORE?", CYAN)
    parts = [(selector, "1. pick streams by label", CYAN), (stages[0], "2. read fields", AMBER)]
    parts += [(s, "%d. filter" % (3 + i), GREEN) for i, s in enumerate(stages[1:])]
    widths = [760, 200, 340, 340]
    x = 60
    for (text, what, colour), w in zip(parts, widths):
        cell(d, x, 300, w, 70, text, colour, 28)
        label(d, x + w // 2, 390, what, colour, 24, True, "ma")
        x += w + 40
        if x < 1800:
            label(d, x - 20, 335, "|", MUTED, 40, True, "mm")
    header(d, 60, 480, "WHAT COMES BACK: ONE LINE, READ BY ITS FIELDS", GREEN)
    for k, (key, v) in enumerate(ans.items()):
        x, y = 60 + (k % 3) * 600, 560 + (k // 3) * 130
        label(d, x + 4, y, key, MUTED, 26, True)
        cell(d, x, y + 40, 560, 64, v, AMBER if key in ("actor", "source_ip") else GREEN, 30)
    caption(d, "Labels choose where to look; the line's own fields answer the question.")


def streams(img, d, cfg):
    """The push with its tenant and labels, the four streams Loki made, and a push without a tenant."""
    push = facts(section("ONE", "THE PUSH"))
    held = section("ONE", "WHAT LOKI HOLDS NOW")
    rows = [re.match(r"\s*(\{.*\})\s+(\d+) lines?", ln) for ln in held]
    rows = [(m.group(1), int(m.group(2))) for m in rows if m]
    refused = facts(section("ONE", "A PUSH WITH NO TENANT"))["answer"].split("  <-")[0]
    header(d, 60, 215, "THE PUSH", CYAN)
    for k, key in enumerate(("header", "labels", "answers")):
        y = 285 + k * 60
        label(d, 60, y + 22, key, MUTED, 28, True, "lm")
        fit_mono(d, 220, y + 22, push[key], GREEN if key == "answers" else TEXT, 28, 820, "lm")
    header(d, 1100, 215, "A PUSH WITH NO TENANT HEADER", ROSE)
    cell(d, 1100, 290, 760, 70, refused, ROSE, 32)
    label(d, 1100, 380, "Loki must know whose logs these are.", MUTED, 26)
    header(d, 60, 500, "WHAT LOKI HOLDS NOW: ONE STREAM PER LABEL SET", AMBER)
    most = max(n for _, n in rows)
    for k, (sel, n) in enumerate(rows):
        y = 575 + k * 80
        fit_mono(d, 60, y + 30, sel, CYAN, 28, 1000, "lm")
        for j in range(n):
            d.rounded_rectangle([1120 + j * 150, y, 1250 + j * 150, y + 60], radius=10, fill=BG_CELL, outline=GREEN, width=3)
            label(d, 1185 + j * 150, y + 30, "line", GREEN, 24, True, "mm")
    caption(d, "Each push names its tenant; its labels decide the stream the line joins.")


def address_story(img, d, cfg):
    """Everything from the transfer's address, in time order, and Loki's count of failures."""
    rows = [ln.split() for ln in section("TWO", "ITS ANSWER", 2) if ln.strip().startswith("req-")]
    count = facts(section("TWO", "ITS ANSWER", 3))
    query = section("TWO", "QUERY 3")[0].strip()
    header(d, 60, 215, "QUERY 3: EVERYTHING FROM THAT ADDRESS", CYAN)
    fit_mono(d, 60, 290, query, TEXT, 30, 1800)
    for k, (req, actor, kind, outcome) in enumerate(rows):
        x = 60 + k * 455
        colour = ROSE if outcome == "failure" else (ROSE if kind == "money-movement" else AMBER)
        cell(d, x, 370, 420, 80, kind, colour, 30)
        label(d, x + 210, 470, outcome, colour, 28, True, "ma")
        label(d, x + 210, 510, "%s, as %s" % (req, actor), MUTED, 24, False, "ma")
        if k:
            draw.arrow_right(d, x - 35, 410, x - 2, MUTED, 4)
    header(d, 60, 600, "QUERY 4: FAILURES BY ADDRESS, COUNTED BY LOKI", AMBER)
    for k, (ip, n) in enumerate(count.items()):
        label(d, 60, 690 + k * 60, ip, TEXT, 34, True)
        label(d, 420, 690 + k * 60, n, AMBER, 34, True)
    label(d, 60, 800, "Two failures, then a success, then the money left.", TEXT, 30, True)
    caption(d, "Someone had Ana's password: the log says it in two queries.")


def labels_vs_content(img, d, cfg):
    """150 users as a label make 150 streams; the same users inside the line make one."""
    s = facts(section("THREE", "THE STREAMS"))
    users = int(re.search(r"x (\d+) actors", s["labelled"]).group(1))
    asked = section("THREE", "ASKING FOR ONE USER")
    header(d, 60, 215, "THE USER AS A LABEL: " + s["labelled"].split("= ")[1].split(";")[0] + " STREAMS", ROSE)
    cols = 30
    for i in range(users):
        x, y = 60 + (i % cols) * 60, 285 + (i // cols) * 46
        d.rounded_rectangle([x, y, x + 50, y + 36], radius=6, fill=BG_CELL, outline=ROSE, width=2)
    fit_mono(d, 60, 530, s["labelled"], ROSE, 28, 1800)
    header(d, 60, 600, "THE USER INSIDE THE LINE: " + s["content"].split("= ")[1].split(";")[0] + " STREAM", GREEN)
    d.rounded_rectangle([60, 670, 1860, 720], radius=10, fill=BG_CELL, outline=GREEN, width=3)
    label(d, 960, 695, "%d lines, one stream" % users, GREEN, 26, True, "mm")
    fit_mono(d, 60, 745, s["content"], GREEN, 28, 1800)
    fit_mono(d, 60, 810, asked[0].strip() + asked[1].strip().replace("->", "  ->"), TEXT, 26, 1800)
    fit_mono(d, 60, 855, asked[2].strip() + asked[3].strip().replace("->", "  ->"), TEXT, 26, 1800)
    caption(d, "Every label value is a new stream and index entry: keep users in the line.")


def alert_threshold(img, d, cfg):
    """Failures per address in five minutes against the rule's threshold of three, before and after Ben's four."""
    def counts(head):
        out = []
        for ln in section("FOUR", head):
            m = re.match(r"\s*(\d+\.\d+\.\d+\.\d+)\s+(\d+) >= (\d+)\? (\w+)", ln)
            if m:
                out.append((m.group(1), int(m.group(2)), int(m.group(3)), m.group(4)))
        return out
    before, after = counts("BEFORE"), counts("AFTER")
    fired = facts(section("FOUR", "AFTER"))["firing"]
    text = section("FOUR", "AFTER")[-1].strip()
    typo = facts(section("FOUR", "THE RULE PasswordGuessingTypo"))
    threshold = after[0][2]
    for col, (title, rows, colour) in enumerate((("BEFORE", before, CYAN), ("AFTER BEN'S FOUR FAILURES", after, AMBER))):
        x0 = 60 + col * 930
        header(d, x0, 215, title, colour)
        base, unit = 640, 80
        d.line([x0, base - threshold * unit, x0 + 860, base - threshold * unit], fill=ROSE, width=3)
        label(d, x0 + 860, base - threshold * unit - 34, ">= %d fires" % threshold, ROSE, 24, True, "ra")
        for k, (ip, n, _, yes) in enumerate(rows):
            x = x0 + 40 + k * 300
            c = ROSE if yes == "yes" else GREEN
            d.rectangle([x, base - n * unit, x + 200, base], fill=c)
            label(d, x + 100, base - n * unit - 40, str(n), c, 30, True, "ma")
            label(d, x + 100, base + 14, ip, TEXT, 24, True, "ma")
        d.line([x0, base, x0 + 860, base], fill=LINE, width=3)
    label(d, 60, 720, "firing: " + fired, ROSE, 30, True)
    fit_mono(d, 60, 770, text, TEXT, 26, 1800)
    label(d, 60, 830, "The typo rule (outcom): counts " + typo["counts"].split(":")[0] + ", state "
          + typo["state"].split()[0] + ", never fires", AMBER, 28, True)
    caption(d, "A rule is code: feed it a known bad event and check that it fires.")


def tenants(img, d, cfg):
    """What another tenant, an old event, a delete and Grafana's data sources get from Loki."""
    shop = facts(section("FIVE", "ANOTHER TENANT"))
    old = facts(section("FIVE", "AN EVENT STAMPED"))
    delete = facts(section("FIVE", "ASKING LOKI TO DELETE"))
    graf = facts(section("FIVE", "GRAFANA"))
    header(d, 60, 215, heading("FIVE", "ANOTHER TENANT"), CYAN)
    cell(d, 60, 285, 860, 64, "lines " + shop["lines"], AMBER, 28)
    header(d, 1000, 215, "GRAFANA: A DATA SOURCE PER TENANT", CYAN)
    for k, t in enumerate(("bank", "shop")):
        y = 285 + k * 80
        label(d, 1000, y + 32, t, MUTED, 28, True, "lm")
        cell(d, 1100, y, 760, 64, graf[t], GREEN if t == "bank" else AMBER, 26)
    header(d, 60, 480, "AN EVENT STAMPED TEN DAYS AGO", ROSE)
    fit_mono(d, 60, 550, old["limit"], TEXT, 26, 860)
    fit_mono(d, 60, 595, old["event"], TEXT, 26, 860)
    cell(d, 60, 640, 860, 64, old["answer"].split("  <-")[0], ROSE, 28)
    header(d, 1000, 480, "ASKING LOKI TO DELETE", ROSE)
    cell(d, 1000, 550, 860, 64, delete["answer"], ROSE, 26)
    label(d, 1000, 640, section("FIVE", "ASKING LOKI TO DELETE")[1].strip(), MUTED, 26)
    job = [ln.strip() for ln in section("FIVE", "STILL THE TEAM'S JOB")]
    header(d, 60, 750, "STILL THE TEAM'S JOB", AMBER)
    for k, ln in enumerate(job):
        label(d, 60, 815 + k * 40, "- " + ln, TEXT, 26)
    caption(d, "Tenants separate data; who may use Grafana is who reads the logs.")
