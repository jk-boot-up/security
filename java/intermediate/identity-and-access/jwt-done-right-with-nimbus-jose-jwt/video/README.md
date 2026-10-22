# JWT Done Right with Nimbus JOSE + JWT — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `jwt-done-right-with-nimbus-jose-jwt-explained.mp4` | the video, 1920×1080 |
| `jwt-done-right-with-nimbus-jose-jwt-explained.m4a` | audio only |
| `jwt-done-right-with-nimbus-jose-jwt-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> JWT validation with Nimbus JOSE + JWT 10 in Java 25, the same checks as the plain project with a standard library and a real key set endpoint: decoding versus verifying, a fixed algorithm and type against none, HS256 key confusion and the wrong type, expiry with clock skew, issuer, audience and scope, keys by id from a published set with caching and rotation, a retired key that lives on in the cache, a minimum time between refetches, jku and jwk headers that are never followed, and why a token cannot be taken back.
