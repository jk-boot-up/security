# SBOMs and Scanning with syft and grype — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `sbom-and-provenance-with-syft-and-grype-explained.mp4` | the video, 1920×1080 |
| `sbom-and-provenance-with-syft-and-grype-explained.m4a` | audio only |
| `sbom-and-provenance-with-syft-and-grype-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> SBOMs and vulnerability scanning with the real syft and grype commands: images built here with log4j 2.14.1, 2.16.0 and 2.25.5, a CycloneDX bill of materials with package URLs, grype matching log4j-shell and its follow-ups with the versions that fix them, a scan of the bill alone, a diff of two bills, a deployment gate with time-limited exceptions that cannot cover a critical advisory, a renamed jar that no scanner finds, a stale bill, and the age of the vulnerability database.
