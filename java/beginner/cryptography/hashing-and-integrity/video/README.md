# Hashing and Integrity — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `hashing-and-integrity-explained.mp4` | the video, 1920×1080 |
| `hashing-and-integrity-explained.m4a` | audio only |
| `hashing-and-integrity-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Hashing and integrity in Java: how do you know data was not changed? A checksum catches accidents, a cryptographic hash such as SHA-256 gives data a fixed-size fingerprint, and a keyed hash such as HMAC proves it was made by someone holding a secret. Explained with an online store's invoices and a downloadable installer, like a wax seal on a letter: a seal anyone can copy proves nothing, and a seal made with a private ring proves a lot. We see a CRC pass a forged invoice, watch one changed digit flip about half the bits of a SHA-256, see why a hash kept beside the data protects nothing, tag invoices with HMAC, and finish with the bill: joined fields can be ambiguous, tags can be replayed, and everyone with the key can forge.
