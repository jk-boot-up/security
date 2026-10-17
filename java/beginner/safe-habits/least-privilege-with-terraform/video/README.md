# Least Privilege with Terraform and LocalStack — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `least-privilege-with-terraform-explained.mp4` | the video, 1920×1080 |
| `least-privilege-with-terraform-explained.m4a` | audio only |
| `least-privilege-with-terraform-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Least privilege with Terraform and LocalStack, in Java: cloud permissions are code, so they can be checked like code. Explained with an online store, like a key cabinet where the nightly export gets a key to one drawer and not the master key. We write AWS IAM policies in Terraform, apply them with OpenTofu to a local AWS emulator in Docker, read the same policies back in Java and count the blast radius of each, see how wildcard matching and the difference between a bucket and its contents trips people up, watch an explicit deny and a permissions boundary narrow an over-broad policy, and run a linter that catches iam PassRole on every role. Everything runs on your own machine, with no cloud account.
