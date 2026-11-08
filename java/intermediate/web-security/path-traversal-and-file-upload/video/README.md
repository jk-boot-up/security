# Path Traversal, File Upload and Open Redirects — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `path-traversal-and-file-upload-explained.mp4` | the video, 1920×1080 |
| `path-traversal-and-file-upload-explained.m4a` | audio only |
| `path-traversal-and-file-upload-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Path traversal, unsafe file upload and open redirects, in Java. Three everyday features take a name or a link from a customer: download an invoice, upload a product photo, and return to a page after signing in. A file name with two dots walks out of its folder and reads the database settings, an uploaded file keeps the name and contents the customer chose, and a return address can send a signed-in customer to a look-alike site. Explained with an online store, like a cloakroom ticket that someone has rewritten to point at the manager's office. We watch a text check on the path fail against a folder with a similar name and a symbolic link, then compare paths as paths and follow links first, let the server decide each upload's name and type from its bytes and keep it outside the web root, and send customers only to paths on our own site.
