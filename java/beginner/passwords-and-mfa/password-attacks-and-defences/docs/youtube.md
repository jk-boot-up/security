# YouTube sheet — Password Attacks and Defences

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Password Attacks and Defences Explained in Java

## Description

Password attacks and defences in Java: attackers do not need to steal a database to guess passwords, they can simply try them at the login page. Explained with an online bank, like a night guard at a vault who must stop a patient thief without locking out the customers. We run a dictionary attack, password spraying and credential stuffing against a login with no defences, then add an account lockout, a rate limit per address, a second factor and a check against known breaches, count what each one stops and what it misses, and finish with the bill: a lockout can be turned into a denial of service, and error messages can reveal which usernames exist.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/passwords-and-mfa/password-attacks-and-defences

Chapters:

```
00:00 Introduction
01:18 The Scenario
01:53 Three Attacks
02:22 Act One — No defences
03:07 Act Two — Lock an account after 5 wrong passwords, for 10 ticks
03:50 Act Three — Allow each address 5 attempts in every 10 ticks
04:24 Act Four — Credential stuffing: passwords leaked from another site
05:24 Act Five — The bill: a lock can be used against the customer, and messages can leak
06:24 What Stops What
06:55 Who Does What
07:24 Where You Have Seen It
07:56 How To Use Them
08:21 Thanks for Watching
```

## Tags

password attacks, brute force, credential stuffing, password spraying, rate limiting, account lockout, security basics, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/password-attacks-and-defences-explained.mp4` |
| Captions | `video/password-attacks-and-defences-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (47)
- [ ] tags are at most 500 characters in total — ok (145)
- [ ] description is at most 5,000 characters — ok (656)
- [ ] chapters start at 00:00 and there are at least three
