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
01:12 The Scenario
01:33 The Words
02:04 What the Letters Stand For
03:07 What the Letters Stand For (continued)
03:32 Who checks whom in a TLS connection
04:27 From three properties to a checked connection
05:37 Act One — TLS from an SSL bundle
05:50 Act One — TLS from an SSL bundle
06:08 Plain HTTP is turned away, HTTPS sets HSTS and the cookie
07:11 Act Two — Redirect, HSTS and secure cookies
07:35 A new certificate file, picked up while the server runs
08:35 Act Three — Renewing a certificate without a restart
08:58 Three clients, and the certificate each one shows
09:58 Act Four — Mutual TLS: the server checks the client too
10:13 Three probes, and what is still your job
11:08 Act Five — What Spring leaves to you
11:22 Plain Version and Framework Version
11:47 Where You Have Seen It
12:13 How To Use It
12:41 The Java Classes
12:55 Shop — TLS as properties: an SSL bundle
13:12 Shop — mutual TLS: two more properties
13:28 ShopApp — redirect plain HTTP, and send HSTS
13:45 ShopApp — who is calling, from the client certificate
13:57 ShopClient — the bundle's trust, and a version limit
14:20 Certificates — a fingerprint is a hash of the certificate
14:38 HttpsSpringDemo — letters instead of machine-made hashes
14:55 Thanks for Watching
```

## Tags

spring boot, https, tls 1.3, ssl bundles, hsts, mtls, certificate renewal, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/https-server-with-spring-boot-explained.mp4` |
| Captions | `video/https-server-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS and Certificates in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (63)
- [ ] tags are at most 500 characters in total — ok (151)
- [ ] description is at most 5,000 characters — ok (683)
- [ ] chapters start at 00:00 and there are at least three
