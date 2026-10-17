# Backups and Recovery with RustFS, an S3-Compatible Object Store — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `backups-and-recovery-with-rustfs-explained.mp4` | the video, 1920×1080 |
| `backups-and-recovery-with-rustfs-explained.m4a` | audio only |
| `backups-and-recovery-with-rustfs-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Backups and recovery with RustFS, an S3-compatible object store, in Java: the same S3 API as Amazon S3, run locally in Docker. Explained with an online store, like a safe deposit box where the courier can post envelopes through a slot but cannot open the box. We watch ransomware overwrite a file and restore the older version, lock a backup with Object Lock so that not even the root account can delete it, give the backup job an identity that can only add objects, catch a damaged upload with a SHA-256 checksum, and finish with the bill: storage grows with every version, a compliance lock cannot be undone, and the root account is the real key.
