# Dependency Confusion — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `dependency-confusion-explained.mp4` | the video, 1920×1080 |
| `dependency-confusion-explained.m4a` | audio only |
| `dependency-confusion-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Dependency confusion in Java, for an online store's build: an attacker registers the store's internal package name in a public registry, a resolver that takes the highest version installs it and its install script runs on the build machine, repository order fails for a new or copied name, and the fixes: names decide which repository may supply them and fail closed, a lockfile with digests, and a watch for internal names in public.
