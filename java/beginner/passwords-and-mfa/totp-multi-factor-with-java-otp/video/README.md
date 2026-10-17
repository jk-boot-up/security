# One-Time Codes with the java-otp Library — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `totp-multi-factor-with-java-otp-explained.mp4` | the video, 1920×1080 |
| `totp-multi-factor-with-java-otp-explained.m4a` | audio only |
| `totp-multi-factor-with-java-otp-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> One-time codes with the java-otp library, in Java: a library can make the six-digit code for you and check it for one time step, but a bank still has to decide the rest. Explained with an online bank, like a clerk who buys a ready-made stamp but still has to decide who may use it and how often. We check the library's codes against the values published in RFC 4226 and RFC 6238, see that it validates only a single step so the window is written by the bank, try SHA-256 and SHA-512 and eight digits, build the otpauth link a QR code carries, and finish with the bill: the library keeps no replay memory, no secrets and no attempt limits.
