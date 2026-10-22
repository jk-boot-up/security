# Session Management with Spring Session and Redis — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `session-management-with-spring-session-and-redis-explained.mp4` | the video, 1920×1080 |
| `session-management-with-spring-session-and-redis-explained.m4a` | audio only |
| `session-management-with-spring-session-and-redis-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Session management with Spring Session and Redis in Spring Boot 4.1: a bank whose sessions live in a password-protected Redis, why a stolen copy of the keys is a set of live sessions, session fixation and Spring Security's new id at sign-in, the idle timeout and the absolute timeout that Spring Session does not have, signing out, signing out everywhere, a password change and a limit on sessions, two servers sharing one store, a bank that fails closed when Redis stops answering, and a stranger filling Redis through the request cache.
