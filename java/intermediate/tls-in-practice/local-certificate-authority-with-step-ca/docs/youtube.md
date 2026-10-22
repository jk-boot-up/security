# YouTube sheet — A Local Certificate Authority with step-ca

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

A Local Certificate Authority with step-ca: Tokens, ACME, Renewal and Revocation (OWASP A04)

## Description

A real step-ca 0.3x with the step command: a root and an issuing authority, a client that must be given the root, one-time provisioner tokens that work once and only for their name, a name policy and a lifetime limit, the ACME provisioner answering challenges on port 80, renewal with the old key and with a new key, a key that cannot renew for another certificate, revocation that stops renewal and appears in a signed list, a client that never checks, a client that fails closed without a list, and what the CA's machine and password mean.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/local-certificate-authority-with-step-ca

Chapters:

```
00:00 Introduction
01:27 The Scenario
01:46 Act One — A certificate authority that runs
02:02 Act Two — A certificate for a service, with a token
02:21 Act Three — A certificate for a service, with ACME
02:39 Act Four — Renewing before it ends
02:55 Act Five — Revocation, and the bill
03:18 What It Costs
03:43 Who Does What
03:56 Where You Have Seen It
04:10 How To Use It
04:30 Thanks for Watching
```

## Tags

step-ca, smallstep, acme, certificate authority, tls, revocation, crl, owasp top 10, a04, java

## Files

| What | File |
| --- | --- |
| Video | `video/local-certificate-authority-with-step-ca-explained.mp4` |
| Captions | `video/local-certificate-authority-with-step-ca-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (92)
- [ ] tags are at most 500 characters in total — ok (85)
- [ ] description is at most 5,000 characters — ok (541)
- [ ] chapters start at 00:00 and there are at least three
