# Least Privilege with Spring Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `least-privilege-with-spring-security-explained.mp4` | the video, 1920×1080 |
| `least-privilege-with-spring-security-explained.m4a` | audio only |
| `least-privilege-with-spring-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Least privilege with Spring Security, in Java: the easy way to build a feature is to give it admin power. Explained with an online store, like a hotel that hands every employee the master key. We start a real Spring Boot server, see that a path with no rule is denied even for an admin, guard refunds with method security and see what PostAuthorize really does, enforce two people for a refund with a rule that no role can switch off, compare a stolen admin token with a stolen scoped service token using Spring Security's own authorization manager, and give a month-end grant that expires by itself on an injected clock.
