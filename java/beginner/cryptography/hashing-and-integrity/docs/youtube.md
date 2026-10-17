# YouTube sheet — Hashing and Integrity

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Hashing and Integrity Explained in Java (SHA-256, HMAC)

## Description

Hashing and integrity in Java: how do you know data was not changed? A checksum catches accidents, a cryptographic hash such as SHA-256 gives data a fixed-size fingerprint, and a keyed hash such as HMAC proves it was made by someone holding a secret. Explained with an online store's invoices and a downloadable installer, like a wax seal on a letter: a seal anyone can copy proves nothing, and a seal made with a private ring proves a lot. We see a CRC pass a forged invoice, watch one changed digit flip about half the bits of a SHA-256, see why a hash kept beside the data protects nothing, tag invoices with HMAC, and finish with the bill: joined fields can be ambiguous, tags can be replayed, and everyone with the key can forge.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/hashing-and-integrity

Chapters:

```
00:00 Introduction
01:22 The Scenario
01:58 Three Tools
02:35 Act One — A checksum for accidents
03:26 Act Two — A cryptographic hash: SHA-256
04:31 Act Three — A hash kept beside the data
05:30 Act Four — A keyed hash: HMAC-SHA-256
06:19 Act Five — The bill: boundaries, repeats, and a shared key
07:27 What Each One Proves
08:04 Who Does What
08:35 Where You Have Seen It
09:04 How To Use It
09:37 Thanks for Watching
```

## Tags

hashing, sha-256, hmac, integrity, checksum, crc32, cryptography basics, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/hashing-and-integrity-explained.mp4` |
| Captions | `video/hashing-and-integrity-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (55)
- [ ] tags are at most 500 characters in total — ok (98)
- [ ] description is at most 5,000 characters — ok (734)
- [ ] chapters start at 00:00 and there are at least three
