# Secrets Management with OpenBao — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `secrets-management-with-openbao-explained.mp4` | the video, 1920×1080 |
| `secrets-management-with-openbao-explained.m4a` | audio only |
| `secrets-management-with-openbao-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Secrets management with OpenBao 2.7 and a real PostgreSQL in Docker: AppRole logins with a secret id that works once, policies that name what each application may read, versioned secrets with a changeover and a destroyed version, an audit trail that hashes values, per-application database logins that really connect and really stop, renewal capped at a maximum, revoking one lease or all of an application's leases, a static password that OpenBao rotates, and a sealed server that the store cannot start without.
