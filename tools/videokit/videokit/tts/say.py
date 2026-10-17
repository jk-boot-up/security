"""macOS `say`. Kept so older videos can be rebuilt with their original voice."""

import os
import subprocess
import tempfile

import numpy as np
import soundfile as sf


class SayEngine:
    sample_rate = 24000

    def __init__(self, voice="Samantha", rate=145, **_):
        self.voice, self.rate = voice, int(rate)

    def speak(self, text):
        with tempfile.TemporaryDirectory() as tmp:
            aiff, wav = os.path.join(tmp, "s.aiff"), os.path.join(tmp, "s.wav")
            subprocess.run(["say", "-v", self.voice, "-r", str(self.rate), "-o", aiff, text],
                           check=True)
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", aiff,
                            "-ar", str(self.sample_rate), "-ac", "1", wav], check=True)
            data, _ = sf.read(wav, dtype="float32")
        return np.asarray(data, dtype=np.float32)
