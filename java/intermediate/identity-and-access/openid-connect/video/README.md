# OpenID Connect — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `openid-connect-explained.mp4` | the video, 1920×1080 |
| `openid-connect-explained.m4a` | audio only |
| `openid-connect-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> OpenID Connect in Java with only the JDK: "sign in with the bank" at an online store, like a passport checked at a hotel desk rather than a key card that only opens a door. We follow a sign-in from the store to the bank and back, look inside the ID token and see why an access token is not proof of who signed in, check the ID token the way OpenID Connect Core requires (signature with a published key, the exact issuer, the audience and azp, expiry with skew, and the nonce bound to this browser), load the provider's metadata and keys through discovery and refuse a provider posing as another, rotate signing keys, show why the customer is identified by issuer and subject and never by email, and finish with sign-out: RP-initiated logout and back-channel logout tokens.
