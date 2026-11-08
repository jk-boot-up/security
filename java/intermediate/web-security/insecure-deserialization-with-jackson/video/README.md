# Insecure Deserialization with Jackson — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `insecure-deserialization-with-jackson-explained.mp4` | the video, 1920×1080 |
| `insecure-deserialization-with-jackson-explained.m4a` | audio only |
| `insecure-deserialization-with-jackson-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Insecure deserialization with Jackson 3, in Java. JSON is only data until a type id tells Jackson which class to build: when a field is typed by a class name taken from the data, whoever writes the cookie chooses the class, and a class from another team's plug-in can delete a file just by being built. Explained with an online store's cart cookie, like a flat-pack kit that names its own parts, and a factory that will fetch any part named on the box. We see that Jackson 3 refuses class names for an Object field by default, but not for an ordinary interface or with global default typing, then fix it three ways: short names from a list, a validator that lists the allowed classes, or no types in the data at all. We finish with data-only requests priced by the shop, strict binding that refuses a forged price, and limits on nesting depth and length.
