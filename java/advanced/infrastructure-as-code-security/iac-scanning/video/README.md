# IaC Scanning — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `iac-scanning-explained.mp4` | the video, 1920×1080 |
| `iac-scanning-explained.m4a` | audio only |
| `iac-scanning-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Infrastructure-as-code scanning for an online store's Kubernetes manifests: eight rules written in Java and tested with their own samples, Checkov, Trivy and KICS compared on the same files, the password all three miss, exceptions with an owner and an end date, and a baseline that fails the build only on new findings.
