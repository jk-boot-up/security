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
02:03 Act One — Failing open
02:43 Act Two — Failing closed
03:24 Act Three — Failing halfway
04:03 Act Four — What the customer is told, and what the log keeps
04:41 Act Five — The bill, and how to pay it
05:26 Every Path Ends Somewhere
05:53 Who Does What
06:22 Where You Have Seen It
06:42 How To Use It
07:11 Thanks for Watching
```

## Tags

fail closed, error handling, owasp top 10, a10, exceptional conditions, circuit breaker, try with resources, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/fail-closed-error-handling-explained.mp4` |
| Captions | `video/fail-closed-error-handling-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (57)
- [ ] tags are at most 500 characters in total — ok (127)
- [ ] description is at most 5,000 characters — ok (765)
- [ ] chapters start at 00:00 and there are at least three
