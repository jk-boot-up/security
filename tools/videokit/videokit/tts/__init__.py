"""Voice engines. Each turns one phrase of text into mono float32 samples.

An engine is any object with a `sample_rate` attribute and a
`speak(text) -> numpy.ndarray` method, so adding one is a new module plus a
line in ENGINES.
"""

import importlib

ENGINES = {
    "kokoro": "videokit.tts.kokoro:KokoroEngine",   # open source, Apache 2.0
    "piper": "videokit.tts.piper:PiperEngine",      # open source, MIT
    "coqui": "videokit.tts.coqui:CoquiEngine",      # open source, MPL 2.0 (own venv)
    "say": "videokit.tts.say:SayEngine",            # macOS built-in voices
}


def get(name, voice, speed=1.0, rate=145):
    """Build the named engine."""
    if name not in ENGINES:
        raise ValueError("unknown voice engine %r (have: %s)" % (name, ", ".join(ENGINES)))
    module, cls = ENGINES[name].split(":")
    return getattr(importlib.import_module(module), cls)(voice=voice, speed=speed, rate=rate)


def from_settings(s):
    return get(s.engine, s.voice, s.speed, s.rate)
