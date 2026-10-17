"""SRT captions, timed from the phrase timings the narrator recorded.

Cues are cut with the project's own splitter (the YouTube-doc generator
counts cues with it to find chapter starts), then each cue is placed on the
speech it carries. Pauses stay caption-free instead of being smeared across
the neighbouring lines.
"""


def _clock(phrases):
    """Map a character offset in the spoken text to seconds into the scene."""
    spans, pos = [], 0
    for p in phrases:
        spans.append((pos, pos + len(p["text"]), p["start"], p["end"]))
        pos += len(p["text"]) + 1

    def at(offset):
        for a, b, t0, t1 in spans:
            if offset <= b:
                return t0 if offset < a else t0 + (t1 - t0) * (offset - a) / max(b - a, 1)
        return spans[-1][3]

    return " ".join(p["text"] for p in phrases), at


def ts(seconds):
    ms = int(round(seconds * 1000))
    h, ms = divmod(ms, 3_600_000)
    m, ms = divmod(ms, 60_000)
    s, ms = divmod(ms, 1000)
    return "%02d:%02d:%02d,%03d" % (h, m, s, ms)


def scene_cues(cues, phrases, duration, tail_pad):
    """(start, end, text) per cue, in seconds from the start of the scene."""
    if phrases:
        text, at = _clock(phrases)
        out, pos = [], 0
        for cue in cues:
            a = text.find(cue, pos)
            a = a if a >= 0 else pos
            pos = a + len(cue)
            out.append((at(a), at(pos), cue))
        return out
    # No timings (e.g. audio made elsewhere): spread cues by length.
    speech = max(duration - tail_pad, 0.1)
    total = sum(len(c) for c in cues) or 1
    out, t = [], 0.0
    for cue in cues:
        span = speech * len(cue) / total
        out.append((t, t + span, cue))
        t += span
    return out


def build(scenes, durations, phrase_timings, split_cues, tail_pad):
    """The whole SRT. durations and phrase_timings are keyed by scene key."""
    blocks, clock = [], 0.0
    for scene in scenes:
        key = scene["key"]
        cues = split_cues(scene["narration"])
        for start, end, cue in scene_cues(cues, phrase_timings.get(key), durations[key], tail_pad):
            blocks.append("%d\n%s --> %s\n%s\n" % (len(blocks) + 1, ts(clock + start),
                                                    ts(clock + end), cue))
        clock += durations[key]
    return "\n".join(blocks)
