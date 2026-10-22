# Secrets in Logs with Logback — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `secrets-in-errors-and-logs-with-logback-explained.mp4` | the video, 1920×1080 |
| `secrets-in-errors-and-logs-with-logback-explained.m4a` | audio only |
| `secrets-in-errors-and-logs-with-logback-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Logback 1.6 and logstash-logback-encoder 9 against a careless checkout log: card number, cvv, token and database password in the file, a forged second event from a line break, a layout with a mask converter and a masked exception converter, a Luhn check so that an order number is not taken for a card, a Secret and a Card that print safely, JSON events with a masking decorator by path and by shape, a record that leaks its fields, and a secret in a shape nobody listed.
