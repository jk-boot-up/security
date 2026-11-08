# Path Traversal and File Upload with Spring Boot — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `path-traversal-and-file-upload-with-spring-boot-explained.mp4` | the video, 1920×1080 |
| `path-traversal-and-file-upload-with-spring-boot-explained.m4a` | audio only |
| `path-traversal-and-file-upload-with-spring-boot-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Path traversal and file upload in Spring Boot 4, in Java. Spring hands a controller the customer's file name exactly as it was sent, in a query parameter or from MultipartFile.getOriginalFilename, so a name with two dots walks out of the invoice folder, a text check lets a folder with a similar name through, and an upload keeps a name that overwrites the settings. Explained with an online store, like a cloakroom ticket that someone has rewritten to point at the manager's office. We see that Spring's own static handler refuses two dots but follows a symbolic link out of its folder, then fix the shop with a name pattern, a real-path check, server-made names, a type decided from the bytes, downloads with nosniff and multipart size limits, and a return address that is only ever a path on the site.
