"""The cover: the thumbnail design, also used as each video's title frame.

Dark navy with a cyan grid, a shield and padlock, the author's photo, the
title, and 3 to 5 cards naming the project's key ideas (cover_cards.py).
Laid out in 1920x1080 units and drawn at 3840x2160, like every slide.

    render(rel, headline, tagline, cards, lead)  -> the 4K image
    for_project(project_dir, lead)               -> the same, from the project's own files
"""

import random
import runpy
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

from .draw import Draw, S, gradient
from .theme import AUTHOR, H, MONO, SANS_B, W, font
from .cover_cards import CARDS

AUTHOR_PHOTO = Path(__file__).with_name("author.jpg")
GITHUB_LABEL, GITHUB = "GitHub:", "https://github.com/jk-boot-up/security/"
TAGLINES = {"beginner": "SECURITY BASICS  -  JAVA", "intermediate": "APPLIED SECURITY  -  JAVA",
            "advanced": "ADVANCED SECURITY  -  JAVA"}

TEXT = (226, 240, 250)
CYAN = (34, 211, 238)
GREEN = (74, 222, 128)
AMBER = (251, 191, 36)
ROSE = (251, 113, 133)
INK = (6, 16, 32)
GRID = (14, 42, 66)
CARD_COLOURS = (ROSE, AMBER, GREEN)


# ---------------------------------------------------------------- helpers
def fit(path, text, max_w, start, floor):
    size = start
    while font(path, size).getlength(text) > max_w and size > floor:
        size -= 2
    return font(path, size)


def glow(img, draw_fn, radius):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw_fn(Draw(layer))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius * S)))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius * S / 3)))


def shield_points(cx, top, w, h, steps=40):
    left, right = cx - w / 2, cx + w / 2
    side, tip = top + h * 0.42, top + h

    def bez(p0, p1, p2, t):
        return tuple((1 - t) ** 2 * a + 2 * (1 - t) * t * b + t ** 2 * c for a, b, c in zip(p0, p1, p2))

    pts = [bez((left, top), (cx, top + 40), (right, top), i / steps) for i in range(steps + 1)]
    pts += [bez((right, side), (right, top + h * 0.82), (cx, tip), i / steps) for i in range(steps + 1)]
    pts += [bez((cx, tip), (left, top + h * 0.82), (left, side), i / steps) for i in range(steps + 1)]
    return pts


def padlock(d, cx, cy, s, body, ink, width):
    bw, bh = s, s * 0.78
    bx, by = cx - bw / 2, cy - bh / 2 + s * 0.22
    sw = s * 0.62
    d.arc([cx - sw / 2, by - sw * 0.85, cx + sw / 2, by + sw * 0.35], 180, 360, fill=body, width=width)
    d.line([(cx - sw / 2 + width / 2 - 1, by - sw * 0.25), (cx - sw / 2 + width / 2 - 1, by)], fill=body, width=width)
    d.line([(cx + sw / 2 - width / 2, by - sw * 0.25), (cx + sw / 2 - width / 2, by)], fill=body, width=width)
    d.rounded_rectangle([bx, by, bx + bw, by + bh], radius=s * 0.12, fill=body)
    kr, ky = s * 0.1, by + bh * 0.42
    d.ellipse([cx - kr, ky - kr, cx + kr, ky + kr], fill=ink)
    d.polygon([(cx - kr * 0.55, ky), (cx + kr * 0.55, ky), (cx + kr * 0.8, ky + bh * 0.33),
               (cx - kr * 0.8, ky + bh * 0.33)], fill=ink)


def icon(d, kind, cx, cy, c):
    """Small line icons, about 110 x 90, centred on (cx, cy)."""
    if kind == "eye_off":
        d.ellipse([cx - 52, cy - 26, cx + 52, cy + 26], outline=c, width=6)
        d.ellipse([cx - 14, cy - 14, cx + 14, cy + 14], fill=c)
        d.line([(cx - 50, cy + 40), (cx + 50, cy - 40)], fill=c, width=7)
    elif kind == "seal":
        d.regular_polygon((cx, cy, 48), 8, rotation=22.5, outline=c, width=6)
        d.line([(cx - 22, cy + 2), (cx - 6, cy + 20), (cx + 24, cy - 18)], fill=c, width=8, joint="curve")
    elif kind == "check":
        d.ellipse([cx - 44, cy - 44, cx + 44, cy + 44], outline=c, width=6)
        d.line([(cx - 22, cy + 2), (cx - 6, cy + 20), (cx + 24, cy - 18)], fill=c, width=8, joint="curve")
    elif kind == "pulse":
        pts = [(-60, 0), (-28, 0), (-16, -34), (0, 34), (14, -18), (24, 0), (60, 0)]
        d.line([(cx + x, cy + y) for x, y in pts], fill=c, width=7, joint="curve")
    elif kind == "lock":
        padlock(d, cx, cy - 4, 62, c, INK, 9)
    elif kind == "key":
        d.ellipse([cx - 56, cy - 24, cx - 8, cy + 24], outline=c, width=7)
        d.line([(cx - 10, cy), (cx + 56, cy)], fill=c, width=8)
        d.line([(cx + 40, cy), (cx + 40, cy + 20)], fill=c, width=7)
        d.line([(cx + 24, cy), (cx + 24, cy + 16)], fill=c, width=7)
    elif kind == "clock":
        d.ellipse([cx - 44, cy - 44, cx + 44, cy + 44], outline=c, width=6)
        d.line([(cx, cy), (cx, cy - 28)], fill=c, width=7)
        d.line([(cx, cy), (cx + 22, cy + 10)], fill=c, width=7)
    elif kind == "user":
        d.ellipse([cx - 20, cy - 44, cx + 20, cy - 4], outline=c, width=6)
        d.arc([cx - 42, cy + 2, cx + 42, cy + 86], 180, 360, fill=c, width=6)
        d.line([(cx - 42, cy + 44), (cx + 42, cy + 44)], fill=c, width=6)
    elif kind == "doc":
        d.polygon([(cx - 34, cy - 46), (cx + 14, cy - 46), (cx + 34, cy - 26), (cx + 34, cy + 46),
                   (cx - 34, cy + 46)], outline=c, width=6)
        for dy in (-12, 6, 24):
            d.line([(cx - 20, cy + dy), (cx + 20, cy + dy)], fill=c, width=5)
    elif kind == "warn":
        d.polygon([(cx, cy - 46), (cx + 50, cy + 40), (cx - 50, cy + 40)], outline=c, width=6)
        d.line([(cx, cy - 16), (cx, cy + 14)], fill=c, width=7)
        d.ellipse([cx - 5, cy + 22, cx + 5, cy + 32], fill=c)
    elif kind == "link":
        d.rounded_rectangle([cx - 58, cy - 18, cx + 4, cy + 18], radius=18, outline=c, width=7)
        d.rounded_rectangle([cx - 4, cy - 18, cx + 58, cy + 18], radius=18, outline=c, width=7)
    elif kind == "server":
        for dy in (-40, -8, 24):
            d.rounded_rectangle([cx - 50, cy + dy, cx + 50, cy + dy + 26], radius=6, outline=c, width=5)
            d.ellipse([cx + 28, cy + dy + 9, cx + 36, cy + dy + 17], fill=c)
    elif kind == "shield":
        pts = shield_points(cx, cy - 44, 76, 90, steps=12)
        d.line(pts + [pts[0]], fill=c, width=6, joint="curve")
        d.line([(cx - 16, cy - 2), (cx - 4, cy + 12), (cx + 18, cy - 14)], fill=c, width=7, joint="curve")
    elif kind == "globe":
        d.ellipse([cx - 44, cy - 44, cx + 44, cy + 44], outline=c, width=6)
        d.ellipse([cx - 18, cy - 44, cx + 18, cy + 44], outline=c, width=5)
        d.line([(cx - 44, cy), (cx + 44, cy)], fill=c, width=5)
    elif kind == "search":
        d.ellipse([cx - 40, cy - 40, cx + 16, cy + 16], outline=c, width=7)
        d.line([(cx + 10, cy + 10), (cx + 42, cy + 42)], fill=c, width=9)
    elif kind == "bug":
        d.ellipse([cx - 24, cy - 30, cx + 24, cy + 38], outline=c, width=6)
        d.ellipse([cx - 13, cy - 46, cx + 13, cy - 24], fill=c)
        for dy in (-12, 6, 24):
            d.line([(cx - 44, cy + dy - 6), (cx - 24, cy + dy)], fill=c, width=5)
            d.line([(cx + 24, cy + dy), (cx + 44, cy + dy - 6)], fill=c, width=5)
    elif kind == "code":
        d.line([(cx - 22, cy - 30), (cx - 50, cy), (cx - 22, cy + 30)], fill=c, width=8, joint="curve")
        d.line([(cx + 22, cy - 30), (cx + 50, cy), (cx + 22, cy + 30)], fill=c, width=8, joint="curve")
        d.line([(cx + 10, cy - 38), (cx - 10, cy + 38)], fill=c, width=7)
    elif kind == "box":
        d.polygon([(cx, cy - 44), (cx + 44, cy - 22), (cx, cy), (cx - 44, cy - 22)], outline=c, width=6)
        d.line([(cx - 44, cy - 22), (cx - 44, cy + 26), (cx, cy + 48), (cx + 44, cy + 26), (cx + 44, cy - 22)],
               fill=c, width=6, joint="curve")
        d.line([(cx, cy), (cx, cy + 48)], fill=c, width=6)
    else:
        raise ValueError(kind)


def portrait(img, x, y, w, h, colour):
    with Image.open(AUTHOR_PHOTO) as ph:
        photo = ph.convert("RGB").resize((w * S, h * S), Image.LANCZOS)
    mask = Image.new("L", (w * S, h * S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w * S - 1, h * S - 1], radius=28 * S, fill=255)
    glow(img, lambda g: g.rounded_rectangle([x - 6, y - 6, x + w + 6, y + h + 6], radius=34,
                                             outline=colour + (255,), width=12), 22)
    img.paste(photo, (x * S, y * S), mask)
    Draw(img).rounded_rectangle([x - 6, y - 6, x + w + 6, y + h + 6], radius=34,
                                outline=colour, width=7)


def name_pill(d, x, y, lead):
    text = lead + AUTHOR
    fnt = font(SANS_B, 54)
    w = int(fnt.getlength(text)) + 96
    d.rounded_rectangle([x, y, x + w, y + 104], radius=52, fill=INK, outline=CYAN, width=4)
    d.text((x + w // 2, y + 22), text, font=fnt, fill=CYAN, anchor="ma")
    return x + w


# ---------------------------------------------------------------- the thumbnail
def render(slug, headline, tagline, cards, lead="by "):
    """The cover at 3840x2160 (laid out in 1920x1080 units), as an RGB image."""
    img = Image.new("RGB", (W * S, H * S))
    gradient(img, (4, 20, 38), (2, 8, 20))
    img = img.convert("RGBA")
    d = Draw(img)

    for x in range(0, W, 60):
        d.line([(x, 0), (x, H)], fill=GRID, width=1)
    for y in range(0, H, 60):
        d.line([(0, y), (W, y)], fill=GRID, width=1)
    rnd = random.Random(slug)              # a different but stable pattern per project
    bits = font(MONO, 22)
    for _ in range(120):
        x = rnd.randrange(1000, W - 20, 30)
        y = rnd.randrange(30, 540, 30)
        d.text((x, y), rnd.choice("01"), font=bits, fill=(20, 70 + rnd.randrange(40), 90))
    for _ in range(14):
        x = rnd.randrange(1020, W - 120, 60)
        y = rnd.randrange(60, 520, 60)
        length = rnd.choice((120, 180, 240))
        x2, y2 = (x + length, y) if rnd.random() < 0.5 else (x, y + length)
        d.line([(x, y), (x2, y2)], fill=(22, 90, 120), width=3)
        for px, py in ((x, y), (x2, y2)):
            d.ellipse([px - 6, py - 6, px + 6, py + 6], outline=(34, 140, 170), width=3)

    cx, top, sw, sh = 1210, 70, 380, 450
    pts = shield_points(cx, top, sw, sh)
    glow(img, lambda g: g.line(pts + [pts[0]], fill=CYAN + (255,), width=14, joint="curve"), 24)
    d = Draw(img)
    d.polygon(pts, fill=INK)
    d.line(pts + [pts[0]], fill=CYAN, width=10, joint="curve")
    inner = shield_points(cx, top + 26, sw - 60, sh - 52)
    d.line(inner + [inner[0]], fill=(22, 90, 120), width=3, joint="curve")
    padlock(d, cx, top + 190, 150, GREEN, INK, 22)

    portrait(img, 1460, 64, 340, 432, GREEN)
    d = Draw(img)

    d.rectangle([0, 0, W, 14], fill=CYAN)
    d.text((110, 88), TAGLINES[slug.split("/")[0]], font=font(SANS_B, 36), fill=GREEN)
    # Both headline lines share one size, small enough for the longer one.
    max_w = 880
    size = min(fit(SANS_B, t, max_w, 122, 52).size for t, _ in headline)
    fnt = font(SANS_B, size)
    y1 = 152 + (122 - size) // 2
    d.text((110, y1), headline[0][0], font=fnt, fill=TEXT)
    d.text((110, y1 + 136 - (122 - size) // 3), headline[1][0], font=fnt, fill=CYAN)
    d.rectangle([110, 462, 620, 472], fill=GREEN)
    # The code's home, under the rule.
    gfont = font(SANS_B, 38)
    d.text((110, 494), GITHUB_LABEL, font=gfont, fill=GREEN)
    d.text((110 + gfont.getlength(GITHUB_LABEL + " "), 494), GITHUB, font=gfont, fill=CYAN)

    n = len(cards)
    gap, y0, ch = (40 if n <= 3 else 26), 560, 280
    cw = (W - 220 - gap * (n - 1)) // n
    x0 = 110
    colours = {3: CARD_COLOURS, 4: CARD_COLOURS + (CYAN,), 5: CARD_COLOURS + (CYAN, (192, 132, 252)),
               2: CARD_COLOURS[:2]}[n]
    big = n <= 3
    for i, ((word, note, kind), c) in enumerate(zip(cards, colours)):
        x = x0 + i * (cw + gap)
        d.rounded_rectangle([x, y0, x + cw, y0 + ch], radius=24, fill=INK, outline=c, width=5)
        letter = next(ch_ for ch_ in word if ch_.isalnum()).upper()
        r = 50 if big else 40
        d.ellipse([x + 26, y0 + 30, x + 26 + 2 * r, y0 + 30 + 2 * r], fill=c)
        d.text((x + 26 + r, y0 + 30 + r), letter, font=font(SANS_B, 64 if big else 50), fill=INK, anchor="mm")
        icon(d, kind, x + cw - (90 if big else 76), y0 + 80, c)
        d.text((x + cw // 2, y0 + 158), word, font=fit(SANS_B, word, cw - 40, 50 if big else 40, 22), fill=TEXT, anchor="ma")
        d.text((x + cw // 2, y0 + 220), note, font=fit(SANS_B, note, cw - 30, 34 if big else 30, 20), fill=c, anchor="ma")

    right = name_pill(d, 110, 900, lead)
    room = W - 110 - (right + 50)
    tfont = font(SANS_B, 40)
    if tfont.getlength(tagline) <= room:
        d.text((W - 110, 930), tagline, font=tfont, fill=AMBER, anchor="ra")
    else:                                    # two lines, as large as fits
        def balanced(words, n):
            """Split words into n lines whose longest line is as short as possible."""
            if n == 1:
                return [" ".join(words)]
            best = None
            for k in range(1, len(words) - n + 2):
                rest = balanced(words[k:], n - 1)
                cand = [" ".join(words[:k])] + rest
                if best is None or max(map(len, cand)) < max(map(len, best)):
                    best = cand
            return best
        for nlines in (2, 3):
            lines = balanced(tagline.split(), nlines)
            for size in range(36, 21, -2):
                tfont = font(SANS_B, size)
                if all(tfont.getlength(ln) <= room for ln in lines):
                    break
            if all(tfont.getlength(ln) <= room for ln in lines):
                break
        for j, ln in enumerate(lines):
            d.text((W - 110, 952 - int(size * 0.62 * (len(lines) - 1)) + j * int(size * 1.25)),
                   ln, font=tfont, fill=AMBER, anchor="ra")

    return img.convert("RGB")


def poster_of(p):
    """Headline and tagline: the headline from the video settings, the tagline in full from concept.toml
    (the video settings cut long taglines at 90 characters)."""
    import ast, re, tomllib
    try:
        poster = runpy.run_path(str(p / "video" / "scenes.py"))["VIDEO"]["poster"]
    except Exception:                         # a scenes.py that does not load: read VIDEO's line alone
        line = next(l for l in open(p / "video" / "scenes.py") if l.startswith("VIDEO = "))
        poster = ast.literal_eval(line[len("VIDEO = "):])["poster"]
    tagline = poster["taglines"][0][0]
    concept = p / "concept.toml"
    if concept.exists():
        m = re.search(r'^tagline = "(.*)"$', concept.read_text(), re.M)
        full = m.group(1) if m else ""
        if full.startswith(tagline.rstrip()[:40]):
            tagline = full
    return poster["headline"], tagline


def for_project(pdir, lead="by "):
    """The cover for a project directory, or None when it has no cards."""
    pdir = Path(pdir)
    rel = "/".join(pdir.parts[-3:])
    if rel not in CARDS:
        return None
    headline, tagline = poster_of(pdir)
    return render(rel, headline, tagline, CARDS[rel], lead)
