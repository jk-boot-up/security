"""Settings for one build: which voice, what pacing, what output format.

Defaults live here. A project may override any of them in an optional
`video/videokit.toml`, and the environment overrides both, e.g.

    VIDEOKIT_VOICE=af_heart tools/videokit/videokit.sh all <project>

videokit.toml looks like:

    [voice]
    engine = "piper"       # piper | kokoro | say
    voice  = "en_US-amy-medium"
    speed  = 0.8           # 1.0 is the voice's normal pace
    rate   = 145           # say: words per minute

    [pacing]
    sentence_gap = 0.7     # seconds of breath after each sentence
    tail_pad     = 0.9     # seconds of silence before each slide change
"""

import dataclasses
import os
import tomllib
from pathlib import Path


@dataclasses.dataclass
class Settings:
    engine: str = "piper"
    voice: str = "en_US-amy-medium"
    speed: float = 0.8
    rate: int = 145
    sentence_gap: float = 0.7
    tail_pad: float = 0.9
    fps: int = 30
    loudness: float = -16.0      # YouTube's integrated loudness target, LUFS
    true_peak: float = -1.5
    loudness_range: float = 11.0
    audio_bitrate: str = "192k"
    caption_chars: int = 84
    pause_scale: float = 1.5     # multiplies every [[slnc N]] pause
    version: str = ""            # named alternative build, e.g. amy-slow

    def voice_id(self):
        """Everything that changes the sound of the speech, for cache keys."""
        from .script import LEXICON_VERSION
        return "%s:%s:%s:%s:%s:%s:lex%d" % (self.engine, self.voice, self.speed, self.rate,
                                            self.sentence_gap, self.pause_scale, LEXICON_VERSION)


def load(video_dir=None):
    """Defaults, then the project's videokit.toml, then VIDEOKIT_* variables."""
    s = Settings()
    fields = {f.name: f.type for f in dataclasses.fields(Settings)}
    cast = {"str": str, "float": float, "int": int, str: str, float: float, int: int}

    # A version (VIDEOKIT_VERSION=amy-slow) layers video/videokit-<version>.toml
    # on top of videokit.toml and writes to separate outputs (see project.py).
    version = os.environ.get("VIDEOKIT_VERSION", "")
    names = ["videokit.toml"] + (["videokit-%s.toml" % version] if version else [])
    for name in (names if video_dir is not None else []):
        toml = Path(video_dir) / name
        if version and name != "videokit.toml" and not toml.exists():
            raise ValueError("no %s for version %r" % (toml, version))
        if toml.exists():
            for section in tomllib.loads(toml.read_text()).values():
                for k, v in (section.items() if isinstance(section, dict) else []):
                    if k not in fields:
                        raise ValueError("%s: unknown setting %r" % (toml, k))
                    setattr(s, k, cast[fields[k]](v))

    for k, t in fields.items():
        v = os.environ.get("VIDEOKIT_" + k.upper())
        if v is not None:
            setattr(s, k, cast[t](v))
    return s
