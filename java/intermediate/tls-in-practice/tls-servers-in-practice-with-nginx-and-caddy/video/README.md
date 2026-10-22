# TLS Servers in Practice with nginx and Caddy — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tls-servers-in-practice-with-nginx-and-caddy-explained.mp4` | the video, 1920×1080 |
| `tls-servers-in-practice-with-nginx-and-caddy-explained.m4a` | audio only |
| `tls-servers-in-practice-with-nginx-and-caddy-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A real nginx 1.31 and a real Caddy 2.11 on loopback ports in front of the same shop: a certificate from a local root made with the step command, a client that needs the root and the right name, the redirect from http, the Server header and server_tokens, TLS 1.2 against a TLS 1.3 only setting, HSTS and the add_header inheritance trap, Caddy's own local authority with 12-hour certificates and automatic redirects, OCSP stapling that needs a responder, and nginx serving the old certificate until it is reloaded.
