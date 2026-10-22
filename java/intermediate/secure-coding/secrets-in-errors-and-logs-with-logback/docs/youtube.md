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
01:36 The Scenario
01:50 Act One — What the log keeps
02:06 Act Two — Covering the shapes of secrets in the layout
02:30 Act Three — Make the secret unprintable
02:52 Act Four — Structured logs
03:12 Act Five — The bill
03:30 What It Costs
03:55 Who Does What
04:17 Where You Have Seen It
04:31 How To Use It
04:51 Thanks for Watching
```

## Tags

logback, logstash, log masking, log injection, secrets in logs, structured logging, json logs, owasp top 10, a09, java

## Files

| What | File |
| --- | --- |
| Video | `video/secrets-in-errors-and-logs-with-logback-explained.mp4` |
| Captions | `video/secrets-in-errors-and-logs-with-logback-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (88)
- [ ] tags are at most 500 characters in total — ok (109)
- [ ] description is at most 5,000 characters — ok (471)
- [ ] chapters start at 00:00 and there are at least three
