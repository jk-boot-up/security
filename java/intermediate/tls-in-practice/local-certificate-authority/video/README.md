# A Local Certificate Authority — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `local-certificate-authority-explained.mp4` | the video, 1920×1080 |
| `local-certificate-authority-explained.m4a` | audio only |
| `local-certificate-authority-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A local certificate authority in Java with real X.509 certificates, checked by the JDK's own validator: an offline root and a constrained issuing authority, a policy the issuer applies (lifetime, names, teams), signing requests that prove key possession, a name constraint that holds even when the policy is skipped, revocation lists that only help clients that check them and fail closed when stale, and 24-hour certificates renewed by automation instead.
