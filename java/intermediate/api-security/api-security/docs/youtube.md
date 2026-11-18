# YouTube sheet — API Security: Mass Assignment, Data Exposure and Resource Limits

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

API Security in Java: Mass Assignment, Data Exposure and Rate Limits

## Description

API security in Java: mass assignment, excessive data exposure, and unrestricted resource consumption, from the OWASP API Security Top 10. A bank's JSON API that copies every field the app sends into the account lets a customer make herself an admin with a huge credit limit; one that returns the whole record sends the card number and PIN hash to the phone; and one that answers any page size, any number of times, hands over every customer and never slows down. Explained with a bank API, like a bank counter where the customer fills in their own credit limit, the clerk passes over the whole file, and the queue has no end. We fix it with explicit request and response models, a cap on page size, and a token bucket for each caller that answers 429 with Retry-After. We finish with the cost: more code, and limits that need real numbers.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/api-security/api-security

Chapters:

```
00:00 Introduction
01:00 The Scenario
01:27 The Words
02:02 What the Letters Stand For
03:01 What the Letters Stand For (continued)
03:16 Three limits on one record
04:20 One field in, the whole record out
05:13 Act One — A profile call that works
05:37 Two keys added by hand, copied onto the record
06:28 Act Two — Whatever the caller sends becomes a field
06:47 Everything stored is shown, at any size, at any rate
07:45 Act Three — The response is the whole record, and a list has no end
07:57 Act Three — The response is the whole record, and a list has no end
08:12 Named fields in, named fields out, a capped page
09:10 Act Four — Explicit models and a page cap
09:27 Act Four — Explicit models and a page cap
09:43 Act Four — Explicit models and a page cap
09:58 A bucket of tokens per caller
11:07 Act Five — The budget runs out, and refills
11:27 Act Five — The budget runs out, and refills
11:49 What It Costs
12:29 Where You Have Seen It
13:04 How To Use It
13:38 The Java Classes
13:54 NaiveApi — mass assignment
14:08 UpdateProfileRequest — unknown fields refused
14:22 AccountView — five fields, last four digits
14:37 SafeApi — cap the page, then charge for it
14:54 RateLimiter — refill, take, or say how long to wait
15:10 Thanks for Watching
```

## Tags

api security, mass assignment, rate limiting, owasp api top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/api-security-explained.mp4` |
| Captions | `video/api-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: API Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (68)
- [ ] tags are at most 500 characters in total — ok (136)
- [ ] description is at most 5,000 characters — ok (840)
- [ ] chapters start at 00:00 and there are at least three
