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
02:00 What the Letters Stand For
02:24 One leak, four outcomes
03:17 Three places a secret leaks from
04:24 Act One — Where secrets leak
04:43 Act One — Where secrets leak
05:12 Act One — Where secrets leak
05:35 The secret file's permissions, before and after the fix
06:33 Act Two — Secrets injected at run time
07:03 Two leases on one time line
08:05 Act Three — A secrets service that hands out leased logins
08:30 Act Three — A secrets service that hands out leased logins
08:45 Act Three — A secrets service that hands out leased logins
08:56 A key rotation in three steps
09:44 Act Four — Rotation without downtime, and the blast radius of a leak
10:17 What it costs
11:01 Act Five — The bill
11:13 From Worst to Best
11:43 Where You Have Seen It
12:09 How To Use It
12:39 The Java Classes
12:50 Secret — never printed, wiped after use
13:05 SecretScanner — a name, or a random-looking value
13:21 SecretScanner — the entropy sum
13:38 ImageLayers — a delete is a new layer
13:53 MountedSecrets — refuse a file others can read
14:05 SecretsService — a new login for each lease
14:17 SecretsService — a renewal never passes the maximum
14:28 PaymentProvider — any active version works
14:40 Thanks for Watching
```

## Tags

secrets management, api keys, secret scanning, secret rotation, dynamic credentials, openbao, vault, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/secrets-management-explained.mp4` |
| Captions | `video/secrets-management-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secrets and Supply Chain Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (170)
- [ ] description is at most 5,000 characters — ok (751)
- [ ] chapters start at 00:00 and there are at least three
