# Failing Closed: Secure Error Handling — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `fail-closed-error-handling-explained.mp4` | the video, 1920×1080 |
| `fail-closed-error-handling-explained.m4a` | audio only |
| `fail-closed-error-handling-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Failing closed in Java, OWASP Top 10 A10, mishandling of exceptional conditions. Explained with an online bank, like a bank vault whose door unlocks itself during a power cut. We watch a payment check approve a risky payment because the fraud service was down, and skip a daily limit because a value could not be read; rebuild it so that only a check that ran and passed can say yes; make a transfer to a frozen account all or nothing, and give database connections back on every path; show the customer a reference instead of a stack trace while the log keeps the detail without the card number; and finish with the bill: failing closed costs availability, paid for with time budgets, a limited retry, a circuit breaker that refuses fast, and an alert on refusals.
