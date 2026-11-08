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
00:57 The Scenario
01:26 The Words
01:50 What the Letters Stand For
02:46 What the Letters Stand For (continued)
03:05 What SHA-256 does to one invoice
03:47 A checksum: an accident, then an attack
04:35 Act One — A checksum for accidents
04:53 One digit changed: a completely different hash
05:35 Act Two — A cryptographic hash: SHA-256
05:52 A hash beside the data, and a hash by another road
06:41 Act Three — A hash kept beside the data
06:51 Act Three — A hash kept beside the data
06:58 HMAC: a tag only the key holder can make
07:48 Act Four — A keyed hash: HMAC-SHA-256
08:06 Act Four — A keyed hash: HMAC-SHA-256
08:18 How the HMAC tag is made
09:06 Act Five — How the tag is made, step by step
09:19 Act Five — How the tag is made, step by step
09:31 What HMAC does not solve on its own
10:23 Act Six — The bill: boundaries, repeats, and a shared key
10:34 Act Six — The bill: boundaries, repeats, and a shared key
10:45 Where You Have Seen It
11:09 How To Use It
11:37 The Java Classes
11:46 IntegrityDemo — the six acts
11:55 Crc32Checksum — no secret
12:08 Sha256 — any input, 32 bytes out
12:20 InvoiceStore — the attacker rewrites both
12:35 Hmac — tag and verify
12:50 HmacSteps — the four steps
13:06 SignedInvoices — accept, and the length in front
13:23 Thanks for Watching
```

## Tags

hashing, sha-256, hmac, integrity, checksum, crc32, cryptography basics, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/hashing-and-integrity-explained.mp4` |
| Captions | `video/hashing-and-integrity-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cryptography Basics in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (55)
- [ ] tags are at most 500 characters in total — ok (149)
- [ ] description is at most 5,000 characters — ok (734)
- [ ] chapters start at 00:00 and there are at least three
