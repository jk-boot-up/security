# Threat Modeling with pytm — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `threat-modeling-with-pytm-explained.mp4` | the video, 1920×1080 |
| `threat-modeling-with-pytm-explained.m4a` | audio only |
| `threat-modeling-with-pytm-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The bank's data-flow design (a customer, a mobile app, an API gateway, a payments service, two stores and a card processor across four trust boundaries) written as a pytm 1.4 model by a Java program: the 113 threats of pytm's library checked against every element and flow, 170 findings when nothing is said, 167, 159, 111 and 94 as encryption, authentication, access control and input handling, and resilience are declared, a diagram drawn by Graphviz, a report from a template, a draft with a flow that skips a boundary that pytm accepts, and a refund flow over plain http that adds findings.
