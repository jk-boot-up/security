# Cross-Site Scripting: Output Encoding and CSP — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cross-site-scripting-explained.mp4` | the video, 1920×1080 |
| `cross-site-scripting-explained.m4a` | audio only |
| `cross-site-scripting-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Cross-site scripting (XSS) explained in Java, with the defences that work: contextual output encoding and a strict Content-Security-Policy. Explained with an online store's product reviews, like a shop notice board where anyone's note could be mistaken for the shop's own sign. We see a review change the page, write an encoder for each place text goes (between tags, in an attribute, in a URL, in a script), see why filtering input is not the fix, serve a page with a fresh nonce and a strict policy that refuses any script without it, and finish with the bill: every output context needs its encoder, and the policy needs care.
