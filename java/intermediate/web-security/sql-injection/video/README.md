# SQL Injection and Its Defences — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `sql-injection-explained.mp4` | the video, 1920×1080 |
| `sql-injection-explained.m4a` | audio only |
| `sql-injection-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> SQL injection and its defences, in Java. When a search page pastes a customer's words into a SQL statement, a single quote can end the text the program opened and turn the rest of the words into commands: a hidden product appears, and a UNION adds the customers' e-mail addresses to the results. Explained with an online store and its product search, like a waiter who lets the customer dictate the kitchen's order slip word for word. We watch a working search fail, then fix it with parameters, choose the sort column from a fixed list, and connect as a database user that can read only products. We finish with the cost: names cannot be parameters, and least privilege needs a separate database user for each job.
