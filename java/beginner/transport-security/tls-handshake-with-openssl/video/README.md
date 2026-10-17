# The TLS Handshake with OpenSSL — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tls-handshake-with-openssl-explained.mp4` | the video, 1920×1080 |
| `tls-handshake-with-openssl-explained.m4a` | audio only |
| `tls-handshake-with-openssl-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The TLS handshake with OpenSSL, in Java: the best way to trust your TLS setup is to point a second implementation at it. Explained with an online store, like asking a stranger to read your map back to you. We run a real TLS server and client on one machine, write the certificate and key as PEM files that other programs read, see the client refuse an impostor and a wrong name, offer old and new protocol versions, and use openssl s_client and s_server in the tests as the other end of the conversation, and finish with the bill: openssl only checks what you ask it to check.
