# Secure Pipeline — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `secure-pipeline-explained.mp4` | the video, 1920×1080 |
| `secure-pipeline-explained.m4a` | audio only |
| `secure-pipeline-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A local release pipeline in Java for a bank's Gradle library: reproducible builds, cosign signatures, SLSA-style provenance, gates for secrets, dependencies, tests and signatures that fail closed when a tool is missing or crashes, and a deploy step that refuses forged releases.
