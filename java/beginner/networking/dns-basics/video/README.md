# DNS Basics for Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `dns-basics-explained.mp4` | the video, 1920×1080 |
| `dns-basics-explained.m4a` | audio only |
| `dns-basics-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> DNS basics for security in Java: messages, caching, and cache poisoning. Explained with an online store. We build a DNS message byte by byte with name compression, cache answers with a time to live, run a real exchange over UDP, poison a resolver's cache by guessing a transaction id, and see why random ids and ports, capped lifetimes, DNSSEC and TLS each stop a part of the attack.
