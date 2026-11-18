# YouTube sheet — Security Logging

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Security Logging in Java (OWASP A09: What to Log, Redaction, Tamper Evidence)

## Description

Security logging in Java, OWASP Top 10 A09: a log that can answer who moved the money, holds no secrets, and shows when it was changed. Explained with an online bank, like a bank's visitor book that records every visit but never the combination of the safe. We compare an old logger that cannot say who made a transfer with a structured security log that can, remove passwords, tokens and session ids and mask card numbers, stop a typed line break from forging a log line, chain every line to the one before it with SHA-256 so a deleted or edited line is found, and finish with the bill: logs are personal data with retention limits, they must leave the host to survive a breach, and their volume needs a budget.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/detection-and-response/security-logging

Chapters:

```
00:00 Introduction
01:02 The Scenario
01:23 The Words
01:57 What the Letters Stand For
02:58 What the Letters Stand For (continued)
03:11 One transfer, two logs
04:13 Every event as ana, one lane per address
05:17 Act One — What a security log must answer
05:44 Act One — What a security log must answer
06:06 Act One — What a security log must answer
06:33 Five secrets, before and after the redactor
07:34 Act Two — What never to log
07:55 Act Two — What never to log
08:22 One failed login: two lines in plain text, one in JSON
09:19 Act Three — Text that tries to write the log
09:47 Act Three — Text that tries to write the log
10:13 Eight lines chained by SHA-256, and an edit to line 6
11:11 Act Four — A log that shows it was changed
11:31 Act Four — A log that shows it was changed
11:57 Act Four — A log that shows it was changed
12:13 A copy that survives, a period to keep, and the bytes
13:19 Act Five — The bill
13:36 Act Five — The bill
14:00 Act Five — The bill
14:27 Four Properties of a Good Log
14:58 Where You Have Seen It
15:28 How To Use It
16:01 The Java Classes
16:12 SecurityLog — fixed fields, then the chain
16:31 Redactor — removed by the field's name
16:43 Redactor — card numbers caught by their shape
16:56 Json — escape, never trust
17:08 ChainCheck — each prev must be the hash of the line before
17:23 Retention — the first kept line's prev is the anchor
17:37 Thanks for Watching
```

## Tags

security logging, owasp top 10, a09, log injection, audit log, tamper evident, hash chain, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/security-logging-explained.mp4` |
| Captions | `video/security-logging-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Detection and Response in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (77)
- [ ] tags are at most 500 characters in total — ok (160)
- [ ] description is at most 5,000 characters — ok (712)
- [ ] chapters start at 00:00 and there are at least three
