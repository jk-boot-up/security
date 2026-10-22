# Container Hardening with Docker and kind — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `container-hardening-with-docker-and-kind-explained.mp4` | the video, 1920×1080 |
| `container-hardening-with-docker-and-kind-explained.m4a` | audio only |
| `container-hardening-with-docker-and-kind-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Container hardening measured with thirteen real attempts inside real Docker containers (write to the app, run a dropped file, become root, sniff the network, mount, read the Docker socket, the host's files, the host network and processes, memory and process limits, the secret in the environment) from the tutorial's docker run with 13 of 13 possible down to a hardened container with 1, then a real kind cluster with Pod Security Admission: a tutorial pod refused by baseline and restricted, a plain pod admitted by baseline and refused by restricted, a namespace that only warns, and the attacks that remain in the pods that are admitted.
