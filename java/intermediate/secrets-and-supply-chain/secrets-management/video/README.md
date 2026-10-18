# Secrets Management — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `secrets-management-explained.mp4` | the video, 1920×1080 |
| `secrets-management-explained.m4a` | audio only |
| `secrets-management-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Secrets management in Java: where database passwords and API keys leak, and how a secrets service hands out short-lived logins instead. Explained with an online store, like a hotel that gives each guest a key card that stops working at checkout, instead of copying one master key for everyone. A small scanner finds secrets committed to a repository, a child process reads a key from the environment, and a deleted file is still in an image layer. Then secrets are injected at run time from owner-only files and redacted from logs, a secrets service leases a separate database login to each application with a time to live, a payment key is rotated with no downtime, and the bill: the secrets service itself becomes the most valuable thing to protect.
