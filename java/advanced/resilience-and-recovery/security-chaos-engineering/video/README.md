# Security Chaos Engineering — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `security-chaos-engineering-explained.mp4` | the video, 1920×1080 |
| `security-chaos-engineering-explained.m4a` | audio only |
| `security-chaos-engineering-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Security chaos engineering in Java with Toxiproxy: a bank payments service whose authorization, fraud score, audit trail, rate limit and revocation check are tested with injected latency, hangs, resets, a dead dependency and a trickle, a first version that fails open, a hardened version that fails closed, alerts counted in failed probes and a scoreboard of hypotheses held and broken.
