# Cross-Site Request Forgery (CSRF) and Its Defences — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cross-site-request-forgery-explained.mp4` | the video, 1920×1080 |
| `cross-site-request-forgery-explained.m4a` | audio only |
| `cross-site-request-forgery-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Cross-site request forgery and its defences, in Java: a browser attaches a site's cookie to every request sent to that site, even one that another site started. Explained with an online bank and a transfer form. A page on another site makes Ana's logged-in browser send money she never meant to send; then a per-session token in every form stops it, SameSite cookies stop the cookie from riding along, Fetch Metadata and Origin checks show where the request came from, and the bill shows what each defence still misses: older browsers, same-site subdomains, forms that forgot the token, and why APIs with bearer tokens are exposed differently. Mapped to OWASP Top 10 A01 and CWE-352.
