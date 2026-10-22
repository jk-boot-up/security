# Input Validation with Hibernate Validator — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `input-validation-with-hibernate-validator-explained.mp4` | the video, 1920×1080 |
| `input-validation-with-hibernate-validator-explained.m4a` | audio only |
| `input-validation-with-hibernate-validator-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Hibernate Validator 9.1 with Jakarta Validation 3.1 on a checkout form written as Java records: constraints for quantity, sku, e-mail and country; all violations reported at once with their paths; a custom constraint for invisible characters and a class-level rule that checks the postal code against the country; method validation on a service; a group sequence that checks limits before shapes before a catalogue lookup; a cart of 10000 lines and a nested pattern stopped by a length limit; and what validation does not do, including message templates that evaluate expression language.
