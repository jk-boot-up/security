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
00:49 The Scenario
01:17 The Words
01:52 What the Letters Stand For
02:54 The same failure, two answers
03:44 One risky payment, five fraud checks, four approvals
04:41 Act One — Failing open
04:55 Act One — Failing open
05:07 Start from no: six payments, one yes
06:06 Act Two — Failing closed
06:18 Act Two — Failing closed
06:36 One transfer, four versions, three losses
07:31 Act Three — Failing halfway
07:56 Act Three — Failing halfway
08:14 One failure, three places
09:09 Act Four — What the customer is told, and what the log keeps
09:27 Act Four — What the customer is told, and what the log keeps
09:40 Ten coffees in an outage: calls that reach the fraud check
10:33 Act Five — The bill, and how to pay it
10:50 Act Five — The bill, and how to pay it
11:08 What It Costs
11:49 Where You Have Seen It
12:23 How To Use It
13:00 The Java Classes
13:15 OpenPaymentService — an empty catch keeps the yes
13:25 ClosedPaymentService — only a low-risk answer is a yes
13:42 FraudClient — connect and read timeouts
13:56 Transfers — rollbackFor, and the proxy
14:15 Breaker — open means no
14:28 ErrorHandling — a ProblemDetail with a reference
14:43 Thanks for Watching
```

## Tags

fail closed, spring boot, transactional, error handling, owasp top 10, a10, retry, circuit breaker, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/fail-closed-error-handling-with-spring-boot-explained.mp4` |
| Captions | `video/fail-closed-error-handling-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Configuration in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (94)
- [ ] tags are at most 500 characters in total — ok (168)
- [ ] description is at most 5,000 characters — ok (444)
- [ ] chapters start at 00:00 and there are at least three
