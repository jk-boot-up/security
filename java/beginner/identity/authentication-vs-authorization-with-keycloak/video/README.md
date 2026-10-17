# Authentication vs Authorization with Keycloak — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `authentication-vs-authorization-with-keycloak-explained.mp4` | the video, 1920×1080 |
| `authentication-vs-authorization-with-keycloak-explained.m4a` | audio only |
| `authentication-vs-authorization-with-keycloak-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Authentication vs authorization with Keycloak, in Java: let a login server do the logging in. Explained with an online store, like a theatre that sells tickets at one box office and only checks tickets at the doors. We see a Spring Boot resource server that holds no passwords and trusts only tokens signed by the login server, why no token gives 401 and the wrong role gives 403, how roles in a Keycloak token become authorities, why a correctly signed token still needs the right issuer and audience, what happens when a role is taken away but the token is still alive, and what it costs to end a token early. A real Keycloak in Docker repeats the checks in the integration tests.
