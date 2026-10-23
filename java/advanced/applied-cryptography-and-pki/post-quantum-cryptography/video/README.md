# Post-Quantum Cryptography — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `post-quantum-cryptography-explained.mp4` | the video, 1920×1080 |
| `post-quantum-cryptography-explained.m4a` | audio only |
| `post-quantum-cryptography-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Post-quantum cryptography in the JDK for a bank: ML-KEM and ML-DSA, a hybrid X25519 plus ML-KEM key exchange, size tables, harvest now decrypt later with Mosca's inequality, implicit rejection, an inventory of classical cryptography, and a Bouncy Castle cross-check.
