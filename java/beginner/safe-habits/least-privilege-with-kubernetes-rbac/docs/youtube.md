# YouTube sheet — Least Privilege with Kubernetes RBAC

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Least Privilege with Kubernetes RBAC (cluster-admin, Wildcards, Escalation)

## Description

Least privilege with Kubernetes RBAC, in Java: every service account is a key, and the question is how many doors it opens. Explained with an online store, like a master key given to the night cleaner. We compare a nightly export job given cluster-admin with one scoped to two actions, fall into the wildcard trap where read everything includes secrets, find the escalation paths of create pods, bind and escalate, see that a namespaced binding keeps a cluster-wide role inside its namespace, and lint the manifests. The Java evaluator is checked against a real kind cluster and kubectl auth can-i.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/least-privilege-with-kubernetes-rbac

Chapters:

```
00:00 Introduction
01:06 The Scenario
01:28 The Words
01:54 Act One — The nightly job that was given cluster-admin
02:27 Act Two — The wildcard trap: read everything
02:57 Act Three — Ways to become more powerful
03:30 Act Four — A cluster-wide role through a namespaced binding
03:57 Act Five — A linter for the habits that cause incidents
04:29 From Broad to Narrow
04:59 Who Does What
05:31 Where You Have Seen It
05:57 How To Use It
06:24 Thanks for Watching
```

## Tags

kubernetes, rbac, least privilege, service account, cluster-admin, kind, kubectl, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/least-privilege-with-kubernetes-rbac-explained.mp4` |
| Captions | `video/least-privilege-with-kubernetes-rbac-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (107)
- [ ] description is at most 5,000 characters — ok (598)
- [ ] chapters start at 00:00 and there are at least three
