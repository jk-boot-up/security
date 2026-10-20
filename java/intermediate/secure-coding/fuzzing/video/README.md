# Fuzzing: Finding the Inputs Nobody Tested — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `fuzzing-explained.mp4` | the video, 1920×1080 |
| `fuzzing-explained.m4a` | audio only |
| `fuzzing-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Fuzzing in Java, explained from first principles with an online store, like a new employee who presses every button on the till in every order until something breaks. The store's order-import and cart parsers pass all their example tests, yet a mutation fuzzer finds a missing-field crash, an integer overflow and a stack overflow from deep nesting. Guided by coverage, keeping every input that reaches a new branch, it then finds a gift-message bug that blind mutation never reaches in a hundred thousand runs. Each crash is shrunk to its smallest input by delta debugging and kept as a regression test, the fixed parsers survive the same fuzzer, a property check confirms that parsing and printing agree, and the bill: fuzzing finds crashes, not wrong business decisions.
