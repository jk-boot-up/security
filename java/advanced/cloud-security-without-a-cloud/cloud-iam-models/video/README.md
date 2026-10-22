# Cloud IAM Models — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `cloud-iam-models-explained.mp4` | the video, 1920×1080 |
| `cloud-iam-models-explained.m4a` | audio only |
| `cloud-iam-models-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A cloud permission evaluator written in Java 25 for the policy language of the big providers: identity policies, explicit deny, NotAction, wildcards, conditions and policy variables; resource policies and the difference between one account and two; organization policies; roles with trust policies that ask for a second factor; permission boundaries and session policies that only narrow; a resource policy that names a session; the confused deputy attack on a reporting service that assumes roles in its customers' accounts and the external id that stops it; the same idea between services with a source ARN; and a search for privilege escalation paths.
