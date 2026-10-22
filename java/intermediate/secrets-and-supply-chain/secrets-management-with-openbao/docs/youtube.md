# YouTube sheet — Secrets Management with OpenBao

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Secrets Management with OpenBao: AppRole, Leased Database Logins, Rotation and Revocation

## Description

Secrets management with OpenBao 2.7 and a real PostgreSQL in Docker: AppRole logins with a secret id that works once, policies that name what each application may read, versioned secrets with a changeover and a destroyed version, an audit trail that hashes values, per-application database logins that really connect and really stop, renewal capped at a maximum, revoking one lease or all of an application's leases, a static password that OpenBao rotates, and a sealed server that the store cannot start without.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secrets-and-supply-chain/secrets-management-with-openbao

Chapters:

```
00:00 Introduction
01:35 The Scenario
02:02 Act One — A secrets server, and who may ask
02:32 Act Two — Secrets that stay still: versions, rotation and the audit trail
03:01 Act Three — Database logins that are leased
03:32 Act Four — Rotation, and the blast radius of a leak
03:59 Act Five — The bill
04:29 What It Costs
04:56 Who Does What
05:09 Where You Have Seen It
05:21 How To Use It
05:41 Thanks for Watching
```

## Tags

openbao, vault, secrets management, approle, dynamic secrets, database credentials, postgresql, owasp top 10, a02, java

## Files

| What | File |
| --- | --- |
| Video | `video/secrets-management-with-openbao-explained.mp4` |
| Captions | `video/secrets-management-with-openbao-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (89)
- [ ] tags are at most 500 characters in total — ok (110)
- [ ] description is at most 5,000 characters — ok (513)
- [ ] chapters start at 00:00 and there are at least three
