# Secret Scanning — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `secret-scanning-explained.mp4` | the video, 1920×1080 |
| `secret-scanning-explained.m4a` | audio only |
| `secret-scanning-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Secret scanning for a bank's git repository: a scanner written in Java next to gitleaks, files now versus the whole history, look-alikes and misses, an allowlist and a baseline, a pre-commit hook, a pre-receive hook and why a removed key must still be revoked.
