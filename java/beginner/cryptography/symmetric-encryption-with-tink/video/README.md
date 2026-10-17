# Symmetric Encryption with Google Tink — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `symmetric-encryption-with-tink-explained.mp4` | the video, 1920×1080 |
| `symmetric-encryption-with-tink-explained.m4a` | audio only |
| `symmetric-encryption-with-tink-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Symmetric encryption with Google Tink, in Java: a library designed so that the easy way is the safe way. Explained with an online bank's account statements, like a lock box maker who picks the lock, the combination and the tamper seal for you, and offers no unsafe models. We seal a statement line with one call and watch Tink pick a fresh nonce each time, see a flipped bit and a moved message refused, rotate the key without losing old data, store the keyset encrypted under a master key, and finish with the bill: the format is Tink's own, it adds a library, and the master key still has to live somewhere.
