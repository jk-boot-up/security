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
01:57 Act One — What a security log must answer
02:42 Act Two — What never to log
03:27 Act Three — Text that tries to write the log
04:12 Act Four — A log that shows it was changed
05:03 Act Five — The bill
06:00 Four Properties of a Good Log
06:30 Who Does What
07:06 Where You Have Seen It
07:36 How To Use It
08:10 Thanks for Watching
```

## Tags

security logging, owasp top 10, a09, log injection, audit log, tamper evident, hash chain, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/security-logging-explained.mp4` |
| Captions | `video/security-logging-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (77)
- [ ] tags are at most 500 characters in total — ok (109)
- [ ] description is at most 5,000 characters — ok (712)
- [ ] chapters start at 00:00 and there are at least three
