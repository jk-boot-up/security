"""Kokoro: an 82M-parameter open-source voice model (Apache 2.0), run on ONNX.

The model files (~350 MB) are downloaded once into $VIDEOKIT_HOME/models.
Kokoro phonemises through espeak-ng; the copy bundled with the pip wheel
looks for its data at the path it was built on, so Homebrew's is used
instead (`brew install espeak-ng`), or whatever ESPEAK_LIB points at.
"""

import os
import shutil
import subprocess
import urllib.request
from pathlib import Path

import numpy as np

MODEL_URL = "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/"
MODEL_FILES = ("kokoro-v1.0.onnx", "voices-v1.0.bin")


def models_dir():
    d = Path(os.environ.get("VIDEOKIT_HOME", Path.home() / ".cache" / "videokit")) / "models"
    d.mkdir(parents=True, exist_ok=True)
    for name in MODEL_FILES:
        if not (d / name).exists():
            print("videokit: downloading %s" % name)
            tmp = d / (name + ".part")
            urllib.request.urlretrieve(MODEL_URL + name, tmp)
            tmp.rename(d / name)
    return d


def espeak_library():
    lib = os.environ.get("ESPEAK_LIB")
    if not lib and shutil.which("brew"):
        prefix = subprocess.run(["brew", "--prefix", "espeak-ng"], capture_output=True,
                                text=True).stdout.strip()
        lib = prefix and os.path.join(prefix, "lib", "libespeak-ng.dylib")
    if not lib or not os.path.exists(lib):
        raise SystemExit("videokit: espeak-ng is required -- brew install espeak-ng")
    return lib


class KokoroEngine:
    sample_rate = 24000

    def __init__(self, voice="af_bella", speed=0.9, **_):
        from kokoro_onnx import EspeakConfig, Kokoro
        lib = espeak_library()
        data = os.path.join(os.path.dirname(os.path.dirname(lib)), "share", "espeak-ng-data")
        d = models_dir()
        self.model = Kokoro(str(d / MODEL_FILES[0]), str(d / MODEL_FILES[1]),
                            espeak_config=EspeakConfig(lib_path=lib, data_path=data))
        self.voice, self.speed = voice, float(speed)
        # Voice ids start with a for American English, b for British.
        self.lang = "en-gb" if voice.startswith("b") else "en-us"

    def speak(self, text):
        samples, sr = self.model.create(text, voice=self.voice, speed=self.speed, lang=self.lang)
        assert sr == self.sample_rate
        return samples.astype(np.float32)

    def voices(self):
        return sorted(self.model.get_voices())
