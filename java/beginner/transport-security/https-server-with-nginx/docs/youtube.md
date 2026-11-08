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
01:20 The Scenario
01:47 The Words
02:22 What the Letters Stand For
03:25 What the Letters Stand For (continued)
04:15 What the Letters Stand For (continued)
04:47 Why the client trusts the shop's certificate
05:47 TLS ends at nginx, plain HTTP goes on to the app
06:53 Act One — nginx ends the TLS for the shop
07:13 Two servers, two clients, and what one cipher name means
08:19 Act Two — Which TLS versions the shop allows
08:35 The plain port only redirects, and HSTS lasts two years
09:42 Act Three — The plain port, the redirect and HSTS
10:01 The client checks the server, then the server checks the client
11:10 Act Four — Who the client trusts, and who the server trusts
11:21 Act Four — Who the client trusts, and who the server trusts
11:36 A forged header, a config test, and seven habits
12:42 Act Five — The proxy trap, a config check and a score
13:03 Act Five — The proxy trap, a config check and a score
13:13 A Careful Configuration
13:48 Where You Have Seen It
14:20 How To Use It
14:51 The Java Classes
15:05 NginxConfig — TLS 1.3, and mutual TLS when asked
15:26 NginxConfig — the app is told what nginx saw
15:40 Nginx — nginx -t before any customer
15:57 Pki — trust the demo root, or the JDK's list
16:13 TlsClient — the versions it offers
16:28 Probe — a request a normal client would not send
16:37 ConfigScore — one habit: HSTS on every answer
16:50 Thanks for Watching
```

## Tags

nginx, https, tls 1.3, hsts, mtls, reverse proxy, http2, x-forwarded-proto, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/https-server-with-nginx-explained.mp4` |
| Captions | `video/https-server-with-nginx-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS and Certificates in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (64)
- [ ] tags are at most 500 characters in total — ok (151)
- [ ] description is at most 5,000 characters — ok (662)
- [ ] chapters start at 00:00 and there are at least three
