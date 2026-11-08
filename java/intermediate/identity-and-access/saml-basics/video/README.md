# SAML Single Sign-On and Signature Wrapping — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `saml-basics-explained.mp4` | the video, 1920×1080 |
| `saml-basics-explained.m4a` | audio only |
| `saml-basics-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> SAML single sign-on in Java with real XML signatures (RSA-SHA256, enveloped, exclusive canonicalization): a shop that signs customers in through their bank, a provider that trusts the key inside the message and accepts a self-signed admin, XML signature wrapping that gets a pinned-key provider to check one assertion and read another, and a strict provider that pins the key, reads only what the signature covers, and checks expiry, audience, the request, replay and DOCTYPEs.
