# Dependency Confusion with Gradle — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `dependency-confusion-with-gradle-explained.mp4` | the video, 1920×1080 |
| `dependency-confusion-with-gradle-explained.m4a` | audio only |
| `dependency-confusion-with-gradle-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Dependency confusion with real Gradle 9 builds against a private and a public Maven repository on loopback ports: a version range that takes the attacker's 99.0.0 from the public repository, an exact version taken from the first repository that has it, content filters that keep an internal group out of the public repository and show zero requests for it, dependency verification metadata that refuses an unlisted version and the same version with other bytes, a filter for com.shop that does not cover com.shop.tools, and checksums written from a compromised build.
