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

import os
import re
import textwrap

from . import draw
from .theme import (ACCENT, CYAN, AMBER, CLIENT, COMMENT, CONSOLE, GOLD, GREEN, H, LINE, MONO,
                    MONO_B, MUTED, POSTER_BOTTOM, POSTER_TOP, RED, ROSE, RULE, SANS, SANS_B,
                    TEXT, W, colour, font)

DEFAULTS = dict(
    footer=None,            # right-hand footer; default "<first title>  ·  Security"
    tagline="SECURITY BASICS  -  JAVA",
    highlight=dict(quote=[], code_return=["return"], console_amber=[], console_rose=[], console_green=[], console_cyan=[]),
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
    fnt, lh = _body_font(scene["body"], SANS, 260)
    y = 260
    for ln in scene["body"]:
        col = TEXT
        if ln.startswith("✗"):
            col = RED
        elif ln.startswith("✓"):
            col = GREEN
        elif ln.strip().startswith(tuple("1234")) and "." in ln[:6]:
            col = CLIENT
        d.text((110, y), ln, font=fnt, fill=col)
        y += lh


BODY_MAX, BODY_MIN = 52, 40     # body text size range, in 1920x1080 units


def _body_font(lines, path, top, left=110):
    """The largest body font where every line fits the width and the block fits the height."""
    for size in range(BODY_MAX, BODY_MIN - 1, -2):
        fnt, lh = font(path, size), int(size * 1.5)
        if (max((fnt.getlength(ln) for ln in lines), default=0) <= W - left - 100
                and top + lh * len(lines) <= H - 110):
            return font(path, size), lh
    return font(path, BODY_MIN), int(BODY_MIN * 1.5)


def quote(scene, img, d, cfg):
    draw.title(d, scene["title"])
    body = scene["body"]
    if isinstance(body, str):
        # A bare string would be drawn one character per line.
        raise TypeError("quote body must be a list of lines, not a string")
    d.rectangle([100, 260, 112, 860], fill=ACCENT)
    plain, lh = _body_font(body, SANS_B, 290, left=150)
    bold = font(SANS_B, plain.size - 2)
    plain = font(SANS, plain.size)
    y = 290
    for ln in body:
        col, fnt = TEXT, plain
        if _starts(ln, cfg["highlight"]["quote"]):
            col, fnt = ACCENT, bold
        d.text((150, y), ln, font=fnt, fill=col)
        y += lh


def code(scene, img, d, cfg):
    draw.title(d, scene["title"])
    lines = scene["body"].split("\n")
    top = 250
    size, lh = draw.fit_mono(lines, W - 272, (H - 110) - top, 1.52, start=44)
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


CONSOLE_DIM = (203, 213, 225)   # secondary console lines: light enough to stay sharp after YouTube's encoding
CONSOLE_MAX, CONSOLE_MIN = 48, 34   # long lines are wrapped rather than the text going below MIN


def _wrap_console(lines, size):
    """Wrap lines too wide for the console at `size`, keeping each line's indent."""
    width = int((W - 272) / (font(MONO, size).getlength("M")))
    out = []
    for ln in lines:
        if len(ln) <= width:
            out.append(ln)
            continue
        indent = len(ln) - len(ln.lstrip())
        out += textwrap.wrap(ln.strip(), width=width, initial_indent=" " * indent,
                             subsequent_indent=" " * (indent + 4)) or [ln]
    return out


def console(scene, img, d, cfg):
    draw.title(d, scene["title"])
    lines = scene["body"].split("\n")
    top = 250
    # The largest size at which the lines, wrapped to the console's width, fit the slide.
    raw = lines
    fits = False
    for size in range(CONSOLE_MAX, CONSOLE_MIN - 1, -1):
        lines = _wrap_console(raw, size)
        for ratio in (1.5, 1.32):                # tighter spacing before smaller text
            lh = int(size * ratio)
            if top + lh * len(lines) + 56 <= H - 110:
                fits = True
                break
        if fits:
            break
    if not fits:                                 # very long output: shrink below MIN only then
        size, lh = draw.fit_mono(lines, W - 272, (H - 110) - top, 1.32, start=CONSOLE_MIN, floor=24)
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
        elif _starts(ln.strip(), cfg["highlight"]["console_green"]):
            col = GREEN
        elif _starts(ln.strip(), cfg["highlight"]["console_cyan"]):
            col = CYAN
        elif ln.startswith(" "):
            col = CONSOLE_DIM
        d.text((136, y), ln, font=font(MONO, size), fill=col)
        y += lh


def figure(scene, img, d, cfg):
    """A picture drawn by the project itself: the function named in the scene's body,
    from the project's video/figures.py, draws below the title with the draw primitives."""
    import importlib.util
    draw.title(d, scene["title"])
    path = cfg["project_dir"] / "video" / "figures.py"
    spec = importlib.util.spec_from_file_location("project_figures", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    getattr(mod, scene["body"])(img, d, cfg)


def diagram(scene, img, d, cfg):
    """Placeholder: projects with diagram scenes draw them in make_slides.py."""
    draw.title(d, scene["title"])
    d.text((W // 2, 500), "(not used in this video)", font=font(SANS, 40), fill=MUTED, anchor="ma")


def poster(scene, img, d, cfg):
    """The title frame: the project's cover (the thumbnail design, with the author's
    photo) when it has cover cards; otherwise the older card from VIDEO["poster"]."""
    from . import cover
    art = cover.for_project(cfg["project_dir"], lead="Presented by ") if cfg.get("project_dir") else None
    if art is not None:
        img.paste(art)
        return
    p = cfg["poster"] or {}
    draw.gradient(img, POSTER_TOP, POSTER_BOTTOM)
    d = draw.Draw(img)
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
    draw.name_pill(d, 110, 896, lead="Presented by ")


AUTHOR_PHOTO = os.path.join(os.path.dirname(__file__), "author.jpg")


def _portrait(img, x, y, w, h, ring):
    """The author's photo in a rounded frame with a soft glow, as on the thumbnails.
    Coordinates are in 1920x1080 units; the image itself is draw.S times larger."""
    from PIL import Image, ImageDraw, ImageFilter
    k = draw.S
    X, Y, Wd, Hd = x * k, y * k, w * k, h * k
    glow = Image.new("L", img.size, 0)
    ImageDraw.Draw(glow).rounded_rectangle([X - 6 * k, Y - 6 * k, X + Wd + 6 * k, Y + Hd + 6 * k],
                                           radius=34 * k, outline=255, width=12 * k)
    glow = glow.filter(ImageFilter.GaussianBlur(22 * k))
    img.paste(Image.new("RGB", img.size, ring), (0, 0), glow)
    with Image.open(AUTHOR_PHOTO) as ph:
        photo = ph.convert("RGB").resize((Wd, Hd), Image.LANCZOS)
    mask = Image.new("L", (Wd, Hd), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, Wd - 1, Hd - 1], radius=28 * k, fill=255)
    img.paste(photo, (X, Y), mask)
    ImageDraw.Draw(img).rounded_rectangle([X - 6 * k, Y - 6 * k, X + Wd + 6 * k, Y + Hd + 6 * k],
                                          radius=34 * k, outline=ring, width=7 * k)


# The end card uses the thumbnails' cyan theme.
END_TOP, END_BOTTOM = (4, 20, 38), (2, 8, 20)
END_GREEN = (74, 222, 128)
END_INK = (6, 16, 32)


def outro(scene, img, d, cfg):
    """The end card, in the thumbnails' cyan theme: the author's photo, like,
    subscribe, share, and who made it."""
    draw.gradient(img, END_TOP, END_BOTTOM)
    # No background grid: its 1-pixel lines do not survive YouTube's encoding,
    # so the copy viewers see would differ from the slide.
    _portrait(img, 110, 190, 440, 560, END_GREEN)
    d = draw.Draw(img)
    d.rectangle([0, 0, W, 14], fill=CYAN)
    left, right = 660, W - 100                    # the column beside the photo
    mid = (left + right) // 2
    d.text((mid, 150), scene["title"],
           font=draw.fitted_font(SANS_B, scene["title"], right - left, 96, 60), fill=TEXT, anchor="ma")
    d.line([(mid - 240, 290), (mid + 240, 290)], fill=CYAN, width=6)
    labels = [("LIKE", END_GREEN), ("SUBSCRIBE", ROSE), ("SHARE", CYAN)]
    gap = 50
    box_w = (right - left - 2 * gap) // 3
    for i, (label, c) in enumerate(labels):
        x = left + i * (box_w + gap)
        d.rounded_rectangle([x, 350, x + box_w, 490], radius=24, fill=END_INK,
                            outline=c, width=5)
        d.text((x + box_w // 2, 388), label,
               font=draw.fitted_font(SANS_B, label, box_w - 40, 58, 40), fill=c, anchor="ma")
    body = scene["body"]
    fnt = font(SANS, 42)
    for size in range(42, 31, -2):
        fnt = font(SANS, size)
        if max((fnt.getlength(ln) for ln in body), default=0) <= right - left:
            break
    y = 550
    for ln in body:
        d.text((mid, y), ln, font=fnt, fill=TEXT, anchor="ma")
        y += int(fnt.size * 1.5)
    repo = _repository(cfg.get("project_dir"))
    if repo:                                      # where the code is, as in the description
        label, path = "GitHub Repo: ", repo + "/"
        lf, pf = font(SANS_B, 36), font(MONO_B, 36)
        x = mid - (lf.getlength(label) + pf.getlength(path)) / 2
        ry = max(y + 20, 800)
        d.text((x, ry), label, font=lf, fill=TEXT)
        d.text((x + lf.getlength(label), ry + 2), path, font=pf, fill=CYAN)
    draw.name_pill(d, None, 880, centre=mid, colour=CYAN, fill=END_INK, lead="Authored by ")


def _repository(pdir):
    """The repository (github.com/<owner>/<repo>) from the project's docs/youtube.md,
    without the scheme."""
    if not pdir:
        return None
    path = os.path.join(str(pdir), "docs", "youtube.md")
    if not os.path.exists(path):
        return None
    m = re.search(r"\b(github\.com/[\w.-]+/[\w.-]+)", open(path).read())
    return m.group(1) if m else None


KINDS = dict(poster=poster, outro=outro, title=title, bullets=bullets, quote=quote,
             code=code, console=console, diagram=diagram, figure=figure)
FULL_BLEED = ("title", "poster", "outro")    # no page-number footer on these
