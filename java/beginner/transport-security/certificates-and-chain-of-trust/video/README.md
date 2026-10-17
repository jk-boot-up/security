# Certificates and the Chain of Trust — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `certificates-and-chain-of-trust-explained.mp4` | the video, 1920×1080 |
| `certificates-and-chain-of-trust-explained.m4a` | audio only |
| `certificates-and-chain-of-trust-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Certificates and the chain of trust in Java: how a browser knows that a public key really belongs to shop.example. Explained with an online store, like a passport signed by an office whose stamp is signed by a ministry you already trust. We build a root, an intermediate and a server certificate by writing their encoding byte by byte, validate the chain with the JDK, then watch it refuse an expired certificate, a certificate from an authority nobody trusts, a changed certificate, and a server certificate that tries to issue others. We check the host name as the separate step it is, and finish with the bill: revocation, and what a stolen authority key allows.
