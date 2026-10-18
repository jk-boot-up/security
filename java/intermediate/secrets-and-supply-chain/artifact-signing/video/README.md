# Artifact Signing: Signed Releases, Key Rotation and Transparency Logs — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `artifact-signing-explained.mp4` | the video, 1920×1080 |
| `artifact-signing-explained.m4a` | audio only |
| `artifact-signing-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Artifact signing in Java: an online store's deployment downloads its release jar and runs whatever arrives, so a jar with one byte changed on a mirror goes live. Then a signed release statement, with the SHA-256 of the exact bytes and an Ed25519 signature checked against a pinned key, refuses a changed jar, an edited statement and a forged key. Signing keys are kept per channel, rotated with an overlap and revoked, and a backdated signature from a stolen key shows why the signer's own date cannot be trusted. A public, append-only Merkle log, with inclusion and consistency proofs as in Certificate Transparency, makes every signature visible: the store's monitor spots a release it never made, and a log that shows two different views is caught. The bill: verify at the point of use, protect the signing key, and consider keyless signing. Mapped to OWASP Top 10 2025 A08 and A03, CWE-494 and CWE-347, and SLSA.
