# Artifact Signing with cosign — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `artifact-signing-with-cosign-explained.mp4` | the video, 1920×1080 |
| `artifact-signing-with-cosign-explained.m4a` | audio only |
| `artifact-signing-with-cosign-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Signing container images with cosign 3 against a real registry in Docker: a tag moved to another image, a gate that resolves the tag to a digest and verifies a signature made by a pinned key, the signature stored next to the image, signed claims such as channel=stable, a beta build refused by the stable gate, key rotation with an overlap, a thief with a retired key, signed provenance statements with a rule of the gate's own, an attestation from the wrong key or for another digest, a pull that never meets the gate, and why keyless signing needs services a local lab does not have.
