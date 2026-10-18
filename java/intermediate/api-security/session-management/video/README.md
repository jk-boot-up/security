# Session Management: Ids, Rotation, Timeouts and Sign-Out — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `session-management-explained.mp4` | the video, 1920×1080 |
| `session-management-explained.m4a` | audio only |
| `session-management-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Session management in Java: after a customer signs in, a session id in a cookie is all that proves who they are, so it is as good as a password for as long as it lives. Explained with an online bank. A session id is 128 random bits, stored only as its hash; a bank that keeps the same id at sign-in lets a stranger who planted that id read the account (session fixation), and issuing a new id at sign-in and at step-up stops it; idle and absolute timeouts end sessions, and payments need the password again; sign-out ends the session on the server, everywhere, and after a password change, with a limit on sessions; and the cookie carries Secure, HttpOnly, SameSite=Strict and the __Host- prefix. The bill: many servers need a shared session store, and timeouts cost convenience. Mapped to OWASP Top 10 2025 A07, CWE-384 and CWE-613.
