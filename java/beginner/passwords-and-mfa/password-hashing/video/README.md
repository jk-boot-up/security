# Password Hashing — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `password-hashing-explained.mp4` | the video, 1920×1080 |
| `password-hashing-explained.m4a` | audio only |
| `password-hashing-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Password hashing in Java: a shop should never store passwords as they are. Store a hash, which is a one-way scramble, add a salt so identical passwords look different, and make the hash slow so every guess costs real work. Explained with an online store's customer accounts, like a safe that is hard and slow to crack even when the thief has carried it home. We watch four ways to store five passwords, from plain text to PBKDF2, count the attacker's work in steps instead of seconds, and finish with the cost: every login pays for the slow hash, and old hashes must be upgraded.
