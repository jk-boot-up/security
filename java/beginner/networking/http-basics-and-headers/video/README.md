# HTTP Basics and Security Headers — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `http-basics-and-headers-explained.mp4` | the video, 1920×1080 |
| `http-basics-and-headers-explained.m4a` | audio only |
| `http-basics-and-headers-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> HTTP basics and security headers in Java: an HTTP message is text, and the sender writes every line. Explained with an online store. We parse and write a request byte by byte, refuse messages that two programs could read differently (the idea behind request smuggling), refuse header injection through a customer's search text, judge session cookies for HttpOnly, Secure and SameSite, and score a real response from a real local server against five security headers.
