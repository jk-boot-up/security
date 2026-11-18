# Detecting Attacks in Logs — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `detecting-attacks-explained.mp4` | the video, 1920×1080 |
| `detecting-attacks-explained.m4a` | audio only |
| `detecting-attacks-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Detecting attacks in a bank's sign-in and admin logs, in Java: five detection rules (brute force, password spraying, impossible travel, success after failures, privilege change) run over one synthetic Monday with four hidden attacks, measured against ground truth: a first version raises 29 alerts, most false; tuning thresholds and adding context such as the VPN's addresses brings it to 9 alerts, 7 real, with every attack noticed.
