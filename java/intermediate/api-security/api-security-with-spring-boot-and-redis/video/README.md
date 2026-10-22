# API Security with Spring Boot and Redis — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `api-security-with-spring-boot-and-redis-explained.mp4` | the video, 1920×1080 |
| `api-security-with-spring-boot-and-redis-explained.m4a` | audio only |
| `api-security-with-spring-boot-and-redis-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> API security with Spring Boot 4.1 and Redis, from the OWASP API Top 10: a bank API that binds the request body to the stored account so a customer promotes themselves to admin, returns the whole record and every customer on a huge page, and answers a thousand requests in a loop; then request records with Bean Validation and unknown fields refused, response records and a page ceiling, and a token bucket in Redis written as one Lua script that gives one budget to every server, refuses with 429 and a Retry-After, and fails closed when Redis stops answering.
