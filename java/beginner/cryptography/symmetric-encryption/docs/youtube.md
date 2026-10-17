# YouTube sheet — Symmetric Encryption (AES-GCM)

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Symmetric Encryption (AES-GCM) Explained in Java

## Description

Symmetric encryption in Java: one secret key locks the data and the same key unlocks it. Explained with an online bank's account statements, like a lock box where the same key closes and opens it, and where a good lock box also shows if anyone has been at it. We store a statement in plain text, encrypt it with AES in ECB mode and see its repeated payments show through, switch to AES-GCM with a fresh nonce for every message and watch a flipped bit and a moved message get refused, show what an attacker learns from a single reused nonce, and finish with the bill: the size still shows, random nonces run out near four billion messages, and a lost key is lost data.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/symmetric-encryption

Chapters:

```
00:00 Introduction
01:04 The Scenario
01:38 The Words
02:09 Act One — A statement stored as it is
02:36 Act Two — AES in ECB mode: every block on its own
03:21 Act Three — AES-GCM: hiding and checking
04:18 Act Four — The one mistake: using a nonce twice
05:12 Act Five — The bill: sizes, nonces, and the key
05:58 The Modes Compared
06:30 Who Does What
07:00 Where You Have Seen It
07:31 How To Use It
07:58 Thanks for Watching
```

## Tags

aes, aes-gcm, symmetric encryption, nonce, ecb mode, authenticated encryption, cryptography basics, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/symmetric-encryption-explained.mp4` |
| Captions | `video/symmetric-encryption-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (48)
- [ ] tags are at most 500 characters in total — ok (125)
- [ ] description is at most 5,000 characters — ok (667)
- [ ] chapters start at 00:00 and there are at least three
