# Digital Signatures (Ed25519) — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `digital-signatures-explained.mp4` | the video, 1920×1080 |
| `digital-signatures-explained.m4a` | audio only |
| `digital-signatures-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Digital signatures in Java: how can a message prove who made it, so that anyone can check but only one party can sign? Explained with an online store's price list, like the signature of a notary that anyone can compare with a specimen on file but nobody else can write. We see how an HMAC tag can be forged by anyone who can check it, sign a price list with Ed25519 and check it with only the public key, tie a signature to its purpose so it cannot be reused for another, stop a genuine old list being replayed with a version and an expiry inside the signed bytes, and finish with the bill: a signature is only as good as the key you check it against, and a stolen private key signs anything.
