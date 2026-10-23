# Certificate Lifecycle — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `certificate-lifecycle-explained.mp4` | the video, 1920×1080 |
| `certificate-lifecycle-explained.m4a` | audio only |
| `certificate-lifecycle-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Certificate lifecycle for a bank: a private root and intermediate with openssl, CRL, OCSP and stapling with hard fail and soft fail, short-lived certificates renewed by step-ca, the outage of a missed renewal, and an expiry monitor over a fleet.
