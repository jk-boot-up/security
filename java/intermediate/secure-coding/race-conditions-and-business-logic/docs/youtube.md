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
01:22 The Scenario
01:59 The Words
02:43 What the Letters Stand For
03:25 Two withdrawals, one gap
04:34 Every order of the two withdrawals' steps
05:38 Act One — Check, then act
06:03 Act One — Check, then act
06:22 Act One — Check, then act
06:36 Act One — Check, then act
06:52 A lock and a compare-and-set, step by step
08:06 Act Two — Making the check and the write one step
08:26 Act Two — Making the check and the write one step
08:49 Act Two — Making the check and the write one step
09:07 Act Two — Making the check and the write one step
09:26 A payment sent twice, without and with a key
10:40 Act Three — Sending the same request twice
11:02 Act Three — Sending the same request twice
11:27 Three rules the server keeps, before and after
12:48 Act Four — Rules the server must keep
13:07 Act Four — Rules the server must keep
13:32 Act Four — Rules the server must keep
13:56 Two lock orders, and two servers with their own locks
15:07 Act Five — The bill
15:25 Act Five — The bill
15:43 Act Five — The bill
16:08 Ways to Close the Gap
16:37 What It Costs
17:11 Where You Have Seen It
17:40 How To Use It
18:09 The Java Classes
18:21 Interleaving — one letter, one step
18:39 NaiveWithdrawal — read, check, write
18:52 LockedWithdrawal — wait for the lock, then read
19:08 Ledger and CasWithdrawal — compare, then set, in one step
19:23 Ledger and CasWithdrawal — on failure, start again
19:33 Bank — claim the key in one step
19:51 Bank — rules the server keeps
20:01 Checkout — the server's price, one coupon
20:15 Order — each step from the right state only
20:29 LockingTransfer — the smaller account first
20:44 Thanks for Watching
```

## Tags

race condition, toctou, business logic, idempotency key, compare and set, deadlock, owasp top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/race-conditions-and-business-logic-explained.mp4` |
| Captions | `video/race-conditions-and-business-logic-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (60)
- [ ] tags are at most 500 characters in total — ok (167)
- [ ] description is at most 5,000 characters — ok (825)
- [ ] chapters start at 00:00 and there are at least three
