# API Security: Mass Assignment, Data Exposure and Resource Limits — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `api-security-explained.mp4` | the video, 1920×1080 |
| `api-security-explained.m4a` | audio only |
| `api-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> API security in Java: mass assignment, excessive data exposure, and unrestricted resource consumption, from the OWASP API Security Top 10. A bank's JSON API that copies every field the app sends into the account lets a customer make herself an admin with a huge credit limit; one that returns the whole record sends the card number and PIN hash to the phone; and one that answers any page size, any number of times, hands over every customer and never slows down. Explained with a bank API, like a bank counter where the customer fills in their own credit limit, the clerk passes over the whole file, and the queue has no end. We fix it with explicit request and response models, a cap on page size, and a token bucket for each caller that answers 429 with Retry-After. We finish with the cost: more code, and limits that need real numbers.
