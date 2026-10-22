# OAuth 2.0 Authorization Code with PKCE and Keycloak — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `oauth2-authorization-code-pkce-with-keycloak-explained.mp4` | the video, 1920×1080 |
| `oauth2-authorization-code-pkce-with-keycloak-explained.m4a` | audio only |
| `oauth2-authorization-code-pkce-with-keycloak-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> OAuth 2.0 authorization code flow with PKCE against a real Keycloak 26 in Docker, with a Spring Security 7 resource server: the flow step by step with a consent screen, PKCE that burns a code at the first wrong try, exact redirect addresses (a path trick, a lookalike host, a query, upper case and a trailing slash are all refused), the state check Mallory's link fails, a code that works once and ends the session when reused, a code that expires, scopes the client was not registered for, refresh token rotation and reuse detection, the implicit flow and the password grant switched off, and a confidential client that still needs PKCE.
