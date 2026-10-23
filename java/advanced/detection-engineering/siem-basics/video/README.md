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

> SIEM basics for a bank: collecting web, database and syslog lines, dead letters for what does not parse, one common event shape, the time zone mistake that breaks a correlation, a rule that sees an attack only across sources, and turning alerts into cases.
