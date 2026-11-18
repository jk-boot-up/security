# Certificate Pinning and What It Costs — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `certificate-pinning-explained.mp4` | the video, 1920×1080 |
| `certificate-pinning-explained.m4a` | audio only |
| `certificate-pinning-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Certificate pinning in Java for a mobile banking app, with real certificate chains: an interceptor whose root the device trusts, an SPKI pin that refuses it, the outage when the server renews with a new key (10,000 installs fail at once), pinning an issuing CA or a leaf with a backup key, a report-only mode, and why browsers dropped server-set pins (HPKP).
