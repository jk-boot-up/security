# Service Mesh and Automatic mTLS — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `service-mesh-mtls-explained.mp4` | the video, 1920×1080 |
| `service-mesh-mtls-explained.m4a` | audio only |
| `service-mesh-mtls-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A service mesh with Linkerd on a real kind cluster: automatic mutual TLS, identities taken from ServiceAccounts, an authorization policy that names who may call, and the limits: pods outside the mesh, root on the node, application flaws, ServiceAccount control and certificate lifetime.
