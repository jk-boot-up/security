# Security Logging with Loki and Grafana — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `security-logging-with-loki-and-grafana-explained.mp4` | the video, 1920×1080 |
| `security-logging-with-loki-and-grafana-explained.m4a` | audio only |
| `security-logging-with-loki-and-grafana-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A real Loki 3.7 and Grafana 13 in Docker with the bank's eight security events as JSON lines: pushes answered with 204 and refused without a tenant, LogQL for the transfer, everything from the same address and failures counted by address, the cost of putting the user in a label (150 streams against 1), the Loki ruler firing on four failed logins from one address and a rule with a typo that never does, tenant isolation, an event ten days old refused, no delete API, and Grafana reading the same data through a data source that sends the tenant.
