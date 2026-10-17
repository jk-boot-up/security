# Webhook Security: Signatures, Replays and Idempotency — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `webhook-security-explained.mp4` | the video, 1920×1080 |
| `webhook-security-explained.m4a` | audio only |
| `webhook-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Webhook security in Java: a payment provider tells an online store "payment succeeded" by posting to a public address, and anyone can post to that address. A forged message marks an order paid that was never paid; then an HMAC-SHA256 signature over the exact raw body stops forgeries and changed bodies, a signed timestamp and a record of processed events stop a real refund message from being replayed and paid twice, idempotent handlers cope with retries and events out of order, and two secrets overlap while the secret is changed. The bill: keep the raw bytes, store and rotate the secret, and still confirm critical events with the provider's API. Mapped to the OWASP API Security Top 10 API8 and API10 and CWE-345.
