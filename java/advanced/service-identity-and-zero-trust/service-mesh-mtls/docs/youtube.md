# YouTube sheet — Service Mesh and Automatic mTLS

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Service Mesh mTLS with Linkerd: Identities, Policy and What It Does Not Protect

## Description

A service mesh with Linkerd on a real kind cluster: automatic mutual TLS, identities taken from ServiceAccounts, an authorization policy that names who may call, and the limits: pods outside the mesh, root on the node, application flaws, ServiceAccount control and certificate lifetime.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/service-identity-and-zero-trust/service-mesh-mtls

Chapters:

```
00:00 Introduction
02:49 The Scenario
03:07 Act One — Before and after the proxies
03:21 Act Two — Identities from ServiceAccounts
03:33 Act Three — Mutual TLS is not a decision
03:45 Act Four — A policy names who may call
03:57 Act Five — What the mesh does not protect
04:08 What It Costs
04:41 Who Does What
05:06 Where You Have Seen It
05:28 How To Use It
05:49 Thanks for Watching
```

## Tags

service mesh, linkerd, mtls, workload identity, serviceaccount, authorization policy, zero trust, kubernetes, kind, certificate rotation

## Files

| What | File |
| --- | --- |
| Video | `video/service-mesh-mtls-explained.mp4` |
| Captions | `video/service-mesh-mtls-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (79)
- [ ] tags are at most 500 characters in total — ok (127)
- [ ] description is at most 5,000 characters — ok (286)
- [ ] chapters start at 00:00 and there are at least three
