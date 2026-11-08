# Insecure Deserialization and Its Defences — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `insecure-deserialization-explained.mp4` | the video, 1920×1080 |
| `insecure-deserialization-explained.m4a` | audio only |
| `insecure-deserialization-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Insecure deserialization and its defences, in Java. Keeping a shopping cart in a cookie as a serialized Java object looks easy, but the bytes decide which classes the server builds, and some classes run code while they are being read: a customer who edits the cookie can lower a price, or make the server delete a file before it even sees that the cookie is not a cart. Explained with an online store, like a flat-pack kit whose instructions you must follow while you unpack it, written by whoever packed the box. We add an allow-list ObjectInputFilter that refuses unknown classes before they are built, see that it still accepts a forged price, then replace the object with plain, signed data: product codes and quantities, an HMAC checked first, and prices looked up on the server. We finish with the cost: an allow-list needs upkeep, and signing needs a key.
