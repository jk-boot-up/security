# Password Attacks and Defences with nginx — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `password-attacks-and-defences-with-nginx-explained.mp4` | the video, 1920×1080 |
| `password-attacks-and-defences-with-nginx-explained.m4a` | audio only |
| `password-attacks-and-defences-with-nginx-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Password attacks and defences with nginx, in Java: a rate limit is a bouncer at the door, and what it counts decides whether it works. Explained with an online bank's login behind a real nginx container, like a door guard who counts visitors by their face and not by the name they give. We send twenty guesses at the login with no limit, then with limit_req keyed on the client's address, then keyed on an X-Forwarded-For header the attacker writes, then on the real address again, and finish with the bill: a limit per username locks the owner out too, and a spray of different usernames never reaches it.
