# Security Logging — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `security-logging-explained.mp4` | the video, 1920×1080 |
| `security-logging-explained.m4a` | audio only |
| `security-logging-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Security logging in Java, OWASP Top 10 A09: a log that can answer who moved the money, holds no secrets, and shows when it was changed. Explained with an online bank, like a bank's visitor book that records every visit but never the combination of the safe. We compare an old logger that cannot say who made a transfer with a structured security log that can, remove passwords, tokens and session ids and mask card numbers, stop a typed line break from forging a log line, chain every line to the one before it with SHA-256 so a deleted or edited line is found, and finish with the bill: logs are personal data with retention limits, they must leave the host to survive a breach, and their volume needs a budget.
