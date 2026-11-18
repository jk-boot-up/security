# Container Hardening — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `container-hardening-explained.mp4` | the video, 1920×1080 |
| `container-hardening-explained.m4a` | audio only |
| `container-hardening-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Container hardening in Java for a containerised web shop: an attacker model asks thirteen questions of a container's settings (read secrets, tamper with the app, become root, use the Docker socket, reach the host, exhaust memory...); docker run defaults allow 7, a tutorial configuration all 13, and one setting at a time (no host access, seccomp, no capabilities, non-root, no new privileges, read-only root, limits) brings it to 1, at the Restricted Pod Security level, printed as Docker flags and a Kubernetes securityContext.
