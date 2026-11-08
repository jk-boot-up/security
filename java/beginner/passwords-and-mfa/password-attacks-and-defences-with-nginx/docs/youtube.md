# YouTube sheet — Password Attacks and Defences with nginx

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Rate Limiting a Login with nginx

## Description

Password attacks and defences with nginx, in Java: a rate limit is a bouncer at the door, and what it counts decides whether it works. Explained with an online bank's login behind a real nginx container, like a door guard who counts visitors by their face and not by the name they give. We send twenty guesses at the login with no limit, then with limit_req keyed on the client's address, then keyed on an X-Forwarded-For header the attacker writes, then on the real address again, and finish with the bill: a limit per username locks the owner out too, and a spray of different usernames never reaches it.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/passwords-and-mfa/password-attacks-and-defences-with-nginx

Chapters:

```
00:00 Introduction
01:23 The Scenario
01:53 The Words
02:23 What the Letters Stand For
03:19 One request, three possible keys
04:28 No limit: every guess reaches the login
05:21 Act One — nginx in front of the login, with no limit
05:39 How the limit decides, guess by guess
06:50 Act Two — limit_req on the client's address: 1r/m, burst 5
07:07 Keyed on a header: the attacker picks the key
08:02 Act Three — The same limit, but counted on the X-Forwarded-For header
08:17 Keyed on the real address: the header is ignored
09:10 Act Four — The same limit, counted on the real connection address
09:23 Keyed on the username: the owner pays
10:22 Act Five — The bill: a limit per username
10:43 Where You Have Seen It
11:10 How To Use It
11:44 The Java Classes
11:52 NginxAttacksDemo — one act: start nginx, then attack
12:09 NginxConfig — the key, and the two limit lines
12:31 Nginx — start a pinned image with that file
12:46 Attacker — one guess, and the count
13:05 LeakyBucket — the excess for one key
13:28 Thanks for Watching
```

## Tags

nginx, rate limiting, limit_req, brute force, x-forwarded-for, password attacks, docker, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/password-attacks-and-defences-with-nginx-explained.mp4` |
| Captions | `video/password-attacks-and-defences-with-nginx-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Passwords and MFA in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (32)
- [ ] tags are at most 500 characters in total — ok (165)
- [ ] description is at most 5,000 characters — ok (606)
- [ ] chapters start at 00:00 and there are at least three
