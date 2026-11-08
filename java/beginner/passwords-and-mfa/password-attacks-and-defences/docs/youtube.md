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
00:49 The Scenario
01:14 The Words
01:42 What the Letters Stand For
02:24 One guess, checked
03:13 Deep on one account, or wide over all of them
03:55 Act One — No defences
04:11 Act One — No defences
04:29 How a lockout turns tick 13 into tick 33
04:50 Act Two — Lock an account after 5 wrong passwords, for 10 ticks
05:15 A rate limit: 5 attempts in every 10 ticks
05:56 Act Three — Allow each address 5 attempts in every 10 ticks
06:15 Ten leaked pairs against three sets of defences
07:05 Act Four — Credential stuffing: passwords leaked from another site
07:27 What the defences cost
08:13 Act Five — The bill: what the defences cost
08:26 Act Five — The bill: what the defences cost
08:40 What Stops What
09:05 How To Use It
09:25 The Java Classes
09:34 AttacksDemo — a defence, an attack, the steps
09:47 Bank — refused before the password is checked
10:04 Bank — the check, the lock, and the last two defences
10:19 Defences — zero means off
10:32 Attacks — a refused attempt is tried again
10:49 Timeline — runs of the same answer
11:06 Reply and Outcome — seven possible answers
11:17 Ticker — no waiting, the same every run
11:26 Thanks for Watching
```

## Tags

password attacks, brute force, credential stuffing, password spraying, rate limiting, account lockout, security basics, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/password-attacks-and-defences-explained.mp4` |
| Captions | `video/password-attacks-and-defences-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Passwords and MFA in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (47)
- [ ] tags are at most 500 characters in total — ok (196)
- [ ] description is at most 5,000 characters — ok (656)
- [ ] chapters start at 00:00 and there are at least three
