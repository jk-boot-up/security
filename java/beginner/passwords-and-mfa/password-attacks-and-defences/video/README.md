# Password Attacks and Defences — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `password-attacks-and-defences-explained.mp4` | the video, 1920×1080 |
| `password-attacks-and-defences-explained.m4a` | audio only |
| `password-attacks-and-defences-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Password attacks and defences in Java: attackers do not need to steal a database to guess passwords, they can simply try them at the login page. Explained with an online bank, like a night guard at a vault who must stop a patient thief without locking out the customers. We run a dictionary attack, password spraying and credential stuffing against a login with no defences, then add an account lockout, a rate limit per address, a second factor and a check against known breaches, count what each one stops and what it misses, and finish with the bill: a lockout can be turned into a denial of service, and error messages can reveal which usernames exist.
