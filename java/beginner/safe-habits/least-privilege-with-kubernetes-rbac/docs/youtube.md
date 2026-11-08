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
01:09 The Scenario
01:31 The Words
01:57 What the Letters Stand For
02:56 How RBAC gives a job a permission
03:59 56 things a token could do, before and after
05:19 Act One — The nightly job that was given cluster-admin
05:41 Act One — The nightly job that was given cluster-admin
05:54 What "read everything" really allows
07:00 Act Two — The wildcard trap: read everything
07:20 Four ways to more power than the role says
08:26 Act Three — Ways to become more powerful
08:50 The binding sets the scope, not the role
10:02 Act Four — A cluster-wide role through a namespaced binding
10:20 What the linter finds
11:22 Act Five — A linter for the habits that cause incidents
11:39 From Broad to Narrow
12:10 Where You Have Seen It
12:36 How To Use It
13:02 The Java Classes
13:14 Manifests — the four kinds of record
13:39 Rbac — one rule allows a request
13:56 Rbac — the binding sets the scope
14:09 Rbac — blast radius: count every pair
14:25 Linter — wildcards and escalation verbs
14:44 Thanks for Watching
```

## Tags

kubernetes, rbac, least privilege, service account, cluster-admin, kind, kubectl, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/least-privilege-with-kubernetes-rbac-explained.mp4` |
| Captions | `video/least-privilege-with-kubernetes-rbac-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (158)
- [ ] description is at most 5,000 characters — ok (598)
- [ ] chapters start at 00:00 and there are at least three
