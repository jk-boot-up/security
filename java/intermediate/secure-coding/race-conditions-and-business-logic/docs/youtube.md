# YouTube sheet — Race Conditions and Business Logic

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Race Conditions and Business Logic Flaws in Java (OWASP A06)

## Description

Race conditions and business logic flaws in Java, OWASP Top 10 A06, insecure design. Explained with an online bank and store, like two cashiers who each check the same nearly empty account and both hand over the money. We watch two withdrawals of 80 from a balance of 100 both succeed, and a single-use coupon used twice, by running the steps of two requests in a written order; fix them with a lock and with a compare-and-set, so the balance can never go below zero; stop a retried payment from paying twice with an idempotency key; keep the server's own rules about negative amounts, transfers to yourself, stacked coupons, totals sent by the client and orders refunded twice; and finish with the bill: locks make requests take turns, locks taken in the wrong order deadlock, and a lock in one server does not stop another.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/race-conditions-and-business-logic

Chapters:

```
00:00 Introduction
01:12 The Scenario
01:33 The Words
02:04 Act One — Check, then act
02:49 Act Two — Making the check and the write one step
03:38 Act Three — Sending the same request twice
04:19 Act Four — Rules the server must keep
05:08 Act Five — The bill
05:51 Ways to Close the Gap
06:20 Who Does What
06:51 Where You Have Seen It
07:19 How To Use It
07:48 Thanks for Watching
```

## Tags

race condition, toctou, business logic, idempotency key, compare and set, deadlock, owasp top 10, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/race-conditions-and-business-logic-explained.mp4` |
| Captions | `video/race-conditions-and-business-logic-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (60)
- [ ] tags are at most 500 characters in total — ok (116)
- [ ] description is at most 5,000 characters — ok (825)
- [ ] chapters start at 00:00 and there are at least three
