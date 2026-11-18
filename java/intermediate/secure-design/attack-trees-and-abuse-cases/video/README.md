# Attack Trees and Abuse Cases — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `attack-trees-and-abuse-cases-explained.mp4` | the video, 1920×1080 |
| `attack-trees-and-abuse-cases-explained.m4a` | audio only |
| `attack-trees-and-abuse-cases-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Attack trees and abuse cases in Java for an online bank: one goal (move money out of Ana's account), fourteen attacker steps with a cost and a chance, AND and OR gates that give the cheapest path (50 EUR: change the account number) and the chance that some path works; eight priced defences, none enough on its own; a plan that cuts the cheapest path and looks again; and abuse cases written as checks that fail until the defence exists.
