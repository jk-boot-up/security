"""Draw the project's diagrams as PNG files from their pattern.toml description.

Three kinds, all laid out by the description rather than by an algorithm, so
the result is predictable and a cramped diagram is fixed by moving a box:

  flow      boxes on a grid (col, row) joined by labelled arrows
  class     the same, but each box lists its members in a monospace font
  sequence  participants with lifelines, messages top to bottom, and notes

    [[diagram]]
    name = "architecture-diagram"      # docs/images/<name>.png
    kind = "flow"
    title = "..."
    nodes = [{id = "a", label = "Checkout", sub = "adds the lines", col = 0, row = 0, colour = "client"}]
    edges = [{from = "a", to = "b", label = "total()", dashed = false}]

    kind = "sequence"
    participants = ["Checkout", "Cart", "Money"]
    steps = [["Checkout", "Cart", "total()"], ["Cart", "Checkout", "£28.46", "reply"],
             ["note", "Cart", "rounded once, at the end"]]
"""

import math
import textwrap

from PIL import Image, ImageDraw

from videokit.slides import theme as T

S = 2                                   # draw at twice the size, for sharp text
CELL_W, CELL_H, BOX_W = 440 * S, 200 * S, 290 * S
MARGIN, TITLE_H = 40 * S, 70 * S


def _font(bold=False, mono=False, size=20):
    path = (T.MONO_B if bold else T.MONO) if mono else (T.SANS_B if bold else T.SANS)
    return T.font(path, size * S)


def _colour(name):
    return T.colour(name or "accent")


def _text_w(d, text, font):
    return d.textlength(text, font=font)


def _box_lines(node, kind):
    sub = node.get("sub", "")
    if kind == "class":
        return list(sub) if isinstance(sub, list) else [s for s in sub.split("\n") if s]
    text = " ".join(sub) if isinstance(sub, list) else sub
    return textwrap.wrap(text, 30) if text else []


def _arrowhead(d, x1, y1, x2, y2, colour):
    a = math.atan2(y2 - y1, x2 - x1)
    L, W = 14 * S, 7 * S
    pts = [(x2, y2),
           (x2 - L * math.cos(a) + W * math.sin(a), y2 - L * math.sin(a) - W * math.cos(a)),
           (x2 - L * math.cos(a) - W * math.sin(a), y2 - L * math.sin(a) + W * math.cos(a))]
    d.polygon(pts, fill=colour)


def _line(d, x1, y1, x2, y2, colour, dashed=False, width=3):
    if not dashed:
        d.line([(x1, y1), (x2, y2)], fill=colour, width=width * S // 2 + 1)
        return
    length = math.hypot(x2 - x1, y2 - y1) or 1
    step, dash = 14 * S, 8 * S
    for t in range(0, int(length), step):
        a, b = t / length, min(t + dash, length) / length
        d.line([(x1 + (x2 - x1) * a, y1 + (y2 - y1) * a),
                (x1 + (x2 - x1) * b, y1 + (y2 - y1) * b)], fill=colour, width=width * S // 2 + 1)


def _clip(cx, cy, w, h, tx, ty):
    """Where the line from a box's centre towards (tx, ty) leaves the box."""
    dx, dy = tx - cx, ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    sx = (w / 2) / abs(dx) if dx else float("inf")
    sy = (h / 2) / abs(dy) if dy else float("inf")
    s = min(sx, sy)
    return cx + dx * s, cy + dy * s


def _label(d, x, y, text, font, colour=T.TEXT):
    w = _text_w(d, text, font)
    h = font.size
    d.rounded_rectangle([x - w / 2 - 6 * S, y - h / 2 - 4 * S, x + w / 2 + 6 * S, y + h / 2 + 6 * S],
                        radius=6 * S, fill=T.BG)
    d.text((x - w / 2, y - h / 2), text, font=font, fill=colour)


def draw_grid(spec):
    kind = spec.get("kind", "flow")
    nodes = {n["id"]: n for n in spec["nodes"]}
    cols = max(n["col"] for n in nodes.values()) + 1
    rows = max(n["row"] for n in nodes.values()) + 1
    W = MARGIN * 2 + cols * CELL_W
    H = TITLE_H + MARGIN * 2 + rows * CELL_H
    img = Image.new("RGB", (W, H), T.BG)
    d = ImageDraw.Draw(img)
    d.text((MARGIN, MARGIN // 2), spec.get("title", ""), font=_font(True, size=26), fill=T.TEXT)

    head, body = _font(True, size=20), _font(mono=(kind == "class"), size=15 if kind == "class" else 16)
    geo = {}
    for n in nodes.values():
        lines = _box_lines(n, kind)
        h = 48 * S + len(lines) * (body.size + 6 * S) + (10 * S if lines else 0)
        cx = MARGIN + n["col"] * CELL_W + CELL_W / 2
        cy = TITLE_H + MARGIN + n["row"] * CELL_H + CELL_H / 2
        geo[n["id"]] = (cx, cy, BOX_W, h, lines)

    # Trust boundaries: dashed boxes drawn behind everything, around the nodes that trust each other.
    for z in spec.get("zones", []):
        members = [geo[i] for i in z["nodes"]]
        pad = 34 * S
        x0 = min(g[0] - g[2] / 2 for g in members) - pad
        y0 = min(g[1] - g[3] / 2 for g in members) - pad - 22 * S
        x1 = max(g[0] + g[2] / 2 for g in members) + pad
        y1 = max(g[1] + g[3] / 2 for g in members) + pad
        zc = _colour(z.get("colour", "muted"))
        corners = [(x0, y0), (x1, y0), (x1, y1), (x0, y1), (x0, y0)]
        for (ax_, ay_), (bx_, by_) in zip(corners, corners[1:]):
            _line(d, ax_, ay_, bx_, by_, zc, dashed=True, width=2)
        d.text((x0 + 12 * S, y0 + 8 * S), z["label"], font=_font(True, size=14), fill=zc)

    for e in spec.get("edges", []):
        ax, ay, aw, ah, _ = geo[e["from"]]
        bx, by, bw, bh, _ = geo[e["to"]]
        x1, y1 = _clip(ax, ay, aw, ah, bx, by)
        x2, y2 = _clip(bx, by, bw, bh, ax, ay)
        colour = _colour(e.get("colour", "muted"))
        _line(d, x1, y1, x2, y2, colour, e.get("dashed", False))
        _arrowhead(d, x1, y1, x2, y2, colour)
        if e.get("label"):
            mx, my = (x1 + x2) / 2, (y1 + y2) / 2
            if abs(x2 - x1) >= abs(y2 - y1):          # mostly horizontal: above the line
                my -= 16 * S
            else:                                      # mostly vertical: beside it
                mx += _text_w(d, e["label"], _font(size=14)) / 2 + 12 * S
            _label(d, mx, my, e["label"], _font(size=14), T.MUTED)

    for nid, (cx, cy, w, h, lines) in geo.items():
        n = nodes[nid]
        c = _colour(n.get("colour"))
        x0, y0 = cx - w / 2, cy - h / 2
        shape = n.get("shape", "box")
        if shape == "ellipse":
            d.ellipse([x0 - 10 * S, y0 - 6 * S, x0 + w + 10 * S, y0 + h + 6 * S], fill=T.PANEL, outline=c, width=3 * S)
        else:
            d.rounded_rectangle([x0, y0, x0 + w, y0 + h], radius=12 * S, fill=T.PANEL, outline=c, width=3 * S)
            if shape == "store":                       # a data store: lines above and below, like an open drum
                d.line([(x0, y0 + 9 * S), (x0 + w, y0 + 9 * S)], fill=c, width=S)
                d.line([(x0, y0 + h - 9 * S), (x0 + w, y0 + h - 9 * S)], fill=c, width=S)
        tw = _text_w(d, n["label"], head)
        d.text((cx - tw / 2, y0 + 12 * S), n["label"], font=head, fill=c)
        if lines:
            if kind == "class":
                d.line([(x0, y0 + 44 * S), (x0 + w, y0 + 44 * S)], fill=c, width=S)
            for i, line in enumerate(lines):
                y = y0 + 52 * S + i * (body.size + 6 * S)
                lw = _text_w(d, line, body)
                x = x0 + 14 * S if kind == "class" else cx - lw / 2
                d.text((x, y), line, font=body, fill=T.TEXT if kind == "class" else T.MUTED)
    # Threat badges: a small rose tag per threat id (see the threat table) at the top right of a node, or by an edge label.
    badge = _font(True, size=13)
    for nid, (cx, cy, w, h, lines) in geo.items():
        tags = nodes[nid].get("threats", [])
        x = cx + w / 2 + 6 * S
        for tag in tags:
            tw = _text_w(d, tag, badge) + 14 * S
            d.rounded_rectangle([x - tw, cy - h / 2 - 12 * S, x, cy - h / 2 + 12 * S], radius=8 * S, fill=T.ROSE)
            d.text((x - tw + 7 * S, cy - h / 2 - 8 * S), tag, font=badge, fill=T.BG)
            x -= tw + 4 * S
    for e in spec.get("edges", []):
        tags = e.get("threats", [])
        if not tags:
            continue
        ax, ay, aw, ah, _ = geo[e["from"]]
        bx, by, bw, bh, _ = geo[e["to"]]
        x1, y1 = _clip(ax, ay, aw, ah, bx, by)
        x2, y2 = _clip(bx, by, bw, bh, ax, ay)
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2 + 20 * S
        text = " ".join(tags)
        tw = _text_w(d, text, badge) + 14 * S
        d.rounded_rectangle([mx - tw / 2, my - 12 * S, mx + tw / 2, my + 12 * S], radius=8 * S, fill=T.ROSE)
        d.text((mx - tw / 2 + 7 * S, my - 8 * S), text, font=badge, fill=T.BG)
    return img


def draw_sequence(spec):
    parts = spec["participants"]
    steps = spec["steps"]
    col = 260 * S
    W = MARGIN * 2 + col * len(parts)
    row = 62 * S
    H = TITLE_H + MARGIN * 2 + 70 * S + row * (len(steps) + 1)
    img = Image.new("RGB", (W, H), T.BG)
    d = ImageDraw.Draw(img)
    d.text((MARGIN, MARGIN // 2), spec.get("title", ""), font=_font(True, size=26), fill=T.TEXT)
    x = {p: MARGIN + col * i + col / 2 for i, p in enumerate(parts)}
    top = TITLE_H + MARGIN
    head = _font(True, size=17)
    for p in parts:
        w = max(_text_w(d, p, head) + 30 * S, 150 * S)
        d.rounded_rectangle([x[p] - w / 2, top, x[p] + w / 2, top + 44 * S], radius=10 * S,
                            fill=T.PANEL, outline=T.ACCENT, width=2 * S)
        d.text((x[p] - _text_w(d, p, head) / 2, top + 11 * S), p, font=head, fill=T.TEXT)
        _line(d, x[p], top + 44 * S, x[p], H - MARGIN, T.LINE, dashed=True, width=2)
    msg = _font(size=14)
    y = top + 44 * S + row * 0.8
    for s in steps:
        if s[0] == "note":
            who, text = s[1], s[2]
            w = _text_w(d, text, msg) + 24 * S
            d.rounded_rectangle([x[who] - w / 2, y - 16 * S, x[who] + w / 2, y + 18 * S], radius=6 * S,
                                fill=T.INK, outline=T.AMBER, width=S)
            d.text((x[who] - w / 2 + 12 * S, y - 9 * S), text, font=msg, fill=T.AMBER)
        elif s[0] == s[1]:
            p = s[0]
            d.line([(x[p], y), (x[p] + 50 * S, y), (x[p] + 50 * S, y + 22 * S), (x[p] + 6 * S, y + 22 * S)],
                   fill=T.CLIENT, width=2 * S)
            _arrowhead(d, x[p] + 50 * S, y + 22 * S, x[p], y + 22 * S, T.CLIENT)
            d.text((x[p] + 58 * S, y - 6 * S), s[2], font=msg, fill=T.TEXT)
        else:
            reply = len(s) > 3 and s[3] == "reply"
            colour = T.GREEN if reply else T.CLIENT
            x1, x2 = x[s[0]], x[s[1]]
            _line(d, x1, y, x2, y, colour, dashed=reply, width=2)
            _arrowhead(d, x1, y, x2, y, colour)
            _label(d, (x1 + x2) / 2, y - 14 * S, s[2], msg)
        y += row
    return img


def render(spec, out_path):
    img = draw_sequence(spec) if spec.get("kind") == "sequence" else draw_grid(spec)
    img = img.resize((img.width // S * 2 // 2, img.height // S * 2 // 2), Image.LANCZOS) \
        if img.width > 3200 else img
    img.save(out_path, optimize=True)
    return out_path
