# Authentication vs Authorization — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `authentication-vs-authorization-explained.mp4` | the video, 1920×1080 |
| `authentication-vs-authorization-explained.m4a` | audio only |
| `authentication-vs-authorization-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Authentication vs authorization in Java: authentication finds out who is asking, and authorization decides what that person may do. Explained with an online store, like a hotel where the front desk checks your passport and the key card decides which doors open. We watch a shop with no checks, add a login with session tokens, see why a logged-in customer can still refund orders, add roles and ownership rules with the difference between a 401 and a 403, and finish with the cost: sessions expire and roles copied at login can go stale.
