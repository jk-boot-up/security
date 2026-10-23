# Ransomware Resilience — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `ransomware-resilience-explained.mp4` | the video, 1920×1080 |
| `ransomware-resilience-explained.m4a` | audio only |
| `ransomware-resilience-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Ransomware resilience for a bank's ledger backups in Java with the AWS SDK against RustFS: object lock in COMPLIANCE mode, versioning, separate writer and recovery credentials, an air-gapped copy, a guard with a canary file and entropy that stops poisoned backups, and a restore exercise with computed RTO and RPO.
