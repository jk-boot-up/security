# YouTube sheet — An HTTPS Server, Put Together

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

An HTTPS Server in Java (TLS, Redirect, HSTS, Secure Cookies)

## Description

An HTTPS server put together in Java: certificates, TLS, redirects, HSTS and cookies in one working shop. Explained with an online store, like a shop with a strong front door, a sign that points everyone to it, and a rule that valuables never leave through the back. We start a real HTTPS server on this machine, make a checked request, see the plain port only redirect, keep a session cookie on HTTPS with the Secure flag and watch an unflagged cookie leak, see the client refuse an untrusted certificate, a wrong name and a downgrade, and score a careful shop against a careless one on five checks.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/transport-security/https-server

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:29 The Words
02:02 Act One — An HTTPS request that is checked
02:32 Act Two — Plain HTTP only redirects
03:04 Act Three — A cookie that stays on HTTPS
03:34 Act Four — What the client refuses
04:05 Act Five — Scoring a shop from outside
04:41 The Five-Point Checklist
05:21 Who Does What
05:54 Where You Have Seen It
06:20 How To Use It
06:51 Thanks for Watching
```

## Tags

https, tls, hsts, secure cookie, redirect, certificate, java httpserver, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/https-server-explained.mp4` |
| Captions | `video/https-server-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (98)
- [ ] description is at most 5,000 characters — ok (600)
- [ ] chapters start at 00:00 and there are at least three
