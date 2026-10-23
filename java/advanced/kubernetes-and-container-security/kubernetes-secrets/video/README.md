# Kubernetes Secrets — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `kubernetes-secrets-explained.mp4` | the video, 1920×1080 |
| `kubernetes-secrets-explained.m4a` | audio only |
| `kubernetes-secrets-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Kubernetes Secrets for a bank on a real kind cluster: base64 is not encryption, reading the raw value from etcd before and after encryption at rest, OpenBao with a short-lived token, and bound ServiceAccount tokens against the old token Secret.
