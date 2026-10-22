# Policy as Code — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `policy-as-code-explained.mp4` | the video, 1920×1080 |
| `policy-as-code-explained.m4a` | audio only |
| `policy-as-code-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Policy as code for a bank's Kubernetes deployments: a deny-by-default policy in Rego, a table of requests it must decide as a reviewer expects, the holes a missing field leaves, tests for the policy and mutation testing of those tests, an admission review, Conftest in the build and a warning-first rollout.
