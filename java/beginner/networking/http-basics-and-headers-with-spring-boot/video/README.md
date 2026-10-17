# HTTP Basics and Security Headers with Spring Boot — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `http-basics-and-headers-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `http-basics-and-headers-with-spring-boot-explained.m4a` | audio only |
| `http-basics-and-headers-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> HTTP basics and security headers with Spring Boot, in Java: the earlier project built HTTP by hand, and this one lets a real framework do it. Explained with an online store, like fitting the shop with standard locks instead of forging them. We start two real Spring Boot servers on the loopback interface, one with headers switched off and one hardened, and score their real responses. We see why Strict-Transport-Security appears only over HTTPS, harden the session cookie with three properties, watch Tomcat refuse a line break in a header, bound the size of a request, and note the cost: a framework's defaults are a starting point, and trusting a forwarded header is a decision.
