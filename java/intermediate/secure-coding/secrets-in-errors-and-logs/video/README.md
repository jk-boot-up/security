# Secrets in Errors and Logs — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `secrets-in-errors-and-logs-explained.mp4` | the video, 1920×1080 |
| `secrets-in-errors-and-logs-explained.m4a` | audio only |
| `secrets-in-errors-and-logs-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Secrets in errors and logs, in Java, for an online shop's checkout: a stack trace that shows the customer the database password, logs that keep card numbers, the CVV and session tokens, a line break in a name that forges a log entry, and the fixes: a generic answer with a reference, a Secret type that prints as [REDACTED], a one-line logger with Luhn-checked card masking, and canary tests.
