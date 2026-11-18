# SIEM Basics — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `siem-basics-explained.mp4` | the video, 1920×1080 |
| `siem-basics-explained.m4a` | audio only |
| `siem-basics-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> SIEM basics for a bank: one web, database and syslog line each turned into one common event shape, dead letters for what does not parse, one query on the common fields, the time zone offset worked out and the alert it decides, a brute-force rule that only sees the attack across sources, and two alerts grouped into one case.
