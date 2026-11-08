# YouTube sheet — Authentication vs Authorization

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Authentication vs Authorization in Java

## Description

Authentication vs authorization in Java: authentication finds out who is asking, and authorization decides what that person may do. Explained with an online store, like a hotel where the front desk checks your passport and the key card decides which doors open. We watch a shop with no checks, add a login with session tokens, see why a logged-in customer can still refund orders, add roles and ownership rules with the difference between a 401 and a 403, and finish with the cost: sessions expire and roles copied at login can go stale.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/identity/authentication-vs-authorization

Chapters:

```
00:00 Introduction
01:14 The Scenario
01:45 Two Questions
02:26 What the Letters Stand For
03:22 What the Letters Stand For (continued)
03:41 One gate, two questions
04:43 No checks: every request gets through
05:32 Act One — The first version, with no checks
05:52 Authentication: a password in, a token out
07:02 Act Two — Authentication: finding out who is asking
07:28 How the password is checked
08:41 Act Three — How a password is checked, step by step
09:08 A login, and nothing else
09:59 Act Four — Authenticated is not the same as authorized
10:17 Two questions: who, then may they
11:24 Act Five — Authorization: what may this person do?
11:34 Act Five — Authorization: what may this person do?
11:48 How the gate decides one request
12:53 Act Six — How the gate decides, step by step
13:03 Act Six — How the gate decides, step by step
13:15 What the checks cost
14:21 Act Seven — The bill: sessions expire, and roles go stale
14:45 The Gate Asks In Order
15:17 Where You Have Seen It
15:47 How To Use It
16:17 The Java Classes
16:26 AuthDemo — the seven acts
16:39 Credentials — hash, and verify
17:01 Authenticator — login, then authenticate each request
17:21 LoginOnlyGate — any live session passes
17:34 PolicyGate — check: 401, then 403
17:52 PolicyGate — allowed: the rules, in one place
18:12 UserDirectory — find counts the lookups
18:24 Status — two different failures
18:35 Thanks for Watching
```

## Tags

authentication, authorization, 401 vs 403, sessions, access control, security basics, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/authentication-vs-authorization-explained.mp4` |
| Captions | `video/authentication-vs-authorization-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity Basics in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (39)
- [ ] tags are at most 500 characters in total — ok (163)
- [ ] description is at most 5,000 characters — ok (537)
- [ ] chapters start at 00:00 and there are at least three
