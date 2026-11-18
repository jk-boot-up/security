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
00:58 The Scenario
01:28 The Words
02:13 What the Letters Stand For
03:16 What the Letters Stand For (continued)
03:31 How a pod gets its name
04:33 The same call, before and after the proxies
05:43 Act One — Before and after the proxies
06:11 Act One — Before and after the proxies
06:28 The subject of each workload's certificate
07:21 Act Two — An identity from each ServiceAccount
07:39 Act Two — An identity from each ServiceAccount
07:46 Who answered, with mutual TLS and no policy
08:44 Act Three — Mutual TLS alone does not decide
08:59 Act Three — Mutual TLS alone does not decide
09:19 Who answered, with a policy that names one caller
10:13 Act Four — A policy names who may call
10:23 Act Four — A policy names who may call
10:32 Act Four — A policy names who may call
10:47 What the mesh still lets through, measured
11:51 Act Five — What the mesh does not protect
12:11 Act Five — What the mesh does not protect
12:31 A workload certificate's dates, and its renewal
13:34 Act Six — Certificate lifetime and renewal
14:06 What It Costs
14:47 Who Does What
15:29 Where You Have Seen It
16:00 How To Use It
16:29 The Java Classes
16:42 Service — the identity: five parts joined by dots
16:55 Lab — inject the proxies
17:06 Lab — every caller calls every service
17:17 Policies — a Server, then the one identity allowed
17:34 Design — unwanted and missing pairs
17:47 Metrics — the identity, or no identity
18:00 Wire — is the request there to read?
18:12 CertInfo — the lifetime, without the skew
18:25 Thanks for Watching
```

## Tags

service mesh, linkerd, mtls, workload identity, serviceaccount, authorization policy, zero trust, kubernetes, kind, certificate rotation, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/service-mesh-mtls-explained.mp4` |
| Captions | `video/service-mesh-mtls-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Service Identity and Zero Trust in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (79)
- [ ] tags are at most 500 characters in total — ok (178)
- [ ] description is at most 5,000 characters — ok (286)
- [ ] chapters start at 00:00 and there are at least three
