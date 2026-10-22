# Tokens in the Browser with Spring Boot: a BFF and DPoP — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tokens-in-the-browser-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `tokens-in-the-browser-with-spring-boot-explained.m4a` | audio only |
| `tokens-in-the-browser-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Tokens in the browser with Spring Boot 4.1, Spring Security 7.1 and Keycloak 26: a single-page app whose localStorage tokens are stolen by a third-party script, a backend for the frontend that holds the tokens on the server behind a __Host-bff cookie with an XSRF header, SameSite and preflight against requests another site starts, a refresh done on the server, then DPoP-bound tokens checked by Spring Security's resource server with proofs that are refused when copied, reused, old, for another request or from another key, and the bill: shared sessions, an extra hop, a lost key, and a script on the open page.
