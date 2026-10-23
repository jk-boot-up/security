# Workload Identity — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `workload-identity-explained.mp4` | the video, 1920×1080 |
| `workload-identity-explained.m4a` | audio only |
| `workload-identity-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Workload identity for a bank's services with SPIFFE and SPIRE: a shared secret that leaks, SPIFFE IDs chosen by registration entries, 20 second X.509 SVIDs, mutual verification between two Java services, automatic rotation and a stolen SVID.
