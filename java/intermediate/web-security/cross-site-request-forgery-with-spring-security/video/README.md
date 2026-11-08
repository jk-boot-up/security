# Cross-Site Request Forgery with Spring Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cross-site-request-forgery-with-spring-security-explained.mp4` | the video, 1920×1080 |
| `cross-site-request-forgery-with-spring-security-explained.m4a` | audio only |
| `cross-site-request-forgery-with-spring-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Cross-site request forgery with Spring Security 7, in Java. A browser attaches a bank's session cookie to every request to the bank, even one sent by a page on another site, so a forged form can move a customer's money. Explained with an online bank, like a signed cheque that anyone could fill in, until the bank also asks for a code printed only in the customer's own cheque book. We switch the protection off to see the attack, then watch Spring Security's default token refuse it, use the cookie mode a single-page app needs, and find the three ways the defence is lost: a switch, an exemption, and a route that moves money on GET. We finish with the bill: SameSite is not set by default, and every state-changing route needs a test without a token.
