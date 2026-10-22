# TLS Misconfigurations with testssl.sh — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tls-misconfigurations-with-testssl-explained.mp4` | the video, 1920×1080 |
| `tls-misconfigurations-with-testssl-explained.m4a` | audio only |
| `tls-misconfigurations-with-testssl-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The real testssl.sh 3.2 against five local servers: a nginx configured to the guidance, a configuration copied from the first search result with obsolete suites, no HSTS or security headers and a version in the Server header, a server written with the JDK that speaks TLS 1.0 and 1.1 and has no forward secrecy, an expired certificate, a certificate for another name, and a scan by address that raises a false alarm. The findings become a policy of eleven rules with exceptions that carry a reason.
