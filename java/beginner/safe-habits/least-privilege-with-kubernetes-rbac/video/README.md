# Least Privilege with Kubernetes RBAC — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `least-privilege-with-kubernetes-rbac-explained.mp4` | the video, 1920×1080 |
| `least-privilege-with-kubernetes-rbac-explained.m4a` | audio only |
| `least-privilege-with-kubernetes-rbac-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Least privilege with Kubernetes RBAC, in Java: every service account is a key, and the question is how many doors it opens. Explained with an online store, like a master key given to the night cleaner. We compare a nightly export job given cluster-admin with one scoped to two actions, fall into the wildcard trap where read everything includes secrets, find the escalation paths of create pods, bind and escalate, see that a namespaced binding keeps a cluster-wide role inside its namespace, and lint the manifests. The Java evaluator is checked against a real kind cluster and kubectl auth can-i.
