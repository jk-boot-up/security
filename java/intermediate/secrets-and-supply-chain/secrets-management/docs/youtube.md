# YouTube sheet — Secrets Management

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Secrets Management in Java (Leaks, Run-Time Injection, Leases, Rotation)

## Description

Secrets management in Java: where database passwords and API keys leak, and how a secrets service hands out short-lived logins instead. Explained with an online store, like a hotel that gives each guest a key card that stops working at checkout, instead of copying one master key for everyone. A small scanner finds secrets committed to a repository, a child process reads a key from the environment, and a deleted file is still in an image layer. Then secrets are injected at run time from owner-only files and redacted from logs, a secrets service leases a separate database login to each application with a time to live, a payment key is rotated with no downtime, and the bill: the secrets service itself becomes the most valuable thing to protect.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/secrets-management

Chapters:

```
00:00 Introduction
01:09 The Scenario
01:29 The Words
02:00 Act One — Where secrets leak
02:44 Act Two — Secrets injected at run time
03:18 Act Three — A secrets service that hands out leased logins
04:09 Act Four — Rotation without downtime, and the blast radius of a leak
04:53 Act Five — The bill
05:28 From Worst to Best
05:58 Who Does What
06:28 Where You Have Seen It
06:54 How To Use It
07:24 Thanks for Watching
```

## Tags

secrets management, api keys, secret scanning, secret rotation, dynamic credentials, openbao, vault, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/secrets-management-explained.mp4` |
| Captions | `video/secrets-management-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (119)
- [ ] description is at most 5,000 characters — ok (751)
- [ ] chapters start at 00:00 and there are at least three
