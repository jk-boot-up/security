# Keystores and Key Management Basics — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `keystore-basics-explained.mp4` | the video, 1920×1080 |
| `keystore-basics-explained.m4a` | audio only |
| `keystore-basics-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Keystores and key management basics in Java: the best encryption in the world fails if the key is lying around. Explained with an online bank, like a bank that spends a fortune on a vault and leaves the key under the doormat. We find a key hard-coded in a compiled class, see that a key file is only as private as its permissions, keep keys in an encrypted PKCS12 keystore that shows neither the key nor its name, derive keys from passwords with PBKDF2 and from a master key with HKDF, rotate keys using key names inside the messages, and finish with the bill: the keystore needs a password too, and the file records weaker protection settings than you might expect.
