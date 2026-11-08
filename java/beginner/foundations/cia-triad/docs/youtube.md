# YouTube sheet — The CIA Triad

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

The CIA Triad Explained in Java

## Description

The CIA triad in Java: three promises a system makes about its information. Confidentiality means only the right people can see it. Integrity means nobody can change it without being noticed. Availability means it is there when it is needed. Explained with an online store and its orders, like a bank vault with a thick door, sealed money bags and doors that open every morning. We watch a first version fail all three promises, then add a role check, a tamper seal and a rate limit, and finish with the cost: strict controls can get in the way of an emergency, so even the exception has to be logged.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/foundations/cia-triad

Chapters:

```
00:00 Introduction
01:21 The Scenario
01:52 The Three Promises
02:27 What the Letters Stand For
03:34 One order, three promises
04:31 No controls: three requests, three failures
05:40 Act One — The first version, with no controls
06:05 Each role sees only what it needs
07:06 Act Two — Confidentiality: each role sees only what it needs
07:26 Sealing one order
08:29 Act Three — Integrity: every order is saved with a seal
08:51 How the seal is made
10:15 Act Four — How the seal is made, step by step
10:36 Act Four — How the seal is made, step by step
10:49 An edit in storage breaks the seal
11:47 Act Five — A change behind the application is caught
12:00 Act Five — A change behind the application is caught
12:16 The rate limiter counts each client
13:23 Act Six — Availability: one client cannot use up the whole server
13:39 Act Six — Availability: one client cannot use up the whole server
13:53 Emergency access: a reason, and a log
14:57 Act Seven — The bill: strict controls get in the way of emergencies
15:12 Which Control Protects Which Promise
15:43 Where You Have Seen It
16:19 How To Use It
16:46 The Java Classes
16:55 CiaDemo — the seven acts
17:08 NaiveStore — no checks at all
17:25 SecureStore — read: the seal first, then the role
17:46 SecureStore — verified: work the seal out again
18:06 SecureStore — breakGlass: a reason, and a log
18:24 Seal — of and matches
18:44 SealSteps — the three steps
19:05 RateLimiter — count, then compare
19:21 TickServer — serve one tick
19:40 Thanks for Watching
```

## Tags

cia triad, confidentiality, integrity, availability, security basics, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cia-triad-explained.mp4` |
| Captions | `video/cia-triad-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (31)
- [ ] tags are at most 500 characters in total — ok (148)
- [ ] description is at most 5,000 characters — ok (601)
- [ ] chapters start at 00:00 and there are at least three
