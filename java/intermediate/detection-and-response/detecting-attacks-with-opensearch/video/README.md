# Detecting Attacks with OpenSearch — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `detecting-attacks-with-opensearch-explained.mp4` | the video, 1920×1080 |
| `detecting-attacks-with-opensearch-explained.m4a` | audio only |
| `detecting-attacks-with-opensearch-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A real OpenSearch 3.9 with its alerting plugin in Docker, loaded with the 772 events of one synthetic Monday that hide four attacks: brute force, spraying, impossible travel and a privilege change. The detections written as queries (a filter, a terms aggregation, a date histogram and a threshold), tuned one threshold at a time and scored against the truth, then saved as bucket-level and query-level monitors and run once with dryrun; impossible travel done in code from the events read back; a mapping mistake that stops a rule, and an attack that falls between two fixed time buckets.
