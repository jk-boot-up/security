"""Drawing primitives: reusable by the standard slide kinds and by any
project's own custom diagram slides (`from videokit.slides.draw import box`)."""

from PIL import Image, ImageDraw

from .theme import (ACCENT, AUTHOR, BG, CLIENT, FOOTER, H, INK, LINE, MONO_B, MUTED, PANEL,
                    SANS, SANS_B, TEXT, W, font)
from .theme import font as _font


# Slides are laid out in 1920x1080 units but drawn S times larger, so text and lines
# are rendered natively at 4K instead of being stretched up from 1080p.
S = 2


def _pt(v):
    return v * S


def _xy(xy):
    """Scale a coordinate list: [(x, y), ...], [x0, y0, x1, y1] or (x, y)."""
    return [tuple(_pt(c) for c in p) if isinstance(p, (tuple, list)) else _pt(p) for p in xy]


class Draw:
    """ImageDraw in 1920x1080 units on an S-times larger image.

    Fonts are measured at their nominal size by the layout code; here each one is
    swapped for the same face at S times the size, so the text lands where it was
    laid out but with S times the detail.
    """

    def __init__(self, img):
        self._d = ImageDraw.Draw(img)

    def text(self, xy, text, font=None, **kw):
        for k in ("spacing", "stroke_width"):
            if k in kw:
                kw[k] = _pt(kw[k])
        big = _font(font.path, font.size * S) if font is not None else None
        self._d.text(tuple(_pt(c) for c in xy), text, font=big, **kw)

    def line(self, xy, fill=None, width=1, **kw):
        self._d.line(_xy(xy), fill=fill, width=_pt(width), **kw)

    def rectangle(self, xy, fill=None, outline=None, width=1):
        self._d.rectangle(_xy(xy), fill=fill, outline=outline, width=_pt(width))

    def rounded_rectangle(self, xy, radius=0, fill=None, outline=None, width=1, **kw):
        self._d.rounded_rectangle(_xy(xy), radius=_pt(radius), fill=fill, outline=outline,
                                  width=_pt(width), **kw)

    def polygon(self, xy, fill=None, outline=None, width=1):
        self._d.polygon(_xy(xy), fill=fill, outline=outline, width=_pt(width))

    def regular_polygon(self, circle, sides, rotation=0, fill=None, outline=None, width=1):
        x, y, r = circle
        self._d.regular_polygon((_pt(x), _pt(y), _pt(r)), sides, rotation=rotation,
                                fill=fill, outline=outline, width=_pt(width))

    def arc(self, xy, start, end, fill=None, width=1):
        self._d.arc(_xy(xy), start, end, fill=fill, width=_pt(width))

    def ellipse(self, xy, fill=None, outline=None, width=1):
        self._d.ellipse(_xy(xy), fill=fill, outline=outline, width=_pt(width))


def base_slide():
    img = Image.new("RGB", (W * S, H * S), BG)
    d = Draw(img)
    d.rectangle([0, 0, W, 8], fill=ACCENT)
    return img, d


def footer(d, left, right):
    d.text((70, H - 60), left, font=font(SANS, 28), fill=MUTED)
    d.text((W - 70, H - 60), right, font=font(SANS, 28), fill=MUTED, anchor="ra")


def title(d, text):
    fnt = fitted_font(SANS_B, text, W - 200, 62, 48)
    if fnt.getlength(text) <= W - 200:
        d.text((100, 92), text, font=fnt, fill=TEXT)
    else:                                       # too long for one line: two balanced lines
        words = text.split()
        cut = min(range(1, len(words)), key=lambda i: abs(len(" ".join(words[:i])) - len(" ".join(words[i:]))))
        top, bottom = " ".join(words[:cut]), " ".join(words[cut:])
        fnt = fitted_font(SANS_B, max(top, bottom, key=len), W - 200, 52, 36)
        d.text((100, 52), top, font=fnt, fill=TEXT)
        d.text((100, 52 + int(fnt.size * 1.3)), bottom, font=fnt, fill=TEXT)
    d.line([(100, 190), (W - 100, 190)], fill=LINE, width=2)


def fit_mono(lines, max_w, max_h, line_ratio, start=34, floor=16):
    """Largest mono font size where the block fits the given box."""
    for size in range(start, floor - 1, -1):
        lh = int(size * line_ratio)
        if lh * len(lines) + 56 > max_h:
            continue
        widest = max((font(MONO_B, size).getlength(ln) for ln in lines), default=0)
        if widest <= max_w:
            return size, lh
    return floor, int(floor * line_ratio)


def fitted_font(path, text, max_w, start, floor):
    fnt = font(path, start)
    while fnt.getlength(text) > max_w and fnt.size > floor:
        fnt = font(path, fnt.size - 2)
    return fnt


def box(d, x, y, w, h, label, sub, colour, label_size=30, sub_size=22):
    """A labelled component box for architecture diagrams."""
    d.rounded_rectangle([x, y, x + w, y + h], radius=14, fill=PANEL, outline=colour, width=3)
    fnt = font(SANS_B, label_size)
    while fnt.getlength(label) > w - 24 and fnt.size > 14:
        fnt = font(SANS_B, fnt.size - 2)
    sub_fnt = font(SANS, sub_size)
    while sub_fnt.getlength(sub) > w - 20 and sub_fnt.size > 12:
        sub_fnt = font(SANS, sub_fnt.size - 1)
    d.text((x + w // 2, y + h // 2 - 26), label, font=fnt, fill=TEXT, anchor="ma")
    d.text((x + w // 2, y + h // 2 + 14), sub, font=sub_fnt, fill=MUTED, anchor="ma")


def arrow(d, x1, y1, x2, y2, colour, width=3):
    """Arrow with the head at (x2, y2), pointing downwards."""
    d.line([(x1, y1), (x2, y2)], fill=colour, width=width)
    d.polygon([(x2 - 10, y2 - 16), (x2 + 10, y2 - 16), (x2, y2)], fill=colour)


def arrow_right(d, x1, y, x2, colour, width=3):
    d.line([(x1, y), (x2, y)], fill=colour, width=width)
    d.polygon([(x2 - 16, y - 10), (x2 - 16, y + 10), (x2, y)], fill=colour)


def arrow_left(d, x1, y, x2, colour, width=3):
    """Arrow with the head at (x2, y), pointing leftwards."""
    d.line([(x1, y), (x2, y)], fill=colour, width=width)
    d.polygon([(x2 + 16, y - 10), (x2 + 16, y + 10), (x2, y)], fill=colour)


def arrow_up(d, x, y1, y2, colour, width=3):
    d.line([(x, y1), (x, y2)], fill=colour, width=width)
    d.polygon([(x - 10, y2 + 16), (x + 10, y2 + 16), (x, y2)], fill=colour)


def arrow_diag_up(d, x1, y1, x2, y2, colour, width=3):
    """Arrow with the head at (x2, y2), pointing upwards along any slope."""
    d.line([(x1, y1), (x2, y2)], fill=colour, width=width)
    d.polygon([(x2 - 10, y2 + 16), (x2 + 10, y2 + 16), (x2, y2)], fill=colour)


def gradient(img, top, bottom):
    """A vertical wash, used by the poster and the sign-off card."""
    g = ImageDraw.Draw(img)                    # full resolution: one line per real pixel row
    w, h = img.size
    for y in range(h):
        t = y / (h - 1)
        g.line([(0, y), (w, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bottom)))


def pill(d, x, y, w, h, colour, code, note, tag=None):
    """A code sample in a coloured box, with a one-line verdict under it.

    `tag` puts a small chip on the top edge. Colour and the label carry the
    before/after contrast; nothing is ever struck through, because a rule
    drawn through text is illegible at YouTube thumbnail size.
    """
    d.rounded_rectangle([x, y, x + w, y + h], radius=22, fill=INK, outline=colour, width=5)
    if tag:
        tag_font = font(SANS_B, 26)
        tag_w = int(tag_font.getlength(tag)) + 44
        d.rounded_rectangle([x + 28, y - 20, x + 28 + tag_w, y + 20], radius=20, fill=colour)
        d.text((x + 28 + tag_w // 2, y - 16), tag, font=tag_font, fill=INK, anchor="ma")
    fnt = font(MONO_B, 38)
    while fnt.getlength(code) > w - 60:
        fnt = font(MONO_B, fnt.size - 2)
    d.text((x + w // 2, y + 46), code, font=fnt, fill=TEXT, anchor="ma")
    note_fnt = font(SANS_B, 34)
    while note_fnt.getlength(note) > w - 60 and note_fnt.size > 20:
        note_fnt = font(SANS_B, note_fnt.size - 1)
    d.text((x + w // 2, y + h - 58), note, font=note_fnt, fill=colour, anchor="ma")


def name_pill(d, x, y, author=AUTHOR, centre=W // 2, colour=CLIENT, fill=INK, lead="by "):
    """The author's name in a box, sized to the text. x=None centres it on `centre`."""
    text = lead + author
    fnt = font(SANS_B, 54)
    w = int(fnt.getlength(text)) + 96
    if x is None:
        x = centre - w // 2
    d.rounded_rectangle([x, y, x + w, y + 104], radius=52, fill=fill, outline=colour, width=4)
    d.text((x + w // 2, y + 22), text, font=fnt, fill=colour, anchor="ma")
