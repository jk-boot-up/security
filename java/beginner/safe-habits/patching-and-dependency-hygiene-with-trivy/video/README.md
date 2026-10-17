# Patching and Dependency Hygiene with Trivy and Gradle — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `patching-and-dependency-hygiene-with-trivy-explained.mp4` | the video, 1920×1080 |
| `patching-and-dependency-hygiene-with-trivy-explained.m4a` | audio only |
| `patching-and-dependency-hygiene-with-trivy-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Patching and dependency hygiene with Trivy and Gradle, in Java: the tools that do the checking for you. Explained with an online store whose libraries include the famous log4j version. We pin every library with a Gradle lock file, write a CycloneDX bill of materials, let the real Trivy scanner read it in Docker, compare its report with our own checks, decide what to fix first and to which version, upgrade and see the critical problems disappear, and let Gradle's dependency verification refuse a download whose checksum was changed. We finish with the bill: a scanner is only as good as its database, and every update needs a review.
