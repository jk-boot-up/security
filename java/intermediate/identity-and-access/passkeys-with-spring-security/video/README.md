# Passkeys with Spring Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `passkeys-with-spring-security-explained.mp4` | the video, 1920×1080 |
| `passkeys-with-spring-security-explained.m4a` | audio only |
| `passkeys-with-spring-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Passkeys with Spring Security 7.1 and WebAuthn4J in a bank: registration with a challenge, a relying party id and a COSE public key the bank keeps, sign-in with a fresh challenge that is signed once, a look-alike site whose origin and relying party id the browser and the bank refuse, the defaults that accept a touch without a fingerprint and a cloned key, a wrapper that requires user verification and suspends a passkey whose counter goes backwards, synced passkeys with a counter of zero, a lost phone, removing its passkey, and switching passwords off for customers with two passkeys.
