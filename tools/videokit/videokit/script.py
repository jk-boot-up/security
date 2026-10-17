"""The narration markup: plain sentences, plus `[[slnc N]]` pause markers.

    "Third, security. [[slnc 300]] A token is like a wristband."

Every sentence end gets a short breath; a marker replaces that breath with
N milliseconds, and may also sit mid-sentence to read a list one item at a
time. The markup is the same one macOS `say` understands, so older scripts
work unchanged.
"""

import dataclasses
import re

MARKER = re.compile(r"\[\[slnc (\d+)\]\]")
SENTENCE = re.compile(r"[^.!?]+[.!?]*")


@dataclasses.dataclass(frozen=True)
class Phrase:
    text: str
    pause: float   # seconds of silence after it


def phrases(narration, sentence_gap=0.45, pause_scale=1.0):
    """Split narration into the phrases to speak, each with its pause."""
    out = []
    tokens = MARKER.split(narration)          # text, ms, text, ms, ..., text
    for i in range(0, len(tokens), 2):
        chunk = re.sub(r"\s+", " ", tokens[i]).strip()
        marked = i + 1 < len(tokens)
        sentences = [s.strip() for s in SENTENCE.findall(chunk) if s.strip()]
        for j, s in enumerate(sentences):
            last = j == len(sentences) - 1
            gap = int(tokens[i + 1]) / 1000.0 * pause_scale if last and marked else sentence_gap
            out.append(Phrase(s, gap))
    if out:
        # The scene's closing silence is the pipeline's tail pad, not a phrase's.
        out[-1] = Phrase(out[-1].text, 0.0)
    return out


def spoken_text(narration):
    """What is actually said: the narration with the markers removed."""
    return re.sub(r"\s+", " ", MARKER.sub(" ", narration)).strip()


def split_cues(text, max_chars=84):
    """Split narration into caption-sized chunks, preferring sentence ends."""
    sentences = re.findall(r"[^.!?]+[.!?]?", spoken_text(text))
    cues, cur = [], ""
    for s in sentences:
        s = s.strip()
        if not s:
            continue
        if len(cur) + len(s) + 1 <= max_chars:
            cur = (cur + " " + s).strip()
            continue
        if cur:
            cues.append(cur)
        while len(s) > max_chars:
            cut = s.rfind(" ", 0, max_chars)
            cut = cut if cut > 0 else max_chars
            cues.append(s[:cut].strip())
            s = s[cut:].strip()
        cur = s
    if cur:
        cues.append(cur)
    return cues


# How things must be *said*, applied to each phrase just before it reaches
# the voice engine, so captions and narration.md keep the written form.
# "ID" is an identifier and must be read "I D", never as the word "id".
LEXICON_VERSION = 4
# How the author's name should sound. English voices mangle the written
# spelling (Jaya-SEK-har, kon-DUR-oo); this respelling gives the Telugu
# stress: JAH-ya-SHAY-kar KON-du-ru.
AUTHOR_SPOKEN = "Jaya Shaykar"      # first name only, by the author's choice

LEXICON = [
    (re.compile(r"\bJayasekhar(?:\s+Konduru)?\b", re.I), AUTHOR_SPOKEN),
    (re.compile(r"\bAPIs\b"), "A P eyes"),
    (re.compile(r"\b((?:[A-Z] )+)Is\b"), r"\1eyes"),      # "A P Is" -> "A P eyes"
    (re.compile(r"\bAPI\b"), "A P I"),
    (re.compile(r"\b(ID|Id|id)s\b"), "I Ds"),
    (re.compile(r"\b(ID|Id|id)\b"), "I D"),
    (re.compile(r"(?<=[a-z])Ids\b"), " I Ds"),     # orderIds -> order I Ds
    (re.compile(r"(?<=[a-z])I[Dd]\b"), " I D"),    # orderId, userID -> order I D
]


# A run of single capital letters is spelled out ("A P I", "S Q S"). Voices
# read a lone "A" as the article ("apee") and "ay" as "eye" ("ipi"); espeak
# says "eh" as the letter A, so "A P I" is spoken "eh P I".
LETTERS = re.compile(r"\b[A-Z](?: [A-Z])+\b")


def speakable(text):
    """The text as the voice should pronounce it."""
    for pattern, spoken in LEXICON:
        text = pattern.sub(spoken, text)
    return LETTERS.sub(lambda m: re.sub(r"\bA\b", "eh", m.group(0)), text)
