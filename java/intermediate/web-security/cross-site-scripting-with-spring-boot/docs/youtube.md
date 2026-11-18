# YouTube sheet — Cross-Site Scripting with Spring Boot: Thymeleaf Escaping and a Nonce CSP

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

XSS Defences in Spring Boot: Thymeleaf Escaping and a Nonce-Based CSP

## Description

Cross-site scripting defences with Spring Boot and Thymeleaf: the template engine does the encoding, as long as you do not switch it off. Explained with an online store's product reviews. We see th:text escape a review by default, see th:utext turn the same review into markup, write values into an attribute, a link and a script the way Thymeleaf escapes each, send a strict Content-Security-Policy from Spring Security with a fresh nonce for every page, and finish with the bill: escaping protects only what goes through it, and the nonce needs one small filter.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/cross-site-scripting-with-spring-boot

Chapters:

```
00:00 Introduction
01:08 The Scenario
01:27 The Words
01:55 What the Letters Stand For
02:54 What the Letters Stand For (continued)
03:26 From the model to the browser
04:17 th:text: the value goes in, escaped text comes out
05:03 Act One — Thymeleaf escapes by default
05:23 th:utext: the same value becomes part of the page
06:08 Act Two — th:utext, the one switch that turns escaping off
06:36 Three places, three ways of escaping
07:30 Act Three — The other places a value can go
07:41 Act Three — The other places a value can go
08:02 One nonce per request, in the header and in the page
08:57 Act Four — A strict Content-Security-Policy from Spring Security
09:20 Act Four — A strict Content-Security-Policy from Spring Security
09:42 What It Costs
10:20 Two Lines of Defence
10:48 Where You Have Seen It
11:12 How To Use It
11:41 The Java Classes
11:57 ReviewController — plain values, no encoding
12:06 NonceFilter — before the headers are written
12:17 Shop — the filter, then the header
12:29 Templates — search every template
12:40 Thanks for Watching
```

## Tags

xss, cross-site scripting, spring boot, thymeleaf, spring security, content security policy, csp nonce, owasp top 10, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cross-site-scripting-with-spring-boot-explained.mp4` |
| Captions | `video/cross-site-scripting-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (69)
- [ ] tags are at most 500 characters in total — ok (165)
- [ ] description is at most 5,000 characters — ok (564)
- [ ] chapters start at 00:00 and there are at least three
