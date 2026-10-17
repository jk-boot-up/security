# Broken Access Control with Spring Security — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `broken-access-control-with-spring-security-explained.mp4` | the video, 1920×1080 |
| `broken-access-control-with-spring-security-explained.m4a` | audio only |
| `broken-access-control-with-spring-security-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Broken access control with Spring Security, OWASP A01, in Java. Explained with an online store, like a building with a good lock on every door the architect drew, and no lock on the doors added later. A real Spring Boot 4 store with three common mistakes lets seven of nine attacks through: a sign-in rule where an ownership rule was needed, a pattern that covers /admin/users but not /admin/users/ben, and permitAll() at the end. Then it is fixed with deny by default in the filter chain, the owner in the query, @PreAuthorize and @PostAuthorize compared, a log of every refusal from AuthorizationDeniedEvent, and a route inventory test that reads Spring MVC's own handler mapping and fails the build when a route has no rule.
