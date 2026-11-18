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
00:51 The Scenario
01:22 The Words
02:01 What the Letters Stand For
03:11 How an application logs in
04:10 A policy, and six reads it decides
05:15 Act One — A secrets server, and who may ask
05:33 Act One — A secrets server, and who may ask
05:51 Versions through a changeover, and a hidden value
06:57 Act Two — Secrets that stay still: versions, rotation and the audit trail
07:18 Act Two — Secrets that stay still: versions, rotation and the audit trail
07:35 Two leased database logins
08:37 Act Three — Database logins that are leased
08:54 Act Three — Database logins that are leased
09:17 Rotate a static password, revoke leased logins
10:04 Act Four — Rotation, and the blast radius of a leak
10:25 Sealed means down
11:14 Act Five — The bill
11:21 Act Five — The bill
11:41 What It Costs
12:16 Where You Have Seen It
12:51 How To Use It
13:24 The Java Classes
13:36 BaoLab — pinned images, loopback only
13:52 Bao — every request carries the token
14:05 BaoSecretsDemo — one policy per application
14:18 BaoSecretsDemo — a login role, and a database role
14:35 BaoSecretsDemo — lease, renew
14:48 PolicyRule — a star covers what is below it
15:04 Thanks for Watching
```

## Tags

openbao, vault, secrets management, approle, dynamic secrets, database credentials, postgresql, owasp top 10, a02, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/secrets-management-with-openbao-explained.mp4` |
| Captions | `video/secrets-management-with-openbao-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (89)
- [ ] tags are at most 500 characters in total — ok (161)
- [ ] description is at most 5,000 characters — ok (513)
- [ ] chapters start at 00:00 and there are at least three
