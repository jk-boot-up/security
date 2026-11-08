# YouTube sheet — Symmetric Encryption (AES-GCM)

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Symmetric Encryption (AES-GCM) Explained in Java

## Description

Symmetric encryption in Java: one secret key locks the data and the same key unlocks it. Explained with an online bank's account statements, like a lock box where the same key closes and opens it, and where a good lock box also shows if anyone has been at it. We store a statement in plain text, encrypt it with AES in ECB mode and see its repeated payments show through, switch to AES-GCM with a fresh nonce for every message and watch a flipped bit and a moved message get refused, show what an attacker learns from a single reused nonce, and finish with the bill: the size still shows, random nonces limit one key to about four billion messages, and a lost key is lost data.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/symmetric-encryption

Chapters:

```
00:00 Introduction
01:07 The Scenario
01:40 The Words
02:12 What the Letters Stand For
03:21 What the Letters Stand For (continued)
04:01 What AES does to one line
05:05 Act One — A statement stored as it is
05:45 ECB: the same line gives the same block
06:48 Act Two — AES in ECB mode: every block on its own
07:07 Act Two — AES in ECB mode: every block on its own
07:36 Sealing one line with AES-GCM
08:48 Act Three — AES-GCM, step by step: sealing one line
09:24 How the tag is made
10:58 Act Four — How the tag is made, step by step
11:27 Act Four — How the tag is made, step by step
11:48 Same line, new nonce: a different result
12:33 Act Five — The same line, sealed again
12:55 Opening: the tag is checked first
13:50 Act Six — Opening it, and three changes
14:22 The mistake: one nonce used twice
15:42 Act Seven — The one mistake: using a nonce twice
16:21 What encryption does not hide
17:13 Act Eight — The bill: sizes, nonces, and the key
17:37 The Modes Compared
18:09 Where You Have Seen It
18:40 How To Use It
19:07 The Java Classes
19:16 SymmetricDemo — the eight acts
19:28 GcmBox — seal: encrypt and add the tag
19:55 GcmBox — open: the tag is checked first
20:18 Sealed — what gets stored
20:33 NonceSource — random, counting, repeated
20:52 EcbCipher — no nonce, no tag
21:02 TagSteps — the four steps
21:17 Hex — helpers
21:24 Thanks for Watching
```

## Tags

aes, aes-gcm, symmetric encryption, nonce, ecb mode, authenticated encryption, cryptography basics, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/symmetric-encryption-explained.mp4` |
| Captions | `video/symmetric-encryption-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cryptography Basics in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (48)
- [ ] tags are at most 500 characters in total — ok (176)
- [ ] description is at most 5,000 characters — ok (677)
- [ ] chapters start at 00:00 and there are at least three
