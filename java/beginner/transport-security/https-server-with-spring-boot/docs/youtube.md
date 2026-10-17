# YouTube sheet — An HTTPS Server with Spring Boot

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

HTTPS Server with Spring Boot (SSL Bundles, HSTS, Reload, mTLS)

## Description

An HTTPS server with Spring Boot, in Java: the earlier project built HTTPS from the JDK's parts, and this one lets Spring Boot do the plumbing. Explained with an online store, like fitting the shop with a standard door that the locksmith has certified, and still walking round the outside to check it is shut. We configure TLS 1.3 from an SSL bundle, make a client that verifies the certificate and the host name, redirect plain HTTP with Spring Security and add HSTS and secure cookies, renew the certificate while the server keeps running, require a client certificate for mutual TLS, and finish with what the framework leaves to you: error pages, headers and the private key file.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/transport-security/https-server-with-spring-boot

Chapters:

```
00:00 Introduction
01:08 The Scenario
01:29 The Words
02:00 Act One — TLS from an SSL bundle
02:47 Act Two — Redirect, HSTS and secure cookies
03:28 Act Three — Renewing a certificate without a restart
03:57 Act Four — Mutual TLS: the server checks the client too
04:35 Act Five — What Spring leaves to you
05:17 Plain Version and Framework Version
05:42 Who Does What
06:13 Where You Have Seen It
06:39 How To Use It
07:07 Thanks for Watching
```

## Tags

spring boot, https, tls 1.3, ssl bundles, hsts, mtls, certificate renewal, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/https-server-with-spring-boot-explained.mp4` |
| Captions | `video/https-server-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (63)
- [ ] tags are at most 500 characters in total — ok (100)
- [ ] description is at most 5,000 characters — ok (683)
- [ ] chapters start at 00:00 and there are at least three
