# OpenID Connect with Spring Security and Keycloak — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `openid-connect-with-spring-security-and-keycloak-explained.mp4` | the video, 1920×1080 |
| `openid-connect-with-spring-security-and-keycloak-explained.m4a` | audio only |
| `openid-connect-with-spring-security-and-keycloak-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> OpenID Connect login with Spring Security 7 and a real Keycloak 26 in Docker: the authorization request with state, nonce and PKCE, the ID token against the access token, what Spring's validator refuses (a key never published, a wrong issuer, a wrong audience, azp, expiry with skew, no subject, alg none), discovery that checks the issuer and not where the keys are, key rotation, an account takeover by e-mail address through a second provider, userinfo, RP-initiated logout, and real back-channel logout from Keycloak that ends one of Ana's two sessions.
