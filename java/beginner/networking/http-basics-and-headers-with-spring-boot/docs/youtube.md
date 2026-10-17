# YouTube sheet — HTTP Basics and Security Headers with Spring Boot

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

HTTP Security Headers with Spring Boot (HSTS, CSP, Cookies, Limits)

## Description

HTTP basics and security headers with Spring Boot, in Java: the earlier project built HTTP by hand, and this one lets a real framework do it. Explained with an online store, like fitting the shop with standard locks instead of forging them. We start two real Spring Boot servers on the loopback interface, one with headers switched off and one hardened, and score their real responses. We see why Strict-Transport-Security appears only over HTTPS, harden the session cookie with three properties, watch Tomcat refuse a line break in a header, bound the size of a request, and note the cost: a framework's defaults are a starting point, and trusting a forwarded header is a decision.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/http-basics-and-headers-with-spring-boot

Chapters:

```
00:00 Introduction
01:06 The Scenario
01:29 The Words
01:58 Act One — The same page, with and without header writers
02:30 Act Two — The same page behind a proxy that ends HTTPS
02:59 Act Three — The cookie that carries the cart's session
03:22 Act Four — A line break inside a header
03:56 Act Five — Limits on the size of a request
04:21 What Is Decided Where
04:48 Who Does What
05:17 Where You Have Seen It
05:41 How To Use It
06:03 Thanks for Watching
```

## Tags

spring boot, spring security, security headers, hsts, csp, cookies, samesite, tomcat, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/http-basics-and-headers-with-spring-boot-explained.mp4` |
| Captions | `video/http-basics-and-headers-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (67)
- [ ] tags are at most 500 characters in total — ok (110)
- [ ] description is at most 5,000 characters — ok (682)
- [ ] chapters start at 00:00 and there are at least three
