# Zero Trust Access — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `zero-trust-access-explained.mp4` | the video, 1920×1080 |
| `zero-trust-access-explained.m4a` | audio only |
| `zero-trust-access-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Zero trust access in Java with Keycloak and OPA: an identity-aware proxy that checks a JWT, asks a policy engine for a decision and writes an audit record for every request, compared with the network perimeter, plus token lifetime, revocation and what happens when a part is down.
