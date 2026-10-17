# Digital Signatures with Bouncy Castle and OpenSSL — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `digital-signatures-with-bouncy-castle-explained.mp4` | the video, 1920×1080 |
| `digital-signatures-with-bouncy-castle-explained.m4a` | audio only |
| `digital-signatures-with-bouncy-castle-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Digital signatures with Bouncy Castle and OpenSSL, in Java: the JDK can sign, but real systems exchange signed messages in standard formats. Explained with an online bank, like a notary's stamp that any office in the world knows how to check. We sign a statement in the standard CMS format, attached and detached, see why ECDSA needs a nonce that never repeats and how deterministic signing removes the risk, reproduce the published Ed25519 and Ed448 vectors, build a compact signed token by hand and see why the verifier must choose the algorithm, and finish with the bill: a valid signature does not say who owns the key, and the signing time is only the signer's word.
