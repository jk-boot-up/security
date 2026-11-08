# Server-Side Request Forgery (SSRF) and Its Defences — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `server-side-request-forgery-explained.mp4` | the video, 1920×1080 |
| `server-side-request-forgery-explained.m4a` | audio only |
| `server-side-request-forgery-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Server-side request forgery, SSRF, and its defences, in Java. A feature that makes the server fetch a web address for a customer, like importing a product image, lets the customer reach whatever the server can reach: the cloud metadata service with its credentials, the server's own settings, and internal admin pages. Explained with an online store, like a courier who will collect a parcel from any address you write down, including the locked rooms inside the warehouse. We watch a blocklist of names fail against other spellings of the same address, a name that points inside, and a redirect, then fix it by checking the address the connection really uses, connecting to exactly that address, and checking every redirect again. We finish with the cost: an allow-list needs upkeep, and network rules for outgoing traffic still matter.
