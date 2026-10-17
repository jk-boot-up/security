# Asymmetric Encryption with Bouncy Castle and OpenSSL — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `asymmetric-encryption-with-bouncy-castle-explained.mp4` | the video, 1920×1080 |
| `asymmetric-encryption-with-bouncy-castle-explained.m4a` | audio only |
| `asymmetric-encryption-with-bouncy-castle-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Asymmetric encryption with Bouncy Castle and OpenSSL, in Java: the JDK can do the maths, but real systems exchange keys and encrypted files in standard formats. Explained with an online bank and its customer Ana, like a diplomat's sealed pouch that any embassy in the world knows how to open. We write keys as PEM text and protect a private key with a password, make a self-signed certificate, seal a statement in a standard CMS envelope, have the openssl command-line tool open it as a check that it really is a standard, and finish with the bill: the easy call picks the older RSA padding, the library is large, and registering it changes the whole program.
