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
01:03 The Scenario
01:26 The Words
01:46 What the Letters Stand For
02:36 What a hash does to one password
03:14 The row holds the password itself
03:42 Act One — Passwords stored as they are
03:53 No salt: one table cracks every account
04:28 Act Two — A fast hash, with no salt
04:47 How a salted hash is made
05:30 Act Three — A fast hash, with a salt for each account
05:45 Act Three — A fast hash, with a salt for each account
05:57 What one hundred thousand rounds are
06:44 Act Four — A slow hash, with a salt for each account
07:00 Act Four — A slow hash, with a salt for each account
07:18 Act Four — A slow hash, with a salt for each account
07:33 The bill, and an upgrade at login
08:09 Act Five — The bill: login costs, and upgrades
08:26 Four Ways, Compared
08:49 Where You Have Seen It
09:13 How To Use It
09:38 The Java Classes
09:44 PasswordHasher — one interface, four ways
09:55 SaltedSha256 — salt, then password, then SHA-256
10:09 Pbkdf2Hasher — the rounds are stored with the hash
10:30 RoundSteps — each round from the one before
10:45 AccountStore — upgrade after a correct password
10:59 Attacker — a table, or guesses per account
11:11 SaltSource — secure in real use, counting in the demo
11:29 Thanks for Watching
```

## Tags

password hashing, salt, pbkdf2, password storage, security basics, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/password-hashing-explained.mp4` |
| Captions | `video/password-hashing-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Passwords and MFA in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (34)
- [ ] tags are at most 500 characters in total — ok (145)
- [ ] description is at most 5,000 characters — ok (579)
- [ ] chapters start at 00:00 and there are at least three
