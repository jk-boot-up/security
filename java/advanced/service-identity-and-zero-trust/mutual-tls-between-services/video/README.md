# Mutual TLS Between Services — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `mutual-tls-between-services-explained.mp4` | the video, 1920×1080 |
| `mutual-tls-between-services-explained.m4a` | audio only |
| `mutual-tls-between-services-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Mutual TLS between a bank's services with step-ca and the JDK: client certificates, authorization by the identity in the certificate and not by the network, short-lived certificates renewed with the server reloading trust, and what a revoked certificate can still do.
