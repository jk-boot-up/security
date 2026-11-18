# YouTube sheet — Secrets in Errors and Logs

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Secrets in Errors and Logs: Redaction, Log Injection and Safe Errors (Java)

## Description

Secrets in errors and logs, in Java, for an online shop's checkout: a stack trace that shows the customer the database password, logs that keep card numbers, the CVV and session tokens, a line break in a name that forges a log entry, and the fixes: a generic answer with a reference, a Secret type that prints as [REDACTED], a one-line logger with Luhn-checked card masking, and canary tests.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/secrets-in-errors-and-logs

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:19 The Words
02:00 What the Letters Stand For
02:53 What the Letters Stand For (continued)
03:01 One failure, two readers
04:02 What one error page gives away
05:08 Act One — What the customer sees when the database is down
05:29 Act One — What the customer sees when the database is down
05:47 Four secrets in the log, and a line the customer wrote
06:56 Act Two — What the log keeps
07:11 Act Two — What the log keeps
07:33 A reference for the customer, the detail for the team
08:36 Act Three — A generic answer, and the details where only the team looks
08:58 Act Three — A generic answer, and the details where only the team looks
09:09 The Luhn check, and what each rule did
10:19 Act Four — Redaction: what it finds and what it misses
10:34 Act Four — Redaction: what it finds and what it misses
10:52 Act Four — Redaction: what it finds and what it misses
11:10 Act Four — Redaction: what it finds and what it misses
11:28 What It Costs
12:04 Where You Have Seen It
12:38 How To Use It
13:18 The Java Classes
13:31 Checkout — the error path
13:45 Secret — toString is always redacted
14:00 SafeLogger — escape, redact, cut
14:15 Redactor — named rules, run in order
14:28 Luhn — double every second digit
14:43 Canaries — a leak is a match
14:58 Thanks for Watching
```

## Tags

logging, secrets, log injection, redaction, stack trace, owasp, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/secrets-in-errors-and-logs-explained.mp4` |
| Captions | `video/secrets-in-errors-and-logs-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (134)
- [ ] description is at most 5,000 characters — ok (392)
- [ ] chapters start at 00:00 and there are at least three
