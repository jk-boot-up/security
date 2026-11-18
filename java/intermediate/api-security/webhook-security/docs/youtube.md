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
01:54 What the Letters Stand For
02:53 What the Letters Stand For (continued)
03:35 A message and its signature
04:31 A forged payment, and what the provider really knows
05:26 Act One — An endpoint that trusts whatever is posted
05:46 An HMAC, step by step, with the real values
07:00 Act Two — How the provider signs a message
07:20 Act Two — How the provider signs a message
07:36 The v1 sent, the v1 computed, and the answer
08:34 Act Three — The store checks the signature
08:50 Act Three — The store checks the signature
09:02 One real refund, sent again
10:14 Act Four — A real message sent again
10:29 Act Four — A real message sent again
10:50 Events twice and out of order, and a change of secret
11:57 Act Five — Twice, out of order, and a new secret
12:16 Act Five — Twice, out of order, and a new secret
12:31 The same event, different bytes, a different v1
13:23 Act Six — The bill
13:36 Act Six — The bill
13:53 Four Questions for Every Message
14:20 Who Does What
14:53 Where You Have Seen It
15:20 How To Use It
15:49 The Java Classes
16:04 Signatures — what is signed, and the header
16:15 Signatures — a constant-time comparison, under any secret
16:27 HmacSteps — pad, mix, hash twice
16:41 Receiver — signature, time, then event id
16:57 Receiver — not yet: try again later
17:12 Orders — a state check
17:25 WebhookServer — keep the raw bytes
17:36 Thanks for Watching
```

## Tags

webhooks, hmac, webhook signature, replay attack, idempotency, api security, owasp api top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/webhook-security-explained.mp4` |
| Captions | `video/webhook-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: API Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (74)
- [ ] tags are at most 500 characters in total — ok (164)
- [ ] description is at most 5,000 characters — ok (720)
- [ ] chapters start at 00:00 and there are at least three
