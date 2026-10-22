# CORS with Spring Boot — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cors-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `cors-with-spring-boot-explained.m4a` | audio only |
| `cors-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> CORS in Spring Boot 4.1 with Spring Security 7: a bank API with no policy where a page can send but not read, the policies that look like fixes (a wildcard with credentials, a reflecting origin pattern, a list that allows null, a pattern that matches a lookalike), an exact allow-list that Spring enforces with a 403 before the controller, the preflight that Spring Security answers only if the policy is registered there, and why CORS is a browser rule and not a defence.
