# SAML Basics with Keycloak — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `saml-basics-with-keycloak-explained.mp4` | the video, 1920×1080 |
| `saml-basics-with-keycloak-explained.m4a` | audio only |
| `saml-basics-with-keycloak-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> SAML 2.0 single sign-on with Spring Security 7 and a real Keycloak 26 in Docker: the authentication request and the signed response, an edited name, a response self-signed with a valid key of the attacker's, a genuine response signed by another realm, signature wrapping with an unsigned admin assertion before or after the signed one, no signature, a DOCTYPE, replay, an assertion that has expired, a response the bank signed for another shop, and a key rotation at the bank that refuses every shopper until the shop reloads its metadata.
