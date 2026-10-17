"""The standard slide kinds, selected by each scene's `kind`.

Per-project touches come from an optional VIDEO dict in the project's
scenes.py rather than from code, e.g.

    VIDEO = dict(
        footer="Hashing  ·  Security",
        highlight=dict(quote=["It usually wins"], console_amber=["NOTHING"]),
        poster=dict(
            headline=[("RETRY", "text"), ("WITH BACKOFF", "accent")],
            before=dict(code="call()", note="fails once, fails for good", tag="BEFORE"),
            after=dict(code="retry(call)", note="rides out a blip", tag="AFTER"),
            taglines=[("One line, three tries.", "text"), ("Real failures.", "gold")],
        ),
    )

Every key is optional; see DEFAULTS.
"""

from PIL import ImageDraw

from . import draw
from .theme import (ACCENT, AMBER, CLIENT, COMMENT, CONSOLE, GOLD, GREEN, H, LINE, MONO,
                    MONO_B, MUTED, POSTER_BOTTOM, POSTER_TOP, RED, ROSE, RULE, SANS, SANS_B,
                    TEXT, W, colour, font)

DEFAULTS = dict(
    footer=None,            # right-hand footer; default "<first title>  ·  Security"
    tagline="SECURITY BASICS  -  JAVA",
    highlight=dict(quote=[], code_return=["return"], console_amber=[], console_rose=[]),
    poster=None,
)


def settings(video):
    s = dict(DEFAULTS)
    s.update(video or {})
    s["highlight"] = {**DEFAULTS["highlight"], **(video or {}).get("highlight", {})}
    return s


def _starts(text, prefixes):
    return bool(prefixes) and text.startswith(tuple(prefixes))


def title(scene, img, d, cfg):
    d.text((W // 2, 400), scene["title"], font=font(SANS_B, 104), fill=TEXT, anchor="ma")
    y = 580
    for ln in scene["body"]:
        d.text((W // 2, y), ln, font=font(SANS, 42), fill=MUTED, anchor="ma")
        y += 72
    d.line([(W // 2 - 220, 545), (W // 2 + 220, 545)], fill=ACCENT, width=4)


def bullets(scene, img, d, cfg):
    draw.title(d, scene["title"])
    y = 260
    for ln in scene["body"]:
        col = TEXT
        if ln.startswith("✗"):
            col = RED
        elif ln.startswith("✓"):
            col = GREEN
        elif ln.strip().startswith(tuple("1234")) and "." in ln[:6]:
            col = CLIENT
        d.text((110, y), ln, font=font(SANS, 40), fill=col)
        y += 60


def quote(scene, img, d, cfg):
    draw.title(d, scene["title"])
    body = scene["body"]
    if isinstance(body, str):
        # A bare string would be drawn one character per line.
        raise TypeError("quote body must be a list of lines, not a string")
    d.rectangle([100, 260, 112, 860], fill=ACCENT)
    y = 290
    for ln in body:
        col, fnt = TEXT, font(SANS, 42)
        if _starts(ln, cfg["highlight"]["quote"]):
            col, fnt = ACCENT, font(SANS_B, 40)
        d.text((150, y), ln, font=fnt, fill=col)
        y += 62


def code(scene, img, d, cfg):
    draw.title(d, scene["title"])
    lines = scene["body"].split("\n")
    top = 250
    size, lh = draw.fit_mono(lines, W - 272, (H - 110) - top, 1.52)
    d.rounded_rectangle([100, top, W - 100, top + lh * len(lines) + 56],
                        radius=16, fill=CONSOLE, outline=LINE, width=2)
    y = top + 28
    for ln in lines:
        col, fnt, s = TEXT, font(MONO, size), ln.strip()
        if s.startswith("//"):
            col = COMMENT
        elif s.startswith("throw"):
            col = ROSE
        elif _starts(s, cfg["highlight"]["code_return"]):
            col = CLIENT
        elif s.startswith("public"):
            col, fnt = AMBER, font(MONO_B, size)
        elif s.startswith(("private", "static", "protected")):
            col = MUTED
        d.text((136, y), ln, font=fnt, fill=col)
        y += lh


def console(scene, img, d, cfg):
    draw.title(d, scene["title"])
    lines = scene["body"].split("\n")
    top = 250
    size, lh = draw.fit_mono(lines, W - 272, (H - 110) - top, 1.62, start=32)
    d.rounded_rectangle([100, top, W - 100, top + lh * len(lines) + 56],
                        radius=16, fill=CONSOLE, outline=LINE, width=2)
    y = top + 28
    for ln in lines:
        col = TEXT
        if ln.startswith("$"):
            col = MUTED
        elif _starts(ln.strip(), cfg["highlight"]["console_amber"]):
            col = AMBER
        elif _starts(ln.strip(), cfg["highlight"]["console_rose"]):
            col = ROSE
        elif ln.startswith(" "):
            col = MUTED
        d.text((136, y), ln, font=font(MONO, size), fill=col)
        y += lh


def diagram(scene, img, d, cfg):
    """Placeholder: projects with diagram scenes draw them in make_slides.py."""
    draw.title(d, scene["title"])
    d.text((W // 2, 500), "(not used in this video)", font=font(SANS, 40), fill=MUTED, anchor="ma")


def poster(scene, img, d, cfg):
    """Title card and YouTube thumbnail in one, from VIDEO["poster"]."""
    p = cfg["poster"] or {}
    draw.gradient(img, POSTER_TOP, POSTER_BOTTOM)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W, 14], fill=GOLD)
    d.text((110, 88), cfg["tagline"], font=font(SANS_B, 36), fill=GOLD)

    headline = p.get("headline") or [(scene["title"].upper(), "text")]
    for (text, c), y in zip(headline, (152, 288)):
        d.text((110, y), text, font=draw.fitted_font(SANS_B, text, W - 220, 122, 40),
               fill=colour(c))
    d.rectangle([110, 462, 620, 472], fill=RULE)
    d.rounded_rectangle([110, 516, W - 110, 866], radius=26, outline=ACCENT, width=4)

    before, after = p.get("before"), p.get("after")
    if before:
        draw.pill(d, 150, 556, 700, 170, colour(before.get("colour", "red")),
                  before["code"], before["note"], tag=before.get("tag"))
    if after:
        draw.pill(d, 1070, 556, 700, 170, colour(after.get("colour", "green")),
                  after["code"], after["note"], tag=after.get("tag"))
    if before and after:
        y = 636
        d.line([(880, y), (1010, y)], fill=TEXT, width=8)
        d.polygon([(1010, y - 22), (1010, y + 22), (1052, y)], fill=TEXT)
    for (text, c), y in zip(p.get("taglines", []), (744, 796)):
        d.text((W // 2, y), text, font=font(SANS_B, 44), fill=colour(c), anchor="ma")
    draw.name_pill(d, 110, 896)


def outro(scene, img, d, cfg):
    """The sign-off card: like, subscribe, and who made it."""
    draw.gradient(img, POSTER_BOTTOM, POSTER_TOP)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W, 14], fill=GOLD)
    d.text((W // 2, 150), scene["title"], font=font(SANS_B, 96), fill=TEXT, anchor="ma")
    d.line([(W // 2 - 240, 290), (W // 2 + 240, 290)], fill=RULE, width=6)
    labels = [("LIKE", GREEN), ("SUBSCRIBE", RED), ("SHARE", CLIENT)]
    box_w, gap = 480, 60
    left = (W - (box_w * 3 + gap * 2)) // 2
    for i, (label, c) in enumerate(labels):
        x = left + i * (box_w + gap)
        d.rounded_rectangle([x, 350, x + box_w, 490], radius=24, fill=(12, 18, 46),
                            outline=c, width=5)
        d.text((x + box_w // 2, 388), label, font=font(SANS_B, 58), fill=c, anchor="ma")
    y = 580
    for ln in scene["body"]:
        d.text((W // 2, y), ln, font=font(SANS, 42), fill=MUTED, anchor="ma")
        y += 66
    draw.name_pill(d, None, 880)


KINDS = dict(poster=poster, outro=outro, title=title, bullets=bullets, quote=quote,
             code=code, console=console, diagram=diagram)
FULL_BLEED = ("title", "poster", "outro")    # no page-number footer on these
