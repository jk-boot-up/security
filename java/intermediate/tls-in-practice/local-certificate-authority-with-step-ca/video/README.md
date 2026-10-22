# A Local Certificate Authority with step-ca — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `local-certificate-authority-with-step-ca-explained.mp4` | the video, 1920×1080 |
| `local-certificate-authority-with-step-ca-explained.m4a` | audio only |
| `local-certificate-authority-with-step-ca-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A real step-ca 0.3x with the step command: a root and an issuing authority, a client that must be given the root, one-time provisioner tokens that work once and only for their name, a name policy and a lifetime limit, the ACME provisioner answering challenges on port 80, renewal with the old key and with a new key, a key that cannot renew for another certificate, revocation that stops renewal and appears in a signed list, a client that never checks, a client that fails closed without a list, and what the CA's machine and password mean.
