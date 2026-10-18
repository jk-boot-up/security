# OAuth 2.0 Authorization Code Flow with PKCE — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `oauth2-authorization-code-pkce-explained.mp4` | the video, 1920×1080 |
| `oauth2-authorization-code-pkce-explained.m4a` | audio only |
| `oauth2-authorization-code-pkce-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The OAuth 2.0 authorization code flow with PKCE, in Java with only the JDK. Explained with an online bank and a budgeting app, like a hotel key card that the front desk issues for one room and one stay, without ever handing over the master key. We walk through every message of the flow, show that a leaked authorization code is useless without the PKCE code verifier, checked against the published example in RFC 7636, register exact redirect addresses so a code cannot be sent anywhere else, bind the state to the browser to stop login forgery, make codes single-use and short-lived, limit the app with scopes, rotate refresh tokens so that a stolen one revokes the whole family, and finish with current practice from RFC 9700: no implicit flow, no password grant, and a real authorization server in production.
