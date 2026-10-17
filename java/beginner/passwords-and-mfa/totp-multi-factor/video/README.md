# One-Time Codes (TOTP) — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `totp-multi-factor-explained.mp4` | the video, 1920×1080 |
| `totp-multi-factor-explained.m4a` | audio only |
| `totp-multi-factor-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> One-time codes (TOTP) in Java: a second factor made from a secret that the phone and the bank share, and the current time. Explained with an online bank, like a safe-deposit box that needs your key and a bank clerk's key turned together, where the clerk's key changes every thirty seconds. We build the code from HMAC following RFC 4226 and RFC 6238, check it against the published test values, see how a window of one step forgives a clock that is a little out, stop a code being used twice, and finish with the bill: a code typed into a fake bank page still works, the secret also lives on the bank's server, and recovery codes are a second way in.
