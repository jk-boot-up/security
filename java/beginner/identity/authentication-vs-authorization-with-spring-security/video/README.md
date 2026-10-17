# Authentication vs Authorization with Spring Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `authentication-vs-authorization-with-spring-security-explained.mp4` | the video, 1920×1080 |
| `authentication-vs-authorization-with-spring-security-explained.m4a` | audio only |
| `authentication-vs-authorization-with-spring-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Authentication vs authorization with Spring Security, in Java: authentication finds out who is asking, and authorization decides what that person may do. Explained with an online store, like a hotel where the front desk checks your passport and the key card decides which doors open. We use Spring Security's filter chain, BCrypt password storage and PreAuthorize rules, watch a login-only shop let a customer refund orders, see Spring answer 401 and 403, and finish with the cost: a forgotten rule is an open endpoint, so a second rule on the address is worth having, and HTTP Basic checks the password on every request.
