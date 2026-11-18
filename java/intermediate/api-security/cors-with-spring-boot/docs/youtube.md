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
00:55 The Scenario
01:31 The Words
02:13 What the Letters Stand For
03:19 Where Spring checks CORS, and where it refuses
04:16 Sending is not reading
05:15 Act One — With no policy, a page may send but not read
05:31 Act One — With no policy, a page may send but not read
05:49 Four policies, three origins, who reads the balance
06:53 Act Two — Four policies that look like fixes
07:12 Act Two — Four policies that look like fixes
07:23 Act Two — Four policies that look like fixes
07:39 One partner on the list, and what each page gets
08:29 Act Three — An exact allow-list names one partner
08:43 Act Three — An exact allow-list names one partner
08:58 Act Three — An exact allow-list names one partner
09:15 The same list, registered in two places
10:01 Act Four — The policy must sit before the login check
10:27 A browser rule, and a caller with no browser
11:19 Act Five — CORS is a browser rule, not a defence
11:43 What It Costs
12:21 Where You Have Seen It
12:55 How To Use It
13:30 The Java Classes
13:45 CorsSettings — the unsafe policies, and the exact list
14:05 BankSecurity — http.cors, or addCorsMappings
14:26 Browser — simple requests, and the read check
14:44 BankController — the log behind every route
14:57 Thanks for Watching
```

## Tags

cors, spring boot, spring security, preflight, allowedOriginPatterns, owasp top 10, a02, java, api security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cors-with-spring-boot-explained.mp4` |
| Captions | `video/cors-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: API Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (87)
- [ ] tags are at most 500 characters in total — ok (150)
- [ ] description is at most 5,000 characters — ok (472)
- [ ] chapters start at 00:00 and there are at least three
