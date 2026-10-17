"""Where one concept project's video sources and outputs live.

The only contract videokit relies on is the one every project already
follows: a `video/` directory holding `scenes.py` (a SCENES list whose items
carry `key` and `narration`) and `make_slides.py` (which renders one
`<key>.png` per scene into the directory it is given).
"""

import importlib.util
import sys
from pathlib import Path

from . import config, script

REPO = Path(__file__).resolve().parents[3]


class Project:
    def __init__(self, video_dir):
        self.video_dir = Path(video_dir).resolve()
        self.dir = self.video_dir.parent
        self.name = self.dir.name                       # e.g. password-hashing
        self.slug = self.name
        # A named version builds beside the main one and never overwrites it.
        self.version = config.load(self.video_dir).version
        tag = "-" + self.version if self.version else ""
        self.build = self.video_dir / ("build" + tag)
        stem = self.video_dir / (self.name + "-explained" + tag)
        self.mp4 = stem.with_suffix(".mp4")
        self.m4a = stem.with_suffix(".m4a")
        self.srt = stem.with_suffix(".srt")
        self.poster = self.video_dir / "poster.png"
        self._scenes = None

    @classmethod
    def locate(cls, target):
        """A project from its directory, its video/ directory, or its slug."""
        p = Path(target)
        for cand in (p / "video", p):
            if (cand / "scenes.py").exists():
                return cls(cand)
        hits = sorted(REPO.glob("*/*/*/%s/video/scenes.py" % target))
        if len(hits) != 1:
            raise SystemExit("videokit: %s project for %r" % (
                "no" if not hits else "more than one", target))
        return cls(hits[0].parent)

    def settings(self):
        return config.load(self.video_dir)

    def _module(self):
        """scenes.py, imported without clashing with other projects' copies."""
        if self._scenes is None:
            path = self.video_dir / "scenes.py"
            spec = importlib.util.spec_from_file_location("scenes_" + self.slug, path)
            mod = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(mod)
            self._scenes = mod
        return self._scenes

    @property
    def scenes(self):
        return self._module().SCENES

    @property
    def video(self):
        """Optional per-project slide settings (see videokit.slides.kinds)."""
        return getattr(self._module(), "VIDEO", None)

    def split_cues(self):
        """The project's own caption splitter when it has one.

        the YouTube document counts each scene's cues with the project's
        make_subtitles.split_cues to find chapter starts in the SRT, so the
        SRT must be cut by exactly the same function.
        """
        path = self.video_dir / "make_subtitles.py"
        if not path.exists():
            return script.split_cues
        ns = {"__name__": "videokit_project_subtitles"}
        sys.path.insert(0, str(self.video_dir))
        try:
            exec(compile(path.read_text(), str(path), "exec"), ns)
        finally:
            sys.path.pop(0)
        return ns.get("split_cues", script.split_cues)

