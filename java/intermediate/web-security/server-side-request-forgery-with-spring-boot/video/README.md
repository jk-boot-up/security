# Server-Side Request Forgery with Spring Boot — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `server-side-request-forgery-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `server-side-request-forgery-with-spring-boot-explained.m4a` | audio only |
| `server-side-request-forgery-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Server-side request forgery, SSRF, in Spring Boot 4, in Java. A staff form that imports a product image from a web address lets Spring's RestClient fetch anything the server can reach: the cloud metadata service, localhost, and the office admin page. Explained with an online store, like a courier who will collect a parcel from any address you write down, including the locked rooms inside the warehouse. We watch a blocklist of names fail against a decimal spelling, a short loopback address and a redirect, then fix it: look the name up once, judge every address it resolves to, connect to the address that was judged, judge every redirect again, and accept only a small image that arrives in time. We finish with the mistake that looks like a fix, checking a name and then looking it up again, and the cost: a second wall of network rules for outgoing traffic.
