# Kubernetes Network Policies — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `kubernetes-network-policies-explained.mp4` | the video, 1920×1080 |
| `kubernetes-network-policies-explained.m4a` | audio only |
| `kubernetes-network-policies-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Kubernetes network policies for a bank on a real kind cluster: default deny, one allow for each line of the design, measuring all pairs of pods against the design, the mistakes that are accepted silently, egress and DNS, and labels as the credential.
