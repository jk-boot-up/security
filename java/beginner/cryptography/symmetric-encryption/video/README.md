# Symmetric Encryption (AES-GCM) — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `symmetric-encryption-explained.mp4` | the video, 1920×1080 |
| `symmetric-encryption-explained.m4a` | audio only |
| `symmetric-encryption-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Symmetric encryption in Java: one secret key locks the data and the same key unlocks it. Explained with an online bank's account statements, like a lock box where the same key closes and opens it, and where a good lock box also shows if anyone has been at it. We store a statement in plain text, encrypt it with AES in ECB mode and see its repeated payments show through, switch to AES-GCM with a fresh nonce for every message and watch a flipped bit and a moved message get refused, show what an attacker learns from a single reused nonce, and finish with the bill: the size still shows, random nonces run out near four billion messages, and a lost key is lost data.
