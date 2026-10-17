# HTTPS Server with nginx — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `https-server-with-nginx-explained.mp4` | the video, 1920×1080 |
| `https-server-with-nginx-explained.m4a` | audio only |
| `https-server-with-nginx-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> HTTPS with nginx, checked from Java: the web server in front of an online store ends the TLS. Explained with an online store, like a receptionist who checks every visitor at the front door so the staff inside need not. We run real nginx containers and talk to them with a Java client that checks the certificate chain and the host name: TLS 1.3 with HTTP/2, which protocol versions to allow, the redirect from the plain port and HSTS, an impostor certificate and a wrong name refused, client certificates with mutual TLS, a forged X-Forwarded-Proto header that fools the application, a configuration check with nginx -t, and a seven-point score for TLS settings.
