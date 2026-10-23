# Hardware-Backed Keys — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `hardware-backed-keys-explained.mp4` | the video, 1920×1080 |
| `hardware-backed-keys-explained.m4a` | audio only |
| `hardware-backed-keys-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Hardware-backed keys for a bank: a payment-signing key generated on a SoftHSM2 token, signing from Java with PKCS#11, five failed attempts to export it, PINs, a key in a file for contrast, key wrapping, and what a compromised host can still do.
