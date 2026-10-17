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
01:26 The Scenario
01:56 The Words
02:25 Act One — nginx in front of the login, with no limit
02:58 Act Two — limit_req on the client's address: 1r/m, burst 5
03:37 Act Three — The same limit, but counted on the X-Forwarded-For header
04:12 Act Four — The same limit, counted on the real connection address
04:46 Act Five — The bill: a limit per username
05:32 Choosing the Key
06:06 Who Does What
06:37 Where You Have Seen It
07:07 How To Use It
07:31 Thanks for Watching
```

## Tags

nginx, rate limiting, limit_req, brute force, x-forwarded-for, password attacks, docker, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/password-attacks-and-defences-with-nginx-explained.mp4` |
| Captions | `video/password-attacks-and-defences-with-nginx-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (32)
- [ ] tags are at most 500 characters in total — ok (114)
- [ ] description is at most 5,000 characters — ok (606)
- [ ] chapters start at 00:00 and there are at least three
