# YouTube sheet — Failing Closed: Secure Error Handling

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Failing Closed in Java: Secure Error Handling (OWASP A10)

## Description

Failing closed in Java, OWASP Top 10 A10, mishandling of exceptional conditions. Explained with an online bank, like a bank vault whose door unlocks itself during a power cut. We watch a payment check approve a risky payment because the fraud service was down, and skip a daily limit because a value could not be read; rebuild it so that only a check that ran and passed can say yes; make a transfer to a frozen account all or nothing, and give database connections back on every path; show the customer a reference instead of a stack trace while the log keeps the detail without the card number; and finish with the bill: failing closed costs availability, paid for with time budgets, a limited retry, a circuit breaker that refuses fast, and an alert on refusals.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-configuration/fail-closed-error-handling

Chapters:

```
00:00 Introduction
01:08 The Scenario
01:33 The Words
02:03 What the Letters Stand For
02:50 The same failure, two answers
03:43 Two payments, two broken checks, two approvals
04:45 Act One — Failing open
05:08 Act One — Failing open
05:24 Start from no: five payments, one yes
06:23 Act Two — Failing closed
06:38 Act Two — Failing closed
07:01 Half a transfer, and connections that never come back
08:05 Act Three — Failing halfway
08:26 Act Three — Failing halfway
08:47 One failure, three places: the raw page, the safe page, the log
09:40 Act Four — What the customer is told, and what the log keeps
10:00 Act Four — What the customer is told, and what the log keeps
10:17 Ten coffees in an outage, with and without a breaker
11:18 Act Five — The bill, and how to pay it
11:37 Act Five — The bill, and how to pay it
11:58 Every Path Ends Somewhere
12:26 Where You Have Seen It
12:46 How To Use It
13:15 The Java Classes
13:29 NaiveAuthorizer — an empty catch keeps the yes
13:42 SafeAuthorizer — only a passed check returns yes
13:57 FraudService — given up when the budget runs out
14:12 Transfers — check first, undo on failure
14:26 ConnectionPool — try-with-resources
14:38 SecurityLog — keep only the last four digits
14:52 CircuitBreaker — open means no
15:06 Thanks for Watching
```

## Tags

fail closed, error handling, owasp top 10, a10, exceptional conditions, circuit breaker, try with resources, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/fail-closed-error-handling-explained.mp4` |
| Captions | `video/fail-closed-error-handling-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Configuration in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (57)
- [ ] tags are at most 500 characters in total — ok (178)
- [ ] description is at most 5,000 characters — ok (765)
- [ ] chapters start at 00:00 and there are at least three
