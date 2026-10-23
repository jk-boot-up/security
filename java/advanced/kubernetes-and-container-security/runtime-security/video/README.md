# Runtime Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `runtime-security-explained.mp4` | the video, 1920×1080 |
| `runtime-security-explained.m4a` | audio only |
| `runtime-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Runtime security for a bank's pods on a real kind cluster: Falco rules for a shell in a container, an unexpected write and an unexpected outbound connection, why Falco's driver cannot run on Docker Desktop, a Linux audit sensor in its place, and prevent versus detect.
