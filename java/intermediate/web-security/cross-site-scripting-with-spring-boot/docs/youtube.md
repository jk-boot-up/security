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
01:55 Act One — Thymeleaf escapes by default
02:20 Act Two — th:utext, the one switch that turns escaping off
02:51 Act Three — The other places a value can go
03:25 Act Four — A strict Content-Security-Policy from Spring Security, with a fresh nonce for every page
04:03 Act Five — The bill
04:42 Two Lines of Defence
05:09 Who Does What
05:38 Where You Have Seen It
06:03 How To Use It
06:31 Thanks for Watching
```

## Tags

xss, cross-site scripting, spring boot, thymeleaf, spring security, content security policy, csp nonce, owasp top 10, java

## Files

| What | File |
| --- | --- |
| Video | `video/cross-site-scripting-with-spring-boot-explained.mp4` |
| Captions | `video/cross-site-scripting-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (69)
- [ ] tags are at most 500 characters in total — ok (114)
- [ ] description is at most 5,000 characters — ok (564)
- [ ] chapters start at 00:00 and there are at least three
