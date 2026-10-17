"""Colours, fonts and canvas size shared by every slide."""

import os

import matplotlib
from PIL import ImageFont

W, H = 1920, 1080

BG = (15, 23, 42)
PANEL = (30, 41, 59)
TEXT = (226, 232, 240)
MUTED = (148, 163, 184)
ACCENT = (167, 139, 250)
CLIENT = (56, 189, 248)
GREEN = (52, 211, 153)
AMBER = (251, 191, 36)
ROSE = (251, 113, 133)
CYAN = (34, 211, 238)
RED = (248, 113, 113)
LINE = (71, 85, 105)
FOOTER = (80, 94, 118)
INK = (12, 18, 46)          # the dark fill inside poster pills and buttons
CONSOLE = (2, 6, 23)
COMMENT = (100, 180, 130)
GOLD = AMBER
RULE = ROSE
POSTER_TOP = (49, 16, 92)
POSTER_BOTTOM = (10, 18, 58)

# Colour names usable from a project's scenes.py VIDEO settings.
NAMED = dict(text=TEXT, muted=MUTED, accent=ACCENT, client=CLIENT, green=GREEN,
             amber=AMBER, gold=GOLD, rose=ROSE, cyan=CYAN, red=RED)

AUTHOR = "Jayasekhar Konduru"

# DejaVu ships inside matplotlib, so every machine renders identical slides.
FONT_DIR = os.path.join(os.path.dirname(matplotlib.__file__), "mpl-data", "fonts", "ttf")
SANS = os.path.join(FONT_DIR, "DejaVuSans.ttf")
SANS_B = os.path.join(FONT_DIR, "DejaVuSans-Bold.ttf")
MONO = os.path.join(FONT_DIR, "DejaVuSansMono.ttf")
MONO_B = os.path.join(FONT_DIR, "DejaVuSansMono-Bold.ttf")

_fonts = {}


def font(path, size):
    if (path, size) not in _fonts:
        _fonts[(path, size)] = ImageFont.truetype(path, size)
    return _fonts[(path, size)]


def colour(c):
    """A colour given as a name from NAMED or as an RGB tuple."""
    return NAMED[c] if isinstance(c, str) else tuple(c)
