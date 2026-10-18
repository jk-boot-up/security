# Security Requirements with OWASP ASVS — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `security-requirements-with-asvs-explained.mp4` | the video, 1920×1080 |
| `security-requirements-with-asvs-explained.m4a` | audio only |
| `security-requirements-with-asvs-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Security requirements with OWASP ASVS, in Java, for OWASP Top 10 A06, insecure design. Explained with an online store and an online bank, like a building inspector's checklist that the builder runs every evening instead of once at the end. A small decision model puts a brochure site at L1, the store at L2 and the bank at L3, with its reasons; fourteen requirements are written down as data, each with its chapter, level and how it is verified; ten checks ask the running store questions, and nine pass; a coverage report shows what is met, failed and still manual, and a release with the debug flag on is refused; and the bill: a standard is a starting point, not a threat model, and people still review what no test can.
