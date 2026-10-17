# JWT Done Right — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `jwt-done-right-explained.mp4` | the video, 1920×1080 |
| `jwt-done-right-explained.m4a` | audio only |
| `jwt-done-right-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> JSON Web Tokens done right, in Java with only the JDK. Explained with an online bank's accounts API, like a cinema ticket that anyone can read but only the box office can stamp. We take a bank access token apart into its three parts, show that a reader which only decodes believes a forged subject, fix the algorithm and token type on the server so that a header saying none or HS256 is refused, check expiry with a small clock skew, not-before, issuer, audience and scope, choose keys by key id from a set the API already holds and rotate them without downtime, and finish with the bill: an access token cannot be revoked before it expires, so lifetimes are minutes and refresh tokens stay on the server. Checked against the published Ed25519 example in RFC 8037.
