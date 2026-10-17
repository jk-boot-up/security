# Threat Modeling with STRIDE — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `threat-modeling-explained.mp4` | the video, 1920×1080 |
| `threat-modeling-explained.m4a` | audio only |
| `threat-modeling-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Threat modeling with STRIDE in Java, for OWASP Top 10 A06, insecure design. Explained with an online bank, like an architect who walks through a building plan asking where a burglar would get in, before the walls are built. The bank's mobile app, gateway, payments service, stores and card processor become a data-flow model with trust boundaries, checked for gaps; STRIDE rules turn it into twenty candidate threats; each one is rated and decided: mitigate, accept with an owner and a review date, transfer, or avoid; every mitigation becomes an executable check, so adding an unencrypted refund flow fails a test before release; and the bill: a model is only worth what its last update was, and an accepted risk has an expiry date.
