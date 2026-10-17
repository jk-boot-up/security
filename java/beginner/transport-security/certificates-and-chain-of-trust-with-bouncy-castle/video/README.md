# Certificates and Chain of Trust with Bouncy Castle and OpenSSL — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `certificates-and-chain-of-trust-with-bouncy-castle-explained.mp4` | the video, 1920×1080 |
| `certificates-and-chain-of-trust-with-bouncy-castle-explained.m4a` | audio only |
| `certificates-and-chain-of-trust-with-bouncy-castle-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Certificates and chain of trust with Bouncy Castle and OpenSSL, in Java: the JDK checks certificates but cannot make them. Explained with an online store, like a passport office that issues passports, limits what each office may issue, and publishes a list of lost ones. We build a root, an intermediate and a server certificate with Bouncy Castle, limit the intermediate to the store's own names, publish signed revocation lists, send a signing request so the private key never leaves the shop, have the openssl command-line tool verify the same chain, and finish with the bill: a stolen authority key breaks everything below it, and every extension is a place to be quietly wrong.
