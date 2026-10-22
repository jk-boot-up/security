# Inspecting TLS Traffic with Wireshark — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `inspecting-tls-traffic-with-wireshark-explained.mp4` | the video, 1920×1080 |
| `inspecting-tls-traffic-with-wireshark-explained.m4a` | audio only |
| `inspecting-tls-traffic-with-wireshark-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A real nginx, the openssl client with a key log file, a tap that writes a capture file of a login, and tshark 4.6: what the capture shows without secrets (the server name, the cipher, encrypted records), what the key log opens (the request, the form, the cookie, the encrypted certificate), why one connection's key log opens no other, a log with only the client's lines, and the difference between a leaked server key on a recording made with ephemeral keys and one made with RSA key exchange.
