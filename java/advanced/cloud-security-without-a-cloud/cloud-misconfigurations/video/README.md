# Cloud Misconfigurations — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cloud-misconfigurations-explained.mp4` | the video, 1920×1080 |
| `cloud-misconfigurations-explained.m4a` | audio only |
| `cloud-misconfigurations-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Cloud misconfigurations on a local AWS emulator for a bank: public buckets, a wildcard administrator policy, ports open to the world, the metadata service without a token and unencrypted disks, found by a scanner written in Java, fixed, and found again in OpenTofu files by Checkov and Trivy, with the metadata endpoint attacked and defended.
