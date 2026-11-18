# Linux Hardening Checker — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `linux-hardening-explained.mp4` | the video, 1920×1080 |
| `linux-hardening-explained.m4a` | audio only |
| `linux-hardening-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A Linux hardening checker in Java for an online store's server image: unexpected set-user-id files, loose permissions on secrets, a second root account and empty passwords, SSH root login and password authentication, kernel settings, and services listening on every interface; it applies the permission fixes a machine can make safely, leaves the decisions to a person, and runs again in every build.
