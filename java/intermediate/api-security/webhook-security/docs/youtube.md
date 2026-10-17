# YouTube sheet — Webhook Security: Signatures, Replays and Idempotency

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Webhook Security in Java (HMAC Signatures, Replay Protection, Idempotency)

## Description

Webhook security in Java: a payment provider tells an online store "payment succeeded" by posting to a public address, and anyone can post to that address. A forged message marks an order paid that was never paid; then an HMAC-SHA256 signature over the exact raw body stops forgeries and changed bodies, a signed timestamp and a record of processed events stop a real refund message from being replayed and paid twice, idempotent handlers cope with retries and events out of order, and two secrets overlap while the secret is changed. The bill: keep the raw bytes, store and rotate the secret, and still confirm critical events with the provider's API. Mapped to the OWASP API Security Top 10 API8 and API10 and CWE-345.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/api-security/webhook-security

Chapters:

```
00:00 Introduction
01:04 The Scenario
01:26 The Words
01:54 Act One — An endpoint that trusts whatever is posted
02:26 Act Two — A signature over the exact bytes
03:11 Act Three — A real message sent again
03:59 Act Four — Twice, out of order, and a new secret
04:41 Act Five — The bill
05:21 Four Questions for Every Message
05:48 Who Does What
06:16 Where You Have Seen It
06:42 How To Use It
07:12 Thanks for Watching
```

## Tags

webhooks, hmac, webhook signature, replay attack, idempotency, api security, owasp api top 10, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/webhook-security-explained.mp4` |
| Captions | `video/webhook-security-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (74)
- [ ] tags are at most 500 characters in total — ok (113)
- [ ] description is at most 5,000 characters — ok (720)
- [ ] chapters start at 00:00 and there are at least three
