# Security Misconfiguration — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `security-misconfiguration-explained.mp4` | the video, 1920×1080 |
| `security-misconfiguration-explained.m4a` | audio only |
| `security-misconfiguration-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Security misconfiguration in Java, OWASP Top 10 A02: the settings nobody changed. Explained with an online store, like a shop that fits a strong lock and leaves the spare key in the door. We start the store with its shipped defaults and let a checker find a default admin password, a debug page that prints the secret, a directory listing that shows a backup file, and stack traces in error pages; harden it with secure defaults and a gate that refuses to start with a default secret; catch development settings that drift into production; remove version banners, the TRACE method and a settings file anyone can read; and finish with the bill: a baseline that must be checked on every change, at every startup, and against the live store.
