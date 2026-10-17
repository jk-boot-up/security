# Input Validation Done Right — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `input-validation-explained.mp4` | the video, 1920×1080 |
| `input-validation-explained.m4a` | audio only |
| `input-validation-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Input validation in Java, done right, for OWASP Top 10 A05 and A06. Explained with an online store, like a door attendant who checks tickets at the door instead of trusting a sticker the visitor printed at home. We send a negative quantity and a hidden price straight past the checkout page, parse every field into a type that cannot hold anything else, fold full-width and look-alike names into one canonical form, limit request bodies, carts and fields, count the steps of a regular expression that doubles its work with every character, and finish with the bill: a valid value is still not safe to put in a page or a query.
