# SBOMs and Build Provenance: What Is Inside, Who Built It, and From What — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `sbom-and-provenance-explained.mp4` | the video, 1920×1080 |
| `sbom-and-provenance-explained.m4a` | audio only |
| `sbom-and-provenance-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> SBOMs and build provenance in Java: an online store's release jar comes with a CycloneDX bill of materials that lists every library with its package URL and SHA-256, and finds a library changed after the build, but cannot say who built the jar or from which source. Signed provenance answers that: an in-toto statement with a SLSA Provenance predicate names the artifact by digest, the source repository, branch and commit, and the builder, signed with Ed25519 in a DSSE envelope. A deployment gate then refuses a build of a fork, an unreviewed branch, a laptop-signed build, a statement naming the wrong builder, an edited statement and a swapped jar. SLSA build levels are explained by what each adds, and reproducible builds let anyone rebuild the commit and check the digest. The bill: provenance helps only when checked, the builder's key must be protected, and an SBOM goes stale. Mapped to OWASP Top 10 2025 A03 and A08, SLSA, in-toto and NIST SSDF.
