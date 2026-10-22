# YouTube sheet — CORS with Spring Boot

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

CORS in Spring Boot: Reflecting Origins, null, Lookalikes and the Preflight (OWASP A02)

## Description

CORS in Spring Boot 4.1 with Spring Security 7: a bank API with no policy where a page can send but not read, the policies that look like fixes (a wildcard with credentials, a reflecting origin pattern, a list that allows null, a pattern that matches a lookalike), an exact allow-list that Spring enforces with a 403 before the controller, the preflight that Spring Security answers only if the policy is registered there, and why CORS is a browser rule and not a defence.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/api-security/cors-with-spring-boot

Chapters:

```
00:00 Introduction
01:33 The Scenario
01:54 Act One — No policy: a page may send, but not read
02:23 Act Two — Policies that look like fixes
02:54 Act Three — An exact allow-list
03:27 Act Four — Where the policy is registered
03:55 Act Five — The bill
04:26 What It Costs
04:50 Who Does What
05:14 Where You Have Seen It
05:30 How To Use It
05:55 Thanks for Watching
```

## Tags

cors, spring boot, spring security, preflight, allowedOriginPatterns, owasp top 10, a02, java, api security

## Files

| What | File |
| --- | --- |
| Video | `video/cors-with-spring-boot-explained.mp4` |
| Captions | `video/cors-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (87)
- [ ] tags are at most 500 characters in total — ok (99)
- [ ] description is at most 5,000 characters — ok (472)
- [ ] chapters start at 00:00 and there are at least three
