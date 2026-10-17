# Asymmetric Encryption (RSA and Key Agreement) — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `asymmetric-encryption-explained.mp4` | the video, 1920×1080 |
| `asymmetric-encryption-explained.m4a` | audio only |
| `asymmetric-encryption-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Asymmetric encryption in Java: how can an online bank and a customer share a secret key when they have never met and an eavesdropper hears everything? Explained with a mailbox that anyone can drop a letter into but only the owner can open. We show why a key cannot be sent in the clear, lock a key with an RSA public key using OAEP and see why the same message gives different bytes each time, carry a ten-thousand-byte statement with hybrid encryption, agree a key with X25519 and HKDF without sending any key at all, and finish with the bill: nothing in the maths says whose public key it is, so a man in the middle can read everything.
