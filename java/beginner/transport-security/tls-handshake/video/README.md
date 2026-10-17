# The TLS Handshake — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tls-handshake-explained.mp4` | the video, 1920×1080 |
| `tls-handshake-explained.m4a` | audio only |
| `tls-handshake-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The TLS handshake in Java: how two strangers agree on a secret in public. Explained with an online store, like two people who must settle on a code word while a stranger listens. Using two real TLS engines wired together in memory, we watch a sniffer see a card number without TLS and only scrambled records with it, follow the handshake from the readable ClientHello to the encrypted rest, see a client refuse an impostor and a wrong site name, see that one changed bit in a record is refused, see that old protocol versions are switched off, and finish with the bill.
