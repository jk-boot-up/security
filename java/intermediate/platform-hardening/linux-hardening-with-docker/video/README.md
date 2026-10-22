# Linux Hardening with Docker — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `linux-hardening-with-docker-explained.mp4` | the video, 1920×1080 |
| `linux-hardening-with-docker-explained.m4a` | audio only |
| `linux-hardening-with-docker-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Two real Docker images built from Dockerfiles for an online store's server: a typical one with an account without a password, a second root, a copied shell with the setuid bit, a world-writable upload folder, loose modes on the shadow file, a host key and a config, an sshd_config that allows root and empty passwords, and three listeners on every interface; a scan run as root inside the container finds 19 findings, a machine fixes 7 and docker diff shows where they live, a new container has all 19 again, and the same shop with its decisions in the recipe has none.
