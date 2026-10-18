# Passkeys and WebAuthn — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `passkeys-explained.mp4` | the video, 1920×1080 |
| `passkeys-explained.m4a` | audio only |
| `passkeys-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Passkeys and WebAuthn explained in Java with only the JDK. Explained with an online bank's passwordless sign-in, like a house key cut to fit one front door, that simply does not turn in any other lock. We register a passkey and see that the bank stores only a public key, sign in with a fresh single-use challenge, watch a look-alike site get nothing at all while a typed password is phished, require a fingerprint for a payment, catch a copied key with the signature counter, and finish with the bill: a lost phone, recovery codes, and a password that must be switched off before the account is really phishing-resistant.
