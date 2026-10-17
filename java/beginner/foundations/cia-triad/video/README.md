# The CIA Triad — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cia-triad-explained.mp4` | the video, 1920×1080 |
| `cia-triad-explained.m4a` | audio only |
| `cia-triad-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The CIA triad in Java: three promises a system makes about its information. Confidentiality means only the right people can see it. Integrity means nobody can change it without being noticed. Availability means it is there when it is needed. Explained with an online store and its orders, like a bank vault with a thick door, sealed money bags and doors that open every morning. We watch a first version fail all three promises, then add a role check, a tamper seal and a rate limit, and finish with the cost: strict controls can get in the way of an emergency, so even the exception has to be logged.
