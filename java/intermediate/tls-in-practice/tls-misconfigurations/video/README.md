# Finding TLS Misconfigurations — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tls-misconfigurations-explained.mp4` | the video, 1920×1080 |
| `tls-misconfigurations-explained.m4a` | audio only |
| `tls-misconfigurations-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A TLS configuration checker in Java for an online shop's servers: old protocols, broken and non-forward-secret cipher suites, expired, misnamed and incomplete certificate chains, weak keys and SHA-1 signatures, missing HSTS, redirects and Secure cookies, each a rule with a stable id and a severity, a report with a grade, and a move to Mozilla's intermediate policy that shows what a configuration can fix and what it cannot.
