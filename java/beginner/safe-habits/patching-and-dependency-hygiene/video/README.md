# Patching and Dependency Hygiene — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `patching-and-dependency-hygiene-explained.mp4` | the video, 1920×1080 |
| `patching-and-dependency-hygiene-explained.m4a` | audio only |
| `patching-and-dependency-hygiene-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Patching and dependency hygiene in Java: most of a program is libraries somebody else wrote, and their bugs become yours. Explained with an online store, like a restaurant that must know every ingredient in every dish before a supplier announces a recall. We list what the store ships in a bill of materials, follow dependencies of dependencies, compare versions as numbers and match them against known problems, score each problem with the real CVSS equation, decide what to fix first by asking whether the store even uses the flawed feature, measure fix times against deadlines on a simulated clock, and pin the checksum of every artifact so a changed download is refused. All the problems in the demo are fictional.
