# An HTTPS Server, Put Together — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `https-server-explained.mp4` | the video, 1920×1080 |
| `https-server-explained.m4a` | audio only |
| `https-server-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> An HTTPS server put together in Java: certificates, TLS, redirects, HSTS and cookies in one working shop. Explained with an online store, like a shop with a strong front door, a sign that points everyone to it, and a rule that valuables never leave through the back. We start a real HTTPS server on this machine, make a checked request, see the plain port only redirect, keep a session cookie on HTTPS with the Secure flag and watch an unflagged cookie leak, see the client refuse an untrusted certificate, a wrong name and a downgrade, and score a careful shop against a careless one on five checks.
