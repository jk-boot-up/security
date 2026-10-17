"""Piper: a small open-source neural voice (MIT), one ONNX model per voice.

Voices are named like en_US-lessac-high and are downloaded on first use from
the rhasspy/piper-voices repository into $VIDEOKIT_HOME/models/piper/.
Speed maps to Piper's length_scale (1 / speed), so 0.9 is a little slower.
"""

import io
import unicodedata
import urllib.request
import wave

import numpy as np

from .kokoro import espeak_library, models_dir

BASE = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en"


def _model(voice):
    d = models_dir() / "piper"
    d.mkdir(parents=True, exist_ok=True)
    lang, name, quality = voice.split("-")
    for ext in (".onnx", ".onnx.json"):
        path = d / (voice + ext)
        if not path.exists():
            url = "%s/%s/%s/%s/%s%s" % (BASE, lang, name, quality, voice, ext)
            urllib.request.urlretrieve(url, path)
    return d / (voice + ".onnx")


class PiperEngine:
    def __init__(self, voice="en_US-lessac-high", speed=0.9, rate=None):
        from piper import PiperVoice, SynthesisConfig
        import piper.voice as pv
        from phonemizer.backend import EspeakBackend
        from phonemizer.backend.espeak.wrapper import EspeakWrapper
        from phonemizer.separator import Separator

        # The macOS piper wheel's bundled espeak looks for its data at a CI
        # build path; phonemise with the Homebrew espeak-ng instead.
        EspeakWrapper.set_library(str(espeak_library()))
        backend = EspeakBackend("en-us", preserve_punctuation=True, with_stress=True)

        class Phonemizer:
            def phonemize(self, _voice, text, vowel_clusters=None):
                ipa = backend.phonemize([text], separator=Separator(phone="", word=" "),
                                        strip=True)[0]
                return [list(unicodedata.normalize("NFD", ipa))]

        pv._ESPEAK_PHONEMIZER = Phonemizer()
        self.voice = PiperVoice.load(str(_model(voice)))
        self.config = SynthesisConfig(length_scale=1.0 / speed)
        self.sample_rate = self.voice.config.sample_rate

    def speak(self, text):
        buf = io.BytesIO()
        with wave.open(buf, "wb") as w:
            self.voice.synthesize_wav(text, w, syn_config=self.config)
        buf.seek(0)
        with wave.open(buf) as w:
            pcm = w.readframes(w.getnframes())
        return np.frombuffer(pcm, dtype=np.int16).astype(np.float32) / 32768.0
