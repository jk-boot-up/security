# Cross-Site Scripting with Spring Boot: Thymeleaf Escaping and a Nonce CSP — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cross-site-scripting-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `cross-site-scripting-with-spring-boot-explained.m4a` | audio only |
| `cross-site-scripting-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Cross-site scripting defences with Spring Boot and Thymeleaf: the template engine does the encoding, as long as you do not switch it off. Explained with an online store's product reviews. We see th:text escape a review by default, see th:utext turn the same review into markup, write values into an attribute, a link and a script the way Thymeleaf escapes each, send a strict Content-Security-Policy from Spring Security with a fresh nonce for every page, and finish with the bill: escaping protects only what goes through it, and the nonce needs one small filter.
