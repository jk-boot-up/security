# YouTube sheet — HTTPS Server with nginx

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

HTTPS Server with nginx (TLS 1.3, HSTS, mTLS, Forwarded Headers)

## Description

HTTPS with nginx, checked from Java: the web server in front of an online store ends the TLS. Explained with an online store, like a receptionist who checks every visitor at the front door so the staff inside need not. We run real nginx containers and talk to them with a Java client that checks the certificate chain and the host name: TLS 1.3 with HTTP/2, which protocol versions to allow, the redirect from the plain port and HSTS, an impostor certificate and a wrong name refused, client certificates with mutual TLS, a forged X-Forwarded-Proto header that fools the application, a configuration check with nginx -t, and a seven-point score for TLS settings.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/transport-security/https-server-with-nginx

Chapters:

```
00:00 Introduction
01:09 The Scenario
01:36 The Words
02:12 Act One — nginx ends the TLS for the shop
02:50 Act Two — Which TLS versions the shop allows
03:23 Act Three — The plain port, the redirect and HSTS
04:01 Act Four — Who the client trusts, and who the server trusts
04:50 Act Five — The proxy trap, a config check and a score
05:38 A Careful Configuration
06:12 Who Does What
06:44 Where You Have Seen It
07:16 How To Use It
07:48 Thanks for Watching
```

## Tags

nginx, https, tls 1.3, hsts, mtls, reverse proxy, http2, x-forwarded-proto, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/https-server-with-nginx-explained.mp4` |
| Captions | `video/https-server-with-nginx-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (64)
- [ ] tags are at most 500 characters in total — ok (100)
- [ ] description is at most 5,000 characters — ok (662)
- [ ] chapters start at 00:00 and there are at least three
