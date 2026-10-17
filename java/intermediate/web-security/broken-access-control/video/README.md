# Broken Access Control — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `broken-access-control-explained.mp4` | the video, 1920×1080 |
| `broken-access-control-explained.m4a` | audio only |
| `broken-access-control-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Broken access control in Java, the number one risk in the OWASP Top 10. Explained with an online store, like a hotel where every key card opens every door because the lock only checks that the card is a real one. We walk order numbers and read other customers' orders, open an admin report nobody linked to, send a refund that the page only hid, delete an account through a DELETE that was never checked, and change another customer's delivery address. Then we fix all of it with one policy at one choke point, deny by default, object-level ownership checks, a log of every refusal, and a route inventory test that fails the build when a new route has no rule.
