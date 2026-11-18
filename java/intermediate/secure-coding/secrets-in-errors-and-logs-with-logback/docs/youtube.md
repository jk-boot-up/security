# YouTube sheet — Secrets in Logs with Logback

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Secrets in Logs with Logback: Masking, Unprintable Types and Structured JSON (OWASP A09)

## Description

Logback 1.6 and logstash-logback-encoder 9 against a careless checkout log: card number, cvv, token and database password in the file, a forged second event from a line break, a layout with a mask converter and a masked exception converter, a Luhn check so that an order number is not taken for a card, a Secret and a Card that print safely, JSON events with a masking decorator by path and by shape, a record that leaks its fields, and a secret in a shape nobody listed.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-coding/secrets-in-errors-and-logs-with-logback

Chapters:

```
00:00 Introduction
00:53 The Scenario
01:25 The Words
02:08 What the Letters Stand For
03:01 One card number, four ways
03:57 Three log calls, four lines, four secrets
04:56 Act One — What the log keeps
05:06 Act One — What the log keeps
05:24 Act One — What the log keeps
05:33 The same events, masked in the layout
06:36 Act Two — Covering the shapes of secrets in the layout
06:49 Act Two — Covering the shapes of secrets in the layout
07:06 Act Two — Covering the shapes of secrets in the layout
07:15 Types that print safely
08:09 Act Three — Make the secret unprintable
08:26 Act Three — Make the secret unprintable
08:42 Events as data
09:41 Act Four — Structured logs
10:03 Act Four — Structured logs
10:15 What the mask knows, and what it misses
11:13 Act Five — What a mask cannot see
11:26 Act Five — What a mask cannot see
11:38 What It Costs
12:14 Where You Have Seen It
12:45 How To Use It
13:14 The Java Classes
13:25 LogSetup — our own conversion words
13:38 MaskConverter — mask, then escape line breaks
13:49 Masker — patterns, then cards
14:05 Masker — a card only if Luhn passes
14:16 Secret and Card — toString and @JsonValue
14:29 LoggingDemo.masking — by path, and by shape
14:44 Thanks for Watching
```

## Tags

logback, logstash, log masking, log injection, secrets in logs, structured logging, json logs, owasp top 10, a09, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/secrets-in-errors-and-logs-with-logback-explained.mp4` |
| Captions | `video/secrets-in-errors-and-logs-with-logback-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Coding in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (88)
- [ ] tags are at most 500 characters in total — ok (160)
- [ ] description is at most 5,000 characters — ok (471)
- [ ] chapters start at 00:00 and there are at least three
