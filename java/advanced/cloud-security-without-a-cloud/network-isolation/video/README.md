# Network Isolation — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `network-isolation-explained.mp4` | the video, 1920×1080 |
| `network-isolation-explained.m4a` | audio only |
| `network-isolation-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Network segmentation measured on real Docker networks for an online store's six services: one flat network where a taken-over front reaches the database, the cache, the admin panel and the build server; three coarse networks; a layout generated from the design as code with one internal network for each allowed pair; drift found by comparing what can talk with what should; published ports; and which services can reach a server on the internet.
