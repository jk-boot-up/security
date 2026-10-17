# The Principle of Least Privilege — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `least-privilege-explained.mp4` | the video, 1920×1080 |
| `least-privilege-explained.m4a` | audio only |
| `least-privilege-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The principle of least privilege in Java: give people and programs only the access they need. Explained with an online store, like hotel keys that open one room and expire at checkout, not a master key for every guest. We model roles and permissions with deny by default, measure the blast radius of a stolen credential for an admin and for a scoped job, require two different people for a refund, give access that expires on its own, find permissions nobody uses, and lock a file to its owner with operating system permissions.
