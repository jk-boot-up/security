# Fuzzing with Jazzer — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `fuzzing-with-jazzer-explained.mp4` | the video, 1920×1080 |
| `fuzzing-with-jazzer-explained.m4a` | audio only |
| `fuzzing-with-jazzer-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> The real Jazzer 0.30 (libFuzzer for the JVM) fuzzing an online store's order line parser and cart parser: a fuzz target of three lines, seed inputs from the example tests, a fixed random seed, three distinct crashes in the careless order line parser, a gift-message crash in the cart parser that needs a seed with a note field, coverage points with and without seeds, each crashing input made smallest and replayed as a regression test with the Jazzer JUnit integration, a sanitizer that reports a regular expression injection in a search box, and what a fuzzer does not find.
