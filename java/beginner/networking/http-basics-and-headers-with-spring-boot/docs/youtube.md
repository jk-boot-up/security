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
01:14 The Scenario
01:37 The Words
02:07 What the Letters Stand For
03:00 Where the headers are written
04:06 One request, two stores, five headers
05:19 Act One — The same page, with and without header writers
05:34 Act One — The same page, with and without header writers
05:57 HTTPS ends at the proxy, which tells the store
07:11 Act Two — The same page behind a proxy that ends HTTPS
07:25 Act Two — The same page behind a proxy that ends HTTPS
07:55 Two session cookies, checked attribute by attribute
08:58 Act Three — The cookie that carries the cart's session
09:26 The container refuses a line break in a header
10:37 Act Four — A line break inside a header
10:58 One big header against two limits
12:07 Act Five — Limits on the size of a request
12:27 What Is Decided Where
12:54 Where You Have Seen It
13:18 How To Use It
13:40 The Java Classes
13:53 Shop — the filter chain that writes the headers
14:13 HttpSpringDemo — the properties of the hardened store
14:29 ShopController — the search that puts customer text in a header
14:43 HeaderAudit — a header is only worth its value
15:00 Cookie — what is wrong for a session cookie
15:13 Wire — raw bytes a normal client would never send
15:33 Thanks for Watching
```

## Tags

spring boot, spring security, security headers, hsts, csp, cookies, samesite, tomcat, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/http-basics-and-headers-with-spring-boot-explained.mp4` |
| Captions | `video/http-basics-and-headers-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Networking for Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (67)
- [ ] tags are at most 500 characters in total — ok (161)
- [ ] description is at most 5,000 characters — ok (682)
- [ ] chapters start at 00:00 and there are at least three
