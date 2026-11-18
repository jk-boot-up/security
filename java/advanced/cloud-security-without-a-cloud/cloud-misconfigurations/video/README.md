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

> Cloud misconfigurations on a local AWS emulator (Moto) for a bank: public buckets, a wildcard administrator policy, ports open to the world, the instance metadata service without a token and unencrypted disks. A scanner written in Java reads the account through the AWS SDK and finds 13 findings from 9 rules; the fixes are 7 edits and 2 replacements; the same setup as OpenTofu files is scanned by Checkov and Trivy at pinned versions and compared rule by rule; server-side request forgery against IMDSv1 and IMDSv2 is shown request by request; and the emulator accepts a public policy that AWS would refuse.
