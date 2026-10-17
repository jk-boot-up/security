"""Slides: one 1920x1080 PNG per scene.

A project with its own make_slides.py (for hand-drawn diagram scenes) keeps
it and it is run as before; it can import the primitives from
videokit.slides.draw. A project without one is rendered by the standard
kinds, so a new project needs no slide code at all.
"""

import subprocess
import sys
from pathlib import Path

from . import draw, kinds


def render_standard(scenes, out_dir, video=None):
    """Render scenes with the standard kinds. Returns the PNG paths."""
    cfg = kinds.settings(video)
    right = cfg["footer"] or "%s  ·  Security" % scenes[0]["title"]
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    paths = []
    for i, scene in enumerate(scenes, start=1):
        img, d = draw.base_slide()
        kinds.KINDS[scene["kind"]](scene, img, d, cfg)
        if scene["kind"] not in kinds.FULL_BLEED:
            draw.footer(d, "%d / %d" % (i, len(scenes)), right)
        path = out_dir / (scene["key"] + ".png")
        img.save(path)
        paths.append(path)
    return paths


def render(project):
    """Render a project's slides into its build directory."""
    own = project.video_dir / "make_slides.py"
    if own.exists():
        lib = str(Path(__file__).resolve().parents[2])
        done = subprocess.run([sys.executable, own.name, str(project.build)],
                              cwd=project.video_dir, capture_output=True, text=True,
                              env={**__import__("os").environ, "PYTHONPATH": lib})
        if done.returncode:
            raise RuntimeError("make_slides.py failed:\n" + done.stderr[-2000:])
        return [project.build / (s["key"] + ".png") for s in project.scenes]
    return render_standard(project.scenes, project.build, project.video)
