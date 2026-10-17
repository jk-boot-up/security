"""Audio steps: trimming, clean-up, frame alignment, joining, loudness.

Why the pipeline looks the way it does (each point was a real defect once):

* Narration stays lossless PCM until one single AAC encode at the very end.
  AAC adds priming samples to every separately encoded clip, and joining
  such clips leaves a hole at every scene change.
* Each scene is padded to a whole number of video frames, so the slide and
  its narration agree per scene and cannot drift apart over a long video.
* There is no spectral denoiser: synthesised speech has almost no noise
  floor, so afftdn ends up subtracting speech and leaves it warbling.
* Loudness is normalised once over the whole narration: measure, then one
  constant gain and a peak limiter. Per-scene levelling makes the level step
  at every join; ffmpeg's loudnorm in dynamic mode can put a gap mid-sentence.
"""

import json
import math
import os

import numpy as np

from . import ffmpeg

# Upsample cleanly to 48 kHz, drop rumble, lift consonants slightly.
CLEANUP = ("aresample=48000:filter_size=512:cutoff=0.98:linear_interp=1,"
           "highpass=f=75,equalizer=f=3000:t=q:w=1.5:g=1.5")
PCM = ["-c:a", "pcm_s16le", "-ar", "48000", "-ac", "2"]


def trim_silence(x, sr, thresh=0.01, lead=0.02, tail=0.06):
    """Cut an engine's own leading/trailing silence, keeping a little air."""
    idx = np.where(np.abs(x) > thresh)[0]
    if len(idx) == 0:
        return x
    return x[max(0, idx[0] - int(lead * sr)):min(len(x), idx[-1] + int(tail * sr))]


def prepare_scene(src, dst, tail_pad, fps):
    """Clean one scene's speech, add the tail pause, pad to a frame boundary.

    Returns the scene's length in video frames.
    """
    raw = str(dst) + ".raw.wav"
    ffmpeg.run("-i", src, "-af", "%s,apad=pad_dur=%s" % (CLEANUP, tail_pad), *PCM, raw)
    frames = math.ceil(ffmpeg.duration(raw) * fps)
    ffmpeg.run("-i", raw, "-af", "apad", "-t", frames / fps, *PCM, dst)
    os.remove(raw)
    return frames


def concat(paths, dst, list_file):
    """Losslessly join WAVs: PCM in, PCM out, so no seams."""
    with open(list_file, "w") as fh:
        fh.writelines("file '%s'\n" % p for p in paths)
    ffmpeg.run("-f", "concat", "-safe", "0", "-i", list_file, *PCM, dst)


def loudnorm_filter(src, integrated=-16.0, true_peak=-1.5, lra=11.0):
    """Measure src; return a filter that brings it to `integrated` LUFS.

    One constant gain, then a peak limiter at `true_peak`. ffmpeg's own
    loudnorm only stays linear when the gain fits under the peak ceiling;
    otherwise it silently switches to dynamic mode, which rides the level
    and can drop a hole into the timeline. Kokoro's speech is peaky enough
    to trigger that, so the gain and the limiting are done explicitly.
    """
    target = "loudnorm=I=%s:TP=%s:LRA=%s" % (integrated, true_peak, lra)
    txt = ffmpeg.stderr("-i", src, "-af", target + ":print_format=json", "-f", "null", "-")
    blob = txt[txt.rindex("{"):]
    m = json.loads(blob[:blob.index("}") + 1])
    gain = integrated - float(m["input_i"])
    limit = 10 ** (true_peak / 20)
    return ("volume=%.2fdB,alimiter=limit=%.4f:attack=5:release=50:level=0:latency=1"
            % (gain, limit))


def encode(src, dst, filters="", bitrate="192k"):
    """One AAC file from a WAV, e.g. for voice samples."""
    ffmpeg.run("-i", src, *(["-af", filters] if filters else []),
               "-c:a", "aac", "-b:a", bitrate, "-ar", "48000", "-movflags", "+faststart", dst)
