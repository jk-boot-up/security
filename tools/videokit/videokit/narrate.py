"""Speak narration phrase by phrase, with real silence for the pauses.

Output per scene is a 24 kHz mono WAV plus a JSON list of when each phrase
starts and ends, which is what lets the captions follow the speech exactly.

Results are cached on disk keyed by the narration text and the voice
settings, so rebuilding after editing one scene re-voices only that scene.
"""

import hashlib
import json
import re
from pathlib import Path

import numpy as np
import soundfile as sf

from . import audio, script, tts


def speak(engine, narration, sentence_gap=0.45, pause_scale=1.0):
    """Narration markup -> (samples, [{"text", "start", "end"}, ...])."""
    sr = engine.sample_rate
    parts, timings, clock = [], [], 0.0
    for p in script.phrases(narration, sentence_gap, pause_scale):
        speech = audio.trim_silence(engine.speak(script.speakable(p.text)), sr)
        start, clock = clock, clock + len(speech) / sr
        timings.append({"text": p.text, "start": round(start, 3), "end": round(clock, 3)})
        parts += [speech, np.zeros(int(p.pause * sr), dtype=np.float32)]
        clock += p.pause
    return (np.concatenate(parts) if parts else np.zeros(1, np.float32)), timings


# The voice is female and the author is male: the audio must never claim to be him.
FORBIDDEN = re.compile(r"\bI(?: am|'m)\s+Jayasekhar\b|\bmy name is Jayasekhar\b", re.I)


def check_wording(scenes):
    """Refuse narration that breaks the spec's hard wording rules."""
    bad = [s["key"] for s in scenes if FORBIDDEN.search(s["narration"])]
    if bad:
        raise ValueError("first-person credit in %s: say 'This video is presented by "
                         "Jayasekhar Konduru.'" % ", ".join(bad))


def cache_key(narration, settings):
    return hashlib.sha1((settings.voice_id() + "\n" + narration).encode()).hexdigest()[:16]


def narrate_scenes(scenes, settings, out_dir, force=False):
    """Voice every scene into out_dir as <key>.wav + <key>.json.

    Returns how many scenes were (re)voiced; the rest came from the cache.
    The engine is only loaded if something actually needs voicing.
    """
    check_wording(scenes)
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    engine, voiced = None, 0
    for scene in scenes:
        wav, meta = out_dir / (scene["key"] + ".wav"), out_dir / (scene["key"] + ".json")
        key = cache_key(scene["narration"], settings)
        if not force and wav.exists() and meta.exists() \
                and json.loads(meta.read_text()).get("cache") == key:
            continue
        engine = engine or tts.from_settings(settings)
        samples, timings = speak(engine, scene["narration"], settings.sentence_gap,
                                 settings.pause_scale)
        sf.write(wav, samples, engine.sample_rate)
        meta.write_text(json.dumps({"cache": key, "phrases": timings}, indent=1))
        voiced += 1
    return voiced


def timings(out_dir, key):
    """The phrase timings narrate_scenes() recorded for one scene, or None."""
    meta = Path(out_dir) / (key + ".json")
    return json.loads(meta.read_text())["phrases"] if meta.exists() else None
