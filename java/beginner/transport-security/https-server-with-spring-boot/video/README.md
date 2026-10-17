# An HTTPS Server with Spring Boot — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `https-server-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `https-server-with-spring-boot-explained.m4a` | audio only |
| `https-server-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> An HTTPS server with Spring Boot, in Java: the earlier project built HTTPS from the JDK's parts, and this one lets Spring Boot do the plumbing. Explained with an online store, like fitting the shop with a standard door that the locksmith has certified, and still walking round the outside to check it is shut. We configure TLS 1.3 from an SSL bundle, make a client that verifies the certificate and the host name, redirect plain HTTP with Spring Security and add HSTS and secure cookies, renew the certificate while the server keeps running, require a client certificate for mutual TLS, and finish with what the framework leaves to you: error pages, headers and the private key file.
