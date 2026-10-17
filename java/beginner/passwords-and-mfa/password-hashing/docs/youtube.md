# YouTube sheet — Password Hashing

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Password Hashing Explained in Java

## Description

Password hashing in Java: a shop should never store passwords as they are. Store a hash, which is a one-way scramble, add a salt so identical passwords look different, and make the hash slow so every guess costs real work. Explained with an online store's customer accounts, like a safe that is hard and slow to crack even when the thief has carried it home. We watch four ways to store five passwords, from plain text to PBKDF2, count the attacker's work in steps instead of seconds, and finish with the cost: every login pays for the slow hash, and old hashes must be upgraded.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/passwords-and-mfa/password-hashing

Chapters:

```
00:00 Introduction
01:21 The Scenario
01:55 The Words
02:29 Act One — Passwords stored as they are
03:01 Act Two — A fast hash, with no salt
03:50 Act Three — A fast hash, with a salt for each account
04:39 Act Four — A slow hash, with a salt for each account
05:36 Act Five — The bill: every login pays, and old hashes must be upgraded
06:32 Four Ways, Compared
07:15 Who Does What
07:53 Where You Have Seen It
08:26 How To Use It
08:58 Thanks for Watching
```

## Tags

password hashing, salt, pbkdf2, password storage, security basics, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/password-hashing-explained.mp4` |
| Captions | `video/password-hashing-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (34)
- [ ] tags are at most 500 characters in total — ok (94)
- [ ] description is at most 5,000 characters — ok (579)
- [ ] chapters start at 00:00 and there are at least three
