# Tokens in the Browser — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `tokens-in-the-browser-explained.mp4` | the video, 1920×1080 |
| `tokens-in-the-browser-explained.m4a` | audio only |
| `tokens-in-the-browser-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Where should a single-page app keep its OAuth tokens? In Java with only the JDK, explained with an online bank's banking app, like a hotel guest who leaves the room key on the restaurant table instead of at reception. We put the access and refresh tokens in localStorage and let a third-party script on the page read them and use the refresh token ten days later, move the OAuth client to a backend for the frontend that keeps the tokens on the server and gives the browser only a __Host- cookie, stop requests started by another site with SameSite and a request header, bind tokens to a browser key with DPoP so a stolen token is refused without a fresh proof, and finish with the bill: one more server to run, a shared session store, and a script on the open page that can still act as the user. Checked against the RFC 8037 thumbprint example.
