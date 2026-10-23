# YouTube sheet — Workload Identity

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Workload Identity with SPIFFE and SPIRE: Short-Lived SVIDs Instead of Shared Secrets

## Description

Workload identity for a bank's services with SPIFFE and SPIRE: a shared secret that leaks, SPIFFE IDs chosen by registration entries, 20 second X.509 SVIDs, mutual verification between two Java services, automatic rotation and a stolen SVID.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/service-identity-and-zero-trust/workload-identity

Chapters:

```
00:00 Introduction
01:52 The Scenario
02:14 Act One — One shared secret
02:26 Act Two — Identity by registration
02:40 Act Three — Two services check each other
02:53 Act Four — Rotation without a restart
03:05 Act Five — A stolen SVID
03:19 Act Six — No entry, no identity
03:33 What It Costs
04:16 Who Does What
04:39 Where You Have Seen It
04:58 How To Use It
05:17 Thanks for Watching
```

## Tags

spiffe, spire, workload identity, svid, mtls, zero trust, service identity, x509, secret rotation, java

## Files

| What | File |
| --- | --- |
| Video | `video/workload-identity-explained.mp4` |
| Captions | `video/workload-identity-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (84)
- [ ] tags are at most 500 characters in total — ok (94)
- [ ] description is at most 5,000 characters — ok (241)
- [ ] chapters start at 00:00 and there are at least three
