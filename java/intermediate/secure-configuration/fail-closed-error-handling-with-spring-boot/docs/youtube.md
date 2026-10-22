# YouTube sheet — Failing Closed with Spring Boot

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Failing Closed in Spring Boot: Fraud Checks, @Transactional Traps, Error Responses (OWASP A10)

## Description

Failing closed in Spring Boot 4.1, OWASP Top 10 A10: a payment check that approves when the fraud service is broken, the same check rebuilt with a time budget, Spring Framework 7 retry and a breaker, four transfers of which three leave the money half moved because of a checked exception or a call inside the same class, Boot's error response that names the server and the card, one error handler with a reference, and the cost in availability.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-configuration/fail-closed-error-handling-with-spring-boot

Chapters:

```
00:00 Introduction
01:36 The Scenario
01:59 Act One — Failing open
02:26 Act Two — Failing closed
02:58 Act Three — Failing halfway
03:33 Act Four — What the customer is told
04:04 Act Five — The bill, and how to pay it
04:35 What It Costs
04:58 Who Does What
05:26 Where You Have Seen It
05:42 How To Use It
06:05 Thanks for Watching
```

## Tags

fail closed, spring boot, transactional, error handling, owasp top 10, a10, retry, circuit breaker, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/fail-closed-error-handling-with-spring-boot-explained.mp4` |
| Captions | `video/fail-closed-error-handling-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (94)
- [ ] tags are at most 500 characters in total — ok (117)
- [ ] description is at most 5,000 characters — ok (444)
- [ ] chapters start at 00:00 and there are at least three
