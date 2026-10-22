# Attack Trees and Abuse Cases in Threat Dragon — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `attack-trees-and-abuse-cases-with-threat-dragon-explained.mp4` | the video, 1920×1080 |
| `attack-trees-and-abuse-cases-with-threat-dragon-explained.m4a` | audio only |
| `attack-trees-and-abuse-cases-with-threat-dragon-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The online bank's attack tree turned into an OWASP Threat Dragon 2.6 threat model by a Java program: one diagram with two trust boundaries, actors, processes, a store and flows, and one STRIDE threat for each of the eight abuse cases with a severity and a score from the tree; the real threat-dragon image's own schema and Ajv validating the file and refusing four broken variants; the plan of four defences taking the open threats from 8 to 2 in the file, in step with the abuse case tests; checks the schema does not make; and the editor's server running from the image on a loopback port.
