# Inspecting TLS Traffic with a Key Log — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `inspecting-tls-traffic-explained.mp4` | the video, 1920×1080 |
| `inspecting-tls-traffic-explained.m4a` | audio only |
| `inspecting-tls-traffic-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Inspecting TLS traffic in plain Java, with nothing hidden: a TLS 1.3 style key schedule (HKDF-SHA-256, X25519) and AES-GCM records, what a capture shows a network observer, the NSS key log format behind SSLKEYLOGFILE, a client that refuses to write one in production, a decryptor that opens exactly the connections and directions a key log lists, and forward secrecy: a leaked server key opens recorded static RSA sessions and nothing ephemeral.
