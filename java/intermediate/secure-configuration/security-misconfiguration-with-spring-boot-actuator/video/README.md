# Security Misconfiguration with Spring Boot Actuator — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `security-misconfiguration-with-spring-boot-actuator-explained.mp4` | the video, 1920×1080 |
| `security-misconfiguration-with-spring-boot-actuator-explained.m4a` | audio only |
| `security-misconfiguration-with-spring-boot-actuator-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Security misconfiguration with Spring Boot Actuator, OWASP Top 10 A02, in an online store on Spring Boot 4.1: the tutorial setting that exposes everything, a payment key read from the environment endpoint and found again in a heap dump, what Spring Boot already hides and what it cannot, a hardened store with its own loopback port, a short endpoint list, login rules and a read-only loggers endpoint, and one added line that costs a check.
