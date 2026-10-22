# Failing Closed with Spring Boot — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `fail-closed-error-handling-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `fail-closed-error-handling-with-spring-boot-explained.m4a` | audio only |
| `fail-closed-error-handling-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Failing closed in Spring Boot 4.1, OWASP Top 10 A10: a payment check that approves when the fraud service is broken, the same check rebuilt with a time budget, Spring Framework 7 retry and a breaker, four transfers of which three leave the money half moved because of a checked exception or a call inside the same class, Boot's error response that names the server and the card, one error handler with a reference, and the cost in availability.
