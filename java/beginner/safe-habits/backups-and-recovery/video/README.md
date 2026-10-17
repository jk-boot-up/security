# Backups and Recovery — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `backups-and-recovery-explained.mp4` | the video, 1920×1080 |
| `backups-and-recovery-explained.m4a` | audio only |
| `backups-and-recovery-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Backups and recovery in Java: the last line of defence when everything else fails. Explained with an online store, like a fire drill, because a plan nobody has practised is only a hope. We make full and incremental backups with a manifest of SHA-256 fingerprints, catch a changed and a deleted file, show a backup that passes its checks and still cannot be restored, grade a setup against the 3-2-1 rule, prune snapshots by a retention policy, watch ransomware encrypt the shop and its mounted backup folder, restore from a store that only adds versions, work out recovery point and recovery time from a timeline, and seal the archive with AES-GCM, because a backup is a copy of every secret.
