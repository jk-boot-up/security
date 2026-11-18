# YouTube sheet — Kubernetes Admission Control

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Kubernetes Admission Control: Kyverno Policies, Signed Images and Exceptions on a Real Cluster

## Description

Kubernetes admission control with Kyverno on a real kind cluster for a bank: validating policies, audit mode and policy reports, a mutating policy, cosign-signed images by digest, time-limited exceptions and what happens when the controller is down.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/kubernetes-and-container-security/kubernetes-admission-control

Chapters:

```
00:00 Introduction
00:53 The Scenario
01:25 The Words
02:08 What the Letters Stand For
03:04 One request, every policy
04:03 Ten requests, and how an image name is read
05:27 Act One — Policies that refuse
05:40 Act One — Policies that refuse
06:03 Act One — Policies that refuse
06:19 Act One — Policies that refuse
06:37 Audit first: the report shows what admission cannot
07:36 Act Two — What was there before, and audit mode
07:59 Before and after the mutation
08:58 Act Three — A policy that changes the request
09:20 One exception, four requests
10:15 Act Four — Exceptions
10:34 Kyverno stopped: the API server asks no one
11:20 Act Five — When the admission controller is down
11:38 What It Costs
12:12 Where You Have Seen It
12:44 How To Use It
13:20 The Java Classes
13:36 Cluster — its own kubeconfig, and timeouts tried again
13:57 Registry — push, then read the digest the registry gives
14:14 Kyverno — a pinned release, checked before it is applied
14:31 Verdict — admitted, or refused by which policies
14:44 TagRule — a colon before the last slash is a port
14:59 Thanks for Watching
```

## Tags

kubernetes admission control, kyverno, admission webhook, cosign, signed images, policy report, kind, kubernetes security, devsecops, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/kubernetes-admission-control-explained.mp4` |
| Captions | `video/kubernetes-admission-control-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Kubernetes and Container Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (94)
- [ ] tags are at most 500 characters in total — ok (180)
- [ ] description is at most 5,000 characters — ok (249)
- [ ] chapters start at 00:00 and there are at least three
