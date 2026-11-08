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
01:06 The Scenario
01:34 The Words
02:07 What the Letters Stand For
03:11 One shop, two doors
04:07 Three checks before a single byte of the cart
05:17 Act One — An HTTPS request that is checked
05:39 The first visit is redirected, the next ones go straight to HTTPS
06:50 Act Two — Plain HTTP only redirects
07:13 Which requests carry the session cookie
08:05 Act Three — A cookie that stays on HTTPS
08:25 Four attempts, and why three are refused
09:30 Act Four — What the client refuses
09:50 Same certificate, five points, two scores
10:49 Act Five — Scoring a shop from outside
11:03 The Five-Point Checklist
11:42 Where You Have Seen It
12:10 How To Use It
12:41 The Java Classes
12:54 ShopServer — the plain door only redirects
13:08 ShopServer — the HTTPS door adds HSTS and a Secure cookie
13:28 TestCertificates — the client trusts exactly one certificate
13:46 ShopClient — verify, and never follow a redirect blindly
14:03 HttpsDemo — the three checks, with the real certificate
14:16 HttpsChecklist — one check: plain HTTP must redirect
14:35 Thanks for Watching
```

## Tags

https, tls, hsts, secure cookie, redirect, certificate, java httpserver, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/https-server-explained.mp4` |
| Captions | `video/https-server-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS and Certificates in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (149)
- [ ] description is at most 5,000 characters — ok (600)
- [ ] chapters start at 00:00 and there are at least three
